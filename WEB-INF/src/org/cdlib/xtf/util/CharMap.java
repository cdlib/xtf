package org.cdlib.xtf.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;


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
 * Maintains an in-memory, one-to-one mapping from characters in one set to
 * characters in another. The list is read from a disk file, which may be 
 * sorted or unsorted.
 * 
 * The format of file entries should be one pair per line, separated by a bar
 * ("|") character. The first word is considered the "key", the second is the
 * "value". Each should be a four-digit hex number representing a Unicode
 * code point.
 */
public class CharMap 
{
    /** The mapping of chars. */
    private char[] map = new char[65536];
    
    /** Cache of mapped words */
    private HashMap cache = new HashMap(100);
    
    /** Construct a char map by reading in a file. */
    public CharMap( File f ) 
        throws IOException
    {
        readFile( new BufferedReader(new FileReader(f)) );
    }
    
    /** Construct a char map by reading from an InputStream. */
    public CharMap( InputStream s )
        throws IOException
    {
        readFile( new BufferedReader(new InputStreamReader(s)) );
    }
    
    /** Map the characters in a word and return the mapped resulting word,
     *  or null if no mappings found.
     */
    public synchronized String mapWord( String word )
    {
        // Have we already looked up this word? If so, save time.
        String val = null;
        if( cache.containsKey(word) ) {
            val = (String) cache.get(word);
            return val;
        }
        
        // Map the chars in the word. 
        char[] oldChars = word.toCharArray();
        char[] newChars = new char[ word.length() ];
        boolean anyChanges = false;
        
        for( int i = 0; i < oldChars.length; i++ ) 
        {
            char oldChar = oldChars[i];
            char newChar = oldChars[i];
            
            // Map this character. Note this has to be done repeatedly until
            // we reach a non-mapped char.
            //
            while( map[newChar] != 0 ) {
                newChar = map[newChar];
                anyChanges = true;
            }
            newChars[i] = newChar;
        }
    
        // If no mapped chars were found, record that fact in the cache,
        // and return null.
        //
        if( !anyChanges ) {
            cache.put( word, null );
            return null;
        }

        // Okay, reconstitute the new word and cache it.
        String newWord = new String( newChars );
        cache.put( word, newWord );
        return newWord;

    } // mapWord()
    
    /**
     * Read in the contents of a char file. The file need not be in sorted 
     * order.
     * 
     * @param  reader     Reader to get the data from
     * @throws IOException
     */
    private void readFile( BufferedReader reader ) 
        throws IOException
    {
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
            
            // Break out the two fields. If no bar, skip this line.
            int barPos = line.indexOf( '|' );
            if( barPos < 0 )
                continue;
            
            String key = line.substring(0, barPos).trim();
            String val = line.substring(barPos+1).trim();
            
            if( key.length() == 0 || val.length() == 0 )
                continue;
            
            // Parse the hex codes to character values from 0..65535
            int keyCode = -1;
            int valCode = -1;
            try {
                keyCode = Integer.parseInt( key, 16 );
                valCode = Integer.parseInt( val, 16 );
            }
            catch( NumberFormatException e ) { }
            
            if( keyCode < 0 || keyCode > 65535 ||
                valCode < 0 || valCode > 65535 )
            {
                Trace.warning( "Warning: Invalid key/val char mapping: " +
                    "'" + key + "' -> '" + val + "'" );
                continue;
            }
            
            // Record the entry.
            map[keyCode] = (char)valCode;
        } // while
        
    } // readFile()
    
} // class WordMap
