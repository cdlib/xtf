package org.cdlib.xtf.util;

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
import java.util.Vector;

/**
 * Implements a simple mapping, from object keys to integers. Each new key
 * is assigned a consecutive number, starting at zero.
 * 
 * @author Martin Haye
 */
public class ConsecutiveMap
{
    /** Mapping used to keep the unique set of keys */
    private HashMap map     = new HashMap( 100 );
    
    /** Vector of all unique keys, in order of addition */
    private Vector  inOrder = new Vector( 100 );
    
    /**
     * If the key is already present in the map, return its assigned number.
     * Otherwise, add it and assign it a new number.
     * 
     * @param key   The key to look up
     * @return      A number associated with that key
     */
    public int put( Object key )
    {
        Integer num = (Integer) map.get(key);
        if( num == null ) {
            num = Integer.valueOf( inOrder.size() );
            inOrder.add( key );
            map.put( key, num );
        }
        
        return num.intValue();
    } // put()
    
    /**
     * Retrieve the namecode for the given key. If not found, returns -1.
     */
    public int get( Object key )
    {
        Object num = map.get(key);
        if( num == null )
            return -1;
        return ((Integer)num).intValue();
    } // get()
    
    /**
     * Check if the given key is present in the map yet.
     */
    public boolean has( Object key )
    {
        return map.get(key) != null;
    } // has()
    
    /**
     * Retrieve an array of all the keys, ordered by consecutive number.
     */
    public Object[] getArray()
    {
        return inOrder.toArray();
    } // getArray()
    
} // class ConsecutiveMap
