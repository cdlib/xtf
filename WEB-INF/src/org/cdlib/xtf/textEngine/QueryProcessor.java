package org.cdlib.xtf.textEngine;

import java.io.File;
import java.io.IOException;

import javax.xml.transform.Source;

/*
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
 
/*
 * This file created on Mar 1, 2005 by Martin Haye
 */

/**
 * Takes a QueryRequest, rewrites the queries if necessary to remove stop-
 * words and form n-grams, then consults the index(es), and produces a 
 * QueryResult.
 * 
 * @author Martin Haye
 */
public abstract class QueryProcessor 
{
    /**
     * Takes a query request and handles searching the index and forming 
     * the results.
     * 
     * @param queryReqDoc   The request to process, in XML format
     * @param baseDir       Directory to use for resolving relative paths
     *                      (if any)
     * @return              Zero or more document hits
     */
    public abstract QueryResult processReq( Source queryReqDoc, File baseDir )
        throws IOException;
    
    /** Optional method: takes a pre-parsed QueryRequest (for speed) */
    public QueryResult processReq( QueryRequest req )
        throws IOException
    {
        return processReq( req.getSource(), req.getBaseDir() );
    }
}
