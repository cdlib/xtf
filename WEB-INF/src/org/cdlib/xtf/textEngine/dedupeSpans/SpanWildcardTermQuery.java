package org.cdlib.xtf.textEngine.dedupeSpans;

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

import java.util.Vector;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.WildcardTermEnum;
import org.cdlib.xtf.textEngine.NgramQueryRewriter;
import org.cdlib.xtf.textEngine.TermLimitException;

/** Matches spans containing a wildcard term. */
public class SpanWildcardTermQuery extends SpanTermQuery {

  private int termLimit;
  
  /** Construct a SpanWildcardTermQuery matching expanded terms */
  public SpanWildcardTermQuery(Term term, int termLimit) { 
    super(term); 
    this.termLimit = termLimit;
  }
  
  public Spans getSpans(final IndexReader reader, final Searcher searcher) 
    throws IOException 
  {
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
          if (nTerms++ == termLimit)
              throw new TermLimitException( 
                   "Wildcard query on '" + getTerm().field() +
                   "' matched too many terms (more than " + 
                   termLimit + ")");
        }
      } while (enumerator.next());
    } finally {
      enumerator.close();
    }
      
    // Now build a big OR query for all the terms.
    SpanOrQuery orQuery = new SpanOrQuery(
      (SpanQuery[])termQueries.toArray(new SpanQuery[0]));
    
    // Use it to do the work.
    return orQuery.getSpans(reader, searcher);
  }

}
