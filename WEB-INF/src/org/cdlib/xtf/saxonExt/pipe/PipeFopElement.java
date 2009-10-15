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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;

import org.cdlib.xtf.saxonExt.ElementWithContent;
import org.cdlib.xtf.saxonExt.InstructionWithContent;
import org.cdlib.xtf.servletBase.TextServlet;
import org.cdlib.xtf.util.Trace;
import org.cdlib.xtf.xslt.FileUtils;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.BadPdfFormatException;
import com.lowagie.text.pdf.PdfCopy;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.SimpleBookmark;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.instruct.TailCall;
import net.sf.saxon.om.Item;
import net.sf.saxon.tinytree.TinyBuilder;
import net.sf.saxon.trans.XPathException;

/**
 * Implements a Saxon extension that runs the FOP processor
 * to transform XSL-FO formatting instructions into a PDF file,
 * and pipes that PDF back to the client.
 */
public class PipeFopElement extends ElementWithContent 
{
  private static FopFactory fopFactory;
  
  public void prepareAttributes() throws XPathException 
  {
    String[] mandatoryAtts = { };
    String[] optionalAtts = { "fileName", 
                              "author", "creator", "keywords", "producer", "title",
                              "appendPDF", "fallbackIfError" };
    parseAttributes(mandatoryAtts, optionalAtts);
  }

  public Expression compile(Executable exec) throws XPathException { 
    return new PipeFopInstruction(attribs, compileContent(exec));
  }

  /** Worker class for PipeFopElement */
  private static class PipeFopInstruction extends InstructionWithContent 
  {
    public PipeFopInstruction(Map<String, Expression> attribs, Expression content) 
    {
      super("pipe:pipeFop", attribs, content);
    }

    /**
     * The real workhorse.
     */
    @Override
    public TailCall processLeavingTail(XPathContext context) 
      throws XPathException 
    {
      // Set the content type
      HttpServletResponse servletResponse = TextServlet.getCurResponse();
      servletResponse.setHeader("Content-type", "application/pdf");
      
      // If file name specified, add the Content-disposition header.
      String fileName;
      if (attribs.containsKey("fileName")) {
        fileName = attribs.get("fileName").evaluateAsString(context);
        servletResponse.setHeader("Content-disposition", "attachment; filename=\"" + fileName + "\"");
      }
      
      try {

        // According to the Apache docs, FOP may not be thread-safe. So, we need to
        // single-thread it.
        //
        synchronized(FopFactory.class) 
        {
          // For speed, only create FOP factory if we haven't already got one.
          if (fopFactory == null)
            fopFactory = FopFactory.newInstance();
          
          // Apply the optional things that can be added to the PDF header
          FOUserAgent foAgent = fopFactory.newFOUserAgent();
          if (attribs.containsKey("author"))
            foAgent.setAuthor(attribs.get("author").evaluateAsString(context));
          if (attribs.containsKey("creator"))
            foAgent.setCreator(attribs.get("creator").evaluateAsString(context));
          if (attribs.containsKey("keywords"))
            foAgent.setKeywords(attribs.get("keywords").evaluateAsString(context));
          if (attribs.containsKey("producer"))
            foAgent.setProducer(attribs.get("producer").evaluateAsString(context));
          if (attribs.containsKey("title"))
            foAgent.setTitle(attribs.get("title").evaluateAsString(context));
          

          // Interesting workaround: using FOP normally results in an AWT "Window"
          // being created. However, since we're running in a servlet container, this
          // isn't generally desirable (and often isn't possible.) So we let AWT know
          // that it's running in "headless" mode, and this prevents the window from
          // being created.
          //
          System.setProperty("java.awt.headless", "true");
          
          // Despite generally being a NodeInfo, 'content' doesn't seem to work directly
          // as a Source. Fortunately TinyBuilder gives us very fast way to convert it.
          //
          Item contentItem = content.evaluateItem(context);
          Source src = TinyBuilder.build((Source)contentItem, null, context.getConfiguration());
          
          // Setup JAXP using identity transformer
          TransformerFactory transFactory = new net.sf.saxon.TransformerFactoryImpl();
          Transformer transformer = transFactory.newTransformer(); // identity transformer

          // Now run FOP
          ByteArrayOutputStream finalOut = new ByteArrayOutputStream();
          if (attribs.containsKey("appendPDF")) 
          {
            ByteArrayOutputStream tmpOut = new ByteArrayOutputStream();
            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foAgent, tmpOut);
            transformer.transform(src, new SAXResult(fop.getDefaultHandler()));
            appendPdf(context, tmpOut.toByteArray(), finalOut);
          }
          else 
          {
            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foAgent, finalOut);
            transformer.transform(src, new SAXResult(fop.getDefaultHandler()));
          }
          
          // Now we know the output length.
          servletResponse.setHeader("Content-length", Integer.toString(finalOut.size()));
          
          // Go ahead and send it.
          servletResponse.getOutputStream().write(finalOut.toByteArray());
        }
      } 
      catch (Throwable e) 
      {
        // Fall back if requested
        if (attribs.containsKey("fallbackIfError")) 
        {
          Trace.warning("pipeFop failed, falling back to just piping PDF file:" + e.toString());
          String fallbackStr = attribs.get("fallbackIfError").evaluateAsString(context);
          if (fallbackStr.matches("yes|Yes|true|True|1") && attribs.containsKey("appendPDF")) {
            try {
              String path = attribs.get("appendPDF").evaluateAsString(context);
              File file = FileUtils.resolveFile(context, path);
              servletResponse.setHeader("Content-length", Long.toString(file.length()));
              PipeFileElement.copyFileToStream(file, servletResponse.getOutputStream());
              e = null;
            }
            catch (IOException e2) {
              e = e2;
            }
          }
        }
        
        // Process the resulting exception
        if (e == null)
          ; // pass
        else if (e instanceof IOException)
          dynamicError("IO Error while piping FOP: " + e.toString(), "PIPE_FOP_001", context);
        else if (e instanceof TransformerException)
          dynamicError("Transform Error while piping FOP: " + e.toString(), "PIPE_FOP_002", context);
        else if (e instanceof FOPException)
          dynamicError("FOP Error while piping FOP: " + e.toString(), "PIPE_FOP_003", context);
        else if (e instanceof DocumentException)
          dynamicError("PDF Copy Error while piping FOP: " + e.toString(), "PIPE_FOP_004", context);
        else
          dynamicError("Error while piping FOP: " + e.toString(), "PIPE_FOP_005", context);
      }

