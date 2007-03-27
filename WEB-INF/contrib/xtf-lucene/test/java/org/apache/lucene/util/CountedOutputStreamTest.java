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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import junit.framework.TestCase;

/**
 * Test the {@link CountedOutputStream} class.
 * 
 * @author Martin Haye
 */
public class CountedOutputStreamTest extends TestCase
{
  public void testCounting() throws IOException
  {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    CountedOutputStream cos = new CountedOutputStream(bos);
    
    assertEquals(cos.nWritten(), 0);
    
    cos.write(0);
    assertEquals(cos.nWritten(), 1);
    
    cos.write(new byte[] {1,2,3});
    assertEquals(cos.nWritten(), 4);
    
    cos.close();
  }
}
