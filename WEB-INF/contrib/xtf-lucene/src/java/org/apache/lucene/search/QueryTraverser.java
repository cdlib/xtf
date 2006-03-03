package org.apache.lucene.search;

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

import org.apache.lucene.chunk.SpanChunkedNotQuery;
import org.apache.lucene.chunk.SpanDechunkingQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanNotNearQuery;
import org.apache.lucene.search.spans.SpanNotQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanRangeQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.SpanWildcardQuery;

/**
 * Utility class for performing external traversal tasks on Lucene queries. 
 * The base class simply provides a framework. Derived classes should override 
 * methods for those parts of a query they need to process, rewrite, and the 
 * base will take care of calling them properly. 
 */
public abstract class QueryTraverser {

  /**
   * Traverse a query of any supported type.
   * 
   * @param q   Query to traverse
   */
  public void traverseQuery(Query q) {
    if (q instanceof BooleanQuery)
      traverse((BooleanQuery) q);
    else if (q instanceof SpanNearQuery)
      traverse((SpanNearQuery) q);
    else if (q instanceof SpanOrQuery)
      traverse((SpanOrQuery) q);
    else if (q instanceof SpanChunkedNotQuery)
      traverse((SpanChunkedNotQuery) q);
    else if (q instanceof SpanNotQuery)
      traverse((SpanNotQuery) q);
    else if (q instanceof SpanNotNearQuery)
      traverse((SpanNotNearQuery) q);
    else if (q instanceof SpanDechunkingQuery)
      traverse((SpanDechunkingQuery) q);
    else if (q instanceof TermQuery)
      traverse((TermQuery)q);
    else if (q instanceof SpanWildcardQuery) // must be before SpanTermQuery
      traverse((SpanWildcardQuery)q);
    else if (q instanceof SpanTermQuery)
      traverse((SpanTermQuery)q);
    else if (q instanceof SpanRangeQuery)
      traverse((SpanRangeQuery)q);
    else
      assert false : "unsupported query type for traversal";
  } // traverseQuery()

  /**
   * Traverse a BooleanQuery.
   * 
   * @param bq  The query to traverse
   */
  protected void traverse(BooleanQuery bq) {
    BooleanClause[] clauses = bq.getClauses();
    for (int i = 0; i < clauses.length; i++)
      traverseQuery(clauses[i].query);
  } // traverse()

  /**
   * Traverse a span NEAR query.
   * 
   * @param nq  The query to traverse
   */
  protected void traverse(SpanNearQuery nq) {
    SpanQuery[] clauses = nq.getClauses();
    for (int i = 0; i < clauses.length; i++)
      traverseQuery(clauses[i]);
  } // traverse()

  /**
   * Traverse a span-based OR query.
   * 
   * @param oq  The query to traverse
   */
  protected void traverse(SpanOrQuery oq) {
    SpanQuery[] clauses = oq.getClauses();
    for (int i = 0; i < clauses.length; i++)
      traverseQuery(clauses[i]);
  } // traverse()

  /**
   * Traverse a span-based chunked NOT query.
   * 
   * @param nq  The query to traverse
   */
  protected void traverse(SpanChunkedNotQuery nq) {
    traverseQuery(nq.getInclude());
    traverseQuery(nq.getExclude());
  } // traverse()

  /**
   * Traverse a span-based NOT query.
   * 
   * @param nq  The query to traverse
   */
  protected void traverse(SpanNotQuery nq) {
    traverseQuery(nq.getInclude());
    traverseQuery(nq.getExclude());
  } // traverse()

  /**
   * Traverse a span-based NOT NEAR query.
   * 
   * @param nq  The query to traverse
   */
  protected void traverse(SpanNotNearQuery nq) {
    traverseQuery(nq.getInclude());
    traverseQuery(nq.getExclude());
  } // traverse()

  /**
   * Traverse a span dechunking query. 
   * 
   * @param nq  The query to traverse
   */
  protected void traverse(SpanDechunkingQuery nq) {
    traverseQuery(nq.getWrapped());
  } // traverse()

  /** 
   * Traverse a term query. The base class does nothing.
   * 
   * @param q  The query to traverse
   */
  protected void traverse(TermQuery q) {
  }
  
  /** 
   * Traverse a span term query.
   * 
   * @param q  The query to traverse
   */
  protected void traverse(SpanTermQuery q) {
  }
  
  /** 
   * Traverse a span wildcard query. The base class does nothing.
   * 
   * @param q  The query to traverse
   */
  protected void traverse(SpanWildcardQuery q) {
  }
  
  /** 
   * Traverse a span range query. The base class does nothing.
   * 
   * @param q  The query to traverse
   */
  protected void traverse(SpanRangeQuery q) {
  }
}