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

import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.Weight;


class SpanScorer extends Scorer {
  private Spans spans;
  private Weight weight;
  private float value;

  private boolean firstTime = true;
  private boolean more = true;

  private int doc;
  private float freq;

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

    if (!more) return false;

    freq = 0.0f;
    doc = spans.doc();

    while (more && doc == spans.doc()) {
      int matchLength = spans.end() - spans.start();
      freq += getSimilarity().sloppyFreq(matchLength);
      more = spans.next();
    }

    return more || freq != 0.0f;
  }

  public int doc() { return doc; }

  public float score() throws IOException {
    // doc norm has already been accounted for in SpanTermQuery
    return getSimilarity().tf(freq) * value;
  }

  public boolean skipTo(int target) throws IOException {
    // Very important: because the next() method actually advances the spans
    // one step more than is reported back, we have to avoid skipping if the
    // effect of the skip has already been achieved.
    //
    if (doc == 0 || target > spans.doc())
      more = spans.skipTo(target);

    if (!more) return false;

    freq = 0.0f;
    doc = spans.doc();

    while (more && spans.doc() == target) {
      freq += getSimilarity().sloppyFreq(spans.end() - spans.start());
      more = spans.next();
    }

    return more || freq != 0.0f;
  }

  public Explanation explain(final int doc) throws IOException {
    Explanation tfExplanation = new Explanation();

    skipTo(doc);

    float phraseFreq = (doc() == doc) ? freq : 0.0f;
    tfExplanation.setValue(getSimilarity().tf(phraseFreq));
    tfExplanation.setDescription("tf(phraseFreq=" + phraseFreq + ")");

    return tfExplanation;
  }

}
