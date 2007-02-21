package org.cdlib.xtf.lazyTree;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import org.cdlib.xtf.util.IntegerValues;
import org.cdlib.xtf.util.Trace;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.trace.InstructionInfo;
import net.sf.saxon.trace.TraceListener;

/**
 * Used to keep track of the current instruction being executed, and to
 * keep track of counts for each one.
 *
 * @author Martin Haye
 */
public class ProfilingListener implements TraceListener 
{
  /**
   * Stack of instructions, used to keep track of what XSLT instruction is
   * being processed. Must be thread-local, since the same stylesheet may
   * be in use by multiple threads at one time.
   */
  private ThreadLocal tlInstructionStack = new ThreadLocal();

  /** Dummy used for counting nodes when no instruction is specified */
  private static final ProfileCount emptyInstr = new ProfileCount();

  /**
   * Keeps a count of how many nodes are accessed by each instruction.
   * Must be thread-local, since the same stylesheet may be in use by
   * multiple threads at one time.
   */
  private ThreadLocal tlCountMap = new ThreadLocal();

  /** Unused */
  public void open() {
  }

  /** Unused */
  public void close() {
  }

  /** Get a thread-local instruction stack */
  private LinkedList getInstructionStack() 
  {
    LinkedList stack = (LinkedList)tlInstructionStack.get();
    if (stack == null) {
      stack = new LinkedList();
      tlInstructionStack.set(stack);
    }
    return stack;
  } // getInstructionStack()

  /** Get a thread-local count map */
  private HashMap getCountMap() 
  {
    HashMap map = (HashMap)tlCountMap.get();
    if (map == null) {
      map = new HashMap();
      tlCountMap.set(map);
    }
    return map;
  } // getCountMap()

  /**
   * Record the instruction being entered, so that subsequent counts can
   * be attributed to it.
   */
  public void enter(InstructionInfo instruction, XPathContext context) 
  {
    LinkedList instructionStack = getInstructionStack();
    /*
        for( int i = 0; i < instructionStack.size(); i++ )
            Trace.debug( "  " );
        String s = instruction.getSystemId();
        s = s.substring( s.lastIndexOf('/') + 1 );
        Trace.debug( s + ":" + instruction.getLineNumber() );
    }
    */
    instructionStack.addLast(new ProfileCount(instruction.getSystemId(),
                                              instruction.getLineNumber()));
  }

  /**
   * Called when an instruction is exited. Subsequent counts get applied to
   * the instruction that was previously active.
   */
  public void leave(InstructionInfo instruction) {
  }

  /** Unused */
  public void startCurrentItem(Item currentItem) {
  }

  /** Unused */
  public void endCurrentItem(Item currentItem) {
  }

  /**
   * Increases the count by one for the current instruction.
   */
  public void bumpCount(int nodeNum) 
  {
    LinkedList instructionStack = getInstructionStack();
    HashMap countMap = getCountMap();

    ProfileCount instr;
    if (instructionStack.isEmpty())
      instr = emptyInstr;
    else
      instr = (ProfileCount)instructionStack.getLast();

    ProfileCount pc = (ProfileCount)countMap.get(instr);
    if (pc == null) {
      pc = new ProfileCount(instr.systemId, instr.lineNum);
      countMap.put(instr, pc);
    }

    if (!pc.nodes.containsKey(IntegerValues.valueOf(nodeNum))) {
      pc.count++;
      pc.nodes.put(IntegerValues.valueOf(nodeNum), new Boolean(true));
    }
  } // bumpCount()

  /**
   * Gets a list of all the counts, sorted by ascending count. The act of
   * getting the counts clears out the table, so that a fresh profile can
   * be made of the next run.
   */
  public ProfileCount[] getCounts() 
  {
    HashMap countMap = getCountMap();

    // Make a list of all the values.
    ArrayList list = new ArrayList(countMap.values());
    Collections.sort(list,
                     new Comparator() 
      {
        public int compare(Object o1, Object o2) {
          ProfileCount p1 = (ProfileCount)o1;
          ProfileCount p2 = (ProfileCount)o2;
          if (p1.count != p2.count)
            return p1.count - p2.count;
          if (!p1.systemId.equals(p2.systemId))
            return p1.systemId.compareTo(p2.systemId);
          return p1.lineNum - p2.lineNum;
        }
      });

    // Sort the list
    ProfileCount[] array = new ProfileCount[list.size()];
    list.toArray(array);

    // Clear out the map for the next profiling run.
    countMap.clear();

    // And we're done.
    return array;
  } // getCounts()

  /**
   * Simple data structure to keep track of counts.
   */
  public static class ProfileCount 
  {
    /** ID representing the XSLT file of the instruction */
    public String systemId;

    /** Line number of the instruction within the XSLT file */
    public int lineNum;

    /** Count of how many nodes were referenced to serve this instr */
    public int count;

    /** Map of each node hit by this instruction */
    public HashMap nodes = new HashMap();

    /** Construct an empty ProfileCount */
    public ProfileCount() {
    }

    /** Construct a ProfileCount referencing a specific instruction */
    public ProfileCount(String systemId, int lineNum) {
      this.systemId = systemId;
      this.lineNum = lineNum;
    }

    /** Obtain a hash code so that ProfileCounts can be stored in a map */
    public int hashCode() {
      if (systemId != null)
        return lineNum ^ systemId.hashCode();
      return lineNum;
    }

    /** Determine if this ProfileCount is the same as another */
    public boolean equals(Object other) {
      if (!(other instanceof ProfileCount))
        return false;
      ProfileCount p = (ProfileCount)other;
      return p.systemId == systemId && p.lineNum == lineNum;
    }
  } // class ProfileCount

  /**
   * Prints the results of a trace run, to Trace.info().
   */
  public void printProfile()
    throws IOException 
  {
    // Get a sorted array of the counts.
    ProfileCount[] counts = getCounts();

    // Print it out.
    for (int i = counts.length - 1; i >= 0; i--) 
    {
      Trace.info(
        counts[i].count + " " + counts[i].systemId + ":" + counts[i].lineNum);
      /*

      // For fun, print out all the nodes too.
      Set keys = counts[i].nodes.keySet();
      List list = new ArrayList(keys);
      Collections.sort( list );
      for( Iterator iter = list.iterator(); iter.hasNext(); ) {
          int nodeNum = ((Integer)iter.next()).intValue();
          NodeImpl node = getNode( nodeNum );
          String path = Navigator.getPath( node );
          Trace.info( "    " + path );
      }
      */
    }
  } // printProfile()
} // class ProfilingListener

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
