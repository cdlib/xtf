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

/**
 * Stores a position within a single document field. Used by
 * {@link BasicWordIter} to provide basic context marking over normal fields.
 *
 * <p>Created: Jan 15, 2005</p>
 *
 * @author  Martin Haye
 * @version $Id: BasicMarkPos.java,v 1.1 2005-02-08 23:19:35 mhaye Exp $
 */
public class BasicMarkPos extends MarkPos
{
  /** Absolute position, in number of words, from start of field */
  protected int    wordPos;
  
  /** Absolute position, in number of characters, from start of field */
  protected int    charPos;
  
  /** The full text of the field */
  protected String fullText;
  
  /** Retrieves the absolute position, in number of words, from the start
   *  of the field.
   */
  public int wordPos() { return wordPos; }
  
  /** Counts the number of characters of text starting at this position
   *  and ending at another position. It is an error if they are out of
   *  order.
   */
  public int countTextTo(MarkPos other) {
    return ((BasicMarkPos)other).charPos - charPos;
  }

  /** Retrieves all the text starting at this position and ending at another
   *  position. It is an error if they are out of order.
   */
  public String getTextTo(MarkPos other) {
    if (other != null) {
      assert charPos <= ((BasicMarkPos)other).charPos;
      return fullText.substring(charPos, ((BasicMarkPos)other).charPos);
    }
    else
      return fullText.substring(charPos);
  }
}
