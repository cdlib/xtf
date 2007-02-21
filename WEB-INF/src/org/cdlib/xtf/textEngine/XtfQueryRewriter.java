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
import org.apache.lucene.search.QueryRewriter;
import org.apache.lucene.search.spans.SpanQuery;

/**
 * Utility class for performing external rewriting, or transformation, tasks
 * on Lucene queries. The base class simply provides a framework. Derived
 * classes should override methods for those parts of a query they need to
 * rewrite, and the base will take care of gluing them together properly.
 */
public abstract class XtfQueryRewriter extends QueryRewriter 
{
  /**
   * Rewrite a query of any supported type.
   *
   * @param q   Query to rewrite
   * @return    A new query, or 'q' unchanged if no change was needed.
   */
  public Query rewriteQuery(Query q) {
    if (q instanceof SpanSectionTypeQuery)
      return rewrite((SpanSectionTypeQuery)q);
    else if (q instanceof SpanExactQuery)
      return rewrite((SpanExactQuery)q);
    else if (q instanceof MoreLikeThisQuery)
      return rewrite((MoreLikeThisQuery)q);
    else if (q instanceof NumericRangeQuery)
      return rewrite((NumericRangeQuery)q);
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
  protected Query rewrite(SpanSectionTypeQuery stq) 
  {
    // Rewrite the sub-queries
    SpanQuery textQuery = (SpanQuery)rewriteQuery(stq.getTextQuery());
    SpanQuery secTypeQuery = (SpanQuery)rewriteQuery(stq.getSectionTypeQuery());

    // If the sub-queries didn't change, then neither does the main query.
    if (textQuery == stq.getTextQuery() &&
        secTypeQuery == stq.getSectionTypeQuery() &&
        !forceRewrite(stq))
      return stq;

    // Make a new query
    Query newq = new SpanSectionTypeQuery(textQuery, secTypeQuery);
    copyBoost(stq, newq);
    return newq;
  } // rewrite()

  /** Rewrite an exact query. The base class rewrites each of the sub-clauses. */
  protected Query rewrite(final SpanExactQuery q) 
  {
    // Rewrite each clause. Do not allow single clauses to be promoted, as that
    // would get rid of the 'exactness' requirement.
    //
    return rewriteClauses(q, q.getClauses(), false,
                          new SpanClauseJoiner() 
    {
        public SpanQuery join(SpanQuery[] clauses) {
          return new SpanExactQuery(clauses);
        }
    });
  }

  /** Rewrite a "more like this" query */
  protected Query rewrite(MoreLikeThisQuery mlt) {
    Query rewrittenSub = rewriteQuery(mlt.getSubQuery());
    if (rewrittenSub == mlt.getSubQuery() && !forceRewrite(mlt))
      return mlt;
    MoreLikeThisQuery ret = (MoreLikeThisQuery)mlt.clone();
    ret.setSubQuery(rewrittenSub);
    return ret;
  }

  /** Rewrite a numeric range query */
  protected Query rewrite(NumericRangeQuery nrq) {
    if (!forceRewrite(nrq))
      return nrq;
    return (NumericRangeQuery)nrq.clone();
  }
} // class XtfQueryRewriter
