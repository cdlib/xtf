package org.apache.lucene.search.spell;


/*
 * Copyright (c) 2006, Regents of the University of California
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
 *
 * Acknowledgements:
 *
 * A significant amount of new and/or modified code in this module
 * was made possible by a grant from the Andrew W. Mellon Foundation,
 * as part of the Melvyl Recommender Project.
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import org.cdlib.xtf.util.ProgressTracker;

/**
 * Automated test of spelling correction to determine its overall accuracy.
 *
 * @author Martin Haye
 */
public class SpellTest 
{
  private String stopAt = null; //"b";

  /**
   * Command-line driver.
   */
  public static void main(String[] args)
    throws IOException 
  {
    if (args.length == 2 && args[0].equals("-build"))
      new SpellTest().build(args[1]);
    else if (args.length == 2 && args[0].equals("-test"))
      new SpellTest().test(args[1]);
    else {
      System.err.println("Usage: {-build wordfile}|{-test pairfile}\n");
      System.exit(1);
    }
  } // main()

  /**
   * Build a spelling index using a word frequency file. The file should
   * be sorted by descending frequency.
   */
  private void build(String wordFile)
    throws IOException 
  {
    // Read in each line
    System.out.println("Reading word file...");
    BufferedReader reader = new BufferedReader(new FileReader(wordFile));
    ArrayList<Pair> pairList = new ArrayList<Pair>();
    while (true) 
    {
      // It should consist of a word, a space, and the frequency
      String line = reader.readLine();
      if (line == null)
        break;
      int spacePos = line.indexOf(' ');
      assert spacePos >= 0;

      String word = line.substring(0, spacePos);
      int freq = Integer.parseInt(line.substring(spacePos + 1));

      // Clamp frequencies greater than 10000
      if (freq > 10000)
        freq = 10000;

      // For testing, do only part of the dictionary.
      if (stopAt != null && word.compareTo(stopAt) >= 0)
        continue;

      // Skip terms that have digits and weird characters.
      int i;
      for (i = 0; i < word.length(); i++) {
        char c = word.charAt(i);
        if (Character.isDigit(c) || c == '%')
          break;
      }
      if (i < word.length())
        continue;

      // Add the pair to our list
      Pair pair = new Pair();
      pair.word = word;
      pair.freq = freq;
      pairList.add(pair);
    } // while
    reader.close();

    // Construct the spelling index.
    writeSpellIndex(pairList);
  } // build()

  /**
   * Constructs a spelling index based on all the words in the list.
   */
  private void writeSpellIndex(ArrayList<Pair> pairList)
    throws IOException 
  {
    System.out.println("Writing spell index...");

    long startTime = System.currentTimeMillis();
    SpellWriter spellWriter = SpellWriter.open("spell", null, 1);
    spellWriter.clearIndex();

    // Add each word the specified number of times.
    for (int i = 0; i < pairList.size(); i++) 
    {
      Pair pair = pairList.get(i);
      for (int j = 0; j < (pair.freq + 1); j++) {
        spellWriter.queueBreak(); // we don't have any real colocation data
        spellWriter.queueWord(pair.word);
      }
    }

    // Now comes the bulk of the work
    ProgressTracker prog = new ProgressTracker() 
    {
      public void report(int pctDone, String descrip) {
        String pctTxt = Integer.toString(pctDone);
        while (pctTxt.length() < 3)
          pctTxt = " " + pctTxt;
        System.out.println("[" + pctTxt + "%] " + descrip);
      }
    };
    prog.setMinInterval(1000);
    spellWriter.flushQueuedWords(prog);
    spellWriter.close();

    System.out.println(
      "Done. Total time: " + (System.currentTimeMillis() - startTime) +
      " msec");
  } // writeSpellIndex()

  static String topSugg = null;

