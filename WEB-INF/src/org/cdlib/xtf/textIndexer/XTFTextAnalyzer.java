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
import java.io.Reader;
import java.util.Set;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.*;
import org.cdlib.xtf.util.FastStringReader;
import org.cdlib.xtf.util.FastTokenizer;


////////////////////////////////////////////////////////////////////////////////

/** The <code>XTFTextAnalyzer</code> class performs the task of breaking up a
 *  contiguous chunk of text into a list of separate words (<b><i>tokens</i></b>
 *  in Lucene parlance.) The resulting list of words is what Lucene iterates
 *  through and adds to its database for an index. <br><br>
 * 
 *  Within this analyzer, there are four main phases: 
 * 
 *  <blockquote dir=ltr style="MARGIN-RIGHT: 0px">
 *    <u>Tokenizing</u><br>
 *    The first phase is the conversion of the contiguous text into a list of 
 *    separate tokens. This step is performed by the {@link FastTokenizer} 
 *    class. This class uses a set of rules to separate words in western text 
 *    from the  spacing and puntuation that normally accompanies it. The 
 *    <code>FastTokenizer</code> class uses the same basic tokenizing rules as
 *    the Lucene <code>StandardAnalyzer</code> class, but has been optimized 
 *    for speed. <br><br>
 * 
 *    <u>Special Token Filtering</u><br>
 *    In the process of creating chunks of text for indexing, the Text Indexer
 *    program inserts 
 *    {@linkplain XMLTextProcessor#insertVirtualWords(StringBuffer) virtual words}
 *    and other special tokens that help it to relate the chunks of text stored
 *    in the Lucene index back to its original XML source text. The
 *    <code>XTFTextAnalyzer</code> looks for those special tokens, removes them
 *    from the token list, and translates them into position increments for the
 *    first non-special tokens they preceed. For more about special token 
 *    filtering, see the {@link XtfSpecialTokensFilter} class. <br><br>
 * 
 *    <u>Stop-Word Filtering</u><br>
 *    The third step performed by the <code>XTFTextAnalyzer</code> is to remove
 *    certain words called <i>stop-words</i>. Stop-words are words that by 
 *    themselves are not worth indexing, such as <i>a</i>, <i>the</i>, 
 *    <i>and</i>, <i>of</i>, etc. These words appear so many times in English
 *    text, that indexing all their occurences just slows down searching for
 *    other words without providing any real value. Consequently, they are 
 *    filtered out of the token list. <br><br>
 *
 *    It should be noted, however, that while stop-words are filtered, they are
 *    not simply omitted from the database. This is because stop-words do 
 *    impart special meaning when they appear in certain phrases or titles. For 
 *    example, in <i>Man <b>of</b> War</i> the word <i>of</i> doesn't simply 
 *    act as a conjunction, but rather helps form the common name for a type of
 *    jellyfish. Similarly, the word <i>and</i> in the phrase <i>black 
 *    <b>and</b> white</i> doesn't simply join <i>black</i> and <i>white</i>, 
 *    but forms a phrase meaning a condition where no ambiguity exists. In 
 *    these cases it is important to preserve the stop-words, because ignoring 
 *    them would produce undesired matches. For example, in a search for the 
 *    words "man of war" (meaning the jellyfish), ignoring stop-words would 
 *    produce "man and war", "man in war", and "man against war" as undesired 
 *    matches. <br><br>
 * 
 *    To record stop-words in special phases without slowing searching, the 
 *    <code>XTFTextAnalyzer</code> performs an operation called 
 *    <b><i>n-gramming</i></b> for its third phase of filtering. For more 
 *    details about how n-grams actually work, see the {@link NgramStopFilter} 
 *    class. <br><br>
 * 
 *    <u>Lowercase Conversion</u><br>
 *    The fourth and final step performed by the <code>XTFTextAnalyzer</code> 
 *    is to convert all the remaining tokens in the token list to lowercase. 
 *    Converting indexed words search phrases to lowercase has the effect of  
 *    making searches case insensitive. <br><br>
 *  </blockquote>
 *  
 *  Once the <code>XTFTextAnalyzer</code> has completed its work, it returns
 *  the final list of tokens back to Lucene to be added to the index database. 
 *  <br><br>
 */
public class XTFTextAnalyzer extends Analyzer {
  
