package org.cdlib.xtf.dynaXML;


/*
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.parsers.SAXParser;
import javax.xml.transform.Templates;
import javax.xml.transform.sax.SAXResult;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.ReceivingContentHandler;
import net.sf.saxon.om.NamePool;
import org.cdlib.xtf.lazyTree.LazyTreeBuilder;
import org.cdlib.xtf.servletBase.TextServlet;
import org.cdlib.xtf.textEngine.IndexUtil;
import org.cdlib.xtf.util.DocTypeDeclRemover;
import org.cdlib.xtf.util.Path;
import org.cdlib.xtf.util.StructuredFile;
import org.cdlib.xtf.util.StructuredStore;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/*
 * This file created on Mar 11, 2005 by Martin Haye
 */

/**
 * Provides local filesystem-based access to lazy and non-lazy versions of
 * a source XML document.
 *
 * @author Martin Haye
 */
public class DefaultDocLocator implements DocLocator 
{
  /** Servlet we are part of */
  private TextServlet servlet;

  /** Attach to a servlet */
  public void setServlet(TextServlet servlet) {
    this.servlet = servlet;
  }

  /**
   * Search for a StructuredStore containing the "lazy" or persistent
   * representation of a given document. Index parameters are specified,
   * since often the lazy file is stored along with the index. This method
   * is called first, and if it returns null, then
   * {@link #getInputSource(String, boolean)} will be called as a fall-back.
   *
   * @param indexConfigPath Path to the index configuration file
   * @param indexName       Name of the index being searched
   * @param sourcePath      Path to the source document
   * @param preFilter       Prefilter stylesheet to run (or null for none)
   * @param removeDoctypeDecl Set to true to remove DOCTYPE declaration from
   *                          the XML document.
   *
   * @return                Store containing the tree, or null if none
   *                        could be found.
   */
  public StructuredStore getLazyStore(String indexConfigPath, String indexName,
                                      String sourcePath, Templates preFilter,
                                      boolean removeDoctypeDecl)
    throws IOException 
  {
    // If no 'index' specified in the docInfo, then there's no way we can
    // find the lazy file.
    //
    if (indexConfigPath == null || indexName == null)
      return null;

    // If the source isn't a local file, we also can't use a lazy file.
    if (sourcePath.startsWith("http:"))
      return null;
    if (sourcePath.startsWith("https:"))
      return null;

    // If it's a directory, something went wrong. No lazy file for sure.
    File sourceFile = new File(sourcePath);
    if (!sourceFile.isFile())
      return null;

    // Figure out where the lazy file is (or should be.)
    File lazyFile = IndexUtil.calcLazyPath(new File(servlet.getRealPath("")),
                                           new File(indexConfigPath),
                                           indexName,
                                           new File(sourcePath),
                                           false);

    // If we can't read it, try to build it instead.
    if (!lazyFile.canRead()) 
    {
      boolean stripWhitespace = false;
      try {
        stripWhitespace = IndexUtil.getIndexInfo(new File(indexConfigPath),
                                                 indexName).stripWhitespace;
      }
      catch (Exception e) {
      }

      buildLazyStore(lazyFile,
                     sourcePath,
                     preFilter,
                     removeDoctypeDecl,
                     stripWhitespace);
    }

    // Cool. Open the lazy file.
    return StructuredFile.open(lazyFile);
  } // getLazyStore()

  /**
   * Retrieve the data stream for an XML source document.
   *
   * @param sourcePath  Path to the source document
   * @param removeDoctypeDecl Set to true to remove DOCTYPE declaration from
   *                          the XML document.
   *
   * @return            Data stream for the document.
   */
  public InputSource getInputSource(String sourcePath, boolean removeDoctypeDecl)
    throws IOException 
  {
    // If it's non-local, load the URL.
    if (sourcePath.startsWith("http:") || sourcePath.startsWith("https:")) {
      return new InputSource(sourcePath);
    }

    // Okay, assume it's a local file.
    InputStream inStream = new FileInputStream(sourcePath);

    // Remove DOCTYPE declarations, since the XML reader will barf 
    // if it can't resolve the entity reference, and we really 
    // don't care one way or the other.
    //
    if (removeDoctypeDecl)
      inStream = new DocTypeDeclRemover(inStream);

    // Make the input source, and give it a real system ID.
    InputSource inSrc = new InputSource(inStream);
    inSrc.setSystemId(new File(sourcePath).toURL().toString());

    // All done!
    return inSrc;
  } // getInputSource()

