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
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.tree.TreeBuilder;

import org.cdlib.xtf.servletBase.TextConfig;
import org.cdlib.xtf.servletBase.TextServlet;
import org.cdlib.xtf.textEngine.DocHit;
import org.cdlib.xtf.textEngine.IndexUtil;
import org.cdlib.xtf.textEngine.QueryProcessor;
import org.cdlib.xtf.textEngine.QueryRequest;
import org.cdlib.xtf.textEngine.QueryRequestParser;
import org.cdlib.xtf.textEngine.QueryResult;
import org.cdlib.xtf.textEngine.ResultField;
import org.cdlib.xtf.textEngine.ResultGroup;
import org.cdlib.xtf.textEngine.Snippet;
import org.cdlib.xtf.util.Attrib;
import org.cdlib.xtf.util.AttribList;
import org.cdlib.xtf.util.Trace;
import org.cdlib.xtf.util.XMLWriter;
import org.cdlib.xtf.util.XTFSaxonErrorListener;

/**
 * The crossQuery servlet coordinates the process of parsing a URL query,
 * activating the textEngine to find all occurrences, and finally formatting
 * the results.
 */
public class CrossQuery extends TextServlet
{
    /** Holds global servlet configuration info */
    protected static CrossQueryConfig config;

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
                // many browsers.
                //
                String value = req.getParameter( name );
                attribs.put( name, convertUTF8inURL(value) );
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
    protected void apply( AttribList          attribs,
                          HttpServletRequest  req,
                          HttpServletResponse res )
        throws Exception
    {
        // Generate a query request document from the queryParser stylesheet.
        Source queryReqDoc = generateQueryReq( req, attribs );

        // Process it to generate result document hits
        QueryProcessor proc     = createQueryProcessor();
        QueryRequest   queryReq = new QueryRequestParser().parseRequest(
                                         queryReqDoc,
                                         new File(getRealPath("")) );
        QueryResult    result   = proc.processRequest( queryReq ); 
        // Format the hits for the output document.
        formatHits( req, res, attribs, result, queryReq.displayStyle );

    } // apply()


    /**
     * Creates a query request using the queryParser stylesheet and the given
     * attributes.
     *
     * @param req        The original HTTP request
     * @param attribs    Attributes to pass to the stylesheet.
     */
    protected Source generateQueryReq( HttpServletRequest req,
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

        // Add the special computed attributes.
        stuffSpecialAttribs( req, trans );

        NodeInfo    input  = tokenizeParams( attribs );
        TreeBuilder output = new TreeBuilder();

        if( Trace.getOutputLevel() >= Trace.debug ) {
            Trace.debug( "*** queryParser input ***" );
            Trace.debug( XMLWriter.toString(input) );
        }

        // Make sure errors get directed to the right place.
        if( !(trans.getErrorListener() instanceof XTFSaxonErrorListener) )
            trans.setErrorListener( new XTFSaxonErrorListener() );

        // Now perform the transformation.
        trans.transform( input, output );

        // And return the output tree.
        return output.getCurrentRoot();
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
    protected void formatHits( HttpServletRequest  req,
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

        // If we are in raw mode, use a null transform instead of the
        // stylesheet.
        //
        String raw = req.getParameter("raw");
        if( "yes".equals(raw) || "true".equals(raw) || "1".equals(raw) )
        {
            res.setContentType("text/xml");

            trans = IndexUtil.createTransformer();
            Properties props = trans.getOutputProperties();
            props.put( "indent", "yes" );
            props.put( "method", "xml" );
            trans.setOutputProperties( props );
        }

        // Stuff all the common config properties into the transformer in
        // case the query generator needs access to them.
        //
        stuffAttribs( trans, config.attribs );

        // Also stuff the URL parameters (in case stylesheet wants them)
        stuffAttribs( trans, attribs );

        // Add the special computed parameters.
        stuffSpecialAttribs( req, trans );

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

        buf.append( "<crossQueryResult" +
                    " totalDocs=\"" + result.totalDocs + "\" " +
                    " startDoc=\"" +
                        (result.totalDocs > 0 ? result.startDoc+1 : 0) + "\" " +
                        // Note above: 1-based start
                    " endDoc=\"" + result.endDoc + "\">" );

        // Add the top-level doc hits.
        structureDocHits( result.docHits, result.startDoc, buf );

        // If grouping was specified, add that info too.
        if( result.fields != null ) 
        {
            // Process each field in turn
            for( int i = 0; i < result.fields.length; i++ ) 
            {
                ResultField field = result.fields[i];
                buf.append( 
                    "<groupedField field=\"" + field.field + "\" " +
                    "totalGroups=\"" + field.totalGroups + "\" " +
                    "startGroup=\"" + (field.endGroup > 0 ? field.startGroup+1 : 0) + "\" " +
                    "endGroup=\"" + (field.endGroup) + "\">" );
                if( field.groups == null )
                    continue;
                
                // Process each group within the field.
                for( int j = 0; j < field.groups.length; j++ ) {
                    ResultGroup group = field.groups[j];
                    buf.append( 
                        "<group value=\"" + group.value + "\" " +
                        "totalDocs=\"" + group.totalDocs + "\" " +
                        "startDoc=\"" + (group.endDoc > 0 ? group.startDoc+1 : 0) + "\" " +
                        "endDoc=\"" + (group.endDoc) + "\">" );
                    if( group.docHits != null )
                        structureDocHits( group.docHits, group.startDoc, buf );
                    buf.append( "</group>" );
                } // for j
                buf.append( "</groupedField>" );
            } // for i
        } // if
        
        // Add the final tag.
        buf.append( "</crossQueryResult>\n" );

        // Now parse that into a document that can be fed to the stylesheet.
        String str = buf.toString();
        return new StreamSource( new StringReader(str) );

    } // structureHits()


    /**
     * Does the work of turning DocHits into XML.
     * 
     * @param docHits Array of DocHits to structure
     * @param buf     Buffer to add the XML to
     */
    private void structureDocHits( DocHit[]     docHits, 
                                   int          startDoc, 
                                   StringBuffer buf ) 
    {
        if( docHits == null )
            return;
        
        for( int i = 0; i < docHits.length; i++ ) {
            DocHit docHit = docHits[i];
            buf.append( "  <docHit" +
                        " rank=\"" + (i+startDoc+1) + "\"" +
                        " path=\"" + docHit.filePath() + "\"" +
                        " score=\"" + Math.round(docHit.score * 100) + "\"" +
                        " totalHits=\"" + docHit.totalSnippets() + "\"" +
                        ">\n" );
            if( !docHit.metaData().isEmpty() ) {
                buf.append( "    <meta>\n" );
                for( Iterator atts = docHit.metaData().iterator(); atts.hasNext(); )
                {
                    Attrib attrib = (Attrib) atts.next();
                    buf.append( attrib.value );
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
            } // for j
  
            buf.append( "  </docHit>\n\n" );
        } // for i
        
    } // structureDocHits()

} // class CrossQuery
