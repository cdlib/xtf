package org.apache.lucene.spelt;

/**
 * Copyright 2007 The Apache Software Foundation
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


// old: import org.apache.lucene.search.spell.Dictionary;
// old: import org.apache.lucene.search.spell.SpellChecker;
// old: import org.apache.lucene.store.Directory;
// old: import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.ProgressTracker;
import org.apache.lucene.util.StringUtil;

/**
 * A command-line driver class to test out the old and new spelling 
 * correction engines.
 * 
 * @author Martin Haye
 */
public class SpellTestCmdLine
{

  /**
   * Parse command line arguments and run.
   */
  public static void main(String[] args)
  {
    long startTime = System.currentTimeMillis();
   
    System.out.println("\n*** Spelt command-line test utility ***\n");
    if (args.length < 3)
      printUsageAndExit();
    
    try
    {
      int alg = 2; // Default to Spelt (new) algorithm
      
      // Parse all the arguments.
      for (int i=0; i<args.length; i++)
      {
        // Is the "old" (Lucene) algorithm requested?
// old:         if (args[i].equals("-old")) {
// old:           alg = 1;
// old:           continue;
// old:         }
        
        // Is it a "build" command?
        if (args[i].equals("-build") && i+3 <= args.length) {
          buildDictionary(alg, args[i+1], args[i+2]);
          i += 2;
        }
        
        // Is it a "test" command?
        else if (args[i].equals("-test") && i+3 <= args.length) {
          testDictionary(alg, args[i+1], args[i+2]);
          i += 2;
        }
        else
          printUsageAndExit();
      }
    }
    catch (IOException e) {
      System.out.flush();
      System.err.println("Unexpected exception during spellcheck: " + e);
      e.printStackTrace(System.err);
    }
    
    // Tell the user how long we spent building the dictionary.
    long elapsed = System.currentTimeMillis() - startTime;
    System.out.println("Done. Total time: " + (elapsed / 1000.0f) + " sec");
  }

  /**
   * Prints out a message saying how to use this tool, then exits.
   */
  private static void printUsageAndExit()
  {
    {
      System.err.println("Usage:  java -jar spelt.jar " +
                         // old: "{-old} " +
                         "-build <src-files-dir> <dictionary-dir>\n" +
                         "   or:  java -jar spelt.jar " +
                         // old: "{-old} " +
                         "-test <test-queries-file> <dictionary-dir>\n" +
                         "\n" +
                         " where  <test-queries-file> contains one or more lines like this\n" +
                         "        misspelled phrase\n" +
                         "   or   misspelled phrase -> correct phrase 1|correct phrase 2|...\n");
      System.exit(1);
    }
  }

  /**
   * Rip text from files in a given source directory and add them to a
   * spelling dictionary, using the specified algorithm.
   * 
   * @param alg     0 for null (to test speed of ripper);
   *                1 for the old Lucene algorithm;
   *                2 for the new Spelt algorithm.
   * @param srcDir  Directory to grab files to rip
   * @param dictDir Directory to put spelling dictionary in
   */
  private static void buildDictionary(int alg, String srcDir, String dictDir) 
    throws IOException
  { 
    
    // Clear all the files in the target dir.
    File dictDirFile = new File(dictDir);
    if (dictDirFile.isDirectory()) {
      for (File f : dictDirFile.listFiles())
        f.delete();
      if (!dictDirFile.delete())
        throw new IOException("Error deleting old dictionary from '" + dictDir + "'");
    }
    
    // Make a builder... what kind depends on the selected algorithm.
    DictBuilder builder = (alg == 0) ? new DictBuilder() /* does nothing */ :
                          //(alg == 1) ? new LuceneDictBuilder(dictDir) :
                          (alg == 2) ? new SpeltDictBuilder(dictDir) :
                          null;
    
    // Rip all the text.
    System.out.println("Ripping text and adding to dictionary...");
    TextRipper ripper = new TextRipper(srcDir);
    builder.add(ripper);
    
    // And finish up.
    System.out.println("Finishing dictionary...");
    ProgressTracker prog = new ProgressTracker() 
    {
      public void report(int pctDone, String descrip) {
        String pctTxt = Integer.toString(pctDone);
        while (pctTxt.length() < 3)
          pctTxt = " " + pctTxt;
        System.out.println("[" + pctTxt + "%] " + descrip);
      }
    };
    prog.setMinInterval(3000);
    builder.finish(prog);
  }
  
