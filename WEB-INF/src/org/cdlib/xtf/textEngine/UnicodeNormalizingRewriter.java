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
import org.apache.lucene.search.spans.SpanWildcardQuery;
import org.cdlib.xtf.util.FastCache;
import org.cdlib.xtf.util.Normalizer;

/*
 * This file created on Apr 15, 2005 by Martin Haye
 */

/**
 * Rewrites a Lucene query to replace all non-normalized words
 * (i.e. not encoded in Normalized-Form-C) with normalized ones.
 * For instance, many diacritics actually need to be combined with
 * their main letter rather than as separate combining marks.
 *
 * @author Martin Haye
 */
public class UnicodeNormalizingRewriter extends XtfQueryRewriter 
{
  /** How many recent mappings to maintain */
  private static final int CACHE_SIZE = 5000;

  /** Keep a cache of lookups performed to-date */
  private FastCache<String, String> cache = new FastCache(CACHE_SIZE);

  /** Set of fields that are tokenized in the index */
  private Set tokenizedFields;

  /** Construct a new rewriter. Will only operate on tokenized fields. */
  public UnicodeNormalizingRewriter(Set tokFields) {
    this.tokenizedFields = tokFields;
  }

  /**
   * Rewrite a term query. This is only called for artificial queries
   * introduced by XTF system itself, and therefore we don't map here.
   */
  protected Query rewrite(TermQuery q) {
    return q;
  }

  /**
   * Rewrite a span term query. Normalizes Unicode to NFC.
   *
   * @param q  The query to rewrite
   * @return   Rewritten version, or 'q' unchanged if no changed needed.
   */
  protected Query rewrite(SpanTermQuery q) 
  {
    Term t = q.getTerm();
    if (!tokenizedFields.contains(t.field()))
      return q;

    // Only do the (sometimes lengthy) normalization step if we haven't already 
    // looked up this token.
    //
    String text = t.text();
    if (!cache.contains(text)) {
      String normalizedText = Normalizer.normalize(text);
      cache.put(text, normalizedText);
    }
    String newText = cache.get(text);
    if (newText.equals(text))
      return q;
    
    Term newTerm = new Term(t.field(), newText);
    return copyBoost(q, new SpanTermQuery(newTerm, q.getTermLength()));
  }

  /**
   * Rewrite a wildcard term query. Normalizes Unicode encoding to NFC in all words.
   *
   * @param q  The query to rewrite
   * @return   Rewritten version, or 'q' unchanged if no changed needed.
   */
  protected Query rewrite(SpanWildcardQuery q) 
  {
    assert q instanceof XtfSpanWildcardQuery;

    Term t = q.getTerm();
    if (!tokenizedFields.contains(t.field()))
      return q;

    // Only do the (sometimes lengthy) normalization step if we haven't already 
    // looked up this token.
    //
    String text = t.text();
    if (!cache.contains(text)) {
      String normalizedText = Normalizer.normalize(text);
      cache.put(text, normalizedText);
    }
    String newText = cache.get(text);
    if (newText.equals(text))
      return q;
    
    Term newTerm = new Term(t.field(), newText);
    return copyBoost(q, new XtfSpanWildcardQuery(newTerm, q.getTermLimit()));
  }
} // class UnicodeNormalizingRewriter
