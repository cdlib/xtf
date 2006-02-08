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

import java.util.Vector;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryRewriter;
import org.apache.lucene.search.spans.SpanQuery;

/**
 * Utility class for performing external rewriting, or transformation, tasks
 * on Lucene queries. The base class simply provides a framework. Derived
 * classes should override methods for those parts of a query they need to
 * rewrite, and the base will take care of gluing them together properly. 
 */
public abstract class XtfQueryRewriter extends QueryRewriter {

  /**
   * Rewrite a query of any supported type.
   * 
   * @param q   Query to rewrite
   * @return    A new query, or 'q' unchanged if no change was needed.
   */
  public Query rewriteQuery(Query q) {
    if (q instanceof SpanSectionTypeQuery)
      return rewrite((SpanSectionTypeQuery) q);
    else if (q instanceof SpanExactQuery)
      return rewrite((SpanExactQuery)q);
    else if (q instanceof MoreLikeThisQuery)
      return rewrite((MoreLikeThisQuery)q);
    else
      return super.rewriteQuery(q);
  } // rewriteQuery()

  /**
   * Rewrite a section type query. If's very simple: simply rewrite the
   * sub-queries.
   * 
   * @param stq  The query to rewrite
   * @return     Rewritten version, or 'nq' unchanged if no changed needed.
   */
  protected Query rewrite(SpanSectionTypeQuery stq) {
    // Rewrite the sub-queries
    SpanQuery textQuery = (SpanQuery) rewriteQuery(stq.getTextQuery());
    SpanQuery secTypeQuery = (SpanQuery) rewriteQuery(stq.getSectionTypeQuery());

    // If the sub-queries didn't change, then neither does the main query.
    if (textQuery == stq.getTextQuery()
        && secTypeQuery == stq.getSectionTypeQuery()
        && !forceRewrite(stq))
      return stq;

    // Make a new query
    Query newq = new SpanSectionTypeQuery(textQuery, secTypeQuery);
    copyBoost(stq, newq);
    return newq;

  } // rewrite()

  /** Rewrite an exact query. The base class rewrites each of the sub-clauses. */
  protected Query rewrite(SpanExactQuery eq) {
    // Rewrite each clause.
    SpanQuery[] clauses = eq.getClauses();
    Vector newClauses = new Vector();
    boolean anyChanges = false;

    for (int i = 0; i < clauses.length; i++) {
      SpanQuery clause = (SpanQuery) rewriteQuery(clauses[i]);
      if (clause != clauses[i])
        anyChanges = true;
      
      // If the clause ended up null, skip it.
      if (clause == null)
        continue;

      // Retain everything else.
      newClauses.add(clause);
    } // for i

    // If no changes, just return the original query.
    boolean force = forceRewrite(eq);
    if (!anyChanges && !force)
      return eq;

    // If no clauses, let the caller know they can delete this query.
    if (newClauses.isEmpty())
      return null;

    // If we have only one clause, return just that. Pass on the parent's
    // boost to the only child.
    //
    if (newClauses.size() == 1 && !force)
      return combineBoost(eq, (Query) newClauses.elementAt(0));

    // Construct a new 'exact' query joining all the rewritten clauses.
    SpanQuery[] newArray = new SpanQuery[newClauses.size()];
    return copyBoost(eq, new SpanExactQuery((SpanQuery[]) newClauses
        .toArray(newArray)));

  }
  
  /** Rewrite a "more like this" query */
  protected Query rewrite(MoreLikeThisQuery mlt) {
    Query rewrittenSub = rewriteQuery(mlt.getSubQuery());
    if (rewrittenSub == mlt.getSubQuery() && !forceRewrite(mlt))
        return mlt;
    return copyBoost(mlt, new MoreLikeThisQuery(rewrittenSub));
  }
  
}