package org.cdlib.xtf.textIndexer;

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

import org.cdlib.xtf.util.*;


////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

/**
 * This class records configuration information about the current state of 
 * the TextIndexer application. <br><br>
 * 
 * The main TextIndexer class uses this class to maintain information about its 
 * current processing state. This information includes: <br><br>
 * - The path to the configuration file that defines indices to be created or
 *   updated. <br>
 * - The output (trace) level to display while processing indices. <br>
 * - Whether or not the current index should be rebuilt from scratch (clean 
 *   build).<br>
 * - Whether or not the indexes should be optimized after being built.<br>
 * - The source text and index database directories to use for the current 
 *   index being processed. (This information is actually stored in a separate
 *   IndexInfo sub-structure within IndexerConfig.) <br> 
 * <br><br>
 */

public class IndexerConfig 

{ 
  /** Path of the XTF home directory */
  public String xtfHomePath;
  
  /** Path to the config file. */
  public String cfgFilePath;
  
  /** Trace level to output. Should be one or more of the trace constants 
   *  defined by the class org.cdlib.xtf.util.Trace .
   */
  public int traceLevel;
  
  /** Flag indicating whether or not to build index from scratch or 
   *  incrementally. <br><br>
   *  
   *  true  = Build index from scratch.  <br>
   *  false = Build index incrementally. <br><br>
   */
  public boolean   clean;
  
  /** Flag indicating whether or not index must still be cleaned or not. Set
   *  to the value of clean just prior to processing each index listed in the
   *  config file. <br><br>
   *  
   *  true  = Build index from scratch.  <br>
   *  false = Build index incrementally. <br><br>
   */
  public boolean   mustClean;
  
  /** Flag indicating whether to build lazy files during the indexing process.
   *  <br><br>
   *  
   *  true  = Build lazy files. <br>
   *  False = Do not build lazy files. <br><br>
   */
  public boolean   buildLazyFiles;
  
  /** Flag indicating whether or not to optimize the index after building it.
   *  <br><br>
   *  
   *  true  = Optimize index after building.  <br>
   *  false = Do not optimize index. <br><br>
   */
  public boolean   optimize;
  
  /** 
   * Flag indicating whether or not to build spelling dictionaries for 
   * the index after building it.
   * <br><br>
   *  
   *  true  = Build spelling dictionaries for index after building.  <br>
   *  false = Do not build spelling dictionaries index. <br><br>
   */
  public boolean   updateSpellDict;
  
  /** 
   * Flag indicating whether or not to skip the main indexing pass. Useful
   * for debugging later phases, such as optimization or spelling.
   * <br><br>
   *  
   *  true  = Skip the main indexing pass.  <br>
   *  false = Perform the main indexing pass. <br><br>
   */
  public boolean   skipIndexing;
  
  /** Index specific information for the current index being created or 
   *  updated.
   */
  public IndexInfo indexInfo;

  //////////////////////////////////////////////////////////////////////////// 
  
  /** Default constructor. <br><br>
   *  
   *  Initializes data members for this calls to reasonable default values.
   *  <br><br>
   */
  
  public IndexerConfig()
  
  {
    
    // Default to the XTF conf directory for the config file Path.
    cfgFilePath = "conf/textIndexer.conf";
    
    // Default to incrementally updating the index. 
    clean = false;
    
    // Default to building lazy files during the run.
    buildLazyFiles = true;
    
    // Default to always optimizing the index.
    optimize = true;
    
    // Default to making spellcheck dictionary (if enabled in the index's info)
    updateSpellDict = true;
    
    // Default to performing the main indexing pass
    skipIndexing = false;
    
    // Set the default trace level to display errors.
    traceLevel = Trace.info;
    
    // Set defaults for the index info structure.
    indexInfo = new IndexInfo();
  
  } // IndexerConfig()
  
  
  //////////////////////////////////////////////////////////////////////////// 
  
  /** Processes command line arguments to set the corresponding data members 
   *  in this class.
   *  <br><br>
   * 
   *  @param args      A string containing the command line arguments passed to
   *                   the text indexer.
   * 
   *  @param startArg  The character index at which to begin processing the next
   *                   command line argument. 
   * 
   *  @return          The character index at which to resume command line 
   *                   argument processing the next time this function is 
   *                   called.
   * 
   *  @.notes          This function looks for the following command line 
   *                   flags: <br><br>
   * 
   *                   -config {path}  (exactly one required)<br>   
   *                    The path/name of the configuration file describing 
   *                    the index(s) to process. <br><br>
   * 
   *                   -clean  (optional)<br>   
   *                    A flag indicating that the index names that follow 
   *                    should be rebuilt from scratch. If not specified, the
   *                    index names that follow will be added to or updated
   *                    incrementally. <br><br>
   *
   *                   -incremental  (optional)<br>   
   *                    A flag indicating that the index names that follow 
   *                    should be added to or updated incrementally. If this 
   *                    flag and the -clean flag are both omitted, incremental
   *                    updating will be used by default. <br><br>
   *
   *                   -index {name}  (one or more required)<br>   
   *                    The name of an index defined in the configuration file
   *                    to create or update. <br><br>
   * 
   *                   -trace [errors | warnings | info | debug]  (optional)<br>   
   *                    Identifies the level of output the indexer should 
   *                    echo back to the user. If not specified, info level
   *                    output is used as the default. <br><br>
   */

