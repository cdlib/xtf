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
 */

import java.io.IOException;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.util.PriorityQueue;

/** Calculates spans that match several queries "near" each other. In-order
 *  matches score higher than out-of-order matches.
 */
class NearSpans implements Spans {
  private SpanNearQuery query;

  private Similarity similarity;
  
  private List ordered = new ArrayList();         // spans in query order
  private int slop;                               // from query
  private boolean inOrder;                        // from query

  private SpansCell first;                        // linked list of spans
  private SpansCell last;                         // sorted by doc only

  private int totalLength;                        // sum of current lengths
  private float totalScore;                       // sum of current scores
  private int totalSlop;                          // sloppiness of current match

  private CellQueue queue;                        // sorted queue of spans
  private SpansCell max;                          // max element in queue

  private boolean more = true;                    // true iff not done
  private boolean firstTime = true;               // true before first next()

  private class CellQueue extends PriorityQueue {
    public CellQueue(int size) {
      initialize(size);
    }
    
    protected final boolean lessThan(Object o1, Object o2) {
      SpansCell spans1 = (SpansCell)o1;
      SpansCell spans2 = (SpansCell)o2;
      if (spans1.doc() == spans2.doc()) {
        if (spans1.start() == spans2.start()) {
          if (spans1.end() == spans2.end()) {
            return spans1.index > spans2.index;
          } else {
            return spans1.end() < spans2.end();
          }
        } else {
          return spans1.start() < spans2.start();
        }
      } else {
        return spans1.doc() < spans2.doc();
      }
    }
  }


  /** Wraps a Spans, and can be used to form a linked list. */
  private class SpansCell implements Spans {
    private Spans spans;
    private SpansCell next;
    private int length = -1;
    private float score;
    private int index;

    public SpansCell(Spans spans, int index) {
      this.spans = spans;
      this.index = index;
    }

    public boolean next() throws IOException {
      if (length != -1) {                         // subtract old length
        totalLength -= length;
        totalScore -= score;
      }

      boolean more = spans.next();                // move to next

      if (more) {
        length = end() - start();                 // compute new length
        totalLength += length;                    // add new length to total
        score = spans.score();
        totalScore += score;
        
        if (max == null || doc() > max.doc() ||   // maintain max
            (doc() == max.doc() && end() > max.end()))
          max = this;
      }

      return more;
    }

    public boolean skipTo(int target) throws IOException {
      if (length != -1) {                         // subtract old length
        totalLength -= length;
        totalScore -= score;
      }

      boolean more = spans.skipTo(target);        // skip

      if (more) {
        length = end() - start();                 // compute new length
        totalLength += length;                    // add new length to total
        score = spans.score();
        totalScore += score;

        if (max == null || doc() > max.doc() ||   // maintain max
            (doc() == max.doc() && end() > max.end()))
          max = this;
      }

      return more;
    }

    public int doc() { return spans.doc(); }
    public int start() { return spans.start(); }
    public int end() { return spans.end(); }
    public float score() { throw new UnsupportedOperationException(); } 
    public Explanation explain() throws IOException { return spans.explain(); }
    public void collectTerms(Set terms) { }
    
    public String toString() { return spans.toString() + "#" + index; }
  }

  public NearSpans(SpanNearQuery query, IndexReader reader, Searcher searcher)
    throws IOException {
    this.query = query;
    this.slop = query.getSlop();
    this.inOrder = query.isInOrder();

    SpanQuery[] clauses = query.getClauses();     // initialize spans & list
    queue = new CellQueue(clauses.length);
    for (int i = 0; i < clauses.length; i++) {
      SpansCell cell =                            // construct clause spans
        new SpansCell(clauses[i].getSpans(reader, searcher), i);
      ordered.add(cell);                          // add to ordered
    }
    
    similarity = searcher.getSimilarity();
  }

  public boolean next() throws IOException {
    if (firstTime) {
      initList(true);
      listToQueue();                              // initialize queue
      firstTime = false;
    } else if (more) {
      more = min().next();                        // trigger further scanning
      if (more)
        queue.adjustTop();                        // maintain queue
    }

    while (more) {

      boolean queueStale = false;

      // Get rid of cached slop value.
      totalSlop = -1;

      if (min().doc() != max.doc()) {             // maintain list
        queueToList();
        queueStale = true;
      }

      // skip to doc w/ all clauses

      while (more && first.doc() < last.doc()) {
        more = first.skipTo(last.doc());          // skip first upto last
        firstToLast();                            // and move it to the end
        queueStale = true;
      }

      if (!more) return false;

      // found doc w/ all clauses

      if (queueStale) {                           // maintain the queue
        listToQueue();
        queueStale = false;
      }

      if (atMatch())
        return true;
      
      // trigger further scanning
      if (inOrder && checkSlop()) {
        /* There is a non ordered match within slop and an ordered match is needed. */
        more = firstNonOrderedNextToPartialList();
        if (more) {
          partialListToQueue();                            
        }
      } else {
        more = min().next();
        if (more) {
          queue.adjustTop();                      // maintain queue
        }
      }
    }
    return false;                                 // no more matches
  }

