package org.cdlib.xtf.dynaXML.test;

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

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.cdlib.xtf.util.Path;
import org.cdlib.xtf.util.Trace;

/**
 * CDL-specific test: searches for every chunk of every document in an index.
 * 
 * @author Martin Haye
 */
public class CDLSearchTest extends SearchTest
{
  private static final String server    = "http://foo.bar/";
  private static final String urlPrefix = server + "dynaXML";
  private int totalUrls = 0;
  private int urlsDone  = 0;
  private final String searchTerm = "those";
  
  private String  skipToDoc = null;
  private boolean skipping = true; // Don't change this.
  
  private String  debugDoc = null;
  private int     debugNum = -1;

  /**
   * Default constructor
   * 
   * @param baseDir   The normal base directory used for DynaXML
   */
  public CDLSearchTest( String baseDir )
    throws ServletException
  {
    super(baseDir);
  }
  
  /**
   * Parses out the document ID given a document's path. Very CDL-specific.
   * 
   * @param path    Path to the document
   * @return        9-digit ID (letters and numbers)
   */
  private String docIdFromPath( String path )
  {
    int lastSlash = path.lastIndexOf( '/' );
    assert lastSlash >= 0;
    
    int dotXml = path.lastIndexOf( ".xml" );
    assert dotXml >= 0;
    
    return path.substring( lastSlash+1, dotXml );
  } // docIdFromPath()
  
  /**
   * Scans an index for all the documents present in it, and returns their 
   * paths.
   * 
   * @return  Array of all the document paths
   */
  private String[] getDocs( String indexPath, String sourcePath )
    throws IOException
  {
    IndexReader reader = IndexReader.open( indexPath );
    
    // Figure out how many entries we'll have, and make our array that big.
    Term term = new Term( "docInfo", "1" );
    int nDocs = reader.docFreq( term );
    String[] docPaths = new String[nDocs];
        
    // Get a list of all the "header" chunks for documents in this
    // index (i.e., documents with a "docInfo" field.)
    //
    TermDocs docHeaders = reader.termDocs( term );
    
    // Look up each document.
    int i = 0;
    while( docHeaders.next() ) {
        int docNum = docHeaders.doc();
        Document doc = reader.document( docNum );
        String key = doc.get( "key" );
        assert key != null && key.length() > 0 : "missing key in DocInfo";
        String partPath = key.substring( key.indexOf(':') + 1 );
        docPaths[i++] = Path.normalizeFileName(sourcePath + "/" + partPath);
    }
    
    // If some documents have been deleted, our loop will have generated less
    // than the expected number. For ease of the caller's life, just shrink
    // the array to exactly fit.
    //
    if( i != docPaths.length ) {
          String[] newArray = new String[i];
          System.arraycopy( docPaths, 0, newArray, 0, i );
          docPaths = newArray;
    }
    
    // All done.
    return docPaths;
  } // getDocIds()
  
  /**
   * If the URL hasn't been processed yet, it is added to the LinkedList.
   * 
   * @param processedUrls   Set of all processed URLs so far
   * @param url             URL to push
   * @param urlStack        LinkedList to add to
   */
  private void pushUrl( HashSet processedUrls, String url, LinkedList urlStack )
  {
    if( !url.startsWith("http") )
        url = server + url;
    
    if( !url.startsWith(urlPrefix) )
        return;
    
    url = url.replaceAll( "&amp;", "&" );
    url = url.replaceAll( "&&", "&" );
    url = url.replaceAll( "#.*", "" );
    url = url.replaceAll( "&anchor.id=[^&#]+", "" );
    url = url.replaceAll( "&set.anchor=[^&#]+", "" );
    url = url.replaceAll( "&toc.id=0", "" );
    url = url.replaceAll( "&toc.id=&", "&" );
    url = url.replaceAll( "&toc.depth=0", "" );
    url = url.replaceAll( "&toc.depth=&", "&" );
    url = url.replaceAll( "&chunk.id=0", "" );
    url = url.replaceAll( "&chunk.id=&", "&" );
    url = url.replaceAll( "&query=[^&#]*", "" );
    url += "&query=" + searchTerm;

    Matcher matcher = Pattern.compile("&doc.view=([^&#]*)").matcher(url);
    String view = (matcher.find()) ? matcher.group(1) : "";
    
    if( "bbar|toc|".indexOf(view+"|") >= 0 )
        url = url.replaceAll( "&chunk.id=[^&#]*", "" );

    if( "bbar|content|".indexOf(view+"|") >= 0 ) {
        url = url.replaceAll( "&toc.id=[^&#]*", "" );
        url = url.replaceAll( "&toc.depth=[^&#]*", "" );
    }
    
    if( !processedUrls.contains(url) ) {
        urlStack.addLast( url );
        processedUrls.add( url );
        totalUrls++;
    }
  } // pushUrl()

