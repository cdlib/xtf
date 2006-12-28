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
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.cdlib.xtf.util.CountedInputStream;
import org.cdlib.xtf.util.FastStringCache;
import org.cdlib.xtf.util.IntList;
import org.cdlib.xtf.util.IntegerValues;
import org.cdlib.xtf.util.StringList;
import org.cdlib.xtf.util.StructuredFile;
import org.cdlib.xtf.util.SubStoreWriter;

/**
 * <p>
 * Spell Writer class <br/>(built on Nicolas Maisonneuve / David Spencer code).
 * Provides efficient, high-volume updates to a spelling correction index.
 * </p>
 * 
 * @author Martin Haye
 * @version 1.0
 */
public class SpellWriter {
  /** Field name for each word in the ngram index. */
  public static final String F_WORD = "word";

  /** Directory to store the spelling dictionary in */
  private String spellIndexPath;
  
  /** Lucene Directory of the spelling dictionary */
  private Directory spellIndexDir;
  
  /** Directory to get term frequencies from */
  private String mainIndexPath;
  
  /** File to queue words into */
  private File wordQueueFile;

  /** File to queue words into */
  private File pairQueueFile;
  
  /** How large to make the cache of recently added words */
  private static final int CACHE_SIZE = 20000;

  /** Cache of recently added words (so we don't look them up twice.) */
  private FastStringCache recentWords = new FastStringCache(CACHE_SIZE);

  /** Boolean object to mark words in cache */
  private static final Boolean trueValue = new Boolean(true);

  /** Max # of queued words to process at a time */
  private static final int BLOCK_SIZE = 50000;
  
  /** Number of words added during flush */
  private int totalAdded;
  
  /** For writing to the word queue */
  private PrintWriter wordQueueWriter = null;
  
  /** For writing to the pair queue */
  private PrintWriter pairQueueWriter = null;
  
  /** For counting pair frequencies prior to write */
  private HashMap pairHash = null;

  /** Max # of pairs to hash before flushing */
  private static final int MAX_PAIR_HASH_SIZE = 200000;

  /** For checking term existence */
  private IndexReader spellIndexReader;

  /** For writing new terms */
  private IndexWriter spellIndexWriter;

  /** Used for calculating double metaphone keys */
  private static DoubleMetaphone doubleMetaphone = new DoubleMetaphone();
    
  /** 
   * Establishes the directory to store the dictionary in. 
   */
  public synchronized void open( String mainIndexPath, String spellIndexPath ) 
    throws IOException 
  {
    this.mainIndexPath = new File(mainIndexPath).getAbsolutePath() + File.separator;
    this.spellIndexPath = new File(spellIndexPath).getAbsolutePath() + File.separator;
    this.wordQueueFile = new File(spellIndexPath + "newWords.txt");
    this.pairQueueFile = new File(spellIndexPath + "newPairs.txt");
    
    // Open the index directory, if it exists. If not, create it.
    if (new File(spellIndexPath).isDirectory()) {
        spellIndexDir  = FSDirectory.getDirectory(spellIndexPath, false);
        IndexReader.unlock( spellIndexDir );
    }
    else
        spellIndexDir = FSDirectory.getDirectory( spellIndexPath, true );
  }

  /** 
   * Closes all files. Does NOT write queued words (they stay queued on
   * disk.) 
   */
  public synchronized void close() throws IOException {
    closeQueueWriters();
    closeSpellIndexReader();
    closeSpellIndexWriter();
  }

  /** Delete all words in the dictionary (including those queued on disk) */
  public synchronized void clearIndex() throws IOException {
    close();
    wordQueueFile.delete();
    pairQueueFile.delete();
    recentWords.clear();
    if (spellIndexDir != null) {
        IndexReader.unlock(spellIndexDir);
        IndexWriter writer = new IndexWriter(spellIndexDir, null, true);
        writer.close();
    }
  }

