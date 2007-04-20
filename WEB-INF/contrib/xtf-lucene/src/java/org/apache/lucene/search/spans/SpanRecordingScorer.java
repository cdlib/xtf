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
 *
 * Acknowledgements:
 *
 * A significant amount of new and/or modified code in this module
 * was made possible by a grant from the Andrew W. Mellon Foundation,
 * as part of the Melvyl Recommender Project.
 */
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.RecordingSearcher;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;

/** Runs a span query and scores the resulting spans, passing them to a
 *  SpanHitCollector if specified.
 */
public class SpanRecordingScorer extends SpanScorer 
{
  /** Field being queried (a Span query can only work on one field */
  private String field;

  /** Max # of spans to record (highest scoring are kept, others tossed) */
  private int maxSpans;

  /** Number of spans recorded for this document */
  int nSpans;

  /** Last document that was scored */
  int scoredDoc;

  /** Total deduped, not limited by {@link SpanRecordingScorer#maxSpans} */
  int totalDeduped;

  /** Array of recorded spans, in position order */
  Span[] posOrder = new Span[0];

  /** Array to de-dupe, in position order */
  Span[] toDedupe = new Span[0];

  /** How many spans to de-duplicate */
  int nToDedupe;

  /** Set of all search terms */
  Set terms;

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
    super(spans, weight, similarity);

    this.spans = spans;
    this.maxSpans = maxSpans;

    value = weight.getValue();
    field = ((SpanQuery)weight.getQuery()).getField();

    // Register ourselves with the searcher, so it will know how to call us
    // to get the matching spans.
    //
    Searcher searcher = weight.getSearcher();
    if (searcher instanceof RecordingSearcher)
      ((RecordingSearcher)searcher).registerRecordingScorer(this);

    // Make a map of all the terms.
    Collection termColl = ((SpanQuery)weight.getQuery()).getTerms();
    terms = new HashSet(termColl.size() * 2);
    for (Iterator iter = termColl.iterator(); iter.hasNext();) 
    {
      String term = ((Term)iter.next()).text();
      terms.add(term);

      // If this is a probable bi-gram, add both the component terms to the
      // map as well.
      //
      int sepPos = term.indexOf('~');
      if (sepPos > 0) {
        terms.add(term.substring(0, sepPos));
        terms.add(term.substring(sepPos + 1));
      }
    }
  }

  /** 
   * Worker method used by {@link SpanRecordingScorer#next()} and 
   * {@link SpanRecordingScorer#skipTo(int)}. 
   */
  protected boolean advance()
    throws IOException 
  {
    if (!more)
      return false;

    freq = 0.0f;
    doc = spans.doc();
    nSpans = 0;

    while (more && doc == spans.doc()) 
    {
      final float score = spans.score();
      freq += score;

      if (nSpans == posOrder.length)
        expand();

      Span span = posOrder[nSpans++];
      span.start = spans.start();
      span.end = spans.end();
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
    if (posOrder != null)
      System.arraycopy(posOrder, 0, newSpans, 0, nSpans);
    posOrder = newSpans;

    Span[] newSpans2 = new Span[top];
    if (toDedupe != null)
      System.arraycopy(toDedupe, 0, newSpans2, 0, nSpans);
    toDedupe = newSpans2;

    for (int i = nSpans; i < top; i++) {
      posOrder[i] = new Span();
      toDedupe[i] = new Span();
    }
  }

  public int getSpanDoc() {
    return scoredDoc;
  }

  public String getField() {
    return field;
  }

  public int getSpanCount() {
    return nToDedupe;
  }

  public Span[] getSpans() {
    return toDedupe;
  }

  public int getMaxSpans() {
    return maxSpans;
  }

  public Set getTerms() {
    return terms;
  }

  public float score()
    throws IOException 
  {
    // Remember that this scorer is part of the result, so we know to record
    // spans for it.
    //
    if (scoredDoc != doc) 
    {
      scoredDoc = doc;

      // Save the current spans in a special area awaiting deduplication
      // (put off deduplication until actually requested.)
      //
      Span[] tmp = posOrder;
      posOrder = toDedupe;
      toDedupe = tmp;
      nToDedupe = nSpans;
    }

    // Calculate the score as usual.
    return super.score();
  }
}
