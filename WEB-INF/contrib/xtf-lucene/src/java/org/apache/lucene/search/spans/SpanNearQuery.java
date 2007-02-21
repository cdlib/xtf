package org.apache.lucene.search.spans;


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
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;

/** Matches spans which are near one another.  One can specify <i>slop</i>, the
 * maximum edit distance, as well as whether matches are required to be in-order. In-order
 * matches are scored higher than out-of-order.
 */
public class SpanNearQuery extends SpanQuery 
{
  private List clauses;
  private int slop;
  private boolean inOrder;
  private String field;

  /** Construct a SpanNearQuery.  Matches spans matching a span from each
   * clause, with up to <code>slop</code> total edit distance between them.
   * When <code>inOrder</code> is true, the spans from each clause
   * must be ordered as in <code>clauses</code>.
   */
  public SpanNearQuery(SpanQuery[] clauses, int slop, boolean inOrder) 
  {
    // copy clauses array into an ArrayList
    this.clauses = new ArrayList(clauses.length);
    for (int i = 0; i < clauses.length; i++) 
    {
      SpanQuery clause = clauses[i];
      if (i == 0) { // check field
        field = clause.getField();
      }
      else if (!clause.getField().equals(field)) {
        throw new IllegalArgumentException("Clauses must have same field.");
      }
      this.clauses.add(clause);
    }

    this.slop = slop;
    this.inOrder = inOrder;
  }

  /** Return the clauses whose spans are matched. */
  public SpanQuery[] getClauses() {
    return (SpanQuery[])clauses.toArray(new SpanQuery[clauses.size()]);
  }

  /** Return the maximum number of intervening unmatched positions permitted.*/
  public int getSlop() {
    return slop;
  }

  /** Return true if matches are required to be in-order.*/
  public boolean isInOrder() {
    return inOrder;
  }

  /** Set the maximum edit distance permitted.*/
  public void setSlop(int slop) {
    this.slop = slop;
  }

  public String getField() {
    return field;
  }

  public Collection getTerms() 
  {
    Collection terms = new ArrayList();
    Iterator i = clauses.iterator();
    while (i.hasNext()) {
      SpanQuery clause = (SpanQuery)i.next();
      terms.addAll(clause.getTerms());
    }
    return terms;
  }

  public Query[] getSubQueries() {
    return (Query[])clauses.toArray(new Query[clauses.size()]);
  }

  public Query rewrite(IndexReader reader)
    throws IOException 
  {
    List newClauses = new ArrayList(clauses.size());
    boolean anyChanged = false;
    for (Iterator i = clauses.iterator(); i.hasNext();) {
      SpanQuery clause = (SpanQuery)i.next();
      SpanQuery rewrittenClause = (SpanQuery)clause.rewrite(reader);
      newClauses.add(rewrittenClause);
      if (clause != rewrittenClause)
        anyChanged = true;
    }

    if (!anyChanged)
      return this;

    SpanNearQuery clone = (SpanNearQuery)this.clone();
    clone.clauses = newClauses;
    return clone;
  }

  public String toString(String field) 
  {
    StringBuffer buffer = new StringBuffer();
    buffer.append("spanNear([");
    Iterator i = clauses.iterator();
    while (i.hasNext()) 
    {
      SpanQuery clause = (SpanQuery)i.next();
      buffer.append(clause.toString(field));
      if (i.hasNext()) {
        buffer.append(", ");
      }
    }
    buffer.append("], ");
    buffer.append(slop);
    buffer.append(", ");
    buffer.append(inOrder);
    buffer.append(")");
    return buffer.toString();
  }

  public Spans getSpans(final IndexReader reader, final Searcher searcher)
    throws IOException 
  {
    if (clauses.size() == 0) // optimize 0-clause case
      return new SpanOrQuery(getClauses()).getSpans(reader, searcher);

    if (clauses.size() == 1) // optimize 1-clause case
      return ((SpanQuery)clauses.get(0)).getSpans(reader, searcher);

    return new NearSpans(this, reader, searcher);
  }
}
