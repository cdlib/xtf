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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;
import java.util.HashMap;


/**
 * A simple structured file with a flat top-level directory. {@link Subfile}s 
 * can be added to an existing file.
 * 
 * @author Martin Haye
 */
public class StructuredFile
{
    /** Actual file path of the structured file */
    private File             file;
    
    /** Used to read/write the disk file */
    private RandomAccessFile realFile;
    
    /** 
     * File position of the subfile directory (zero if the directory has been 
     * erased) 
     */
    private int              dirPos;
    
    /** Current subfile directory */
    private Directory        dir;
    
    /** 
     * True when creating a sub-file; enforces the rule that only one sub-file
     * may be created at a time.
     */
    private Subfile          creatingSubfile;
    
    /** 
     * Directory entry of the sub-file being created (if
     * {@link #creatingSubfile} is true).
     */
    private DirEntry         creatingEnt;
    
    /** List of currently opened subfiles */
    private LinkedList       openSubfiles = new LinkedList();
    
    /** 
     * The sub-file that last accessed the file. This is checked every time
     * a sub-file wants to access the file, to see if the file pointer needs
     * to be saved/restored. Package-private, since the Subfile class needs
     * access to it.
     */
    Subfile curSubfile = null;
            
    /** Number of currently open Structured files */  
    private int              openCount = 0;
    
    /** 
     * Map of currently open Structured files. Used to ensure that
     * only one instance of a given file is in memory at any given time,
     * to avoid concurrency problems.
     */
    private static HashMap   fileMap = new HashMap();
            
    /**
     * Instances should never be created by outside parties, so the constructor
     * is strictly private.
     */
    private StructuredFile( File file, boolean create )
        throws IOException
    {
        this.file = file;
        if( !create && !file.exists() )
            throw new FileNotFoundException( file.toString() );
        realFile = new RandomAccessFile( file, "rw" );
        if( create ) {
            realFile.setLength( 0 );
            
            // Now write the header. First the identifier.
            realFile.writeByte( 's' );
            realFile.writeByte( 's' );
            realFile.writeByte( 'f' );
            realFile.writeByte( 0 );
            
            // Write a placeholder for the directory position.
            realFile.writeInt( 0 );
            
            // Now write a new directory.
            dir = new Directory();
            writeDirectory();
        }
        else
            readHeader();
    } // constructor
    
    /** Gets the File (path) associated with this structured file */
    public File getFile() {
        return file;
    }

    /**
     * Reads the file header and directory of the structured file. Called by
     * constructor.
     */
    private void readHeader()
        throws IOException
    {
        realFile.seek( 0 );

        // Verify the header.
        if( realFile.readByte() != 's' ||
            realFile.readByte() != 's' ||
            realFile.readByte() != 'f' ||
            realFile.readByte() != 0 )
                throw new IOException( "File is not a structured file" );
        
        // Get the position of the directory, and read it in.
        dirPos = realFile.readInt();
        realFile.seek( dirPos );
        dir = new Directory( realFile );
    } // readHeader()
    
    /**
     * Create a structured file from scratch. Any existing file is replaced by
     * a new empty one.
     * 
     * @param file  The file path to write to.
     */
    public static synchronized StructuredFile create( File file )
        throws IOException
    {
        StructuredFile sf;
        
        // Do we already have an open instance?
        if( fileMap.get(file) != null )
            sf = (StructuredFile) fileMap.get(file);
        else {
            sf = new StructuredFile( file, true );
            fileMap.put( file, sf );
        }
        
        sf.openCount++;
        return sf;
    }
    
    /**
     * Open an existing structured file.
     * @param file  The file to open.
     * @throws FileNotFoundException    If the file doesn't exist.
     */
    public static synchronized StructuredFile open( File file )
        throws FileNotFoundException, IOException
    {
        StructuredFile sf;
        
        if( fileMap.get(file) != null )
            sf = (StructuredFile) fileMap.get(file);
        else {
            sf = new StructuredFile( file, false );
            fileMap.put( file, sf );
        }

        sf.openCount++;
        return sf;
    }
    