  /** Test the spelling index */
  private static void testDictionary(int alg, String testFile, String dictDir)
    throws IOException 
  {
    // Make a specific tester for the specified algorithm.
    SuggTester suggTester = //(alg == 1) ? new LuceneSuggTester(dictDir) :
                            (alg == 2) ? new SpeltSuggTester(dictDir) :
                            null;

    // Open the test query file and read each line.
    BufferedReader lineReader = new BufferedReader(new FileReader(testFile));
    int nTried = 0;
    int nCorrect = 0;
    while (true) 
    {
      // It should consist of phrase, a separator, and a correction
      String line = lineReader.readLine();
      if (line == null)
        break;

      // Strip off any comment, and skip blank lines.
      line = line.replaceFirst(";.*", "").trim();
      if (line.length() == 0)
        break;

      // Split into the incorrect phrase, and the correction(s)
      String[] parts = line.split("->");
      if (parts.length != 1 && parts.length != 2) {
        System.out.println("Unrecognized test line: " + line);
        continue;
      }
      String origPhrase = parts[0].trim();
      String[] correctPhrases = (parts.length == 1) ? null :
                                parts[1].trim().split("\\|");
      
      // Give feedback
      System.out.println("Incorrect phrase: " + origPhrase);

      // Break the original phrase into words, and get a suggestion.
      String[] origWords = StringUtil.splitWords(origPhrase);
      String[] suggWords = suggTester.suggest(origWords);
      String suggPhrase = StringUtil.join(suggWords);
      System.out.println("Suggested phrase: " + suggPhrase);
      nTried++;
      
      // If no correct versions available, we're done with this phrase.
      if (correctPhrases == null)
        continue;
      
      // Check against the correct versions.
      boolean found = false;
      for (String s : correctPhrases)
        found |= (s.trim().equalsIgnoreCase(suggPhrase));
      
      if (found) {
        System.out.println("--> CORRECT");
        nCorrect++;
        continue;
      }
      
      System.out.println("--> INCORRECT. Answer should have been:");
      for (String s : correctPhrases)
        System.out.println("    " + s);
    } // while

    System.out.println();
    System.out.printf("TOTAL: %d correct out of %d tried = %.1f%%\n", nCorrect, nTried, nCorrect * 100.0 / nTried);
    System.out.println();

    suggTester.close();
  }
  
  /** Create a default stop-word set */
  private static HashSet makeStopSet()
  {
    HashSet stopSet = new HashSet();
    String[] stopWords = StringUtil.splitWords(
        "a an and are as at be but by for if in into is it no not of on or s such t that the their then there these they this to was will with");
    for (int i=0; i<stopWords.length; i++)
      stopSet.add(stopWords[i]);
    return stopSet;
  }

  /** Common interface for various dictionary-building algorithms */
  private static class DictBuilder 
  {
    void add(Iterator words) throws IOException 
    {
      while (words.hasNext()) 
        words.next(); 
    }
    
    void finish(ProgressTracker prog) throws IOException { }
  }

  /** Builds an old-style Lucene spelling dictionary */
// old:   private static class LuceneDictBuilder extends DictBuilder
// old:   {
// old:     SpellChecker spellChecker;
// old:     
// old:     LuceneDictBuilder(String dictDir) throws IOException
// old:     {
// old:       Directory spellIndex = FSDirectory.getDirectory(new File(dictDir));
// old:       spellChecker = new SpellChecker(spellIndex);
// old:     }
// old:     
// old:     void add(final Iterator words) throws IOException
// old:     {
// old:       spellChecker.indexDictionary(new Dictionary() {
// old:         public Iterator getWordsIterator() {
// old:           return words;
// old:         }
// old:       });
// old:     }
// old:   }
  
  /** Builds a new-style Spelt spelling dictionary */
  private static class SpeltDictBuilder extends DictBuilder
  {
    SpellWriter spellWriter;

    SpeltDictBuilder(String dictDir) throws IOException
    {
      final int minWordFreq = 2;
      spellWriter = SpellWriter.open(dictDir, makeStopSet(), minWordFreq);
    }
    
    void add(Iterator words) throws IOException
    {
      while (words.hasNext())
        spellWriter.queueWord((String)words.next());
    }
    
    void finish(ProgressTracker prog) throws IOException
    {
      spellWriter.flushQueuedWords(prog);
      spellWriter.close();
    }

  }
  
  /**
   * Scans a directory for files, and rips text from all of them. The words
   * are accessible in the form of an Iterator.
   */
  private static class TextRipper implements Iterator
  {
    Stack fileStack = new Stack();
    BufferedReader reader;
    boolean more = true;
    String line;
    Matcher words = null;
    
