package org.apache.lucene.ngram;

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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.lucene.chunk.SpanChunkedNotQuery;
import org.apache.lucene.chunk.SpanDechunkingQuery;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanRangeQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.SpanWildcardQuery;

import org.cdlib.xtf.textEngine.SpanSectionTypeQuery;
import org.cdlib.xtf.util.Tester;
import org.cdlib.xtf.util.Trace;

/**
 * Rewrites a query to eliminate stop words by combining them with
 * adjacent non-stop-words, forming "n-grams". This is a fairly in-depth
 * process, as n-gramming across NEAR and OR queries is complex.
 *
 * @author  Martin Haye
 * @version $Id: NgramQueryRewriter.java,v 1.1 2005-02-08 23:19:39 mhaye Exp $
 */
public class NgramQueryRewriter {
  /** Set of stop-words (e.g. "the", "a", "and", etc.) to remove */
  private Set stopSet;

  /** Maximum slop to allow in a query, based on the index being queried */
  private int maxSlop;

  /** Keeps track of all stop-words removed from the query */
  private HashSet removedTerms = new HashSet();

  /**
   * Constructs a rewriter using the given stopword set.
   * 
   * @param stopSet   Set of stopwords to remove or n-gram. This can be
   *                  constructed easily by calling 
   *                  {@link #makeStopSet(String)}.
   * @param maxSlop   Maximum slop to allow in a query, based on the index
   *                  being queried.
   */
  public NgramQueryRewriter(Set stopSet, int maxSlop) {
    this.stopSet = stopSet;
    this.maxSlop = maxSlop;
  } // constructor

  /**
   * Make a stop set given a space, comma, or semicolon delimited list of
   * stop words.
   *
   * @param stopWords   String of words to make into a set
   *
   * @return            A stop word set suitable for use when constructing 
   *                    an {@link NgramQueryRewriter}.
   */
  public static Set makeStopSet(String stopWords) {
    return NgramStopFilter.makeStopSet(stopWords);
  } // makeStopSet()

  /**
   * Determines if the given string is an n-gram of a real word with a
   * stop-word.
   * 
   * @param stopWords   The set of stop-words
   * @param str         The string to check
   * @return            true if it's an n-gram
   */
  public static boolean isNgram(Set stopWords, String str) {
    int pos = str.indexOf('~');

    // A tilde tells us if it's an ngram.
    if (pos < 0)
      return false;

    // Let's do some sanity checking
    assert pos > 0 && pos < str.length() - 1 : "n-gram tilde cannot be at start or end of term";
    String before = str.substring(0, pos);
    String after = str.substring(pos + 1);
    assert stopWords.contains(before) || stopWords.contains(after);

    // It's an n-gram.
    return true;
  } // isNgram()

  /**
   * Rewrite a query of any supported type. Stop words will either be
   * removed or n-grammed.
   * 
   * @param q   Query to rewrite
   * @return    A new query, or 'q' unchanged if no change was needed.
   */
  public Query rewriteQuery(Query q) {
    if (q instanceof BooleanQuery)
      return rewriteBoolean((BooleanQuery) q);
    if (q instanceof SpanNearQuery)
      return rewriteNear((SpanNearQuery) q);
    if (q instanceof SpanOrQuery)
      return rewriteOr((SpanOrQuery) q);
    if (q instanceof SpanChunkedNotQuery)
      return rewriteNot((SpanChunkedNotQuery) q);
    if (q instanceof SpanDechunkingQuery)
      return rewriteDechunk((SpanDechunkingQuery) q);
    if (q instanceof SpanSectionTypeQuery)
      return rewriteSecType((SpanSectionTypeQuery) q);
    if (q instanceof TermQuery)
      return q;

    // Term, wildcard and range queries simply need to be told which 
    // words to remove.
    //
    if (q instanceof SpanTermQuery) {
      ((SpanTermQuery) q).setStopWords(stopSet);
      return q;
    }
    if (q instanceof SpanWildcardQuery) {
      ((SpanWildcardQuery) q).setStopWords(stopSet);
      return q;
    }
    if (q instanceof SpanRangeQuery) {
      ((SpanRangeQuery) q).setStopWords(stopSet);
      return q;
    }

    assert false : "unsupported query type for rewriting";
    return null;
  } // rewriteQuery()

