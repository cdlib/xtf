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
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;

import org.cdlib.xtf.textIndexer.CrimsonBugWorkaround;
import org.cdlib.xtf.textIndexer.IndexInfo;
import org.cdlib.xtf.textIndexer.IndexerConfig;
import org.cdlib.xtf.util.DocTypeDeclRemover;
import org.cdlib.xtf.util.Path;
import org.cdlib.xtf.util.XTFSaxonErrorListener;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * This class provides methods related to, but not always part of, a text
 * index. For instance, there are methods to calculate document keys (as 
 * used in an index), or lazy file paths. It also maintains a publicly 
 * accessible cache of index info entries read from the index config file(s).
 * 
 * @author Martin Haye
 */
public class IndexUtil
{
  private static ConfigCache configCache = new ConfigCache();
  private static SAXParserFactory saxParserFactory = null;
  
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
    throws IOException
  {
    // First, load the particular index info from the config file (though if
    // we've already loaded it, the cache will just return it.)
    //
    IndexerConfig idxCfg;
    try {
        idxCfg = configCache.find(idxConfigFile, idxName);
    }
    catch( Exception e ) {
        if( e instanceof IOException )
            throw (IOException) e;
        throw new RuntimeException( e );
    }
    
    // If we couldn't find the index name, throw an exception.
    if( idxCfg.indexInfo == null || idxCfg.indexInfo.sourcePath == null )
        throw new RuntimeException( "Index name '" + idxName + 
                                    "' not found in index config file" );
    
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
    throws IOException
  {
    // First, load the particular index info from the config file (though if
    // we've already loaded it, the cache will just return it.)
    //
    IndexerConfig config;
    try {
        config = configCache.find(idxConfigFile, idxName);
    }
    catch( Exception e ) {
        if( e instanceof IOException )
            throw (IOException) e;
        throw new RuntimeException( e );
    }
    
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
  public static SAXParser createSAXParser()
  {
    // If we don't have a factory yet, make one...
    if( saxParserFactory == null ) 
    {
        // For some evil reason, Resin overrides the default transformer
        // and parser implementations with its own, deeply inferior,
        // versions. Screw that.
        //
        System.setProperty( "javax.xml.parsers.TransformerFactory",
                            "net.sf.saxon.TransformerFactoryImpl" );
        
        // Our first choice is the new parser supplied by Java 1.5.
        // Second choice is the older (but reliable) Crimson parser.
        //
        try {
            Class.forName("com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl");
            System.setProperty( "javax.xml.parsers.SAXParserFactory",
                                "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl" );
        }
        catch( ClassNotFoundException e ) {
            try {
                Class.forName("org.apache.crimson.jaxp.SAXParserFactoryImpl");
                System.setProperty( "javax.xml.parsers.SAXParserFactory",
                                    "org.apache.crimson.jaxp.SAXParserFactoryImpl" );
            }
            catch( ClassNotFoundException e2 ) {
                ; // Okay, accept whatever the default is.
            }
        }
        
        // Create a SAX parser factory.
        saxParserFactory = SAXParserFactory.newInstance();
    }
    
    // Use the parser factory to make a new parser.
    synchronized( saxParserFactory ) {
        try {
            SAXParser xmlParser = saxParserFactory.newSAXParser();
            XMLReader xmlReader = xmlParser.getXMLReader();
            xmlReader.setFeature( 
                      "http://xml.org/sax/features/namespaces", true );
            xmlReader.setFeature( 
                      "http://xml.org/sax/features/namespace-prefixes", false );
            return xmlParser;
        }
        catch( SAXException e ) {
            throw new RuntimeException( e );
        }
        catch( ParserConfigurationException e ) {
            throw new RuntimeException( e );
        }
    }
    
  } // createSaxParser() 
    
  /**
   * Applies the standard set of filters for an XML document. In our case,
   * this involves removing document type declarations, and working around
   * a bug in the Apache Crimson parser.
   * 
   * @param inStream    Document stream to filter
   * @param saxParser   Parser that will be used to parse the document
   * @param removeDoctypeDecl true to remove DOCTYPE declaration; false to
   *                          leave them alone.
   * 
   * @return            Filtered input stream
   */
  public static InputStream filterXMLDocument( InputStream inStream, 
                                               SAXParser   saxParser,
                                               boolean     removeDoctypeDecl )
  {
      // Remove DOCTYPE declarations, since the XML reader will barf if it
      // can't resolve the entity reference, and we really don't care.
      //
      if( removeDoctypeDecl )
          inStream = new DocTypeDeclRemover( inStream );
      
      // Work around a nasty bug in the Apache Crimson parser. If it
      // finds a ']' character at the end of its 8193-byte buffer,
      // and that is preceded by a '>' character then it crashes. The 
      // following filter inserts a space in such cases.
      //
      if( saxParser.getClass().getName().equals("org.apache.crimson.jaxp.SAXParserImpl") )
          inStream = new CrimsonBugWorkaround( inStream );
      
      return inStream;
  }
  
   
  public static void applyPreFilter( Templates      prefilterStylesheet,
                                     SAXParser      saxParser,
                                     InputSource    inSrc,
                                     ContentHandler contentHandler )
    throws SAXException, 
           TransformerException,
           TransformerConfigurationException
  {
    // Create an actual transform filter from the stylesheet for this
    // particular document we're indexing.
    //
    Transformer filter = prefilterStylesheet.newTransformer();
        
    // And finally, make a SAX source that combines the XML reader with
    // the filtered input source.
    //
    SAXSource srcText = new SAXSource( saxParser.getXMLReader(), inSrc );
  
    // Identify this class as the parser for the XML events that 
    // the XSLT translation will produce.
    //
    SAXResult filteredText = new SAXResult( contentHandler );
  
    // Make sure errors get directed to the right place.
    if( !(filter.getErrorListener() instanceof XTFSaxonErrorListener) )
        filter.setErrorListener( new XTFSaxonErrorListener() );

    // Perform the translation.
    filter.transform( srcText, filteredText );
    
  } // applyPreFilter()

} // class FileCalc
