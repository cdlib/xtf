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
