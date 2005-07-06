package org.cdlib.xtf.servletBase;

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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.Iterator;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.tree.TreeBuilder;
import net.sf.saxon.value.StringValue;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.limit.ExcessiveWorkException;
import org.cdlib.xtf.textEngine.DefaultQueryProcessor;
import org.cdlib.xtf.textEngine.QueryProcessor;
import org.cdlib.xtf.textIndexer.XTFTextAnalyzer;
import org.cdlib.xtf.util.Attrib;
import org.cdlib.xtf.util.AttribList;
import org.cdlib.xtf.util.EasyNode;
import org.cdlib.xtf.util.GeneralException;
import org.cdlib.xtf.util.Path;
import org.cdlib.xtf.util.Trace;
import org.cdlib.xtf.util.XMLFormatter;
import org.cdlib.xtf.util.XTFSaxonErrorListener;

/**
 * Base class for the crossQuery and dynaXML servlets. Handles first-time
 * initialization, config file loading and some parsing, error handling, and
 * a few utility methods.
 */
public abstract class TextServlet extends HttpServlet 
{
    /** Caches stylesheets (based on their URL) */
    public static StylesheetCache stylesheetCache;

    /** Context useful for mapping partial paths to full paths */
    private static ServletContext staticContext;

    /** Base directory specified in servlet config (if any) */
    private static String baseDir;

    /** Flag to discern whether class has been initialized yet */
    private boolean isInitted = false;

    /** 
     * Last modification time of the configuration file, so we can decide
     * when we need to re-initialize the servlet.
     */
    private long configFileLastModified = 0;

    /** 
     * During tokenization, the '*' wildcard has to be changed to a word
     * to keep it from being removed.
     */
    private static final String SAVE_WILD_STAR = "jwxbkn";

    /** 
     * During tokenization, the '?' wildcard has to be changed to a word
     * to keep it from being removed.
     */
    private static final String SAVE_WILD_QMARK   = "vkyqxw";


    /** 
     * Extracts all of the text data from a tree element node.
     *
     * @param element   element to get text from
     * @return          Concatenated text from the element.
     */
    public static String getText( EasyNode element )
    {
        String text = "";
        for( int i = 0; i < element.nChildren(); i++ ) {
            EasyNode n = element.child( i );
            if( !n.isText() )
                continue;
            text = text + n.toString();
        }

        return text.trim();
    }


    /**
     * Translate a partial filesystem path to a full path.
     *
     * @param partialPath   A partial (or full) path
     * @return              The full path
     */
    public static String getRealPath( String partialPath )
    {
        if( staticContext == null )
            return partialPath;
        
        if( partialPath.startsWith("http://") )
            return partialPath;
        if( partialPath.startsWith("/") || partialPath.startsWith("\\") )
            return partialPath;
        if( partialPath.length() > 1 && partialPath.charAt(1) == ':' )
            return partialPath;
        if( !isEmpty(baseDir) )
            return Path.resolveRelOrAbs( baseDir, partialPath );
        return staticContext.getRealPath( partialPath );
    } // getRealPath()

    /**
     * Utility function - check if string is null or ""
     *
     * @param s     String to check
     * @return      true if the string is null or the empty string ("")
     */
    public static boolean isEmpty( String s )
    {
        return (s == null || s.equals(""));
    }