    TextRipper(String dir) throws IOException 
    {
      fileStack.push(new File(dir));
      advance();
    }
    
    /**
     * Scan to the next file in the sequence, and open it.
     * 
     * @return true if there was a file to open
     */
    boolean nextFile() throws IOException
    {
      // Close the existing file, if any
      if (reader != null) {
        reader.close();
        reader = null;
      }

      // Scan until we find a file we can read.
      while (reader == null && !fileStack.isEmpty())
      {
        File file = (File) fileStack.pop();
        if (file.isFile()) {
          String path = file.getCanonicalPath();
          if (path.matches(".*\\.(xml|txt|text|html|htm|xhtml)"))
          {
            System.out.println(file);
            reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), "UTF-8"));
          }
        }
        else if (file.isDirectory()) {
          File[] subFiles = file.listFiles();
          for (int i=0; i<subFiles.length; i++)
              fileStack.push(subFiles[i]);
        }
      }
      
      return reader != null;
    }
    
    /** Pattern for matching words */
    Pattern wordPat = Pattern.compile("\\w+('\\w+)?");
    
    /**
     * Advance to the next word in the current file, or the next file if at the
     * end of the current one.
     */
    void advance() throws IOException
    {
      while (more)
      {
        // Get the next file if necessary
        if (reader == null) {
          if (!nextFile()) {
            more = false;
            break;
          }
        }
        
        // Get another line if necessary
        if (words == null || !words.find())
        {
          line = reader.readLine();
          if (line == null) {
            reader.close();
            reader = null;
            continue;
          }
          
          // Make a half-hearted attempt to strip out XML stuff
          line = stripXML(line);
          
          // And save the words to iterate.
          words = wordPat.matcher(line);
          continue;
        }

        // Got one!
        break;
      }
    }

    /** Pattern for matching XML and HTML elements */
    final Pattern xmlPat = Pattern.compile("<[^<]*>");
    
    /** Try to strip XML and HTML elements from a line */
    String stripXML(String line)
    {
      if (line.indexOf('<') < 0)
        return line;
      return xmlPat.matcher(line).replaceAll("");
    }

    /** Check if there's another word to get */
    public boolean hasNext() {
      return more;
    }

    /** Get the next word in the sequence */
    public Object next() {
      String ret = line.substring(words.start(), words.end());
      try {
        advance();
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
      return ret;
    }

    /** Not implemented */
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
  
  /**
   * Generic strategy for testing spelling suggestion algorithms
   */
  private interface SuggTester
  {
    String[] suggest(String[] origPhrase) throws IOException;
    void close() throws IOException;
  }
  
  /** Get spelling suggestions using Lucene (old) algorithm */
// old:   private static class LuceneSuggTester implements SuggTester
// old:   {
// old:     SpellChecker spellChecker;
// old:     
// old:     LuceneSuggTester(String dictDir) throws IOException
// old:     {
// old:       Directory spellIndex = FSDirectory.getDirectory(new File(dictDir));
// old:       spellChecker = new SpellChecker(spellIndex);
// old:     }
// old:     
// old:     public String[] suggest(String[] origPhrase) throws IOException
// old:     {
// old:       String[] ret = new String[origPhrase.length];
// old:       boolean anyChange = false;
// old:       for (int i=0; i<ret.length; i++) {
// old:         String[] suggs = spellChecker.suggestSimilar(origPhrase[i], 1);
// old:         if (suggs.length == 0)
// old:           ret[i] = origPhrase[i];
// old:         else {
// old:           ret[i] = suggs[0]; 
// old:           anyChange |= !WordEquiv.DEFAULT.isEquivalent(ret[i], origPhrase[i]);
// old:         }
// old:       }
// old:       if (!anyChange)
// old:         return null;
// old:       return ret;
// old:     }
// old:     
// old:     public void close() { }
// old:   }
  
  /** Get spelling suggestions using the Spelt (new) algorithm */
  private static class SpeltSuggTester implements SuggTester
  {
    SpellReader spellReader;
    
    SpeltSuggTester(String dictDir) throws IOException
    {
      spellReader = SpellReader.open(new File(dictDir), 
                                     makeStopSet(), 
                                     WordEquiv.DEFAULT);
    }
    
    public String[] suggest(String[] origPhrase) throws IOException
    {
      return spellReader.suggestKeywords(origPhrase);
    }
    
    public void close() throws IOException 
    {
      spellReader.close();
    }
  }
}
