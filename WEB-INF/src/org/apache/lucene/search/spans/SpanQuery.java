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

import java.util.Collection;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.Searcher;

/** Base class for span-based queries. */
public abstract class SpanQuery extends Query {
  private int   spanRecording = 0;                // # spans to record; -1=all

  /** Expert: Returns the matches for this query in an index.  Used internally
   * to search for spans. */
  public abstract Spans getSpans(IndexReader reader, Searcher searcher) throws IOException;

  /** Returns the name of the field matched by this query.*/
  public abstract String getField();

  /** Returns a collection of all terms matched by this query.*/
  public abstract Collection getTerms();

  /** Turn on recording of matching spans, and set the max number of spans
   *  to record for a given document. */
  public void setSpanRecording(int n) { spanRecording = n; }
  
  /** Retrieve the max number of spans to record for a given document, or
   *  zero if span recording is currently off. */
  public int getSpanRecording() { return spanRecording; }
  
  protected Weight createWeight(Searcher searcher) {
    return new SpanWeight(this, searcher);
  }

}

