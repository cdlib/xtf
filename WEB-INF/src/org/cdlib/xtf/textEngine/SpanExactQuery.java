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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.Spans;

/**
 * Just like a SpanNearQuery with slop set to zero, except that it also looks for
 * the special 'start-of-field' and 'end-of-field' tokens inserted by the
 * text indexer. Thus, it will match either the entire field, or none of it.
 * 
 * @author Martin Haye
 */
public class SpanExactQuery extends SpanQuery
{
    // Special token guaranteed to be less than startToken, but greater than any
    // normal token.
    //
    public static final String startTokenMinus = "\uE000";

    // Special token that marks the start of a field
    public static final char startToken = '\uEBEB';
    
    // Special token that marks the end of a field
    public static final char endToken   = '\uEE1D';
  
    // The clauses to match (not including the special start and end tokens)
    private SpanQuery[] clauses;
    
    /**
     * Construct an exact query on a set of clauses.
     * 
     * @param clauses         Clauses to match.
     */
    public SpanExactQuery( SpanQuery[] clauses )
    {
        // Must have at least one clause to work.
        if( clauses == null || clauses.length == 0 )
            throw new RuntimeException( "SpanExactQuery requires at least one clause" );

        // Record the input parms
        this.clauses = clauses; 
    } // constructor
    
    // inherit javadoc
    public Query rewrite( IndexReader reader ) throws IOException 
    {
        List    newClauses = new ArrayList( clauses.length );
        boolean anyChanged = false;
        for( int i = 0; i < clauses.length; i++ ) {
          SpanQuery clause = clauses[i];
          SpanQuery rewrittenClause = (SpanQuery)clause.rewrite(reader);
          newClauses.add(rewrittenClause);
          if (clause != rewrittenClause)
            anyChanged = true;
        }
        
        if (!anyChanged)
          return this;
        
        SpanExactQuery clone = (SpanExactQuery)this.clone();
        clone.clauses = (SpanQuery[]) 
            newClauses.toArray( new SpanQuery[newClauses.size()] );
        return clone;
    }
  
    /** Return the clauses whose spans are matched. */
    public SpanQuery[] getClauses() {
        return clauses;
    }
    
    /** Return all the sub-queries (clauses in our case) */
    public Query[] getSubQueries() {
        return clauses;
    }
    
    /** 
     * Iterate all the spans from the text query that match the sectionType
     * query also.
     */
    public Spans getSpans( final IndexReader reader, final Searcher searcher )
        throws IOException
    {
        // Modify the first and last clauses to include the special start-of-field
        // and end-of-field markers.
        //
        SpanQuery[] newClauses = new SpanQuery[clauses.length];
        for( int i = 0; i < clauses.length; i++ ) {
            if( !(clauses[i] instanceof SpanTermQuery) )
                throw new RuntimeException( "Exact queries only support plain terms" );
            
            // We only fool with the first and last clauses
            if( i > 0 && i < clauses.length-1 ) {
                newClauses[i] = clauses[i];
                continue;
            }
            
            // Get the term out.
            SpanTermQuery oldClause = (SpanTermQuery) clauses[i];
            String        term      = oldClause.getTerm().text();
            
            // If this is the first clause, add the start-of-field marker.
            if( i == 0 )
                term = startToken + term;
            
            // If this is the last clause, add the end-of-field marker.
            if( i == clauses.length-1 )
                term = term + endToken;
            
            // Construct the new clause.
            SpanTermQuery newClause = new SpanTermQuery(
                new Term(oldClause.getTerm().field(), term),
                oldClause.getStopWords() );
            newClause.setBoost( oldClause.getBoost() );
            
            newClauses[i] = newClause;
        } // for i
            
        // And make a near query out of it.
        SpanQuery q = new SpanNearQuery(newClauses, 20, true);
        q.setSpanRecording( getSpanRecording() );
        return q.getSpans( reader, searcher );
    }
    
    public String getField() {
        return clauses[0].getField();
    }

    public Collection getTerms() {
        Collection terms = new ArrayList();
        for( int i = 0; i < clauses.length; i++ ) {
            SpanQuery clause = clauses[i];
            terms.addAll(clause.getTerms());
        }
        return terms;
    }

    public String toString( String field ) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("spanExact(");
        for( int i = 0; i < clauses.length; i++ ) {
            SpanQuery clause = clauses[i];
            buffer.append( clause.toString(field) );
            if( i < clauses.length-1 )
              buffer.append( ", " );
        }
        buffer.append(")");
        return buffer.toString();
    }

} // class SpanExactQuery
