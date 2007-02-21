package org.apache.lucene.search.spans;


/**
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Acknowledgements:
 *
 * A significant amount of new and/or modified code in this module
 * was made possible by a grant from the Andrew W. Mellon Foundation,
 * as part of the Melvyl Recommender Project.
 */
import java.io.IOException;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;

/** Calculates spans that match several queries "near" each other. In-order
 *  matches score higher than out-of-order matches.
 */
class NearSpans implements Spans 
{
  private SpanNearQuery query;
  private Similarity similarity;
  private List ordered = new ArrayList(); // spans in query order
  private int slop; // from query
  private boolean inOrder; // from query
  private SpansCell firstCell; // linked list of spans
  private SpansCell lastCell; // fully sorted
  private int nCellsInList; // number of cells added so far
  private int totalLength; // sum of current lengths
  private float totalScore; // sum of current scores
  private int totalSlop; // sloppiness of current match
  private boolean more = true; // true iff not done
  private boolean firstTime = true; // true before first next()

  /** Wraps a Spans, and can be used to form a linked list. */
  private class SpansCell implements Spans 
  {
    private Spans spans;
    private SpansCell prevCell;
    private SpansCell nextCell;
    private int length = -1;
    private float score;
    private int index;

    public SpansCell(Spans spans, int index) {
      this.spans = spans;
      this.index = index;
    }

    public boolean next()
      throws IOException 
    {
      preChange();

      boolean more = spans.next(); // move to next
      if (more)
        postChange();

      return more;
    }

    public boolean skipTo(int target)
      throws IOException 
    {
      preChange();

      boolean more = spans.skipTo(target); // skip

      if (more)
        postChange();

      return more;
    }

    /** Called just before advancing the cell */
    private void preChange() 
    {
      if (length != -1) { // subtract old length
        totalLength -= length;
        totalScore -= score;
      }
    }

    /** Called just after advancing the cell */
    private void postChange() {
      length = end() - start(); // compute new length
      totalLength += length; // add new length to total
      score = spans.score();
      totalScore += score;
      adjustPosition();
    }

    // If cell needs to move toward end of list, move it.
    public void adjustPosition() 
    {
      // Already at the end? Can't move forward.
      if (this == lastCell)
        return;

      // Optimization for common case: jump to end
      SpansCell putBefore;
      if (!lessThan(lastCell))
        putBefore = null;
      else 
      {
        // Find where to put it
        putBefore = nextCell;
        while (putBefore != null && !lessThan(putBefore))
          putBefore = putBefore.nextCell;
      }

      // If changing position, unlink and relink
      if (putBefore != nextCell) {
        unlink();
        linkBefore(putBefore);
      }
    }

    /** Remove the current cell from the linked list */
    private void unlink() 
    {
      if (prevCell == null)
        firstCell = nextCell;
      else
        prevCell.nextCell = nextCell;

      if (nextCell == null)
        lastCell = this;
      else
        nextCell.prevCell = prevCell;

      nextCell = prevCell = null;

      --nCellsInList;
    }

    /** Link the cell into the list just before 'other', or at the tail if null */
    private void linkBefore(SpansCell other) {
      nextCell = other;
      prevCell = (other == null) ? lastCell : other.prevCell;
      ++nCellsInList;
      fixLinks();
    }

    /** Link the cell into the list just after 'other', or at the head if null */
    private void linkAfter(SpansCell other) {
      prevCell = other;
      nextCell = (other == null) ? firstCell : other.nextCell;
      ++nCellsInList;
      fixLinks();
    }

    /** Helper function for linkAfter and linkBefore */
    private void fixLinks() 
    {
      if (nextCell == null)
        lastCell = this;
      else
        nextCell.prevCell = this;

      if (prevCell == null)
        firstCell = this;
      else
        prevCell.nextCell = this;
    }

