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
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.index.IndexReader;

/**
 * Reads and caches chunks from an index.
 */
public class ChunkSource 
{
  /** Reader to load chunk text from */
  protected IndexReader reader;

  /** Map of document to chunk numbers */
  protected DocNumMap docNumMap;

  /** The main document number */
  protected int mainDocNum;

  /** Max number of words per chunk */
  protected int chunkSize;

  /** Numer of words one chunk overlaps with the next */
  protected int chunkOverlap;

  /** Number of words per chunk minus the overlap */
  protected int chunkBump;

  /** First chunk in the document */
  protected int firstChunk;

  /** Last chunk in the document */
  protected int lastChunk;

  /** Field to read from the chunks */
  protected String field;

  /** Analyzer to use for tokenizing the text */
  protected Analyzer analyzer;

  /** Cache of recently loaded chunks */
  protected LinkedList chunkCache = new LinkedList();

  /** Max # of chunks to cache */
  protected int chunkCacheSize = 10;

  /**
   * Construct the iterator and read in starting text from the given
   * chunk.
   *
   * @param reader where to read the chunks from
   * @param docNumMap provides a mapping from main document number to
   *                  to chunk numbers.
   * @param mainDocNum is the document ID of the main doc
   * @param field is the name of the field to read in
   * @param analyzer will be used to tokenize the stored field contents
   */
  public ChunkSource(IndexReader reader, DocNumMap docNumMap, int mainDocNum,
                     String field, Analyzer analyzer) 
  {
    this.reader = reader;
    this.docNumMap = docNumMap;
    this.mainDocNum = mainDocNum;
    this.field = field;
    this.analyzer = analyzer;

    chunkSize = docNumMap.getChunkSize();
    chunkOverlap = docNumMap.getChunkOverlap();
    chunkBump = chunkSize - chunkOverlap;
    firstChunk = docNumMap.getFirstChunk(mainDocNum);
    lastChunk = docNumMap.getLastChunk(mainDocNum);
  }

  /**
   * Create a new storage place for chunk tokens (derived classes may
   * wish to override)
   */
  protected Chunk createChunkTokens(int chunkNum) {
    return new Chunk(this, chunkNum);
  }

  /**
   * Check if the given chunk is contained within the main document for this
   * chunk source. Essentially, if the chunk number is beyond the first or
   * last chunks, or is deleted, it's not in the main doc.
   */
  public boolean inMainDoc(int chunkNum) {
    if (chunkNum < firstChunk)
      return false;
    if (chunkNum > lastChunk)
      return false;
    if (reader.isDeleted(chunkNum))
      return false;
    return true;
  }

  /**
   * Read the text for the given chunk (derived classes may
   * wish to override)
   */
  protected void loadText(int chunkNum, Chunk chunk)
    throws IOException 
  {
    chunk.text = reader.document(chunkNum).get(field);
  }

  /**
   * Read in and tokenize a chunk. Maintains a cache of recently loaded
   * chunks for speed.
   */
  public Chunk loadChunk(int chunkNum) 
  {
    Token t;

    try 
    {
      // Is the requested chunk already cached? If so, just return it.
      for (Iterator i = chunkCache.iterator(); i.hasNext();) {
        Chunk c = (Chunk)i.next();
        if (c.chunkNum == chunkNum)
          return c;
      }

      // Make a new chunk to store things in.
      Chunk chunk = createChunkTokens(chunkNum);
      chunk.minWordPos = (chunkNum - firstChunk) * chunkBump;
      chunk.maxWordPos = chunk.minWordPos - 1;

      // Load in the text of the chunk.
      loadText(chunkNum, chunk);

      // Make a token stream out of it.
      TokenStream stream = analyzer.tokenStream(field,
                                                new StringReader(chunk.text));

      // Pull out all the tokens and make them into a list. Stop at the
      // first token when overlaps with the next chunk (unless this is
      // the very last chunk.)
      //
      ArrayList tokenList = new ArrayList(10);
      int wordPos = chunk.maxWordPos;
      while ((t = stream.next()) != null) 
      {
        wordPos += t.getPositionIncrement();
        if (chunkNum < lastChunk && wordPos >= chunk.minWordPos + chunkBump) {
          chunk.text = chunk.text.substring(0, t.startOffset());
          break;
        }
        tokenList.add(t);
        chunk.maxWordPos = wordPos;
      }
      stream.close();

      // Convert the token list into a handy array.
      chunk.tokens = (Token[])tokenList.toArray(new Token[tokenList.size()]);

      // Make room in the chunk cache if necessary.
      if (chunkCache.size() == chunkCacheSize)
        chunkCache.removeFirst();

      chunkCache.add(chunk);

      // All done!
      return chunk;
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** Retrieve the max number of words per chunk */
  public int getChunkSize() {
    return chunkSize;
  }

  /** Retrieve the number of words one chunk overlaps with the next */
  public int getChunkOverlap() {
    return chunkOverlap;
  }
} // class ChunkSource
