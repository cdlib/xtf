package org.cdlib.xtf.textEngine.dedupeSpans;

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
import org.cdlib.xtf.textEngine.XtfSimilarity;

/** Runs a span query and scores the resulting spans, passing them to a
 *  SpanHitCollector if specified.
 */
class SpanScorer extends Scorer {
  private Spans spans;
  private Weight weight;
  private byte[] norms;
  private float value;
  private int maxCoord;
  
  private boolean firstTime = true;
  private boolean more = true;

  private int doc;
  private float freq;
  private int coordBits;
  private int start;
  private int end;
  
  private SpanHitCollector collector;
  private DedupeQueue      dedupeQueue;

  SpanScorer(Spans spans, Weight weight, Similarity similarity, byte[] norms,
             SpanHitCollector collector, int maxCoord)
    throws IOException {
    super(similarity);
    this.spans = spans;
    this.norms = norms;
    this.weight = weight;
    this.collector = collector;
    this.maxCoord = maxCoord;
    
    value = weight.getValue();
    if( collector != null )
        dedupeQueue = new DedupeQueue( collector, (XtfSimilarity)similarity );
  }

  public boolean next() throws IOException {
    if (firstTime) {
      // For highlighting purposes, let the collector know the full (expanded)
      // list of terms that might be hit.
      //
      if( collector != null )
          collector.collectTerms( spans.allTerms() );
      more = spans.next();
      firstTime = false;
    }

    return advance();
  }
  
  private final boolean advance() throws IOException 
  {

    if( !more ) {
        if( collector != null ) {
            dedupeQueue.flush();
            collector.finish();
        }
        return false;
    }
      
    freq = 0.0f;
    start = Integer.MAX_VALUE;
    end = Integer.MIN_VALUE;
    coordBits = 0;
    doc = spans.doc();
    if( collector != null )
        dedupeQueue.startChunk( doc );

    while (more && doc == spans.doc()) {
      addToQueue();
      more = spans.next();
    }

    if( freq != 0.0f ) {
        // Get the number of terms matched. Note that it may be zero in the
        // case of a wildcard query.
        //
        int coord = Math.max( 1, countBits(coordBits) );
        
        // Include the coordination factor (roughly, the number of terms
        // matched), and the norm factor (basically the chunk's boost.)
        //
        float coordFactor = getSimilarity().coord(coord, maxCoord);
        float norm = Similarity.decodeNorm( norms[doc] );
        if( collector != null )
            dedupeQueue.finishChunk( coordFactor * norm );
    }
    
    if( !more && collector != null ) {
        dedupeQueue.flush();
        collector.finish();
    }

    return more || freq != 0.0f;
  }

  public int doc() { return doc; }

  public float score() throws IOException {
    float raw = getSimilarity().tf(freq) * value;
    return raw * Similarity.decodeNorm(norms[doc]); // normalize
  }

  public boolean skipTo(int target) throws IOException {
    more = spans.skipTo(target);
    return advance();
  }

  private final void addToQueue() {
    final float score = spans.score();
    coordBits |= spans.coordBits();
    freq += score;
    
    final int spanStart = spans.start();
    final int spanEnd   = spans.end();
    if( spanStart < start )
        start = spanStart;
    if( spanEnd > end )
        end = spanEnd;
      
    if (collector != null && score != 0.0f)
      dedupeQueue.add(spans.start(), spans.end(), score);
  }
  
  public Explanation explain(final int doc) throws IOException {
    Explanation tfExplanation = new Explanation();

    skipTo(doc);

    float phraseFreq = (doc() == doc) ? freq : 0.0f;
    tfExplanation.setValue(getSimilarity().tf(phraseFreq));
    tfExplanation.setDescription("tf(phraseFreq=" + phraseFreq + ")");

    return tfExplanation;
  }
  
  private static int[] countTable;
  
  private int countBits( int mask ) {
    if( countTable == null ) {
      countTable = new int[256];
      for (int i = 0; i < 256; i++) {
        for (int j = i; j != 0; j >>= 1)
            countTable[i] += (j & 1);
      }
    }
    
    return countTable[(mask >>  0) & 0xff] +
           countTable[(mask >>  8) & 0xff] +
           countTable[(mask >> 16) & 0xff] +
           countTable[(mask >> 24) & 0xff];
  }

}