    /**
     * Utlity function - if the value is null, throws an exception.
     *
     * @param value     The value to check for null
     * @param descrip   If exception is thrown, this will be the message.
     * @throws GeneralException    Only if the value is null
     */
    public static void requireOrElse( String value, String descrip )
        throws GeneralException
    {
        if( isEmpty(value) )
            throw new GeneralException( descrip );
    } // requireOrElse()

    
    /**
     * Ensures that the servlet has been properly initialized. If init()
     * hasn't been called yet, or if the config file changes, then this
     * method reads the config file, then calls derivedInit().
     * 
     * @throws Exception    If an error occurs reading config
     */
    protected final void firstTimeInit( boolean forceInit )
        throws Exception
    {
        // Even if multiple instances get called at the same time, make sure
        // that we init once and only once.
        //
        synchronized( getClass() ) {

            // Record the context so we can easily translate paths.
            staticContext = getServletContext();

            // If a base directory was specified, record it.
            baseDir = getServletConfig().getInitParameter( "base-dir" );
            if( !isEmpty(baseDir) && !baseDir.endsWith("/") )
                baseDir = baseDir + "/";

            // If the modification time of the configuration file has
            // changed, we need to re-initialize.
            //
            String configPath  = getRealPath( getConfigName() );
            File configFile = new File(configPath);
            if( configFileLastModified > 0 &&
                configFile.lastModified() != configFileLastModified ) 
            {
                stylesheetCache.clear();
                isInitted = false;
            }
            configFileLastModified = configFile.lastModified();

            // Force reinitialization if requested
            if( forceInit )
                isInitted = false;
            
            // Only init once.
            if( isInitted )
                return;

            // Read in the configuration file.
            TextConfig config = readConfig( configPath );

            // Set up the Trace facility. We want timestamps, and we use a
            // format that matches Resin's.
            //
            Trace.printTimestamps( true );
            Trace.dateFormat = new SimpleDateFormat( "HH:mm:ss.SSS" );
            
            // Make sure output lines get flushed immediately, since we may
            // be sharing the log file with other servlets.
            //
            Trace.setAutoFlush( true );

            // Establish the trace output level.
            Trace.setOutputLevel( 
                (config.logLevel.equals("silent"))   ? Trace.silent :
                (config.logLevel.equals("errors"))   ? Trace.errors :
                (config.logLevel.equals("warnings")) ? Trace.warnings :
                (config.logLevel.equals("debug"))    ? Trace.debug :
                Trace.info );

            // And let everyone know the servlet has restarted
            Trace.error( "" );
            Trace.error( "*** SERVLET RESTART: " + getServletInfo() + " ***" );
            Trace.error( "" );
            Trace.error( "Log level: " + config.logLevel );

            // Create the caches
            stylesheetCache = new StylesheetCache( 
                config.stylesheetCacheSize,
                config.stylesheetCacheExpire,
                config.dependencyCheckingEnabled );

            // Mark the flag so we won't init again.
            isInitted = true;
        }
    } // firstTimeInit()
    
    
    /**
     * Derived classes must supply this method. It is the main entry point for
     * processing an HTTP request.
     */
    public abstract void doGet( HttpServletRequest req, 
                                HttpServletResponse res )
        throws IOException;


    /**
     * Derived classes must supply this method. Simply returns the relative
     * path name of the configuration file (e.g. "conf/dynaXml.conf").
     */
    protected abstract String getConfigName();

    
    /** 
     * Derived classes must supply this method. It reads in the servlet's
     * configuration file, and performs any derived class initialization
     * as necessary.
     * 
     * @param path        Path to the configuration file
     * @return            Parsed config information
     */
    protected abstract TextConfig readConfig( String path )
        throws Exception;


    /** 
     * Derived classes must supply this method. It simply returns the
     * configuration info that was read previously by readConfig()
     */
    protected abstract TextConfig getConfig();


    /**
     * Adds all URL attributes from the request into a transformer.
     *
     * @param   trans   The transformer to stuff the parameters in
     * @param   req     The request containing the parameters
     */
    public static void stuffAttribs( Transformer trans, 
                                     HttpServletRequest req )
    {
        Enumeration p = req.getParameterNames();
        while( p.hasMoreElements() ) {
            String name = (String) p.nextElement();
            String value = req.getParameter( name );
            
            // Skip parameters with empty values.
            if( value == null || value.length() == 0 )
                continue;
            
            // Deal with screwy URL encoding of Unicode strings on
            // many browsers.
            //
            value = convertUTF8inURL( value );
            trans.setParameter( name, new StringValue(value) );
        }
        
        stuffSpecialAttribs( req, trans );
        
    } // stuffAttribs()

