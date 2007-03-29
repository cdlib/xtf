package org.apache.lucene.spelt;

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
import java.io.IOException;

import junit.framework.TestCase;

/** Test the {@link FreqData} class */
public class FreqDataTest extends TestCase
{
  public void testFreqData() throws IOException
  {
    // Let's build up some data first.
    FreqData data = new FreqData();
    data.add("foo", 1);
    data.add("bar", 1);
    data.add("foo", 2);
    data.add("baz", 89);
    assertEquals(data.get("foo"), 3);
    assertEquals(data.get("bar"), 1);
    assertEquals(data.get("baz"), 89);
    
    // Try some multi-word data too.
    data.add("foo", "bar", 5);
    data.add("bar", "foo", 7);
    assertEquals(data.get("foo", "bar"), 5);
    assertEquals(data.get("bar", "foo"), 7);
    
    // Let's store it in a disk file and make sure we can re-load it accurately
    File file = File.createTempFile("FreqDataTest", null);
    try {
      data.save(file);
      FreqData data2 = new FreqData();
      data2.add(file);

      assertEquals(data2.get("foo"), 3);
      assertEquals(data2.get("bar"), 1);
      assertEquals(data2.get("baz"), 89);
      assertEquals(data2.get("foo", "bar"), 5);
      assertEquals(data2.get("bar", "foo"), 7);
      
      // Also, reloading the same data should double the counts.
      data.add(file);
      assertEquals(data.get("foo"), 6);
      assertEquals(data.get("bar"), 2);
      assertEquals(data.get("baz"), 89*2);
      assertEquals(data.get("foo", "bar"), 10);
      assertEquals(data.get("bar", "foo"), 14);
    }
    finally {
      file.delete();
    }
  }
}
