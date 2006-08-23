package org.cdlib.xtf.textEngine;

/*
 * Copyright (c) 2006, Regents of the University of California
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
import java.util.Iterator;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanOrNearQuery;
import org.apache.lucene.search.spans.SpanQuery;

/** 
 * Matches spans containing all the queries in any of the specified fields.
 * Fields with a higher concentration of terms (i.e. terms closer together)
 * will be scored higher.
 */
public class MultiFieldAndQuery extends Query 
{
  private String[]    fields;
  private SpanQuery[] spanQueries;
  private Query[]     termQueries;
  private int         slop;
  private int         spanRecording;
  
  public MultiFieldAndQuery(String[] fields, SpanQuery[] subQueries, 
                            int slop, int spanRecording) 
  {
    this.fields        = fields;
    this.spanQueries   = subQueries;
    this.slop          = slop;
    this.spanRecording = spanRecording;
    
    // For efficiency, we'll want non-span versions of the sub-queries, when
    // possible.
    //
    UnspanningQueryRewriter unspanningRewriter = new UnspanningQueryRewriter();
    this.termQueries = new Query[spanQueries.length];
    for (int i = 0; i < spanQueries.length; i++)
      termQueries[i] = unspanningRewriter.rewriteQuery(spanQueries[i]); 
  }

  /** Return the fields the query is to match */
  public String[] getFields() { return fields; } 

  /** Return the string terms that will be matched across fields */
  public SpanQuery[] getClauses() { return spanQueries; } 
  
  /** Return the slop factor for matches within a single field */
  public int getSlop() { return slop; }
  
  public Collection getTerms() { 
    ArrayList ret = new ArrayList();
    for (int i = 0; i < fields.length; i++) {
      for (int j = 0; j < spanQueries.length; j++) {
        Collection subTerms = spanQueries[j].getTerms();
        for (Iterator iter = subTerms.iterator(); iter.hasNext(); )
          ret.add(new Term(fields[i], ((Term)iter.next()).text()));
      }
    }
    return ret;
  }
  
  public Query rewrite(IndexReader reader) throws IOException 
  {
    BooleanQuery mainQuery = new BooleanQuery();
  
    // We'll be changing the field names a lot.
    RefieldingQueryRewriter refielder = new RefieldingQueryRewriter();
    
    // Form a clause for each term, across all fields. This implements:
    //
    // And(
    //   term1 in field1 or field2 or field3...
    //   term2 in field1 or field2 or field3...
    //   ..
    // )
    //
    for (int i = 0; i < termQueries.length; i++) {
      BooleanQuery termOrQuery = new BooleanQuery();
      for (int j = 0; j < fields.length; j++) {
        Query tq = refielder.refield(termQueries[i], fields[j]);
        termOrQuery.add(tq, false, false);
      }
      
      // Make sure these don't contribute to the overall score, but each
      // term must match in at least one field.
      //
      termOrQuery.setBoost(0.0f);
      mainQuery.add(termOrQuery, true, false);
    }
    
    // For highlighting and scoring computations, make a clause for
    // each field, searching for all terms if present. This implements:
    //
    // Or(
    //   OrNear(field1: term1,term2,...)
    //   OrNear(field2: term1,term2,...)
    //   ..
    // )
    //
    for (int i = 0; i < fields.length; i++) {
      SpanQuery[] termQueries = new SpanQuery[spanQueries.length];
      for (int j = 0; j < spanQueries.length; j++)
        termQueries[j] = (SpanQuery) refielder.refield(spanQueries[j], fields[i]);
      SpanOrNearQuery fieldOrQuery = new SpanOrNearQuery(termQueries, slop, true);
      fieldOrQuery.setSpanRecording(spanRecording);
      mainQuery.add(fieldOrQuery, false, false);
    }

    // That's it!
    Query finalQuery = mainQuery.rewrite(reader);
    return finalQuery;
  }

  // inherit JavaDoc
  public String toString(String field) {
    StringBuffer buffer = new StringBuffer();
    buffer.append("MultiFieldAnd(");
    
    for (int i=0; i<spanQueries.length; i++) {
      if (i>0)
        buffer.append("|");
      buffer.append(spanQueries[i]);
    }

    buffer.append(")");
    return buffer.toString();
  }

  /** Return the number of spans to record for each doc-field */
  public int getSpanRecording() {
    return spanRecording;
  }

} // class MultiFieldAndQuery
