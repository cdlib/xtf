package org.cdlib.xtf.dynaXML;


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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.SocketException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;

import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.trans.KeyManager;
import net.sf.saxon.tree.TreeBuilder;

import org.cdlib.xtf.servletBase.RedirectException;
import org.cdlib.xtf.servletBase.TextConfig;
import org.cdlib.xtf.servletBase.TextServlet;
import org.cdlib.xtf.textEngine.IndexUtil;
import org.cdlib.xtf.textEngine.QueryProcessor;
import org.cdlib.xtf.textEngine.QueryRequestParser;
import org.cdlib.xtf.util.AttribList;
import org.cdlib.xtf.util.EasyNode;
import org.cdlib.xtf.util.GeneralException;
import org.cdlib.xtf.util.StructuredStore;
import org.cdlib.xtf.util.Trace;
import org.cdlib.xtf.util.XMLFormatter;
import org.cdlib.xtf.util.XMLWriter;
import org.cdlib.xtf.util.XTFSaxonErrorListener;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.cdlib.xtf.lazyTree.LazyDocument;
import org.cdlib.xtf.lazyTree.LazyKeyManager;
import org.cdlib.xtf.lazyTree.LazyTreeBuilder;
import org.cdlib.xtf.lazyTree.PersistentTree;
import org.cdlib.xtf.lazyTree.SearchTree;

/**
 * Main dynaXML servlet.
 *
 * Processes a URL requesting a document, using the docReqParser stylesheet
 * to parse the request and locate the document. Checks permissions based on
 * the book being accessed and the requestor's IP address or other parameters.
 * Performs optional text querying and search hit marking, and finally
 * transforms the annotated document using a display stylesheet to form the
 * final HTML result page.
 */
public class DynaXML extends TextServlet 
{
  /** Handles authentication */
  Authenticator authenticator;

  /** Holds global servlet configuration info */
  private DynaXMLConfig config;

  /** Locator used to find lazy and non-lazy document files */
  private DocLocator docLocator = createDocLocator();

  /**
   * Called by the superclass to find out the name of our specific config
   * file.
   */
  public String getConfigName() {
    return "conf/dynaXML.conf";
  }

  /**
   * Reads in the configuration file and sets up our helpers (caching,
   * authentication, etc.)
   */
  protected TextConfig readConfig(String configPath) 
  {
    // Load the configuration file.
    config = new DynaXMLConfig(this, configPath);

    // Create a helper for authentication.
    authenticator = new Authenticator(this);

    // And we're done.
    return config;
  } // readConfig()

  /**
   * Retrieves the current configuration information (that was read in by
   * readConfig()).
   */
  public TextConfig getConfig() {
    return config;
  }

  /**
  * Retrieves the IP address of the client who is requesting a page from
  * this servlet. Handles un-reverse-proxying if necessary.
  *
  * @param    req     The HTTP request being processed
  *
  * @return           The IP address (e.g. 123.22.182.1), or empty string
  *                   if not able to figure it out.
  */
  private String getClientIP(HttpServletRequest req) 
  {
    // Start with the nominal IP address in the HTTP header.
    String ip = req.getRemoteAddr();
    Trace.debug(
      "Checking IP \"" + ip + "\" vs reverse proxy IP \"" +
      config.reverseProxyIP + "\"");

    // If it matches the configured address of the reverse proxy, we have
    // to un-map it.
    //
    if (ip.equals(config.reverseProxyIP)) 
    {
      Trace.debug("...matches reverseProxyIP");

      // Normal reverse proxies store the real IP address in a standard
      // header. Check that first.
      //
      String header = req.getHeader(config.reverseProxyDefaultMarker);
      if (!isEmpty(header)) {
        Trace.debug(
          "...using marker " + config.reverseProxyDefaultMarker + " -> " +
          header);
        ip = header;
      }

      // However, some proxies have a special header. If it's present,
      // override with that.
      //
      if (!isEmpty(config.reverseProxyMarker)) 
      {
        header = req.getHeader(config.reverseProxyMarker);
        if (!isEmpty(header)) {
          Trace.debug(
            "...using marker " + config.reverseProxyMarker + " -> " + header);
          ip = header;
        }
      }
    }

    // Some broken proxies prepend "unknown" to the real IP address.
    // To work around these, skip all characters until we hit a digit.
    //
    while (ip.length() > 0 && !Character.isDigit(ip.charAt(0)))
      ip = ip.substring(1);

    // All done!
    return ip;
  } // getClientIP()

