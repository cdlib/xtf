package org.apache.lucene.mark;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.apache.lucene.search.spans.FieldSpans;
import org.apache.lucene.search.spans.Span;
import org.apache.lucene.search.spans.SpanPosComparator;

/**
 * Workhorse class that handles marking hits, context surrounding hits, and
 * search terms.
 *
 * <p>Created: Dec 26, 2004</p>
 *
 * @author  Martin Haye
 * @version $Id: ContextMarker.java,v 1.1 2005-09-12 19:06:11 mhaye Exp $
 */
public class ContextMarker {
  
  /** 
   * The following modes can be used for term marking:
   * 
   * <p>MARK_NO_TERMS: Terms are not marked</p>
   * 
   * <p>MARK_SPAN_TERMS: Search terms are marked only within span hits.</p>
   * 
   * <p>MARK_CONTEXT_TERMS: Search terms are marked within span hits and,
   * if found, within the context surrounding those hits.</p>
   * 
   * <p>MARK_ALL_TERMS: Search terms are marked wherever they are found.</p>
   */
  public static final int MARK_NO_TERMS       = 0;
  
  /** See {@link #MARK_NO_TERMS} */
  public static final int MARK_SPAN_TERMS     = 1;

  /** See {@link #MARK_NO_TERMS} */
  public static final int MARK_CONTEXT_TERMS  = 2;

  /** See {@link #MARK_NO_TERMS} */
  public static final int MARK_ALL_TERMS      = 3;
  
  /** Target size (in chars) of the context surrounding each hit */
  private int maxContext;

  /** Iterator used for locating the start of the hit/context */
  private WordIter iter0;

  /** Iterator used for locating the end of the hit/context */
  private WordIter iter1;

  /** Client instance which receives the resulting marks */
  private MarkCollector collector;

  /** Set of search terms to mark */
  private Set terms;

  /** Set of stop-words to avoid marking outside of hits */
  private Set stopSet;

  /** Whether to mark terms inside/outside hits, context, etc. See
   *  {@link SpanDocument#MARK_SPAN_TERMS}, etc.
   */
  private int termMode;

  /** Word position up to which we've marked all terms */
  private int termsMarkedPos = 0;

  /** Used to temporary position storage */
  private MarkPos tmpPos;

  /** End of the previous context */
  private int prevEndWord = -1;

  /**
   * Mark context, spans, and terms within the given field of this document.
   * Context around each hit will be up to 80 characters (including the
   * text of the hit itself). Search terms will only be marked within hits.
   * If you would like to override these defaults, use one of the other
   * variations of this method. 
   * 
   * @param field       field name to mark
   * @param fieldSpans  spans to mark with
   * @param collector   collector to receive the marks
   */
  public void markField(String field, FieldSpans fieldSpans, 
                        MarkCollector collector)
  {
    BasicWordIter wordIter = new BasicWordIter();
    markField(fieldSpans, field, wordIter, 80, 
              MARK_SPAN_TERMS, null, collector);
  }
  
  /**
   * Mark context, spans, and terms within the given field of this document.
   * 
   * @param field       field name to mark
   * @param iter        iterator over the words in the field
   * @param maxContext  target number of characters for context around
   *                    each hit (including the text of the hit itself.)
   *                    80 is often a good choice. Specify zero to turn off
   *                    context marking.
   * @param termMode    what areas to mark hits - see {@link #MARK_NO_TERMS}.
   * @param stopSet     set of stop words to avoid marking outside hits
   * @param collector   collector to receive the marks
   */
  public void markField(String field, FieldSpans fieldSpans,
                        WordIter iter, int maxContext,
                        int termMode, Set stopSet, MarkCollector collector)
  {
    markField(fieldSpans, field, iter, maxContext, 
              termMode, stopSet, collector);
  }
  
