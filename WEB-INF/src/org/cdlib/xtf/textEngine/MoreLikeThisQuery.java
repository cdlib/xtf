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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.HitCollector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.similar.MoreLikeThis;
import org.cdlib.xtf.textIndexer.XTFTextAnalyzer;
import org.cdlib.xtf.util.CharMap;
import org.cdlib.xtf.util.Trace;
import org.cdlib.xtf.util.WordMap;

/** 
 * Processes the sub-query and uses the first document as the "target".
 * Then we determine the most "interesting" terms in the target document,
 * and finally perform a query on those terms to find more like the target.
 * The target document itself will NOT be included in the results.
 */
public class MoreLikeThisQuery extends Query 
{
  private Query subQuery;
  private int targetDoc;
  private Set stopSet;
  private WordMap pluralMap;
  private CharMap accentMap;

  /** Constructs a span query selecting all terms greater than
   * <code>lowerTerm</code> but less than <code>upperTerm</code>.
   * There must be at least one term and either term may be null,
   * in which case there is no bound on that side, but if there are
   * two terms, both terms <b>must</b> be for the same field. Applies
   * a limit on the total number of terms matched.
   */
  public MoreLikeThisQuery( Query subQuery )
  {
    this.subQuery = subQuery;
  }
  
  /** Retrieve the sub-query */
  public Query getSubQuery() {
    return subQuery;
  }

  /** Establish the set of stop words to ignore */
  public void setStopWords( Set set ) {
      this.stopSet = set;
  }
  
  /** Establish the plural map in use */
  public void setPluralMap( WordMap map ) {
      this.pluralMap = map;
  }
  
  /** Establish the accent map in use */
  public void setAccentMap( CharMap map ) {
      this.accentMap = map;
  }

  /**
   * Generate a query that will produce "more documents like" the first
   * in the sub-query.
   */
  public Query rewrite( IndexReader reader ) throws IOException 
  {
    // Determine the target document.
    IndexSearcher searcher = new IndexSearcher( reader );
    targetDoc = -1;
    HitCollector collector = new HitCollector() {
        public void collect( int doc, float score ) {
            if( targetDoc < 0 )
                targetDoc = doc;
        }
    }; 
    
    searcher.search( subQuery, collector );
    
    // If none, make a query that will definitely return nothing at all.
    if( targetDoc < 0 )
        return new TermQuery( new Term("fribbleSnarf", "!*@&#(*&") );
    
    // Use a helper class to construct the query for similar documents. Use 
    // the indexer's actual analyzer, so that our results always agree. 
    //
    MoreLikeThis mlt = new MoreLikeThis( reader ) {
        protected boolean isNoiseWord( String term ) {
            if( term.length() > 0 ) {
                if( term.charAt(0) == Constants.FIELD_START_MARKER ||
                    term.charAt(term.length()-1) == Constants.FIELD_END_MARKER )
                {
                    return true;
                }
            }
            return super.isNoiseWord( term );
        }
    };

    mlt.setAnalyzer( new XTFTextAnalyzer(null, pluralMap, accentMap) );
    mlt.setStopWords( stopSet );
    
    // gather list of valid fields from lucene
    Collection fieldColl = reader.getFieldNames( true );
    ArrayList fieldList = new ArrayList( fieldColl.size() );
    for( Iterator iter = fieldColl.iterator(); iter.hasNext(); ) {
        String field = (String) iter.next();
        if( !field.equals("chunkCount") &&
            !field.equals("docInfo") &&
            !field.equals("fileDate") &&
            !field.equals("indexInfo") && 
            !field.equals("key") && 
            !field.equals("recordNum") &&
            !field.equals("text") &&
            !field.startsWith("sort-") &&
            !field.startsWith("facet-") )
        {
            fieldList.add( field );
        }
    }
    
    // Set options.
    mlt.setFieldNames( (String[])
        fieldList.toArray(new String[fieldList.size()]) );
    mlt.setMinWordLen( 4 );
    mlt.setMaxWordLen( 12 );
    mlt.setMinDocFreq( 2 );
    int nDocs = reader.docFreq(new Term("docInfo", "1"));
    int maxDocFreq = Math.max( 5, nDocs / 20 );
    mlt.setMaxDocFreq( maxDocFreq );
    mlt.setMinTermFreq( 2 );
    mlt.setBoost( true );
    mlt.setMaxQueryTerms( 10 );

    // Make the similarity query
    Query ret = mlt.like( targetDoc );
    if( Trace.getOutputLevel() >= Trace.debug )
        Trace.debug( "More-like query: " + ret );
    
    // TODO: Remove the target document from the result set.
    return ret;
  }

  /** Prints a user-readable version of this query. */
  public String toString(String field) {
    return "moreLikeThis(" + subQuery.toString(field) + ")";
  }

} // class MoreLikeThisQuery
