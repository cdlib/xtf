package org.cdlib.xtf.util;

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

import java.io.IOException;
import java.io.Writer;

/**
 * This is a simple PrintStream derivative that sends its output to the
 * XTF Trace class instead of stdout or stderr.
 */
public class TraceWriter extends Writer
{
  /** What level to output messages at */
  private int traceLevel;
 
  /** Buffer to build up each line, flushed at newline */
  private StringBuffer buf = new StringBuffer();
  
  /**
   * Construct a TraceWriter, recording the Trace level that future
   * messages written to the stream will be output at.
   * 
   * @param traceLevel  Level to output future messages.
   */
  public TraceWriter( int traceLevel )
  {
    this.traceLevel = traceLevel;
  } // constructor
  
  /** Write a series of characters. Each newline-separated line is written
   *  to the Trace stream at the configured debug level.
   */
  public void write( char cbuf[], int off, int len ) 
    throws IOException
  {
    for( int i = 0; i < len; i++ ) {
        char c = cbuf[i+off];
        if( c == '\n' ) {
            output( buf.toString() );
            buf.setLength( 0 );
        }
        else
            buf.append( c );
    }
  } // write()

  /** Output a string at the configured debug level */
  private void output( String str )
  {
    switch( traceLevel ) {
    case Trace.debug:
        Trace.debug( str );
        break;
    case Trace.info:
        Trace.info( str );
        break;
    case Trace.warnings:
        Trace.warning( str );
        break;
    case Trace.errors:
        Trace.error( str );
        break;
    default:
        assert false : "Unrecognized trace level";
    }
  } // output()
  
  /** Flush any remaining output in the buffer */
  public void flush() throws IOException
  {
    if( buf.length() > 0 ) {
        output( buf.toString() );
        buf.setLength( 0 );
    }
  } // flush()
  
  /** Close the stream */
  public void close() throws IOException
  {
    flush();
  } // close()
  
} // class TraceWriter
