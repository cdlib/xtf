package org.cdlib.xtf.cache;


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
 * A cache that generates an entry if one isn't found. The generate()
 * method must be supplied by the derived class.
 */
public abstract class GeneratingCache extends Cache 
{
  /**
   * Constructor - sets up the parameters of the cache.
   *
   * @param maxEntries    Maximum # of entries. Beyond this, older ones
   *                      will be removed. Zero means no limit.
   * @param maxTime       Time (in seconds) an entry can stay in the cache
   *                      without being used. Entries older than this will
   *                      be removed. Zero means no limit.
   */
  public GeneratingCache(int maxEntries, int maxTime) {
    super(maxEntries, maxTime);
  }

  /**
   * Check the cache for an entry matching the given key. If not found,
   * one is generated.
   *
   * @param key   The key to look up
   * @return      Value corresponding to that key. Never null.
   */
  public synchronized Object find(Object key)
    throws Exception 
  {
    // If we have already generated the value for this key, freshen the
    // entry and return it.
    //
    if (has(key)) {
      ListEntry entry = (ListEntry)keyMap.get(key);
      return entry.value;
    }

    // Otherwise, create an entry and generate a value for it.
    curEntry = new ListEntry();
    curEntry.key = key;
    curEntry.value = generate(key);

    // Add it to the age list (at the tail, since it's the most recently
    // used).
    curEntry.lastUsedTime = curEntry.setTime = System.currentTimeMillis();
    ageList.addTail(curEntry);

    // Add it to the key map and log the action.
    keyMap.put(key, curEntry);
    logAction("Generated", key, curEntry.value);

    // Clear the current entry to prevent any future refs to it.
    Object value = curEntry.value;
    curEntry = null;

    // Since we've modified the age list, clean up if necessary.
    cleanup();

    // And return the generated value.
    return value;
  } // find()

  /**
   * Can be called by the generate() method to add a dependency to the
   * key being generated.
   *
   * @param d     The dependency to add
   */
  public void addDependency(Dependency d) {
    assert curEntry != null : "addDependency() may only be called from within generate()";
    curEntry.dependencies.add(d);
  }

  /**
   * Called when find() fails to locate an entry for the given key. This
   * method must be supplied by the derived class, and must produce a value
   * for the key, or throw an exception if it can't.
   *
   * @param   key         The key to generate a value for.
   * @return              The value for that key
   * @throws Exception    If a value cannot be generated for any reason.
   */
  protected abstract Object generate(Object key)
    throws Exception;

  /** The entry being generated */
  private ListEntry curEntry;
} // class GeneratingCache
