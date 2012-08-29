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
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import net.sf.saxon.Filter;
import net.sf.saxon.value.StringValue;

import org.cdlib.xtf.saxonExt.sql.SQLConnect;
import org.cdlib.xtf.textIndexer.CrimsonBugWorkaround;
import org.cdlib.xtf.textIndexer.IndexInfo;
import org.cdlib.xtf.textIndexer.IndexerConfig;
import org.cdlib.xtf.util.Attrib;
import org.cdlib.xtf.util.AttribList;
import org.cdlib.xtf.util.DocTypeDeclRemover;
import org.cdlib.xtf.util.Path;
import org.cdlib.xtf.util.XTFSaxonErrorListener;
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
  private static TransformerFactory transformerFactory = null;

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
  public static IndexInfo getIndexInfo(File idxConfigFile, String idxName)
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
   */
  public static File calcLazyPath(File xtfHome, File idxConfigFile,
                                  String idxName, File srcTextFile,
                                  boolean createDir)
    throws IOException 
  {
    // First, load the particular index info from the config file (though if
    // we've already loaded it, the cache will just return it.)
    //
    IndexerConfig idxCfg;
    try {
      idxCfg = configCache.find(idxConfigFile, idxName);
    }
    catch (Exception e) {
      if (e instanceof IOException)
        throw (IOException)e;
      throw new RuntimeException(e);
    }

    // If we couldn't find the index name, throw an exception.
    if (idxCfg.indexInfo == null || idxCfg.indexInfo.sourcePath == null)
      throw new RuntimeException(
        "Index name '" + idxName + "' not found in index config file");

    // Use the other form of calcLazyPath() to do the rest of the work.
    return calcLazyPath(xtfHome, idxCfg.indexInfo, srcTextFile, createDir);
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
   */
  public static File calcLazyPath(File xtfHome, IndexInfo idxInfo,
                                  File srcTextFile, boolean createDir)
    throws IOException 
  {
    // Figure out the part of the source file's path that matches the index
    // data directory.
    //
    String sourcePath;
    if (idxInfo.cloneData && srcTextFile.toString().contains("/dataClone/"))
      sourcePath = Path.normalizePath(idxInfo.indexPath) + "dataClone/" + idxInfo.indexName + "/";
    else
      sourcePath = idxInfo.sourcePath;
    String fullSourcePath = Path.resolveRelOrAbs(xtfHome.toString(), sourcePath);
    String prefix = Path.calcPrefix(srcTextFile.getParent(),
                                    fullSourcePath.toString());
    if (prefix == null) {
      throw new IOException(
        "XML source file " + srcTextFile + " is not contained within " +
        idxInfo.sourcePath);
    }

    // Form the result by adding the non-overlapping part to the 'lazy'
    // directory within the index directory.
    //
    String srcTextPath = Path.normalizeFileName(srcTextFile.toString());
    String after = srcTextPath.substring(prefix.length());
    String lazyPath = idxInfo.indexPath + "lazy/" + idxInfo.indexName + "/" +
                      after + ".lazy";
    lazyPath = Path.resolveRelOrAbs(xtfHome.toString(), lazyPath);
    File lazyFile = new File(lazyPath);

    // If we've been asked to create the directory, do it now.
    if (createDir) {
      if (!Path.createPath(lazyFile.getParentFile().toString()))
        throw new IOException("Error creating lazy file path");
    }

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
   */
  public static String calcDocKey(File xtfHome, File idxConfigFile,
                                  String idxName, File srcTextFile)
    throws IOException 
  {
    // First, load the particular index info from the config file (though if
    // we've already loaded it, the cache will just return it.)
    //
    IndexerConfig config;
    try {
      config = configCache.find(idxConfigFile, idxName);
    }
    catch (Exception e) {
      if (e instanceof IOException)
        throw (IOException)e;
      throw new RuntimeException(e);
    }

    // Use the other form of calcDocKey() to do the rest of the work.
    return calcDocKey(xtfHome, config.indexInfo, srcTextFile);
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
   */
  public static String calcDocKey(File xtfHomeFile, IndexInfo idxInfo,
                                  File srcTextFile)
    throws IOException 
  {
    // Figure out the part of the source file's path that matches the index
    // data directory.
    //
    String sourcePath;
    if (idxInfo.cloneData && srcTextFile.toString().contains("/dataClone/"))
      sourcePath = Path.normalizePath(idxInfo.indexPath) + "dataClone/" + idxInfo.indexName + "/";
    else
      sourcePath = idxInfo.sourcePath;
    
    String fullSourcePath = Path.resolveRelOrAbs(xtfHomeFile, sourcePath);
    String prefix = Path.calcPrefix(srcTextFile.getParent(), fullSourcePath);
    if (prefix == null) {
      throw new IOException(
        "XML source file " + srcTextFile + " is not contained within " +
        sourcePath);
    }

    // Form the result using the index name and the non-overlapping part.
    String srcTextPath = Path.normalizeFileName(srcTextFile.toString());
    String after = srcTextPath.substring(prefix.length());
    String key = idxInfo.indexName + ":" + after;

    // And we're done.
    return key;
  } // calcDocKey()

  /**
   * Create a SAX parser using the best implementation we can find. We prefer
   * the new parser supplied by Java 1.5. Failing that, we try for the Crimson
   * parser, and if that's not found, we try the default.
   */
  public static SAXParser createSAXParser() 
  {
    // If we don't have a factory yet, make one...
    if (saxParserFactory == null) 
    {
      // Our first choice is the new parser supplied by Java 1.5.
      // Second choice is the older (but reliable) Crimson parser.
      //
      try 
      {
        Class factoryClass = Class.forName(
          "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl");
        saxParserFactory = (SAXParserFactory)factoryClass.newInstance();
      }
      catch (ClassNotFoundException e) {
        try 
        {
          Class factoryClass = Class.forName(
            "org.apache.crimson.jaxp.SAXParserFactoryImpl");
          saxParserFactory = (SAXParserFactory)factoryClass.newInstance();
        }
        catch (ClassNotFoundException e2) {
          // Okay, accept whatever the default is.
          saxParserFactory = SAXParserFactory.newInstance();
        }
        catch (InstantiationException e2) {
          throw new RuntimeException(e2);
        }
        catch (IllegalAccessException e2) {
          throw new RuntimeException(e2);
        }
      }
      catch (InstantiationException e) {
        throw new RuntimeException(e);
      }
      catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    // Use the parser factory to make a new parser.
    synchronized (saxParserFactory) 
    {
      try {
        SAXParser xmlParser = saxParserFactory.newSAXParser();
        XMLReader xmlReader = xmlParser.getXMLReader();
        xmlReader.setFeature("http://xml.org/sax/features/namespaces", true);
        xmlReader.setFeature("http://xml.org/sax/features/namespace-prefixes",
                             false);
        
        // For speed, and to make indexing utterly reliable, don't load external
        // DTDs. If this fails, we ignore it (at least we tried.)
        //
        try {
          xmlReader.setFeature(
              "http://apache.org/xml/features/nonvalidating/load-external-dtd",
              false);
        }
        catch (SAXException err) {
        }
        
        // All done
        return xmlParser;
      }
      catch (SAXException e) {
        throw new RuntimeException(e);
      }
      catch (ParserConfigurationException e) {
        throw new RuntimeException(e);
      }
    }
  } // createSaxParser() 

  /**
   * Create an XML reader using the best implementation we can find. We prefer
   * the new parser supplied by Java 1.5. Failing that, we try for the Crimson
   * parser, and if that's not found, we try the default.
   */
  public static XMLReader createXMLReader() 
  {
    try {
      SAXParser parser = createSAXParser();
      return parser.getXMLReader();
    }
    catch (SAXException e) {
      throw new RuntimeException(e);
    }
  } // createXMLReader()

  /**
   * Get a TransformerFactory.
   */
  private static TransformerFactory getTransformerFactory() 
  {
    // If we don't have a factory yet, make one.
    if (transformerFactory == null)
      transformerFactory = new net.sf.saxon.TransformerFactoryImpl();

    return transformerFactory;
  } // getTransformerFactory()

  /**
   * Create a Saxon transformer.
   */
  public static Transformer createTransformer() 
  {
    // Make the new transformer that was requested.
    try {
      return getTransformerFactory().newTransformer();
    }
    catch (TransformerConfigurationException e) {
      throw new RuntimeException(e);
    }
  } // createTransformer()

  /**
   * Applies the standard set of filters for an XML document. In our case,
   * this involves removing document type declarations, and working around
   * a bug in the Apache Crimson parser.
   *
   * @param inStream    Document stream to filter
   * @param applyCrimsonWorkaround true to apply the workaround for the
   *                               8193-byte bug in the Crimson XML parser.
   * @param removeDoctypeDecl true to remove DOCTYPE declaration; false to
   *                          leave them alone.
   *
   * @return            Filtered input stream
   */
  public static InputStream filterXMLDocument(InputStream inStream,
                                              boolean applyCrimsonWorkaround,
                                              boolean removeDoctypeDecl) 
  {
    // Remove DOCTYPE declarations, since the XML reader will barf if it
    // can't resolve the entity reference, and we really don't care.
    //
    if (removeDoctypeDecl)
      inStream = new DocTypeDeclRemover(inStream);

    // Work around a nasty bug in the Apache Crimson parser. If it
    // finds a ']' character at the end of its 8193-byte buffer,
    // and that is preceded by a '>' character then it crashes. The 
    // following filter inserts a space in such cases.
    //
    if (applyCrimsonWorkaround)
      inStream = new CrimsonBugWorkaround(inStream);

    return inStream;
  }

  /**
   * Applies the standard set of filters for an XML document. In our case,
   * this involves removing document type declarations, and working around
   * a bug in the Apache Crimson parser.
   *
   * @param inStream    Document stream to filter
   * @param saxParser   Parser that will be used to parse the document; used
   *                    to determine whether or not to apply the Crimson
   *                    parser workaround.
   * @param removeDoctypeDecl true to remove DOCTYPE declaration; false to
   *                          leave them alone.
   *
   * @return            Filtered input stream
   */
  public static InputStream filterXMLDocument(InputStream inStream,
                                              SAXParser saxParser,
                                              boolean removeDoctypeDecl) 
  {
    boolean applyCrimsonWorkaround = saxParser.getClass().getName().equals(
      "org.apache.crimson.jaxp.SAXParserImpl");

    return filterXMLDocument(inStream, applyCrimsonWorkaround, removeDoctypeDecl);
  }

  /**
   * Apply one or more prefilter stylesheets to an XML input source. Pass the
   * filtered data to to the specified Result.
   *
   * @param prefilterStylesheets    Stylesheets to process
   * @param reader                  Reader to use for parsing the input XML
   * @param xmlSource               Source of XML data
   * @param ultimateResult          Where to send the output
   */
  public static void applyPreFilters(Templates[] prefilterStylesheets,
                                     XMLReader reader, InputSource xmlSource,
                                     AttribList passThroughAttribs,
                                     Result ultimateResult)
    throws SAXException, TransformerException, TransformerConfigurationException 
  {
    assert prefilterStylesheets.length > 0 : "applyPrefilters must have at least one stylesheet";

    XMLReader lastInChain = reader;
    SAXTransformerFactory stf = (SAXTransformerFactory)getTransformerFactory();

    // Process each prefilter.
    for (int i = 0; i < prefilterStylesheets.length; i++) 
    {
      // Create an XMLFilter from the stylesheet
      Filter filter = (Filter)stf.newXMLFilter(prefilterStylesheets[i]);
      Transformer trans = filter.getTransformer();
      
      // Give it the pass-through attributes.
      if (passThroughAttribs != null)
      {
        for (Iterator iter = passThroughAttribs.iterator(); iter.hasNext();) {
          Attrib a = (Attrib)iter.next();
          if (a.value == null || a.value.length() == 0)
            continue;
          trans.setParameter(a.key, new StringValue(a.value));
        }
      }

      // Make sure errors get directed to the right place.
      if (!(trans.getErrorListener() instanceof XTFSaxonErrorListener))
        trans.setErrorListener(new XTFSaxonErrorListener());

      // Hook up its input.
      filter.setParent(lastInChain);

      // Onward.
      lastInChain = filter;
    } // for i

    // Set up the transformer to process the SAX events generated
    // by the last filter in the chain.
    //
    Transformer transformer = stf.newTransformer();
    SAXSource transformSource = new SAXSource(lastInChain, xmlSource);
    transformer.transform(transformSource, ultimateResult);

    // If any SQL connections were opened during the transformation, close
    // them now.
    //
    SQLConnect.closeThreadConnections();
  } // applyPreFilter()
} // class FileCalc
