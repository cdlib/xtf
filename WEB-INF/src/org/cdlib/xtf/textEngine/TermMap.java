package org.cdlib.xtf.textEngine;

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

import org.apache.lucene.index.Term;

/**
 * This is a quick-to-access map of terms, used to identify words that should
 * be highlighted in a snippet. It also takes care of the final enforcement of
 * the termLimit.
 * 
 * @author Martin Haye
 */
public class TermMap implements Cloneable
{
    /** Maps terms to fields */
    private HashMap hashMap = new HashMap();
    
    /** Limit on the total number of terms allowed */ 
    private int termLimit = -1;
    
    /** Current number of terms in the map */
    private int nTerms    = 0;
    
    /** Construct a map and specify a limit on the total # of terms */
    public TermMap( int termLimit ) {
        this.termLimit = termLimit;
        nTerms = 0;
    }
    
    /** Obtains an exact, independent, copy of this map */
    public Object clone() {
        TermMap newMap = new TermMap( termLimit );
        newMap.hashMap = (HashMap) hashMap.clone();
        newMap.nTerms = nTerms;
        return newMap;
    }

    /** 
     * Add a term to the map. If the term limit is exceeded, a (runtime)
     * TermLimitException will be thrown.
     */
    public void put( Term term ) {
        
        // Note when counting terms that we don't want to count unique terms.
        // That would allow nasty queries that use the same term over and
        // over thousands of times.
        //
        if( nTerms++ == termLimit )
            throw new TermLimitException( 
                 "The query matched too many terms (more than " + 
                 termLimit + ")");
        
        // Add it.
        String lcName = term.text().toLowerCase();
        if( hashMap.get(lcName) == null )
            hashMap.put( lcName, new LinkedList() );
        LinkedList list = (LinkedList) hashMap.get( lcName );
        list.add( term.field() );
    }
    
    public boolean contains( String termText ) {
        return hashMap.containsKey( termText.toLowerCase() );
    }
    
    /**
     * Determines if the given term is present. If so, checks if the any field
     * for that term matches 'perferredField', and if so, return that field.
     * Otherwise, return the first field the term was in.
     * 
     * @param termText  Term text to look for; case is ignored.
     * @param preferredField  Field to look for
     */
    public String getField( String termText, String preferredField ) {
        LinkedList list = (LinkedList) hashMap.get( termText.toLowerCase() );
        if( list == null )
            return null;
        for( Iterator iter = list.iterator(); iter.hasNext(); ) {
            String field = (String) iter.next();
            if( field.equals(preferredField) )
                return field;
        }
        return (String) list.getFirst();
    }
} // class TermMap
