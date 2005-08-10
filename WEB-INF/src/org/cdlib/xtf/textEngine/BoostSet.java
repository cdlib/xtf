package org.cdlib.xtf.textEngine;

/*
 * Copyright (c) 2005, Regents of the University of California
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.WeakHashMap;

import org.apache.lucene.chunk.DocNumMap;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermPositions;
import org.cdlib.xtf.util.Trace;

/**
 * Holds a set of boost factors to apply to individual documents in the
 * document set.
 * 
 * @author Martin Haye
 */
public class BoostSet 
{
  /** Cached data. If the reader goes away, our cache will too. */
  private static WeakHashMap cache = new WeakHashMap();
  
  /** Number of warnings emitted so far. After 10, we suppress them. */
  private int nWarnings = 0;
  
  /** Set of boost values, one per document ID */
  float[] boostByDoc;
  
  /**
   * Retrieves BoostSet for a given File from a given reader. Maintains a cache
   * so that if the same File is requested again for this reader, we don't have
   * to re-read the boost data.
   * 
   * @param indexReader  Index to correlate the data to
   * @param docNumMap    Used to map chunk numbers to document numbers
   * @param inFile       Which file to read
   * @return             Group data for the specified field
   */
  public static BoostSet getCachedSet( IndexReader indexReader,
                                       DocNumMap   docNumMap,
                                       File        inFile )
    throws IOException
  {
    // See if we have a cache for this reader.
    HashMap readerCache = (HashMap) cache.get( indexReader );
    if( readerCache == null ) {
        readerCache = new HashMap();
        cache.put( indexReader, readerCache );
    }
    
    // Now see if we've already read data for this field.
    BoostSet set = (BoostSet) readerCache.get( inFile );
    if( set == null )
    {
        // Don't have cached data, so read and remember it.
        set = new BoostSet( indexReader, docNumMap, inFile );
        readerCache.put( inFile, set );
    }
    
    return set;
    
  } // getCachedSet()
  
  /** Get the boost factor associated with the given document, or 1.0f if
   *  not found.
   *  
   *  @param docId   Document ID to look up
   *  @return        Boost factor, or 1.0f if not found.
   */
  public final float getBoost( int docId )
  {
    if( docId < 0 || docId >= boostByDoc.length )
        return 1.0f;
    return boostByDoc[docId];
  } // getBoost()
  
  /** Do not construct directly; use {@link #getCachedSet(IndexReader, File)}
   *  instead. Constructs a BoostSet by reading a file containing document
   *  key -> boost factor mappings, and correlating it with the keys in the
   *  given index reader.
   */
  private BoostSet( IndexReader indexReader, DocNumMap docNumMap, File inFile )
    throws IOException
  {
    // Figure out the max doc ID, make an array that big, and fill it with
    // the default value (1.0).
    //
    int maxDoc = indexReader.maxDoc();
    boostByDoc = new float[maxDoc+1];
    Arrays.fill( boostByDoc, 1.0f );
    
    // Iterate all the keys in the index.
    DocIter docIter = null;
    LineIter lineIter = null;
    try 
    {
      docIter = new DocIter( indexReader, docNumMap );
      lineIter = new LineIter( new BufferedReader( new FileReader(inFile) ) );
      
      // Process all matches
      while( !docIter.done() && !lineIter.done() )
      {
          String docKey  = docIter.key();
          String lineKey = lineIter.key();
          int diff = docKey.compareTo( lineKey );
          if( diff < 0 ) {
              System.out.println( "Skipping doc " + docKey );
              docIter.next();
              continue;
          }
          else if( diff > 0 ) {
              warn( "Boost document key '" + lineKey + "' not found in index" );
              lineIter.next();
              continue;
          }
          
          // Found a match.
          int docId = docIter.docId();
          Trace.info( docKey + " -> " + docId );
          boostByDoc[docId] = lineIter.boost();
          
          docIter.next();
          lineIter.next();
      }
      
      // Warn about any leftover docs
      while( !docIter.done() ) {
          System.out.println( "Skipping doc " + docIter.key() );
          docIter.next();
      }
      
      // Warn about any leftover lines
      while( !lineIter.done() ) {
          warn( "Boost document key '" + lineIter.key() + "' not found in index" );
          lineIter.next();
      }
          
    } finally {
      if( docIter != null )
          docIter.close();
      if( lineIter != null )
          lineIter.close();
    }

  } // constructor
  
