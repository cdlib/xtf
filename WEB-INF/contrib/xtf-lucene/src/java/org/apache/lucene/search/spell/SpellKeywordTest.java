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
  
  /** Test the spelling index */
  private void test( String testFile ) throws IOException 
  {
    long startTime = System.currentTimeMillis();
    
    // Open the spelling dictionary.
    SpellReader spellReader = SpellReader.open( new File("spell") );
    
    // Open the debug stream.
    PrintWriter debugWriter = new PrintWriter(new FileWriter("spellDebug.log"));
    spellReader.setDebugWriter(debugWriter);
    
    // Open the file and read each line.
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
        String correctPhrase = parts[1].trim();
        
        // Break the original phrase into words
        String[] origWords = origPhrase.split("\\s");
        
        // Get a suggestion from the spell checker.
        debugWriter.println("orig=\"" + origPhrase + "\", corr=\"" + correctPhrase + "\"");
        String[] suggWords = spellReader.suggestKeywords(origWords);
        
        // Form up the suggested phrase.
        String suggPhrase = StringUtil.join(suggWords);
        nTried++;
        
        // Output the results.
        debugWriter.print("--> sugg=\"" + suggPhrase + "\"");
        if (!suggPhrase.equals(correctPhrase) ) {
          debugWriter.print("  WRONG");
          System.out.println("orig=\"" + origPhrase + "\", corr=\"" + correctPhrase + "\", sugg=\"" + suggPhrase + "\"");
        }
        else
          nCorrect++;
        debugWriter.println();
        
        // Accumulate totals.
        
        if( nTried == 1 )
            debugWriter.println( "Time to first word: " + (System.currentTimeMillis() - startTime) + " msec" );
        
    } // while
    
    System.out.println();
    System.out.print( "**TOTAL** " );
    System.out.printf("%.1f%%", nCorrect * 100.0 / nTried);
    
    System.out.println( "Done. Total time: " + (System.currentTimeMillis() - startTime) + " msec" );
    
    lineReader.close();
    spellReader.close();
    debugWriter.close();
    
  } // test()
  
} // class
