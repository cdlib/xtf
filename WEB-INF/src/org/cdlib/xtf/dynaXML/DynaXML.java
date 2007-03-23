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
import java.util.LinkedList;
import java.util.Properties;
import java.util.StringTokenizer;
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
import org.cdlib.xtf.servletBase.RedirectException;
import org.cdlib.xtf.servletBase.TextConfig;
import org.cdlib.xtf.servletBase.TextServlet;
import org.cdlib.xtf.textEngine.IndexUtil;
import org.cdlib.xtf.util.StructuredStore;
import org.cdlib.xtf.util.Trace;
import org.cdlib.xtf.util.XMLWriter;
import org.cdlib.xtf.util.XTFSaxonErrorListener;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.cdlib.xtf.cache.CacheDependency;
import org.cdlib.xtf.cache.Dependency;
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
  /** Caches docInfo lookups (based on their docId) */
  private DocInfoCache docLookupCache;

  /** Handles authentication */
  Authenticator authenticator;

  /** Holds global servlet configuration info */
  private DynaXMLConfig config;

  /** Locator used to find lazy and non-lazy document files */
  private DocLocator docLocator = createDocLocator();

  /** Set to only allow lazy documents (set by TestableDynaXML only) */
  protected static boolean forceLazy = false;

  /**
   * Gets a dependency on the docLookup cache for the given document.
   * Useful when making other cache entries that depend on the docLookup.
   *
   * @param docKey    The document being worked on
   * @return          A dependency on the doc lookup for that document.
   */
  public Dependency getDocDependency(LinkedList docKey) {
    if (getConfig().dependencyCheckingEnabled)
      return new CacheDependency(docLookupCache, docKey);
    else
      return null;
  } // getDocDependency()

  /**
   * Called by the superclass to find out the name of our specific config
   * file.
   */
  protected String getConfigName() {
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

    // Create the caches
    docLookupCache = new DocInfoCache(this);

    // Create a helper for authentication.
    authenticator = new Authenticator(this);

    // And we're done.
    return config;
  } // readConfig()

  /**
   * Retrieves the current configuration information (that was read in by
   * readConfig()).
   */
  protected TextConfig getConfig() {
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

      // Get the document info, given the current URL params.
      StringTokenizer st = new StringTokenizer(config.docLookupParams,
                                               " \t\r\n,;");
      LinkedList docKey = new LinkedList();
      while (st.hasMoreTokens()) 
      {
        String name = st.nextToken();

        // Deal with screwy URL encoding of Unicode strings on
        // many browsers.
        //
        String value = req.getParameter(name);
        if (value != null) {
          docKey.add(name);
          docKey.add(convertUTF8inURL(value));
        }
      }

      DocInfo docInfo = docLookupCache.find(docKey);

      // If source overridden in the URL, make sure it's really
      // external.
      //
      if (!isEmpty(source) && source.startsWith("http://")) 
      {
        docInfo = new DocInfo(docInfo);
        docInfo.source = source;
      }
      else {
        // Check that the document actually exists.
        File docFile = new File(docInfo.source);
        if (!docFile.canRead())
          throw new InvalidDocumentException();
      }

      // Authenticate (if necessary)
      if (!authenticate(docKey, docInfo, req, res))
        return;

      // This does the bulk of the work.
      apply(docInfo, req, res);
    }
    catch (Exception e) {
      if (!(e instanceof RedirectException)) 
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
   * Performs user authentication for a request, given the authentication
   * info for the document.
   *
   * @param docKey    Cache key for this document
   * @param docInfo   Info structure containing authentication parameters
   * @param req       The request being processed
   * @param res       Where to send results if authentication fails
   *
   * @return          true iff authentication succeeds
   */
  protected boolean authenticate(LinkedList docKey, DocInfo docInfo,
                                 HttpServletRequest req, HttpServletResponse res)
    throws Exception 
  {
    // Determine the real IP address of the client.
    String ipAddr = getClientIP(req);

    // Check if this client has permission to access the document.
    // An exception thrown if not; false returned if a redirect
    // to an external page must happen first.
    //
    if (!authenticator.checkAuth(docKey, ipAddr, docInfo.authSpecs, req, res)) {
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
  * @param docInfo    Document information (stylesheet, source, etc.)
  * @param req        The original HTTP request
  * @param res        Where to send the HTML response
  *
  * @exception TransformerException  If there's an error in the stylesheet.
  * @exception IOException           If stylesheet or source can't be read.
  */
  private void apply(DocInfo docInfo, HttpServletRequest req,
                     HttpServletResponse res)
    throws Exception 
  {
    boolean dump = false;

    // First, load the stylesheet.
    Templates pss = stylesheetCache.find(docInfo.style);

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
    readBranding(docInfo.brand, req, transformer);

    // Get the source document.
    Source sourceDoc = getSourceDoc(docInfo, transformer);

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
    catch (Exception e) {
      if (config.stylesheetProfiling) {
        Trace.info("Profile for request: " + getRequestURL(req));
        Trace.tab();
        ((PersistentTree)sourceDoc).printProfile();
        Trace.untab();
        Trace.info("End of profile.");
      }
      if (dump && sourceDoc instanceof SearchTree) {
        ((SearchTree)sourceDoc).pruneUnused();
        File file = new File("C:\\tmp\\tree.dump");
        Trace.info("Dumping " + file.getAbsolutePath());
        PrintWriter outWriter = new PrintWriter(
          new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
        outWriter.println(XMLWriter.toString(sourceDoc));
        outWriter.close();
      }
      throw e;
    }

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
  } // apply()

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
    throws IOException, SAXException, ParserConfigurationException,
             InvalidDocumentException 
    {
    // If a pre-filter stylesheet was specified, load it.
    Templates preFilter = null;
    if (docInfo.preFilter != null) 
    {
      try {
        preFilter = stylesheetCache.find(docInfo.preFilter);
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
    StructuredStore lazyStore = docLocator.getLazyStore(docInfo.indexConfig,
                                                        docInfo.indexName,
                                                        docInfo.source,
                                                        preFilter,
                                                        docInfo.removeDoctypeDecl);

    // If not found...
    if (lazyStore == null) 
    {
      // Can't perform queries without a lazy tree and its corresponding
      // index.
      //
      if (docInfo.query != null)
        throw new UnsupportedQueryException();

      // If we're required to use a lazy store, throw an exception since
      // we couldn't find one.
      //
      if (forceLazy)
        throw new InvalidDocumentException();

      // Can't find a lazy store... does the original source file exist?
      if (!docInfo.source.startsWith("http://")) {
        File srcFile = new File(docInfo.source);
        if (!srcFile.isFile() || !srcFile.canRead())
          throw new InvalidDocumentException();
      }

      // Okay, read the original source file.
      XMLReader xmlReader = IndexUtil.createXMLReader();
      InputSource inSrc = docLocator.getInputSource(docInfo.source,
                                                    docInfo.removeDoctypeDecl);
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
    if (docInfo.query != null && docInfo.query.query != null) {
      String docKey = IndexUtil.calcDocKey(new File(getRealPath("")),
                                           new File(docInfo.indexConfig),
                                           docInfo.indexName,
                                           new File(docInfo.source));
      SearchTree tree = new SearchTree(config, docKey, lazyStore);
      tree.search(createQueryProcessor(), docInfo.query);
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
