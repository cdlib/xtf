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

import org.apache.lucene.mark.MarkPos;

/**
 * Tracks the position of a {@link ChunkedWordIter} as it progresses through
 * a document which has been broken into chunks.
 */
public class ChunkMarkPos extends MarkPos 
{
  /** Word position within the entire document */
  protected int wordPos;

  /** Character position within the current chunk */
  protected int charPos;

  /** Current chunk */
  protected Chunk chunk;

  // inherit javadoc
  public int wordPos() {
    return wordPos;
  }

  // inherit javadoc
  public int countTextTo(MarkPos other) {
    ChunkMarkPos cm = (ChunkMarkPos) other;

    if (other == null)
      return chunk.text.length() - charPos;

    if (chunk == cm.chunk)
      return cm.charPos - charPos;

    if (chunk.chunkNum > cm.chunk.chunkNum) {
      assert false;
      throw new RuntimeException("Cannot countTextTo() backward position");
    }

    int count = chunk.text.length() - charPos;
    for (int i = chunk.chunkNum + 1; i < cm.chunk.chunkNum; i++)
      count += chunk.source.loadChunk(i).text.length();
    count += cm.charPos;
    return count;
  }

  // inherit javadoc
  public String getTextTo(MarkPos other) {
    ChunkMarkPos cm = (ChunkMarkPos) other;

    if (other == null)
      return chunk.text.substring(charPos);

    if (chunk.chunkNum == cm.chunk.chunkNum) {
      assert charPos <= cm.charPos : "Cannot getTextTo() backward position";
      return chunk.text.substring(charPos, cm.charPos);
    }

    if (chunk.chunkNum > cm.chunk.chunkNum) {
      assert false;
      throw new RuntimeException("Cannot getTextTo() backward position");
    }
    
    if (cm.chunk.chunkNum - chunk.chunkNum > 2 ) {
      assert false : "getText spanning many chunks is probably a logic error";
    }

    StringBuffer buf = new StringBuffer();
    buf.append(chunk.text.substring(charPos));
    for (int i = chunk.chunkNum + 1; i < cm.chunk.chunkNum; i++)
      buf.append(chunk.source.loadChunk(i).text);
    buf.append(cm.chunk.text.substring(0, cm.charPos));
    return buf.toString();
  }
}