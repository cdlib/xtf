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


public class PDFToString {
  
  static Logger logger = Logger.getRootLogger();
  static boolean mustConfigureLogger = true;
  
  static String convert( String PDFFilePath )

  {
    String xmlStr = null;
    
    if( mustConfigureLogger ) {
      BasicConfigurator.configure();
      logger.setLevel( Level.OFF );
      mustConfigureLogger = false;    
    }
    
    try {
        PDDocument pdfDoc = null;
        
        try {
            pdfDoc = PDDocument.load( PDFFilePath );
    
            if( pdfDoc.isEncrypted() ) {
                Trace.info( "*** PDF File is Encrypted. File Skipped." );
                throw new Exception();
            }               
                 
            PDFTextStripper stripper = new PDFTextStripper();
               
            xmlStr = FormatXML.procInstr( 
                         "xml version=\"1.0\" encoding=\"utf-8\"" );
               
            FormatXML.tabSize( 4 );
            FormatXML.blankLineAfterTag( false );
               
            int pageCount = pdfDoc.getPageCount();
               
            xmlStr += FormatXML.beginTag( 
                          "pdfDocument", 
                          FormatXML.attr( "file", PDFFilePath ) +
                          FormatXML.attr( "pageCount", pageCount ) );
               
            for( int i = 1; i <= pageCount; i++ ) {                     
               
                xmlStr += FormatXML.beginTag( "pdfPage",
                              FormatXML.attr( "number", i ) );
 
                stripper.setStartPage( i );
                stripper.setEndPage( i );

                String pdfText = stripper.getText( pdfDoc );
                pdfText = pdfText.replaceAll( "&", "&amp;" );
                pdfText = pdfText.replaceAll( "<", "&lt;"  );
                pdfText = pdfText.replaceAll( ">", "&gt;"  );
                   
                xmlStr += FormatXML.text( pdfText, 128 ) + 
                          FormatXML.newLineAfterText();
                   
                xmlStr += FormatXML.endTag();                   

            } // for( int i = 1; i <= pageCount; i++ )

            xmlStr += FormatXML.endAllTags();
                 
          }
              
          catch( Throwable t ) {
              Trace.error( "*** PDFToXML.convert() Exception: " + t.getClass() );
              Trace.error( "                    With message: " + t.getMessage() );
          }
    
          finally {
             if( pdfDoc != null ) pdfDoc.close();
          }
 
      } // try
     
      catch( Exception e ) {}
      
      return xmlStr;
  }
  
} // class PDFToString()
