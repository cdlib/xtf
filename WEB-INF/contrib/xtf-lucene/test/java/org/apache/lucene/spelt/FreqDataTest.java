package org.apache.lucene.spelt;

/*
 * Copyright 2007 The Apache Software Foundation.
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

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

/** 
 * Test the {@link FreqData} class 
 *
 * @author Martin Haye
 */
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
