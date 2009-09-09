package org.cdlib.xtf.saxonExt.pipe;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.cdlib.xtf.saxonExt.ElementWithContent;
import org.cdlib.xtf.saxonExt.InstructionWithContent;
import org.cdlib.xtf.servletBase.TextServlet;
import org.cdlib.xtf.xslt.FileUtils;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.instruct.TailCall;
import net.sf.saxon.trans.XPathException;

/**
 * Pipes the contents of a file directly to the servlet request
 * output stream, bypassing any further stylesheet processing.
 */
public class PipeFileElement extends ElementWithContent 
{
  public void prepareAttributes() throws XPathException 
  {
    String[] mandatoryAtts = { "path", "mimeType" };
    String[] optionalAtts = { "fileName" };
    parseAttributes(mandatoryAtts, optionalAtts);
  }

  public Expression compile(Executable exec) throws XPathException { 
    return new PipeFileInstruction(attribs, compileContent(exec));
  }

  /** Worker class for PipeFileElement */
  private static class PipeFileInstruction extends InstructionWithContent 
  {
    public PipeFileInstruction(Map<String, Expression> attribs, Expression content) 
    {
      super("pipe:pipeFile", attribs, content);
    }

    /**
     * The real workhorse.
     */
    @Override
    public TailCall processLeavingTail(XPathContext context) 
      throws XPathException 
    {
      // Build the full path.
      String path = attribs.get("path").evaluateAsString(context);
      File file = FileUtils.resolveFile(context, path);
      
      // Make sure it's readable.
      if (!file.canRead()) {
        dynamicError("Cannot read path '" + path + "' (resolved to '" + file.toString() + "'", 
                     "PIPE_FILE_001", context);
      }
      
      // Set the content length and type
      HttpServletResponse servletResponse = TextServlet.getCurResponse();
      servletResponse.setHeader("Content-length", Long.toString(file.length()));
      servletResponse.setHeader("Content-type", attribs.get("mimeType").evaluateAsString(context));
      
      // If file name specified, add the Content-disposition header.
      String fileName;
      if (attribs.containsKey("fileName")) {
        fileName = attribs.get("fileName").evaluateAsString(context);
        servletResponse.setHeader("Content-disposition", "attachment; filename=\"" + fileName + "\"");
      }
      
      // Now copy the file to the output stream.
      byte[] buf = null;
      InputStream fileIn = null;
      OutputStream servOut = null;
      try
      {
        fileIn = new FileInputStream(file);
        servOut = servletResponse.getOutputStream();
        buf = PipeBufferPool.allocBuffer();
        int got;
        while ((got = fileIn.read(buf)) >= 0)
          servOut.write(buf, 0, got);
        servOut.flush();
      } 
      catch (IOException e) {
        dynamicError("IO Error while piping file: " + e.toString(), "PIPE_FILE_002", context);
      }
      finally 
      {
        // Clean up after ourselves.
        if (buf != null)
          PipeBufferPool.deallocBuffer(buf);
        if (servOut != null)
          try { servOut.close(); } catch (IOException e) { /* ignore */ }
        if (fileIn != null)
          try { fileIn.close(); } catch (IOException e) { /* ignore */ }
      }
          
      // All done.
      return null;
    }
  }
}
