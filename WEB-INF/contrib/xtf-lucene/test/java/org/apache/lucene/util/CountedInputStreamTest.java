package org.apache.lucene.util;

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

import java.io.ByteArrayInputStream;
import java.io.IOException;

import junit.framework.TestCase;

/**
 * Test the {@link CountedInputStream} class.
 * 
 * @author Martin Haye
 */
public class CountedInputStreamTest extends TestCase
{
  public void testCounting() throws IOException
  {
    byte[] bytes = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
    ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
    CountedInputStream cis = new CountedInputStream(bis);
    
    assertEquals(cis.nRead(), 0);
    
    cis.read();
    assertEquals(cis.nRead(), 1);
    
    cis.skip(2);
    assertEquals(cis.nRead(), 3);
    
    byte[] buf = new byte[7];
    cis.read(buf);
    assertEquals(cis.nRead(), 10);
    
    cis.close();
  }
}
