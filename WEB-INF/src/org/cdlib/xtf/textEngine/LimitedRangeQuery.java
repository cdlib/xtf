package org.cdlib.xtf.textEngine;

/**
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.util.Set;

import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 * <p>A Query that matches documents within an inclusive or exclusive range, 
 * but enforces a limit on the total number of terms that may be searched.</p>
 * 
 * <p>This is a modified version of RangeQuery from the Lucene 
 * distribution.</p>
 */
public class LimitedRangeQuery extends Query
{
    /** First term (lexicographically) to search for */
    private Term lowerTerm;

    /** Last term (lexicographically) to search for */
    private Term upperTerm;
    
    /** 
     * true to include the first and last terms; false to only use terms
     * between them.
     */
    private boolean inclusive;
    
    /** Limit on the total number of terms */
    private int termLimit;
    
    /** Map to add all terms to */
    private TermMap termMap;
    
    /** Set of stop-words (e.g. "the", "a", "and", etc.) */
    private Set stopWords;

    /** Constructs a query selecting all terms greater than
     * <code>lowerTerm</code> but less than <code>upperTerm</code>.
     * There must be at least one term and either term may be null,
     * in which case there is no bound on that side, but if there are
     * two terms, both terms <b>must</b> be for the same field.
     */
    public LimitedRangeQuery( Term lowerTerm, Term upperTerm, 
                              boolean inclusive, int termLimit,
                              TermMap termMap )
    {
        if( lowerTerm == null && upperTerm == null )
            throw new IllegalArgumentException(
                    "At least one term must be non-null");
        if (lowerTerm != null && upperTerm != null && 
            lowerTerm.field() != upperTerm.field())
        {
            throw new IllegalArgumentException(
                    "Both terms must be for the same field");
        }

        // if we have a lowerTerm, start there. otherwise, start at beginning
        if( lowerTerm != null )
            this.lowerTerm = lowerTerm;
        else
            this.lowerTerm = new Term( upperTerm.field(), "" );

        this.upperTerm = upperTerm;
        this.inclusive = inclusive;
        this.termLimit = termLimit;
        this.termMap   = termMap;
    }
    
    /** Establishes the list of stop-words to remove */
    public void setStopWords( Set stopWords ) {
        this.stopWords = stopWords;
    }

    /**
     * LuceneFIXME: Describe <code>rewrite</code> method here.
     *
     * @param reader an <code>IndexReader</code> value
     * @return a <code>Query</code> value
     * @exception IOException if an error occurs
     */
    public Query rewrite(IndexReader reader) throws IOException {

        BooleanQuery query = new BooleanQuery();
        TermEnum enumerator = reader.terms(lowerTerm);

        try {

            boolean checkLower = false;
            if (!inclusive) // make adjustments to set to exclusive
                checkLower = true;

            String testField = getField();
            int nTerms = 0;

            do {
                Term term = enumerator.term();
                if (term != null && term.field() == testField) {
                    if (stopWords != null && stopWords.contains(term.text()))
                        continue;
                    if (!checkLower || term.text().compareTo(lowerTerm.text()) > 0) {
                        checkLower = false;
                        if (upperTerm != null) {
                            int compare = upperTerm.text().compareTo(term.text());
                            /* if beyond the upper term, or is exclusive and
                             * this is equal to the upper term, break out */
                            if ((compare < 0) || (!inclusive && compare == 0))
                                break;
                        }
                        TermQuery tq = new TermQuery(term); // found a match
                        tq.setBoost(getBoost()); // set the boost
                        query.add(tq, false, false); // add to query
                        if (nTerms++ == termLimit)
                            throw new TermLimitException( 
                                "Range query on '" + getField() + 
                                "' matched too many terms (more than " + 
                                termLimit + ")");
                        if( termMap != null )
                            termMap.put( term );
                    }
                }
                else {
                    break;
                }
            }
            while (enumerator.next());
        }
        finally {
            enumerator.close();
        }
        return query;
    }

    public Query combine(Query[] queries) {
        return Query.mergeBooleanQueries(queries);
    }

    /** Returns the field name for this query */
    public String getField() {
        return (lowerTerm != null ? lowerTerm.field() : upperTerm.field());
    }

    /** Returns the lower term of this range query */
    public Term getLowerTerm() { return lowerTerm; }

    /** Returns the upper term of this range query */
    public Term getUpperTerm() { return upperTerm; }

    /** Returns <code>true</code> if the range query is inclusive */
    public boolean isInclusive() { return inclusive; }


    /** Prints a user-readable version of this query. */
    public String toString(String field)
    {
        StringBuffer buffer = new StringBuffer();
        if (!getField().equals(field))
        {
            buffer.append(getField());
            buffer.append(":");
        }
        buffer.append(inclusive ? "[" : "{");
        buffer.append(lowerTerm != null ? lowerTerm.text() : "null");
        buffer.append(" TO ");
        buffer.append(upperTerm != null ? upperTerm.text() : "null");
        buffer.append(inclusive ? "]" : "}");
        if (getBoost() != 1.0f)
        {
            buffer.append("^");
            buffer.append(Float.toString(getBoost()));
        }
        return buffer.toString();
    }
}