  /**
   * Rewrite a BooleanQuery. Prohibited or allowed (not required) clauses
   * that are single stop words will be removed. Required clauses will
   * have n-gramming applied.
   * 
   * @param bq  The query to rewrite
   * @return    Rewritten version, or 'bq' unchanged if no changed needed.
   */
  private Query rewriteBoolean(BooleanQuery bq) {
    // Classify all the clauses as required, prohibited, or just allowed.
    // Rewrite them along the way.
    //
    Vector required = new Vector();
    Vector prohibited = new Vector();
    Vector allowed = new Vector();

    // Process each clause in turn
    BooleanClause[] clauses = bq.getClauses();
    boolean anyChange = false;
    for (int i = 0; i < clauses.length; i++) {
      assert !(clauses[i].prohibited && clauses[i].required) : "clauses cannot be both prohibited and required";

      // Single stop words which are either prohibited or not required
      // must be removed. Make sure to add them to the removed list
      // so the user will be notified.
      //
      if (clauses[i].prohibited || !clauses[i].required) {
        if (stopSet.contains(extractTermText(clauses[i].query))) {
          removedTerms.add(extractTermText(clauses[i].query));
          anyChange = true;
          continue;
        }
      }

      // Rewrite the clause and/or its descendants
      Query rewrittenQuery = rewriteQuery(clauses[i].query);
      if (rewrittenQuery != clauses[i].query)
        anyChange = true;

      // And add it to the appropriate vector.
      if (rewrittenQuery == null)
        continue;
      else if (clauses[i].prohibited)
        prohibited.add(rewrittenQuery);
      else if (clauses[i].required)
        required.add(rewrittenQuery);
      else
        allowed.add(rewrittenQuery);
    } // for i

    // N-gram the required clauses, but not looking for an exact match.
    if (!required.isEmpty()) {
      if (ngramQueries(required, maxSlop))
        anyChange = true;
    }

    // If no changes were needed, return the original query unchanged.
    if (!anyChange)
      return bq;

    // If we ended up with nothing, let the caller know.
    if (required.isEmpty() && prohibited.isEmpty() && allowed.isEmpty())
      return null;

    // If we ended up with a single required clause and no other clauses, return
    // just that.
    //
    if (required.size() == 1 && prohibited.isEmpty() && allowed.isEmpty())
      return combineBoost(bq, (Query) required.elementAt(0));

    // Otherwise, we need to construct a new one.
    bq = (BooleanQuery) copyBoost(bq, new BooleanQuery());

    for (Iterator iter = required.iterator(); iter.hasNext();)
      bq.add((Query) iter.next(), true, false);

    for (Iterator iter = prohibited.iterator(); iter.hasNext();)
      bq.add((Query) iter.next(), false, true);

    for (Iterator iter = allowed.iterator(); iter.hasNext();)
      bq.add((Query) iter.next(), false, false);

    return bq;

  } // rewriteBoolean()

  /**
   * Rewrite a span NEAR query. Stop words will be n-grammed into adjacent
   * terms.
   * 
   * @param nq  The query to rewrite
   * @return    Rewritten version, or 'nq' unchanged if no changed needed.
   */
  private Query rewriteNear(SpanNearQuery nq) {
    // Rewrite each clause and make a vector of the new ones.
    SpanQuery[] clauses = nq.getClauses();
    Vector newClauses = new Vector();
    boolean anyChanges = false;

    for (int i = 0; i < clauses.length; i++) {
      // Rewrite this clause, and record any difference.
      SpanQuery clause = (SpanQuery) rewriteQuery(clauses[i]);
      if (clause != clauses[i])
        anyChanges = true;

      // If rewriting resulted in removing the query, toss it.
      if (clause == null)
        continue;

      // Add it to the vector
      newClauses.add(clause);
    } // for i

    // N-gram the rewritten clauses.
    anyChanges |= ngramQueries(newClauses, nq.getSlop());

    // If no changes, just return the original query.
    if (!anyChanges)
      return nq;

    // If we end up with no clauses, let the caller know.
    if (newClauses.isEmpty())
      return null;

    // If we end up with a single clause, return just that.
    if (newClauses.size() == 1) {

      // Since we're getting rid of the parent, pass on its boost to the
      // child.
      //
      return combineBoost(nq, (Query) newClauses.elementAt(0));
    }

    // Construct a new 'near' query joining all the rewritten clauses.
    SpanQuery[] newArray = new SpanQuery[newClauses.size()];
    return copyBoost(nq, new SpanNearQuery((SpanQuery[]) newClauses
        .toArray(newArray), nq.getSlop(), false));

  } // rewriteNear()

  /**
   * Rewrite a span-based OR query. The procedure in this case is simple:
   * remove all stop words, with no n-gramming performed.
   * 
   * @param oq  The query to rewrite
   * @return    Rewritten version, or 'oq' unchanged if no changed needed.
   */
  private Query rewriteOr(SpanOrQuery oq) {
    // Rewrite each clause. Along the way, recognize and n-gram sequences of
    // terms.
    //
    SpanQuery[] clauses = oq.getClauses();
    Vector terms = new Vector();
    Vector newClauses = new Vector();
    boolean anyChanges = false;

    for (int i = 0; i < clauses.length; i++) {
      SpanQuery clause = (SpanQuery) rewriteQuery(clauses[i]);
      if (clause != clauses[i])
        anyChanges = true;
      if (clause == null)
        continue;

      // Skip stop-words.
      if (stopSet.contains(extractTermText(clause))) {
        removedTerms.add(extractTermText(clause));
        anyChanges = true;
        continue;
      }

      // Retain everything else.
      newClauses.add(clause);
    } // for i

    // If no changes, just return the original query.
    if (!anyChanges)
      return oq;

    // If no clauses, let the caller know they can delete this query.
    if (newClauses.isEmpty())
      return null;

    // If we have only one clause, return just that. Pass on the parent's
    // boost to the only child.
    //
    if (newClauses.size() == 1)
      return combineBoost(oq, (Query) newClauses.elementAt(0));

    // Construct a new 'or' query joining all the rewritten clauses.
    SpanQuery[] newArray = new SpanQuery[newClauses.size()];
    return copyBoost(oq, new SpanOrQuery((SpanQuery[]) newClauses
        .toArray(newArray)));

  } // rewriteOr()

  /**
   * Rewrite a span-based NOT query. The procedure in this case is simple:
   * simply rewrite both the include and exclude clauses.
   * 
   * @param nq  The query to rewrite
   * @return    Rewritten version, or 'nq' unchanged if no changed needed.
   */
  private Query rewriteNot(SpanChunkedNotQuery nq) {
    // Rewrite the sub-queries
    SpanQuery include = (SpanQuery) rewriteQuery(nq.getInclude());
    SpanQuery exclude = (SpanQuery) rewriteQuery(nq.getExclude());

    // If the sub-queries didn't change, then neither does this NOT.
    if (include == nq.getInclude() && exclude == nq.getExclude())
      return nq;

    // Make a new NOT query
    Query newq = new SpanChunkedNotQuery(include, exclude, nq.getSlop());
    copyBoost(nq, newq);
    return newq;

  } // rewriteNot()

