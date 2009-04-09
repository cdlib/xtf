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
import java.io.IOException;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.LockFactory;

/**
 * Represents a Lucene directory in every way except that it allows the
 * underlying directory pointer to be flipped. This facilitates warming
 * a directory in the background before flipping it to the foreground.
 *
 * @author Martin Haye
 */
public class FlippingDirectory extends Directory
{
  private Directory wrapped;
  
  /** Wrap a directory to start with */
  public FlippingDirectory(Directory toWrap)
  {
    wrapped = toWrap;
  }
  
  /** 
   * Switch to a different underlying directory. Note that it should still
   * contain the same files, and thus should be the old directory with a
   * simply a different name. 
   */
  public void flipTo(Directory other)
  {
    wrapped = other;
  }

  /////////////////////////////////////////////////////////////////////////////
  // Delegated methods
  /////////////////////////////////////////////////////////////////////////////
  
  @Override
  public void clearLock(String name) throws IOException {
    wrapped.clearLock(name);
  }
  
  @Override
  public void close() throws IOException {
    wrapped.close();
  }
  
  @Override
  public IndexOutput createOutput(String name) throws IOException {
    return wrapped.createOutput(name);
  }
  
  @Override
  public void deleteFile(String name) throws IOException {
    wrapped.deleteFile(name);
  }
  
  @Override
  public boolean fileExists(String name) throws IOException {
    return wrapped.fileExists(name);
  }
  
  @Override
  public long fileLength(String name) throws IOException {
    return wrapped.fileLength(name);
  }
  
  @Override
  public long fileModified(String name) throws IOException {
    return wrapped.fileModified(name);
  }
  
  @Override
  public LockFactory getLockFactory() {
    return wrapped.getLockFactory();
  }
  
  @Override
  public String getLockID() {
    return wrapped.getLockID();
  }
  
  @Override
  public String[] list() throws IOException {
    return wrapped.list();
  }
  
  @Override
  public Lock makeLock(String name) {
    return wrapped.makeLock(name);
  }
  
  @Override
  public IndexInput openInput(String name) throws IOException {
    return wrapped.openInput(name);
  }
  
  @Override
  @SuppressWarnings("deprecation")
  public void renameFile(String from, String to) throws IOException {
    wrapped.renameFile(from, to);
  }
  
  @Override
  public void setLockFactory(LockFactory lockFactory) {
    wrapped.setLockFactory(lockFactory);
  }
  
  @Override
  public void touchFile(String name) throws IOException {
    wrapped.touchFile(name);
  }
  
}