  /**
   * Queue the given word if not recently checked. Caller should periodically
   * call checkFlush() to see whether the queue is getting full. The queue will
   * auto-expand if necessary, but it's better to flush it when near full.
   */
  public synchronized void queueWord(String field, String prevWord, String word) throws IOException {
    openQueueWriters();
    
    if (prevWord != null) 
    {
        // Calculate a key for this pair, and get the current count
        String key = field + "|" + prevWord + "|" + word;
        Integer val = (Integer) pairHash.get(key);
        
        // Increment the count
        if (val == null)
            val = IntegerValues.valueOf(1);
        else
            val = IntegerValues.valueOf(val.intValue() + 1);
        
        // Store it, and if the hash is full, flush it.
        pairHash.put(key, val);
        if (pairHash.size() >= MAX_PAIR_HASH_SIZE)
            flushPairHash();
    }

    // If we've seen the word recently, skip it.
    if (recentWords.contains(word))
      return;
    recentWords.put(word, trueValue);
    
    // Write the word.
    wordQueueWriter.println(word);
  } // queueWord()
  
  /** 
   * Flush any accumulated pairs, with their counts. For efficiency, skip any 
   * pair that appeared only once.
   */
  private void flushPairHash() throws IOException {
    Set keySet = pairHash.keySet();
    ArrayList list = new ArrayList(keySet);
    Collections.sort(list);
    for (int i=0; i<list.size(); i++) {
        String key = (String) list.get(i);
        int count = ((Integer)pairHash.get(key)).intValue();
        if (count > 1)
            pairQueueWriter.println( count + "|" + key);
    }
    pairQueueWriter.flush();
    pairHash.clear();
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
    
    // Phase 3: Get term frequency samples for all tokenized fields.
    if( totalAdded > 0 )
        flushPhase3();
    else
        progress(3, 100, 0);
    
  } // flushQueuedWords()

  /**
   * Performs the word-adding phase of the flush procedure.
   * 
   * @return    the number of pairs added
   * @throws    IOException if something goes wrong
   */
  private int flushPhase1() throws IOException
  {
    // Open up the queue, and put a counter on it so we can give accurate
    // progress messages.
    //
    InputStream queueRaw = null;
    try {
        queueRaw = new FileInputStream(wordQueueFile);
    }
    catch( IOException e ) {
        progress(1, 100, 0);
        return 0;
    }
    
    CountedInputStream queueCounted = new CountedInputStream(queueRaw);
    BufferedReader     queueReader  = new BufferedReader(new InputStreamReader(queueCounted, "UTF-8"));
    
    // Initial progress message
    progress(1, 0, 0);
    
    // Process each word in the queue
    long fileTotal = wordQueueFile.length();
    int prevPctDone = 0;
    totalAdded = 0;

    try {
        boolean eof = false;
        while( !eof ) {
          
            // Read in a block of words.
            ArrayList block = new ArrayList(BLOCK_SIZE);
            
            try {
                while (block.size() < BLOCK_SIZE && !eof) {
                    String word = queueReader.readLine();
                    if (word == null)
                        eof = true;
                    else
                        block.add(word);
                }
            }
            catch (EOFException e) { eof = true; }
        
            // Calculate the percent that will be done after this block.
            long filePos = queueCounted.nRead();
            int pctDone = (int) ((filePos+1) * 100 / (fileTotal+1));
            
            // Add the block.
            addBlock(block, prevPctDone, pctDone);
            prevPctDone = pctDone;
        } // while
    }
    finally {
        queueReader.close();
        queueCounted.close();
        queueRaw.close();
        closeSpellIndexWriter();
    }
    
    // Now that we've added them, clear the queue (truncate the file). Try
    // to delete it as well.
    //
    FileOutputStream tmp = new FileOutputStream(wordQueueFile);
    tmp.close();
    wordQueueFile.delete();
    
    // All done.
    progress(1, 100, totalAdded);
    return totalAdded;
  }

