package org.cdlib.xtf.textIndexer;

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
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.cdlib.xtf.util.Tester;
import org.cdlib.xtf.util.Trace;

/**
 * Optimizes query speed by getting rid of stop words, but doing it in a way
 * that still allows them to be queried. We do this by joining stop words to
 * their neighboring (non-stop) words to form what Doug Cutting of Lucene fame
 * calls "n-grams."
 * 
 * An example: "man of the year" would be indexed as "man man-of the-year 
 * year". Then a query for the exact phrase "man of the year" would query for 
 * "man-of the-year" and get the correct hit without needing to scan the 
 * 50-zillion occurrences of "of" and "the".
 * 
 * @author Martin Haye
 */
public class NgramStopFilter extends TokenFilter
{
  private Set           stopSet;
  private int           chunkSize;
  
  private boolean       firstTime = true;
  
  private Token         nextToken = null;
  private Token         outputQueue = null;
  
  private int           accumIncrement = 0;
  
  private final Token   virtualToken = new Token( 
                                 XtfSpecialTokensFilter.virtualWord, 0, 0 );

  private int           prevEndPos    = 0;
  
  private int           outputPos = 0;
  private int           inputPos  = 0;
  
  /**
   * Construct a token stream to filter 'stopWords' out of 'input'.
   * 
   * @param input       Input stream of tokens to process
   * @param stopSet     Set of stop words to filter out. This can be most easily
   *                    made by calling {@link #makeStopSet(String) makeStopSet()}.
   */
  public NgramStopFilter( TokenStream  input, 
                          Set          stopSet,
                          int          chunkSize )
  {
    // Initialize the super-class
    super( input );

    // Record the set of stop words and the chunk size for later reference.
    this.stopSet = stopSet;
    this.chunkSize = chunkSize;
    
  } // constructor

  /**
   * Make a stop set given a space, comma, or semicolon delimited list of
   * stop words.
   *
   * @param stopWords   String of words to make into a set
   *
   * @return            A stop word set suitable for use when constructing an
   *                    {@link NgramStopFilter NgramStopFilter}.
   */  
  public static Set makeStopSet( String stopWords )
  {
    // Break the list of stop words into a set. Be sure to convert the words
    // to lower-case, because the set of incoming tokens is assumed to already
    // be lower-case.
    //
    HashSet set = new HashSet();
    StringTokenizer stok = new StringTokenizer( stopWords, " \r\n\t\f\b,;" );
    
    while( stok.hasMoreTokens() )
        set.add( stok.nextToken().toLowerCase() );
        
    return set;    
  } // makeStopSet()
  
  /**
   * Retrieve the next token in the stream. Adds a layer of checking on top,
   * to make absolutely sure that we don't accidentally introduce extra
   * position increments, or miss some.
   */
  public Token next()
    throws IOException
  {
    Token t = nextInternal();
    if( t != null ) {
        outputPos += t.getPositionIncrement();
        assert outputPos <= inputPos;
    }
    
    return t;
  }
  
  /**
   * Retrieve the next token in the stream.
   */
  public Token nextInternal()
    throws IOException
  {
    // If this is the first time through, prime the pump.
    if( firstTime ) {
        nextToken = nextInput();
        firstTime = false;
    }
    
    // If we've queued up an output token, output it now.
    if( outputQueue != null ) {
        Token t = outputQueue;
        outputQueue = null;
        return t;
    }
    
    // Keep going until we get a real token we can output.
    while( true ) 
    {
        // Advance to the next input token.
        Token curToken = nextToken;
        if( curToken == null )
            return null;
        nextToken = nextInput();
        
        // Flags.
        boolean curIsReal  = !stopSet.contains( curToken.termText() );
    
        // There are four main cases to deal with:
        // (1) A real word followed by another real word
        // (2) A real word followed by a stop-word
        // (3) A stop word followed by a real word
        // (4) A stop word followed by another stop word.
        //
        
        // First, deal with cases (1) and (2), which begin with a real word.
        if( curIsReal )
        {
            boolean nextIsReal = nextToken != null && 
                                 !stopSet.contains(nextToken.termText()) &&
                                 nextToken.getPositionIncrement() == 1;

            // In all cases, we're going to return the real word. So take care
            // of any increment accumulated from entirely removed stop words.
            //
            if( accumIncrement > 0 ) {
                curToken.setPositionIncrement( 
                       curToken.getPositionIncrement() + accumIncrement );
                accumIncrement = 0;
            }

            // Case 1: Real word followed by another real word. Just pass the current
            //         token through without any change.
            //
            // Example: <quick>,brown,fox -> <quick>,brown,fox
            //
            if( nextIsReal )
                return curToken;
        
            // Case 2: Real followed by stop. Pass on the real token, but also 
            //         queue a token of the real glommed with the stop.
            //
            // Example: <man>,of,war -> <man> <man-of>,of-war,war
            //
            if( nextToken != null && nextToken.getPositionIncrement() == 1 )
                outputQueue = glomToken( curToken, nextToken, 0 );
            return curToken;
        }
        
        // Now deal with cases (3) and (4), which begin with a stop word.
        // If the next token has a normal increment (normal being 1), then
        // glom the current with the next. For abnormal increment, don't
        // glom, and skip the single stop word.
        //
        if( nextToken == null ||
            nextToken.getPositionIncrement() > 1 )
        {
            accumIncrement += curToken.getPositionIncrement();
            continue;
        }
        
        // Glom away!
        return glomToken( curToken, nextToken, 1 );
    } // while( true )
    
  } // next()
  
