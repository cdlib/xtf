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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.HitCollector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.PriorityQueue;

import org.cdlib.xtf.textEngine.dedupeSpans.SpanHit;
import org.cdlib.xtf.textEngine.dedupeSpans.SpanHitCollector;
import org.cdlib.xtf.textEngine.dedupeSpans.SpanQuery;
import org.cdlib.xtf.textEngine.sort.FieldSortedHitQueue;
import org.cdlib.xtf.textEngine.sort.SortField;
import org.cdlib.xtf.textEngine.sort.SparseStringComparator;
import org.cdlib.xtf.textEngine.workLimiter.LimIndexReader;
import org.cdlib.xtf.util.Trace;

/**
 * Takes a QueryRequest, rewrites the queries if necessary to remove stop-
 * words and form n-grams, then consults the index(es), and produces a 
 * QueryResult.
 * 
 * @author Martin Haye
 */
public class QueryProcessor
{
    /** Map of all XtfSearchers, so we can re-use them */
    private static HashMap searchers = new HashMap();
    
    /** Lucene searcher to perform the base query */
    private IndexSearcher  searcher;
    
    /** Lucene reader from which to read index data */
    private IndexReader    reader;

    /** Keeps track of which chunks belong to which documents */
    private DocNumMap      docNumMap;
    
    /** Max size of a chunk (in words) */
    private int            chunkSize;
    
    /** Number of words a chunk shares with its successor */
    private int            chunkOverlap;
    
    /** Stop-words to remove (e.g. "the", "a", "and", etc.) */
    private Set            stopSet;
    
    /** 
     * Keeps track of the maximum score for any span, used to normalize 
     * them at the end. 
     */
    private float          maxSpanScore = 0.0f;
    
    /** Total number of documents hit (not just those that scored high) */
    private int            nDocsHit = 0;
    
    private static final SparseStringComparator sparseStringComparator = 
        new SparseStringComparator(); 
    