  /**
   * Rewrite a span dechunking query. If's very simple: simply rewrite the
   * clause the query wraps.
   * 
   * @param nq  The query to rewrite
   * @return    Rewritten version, or 'nq' unchanged if no changed needed.
   */
  private Query rewriteDechunk(SpanDechunkingQuery nq) {
    // Rewrite the sub-queries
    SpanQuery sub = (SpanQuery) rewriteQuery(nq.getWrapped());

    // If the sub-query didn't change, then neither does the main query.
    if (sub == nq.getWrapped())
      return nq;

    // Make a new dechunking query
    Query newq = new SpanDechunkingQuery(sub);
    copyBoost(nq, newq);
    return newq;

  } // rewriteDechunk()

  /**
   * Rewrite a span dechunking query. If's very simple: simply rewrite the
   * clause the query wraps.
   * 
   * @param stq  The query to rewrite
   * @return     Rewritten version, or 'nq' unchanged if no changed needed.
   */
  private Query rewriteSecType(SpanSectionTypeQuery stq) {
    // Rewrite the sub-queries
    SpanQuery textQuery = (SpanQuery) rewriteQuery(stq.getTextQuery());
    SpanQuery secTypeQuery = (SpanQuery) rewriteQuery(stq.getSectionTypeQuery());

    // If the sub-queries didn't change, then neither does the main query.
    if (textQuery == stq.getTextQuery()
        && secTypeQuery == stq.getSectionTypeQuery())
      return stq;

    // Make a new dechunking query
    Query newq = new SpanSectionTypeQuery(textQuery, secTypeQuery);
    copyBoost(stq, newq);
    return newq;

  } // rewriteSecType()

  /**
   * Removes stop words from a set of consecutive queries by combining
   * them with adjacent non-stop-words.
   * 
   * @param queryVec    Vector of queries to work on
   * @param slop        zero for exact matching, non-zero for 'near' matching.
   * @return            true if any modification was made to the vector
   */
  boolean ngramQueries(Vector queryVec, int slop) {
    assert queryVec.size() > 0 : "cannot ngram empty list";

    // Get a handy array of the queries.
    int nQueries = queryVec.size();
    Query[] queries = new Query[nQueries];
    for (int i = 0; i < nQueries; i++)
      queries[i] = (Query) queryVec.elementAt(i);

    // Extract the term text from each query.
    String[] terms = new String[nQueries];
    for (int i = 0; i < nQueries; i++)
      terms[i] = extractTermText(queries[i]);

    // If there's only one query, and it's not a stop word, the we have 
    // nothing to do.
    //
    if (nQueries == 1 && !stopSet.contains(terms[0]))
      return false;

    // Find out if none of the queries are stop words (so we can take the easy
    // way out). 
    //
    // Along the way, make note of the stop words so we can later 
    // inform the user (since we're going to remove them one way or another.)
    //
    // Also, count the max # of consecutive stop words.
    //
    int nStopWords = 0;
    int consecStopWords = 0;
    int maxConsecStopWords = 0;
    for (int i = 0; i < nQueries; i++) {
      if (!stopSet.contains(terms[i])) {
        consecStopWords = 0;
        continue;
      }
      removedTerms.add(terms[i]);
      nStopWords++;
      consecStopWords++;
      if (consecStopWords > maxConsecStopWords)
        maxConsecStopWords = consecStopWords;
    }

    // No stop words? Nothing to do.
    if (nStopWords == 0)
      return false;

    // If the query is entirely stop words, it's not going to produce
    // anything useful. Just clear the query list and let the caller know 
    // we have made a change.
    //
    if (nStopWords == nQueries) {
      queryVec.clear();
      return true;
    }

    // At this point, we know the query has at least one stop word and
    // at least one real word.
    //
    // We have three cases to worry about:
    //    (1) Phrase search
    //    (2) Near search with max 2 consecutive stop words
    //    (3) Near search with 3 or more consecutive stop words.
    //
    queryVec.clear();

    // Case (1): Phrase search
    //
    if (slop == 0)
      queryVec.add(ngramTermsExact(queries, terms, slop));

    // Case (2): Near search with max 2 consecutive stop words
    else if (maxConsecStopWords <= 2)
      queryVec.add(ngramTermsInexact(queries, terms, slop));

    // Case (3): Near search with 3 or more consecutive stop words
    else {
      // This case is a bit strange. Since doing an inexact query will end
      // up eliminating at least one stop word, we also do an exact query,
      // and let the best match win.
      //
      SpanQuery[] both = new SpanQuery[2];
      both[0] = ngramTermsExact(queries, terms, slop);
      both[1] = ngramTermsInexact(queries, terms, slop);
      queryVec.add(new SpanOrQuery(both));
    }

    // We definitely made changes
    return true;
  } // ngramQueries()

