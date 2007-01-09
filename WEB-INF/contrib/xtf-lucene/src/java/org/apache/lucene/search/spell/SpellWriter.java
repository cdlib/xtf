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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.regex.Pattern;

import org.cdlib.xtf.util.CountedInputStream;
import org.cdlib.xtf.util.IntList;
import org.cdlib.xtf.util.IntegerValues;

/**
 * <p>
 * Spell Writer class <br/>(very distantly based on Nicolas Maisonneuve /
 * David Spencer code).
 * 
 * Provides efficient, high-volume updates to a spelling correction index.
 * </p>
 * 
 * @author Martin Haye
 */
public class SpellWriter 
{
  /** Directory to store the spelling dictionary in */
  private String spellIndexPath;
  
  /** File to queue words into */
  private File wordQueueFile;

  /** File to queue words into */
  private File pairQueueFile;

  /** File containing compiled word frequencies */
  private File freqFile;
  
  /** File containing frequency sample data */
  private File sampleFile;
  
  /** File containing edit map data */ 
  private File edmapFile;
  
  /** File containing compiled pair frequency data */
  private File pairFreqFile;
  
  /** Boolean object to mark words in cache */
  private static final Boolean trueValue = new Boolean(true);

  /** Number of words added during flush */
  private int totalAdded;
  
  /** For writing to the word queue */
  private PrintWriter wordQueueWriter = null;
  
  /** For writing to the pair queue */
  private PrintWriter pairQueueWriter = null;
  
  /** How large to make the cache of recently added words */
  private static final int MAX_RECENT_WORDS = 20000;

  /** For counting word frequencies prior to write */
  private HashMap recentWords = new HashMap(MAX_RECENT_WORDS);

  /** Max # of pairs to hash before flushing */
  private static final int MAX_RECENT_PAIRS = 200000;

  /** For counting pair frequencies prior to write */
  private HashMap recentPairs = new HashMap(MAX_RECENT_PAIRS);

  /** Minimum frequency to retain in frequency data file */
  private int minWordFreq;

  /** Used for calculating double metaphone keys */
  private static DoubleMetaphone doubleMetaphone = new DoubleMetaphone();
  
  /** Used for splitting lines delimited with bar */
  Pattern splitPat = Pattern.compile("\\|");
    
  /** 
   * Establishes the directory to store the dictionary in. 
   */
  public synchronized void open(String spellIndexPath, int minWordFreq) 
    throws IOException 
  {
    this.minWordFreq = minWordFreq;
    this.spellIndexPath = new File(spellIndexPath).getAbsolutePath() + File.separator;
    
    // Figure out the files we're going to store stuff in
    wordQueueFile = new File(spellIndexPath, "newWords.txt");
    pairQueueFile = new File(spellIndexPath, "newPairs.txt");
    freqFile = new File(spellIndexPath, "words.dat");
    sampleFile = new File(spellIndexPath, "freqSamples.dat");
    edmapFile = new File(spellIndexPath, "edmap.dat");
    pairFreqFile = new File(spellIndexPath, "pairs.dat");
    
    // If the index directory doesn't exist, make it.
    File dir = new File(spellIndexPath);
    if (!dir.isDirectory()) {
      if (!dir.mkdir())
        throw new IOException("Error creating spelling index directory");
    }
  }

  /** 
   * Closes all files. Does NOT write queued words (they stay queued on
   * disk.) 
   */
  public synchronized void close() throws IOException {
    closeQueueWriters();
  }

  /** Delete all words in the dictionary (including those queued on disk) */
  public synchronized void clearIndex() throws IOException {
    close();
    
    wordQueueFile.delete();
    pairQueueFile.delete();
    freqFile.delete();
    sampleFile.delete();
    edmapFile.delete();
    pairFreqFile.delete();
    
    recentWords.clear();
    recentPairs.clear();
  }

