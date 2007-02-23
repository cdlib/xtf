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
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import org.apache.lucene.bigram.BigramQueryRewriter;
import org.apache.lucene.chunk.DocNumMap;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.spell.SpellReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.cdlib.xtf.util.CharMap;
import org.cdlib.xtf.util.WordMap;

/**
 * Used to keep a set of searcher, reader, and doc-num-map that are consistent
 * with each other and also up-to-date.
 *
 * @author Martin Haye
 */
public class XtfSearcher 
{
  /** Path to the index directory */
  private String indexPath;

  /** The index directory to read from */
  private Directory directory;

  /** How often to check for an out-of-date directory */
  private long updatePeriod;

  /** Last time we checked for out-of-date */
  private long lastCheckTime;

  /** Version number of the index last time we checked */
  private long curVersion;

  /** Reader used to access the index */
  private IndexReader indexReader;

  /** Keeps track of which chunks belong to which documents */
  private DocNumMap docNumMap;

  /** Fetching spelling suggestions */
  private SpellReader spellReader;

  /** Max # of words in a chunk */
  private int chunkSize;

  /** Amount of overlap, in words, between adjacent chunks */
  private int chunkOverlap;

  /** Stop-words associated with the index (e.g. "the", "a", "and", etc.) */
  private Set stopSet;

  /** Map of plural words to singular words */
  private WordMap pluralMap;

  /** Map of accented chars to remove diacritics from */
  private CharMap accentMap;

  /** Set of all indexed fields in the index */
  private Set indexedFields;

  /** Set of all fields which are tokenized in the index */
  private Set tokenizedFields;

  /** Whether this index is "sparse" (i.e. more than 5 chunks per doc) */
  private boolean isSparse;

  /**
   * Construct a searcher set on the given directory.
   *
   * @param indexPath             Directory to load index data from
   * @param updateCheckSeconds    How often to check for an updated index
   */
  public XtfSearcher(String indexPath, int updateCheckSeconds)
    throws IOException 
  {
    this.indexPath = indexPath;
    directory = FSDirectory.getDirectory(indexPath);
    curVersion = -99;
    updatePeriod = ((long)updateCheckSeconds) * 1000;
    update();
  } // XtfSearcher

