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

import org.apache.lucene.search.FieldDoc;

/**
 * Expert: Extends a {@link FieldDoc} with the ability to store matching
 * spans. These can then be used to construct a {@link SpanDocument} to
 * allow marking search terms, context, and span hits within the document's 
 * stored fields. 
 *
 * @author Martin Haye
 * @see FieldDoc
 * @see SpanDocument
 */
public class SpanFieldDoc extends FieldDoc implements FieldSpansContainer 
{
  /** Expert: The spans for each applicable field in the document */
  public FieldSpans fieldSpans;

  /** Expert: Constructs a ScoreDoc. */
  public SpanFieldDoc(int doc, float score, FieldSpans spans) {
    super(doc, score);
    this.fieldSpans = spans;
  }

  /** Expert: Creates one of these objects with the given sort information. */
  public SpanFieldDoc (int doc, float score, Comparable[] fields, 
                         FieldSpans marks) {
    super (doc, score, fields);
    this.fieldSpans = marks;
  }
  
  public FieldSpans getFieldSpans() { return fieldSpans; }
}