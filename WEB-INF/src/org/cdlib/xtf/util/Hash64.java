package org.cdlib.xtf.util;


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
 * Utility class that decodes Base64 data.
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