    /**
     * Adds all the attributes in the list to the transformer as parameters
     * that can be used by the stylesheet.
     *
     * @param   trans   The transformer to stuff the parameters in
     * @param   list    The list containing attributes to stuff
     */
    public static void 
    stuffAttribs( Transformer trans, AttribList list )
    {
        for( Iterator i = list.iterator(); i.hasNext(); ) {
            Attrib a = (Attrib) i.next();
            if( a.value == null || a.value.length() == 0 )
                continue;
            trans.setParameter( a.key, new StringValue(a.value) );
        }
    } // stuffAttribs()


    /**
     * Calculates and adds the "servlet.path" and "root.path" attributes 
     * to the given transformer.
     */
    public static void 
    stuffSpecialAttribs( HttpServletRequest req, Transformer trans )
    {
    
        // This is useful so the stylesheet can be entirely portable... it
        // can call itself in new URLs by simply using this path. Some servlet
        // containers include the parameters, so strip those if present.
        //
        String uri = req.getRequestURL().toString();
        if( !uri.startsWith("http") )
            uri = req.getRequestURI();
        if( uri.indexOf('?') >= 0 )
            uri = uri.substring(0, uri.indexOf('?') );
        trans.setParameter( "servlet.path", new StringValue(uri) );
        
        // Another useful parameter is the path to this instance in the
        // servlet container, for other resources such as icons and
        // CSS files.
        //
        String rootPath = uri;
        if( rootPath.endsWith("/") )
            rootPath = rootPath.substring( 0, rootPath.length() - 1 );
        
        int slashPos = rootPath.lastIndexOf('/');
        if( slashPos >= 1 )
            rootPath = rootPath.substring( 0, slashPos );

        String lookFor = "/servlet";
        if( rootPath.endsWith(lookFor) )
            rootPath = rootPath.substring( 0, 
                                   rootPath.length() - lookFor.length() );
        
        rootPath = rootPath + "/";
        trans.setParameter( "root.path", new StringValue(rootPath) );
        
        // Stuff all the HTTP request parameters for the stylesheet to
        // use if it wants to.
        //
        Enumeration i = req.getHeaderNames();
        String url = req.getRequestURL().toString();
        if( req.getQueryString() != null && req.getQueryString().length() > 0 &&
            url.indexOf('?') < 0 )
        {
            url = url + "?" + req.getQueryString();
        }
        trans.setParameter( "http.URL", url );
        while( i.hasMoreElements() ) {
            String name = (String) i.nextElement();
            String value = req.getHeader( name );
            trans.setParameter( "http." + name, new StringValue(value) );
        }
        
    } // stuffSpecialAttribs()


    /**
     * Reads a brand profile (a simple stylesheet) and stuffs all the output 
     * tags into the specified transformer as parameters.
     *
     * @param path          Filesystem path to the brand profile
     * @param req           HTTP servlet request containing URL parameters
     * @param targetTrans   Where to stuff the attributes into
     * @throws Exception    If an error occurs loading or parsing the profile.
     */
    protected void readBranding( String             path, 
                                 HttpServletRequest req,
                                 Transformer        targetTrans )
        throws Exception
    {
        if( path == null || path.equals("") )
            return;

        // First, load the stylesheet.
        Templates pss = stylesheetCache.find( path );

        // Make a transformer and stuff it full of parameters.
        Transformer trans = pss.newTransformer();
        stuffAttribs( trans, req );
        stuffAttribs( trans, getConfig().attribs );
        
        // Make a tiny document for it to transform
        String doc = "<dummy>dummy</dummy>\n";
        StreamSource src = new StreamSource( new StringReader(doc) );
        
        // Make sure errors get directed to the right place.
        if( !(trans.getErrorListener() instanceof XTFSaxonErrorListener) )
            trans.setErrorListener( new XTFSaxonErrorListener() );

        // Now request it to give us the info we crave.
        TreeBuilder result = new TreeBuilder();
        trans.transform( src, result );

        // Process all the tags.
        EasyNode root = new EasyNode( result.getCurrentRoot() );
        for( int i = 0; i < root.nChildren(); i++ ) {
            EasyNode el = root.child( i );
            if( !el.isElement() )
                continue;

            String  tagName = el.name();
            String  strVal  = getText( el );

            targetTrans.setParameter( tagName, new StringValue(strVal) );

        } // for node

    } // readBranding()

