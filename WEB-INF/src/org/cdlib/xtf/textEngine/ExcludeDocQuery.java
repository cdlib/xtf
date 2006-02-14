package org.cdlib.xtf.textEngine;

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

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Weight;

import java.io.IOException;


/**
 * A query that excludes a specific document from the result set.
 */
public class ExcludeDocQuery
extends Query {

  Query query;
  int   docToExclude;

  /**
   * Constructs a new query which applies a filter to the results of the original query.
   * Filter.bits() will be called every time this query is used in a search.
   * @param query  Query to be filtered, cannot be <code>null</code>.
   * @param filter Filter to apply to query results, cannot be <code>null</code>.
   */
  public ExcludeDocQuery (Query query, int docToExclude) {
    this.query = query;
    this.docToExclude = docToExclude;
  }

  /**
   * Returns a Weight that applies the filter to the enclosed query's Weight.
   * This is accomplished by overriding the Scorer returned by the Weight.
   */
  public Weight createWeight (final Searcher searcher) {
    Weight x = null;
    try {
        x = query.weight(searcher);
    }
    catch( IOException e ) { throw new RuntimeException(e); }
    final Weight weight = x;
    return new Weight() {

      // pass these methods through to enclosed query's weight
      public float getValue() { return weight.getValue(); }
      public float sumOfSquaredWeights() throws IOException { return weight.sumOfSquaredWeights(); }
      public void normalize (float v) { weight.normalize(v); }
      public Explanation explain (IndexReader ir, int i) throws IOException { return weight.explain (ir, i); }

      // return this query
      public Query getQuery() { return ExcludeDocQuery.this; }

      // return a scorer that overrides the enclosed query's score if
      // the given hit has been filtered out.
      public Scorer scorer (IndexReader indexReader) throws IOException {
        final Scorer scorer = weight.scorer (indexReader);
        return new Scorer (query.getSimilarity (searcher)) {

          // pass these methods through to the enclosed scorer
          public boolean next() throws IOException { return scorer.next(); }
          public int doc() { return scorer.doc(); }
          public boolean skipTo (int i) throws IOException { return scorer.skipTo(i); }

          // if the document has been filtered out, set score to 0.0
          public float score() throws IOException {
            return (docToExclude != scorer.doc()) ? scorer.score() : 0.0f;
          }

          // add an explanation about whether the document was filtered
          public Explanation explain (int i) throws IOException {
            Explanation exp = scorer.explain (i);
            if (docToExclude != i)
              exp.setDescription ("allowed by filter: "+exp.getDescription());
            else
              exp.setDescription ("removed by filter: "+exp.getDescription());
            return exp;
          }
        };
      }
    };
  }

  /** Rewrites the wrapped query. */
  public Query rewrite(IndexReader reader) throws IOException {
    Query rewritten = query.rewrite(reader);
    if (rewritten != query) {
      ExcludeDocQuery clone = (ExcludeDocQuery)this.clone();
      clone.query = rewritten;
      return clone;
    } else {
      return this;
    }
  }

  public Query getQuery() {
    return query;
  }

  /** Prints a user-readable version of this query. */
  public String toString (String s) {
    return "excludeDoc("+docToExclude+","+query.toString(s)+")";
  }

  /** Returns true iff <code>o</code> is equal to this. */
  public boolean equals(Object o) {
    if (o instanceof ExcludeDocQuery) {
      ExcludeDocQuery fq = (ExcludeDocQuery) o;
      return (query.equals(fq.query) && docToExclude == fq.docToExclude);
    }
    return false;
  }

  /** Returns a hash code value for this object. */
  public int hashCode() {
    return query.hashCode() ^ docToExclude;
  }
}
