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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.saxon.Controller;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.trans.KeyManager;

import org.cdlib.xtf.servletBase.TextConfig;
import org.cdlib.xtf.servletBase.TextServlet;
import org.cdlib.xtf.textEngine.IdxConfigUtil;
import org.cdlib.xtf.textEngine.QueryProcessor;
import org.cdlib.xtf.util.DocTypeDeclRemover;
import org.cdlib.xtf.util.Trace;
import org.cdlib.xtf.util.XMLWriter;
import org.cdlib.xtf.util.XTFSaxonErrorListener;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import org.cdlib.xtf.cache.CacheDependency;
import org.cdlib.xtf.cache.Dependency;
import org.cdlib.xtf.lazyTree.LazyDocument;
import org.cdlib.xtf.lazyTree.LazyKeyManager;
import org.cdlib.xtf.lazyTree.LazyTreeBuilder;
import org.cdlib.xtf.lazyTree.PersistentTree;
import org.cdlib.xtf.lazyTree.SearchTree;

/**
 * Main dynaXML servlet. 
 *
 * Processes a URL requesting a document, using the docReqParser stylesheet
 * to parse the request and locate the document. Checks permissions based on 
 * the book being accessed and the requestor's IP address or other parameters. 
 * Performs optional text querying and search hit marking, and finally
 * transforms the annotated document using a display stylesheet to form the
 * final HTML result page.
 */
public class DynaXML extends TextServlet
{
    /** Caches docInfo lookups (based on their docId) */
    private static DocInfoCache docLookupCache;

    /** Handles authentication */
    static Authenticator authenticator;

    /** Holds global servlet configuration info */
    private static DynaXMLConfig config;
    
    /**
     * Gets a dependency on the docLookup cache for the given document.
     * Useful when making other cache entries that depend on the docLookup.
     *
     * @param docKey    The document being worked on
     * @return          A dependency on the doc lookup for that document.
     */
    public Dependency getDocDependency( LinkedList docKey )
    {
        if( getConfig().dependencyCheckingEnabled )
            return new CacheDependency( docLookupCache, docKey );
        else
            return null;

    } // getDocDependency()

    
    /**
     * Called by the superclass to find out the name of our specific config
     * file.
     */
    protected String getConfigName() { return "conf/dynaXML.conf"; }
    

    /**
     * Reads in the configuration file and sets up our helpers (caching,
     * authentication, etc.)
     *
     * @throws Exception    If an error occurs reading config
     */
    protected TextConfig readConfig( String configPath )
        throws Exception
    {
        // Load the configuration file.
        config = new DynaXMLConfig( configPath );
        
        // Create the caches
        docLookupCache = new DocInfoCache( this );

        // Create a helper for authentication.
        authenticator = new Authenticator( this );
        
        // And we're done.
        return config;
    } // readConfig()


    /**
     * Retrieves the current configuration information (that was read in by
     * readConfig()).
     */
    protected TextConfig getConfig() { return config; }

        
    /**
    * Retrieves the IP address of the client who is requesting a page from
    * this servlet. Handles un-reverse-proxying if necessary.
    *
    * @param    req     The HTTP request being processed
    *
    * @return           The IP address (e.g. 123.22.182.1), or empty string 
    *                   if not able to figure it out.
    */
    private String getClientIP( HttpServletRequest req )
    {
        // Start with the nominal IP address in the HTTP header.
        String ip = req.getRemoteAddr();
        Trace.debug( "Checking IP \"" + ip +
            "\" vs reverse proxy IP \"" + config.reverseProxyIP + "\"" );

        // If it matches the configured address of the reverse proxy, we have
        // to un-map it.
        //
        if( ip.equals(config.reverseProxyIP) ) {

            Trace.debug( "...matches reverseProxyIP" );

            // Normal reverse proxies store the real IP address in a standard
            // header. Check that first.
            //
            String header = req.getHeader( config.reverseProxyDefaultMarker );
            if( !isEmpty(header) ) {
                Trace.debug( "...using marker " + 
                    config.reverseProxyDefaultMarker + " -> " + header );
                ip = header;
            }

            // However, some proxies have a special header. If it's present,
            // override with that.
            //
            if( !isEmpty(config.reverseProxyMarker) ) {
                header = req.getHeader( config.reverseProxyMarker );
                if( !isEmpty(header) ) {
                    Trace.debug( "...using marker " + 
                        config.reverseProxyMarker + " -> " + header );
                    ip = header;
                }
            }
        }

        // Some broken proxies prepend "unknown" to the real IP address.
        // To work around these, skip all characters until we hit a digit.
        //
        while( ip.length() > 0 && !Character.isDigit(ip.charAt(0)) )
            ip = ip.substring( 1 );

        // All done!
        return ip;

    } // getClientIP()


