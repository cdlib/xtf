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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;

/** Calculates spans that match several queries "near" each other. In-order
 *  matches score higher than out-of-order matches.
 */
class OrNearSpans implements Spans 
{
  private SpanOrNearQuery query;
  private Similarity similarity;
  private int nClauses; // Number of original clauses
  private ArrayList cells; // Spans in position order
  private int slop; // from query
  private boolean penalizeOutOfOrder; // from query
  private boolean more = true; // true iff not done
  private boolean firstTime = true; // true before first next()
  private int matchDist; // Slop for current match
  private float matchTotalScore; // Sum of scores for current match
  private int matchEndCell; // Last cell # in current match
  private int matchNumCells; // Number of matching cells

  private class SpansCell 
  {
    Spans spans;
    int index;

    SpansCell(Spans spans, int index) {
      this.spans = spans;
      this.index = index;
    }

    final int doc() {
      return spans.doc();
    }

    final int start() {
      return spans.start();
    }

    final int end() {
      return spans.end();
    }

    final float score() {
      return spans.score();
    }

    final boolean next()
      throws IOException 
    {
      return spans.next();
    }

    final boolean skipTo(int doc)
      throws IOException 
    {
      return spans.skipTo(doc);
    }

    int compareTo(SpansCell other) 
    {
      if (doc() == other.doc()) 
      {
        if (start() == other.start()) {
          if (end() == other.end())
            return index - other.index;
          else
            return end() - other.end();
        }
        else {
          return start() - other.start();
        }
      }
      else
        return doc() - other.doc();
    }
  } // class SpansCell

  private static final Comparator cellComparator = new Comparator() 
  {
    public int compare(Object o1, Object o2) {
      return ((SpansCell)o1).compareTo((SpansCell)o2);
    }
  };

  public OrNearSpans(SpanOrNearQuery query, IndexReader reader,
                     Searcher searcher)
    throws IOException 
  {
    this.query = query;
    this.slop = query.getSlop();
    this.penalizeOutOfOrder = query.penalizeOutOfOrder();

    SpanQuery[] clauses = query.getClauses();
    nClauses = clauses.length;
    cells = new ArrayList(nClauses);
    for (int i = 0; i < nClauses; i++)
      cells.add(new SpansCell(clauses[i].getSpans(reader, searcher), i));

    similarity = searcher.getSimilarity();
  }

  public boolean next()
    throws IOException 
  {
    if (firstTime) {
      more = initCells(-1);
      firstTime = false;
    }
    else if (more) {
      if (!nextCell())
        more = advance(-1);
    }

    // All done.
    return more;
  }

  private boolean initCells(int skipTo)
    throws IOException 
  {
    // Init each cell. If it runs off the end, delete it.
    for (int i = 0; i < cells.size(); i++) 
    {
      SpansCell cell = (SpansCell)cells.get(i);
      boolean cellMore = (skipTo >= 0) ? cell.skipTo(skipTo) : cell.next();

      // If it ran off the end, remove it from the list.
      if (!cellMore) {
        cells.remove(i);
        --i;
      }
    }

    Collections.sort(cells, cellComparator);

    if (cells.isEmpty())
      return false;

    // Init scoring parameters.
    matchEndCell = 0;
    matchNumCells = 0;
    matchTotalScore = ((SpansCell)cells.get(0)).score();
    matchDist = 0;
    return true;
  } // initCells()

  private boolean advance(int skipTo)
    throws IOException 
  {
    // Advance the first cell. If it runs out, just remove it (the other cells
    // remain in their proper order.)
    //
    SpansCell cell = (SpansCell)cells.get(0);
    boolean cellMore = (skipTo >= 0) ? cell.skipTo(skipTo) : cell.next();
    if (cellMore) 
    {
      // Now put it in the right place in the position ordered array.
      int i;
      for (i = 0; i < cells.size() - 1; i++) 
      {
        SpansCell next = (SpansCell)cells.get(i + 1);
        if (cell.compareTo(next) < 0)
          break;

        cells.set(i, next);
      }
      cells.set(i, cell);
    }
    else {
      cells.remove(0);
      if (cells.isEmpty())
        return false;
    }

    // Init scoring parameters.
    matchEndCell = 0;
    matchNumCells = 0;
    matchTotalScore = ((SpansCell)cells.get(0)).score();
    matchDist = 0;

    // All done.
    return true;
  } // advance()

