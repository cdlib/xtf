package org.cdlib.xtf.util;

import java.io.IOException;

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

/*
 * This file created on Mar 11, 2005 by Martin Haye
 */

/**
 * Writes to a single sub-store within a {@link StructuredStore}. A sub-store
 * provides most of the interface of a RandomAccessFile, and takes care of
 * writing to the correct subset of the main StructuredStore. 
 * 
 * @author Martin Haye
 */
public abstract class SubStoreWriter 
{
  public void write(byte[] b) throws IOException
  {
      write( b, 0, b.length );
  }
  
  public abstract void write(byte[] b, int off, int len) throws IOException;
  public abstract void writeByte(int b) throws IOException;
  
  public void writeChars( String s ) throws IOException
  {
      int clen = s.length();
      int blen = 2*clen;
      byte[] b = new byte[blen];
      char[] c = new char[clen];
      s.getChars( 0, clen, c, 0 );
      for( int i = 0, j = 0; i < clen; i++ ) {
          b[j++] = (byte)(c[i] >>> 8);
          b[j++] = (byte)(c[i] >>> 0);
      }
      write( b );
  }

  public abstract void writeInt(int v) throws IOException;
  
  public abstract long length() throws IOException;
  public abstract void close() throws IOException;
}