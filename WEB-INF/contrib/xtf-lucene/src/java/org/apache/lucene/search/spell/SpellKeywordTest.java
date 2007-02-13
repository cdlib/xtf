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
import java.io.StringReader;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.lucene.analysis.Token;
import org.cdlib.xtf.textEngine.DefaultQueryProcessor;
import org.cdlib.xtf.textEngine.QueryRequest;
import org.cdlib.xtf.textEngine.QueryRequestParser;
import org.cdlib.xtf.textEngine.QueryResult;
import org.cdlib.xtf.textEngine.XtfSearcher;
import org.cdlib.xtf.textIndexer.tokenizer.ParseException;
import org.cdlib.xtf.textIndexer.tokenizer.XTFTokenizer;
import org.cdlib.xtf.util.Path;
import org.cdlib.xtf.util.StringList;
import org.cdlib.xtf.util.StringUtil;

/**
 * Automated test of keyword spelling correction to determine its overall 
 * accuracy.
 *
 * @author Martin Haye
 */
public class SpellKeywordTest 
{
  /**
   * Command-line driver.
   */
  public static void main(String[] args) throws IOException 
  {
    if( args.length == 1 )
        new SpellKeywordTest().test( args[0] );
    else {
        System.err.println( "Usage: test <wordFile>\n" );
        System.exit( 1 );
    }
  } // main()

  /** Directory in which the index (and spelling data) is stored */
  private String indexDir;
  
  /** Test the spelling index */
  private void test( String testFile ) throws IOException 
  {
    long startTime = System.currentTimeMillis();
    
    // Create a query processor, and open the index.
    indexDir = Path.resolveRelOrAbs(new File(".").getAbsoluteFile(), "index");
    XtfSearcher searcher = DefaultQueryProcessor.getXtfSearcher(indexDir);
    
    // Attach a debug stream to the spell reader.
    SpellReader spellReader = searcher.spellReader();
    PrintWriter debugWriter = new PrintWriter(new FileWriter("spellDebug.log"));
    spellReader.setDebugWriter(debugWriter);
    
    // Open the test query file and read each line.
    BufferedReader lineReader = new BufferedReader(new FileReader(testFile));
    int nTried = 0;
    int nCorrect = 0;
    while( true ) 
    {
        // It should consist of phrase, a separator, and a correction
        String line = lineReader.readLine();
        if( line == null )
            break;
        
        // Strip off any comment.
        line = line.replaceFirst(";.*", "");
        
        // Get the pieces.
        String[] parts = line.split("->");
        if (parts.length != 2) {
          System.out.println("Unrecognized test line: " + line);
          continue;
        }
        String origPhrase = parts[0].trim();
        String correctPhraseStr = parts[1].trim();
        String[] correctPhrases = correctPhraseStr.split("\\|");

        /*
         * This ends up skipping some interesting cases.
         * 
        // Check that all the correct words are in our dictionary. If not,
        // there's no point in trying.
        //
        String[] correctWords = StringUtil.splitWords(correctPhrase);
        boolean skip = false;
        for (String s : correctWords) {
          if (!spellReader.inDictionary(s))
            skip = true;
        }
        if (skip) {
          debugWriter.println("Skip: orig=\"" + origPhrase + "\", corr=\"" + correctPhrase + "\"");
          continue;
        }
        */
        
        // Try out the spelling suggestion algorithms
        debugWriter.println("orig=\"" + origPhrase + "\", corr=\"" + correctPhraseStr + "\"");
        String suggPhrase = testPhrase(origPhrase, debugWriter);
        
        // Output the results.
        nTried++;
        debugWriter.print("--> sugg=\"" + suggPhrase + "\"");
        boolean correct = false;
        for (int i = 0; i < correctPhrases.length; i++)
          correct |= correctPhrases[i].equalsIgnoreCase(suggPhrase);
        if (!correct) {
          debugWriter.print("  WRONG");
          System.out.println("orig=\"" + origPhrase + "\", corr=\"" + correctPhraseStr + "\", sugg=\"" + suggPhrase + "\"");
        }
        else
          nCorrect++;
        debugWriter.println();
        
        // Record our warm-up time.
        if( nTried == 1 )
            debugWriter.println( "Time to first word: " + (System.currentTimeMillis() - startTime) + " msec" );
        
    } // while
    
    System.out.println();
    System.out.print( "**TOTAL** " );
    System.out.printf("%.1f%%", nCorrect * 100.0 / nTried);
    System.out.println();
    
    System.out.println( "Done. Total time: " + (System.currentTimeMillis() - startTime) + " msec" );
    
    lineReader.close();
    spellReader.close();
    debugWriter.close();
    
  } // test()

  private String testPhrase(String origPhrase, 
                            PrintWriter debugWriter) 
    throws IOException
  {
    // Break the original phrase into words
    String[] origWords = split(origPhrase);
    
    // Create a query using those words.
    StringBuffer buf = new StringBuffer();
    buf.append("<query indexPath=\"index\" termLimit=\"1000\" workLimit=\"20000000\" " +
               "style=\"style/crossQuery/resultFormatter/marc/resultFormatter.xsl\" " +
               "startDoc=\"1\" maxDocs=\"20\" normalizeScores=\"false\">\n" +
               "<spellcheck suggestionsPerTerm=\"1\" totalDocsCutoff=\"1000\" fields=\"text,title-main,author,subject,note\"/>\n" +
               "<and fields=\"text title-main author subject note\" boosts=\"0.5  1.0        1.0    0.5     1.0 \" slop=\"10\" maxTextSnippets=\"3\" maxMetaSnippets=\"all\">\n");
    for (String s : origWords)
      buf.append("<term>" + s + "</term>\n");
    buf.append("</and></query>");
    
    // Transform it into a query request.
    QueryRequestParser parser = new QueryRequestParser();
    Source src = new StreamSource(new StringReader(buf.toString()));
    QueryRequest req = parser.parseRequest(src, new File(".").getAbsoluteFile(), indexDir);
    
    // Now process the query.
    DefaultQueryProcessor queryProcessor = new DefaultQueryProcessor();
    QueryResult res = queryProcessor.processRequest(req);
    
    // See if it made a spelling suggestion. If not, return the original.
    if (res.suggestions == null || res.suggestions.length == 0)
      return null;
    
    // Okay, put together the first suggestion.
    String[] suggWords = new String[origWords.length];
    for (int i=0; i<origWords.length; i++) {
      suggWords[i] = origWords[i];
      for (int j=0; j<res.suggestions.length; j++) {
        if (res.suggestions[j].origTerm.equals(origWords[i]))
          suggWords[i] = res.suggestions[j].suggestedTerm;
      }
    }
    return StringUtil.join(suggWords);
  }
  
  public String[] split(String in) throws ParseException, IOException
  {
    XTFTokenizer toks = new XTFTokenizer(new StringReader(in));
    StringList list = new StringList();
    while (true) {
      Token t = toks.next();
      if (t == null)
        break;
      list.add(t.termText());
    }
    return list.toArray();
  }
  
} // class
