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
 *
 * Acknowledgements:
 *
 * A significant amount of new and/or modified code in this module
 * was made possible by a grant from the Andrew W. Mellon Foundation,
 * as part of the Melvyl Recommender Project.
 */
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
import org.apache.lucene.search.FlippableStringComparator;
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
import org.apache.lucene.spelt.SpellReader;
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
  private IndexReader indexReader;

  /** Fetches spelling suggestions */
  private SpellReader spellReader;

  /** Keeps track of which chunks belong to which documents */
  private DocNumMap docNumMap;

  /** Max size of a chunk (in words) */
  @SuppressWarnings("unused")
  private int chunkSize;

  /** Number of words a chunk shares with its successor */
  private int chunkOverlap;

  /** Stop-words to remove (e.g. "the", "a", "and", etc.) */
  private Set stopSet;

  /** Mapping of plural words to singular words */
  private WordMap pluralMap;

  /** Mapping of accented chars to chars without diacritics */
  private CharMap accentMap;

  /** Whether the index is "sparse" (i.e. more than 5 chunks per doc) */
  private boolean isSparse;

  /** Total number of documents hit (not just those that scored high) */
  private int nDocsHit;

  /** Maximum document score (used to normalize scores) */
  private float maxDocScore;

  /** Document normalization factor (calculated from {@link #maxDocScore}) */
  private float docScoreNorm;

  /** Used to warm up indexes prior to use */
  private static IndexWarmer indexWarmer;

  /** Comparator used for sorting strings in "sparse" indexes */
  private static final SparseStringComparator sparseStringComparator = new SparseStringComparator();

  /** Comparator used for sorting strings in "compact" indexes */
  private static final FlippableStringComparator compactStringComparator = new FlippableStringComparator();
  
  /** Record an index warmer to use for background warming. */
  public static void setIndexWarmer(IndexWarmer warmer) {
    indexWarmer = warmer;
  }

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
  public synchronized QueryResult processRequest(final QueryRequest req)
    throws IOException 
  {
    // Clear out our counters.
    nDocsHit = 0;
    maxDocScore = 0;

    // Make an vector to store the hits (we'll make it into an array
    // later, when we know how many we have.)
    //
    Vector hitVec = new Vector(10);

    if (indexWarmer == null)
      throw new IOException("Fatal: must call setIndexWarmer() before DefaultQueryProcessor.processRequest()");

    // Get a reader, searcher, and document number map that will all be
    // consistent with each other and up-to-date.
    //
    XtfSearcher xtfSearcher = indexWarmer.getSearcher(req.indexPath);
    synchronized (xtfSearcher) {
      xtfSearcher.update();
      indexReader = xtfSearcher.indexReader();
      docNumMap = xtfSearcher.docNumMap();
      chunkSize = xtfSearcher.chunkSize();
      chunkOverlap = xtfSearcher.chunkOverlap();
      stopSet = xtfSearcher.stopSet();
      pluralMap = xtfSearcher.pluralMap();
      accentMap = xtfSearcher.accentMap();
      spellReader = xtfSearcher.spellReader();
      isSparse = xtfSearcher.isSparse();
    }

    // Apply a work limit to the query if we were requested to. If no
    // specific limit was set, use a limiter with an infinite limit 
    // (because we still need it to check periodically if the thread 
    // should kill itself.)
    //
    IndexReader limReader = new XtfLimIndexReader(indexReader,
                                                  (req.workLimit > 0)
                                                  ? req.workLimit
                                                  : Integer.MAX_VALUE);

    // Translate -1 maxDocs to "essentially all"
    int maxDocs = req.maxDocs;
    if (maxDocs < 0)
      maxDocs = docNumMap.getDocCount();

    // Make a queue that will accumulate the hits and pick the first
    // load of them for us. If there is a sort field specification,
    // do it in field-sorted order; otherwise, sort by score.
    //
    final PriorityQueue docHitQueue = createHitQueue(indexReader,
                                                     req.startDoc +
                                                       req.maxDocs,
                                                     req.sortMetaFields,
                                                     isSparse);

    // Start making the result by filling in its context.
    QueryResult result = new QueryResult();
    result.context = new QueryContext();
    result.context.accentMap = accentMap;
    result.context.pluralMap = pluralMap;
    result.context.stopSet = stopSet;
    result.scoresNormalized = req.normalizeScores;

    // If no query was specified, then there will be no results.
    Query query = req.query;
    if (query == null) {
      result.docHits = new DocHit[0];
      return result;
    }

    // Perform standard tokenization tasks: change words to lowercase,
    // remove apostrophes, etc.
    //
    Set tokFields = xtfSearcher.tokenizedFields();
    query = new StdTermRewriter(tokFields).rewriteQuery(query);

    // If a plural map is present, change plural words to non-plural.
    if (pluralMap != null)
      query = new PluralFoldingRewriter(pluralMap, tokFields).rewriteQuery(query);

    // If an accent map is present, remove diacritics.
    if (accentMap != null)
      query = new AccentFoldingRewriter(accentMap, tokFields).rewriteQuery(query);

    // Rewrite the query for bigrams (if we have stop-words to deal with.)
    if (stopSet != null)
      query = new XtfBigramQueryRewriter(stopSet, chunkOverlap, tokFields).rewriteQuery(
        query);

    // If there's nothing left (for instance if the query was all stop-words)
    // then there will be no results.
    //
    if (query == null) {
      result.docHits = new DocHit[0];
      return result;
    }

    // Fix up all the "infinite" slop entries to be actually limited to
    // the chunk overlap size. That way, we'll get consistent results and
    // the user won't be able to tell where the chunk boundaries are. 
    // Also, attach the docNumMap to every SpanDechunkingQuery.
    //
    final Query finalQuery = new SlopFixupRewriter(docNumMap,
                                                   stopSet,
                                                   pluralMap,
                                                   accentMap).rewriteQuery(query);

    // If debugging is enabled, print out the final rewritten and fixed
    // up query.
    //
    if (finalQuery != req.query)
      Trace.debug("Rewritten query: " + finalQuery.toString());

    // While processing the query, we want to lazily generate DocHits,
    // and only generate a DocHit once even if it's added to multiple
    // groups.
    //
    final DocHitMakerImpl docHitMaker = new DocHitMakerImpl();

    // If we're to apply a set of additional boost sets to the documents,
    // get the set now.
    //
    final BoostSet boostSet = (req.boostSetParams == null) ? null
      : BoostSet.getCachedSet(indexReader,
          new File(
            req.boostSetParams.path),
          req.boostSetParams.field);

    // Make a Lucene searcher that will access the index according to
    // our query.
    //
    RecordingSearcher searcher = new RecordingSearcher(limReader);

    // If grouping was specified by the query, read in all the group data.
    // Note that the GroupData class holds its own cache so we don't have
    // to read data for a given field more than once.
    //
    final GroupCounts[] groupCounts = (req.facetSpecs == null) ? null
                                      : prepGroups(req,
                                                   boostSet,
                                                   searcher,
                                                   finalQuery);

    // Now for the big show... go get the hits!
    searcher.search(finalQuery, null,
      new SpanHitCollector() 
      {
        public void collect(int doc, float score, FieldSpanSource spanSource) 
        {
          // Apply a boost (if there's a boost set)
          score = applyBoost(doc, score, boostSet, req);

          // Ignore deleted entries, and entries boosted down to zero.
          if (score <= 0.0f)
            return;

          // Bump the count of documents hit, and update the max score.
          nDocsHit++;
          if (score > maxDocScore)
            maxDocScore = score;

          // Record the hit.
          docHitMaker.reset(doc, score, spanSource);
          if (req.maxDocs > 0)
            docHitMaker.insertInto(docHitQueue);

          // If grouping is enabled, add this document to the counts.
          if (groupCounts != null) {
            for (int i = 0; i < groupCounts.length; i++)
              groupCounts[i].addDoc(docHitMaker);
          }
        } // collect()
      });

    // Take the high-ranking hits and add them to the hit vector.
    // Note that they come out of the hit queue in backwards order.
    //
    int nFound = docHitQueue.size();
    DocHitImpl[] hitArray = new DocHitImpl[nFound];
    for (int i = 0; i < nFound; i++) {
      int index = nFound - i - 1;
      hitArray[index] = (DocHitImpl)docHitQueue.pop();
    }

    // Calculate the document score normalization factor.
    docScoreNorm = 1.0f;
    if (req.normalizeScores && maxDocScore > 0.0f)
      docScoreNorm = 1.0f / maxDocScore;

    // We'll need a query weight if we're being asked to explain the
    // scores.
    //
    Weight weight = null;
    if (req.explainScores)
      weight = finalQuery.weight(searcher);

    // Finish off the hits (read in the fields, normalize, make snippets).
    SnippetMaker snippetMaker = new SnippetMaker(limReader,
                                                 docNumMap,
                                                 stopSet,
                                                 pluralMap,
                                                 accentMap,
                                                 req.maxContext,
                                                 req.termMode);
    for (int i = req.startDoc; i < nFound; i++) 
    {
      if (req.explainScores) {
        hitArray[i].finishWithExplain(snippetMaker,
                                      docScoreNorm,
                                      weight,
                                      boostSet,
                                      req.boostSetParams);
      }
      else
        hitArray[i].finish(snippetMaker, docScoreNorm);
      if (result.textTerms == null)
        result.textTerms = hitArray[i].textTerms();
      hitVec.add(hitArray[i]);
    }

    // If grouping was enabled, group the hits and finish all of them.
    if (groupCounts != null) 
    {
      result.facets = new ResultFacet[groupCounts.length];
      for (int i = 0; i < groupCounts.length; i++) {
        result.facets[i] = groupCounts[i].getResult();
        finishGroup(result.facets[i].rootGroup,
                    snippetMaker,
                    req,
                    weight,
                    boostSet);
      } // for if
    }

    // Done with that searcher
    searcher.close();
    searcher = null;
    assert req.maxDocs < 0 || hitVec.size() <= req.maxDocs;

    // Pack up the results into a tidy array.
    result.totalDocs = nDocsHit;
    result.startDoc = req.startDoc;
    result.endDoc = req.startDoc + hitVec.size();
    result.docHits = (DocHit[])hitVec.toArray(new DocHit[hitVec.size()]);

    // Make spelling suggestions if applicable.
    if (spellReader != null && req.spellcheckParams != null)
      spellCheck(req, result, tokFields);

    // All done.
    return result;
  } // processReq()

  /**
   * Checks spelling of query terms, if spelling suggestion is enabled and
   * the result falls below the cutoff threshholds.
   *
   * @param req   Original query request
   * @param res   Results of the query
   * @param tokFields  Set of tokenized fields (in case no field list was
   *                   specified in the query request.)
   */
  private void spellCheck(QueryRequest req, QueryResult res, Set tokFields)
    throws IOException 
  {
    // We can use a handy reference to the spellcheck params, and to the
    // field list.
    //
    SpellcheckParams params = req.spellcheckParams;

    // When checking the cutoffs, account for the possibility that
    // the query might be faceted, in which case we want the document
    // count of the biggest facet.
    //
    int totalDocs = res.totalDocs;
    if (res.facets != null) 
    {
      for (int i = 0; i < res.facets.length; i++) {
        if (res.facets[i].rootGroup != null)
          totalDocs = Math.max(totalDocs, res.facets[i].rootGroup.totalDocs);
      }
    }

    // Check the cutoffs. If the documents scored well, or there were
    // a lot of them, then suggestions aren't needed.
    //
    if (params.docScoreCutoff > 0 && maxDocScore > params.docScoreCutoff)
      return;
    if (params.totalDocsCutoff > 0 && totalDocs > params.totalDocsCutoff)
      return;

    // Gather the query terms, grouped by field set.
    Set spellFieldSet = params.fields != null ? params.fields : tokFields;
    LinkedHashMap fieldsMap = gatherKeywords(req.query, spellFieldSet);

    // Make suggestions for each field set.
    LinkedHashMap out = new LinkedHashMap();
    for (Iterator fi = fieldsMap.keySet().iterator(); fi.hasNext();) 
    {
      // Make a list of fields and terms.
      LinkedHashSet fieldsSet = (LinkedHashSet)fi.next();
      String[] fields = (String[])fieldsSet.toArray(new String[fieldsSet.size()]);

      LinkedHashSet termsSet = (LinkedHashSet)fieldsMap.get(fieldsSet);
      String[] terms = (String[])termsSet.toArray(new String[termsSet.size()]);

      // Get some suggestions
      String[] suggested = spellReader.suggestKeywords(terms);

      // If no suggestions, skip these fields.
      if (suggested == null)
        continue;
      assert suggested.length == terms.length;

      // Record each suggestion.
      for (int i = 0; i < suggested.length; i++) 
      {
        // Skip duplicated in different field sets... retain only the
        // first one.
        //
        if (out.containsKey(terms[i]))
          continue;

        // Skip suggestions that don't change anything.
        if (terms[i].equals(suggested[i]))
          continue;

        // Okay, record it.
        SpellingSuggestion sugg = new SpellingSuggestion();
        sugg.origTerm = terms[i];
        sugg.fields = fields;
        sugg.suggestedTerm = suggested[i];
        out.put(terms[i], sugg);
      }
    } // for fi

    // If no suggestions, we're done.
    if (out.size() == 0)
      return;

    // Make sure the suggestions result in better results.
    if (!spellingImprovesResults(req, res, spellFieldSet, out))
      return;

    // Record the final suggestions in an array.
    res.suggestions = (SpellingSuggestion[])out.values().toArray(
      new SpellingSuggestion[out.values().size()]);
  } // spellCheck()

  /**
   * Re-runs the original query, except with terms replaced by their suggestions.
   * Checks that the results are improved -- at present that means that there
   * are more of them, and their max score is higher.
   *
   * @param origReq   Original query request
   * @param origRes   Results of the original query
   * @param spellFieldSet  Set of fields to rewrite terms within
   * @param suggs     Map of terms to their suggested replacements
   * @return          true if the suggestions improve the results.
   * @throws IOException
   */
  private boolean spellingImprovesResults(QueryRequest origReq,
                                          QueryResult origRes,
                                          Set spellFieldSet, LinkedHashMap suggs)
    throws IOException 
  {
    // First, clone the original request, and then turn off spellcheck for
    // the clone so that we don't get in an infinite recursive loop.
    //
    QueryRequest newReq = (QueryRequest)origReq.clone();
    newReq.spellcheckParams = null;

    // Before re-querying, save the max doc score.
    float origMaxDocScore = maxDocScore;

    // Now apply the spelling suggestions to the original query.
    newReq.query = new SpellSuggRewriter(suggs, spellFieldSet).rewriteQuery(
      newReq.query);
    QueryResult newRes = this.processRequest(newReq);

    // If the new query returns nothing and the old query also returned
    // nothing, it's a semi-failure. There's no use suggesting the new
    // words even if they are better, because it won't help the user.
    //
    if (newRes.totalDocs == 0 && origRes.totalDocs == 0) 
    {
      //System.out.print("No docs before or after: " + newReq.query.toString() + "... ");
      return false;
    }

    // If the new query returns less results, consider it a failure.
    if (newRes.totalDocs < origRes.totalDocs) 
    {
      //System.out.print("Fewer docs: " + newReq.query.toString() + "... ");
      return false;
    }

    // If the max doc score is lower, that's also a failure.
    if (maxDocScore < origMaxDocScore) 
    {
      //System.out.print("Lower score: " + newReq.query.toString() + "... ");
      return false;
    }

    // Cool! We think this is a better query.
    return true;
  }

  /**
   * Make a list of all the terms present in the given query,
   * grouped by field set.
   *
   * @param query          The query to traverse
   * @param desiredFields  The set of fields to limit to. If null, all
   *                       fields are considered.
   *
   * @return  An ordered map consisting of entries of a key and a
   *          value. The key is an ordered set of field names.
   *          The value is an ordered set of words.
   */
  private LinkedHashMap gatherKeywords(Query query, final Set desiredFields) 
  {
    // Make an ordered set of words, each with an ordered list of fields
    final LinkedHashMap termMap = new LinkedHashMap();

    XtfQueryTraverser trav = new XtfQueryTraverser() 
    {
      private void add(Term t) {
        final String field = t.field();
        final String word = t.text();
        if (desiredFields != null && !desiredFields.contains(field))
          return;
        if (!termMap.containsKey(word))
          termMap.put(word, new LinkedHashSet());
        ((LinkedHashSet)termMap.get(word)).add(field);
      }

      public void traverseQuery(Query q) 
      {
        // Skip queries boosted to nothing
        if (q.getBoost() > 0.001f)
          super.traverseQuery(q);
      }

      protected void traverse(TermQuery q) {
        add(q.getTerm());
      }

      protected void traverse(SpanTermQuery q) {
        add(q.getTerm());
      }
      
      protected void traverse(SpanExactQuery q) {
        // Do not correct inside "exact" queries
      }

      protected void traverse(BooleanQuery bq) 
      {
        BooleanClause[] clauses = bq.getClauses();
        for (int i = 0; i < clauses.length; i++) {
          if (clauses[i].getOccur() != BooleanClause.Occur.MUST_NOT)
            traverseQuery(clauses[i].getQuery());
        }
      } // traverse()

      protected void traverse(SpanChunkedNotQuery nq) 
      {
        traverseQuery(nq.getInclude());

        // No: traverseQuery(nq.getExclude());
      } // traverse()

      protected void traverse(SpanNotQuery nq) 
      {
        traverseQuery(nq.getInclude());

        // No: traverseQuery(nq.getExclude());
      } // traverse()

      protected void traverse(SpanNotNearQuery nq) 
      {
        traverseQuery(nq.getInclude());

        // No: traverseQuery(nq.getExclude());
      } // traverse()
    };
    trav.traverseQuery(query);

    // Now invert: for each unique set of fields, make an ordered list
    // of the keywords.
    //
    LinkedHashMap fieldsMap = new LinkedHashMap();
    for (Iterator ti = termMap.keySet().iterator(); ti.hasNext();) 
    {
      String word = (String)ti.next();
      LinkedHashSet fieldsSet = (LinkedHashSet)termMap.get(word);

      if (!fieldsMap.containsKey(fieldsSet))
        fieldsMap.put(fieldsSet, new LinkedHashSet());
      ((LinkedHashSet)fieldsMap.get(fieldsSet)).add(word);
    }

    // All done.
    return fieldsMap;
  } // gatherKeywords()

  /**
   * Create the GroupCounts objects for the given query request. Also handles
   * creating the proper hit queue for each one.
   *
   * @param req       query request containing group specs
   * @param query     query to use to form dynamic groups
   * @param searcher  searcher for dynamic groups
   * @param boostSet  boost set for dynamic groups
   */
  private GroupCounts[] prepGroups(final QueryRequest req,
                                   final BoostSet boostSet,
                                   RecordingSearcher searcher, Query query)
    throws IOException 
  {
    GroupData[] groupData = new GroupData[req.facetSpecs.length];
    Vector dynamicGroupVec = new Vector();

    // First get data for each group
    for (int i = 0; i < req.facetSpecs.length; i++) 
    {
      FacetSpec spec = req.facetSpecs[i];
      if (spec.field.startsWith("java:")) {
        groupData[i] = createDynamicGroup(indexReader, spec.field);
        dynamicGroupVec.add(groupData[i]);
      }
      else
        groupData[i] = StaticGroupData.getCachedData(indexReader, spec.field);
    }

    // If there are dynamic groups, pre-scan the query and hand them the
    // documents and scores.
    //
    if (!dynamicGroupVec.isEmpty()) 
    {
      final DynamicGroupData[] dynGroups = (DynamicGroupData[])dynamicGroupVec.toArray(
        new DynamicGroupData[dynamicGroupVec.size()]);
      searcher.search(query, null,
                      new SpanHitCollector() 
      {
          public void collect(int doc, float score, FieldSpanSource spanSource) 
          {
            // Apply a boost (if there's a boost set)
            score = applyBoost(doc, score, boostSet, req);

            // If document isn't deleted, collect it.
            if (score > 0.0f) {
              for (int i = 0; i < dynGroups.length; i++)
                dynGroups[i].collect(doc, score);
            }
          } // collect()
      });

      // Finish off the dynamic group data.
      for (int i = 0; i < dynGroups.length; i++)
        dynGroups[i].finish();
    } // if

    // Now make a GroupCount object around each data object.
    GroupCounts[] groupCounts = new GroupCounts[req.facetSpecs.length];
    for (int i = 0; i < req.facetSpecs.length; i++) {
      FacetSpec spec = req.facetSpecs[i];
      HitQueueMakerImpl maker = new HitQueueMakerImpl(indexReader,
                                                      spec.sortDocsBy,
                                                      isSparse);
      groupCounts[i] = new GroupCounts(groupData[i], spec, maker);
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
  private GroupData createDynamicGroup(IndexReader indexReader, String field)
    throws IOException 
  {
    // Parse out the class name and parameters
    Pattern pat = Pattern.compile("java:([\\w.]+)\\((.*)\\)");
    Matcher matcher = pat.matcher(field);
    if (!matcher.matches())
      throw new RuntimeException(
        "Unrecognized dynamic facet field '" + field + "'");

    String className = matcher.group(1);
    String params = matcher.group(2);

    // Create an instance of the given class.
    DynamicGroupData dynData = null;
    try {
      Class c = Class.forName(className);
      dynData = (DynamicGroupData)c.newInstance();
    }
    catch (ClassNotFoundException e) {
      throw new RuntimeException(
        "Dynamic facet class '" + className + "' not found");
    }
    catch (InstantiationException e) {
      throw new RuntimeException(
        "Cannot instantiate dynamic facet class '" + className + "'",
        e);
    }
    catch (IllegalAccessException e) {
      throw new RuntimeException(
        "Cannot instantiate dynamic facet class '" + className + "'",
        e);
    }
    catch (ClassCastException e) {
      throw new RuntimeException(
        "Class '" + className + "' must be derived from DynamicGroupData");
    }

    // Initialize the new instance, and we're done.
    dynData.init(indexReader, params);
    return dynData;
  } // createDynamicGroup()

  /**
   * Finishes DocHits within a single group (also processes all its
   * descendant groups.)
   *
   * @param group         Group to finish
   * @param snippetMaker  Used to make snippets for any DocHits inside the
   *                      group.
   * @param req           Determines whether to finish with 'explain' or not
   * @param weight        Used for score explanations
   * @param boostSet      Used for score explanations
   */
  private void finishGroup(ResultGroup group, SnippetMaker snippetMaker,
                           QueryRequest req, Weight weight, BoostSet boostSet)
    throws IOException 
  {
    // Finish DocHits for this group
    if (group.docHits != null) 
    {
      for (int k = 0; k < group.docHits.length; k++) 
      {
        DocHitImpl hit = (DocHitImpl)group.docHits[k];
        if (req.explainScores) {
          hit.finishWithExplain(snippetMaker,
                                docScoreNorm,
                                weight,
                                boostSet,
                                req.boostSetParams);
        }
        else
          hit.finish(snippetMaker, docScoreNorm);
      } // for k
    }

    // Now finish all the descendants.
    if (group.subGroups != null) {
      for (int j = 0; j < group.subGroups.length; j++)
        finishGroup(group.subGroups[j], snippetMaker, req, weight, boostSet);
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
  public void resetCache() {
    searchers.clear();
  } // resetCache()

  /**
   * If a boost set was specified, boost the given document's score according to the
   * set.
   */
  private float applyBoost(int doc, float score, BoostSet boostSet,
                           QueryRequest req) 
  {
    // If we're boosting, apply that factor.
    if (score > 0 && boostSet != null) {
      float boost = boostSet.getBoost(doc, req.boostSetParams.defaultBoost);
      if (req.boostSetParams.exponent != 1.0f)
        boost = (float)Math.pow(boost, req.boostSetParams.exponent);
      score *= boost;
    }

    return score;
  }
  
  /**
   * Creates either a standard score-sorting hit queue, or a field-sorting
   * hit queue, depending on whether the query is to be sorted.
   *
   * @param reader     will be used to read the field contents
   * @param inSize     size of the queue (typically startDoc + maxDocs). If
   *                   this number is >= 999999, an infinitely resizing
   *                   queue will be created.
   * @param sortFields space or comma delimited list of fields to sort by
   * @param isSparse   if index is sparse (i.e. more than 5 chunks per doc)
   * @return           an appropriate hit queue
   */
  private static PriorityQueue createHitQueue(IndexReader reader, int inSize,
                                              String sortFields,
                                              boolean isSparse)
    throws IOException 
  {
    // If a large size is requested, start with a small queue and expand
    // later, if necessary.
    //
    int size = (inSize >= 999999) ? 1 : inSize;

    // If no sort fields, do a simple score sort.
    PriorityQueue ret;
    if (sortFields == null)
      ret = new HitQueue(size);
    else
    {
      // Parse out the list of fields to sort by.
      Vector fieldNames = new Vector();
      StringTokenizer st = new StringTokenizer(sortFields, " \t\r\n,;");
      while (st.hasMoreTokens())
        fieldNames.add(st.nextToken());
  
      // If there were none, do a simple score sort.
      if (fieldNames.size() == 0)
        ret = new HitQueue(size);
      else
      {
        // Okay, make a SortField out of each one, in priority order from 
        // highest to lowest. After all the fields, an implicit score sorter 
        // is added so that documents which match in all other respects
        // will come out ordered by score.
        //
        // Each name can be optionally prefixed with "-" to sort in reverse,
        // or "+" to sort in normal order (but "+" is unnecessary, since
        // normal order is the default.)
        //
        // There's also a more verbose and powerful way to affect sort order: 
        // modifiers. Possible modifiers are ":ascending", ":descending", 
        // ":emptyFirst", and ":emptyLast".
        //
        SortField[] fields = new SortField[fieldNames.size() + 2];
        for (int i = 0; i < fieldNames.size(); i++) 
        {
          String name = (String)fieldNames.elementAt(i);
          boolean ascending = false;
          boolean descending = false;
          boolean emptyFirst = false;
          boolean emptyLast = false;
          
          // Check for the short-hand "-" and "+" prefixes
          if (name.startsWith("-")) {
            descending = true;
            name = name.substring(1);
          }
          else if (name.startsWith("+")) {
            ascending = true;
            name = name.substring(1);
          }

          // Check for more verbose ":" modifiers after the field name
          String[] parts = name.split(":");
          name = parts[0];
          for (int j=1; j<parts.length; j++) 
          {
            if (parts[j].equalsIgnoreCase("ascending"))
              ascending = true;
            else if (parts[j].equalsIgnoreCase("descending"))
              descending = true;
            else if (parts[j].equalsIgnoreCase("emptyFirst"))
              emptyFirst = true;
            else if (parts[j].equalsIgnoreCase("emptyLast"))
              emptyLast = true;
            else
              throw new IOException("Unknown sort modifier: '" + parts[j] + "'");
          }
          
          // Check for conflicting modifiers.
          if ((ascending && descending) || (emptyFirst && emptyLast))
            throw new IOException("Conflicting sort modifiers");
          
          // Interpret the modifiers.
          boolean reverse;
          if (ascending)
            reverse = false;
          else if (descending)
            reverse = true;
          else
            reverse = false; // default
          
          boolean flipEmpty;
          if (!reverse) {
            if (emptyFirst)
              flipEmpty = true;
            else if (emptyLast)
              flipEmpty = false;
            else
              flipEmpty = false; // default
          }
          else {
            if (emptyFirst)
              flipEmpty = false;
            else if (emptyLast)
              flipEmpty = true;
            else
              flipEmpty = true; // default
          }
    
          String finalName = flipEmpty ? (name + ":flipEmpty") : name;
          
          // Though not strictly necessary, allow the user to specify "score" or
          // "relevance" to sort by those. That way, automated programs can always give
          // a "sortDocsBy" field.
          //
          if (name.equals("score") || name.equals("relevance")) {
            if (reverse || flipEmpty)
              throw new RuntimeException("Illegal modifier on sortDocsBy 'score'");
            fields[i] = SortField.FIELD_SCORE;
          }
          else if (isSparse)
            fields[i] = new SortField(finalName, sparseStringComparator, reverse);
          else
            fields[i] = new SortField(finalName, compactStringComparator, reverse);
        }
        
        // Default tie-breakers: first, score. If score is equal, sort by doc ID.
        fields[fieldNames.size()]   = SortField.FIELD_SCORE;
        fields[fieldNames.size()+1] = SortField.FIELD_DOC;
    
        // And make the final hit queue.
        ret = new FieldSortedHitQueue(reader, fields, size);
      }
    }
    
    // If a ton of hits is requested, make the queue into a resizing one.
    if (inSize >= 999999)
      ret.setExpandable();
    
    // All done.
    return ret;
  } // createHitQueue()

  private static class DocHitMakerImpl implements GroupCounts.DocHitMaker 
  {
    private int doc;
    private float score;
    private FieldSpanSource spanSrc;
    private DocHitImpl docHit;

    public final void reset(int doc, float score, FieldSpanSource spanSrc) 
    {
      this.doc = doc;
      this.score = score;
      this.spanSrc = spanSrc;

      docHit = null;
    }

    public final int getDocNum() {
      return doc;
    }

    public final float getScore() {
      return score;
    }

    public final boolean insertInto(PriorityQueue queue) 
    {
      boolean justMade = false;
      if (docHit == null) {
        docHit = new DocHitImpl(doc, score);
        justMade = true;
      }

      boolean inserted = queue.insert(docHit);

      if (inserted && justMade)
        docHit.setSpans(spanSrc.getSpans(doc));

      return inserted;
    }
  } // class DocHitMaker

  private static class HitQueueMakerImpl implements GroupCounts.HitQueueMaker 
  {
    private IndexReader reader;
    private String sortFields;
    private boolean isSparse;

    public HitQueueMakerImpl(IndexReader reader, String sortFields,
                             boolean isSparse) 
    {
      this.reader = reader;
      this.sortFields = sortFields;
      this.isSparse = isSparse;
    }

    public PriorityQueue makeQueue(int size) 
    {
      try {
        return DefaultQueryProcessor.createHitQueue(reader,
                                                    size,
                                                    sortFields,
                                                    isSparse);
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  } // class HitQueueMakerImpl
} // class QueryProcessor
