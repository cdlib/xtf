package org.cdlib.xtf.crossQuery;

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
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Iterator;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.cdlib.xtf.servletBase.TextConfig;
import org.cdlib.xtf.servletBase.TextServlet;
import org.cdlib.xtf.textEngine.DocHit;
import org.cdlib.xtf.textEngine.QueryResult;
import org.cdlib.xtf.textEngine.QueryProcessor;
import org.cdlib.xtf.textEngine.QueryRequest;
import org.cdlib.xtf.textEngine.Snippet;
import org.cdlib.xtf.util.Attrib;
import org.cdlib.xtf.util.AttribList;
import org.cdlib.xtf.util.Trace;
import org.cdlib.xtf.util.XTFSaxonErrorListener;

/**
 * The crossQuery servlet coordinates the process of parsing a URL query,
 * activating the textEngine to find all occurrences, and finally formatting
 * the results.
 */
public class CrossQuery extends TextServlet
{
    /** Holds global servlet configuration info */
    private static CrossQueryConfig config;
    
    /**
     * Called by the superclass to find out the name of our specific config
     * file.
     */
    protected String getConfigName() { return "conf/crossQuery.conf"; }
    

    /**
     * Loads the specific configuration file for crossQuery.
     *
     * @throws Exception    If an error occurs reading config
     */
    protected TextConfig readConfig( String configPath )
        throws Exception
    {
        // Load the configuration file.
        config = new CrossQueryConfig( configPath );
        
        // And we're done.
        return config;
    } // readConfig()


    /**
     * Retrieves the current configuration information (that was read in by
     * readConfig()).
     */
    protected TextConfig getConfig() { return config; }

        
    /**
     * Handles the HTTP 'get' method. Initializes the servlet if nececssary,
     * then parses the HTTP request and processes it appropriately.
     *
     * @param     req            The HTTP request (in)
     * @param     res            The HTTP response (out)
     * @exception IOException    If unable to read an index or data file, or
     *                           if unable to write the output stream.
     */ 
    public void doGet( HttpServletRequest req, HttpServletResponse res )
        throws IOException
    {
        // Record a reference to the output stream for ease of use.
        ServletOutputStream out = res.getOutputStream();

        try {

            // Get the parameters out of the request structure.
            String clearCaches = req.getParameter( "clear-caches" );

            // If we've been asked to clear the caches, do it now, by simply
            // forcing a re-init.
            //
            // If not initialized yet, do it now.
            firstTimeInit( "yes".equals(clearCaches) );

            // Set the default output content type
            res.setContentType("text/html");

            // Output extended debugging info if requested.
            Trace.debug( "Processing request: " + 
                req.getRequestURL().toString() + "?" + req.getQueryString());
            
            // Translate the URL parameters to an AttribList
            AttribList attribs = new AttribList();
            Enumeration p = req.getParameterNames();
            while( p.hasMoreElements() ) {
                String name = (String) p.nextElement();
                
                // Deal with screwy URL encoding of Unicode strings on
                // many browsers. Someday we'll do this more robustly.
                //
                String value = req.getParameter( name );
                if( value.indexOf('\u00c2') >= 0 ||
                    value.indexOf('\u00c3') >= 0) 
                {
                    try {
                        byte[] bytes = value.getBytes("ISO-8859-1");
                        value = new String(bytes, "UTF-8");
                    }
                    catch( UnsupportedEncodingException e ) { }
                }
                attribs.put( name, value );
            }

            // This is useful so the stylesheet can be entirely 
            // portable... it can call itself in new URLs by simply using 
            // this path. Some servlet containers include the parameters, 
            // so strip those if present.
            //
            String uri = req.getRequestURI();
            if( uri.indexOf('?') >= 0 )
                uri = uri.substring(0, uri.indexOf('?') );
            attribs.put( "servlet.path", uri );

            // This does the bulk of the work.
            apply( attribs, req, res );
        } 
        catch( Exception e ) {
            genErrorPage( req, res, e );
            return;
        }
    } // doGet()


    /**
    * Informational method required by Servlet interface. Doesn't seem to
    * matter what it says.
    *
    * @return   A string describing this servlet.
    */
    public String getServletInfo() {
        return "crossQuery search servlet";
    } // getServletInfo()