    /**
     * This is the main entry point. Takes a query request and handles
     * searching the index and forming the results.
     * 
     * @param req   The request to process
     * @return      Zero or more document hits
     */
    public QueryResult processReq( QueryRequest req )
        throws IOException
    {
        // Make an vector to store the hits (we'll make it into an array
        // later, when we know how many we have.)
        //
        Vector hitVec = new Vector( 10 );
        
        // Run each combo query in turn, until either we've gathered the 
        // requested number of hits or we've run out of queries.
        //
        int skip = req.startDoc;
        for( int i = 0; i < req.comboQueries.length; i++ ) 
        {
            String indexPath = req.comboQueries[i].indexPath;
            
            XtfSearcher xtfSearcher = null;
            synchronized( searchers ) {
                xtfSearcher = (XtfSearcher) searchers.get( indexPath );
                if( xtfSearcher == null ) {
                    Directory dir = FSDirectory.getDirectory( indexPath, false );
                    xtfSearcher = new XtfSearcher( dir, 30 ); // 30-sec update chk
                    searchers.put( indexPath, xtfSearcher );
                }
            }
            
            // Get a reader, searcher, and document number map that will all be
            // consistent with each other and up-to-date.
            //
            synchronized( xtfSearcher ) {
                xtfSearcher.update();
                reader       = xtfSearcher.reader();
                docNumMap    = xtfSearcher.docNumMap();
                chunkSize    = xtfSearcher.chunkSize();
                chunkOverlap = xtfSearcher.chunkOverlap();
                stopSet      = xtfSearcher.stopSet();
            }
            
            // Apply a work limit to the query if we were requested to.
            if( req.comboQueries[i].workLimit > 0 ) {
                reader = new LimIndexReader( reader, 
                                             req.comboQueries[i].workLimit );
            }
            
            // Make a Lucene searcher that will access the index according to
            // our query.
            //
            searcher = new IndexSearcher( reader );
            
            // Establish the XTF similarity, if the default hasn't been
            // overridden.
            //
            if( !(searcher.getSimilarity() instanceof XtfSimilarity) )
                searcher.setSimilarity( new XtfSimilarity() );
            
            // Now for the big show: get a whole bunch of hits.
            int nDocs  = req.maxDocs - hitVec.size();
            int nFound = processCombo( req.comboQueries[i], skip, 
                                       nDocs, hitVec );
            skip = Math.max( 0, skip - nFound );
            
            // Done with that searcher
            searcher.close();
            searcher = null;
            
            // Did we get enough documents yet? If not, try the next query.
            if( hitVec.size() >= req.maxDocs )
                break;
        }
        
        assert req.maxDocs < 0 || hitVec.size() <= req.maxDocs;
        
        // And we're done. Pack up the results into a tidy array.
        QueryResult result = new QueryResult();
        if( hitVec.isEmpty() ) {
            result.docHits = new DocHit[0];
            return result;
        }

        result.totalDocs = nDocsHit;
        result.startDoc  = req.startDoc;
        result.endDoc    = req.startDoc + hitVec.size();
        result.docHits   = (DocHit[]) hitVec.toArray( new DocHit[hitVec.size()] );
        
        return result;
    } // processReq()
    
    
    /**
     * QueryProcessor maintains a static cache of Lucene searchers, one for
     * each index directory. If data is changed, normally it's not recognized
     * until a periodic (every 30 seconds) check.
     * 
     * Calling this method forces new changes to an index to be immediately
     * recognized.
     */
    public static void resetCache()
    {
        searchers.clear();
    } // resetCache()
    
    
    /**
     * Processes a combination text/meta query, though one or the other may
     * be absent.
     * 
     * @param combo     The query to process
     * @param skip      How many hits to skip before adding to hitVec
     * @param maxDocs   Max number of hits to add to hitVec
     * @param hitVec    Vector for accumulating the hits
     * @return          Number of hits found (including those skipped)
     */
    private int processCombo( ComboQuery combo,
                              int        skip,
                              int        maxDocs,
                              Vector     hitVec )
        throws IOException
    {
        // Fix up all the "infinite" slop entries to be actually limited to
        // the chunk overlap size. That way, we'll get consistent results and
        // the user won't be able to tell where the chunk boundaries are.
        //
        combo.fixupSlop( chunkOverlap, chunkSize - chunkOverlap );
        
        // Translate -1 maxDocs to "essentially all"
        if( maxDocs < 0 )
            maxDocs = docNumMap.getDocCount();

        // Since the query might be used over and over, we have to make our
        // own copy of the term map that we can modify.
        //
        TermMap terms = (TermMap) combo.terms.clone();
        
        // Make a queue that will accumulate the hits and pick the first
        // load of them for us. If there is a sort field specification,
        // do it in field-sorted order; otherwise, sort by score.
        //
        PriorityQueue docHitQueue = 
                          createHitQueue( reader, combo, skip + maxDocs );
        
        // If there is a meta-query, get all of its hits first. Why? Because
        // it will generally run faster than a text query, as there are many
        // fewer documents than chunks. Also, the results can be used to 
        // speed up the text query by skipping chunks in non-matching docs.
        //
        HashMap       docHitMap   = new HashMap();
        
        if( combo.metaQuery != null )
            gatherMetaHits( combo.metaQuery,
                            (combo.textQuery != null) ? docHitMap : null,
                            (combo.textQuery == null) ? docHitQueue : null );
        
        // Now run the text query if there is one.
        if( combo.textQuery != null )
            gatherTextHits( combo.textQuery,
                            combo.sectionTypeQuery,
                            terms,
                            (combo.metaQuery != null) ? docHitMap : null,
                            docHitQueue,
                            combo.maxSnippets,
                            combo.maxContext );
        
        // Making snippets is a lot of work; we need a helper.
        SnippetMaker snippetMaker = new SnippetMaker( 
            reader, docNumMap, 
            chunkSize, chunkOverlap,
            stopSet,
            combo.maxContext, 
            terms );

        // Finally, take the high-ranking hits and add them to the hit vector.
        // Note that they come out of the hit queue in backwards order.
        //
        int nFound = docHitQueue.size();
        DocHit[] hitArray = new DocHit[nFound];
        float maxDocScore = 0.0f;
        for( int i = 0; i < nFound; i++ ) {
            int index = nFound - i - 1;
            hitArray[index] = (DocHit) docHitQueue.pop();
            maxDocScore = Math.max( maxDocScore, hitArray[index].score );
        }

        // Calculate the document score normalization factor.
        float docScoreNorm = 1.0f;
        if( skip < nFound && nFound > 0 && maxDocScore > 0.0f )  
            docScoreNorm = 1.0f / maxDocScore;

        // Calculate the chunk score norm factor as well.
        float chunkScoreNorm = 1.0f;
        if( maxSpanScore != 0.0f )
            chunkScoreNorm = 1.0f / maxSpanScore;      

        // Finish off the hits (read in the fields, normalize, make snippets).
        for( int i = skip; i < nFound; i++ ) {
            hitArray[i].finish( snippetMaker, 
                                docScoreNorm, chunkScoreNorm );
            hitVec.add( hitArray[i] );
        }

        // And we're done.
        return nFound;
    } // processCombo()

