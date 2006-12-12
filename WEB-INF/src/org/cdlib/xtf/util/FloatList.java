package org.cdlib.xtf.util;

import java.util.Arrays;

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

// A fast, array-based, expandable list of floats
public class FloatList
{
  private float[] data;
  private int     size = 0;
  
  public FloatList() {
      this( 10 );
  }
  
  public FloatList( int initialCapacity ) {
      data = new float[initialCapacity];
  }
  
  public final void add( float value ) {
      if( size == data.length )
          data = ArrayUtil.expand( data );
      data[size++] = value;
  }
  
  public final void ensureCapacity( int cap ) {
      if( cap > data.length )
          data = ArrayUtil.resize( data, cap );
  }
  
  public final void compact() {
      if( size != data.length )
          data = ArrayUtil.resize( data, size );
  }
  
  public final void resize( int newSize ) {
      if( newSize != size ) {
          data = ArrayUtil.resize( data, newSize );
          if( newSize > size )
              Arrays.fill( data, size, newSize, 0.0f );
      }
  }
  
  public final float[] toArray() {
      float[] ret = new float[size];
      System.arraycopy( data, 0, ret, 0, size );
      return ret;
  }
  
  public final boolean isEmpty() { return size == 0; }
  
  public final void clear() { size = 0; }
  
  public final int size() { return size; }
  
  public final float get(int index) { return data[index]; }
  
  public final float getLast() { return data[size-1]; }
  
  public final void set( int index, float value ) {
      data[index] = value;
  }

  public final void fill( float value ) {
      Arrays.fill( data, value );
  }
  
  public final void sort() {
      compact();
      Arrays.sort( data );
  }
  
  public final int binarySearch( float searchFor ) {
      sort();
      return Arrays.binarySearch( data, searchFor );
  }
  
} // class FloatList
