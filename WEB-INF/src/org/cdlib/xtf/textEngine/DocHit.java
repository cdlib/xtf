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
import java.util.Set;

import org.apache.lucene.document.DateField;
import org.apache.lucene.document.Field;
import org.apache.lucene.mark.FieldSpans;
import org.apache.lucene.mark.SpanDocument;
import org.apache.lucene.search.ScoreDoc;
import org.cdlib.xtf.textIndexer.XtfSpecialTokensFilter;
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
    
    /** Spans per field */
    private FieldSpans fieldSpans;
    
    /** Array of pre-built snippets */
    private Snippet[] snippets;
     
    /** Index key for this document */
    private String docKey;
    
    /** Date the original source XML document was last modified */
    private long fileDate = -1;
    
    /** Total number of chunks for this document */
    private int chunkCount = -1;
    
    /** Document's meta-data fields (copied from the docInfo chunk) */
    private AttribList metaData;
    
    /** Bump marker used to denote different meta-data fields w/ same name */
    private char bumpMarker = XtfSpecialTokensFilter.bumpMarker;
    
    /**
     * Construct a document hit. Package-private because these should only
     * be constructed inside the text engine.
     * 
     * @param docNum    Lucene ID for the document info chunk
     * @param score     Score for this hit
     */
    DocHit( int docNum, float score, FieldSpans spans ) {
        super(docNum, score);
        this.score      = score;
        this.fieldSpans = spans;
    }
    
    /**
     * Called after all hits have been gathered to normalize the scores and
     * associate a snippetMaker for later use.
     * 
     * @param snippetMaker    Will be used later by snippet() to actually
     *                        create the snippets.
     * @param docScoreNorm    Multiplied into the document's score
     */
    void finish( SnippetMaker snippetMaker,
                 float        docScoreNorm )
    {
        // Record the snippet maker... we'll use it later if loading is
        // necessary.
        //
        this.snippetMaker = snippetMaker;

        // Adjust our score.
        score *= docScoreNorm;
    } // finish()
    
    /**
     * Read in the document info chunk and record the path, date, etc. that
     * we find there.
     */
    private void load()
    {
        // Read in our fields
        SpanDocument spanDoc;
        try {
            assert !snippetMaker.reader.isDeleted( doc );
            spanDoc = new SpanDocument( snippetMaker.reader.document(doc),
                                        fieldSpans );
        }
        catch( IOException e ) {
            throw new HitLoadException( e );
        }
        
        // Record the ones of interest.
        metaData = new AttribList();
        for( Enumeration e = spanDoc.fields(); e.hasMoreElements(); ) {
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
            {
                // Lucene will fail subtly if we add two fields with the same 
                // name. Basically, the terms for each field are added at 
                // overlapping positions, causing a phrase search to easily 
                // span them. To counter this, the text indexer artificially 
                // introduces bump markers between them. And now, we reverse 
                // the process so it's invisible to the end-user.
                //
                String markedValue = 
                            snippetMaker.markField(spanDoc, name, value);
                if( markedValue.indexOf(bumpMarker) < 0 ) {
                    metaData.put( name, markedValue );
                    continue;
                }
                    
                int startPos = 0;
                while( startPos < markedValue.length() ) {
                    int bumpPos = markedValue.indexOf( bumpMarker, startPos );
                    if( bumpPos < 0 ) {
                        metaData.put( name, markedValue.substring(startPos) );
                        break;
                    }
                    
                    String val = markedValue.substring(startPos, bumpPos);
                    metaData.put( name, val );
                    
                    startPos = 1 + markedValue.indexOf( bumpMarker, bumpPos+1 );
                    if( Character.isWhitespace(markedValue.charAt(startPos)) )
                        startPos++; 
                }
            }
        }
        
        // We should have gotten at least the special fields.
        assert docKey     != null : "Incomplete data in index - missing 'key'";
        assert chunkCount != -1   : "Incomplete data in index - missing 'chunkCount'";

    } // finish()
    
    /**
     * Fetch a map that can be used to check whether a given term is present
     * in the original query that produced this hit.
     */
    public Set terms()
    {
        return fieldSpans.getTerms("text");
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
        if( fieldSpans == null )
            return 0;
        return fieldSpans.getSpanTotal("text");
    }
    
    /**
     * Return the number of snippets available (limited by the max # specified
     * in the original query.)
     */
    public final int nSnippets() {
        if( fieldSpans == null )
            return 0;
        return fieldSpans.getSpanCount("text");
    }
    
    /**
     * Retrieve the specified snippet.
     * 
     * @param hitNum    0..nSnippets()
     * @param getText   true to fetch the snippet text in context, false to
     *                  only fetch the rank, score, etc.
     */
    public final Snippet snippet( int hitNum, boolean getText ) 
    {
        // If we haven't built the snippets yet (or if we didn't get the
        // text for them), do so now.
        //
        if( snippets == null || (getText && snippets[hitNum].text == null) )
            snippets = snippetMaker.makeSnippets( fieldSpans, doc, 
                                                  "text", getText );
        
        // Return the pre-built snippet.
        return snippets[hitNum];
    } // snippet()
    
} // class DocHit
