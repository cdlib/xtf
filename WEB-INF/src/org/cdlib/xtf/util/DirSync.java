package org.cdlib.xtf.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.cdlib.xtf.util.ProcessRunner.CommandFailedException;

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

/**
 * Routines to synchronize one directory hierarchy to match another.
 *
 * @author Martin Haye
 */
public class DirSync 
{
  private StringBuffer linkCmds;
  private ArrayList<File> fixDirs = new ArrayList();
  private ArrayList<Long> fixTimes = new ArrayList();
  private SubDirFilter filter;

  /**
   * Initialize a directory syncer with no sub-directory filter
   * (all sub-directories will be scanned.)
   */
  public DirSync() { 
    this(null);
  }
  
  /**
   * Initialize with a sub-directory filter.
   */
  public DirSync(SubDirFilter filter) {
    this.filter = filter;
  }
  
  
  /**
   * Sync the files from source to dest.
   * 
   * @param srcDir      Directory to match
   * @param dstDir      Directory to modify
   * @throws IOException If anything goes wrong
   */
  public void syncDirs(File srcDir, File dstDir) 
    throws IOException
  {
    linkCmds = new StringBuffer();
    syncDirs(srcDir, dstDir, 0);
    flushLinks();
  }
  
  /**
   * The main workhorse of the scanner.
   * 
   * @param srcDir      Directory to match
   * @param dstDir      Directory to modify
   * @param level       0,1,2... during recursive scan
   * @throws IOException If anything goes wrong
   */
  private void syncDirs(File srcDir, File dstDir, int level) throws IOException
  {
    String[] srcList = readSortedDir(srcDir);
    String[] dstList = readSortedDir(dstDir);
    int srcEnd = srcList.length;
    int dstEnd = dstList.length;
    int s = 0;
    int d = 0;
    while (s < srcEnd || d < dstEnd)
    {
      File srcFile = null;
      File dstFile = null;
      long direction; // if < 0 copy-src; if == 0 skip; if > 0 delete-dst
      
      // Decide what to do. First, if we still have source files...
      if (s < srcEnd) 
      {
        srcFile = new File(srcDir, srcList[s]);
        
        // And we still have dest files...
        if (d < dstEnd) 
        {
          dstFile = new File(dstDir, dstList[d]);
          
          // Compare the name. If no match, do the lesser first
          direction = srcList[s].compareTo(dstList[d]);
          
          // If same name...
          if (direction == 0)
          {
            // For directories, keep scanning down. 
            if (srcFile.isDirectory() && dstFile.isDirectory())
              direction = 0;
            else 
            {
              // For files, delete dest if older than src; skip if same mod time.
              direction = srcFile.lastModified() - dstFile.lastModified();
              
              // Special case: skip if source older than dest.
              if (direction < 0)
                direction = 0;
            }
          }
        }
        // Ran out of dest files. Copy src.
        else
          direction = -1;
      }
      // Ran out of src files. Copy dst.
      else {
        dstFile = new File(dstDir, dstList[d]);
        direction = 1;
      }

      // Now act on the decision made above.
      if (direction < 0) {
        File newFile = new File(dstDir, srcList[s]);
        if (srcFile.isDirectory())
          copyDir(srcFile, newFile, level+1);
        else
          copyFile(srcFile, newFile);
        s++;
      }
      else if (direction > 0) {
        if (dstFile.isDirectory())
          deleteDir(dstFile);
        else
          deleteFile(dstFile);
        d++;
      }
      else {
        if (srcFile.isDirectory())
          copyDir(srcFile, dstFile, level+1);
        else
          skipFile(srcFile, dstFile);
        s++;
        d++;
      }
    }
  }
  
  private void skipFile(File srcFile, File dstFile) {
    //System.out.format("Skip file %s == %s\n", srcFile.toString(), dstFile.toString());
  }

  private void deleteFile(File file) throws IOException {
    //System.out.format("Delete file %s\n", file.toString());
    if (!file.delete())
      throw new IOException("Sync error: cannot delete file '" + file.toString() + "'");
  }

  private void deleteDir(File dir) throws IOException {
    //System.out.format("Delete dir %s\n", dir.toString());
    if (filter == null || filter.approve(dir.toString()))
      Path.deleteDir(dir);
  }

  private void copyFile(File srcFile, File dstFile) {
    //System.out.format("Copy file %s to %s\n", srcFile.toString(), dstFile.toString());
    linkCmds.append("link('" + srcFile.toString() + "', '" + dstFile.toString() + "') or die;\n");
  }

  private void copyDir(File srcDir, File dstDir, int level) throws IOException {
    //System.out.format("Copy dir %s to %s\n", srcDir.toString(), dstDir.toString());
    if (filter == null || filter.approve(srcDir.toString())) {
      if (!dstDir.exists() && !dstDir.mkdir())
        throw new IOException("Error creating directory '" + dstDir + "'");
      syncDirs(srcDir, dstDir, level);
      if (srcDir.lastModified() != dstDir.lastModified()) {
        fixDirs.add(dstDir);
        fixTimes.add(srcDir.lastModified());
      }
    }
    //else
    //  System.out.format("Skip dir %s\n", srcDir.toString());
  }

  /**
   * Create all the hard links that we determined need to be made. Since
   * Java (as of 1.6) has no ability to create these, we call out and have
   * Perl do it for us.
   * 
   * @throws IOException    If something goes wrong.
   */
  private void flushLinks() throws IOException 
  {
    // No links? Nothing to do.
    if (linkCmds.length() == 0)
      return;
    
    // Fire up Perl and run the script to make the links.
    try {
      String[] args = new String[1];
      args[0] = "perl";
      //System.out.println("Link cmds:\n" + linkCmds.toString());
      ProcessRunner.runAndGrab(args, linkCmds.toString(), 0);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (CommandFailedException e) {
      throw new IOException(e.getMessage());
    }
    
    // Now that we've copied all the files, fix up the directory mod times.
    assert fixDirs.size() == fixTimes.size(); // Should have added in parallel
    for (int i = 0; i < fixDirs.size(); i++) {
      File dstDir = fixDirs.get(i);
      long time = fixTimes.get(i);
      if (!dstDir.setLastModified(time))
        throw new IOException("Error setting last modified date on dir '" + dstDir + "'");
    }
  }

  /**
   * Get a sorted list of the files (and subdirectories) within a directory.
   */
  private static String[] readSortedDir(File dir)
  {
    String[] list = dir.list();
    Collections.sort(Arrays.asList(list));
    String prev = null;
    for (String s : list) {
      assert prev == null || s.compareTo(prev) >= 0;
      prev = s;
    }
    return list;
  }
}