  /**
   * Given a sequence of terms consisting of mixed stop and real words,
   * figure out the ngrammed sequence that will give hits on at least
   * the real words, and give priority to ones that are near the closest
   * stop words.
   * 
   * Examples:  "man of the world" 
   *                    -> "(man or man-of) near (the-world or world)"
   *            "hello there"
   *                    -> "hello there"
   *            "it is not a problem"  
   *                    -> "(a-problem or problem)"
   * 
   * @param queries Original queries in the sequence
   * @param terms   Corresponding term text of each query
   * @param slop    Sloppiness for the resulting query
   * 
   * @return        A new query possibly containing n-grams
   */
  private SpanQuery ngramTermsInexact(Query[] queries, String[] terms, int slop) {
    SpanQuery[] clauses = new SpanQuery[terms.length * 2];
    int nClauses = 0;

    // Process each term in turn, looking at its relation to the next term.
    for (int i = 0; i < terms.length; i++) {
      // There are six cases to consider:
      // (1) real followed by nothing
      // (2) real followed by real
      // (3) real followed by stop
      // (4) stop followed by nothing
      // (5) stop followed by real
      // (6) stop followed by stop
      //
      // First, handle cases (1), (2) and (3), which all start with a 
      // real word.
      //
      if (!stopSet.contains(terms[i])) {
        // If the previous term was a stop-word, then this real
        // word has already been incorporated. Skip it.
        //
        if (i > 0 && stopSet.contains(terms[i - 1]))
          continue;

        // Case 1 and 2: Real followed by nothing or another real word. 
        //               In these cases, there's no need to glom.
        //
        if (i == terms.length - 1 || !stopSet.contains(terms[i + 1])) {
          clauses[nClauses++] = convertToSpanQuery(queries[i]);
          continue;
        }

        // Case 3: Real followed by stop. In this case, we make an
        // OR-query, like this: (real OR real-stop)
        //
        SpanQuery[] both = new SpanQuery[2];
        both[0] = convertToSpanQuery(queries[i]);
        both[1] = convertToSpanQuery(glomQueries(queries[i], queries[i + 1]));
        clauses[nClauses++] = new SpanOrQuery(both);
        continue;
      }

      // Now handle cases (4), (5) and (6) that begin with a stop word.
      //
      // Case (4): Stop followed by nothing. Just drop the stop word.
      //
      if (i == terms.length - 1)
        continue;

      // Case (5): Stop followed by real. In this case, we make an OR
      //           query, like this: (stop-real OR real)
      //
      if (!stopSet.contains(terms[i + 1])) {
        SpanQuery[] both = new SpanQuery[2];
        both[0] = convertToSpanQuery(glomQueries(queries[i], queries[i + 1]));
        both[1] = convertToSpanQuery(queries[i + 1]);
        clauses[nClauses++] = new SpanOrQuery(both);
        continue;
      }

      // Case (6): Stop followed by stop. Throw it away.
      continue;
    } // for i

    // If we ended up with only one clause, just return that.
    if (nClauses == 1)
      return clauses[0];

    // Otherwise, join them all together in a "near" query.
    SpanQuery[] resized = new SpanQuery[nClauses];
    System.arraycopy(clauses, 0, resized, 0, nClauses);
    return new SpanNearQuery(resized, slop, false);
  } // ngramTermsInexact()

  /**
   * Converts non-span queries to span queries, and passes span queries through
   * unchanged.
   * 
   * @param q   Query to convert (span or non-span)
   * @return    Equivalent SpanQuery.
   */
  private SpanQuery convertToSpanQuery(Query q) {
    if (q instanceof SpanQuery)
      return (SpanQuery) q;
    if (q instanceof TermQuery) {
      Term t = ((TermQuery) q).getTerm();
      return (SpanQuery) copyBoost(q, new SpanTermQuery(t, stopSet));
    }
    assert false : "case not handled";
    return null;
  } // convertToSpanQuery()

  /**
   * Construct a term given its text and field name. This function is used
   * instead of Term's constructor to add an extra check that the text
   * is never a stop word.
   * 
   * @param text    Text for the new term
   * @param field   Field being queried
   * 
   * @return        A properly constructed Term, never a stop-word.
   */
  private Term newTerm(String text, String field) {
    assert !stopSet.contains(text) : "cannot directly query a stop-word";
    return new Term(text, field);
  } // newTerm()

  /**
   * Given a sequence of terms consisting of mixed stop and real words,
   * figure out the ngrammed sequence required to get an exact match with
   * the index.
   * 
   * Examples:  "man of the world"     -> "man-of of-the the-world"
   *            "hello there"          -> "hello there"
   *            "it is not a problem"  -> "it-is is-not not-a a-problem"
   * 
   * @param queries Original queries in the sequence
   * @param terms   Corresponding term text of each query
   * @param slop    Sloppiness for the resulting query
   * 
   * @return        A new query possibly containing n-grams
   */
  private SpanQuery ngramTermsExact(Query[] queries, String[] terms, int slop) {
    Vector newQueries = new Vector(queries.length * 2);

    // Process each term in turn, looking at its relation to the next term.
    for (int i = 0; i < terms.length; i++) {

      // There are six cases to consider:
      // (1) real followed by nothing
      // (2) real followed by real
      // (3) real followed by stop
      // (4) stop followed by nothing
      // (5) stop followed by real
      // (6) stop followed by stop
      //
      // First, handle cases (1), (2) and (3), which all start with a 
      // real word.
      //
      if (!stopSet.contains(terms[i])) {

        // Cases 1 and 2: Real followed by nothing or another real 
        //                word. In this case, there's no need to glom.
        //
        if (i == terms.length - 1 || !stopSet.contains(terms[i + 1])) {
          // If the previous term was a stop-word, then this real
          // word has already been incorporated. Skip it.
          //
          if (i > 0 && stopSet.contains(terms[i - 1]))
            continue;
          newQueries.add(queries[i]);
          continue;
        }

        // Case 3: Real followed by stop. In this case, we stick the
        // real and the stop together.
        //
        newQueries.add(glomQueries(queries[i], queries[i + 1]));
        continue;
      }

      // Now handle cases (4), (5) and (6) that start with a stop word.
      //
      // Case (4): stop word followed by nothing. Just throw it away.
      //           Don't worry, it should have been incorporated into 
      //           the previous glommed term.
      //
      if (i == terms.length - 1)
        continue;

      // Cases (5) and (6): stop word followed by anything else. Just 
      //                    glom the stop word with whatever comes after.
      //
      newQueries.add(glomQueries(queries[i], queries[i + 1]));
    } // for i

    // Convert the vector of queries to a handy array.
    SpanQuery[] newArray = new SpanQuery[newQueries.size()];
    newQueries.toArray(newArray);

    // And finally, make the "near" query that will join them all.
    return new SpanNearQuery(newArray, slop, false);
  } // ngramTermsExact()

