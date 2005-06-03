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
        
        // Get the next input token.
        Token curToken  = input.next();
        final int startPos = (curToken != null) ? curToken.startOffset() : srcChars.length;
        final int endPos   = (curToken != null) ? curToken.endOffset()   : srcChars.length;
        
        // If we're not within an element, simply return tokens until we reach
        // an element start.
        //
        if( !inElement )
        {
            // If we're at the end of the stream, get out.
            if( curToken == null )
                return curToken;
            
            // Is this start of an element? If not, return it verbatim.
            if( srcChars[startPos] != '<' )
                return curToken;
            
            // Remember that we're starting an element, and get the name of it.
            elementStart = curToken;
            inElement = true;
            continue;
        }
        
        // If we're starting an element, this token must be the name.
        if( elementStart != null ) {
          
            // Is this the end tag? If so, include the extra ">" in it.
            int start = elementStart.startOffset();
            int end = endPos;
            if( srcChars[startPos-1] == '/' ) {
                while( end < srcChars.length && srcChars[end] != '>' )
                    end++;
            }
    
            // Okay, we found the start of the element. Make a new token.
            Token newToken = new Token( new String(srcChars, start, end-start),
                                        start, end,
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
            if( i < startPos )
                inQuote = false;
            else
                quoteStart = endPos-1;
        }
        
        // Does this token mark the end of the element?
        if( srcChars[startPos] == '>' ) {
            // Include the '/' if the element has no content.
            int start = startPos;
            if( srcChars[startPos-1] == '/' )
                --start;
            
            Token endToken = new Token( new String(srcChars, start, endPos-start),
                                        start, endPos,
                                        XML_TYPE );
            inElement = false;
            return endToken;
        }
        
        // Inside a quote, add a quote mark to differentiate the token from normal
        // ones.
        //
        if( inQuote ) {
            Token attrToken = new Token( '\"' + curToken.termText() + '\"',
                                         startPos, endPos,
                                         XML_TYPE );
            return attrToken;
        }
        
        // Is this the start of an attribute name? If so, mark it and look for the
        // end of the name.
        //
        if( !inAttrName ) {
            attrNameStart = startPos;
            inAttrName = true;
            continue;
        }
        
        // If no '=' found, keep scanning for the end of the attr name.
        if( srcChars[startPos] != '=' )
            continue;
          
        // Found it. Record the attribute name.
        String attrName = 
            new String(srcChars, attrNameStart, startPos-attrNameStart).trim();
        Token attrToken = new Token( attrName + "=",
                                     attrNameStart, endPos,
                                     XML_TYPE );

        // We're done with the name now, and ready for the quoted contents.
        inAttrName = false;
        inQuote = true;
        quoteStart = endPos;
        while( quoteStart < srcChars.length && srcChars[quoteStart] != '\"' )
            quoteStart++;
        
        // Special case: empty string
        if( quoteStart+1 < srcChars.length && srcChars[quoteStart+1] == '\"' ) {
            tokenQueue.add( new Token("\"\"",
                                      quoteStart, quoteStart,
                                      XML_TYPE) );
        }
        
        return attrToken;

    } // while
    
  } // next()
    
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

      String out = outBuf.toString();
      //out = out.replaceAll(" ", "");
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
        
        assert testFilter("<element>").equals(":<element:>");
        
        assert testFilter("<element attr=\"a\"/>").
                   equals(":<element:attr=:\"a\":/>");
        
        assert testFilter("<element attr=\"a b c\"/>").
                   equals(":<element:attr=:\"a\":\"b\":\"c\":/>");
        
        assert testFilter("<element attr=\"\"/>").
                   equals(":<element:attr=:\"\":/>");

        assert testFilter("<element/>").equals(":<element:/>");
        
        assert testFilter("<element>x y z</element>").
                   equals(":<element:>:x:y:z:</element:>");
        
        assert testFilter("<element att1=\"foo bar\" att2=\"wow\">hello there</element>").
                   equals(":<element:att1=:\"foo\":\"bar\":att2=:\"wow\":>:hello:there:</element:>");
        
        String bump = XtfSpecialTokensFilter.bumpMarker +
                      "5" +
                      XtfSpecialTokensFilter.bumpMarker;
        
        assert testFilter("x" + bump + "<element att=\"a\"/>").
                   equals(":x::::::<element:att=:\"a\":/>");
        
        assert testFilter("<el1/>" + bump + "<el2/>").
                   equals(":<el1:/>::::::<el2:/>");
        
        assert testFilter("<$ foo=\"bar\">xyz").
                   equals(":<$:foo=:\"bar\":>:xyz");
        
      } catch (IOException e) {
        assert false;
      }

    } // testImpl()
  };
  
} // class TagFilter
