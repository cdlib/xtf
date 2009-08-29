package org.cdlib.xtf.saxonExt.exec;

import java.util.ArrayList;
import java.util.List;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.AxisIterator;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.style.ExtensionInstruction;
import net.sf.saxon.trans.XPathException;

/*
 * Copyright (c) 2009, Regents of the University of California
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

/**
 * Implements almost the same thing as Exec.run saxon extension, except that
 * instead of returning stdout as a string or XML, and instead of calling
 * a command line program it is interpreted as a request to another web service. 
 * The results of that request are sent directly to the servlet output stream.
 */
public class PipeRequestElement extends ExtensionInstruction 
{
  protected Expression url;
  protected int timeoutMsec;
  protected String method;

  public void prepareAttributes()
    throws XPathException 
  {
    // Get mandatory 'url' attribute
    url = makeAttributeValueTemplate(getAttributeList().getValue("", "url"));
    if (url == null) {
      reportAbsence("url");
      return;
    }

    // Get optional 'timeout' attribute
    String timeoutStr = getAttributeList().getValue("", "timeout");
    if (timeoutStr == null)
      timeoutMsec = 0;
    else 
    {
      try {
        timeoutMsec = (int)(Float.parseFloat(timeoutStr) * 1000);
        timeoutMsec = Math.max(0, timeoutMsec);
      }
      catch (NumberFormatException e) {
        compileError("'timeout' must be a number");
      }
    }
    
    // Get optional 'method' attribute
    String methodStr = getAttributeList().getValue("", "method");
    if (methodStr == null || methodStr.equals("GET"))
      method = "GET";
    else if (methodStr.equals("PUT"))
      method = "PUT";
    else
      compileError("'method' must be 'GET' or 'PUT'");
  } // prepareAttributes()

  public List getArgInstructions(Executable exec)
    throws XPathException 
  {
    List list = new ArrayList(10);

    AxisIterator kids = iterateAxis(Axis.CHILD);
    NodeInfo child;
    while (true) 
    {
      child = (NodeInfo)kids.next();
      if (child == null)
        break;
      if (child instanceof ArgElement)
        list.add(((ArgElement)child).compile(exec));
      if (child instanceof InputElement) {
        list.add(((InputElement)child).compile(exec));
      }
    }

    return list;
  } // getArgInstructions()

  
  public Expression compile(Executable exec)
    throws XPathException 
  {
    return new PipeRequestInstruction(url, timeoutMsec, method, getArgInstructions(exec));
  }
}
