package org.cdlib.xtf.util;

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

import java.io.IOException;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardTokenizerConstants;

/**
 * Used for debugging optimized FastTokenStream, this class checks the main
 * TokenStream against a reference one for equality. Any difference is flagged
 * with an assertion failure.
 * 
 * @author Martin Haye
 */
public class CheckingTokenStream extends TokenStream
{
    /** Main token stream that is being checked */
    TokenStream main;
    
    /** Reference stream to check the main one against */
    TokenStream ref;
    
    /** Token type for words containing apostrophes */
    private static final String APOSTROPHE_TYPE = 
        StandardTokenizerConstants.tokenImage[
                                  StandardTokenizerConstants.APOSTROPHE];
    
    /** Token type for acronyms */
    private static final String ACRONYM_TYPE = 
        StandardTokenizerConstants.tokenImage[
                                  StandardTokenizerConstants.ACRONYM];

    /** Construct a CheckingTokenStream */
    public CheckingTokenStream( TokenStream main, TokenStream ref ) {
        this.main = main;
        this.ref  = ref;
        
        // Assertions must be enabled!
        boolean flag = false;
        assert flag = true;
        if( !flag )
            throw new RuntimeException( 
                     "CheckingTokenStream requires assertions to be enabled" );
    } // constructor
    
    /**
     * Get the next token from the main stream. Checks that this token matches
     * the next one in the reference stream.
     */
    public Token next() 
        throws IOException
    {
        Token t1 = main.next();
        Token t2 = ref.next();
        if( t1 == null || t2 == null )
            assert t1 == t2;
        else {
            assert t1.termText().equals( t2.termText() );
            assert t1.startOffset() == t2.startOffset();
            assert t1.endOffset() == t2.endOffset();
            assert t1.getPositionIncrement() == t2.getPositionIncrement();
            assert idType(t1.type()).equals( idType(t2.type()) );
        }
        return t1;
    }
    
    /** Map the type to apostrophe, acronym, or other */
    private String idType( String type ) {
        if( type.equals(APOSTROPHE_TYPE) )
            return APOSTROPHE_TYPE;
        if( type.equals(ACRONYM_TYPE) )
            return ACRONYM_TYPE;
        return "other";
    }
    
    /** Close the token stream */
    public void close()
        throws IOException
    {
        main.close();
        ref.close();
    }
    
} // class CheckingTokenizer
