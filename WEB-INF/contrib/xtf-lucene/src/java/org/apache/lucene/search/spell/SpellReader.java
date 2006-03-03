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

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
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

  /** Index searcher, querying the spelling index */
  private IndexSearcher searcher;

  /** Used for calculating double metaphone keys */
  private static DoubleMetaphone dmph = new DoubleMetaphone();
    
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
    if (docFreq > goalFreq && num_sug == 1)
        return new SuggestWord[0];

    // Form a query using ngrams of each length from the original word.
    BooleanQuery query=new BooleanQuery();
    String[] grams;
    String key;

    add(query, "trans", word, bTrans);
    add(query, "drop", word, bInsert);
    for (int i=0; i<word.length(); i++) {
        String dropped = word.substring(0, i) + 
                         word.substring(Math.min(word.length(), i+1));
        if( dropped.length() > 0 )
            add(query, "word", dropped, bDelete);
    }

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
    String wordDMPH = calcDMPH( word );

    // Make a queue of the best matches. Leave a little extra room for some
    // to hang off the end at the same score but different frequencies.
    //
    float min = accuracy;
    SuggestWord sugword = new SuggestWord();
    final SuggestWordQueue sugqueue = new SuggestWordQueue(num_sug+10);
    for( int i = 0; i < hits.scoreDocs.length; i++ )
    {
        // Get original word
        sugword.string = searcher.doc(hits.scoreDocs[i].doc).get(F_WORD); 

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
            if( sugword.freq > 2 )
                sugword.score += 0.10f;
            else if (sugword.freq > 1 )
                sugword.score += 0.05f;
        }
        
        // If the metaphone matches, nudge the score
        if( calcDMPH(sugword.string).equals(wordDMPH) )
            sugword.score += 0.10f;
        
        sugqueue.insert(sugword);
        
        // If queue full, maintain the min score
        if (sugqueue.size()==num_sug)
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
   * Calculate the double-metaphone for a word.
   */
  public String calcDMPH( String word )
  {
    String ret = dmph.doubleMetaphone( word );
    if( word.endsWith("s") && !ret.endsWith("S") )
        ret += "S";
    return ret;
  }


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
   * Form all ngrams for a given word.
   * @param text the word to parse
   * @param ng the ngram length e.g. 3
   * @return an array of all ngrams in the word and note that duplicates are not removed
   */
  private static String[] formGrams (String text, int ng) {
    int len=text.length();
    String[] res=new String[len-ng+1];
    for (int i=0; i<len-ng+1; i++) {
        res[i]=text.substring(i, i+ng);
    }
    return res;
  }
  
  
  /**
   * Check if the first word can be transformed into the second word by a 
   * simple transposition of two characters. This is a common form of typo.
   */
  private boolean isTranspose( String w1, String w2 ) {
    if (w1.length() != w2.length() || w1.equals(w2))
        return false;
    int i = 0;
    while (w1.charAt(i) == w2.charAt(i))
        i++;
    
    int j = w1.length() - 1;
    while (w1.charAt(j) == w2.charAt(j))
        j--;
    
    return (i+1 == j) && 
           (w1.charAt(i) == w2.charAt(j)) && 
           (w1.charAt(j) == w2.charAt(i));
  }

  
  private void openSearcher() throws IOException {
    if (searcher == null)
        searcher = new IndexSearcher(spellIndex);
  }

  
  protected void finalize () throws Throwable {
    close();
  }
  
} // class SpellReader
