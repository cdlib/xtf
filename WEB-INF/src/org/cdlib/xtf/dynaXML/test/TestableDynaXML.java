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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.LinkedList;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.om.AllElementStripper;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.trans.KeyManager;
import net.sf.saxon.tree.TreeBuilder;
import org.cdlib.xtf.dynaXML.DocInfo;
import org.cdlib.xtf.dynaXML.DynaXML;
import org.cdlib.xtf.dynaXML.InvalidDocumentException;
import org.cdlib.xtf.lazyTree.LazyKeyManager;
import org.cdlib.xtf.util.DocTypeDeclRemover;
import org.cdlib.xtf.util.Trace;
import org.cdlib.xtf.util.XMLWriter;
import org.cdlib.xtf.util.XTFSaxonErrorListener;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Extends the DynaXML servlet to allow switching between two different
 * representations of the same document, to allow verification that the output
 * is the same for both of them. One version will be the SearchTree, the other
 * is annotated using TreeAnnotater. Any exceptions will be thrown upward
 * rather than generating an error page.
 *
 * @author Martin Haye
 */
public class TestableDynaXML extends DynaXML 
{
  private boolean useAnnotated = false;
  private boolean dump = false;
  private String prevAnnotatedPath;
  private DocumentInfo prevAnnotatedTree;
  private String searchTerm;

  /**
   * Tells whether to use the annotated version or the SearchTree version.
   *
   * @param flag    frue for annotated, false for SearchTree
   */
  public void useAnnotated(boolean flag) {
    this.useAnnotated = flag;
  }

  /** Sets the term to use in text searches */
  public void setSearchTerm(String term) {
    this.searchTerm = term;
  }

  /**
   * Get a version of the source tree with hits marked in context by a stupid
   * but reliable annotater.
   *
   * @param sourcePath      Path to the document
   * @return                Root of the annotated document
   */
  public DocumentInfo getAnnotatedTree(String sourcePath)
    throws IOException, SAXException, ParserConfigurationException,
             TransformerException 
    {
    // If we've already made this tree, save time by not doing it again.
    sourcePath = sourcePath.replaceAll("\\\\", "/");
    if (sourcePath.equals(prevAnnotatedPath)) {
      return prevAnnotatedTree;
    }

    // Make sure the search term has been set.
    assert searchTerm != null;

    // First, read in the source in DOM format.
    DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = fac.newDocumentBuilder();

    // Convert our XML text file into a SAXON input source.
    //
    // Remove DOCTYPE declarations, since the XML reader will barf if it
    // can't resolve the entity reference, and we really don't care.
    //
    InputStream inStream = new FileInputStream(new File(sourcePath));
    inStream = new DocTypeDeclRemover(inStream);
    Document doc = builder.parse(inStream);

    // Annotate the tree with search results.
    TreeAnnotater annotater = new TreeAnnotater();
    annotater.processDocument(doc, searchTerm);

    // For speed of transformation, we need to make it into a NodeImpl
    // tree.
    //
    Configuration config = new Configuration();
    Source src = new DOMSource(doc);
    config.setErrorListener(new XTFSaxonErrorListener());
    prevAnnotatedTree = (DocumentInfo)TreeBuilder.build(src,
                                                        new AllElementStripper(),
                                                        config);
    prevAnnotatedPath = sourcePath;
    return prevAnnotatedTree;
  } // getAnnotatedTree()

  /**
   * Writes out a source tree, removing things like score and rank that can
   * reasonably vary between the annotated and SearchTree versions of a
   * document.
   *
   * @param fileName    File to write to
   * @param tree        The document to dump
   */
  public static void dumpTree(String fileName, Source tree) 
  {
    try 
    {
      File file = new File(fileName);
      Trace.info("Dumping " + file.getAbsolutePath());
      PrintWriter outWriter = new PrintWriter(
        new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));

      String str = XMLWriter.toString(tree);
      str = str.replaceAll(" score=\"\\w+\"", "");
      str = str.replaceAll(" rank=\"\\w+\"", "");
      str = str.replaceAll(" more=\"\\w+\"", "");
      str = str.replaceAll(" xmlns:xtf=\"http://cdlib.org/xtf\"", "");

      outWriter.println(str);
      outWriter.close();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  } // dumpTree()

  /** Establishes whether to dump the tree on each iteration */
  public void setDump(boolean flag) {
    dump = flag;
  }

  /**
   * Does the work of locating and loading the source document. Handles
   * fetching a file from a URL, lazy file, or a plain XML file on disk.
   * Also fires up a text query if requested.
   *
   * @param docInfo       Tells which document to load, the query to
   *                      apply, tec.
   * @param transformer   The XSLT transformer that will be used on the
   *                      document.
   *
   * @return              An XML Source object representing the loaded
   *                      document.
   *
   * @throws IOException  If a problem is encountered loading a file or URL
   * @throws SAXException If the document cannot be parsed as valid XML
   * @throws ParserConfigurationException Miscellaneous configuration
   *                                      problems
   */
  protected Source getSourceDoc(DocInfo docInfo, Transformer transformer)
    throws IOException, SAXException, ParserConfigurationException 
  {
    // If no lazy file is available, skip the document.
    forceLazy = true;

    // If not annotating, use the normal SearchTree...
    if (!useAnnotated) {
      Source realSrc = super.getSourceDoc(docInfo, transformer);
      if (dump)
        dumpTree("C:\\tmp\\tree.dump", realSrc);
      return realSrc;
    }

    // Otherwise, make sure we're using a normal key manager (SearchTree
    // overrides it.)
    //
    Controller c = (Controller)transformer;
    Executable e = c.getExecutable();
    KeyManager k = e.getKeyManager();
    if (k instanceof LazyKeyManager)
      e.setKeyManager(((LazyKeyManager)k).getUnderlyingManager());

    // A Saxon bug somewhere in last() happens when we allow stripping. Since
    // our tree is pre-stripped anyway, we can safely turn off stripping.
    //
    e.setStripsWhitespace(false);

    try {
      Source src = getAnnotatedTree(docInfo.source);
      if (dump)
        dumpTree("C:\\tmp\\annotated.dump", src);
      return src;
    }
    catch (TransformerException ex) {
      throw new RuntimeException(ex);
    }
  } // getSourceDoc()

  /**
   * Performs user authentication for a request, given the authentication
   * info for the document. In the case of testing, we never fail
   * authentication.
   */
  protected boolean authenticate(LinkedList docKey, DocInfo docInfo,
                                 HttpServletRequest req, HttpServletResponse res)
    throws Exception 
  {
    return true;
  }

  /**
  * Generate an error page based on the given exception. Utilizes the system
  * error stylesheet to produce a nicely formatted HTML page.
  *
  * @param req  The HTTP request we're responding to
  * @param res  The HTTP result to write to
  * @param exc  The exception producing the error. If it's a
  *             DynaXMLException, the attributes will be passed to
  *             the error stylesheet.
  */
  protected void genErrorPage(HttpServletRequest req, HttpServletResponse res,
                              Exception exc) 
  {
    if (exc instanceof InvalidDocumentException) {
      Trace.error("Invalid document. ");
      return;
    }
    if (exc.getClass().getName().indexOf("QueryFormatError") >= 0) {
      Trace.error("Query format error.");
      return;
    }
    Trace.error("Exception occurred in dynaXML: " + exc);
    Trace.error("");
    exc.printStackTrace(System.out);
    throw new RuntimeException(exc);
  } // genErrorPage()
} // class TestableDynaXML