    /**
    * Creates the query request, processes it, and formats the results.
    *
    * @param attribs    Attributes to pass to the stylesheets.
    * @param req        The original HTTP request
    * @param res        Where to send the response
    *
    * @exception Exception  Passes on various errors that might occur.
    */
    private void apply( AttribList          attribs,
                        HttpServletRequest  req, 
                        HttpServletResponse res )
        throws Exception
    {
        ServletOutputStream out = res.getOutputStream();
        
        // Generate a query request.
        QueryRequest mq = generateQueryReq( req, attribs );
        
        // Process it to generate result document hits
        QueryProcessor proc = new QueryProcessor();
        QueryResult result = proc.processReq(mq);
        
        // Format the hits for the output document.
        formatHits( req, res, attribs, result, mq.displayStyle );
        
    } // apply()

    
    /**
     * Creates a query request using the queryParser stylesheet and the given 
     * attributes.
     * 
     * @param req        The original HTTP request
     * @param attribs    Attributes to pass to the stylesheet.
     */
    private QueryRequest generateQueryReq( HttpServletRequest req, 
                                           AttribList attribs )
        throws Exception
    {
        // Locate the query formatting stylesheet.
        Templates genSheet = stylesheetCache.find( config.queryParserSheet );
        
        // Make a transformer for this specific query.
        Transformer trans = genSheet.newTransformer();
        
        // Stuff all the common config properties into the transformer in
        // case the query generator needs access to them.
        //
        stuffAttribs( trans, config.attribs );
        
        // Also stuff the URL parameters, in case it wants them that way
        // instead of tokenized.
        //
        stuffAttribs( trans, attribs );
        
        // Finally we can generate the query... it will do the rest of the work.
        return new QueryRequest( trans, attribs, new File(getRealPath("")) );
    } // generateQueryReq()
    
    /**
     * Formats a list of hits using the resultFormatter stylesheet.
     * 
     * @param req           The original HTTP request
     * @param res           Where to send the HTML response
     * @param attribs       Parameters to pass to the stylesheet
     * @param result        Hits resulting from the query request
     * @param displayStyle  Path of the resultFormatter stylesheet
     */
    private void formatHits( HttpServletRequest  req,
                             HttpServletResponse res,
                             AttribList          attribs,
                             QueryResult         result,
                             String              displayStyle )
        throws Exception
    {
        // Locate the display stylesheet.
        Templates displaySheet = stylesheetCache.find( displayStyle );
        
        // Make a transformer for this specific query.
        Transformer trans = displaySheet.newTransformer();
        
        // Stuff all the common config properties into the transformer in
        // case the query generator needs access to them.
        //
        stuffAttribs( trans, config.attribs );
        
        // Also stuff the URL parameters (in case stylesheet wants them)
        stuffAttribs( trans, attribs );
        
        // Make an input document for it based on the document hits.
        Source sourceDoc = structureHits( result ); 
        
        // Make sure errors get directed to the right place.
        if( !(trans.getErrorListener() instanceof XTFSaxonErrorListener) )
            trans.setErrorListener( new XTFSaxonErrorListener() );

        // Do it!
        trans.transform( sourceDoc, new StreamResult(res.getOutputStream()) );
    } // formatHits()
    
    /**
     * Makes an XML document out of the list of document hits, and returns a
     * Source object that represents it.
     * 
     * @param result    Hits resulting from the query
     * @return          XML Source containing all the hits and snippets.
     */
    private Source structureHits( QueryResult result )
    {
        StringBuffer buf = new StringBuffer( 1000 );
        
        buf.append( "<crossQueryResult " +
                    "totalDocs=\"" + result.totalDocs + "\" " +
                    "startDoc=\"" + 
                        (result.totalDocs > 0 ? result.startDoc+1 : 0) + "\" " + 
                        // Note above: 1-based start
                    "endDoc=\"" + result.endDoc + "\">" );
        
        if( result.docHits != null ) {
            for( int i = 0; i < result.docHits.length; i++ ) {
                DocHit docHit = result.docHits[i];
                buf.append( "  <docHit rank=\"" + (i+result.startDoc+1) + 
                            "\" " +
                            "path=\"" + docHit.filePath() + "\" score=\"" +
                            Math.round(docHit.score() * 100) + "\">\n" );
                if( !docHit.metaData().isEmpty() ) {
                    buf.append( "    <meta>\n" );
                    for( Iterator atts = docHit.metaData().iterator(); atts.hasNext(); )
                    {
                        Attrib attrib = (Attrib) atts.next();
                        buf.append( "      <" + attrib.key + ">" );
                        buf.append( makeHtmlString(attrib.value, true) );
                        buf.append( "</" + attrib.key + ">\n" );
                    } // for atts
                    buf.append( "    </meta>\n" );
                }

                for( int j = 0; j < docHit.nSnippets(); j++ ) 
                {
                    Snippet  snippet = docHit.snippet( j, true );
                    buf.append( 
                        "    <snippet rank=\"" + (j+1) + "\" score=\"" + 
                        Math.round(snippet.score * 100) + "\"" );
                    
                    if( snippet.sectionType != null )
                        buf.append( " sectionType=\"" + snippet.sectionType + "\"" );
                    
                    buf.append( ">" + 
                        makeHtmlString(snippet.text, true) + 
                        "</snippet>\n" );
                }
                    
                buf.append( "  </docHit>\n\n" );
            } // for i
        }
            
        buf.append( "</crossQueryResult>\n" );
        
        // Now parse that into a document that can be fed to the stylesheet.
        String str = buf.toString();
        return new StreamSource( new StringReader(str) );
        
    } // structureHits()
    
} // class CrossQuery
