package org.cdlib.xtf.dynaXML;

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

import java.util.Vector;

import org.cdlib.xtf.textEngine.QueryRequest;

/** Holds document information specific to a docId */
public class DocInfo
{
    /** Default constructor */
    public DocInfo() { }

    /** 
     * Copy constructor. Note that the authSpecs vector, while copied,
     * does not copy each authSpec. Rather, the vector contains ref's
     * to the same authSpecs as the original.
     * 
     * @param other     DocInfo to copy data from
     */
    public DocInfo( DocInfo other ) {
        style       = other.style;
        source      = other.source;
        indexConfig = other.indexConfig;
        indexName   = other.indexName;
        brand       = other.brand;
        authSpecs   = new Vector(other.authSpecs);
    }

    /** Path to the display stylesheet (relative to servlet base dir) */
    public String style;

    /** Path to the source XML document (relative to servlet base dir) */
    public String source;
    
    /** Path to the index configuration file (relative to servlet base dir) */
    public String indexConfig;
    
    /** Name of the index within which the lazy file is stored */
    public String indexName;

    /** 
     * Path to a brand profile (a simple XML document containing
     * parameters that are passed to the display stylesheet. If relative,
     * interpreted relative to the servlet base directory.
     */
    public String brand;

    /** 
     * List of authentication specs, which are evaluated in order until one
     * is found that definitely allows or denies access.
     */
    public Vector authSpecs = new Vector(3);
    
    /** Text query to run on the document, or null for none. */
    public QueryRequest query;

} // class DocInfo

