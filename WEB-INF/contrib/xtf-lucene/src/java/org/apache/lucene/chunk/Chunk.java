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
import org.apache.lucene.analysis.Token;

/**
 * Keeps track of all the tokens in a given chunk of text, also maintaining
 * a reference back to the source of the chunk. Instances of this class are
 * typically created by and cached by a {@link ChunkSource}.
 */
public class Chunk 
{
  /** The source of this chunk */
  public ChunkSource source;

  /** Absolute number (i.e. document ID) of this chunk */
  public int chunkNum;

  /** Original text value of the chunk */
  public String text;

  /** Tokens extracted from the text */
  public Token[] tokens;

  /** Absolute word position of the first token */
  public int minWordPos;

  /** Absolute word position of the last token */
  public int maxWordPos;

  /** Construct a new chunk */
  public Chunk(ChunkSource source, int chunkNum) {
    this.source = source;
    this.chunkNum = chunkNum;
  }
}
