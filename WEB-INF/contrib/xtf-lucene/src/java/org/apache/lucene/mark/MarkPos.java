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
 * Represents an abstract position within the text of a document. Provides
 * methods to get the integer word position, and to count/extract text
 * between this position and another position. Derived classes store
 * additional information specific to certain types of documents.
 *
 * <p>Created: Dec 14, 2004</p>
 *
 * @author  Martin Haye
 */
public abstract class MarkPos implements Cloneable 
{
  /** Make an exact copy of this object */
  public Object clone() 
  {
    try {
      return super.clone();
    }
    catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  /** Retrieves the absolute position (in number of words) from the start
   *  of the document or field. Not the same as the count of tokens, since
   *  not all tokens have getPositionIncrement() equal to 1.
   */
  public abstract int wordPos();

  /** Counts the number of characters of text starting at this position
   *  and ending at another position. It is an error if they are out of
   *  order.
   */
  public abstract int countTextTo(MarkPos other);

  /** Retrieves all the text starting at this position and ending at another
   *  position. It is an error if they are out of order.
   */
  public abstract String getTextTo(MarkPos other);
}
