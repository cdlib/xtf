package org.apache.lucene.search.spans;

/**
 * Copyright 2005 The Apache Software Foundation
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.RecordingSearcher;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;

/** Runs a span query and scores the resulting spans, passing them to a
 *  SpanHitCollector if specified.
 */
public class SpanRecordingScorer extends Scorer 
{
  /** Sequence of spans  to score */
  private Spans spans;
  
  /** Weighted value for scoring this query */
  private float value;
  
  /** Field being queried (a Span query can only work on one field */
  private String field;
  
  /** Max # of spans to record (highest scoring are kept, others tossed) */
  private int maxSpans;

  /** true before {@link #next()} is called the first time */
  private boolean firstTime = true;
  
  /** true if there are more spans to process */
  private boolean more = true;

  /** Current document whose spans we're looking at */
  private int doc;
  
  /** Cumulative score for the current document */
  private float freq;

  /** Number of spans recorded for this document */
  int     nSpans;
  
  /** Last document that was scored and deduped */
  int     scoredDoc;
  
  /** Spans from last document scored */
  ArrayList scoredSpans;
  
  /** Total deduped, not limited by {@link #maxSpans} */
  int     totalDeduped;
  
  /** Array of recorded spans, in position order */
  Span[]  posOrder = new Span[0];
  
  /** Array of recorded spans, in descending score order */
  ScoreOrder[] scoreOrder;
  
  /** Set of all search terms */
  Set     terms;

  /**
   * Construct a recording scorer.
   * 
   * @param spans set of spans to process
   * @param weight weight of this query
   * @param similarity used to calculate scores, and compare queries
   * @param maxSpans max # of spans to collect
   * @throws IOException
   */
  SpanRecordingScorer(Spans spans, SpanWeight weight, Similarity similarity,
                      int maxSpans)
    throws IOException
  {
    super(similarity);

    this.spans = spans;
    this.maxSpans = maxSpans;

    value = weight.getValue();
    field = ((SpanQuery)weight.getQuery()).getField();

    // Register ourselves with the searcher, so it will know how to call us
    // to get the matching spans.
    //
    Searcher searcher = weight.getSearcher();
    if (searcher instanceof RecordingSearcher )
      ((RecordingSearcher)searcher).registerRecordingScorer(this);
    
    // Make a map of all the terms.
    Collection termColl = ((SpanQuery)weight.getQuery()).getTerms();
    terms = new HashSet(termColl.size()*2);
    for (Iterator iter = termColl.iterator(); iter.hasNext();) {
      String term = ((Term)iter.next()).text();
      terms.add(term);

      // If this is a probable bi-gram, add both the component terms to the
      // map as well.
      //
      int sepPos = term.indexOf( '~' );
      if( sepPos > 0 ) {
        terms.add(term.substring(0, sepPos));
        terms.add(term.substring(sepPos+1));
      }
    }
  }

  // inherit javadoc
  public boolean next() throws IOException {
    if (firstTime) {
      more = spans.next();
      firstTime = false;
    }

    return advance();
  }

  /** Worker method used by {@link #next()} and {@link #skipTo(int)}. */
  private final boolean advance() throws IOException {
    if (!more)
      return false;

    freq = 0.0f;
    doc = spans.doc();
    nSpans = 0;

    while (more && doc == spans.doc()) {
      final float score = spans.score();
      freq += score;

      if (nSpans == posOrder.length)
        expand();

      Span span = posOrder[nSpans++];
      span.start = spans.start();
      span.end   = spans.end();
      span.score = score;

      assert span.start < span.end : "Invalid span!";

      more = spans.next();
    }

    return more || freq != 0.0f;
  }

  /** Enlarge the arrays used to keep track of spans */
  private void expand()
  {
    int top = Math.max(10, posOrder.length * 3 / 2);

    Span[] newSpans = new Span[top];
    if( posOrder != null )
      System.arraycopy(posOrder, 0, newSpans, 0, nSpans);
    posOrder = newSpans;

    ScoreOrder[] newScore = new ScoreOrder[top];
    if( scoreOrder != null )
      System.arraycopy(scoreOrder, 0, newScore, 0, nSpans);
    scoreOrder = newScore;

    for (int i=nSpans; i<top; i++) {
      posOrder[i] = new Span();
      scoreOrder[i] = new ScoreOrder();
    }
  }

  public void recordSpans(int targetDoc, FieldSpans fieldSpans) {
    assert nSpans > 0 : "A valid span hit should have at least one span!";
    if( scoredDoc == targetDoc )
        fieldSpans.recordSpans(field, totalDeduped, scoredSpans, terms);
  }

  public int doc() { return doc; }

  public float score() throws IOException {

    // Deduplicate the spans we have, because recordSpans() will be called.
    scoredDoc = doc;
    scoredSpans = deduplicate();
    
    // doc norm has already been accounted for in SpanTermQuery
    return getSimilarity().tf(freq) * value;
  }

