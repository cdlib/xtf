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

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.lucene.bigram.BigramQueryRewriter;
import org.apache.lucene.chunk.SpanChunkedNotQuery;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;

import org.cdlib.xtf.textEngine.SpanExactQuery;
import org.cdlib.xtf.util.Tester;
import org.cdlib.xtf.util.Trace;

/**
 * Rewrites a query to eliminate stop words by combining them with
 * adjacent non-stop-words, forming "bi-grams". This is a fairly in-depth
 * process, as bi-gramming across NEAR and OR queries is complex.
 */
public class XtfBigramQueryRewriter extends BigramQueryRewriter {

  /**
   * Constructs a rewriter using the given stopword set.
   * 
   * @param stopSet   Set of stopwords to remove or bi-gram. This can be
   *                  constructed easily by calling 
   *                  {@link #makeStopSet(String)}.
   * @param maxSlop   Maximum slop to allow in a query, based on the index
   *                  being queried.
   */
  public XtfBigramQueryRewriter(Set stopSet, int maxSlop) {
    super( stopSet, maxSlop );
  } // constructor

  /**
   * Rewrite a query of any supported type. Stop words will either be
   * removed or bi-grammed.
   * 
   * @param q   Query to rewrite
   * @return    A new query, or 'q' unchanged if no change was needed.
   */
  public Query rewriteQuery(Query q) {
    if (q instanceof SpanSectionTypeQuery)
      return rewrite((SpanSectionTypeQuery) q);
    else if( q instanceof SpanExactQuery )
        return rewrite((SpanExactQuery)q);
    else if( q instanceof MoreLikeThisQuery )
        return rewrite((MoreLikeThisQuery)q);
    else if( q instanceof NumericRangeQuery )
      return rewrite((NumericRangeQuery)q);
    return super.rewriteQuery(q);
  }
  
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
        && secTypeQuery == stq.getSectionTypeQuery())
      return stq;

    // Make a new query
    Query newq = new SpanSectionTypeQuery(textQuery, secTypeQuery);
    copyBoost(stq, newq);
    return newq;

  } // rewrite()
  
  /**
   * Rewrite a span EXACT query. Stop words will be bi-grammed into adjacent
   * terms.
   * 
   * @param eq  The query to rewrite
   * @return    Rewritten version, or 'eq' unchanged if no changed needed.
   */
  protected Query rewrite(SpanExactQuery eq) {
    // Rewrite each clause and make a vector of the new ones.
    SpanQuery[] clauses = eq.getClauses();
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
    anyChanges |= bigramQueries(newClauses, 0);

    // If no changes, just return the original query.
    if (!anyChanges)
      return eq;

    // If we end up with no clauses, let the caller know.
    if (newClauses.isEmpty())
      return null;

    // If we end up with a single clause, return just that.
    if (newClauses.size() == 1) {

      // Since we're getting rid of the parent, pass on its boost to the
      // child.
      //
      return combineBoost(eq, (Query) newClauses.elementAt(0));
    }

    // Construct a new 'exact' query joining all the rewritten clauses.
    SpanQuery[] newArray = new SpanQuery[newClauses.size()];
    return copyBoost(eq, 
        new SpanExactQuery((SpanQuery[]) newClauses.toArray(newArray)));
  } // rewrite()

  /** Rewrite a "more like this" query */
  protected Query rewrite(MoreLikeThisQuery mlt) {
    Query rewrittenSub = rewriteQuery(mlt.getSubQuery());
    if (rewrittenSub == mlt.getSubQuery() && !forceRewrite(mlt))
        return mlt;
    return copyBoost(mlt, new MoreLikeThisQuery(rewrittenSub));
  }
  
  /** Rewrite a numeric range query */
  protected Query rewrite(NumericRangeQuery nrq) {
    if (!forceRewrite(nrq))
        return nrq;
    return (NumericRangeQuery) nrq.clone();
  }
  
  /**
   * Basic regression test
   */
  public static final Tester tester = new Tester("XtfBigramStopFilter") {
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
      return new SpanTermQuery(new Term("text", text));
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
      BigramQueryRewriter rewriter = new BigramQueryRewriter(stopSet, 20);
      Query newQ = rewriter.rewriteQuery(query);
      String result = queryToText(newQ);
      Trace.debug(queryToText(query) + " --> " + result);
      assert result.equals(expectedResult);
    } // testQuery()

    private void testUnchanged(Query query) {
      BigramQueryRewriter rewriter = new BigramQueryRewriter(stopSet, 20);
      Query newQ = rewriter.rewriteQuery(query);
      assert query == newQ;
    } // testQuery()

    /**
     * Run the test.
     */
    protected void testImpl() {
      stopSet = BigramQueryRewriter.makeStopSet("a and it is the of");
      
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
          "\"(man^0 OR man~of) (of~war OR war^0)\"~20");

      // Test AND queries with non~term clauses.
      testQuery(
          and(join(term("the"), or(terms("white beige")), term("rabbit"))),
          "\"((the~white OR the~beige) OR (white OR beige)^0) rabbit\"~20");

      // Test boost propagation
      testQuery(boost(2, and(join(term("eat"), boost(5, term("the")),
          term("wave")))), "\"(eat^0 OR eat~the^5) (the~wave^5 OR wave^0)\"~20^2");
      testQuery(boost(5, and(join(boost(2, term("eat")), boost(3,
          or(terms("the a")))))), "eat^10");

      ////////////////////////////////////////////////////////////////////////
      // NEAR QUERIES
      ////////////////////////////////////////////////////////////////////////

      testUnchanged(near(5, terms("three freezy trees")));
      testUnchanged(near(5, join(term("three"), or(terms("freezy breezy")),
          term("trees"))));
      testQuery(near(5, terms("man of war")),
          "\"(man^0 OR man~of) (of~war OR war^0)\"~5");
      testQuery(near(5, terms("when it is a problem")),
          "(\"when~it it~is is~a a~problem\"~5 OR "
              + "\"(when^0 OR when~it) (a~problem OR problem^0)\"~5^0)");
      testQuery(near(5, terms("it is a problem")),
          "(\"it~is is~a a~problem\"~5 OR (a~problem OR problem^0)^0)");
      testQuery(near(5, terms("when it is a")),
          "(\"when~it it~is is~a\"~5 OR (when^0 OR when~it)^0)");

      // Try some near queries with non~term clauses.
      testQuery(near(5, join(or(terms("shake bake")), term("it"))),
          "((shake OR bake)^0 OR (shake~it OR bake~it))");
      testQuery(
          near(5, join(or(terms("shake bake")), term("it"), term("now"))),
          "\"((shake OR bake)^0 OR (shake~it OR bake~it)) "
              + "(it~now OR now^0)\"~5");
      testQuery(near(5, join(term("jeff"), or(terms("shakes bakes")),
          term("it"))),
          "\"jeff ((shakes OR bakes)^0 OR (shakes~it OR bakes~it))\"~5");

      // Test boost propagation
      testQuery(boost(2, near(5, join(boost(3, or(join(boost(4, term("shake")),
          boost(5, term("bake"))))), boost(6, term("it")),
          boost(7, term("now"))))), "\"((shake^4 OR bake^5)^2 OR "
          + "(shake~it^6 OR bake~it^6)^3) " + "(it~now^7 OR now^5)\"~5^2");
      testQuery(boost(7, near(5, join(boost(6, or(join(boost(5, term("shake")),
          boost(4, term("bake"))))), boost(3, term("it")),
          boost(2, term("now"))))), "\"((shake^5 OR bake^4)^4 OR "
          + "(shake~it^5 OR bake~it^4)^6) " + "(it~now^3 OR now^1)\"~5^7");

      ////////////////////////////////////////////////////////////////////////
      // OR QUERIES
      ////////////////////////////////////////////////////////////////////////

      testUnchanged(or(join(term("foo"), and(terms("bar gaz")))));
      testQuery(or(join(term("arf"), and(terms("the dog")), term("said"))),
          "(arf OR (the~dog OR dog^0) OR said)");
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
          "((the~cow OR cow^0) NOT (the~dog OR dog^0))~5");
      testQuery(and(join(term("like"), term("a"), not(5, term("cow"),
          term("dog")))),
          "\"(like^0 OR like~a) ((a~cow NOT dog)~5 OR (cow NOT dog)~5^0)\"~20");

      // A couple tests anticipating future support for case sensitivity and 
      // accent insensitivity.
      //
      testQuery(and(join(term("the"), not(0, term("hat"),
          or(terms("hat~p hat~c"))), term("trick"))),
          "\"((the~hat NOT (hat~p OR hat~c))~0 OR "
              + "(hat NOT (hat~p OR hat~c))~0^0) trick\"~20");
      testQuery(and(join(term("hank"), not(0, term("hat"),
          or(terms("hat~p hat~c"))), term("is"))),
          "\"hank ((hat NOT (hat~p OR hat~c))~0^0 OR "
              + "(hat~is NOT (hat~p OR hat~c))~0)\"~20");

      ////////////////////////////////////////////////////////////////////////
      // BOOLEAN QUERIES
      ////////////////////////////////////////////////////////////////////////

      testUnchanged(bool(regTerm("hello"), true, false, regTerm("kitty"),
          false, true, regTerm("pencil"), true, false));

      testQuery(bool(regTerm("cats"), true, false, regTerm("and"), false, true,
          regTerm("hats"), true, false), "(+cats +hats)");
      testQuery(bool(regTerm("cats"), true, false, regTerm("and"), false,
          false, regTerm("hats"), true, false), "(+cats +hats)");
      testQuery(bool(regTerm("is"), true, false, regTerm("it"), true, false,
          regTerm("fun"), false, false), "(fun)");

      // Test BooleanQuery with non~term clauses
      testQuery(bool(regTerm("whip"), true, false, or(terms("it them")), true,
          false, regTerm("good"), true, false), "(+whip +them +good)");

      // Test boost propagation
      testQuery(boost(2, bool(boost(3, regTerm("it")), false, false, boost(4,
          regTerm("and")), false, false, boost(5, regTerm("harry")), true,
          false)), "harry^10");

    } // testImpl()
  }; // Tester

} // class XtfBigramQueryRewriter
