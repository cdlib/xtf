package org.cdlib.xtf.textEngine;

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
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import org.cdlib.xtf.textEngine.dedupeSpans.SpanNearQuery;
import org.cdlib.xtf.textEngine.dedupeSpans.SpanNotQuery;
import org.cdlib.xtf.textEngine.dedupeSpans.SpanOrQuery;
import org.cdlib.xtf.textEngine.dedupeSpans.SpanQuery;
import org.cdlib.xtf.textEngine.dedupeSpans.SpanTermQuery;
import org.cdlib.xtf.textEngine.dedupeSpans.SpanWildcardTermQuery;
import org.cdlib.xtf.textIndexer.XTFTextAnalyzer;
import org.cdlib.xtf.util.Attrib;
import org.cdlib.xtf.util.AttribList;
import org.cdlib.xtf.util.GeneralException;
import org.cdlib.xtf.util.Path;
import org.cdlib.xtf.util.Trace;
import org.cdlib.xtf.util.XMLWriter;
import org.cdlib.xtf.util.XTFSaxonErrorListener;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Processes URL parameters into a Lucene query, using a stylesheet to perform
 * the heavy lifting.
 * 
 * @author Martin Haye
 */
public class QueryRequest
{
    /** 
     * One or more combined meta/text queries. This is an array because it's
     * possible to concatenate results from multiple indexes.
     */
    public ComboQuery[] comboQueries;
    
    /** Path (base dir relative) for the resultFormatter stylesheet */
    public String       displayStyle;
    
    /** Document rank to start with (0-based) */
    public int          startDoc;
    
    /** Max # documents to return from this query */
    public int          maxDocs;

    /** Mode: processing meta-data query */ 
    private final static int MODE_META = 1;
    
    /** Mode: processing text query */
    private final static int MODE_TEXT = 2;
    
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

    /** ComboQuery currently being worked on */
    private ComboQuery curCombo;
    
    /** 
     * Used to enforce index path being the same on combo query as it is
     * on its embedded meta- or text-query.
     */
    private String origIndexPath;
    
    /** 
     * Keeps track of the servlet base directory, used to map relative
     * file paths.
     */
    private File        baseDir;
    
