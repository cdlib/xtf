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
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Packs ints and strings into a byte buffer, using an efficient variable-size
 * int format. Transparently compresses and decompresses large buffers.
 * 
 * @author Martin Haye
 */
public class PackedByteBuf
{
    /** Byte buffer to read from or write to */
    private byte[]  bytes;
    
    /** Current position within the byte buffer, {@link #bytes} */
    private int     pos = 0;
    
    /** Temporary buffer used when decoding strings */
    private char[]  strChars;
    
    /** Tells whether we've attempted to compress the buffer */
    private boolean compressTried = false;
    
    /** Tells whether compression succeeded */
    private boolean compressed    = false;
    
    /** Original (uncompressed) length of the buffer */
    private int     uncompLen     = 0;
    
    /** Used to compress/decompress data */
    private static ThreadLocal compressInfo = new ThreadLocal();
    
    /** Special marker used to denote a compressed buffer */
    private static final byte compressMarker = (byte)0xBF; 
    
    /** Minimum buffer size to compress (below this, it's probably not
     *  worth even trying). Initial value: 100.
     */
    private static final int  compressLimitMin = 100; 
    
    /** Maximum buffer size to compress (above this, we wouldn't have
     *  enough bytes to store the length.) Initial value: 65000
     */
    private static final int  compressLimitMax = 65000; 
    
    /** 
     * Construct a byte buffer for writing into.
     *  
     * @param initialSize   Hint as to the likely maximum amount of data bytes.
     *                      If this is exceeded, the buffer will expand itself
     *                      automatically.
     */    
    public PackedByteBuf( int initialSize )
    {
        bytes = new byte[initialSize];
    } // constructor
    
    /**
     * Construct a byte buffer for reading from. This constructor reads a chunk
     * of data from 'in', starting at its current position.
     * 
     * @param in    Source for data
     * @param len   How many bytes to read
     */
    public PackedByteBuf( DataInput in, int len )
        throws IOException
    {
        bytes = new byte[len];
        in.readFully( bytes, 0, len );
        decompress();
    } // constructor

    /**
     * Construct a byte buffer from packed data that has been read somehow.
     * 
     * @param rawBytes  The raw data
     */
    public PackedByteBuf( byte[] rawBytes )
    {
        setBytes( rawBytes );
    } // constructor

    
    /**
     * Take a chunk of raw data for unpacking.
     * 
     * @param rawBytes    The raw data
     */
    public void setBytes( byte[] rawBytes )
    {
        bytes = rawBytes;
        reset();
        decompress();
    } // setBytes()
    
    /**
     * Given a raw buffer, this method determines if it is compressed, and if
     * so, decompresses it. Note that 'pos' may come out non-zero; reading should
     * start at 'pos', not zero.
     *
     */
    private void decompress()
    {
        // If the first byte isn't the special marker, there's no way the
        // buffer is compressed.
        //
        if( bytes.length < 3 || bytes[0] != compressMarker )
            return;
        
        // Okay, get the decompressed size.
        int size = (((int)bytes[1])&0xff) | ((((int)bytes[2])&0xff) << 8);
        if( size == 0 ) {
            // Special case: uncompressed data, but the first byte just happens
            // to be the marker.
            //
            bytes[2] = compressMarker;
            pos = 2;
            return;
        }
        
        // Get the thread-local compression info.
        CompressInfo info = (CompressInfo) compressInfo.get();
        if( info == null ) {
            info = new CompressInfo();
            compressInfo.set( info );
        }
        if( info.inflater == null )
            info.inflater = new Inflater( true ); // no header info
            
        // Make a buffer big enough to hold the decompressed data. Leave some
        // extra bytes at the end so we can catch errors resulting from too
        // much inflation.
        //
        byte[] outBuf = new byte[size+5];
        info.inflater.reset();
        info.inflater.setInput( bytes, 3, bytes.length - 3 );
        try {
            int resultLength = info.inflater.inflate( outBuf );
            assert resultLength == size;
        }
        catch( DataFormatException e ) {
            assert false : "PackedByteBuf data corrupted";
            throw new RuntimeException( e );
        }
        
        bytes = outBuf;
        pos = 0;
    } // decompress()
    