  public boolean skipTo(int target) throws IOException {
    if (firstTime) {                              // initialize
      initList(false);
      for (SpansCell cell = first; more && cell!=null; cell=cell.next) {
        more = cell.skipTo(target);               // skip all
      }
      if (more) {
        listToQueue();
      }
      firstTime = false;
    } else {                                      // normal case
      while (more && min().doc() < target) {      // skip as needed
        more = min().skipTo(target);
        if (more)
          queue.adjustTop();
      }
    }
    
    // Get rid of cached slop value.
    totalSlop = -1;
    
    if (more) {

      if (atMatch())                              // at a match?
        return true;

      return next();                              // no, scan
    }

    return false;
  }

  private SpansCell min() { return (SpansCell)queue.top(); }

  public int doc() { return min().doc(); }
  public int start() { return min().start(); }
  public int end() { return max.end(); }
  public float score() { return totalScore *
                                query.getBoost() *
                                similarity.sloppyFreq(totalSlop()); 
  }

  public String toString() {
    return "spans("+query.toString()+")@"+
      (firstTime?"START":(more?(doc()+":"+start()+"-"+end()):"END"));
  }

  private void initList(boolean next) throws IOException {
    for (int i = 0; more && i < ordered.size(); i++) {
      SpansCell cell = (SpansCell)ordered.get(i);
      if (next)
        more = cell.next();                       // move to first entry
      if (more) {
        addToList(cell);                          // add to list
      }
    }
  }

  private void addToList(SpansCell cell) {
    if (last != null) {			  // add next to end of list
      last.next = cell;
    } else
      first = cell;
    last = cell;
    cell.next = null;
  }

  private void firstToLast() {
    last.next = first;			  // move first to end of list
    last = first;
    first = first.next;
    last.next = null;
  }

  private void queueToList() {
    last = first = null;
    while (queue.top() != null) {
      addToList((SpansCell)queue.pop());
    }
  }
  
  private boolean firstNonOrderedNextToPartialList() throws IOException {
    /* Creates a partial list consisting of first non ordered and earlier.
     * Returns first non ordered .next().
     */
    last = first = null;
    int orderedIndex = 0;
    while (queue.top() != null) {
      SpansCell cell = (SpansCell)queue.pop();
      addToList(cell);
      if (cell.index == orderedIndex) {
        orderedIndex++;
      } else {
        return cell.next();
        // FIXME: continue here, rename to eg. checkOrderedMatch():
        // when checkSlop() and not ordered, repeat cell.next().
        // when checkSlop() and ordered, add to list and repeat queue.pop()
        // without checkSlop(): no match, rebuild the queue from the partial list.
        // When queue is empty and checkSlop() and ordered there is a match.
      }
    }
    throw new RuntimeException("Unexpected: ordered");
  }

  private void listToQueue() {
    queue.clear(); // rebuild queue
    partialListToQueue();
  }

  private void partialListToQueue() {
    for (SpansCell cell = first; cell != null; cell = cell.next) {
      queue.put(cell);                      // add to queue from list
    }
  }

  private boolean atMatch() {
    return (min().doc() == max.doc())
          && checkSlop()
          && (!inOrder || matchIsOrdered());
  }
  
  private boolean checkSlop() {
    int matchLength = max.end() - min().start();
    
    // Is a match even possible?
    if (matchLength - totalLength > slop)
      return false;
    
    // Do a more thorough slop calculation.
    if (totalSlop() > slop)
      return false;
    
    return true;
  }

  private boolean matchIsOrdered() {
    int lastStart = -1;
    for (int i = 0; i < ordered.size(); i++) {
      int start = ((SpansCell)ordered.get(i)).start();
      if (!(start > lastStart))
        return false;
      lastStart = start;
    }
    return true;
  }

  private int totalSlop() {
      
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
      lastEnd   = end;
    } // for i
    
    return totalSlop = matchSlop;
  }
  
  public Explanation explain() throws IOException {
    
    Explanation result = new Explanation(0, "weight("+toString()+"), product of:");
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
        "sloppyFreq(slop=" + totalSlop + ")");
    result.addDetail(slopExpl);
    
    result.setValue(sumExpl.getValue() *
                    boostExpl.getValue() *
                    slopExpl.getValue());
    return result;
  }
}