  /**
   * Scans the HTML result and adds all relevant URLs that haven't already been
   * processed to the urlStack.
   * 
   * @param processedUrls   Set of URLs which have already been processed
   * @param htmlResult      HTML to scan
   * @param urlStack        LinkedList to add to
   */
  private void pushUrls( HashSet processedUrls, 
                         String  htmlResult, 
                         LinkedList   urlStack )
  {
    // Scan for hyperlink references
    Matcher matcher = Pattern.compile("<a\\s+href=\"([^\"]*)\"").matcher(htmlResult);
    while( matcher.find() ) 
        pushUrl( processedUrls, matcher.group(1), urlStack );    
    
    matcher = Pattern.compile(
        "<frame\\s+[^>]*src=\"([^\"]*)\"").matcher(htmlResult);
    while( matcher.find() ) 
        pushUrl( processedUrls, matcher.group(1), urlStack );    
  } // pushUrls()
  
  /**
   * Attempts to parse out a chunk ID from a URL.
   * 
   * @param url   URL to scan
   * @return      Chunk ID, or "" if none.
   */
  private String urlToChunkId( String url )
  {
    int pos1 = url.indexOf( "chunk.id=" );
    if( pos1 < 0 )
        return "";
    int pos2 = url.indexOf( "&", pos1 );
    if( pos2 < 0 )
        pos2 = url.length();
    
    return url.substring( pos1 + "chunk.id=".length(), pos2 );
  } // urlToChunkId()

  /**
   * Transforms parts of the text that are okay to mismatch into matching text.
   */
  private String normalizeResult( String result ) {
    result = result.replaceAll( " xmlns:\\w+=\"[^\"]+\"", "" );
    return result;
  }
  
  /**
   * Breaks up a string by newlines into an array of strings, one per line.
   * 
   * @param str   String to break up
   * @return      Array of the lines
   */
  String[] slurp( String str )
  {
      BufferedReader br = new BufferedReader( new StringReader(str) );
      Vector lines = new Vector( 100 );
      while( true ) {
          try {
              String line = br.readLine();
              if( line == null )
                  break;
              lines.add( line );
          }
          catch( IOException e ) {
              assert false : "String reader should never have IO exception";
              throw new RuntimeException( e );
          }
      } // while
      
      return (String[]) lines.toArray( new String[lines.size()] );
  } // slurp()
  
  /**
   * Compares two strings for equality. If not equal, a message is printed.
   * 
   * @param result1   First string
   * @param result2   Second string
   * @return          true if equal, false if not (and message printed.)
   */
  private boolean sameResults( String result1, String result2 )
  {
    if( result1.equals(result2) )
        return true;
    
    Trace.info( "\n*** Mismatch! ***" );
    return false;
  } // sameResults()
  
