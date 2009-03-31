package org.cdlib.xtf.util;


/**
 * Copyright (c) 2009, Regents of the University of California
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

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

/**
 * This class provides an efficient means to determine if a given subdirectory
 * is "in" or "out" of the set of directories specified to index. Essentially,
 * if a given directory has an ancestor or a descendant in the set, it
 * qualifies. That is, ancestors and cousins of the set directories will be
 * indexed, but not necessarily all the cousins, nephews, nieces, etc.
 */
public class SubDirFilter 
{
  private HashSet targets   = new HashSet();
  private HashSet ancestors = new HashSet();

  /** Tell if nothing has been added yet */
  public boolean isEmpty() {
    return targets.isEmpty();
  }

  /**
   * Adds a directory to the set.
   */
  public void add(File dirFile) {
    targets.add(dirFile.toString());
    for (String a : ancestorOrSelf(dirFile)) {
      if (!ancestors.add(a))
        break;
    }
  }

  /**
   * Checks if the given directory is in the set, where "in" is defined as
   * having an ancestor or descendant within the set.
   */
  public boolean approve(String dir) {
    return approve(new File(Path.normalizePath(dir)));
  }
  
  /**
   * Checks if the given directory is in the set, where "in" is defined as
   * having an ancestor or descendant within the set.
   */
  public boolean approve(File dirFile)
  {
    // If this dir has descendants in the set, yes.
    if (ancestors.contains(dirFile.toString()))
      return true;
    
    // If this dir has ancestors in the set, yes.
    for (String a : ancestorOrSelf(dirFile)) {
      if (targets.contains(a))
        return true;
    }
    
    // Otherwise, no.
    return false;
  }

  /**
   * Make a list of the directory and all its ancestors.
   */
  private ArrayList<String> ancestorOrSelf(File dir)
  {
    ArrayList<String> list = new ArrayList();
    boolean found = false;
    for (; !found && dir != null; dir = dir.getParentFile())
      list.add(dir.toString());
    return list;
  }
  
} // class SubdirFilter