    /**
     * Obtain a copy of this buffer (with only the valid bytes)
     */
    public Object clone()
    {
        PackedByteBuf other = new PackedByteBuf( pos );
        System.arraycopy( bytes, 0, other.bytes, 0, pos );
        other.pos = pos;
        return other;
    } // clone()
    
    /**
     * Write out a single byte.
     */
    public void writeByte( byte b )
    {
        ensureSize( 1 );
        bytes[pos++] = b;
    } // writeByte()
    
    /**
     * Write out a bunch of bytes
     */
    public void writeBytes( byte[] b )
    {
        writeBytes( b, 0, b.length );
    } // writeBytes()
    
    /**
     * Write out a bunch of bytes
     */
    public void writeBytes( byte[] b, int offset, int length )
    {
        ensureSize( length );
        System.arraycopy( b, offset, bytes, pos, length );
        pos += length;
    } // writeBytes()
    
    /**
     * Write a (non-negative) integer to the buffer. Writes between 1 and 5
     * bytes, depending on the size of the number. 
     * 
     * @param n     The number to write
     */
    public void writeInt( int n )
    {
        assert n >= 0 : "Negative ints not allowed in PackedByteBuf.writeInt()";
        ensureSize( 4 );
        
        // Determine how big the number is
        int shift = 7;
        while( (n >> shift) != 0 )
            shift += 7;
        
        // Now write out the bytes, high-order first. Why? Because it's faster
        // to read that way, and read speed is what we need. :)
        //
        for( shift -= 7; shift > 0; shift -= 7 )
            bytes[pos++] = (byte) (((n >> shift) & 0x7f) | 0x80);
        bytes[pos++] = (byte) (n & 0x7f);
    } // writeInt()
    
    /**
     * Write a string to the buffer, using an efficient format.
     * 
     * @param s  The string to write.
     */
    public void writeString( String s )
    {
        // Quick out for empty strings.
        if( s.length() == 0 ) {
            ensureSize( 1 );
            bytes[pos++] = 0;
            return;
        }
        
        // Special case: if it's a short string, and no entries use the
        // high byte, do an optimized version.
        //
        int allBits = 0;
        for( int i = 0; i < s.length(); i++ )
            allBits |= s.charAt( i );
        if( s.length() < 64 && (allBits & 0xff) == allBits ) {
            ensureSize( s.length() + 1 );
            bytes[pos++] = (byte) s.length();
            char[] chars = s.toCharArray();
            assert chars.length == s.length();
            for( int i = 0; i < chars.length; i++ )
                bytes[pos++] = (byte) (chars[i] & 0xff);
            return;
        }
                
        // Otherwise, do it the old-fashioned way.
        writeInt( s.length() + 64 );
        for( int i = 0; i < s.length(); i++ )
            writeInt( s.charAt(i) );
    } // writeString()
    
    /**
     * Write a general character sequence to the buffer, using an efficient 
     * format.
     * 
     * @param s  The sequence to write.
     */
    public void writeCharSequence( CharSequence s )
    {
        // Quick out for empty strings.
        if( s.length() == 0 ) {
            bytes[pos++] = 0;
            return;
        }
        
        // Special case: if it's a short string, and all no entries use the
        // high byte, do an optimized version.
        //
        int allBits = 0;
        for( int i = 0; i < s.length(); i++ )
            allBits |= s.charAt( i );
        if( s.length() < 64 && (allBits & 0xff) == allBits ) {
            ensureSize( s.length() + 1 );
            bytes[pos++] = (byte) s.length();
            for( int i = 0; i < s.length(); i++ )
                bytes[pos++] = (byte) (s.charAt(i));
            return;
        }
                
        // Otherwise, do it the old-fashioned way.
        writeInt( s.length() + 64 );
        for( int i = 0; i < s.length(); i++ )
            writeInt( s.charAt(i) );
    } // writeCharSequence()
    