  /**
   * Runs the test for a single document. First, examines the document to find
   * all the chunk IDs, then does a chunk test for each one.
   * 
   * @param docPath   File path of the document to test
   */
  private void testDoc( String docPath )
    throws Exception
  {
    if( debugDoc != null && !docIdFromPath(docPath).endsWith(debugDoc) ) {
        Trace.info( "Skipping " + docIdFromPath(docPath) );
        return;
    }

    if( skipToDoc != null && skipping ) {
        if( docIdFromPath(docPath).endsWith(skipToDoc) )
            skipping = false;
        if( skipping ) {
            Trace.info( "Skipping " + docIdFromPath(docPath) );
            return;
        }
    }
    
    // Read and annotate the document.
    Trace.info( "Processing " + docIdFromPath(docPath) + "..." );
    dynaXML.setSearchTerm( searchTerm );
    //DocumentInfo doc = dynaXML.getAnnotatedTree( docPath );
    
    // Make the first entry on our to-do LinkedList.
    LinkedList urlStack = new LinkedList();
    urlStack.addLast( urlPrefix + 
                      "?docId=" + docIdFromPath(docPath) +
                      "&doc.view=frames" +
                      "&query=" + searchTerm );
    totalUrls = 1;
    urlsDone  = 0;
    
    // Process until we've traversed all the links.
    HashSet processedUrls = new HashSet();
    int nMismatches = 0;
    while( !urlStack.isEmpty() ) 
    {
      String url = (String) urlStack.removeFirst();
      
      processedUrls.add( url );
      urlsDone++;
      Trace.info( "  " + urlsDone + "/" + totalUrls + 
                  ": " + url.substring(urlPrefix.length()+1) + "..." );

      if( urlsDone == debugNum ) {
          dynaXML.setProfiling( true );
          dynaXML.setDump( true );
      }
      
      long start1 = System.currentTimeMillis();
      
      dynaXML.useAnnotated( true );
      String result1 = runDynaXML( url );
      result1 = normalizeResult( result1 );
      
      long start2 = System.currentTimeMillis();

      dynaXML.useAnnotated( false );
      String result2 = runDynaXML( url );
      result2 = normalizeResult( result2 );
      
      long end2 = System.currentTimeMillis();

      long elapsed = end2 - start1;
      long rate = result2.length() / (elapsed+1);
      
      long rate1 = result1.length() / (start2-start1+1);
      long rate2 = result2.length() / (end2-start2+1);
      
      Trace.info( " (time=" + (start2-start1) + "/" + (end2-start2) +
                          ", bytes=" + result1.length() + 
                          ", rate=" + rate1 + "/" + rate2 + ")" );
      
      // If the rate is crappy and it took a while to process this URL,
      // print out a profile.
      //
      if( false && rate < 20 && elapsed > 500 ) {
          dynaXML.setProfiling( true );
          dynaXML.useAnnotated( false );
          runDynaXML( url );
          dynaXML.setProfiling( false );
      }
      
      // Make sure both methods came up with the same result.
      if( !sameResults(result1, result2) ) {
          Trace.info( "Mismatch: " + url );
          nMismatches++;
          System.exit( 1 ); // Temporary
      }
      
      if( urlsDone == debugNum ) {
          PrintWriter outWriter;
          
          outWriter = new PrintWriter( new OutputStreamWriter(
                          new FileOutputStream("C:\\tmp\\out1.html"), "UTF-8") );
          outWriter.println( result1 );
          outWriter.close();
          
          outWriter = new PrintWriter( new OutputStreamWriter(
                          new FileOutputStream("C:\\tmp\\out2.html"), "UTF-8") );
          outWriter.println( result2 );
          outWriter.close();

          System.exit( 0 );
      }
      
      pushUrls( processedUrls, result1, urlStack );
    
    } // while( !urlStack.isEmpty() )
    
  } // testDoc()

  /**
   * Actual controlling routine that manages the test through its various
   * stages.
   */
  public void runTest( String indexPath, String sourcePath )
    throws Exception
  {
    // Get a list of all the documents first.
    Trace.info( "Getting docs..." );
    String[] docPaths = getDocs( indexPath, sourcePath );
    Trace.info( "Got docs." );
    
    // Now test each one.
    for( int i = 0; i < docPaths.length; i++ )
        testDoc( docPaths[i] );
  } // runTest()
  
  /**
   * Runs the test.
   */
  public static void main( String[] args )
  {
    // Make sure the source directory is specified.
    if( args.length < 3 ) {
        Trace.error( "Usage: CDLSearchTest [baseDir] [indexPath] [sourcePath] {start-doc-id}" );
        System.exit( 1 );
    }
    
    // Make sure assertions are enabled...
    boolean flag = false;
    assert flag = true;
    if( !flag ) {
        Trace.error( 
            "Assertions must be enabled for this test. Pass -ea to Jave VM." );
        System.exit( 1 );
    }
    
    // Try a request.
    try {
        CDLSearchTest test = new CDLSearchTest( args[0] );
        if( args.length > 3 )
            test.skipToDoc = args[3];
        test.runTest( args[1], args[2] );
    }
    catch( Exception e ) {
        Trace.error( "Fatal exception: " + e );
        System.exit( 1 );
    }
    
    // The servlet keeps threads alive, so we have to exit forcefully.
    System.exit( 0 );
    
  } // main()
  
} // class CDLSearchTest