    /** Debugging only: check that the links are all correct */
    @SuppressWarnings("unused")
    private void checkList() 
    {
      System.out.println("DEBUGGING ONLY");

      int nCells = 0;
      for (SpansCell cell = firstCell; cell != null; cell = cell.nextCell) 
      {
        if (cell == firstCell)
          assert cell.prevCell == null;

        if (cell == lastCell)
          assert cell.nextCell == null;

        if (cell.prevCell == null)
          assert firstCell == cell;
        else
          assert cell.prevCell.nextCell == cell;

        if (cell.nextCell == null)
          assert lastCell == cell;
        else
          assert cell.nextCell.prevCell == cell;
        ++nCells;
        assert nCells <= nCellsInList; // infinite loop?
      }
      assert nCells == nCellsInList;
    }

    /** Ordering function for cells in the list */
    private final boolean lessThan(SpansCell otherCell) 
    {
      if (doc() == otherCell.doc()) 
      {
        if (start() == otherCell.start()) 
        {
          if (end() == otherCell.end()) {
            return index > otherCell.index; // do not flip: needed for out-of-order check
          }
          else {
            return end() < otherCell.end();
          }
        }
        else {
          return start() < otherCell.start();
        }
      }
      else {
        return doc() < otherCell.doc();
      }
    }

    public int doc() {
      return spans.doc();
    }

    public int start() {
      return spans.start();
    }

    public int end() {
      return spans.end();
    }

    public float score() {
      throw new UnsupportedOperationException();
    }

    public Explanation explain()
      throws IOException 
    {
      return spans.explain();
    }

    public void collectTerms(Set terms) {
    }

    public String toString() {
      return spans.toString() + "#" + index;
    }
  }

  public NearSpans(SpanNearQuery query, IndexReader reader, Searcher searcher)
    throws IOException 
  {
    this.query = query;
    this.slop = query.getSlop();
    this.inOrder = query.isInOrder();

    SpanQuery[] clauses = query.getClauses(); // initialize spans & list
    for (int i = 0; i < clauses.length; i++) {
      SpansCell cell =  // construct clause spans
                       new SpansCell(clauses[i].getSpans(reader, searcher), i);
      ordered.add(cell); // add to ordered
    }

    similarity = searcher.getSimilarity();
  }

  public boolean next()
    throws IOException 
  {
    if (firstTime) {
      initList(-1);
      firstTime = false;
    }
    else if (more) {
      more = advanceOneCell(); // trigger further scanning
    }

    while (more) 
    {
      // Get rid of cached slop value.
      totalSlop = -1;

      // skip to doc w/ all clauses
      while (more && firstCell.doc() < lastCell.doc()) {
        more = firstCell.skipTo(lastCell.doc()); // skip first upto last
      }

      if (!more)
        return false;

      // found doc w/ all clauses - is there a match?
      if (atMatch())
        return true;

      // Trigger further scanning.
      more = advanceOneCell();
    }
    return false; // no more matches
  }

  private boolean advanceOneCell()
    throws IOException 
  {
    // Is it even possible to adjust the order and get a better match?
    int matchLength = lastCell.end() - firstCell.start();
    if (matchLength - totalLength > slop) 
    {
      // Nope... just advance the first cell.
      return firstCell.next();
    }

    // If things are out of order, but the endpoints are within the
    // specified slop, we might be able to get a better match
    // by advancing one of the out-of-order spans, rather than
    // the first span. This can happen, for instance, if there are
    // repeated terms in a phrase query.
    //
    int index = 0;
    for (SpansCell cell = firstCell; cell != null; cell = cell.nextCell) {
      if (cell.index != index++)
        return cell.next();
    }

    // No out-of-order cell found... just advance the first cell.
    return firstCell.next();
  }

  public boolean skipTo(int target)
    throws IOException 
  {
    if (firstTime) { // initialize
      initList(target);
      firstTime = false;
    }
    else { // normal case
      while (more && firstCell.doc() < target) { // skip as needed
        more = firstCell.skipTo(target);
      }
    }

    // Get rid of cached slop value.
    totalSlop = -1;

    if (more) 
    {
      if (atMatch()) // at a match?
        return true;

      return next(); // no, scan
    }

    return false;
  }