  /** The list of stop-words currently set for this filter. */
  private Set stopSet;
  
  /** The max size of chunks */
  private int chunkSize;
  
  /** A reference to the contiguous source text block to be tokenized and
   *  filtered. (Used by the {@link XTFTextAnalyzer#tokenStream(String,Reader) tokenStream()}
   *  method to read the source text for filter operations in random access
   *  fashion.)
   */
  private StringBuffer srcText;
  
  //////////////////////////////////////////////////////////////////////////////

  /** Constructor. <br><br>
   * 
   *  This method creates a <code>XTFTextAnalyzer</code> and initializes its
   *  member variables.
   * 
   *  @param  stopSet  The set of stop-words to be used when filtering text.
   *                   For more information about stop-words, see the 
   *                   {@link XTFTextAnalyzer} class description.
   * 
   *  @param  srcText  A reference to the original source text to be analyzed. 
   *                   (Saved internally for random access filter operations not 
   *                   normally possible through the serial <code>Reader</code> 
   *                   object passed to 
   *                   {@link XTFTextAnalyzer#tokenStream(String,Reader) tokenStream()}.)
   *                   <br><br>
   * 
   *  @.notes
   *    Use this method to initialize an instance of an 
   *    <code>XTFTextAnalyzer</code> and pass it to a Lucene 
   *    <code>IndexWriter</code> instance. Lucene will then call the
   *    {@link XTFTextAnalyzer#tokenStream(String,Reader) tokenStream()} 
   *    method each time a chunk of text is added to the index. <br><br> 
   */

  public XTFTextAnalyzer( Set          stopSet,
                          int          chunkSize,
                          StringBuffer srcText )
  {
    
    // Record the set of stop words to use and the combine distance
    this.stopSet         = stopSet;
    
    // Record the chunk size (used for checking)
    this.chunkSize       = chunkSize;
    
    // Store the blurbed text for later reference.
    this.srcText = srcText;
    
  } // public XTFTextAnalyzer( stopWords, blurbedText )
  

  //////////////////////////////////////////////////////////////////////////////

  /** Convert a chunk of contiguous text to a list of tokens, ready for
   *  indexing.
   * 
   *  @param fieldName  The name of the Lucene database field that the
   *                    resulting tokens will be place in. Used to decide
   *                    which filters need to be applied to the text.
   * 
   *  @param reader     A <code>Reader</code> object from which the source
   *                    text 
   * 
   *  @return
   *    A filtered <code>TokenStream</code> containing the tokens that should 
   *    be indexed by the Lucene database. <br><br>
   */
  public TokenStream tokenStream( String fieldName, Reader reader ) 

  {
    
    // Create a fast string reader.
    String text;
    if( fieldName.equals("text") )
        text = srcText.toString();
    else {
        char[] ch = new char[256];
        StringBuffer buf = new StringBuffer( 256 );
        while( true ) {
            try {
                int nRead = reader.read( ch );
                if( nRead < 0 )
                    break;
                buf.append( ch );
            }
            catch( IOException e ) { 
                // This really can't happen, given that the reader is always
                // a StringReader. But if it does, barf out.
                //
                throw new RuntimeException( e ); 
            }
        } // while
        text = buf.toString();
    }
    
    FastStringReader fastReader = new FastStringReader( text );
    
    // Convert the text into tokens.
    TokenStream result = new FastTokenizer(fastReader);
    
    // Perform the standard filtering.
    result = new StandardFilter( result );
    
    // If the current field is the text field, filter special tokens.
    if( fieldName.equals("text") )
        result = new XtfSpecialTokensFilter( result, srcText );
    
    // Normalize everything to be lowercase.
    result = new LowerCaseFilter( result );
    
    // Convert stop-words to n-grams (if any stop words were specified). We must
    // do this after XtfSpecialTokensFilter to ensure that special tokens don't
    // become part of any n-grams. Also, we must do it after the lower-case
    // filter so we can properly recognize the stop words.
    //
    if( stopSet != null ) {
        result = new NgramStopFilter( 
                              result, 
                              stopSet,
                              fieldName.equals("text") ? chunkSize : -1 );
    }

    // Return the final list of tokens to the caller.
    return result;

  } // public tokenStream()
   
} // class XTFTextAnalyzer  