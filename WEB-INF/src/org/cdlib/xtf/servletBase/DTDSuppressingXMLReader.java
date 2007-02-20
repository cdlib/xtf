package org.cdlib.xtf.servletBase;


/*
 * Copyright (c) 2006, Regents of the University of California
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
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

/**
 * Simple wrapper for an XML reader that requests it to avoid loading external
 * DTDs. This not only speeds things up, it also allows our service to work
 * even if the external service is unavailable.
 *
 * @author Martin Haye
 */
public class DTDSuppressingXMLReader implements XMLReader 
{
  /** The wrapped XML reader to which all methods are delegated */
  private XMLReader reader;

  /**
   * Construct the XML reader and set a flag on it to avoid loading
   * external DTDs
   */
  public DTDSuppressingXMLReader() 
  {
    // First, create the reader.
    try {
      reader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
    }
    catch (ParserConfigurationException err) {
      throw new TransformerFactoryConfigurationError(err);
    }
    catch (SAXException err) {
      throw new TransformerFactoryConfigurationError(err);
    }

    // Then ask it to not load external DTDs (unless validating.)
    try 
    {
      reader.setFeature(
        "http://apache.org/xml/features/nonvalidating/load-external-dtd",
        false);
    }
    catch (SAXNotSupportedException err) {
      // ignore
    }
    catch (SAXNotRecognizedException err) {
      // ignore
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  // Delegated methods
  ////////////////////////////////////////////////////////////////////////////
  public boolean equals(Object obj) {
    return reader.equals(obj);
  }

  public ContentHandler getContentHandler() {
    return reader.getContentHandler();
  }

  public DTDHandler getDTDHandler() {
    return reader.getDTDHandler();
  }

  public EntityResolver getEntityResolver() {
    return reader.getEntityResolver();
  }

  public ErrorHandler getErrorHandler() {
    return reader.getErrorHandler();
  }

  public boolean getFeature(String name)
    throws SAXNotRecognizedException, SAXNotSupportedException 
  {
    return reader.getFeature(name);
  }

  public Object getProperty(String name)
    throws SAXNotRecognizedException, SAXNotSupportedException 
  {
    return reader.getProperty(name);
  }

  public int hashCode() {
    return reader.hashCode();
  }

  public void parse(String systemId)
    throws IOException, SAXException 
  {
    reader.parse(systemId);
  }

  public void parse(InputSource input)
    throws IOException, SAXException 
  {
    reader.parse(input);
  }

  public void setContentHandler(ContentHandler handler) {
    reader.setContentHandler(handler);
  }

  public void setDTDHandler(DTDHandler handler) {
    reader.setDTDHandler(handler);
  }

  public void setEntityResolver(EntityResolver resolver) {
    reader.setEntityResolver(resolver);
  }

  public void setErrorHandler(ErrorHandler handler) {
    reader.setErrorHandler(handler);
  }

  public void setFeature(String name, boolean value)
    throws SAXNotRecognizedException, SAXNotSupportedException 
  {
    reader.setFeature(name, value);
  }

  public void setProperty(String name, Object value)
    throws SAXNotRecognizedException, SAXNotSupportedException 
  {
    reader.setProperty(name, value);
  }

  public String toString() {
    return reader.toString();
  }
} // class DTDSuppressingXMLReader
