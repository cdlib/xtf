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

import org.apache.lucene.util.PriorityQueue;
import org.cdlib.xtf.textEngine.XtfSimilarity;

/**
 * Records spans produced by a SpanQuery, and passes them to a SpanHitCollector.
 * If the spans overlap, the highest-scoring span will win; if other spans
 * remain that don't overlap, they will pass to SpanHitCollector also, though
 * the scores of nearby spans will be reduced. In any case, the hit collector 
 * will never receive overlapping hits.
 * 
 * @author Martin Haye
 */
public class DedupeQueue
{
    /** Collector to pass the final (de-duplicated) spans to */
    private SpanHitCollector collector;
    
    /** Used to compute the dampening effect of nearby hits */
    private XtfSimilarity similarity;
    
    /** Number of words in an index chunk */
    private int chunkSize;
    
    /** Number of words chunks overlap by in the index */
    private int chunkOverlap;
    
    /** chunkSize - chunkOverlap */
    private int chunkBump;
    
    /** First chunk number in a series of overlapping chunks */
    private int baseChunk;
    
    /** Current chunk number */
    private int curChunk = -99;
    
    /** Word offset of the current chunk from the base chunk */
    private int curWordOffset;
    
    /** Stores the word offset of the farthest span */
    private int farEnd;
    
    /** Maximum number of possibly overlapping hits to evaluate.
     *  If this is exceeded, an approximation algorithm will half the
     *  queue, and then de-duplication will proceed as normal.<br><br>
     * 
     *  Default value: 100
     */
    private final static int maxSize = 100;
    
    /** A pre-queue used to record span hits until the end of a chunk is
     *  reached. Then their scores are adjusted and then the hits are
     *  added to the real queue.
     */
    private SpanHit[] chunkQueue    = new SpanHit[maxSize];
    
    /** Number of entries in the pre-queue */
    private int       chunkQueueTop = 0;

    /** Span hits ready for de-duplication, in document order */
    private SpanHit[] ents        = new SpanHit[maxSize];
    
    /** Number of hits ready for de-duplication */
    private int       size        = 0;
    
    /** Span hits ready for de-duplication, in descending score order */
    private Heap      heap        = new Heap( maxSize );
    
