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
import java.util.Random;

import org.cdlib.xtf.util.Tester;

/** 
 * A fast, array-based, expandable list of longs.
 * 
 * @author Martin Haye
 */
public class LongList 
{
  private long[] data;
  private int size = 0;

  public LongList() {
    this(10);
  }

  public LongList(int initialCapacity) {
    data = new long[initialCapacity];
  }

  public final void add(long value) {
    if (size == data.length)
      data = ArrayUtil.expand(data);
    data[size++] = value;
  }

  public final void ensureCapacity(int cap) {
    if (cap > data.length)
      data = ArrayUtil.resize(data, cap);
  }

  public final void compact() {
    if (size != data.length)
      data = ArrayUtil.resize(data, size);
  }

  public final void resize(int newSize) 
  {
    if (newSize != size) {
      data = ArrayUtil.resize(data, newSize);
      if (newSize > size)
        Arrays.fill(data, size, newSize, 0L);
      size = newSize;
    }
  }

  public final long[] toArray() {
    long[] ret = new long[size];
    System.arraycopy(data, 0, ret, 0, size);
    return ret;
  }

  public final boolean isEmpty() {
    return size == 0;
  }

  public final void clear() {
    size = 0;
  }

  public final int size() {
    return size;
  }

  public final long get(int index) {
    return data[index];
  }

  public final long getLast() {
    return data[size - 1];
  }

  public final void set(int index, long value) {
    data[index] = value;
  }

  public final void fill(long value) {
    Arrays.fill(data, value);
  }

  public final void sort() {
    compact();
    Arrays.sort(data);
  }

  public final int binarySearch(long searchFor) {
    compact();
    return Arrays.binarySearch(data, searchFor);
  }

  public final int[] calcSortMap() 
  {
    int i;

    // First, form a mapping representing the current order.
    int[] map = new int[size];
    for (i = 0; i < size; i++) {
      if (data[i] < 0)
        throw new RuntimeException(
          "This radix-sort cannot handle negative numbers");
      map[i] = i;
    }

    // Now perform a radix sort by bytes
    final int MAX_BITS = 64;
    final short[] curByte = new short[size];
    final int[] count = new int[256];
    int[] newMap = new int[size];
    for (int shift = 0; shift < MAX_BITS; shift += 8) 
    {
      // Count the size of each bucket.
      Arrays.fill(count, 0);
      for (i = 0; i < size; i++)
        ++count[curByte[i] = (short)((data[map[i]] >> shift) & 0xFF)];

      // Calculate the offset of each bucket
      int pos = 0;
      for (i = 0; i < 256; i++) {
        int tmp = pos;
        pos += count[i];
        count[i] = tmp;
      }

      // Compute the new mapping.
      for (i = 0; i < size; i++)
        newMap[count[curByte[i]]++] = map[i];

      // Flip and prepare for next pass.
      int[] tmp = map;
      map = newMap;
      newMap = tmp;
    } // for shift

    // Sorting algorithms are famous for bugs. Make a check pass to ensure that
    // we didn't mess up.
    //
    long prev = Long.MIN_VALUE;
    for (i = 0; i < size; i++) {
      long tmp = data[map[i]];
      if (tmp < prev)
        throw new RuntimeException(
          "Fatal internal error: sort algorithm has a bug");
      prev = tmp;
    }

    // Return the final mapping.
    return map;
  } // calcSortMapping()

  public final void remap(int[] map) 
  {
    long[] newData = new long[size];
    assert map.length == size;
    for (int i = 0; i < size; i++)
      newData[i] = data[map[i]];

    data = newData;
  }

  /**
   * Basic regression test
   */
  public static final Tester tester = new Tester("LongList") 
  {
    protected void testImpl() 
    {
      LongList list = new LongList();
      final int SIZE = 100000;
      Random rand = new Random(1);
      for (int i = 0; i < SIZE; i++)
        list.add(Math.abs(rand.nextLong()));

      int[] map = list.calcSortMap();
      list.remap(map);

      for (int i = 1; i < SIZE; i++)
        assert list.get(i - 1) <= list.get(i);
    } // testImpl()
  };
} // class LongList
