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
 * Utlity class for finding prime numbers (useful for making hash tables).
 * 
 * @author Martin Haye
 */
public class Prime
{
    /** 
     * Determines the least prime number greater than n. Useful for sizing
     * the hash table so that modulo arithmetic produces good results.
     */
    public static int findAfter( int n )
    {
        int i, j;
        
        if( n < 2 )
            n = 2;
        
        int arraySize = n * 2;
        byte array[] = new byte[arraySize];
        
        for( i = 2; i < n; i++ ) {
            if( array[i] == 1 )
                continue;
            for( j = i*2; j < arraySize; j += i )
                array[j] = 1;
        }
        
        for( i = n+1; i < arraySize && array[i] != 0; i++ )
            ;
        assert i < arraySize : "Sieve algorithm incorrect";
        
        return i;
    } // findPrimeAfter

    // Perform a basic regression test on the Prime class.
    public static final Tester tester = new Tester("Prime") {
        public void testImpl() {
            assert findAfter(2) == 3;
            assert findAfter(5) == 7;
            assert findAfter(65) == 67;
            assert findAfter(241) == 251;
            assert findAfter(505) == 509;
            assert findAfter(7908) == 7919;
        }
    };
        
} // class Prime
