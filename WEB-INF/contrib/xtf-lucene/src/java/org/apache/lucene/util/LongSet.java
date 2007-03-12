package org.apache.lucene.util;

/*
 * Copyright 2006-2007 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Arrays;

import org.cdlib.xtf.util.Tester;

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
  public LongSet(int maxSize) {
    curSize = 0;
    this.hashSize = Prime.findAfter(maxSize * 2);
    ents = new long[hashSize];
    Arrays.fill(ents, -1L);
  } // constructor

  /**
   * Add a value to the set, if it's not already present.
   */
  public void add(long val) 
  {
    int pos = (int)(val % hashSize);
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
    int pos = (int)(val % hashSize);
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
      int pos = (int)(val % newSize);
      while (newEnts[pos] >= 0)
        pos = (pos + 1) % newSize;
      newEnts[pos] = val;
    }

    // Toss the old hash.
    ents = newEnts;
    hashSize = newSize;
  }

  /** Tells how many entries are currently in the set */
  public int size() {
    return curSize;
  } // size()

  /**
   * Basic regression test
   */
  public static final Tester tester = new Tester("LongSet") 
  {
    protected void testImpl() 
    {
      LongSet hash = new LongSet(2);

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
