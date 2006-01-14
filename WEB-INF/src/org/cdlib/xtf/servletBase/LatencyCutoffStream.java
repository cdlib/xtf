package org.cdlib.xtf.servletBase;

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

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletOutputStream;

import org.cdlib.xtf.util.Trace;

/**
 * This class prints out latency information after a given number of bytes
 * have been output.
 */
class LatencyCutoffStream extends ServletOutputStream
{
    /** 
     * Constructor.
     *
     * @param realOut       The output stream to receive the limited output
     * @param limit         How many characters to output the message after
     * @param url           The URL of the request being served
     */
    public LatencyCutoffStream( OutputStream realOut, 
                           int          limit, 
                           long         reqStartTime, 
                           String       url )
    {
        this.realOut      = realOut;
        this.limit        = limit;
        this.url          = url;
        this.reqStartTime = reqStartTime;
    }

    /** Close the output stream */
    public void close()
        throws IOException
    {
        realOut.close();
    }

    /** Flush any pending data to the output stream */
    public void flush()
        throws IOException
    {
        realOut.flush();
    }

    /** Write an array of bytes to the output stream */
    public void write( byte[] b )
        throws IOException
    {
        write( b, 0, b.length );
    }

    /** Write a subset of bytes to the stream */
    public void write( byte[] b, int off, int len )
        throws IOException
    {
        if( !isReported && total > limit )
            reportLatency();
        realOut.write( b, off, len );
        total += len;
    }

    /** Write a single byte to the stream */
    public void write( int b )
        throws IOException
    {
        if( !isReported && total > limit )
            reportLatency();
        realOut.write( b );
        ++total;
    }

    /** Tells whether the latency was reported yet */
    public boolean isReported() { return isReported; }
    
    /** Report the latency and set the flag saying it has been done. */
    private void reportLatency() 
    {
        isReported = true;
        long latency = System.currentTimeMillis() - reqStartTime;
        Trace.info( "Latency (cutoff): " + latency + 
                    " msec for request: " + url );
    }
          
    /** The output stream to receive the output */
    private OutputStream realOut;

    /** How many bytes have been output so far */
    private int          total = 0;

    /** The limit on the number of bytes after which the message is printed */
    private int          limit;

    /** The URL of the request being served */
    private String       url;
    
    /** The start of the request, for timing purposes */
    private long         reqStartTime;
    
    /** Whether the message has been printed yet */
    private boolean      isReported = false;

} // class LatencyCutoffStream

