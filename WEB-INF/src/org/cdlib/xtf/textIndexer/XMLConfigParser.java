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

//import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.util.zip.GZIPInputStream;

// import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.cdlib.xtf.textEngine.IndexUtil;
import org.cdlib.xtf.util.Path;
import org.cdlib.xtf.util.Trace;

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

/**
 * This class parses TextIndexer configuration XML files. <br><br>
 *
 * The TextIndexer uses a configuration file that describes one or more index
 * names. Each index description identifies the source text and Lucene database
 * directories associated with the index, and the chunk size and overlap for
 * the index. <br><br>
 *
 * The format of the configuration file is as follows:
 *
 * <code><blockquote dir=ltr style="MARGIN-RIGHT: 0px">
 * <b>&lt;?xml version="1.0" encoding="utf-8"?&gt;</b><br>
 * <b>&lt;textIndexer-config&gt;</b><br><br>
 *
 *  <blockquote dir=ltr style="MARGIN-RIGHT: 0px">
 *  <b>&lt;index name="</b><font color=#0000ff><i>IndexName</i></font><b>"&gt; </b>
 *    <blockquote dir=ltr style="MARGIN-RIGHT: 0px">
 *    <b>&lt;db path="</b><font color=#0000ff><i>LuceneIndexPath</i></font><b>"/&gt; </b> <br>
 *    <b>&lt;src path="</b><font color=#0000ff><i>XMLSourcePath</i></font><b>"/&gt; </b> <br>
 *    <b>&lt;chunk size="</b><font color=#0000ff><i>ChunkSize</i></font><b>"
 *             overlap="</b><font color=#0000ff><i>ChunkOverlap</i></font><b>"/&gt; </b> <br>
 *    <b>&lt;skip files= "</b><font color=#0000ff><i>*.xxx*, *.yyy, ... </i></font><b>"/&gt; </b><br>
 *    <b>&lt;inputfilter path="</b><font color=#0000ff><i>XSLPreFilterFile</i></font><b>"/&gt;</b><br><br>
 *    </blockquote>
 *  <b>&lt;/index&gt; </b><br><br>
 *
 *  </blockquote>
 *
 *  <b>&lt;/textIndexer-config&gt;</b><br>
 * </blockquote>
 * </blockquote>
 * </code>
 *
 * The arguments should appear at most once for each index specified. If
 * multiple instances of the arguments are specified for an index, the
 * last one is used. <br><br>
 *
 * A simple example of a TextIndexer config file might look as follows:
 * <br><br>
 *
 * <code>
 * <blockquote dir=ltr style="MARGIN-RIGHT: 0px"><b>
 * &lt;?xml version="1.0" encoding="utf-8"?&gt; <br>
 * &lt;textIndexer-config&gt;
 *
 *   <blockquote dir=ltr style="MARGIN-RIGHT: 0px">
 *   &lt;index name="AllText"&gt;
 *     <blockquote dir=ltr style="MARGIN-RIGHT: 0px">
 *     &lt;db path="./IndexDBs"/&gt; <br>
 *     &lt;src path="./SourceText"/&gt; <br>
 *     &lt;chunk size="100" overlap="50"/&gt; <br>
 *     &lt;skip files="*.mets*, *AuthMech*"/&gt; <br>
 *     &lt;inputfilter path="./BasicFilter.xsl"/&gt;
 *   </blockquote>
 *   &lt;/index&gt; <br><br>
 *
  *   </blockquote>
 *
 * &lt;/textIndexer-config&gt;
 * </blockquote></b>
 * </code>
 *
 * @.notes
 *
 * This class is derived from the SAX {@link org.xml.sax.helpers.DefaultHandler} class so that
 * its {@link XMLConfigParser#startElement(String,String,String,Attributes) startElement()}
 * and {@link XMLConfigParser#endElement(String,String,String) endElement()}
 * methods can be called internally from the Java {@link javax.xml.parsers.SAXParser}
 * class. <br><br>
 *
 * To use this class, simply instantiate a copy, and then call its
 * {@link XMLConfigParser#configure(IndexerConfig) configure()} method.<br><br>
 */
