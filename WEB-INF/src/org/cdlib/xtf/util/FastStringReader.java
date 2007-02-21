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
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * Used to bypass the slowness of a Lucene StringReader (but only when used
 * in conjuction with a {@link FastTokenizer}).
 *
 * @author Martin Haye
 */
public class FastStringReader extends StringReader 
{
  /** The actual string to read from */
  private String str;

  /** Construct a reader for the given string */
  public FastStringReader(String s) {
    super(s);
    str = s;
  }

  /** Wrap a normal reader with a fast string reader */
  public FastStringReader(Reader reader) {
    this(readerToString(reader));
  }

  /** Read all the characters from a Reader, and return the resulting
   *  concatenated string.
   */
  public static String readerToString(Reader reader) 
  {
    char[] ch = new char[256];
    StringBuffer buf = new StringBuffer(256);
    while (true) 
    {
      try 
      {
        int nRead = reader.read(ch);
        if (nRead < 0)
          break;
        buf.append(ch, 0, nRead);
      }
      catch (IOException e) {
        // This really can't happen, given that the reader is always
        // a StringReader. But if it does, barf out.
        //
        throw new RuntimeException(e);
      }
    } // while
    return buf.toString();
  }

  /** Get the string back */
  public String getString() {
    return str;
  }
} // class FastStringReader
