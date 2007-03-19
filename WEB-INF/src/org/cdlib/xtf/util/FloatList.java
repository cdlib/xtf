package org.cdlib.xtf.util;

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

import org.apache.lucene.util.ArrayUtil;

/** 
 * A fast, array-based, expandable list of floats.
 * 
 * @author Martin Haye
 */
public class FloatList 
{
  private float[] data;
  private int size = 0;

  public FloatList() {
    this(10);
  }

  public FloatList(int initialCapacity) {
    data = new float[initialCapacity];
  }

  public final void add(float value) {
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
        Arrays.fill(data, size, newSize, 0.0f);
      size = newSize;
    }
  }

  public final float[] toArray() {
    float[] ret = new float[size];
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

  public final float get(int index) {
    return data[index];
  }

  public final float getLast() {
    return data[size - 1];
  }

  public final void set(int index, float value) {
    data[index] = value;
  }

  public final void fill(float value) {
    Arrays.fill(data, value);
  }

  public final void sort() {
    compact();
    Arrays.sort(data);
  }

  public final int binarySearch(float searchFor) {
    sort();
    return Arrays.binarySearch(data, searchFor);
  }
} // class FloatList
