package org.cdlib.xtf.textEngine;


/*
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
import java.util.HashMap;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.cdlib.xtf.util.Path;
import org.cdlib.xtf.util.Trace;

/**
 * Handles background warming of new (or changed) indexes, so that servlets can
 * continue serving using their existing index, and switch quickly to the new
 * one when it is ready.
 *
 * @author Martin Haye
 */
public class IndexWarmer
{
  private String xtfHome;
  private HashMap<String, Entry> entries  = new HashMap();
  private BgThread bgThread;
  private int updateInterval;
  
  /**
   * Construct the warmer and start up the background warming thread.
   */
  public IndexWarmer(String xtfHome, int updateInterval)
  {
    // Record the parameters
    this.xtfHome = xtfHome;
    this.updateInterval = updateInterval;
    
    // Fire up the background warming thread (unless disabled).
    if (updateInterval > 0) {
      bgThread = new BgThread(this);
      bgThread.setDaemon(true);
      bgThread.start();
    }
  }
  
  /**
   * Get a searcher for the given index path. If there isn't one already,
   * we create one in the foreground (we don't return til it's ready).
   */
  public synchronized XtfSearcher getSearcher(String indexPath) 
    throws IOException
  {
    indexPath = Path.resolveRelOrAbs(xtfHome, indexPath);
    Entry ent = entries.get(indexPath);

    // If this is the background warmer thread, this must be a request as part
    // of validation so use the new searcher.
    //
    if (Thread.currentThread() == bgThread) {
      String nonPendingPath = indexPath.replaceAll("-pending$", "");
      ent = entries.get(nonPendingPath);
      assert ent != null;
      return ent.newSearcher;
    }
    
    // Look up (or create if necessary) the entry for this path.
    if (ent == null) {
      ent = new Entry(Path.resolveRelOrAbs(xtfHome, indexPath));
      entries.put(indexPath, ent);
    }
    
    // If we don't have a searcher, warm this index immediately (in foreground).
    if (ent.curSearcher == null) 
    {
      if (!IndexReader.indexExists(indexPath))
        throw new IOException(String.format("Directory '%s' is missing or does not contain a valid index.", indexPath));
      
      // Read the index and ancillary files (plural/accent map, spelling, etc.)
      ent.curSearcher = new XtfSearcher(indexPath, 0); // disable update check
      
      // NOTE: We cannot validate in the foreground thread, because it messes up
      //       the thread-local variables that keep track of the current HTTP
      //       request, current servlet, etc.
      ;
      // TODO: Figure out a way to validate in the background thread.
      ;
      // TODO: Also, figure a way to rotate in a brand-new index without
      //       waiting for the background warmer to pick it up.
      ;
    }
    
    // All done.
    return ent.curSearcher;
  }

  /** 
   * Thread that sits in the background and periodically checks if there are
   * indexes in need of warming, and warms them.
   */
  private static class BgThread extends Thread
  {
    private IndexWarmer warmer;
    
    BgThread(IndexWarmer warmer) {
      this.warmer = warmer;
    }
    
    @Override
    public void run()
    {
      while (true)
      {
        // Wait a while.
        try {
          Thread.sleep(warmer.updateInterval * 1000);
        } catch (InterruptedException e) {
          return;
        }
        
        // See if any index needs to be warmed up.
        Entry toUpdate = scanForUpdates();
        if (toUpdate != null)
          warm(toUpdate);
      }
    }
    
    // For each index, check if there's a new version.
    private Entry scanForUpdates() 
    {
      synchronized (warmer)
      {
        for (Entry ent : warmer.entries.values()) 
        {
          // Skip entries that failed for any reason.
          if (ent.exception != null)
            continue;
          
          // If it's a rotating index (and something is pending), rotate now.
          if (ent.newPath.exists() && (ent.pendingPath.exists() || ent.sparePath.exists())) 
          { 
            if (ent.pendingPath.exists())
              return ent;
          }
          else if (ent.curSearcher != null)
          {
            // Old-style check: is there a new version?
            try {
              if (!ent.curSearcher.isUpToDate())
                return ent;
            } catch (IOException e) {
              ent.exception = e;
              Trace.error(String.format("Error checking index '%s': %s", ent.indexPath, e.toString()));
            }
          }
        }
        return null; // nothing to do.
      }
    }
    
