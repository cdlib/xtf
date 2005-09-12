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

import org.apache.lucene.search.spans.Span;

/**
 * Receives callbacks to mark terms, context start/end, and span start/end
 * by {@link SpanDocument#markField(String, MarkCollector)} and its cousins.
 *
 * <p>Created: Dec 14, 2004</p>
 *
 * @author  Martin Haye
 * @version $Id: MarkCollector.java,v 1.1 2005-09-12 19:06:11 mhaye Exp $
 */
public interface MarkCollector
{
  /** Marks the position of the very start of the field. */ 
  void beginField(MarkPos pos);

  /** 
   * Marks the start and end of a search term. Depending on the term marking
   * mode, this may occur only within hits, or in the context area surrounding
   * hits, or in the whole field.
   * 
   * @param startPos  start character
   * @param endPos    end character
   * @param term      term text as found in the index
   */
  void term(MarkPos startPos, MarkPos endPos, String term);
  
  /** 
   * If context marking is enabled, this is called to mark the start of the
   * context surrounding a hit. It will be followed by a call to
   * {@link #beginSpan(MarkPos, Span) beginSpan()}, one or more calls to
   * {@link #term(MarkPos, MarkPos, String) term()}, then a call to
   * {@link #endSpan(MarkPos) endSpan()}, and finally a call to
   * {@link #endContext(MarkPos) endContext()}.
   * 
   * @param pos   starting position for context
   * @param span  the hit for which context is being marked
   */
  void beginContext(MarkPos pos, Span span);
  
  /** 
   * Marks the beginning of a hit. If context marking is enabled, this
   * will always occur within a
   * {@link #beginContext(MarkPos, Span) beginContext()}/
   * {@link #endContext(MarkPos) endContext()}
   * pair. It will be followed by one or more calls to
   * {@link #term(MarkPos, MarkPos, String) term()}, then a call to
   * {@link #endSpan(MarkPos) endSpan()}.
   */
  void beginSpan(MarkPos pos, Span span);
  
  /** Marks the end of a hit. Always follows 
   * {@link #beginSpan(MarkPos, Span) beginSpan()}. 
   */
  void endSpan(MarkPos pos);

  /**
   * If context marking is enabled, this is called to mark the end of the
   * context surrounding a hit. Always follows 
   * {@link #beginContext(MarkPos, Span) beginContext()}.
   */
  void endContext(MarkPos pos);

  /** Marks the very end of the field. */
  void endField(MarkPos pos);
}
