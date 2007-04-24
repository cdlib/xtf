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

import junit.framework.TestCase;

/** 
 * Test the {@link TRStringDistance2} class 
 *
 * @author Martin Haye
 */
public class TRStringDistance2Test extends TestCase
{
  public void testDist()
  {
    TRStringDistance2 dist = new TRStringDistance2("hello");
    assertEquals(dist.getDistance("hello"), 0);
    assertEquals(dist.getDistance("helo"),  1);
    assertEquals(dist.getDistance("ehllo"), 1);
    assertEquals(dist.getDistance("helol"), 1);
    assertEquals(dist.getDistance("helo"),  1);
    assertEquals(dist.getDistance("helxo"), 2);
    assertEquals(dist.getDistance("hllo"),  2);
    assertEquals(dist.getDistance("hell"),  2);
    assertEquals(dist.getDistance("ello"),  2);
    assertEquals(dist.getDistance("xallo"), 4);
    assertEquals(dist.getDistance("helpq"), 4);
    assertEquals(dist.getDistance("ehlpq"), 5);
    
    TRStringDistance2 dist2 = new TRStringDistance2("a");
    assertEquals(dist2.getDistance("a"), 0);
    assertEquals(dist2.getDistance("b"), 2);
    assertEquals(dist2.getDistance("aa"), 1);
  }
}
