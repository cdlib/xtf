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

import org.apache.lucene.mark.BasicWordIter;
import org.apache.lucene.mark.MarkPos;
import org.apache.lucene.mark.WordIter;

/**
 * Iterates over words in a large document that has been broken up into
 * many overlapping {@link Chunk}s. Applies section limits at empty chunks
 * (section limits can be overcome in any method to which they apply by 
 * simply setting the 'force' parameter.)
 *
 * @author  Martin Haye
 * @version $Id: ChunkedWordIter.java,v 1.3 2005-03-02 21:07:12 mhaye Exp $
 */
public class ChunkedWordIter extends BasicWordIter implements Cloneable {
  
  /** Source for fetching chunks */
  protected ChunkSource chunkSource;

  /** Current chunk whose tokens we're currently traversing */
  protected Chunk chunk;

  /**
   * Construct the iterator to access text from the given chunk source.
   * 
   * @param chunkSource   Source to read chunks from.
   */
  public ChunkedWordIter(ChunkSource chunkSource) {
    this.chunkSource = chunkSource;
  }

  // inherit javadoc
  public boolean next(boolean force) {
    if (tokens == null)
      reseek(0);
    else if (tokNum == tokens.length - 1) {

      while( true ) 
      {
          // If we're at the very end, don't go further.
          if (!chunkSource.inMainDoc(chunk.chunkNum + 1))
            return false;
    
          // Don't skip past a section boundary unless requested to.
          Chunk next = chunkSource.loadChunk(chunk.chunkNum + 1);
          if (next.tokens.length == 0) {
              if( !force)
                  return false;
              chunk = next;
              continue;
          }
    
          // Go to the next chunk.
          reseek(next);
          return true;
      }
    }

    // Now do the normal work next() always does.
    return super.next(force);
  } 

  // inherit javadoc
  public boolean prev(boolean force) {
    if (tokens == null)
      return false;
    else if (tokNum == 0) {

      while( true ) {
          // If we're at the very beginning, don't go further.
          if (!chunkSource.inMainDoc(chunk.chunkNum - 1))
            return false;
    
          // Don't back over a section boundary unless requested to.
          Chunk prev = chunkSource.loadChunk(chunk.chunkNum - 1);
          if( prev.tokens.length == 0 ) {
              if( !force )
                  return false;
              chunk = prev;
              continue;
          }
    
          // Go to the previous chunk.
          reseek(prev);
    
          // Skip to the end of it.
          while (wordPos < chunk.maxWordPos)
            super.next(true);
    
          // All done.
          return true;
      }
    }

    // Now do the normal work prev() always does.
    return super.prev(force);
  }

  // inherit javadoc
  protected void reseek(int targetPos) {
    if (   chunk != null 
        && targetPos >= chunk.minWordPos
        && targetPos < chunk.maxWordPos)
      return;

    int targetChunk = (targetPos / chunkSource.chunkBump)
                    + chunkSource.firstChunk;
    if (targetChunk == chunkSource.lastChunk + 1)
      targetChunk = chunkSource.lastChunk;
    chunk = chunkSource.loadChunk(targetChunk);
    reseek(chunkSource.loadChunk(targetChunk));
    assert chunk.tokens.length > 0 : "reseek should never hit empty chunk";
    assert targetPos - wordPos < chunkSource.docNumMap.getChunkSize() : "Incorrect calculation";
  }

  // inherit javadoc
  protected void reseek(Chunk toChunk) {
    chunk = toChunk;
    tokens = chunk.tokens;
    tokNum = 0;
    if (chunk.tokens.length > 0)
      wordPos = chunk.minWordPos - 1 + chunk.tokens[0].getPositionIncrement();
    else
      wordPos = chunk.minWordPos;
  } // reseek()

  // inherit javadoc
  public void seekFirst(int targetPos, boolean force) {
    if (force)
      reseek(targetPos);

    super.seekFirst(targetPos, force);
  }

  // inherit javadoc
  public void seekLast(int targetPos, boolean force) {
    if (force)
      reseek(targetPos);

    super.seekLast(targetPos, force);
  }

  // inherit javadoc
  public MarkPos createPos() {
    return new ChunkMarkPos();
  }

  // inherit javadoc
  public void getPos(MarkPos pos, int startOrEnd) {
    ChunkMarkPos cm = (ChunkMarkPos) pos;

    switch (startOrEnd) {
    
      // FIELD_START and FIELD_END don't make sense for chunked access.
      case WordIter.FIELD_START:
      case WordIter.FIELD_END:
        cm.wordPos = cm.charPos = -1;
        cm.chunk = null;
        break;
        
      // First character of the current word
      case WordIter.TERM_START:
        cm.wordPos = wordPos;
        cm.charPos = tokens[tokNum].startOffset();
        cm.chunk = chunk;
        break;
        
      // Last character (plus one) of the current word
      case WordIter.TERM_END:
        cm.wordPos = wordPos;
        cm.charPos = tokens[tokNum].startOffset() + tokens[tokNum].endOffset()
            - tokens[tokNum].startOffset();
        cm.chunk = chunk;
        break;
        
      // End of word plus spaces and punctuation.
      case WordIter.TERM_END_PLUS:
        cm.wordPos = wordPos;
        if (tokNum == tokens.length - 1)
          cm.charPos = tokens[tokNum].startOffset() + chunk.text.length()
              - tokens[tokNum].startOffset();
        else
          cm.charPos = tokens[tokNum].startOffset()
              + tokens[tokNum + 1].startOffset() - tokens[tokNum].startOffset();
        cm.chunk = chunk;
        break;
        
      default:
        assert false : "Unknown start/end mode";
    }
  }
}
