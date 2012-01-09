package org.cdlib.xtf.saxonExt.pipe;

/*
 * Copyright (c) 2012, Regents of the University of California
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

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
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
import com.lowagie.text.Image;
import com.lowagie.text.pdf.BadPdfFormatException;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfCopy;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfObject;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfString;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.RandomAccessFileOrArray;
import com.lowagie.text.pdf.SimpleBookmark;
import com.lowagie.text.pdf.PdfCopy.PageStamp;

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
  
  private enum MergeAt { START, END };
  private enum MergeMode { SEQUENTIAL, OVERLAY, UNDERLAY };
  
  public void prepareAttributes() throws XPathException 
  {
    String[] mandatoryAtts = { };
    String[] optionalAtts = { "fileName", 
                              "author", "creator", "keywords", "producer", "title",
                              "overrideMetadata", // default: no
                              "appendPDF",        // backward compatibility only (no default)
                              "mergePDFFile",     // default: none
                              "mergeAt",          // "begin", *"end"
                              "mergeMode",        // *"sequential", "overlay", "underlay"
                              "fallbackIfError",  // default: yes
                              "fontDirs",         // default: none
                              "waitTime"          // default: 5 (seconds)
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
      String fileName = getAttribStr("fileName", context);
      if (fileName != null && fileName.length() != 0)
        servletResponse.setHeader("Content-disposition", "attachment; filename=\"" + fileName + "\"");
      
      // Get name of file to merge, if any.
      String nameToMerge = getAttribStr("mergePDFFile", context,
                       /*backward-compatibility:*/ getAttribStr("appendPDF", context, null));
      
      // Resolve it to a full path.
      File fileToMerge = null;
      if (nameToMerge != null) {
        fileToMerge = FileUtils.resolveFile(context, nameToMerge);
        if (!fileToMerge.canRead())
          dynamicError("Cannot read file '" + fileToMerge.toString() + "'", "PIPE_FOP_010", context);
      }
      
      // Merge mode (if any)
      MergeMode mergeMode = MergeMode.SEQUENTIAL;
      String tmp = getAttribStr("mergeMode", context, "sequential");
      if (tmp.equalsIgnoreCase("sequential"))
        mergeMode = MergeMode.SEQUENTIAL;
      else if (tmp.equalsIgnoreCase("overlay"))
        mergeMode = MergeMode.OVERLAY;
      else if (tmp.equalsIgnoreCase("underlay"))
        mergeMode = MergeMode.UNDERLAY;
      else
        dynamicError("Unrecognized mergeMode '" + tmp + "'", "PIPE_FOP_008", context);
        
      // Merge location (if any)
      MergeAt mergeAt = MergeAt.START;
      tmp = getAttribStr("mergeAt", context, "start");
      if (tmp.equalsIgnoreCase("start"))
        mergeAt = MergeAt.START;
      else if (tmp.equalsIgnoreCase("end"))
        mergeAt = MergeAt.END;
      else
        dynamicError("Unrecognized mergeAt '" + tmp + "'", "PIPE_FOP_009", context);
        
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
        // in a temp file. We don't use a memory buffer since sometimes these things
        // can be quite huge.
        //
        File tempFile = new File(FileUtils.createTempFile(context, "xtfFop.", ".tmp"));
        FileOutputStream fopOut = new FileOutputStream(tempFile);
        
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
          if (fopOut != null)
            fopOut.close();
        }
        
        // Now that we've released the FOP lock, check if we need to merge a PDF or not.
        
        File finalOut;
        if (fileToMerge != null) {
          File tempFile2 = new File(FileUtils.createTempFile(context, "xtfFopMerge.", ".tmp"));
          finalOut = tempFile2;
          OutputStream mergeOut = new BufferedOutputStream(new FileOutputStream(tempFile2));
          try {
            mergePdf(context, tempFile, fileToMerge, mergeMode, mergeAt, mergeOut);
          }
          finally {
            mergeOut.close();
          }
        }
        else 
          finalOut = tempFile;
        
        // Now we know the output length, so let the client know and then send it.
        servletResponse.setHeader("Content-length", Long.toString(finalOut.length()));
        PipeFileElement.copyFileToStream(finalOut, servletResponse.getOutputStream());
      } 
      catch (Throwable e) 
      {
        // If requested, fall back to simply piping the PDF file itself, without any FOP prefix.
        if (fileToMerge != null)
        {
          if (getAttribBool("fallbackIfError", context, true))
          {
            try {
              Trace.warning("Warning: pipeFop failed, falling back to just piping PDF file. Cause: " + e.toString());
              servletResponse.setHeader("Content-length", Long.toString(fileToMerge.length()));
              PipeFileElement.copyFileToStream(fileToMerge, servletResponse.getOutputStream());
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
    
    /** 
     * Do the work of joining the FOP output and a PDF together. This involves
     * several steps:
     * 
     *  1. Based on parameters specified in the PipeFOP command, determine how
     *     the pages will overlap.
     *  2. Merge bookmarks and metadata
     *  3. Output the pages  
     */
    private void mergePdf(XPathContext context, 
                           File origPdfData, 
                           File fileToAppend,
                           MergeMode mergeMode, 
                           MergeAt mergeAt, 
                           OutputStream outStream)
      throws IOException, DocumentException, BadPdfFormatException, XPathException
    {
      RandomAccessFileOrArray[] randFiles = new RandomAccessFileOrArray[2];
      PdfReader[] readers = new PdfReader[2];
      HashMap<String,String>[] infos = new HashMap[2];
      int[] nInPages = new int[2];
      int[] pageOffsets = new int[2];
      int nOutPages = 0;
      
      // For large PDFs, use a buffered random access file rather than the default
      // memory-mapped file that iText assumes.
      //
      randFiles[0] = (origPdfData.length() > 1024*1024) ? 
                     new BufferedRandomAccessFile(origPdfData.toString()) : 
                     new RandomAccessFileOrArray(origPdfData.toString());
      randFiles[1] = (fileToAppend.length() > 1024*1024) ? 
                     new BufferedRandomAccessFile(fileToAppend.toString()) : 
                     new RandomAccessFileOrArray(fileToAppend.toString());
                     
      // Read in the PDF that FOP generated and the one we're merging
      readers[0] = new PdfReader(randFiles[0], null);
      readers[1] = new PdfReader(randFiles[1], null);
      
      // Perform processing that's identical for both
      for (int i=0; i<2; i++) 
      {
        readers[i].consolidateNamedDestinations();
        infos[i] = readers[i].getInfo();
        nInPages[i] = readers[i].getNumberOfPages();
      }
      
      // Calculate page offsets depending on the merge mode.
      switch (mergeMode) 
      {
        case SEQUENTIAL:
          nOutPages = nInPages[0] + nInPages[1];
          switch (mergeAt) {
            case START:
              pageOffsets[0] = nInPages[1];
              pageOffsets[1] = 0;
              break;
            case END:
              pageOffsets[0] = 0;
              pageOffsets[1] = nInPages[0];
              break;
          }
          break;
          
        case OVERLAY:
        case UNDERLAY:
          nOutPages = Math.max(nInPages[0], nInPages[1]);
          pageOffsets[0] = 0;
          if (mergeAt == MergeAt.END)
            pageOffsets[1] = Math.max(0, nInPages[0] - nInPages[1]);
          else
            pageOffsets[1] = 0;
          break;
      }
      
      // Construct the copying writer
      Document pdfDocument = new Document(readers[0].getPageSizeWithRotation(1));
      PdfCopy pdfWriter = new PdfCopy(pdfDocument, outStream);
      pdfDocument.open();
      
      // Merge the metadata
      mergeMetadata(infos, pdfWriter, context);
      
      // Copy bookmarks from both PDFs
      ArrayList allBookmarks = new ArrayList();
      for (int i=0; i<2; i++) {
        List bookmarks = SimpleBookmark.getBookmark(readers[i]);
        if (bookmarks != null) {
          if (pageOffsets[i] != 0)
            SimpleBookmark.shiftPageNumbers(bookmarks, pageOffsets[i], null);
          allBookmarks.addAll(bookmarks);
        }
      }
      
      PageInfo[] basePages = new PageInfo[nOutPages];
      PageInfo[] mergePages = new PageInfo[nOutPages];
      
      // Gather all the info we'll need to merge the pages. For some reason,
      // iText needs us to make all the template images before using any
      // of them.
      //
      for (int i = 0; i < nOutPages; i++)
      {
        for (int j=0; j<2; j++)
        {
          int inPageNum = i - pageOffsets[j];
          if (inPageNum < 0 || inPageNum >= nInPages[j])
            continue;
          
          PageInfo info = new PageInfo();
          info.reader = readers[j];
          info.pageNum = inPageNum+1;
          
          if (basePages[i] == null)
            basePages[i] = info;
          else {
            info.impPage = pdfWriter.getImportedPage(info.reader, info.pageNum);
            info.image = Image.getInstance(info.impPage);
            mergePages[i] = info;
          }
        }
      }

      for (int i = 0; i < nOutPages; i++)
      {
        PageInfo basePage = basePages[i];
        PageInfo mergePage = mergePages[i];
        boolean over = mergeMode == MergeMode.OVERLAY;

        basePage.impPage = pdfWriter.getImportedPage(basePage.reader, basePage.pageNum);
        
        if (mergePage != null)
        {
          PageStamp ps = pdfWriter.createPageStamp(basePage.impPage);
          PdfContentByte contentBuf = null;
          if (over)
            contentBuf = ps.getOverContent();
          else
            contentBuf = ps.getUnderContent();
          
          Image img = Image.getInstance(mergePage.image); // this is the trick
          
          // When adding the image, we need to construct a matrix that will properly orient it.
          int rotation = mergePage.reader.getPageRotation(mergePage.pageNum);
          float w = basePage.impPage.getWidth();
          float h = basePage.impPage.getHeight();
          switch (rotation)
          {
            case 0:
              contentBuf.addImage(img, w, 0, 0, h, 0, 0);
              break;
            case 90:
              contentBuf.addImage(img, 0, -h, w, 0, 0, h);
              break;
            case 180:
              contentBuf.addImage(img, -w, 0, 0, -h, w, h);
              break;
            case 270:
              contentBuf.addImage(img, 0, h, -w, 0, w, 0);
              break;
          }
          ps.alterContents();
        }
        
        pdfWriter.addPage(basePage.impPage);
      }
            
      // Set the combined bookmarks.
      if (!allBookmarks.isEmpty())
        pdfWriter.setOutlines(allBookmarks);
      
      // And we're done.
      pdfDocument.close();
    }

    /**
     * Merge metadata from the FOP-generated PDF and a PDF we're merging into it.
     * Generally metadata in the merge file takes precedence over the FOP metadata,
     * but the "overrideMetadata" option reverses that behavior.
     */
    private void mergeMetadata(HashMap<String, String>[] infos, PdfWriter pdfWriter, XPathContext context) 
      throws XPathException 
    {
      boolean override = getAttribBool("overrideMetadata", context, false);
      HashMap<String, String> toPut = new HashMap();
      if (override) {
        toPut.putAll(infos[1]);
        toPut.putAll(infos[0]);
      }
      else {
        toPut.putAll(infos[0]);
        toPut.putAll(infos[1]);
      }
      
      PdfDictionary outInfo = pdfWriter.getInfo();
      for (String key : toPut.keySet())
      {
        // Keep iText as the producer
        if (key.equals("Producer"))
          continue;
        
        // Filter out empty values.
        String val = toPut.get(key).trim();
        if (val.length() == 0)
          continue;
        
        // Add the new metadata
        outInfo.put(new PdfName(key), new PdfString(val, PdfObject.TEXT_UNICODE));
      }
    }
    
    private static class PageInfo
    {
      PdfReader reader;
      int pageNum;
      PdfImportedPage impPage;
      Image image;
    } 
  }  
}
