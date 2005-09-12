package org.apache.lucene.mark;

/**
 * Copyright 2004 The Apache Software Foundation
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
 * Interface for iterating over the contents of a field or document on a
 * word-oriented basis. Instances of this class are used for context and
 * hit marking operations by {@link ContextMarker}. Optionally, derived 
 * classes can support semi-rigid section boundaries within the text; the
 * context for a given hit will never cross one of these boundaries.
 *
 * <p>Created: Dec 17, 2004</p>
 *
 * @author  Martin Haye
 * @version $Id: WordIter.java,v 1.1 2005-09-12 19:06:11 mhaye Exp $
 */
public interface WordIter extends Cloneable
{
  /** Make an exact, independent, copy of this iterator */
  Object clone();
  
  /** Advance to the next word. If 'force' is set, ignore any section
   *  boundary between this word and the next.
   * 
   * @param force   true to ignore section boundaries
   * @return        true if there was another word to advance to, false if
   *                we've reached then end (or, if 'force' is false, a 
   *                section boundary).
   */
  boolean next(boolean force);
  
  /** Back up to the previous word. If 'force' is set, ignore any section
   *  boundary between this word and the previous.
   * 
   * @param force   true to ignore section boundaries
   * @return        true if there was room to back up, false if we've reached
   *                the start (or, if 'force' is false, a section boundary).
   */
  boolean prev(boolean force);
  
  /** Reposition the iterator at the first word whose position is
   *  greater than or equal to 'wordPos'.
   * 
   * @param wordPos   Position to seek to
   * @param force     true to ignore section boundaries
   */
  void seekFirst(int wordPos, boolean force);

  /** Reposition the iterator at the last word whose position is
   *  less than or equal to 'wordPos'.
   * 
   * @param wordPos   Position to seek to
   * @param force     true to ignore section boundaries
   */
  void seekLast(int wordPos, boolean force);
  
  /** Retrieve the text of the term at the current position */
  String  term();
  
  /** Retrieve the start or end of the current position.
   * 
   * @param startOrEnd  FIELD_START for the very start of the field;
   *                    TERM_START for the first character of the word;
   *                    TERM_END for the last character of the word;
   *                    TERM_END_PLUS for the last character plus any trailing
   *                    punctuation and/or spaces;
   *                    FIELD_END for the very last end of the field.
   */
  MarkPos getPos(int startOrEnd);

  /** Replace the position within a MarkPos created by {@link #getPos(int)}
   *  using the iterator's current position. 
   * 
   * @param startOrEnd  FIELD_START for the very start of the field;
   *                    TERM_START for the first character of the word;
   *                    TERM_END for the last character of the word;
   *                    TERM_END_PLUS for the last character plus any trailing
   *                    punctuation and/or spaces;
   *                    FIELD_END for the very last end of the field.
   */
  void getPos(MarkPos pos, int startOrEnd);
  
  /** See {@link #getPos(int)} or {@link #getPos(MarkPos,int)} */
  static final int FIELD_START   = 0;

  /** See {@link #getPos(int)} or {@link #getPos(MarkPos,int)} */
  static final int TERM_START    = 1;

  /** See {@link #getPos(int)} or {@link #getPos(MarkPos,int)} */
  static final int TERM_END      = 2;

  /** See {@link #getPos(int)} or {@link #getPos(MarkPos,int)} */
  static final int TERM_END_PLUS = 3;

  /** See {@link #getPos(int)} or {@link #getPos(MarkPos,int)} */
  static final int FIELD_END     = 4;
}