  /**
   * Handles the HTTP 'get' method. Based on the HTTP request, produces an
   * appropriate response.
   *
   * @param     req            The HTTP request
   * @param     res            The HTTP response
   * @exception IOException    If unable to write the output stream.
   */
  public void doGet(HttpServletRequest req, HttpServletResponse res)
    throws IOException 
  {
    try 
    {
      // Get the parameters out of the request structure.
      String source = req.getParameter("source");

      // If profiling is enabled, we have to notify the stylesheet
      // cache.
      //
      stylesheetCache.enableProfiling(config.stylesheetProfiling);

      // Set the default output content type
      res.setContentType("text/html");

      // Output extended debugging info if requested.
      Trace.debug("Processing request: " + getRequestURL(req));

      // Run the document request parser
      DocRequest docReq = runDocReqParser(req, makeAttribList(req));

      // If source overridden in the URL, make sure it's really
      // external.
      //
      if (!isEmpty(source) && source.startsWith("http://")) 
      {
        docReq = new DocRequest(docReq);
        docReq.source = source;
      }
      else {
        // Check that the document actually exists.
        File docFile = new File(docReq.source);
        if (!docFile.canRead())
          throw new InvalidDocumentException();
      }

      // Authenticate (if necessary)
      if (!authenticate(docReq, req, res))
        return;

      // This does the bulk of the work.
      apply(docReq, req, res);
    }
    catch (Exception e) {
      if (!(e instanceof RedirectException) && !(e instanceof SocketException)) 
      {
        try {
          genErrorPage(req, res, e);
        }
        catch (RedirectException re) {
        }
      }
      return;
    }
  } // doGet()

