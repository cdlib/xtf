package org.apache.lucene.spelt;

/**
 * Copyright 2007 The Apache Software Foundation
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

import java.util.ArrayList;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 * <p>
 *   Traverses and rewrites simple Lucene queries. This includes boolean
 *   and phrase queries, but not much else. Very useful for extracting and/or
 *   changing the terms in a query. Usually you derive a class and then
 *   override {@link #rewrite(Term)} to do what you need.
 * </p>
 * <p>
 *  If you need to handle other types of queries, derive a class and
 *  provide a {@link #rewriteQuery(Query)} method that dispatches to your
 *  custom rewriting methods.
 * </p.
 * 
 * @author Martin Haye
 */
public abstract class SimpleQueryRewriter
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
    if (q instanceof PhraseQuery)
      return rewrite((PhraseQuery)q);
    if (q instanceof TermQuery)
      return rewrite((TermQuery)q);

    // Unknown type: do nothing
    return q;
  } // rewriteQuery()

  /**
   * Rewrite a BooleanQuery.
   *
   * @param bq  The query to rewrite
   * @return    Rewritten version, or 'bq' unchanged if no changed needed.
   */
  protected Query rewrite(BooleanQuery bq) 
  {
    ArrayList<BooleanClause> newClauses = new ArrayList<BooleanClause>();
    boolean anyChange = false;
    for (BooleanClause oldClause : bq.getClauses())
    {
      // Rewrite the clause and its descendants
      Query rewritten = rewriteQuery(oldClause.getQuery());
      if (rewritten != oldClause.getQuery()) { 
        anyChange = true;
        if (rewritten != null)
          newClauses.add(new BooleanClause(rewritten, oldClause.getOccur()));
      }
      else
        newClauses.add(oldClause);
    }

    // If no clauses changed, then the BooleanQuery doesn't change either.
    if (!anyChange)
      return bq;

    // If we ended up with nothing, let the caller know.
    if (newClauses.isEmpty())
      return null;

    // If we ended up with a single clause, return just that.
    if (newClauses.size() == 1) {
      BooleanClause clause = newClauses.get(0);
      if (clause.getOccur() != BooleanClause.Occur.MUST_NOT) {
        Query newq = clause.getQuery();
        newq.setBoost(Math.max(bq.getBoost(), newq.getBoost()));
        return newq;
      }
    }

    // Otherwise, we need to construct a new BooleanQuery.
    bq = new BooleanQuery(bq.isCoordDisabled());
    bq.setBoost(bq.getBoost());
    for (BooleanClause newClause : newClauses)
      bq.add(newClause);
    return bq;
  }
  
  /**
   * Rewrite a phrase query. The base class does nothing.
   *
   * @param pq  The query to rewrite
   * @return    Rewritten version, or 'pq' unchanged if no change needed.
   */
  protected Query rewrite(PhraseQuery pq) 
  {
    Term[] oldTerms = pq.getTerms();
    
    ArrayList<Term> newTerms = new ArrayList<Term>();
    
    // Rewrite each term in turn.
    boolean anyChange = false;
    for (int i=0; i<oldTerms.length; i++) {
      Term newTerm = rewrite(oldTerms[i]);
      if (newTerm != oldTerms[i]) {
        anyChange = true;
        if (newTerm != null) 
        {
          // If the term is splitting, make it into two terms.
          int spacePos = newTerm.text().indexOf(' ');
          if (oldTerms[i].text().indexOf(' ') < 0 && spacePos >= 0) {
            newTerms.add(new Term(newTerm.field(), newTerm.text().substring(0, spacePos)));
            newTerms.add(new Term(newTerm.field(), newTerm.text().substring(spacePos+1)));
          }
          else
            newTerms.add(newTerm);
        }
      }
      else {
        newTerms.add(oldTerms[i]);
      }
    }
    
    // If no changes, return the original.
    if (!anyChange)
      return pq;
    
    // If all terms disappeared, inform the caller.
    if (newTerms.size() == 0)
      return null;
    
    // If only one term, convert to a term query.
    if (newTerms.size() == 1) {
      TermQuery newq = new TermQuery(newTerms.get(0));
      newq.setBoost(pq.getBoost());
      return newq;
    }
    
    // Make a new phrase query.
    PhraseQuery newq = new PhraseQuery();
    newq.setBoost(pq.getBoost());
    newq.setSlop(pq.getSlop());
    for (int i=0; i<newTerms.size(); i++)
      newq.add(newTerms.get(i));
    return newq;
  }

  /**
   * Rewrite a term query. The base class rewrites the term itself.
   *
   * @param q  The query to rewrite
   * @return   Rewritten version, or 'q' unchanged if no change needed.
   */
  protected Query rewrite(TermQuery q) 
  {
    Term oldTerm = q.getTerm();
    Term newTerm = rewrite(oldTerm);
    
    // If the term is unchanged, don't change the query.
    if (oldTerm == newTerm)
      return q;
    
    // If the term is going away, inform the caller
    if (newTerm == null)
      return null;
    
    // If the term is splitting, make it into two terms.
    int spacePos = newTerm.text().indexOf(' ');
    if (oldTerm.text().indexOf(' ') < 0 && spacePos >= 0) {
      PhraseQuery pq = new PhraseQuery();
      pq.add(new Term(newTerm.field(), newTerm.text().substring(0, spacePos)));
      pq.add(new Term(newTerm.field(), newTerm.text().substring(spacePos+1)));
      pq.setBoost(q.getBoost());
      return pq;
    }
    
    // Make a new query for the new term.
    TermQuery newQuery = new TermQuery(newTerm);
    newQuery.setBoost(q.getBoost());
    return newQuery;
  }
  
  /**
   * Rewrite a term (e.g. part of a TermQuery or PhraseQuery). The base
   * class does nothing.
   * 
   * @param t   The term to rewrite
   * @return    Rewritten version, or 't' unchanged if no change needed.
   */
  protected Term rewrite(Term t) {
    return t;
  }
}
