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
 * A cache that holds key/value pairs. The value is specifed when a key is
 * added to the cache.
 */
public class SimpleCache extends Cache
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
    public SimpleCache( int maxEntries, int maxTime )
    {
        super( maxEntries, maxTime );
    }


    /**
     * Set the value for a key. If already present, the old value is replaced.
     *
     * @param key   Key to set the value for
     * @param value Value for that key.
     */
    public void set( Object key, Object value )
    {
        set( key, value, null );
    }


    /**
     * Set the value for a key, optionally adding a dependency for it.  
     * If the key is already present, the old value is replaced.
     *
     * @param key           The key that will be used to look up the value
     * @param value         The value to associate with that key
     * @param dependency    A dependency to add to the key, or null for none.
     */
    public synchronized void set( Object key, 
                                  Object value, 
                                  Dependency dependency )
    {
        ListEntry entry;

        // If we already have this key, replace the value.
        if( has(key) ) {
            entry = (ListEntry) keyMap.get( key );
            entry.value = value;
            entry.dependencies.clear();
            if( dependency != null )
                entry.dependencies.add( dependency );
            entry.setTime = System.currentTimeMillis();
            logAction( "Replaced", key, value );
            return;
        }

        // Otherwise, add a new entry.
        entry = new ListEntry();
        entry.key   = key;
        entry.value = value;
        if( dependency != null )
            entry.dependencies.add( dependency );

        // Add it to the age list (at the tail, since it's the most recently
        // used).
        entry.lastUsedTime = entry.setTime = 
            System.currentTimeMillis();
        ageList.addTail( entry );

        // Add it to the key map and log the action.
        keyMap.put( key, entry );
        logAction( "Added", key, value );

        // Since we've modified the age list, clean up if necessary.
        cleanup();

    } // set()


    /**
     * Gets the value associated with a key, or null if none.
     *
     * @param key       The key to look for
     * @return          The value for that key, or null if the key isn't
     *                  in the cache.
     */
    public synchronized Object get( Object key )
    {
        if( has(key) )
            return ((ListEntry)keyMap.get(key)).value;
        else
            return null;

    } // get()


    /** 
     * Add a dependency to an existing entry. If the dependency later becomes
     * invalid, the key will be removed from the cache.
     *
     * @param key       The key to add a dependency to
     * @param d         The dependency to add to it.
     */
    public synchronized void addDependency( Object key, Dependency d )
    {
        if( !has(key) )
            return;
        ((ListEntry)keyMap.get(key)).dependencies.add( d );

    } // addDependency()

} // class SimpleCache


