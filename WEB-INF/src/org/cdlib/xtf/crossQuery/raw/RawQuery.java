package org.cdlib.xtf.crossQuery.raw;


/**
 * Copyright (c) 2007, Regents of the University of California
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
import java.io.StringReader;
import java.net.SocketException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.stream.StreamSource;

import org.cdlib.xtf.crossQuery.CrossQuery;
import org.cdlib.xtf.crossQuery.CrossQueryConfig;
import org.cdlib.xtf.servletBase.RedirectException;
import org.cdlib.xtf.servletBase.TextConfig;
import org.cdlib.xtf.textEngine.QueryProcessor;
import org.cdlib.xtf.textEngine.QueryRequest;
import org.cdlib.xtf.textEngine.QueryRequestParser;
import org.cdlib.xtf.textEngine.QueryResult;
import org.cdlib.xtf.util.AttribList;

/**
 * Derived version of the crossQuery servlet, used to provide a "web service"
 * interface to XTF. Takes an HTTP post containing a single XTF query in XML, 
 * parses the request, executes the query, and returns raw XML-formatted 
 * results. 
 *
 * @author Martin Haye
 */
public class RawQuery extends CrossQuery 
{
  /** 
   * We're keeping this servlet intentionally very simple -- so no config file.
   */
  protected TextConfig readConfig(String configPath) 
  {
    config = new RawQueryConfig(this);
    return config;
  }
  
  // inherit JavaDoc
  public String getConfigName() {
    return "no config file";
  }
  
  /**
   * Handles the HTTP 'get' and 'put' methods. Initializes the servlet if 
   * nececssary, then parses the HTTP request and processes it appropriately.
   *
   * @param     req            The HTTP request (in)
   * @param     res            The HTTP response (out)
   * @exception IOException    If unable to read an index or data file, or
   *                           if unable to write the output stream.
   */
  public void doGet(HttpServletRequest req, HttpServletResponse res)
    throws IOException 
  {
    try 
    {
      // Set the default output content type
      res.setContentType("text/xml");

      // If an error occurs, be sure to just format it "raw" (don't use an
      // error generator stylesheet)
      //
      req.setAttribute("org.cdlib.xtf.servlet.raw", "1");
      
      // This does the bulk of the work.
      apply(req, res);
    }
    catch (Exception e) {
      if (!(e instanceof SocketException)) 
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

  // inherit JavaDoc
  public String getServletInfo() {
    return "rawQuery search servlet";
  } // getServletInfo()

  /**
  * Creates the query request, processes it, and formats the results.
  *
  * @param req        The original HTTP request
  * @param res        Where to send the response
  *
  * @exception Exception  Passes on various errors that might occur.
  */
  protected void apply(HttpServletRequest req,
                       HttpServletResponse res)
    throws Exception 
  {
    // Record the start time.
    long startTime = System.currentTimeMillis();

    // Grab the "query" parameter -- it must be present.
    String queryText = req.getParameter("query");
    if (queryText == null || queryText.length() == 0)
      throw new RuntimeException("'query' parameter must be specified");
    
    // Parse the XML query to make an XTF QueryRequest
    QueryRequest queryReq = new QueryRequestParser().parseRequest(
      new StreamSource(new StringReader(queryText)),
      new File(getRealPath("")));

    // Fill in the auxiliary info
    queryReq.parserInput = null;
    queryReq.parserOutput = queryText;

    // Process it to generate result document hits
    QueryProcessor proc = createQueryProcessor();
    QueryResult queryResult = proc.processRequest(queryReq);

    // Format the hits for the output document.
    formatHits("crossQueryResult",
               req,
               res,
               new AttribList(),
               queryReq,
               queryResult,
               startTime);
  } // apply()

  private class RawQueryConfig extends CrossQueryConfig
  {
    RawQueryConfig(RawQuery servlet) { super(servlet); }
  }} // class TestableCrossQuery
