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
import java.util.Vector;
import org.apache.lucene.chunk.SpanChunkedNotQuery;
import org.apache.lucene.chunk.SpanDechunkingQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanNotNearQuery;
import org.apache.lucene.search.spans.SpanNotQuery;
import org.apache.lucene.search.spans.SpanOrNearQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanRangeQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.SpanWildcardQuery;

/**
 * Utility class for performing external rewriting, or transformation, tasks
 * on Lucene queries. The base class simply provides a framework. Derived
 * classes should override methods for those parts of a query they need to
 * rewrite, and the base will take care of gluing them together properly.
 */
public abstract class QueryRewriter 
{
  /**
   * Rewrite a query of any supported type.
   *
   * @param q   Query to rewrite
   * @return    A new query, or 'q' unchanged if no change was needed.
   */
  public Query rewriteQuery(Query q) {
    if (q instanceof BooleanQuery)
      return rewrite((BooleanQuery)q);
    if (q instanceof SpanNearQuery)
      return rewrite((SpanNearQuery)q);
    if (q instanceof SpanOrQuery)
      return rewrite((SpanOrQuery)q);
    if (q instanceof SpanOrNearQuery)
      return rewrite((SpanOrNearQuery)q);
    if (q instanceof SpanChunkedNotQuery)
      return rewrite((SpanChunkedNotQuery)q);
    if (q instanceof SpanNotQuery)
      return rewrite((SpanNotQuery)q);
    if (q instanceof SpanNotNearQuery)
      return rewrite((SpanNotNearQuery)q);
    if (q instanceof SpanDechunkingQuery)
      return rewrite((SpanDechunkingQuery)q);
    if (q instanceof TermQuery)
      return rewrite((TermQuery)q);
    if (q instanceof SpanWildcardQuery) // must be before SpanTermQuery
      return rewrite((SpanWildcardQuery)q);
    if (q instanceof SpanTermQuery)
      return rewrite((SpanTermQuery)q);
    if (q instanceof SpanRangeQuery)
      return rewrite((SpanRangeQuery)q);
    assert false : "Unsupported query type for rewriting: " +
    q.getClass().getName();
    return null;
  } // rewriteQuery()

  /**
   * Can be used to force some or all queries to be rewritten even if no
   * changes. This is useful for copying queries, or easily making changes
   * to them.
   *
   * The base class always returns false; derived classes should override
   * this method if they want copying behavior.
   */
  public boolean forceRewrite(Query q) {
    return false;
  }

  /**
   * Rewrite a BooleanQuery.
   *
   * @param bq  The query to rewrite
   * @return    Rewritten version, or 'bq' unchanged if no changed needed.
   */
  protected Query rewrite(BooleanQuery bq) 
  {
    Vector newClauses = new Vector();
    BooleanClause[] clauses = bq.getClauses();
    boolean anyChange = false;
    for (int i = 0; i < clauses.length; i++) 
    {
      // Rewrite the clause and/or its descendants
      Query rewrittenQuery = rewriteQuery(clauses[i].getQuery());
      if (rewrittenQuery != clauses[i].getQuery()) 
      {
        anyChange = true;
        if (rewrittenQuery != null) {
          newClauses.add(new BooleanClause(rewrittenQuery,
                                           clauses[i].getOccur()));
        }
      }
      else
        newClauses.add(clauses[i]);
    }

    // If no clauses changed, then the BooleanQuery doesn't change either.
    boolean force = forceRewrite(bq);
    if (!anyChange && !force)
      return bq;

    // If we ended up with nothing, let the caller know.
    if (newClauses.isEmpty())
      return null;

    // If we ended up with a single clause, return just that.
    if (newClauses.size() == 1 && !force) {
      BooleanClause clause = (BooleanClause)newClauses.elementAt(0);
      if (clause.getOccur() == BooleanClause.Occur.MUST_NOT)
        return combineBoost(bq, clause.getQuery());
    }

    // Otherwise, we need to construct a new BooleanQuery.
    bq = (BooleanQuery)copyBoost(bq, new BooleanQuery(bq.isCoordDisabled()));
    for (int i = 0; i < newClauses.size(); i++)
      bq.add((BooleanClause)newClauses.elementAt(i));

    return bq;
  } // rewrite()

  /**
   * Rewrite a span NEAR query.
   *
   * @param q  The query to rewrite
   * @return    Rewritten version, or 'q' unchanged if no changed needed.
   */
  protected Query rewrite(final SpanNearQuery q) 
  {
    // Rewrite each clause. Allow single clauses to be promoted.
    return rewriteClauses(q, q.getClauses(), true,
      new SpanClauseJoiner() 
      {
        public SpanQuery join(SpanQuery[] clauses) {
          return new SpanNearQuery(clauses, q.getSlop(), q.isInOrder());
        }
      });
  } // rewrite()

