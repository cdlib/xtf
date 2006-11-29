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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.cdlib.xtf.util.CountedInputStream;
import org.cdlib.xtf.util.FastStringCache;

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
  private String indexPath;
  
  /** Lucene Directory of the spelling dictionary */
  private Directory indexDir;
  
  /** File to queue words into */
  private File queueFile;

  /** How large to make the cache of recently added words */
  private int cacheSize = 20000;

  /** Cache of recently added words (so we don't look them up twice.) */
  private FastStringCache recentWords = new FastStringCache(cacheSize);

  /** Boolean object to mark words in cache */
  private static final Boolean trueValue = new Boolean(true);

  /** Max # of queued words to process at a time */
  private int blockSize = 50000;
  
  /** Number of words added during flush */
  private int totalAdded;
  
  /** For writing to the word queue */
  private ObjectOutputStream queueWriter = null;

  /** For checking term existence */
  private IndexReader indexReader;

  /** For writing new terms */
  private IndexWriter indexWriter;

  /** Used for calculating double metaphone keys */
  private static DoubleMetaphone doubleMetaphone = new DoubleMetaphone();
    
  /** 
   * Establishes the directory to store the dictionary in. 
   */
  public synchronized void open( String indexPath ) 
    throws IOException 
  {
    this.indexPath = new File(indexPath).getAbsolutePath();
    this.queueFile = new File(indexPath + "newWords.dat");
    
    // Open the index directory, if it exists. If not, create it.
    if (new File(indexPath).isDirectory()) {
        indexDir  = FSDirectory.getDirectory(indexPath, false);
        IndexReader.unlock( indexDir );
    }
    else
        indexDir = FSDirectory.getDirectory( indexPath, true );
  }

  /** 
   * Closes all files. Does NOT write queued words (they stay queued on
   * disk.) 
   */
  public synchronized void close() throws IOException {
    closeQueueWriter();
    closeIndexReader();
    closeIndexWriter();
  }

  /** Delete all words in the dictionary (including those queued on disk) */
  public synchronized void clearIndex() throws IOException {
    close();
    queueFile.delete();
    recentWords.clear();
    if (indexDir != null) {
        IndexReader.unlock(indexDir);
        IndexWriter writer = new IndexWriter(indexDir, null, true);
        writer.close();
    }
  }

  /**
   * Queue the given word if not recently checked. Caller should periodically
   * call checkFlush() to see whether the queue is getting full. The queue will
   * auto-expand if necessary, but it's better to flush it when near full.
   */
  public synchronized void queueWord(String word) throws IOException {
    if (recentWords.contains(word))
      return;
    recentWords.put(word, trueValue);
    
    openQueueWriter();
    queueWriter.writeUTF(word);
  } // queueWord()

  /** Check if any words are queued for add. */
  public synchronized boolean anyWordsQueued() throws IOException {
    closeQueueWriter();
    long queueSize = queueFile.length();
    return queueSize > 1;
  }

  /** 
   * Called periodically during the flush process. Override to output status
   * messages.
   */
  public void progress(int percentDone, int totalAdded) {
    // Default: do nothing.
  }
  
  /**
   * Ensures that all words in the queue are written to the dictionary on disk.
   * 
   * @return number of non-duplicate words actually written
   */
  public synchronized int flushQueuedWords() throws IOException {
    closeQueueWriter();
    
    InputStream queueRaw = null;
    try {
        queueRaw = new BufferedInputStream(new FileInputStream(queueFile));
    }
    catch( IOException e ) {
        return 0;
    }
    
    CountedInputStream queueCounted = new CountedInputStream(queueRaw);
    ObjectInputStream  queueReader  = new ObjectInputStream(queueCounted);
    
    long fileTotal = queueFile.length();
    int prevPctDone = 0;
    totalAdded = 0;

    try {
        boolean eof = false;
        while( !eof ) {
          
            // Read in a block of words.
            //System.out.print(" r");
            
            ArrayList block = new ArrayList(blockSize);
            
            try {
                while (block.size() < blockSize && !eof) {
                    String word = queueReader.readUTF();
                    block.add(word);
                }
            }
            catch (EOFException e) { eof = true; }
        
            // Calculate the percent that will be done after this block.
            long filePos = queueCounted.nRead();
            int pctDone = (int) ((filePos+1) * 100 / (fileTotal+1));
            
            // Add the block.
            addBlock(block, prevPctDone, pctDone);
        } // while
    }
    finally {
        queueReader.close();
        queueCounted.close();
        queueRaw.close();
        closeIndexWriter();
    }
    
    // Now that we've added them, clear the queue (truncate the file). Try
    // to delete it as well.
    //
    FileOutputStream tmp = new FileOutputStream(queueFile);
    tmp.close();
    queueFile.delete();

    // All done.
    return totalAdded;
  } // flushQueuedWords()

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
    openIndexReader();
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
      if (indexReader != null && indexReader.docFreq(new Term(F_WORD, word)) > 0)
        continue;

      // Okay, got a live one. Mark it for addition to the index.
      wordsToAdd.set(i);
    } // for i

    // Now that we've identified the words that aren't yet in our index,
    // add them one at a time.
    //
    //System.out.print("w");
    openIndexWriter();
    int nTotal = block.size();
    for (int i = 0; i < nTotal; i++) {
      // Only index words that have been marked to add.
      if (!wordsToAdd.get(i))
        continue;

      String word = (String) block.get(i);
      int len = word.length();

      indexWriter.addDocument(createDocument(word, getMin(len), getMax(len)));
      ++totalAdded;
      
      // Every 1000 words or so, update the progress counter 
      if( (i & 0x3FF) == 0 ) {
          int pctDone = (i * (pct2 - pct1) / nTotal) + pct1;
          progress( pctDone, totalAdded );
      }
    }
    
    // Finish progress counter
    progress( pct2, totalAdded );
    
    // Optimize the index so that future access (and block adds) are fast.
    //System.out.print( "o " );
    indexWriter.optimize();
  } // addBlock()
    
  /** Opens the queue writer, closing the queue reader if necessary. */
  private void openQueueWriter() throws IOException {
    // If already open, skip re-opening.
    if (queueWriter != null)
        return;

    // Open the writer now. Be sure to append if it already exists.
    queueWriter = new ObjectOutputStream(new BufferedOutputStream(
                           new FileOutputStream(queueFile, true)));
  } // openQueueWriter()

  /** Closes the queue writer if it's open */
  private void closeQueueWriter() throws IOException {
    if (queueWriter != null) {
        queueWriter.close();
        queueWriter = null;
    }
  } // closeQueueWriter()

  /**
   * Opens the IndexReader, closing the IndexWriter if necessary. If the reader
   * can't be opened (for instance, if the index doesn't exist), the exception
   * is ignored and 'reader' is simply set to null.
   */
  private void openIndexReader() throws IOException 
  {
    // If already open, do nothing.
    if (indexReader != null)
        return;
    
    // If no directory yet, do nothing.
    if( indexDir == null )
        return;

    // Close the writer, open the reader.
    closeIndexWriter();
    try {
        if( IndexReader.indexExists(indexDir) )
            indexReader = IndexReader.open( indexDir );
    } catch (IOException e) { }
  } // openIndexReader()

  /** Closes the IndexReader if it's open */
  private void closeIndexReader() throws IOException 
  {
    if (indexReader != null) {
        indexReader.close();
        indexReader = null;
    }
  } // closeIndexReader()

  /** Opens the IndexWriter, closing the IndexReader if necessary. */
  private void openIndexWriter() throws IOException {
    // Close the reader if it's open.
    closeIndexReader();
    
    // Open the writer now.
    indexWriter = new IndexWriter(indexDir, new WhitespaceAnalyzer(), 
        !IndexReader.indexExists(indexDir));

    // Set some factors that will help speed things up.
    indexWriter.mergeFactor = 300;
    indexWriter.minMergeDocs = 150;
  } // openIndexWriter()

  /** Closes the IndexWriter if it's open */
  private void closeIndexWriter() throws IOException {
    if (indexWriter != null) {
        indexWriter.close();
        indexWriter = null;
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
    
    String beg = word.substring(0, 1).toUpperCase();
    if( !ret.startsWith(beg) )
        ret = beg + ret;
    
    String end = word.substring(word.length()-1).toUpperCase();
    if( !ret.endsWith(end) )
        ret += end;
    
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
      //addTrans(text, doc);
      addMetaphone(text, doc);
      //addDrop(text, doc);
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


  static void addTrans (String text, Document doc) {
      final int len=text.length();
      final char[] ch = text.toCharArray();
      final String key="trans";

      char tmp;
      for (int i=0; i<len-1; i++) {
          tmp = ch[i];
          ch[i] = ch[i+1];
          ch[i+1] = tmp;
          
          doc.add(new Field(key, new String(ch), false, true, false));
          
          tmp = ch[i];
          ch[i] = ch[i+1];
          ch[i+1] = tmp;
      }
  }


  static void addMetaphone (String text, Document doc) {
      String metaPhone = calcMetaphone(text);
      doc.add(new Field("metaphone", metaPhone, false, true, false));
  }


  static void addDrop (String text, Document doc) {
      final int len=text.length();
      final String key="drop";
      for (int i=0; i<len; i++) {
          String dropped = text.substring(0, i) + 
                           text.substring(Math.min(len, i+1));
          if( dropped.length() > 0 )
              doc.add(new Field(key, dropped, false, true, false));
      }
  }


  protected void finalize() throws Throwable {
    close();
  }
} // class SpellWriter
