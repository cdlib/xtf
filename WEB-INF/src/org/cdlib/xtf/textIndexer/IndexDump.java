package org.cdlib.xtf.textIndexer;

/**
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

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.cdlib.xtf.textEngine.Constants;
import org.cdlib.xtf.util.Path;
import org.cdlib.xtf.util.Trace;

/**
 * This class dumps the contents of user-selected fields from an XTF text
 * index.
 * 
 * @author Martin Haye
 */
public class IndexDump 
{
    
  //////////////////////////////////////////////////////////////////////////////
  
  /** Main entry-point for the index dumper. <br><br>
   * 
   *  This function takes the command line arguments passed and uses them to
   *  find an index and print out fields in it.
   */
  public static void main( String[] args )
  
  {
    
    try {
      
      IndexerConfig     cfgInfo          = new IndexerConfig();
      XMLConfigParser   cfgParser        = new XMLConfigParser();
      
      int     startArg  = 0;
      boolean showUsage = false;
      
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
          // to scan and a field to dump. That requires three args; if we don't 
          // get that many, we will show the usage text and bail.
          //
          if( args.length < 4 ) 
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
          
          // Find all the field names that should be dumped.
          Vector fieldNames = new Vector();
          while( startArg < args.length &&
                 args[startArg].equals("-field") )
          {
              startArg++;
              if( startArg == args.length || args[startArg].startsWith("-") )
                  showUsage = true;
              else {
                  if( args[startArg].equals("text") ) {
                      Trace.error( "Error: the 'text' field cannot be dumped" );
                      System.exit( 1 );
                  }
                  fieldNames.add( args[startArg] );
                  startArg++;
              }
          }
          if( fieldNames.isEmpty() )
              showUsage = true;
          
          // If the config file was read successfully, we can begin processing.
          if( showUsage ) {
              
              // Do so...
              Trace.error( "  usage: " );
              Trace.tab();
              Trace.error( "indexDump {-config <configfile>}? "  +
                           "-index <indexname> " + 
                           "-field fieldName1 {-field fieldName2}*... \n\n" );
              Trace.untab();
              
              // And then bail.
              System.exit( 1 );
              
          } // if( showUsage )    
           
          // Go for it.
          dumpFields( cfgInfo, fieldNames );
        
      } // for(;;)
      
    } // try
    
    // Log any unhandled exceptions.    
    catch( Exception e ) {
        Trace.error( "*** Last Chance Exception: " + e.getClass() );
        Trace.error( "             With message: " + e.getMessage() );
        Trace.error( "" );
        e.printStackTrace( System.out );
        System.exit( 1 );
    }
      
    catch( Throwable t ) {
        Trace.error( "*** Last Chance Exception: " + t.getClass() );
        Trace.error( "             With message: " + t );
        Trace.error( "" );
        t.printStackTrace( System.out );
        System.exit( 1 );
    }
      
    // Exit successfully.
    System.exit( 0 );
      
  } // main()
  
  
  //////////////////////////////////////////////////////////////////////////////
  
  private static void dumpFields( IndexerConfig cfgInfo, Vector fieldVec )
  
    throws IOException
  
  {
    IndexInfo idxInfo = cfgInfo.indexInfo;
    
    String[] fields = (String[]) fieldVec.toArray( new String[fieldVec.size()] );
    
    // Try to open the index for reading. If we fail and throw, skip the 
    // index.
    //
    String idxPath = Path.resolveRelOrAbs(cfgInfo.xtfHomePath, 
                                          idxInfo.indexPath);
    File   idxFile = new File( idxPath );
    IndexReader indexReader = IndexReader.open( idxPath );

    // Iterate every document.
    Field[] empty = new Field[0];
    int maxDoc = indexReader.maxDoc();
    for( int i = 0; i < maxDoc; i++ ) 
    {
        if( indexReader.isDeleted(i) )
            continue;
        
        // See if any of the desired fields are present, and if so, record
        // their values.
        //
        Document doc = indexReader.document( i );
        Vector toPrint = new Vector();
        boolean gotAny = false;
        for( int j = 0; j < fields.length; j++ ) {
            Field got = doc.getField( fields[j] );
            if( got == null ) {
                toPrint.add( "" );
                continue;
            }
            
            toPrint.add( stripValue(got.stringValue()) );
            gotAny = true;
        }
        
        // If we got any values, print out all of them (even the empties.)
        if( gotAny ) {
            for( int j = 0; j < toPrint.size(); j++ ) {
                System.out.print( (String) toPrint.get(j) );
                System.out.print( "|" );
            }
            System.out.print( "\n" );
        }
    }
    
    // Close the term enumeration and reader.
    indexReader.close();
    indexReader = null;
    
  } // dumpFields()

  
  //////////////////////////////////////////////////////////////////////////////
  
  /**
   * Removes XTF's special characters (such as bump markers and field start/end
   * markers) from the input string. Also changes characters we use for
   * field and value markers ('|' and ';') to something else so they won't
   * be taken for markers.
   */
  private static String stripValue( String str ) 
  {
    char[] in  = str.toCharArray();
    char[] out = new char[in.length];
    int outLen = 0;
    
    for( int i = 0; i < in.length; i++ ) {
        switch( in[i] ) {
        case Constants.FIELD_START_MARKER:
        case Constants.FIELD_END_MARKER:
        case Constants.NODE_MARKER:
            break;
        case Constants.BUMP_MARKER:
            i++;
            while( i < in.length && in[i] != Constants.BUMP_MARKER )
                i++;
            if( i < in.length )
                i++;
            out[outLen++] = ';';
            break;
        case ';':
            out[outLen++] = ',';
            break;
        case '|':
            out[outLen++] = '.';
            break;
        default:
            out[outLen++] = in[i];
            break;
        }
    } // for i
    
    return new String( out, 0, outLen );
  } // stripValue()
  
} // class IndexDump
