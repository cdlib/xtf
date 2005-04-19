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
import java.util.ArrayList;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermPositions;
import org.apache.lucene.ngram.NgramQueryRewriter;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;

/** Matches spans containing a term. */
public class SpanTermQuery extends SpanQuery {
  private Term term;
  protected Set stopSet;

  /** Construct a SpanTermQuery matching the named term's spans. */
  public SpanTermQuery(Term term) { 
      this.term = term;
  }

  /** Construct a SpanTermQuery matching the named term's spans, using
   *  the specified stop-word set. */
  public SpanTermQuery(Term term, Set stopSet) { 
      this.term = term;
      this.stopSet = stopSet;
  }

  /** Return the term whose spans are matched. */
  public Term getTerm() { return term; }
  
  public boolean hasStopWord()
  {
      if( stopSet != null )
          return NgramQueryRewriter.isNgram(stopSet, term.text());
      else
          return false;
  }
  
  public void setStopWords(Set set) {
      this.stopSet = set;
  }
  
  public Set getStopWords() {
      return stopSet;
  }

  public String getField() { return term.field(); }

  public Collection getTerms() {
    Collection terms = new ArrayList();
    terms.add(term);
    return terms;
  }

  public String toString(String field) {
    if (term.field().equals(field))
      return term.text();
    else
      return term.toString();
  }

  public Spans getSpans(final IndexReader reader, final Searcher searcher) 
      throws IOException 
  {

    // Calculate a score value for this term, including the boost.
    float idf = getSimilarity(searcher).idf(term, searcher); // compute idf
    final float value = idf * getBoost();             // compute query weight

    final byte[] docNorms = reader.norms(term.field());

    return new Spans() {
        private TermPositions positions = reader.termPositions(term);

        private int doc = -1;
        private int freq;
        private int count;
        private int position;

        public boolean next() throws IOException {
          if (count == freq) {
            if (!positions.next()) {
              doc = Integer.MAX_VALUE;
              return false;
            }
            doc = positions.doc();
            freq = positions.freq();
            count = 0;
          }
          position = positions.nextPosition();
          count++;
          return true;
        }

        public boolean skipTo(int target) throws IOException {
          if (!positions.skipTo(target)) {
            doc = Integer.MAX_VALUE;
            return false;
          }

          doc = positions.doc();
          freq = positions.freq();
          count = 0;

          position = positions.nextPosition();
          count++;

          return true;
        }

        public int doc() { return doc; } 
        public int start() { return position; }
        public int end() { return position + (hasStopWord() ? 2 : 1); }
        public float score() { return value * 
                               Similarity.decodeNorm(docNorms[doc]); }
        public void collectTerms(Set terms) { terms.add(term.text()); }

        public String toString() {
          return "spans(" + SpanTermQuery.this.toString() + ")@"+
            (doc==-1?"START":(doc==Integer.MAX_VALUE)?"END":doc+"-"+position);
        }

      };
  }

}