public class XMLConfigParser extends DefaultHandler 
{
  private boolean isConfigFile = false;
  private boolean indexNameFound = false;
  private boolean inNamedIndexBlock = false;
  private IndexerConfig configInfo;

  ////////////////////////////////////////////////////////////////////////////

  /**
   * This method parses a config file and stores the resulting parameters in
   * a config info structure. <br><br>
   *
   * To read indexing configuration info, create an instance of this class and
   * call this method with the path/name of the config file to read. <br><br>
   *
   * @param  cfgInfo  Upon entry, a config structure with the path/name of the
   *                  config file to read in the
   *                  {@link IndexerConfig#cfgFilePath cfgFilePath} field. <br><br>
   *
   *                  Upon return, the same config structure with
   *                  parameter values from the config file stored in their
   *                  respective fields. <br><br>
   *
   * @throws  Exception  Any internal exceptions generated while parsing the
   *                     configuration file. <br><br>
   *
   * @.notes
   * The format of the XML file is explained in greater detail in the description
   * for the {@link XMLConfigParser} class. <br><br>
   *
   */
  public int configure(IndexerConfig cfgInfo)
    throws Exception //, ParserConfigurationException, SAXException, IOException
   
  {
    // Start out having not confirmed that this is a config file, or
    // that we've found the specified index name.
    //
    isConfigFile = false;
    indexNameFound = false;
    inNamedIndexBlock = false;

    try 
    {
      // Make sure we can access the file.
      if (!new File(cfgInfo.cfgFilePath).canRead()) {
        Trace.error(
          "Error: unable to read textIndexer config file \"" +
          cfgInfo.cfgFilePath + "\"");
        return -1;
      }

      // Create a reference to the passed config info class that 
      // all the methods can access.
      //
      configInfo = cfgInfo;

      // Instantiate a new SAX parser instance.
      SAXParser xmlParser = IndexUtil.createSAXParser();

      // Call the XML parser to process the config file, using this object
      // as the tag handler. Make sure to convert file to a proper URI,
      // since on Windows weird stuff happens with "C:\blah"
      //
      xmlParser.parse(new File(cfgInfo.cfgFilePath).toURI().toString(), this);
    } // try

    catch (Throwable t) 
    {
      // Log what happened.
      Trace.error(
        "*** Caught an XML Parser Exception: " + t.getClass() + "\n" +
        "    With message: " + t.getMessage());

      throw (t instanceof Exception) ? (Exception)t : new RuntimeException(t);
    }

    // If we failed to read the config file 
    if (!(isConfigFile && indexNameFound))
      return -1;

    // Make sure all the required items were specified.
    if (cfgInfo.indexInfo.indexPath == null ||
        cfgInfo.indexInfo.indexPath.equals("")) 
    {
      Trace.error("Error: Index configuration file failed to specify 'db' element");
      return -1;
    }

    if (cfgInfo.indexInfo.sourcePath == null ||
        cfgInfo.indexInfo.sourcePath.equals("")) 
    {
      Trace.error(
        "Error: Index configuration file failed to specify 'sourcePath' element");
      return -1;
    }

    if (cfgInfo.indexInfo.docSelectorPath == null ||
        cfgInfo.indexInfo.docSelectorPath.equals("")) 
    {
      Trace.error(
        "Error: Index configuration file failed to specify 'docSelectorPath' element");
      return -1;
    }

    return 0;
  } // public configure() 

  ////////////////////////////////////////////////////////////////////////////

