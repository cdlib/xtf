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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.chunk.DocNumMap;
import org.apache.lucene.chunk.SpanChunkedNotQuery;
import org.apache.lucene.chunk.SparseStringComparator;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FieldSortedHitQueue;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RecordingSearcher;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SpanHitCollector;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.FieldSpanSource;
import org.apache.lucene.search.spans.SpanNotNearQuery;
import org.apache.lucene.search.spans.SpanNotQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spell.SpellReader;
import org.apache.lucene.util.PriorityQueue;
import org.cdlib.xtf.textEngine.facet.DynamicGroupData;
import org.cdlib.xtf.textEngine.facet.FacetSpec;
import org.cdlib.xtf.textEngine.facet.GroupCounts;
import org.cdlib.xtf.textEngine.facet.GroupData;
import org.cdlib.xtf.textEngine.facet.ResultFacet;
import org.cdlib.xtf.textEngine.facet.ResultGroup;
import org.cdlib.xtf.textEngine.facet.StaticGroupData;
import org.cdlib.xtf.util.CharMap;
import org.cdlib.xtf.util.Trace;
import org.cdlib.xtf.util.WordMap;

/**
 * Takes a QueryRequest, rewrites the queries if necessary to remove stop-
 * words and form bi-grams, then consults the index(es), and produces a 
 * QueryResult.
 * 
 * @author Martin Haye
 */
public class DefaultQueryProcessor extends QueryProcessor
{
    /** Map of all XtfSearchers, so we can re-use them */
    private static HashMap searchers = new HashMap();
    
    /** Lucene reader from which to read index data */
    private IndexReader    indexReader;
    
    /** Fetches spelling suggestions */
    private SpellReader    spellReader;

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
    
    /** Whether the index is "sparse" (i.e. more than 5 chunks per doc) */
    private boolean        isSparse;
    
    /** Total number of documents hit (not just those that scored high) */
    private int            nDocsHit;
    
    /** Maximum document score (used to normalize scores) */
    private float          maxDocScore;
    
    /** Document normalization factor (calculated from {@link #maxDocScore}) */
    private float          docScoreNorm;
    
    /** Comparator used for sorting strings */
    private static final SparseStringComparator sparseStringComparator = 
        new SparseStringComparator(); 
    
    /**
     * This is main entry point. Takes a pre-parsed query request and handles 
     * searching the index and forming the results.<br>
     * 
     * This method is synchronized because it uses two instance variables,
     * so access by multiple threads would result in incorrect counting. For
     * maximum efficiency, each thread should really use its own instance.
     * 
     * @param req      The pre-parsed request to process
     * @return         Zero or more document hits
     */
    public synchronized QueryResult processRequest( final QueryRequest req )
        throws IOException
    {
        // Clear out our counters.
        nDocsHit = 0;
        maxDocScore = 0;
        
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
            indexReader  = xtfSearcher.indexReader();
            docNumMap    = xtfSearcher.docNumMap();
            chunkSize    = xtfSearcher.chunkSize();
            chunkOverlap = xtfSearcher.chunkOverlap();
            stopSet      = xtfSearcher.stopSet();
            pluralMap    = xtfSearcher.pluralMap();
            accentMap    = xtfSearcher.accentMap();
            spellReader  = xtfSearcher.spellReader();
            isSparse     = xtfSearcher.isSparse();
        }
        
        // Apply a work limit to the query if we were requested to. If no
        // specific limit was set, use a limiter with an infinite limit 
        // (because we still need it to check periodically if the thread 
        // should kill itself.)
        //
        IndexReader limReader = new XtfLimIndexReader( indexReader, 
            (req.workLimit > 0) ? req.workLimit : Integer.MAX_VALUE );
        
        // Translate -1 maxDocs to "essentially all"
        int maxDocs = req.maxDocs;
        if( maxDocs < 0 )
            maxDocs = docNumMap.getDocCount();

