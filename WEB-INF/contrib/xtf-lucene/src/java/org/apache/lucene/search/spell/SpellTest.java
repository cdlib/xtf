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
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.FSDirectory;
import org.cdlib.xtf.util.Trace;

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
  public static void main(String[] args) throws IOException 
  {
    if( args.length == 2 && args[0].equals("-build") )
        new SpellTest().build( args[1] );
    else if( args.length == 2 && args[0].equals("-test") )
        new SpellTest().test( args[1] );
    else {
        System.err.println( "Usage: {-build wordfile}|{-test pairfile}\n" );
        System.exit( 1 );
    }
  } // main()
  
  /** 
   * Build a spelling index using a word frequency file. The file should
   * be sorted by descending frequency.
   */
  private void build( String wordFile ) throws IOException
  {
    // Read in each line
    System.out.println( "Reading word file..." );
    BufferedReader reader = new BufferedReader(new FileReader(wordFile));
    ArrayList pairList = new ArrayList();
    while( true ) 
    {
        // It should consist of a word, a space, and the frequency
        String line = reader.readLine();
        if( line == null )
            break;
        int spacePos = line.indexOf( ' ' );
        assert spacePos >= 0;
        
        String word = line.substring( 0, spacePos );
        int    freq = Integer.parseInt( line.substring(spacePos+1) );
        
        // Clamp frequencies greater than 10000
        if( freq > 10000 )
            freq = 10000;
        
        // For testing, do only part of the dictionary.
        if( stopAt != null && word.compareTo(stopAt) >= 0 )
            continue;
        
        // Skip terms that have digits and weird characters.
        int i;
        for( i = 0; i < word.length(); i++ ) {
            char c = word.charAt( i );
            if( Character.isDigit(c) || c == '%' )
                break;
        }
        if( i < word.length() )
            continue;
        
        // Add the pair to our list
        Pair pair  = new Pair();
        pair.token = new Token( word, 0, 1 );
        pair.freq  = freq;
        pairList.add( pair ); 
    } // while
    reader.close();

    // Construct all the documents that embody the frequency data
    //writeFreqIndex( pairList );
    
    // Construct the spelling index.
    writeSpellIndex( pairList );
  } // build()

  private void writeFreqIndex( ArrayList pairList ) throws IOException
  {
    System.out.println( "Building frequency index..." );
    
    // Create a new index directory (clear any old one)
    FSDirectory indexDir = FSDirectory.getDirectory( "index", true );
    FreqAnalyzer analyzer = new FreqAnalyzer( pairList );
    IndexWriter indexWriter = new IndexWriter( indexDir, analyzer, true );
    
    // Increase the max field length to handle our biggest document.
    indexWriter.maxFieldLength = pairList.size() * 2;
    
    // Make a document for each frequency (including words for all higher
    // frequencies.)
    //
    StringReader nullReader = new StringReader("");
    int maxFreq = ((Pair)pairList.get(0)).freq;
    for( int limit = 0; limit <= maxFreq; limit++ )
    {
        if( (limit % 1000) == 0 ) 
            System.out.print( limit + "\r" );
        analyzer.limit = limit;
        Document doc = new Document();
        doc.add( Field.Text("words", nullReader) );
        indexWriter.addDocument( doc );
    } // for limit
    
    indexWriter.optimize();
    indexWriter.close();
    
    System.out.println( "Done." );
  } // writeFreqIndex()
  
  /**
   * Constructs a spelling index based on all the words in the list.
   */
  private void writeSpellIndex( ArrayList pairList ) throws IOException
  {
    System.out.println( "Writing spell index..." );
    
    long startTime = System.currentTimeMillis();
    SpellWriter spellWriter = new SpellWriter() {
        int prevPhase = -1;
        int prevPct   = -1;
        long prevTime = System.currentTimeMillis();
        public void progress( int phase, int pctDone, int totalAdded ) {
            long curTime = System.currentTimeMillis();
            long elapsed = curTime - prevTime;
            if( phase != prevPhase ) {
                Trace.untab();
                System.out.println( "Phase " + phase + ":" );
                Trace.tab();
                prevPhase = phase;
                prevPct = -1;
            }
            if( pctDone > prevPct && 
                (pctDone == 0 || pctDone == 100 || elapsed > 30000) ) 
            {
                prevTime = curTime;
                prevPct = pctDone;
                String pctTxt = Integer.toString(pctDone);
                while( pctTxt.length() < 3 ) 
                    pctTxt = " " + pctTxt;
                System.out.println( "[" + pctTxt + "%] Added " + totalAdded + "." );
            }
        }
    };
    spellWriter.open( "index", "spell" );
    spellWriter.clearIndex();
    
    for( int i = 0; i < pairList.size(); i++ ) {
        Pair pair = (Pair) pairList.get( i );
        spellWriter.queueWord( null, null, pair.token.termText() );
    }

    spellWriter.flushQueuedWords();
    spellWriter.close();
    
    System.out.println( "Done. Total time: " + (System.currentTimeMillis() - startTime) + " msec" );
    
  } // writeSpellIndex()
  
  /** Test the spelling index */
  private void test( String testFile ) throws IOException 
  {
    long startTime = System.currentTimeMillis();
    
    // Open the spelling index.
    SpellReader spellReader = new SpellReader( new File("spell") );
    
    // Open the frequency index.
    FSDirectory indexDir = FSDirectory.getDirectory( "index", false );
    IndexReader indexReader = IndexReader.open( indexDir );
    
    final int[] sizes = { 1, 5, 10, 25, 50 };
    //final int[] sizes = { 1 };
    final int[] totals = new int[sizes.length];
    int nWords = 0;
    
    NumberFormat fmt = DecimalFormat.getInstance();
    fmt.setMaximumFractionDigits( 1 );
    
    // Open the file and read each line.
    BufferedReader lineReader = new BufferedReader(new FileReader(testFile));
    ArrayList missed = new ArrayList();
    while( true ) 
    {
        // It should consist of a word, a separator, and a correction
        String line = lineReader.readLine();
        if( line == null )
            break;
        
        String word;
        String correction;
        
        int arrowPos = line.indexOf( "->" );
        if( arrowPos > 0 ) {
            word = line.substring(0, arrowPos).trim();
            correction = line.substring(arrowPos+2).trim();
        }
        else {
            int spacePos = line.indexOf( '\t' );
            if( spacePos > 0 ) {
                word = line.substring(0, spacePos).trim();
                correction = line.substring(spacePos+1).trim();
            }
            else {
                System.out.println( "Unrecognized test line: " + line );
                continue;
            }
        }
        
        word = word.toLowerCase();
        correction = correction.toLowerCase();
        
        // If equal, skip (this can happen for uppercase -> lowercase)
        if( word.equals(correction) )
            continue;
        
        // For testing, stop early.
        if( stopAt != null && word.compareTo(stopAt) >= 0 )
            continue;
        
        // Skip if the target word isn't in our dictionary.
        if( !spellReader.inDictionary(correction) )
            continue;
        
        // Bump the word count.
        ++nWords;
        
        System.out.print( word + " " + correction );

        // Get some suggestions.
        int pos = trySpell( spellReader, indexReader, 
                            word, correction, sizes[sizes.length-1] );
        
        // Increment the correct totals.
        if( pos < 0 )
            missed.add( word );
        else
        {
            for( int j = 0; j < sizes.length; j++ ) {
                if( pos < sizes[j] )
                    totals[j]++;
            }
        }
                
        System.out.println( " " + (pos+1) );
        
        if( nWords == 1 )
            System.out.println( "Time to first word: " + (System.currentTimeMillis() - startTime) + " msec" );
        
    } // while
    
    System.out.println();
    double pctMissed = missed.size() * 100.0 / nWords;
    System.out.print( "Missed " + fmt.format(pctMissed) + "%: " );
    for( int i = 0; i < missed.size(); i++ )
        System.out.print( missed.get(i) + " " );
    System.out.println();
    
    System.out.println();
    System.out.print( "**TOTAL** " );
    for( int i = 0; i < sizes.length; i++ ) {
        double pct = totals[i] * 100.0 / nWords;
        System.out.print( " " + fmt.format(pct) );
    }
    System.out.println();
    
    System.out.println( "Done. Total time: " + (System.currentTimeMillis() - startTime) + " msec" );
    
    lineReader.close();
    indexReader.close();
    spellReader.close();
    
  } // test()
  
  /**
   * Gather the specified number of suggestions for a word, and return the
   * position of the correction in the result list.
   */
  private int trySpell( SpellReader spellReader,
                        IndexReader indexReader,
                        String      word, 
                        String      correction, 
                        int         nSuggestions )
    throws IOException
  {
    ArrayList list = new ArrayList();
    //String[] fields = { "text", "title-main", "author", "subject", "note" };
    String[] fields = { "words" };
    
    SuggestWord[] suggestions = spellReader.suggestSimilar( 
        word, nSuggestions, indexReader, fields, 
        0, 0.5f );
    
    int found = -1;
    for( int i = 0; i < suggestions.length; i++ )
    {
        if( suggestions[i].string.equals(correction) ) {
            found = i;
            break;
        }
    } // for i

    /*
    if( found != 0 ) {
        System.out.println( "...orig metaphone: " + SpellWriter.calcMetaphone(word) );
        for( int i = 0; i < suggestions.length; i++ ) {
            SuggestWord sugg = suggestions[i];
            System.out.print( "   " );
            System.out.print( (i == found)     ? "*" : " " );
            System.out.println( sugg.string +
                "\t" + sugg.score +
                "\t" + sugg.origScore +
                "\t" + sugg.freq +
                "\t" + SpellWriter.calcMetaphone(sugg.string) );
        }
    }
    */
    
    return found;
    
  } // trySpell()
  
  /**
   * Gather the specified number of suggestions for a word, and return the
   * position of the correction in the result list.
   */
  private int trySpellOld( SpellReader spellReader,
                           IndexReader indexReader,
                           String      word, 
                           String      correction, 
                           int         nSuggestions )
    throws IOException
  {
    SuggestWord[] suggestions = spellReader.suggestSimilar( 
        word, nSuggestions, indexReader, "words", 
        0, 0.5f );
    
    int found = -1;
    for( int i = 0; i < suggestions.length; i++ )
    {
        if( suggestions[i].string.equals(correction) ) {
            found = i;
            break;
        }
    } // for i

    /*
    if( found != 0 ) {
        System.out.println( "...orig metaphone: " + SpellWriter.calcMetaphone(word) );
        for( int i = 0; i < suggestions.length; i++ ) {
            SuggestWord sugg = suggestions[i];
            System.out.print( "   " );
            System.out.print( (i == found)     ? "*" : " " );
            System.out.println( sugg.string +
                "\t" + sugg.score +
                "\t" + sugg.origScore +
                "\t" + sugg.freq +
                "\t" + SpellWriter.calcMetaphone(sugg.string) );
        }
    }
    */
    
    return found;
    
  } // trySpell()
  
  /** Records a token/frequency pair */
  private class Pair
  {
    Token token;
    int   freq;
  } // class Pair
  
  /** Creates the synthetic field contents to embody the frequency data */
  private class FreqAnalyzer extends Analyzer
  {
    private ArrayList pairList;
    
    public FreqAnalyzer( ArrayList pairList ) {
      this.pairList = pairList;
    }
    
    public int limit;
    
    public TokenStream tokenStream( String field, Reader reader )
    {
      return new TokenStream() 
      {
          private int cur = 0;
          
          public Token next() 
          {
              if( cur == pairList.size() )
                  return null;
              
              Pair pair = (Pair)pairList.get(cur);
              if( pair.freq < limit )
                  return null;
              
              cur++;
              return pair.token;
          }
      };
    } // tokenStream()
  } // class FreqAnalyzer
} // class SpellTest
