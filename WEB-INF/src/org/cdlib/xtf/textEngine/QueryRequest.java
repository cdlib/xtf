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
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.chunk.SpanChunkedNotQuery;
import org.apache.lucene.chunk.SpanDechunkingQuery;
import org.apache.lucene.index.Term;
import org.apache.lucene.mark.SpanDocument;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanRangeQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.SpanWildcardQuery;

import org.cdlib.xtf.textIndexer.XTFTextAnalyzer;
import org.cdlib.xtf.util.Attrib;
import org.cdlib.xtf.util.AttribList;
import org.cdlib.xtf.util.GeneralException;
import org.cdlib.xtf.util.Path;
import org.cdlib.xtf.util.Trace;
import org.cdlib.xtf.util.XMLWriter;
import org.cdlib.xtf.util.XTFSaxonErrorListener;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Processes URL parameters into a Lucene query, using a stylesheet to perform
 * the heavy lifting.
 * 
 * @author Martin Haye
 */
public class QueryRequest implements Cloneable
{
    /** Path (base dir relative) for the resultFormatter stylesheet */
    public String     displayStyle;
    
    /** Document rank to start with (0-based) */
    public int        startDoc     = 0;
    
    /** Max # documents to return from this query */
    public int        maxDocs      = 10;
    
    /** Path to the Lucene index we want to search */
    public String     indexPath;

    /** The Lucene query to perform */
    public Query      query;
    
    /** Optional list of fields to sort documents by */
    public String     sortMetaFields;

    /** Target size, in characters, for snippets */
    public int        maxContext   = 80;
    
    /** Limit on the total number of terms allowed */
    public int        termLimit    =  50;
    
    /** Limit on the total amount of "work" */
    public int        workLimit    =   0;
    
