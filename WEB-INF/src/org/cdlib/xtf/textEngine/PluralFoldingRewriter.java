package org.cdlib.xtf.textEngine;

/*
 * Copyright (c) 2005, Regents of the University of California
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

import java.util.Set;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.cdlib.xtf.util.WordMap;

/*
 * This file created on Apr 15, 2005 by Martin Haye
 */

/**
 * Rewrites a Lucene query to replace all plural words with their singular 
 * equivalents.
 * 
 * @author Martin Haye
 */
public class PluralFoldingRewriter extends XtfQueryRewriter 
{
  
  private WordMap pluralMap;
  private Set tokenizedFields;

  /** Construct a new rewriter to use the given map  */
  public PluralFoldingRewriter( WordMap pluralMap, Set tokFields ) {
    this.pluralMap = pluralMap;
    this.tokenizedFields = tokFields;
  }
  
  /** 
   * Rewrite a term query. This is only called for artificial queries
   * introduced by XTF system itself, and therefore we don't map here.
   */
  protected Query rewrite( TermQuery q ) {
    return q;
  }
  
  /** 
   * Rewrite a span term query. Maps plural words to singular, but only
   * for tokenized fields.
   * 
   * @param q  The query to rewrite
   * @return   Rewritten version, or 'q' unchanged if no changed needed.
   */
  protected Query rewrite( SpanTermQuery q ) {
    Term t = q.getTerm();
    if( !tokenizedFields.contains(t.field()) )
      return q;
    
    String mapped = pluralMap.lookup( t.text() );
    if( mapped == null )
        return q;
    
    Term newTerm = new Term( t.field(), mapped );
    return copyBoost( q, new SpanTermQuery(newTerm, q.getTermLength()) ); 
  }
  
} // class PluralFoldingRewriter