  /** Test the spelling index */
  private void test(String testFile)
    throws IOException 
  {
    long startTime = System.currentTimeMillis();

    // Open the spelling index.
    SpellReader spellReader = SpellReader.open(new File("spell"), null, null);

    // Open the debug stream.
    PrintWriter debugWriter = new PrintWriter(new FileWriter("spellDebug.log"));
    spellReader.setDebugWriter(debugWriter);

    final int[] sizes = { 1, 5, 10, 25, 50 };

    //final int[] sizes = { 1 };
    final int[] totals = new int[sizes.length];
    int nWords = 0;

    NumberFormat fmt = DecimalFormat.getInstance();
    fmt.setMaximumFractionDigits(1);

    // Open the file and read each line.
    BufferedReader lineReader = new BufferedReader(new FileReader(testFile));
    ArrayList<String> missed = new ArrayList<String>();
    while (true) 
    {
      // It should consist of a word, a separator, and a correction
      String line = lineReader.readLine();
      if (line == null)
        break;

      String word;
      String correction;

      int arrowPos = line.indexOf("->");
      if (arrowPos > 0) 
      {
        word = line.substring(0, arrowPos).trim();
        correction = line.substring(arrowPos + 2).trim();
      }
      else {
        int spacePos = line.indexOf('\t');
        if (spacePos > 0) {
          word = line.substring(0, spacePos).trim();
          correction = line.substring(spacePos + 1).trim();
        }
        else {
          System.out.println("Unrecognized test line: " + line);
          continue;
        }
      }

      word = word.toLowerCase();
      correction = correction.toLowerCase();

      // If equal, skip (this can happen for uppercase -> lowercase)
      if (word.equals(correction))
        continue;

      // For testing, stop early.
      if (stopAt != null && word.compareTo(stopAt) >= 0)
        continue;

      // Skip if the target word isn't in our dictionary.
      if (!spellReader.inDictionary(correction))
        continue;

      // Bump the word count.
      ++nWords;

      // Get some suggestions.
      debugWriter.println("orig=" + word + ", lookFor=" + correction);
      int pos = trySpell(spellReader, word, correction, sizes[sizes.length - 1]);
      debugWriter.println();

      // Increment the correct totals.
      if (pos < 0)
        missed.add(word);
      else 
      {
        for (int j = 0; j < sizes.length; j++) {
          if (pos < sizes[j])
            totals[j]++;
        }
      }

      if (pos != 0) {
        System.out.print(word + " " + correction);
        System.out.print(" " +
                         new TRStringDistance(word).getDistance(correction));
        System.out.println(" " + topSugg + " " + (pos + 1));
      }

      if (nWords == 1)
        debugWriter.println(
          "Time to first word: " + (System.currentTimeMillis() - startTime) +
          " msec");
    } // while

    System.out.println();
    double pctMissed = missed.size() * 100.0 / nWords;
    System.out.print("Missed " + fmt.format(pctMissed) + "%: ");
    for (int i = 0; i < missed.size(); i++)
      System.out.print(missed.get(i) + " ");
    System.out.println();

    System.out.println();
    System.out.print("**TOTAL** ");
    for (int i = 0; i < sizes.length; i++) {
      double pct = totals[i] * 100.0 / nWords;
      System.out.print(" " + fmt.format(pct));
    }
    System.out.println();

    System.out.println(
      "Done. Total time: " + (System.currentTimeMillis() - startTime) +
      " msec");

    lineReader.close();
    spellReader.close();
    debugWriter.close();
  } // test()

  /**
   * Gather the specified number of suggestions for a word, and return the
   * position of the correction in the result list.
   */
  private int trySpell(SpellReader spellReader, String word, String correction,
                       int nSuggestions)
    throws IOException 
  {
    String[] suggestions = spellReader.suggestSimilar(word, nSuggestions);

    int found = -1;
    topSugg = null;
    for (int i = 0; i < suggestions.length; i++) 
    {
      if (i == 0)
        topSugg = suggestions[i];
      if (suggestions[i].equals(correction)) {
        found = i;
        break;
      }
    } // for i

    return found;
  } // trySpell()

  /** Records a token/frequency pair */
  private class Pair {
    String word;
    int freq;
  } // class Pair
} // class SpellTest
