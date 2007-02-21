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
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermPositions;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;

/** Matches spans containing a term. */
public class SpanTermQuery extends SpanQuery 
{
  private Term term;
  private int termLength;

  /** Construct a SpanTermQuery matching the named term's spans. */
  public SpanTermQuery(Term term) {
    this.term = term;
    this.termLength = 1;
  }

  /** Construct a SpanTermQuery matching the named term's spans, using
   *  the specified stop-word set. */
  public SpanTermQuery(Term term, int termLength) {
    this.term = term;
    this.termLength = termLength;
  }

  /** Return the term whose spans are matched. */
  public Term getTerm() {
    return term;
  }

  /** Return the length of the term in positions (typically 1) */
  public int getTermLength() {
    return termLength;
  }

  public String getField() {
    return term.field();
  }

  public Collection getTerms() {
    Collection terms = new ArrayList();
    terms.add(term);
    return terms;
  }

  public String toString(String field) 
  {
    StringBuffer buffer = new StringBuffer();
    if (!term.field().equals(field)) {
      buffer.append(term.field());
      buffer.append(":");
    }
    buffer.append(term.text());
    if (getBoost() != 1.0f) {
      buffer.append("^");
      buffer.append(Float.toString(getBoost()));
    }
    return buffer.toString();
  }

  public Spans getSpans(final IndexReader reader, final Searcher searcher)
    throws IOException 
  {
    // Calculate a score value for this term, including the boost.
    final float idf = getSimilarity(searcher).idf(term, searcher); // compute idf
    final float value = idf * getBoost(); // compute query weight

    final byte[] fieldNorms = reader.norms(term.field());

    return new Spans() 
    {
      private TermPositions positions = reader.termPositions(term);
      private int doc = -1;
      private int freq;
      private int count;
      private int position;

      public boolean next()
        throws IOException 
      {
        if (count == freq) 
        {
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

      public boolean skipTo(int target)
        throws IOException 
      {
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

      public int doc() {
        return doc;
      }

      public int start() {
        return position;
      }

      public int end() {
        return position + termLength;
      }

      public float score() {
        return value * Similarity.decodeNorm(fieldNorms[doc]);
      }

      public String toString() {
        return "spans(" + SpanTermQuery.this.toString() + ")@" +
               (doc == -1 ? "START"
                : (doc == Integer.MAX_VALUE) ? "END" : doc + ":" + position);
      }

      public Explanation explain()
        throws IOException 
      {
        Explanation result = new Explanation();
        result.setDescription("weight(" + toString() + "), product of:");

        // Explain idf
        Explanation idfExpl = new Explanation(idf,
                                              "idf(docFreq=" +
                                              searcher.docFreq(term) + ")");
        result.addDetail(idfExpl);

        // Explain boost
        Explanation boostExpl = new Explanation(getBoost(), "boost");
        if (getBoost() != 1.0f)
          result.addDetail(boostExpl);

        // Explain norm 
        Explanation fieldNormExpl = new Explanation();
        float fieldNorm = fieldNorms != null
                          ? Similarity.decodeNorm(fieldNorms[doc]) : 0.0f;
        fieldNormExpl.setValue(fieldNorm);
        fieldNormExpl.setDescription(
          "fieldNorm(field=" + getField() + ", doc=" + doc + ")");
        result.addDetail(fieldNormExpl);

        result.setValue(
          boostExpl.getValue() * idfExpl.getValue() * fieldNormExpl.getValue());
        return result;
      }
    };
  }
}
