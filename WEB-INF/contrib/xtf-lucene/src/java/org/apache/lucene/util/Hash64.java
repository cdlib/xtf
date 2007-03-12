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

/**
 * Utility class that calculates good 64-bit hash codes for strings.
 * 
 * @author Martin Haye
 */
public class Hash64 
{
  private static final long MAGIC_PRIME = 55354217;

  /** Calculate a non-negative 64-bit hash code for a string */
  public static long hash(String s) {
    long h = 0L;
    for (int i = 0; i < s.length(); i++)
      h = MAGIC_PRIME * h + s.charAt(i);
    return h & 0x7fffffffffffffffL;
  }

  /** Calculate a non-negative 64-bit hash code for two strings */
  public static long hash(String s1, String s2) {
    long h = 0;
    for (int i = 0; i < s1.length(); i++)
      h = MAGIC_PRIME * h + s1.charAt(i);
    h = MAGIC_PRIME * h + '|';
    for (int i = 0; i < s2.length(); i++)
      h = MAGIC_PRIME * h + s2.charAt(i);
    return h & 0x7fffffffffffffffL;
  }

  /** Calculate a non-negative 64-bit hash code for three strings */
  public static long hash(String s1, String s2, String s3) {
    long h = 0;
    for (int i = 0; i < s1.length(); i++)
      h = MAGIC_PRIME * h + s1.charAt(i);
    h = MAGIC_PRIME * h + '|';
    for (int i = 0; i < s2.length(); i++)
      h = MAGIC_PRIME * h + s2.charAt(i);
    h = MAGIC_PRIME * h + '|';
    for (int i = 0; i < s3.length(); i++)
      h = MAGIC_PRIME * h + s3.charAt(i);
    return h & 0x7fffffffffffffffL;
  }
} // class Hash64