  /**
   * Mark context, spans, and terms a field of data.
   * 
   * @param field       field name to mark
   * @param iter        iterator over the words in the field
   * @param maxContext  target number of characters for context around
   *                    each hit (including the text of the hit itself.)
   *                    80 is often a good choice. Specify zero to turn off
   *                    context marking.
   * @param termMode    what areas to mark hits - see {@link #MARK_NO_TERMS}.
   * @param stopSet     set of stop words to avoid marking outside hits
   * @param collector   collector to receive the marks
   */
  public static void markField(FieldSpans fieldSpans, 
                               String field, WordIter iter, int maxContext,
                               int termMode, Set stopSet, 
                               MarkCollector collector)
  {
    if (termMode < MARK_NO_TERMS || termMode > MARK_ALL_TERMS)
      throw new IllegalArgumentException( "Invalid termMode" );
    
    // No field spans? No marking to do.
    if( fieldSpans == null ) {
      collector.beginField(iter.getPos(WordIter.FIELD_START));
      collector.endField(iter.getPos(WordIter.FIELD_END));
      return;
    }
    
    // Get the map of terms for the field.
    Set terms = fieldSpans.getTerms(field);
    
    // Optimization: if there are no terms to mark and no spans, we've got
    // nothing left to do.
    //
    ArrayList origSpans = fieldSpans.getSpans(field);
    if ((terms == null || terms.isEmpty() || termMode == MARK_NO_TERMS) &&
        (origSpans == null || origSpans.isEmpty()))
    {
      collector.beginField(iter.getPos(WordIter.FIELD_START));
      collector.endField(iter.getPos(WordIter.FIELD_END));
      return;
    }

    // Make a list of the spans sorted by position, so we can tick them off 
    // as we roll along.
    //
    if (origSpans == null)
        origSpans = new ArrayList(0);
    ArrayList posOrderSpans = (ArrayList)(origSpans.clone());
    Collections.sort(posOrderSpans, SpanPosComparator.theInstance);
    
    // At the moment, the highest span score is always at the start of the
    // list. However, that might change in the future. Why risk that bug,
    // when it's easy to do it the right way now.
    //
    float maxScore = 0.0f;
    Iterator spanIter;
    for (spanIter = posOrderSpans.iterator(); spanIter.hasNext(); )
      maxScore = Math.max(maxScore, ((Span)spanIter.next()).score);
    
    // Now normalize all the scores.
    for (spanIter = posOrderSpans.iterator(); spanIter.hasNext(); )
      ((Span)spanIter.next()).score /= maxScore;
    
    // We need the big guns for context marking.
    ContextMarker marker = new ContextMarker(maxContext, 
                                             termMode, terms, stopSet,
                                             iter, collector);
    marker.mark(posOrderSpans, maxContext);
  }

  /** Construct a new marker */
  public ContextMarker(int maxContext, int termMode, Set terms, Set stopSet,
                       WordIter wordIter, MarkCollector collector) 
  {
    this.maxContext = maxContext;
    this.termMode = termMode;
    this.terms = terms;
    this.stopSet = stopSet;
    this.iter0 = wordIter;
    this.collector = collector;
  }

  /**
   * Mark a series of spans.
   *  
   * @param posOrderSpans   Spans to mark, in ascending position order.
   * @param maxContext      Target # of chars for context around hits 
   *                        (0 for none)
   */
  public void mark(ArrayList posOrderSpans, int maxContext) {
    
    // Create holders for start/end of context.
    MarkPos contextStart = null;
    MarkPos contextEnd = null;
    if (maxContext > 0 && !posOrderSpans.isEmpty()) {
      iter0.seekFirst(((Span) posOrderSpans.get(0)).start, true);
      contextStart = iter0.getPos(WordIter.TERM_START);
      contextEnd = (MarkPos) contextStart.clone();
    }

    // Mark the start of the field.
    collector.beginField(iter0.getPos(WordIter.FIELD_START));

    // Process each span in turn.
    Iterator posSpanIter = posOrderSpans.iterator();
    Span posSpan = posSpanIter.hasNext() ? (Span) posSpanIter.next() : null;
    while (posSpan != null) {
      Span nextSpan = posSpanIter.hasNext() ? (Span) posSpanIter.next() : null;

      // Find the start and end of the context surrounding the span (if
      // context is enabled.)
      //
      if (maxContext > 0)
        findContext(posSpan, nextSpan, contextStart, contextEnd);

      // Now mark the hit.
      emitMarks(posSpan, contextStart, contextEnd);

      // Onward...
      posSpan = nextSpan;
    }

    // If we're marking terms outside the context, finish them off.
    if (termMode >= MARK_ALL_TERMS)
      markTerms(iter0, termsMarkedPos, Integer.MAX_VALUE, false);

    // And finally, mark the start of the field.
    collector.endField(iter0.getPos(WordIter.FIELD_END));
  }

