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

/**
 * Utility class that decodes Base64 data.
 */
public class Base64
{
    /** 
     * Given a character in the Base64 set, figure out the decimal
     * equivalent.
     *
     * @param c     The character to decode
     * @return      The decimal equivalent (also maps invalid chars to zero)
     */
    private static int decodeChar( char c )
    {
        if( c >= 'A' && c <= 'Z' )
            return c - 'A';
        else if( c >= 'a' && c <= 'z' )
            return c - 'a' + 26;
        else if( c >= '0' && c <= '9' )
            return c - '0' + 52;
        else if( c == '+' )
            return 62;
        else if( c == '/' )
            return 63;
        else
            return 0;

    } // decodeChar()


    /**
     * Combines bits from two different bytes into a single character.
     */
    private static String decodeBits( int bits1, int pos1, int count1,
                                      int bits2, int pos2, int count2 )
    {
        int num1 = (bits1 >> pos1) & (0xFF >> (8-count1));
        int num2 = (bits2 >> pos2) & (0xFF >> (8-count2));
        char c = (char) ((num1 << count2) + num2);
        return Character.toString( c );

    } // decodeBits()


    /**
     * Decodes a 4-character Base64 'quantum' into a 3-character string.
     */
    private static String decodeQuantum( String quantum )
    {
        int[] bits = new int[4];
        for( int i = 0; i < 4; i++ )
            bits[i] = decodeChar( quantum.charAt(i) );

        String ch1 = decodeBits( bits[0], 0, 6, bits[1], 4, 2 );
        String ch2 = decodeBits( bits[1], 0, 4, bits[2], 2, 4 );
        String ch3 = decodeBits( bits[2], 0, 2, bits[3], 0, 6 );
        if( quantum.endsWith("==") )
            return ch1;
        else if( quantum.endsWith("=") )
            return ch1 + ch2;
        else
            return ch1 + ch2 + ch3;

    } // decodeQuantum()


    /**
     * Decodes a full Base64 string to the corresponding normal string.
     *
     * @param base64    The base64 string to decode (e.g. "HX1+9/6fE97=")
     * @return          Decoded version of the string.
     */
    public static String decodeString( String base64 )
    {
        String out = "";
        while( base64.length() >= 4 ) {
            String quantum = base64.substring( 0, 4 );
            base64 = base64.substring( 4 );
            String str = decodeQuantum( quantum );
            out += str;
        }

        return out;

    } // decodeString()

} // class Base64


