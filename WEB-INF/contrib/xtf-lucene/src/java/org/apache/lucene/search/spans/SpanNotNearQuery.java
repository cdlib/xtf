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
 */

import java.io.IOException;

import java.util.Collection;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;

/** 
 * Removes matches which from one SpanQuery which are too close to
 * spans from another SpanQuery.
 */
public class SpanNotNearQuery extends SpanQuery {
  private SpanQuery include;
  private SpanQuery exclude;
  private int       slop;

  /** Construct a SpanNotQuery matching spans from <code>include</code> which
   * have no overlap with spans from <code>exclude</code>.*/
  public SpanNotNearQuery(SpanQuery include, SpanQuery exclude, int slop) {
    this.include = include;
    this.exclude = exclude;
    this.slop    = slop;

    if (!include.getField().equals(exclude.getField()))
      throw new IllegalArgumentException("Clauses must have same field.");
  }

  /** Return the SpanQuery whose matches are filtered. */
  public SpanQuery getInclude() { return include; }

  /** Return the SpanQuery whose matches must not overlap those returned. */
  public SpanQuery getExclude() { return exclude; }
  
  /** Return the distance that must separate matches from excluded spans.*/
  public int getSlop() {
      return slop;
  }

  public String getField() { return include.getField(); }

  public Collection getTerms() { return include.getTerms(); }
  
  public Query[] getSubQueries() {
    Query[] result = new Query[2];
    result[0] = include;
    result[1] = exclude;
    return result;
  }

  public Query rewrite(IndexReader reader) throws IOException {
    SpanQuery rewrittenInclude = (SpanQuery)include.rewrite(reader);
    SpanQuery rewrittenExclude = (SpanQuery)exclude.rewrite(reader);
    if (rewrittenInclude == include && rewrittenExclude == exclude)
      return this;
    SpanNotNearQuery clone = (SpanNotNearQuery)this.clone();
    clone.include = rewrittenInclude;
    clone.exclude = rewrittenExclude;
    return clone;
  }

  public String toString(String field) {
    StringBuffer buffer = new StringBuffer();
    buffer.append("spanNotNear(");
    buffer.append(include.toString(field));
    buffer.append(", ");
    buffer.append(exclude.toString(field));
    buffer.append(")");
    return buffer.toString();
  }


  public Spans getSpans(final IndexReader reader, final Searcher searcher) 
      throws IOException 
  {
    return new Spans() {
        private Spans includeSpans = include.getSpans(reader, searcher);
        private boolean moreInclude = true;

        private Spans excludeSpans = exclude.getSpans(reader, searcher);
        private boolean moreExclude = true;
        private boolean firstTime = true;

        public boolean next() throws IOException {
          if (moreInclude)                        // move to next include
            moreInclude = includeSpans.next();

          return advance();
        }
        
        private boolean advance() throws IOException {
          while (moreInclude && moreExclude) {

            if (includeSpans.doc() > excludeSpans.doc()) // skip exclude
              moreExclude = excludeSpans.skipTo(includeSpans.doc());

            while (moreExclude                    // while exclude is before
                   && includeSpans.doc() == excludeSpans.doc()
                   && excludeSpans.end() <= (includeSpans.start()-slop)) {
              moreExclude = excludeSpans.next();  // increment exclude
            }

            if (!moreExclude                      // if no intersection
                || includeSpans.doc() != excludeSpans.doc()
                || excludeSpans.start() >= (includeSpans.end()+slop))
              break;                              // we found a match

            moreInclude = includeSpans.next();    // intersected: keep scanning
          }
          return moreInclude;
        }
        
        public boolean skipTo(int target) throws IOException {
          if (moreInclude)                        // skip include
            moreInclude = includeSpans.skipTo(target);

          return advance();
        }
        
        public int doc() { return includeSpans.doc(); }
        public int start() { return includeSpans.start(); }
        public int end() { return includeSpans.end(); }
        public float score() { return includeSpans.score() * getBoost(); }

        public String toString() {
          return "spans(" + SpanNotNearQuery.this.toString() + ")";
        }
        
        public Explanation explain() throws IOException { 
          if (getBoost() == 1.0f)
            return includeSpans.explain();
          
          Explanation result = new Explanation(0, 
              "weight("+toString()+"), product of:" );
          
          Explanation boostExpl = new Explanation(getBoost(), "boost");
          result.addDetail(boostExpl);
          
          Explanation inclExpl = includeSpans.explain(); 
          result.addDetail(inclExpl);
          
          result.setValue(boostExpl.getValue() * inclExpl.getValue());
          return result;
        }
      };
  }

}
