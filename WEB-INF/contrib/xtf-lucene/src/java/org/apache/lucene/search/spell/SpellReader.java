package org.apache.lucene.search.spell;

/**
 * Copyright 2002-2006 The Apache Software Foundation
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;

/**
 * <p>
 * Spell Reader class <br/>(built on Nicolas Maisonneuve / David Spencer code).
 * Provides efficient, high-volume suggestions from a spelling correction 
 * index.
 * </p>
 * 
 * @author Martin Haye
 * @version 1.0
 */
public class SpellReader 
{
  /** Field name for each word in the ngram index. */
  public static final String F_WORD="word";

  /** The spell index directory */
  Directory spellIndex;

  /** Boost value for start gram */
  private float bStart=2.0f;
  
  /** Boost value for end gram */
  private float bEnd=1.0f;
  
  /** Boost value for simple transposition */
  private float bTrans=6.0f;
  
  /** Boost value for single-char insertion */
  private float bInsert=2.0f;
  
  /** Boost value for single-char deletion */
  private float bDelete=2.0f;
  
  /** Boost value for metaphones */
  private float bMetaphone=3.0f;

  /** Index searcher, for querying the spelling index */
  private IndexSearcher searcher;

  /** Cache of term frequencies per field */
  private static final WeakHashMap termFreqCache = new WeakHashMap();

  /** Construct a reader for the given spelling index directory */
  public SpellReader (Directory spellIndex) {
    this.spellIndex = spellIndex;
  }

  /** Closes any open files and/or resources associated with the SpellReader */
  public void close() throws IOException
  {
    if (searcher != null)
        searcher.close();
    searcher = null;
  }
  
  public boolean inDictionary (String word) throws IOException {
    openSearcher();
    int freq = searcher.docFreq(new Term(F_WORD, word));
    return freq > 0;
  }
      
  /**
   * Suggest similar words
   * 
   * @param word      the word you want a spell check done on
   * @param num_sug   the max number of words to suggest
   * 
   * @throws IOException  if something goes wrong during the process
   * @return String[]     the sorted list of the suggest words, ordered by
   *                      edit distance.
   */
  public String[] suggestSimilar (String word, int num_sug) throws IOException {
      return suggestSimilar(word, num_sug, null, null, false);
  }


  /**
   * Suggest similar words (restricted or not to a field of a user index)
   * 
   * @param word          the word you want a spell check done on
   * @param num_sug       max number of words to suggest
   * @param ir            the indexReader of the user index (can be null; see field param)
   * @param field         the field of the user index: if field is not null, the 
   *                      suggested words are restricted to the words present in 
   *                      this field.
   * @param morePopular   if true, return only the suggest words that are more 
   *                      frequent in the user index than the searched word
   *                      (only if restricted mode (ir!=null and field!=null))
   *                      
   * @throws IOException  if something goes wrong during the process
   * @return String[]     the sorted list of the suggest words with these
   *                      criteria: first, the edit distance; second: (only if 
   *                      restricted mode): the popularity of the suggest word 
   *                      in the field of the user index
   */
  public String[] suggestSimilar (String word, int num_sug, 
                                  IndexReader ir, String field,
                                  boolean morePopular) 
      throws IOException 
  {
    SuggestWord[] list = suggestSimilar(word, num_sug, ir, field, 
                               morePopular ? 1 : 0, 0.5f);
    
    // Throw away other data and just return list of strings
    String[] ret = new String[list.length];
    for (int i=0; i<list.length; i++)
        ret[i] = list[i].string;
    return ret;
  }