  /**
   * Queue the given word if not recently checked. Caller should periodically
   * call checkFlush() to see whether the queue is getting full. The queue will
   * auto-expand if necessary, but it's better to flush it when near full.
   */
  public synchronized void queueWord(String prevWord, String word) throws IOException 
  {
    openQueueWriters();
    
    // Is this a pair?
    if (prevWord != null) 
    {
        // Calculate a key for this pair, and get the current count
        String key = prevWord + "|" + word;
        Integer val = (Integer) recentPairs.get(key);
        
        // Increment the count
        if (val == null)
            val = IntegerValues.valueOf(1);
        else
            val = IntegerValues.valueOf(val.intValue() + 1);
        
        // Store it, and if the hash is full, flush it.
        recentPairs.put(key, val);
        if (recentPairs.size() >= MAX_RECENT_PAIRS)
            flushRecentPairs();
    }

    // Bump the count for this word.
    Integer val = (Integer) recentWords.get(word);
    if (val == null)
        val = IntegerValues.valueOf(1);
    else
        val = IntegerValues.valueOf(val.intValue() + 1);
    
    // Store it, and if the hash is full, flush it.
    recentWords.put(word, val);
    if (recentWords.size() >= MAX_RECENT_WORDS)
        flushRecentWords();
  } // queueWord()
  
  /** 
   * Flush any accumulated pairs, with their counts. For efficiency, skip any 
   * pair that appeared only once.
   */
  private void flushRecentPairs() throws IOException {
    Set keySet = recentPairs.keySet();
    ArrayList list = new ArrayList(keySet);
    Collections.sort(list);
    for (int i=0; i<list.size(); i++) {
        String key = (String) list.get(i);
        int count = ((Integer)recentPairs.get(key)).intValue();
        if (count > 1)
            pairQueueWriter.println(key + "|" + count);
    }
    pairQueueWriter.flush();
    recentPairs.clear();
  }

  /** 
   * Flush any accumulated words, with their counts.
   */
  private void flushRecentWords() throws IOException {
    Set keySet = recentWords.keySet();
    ArrayList list = new ArrayList(keySet);
    Collections.sort(list);
    for (int i=0; i<list.size(); i++) {
        String key = (String) list.get(i);
        int count = ((Integer)recentWords.get(key)).intValue();
        wordQueueWriter.println(key + "|" + count);
    }
    wordQueueWriter.flush();
    recentWords.clear();
  }

  /** Check if any words are queued for add. */
  public synchronized boolean anyWordsQueued() throws IOException {
    closeQueueWriters();
    long queueSize = wordQueueFile.length();
    return queueSize > 1;
  }

  /** 
   * Called periodically during the flush process. Override to output status
   * messages.
   */
  public void progress(int phase, int percentDone, int totalAdded) {
    // Default: do nothing.
  }
  
  /**
   * Ensures that all words in the queue are written to the dictionary on disk.
   * 
   * @return number of non-duplicate words actually written
   */
  public synchronized void flushQueuedWords() throws IOException {
    closeQueueWriters();
    
    int totalAdded = 0;
    
    // Phase 1: Add words and their ngrams to the Lucene index
    totalAdded += flushPhase1();
    
    // Phase 2: Accumulate pairs into the pair data file
    totalAdded += flushPhase2();
    
  } // flushQueuedWords()