  public int readCmdLine( String[] args, int startArg )
  
  {
    int i;
    
    // If there aren't any command line arguments left to check,
    // tell the caller we didn't find the necessary info to
    // continue.
    //
    if( startArg >= args.length ) return -1;
    
    // Assume we haven't read the necessary arguments yet.
    boolean gotIdxName = false;
    
    // Start with no sub-directory selected.
    indexInfo.subDir = null;
    
    // Process the command line arguments based on where we left off
    // last time (if there was a last time.)
    //
    for( i = startArg; i < args.length; i++ ) {
      
        // If we found the -config argument...
        if( args[i].equalsIgnoreCase("-config") ) {  
            
            // And there aren't any more arguments, tell the caller
            // that we failed to get enough info to continue.
            //
            if( ++i >= args.length ) return -1;              
            
            // Otherwise, pickup the Path/name of config file file
            // to use from the next argument, and flag that we have
            // it.
            //
            cfgFilePath = args[i];
        }
        
        // If we found the -index argument...
        else if( args[i].equalsIgnoreCase("-index") ) {  
          
            // And there aren't any more arguments, tell the caller
            // that we failed to get enough info to continue.
            //
            if( ++i >= args.length ) return -1;           
            
            // Otherwise, pick up the index name, and flag that we
            // found it.
            //
            indexInfo.indexName = args[i];
            gotIdxName = true;
        }
        
        // If the user wants to specify that indexing should apply only
        // to a specified sub-directory, record that info now.
        //
        else if( args[i].equalsIgnoreCase("-dir") ) 
        {
            // If there aren't any more arguments, tell the caller
            // that we failed to get enough info to continue.
            //
            if( ++i >= args.length ) return -1;           
            
            // This should only be specified max once per index.
            if( indexInfo.subDir != null ) {
                Trace.error( "Error: Only one directory may be specified per index" );
                return -1;
            }
            
            indexInfo.subDir = args[i];
        }
        // If the user asked for a clean index, flag it.
        else if( args[i].equalsIgnoreCase("-clean") ) 
            clean  = true;
        
        // If the user asked for an incremental index update, flag it.  
        else if( args[i].equalsIgnoreCase("-incremental") ) 
            clean  = false;
        
        // If the user asked for optimization after build, flag it.
        else if( args[i].equalsIgnoreCase("-optimize") )
              optimize = true;
        
        // If the user asked for no optimization after build, flag it.
        else if( args[i].equalsIgnoreCase("-nooptimize") )
              optimize = false;
        
        // If the user asked for optimization after build, flag it.
        else if( args[i].equalsIgnoreCase("-updatespell") )
              updateSpellDict = true;
        
        // If the user asked for no optimization after build, flag it.
        else if( args[i].equalsIgnoreCase("-noupdatespell") )
              updateSpellDict = false;
        
        // If the user asked for lazy files to be built, flag it.
        else if( args[i].equalsIgnoreCase("-buildlazy") )
              buildLazyFiles = true;
        
        // If the user asked for no lazy files to be built, flag it.
        else if( args[i].equalsIgnoreCase("-nobuildlazy") )
              buildLazyFiles = false;
        
        // If the user asked for us to skip the main indexing pass, flat it.
        else if( args[i].equalsIgnoreCase("-skipindexing") )
              skipIndexing = true;
        
        // If we found the -trace argument...
        else if( args[i].equalsIgnoreCase("-trace") ) {  
          
            // And there aren't any more arguments, tell the caller
            // that we failed to get enough info to continue.
            //
            if( ++i >= args.length ) return -1;              
            
            // Otherwise, pickup the trace level to use.
            String traceLevelStr = args[i];
            
            // Convert the trace level sting from the command line argument
            // into the equivalent constant.
            //
            if( traceLevelStr.equalsIgnoreCase("warnings") )
                traceLevel = Trace.warnings; 
            else if( traceLevelStr.equalsIgnoreCase("info") )
                traceLevel = Trace.info; 
            else if( traceLevelStr.equalsIgnoreCase("debug") )
                traceLevel = Trace.debug; 
            else 
                traceLevel = Trace.errors; 
        }
        else {
            Trace.error( "Unrecognized command-line parameter: " + 
                         args[i] );
            return -1;
        }

        // If we got to this point and have the index name, we're ready to go.
        if( gotIdxName ) { i++; break; }
    
    } // for(;;)
    
    // If we didn't get the index name, bail.
    if( !gotIdxName ) return -1;
    
    // Otherwise, return the index of the command line argument to 
    // resume processing at the next time.
    //
    return i;
    
  } // public readCmdLine()
  
} // class IndexerConfig
