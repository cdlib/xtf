package org.apache.lucene.search.spans;

/**
 * Copyright 2005 The Apache Software Foundation
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

import java.util.Vector;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.ngram.NgramQueryRewriter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.WildcardTermEnum;
import org.cdlib.xtf.textEngine.TermLimitException;

/** Matches spans containing a wildcard term.
 * 
 * @author  Martin Haye
 * @version $Id: SpanWildcardQuery.java,v 1.3 2005-05-25 00:51:23 mhaye Exp $
 */
public class SpanWildcardQuery extends SpanTermQuery {

  /** Limit on the total number of terms matched */
  private int termLimit;
  
  /** Limit on the number of terms to report on an error */
  private static final int TERMS_TO_REPORT = 50;
  
  /** Construct a SpanWildcardTermQuery matching expanded terms */
  public SpanWildcardQuery(Term term) { 
    this(term, Integer.MAX_VALUE); 
  }
  
  /** Construct a SpanWildcardTermQuery matching expanded terms, but
   *  limiting the total number of terms matched.
   */
  public SpanWildcardQuery(Term term, int termLimit) { 
    super(term); 
    this.termLimit = termLimit;
  }
  
  /** Retrieve the term limit this was constructed with */
  public int getTermLimit() {
    return termLimit;
  }
  
  /**
   * This method is actually the workhorse of the class. Rewrites the 
   * wildcard query as a large span OR query on all of the matching terms.
   */
  public Query rewrite(IndexReader reader) throws IOException 
  {
    StringBuffer termReport = new StringBuffer(100);
    
    // Enumerate all the matching terms, and make a term query for each one.
    WildcardTermEnum enumerator = new WildcardTermEnum(reader, getTerm());
    Vector termQueries = new Vector();
    try {
      int nTerms = 0;
      do {
        Term t = enumerator.term();
        if (t != null) {
            
          // Skip stop words
          if (stopSet != null && stopSet.contains(t.text()))
            continue;
          
          // Skip n-grams containing stop words. 
          if( stopSet != null && 
              NgramQueryRewriter.isNgram(stopSet, t.text()) )
          {
              continue;
          }
          
          // Found a match
          SpanTermQuery tq = new SpanTermQuery(t);
          tq.setBoost(getBoost() * enumerator.difference()); // set the boost
          termQueries.add( tq );
          
          if (nTerms < TERMS_TO_REPORT ) {
              termReport.append( t.text() );
              termReport.append( " " );
          }
          
          if (nTerms++ == termLimit)
              throw new TermLimitException( 
                   "Wildcard query on '" + getTerm().field() +
                   "' matched too many terms (more than " + 
                   termLimit + "). First " + TERMS_TO_REPORT + " matches: " +
                   termReport.toString() );
        }
      } while (enumerator.next());
    } finally {
      enumerator.close();
    }
      
    // Now build a big OR query for all the terms.
    SpanOrQuery orQuery = new SpanOrQuery(
      (SpanQuery[])termQueries.toArray(new SpanQuery[0]));
    orQuery.setSpanRecording(getSpanRecording());
    return orQuery;
  }

  /** Should never be called on the wildcard query itself, only on the
   *  result of {@link #rewrite(IndexReader)}.
   */
  public Spans getSpans(final IndexReader reader, final Searcher searcher) 
    throws IOException
  {
    throw new UnsupportedOperationException();
  }
  
  public String toString(String field) {
    return "wild(" + super.toString(field) + ")";
  }
}