    /**
     * Create the queue and attach it to the given collector. Future
     * de-duplicated hits will be passed to that collector.
     */
    public DedupeQueue( SpanHitCollector collector, 
                        XtfSimilarity similarity )
    {
        this.collector = collector;
        this.similarity = similarity;
        chunkSize    = collector.getChunkSize();
        chunkOverlap = collector.getChunkOverlap();
        chunkBump    = chunkSize - chunkOverlap;
    } // constructor
    
    
    /**
     * Add a possible hit to the queue.
     * 
     * @param start     Start position of the span
     * @param end       End position of the span
     * @param score     Score for this span
     */
    public void add( int start, int end, float score )
    {
        // Adjust start and end based on the chunk start
        start += curWordOffset;
        end   += curWordOffset;
        
        // Add it to the chunk queue for now, until finishChunk() is called.
        if( chunkQueueTop == chunkQueue.length ) {
            SpanHit[] oldQueue = chunkQueue;
            chunkQueue = new SpanHit[chunkQueueTop*3/2];
            System.arraycopy( oldQueue, 0, chunkQueue, 0, chunkQueueTop );
        }
        chunkQueue[chunkQueueTop++] = new SpanHit( curChunk, start, end, score );
    } // add()
    
    
    /**
     * Called when all the hits for a particular chunk have been added.
     * Adjusts their score by the given factor and then does the work of
     * de-duplicating them.
     * 
     * @param scoreFactor   Adjustment factor for the scores
     */
    public void finishChunk( float scoreFactor )
    {
        // Process each hit in the chunk queue.
        for( int i = 0; i < chunkQueueTop; i++ ) 
        {
            SpanHit hit = chunkQueue[i];
            
            // Adjust this hit's score.
            hit.score *= scoreFactor;
            
            // If this new hit starts at least chunkOverlap beyond the end of 
            // everything in the queue, we can take this golden opportunity to 
            // flush it. Note that in the case where it's less than chunkOverlap,
            // there's a possibility that a hit in the next chunk could overlap
            // with it and we wouldn't detect it.
            //
            if( size > 0 && hit.start >= 
                (ents[size-1].end + (chunkOverlap*2)) )
            {
                flush();
            }
            
            // In the extremely unlikely case that we have a huge number of
            // possibly overlapping spans, a rational way to proceed is to flush
            // what we have, and then insert a fake entry pretending to be the
            // whole mess, so that new hits won't overlap any of the old ones.
            //
            if( size == maxSize ) {
                flush();
                ents[0] = new SpanHit( curChunk, 0, farEnd, 0.0f );
                ents[0].index = -1;
                heap.put( ents[0] );
                size = 1;
            }
            
            // Okay, add this new entry to both our array and to the heap (which is
            // kept so that the highest scoring entry is always on top.)
            //
            hit.index = size;
            ents[size++] = hit;
            heap.put( hit );
            assert heap.size() == size;
        } // for i
        
        // Ready for the next chunk.
        chunkQueueTop = 0;
        
    } // finishChunk()

    
    /**
     * Find the highest non-overlapping entries in the queue and pass them to
     * the user-supplied span hit collector. Note that hits are *NOT* collected
     * in position order, but rather in order of descending score.
     */
    public void flush()
    {
        // Repeatedly draw off the highest element until the heap is empty.
        for( int i = size; i > 0; i-- ) {
            
            // Get the highest scoring element.
            SpanHit ent = (SpanHit) heap.pop();
            
            // If it was cancelled due to overlap with a prior (better) element,
            // skip it.
            //
            if( ent.index < 0 )
                continue;
            
            // Devalue nearby entries, and cancel out overlapping ones.
            //
            for( int j = 0; j < size; j++ ) {
                if( ents[j].index < 0 ) continue;

                if( ents[j].start >= ent.end ||
                    ents[j].end   <= ent.start )
                {
                    // Not overlapping... devalue close-by hits a lot,
                    // devalue distant ones a little.
                    //
                    int dist = Math.abs( ent.start - ents[j].start );
                    if( dist < chunkOverlap ) {
                        float devalue = similarity.damp( dist, chunkOverlap );
                        ents[j].score *= devalue;
                    }
                }
                else {
                    // Overlapping hit... kill it entirely.
                    ents[j].index = -1;
                }
            }
            
            // Remember the furthest end.
            if( ent.end > farEnd )
                farEnd = ent.end;
            
            // Notify the collector -- we have a hit! Gotta be a little tricky
            // to make sure the word positions are relative to the right
            // chunk.
            //
            int adjust = (ent.chunk-baseChunk) * chunkBump;
            ent.start -= adjust;
            ent.end   -= adjust;
            assert( ent.start >= 0         && ent.start <  chunkSize );
            assert( ent.end   >  ent.start && ent.end   <= chunkSize );
            collector.collectSpan( ent );
            
        } // for i
        
        // All gone now.
        size = 0;
        
    } // flush()
    

    /**
     * Called when hits are about to be added for a new chunk.
     * 
     * @param chunk    Chunk number that is being started
     */
    public void startChunk( int chunk ) {
        assert chunkQueueTop == 0; // Must call finishChunk() before starting next.
        assert curChunk != chunk; // Shouldn't call twice for same chunk.
        if( curChunk != chunk-1 ) {
            assert chunk > curChunk : "chunk #'s must be monotonic";
            if( size > 0 )
                flush();
            baseChunk = chunk;
            curWordOffset = 0;
            farEnd = 0;
        }
        else
            curWordOffset += chunkBump;
        curChunk = chunk;
    } // startChunk()

    
    /** Keeps the highest scoring SpanHit at the top */
    private class Heap extends PriorityQueue
    {
        public Heap( int size ) {
            initialize( size );
        }
        
        public boolean lessThan( Object o1, Object o2 ) {
            final float s1 = ((SpanHit)o1).score;
            final float s2 = ((SpanHit)o2).score;
            if( s1 == s2 ) {
                if( ((SpanHit)o1).start == ((SpanHit)o2).start )
                    return ((SpanHit)o1).end < ((SpanHit)o2).end;
                return ((SpanHit)o1).start < ((SpanHit)o2).start;
            }
            return s1 > s2;
        }
    } // class Heap

} // class DedupeQueue