  /**
   * Performs the word-adding phase of the flush procedure.
   * 
   * @return    the number of pairs added
   * @throws    IOException if something goes wrong
   */
  private int flushPhase1() throws IOException
  {
    // If there are no new words to add, skip this phase.
    if (!wordQueueFile.canRead()) {
      progress(1, 100, 0);
      return 0;
    }
      
    // Initial progress message
    progress(1, 0, 0);
    
    // Read the existing frequency list (if any)
    HashMap freqMap = new HashMap();
    readFreqs(freqFile, freqMap);
    
    // Add in the new frequencies, then remove entries with low frequency.
    readFreqs(wordQueueFile, freqMap);
    cullFreqs(freqMap, minWordFreq);
    
    // And write out the accumulated frequencies
    File newFreqFile = new File(spellIndexPath, "words.dat.new");
    writeFreqs(newFreqFile, freqMap);
    
    // Write out frequency samples for statistical purposes.
    File newSampleFile = new File(spellIndexPath, "freqSamples.dat.new");
    writeFreqSamples(freqMap, newSampleFile);
    
    // Let's call that 5% of the work.
    progress(1, 5, 0);
    
    // Now comes the time-consuming work: making the edit distance map, giving
    // progress updates once in a while.
    //
    HashMap editMap = new HashMap();
    int nDone = 0;
    for (Iterator keyIter = freqMap.keySet().iterator(); keyIter.hasNext(); ) {
      addCombos((String) keyIter.next(), editMap);
      if ((nDone++ & 0xFFF) == 0) {
        int pctDone = (nDone * 45 / freqMap.size()) + 5;
        progress(1, pctDone, nDone);
      }
    }
    
    // Write out the new edit map.
    File newEdmapFile = new File(spellIndexPath, "edmap.dat.new");
    writeEdMap(freqMap, editMap, nDone, newEdmapFile);
    
    // Clear the queue, and replace the old data files.
    replaceFile(freqFile, newFreqFile);
    replaceFile(sampleFile, newSampleFile);
    replaceFile(edmapFile, newEdmapFile);
    deleteFile(wordQueueFile);
    
    // All done.
    progress(1, 100, totalAdded);
    return totalAdded;
  }

  /**
   * Write out a prefix-compressed edit-distance map, which also contains
   * term frequencies.
   */
  private void writeEdMap(HashMap freqMap, HashMap editMap, int nDone, File file) 
    throws IOException
  {
    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
    StringBuffer sbuf = new StringBuffer();
    CharsetEncoder enc = Charset.forName("UTF-8").newEncoder();
    byte[] bytes = new byte[1000];
    ByteBuffer bbuf = ByteBuffer.wrap(bytes);
    
    ArrayList edKeys = new ArrayList(editMap.keySet());
    Collections.sort(edKeys);
    
    int pos = 0;
    IntList sizes = new IntList(edKeys.size());
    for (int i=0; i<edKeys.size(); i++) {
      String key = (String) edKeys.get(i);
      ArrayList words = new ArrayList((LinkedList) editMap.get(key));
      Collections.sort(words);
      if (words.size() == 0)
        continue;
      
      String prev = (String) words.get(0);
      int freq = ((IntHolder)freqMap.get(prev)).value;
      
      // Write the key and the first word in full
      sbuf.setLength(0);
      sbuf.append(key);
      sbuf.append('|');
      sbuf.append(prev);
      sbuf.append('|');
      sbuf.append(Integer.toString(freq));
      
      // Prefix-compress the list.
      for (int j=0; j<words.size(); j++) {
        String word = (String) words.get(j);
        
        // Skip duplicates
        if (word.equals(prev))
          continue;
        
        // Get the frequency of the word
        freq = ((IntHolder)freqMap.get(word)).value;
        
        // Figure out how many characters overlap.
        int k;
        for (k=0; k<Math.min(prev.length(), word.length()); k++) {
          if (word.charAt(k) != prev.charAt(k))
            break;
        }
        
        // Write it the prefix length, suffix, and frequency
        sbuf.append('|');
        sbuf.append((char) ('0' + k));
        sbuf.append(word.substring(k));
        sbuf.append('|');
        sbuf.append(Integer.toString(freq));
        
        // Next...
        prev = word;
      }
      
      // Done with this line.
      sbuf.append('\n');
      int needed = (int) enc.maxBytesPerChar() * sbuf.length();
      if (bytes.length < needed) {
        bytes = new byte[needed];
        bbuf = ByteBuffer.wrap(bytes);
      }
      bbuf.position(0);
      enc.encode(CharBuffer.wrap(sbuf), bbuf, true);
      int nBytes = bbuf.position();
      out.write(bytes, 0, nBytes);
      sizes.add(nBytes);
      pos += nBytes;
      
      // Give progress every once in a while.
      if ((i & 0xFFF) == 0) {
        int pctDone = (i * 49 / editMap.size()) + 50;
        progress(1, pctDone, nDone);
      }
    }    
    
    // At the end of the file, write an index of positions.
    int indexPos = pos;
    OutputStreamWriter w = new OutputStreamWriter(out, "UTF-8");
    w.append("edMap index\n");
    w.append(Integer.toString(edKeys.size()));
    w.append('\n');
    for (int i=0; i<edKeys.size(); i++) {
      String key = (String) edKeys.get(i);
      w.append(key);
      w.append('|');
      w.append(Integer.toString(sizes.get(i)));
      w.append('\n');
    }
    
    // And finally, at the very end, write the position of the index.
    sbuf.replace(0, sbuf.length(), Integer.toString(indexPos));
    while (sbuf.length() < 20)
      sbuf.insert(0, ' ');
    sbuf.append('\n');
    w.append(sbuf);
    
    // All done.
    w.close();
  }