    /**
     * Creates either a standard score-sorting hit queue, or a field-sorting
     * hit queue, depending on the combo query.
     * 
     * @param combo     The query being processed
     * @return          An appropriate hit queue
     */
    private PriorityQueue createHitQueue( IndexReader reader,
                                          ComboQuery  combo, 
                                          int         nDocs )
        throws IOException
    {
        // If no sort fields, do a simple score sort.
        if( combo.sortMetaFields == null )
            return new HitQueue( nDocs );
        
        // Parse out the list of fields to sort by.
        Vector fieldNames = new Vector();
        StringTokenizer st = new StringTokenizer( combo.sortMetaFields, 
                                                  " \t\r\n,;" );
        while( st.hasMoreTokens() )
            fieldNames.add( st.nextToken() );
        
        // If there were none, do a simple score sort.
        if( fieldNames.size() == 0 )
            return new HitQueue( nDocs );
        
        // Okay, make a SortField out of each one, in priority order from 
        // highest to lowest. After all the fields, an implicit score sorter 
        // is added so that documents which match in all other respects
        // will come out ordered by score.
        //
        // Each name can be optionally prefixed with "-" to sort in reverse,
        // or "+" to sort in normal order (but "+" is unnecessary, since
        // normal order is the default.)
        //
        SortField[] fields = new SortField[fieldNames.size() + 1];
        for( int i = 0; i < fieldNames.size(); i++ ) {
            String name = (String) fieldNames.elementAt(i);
            boolean reverse = false;
            if( name.startsWith("-") ) {
                reverse = true;
                name = name.substring( 1 );
            }
            else if( name.startsWith("+") ) {
                reverse = false;
                name = name.substring( 1 );
            }
            
            fields[i] = new SortField( name, sparseStringComparator, reverse );
        }
        fields[fieldNames.size()] = SortField.FIELD_SCORE;
        
        // And make the final hit queue.
        return new FieldSortedHitQueue( reader, fields, nDocs );

    } // createHitQueue()


    /**
     * Processes a meta-data query, adding the hits to a DocHitQueue (and to
     * a map of all doc hits).
     * 
     * @param metaQuery     Meta-data query to run
     * @param docHitMap     Map to add document hits to
     * @param docHitQueue   Queue that keeps hits in ranked order
     * @throws IOException
     */
    private void gatherMetaHits( final Query         metaQuery,
                                 final HashMap       docHitMap, 
                                 final PriorityQueue docHitQueue )
        throws IOException
    {
        final Query rewritten = (stopSet == null) ? metaQuery :
            new NgramQueryRewriter(stopSet, chunkOverlap).rewriteQuery(metaQuery);
        
        if( rewritten == null )
            return;

        searcher.search( rewritten, null, new HitCollector() {
            public void collect( int doc, float score ) 
            {
                // Ignore deleted entries.
                if( score <= 0.0f )
                    return;
                
                // Make sure this is really a document, not a chunk.
                assert docNumMap.getFirstChunk(doc) >= 0;
                
                // Add the doc hit to the hit map if necessary.
                DocHit docHit = new DocHit( doc, score );
                if( docHitMap != null )
                    docHitMap.put( new Integer(doc), docHit );
                
                // If the score is high enough, add it to the queue and bump
                // the count of documents hit.
                //
                if( docHitQueue != null ) {
                    docHitQueue.insert( docHit );
                    nDocsHit++;
                }
            } // collect()
        } );
    } // gatherMetaHits()

