package org.cdlib.xtf.textIndexer;

/*
 * Copyright (c) 2004, Regents of the University of California
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.transform.Templates;

import org.cdlib.xtf.textEngine.IndexUtil;
import org.cdlib.xtf.util.Path;
import org.cdlib.xtf.util.StructuredStore;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Supplies a single file containing a single record to the 
 * {@link XMLTextProcessor}. Classes must supply the {@link #xmlSource()}
 * method depending on the kind of input data.
 *
 * @author Martin Haye
 */
public class XMLIndexSource extends IndexSource 
{
  /** Simple constructor */
  public XMLIndexSource( InputSource inSrc, String key )
  {
    String sysId = inSrc.getSystemId();

    this.inSrc        = inSrc;
    this.path         = (sysId == null) ? null : new File(sysId);
    this.key          = key;
    this.preFilters   = null;
    this.displayStyle = null;
    this.lazyStore    = null;
  }
  
  /** Constructor -- initializes all the fields */
  public XMLIndexSource( InputSource     inSrc,
                         File            path,
                         String          key,
                         Templates[]     preFilters,
                         Templates       displayStyle,
                         StructuredStore lazyStore )
  {
    this.inSrc        = inSrc;
    this.path         = path;
    this.key          = key;
    this.preFilters   = preFilters;
    this.displayStyle = displayStyle;
    this.lazyStore    = lazyStore;
  }
  
  public void removeDoctypeDecl( boolean flag )
  {
    this.removeDoctypeDecl = flag;
  }

  /** Source of XML data */
  private InputSource inSrc;
  
  /** Path to the file, or null if it's not a local file. */
  private File path;
  
  /** Key used to identify this file in the index */
  private String key;
  
  /** XSLT pre-filters used to massage the XML document (null for none) */
  private Templates[] preFilters;

  /** Stylesheet from which to gather XSLT key definitions to be computed
   *  and cached on disk. Typically, one would use the actual display 
   *  stylesheet for this purpose, guaranteeing that all of its keys will be 
   *  pre-cached.<br><br>
   * 
   *  Background: stylesheet processing can be optimized by using XSLT 'keys', 
   *  which are declared with an &lt;xsl:key&gt; tag. The first time a key 
   *  is used in a given source document, it must be calculated and its values 
   *  stored on disk. The text indexer can optionally pre-compute the keys so 
   *  they need not be calculated later during the display process.
   */
  private Templates displayStyle;
  
  /** 
   * Empty storage in which to build the persistent version of the
   * document (aka the "lazy tree"), or null to avoid building it.
   */
  private StructuredStore lazyStore;
  
  /** Whether to remove DOCTYPE decl (this is kind of a kludge) */
  private boolean removeDoctypeDecl = false;
  
  /** Keep track of whether we've processed this file yet */
  private boolean isDone = false;
  
  /** A parser we can use to tell whether we need to apply crimson workaround */
  private static SAXParser saxParser = IndexUtil.createSAXParser();
  
  // inherit JavaDoc
  public File path() { return path; }
  
  // inherit JavaDoc
  public String key() { return key; }
  
  // inherit JavaDoc
  public Templates[] preFilters() { return preFilters; }
  
  // inherit JavaDoc
  public Templates displayStyle() { return displayStyle; }
  
  // inherit JavaDoc
  public long totalSize() {
    if( path == null )
        return 1;
    return path.length();
  }
  
  // inherit JavaDoc
  public IndexRecord nextRecord() throws SAXException, IOException
  {
    // Don't process the record twice.
    if( isDone )
        return null;
    
    // Okay, construct a SrcRecord for the whole file.
    try {
        return new IndexRecord() {
          public InputSource xmlSource() throws IOException { 
              return filterInput(); 
          }
          public int recordNum()   { return 0; }
          public int percentDone() { return 100; }
          public StructuredStore lazyStore() { 
              return lazyStore; 
          }
        };
    }
    finally {
        isDone = true;
    }
    
  } // nextRecord()
  
  /** 
   * Filter the input, if necessary, to remove DOCTYPE declarations, or
   * work around a bug in the Crimson parser.
   */
  protected InputSource filterInput() throws IOException
  {
    // If the input source is a reader, don't filter it.
    if( inSrc.getCharacterStream() != null )
        return inSrc;
    
    // If no kludgy steps to perform, skip this step.
    boolean applyCrimsonWorkaround = 
        saxParser.getClass().getName().equals("org.apache.crimson.jaxp.SAXParserImpl");
    if( !applyCrimsonWorkaround && !removeDoctypeDecl )
        return inSrc;
    
    // Convert the input source to an input stream if it isn't one already.
    InputStream inStream;
    if( inSrc.getByteStream() != null )
        inStream = inSrc.getByteStream();
    else if( inSrc.getSystemId() != null &&
             inSrc.getSystemId().length() > 0 )
    {
        // Make sure we can read the file.
        String path = Path.normalizeFileName( inSrc.getSystemId() );
        if( path.startsWith("file:/") )
            path = path.substring( 6 );
        if( !(new File(path).canRead()) )
            throw new FileNotFoundException( inSrc.getSystemId() );
        inStream = new FileInputStream( path );
    }
    else
        throw new IOException( "Must pass a Reader, InputStream or system ID to index" );
    
    // Apply kludgy filters if necessary.
    inStream = IndexUtil.filterXMLDocument( inStream,
        applyCrimsonWorkaround, removeDoctypeDecl );
    
    // Finally, make a new InputSource from the filtered stream.
    InputSource finalSrc = new InputSource( inStream );
    if( inSrc.getSystemId() != null )
        finalSrc.setSystemId( inSrc.getSystemId() );
    
    return finalSrc;
    
  } // filterInput()
  
  
} // class SimpleSrcTextInfo