      // All done.
      return null;
    }

    private void appendPdf(XPathContext context, byte[] origPdfData, OutputStream outStream)
        throws IOException, DocumentException, BadPdfFormatException, XPathException
    {
      // Read in the PDF that FOP generated.
      PdfReader pdfReader = new PdfReader(origPdfData);
      pdfReader.consolidateNamedDestinations();
      Document pdfDocument = new Document(pdfReader.getPageSizeWithRotation(1));
      PdfCopy pdfWriter = new PdfCopy(pdfDocument, outStream);
      pdfDocument.open();
      
      // Copy bookmarks from the FOP-generated PDF
      ArrayList allBookmarks = new ArrayList();
      List bookmarks1 = SimpleBookmark.getBookmark(pdfReader);
      if (bookmarks1 != null)
          allBookmarks.addAll(bookmarks1);
      
      // Copy pages from the FOP-generated PDF
      int nOrigPages = pdfReader.getNumberOfPages();
      for (int i = 1; i <= nOrigPages; i++)
        pdfWriter.addPage(pdfWriter.getImportedPage(pdfReader, i));
      
      // Build the full path to the PDF file we're going to append.
      String path = attribs.get("appendPDF").evaluateAsString(context);
      File file = FileUtils.resolveFile(context, path);
      
      // Read in the append PDF
      PdfReader pdfReader2 = new PdfReader(new BufferedInputStream(new FileInputStream(file)));
      pdfReader2.consolidateNamedDestinations();
      
      // Copy its bookmarks, shifting up their page targets.
      List bookmarks2 = SimpleBookmark.getBookmark(pdfReader2);
      if (bookmarks2 != null) {
        SimpleBookmark.shiftPageNumbers(bookmarks2, nOrigPages, null);
        allBookmarks.addAll(bookmarks2);
      }
      
      // And copy the append pages
      int nAppendPages = pdfReader2.getNumberOfPages();
      for (int i = 1; i <= nAppendPages; i++)
        pdfWriter.addPage(pdfWriter.getImportedPage(pdfReader2, i));
      
      // Set the combined bookmarks.
      if (!allBookmarks.isEmpty())
        pdfWriter.setOutlines(allBookmarks);
      
      // And we're done.
      pdfDocument.close();
    }
  }
}
