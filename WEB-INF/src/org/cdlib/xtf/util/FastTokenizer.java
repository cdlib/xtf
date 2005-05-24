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
import java.io.Reader;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.Tokenizer;
import org.cdlib.xtf.textIndexer.tokenizer.XTFTokenizer;

/**
 * Like Lucene's StandardTokenizer, but handles the easy cases very quickly.
 * Punts the hard cases to a real StandardTokenizer, but this is rare enough
 * that the speed increase is very substantial.
 * 
 * Does not currently support Chinese/Japanese/Korean, but adding this support
 * would be pretty easy.
 * 
 * @author Martin Haye
 */
public class FastTokenizer extends Tokenizer
{
    /** Array of characters to read from */
    private char[] source;
    
    /** Position within the {@link #source} array */
    private int    pos = 0;
    
    /** We use a special character to mark the end of a 
     * {@link DribbleReader}. 
     */
    static final char   fakeChar = '\u1049';
    
    /** This is the special word used by DribbleReader */
    static final String fakeWord = "" + fakeChar;

    /** 
     * Used to dribble out tokens to a standard tokenizer; used when
     * we encounter a case that's hard to figure out. 
     */
    private DribbleReader dribbleReader;
    
    /** Standard tokenizer, used for hard cases only */
    private Tokenizer stdTokenizer;
    
    private static final char[] charType = new char[0x10000];
    
