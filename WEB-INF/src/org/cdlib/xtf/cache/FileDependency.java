package org.cdlib.xtf.cache;

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

import java.io.File;

/**
 * This class represents a dependency on a given file. The dependency becomes 
 * stale if the file modification time changes after the dependency is created.
 */
public class FileDependency extends Dependency
{
    /** 
     * Constructor - stores the modification date of the file.
     * 
     * @param file  The file to base the dependency on.
     */
    public FileDependency( File file )
    {
        this.file = file;
        this.lastModified = file.lastModified();
    }

    /**
     * Constructor - stores the modification date of the file.
     *
     * @param path  Full path to the file on which to base the dependency.
     */
    public FileDependency( String path )
    {
        this( new File(path) );
    }

    /**
     * Checks if this dependency is still valid.
     *
     * @return  true iff the file modification time is unchanged.
     */
    public boolean validate()
    {
        // We don't have a good way at present to quickly check the last-mod
        // date of a URL. So skip it.
        //
        if( file.getPath().startsWith("http:") )
            return true;

        // If we can still read the file, check the mod time.
        return (file.canRead() && file.lastModified() == lastModified);
    }
    
    /** Make a human-readable representation */
    public String toString()
    {
        return "FileDependency(" + file.toString() + ":" + lastModified;
    }

    /** The file we're tracking */
    private File file;

    /** When the file was modified */
    private long lastModified;

} // class FileDependency

