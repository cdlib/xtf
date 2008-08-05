package org.cdlib.xtf.saxonExt.exec;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.cdlib.xtf.servletBase.TextServlet;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;

/*
 * Copyright (c) 2008, Regents of the University of California
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
 * Helper class that does most of the work for {@link PipeImageElement}.
 */
public class PipeImageInstruction extends RunInstruction 
{
  public PipeImageInstruction(Expression command, int timeout, List args)
  {
    super(command, timeout, args);
  }
  
  public String getExpressionType() {
    return "exec:pipeImage";
  }

  public Item evaluateItem(XPathContext context)
    throws XPathException 
  {
    String[] argArray = gatherArgs(context);
    byte[] outBytes = runAndGrab(context, argArray);
    
    // Figure out whether the returned data is PNG or JPEG data
    int[] pngHeader  = { 0x89, 0x50, 0x4e, 0x47 };
    int[] jpegHeader = { 0xff, 0xd8, 0xff, 0xe0 };
    boolean isPNG  = true;
    boolean isJPEG = true;
    for (int i = 0; i < 4; i++) {
      if (i >= outBytes.length || outBytes[i] != (byte)pngHeader[i])
        isPNG = false;
      if (i >= outBytes.length || outBytes[i] != (byte)jpegHeader[i])
        isJPEG = false;
    }

    // It better be one or the other
    if (!isPNG && !isJPEG) {
      dynamicError(
          "Error: no PNG or JPEG returned by external command '" + command,
          "IMPI0001", context);
    }
    
    // Set the corresponding MIME type
    HttpServletResponse res = TextServlet.getCurResponse();
    if (isPNG)
      res.setContentType("image/png");
    else
      res.setContentType("image/jpeg");
    
    // Finally, output the result.
    ServletOutputStream out;
    try {
      out = res.getOutputStream();
      out.write(outBytes);
    } catch (IOException e) {
      dynamicError("Exception while writing output stream", "IMPI0002", context);
    }
    return null;
  } // evaluateItem()

} // class PipeImageInstruction