    /**
     * Write another buffer into this one.
     * 
     * @param b  The buffer to write.
     */
    public void writeBuffer( PackedByteBuf b )
    {
        writeInt( b.length() );
        writeBytes( b.bytes, 0, b.length() );
    } // writeBuffer()
    
    /**
     * Make sure that the buffer has room to hold at least nBytes bytes. If
     * not, it's expanded to make room.
     */
    private void ensureSize( int nBytes )
    {
        assert !compressTried;
        
        if( pos + nBytes >= bytes.length ) {
            byte[] newBytes = new byte[(pos + nBytes)*3/2];
            System.arraycopy( bytes, 0, newBytes, 0, pos );
            bytes = newBytes;
        }
    } // ensureSize()
    
    /**
     * Resets the buffer so that reads/writes occur at the start. Also sets
     * length() to zero (though reads can occur past length()).
     */
    public void reset()
    {
        pos = 0;
        compressed = compressTried = false;
    }

    /**
     * If the buffer hasn't been compressed yet, do so. After compression, no
     * more bytes may be added.
     */
    private void compress()
    {
        // If already done, get out. Also, if buffer is small, we're very
        // unlikely to get a good savings, so skip it.
        //
        if( compressTried || pos < compressLimitMin || pos > compressLimitMax )
            return;
        compressTried = true;
        
        // Get the thread-local compression info.
        CompressInfo info = (CompressInfo) compressInfo.get();
        if( info == null ) {
            info = new CompressInfo();
            compressInfo.set( info );
        }
        if( info.deflater == null )
            info.deflater = new Deflater( 9, true ); // no header info
            
        // Figure out how much space we need.
        int needed = (pos*4) + 50;
        if( info.buf.length < needed )
            info.buf = new byte[needed];
        info.deflater.reset();
        info.deflater.setInput( bytes, 0, pos );
        info.deflater.finish();
        int compressedDataLength = info.deflater.deflate(info.buf);
        assert compressedDataLength != 0 : "Deflater should not need more data";
        
        // If compression doesn't save any space, forget it.
        if( (compressedDataLength+3) >= pos )
            return;
        
        compressed = true;
        uncompLen = pos;

        System.arraycopy( info.buf, 0, bytes, 0, compressedDataLength );
        pos = compressedDataLength;
    }
    
    /**
     * Makes the buffer as small as possible to hold the existing data; after
     * this operation, no more data may be added.
     */
    public void compact()
    {
        compress();
        
        if( bytes.length != pos ) {
            byte[] copy = new byte[pos];
            System.arraycopy( bytes, 0, copy, 0, pos );
            bytes = copy;
        }
    } // compact()

    /**
     * Returns the number of bytes currently in the buffer. After this operation,
     * no more data may be added.
     */
    public int length()
    {
        compress();
        if( !compressed ) {
            if( pos > 0 && bytes[0] == compressMarker )
                return pos + 2;
            return pos;
        }
        return pos + 3;
    }
   
    /**
     * Copy the entire contents of the buffer to an output sink.
     * 
     * @param out   Where to write the data to.
     */ 
    public void output( DataOutput out )
        throws IOException
    {
        compress();
        output( out, pos );
    }

    /**
     * Copy some or all of the buffer to an output sink. Note that if 'len'
     * is greater than the buffer's size, the output will be padded at the
     * end with zeros.
     * 
     * @param out   Where to write the data to
     * @param len   How many bytes to write (okay to exceed buffer length)
     */
    public void output( DataOutput out, int len )
        throws IOException
    {
        assert pos > 0 : "Cannot output empty buffer";
        
        // The first byte is special: it marks whether the buffer is compressed
        // or not.
        //
        compress();
        if( compressed ) {
            out.write( compressMarker );
            assert( (uncompLen>>16) == 0 ) : "Tried to compress too much data"; 
            out.write( uncompLen & 0xff );
            out.write( (uncompLen>>8) & 0xff );
            out.write( bytes[0] );
            assert length() == pos + 3;
        }
        else {
            out.write( bytes[0] );
            if( bytes[0] == compressMarker ) {
                out.writeShort( 0 );
                assert length() == pos + 2;
            }
            else
                assert length() == pos;
        }

        // Write the rest of the bytes, and pad with zeros if requested.
        int nToWrite = (len <= pos) ? len : pos;
        out.write( bytes, 1, nToWrite-1 );
        if( nToWrite < len ) {
            byte[] zeros = new byte[len - nToWrite];
            out.write( zeros, 0, len - nToWrite );
        }
    }
    