    /**
     * Does the work of warming up an index.
     */
    private void warm(Entry ent) 
    {
      try 
      {
        Trace.info(String.format("Warming index [%s]", ent.indexPath));
        Trace.tab();
        
        File indexPath;
        Directory dir;

        // For new-style (rotating) warming, we're going to start with the
        // pending directory. Later, after it's warm we'll rename it and flip.
        //
        if (ent.newPath.exists() && ent.pendingPath.exists()) {
          indexPath = ent.pendingPath;
          dir = new FlippingDirectory(NativeFSDirectory.getDirectory(indexPath));
        }
        
        // Old-style warming is simpler.
        else {
          indexPath = ent.currentPath;
          dir = NativeFSDirectory.getDirectory(indexPath);
        }
        
        // Okay, load up the index along with ancillary files. Disable its update check.
        ent.newSearcher = new XtfSearcher(indexPath.toString(), dir, 0);
        
        // Validate this new index. If it fails, don't flip.
        IndexValidator val = new IndexValidator();
        if (!val.validate(warmer.xtfHome, indexPath.toString(), ent.newSearcher.indexReader()))
          throw new IndexValidator.ValidationError("Index validation failed.");
        
        // Ready to flip! Make sure everybody is locked out while we do it.
        synchronized (warmer) 
        {
          // If rotating...
          if (dir instanceof FlippingDirectory)
          {
            // Rotate  [spare] <- [current] <- [pending]
            if (ent.currentPath.exists()) {
              if (!ent.currentPath.renameTo(ent.sparePath))
                throw new IOException(String.format("Error renaming '%s' to '%s'", ent.currentPath, ent.sparePath));
            }
            if (!ent.pendingPath.renameTo(ent.currentPath))
              throw new IOException(String.format("Error renaming '%s' to '%s'", ent.pendingPath, ent.currentPath));
            
            // Flip to use the new current directory. We use this flip mechanism
            // so that the Lucene IndexReader doesn't have to close and reopen
            // all its files.
            //
            ((FlippingDirectory)dir).flipTo(NativeFSDirectory.getDirectory(ent.currentPath));
          }
          
          // Finally record the flip in the entry, so that future requests will
          // pick up the new Searcher.
          //
          ent.curSearcher = ent.newSearcher;
          ent.newSearcher = null;
          Trace.untab();
          Trace.info("Done.");
        }
      } 
      catch (Throwable exc) 
      {
        // If anything goes wrong, log the exception and record it in the entry.
        // Recording the exception will prevent us from re-trying.
        //
        ent.exception = exc;
        Trace.untab();
        Trace.error(String.format("Error warming index '%s': %s", ent.indexPath, exc.toString()));
      }
    }
    
  } // class BgThread

  /** An entry mapping indexPath to XtfSearcher */
  private static class Entry
  {
    String      indexPath;

    File currentPath;
    File pendingPath;
    File sparePath;
    File newPath;
    
    XtfSearcher curSearcher;
    XtfSearcher newSearcher;
    Throwable   exception;

    Entry(String indexPath)
    {
      this.indexPath  = indexPath;
      currentPath     = new File(indexPath);
      File parentDir  = currentPath.getParentFile();
      newPath         = new File(parentDir, currentPath.getName() + "-new");
      sparePath       = new File(parentDir, currentPath.getName() + "-spare");
      pendingPath     = new File(parentDir, currentPath.getName() + "-pending");
    }
  } // class Entry  

} // class IndexWarmer
