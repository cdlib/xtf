package org.cdlib.xtf.dynaXML.test;

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

import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.w3c.dom.Attr;

/**
 * Performs brute-force (that is, stupid but reliable) single-term searching
 * and hit marking on a DOM tree.
 * 
 * @author Martin Haye
 */
public class TreeAnnotater
{
  private StandardAnalyzer analyzer = new StandardAnalyzer();
  private String searchTerm;
  private Document doc;
  private static final String xtfUri = "http://cdlib.org/xtf";
  private int totalHitCount = 0;
  
  /**
   * Process an entire document, marking hits and hit counts as we go.
   */
  public void processDocument( Document doc, String term ) {
    this.doc = doc;
    this.searchTerm = term;
    int nHits = processElement( doc.getDocumentElement(), 0 );
    assert( totalHitCount == nHits );
  }
  
  /**
   * Determine whether a string contains only whitespace characters.
   */
  private boolean isAllWhitespace( String s ) 
  {
    for( int i = 0; i < s.length(); i++ ) {
        if( !Character.isWhitespace(s.charAt(i)) )
            return false;
    }
    return true;
    
  } // isAllWhitespace()
  
  /**
   * Traverse an element of the tree. Process all its children, and if any has
   * hits, record the hit count on this element.
   * 
   * @param parent  The element to traverse
   * @return        The number of hits found within it.
   */
  public int processElement( Element parent, int level ) 
  {
    int firstHit = totalHitCount;
    
    // Process each child in turn.
    int nHits = 0;
    Node next;
    for( Node child = parent.getFirstChild(); child != null; child = next ) 
    {
        // Before expanding or deleting anything, record the next real node
        // in the tree so we can process it next.
        //
        next = child.getNextSibling();
        
        // Process text nodes separately.
        if( child instanceof Text ) 
        {
            // Remove text nodes that are only whitespace.
            if( isAllWhitespace(((Text)child).getData()) )
                parent.removeChild( child );
            else
                nHits += processText( (Text)child );
        }
        else if( child instanceof Element ) {
            if( child.getNodeName().equals("xtf:hit") )
                continue;
            nHits += processElement( (Element)child, level+1 );
        }
    } // for i
    
    // Record the hit count if non-zero (and always on the root node, even if 0).
    if( nHits > 0 || level < 1 ) {
        Attr attr = doc.createAttributeNS( xtfUri, "xtf:hitCount" );
        attr.setValue( Integer.toString(nHits) );
        parent.setAttributeNode( attr );

        attr = doc.createAttributeNS( xtfUri, "xtf:firstHit" );
        attr.setValue( Integer.toString(firstHit+1) ); // XSLT is 1-based
        parent.setAttributeNode( attr );
    }
    
    return nHits;
    
  } // process()
  
  /**
   * Recursively scans a text node for hits. All hits are marked with 
   * special elements.
   *
   * @param node  Node to scan
   * @return      How many hits were found
   */
  private int processText( Text node )
  {
    int hitCount = 0;
    
    // Tokenize the characters, looking for a search term.
    while( true ) {
       String text = node.getData();
       TokenStream tokens = 
            analyzer.tokenStream( "text", new StringReader(text) );
        Token t;
        while( true ) {
            try { t = tokens.next(); }
            catch( IOException e ) { throw new RuntimeException(e); }
            if( t == null )
                break;
            if( t.termText().equals(searchTerm) )
                break;
        }
        
        if( t == null )
            break;

        // Output the non-term text.
        node.setData( text.substring(0, t.startOffset()) );
        
        // Now do the <hit> and <term> elements.
        Element hitEl  = doc.createElementNS(xtfUri, "xtf:hit");
        totalHitCount++; // Increment before setting attr to get 1-based for XSL.
        hitEl.setAttribute( "hitNum", 
                            Integer.toString(totalHitCount) );
        node.getParentNode().insertBefore( hitEl, node.getNextSibling() );
        
        Element termEl = doc.createElementNS(xtfUri, "xtf:term");
        hitEl.appendChild( termEl );
        
        Text termText = doc.createTextNode( 
                           text.substring(t.startOffset(), t.endOffset()) );
        termEl.appendChild( termText );
        
        // Bump the hit count
        hitCount++;
        
        // Resume where we left off.
        Text nextText = doc.createTextNode(
                           text.substring(t.endOffset(), text.length()) );
        node.getParentNode().insertBefore( nextText, hitEl.getNextSibling() );
        node = nextText;
    } // while
    
    // All done!
    return hitCount;
    
  } // processText()

} // class SearchFilter