  /**
   * Locate the start and end of context for the given hit.  
   * 
   * @param posSpan       hit for which to find context
   * @param nextSpan      following hit (or null if none)
   * @param contextStart  OUT: start of context
   * @param contextEnd    OUT: end of context
   */
  void findContext(Span posSpan, Span nextSpan, 
                   MarkPos contextStart, MarkPos contextEnd) 
  {
    // Position our iterators at the start and end of the span. For the start,
    // be sure to set the 'force' flag to speed by any soft barriers.
    //
    iter0.seekFirst(posSpan.start, true);
    iter1 = (WordIter) iter0.clone();
    iter1.seekLast(posSpan.end - 1, false);

    // Sanity check (but only if assertions are enabled.)
    boolean assertionsEnabled = false;
    assert assertionsEnabled = true;
    if (assertionsEnabled && terms != null) {
      String startTerm = iter0.term();
      if (!terms.contains(startTerm)) {
        ArrayList sortTerms = new ArrayList(terms.size());
        sortTerms.addAll(terms);
        Collections.sort(sortTerms);
        System.out.println("Terms: " + sortTerms.toString());
        assert false : "first term in span not in term map - perhaps wrong analyzer was used?";
      }

      String endTerm = iter1.term();
      if (!terms.contains(endTerm)) {
        ArrayList sortTerms = new ArrayList(terms.size());
        sortTerms.addAll(terms);
        Collections.sort(sortTerms);
        System.out.println("Terms: " + sortTerms.toString());
        assert false : "last term in span not in term map - perhaps wrong analyzer was used?";
      }
    }

    // Record the starting and ending positions as the first attempt at the
    // context start/end positions.
    //
    iter0.getPos(contextStart, WordIter.TERM_START);
    iter1.getPos(contextEnd, WordIter.TERM_END_PLUS);

    // Now alternately add context at the start and at the end.
    int addedToStart = 0;
    int addedToEnd = 0;
    boolean more0 = true;
    boolean more1 = true;
    int spanChars = contextStart.countTextTo(contextEnd);

    while ((more0 || more1)
        && (spanChars + addedToStart + addedToEnd) < maxContext) {
      // Can we add some more context to the start?
      if ((!more1 || addedToStart <= addedToEnd) && more0
          && (more0 = iter0.prev(false))) {
        if (tmpPos == null)
          tmpPos = iter0.getPos(WordIter.TERM_START);
        else
          iter0.getPos(tmpPos, WordIter.TERM_START);

        // Enforce limit: don't encroach on the previous span's context.
        if (tmpPos.wordPos() <= prevEndWord)
          more0 = false;
        else if (tmpPos.countTextTo(contextEnd) > maxContext)
          more0 = false;
        else {
          addedToStart += tmpPos.countTextTo(contextStart);
          iter0.getPos(contextStart, WordIter.TERM_START);
        }
      }

      // Can we add more context to the end?
      if ((!more0 || addedToEnd <= addedToStart) && more1
          && (more1 = iter1.next(false))) {
        if (tmpPos == null)
          tmpPos = iter1.getPos(WordIter.TERM_END_PLUS);
        else
          iter1.getPos(tmpPos, WordIter.TERM_END_PLUS);

        // Enforce limit: don't encroach on the next span's probable context.
        // We'll define "probable context" as halfway between the end of the
        // current span and the start of the next span.
        //
        if (nextSpan != null
            && (tmpPos.wordPos() >= (posSpan.end + nextSpan.start) / 2))
          more1 = false;
        else if (contextStart.countTextTo(tmpPos) > maxContext)
          more1 = false;
        else {
          addedToEnd += contextEnd.countTextTo(tmpPos);
          iter1.getPos(contextEnd, WordIter.TERM_END_PLUS);
        }
      }
    }

    // Check our work.
    if (spanChars < maxContext)
      assert (spanChars + addedToStart + addedToEnd) <= maxContext;

    // Remember the end of context, so the context of the next span won't
    // overlap it.
    //
    prevEndWord = contextEnd.wordPos();
  } // findContext()

