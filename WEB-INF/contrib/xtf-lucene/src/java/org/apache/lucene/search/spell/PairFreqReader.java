package org.apache.lucene.search.spell;

/*
 * Copyright (c) 2006, Regents of the University of California
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
 * 
 * Acknowledgements:
 * 
 * A significant amount of new and/or modified code in this module
 * was made possible by a grant from the Andrew W. Mellon Foundation,
 * as part of the Melvyl Recommender Project.
 */

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.cdlib.xtf.util.Hash64;

/**
 * Reads pair frequency data from a file created by {@link PairFreqWriter}, 
 * leaving the data on disk and accessing it via file APIs.
 *
 * @author Martin Haye
 */
public class PairFreqReader 
{
  /** File to read data from */
  private RandomAccessFile in;
  
  /** Offset of the first data pair */
  private long startOffset;
  
  /** Number of data pairs in the file */
  private int nPairs;
  
  /**
   * Opens a frequency data file.
   * 
   * @param f   The file to open
   * @throws IOException if something goes wrong
   */
  public void open(File f) throws IOException 
  {
    // Open the file and verify the magic number.
    in = new RandomAccessFile(f, "r");
    try {
      long magic = in.readLong();
      if (magic != PairFreqWriter.MAGIC_NUM)
        throw new IOException("Pair data file corrupt or not recognized");
      
      // Record the number of pairs, and the offset of the first pair.
      nPairs = in.readInt();
      startOffset = in.getFilePointer();
      
      if (startOffset + (nPairs * 12) != in.length())
        throw new IOException("Pair data file truncated or corrupt");
    }
    catch(IOException e) {
      in.close();
      throw e;
    }
  }
  
  /** Close the frequency data file */ 
  public void close() {
    if (in != null) {
      try {
        in.close();
      }
      catch (IOException e) { /*ignore*/ }
    }
    in = null;
  }
  
  /** Get the count for a given field/word pair, or 0 if not found.
   *  
   * @throws IOException if the file can't be read.
   */
  public int getCount(String field, String word) throws IOException {
    return getCount(Hash64.hash(field, word));
  }

  /** Get the count for a given field/word/word triplet, or 0 if not found.
   *  
   * @throws IOException if the file can't be read
   */
  public int getCount(String field, String word1, String word2) throws IOException {
    return getCount(Hash64.hash(field, word1, word2));
  }
  
  /** Get the count for a hash code, or 0 if not found.
   *  
   * @throws IOException if the file can't be read
   */
  private int getCount(long hash) throws IOException
  {
    // Standard binary search, but disk-based.
    int lo = 0;
    int hi = nPairs;
    while (lo <= hi) 
    {
      int mid = (lo + hi) / 2;
      in.seek(mid*12 + startOffset);
      long test = in.readLong();
      if (hash > test)
        lo = mid + 1;
      else if (hash < test)
        hi = mid - 1;
      else
        return in.readInt();
    }
    
    // Not found.
    return 0;
  }

} // class
