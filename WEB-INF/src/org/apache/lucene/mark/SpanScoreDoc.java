package org.apache.lucene.mark;

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

import org.apache.lucene.search.ScoreDoc;

/** Expert: Extends a {@link ScoreDoc} with the ability to store matching
 * spans. These can then be used to construct a {@link SpanDocument} to
 * allow marking search terms, context, and span hits within the document's 
 * stored fields. 
 *
 * @author  Martin Haye
 * @version $Id: SpanScoreDoc.java,v 1.1 2005-02-08 23:19:38 mhaye Exp $
 * @see ScoreDoc
 * @see SpanDocument
 */
public class SpanScoreDoc extends ScoreDoc 
  implements java.io.Serializable, FieldSpansContainer 
{
  /** Expert: The marked fields in the document */
  public FieldSpans fieldSpans;

  /** Expert: Constructs a SpanScoreDoc. */
  public SpanScoreDoc(int doc, float score, FieldSpans spans) {
    super(doc, score);
    this.fieldSpans = spans;
  }
  
  /** Retrieves the matching spans for fields in the query */
  public FieldSpans getFieldSpans() { return fieldSpans; }
}
