package org.cdlib.xtf.saxonExt.pipe;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.ListIterator;

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

/**
 * Keeps a pool of buffers used by the Pipe saxon extension functions, to
 * minimize per-request memory gobbling.
 */
class PipeBufferPool
{
  static final int MAX_SPARE_BUFS = 4;
  static final int BUF_SIZE = 32*1024; // 32 Kbytes
  static LinkedList spareBuffers = new LinkedList();

  /**
   * Allocate a buffer to use for I/O. Uses previously allocated buffer if 
   * possible (that buffer must have been deallocated using deallocBuffer()).
   */
  static synchronized byte[] allocBuffer()
  {
    byte[] buf = null;
    
    // Look for a previous buffer we can use.
    ListIterator iter = spareBuffers.listIterator();
    while (iter.hasNext() && buf == null)
    {
      Object obj = iter.next();
      iter.remove();
      
      // If it's a weak reference, the buffer might still be around.
      if (obj instanceof WeakReference) 
      {
        WeakReference<byte[]> ref = (WeakReference<byte[]>)obj;
        buf = ref.get();
      }
      else
        buf = (byte[]) obj;
    }

    // If no buffers available to re-use, create a new one.
    if (buf == null)
      buf = new byte[BUF_SIZE];
    
    // All done.
    return buf;
  }
  
  /**
   * Return a buffer so it can be re-used later. If we already have enough
   * spare buffers then make it a weak reference so the buffer can be 
   * garbage-collected.
   */
  static synchronized void deallocBuffer(byte[] buf)
  {
    // Remove buffers which got garbage-collected from the list.
    ListIterator iter = spareBuffers.listIterator();
    while (iter.hasNext()) {
      Object obj = iter.next();
      if (obj instanceof WeakReference && ((WeakReference)obj).get() == null)
        iter.remove();
    }
      
    // If we could use another permanent buffer, keep forever.
    if (spareBuffers.size() < MAX_SPARE_BUFS)
      spareBuffers.addFirst(buf);
    else 
    {
      // Otherwise make a weak reference so the buffer can be garbage
      // collected. There's still the chance that we'll get to re-use
      // it.
      //
      spareBuffers.addFirst(new WeakReference(buf));
    }
  }
}
