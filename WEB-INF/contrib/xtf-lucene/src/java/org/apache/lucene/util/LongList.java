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

/** 
 * A fast, array-based, expandable list of longs.
 * 
 * @author Martin Haye
 */
public class LongList 
{
  private long[] data;
  private int size = 0;

  /** Basic constructor - initializes with capacity of 10 */
  public LongList() {
    this(10);
  }

  /** Constructor to specify initial capacity explicitly */
  public LongList(int initialCapacity) {
    data = new long[initialCapacity];
  }

  /** Add a value to the end of the list, expanding the array if necessary */
  public final void add(long value) {
    if (size == data.length)
      resize(size * 3 / 2);
    data[size++] = value;
  }

  /** Ensure that at least the given number of elements can be stored */
  public final void ensureCapacity(int cap) {
    if (cap > data.length)
      resize(cap);
  }

  /** Resize the array so it exactly fits the current elements */
  public final void compact() {
    if (size != data.length)
      resize(size);
  }

  /** 
   * Resize the array to the specified size. If smaller than the current
   * number of elements, the ones at the end will be lost.
   */
  public final void resize(int newSize) 
  {
    if (newSize != size) {
      long[] oldData = data;
      data = new long[newSize];
      System.arraycopy(oldData, 0, data, 0, Math.min(size, newSize));
      size = Math.min(size, newSize);
    }
  }

  /** 
   * Get an array of the elements. This is a copy, so it's safe to modify
   * without disturbing the contents of the list. 
   */
  public final long[] toArray() {
    long[] ret = new long[size];
    System.arraycopy(data, 0, ret, 0, size);
    return ret;
  }

  /** Check if the list is empty (i.e. size() == 0) */
  public final boolean isEmpty() {
    return size == 0;
  }

  /** Remove all elements from the list (but doesn't resize the array) */
  public final void clear() {
    size = 0;
  }

  /** Retrieve the current number of elements in the list */
  public final int size() {
    return size;
  }

  /** Get an element from the list */
  public final long get(int index) {
    return data[index];
  }

  /** Get the last element from the list */
  public final long getLast() {
    return data[size - 1];
  }

  /** Set an element in the list */
  public final void set(int index, long value) {
    data[index] = value;
  }

  /** Fill the list with a given data value */
  public final void fill(long value) {
    Arrays.fill(data, value);
  }

  /** Sort all the elements in the list in ascending order */
  public final void sort() {
    compact();
    Arrays.sort(data);
  }

  /** 
   * Perform a binary search for the given value. Note that the list must
   * already be in ascending sorted order for this to work correctly; call
   * {@link #sort()} if necessary.
   * 
   * @return    Same as for {@link Arrays#binarySearch(int[], int)}
   */
  public final int binarySearch(long searchFor) {
    compact();
    return Arrays.binarySearch(data, searchFor);
  }

  /**
   * Calculate a reordering of the elements of the list that would put them
   * in sorted order. Useful for maintaining multiple lists, and sorting them
   * by a primary key; this is done by calling this method to get the mapping,
   * then calling {@link #remap(int[])} on each array including the original.
   * 
   * @return    An array of the same size as the number of elements. Each
   *            element of the array specifies the position that corresponding
   *            element should be placed in a sorted array.
   */
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

  /** Apply a sort order to the elements -- see {@link #calcSortMap()} */
  public final void remap(int[] map) 
  {
    long[] newData = new long[size];
    assert map.length == size;
    for (int i = 0; i < size; i++)
      newData[i] = data[map[i]];

    data = newData;
  }
}
