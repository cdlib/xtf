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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import org.apache.lucene.util.PriorityQueue;
import org.cdlib.xtf.textEngine.dedupeSpans.SpanHit;
import org.cdlib.xtf.util.ArrayIterator;

/**
 * Implements a span list kept in order ranked from highest score to least.
 * While it accumulates the score and count for every hit, the actual list
 * of top hits will only contain one for each chunk (this has the effect of
 * separating the hits in the top display so the user gets more of an idea
 * of the range of hits within a single document.)
 * 
 * Also supports an 'unlimited' mode where all spans are accumulated 
 * regardless of score, but are still returned in score-sorted order. 
 * 
 * @author Martin Haye
 */
public class RankedSpanList extends SpanList
{
    private int     maxSpans;
    
    /**
     * Compares SpanHits, ordering by descending score, ascending doc, 
     * ascending start, and ascending end (in that precedence order).
     */
    final class HitComparator implements Comparator
    {
        public int compare(Object o1, Object o2) 
        {
            SpanHit hitA = (SpanHit)o1;
            SpanHit hitB = (SpanHit)o2;
            
            // Order by score, descending
            if( hitA.score != hitB.score )
                return (hitA.score < hitB.score) ? 1 : -1;
            
            if( hitA.chunk != hitB.chunk )
                return hitA.chunk - hitB.chunk;
            if( hitA.start != hitB.start )
                return hitA.start - hitB.start;
            return hitA.end - hitB.end;
        }
    }
    
    /**
     * Used to keep a sorted heap of the top 'size' hits. Sort by descending
     * score, ascending doc, ascending start, and ascending end (in that
     * precedence order).
     */
    final class SpanQueue extends PriorityQueue
    {
        HitComparator comparator = new HitComparator();
        
        SpanQueue( int size ) { initialize(size); }
        
        protected final boolean lessThan(Object a, Object b) {
            return comparator.compare(a, b) > 0;
        }
    } // class ChunkQueue    
    
    /** Queue used to keep spans in order and toss low-scoring ones */
    private SpanQueue queue;
    
    /** List used only in unlimited mode, to accumulate <b>all</b> spans */
    private ArrayList all;
    
    /** Final list of spans, sorted in descending score order */
    private SpanHit[] sorted;
    
    
    /**
     * Construct a list that will hold, at most, 'maxSpans' chunks.
     * 
     * @param maxSpans     Max # of spans to hold, or -1 for unlimited.
     */
    public RankedSpanList( int maxSpans )
    {
        this.maxSpans = maxSpans;
        
        // If maxChunks < 0, it means the user wants all the chunks (but
        // still sorted.) If >= 0, we make a queue that only keeps track
        // of the top 'maxChunks' hits.
        //
        if( maxSpans < 0 )
            all = new ArrayList();
        else
            queue = new SpanQueue( maxSpans );
    }
    
    
    /* 
     * Adds a chunk to the list. If we already have enough chunks and the score
     * is lower than all of them, we skip the add. They must be added in 
     * ascending order by chunk number. Note that there may be multiple hits
     * per chunk, and that's actually just fine. The de-dupe queue will ensure
     * that they have sufficient distance between them.
     */
    public void add( SpanHit hit )
    {
        assert sorted == null : "All adds should be done before any iteration";
        
        // Update the total count and score.
        updateStats( hit );
        
        // And put it into the queue. If it scores less than the lowest in the
        // queue, this will actually be a no-op.
        //
        if( maxSpans < 0 )
            all.add( hit );
        else
            queue.insert( hit );
    } // add()


    /* 
     * Tells how many hits are in the list.
     */
    public int size()
    {
        if( sorted != null )
            return sorted.length;
        if( maxSpans < 0 )
            return all.size();
        return queue.size();
    }
    

    /* 
     * Returns an iterator that produces each chunk in turn, in descending
     * score order.
     */
    public Iterator iterator()
    {
        // If we haven't yet constructed the sorted array, do it now.
        if( sorted == null ) {
            sorted = new SpanHit[size()];
            
            // Handle unlimited mode separately...
            if( maxSpans < 0 ) {
                Collections.sort( all, new HitComparator() );
                sorted = (SpanHit[]) all.toArray( new SpanHit[size()] );
            }
            
            // ... from limited mode.
            else {
                for( int i = sorted.length-1; i >= 0; i-- )
                    sorted[i] = (SpanHit) queue.pop();
            }
            queue = null;
        }
            
        // Now our work is simple.
        return new ArrayIterator( sorted );
    }
    
} // class RankedChunkList
