package org.cdlib.xtf.util;


/*
 * Copyright (c) 2005, Regents of the University of California
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
 * A fast but inflexible cache where the keys are integers, the size
 * is fixed, and a crude LRU policy is enforced. Handles consecutive keys
 * gracefully. Doesn't support resizing, deletion, or iteration (not that these
 * operations would be hard, just that they haven't been needed so far.)
 *
 * @author Martin Haye
 */
public class FastIntCache 
{
  private int size;
  private IntHash oldHash;
  private IntHash newHash;

  /**
   * Construct a new cache. Basically, two hash tables are used, each with
   * the given capacity. When one fills up, the other is thrown out. A
   * Least-Recently-Used policy is effected by migrating entries from the old
   * hash to the new hash when they are accessed through get().
   *
   * @param size  How large to make each of the two internal hash tables.
   */
  public FastIntCache(int size) {
    this.size = size;
    clear();
  } // FastCache()

  /** Clears all entries from the cache */
  public void clear() {
    oldHash = new IntHash(1);
    newHash = new IntHash(size);
  }

  /** Check whether the given key is present in the cache */
  public boolean contains(int key) {
    return newHash.contains(key) || oldHash.contains(key);
  }

  /** Retrieve the value for the given key, or null if not found. */
  public Object get(int key) 
  {
    // First, check the new hash
    Object retVal = newHash.get(key);
    if (retVal == null) 
    {
      // Darn. Check the old hash. If it's there, put it into the new hash
      // since it's been used recently. We don't need to worry about 
      // deleting the old entry, as it will disappear eventually when the 
      // hashes are swapped.
      //
      retVal = oldHash.get(key);
      if (retVal != null)
        put(key, retVal);
    }

    // All done.
    return retVal;
  } // get()

  /**
   * Add a key/value pair to the cache. May result in pushing older items
   * out of the cache.
   */
  public void put(int key, Object val) 
  {
    // If the new hash is full, swap and create a new empty hash.
    if (newHash.size() >= size) {
      oldHash = newHash;
      newHash = new IntHash(size);
    }

    // Now that we're sure there's room, put it in the new hash.
    newHash.put(key, val);
  } // put()

  /**
   * Basic regression test
   */
  public static final Tester tester = new Tester("FastIntCache") 
  {
    protected void testImpl() 
    {
      IntHash.tester.test();

      FastIntCache cache = new FastIntCache(3);

      cache.put(1, "a");
      cache.put(2, "b");
      cache.put(3, "c");
      assert cache.contains(1);
      assert cache.contains(2);
      assert cache.contains(3);

      cache.put(4, "d");
      assert cache.get(2).equals("b");
      cache.put(5, "e");
      cache.put(6, "f");
      assert !cache.contains(1);
      assert cache.contains(2);
      assert cache.contains(5);
      assert cache.contains(6);
    } // testImpl()
  };
} // class FastIntCache
