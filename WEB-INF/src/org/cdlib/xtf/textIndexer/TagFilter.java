package org.cdlib.xtf.textIndexer;

/*
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
import java.util.LinkedList;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.cdlib.xtf.textEngine.Constants;
import org.cdlib.xtf.util.FastStringReader;
import org.cdlib.xtf.util.Tester;
import org.cdlib.xtf.util.Trace;

/*
 * This file created on Apr 14, 2005 by Martin Haye
 */

/**
 * Spots XML elements in a token stream and marks them specially.
 * 
 * @author Martin Haye
 */
public class TagFilter extends TokenFilter 
{
  /** Type of returned 'element' tokens */
  public static final String XML_TYPE = "ELEMENT".intern();
  
  /** The source text being tokenized */
  private char[] srcChars;
  
  /** True while we're processing inside an element definition */
  private boolean inElement = false;
  
  /** Name of the last element we've started */
  private String elementName = null; 
  
  /** True while we're in an element end tag */
  private boolean inEndTag = false;
  
  /** Start position of insides of element def */
  private Token elementStart = null;
  
  /** True when we're within a quoted attribute value */
  private boolean inQuote = false;
  
  /** Position of initial quote mark */
  private int quoteStart = -1;
  
  /** True when we're within an attribute name */
  private boolean inAttrName = false;
  
  /** Start position of the attribute name */
  private int attrNameStart = -1;
  
  /** Name of the current attribute */
  private String attrName = null;
  
  /** Accumulated position increment for next actual token emitted */
  private int accumPosIncr = 0;
  
  /** Queued tokens */
  private LinkedList tokenQueue = new LinkedList();
  
  /**
   * Construct a token stream to mark XML elements.
   * 
   * @param input       Input stream of tokens to process
   */
  public TagFilter( TokenStream input, String srcText )
  {
    // Initialize the super-class
    super(input);
    
    // Record a reference to the source text, in a handy form.
    srcChars = srcText.toCharArray();
    
  } // constructor
  
  /** Retrieve the next token in the stream. */ 
  public Token next() throws IOException 
  {
    // Loop until we can generate a real token.
    while( true ) {
    
        // If there are queued tokens, return the first one.
        if( !tokenQueue.isEmpty() )
            return (Token) tokenQueue.removeFirst();
        
        // Get the next input token and process it.
        Token curToken = input.next();
        Token toReturn = processNext( curToken );
        if( toReturn != null || curToken == null ) 
        {
            // If tokens were queued before this one, avoid going out of order.
            if( tokenQueue.isEmpty() )
                return toReturn;
            else
                tokenQueue.add( toReturn );
        }
    } // while
    
  } // next()
        
  /**
   * Does most of the work of processing a token
   * 
   * @param curToken    The token from the input stream
   * @return            Token to return immediately, or null for none. If this is
   *                    null and curToken was null, should return null
   *                    immediately.
   */
  private Token processNext( Token curToken )
  {
    int startPos = (curToken != null) ? curToken.startOffset() : srcChars.length;
    int endPos   = (curToken != null) ? curToken.endOffset()   : srcChars.length;

    // If we're not within an element, simply return tokens until we reach
    // an element start.
    //
    if( !inElement )
    {
        // If we're at the end of the stream, get out.
        if( curToken == null )
            return null;
        
        // Is this an entity ref?
        if( startPos > 0 && srcChars[startPos-1] == '&' ) 
        {
            String tokVal;
            if( curToken.termText().equals("amp") )
                tokVal = "&";
            else if( curToken.termText().equals("lt") )
                tokVal = "<";
            else if( curToken.termText().equals("gt") )
                tokVal = ">";
            else
                throw new RuntimeException( "Unexpected entity ref" );
            
            return new Token( tokVal, startPos-1, endPos+1, XML_TYPE );  
        }
        
        // Is this start of an element? If not, return it verbatim.
        if( srcChars[startPos] != '<' )
            return curToken;
        
        // Remember that we're starting an element, and get the name of it.
        elementStart = curToken;
        inElement = true;
        return null;
    }
    
    // If we're starting an element decl, this token must be the name.
    if( elementStart != null ) 
    {
        // Record the element name.
        elementName = new String( srcChars, startPos, endPos-startPos );

        // Skip the opening '<' when forming the token (but include the '/'
        // if this is ending the element.)
        //
        int start = elementStart.startOffset() + 1;
        int end   = endPos;
        
        // Record whether this is the start or end of the tag.
        inEndTag = srcChars[start] == '/';
        
        // Okay, make a new token.
        String name = Constants.ELEMENT_MARKER + 
                      (inEndTag ? "" : "<") + 
                      new String(srcChars, start, end-start);
        Token newToken = new Token( name, 
                                    elementStart.startOffset(), end, 
                                    XML_TYPE );
        newToken.setPositionIncrement( elementStart.getPositionIncrement() );
        elementStart = null;
        return newToken;
    }
    
    // If we're in a quoted string, see if the end of the quote has been 
    // reached
    //
    if( inQuote ) {
        int i = quoteStart + 1;
        while( i < startPos && srcChars[i] != '\"' )
            i++;
        if( i < startPos ) {
            String tokValue = Constants.ATTRIBUTE_MARKER +
                              "]" + attrName;
            tokenQueue.add( new Token(tokValue, startPos, startPos, XML_TYPE) );
            inQuote = false;
        }
        else
            quoteStart = endPos-1;
    }
    
    // Does this token mark the end of the element declaration?
    if( srcChars[startPos] == '>' ) 
    {
        // Go back to normal mode.
        inElement = false;

        // Mark the end of the element declaration (only if this is the start of 
        // the tag.)
        //
        if( !inEndTag ) {
            tokenQueue.add( new Token(Constants.ELEMENT_MARKER + ">" + elementName,
                                      startPos, endPos, XML_TYPE) );
        }
        inEndTag = false;
        
        // If preceded by a '/', this is a shorthand for an element with no
        // content. Emit an end token for it anyway.
        //
        if( srcChars[startPos-1] == '/' ) {
            String name = Constants.ELEMENT_MARKER + "/" + elementName;
            return new Token( name,startPos, endPos, XML_TYPE );
        }
        
        return null;
    } // if
    
    // Inside a quote, add a marker to differentiate the token from normal
    // ones.
    //
    if( inQuote ) {
        String name = Constants.ATTRIBUTE_MARKER + curToken.termText();
        return new Token( name, startPos, endPos, XML_TYPE );
    }
    
    // Is this the start of an attribute name? If so, mark it and look for the
    // end of the name.
    //
    if( !inAttrName ) {
        attrNameStart = startPos;
        inAttrName = true;
        return null;
    }
    
    // If no '=' found, keep scanning for the end of the attr name.
    if( srcChars[startPos] != '=' )
        return null;
      
    // Found it. Record the attribute name.
    attrName = new String(
        srcChars, attrNameStart, startPos-attrNameStart).trim();
    tokenQueue.add( new Token(Constants.ATTRIBUTE_MARKER + "[" + attrName,
                              attrNameStart, endPos,
                              XML_TYPE) );

    // We're done with the name now, and ready for the quoted contents.
    inAttrName = false;
    inQuote = true;
    quoteStart = endPos;
    while( quoteStart < srcChars.length && srcChars[quoteStart] != '\"' )
        quoteStart++;
    
    // Special case: empty string
    if( quoteStart+1 < srcChars.length && srcChars[quoteStart+1] == '\"' ) {
        return new Token( Constants.ATTRIBUTE_MARKER + "",
                          quoteStart, quoteStart,
                          XML_TYPE );
    }

    return null;
    
  } // processNext()
    
