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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import org.cdlib.xtf.util.EmbeddedList;
import org.cdlib.xtf.util.LinkableImpl;

/**
 * Cache is an abstract class used for code shared by SimpleCache and
 * GeneratingCache. Contains the workhorse functions for maintaining
 * a cache, expiring entries based on age or count, checking dependencies,
 * and checking for a key.
 */
public abstract class Cache<K,V> 
{
  /**
   * Constructor - sets up the parameters of the cache.
   *
   * @param maxEntries    Number of entries allowed. If additional entries
   *                      are created, older ones will be removed. Zero
   *                      means no limit.
   * @param maxTime       Time (in seconds) that a cache entry will remain.
   *                      If the entry hasn't been used in that time, it
   *                      is removed from the cache. Zero means no time
   *                      limit.
   */
  public Cache(int maxEntries, int maxTime) 
  {
    this.maxEntries = maxEntries;
    this.maxTime = maxTime;

    clear();
  }

  /**
   * Checks if the cache currently contains an entry for the given key.
   * If an entry exists but has stale dependencies, it is removed and
   * false is returned. Otherwise, if one exists it is freshened (i.e. its
   * expiration countdown is reset).
   *
   * @param key   The key to look for.
   * @return      true iff the key has a valid entry in the cache.
   */
  public synchronized boolean has(K key) 
  {
    cleanup();

    if (keyMap.containsKey(key)) 
    {
      ListEntry entry = (ListEntry)keyMap.get(key);

      // If dependency checks pass, freshen the entry and return.
      if (dependenciesValid(key)) {
        entry.lastUsedTime = System.currentTimeMillis();
        ageList.moveToTail(entry);
        cleanup();
        return true;
      }
      else {
        ageList.remove(entry);
        keyMap.remove(key);
        logAction("Removed (stale dependencies)", key, entry.value);
        cleanup();
      }
    }

    return false;
  } // has()

  /**
   * Gets the time the entry for the given key was created, or zero if the
   * key isn't present. The time is number of milliseconds since the epoch,
   * just like System.currentTimeMillis().
   *
   * @param key   The key to look for
   * @return      The time (in milliseconds from the epoch) that the entry
   *              was created, or zero if not present.
   */
  public synchronized long lastSet(K key) {
    ListEntry ent = (ListEntry)keyMap.get(key);
    return (ent == null) ? 0 : ent.setTime;
  } // lastSet()

  /**
   * Check the dependencies of a cache entry, if present.
   *
   * @param key   The key to check
   * @return      true iff the cache entry for the key is still valid.
   */
  public synchronized boolean dependenciesValid(K key) 
  {
    cleanup();

    ListEntry ent = (ListEntry)keyMap.get(key);
    if (ent == null)
      return false;

    Iterator i = ent.dependencies.iterator();
    while (i.hasNext()) {
      Dependency d = (Dependency)i.next();
      if (!d.validate())
        return false;
    }

    return true;
  } // dependenciesValid()

  /**
   * Get the list of dependencies for a cache entry, if present.
   *
   * @param key   The key to check
   * @return      An iterator that will produce each dependency, or
   *              null if no dependencies.
   */
  public synchronized Iterator getDependencies(K key) 
  {
    cleanup();

    ListEntry ent = (ListEntry)keyMap.get(key);
    return (ent == null) ? new NullIterator() : ent.dependencies.iterator();
  } // getDependencies()

  /**
   * Remove an entry from the cache.
   *
   * @param key   The key to look up
   * @return      The value that was held for the key, or null if not found.
   */
  public synchronized V remove(K key) 
  {
    cleanup();

    // If we have the key, remove it and return the object.
    if (keyMap.containsKey(key)) {
      ListEntry entry = (ListEntry)keyMap.get(key);
      ageList.remove(entry);
      keyMap.remove(key);
      logAction("Removed", key, entry.value);
      cleanup();
      return entry.value;
    }

    // Not found? Let the caller know.
    return null;
  } // remove()

  /**
   * Remove all entries from the cache.
   */
  public synchronized void clear() 
  {
    // Clear the list and map
    ageList = new EmbeddedList();
    keyMap = new HashMap(maxEntries);
  } // clear()

  /** Tells how many entries are currently cached */
  public synchronized int size() {
    return keyMap.size();
  }

  /**
   * Maintains the maxEntries and maxTime constraints imposed on the cache.
   * Schedules additional cleanup when necessary.
   */
  protected synchronized void cleanup() 
  {
    // Do we have a size constraint?
    if (maxEntries >= 0) 
    {
      // Remove entries until we meet the maxEntries restriction.
      while (ageList.getCount() > maxEntries) {
        ListEntry ent = (ListEntry)ageList.removeHead();
        logAction(
          "Expired to maintain max # cache entries... was " +
          (ageList.getCount() + 1) + ", must be <= " + maxEntries,
          ent.key,
          ent.value);
        keyMap.remove(ent.key);
      }
    }

    // Do we have a time constraint?
    if (maxTime > 0) 
    {
      // Remove entries older than the max time.
      long maxTimeMillis = maxTime * 1000;
      long expireTime = System.currentTimeMillis() - maxTimeMillis;
      while (ageList.getCount() > 0 &&
             ((ListEntry)ageList.getHead()).lastUsedTime < expireTime) 
      {
        ListEntry ent = (ListEntry)ageList.removeHead();
        logAction(
          "Expired due to over-age... age is " +
          ((System.currentTimeMillis() - ent.lastUsedTime) / 1000) +
          " sec, must be < " + maxTime + " sec.",
          ent.key,
          ent.value);
        keyMap.remove(ent.key);
      }
    }
  } // cleanup()

  /**
   * Derived classes can override this method to print out log messages
   * when significant things happen (entries are added, removed, expired,
   * etc.)
   *
   * @param action    What happened ("Added", "Removed", etc.)
   * @param key       The key involved in the action
   * @param value     The value involved in the action
   */
  protected void logAction(String action, K key, V value) {
  }

  /** Used to return an iterator that does nothing */
  protected class NullIterator implements Iterator 
  {
    public boolean hasNext() {
      return false;
    }

    public Object next() {
      return null;
    }

    public void remove() {
    }
  } // class NullIterator()

  /** An entry in the age list maintained by the cache */
  protected class ListEntry extends LinkableImpl 
  {
    /** The key being tracked */
    K key;

    /** The generated or set value for that key */
    V value;

    /** The time (millis since epoch) since the entry was used */
    long lastUsedTime;

    /** The time (millis since epoch) the entry was created */
    long setTime;

    /** Things this entry depends on */
    LinkedList dependencies = new LinkedList();
  } // class ListEntry

  /** Maximum number of entries the cache may contain */
  private int maxEntries;

  /**
   * Maximum amount of time (in seconds) an entry can stay in the cache
   * without being used.
   */
  private int maxTime;

  /** Maintains a mapping of key to ListEntry, for fast key lookups */
  protected HashMap<K,ListEntry> keyMap;

  /**
   * A list, kept sorted by descending age, of all the entries. This is
   * used to find the least-recently-used entry to remove when the cache
   * constraints (time or # of entries) are exceeded.
   */
  protected EmbeddedList ageList;
} // class Cache