  // Attempt to extend the match by one more cell.
  private boolean nextCell() 
  {
    SpansCell prevCell = (SpansCell)cells.get(matchEndCell);

    while (true) 
    {
      // If we run out of cells, we can't extend.
      if ((matchEndCell + 1) == cells.size())
        return false;

      // Okay, get the next cell.
      SpansCell curCell = (SpansCell)cells.get(matchEndCell + 1);

      // If the cells are in different docs, they can't be connected.
      if (curCell.doc() != prevCell.doc())
        return false;

      // If the cells overlap, skip the new one.
      if (curCell.start() < prevCell.end()) {
        ++matchEndCell;
        continue;
      }

      // Add up the edit distance (accounting for out-of-order if requested).
      assert curCell.compareTo(prevCell) >= 0;
      int curDist;
      if (penalizeOutOfOrder && curCell.index < prevCell.index + 1)
        curDist = curCell.end() - prevCell.start();
      else
        curDist = curCell.start() - prevCell.end();
      if (curDist < 0)
        curDist = -curDist;
      matchDist += curDist;

      // If we're beyond the maximum allowable slop, stop here.
      if (matchDist > slop)
        return false;

      // Cool. This looks like the place to be.
      matchTotalScore += curCell.score();
      ++matchEndCell;
      ++matchNumCells;
      return true;
    }
  } // nextCell()

  public boolean skipTo(int target)
    throws IOException 
  {
    if (firstTime) 
    {
      more = initCells(target);
      firstTime = false;
    }
    else if (more) {
      // Skip as needed
      while (more && ((SpansCell)cells.get(0)).doc() < target)
        more = advance(target);
    }

    return more;
  }

  public int doc() {
    return ((SpansCell)cells.get(0)).doc();
  }

  public int start() {
    return ((SpansCell)cells.get(0)).start();
  }

  public int end() {
    return ((SpansCell)cells.get(matchEndCell)).end();
  }

  public float score() 
  {
    // Calculate the score for this span. The bulk of the score comes from
    // the sum of the matched sub-spans. This guarantees (assuming all term
    // scores are equal) that matches with more terms will always score
    // higher than those with fewer terms.
    //
    float coordFactor = (float)(matchNumCells + 1) / (nClauses + 1);

    // But a bit of the score comes from the edit distance involved in this
    // match.
    //
    float distFactor = similarity.sloppyFreq(matchDist) / (nClauses + 1);

    // Combine these together with the boost to make the final score.
    return matchTotalScore * (coordFactor + distFactor) * query.getBoost();
  }

  public String toString() {
    return "spans(" + query.toString() + ")@" +
           (firstTime ? "START"
            : (more ? (doc() + ":" + start() + "-" + end()) : "END"));
  }

  public Explanation explain()
    throws IOException 
  {
    Explanation result = new Explanation(0,
                                         "weight(" + toString() +
                                         "), product of:");

    // Explain the total of the matches (simplify if only one match)
    Explanation totalExpl;
    if (matchEndCell == 0)
      totalExpl = ((SpansCell)cells.get(0)).spans.explain();
    else 
    {
      float totalScore = 0.0f;
      totalExpl = new Explanation(0, "totalMatchScore, sum of:");
      SpansCell prevCell = null;
      for (int i = 0; i <= matchEndCell; i++) {
        SpansCell cell = (SpansCell)cells.get(i);
        if (prevCell != null && cell.start() < prevCell.end())
          continue;
        totalScore += cell.score();
        totalExpl.addDetail(cell.spans.explain());
        prevCell = cell;
      }
      totalExpl.setValue(totalScore);
    }
    result.addDetail(totalExpl);

    // Explain the boost, if any.
    Explanation boostExpl = new Explanation(query.getBoost(), "boost");
    if (boostExpl.getValue() != 1.0f)
      result.addDetail(boostExpl);

    // Explain the distance factor
    Explanation distExpl = new Explanation(0,
                                           "distFactor(sloppyFreq/" +
                                           (nClauses + 1) + ")");
    Explanation slopExpl = new Explanation(similarity.sloppyFreq(matchDist),
                                           "sloppyFreq(slop=" + matchDist +
                                           ")");
    distExpl.addDetail(slopExpl);
    distExpl.setValue(slopExpl.getValue() / (nClauses + 1));

    // Explain the coordination factor
    Explanation coordExpl = new Explanation(
      (float)(matchNumCells + 1) / (nClauses + 1),
      "coordFactor(" + (matchNumCells + 1) + "/" + (nClauses + 1) + ")");

    // Explain the combined factors.
    Explanation combinedFactorsExpl = new Explanation(
      distExpl.getValue() + coordExpl.getValue(),
      "combinedFactors = sum of");
    combinedFactorsExpl.addDetail(distExpl);
    combinedFactorsExpl.addDetail(coordExpl);
    result.addDetail(combinedFactorsExpl);

    result.setValue(
      totalExpl.getValue() * boostExpl.getValue() * combinedFactorsExpl.getValue());
    return result;
  }
}