  public boolean skipTo(int target) throws IOException {
    if (doc == 0 || (more && target > spans.doc()))
      more = spans.skipTo(target);
    return advance();
  }

  public Explanation explain(final int doc) throws IOException {
    Explanation tfExplanation = new Explanation();

    skipTo(doc);

    float phraseFreq = (doc() == doc) ? freq : 0.0f;
    tfExplanation.setValue(getSimilarity().tf(phraseFreq));
    tfExplanation.setDescription("tf(phraseFreq=" + phraseFreq + ")");

    return tfExplanation;
  }

  /**
   * Takes the array of recorded spans and picks out the highest-ranking,
   * non-overlapping spans. It does this by taking the top ranked span and 
   * eliminating any that overlap from it, then repeating until all are
   * chosen or eliminated.
   */
  private ArrayList deduplicate() {
    // First, sort the spans in ascending order by start/end. This will make
    // it easy to detect overlap later.
    //
    Arrays.sort(posOrder, 0, nSpans, SpanPosComparator.theInstance);

    // Record the links in start/end order.
    for (int i=0; i<nSpans; i++) {
      scoreOrder[i].span = posOrder[i];
      scoreOrder[i].posOrder = i;
      scoreOrder[i].cancelled = false;
      scoreOrder[i].prevInPosOrder = 
          ((i-1) >= 0)     ? scoreOrder[i-1] : null;
      scoreOrder[i].nextInPosOrder = 
          ((i+1) < nSpans) ? scoreOrder[i+1] : null;
      scoreOrder[i].nextDeduped = null;
    }

    // Now make a second sort, this time by descending score.
    Arrays.sort(scoreOrder, 0, nSpans, theScoreComparator);

    // De-duplicate the score array, starting with the high scores first.
    int nDeduped = 0;
    totalDeduped = 0;
    ScoreOrder firstDeduped = null;
    ScoreOrder lastDeduped = null;
    ScoreOrder o;
    for (int i=0; i<nSpans; i++) {
      // Skip entries that have already been cancelled.
      if (scoreOrder[i].cancelled)
        continue;

      // We found an entry we want to keep. Link it into our list.
      totalDeduped++;
      if (nDeduped < maxSpans) {
        if (firstDeduped == null)
          firstDeduped = scoreOrder[i];
        else
          lastDeduped.nextDeduped = scoreOrder[i];
        lastDeduped = scoreOrder[i];
        nDeduped++;
      }

      // Cancel any overlapping entries before this one, stopping at
      // one that doesn't overlap (all those behind it won't either).
      //
      final Span scoreSpan = scoreOrder[i].span;
      o = scoreOrder[i].prevInPosOrder;
      while (o != null && o.span.end > scoreSpan.start) {
        o.cancelled = true;
        assert o.posOrder == 0 ||
               o.prevInPosOrder.posOrder == o.posOrder-1;
        o = o.prevInPosOrder;
      }

      // Similarly, cancel overlapping entries after this one.
      o = scoreOrder[i].nextInPosOrder; 
      while (o != null && o.span.start < scoreSpan.end) {
        o.cancelled = true;
        assert o.posOrder == nSpans-1 ||
               o.nextInPosOrder.posOrder == o.posOrder+1;
        o = o.nextInPosOrder;
      }
    }

    // Build the final result array.
    ArrayList list = new ArrayList(nDeduped);
    int rank = 0;
    float prevScore = Float.MAX_VALUE;
    for (o = firstDeduped; o != null; o = o.nextDeduped) {
      assert !o.cancelled : "kept span was cancelled";
      Span s = (Span) o.span.clone();
      assert s.score <= prevScore : "incorrect dedupe list linking";
      if (rank == nDeduped-1)
        assert o == lastDeduped;
      prevScore = s.score;
      s.rank = rank++;
      list.add(s);
    }
    assert rank == nDeduped : "incorrect dedupe list linking";
    return list;
  }

  /** Keeps track of the next and previous spans, in score order */
  private class ScoreOrder {
    Span       span;
    int        posOrder;
    boolean    cancelled;
    ScoreOrder nextInPosOrder;
    ScoreOrder prevInPosOrder;
    ScoreOrder nextDeduped;
  }

  /** Used to sort spans by descending score, then by position */
  private static class ScoreComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      Span s1 = ((ScoreOrder)o1).span;
      Span s2 = ((ScoreOrder)o2).span;
      if (s1.score < s2.score) return  1;
      if (s1.score > s2.score) return -1;
      if (s1.start == s2.start)
        return s2.end - s1.end; // If overlapping, sort longer spans first.
      return s1.start - s2.start;
    }
  }

  private static ScoreComparator theScoreComparator = new ScoreComparator();

}
