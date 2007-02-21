package org.cdlib.xtf.textEngine;


/**
 * Copyright (c) 2004, Regents of the University of California
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
import java.util.List;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.Spans;

/**
 * Just like a SpanNearQuery with slop set to zero, except that it also looks for
 * the special 'start-of-field' and 'end-of-field' tokens inserted by the
 * text indexer. Thus, it will match either the entire field, or none of it.
 *
 * @author Martin Haye
 */
public class SpanExactQuery extends SpanQuery 
{
  // The clauses to match (not including the special start and end tokens)
  private SpanQuery[] clauses;

  /**
   * Construct an exact query on a set of clauses.
   *
   * @param clauses         Clauses to match.
   */
  public SpanExactQuery(SpanQuery[] clauses) 
  {
    // Must have at least one clause to work.
    if (clauses == null || clauses.length == 0)
      throw new RuntimeException("SpanExactQuery requires at least one clause");

    // Record the input parms
    this.clauses = clauses;
  } // constructor

  // inherit javadoc
  public Query rewrite(IndexReader reader)
    throws IOException 
  {
    List newClauses = new ArrayList(clauses.length);
    boolean anyChanged = false;
    for (int i = 0; i < clauses.length; i++) {
      SpanQuery clause = clauses[i];
      SpanQuery rewrittenClause = (SpanQuery)clause.rewrite(reader);
      newClauses.add(rewrittenClause);
      if (clause != rewrittenClause)
        anyChanged = true;
    }

    if (!anyChanged)
      return this;

    SpanExactQuery clone = (SpanExactQuery)this.clone();
    clone.clauses = (SpanQuery[])newClauses.toArray(
      new SpanQuery[newClauses.size()]);
    return clone;
  }

  /** Return the clauses whose spans are matched. */
  public SpanQuery[] getClauses() {
    return clauses;
  }

  /** Return all the sub-queries (clauses in our case) */
  public Query[] getSubQueries() {
    return clauses;
  }