    /**
     * Create a new sub-file with the specified name. Returns a Subfile that
     * has most of the interface of a RandomAccessFile, except that seeks
     * will be relative to the sub-file start.
     * 
     * Only one subfile may be created at a time (though many others may be
     * opened, provided they were created before.)
     * 
     * The caller must call Subfile.close() when the file is complete, to
     * ensure that the directory gets written.
     * 
     * @param name  Name of the sub-file to create. Must not exist.
     * @return      A subfile to write to.
     */
    public synchronized Subfile createSubfile( String name )
        throws IOException
    {
        // Can only create one sub-file at a time.
        if( creatingSubfile != null )
            throw new IOException( "Can only create one sub-file at a time" );
                
        // Make sure it doesn't exist, since we have no way to overwrite one.
        if( dir.find(name) != null )
            throw new IOException( "Cannot create sub-file: already exists" );
        
        // Erase the current directory (it will be overwritten by the new 
        // subfile).
        //
        eraseDirectory();
        
        // Add a new entry to the directory (but leave the length at zero.)
        DirEntry ent = new DirEntry();
        ent.name = name;
        ent.segOffset = (int) realFile.length();
        dir.add( ent );

        // Finally create the Subfile and record that it's open now.
        Subfile subfile = new Subfile( realFile, this, ent.segOffset, -1 );
        creatingSubfile = subfile;
        creatingEnt     = ent;
        return subfile;
    } // createSubfile()
    
    /**
     * Opens a pre-existing subfile for read (or write). Returns a subfile that
     * has most of the interface of a RandomAccessFile, except that seeks will
     * be relative to the sub-file start, and IO operations cannot exceed the
     * boundaries of the sub-file.
     * 
     * Many subfiles may be open simultaneously; each one has an independent
     * file pointer. Each one is light weight, so it's okay to have many open
     * at a time.
     * 
     * @param name  Name of pre-existing subfile to open.
     */
    public synchronized Subfile openSubfile( String name )
        throws FileNotFoundException, IOException
    {
        // Find the directory entry.
        DirEntry ent = dir.find( name );
        if( ent == null )
            throw new FileNotFoundException( "Sub-file " + name + " not found." );
        
        // Make sure this isn't currently being created.
        if( creatingEnt == ent )
            throw new IOException( "Cannot open in-progress subfile" );
        
        // Make a sub-file instance to read it.
        Subfile sub = new Subfile( realFile, this, 
                                   ent.segOffset, ent.segLength );
        openSubfiles.add( sub );
        return sub;
    } // openSubfile()

    /**
     * Called by a subfile when its close() method is called. If the subfile is
     * newly created, we update the directory.
     * 
     * @param subfile   The sub-file being closed.
     */
    synchronized void closeSubfile( Subfile subfile )
        throws IOException
    {
        // If this is a newly created file, update the directory entry.
        if( creatingSubfile == subfile ) {
            creatingEnt.segLength = (int)realFile.length() - 
                                    creatingEnt.segOffset;
            writeDirectory();
            creatingSubfile = null;
            creatingEnt = null;
        }
        else {
            // Remove this from the list of open files.
            if( !openSubfiles.remove(subfile) )
                assert false : "Tried to close sub-file not in list";
        }
        
        if( curSubfile == subfile )
            curSubfile = null;
    } // closeSubfile()
    
    /**
     * Sets a user-defined version number for the file. It can be retrieved
     * later with {@link #getUserVersion()}.
     * 
     * @param ver   The version number to set.
     */
    public synchronized void setUserVersion( String ver )
        throws IOException
    {
        // Only update if changing.
        if( dir.getUserVersion().equals(ver) )
            return;
        
        // Okay, change the directory.
        dir.setUserVersion( ver );
        
        // Write the new directory on disk, unless it will already be when
        // a created file is closed.
        //
        if( dirPos != 0 ) {
            eraseDirectory();
            writeDirectory();
        }
        
    } // setUserVersion()
    
    /**
     * Gets the user version (if any) set by {@link #setUserVersion(String)}.
     */
    public String getUserVersion()
    {
        return dir.getUserVersion();
    } // getUserVersion()

