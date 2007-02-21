package org.cdlib.xtf.textEngine.facet;


/*
 * Copyright (c) 2004, Regents of the University of California
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
import java.util.Vector;
import org.cdlib.xtf.util.IntegerValues;

/** Select the top level of the hierarchy that has a choice. */
public class TopChoiceSelector extends GroupSelector 
{
  private int bestLevel;
  private int bestParent;
  private Vector bestChildren = new Vector(10);
  private int[] firstChild;

  public void reset(boolean conservative) {
    super.reset(conservative);
    bestLevel = 999999999;
    bestParent = -1;
    firstChild = new int[counts.nGroups()];
  }

  public void process(int group) 
  {
    // In conservative mode, we have to select the entire tree
    if (conservative) {
      next.process(group);
      return;
    }

    // Normal (non-conservative mode)...
    int parent = counts.parent(group);

    // If we haven't seen this parent before, record the group as its first
    // child.
    //
    if (firstChild[parent] == 0) {
      firstChild[parent] = group;
      return;
    }

    // Ok, we know now that it has more than one child... If this is our 
    // current best candidate, simply add this child to its list.
    //
    if (parent == bestParent) {
      bestChildren.add(IntegerValues.valueOf(group));
      return;
    }

    // Figure out its level.
    int level = 0;
    for (int g = parent; g >= 0; g = counts.parent(g))
      level++;

    // If it's not as good as the level we've found, skip it.
    if (level >= bestLevel)
      return;

    // We have a new parent that's better than we had before. We recorded the
    // first child already; this is the second one.
    //
    bestParent = parent;
    bestLevel = level;
    bestChildren.setSize(0);
    bestChildren.add(IntegerValues.valueOf(firstChild[parent]));
    bestChildren.add(IntegerValues.valueOf(group));
  } // process()

  public void flush() 
  {
    // If we found a level with choices...
    if (bestParent >= 0) 
    {
      // Okay, process the children at the best level we found.
      for (int i = 0; i < bestChildren.size(); i++)
        next.process(((Integer)bestChildren.elementAt(i)).intValue());
    }

    // Pass the flush on.
    next.flush();
  } // flush()

  public String toString() {
    return "topChoices -> " + next.toString();
  }
}
