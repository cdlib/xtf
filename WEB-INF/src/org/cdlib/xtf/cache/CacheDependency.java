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
 * CacheDependency represents a dependency on an entry of the same or another
 * cache. If the cache entry changes, disappears, or its dependencies become 
 * stale, then this dependency also becomes stale.
 */
public class CacheDependency extends Dependency
{
    /**
     * Constructor.
     *
     * @param cache     The cache containing the entry to depend on
     * @param key       Key value to depend on within that cache.
     */
    public CacheDependency( Cache cache, Object key )
    {
        this.cache = cache;
        this.key   = key;
        this.lastSet = cache.lastSet( key );
    }

    /**
     * Checks if the dependency is still valid. If the cache entry has changed,
     * disappeared, or has invalid dependencies, then this dependency is stale.
     *
     * @return  true iff the dependency is still fresh.
     */
    public boolean validate()
    {
        return (cache.has(key) && 
                cache.lastSet(key) == lastSet &&
                cache.dependenciesValid(key) );
    }

    /** The cache we're depending on */
    public Cache  cache;

    /** The key within that cache we're depending on */
    public Object key;

    /** The set time of the cache entry when this dependency was created. */
    public long   lastSet;

} // class CacheDependency