  /**
   * Suggest similar words (restricted or not to a field of a user index)
   * 
   * @param word          the word you want a spell check done on
   * @param num_sug       max number of words to suggest
   * @param ir            the indexReader of the user index (can be null; see field param)
   * @param field         the field of the user index: if field is not null, the 
   *                      suggested words are restricted to the words present in 
   *                      this field.
   * @param morePopularFactor  return only the suggest words that are more 
   *                           frequent by the given factor than the searched 
   *                           word (only if restricted mode = 
   *                           (indexReader!=null and field!=null))
   * @param accuracy      minimum accuracy of returned words (0.5f is good)
   * 
   * @throws IOException  if something goes wrong during the process
   * @return String[]     the sorted list of the suggest words with these
   *                      criteria: first, the edit distance; second: (only if 
   *                      restricted mode): the popularity of the suggest word 
   *                      in the field of the user index
   */
  public SuggestWord[] suggestSimilar (final String word, 
                                       final int num_sug, 
                                       final IndexReader ir, 
                                       final String field,
                                       float morePopularFactor, 
                                       final float accuracy) 
      throws IOException 
  {
    final TRStringDistance sd = new TRStringDistance(word);
    final int lengthWord = word.length();

    // Determine the frequency above which the suggestions must be
    final int docFreq  = (ir!=null) ? ir.docFreq(new Term(field, word)) : 0;
    final int goalFreq = (int)(morePopularFactor * docFreq);

    // If the word exists in the index and we're not concerned about finding
    // a more popular one (or more than one), then we have nothing to suggest.
    //
    //if (docFreq > goalFreq && num_sug == 1)
    //    return new SuggestWord[0];

    // Form a query using ngrams of each length from the original word.
    BooleanQuery query=new BooleanQuery();
    String[] grams;
    String key;

    addTrans(query, word, bTrans);
    add(query, "metaphone", SpellWriter.calcMetaphone(word), bMetaphone);

    for (int ng=SpellWriter.getMin(lengthWord); 
         ng<=SpellWriter.getMax(lengthWord); 
         ng++) 
    {
        key="gram"+ng; // form key

        grams=formGrams(word, ng); // form word into ngrams (allow dups too)

        if (grams.length==0)
            continue; // hmm

        if (bStart>0)
            add(query, "start"+ng, grams[0], bStart); // matches start of word
        if (bEnd>0)
            add(query, "end"+ng, grams[grams.length-1], bEnd); // matches end of word
        for (int i=0; i<grams.length; i++)
            add(query, key, grams[i]);
    }
    
    // Using the query built above, go look for matches.
    openSearcher();
    
    // To ensure that we've caught all the good candidates, scan ten times the
    // requested number of matches (and at least 100).
    //
    int nToScan = Math.max(100, 10*num_sug);
    TopDocs hits = searcher.search( query, null, nToScan );
    
    // Calculate the main word's metaphone once.
    String metaphone = SpellWriter.calcMetaphone( word );
    
    // If we're checking index frequencies, get data on which terms are rare
    // and which are common.
    //
    int[] termFreqs = null;
    if (ir != null && field != null)
        termFreqs = getTermFreqs(ir, field);

    // Make a queue of the best matches. Leave a little extra room for some
    // to hang off the end at the same score but different frequencies.
    //
    float min = accuracy;
    SuggestWord sugword = new SuggestWord();
    int queueSize = num_sug+10;
    final SuggestWordQueue sugqueue = new SuggestWordQueue(queueSize);
    for( int i = 0; i < hits.scoreDocs.length; i++ )
    {
        // Get original word
        sugword.string = searcher.doc(hits.scoreDocs[i].doc).get(F_WORD);
        
        //System.out.println( "L: " + sugword.string + ", " + hits.scoreDocs[i].score );

        // Don't suggest a word for itself, that would be silly.
        if (sugword.string.equals(word))
            continue;

        // Calculate the edit distance (and normalize with the min word length)
        float dist = sd.getDistance(sugword.string) / 2.0f;
        sugword.score = 1.0f-(dist/Math.min(sugword.string.length(), lengthWord));
        
        // Enforce the accuracy limit imposed on us.
        if( sugword.score < accuracy )
            continue;
        
        // If a user index was supplied...
        if (ir!=null) 
        {
            // Get the word frequency from the index
            sugword.freq = ir.docFreq(new Term(field, sugword.string)); 
            
            // Don't suggest a word that is not present in the field
            if ((goalFreq>sugword.freq)||sugword.freq<1)  
                continue;
            
            // If this word is more frequent than normal, give it a nudge up.
            sugword.origScore = sugword.score;
            if( sugword.freq >= termFreqs[0] ) // 1000
                sugword.score += 0.25;
            else if( sugword.freq >= termFreqs[1] ) // 40
                sugword.score += 0.20;
            else if( sugword.freq >= termFreqs[2] ) // 4
                sugword.score += 0.15;
            else if( sugword.freq >= termFreqs[3] ) // 2
                sugword.score += 0.10f;
            else if (sugword.freq >= termFreqs[4] ) // 1
                sugword.score += 0.05f;
        }
        
        // If the metaphone matches, nudge the score
        if( SpellWriter.calcMetaphone(sugword.string).equals(metaphone) )
            sugword.score += 0.10f;
        
        sugqueue.insert(sugword);
        
        // If queue full, maintain the min score
        if (sugqueue.size()==queueSize)
            min=((SuggestWord) sugqueue.top()).score;
        
        // Prepare for next go-round.
        sugword=new SuggestWord();
    }
    
    // Pop everything out of the queue and convert to an array
    SuggestWord[] list = new SuggestWord[Math.min(num_sug, sugqueue.size())];
    for (int i=sugqueue.size()-1; i>=0; i--) {
        SuggestWord sugg = (SuggestWord) sugqueue.pop();
        if (i < list.length)
            list[i] = sugg;
    }
    return list;
  }
  
