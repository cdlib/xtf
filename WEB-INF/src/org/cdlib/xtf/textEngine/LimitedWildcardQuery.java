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

import java.io.IOException;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.FilteredTermEnum;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.WildcardTermEnum;

/**
 * Just like Lucene's WildcardQuery, with two exceptions:
 *   1. If the number of terms exceeds a specified limit, throws an exception.
 *   2. Matching terms are added to a term map for later reference.
 * 
 * @author Martin Haye
 */
public class LimitedWildcardQuery extends WildcardQuery
{
    /** Current number of terms */
    private int nTerms = 0;
    
    /** Limit on the total number of terms generated */
    private int termLimit;
    
    /** 
     * Map to which we should add terms (so they can be marked up later 
     * in text hits 
     */
    private TermMap termMap;
    
    /** Stop-words for the current index (e.g. the, a, and, etc.) */
    private Set stopWords;
    
    /**
     * Construct a query on the given wildcard term specification, enforcing
     * a limit on the total number of terms. Each term is added to the
     * term map.
     * 
     * @param term          Wildcard specification
     * @param termLimit     Limit on the number of terms to match
     * @param termMap       Map to add each term to
     */
    public LimitedWildcardQuery( Term       term, 
                                 int        termLimit,
                                 TermMap    termMap )
    {
        super( term );
        this.termLimit = termLimit;
        this.termMap   = termMap;
    }
    
    /** 
     * Establish a list of stop-words (e.g. "the", "a", "and", etc.) to
     * remove.
     */
    public void setStopWords( Set stopWords ) {
        this.stopWords = stopWords;
    }

    /**
     * Given an index reader, obtain an enumeration of all the terms matching
     * the wildcard specification given to the constructor.
     */
    protected FilteredTermEnum getEnum( IndexReader reader ) 
        throws IOException 
    {
        return new LimitedWildcardTermEnum( reader, getTerm() );
    }
    
    /** Takes care of enumerating all matching terms */
    class LimitedWildcardTermEnum extends WildcardTermEnum
    {
        private Set  stopWords;
        private Term curTerm;
        
        LimitedWildcardTermEnum( IndexReader reader, Term term )
            throws IOException
        {
            super( reader, term );
        }
        
        public boolean next() throws IOException
        {
            while( true ) {
                boolean more = super.next();
                if( !more ) {
                    curTerm = null;
                    return false;
                }
                curTerm = super.term();
                
                // If no stop-word set is active, this is the term 
                // we want.
                //
                if( stopWords == null )
                    break;
                
                // Skip stop-words.
                if( stopWords.contains(curTerm.text()) )
                    continue;

                // Skip n-grams containing stop words.
                if( NgramQueryRewriter.isNgram(stopWords, curTerm.text()) )
                    continue;
            }
            
            nTerms++;
            if( nTerms > termLimit ) {
                throw new TermLimitException( 
                        "Wildcard term query '" + getTerm().field() +
                        "' matched too many terms (more than " + 
                        termLimit + ")");
            }
            termMap.put( curTerm );
            
            return true;
        }
        
        public Term term()
        {
            return curTerm;
        } // term()
        
    } // class LimitedWildcardTermEnum()
    
} // class LimitedWildcardQuery