  /**
   * Rewrite a span OR-NEAR query.
   *
   * @param q  The query to rewrite
   * @return    Rewritten version, or 'nq' unchanged if no changed needed.
   */
  protected Query rewrite(final SpanOrNearQuery q) 
  {
    // Rewrite each clause. Allow single clauses to be promoted.
    return rewriteClauses(
      q,
      q.getClauses(),
      true,
      new SpanClauseJoiner() 
      {
        public SpanQuery join(SpanQuery[] clauses) {
          return new SpanOrNearQuery(clauses,
                                     q.getSlop(),
                                     q.penalizeOutOfOrder());
        }
      });
  } // rewrite()

  /**
   * Rewrite a span-based OR query.
   *
   * @param q  The query to rewrite
   * @return    Rewritten version, or 'oq' unchanged if no changed needed.
   */
  protected Query rewrite(final SpanOrQuery q) 
  {
    // Rewrite each clause. Allow single clauses to be promoted.
    return rewriteClauses(q, q.getClauses(), true,
      new SpanClauseJoiner() 
      {
        public SpanQuery join(SpanQuery[] clauses) {
          return new SpanOrQuery(clauses);
        }
      });
  } // rewrite()

  /**
   * Rewrite a span-based NOT query. The procedure in this case is simple:
   * simply rewrite both the include and exclude clauses.
   *
   * @param nq  The query to rewrite
   * @return    Rewritten version, or 'nq' unchanged if no changed needed.
   */
  protected Query rewrite(SpanChunkedNotQuery nq) 
  {
    // Rewrite the sub-queries
    SpanQuery include = (SpanQuery)rewriteQuery(nq.getInclude());
    SpanQuery exclude = (SpanQuery)rewriteQuery(nq.getExclude());

    // If the sub-queries didn't change, then neither does this NOT.
    if (include == nq.getInclude() &&
        exclude == nq.getExclude() &&
        !forceRewrite(nq))
      return nq;

    // Make a new NOT query
    Query newq = new SpanChunkedNotQuery(include, exclude, nq.getSlop());
    copyBoost(nq, newq);
    return newq;
  } // rewrite()

  /**
   * Rewrite a span-based NOT query. The procedure in this case is simple:
   * simply rewrite both the include and exclude clauses.
   *
   * @param nq  The query to rewrite
   * @return    Rewritten version, or 'nq' unchanged if no changed needed.
   */
  protected Query rewrite(SpanNotQuery nq) 
  {
    // Rewrite the sub-queries
    SpanQuery include = (SpanQuery)rewriteQuery(nq.getInclude());
    SpanQuery exclude = (SpanQuery)rewriteQuery(nq.getExclude());

    // If the sub-queries didn't change, then neither does this NOT.
    if (include == nq.getInclude() &&
        exclude == nq.getExclude() &&
        !forceRewrite(nq))
      return nq;

    // Make a new NOT query
    Query newq = new SpanNotQuery(include, exclude);
    copyBoost(nq, newq);
    return newq;
  } // rewrite()

  /**
   * Rewrite a span-based NOT query. The procedure in this case is simple:
   * simply rewrite both the include and exclude clauses.
   *
   * @param nq  The query to rewrite
   * @return    Rewritten version, or 'nq' unchanged if no changed needed.
   */
  protected Query rewrite(SpanNotNearQuery nq) 
  {
    // Rewrite the sub-queries
    SpanQuery include = (SpanQuery)rewriteQuery(nq.getInclude());
    SpanQuery exclude = (SpanQuery)rewriteQuery(nq.getExclude());

    // If the sub-queries didn't change, then neither does this NOT.
    if (include == nq.getInclude() &&
        exclude == nq.getExclude() &&
        !forceRewrite(nq))
      return nq;

    // Make a new NOT query
    Query newq = new SpanNotNearQuery(include, exclude, nq.getSlop());
    copyBoost(nq, newq);
    return newq;
  } // rewrite()

  /**
   * Rewrite a span dechunking query. If's very simple: simply rewrite the
   * clause the query wraps.
   *
   * @param nq  The query to rewrite
   * @return    Rewritten version, or 'nq' unchanged if no changed needed.
   */
  protected Query rewrite(SpanDechunkingQuery nq) 
  {
    // Rewrite the sub-queries
    SpanQuery sub = (SpanQuery)rewriteQuery(nq.getWrapped());

    // If the sub-query didn't change, then neither does the main query.
    if (sub == nq.getWrapped() && !forceRewrite(nq))
      return nq;

    // No sub-query? Don't wrap it then.
    if (sub == null)
      return null;

    // Make a new dechunking query
    Query newq = new SpanDechunkingQuery(sub);
    copyBoost(nq, newq);
    return newq;
  } // rewrite()

  /**
   * Rewrite a term query. The base class does nothing.
   *
   * @param q  The query to rewrite
   * @return   Rewritten version, or 'q' unchanged if no changed needed.
   */
  protected Query rewrite(TermQuery q) {
    return q;
  }

  /**
   * Rewrite a span term query. The base class does nothing unless
   * rewriting is forced.
   *
   * @param q  The query to rewrite
   * @return   Rewritten version, or 'q' unchanged if no changed needed.
   */
  protected Query rewrite(SpanTermQuery q) {
    return forceRewrite(q) ? ((Query)q.clone()) : q;
  }

