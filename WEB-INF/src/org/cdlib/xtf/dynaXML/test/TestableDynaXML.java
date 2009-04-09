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
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;

import org.cdlib.xtf.dynaXML.DocRequest;
import org.cdlib.xtf.dynaXML.DynaXML;
import org.cdlib.xtf.dynaXML.InvalidDocumentException;
import org.cdlib.xtf.lazyTree.SearchTree;
import org.cdlib.xtf.servletBase.TextConfig;
import org.cdlib.xtf.test.FakeServletConfig;
import org.cdlib.xtf.test.FakeServletContext;
import org.cdlib.xtf.test.FakeServletRequest;
import org.cdlib.xtf.test.FakeServletResponse;
import org.cdlib.xtf.test.NullOutputStream;
import org.cdlib.xtf.textEngine.DefaultQueryProcessor;
import org.cdlib.xtf.textEngine.QueryProcessor;
import org.cdlib.xtf.util.AttribList;
import org.xml.sax.SAXException;

/**
 * Extends the DynaXML servlet slightly to allow programmatic testing of
 * the servlet. Authentication always succeeds. Any exceptions will be
 * thrown upward rather than generating an error page.
 *
 * @author Martin Haye
 */
public class TestableDynaXML extends DynaXML 
{
  private String baseDir;
  private String indexDirOverride;
  private ThreadLocal<Integer> nHits = new ThreadLocal<Integer>();
  
  /**
   * Simplified initialization for use outside a real servlet container.
   * 
   * @param baseDir the XTF home directory.
   * @throws ServletException if anything goes wrong.
   */
  public TestableDynaXML(String baseDir) throws ServletException
  {
    this.baseDir = baseDir;
    FakeServletContext context = new FakeServletContext();
    FakeServletConfig config = new FakeServletConfig(context, baseDir, "dynaXML");
    super.init(config);
  }
  
  /** Allows overriding the directory specified in future query requests. */
  public void overrideIndexDir(String dir) {
    indexDirOverride = dir;
  }
  
  /** Return the number of hits in the last request processed by this thread */
  public int nHits() { return nHits.get(); }
  
  /** For test mode, do nothing to the current trace flags. */
  @Override
  protected void setupTrace(TextConfig config) { }
  
  /** Allow overriding the index directory */
  @Override
  protected DocRequest runDocReqParser(HttpServletRequest req,
                                       AttribList attribs)
    throws Exception
  {
    DocRequest docRequest = super.runDocReqParser(req, attribs);
    if (indexDirOverride != null && docRequest.query != null)
      docRequest.query.indexPath = indexDirOverride;
    return docRequest;
  }

  /** For test mode, don't do background warming. */
  @Override
  protected QueryProcessor getQueryProcessor()
  {
    DefaultQueryProcessor processor = new DefaultQueryProcessor();
    processor.setXtfHome(baseDir);
    return processor;
  }

  /**
   * Simplified method to test-get the given URL. Throws away the output
   * but retains the number of hits.
   * 
   * @param url the URL to test-get
   */
  public void service(String url) throws ServletException, IOException
  {
    FakeServletRequest req = new FakeServletRequest(url);
    NullOutputStream out = new NullOutputStream();
    FakeServletResponse res = new FakeServletResponse(out);
    super.service(req, res);
  }

  /**
   * Perform the normal dynaXML getSourceDoc() operation, then record the
   * resulting number of hits (if any) that came out.
   */
  @Override
  protected Source getSourceDoc(DocRequest docReq, Transformer transformer) 
    throws InvalidDocumentException, IOException, SAXException, ParserConfigurationException
  {
    Source src = super.getSourceDoc(docReq, transformer);
    if (src instanceof SearchTree)
      nHits.set(((SearchTree)src).getTotalHits());
    else
      nHits.set(0);
    return src;
  }
  
  /**
   * Performs user authentication for a request, given the authentication
   * info for the document. In the case of testing, we never fail
   * authentication.
   */
  @Override
  protected boolean authenticate(DocRequest docReq,
                                 HttpServletRequest req, HttpServletResponse res)
    throws Exception 
  {
    return true;
  }

  /**
   * Would normally generate an error page. Instead we throw an exception
   * upward.
   */
  @Override
  protected void genErrorPage(HttpServletRequest req, HttpServletResponse res,
                              Exception exc) 
  {
    throw new RuntimeException(exc);
  } // genErrorPage()
} // class TestableDynaXML
