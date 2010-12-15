package org.cdlib.xtf.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
 * Routines to synchronize one directory hierarchy to match another. Now uses
 * rsync for speed and simplicity, and adds a threshold above which we avoid
 * per-subdirectory syncing and just do the whole thing.
 *
 * @author Martin Haye
 */
public class DirSync 
{
  private static final int MAX_SELECTIVE_SYNC = 500;
  private static final int MAX_RSYNC_BATCH = 2;
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
    // If there are no directories specified, or there are too many,
    // just rsync the entire source to the dest.
    //
    if (filter == null || filter.size() > MAX_SELECTIVE_SYNC)
      runRsync(srcDir, dstDir, null, new String[] { "--exclude=scanDirs.list" });
    
    // Otherwise do a selective sync.
    else
      selectiveSync(srcDir, dstDir);
    
    // Always do the scanDirs.list file last, since it governs incremental syncing.
    // If it were done before other files, and the sync process aborted, we might
    // mistakenly think two directories were perfectly in sync when in fact they
    // are different.
    //
    runRsync(new File(srcDir, "scanDirs.list"), dstDir, null, null);
  }
  
  /**
   * The main workhorse of the scanner.
   * 
   * @param srcDir      Directory to match
   * @param dstDir      Directory to modify
   * @param subDirs     Sub-directories to rsync
   * @throws IOException If anything goes wrong
   */
  private void selectiveSync(File srcDir, File dstDir) 
    throws IOException
  {
    // First, sync the top-level files (no sub-dirs)
    runRsync(srcDir, dstDir, null, new String[] { "--exclude=/*/", "--exclude=scanDirs.list" });
    
    // Now sync the subdirectories in batches, not to exceed the batch limit
    if (!filter.isEmpty())
    {
      ArrayList<String> dirBatch = new ArrayList();
      String basePath = srcDir.getCanonicalPath() + "/";
      for (String target : filter.getTargets()) 
      {
        String targetPath = new File(target).getCanonicalPath();
        assert targetPath.startsWith(basePath);
        targetPath = targetPath.substring(basePath.length());
        
        dirBatch.add(targetPath);
        if (dirBatch.size() >= MAX_RSYNC_BATCH) {
          runRsync(srcDir, dstDir, dirBatch, null);
          dirBatch.clear();
        }
      }
      
      // Finish the last batch of subdirs (if any)
      if (!dirBatch.isEmpty())
        runRsync(srcDir, dstDir, dirBatch, new String[] { "--exclude=scanDirs.list" });
    }
  }
  
  /**
   * Run an rsync command with the standard arguments plus the
   * specified subdirectories and optional extra args.
   *
   * @param src          Directory (or file) to match
   * @param dst          Directory (or file) to modify
   * @param subDirs      Sub-directories to rsync (null for all)
   * @throws IOException If anything goes wrong
   */
  public void runRsync(File src, File dst, 
                       List<String> subDirs,
                       String[] extraArgs) 
    throws IOException
  {
    try 
    {
      // First the basic arguments
      ArrayList<String> args = new ArrayList(6);
      args.add("rsync");
      args.add("-av");
      //args.add("--dry-run");
      args.add("--delete");
      
      // Add any extra arguments at this point, before the paths.
      if (extraArgs != null) {
        for (String extra : extraArgs)
          args.add(extra);
      }

      // We want to hard link dest files to the source
      if (src.isDirectory())
        args.add("--link-dest=" + src.getAbsolutePath() + "/");
      
      // For the source, add in the weird "./" syntax for relative syncing, e.g.
      // rsync --relative server.org:data/13030/pairtree_root/qt/00/./{01/d5,04/k4} data/13030/pairtree_root/qt/00/
      //
      if (subDirs != null) { 
        args.add("--relative");
        for (String subDir : subDirs)
          args.add(src.getAbsolutePath() + "/./" + subDir);
      }
      else
        args.add(src.getAbsolutePath() + (src.isDirectory() ? "/" : ""));

      // Finally add the destination path
      args.add(dst.getAbsolutePath() + (dst.isDirectory() ? "/" : ""));

      // And run the command
      String[] argArray = args.toArray(new String[args.size()]);
      ProcessRunner.runAndGrab(argArray, "", 0);
    } 
    catch (InterruptedException e) {
      throw new RuntimeException(e);
    } 
    catch (CommandFailedException e) {
      throw new IOException(e.getMessage());
    }
  }
}