        // Make a queue that will accumulate the hits and pick the first
        // load of them for us. If there is a sort field specification,
        // do it in field-sorted order; otherwise, sort by score.
        //
        final PriorityQueue docHitQueue =
            createHitQueue( indexReader, 
                            req.startDoc + req.maxDocs, 
                            req.sortMetaFields,
                            isSparse );
        
        // Start making the result by filling in its context.
        QueryResult result = new QueryResult();
        result.context = new QueryContext();
        result.context.accentMap = accentMap;
        result.context.pluralMap = pluralMap;
        result.context.stopSet   = stopSet;
        result.scoresNormalized  = req.normalizeScores;
        
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
        
        // Rewrite the query for bigrams (if we have stop-words to deal with.)
        if( stopSet != null )
            query = new XtfBigramQueryRewriter(stopSet, chunkOverlap).
                            rewriteQuery(query);
        
        // If there's nothing left (for instance if the query was all stop-words)
        // then there will be no results.
        //
        if( query == null ) {
            result.docHits = new DocHit[0];
            return result;
        }
        
        // Fix up all the "infinite" slop entries to be actually limited to
        // the chunk overlap size. That way, we'll get consistent results and
        // the user won't be able to tell where the chunk boundaries are.
        //
        final Query finalQuery = 
            new SlopFixupRewriter(docNumMap, stopSet, pluralMap, accentMap).
            rewriteQuery(query);
        
        // If debugging is enabled, print out the final rewritten and fixed
        // up query.
        //
        if( finalQuery != req.query )
            Trace.debug( "Rewritten query: " + finalQuery.toString() );

        // While processing the query, we want to lazily generate DocHits,
        // and only generate a DocHit once even if it's added to multiple
        // groups.
        //
        final DocHitMakerImpl docHitMaker = new DocHitMakerImpl();
        
        // If we're to apply a set of additional boost sets to the documents,
        // get the set now.
        //
        final BoostSet boostSet = (req.boostSetParams == null) ? null :
            BoostSet.getCachedSet( indexReader, 
                                   new File(req.boostSetParams.path), 
                                   req.boostSetParams.field );
        
        // Make a Lucene searcher that will access the index according to
        // our query.
        //
        RecordingSearcher searcher = new RecordingSearcher( limReader );
        
        // If grouping was specified by the query, read in all the group data.
        // Note that the GroupData class holds its own cache so we don't have
        // to read data for a given field more than once.
        //
        final GroupCounts[] groupCounts = (req.facetSpecs == null) ? null :
                    prepGroups( req, boostSet, searcher, finalQuery );
        
