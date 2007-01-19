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
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.tree.TreeBuilder;

import org.cdlib.xtf.servletBase.TextConfig;
import org.cdlib.xtf.servletBase.TextServlet;
import org.cdlib.xtf.servletBase.RedirectException;
import org.cdlib.xtf.textEngine.IndexUtil;
import org.cdlib.xtf.textEngine.QueryProcessor;
import org.cdlib.xtf.textEngine.QueryRequest;
import org.cdlib.xtf.textEngine.QueryRequestParser;
import org.cdlib.xtf.textEngine.QueryResult;
import org.cdlib.xtf.util.AttribList;
import org.cdlib.xtf.util.Trace;
import org.cdlib.xtf.util.XMLFormatter;
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
    protected CrossQueryConfig config;
    
    /** Used to format decimal numbers */
    protected static DecimalFormat decimalFormat = new DecimalFormat();

    /**
     * Called by the superclass to find out the name of our specific config
     * file.
     */
    protected String getConfigName() { return "conf/crossQuery.conf"; }


    /**
     * Loads the specific configuration file for crossQuery.
     */
    protected TextConfig readConfig( String configPath )
    {
        // Load the configuration file.
        config = new CrossQueryConfig( this, configPath );

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

            // Set the default output content type
            res.setContentType("text/html");

            // If in step mode, output the frameset and top frame...
            String stepStr = stepSetup( req, res );
            if( stepStr != null ) {
                ServletOutputStream out = res.getOutputStream();
                out.println( stepStr );
                out.close();
                return;
            }
            
            // Output extended debugging info if requested.
            Trace.debug( "Processing request: " + getRequestURL(req) );

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

            // This does the bulk of the work.
            apply( attribs, req, res );
        }
        catch( Exception e ) {
            if( !(e instanceof RedirectException) ) {
                try {
                    genErrorPage( req, res, e );
                }
                catch( RedirectException re ) { }
            }
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
        // Record the start time.
        long startTime = System.currentTimeMillis();
        
        // If a query router was specified, run it.
        QueryRoute queryRoute = runQueryRouter( req, res, attribs );
        if( queryRoute == null )
            return;
        
        // Generate a query request.
        QueryRequest queryReq = runQueryParser( req, res, queryRoute, attribs );
        if( queryReq == null )
            return;
        
        // Process it to generate result document hits
        QueryProcessor proc = createQueryProcessor();
        QueryResult queryResult = proc.processRequest( queryReq ); 
        
        // Format the hits for the output document.
        formatHits( "crossQueryResult", req, res, attribs, 
                    queryReq, queryResult, startTime );

    } // apply()


    /**
     * Creates a query request using the queryParser stylesheet and the given
     * attributes.
     *
     * @param req          The original HTTP request
     * @param res          The HTTP response (used for step mode only)
     * @param attribs      Attributes to pass to the stylesheet.
     * @return             A route to the parser, or null if before that step
     */
    protected QueryRoute runQueryRouter( HttpServletRequest  req,
                                         HttpServletResponse res,
                                         AttribList          attribs )
        throws Exception
    {
        String step = req.getParameter( "debugStep" );
        
        // If no router specified but a parser was, return a default route.
        // This is for backward compatibility.
        //
        if( config.queryRouterSheet == null ) {
            if( "1b".equals(step) ) {
                res.setContentType("text/html");
                res.getOutputStream().println( 
                    "queryRouter stylesheet not specified;<br/> " +
                    "using default route to: " + config.queryParserSheet );
                return null;
            }
            
            return QueryRoute.createDefault( config.queryParserSheet );
        }
      
        // Make a <parameters> block, without tokenizing
        XMLFormatter fmt = new XMLFormatter();
        fmt.blankLineAfterTag( false );
        buildParamBlock( attribs, fmt, null, null );

        // If in step 1, just output the parameter block.
        if( "1b".equals(step) ) {
            res.setContentType("text/xml");
            res.getOutputStream().println( fmt.toString() );
            return null;
        }
        
        // Locate the stylesheet and make a tranformer.
        Templates   sheet = stylesheetCache.find( config.queryRouterSheet );
        Transformer trans = sheet.newTransformer();

        // Stuff all the common config properties into the transformer in
        // case the query router needs access to them.
        //
        stuffAttribs( trans, config.attribs );

        // Also stuff the URL parameters, in case it wants them that way.
        stuffAttribs( trans, attribs );

        // Add the special computed attributes.
        stuffSpecialAttribs( req, trans );
        
        if( Trace.getOutputLevel() >= Trace.debug ) {
            String tmp = fmt.toString();
            if( tmp.endsWith("\n") ) tmp = tmp.substring(0, tmp.length()-1);
            Trace.debug( "*** queryRouter input ***\n" + tmp );
        }

        // Make sure errors get directed to the right place.
        if( !(trans.getErrorListener() instanceof XTFSaxonErrorListener) )
            trans.setErrorListener( new XTFSaxonErrorListener() );

        // Now perform the transformation.
        TreeBuilder output = new TreeBuilder();
        trans.transform( fmt.toSource(), output );

        // Get the result.
        Source queryRouteDoc = output.getCurrentRoot();
        
        if( Trace.getOutputLevel() >= Trace.debug ) {
            Trace.debug( "*** queryRouter output ***\n" +
                         XMLWriter.toString(queryRouteDoc, false) );
        }
        
        // Parse it into the final route.
        QueryRoute route = QueryRoute.parse( (NodeInfo) queryRouteDoc );
        
        // Translate relative path, if necessary.
        route.queryParserSheet = getRealPath( route.queryParserSheet );
        
        // Record extra stuff for debugging/step mode
        route.routerInput = fmt.toString();
        route.routerOutput = XMLWriter.toString( queryRouteDoc, false );
        
        // All done.
        return route;
      
    } // runQueryRouter()

     
    /**
     * Creates a query request using the queryParser stylesheet and the given
     * attributes.
     *
     * @param req          The original HTTP request
     * @param res          The HTTP response (used for step mode only)
     * @param route        Route to the query parser
     * @param attribs      Attributes to pass to the stylesheet.
     * @return             A parsed query request, or null if before that step
     */
    protected QueryRequest runQueryParser( HttpServletRequest  req,
                                           HttpServletResponse res,
                                           QueryRoute          route,
                                           AttribList          attribs )
        throws Exception
    {
        // Make a <parameters> block.
        XMLFormatter fmt = new XMLFormatter();
        fmt.blankLineAfterTag( false );
        buildParamBlock( attribs, fmt, 
                         route.tokenizerMap, route.routerOutput );
        
        // If in step 2, just output the parameter block.
        String step = req.getParameter( "debugStep" );
        if( "2b".equals(step) ) {
            res.setContentType("text/xml");
            res.getOutputStream().println( fmt.toString() );
            return null;
        }
        
        // Locate the query formatting stylesheet.
        Templates genSheet = stylesheetCache.find( route.queryParserSheet );

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
        
        if( Trace.getOutputLevel() >= Trace.debug ) {
            String tmp = fmt.toString();
            if( tmp.endsWith("\n") ) tmp = tmp.substring(0, tmp.length()-1);
            Trace.debug( "*** queryParser input ***\n" + tmp );
        }

        // Make sure errors get directed to the right place.
        if( !(trans.getErrorListener() instanceof XTFSaxonErrorListener) )
            trans.setErrorListener( new XTFSaxonErrorListener() );

        // Now perform the transformation.
        TreeBuilder output = new TreeBuilder();
        trans.transform( fmt.toSource(), output );

        // Get the result.
        Source queryReqDoc = output.getCurrentRoot();
        
        // Output useful debug info
        if( Trace.getOutputLevel() >= Trace.debug ) {
            Trace.debug( "*** queryParser output ***\n" + 
                         XMLWriter.toString(queryReqDoc, false) );
        }

        // Shunt if necessary (for instance, in step mode)
        if( shuntQueryReq(req, res, queryReqDoc) )
            return null;

        // Process it to generate result document hits
        QueryRequest queryReq = new QueryRequestParser().parseRequest(
                                       queryReqDoc, new File(getRealPath("")) );
        
        // Fill in the auxiliary info
        queryReq.parserInput  = fmt.toString();
        queryReq.parserOutput = XMLWriter.toString( queryReqDoc, false );
        
        // All done.
        return queryReq;
      
    } // runQueryParser()

     
    /**
     * Called right after the raw query request has been generated, but
     * before it is parsed. Gives us a chance to stop processing here in
     * step mode.
     */
    protected boolean shuntQueryReq( HttpServletRequest req,
                                     HttpServletResponse res,
                                     Source queryReqDoc )
        throws IOException
    {
        // If we're on step 2b, simply output the query request.
        String step = req.getParameter( "debugStep" );
        if( "3b".equals(step) ) {
            res.setContentType("text/xml");
            res.getOutputStream().println( XMLWriter.toString(queryReqDoc) );
            return true;
        }
        
        return false;
        
    } // shuntQueryReq()
    
    
    /**
     * Formats a list of hits using the resultFormatter stylesheet.
     *
     * @param mainTagName   Name of the top-level tag to generate (e.g.
     *                      "crossQueryResult", etc.)
     * @param req           The original HTTP request
     * @param res           Where to send the HTML response
     * @param attribs       Parameters to pass to the stylesheet
     * @param queryRequest  Query request that produced the hits
     * @param queryResult   Hits resulting from the query request
     * @param startTime     Time (in milliseconds) request began
     */
    protected void formatHits( String              mainTagName,
                               HttpServletRequest  req,
                               HttpServletResponse res,
                               AttribList          attribs,
                               QueryRequest        queryRequest,
                               QueryResult         queryResult,
                               long                startTime )
        throws Exception
    {
        // Locate the display stylesheet.
        Templates displaySheet = stylesheetCache.find( queryRequest.displayStyle );

        // Figure out the output mime type
        res.setContentType(calcMimeType(displaySheet));

        // Make a transformer for this specific query.
        Transformer trans = displaySheet.newTransformer();

        // Stuff all the common config properties into the transformer in
        // case the query generator needs access to them.
        //
        stuffAttribs( trans, config.attribs );

        // Also stuff the URL parameters (in case stylesheet wants them)
        stuffAttribs( trans, attribs );

        // Add the special computed parameters.
        stuffSpecialAttribs( req, trans );

        // Make an input document for it based on the document hits. Insert
        // an attribute documenting how long the query took, including
        // formatting the hits.
        //
        StringBuffer extraStuff = new StringBuffer();
        if( queryRequest.parserInput != null )
            extraStuff.append( queryRequest.parserInput );
        if( queryRequest.parserOutput != null )
            extraStuff.append( queryRequest.parserOutput );
        String hitsString = queryResult.hitsToString( mainTagName, extraStuff.toString() );
        String prefix = "<" + mainTagName + " ";
        assert hitsString.startsWith( prefix );
        long queryTime = System.currentTimeMillis() - startTime;
        String formattedTime = decimalFormat.format( queryTime / 1000.0 );
        hitsString = prefix + "queryTime=\"" + formattedTime + "\" " +
            hitsString.substring(prefix.length());
        Source sourceDoc = new StreamSource( new StringReader(hitsString) );

        // If we are in raw mode (or on step 4 in step mode), use a null 
        // transform instead of the stylesheet.
        //
        String raw = req.getParameter( "raw" );
        String step = req.getParameter( "debugStep" );
        if( "yes".equals(raw) || "true".equals(raw) || "1".equals(raw) ||
            "4b".equals(step) )
        {
            res.setContentType("text/xml");

            trans = IndexUtil.createTransformer();
            Properties props = trans.getOutputProperties();
            props.put( "indent", "yes" );
            props.put( "method", "xml" );
            trans.setOutputProperties( props );
        }

        // Make sure errors get directed to the right place.
        if( !(trans.getErrorListener() instanceof XTFSaxonErrorListener) )
            trans.setErrorListener( new XTFSaxonErrorListener() );

        // Do it!
        trans.transform( sourceDoc, createFilteredReceiver(trans, req, res) );
    } // formatHits()

    
    /**
     * Checks if step mode is active and in the setup phase. If so, outputs
     * the frameset and information frames.
     * 
     * @param     req            The HTTP request (in)
     * @param     res            The HTTP response (out)
     * 
     * @return A string to output if in step setup phase, null to do normal 
     *         processing
     */
    protected String stepSetup( HttpServletRequest  req,
                                HttpServletResponse res) 
        throws IOException
    {
        String baseUrl = res.encodeURL( getRequestURL(req) );
        baseUrl = baseUrl.replaceAll("\"", "&quot;"); // because we're embedding in HTML
            
        String step = req.getParameter( "debugStep" );
        if( step == null || step.length() == 0 )
            return null;
        
        // Output the frame set, with two frames: one for info, one for data.
        if( step.matches("^[0-9]$") ) 
        {
            String urlA = baseUrl.replaceAll( "debugStep="+step, "debugStep="+step+"a" );
            String urlB = baseUrl.replaceAll( "debugStep="+step, "debugStep="+step+"b" );
            
            return 
                "<html>\n" +
                "  <head>\n" +
                "    <title>crossQuery Step " + step + "</title>\n" +
                "  </head>\n" +
                "  <frameset rows=\"195,*\" border=\"2\" framespacing=\"2\" " +
                            "frameborder=\"1\">\n" +
                "    <frame title=\"Info\" name=\"info\" src=\"" + urlA + "\">\n" +
                "    <frame title=\"Data\" name=\"data\" src=\"" + urlB + "\">\n" +
                "  </frameset>\n" +
                "</html>";
        }
        
        // Output the contents of the info frame
        if( step.matches("^[0-9]a$") ) {
            int stepNum = Integer.parseInt( step.substring(0,1) );
            StringBuffer out = new StringBuffer();
            out.append( 
                "<html>\n" +
                "  <body>\n" +
                "    <b><i>crossQuery</b></i> Step " + stepNum + " &nbsp;&nbsp; " );
            
            String prevUrl = (stepNum == 1) ? null :
                baseUrl.replaceAll( "debugStep="+step, "debugStep="+(stepNum-1) );
            String nextUrl = (stepNum == 5) ? null :
                baseUrl.replaceAll( "debugStep="+step, "debugStep="+(stepNum+1) );
              
            if( stepNum > 1 )
                out.append( "<a href=\"" + prevUrl + "\" target=\"_top\">[Previous]</a> " );
            else
                out.append( "<font color=\"#C0C0C0\">[Previous]</font> " );
            
            if( stepNum < 5 )
                out.append( "<a href=\"" + nextUrl + "\" target=\"_top\">[Next]</a>" );
            else
                out.append( "<font color=\"#C0C0C0\">[Next]</font>" );
            
            out.append(
                "    <table cellspacing=\"5\" cellpadding=\"0\">\n" +
                "      <tr>\n" );
            
            for( int i = 1; i <= 5; i++ ) {
                if( i == stepNum )
                    out.append( "<td bgcolor=\"#E0E0E0\"><b>" );
                else
                    out.append( "<td>" );

                if( i != stepNum ) {
                    String link = baseUrl.replaceAll( "debugStep="+step, "debugStep="+i );
                    out.append( "<a href=\"" + link + "\" target=\"_top\">" );
                }
                out.append( "Step " + i + "<br>" );
                if( i != stepNum )
                    out.append( "</a>" );

                switch( i ) {
                case 1: out.append( "Raw URL parameters" ); break;
                case 2: out.append( "Tokenized URL parameters" ); break;
                case 3: out.append( "XML query" ); break;
                case 4: out.append( "Raw results" ); break;
                case 5: out.append( "Formatted results" ); break;
                }
                
                out.append( "</td>" );
                if( i < 5 ) {
                    out.append( "<td>--></td><td>" );
                    switch( i ) {
                    case 1: out.append( "Query Router" ); break;
                    case 2: out.append( "Query Parser" ); break;
                    case 3: out.append( "Text Engine" ); break;
                    case 4: out.append( "Result Formatter" ); break;
                    }
                    out.append( "</td><td>--></td>" );
                }
                
                if( i == stepNum )
                    out.append( "</b>" );
                out.append( "</td>\n" );
            }
            
            out.append(
                "      </tr>\n" +
                "    </table>\n" );
            
            switch( stepNum ) {
            case 1: 
                    if( config.queryRouterSheet == null ) {
                        out.append( "Step 1 is the raw URL parameters to be fed to the " +
                        "<b>Query Router</b> stylesheet. Since no query router was " +
                        "specified, the request will be routed automatically to " +
                        "<code><b>" + config.queryParserSheet + "</b></code>. Skip to " +
                        "<a href=\"" + nextUrl + "\" target=\"_top\">step 2</a>." );
                        break;
                    }
                    out.append( 
                        "In step 1, parameters specified in the URL are " +
                        "translated, without tokenizing, to an XML " +
                        "<code>&lt;parameters&gt;</code> " +
                        "block (shown below). Next, this will be fed to the " +
                        "<b>Query Router</b> stylesheet, <code><b>" +
                        config.queryRouterSheet + "</b></code>. The result " +
                        "should be the route to a query parser stylesheet in " +
                        "<a href=\"" + nextUrl + "\" target=\"_top\">step 2</a>." );
                    break;
            case 2: out.append( 
                        "In step 2, parameters specified in the URL are " +
                        "tokenized and translated to an XML " +
                        "<code>&lt;parameters&gt;</code> " +
                        "block (shown below). Next, this will be fed to the " +
                        "<b>Query Parser</b> stylesheet. The result " +
                        "should be an XML query in " +
                        "<a href=\"" + nextUrl + "\" target=\"_top\">step 3</a>." );
                    break;
            case 3: out.append( 
                        "Step 3: The URL parameters from " +
                        "<a href=\"" + prevUrl + "\" target=\"_top\">step 2</a> " +
                        "have now been processed by the <b>Query Parser</b> stylesheet " +
                        "into an XML query, shown below. Next, XTF's <b>Text Engine</b> " +
                        "will execute this query to produce the raw search " +
                        "results in " +
                        "<a href=\"" + nextUrl + "\" target=\"_top\">step 4</a>. " +
                        "Note that the final <b>Result Formatter</b> stylesheet " +
                        "(for step 5) is specified here as well." );
                    break;
            case 4: out.append( 
                        "In step 4, XTF's <b>Text Engine</b> has executed " +
                        "the XML query from " +
                        "<a href=\"" + prevUrl + "\" target=\"_top\">step 3</a> " +
                        "to produce raw search results, shown below. These will be " +
                        "fed in turn to the <b>Result Formatter</b> stylesheet " +
                        "to produce the final HTML page in " +
                        "<a href=\"" + nextUrl + "\" target=\"_top\">step 5</a>." );
                        break;
            case 5: out.append( 
                        "Step 5 shows the final HTML result produced by " +
                        "feeding the raw search results from " +
                        "<a href=\"" + prevUrl + "\" target=\"_top\">step 4</a> " +
                        "into the <b>Result Formatter</b> stylesheet." );
            }
            
            out.append(
                "  </body>\n" +
                "</html>" );
            
            return out.toString();
        }
        
        return null;
    } // stepSetup()
    
} // class CrossQuery
