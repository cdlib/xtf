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

import org.apache.lucene.index.IndexReader;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Explanation;

class SpanWeight implements Weight {
  private Searcher searcher;
  private float value;
  private float queryNorm;
  private float queryWeight;

  private SpanQuery query;

  public SpanWeight(SpanQuery query, Searcher searcher) {
    this.searcher = searcher;
    this.query = query;
  }

  public Searcher getSearcher() { return searcher; }
  public Query getQuery() { return query; }
  public float getValue() { return value; }

  public float sumOfSquaredWeights() throws IOException {
    queryWeight = query.getBoost();         // compute query weight
    return queryWeight * queryWeight;             // square it
  }

  public void normalize(float queryNorm) {
    this.queryNorm = queryNorm;
    queryWeight *= queryNorm;                     // normalize query weight
    value = queryWeight;                          // idf handled at lower level
  }

  public Scorer scorer(IndexReader reader) throws IOException {
    if (query.getSpanRecording() == 0) {
      return new SpanScorer(query.getSpans(reader, searcher), this,
                            query.getSimilarity(searcher));
    } else {
      return new SpanRecordingScorer(query.getSpans(reader, searcher),
                                     this,
                                     query.getSimilarity(searcher),
                                     query.getSpanRecording());
    }
  }

  public Explanation explain(IndexReader reader, int doc)
    throws IOException {

    Explanation result = new Explanation();
    result.setDescription("weight("+getQuery()+" in "+doc+"), product of:");
    String field = ((SpanQuery)getQuery()).getField();

    // explain query weight
    Explanation queryExpl = new Explanation();
    queryExpl.setDescription("queryWeight(" + getQuery() + "), product of:");

    Explanation boostExpl = new Explanation(getQuery().getBoost(), "boost");
    if (getQuery().getBoost() != 1.0f)
      queryExpl.addDetail(boostExpl);

    Explanation queryNormExpl = new Explanation(queryNorm, "queryNorm");
    queryExpl.addDetail(queryNormExpl);

    queryExpl.setValue(boostExpl.getValue() *
                       queryNormExpl.getValue());

    result.addDetail(queryExpl);

    // explain field weight
    Explanation tfExpl = scorer(reader).explain(doc);
    result.addDetail(tfExpl);

    // combine them
    result.setValue(queryExpl.getValue() * tfExpl.getValue());

    if (queryExpl.getValue() == 1.0f)
      return tfExpl;

    return result;
  }
}
