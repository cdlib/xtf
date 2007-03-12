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
 * A fast, array-based, expandable list of strings.
 * 
 * @author Martin Haye
 */
public class StringList 
{
  private String[] data;
  private int size = 0;

  public StringList() {
    this(10);
  }

  public StringList(int initialCapacity) {
    data = new String[initialCapacity];
  }

  public final void add(String value) {
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
        Arrays.fill(data, size, newSize, null);
      size = newSize;
    }
  }

  public final String[] toArray() {
    String[] ret = new String[size];
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

  public final String get(int index) {
    return data[index];
  }

  public final String getLast() {
    return data[size - 1];
  }

  public final void set(int index, String value) {
    data[index] = value;
  }

  public final void fill(String value) {
    Arrays.fill(data, value);
  }

  public final void sort() {
    compact();
    Arrays.sort(data);
  }

  public final int binarySearch(String searchFor) {
    compact();
    return Arrays.binarySearch(data, searchFor);
  }
} // class StringList
