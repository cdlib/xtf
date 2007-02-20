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
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.cdlib.xtf.textEngine.Constants;

/**
 * Ensures that the tokens at the start and end of the stream are indexed both
 * with and without the special start-of-field/end-of-field markers.
 *
 * @author Martin Haye
 */
public class StartEndFilter extends TokenFilter 
{
  /** Token queued for next() */
  private Token queuedToken = null;

  /**
   * Construct a token stream that fixes the start/end markers.
   *
   * @param input       Input stream of tokens to process
   */
  public StartEndFilter(TokenStream input) 
  {
    // Initialize the super-class
    super(input);
  } // constructor

  /** Retrieve the next token in the stream. */
  public Token next()
    throws IOException 
  {
    Token t;

    // If we have a token queued up, return that.
    if (queuedToken != null) {
      t = queuedToken;
      queuedToken = null;
      return t;
    }

    // Get the next token. If we're at the end of the stream, get out.
    t = input.next();
    if (t == null)
      return t;

    // If it starts or ends with the special token character, index both with and
    // without it.
    //
    String term = t.termText();
    boolean isStartToken = (term.charAt(0) == Constants.FIELD_START_MARKER);
    boolean isEndToken = (term.charAt(term.length() - 1) == Constants.FIELD_END_MARKER);
    if (isStartToken || isEndToken) {
      if (isStartToken)
        term = term.substring(1);
      if (isEndToken)
        term = term.substring(0, term.length() - 1);
      queuedToken = new Token(term, t.startOffset(), t.endOffset(), t.type());
      queuedToken.setPositionIncrement(0);
    }

    // Return the original token first.
    return t;
  } // next()
} // class StartEndFilter
