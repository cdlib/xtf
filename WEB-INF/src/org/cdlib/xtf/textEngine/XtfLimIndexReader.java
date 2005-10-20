package org.cdlib.xtf.textEngine;

/*
 * Copyright (c) 2005, Regents of the University of California
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

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.limit.LimIndexReader;
import org.cdlib.xtf.util.ThreadWatcher;

/**
 * Just like a {@link LimIndexReader} except it also does a periodic check if
 * the request has taken too long and should kill itself.
 */
public class XtfLimIndexReader extends LimIndexReader
{
  private int killCheckCounter = 0;

  /** Construct the index reader */
  public XtfLimIndexReader( IndexReader toWrap, int workLimit ) 
  {
    super( toWrap, workLimit );
  } // constructor
  

  /**
   * Called by LimTermDocs and LimTermPositions to notify us that a certain
   * amount of work has been done. We check the limit, and if exceeded, throw
   * an exception.
   * 
   * @param amount    How much work has been done. The unit is typically one
   *                  term or term-position.
   */
  protected final void work( int amount ) throws IOException 
  {
    super.work( amount );
    
    // Every once in a while, check if our thread has exceeded its time
    // limit and should kill itself.
    //
    if( killCheckCounter++ > 1000 ) {
        killCheckCounter = 0;
        if( ThreadWatcher.shouldDie(Thread.currentThread()) )
            throw new RuntimeException( "Runaway request - time limit exceeded" );
    }
  } // work()

} // class XtfLimIndexReader