  /**
   * Iterate all the spans from the text query that match the sectionType
   * query also.
   */
  public Spans getSpans(final IndexReader reader, final Searcher searcher)
    throws IOException 
  {
    // Modify the first and last clauses to include the special start-of-field
    // and end-of-field markers.
    //
    ArrayList newClauses = new ArrayList(clauses.length);
    for (int i = 0; i < clauses.length; i++) 
    {
      if (!(clauses[i] instanceof SpanTermQuery))
        throw new RuntimeException("Exact queries only support plain terms");

      // We only fool with the first and last clauses
      boolean isFirst = (i == 0);
      boolean isLast = (i == clauses.length - 1);
      if (!isFirst && !isLast) {
        newClauses.add(clauses[i]);
        continue;
      }

      // Get the term out.
      SpanTermQuery oldClause = (SpanTermQuery)clauses[i];
      String oldTerm = oldClause.getTerm().text();
      String field = oldClause.getTerm().field();
      int length = oldClause.getTermLength();

      // Handy things we'll need later
      SpanQuery detachedStartQuery = new SpanTermQuery(new Term(
                                                                field,
                                                                "" +
                                                                  Constants.FIELD_START_MARKER));
      SpanQuery detachedEndQuery = new SpanTermQuery(new Term(
                                                              field,
                                                              "" +
                                                                Constants.FIELD_END_MARKER));

      // We may need to OR up to four clauses together. Why? Consider
      // this data:
      //
      //   <subject>(foo)</subject>
      //
      // This will be indexed with start/end markers like this:
      //
      //   ?(foo)?      (where ? represents the markers)
      //
      // Normally our exact query would just look for ?foo?, but that
      // won't match the above data because the parentheses force the
      // markers to be indexed as individual terms rather than as part
      // of the word "foo". But we really want to be able to get an 
      // exact match on data that has punctuation (and on data that 
      // doesn't.) So we form a query that looks like this:
      //
      //   OR("?foo?", "? foo?", "?foo ?", "? foo ?")
      //
      // Note the spacing. This basically queries for all combinations
      // of attached or detached markers. In the case of a one-term
      // exact query, you end up with four clauses in the OR as above. 
      // But in the case of multiple terms, like a query for "cat bar flu" 
      // you would get:
      //
      //   PHRASE(OR("?cat", "? cat"), "bar", OR("flu?", "flu ?"))
      //
      // So following is logic that accomplishes all this magic...
      //
      ArrayList orClauses = new ArrayList();

      // We may need the start marker: (0) absent; (1) detached; or
      // (2) attached.
      //
      for (int startAtt = 0; startAtt < 3; ++startAtt) 
      {
        // It can only be absent if this isn't the first token.
        // Likewise, it can only be present if this is the first.
        //
        if ((startAtt == 0 && isFirst) || (startAtt != 0 && !isFirst))
          continue;

        // Okay, we have the same three choices for the end marker.
        for (int endAtt = 0; endAtt < 3; ++endAtt) 
        {
          // It can only be absent if this isn't the last token.
          // Likewise, it can only be present if this is the last.
          //
          if ((endAtt == 0 && isLast) || (endAtt != 0 && !isLast))
            continue;

          // We'll form a phrase of up to three terms.
          ArrayList phraseClauses = new ArrayList();

          // First, a detached start marker.
          if (startAtt == 1)
            phraseClauses.add(detachedStartQuery);

          // Next, the term itself with or without attached markers.
          String newTerm = oldTerm;
          if (startAtt == 2)
            newTerm = Constants.FIELD_START_MARKER + newTerm;
          if (endAtt == 2)
            newTerm = newTerm + Constants.FIELD_END_MARKER;

          SpanQuery newClause = new SpanTermQuery(new Term(field, newTerm),
                                                  length);
          newClause.setBoost(oldClause.getBoost());
          phraseClauses.add(newClause);

          // Finally, a detached end marker.
          if (endAtt == 1)
            phraseClauses.add(detachedEndQuery);

          // If only one term, skip the phrase query.
          int nTerms = phraseClauses.size();
          if (nTerms == 1)
            orClauses.add(phraseClauses.get(0));
          else {
            orClauses.add(
              new SpanNearQuery(
                (SpanQuery[])phraseClauses.toArray(new SpanQuery[nTerms]),
                0,
                true));
          }
        } // for endAtt
      } // for startAtt

      // If only one clause, skip the OR
      if (orClauses.size() == 1) {
        newClauses.add(orClauses.get(0));
        continue;
      }

      // Make an OR to stick them together
      SpanOrQuery orQuery = new SpanOrQuery(
        (SpanQuery[])orClauses.toArray(new SpanQuery[orClauses.size()]));
      newClauses.add(orQuery);
    } // for i

    // And make a near query out of the whole thing.
    SpanQuery q = new SpanNearQuery(
      (SpanQuery[])newClauses.toArray(new SpanQuery[newClauses.size()]),
      0,
      true);
    q.setSpanRecording(getSpanRecording());

    // Return the spans from the rewritten query.
    return q.getSpans(reader, searcher);
  }

  public String getField() {
    return clauses[0].getField();
  }

  public Collection getTerms() 
  {
    Collection terms = new ArrayList();
    for (int i = 0; i < clauses.length; i++) {
      SpanQuery clause = clauses[i];
      terms.addAll(clause.getTerms());
    }
    return terms;
  }

  public String toString(String field) 
  {
    StringBuffer buffer = new StringBuffer();
    buffer.append("spanExact(");
    for (int i = 0; i < clauses.length; i++) {
      SpanQuery clause = clauses[i];
      buffer.append(clause.toString(field));
      if (i < clauses.length - 1)
        buffer.append(", ");
    }
    buffer.append(")");
    return buffer.toString();
  }
} // class SpanExactQuery
