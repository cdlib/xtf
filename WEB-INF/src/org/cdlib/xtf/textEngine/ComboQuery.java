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
import java.util.LinkedList;

import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.cdlib.xtf.textEngine.dedupeSpans.SpanNearQuery;
import org.cdlib.xtf.textEngine.dedupeSpans.SpanNotQuery;
import org.cdlib.xtf.textEngine.dedupeSpans.SpanQuery;

/**
 * Represents a query on a single index: a meta-data query, a text query, or
 * a combination of the two.
 *
 * @author Martin Haye
 */
public class ComboQuery
{
    /** Path (relative to servlet base dir) of the index to search */
    public String     indexPath;
    
    /** Meta-data query to perform */
    public Query      metaQuery;
    
    /** Full-text query to perform */
    public SpanQuery  textQuery;
    
    /** Optional query to restrict full text results based on sectionType */
    public Query      sectionTypeQuery;
    
    /** Optional list of fields to sort documents by */
    public String     sortMetaFields;
    
    /** Contains all terms refrenced by the query */
    public TermMap    terms;
    
    /** Limit on the total number of terms allowed */
    public int        termLimit    =  50;
    
    /** Limit on the total amount of "work" */
    public int        workLimit    =   0;
    
    /** Maximum number of snippets to return for each document */
    public int        maxSnippets  = 100;
    
    /** Target size (in characters) of text snippets */
    public int        maxContext   =  80;
    
    /** List of queries that need some sort of fix-up after certain parameters
     *  (like index chunk size) are known.
     */
    private LinkedList slopFixups = new LinkedList();
    
    /** Default constructor - does nothing */
    public ComboQuery() { }
    
    /** Construct a combined query from a meta and/or text query */
    public ComboQuery( Query meta, SpanQuery text ) {
        this.metaQuery = meta;
        this.textQuery = text;
    }
    
    /** 
     * Mark a sub-query as needing slop fixup after index parameters are 
     * known.
     * 
     * @param query     The query to mark
     * @param maxSlop   value to clamp the slop down to
     * 
     */ 
    public void addSlopFixup( Query query, int maxSlop ) {
        Fixup f = new Fixup();
        f.query = query;
        f.maxSlop = maxSlop;
        slopFixups.add( f );
    }
    
    /**
     * If a query is rewritten, its fixup needs to be copied from the old
     * to the new.
     * 
     * @param oldQuery      Previous query which should have been marked
     *                      using {@link #addSlopFixup(Query, int)}.
     * @param newQuery      Query that's replacing it.
     */
    public void remapFixup( Query oldQuery, Query newQuery ) {
        for( Iterator iter = slopFixups.iterator(); iter.hasNext(); ) {
            Fixup f = (Fixup) iter.next();
            if( f.query == oldQuery ) {
                f.query = newQuery;
                return;
            }
        }
        assert false : "failed to remap fixup";
    }
    
    /**
     * After index parameters are known, this method should be called to
     * update the slop parameters of queries that need to know.
     * 
     * @param maxSlop       The maximum slop value for this index.
     * @param chunkBump     The amount of words between one chunk and the next.
     */
    public void fixupSlop( int maxSlop, int chunkBump ) {
        for( Iterator iter = slopFixups.iterator(); iter.hasNext(); ) 
        {
            Fixup f = (Fixup) iter.next();

            Query query = f.query;
            int   slop  = Math.min( maxSlop, f.maxSlop );
            
            if( query instanceof SpanNearQuery )
                ((SpanNearQuery)query).setSlop( slop );
            else if( query instanceof SpanNotQuery )
                ((SpanNotQuery)query).setSlop( slop, chunkBump );
            else if( query instanceof PhraseQuery )
                assert false : "PhraseQuery no longer supported";
        }
    } // fixup()
    
    /** Keeps track of a single fix-up request */
    private class Fixup {
        Query query;
        int   maxSlop;
    }
    
} // class ComboQuery