  /**
   * Create a lazy document by loading the original, building the lazy
   * tree, and writing it out.
   *
   * @param lazyFile      Lazy file to create
   * @param sourcePath    Path to the source document
   * @param preFilter     A prefilter stylesheet (or null for no pre-filtering.)
   * @param removeDoctypeDecl true to remove DOCTYPE declarations from the
   *                          XML document
   * @param stripWhitespace If set, whitespace will be removed between elements
   *                        in the lazy file.
   */
  private void buildLazyStore(File lazyFile, String sourcePath,
                              Templates preFilter, boolean removeDoctypeDecl,
                              boolean stripWhitespace)
    throws IOException 
  {
    // The directory the lazy file is to be stored in might not exist yet.
    // If not, we need to create it now before making the lazy file.
    //
    Path.createPath(lazyFile.getParent());

    // While we parse the source document, we're going to also build up 
    // a tree that will be written to the lazy file.
    //
    LazyTreeBuilder lazyBuilder = new LazyTreeBuilder();
    StructuredStore lazyStore = StructuredFile.create(lazyFile);
    Receiver lazyReceiver = lazyBuilder.begin(lazyStore);

    lazyBuilder.setNamePool(NamePool.getDefaultNamePool());

    ReceivingContentHandler lazyHandler = new ReceivingContentHandler();
    lazyHandler.setReceiver(lazyReceiver);
    lazyHandler.setPipelineConfiguration(lazyReceiver.getPipelineConfiguration());

    // Instantiate a new XML parser, being sure to get the right one.
    SAXParser xmlParser = IndexUtil.createSAXParser();

    // Open the source file for reading
    InputStream inStream = new FileInputStream(sourcePath);

    // Apply the standard set of document filters.
    InputSource inSrc = new InputSource(IndexUtil.filterXMLDocument(
                                                                    inStream,
                                                                    xmlParser,
                                                                    removeDoctypeDecl));

    // Put a proper system ID onto the InputSource.
    inSrc.setSystemId(new File(sourcePath).toURL().toString());

    // Make a DefaultHandler that will pass events to the lazy receiver.
    LazyPassthru passthru = new LazyPassthru(lazyHandler, stripWhitespace);

    // Apply a prefilter if one was specified.
    if (preFilter == null) 
    {
      try {
        xmlParser.parse(inSrc, passthru);
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    else {
      // Apply the pre-filter.
      try {
        Templates[] array = new Templates[1];
        array[0] = preFilter;
        IndexUtil.applyPreFilters(array,
                                  xmlParser.getXMLReader(),
                                  inSrc,
                                  new SAXResult(passthru));
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    // Finish off the lazy file.
    lazyBuilder.finish(lazyReceiver, true);
  } // buildLazyStore()

  /**
   * Passes SAX events to a ContentHandler. Also performs character
   * buffering that mimics what the textIndexer normally does.
   */
  private static class LazyPassthru extends DefaultHandler 
  {
    private StringBuffer charBuf = new StringBuffer();
    private ContentHandler lazyHandler;
    private boolean stripWhitespace;

    public LazyPassthru(ContentHandler lazyHandler, boolean stripWhitespace) {
      this.lazyHandler = lazyHandler;
      this.stripWhitespace = stripWhitespace;
    }

    public void startDocument()
      throws SAXException 
    {
      lazyHandler.startDocument();
    }

    public void startElement(String uri, String localName, String qName,
                             Attributes atts)
      throws SAXException 
    {
      flushCharacters();
      lazyHandler.startElement(uri, localName, qName, atts);
    }

    public void endElement(String uri, String localName, String qName)
      throws SAXException 
    {
      flushCharacters();
      lazyHandler.endElement(uri, localName, qName);
    }

    public void processingInstruction(String target, String data)
      throws SAXException 
    {
      lazyHandler.processingInstruction(target, data);
    }

    public void endDocument()
      throws SAXException 
    {
      lazyHandler.endDocument();
    }

    public void characters(char[] ch, int start, int length) {
      charBuf.append(ch, start, length);
    }

    private void flushCharacters()
      throws SAXException 
    {
      // If the entire buffer is whitespace (or empty), we can safely 
      // strip it.
      //
      int i = 0;
      if (stripWhitespace) {
        for (i = 0; i < charBuf.length(); i++)
          if (!Character.isWhitespace(charBuf.charAt(i)))
            break;
      }
      if (i < charBuf.length())
        lazyHandler.characters(charBuf.toString().toCharArray(),
                               0,
                               charBuf.length());
      charBuf.setLength(0);
    }
  }
  ;
} // class DefaultDocLocator
