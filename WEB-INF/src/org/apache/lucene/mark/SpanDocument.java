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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.search.spans.Span;
import org.apache.lucene.search.spans.SpanPosComparator;

/** 
 * Extends a {@link Document} with the ability to mark search terms, context,
 * and span hits within the document's stored fields. Typically instances of
 * this class are produced by a search using a query with span recording 
 * turned on (see 
 * {@link org.apache.lucene.search.spans.SpanQuery#setSpanRecording(int)}).
 *
 * <p>Created: Dec 9, 2004</p>
 *
 * @author  Martin Haye
 * @version $Id: SpanDocument.java,v 1.1 2005-02-08 23:19:38 mhaye Exp $
 */
public class SpanDocument extends Document implements FieldSpansContainer
{
  /** Record of the spans and search terms on a per-field basis */
  private FieldSpans fieldSpans;
  
  /** Wrap a {@link Document} with a set of matching search spans */
  public SpanDocument(Document toWrap, FieldSpans spans) 
    throws IOException
  {
    this.fieldSpans = spans;
    
    // Copy all the fields from the wrapped document. That way, our methods
    // need do nothing special in future.
    //
    Enumeration e = toWrap.fields();
    while (e.hasMoreElements())
      add((Field)e.nextElement());
  }
  
  /** Get the span information for all fields in the document */
  public FieldSpans getFieldSpans() { return fieldSpans; }
  
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
  
  /**
   * Mark context, spans, and terms within the given field of this document.
   * Context around each hit will be up to 80 characters (including the
   * text of the hit itself). Search terms will only be marked within hits.
   * If you would like to override these defaults, use one of the other
   * variations of this method. 
   * 
   * @param field       field name to mark
   * @param collector   collector to receive the marks
   */
  public void markField(String field, MarkCollector collector)
  {
    String value = get(field);
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
  public void markField(String field, WordIter iter, int maxContext,
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
}
