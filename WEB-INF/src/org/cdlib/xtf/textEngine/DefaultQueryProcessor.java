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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.lucene.chunk.DocNumMap;
import org.apache.lucene.chunk.SpanChunkedNotQuery;
import org.apache.lucene.chunk.SpanDechunkingQuery;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.limit.LimIndexReader;
import org.apache.lucene.mark.SpanHitCollector;
import org.apache.lucene.mark.FieldSpans;
import org.apache.lucene.ngram.NgramQueryRewriter;
import org.apache.lucene.search.FieldSortedHitQueue;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SparseStringComparator;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanRangeQuery;
import org.apache.lucene.search.spans.SpanWildcardQuery;
import org.apache.lucene.util.PriorityQueue;
import org.cdlib.xtf.textEngine.facet.FacetSpec;
import org.cdlib.xtf.textEngine.facet.GroupCounts;
import org.cdlib.xtf.textEngine.facet.GroupData;
import org.cdlib.xtf.textEngine.facet.ResultFacet;
import org.cdlib.xtf.textEngine.facet.ResultGroup;
import org.cdlib.xtf.util.CharMap;
import org.cdlib.xtf.util.Trace;
import org.cdlib.xtf.util.WordMap;

/**
 * Takes a QueryRequest, rewrites the queries if necessary to remove stop-
 * words and form n-grams, then consults the index(es), and produces a 
 * QueryResult.
 * 
 * @author Martin Haye
 */
