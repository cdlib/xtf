package org.cdlib.xtf.textEngine;


/**
 * Copyright (c) 2006, Regents of the University of California
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
 *
 * Acknowledgements:
 *
 * A significant amount of new and/or modified code in this module
 * was made possible by a grant from the Andrew W. Mellon Foundation,
 * as part of the Melvyl Recommender Project.
 */
import java.util.Stack;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;

/**
 * This class converts some common span queries to their faster,
 * non-span equivalents.
 *
 * @author Martin Haye
 */
public class UnspanningQueryRewriter extends XtfQueryRewriter 
{
  private Stack parentStack = new Stack();

  public Query rewriteQuery(Query q) 
  {
    try {
      parentStack.push(q);
      return super.rewriteQuery(q);
    }
    finally {
      parentStack.pop();
    }
  }

  /**
   * For span queries with children, we don't want to un-span those children
   * because the span queries would then break.
   */
  private boolean suppressRewrite() 
  {
    for (int i = 0; i < parentStack.size() - 1; i++) {
      if (parentStack.get(i) instanceof SpanQuery)
        return true;
    }
    return false;
  }

  /**
   * Replace span term queries, if they're not children of another span
   * query, with normal term queries.
   */
  protected Query rewrite(SpanTermQuery q) {
    if (suppressRewrite())
      return q;
    return new TermQuery(q.getTerm());
  }

  /**
   * Replace span OR queries with more efficient plain OR, unless the parent
   * query is another span query.
   */
  protected Query rewrite(SpanOrQuery oq) 
  {
    if (suppressRewrite())
      return oq;

    // Rewrite each term, and add to a plain boolean query.
    BooleanQuery newQuery = new BooleanQuery();

    SpanQuery[] clauses = oq.getClauses();
    for (int i = 0; i < clauses.length; i++)
      newQuery.add(rewriteQuery(clauses[i]), false, false);

    // Retain the original boost, if any.
    return copyBoost(oq, newQuery);
  }
} // class FieldSwappingQueryRewriter
