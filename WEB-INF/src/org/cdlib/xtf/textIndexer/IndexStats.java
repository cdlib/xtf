package org.cdlib.xtf.textIndexer;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.cdlib.xtf.textEngine.IdxConfigUtil;
import org.cdlib.xtf.util.Path;
import org.cdlib.xtf.util.Trace;

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

/**
 * This class calculates and prints out some useful statistics about an
 * existing index, such as number of documents, size, etc.
 * 
 * @author Martin Haye
 */
public class IndexStats 
{
    
  private static long totalDocs       = 0;
  private static long totalChunks     = 0;
  private static long totalLazySize   = 0;
  private static long totalLuceneSize = 0;

  //////////////////////////////////////////////////////////////////////////////
  
  /** Main entry-point for the statistics gatherer. <br><br>
   * 
   *  This function takes the command line arguments passed and uses them to
   *  find an index and calculate statistics for it.
   *  <br><br>
   * 
   *  @param  args    Command line arguments to process. The command line 
   *  arguments required by the IndexStats program are as follows:
   *
   * <blockquote dir=ltr style="MARGIN-RIGHT: 0px"><code>
   * <b>IndexStats {-config</b> <font color=#0000ff><i>CfgFilePath</i></font>} 
   * <b>-index</b> <font color=#0000ff><i>IndexName</i></font> }+
   * </b></code></blockquote>
   * 
   * For a complete description of each command line argument, see the 
   * {@link TextIndexer} class description.
   * <br><br>
   * 
   */
  public static void main( String[] args )
  
  {
    
    try {
      
      IndexerConfig     cfgInfo          = new IndexerConfig();
      XMLConfigParser   cfgParser        = new XMLConfigParser();
      
      int     startArg  = 0;
      boolean showUsage = false;
      
      // Regardless of whether we succeed or fail, say our name.
      Trace.info( "IndexStats v" + 1.0 );
      Trace.info( "" );
      
      // Make sure the XTF_HOME environment variable is specified.
      cfgInfo.xtfHomePath = System.getProperty( "xtf.home" );
      if( cfgInfo.xtfHomePath == null || cfgInfo.xtfHomePath.length() == 0 ) {
          Trace.error( "Error: xtf.home property not found" );
          return;
      }
      
      cfgInfo.xtfHomePath = Path.normalizePath( cfgInfo.xtfHomePath );
      if( !new File(cfgInfo.xtfHomePath).isDirectory() ) {
          Trace.error( "Error: xtf.home directory \"" + cfgInfo.xtfHomePath + 
                       "\" does not exist or cannot be read." );
          return;
      }

      // Process each index
      for(;;) {
        
          // The minimum set of arguments consists of the name of an index
          // to scan. That requires two args; if we don't get that many,
          // we will show the usage text and bail.
          //
          if( args.length < 2 ) 
              showUsage = true;
          
          // We have enough arguments, so...
          else {
            
              // Read the command line arguments until we find what we
              // need to do some work, or until we run out.
              //
              int ret = cfgInfo.readCmdLine( args, startArg );
              
              // If we didn't find enough command line arguments... 
              if( ret == -1 ) {
                
                  // And this is the first time we've visited the command 
                  // line arguments, avoid trying to doing work and just 
                  // display the usage text. Otherwise, we're done.
                  //
                  if( startArg == 0 ) showUsage = true;
                  else                break;
                  
              } // if( ret == -1 )
              
              // We did find enough command line arguments, so...
              //
              else {
                  // Make sure the configuration path is absolute
                  if( !(new File(cfgInfo.cfgFilePath).isAbsolute()) ) {
                      cfgInfo.cfgFilePath = Path.resolveRelOrAbs( 
                          cfgInfo.xtfHomePath, cfgInfo.cfgFilePath);
                  }

                  // Get the configuration for the index specified by the 
                  // current command line arguments.
                  //
                  if( cfgParser.configure(cfgInfo) < 0 ) {
                      Trace.error( "Error: index '" +
                                   cfgInfo.indexInfo.indexName + 
                                   "' not found\n" );
                      return;
                  }

              } // else( ret != -1 )
              
              // Save the new start argument for the next time.
              startArg = ret;
              
          } // else( args.length >= 4 )
          
          // If the config file was read successfully, we can begin processing.
          if( showUsage ) {
              
              // Do so...
              Trace.error( "  usage: " );
              Trace.tab();
              Trace.error( "indexStats {-config <configfile>}? "  +
                           "-index <indexname>}+ \n\n" );
              Trace.untab();
              
              // And then bail.
              return;
              
          } // if( showUsage )    
           
          try {
                
              // Say what index we're working on.
              Trace.info( "Index: \"" + cfgInfo.indexInfo.indexName +"\"" );
              Trace.tab();

              // Output general information about the index
              Trace.info( "" );
              Trace.info( "Configuration Info..." );
              Trace.tab();
              
              Trace.info( "Chunk Size = "             +
                          cfgInfo.indexInfo.getChunkSize() +
                          ", Overlap = "                   +
                          cfgInfo.indexInfo.getChunkOvlp() );
              
              Trace.info( "Index Path = " +
                          Path.resolveRelOrAbs(cfgInfo.xtfHomePath, 
                                               cfgInfo.indexInfo.indexPath) );
              Trace.info( "Data Path  = " + 
                          Path.resolveRelOrAbs(cfgInfo.xtfHomePath, 
                                               cfgInfo.indexInfo.sourcePath) );
              
              Trace.info( "Stop Words = " + cfgInfo.indexInfo.stopWords );
              
              Trace.untab();
              
              // Calculate the remaining statistics
              Trace.info( "" );
              Trace.info( "Statistics..." );
              Trace.tab();

              calcStats( cfgInfo );
              
              // And print them out
              Trace.info( "Total Documents     = " + totalDocs );
              Trace.info( "Avg Chunks Per Doc  = " + 
                          totalChunks / (totalDocs+1) );
              Trace.info( "Lucene Index Size   = " + 
                          printBig(totalLuceneSize) );
              Trace.info( "Persistent Doc Size = " + 
                          printBig(totalLazySize) );
              Trace.info( "Total Index Size    = " + 
                          printBig(totalLuceneSize + totalLazySize) );
              
              Trace.info( "" );
              
              Trace.untab();
              Trace.untab();
              Trace.info( "Done." );
              Trace.info( "" );
              
          } // try to index a document
            
          catch( Exception e ) {
              
              Trace.clearTabs();
              Trace.error( "*** Last Chance Exception: " + e.getClass() );
              Trace.error( "             With message: " + e.getMessage() );
              Trace.error( "" );
              e.printStackTrace( System.out );
              
              return;
          }
          catch( Throwable t ) {
              
              Trace.clearTabs();
              Trace.error( "*** Last Chance Exception: " + t.getClass() );
              Trace.error( "" );
              t.printStackTrace( System.out );
              
              return;
          }
                    
      } // for(;;)
      
    } // try
    
    // Log any unhandled exceptions.    
    catch( Exception e ) {
        Trace.error( "*** Last Chance Exception: " + e.getClass() );
        Trace.error( "             With message: " + e.getMessage() );
        Trace.error( "" );
        e.printStackTrace( System.out );
    }
      
    catch( Throwable t ) {
        Trace.error( "*** Last Chance Exception: " + t.getClass() );
        Trace.error( "             With message: " + t );
        Trace.error( "" );
        t.printStackTrace( System.out );
    }
      
    // Exit successfully.
    return;
      
  } // main()
  
  
  //////////////////////////////////////////////////////////////////////////////
  
