package org.apache.lucene.bigram;

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
import java.util.Vector;

import org.apache.lucene.chunk.SpanChunkedNotQuery;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryRewriter;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;

/**
 * Rewrites a query to eliminate stop words by combining them with
 * adjacent non-stop-words, forming "bi-grams" (or bi-grams with 2 words). 
 * This is a fairly in-depth process, as bi-gramming across NEAR and OR 
 * queries is complex.
 */
public class BigramQueryRewriter extends QueryRewriter {
  /** Set of stop-words (e.g. "the", "a", "and", etc.) to remove */
  protected Set stopSet;

  /** Maximum slop to allow in a query, based on the index being queried */
  protected int maxSlop;

  /** Keeps track of all stop-words removed from the query */
  protected HashSet removedTerms = new HashSet();

  /**
   * Constructs a rewriter using the given stopword set.
   * 
   * @param stopSet   Set of stopwords to remove or bi-gram. This can be
   *                  constructed easily by calling 
   *                  {@link #makeStopSet(String)}.
   * @param maxSlop   Maximum slop to allow in a query, based on the index
   *                  being queried.
   */
  public BigramQueryRewriter(Set stopSet, int maxSlop) {
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
   *                    an {@link BigramQueryRewriter}.
   */
  public static Set makeStopSet(String stopWords) {
    return BigramStopFilter.makeStopSet(stopWords);
  } // makeStopSet()

  /**
   * Determines if the given string is an bi-gram of a real word with a
   * stop-word.
   * 
   * @param stopWords   The set of stop-words
   * @param str         The string to check
   * @return            true if it's an bi-gram
   */
  public static boolean isBigram(Set stopWords, String str) {
    int pos = str.indexOf('~');

    // A tilde tells us if it's an bigram.
    if (pos < 0)
      return false;

    // Let's do some sanity checking
    assert pos > 0 && pos < str.length() - 1 : "bi-gram tilde cannot be at start or end of term";
    String before = str.substring(0, pos);
    String after = str.substring(pos + 1);
    assert stopWords.contains(before) || stopWords.contains(after);

    // It's an bi-gram.
    return true;
  } // isBigram()

  /**
   * Rewrite a BooleanQuery. Prohibited or allowed (not required) clauses
   * that are single stop words will be removed. Required clauses will
   * have bi-gramming applied.
   * 
   * @param bq  The query to rewrite
   * @return    Rewritten version, or 'bq' unchanged if no changed needed.
   */
  protected Query rewrite(BooleanQuery bq) {
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

      // Single stop words must be removed. Make sure to add them to the 
      // removed list so the user will be notified.
      //
      if (stopSet.contains(extractTermText(clauses[i].query))) {
        removedTerms.add(extractTermText(clauses[i].query));
        anyChange = true;
        continue;
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

    // NOTE: 
    // Do NOT bi-gram the required clauses, because they don't have any real
    // order, and besides, they might be from entirely different fields.

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

  } // rewrite()

  /**
   * Rewrite a span NEAR query. Stop words will be bi-grammed into adjacent
   * terms.
   * 
   * @param nq  The query to rewrite
   * @return    Rewritten version, or 'nq' unchanged if no changed needed.
   */
  protected Query rewrite(SpanNearQuery nq) {
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

    // Bi-gram the rewritten clauses.
    anyChanges |= bigramQueries(newClauses, nq.getSlop());

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

  } // rewrite()

  /**
   * Rewrite a span-based OR query. The procedure in this case is simple:
   * remove all stop words, with no bi-gramming performed.
   * 
   * @param oq  The query to rewrite
   * @return    Rewritten version, or 'oq' unchanged if no changed needed.
   */
  protected Query rewrite(SpanOrQuery oq) {
    // Rewrite each clause. Along the way, recognize and bi-gram sequences of
    // terms.
    //
    SpanQuery[] clauses = oq.getClauses();
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

  } // rewrite()
  
  /**
   * Removes stop words from a set of consecutive queries by combining
   * them with adjacent non-stop-words.
   * 
   * @param queryVec    Vector of queries to work on
   * @param slop        zero for exact matching, non-zero for 'near' matching.
   * @return            true if any modification was made to the vector
   */
  protected boolean bigramQueries(Vector queryVec, int slop) {
    assert queryVec.size() > 0 : "cannot bigram empty list";

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
      queryVec.add(bigramTermsExact(queries, terms, slop));

    // Case (2): Near search with max 2 consecutive stop words
    else if (maxConsecStopWords <= 2)
      queryVec.add(bigramTermsInexact(queries, terms, slop));

    // Case (3): Near search with 3 or more consecutive stop words
    else {
      // This case is a bit strange. Since doing an inexact query will end
      // up eliminating at least one stop word, we also do an exact query,
      // and let the best match win.
      //
      SpanQuery[] both = new SpanQuery[2];
      both[0] = bigramTermsExact(queries, terms, slop);
      both[1] = bigramTermsInexact(queries, terms, slop);
      queryVec.add(new SpanOrQuery(both));
    }

    // We definitely made changes
    return true;
  } // bigramQueries()

  /**
   * Given a sequence of terms consisting of mixed stop and real words,
   * figure out the bigrammed sequence that will give hits on at least
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
   * @return        A new query possibly containing bi-grams
   */
  protected SpanQuery bigramTermsInexact(Query[] queries, String[] terms, int slop) {
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
  } // bigramTermsInexact()

  /**
   * Converts non-span queries to span queries, and passes span queries through
   * unchanged.
   * 
   * @param q   Query to convert (span or non-span)
   * @return    Equivalent SpanQuery.
   */
  protected SpanQuery convertToSpanQuery(Query q) {
    if (q instanceof SpanQuery)
      return (SpanQuery) q;
    if (q instanceof TermQuery) {
      Term t = ((TermQuery) q).getTerm();
      int termLength = isBigram(stopSet, t.text()) ? 2 : 1;
      return (SpanQuery) copyBoost(q, new SpanTermQuery(t, termLength));
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
  protected Term newTerm(String field, String text) {
    assert !stopSet.contains(text) : "cannot directly query a stop-word";
    return new Term(field, text);
  } // newTerm()

  /**
   * Given a sequence of terms consisting of mixed stop and real words,
   * figure out the bigrammed sequence required to get an exact match with
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
   * @return        A new query possibly containing bi-grams
   */
  protected SpanQuery bigramTermsExact(Query[] queries, String[] terms, int slop) {
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
  } // bigramTermsExact()

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
  protected Query glomQueries(Query q1, Query q2) {
    // If they're both terms, our work is easy.
    if (q1 instanceof SpanTermQuery && q2 instanceof SpanTermQuery) {
      SpanTermQuery st1 = (SpanTermQuery) q1;
      SpanTermQuery st2 = (SpanTermQuery) q2;

      Term t = newTerm(st1.getField(), st1.getTerm().text() + "~"
          + st2.getTerm().text());
      int termLength = isBigram(stopSet, t.text()) ? 2 : 1;
      return copyBoost(st1, st2, new SpanTermQuery(t, termLength));
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
  protected SpanQuery glomInside(SpanOrQuery oq, SpanTermQuery term,
      boolean before) {
    SpanQuery[] clauses = oq.getClauses();
    boolean anyChanges = false;
    for (int i = 0; i < clauses.length; i++) {
      if (clauses[i] instanceof SpanTermQuery) {
        String ctText = extractTermText(clauses[i]);
        String newText = before ? (extractTermText(term) + "~" + ctText)
                                : (ctText + "~" + extractTermText(term));
        SpanQuery oldClause = clauses[i];
        int termLength = isBigram(stopSet, newText) ? 2 : 1;
        clauses[i] = new SpanTermQuery(
            newTerm(term.getTerm().field(), newText), termLength);
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
  protected SpanQuery glomInside(SpanChunkedNotQuery nq, SpanTermQuery term,
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
  protected String extractTermText(Object obj) {
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
  protected Term extractTerm(Object obj) {
    if (obj instanceof Term)
      return (Term) obj;
    if (obj instanceof TermQuery)
      return ((TermQuery) obj).getTerm();
    if (obj instanceof SpanTermQuery)
      return ((SpanTermQuery) obj).getTerm();
    return null;
  } // extractTerm()

} // class BigramQueryRewriter
