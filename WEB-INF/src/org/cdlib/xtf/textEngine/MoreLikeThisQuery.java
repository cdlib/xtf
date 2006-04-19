package org.cdlib.xtf.textEngine;

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
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DefaultSimilarity;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.HitCollector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.PriorityQueue;
import org.cdlib.xtf.textIndexer.XTFTextAnalyzer;
import org.cdlib.xtf.util.CharMap;
import org.cdlib.xtf.util.Trace;
import org.cdlib.xtf.util.WordMap;

/** 
 * Processes the sub-query and uses the first document as the "target".
 * Then we determine the most "interesting" terms in the target document,
 * and finally perform a query on those terms to find more like the target.
 * The target document itself will NOT be included in the results.
 */
public class MoreLikeThisQuery extends Query 
{
  private Query subQuery;
  private int targetDoc;
  private Set stopSet;
  private WordMap pluralMap;
  private CharMap accentMap;
  
  /** Ignore words less freqent that this. */
  private int minTermFreq = 2;

  /** Ignore words which do not occur in at least this many docs. */
  private int minDocFreq = 2;

  /** Ignore words which occur in at least this many docs. */
  private int maxDocFreq = -1;

  /** Should we apply a boost to the Query based on the scores? */
  private boolean boost = true;

  /** Field name(s) we'll analyze. */
  private String[] fields = null;
  
  /** Boost values for the fields */
  private Map boostMap = new HashMap();

  /**
   * The maximum number of tokens to parse in each example doc field that is 
   * not stored with TermVector support
   */
  private int maxNumTokensParsed = 5000;

  /** Ignore words if less than this len. */
  private int minWordLen = 4;

  /** Ignore words if greater than this len. */
  private int maxWordLen = 12;

  /** Don't return a query longer than this. */
  private int maxQueryTerms = 10;

  /** For idf() calculations. */
  private Similarity similarity = new DefaultSimilarity();


  /** Constructs a span query selecting all terms greater than
   * <code>lowerTerm</code> but less than <code>upperTerm</code>.
   * There must be at least one term and either term may be null,
   * in which case there is no bound on that side, but if there are
   * two terms, both terms <b>must</b> be for the same field. Applies
   * a limit on the total number of terms matched.
   */
  public MoreLikeThisQuery( Query subQuery )
  {
    this.subQuery = subQuery;
  }
  
  /** Retrieve the sub-query */
  public Query getSubQuery() {
    return subQuery;
  }
  
  /** Set the sub-query */
  public void setSubQuery(Query subQuery) {
    this.subQuery = subQuery;
  }

  /** Establish the set of stop words to ignore */
  public void setStopWords( Set set ) {
      this.stopSet = set;
  }
  
  /** Establish the plural map in use */
  public void setPluralMap( WordMap map ) {
      this.pluralMap = map;
  }
  
  /** Establish the accent map in use */
  public void setAccentMap( CharMap map ) {
      this.accentMap = map;
  }

  /** Ignore words which occur in at least this many docs. */
  public void setMaxDocFreq(int maxDocFreq) {
    this.maxDocFreq = maxDocFreq;
  }
  
  /** Field name(s) we'll analyze. */
  public void setFieldNames(String[] fieldNames) 
  {
    // Parse out field boosts, and get rid of fields with zero boost.
    boostMap = new HashMap( fieldNames.length );
    ArrayList fieldList = new ArrayList( fieldNames.length );
    
    for( int i = 0; i < fieldNames.length; i++ ) {
        String field = fieldNames[i];
        int caretPos = field.indexOf('^');
        if( caretPos < 0 )
            fieldList.add( field );
        else {
            String name  = field.substring(0, caretPos);
            float  boost = Float.parseFloat( field.substring(caretPos+1) );
            if( boost == 0.0f )
                continue;
            
            fieldList.add( name );
            if( boost != 1.0f )
                boostMap.put( name, new Float(boost) );
        }
    }
    
    this.fields = (String[]) fieldList.toArray( new String[fieldList.size()] );
  }
  
  /**
   * The maximum number of tokens to parse in each example doc field that is 
   * not stored with TermVector support
   */
  public void setMaxNumTokensParsed(int maxNumTokensParsed) {
    this.maxNumTokensParsed = maxNumTokensParsed;
  }
  
  /** Don't return a query longer than this. */
  public void setMaxQueryTerms(int maxQueryTerms) {
    this.maxQueryTerms = maxQueryTerms;
  }
  
