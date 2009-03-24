package org.cdlib.xtf.textIndexer;


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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.cdlib.xtf.util.DirSync;
import org.cdlib.xtf.util.SubDirFilter;
import org.cdlib.xtf.util.Trace;

/**
 * Takes care of copying the differences between a source index and a dest
 * index to make them exactly equal. Doesn't have to scan every data directory
 * and lazy file, since it uses the DocSelCache to get an idea of the subset
 * of things that actually need to be scanned.
 */
public class IndexSync 
{
  /**
   * Perform the minimum necessary work to ensure that the contents of dstDir
   * exactly match srcDir.
   * 
   * @throws IOException        If anything goes wrong.
   */
  public void syncDirs(File srcDir, File dstDir) throws IOException
  {
    SubDirFilter filter = calcFilter(srcDir, dstDir);
    DirSync dirSync = new DirSync(filter);
    dirSync.syncDirs(srcDir, dstDir);
  }
  
  /**
   * Determine the sub-directory filter for directory scanning. If the dst
   * is an ancestor of src, we do intelligent filtering; otherwise we scan
   * the whole thing.
   * @throws IOException If anything goes wrong.
   */
  private SubDirFilter calcFilter(File srcDir, File dstDir) 
    throws IOException 
  {
    SubDirFilter filter = new SubDirFilter();
    
    String srcTime = oldestTime(srcDir);
    String dstTime = oldestTime(dstDir);
    
    File srcCacheFile = new File(srcDir, "docSelect.cache");
    File dstCacheFile = new File(dstDir, "docSelect.cache");
    
    if (srcTime.equals(dstTime) &&
        srcCacheFile.canRead() &&
        dstCacheFile.canRead())
    {
      File srcLazyDir = null;
      File srcCloneDir = null;
      for (File f : srcDir.listFiles())
      {
        if (!f.isDirectory())
          continue;
        else if (f.getName().equals("lazy"))
          srcLazyDir = f;
        else if (f.getName().equals("dataClone"))
          srcCloneDir = f;
        else
          filter.add(f);
      }
      
      DocSelCache srcCache = new DocSelCache();
      srcCache.load(srcCacheFile);
      
      DocSelCache dstCache = new DocSelCache();
      dstCache.load(dstCacheFile);
      
      for (Map.Entry<String, DocSelCache.Entry> e : srcCache.entrySet())
      {
        String key = e.getKey();
        DocSelCache.Entry dstEntry = dstCache.get(key);
        if (dstEntry != null && dstEntry.scanTime == e.getValue().scanTime)
          continue;
        
        if (srcLazyDir != null)
          filter.add(new File(srcLazyDir, key));
        if (srcCloneDir != null)
          filter.add(new File(srcCloneDir, key));
      }
      
      for (String key : dstCache.keySet()) 
      {
        if (srcCache.containsKey(key))
          continue;
        
        if (srcLazyDir != null)
          filter.add(new File(srcLazyDir, key));
        if (srcCloneDir != null)
          filter.add(new File(srcCloneDir, key));
      }
    }
    
    if (filter.isEmpty()) {
      Trace.info("Syncing entire source directory.");
      filter.add(srcDir);
    }
    else
      Trace.info("Syncing changed directories only.");
    
    return filter;
  }

  /** 
   * Determine the oldest file within a directory (or the dir itself) and
   * return a human-readable version of that time.
   */
  public static String oldestTime(File dir)
  {
    long min = dir.lastModified();
    for (File f : dir.listFiles()) {
      if (f.lastModified() < min)
        min = f.lastModified();
    }
    return new SimpleDateFormat().format(new Date(min));
  }

  /** 
   * Determine the newest file within a directory (or the dir itself) and
   * return a human-readable version of that time.
   */
  public static String newestTime(File dir)
  {
    long max = dir.lastModified();
    for (File f : dir.listFiles()) {
      if (f.lastModified() > max)
        max = f.lastModified();
    }
    return new SimpleDateFormat().format(new Date(max));
  }
}
