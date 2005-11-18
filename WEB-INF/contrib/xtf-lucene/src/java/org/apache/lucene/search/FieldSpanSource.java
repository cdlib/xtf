package org.apache.lucene.search;

import org.apache.lucene.search.spans.FieldSpans;
import org.apache.lucene.search.spans.SpanRecordingScorer;

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

/**
 * This class, an instance of which is passed to a SpanHitCollector for each
 * hit, retrieves FieldSpans when requested. This is performed lazily so that
 * it can be avoided for hits that don't make the grade.
 */
public class FieldSpanSource 
{
  SpanRecordingScorer[] scorers;
  int curDoc = -1;
  
  /**
   * Package-private on purpose. Should only be created by RecordingSearcher.
   */
  FieldSpanSource(SpanRecordingScorer[] scorers) {
    this.scorers = scorers;
  }
  
  /**
   * Retrieve the spans for the given document.
   * 
   * @param doc Document to get spans for. Typically, the FieldSpanSource can
   *            only get spans for the most recent document collected.
   * @return    Recorded spans for the document.
   */
  public FieldSpans getSpans(int doc) {
    if (doc != curDoc)
        throw new UnsupportedOperationException( "Foo" );
    
    FieldSpans fieldSpans = new FieldSpans();
    for (int i=0; i<scorers.length; i++)
      scorers[i].recordSpans(doc, fieldSpans);
    return fieldSpans;
  }
}
