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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import javax.xml.transform.Templates;

import org.cdlib.xtf.util.StructuredStore;
import org.xml.sax.InputSource;

import sun.text.Normalizer;

/**
 * Transforms an HTML file to a single-record XML file.
 *
 * @author Martin Haye
 */
public class TextIndexSource extends XMLIndexSource 
{
  /** Constructor -- initializes all the fields */
  public TextIndexSource( File            textFile,
                          String          key,
                          Templates[]     preFilters,
                          Templates       displayStyle,
                          StructuredStore lazyStore )
  {
    super( null, textFile, key, preFilters, displayStyle, lazyStore );
    this.textFile = textFile;
  }
  
  /** Source of text data */
  private File textFile;

  /** Transform the text file to XML data */
  protected InputSource filterInput() throws IOException
  {
    // Map XML special characters in the text, and add a dummy
    // top-level element.
    //
    Reader reader = new BufferedReader(
            new InputStreamReader(new FileInputStream(textFile), "UTF-8"));
    char[] tmp = new char[1000];
    StringBuffer buf = new StringBuffer( 1000 );
    while( true ) {
        int nRead = reader.read( tmp );
        if( nRead <= 0 )
            break;
        buf.append( tmp, 0, nRead );
    }
    
    String str = normalize( buf.toString() );
    str = "<doc><text-data>" + str + "</text-data></doc>";
    
    // And make an InputSource with a proper system ID
    InputSource finalSrc = new InputSource( new StringReader(str) );
    finalSrc.setSystemId( textFile.toURL().toString() );
    return finalSrc;
  } // filterInput()
  
  /**
   * Prepare a string for inclusion in an XML document. Unicode strings are
   * normalized to their canonical equivalents, a few characters are 
   * escaped as entities, and invalid characters are removed.
   * 
   * @param s   string to normalize
   * @return    possibly changed version of the string
   */
  private String normalize( String s)
  {
    char[] ch = s.toCharArray();
      
    // Scan for suspicious characters that might need Unicode 
    // normalization.
    //
    boolean needNormalize = false;
    int     needEscape    = 0;
    for( int i = 0; i < ch.length; i++ ) {
        if( (ch[i] & ~0x7f) != 0 )
            needNormalize = true;
        
        if( ch[i] == '&' || ch[i] == '<' )
            ++needEscape;
        else if( ch[i] < '\u0020' &&
            (ch[i] != '\t' && ch[i] != '\n' && ch[i] != '\r') )
        {
            ++needEscape;
        }
        else if( ch[i] >= '\uD800' && ch[i] <= '\uDFFF' )
            ++needEscape;
        else if( ch[i] >= '\uFFFE' && ch[i] <= '\uFFFF' )
            ++needEscape;
    }
    
    if( needNormalize ) {
        String s2 = Normalizer.normalize( s, Normalizer.COMPOSE, 0 );
        if( !s.equals(s2) ) {
            s = s2;
            ch = s2.toCharArray();
        }
    }
    
    if( needEscape > 0 ) {
        int maxSpace = ch.length + (needEscape*5);
        char[] newCh = new char[maxSpace];
        int dp = 0;
        for( int sp = 0; sp < ch.length; sp++ ) {
            if( ch[sp] == '&' ) {
                newCh[dp++] = '&';
                newCh[dp++] = 'a';
                newCh[dp++] = 'm';
                newCh[dp++] = 'p';
                newCh[dp++] = ';';
            }
            else if( ch[sp] == '<' ) {
                newCh[dp++] = '&';
                newCh[dp++] = 'l';
                newCh[dp++] = 't';
                newCh[dp++] = ';';
            }
            else if( ch[sp] < '\u0020' &&
                (ch[sp] != '\t' && ch[sp] != '\n' && ch[sp] != '\r') )
            {
                ; // delete invalid character
            }
            else if( ch[sp] >= '\uD800' && ch[sp] <= '\uDFFF' )
                ; // delete invalid character
            else if( ch[sp] >= '\uFFFE' && ch[sp] <= '\uFFFF' )
                ; // delete invalid character
            else
                newCh[dp++] = ch[sp];
        }
        s = new String( newCh, 0, dp );
    }
    
    return s;
  } // normalize()

} // class TextSrcFile
