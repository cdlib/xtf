package org.apache.lucene.spelt;

/**
 * Copyright (c) 2007, Regents of the University of California
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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;

import org.apache.lucene.util.StringUtil;

import junit.framework.TestCase;

/** 
 * Test the {@link SpellReader} and {@link SpellWriter} classes 
 *
 * @author Martin Haye
 */
public class SpellReadWriteTest extends TestCase
{
  private File dictDir;
  private SpellReader reader;
  private PrintWriter debugWriter;
  
  static final HashSet STOP_SET = new HashSet();
  static {
    for (String word : StringUtil.splitWords(
        "a an and are as at be but by for if in into is it no not of on or s " +
        "such t that the their then there these they this to was will with"))
      STOP_SET.add(word);
  }
  
  /** Create the temporary spelling dictionary */
  protected @Override void setUp() throws Exception 
  {
    super.setUp();
    
    // Create a spelling dictionary to test with
    dictDir = File.createTempFile("SpellReadWriteTest", null);
    dictDir.delete(); // Get rid of normal file, so we can make directory
    
    if (dictDir.isDirectory())
      for (File f : dictDir.listFiles()) f.delete();
    dictDir.delete();
    
    SpellWriter writer = SpellWriter.open(dictDir);
    writer.setStopwords(STOP_SET);
    writer.setMinWordFreq(1);
    
    try 
    {
      // Simplest possible tokenization
      for (String word : CALL_OF_THE_WILD.split("\\W+"))
        writer.queueWord(word);
      writer.flushQueuedWords();
      
      // Open a reader for the tests to use.
      reader = SpellReader.open(dictDir);
      reader.setStopwords(STOP_SET);
      
      // For debugging purposes, this gives a view into the guts.
      debugWriter = new PrintWriter(new FileWriter(
          new File(dictDir, "spellDebug.log")));
      reader.setDebugWriter(debugWriter);
    }
    finally {
      writer.close();
    }
  }
  
  /** Blow away the temporary spelling dictionary */
  protected @Override void tearDown() throws IOException
  {
    reader.close();
    debugWriter.close();
    if (dictDir.isDirectory()) {
      for (File f : dictDir.listFiles())
        f.delete();
      dictDir.delete();
    }
  }
  
  /** Test out single-word replacements */
  public void testSingleWords() throws IOException
  {
    // First, test some words that shouldn't get corrected
    checkSuggestion("London", null);
    checkSuggestion("newspapers", null);
    checkSuggestion("asdfkjlh", null);
    
    // Also make sure stop words don't result in suggestions
    checkSuggestion("the", null);
    checkSuggestion("and", null);
    
    // Okay, let's try some things that should get a suggestion.
    checkSuggestion("newpapers", "newspapers");
    checkSuggestion("newspaper", "newspapers");
    checkSuggestion("bck", "buck");
    checkSuggestion("bcuk", "buck");
    
    // Check the case copying facility
    checkSuggestion("Newpapers", "Newspapers");
    checkSuggestion("NEWPAPERS", "NEWSPAPERS");
    checkSuggestion("Bck", "Buck");
  }
  
  /** Test out multi-word replacements */
  public void testMultiWords() throws IOException
  {
    checkSuggestion("news papers", "newspapers");
    checkSuggestion("readnewspapers", "read newspapers");
    checkSuggestion("readn ewspapers", "read newspapers");
    
    checkSuggestion("orchards and bery patches", "orchards and berry patches");
  }
  
  /** Check that the given series of input words gets the right suggestion */
  private void checkSuggestion(String inWords, String outWords) 
    throws IOException
  {
    String[] ret = reader.suggestKeywords(inWords.split("\\W+"));
    assertEquals(outWords, StringUtil.join(ret));
  }
  
