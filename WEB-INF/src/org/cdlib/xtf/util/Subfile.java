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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Represents a single sub-file within a {@link StructuredFile}. A sub-file
 * provides standard DataInput/DataOutput facilities, and takes care of
 * writing to the correct subset of the main StructuredFile. 
 * 
 * @author Martin Haye
 */
public class Subfile implements DataInput, DataOutput
{
    /** Actual disk file to write to */
    private RandomAccessFile file;
    
    /** The structured file that owns this Subfile */
    private StructuredFile parent;
    
    /** Absolute file position for the subfile's start */
    private long segOffset;
    
    /** Length of this subfile */
    private long segLength;
    
    /** Current read/write position within the subfile */
    private long curPos = 0;
    
    /**
     * Construct a subfile. If segLength is nonzero, reads and writes will
     * be constrained to the specified limit.
     * 
     * @param file      Disk file to attach to
     * @param parent    Structured file to attach to
     * @param segOffset Beginning offset of the segment
     * @param segLength Length of the segment (-1 for unlimited)
     */
    Subfile( RandomAccessFile file, StructuredFile parent,
             long segOffset, long segLength )
        throws IOException
    {
        this.file      = file;
        this.parent    = parent;
        this.segOffset = segOffset;
        this.segLength = segLength;
        curPos = segOffset;
    }

    public void close() throws IOException
    {
        synchronized( parent ) {
            parent.closeSubfile( this );
            file = null;
        }
    }

    public long getFilePointer() throws IOException
    {
        synchronized( parent ) {
            checkLength( 0 );
            return file.getFilePointer() - segOffset;
        }
    }

    public long length() throws IOException
    {
        synchronized( parent ) {
            if( segLength < 0 )
                return file.length() - segOffset;
            else
                return segLength;
        }
    }
    
    /**
     * Ensure that the sub-file has room to read the specified number of
     * bytes. As a side-effect, we also check that the main file position
     * is current for this sub-file, and if not, we save the position for
     * the other sub-file and restore ours.
     * 
     * @param nBytes    Amount of space desired
     */
    private void checkLength( int nBytes )
        throws IOException
    {
        synchronized( parent ) {
            if( parent.curSubfile != this ) {
                if( parent.curSubfile != null )
                    parent.curSubfile.curPos = file.getFilePointer();
                file.seek( curPos );
                parent.curSubfile = this;
            }
            
            if( segLength < 0 )
                return;
            if( file.getFilePointer() + nBytes - segOffset > segLength )
                throw new EOFException( "End of sub-file reached" );
        }
    }

    public int read(byte[] b) throws IOException
    {
        synchronized( parent ) {
            checkLength( b.length );
            return file.read(b);
        }
    }

    public int read(byte[] b, int off, int len) throws IOException
    {
        synchronized( parent ) {
            checkLength( len );
            return file.read(b, off, len);
        }
    }

    public void readFully(byte[] b) throws IOException
    {
        synchronized( parent ) {
            checkLength( b.length );
            file.readFully(b);
        }
    }

    public void readFully(byte[] b, int off, int len) throws IOException
    {
        synchronized( parent ) {
            checkLength( len );
            file.readFully(b, off, len);
        }
    }

    public void seek(long pos) throws IOException
    {
        synchronized( parent ) {
            if( parent.curSubfile != this ) {
                if( parent.curSubfile != null )
                    parent.curSubfile.curPos = file.getFilePointer();
                parent.curSubfile = this;
            }
            
            if( segLength >= 0 && pos > segLength )
                throw new EOFException( "Cannot seek past end of subfile" );
            file.seek(pos + segOffset);
        }
    }

    public void setLength(long newLength) throws IOException
    {
        synchronized( parent ) {
            if( segLength >= 0 )
                throw new IOException( "Cannot set length of previously created subfile" );
            file.setLength(newLength + segOffset);
        }
    }

    public int skipBytes(int n) throws IOException
    {
        synchronized( parent ) {
            checkLength( n );
            return file.skipBytes(n);
        }
    }