    /** 
     * This table is a very quick way to identify the type of a character
     * (alpha, numeric, punctuation, etc.)
     */
    static {
        
        // Alpha-numeric... alpha
        setCharType( 'a', '\u0041', '\u005a' );
        setCharType( 'a', '\u0061', '\u007a' );
        setCharType( 'a', '\u00c0', '\u00d6' );
        setCharType( 'a', '\u00d8', '\u00f6' );
        setCharType( 'a', '\u00f8', '\u00ff' );
        setCharType( 'a', '\u0100', '\u1fff' );

        // ...num
        setCharType( 'a', '\u0030', '\u0039' );
        setCharType( 'a', '\u0660', '\u0669' );
        setCharType( 'a', '\u06f0', '\u06f9' );
        setCharType( 'a', '\u0966', '\u096f' );
        setCharType( 'a', '\u09e6', '\u09ef' );
        setCharType( 'a', '\u0a66', '\u0a6f' );
        setCharType( 'a', '\u0ae6', '\u0aef' );
        setCharType( 'a', '\u0b66', '\u0b6f' );
        setCharType( 'a', '\u0be7', '\u0bef' );
        setCharType( 'a', '\u0c66', '\u0c6f' );
        setCharType( 'a', '\u0ce6', '\u0cef' );
        setCharType( 'a', '\u0d66', '\u0d6f' );
        setCharType( 'a', '\u0e50', '\u0e59' );
        setCharType( 'a', '\u0ed0', '\u0ed9' );
        setCharType( 'a', '\u1040', '\u1049' );
        
        // XTF internal markers
        charType['\uEBEB'] = 'a'; // start-of-field marker
        charType['\uEE1D'] = 'a'; // end-of-field marker

        // Whitespace
        charType[' ' ] = 'w';
        charType['\t'] = 'w';
        charType['\n'] = 'w';
        charType['\r'] = 'w';
        charType['\f'] = 'w';
        
        // Punctuation
        charType['\''] = 'p';
        charType['.']  = 'p';
        charType['&']  = 'p';
        charType['@']  = 'p';
        charType['-']  = 'p';
        charType['/']  = 'p';
        charType[',']  = 'p';
        
        // Currency Symbols
        charType['\u0024'] = 's'; // Dollar
        charType['\u00a2'] = 's'; // Cent
        charType['\u00a3'] = 's'; // Pound Sterling
        charType['\u00a4'] = 's'; // currency symbol
        charType['\u00a5'] = 's'; // Yen
        charType['\u0192'] = 's'; // Florin currency symbol (Dutch)
        charType['\u20a3'] = 's'; // Franc
        charType['\u20a4'] = 's'; // Lira
        charType['\u20a7'] = 's'; // Peseta
        charType['\u20ac'] = 's'; // Euro
        
        // Fractions
        charType['\u00bc'] = 's'; // one quarter
        charType['\u00bd'] = 's'; // one half
        charType['\u00be'] = 's'; // three quarters
        charType['\u2153'] = 's'; // one third
        charType['\u2154'] = 's'; // two thirds
        charType['\u2155'] = 's'; // one fifth
        charType['\u2156'] = 's'; // two fifths
        charType['\u2157'] = 's'; // three fifths
        charType['\u2158'] = 's'; // four fifths
        charType['\u2159'] = 's'; // one sixth
        charType['\u215a'] = 's'; // five sixths
        charType['\u215b'] = 's'; // one eighth
        charType['\u215c'] = 's'; // three eighths
        charType['\u215d'] = 's'; // five eighths
        charType['\u215e'] = 's'; // seven eighths
        
        // Math symbols
        charType['\u002b'] = 's'; // plus
        charType['\u2212'] = 's'; // minus
        charType['\u003d'] = 's'; // equals
        charType['\u2260'] = 's'; // not equal
        charType['\u003c'] = 's'; // less than
        charType['\u003e'] = 's'; // greater than
        charType['\u2264'] = 's'; // less than or equal
        charType['\u2265'] = 's'; // greater than or equal
        charType['\u00b1'] = 's'; // plus/minus
        charType['\u00d7'] = 's'; // multiply
        charType['\u00f7'] = 's'; // divide
        charType['\u2219'] = 's'; // period-centered bullet operator
        charType['\u00b7'] = 's'; // mid-dot (same as period-centered bullet operator)
        charType['\u007e'] = 's'; // tilde
        charType['\u005e'] = 's'; // circumflex
        charType['\u00b0'] = 's'; // degree
        charType['\u00ac'] = 's'; // logical not
        charType['\u2248'] = 's'; // approximately equal
        charType['\u00b5'] = 's'; // micro
        charType['\u221e'] = 's'; // infinity
        charType['\u2202'] = 's'; // partial differential
        charType['\u220f'] = 's'; // product
        charType['\u03c0'] = 's'; // lower-case greek pi
        charType['\u222b'] = 's'; // integral
        charType['\u2126'] = 's'; // ohm
        charType['\u221a'] = 's'; // radical
        charType['\u2206'] = 's'; // increment
        charType['\u2211'] = 's'; // summation
        charType['\u25ca'] = 's'; // lozenge
        charType['\u212e'] = 's'; // estimate
        charType['\u2032'] = 's'; // single prime
        charType['\u2033'] = 's'; // double prime
        charType['\u2116'] = 's'; // numero
        
        // Other symbols
        charType['\u00ae'] = 's'; // registered trademark
        charType['\u00a9'] = 's'; // copyright
        charType['\u2122'] = 's'; // trademark
    };
    
    /** Utility method used when setting up the character type table */
    private static void setCharType( char type, char from, char to ) {
        for( char i = from; i <= to; i++ )
            charType[i] = type;
    }
    
    /** 
     * Create a tokenizer that will tokenize the stream of characters from
     * the given reader. Note that the reader must be an instance of
     * FastStringReader, or else fast tokenization isn't possible.
     * 
     * @param reader    Reader to get data from.
     */
    public FastTokenizer( FastStringReader reader ) {
        super( reader );
        String str = reader.getString();
        source = str.toCharArray();
    }
    