public class DefaultQueryProcessor extends QueryProcessor
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
    
    /** Mapping of plural words to singular words */
    private WordMap        pluralMap;
    
    /** Mapping of accented chars to chars without diacritics */
    private CharMap        accentMap;
    
    /** Total number of documents hit (not just those that scored high) */
    private int            nDocsHit = 0;
    
    /** Maximum document score (used to normalize scores) */
    private float          maxDocScore = 0;
    
    /** Document normalization factor (calculated from {@link #maxDocScore}) */
    private float          docScoreNorm;
    
    /** Comparator used for sorting strings */
    private static final SparseStringComparator sparseStringComparator = 
        new SparseStringComparator(); 
    
    /**
     * This is main entry point. Takes a pre-parsed query request and handles 
     * searching the index and forming the results.
     * 
     * @param req      The pre-parsed request to process
     * @return         Zero or more document hits
     */
    public QueryResult processRequest( final QueryRequest req )
        throws IOException
    {
        // Make an vector to store the hits (we'll make it into an array
        // later, when we know how many we have.)
        //
        Vector hitVec = new Vector( 10 );
        
        String indexPath = req.indexPath;
        
        XtfSearcher xtfSearcher = null;
        synchronized( searchers ) {
            xtfSearcher = (XtfSearcher) searchers.get( indexPath );
            if( xtfSearcher == null ) {
                xtfSearcher = new XtfSearcher( indexPath, 30 ); // 30-sec update chk
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
            pluralMap    = xtfSearcher.pluralMap();
            accentMap    = xtfSearcher.accentMap();
        }
        
        // Apply a work limit to the query if we were requested to.
        if( req.workLimit > 0 ) {
            reader = new LimIndexReader( reader, 
                                         req.workLimit );
        }
        
        // Make a Lucene searcher that will access the index according to
        // our query.
        //
        searcher = new IndexSearcher( reader );
        
        // Translate -1 maxDocs to "essentially all"
        int maxDocs = req.maxDocs;
        if( maxDocs < 0 )
            maxDocs = docNumMap.getDocCount();

        // Make a queue that will accumulate the hits and pick the first
        // load of them for us. If there is a sort field specification,
        // do it in field-sorted order; otherwise, sort by score.
        //
        final PriorityQueue docHitQueue =
            createHitQueue( reader, 
                            req.startDoc + req.maxDocs, 
                            req.sortMetaFields );
        
        // Start making the result by filling in its context.
        QueryResult result = new QueryResult();
        result.context = new QueryContext();
        result.context.accentMap = accentMap;
        result.context.pluralMap = pluralMap;
        result.context.stopSet   = stopSet;
        
        // If no query was specified, then there will be no results.
        Query query = req.query;
        if( query == null ) {
            result.docHits = new DocHit[0];
            return result;
        }
        
        // If a plural map is present, change plural words to non-plural.
        if( pluralMap != null )
            query = new PluralFoldingRewriter(pluralMap).rewriteQuery(query);
        
        // If an accent map is present, remove diacritics.
        if( accentMap != null )
            query = new AccentFoldingRewriter(accentMap).rewriteQuery(query);
        
        // Rewrite the query for ngrams (if we have stop-words to deal with.)
        if( stopSet != null )
            query = new NgramQueryRewriter(stopSet, chunkOverlap).
                            rewriteQuery(query);
        
        // If there's nothing left (for instance if the query was all stop-words)
        // then there will be no results.
        //
        if( query == null ) {
            result.docHits = new DocHit[0];
            return result;
        }
        
        final Query finalQuery = query;
        if( finalQuery != req.query )
            Trace.debug( "Rewritten query: " + finalQuery.toString() );

        // Fix up all the "infinite" slop entries to be actually limited to
        // the chunk overlap size. That way, we'll get consistent results and
        // the user won't be able to tell where the chunk boundaries are.
        //
        fixupSlop( finalQuery, docNumMap, stopSet );
        
        // If grouping was specified by the query, read in all the group data.
        // Note that the GroupData class holds its own cache so we don't have
        // to read data for a given field more than once.
        //
        final GroupCounts[] groupCounts = (req.facetSpecs == null) ? null :
                                          prepGroups( req );
        
        // While processing the query, we want to lazily generate DocHits,
        // and only generate a DocHit once even if it's added to multiple
        // groups.
        //
        final DocHitMakerImpl docHitMaker = new DocHitMakerImpl();
        
        // If we're to apply a set of additional boost sets to the documents,
        // get the set now.
        //
        final BoostSet boostSet = (req.boostSetPath == null) ? null :
            BoostSet.getCachedSet(reader, docNumMap, new File(req.boostSetPath) );
        
        // Now for the big show... go get the hits!
        searcher.search( finalQuery, null, new SpanHitCollector() {
            public void collect( int doc, float score, FieldSpans fieldSpans ) 
            {
                // Ignore deleted entries.
                if( score <= 0.0f )
                    return;
                
                // Make sure this is really a document, not a chunk.
                if( docNumMap.getFirstChunk(doc) < 0 )
                    return;
                
                // If we're boosting, apply that factor.
                if( boostSet != null )
                    score *= boostSet.getBoost( doc );

                // Bump the count of documents hit, and update the max score.
                nDocsHit++;
                if( score > maxDocScore )
                    maxDocScore = score;
                
                // Record the hit.
                docHitMaker.reset( doc, score, fieldSpans );
                if( req.maxDocs > 0 ) {
                    if( req.maxDocs >= 999999999 )
                        docHitQueue.ensureCapacity( 1 );
                    docHitQueue.insert( docHitMaker.getDocHit() );
                }
                
                // If grouping is enabled, add this document to the counts.
                if( groupCounts != null ) {
                    for( int i = 0; i < groupCounts.length; i++ )
                        groupCounts[i].addDoc( docHitMaker );
                }
            } // collect()
            
            // If no field marks, just record a null for them.
            public void collect( int doc, float score ) {
                collect( doc, score, null );
            }
        } );
        
        // Take the high-ranking hits and add them to the hit vector.
        // Note that they come out of the hit queue in backwards order.
        //
        int nFound = docHitQueue.size();
        DocHitImpl[] hitArray = new DocHitImpl[nFound];
        for( int i = 0; i < nFound; i++ ) {
            int index = nFound - i - 1;
            hitArray[index] = (DocHitImpl) docHitQueue.pop();
        }

        // Calculate the document score normalization factor.
        docScoreNorm = 1.0f;
        if( maxDocScore > 0.0f )  
            docScoreNorm = 1.0f / maxDocScore;

        // Finish off the hits (read in the fields, normalize, make snippets).
        SnippetMaker snippetMaker = new SnippetMaker( reader,
                                                      docNumMap,
                                                      stopSet,
                                                      pluralMap,
                                                      accentMap,
                                                      req.maxContext,
                                                      req.termMode );
        for( int i = req.startDoc; i < nFound; i++ ) {
            hitArray[i].finish( snippetMaker, docScoreNorm );
            if( result.textTerms == null )
                result.textTerms = hitArray[i].textTerms();
            hitVec.add( hitArray[i] );
        }
        
        // If grouping was enabled, group the hits and finish all of them.
        if( groupCounts != null ) {
            result.facets = new ResultFacet[groupCounts.length];
            for( int i = 0; i < groupCounts.length; i++ ) {
                result.facets[i] = groupCounts[i].getResult();
                finishGroup( result.facets[i].rootGroup, snippetMaker );
            } // for if
        }

        // Done with that searcher
        searcher.close();
        searcher = null;
        
        assert req.maxDocs < 0 || hitVec.size() <= req.maxDocs;
        
        // And we're done. Pack up the results into a tidy array.
        result.totalDocs = nDocsHit;
        result.startDoc  = req.startDoc;
        result.endDoc    = req.startDoc + hitVec.size();
        result.docHits   = (DocHit[]) hitVec.toArray( new DocHit[hitVec.size()] );
        
        return result;
    } // processReq()

    /**
     * Create the GroupCounts objects for the given query request. Also handles
     * creating the proper hit queue for each one.
     * 
     * @param req Query request containing group specs
     */
    private GroupCounts[] prepGroups( QueryRequest req )
      throws IOException
    {
        GroupCounts[] groupCounts = new GroupCounts[req.facetSpecs.length];
        
        for( int i = 0; i < req.facetSpecs.length; i++ ) {
            FacetSpec spec = req.facetSpecs[i];
            GroupData data = GroupData.getCachedData( reader, spec.field );
            HitQueueMakerImpl maker = 
                                new HitQueueMakerImpl( reader, spec.sortDocsBy );
            groupCounts[i] = new GroupCounts( data, spec, maker );
        }
        
        return groupCounts;
        
    } // prepGroups()

    /**
     * Process group counts into final results. This includes forming the
     * groups, and finishing each DocHit with a snippetMaker (and normalizing
     * scores within that group.)
     * 
     * @param req           Query request containing the group specs
     * @param result        Where to stuff the resulting groups
     * @param groupCounts   Group counts to use
     * @param snippetMaker  Used to make snippets for any DocHits inside the
     *                      groups.
     */
    private void finishGroups( QueryRequest  req, 
                               QueryResult   result, 
                               GroupCounts[] groupCounts,
                               SnippetMaker  snippetMaker )
    {
        result.facets = new ResultFacet[groupCounts.length];
        for( int i = 0; i < groupCounts.length; i++ )
        {
            result.facets[i] = groupCounts[i].getResult();
            
            // Scan each group for DocHits, and finish all we find.
            finishGroup( result.facets[i].rootGroup, snippetMaker );
        } // for if
        
    } // finishGroups()
    
    /**
     * Finishes DocHits within a single group (also processes all its
     * descendant groups.)
     * 
     * @param group         Group to finish
     * @param snippetMaker  Used to make snippets for any DocHits inside the
     *                      group.
     */
    private void finishGroup( ResultGroup group,
                              SnippetMaker  snippetMaker )
    {
      // Finish DocHits for this group
      if( group.docHits != null ) {
          for( int k = 0; k < group.docHits.length; k++ ) {
              DocHitImpl hit = (DocHitImpl) group.docHits[k];
              hit.finish( snippetMaker, docScoreNorm );
          } // for k
      }
      
      // Now finish all the descendants.
      if( group.subGroups != null ) {
          for( int j = 0; j < group.subGroups.length; j++ ) 
              finishGroup( group.subGroups[j], snippetMaker );
      }
        
    } // finishGroup()
    
    /**
     * After index parameters are known, this method should be called to
     * update the slop parameters of queries that need to know. Also informs
     * wildcard and range queries of the stopword set.
     */
    private void fixupSlop( Query query, DocNumMap docNumMap, Set stopSet )
    {
        // First, fix up this query if necessary.
        boolean isText = (query instanceof SpanQuery) ?
            ((SpanQuery)query).getField().equals("text") : false;
            
        if( query instanceof SpanNearQuery ) {
            SpanNearQuery nq = (SpanNearQuery) query;
            
            // For text queries, set the max to the chunk overlap size. For
            // meta-data fields, set it to the bump between multiple values
            // for the same field, *minus one* to prevent matches across the
            // boundary.
            //
            int maxSlop = isText ? docNumMap.getChunkOverlap() : (1000000-1);
            nq.setSlop( Math.min(nq.getSlop(), maxSlop) );
        }
        else if( query instanceof SpanChunkedNotQuery ) {
            SpanChunkedNotQuery nq = (SpanChunkedNotQuery) query;
            nq.setSlop( 
                Math.min(nq.getSlop(), docNumMap.getChunkOverlap()),
                docNumMap.getChunkSize() - docNumMap.getChunkOverlap() ); 
        }
        else if( query instanceof SpanDechunkingQuery ) {
            SpanDechunkingQuery dq = (SpanDechunkingQuery) query;
            dq.setDocNumMap( docNumMap );
        }
        else if( query instanceof SpanWildcardQuery ) {
            SpanWildcardQuery wq = (SpanWildcardQuery) query;
            wq.setStopWords( stopSet );
        }
        else if( query instanceof SpanRangeQuery ) {
            SpanRangeQuery rq = (SpanRangeQuery) query;
            rq.setStopWords( stopSet );
        }
        
        // Now process any sub-queries it has.
        Query[] subQueries = query.getSubQueries();
        if( subQueries != null ) {
            for( int i = 0; i < subQueries.length; i++ )
                fixupSlop( subQueries[i], docNumMap, stopSet );
        }
    } // fixupSlop()
    
    /**
     * QueryProcessor maintains a static cache of Lucene searchers, one for
     * each index directory. If data is changed, normally it's not recognized
     * until a periodic (every 30 seconds) check.
     * 
     * Calling this method forces new changes to an index to be immediately
     * recognized.
     */
    public void resetCache()
    {
        searchers.clear();
    } // resetCache()
    
    
    /**
     * Creates either a standard score-sorting hit queue, or a field-sorting
     * hit queue, depending on whether the query is to be sorted.
     *
     * @param reader     will be used to read the field contents
     * @param size       size of the queue (typically startDoc + maxDocs)
     * @param sortFields space or comma delimited list of fields to sort by
     * @return           an appropriate hit queue
     */
    private static PriorityQueue createHitQueue( IndexReader  reader,
                                                 int          size,
                                                 String       sortFields )
        throws IOException
    {
        // If a large size is requested, start with a small queue and expand
        // later, if necessary.
        //
        if( size >= 999999999 )
            size = 10;
      
        // If no sort fields, do a simple score sort.
        if( sortFields == null )
            return new HitQueue( size );
        
        // Parse out the list of fields to sort by.
        Vector fieldNames = new Vector();
        StringTokenizer st = new StringTokenizer( sortFields, " \t\r\n,;" );
        while( st.hasMoreTokens() )
            fieldNames.add( st.nextToken() );
        
        // If there were none, do a simple score sort.
        if( fieldNames.size() == 0 )
            return new HitQueue( size );
        
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
        return new FieldSortedHitQueue( reader, fields, size );

    } // createHitQueue()

    private static class DocHitMakerImpl implements GroupCounts.DocHitMaker
    {
      private int        doc;
      private float      score;
      private FieldSpans spans;
      private DocHit     docHit;
      
      public final void reset( int doc, float score, FieldSpans spans ) {
        this.doc   = doc;
        this.score = score;
        this.spans = spans;
        
        docHit = null;
      }
      
      public final int getDocNum() {
        return doc;
      }
      
      public final DocHit getDocHit() {
        if( docHit == null )
            docHit = new DocHitImpl( doc, score, spans );
        return docHit;
      }
    } // class DocHitMaker
    
    private static class HitQueueMakerImpl implements GroupCounts.HitQueueMaker
    {
      private IndexReader reader;
      private String      sortFields;
      
      public HitQueueMakerImpl( IndexReader reader, String sortFields ) {
        this.reader = reader;
        this.sortFields = sortFields;
      }
      
      public PriorityQueue makeQueue( int size ) {
        try {
            return DefaultQueryProcessor.createHitQueue( 
                         reader, size, sortFields );
        }
        catch( IOException e ) {
            throw new RuntimeException( e );
        }
      }
    } // class HitQueueMakerImpl
} // class QueryProcessor