  /**
   * Add a block of words to the index, being sure to avoid adding duplicates.
   * 
   * @param block           Words to add
   * @param pct1            % done before add
   * @param pct2            % done after add
   * @throws IOException    If something bad happens during the process
   */
  private void addBlock(ArrayList block, int pct1, int pct2) throws IOException
  {
    // Sort the queued words. Checking them in order is faster since
    // the IndexReader can enumerate the terms in order.
    //
    Collections.sort(block);
    
    // Mark words that aren't duplicates and aren't in the index yet.
    openSpellIndexReader();
    String prevWord = null;
    BitSet wordsToAdd = new BitSet(block.size());
    for (int i = 0; i < block.size(); i++) {
      String word = (String) block.get(i);

      // Ignore duplicate words in the list. These can occur because our
      // cache of recently checked words is finite.
      //
      if (word.equals(prevWord))
        continue;
      prevWord = word;

      // Ignore words that are too short.
      if (word.length() < 3)
        continue;

      // Ignore words that are already in the index. Note that the
      // reader does fancy seeking and caching to make docFreq() pretty
      // fast.
      //
      if (spellIndexReader != null && spellIndexReader.docFreq(new Term(F_WORD, word)) > 0)
        continue;

      // Okay, got a live one. Mark it for addition to the index.
      wordsToAdd.set(i);
    } // for i

    // Now that we've identified the words that aren't yet in our index,
    // add them one at a time.
    //
    //System.out.print("w");
    openSpellIndexWriter();
    int nTotal = block.size();
    for (int i = 0; i < nTotal; i++) {
      // Only index words that have been marked to add.
      if (!wordsToAdd.get(i))
        continue;

      String word = (String) block.get(i);
      int len = word.length();

      spellIndexWriter.addDocument(createDocument(word, getMin(len), getMax(len)));
      ++totalAdded;
      
      // Every 1000 words or so, update the progress counter 
      if( (i & 0x3FF) == 0 ) {
          int pctDone = (i * (pct2 - pct1) / nTotal) + pct1;
          progress( 1, pctDone, totalAdded );
      }
    }
    
    // Optimize the index so that future access (and block adds) are fast.
    //System.out.print( "o " );
    spellIndexWriter.optimize();

    // Finish progress counter
    progress( 1, pct2, totalAdded );
    
  } // addBlock()
    
