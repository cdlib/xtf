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

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.TokenFilter;
import org.cdlib.xtf.textEngine.Constants;

////////////////////////////////////////////////////////////////////////////////

/**
 * The <code>XtfSpecialTokensFilter</code> class is used by the 
 * {@link XTFTextAnalyzer} class to convert special "bump" count values in
 * text chunks to actual position increments for words prior to adding them 
 * to a Lucene index. <br><br>
 * 
 * The way in which Lucene adds words to an index database is to convert a 
 * contiguous chunk of text into a list of discrete words (<b><i>tokens</i></b>
 * in Lucene parlance.) Then, when the Lucene <code>IndexWriter.addDocument()</code> 
 * function is called, Lucene traverses the list of tokens, and calls an
 * instance of a <code>TokenFilter</code> derived class to pre-process each 
 * token. The resulting output from the filter is what Lucene actually 
 * adds to the database. <br><br>
 * 
 * Each token entry in the list consists of the token (word) itself, and its 
 * position increment from the previous token (referred to as "word bump" in 
 * other text indexer related classes.) Since a special bump count value in
 * the original text looks like any other token to Lucene, it simply passes it
 * on to the <code>XtfSpecialTokensFilter</code> to pre-process. The 
 * filter recognizes the special token, removes it from the token list,
 * converts it to a number, and sets it as the position increment for the
 * first non-special token that follows. The output of the
 * <code>XtfSpecialTokensFilter</code> is then a list of actual tokens to be 
 * indexed and their associated position increments. <br><br>
 * 
 * For more information on word bump and virtual words, see the 
 * {@link XMLTextProcessor} class, and its member function 
 * {@link XMLTextProcessor#insertVirtualWords(StringBuffer) insertVirtualWords() }.
 */

public class XtfSpecialTokensFilter extends TokenFilter 

{
  
  /** A reference to the original contiguous text that the input token list
   *  corresponds. See the 
   *  {@linkplain XtfSpecialTokensFilter#XtfSpecialTokensFilter(TokenStream, String) constructor}
   *  for more about how this reference is used. <br><br>
   *  
   */
  private String srcText;
  
  //////////////////////////////////////////////////////////////////////////////
 
  /** Constructor for the <code>XtfSpecialTokensFilter</code>. <br><br>
   * 
   *  @param  srcTokens  The source token stream to filter.
   * 
   *  @param  srcText    The original source text chunk from wich the source
   *                     token stream was derived. <br><br>
   * 
   * @.notes
   *    This class stores a reference to the original chunk of text from which 
   *    the source token stream is derived. This is so that the filter can
   *    perform look-back and look-ahead operations to identify special token
   *    by their markers. This is necessary because the standard tokenizer 
   *    that creates the source token stream for this filter considers our 
   *    markers to be punctuation  rather than part of the token, and strips 
   *    them out. <br><br>
   */
  public XtfSpecialTokensFilter( TokenStream srcTokens, String srcText )
  
  {
    super( srcTokens );
    this.srcText = srcText;
  }
  
  
  //////////////////////////////////////////////////////////////////////////////
  
  /** Return the next output token from this filter. <br><br>
   * 
   *  Called by Lucene to retrieve the next non-special token from this filter.
   *  <br><br>
   *
   *  @return  The next non-special token output by this filter. <br><br>
   *  
   *  @throws  IOException  Any exceptions generated by the look-back/look-ahead
   *                        character processing performed by this function.
   *                        <br><br>
   * 
   *  @.notes
   *    For more information about the filtering performed by this function,
   *    see the {@link XtfSpecialTokensFilter} class description. <br><br>
   */
  public Token next() throws IOException

  {
    int   defaultWordBump = 1;
    int   bumpValue       = defaultWordBump;
    Token theToken        = null;
    
    // Process (possibly) multiple special bump tokens in a row.
    for(;;) {
    
        // Get the next token. If there isn't one, we're done.
        theToken = input.next();
        if( theToken == null ) return null;
        
        // Figure out where the token starts and ends in the source text.
        int wordStart = theToken.startOffset();
        int wordEnd   = theToken.endOffset();
        
        // Assume the current token isn't flanked by the special token markers.
        char startMark = ' ';
        char endMark   = ' ';
        
        // If the current token isn't the first thing in the buffer, see what
        // comes immediately before it.
        //
        if( wordStart != 0 )
            startMark = srcText.charAt( wordStart-1 );
        
        // If the current token isn't the last thing in the buffer, see what
        // comes immediately after it.
        //
        if( wordEnd != srcText.length() ) 
            endMark = srcText.charAt( wordEnd );
        
        // If the current token isn't flanked on both sides by special token
        // markers...
        //    
        if( startMark != Constants.BUMP_MARKER || endMark != Constants.BUMP_MARKER )
        {
          
            // Set the word increment (bump value) for the non-special
            // token encountered to the default word bump.
            //
            theToken.setPositionIncrement( bumpValue );
            
            // Return the resulting bumped token to the caller.
            return theToken;
        
        } // if( startMark != marker || endMark != marker )
        
        // The current token is a special bump value. So accumulate this
        // bump value for the position increment of the next non-special
        // token.
        //
        bumpValue += Integer.valueOf(theToken.termText()).intValue();
    
    } // for(;;)
    
    // Should never get here.
    // return theToken;
    
  } // public next()

} // class XtfSpecialTokensFilter