  /**
   * If less than 10 warnings have been emitted, we print this one out.
   * Otherwise, we suppress it.
   * 
   * @param msg   The message to emit
   */
  private void warn( String msg )
  {
    ++nWarnings;
    if( nWarnings < 10 )
        Trace.warning( msg );
    else if( nWarnings == 10 )
        Trace.warning( "Further warnings suppressed." );
  } // warn()
  
  /** 
   * Iterates all the document keys in an index 
   */
  private class DocIter
  {
    DocNumMap     docNumMap;
    boolean       done = false;
    String        prevDocKey = "";
    String        docKey;
    final String  field = "key";
    TermPositions termPositions;
    TermEnum      termEnum;
    
    /** Construct from an index reader */
    DocIter( IndexReader indexReader, DocNumMap docNumMap ) 
      throws IOException 
    {
      this.docNumMap = docNumMap;
      termPositions = indexReader.termPositions();
      termEnum      = indexReader.terms(new Term(field, ""));
      readDocKey();
    }
    
    /** Return true if there are no more documents to read */
    boolean done() { return done; }
    
    /** Gets the key of the current document */
    String key() { return docKey; }

    /** Gets the Lucene document ID of the current document */
    int docId() throws IOException {
      termPositions.seek( termEnum );
      while( termPositions.next() ) {
          int chunk = termPositions.doc();
          int doc = docNumMap.getDocNum( chunk );
          assert doc >= 0 : "error mapping first chunk";
          return doc;
      }

      assert false : "error reading term positions";
      return -1;
    }
    
    /** Advances to the next document in the index */
    void next() throws IOException {
      if( !termEnum.next() )
          done = true;
      readDocKey();
    }
    
    /** Clean up */
    void close() throws IOException {
      termPositions.close();
      termEnum.close();
    }
    
    /** Fetch the current document key; update done */
    private void readDocKey()
    {
      Term term = termEnum.term();
      if( !term.field().equals(field) ) {
          done = true;
          return;
      }
      
      docKey = term.text();
      
      // Strip the path and index name
      int lastSlash = docKey.lastIndexOf( '/' );
      if( lastSlash < 0 )
          lastSlash = docKey.indexOf( ':' );
      docKey = docKey.substring( lastSlash + 1 );
      
      // Strip any file extension(s)
      int dotPos = docKey.indexOf( '.' );
      if( dotPos >= 0 )
          docKey = docKey.substring( 0, dotPos );
      
      assert docKey.compareTo(prevDocKey) > 0 : "doc keys coming out in wrong order";
      prevDocKey = docKey;
    }
    
  } // class DocIter
  
  /** 
   * Iterates all the lines in a boost file 
   */
  private class LineIter
  {
    BufferedReader reader;
    boolean        done = false;
    String         prevLineKey = "";
    String         lineKey;
    float          lineBoost;
    
    /** Construct from a reader */
    LineIter( BufferedReader reader ) throws IOException {
      this.reader = reader;
      readLine();
    }
    
    /** Returns true if no more lines to read */
    boolean done() { return done; }
    
    /** Get the document key of the current line */
    String key() { return lineKey; }
    
    /** Get the boost factor of the current line */
    float boost() { return lineBoost; }
    
    /** Advance to the next line */
    void next() throws IOException {
      readLine();
    }
    
    /** Clean up */
    void close() throws IOException {
      reader.close();
    }
    
    /** Read the next line in the file */
    private void readLine() throws IOException {
      while( !done ) {
          String line = reader.readLine();
          if( line == null ) {
              done = true;
              break;
          }
          int sepPos = line.indexOf( '|' );
          if( sepPos < 0 ) {
              warn( "Boost line missing separator: '" + line + "'" );
              continue;
          }
          lineKey = line.substring( 0, sepPos );
          lineBoost = Float.parseFloat( line.substring(sepPos+1) );
          
          if( lineKey.compareTo(prevLineKey) <= 0 ) {
              Trace.error( 
                  "Error: Boost set lines out of order: '" + prevLineKey +
                  "' came before '" + lineKey + "', but should come after." );
              done = true;
              break;
          }
          prevLineKey = lineKey;
          
          // Got a valid line.
          break;
      }
    } // readLine()
    
  } // class LineIter
  
} // class BoostSet