  /** Some test data for us to play with (thanks Project Gutenberg!) */
  public final String CALL_OF_THE_WILD =
    "The Call of the Wild\n" +
    "by Jack London\n" +
    "\n" +
    "Buck did not read the newspapers, or he would have known that " +
    "trouble was brewing, not alone for himself, but for every tide" +
    "water dog, strong of muscle and with warm, long hair, from Puget " +
    "Sound to San Diego.  Because men, groping in the Arctic darkness, " +
    "had found a yellow metal, and because steamship and transportation " +
    "companies were booming the find, thousands of men were rushing " +
    "into the Northland.  These men wanted dogs, and the dogs they " +
    "wanted were heavy dogs, with strong muscles by which to toil, and " +
    "furry coats to protect them from the frost.\n" +
    "\n"+
    "Buck lived at a big house in the sun-kissed Santa Clara Valley. " +
    "Judge Miller's place, it was called.  It stood back from the road, " +
    "half hidden among the trees, through which glimpses could be " +
    "caught of the wide cool veranda that ran around its four sides. " +
    "The house was approached by gravelled driveways which wound about " +
    "through wide-spreading lawns and under the interlacing boughs of " +
    "tall poplars.  At the rear things were on even a more spacious " +
    "scale than at the front.  There were great stables, where a dozen " +
    "grooms and boys held forth, rows of vine-clad servants' cottages, " +
    "an endless and orderly array of outhouses, long grape arbors, " +
    "green pastures, orchards, and berry patches.  Then there was the " +
    "pumping plant for the artesian well, and the big cement tank where " +
    "Judge Miller's boys took their morning plunge and kept cool in the " +
    "hot afternoon.\n" +
    "\n"+
    "And over this great demesne Buck ruled.  Here he was born, and " +
    "here he had lived the four years of his life.  It was true, there " +
    "were other dogs, There could not but be other dogs on so vast a " +
    "place, but they did not count.  They came and went, resided in the " +
    "populous kennels, or lived obscurely in the recesses of the house " +
    "after the fashion of Toots, the Japanese pug, or Ysabel, the " +
    "Mexican hairless,--strange creatures that rarely put nose out of " +
    "doors or set foot to ground. On the other hand, there were the fox " +
    "terriers, a score of them at least, who yelped fearful promises at " +
    "Toots and Ysabel looking out of the windows at them and protected " +
    "by a legion of housemaids armed with brooms and mops.\n" +
    "\n"+
    "But Buck was neither house-dog nor kennel-dog.  The whole realm " +
    "was his.  He plunged into the swimming tank or went hunting with " +
    "the Judge's sons; he escorted Mollie and Alice, the Judge's " +
    "daughters, on long twilight or early morning rambles; on wintry " +
    "nights he lay at the Judge's feet before the roaring library fire; " +
    "he carried the Judge's grandsons on his back, or rolled them in " +
    "the grass, and guarded their footsteps through wild adventures " +
    "down to the fountain in the stable yard, and even beyond, where " +
    "the paddocks were, and the berry patches.  Among the terriers he " +
    "stalked imperiously, and Toots and Ysabel he utterly ignored, for " +
    "he was king,--king over all creeping, crawling, flying things of " +
    "Judge Miller's place, humans included.\n" +
    "\n"+
    "His father, Elmo, a huge St.  Bernard, had been the Judge's " +
    "inseparable companion, and Buck bid fair to follow in the way of " +
    "his father.  He was not so large,--he weighed only one hundred and " +
    "forty pounds,--for his mother, Shep, had been a Scotch shepherd " +
    "dog.  Nevertheless, one hundred and forty pounds, to which was " +
    "added the dignity that comes of good living and universal respect, " +
    "enabled him to carry himself in right royal fashion.  During the " +
    "four years since his puppyhood he had lived the life of a sated " +
    "aristocrat; he had a fine pride in himself, was even a trifle " +
    "egotistical, as country gentlemen sometimes become because of " +
    "their insular situation.  But he had saved himself by not becoming " +
    "a mere pampered house-dog.  Hunting and kindred outdoor delights " +
    "had kept down the fat and hardened his muscles; and to him, as to " +
    "the cold-tubbing races, the love of water had been a tonic and a " +
    "health preserver.\n" +
    "\n"+
    "And this was the manner of dog Buck was in the fall of 1897, when " +
    "the Klondike strike dragged men from all the world into the frozen " +
    "North.  But Buck did not read the newspapers, and he did not know " +
    "that Manuel, one of the gardener's helpers, was an undesirable " +
    "acquaintance.  Manuel had one besetting sin.  He loved to play " +
    "Chinese lottery.  Also, in his gambling, he had one besetting " +
    "weakness--faith in a system; and this made his damnation certain. " +
    "For to play a system requires money, while the wages of a " +
    "gardener's helper do not lap over the needs of a wife and numerous " +
    "progeny.\n";
}
