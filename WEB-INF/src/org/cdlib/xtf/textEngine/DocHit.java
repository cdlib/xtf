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

import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;

import org.apache.lucene.document.DateField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.search.ScoreDoc;
import org.cdlib.xtf.textEngine.dedupeSpans.SpanHit;
import org.cdlib.xtf.util.AttribList;

/**
 * Represents a query hit at the document level. May contain {@link Snippet}s
 * if those were requested.
 * 
 * @author Martin Haye
 */
public class DocHit extends ScoreDoc
{
    // The following are package-private, not class-private, because they are
    // accessed by DocHitQueue or other package classes.
    //
    
    /** Used to load and format snippets */
    SnippetMaker snippetMaker;
    
    /** List of snippets, ordered by score */
    SpanList spanHitList;
    
    /** 
     * Number of snippets found in the document (not just those that scored
     * high enough to report.
     */
    private int totalSnippets; // >= number of snippets
    
    /** Sorted array of spans */
    private SpanHit[] spanHits;
     
    /** Index key for this document */
    private String docKey;
    
    /** Date the original source XML document was last modified */
    private long fileDate = -1;
    
    /** Total number of chunks for this document */
    private int chunkCount = -1;
    
    /** Document's meta-data fields (copied from the docInfo chunk) */
    private AttribList metaData;
    
    /**
     * Construct a document hit. Package-private because these should only
     * be constructed inside the text engine.
     * 
     * @param docNum    Lucene ID for the document info chunk
     * @param score     Score for this hit
     */
    DocHit( int docNum, float score ) {
        super( docNum, score );
    }
    
    /**
     * Called after all hits have been gathered to normalize the scores and
     * associate a snippetMaker for later use.
     * 
     * @param snippetMaker    Will be used later by snippet() to actually
     *                        create the snippets.
     * @param docScoreNorm    Multiplied into the document's score
     * @param chunkScoreNorm  Multiplied into the score for each chunk
     */
    void finish( SnippetMaker snippetMaker,
                 float        docScoreNorm,
                 float        chunkScoreNorm )
    {
        // Record the snippet maker... we'll use it later if loading is
        // necessary.
        //
        this.snippetMaker = snippetMaker;

        // Adjust our score.
        score *= docScoreNorm;

        // If no span hits, we're done.
        if( spanHitList == null ) {
            spanHits = new SpanHit[0];
            return;
        }
        
        totalSnippets = spanHitList.totalHits;
        
        // Adjust the chunk scores
        spanHits = new SpanHit[spanHitList.size()];
        int hitNum = 0;
        for( Iterator iter = spanHitList.iterator(); iter.hasNext(); ) 
        {
            // Adjust the score
            SpanHit sp = (SpanHit) iter.next();
            sp.score *= chunkScoreNorm;
            
            // Record it.
            spanHits[hitNum++] = sp;
        } // for iter
        
        // No need for the list any more, now that we have a nice array. Might
        // as well free it up so it can be garbage-collected.
        //
        spanHitList = null;
    } // finish()
    
    /**
     * Read in the document info chunk and record the path, date, etc. that
     * we find there.
     */
    private void load()
    {
        // Read in our fields
        Document fields;
        try {
            fields = snippetMaker.reader.document( doc );
        }
        catch( IOException e ) {
            throw new HitLoadException( e );
        }
        
        // Record the ones of interest.
        metaData = new AttribList();
        for( Enumeration e = fields.fields(); e.hasMoreElements(); ) {
            Field f = (Field) e.nextElement();
            String name = f.name();
            String value = f.stringValue();
            
            if( name.equals("key") )
                docKey = value;
            else if( name.equals("fileDate") )
                fileDate = DateField.stringToTime( value );
            else if( name.equals("chunkCount") )
                chunkCount = Integer.parseInt( value );
            else if( !name.equals("docInfo") )
                metaData.put( name, snippetMaker.makeFull(value, f.name()) );
        }
        
        // We should have gotten at least the special fields.
        assert docKey     != null : "Incomplete data in index - missing 'key'";
        assert chunkCount != -1   : "Incomplete data in index - missing 'chunkCount'";

    } // finish()
    
    /**
     * Fetch a map that can be used to check whether a given term is present
     * in the original query that produced this hit.
     */
    public TermMap terms()
    {
        return snippetMaker.terms();
    }
    
    /**
     * Fetch the set of stopwords used when processing the query.
     */
    public Set stopSet()
    {
        return snippetMaker.stopSet();
    }
    
    /**
     * Return the relevance score (0.0 to 1.0) for this hit, as computed by
     * Lucene.
     */
    public final float score() {
        return score;
    }
    
    /**
     * Return the date (as the number of milliseconds since Jan 1, 1970) of
     * the source file as recorded in the index.
     */
    public final long fileDate() {
        if( fileDate < 0 ) load();
        return fileDate;
    }
    
    /**
     * Retrieve the original file path as recorded in the index (if any.)
     */
    public final String filePath()
    {
        if( docKey == null ) load();
        return docKey;
    } // filePath()
    
    /**
     * Retrieve a list of all meta-data name/value pairs associated with this
     * document.
     */
    public final AttribList metaData() {
        if( docKey == null ) load();
        return metaData;
    }

    /** Return the total number of snippets found for this document (not the
     *  number actually returned, which is limited by the max # of snippets
     *  specified in the query.)
     */
    public final int totalSnippets() {
        return totalSnippets;
    }
    
    /**
     * Return the number of snippets available (limited by the max # specified
     * in the original query.)
     */
    public final int nSnippets() {
        return spanHits.length;
    }
    
    /**
     * Retrieve the specified snippet.
     * 
     * @param hitNum    0..nSnippets()
     * @param getText   true to fetch the snippet text in context, false to
     *                  only fetch the rank, score, etc.
     */
    public final Snippet snippet( int hitNum, boolean getText ) {
        try {
            Snippet snippet = snippetMaker.make( doc, spanHits[hitNum], 
                                                 getText );
            snippet.rank = hitNum;
            return snippet;
        }
        catch( IOException e ) {
            throw new HitLoadException( e );
        }
    } // snippet()
    
} // class DocHit