        // Now for the big show... go get the hits!
        searcher.search( finalQuery, null, new SpanHitCollector() {
            public void collect( int doc, float score, FieldSpanSource spanSource ) 
            {
                // Apply a boost (if there's a boost set)
                score = applyBoost( doc, score, boostSet, req );

                // Ignore deleted entries, and entries boosted down to zero.
                if( score <= 0.0f )
                    return;
                
                // Bump the count of documents hit, and update the max score.
                nDocsHit++;
                if( score > maxDocScore )
                    maxDocScore = score;
                
                // Record the hit.
                docHitMaker.reset( doc, score, spanSource );
                if( req.maxDocs > 0 ) {
                    if( req.maxDocs >= 999999999 )
                        docHitQueue.ensureCapacity( 1 );
                    docHitMaker.insertInto( docHitQueue );
                }
                
                // If grouping is enabled, add this document to the counts.
                if( groupCounts != null ) {
                    for( int i = 0; i < groupCounts.length; i++ )
                        groupCounts[i].addDoc( docHitMaker );
                }
            } // collect()
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
        if( req.normalizeScores && maxDocScore > 0.0f )  
            docScoreNorm = 1.0f / maxDocScore;
        
        // We'll need a query weight if we're being asked to explain the
        // scores.
        //
        Weight weight = null;
        if( req.explainScores )
            weight = finalQuery.weight( searcher );

        // Finish off the hits (read in the fields, normalize, make snippets).
        SnippetMaker snippetMaker = new SnippetMaker( limReader,
                                                      docNumMap,
                                                      stopSet,
                                                      pluralMap,
                                                      accentMap,
                                                      req.maxContext,
                                                      req.termMode );
        for( int i = req.startDoc; i < nFound; i++ ) {
            if( req.explainScores ) {
                hitArray[i].finishWithExplain( 
                    snippetMaker, docScoreNorm, weight,
                    boostSet, req.boostSetParams );
            }
            else
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
        
        // Make spelling suggestions if applicable.
        if( spellReader != null && req.spellcheckParams != null )
            spellCheck( req, result );
        
        // And we're done. Pack up the results into a tidy array.
        result.totalDocs = nDocsHit;
        result.startDoc  = req.startDoc;
        result.endDoc    = req.startDoc + hitVec.size();
        result.docHits   = (DocHit[]) hitVec.toArray( new DocHit[hitVec.size()] );
        
        return result;
    } // processReq()
    
    /**
     * Checks spelling of query terms, if spelling suggestion is enabled and
     * the result falls below the cutoff threshholds.
     * 
     * @param req   Original query request
     * @param res   Results of the query
     */
    private void spellCheck( QueryRequest req, QueryResult res )
        throws IOException
    {
        // First, gather a list of all the query terms.
        ArrayList queryTerms = gatherTerms( req.query );
        
        // We can use a handy reference to the spellcheck params, and to the
        // field list.
        //
        SpellcheckParams params = req.spellcheckParams;
        
        // Check the cutoffs. If the documents scored well, or there were
        // a lot of them, then suggestions aren't needed.
        //
        if( maxDocScore > params.docScoreCutoff )
            return;
        if( res.totalDocs > params.totalDocsCutoff )
            return;
        
        // Make suggestions for each term
        ArrayList out = new ArrayList();
        for( int i = 0; i < queryTerms.size(); i++ ) 
        {
            // Check if this term's field falls within the list of fields 
            // for which we want suggestions. If not, skip it.
            //
            Term term = (Term) queryTerms.get( i );
            if( params.fields != null && !params.fields.contains(term.field()) )
                continue;
          
            // Get some suggestions
            SpellingSuggestion sugg = new SpellingSuggestion();
            sugg.origTerm = term;
            sugg.altWords = spellReader.suggestSimilar(
                term.text(),
                req.spellcheckParams.suggestionsPerTerm,
                indexReader,
                term.field(),
                params.termOccurrenceFactor,
                params.accuracy );
            
            // If any alternatives suggested, record the result.
            if( sugg.altWords.length > 0 )
                out.add( sugg );
        } // for i
        
        // Convert to an array.
        if( out.size() > 0 ) {
            res.suggestions = (SpellingSuggestion[])
                out.toArray( new SpellingSuggestion[out.size()] );
        }
        
    } // spellCheck()
    
    
    /**
     * Make a list of all the terms present in the given query.
     * 
     * @param query   The query to traverse
     * @return        List of the terms
     */
    private ArrayList gatherTerms( Query query )
    {
        final ArrayList list = new ArrayList();
        
        XtfQueryTraverser trav = new XtfQueryTraverser() {
          public void traverseQuery( Query q ) {
              // Skip queries boosted to nothing
              if( q.getBoost() > 0.001f )
                  super.traverseQuery( q );
          }
          protected void traverse( TermQuery q ) {
              list.add( q.getTerm() );
          }
          protected void traverse( SpanTermQuery q ) {
              list.add( q.getTerm() );
          }
          protected void traverse(BooleanQuery bq) {
              BooleanClause[] clauses = bq.getClauses();
              for (int i = 0; i < clauses.length; i++) {
                  if( !clauses[i].prohibited )
                      traverseQuery(clauses[i].query);
              }
          } // traverse()
          protected void traverse(SpanChunkedNotQuery nq) {
              traverseQuery(nq.getInclude());
              // No: traverseQuery(nq.getExclude());
          } // traverse()
          protected void traverse(SpanNotQuery nq) {
            traverseQuery(nq.getInclude());
            // No: traverseQuery(nq.getExclude());
          } // traverse()
          protected void traverse(SpanNotNearQuery nq) {
            traverseQuery(nq.getInclude());
            // No: traverseQuery(nq.getExclude());
          } // traverse()
        };
        trav.traverseQuery( query );
        
        return list;
    } // gatherTerms()

    /**
     * Create the GroupCounts objects for the given query request. Also handles
     * creating the proper hit queue for each one.
     * 
     * @param req       query request containing group specs
     * @param query     query to use to form dynamic groups
     * @param searcher  searcher for dynamic groups
     * @param boostSet  boost set for dynamic groups
     */
    private GroupCounts[] prepGroups( final QueryRequest req, 
                                      final BoostSet boostSet, 
                                      RecordingSearcher searcher, 
                                      Query query )
      throws IOException
    {
        GroupData[] groupData = new GroupData[req.facetSpecs.length];
        Vector dynamicGroupVec = new Vector();
        
        // First get data for each group
        for( int i = 0; i < req.facetSpecs.length; i++ ) {
            FacetSpec spec = req.facetSpecs[i];
            if( spec.field.startsWith("java:") ) {
                groupData[i] = createDynamicGroup( indexReader, spec.field );
                dynamicGroupVec.add( groupData[i] );
            }
            else
                groupData[i] = StaticGroupData.getCachedData( indexReader, spec.field );
        }
        
        // If there are dynamic groups, pre-scan the query and hand them the
        // documents and scores.
        //
        if( !dynamicGroupVec.isEmpty() ) {
            final DynamicGroupData[] dynGroups = (DynamicGroupData[])
                dynamicGroupVec.toArray( new DynamicGroupData[dynamicGroupVec.size()] );
            searcher.search( query, null, new SpanHitCollector() {
                public void collect( int doc, float score, FieldSpanSource spanSource ) 
                {
                    // Apply a boost (if there's a boost set)
                    score = applyBoost( doc, score, boostSet, req );
                    
                    // If document isn't deleted, collect it.
                    if( score > 0.0f ) {
                        for( int i = 0; i < dynGroups.length; i++ )
                            dynGroups[i].collect(  doc, score );
                    }
                } // collect()
            } );
            
            // Finish off the dynamic group data.
            for( int i = 0; i < dynGroups.length; i++ )
                dynGroups[i].finish();
        } // if

        // Now make a GroupCount object around each data object.
        GroupCounts[] groupCounts = new GroupCounts[req.facetSpecs.length];
        for( int i = 0; i < req.facetSpecs.length; i++ ) {
          FacetSpec spec = req.facetSpecs[i];
          HitQueueMakerImpl maker = new HitQueueMakerImpl( 
              indexReader, spec.sortDocsBy, isSparse );
          groupCounts[i] = new GroupCounts( groupData[i], spec, maker );
      }
      
        // All done.
        return groupCounts;
        
    } // prepGroups()

    /**
     * Create a dynamic group based on a field specification.
     * 
     * @param indexReader   Where to get the data from
     * @param field         Special field name starting with "java:"
     * @return              Dynamic group data
     * @throws IOException 
     */
    private GroupData createDynamicGroup( IndexReader indexReader, String field ) 
        throws IOException
    {
        // Parse out the class name and parameters
        Pattern pat = Pattern.compile( "java:([\\w.]+)\\((.*)\\)" );
        Matcher matcher = pat.matcher( field );
        if( !matcher.matches() )
            throw new RuntimeException( "Unrecognized dynamic facet field '" + field + "'" );
        
        String className = matcher.group( 1 );
        String params    = matcher.group( 2 );
        
        // Create an instance of the given class.
        DynamicGroupData dynData = null;
        try {
            Class c = Class.forName( className );
            dynData = (DynamicGroupData) c.newInstance();
        } 
        catch (ClassNotFoundException e) {
            throw new RuntimeException( "Dynamic facet class '" + className + "' not found" );
        } 
        catch (InstantiationException e) {
            throw new RuntimeException( "Cannot instantiate dynamic facet class '" + className + "'", e );
        } 
        catch (IllegalAccessException e) {
            throw new RuntimeException( "Cannot instantiate dynamic facet class '" + className + "'", e );
        } 
        catch( ClassCastException e ) {
            throw new RuntimeException( "Class '" + className + "' must be derived from DynamicGroupData" );
        }

        // Initialize the new instance, and we're done.
        dynData.init( indexReader, params );
        return dynData;
        
    } // createDynamicGroup()

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
     * If a boost set was specified, boost the given document's score according to the
     * set.
     */
    private float applyBoost(int doc, float score, 
                             BoostSet boostSet, QueryRequest req)
    {
        // If we're boosting, apply that factor.
        if( score > 0 && boostSet != null ) {
            float boost = boostSet.getBoost( doc, req.boostSetParams.defaultBoost );
            if( req.boostSetParams.exponent != 1.0f )
                boost = (float) Math.pow(boost, req.boostSetParams.exponent);
            score *= boost;
        }
        
        return score;
    }

    /**
     * Creates either a standard score-sorting hit queue, or a field-sorting
     * hit queue, depending on whether the query is to be sorted.
     *
     * @param reader     will be used to read the field contents
     * @param size       size of the queue (typically startDoc + maxDocs)
     * @param sortFields space or comma delimited list of fields to sort by
     * @param isSparse   if index is sparse (i.e. more than 5 chunks per doc)
     * @return           an appropriate hit queue
     */
    private static PriorityQueue createHitQueue( IndexReader  reader,
                                                 int          size,
                                                 String       sortFields,
                                                 boolean      isSparse )
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
            
            if( isSparse )
                fields[i] = new SortField( name, sparseStringComparator, reverse );
            else
                fields[i] = new SortField( name, SortField.STRING, reverse );
        }
        fields[fieldNames.size()] = SortField.FIELD_SCORE;
        
        // And make the final hit queue.
        return new FieldSortedHitQueue( reader, fields, size );

    } // createHitQueue()

