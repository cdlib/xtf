package org.cdlib.xtf.textIndexer;

/**
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
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import org.w3c.tidy.Tidy;

import org.cdlib.xtf.util.*;

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

/** This class provides a single static {@link HTMLToString#convert(string) convert() }
 *  method that converts an HTML file into an XML string that can be
 *  pre-filtered and added to a Lucene database by the 
 *  {@link XMLTextProcessor#parseText(SrcTextInfo) parseText() } method of the 
 *  {@link XMLTextProcessor } class. <br><br>
 * 
 *  Internally, the HTML to XML file conversion is performed by the jTidy
 *  library, which is a variant of the HTMLTidy converter. 
 */
public class HTMLToString {

  /** Create the HTMLTidy object that will do the work. */
  static Tidy tidy = new Tidy();  
 
  //////////////////////////////////////////////////////////////////////////////
  
  /** Convert an HTML file into an HTMLTidy style XML string.
   * 
   *  @param HTMLFileName  The name of the HTML file to convert to an XML string.
   * 
   *  @return 
   *      If successful, a string containing the XML equivalent of the source
   *      HTML file. If an error occurred, this method returns <code>null</code>.
   * 
   */
  static public String convert( String HTMLFileName )
 
  {

    // Tell Tidy to supress warning and other output messsages.
    tidy.setQuiet( true );   
    tidy.setShowWarnings( false );
  
    // Tell Tidy to make XML as it output.
    tidy.setXmlOut( true );
    
    // Output non-breaking spaces as "&nbsp;" so we can easily detect them
    // replace them with &#160; below to avoid problems parsing the XML.
    //
    tidy.setQuoteNbsp( true );
        
    try {
        
        // Get hold of the source file.
        FileInputStream in = new FileInputStream( HTMLFileName );
        
        // Create a buffer to output the XML to.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        // Convert the HTML to XML.
        tidy.parse( in, out );
        
        // Get a string version of the resulting XML.
        String retStr = out.toString();
        
        // Check to see if the XML has a document type tag.
        int docStartIdx = retStr.indexOf( "<!DOCTYPE" );
        
        // If it does...
        if( docStartIdx != -1 ) {

            // Find the end of the tag.
            int docEndIdx = retStr.indexOf( ">", docStartIdx );
            
            // Isolate the entire document type tag.
            String docTypeStr = retStr.substring( docStartIdx, 
                                                  docEndIdx-docStartIdx+1 );
            
            // And then remove the document tag from the XML. Why? Because
            // it often causes trouble when the XML parser tries to interpret
            // it. Especially coming from HTML.
            //
            retStr = retStr.replaceAll( docTypeStr, "" );
        }
        
        // Also, XML doesn't support the "&nbsp;" alias, so replace any 
        // occurences with the actual character code for a non-breaking
        // space.
        // 
        retStr = retStr.replaceAll( "&nbsp;", "&#160;" );
        
        // Finally, return the XML string to the caller.
        return retStr;
    
    } //try
    
    // If anything went wrong, say what it was.
    catch( Throwable t ) {
        Trace.error( "*** HTMLToXML.convert() Exception: " + t.getClass() );
        Trace.error( "                     With message: " + t.getMessage() );
    }

    // If we got to this point, something went wrong. So return a null
    // string back to the caller.
    //
    return null;
  
  } // static public String convert()

} // class HTMLToString
