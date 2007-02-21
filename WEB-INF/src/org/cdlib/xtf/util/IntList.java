package org.cdlib.xtf.util;

import java.util.Arrays;
import java.util.Random;

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

// A fast, array-based, expandable list of ints
public class IntList 
{
  private int[] data;
  private int size = 0;

  public IntList() {
    this(10);
  }

  public IntList(int initialCapacity) {
    data = new int[initialCapacity];
  }

  public final void add(int value) {
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
        Arrays.fill(data, size, newSize, 0);
      size = newSize;
    }
  }

  public final int[] toArray() {
    int[] ret = new int[size];
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

  public final int get(int index) {
    return data[index];
  }

  public final int getLast() {
    return data[size - 1];
  }

  public final void set(int index, int value) {
    data[index] = value;
  }

  public final void fill(int value) {
    Arrays.fill(data, value);
  }

  public final void sort() {
    compact();
    Arrays.sort(data);
  }

  public final int binarySearch(int searchFor) {
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
    final int MAX_BITS = 32;
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
    int prev = Integer.MIN_VALUE;
    for (i = 0; i < size; i++) {
      int tmp = data[map[i]];
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
    int[] newData = new int[size];
    assert map.length == size;
    for (int i = 0; i < size; i++)
      newData[i] = data[map[i]];

    data = newData;
  }

  /**
   * Basic regression test
   */
  public static final Tester tester = new Tester("IntList") 
  {
    protected void testImpl() 
    {
      IntList list = new IntList();
      final int SIZE = 100000;
      Random rand = new Random(1);
      for (int i = 0; i < SIZE; i++)
        list.add(Math.abs(rand.nextInt()));

      int[] map = list.calcSortMap();
      list.remap(map);

      for (int i = 1; i < SIZE; i++)
        assert list.get(i - 1) <= list.get(i);
    } // testImpl()
  };
} // class IntList
