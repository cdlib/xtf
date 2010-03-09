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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
import org.xml.sax.SAXException;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;

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
  private static HashMap<String, FopFactory> fopFactories = new HashMap();
  private static Lock fopLock = new ReentrantLock();
  
  public void prepareAttributes() throws XPathException 
  {
    String[] mandatoryAtts = { };
    String[] optionalAtts = { "fileName", 
                              "author", "creator", "keywords", "producer", "title",
                              "appendPDF", 
                              "fallbackIfError", // default: yes
                              "fontDirs",        // default: none
                              "waitTime"         // default: 5 (seconds)
                            };
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
      
      // If output file name specified, add the Content-disposition header.
      String fileName;
      if (attribs.containsKey("fileName")) {
        fileName = attribs.get("fileName").evaluateAsString(context);
        servletResponse.setHeader("Content-disposition", "attachment; filename=\"" + fileName + "\"");
      }
      
      // Build the full path to the PDF file we're going to append, if any.
      File fileToAppend = null;
      if (attribs.containsKey("appendPDF")) {
        String path = attribs.get("appendPDF").evaluateAsString(context);
        fileToAppend = FileUtils.resolveFile(context, path);
      }

      try {

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

        // So that we can keep the lock on FOP short, and also so we can send an
        // accurate Content-length header to the client, we'll accumulate the FOP output
        // in a byte buffer. We use the Apache ByteArrayOutputStream class since it
        // doesn't constantly realloc-copy when the buffer needs to grow.
        //
        ByteArrayOutputStream fopOut = new ByteArrayOutputStream();
        
        // According to the Apache docs, FOP may not be thread-safe. So, we need to
        // single-thread it. However, we must at all costs keep requests from backing
        // up behind each other in a scenario where many clients are making requests
        // all at once. So put a time limit on it.
        //
        int lockTime;
        if (attribs.containsKey("waitTime"))
          lockTime = Integer.parseInt(attribs.get("waitTime").evaluateAsString(context));
        else
          lockTime = 5; // default to waiting 5 seconds
        boolean gotLock = false;
        try {
          if (lockTime <= 0) {
            gotLock = true;
            fopLock.lock();
          }
          else
            gotLock = fopLock.tryLock(lockTime, TimeUnit.SECONDS);
        
          // Failure to get the lock is an error. However, this exception will
          // be caught below and, if requested, we'll fall back to just outputting
          // the append PDF.
          //
          if (!gotLock)
            throw new TimeoutException("Timed out trying to obtain FOP lock");
          
          // For speed, only create FOP factory if we haven't already got one.
          FopFactory fopFactory = createFopFactory(context);
          
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

          // Now run FOP
          Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foAgent, fopOut);
          transformer.transform(src, new SAXResult(fop.getDefaultHandler()));
        }
        finally 
        {
          // Always release the FOP lock when we're done, regardless of what happened.
          if (gotLock)
            fopLock.unlock();
        }
        
        // Now that we've released the FOP lock, check if we need to append a PDF or not.
        ByteArrayOutputStream finalOut;
        if (fileToAppend != null) { 
          finalOut = new ByteArrayOutputStream(fopOut.size() + (int)fileToAppend.length());
          appendPdf(context, fopOut.toByteArray(), fileToAppend, finalOut);
        }
        else 
          finalOut = fopOut;
        
        // Now we know the output length, so let the client know and then send it.
        servletResponse.setHeader("Content-length", Integer.toString(finalOut.size()));
        servletResponse.getOutputStream().write(finalOut.toByteArray());
      } 
      catch (Throwable e) 
      {
        // If requested, fall back to simply piping the PDF file itself, without any FOP prefix.
        if (attribs.containsKey("appendPDF"))
        {
          String fallbackStr = "yes"; // default to yes if not specified
          if (attribs.containsKey("fallbackIfError"))
            fallbackStr = attribs.get("fallbackIfError").evaluateAsString(context);
          if (fallbackStr.matches("yes|Yes|true|True|1")) 
          {
            try {
              Trace.warning("Warning: pipeFop failed, falling back to just piping PDF file. Cause: " + e.toString());
              servletResponse.setHeader("Content-length", Long.toString(fileToAppend.length()));
              PipeFileElement.copyFileToStream(fileToAppend, servletResponse.getOutputStream());
              e = null;
            }
            catch (IOException e2) {
              e = e2;
            }
          }
        }
        
        // Process any resulting exception into a Saxon dynamic error.
        if (e != null) {
          String code;
          if (e instanceof IOException)
            code = "PIPE_FOP_001";
          else if (e instanceof TransformerException)
            code = "PIPE_FOP_002";
          else if (e instanceof FOPException)
            code = "PIPE_FOP_003";
          else if (e instanceof DocumentException)
            code = "PIPE_FOP_004";
          else if (e instanceof TimeoutException)
            code = "PIPE_FOP_005";
          else
            code = "PIPE_FOP_006";
          dynamicError(e, "Error while piping FOP: " + e.toString(), code, context);
        }
      }

      // All done.
      return null;
    }

    /** Create a FOP factory and configure it, if we don't already have one. */ 
    private FopFactory createFopFactory(XPathContext context) 
      throws ConfigurationException, SAXException, IOException, XPathException 
    {
      // See if any font directories were specified.
      String fontDirs = "";
      if (attribs.containsKey("fontDirs"))
        fontDirs = attribs.get("fontDirs").evaluateAsString(context);
      
      // If we've already created a factory with this set of font directories,
      // don't re-create (it's expensive.)
      //
      if (fopFactories.containsKey(fontDirs))
        return fopFactories.get(fontDirs);
      
      // Gotta make a new one.
      FopFactory factory = FopFactory.newInstance();
      if (fontDirs.length() > 0) 
      {
        // The only way I've figured out to put font search directories into the 
        // factory is to feed in an XML config file. So construct one.
        //
        StringBuilder buf = new StringBuilder();
        buf.append("<?xml version=\"1.0\"?>" +
                   "<fop version=\"1.0\">" +
                   "  <renderers>" +
                   "    <renderer mime=\"application/pdf\">" +
                   "      <fonts>");
        
        for (String dir : fontDirs.split(";"))
          buf.append("        <directory>" + dir + "</directory>");
        
        buf.append("      </fonts>" +
                   "    </renderer>" +
                   "  </renderers>" +
                   "</fop>");

        // Jump through hoops to make the XML into an InputStream
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(bos);
        osw.write(buf.toString());
        osw.flush();
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        
        // Build the configuration and stick it into the factory.
        DefaultConfigurationBuilder cfgBuilder = new DefaultConfigurationBuilder();
        Configuration config = cfgBuilder.build(bis);
        factory.setUserConfig(config);
      }
      
      // Cache this factory so we don't have to create it again (they're expensive.)
      fopFactories.put(fontDirs, factory);
      return factory;
    }

    /** Do the work of joining the FOP output and a PDF together. **/
    private void appendPdf(XPathContext context, 
                           byte[] origPdfData, 
                           File fileToAppend,
                           OutputStream outStream)
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
      
      // Read in the append PDF
      PdfReader pdfReader2 = new PdfReader(new BufferedInputStream(new FileInputStream(fileToAppend)));
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
