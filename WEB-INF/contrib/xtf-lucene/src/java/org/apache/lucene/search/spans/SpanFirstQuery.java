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
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;

/** Matches spans near the beginning of a field. */
public class SpanFirstQuery extends SpanQuery {
  private SpanQuery match;
  private int end;

  /** Construct a SpanFirstQuery matching spans in <code>match</code> whose end
   * position is less than or equal to <code>end</code>. */
  public SpanFirstQuery(SpanQuery match, int end) {
    this.match = match;
    this.end = end;
  }

  /** Return the SpanQuery whose matches are filtered. */
  public SpanQuery getMatch() { return match; }

  /** Return the maximum end position permitted in a match. */
  public int getEnd() { return end; }

  public String getField() { return match.getField(); }

  public Collection getTerms() { return match.getTerms(); }

  public Query[] getSubQueries() {
    Query[] result = new Query[1];
    result[0] = match;
    return result;
  }

  public Query rewrite(IndexReader reader) throws IOException {
    SpanQuery rewrittenMatch = (SpanQuery)match.rewrite(reader);
    if (rewrittenMatch == match)
      return this;
    SpanFirstQuery clone = (SpanFirstQuery)this.clone();
    clone.match = rewrittenMatch;
    return clone;
  }

  public String toString(String field) {
    StringBuffer buffer = new StringBuffer();
    buffer.append("spanFirst(");
    buffer.append(match.toString(field));
    buffer.append(", ");
    buffer.append(end);
    buffer.append(")");
    return buffer.toString();
  }

  public Spans getSpans(final IndexReader reader, final Searcher searcher) 
      throws IOException 
  {
    return new Spans() {
        private Spans spans = match.getSpans(reader, searcher);

        public boolean next() throws IOException {
          while (spans.next()) {                  // scan to next match
            if (end() <= end)
              return true;
          }
          return false;
        }

        public boolean skipTo(int target) throws IOException {
          if (!spans.skipTo(target))
            return false;

          if (spans.end() <= end)                 // there is a match
            return true;

          return next();                          // scan to next match
        }

        public int doc() { return spans.doc(); }
        public int start() { return spans.start(); }
        public int end() { return spans.end(); }
        public float score() { return spans.score() * getBoost(); }

        public String toString() {
          return "spans(" + SpanFirstQuery.this.toString() + ")";
        }
        
        public Explanation explain() throws IOException { 
          if (getBoost() == 1.0f)
            return spans.explain();
          
          Explanation result = new Explanation(0, 
              "weight("+toString()+"), product of:" );
          
          Explanation boostExpl = new Explanation(getBoost(), "boost");
          result.addDetail(boostExpl);
          
          Explanation inclExpl = spans.explain(); 
          result.addDetail(inclExpl);
          
          result.setValue(boostExpl.getValue() * inclExpl.getValue());
          return result;
        }

      };
  }

}
