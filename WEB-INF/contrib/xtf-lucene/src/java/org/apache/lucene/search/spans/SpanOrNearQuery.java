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
 *
 * Acknowledgements:
 *
 * A significant amount of new and/or modified code in this module
 * was made possible by a grant from the Andrew W. Mellon Foundation,
 * as part of the Melvyl Recommender Project.
 */
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;

/** Matches spans which are near one another. Unlike a standard 'near' query,
 * all of the sub-queries need not be present. One can specify <i>slop</i>, the
 * maximum edit distance for those that are present in a given document.
 * If enabled, in-order matches are scored higher than out-of-order.
 */
public class SpanOrNearQuery extends SpanQuery 
{
  private SpanQuery[] clauses;
  private int slop;
  private boolean penalizeOutOfOrder;
  private String field;

  /** Construct a SpanOrNearQuery.  Matches spans matching a span from each
   * clause if present, with up to <code>slop</code> total edit distance
   * between them.
   *
   * When <code>penalizeOutOfOrder</code> is true, out of order clauses are
   * scored lower than in-order clauses.
   */
  public SpanOrNearQuery(SpanQuery[] clauses, int slop,
                         boolean penalizeOutOfOrder) 
  {
    // Verify that all clauses are for the same field.
    this.clauses = clauses;
    for (int i = 0; i < clauses.length; i++) {
      SpanQuery clause = clauses[i];
      if (i == 0)
        field = clause.getField();
      else if (!clause.getField().equals(field))
        throw new IllegalArgumentException("Clauses must have same field.");
    }

    this.slop = slop;
    this.penalizeOutOfOrder = penalizeOutOfOrder;
  }

  /** Return the clauses whose spans are matched. */
  public SpanQuery[] getClauses() {
    return clauses;
  }

  /** Return the maximum number of intervening unmatched positions permitted.*/
  public int getSlop() {
    return slop;
  }

  /** Return true if matches are penalized for being out of order. */
  public boolean penalizeOutOfOrder() {
    return penalizeOutOfOrder;
  }

  /** Set the maximum edit distance permitted.*/
  public void setSlop(int slop) {
    this.slop = slop;
  }

  public String getField() {
    return field;
  }

  public Collection getTerms() {
    Collection terms = new ArrayList();
    for (int i = 0; i < clauses.length; i++)
      terms.addAll(clauses[i].getTerms());
    return terms;
  }

  public Query[] getSubQueries() {
    return clauses;
  }

  public Query rewrite(IndexReader reader)
    throws IOException 
  {
    List newClauses = new ArrayList(clauses.length);
    boolean anyChanged = false;
    for (int i = 0; i < clauses.length; i++) {
      SpanQuery rewrittenClause = (SpanQuery)clauses[i].rewrite(reader);
      newClauses.add(rewrittenClause);
      if (clauses[i] != rewrittenClause)
        anyChanged = true;
    }

    if (!anyChanged)
      return this;

    SpanOrNearQuery clone = (SpanOrNearQuery)this.clone();
    clone.clauses = (SpanQuery[])newClauses.toArray(
      new SpanQuery[newClauses.size()]);
    return clone;
  }

  public String toString(String field) 
  {
    StringBuffer buffer = new StringBuffer();
    buffer.append("spanOrNear([");
    for (int i = 0; i < clauses.length; i++) {
      buffer.append(clauses[i].toString(field));
      if (i < clauses.length - 1)
        buffer.append(", ");
    }
    buffer.append("], ");
    buffer.append(slop);
    buffer.append(", ");
    buffer.append(penalizeOutOfOrder);
    buffer.append(")");
    if (getBoost() != 1.0f)
      buffer.append("^" + getBoost());
    return buffer.toString();
  }

  public Spans getSpans(final IndexReader reader, final Searcher searcher)
    throws IOException 
  {
    if (clauses.length == 1) // optimize 1-clause case
      return clauses[0].getSpans(reader, searcher);

    return new OrNearSpans(this, reader, searcher);
  }
}
