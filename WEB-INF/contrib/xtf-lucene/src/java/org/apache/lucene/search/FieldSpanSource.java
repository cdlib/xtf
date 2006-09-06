package org.apache.lucene.search;

/*
 * Copyright (c) 2005, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, 
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, 
 *   this list of conditions and the following disclaimer in the documentation 
 *   and/or other materials provided with the distribution.
 * - Neither the name of the University of California nor the names of its
 *   contributors may be used to endorse or promote products derived from this 
 *   software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.search.spans.FieldSpans;
import org.apache.lucene.search.spans.Span;
import org.apache.lucene.search.spans.SpanPosComparator;
import org.apache.lucene.search.spans.SpanRecordingScorer;

/**
 * This class, an instance of which is passed to a SpanHitCollector for each
 * hit, retrieves FieldSpans when requested. This is performed lazily so that
 * it can be avoided for hits that don't make the grade.
 */
public class FieldSpanSource 
{
  String[] fields;
  SpanRecordingScorer[][] scorersPerField;
  ScoreOrder[] scoreOrder = new ScoreOrder[0];
  int curDoc = -1;
  
  /**
   * Package-private on purpose. Should only be created by RecordingSearcher.
   */
  FieldSpanSource(SpanRecordingScorer[] scorers) 
  {
    // Make a list of scorers per field
    HashMap map = new HashMap();
    for (int i = 0; i < scorers.length; i++ ) {
      String field = scorers[i].getField();
      ArrayList list = (ArrayList) map.get(field);
      if (list == null) {
        list = new ArrayList();
        map.put(field, list);
      }
      list.add(scorers[i]);
    }
    
    // Convert the map to handy arrays.
    fields = (String[]) map.keySet().toArray(new String[map.size()]);
    scorersPerField = new SpanRecordingScorer[fields.length][];
    for (int i = 0; i < fields.length; i++) {
      ArrayList list = (ArrayList) map.get(fields[i]);
      scorersPerField[i] = (SpanRecordingScorer[]) list.toArray(
        new SpanRecordingScorer[list.size()]);
    }
  } // constructor
  
  /**
   * Retrieve the spans for the given document.
   * 
   * @param doc Document to get spans for. Typically, the FieldSpanSource can
   *            only get spans for the most recent document collected.
   * @return    Recorded spans for the document.
   */
  public synchronized FieldSpans getSpans(int doc) {
    if (doc != curDoc)
      throw new UnsupportedOperationException( "FieldSpanSource can only retrieve spans for current document" );
    
    // Process the spans for each field
    FieldSpans ret = new FieldSpans();
    for (int i = 0; i < fields.length; i++) 
      addSpans(doc, fields[i], scorersPerField[i], ret);
    return ret;
  }

