package org.cdlib.xtf.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Map;

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

/**
 * Maintains an in-memory, one-to-one mapping from words in one set to words in
 * another. The list is read from a disk file, which may be sorted or unsorted.
 * The format of file entries should be one pair per line, separated by a bar
 * ("|") character. The first word is considered the "key", the second is the
 * "value".
 */
public class WordMap 
{
    /** Keep a cache of lookups performed to-date */
    private HashMap   cache      = new HashMap( 100 );
    
    /** Map of blocks, keyed by the first word in each block */
    private HashMap   blockMap   = new HashMap( 100 );
    
    /** Sorted list of the block keys, for fast binary searching */
    private ArrayList blockHeads = new ArrayList( 100 );;
    
    /** Construct a word map by reading in a file. */
    public WordMap( File f ) 
        throws IOException
    {
        readFile( new BufferedReader(new FileReader(f)) );
    }
    
    /** Construct a word map by reading from an InputStream. */
    public WordMap( InputStream s )
        throws IOException
    {
        readFile( new BufferedReader(new InputStreamReader(s)) );
    }
    
    /** Look up a word, and return the corresponding value, or null if none. */
    public synchronized String lookup( String word )
    {
        // Have we already looked up this word? If so, save time.
        String val = null;
        if( cache.containsKey(word) ) {
            val = (String) cache.get(word);
            return val;
        }
        
        // Find the appropriate block.
        int blockNum = Collections.binarySearch( blockHeads, word );
        if( blockNum < 0 )
            blockNum = -blockNum - 2;
        if( blockNum < 0 ) {
            cache.put( word, null );
            return null;
        }
        
        // Search that block.
        String prev  = (String) blockHeads.get( blockNum );
        String block = (String) blockMap.get( prev );
        int pos = 0;
        while( pos < block.length() ) {
            int keyShare = block.charAt(pos) - '0';
            int barPos = block.indexOf( '|', pos+1 );
            String key = prev.substring(0, keyShare) +
                         block.substring(pos+1, barPos);
            int end = block.indexOf( '\n', barPos+1 );

            int comp = key.compareTo( word );
            if( comp > 0 )
                break;
            if( comp != 0 ) {
                pos = end + 1;
                if( pos < 0 )
                    break;
                prev = key;
                continue;
            }
            
            int valShare = block.charAt( barPos+1 ) - '0';
            val = key.substring(0, valShare) +
                  block.substring(barPos+2, end);
            cache.put( word, val );
            return val;
        }
        
        // Not found.
        cache.put( word, null );
        return null;
        
    } // lookup()
    
    /**
     * Read in the contents of a word file, forming blocks of 128 entries per
     * block. The file need not be in sorted order.
     * 
     * @param  reader  Reader to get the data from
     * @throws IOException
     */
    private void readFile( BufferedReader reader ) 
        throws IOException
    {
        TreeMap entries  = new TreeMap();
        HashMap randomCheck = new HashMap();
        
        while( true ) {
            String line = reader.readLine();
            if( line == null )
                break;
            
            // Strip off any trailing comment.
            if( line.indexOf("//") >= 0 )
                line = line.substring( 0, line.indexOf("//") );
            if( line.indexOf("#") >= 0 )
                line = line.substring( 0, line.indexOf("#") );
            if( line.indexOf(";") >= 0 )
                line = line.substring( 0, line.indexOf(";") );
            
            // Break out the two fields
            int barPos = line.indexOf( '|' );
            if( barPos < 0 )
                continue;
            
            String key = line.substring(0, barPos).trim();
            String val = line.substring(barPos+1).trim();
            
            if( key.length() == 0 || val.length() == 0 )
                continue;
            
            // Record the entry.
            entries.put( key, val );
        } // while
        
        // Divide the entries into sets of 128, and prefix-encode each block.
        StringBuffer buf = new StringBuffer();
        int nEntries = 0;
        String prev = "";
        String firstKey = "";
        for( Iterator iter = entries.entrySet().iterator(); iter.hasNext(); ) 
        {
            Map.Entry entry = (Map.Entry) iter.next();
            String key = (String) entry.getKey();
            String val = (String) entry.getValue();
            
            if( firstKey.length() == 0 ) {
                firstKey = key;
                prev = firstKey;
            }
         
            // Figure out how many characters the key has in common with
            // the previous one.
            //
            int keyShare;
            for( keyShare = 0; keyShare < key.length(); keyShare++ ) {
                if( keyShare == prev.length() )
                    break;
                if( key.charAt(keyShare) != prev.charAt(keyShare) )
                    break;
            }
            
            // Figure out how many characters the value has in common with
            // the key.
            //
            int valShare;
            for( valShare = 0; valShare < key.length(); valShare++ ) {
                if( valShare == val.length() )
                    break;
                if( key.charAt(valShare) != val.charAt(valShare) )
                    break;
            }
            
            // Now create the entry.
            buf.append( ((char)(keyShare+'0')) + key.substring(keyShare) + '|' +
                        ((char)(valShare+'0')) + val.substring(valShare) + "\n" );
            
            // Record this key for the next time round.
            prev = key;
            
            if( Math.random() < .01 ) {
                randomCheck.put( key, val );
            }
            
            // If we've reached 128 entries in this block, or we've reached
            // the end of the entries, store the block.
            //
            nEntries++;
            if( nEntries == 128 || !iter.hasNext() ) 
            {
                String block = buf.toString();
                blockMap.put( firstKey, block );
                blockHeads.add( firstKey );
                
                // Reset for next block
                nEntries = 0;
                firstKey = prev = "";
                buf.setLength( 0 );
            }
        } // while
        
        // Do some random checks to make sure we set things up correctly.
        for( Iterator iter = randomCheck.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry ent = (Map.Entry) iter.next();
            String result = lookup( (String) ent.getKey() );
            assert result.equals( ent.getValue() );
        }
        assert lookup("a") == null;
        assert lookup("zzzzzz") == null;
        
        // Clear out the random checks from the cache, to start with a nice
        // small memory footprint.
        //
        cache.clear();
        
    } // readFile()
    
} // class WordMap
