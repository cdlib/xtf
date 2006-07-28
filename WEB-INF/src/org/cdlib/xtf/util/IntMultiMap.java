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
  private int[]   keyLinks;
  
  private static final int BLOCK_SIZE = 32760;
  private Block[] blocks = { new Block() };
  private short   curBlockNum = 0;
  private Block   curBlock = blocks[0];
  private short   curBlockTop = 0;

  /**
   * Calculate the size in bytes of the major structures of the map.
   * 
   * @return    Approximate size in bytes
   */
  public int byteSize()
  {
    return (blocks.length * 8) +
           ((curBlockNum+1) * BLOCK_SIZE * 8);
  }
  
  /**
   * Initialize the mapping table.
   * 
   * @param maxKey  One larger than the largest key value that will ever be 
   *                passed to {@link #add(int, int)}. Note that the mapping 
   *                cannot be expanded.
   */
  public IntMultiMap(int maxKey) {
    keyLinks = new int[maxKey];
    Arrays.fill(keyLinks, (short) -1);
  } // constructor

  /**
   * Add a new association between a key and a value. Note that each key is
   * allowed to have multiple values associated with it.
   */
  public void add(int key, int value)
  {
    // Is there room in the current block? If not, make a new one.
    if (curBlockTop == BLOCK_SIZE) 
    {
      ++curBlockNum;
      if ((curBlockNum & 0x8000) != 0)
        throw new RuntimeException("Too many blocks - increase block size");
      
      // Resize the array of blocks if necessary
      if (curBlockNum == blocks.length) {
        Block[] oldBlocks = blocks;
        blocks = new Block[blocks.length * 2];
        System.arraycopy(oldBlocks, 0, blocks, 0, oldBlocks.length);
      }

      // Allocate the new block.
      curBlock = blocks[curBlockNum] = new Block();
      curBlockTop = 0;
    }

    // Link in the new entry.
    curBlock.links[curBlockTop] = keyLinks[key];
    curBlock.values[curBlockTop] = value;
    keyLinks[key] = (((int) curBlockNum) << 16) | curBlockTop;
    ++curBlockTop;
  } // add()
  
  /**
   * For iteration: get the first position for the given key, or -1 if it has none.
   */
  public final int firstPos( int key ) {
    return keyLinks[key];
  }
  
  /**
   * For iteration: get the next position after the given pos, or -1 if we're at
   * the end of the chain.
   */
  public final int nextPos( int prevPos ) {
    return blocks[(int)prevPos >> 16].links[(int)prevPos & 0x7FFF];
  }

  /**
   * Retrieve the value for a given link.
   */
  public final int getValue( int pos ) {
    return blocks[(int)pos >> 16].values[(int)pos & 0x7FFF];
  }

  /**
   * Keeps track of a block of values, with links to the following values. 
   */
  private class Block
  {
    int[] values = new int[BLOCK_SIZE];
    int[] links = new int[BLOCK_SIZE];

    Block() {
      Arrays.fill(values, Integer.MIN_VALUE);
      Arrays.fill(links, -1);
    }
  } // class Block

  /**
   * Basic regression test
   */
  public static final Tester tester = new Tester("IntMultiMap") {
    protected void testImpl()
    {
      IntMultiMap map = new IntMultiMap(10);

      map.add(1, 10);
      map.add(1, 11);

      map.add(2, 20);
      map.add(2, 21);
      map.add(2, 22);

      int pos;
      pos = map.firstPos(1);
      assert pos >= 0;
      assert map.getValue(pos) == 11;
      pos = map.nextPos(pos);
      assert pos >= 0;
      assert map.getValue(pos) == 10;
      pos = map.nextPos(pos);
      assert pos < 0;

      pos = map.firstPos(2);
      assert pos >= 0;
      assert map.getValue(pos) == 22;
      pos = map.nextPos(pos);
      assert pos >= 0;
      assert map.getValue(pos) == 21;
      pos = map.nextPos(pos);
      assert pos >= 0;
      assert map.getValue(pos) == 20;
      pos = map.nextPos(pos);
      assert pos < 0;
    } // testImpl()
  };

} // class OneToManyInt