  /**
   * Rewrite a span wildcard query. The base class does nothing unless
   * rewriting is forced.
   *
   * @param q  The query to rewrite
   * @return   Rewritten version, or 'q' unchanged if no changed needed.
   */
  protected Query rewrite(SpanWildcardQuery q) {
    return forceRewrite(q) ? ((Query)q.clone()) : q;
  }

  /**
   * Rewrite a span range query. The base class does nothing unless
   * rewriting is forced.
   *
   * @param q  The query to rewrite
   * @return   Rewritten version, or 'q' unchanged if no changed needed.
   */
  protected Query rewrite(SpanRangeQuery q) {
    return forceRewrite(q) ? ((Query)q.clone()) : q;
  }

  /**
   * Copies the boost value from an old query to a newly created one. Also
   * copies the spanRecording attribute.
   *
   * Returns the new query for ease of chaining.
   *
   * @param oldQuery    Query to copy from
   * @param newQuery    Query to copy to
   * @return            Value of 'newQuery' (useful for chaining)
   */
  protected Query copyBoost(Query oldQuery, Query newQuery) 
  {
    newQuery.setBoost(oldQuery.getBoost());
    if (newQuery instanceof SpanQuery && oldQuery instanceof SpanQuery) {
      ((SpanQuery)newQuery).setSpanRecording(
        ((SpanQuery)oldQuery).getSpanRecording());
    }
    return newQuery;
  } // copyBoost()

  /**
   * Copies the max boost value from two old queries to a newly created one.
   * Also copies the spanRecording attribute.
   *
   * Returns the new query for ease of chaining.
   *
   * @param oldQuery1    First query to copy from
   * @param oldQuery2    Second query to copy from
   * @param newQuery     Query to copy to
   * @return             Value of 'newQuery' (useful for chaining)
   */
  protected Query copyBoost(Query oldQuery1, Query oldQuery2, Query newQuery) 
  {
    newQuery.setBoost(Math.max(oldQuery1.getBoost(), oldQuery2.getBoost()));
    if (newQuery instanceof SpanQuery) {
      ((SpanQuery)newQuery).setSpanRecording(
        Math.max(((SpanQuery)oldQuery1).getSpanRecording(),
                 ((SpanQuery)oldQuery2).getSpanRecording()));
    }
    return newQuery;
  } // copyBoost()

  /**
   * Combines the boost value from an old query with that of a newly created
   * one. Also preserves the spanRecording attribute.
   *
   * Returns the new query for ease of chaining.
   *
   * @param oldQuery    Query to combine from
   * @param newQuery    Query to combine to
   * @return            Value of 'newQuery' (useful for chaining)
   */
  protected Query combineBoost(Query oldQuery, Query newQuery) 
  {
    newQuery.setBoost(oldQuery.getBoost() * newQuery.getBoost());
    if (newQuery instanceof SpanQuery && oldQuery instanceof SpanQuery) {
      ((SpanQuery)newQuery).setSpanRecording(
        Math.max(((SpanQuery)oldQuery).getSpanRecording(),
                 ((SpanQuery)newQuery).getSpanRecording()));
    }
    return newQuery;
  } // copyBoost()

  /**
   * Utility function that takes care of rewriting a series of span query
   * clauses.
   *
   * @param oldQuery    Query being rewritten
   * @param oldClauses  Clauses to rewrite
   * @param promoteSingle true to allow single-clause result to be returned,
   *                    false to force wrapping.
   * @param joiner      Handles joining new clauses into wrapper query
   * @return            New rewritten query, or 'oldQuery' if no changes.
   */
  protected Query rewriteClauses(Query oldQuery, SpanQuery[] oldClauses,
                                 boolean promoteSingle, SpanClauseJoiner joiner) 
  {
    Vector newClauses = new Vector();
    boolean anyChanges = false;

    for (int i = 0; i < oldClauses.length; i++) 
    {
      SpanQuery clause = (SpanQuery)rewriteQuery(oldClauses[i]);
      if (clause != oldClauses[i])
        anyChanges = true;

      // If the clause ended up null, skip it.
      if (clause == null)
        continue;

      // Retain everything else.
      newClauses.add(clause);
    } // for i

    // If no changes, just return the original clauses.
    boolean force = forceRewrite(oldQuery);
    if (!anyChanges && !force)
      return oldQuery;

    // If we ended up with zero clauses, let the caller know they can delete
    // the query.
    //
    if (newClauses.isEmpty())
      return null;

    // If only one clause (and we're allowed to shunt), just return the single
    // clause instead of a wrapping query.
    //
    if (newClauses.size() == 1) 
    {
      // Since we're getting rid of the parent, pass on its boost to the
      // child.
      //
      return combineBoost(oldQuery, (SpanQuery)newClauses.get(0));
    }

    // Construct a new query joining all the rewritten clauses.
    SpanQuery[] newArray = (SpanQuery[])newClauses.toArray(
      new SpanQuery[newClauses.size()]);
    Query newQuery = joiner.join(newArray);
    return copyBoost(oldQuery, newQuery);
  }

  /** Utility class that joins clauses into an Or query, And query, etc. */
  public interface SpanClauseJoiner {
    public SpanQuery join(SpanQuery[] clauses);
  }
}
