package org.cdlib.xtf.textIndexer;

import java.util.StringTokenizer;

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
 * Maps accented characters (such as grave and accute accents, umlauts, etc.)
 * to corresponding non-accented characters, to make searching easier for
 * people who can't type the accents. 
 */
public class AccentMapper 
{
    /** This is a mapping of pairs: single accented character to one or two
     *  non-accented characters.
     */
    private static String mapTbl = 
                            "\u00c0A|"  + // Upper-case A with grave
                            "\u00c1A|"  + // Upper-case A with acute
                            "\u00c2A|"  + // Upper-case A with circumflex
                            "\u00c3A|"  + // Upper-case A with tilde
                            "\u00c4A|"  + // Upper-case A with umlaut
                            "\u00c5A|"  + // Upper-case A with ring above
                            "\u00c6AE|" + // Upper-case ligature AE
                            "\u00c7C|"  + // Upper-case C with cedilla
                            "\u00c8E|"  + // Upper-case E with grave
                            "\u00c9E|"  + // Upper-case E with acute
                            "\u00caE|"  + // Upper-case E with circumflex
                            "\u00cbE|"  + // Upper-case E with umlaut
                            "\u00ccI|"  + // Upper-case I with grave
                            "\u00cdI|"  + // Upper-case I with acute
                            "\u00ceI|"  + // Upper-case I with circumflex
                            "\u00cfI|"  + // Upper-case I with umlaut
                            "\u00d0D|"  + // Upper-case Eth
                            "\u00d1N|"  + // Upper-case N with tilde
                            "\u00d2O|"  + // Upper-case O with grave
                            "\u00d3O|"  + // Upper-case O with acute
                            "\u00d4O|"  + // Upper-case O with circumflex
                            "\u00d5O|"  + // Upper-case O with umlaut
                            "\u00d80|"  + // Upper-case O with stroke
                            "\u00d9U|"  + // Upper-case U with grave
                            "\u00daU|"  + // Upper-case U with acute
                            "\u00dbU|"  + // Upper-case U with circumflex
                            "\u00dcU|"  + // Upper-case U with umlaut
                            "\u00ddY|"  + // Upper-case Y with acute
                            "\u00dfss|" + // Sharp-S (German SZ)
                            "\u00e0a|"  + // Lower-case A with grave
                            "\u00e1a|"  + // Lower-case A with acute
                            "\u00e2a|"  + // Lower-case A with circumflex
                            "\u00e3a|"  + // Lower-case A with tilde
                            "\u00e4a|"  + // Lower-case A with umlaut
                            "\u00e5a|"  + // Lower-case A with ring above
                            "\u00e6ae|" + // Lower-case ligature AE
                            "\u00e7c|"  + // Lower-case C with cedilla
                            "\u00e8e|"  + // Lower-case E with grave
                            "\u00e9e|"  + // Lower-case E with acute
                            "\u00eae|"  + // Lower-case E with circumflex
                            "\u00ebe|"  + // Lower-case E with umlaut
                            "\u00eci|"  + // Lower-case I with grave
                            "\u00edi|"  + // Lower-case I with acute
                            "\u00eei|"  + // Lower-case I with circumflex
                            "\u00efi|"  + // Lower-case I with umlaut
                            "\u00f0d|"  + // Lower-case Eth
                            "\u00f1n|"  + // Lower-case N with tilde
                            "\u00f2o|"  + // Lower-case O with grave
                            "\u00f3o|"  + // Lower-case O with acute
                            "\u00f4o|"  + // Lower-case O with circumflex
                            "\u00f5o|"  + // Lower-case O with umlaut
                            "\u00f80|"  + // Lower-case O with stroke
                            "\u00f9u|"  + // Lower-case U with grave
                            "\u00fau|"  + // Lower-case U with acute
                            "\u00fbu|"  + // Lower-case U with circumflex
                            "\u00fcu|"  + // Lower-case U with umlaut
                            "\u00fdy|"  + // Lower-case Y with acute
                            "\u00ffy|"  + // Lower-case Y with umlaut
                            "\u0160S|"  + // Upper-case S with caron
                            "\u0161s|"  + // Lower-case S with caron
                            "\u0152OE|" + // Upper-case ligature OE
                            "\u0153oe|" + // Lower-case ligature OE
                            "\u0178Y|"  + // Upper-case Y with umlaut
                            "\u017dZ|"  + // Upper-case Z with caron
                            "\u017ez|"  + // Lower-case Z with caron
                            "";
              
    /** Max character that we are mapping from (keeps the table small) */
    private static final char maxChar = '\u0200';
    
    /** Lookup table, mapping accented character to an un-accented string */
    private static String[] charMap = new String[maxChar];
    
    // Initialize the character map
    static {
        StringTokenizer t = new StringTokenizer( mapTbl, "|" );
        while( t.hasMoreTokens() ) {
            String token = t.nextToken();
            char c = token.charAt( 0 );
            String trans = token.substring( 1 );
            assert trans.length() >= 1 && trans.length() <= 2;
            charMap[c] = trans;
        }
    }
    
    /**
     * Map all the accents within the word to corresponding non-accented
     * characters.
     * 
     * @param word  The word to map
     * @return      Corresponding mapped word (might be unchanged)
     */
    public static String map( String word )
    {
        // First, check if *any* character needs mapping. Most words don't
        // need mapping, so this is usually a quick out.
        //
        int i;
        for( i = 0; i < word.length(); i++ ) {
            char c = word.charAt( i );
            if( c < maxChar && charMap[c] != null )
                break;
        }
        if( i < word.length() )
            return word;
        
        // Okay, we have work to do. Map each character, assembling a new word.
        StringBuffer newWord = new StringBuffer( word.length() * 2 );
        for( i = 0; i < word.length(); i++ ) {
            char c = word.charAt( i );
            if( c < maxChar && charMap[c] != null )
                newWord.append( charMap[c] );
            else
                newWord.append( c );
        }
        
        // All done.
        return newWord.toString();
        
    } // map()
} // class AccentMapper