  public int doc() {
    return firstCell.doc();
  }

  public int start() {
    return firstCell.start();
  }

  public int end() {
    return lastCell.end();
  }

  public float score() {
    return totalScore * query.getBoost() * similarity.sloppyFreq(totalSlop());
  }

  public String toString() {
    return "spans(" + query.toString() + ")@" +
           (firstTime ? "START"
            : (more ? (doc() + ":" + start() + "-" + end()) : "END"));
  }

  private void initList(int target)
    throws IOException 
  {
    for (int i = 0; more && i < ordered.size(); i++) {
      SpansCell cell = (SpansCell)ordered.get(i);
      cell.linkAfter(null); // link as first to start with
      if (target < 0)
        more = cell.next(); // move to first entry
      else
        more = cell.skipTo(target);
    }
  }

  private boolean atMatch() {
    return (firstCell.doc() == lastCell.doc()) && checkSlop() &&
           (!inOrder || matchIsOrdered());
  }

  private boolean checkSlop() 
  {
    int matchLength = lastCell.end() - firstCell.start();

    // Is a match even possible?
    if (matchLength - totalLength > slop)
      return false;

    // Do a more thorough slop calculation.
    if (totalSlop() > slop)
      return false;

    return true;
  }

  private boolean matchIsOrdered() 
  {
    int lastStart = -1;
    for (int i = 0; i < ordered.size(); i++) {
      int start = ((SpansCell)ordered.get(i)).start();
      if (!(start > lastStart))
        return false;
      lastStart = start;
    }
    return true;
  }

  private int totalSlop() 
  {
    // If cached value is still valid, just return it.
    if (totalSlop >= 0)
      return totalSlop;

    // Need to recalculate.
    int matchSlop = 0;
    int lastStart = -1;
    int lastEnd = -1;
    for (int i = 0; i < ordered.size(); i++) 
    {
      SpansCell cell = (SpansCell)ordered.get(i);
      int start = cell.start();
      int end = cell.end();

      // First cell, just record the start and end. Subsequent cells, 
      // calculate the slop.
      //
      if (i > 0) 
      {
        // Is the new cell before the old? Penalize it for being out-of-order.
        if (end <= lastStart)
          matchSlop += (lastStart - end) + 1;

        // Is it after?
        else if (start >= lastEnd)
          matchSlop += (start - lastEnd);

        // Overlapping... zero slop
        else
          ; // do nothing
      } // if

      lastStart = start;
      lastEnd = end;
    } // for i

    return totalSlop = matchSlop;
  }

  public Explanation explain()
    throws IOException 
  {
    Explanation result = new Explanation(0,
                                         "weight(" + toString() +
                                         "), product of:");
    Explanation sumExpl = new Explanation(0, "totalMatchScore, sum of:");

    // Explain the sum of the matches
    float totalScore = 0.0f;
    for (int i = 0; i < ordered.size(); i++) {
      SpansCell cell = (SpansCell)ordered.get(i);
      totalScore += cell.score;
      sumExpl.addDetail(cell.spans.explain());
    }
    sumExpl.setValue(totalScore);
    result.addDetail(sumExpl);

    // Explain the boost, if any.
    Explanation boostExpl = new Explanation(query.getBoost(), "boost");
    if (query.getBoost() != 1.0f)
      result.addDetail(boostExpl);

    // And explain the slop adjustment.
    int totalSlop = totalSlop();
    Explanation slopExpl = new Explanation(similarity.sloppyFreq(totalSlop),
                                           "sloppyFreq(slop=" + totalSlop +
                                           ")");
    result.addDetail(slopExpl);

    result.setValue(
      sumExpl.getValue() * boostExpl.getValue() * slopExpl.getValue());
    return result;
  }
}
