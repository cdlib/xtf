/**
 * 
 */
package org.cdlib.xtf.xslt;

/*
 * Copyright (c) 2007, Regents of the University of California
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

import java.io.IOException;
import java.util.Stack;

import org.cdlib.xtf.servletBase.DTDSuppressingXMLReader;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.ext.LexicalHandler;

/**
 * Like a normal XMLReader, except that it stops processing when the first
 * end-element tag is encountered. This reads in the first part of an XML file,
 * which is like a "stub" version of the file. Also, we jump through very
 * special hoops to make the DTD declaration available (it's not normally
 * part of Saxon's data model.)
 * 
 * This file created November 1, 2007 by Martin Haye
 */
class XMLStubReader extends DTDSuppressingXMLReader
{
  /** Thrown after the first element end marker is found */
  private class GetOut extends RuntimeException { }
  
  /** 
   * Establish the content handler that will receive events. We wrap it
   * to perform special processing.
   */
  public void setContentHandler(ContentHandler handler) {
    StubContentHandler stubHandler = new StubContentHandler(handler);
    super.setContentHandler(stubHandler);
  }
  
  /**
   * Catch requests to set the "lexical handler". We insert a stub handler
   * in the chain so we can catch the DTD declaration.
   */
  public void setProperty(String name, Object value)
    throws SAXNotRecognizedException, SAXNotSupportedException 
  {
    if (name.equals("http://xml.org/sax/properties/lexical-handler")) {
      StubLexicalHandler stubHandler = new StubLexicalHandler((LexicalHandler)value);
      super.setProperty(name, stubHandler);
    }
    else      
      super.setProperty(name, value);
  }
  
  /** Parse the input document, but stop at the first end-element marker */
  public void parse (InputSource input)
    throws IOException, SAXException
  {
    try {
      super.parse(input);
    }
    catch (GetOut g) {
      // This is normal: do nothing.
    }
  }
  
  /**
   * Handles 'lexical' events. We mainly need to catch the start of the DTD
   * and turn that even into an unparsed entity declaration so that Saxon will
   * keep a record of it.
   */
  private class StubLexicalHandler implements LexicalHandler
  {
    private LexicalHandler out;
    
    public StubLexicalHandler(LexicalHandler out) {
      this.out = out;
    }

    ////////////////// Special delegate methods /////////////////
    
    public void startDTD(String name, String publicId, String systemId)
    throws SAXException 
    {
      out.startDTD(name, publicId, systemId);
      if (out instanceof DTDHandler && publicId != null && systemId != null)
        ((DTDHandler)out).unparsedEntityDecl(name, publicId, systemId, null);
    }
  
    /////////////// Pass-through delegate methods /////////////////
    
    public void endDTD() throws SAXException {
      out.endDTD();
    }
  
    public void startEntity(String name) throws SAXException {
      out.startEntity(name);
    }
    
    public void endEntity(String name) throws SAXException {
      out.endEntity(name);
    }
    
    public void comment(char[] ch, int start, int length) throws SAXException {
      out.comment(ch, start, length);
    }

    public void startCDATA() throws SAXException {
      out.startCDATA();
    }

    public void endCDATA() throws SAXException {
      out.endCDATA();
    }
  }
  
  /**
   * Handles content events from the XML parser. 
   */
  private class StubContentHandler implements ContentHandler 
  {
    private ContentHandler out;
    private Stack<String[]> eventStack = new Stack<String[]>();
    
    /** Construct the content handler, passing events to 'out' */
    public StubContentHandler(ContentHandler out) {
      this.out = out;
    }

    ///////////// Special delegate methods /////////////
    
    public void startElement(String uri, String localName, String name,
        Attributes atts) throws SAXException 
    {
      // Keep track of which elements have been started, so we can auto-close
      // them later.
      //
      eventStack.push(new String[] { "element", uri, localName, name });
      out.startElement(uri, localName, name, atts);
    }


    public void endElement(String uri, String localName, String name)
      throws SAXException 
    {
      // We've reached the first end-element. Let's simulate the situation that
      // the document just ends here. So we unwind the event stack, closing all 
      // elements and prefix mappings.
      //
      while (!eventStack.isEmpty()) {
        String[] event = eventStack.pop();
        if (event[0].equals("element"))
          out.endElement(event[1], event[2], event[3]);
        else if (event[0].equals("prefix"))
          out.endPrefixMapping(event[1]);
        else
          assert false;
      }
      
      // All done. End the document, and abort the rest of the parse().
      out.endDocument();
      throw new GetOut();
    }

    public void startPrefixMapping(String prefix, String uri)
      throws SAXException 
    {
      // Keep track of open prefix mappings so we can auto-close them later.
      eventStack.push(new String[] { "prefix", prefix });
      out.startPrefixMapping(prefix, uri);
    }
    
    public void endPrefixMapping(String prefix) throws SAXException {
      out.endPrefixMapping(prefix);
      String[] event = eventStack.pop();
      assert event[0].equals("prefix");
      assert event[1].equals(prefix);
    }
  
    /////////// Pass-through delegate methods ///////////
    
    public void startDocument() throws SAXException {
      out.startDocument();
    }

    public void endDocument() throws SAXException {
      out.endDocument();
    }

    public void characters(char[] ch, int start, int length)
      throws SAXException 
    {
      out.characters(ch, start, length);
    }

    public void ignorableWhitespace(char[] ch, int start, int length)
      throws SAXException 
    {
      out.ignorableWhitespace(ch, start, length);
    }

    public void processingInstruction(String target, String data)
      throws SAXException 
    {
      out.processingInstruction(target, data);
    }

    public void setDocumentLocator(Locator locator) {
      out.setDocumentLocator(locator);
    }

    public void skippedEntity(String name) throws SAXException {
      out.skippedEntity(name);
    }

  }
}