  /** Write term frequency samples to the given file. */
  private void writeFreqSamples(HashMap freqMap, File file)
      throws IOException 
  {
    // Calculate the mean of the term frequencies
    long totalFreq = 0L;
    for (Iterator iter = freqMap.values().iterator(); iter.hasNext(); )
      totalFreq += ((IntHolder)iter.next()).value;
    double avgFreq = totalFreq / (double)freqMap.size();
    
    // Eliminate all at- or below-average frequencies.
    IntList aboveAvgFreqs = new IntList( freqMap.size() );
    for (Iterator iter = freqMap.values().iterator(); iter.hasNext(); ) {
      int freq = ((IntHolder)iter.next()).value;
      if (freq > avgFreq)
        aboveAvgFreqs.add(freq);
    }
    
    // Sort the array by frequency.
    aboveAvgFreqs.sort();
    
    // If more than 1000 entries, sample it down.
    final int MAX_SAMPLES = 1000;
    IntList finalFreqs;
    if (aboveAvgFreqs.size() < MAX_SAMPLES)
        finalFreqs = aboveAvgFreqs;
    else
    {
        finalFreqs = new IntList(MAX_SAMPLES);
        for( int i = 0; i < MAX_SAMPLES; i++ ) {
            int pos = (int) (((long)i) * aboveAvgFreqs.size() / MAX_SAMPLES);
            finalFreqs.add(aboveAvgFreqs.get(pos));
        }
    }
    
    // Make sure the very first sample reflects the average
    if (finalFreqs.size() > 0)
        finalFreqs.set(0, (int)avgFreq);

    // Write out the data
    PrintWriter writer = new PrintWriter(new FileWriter(file));
    writer.println(freqMap.size());
    writer.println(finalFreqs.size());
    for (int i = 0; i < finalFreqs.size(); i++)
        writer.println(finalFreqs.get(i));
    writer.close();
  } // writeFreqSamples()

  /**
   * Add combinations of the first six letters of the word, capturing all the
   * possibilities that represent an edit distance of 2 or less.
   */
  private void addCombos(String word, HashMap editMap)
  {
    // Add combinations to the edit map
    addCombo(word, editMap, 0, 1, 2, 3);
    addCombo(word, editMap, 0, 1, 2, 4);
    addCombo(word, editMap, 0, 1, 2, 5);
    addCombo(word, editMap, 0, 1, 3, 4);
    addCombo(word, editMap, 0, 1, 3, 5);
    addCombo(word, editMap, 0, 1, 4, 5);
    addCombo(word, editMap, 0, 2, 3, 4);
    addCombo(word, editMap, 0, 2, 3, 5);
    addCombo(word, editMap, 0, 2, 4, 5);
    addCombo(word, editMap, 0, 3, 4, 5);
    if (word.length() > 1) {
      addCombo(word, editMap, 1, 2, 3, 4);
      addCombo(word, editMap, 1, 2, 3, 5);
      addCombo(word, editMap, 1, 2, 4, 5);
      addCombo(word, editMap, 1, 3, 4, 5);
      if (word.length() > 2)
        addCombo(word, editMap, 2, 3, 4, 5);
    }
  }
      
