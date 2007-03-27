package org.apache.lucene.util;

/*
 * Copyright 2006-2007 The Apache Software Foundation.
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

/**
 * Convenient class for reporting progress on a long, possibly complex
 * multi-phase, process.
 * 
 * @author Martin Haye
 */
public abstract class ProgressTracker implements Cloneable 
{
  private float loPct;
  private float hiPct;
  private int minInterval = 30 * 1000; // 30 seconds
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
  public ProgressTracker[] split(long[] works) 
  {
    long total = 0;
    for (int i = 0; i < works.length; i++)
      total += works[i];
    ProgressTracker[] out = new ProgressTracker[works.length];
    long accum = 0;
    for (int i = 0; i < works.length; i++) {
      out[i] = (ProgressTracker)clone();
      out[i].loPct = loPct + (accum * (hiPct - loPct) / total);
      accum += works[i];
      out[i].hiPct = loPct + (accum * (hiPct - loPct) / total);
    }
    return out;
  }

  /** Clone this tracker */
  public Object clone() 
  {
    try {
      return super.clone();
    }
    catch (CloneNotSupportedException e) {
      return null;
    }
  }

  /** To be called periodically by code that does work. */
  public void progress(long workDone, long totalWork, String descrip)
  {
    progress(workDone, totalWork, descrip, false);
  }
  
  /** To be called periodically by code that does work. */
  public void progress(long workDone, long totalWork, String descrip, 
                       boolean force) 
  {
    // Calculate the percent done
    int pctDone = (int)(loPct + (workDone * (hiPct - loPct) / totalWork));
    
    // If we no percentage change, skip this unless forced.
    if (!force && pctDone <= prevPctDone.value)
      return;
    
    // Percent has changed. However, if it's not time to print a message yet,
    // consider this boring and skip it (unless forced)
    //
    if (!force && System.currentTimeMillis() - prevTime.value < minInterval)
    {
      return;
    }
    
    // Okay, time to actually report what's going on.
    report(pctDone, descrip.equals(prevDescrip.value) ? "" : descrip);
    
    // Record the current values for comparison next time
    prevTime.value = System.currentTimeMillis();
    prevPctDone.value = pctDone;
    prevDescrip.value = descrip;
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