    /**
     * Handles the HTTP 'get' method. Based on the HTTP request, produces an
     * appropriate response.
     *
     * @param     req            The HTTP request
     * @param     res            The HTTP response
     * @exception IOException    If unable to write the output stream.
     */ 
    public void doGet( HttpServletRequest req, HttpServletResponse res )
        throws IOException
    {
        try {

            // Get the parameters out of the request structure.
            String clearCaches = req.getParameter( "clear-caches" );
            String source      = req.getParameter( "source" );

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

            // Get the document info, given the current URL params.
            StringTokenizer st = new StringTokenizer( config.docLookupParams, 
                                                      " \t\r\n,;" );
            LinkedList docKey = new LinkedList();
            while( st.hasMoreTokens() ) {
                String name = st.nextToken();
                
                // Deal with screwy URL encoding of Unicode strings on
                // many browsers. Someday we'll do this more robustly.
                //
                String value = req.getParameter( name );
                if( value != null ) {
                    if( value.indexOf('\u00c2') >= 0 ||
                        value.indexOf('\u00c3') >= 0) 
                    {
                        try {
                            byte[] bytes = value.getBytes("ISO-8859-1");
                            value = new String(bytes, "UTF-8");
                        }
                        catch( UnsupportedEncodingException e ) { }
                    }
                    docKey.add( name );
                    docKey.add( value );
                }
            }
            
            DocInfo docInfo = docLookupCache.find( docKey );

            // If source overridden in the URL, make sure it's really
            // external.
            //
            if( !isEmpty(source) && source.startsWith("http://") ) {
                docInfo = new DocInfo(docInfo);
                docInfo.source = source;
            }
            else {

                // Check that the document actually exists.
                File docFile = new File( docInfo.source );
                if( !docFile.canRead() )
                    throw new InvalidDocumentException();
            }

            // Authenticate (if necessary)
            if( !authenticate(docKey, docInfo, req, res) )
                return;

            // This does the bulk of the work.
            apply( docInfo, req, res );
        } 
        catch( Exception e ) {
            genErrorPage( req, res, e );
            return;
        }
    } // doGet()


    /**
     * Performs user authentication for a request, given the authentication
     * info for the document.
     * 
     * @param docKey    Cache key for this document
     * @param docInfo   Info structure containing authentication parameters
     * @param req       The request being processed
     * @param res       Where to send results if authentication fails
     * 
     * @return          true iff authentication succeeds
     */
    protected boolean authenticate( LinkedList          docKey,
                                    DocInfo             docInfo,
                                    HttpServletRequest  req,
                                    HttpServletResponse res )
        throws Exception
    {
        // Determine the real IP address of the client.
        String ipAddr = getClientIP( req );
        
        // Check if this client has permission to access the document.
        // An exception thrown if not; false returned if a redirect
        // to an external page must happen first.
        //
        if( !authenticator.checkAuth( docKey, ipAddr, docInfo.authSpecs, 
                                      req, res ) )
        {
            return false;
        }
        
        return true;
    } // authenticate()
    
    /**
    * Informational method required by Servlet interface. Doesn't seem to
    * matter what it says.
    *
    * @return   A string describing this servlet.
    */
    public String getServletInfo() {
        return "dynaXML dynamic publishing servlet";
    } // getServletInfo()


