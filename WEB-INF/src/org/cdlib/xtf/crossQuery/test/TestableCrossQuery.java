package org.cdlib.xtf.crossQuery.test;


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
import org.cdlib.xtf.crossQuery.CrossQuery;
import org.cdlib.xtf.crossQuery.QueryRoute;
import org.cdlib.xtf.servletBase.TextConfig;
import org.cdlib.xtf.test.FakeServletConfig;
import org.cdlib.xtf.test.FakeServletContext;
import org.cdlib.xtf.test.FakeServletRequest;
import org.cdlib.xtf.test.FakeServletResponse;
import org.cdlib.xtf.test.NullOutputStream;
import org.cdlib.xtf.textEngine.DefaultQueryProcessor;
import org.cdlib.xtf.textEngine.QueryProcessor;
import org.cdlib.xtf.textEngine.QueryRequest;
import org.cdlib.xtf.textEngine.QueryResult;
import org.cdlib.xtf.util.AttribList;

/**
 * Derived version of the crossQuery servlet, used to abuse crossQuery during
 * load tests. The main difference is that it throws exceptions upward instead
 * of formatting an error page. This ensures that exceptions don't get hidden
 * in the noise.
 * 
 * Also, for each thread we track the number of hits returned by the last
 * request.
 *
 * @author Martin Haye
 */
public class TestableCrossQuery extends CrossQuery 
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
  public TestableCrossQuery(String baseDir) throws ServletException
  {
    this.baseDir = baseDir;
    FakeServletContext context = new FakeServletContext();
    FakeServletConfig config = new FakeServletConfig(context, baseDir, "crossQuery");
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
  protected QueryRequest runQueryParser(HttpServletRequest req,
                                        HttpServletResponse res,
                                        QueryRoute route, 
                                        AttribList attribs)
    throws Exception 
  {
    QueryRequest queryReq = super.runQueryParser(req, res, route, attribs);
    if (indexDirOverride != null)
      queryReq.indexPath = indexDirOverride;
    return queryReq;
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

  // inherit Javadoc
  @Override
  protected void formatHits(String mainTagName, HttpServletRequest req,
                            HttpServletResponse res, AttribList attribs,
                            QueryRequest queryRequest, QueryResult queryResult,
                            long startTime)
    throws Exception 
  {
    nHits.set(queryResult.totalDocs);
    super.formatHits(mainTagName,
                     req,
                     res,
                     attribs,
                     queryRequest,
                     queryResult,
                     startTime);
  }

  // inherit Javadoc
  @Override
  protected void genErrorPage(HttpServletRequest req, HttpServletResponse res,
                              Exception exc) 
  {
    nHits.set(-1);
    throw new RuntimeException(exc);
  } // genErrorPage()
} // class TestableCrossQuery
