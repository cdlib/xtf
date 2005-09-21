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
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashMap;

import org.w3c.tidy.Configuration;
import org.w3c.tidy.Tidy;

import org.cdlib.xtf.util.*;

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

/** This class provides a single static {@link HTMLToString#convert(InputStream) convert() }
 *  method that converts an HTML file into an XML string that can be
 *  pre-filtered and added to a Lucene database by the 
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
   *  @param HTMLInputStream  Stream of HTML text to convert to an XML string.
   * 
   *  @return 
   *      If successful, a string containing the XML equivalent of the source
   *      HTML file. If an error occurred, this method returns <code>null</code>.
   * 
   */
  static public String convert( InputStream HTMLInputStream )
 
  {

    // Tell Tidy to supress warning and other output messsages.
    if( Trace.getOutputLevel() == Trace.debug ) {
        tidy.setErrout( new PrintWriter(new TraceWriter(Trace.debug)) );
        tidy.setQuiet( false );   
        tidy.setShowWarnings( true );
    }
    else {
        tidy.setQuiet( true );   
        tidy.setShowWarnings( false );
    }
  
    // Tell Tidy to make XML as it output.
    tidy.setXmlOut( true );
    
    // Output non-breaking spaces as "&nbsp;" so we can easily detect them
    // replace them with &#160; below to avoid problems parsing the XML.
    //
    tidy.setQuoteNbsp( true );
    
    // Default to UTF-8 character encoding.
    tidy.setCharEncoding( Configuration.UTF8 );
        
    try {
        
        // Create a buffer to output the XML to.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        // Convert the HTML to XML.
        tidy.parse( HTMLInputStream, out );
        
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
        // space. Likewise for other non-XML codes.
        // 
        retStr = replaceHtmlCodes( retStr );
        
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

  
  //////////////////////////////////////////////////////////////////////////////
  
  /** 
   * Table of conversions from HTML ampersand codes to UNICODE. We indicate
   * the few codes that don't need conversion at the start of the table.
   */
  static final String[] htmlCodes = {
      
      // XML-compatible codes.
      "lt", "lt",
      "gt", "gt",
      "amp", "amp",
      "apos", "apos",
      "quot", "quot",
      
      // HTML-only codes.
      "nbsp", "160",
      "iexcl", "161",
      "cent", "162",
      "cent", "162",
      "pound", "163",
      "curren", "164",
      "yen", "165",
      "brvbar", "166",
      "sect", "167",
      "uml", "168",
      "copy", "169",
      "ordf", "170",
      "laquo", "171",
      "not", "172",
      "shy", "173",
      "reg", "174",
      "macr", "175",
      "deg", "176",
      "plusmn", "177",
      "sup2", "178",
      "sup3", "179",
      "acute", "180",
      "micro", "181",
      "para", "182",
      "middot", "183",
      "cedil", "184",
      "sup1", "185",
      "ordm", "186",
      "raquo", "187",
      "frac14", "188",
      "frac12", "189",
      "frac34", "190",
      "iquest", "191",
      "Agrave", "192",
      "Aacute", "193",
      "Acirc", "194",
      "Atilde", "195",
      "Auml", "196",
      "Aring", "197",
      "AElig", "198",
      "Ccedil", "199",
      "Egrave", "200",
      "Eacute", "201",
      "Ecirc", "202",
      "Euml", "203",
      "Igrave", "204",
      "Iacute", "205",
      "Icirc", "206",
      "Iuml", "207",
      "ETH", "208",
      "Ntilde", "209",
      "Ograve", "210",
      "Oacute", "211",
      "Ocirc", "212",
      "Otilde", "213",
      "Ouml", "214",
      "times", "215",
      "Oslash", "216",
      "Ugrave", "217",
      "Uacute", "218",
      "Ucirc", "219",
      "Uuml", "220",
      "Yacute", "221",
      "THORN", "222",
      "szlig", "223",
      "szlig", "223",
      "agrave", "224",
      "aacute", "225",
      "acirc", "226",
      "atilde", "227",
      "auml", "228",
      "aring", "229",
      "aelig", "230",
      "ccedil", "231",
      "egrave", "232",
      "eacute", "233",
      "ecirc", "234",
      "euml", "235",
      "igrave", "236",
      "iacute", "237",
      "icirc", "238",
      "iuml", "239",
      "eth", "240",
      "ntilde", "241",
      "ograve", "242",
      "oacute", "243",
      "ocirc", "244",
      "otilde", "245",
      "ouml", "246",
      "divide", "247",
      "oslash", "248",
      "ugrave", "249",
      "uacute", "250",
      "ucirc", "251",
      "uuml", "252",
      "yacute", "253",
      "thorn", "254",
      "yuml", "255",
      "OElig", "338",
      "oelig", "339",
      "Scaron", "352",
      "scaron", "353",
      "Yuml", "376",
      "fnof", "402",
      "circ", "710",
      "tilde", "732",
      "Alpha", "913",
      "Beta", "914",
      "Gamma", "915",
      "Delta", "916",
      "Epsilon", "917",
      "Zeta", "918",
      "Eta", "919",
      "Theta", "920",
      "Iota", "921",
      "Kappa", "922",
      "Lambda", "923",
      "Mu", "924",
      "Nu", "925",
      "Xi", "926",
      "Omicron", "927",
      "Pi", "928",
      "Rho", "929",
      "Sigma", "931",
      "Tau", "932",
      "Upsilon", "933",
      "Phi", "934",
      "Chi", "935",
      "Psi", "936",
      "Omega", "937",
      "alpha", "945",
      "beta", "946",
      "gamma", "947",
      "delta", "948",
      "epsilon", "949",
      "zeta", "950",
      "eta", "951",
      "theta", "952",
      "iota", "953",
      "kappa", "954",
      "lambda", "955",
      "mu", "956",
      "nu", "957",
      "xi", "958",
      "omicron", "959",
      "pi", "960",
      "rho", "961",
      "sigmaf", "962",
      "sigma", "963",
      "tau", "964",
      "upsilon", "965",
      "phi", "966",
      "chi", "967",
      "psi", "968",
      "omega", "969",
      "thetasym", "977",
      "upsih", "978",
      "piv", "982",
      "ensp", "8194",
      "emsp", "8195",
      "thinsp", "8201",
      "zwnj", "8204",
      "zwj", "8205",
      "lrm", "8206",
      "rlm", "8207",
      "ndash", "8211",
      "mdash", "8212",
      "lsquo", "8216",
      "rsquo", "8217",
      "sbquo", "8218",
      "ldquo", "8220",
      "rdquo", "8221",
      "bdquo", "8222",
      "dagger", "8224",
      "Dagger", "8225",
      "bull", "8226",
      "hellip", "8230",
      "permil", "8240",
      "prime", "8242",
      "Prime", "8243",
      "lsaquo", "8249",
      "rsaquo", "8250",
      "oline", "8254",
      "frasl", "8260",
      "euro", "8364",
      "image", "8465",
      "weierp", "8472",
      "real", "8476",
      "trade", "8482",
      "alefsym", "8501",
      "larr", "8592",
      "uarr", "8593",
      "rarr", "8594",
      "darr", "8595",
      "harr", "8596",
      "crarr", "8629",
      "lArr", "8656",
      "uArr", "8657",
      "rArr", "8658",
      "dArr", "8659",
      "hArr", "8660",
      "forall", "8704",
      "part", "8706",
      "exist", "8707",
      "empty", "8709",
      "nabla", "8711",
      "isin", "8712",
      "notin", "8713",
      "ni", "8715",
      "prod", "8719",
      "sum", "8721",
      "minus", "8722",
      "lowast", "8727",
      "radic", "8730",
      "prop", "8733",
      "infin", "8734",
      "ang", "8736",
      "and", "8743",
      "or", "8744",
      "cap", "8745",
      "cup", "8746",
      "int", "8747",
      "there4", "8756",
      "sim", "8764",
      "cong", "8773",
      "asymp", "8776",
      "ne", "8800",
      "equiv", "8801",
      "le", "8804",
      "ge", "8805",
      "sub", "8834",
      "sup", "8835",
      "nsub", "8836",
      "sube", "8838",
      "supe", "8839",
      "oplus", "8853",
      "otimes", "8855",
      "perp", "8869",
      "sdot", "8901",
      "lceil", "8968",
      "rceil", "8969",
      "lfloor", "8970",
      "rfloor", "8971",
      "lang", "9001",
      "rang", "9002",
      "loz", "9674",
      "spades", "9824",
      "clubs", "9827",
      "hearts", "9829",
      "diams", "9830",
  };

  /** Build a HashMap from the code table above */
  private static HashMap htmlCodeMap = new HashMap();
  static {
    for( int i = 0; i < htmlCodes.length; i += 2 )
      htmlCodeMap.put( htmlCodes[i], htmlCodes[i+1] );
  }
 
  
  //////////////////////////////////////////////////////////////////////////////
  
  /** Convert any non-XML ampersand codes within a string to their unicode
   *  equivalents.
   * 
   *  @param in  The string within which to convert codes.
   */
  public static String replaceHtmlCodes( String in )
  {
    // Scan through the string, looking for ampersand codes.
    StringBuffer out = new StringBuffer( in.length() * 3 / 2 );
    char[] inChars = in.toCharArray();
    int i = 0;
    while( i < inChars.length ) 
    {
        // Look for an ampersand code (but not "&#xxx;")
        if( inChars[i] != '&' || 
            (i < inChars.length-1 && inChars[i+1] == '#') ) 
        {
            out.append( inChars[i++] );
            continue;
        }
        
        // Find the end of the code.
        int start = i + 1;
        int end = start;
        while( end < inChars.length && Character.isLetterOrDigit(inChars[end]) )
            end++;
        if( end == inChars.length || inChars[end] != ';' ) {
            out.append( inChars[i++] );
            continue;
        }
        
        // Look the code up in our map.
        String code  = in.substring( start, end );
        String mapTo = (String) htmlCodeMap.get( code );
        
        // If not found, delete the whole code.
        if( mapTo == null ) {
            i = end + 1;
            continue;
        }

        // If it's already XML-compatible, pass it through unchanged.
        if( code.equals(mapTo) ) {
            out.append( inChars[i++] );
            continue;
        }
        
        // Replace it with an XML-compatible code.
        out.append( "&#" );
        out.append( mapTo );
        out.append( ";" );
        i = end + 1;
    } // for i
    
    // Convert the result back to a string.
    return out.toString();
  } // replaceHtmlCodes()

} // class HTMLToString
