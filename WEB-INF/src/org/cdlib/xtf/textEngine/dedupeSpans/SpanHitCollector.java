package org.cdlib.xtf.textEngine.dedupeSpans;

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

import java.util.Collection;

/**
 * Clients of the de-duplicating span system must implement this interface. It
 * is used to inquire about the chunk size and overlap, and to inform the client
 * of the full set of matching terms and the actual spans themselves.
 * 
 * @author Martin Haye
 */
public interface SpanHitCollector
{
    /**
     * Receives a collection of all the terms referenced by a Span query.
     */
    void collectTerms( Collection coll );
    
    /**
     * Receives a span hit. Note that span hits in the same chunk are NOT
     * sorted by position but rather by descending score.
     */
    void collectSpan( SpanHit hit );
    
    /**
     * Called when all spans have been collected.
     */
    void finish();

    /**
     * Called by the span span system to find out the max # of words in a chunk
     */
    int getChunkSize();

    /**
     * Called by the span system to find out how many words consecutive
     * chunks overlap.
     */
    int getChunkOverlap();
} // interface SpanHitCollector
