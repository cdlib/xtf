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

import java.util.ArrayList;

import junit.framework.TestCase;

/** Test the {@link ProgressTracker} class */
public class ProgressTrackerTest extends TestCase
{
  /** Test the functions of a single, non-split tracker */
  public void testSingle()
  {
    TestTracker tracker = new TestTracker();
    
    // Turn off interval tracking, since we can't really do this reliably in a
    // JUnit test.
    //
    tracker.setMinInterval(Integer.MAX_VALUE/2);
    
    tracker.progress(0, 100, "start");
    
    tracker.progress(10, 100, "midway 1", true);
    tracker.progress(20, 100, "midway 2");
    try { Thread.sleep(100); }
    catch (InterruptedException e) { }
    
    tracker.progress(40, 100, "midway 3");
    tracker.progress(60, 100, "midway 4");
    tracker.progress(80, 100, "midway 5", true);
    
    tracker.progress(90, 100, "ending");
    tracker.progress(100, 100, "end", true);
    
    assertEquals(tracker.list.get(0), "0% start");
    assertEquals(tracker.list.get(1), "10% midway 1");
    assertEquals(tracker.list.get(2), "80% midway 5");
    assertEquals(tracker.list.get(3), "100% end");
  }
  
  /** Very simple wrapper that simply records the last pct and message */
  private class TestTracker extends ProgressTracker
  {
    ArrayList<String> list = new ArrayList<String>();
    
    public void report(int pctDone, String descrip) {
      list.add(pctDone + "% " + descrip);
    }
  }
}
