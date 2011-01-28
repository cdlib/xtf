package org.cdlib.xtf.test;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.cdlib.xtf.crossQuery.CrossQuery;
import org.cdlib.xtf.dynaXML.DynaXML;
import org.cdlib.xtf.util.Trace;

// Copyright (c) 2010, Regents of the University of California.
// See license at end of file.
 
/**
 * Allows command-line access to crossQuery and dynaXML by simulating a servlet
 * container.
 */
public class FakeServletContainer 
{
  private static CrossQuery crossQuery;
  private static DynaXML dynaXML;
  private static FakeServletConfig config;

  private static void service(HttpServlet servlet, String url) 
    throws ServletException, IOException
  {
    FakeServletRequest req = new FakeServletRequest(url);
    FakeOutputStream out = new FakeOutputStream();
    FakeServletResponse res = new FakeServletResponse(out);
    servlet.service(req, res);
    System.out.print(out.buf.toString());
  }
  
  private static void service(String url) 
    throws ServletException, IOException, InterruptedException
  {
    Pattern pausePat = Pattern.compile("pause\\((\\d+)\\)");
    Matcher m = pausePat.matcher(url);
    if (m.matches()) {
      int secs = Integer.parseInt(m.group(1));
      Trace.info(String.format("Pausing %s seconds", secs));
      Thread.sleep(secs);
    }
    else if (url.contains("/search")) {
      if (crossQuery == null) {
        crossQuery = new CrossQuery();
        crossQuery.init(config);
      }
      Trace.info("Servicing crossQuery URL '" + url + "'");
      service(crossQuery, url);
    }
    else if (url.contains("/view")) {
      if (dynaXML == null) {
        dynaXML = new DynaXML();
        dynaXML.init(config);
      }
      Trace.info("Servicing dynaXML URL '" + url + "'");
      service(dynaXML, url);
    }
    else
      Trace.warning("Unrecognized URL pattern: '" + url + "'");
  }
  
  public static void main(String[] args) 
    throws InterruptedException, ServletException, IOException
  {
    FakeServletContext context = new FakeServletContext();
    config = new FakeServletConfig(context, System.getProperty("user.dir"), "crossQuery");

    for (int i=0; i<args.length; i++)
      service(args[i]);
  }
}

/**
 * Copyright (c) 2010, Regents of the University of California. 
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