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

import java.io.IOException;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;

/*
 * This file created on Apr 14, 2005 by Martin Haye
 */

/**
 * Performs special tokenization for facet fields. Looks for the hierarchy
 * marker "::" between hierarchy levels. For instance, the string
 * "US::California::Alameda County::Berkeley" would be made into four tokens:
 * 
 *    US
 *    US::California
 *    US::California::Alameda County
 *    US::California::Alameda County::Berkeley
 * 
 * @author Martin Haye
 */
public class FacetTokenizer extends TokenStream 
{
  String str;
  int    pos = 0;
  Token  nextToken = null;
  
  /**
   * Construct a token stream to remove accents from the input tokens.
   * 
   * @param str   The string to tokenize
   */
  public FacetTokenizer( String str )
  {
    this.str = str;
  } // constructor

  
  /** Retrieve the next token in the stream. */ 
  public Token next() throws IOException 
  {
    Token t;
    
    // Do we have a queued token? If so, return it.
    if( nextToken != null ) {
        t = nextToken;
        nextToken = null;
    }
    else {
            
        // Are we at the end? If so, tell the caller.
        if( pos > str.length() )
            return null;
        
        // Find the next divider. If not found, eat everything to the end.
        pos = str.indexOf( "::", pos );
        if( pos < 0 )
            pos = str.length();
        
        // Form the new token and advance.
        String term = str.substring( 0, pos );
        t = new Token( term, 0, pos );
        pos += 2;
        
        // If the lower-case version is different, queue that token as well.
        // That way queries will work properly.
        //
        String lcTerm = term.toLowerCase();
        if( !lcTerm.equals(term) )
            nextToken = new Token( lcTerm, 0, pos );
    }
    return t;
    
  } // next()

} // class FacetTokenizer