  /** Ignore words if greater than this len. */
  public void setMaxWordLen(int maxWordLen) {
    this.maxWordLen = maxWordLen;
  }
  
  /** Ignore words which do not occur in at least this many docs. */
  public void setMinDocFreq(int minDocFreq) {
    this.minDocFreq = minDocFreq;
  }
  
  /** Ignore words less freqent that this. */
  public void setMinTermFreq(int minTermFreq) {
    this.minTermFreq = minTermFreq;
  }
  
  /** Ignore words if less than this len. */
  public void setMinWordLen(int minWordLen) {
    this.minWordLen = minWordLen;
  }

  /** Should we apply a boost to the Query based on the scores? */
  public void setBoost(boolean boost) {
    this.boost = boost;
  }

  /**
   * Generate a query that will produce "more documents like" the first
   * in the sub-query.
   */
  public Query rewrite( IndexReader reader ) throws IOException 
  {
    // Determine the target document.
    IndexSearcher searcher = new IndexSearcher( reader );
    targetDoc = -1;
    HitCollector collector = new HitCollector() {
        public void collect( int doc, float score ) {
            if( targetDoc < 0 )
                targetDoc = doc;
        }
    }; 
    
    searcher.search( subQuery, collector );
    
    // If none, make a query that will definitely return nothing at all.
    if( targetDoc < 0 )
        return new TermQuery( new Term("fribbleSnarf", "!*@&#(*&") );
    
    // gather list of valid fields from lucene
    String[] fields = this.fields;
    if( fields == null ) 
    {
        Collection fieldColl = reader.getFieldNames( true );
        ArrayList fieldList = new ArrayList( fieldColl.size() );
        for( Iterator iter = fieldColl.iterator(); iter.hasNext(); ) {
            String field = (String) iter.next();
            if( !field.equals("chunkCount") &&
                !field.equals("docInfo") &&
                !field.equals("fileDate") &&
                !field.equals("indexInfo") && 
                !field.equals("key") && 
                !field.equals("recordNum") &&
                !field.equals("text") &&
                !field.startsWith("sort-") &&
                !field.startsWith("facet-") )
            {
                fieldList.add( field );
            }
        }
        fields = (String[]) fieldList.toArray(new String[fieldList.size()]);
    }

    // If we've been asked to calculate the max document frequence, do it now.
    if( maxDocFreq < 0 ) {
        int nDocs = reader.docFreq(new Term("docInfo", "1"));
        maxDocFreq = Math.max( 5, nDocs / 20 );
    }
    
    // Make the "more like this" query
    Analyzer analyzer = new XTFTextAnalyzer( null, pluralMap, accentMap );
    Query rawQuery = createQuery( retrieveTerms(reader, targetDoc, analyzer) );
    
    // Exclude the original document in the result set.
    Query ret = new MoreLikeWrapper( this, rawQuery );
    Trace.info( "More-like query: " + ret );
    if( Trace.getOutputLevel() >= Trace.debug )
        Trace.debug( "More-like query: " + ret );

    return ret;
  }
  
  
  /**
   * Create the More like query from a PriorityQueue
   */
  private Query createQuery(PriorityQueue q) 
  {
    // Pop everything from the queue.
    QueryWord[] queryWords = new QueryWord[q.size()];
    for( int i = q.size()-1; i >= 0; i-- )
        queryWords[i] = (QueryWord) q.pop();
    
    BooleanQuery query = new BooleanQuery();
    QueryWord qw;
    
    // At the moment, there's no need to scale by the best score. It simply
    // clouds the query explanation. It doesn't affect the scores, since
    // Lucene applies a query normalization factor anyway.
    //
    //float bestScore = (queryWords.length > 0) ? queryWords[0].score : 0.0f;

    for( int i = 0; i < queryWords.length; i++ )
    {
        qw = queryWords[i];
        TermQuery tq = new TermQuery( qw.term );

        if( boost )
            tq.setBoost( qw.score /* / bestScore */ );
        
        try {
            query.add( tq, false, false );
        }
        catch (BooleanQuery.TooManyClauses ignore) {
            break;
        }
    }

    return query;
    
  } // createQuery()

  
  /**
   * Create a PriorityQueue from a word->tf map.
   *
   * @param words a map of words keyed on the word(String) with Int objects as the values.
   */
  private PriorityQueue createQueue( IndexReader indexReader, Map words ) 
      throws IOException 
  {
    // Will order words by score
    int queueSize = Math.min( words.size(), maxQueryTerms );
    QueryWordQueue queue = new QueryWordQueue( queueSize );
    
    // For reference in score calculations, get the total # of docs in index
    int numDocs = indexReader.numDocs();

    // For each term...
    Iterator it = words.keySet().iterator();
    while (it.hasNext()) 
    { 
        Term term = (Term) it.next();
        
        // Filter out words that don't occur enough times in the source doc
        int tf = ((Int) words.get(term)).x; 
        if( minTermFreq > 0 && tf < minTermFreq )
            continue; 

        // Filter out words that don't occur in enough docs
        int docFreq = indexReader.docFreq( term );
        if( minDocFreq > 0 && docFreq < minDocFreq )
            continue; 

        // Filter out words that occur in too many docs
        if( maxDocFreq > 0 && docFreq > maxDocFreq )
            continue; 

        // Handle potential index update problem
        if (docFreq == 0)
            continue; 

        // Calculate a score for this term.
        float idf = similarity.idf(docFreq, numDocs);
        float score = tf * idf;

        // Boost if necessary.
        Float found = (Float) boostMap.get( term.field() );
        if( found != null )
            score *= found.floatValue();
        
        // Okay, add an entry to the queue.
        queue.insert( new QueryWord(term, score) );
    }
    
    return queue;
    
  } // createQueue()

