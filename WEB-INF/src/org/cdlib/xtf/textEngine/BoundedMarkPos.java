package org.cdlib.xtf.textEngine;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.mark.BasicMarkPos;
import org.apache.lucene.mark.MarkPos;

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

/**
 * Helps with marking fields that contain bump markers.
 *
 * @author Martin Haye
 */
public class BoundedMarkPos extends BasicMarkPos 
{
  private Token[] tokens;
  private int tokNum;

  /** Creates a new mark pos */
  BoundedMarkPos(Token[] tokens) {
    this.tokens = tokens;
  }

  /** Establishes the token number of this mark pos */
  final void setTokNum(int tokNum) {
    this.tokNum = tokNum;
  }

  /**
   * Ensures that no XML elements or attributes are accidentally included in
   * the text. This is because, at the moment, we don't deal with all the
   * complexities of marking across XML tags (and it is very complex.)
   */
  public String getTextTo(MarkPos other, boolean checkUnmarkable) 
  {
    if (checkUnmarkable && other != null) 
    {
      // Check all the tokens between the two marks.
      for (int i = tokNum; i <= ((BoundedMarkPos)other).tokNum; i++) 
      {
        String term = tokens[i].termText();
        if (term.length() == 0)
          continue;
        if (term.charAt(0) == Constants.ELEMENT_MARKER ||
            term.charAt(0) == Constants.ATTRIBUTE_MARKER) 
        {
          throw new UnmarkableException();
        }
      } // for i
    } // if

    // Check passed... get the text.
    return super.getTextTo(other);
  }

  /**
   * Called by BoundedWordIter when called to get the END_PLUS of a token. We
   * strip off bump markers, whitespace, and end-of-field markers.
   */
  public void stripMarkers(int termEnd) 
  {
    // Remove bump markers.
    while (true) {
      int tmp = fullText.lastIndexOf(Constants.BUMP_MARKER, charPos - 1);
      if (tmp < termEnd)
        break;
      charPos = tmp;
    }

    // Remove trailing whitespace and end-of-field markers.
    for (; charPos > termEnd; charPos--) 
    {
      char c = fullText.charAt(charPos - 1);
      if (!Character.isWhitespace(fullText.charAt(charPos - 1)) &&
          c != Constants.FIELD_END_MARKER) 
      {
        break;
      }
    } // for
  } // stripMarkers()

  /** Exception thrown if asked to mark past XML elements or attributes */
  public static class UnmarkableException extends RuntimeException {
  }
} // class BoundedMarkPos()
