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

import org.apache.lucene.search.Weight;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Similarity;


class SpanScorer extends Scorer {
  protected Spans spans;
  protected Weight weight;
  protected float value;

  protected boolean firstTime = true;
  protected boolean more = true;

  protected int doc;
  protected float freq;

  SpanScorer(Spans spans, Weight weight, Similarity similarity)
    throws IOException {
    super(similarity);
    this.spans = spans;
    this.weight = weight;
    this.value = weight.getValue();
  }

  public boolean next() throws IOException {
    if (firstTime) {
      more = spans.next();
      firstTime = false;
    }
    return advance();
  }
  
  protected boolean advance() throws IOException {

    if (!more) return false;

    freq = 0.0f;
    doc = spans.doc();

    while (more && doc == spans.doc()) {
      freq += spans.score();
      more = spans.next();
    }

    return more || freq != 0.0f;
  }

  public int doc() { return doc; }

  public float score() throws IOException {
    // norms taken care of now by the term query.
    return getSimilarity().tf(freq) * value; // raw score
  }

  public boolean skipTo(int target) throws IOException {
    // Very important: because the next() method actually advances the spans
    // one step more than is reported back, we have to avoid skipping if the
    // effect of the skip has already been achieved.
    //
    if (firstTime || target > spans.doc()) {
      more = spans.skipTo(target);
      firstTime = false;
    }
    
    return advance();
  }

  public Explanation explain(final int target) throws IOException {

    Explanation sfExpl = new Explanation(0.0f, "spanFreq, sum of:" );
    
    more = spans.skipTo(target);
    freq = 0.0f;

    int nSpans = 0;
    while (more && spans.doc() == target) {
      final float score = spans.score();
      sfExpl.addDetail(spans.explain());
      nSpans++;
      freq += score;
      more = spans.next();
    }
    
    sfExpl.setValue(freq);

    Explanation tfExpl = new Explanation(
        getSimilarity().tf(freq),
        "tf(spanFreq=" + freq + ")");
    if (nSpans == 1)
      tfExpl.addDetail(sfExpl.getDetails()[0]);
    else
      tfExpl.addDetail(sfExpl);
    return tfExpl;
  }

}