    /**
    * Loads the source document, optionally performs a text search on it, and
    * then runs the document formatter to produce the final HTML result page.
    *
    * @param docInfo    Document information (stylesheet, source, etc.)
    * @param req        The original HTTP request
    * @param res        Where to send the HTML response
    *
    * @exception TransformerException  If there's an error in the stylesheet.
    * @exception IOException           If stylesheet or source can't be read.
    */
    private void apply( DocInfo             docInfo,
                        HttpServletRequest  req, 
                        HttpServletResponse res )
        throws Exception
    {
        ServletOutputStream out = res.getOutputStream();
        
        boolean dump = false;
        
        // First, load the stylesheet.
        Templates pss = stylesheetCache.find( docInfo.style );

        // Figure out the output mime type
        Properties details = pss.getOutputProperties();
        String mime = details.getProperty( OutputKeys.MEDIA_TYPE );
        if( mime == null ) {
            // Take a guess.
            res.setContentType("text/html");
        } else {
            res.setContentType(mime);
        }

        // Make a transformer and stuff it full of parameters. But if it's the
        // same stylesheet as we used last time in this thread, we can re-use
        // it for speed (keys won't have to be rebuilt.)
        //
        Transformer transformer = pss.newTransformer();
        stuffAttribs( transformer, req );
        stuffAttribs( transformer, config.attribs );

        // Also read the brand profile. It's just a simple stylesheet and we
        // stuff the output tags into parameters. They can be whatever the 
        // stylesheet writer desires.
        //
        readBranding( docInfo.brand, req, transformer );
        
        // Get the source document.
        Source sourceDoc = getSourceDoc( docInfo, transformer );
        
        // If we are in raw mode, use a null transform instead of the
        // stylesheet.
        //
        String raw = req.getParameter("raw");
        if( "yes".equals(raw) || "true".equals(raw) || "1".equals(raw) ) 
        {
            res.setContentType("text/xml");
            
            TransformerFactory factory = new net.sf.saxon.TransformerFactoryImpl();
            transformer = factory.newTransformer();
            Properties props = transformer.getOutputProperties();
            props.put( "indent", "yes" );
            props.put( "method", "xml" );
            transformer.setOutputProperties( props );
        }
            
        // Modify as necessary
        if( dump && sourceDoc instanceof PersistentTree )
            ((PersistentTree)sourceDoc).setAllPermanent( true );

        if( config.stylesheetProfiling && sourceDoc instanceof PersistentTree )
            ((PersistentTree)sourceDoc).enableProfiling( (Controller)transformer );

        // Make sure errors get directed to the right place.
        if( !(transformer.getErrorListener() instanceof XTFSaxonErrorListener) )
            transformer.setErrorListener( new XTFSaxonErrorListener() );
        
        // Now do the bulk of the work
        try {
            transformer.transform( sourceDoc, new StreamResult(out) );
        }
        catch( Exception e ) {
            if( dump && sourceDoc instanceof SearchTree ) {
                ((SearchTree)sourceDoc).pruneUnused();
                File file = new File( "C:\\tmp\\tree.dump" );
                Trace.info( "Dumping " + file.getAbsolutePath() );
                PrintWriter outWriter = 
                    new PrintWriter( 
                        new OutputStreamWriter(
                            new FileOutputStream(file), "UTF-8" ) );
                outWriter.println( XMLWriter.toString(sourceDoc) );
                outWriter.close();
            }
            throw e;
        }
        
        // Clean up.
        if( config.stylesheetProfiling && sourceDoc instanceof PersistentTree ) 
        {
            Trace.info( "Profile for request: " + 
                        req.getRequestURL().toString() + "?" + 
                        req.getQueryString() );
            Trace.tab();
            ((PersistentTree)sourceDoc).printProfile();
            Trace.untab();
            Trace.info( "End of profile." );
            
            ((PersistentTree)sourceDoc).disableProfiling();
        }
        
        // Debugging: dump search tree.
        if( dump && sourceDoc instanceof SearchTree ) {
            ((SearchTree)sourceDoc).pruneUnused();
            File file = new File( "C:\\tmp\\tree.dump" );
            Trace.info( "Dumping " + file.getAbsolutePath() );
            PrintWriter outWriter = 
                new PrintWriter( 
                    new OutputStreamWriter(
                        new FileOutputStream(file), "UTF-8" ) );
            outWriter.println( XMLWriter.toString(sourceDoc) );
            outWriter.close();
        }

        // It's a good idea to close disk-based trees when done using them.
        if( sourceDoc instanceof PersistentTree )
            ((PersistentTree)sourceDoc).close();
    } // apply()


