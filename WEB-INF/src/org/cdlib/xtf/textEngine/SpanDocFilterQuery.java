package org.cdlib.xtf.textEngine;

/**
 * Copyright (c) 2004, Regents of the University of California
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Searcher;
import org.cdlib.xtf.textEngine.dedupeSpans.SpanQuery;
import org.cdlib.xtf.textEngine.dedupeSpans.Spans;
import org.cdlib.xtf.textEngine.dedupeSpans.TermMaskSet;

/**
 * Speeds combined meta and text queries by skipping all text chunks that don't
 * match one of the meta-documents.
 * 
 * @author Martin Haye
 */
public class SpanDocFilterQuery extends SpanQuery
{
    /** Array of documents we're interested in */
    private Integer[] docs;
    
    /** Keeps track of which chunks belong to which documents */
    private DocNumMap docNumMap;
    
    /** Text query to filter */
    private SpanQuery textQuery;
    
    /**
     * Construct a filtered query.
     * 
     * @param docHits       Map of all the documents hit by a meta-query.
     * @param docNumMap     Keeps track of which chunks belong to which docs.
     * @param textQuery     Text query to filter.
     */
    public SpanDocFilterQuery( HashMap docHits, DocNumMap docNumMap,
                               SpanQuery textQuery )
    {
        // Record the input parms
        this.docNumMap = docNumMap;
        this.textQuery = textQuery;
        
        // Make a sorted list of all the documents that were hit.
        ArrayList l = new ArrayList( docHits.keySet() );
        Collections.sort( l );
        docs = (Integer[]) l.toArray( new Integer[l.size()] );
    } // constructor
    
    /** Gets an iterator over all the spans */
    public Spans getSpans( final IndexReader reader, final Searcher searcher )
        throws IOException
    {
      return new Spans() {
        private Spans   textSpans = textQuery.getSpans(reader, searcher);
        private boolean moreText = true;
        
        private int     docIndex = -1;
        private boolean moreDocs = true;
        
        private int     curDoc;
        private int     firstChunk;
        private int     lastChunk;
        
        private boolean firstTime = true;

        public boolean next() throws IOException {
          if (moreText)                        // move to next text
            moreText = textSpans.next();
          if( firstTime ) {
              moreDocs = nextDoc();
              firstTime = false;
          }

          while( moreText && moreDocs ) {
              // Advance text to doc, or doc to text
              final int chunk = textSpans.doc();
              if( chunk < firstChunk )
                  moreText = textSpans.skipTo( firstChunk );
              else if( chunk > lastChunk )
                  moreDocs = nextDoc();
              else
                  break;
          }

          return moreText && moreDocs;
        }
        
        public boolean skipTo(int target) throws IOException {
          throw new RuntimeException( "SpanDocFilterQuery.skipTo: not impl" );
        }

        private boolean nextDoc()
        {
          docIndex++;
          if( docIndex >= docs.length )
              return false;
          
          curDoc = docs[docIndex].intValue();
          firstChunk = docNumMap.getFirstChunk( curDoc );
          lastChunk  = docNumMap.getLastChunk ( curDoc );
          
          return true;
        } // nextDoc()
        
        public int doc() { return textSpans.doc(); }
        public int start() { return textSpans.start(); }
        public int end() { return textSpans.end(); }
        public float score() { return textSpans.score(); }
        public int coordBits() { return textSpans.coordBits(); }
        public Collection allTerms() { return textSpans.allTerms(); }
        public String toString() { return textSpans.toString(); }

      };
    }
    
    public void calcCoordBits( TermMaskSet maskSet ) {
        textQuery.calcCoordBits( maskSet );
    }

    public String getField() {
        return textQuery.getField();
    }

    public Collection getTerms() {
        return textQuery.getTerms();
    }

    public String toString( String field ) {
        return textQuery.toString( field );
    }

} // class SpanDocFilterQuery
