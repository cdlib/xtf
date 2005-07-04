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

/**
 * Base class for the various selector classes that are chained together
 * to execute a selection expression in a faceted query.
 * 
 * @author Martin Haye
 */
public abstract class GroupSelector 
{
  protected boolean       conservative;
  protected GroupSelector next;
  protected GroupCounts   counts;
  
  /** Set the next selector in the chain */
  public void setNext( GroupSelector next ) {
    this.next = next;
  }

  /** Set the counts to be used */
  public void setCounts( GroupCounts counts ) {
    this.counts = counts;
    if( next != null )
        next.setCounts( counts );
  }

  /** Reset the selector */
  public void reset( boolean conservative ) {
    this.conservative = conservative;
    if( next != null )
        next.reset( conservative );
  }
  
  /** Process the next group */
  public abstract void process( int group );
  
  /** Flush any queued groups */
  public void flush() {
    if( next != null )
        next.flush();
  }
  
  /** Get a string representation */
  public abstract String toString();
  
} // class GroupSelector