  private static void calcStats( IndexerConfig cfgInfo )
  
    throws IOException
  
  {
    IndexInfo idxInfo = cfgInfo.indexInfo;
      
    // Try to open the index for reading. If we fail and throw, skip the 
    // index.
    //
    String idxPath = Path.resolveRelOrAbs(cfgInfo.xtfHomePath, 
                                          idxInfo.indexPath);
    File   idxFile = new File( idxPath );
    IndexReader indexReader = IndexReader.open( idxPath );
    
    // Get a list of all the "header" chunks for documents in this
    // index (i.e., documents with a "docInfo" field.)
    //
    TermQuery     docQuery = new TermQuery( new Term("docInfo", "1") );
    
    IndexSearcher indexSearcher = new IndexSearcher( indexReader );
    Hits hits = indexSearcher.search( docQuery );
    
    // Step through each of the documents found.
    for( int i = 0; i < hits.length(); i++ ) {
        
        // Get the current document.
        Document doc = indexReader.document( hits.id(i) );
      
        // Get the key, which contains the index name and the path from its
        // source directory.
        //
        String key = doc.get( "key" );
        assert key.indexOf(':') >= 0 : "Invalid index key - missing ':'";
        String indexName = key.substring( 0, key.indexOf(':') );
        String relPath   = key.substring( key.indexOf(':') + 1 );
        
        // Skip documents that aren't part of the index we want.
        if( !indexName.equals(idxInfo.indexName) )
            continue;
        
        // Track how many documents there are.
        totalDocs++;
        
        // Track how many chunks there are.
        try {
            totalChunks += Integer.parseInt( doc.get("chunkCount") );
        }
        catch( NumberFormatException e ) {
        }
      
        // Create a reference to the source XML document.
        String sourceDir = Path.resolveRelOrAbs( cfgInfo.xtfHomePath, 
                                                 idxInfo.sourcePath );
        String currPath = Path.resolveRelOrAbs( sourceDir, relPath );
        File   currFile = new File( currPath );
        
        // Also find the size of the lazy file, if any.
        File lazyFile = 
            IdxConfigUtil.calcLazyPath( new File(cfgInfo.xtfHomePath),
                                        idxInfo, 
                                        currFile, 
                                        false );
        
        if( lazyFile.canRead() )
            totalLazySize += lazyFile.length();
    
    } // for( int i... )
    
    // Close up the index reader and searcher
    indexSearcher.close();
    indexReader.close();
    
    // Now calculate the size of the Lucene index files.
    if( idxFile.isDirectory() ) {
        
        String[] children = idxFile.list();
        for( int i = 0; i < children.length; i++ ) {
            File child = new File( idxFile, children[i] );
            if( child.isFile() )
                totalLuceneSize += child.length();
        }
    }
  
  } // calcStats()
  
  
  //////////////////////////////////////////////////////////////////////////////

  private static String printBig( long num )
  
  {
  
    float mBytes = num / 1024.0f / 1024.0f;
    mBytes = ((int) (mBytes * 100)) / 100.0f;
    return mBytes + " Mb";
      
  } // printBig()
  
  
} // class IndexStats
