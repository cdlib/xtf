package org.cdlib.xtf.textEngine;

import java.util.Set;

import org.cdlib.xtf.textEngine.facet.ResultFacet;

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
 * Represents the results of a query. This consists of a few statistics,
 * followed by an array of document hit(s).
 * 
 * @author Martin Haye
 */
public class QueryResult
{
    /** 
     * Context of the query (including stop word list, and maps for
     * plurals and accents). CrossQuery doesn't use the context, but dynaXML 
     * does.
     */
    public QueryContext context;
    
    /** 
     * A set that can be used to check whether a given term is present
     * in the original query that produced this hit. Only applies to the "text"
     * field (i.e. the full text of the document.) CrossQuery doesn't use 
     * the text term set, but dynaXML does.
     */
    public Set textTerms;
    
    /** 
     * Total number of documents matched by the query (possibly many more
     * than are returned in this particular request.)
     */
    public int totalDocs;
    
    /** Ordinal rank of the first document hit returned (0-based) */
    public int startDoc;
    
    /** Oridinal rank of the last document hit returned, plus 1 */
    public int endDoc;
    
    /** One hit per document */
    public DocHit[] docHits;
    
    /** Faceted results grouped by field value (if specified in query) */
    public ResultFacet[] facets;
    
} // class QueryResult
