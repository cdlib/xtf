package org.cdlib.xtf.textEngine;

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

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;

/**
 * Used to map chunk indexes to the corresponding document index, and 
 * vice-versa.
 * 
 * @author Martin Haye
 */
public class DocNumMap
{
    /** Total number of docInfo chunks found */
    private int nDocs;
    
    /** Array of indexes, one for each docInfo chunk */
    private int[] docNums;
    
    /** Caches result of previous scan, used for speed */
    private int prevNum = -1;
    
    /** Used in binary searching */
    private int low     = -1;
    
    /** Used in binary searching */
    private int high    = -1;
    
    /** 
     * Make a map for the given reader. This reads in all the docInfo chunks
     * to determine the range of text chunks for each document.
     */
    public DocNumMap( IndexReader reader )
        throws IOException
    {
        // Figure out how many entries we'll have, and make our array that big.
        Term term = new Term( "docInfo", "1" );
        nDocs = reader.docFreq( term );
        docNums = new int[nDocs];
            
        // Get a list of all the "header" chunks for documents in this
        // index (i.e., documents with a "docInfo" field.)
        //
        TermDocs docHeaders = reader.termDocs( term );
        
        // Record each document number.
        int i = 0;
        while( docHeaders.next() )
                docNums[i++] = docHeaders.doc();
        nDocs = i; // Account for possibly deleted docs
    } // constructor
    
    
    /**
     * Return a count of the number of documents (not chunks) in the index.
     */
    public final int getDocCount()
    {
        return nDocs;
    }

    /**
     * Given a chunk number, return the corresponding document number that it
     * is part of. Note that like all Lucene indexes, this is ephemeral and
     * only applies to the given reader. If not found, returns -1; this can
     * basically only happen if the chunk number is greater than all document
     * numbers.
     * 
     * @param chunkNumber Chunk number to translate
     * @return Document index, or -1 if no match.
     */
    public final int getDocNum( int chunkNumber )
    {
        // Do a binary search for the chunk
        scan( chunkNumber );
        
        // Return the upper end, since the document info is written after
        // all of its chunks.
        //
        if( high == nDocs )
            return -1;
        return docNums[high];
    } // getDocNum()
    
    
    /**
     * Given a document number, this method returns the number of its first
     * chunk.
     */
    public final int getFirstChunk( int docNum )
    {
        // Scan for the document
        scan( docNum );
        
        // If not found, get out.
        if( low < 0 || docNums[low] != docNum )
            return -1;
        
        if( low == 0 )
            return 1; // Account for index info chunk
        else
            return docNums[low-1] + 1;
        
    } // getFirstchunk()
    
    
    /**
     * Given a document number, this method returns the number of its last
     * chunk.
     */
    public final int getLastChunk( int docNum ) {
        return docNum - 1;
    }

    
    /**
     * Perform a binary search looking for the given number. On exit, the
     * 'low' and 'high' member variables will be indexes into the array that
     * bracket the value.
     * 
     * @param num   The number to look for.
     */
    private void scan( int num ) 
    {
        // Early-out
        if( num == prevNum )
            return;
                
        // Perform a simple binary search.
        int high = nDocs, low = -1, probe;
        while( high - low > 1 )
        {
            probe = (high + low) / 2;
            if( docNums[probe] > num )
                high = probe;
            else
                low = probe;
        }
    
        // At this point, low and high bracket the value searched for.
        assert low == -1     || docNums[low]  <= num;
        assert high == nDocs || docNums[high] >  num;
        
        this.low  = low;
        this.high = high;
    } // scan()
    
} // class DocNumMap
