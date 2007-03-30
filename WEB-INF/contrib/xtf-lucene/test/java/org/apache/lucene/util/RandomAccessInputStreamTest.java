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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import junit.framework.TestCase;

/**
 * Test the {@link RandomAccessInputStream} class.
 *
 * @author Martin Haye
 */
public class RandomAccessInputStreamTest extends TestCase
{
  private File testFile;  
  
  /** Create a test file */
  protected void setUp() throws Exception
  {
    testFile = File.createTempFile("RandomAccessInputStreamTest", null);
    FileOutputStream out = new FileOutputStream(testFile);
    out.write(new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9});
    out.close();
  }
  
  public void testStream() throws IOException
  {
    RandomAccessFile raf = new RandomAccessFile(testFile, "r");
    assertEquals(raf.read(), 0);
    assertEquals(raf.read(), 1);
    assertEquals(raf.getFilePointer(), 2);
    
    RandomAccessInputStream in = new RandomAccessInputStream(raf);
    assertEquals(in.getFilePointer(), 2);
    assertEquals(in.read(), 2);
    
    in.seek(6);
    assertEquals(in.read(), 6);
    
    in.seek(0);
    assertEquals(in.read(), 0);
    
    in.close();
  }

  /** Remove the test file we created */
  protected void tearDown() throws Exception
  {
    testFile.delete();
  }
}
