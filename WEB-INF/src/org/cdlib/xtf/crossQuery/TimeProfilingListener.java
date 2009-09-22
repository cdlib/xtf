package org.cdlib.xtf.crossQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import org.cdlib.xtf.util.Trace;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.trace.InstructionInfo;
import net.sf.saxon.trace.TraceListener;

/**
 * Used to keep track of the current instruction being executed, and to
 * keep track of time spent in each one.
 *
 * @author Martin Haye
 */
public class TimeProfilingListener implements TraceListener 
{
  /**
   * Stack of instructions, used to keep track of what XSLT instruction is
   * being processed. Must be thread-local, since the same stylesheet may
   * be in use by multiple threads at one time.
   */
  private ThreadLocal<LinkedList<ProfileInstr>> stack = new ThreadLocal();

  /**
   * Keeps a count of how many nodes are accessed by each instruction.
   * Must be thread-local, since the same stylesheet may be in use by
   * multiple threads at one time.
   */
  private ThreadLocal<HashMap<ProfileInstr, ProfileTime>> timeMap = new ThreadLocal();

  /** Called when the stylesheet begins execution, but before any instructions. */
  public void open() 
  {
    // Allocate stack and map for this thread if not done yet.
    if (stack.get() == null)
      stack.set(new LinkedList());
    if (timeMap.get() == null)
      timeMap.set(new HashMap());
    
    // Prepare for the first instruction.
    clearStack();
  }

  /** Called when finished processing the stylesheet. We finish up the profile. */
  public void close() 
  {
    ProfileInstr t = stack.get().get(0);
    long totalTime = System.currentTimeMillis() - t.start;
    long selfTime = totalTime - t.descendantTime;
    addTime(t, selfTime);
  }

  /** Clear the stack, and put the global entry at the top of it. */
  private void clearStack() {
    stack.get().clear();
    ProfileInstr globalInstr = new ProfileInstr("[Global variables and keys]", 0);
    globalInstr.start = System.currentTimeMillis();
    stack.get().add(globalInstr);
  }  

  /**
   * Record the instruction being entered, so that subsequent times can
   * be attributed to it.
   */
  public void enter(InstructionInfo instruction, XPathContext context) 
  {
    ProfileInstr pi = new ProfileInstr(instruction.getSystemId(),
                                       instruction.getLineNumber());
    pi.start = System.currentTimeMillis();
    stack.get().addLast(pi);
  }

  /** Add time to a given instruction */
  private void addTime(ProfileInstr instr, long selfTime)
  {
    if (selfTime == 0)
      return;
    
    ProfileTime ent = timeMap.get().get(instr);
    if (ent == null) {
      ent = new ProfileTime(instr);
      timeMap.get().put(instr, ent);
    }
    ent.time += selfTime;
  }
  
  /**
   * Called when an instruction is exited. Subsequent times get applied to
   * the instruction that was previously active.
   */
  public void leave(InstructionInfo instruction)
  {
    // Figure out how much time was spent on the current instruction
    ProfileInstr curInstr = stack.get().removeLast();
    long curTime = System.currentTimeMillis() - curInstr.start;
    long selfTime = curTime - curInstr.descendantTime;
    assert selfTime >= 0; // how could it be negative?
    
    // Accumulate total time for this instruction
    addTime(curInstr, selfTime);
      
    // Note this instruction's time so it can later be subtracted
    // from the parent instruction.
    ProfileInstr parentInstr = stack.get().getLast();
    parentInstr.descendantTime += curTime;
  }

  /** Unused */
  public void startCurrentItem(Item currentItem) {
  }

  /** Unused */
  public void endCurrentItem(Item currentItem) {
  }
  
  /**
   * Gets a list of all the times, sorted by ascending time. The act of
   * getting the times clears out the table, so that a fresh profile can
   * be made of the next run.
   */
  public ProfileTime[] getTimes() 
  {
    // Make a list of all the values.
    ArrayList<ProfileTime> list = new ArrayList(timeMap.get().values());
    Collections.sort(list,
      new Comparator<ProfileTime>() 
      {
        public int compare(ProfileTime p1, ProfileTime p2) {
          if (p1.time != p2.time)
            return (int)(p1.time - p2.time);
          if (!p1.instr.systemId.equals(p2.instr.systemId))
            return p1.instr.systemId.compareTo(p2.instr.systemId);
          return p1.instr.lineNum - p2.instr.lineNum;
        }
      });

    // Sort the list
    ProfileTime[] array = new ProfileTime[list.size()];
    list.toArray(array);

    // Clear out the map in preparation for the next profiling run.
    timeMap.get().clear();

    // And we're done.
    return array;
  } // getCounts()
  
  public static class ProfileInstr
  {
    /** ID representing the XSLT file of the instruction */
    public String systemId;

    /** Line number of the instruction within the XSLT file */
    public int lineNum;
    
    /** Time instruction was started */
    public long start;
    
    /** Accumulated time in descendants */
    public long descendantTime;
    
    /** Construct a new ProfileInstr */
    public ProfileInstr(String sysid, int line) {
      this.systemId = sysid;
      this.lineNum = line;
    }

    /** Obtain a hash code so that ProfileInstrs can be used as keys in a map */
    public int hashCode() {
      if (systemId != null)
        return lineNum ^ systemId.hashCode();
      return lineNum;
    }

    /** Determine if this ProfileTime is the same as another */
    public boolean equals(Object other) {
      if (!(other instanceof ProfileInstr))
        return false;
      ProfileInstr p = (ProfileInstr)other;
      return p.systemId == systemId && p.lineNum == lineNum;
    }
  }
  
  // Simple wrapper class to keep track of time.
  public static class ProfileTime
  {
    public ProfileInstr instr;
    public long         time;

    public ProfileTime(ProfileInstr instr) {
      this.instr = instr;
    }
  }

  /**
   * Prints the results of a trace run, to Trace.info().
   */
  public void printProfile()
    throws IOException 
  {
    // Get a sorted array of the counts.
    ProfileTime[] times = getTimes();

    // Print it out, in reverse order.
    for (int i = times.length - 1; i >= 0; i--) 
    {
      String s = times[i].time + " " + times[i].instr.systemId;
      if (times[i].instr.lineNum != 0)
        s = s + ":" + times[i].instr.lineNum;
      Trace.info(s);
    }
  }
}

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/ 
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License. 
//
// The Original Code is: most of this file. 
//
// The Initial Developer of the Original Code is
// Michael Kay of International Computers Limited (michael.h.kay@ntlworld.com).
//
// Portions created by Martin Haye are Copyright (C) Regents of the University 
// of California. All Rights Reserved. 
//
// Contributor(s): Martin Haye. 
//
