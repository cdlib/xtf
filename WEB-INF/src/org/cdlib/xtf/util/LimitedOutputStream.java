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

import java.io.IOException;
import java.io.OutputStream;

/**
 * This class is useful only for testing the transmission speed of data
 * by limiting the size of the output stream. Not to be used in production.
 */
class LimitedOutputStream extends OutputStream
{
    /** 
     * Constructor.
     *
     * @param realOut       The output stream to receive the limited output
     * @param limit         How many characters to limit it to.
     */
    public LimitedOutputStream( OutputStream realOut, int limit )
    {
        this.realOut = realOut;
        this.limit   = limit;
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
        int max = limit - total;
        if( max > 0 ) {
            if( max > len )
                max = len;
            realOut.write( b, off, max );
            total += max;
        }
    }

    /** Write a single byte to the stream */
    public void write( int b )
        throws IOException
    {
        if( total < limit ) {
            realOut.write( b );
            ++total;
        }
    }

    /** The output stream to receive the limited output */
    private OutputStream realOut;

    /** How many bytes have been output so far */
    private int          total = 0;

    /** The limit on the number of bytes */
    private int          limit;

} // class LimitedOutputStream

