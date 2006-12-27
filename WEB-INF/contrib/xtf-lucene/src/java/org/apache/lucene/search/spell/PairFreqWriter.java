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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.cdlib.xtf.util.IntList;
import org.cdlib.xtf.util.LongList;

/**
 * A fast, simple, in-memory data structure for holding frequency data used
 * to produce spelling suggestions.
 *
 * @author Martin Haye
 */
public class PairFreqWriter 
{
  /** List of keys */
  private LongList keys = new LongList();
  
  /** One count per key */
  private IntList  counts = new IntList();
  
  /** Tracks whether data has been sorted. If not, needs re-sort */
  private boolean sorted = true;
  
  /** Magic number stored in file when data is written */
  static final long MAGIC_NUM = ((long)'P') << (7*8) |
                                ((long)'a') << (6*8) |
                                ((long)'i') << (5*8) |
                                ((long)'r') << (4*8) |
                                ((long)'F') << (3*8) |
                                ((long)'r') << (2*8) |
                                ((long)'q') << (1*8) |
                                ((long)'1') << (0*8);
  
  /** Add a count for a given field/word pair */
  public final void add(String field, String word, int count) {
    add(calcLongHash(field, word), count);
  }
  
  /** Add a count for a given field/word/word triplet */
  public final void add(String field, String word1, String word2, int count) {
    add(calcLongHash(field, word1, word2), count);
  }
  
  /** Add a count for a given hash code and count */
  private void add(long hash, int count)
  {
    keys.add(hash);
    counts.add(count);
    sorted = false;
  }
  
  /** Get the count for a given field/word pair, or zero if not found */
  public final int get(String field, String word) {
    return get(calcLongHash(field, word));
  }
  
  /** Get the count for a given field/word/word triplet, or zero if not found */
  public final int get(String field, String word1, String word2) {
    return get(calcLongHash(field, word1, word2));
  }
  
  /** Get the count for a given hash code and count, or zero if not found */
  private int get(long hash)
  {
    // Before using binary search, ensure the data is sorted.
    sort();
    
    // See if we can locate the given hash code.
    int pos = keys.binarySearch(hash);
    if (pos < 0)
      return 0;
    
    // Got it!
    return counts.get(pos);
  }
  
  /**
   * Append sorted counts from an input stream that were saved by
   * {@link #save(DataOutputStream)}.
   * 
   * @param f             File to load from
   * @throws IOException  if anything goes wrong
   */
  public void add(File f) throws IOException
  {
    int prevSize = keys.size();
    
    // Open the file
    DataInputStream s =
      new DataInputStream(
        new BufferedInputStream(
          new FileInputStream(f)));

    try {
      // Check the magic number
      long magic = s.readLong();
      if (magic != MAGIC_NUM)
        throw new IOException("unrecognized format of frequency data");
      
      // Find out how many counts are stored, and prepare space for them.
      int numCounts = s.readInt();
      keys.ensureCapacity(keys.size() + numCounts);
      counts.ensureCapacity(keys.size() + numCounts);
  
      // Read each pair, verify ascending key order, and add it to our lists.
      long prevKey = -1;
      for (int i=0; i<numCounts; i++) {
        long key = s.readLong();
        int  count = s.readInt();
        if (key <= prevKey)
          throw new IOException("freq data was not sorted correctly on disk, or file is corrupt");
        if (count < 0)
          throw new IOException("frequency data file is corrupted");
        prevKey = key;
        add(key, count);
      }
      
      // If we weren't appending, there's no need to re-sort.
      if (prevSize == 0)
        sorted = true;
    }
    finally {
      s.close();
    }
  }
  
  /**
   * Save sorted counts to an input stream. These can later be loaded by
   * {@link #add(DataInputStream)}.
   * 
   * @param f             File to write to (existing contents are replaced)
   * @throws IOException  if anything goes wrong
   */
  public void save(File f) throws IOException
  {
    // Make sure the data is in sorted order
    sort();
    
    // Open the file
    DataOutputStream s = 
      new DataOutputStream(
        new BufferedOutputStream(
          new FileOutputStream(f)));
    
    // Write out the data
    try {
      s.writeLong(MAGIC_NUM);
      s.writeInt(keys.size());
      for (int i=0; i<keys.size(); i++) {
        s.writeLong(keys.get(i));
        s.writeInt(counts.get(i));
      }
    }
    finally {
      s.close();
    }
  }
  
  /** If not already sorted, re-sort the data */
  private void sort()
  {
    // Already sorted, or no data? Forget it.
    if (sorted || keys.size() == 0)
      return;
    
    // First step: sort both lists.
    final int[] map = keys.calcSortMap();
    keys.remap(map);
    counts.remap(map);
    
    // Now merge duplicates.
    long key   = keys.get(0);
    int  count = counts.get(0);
    int  dp    = 0;
    for (int sp = 1; sp < keys.size(); sp++) {
      final long nextKey = keys.get(sp);
      if (nextKey != key) {
        assert nextKey > key : "calcSortMap didn't work right";
        keys.set(dp, key);
        counts.set(dp, count);
        dp++;
        key = nextKey;
        count = 0;
      }
      count += counts.get(sp);
    }
    
    // Chop off any unused space caused by merging.
    keys.resize(dp);
    counts.resize(dp);
    
    // Lastly, remember that we don't have to sort again.
    sorted = true;
  }
 
  /** Calculate a non-negative 64-bit hash code for two strings */
  static long calcLongHash(String s1, String s2)
  {
    long h = 0;
    for (int i=0; i<s1.length(); i++)
      h = 31*h + s1.charAt(i);
    h = 31*h + '|';
    for (int i=0; i<s2.length(); i++)
      h = 31*h + s2.charAt(i);
    return h & 0x7fffffffffffffffL;
  }

  /** Calculate a non-negative 64-bit hash code for three strings */
  static long calcLongHash(String s1, String s2, String s3)
  {
    long h = 0;
    for (int i=0; i<s1.length(); i++)
      h = 31*h + s1.charAt(i);
    h = 31*h + '|';
    for (int i=0; i<s2.length(); i++)
      h = 31*h + s2.charAt(i);
    h = 31*h + '|';
    for (int i=0; i<s3.length(); i++)
      h = 31*h + s3.charAt(i);
    return h & 0x7fffffffffffffffL;
  }

} // class