    private static class DocHitMakerImpl implements GroupCounts.DocHitMaker
    {
      private int             doc;
      private float           score;
      private FieldSpanSource spanSrc;
      private DocHitImpl      docHit;
      
      public final void reset( int doc, float score, FieldSpanSource spanSrc ) {
        this.doc     = doc;
        this.score   = score;
        this.spanSrc = spanSrc;
        
        docHit = null;
      }
      
      public final int getDocNum() {
        return doc;
      }
      
      public final boolean insertInto( PriorityQueue queue ) {
        boolean justMade = false;
        if( docHit == null ) {
            docHit = new DocHitImpl( doc, score );
            justMade = true;
        }
        
        boolean inserted = queue.insert( docHit );
        
        if( inserted && justMade )
            docHit.setSpans( spanSrc.getSpans(doc) );
        
        return inserted;
      }
    } // class DocHitMaker
    
    private static class HitQueueMakerImpl implements GroupCounts.HitQueueMaker
    {
      private IndexReader reader;
      private String      sortFields;
      private boolean     isSparse;
      
      public HitQueueMakerImpl( IndexReader reader, 
                                String sortFields, 
                                boolean isSparse ) 
      {
        this.reader = reader;
        this.sortFields = sortFields;
        this.isSparse = isSparse;
      }
      
      public PriorityQueue makeQueue( int size ) {
        try {
            return DefaultQueryProcessor.createHitQueue( 
                         reader, size, sortFields, isSparse );
        }
        catch( IOException e ) {
            throw new RuntimeException( e );
        }
      }
    } // class HitQueueMakerImpl
} // class QueryProcessor
