package org.apache.lucene.util;

/**
 * Copyright (c) 2007, Regents of the University of California
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import junit.framework.TestCase;

/** 
 * Test the {@link FileSorter} class
 * 
 * @author Martin Haye
 */
public class FileSorterTest extends TestCase
{
  public void testSort() throws IOException
  {
    // Create a file of random stuff to sort.
    File tmpIn = File.createTempFile("FileSorterTest", ".in.tmp");
    File tmpOut = File.createTempFile("FileSorterTest", ".out.tmp");
    try {
      final int NLINES = 1000;
      final int LINE_LENGTH = 50;
      String[] lines = new String[NLINES];
      BufferedWriter writer = new BufferedWriter(new FileWriter(tmpIn));
      try {
        StringBuffer buf = new StringBuffer();
        Random rand = new Random(1);
        for (int i=0; i<NLINES; i++) {
          buf.setLength(0);
          for (int j=0; j<LINE_LENGTH; j++)
            buf.append((char)(rand.nextInt(96) + 32));
          lines[i] = buf.toString();
          writer.write(lines[i]);
          writer.write('\n');
        }
      }
      finally {
        writer.close();
      }
      
      // Sort it using the file sorter
      FileSorter.sort(tmpIn, tmpOut, null, 15000);

      // For comparison, sort our array in memory
      String[] sortedLines = new String[NLINES];
      System.arraycopy(lines, 0, sortedLines, 0, NLINES);
      Arrays.sort(sortedLines);
      
      // Check that the FileSorter did it right.
      BufferedReader reader = new BufferedReader(new FileReader(tmpOut));
      for (int i=0; i<NLINES; i++) {
        String line = reader.readLine();
        assertEquals(sortedLines[i], line);
      }
    }
    finally {
      tmpIn.delete();
      tmpOut.delete();
    }
  }
}
