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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.regex.Pattern;

import org.apache.lucene.util.PriorityQueue;
import org.cdlib.xtf.util.Hash64;
import org.cdlib.xtf.util.IntList;
import org.cdlib.xtf.util.LongSet;
import org.cdlib.xtf.util.StringList;
import org.cdlib.xtf.util.StringUtil;

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
  /** The spell index directory */
  File spellDir;
  
  /** Keys in the edit map file */
  private IntList edMapKeys;
  
  /** Positions in the edit map file */
  private IntList edMapPosns;
  
  /** File for reading edit map entries */
  private RandomAccessFile edMapFile;
  
  /** Charset decoder for reading edit map entries */
  private CharsetDecoder edMapDecoder;
  
  /** Pair frequency data */
  private PairFreqData pairFreqs;
  
  /** Frequencies from the term data, sampled at 5 levels */
  private int[] freqSamples;

  /** Where to send debugging info (or null for none) */
  private PrintWriter debugWriter = null;

  /** Pattern used for splitting up lines delimited by bars */
  private final Pattern splitPat = Pattern.compile("\\||\n");
  
  /** Protected constructor -- use {@link #open(File)} instead. */
  protected SpellReader() { }
  
  /** Check if there's a valid dictionary in the given directory */
  public static boolean isValidDictionary(File spellDir)
  {
    if (!spellDir.isDirectory() || !spellDir.canRead())
      return false;
    File file = new File(spellDir, "pairs.dat");
    return file.canRead();
  }

  /** Open a reader for the given spelling index directory. */
  public static SpellReader open(File spellIndexDir) 
    throws IOException 
  {
    SpellReader reader = new SpellReader();
    reader.spellDir = spellIndexDir;
    reader.openEdmap();
    reader.loadFreqSamples();
    return reader;
  }

  /** Read the index for the edit map file */
  private void openEdmap() throws IOException
  {
    long startTime = System.currentTimeMillis();
    File file = new File(spellDir, "edmap.dat");

    try
    {
      // First, open the map file. At the end, we'll find the position of the index.
      FileInputStream in = new FileInputStream(file);
      in.skip(file.length() - 20);
      BufferedReader reader = new BufferedReader(new InputStreamReader(in));
      String line = reader.readLine();
      int indexPos = Integer.parseInt(line.trim());
      
      // Now re-open and read the index.
      reader.close();
      in = new FileInputStream(file);
      in.skip(indexPos);
      reader = new BufferedReader(new InputStreamReader(in));
    
      // Check that we're really looking at a valid index
      line = reader.readLine();
      if (!line.equals("edMap index"))
        throw new IOException("edmap file corrupt");
      
      // Find out how many keys there are and allocate our lists.
      line = reader.readLine();
      int nKeys = Integer.parseInt(line);
      edMapKeys = new IntList(nKeys);
      edMapPosns = new IntList(nKeys+1);
  
      // And read each key/size line
      int prevKey = 0;
      int pos = 0;
      for (int i=0; i<nKeys; i++) 
      {
        line = reader.readLine();
        String[] tokens = splitPat.split(line);
        if (tokens.length != 2)
          throw new IOException("edmap file corrupt");
        if (tokens[0].length() != 4)
          throw new IOException("edmap file corrupt");
        
        int key = comboKey(tokens[0], 0, 1, 2, 3);
        assert key >= prevKey : "edmap file out of order or corrupt";
        prevKey = key;
        
        edMapKeys.add(key);
        
        int size = Integer.parseInt(tokens[1]);
        edMapPosns.add(pos);
        pos += size;
      }
      
      reader.close();
      
      if (edMapKeys.size() != nKeys)
        throw new IOException("edmap file index truncated");
      
      // Make one extra position entry, and record the index start (as it's 
      // the end of the last key entry)
      //
      edMapPosns.add(indexPos);
    }
    catch(NumberFormatException e) {
      throw new IOException("edmap file corrupt");
    }
    
    // Make a charset decoder that will be used to decode the UTF-8 data
    edMapDecoder = Charset.forName("UTF-8").newDecoder();
    
    // Finally, open a random-access version of the file for the actual
    // spellcheck process.
    //
    edMapFile = new RandomAccessFile(file, "r");
    
    // Print stats
    if (debugWriter != null) {
      debugWriter.println("EdMap index load time: " + (System.currentTimeMillis() - startTime));
      debugWriter.println("  nKeys: " + edMapKeys.size());
    }
  }

  /** Closes any open files and/or resources associated with the SpellReader */
  public void close() throws IOException
  {
    edMapFile.close();
  }
  
  /** Establishes a destination for detailed debugging output */
  public void setDebugWriter(PrintWriter w) {
    debugWriter = w;
  }
  
  /**
   * Read the list of edit-map words for the given 4-character key.
   *
   * @param orig the original word being considered
   * @param key the 4-char key to look up
   * @param minFreq minimum frequency of words to be queued
   * @param checked set of words that have already been considered
   * @param queue receives the resulting words
   * @return true iff the key was found
   */
  private boolean readEdKey(Word orig, int key, int minFreq,
                            LongSet checked, WordQueue queue) 
    throws IOException
  {
    // Look up this key in our index.
    int idxNum = edMapKeys.binarySearch(key);
    if (idxNum < 0)
      return false;
    
    // Read in the corresponding chunk of data
    int startPos = edMapPosns.get(idxNum);
    int endPos   = edMapPosns.get(idxNum+1);
    byte[] bytes = new byte[endPos-startPos];
    edMapFile.seek(startPos);
    if (edMapFile.read(bytes) != bytes.length)
      throw new IOException("error reading from edMap file");
    
    // Decode the string data from UTF-8
    String line = edMapDecoder.decode(ByteBuffer.wrap(bytes)).toString().trim();
    
    // Break up all the tokens.
    String[] tokens = splitPat.split(line);
    if (tokens.length < 3 || ((tokens.length-1) % 2) != 0)
      throw new IOException("edmap file corrupt");
    
    // Make sure we got the right key!
    if (key != comboKey(tokens[0], 0, 1, 2, 3))
      throw new IOException("edmap index incorrect");

    // Record each word in the list (and their frequencies)
    String prev = null;
    for (int j=1; j<tokens.length; j+=2) {
      String word = tokens[j];
      int freq;
      try { freq = Integer.parseInt(tokens[j+1]); }
      catch (NumberFormatException e) { throw new IOException("edmap file corrupt"); }
      
      // Handle prefix compression
      if (prev != null) {
        int overlap = word.charAt(0) - '0';
        word = prev.substring(0, overlap) + word.substring(1);
      }
      prev = word;
      
      // Don't consider any word twice.
      long hash = Hash64.hash(word);
      if (checked.contains(hash))
        continue;
      checked.add(hash);
      
      // If the frequency is too low, skip it.
      if (freq < minFreq)
        continue;
      
      // Eliminate suggestions that are too distant from the original. In
      // testing, this has the effect of increasing accuracy for the #1
      // spot, and in general getting rid of many "ridiculous" suggestions,
      // but it does eliminate certain distant suggestions way down the
      // list.
      //
      if (orig.wordDist(word) > 4)
        continue;
      
      // Add the new word to the queue.
      Word w = new Word(orig, word, freq);
      queue.insert(w);
    }
    
    // All done.
    return true;
  }
  
  /** 
   * Find words "close" to the given one, and add them to a queue.
   * In this case, "close" means that the first six characters have an
   * edit distance of 2 or less. Well, it means approximately that
   * anyway.
   * 
   * More precisely, we iterate all possible 4-letter keys that can be
   * constructed by deleting two of the first six characters in the
   * word. For each key, we add all words that share it.
   */
  private void findCloseWords(Word orig, int minFreq, WordQueue queue) 
    throws IOException
  {
    LongSet checked = new LongSet(100);
    readEdKey(orig, comboKey(orig.word,  0, 1, 2, 3), minFreq, checked, queue);
    readEdKey(orig, comboKey(orig.word,  0, 1, 2, 4), minFreq, checked, queue);
    readEdKey(orig, comboKey(orig.word,  0, 1, 2, 5), minFreq, checked, queue);
    readEdKey(orig, comboKey(orig.word,  0, 1, 3, 4), minFreq, checked, queue);
    readEdKey(orig, comboKey(orig.word,  0, 1, 3, 5), minFreq, checked, queue);
    readEdKey(orig, comboKey(orig.word,  0, 1, 4, 5), minFreq, checked, queue);
    readEdKey(orig, comboKey(orig.word,  0, 2, 3, 4), minFreq, checked, queue);
    readEdKey(orig, comboKey(orig.word,  0, 2, 3, 5), minFreq, checked, queue);
    readEdKey(orig, comboKey(orig.word,  0, 2, 4, 5), minFreq, checked, queue);
    readEdKey(orig, comboKey(orig.word,  0, 3, 4, 5), minFreq, checked, queue);     
    readEdKey(orig, comboKey(orig.word,  1, 2, 3, 4), minFreq, checked, queue);
    readEdKey(orig, comboKey(orig.word,  1, 2, 3, 5), minFreq, checked, queue);
    readEdKey(orig, comboKey(orig.word,  1, 2, 4, 5), minFreq, checked, queue);
    readEdKey(orig, comboKey(orig.word,  1, 3, 4, 5), minFreq, checked, queue);
    readEdKey(orig, comboKey(orig.word,  2, 3, 4, 5), minFreq, checked, queue);
  }

  /** 
   * Calculate a four letter key for the given word, by sticking together
   * characters from the given positions.
   */
  private int comboKey(String word, int p0, int p1, int p2, int p3)
  {
    int[] ch = new int[4];
    ch[0] = word.length() > p0 ? comboChar(word.charAt(p0)) : ' ';
    ch[1] = word.length() > p1 ? comboChar(word.charAt(p1)) : ' ';
    ch[2] = word.length() > p2 ? comboChar(word.charAt(p2)) : ' ';
    ch[3] = word.length() > p3 ? comboChar(word.charAt(p3)) : ' ';

    return (ch[0] << 24) |
           (ch[1] << 16) |
           (ch[2] << 8) |
           (ch[3] << 0);
  }
  
  private int comboChar(int c) {
    if (c >= 0x20 && (c & ~0x7f) == 0)
      return c;
    c = (char) ((c & 0x7f) | 0x20);
    return (c == '|') ? '*' : c;
  }
  
  /** Check if the given word is in the spelling dictionary */
  public boolean inDictionary (String word) throws IOException 
  {
    return findWordFreq(word.toLowerCase()) > 0;
  }
  
  /** 
   * Determine the frequency of a given word. Fairly inefficient, so should
   * only be called once in a while.
   */
  private int findWordFreq(String word) throws IOException 
  {
    // We can tell if a word is in the dictionary by looking up the simplest
    // edit map key for the word.
    //
    WordQueue queue = new WordQueue(1000);
    Word orig = new Word(word);
    LongSet checked = new LongSet(100);
    readEdKey(orig, comboKey(word, 0,1,2,3), 0, checked, queue);
    
    // Once we have the key words, see if this word is in the list.
    while (queue.size() > 0) {
      Word test = (Word) queue.pop();
      if (word.equals(test.word))
        return test.freq;
    }
    
    // Couldn't find it... we don't think it's in the dictionary.
    return 0;
  }
  
  
  /**
   * Suggest similar words to a given original word, but not including the
   * word itself.
   */
  public synchronized String[] suggestSimilar(String str, int numSugg)
    throws IOException 
  {
    // Get suggestions, including the original word
    Word[] suggs = suggestSimilar(new Word(str), numSugg + 1, 1);
    
    // Make an array, not including the original word
    StringList out = new StringList();
    for (int i=0; i<suggs.length; i++) {
      if (suggs[i].word.equals(str))
        continue;
      out.add(suggs[i].word);
    }
    return out.toArray();
  }
  
  /**
   * Suggest similar words to a given original word. A minimum frequency limit
   * is enforced.
   */
  private Word[] suggestSimilar(Word word, int numSugg, int minFreq)
    throws IOException 
  {
    int queueSize = numSugg + 10;
    final WordQueue queue = new WordQueue(queueSize);
    
    // Find all words that are close to the original and queue them.
    findCloseWords(word, minFreq, queue);
    
    // Pop everything out of the queue and convert to an array
    Word[] array = new Word[Math.min(numSugg, queue.size())];
    for (int i=queue.size()-1; i>=0; i--) {
        Word sugg = (Word) queue.pop();
        if (i < array.length)
            array[i] = sugg;
    }
    
    if (debugWriter != null) {
      debugWriter.println("  Final suggestion(s):");
      for (int i=0; i<array.length; i++) {
        debugWriter.print("    ");
        array[i].debug(debugWriter);
      }
    }
    
    return array;
  }

  /**
   * Keyword-oriented spelling suggestion mechanism. For an ordered list of
   * terms, come up with suggestions that have a good chance of improving  
   * the precision and/or recall.
   * 
   * @param terms           Ordered list of query terms
   * @return                One suggestion per term. If unchanged, there
   *                        was no better suggestion. If null, it is
   *                        suggested that the term be deleted.
   */
  public synchronized String[] suggestKeywords(String[] terms) 
    throws IOException
  {
    // No terms? Then we can't suggest anything.
    if (terms.length == 0)
        return terms;
    
    // Start with a null change.
    Phrase bestPhrase = new Phrase();
    bestPhrase.descrip = "no change";
    bestPhrase.words = new Word[terms.length];
    for (int i=0; i<terms.length; i++)
      bestPhrase.words[i] = new Word(terms[i].toLowerCase());
    bestPhrase.calcScore();
    
    // If there's just one word, our work is simple: just find the best 
    // replacement for that word.
    //
    if (terms.length == 1)
      bestPhrase = subWord(bestPhrase, 0);
    else
    {
      // Consider two-word changes at each position.
      for (int i = 0; i < terms.length-1; i++)
          bestPhrase = subPair(bestPhrase, i);
    }
    
    // Convert to a string array, and recover the original case mapping. If
    // our suggestion only varies by case however, stick with the original.
    //
    String[] out = bestPhrase.toStringArray();
    for (int i=0; i<out.length; i++) {
      if (out[i] != null) {
        if (out[i].equalsIgnoreCase(terms[i]))
          out[i] = terms[i];
        else
          out[i] = StringUtil.copyCase(terms[i], out[i]);
      }
    }
    
    // All done.
    return out;
  } // suggestKeywords()
  
  /**
   * Substitute a single word at the given position, trying to improve the score.
   * 
   * @param in      the best we've done so far
   * @param pos     position to substitute at
   * @return        the best we can do at that position
   */
  private Phrase subWord(Phrase in, int pos) throws IOException
  {
    // Get a suggestion for replacing the word.
    int origFreq = findWordFreq(in.words[pos].word);
    Word[] suggs = suggestSimilar(in.words[pos], 1, origFreq+1);
    if (suggs.length == 0)
      return in;
    Word sugg = suggs[0];
    
    assert !sugg.word.equals(in.words[pos].word);
    
    // If no improvement, return the original.
    if (sugg == in.words[pos])
      return in;
    
    // Make a new phrase.
    Phrase out = (Phrase) in.clone();
    out.descrip = "replace '" + out.words[pos] + "' with '" + sugg + "'";
    out.words[0] = sugg;
    out.calcScore();
    return out;
  }

  /**
   * Consider a set of changes to the pair of words at the given position.
   * 
   * @param in  the current best we've found
   * @param pos           position to consider
   * @return              new best
   */
  private Phrase subPair(Phrase in, int pos) 
    throws IOException
  {
    Word word1 = in.words[pos];
    Word word2 = in.words[pos+1];
    
    // Get a list of independent suggestions for both words.
    final int NUM_SUG = 100;
    Word[] list1 = suggestSimilar(word1, NUM_SUG, 0);
    Word[] list2 = suggestSimilar(word2, NUM_SUG, 0);
    
    /*
    System.out.println("List 1:");
    for (int p1 = 0; p1 < list1.length; p1++)
        System.out.println("  " + list1[p1] + " " + list1[p1].metaphone);
    System.out.println("List 2:");
    for (int p2 = 0; p2 < list2.length; p2++)
        System.out.println("  " + list2[p2] + " " + list2[p2].metaphone);
    */
    
    // Now score all possible combinations, looking for the best one.
    float bestScore = 0.0f;
    Word bestSugg1 = null;
    Word bestSugg2 = null;
    for (int p1 = 0; p1 < list1.length; p1++) {
        Word sugg1 = list1[p1];
        for (int p2 = 0; p2 < list2.length; p2++) {
            Word sugg2 = list2[p2];
            float pairScore = scorePair(sugg1, sugg2);
            
            //System.out.println("replace '" + word1 + " " + word2 + "' with '" +
            //    sugg1 + " " + sugg2 + "': " + score);
            
            if (pairScore > bestScore) {
                bestScore = pairScore;
                bestSugg1 = sugg1;
                bestSugg2 = sugg2;
            }
        }
    }
    
    // If we couldn't find any pair that results in improvement, do nothing.
    if (bestSugg1 == null)
      return in;
    
    // If we found something better than doing nothing, record it.
    Phrase bestPhrase = (Phrase) in.clone();
    if (bestSugg2.equals(word2))
        bestPhrase.descrip = "replace '" + word1 + "' with '" + bestSugg1 + "'";
    else if (bestSugg1.equals(word1))
        bestPhrase.descrip = "replace '" + word2 + "' with '" + bestSugg2 + "'";
    else {
        bestPhrase.descrip = "replace '" + word1 + " " + word2 + "' with '" +
            bestSugg1 + " " + bestSugg2 + "'";
    }
    bestPhrase.words[pos] = bestSugg1;
    bestPhrase.words[pos+1] = bestSugg2;
    bestPhrase.calcScore();
    
    if (bestPhrase.score > in.score)
      return bestPhrase;
    else
      return in;
  }

  /**
   * Calculate a score for a suggested replacement for a given word.
   */
  private float scorePair(Word sugg1, Word sugg2)
    throws IOException
  {
    openPairFreqs();

    int origPairFreq = pairFreqs.get(sugg1.orig.word, sugg2.orig.word);
    int suggPairFreq = pairFreqs.get(sugg1.word, sugg2.word);
    if (suggPairFreq <= origPairFreq)
      return 0.0f;
    
    double freqFactor = (suggPairFreq + 1.0) / (origPairFreq + 1.0);
    float freqBoost = (float) (Math.log(freqFactor) / Math.log(100.0));
    return freqBoost;
  }
  
  /** Get the term frequency sample array for our dictionary. */
  private void loadFreqSamples()
      throws IOException 
  {
    // Default if no frequencies found will be to turn off frequency boosting
    int[] res = new int[5];
    res[0] = res[1] = res[2] = res[3] = res[4] = Integer.MAX_VALUE;

    // Find the frequency samples file and open it
    File freqSamplesFile = new File(spellDir, "freqSamples.dat");
    if (!freqSamplesFile.canRead())
      throw new IOException("Cannot open frequency samples file '" + freqSamplesFile + "'");
    
    BufferedReader reader = new BufferedReader(new FileReader(freqSamplesFile));
    int nSamples = 0;
    int[] samples = null;
    try {
      // If there were less than 500 terms to sample, turn off frequency
      // boosting.
      //
      int nTerms = Integer.parseInt(reader.readLine());
      if (nTerms >= 500) 
      {
        // Read in the samples.
        nSamples = Integer.parseInt(reader.readLine());
        samples = new int[nSamples];
        for (int i=0; i<nSamples; i++)
          samples[i] = Integer.parseInt(reader.readLine());
      }
    }
    catch (NumberFormatException e) {
      throw new IOException("term frequencies file corrupt");
    }
    finally {
      reader.close();
    }
    
    // Pick out the levels of most interest to us
    if (samples != null) {
      res[0] = samples[(int) (nSamples * 0.99)]; // top 1%
      res[1] = samples[(int) (nSamples * 0.90)]; // top 10%
      res[2] = samples[(int) (nSamples * 0.50)]; // top 50%
      res[3] = samples[(int) (nSamples * 0.25)]; // top 75%
      res[4] = samples[0];                       // all above-avg words
    }
      
    // All done.
    freqSamples = res;
  }

  private void openPairFreqs() throws IOException {
    if (pairFreqs == null) {
        pairFreqs = new PairFreqData();
        pairFreqs.add(new File(spellDir, "pairs.dat"));
    }
  }
  
  protected void finalize () throws Throwable {
    close();
  }
  
  private String calcMetaphone(String word) {
    return SpellWriter.calcMetaphone(word);
  }
  
  /**
   * Keeps track of a single word, either an original or suggested word.
   */
  private final class Word {
    public String            word;
    public Word              orig;
    public int               freq;
    
    public String            metaphone;
    
    private TRStringDistance wordDist;
    private TRStringDistance mphDist;
    
    public float             score;
    public float             freqBoost;

    /** Contructor for original words */
    public Word(String word) throws IOException {
      this(null, word, 0);
    }
    
    /** Constructor for suggested replacement words */
    public Word(Word inOrig, String word, int freq) throws IOException {
      this.word = word;
      this.orig = (inOrig == null) ? this : inOrig;
      this.freq = freq;
      
      metaphone = calcMetaphone(word);
      wordDist = mphDist = null; // lazily created if necessary
      
      // Calculate the edit distance and turn it into the base score
      float dist = orig.wordDist(word) / 2.0f;
      score = 1.0f - (dist/orig.length());
      
      // If the metaphone matches, nudge the score
      if (metaphone.equals(orig.metaphone))
        score += 0.1f;
      
      // If the first and last letters match, nudge the score.
      if (word.charAt(0) == orig.word.charAt(0) &&
          word.charAt(word.length()-1) == orig.word.charAt(orig.word.length()-1))
        score += 0.1f;
      
      // If this word is more frequent than normal, give it a nudge up.
      freqBoost = calcFreqBoost(freqSamples, freq);
      score += freqBoost;
    }
    
    public int length() { return word.length(); }
    
    public boolean equals(Word other) { return word.equals(other.word); }
    
    public int wordDist(String other) { 
      if (wordDist == null)
        wordDist = new TRStringDistance(word);
      return wordDist.getDistance(other); 
    }
    
    public int mphDist(String other) { 
      if (mphDist == null)
        mphDist = new TRStringDistance(metaphone);
      return mphDist.getDistance(other); 
    }
    
    public String toString() { return word; }
    
    /** Dump debugging output about this word */
    public void debug(PrintWriter w)
    {
      align(w, "word=" + word + "[" + orig.wordDist(word) + "]", 22);
      align(w, "mph=" + metaphone + "[" + orig.mphDist(metaphone) + "]", 13);
      align(w, "freq=" + freq, 8);
      
      // Calculate the edit distance and turn it into the base score
      float dist = orig.wordDist(word) / 2.0f;
      align(w, "base=" + (1.0f - (dist/orig.length())), 14);
      
      // If the metaphone matches, nudge the score
      String mphStr = "0";
      if (metaphone.equals(orig.metaphone))
        mphStr = "0.1";
      align(w, "mphBoost=" + mphStr, 13);
      
      // If the first and last letters match, nudge the score.
      String matchStr = "0";
      if (word.charAt(0) == orig.word.charAt(0) &&
          word.charAt(word.length()-1) == orig.word.charAt(orig.word.length()-1))
        matchStr = "" + 0.1f;
      align(w, "matchBoost=" + matchStr, 15);
      
      // If any frequency boost appplied, print it.
      align(w, "freqBoost=" + freqBoost, 20);

      // Total score
      align(w, "totalScore=" + score, 22);
      w.println();
    }
    
    private void align(PrintWriter w, String s, int width)
    {
      w.print(s);
      for (int i=0; i<(width - s.length()); i++)
        w.print(" ");
      w.print(" ");
    }

    /**
     * Calculate a boost factor based on the frequency of a term.
     */
    private float calcFreqBoost(int[] termFreqs, int freq)
    {
      if (freq <= 1)
        return 0.0f;
        
      // If this word is more frequent than normal, give it a nudge up.
      int i = 0;
      while(i < 5 && freq < termFreqs[i])
        i++;
      if (i == 0)
        return 0.25f;
      
      int loFreq = (i < 5) ? termFreqs[i] : 0;
      int hiFreq = termFreqs[i-1];
      
      float loBoost = (5-i) * 0.05f;
        
      float boost = (((freq-loFreq) * 50 / (hiFreq-loFreq)) / 1000.0f) + loBoost;
      return boost;
    }
  }    
  
  /** 
   * Queue of words, ordered by score and then frequency 
   */
  private static final class WordQueue extends PriorityQueue 
  {
    WordQueue (int size) {
      initialize(size);
    }

    protected final boolean lessThan (Object a, Object b) {
      Word wa = (Word)a;
      Word wb = (Word)b;
      
      //first criteria: the edit distance
      if (wa.score > wb.score)
        return false;
      if (wa.score < wb.score)
        return true;
  
      //second criteria (if first criteria is equal): the popularity
      if (wa.freq > wb.freq)
        return false;
      if (wa.freq < wb.freq)
        return true;
  
      return false;
    }
  }
  
  /**
   * Track an ordered group of words.
   */
  private class Phrase implements Cloneable
  {
    Word[] words;
    String descrip;
    float  score;

    public Object clone() { 
      try {
        Phrase out = (Phrase) super.clone();
        out.words = new Word[words.length];
        System.arraycopy(words, 0, out.words, 0, words.length);
        return out;
      }
      catch (CloneNotSupportedException e) {
        return null;
      } 
    }
    
    public void calcScore() throws IOException {
      float wordScore = 0.0f;
      float pairScore = 0.0f;
      for (int i=0; i<words.length; i++) {
        wordScore += words[i].score;
        if (i < words.length-1)
          pairScore += scorePair(words[i], words[i+1]);
      }
      score = wordScore + pairScore;
    }
    
    public String[] toStringArray()
    {
      String[] out = new String[words.length];
      for (int i=0; i<words.length; i++)
        out[i] = words[i].word;
      return out;
    }
  }

} // class SpellReader
