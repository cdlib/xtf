package org.cdlib.xtf.dynaXML;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.cdlib.xtf.textEngine.IdxConfigUtil;
import org.cdlib.xtf.util.DocTypeDeclRemover;
import org.cdlib.xtf.util.StructuredFile;
import org.cdlib.xtf.util.StructuredStore;
import org.xml.sax.InputSource;

/*
 * This file created on Mar 11, 2005 by Martin Haye
 */

/**
 * Provides local filesystem-based access to lazy and non-lazy versions of
 * a source XML document.
 * 
 * @author Martin Haye
 */
public class DefaultDocLocator implements DocLocator 
{
    /**
     * Search for a StructuredStore containing the "lazy" or persistent
     * representation of a given document. Index parameters are specified,
     * since often the lazy file is stored along with the index. This method
     * is called first, and if it returns null, then 
     * {@link #getInputSource(String)} will be called as a fall-back.
     * 
     * @param indexConfigPath Path to the index configuration file
     * @param indexName       Name of the index being searched
     * @param sourcePath      Path to the source document
     * 
     * @return                Store containing the tree, or null if none
     *                        could be found.
     */
    public StructuredStore getLazyStore( String indexConfigPath, 
                                         String indexName,
                                         String sourcePath ) 
        throws IOException 
    {
        // If no 'index' specified in the docInfo, then there's no way we can
        // find the lazy file.
        //
        boolean useLazy = true;
        if( indexConfigPath == null || indexName == null )
            return null;
        
        // If the source isn't a local file, we also can't use a lazy file.
        if( sourcePath.startsWith("http:") )
            return null;
        if( sourcePath.startsWith("https:") )
            return null;

        File lazyFile = null;
            
        // Figure out where the lazy file is, and make sure it's actually
        // there and that we can read it.
        //
        lazyFile = IdxConfigUtil.calcLazyPath( 
                          new File(DynaXML.getRealPath("")),
                          new File(indexConfigPath), indexName,
                          new File(sourcePath), false );
        if( !lazyFile.canRead() )
            return null;
        
        // Cool. Open the lazy file.
        return StructuredFile.open( lazyFile );
        
    } // getLazyStore()

    /**
     * Retrieve the data stream for an XML source document. 
     * 
     * @param sourcePath  Path to the source document
     * 
     * @return            Data stream for the document.
     */
    public InputSource getInputSource( String sourcePath ) 
        throws IOException 
    {
        // If it's non-local, load the URL.
        if( sourcePath.startsWith("http:") ||
            sourcePath.startsWith("https:") )
        {
            return new InputSource( sourcePath );
        }
        
        // Okay, assume it's a local file.
        InputStream inStream = new FileInputStream( sourcePath );
        
        // Remove DOCTYPE declarations, since the XML reader will barf 
        // if it can't resolve the entity reference, and we really 
        // don't care one way or the other.
        //
        inStream = new DocTypeDeclRemover( inStream );
        
        // Make the input source, and give it a real system ID.
        InputSource inSrc = new InputSource( inStream );
        inSrc.setSystemId( new File(sourcePath).toURL().toString() );
        
        // All done!
        return inSrc;
    } // getInputSource()

} // class DefaultDocLocator