  /**
   * Joins a stop word to a real word, or vice-versa. Also handles more complex
   * cases, like joining a stop-word to an OR query.
   * 
   * Examples:  the rabbit -> the-rabbit
   *            the (white OR beige) -> the-white OR the-beige
   * 
   * @param q1  First query
   * @param q2  Second query
   * @return    A query representing the join.
   */
  private Query glomQueries(Query q1, Query q2) {
    // If they're both terms, our work is easy.
    if (q1 instanceof SpanTermQuery && q2 instanceof SpanTermQuery) {
      SpanTermQuery st1 = (SpanTermQuery) q1;
      SpanTermQuery st2 = (SpanTermQuery) q2;

      Term t = newTerm(st1.getField(), st1.getTerm().text() + "~"
          + st2.getTerm().text());
      return copyBoost(st1, st2, new SpanTermQuery(t, stopSet));
    }

    if (q1 instanceof TermQuery && q2 instanceof TermQuery) {
      TermQuery t1 = (TermQuery) q1;
      TermQuery t2 = (TermQuery) q2;

      Term t = newTerm(t1.getTerm().field(), t1.getTerm().text() + "~"
          + t2.getTerm().text());
      return copyBoost(t1, t2, new TermQuery(t));
    }

    // If joining a term to an OR query or vice-versa, we have a bunch to do.
    if (q1 instanceof SpanTermQuery && q2 instanceof SpanOrQuery)
      return glomInside((SpanOrQuery) q2, (SpanTermQuery) q1, true);
    if (q1 instanceof SpanOrQuery && q2 instanceof SpanTermQuery)
      return glomInside((SpanOrQuery) q1, (SpanTermQuery) q2, false);

    // If joining a term to a NOT query, only glom it's include clause (the
    // exclude clause is independent.)
    //
    if (q1 instanceof SpanTermQuery && q2 instanceof SpanChunkedNotQuery)
      return glomInside((SpanChunkedNotQuery) q2, (SpanTermQuery) q1, true);
    if (q1 instanceof SpanChunkedNotQuery && q2 instanceof SpanTermQuery)
      return glomInside((SpanChunkedNotQuery) q1, (SpanTermQuery) q2, false);

    // Don't mess with near queries.
    if (q1 instanceof SpanTermQuery && q2 instanceof SpanNearQuery)
      return q2;
    if (q1 instanceof SpanNearQuery && q2 instanceof SpanTermQuery)
      return q1;

    Trace.debug("q1=" + q1.toString());
    Trace.debug("q2=" + q2.toString());
    assert false : "case not handled yet";
    return null;
  } // glomQueries()

  /**
   * Gloms the term onto each clause within an OR query.
   * 
   * @param oq      Query to glom into
   * @param term    Term to glom on
   * @param before  true to prepend the term, false to append.
   * @return        A new glommed query.
   */
  private SpanQuery glomInside(SpanOrQuery oq, SpanTermQuery term,
      boolean before) {
    SpanQuery[] clauses = oq.getClauses();
    boolean anyChanges = false;
    for (int i = 0; i < clauses.length; i++) {
      if (clauses[i] instanceof SpanTermQuery) {
        String ctText = extractTermText(clauses[i]);
        String newText = before ? (extractTermText(term) + "~" + ctText)
                                : (ctText + "~" + extractTermText(term));
        SpanQuery oldClause = clauses[i];
        clauses[i] = new SpanTermQuery(
            newTerm(term.getTerm().field(), newText), stopSet);
        copyBoost(oldClause, term, clauses[i]);
        anyChanges = true;
      } 
      else if (clauses[i] instanceof SpanOrQuery) {
        SpanQuery newq = glomInside((SpanOrQuery) clauses[i], term, before);
        if (newq != oq) {
          clauses[i] = newq;
          anyChanges = true;
        }
      } 
      else
        assert false : "case not handled";
    } // for i

    // No changes? Return the unaltered original query.
    if (!anyChanges)
      return oq;

    // All done!
    return (SpanQuery) copyBoost(oq, new SpanOrQuery(clauses));
  } // glomInside()

  /**
   * Gloms the term onto each clause within a NOT query.
   * 
   * @param nq      Query to glom into
   * @param term    Term to glom on
   * @param before  true to prepend the term, false to append.
   * @return        A new glommed query.
   */
  private SpanQuery glomInside(SpanChunkedNotQuery nq, SpanTermQuery term,
      boolean before) {
    // Only glom into the 'include' clause. The 'exclude' clause is entirely
    // independent.
    //
    SpanQuery newInclude;
    if (before)
      newInclude = (SpanQuery) glomQueries(term, nq.getInclude());
    else
      newInclude = (SpanQuery) glomQueries(nq.getInclude(), term);

    // If no change was made to the 'include' clause, then we needn't change
    // the NOT query.
    //
    if (newInclude == nq.getInclude())
      return nq;

    // Make a new NOT query then.
    return (SpanQuery) copyBoost(nq, 
        new SpanChunkedNotQuery(newInclude, nq.getExclude(), nq.getSlop()));
  } // glomInside()