  /** Methed called when the start tag is encountered in the config file. <br><br>
   *
   * This class is derived from the SAX {@link org.xml.sax.helpers.DefaultHandler}
   * class so that the parser can call this method each time a start tag is
   * encountered in the XML config file.<br><br>
   *
   * @param uri        The current namespace URI in use.
   *
   * @param localName  The local name (i.e., without prefix) of the current
   *                   element, or the empty string if namespace processing is
   *                   disabled.
   *
   * @param qName      The qualified name (i.e., with prefix) for the current
   *                   element, or the empty string if qualified names are
   *                   disabled.
   *
   * @param atts       The specified or defaulted arguments for the current
   *                   element. These consist of any <code>xxx = "yyy"</code>
   *                   style arguments for the element within the &lt; and &gt;.
   *                   <br><br>
   *
   * @throws  SAXException Any internal exceptions generated due to
   *                       syntax problems in the element. <br><br>
   *
   * @.notes
   *  For an explanation of the config file format, see the main description
   *  for the {@link XMLConfigParser} class. <br><br>
   */
  public void startElement(String uri, String localName, String qName,
                           Attributes atts)
    throws SAXException 
  {
    // If we encountered a config ID tag, flag that we're actually in a 
    // config file.
    //
    if (qName.equalsIgnoreCase("textIndexer-config")) {
      isConfigFile = true;
      return;
    }

    // If we're not in a config file, ignore any tags that might
    // happenstantially look like config tags.
    //
    if (!isConfigFile)
      return;

    // If we encountered an index configuration tag...
    if (qName.equalsIgnoreCase("index")) 
    {
      // Get the index name for this configuration block.
      String xmlIdxName = atts.getValue("name").trim();

      // If the name is missing, don't do any more work.
      if (xmlIdxName == null || xmlIdxName.length() == 0)
        return;

      // Get a more convenient reference to the index name that we're
      // looking for.
      //
      String idxName = configInfo.indexInfo.indexName;

      // If the block in the config file matches the specified name,
      // flag that we're in the right block, and should record any
      // config items that may follow.
      //
      if (xmlIdxName.equalsIgnoreCase(idxName)) {
        indexNameFound = true;
        inNamedIndexBlock = true;
        return;
      }

      // We're not in the right block, so flag and ignore it.
      inNamedIndexBlock = false;
      return;
    } // if( qName.equalsIgnoreCase("index") )

    // For all other configuration tags, if we are not in the right
    // config block, ignore them.
    //
    if (!inNamedIndexBlock)
      return;

    // If the current tag is an index database Path...
    if (qName.equalsIgnoreCase("db")) 
    {
      // Save it away as the database root directory. 
      configInfo.indexInfo.indexPath = Path.normalizePath(atts.getValue("path"));

      return;
    }

    // If the current tag is an index source text Path...
    if (qName.equalsIgnoreCase("src")) 
    {
      // Save it away as the root directory from which to get the
      // source XML text files.
      //
      configInfo.indexInfo.sourcePath = Path.normalizePath(atts.getValue("path"));
      return;
    }

    // If the current tag is a docSelector stylesheet path...
    if (qName.equalsIgnoreCase("docSelector")) 
    {
      // Save it away
      configInfo.indexInfo.docSelectorPath = Path.normalizePath(
        atts.getValue("path"));

      return;
    }

    // If the current tag is the chunk size info...
    if (qName.equalsIgnoreCase("chunk")) 
    {
      // Get the size (in words) of the chunk to use.
      String value = atts.getValue("size");

      // If the chunk size was not specified, or 'document' was
      // specified as the chunk size...
      //
      if (value == null || value.length() == 0) 
      {
        // Set the chunk size to be the default, and the overlap too'.
        configInfo.indexInfo.setChunkSize(IndexInfo.defaultChunkSize);
        configInfo.indexInfo.setChunkOvlp(IndexInfo.defaultChunkOvlp);
        return;
      }

      // Otherwise, set the chunk size to be the larger of the default
      // chunk size and the specified chunk size.
      //
      configInfo.indexInfo.setChunkSize(Math.max(IndexInfo.minChunkSize,
                                                 Integer.parseInt(value)));

      // Get the overlap (in words) of chunks for this index.
      value = atts.getValue("overlap");

      // If the chunk overlap was not specified, set the overlap to
      // be half the selected chunk size.
      //
      if (value == null || value.length() == 0) {
        configInfo.indexInfo.setChunkOvlp(
          configInfo.indexInfo.chunkAtt[IndexInfo.chunkSize]);
        return;
      }

      // Otherwise set the chunk overlap the value specified.
      configInfo.indexInfo.setChunkOvlp(Integer.parseInt(value));
      return;
    } // if( qName.equalsIgnoreCase( "chunk") == 0 )

    // If the current tag tells us to do stop-word removal...
    if (qName.equalsIgnoreCase("stopwords")) 
    {
      // Was the value specified in-line?
      String list = atts.getValue("list");
      String path = atts.getValue("path");

      if (list != null && list.length() > 0)
        configInfo.indexInfo.stopWords = atts.getValue("list");

      // Was a path specified?
      else if (path != null && path.length() > 0) 
      {
        path = Path.normalizeFileName(atts.getValue("path"));
        File file = new File(new File(configInfo.xtfHomePath), path);

        try {
          InputStream stream = new FileInputStream(file);
          if (path.endsWith(".gz"))
            stream = new GZIPInputStream(stream);
          Reader reader = new InputStreamReader(stream);
          char[] buf = new char[(int)file.length()];
          int length = reader.read(buf);
          configInfo.indexInfo.stopWords = new String(buf, 0, length);
        }
        catch (IOException e) {
          Trace.error("Error reading stop-words file \"" + path + "\": " + e);
          System.exit(1);
        }
      }

      // If no value was specified, use the default list of stop words.
      else
        configInfo.indexInfo.stopWords = IndexInfo.defaultStopWords;

      return;
    }

    // If the current tag tells us to map plural words...
    if (qName.equalsIgnoreCase("pluralmap")) 
    {
      // Save the path.
      configInfo.indexInfo.pluralMapPath = Path.normalizePath(
        atts.getValue("path"));
      return;
    }

    // If the current tag tells us to map accented chars...
    if (qName.equalsIgnoreCase("accentmap")) 
    {
      // Save the path.
      configInfo.indexInfo.accentMapPath = Path.normalizePath(
        atts.getValue("path"));
      return;
    }

    // If the current tag tells us to create a spellcheck dictionary...
    if (qName.equalsIgnoreCase("spellcheck")) 
    {
      // Validate the attribute.
      String val = atts.getValue("createDict");
      if (val == null)
        val = atts.getValue("createdict");
      if ("yes".equals(val) || "true".equals(val))
        configInfo.indexInfo.createSpellcheckDict = true;
      else if ("no".equals(val) || "false".equals(val))
        configInfo.indexInfo.createSpellcheckDict = false;
      else {
        Trace.error(
          "Unrecognized value for 'createDict' attribute of " +
          "config option: '" + qName + "'");
        System.exit(1);
      }
      return;
    }

    // If the current tag tells us to strip whitespace...
    if (qName.equalsIgnoreCase("whitespace")) 
    {
      // Validate the attribute.
      String val = atts.getValue("strip");
      if ("yes".equals(val) || "true".equals(val))
        configInfo.indexInfo.stripWhitespace = true;
      else if ("no".equals(val) || "false".equals(val))
        configInfo.indexInfo.stripWhitespace = false;
      else {
        Trace.error(
          "Unrecognized value for 'strip' attribute of " + "config option: '" +
          qName + "'");
        System.exit(1);
      }
      return;
    }

    Trace.error("Unknown config option: '" + qName + "'");
    System.exit(1);
  } // public startElement()