    /**
     * Retrieve the next token in the stream, or null if there are no more.
     */
    public Token next()
        throws IOException
    {
        // Skip whitespace and punctuation.
        int tpos = pos;
        final int tlen = source.length;
        char type = 0;
        while( tpos < tlen ) {
            type = charType[source[tpos]];
            if( type == 'a' || type == 's' )
                break;
            tpos++;
        }
        
        final int start = tpos;
        
        // If we hit a symbol, gobble just that character (we'll make a 
        // single-char token out of it.)
        //
        if( type == 's' )
            tpos++;
        else 
        {
            // Gobble up a string of alpha-numeric characters.
            while( tpos < tlen ) {
                type = charType[source[tpos]];
                if( type != 'a' )
                    break;
                tpos++;
            }
        }
        
        pos = tpos;
        
        // If at end of string, return null to mark the fact.
        if( pos == start )
            return null;
        
        // The only situation where our stupid but fast implementation might make
        // a mistake is when there is punctuation followed by alpha-numeric.
        //
        if( tpos >= tlen-1 || type != 'p' || charType[source[tpos+1]] != 'a' ) { 
            return new Token( new String(source, start, pos-start),
                              start, pos );
        }
        
        // Okay, to be safe we'd better use the standard tokenizer. Feed it
        // everything up til the next whitespace (or end-of-string).
        //
        if( dribbleReader == null ) {
            dribbleReader = new DribbleReader();
            stdTokenizer = new XTFTokenizer( dribbleReader );
        }
        
        for( ; pos < source.length; pos++ ) {
            type = charType[source[pos]];
            if( type == 'w' )
                break;
        }
        
        // Special case: the word "x"
        if( source[start] == 'x' || source[start] == 'X' ) {
            if( charType[source[start+1]] != 'a' ) {
                pos = start+1;
                return new Token( "x", start, start+1, "word" );
            }
        }
        
        // Now let's see what it thinks. First, get a reference token, making
        // sure that all old tokens have been dribbled away.
        //
        Token t1 = stdTokenizer.next();
        while( !t1.termText().equals(fakeWord) )
            t1 = stdTokenizer.next();

        dribbleReader.setChars( source, start, pos );
        
        Token t2 = stdTokenizer.next();
        assert !t2.termText().equals( fakeWord );
        assert t2.startOffset() - t1.startOffset() == 2;
        assert t2.termText().charAt(0) == source[start];

        int tokLen = t2.endOffset() - t2.startOffset();
        pos = start + tokLen;
        return new Token( t2.termText(), start, pos, t2.type() );
        
    } // next()
    
    /**
     * This class is used, when the fast tokenizer encounters a questionable
     * situation, to dribble out characters to a standard tokenizer that can
     * do a more complete job.
     * 
     * @author Martin Haye
     */
    private class DribbleReader extends Reader
    {
        /** String used to mark the end of the dribbled text */
        static final String fakeStr  = " " + fakeWord + " ";
        
        /** Character array version of {@link #fakeStr} */
        private final char[] fakeChars = fakeStr.toCharArray();
        
        /** Buffer of characters currently being dribbled */
        private char[] buf = fakeChars;
        
        /** Current position within {@link #buf} */
        private int    pos = 0;
        
        /** Max # of chars to dribble from {@link #buf} */
        private int    max = fakeChars.length;
        
        /** Does nothing... required by interface */
        public void close() throws IOException
        {
        }

        /** Establish a set of characters to dribble out */
        public void setChars( char[] buf, int pos, int max )
        {
            assert this.pos == 0 : "should have eaten previous string";
            assert buf != fakeChars;
            
            for( int i = pos; i < max; i++ ) {
                if( buf[i] == fakeChar )
                    buf[i] = fakeChar-1;
            }

            this.buf = buf;
            this.pos = pos;
            this.max = max;
        } // setChars()
        
        /** Dribble some characters. If we run out, we begin to dribble
         *  the fake word string.
         */
        public int read( char[] cbuf, int off, int len )
            throws IOException
        {
            final int avail = max - pos;
            final int toRead = (len > avail) ? avail : len;

            System.arraycopy( buf, pos, cbuf, off, toRead );
            pos += toRead;
            
            if( pos == max ) {
                buf = fakeChars;
                max = fakeChars.length;
                pos = 0;
            }
            
            return toRead;
        } // read()
    } // class DribbleReader

} // class FastTokenizer
