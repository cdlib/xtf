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
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.cdlib.xtf.util.StructuredFile;
import org.cdlib.xtf.util.SubStoreReader;

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
  File spellDir;

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
  public SpellReader (File spellIndexDir) {
    this.spellDir = spellIndexDir;
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
    SuggestWord[] list = suggestSimilar(word, num_sug, ir, new String[] { field }, 
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
    return suggestSimilar(word, num_sug, ir, new String[] { field }, 
                          morePopularFactor, accuracy);
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
                                       final String[] fields,
                                       float morePopularFactor, 
                                       final float accuracy) 
      throws IOException 
  {
    final TRStringDistance sd = new TRStringDistance(word);
    final int lengthWord = word.length();

    // Determine the frequency above which the suggestions must be. Also,
    // get data on which terms are rare and which are common.
    //
    int docFreq[]     = null;
    int goalFreq[]    = null;
    int termFreqs[][] = null;
    
    if (ir != null) {
        docFreq = new int[fields.length];
        goalFreq = new int[fields.length];
        termFreqs = new int[fields.length][];
        for (int i=0; i<fields.length; i++) {
            docFreq[i] = ir.docFreq(new Term(fields[i], word));
            goalFreq[i] = (int)(morePopularFactor * docFreq[i]);
            termFreqs[i] = getTermFreqs(ir, fields[i]);
        }
    }

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
        
        // Find the field with the best score boost...
        int    bestFreq  = 0;
        float  bestBoost = -1;
        String bestField = null;
        
        for (int fn=0; fn<fields.length; fn++)
        {
            // Get the word frequency from the index
            int freq = ir.docFreq(new Term(fields[fn], sugword.string));

            // Don't suggest a word that is not present in the field
            if ((goalFreq[fn]>freq) || freq<1)  
                continue;
            
            // If this word is more frequent than normal, give it a nudge up.
            float boost = 0.0f;
            if( freq >= termFreqs[fn][0] ) // 1000
                boost = 0.25f;
            else if( freq >= termFreqs[fn][1] ) // 40
                boost = 0.20f;
            else if( freq >= termFreqs[fn][2] ) // 4
                boost = 0.15f;
            else if( freq >= termFreqs[fn][3] ) // 2
                boost = 0.10f;
            else if (freq >= termFreqs[fn][4] ) // 1
                boost = 0.05f;
            
            if (freq > bestFreq) {
                bestField = fields[fn];
                bestBoost = boost;
                bestFreq  = freq;
            }
        } // for fn
        
        if (bestField == null)
            continue;
        
        sugword.origScore = sugword.score;
        sugword.field     = bestField;
        sugword.freq      = bestFreq;
        sugword.score     += bestBoost;
        
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

  /**
   * Keyword-oriented spelling suggestion mechanism. For an ordered list of
   * terms which can appear in any of the specified fields, come up with
   * suggestions that have a good chance of improving  the precision and/or
   * recall.
   * 
   * @param terms           Ordered list of query terms
   * @param fields          Unordered list of fields they can appear in
   * @param indexReader     Used to obtain term frequencies
   * @return                One suggestion per term. If unchanged, there
   *                        was no better suggestion. If null, it is
   *                        suggested that the term be deleted.
   */
  public String[] suggestKeywords(String[] terms, String[] fields, IndexReader indexReader) 
    throws IOException
  {
    String[] out = new String[terms.length];
    
    // For now, just make a simple suggestion for each term. Later we'll do 
    // fancy things with pair frequencies.
    //
    for( int i = 0; i < terms.length; i++ ) {
        SuggestWord[] sugg = suggestSimilar( terms[i], 1,
                                             indexReader,
                                             fields,
                                             1.0f, 0.5f );
        if( sugg == null || sugg.length == 0 )
            out[i] = terms[i];
        else
            out[i] = sugg[0].string;
    }
    
    return out;
  } // suggestKeywords()
  
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

    // Default if no frequencies found will be to turn off frequency boosting
    res = new int[5];
    res[0] = res[1] = res[2] = res[3] = res[4] = Integer.MAX_VALUE;

    // Store the new array in the cache, so we don't have to build it again.
    readerCache.put(fieldName, res);

    // Find the frequency samples file and open it
    File freqSamplesFile = new File(spellDir, "freqSamples.dat");
    if (!freqSamplesFile.canRead())
        throw new IOException("Cannot open frequency samples file '" + freqSamplesFile + "'");
    
    StructuredFile sf = StructuredFile.open(freqSamplesFile);
    int nSamples;
    int[] samples;
    try {
      // Find the subfile with frequency samples of this field
      SubStoreReader sub = null;
      try {
        sub = sf.openSubStore(fieldName + ".samples");
        
        // If there were less than 500 terms to sample, turn off frequency
        // boosting for this field.
        //
        int nTerms = sub.readInt();
        if (nTerms < 500)
          return res;
        
        // Read in the samples.
        nSamples = sub.readInt();
        samples = new int[nSamples];
        for (int i=0; i<nSamples; i++)
          samples[i] = sub.readInt();
      }
      catch (IOException e) {
        return res;
      }
      finally {
        if (sub != null)
          sub.close();
      }
    }
    finally {
      sf.close();
    }
    
    // Pick out the levels of most interest to us
    res[0] = samples[(int) (nSamples * 0.99)]; // top 1%
    res[1] = samples[(int) (nSamples * 0.90)]; // top 10%
    res[2] = samples[(int) (nSamples * 0.50)]; // top 50%
    res[3] = samples[(int) (nSamples * 0.25)]; // top 75%
    res[4] = samples[0];                     // all above-avg words
      
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
        searcher = new IndexSearcher(FSDirectory.getDirectory(spellDir, false));
  }

  
  protected void finalize () throws Throwable {
    close();
  }
  
} // class SpellReader
