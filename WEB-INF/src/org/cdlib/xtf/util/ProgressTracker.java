package org.cdlib.xtf.util;

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

/**
 * Convenient class for reporting progress on a long, possibly complex
 * multi-phase, process.
 */
public abstract class ProgressTracker implements Cloneable
{
  private float loPct;
  private float hiPct;
  private int minInterval = 30*1000; // 30 seconds
  private IntHolder prevPctDone = new IntHolder();
  private LongHolder prevTime = new LongHolder();
  private StringHolder prevDescrip = new StringHolder();
  
  /** Initialize a 0..100% tracker */
  public ProgressTracker() {
    this(0, 100);
  }
  
  /** Initialize a tracker for some other percentage range */
  public ProgressTracker(float loPct, float hiPct) {
    this.loPct = loPct;
    this.hiPct = hiPct;
    prevPctDone.value = -1;
  }
  
  /** 
   * Override the default update interval of 30 seconds
   * 
   * @param millisecs how many milliseconds between updates (minimum)
   */
  public void setMinInterval(int millisecs) {
    minInterval = millisecs;
  }
  
  /** 
   * Split this tracker into two sub-trackers, based on how much work
   * each sub-tracker needs to do.
   */
  public ProgressTracker[] split(long work1, long work2) {
    return split(new long[] { work1, work2 });
  }

  /** 
   * Split this tracker into three sub-trackers, based on how much work
   * each sub-tracker needs to do.
   */
  public ProgressTracker[] split(long work1, long work2, long work3) {
    return split(new long[] { work1, work2, work3 });
  }

  /** 
   * Split this tracker into four sub-trackers, based on how much work
   * each sub-tracker needs to do.
   */
  public ProgressTracker[] split(long work1, long work2, long work3, long work4) {
    return split(new long[] { work1, work2, work3, work4 });
  }

  /** 
   * Split this tracker into an arbitrary number of sub-trackers, based on 
   * how much work each sub-tracker needs to do. This is useful for multi-
   * phase processes.
   */
  public ProgressTracker[] split(long[] works) {
    long total = 0;
    for (int i=0; i<works.length; i++)
      total += works[i];
    ProgressTracker[] out = new ProgressTracker[works.length];
    long accum = 0;
    for (int i=0; i<works.length; i++) {
      out[i] = (ProgressTracker) clone();
      out[i].loPct = loPct + (accum * (hiPct-loPct) / total);
      accum += works[i];
      out[i].hiPct = loPct + (accum * (hiPct-loPct) / total);
    }
    return out;
  }

  /** Clone this tracker */
  public Object clone() {
    try { return (ProgressTracker) super.clone(); }
    catch (CloneNotSupportedException e ) { return null; }
  }
  
  /** To be called periodically by code that does work. */
  public void progress(long workDone, long totalWork, String descrip) 
  {
    // Calculate the percent done
    int pctDone = (int) (loPct + (workDone * (hiPct-loPct) / totalWork));
    
    // If different from the previous value
    //
    if (pctDone != prevPctDone.value || 
        (pctDone == 100 && !descrip.equals(prevDescrip.value))) 
    {
      // Observe the stated minimum interval for updates (except for the last one)
      if (pctDone < 100 && System.currentTimeMillis() - prevTime.value < minInterval)
        return;

      // Report the progress (but not the same description twice)
      report(pctDone, descrip.equals(prevDescrip.value) ? "" : descrip);
      
      // Record the current values for comparison next time
      prevTime.value = System.currentTimeMillis();
      prevPctDone.value = pctDone;
      prevDescrip.value = descrip;
    }
  }
  
  /** Supply this method to actually print out the progress */
  public abstract void report(int pctDone, String descrip);
  
  // Utility holder classes -- useful to keep one copy of the state data
  // across all the sub-clones.
  //
  private class IntHolder {
    int value;
  }
  
  private class LongHolder {
    long value;
  }
  
  private class StringHolder {
    String value;
  }
}
