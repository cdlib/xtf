package org.cdlib.xtf.saxonExt.pipe;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.cdlib.xtf.saxonExt.ElementWithContent;
import org.cdlib.xtf.saxonExt.InstructionWithContent;
import org.cdlib.xtf.servletBase.TextServlet;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.instruct.TailCall;
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
 * Implements a Saxon extension that goes out and makes an HTTP request
 * (either GET or POST) and pipes the results directly through to the servlet
 * response output stream, bypassing any further stylesheet processing.
 */
public class PipeRequestElement extends ElementWithContent 
{
  public void prepareAttributes() throws XPathException 
  {
    String[] mandatoryAtts = { "url" };
    String[] optionalAtts = { "timeout", "method" };
    parseAttributes(mandatoryAtts, optionalAtts);
  }

  public Expression compile(Executable exec) throws XPathException { 
    return new PipeRequestInstruction(attribs, compileContent(exec));
  }

  /** Worker class for PipeRequestElement */
  private static class PipeRequestInstruction extends InstructionWithContent 
  {
    public PipeRequestInstruction(Map<String, Expression> attribs, Expression content) 
    {
      super("pipe:pipeRequest", attribs, content);
    }

    /**
     * The real workhorse.
     */
    @Override
    public TailCall processLeavingTail(XPathContext context) 
      throws XPathException 
    {
      byte[] buf = null;
      OutputStream postOut = null;
      InputStream reqIn = null;
      OutputStream servOut = null;
      
      // Build the full URL
      URL fullURL = null;
      try {
        fullURL = new URL(attribs.get("url").evaluateAsString(context));
      }
      catch (MalformedURLException e) {
        dynamicError("'url' must be a well-formed URL", "PIPE_REQ_001", context);
      }

      // Parse the timeout if specified, and convert from seconds to milliseconds.
      int timeoutMsec = 0;
      if (attribs.containsKey("timeout")) {
        String timeoutStr = attribs.get("timeout").evaluateAsString(context);
        if (timeoutStr != null) {
          try {
            timeoutMsec = (int)(Float.parseFloat(timeoutStr) * 1000);
            timeoutMsec = Math.max(0, timeoutMsec);
          }
          catch (NumberFormatException e) {
            dynamicError("'timeout' must be a number", "PIPE_REQ_002", context);
          }
        }
      }
      
      // Parse the method if specified.
      String method = "GET";
      if (attribs.containsKey("method")) {
        String methodStr = attribs.get("method").evaluateAsString(context);
        if (methodStr == null || methodStr.equals("GET"))
          method = "GET";
        else if (methodStr.equals("POST"))
          method = "POST";
        else
          dynamicError("'method' must be 'GET' or 'POST'", "PIPE_REQ_003", context);
      }
      
      // Is there input to send? If so always use POST instead of GET.
      // Convert the content to a string (we can't use evaluateAsString() since
      // it only pays attention to the first item in a sequence.)
      //
      byte[] inputBytes = new byte[0];
      if (content != null) {
        String inputStr = sequenceToString(content, context).trim();
        if (inputStr.length() > 0) {
          try {
            inputBytes = inputStr.getBytes("UTF-8");
          } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
          }
          method = "POST";
        }
      }
      
      // Now the real work begins.
      try
      {
        // Get a connection object and set timeout on it.
        HttpURLConnection conn = (HttpURLConnection) fullURL.openConnection();
        if (timeoutMsec > 0) {
          conn.setConnectTimeout(timeoutMsec);
          conn.setReadTimeout(timeoutMsec);
        }
        
        if (method.equals("POST") || inputBytes.length > 0) 
        {
          // Set properties for a POST.
          conn.setRequestMethod("POST");
          conn.setRequestProperty("Content-Type", "text/plain");
          conn.setRequestProperty("Content-Length", Integer.toString(inputBytes.length));
          conn.setUseCaches(false);
          conn.setDoInput(true);
          conn.setDoOutput(true);
          
          // Send the request data.
          conn.connect();
          postOut = conn.getOutputStream();
          postOut.write(inputBytes);
          postOut.flush();
          postOut.close();
          postOut = null;
        }
        else 
        {
          // Standard GET request... default properties are fine.
          conn.connect();
        }
        
        // Copy the HTTP status to our outgoing response.
        HttpServletResponse servletResponse = TextServlet.getCurResponse();
        int resCode = conn.getResponseCode();
        servletResponse.setStatus(resCode);
        
        // Copy all the applicable HTTP headers to our outgoing response.
        for (int i=0; true; i++) {
          String key = conn.getHeaderFieldKey(i);
          String val = conn.getHeaderField(i);
          if (val == null)
            break;
          if (key != null && !key.equals("Connection"))
            servletResponse.setHeader(key, val);
        }

        // Pump through the rest of the response without any interpretation.
        reqIn = conn.getInputStream();
        servOut = servletResponse.getOutputStream();
        buf = PipeBufferPool.allocBuffer();
        int got;
        while ((got = reqIn.read(buf)) >= 0)
          servOut.write(buf, 0, got);
        servOut.flush();
        
      } 
      catch (SocketTimeoutException e) {
        dynamicError("External piped request timed out", "PIPE_REQ_004", context);
      }
      catch (IOException e) {
        dynamicError("IO Error during pipe request: " + e.toString(), "PIPE_REQ_005", context);
      }
      finally {
        if (buf != null)
          PipeBufferPool.deallocBuffer(buf);
        if (servOut != null)
          try { servOut.close(); } catch (IOException e) { /* ignore */ }
        if (reqIn != null)
          try { reqIn.close(); } catch (IOException e) { /* ignore */ }
        if (postOut != null)
          try { postOut.close(); } catch (IOException e) { /* ignore */ }
      }
          
      // All done.
      return null;
    }
  }
}