  /** Add a combination of letters to the edit map */
  private void addCombo(String word, HashMap editMap, 
                        int p0, int p1, int p2, int p3)
  {
    String key = comboKey(word, p0, p1, p2, p3);
    LinkedList list = (LinkedList) editMap.get(key);
    if (list == null) {
      list = new LinkedList();
      editMap.put(key, list);
    }
    list.add(word);
  }

  /** Calculate a key from the given characters of the word. */
  static String comboKey(String word, int p0, int p1, int p2, int p3)
  {
    char[] ch = new char[4];
    ch[0] = word.length() > p0 ? word.charAt(p0) : ' ';
    ch[1] = word.length() > p1 ? word.charAt(p1) : ' ';
    ch[2] = word.length() > p2 ? word.charAt(p2) : ' ';
    ch[3] = word.length() > p3 ? word.charAt(p3) : ' ';
    for (int i=0; i<4; i++)
      ch[i] = (char) (ch[i] & 0x7f);
    return new String(ch);
  }

  /** Attempt to delete (and at least truncate) the given file. */ 
  private void deleteFile(File file) throws IOException
  {
    // First, simply try to delete it.
    if (file.delete())
      return;
    
    // Couldn't delete it... at least truncate it.
    FileOutputStream tmp = new FileOutputStream(file);
    tmp.close();
  }
  
  /** Replace an old file with a new one */
  private void replaceFile(File oldFile, File newFile)
  {
    // First, try to delete the old one.
    oldFile.delete();
    
    // Then rename the new one to the old one's name.
    newFile.renameTo(oldFile);
  }

  /**
   * Write out frequency data, in sorted order.
   */
  private void writeFreqs(File file, HashMap freqMap) throws IOException
  {
    BufferedWriter out = new BufferedWriter(new FileWriter(file));
    ArrayList keys = new ArrayList(freqMap.keySet());
    Collections.sort(keys);
    for (int i=0; i<keys.size(); i++) {
      String key = (String) keys.get(i);
      IntHolder freq = (IntHolder) freqMap.get(key);
      out.write(key);
      out.write("|");
      out.write(Integer.toString(freq.value));
      out.write("\n");
    }
    out.close();
  }
  
  /**
   * Remove entries from the map where the freqency is less than the given
   * threshold.
   */
  private void cullFreqs(HashMap freqMap, int minThresh)
  {
    for (Iterator keyIter = freqMap.keySet().iterator(); keyIter.hasNext(); ) {
      String key = (String) keyIter.next();
      IntHolder freq = (IntHolder) freqMap.get(key);
      if (freq.value < minThresh)
        keyIter.remove();
    }
  }

  /**
   * Read an existing frequency file, and add it to the in-memory frequency 
   * tables.
   */
  private void readFreqs(File file, HashMap freqMap) 
    throws IOException
  {
    // Skip if we can't open the file.
    if (!file.canRead())
      return;
    
    // Read each line, consisting of a word and a count separated by "|"
    BufferedReader freqReader = new BufferedReader(new FileReader(file));
    while (true) {
      String line = freqReader.readLine();
      if (line == null)
        break;
      String[] tokens = splitPat.split(line, 0);
      
      // If we haven't seen this word yet, allocate a spot for it.
      IntHolder count = (IntHolder) freqMap.get(tokens[0]);
      if (count == null) {
        count = new IntHolder();
        freqMap.put(tokens[0], count);
      }
      
      // Accumulate the frequency
      count.value += Integer.parseInt(tokens[1]);
    }
    freqReader.close();
  }

