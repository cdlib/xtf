package org.apache.lucene.mark;

/**
 * Copyright 2005 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.util.ArrayList;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;

/**
 * Utility class used to iterate either forward or backward through the
 * tokens in a single string of text.
 *
 * <p>Created: Dec 13, 2004</p>
 *
 * @author  Martin Haye
 * @version $Id: BasicWordIter.java,v 1.2 2005-02-24 05:15:06 mhaye Exp $
 */
public class BasicWordIter implements WordIter, Cloneable
{
  /** The original text to which the tokens refer */
  protected String text;

  /** Array of tokens, holding words from the current chunk */
  protected Token[] tokens;

  /** Current token this iterator is pointed at */
  protected int tokNum;

  /** Word position of the curren token */
  protected int wordPos = -1;

  /** Word position of the last token */
  protected int maxWordPos = -1;

  /**
   * Construct the iterator and read in tokens from the given stream.
   * 
   * @param text    text represented by the tokens
   * @param stream  stream of tokens from the text
   * 
   * @throws IOException      If something goes wrong reading from 'stream'
   */
  public BasicWordIter(String text, TokenStream stream) throws IOException
  {
    Token t;

    // Keep a reference to the text for future use.
    this.text = text;

    // Pull out all the tokens and make them into a list.
    ArrayList tokenList = new ArrayList(10);
    while ((t = stream.next()) != null) {
      tokenList.add(t);
      maxWordPos += t.getPositionIncrement();
    }
    stream.close();

    // Convert the list to an easier-to-use array.
    tokens = (Token[]) tokenList.toArray(new Token[tokenList.size()]);

    // Reset variables, and we're ready to go!
    tokNum = wordPos = -1;
  } // constructor

  /** 
   * Do-nothing constructor - should only be used by derived classes
   * that perform their own initialization.
   */
  protected BasicWordIter() { }

  // inherit javadoc
  public Object clone() {
    try {
      return super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  // inherit javadoc
  public boolean next(boolean force) {
    // Are we at the end?
    if (tokNum >= tokens.length - 1)
      return false;

    // Advance.
    tokNum++;
    wordPos += tokens[tokNum].getPositionIncrement();
    return true;
  }

  // inherit javadoc
  public boolean prev(boolean force) {
    // Are we at the start?
    if (tokNum <= 0)
      return false;

    // Back up one token.
    wordPos -= tokens[tokNum].getPositionIncrement();
    --tokNum;
    return true;
  } // prev()

  // inherit javadoc
  public void seekFirst(int targetPos, boolean force) {
    // Move backward if we have to.
    while (targetPos <= wordPos) {
      if (!prev(force))
        break;
    }

    // Move forward until finished. 
    while (targetPos > wordPos) {
      if (!next(force))
        break;
    }
  }

  // inherit javadoc
  public void seekLast(int targetPos, boolean force) {
    // Move forward if we have to.
    while (targetPos >= wordPos) {
      if (!next(force))
        break;
    }

    // Move backward until finished.
    while (targetPos < wordPos) {
      if (!prev(force))
        break;
    }
  }

  // inherit javadoc
  public MarkPos getPos(int startOrEnd) {
    MarkPos pos = new BasicMarkPos();
    getPos(pos, startOrEnd);
    return pos;
  }

  // inherit javadoc
  public void getPos(MarkPos pos, int startOrEnd) {
    BasicMarkPos bm = (BasicMarkPos) pos;
    bm.fullText = text;

    switch (startOrEnd) {
    
      // Start of field
      case WordIter.FIELD_START:
        bm.wordPos = 0;
        bm.charPos = 0;
        break;
        
      // First character of the current word
      case WordIter.TERM_START:
        bm.wordPos = wordPos;
        bm.charPos = tokens[tokNum].startOffset();
        break;
        
      // Last character (plus one) of the current word
      case WordIter.TERM_END:
        bm.wordPos = wordPos;
        bm.charPos = tokens[tokNum].startOffset() 
                   + tokens[tokNum].endOffset()
                   - tokens[tokNum].startOffset();
        break;
        
      // End of word plus spaces and punctuation
      case WordIter.TERM_END_PLUS:
        bm.wordPos = wordPos;
        if (tokNum < tokens.length - 1)
          bm.charPos = tokens[tokNum].startOffset()
                     + tokens[tokNum + 1].startOffset() 
                     - tokens[tokNum].startOffset();
        else
          bm.charPos = tokens[tokNum].startOffset() 
                     + tokens[tokNum].endOffset()
                     - tokens[tokNum].startOffset();
        break;
        
      // End of field.
      case WordIter.FIELD_END:
        bm.wordPos = maxWordPos;
        bm.charPos = text.length();
        break;
        
      default:
        assert false : "Unknown start/end mode";
    }
  }

  // inherit javadoc
  public final String term() {
    return tokens[tokNum].termText();
  }
}