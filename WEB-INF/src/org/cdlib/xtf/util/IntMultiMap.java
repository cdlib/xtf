package org.cdlib.xtf.util;

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

import java.util.Arrays;

/**
 * This class efficiently implements a "one to many" relationship between
 * integer keys and multiple integer values. The maximum number of keys
 * is fixed at construction time, but the number of values can grow
 * insanely large, without large penalties for resizing arrays, etc.
 * 
 * @author Martin Haye
 */
public class IntMultiMap 
{
  private static final int BLOCK_SIZE = 1024;
  
  private short[] keyBlocks;
  private short[] keyOffsets;
  
  private Block[] blocks   = { new Block() };
  private short   curBlockNum = 0;
  private short   curBlockTop = 0;
  
  /**
   * Initialize the mapping table.
   * 
   * @param maxKey  One larger than the largest key value that will ever be 
   *                passed to {@link #add(int, int)}. Note that the mapping 
   *                cannot be expanded.
   */
  public IntMultiMap( int maxKey )
  {
    keyBlocks  = new short[maxKey];
    Arrays.fill( keyBlocks, (short) -1 );

    keyOffsets = new short[maxKey];
    Arrays.fill( keyOffsets, (short) -1 );
  } // constructor
  
  /**
   * Add a new association between a key and a value. Note that each key is
   * allowed to have multiple values associated with it.
   */
  public void add( int key, int value )
  {
    // Is there room in the current block? If not, make a new one.
    if( curBlockTop == BLOCK_SIZE ) 
    {  
        // Resize the array of blocks if necessary
        ++curBlockNum;
        if( curBlockNum == blocks.length ) {
            Block[] oldBlocks = blocks;
            blocks = new Block[blocks.length * 2];
            if( blocks.length >= 32767 )
                throw new RuntimeException( "Too many blocks - switch from short to int");
            System.arraycopy(oldBlocks, 0, blocks, 0, oldBlocks.length);
        }
    }
    
    // Link in the new entry.
    Block block = blocks[curBlockNum];
    block.blockLinks[curBlockTop]  = keyBlocks[key];
    block.offsetLinks[curBlockTop] = keyOffsets[key];
    block.values[curBlockTop] = value;
    keyBlocks[key] = curBlockNum;
    keyOffsets[key] = curBlockTop;
    ++curBlockTop;
  } // add()
  
  /**
   * Create a new iterator. Note that it can (and should) be re-used, thus 
   * avoiding many object allocations. Also note that this iterator is
   * inextricably linked to this particular map, and will fail mysteriously
   * if used with another map.
   * 
   * @return    A new, uninitialized, iterator.
   */
  public Iterator createIterator()
  {
    return new Iterator();
  }
  
  /**
   * Iterate all the values for the given key.
   * 
   * @param key         Key to get values for
   * @param iterator    Iterator to initialize
   * @return            The same iterator, for easy chaining purposes.
   */
  public Iterator iterateValues( int key, Iterator iterator )
  {
    iterator.init( key );
    return iterator;
  }
  
  /**
   * Keeps track of a block of values, with links to the following values. 
   */
  private class Block
  {
    int[]   values      = new int[BLOCK_SIZE];
    short[] blockLinks  = new short[BLOCK_SIZE]; 
    short[] offsetLinks = new short[BLOCK_SIZE];
  } // class Block
  
  /**
   * Uses to iterate the values for a given key. Can be re-initialized to avoid
   * creating a new iterator for each key.
   */
  private class Iterator
  {
    private int[] values      = new int[1];
    private int   nValues     = 0;
    private int   curValueNum = -1;
    
    private Iterator() { }
    
    private void init( int key )
    {
      // Fetch all the values for the key
      short blockNum = keyBlocks[key];
      short offset   = keyOffsets[key];
      nValues = 0;
      while( blockNum >= 0 ) 
      {
          if( nValues == values.length ) {
              int[] oldValues = values;
              values = new int[values.length * 2];
              System.arraycopy( oldValues, 0, values, 0, oldValues.length );
          }
          
          Block block     = blocks[blockNum];
          values[nValues] = block.values[offset];
          blockNum        = block.blockLinks[offset];
          offset          = block.offsetLinks[offset];
          ++nValues;
      }
      
      // For efficiency, the values as they come out of the linked list are
      // in the reverse order they were added. We start with the last one
      // and go backward, so the client gets them in the order they expect.
      //
      curValueNum = nValues-1;
    }
    
    /**
     * @return  true iff there another value for the current key.
     */
    public final boolean hasNext() {
      return curValueNum >= 0;
    }
    
    /**
     * @return  the next value for the current key
     */
    public final int next() {
      return values[curValueNum--];
    }
  } // class LinkIterator
  
} // class OneToManyInt
