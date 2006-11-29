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
 * Various handy functions for working with arrays.
 * 
 * @author Martin Haye
 */
public class ArrayUtil
{
  // Functions for int arrays
    
  public static int[] resize( int[] in, int newSize ) {
      int[] out = new int[newSize];
      System.arraycopy( in, 0, out, 0, Math.min(in.length, newSize) );
      return out;
  }
  
  public static int[] expand( int[] in ) {
      return resize( in, in.length * 3 / 2 );
  }
  
  // Functions for long arrays
  
  public static long[] resize( long[] in, int newSize ) {
      long[] out = new long[newSize];
      System.arraycopy( in, 0, out, 0, Math.min(in.length, newSize) );
      return out;
  }
  
  public static long[] expand( long[] in ) {
      return resize( in, in.length * 3 / 2 );
  }
  
  // Functions for float arrays
  
  public static float[] resize( float[] in, int newSize ) {
      float[] out = new float[newSize];
      System.arraycopy( in, 0, out, 0, Math.min(in.length, newSize) );
      return out;
  }
  
  public static float[] expand( float[] in ) {
      return resize( in, in.length * 3 / 2 );
  }
  
  // Functions for String arrays
  
  public static String[] resize( String[] in, int newSize ) {
      String[] out = new String[newSize];
      System.arraycopy( in, 0, out, 0, Math.min(in.length, newSize) );
      return out;
  }
  
  public static String[] expand( String[] in ) {
      return resize( in, in.length * 3 / 2 );
  }
  
} // class ArrayUtil
