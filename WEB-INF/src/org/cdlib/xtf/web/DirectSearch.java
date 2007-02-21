package org.cdlib.xtf.web;


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
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class DirectSearch extends HttpServlet 
{
  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException 
  {
    String searchMode = request.getParameter("search.mode");
    String queryString = request.getParameter("query");
    String docPath = request.getParameter("doc.path");
    String docView = request.getParameter("doc.view");
    String chunkID = request.getParameter("chunk.id");
    String tocDepth = request.getParameter("toc.depth");
    String tocID = request.getParameter("toc.id");

    String location;

    if (searchMode.equals("thisbook")) 
    {
      if (docView.equals("frames") ||
          docView.equals("bbar") ||
          docView.equals("toc") ||
          docView.equals("content")) 
      {
        location = new StringBuffer().append("").toString();
        location = new StringBuffer(location).append(docPath).toString();
        location = new StringBuffer(location).append("&doc.view=frames")
                   .toString();
        location = new StringBuffer(location).append("&chunk.id=").toString();
        location = new StringBuffer(location).append(chunkID).toString();
        location = new StringBuffer(location).append("&toc.depth=").toString();
        location = new StringBuffer(location).append(tocDepth).toString();
        location = new StringBuffer(location).append("&toc.id=").toString();
        location = new StringBuffer(location).append(tocID).toString();
        location = new StringBuffer(location).append("&query=").toString();
        location = new StringBuffer(location).append(queryString).toString();
      }
      else {
        location = new StringBuffer().append("").toString();
        location = new StringBuffer(location).append(docPath).toString();
        location = new StringBuffer(location).append("&toc.depth=").toString();
        location = new StringBuffer(location).append(tocDepth).toString();
        location = new StringBuffer(location).append("&toc.id=").toString();
        location = new StringBuffer(location).append(tocID).toString();
        location = new StringBuffer(location).append("&query=").toString();
        location = new StringBuffer(location).append(queryString).toString();
      }

      // Debugging

      //response.setContentType("text/plain");
      //PrintWriter out = response.getWriter();
      //out.println(location);

      // Alternate method using forward
      // Works, but URL might confuse users

      //RequestDispatcher dispatcher = request.getRequestDispatcher(location);
      //dispatcher.forward(request, response);
      response.sendRedirect(location);
    }
    else {
      location = new StringBuffer().append("/search?").toString();
      location = new StringBuffer(location).append("text=").toString();
      location = new StringBuffer(location).append(queryString).toString();

      // Debugging

      //response.setContentType("text/plain");
      //PrintWriter out = response.getWriter();
      //out.println(location);
      response.sendRedirect(location);
    }
  }
}
