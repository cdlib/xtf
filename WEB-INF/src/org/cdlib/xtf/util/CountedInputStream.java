package org.cdlib.xtf.util;

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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Wraps an InputStream, and counts how many bytes have been read from it.
 *
 * @author Martin Haye
 */
public class CountedInputStream extends FilterInputStream 
{
  /** Count of the number of bytes read from the stream so far */
  private long nRead = 0;
  
  /** Wrap an input stream */
  public CountedInputStream( InputStream in ) {
    super( in );
  }
  
  /** Find out how many bytes have been read so far */
  public long nRead() { return nRead; }
  
  // inherit JavaDoc
  public int read() throws IOException {
    int retVal = in.read();
    if( retVal >= 0 )
        ++nRead;
    return retVal;
  }

  // inherit JavaDoc
  public int read( byte b[], int off, int len ) throws IOException {
    int retVal = in.read( b, off, len );
    if( retVal >= 0 )
        nRead += retVal;
    return retVal;
  }

  // inherit JavaDoc
  public long skip( long n ) throws IOException {
    long retVal = in.skip(n);
    if( retVal >= 0 )
        nRead += retVal;
    return retVal;
  }

  // inherit JavaDoc
  public boolean markSupported() {
    return false; // we don't want to worry about marking the number of bytes
  }
  
} // class CountedInputStream
