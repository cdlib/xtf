package org.cdlib.xtf.textEngine;

/**
 * Copyright (c) 2004, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, 
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, 
 *   this list of conditions and the following disclaimer in the documentation 
 *   and/or other materials provided with the distribution.
 * - Neither the name of the University of California nor the names of its
 *   contributors may be used to endorse or promote products derived from this 
 *   software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */

import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.DefaultSimilarity;

/** 
 * Expert: Extended Scoring API for XTF
 *
 * XTF adds a new capability to Lucene: the ability to identify individual
 * snippets within a larger document that has been chunked. The scores of
 * these snippets are then combined to form the score for the entire document.
 * 
 * Subclass XtfSimilarity to override the default way this combining is done. 
 * 
 * @see Similarity
 */
public class XtfSimilarity extends DefaultSimilarity 
{
    
  /** 
   * <p>Computes a score factor based on the distance between the current
   * match and a nearby higher-scoring match.</p> 
   *
   * <p>Matching spans near higher-scoring spans are usually de-valued,
   * so that ranked search results won't tend to bunch up around a single
   * area. Implementations of this method usually return larger values
   * for smaller distances. 
   * 
   * <p>Matching spans repeated in a document indicate the topic of the
   * document, so implementations of this method usually return larger values
   * when <code>freq</code> is large, and smaller values when <code>freq</code>
   * is small.</p>
   *
   * <p>The default implementation: sqrt( dist / maxDist )
   *
   * @param dist is the distance, in words, between this match and the
   *        nearby higher scoring one.
   * @param maxDist is the greatest distance that will ever be passed to
   *        this method; this is typically equal to the index's chunk overlap.
   * @return a score factor
   */
  public float damp( int dist, int maxDist ) {
      return (float) Math.sqrt( dist / (float)maxDist );
  }
    
    
  /** 
   * Computes a score factor based the sum of all the span scores within
   * a document. This value is passed to {@link #combine(float,float)} to
   * form the final document score.
   *
   * <p>Matching spans repeated in a document indicate the topic of the
   * document, so implementations of this method usually return larger values
   * when <code>freq</code> is large, and smaller values when <code>freq</code>
   * is small.
   *
   * <p>The default implementation calls {@link #tf(float)}.
   *
   * @param freq the sum of scores of all matching spans within a document
   * @return a score factor
   */
  public float spanFreq( float freq ) {
      return tf( freq );
  }
    
  /** 
   * Computes a score factor for a document, based on the document's meta-data
   * score and the sum of all matching text spans within the document.
   *
   * <p>The default implementation simply adds the two scores.</p>
   *
   * @param docScore the sccore assigned to the document
   * @param spanSumScore the score of all spans (usually from 
   *                     {@link #spanFreq(float)})
   * @return a score factor
   */
  public float combine( float docScore, float spanSumScore ) {
      return docScore + spanSumScore;
  }
    
} // class XtfSimilarity