  /**
   * Given a term, term query, span term query (or plain string), extract
   * the term text. This method is handy so we don't have to sprinkle if
   * statements everywhere we need to get the text.
   * 
   * @param obj   String, Term, TermQuery, or SpanTermQuery to check
   * @return      text of the term
   */
  private String extractTermText(Object obj) {
    if (obj instanceof String)
      return (String) obj;
    Term t = extractTerm(obj);
    if (t == null)
      return "";
    return t.text();
  } // extractText()

  /**
   * Given a term query, span term query (or plain term), extract
   * the Term itself. This method is handy so we don't have to sprinkle if
   * statements everywhere we need to get the term from a query.
   * 
   * @param obj   Term, TermQuery, or SpanTermQuery to check
   * @return      the Term
   */
  private Term extractTerm(Object obj) {
    if (obj instanceof Term)
      return (Term) obj;
    if (obj instanceof TermQuery)
      return ((TermQuery) obj).getTerm();
    if (obj instanceof SpanTermQuery)
      return ((SpanTermQuery) obj).getTerm();
    return null;
  } // extractTerm()

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
  private Query copyBoost(Query oldQuery, Query newQuery) {
    newQuery.setBoost(oldQuery.getBoost());
    if (newQuery instanceof SpanQuery && oldQuery instanceof SpanQuery) {
      ((SpanQuery) newQuery).setSpanRecording(
          ((SpanQuery) oldQuery).getSpanRecording());
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
  private Query copyBoost(Query oldQuery1, Query oldQuery2, Query newQuery) {
    newQuery.setBoost(Math.max(oldQuery1.getBoost(), oldQuery2.getBoost()));
    if (newQuery instanceof SpanQuery) {
      ((SpanQuery) newQuery).setSpanRecording(
          Math.max(((SpanQuery) oldQuery1).getSpanRecording(), 
                   ((SpanQuery) oldQuery2).getSpanRecording()));
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
  private Query combineBoost(Query oldQuery, Query newQuery) {
    newQuery.setBoost(oldQuery.getBoost() * newQuery.getBoost());
    if (newQuery instanceof SpanQuery && oldQuery instanceof SpanQuery) {
      ((SpanQuery)newQuery).setSpanRecording(
          Math.max(((SpanQuery)oldQuery).getSpanRecording(), 
                   ((SpanQuery)newQuery).getSpanRecording()));
    }
    return newQuery;
  } // copyBoost()

  /**
   * Basic regression test
   */
  public static final Tester tester = new Tester("NgramStopFilter") {
    private Set stopSet = new HashSet();

    private String queryToText(Query q) {
      StringBuffer buf = new StringBuffer();

      if (q.getBoost() != 1.0f) {
        float boost = q.getBoost();
        q.setBoost(1.0f);
        buf.append(queryToText(q));
        q.setBoost(boost);
        buf.append("^" + (int) boost);
        return buf.toString();
      }

      if (q instanceof SpanTermQuery)
        return ((SpanTermQuery) q).getTerm().text();

      if (q instanceof TermQuery)
        return ((TermQuery) q).getTerm().text();

      if (q instanceof SpanNearQuery) {
        SpanQuery[] clauses = ((SpanNearQuery) q).getClauses();
        int slop = ((SpanNearQuery) q).getSlop();

        buf.append("\"");
        for (int i = 0; i < clauses.length; i++) {
          if (buf.length() > 1)
            buf.append(" ");
          buf.append(queryToText(clauses[i]));
        }
        buf.append("\"");
        if (slop != 0)
          buf.append("~" + slop);
        return buf.toString();
      }

      if (q instanceof SpanOrQuery) {
        SpanQuery[] clauses = ((SpanOrQuery) q).getClauses();

        buf.append("(");
        for (int i = 0; i < clauses.length; i++) {
          if (buf.length() > 1)
            buf.append(" OR ");
          buf.append(queryToText(clauses[i]));
        }
        buf.append(")");
        return buf.toString();
      }

      if (q instanceof SpanChunkedNotQuery) {
        SpanChunkedNotQuery nq = (SpanChunkedNotQuery) q;
        buf.append("(");
        buf.append(queryToText(nq.getInclude()));
        buf.append(" NOT ");
        buf.append(queryToText(nq.getExclude()));
        buf.append(")~" + nq.getSlop());
        return buf.toString();
      }

      if (q instanceof BooleanQuery) {
        BooleanClause[] clauses = ((BooleanQuery) q).getClauses();
        buf.append("(");
        for (int i = 0; i < clauses.length; i++) {
          if (buf.length() > 1)
            buf.append(" ");
          if (clauses[i].required)
            buf.append("+");
          else if (clauses[i].prohibited)
            buf.append("-");
          buf.append(queryToText(clauses[i].query));
        }
        buf.append(")");
        return buf.toString();
      }

      return q.toString();
    } // queryToText()

    private SpanQuery term(String text) {
      return new SpanTermQuery(new Term("text", text), stopSet);
    }

    private SpanQuery[] terms(String text) {
      Vector v = new Vector();
      StringTokenizer st = new StringTokenizer(text);
      while (st.hasMoreTokens())
        v.add(term(st.nextToken()));
      return (SpanQuery[]) v.toArray(new SpanQuery[v.size()]);
    }

    private SpanQuery or(SpanQuery[] clauses) {
      return new SpanOrQuery(clauses);
    }

    private SpanQuery not(int slop, SpanQuery include, SpanQuery exclude) {
      return new SpanChunkedNotQuery(include, exclude, slop);
    }

    private SpanQuery near(int slop, SpanQuery[] clauses) {
      return new SpanNearQuery(clauses, slop, false);
    }

    private SpanQuery and(SpanQuery[] clauses) {
      return near(20, clauses);
    }

    private SpanQuery phrase(SpanQuery[] clauses) {
      return near(0, clauses);
    }

    private SpanQuery[] join(SpanQuery q1, SpanQuery q2) {
      SpanQuery[] array = new SpanQuery[2];
      array[0] = q1;
      array[1] = q2;
      return array;
    }

    private SpanQuery[] join(SpanQuery q1, SpanQuery q2, SpanQuery q3) {
      SpanQuery[] array = new SpanQuery[3];
      array[0] = q1;
      array[1] = q2;
      array[2] = q3;
      return array;
    }

    private Query bool(Query q1, boolean require1, boolean prohibit1, Query q2,
        boolean require2, boolean prohibit2, Query q3, boolean require3,
        boolean prohibit3) {
      BooleanQuery q = new BooleanQuery();
      q.add(q1, require1, prohibit1);
      q.add(q2, require2, prohibit2);
      if (q3 != null)
        q.add(q3, require3, prohibit3);
      return q;
    }

    private Query bool(Query q1, boolean require1, boolean prohibit1, Query q2,
        boolean require2, boolean prohibit2) {
      return bool(q1, require1, prohibit1, q2, require2, prohibit2, null,
          false, false);
    }

    private Query regTerm(String text) {
      return new TermQuery(new Term("text", text));
    }

    private SpanQuery boost(float factor, SpanQuery q) {
      q.setBoost(factor);
      return q;
    }

    private Query boost(float factor, Query q) {
      q.setBoost(factor);
      return q;
    }

    private void testQuery(Query query, String expectedResult) {
      NgramQueryRewriter rewriter = new NgramQueryRewriter(stopSet, 20);
      Query newQ = rewriter.rewriteQuery(query);
      String result = queryToText(newQ);
      Trace.debug(queryToText(query) + " --> " + result);
      assert result.equals(expectedResult);
    } // testQuery()

    private void testUnchanged(Query query) {
      NgramQueryRewriter rewriter = new NgramQueryRewriter(stopSet, 20);
      Query newQ = rewriter.rewriteQuery(query);
      assert query == newQ;
    } // testQuery()

    /**
     * Run the test.
     */
    protected void testImpl() {
      stopSet = NgramQueryRewriter.makeStopSet("a and it is the of");

      ////////////////////////////////////////////////////////////////////////
      // PHRASE QUERIES
      ////////////////////////////////////////////////////////////////////////

      // Start with some simple ones
      testUnchanged(phrase(terms("hello there")));
      testQuery(phrase(terms("man of war")), "\"man~of of~war\"");
      testQuery(phrase(terms("man of the world")),
          "\"man~of of~the the~world\"");
      testQuery(phrase(terms("when it is a problem")),
          "\"when~it it~is is~a a~problem\"");
      testQuery(phrase(terms("and martha is")), "\"and~martha martha~is\"");

      // Test phrase queries with non~term clauses.
      testQuery(phrase(join(term("the"), or(terms("white beige")),
          term("rabbit"))), "\"(the~white OR the~beige) rabbit\"");

      // It would be a huge pain to deal with trying to apply inner stop
      // words from an OR query to the outer terms. So we just don't.
      //
      testQuery(phrase(join(term("eat"), or(terms("the a")), term("rabbit"))),
          "\"eat rabbit\"");

      // Test boost propagation
      testQuery(phrase(join(term("eat"), boost(5, term("the")), term("wave"))),
          "\"eat~the^5 the~wave^5\"");
      testQuery(phrase(join(term("eat"), term("the"), boost(5, term("wave")))),
          "\"eat~the the~wave^5\"");

      ////////////////////////////////////////////////////////////////////////
      // AND QUERIES
      ////////////////////////////////////////////////////////////////////////

      // Start with simple ones
      testUnchanged(and(terms("hello there")));
      testQuery(and(terms("man of war")),
          "\"(man OR man~of) (of~war OR war)\"~20");

      // Test AND queries with non~term clauses.
      testQuery(
          and(join(term("the"), or(terms("white beige")), term("rabbit"))),
          "\"((the~white OR the~beige) OR (white OR beige)) rabbit\"~20");

      // Test boost propagation
      testQuery(boost(2, and(join(term("eat"), boost(5, term("the")),
          term("wave")))), "\"(eat OR eat~the^5) (the~wave^5 OR wave)\"~20^2");
      testQuery(boost(5, and(join(boost(2, term("eat")), boost(3,
          or(terms("the a")))))), "eat^10");

      ////////////////////////////////////////////////////////////////////////
      // NEAR QUERIES
      ////////////////////////////////////////////////////////////////////////

      testUnchanged(near(5, terms("three freezy trees")));
      testUnchanged(near(5, join(term("three"), or(terms("freezy breezy")),
          term("trees"))));
      testQuery(near(5, terms("man of war")),
          "\"(man OR man~of) (of~war OR war)\"~5");
      testQuery(near(5, terms("when it is a problem")),
          "(\"when~it it~is is~a a~problem\"~5 OR "
              + "\"(when OR when~it) (a~problem OR problem)\"~5)");
      testQuery(near(5, terms("it is a problem")),
          "(\"it~is is~a a~problem\"~5 OR (a~problem OR problem))");
      testQuery(near(5, terms("when it is a")),
          "(\"when~it it~is is~a\"~5 OR (when OR when~it))");

      // Try some near queries with non~term clauses.
      testQuery(near(5, join(or(terms("shake bake")), term("it"))),
          "((shake OR bake) OR (shake~it OR bake~it))");
      testQuery(
          near(5, join(or(terms("shake bake")), term("it"), term("now"))),
          "\"((shake OR bake) OR (shake~it OR bake~it)) "
              + "(it~now OR now)\"~5");
      testQuery(near(5, join(term("jeff"), or(terms("shakes bakes")),
          term("it"))),
          "\"jeff ((shakes OR bakes) OR (shakes~it OR bakes~it))\"~5");

      // Test boost propagation
      testQuery(boost(2, near(5, join(boost(3, or(join(boost(4, term("shake")),
          boost(5, term("bake"))))), boost(6, term("it")),
          boost(7, term("now"))))), "\"((shake^4 OR bake^5)^3 OR "
          + "(shake~it^6 OR bake~it^6)^3) " + "(it~now^7 OR now^7)\"~5^2");
      testQuery(boost(7, near(5, join(boost(6, or(join(boost(5, term("shake")),
          boost(4, term("bake"))))), boost(3, term("it")),
          boost(2, term("now"))))), "\"((shake^5 OR bake^4)^6 OR "
          + "(shake~it^5 OR bake~it^4)^6) " + "(it~now^3 OR now^2)\"~5^7");

      ////////////////////////////////////////////////////////////////////////
      // OR QUERIES
      ////////////////////////////////////////////////////////////////////////

      testUnchanged(or(join(term("foo"), and(terms("bar gaz")))));
      testQuery(or(join(term("arf"), and(terms("the dog")), term("said"))),
          "(arf OR (the~dog OR dog) OR said)");
      testQuery(or(join(term("the"), and(terms("very nice")), term("rabbit"))),
          "(\"very nice\"~20 OR rabbit)");

      // Test boost propagation
      testQuery(boost(5, or(join(boost(2, term("the")),
          boost(3, term("happy")), boost(4, term("couple"))))),
          "(happy^3 OR couple^4)^5");
      testQuery(boost(5, or(join(boost(2, term("the")),
          boost(3, term("happy")), boost(4, term("it"))))), "happy^15");

      ////////////////////////////////////////////////////////////////////////
      // NOT QUERIES
      ////////////////////////////////////////////////////////////////////////
      testUnchanged(not(5, term("hello"), term("there")));
      testQuery(not(5, and(terms("the cow")), and(terms("the dog"))),
          "((the~cow OR cow) NOT (the~dog OR dog))~5");
      testQuery(and(join(term("like"), term("a"), not(5, term("cow"),
          term("dog")))),
          "\"(like OR like~a) ((a~cow NOT dog)~5 OR (cow NOT dog)~5)\"~20");

      // A couple tests anticipating future support for case sensitivity and 
      // accent insensitivity.
      //
      testQuery(and(join(term("the"), not(0, term("hat"),
          or(terms("hat~p hat~c"))), term("trick"))),
          "\"((the~hat NOT (hat~p OR hat~c))~0 OR "
              + "(hat NOT (hat~p OR hat~c))~0) trick\"~20");
      testQuery(and(join(term("hank"), not(0, term("hat"),
          or(terms("hat~p hat~c"))), term("is"))),
          "\"hank ((hat NOT (hat~p OR hat~c))~0 OR "
              + "(hat~is NOT (hat~p OR hat~c))~0)\"~20");

      ////////////////////////////////////////////////////////////////////////
      // BOOLEAN QUERIES
      ////////////////////////////////////////////////////////////////////////

      testUnchanged(bool(regTerm("hello"), true, false, regTerm("kitty"),
          false, true, regTerm("pencil"), true, false));

      testQuery(bool(regTerm("this"), true, false, regTerm("is"), true, false,
          regTerm("fun"), true, false),
          "\"(this OR this~is) (is~fun OR fun)\"~20");
      testQuery(bool(regTerm("cats"), true, false, regTerm("and"), false, true,
          regTerm("hats"), true, false), "(+cats +hats)");
      testQuery(bool(regTerm("cats"), true, false, regTerm("and"), false,
          false, regTerm("hats"), true, false), "(+cats +hats)");
      testQuery(bool(regTerm("is"), true, false, regTerm("it"), true, false,
          regTerm("fun"), false, false), "(fun)");

      // Test BooleanQuery with non~term clauses
      testQuery(bool(regTerm("whip"), true, false, or(terms("it them")), true,
          false, regTerm("good"), true, false), "(+whip +them +good)");
      testQuery(bool(regTerm("whip"), true, false, and(terms("the dog")), true,
          false, regTerm("good"), true, false),
          "(+whip +(the~dog OR dog) +good)");
      testQuery(bool(regTerm("whip"), true, false, and(terms("the funny dog")),
          true, false, regTerm("good"), true, false),
          "(+whip +\"(the~funny OR funny) dog\"~20 +good)");

      // Test boost propagation
      testQuery(
          boost(2, bool(boost(3, regTerm("pick")), true, false, boost(4,
              regTerm("the")), true, false, boost(5, regTerm("dirt")), true,
              false)), "\"(pick^3 OR pick~the^4) (the~dirt^5 OR dirt^5)\"~20^2");
      testQuery(boost(2,
          bool(boost(3, regTerm("the")), true, false,
              boost(4, regTerm("dirt")), true, false, boost(5, regTerm("man")),
              false, false)), "(+(the~dirt^4 OR dirt^4) man^5)^2");
      testQuery(boost(2, bool(boost(3, regTerm("it")), false, false, boost(4,
          regTerm("and")), false, false, boost(5, regTerm("harry")), true,
          false)), "harry^10");

    } // testImpl()
  }; // Tester

} // class NgramQueryRewriter
