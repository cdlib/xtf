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

import org.pdfbox.pdmodel.PDDocument;
import org.pdfbox.util.PDFTextStripper;

import org.cdlib.xtf.util.*;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.apache.log4j.BasicConfigurator;

//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////

public class PDFToString {
  
  static boolean mustConfigureLogger = true;
  
  /** Reference to the Log4j root logger. Used to squelch PDFBox 
   *  output messages.
   */
  static Logger logger = Logger.getRootLogger();
  
  /** PDFBox text stripper. Created once to save time. */
  static PDFTextStripper stripper = new PDFTextStripper();
  

  //////////////////////////////////////////////////////////////////////////////

  static String convert( String PDFFilePath )

  {
    String xmlStr = null;
    
    // If the Log4j logger needs to be configured, do so now.
    if( mustConfigureLogger ) {
      
        // Configure the logger (whatever that means.)
        BasicConfigurator.configure();
        
        // Turn off the logging so it doesn't interfere with
        // our output logging.
        //
        logger.setLevel( Level.OFF );
        
        // Flag that the logger is configured.
        mustConfigureLogger = false;    
    }
    
    try {
        PDDocument pdfDoc = null;
        
        try {
          
            // Get hold of the PDF document to convert.
            pdfDoc = PDDocument.load( PDFFilePath );
    
            // If the document is encrypted, we've got a problem.
            if( pdfDoc.isEncrypted() ) {
                Trace.info( "*** PDF File is Encrypted. File Skipped." );
                throw new Exception();
            }               
                    
            // Start the XML with an XML format tag.   
            xmlStr = FormatXML.procInstr( 
                         "xml version=\"1.0\" encoding=\"utf-8\"" );
            
            // Set up the tab size and blank line formatting.   
            FormatXML.tabSize( 4 );
            FormatXML.blankLineAfterTag( false );
            
            // Determine how many pages there are in the PDF file.   
            int pageCount = pdfDoc.getPageCount();
            
            // Create an all-enclosing document tag summarizing 
            // the original document name and the number of pages.
            //   
            xmlStr += FormatXML.beginTag( 
                          "pdfDocument", 
                          FormatXML.attr( "file", PDFFilePath ) +
                          FormatXML.attr( "pageCount", pageCount ) );
            
            // Process each page in the PDF document.   
            for( int i = 1; i <= pageCount; i++ ) {                     
               
                // Start with a new page tag.
                xmlStr += FormatXML.beginTag( "pdfPage",
                              FormatXML.attr( "number", i ) );
 
                // Tell the stripper to only process the current page.
                stripper.setStartPage( i );
                stripper.setEndPage( i );

                // Get the text for this page.
                String pdfText = stripper.getText( pdfDoc );
                
                // Replace characters in the PDF text with XML aliases
                // to prevent problems when parsing the XML.
                //
                pdfText = pdfText.replaceAll( "&", "&amp;" );
                pdfText = pdfText.replaceAll( "<", "&lt;"  );
                pdfText = pdfText.replaceAll( ">", "&gt;"  );
                
                // Tack the text onto the XML output, nicely formatted
                // into lines of 128 characters or less.
                //   
                xmlStr += FormatXML.text( pdfText, 128 ) + 
                          FormatXML.newLineAfterText();
                
                // End the current page tag.   
                xmlStr += FormatXML.endTag();                   

            } // for( int i = 1; i <= pageCount; i++ )

            // End any remaining open tags (should only be the pdfDocument
            // tag.)
            //
            xmlStr += FormatXML.endAllTags();
                 
          } // try
          
          // If anything went wrong, say what it was.    
          catch( Throwable t ) {
              Trace.error( "*** PDFToXML.convert() Exception: " + t.getClass() );
              Trace.error( "                    With message: " + t.getMessage() );
          }
    
          // Finally, close up the the PDF document.
          finally {
             if( pdfDoc != null ) pdfDoc.close();
          }
 
      } // try
     
      // Shunt out any other exceptions.
      catch( Exception e ) {}
      
      // Return the resulting XML string to the caller.
      return xmlStr;
      
  } // public convert()
  
} // class PDFToString()
