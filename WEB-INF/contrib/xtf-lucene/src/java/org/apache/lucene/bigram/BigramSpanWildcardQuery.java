package org.apache.lucene.bigram;

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


import java.util.Set;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanWildcardQuery;

/** Matches spans containing a wildcard term. Performs extra filtering to make 
 * sure bi-grams are not matched.
 */
public class BigramSpanWildcardQuery extends SpanWildcardQuery 
{
  private Set stopSet;
  
  /** Construct a SpanWildcardTermQuery matching expanded terms, but
   *  limiting the total number of terms matched.
   */
  public BigramSpanWildcardQuery( Term term, int termLimit ) { 
    super( term, termLimit ); 
  }
  
  public void setStopWords( Set set ) {
      this.stopSet = set;
  }

  protected boolean shouldSkipTerm( Term term )
  {
      if( stopSet == null )
          return false;
      
      // Skip stop words
      if( stopSet.contains(term.text()) )
          return true;
      
      // Skip bi-grams containing stop words. 
      if( BigramQueryRewriter.isBigram(stopSet, term.text()) )
          return true;

      // Others are okay.
      return false;
  }

} // class XtfSpanWildcardQuery
