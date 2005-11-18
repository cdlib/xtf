package org.apache.lucene.search;

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
import java.util.BitSet;
import java.util.Vector;

import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.spans.FieldSpans;
import org.apache.lucene.search.spans.SpanRecordingScorer;
import org.apache.lucene.index.IndexReader;

/** Implements search over a single IndexReader.
 *
 * <p>Applications usually need only call the inherited {@link #search(Query)}
 * or {@link #search(Query,Filter)} methods.
 */
public class RecordingSearcher extends IndexSearcher {
  private IndexReader reader;
  private Vector registered;

  /** Creates a searcher searching the provided index. */
  public RecordingSearcher(IndexReader r) {
    super(r);
    reader = r;
  }

  /** Lower-level search API which supports span collection.
   *
   * <p>{@link SpanHitCollector#collect(int,float,FieldSpans)} is called for 
   * every non-zero scoring document.
   */
  public void search(Query query, SpanHitCollector results) throws IOException {
    search(query, null, results);
  }
  
  /** Lower-level search API which supports span collection.
   *
   * <p>{@link SpanHitCollector#collect(int,float,FieldSpans)} is called for 
   * every non-zero scoring document which matches the filter.
   */
  public void search(Query query, Filter filter,
                     final SpanHitCollector results) throws IOException {
    SpanHitCollector collector = results;
    if (filter != null) {
      final BitSet bits = filter.bits(reader);
      collector = new SpanHitCollector() {
        private final SpanHitCollector mresults = (SpanHitCollector)results;
        public final void collect(int doc, float score, FieldSpanSource src) {
          if (bits.get(doc)) {		  // skip docs not in bits
            mresults.collect(doc, score, src);
          }
        }
      };
    }

    // While forming the weight, any SpanRecordingScorers will register
    // themselves with this Searcher. Keep track of them so we can access 
    // spans during the query.
    //
    Scorer scorer;
    SpanRecordingScorer[] recordingScorers;
    synchronized (this) { // prevent other threads from registering scorers
      registered = new Vector();
      scorer = query.weight(this).scorer(reader);
      recordingScorers = (SpanRecordingScorer[])
        registered.toArray(new SpanRecordingScorer[registered.size()]);
      registered = null;
    }
    if (scorer == null)
      return;
    FieldSpanSource spanSource = new FieldSpanSource(recordingScorers);
    
    // Now process all the documents and collect them and their spans.
    while (scorer.next()) {
      FieldSpans fieldSpans = new FieldSpans();
      int doc = scorer.doc();
      spanSource.curDoc = doc;
      float score = scorer.score(); // must call before recordSpans()
      collector.collect(doc, score, spanSource);
    }
  }

  // Called while building the scorers for a query. All SpanRecordingScorers
  // will register themselves.
  //
  public void registerRecordingScorer(SpanRecordingScorer scorer) {
    if( registered != null )
        registered.add(scorer);
  }
}