    /**
     * Read in a single byte from the buffer.
     */
    public byte readByte()
    {
        return bytes[pos++];
    } // readByte()
    
    /**
     * Read in a bunch of bytes.
     */
    public void readBytes( byte[] bytes )
    {
        readBytes( bytes, 0, bytes.length );
    } // readBytes()
    
    /**
     * Read in a bunch of bytes.
     */
    public void readBytes( byte[] outBytes, int start, int length )
    {
        System.arraycopy( bytes, pos, outBytes, start, length );
        pos += length;
    } // readBytes()
    
    /** Skip a bunch of bytes */
    public void skipBytes( int num )
    {
        pos += num;
    } // skipBytes()
    
    /**
     * Read an integer from a buffer that was previously made with writeInt().
     */
    public int readInt()
    {
        // Optimize for the single-byte (common) case
        int n = bytes[pos++];
        if( (n & 0x80) == 0)
            return n;

        // Okay, handle the full case.
        n &= 0x7f;
        while( true ) {
            n <<= 7;
            byte b = bytes[pos++];
            if( (b & 0x80) == 0 )
                return n | b;
            n |= (b & 0x7f);
        }
    } // readInt()
    
    /** Skip over an integer made with writeInt() */
    public void skipInt()
    {
        while( (bytes[pos] & 0x80) != 0 )
            pos++;
        pos++;
    } // skipInt()
    
    /**
     * Read a string from a buffer that was previously made with writeString().
     */
    public String readString()
    {
        // Quick out for empty strings.
        int length = readInt();
        if( length == 0 )
            return "";
        
        // Handle the optimized case: short strings with no high bytes in
        // their characters.
        //
        char[] chars;
        if( length < 64 ) {
            if( strChars == null || strChars.length < length )
                strChars = new char[length];
            for( int i = 0; i < length; i++ )
                strChars[i] = (char) (((int)bytes[pos++]) & 0xff);
            return new String( strChars, 0, length );
        }
        
        // Handle the old-fashioned case, used for longer strings, or those
        // with high-byte characters.
        //
        length -= 64;
        chars = new char[length];
        for( int i = 0; i < length; i++ )
            chars[i] = (char) (readInt() & 0xffff);
        return new String( chars );
    } // readString()
    
    /**
     * Skip over a string that was written with writeString()
     */
    public void skipString()
    {
        // Handle the optimized version if present.
        int length = readInt();
        if( length < 64 ) {
            pos += length;
            return;
        }
        
        // Handle the old-fashioned version.
        length -= 64;
        for( int i = 0; i < length; i++ )
            skipInt();
    } // skipString()

    /**
     * Read a buffer that was previously packed into this one with 
     * writeBuffer().
     */
    public PackedByteBuf readBuffer()
    {
        int length = readInt();
        PackedByteBuf buf = new PackedByteBuf( length );
        readBytes( buf.bytes );
        return buf;
    } // readBuffer()
    
    /**
     * Skip over a buffer that was written with writeBuffer()
     */
    public void skipBuffer()
    {
        int length = readInt();
        pos += length;
    } // skipBuffer()

    /**
     * Keeps tracks of inflate/deflate stuff on a thread-local basis.
     */
    private class CompressInfo
    {
        Deflater deflater;
        Inflater inflater;
        byte[] buf = new byte[500];
    } // class CompressInfo
    
} // class PackedByteBuf