    /** Term marking mode */
    public int        termMode     = SpanDocument.MARK_SPAN_TERMS;
    
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
            Trace.debug( "*** query request ***" );
            Trace.debug( XMLWriter.toString(queryDoc) );
        }

        parseOutput( (Element) queryDoc );
    } // constructor
    
    
    // Creates an exact copy of this query request.
    public Object clone() 
    {
        try { return super.clone(); }
        catch( CloneNotSupportedException e ) { throw new RuntimeException(e); }
    } // clone()
    
    
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
            XTFTextAnalyzer analyzer = new XTFTextAnalyzer( null, -1 );
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
     */
    private void parseOutput( Element main )
    {
        if( main.getLocalName().equals("error") )
            throw new QueryFormatError( main.getAttribute("message") );
        
        // Process all the top-level attributes.
        NamedNodeMap attrs = main.getAttributes();
        for( int i = 0; i < attrs.getLength(); i++ ) {
            Attr   attr = (Attr) attrs.item( i );
            String name = attr.getName();
            String val  = attr.getValue();
            parseMainAttrib( main, name, val );
        }

        // Process the children. If we find an old <combine> element,
        // traverse it just like a top-level query.
        //
        int childCount = 0;
        for( ElementIter iter = new ElementIter(main); iter.hasNext(); ) 
        {
            Element el = iter.next();
            
            childCount++;
            if( childCount > 1 ) {
                error( "<" + main.getNodeName() + "> element must have " +
                       " exactly one child element" );
            }
            
            query = deChunk( parseQuery(el, null, Integer.MAX_VALUE) );
        }

        if( childCount != 1 ) {
            error( "<" + main.getNodeName() + "> element must have " +
                   " exactly one child element" );
        }
        
        if( main.getLocalName().equals("query") &&
            Trace.getOutputLevel() >= Trace.debug )
        {
            Trace.debug( "Lucene query as parsed: " + query.toString() );
        }
        
        // Check that we got the required parameters.
        if( main.getLocalName().equals("query") ) {
            if( indexPath == null )
                error( "'indexPath' attribute missing from <query> element" );
        }
        
    } // parseOutput()
    
    /**
     * Parse an attribute on the main query element (or, for backward
     * compatability, on its immediate children.)
     * 
     * If the attribute isn't recognized, an error exception is thrown.
     */
    void parseMainAttrib( Element el, String name, String val )
    {
        if( name.equals("style") ) {
            displayStyle = Path.resolveRelOrAbs(baseDir, val);
            if( !(new File(displayStyle).canRead()) &&
                !val.equals("NullStyle.xsl") ) 
            {
                error( "Style \"" + displayStyle + 
                       "\" specified in '" + name + "' element " +
                    
                "does not exist" );
            }
        }

        else if( name.equals("startDoc") ) {
            startDoc = parseIntAttrib( el, "startDoc" );
            
            // Adjust for 1-based start doc.
            startDoc = Math.max( 0, startDoc-1 );
        }
        
        else if( name.equals("maxDocs") )
            maxDocs = parseIntAttrib( el, "maxDocs" );
        
        else if( name.equals("indexPath") ) {
            indexPath = Path.resolveRelOrAbs(baseDir, val);
            if( !(new File(indexPath).exists()) )
                error( "Index path \"" + indexPath + 
                       "\" specified in '" + name + "' element " +
                       "does not exist" );
        }
        
        else if( name.equals("termLimit") )
            termLimit = parseIntAttrib( el, "termLimit" );
        
        else if( name.equals("workLimit") )
            workLimit = parseIntAttrib( el, "workLimit" );
        
        else if( name.equals("sortMetaFields") )
            sortMetaFields = val;
        
        else if( name.equals("maxContext") )
            maxContext = parseIntAttrib( el, "maxContext" );
        
        // Backward compatability.
        else if( name.equals("contextChars") )
            maxContext = parseIntAttrib( el, "contextChars" );
        
        else if( name.equals("termMode") ) {
            if( val.equalsIgnoreCase("none") )
                termMode = SpanDocument.MARK_NO_TERMS;
            else if( val.equalsIgnoreCase("hits") )
                termMode = SpanDocument.MARK_SPAN_TERMS;
            else if( val.equalsIgnoreCase("context") )
                termMode = SpanDocument.MARK_CONTEXT_TERMS;
            else if( val.equalsIgnoreCase("all") )
                termMode = SpanDocument.MARK_ALL_TERMS;
            else
                error( "Unknown value for 'termMode'; expecting " +
                       "'none', 'hits', 'context', or 'all'" ); 
        }
        
        else if( name.equals("field") || name.equals("metaField") )
            ; // handled elsewhere
        
        else if( name.equals("inclusive") &&
                 el.getLocalName().equals("range") )
            ; // handled elsewhere
        
        else if( name.equals("slop") &&
                 el.getLocalName().equals("near") )
            ; // handled elsewhere
        
        else {
            error( "Unrecognized attribute \"" + name + "\" " +
                   "on <" + el.getLocalName() + "> element" );
        }
    } // parseMainAttrib()

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
     * If the given element has a 'field' attribute, return its value;
     * otherwise return 'parentField'. Also checks that field cannot be
     * specified if parentField has already been.
     */
    private String parseField( Element el, String parentField )
        throws QueryGenException
    {
        if( !el.hasAttribute("metaField") && !el.hasAttribute("field") )
            return parentField;
        String attVal = el.getAttribute("field");
        if( attVal == null || attVal.length() == 0 )
            attVal = el.getAttribute( "metaField" );
        
        if( attVal.length() == 0 )
            error( "'field' attribute cannot be empty" );
        if( attVal.equals("sectionType") )
            error( "'sectionType' is not valid for the 'field' attribute" );
        if( parentField != null && !parentField.equals(attVal) )
            error( "Cannot override ancestor 'field' attribute" );
        
        return attVal;
    }

    
    /**
     * Parse a 'sectionType' query element, if one is present. If not, 
     * simply returns null.
     */
    private SpanQuery parseSectionType( Element parent, 
                                        String field,
                                        int maxSnippets )
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
        
        // These sectionType queries only belong in the "text" field.
        if( !(field.equals("text")) )
            error( "'sectionType' element is only appropriate in queries on the 'text' field" );
        
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
        
        return (SpanQuery) parseQuery( child, "sectionType", maxSnippets );
    } // parseSectionType()
    
    /**
     * Recursively parse a query.
     */
    private Query parseQuery( Element parent, String field, int maxSnippets )
        throws QueryGenException
    {
        String name = parent.getLocalName();
        if( !name.matches(
                "^query$|^term$|^all$|^range$|^phrase$|^near$" +
                "|^and$|^or$|^not$" +
                "|^combine$|^meta$|^text$") ) // old stuff, for compatability
        {
            error( "Expected: 'query', 'term', 'all', 'range', 'phrase', " +
                   "'near', 'and', 'or', or 'not'; found '" + name + "'" );
        }
        
        // Old stuff, for compatability.
        if( name.equals("text") )
            field = "text";

        // 'not' queries are handled at the level above.
        assert( !name.equals("not") );
        
        // Default to no boost.
        float boost = 1.0f;
        
        // Validate all attributes.
        NamedNodeMap attrs = parent.getAttributes();
        for( int i = 0; i < attrs.getLength(); i++ ) {
            Attr   attr     = (Attr) attrs.item( i );
            String attrName = attr.getName();
            String attrVal  = attr.getValue();
            
            if( attrName.equals("boost") ) {
                try {
                    boost = Float.parseFloat( attrVal );
                }
                catch( NumberFormatException e ) {
                    error( "Invalid float value \"" + attrVal + "\" for " +
                           "'boost' attribute" );
                }
            }
            else if( attrName.equals("maxSnippets") ) {
                maxSnippets = parseIntAttrib( parent, attrName );
                if( maxSnippets < 0 )
                    maxSnippets = 999999999;
            }
            else
                parseMainAttrib( parent, attrName, attrVal );
        }
        
        // Do the bulk of the parsing below...
        Query result = parseQuery2( parent, name, field, maxSnippets );
        
        // And set any boost that was specified.
        if( boost != 1.0f )
            result.setBoost( boost );
        
        // If a sectionType query was specified, add that to the mix.
        SpanQuery secType = parseSectionType( parent, field, maxSnippets );
        if( secType != null ) {
            SpanQuery combo = 
                 new SpanSectionTypeQuery( (SpanQuery)result, secType );
            combo.setSpanRecording( ((SpanQuery)result).getSpanRecording() );
            result = combo;
        }
        
        // All done!
        return result;
        
    } // parseQuery()
    
    /** 
     * Main work of recursively parsing a query. 
     */
    private Query parseQuery2( Element parent, String name, String field,
                               int maxSnippets )
        throws QueryGenException
    {
        // Term query is the simplest kind.
        if( name.equals("term") ) {
            Term term = parseTerm( parent, field, "term" );
            SpanQuery q = isWildcardTerm(term) ? 
                new SpanWildcardQuery( term, termLimit ) :
                new SpanTermQuery( term );
            q.setSpanRecording( maxSnippets );
            return q;
        }
        
        // Get field name if specified.
        field = parseField( parent, field );
        
        // Range queries are also pretty simple.
        if( name.equals("range") )
            return parseRange( parent, field, maxSnippets );

        // For text queries, 'all', 'phrase', and 'near' can be viewed
        // as phrase queries with different slop values.
        //
        // 'all' means essentially infinite slop (limited to the actual
        //          chunk overlap at runtime.)
        // 'phrase' means zero slop
        // 'near' allows specifying the slop (again limited to the actual
        //          chunk overlap at runtime.)
        //
        if( name.equals("all") || name.equals("phrase") || name.equals("near"))
        {   
            int slop = name.equals("all") ? 999999999 :
                       name.equals("phrase") ? 0 :
                       parseIntAttrib( parent, "slop" );
            return makeProxQuery( parent, slop, field, maxSnippets );
        }
        
        // All other cases fall through to here: and, or. Use our special
        // de-duplicating span logic. First, get all the sub-queries.
        //
        Vector subVec = new Vector();
        Vector notVec = new Vector();
        for( ElementIter iter = new ElementIter(parent); iter.hasNext(); ) {
            Element el = iter.next();
            if( el.getLocalName().equals("sectionType") )
                ; // handled elsewhere
            else if( el.getLocalName().equals("not") ) { 
                Query q = parseQuery2(el, name, field, maxSnippets);
                if( q != null )
                    notVec.add( q );
            }
            else {
                Query q = parseQuery(el, field, maxSnippets);
                if( q != null )
                    subVec.add( q );
            }
        }
        
        // If no sub-queries, return an empty query.
        if( subVec.isEmpty() )
            return null;
        
        // If only one sub-query, just return that.
        if( subVec.size() == 1 && notVec.isEmpty() )
            return (Query) subVec.get(0);
        
        // Divide up the queries by field name.
        HashMap fieldQueries = new HashMap();
        for( int i = 0; i < subVec.size(); i++ ) {
            Query q = (Query) subVec.get(i);
            field = (q instanceof SpanQuery) ? 
                         ((SpanQuery)q).getField() : "<none>";
            if( !fieldQueries.containsKey(field) )
                fieldQueries.put( field, new Vector() );
            ((Vector)fieldQueries.get(field)).add( q );
        } // for i
        
        // Same with the "not" queries.
        HashMap fieldNots = new HashMap();
        for( int i = 0; i < notVec.size(); i++ ) {
            Query q = (Query) notVec.get(i);
            field = (q instanceof SpanQuery) ? 
                         ((SpanQuery)q).getField() : "<none>";
            if( !fieldNots.containsKey(field) )
                fieldNots.put( field, new Vector() );
            ((Vector)fieldNots.get(field)).add( q );
        } // for i
        
        // If we have only queries for the same field, our work is simple.
        if( fieldQueries.size() == 1 ) {
            Vector queries = (Vector) fieldQueries.values().iterator().next();
            Vector nots;
            if( fieldNots.isEmpty() )
                nots = new Vector();
            else {
                assert fieldNots.size() == 1 : "case not handled";
                nots = (Vector) fieldNots.values().iterator().next();
                assert nots.get(0) instanceof SpanQuery : "case not handled";
                String notField = ((SpanQuery)nots.get(0)).getField();
                String mainField = ((SpanQuery)queries.get(0)).getField();
                assert notField.equals(mainField) : "case not handled";
            }
            return processSpanJoin(name, queries, nots, maxSnippets);
        }
        
        // Now form a BooleanQuery containing grouped span queries where
        // appropriate.
        //
        BooleanQuery bq = new BooleanQuery();
        boolean require = !name.equals("or");
        TreeSet keySet = new TreeSet( fieldQueries.keySet() );
        for( Iterator i = keySet.iterator(); i.hasNext(); ) {
            field = (String) i.next();
            Vector queries = (Vector) fieldQueries.get( field );
            Vector nots = (Vector) fieldNots.get( field );
            if( nots == null )
                nots = new Vector();

            if( field.equals("<none>") ||
                (queries.size() == 1 && nots.isEmpty()) )
            {
                for( int j = 0; j < queries.size(); j++ )
                    bq.add( deChunk((Query)queries.get(j)), require, false );
                for( int j = 0; j < nots.size(); j++ )
                    bq.add( deChunk((Query)queries.get(j)), false, true );
                continue;
            }

            // Span query/queries. Join them into a single span query.
            SpanQuery sq = processSpanJoin(name, queries, nots, maxSnippets); 
            bq.add( deChunk(sq), require, false );
        } // for i

        // And we're done.
        return bq;        
    } // parseBoolean() 
        
    
    /**
     * Joins a number of span queries together using a span query.
     * 
     * @param name    'and', 'or', 'near', etc.
     * @param subVec  Vector of sub-clauses
     * @param notVec  Vector of not clauses (may be empty)
     * 
     * @return        A new Span query joining the sub-clauses.
     */
    private SpanQuery processSpanJoin( String name, Vector subVec, 
                                       Vector notVec, int maxSnippets )
    {
        SpanQuery[] subQueries = 
            (SpanQuery[]) subVec.toArray( new SpanQuery[0] ); 
        
        // Now make the top-level query.
        SpanQuery q;
        if( subQueries.length == 1 )
            q = subQueries[0];
        else if( !name.equals("or") ) {
            // We can't know the actual slop until the query is run against
            // an index (the slop will be equal to max proximity). So set
            // it to a big value for now, and it will be clamped by
            // fixupSlop() later whent he query is run.
            //
            q = new SpanNearQuery( subQueries, 999999999, false );
        }
        else
            q = new SpanOrQuery( subQueries );

        q.setSpanRecording( maxSnippets );
        
        // Finish up by handling any not clauses found.
        return processTextNots( q, notVec, maxSnippets );
        
    } // processSpanJoin()

    /**
     * Ensures that the given query, if it is a span query on the "text"
     * field, is wrapped by a de-chunking query.
     */
    private Query deChunk( Query q )
    {
        // We only need to de-chunk span queries, not other queries.
        if( !(q instanceof SpanQuery) )
            return q;
        
        // Furthermore, we only need to de-chunk queries on the "text"
        // field.
        //
        SpanQuery sq = (SpanQuery) q;
        if( !sq.getField().equals("text") )
            return q;
        
        // If it's already de-chunked, no need to do it again.
        if( sq instanceof SpanDechunkingQuery )
            return q;
        
        // Okay, wrap it.
        SpanDechunkingQuery dq = new SpanDechunkingQuery( sq );
        dq.setSpanRecording( sq.getSpanRecording() );
        return dq;
        
    } // deChunk()  
      
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
     * Parse a range query.
     */
    private Query parseRange( Element parent, String field, int maxSnippets )
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
                lower = parseTerm( child, field, "lower" );
            }
            else if( name.equals("upper") ) {
                if( upper != null )
                    error( "'upper' only allowed once as child of 'range' element" );
                upper = parseTerm( child, field, "upper" );
            }
            else
                error( "'range' element may only have 'lower' and/or 'upper' " +
                       "as child elements" );
        } // for iter
        
        // Upper, lower, or both must be specified.
        if( lower == null && upper == null )
            error( "'range' element must have 'lower' and/or 'upper' child element(s)" );
        
        // And we're done.
        SpanQuery q = new SpanRangeQuery( lower, upper, inclusive, termLimit );
        q.setSpanRecording( maxSnippets );
        return q;
    } // parseRange()

    /**
     * If any 'not' clauses are present, this builds a query that filters them
     * out of the main query.
     */
    SpanQuery processTextNots( SpanQuery query, Vector notClauses,
                               int maxSnippets ) 
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
            subQuery.setSpanRecording( maxSnippets );
        }
        
        // Now make the final 'not' query. Note that the actual slop will have
        // to be fixed when the query is run.
        //
        SpanQuery nq = new SpanChunkedNotQuery( query, subQuery, 999999999 );
        nq.setSpanRecording( maxSnippets );
        return nq;
    } // processTextNots();
    
    
    /**
     * Generate a proximity query on a field. This uses the de-duplicating span
     * system.
     * 
     * @param parent The element containing the field name and terms.
     */
    Query makeProxQuery( Element parent, int slop, String field,
                         int maxSnippets )
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
                notVec.add( parseQuery(el, field, maxSnippets) );
            }
            else {
                SpanQuery q;
                if( slop == 0 ) {
                    Term t = parseTerm( el, field, "term" );
                    if( isWildcardTerm(t) )
                        q = new SpanWildcardQuery(t, termLimit);
                    else
                        q = new SpanTermQuery(t);
                    q.setSpanRecording( maxSnippets );
                    terms.add( q );
                }
                else
                    terms.add( parseQuery(el, field, maxSnippets) );
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
                                  slop,
                                  inOrder );
        q.setSpanRecording( maxSnippets );
        
        // And we're done.
        return q;
        
    } // makeTextAllQuery()
    
    
    /**
     * Parses a 'term' element. If not so marked, an exception is thrown.
     * 
     * @param parent The element to parse
     */
    private Term parseTerm( Element parent, String field, String expectedName )
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
