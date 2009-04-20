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

import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.NativeFSLockFactory;

public class NativeFSDirectory extends FSDirectory
{
  private static HashMap<File, LockFactory> lockFactories = new HashMap();
  
  /** Returns the directory instance for the named location.
   * @param path the path to the directory.
   * @return the FSDirectory for the named file.  */
  public static FSDirectory getDirectory(String path)
    throws IOException 
  {
    return getDirectory(new File(path));
  }

  /** Returns the directory instance for the named location.
   * @param file the path to the directory.
   * @return the FSDirectory for the named file.  */
  public static FSDirectory getDirectory(File file)
    throws IOException 
  {
    file = new File(file.getCanonicalPath());
    return getDirectory(file, getLockFactory(file));
  }
  
  /** Get the lock factory for the given directory. If none yet, create one. */
  private static synchronized LockFactory getLockFactory(File f) 
    throws IOException 
  {
    LockFactory ret = lockFactories.get(f);
    if (ret == null) {
      ret = new NativeFSLockFactory(f);
      lockFactories.put(f, ret);
    }
    return ret;
  }
}