    /**
     * Closes the file. This should always be called, to ensure that all
     * sub-files have been closed and that the directory has been written.
     */
    public synchronized void close()
        throws IOException
    {
        synchronized( getClass() ) 
        {
            // Decrement the count, and if it's not zero yet, wait for another
            // close later on.
            //
            openCount--;
            if( openCount > 0 )
                return;
            
            // Remove this instance from the file map, so it never gets used again.
            fileMap.remove( file );
            
            // If a file was being created, close it.
            if( creatingSubfile != null ) {
                creatingSubfile.close();
                creatingSubfile = null;
            }
            
            // If any subfiles are open, close them too.
            while( !openSubfiles.isEmpty() ) {
                Subfile sub = (Subfile) openSubfiles.getFirst();
                sub.close();
            } // while
            
            // And close the underlying file.
            if( realFile != null ) {
                realFile.close();
                realFile = null;
            }
        }
    } // close()
    
    /** 
     * Gets rid of the directory at the end of the file, in preparation for
     * overwriting it with a new subfile. When the subfile is complete, call
     * writeDirectory() to record the change.
     */
    private void eraseDirectory()
        throws IOException
    {
        // If already erased, don't do anything.
        if( dirPos == 0 )
            return;
        
        // Truncate the file (erasing the old directory)
        realFile.setLength( dirPos );
        
        // Change the pointer at the start of the file to zero, denoting that
        // the directory has been erased.
        //
        realFile.seek( 4 );
        realFile.writeInt( 0 );
        dirPos = 0;
    } // eraseDirectory()
    
    /**
     * Writes the current directory at the end of the file. Assumes the
     * previous directory was already erased (by calling eraseDirectory()).
     */
    private void writeDirectory()
        throws IOException
    {
        if( dirPos != 0 )
            throw new IOException( "Cannot writeDirectory() before eraseDirectory()" );
            
        // The new directory position: the end of the file.
        dirPos = (int) realFile.length();
        
        // Write it out.
        realFile.seek( dirPos );
        dir.writeTo( realFile );
        
        // And update the directory position pointer at the start of the file.
        realFile.seek( 4 );
        realFile.writeInt( dirPos );
    } // writeDirectory()
    
    /**
     * Maintains the directory of files within a structured file.
     */
    private class Directory
    {
        /** All the entries currently in the directory */
        private DirEntry[] entries;
        
        /** Version string established by the client */
        private String     userVersion;
        
        /** Create a new directory */
        public Directory()
        {
            entries = new DirEntry[0];
            userVersion = "";
        }
        
        /** Read a directory from a DataInput stream */
        public Directory( DataInput in )
            throws IOException
        {
            // Read the magic code.
            if( in.readByte() != 'd' ||
                in.readByte() != 'i' ||
                in.readByte() != 'r' ||
                in.readByte() != 0 )
                    throw new IOException( "Structured file directory corrupted" );
            
            // Read the length in bytes, then fetch all the data.
            int length = in.readInt();
            PackedByteBuf buf = new PackedByteBuf( in, length );
            
            // Read the user version.
            userVersion = buf.readString();
            
            // Find out how many entries there are, then read them all in.
            int nEntries = buf.readInt();
            entries = new DirEntry[nEntries];
            for( int i = 0; i < nEntries; i++ )
                entries[i] = new DirEntry( buf );
        } // constructor
        
        /** Write out the current directory to a DataOutput stream */
        public void writeTo( DataOutput out )
            throws IOException
        {
            // Pack up the user version.
            PackedByteBuf buf = new PackedByteBuf( 500 );
            buf.writeString( userVersion );
            
            // Pack up all the entries.
            buf.writeInt( entries.length );
            for( int i = 0; i < entries.length; i++ )
                entries[i].writeTo( buf );
            
            // Write the magic code.
            out.writeByte( 'd' );
            out.writeByte( 'i' );
            out.writeByte( 'r' );
            out.writeByte( 0 );
            
            // Write the length (in bytes) of the packed entries, then the
            // entries themselves.
            //
            out.writeInt( buf.length() );
            buf.output( out );
        } // writeTo()
        
        /** 
         * Locate the named directory entry.
         * 
         * @param name is the sub-file name to look for
         * @return the directory entry, or null if not found.
         */
        public DirEntry find( String name )
        {
            for( int i = 0; i < entries.length; i++ ) {
                if( entries[i].name.equals(name) )
                    return entries[i];
            }
            return null;
        }
        
