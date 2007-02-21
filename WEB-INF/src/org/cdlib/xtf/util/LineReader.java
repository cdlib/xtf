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
import java.io.RandomAccessFile;

/**
 * Provides a line-based interface for reading a file. Has the unusual ability
 * to read forward <i>or backward</i>.
 *
 * @author Martin Haye
 */
@SuppressWarnings("cast")
public class LineReader {
  /** Size of our internal buffer */
  private static final int blockSize = 1024;

  /** Internal buffer of file data */
  private byte[] block = new byte[blockSize];

  /** Overall length of the file we're reading */
  private int length;

  /** Current position within the file */
  private int pos = -1;

  /** Starting position of the block buffer within the file */
  private int blockStart = -1;

  /** Ending position of the block buffer within the file */
  private int blockEnd;

  /** File position of the start of the last line read */
  private int linePos;

  /** Actual disk file we're reading */
  private RandomAccessFile file;

  /** Used to accumulate lines */
  private StringBuffer buf = new StringBuffer(500);

  /**
   * Default constructor
   *
   * @param filePath    Path of the file to read
   */
  public LineReader(String filePath)
    throws IOException 
  {
    file = new RandomAccessFile(filePath, "r");
    length = (int)file.length();
    seek(0);
  } // constructor

  /**
   * Read a block of data starting a the given position.
   */
  private void readBlock(int startPos)
    throws IOException 
  {
    file.seek(startPos);
    int nRead = file.read(block);
    blockStart = startPos;
    blockEnd = Math.min(length, startPos + nRead);
  } // readBlock()

  /**
   * Tells how long the file is, in bytes.
   */
  public final int length()
    throws IOException 
  {
    return length;
  }

  /**
   * Reposition the file pointer at the beginning of the line containing the
   * specified byte position.
   */
  public void seek(int toPos)
    throws IOException 
  {
    pos = toPos;
    int newStart = pos - (pos % blockSize);
    if (newStart != blockStart)
      readBlock(newStart);

    // Adjust so we're on an even line boundary.
    char c = prevChar();
    if (c != '\n' && c != '\r')
      prevLine();
    else
      nextChar();
  } // seek()

  /**
   * Get the next character in the input file, and increment the position.
   */
  private char nextChar()
    throws IOException 
  {
    // If at the end of this block, read another one.
    if (pos == blockEnd)
      readBlock(blockEnd);
    assert pos >= blockStart && pos < blockEnd;

    // Get the character
    return (char)(((int)block[(pos++) - blockStart]) & 0xff);
  } // nextChar()

  /**
   * Get the previous character in the input file, and decrement the position.
   */
  private char prevChar()
    throws IOException 
  {
    if (pos == 0)
      return 0;

    // If at the start of this block, read the prior one.
    if (pos == blockStart) {
      readBlock(blockStart - blockSize);
      pos = blockEnd;
    }
    assert pos > blockStart && pos <= blockEnd;

    // Get the character
    return (char)(((int)block[(--pos) - blockStart]) & 0xff);
  } // prevChar()

  /**
   * Retrieves the next line of text from the file.
   *
   * @return    The text line, or null if the end of the file has been reached.
   */
  public String nextLine()
    throws IOException 
  {
    // Clear old data from the accumulation buffer
    buf.setLength(0);

    // Record the starting position of the line.
    linePos = pos;

    // Go until we hit a newline.
    char c = 0;
    while (pos < length) {
      c = nextChar();
      if (c == '\n' || c == '\r')
        break;
      buf.append(c);
    } // while( pos < length )

    // If newline/cr return pair is found, eat it.
    if (pos < length) {
      char c2 = nextChar();
      if ((c == '\n' && c2 != '\r') || (c == '\r' && c2 != '\n'))
        prevChar();
    }

    // All done!
    if (buf.length() == 0 && pos == length)
      return null;
    return buf.toString();
  } // nextLine()

  /**
   * Retrieves the previous line of text from the file.
   *
   * @return    The text line, or null if the start of the file has been reached.
   */
  public String prevLine()
    throws IOException 
  {
    // Clear the old data from the accumulation buffer
    buf.setLength(0);

    // If we're in mid-line, back up til we hit a newline.
    char c = 0;
    while (pos > 0) {
      c = prevChar();
      if (c == '\n' || c == '\r')
        break;
    }

    // Handle newline/cr if present
    if (pos > 0) {
      char c2 = prevChar();
      if ((c == '\n' && c2 != '\r') || (c == '\r' && c2 != '\n'))
        nextChar();
    }

    // At start of file? Go no further.
    if (pos == 0)
      return null;

    // Now accumulate chars until we hit another newline.
    while (pos > 0) {
      c = prevChar();
      if (c == '\n' || c == '\r')
        break;
      buf.append(c);
    } // while( pos > 0 )

    // Leave the newline where it was.
    if (c == '\n' || c == '\r')
      nextChar();

    // Record the starting position of the line.
    linePos = pos;

    // Since we read the characters in backwards, invert the order now.
    final int len = buf.length();
    for (int i = 0; i < len / 2; i++) {
      c = buf.charAt(i);
      buf.setCharAt(i, buf.charAt(len - i - 1));
      buf.setCharAt(len - i - 1, c);
    }

    // And we're done.
    return buf.toString();
  } // prevLine()

  /**
   * Retrieves the file position of the last line fetched by nextLine() or
   * prevLine().
   */
  public int linePos() {
    return linePos;
  }
} // class LineReader