  /**
   * Performs the pair-adding phase of the flush procedure.
   * 
   * @return    the number of pairs added
   * @throws    IOException if something goes wrong
   */
  private int flushPhase2() throws IOException
  {
    // Read in existing pair data (if any)
    PairFreqWriter pairWriter = new PairFreqWriter();
    File pairFreqFile = new File(spellIndexPath + "pairs.dat");
    if (pairFreqFile.canRead())
      pairWriter.add(pairFreqFile);
    
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

            // Break up the four components of each line (separated by |)
            StringTokenizer st = new StringTokenizer(line, "|");
            String countTxt = st.hasMoreTokens() ? st.nextToken() : null;
            String field = st.hasMoreTokens() ? st.nextToken() : null;
            String word1 = st.hasMoreTokens() ? st.nextToken() : null;
            String word2 = st.hasMoreTokens() ? st.nextToken() : null;
            
            if (word2 != null) {
                try {
                    pairWriter.add(field, word1, word2, Integer.parseInt(countTxt));
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
    File newPairFreqFile = new File(spellIndexPath + "pairs.dat.new");
    newPairFreqFile.delete();
    pairWriter.save(newPairFreqFile);
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
  
  /** 
   * Get term frequency samples for all tokenized fields.
   *  
   * @return the number of fields sampled
   * @throws IOException if something goes wrong.*/
  private int flushPhase3() throws IOException
  {
    File sampleFile = new File(spellIndexPath + "freqSamples.dat");
    File newSampleFile = new File(spellIndexPath + "freqSamples.dat.new");
    int nFields = 0;
    totalAdded = 0;
    
    // Initial progress message
    progress(3, 0, 0);

    // Make a list of all tokenized fields.
    IndexReader mainIndexReader = null;
    
    try 
    {
        mainIndexReader = IndexReader.open( mainIndexPath );
        TermDocs docs = mainIndexReader.termDocs();
        Collection fields = mainIndexReader.getFieldNames(true); // indexed only
        StringList tokenizedFields = new StringList();
        for (Iterator iter = fields.iterator(); iter.hasNext(); ) {
            String fieldName = (String) iter.next();
            
            // The only way to tell if a field is tokenized is to look at a
            // document that contains it. And to do that, we need to find
            // one. That means we need a term for the field.
            //
            TermEnum terms = mainIndexReader.terms( new Term(fieldName, "") );
            if (!terms.next()) {
                terms.close();
                continue;
            }
            Term t = terms.term();
            terms.close();
            if (!t.field().equals(fieldName))
                continue;
            
            // Finally, we have a term. Get the first document using that term.
            docs.seek(terms);
            if (!docs.next())
                continue;
            int docId = docs.doc();
            Document doc = mainIndexReader.document(docId);
            
            // Okay, see if this field was tokenized. If not, skip it.
            Field field = doc.getField(fieldName);
            if (field == null || !field.isTokenized())
                continue;
            
            // Got a tokenized field... remember it.
            tokenizedFields.add(fieldName);
        }
        docs.close();
        nFields = tokenizedFields.size();
        
        // No fields? Nothing to do.
        if (nFields == 0) {
            progress(3, 100, 0);
            return 0;
        }
        
        // Now get samples for each one, and write them to a structured file.
        newSampleFile.delete();
        StructuredFile sf = StructuredFile.create(newSampleFile);
        for (int i = 0; i < nFields; i++) {
            progress(3, i * 100 / nFields, totalAdded);
            String fieldName = tokenizedFields.get(i);
            SubStoreWriter sub = sf.createSubStore(fieldName + ".samples");
            totalAdded += writeFreqSamples(mainIndexReader, fieldName, sub);
            sub.close();
        }
        sf.close();
    }
    finally {
        if (mainIndexReader != null)
            mainIndexReader.close();
        mainIndexReader = null;
    }
    
    // Replace the old sample file (if there is one)
    if (sampleFile.canRead() && !sampleFile.delete())
        throw new IOException("Could not delete old term sample file -- permission problem?");
    if (!newSampleFile.renameTo(sampleFile))
        throw new IOException("Could not rename new term sample file -- permission problem?");
    
    // All done.
    progress(3, 100, totalAdded);
    return totalAdded;
  }

  /** Write term frequency samples for the given field. */
  private int writeFreqSamples(IndexReader reader, String fieldName, SubStoreWriter writer)
      throws IOException 
  {
    // Read in all the term frequencies for this field.
    IntList rawFreqs = new IntList( 10000 );
    TermEnum terms = reader.terms( new Term(fieldName, "") );
    long totalFreq = 0;
    int nTerms = 0;
    while( terms.next() ) {
        Term t = terms.term();
        if( !t.field().equals(fieldName) )
            break;
        
        // Skip numeric, two-letter, one-letter terms (and others if client
        // overrides shouldSkipTerm()
        //
        if( shouldSkipTerm(t.text()) )
            continue;
        
        // Record the frequency of this term.
        int docFreq = terms.docFreq();
        rawFreqs.add( docFreq );
        totalFreq += docFreq;
        ++nTerms;
    } // while
    
    terms.close();
    
    // Calculate the mean of the term frequencies
    double avgFreq = totalFreq / (double)rawFreqs.size();
    
    // Eliminate all at- or below-average frequencies.
    IntList aboveAvgFreqs = new IntList( rawFreqs.size() );
    for( int i = 0; i < rawFreqs.size(); i++ ) {
        int freq = rawFreqs.get( i );
        if( freq > avgFreq )
            aboveAvgFreqs.add( freq );
    }
    
    System.out.println( "Field " + fieldName + ": " + aboveAvgFreqs.size() + 
                        " above-avg terms out of " + rawFreqs.size() );
    
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
    writer.writeInt(nTerms);
    writer.writeInt(finalFreqs.size());
    for (int i = 0; i < finalFreqs.size(); i++)
        writer.writeInt(finalFreqs.get(i));
    
    // All done.
    return nTerms;
  } // writeFreqSamples()

  /**
   * Determines if a given term should be skipped (not considered for spelling
   * suggestions). Can be overridden for finer control.
   * 
   * @param term    the term to consider
   * @return        true if the term should be skipped
   */
  public boolean shouldSkipTerm(String term)
  {
    if (term.length() < 2)
      return true;
    
    // Skip words with digits. We seldom want to correct with these,
    // and they introduce a big burden on indexing. Also, skip
    // all n-grams.
    //
    for (int i = 0; i < term.length(); i++) {
        char c = term.charAt(i);
        if (Character.isDigit(c))
            return true;
        if (c == '~')
            return true;
    }
    
    return false;
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
    pairHash = new HashMap(MAX_PAIR_HASH_SIZE);
  } // openQueueWriters()

  /** Closes the queue writer if it's open */
  private void closeQueueWriters() throws IOException {
    if (wordQueueWriter != null) {
        wordQueueWriter.close();
        wordQueueWriter = null;
    }
    if (pairQueueWriter != null) {
        flushPairHash();
        pairHash = null;
        pairQueueWriter.close();
        pairQueueWriter = null;
    }
  } // closeQueueWriters()

  /**
   * Opens the IndexReader, closing the IndexWriter if necessary. If the reader
   * can't be opened (for instance, if the index doesn't exist), the exception
   * is ignored and 'reader' is simply set to null.
   */
  private void openSpellIndexReader() throws IOException 
  {
    // If already open, do nothing.
    if (spellIndexReader != null)
        return;
    
    // If no directory yet, do nothing.
    if( spellIndexDir == null )
        return;

    // Close the writer, open the reader.
    closeSpellIndexWriter();
    try {
        if( IndexReader.indexExists(spellIndexDir) )
            spellIndexReader = IndexReader.open( spellIndexDir );
    } catch (IOException e) { }
  } // openIndexReader()

  /** Closes the IndexReader if it's open */
  private void closeSpellIndexReader() throws IOException 
  {
    if (spellIndexReader != null) {
        spellIndexReader.close();
        spellIndexReader = null;
    }
  } // closeIndexReader()

  /** Opens the IndexWriter, closing the IndexReader if necessary. */
  private void openSpellIndexWriter() throws IOException {
    // Close the reader if it's open.
    closeSpellIndexReader();
    
    // Open the writer now.
    spellIndexWriter = new IndexWriter(spellIndexDir, new WhitespaceAnalyzer(), 
        !IndexReader.indexExists(spellIndexDir));

    // Set some factors that will help speed things up.
    spellIndexWriter.mergeFactor = 300;
    spellIndexWriter.minMergeDocs = 150;
  } // openIndexWriter()

  /** Closes the IndexWriter if it's open */
  private void closeSpellIndexWriter() throws IOException {
    if (spellIndexWriter != null) {
        spellIndexWriter.close();
        spellIndexWriter = null;
    }
  } // closeIndexWriter()

  static int getMin (int l) {
      if (l>5) {
          return 3;
      }
      if (l==5) {
          return 2;
      }
      return 1;
  }


  static int getMax (int l) {
      if (l>5) {
          return 4;
      }
      if (l==5) {
          return 3;
      }
      return 2;
  }


  public static String calcMetaphone( String word )
  {
    String ret = doubleMetaphone.doubleMetaphone( word );
    
    /*
    String beg = word.substring(0, 1).toUpperCase();
    if( !ret.startsWith(beg) )
        ret = beg + ret;
    
    String end = word.substring(word.length()-1).toUpperCase();
    if( !ret.endsWith(end) )
        ret += end;
    */
    
    return ret;
  }


  static String removeDoubles(String text) {
    for (int i=0; i<text.length()-1; i++) {
        if (text.charAt(i) == text.charAt(i+1))
            text = text.substring(0, i) + text.substring(i+1);
    }
    return text;
  }
  
  
  static Document createDocument (String text, int ng1, int ng2) {
      Document doc=new Document();
      doc.add(new Field(F_WORD, text, true, true, false)); // orig term
      addGram(text, doc, ng1, ng2);
      addMetaphone(text, doc);
      return doc;
  }


  static void addGram (String text, Document doc, int ng1, int ng2) {
      text = removeDoubles(text);
      int len=text.length();
      for (int ng=ng1; ng<=ng2; ng++) {
          String key="gram"+ng;
          String end=null;
          for (int i=0; i<len-ng+1; i++) {
              String gram=text.substring(i, i+ng);
              doc.add(new Field(key, gram, false, true, false));
              if (i==0) {
                  doc.add(new Field("start"+ng, gram, false, true, false));
              }
              end=gram;
          }
          if (end!=null) { // may not be present if len==ng1
              doc.add(new Field("end"+ng, end, false, true, false));
          }
      }
  }


  static void addMetaphone (String text, Document doc) {
      String metaPhone = calcMetaphone(text);
      doc.add(new Field("metaphone", metaPhone, false, true, false));
  }


  protected void finalize() throws Throwable {
    close();
  }
  
  private static class IntHolder {
    public int value;
  }
} // class SpellWriter