        /** Add an entry to the Directory */
        public void add( DirEntry entry )
        {
            DirEntry[] newEntries = new DirEntry[entries.length + 1];
            System.arraycopy( entries, 0, newEntries, 0, entries.length );
            newEntries[entries.length] = entry;
            entries = newEntries;
        }
        
        /**
         * @return Returns the user version.
         */
        public String getUserVersion()
        {
            return userVersion;
        }

        /**
         * @param userVersion The user-defined version number to set.
         */
        public void setUserVersion(String userVersion)
        {
            this.userVersion = userVersion;
        }

    } // class Directory
    
    /** A single entry in a Directory */
    private class DirEntry
    {
        /** Sub-file name */
        public String name;
        
        /** Absolute file offset of the sub-file's start */
        public int    segOffset;
        
        /** Length of the sub-file */
        public int    segLength;
        
        /** Create an empty directory entry */
        public DirEntry() { }
        
        /** Read a directory entry from a PackedByteBuf */
        public DirEntry( PackedByteBuf buf )
        {
            name      = buf.readString();
            segOffset = buf.readInt();
            segLength = buf.readInt();
        } // constructor

        /** Write a directory entry to a PackedByteBuf */
        public void writeTo( PackedByteBuf buf )
        {
            buf.writeString( name );
            buf.writeInt( segOffset );
            buf.writeInt( segLength );
        } // writeTo()
    } // class DirEntry
    
    /**
     * Regression test to make sure the code works properly. Creates a file in
     * the current directory, then erases it.
     */
    public static final Tester tester = new Tester("StructuredFile") {
        protected void testImpl()
            throws Exception
        {
            File testFile = new File( "test.sf" );
            StructuredFile f = null;
            
            try {

                // Create a file.
                f = StructuredFile.create( testFile );
                f.close();
                f = StructuredFile.open( testFile );
                
                // Add a couple sub-files.
                Subfile sf1 = f.createSubfile( "foo" );
                sf1.writeInt( 1 );
                sf1.writeByte( 2 );
                sf1.writeInt( 3 );
                sf1.close();
                
                Subfile sf2 = f.createSubfile( "foo2" );
                sf2.writeByte( 8 );
                sf2.writeInt( 9 );
                sf2.close();
                
                f.close();
                
                // Verify the sub-files.
                f = StructuredFile.open( testFile );
                sf2 = f.openSubfile( "foo2" );
                sf1 = f.openSubfile( "foo" );
                
                assert sf2.readByte() == 8;
                assert sf1.readInt() == 1;
                assert sf1.readByte() == 2;
                assert sf2.readInt() == 9;
                assert sf1.readInt() == 3;
                
                // Make sure we can't read past the end of either one.
                boolean ok = false;
                try { sf1.readInt(); } catch( EOFException e ) { ok = true; }
                assert ok;
                
                ok = false;
                try { sf2.readByte(); } catch( EOFException e ) { ok = true; }
                assert ok;
                
                // Test seeking.
                sf1.seek( 4 );
                assert sf1.readByte() == 2;
                sf1.seek( 0 );
                assert sf1.readInt() == 1;
                
                ok = false;
                try { sf1.seek(20); } catch( IOException e ) { ok = true; }
                assert ok;
                
                // Make sure we can add another one.
                Subfile sf3 = f.createSubfile( "foo3" );
                sf3.writeInt( 10 );
                sf3.seek( 0 );
                assert sf3.readInt() == 10;
                
                // Can't create two at the same time
                ok = false;
                try { f.createSubfile("foo4"); } catch( IOException e ) { ok = true; }
                assert ok;
                
                sf3.close();
                
                sf3 = f.openSubfile( "foo3" );
                assert sf3.readInt() == 10;
                
                // Shouldn't be able to open a non-existent sub-file.
                ok = false;
                try { f.openSubfile("foo99"); }
                catch( FileNotFoundException e ) { ok = true; }
                assert ok;
            
            }
            finally {
                // All done. Close and clean up our file.
                if( f != null )
                    f.close();
                testFile.delete();
            }
        } // testImpl()
    };

} // class StructuredFile
