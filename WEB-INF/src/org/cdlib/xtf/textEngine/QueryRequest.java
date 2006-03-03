package org.cdlib.xtf.textEngine;

import org.apache.lucene.mark.ContextMarker;
import org.apache.lucene.search.Query;
import org.cdlib.xtf.textEngine.facet.FacetSpec;

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

/**
 * Stores a single query request to be processed by the XTF text engine.
 * 
 * @author Martin Haye
 */
public class QueryRequest implements Cloneable
{
    /** Path (base dir relative) for the resultFormatter stylesheet */
    public String     displayStyle;
    
    /** Document rank to start with (0-based) */
    public int        startDoc     = 0;
    
    /** Max # documents to return from this query */
    public int        maxDocs      = 10;
    
    /** Path to the Lucene index we want to search */
    public String     indexPath;

    /** The Lucene query to perform */
    public Query      query;
    
    /** Optional list of fields to sort documents by */
    public String     sortMetaFields;

    /** Target size, in characters, for snippets */
    public int        maxContext   = 80;
    
    /** Limit on the total number of terms allowed */
    public int        termLimit    =  50;
    
    /** Limit on the total amount of "work" */
    public int        workLimit    =   0;
    
    /** Term marking mode */
    public int        termMode     = ContextMarker.MARK_SPAN_TERMS;
    
    /** Facet specifications (if any) */
    public FacetSpec[] facetSpecs  = null;
    
    /** Whether to normalize scores (turn off to help debug ranking problems) */
    public boolean    normalizeScores = true;
    
    /** 
     * Whether to calculate an explanation of each score. Time-consuming, so
     * should not be used except during development 
     */
    public boolean    explainScores = false;
    
    /** 
     * Experimental, and probably temporary:
     * Path of file containing document keys -> boost factors. 
     */
    public String     boostSetPath;
    
    /** 
     * Experimental, and probably temporary:
     * Field name for boost set document keys.
     */
    public String     boostSetField;
    
    /** 
     * Experimental, and probably temporary:
     * Exponent applied to all boost set values.
     */
    public float      boostSetExponent = 1.0f;
    
    /** Experimental: provide spelling suggestions */
    public SpellcheckParams spellcheckParams = null;
    
    /** Optional: the <parameters> block sent to the query parser stylesheet */
    public String     parserInput = null;
    
    /** Optional: the raw output of the query parser stylesheet */
    public String     parserOutput = null;
    
    // Creates an exact copy of this query request.
    public Object clone() 
    {
        try { return super.clone(); }
        catch( CloneNotSupportedException e ) { throw new RuntimeException(e); }
    } // clone()
    
} // class QueryRequest
