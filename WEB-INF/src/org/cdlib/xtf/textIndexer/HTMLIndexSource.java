package org.cdlib.xtf.textIndexer;

/*
 * Copyright (c) 2005, Regents of the University of California
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
import java.io.StringReader;

import javax.xml.transform.Templates;

import org.cdlib.xtf.util.StructuredStore;
import org.xml.sax.InputSource;

/**
 * Transforms an HTML file to a single-record XML file.
 *
 * @author Martin Haye
 */
public class HTMLIndexSource extends XMLIndexSource 
{
  /** Constructor -- initializes all the fields */
  public HTMLIndexSource( File            htmlFile,
                          String          key,
                          Templates[]     preFilters,
                          Templates       displayStyle,
                          StructuredStore lazyStore )
  {
    super( null, htmlFile, key, preFilters, displayStyle, lazyStore );
    this.htmlFile = htmlFile;
  }
  
  /** Source of HTML data */
  private File htmlFile;

  /** Transform the HTML file to XML data */
  protected InputSource filterInput() throws IOException
  {
    // Convert the HTML file into an XML string that we can index.
    InputStream inStream = new FileInputStream( htmlFile );
    String htmlXMLStr = HTMLToString.convert( inStream );
    
    // And make an InputSource with a proper system ID
    InputSource finalSrc = new InputSource( new StringReader(htmlXMLStr) );
    finalSrc.setSystemId( htmlFile.toURL().toString() );
    return finalSrc;
  } // filterInput()

} // class HTMLSrcFile