    /**
     * Does the work of locating and loading the source document. Handles
     * fetching a file from a URL, lazy file, or a plain XML file on disk.
     * Also fires up a text query if requested.
     * 
     * @param docInfo       Tells which document to load, the query to
     *                      apply, tec.
     * @param transformer   The XSLT transformer that will be used on the
     *                      document.
     * 
     * @return              An XML Source object representing the loaded
     *                      document.
     * 
     * @throws IOException  If a problem is encountered loading a file or URL
     * @throws SAXException If the document cannot be parsed as valid XML
     * @throws ParserConfigurationException Miscellaneous configuration 
     *                                      problems
     */
    protected Source getSourceDoc( DocInfo docInfo, Transformer transformer )
        throws IOException, SAXException, ParserConfigurationException
    {
        // If no 'index' specified in the docInfo, then there's no way we can
        // find the lazy file.
        //
        boolean useLazy = true;
        if( docInfo.indexConfig == null || docInfo.indexName == null )
            useLazy = false;
        
        // If the source isn't a local file, we also can't use a lazy file.
        boolean isExternalFile = docInfo.source.startsWith("http:") ||
                                 docInfo.source.startsWith("https:");
        if( isExternalFile )
            useLazy = false;

        File lazyFile = null;
        if( useLazy ) {
            
            // Figure out where the lazy file is, and make sure it's actually
            // there and that we can read it.
            //
            try {
                lazyFile = IdxConfigUtil.calcLazyPath( 
                                                  new File(getRealPath("")),
                                                  new File(docInfo.indexConfig),
                                                  docInfo.indexName,
                                                  new File(docInfo.source),
                                                  false );
            }
            catch( Exception e ) { }
            if( lazyFile == null || !lazyFile.canRead() )
                useLazy = false;
        }

        // Get a source stream of events. It might come from a plain file,
        // URLConnection, or a lazy tree. If there is a query, it will also
        // perform search filtering.
        //
        Source sourceDoc = null;
        if( useLazy ) {
            try 
            {
                // If a query was specified, make a SearchTree; otherwise, make
                // a normal lazy tree.
                //
                if( docInfo.query != null ) {
                    String docKey = IdxConfigUtil.calcDocKey(
                                              new File(getRealPath("")),
                                              new File(docInfo.indexConfig),
                                              docInfo.indexName,
                                              new File(docInfo.source) );
                    SearchTree tree = new SearchTree( docKey, lazyFile );
                    tree.search( new QueryProcessor(), docInfo.query );
                    sourceDoc = tree;
                }
                else {
                    LazyTreeBuilder builder = new LazyTreeBuilder();
                    builder.setNamePool( NamePool.getDefaultNamePool() );
                    sourceDoc = builder.load( lazyFile );
                }
                
                // We want to print out any indexes being created, because
                // they should have all been done by the textIndexer.
                //
                ((LazyDocument)sourceDoc).setDebug( true );
            }
            catch( Exception e ) {
                Trace.error( "Error building tree: " + e.toString() );
                if( e instanceof IOException )
                    throw (IOException)e;
                throw new RuntimeException( e );
            }
            
            // We need a special key manager on the lazy tree, so that we can
            // use lazily stored keys on disk.
            //
            Controller c = (Controller) transformer;
            Executable e = c.getExecutable();
            KeyManager k = e.getKeyManager();
            if( !(k instanceof LazyKeyManager) )
                e.setKeyManager( new LazyKeyManager(k, c.getConfiguration()) );
            
            // All done.
            return sourceDoc;
            
        } // if( useLazy )
        
        // Can't use the lazy file... just read the original source file.
        XMLReader lastFilter = SAXParserFactory.newInstance().
            newSAXParser().getXMLReader();
        String url;
        
        InputSource inSrc;
        if( isExternalFile ) {
            url = docInfo.source;
            inSrc = new InputSource( url );
        }
        else {
            url = new File(docInfo.source).toURL().toString();
        
            InputStream inStream = new FileInputStream( docInfo.source );
            
            // Remove DOCTYPE declarations, since the XML reader will barf 
            // if it can't resolve the entity reference, and we really 
            // don't care one way or the other.
            //
            inStream = new DocTypeDeclRemover( inStream );
            
            inSrc = new InputSource( inStream );
            inSrc.setSystemId( url );
        }

        sourceDoc = new SAXSource( lastFilter, inSrc );
        
        // Can't perform queries without a lazy tree and its corresponding
        // index.
        //
        if( docInfo.query != null )
            throw new UnsupportedQueryException(); 
        
        // All done.
        return sourceDoc;
    } // getSourceDoc()
    
    
    /**
     * Tells the servlet whether to perform stylesheet profiling. The profile
     * is (currently) sent to Trace.info().
     * 
     * @param flag    If true, subsequent XSLT transformations will be
     *                profiled.
     */
    public void setProfiling( boolean flag ) {
        config.stylesheetProfiling = flag;
    }
    
} // class TextServlet