  ////////////////////////////////////////////////////////////////////////////

  /** Methed called when the end tag is encountered in the config file. <br><br>
    *
    * This class is derived from the SAX {@link org.xml.sax.helpers.DefaultHandler}
    * class so that the parser can call this method each time an end tag
    * is encountered in the XML config file.<br><br>
    *
    * @param uri        The current namespace URI in use.
    *
    * @param localName  The local name (i.e., without prefix) of the current
    *                   element, or the empty string if namespace processing is
    *                   disabled.
    *
    * @param qName      The qualified name (i.e., with prefix) for the current
    *                   element, or the empty string if qualified names are
    *                   disabled.
    *
    * @throws SAXException  If any internal exceptions generated due to
    *                       syntax problems in the element. <br><br>
    *
    * @.notes
    *  For an explanation of the config file format, see the main description
    *  for the {@link XMLConfigParser} class. <br><br>
    */
  public void endElement(String uri, String localName, String qName)
    throws SAXException// called at element end
   
  {
    // If we got the "end of index" tag, flag that the specified index
    // name is no longer found. This will effectively stop the processing
    // of config file elements.
    //
    // (Note: If another start tag for the specified index name is found,
    //        the indexNameFound tag will get set back to true, and reading
    //        of config info will resume.)
    //
    if (qName.equalsIgnoreCase("index"))
      inNamedIndexBlock = false;
  } // public endElement()
}
