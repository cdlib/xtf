package org.cdlib.xtf.zing;

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

import java.io.File;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.saxon.om.NodeInfo;

import org.cdlib.xtf.crossQuery.CrossQuery;
import org.cdlib.xtf.servletBase.TextConfig;
import org.cdlib.xtf.textEngine.QueryProcessor;
import org.cdlib.xtf.textEngine.QueryRequest;
import org.cdlib.xtf.textEngine.QueryRequestParser;
import org.cdlib.xtf.textEngine.QueryResult;
import org.cdlib.xtf.util.AttribList;
import org.cdlib.xtf.util.EasyNode;
import org.cdlib.xtf.util.XMLFormatter;
import org.cdlib.xtf.util.XMLWriter;

import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLParser;

/**
 * The SRU servlet coordinates the process of parsing a URL query,
 * activating the textEngine to find all occurrences, and finally formatting
 * the results.
 */
public class SRU extends CrossQuery
{
    // inherit JavaDoc
    protected String getConfigName() { return "conf/sru.conf"; }
    

    // inherit JavaDoc
    protected TextConfig readConfig( String configPath )
        throws Exception
    {
        // Load the configuration file.
        config = new SRUConfig( configPath );
        
        // And we're done.
        return config;
    } // readConfig()


    // inherit JavaDoc
    protected TextConfig getConfig() { return config; }

        
    // inherit JavaDoc
    public String getServletInfo() {
        return "SRU servlet";
    } // getServletInfo()

    
    // inherit JavaDoc
    protected void apply( AttribList          attribs,
                          HttpServletRequest  req, 
                          HttpServletResponse res )
        throws Exception
    {
        // Switch the default output mode to XML.
        res.setContentType("text/xml");
      
        // Generate a query request document from the queryParser stylesheet.
        NodeInfo queryReqDoc = (NodeInfo) generateQueryReq( req, attribs );
        
        // If it actually contains an SRW explain response, or an SRW
        // diagnostic, simply output that directly.
        //
        EasyNode node = new EasyNode( queryReqDoc );
        if( directOutput(node, "diagnostics", res) )
            return;
        if( directOutput(node, "explainResponse", res) )
            return;
        
        // Process it to generate result document hits
        QueryProcessor proc     = createQueryProcessor();
        QueryRequest   queryReq = new QueryRequestParser().parseRequest(
                                          queryReqDoc, 
                                          new File(getRealPath("")) ); 
        QueryResult    result   = proc.processRequest( queryReq );
        
        // Format the hits for the output document.
        formatHits( req, res, attribs, result, queryReq.displayStyle );
    }
    
    
    /**
     * Scans the node and its descendants for an SRW 'explainResponse' or
     * 'diagnostics'. If found, it is output directly.
     * 
     * @param node    Node to scan
     * @param name    Name to scan for
     * @return        true if direct output was made
     */
    private boolean directOutput( EasyNode node, 
                                  String name, 
                                  HttpServletResponse res )
        throws IOException
    {
        // If the node is an explainResponse or diagnostic, output it
        // directly.
        //
        if( name.equals(node.name()) ) {
            String strVal = XMLWriter.toString( node );
            res.getWriter().print( strVal );
            return true;
        }
        
        // Scan the children.
        for( int i = 0; i < node.nChildren(); i++ ) {
            if( directOutput(node.child(i), name, res) )
                return true;
        }
        
        // None found in this branch.
        return false;
              
    } // directOutput()
    
      
    /**
     * Break 'val' up into its component tokens and add elements for them.
     * Treats the SRU/SRW 'query' parameter specially, parsing it as CQL.
     * 
     * @param fmt formatter to add to
     * @param name Name of the URL parameter
     * @param val value to tokenize
     */
    protected void tokenize( XMLFormatter fmt, String name, String val )
    {
        // Treat all URL parameters except 'query' normally.
        if( !name.equals("query") ) {
            super.tokenize( fmt, name, val );
            return;
        }
        
        // Got a CQL query. Translate it to XCQL (the XML version of CQL).
        CQLParser parser = new CQLParser();
        try {
            CQLNode parsed = parser.parse( val );
            String text = parsed.toXCQL( fmt.tabCount() / 2 );
            fmt.rawText( text );
        }
        catch( org.z3950.zing.cql.CQLParseException e ) {
            throw new CQLParseException( e.getMessage() );
        }
        catch( IOException e ) {
            throw new RuntimeException( e );
        }
        
    } // tokenize()

} // class SRU
