package org.cdlib.xtf.zing;


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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;
import net.sf.saxon.om.NodeInfo;
import org.cdlib.xtf.crossQuery.CrossQuery;
import org.cdlib.xtf.crossQuery.QueryRoute;
import org.cdlib.xtf.servletBase.TextConfig;
import org.cdlib.xtf.textEngine.QueryProcessor;
import org.cdlib.xtf.textEngine.QueryRequest;
import org.cdlib.xtf.textEngine.QueryResult;
import org.cdlib.xtf.util.AttribList;
import org.cdlib.xtf.util.EasyNode;
import org.cdlib.xtf.util.XMLWriter;

/**
 * The SRU servlet coordinates the process of parsing a URL query,
 * activating the textEngine to find all occurrences, and finally formatting
 * the results.
 */
public class SRU extends CrossQuery 
{
  // inherit JavaDoc
  public String getConfigName() {
    return "conf/sru.conf";
  }

  // inherit JavaDoc
  protected TextConfig readConfig(String configPath) 
  {
    // Load the configuration file.
    config = new SRUConfig(this, configPath);

    // And we're done.
    return config;
  } // readConfig()

  // inherit JavaDoc
  public TextConfig getConfig() {
    return config;
  }

  // inherit JavaDoc
  public String getServletInfo() {
    return "SRU servlet";
  } // getServletInfo()

  // inherit JavaDoc
  protected void apply(AttribList attribs, HttpServletRequest req,
                       HttpServletResponse res)
    throws Exception 
  {
    // Record the start time.
    long startTime = System.currentTimeMillis();

    // Switch the default output mode to XML.
    res.setContentType("text/xml");

    // Make a default route, but set up to do CQL parsing on the query
    // parameter instead of the default tokenization.
    //
    QueryRoute route = QueryRoute.createDefault(config.queryParserSheet);
    route.tokenizerMap.put("query", "CQL");

    // Generate a query request document from the queryParser stylesheet.
    QueryRequest queryReq = runQueryParser(req, res, route, attribs);
    if (queryReq == null)
      return;

    // Process it to generate result document hits
    QueryProcessor proc = createQueryProcessor();
    QueryResult result = proc.processRequest(queryReq);

    // Format the hits for the output document. Include the <parameters> block
    // and the actual query request, in case the stylesheet wants to use these
    // things.
    //
    formatHits("SRUResult", req, res, attribs, queryReq, result, startTime);
  }

  /**
   * Called right after the raw query request has been generated, but
   * before it is parsed. Gives us a chance to stop processing here in
   * if SRW diagnostics should be output instead of running a query.
   */
  protected boolean shuntQueryReq(HttpServletRequest req,
                                  HttpServletResponse res, Source queryReqDoc)
    throws IOException 
  {
    // If it actually contains an SRW explain response, or an SRW
    // diagnostic, simply output that directly.
    //
    EasyNode node = new EasyNode((NodeInfo)queryReqDoc);
    if (directOutput(node, "diagnostics", res))
      return true;
    if (directOutput(node, "explainResponse", res))
      return true;

    return super.shuntQueryReq(req, res, queryReqDoc);
  } // shuntQueryReq()

  /** Add additional stuff to the usual debug step mode */
  protected String stepSetup(HttpServletRequest req, HttpServletResponse res)
    throws IOException 
  {
    String stepStr = super.stepSetup(req, res);
    if (stepStr != null) {
      stepStr = stepStr.replaceAll("crossQuery", "SRU");
      String step = req.getParameter("debugStep");
      if (step.equals("2a"))
        stepStr = stepStr.replaceAll("Next,",
                                     "Note that the 'query' parameter has been " +
                                     "parsed as CQL. Next,");
      stepStr = stepStr.replaceAll("final HTML", "final SRW-formatted XML");
      stepStr = stepStr.replaceAll("XML page", "XML result");
    }

    return stepStr;
  }

  /**
   * Scans the node and its descendants for an SRW 'explainResponse' or
   * 'diagnostics'. If found, it is output directly.
   *
   * @param node    Node to scan
   * @param name    Name to scan for
   * @return        true if direct output was made
   */
  private boolean directOutput(EasyNode node, String name,
                               HttpServletResponse res)
    throws IOException 
  {
    // If the node is an explainResponse or diagnostic, output it
    // directly.
    //
    if (name.equals(node.name())) {
      String strVal = XMLWriter.toString(node);
      res.getWriter().print(strVal);
      return true;
    }

    // Scan the children.
    for (int i = 0; i < node.nChildren(); i++) {
      if (directOutput(node.child(i), name, res))
        return true;
    }

    // None found in this branch.
    return false;
  } // directOutput()
} // class SRU