  private Token nextInput() throws IOException
  {
    Token t = input.next();
    if( t != null ) {
        inputPos += t.getPositionIncrement();
        if( chunkSize >= 0 )
            assert inputPos <= chunkSize;
    }
    return t;
  }

  /**
   * Constructs a new token, drawing the start position, position increment,
   * and end position from the specified tokens.
   */
  private Token glomToken( Token token1, Token token2, int increment )
  {
    // Create the new token from the text of both.
    Token token = new Token( token1.termText() + "~" + token2.termText(),
                             token1.startOffset(),
                             token2.endOffset(),
                             token1.type() );
    
    // Calculate the position increment. The input 'increment' will be either
    // zero (for overlapping), or one (normal). Also, we might have accumulated
    // some increments from removed stop-word pairs.
    //
    assert token2.getPositionIncrement() == 1 : "Cannot glom around word bumps";
    if( increment == 0 )
        token.setPositionIncrement( 0 );
    else
        token.setPositionIncrement( token1.getPositionIncrement() 
                                    - 1 
                                    + increment + accumIncrement );
    accumIncrement = 0;
    
    // All done now.
    return token; 
  } // glomToken()
  
  /**
   * Basic regression test
   */
  public static final Tester tester = new Tester("NgramStopFilter") 
  {
    /**
     * Very simple tokenizer that breaks up a string into a series of Lucene
     * {@link Tokens Token}s.
     */  
    class StringTokenStream extends TokenStream
    {
        private String          str;
        private int             prevEnd = 0;
        private StringTokenizer tok;
        private int             count = 0;
        
        public StringTokenStream( String str, String delim ) {
            this.str = str;
            tok = new StringTokenizer( str, delim );
        }
        
        public Token next() {
            if( !tok.hasMoreTokens() )
                return null;
            count++;
            String term = tok.nextToken();
            Token t = new Token( term,
                                 str.indexOf(term, prevEnd),
                                 str.indexOf(term, prevEnd)+term.length(),
                                 "word" );
            if( t.startOffset() > 0 &&
                str.charAt(t.startOffset()-1) == '.' )
            {
                t.setPositionIncrement( 5 );
            }
            prevEnd = t.endOffset();
            return t;
        }
    } // class StringTokenStream
    
    private HashSet stopSet = new HashSet();
    
    /**
     * Tokenize, filter, and stick back together the input string.
     */
    private String testFilter( String in )
      throws IOException
    {
      StringTokenStream sts = new StringTokenStream( in, " ." );
      NgramStopFilter nsf = new NgramStopFilter( sts, stopSet, -1 );
      
      StringBuffer outBuf = new StringBuffer();
      while( true ) {
          Token t = nsf.next();
          if( t == null )
              break;
          for( int i = 0; i < t.getPositionIncrement(); i++ )
              outBuf.append( '/' );
          if( t.getPositionIncrement() == 0 )
              outBuf.append( ',' );
          outBuf.append( in.substring(t.startOffset(), t.endOffset()) );
      }
      
      String out = outBuf.toString();
      out = out.replaceAll( " ", "" );
      Trace.debug( in + " --> " + out );
      return out;
    } // test()
    
    /**
     * Run the test.
     */
    protected void testImpl() 
    {
      stopSet.add( "a" );
      stopSet.add( "b" );
      stopSet.add( "c" );
      stopSet.add( "d" );
      stopSet.add( "e" );
      
      try {
          assert testFilter("x y").equals("/x/y");
          assert testFilter("x a y").equals("/x,xa/ay/y");
          assert testFilter("x a b y z").equals("/x,xa/ab/by/y/z");
          assert testFilter("x a b c y").equals("/x,xa/ab/bc/cy/y");
          assert testFilter("x a b c d y e").equals("/x,xa/ab/bc/cd/dy/y,ye");
          assert testFilter("x a b").equals("/x,xa/ab");
          assert testFilter("a b y").equals("/ab/by/y");
          assert testFilter("x.y.z").equals("/x/////y/////z");
          assert testFilter("x a.b y").equals("/x,xa//////by/y");
          assert testFilter("x.a b y").equals("/x/////ab/by/y");
          assert testFilter("x a b.y").equals("/x,xa/ab//////y");
          assert testFilter("x a b.c y").equals("/x,xa/ab//////cy/y");
          assert testFilter("x a b.c d y").equals("/x,xa/ab//////cd/dy/y");
          assert testFilter("a x.y b z").equals("/ax/x/////y,yb/bz/z");
          assert testFilter("x y a.z").equals("/x/y,ya//////z"); // yes, 6 slashes
      }
      catch( IOException e ) {
          assert false;
      }
      
    } // testImpl()
  };

} // class NgramStopFilter
