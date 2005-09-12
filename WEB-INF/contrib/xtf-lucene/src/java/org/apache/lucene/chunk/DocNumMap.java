package org.apache.lucene.chunk;

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
 * Provides information on the chunk size and chunk overlap for a given
 * index, and provides a mapping from main documents to the chunks they are
 * made of.
 */
public interface DocNumMap {

  /** Get the max number of words per chunk */
  int getChunkSize();

  /** Get the number of words one chunk overlaps with the next */
  int getChunkOverlap();

  /** Count the number of main documents (not chunks) in the index. */
  int getDocCount();

  /**
   * Given a chunk number, return the corresponding document number that it
   * is part of. Note that like all Lucene indexes, this is ephemeral and
   * only applies to the given reader. If not found, returns -1.
   * 
   * @param chunkNumber Chunk number to translate
   * @return Document index, or -1 if no match.
   */
  int getDocNum(int chunkNumber);

  /** Given a document number, this method returns the number of its first
   *  chunk.
   */
  int getFirstChunk(int docNum);

  /** Given a document number, this method returns the number of its last
   *  chunk.
   */
  int getLastChunk(int docNum);
}