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

import org.apache.lucene.search.ScoreDoc;
import org.cdlib.xtf.util.AttribList;

/**
 * Represents a query hit at the document level. May contain {@link Snippet}s
 * if those were requested.
 * 
 * @author Martin Haye
 */
public abstract class DocHit extends ScoreDoc
{
    /**
     * Construct a document hit. Package-private because these should only
     * be constructed inside the text engine.
     * 
     * @param docNum    Lucene ID for the document info chunk
     * @param score     Score for this hit
     */
    DocHit( int docNum, float score ) {
        super(docNum, score);
    }
    
    /**
     * Retrieve the original file path as recorded in the index (if any.)
     */
    public abstract String filePath();
    
    /**
     * Retrieve this document's record number within the main file, or zero
     * if this is the only record.
     */
    public abstract int recordNum();
    
    /**
     * Retrieve a list of all meta-data name/value pairs associated with this
     * document.
     */
    public abstract AttribList metaData();

    /** Return the total number of snippets found for this document (not the
     *  number actually returned, which is limited by the max # of snippets
     *  specified in the query.)
     */
    public abstract int totalSnippets();
    
    /**
     * Return the number of snippets available (limited by the max # specified
     * in the original query.)
     */
    public abstract int nSnippets();
    
    /**
     * Retrieve the specified snippet. In general, crossQuery will set getText
     * to 'true', while dynaXML may set it either way, depending on whether 
     * the document result formatter stylesheet references the &lt;snippet&gt;
     * elements in the SearchTree. It's always safe, but not quite as 
     * efficient, to assume 'true'. 
     *
     * @param hitNum    0..nSnippets()
     * @param getText   true to fetch the snippet text in context, false to
     *                  optionally skip that work and only fetch the rank, 
     *                  score, etc. 
     *                  
     */ 
    public abstract Snippet snippet( int hitNum, boolean getText ); 

} // class DocHit
