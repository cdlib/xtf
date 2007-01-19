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
 * 
 * Acknowledgements:
 * 
 * A significant amount of new and/or modified code in this module
 * was made possible by a grant from the Andrew W. Mellon Foundation,
 * as part of the Melvyl Recommender Project.
 */

import java.io.IOException;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.search.spell.SpellWriter;
import org.cdlib.xtf.textEngine.Constants;

/**
 * Adds words from the token stream to a SpellWriter.
 * 
 * @author Martin Haye
 */
public class SpellWritingFilter extends TokenFilter 
{
  /** Spelling writer to write to */
  private SpellWriter writer;
  
  /** true before the first token is returned */
  private boolean firstTime = true;
  
  /**
   * Construct a token stream to add tokens to a spelling correction
   * dictionary.
   * 
   * @param input       Input stream of tokens to process
   * @param writer      Spelling dictionary writer
   */
  public SpellWritingFilter( TokenStream input, SpellWriter writer ) 
  {
    // Initialize the super-class
    super(input);

    this.writer = writer;

  } // constructor

  /** Retrieve the next token in the stream. */ 
  public Token next() throws IOException 
  {
    // Get the next token. If we're at the end of the stream, get out.
    Token t = input.next();
    if( t == null )
        return t;
    
    // Make sure the first token for this field doesn't get paired with a
    // token from the previous field.
    //
    if (firstTime) {
        writer.queueBreak();
        firstTime = false;
    }
    
    // Skip words with start/end markers
    String word = t.termText();
    boolean skip = false;
    if( word.charAt(0) == Constants.FIELD_START_MARKER )
        skip = true;
    else if( word.charAt(word.length()-1) == Constants.FIELD_END_MARKER )
        skip = true;
    
    // Skip words with digits. We seldom want to correct with these,
    // and they introduce a big burden on indexing. Also, skip element
    // and attribute markers.
    //
    else
    {
        for( int i = 0; i < word.length(); i++ ) {
            char c = word.charAt(i);
            if( Character.isDigit(c) || c == Constants.ELEMENT_MARKER || c == Constants.ATTRIBUTE_MARKER ) {
                skip = true;
                break;
            }
        }
    }

    // If we're not skipping the word, queue it.
    if( !skip ) 
    {
        // Don't record pairs across sentence boundaries
        if( t.getPositionIncrement() != 1 )
            writer.queueBreak();
        
        // Queue the word
        writer.queueWord( word );
    }
    else
        writer.queueBreak();
    
    // Pass on the token unchanged.
    return t;
    
  } // next()

} // class AccentFoldingFilter