  /**
   * Performs the pair-adding phase of the flush procedure.
   * 
   * @return    the number of pairs added
   * @throws    IOException if something goes wrong
   */
  private int flushPhase2() throws IOException
  {
    // Read in existing pair data (if any)
    PairFreqData pairData = new PairFreqData();
    if (pairFreqFile.canRead())
      pairData.add(pairFreqFile);
    
    // Open the queue, and put a counter on it so we can give accurate
    // progress messages.
    //
    InputStream queueRaw = null;
    try {
      queueRaw = new FileInputStream(pairQueueFile);
    }
    catch( IOException e ) {
      progress(2, 100, 0);
      return 0;
    }
    
    CountedInputStream queueCounted = new CountedInputStream(queueRaw);
    BufferedReader     queueReader  = new BufferedReader(new InputStreamReader(queueCounted, "UTF-8"));
    
    // Initial progress message.
    progress(2, 0, 0);
    
    // Process each pair in the queue.
    long fileTotal = pairQueueFile.length();
    int prevPctDone = 0;
    totalAdded = 0;

    try {
      boolean eof = false;
      while( !eof ) {
        String line = queueReader.readLine();
        if (line == null) {
          eof = true;
          break;
        }

        // Break up the three components of each line (separated by |)
        String[] tokens = splitPat.split(line);
        if (tokens.length == 3) {
          String word1 = tokens[0];
          String word2 = tokens[1];
          String countTxt = tokens[2];
      
          try {
            pairData.add(word1, word2, Integer.parseInt(countTxt));
            ++totalAdded;
            
            // Every 4000 or so words, give some status feedback.
            // Only allocate 90%, leaving 10% for the final write.
            //
            if( (totalAdded & 0xFFF) == 0 ) {
              long filePos = queueCounted.nRead();
              int pctDone = (int) ((filePos+1) * 90 / (fileTotal+1));
              progress(2, pctDone, totalAdded);
            }
          }
          catch(NumberFormatException e) { /*ignore*/ }
        }
      } // while
    }
    finally {
      queueReader.close();
      queueCounted.close();
      queueRaw.close();
    }
    
    // Write out the resulting data and replace the old data file, if any.
    File newPairFreqFile = new File(spellIndexPath, "pairs.dat.new");
    newPairFreqFile.delete();
    pairData.save(newPairFreqFile);
    if (pairFreqFile.canRead() && !pairFreqFile.delete())
      throw new IOException("Could not delete old pair data file -- permission problem?");
    if (!newPairFreqFile.renameTo(pairFreqFile))
      throw new IOException("Could not rename new pair data file -- permission problem?");
    
    // Clear out (and try to delete) the queue file.
    FileOutputStream tmp = new FileOutputStream(pairQueueFile);
    tmp.close();
    pairQueueFile.delete();
    
    // All done.
    progress(2, 100, totalAdded);
    return totalAdded;
  }

  /** Opens the queue writer, closing the queue reader if necessary. */
  private void openQueueWriters() throws IOException {
    // If already open, skip re-opening.
    if (wordQueueWriter != null)
        return;

    // Open the writers now. Be sure to append if they already exist.
    wordQueueWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                           new FileOutputStream(wordQueueFile, true),
                           "UTF-8")));
    pairQueueWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                           new FileOutputStream(pairQueueFile, true),
                           "UTF-8")));
  } // openQueueWriters()

  /** Closes the queue writer if it's open */
  private void closeQueueWriters() throws IOException {
    if (wordQueueWriter != null) {
        flushRecentWords();
        wordQueueWriter.close();
        wordQueueWriter = null;
    }
    if (pairQueueWriter != null) {
        flushRecentPairs();
        recentPairs = null;
        pairQueueWriter.close();
        pairQueueWriter = null;
    }
  } // closeQueueWriters()


  public static String calcMetaphone( String word ) {
    return doubleMetaphone.doubleMetaphone( word );
  }

  protected void finalize() throws Throwable {
    close();
  }
  
  private static class IntHolder {
    public int value;
  }
} // class SpellWriter
