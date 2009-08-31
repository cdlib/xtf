package org.cdlib.xtf.saxonExt.exec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import javax.servlet.http.HttpServletResponse;

import org.cdlib.xtf.servletBase.TextServlet;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.SimpleExpression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
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
 * Helper class that does most of the work for {@link PipeRequestElement}.
 */
public class PipeRequestInstruction extends SimpleExpression 
{
  protected Expression url;
  protected int timeout;
  protected int nArgs;
  protected InputElement.InputInstruction inputExpr;
  protected String method;
  
  // Keep some buffers around for re-use, to minimize per-request mem gobbling.
  protected static final int MAX_SPARE_BUFS = 4;
  protected static final int BUF_SIZE = 32*1024; // 32 Kbytes
  protected static LinkedList spareBuffers = new LinkedList();

  /**
   * Allocate a buffer to use for I/O. Uses previously allocated buffer if 
   * possible (that buffer must have been deallocated using deallocBuffer()).
   */
  protected static synchronized byte[] allocBuffer()
  {
    byte[] buf = null;
    
    // Look for a previous buffer we can use.
    ListIterator iter = spareBuffers.listIterator();
    while (iter.hasNext() && buf == null)
    {
      Object obj = iter.next();
      iter.remove();
      
      // If it's a weak reference, the buffer might still be around.
      if (obj instanceof WeakReference) 
      {
        WeakReference<byte[]> ref = (WeakReference<byte[]>)obj;
        buf = ref.get();
      }
      else
        buf = (byte[]) obj;
    }

    // If no buffers available to re-use, create a new one.
    if (buf == null)
      buf = new byte[BUF_SIZE];
    
    // All done.
    return buf;
  }
  
  /**
   * Return a buffer so it can be re-used later. If we already have enough
   * spare buffers then make it a weak reference so the buffer can be 
   * garbage-collected.
   */
  protected static synchronized void deallocBuffer(byte[] buf)
  {
    // Remove buffers which got garbage-collected from the list.
    ListIterator iter = spareBuffers.listIterator();
    while (iter.hasNext()) {
      Object obj = iter.next();
      if (obj instanceof WeakReference && ((WeakReference)obj).get() == null)
        iter.remove();
    }
      
    // If we could use another permanent buffer, keep forever.
    if (spareBuffers.size() < MAX_SPARE_BUFS)
      spareBuffers.addFirst(buf);
    else 
    {
      // Otherwise make a weak reference so the buffer can be garbage
      // collected. There's still the chance that we'll get to re-use
      // it.
      //
      spareBuffers.addFirst(new WeakReference(buf));
    }
  }

  // Constructor.
  public PipeRequestInstruction(Expression url, int timeout, String method, List args) 
  {
    this.url = url;
    this.timeout = timeout;
    this.method = method;

    nArgs = args.size();

    if (args.size() > 0 &&
        args.get(args.size() - 1) instanceof InputElement.InputInstruction) 
    {
      inputExpr = (InputElement.InputInstruction)args.get(args.size() - 1);
      --nArgs;
    }

    Expression[] sub = new Expression[args.size()];
    for (int i = 0; i < args.size(); i++)
      sub[i] = (Expression)args.get(i);
    setArguments(sub);
  }
  
  /**
   * A subclass must provide one of the methods evaluateItem(), iterate(), or process().
   * This method indicates which of the three is provided.
   */
  public int getImplementationMethod() {
    return Expression.EVALUATE_METHOD;
  }

  public String getExpressionType() {
    return "exec:pipeRequest";
  }
  
  /**
   * The real workhorse.
   */
  public Item evaluateItem(XPathContext context)
    throws XPathException 
  {
    byte[] buf = null;
    OutputStream postOut = null;
    InputStream reqIn = null;
    OutputStream servOut = null;
    
    try
    {
      // Build the full URL
      StringBuilder sb = new StringBuilder();
      sb.append(url.evaluateAsString(context)); // protocol://server[:port]/path
      for (int c = 0; c < nArgs; c++) {
        if (c == 0)
          sb.append("?");
        else
          sb.append("&");
        String strVal = ((ArgElement.ArgInstruction)arguments[c]).getSelectValue(
          context).getStringValue();
        sb.append(strVal);
      } // for c
      
      URL fullURL = new URL(sb.toString());
      
      // Get a connection object and set timeout on it.
      HttpURLConnection conn = (HttpURLConnection) fullURL.openConnection();
      if (timeout > 0) {
        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);
      }
      
      // Is there input to send? If so use POST instead of GET
      byte[] inputBytes = new byte[0];
      if (inputExpr != null)
        inputBytes = inputExpr.getStream(context);
      
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
      buf = allocBuffer();
      int got;
      while ((got = reqIn.read(buf)) >= 0)
        servOut.write(buf, 0, got);
      servOut.flush();
      
    } 
    catch (SocketTimeoutException e) {
      dynamicError("External piped request timed out", "IMPI0002", context);
    }
    catch (IOException e) {
      dynamicError("IO Error during pipe request: " + e.toString(), "IMPI0001", context);
    }
    finally {
      if (buf != null)
        deallocBuffer(buf);
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

} // class PipeImageInstruction