  /** 
   * Emit all the marks for the given hit.
   * 
   * @param posSpan       hit for which to emit marks
   * @param contextStart  start of context (or null if context disabled)
   * @param contextEnd    end of context (or null if context disabled)
   */
  void emitMarks(Span posSpan, MarkPos contextStart, MarkPos contextEnd) 
  {
    // First, do terms up to (but not including) the
    // start of the context.
    //
    if (maxContext > 0) {
      if (termMode >= MARK_ALL_TERMS)
        markTerms(iter0, termsMarkedPos, contextStart.wordPos(), false);

      // Start the context.
      collector.beginContext((MarkPos) contextStart.clone(), posSpan);
    }

    // Mark terms up til the start of the actual span.
    if (termMode >= MARK_CONTEXT_TERMS)
      markTerms(iter0, contextStart.wordPos(), posSpan.start, false);

    // Start the span.
    iter0.seekFirst(posSpan.start, true);
    collector.beginSpan(iter0.getPos(WordIter.TERM_START), posSpan);

    // Mark terms within the span (including stop-words)
    if (termMode >= MARK_SPAN_TERMS)
      markTerms(iter0, posSpan.start, posSpan.end, true);

    // End the span. Instead of the normal end, use the start position plus
    // the term length, so the span end matches the end of the last term
    // exactly.
    //
    iter0.seekLast(posSpan.end - 1, false);
    collector.endSpan(iter0.getPos(WordIter.TERM_END));

    // Mark terms up to (and including) end of context.
    if (maxContext > 0) {
      if (termMode >= MARK_CONTEXT_TERMS)
        markTerms(iter0, posSpan.end, contextEnd.wordPos() + 1, false);

      // End the context.
      collector.endContext((MarkPos) contextEnd.clone());
    }
  }

  /** Mark terms up to (but not including) 'wordPos' */
  private void markTerms(WordIter iter, int fromPos, int toPos,
                         boolean markStopWords) 
  {
    // If there's no map telling what terms to mark, skip it.
    if (terms == null)
      return;

    // Seek to the first spot.
    iter.seekFirst(fromPos, true);
    while (true) {
      
      // Get the new position.
      if (tmpPos == null)
        tmpPos = iter.getPos(WordIter.TERM_START);
      else
        iter.getPos(tmpPos, WordIter.TERM_START);

      // If we tried to seek beyond the end, abort.
      if (tmpPos.wordPos() < fromPos)
        break;

      // Are we done yet?
      if (tmpPos.wordPos() >= toPos)
        break;
      
      // See if we should mark the term. Conditions:
      // (1) The term should be in the set of search terms
      // (2a) It's a stop word, and we're supposed to mark them here, OR
      // (2b) It's not a stop word
      String term = iter.term();
      if (terms.contains(term)) {
        if (markStopWords || stopSet == null || !stopSet.contains(term)) {
          collector.term((MarkPos) tmpPos.clone(), iter
              .getPos(WordIter.TERM_END), term);
        }
      }
      if (!iter.next(true))
        break;
    }

    // Reset the iterator to be strictly within the range
    if (toPos < Integer.MAX_VALUE)
      iter.seekLast(toPos - 1, true);

    termsMarkedPos = toPos;
  }
}