    /**
     * Translates any HTML-special characters (like quote, ampersand, etc.)
     * into the corresponding code (like &amp;quot;)
     * 
     * @param s The string to transform
     */
    public static String makeHtmlString( String s )
    {
        return makeHtmlString( s, false );
    }

    /**
     * Translates any HTML-special characters (like quote, ampersand, etc.)
     * into the corresponding code (like &amp;quot;)
     * 
     * @param s The string to transform
     */
    public static String makeHtmlString( String s, boolean keepTags )
    {
        if( s == null )
            return "";
        
        StringBuffer buf = new StringBuffer();

        boolean inTag = false;
        
        for( int i = 0; i < s.length(); i++ ) {
            char c = s.charAt( i );

            if( keepTags && !inTag && c == '<' ) {
                inTag = true;
                buf.append( c );
                continue;
            }
            
            if( keepTags && inTag && c == '>' ) {
                inTag = false;
                buf.append( c );
                continue;
            }
            
            if( inTag ) {
                buf.append( c );
                continue;
            }
                
            switch( c ) {
                case '<':  buf.append( "&lt;" ); break;
                case '>':  buf.append( "&gt;" ); break;
                case '&':  buf.append( "&amp;" ); break;
                case '\'': buf.append( "&apos;" ); break;
                case '\"': buf.append( "&quot;" ); break;
                case '\n': buf.append( "<br/>\n" ); break;
                default:   buf.append( c ); break;
            }
        }
         
        return buf.toString();
    } // makeHtmlString()
    
    
    /** 
     * Create a QueryProcessor. Checks the system property
     * "org.cdlib.xtf.QueryProcessorClass" to see if there is a user-
     * supplied implementation. If not, a {@link DefaultQueryProcessor} is
     * created.
     */
    public static QueryProcessor createQueryProcessor()
    {
        // Check the system property.
        final String propName = "org.cdlib.xtf.QueryProcessorClass";
        String className = System.getProperty( propName );
        Class theClass = DefaultQueryProcessor.class;
        try {
            // Try to create an object of the correct class.
            if( className != null )
                theClass = Class.forName(className);
            return (QueryProcessor) theClass.newInstance();
        }
        catch( ClassCastException e ) {
            Trace.error( "Error: Class '" + className + "' specified by " +
                "the '" + propName + "' property is not an instance of " +
                QueryProcessor.class.getName() );
            throw new RuntimeException( e );
        }
        catch( Exception e ) {
            Trace.error( "Error creating instance of class '" + className +
                "' specified by the '" + propName + "' property" );
            throw new RuntimeException( e );
        }
    } // createQueryProcessor()
    