  /**
   * Find words for a more-like-this query former.
   *
   * @param docNum the id of the lucene document from which to find terms
   */
  private PriorityQueue retrieveTerms( IndexReader indexReader, 
                                       int         docNum, 
                                       Analyzer    analyzer ) 
    throws IOException 
  {
    Map termFreqMap = new HashMap();
    Document d = indexReader.document( docNum );
    
    for( int i = 0; i < fields.length; i++ ) 
    {
        String fieldName = fields[i];
        String text[] = d.getValues( fieldName );
        if( text == null )
            continue;
        
        for (int j = 0; j < text.length; j++) 
        {
            TokenStream tokens = 
                analyzer.tokenStream( fieldName, new StringReader(text[j]) );
            addTermFrequencies( tokens, fieldName, termFreqMap );
        } // for j
    } // for i

    return createQueue( indexReader, termFreqMap );
  }
  
  /**
   * Adds term frequencies found by tokenizing text from reader into the Map 
   * words.
   * 
   * @param r a source of text to be tokenized
   * @param termFreqMap a Map of terms and their frequencies
   * @param fieldName Used by analyzer for any special per-field analysis
   */
  private void addTermFrequencies( TokenStream tokens, String field, Map termFreqMap )
    throws IOException
  {
    Token token;
    int tokenCount=0;
    while( (token = tokens.next()) != null ) 
    {
        tokenCount++;
        if( tokenCount > maxNumTokensParsed )
            break;
        
        String word = token.termText();
        if( isNoiseWord(word) )
            continue;
        
        // increment frequency
        Term term = new Term( field, word );
        Int cnt = (Int) termFreqMap.get(term);
        if (cnt == null)
            termFreqMap.put(term, new Int());
        else
            cnt.x++;
    }
  }
  

  /** 
   * Determines if the passed term is likely to be of interest in "more like" 
   * comparisons 
   * 
   * @param term The word being considered
   * 
   * @return true if should be ignored, false if should be used in further 
   *              analysis
   */
  protected boolean isNoiseWord(String term)
  {
    int len = term.length();
    
    if( term.length() > 0 &&
        (term.charAt(0) == Constants.FIELD_START_MARKER ||
         term.charAt(term.length()-1) == Constants.FIELD_END_MARKER) )
    {
        return true;
    }
    
    if( minWordLen > 0 && len < minWordLen )
        return true;
    
    if( maxWordLen > 0 && len > maxWordLen )
        return true;

    if( stopSet != null && stopSet.contains(term) )
        return true;
    
    return false;
    
  } // isNoiseWord()
  

  /** Prints a user-readable version of this query. */
  public String toString(String field) {
    return "moreLikeThis(" + subQuery.toString(field) + ")";
  }

  /**
   * Used for frequencies and to avoid renewing Integers.
   */
  private static class Int 
  {
    public int x;

    public Int() {
        x = 1;
    }
  }
  
