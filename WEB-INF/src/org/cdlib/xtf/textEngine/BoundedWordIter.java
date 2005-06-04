package org.cdlib.xtf.textEngine;

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

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.mark.BasicWordIter;
import org.apache.lucene.mark.MarkPos;
import org.apache.lucene.mark.WordIter;

/**
 * Just like a BasicWordIter, except that it enforces "soft" boundaries if
 * the source text contains XTF "bump markers" of a certain size. Basically,
 * this prevents snippets from spanning section boundaries, or the boundaries
 * between different fields of the same name.
 * 
 * @author Martin Haye
 */
class BoundedWordIter extends BasicWordIter
{
  int     boundSize;

  /**
   * Construct a bounded word iterator on the given text. The tokens from
   * the stream must refer to the same text. The skip() method works as
   * normal, but next() and prev() will enforce a soft boundary for any
   * tokens where the position offset meets or exceeds boundSize.
   */
  public BoundedWordIter( String text, TokenStream stream, int boundSize )
    throws IOException
  {
    super( text, stream );
    this.boundSize = boundSize;
  } // constructor
  
  /** 
   * Advance to the next token.
   * 
   * @return true if ok, false if no more. 
   */
  public final boolean next( boolean force )
  {
    if( force )
        return super.next( force );
    
    // Don't advance past separation in field value
    if( tokNum < tokens.length-1 && 
        tokens[tokNum+1].getPositionIncrement() >= boundSize )
    {
        return false;
    }
    
    // Don't advance past 'end-of-field' token
    int offset = tokens[tokNum].endOffset();
    if( offset < text.length() && text.charAt(offset) == Constants.FIELD_END_MARKER )
        return false;
    
    return super.next( force );
  } // next()
  
  /** 
   * Go to the previous token.
   * 
   * @return true if ok, false if no more.
   */
  public final boolean prev( boolean force )
  {
    if( force )
        return super.prev( force );
    
    // Don't back past separation in field value
    if( tokens[tokNum].getPositionIncrement() >= boundSize )
        return false;
    
    // Don't back past 'start-of-field' token
    int offset = tokens[tokNum].startOffset();
    if( offset > 0 && text.charAt(offset-1) == Constants.FIELD_START_MARKER )
        return false;
    
    return super.prev( force );
  } // prev()
  
  /** Create a new place to hold position info */
  public MarkPos getPos( int startOrEnd ) 
  { 
    BoundedMarkPos pos = new BoundedMarkPos( tokens );
    getPos( pos, startOrEnd );
    return pos;
  }

  /** 
   * Get the position of the end of the current word.
   */
  public void getPos( MarkPos pos, int startOrEnd )
  {
    super.getPos( pos, startOrEnd );
    
    switch( startOrEnd ) {
    case WordIter.FIELD_START:
        ((BoundedMarkPos)pos).setTokNum( 0 );
        break;
    case WordIter.FIELD_END:
        ((BoundedMarkPos)pos).setTokNum( tokens.length-1 );
        break;
    case WordIter.TERM_END_PLUS:
        if( startOrEnd == WordIter.TERM_END_PLUS )
            ((BoundedMarkPos)pos).stripMarkers( tokens[tokNum].endOffset() );
        // fall through...
    default:
        ((BoundedMarkPos)pos).setTokNum( tokNum );
    } // switch
  } // recordPos()
  
} // class BoundedWordIter
