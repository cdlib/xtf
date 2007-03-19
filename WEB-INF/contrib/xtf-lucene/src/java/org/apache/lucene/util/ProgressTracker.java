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
  private IntHolder skippedPctDone = new IntHolder();
  private StringHolder skippedDescrip = new StringHolder();

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
    // Calculate the percent done
    int pctDone = (int)(loPct + (workDone * (hiPct - loPct) / totalWork));

    // If different from the previous value
    //
    if (pctDone != prevPctDone.value ||
        (pctDone == 100 && !descrip.equals(prevDescrip.value))) 
    {
      // Observe the stated minimum interval for updates, unless the 
      // non-numeric part of the message has changed.
      //
      boolean nonNumChg = nonNumericChange(descrip, prevDescrip.value);
      if (pctDone < 100 &&
          !nonNumChg &&
          System.currentTimeMillis() - prevTime.value < minInterval)
      {
        if (!descrip.equals(prevDescrip.value)) { 
          skippedPctDone.value = pctDone;
          skippedDescrip.value = descrip;
        }
        return;
      }
      
      // If there is a non-numeric change and we skipped one, output it now.
      if (nonNumChg && skippedDescrip.value != null)
        report(skippedPctDone.value, skippedDescrip.value);
      skippedDescrip.value = null;
      
      // Report the progress (but not the same description twice)
      report(pctDone, descrip.equals(prevDescrip.value) ? "" : descrip);

      // Record the current values for comparison next time
      prevTime.value = System.currentTimeMillis();
      prevPctDone.value = pctDone;
      prevDescrip.value = descrip;
    }
  }
  
  private boolean nonNumericChange(String s1, String s2)
  {
    if (s1 == null || s2 == null)
      return true;
    
    int p1 = 0;
    int p2 = 0;
    while (p1 < s1.length() && p2 < s2.length()) {
      char c1 = s1.charAt(p1);
      char c2 = s2.charAt(p2);
      if (Character.isDigit(c1))
        p1++;
      else if (Character.isDigit(c2))
        p2++;
      else if (c1 != c2)
        return true;
      else {
        p1++;
        p2++;
      }
    }
    if (p1 == s1.length() && p2 == s2.length())
      return false;
    return true;
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