  /**
   * Ensures that this searcher is up-to-date with regards to the index on
   * disk.
   */
  public void update()
    throws IOException 
  {
    // It would be too time-consuming to read the index version number
    // every single time. So only check it periodically.
    //
    if (curVersion >= 0) {
      long time = System.currentTimeMillis();
      if (time - lastCheckTime < updatePeriod)
        return;
      lastCheckTime = time;
    }

    // Get the version on disk. If it's the same as the one we have in
    // memory, no problem.
    //
    long ver = IndexReader.getCurrentVersion(directory);
    if (ver == curVersion)
      return;

    // Okay, better re-open to get the fresh data.
    close();
    indexReader = IndexReader.open(directory);

    // Fetch the chunk size and overlap from the index.
    Hits match = new IndexSearcher(indexReader).search(
      new TermQuery(new Term("indexInfo", "1")));
    if (match.length() == 0)
      throw new IOException("Index missing indexInfo doc");
    assert match.id(0) == 0 : "indexInfo chunk must be first in index";
    Document doc = match.doc(0);

    chunkSize = Integer.parseInt(doc.get("chunkSize"));
    chunkOverlap = Integer.parseInt(doc.get("chunkOvlp"));

    if (chunkSize <= 0 || chunkOverlap <= 0 || chunkOverlap >= chunkSize)
      throw new IOException("Invalid chunkSize/overlap in index");

    // Construct a map from doc # to chunk #'s (and vice-versa)
    docNumMap = new XtfDocNumMap(indexReader, chunkSize, chunkOverlap);

    // Get the stop-word set.
    String stopWords = doc.get("stopWords");
    stopSet = null;
    if (stopWords != null && stopWords.length() > 0)
      stopSet = BigramQueryRewriter.makeStopSet(stopWords);

    // If there's a plural map specified, load it.
    String pluralMapName = doc.get("pluralMap");
    if (pluralMapName != null && pluralMapName.length() > 0) {
      File pluralFile = new File(indexPath, pluralMapName);
      InputStream stream = new FileInputStream(pluralFile);
      if (pluralMapName.endsWith(".gz"))
        stream = new GZIPInputStream(stream);
      pluralMap = new WordMap(stream);
    }

    // If there's an accent map specified, load it.
    String accentMapName = doc.get("accentMap");
    if (accentMapName != null && accentMapName.length() > 0) {
      File accentFile = new File(indexPath, accentMapName);
      InputStream stream = new FileInputStream(accentFile);
      if (accentMapName.endsWith(".gz"))
        stream = new GZIPInputStream(stream);
      accentMap = new CharMap(stream);
    }

    // If there's a spelling correction dictionary, attach to it.
    File spellDir = new File(indexPath, "spellDict");
    if (SpellReader.isValidDictionary(spellDir))
      spellReader = SpellReader.open(spellDir,
                                     stopSet,
                                     new XtfWordEquiv(accentMap, pluralMap));

    // Determine whether this is a "sparse" index. Our definition of
    // sparse is that there are more than 5 chunks per document, meaning
    // that meta-data sorting and grouping will waste a lot of memory
    // if they allocate a slot per chunk.
    //
    int nDocs = indexReader.docFreq(new Term("docInfo", "1"));
    int nChunks = indexReader.maxDoc();
    isSparse = nChunks > (nDocs * 5);

    // Determine the list of all fields.
    indexedFields = new LinkedHashSet(
      indexReader.getFieldNames(IndexReader.FieldOption.ALL));

    // Determine which fields are tokenized.
    tokenizedFields = new LinkedHashSet();
    TermEnum tokTerms = indexReader.terms(new Term("tokenizedFields", ""));
    do {
      Term t = tokTerms.term();
      if (t == null)
        break;
      if (!t.field().equals("tokenizedFields"))
        break;
      tokenizedFields.add(t.text());
    } while (tokTerms.next());

    // Of course, the "text" field is always tokenized.
    tokenizedFields.add("text");

    // Remember the version that we've checked.
    curVersion = ver;
  } // update()

  /**
   * Get the list of all tokenized fields.
   */
  public Set tokenizedFields() {
    return tokenizedFields;
  }

  /**
   * Gets the reader this searcher is using to read indexes.
   */
  public IndexReader indexReader() {
    return indexReader;
  }

  /**
   * Gets the set of all fields that have been indexed.
   */
  public Set indexedFields() {
    return indexedFields;
  }

  /**
   * Gets a map for translating chunk IDs to document IDs (and vice-versa)
   */
  public DocNumMap docNumMap() {
    return docNumMap;
  }

  /**
   * Find out how many words (max) are in a chunk.
   */
  public int chunkSize() {
    return chunkSize;
  }

  /**
   * Find out how many words adjacent chunks can overlap.
   */
  public int chunkOverlap() {
    return chunkOverlap;
  }

  /**
   * Find out the set of stop words, or null if none.
   */
  public Set stopSet() {
    return stopSet;
  }

  /**
   * Find out the plural mapping, or null if none.
   */
  public WordMap pluralMap() {
    return pluralMap;
  }

  /**
   * Find out the accent mapping, or null if none.
   */
  public CharMap accentMap() {
    return accentMap;
  }

  public SpellReader spellReader() {
    return spellReader;
  }

  /**
   * Find out if the index is sparse (i.e. more than 5 chunks per doc)
   */
  public boolean isSparse() {
    return isSparse;
  }

  /**
   * Close down the searcher and all its dependencies.
   */
  public void close()
    throws IOException 
  {
    docNumMap = null;

    if (indexReader != null) {
      indexReader.close();
      indexReader = null;
    }

    if (spellReader != null) {
      spellReader.close();
      spellReader = null;
    }

    curVersion = -99;
  } // close()
} // class XtfSearcher