  /**
   * Creates a document request by running the docReqParser stylesheet and 
   * the given attributes.
   *
   * @param req          The original HTTP request
   * @param attribs      Attributes to pass to the stylesheet.
   * @return             A parsed document request, or null if before that step
   */
  protected DocRequest runDocReqParser(HttpServletRequest req,
                                       AttribList attribs)
    throws Exception 
  {
    DocRequest info = new DocRequest();

    // Running the stylesheet may produce additional dependencies if the
    // sheet reads in extra files dependent on the document ID. To avoid
    // having to throw away the stylesheet entry, we record the current
    // dependencies so we can restore them later.
    // 
    Iterator di = stylesheetCache.getDependencies(
      config.docLookupSheet);
    LinkedList oldStylesheetDeps = new LinkedList();
    while (di.hasNext())
      oldStylesheetDeps.add(di.next());

    // Locate the document request parser stylesheet.
    Templates sheet = stylesheetCache.find(config.docLookupSheet);

    // Get a transformer to handle this stylesheet.
    Transformer trans = sheet.newTransformer();

    // Stuff all the common config properties into the transformer in
    // case the query generator needs access to them.
    //
    stuffAttribs(trans, config.attribs);

    // Also stuff the URL parameters, in case it wants them that way
    // instead of tokenized.
    //
    stuffAttribs(trans, attribs);

    // Add the special computed attributes.
    stuffSpecialAttribs(req, trans);

    // Make sure errors get directed to the right place.
    if (!(trans.getErrorListener() instanceof XTFSaxonErrorListener))
      trans.setErrorListener(new XTFSaxonErrorListener());

    // Make a <parameters> block.
    XMLFormatter fmt = new XMLFormatter();
    fmt.blankLineAfterTag(false);
    buildParamBlock(attribs, fmt, config.tokenizerMap, null);

    if (Trace.getOutputLevel() >= Trace.debug) {
      String tmp = fmt.toString();
      if (tmp.endsWith("\n"))
        tmp = tmp.substring(0, tmp.length() - 1);
      Trace.debug("*** docReqParser input ***\n" + tmp);
    }

    // Now request the stylesheet to give us the info for this document.
    TreeBuilder result;
    result = new TreeBuilder();
    trans.transform(fmt.toSource(), result);

    if (Trace.getOutputLevel() >= Trace.debug) {
      Trace.debug("*** docReqParser output ***");
      Trace.tab();
      Trace.debug(XMLWriter.toString(result.getCurrentRoot()));
      Trace.untab();
    }

    // Extract the data we need.
    EasyNode root = new EasyNode(result.getCurrentRoot());
    for (int i = 0; i < root.nChildren(); i++) 
    {
      EasyNode el = root.child(i);
      String tagName = el.name();

      if (tagName.equals("style"))
        info.style = getRealPath(el.attrValue("path"));
      else if (tagName.equals("source"))
        info.source = getRealPath(el.attrValue("path"));
      else if (tagName.equals("index")) {
        info.indexConfig = getRealPath(el.attrValue("configPath"));
        info.indexName = el.attrValue("name");
      }
      else if (tagName.equals("brand"))
        info.brand = getRealPath(el.attrValue("path"));
      else if (tagName.equals("auth"))
        info.authSpecs.add(authenticator.processAuthTag(el));
      else if (tagName.equals("query")) {
        info.query = new QueryRequestParser().parseRequest(el.getWrappedNode(),
                                                           new File(getRealPath("")));
      }
      else if (tagName.equalsIgnoreCase("preFilter"))
        info.preFilter = getRealPath(el.attrValue("path"));
      else if (tagName.equalsIgnoreCase("removeDoctypeDecl")) {
        String val = el.attrValue("flag");
        if (val.matches("^yes$|^true$"))
          info.removeDoctypeDecl = true;
        else if (val.matches("^no$|^false$"))
          info.removeDoctypeDecl = false;
        else
          throw new DynaXMLException(
            "Expected 'true', 'false', " +
            "'yes', or 'no' for flag attribute of " + tagName +
            " tag specified by docReqParser, but found '" + val + "'");
      }
      else
        throw new DynaXMLException(
          "Unknown tag '" + tagName + "' specified by docReqParser");
    } // for node

    // If no source, assume that means an invalid document ID.
    if (isEmpty(info.source))
      throw new InvalidDocumentException();

    // Make sure a stylesheet was specified.
    requireOrElse(info.style, "docReqParser didn't specify 'style'");

    // Index config and index name must be either both specified or both
    // absent.
    //
    if (isEmpty(info.indexConfig) && !isEmpty(info.indexName))
      throw new GeneralException(
        "docReqParser specified 'indexName' without 'indexConfig'");
    if (!isEmpty(info.indexConfig) && isEmpty(info.indexName))
      throw new GeneralException(
        "docReqParser specified 'indexConfig' without 'indexName'");

    // And we're done.
    return info;
  } // runDocReqParser()

  /**
   * Performs user authentication for a request, given the authentication
   * info for the document.
   *
   * @param docReq    Info structure containing authentication parameters
   * @param req       The request being processed
   * @param res       Where to send results if authentication fails
   *
   * @return          true iff authentication succeeds
   */
  protected boolean authenticate(DocRequest docReq,
                                 HttpServletRequest req, HttpServletResponse res)
    throws Exception 
  {
    // Determine the real IP address of the client.
    String ipAddr = getClientIP(req);

    // Check if this client has permission to access the document.
    // An exception thrown if not; false returned if a redirect
    // to an external page must happen first.
    //
    if (!authenticator.checkAuth(ipAddr, docReq.authSpecs, req, res)) {
      return false;
    }

    return true;
  } // authenticate()

  /**
  * Informational method required by Servlet interface. Doesn't seem to
  * matter what it says.
  *
  * @return   A string describing this servlet.
  */
  public String getServletInfo() {
    return "dynaXML dynamic publishing servlet";
  } // getServletInfo()