    /**
     * Produce a Lucene query using a list of attributes (which are typically
     * gathered from the URL parameters to a servlet.) They are processed
     * through the specified stylesheet.
     * 
     * @param stylesheet Transformer that will be used. Should
     *                   already be stuffed with any global user-specified
     *                   parameters.
     * @param atts       Attributes to pass to the stylesheet, in plain and tokenized
     *                   form.
     * @param baseDir    Directory that paths are interpreted relative to.   
     */
    public QueryRequest( Transformer stylesheet, 
                         AttribList  atts,
                         File        baseDir )
        throws QueryGenException, TransformerException, QueryFormatError
    {
        this.baseDir = baseDir;
        
        Document  input  = tokenizeParams( atts );
        DOMResult output = new DOMResult();
        
        if( Trace.getOutputLevel() >= Trace.debug ) {
            Trace.debug( "*** queryParser input ***" );
            Trace.debug( XMLWriter.toString(input) );
        }
        
        // Make sure errors get directed to the right place.
        if( !(stylesheet.getErrorListener() instanceof XTFSaxonErrorListener) )
            stylesheet.setErrorListener( new XTFSaxonErrorListener() );

        DOMSource src = new DOMSource( input );
        stylesheet.transform( src, output );
        
        if( Trace.getOutputLevel() >= Trace.debug ) {
            Trace.debug( "*** queryParser output ***" );
            Trace.debug( XMLWriter.toString(output.getNode()) );
        }
        
        parseOutput( output.getNode() );
    } // constructor
    
    
    /**
     * Produce a Lucene query from the intermediate format that is normally
     * produced by the formatting stylesheet.
     * 
     * @param queryDoc A DOM document containing the query.
     */
    public QueryRequest( Node queryDoc,
                         File baseDir )
        throws QueryGenException, QueryFormatError
    {
        this.baseDir = baseDir;
        
        if( Trace.getOutputLevel() >= Trace.debug ) {
            Trace.debug( "*** queryParser output ***" );
            Trace.debug( XMLWriter.toString(queryDoc) );
        }

        parseOutput( (Element) queryDoc );
    } // constructor
    
    
    /**
     * Creates a document containing tokenized and untokenized versions of each
     * parameter.
     */
    public static Document tokenizeParams( AttribList atts )
        throws QueryGenException
    {
        Document doc = null;
        
        // Create an empty document. This really should be easier.
        try {
            doc = DocumentBuilderFactory.newInstance().
                                newDocumentBuilder().newDocument();
        } catch( ParserConfigurationException e ) {
            throw new QueryGenException( "Unable to create empty document", e );
        }
        
        // The top-level node marks the fact that this is the parameter list.
        Node topNode = doc.createElement( "parameters" );
        doc.appendChild( topNode );
        
        // Add each parameter to the document.
        for( Iterator iter = atts.iterator(); iter.hasNext(); ) {
            Attrib att = (Attrib) iter.next();
            
            // Don't tokenize the servlet path! It's built-in, not part
            // of the URL.
            //
            if( att.key.equals("servlet.path") )
                continue;
            addParam( doc, topNode, att.key, att.value );
        }
        
        // And we're done.
        return doc;
    } // buildInput()
    
    
    /**
     * Adds the tokenized and un-tokenized version of the attribute as a child
     * element of the given node.
     * 
     * @param topNode Node to add the attribute to
     * @param name Name of the URL parameter
     * @param val String value of the URL parameter
     */
    private static void addParam( Document doc, Node topNode, 
                                  String name, String val )
    {
        // Create the parameter node and assign its name and value.
        Element parmNode = doc.createElement( "param" );
        parmNode.setAttribute( "name", name );
        parmNode.setAttribute( "value", val );
        topNode.appendChild( parmNode );
        
        // Now tokenize it.
        tokenize( doc, parmNode, val );
    } // addParam()
    
    
    /**
     * Break 'val' up into its component tokens and add them to parmNode.
     * 
     * @param doc Used for creating nodes
     * @param parmNode Node to add to
     * @param val The value to tokenize
     */
    private static void tokenize( Document doc, Node parmNode, String val )
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
                    addTokens( inQuote, doc, parmNode, tmpStr );
                }
                inQuote = 0;
                start = i+1;
            }
            else if( inQuote == 0 && c == '\"' ) {
                if( i > start ) {
                    tmpStr = new String( chars, start, i-start );
                    addTokens( inQuote, doc, parmNode, tmpStr );
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
            addTokens( inQuote, doc, parmNode, tmpStr ); 
        }
    } // tokenize()
    
    
    /**
     * Adds one or more token elements to a parameter node. Also handles
     * phrase nodes.
     * 
     * @param inQuote Non-zero means this is a quoted phrase, in which case the
     *                element will be 'phrase' instead of 'token', and it will
     *                be given sub-token elements.
     * @param doc Document used to create nodes
     * @param parmNode The element to add to
     * @param str The token value
     */
    private static void addTokens( char inQuote,  Document doc, 
                                   Node parmNode, String   str )
    {
        // If this is a quoted phrase, tokenize the words within it.
        if( inQuote != 0 ) {
            Element phraseNode = doc.createElement( "phrase" );
            phraseNode.setAttribute( "value", str );
            parmNode.appendChild( phraseNode );
            tokenize( doc, phraseNode, str );
            return;
        }
        
        // We want to retain wildcard characters, but the tokenizer won't see
        // them as part of a word. So substitute, temporarily.
        //
        str = saveWildcards( str );
        
        // Otherwise, use a tokenizer to break up the string.
        try {
            StringBuffer buf = new StringBuffer( str );
            XTFTextAnalyzer analyzer = new XTFTextAnalyzer( null, -1, buf );
            TokenStream toks = analyzer.tokenStream( "text", new StringReader(str) );
            int prevEnd = 0;
            while( true ) {
                Token tok = toks.next();
                if( tok == null )
                    break;
                if( tok.startOffset() > prevEnd )
                    addToken( doc, parmNode, 
                              str.substring(prevEnd, tok.startOffset()),
                              false );
                prevEnd = tok.endOffset();
                addToken( doc, parmNode, tok.termText(), true );
            }
            if( str.length() > prevEnd )
                addToken( doc, parmNode, str.substring(prevEnd, str.length()),
                          false );
        }
        catch( IOException e ) {
            assert false : "How can analyzer throw IO error on string buffer?";
        }
    } // addToken()
    
    
    /**
     * Adds a token element to a parameter node.
     * 
     * @param doc The XML document being added to
     * @param parmNode The element to add to
     * @param str The token value
     * @param isWord true if  token is a real word, false if only punctuation
     */
    private static void addToken( Document doc, 
                                  Node parmNode, 
                                  String str,
                                  boolean isWord )
    {
        // Remove spaces. If nothing is left, don't bother making a token.
        str = str.trim();
        if( str.length() == 0 )
            return;
        
        // Recover wildcards that were saved.
        str = restoreWildcards( str );
        
        // And create the node
        Element tokenNode = doc.createElement( "token" );
        tokenNode.setAttribute( "value", str );
        tokenNode.setAttribute( "isWord", isWord ? "yes" : "no" );
        parmNode.appendChild( tokenNode );
    } // addToken()
    
    
    /**
     * Converts wildcard characters into word-looking bits that would never
     * occur in real text, so the standard tokenizer will keep them part of
     * words. Resurrect using {@link #restoreWildcards(String)}.
     */
    private static String saveWildcards( String s )
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
    private static String restoreWildcards( String s )
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
     * Convenience function to throw a {@link QueryGenException} with the 
     * given message.
     */
    private void error( String message )
        throws QueryGenException
    {
        throw new QueryGenException( message );
    } // error()
    
    
    /**
     * Processes the output of the generator stylesheet, turning it into a
     * Lucene query.
     * 
     * @param output The stylesheet output, whose first (and only) child
     *               should be a 'query' element.
     * @return The resulting Lucene query
     */
    private void parseOutput( Node output )
        throws QueryGenException, QueryFormatError
    {
        for( ElementIter mainIter = new ElementIter(output); 
             mainIter.hasNext(); ) 
        {
            Element main = mainIter.next();
            String  name = main.getLocalName();
            
            if( !name.equals("query") && !name.equals("error") )
                error( "Expected 'query' or 'error' element at " +
                       "top level; found '" + name + "'" );
            
            parseOutput( main );
        }
    } // parseOutput()
    
    /**
     * Processes the main query node, turning it into a Lucene query.
     * 
     * @param main The 'query' element
     * @return The resulting Lucene query
     */
    private void parseOutput( Element main )
    {
        Vector queryVec = new Vector();
        
        String name = main.getLocalName();

        if( name.equals("error") )
            throw new QueryFormatError( main.getAttribute("message") );
        
        // Check for required parameters
        displayStyle = parseStringAttrib( main, "style", "NullStyle.xsl" );
        startDoc     = parseIntAttrib   ( main, "startDoc",  1 );
        maxDocs      = parseIntAttrib   ( main, "maxDocs",  -1 );
        
        // Adjust for 1-based start doc.
        startDoc = Math.max( 0, startDoc-1 );
        
        // Make sure the stylesheet to format the results is present.
        displayStyle = Path.resolveRelOrAbs(baseDir, displayStyle);

        if( !(new File(displayStyle).exists()) && 
            displayStyle.indexOf("NullStyle.xsl") < 0 )
        {
            error( "Style \"" + displayStyle + 
                   "\" specified in '" + name + "' element " +
                   "does not exist" );
        }
        
        // Iterate through the query elements
        for( ElementIter iter = new ElementIter(main); iter.hasNext(); ) {
            Element el   = iter.next();
            name = el.getLocalName();

            if( !name.matches("^combine$|^meta$|^text$") )
                error( "Expected: 'combine', 'meta', or 'text' but found '" +
                       name + "'" );
            
            // Get the path to the index
            ComboQuery cq = new ComboQuery();
            curCombo = cq;
            cq.indexPath = parseStringAttrib( el, "indexPath" );
            origIndexPath = cq.indexPath;
            cq.indexPath = Path.resolveRelOrAbs(baseDir, cq.indexPath);
            
            // Make sure it exists.
            if( !(new File(cq.indexPath).exists()) )
                error( "Index path \"" + cq.indexPath + 
                       "\" specified in '" + name + "' element " +
                       "does not exist" );
            
            // Optionally, the query might specify a term limit and/or a
            // work limit.
            //
            cq.termLimit = parseIntAttrib( el, "termLimit", 50 );
            cq.workLimit = parseIntAttrib( el, "workLimit", -1 );
            
            // Optionally, a meta-query might contain a list of sort fields.
            cq.sortMetaFields = parseStringAttrib( el, "sortMetaFields", "" );

            // Create the map that will be used to store all the terms (and
            // to enforce the term limit.)
            //
            cq.terms = new TermMap( cq.termLimit );
            
            // Now parse a meta-query, text query, or combination.
            if( name.equals("combine") )
                parseCombine(el, cq);
            else if( name.equals("meta") )
                cq.metaQuery = parseQuery(el, null, MODE_META, true);
            else if( name.equals("text") ) {
                cq.maxSnippets  = parseIntAttrib( el, "maxSnippets",   3 );
                cq.maxContext = parseIntAttrib( el, "maxContext", 80 );
                cq.textQuery = (SpanQuery) parseQuery(el, "text", MODE_TEXT, true);
                cq.sectionTypeQuery = parseSectionType( el );
            }
            else
                assert false : "forgot to handle case";
                
            queryVec.add( cq );
        } // for iter
        
        // Make sure at least one query was specified, and that the display
        // sheet was defined.
        //
        if( queryVec.size() == 0 )
            error( "At least one query must be specified" );
        if( displayStyle == null || displayStyle.length() == 0 )
            error( "queryGen stylesheet failed to specify 'style'" );
        
        // Transform the vector to an easy-to-use array.
        comboQueries = (ComboQuery[]) queryVec.toArray( new ComboQuery[0] );

    } // parseOutput()
    
    
    /**
     * Locate the named attribute and retrieve its value as an integer.
     * If not found, an error exception is thrown.
     * 
     * @param el Element to search
     * @param attribName Attribute to find
     */
    private int parseIntAttrib( Element el, String attribName )
        throws QueryGenException
    {
        return parseIntAttrib( el, attribName, false, 0 );
    }
    
    /**
     * Locate the named attribute and retrieve its value as an integer.
     * If not found, return a default value.
     * 
     * @param el Element to search
     * @param attribName Attribute to find
     * @param defaultVal If not found and useDefault is true, return this 
     *                   value.
     */
    private int parseIntAttrib( Element el, 
                                String attribName, 
                                int defaultVal  )
        throws QueryGenException
    {
        return parseIntAttrib( el, attribName, true, defaultVal );
    }
    
    /**
     * Locate the named attribute and retrieve its value as an integer.
     * Handles default processing if requested.
     * 
     * @param el Element to search
     * @param attribName Attribute to find
     * @param useDefault true to supply a default value if none found,
     *                   false to throw an exception if not found.
     * @param defaultVal If not found and useDefault is true, return this 
     *                   value.
     */
    private int parseIntAttrib( Element el, String attribName, 
                                boolean useDefault, int defaultVal )
        throws QueryGenException
    {
        String elName = el.getLocalName();
        String str = parseStringAttrib( el, 
                                        attribName,
                                        useDefault,
                                        null );
        if( str == null && useDefault )
            return defaultVal;
        
        try {
            return Integer.parseInt( str );
        } catch( Exception e ) {
            error( "'" + attribName + "' attribute of '" + elName + 
                   "' element is not a valid integer" );
            return 0;
        }
    } // parseIntAttrib()
    
    
    /**
     * Locate the named attribute and retrieve its value as a string. If
     * not found, an error exception is thrown.
     * 
     * @param el Element to search
     * @param attribName Attribute to find
     */
    private String parseStringAttrib( Element el, 
                                      String  attribName ) 
        throws QueryGenException
    {
        return parseStringAttrib( el, attribName, false, null );
    }
    
    /**
     * Locate the named attribute and retrieve its value as a string. If
     * not found, return a default value.
     * 
     * @param el Element to search
     * @param attribName Attribute to find
     * @param defaultVal If not found, return this value.
     */
    private String parseStringAttrib( Element el, 
                                      String  attribName,
                                      String  defaultVal ) 
        throws QueryGenException
    {
        return parseStringAttrib( el, attribName, true, defaultVal );
    }
    
    /**
     * Locate the named attribute and retrieve its value as a string.
     * Handles default processing if requested.
     * 
     * @param el Element to search
     * @param attribName Attribute to find
     * @param useDefault true to supply a default value if none found,
     *                   false to throw an exception if not found.
     * @param defaultVal If not found and useDefault is true, return this 
     *                   value.
     */
    private String parseStringAttrib( Element el, 
                                      String  attribName, 
                                      boolean useDefault,
                                      String  defaultVal )
        throws QueryGenException
    {
        String elName = el.getLocalName();
        String str = el.getAttribute( attribName );

        if( str == null || str.length() == 0 ) {
            if( !useDefault )
                error( "'" + elName + "' element must specify '" + 
                       attribName + "' attribute" );
            return defaultVal;
        }
        
        return str;
        
    } // parseStringAttrib()
    
    
    /**
     * Parses a 'combine' element, which is used to fuse the results of a meta-
     * query with those of a text query.
     * 
     * @param parent The 'combine' element
     * @param iq The ComboQuery to add to.
     * @return A combined query
     */
    private void parseCombine( Element parent, ComboQuery iq )
        throws QueryGenException
    {
        for( ElementIter iter = new ElementIter(parent); iter.hasNext(); ) {
            Element el   = iter.next();
            String  name = el.getLocalName();
            
            if( name.equals("meta") ) {
                if( iq.metaQuery != null )
                    error( "Only one 'meta' allowed per 'combine' element" );
                iq.metaQuery = parseQuery( el, null, MODE_META, true );
            }
            else if( name.equals("text") ) {
                if( iq.textQuery != null )
                    error( "Only one 'text' allowed per 'combine' element" );
                iq.maxSnippets  = parseIntAttrib( el, "maxSnippets",   3 );
                iq.maxContext = parseIntAttrib( el, "maxContext", 80 );
                iq.textQuery = (SpanQuery) parseQuery( el, "text", 
                                                       MODE_TEXT, true );
                iq.sectionTypeQuery = parseSectionType( el );
            }
            else
                error( "Expected 'meta' or 'text'; found '" + name );
        } // for iter
        
        // Make sure at least one was specified.
        if( iq.metaQuery == null && iq.textQuery == null )
            error( "Must have 'meta', 'text', or both in 'combine' element" );

    } // parseCombine()
    

    /**
     * If the given element has a 'field' attribute, return its value;
     * otherwise return 'parentField'. Also checks that field cannot be
     * specified if parentField has already been.
     */
    private String parseField( Element el, String parentField )
        throws QueryGenException
    {
        if( !el.hasAttribute("metaField") && !el.hasAttribute("field") )
            return parentField;
        String attVal = el.getAttribute("metaField");
        if( attVal == null || attVal.length() == 0 )
            attVal = el.getAttribute( "field" );
        
        if( attVal.length() == 0 )
            error( "'metaField' attribute cannot be empty" );
        if( "text".equals(parentField) && !attVal.equals("text") )
            error( "Within a text query, the 'metaField' attribute " +
                   "must be absent or have the value 'text'" );
        if( attVal.equals("text") )
            error( "Meta queries cannot access the 'text' field" );
        if( parentField != null && !parentField.equals(attVal) )
            error( "Cannot override ancestor 'metaField' attribute" );
        
        return attVal;
    }

    
    /**
     * Parse a 'meta' or 'text' query element.
     */
    private Query parseQuery( Element parent, String field, int mode,
                              boolean addTerms )
        throws QueryGenException
    {
        // Record a field name if specified
        field = parseField( parent, field );
        
        // If indexPath, termLimit, or workLimit are specified, they
        // had better match the top-level combo query.
        //
        if( parent.hasAttribute("indexPath") &&
            !origIndexPath.equals(parent.getAttribute("indexPath")) )
        {
            error( "'" + parent.getLocalName() + 
                    "' element indexPath attribute must match its parent" );
        }
        
        String curTermLimit = Integer.toString( curCombo.termLimit );
        if( parent.hasAttribute("termLimit") &&
            !curTermLimit.equals(parent.getAttribute("termLimit")) )
        {
            error( "'" + parent.getLocalName() + 
                    "' element termLimit attribute must match its parent" );
        }
        
        String curWorkLimit = Integer.toString( curCombo.workLimit );
        if( parent.hasAttribute("workLimit") &&
            !curWorkLimit.equals(parent.getAttribute("workLimit")) )
        {
            error( "'" + parent.getLocalName() + 
                    "' element workLimit attribute must match its parent" );
        }
        
        // The 'sortMetaFields' attribute can be specified either here or in 
        // the parent.
        //
        if( parent.hasAttribute("sortMetaFields") ) 
        {
            String newVal = parent.getAttribute( "sortMetaFields" );
            if( curCombo.sortMetaFields.length() > 0 &&
               !curCombo.sortMetaFields.equals(newVal) )
            {
                error( "'" + parent.getLocalName() + 
                        "' element sortMetaFields attribute must match its parent" );
            }
            curCombo.sortMetaFields = newVal;
        }

        // We require exactly one child element.
        int count = 0;
        Element child = null;
        Element sectionType = null;
        for( ElementIter iter = new ElementIter(parent); iter.hasNext(); ) 
        {
            Element el = iter.next();
            if( el.getNodeName().equals("sectionType") && mode == MODE_TEXT )
                sectionType = el;
            else {
                child = el;
                count++;
            }
        }
        if( count != 1 ) {
            if( mode == MODE_TEXT )
                error( "'" + parent.getLocalName() + "' element requires exactly " +
                       "one child element (other than optional sectionType)" );
            else
                error( "'" + parent.getLocalName() + "' element requires exactly " +
                       "one child element" );
        }
        
        // Here comes the main work.
        return parseBoolean( child, field, mode, addTerms );
    } // parseQuery()
    
    
    /**
     * Parse a 'sectionType' query element. Note that this query is parsed
     * as a span query, because the query processing logic requires it to
     * return documents in ascending ID order (something which the normal
     * BooleanQuery does not do.)
     */
    private SpanQuery parseSectionType( Element parent )
        throws QueryGenException
    {
        // Find the sectionType element (if any)
        Element sectionType = null;
        for( ElementIter iter = new ElementIter(parent); iter.hasNext(); ) 
        {
            Element el = iter.next();
            if( el.getNodeName().equals("sectionType") ) {
                if( sectionType != null )
                    error( "Cannot specify more than one sectionType element" );
                sectionType = el;
            }
        }
        
        if( sectionType == null )
            return null;
        
        // Make sure it only has one child.
        int count = 0;
        Element child = null;
        for( ElementIter iter = new ElementIter(sectionType); iter.hasNext(); ) 
        {
            child = iter.next();
            count++;
        }
        if( count != 1 )
            error( "'sectionType' element requires exactly " +
                   "one child element" );
        
        return (SpanQuery) 
            parseBoolean( child, "sectionType", MODE_TEXT, false );
    } // parseSectionType()
    
    
    /**
     * Parse a regular boolean element.
     */
    private Query parseBoolean( Element parent, String field, int mode,
                                boolean addTerms )
        throws QueryGenException
    {
        assert (mode == MODE_META || mode == MODE_TEXT) : "Must set mode first";

        String name = parent.getLocalName();
        if( !name.matches("^term$|^all$|^range$|^phrase$|^near$|^and$|^or$|^not$") )
            error( "Expected: 'term', 'all', 'range', 'phrase', 'near', " +
                   "'and', 'or', or 'not'; found '" + name + "'" );

        // 'not' queries are handled at the level above.
        assert( !name.equals("not") );

        // We have to make different sorts of queries depending on whether
        // we're looking at meta-data or full text.
        //
        if( mode == MODE_META )
            return parseMetaBoolean( parent, name, field, addTerms );
        else
            return parseTextBoolean( parent, name, field, addTerms );

    } // parseBoolean()
    
    
    /** Determines if the term contains a wildcard character ('*' or '?') */
    private boolean isWildcardTerm( Term term )
    {
        if( term.text().indexOf('*') >= 0 )
            return true;
        if( term.text().indexOf('?') >= 0 )
            return true;
        return false;
    } // isWildcardTerm()
    
    /**
     * Parse a boolean query on a meta-data field. No span logic is required
     * for these, since no snippets are generated.
     */
    private Query parseMetaBoolean( Element parent, String name, String field,
                                    boolean addTerms )
        throws QueryGenException
    {
        // Term query is the simplest kind.
        if( name.equals("term") ) {
            Term term = parseTerm( parent, field, "term", addTerms );
            if( isWildcardTerm(term) )
                return new LimitedWildcardQuery( term, curCombo.termLimit, 
                                                 curCombo.terms );
            else {
                if( addTerms )
                    curCombo.terms.put( term );
                return new TermQuery( term );
            }
        }
    
        // Get field name if specified.
        field = parseField( parent, field );
        
        // 'all', 'phrase', and 'near' can be viewed as phrase queries with
        // different slop values.
        //
        // 'all' means essentially infinite slop (limited to the actual
        //          chunk overlap at runtime.
        // 'phrase' means zero slop
        // 'near' allows specifying the slop (again limited to the actual
        //          chunk overlap at runtime.
        //
        if( name.equals("all") || 
            name.equals("phrase") || 
            name.equals("near"))
        {   
            int slop = name.equals("all") ? Integer.MAX_VALUE :
                       name.equals("phrase") ? 0 :
                       parseIntAttrib( parent, "slop" );
                       
            // Since Lucene's PhraseQuery doesn't allow any flexibility in
            // terms of including arbitrary queries below it, just use the
            // span logic.
            //
            return makeAllQuery( parent, slop, field, addTerms );
        }
        
        // Range queries are pretty specialized.
        if( name.equals("range") )
            return parseRange( parent, field );
        
        // All other cases fall through to here: and, or. Handle the 'not'
        // sub-clauses along the way.
        //
        BooleanQuery bq = new BooleanQuery();
        boolean require = name.equals("and");
        for( ElementIter iter = new ElementIter(parent); iter.hasNext(); ) {
            Element el = iter.next();
            if( el.getLocalName().equals("not") ) 
            {
                // Make sure terms used in the 'not' query aren't added to
                // the term map (since it would be silly to hilight them.)
                //
                bq.add( parseQuery(el, field, MODE_META, false), 
                        false, true );
            }
            else
                bq.add( parseBoolean(el, field, MODE_META, addTerms), 
                        require, false );
        }
        
        return bq;
    } // parseMetaBoolean()


    /**
     * Parse a boolean query on the full text. Since snippets will be
     * generated, this requires use of the fancy span de-duplication logic.
     */
    private Query parseRange( Element parent, String field )
        throws QueryGenException
    {
        // Inclusive or exclusive?
        boolean inclusive = false;
        String yesno = parseStringAttrib( parent, "inclusive", "yes" );
        if( yesno.equals("yes") )
            inclusive = true;
        else if( !yesno.equals("no") )
            error( "'inclusive' attribute for 'range' query must have value " +
                   "'yes' or 'no'" );
        
        // Check the children for the lower and upper bounds.
        Term lower = null;
        Term upper = null;
        for( ElementIter iter = new ElementIter(parent); iter.hasNext(); ) {
            Element child = iter.next();
            String name = child.getLocalName();
            if( name.equals("lower") ) {
                if( lower != null )
                    error( "'lower' only allowed once as child of 'range' element" );
                lower = parseTerm( child, field, "lower", false );
            }
            else if( name.equals("upper") ) {
                if( upper != null )
                    error( "'upper' only allowed once as child of 'range' element" );
                upper = parseTerm( child, field, "upper", false );
            }
            else
                error( "'range' element may only have 'lower' and/or 'upper' " +
                       "as child elements" );
        } // for iter
        
        // Upper, lower, or both must be specified.
        if( lower == null && upper == null )
            error( "'range' element must have 'lower' and/or 'upper' child element(s)" );
        
        // And we're done.
        return new LimitedRangeQuery( lower, upper, inclusive, 
                                      curCombo.termLimit );
    } // parseRange()

    /**
     * Parse a boolean query on the full text. Since snippets will be
     * generated, this requires use of the fancy span de-duplication logic.
     */
    private Query parseTextBoolean( Element parent, String name, String field,
                                    boolean addTerms )
        throws QueryGenException
    {
        // 'not' queries are handled at the parent's level.
        assert !name.equals("not");
        
        // Range queries aren't allowed for the text.
        if( name.equals("range") )
            error( "Range queries not allowed on text, only on meta-info" );
        
        // Term query is the simplest kind.
        if( name.equals("term") ) {
            Term term = parseTerm( parent, field, "term", addTerms );
            if( isWildcardTerm(term) )
                return new SpanWildcardTermQuery( term, curCombo.termLimit );
            else
                return new SpanTermQuery( term );
        }

        // Get field name if specified.
        field = parseField( parent, field );
        
        // For text queries, 'all', 'phrase', and 'near' can be viewed
        // as phrase queries with different slop values.
        //
        // 'all' means essentially infinite slop (limited to the actual
        //          chunk overlap at runtime.
        // 'phrase' means zero slop
        // 'near' allows specifying the slop (again limited to the actual
        //          chunk overlap at runtime.
        //
        if( name.equals("all") || name.equals("phrase") || name.equals("near"))
        {   
            int slop = name.equals("all") ? Integer.MAX_VALUE :
                       name.equals("phrase") ? 0 :
                       parseIntAttrib( parent, "slop" );
            return makeAllQuery( parent, slop, field, addTerms );
        }
        
        // All other cases fall through to here: and, or. Use our special
        // de-duplicating span logic. First, get all the sub-queries.
        //
        Vector subVec = new Vector();
        Vector notVec = new Vector();
        for( ElementIter iter = new ElementIter(parent); iter.hasNext(); ) {
            Element el = iter.next();
            if( el.getLocalName().equals("not") ) 
            {
                // Make sure that we don't add the terms from the 'not' query
                // to the term map, since it would be silly to hilight them.
                //
                notVec.add( parseQuery(el, field, MODE_TEXT, false) );
            }
            else
                subVec.add( parseBoolean(el, field, MODE_TEXT, addTerms) );
        }
        SpanQuery[] subQueries = 
            (SpanQuery[]) subVec.toArray( new SpanQuery[0] ); 
        
        // Now make the top-level query.
        SpanQuery q;
        if( name.equals("and") ) {
            q = new SpanNearQuery( subQueries, Integer.MAX_VALUE );
            
            // We can't know the actual slop until the query is run against
            // an index (the slop will be equal to max proximity). So add it
            // to a list of queries that need slop fixing.
            //
            curCombo.addSlopFixup( q, Integer.MAX_VALUE ); 
        }
        else
            q = new SpanOrQuery( subQueries );
        
        // Finish up by handling any not clauses found.
        return processTextNots( q, notVec );
    } // parseTextBoolean()
    
    
    /**
     * If any 'not' clauses are present, this builds a query that filters them
     * out of the main query.
     */
    SpanQuery processTextNots( SpanQuery query, Vector notClauses ) 
    {
        // If there aren't any 'not' clauses, we're done.
        if( notClauses.isEmpty() )
            return query;
        
        // If there's only one, the sub-query is simple.
        SpanQuery subQuery;
        if( notClauses.size() == 1 )
            subQuery = (SpanQuery) notClauses.get( 0 );
        else 
        {
            // Otherwise, 'or' all the nots together.
            SpanQuery[] subs = (SpanQuery[]) 
                notClauses.toArray( new SpanQuery[0] );
            subQuery = new SpanOrQuery( subs );
        }
        
        // Now make the final 'not' query. Note that the actual slop will have
        // to be fixed when the query is run.
        //
        SpanQuery nq = new SpanNotQuery( query, subQuery, Integer.MAX_VALUE );
        curCombo.addSlopFixup( nq, Integer.MAX_VALUE );
        return nq;
    } // processTextNots();
    
    
    /**
     * Generate a proximity query on a field. This uses the de-duplicating span
     * system.
     * 
     * @param parent The element containing the field name and terms.
     */
    Query makeAllQuery( Element parent, int slop, String field, 
                        boolean addTerms )
        throws QueryGenException
    {
        Vector terms  = new Vector();
        Vector notVec = new Vector();
        for( ElementIter iter = new ElementIter(parent); iter.hasNext(); ) {
            Element el = iter.next();
            if( el.getLocalName().equals("not") ) {
                if( slop == 0 )
                    error( "'not' clauses aren't supported in phrase queries" );
                
                // Make sure to avoid adding the 'not' terms to the term map,
                // since it would be silly to hilight them.
                //
                notVec.add( parseQuery(el, field, MODE_TEXT, false) );
            }
            else {
                if( slop == 0 ) {
                    Term t = parseTerm( el, field, "term", addTerms );
                    if( isWildcardTerm(t) )
                        terms.add( 
                             new SpanWildcardTermQuery(t, curCombo.termLimit) );
                    else
                        terms.add( new SpanTermQuery(t) );
                }
                else
                    terms.add( parseBoolean(el, field, MODE_TEXT, addTerms) );
            }
        }
        
        if( terms.size() == 0 )
            error( "'" + parent.getLocalName() + "' element requires at " +
                   "least one term" );
        
        // Optimization: treat a single-term 'all' query as just a simple
        // term query.
        //
        if( terms.size() == 1 )
            return (SpanQuery) terms.elementAt(0);
        
        // Make a 'near' query out of it. Zero slop implies in-order.
        boolean inOrder = (slop == 0);
        SpanQuery q = new SpanNearQuery( 
                                  (SpanQuery[]) terms.toArray(new SpanQuery[0]), 
                                  slop );
        
        // Set slop according to requested exactness. If it's not exact,
        // infinite, it will have to be restricted to use the actual chunk
        // overlap when the query is run.
        //
        if( slop != 0 )
            curCombo.addSlopFixup( q, slop );
        
        // And we're done.
        return q;
        
    } // makeTextAllQuery()
    
    
    /**
     * Parses a 'term' element. If not so marked, an exception is thrown.
     * 
     * @param parent The element to parse
     */
    private Term parseTerm( Element parent, String field, String expectedName,
                            boolean recordTerms )
        throws QueryGenException
    {
        // Get field name if specified.
        field = parseField( parent, field );
        if( field == null )
            error( "'term' element requires 'field' attribute on " +
                   "itself or an ancestor" );
        
        if( !parent.getLocalName().equals(expectedName) )
            error( "Expected '" + expectedName + "' as child of '" + 
                   parent.getParentNode().getLocalName() +
                   "' element, but found '" + parent.getLocalName() + "'" );
        
        String termText  = getText( parent );
        
        // For now, convert text to lowercase. In the future, we might allow
        // case-sensitive searching.
        //
        termText = termText.toLowerCase();
        
        // Make a term out of the field and the text.
        Term term = new Term( field, termText );
        
        // Add the term to the map (used in highlighting), except if it's
        // part of a range query.
        //
        if( expectedName.equals("term") && recordTerms )
            curCombo.terms.put( term );
        
        return term;
        
    } // parseTerm()
    
    
    /**
     * Ensures that the element has only a single child node (ignoring
     * attributes), and that it's a text node.
     * 
     * @param el The element to get the text of
     * @return The string value of the text
     */
    private String getText( Element el )
        throws QueryGenException
    {
        // There should be no element children, only text.
        int count = 0;
        String text = null;
        for( Node n = el.getFirstChild(); n != null; n = n.getNextSibling() ) {
            if( n.getNodeType() != Node.ATTRIBUTE_NODE &&
                    n.getNodeType() != Node.TEXT_NODE )
            {
                count = -1;
                break;
            }
            if( n.getNodeType() == Node.TEXT_NODE )
                text = n.getNodeValue();
            count++;
        }
        
        if( count != 1 )
            error( "A single text node is required for the '" +
                   el.getLocalName() + "' element" );
        
        return text;
    } // getText()
    

    /**
     * Prints the node, in XML format, to System.err
     */
    private void debugNode( Source node )
    {
        try {
            StringWriter writer = new StringWriter();
            StreamResult tmp = new StreamResult( writer );
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer trans = factory.newTransformer();
            Properties props = trans.getOutputProperties();
            props.put( "indent", "yes" );
            props.put( "method", "xml" );
            trans.setOutputProperties( props ); 
            trans.transform( node, tmp );
            Trace.error( writer.toString() );
        
        }
        catch( Exception e ) {
            Trace.error( "Error in debugNode(): " + e );
        } 
    } // debugNode()
    
    
    /**
     * Exception class used to report errors from the query generator.
     */
    public class QueryFormatError extends GeneralException
    {
        public QueryFormatError( String message ) {
            super( message );
        }
        
        public boolean isSevere() { return false; }
    } // class QueryFormatError
    
    /**
     * Iterates through the element children of a node.
     * 
     * @author Martin Haye
     */
    private class ElementIter
    {
        private Node next;
        
        public ElementIter( Node parent )
        {
            next = parent.getFirstChild();
            while( next != null && next.getNodeType() != Node.ELEMENT_NODE )
                next = next.getNextSibling();
        } // constructor
        
        public boolean hasNext()
        {
            return next != null;
        }
        
        public Element next()
        {
            if( next == null )
                return null;
            
            Element ret = (Element) next;
            
            do
                next = next.getNextSibling();
            while( next != null && next.getNodeType() != Node.ELEMENT_NODE );

            return ret;
        }
        
    } // class ElementIter
    
} // class QueryRequest