    public void write(byte[] b) throws IOException
    {
        synchronized( parent ) {
            checkLength( b.length );
            file.write(b);
        }
    }

    public void write(byte[] b, int off, int len) throws IOException
    {
        synchronized( parent ) {
            checkLength( len );
            file.write(b, off, len);
        }
    }

    public int read() throws IOException
    {
        synchronized( parent ) {
            checkLength( 1 );
            return file.read();
        }
    }

    public boolean readBoolean() throws IOException
    {
        synchronized( parent ) {
            checkLength( 1 );
            return file.readBoolean();
        }
    }

    public byte readByte() throws IOException
    {
        synchronized( parent ) {
            checkLength( 1 );
            return file.readByte();
        }
    }

    public char readChar() throws IOException
    {
        synchronized( parent ) {
            checkLength( 2 );
            return file.readChar();
        }
    }

    public double readDouble() throws IOException
    {
        synchronized( parent ) {
            checkLength( 8 );
            return file.readDouble();
        }
    }

    public float readFloat() throws IOException
    {
        synchronized( parent ) {
            checkLength( 4 );
            return file.readFloat();
        }
    }

    public int readInt() throws IOException
    {
        synchronized( parent ) {
            checkLength( 4 );
            return file.readInt();
        }
    }

    public String readLine() throws IOException
    {
        synchronized( parent ) {
            checkLength( 1 );
            String ret = file.readLine();
            checkLength( 0 );
            return ret;
        }
    }

    public long readLong() throws IOException
    {
        synchronized( parent ) {
            checkLength( 4 );
            return file.readLong();
        }
    }

    public short readShort() throws IOException
    {
        synchronized( parent ) {
            checkLength( 4 );
            return file.readShort();
        }
    }

    public int readUnsignedByte() throws IOException
    {
        synchronized( parent ) {
            checkLength( 1 );
            return file.readUnsignedByte();
        }
    }

    public int readUnsignedShort() throws IOException
    {
        synchronized( parent ) {
            checkLength( 1 );
            return file.readUnsignedShort();
        }
    }

    public String readUTF() throws IOException
    {
        synchronized( parent ) {
            checkLength( 1 );
            String ret = file.readUTF();
            checkLength( 0 );
            return ret;
        }
    }

    public void write(int b) throws IOException
    {
        synchronized( parent ) {
            checkLength( 1 );
            file.write(b);
        }
    }

    public void writeBoolean(boolean v) throws IOException
    {
        synchronized( parent ) {
            checkLength( 1 );
            file.writeBoolean(v);
        }
    }

    public void writeByte(int v) throws IOException
    {
        synchronized( parent ) {
            checkLength( 1 );
            file.writeByte(v);
        }
    }

    public void writeBytes(String s) throws IOException
    {
        synchronized( parent ) {
            checkLength( s.length() );
            file.writeBytes(s);
        }
    }

    public void writeChar(int v) throws IOException
    {
        synchronized( parent ) {
            checkLength( 2 );
            file.writeChar(v);
        }
    }

    public void writeChars(String s) throws IOException
    {
        synchronized( parent ) {
            checkLength( s.length() * 2 );
            file.writeChars(s);
        }
    }

    public void writeDouble(double v) throws IOException
    {
        synchronized( parent ) {
            checkLength( 8 );
            file.writeDouble(v);
        }
    }

    public void writeFloat(float v) throws IOException
    {
        synchronized( parent ) {
            checkLength( 4 );
            file.writeFloat(v);
        }
    }

    public void writeInt(int v) throws IOException
    {
        synchronized( parent ) {
            checkLength( 4 );
            file.writeInt(v);
        }
    }

    public void writeLong(long v) throws IOException
    {
        synchronized( parent ) {
            checkLength( 8 );
            file.writeLong(v);
        }
    }

    public void writeShort(int v) throws IOException
    {
        synchronized( parent ) {
            checkLength( 2 );
            file.writeShort(v);
        }
    }

    public void writeUTF(String str) throws IOException
    {
        synchronized( parent ) {
            checkLength( str.length() * 3 );
            file.writeUTF(str);
        }
    }

} // class Subfile