  /**
  * Loads the source document, optionally performs a text search on it, and
  * then runs the document formatter to produce the final HTML result page.
  *
  * @param docReq     Document information (stylesheet, source, etc.)
  * @param req        The original HTTP request
  * @param res        Where to send the HTML response
  *
  * @exception TransformerException  If there's an error in the stylesheet.
  * @exception IOException           If stylesheet or source can't be read.
  */
  private void apply(DocRequest docReq, HttpServletRequest req,
                     HttpServletResponse res)
    throws Exception 
  {
    boolean dump = false;

    // First, load the stylesheet.
    Templates pss = stylesheetCache.find(docReq.style);

    // Figure out the output mime type
    res.setContentType(calcMimeType(pss));

    // Make a transformer and stuff it full of parameters. But if it's the
    // same stylesheet as we used last time in this thread, we can re-use
    // it for speed (keys won't have to be rebuilt.)
    //
    Transformer transformer = pss.newTransformer();
    stuffAttribs(transformer, req);
    stuffAttribs(transformer, config.attribs);

    // Also read the brand profile. It's just a simple stylesheet and we
    // stuff the output tags into parameters. They can be whatever the 
    // stylesheet writer desires.
    //
    readBranding(docReq.brand, req, transformer);

    // Get the source document.
    Source sourceDoc = getSourceDoc(docReq, transformer);

    // If we are in raw mode, use a null transform instead of the
    // stylesheet.
    //
    String raw = req.getParameter("raw");
    if ("yes".equals(raw) || "true".equals(raw) || "1".equals(raw)) 
    {
      res.setContentType("text/xml");

      transformer = IndexUtil.createTransformer();
      Properties props = transformer.getOutputProperties();
      props.put("indent", "yes");
      props.put("method", "xml");
      transformer.setOutputProperties(props);
    }

    // Modify as necessary
    if (dump && sourceDoc instanceof PersistentTree)
      ((PersistentTree)sourceDoc).setAllPermanent(true);

    // Make sure errors get directed to the right place.
    if (!(transformer.getErrorListener() instanceof XTFSaxonErrorListener))
      transformer.setErrorListener(new XTFSaxonErrorListener());

    // Our tree is pre-stripped, so it would be inefficient to strip it
    // again.
    //
    ((Controller)transformer).getExecutable().setStripsWhitespace(false);

    // Now do the bulk of the work
    try 
    {
      transformer.transform(sourceDoc,
                            createFilteredReceiver(transformer, req, res));
    }
    finally 
    {
      // Clean up.
      if (config.stylesheetProfiling) {
        Trace.info("Profile for request: " + getRequestURL(req));
        Trace.tab();
        ((PersistentTree)sourceDoc).printProfile();
        Trace.untab();
        Trace.info("End of profile.");
      }

      // Debugging: dump search tree.
      if (dump && sourceDoc instanceof SearchTree) {
        ((SearchTree)sourceDoc).pruneUnused();
        File file = new File("C:\\tmp\\tree.dump");
        Trace.info("Dumping " + file.getAbsolutePath());
        PrintWriter outWriter = new PrintWriter(
          new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
        outWriter.println(XMLWriter.toString(sourceDoc));
        outWriter.close();
      }

      // It's a good idea to close disk-based trees when done using them.
      if (sourceDoc instanceof PersistentTree)
        ((PersistentTree)sourceDoc).close();
    }
  } // apply()
  
  /**
   * Get a query processor we can utilize. Can be overridden for specialized
   * processing.
   */
  protected QueryProcessor getQueryProcessor() {
    return TextServlet.createQueryProcessor();
  }

  /**
   * Does the work of locating and loading the source document. Handles
   * fetching a file from a URL, lazy file, or a plain XML file on disk.
   * Also fires up a text query if requested.
   *
   * @param docReq        Tells which document to load, the query to
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
  protected Source getSourceDoc(DocRequest docReq, Transformer transformer)
    throws IOException, SAXException, ParserConfigurationException,
             InvalidDocumentException 
    {
    // If a pre-filter stylesheet was specified, load it.
    Templates preFilter = null;
    if (docReq.preFilter != null) 
    {
      try {
        preFilter = stylesheetCache.find(docReq.preFilter);
      }
      catch (IOException e) {
        throw e;
      }
      catch (SAXException e) {
        throw e;
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    // See if we can find a lazy version of the document (for speed
    // and searching)
    //
    StructuredStore lazyStore = docLocator.getLazyStore(docReq.indexConfig,
                                                        docReq.indexName,
                                                        docReq.source,
                                                        preFilter,
                                                        docReq.removeDoctypeDecl);

    // If not found...
    if (lazyStore == null) 
    {
      // Can't perform queries without a lazy tree and its corresponding
      // index.
      //
      if (docReq.query != null)
        throw new UnsupportedQueryException();

      // Can't find a lazy store... does the original source file exist?
      if (!docReq.source.startsWith("http://")) {
        File srcFile = new File(docReq.source);
        if (!srcFile.isFile() || !srcFile.canRead())
          throw new InvalidDocumentException();
      }

      // Okay, read the original source file.
      XMLReader xmlReader = IndexUtil.createXMLReader();
      InputSource inSrc = docLocator.getInputSource(docReq.source,
                                                    docReq.removeDoctypeDecl);
      return new SAXSource(xmlReader, inSrc);
    }

    // Okay, let's use the lazy version of the document...
    Source sourceDoc = null;
    
    // Get the Saxon configuration to use
    Controller controller = (Controller)transformer;
    Configuration config = controller.getConfiguration();

    // If a query was specified, make a SearchTree; otherwise, make
    // a normal lazy tree.
    //
    if (docReq.query != null && docReq.query.query != null) {
      String docKey = IndexUtil.calcDocKey(new File(getRealPath("")),
                                           new File(docReq.indexConfig),
                                           docReq.indexName,
                                           new File(docReq.source));
      SearchTree tree = new SearchTree(config, docKey, lazyStore);
      tree.search(getQueryProcessor(), docReq.query);
      sourceDoc = tree;
    }
    else {
      LazyTreeBuilder builder = new LazyTreeBuilder(config);
      builder.setNamePool(NamePool.getDefaultNamePool());
      sourceDoc = builder.load(lazyStore);
    }

    // We want to print out any indexes being created, because
    // they should have all been done by the textIndexer.
    //
    ((LazyDocument)sourceDoc).setDebug(true);

    // We need a special key manager on the lazy tree, so that we can
    // use lazily stored keys on disk.
    //
    Executable e = controller.getExecutable();
    KeyManager k = e.getKeyManager();
    if (!(k instanceof LazyKeyManager))
      e.setKeyManager(new LazyKeyManager(controller.getConfiguration(), k));

    // All done.
    return sourceDoc;
  } // getSourceDoc()

  /**
   * Create a DocLocator. Checks the system property
   * "org.cdlib.xtf.DocLocatorClass" to see if there is a user-
   * supplied implementation. If not, a {@link DefaultDocLocator} is
   * created.
   */
  public DocLocator createDocLocator() 
  {
    // Check the system property.
    final String propName = "org.cdlib.xtf.DocLocatorClass";
    String className = System.getProperty(propName);
    Class theClass = DefaultDocLocator.class;
    try 
    {
      // Try to create an object of the correct class.
      if (className != null)
        theClass = Class.forName(className);
      DocLocator loc = (DocLocator)theClass.newInstance();
      loc.setServlet(this);
      return loc;
    }
    catch (ClassCastException e) {
      Trace.error(
        "Error: Class '" + className + "' specified by " + "the '" + propName +
        "' property does not support the " + DocLocator.class.getName() +
        " interface");
      throw new RuntimeException(e);
    }
    catch (Exception e) {
      Trace.error(
        "Error creating instance of class '" + className +
        "' specified by the '" + propName + "' property");
      throw new RuntimeException(e);
    }
  } // createDocLocator()

  /**
   * Tells the servlet whether to perform stylesheet profiling. The profile
   * is (currently) sent to Trace.info().
   *
   * @param flag    If true, subsequent XSLT transformations will be
   *                profiled.
   */
  public void setProfiling(boolean flag) {
    config.stylesheetProfiling = flag;
  }
} // class TextServlet
