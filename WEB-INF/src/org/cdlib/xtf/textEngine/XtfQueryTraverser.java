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

import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryTraverser;
import org.apache.lucene.search.spans.SpanQuery;

/**
 * Utility class for performing external rewriting, or transformation, tasks
 * on Lucene queries. The base class simply provides a framework. Derived
 * classes should override methods for those parts of a query they need to
 * rewrite, and the base will take care of gluing them together properly. 
 */
public abstract class XtfQueryTraverser extends QueryTraverser {

  /**
   * Traverse a query of any supported type.
   * 
   * @param q   Query to traverse
   */
  public void traverseQuery(Query q) {
    if (q instanceof SpanSectionTypeQuery)
      traverse((SpanSectionTypeQuery) q);
    else if (q instanceof SpanExactQuery)
      traverse((SpanExactQuery)q);
    else if (q instanceof MoreLikeThisQuery)
      traverse((MoreLikeThisQuery)q);
    else
      super.traverseQuery(q);
  } // traverseQuery()

  /** Traverse a section type query. */
  protected void traverse(SpanSectionTypeQuery stq) {
    traverseQuery(stq.getTextQuery());
    traverseQuery(stq.getSectionTypeQuery());
  } // traverse()

  /** Traverse an "exact" query. */
  protected void traverse(SpanExactQuery eq) {
    SpanQuery[] clauses = eq.getClauses();
    for (int i = 0; i < clauses.length; i++)
      traverseQuery(clauses[i]);
  }
  
  /** Traverse a "more like this" query */
  protected void traverse(MoreLikeThisQuery mlt) {
    traverseQuery(mlt.getSubQuery());
  }
  
}