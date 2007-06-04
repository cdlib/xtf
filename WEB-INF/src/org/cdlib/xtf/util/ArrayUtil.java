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

/**
 * Various handy functions for working with arrays.
 *
 * @author Martin Haye
 */
public class ArrayUtil 
{
  // Functions for int arrays
  public static int[] resize(int[] in, int newSize) {
    int[] out = new int[newSize];
    System.arraycopy(in, 0, out, 0, Math.min(in.length, newSize));
    return out;
  }

  public static int[] expand(int[] in) {
    return resize(in, in.length * 3 / 2);
  }

  // Functions for long arrays
  public static long[] resize(long[] in, int newSize) {
    long[] out = new long[newSize];
    System.arraycopy(in, 0, out, 0, Math.min(in.length, newSize));
    return out;
  }

  public static long[] expand(long[] in) {
    return resize(in, in.length * 3 / 2);
  }

  // Functions for float arrays
  public static float[] resize(float[] in, int newSize) {
    float[] out = new float[newSize];
    System.arraycopy(in, 0, out, 0, Math.min(in.length, newSize));
    return out;
  }

  public static float[] expand(float[] in) {
    return resize(in, Math.max(in.length + 5, in.length * 3 / 2));
  }

  // Functions for String arrays
  public static String[] resize(String[] in, int newSize) {
    String[] out = new String[newSize];
    System.arraycopy(in, 0, out, 0, Math.min(in.length, newSize));
    return out;
  }

  public static String[] expand(String[] in) {
    return resize(in, in.length * 3 / 2);
  }
} // class ArrayUtil
