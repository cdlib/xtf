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

/** Pass incoming groups to a number of selectors. */
public class UnionSelector extends GroupSelector 
{
  GroupSelector[] selectors;

  public UnionSelector(GroupSelector[] selectors) {
    this.selectors = selectors;
  }

  /** Set the counts to be used */
  public void setCounts(GroupCounts counts) {
    super.setCounts(counts);
    for (int i = 0; i < selectors.length; i++)
      selectors[i].setCounts(counts);
  }

  /** Reset the selector */
  public void reset(boolean conservative) {
    super.reset(conservative);
    for (int i = 0; i < selectors.length; i++)
      selectors[i].reset(conservative);
  }

  /** Process the given group. */
  public void process(int group) 
  {
    // This should only be called at the top level, so that the second
    // selector can rely on the first selector's results entirely.
    //
    assert group == 0 : "UnionSelector should only be top-level";

    // Okay, do each one in turn, processing and completely flushing it before
    // moving on to the next.
    //
    for (int i = 0; i < selectors.length; i++) {
      selectors[i].process(group);
      selectors[i].flush();
    }
  }

  /** Flush any remaining queued groups */
  public void flush() 
  {
    // Already flushed in process() above.
  }

  public String toString() 
  {
    StringBuffer buf = new StringBuffer();
    buf.append("union(");
    for (int i = 0; i < selectors.length; i++) {
      buf.append(selectors[i].toString());
      if (i < selectors.length - 1)
        buf.append("|");
    }
    buf.append(")");
    return buf.toString();
  }
}