  /** Get the term frequencies array for the given field. */
  private int[] getTermFreqs(IndexReader reader, String fieldName)
      throws IOException 
  {
    // Check if we have a cache for this reader yet. If not, make one.
    Map readerCache = (Map) termFreqCache.get(reader);
    if (readerCache == null) {
      readerCache = new HashMap();
      termFreqCache.put(reader, readerCache);
    }

    // Now check if we have a frequencies array already for this field.
    fieldName = fieldName.intern();
    int[] res = (int[]) readerCache.get(fieldName);
    if (res != null)
      return res;

    // Calculate the mean of the term frequencies
    long totalFreq = 0;
    int nTerms = 0;
    TermEnum terms = reader.terms( new Term(fieldName, "") );
    while( terms.next() ) {
        totalFreq += terms.docFreq();
        ++nTerms;
    }
    terms.close();
    double avgFreq = totalFreq / (double)nTerms;
    
    /*
    // Now calculate the standard deviation
    double variation = 0;
    terms = reader.terms( new Term(fieldName, "") );
    while( terms.next() ) {
        double diff = terms.docFreq() - avgFreq;
        variation += diff*diff;
        ++nTerms;
    }
    terms.close();
    
    double stddev = Math.sqrt(variation / (nTerms+1));
    
    // If not enough terms, turn off frequency boosting.
    res = new int[5];
    if( nTerms < 500 )
        res[0] = res[1] = res[2] = res[3] = res[4] = res[5] = Integer.MAX_VALUE;
    else
    {
        // Sort the frequencies, and pick out the levels of interest to us.
        res[0] = (int) (avgFreq + (stddev *22.077520));
        res[1] = (int) (avgFreq + (stddev * 0.848076));
        res[2] = (int) (avgFreq + (stddev * 0.051972));
        res[3] = (int) (avgFreq + (stddev * 0.007800));
        res[4] = (int) (avgFreq);
    }
    */
    
    // Okay, we have to build a new one. Sample at least 10000 above-average
    // terms (if there are that many.)
    //
    final int BUF_SIZE = 20000;
    int[] buffer = new int[BUF_SIZE];
    int termsPerSlot = 1;
    int pos = 0;
    int cycle = 0;
    int cutoff = (int) avgFreq;
    terms = reader.terms( new Term(fieldName, "") );
    while( terms.next() ) 
    {
        int freq = terms.docFreq();
        if( freq <= cutoff )
            continue;
        
        if( ++cycle == termsPerSlot ) 
        {
            cycle = 0;
            buffer[pos++] = freq;
            
            // If the buffer is full, toss half the data and increase the 
            // terms per slot.
            //
            if( pos == BUF_SIZE ) {
                for( int i = 0; i < pos/2; i++ )
                    buffer[i] = buffer[i*2];
                termsPerSlot *= 2;
                pos /= 2;
            }
        }
        ++nTerms;
    }
    
    res = new int[5];
    
    // If not enough terms, turn off frequency boosting.
    if( nTerms < 500 )
        res[0] = res[1] = res[2] = res[3] = res[4] = Integer.MAX_VALUE;
    else
    {
        // Sort the frequencies, and pick out the levels of interest to us.
        Arrays.sort( buffer, 0, pos );
        res[0] = buffer[(int) (pos * 0.99)]; // top 1%
        res[1] = buffer[(int) (pos * 0.90)]; // top 10%
        res[2] = buffer[(int) (pos * 0.50)]; // top 50%
        res[3] = buffer[(int) (pos * 0.25)]; // top 75%
        res[4] = cutoff;                     // all above-avg words
    }
      
    // Store the new array in the cache, so we don't have to build it again.
    readerCache.put(fieldName, res);
    
    // All done.
    return res;

  } // getTermFreqs()


  /**
   * Add a clause to a boolean query.
   */
  private static void add (BooleanQuery q, String k, String v, float boost) {
    Query tq=new TermQuery(new Term(k, v));
    tq.setBoost(boost);
    q.add(new BooleanClause(tq, false, false));
  }


  /**
   * Add a clause to a boolean query.
   */
  private static void add (BooleanQuery q, String k, String v) {
    q.add(new BooleanClause(new TermQuery(new Term(k, v)), false, false));
  }


  /**
   * Add clauses for simple transpositions of the source word.
   */
  private static void addTrans (BooleanQuery q, String text, float boost) {
      final int len=text.length();
      final char[] ch = text.toCharArray();

      char tmp;
      for (int i=0; i<len-1; i++) {
          tmp = ch[i];
          ch[i] = ch[i+1];
          ch[i+1] = tmp;
          
          add(q, F_WORD, new String(ch), boost); 
          
          tmp = ch[i];
          ch[i] = ch[i+1];
          ch[i+1] = tmp;
      }
  }
  /**
   * Form all ngrams for a given word.
   * @param text the word to parse
   * @param ng the ngram length e.g. 3
   * @return an array of all ngrams in the word and note that duplicates are not removed
   */
  private static String[] formGrams (String text, int ng) {
    text = SpellWriter.removeDoubles(text);
    int len=text.length();
    String[] res=new String[len-ng+1];
    for (int i=0; i<len-ng+1; i++) {
        res[i]=text.substring(i, i+ng);
    }
    return res;
  }

  
  private void openSearcher() throws IOException {
    if (searcher == null)
        searcher = new IndexSearcher(spellIndex);
  }

  
  protected void finalize () throws Throwable {
    close();
  }
  
} // class SpellReader
