package org.cdlib.xtf.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

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
 * Maintains an in-memory, one-to-one mapping from characters in one set to
 * characters in another. The list is read from a disk file, which may be
 * sorted or unsorted.
 *
 * The format of file entries should be one pair per line, separated by a bar
 * ("|") character. The first word is considered the "key", the second is the
 * "value". Each should be a four-digit hex number representing a Unicode
 * code point.
 *
 * For speed, an in-memory cache of recently mapped words is maintained.
 */
public class CharMap 
{
  /** The mapping of chars. */
  private char[] map = new char[65536];

  /** Special character to denote null list */
  private static final char NULL_CHAR = '\uEE00';

  /** Size of supplimental mapping of chars... typically there are few */
  private static final int SUPP_HASH_SIZE = 100;

  /** Supplemental mapping of characters after the first */
  private IntHash supplementalCharsMap = new IntHash(SUPP_HASH_SIZE);

  /** How many recent mappings to maintain */
  private static final int CACHE_SIZE = 5000;

  /** Keep a cache of lookups performed to-date */
  private FastCache cache = new FastCache(CACHE_SIZE);

  /** Construct a char map by reading in a file. */
  public CharMap(File f)
    throws IOException 
  {
    readFile(new BufferedReader(new FileReader(f)));
  }

  /** Construct a char map by reading from an InputStream. */
  public CharMap(InputStream s)
    throws IOException 
  {
    readFile(new BufferedReader(new InputStreamReader(s)));
  }

  /** Map the characters in a word and return the mapped resulting word,
   *  or null if no mappings found.
   */
  public synchronized String mapWord(String word) 
  {
    // Have we already looked up this word? If so, save time.
    String val = null;
    if (cache.contains(word)) {
      val = (String)cache.get(word);
      return val;
    }

    // Do a quick scan to see if there are any mappable chars. Usually
    // there are none, so this saves time.
    //
    int i;
    for (i = 0; i < word.length(); i++) {
      if (map[word.charAt(i)] != 0)
        break;
    }

    if (i == word.length()) {
      cache.put(word, null);
      return null;
    }

    // Okay, we need to map at least one character. This might result in
    // the string changing size, so we need to use a buffer.
    //
    StringBuffer buf = new StringBuffer(word.length() + 2);
    buf.append(word);

    i = 0;
    int nIterations = 0;
    while (i < buf.length()) 
    {
      char c = buf.charAt(i);

      // Check for infinite loop (can happen if char X maps to Y 
      // and Y maps back to X, or if X maps to XY)
      //
      if (++nIterations > 100000)
        throw new RuntimeException("Probable infinite loop detected in word map");

      // If no mapping, go on to the next character.
      if (map[c] == 0) {
        ++i;
        continue;
      }

      // If mapping to null, delete the character.
      if (map[c] == NULL_CHAR) {
        buf.deleteCharAt(i);
        continue;
      }

      // Replace the existing char with the new mapped char.
      buf.setCharAt(i, map[c]);

      // If there is a supplemental string to add, put it in.
      String suppChars = (String)supplementalCharsMap.get(c);
      if (suppChars != null)
        buf.insert(i + 1, suppChars);

      // Don't increment, since one of the new chars might need
      // additional mapping.
      //
      ;
    }

    // Reconstitute the new word and cache it. Then we're done.
    String newWord = buf.toString();
    cache.put(word, newWord);
    return newWord;
  } // mapWord()

  /**
   * Read in the contents of a char file. The file need not be in sorted
   * order.
   *
   * @param  reader     Reader to get the data from
   * @throws IOException
   */
  private void readFile(BufferedReader reader)
    throws IOException 
  {
    while (true) 
    {
      String line = reader.readLine();
      if (line == null)
        break;

      // Strip off any trailing comment.
      if (line.indexOf("//") >= 0)
        line = line.substring(0, line.indexOf("//"));
      if (line.indexOf("#") >= 0)
        line = line.substring(0, line.indexOf("#"));
      if (line.indexOf(";") >= 0)
        line = line.substring(0, line.indexOf(";"));

      // Break out the two fields. If no bar, skip this line.
      int barPos = line.indexOf('|');
      if (barPos < 0)
        continue;

      String key = line.substring(0, barPos).trim();
      String val = line.substring(barPos + 1).trim();

      // The key should be exactly four hex digits.
      int keyCode = -1;
      try {
        keyCode = Integer.parseInt(key, 16);
      }
      catch (NumberFormatException e) {
      }
      if (keyCode < 0 || keyCode > 65535 || key.length() != 4) {
        Trace.warning(
          "Warning: Invalid key in char mapping: " + "key '" + key +
          "' must be exactly four hex digits");
        continue;
      }

      // The value should be zero or more sets of four hex digits.
      StringTokenizer st = new StringTokenizer(val);
      StringBuffer valBuf = new StringBuffer(3);
      while (st.hasMoreTokens()) 
      {
        String tok = st.nextToken();
        int valCode = -1;
        try {
          valCode = Integer.parseInt(tok, 16);
        }
        catch (NumberFormatException e) {
        }

        if (valCode < 0 || valCode > 65535) {
          Trace.warning(
            "Warning: Invalid key/val char mapping: " + "'" + key + "' -> '" +
            val + "' (value must be series of 4-digit hex numbers)");
          continue;
        }

        valBuf.append((char)valCode);
      }

      // Record the entry.
      if (valBuf.length() == 0) 
      {
        // Record null entry using a special marker character
        map[keyCode] = NULL_CHAR;
      }
      else {
      
        // Record the first character of the mapping (most mappings
        // only have one anyway.)
        //
        map[keyCode] = valBuf.charAt(0);

        // In the unusual case of a mapping that has more than one
        // character, record the remaining chars in a quick-access 
        // hash.
        //
        if (valBuf.length() > 1)
          supplementalCharsMap.put(keyCode, valBuf.substring(1));
      }
    } // while
  } // readFile()
} // class WordMap
