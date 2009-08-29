package org.cdlib.xtf.saxonExt.exec;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.cdlib.xtf.servletBase.TextServlet;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.SimpleExpression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.DynamicError;
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
  
  private long timeRemaining(XPathContext context, long startTime) 
    throws DynamicError
  {
    // If no timeout, no need to enforce anything.
    if (timeout <= 0)
      return 0;
    
    long spent = System.currentTimeMillis() - startTime;
    if (spent >= timeout)
      dynamicError("Request timed out", "IMPI0001", context);
    return timeout - spent;
  }
  
  public Item evaluateItem(XPathContext context)
    throws XPathException 
  {
    BufferedWriter sockWriter = null;
    InputStreamReader sockReader = null;

    try
    {
      // Check the protocol and extract the host part of the URL
      String initialUrl = url.evaluateAsString(context);
      URL parsedUrl = new URL(initialUrl);
  
      if (!parsedUrl.getProtocol().equals("http"))
        dynamicError("Error: only HTTP protocol is supported", "IMPI0001", context);
      
      String host = parsedUrl.getHost();
      if (parsedUrl.getHost() == null)
        dynamicError("Error: must specify full URL including host name", "IMPI0002", context);
  
      // Get the port.
      int port = parsedUrl.getPort();
      if (port < 0)
        port = 80;
        
      // Build the rest of the URL.
      StringBuilder sb = new StringBuilder();
      sb.append(parsedUrl.getPath());
      
      for (int c = 0; c < nArgs; c++) {
        if (c == 0)
          sb.append("?");
        else
          sb.append("&");
        String strVal = ((ArgElement.ArgInstruction)arguments[c]).getSelectValue(
          context).getStringValue();
        sb.append(strVal);
      } // for c
      
      String pathWithArgs = sb.toString();
      
      // We're going to want to time things.
      long startTime = System.currentTimeMillis();
      
      System.out.println("Begin PipeRequest");
      
      // Create a socket to the host
      InetAddress addr = InetAddress.getByName(host);
      InetSocketAddress sockAddr = new InetSocketAddress(addr, port);
      SocketChannel sockChan = SocketChannel.open();
      sockChan.configureBlocking(false);
      Selector selector = Selector.open();
      SelectionKey selKey = sockChan.register(selector, SelectionKey.OP_CONNECT);
      if (!sockChan.connect(sockAddr))
        connectWithTimeout(context, startTime, sockChan, selector);

      // Send header
      ByteArrayOutputStream sockOutStream = new ByteArrayOutputStream();
      sockWriter = new BufferedWriter(new OutputStreamWriter(sockOutStream, "UTF8"));
      
      // Is there input to send? If so use POST instead of GET
      byte[] inputBytes = new byte[0];
      if (inputExpr != null)
        inputBytes = inputExpr.getStream(context);
      
      if (method.equals("POST") || inputBytes.length > 0) {
        sockWriter.write("POST " + pathWithArgs + " HTTP/1.0\r\n");
        sockWriter.write("Content-Length: " + inputBytes.length + "\r\n");
        sockWriter.write("Content-Type: text/plain\r\n");
        sockWriter.write("\r\n");
        sockWriter.flush();
        sockOutStream.write(inputBytes);
        sockOutStream.flush();
      }
      else {
        sockWriter.write("GET " + pathWithArgs + " HTTP/1.0\r\n\r\n");
        sockWriter.flush();
      }

      // Write it to the socket.
      writeWithTimeout(context, startTime, sockChan, selector, selKey, 
                       sockOutStream.toByteArray());
      
      // Get response
      HttpServletResponse res = TextServlet.getCurResponse();
      String line;
      int lineNum = 0;

      // Grab the first part of the response into our buffer
      byte[] buf = new byte[4096];
      int bufPos = 0;
      boolean eof = false;
      selKey.interestOps(SelectionKey.OP_READ);
      boolean needSelect = true;
      while (bufPos < buf.length && !eof)
      {
        if (needSelect) {
          if (selector.select(timeRemaining(context, startTime)) > 0)
            needSelect = false;
        }
        ByteBuffer bb = ByteBuffer.wrap(buf, bufPos, buf.length - bufPos);
        int got = sockChan.read(bb);
        if (got == 0)
          needSelect = true;
        else if (got < 0)
          eof = true;
        else
          bufPos += got;
      }
      
      // Find the end of the headers.
      int newlineCt = 0;
      int i;
      for (i=0; i<bufPos; i++) {
        if (buf[i] == '\r')
          continue;
        else if (buf[i] == '\n') {
          ++newlineCt;
          if (newlineCt == 2)
            break;
        }
        else
          newlineCt = 0;
      }
      
      if (i == bufPos)
        dynamicError("No HTTP headers returned, or headers too long.", "IMPI0003", context);
      int headerEnd = i+1; // skip newline
      
      // Parse all the headers and copy them to the servlet response.
      BufferedReader headerReader = new BufferedReader(
          new InputStreamReader(new ByteArrayInputStream(buf, 0, headerEnd)));
      while ((line = headerReader.readLine()) != null) {
        if (lineNum == 0) {
          if (!line.matches("^HTTP/1.[^ ]+ +([0-9]+) .*$"))
            dynamicError("Incomprehensible HTTP response", "IMPI0004", context);
          String resCodeStr = line.replaceFirst("^[^ ]+ ", "").replaceFirst(" .*$", "");
          int resCode = Integer.parseInt(resCodeStr);
          String resMsg = line.replaceFirst("^.* [0-9]+ ?", "");
          if (resCode != 200)
            res.sendError(resCode, resMsg);
          else
            res.setStatus(resCode);
        }
        else if (line.trim().length() == 0)
          break;
        else {
          String hdrName = line.replaceAll(":.*", "");
          String hdrVal  = line.replaceAll("[^:]+: ?", "");
          
          // Ignore certain headers, as the servlet container handles them for us.
          if (!hdrName.equalsIgnoreCase("Connection"))
            res.setHeader(hdrName, hdrVal);
        }
        ++lineNum;
      }

      // Pump the rest through without any interpretation.
      OutputStream servletOutputStream = res.getOutputStream();
      if (bufPos > headerEnd)
        servletOutputStream.write(buf, headerEnd, bufPos - headerEnd);
      while (!eof)
      {
        if (needSelect) {
          //selKey = sockChan.register(selector, SelectionKey.OP_READ);
          if (selector.select(timeRemaining(context, startTime)) > 0)
            needSelect = false;
        }
        ByteBuffer bb = ByteBuffer.wrap(buf);
        int got = sockChan.read(bb);
        if (got == 0)
          needSelect = true;
        else if (got < 0)
          eof = true;
        else
          servletOutputStream.write(buf, 0, got);
      }
      
      System.out.println("End PipeRequest");
    } catch (IOException e) {
      dynamicError("IO Error during pipe request: " + e.toString(), "IMPI0005", context);
    }
    finally {
      try {
        if (sockWriter != null)
          sockWriter.close();
        //if (sockOutStream != null)
        //  sockOutStream.close();
        if (sockReader != null)
          sockReader.close();
      }
      catch (IOException e) {
        // ignore
      }
    }
        
    // All done.
    return null;
  }

  private void writeWithTimeout(XPathContext context, long startTime,
                                SocketChannel sockChan, 
                                Selector selector, SelectionKey selKey,
                                byte[] bytesToWrite) 
  throws IOException, DynamicError 
  {
    selKey.interestOps(SelectionKey.OP_WRITE);
    
    int pos = 0;
    boolean needSelect = true;
    while (pos < bytesToWrite.length)
    {
      if (needSelect) {
        if (selector.select(timeRemaining(context, startTime)) > 0)
          needSelect = false;
      }
      ByteBuffer bb = ByteBuffer.wrap(bytesToWrite, pos, bytesToWrite.length - pos); 
      int nWritten = sockChan.write(bb);
      pos += nWritten;
    }
  }

  private void connectWithTimeout(XPathContext context, long startTime,
                                  SocketChannel sockChan, Selector selector) 
    throws DynamicError, IOException 
  {
    while (true)
    {
      if (selector.select(timeRemaining(context, startTime)) <= 0)
        continue;
      if (!sockChan.finishConnect())
        dynamicError("Finish connect failed", "IMPI0004", context);
      return;
    }
  }

} // class PipeImageInstruction
