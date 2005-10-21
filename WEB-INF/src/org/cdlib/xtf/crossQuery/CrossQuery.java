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
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.tree.TreeBuilder;

import org.cdlib.xtf.servletBase.TextConfig;
import org.cdlib.xtf.servletBase.TextServlet;
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
     *
     * @throws Exception    If an error occurs reading config
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
        long reqStartTime = 0;
        if( config == null || config.reportLatency )
            reqStartTime = System.currentTimeMillis();
        
        try {

            // Set the default output content type
            res.setContentType("text/html");

            // If in step mode, output the frameset and top frame...
            String stepStr = stepSetup( req );
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
        finally {
            if( config.reportLatency ) {
                long latency = System.currentTimeMillis() - reqStartTime;
                Trace.info( "Latency: " + latency + " msec for request: " +
                    getRequestURL(req) );
            }
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
        
        // Make a <parameters> block.
        XMLFormatter fmt = new XMLFormatter();
        tokenizeParams( attribs, fmt );

        // If in step 1, just output the parameter block.
        String step = req.getParameter( "debugStep" );
        if( "1b".equals(step) ) {
            res.setContentType("text/xml");
            res.getOutputStream().println( fmt.toString() );
            return;
        }
        
        // Generate a query request document from the queryParser stylesheet.
        Source queryReqDoc = generateQueryReq( req, attribs, fmt.toNode() );
        
        // If we're on step 2b, simply output the query request.
        if( "2b".equals(step) ) {
            res.setContentType("text/xml");
            res.getOutputStream().println( XMLWriter.toString(queryReqDoc) );
            return;
        }

        // Process it to generate result document hits
        QueryProcessor proc     = createQueryProcessor();
        QueryRequest   queryReq = new QueryRequestParser().parseRequest(
                                         queryReqDoc,
                                         new File(getRealPath("")) );
        QueryResult    result   = proc.processRequest( queryReq ); 
        
        // Format the hits for the output document.
        formatHits( "crossQueryResult",
                    req, res, attribs, result, 
                    queryReq.displayStyle, 
                    fmt.toString() + XMLWriter.toString(queryReqDoc, false),
                    startTime );

    } // apply()


    /**
     * Creates a query request using the queryParser stylesheet and the given
     * attributes.
     *
     * @param req          The original HTTP request
     * @param attribs      Attributes to pass to the stylesheet.
     * @param paramBlock   Tokenized versions of the input attributes
     */
    protected Source generateQueryReq( HttpServletRequest req,
                                       AttribList         attribs,
                                       NodeInfo           paramBlock )
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
        
        if( Trace.getOutputLevel() >= Trace.debug ) {
            Trace.debug( "*** queryParser input ***" );
            Trace.debug( XMLWriter.toString(paramBlock) );
        }

        // Make sure errors get directed to the right place.
        if( !(trans.getErrorListener() instanceof XTFSaxonErrorListener) )
            trans.setErrorListener( new XTFSaxonErrorListener() );

        // Now perform the transformation.
        TreeBuilder output = new TreeBuilder();
        trans.transform( paramBlock, output );

        // And return the output tree.
        return output.getCurrentRoot();
    } // generateQueryReq()

    /**
     * Formats a list of hits using the resultFormatter stylesheet.
     *
     * @param mainTagName   Name of the top-level tag to generate (e.g.
     *                      "crossQueryResult", etc.)
     * @param req           The original HTTP request
     * @param res           Where to send the HTML response
     * @param attribs       Parameters to pass to the stylesheet
     * @param result        Hits resulting from the query request
     * @param displayStyle  Path of the resultFormatter stylesheet
     * @param extraStuff    Additional XML to insert into the query
     *                      result document. Typically includes <parameters>
     *                      block and <query> block. If null, then a plain
     *                      result is created without any stuff added in.
     */
    protected void formatHits( String              mainTagName,
                               HttpServletRequest  req,
                               HttpServletResponse res,
                               AttribList          attribs,
                               QueryResult         result,
                               String              displayStyle,
                               String              extraStuff,
                               long                startTime )
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

        // Add the special computed parameters.
        stuffSpecialAttribs( req, trans );

        // Make an input document for it based on the document hits. Insert
        // an attribute documenting how long the query took, including
        // formatting the hits.
        //
        String hitsString = result.hitsToString( mainTagName, extraStuff );
        String prefix = "<" + mainTagName + " ";
        assert hitsString.startsWith( prefix );
        long queryTime = System.currentTimeMillis() - startTime;
        String formattedTime = decimalFormat.format( queryTime / 1000.0 );
        hitsString = prefix + "queryTime=\"" + formattedTime + "\" " +
            hitsString.substring(prefix.length());
        Source sourceDoc = new StreamSource( new StringReader(hitsString) );

        // If we are in raw mode (or on step 3 in step mode), use a null 
        // transform instead of the stylesheet.
        //
        String raw = req.getParameter( "raw" );
        String step = req.getParameter( "debugStep" );
        if( "yes".equals(raw) || "true".equals(raw) || "1".equals(raw) ||
            "3b".equals(step) )
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
        trans.transform( sourceDoc, new StreamResult(res.getOutputStream()) );
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
    protected String stepSetup( HttpServletRequest req ) throws IOException
    {
        String baseUrl = getRequestURL(req);
            
        String step = req.getParameter( "debugStep" );
        if( step == null || step.length() == 0 )
            return null;
        
        // Output the frame set, with two frames: one for info, one for data.
        if( step.matches("^1$|^2$|^3$|^4$") ) 
        {
            String urlA = baseUrl.replaceAll( "debugStep="+step, "debugStep="+step+"a" );
            String urlB = baseUrl.replaceAll( "debugStep="+step, "debugStep="+step+"b" );
            
            return 
                "<html>\n" +
                "  <head>\n" +
                "    <title>crossQuery Step " + step + "</title>\n" +
                "  </head>\n" +
                "  <frameset rows=\"150,*\" border=\"2\" framespacing=\"2\" " +
                            "frameborder=\"1\">\n" +
                "    <frame title=\"Info\" name=\"info\" src=\"" + urlA + "\">\n" +
                "    <frame title=\"Data\" name=\"data\" src=\"" + urlB + "\">\n" +
                "  </frameset>\n" +
                "</html>";
        }
        
        // Output the contents of the info frame
        if( step.matches("^1a$|^2a$|^3a$|^4a$") ) {
            int stepNum = Integer.parseInt( step.substring(0,1) );
            StringBuffer out = new StringBuffer();
            out.append( 
                "<html>\n" +
                "  <body>\n" +
                "    <b><i>crossQuery</b></i> Step " + stepNum + " &nbsp;&nbsp; " );
            
            String prevUrl = (stepNum == 1) ? null :
                baseUrl.replaceAll( "debugStep="+step, "debugStep="+(stepNum-1) );
            String nextUrl = (stepNum == 4) ? null :
                baseUrl.replaceAll( "debugStep="+step, "debugStep="+(stepNum+1) );
              
            if( stepNum > 1 )
                out.append( "<a href=\"" + prevUrl + "\" target=\"_top\">[Previous]</a> " );
            else
                out.append( "<font color=\"#C0C0C0\">[Previous]</font> " );
            
            if( stepNum < 4 )
                out.append( "<a href=\"" + nextUrl + "\" target=\"_top\">[Next]</a>" );
            else
                out.append( "<font color=\"#C0C0C0\">[Next]</font>" );
            
            out.append(
                "    <table cellspacing=\"12\" cellpadding=\"0\">\n" +
                "      <tr>\n" );
            
            for( int i = 1; i <= 4; i++ ) {
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
                case 1: out.append( "URL parameters" ); break;
                case 2: out.append( "XML query" ); break;
                case 3: out.append( "Raw results" ); break;
                case 4: out.append( "Formatted results" ); break;
                }
                
                out.append( "</td>" );
                if( i < 4 ) {
                    out.append( "<td>--> " );
                    switch( i ) {
                    case 1: out.append( "Query Parser" ); break;
                    case 2: out.append( "Text Engine" ); break;
                    case 3: out.append( "Result Formatter" ); break;
                    }
                    out.append( " --></td>" );
                }
                
                if( i == stepNum )
                    out.append( "</b>" );
                out.append( "</td>\n" );
            }
            
            out.append(
                "      </tr>\n" +
                "    </table>\n" );
            
            switch( stepNum ) {
            case 1: out.append( 
                        "In step 1, parameters specified in the URL are " +
                        "translated to an XML <code>&lt;parameters&gt;</code> " +
                        "block (shown below). Next, this will be fed to the " +
                        "<b>Query Parser</b> stylesheet, <code><b>" +
                        config.queryParserSheet + "</b></code>. The result " +
                        "should be an XML query in " +
                        "<a href=\"" + nextUrl + "\" target=\"_top\">step 2</a>." );
                    break;
            case 2: out.append( 
                        "Step 2: The URL parameters from " +
                        "<a href=\"" + prevUrl + "\" target=\"_top\">step 1</a> " +
                        "have now been translated by the <b>Query Parser</b> stylesheet " +
                        "into an XML query, shown below. Next, XTF's <b>Text Engine</b> " +
                        "will process this query to produce the raw search " +
                        "results in " +
                        "<a href=\"" + nextUrl + "\" target=\"_top\">step 3</a>. " +
                        "Note that the final <b>Result Formatter</b> stylesheet " +
                        "(for step 4) is specified here as well." );
                    break;
            case 3: out.append( 
                        "In step 3, XTF's <b>Text Engine</b> has processed " +
                        "the XML query from " +
                        "<a href=\"" + prevUrl + "\" target=\"_top\">step 2</a> " +
                        "to produce raw search results, shown below. These will be " +
                        "fed in turn to the <b>Result Formatter</b> stylesheet " +
                        "to produce the final HTML page in " +
                        "<a href=\"" + nextUrl + "\" target=\"_top\">step 4</a>." );
                        break;
            case 4: out.append( 
                        "Step 4 shows the final HTML result produced by " +
                        "feeding the raw search results from " +
                        "<a href=\"" + prevUrl + "\" target=\"_top\">step 3</a> " +
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
