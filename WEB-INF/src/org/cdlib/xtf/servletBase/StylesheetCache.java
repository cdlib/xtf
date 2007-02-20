package org.cdlib.xtf.servletBase;


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
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import net.sf.saxon.FeatureKeys;
import org.xml.sax.InputSource;
import org.cdlib.xtf.cache.FileDependency;
import org.cdlib.xtf.cache.GeneratingCache;
import org.cdlib.xtf.lazyTree.ProfilingListener;
import org.cdlib.xtf.util.*;

/**
 * This class is used to cache stylesheets so they don't have to be
 * reloaded each time they're used.
 */
public class StylesheetCache extends GeneratingCache 
{
  private boolean dependencyChecking = false;
  private GeneratingCache dependencyReceiver = null;
  private boolean enableProfiling = false;

  /**
   * Constructor.
   *
   * @param maxEntries    Max # of entries before old ones are flushed
   * @param maxTime       Max age (in seconds) before an entry is flushed.
   * @param dependencyChecking Whether to keep track of dependencies and
   *                           invalidate cache entries when dependents
   *                           are updated.
   */
  public StylesheetCache(int maxEntries, int maxTime, boolean dependencyChecking) {
    super(maxEntries, maxTime);
    this.dependencyChecking = dependencyChecking;
  }

  /**
   * Locate the stylesheet for the given filesystem path. If not cached,
   * then load it.
   *
   * @param  path         Filesystem path of the stylesheet to load
   * @return              The parsed stylesheet
   * @throws Exception    If the stylesheet could not be loaded.
   */
  public Templates find(String path)
    throws Exception 
  {
    return (Templates)super.find(path);
  }

  /**
   * Enable or disable profiling (only affects stylesheets that are
   * not already cached)
   */
  public void enableProfiling(boolean flag) {
    enableProfiling = flag;
  }

  /**
   * Set a cache to receive file dependencies when an entry is used or
   * generated.
   *
   * @param  cache        Cache to receive dependencies
   */
  public void setDependencyReceiver(GeneratingCache cache) {
    dependencyReceiver = cache;
  }

  /**
   * Load and parse a stylesheet from the filesystem.
   *
   * @param  key          (String)Filesystem path of the stylesheet to load
   * @return              The parsed stylesheet
   * @throws Exception    If the stylesheet could not be loaded.
   */
  protected Object generate(Object key)
    throws Exception 
  {
    assert dependencyReceiver == null : "stylesheet cache should only have dependencyReceiver " +
      "during external calls, not during find().";
    if (dependencyChecking)
      dependencyReceiver = this;

    try 
    {
      String path = (String)key;
      File file = new File(path);
      if (dependencyChecking)
        addDependency(new FileDependency(file));
      if (!path.startsWith("http:") && !file.canRead())
        throw new GeneralException("Cannot read stylesheet: " + path);

      TransformerFactory factory = new net.sf.saxon.TransformerFactoryImpl();
      if (!(factory.getErrorListener() instanceof XTFSaxonErrorListener))
        factory.setErrorListener(new XTFSaxonErrorListener());

      // Avoid loading external DTDs if possible. This not only speeds
      // things up, but allows our service to work without depending on
      // external servers being up and running at every moment.
      //
      factory.setAttribute(FeatureKeys.SOURCE_PARSER_CLASS,
                           DTDSuppressingXMLReader.class.getName());

      if (enableProfiling) {
        ProfilingListener profilingListener = new ProfilingListener();
        factory.setAttribute(FeatureKeys.TRACE_LISTENER, profilingListener);
        factory.setAttribute(FeatureKeys.LINE_NUMBERING, Boolean.TRUE);
      }

      if (dependencyChecking) {
        factory.setURIResolver(new DepResolver(this, factory.getURIResolver()));
      }

      String url;
      if (path.startsWith("http:"))
        url = path;
      else
        url = file.toURL().toString();
      Templates x = factory.newTemplates(new SAXSource(new InputSource(url)));
      if (x == null)
        throw new TransformerException("Cannot read stylesheet: " + path);

      return x;
    }
    finally {
      dependencyReceiver = null;
    }
  } // generate()

  /** Prints out useful debugging info */
  protected void logAction(String action, Object key, Object value) {
    Trace.debug("StylesheetCache: " + action + ". Path=" + (String)key);
  }

  /**
   * While loading a stylesheet, we record all the sub-stylesheets
   * referenced by it, so that we can form a list of all the dependencies.
   * That way, if any of them are changed, the stylesheet will be auto-
   * matically reloaded.
   *
   * We do it by implementing a pass-through URIResolver that adds a
   * dependency and then does the normal URIResolver work.
   */
  private class DepResolver implements URIResolver 
  {
    /**
     * Constructor.
     *
     * @param cache         The cache to add dependencies to
     * @param realResolver  The URIResolver that does the resolution
     */
    DepResolver(GeneratingCache cache, URIResolver realResolver) {
      this.cache = cache;
      this.realResolver = realResolver;
    }

    /**
     * Resolve a URI, and add a dependency for it to the cache.
     *
     * @param href  Full or partial hyperlink reference
     * @param base  Base URI of the document
     * @return      A Source representing the resolved URI.
     */
    public Source resolve(String href, String base)
      throws TransformerException 
    {
      if (href.indexOf(' ') >= 0)
        href = href.replaceAll(" ", "%20");

      if (base != null && base.indexOf(' ') >= 0)
        base = base.replaceAll(" ", "%20");

      // First, do the real resolution.
      Source src = realResolver.resolve(href, base);

      // If it's a file, add a dependency on it.
      if (src != null && dependencyReceiver != null) 
      {
        String sysId = src.getSystemId();
        if (sysId != null && sysId.startsWith("file:")) {
          String path = sysId.substring("file:".length());
          while (path.startsWith("//"))
            path = path.substring(1);
          dependencyReceiver.addDependency(new FileDependency(path));
        }
      }

      // And we're done.
      return src;
    } // resolve()

    /** The cache to add dependencies to */
    GeneratingCache cache;

    /** Does the work of resolving the URI's */
    URIResolver realResolver;
  } // class DepResolver
} // class StylesheetCache
