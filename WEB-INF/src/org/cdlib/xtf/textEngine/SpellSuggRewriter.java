package org.cdlib.xtf.textEngine;


/*
 * Copyright (c) 2007, Regents of the University of California
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
import java.util.Map;
import java.util.Set;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.SpanWildcardQuery;
import org.apache.lucene.util.StringUtil;

/*
 * This file created on Feb 13, 2007 by Martin Haye
 */

/**
 * Rewrites a Lucene query to replace all misspelled words with their
 * suggested replacements. We limit the replacements to the specified
 * set of fields.
 *
 * @author Martin Haye
 */
public class SpellSuggRewriter extends XtfQueryRewriter 
{
  private Map suggs;
  private Set fields;

  /** Construct a new rewriter. */
  public SpellSuggRewriter(Map suggs, Set fields) {
    this.suggs = suggs;
    this.fields = fields;
  }

  /**
   * Rewrite a term query. This is only called for artificial queries
   * introduced by XTF system itself, and therefore we don't map here.
   */
  protected Query rewrite(TermQuery q) {
    return q;
  }

  /**
   * Rewrite a span term query. Replaces mispelled words
   *
   * @param q  The query to rewrite
   * @return   Rewritten version, or 'q' unchanged if no changed needed.
   */
  protected Query rewrite(SpanTermQuery q) 
  {
    // If this term isn't for a field we're interested in, skip it.
    Term t = q.getTerm();
    if (!fields.contains(t.field()))
      return q;

    // See if we have a suggestion for this term. If not, we're done.
    SpellingSuggestion sugg = (SpellingSuggestion)suggs.get(t.text());
    if (sugg == null)
      return q;

    // If the suggestion is to remove the term, inform the caller.
    if (sugg.suggestedTerm == null)
      return null;

    // If the suggestion is for a single word, our work is easy.
    if (!sugg.suggestedTerm.contains(" ")) {
      Term newTerm = new Term(t.field(), sugg.suggestedTerm);
      return copyBoost(q, new SpanTermQuery(newTerm, q.getTermLength()));
    }

    // For multiple words, form a NEAR query joining them.
    String[] words = StringUtil.splitWords(sugg.suggestedTerm);
    SpanTermQuery[] termQueries = new SpanTermQuery[words.length];
    for (int i = 0; i < words.length; i++)
      termQueries[i] = new SpanTermQuery(new Term(t.field(), words[i]), 1);
    return copyBoost(q, new SpanNearQuery(termQueries, 10, false));
  }

  /**
   * Rewrite a wildcard term query.
   */
  protected Query rewrite(SpanWildcardQuery q) 
  {
    // Do not apply spelling correction to wildcards.
    return q;
  }
} // class AccentFoldingRewriter
