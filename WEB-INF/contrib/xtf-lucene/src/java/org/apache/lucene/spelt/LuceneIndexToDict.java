package org.apache.lucene.spelt;

/*
 * Copyright 2007 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.ProgressTracker;

/**
 * Utility class to convert the stored fields of a Lucene index into a spelling
 * dictionary. This is generally less desirable than integrating dictionary
 * creation into the original index creation process (e.g. using
 * {@link SpellWritingAnalyzer} or {@link SpellWritingFilter}) since that will
 * grab non-stored as well as stored fields. Still, if that isn't an option or
 * if you simply want to test out spelling correction, after-the-fact dictionary
 * creation may be useful.
 * 
 * @author Martin Haye
 */
public class LuceneIndexToDict
{
  /**
   * Read a Lucene index and make a spelling dictionary from it. A minimal token
   * analyzer will be used, which is usually just what is needed for the
   * dictionary. The default set of English stop words will be used (see
   * {@link StopAnalyzer#ENGLISH_STOP_WORDS}).
   * 
   * @param indexDir directory containing the Lucene index
   * @param dictDir directory to receive the spelling dictionary
   */
  public static void createDict(Directory indexDir, File dictDir)
    throws IOException
  {
    createDict(indexDir, dictDir, null);
  }

  /**
   * Read a Lucene index and make a spelling dictionary from it. A minimal token
   * analyzer will be used, which is usually just what is needed for the
   * dictionary. The default set of English stop words will be used (see
   * {@link StopAnalyzer#ENGLISH_STOP_WORDS}).
   * 
   * @param indexDir directory containing the Lucene index
   * @param dictDir directory to receive the spelling dictionary
   * @param prog tracker called periodically to display progress
   */
  public static void createDict(Directory indexDir, 
                                File dictDir, 
                                ProgressTracker prog)
    throws IOException
  {
    // Open and clear the dictionary (since we're going to totally rebuild it)
    SpellWriter spellWriter = SpellWriter.open(dictDir);
    spellWriter.clearDictionary();
    spellWriter.setStopwords(StopFilter.makeStopSet(StopAnalyzer.ENGLISH_STOP_WORDS));
    
    // Now re-tokenize all the fields and queue the words for the dictionary.
    IndexReader indexReader = IndexReader.open(indexDir);
    createDict(indexReader, new MinimalAnalyzer(), spellWriter, prog);
    
    // All done.
    spellWriter.close();
  }

  /**
   * Read a Lucene index and make a spelling dictionary from it. A minimal token
   * analyzer will be used, which is usually just what is needed for the
   * dictionary. The default set of English stop words will be used (see
   * {@link StopAnalyzer#ENGLISH_STOP_WORDS}).
   * 
   * @param indexReader used to read fields from a Lucene index
   * @param analyzer used to tokenize fields from the index; generally,
   *          this should do minimal filtering, taking care to avoid substantive
   *          token modification (such as stemming or depluralization). A good
   *          choice is {@link MinimalAnalyzer}.
   * @param spellWriter receives words to be added to the dictionary
   * @param prog tracker called periodically to display progress
   */
  public static void createDict(IndexReader indexReader,
                                Analyzer analyzer,
                                SpellWriter spellWriter,
                                ProgressTracker prog)
    throws IOException
  {
    // Supply a null progress tracker if none supplied.
    if (prog == null) {
      prog = new ProgressTracker() {
        public @Override void report(int pctDone, String descrip) { }
      };
    }
    
    // Split into phases. Seems like the re-analysis takes a lot longer than
    // dictionary creation.
    //
    ProgressTracker[] phaseTrackers = prog.split(70, 30);
      
    // Now re-tokenize all the fields and queue the words for the dictionary.
    queueWords(indexReader, analyzer, spellWriter, phaseTrackers[0]);
    indexReader.close();
    
    // Perform the final dictionary creation
    spellWriter.flushQueuedWords(phaseTrackers[1]);
  }

  /**
   * Re-tokenize all the words in stored fields within a Lucene index,
   * and queue them to a spelling dictionary. Does not flush the writer
   * to form the final dictionary, so could be called repeatedly to
   * queue words from multiple Lucene indexes.
   * 
   * @param reader used to read fields from a Lucene index
   * @param analyzer used to tokenize fields from the index; generally,
   *          this should do minimal filtering, taking care to avoid substantive
   *          token modification (such as stemming or depluralization). A good
   *          choice is {@link MinimalAnalyzer}.
   * @param writer receives words to be added to the dictionary
   * @param prog tracker called periodically to display progress
   */
  public static void queueWords(IndexReader reader, Analyzer analyzer,
    SpellWriter writer, ProgressTracker prog) throws IOException
  {
    // Iterate every document in the source index
    for (int docId = 0; docId < reader.maxDoc(); docId++) 
    {
      // Give periodic feedback.
      if ((docId & 0xff) == 0)
        prog.progress(docId, reader.maxDoc(), "Re-analyzed " + docId + " documents.");

      // Skip deleted documents
      if (reader.isDeleted(docId))
        continue;
      
      // Get the document.
      Document doc = reader.document(docId);
      if (doc == null)
        continue;

      // Iterate every stored field in the document.
      for (Field field : (List<Field>) doc.getFields()) 
      {
        // Skip fields that aren't tokenized.
        if (!field.isTokenized())
          continue;
        
        // Iterate every value of that field.
        String[] values = doc.getValues(field.name());
        if (values == null)
          continue;

        for (String val : values) {
          // Add each word to the dictionary.
          TokenStream toks = analyzer.tokenStream(field.name(),
              new StringReader(val));
          Token tok;
          while ((tok = toks.next()) != null)
            writer.queueWord(tok.termText());
          writer.queueBreak();
        }
      }
    }
    
    // Force the final progress message.
    prog.progress(100, 100, "Re-analyzed " + reader.maxDoc() + " documents.", true);
  }
  
  /**
   * Command-line interface for build a dictionary directly from a Lucene index
   * without writing any code.
   */
  public static void main(String[] args)
  {
    if (args.length != 2) {
      System.err.println("Usage: ... LuceneIndexToDict <luceneIndexDir> <targetDictDir>");
      System.exit(1);
    }
    
    System.out.println("\n*** Lucene to dictionary conversion utility ***\n");
    
    IndexReader indexReader = null;
    SpellWriter spellWriter = null;
    int exitVal = 1;
    
    try
    {
      File indexDir = new File(args[0]);
      File dictDir = new File(args[1]);
      
      // We'll want to print out status messages periodically.
      final long startTime = System.currentTimeMillis();
      ProgressTracker prog = new ProgressTracker() { 
        public void report(int pctDone, String descrip) {
          System.out.println(String.format("%6.1f sec  [%3d%%]  %s",
            (System.currentTimeMillis() - startTime) / 1000.0f,
            pctDone,
            descrip));
        }
      };
      prog.setMinInterval(3000);
      
      // Go for it.
      createDict(FSDirectory.getDirectory(indexDir), dictDir, prog);
      exitVal = 0;
    }
    catch (IOException e) {
      System.out.flush();
      System.err.println("Unexpected exception: " + e);
      e.printStackTrace(System.err);
    }
    finally 
    {
      try {
        if (indexReader != null)
          indexReader.close();
        if (spellWriter != null)
          spellWriter.close();
      }
      catch (IOException e) { }
    }
    
    System.exit(exitVal);
  }
}