  /**
   * For the given field and list of scorers, calculate (and deduplicate if
   * necessary) the spans for that field.
   * 
   * @param doc       Document for which spans are being recorded
   * @param field     The field being considered
   * @param scorers   All scorers for that field
   * @param out       Where to store the resulting spans.
   */
  private void addSpans(int doc,
                        String field, 
                        SpanRecordingScorer[] scorers, 
                        FieldSpans out)
  {
    // Figure out how many spans total there are for this field. Also
    // accumulate the set of terms.
    //
    int nToDedupe = 0;
    int maxSpans = 0;
    Set terms = null;
    for (int i = 0; i < scorers.length; i++) 
    {
      // Skip scorers that didn't record for this doc.
      if (scorers[i].getSpanDoc() != doc)
        continue;
    
      // Count spans.
      nToDedupe += scorers[i].getSpanCount();
  
      // Track the max # of spans to actually record (which may be less than
      // the number of spans for the doc.)
      //
      maxSpans = Math.max(maxSpans, scorers[i].getMaxSpans());
      
      // Accumulate the set of terms matched by the queries
      if (terms == null)
        terms = scorers[i].getTerms();
      else {
        Set newTerms = new HashSet();
        newTerms.addAll(terms);
        newTerms.addAll(scorers[i].getTerms());
        terms = newTerms;
      }
    }
    
    // No spans? No work to do.
    if (nToDedupe == 0)
      return;
    
    // Collect the raw spans together.
    Span[] toDedupe = new Span[nToDedupe];
    int start = 0;
    for (int i = 0; i < scorers.length; i++) {
      if (scorers[i].getSpanDoc() != doc)
        continue;
    
      int count = scorers[i].getSpanCount();
      System.arraycopy(scorers[i].getSpans(), 0, toDedupe, start, count);
      start += count;
    }
    assert start == nToDedupe : "internal error: mis-counted spans";
    
    // Sort the spans in ascending order by start/end.
    Arrays.sort(toDedupe, SpanPosComparator.theInstance);
    
    // For reference during overlap checks, determine the length of the
    // longest span.
    //
    int longestSpan = 0;
    for(int i = 0; i<nToDedupe; i++)
      longestSpan = Math.max( longestSpan, toDedupe[i].end - toDedupe[i].start );
    
    // Expand the score order array if we need to.
    if (scoreOrder.length < nToDedupe) {
      ScoreOrder[] newScoreOrder = new ScoreOrder[nToDedupe+5];
      System.arraycopy(scoreOrder, 0, newScoreOrder, 0, scoreOrder.length);
      for (int i=scoreOrder.length; i<newScoreOrder.length; i++)
          newScoreOrder[i] = new ScoreOrder();
      scoreOrder = newScoreOrder;
    }
    
    // Record the links in start/end order.
    for (int i=0; i<nToDedupe; i++) {
      scoreOrder[i].span = toDedupe[i];
      scoreOrder[i].posOrder = i;
      scoreOrder[i].cancelled = false;
      scoreOrder[i].prevInPosOrder = 
          ((i-1) >= 0)        ? scoreOrder[i-1] : null;
      scoreOrder[i].nextInPosOrder = 
          ((i+1) < nToDedupe) ? scoreOrder[i+1] : null;
      scoreOrder[i].nextDeduped = null;
    }

    // Now make a second sort, this time by descending score.
    Arrays.sort(scoreOrder, 0, nToDedupe, theScoreComparator);
    
    // De-duplicate the score array, starting with the high scores first.
    int nDeduped = 0;
    int totalDeduped = 0;
    ScoreOrder firstDeduped = null;
    ScoreOrder lastDeduped = null;
    ScoreOrder o;
    for (int i=0; i<nToDedupe; i++) 
    {
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
      // one that can't overlap because it's beyond the length of the
      // longest span. 
      //
      // You might ask, "why not just stop when you hit the first 
      // non-overlapping span?" Good question grasshopper. The answer
      // is that, since the spans are sorted by ascending start,
      // there may be a big span further back that overlaps, and we
      // have no way of knowing.
      //
      final Span scoreSpan = scoreOrder[i].span;
      o = scoreOrder[i].prevInPosOrder;
      while (o != null && (o.span.start + longestSpan) > scoreSpan.start ) {
        assert o.span.start <= scoreSpan.start;
        if (o.span.end > scoreSpan.start)
          o.cancelled = true;
        assert o.posOrder == 0 ||
               o.prevInPosOrder.posOrder == o.posOrder-1;
        o = o.prevInPosOrder;
      }

      // Cancel overlapping entries after this one. Since the spans
      // are sorted by ascending start pos, we can stop at the first
      // non-overlapping span.
      //
      o = scoreOrder[i].nextInPosOrder; 
      while (o != null && o.span.start < scoreSpan.end) {
        o.cancelled = true;
        assert o.posOrder == nToDedupe-1 ||
               o.nextInPosOrder.posOrder == o.posOrder+1;
        o = o.nextInPosOrder;
      }
    }

    // Build the final result array.
    Span[] outSpans = new Span[nDeduped];
    int rank = 0;
    float prevScore = Float.MAX_VALUE;
    int i = 0;
    for (o = firstDeduped; o != null; o = o.nextDeduped) {
      assert !o.cancelled : "kept span was cancelled";
      Span s = (Span) o.span.clone();
      assert s.score <= prevScore : "incorrect dedupe list linking";
      if (rank == nDeduped-1)
        assert o == lastDeduped;
      prevScore = s.score;
      s.rank = rank++;
      outSpans[i++] = s;
    }
    assert rank == nDeduped : "incorrect dedupe list linking";
    
    // Apply a final sort by position.
    Arrays.sort(outSpans, SpanPosComparator.theInstance);
    
    // And output it.
    out.recordSpans(field, totalDeduped, outSpans, terms);
    
  } // deduplicate()

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

} // class FieldSpanSource