  /**
   * Used to keep track of which fields to scan, and how to boost them.
   */
  private static class FieldSpec 
  {
    public String name;
    public float  boost;
    
    public FieldSpec( String name, float boost ) {
      this.name  = name;
      this.boost = boost;
    }
  }
  
  private static class QueryWord
  {
    public Term  term;
    public float score;
    
    public QueryWord( Term term, float score ) {
      this.term  = term;
      this.score = score;
    }
  }
  
  /**
   * PriorityQueue that orders query words by score.
   */
  private static class QueryWordQueue extends PriorityQueue 
  {
    QueryWordQueue( int s ) {
        initialize(s);
    }

    protected boolean lessThan(Object a, Object b) {
        QueryWord aa = (QueryWord) a;
        QueryWord bb = (QueryWord) b;
        return aa.score < bb.score;
    }
  }

  /**
   * Exclude the target document from the set. Also, provide a more 
   * comprehensive score explanation.
   */
  public class MoreLikeWrapper extends Query 
  {
    MoreLikeThisQuery outerQuery;
    String            outerDescrip;
    
    Query             innerQuery;
    String            innerDescrip;

    public MoreLikeWrapper(MoreLikeThisQuery outerQuery, Query innerQuery) {
      this.outerQuery = outerQuery;
      this.innerQuery = innerQuery;
      innerDescrip = "weight(" + innerQuery.toString() + ")";
      outerDescrip = "weight(" + outerQuery.toString() + ")";
    }
  
    /**
     * Returns a Weight that applies the filter to the enclosed query's Weight.
     * This is accomplished by overriding the Scorer returned by the Weight.
     */
    public Weight createWeight (final Searcher searcher) {
      Weight x = null;
      try {
          x = innerQuery.weight(searcher);
      }
      catch( IOException e ) { throw new RuntimeException(e); }
      final Weight weight = x;
      return new Weight() {
  
        // pass these methods through to enclosed query's weight
        public float getValue() { return weight.getValue(); }
        public float sumOfSquaredWeights() throws IOException { return weight.sumOfSquaredWeights(); }
        public void normalize (float v) { weight.normalize(v); }
        public Explanation explain (IndexReader ir, int i) throws IOException {
          Explanation innerExpl = weight.explain (ir, i);
          Explanation wrapperExpl = new Explanation(innerExpl.getValue(), innerDescrip);
          wrapperExpl.addDetail(innerExpl);
          Explanation outerExpl = new Explanation(innerExpl.getValue(), outerDescrip);
          outerExpl.addDetail(wrapperExpl);
          return outerExpl;
        }
  
        // return this query
        public Query getQuery() { return MoreLikeWrapper.this; }
  
        // return a scorer that overrides the enclosed query's score if
        // the given hit has been filtered out.
        public Scorer scorer (IndexReader indexReader) throws IOException {
          final Scorer scorer = weight.scorer (indexReader);
          return new Scorer (innerQuery.getSimilarity (searcher)) {
  
            // pass these methods through to the enclosed scorer
            public boolean next() throws IOException { return scorer.next(); }
            public int doc() { return scorer.doc(); }
            public boolean skipTo (int i) throws IOException { return scorer.skipTo(i); }
  
            // if the document has been filtered out, set score to 0.0
            public float score() throws IOException {
              return (targetDoc != scorer.doc()) ? scorer.score() : 0.0f;
            }
  
            // add an explanation about whether the document was filtered
            public Explanation explain (int i) throws IOException {
              Explanation exp = scorer.explain (i);
              if (targetDoc != i)
                exp.setDescription ("allowed by filter: "+exp.getDescription());
              else
                exp.setDescription ("removed by filter: "+exp.getDescription());
              return exp;
            }
          };
        }
      };
    }
  
    public Query getQuery() {
      return innerQuery;
    }
  
    /** Prints a user-readable version of this query. */
    public String toString (String s) {
      return "excludeDoc("+targetDoc+","+innerQuery.toString(s)+")";
    }
  
    /** Returns true iff <code>o</code> is equal to this. */
    public boolean equals(Object o) {
      if (o instanceof MoreLikeWrapper) {
        MoreLikeWrapper fq = (MoreLikeWrapper) o;
        return (innerQuery.equals(fq.innerQuery));
      }
      return false;
    }
  
    /** Returns a hash code value for this object. */
    public int hashCode() {
      return innerQuery.hashCode();
    }
  } // class MoreLikeWrapper
} // class MoreLikeThisQuery