  /**
   * Basic regression test
   */
  public static final Tester tester = new Tester("TagFilter") {

    /**
     * Tokenize, filter, and stick back together the input string.
     */
    private String testFilter( String in ) throws IOException 
    {
      XTFTextAnalyzer analyzer = new XTFTextAnalyzer( null, null, null );
      TokenStream stream = analyzer.tokenStream( "meta", new FastStringReader(in) );

      StringBuffer outBuf = new StringBuffer();
      while (true) {
        Token t = stream.next();
        if (t == null)
          break;
        if (t.getPositionIncrement() == 0)
          outBuf.append(',');
        else {
            for (int i = 0; i < t.getPositionIncrement(); i++)
              outBuf.append(':');
        }
        outBuf.append(t.termText());
      }
      
      char[] chars = outBuf.toString().toCharArray();
      for( int i = 0; i < chars.length; i++ ) {
          switch( chars[i] ) {
          case Constants.ELEMENT_MARKER:
              chars[i] = '!';
              break;
          case Constants.ATTRIBUTE_MARKER:
              chars[i] = '@';
              break;
          }
      }
      
      String out = new String( chars );
      Trace.debug(in + " --> " + out);
      return out;
    } // test()

    /**
     * Run the test.
     */
    protected void testImpl() {
      try {
        //Trace.setOutputLevel( Trace.debug );
        
        assert testFilter("x y").equals(":x:y");
        
        assert testFilter("<element>").equals(":!<element:!>element");
        
        assert testFilter("<element>x y z</element>").
                   equals(":!<element:!>element:x:y:z:!/element");
        
        assert testFilter("<element attr=\"a\"/>").
                   equals(":!<element:@[attr:@a:@]attr:!>element:!/element");
        
        assert testFilter("<element attr=\"a b c\"/>").
                   equals(":!<element:@[attr:@a:@b:@c:@]attr:!>element:!/element");
        
        assert testFilter("<element attr=\"\"/>").
                   equals(":!<element:@[attr:@:@]attr:!>element:!/element");

        assert testFilter("<element/>").equals(":!<element:!>element:!/element");
        
        assert testFilter("<element att1=\"foo bar\" att2=\"wow\">hello there</element>").
                   equals(":!<element:@[att1:@foo:@bar:@]att1:@[att2:@wow:@]att2:!>element:hello:there:!/element");
        
        String bump = Constants.BUMP_MARKER +
                      "5" +
                      Constants.BUMP_MARKER;
        
        assert testFilter("x" + bump + "<element att=\"a\"/>").
                   equals(":x::::::!<element:@[att:@a:@]att:!>element:!/element");
        
        assert testFilter("<el1/>" + bump + "<el2/>").
                   equals(":!<el1:!>el1:!/el1::::::!<el2:!>el2:!/el2");
        
        assert testFilter("<$ foo=\"bar\">xyz").
                   equals(":!<$:@[foo:@bar:@]foo:!>$:xyz");
        
      } catch (IOException e) {
        assert false;
      }

    } // testImpl()
  };
  
} // class TagFilter
