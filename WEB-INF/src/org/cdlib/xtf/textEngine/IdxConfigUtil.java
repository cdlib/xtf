package org.cdlib.xtf.textEngine;

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

import java.io.File;
import java.io.IOException;

import org.cdlib.xtf.textIndexer.IndexInfo;
import org.cdlib.xtf.textIndexer.IndexerConfig;
import org.cdlib.xtf.util.Path;

/**
 * This class provides methods to calculate document keys (as used in an 
 * index), or lazy file paths. It also maintains a publicly accessible cache 
 * of index info entries read from the index config file(s).
 * 
 * @author Martin Haye
 */
public class IdxConfigUtil
{
  private static ConfigCache configCache = new ConfigCache();
  
  /**
   * Given an index configuration file and the name of an index within that file,
   * fetch the configuration info. This is a memo function, so any given index
   * name will be cached and thus only loaded once.
   * 
   * @param idxConfigFile   Index configuration file to read
   * @param idxName         Name of the index within that file
   *
   * @return                Information for the specified index.
   * @throws Exception      If there is a problem reading the config file.
   */
  public static IndexInfo getIndexInfo( File idxConfigFile, String idxName )
    throws Exception
  {
    return configCache.find(idxConfigFile, idxName).indexInfo;
  } // getIndexInfo()
  
  /**
   * Given an index within a config file and the path to the source XML text
   * of a document, this method infers the correct path to the lazy version
   * of that source document. The lazy version will be somewhere within the
   * index's directory.
   * 
   * @param idxConfigFile       File to load index configuration from
   * @param idxName             Index name within the config
   * @param srcTextFile         Source text file of interest
   * @param createDir           true to create the directory for the lazy file
   *                            if it doesn't exist; false to never create the
   *                            directory.
   * 
   * @return                    Expected location of the lazy version of the 
   *                            source file
   *    
   * @throws Exception          If the config file cannot be loaded, or the
   *                            paths are invalid.        
   */
  public static File calcLazyPath( File    xtfHome,
                                   File    idxConfigFile,
                                   String  idxName,
                                   File    srcTextFile,
                                   boolean createDir )
    throws Exception
  {
    // First, load the particular index info from the config file (though if
    // we've already loaded it, the cache will just return it.)
    //
    IndexerConfig idxCfg = 
        configCache.find(idxConfigFile, idxName);
    
    // Use the other form of calcLazyPath() to do the rest of the work.
    return calcLazyPath( xtfHome, idxCfg.indexInfo,
                         srcTextFile, createDir );
      
  } // public calcLazyPath()

  /**
   * Given an index within a config file and the path to the source XML text
   * of a document, this method infers the correct path to the lazy version
   * of that source document. The lazy version will be somewhere within the
   * index's directory.
   * 
   * @param xtfHome         File at the root of the XTF directory tree
   * @param idxInfo         Configuration info for the index in question.
   * @param srcTextFile     Source text file of interest
   * @param createDir       true to create the directory for the lazy file
   *                        if it doesn't exist; false to never create the
   *                        directory.
   * 
   * @return                Expected location of the lazy version of the 
   *                        source file
   *    
   * @throws Exception      If the config file cannot be loaded, or the
   *                        paths are invalid.        
   */
  public static File calcLazyPath( File      xtfHome,
                                   IndexInfo idxInfo,
                                   File      srcTextFile,
                                   boolean   createDir )
    throws IOException
  {
    // Figure out the part of the source file's path that matches the index
    // data directory.
    //
    String fullSourcePath = Path.resolveRelOrAbs( xtfHome.toString(),
                                                  idxInfo.sourcePath );
    String prefix = Path.calcPrefix( srcTextFile.getParent(),
                                     fullSourcePath.toString() );
    if( prefix == null ) {
        throw new IOException( "XML source file " + srcTextFile +
                               " is not contained within " + 
                               idxInfo.sourcePath );
    }
    
    // Form the result by adding the non-overlapping part to the 'lazy'
    // directory within the index directory.
    //
    String srcTextPath = Path.normalizeFileName(srcTextFile.toString());
    String after = srcTextPath.substring( prefix.length() );
    String lazyPath = idxInfo.indexPath + "lazy/" + 
                      idxInfo.indexName + "/" + after +
                      ".lazy";
    lazyPath = Path.resolveRelOrAbs(xtfHome.toString(), lazyPath);
    File lazyFile = new File(lazyPath);
    
    // If we've been asked to create the directory, do it now.
    if( createDir )
        Path.createPath( lazyFile.getParentFile().toString() );

    // And we're done.
    return lazyFile;
      
  } // public calcLazyPath()

  /**
   * Given an index within a config file and the path to the source XML text
   * of a document, this method infers the correct document key that should be
   * stored in the index.
   * 
   * @param idxConfigFile       File to load index configuration from
   * @param idxName             Index name within the config
   * @param srcTextFile         Source text file of interest
   * 
   * @return                    Document key to store or look for in the index
   *    
   * @throws Exception          If the config file cannot be loaded, or the
   *                            paths are invalid.        
   */
  public static String calcDocKey( File    xtfHome,
                                   File    idxConfigFile,
                                   String  idxName,
                                   File    srcTextFile )
    throws Exception
  {
    // First, load the particular index info from the config file (though if
    // we've already loaded it, the cache will just return it.)
    //
    IndexerConfig config = configCache.find(idxConfigFile, idxName);
    
    // Use the other form of calcDocKey() to do the rest of the work.
    return calcDocKey( xtfHome,
                       config.indexInfo, 
                       srcTextFile );
    
  } // calcDocKey()
  
  /**
   * Given an index within a config file and the path to the source XML text
   * of a document, this method infers the correct document key that should be
   * stored in the index.
   * 
   * @param xtfHomeFile     The XTF_HOME directory
   * @param idxInfo         Configuration info for the index in question.
   * @param srcTextFile     Source text file of interest
   * 
   * @return                Document key to store or look for in the index
   *    
   * @throws Exception      If the config file cannot be loaded, or the
   *                        paths are invalid.        
   */
  public static String calcDocKey( File      xtfHomeFile,
                                   IndexInfo idxInfo,
                                   File      srcTextFile )
      throws IOException
  {

    // Figure out the part of the source file's path that matches the index
    // data directory.
    //
    String fullSourcePath = Path.resolveRelOrAbs(
                                    xtfHomeFile, idxInfo.sourcePath );
    String prefix = Path.calcPrefix( srcTextFile.getParent(), fullSourcePath );
    if( prefix == null ) {
        throw new IOException( "XML source file " + srcTextFile +
                               " is not contained within " + 
                               idxInfo.sourcePath );
    }
    
    // Form the result using the index name and the non-overlapping part.
    String srcTextPath = Path.normalizeFileName(srcTextFile.toString());
    String after = srcTextPath.substring( prefix.length() );
    String key = idxInfo.indexName + ":" + after;
    
    // And we're done.
    return key;
      
  } // calcDocKey()
  
} // class FileCalc
