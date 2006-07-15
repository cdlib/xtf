package org.cdlib.xtf.util;

import java.util.Arrays;

/**
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
 */

// A fast, array-based, expandable list of ints
public class IntList
{
  private int[] data;
  private int   size = 0;
  
  public IntList() {
      this( 10 );
  }
  
  public IntList( int initialCapacity ) {
      data = new int[initialCapacity];
  }
  
  public final void add( int value ) {
      if( size == data.length )
          data = ArrayUtil.expand( data );
      data[size++] = value;
  }
  
  public final void compact() {
      if( size != data.length )
          data = ArrayUtil.resize( data, size );
  }
  
  public final boolean isEmpty() { return size == 0; }
  
  public final void clear() { size = 0; }
  
  public final int size() { return size; }
  
  public final int get(int index) { return data[index]; }
  
  public final int getLast() { return data[size-1]; }
  
  public final void set( int index, int value ) {
      data[index] = value;
  }
  
  public final void fill( int value ) {
      Arrays.fill( data, value );
  }
  
  public final void sort() {
      compact();
      Arrays.sort( data );
  }
  
  public final int binarySearch( int searchFor ) {
      compact();
      return Arrays.binarySearch( data, searchFor );
  }
  
} // class IntList
