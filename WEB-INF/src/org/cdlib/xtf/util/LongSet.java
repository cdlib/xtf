package org.cdlib.xtf.util;

import java.util.Arrays;

/**
 * Copyright (c) 2004, Regents of the University of California
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
 */

/**
 * A fast, expandible set of positive numeric values, stored as a hash. 
 * Doesn't support deletion, and isn't very good at handling sequential
 * values, so beware.
 * 
 * @author Martin Haye
 */
public class LongSet
{
  private int hashSize;
  private long[] ents;
  private int curSize;

  /**
   * Create the hash table that can comfortably hold the specified number
   * of entries. The actual table is created to be the smallest prime
   * greater than size*2.
   * 
   * @param maxSize  Max # of entries
   */
  public LongSet( int maxSize )
  {
      curSize = 0;
      this.hashSize = Prime.findAfter(maxSize*2);
      ents = new long[hashSize];
      Arrays.fill(ents, -1L);
  } // constructor
  
  /**
   * Add a value to the set, if it's not already present.
   */
  public void add(long val)
  {
    int pos = (int) (val % hashSize);
    while (true) {
        final long cur = ents[pos];
        if (cur == val)
            return;
        if (cur < 0)
            break;
        pos = (pos + 1) % hashSize;
    }
    ents[pos] = val;
    ++curSize;
    if (curSize * 2 >= hashSize)
        grow();
  }
  
  /**
   * Check if the given value is contained in the set.
   */
  public boolean contains(long val)
  {
    int pos = (int) (val % hashSize);
    while (true) {
        final long cur = ents[pos];
        if (cur == val)
            return true;
        if (cur < 0)
            break;
        pos = (pos + 1) % hashSize;
    }
    return false;
  }
  
  /**
   * Expand the table and re-hash the existing entries.
   */
  private void grow()
  {
    // Calculate a new size for the hash table.
    int newSize = Prime.findAfter(hashSize * 3 / 2);
    
    // Allocate and clear the new table
    long[] newEnts = new long[newSize];
    Arrays.fill(newEnts, -1L);
    
    // Re-hash the existing entries
    for (int i = 0; i < hashSize; i++) {
        final long val = ents[i];
        if (val < 0)
            continue;
        int pos = (int) (val % newSize);
        while (newEnts[pos] >= 0)
            pos = (pos + 1) % newSize;
       newEnts[pos] = val;
    }
    
    // Toss the old hash.
    ents = newEnts;
    hashSize = newSize;
  }
  
  /** Tells how many entries are currently in the set */
  public int size() 
  {
      return curSize;
  } // size()
      
  /**
   * Basic regression test
   */
  public static final Tester tester = new Tester("LongSet") {
    protected void testImpl() {
      LongSet hash = new LongSet( 2 );
      
      hash.add(100);
      assert hash.contains(100);
      assert !hash.contains(111);
      assert hash.size() == 1;
      
      hash.add(200);
      hash.add(211);
      assert hash.contains(100);
      assert hash.contains(200);
      assert hash.contains(211);
      assert !hash.contains(111);
      assert !hash.contains(212);
      assert hash.size() == 3;
    } // testImpl()
  };
  
} // class LongSet
