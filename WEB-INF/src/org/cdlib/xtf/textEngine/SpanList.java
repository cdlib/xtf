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

import java.util.Iterator;

import org.cdlib.xtf.textEngine.dedupeSpans.SpanHit;

/**
 * Interface for storing a list of chunks (either ranked or un-ranked, limited
 * or unlimited).
 * 
 * @author Martin Haye
 */
public abstract class SpanList
{
    /** Total score of all the spans */
    public float totalScore;
    
    /** Total number of spans */
    public int totalHits;
    
    /** Add a span to the list */
    public abstract void add( SpanHit hit );
    
    /** Find out how many spans are actually in the list (might be less than
     * {@link #totalHits} if some have been filtered out because they scored
     * too low.) 
     */
    public abstract int size();
    
    /** Iterate through the spans in order */
    public abstract Iterator iterator();

    /**
     * Simply updates the statistics based on the hit. Derived versions of 
     * {@link #add(SpanHit)} should call this method for every hit. 
     */
    protected final void updateStats( SpanHit hit )
    {
        totalHits++;
        totalScore += hit.score;
    }
    
} // SpanList
