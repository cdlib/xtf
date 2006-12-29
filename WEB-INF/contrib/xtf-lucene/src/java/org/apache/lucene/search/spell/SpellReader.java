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

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.cdlib.xtf.util.IntMultiMap;
import org.cdlib.xtf.util.StringList;
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
  
  /** Boost value for metaphones */
  private float bMetaphone=3.0f;

  /** Index reader for the spelling index */
  private IndexReader spellIndexReader;
  
  /** Index searcher, for querying the spelling index */
  private IndexSearcher searcher;
  
  /** Pair frequency data */
  private PairFreqWriter pairFreqs;

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
  
  private StringList  allWords  = null;
  private StringList  allMphs   = null;
  private IntMultiMap mphMap = null;

  /**
   * Make a mapping of metaphone to words.
   */
  private void loadMphMap() 
    throws IOException 
  {
    if (mphMap != null)
      return;
    
    long startTime = System.currentTimeMillis();
    
    // First, load all the metaphone strings.
    openSearcher();
    TermEnum terms = spellIndexReader.terms( new Term("metaphone", "") );
    allMphs = new StringList();
    while( terms.next() ) {
      Term t = terms.term();
      if (!t.field().equals("metaphone"))
        break;
      allMphs.add(t.text());
    }
    allMphs.sort();
    terms.close();
    
    // Now load all the words and associate thespellIndexReader metaphones
    allWords = new StringList(spellIndexReader.maxDoc());
    mphMap = new IntMultiMap(allMphs.size());
    for (int docId = 0; docId < spellIndexReader.maxDoc(); docId++) {
      Document d = spellIndexReader.document(docId);
      if (d == null)
        continue;
      
      String word = d.get(F_WORD);
      if (word == null)
        continue;
      
      int wordId = allWords.size();
      allWords.add(word);
      
      String mph = SpellWriter.calcMetaphone(word);
      if (mph.length() == 0)
        continue;
      
      int mphId = allMphs.binarySearch(mph);
      assert mphId >= 0 : "metaphone index is inconsistent";
      
      mphMap.add(mphId, wordId);
    }
    
    System.out.println("MphMap load time: " + (System.currentTimeMillis() - startTime));
    System.out.println("  ndocs: " + spellIndexReader.maxDoc());
    System.out.println("  nWords: " + allWords.size() + ", nMphs: " + allMphs.size());
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

    // To ensure that we've caught all the good candidates, scan ten times the
    // requested number of matches (and at least 100).
    //
    int nToScan = Math.max(100, 10*num_sug);
    String[] hits = queryWords(word, nToScan);
    
    // Calculate the main word's metaphone once.
    String metaphone = SpellWriter.calcMetaphone( word );
    
    // Make a queue of the best matches. Leave a little extra room for some
    // to hang off the end at the same score but different frequencies.
    //
    float min = 0;
    SuggestWord sugword = new SuggestWord();
    int queueSize = num_sug+10;
    final SuggestWordQueue sugqueue = new SuggestWordQueue(queueSize);
    for( int i = 0; i < hits.length; i++ )
    {
        // Get original word
        sugword.string = hits[i];
        
        // Don't suggest a word for itself, that would be silly.
        if (sugword.string.equals(word))
            continue;

        // Calculate the edit distance (and normalize with the min word length)
        float dist = sd.getDistance(sugword.string) / 2.0f;
        sugword.score = 1.0f-(dist/Math.min(sugword.string.length(), lengthWord));
        
        // Enforce the accuracy limit imposed on us.
        if( sugword.score < accuracy )
            continue;
        
        // If the metaphone matches, nudge the score
        String suggMetaphone = SpellWriter.calcMetaphone(sugword.string);
        if( suggMetaphone.equals(metaphone) )
            sugword.score += 0.10f;
        
        int metaDist = new TRStringDistance(metaphone).getDistance(suggMetaphone);
        if (metaDist > 3)
            continue;
        
        // Don't bother checking the frequency if it can't get us into the
        // queue.
        //
        if( sugword.score + 0.25f <= min )
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

        // If the term isn't in any of our fields, skip it.
        if (bestField == null)
            continue;
        
        // Make a suggestion.
        sugword.origScore = sugword.score;
        sugword.field     = bestField;
        sugword.freq      = bestFreq;
        sugword.score     += bestBoost;
        
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
    // No terms? Then we can't suggest anything.
    if (terms.length == 0)
        return terms;
    
    // Start with a null change.
    Change bestChange = new Change();
    bestChange.terms = terms;
    bestChange.fields = fields;
    bestChange.indexReader = indexReader;
    bestChange.descrip = "no change";
    bestChange.score = 0.0f;
    
    // If there's just one word, out work is simple: just find the best 
    // replacement for that word.
    //
    if (terms.length == 1)
        return subWord(bestChange, 0).terms;
    
    // Consider two-word changes at each position.
    for (int i = 0; i < terms.length-1; i++)
        bestChange = subPair(bestChange, i);
    
    return bestChange.terms;
    
  } // suggestKeywords()
  
  /**
   * Consider a set of changes to the pair of words at the given position.
   * 
   * @param in  the current best we've found
   * @param pos           position to consider
   * @return            new best
   */
  private Change subPair(Change in, int pos) 
    throws IOException
  {
    String word1 = in.terms[pos];
    String word2 = in.terms[pos+1];
    
    // As the base case, consider changing nothing.
    float basePairScore = scorePair(in, 0, word1, word2);
    
    // Get a list of independent suggestions for both words.
    final int NUM_SUG = 50;
    SuggestWord[] list1 = suggestSimilar(word1, NUM_SUG, in.indexReader, in.fields, 0.0f, 0.5f);
    SuggestWord[] list2 = suggestSimilar(word2, NUM_SUG, in.indexReader, in.fields, 0.0f, 0.5f);
    
    float baseWord1Score = scoreWord(word1, in.fields, in.indexReader, word1);
    float baseWord2Score = scoreWord(word2, in.fields, in.indexReader, word2);
    
    System.out.println("List 1:");
    for (int p1 = 0; p1 < list1.length; p1++)
        System.out.println("  " + list1[p1].string + " " + SpellWriter.calcMetaphone(list1[p1].string));
    System.out.println("List 2:");
    for (int p2 = 0; p2 < list2.length; p2++)
        System.out.println("  " + list2[p2].string + " " + SpellWriter.calcMetaphone(list2[p2].string));
    
    // Now score all possible combinations, looking for the best one.
    float bestScore = 0.0f;
    String bestSugg1 = null;
    String bestSugg2 = null;
    for (int p1 = 0; p1 < list1.length+1; p1++) {
        String sugg1 = (p1 < list1.length) ? list1[p1].string : word1;
        for (int p2 = 0; p2 < list2.length+1; p2++) {
            String sugg2 = (p2 < list2.length) ? list2[p2].string : word2;
            float pairScore = scorePair(in, pos, sugg1, sugg2);
            float word1Score = (p1 < list1.length) ? list1[p1].score : baseWord1Score;
            float word2Score = (p2 < list2.length) ? list2[p2].score : baseWord2Score;
            float totalScore = (word1Score - baseWord1Score) +
                               (word2Score - baseWord2Score) +
                               (pairScore - basePairScore);
            
            //System.out.println("replace '" + word1 + " " + word2 + "' with '" +
            //    sugg1 + " " + sugg2 + "': " + score);
            
            if (totalScore > bestScore) {
                bestScore = totalScore;
                bestSugg1 = sugg1;
                bestSugg2 = sugg2;
            }
        }
    }
    
    // If we found something better than doing nothing, record it.
    Change out = in;
    if (bestScore > in.score) {
        out = (Change) in.clone();
        if (bestSugg2.equals(word2))
            out.descrip = "replace '" + word1 + "' with '" + bestSugg1 + "'";
        else if (bestSugg1.equals(word1))
            out.descrip = "replace '" + word2 + "' with '" + bestSugg2 + "'";
        else {
            out.descrip = "replace '" + word1 + " " + word2 + "' with '" +
                bestSugg1 + " " + bestSugg2 + "'";
        }
        out.terms[pos] = bestSugg1;
        out.terms[pos+1] = bestSugg2;
        out.score = bestScore;
    }
    return out;
  }

  /** Pick the best of two possible changes, based on max score */
  private static Change max(Change ch1, Change ch2)
  {
    if (ch1.score >= ch2.score)
        return ch1;
    else
        return ch2;
  }

  /**
   * Substitute a single word at the given position, trying to improve the score.
   * 
   * @param in      the best we've done so far
   * @param i       position to substitute at
   * @return        the best we can do at that position
   */
  private Change subWord(Change in, int pos) throws IOException
  {
    String word = in.terms[pos];
    final float baseScore = scoreWord(word, in.fields, in.indexReader, word);

    // Make a queue of the best matches. Leave a little extra room for some
    // to hang off the end at the same score but different frequencies.
    //
    String[] hits = queryWords(word, 500);
    String bestSugg = word;
    float bestScore = baseScore;
    for( int i = 0; i < hits.length; i++ )
    {
        // No need to consider the original word twice.
        String suggWord = hits[i];
        if (suggWord.equals(word))
            continue;

        // Calculate a score for it. If better than what we had, save it.
        float score = scoreWord(word, in.fields, in.indexReader, suggWord);
        if (score > bestScore) {
            bestSugg = suggWord;
            bestScore = score;
        }
    }
    
    // If we found a better suggestion, record it.
    Change out = in;
    if (bestScore > baseScore) {
        out = (Change) in.clone();
        out.descrip = "replace '" + out.terms[pos] + "' with '" + bestSugg + "'";
        out.terms[pos] = bestSugg;
        out.score = (bestScore - baseScore);
    }
    return out;
  }

  /**
   * Form a Lucene query based on the input word, looking for words with
   * similar sets of letters.
   * 
   * @param word        the word we're looking to resemble
   * @param nToScan     max number of potential words to return
   * @return            array of potential words
   */
  private String[] queryWords(String word, int nToScan) throws IOException
  {
    // Form a query using ngrams of each length from the original word.
    BooleanQuery query=new BooleanQuery();
    String[] grams;
    String key;

    addTrans(query, word, bTrans);
    add(query, "metaphone", SpellWriter.calcMetaphone(word), bMetaphone);

    for (int ng=SpellWriter.getMin(word.length()); 
         ng<=SpellWriter.getMax(word.length()); 
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
    TopDocs hits = searcher.search( query, null, nToScan );
    
    // Translate the hits into actual words.
    String[] out = new String[hits.scoreDocs.length];
    for (int i=0; i<hits.scoreDocs.length; i++)
        out[i] = searcher.doc(hits.scoreDocs[i].doc).get(F_WORD);
    return out;
  }

  /**
   * Calculate a score for a suggested replacement for a given word.
   */
  private float scorePair(Change orig, int pos, String sugg1, String sugg2)
    throws IOException
  {
    openPairFreqs();
    float bestFreqBoost = 0.0f;

    for (int i = 0; i < orig.fields.length; i++) {
        int origPairFreq = pairFreqs.get(orig.fields[i], orig.terms[pos], orig.terms[pos+1]);
        int suggPairFreq = pairFreqs.get(orig.fields[i], sugg1, sugg2);
        if (suggPairFreq <= origPairFreq)
            continue;
        double freqFactor = (suggPairFreq + 1.0) / (origPairFreq + 1.0);
        float freqBoost = (float) (Math.log(freqFactor) / Math.log(100.0));
        bestFreqBoost = Math.max(freqBoost, bestFreqBoost);
    }

    return bestFreqBoost;
  }
  
  /**
   * Calculate a score for a suggested replacement for a given word.
   */
  private float scoreWord(String origTerm, String[] fields, 
                          IndexReader ir, String suggTerm) 
    throws IOException
  {
    // Calculate the edit distance (and normalize with the min word length)
    TRStringDistance sd = new TRStringDistance(origTerm);
    float dist = sd.getDistance(suggTerm) / 2.0f;
    float score = 1.0f-(dist/Math.min(suggTerm.length(), origTerm.length()));
    
    // Find the field with the best score boost...
    float freqBoost = 0;
    for (int fn=0; fn<fields.length; fn++)
    {
        // Get the word frequency from the index
        int freq = ir.docFreq(new Term(fields[fn], suggTerm));

        // Don't suggest a word that is not present in the field
        if (freq<1)  
            continue;
        
        // If this word is more frequent than average, give it a nudge up.
        float boost = 0.0f;
        int[] termFreqs = getTermFreqs(ir, fields[fn]);
        if( freq >= termFreqs[0] ) // 1000
            boost = 0.25f;
        else if( freq >= termFreqs[1] ) // 40
            boost = 0.20f;
        else if( freq >= termFreqs[2] ) // 4
            boost = 0.15f;
        else if( freq >= termFreqs[3] ) // 2
            boost = 0.10f;
        else if (freq >= termFreqs[4] ) // 1
            boost = 0.05f;
        
        if (boost > freqBoost)
            freqBoost = boost;
    } // for fn
    
    // If the metaphone matches, nudge the score
    float metaphoneBoost = 0.0f;
    if( SpellWriter.calcMetaphone(origTerm).equals(
             SpellWriter.calcMetaphone(suggTerm)) )
        metaphoneBoost = 0.10f;
    
    // All done.
    return score + freqBoost + metaphoneBoost;
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
    if (searcher == null) {
        spellIndexReader = IndexReader.open(FSDirectory.getDirectory(spellDir, false));
        searcher = new IndexSearcher(spellIndexReader);
    }
  }

  private void openPairFreqs() throws IOException {
    if (pairFreqs == null) {
        pairFreqs = new PairFreqWriter();
        pairFreqs.add(new File(spellDir, "pairs.dat"));
    }
  }
  
  protected void finalize () throws Throwable {
    close();
  }
  
  private class Change implements Cloneable
  {
    String[]    terms;
    String[]    fields;
    IndexReader indexReader;
    String      descrip;
    float       score;

    public Object clone() { 
      try {
        Change out = (Change) super.clone();
        out.terms = new String[terms.length];
        System.arraycopy(terms, 0, out.terms, 0, terms.length);
        return out;
      }
      catch (CloneNotSupportedException e) {
        return null;
      } 
    }
  }
  
} // class SpellReader