    /**
     * Runs a full-text query (possibly constrained by a sectionType query),
     * adding the results to one or more document hits.
     * 
     * @param textQuery             Text query to run
     * @param sectionTypeQuery      Optional sectionType query, or null
     * @param termMap               Map to add matched terms to
     * @param docHitMap             Map to add hits to
     * @param docHitQueue           Queue that maintains a ranked list of hits
     * @param maxSnippets           Max # snippets for any given document
     * @param maxContext            Target # characters for a snippet
     * 
     * @return                      A HashMap of all the text hits
     * 
     * @throws IOException          If something goes wrong reading the index
     */
    private HashMap gatherTextHits( final Query         textQuery,
                                    final Query         sectionTypeQuery,
                                    final TermMap       termMap,
                                    final HashMap       docHitMap,
                                    final PriorityQueue docHitQueue,
                                    final int           maxSnippets,
                                    final int           maxContext )
        throws IOException
    {
        final HashMap textHits = new HashMap();
        
        // Apply n-gram transformations if stop-words have been removed in the
        // index.
        //
        final Query rewritten = (stopSet == null) ? textQuery :
          new NgramQueryRewriter(stopSet, chunkOverlap).rewriteQuery(textQuery);
        
        // If we ended up removing all the query clauses (probably because they
        // were all stop words), there will be no hits to collect.
        //
        if( rewritten == null )
            return textHits;
        
        // If a sectionType query was specified, filter the text hits
        // with it.
        //
        SpanQuery queryToRun;
        if( sectionTypeQuery != null )
            queryToRun = new SpanSectionTypeFilterQuery( 
                    (SpanQuery) rewritten, sectionTypeQuery );
        else
            queryToRun = (SpanQuery) rewritten;

        // If we're merging our results with a meta-search, we can optimize by
        // skipping all chunks that don't belong to the matching documents.
        //
        if( docHitMap != null )
            queryToRun = new SpanDocFilterQuery( docHitMap, docNumMap, 
                                                 queryToRun );
        
        queryToRun.setCollector( new SpanHitCollector() {
            private int    curDoc = -99;
            private DocHit docHit = null;
            
            public void collectTerms( Collection terms )
            {
                for( Iterator iter = terms.iterator(); iter.hasNext(); )
                    termMap.put( (Term)iter.next() );
            } // collectTerms()
                
            public void collectSpan( SpanHit span ) 
            {
                assert !reader.isDeleted( span.chunk );

                // Are we looking at a new main document?
                final int chunk = span.chunk;
                if( chunk > curDoc ) 
                {
                    // Finish the document we were working on.
                    flushDoc();
                    
                    // Figure out the ID of the new document.
                    curDoc = docNumMap.getDocNum( chunk );
                    if( curDoc < 0 ) {
                        Trace.error( 
                            "Unable to map chunk num " + chunk + 
                            "-> document num " +
                            "(possibly due to indexing in progress.)" );
                        return;
                    }
                    
                    // If we're merging with meta hits, find the document hit
                    // to add to. Otherwise, make a new hit.
                    //
                    if( docHitMap != null )
                    {
                        Integer key = new Integer( curDoc );
                        docHit = (DocHit) docHitMap.get( key );
                        assert docHit != null : "failed to filter docs properly";
                    }
                    else
                        docHit = new DocHit( curDoc, 0.0f );
                    
                    // Add an appropriate chunk list.
                    if( docHit.spanHitList == null )
                        docHit.spanHitList = new RankedSpanList( maxSnippets );
                }
                    
                // Add this span hit to the document's list (this also 
                // accumulates the total score of all spans.)
                //
                docHit.spanHitList.add( span );
                
                // Maintain the max score, for normalization purposes.
                maxSpanScore = Math.max( maxSpanScore, span.score );
            } // collect()
                
            public void finish() {
                flushDoc();
            }
            
            private void flushDoc()
            {
                // If no document yet, there's nothing to do.
                if( docHit == null )
                    return;
                                
                // Now let's make a combined score for the document. Use a
                // special XtfSimilarity class so that the exact computation
                // can be overridden by simply setting a different default
                // similarity.
                //
                XtfSimilarity sim = (XtfSimilarity) searcher.getSimilarity();
                float spansScore = sim.spanFreq( 
                                          docHit.spanHitList.totalScore );
                docHit.score = sim.combine( docHit.score, spansScore );
                
                // And add it to the document queue.
                docHitQueue.insert( docHit );
                docHit = null;

                // Bump the number of documents hit.
                nDocsHit++;
            } // flushDoc()

            public int getChunkSize() {
                return chunkSize;
            }

            public int getChunkOverlap() {
                return chunkOverlap;
            }
        } );
        
        searcher.search( queryToRun, null, new HitCollector() {
            public void collect( int chunk, float score ) { }
        } );
        return textHits;
    } // gatherTextHits()
    
} // class QueryProcessor
