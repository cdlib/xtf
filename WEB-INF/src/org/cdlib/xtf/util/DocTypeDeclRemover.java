package org.cdlib.xtf.util;

/**
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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * The parser that comes with the JDK always tries to resolve DOCTYPE 
 * declarations in an XML file, but will barf if it can't. We want to be
 * able to work with such documents regardless of whether the DOCTYPE
 * is resolvable or not. Hence this class, which filters out DOCTYPE 
 * declarations entirely.
 */
public class DocTypeDeclRemover extends BufferedInputStream 
{
  /** Marks whether we've scanned the initial block for a DOCTYPE decl */
  private boolean firstTime = true;
  
  /** How many bytes to scan before giving up */
  private static final int BLOCK_SIZE = 1024;
  
  /** Default constructor: records the input stream to filter. */
  public DocTypeDeclRemover( InputStream in ) {
      super( in, BLOCK_SIZE );
  }
  
  /**
   * See
   * the general contract of the <code>read</code>
   * method of <code>InputStream</code>.
   * 
   * @return     the next byte of data, or <code>-1</code> if the end of the
   *             stream is reached.
   * @exception  IOException  if an I/O error occurs.
   * @see        java.io.FilterInputStream#in
   */
  public int read() throws IOException {
      if( firstTime ) {
          byte[] buf = new byte[1];
          if( read(buf, 0, 1) != 1 )
              return -1;
          return buf[0] & 0xff;
      }
      else
          return super.read();
  }
  
  /** 
   * Read a block of bytes. The first {@link #BLOCK_SIZE} bytes will be
   * scanned for a DOCTYPE declaration, and if one is found it will be
   * converted to an XML comment.
   * 
   * @param b    Buffer to read into
   * @param off  Byte offset to read into
   * @param len  Number of bytes to read
   * @return     Number of bytes read, or <code>-1</code> if the end of
   *             the stream has been reached.
   * @exception  IOException  if an I/O error occurs.
   */
  public int read( byte b[], int off, int len ) throws IOException 
  {
      // The first time through, scan the start of the file for a DOCTYPE
      // declaration.
      //
      if( firstTime ) 
      {
          // Make sure we have a block of data to examine
          super.read( b, off, (len > BLOCK_SIZE-1) ? (BLOCK_SIZE-1) : len );
          
          // Do a sloppy job of converting it to a string.
          char[] cbuf = new char[count];
          for( int i = 0; i < count; i++ )
              cbuf[i] = (char) (((int)buf[i]) & 0xff);
          String s = new String( cbuf );
          
          // Now look for a DOCTYPE declaration.
          int start = s.indexOf( "<!DOCTYPE" );
          int end = s.indexOf( ">", start+1 );
          if( start >= 0 && end >= 0 ) 
          {
              // We found one... change it into an XML comment.
              buf[start+2] = '-';
              buf[start+3] = '-';
              for( int i = start+4; i < end-2; i++ )
                  buf[i] = 'z';
              buf[end-1] = '-';
              buf[end-2] = '-';
          }

          // Reset the file position so the client will see the modified
          // data.
          //
          pos = 0;
          firstTime = false;
      }
      
      return super.read( b, off, len );
      
  } // read( byte[], int, int )
  
} // class DocTypeDeclRemover
