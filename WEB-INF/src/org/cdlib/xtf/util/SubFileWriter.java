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
import java.io.RandomAccessFile;

/**
 * Represents a single sub-file within a {@link StructuredFile}. A sub-file
 * provides standard DataInput/DataOutput facilities, and takes care of
 * writing to the correct subset of the main StructuredFile. 
 * 
 * @author Martin Haye
 */
class SubFileWriter implements SubStoreWriter
{
    /** Actual disk file to write to */
    private RandomAccessFile file;
    
    /** The structured file that owns this Subfile */
    private StructuredFile parent;
    
    /** Absolute file position for the subfile's start */
    private long segOffset;
    
    /** Current write position within the subfile */
    private long writtenPos = 0;
    
    /** Size of the buffer to maintain */
    private static final int BUF_SIZE = 50; // TODO: Increase buffer
    
    /** Buffered data (cuts down access to the physical file) */
    private byte[] buf = new byte[BUF_SIZE];
    
    /** Amount of data buffered */
    private int bufTop = 0;
    
    /**
     * Construct a subfile writer.
     * 
     * @param file      Disk file to attach to
     * @param parent    Structured file to attach to
     * @param segOffset Beginning offset of the segment
     */
    SubFileWriter( RandomAccessFile file, StructuredFile parent,
                   long segOffset )
        throws IOException
    {
        this.file      = file;
        this.parent    = parent;
        this.segOffset = segOffset;
    }

    public void close() throws IOException
    {
        synchronized( parent ) 
        {
            // Force a flush.
            checkLength( BUF_SIZE );
            
            // And notify the main StructuredFile.
            parent.closeWriter( this );
            file = null;
        }
    }

    public long length() throws IOException
    {
        return writtenPos + bufTop;
    }
    
    /**
     * Ensure that the buffer has room for the specified number of bytes.
     * If not, it is flushed.
     * 
     * @param nBytes    Amount of space desired
     */
    private void checkLength( int nBytes )
        throws IOException
    {
        if( parent.curSubFile != this ) {
            file.seek( writtenPos + segOffset );
            parent.curSubFile = this;
        }
        
        if( bufTop + nBytes > BUF_SIZE ) {
            file.write( buf, 0, bufTop );
            writtenPos += bufTop;
            bufTop = 0;
        }
    }
    
    public void write(byte[] b) throws IOException
    {
        write( b, 0, b.length );
    }

    public void write(byte[] b, int off, int len) throws IOException
    {
        synchronized( parent ) {
            checkLength( len );
            if( len > BUF_SIZE ) {
                file.write(b, off, len);
                writtenPos += len;
            }
            else {
                System.arraycopy( b, off, buf, bufTop, len );
                bufTop += len;
            }
        }
    }

    public void writeByte(int v) throws IOException
    {
        synchronized( parent ) {
            checkLength( 1 );
            buf[bufTop++] = (byte) (v & 0xff);
        }
    }

    public void writeChars(String s) throws IOException
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

    public void writeInt(int v) throws IOException
    {
        synchronized( parent ) {
            checkLength( 4 );
            buf[bufTop++] = (byte) ((v >>> 24) & 0xFF);
            buf[bufTop++] = (byte) ((v >>> 16) & 0xFF);
            buf[bufTop++] = (byte) ((v >>>  8) & 0xFF);
            buf[bufTop++] = (byte) ((v >>>  0) & 0xFF);
        }
    }

} // class Subfile
