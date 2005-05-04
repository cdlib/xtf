package org.cdlib.xtf.dynaXML;

import java.io.IOException;

import javax.xml.transform.Templates;

import org.cdlib.xtf.util.StructuredStore;
import org.xml.sax.InputSource;

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
 * This file created on Mar 11, 2005 by Martin Haye
 */

/**
 * Iterface that locates lazy or normal data streams for dynaXML document
 * requests. The default implementation, {@link DefaultDocLocator}, implements
 * local file access, but other implementations can be imagined that
 * read/write files over the network.
 * 
 * @author Martin Haye
 */
public interface DocLocator 
{
    /**
     * Search for a StructuredStore containing the "lazy" or persistent
     * representation of a given document. Index parameters are specified,
     * since often the lazy file is stored along with the index. This method
     * is called first, and if it returns null, then 
     * {@link #getInputSource(String)} will be called as a fall-back.
     * 
     * @param sourcePath      Path to the source document
     * @param indexConfigPath Path to the index configuration file
     * @param indexName       Name of the index being searched
     * @param preFilter       Stylesheet to filter the document with
     * @param removeDoctypeDecl Set to true to remove DOCTYPE declaration from
     *                          the XML document.
     * 
     * @return                Store containing the tree, or null if none
     *                        could be found.
     */
    StructuredStore getLazyStore( String indexConfigPath,
                                  String indexName,
                                  String sourcePath,
                                  Templates preFilter,
                                  boolean removeDoctypeDecl ) 
        throws IOException;
    

    /**
     * Retrieve the data stream for an XML source document. 
     * 
     * @param sourcePath  Path to the source document
     * @param removeDoctypeDecl Set to true to remove DOCTYPE declaration from
     *                          the XML document.
     * 
     * @return            Data stream for the document.
     */
    InputSource getInputSource( String sourcePath,
                                boolean removeDoctypeDecl ) 
        throws IOException;

} // interface DocLocator