    /**
     * Although not completely standardized yet, most modern browsers
     * encode Unicode characters above U+007F to UTF8 in the URL. This
     * method looks for probably UTF8 encodings and converts them back
     * to normal Unicode characters.
     * 
     * @param value   value to convert
     * @return        equivalent value with UTF8 decoded to Unicode
     */
    public static String convertUTF8inURL( String value )
    {
        // Scan the string, looking for likely UTF8.
        char[] chars = value.toCharArray();
        boolean foundUTF = false;
        for( int i = 0; i < chars.length; i++ ) 
        {
            char c = chars[i];
            
            // If somehow we already have 2-byte chars, this probably isn't
            // a UTF8 string.
            //
            if( (c & 0xFF00) != 0 )
                return value;
            
            // Skip the ASCII chars
            if( c <= 0x7F )
                continue;
            
            // Look for a two-byte sequence
            if( c >= 0xC0 && c <= 0xDF &&
                i+1 < chars.length &&
                chars[i+1] >= 0x80 && chars[i+1] <= 0xBF )
            {
                foundUTF = true;
                i++;
            }
            
            // Look for a three-byte sequence
            else if( c >= 0xE0 && c <= 0xEF &&
                     i+2 < chars.length &&
                     chars[i+1] >= 0x80 && chars[i+1] <= 0xBF &&
                     chars[i+2] >= 0x80 && chars[i+2] <= 0xBF )
            {
                foundUTF = true;
                i += 2;
            }
            
            // Look for a four-byte sequence
            else if( c >= 0xF0 && c <= 0xF7 &&
                     i+3 < chars.length &&
                     chars[i+1] >= 0x80 && chars[i+1] <= 0xBF &&
                     chars[i+2] >= 0x80 && chars[i+2] <= 0xBF &&
                     chars[i+3] >= 0x80 && chars[i+3] <= 0xBF )
            {
                foundUTF = true;
                i += 3;
            }
                     
            // Trailing bytes without leading bytes are illegal, and thus
            // likely this string isn't UTF8 encoded.
            //
            else if( c >= 0x80 && c <= 0xBF )
                return value;
            
            // Certain other bytes are also illegal.
            else if( c >= 0xF8 && c <= 0xFF )
                return value;
        }
        
        // No UTF8 chars found? Nothing to do.
        if( !foundUTF )
            return value;

        // Okay, convert the UTF8 value to Unicode.
        try {
            byte[] bytes = value.getBytes("ISO-8859-1");
            return new String(bytes, "UTF-8");
        }
        catch( UnsupportedEncodingException e ) {
            return value;
        }
    } // convertUTF8inURL()
    
    
    /**
     * Creates a document containing tokenized and untokenized versions of each
     * parameter.
     */
    public void tokenizeParams( AttribList atts, XMLFormatter fmt )
    {
        // The top-level node marks the fact that this is the parameter list.
        fmt.beginTag( "parameters" );
        
        // Add each parameter to the document.
        for( Iterator iter = atts.iterator(); iter.hasNext(); ) {
            Attrib att = (Attrib) iter.next();
            
            // Don't tokenize build-in attributes.
            if( att.key.equals("servlet.path") )
                continue;
            if( att.key.equals("root.path") )
                continue;
            if( att.key.startsWith("http.") )
                continue;
            if( att.key.equals("raw") )
                continue;
            
            // Don't tokenize empty attributes.
            if( att.value == null || att.value.length() == 0 )
                continue;
            
            // Got one. Let's tokenize it.
            addParam( fmt, att.key, att.value );
        }
        
        fmt.endTag();
    } // tokenizeParams()
    
    
    /**
     * Adds the tokenized and un-tokenized version of the attribute to the
     * given formatter.
     * 
     * @param fmt formatter to add to
     * @param name Name of the URL parameter
     * @param val String value of the URL parameter
     */
    protected void addParam( XMLFormatter fmt, String name, String val )
    {
        // Create the parameter node and assign its name and value.
        fmt.beginTag( "param" );
        fmt.attr( "name", name );
        fmt.attr( "value", val );
        
        // Now tokenize it.
        tokenize( fmt, name, val );
        
        // All done.
        fmt.endTag();
    } // addParam()
    
    
    /**
     * Break 'val' up into its component tokens and add elements for them.
     * 
     * @param fmt formatter to add to
     * @param name Name of the URL parameter
     * @param val value to tokenize
     */
    protected void tokenize( XMLFormatter fmt, String name, String val )
    {
        char[] chars   = val.toCharArray();
        char   inQuote = 0;
        String tmpStr;
        
        int i;
        int start = 0;
        for( i = 0; i < chars.length; i++ ) {
            char c = chars[i];
            
            if( c == inQuote ) {
                if( i > start ) {
                    tmpStr = new String( chars, start, i-start );
                    addTokens( inQuote, fmt, tmpStr );
                }
                inQuote = 0;
                start = i+1;
            }
            else if( inQuote == 0 && c == '\"' ) {
                if( i > start ) {
                    tmpStr = new String( chars, start, i-start );
                    addTokens( inQuote, fmt, tmpStr );
                }
                inQuote = c;
                start = i+1;
            }
            else
                ; // Don't change start... has result of building up a token.
        } // for i
        
        // Process the last tokens
        if( i > start ) {
            tmpStr = new String( chars, start, i-start );
            addTokens( inQuote, fmt, tmpStr ); 
        }
    } // tokenize()
    
    
    /**
     * Adds one or more token elements to a parameter node. Also handles
     * phrase nodes.
     * 
     * @param inQuote Non-zero means this is a quoted phrase, in which case the
     *                element will be 'phrase' instead of 'token', and it will
     *                be given sub-token elements.
     * @param fmt formatter to add to
     * @param str The token value
     */
    protected void addTokens( char inQuote, XMLFormatter fmt, String str )
    {
        // If this is a quoted phrase, tokenize the words within it.
        if( inQuote != 0 ) {
            fmt.beginTag( "phrase" );
            fmt.attr( "value", str );
            tokenize( fmt, "phrase", str );
            fmt.endTag();
            return;
        }
        
        // We want to retain wildcard characters, but the tokenizer won't see
        // them as part of a word. So substitute, temporarily.
        //
        str = saveWildcards( str );
        
        // Otherwise, use a tokenizer to break up the string.
        try {
            XTFTextAnalyzer analyzer = 
                new XTFTextAnalyzer( null, null, null );
            TokenStream toks = analyzer.tokenStream( "text", new StringReader(str) );
            int prevEnd = 0;
            while( true ) {
                Token tok = toks.next();
                if( tok == null )
                    break;
                if( tok.startOffset() > prevEnd )
                    addToken( fmt, 
                              str.substring(prevEnd, tok.startOffset()),
                              false );
                prevEnd = tok.endOffset();
                addToken( fmt, tok.termText(), true );
            }
            if( str.length() > prevEnd )
                addToken( fmt, str.substring(prevEnd, str.length()),
                          false );
        }
        catch( IOException e ) {
            assert false : "How can analyzer throw IO error on string buffer?";
        }
    } // addToken()
    
    
    /**
     * Adds a token element to a parameter node.
     * 
     * @param fmt formatter to add to
     * @param str The token value
     * @param isWord true if  token is a real word, false if only punctuation
     */
    protected void addToken( XMLFormatter fmt, String str, boolean isWord )
    {
        // Remove spaces. If nothing is left, don't bother making a token.
        str = str.trim();
        if( str.length() == 0 )
            return;
        
        // Recover wildcards that were saved.
        str = restoreWildcards( str );
        
        // And create the node
        fmt.beginTag( "token" );
        fmt.attr( "value", str );
        fmt.attr( "isWord", isWord ? "yes" : "no" );
        fmt.endTag();
    } // addToken()
    
    
    /**
     * Converts wildcard characters into word-looking bits that would never
     * occur in real text, so the standard tokenizer will keep them part of
     * words. Resurrect using {@link #restoreWildcards(String)}.
     */
    protected static String saveWildcards( String s )
    {
        // Early out if no wildcards found.
        if( s.indexOf('*') < 0 && s.indexOf('?') < 0 )
            return s;
        
        // Convert to wordish stuff.
        s = s.replaceAll( "\\*", SAVE_WILD_STAR );
        s = s.replaceAll( "\\?", SAVE_WILD_QMARK );
        return s;
    } // saveWildcards()
    
    
    /**
     * Restores wildcards saved by {@link #saveWildcards(String)}.
     */
    protected static String restoreWildcards( String s )
    {
        // Early out if no wildcards found.
        if( s.indexOf(SAVE_WILD_STAR) < 0 && s.indexOf(SAVE_WILD_QMARK) < 0 )
            return s;

        // Convert back from wordish stuff to real wildcards.
        s = s.replaceAll( SAVE_WILD_STAR, "*" );
        s = s.replaceAll( SAVE_WILD_QMARK,   "?" );
        return s;
    } // restoreWildcards()
    
    
    /**
    * Generate an error page based on the given exception. Utilizes the system
    * error stylesheet to produce a nicely formatted HTML page.
    *
    * @param req  The HTTP request we're responding to
    * @param res  The HTTP result to write to
    * @param exc  The exception producing the error. If it's a
    *             DynaXMLException, the attributes will be passed to
    *             the error stylesheet.
    */
    protected void genErrorPage( HttpServletRequest  req, 
                                 HttpServletResponse res, 
                                 Exception           exc )
    {
        // Contort to obtain a string version of the stack trace.
        ByteArrayOutputStream traceStream = new ByteArrayOutputStream();
        exc.printStackTrace( new PrintStream(traceStream) );
        String strStackTrace = traceStream.toString();

        String htmlStackTrace = makeHtmlString( strStackTrace );

        try {

            ServletOutputStream out = res.getOutputStream();

            // First, load the error generating stylesheet.
            TextConfig config = getConfig();
            Templates pss = stylesheetCache.find( config.errorGenSheet ); 

            // Make a trans and put attributes from the HTTP request
            // and from the global config file into it as attributes that
            // the stylesheet can use.
            //
            Transformer trans = pss.newTransformer();
            stuffAttribs( trans, req );
            stuffAttribs( trans, config.attribs );

            // Figure out just the last part of the exception class name.
            String className = exc.getClass().getName().
                replaceAll( ".*\\.", "" ).
                replaceAll( ".*\\$", "" ).
                replaceAll( "Exception", "" ).
                replaceAll( "Error", "" );

            // Now make a document that the stylesheet can parse. Inside it
            // we'll put the exception info.
            //
            StringBuffer doc = new StringBuffer( 2048 );
            doc.append( "<" + className + ">\n" );
            trans.setParameter( "exception", new StringValue(className) );

            // Give the message (if any)
            String msg = makeHtmlString( exc.getMessage() );
            if( !isEmpty(msg) ) {
                doc.append( "<message>" + msg + "</message>\n" );
                trans.setParameter( "message", new StringValue(msg) );
            }

            // Add any attributes if this is a GeneralException
            if( exc instanceof GeneralException ) {
                GeneralException bve = (GeneralException) exc;
                for( Iterator i = bve.attribs.iterator(); i.hasNext(); ) {
                    Attrib a = (Attrib) i.next();

                    doc.append( "<" + a.key + ">" );
                        doc.append( a.value );
                    doc.append( "</" + a.key + ">\n" );
                    trans.setParameter( a.key, new StringValue(a.value) );
                }
            }

            // Give the stack trace, but only if this is *not* a normal
            // servlet exception. (The normal ones happen normally, and
            // thus don't need stack traces for debugging.)
            //
            if( !(exc instanceof ExcessiveWorkException) &&
                (!(exc instanceof GeneralException) || 
                 ((GeneralException)exc).isSevere()) ) 
            {
                doc.append( "<stackTrace>\n" + 
                            htmlStackTrace + 
                            "</stackTrace>\n" );
                trans.setParameter( "stackTrace", 
                                    new StringValue(htmlStackTrace) );
            }

            doc.append( "</" + className + ">\n" );

            // If this is a severe problem, log it.
            if( exc instanceof GeneralException && 
                ((GeneralException)exc).isSevere() )
            {          
                Trace.error( "Error: " + doc.toString() );
            }

            // Now produce the error page.
            StreamSource src = new StreamSource( 
                new StringReader(doc.toString()) );
            StreamResult dst = new StreamResult( out );
            trans.transform( src, dst );
        }
        catch( Exception e ) {

            // For some reason, we couldn't load or run the error generator.
            // Try to output a reasonable default.
            //
            try {
                Trace.error( "Unable to generate error page because: " + 
                             e.getMessage() + "\n" +
                             "Original problem: " +
                             exc.getMessage() + "\n" + strStackTrace );

                ServletOutputStream out = res.getOutputStream();

                out.println( "<HTML><BODY>" );
                out.println( "<B>Servlet configuration error:</B><br/>" );
                out.println( "Unable to generate error page: " + 
                             e.getMessage() + "<br>" );
                out.println( "Caused by: " + exc.getMessage() + "<br/>" +
                             htmlStackTrace );
                out.println( "</BODY></HTML>" );

            }
            catch( IOException e2 ) {
                // We couldn't even write to the output stream. Give up.
            }
        }

    } // genErrorPage()


} // class TextServlet
