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


/**
 * Provides quick access to a disk-based hash table created by 
 * a {@link DiskHashWriter}.
 * 
 * @author Martin Haye
 */
public class DiskHashReader
{
    /** Size of the header we expect to find */
    static final int headerSize = 12;
    
    /** Subfile to read the hash from */
    private Subfile       subfile;
    
    /** Number of hash slots in the subfile */
    private int           nSlots;
    
    /** Size of each hash slot */
    private int           slotSize;
    
    /** Buffer used to read hash slot bytes */
    private byte[]        slotBytes;
    
    /** Used to decode hash slot values */
    private PackedByteBuf slotBuf;
    
    /**
     * Read in the header of of the hash from the given subfile.
     * 
     * @param subfile   Must have been created by DiskHashWriter.outputTo()
     */
    public DiskHashReader( Subfile subfile )
        throws IOException
    {
        this.subfile = subfile;
        
        // Read the header.
        byte[] magic = new byte[4];
        subfile.read( magic );
        if( magic[0] != 'h' || magic[1] != 'a' || magic[2] != 's' || magic[3] != 'h' )
            throw new IOException( "Subfile isn't a proper DiskHash" );
        
        nSlots   = subfile.readInt();
        slotSize = subfile.readInt();
        
        // Allocate the slot buffer.
        slotBytes = new byte[slotSize];
        slotBuf   = new PackedByteBuf( slotBytes );
    } // constructor
    
    /**
     * Closes the reader (and its associated subfile).
     */
    public void close()
    {
        try {
            subfile.close();
        } catch( Exception e ) { }
        subfile = null;
    } // close()

    /**
     * Locate the entry for the given string key. If not found, returns null.
     * @param key   key to look for
     */
    public PackedByteBuf find( String key  )
        throws IOException
    {
        // Don't allow empty string as a key, since it's used to mark
        // the end of a slot.
        //
        if( key.length() == 0 )
            key = " ";
        
        // Find the location of the slot data. If zero, we can fail now.
        int slotNum = (key.hashCode() & 0xffffff) % nSlots;
        subfile.seek( headerSize + (slotNum * 4) );
        int slotOffset = subfile.readInt();
        if( slotOffset == 0 )
            return null;
        
        assert (slotOffset+slotSize) <= subfile.length() : "Corrupt hash offset";
        
        // Read the slot data (may be too much, but will always be enough).
        subfile.seek( slotOffset );
        subfile.readFully( slotBytes );
        slotBuf.setBytes( slotBytes );
        
        // Now scan the entries
        while( true ) {
            
            // Get the name. If empty, give up.
            String name = slotBuf.readString();
            if( name.length() == 0 )
                return null;
            
            // Does it match? If not, advance to the next slot.
            if( !name.equals(key) ) {
                slotBuf.skipBuffer();
                continue;
            }
            
            // Got a match!
            return slotBuf.readBuffer();
        } // while
    } // find()
    
} // class DiskHashReader
