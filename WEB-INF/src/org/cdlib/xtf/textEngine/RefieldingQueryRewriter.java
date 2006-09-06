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
 */


import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.spans.SpanRangeQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.SpanWildcardQuery;

/**
 * This class swaps the current field of every sub-query to the specified
 * field.
 * 
 * @author Martin Haye
 */
public class RefieldingQueryRewriter extends XtfQueryRewriter
{
  private String newField;

  /** Change the field name of the given query */
  public synchronized Query refield(Query q, String field)
  {
    newField = field;
    return rewriteQuery(q);
  }
  
  /** Switch the field of the given term */
  private Term rewriteTerm(Term t)
  {
    if (t == null)
      return t;
    return new Term(newField, t.text());
  }

  // inherit JavaDoc
  protected Query rewrite(TermQuery q) {
    return new TermQuery(rewriteTerm(q.getTerm()));
  }

  // inherit JavaDoc
  protected Query rewrite(SpanTermQuery q) {
    return new SpanTermQuery(rewriteTerm(q.getTerm()));
  }

  // inherit JavaDoc
  protected Query rewrite(SpanWildcardQuery q) {
    assert q instanceof XtfSpanWildcardQuery;
    return new XtfSpanWildcardQuery(rewriteTerm(q.getTerm()), q.getTermLimit());
  }

  // inherit JavaDoc
  protected Query rewrite(SpanRangeQuery q) {
    assert q instanceof XtfSpanRangeQuery;
    return new XtfSpanRangeQuery(rewriteTerm(q.getLowerTerm()),
                                 rewriteTerm(q.getUpperTerm()),
                                 q.isInclusive(),
                                 q.getTermLimit());
  }

  // inherit JavaDoc
  protected Query rewrite(NumericRangeQuery nrq) {
    return new NumericRangeQuery(newField,
                                 nrq.getLowerVal(),
                                 nrq.getUpperVal(),
                                 nrq.includesLower(),
                                 nrq.includesUpper());
  }

} // class FieldSwappingQueryRewriter
