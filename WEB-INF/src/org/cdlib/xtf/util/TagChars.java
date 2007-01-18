package org.cdlib.xtf.util;

/*
 * Copyright (c) 2006, Regents of the University of California
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
 * 
 * Acknowledgements:
 * 
 * A significant amount of new and/or modified code in this module
 * was made possible by a grant from the Andrew W. Mellon Foundation,
 * as part of the Melvyl Recommender Project.
 */

/**
 * Class that represents a slice of a block, for quick access to byte-level
 * strings without object allocation.
 */
@SuppressWarnings("cast")
public class TagChars
{
  int    length;
  byte[] block;
  int    offset;
  
  public final char charAt(int index) {
    return (char) ((int)block[offset+index] & 0xff);
  }

  public final int length() {
    return length;
  }
  
  public final int indexOf( char c ) {
    for( int i = 0; i < length; i++ ) {
        if( block[offset+i] == c )
            return i;
    }
    return -1;
  }
  
  /** Determines how many characters match at the start of two sequences */
  public final int prefixMatch( TagChars other )
  {
    int minLength = Math.min( length, other.length );
    int i;
    for( i = 0; i < minLength; i++ ) {
        if( block[offset+i] != other.block[other.offset+i] )
            break;
    }
    return i;
  }

  public final String toString()
  {
    char[] chars = new char[length];
    for( int i = 0; i < length; i++ )
      chars[i] = charAt(i);
    return new String(chars);
  }
}