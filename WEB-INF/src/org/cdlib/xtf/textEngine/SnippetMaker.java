package org.cdlib.xtf.textEngine;

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
import java.io.StringReader;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;

import org.cdlib.xtf.textEngine.dedupeSpans.SpanHit;
import org.cdlib.xtf.textIndexer.XTFTextAnalyzer;
import org.cdlib.xtf.textIndexer.XtfSpecialTokensFilter;
import org.cdlib.xtf.util.Trace;

/**
 * Does the heavy lifting of interpreting span hits using the actual document
 * text stored in the index. Marks the hit and any matching terms, and 
 * includes a configurable amount of context words.
 * 
 * @author Martin Haye
 */
public class SnippetMaker
{
    /** Lucene index reader used to fetch text data */
    public  IndexReader  reader;
    
    /** Snippet being worked on currently */
    private Snippet      curSnippet;
    
    /** Lucene analyzer used for tokenizing text */
    private Analyzer     analyzer;
    
    /** Used by the analyzer for additional text processing */
    private StringBuffer blurbedText = new StringBuffer();
    
    /** 
     * Keeps track of which chunks belong to which source document in the
     * index.
     */
    private DocNumMap    docNumMap;
    
    /** Max # of words in an index chunk */
    private int          chunkSize;
    
    /** Amount of overlap between adjacent index chunks */
    private int          chunkOverlap;
    
    /** Set of stop-words removed (e.g. "the", "a", "and", etc.) */
    private Set          stopSet;
    
    /** Target # of characters to include in the snippet. */
    private int          maxContext;
    
    /** Map of all the search terms to hilight */
    private TermMap      terms;
    
    /** Buffer used to accumulate snippet text */
    private StringBuffer buf;
    
    /** Current length (excluding mark-up tags) of the snippet */
    private int          length;
    
    /** Number of context chars we've added before the hit */
    private int          addedBefore;

    /** Number of context chars we've added after the hit */
    private int          addedAfter;
    
    /** First chunk in the document */
    private int          minDoc;
    
    /** Last chunk in the document, plus one */
    private int          maxDoc;
    
    /** 
     * Keeps track of the number of non-word chars at the end, so we can
     * easily insert the &lt;/hit&gt; tag when needed.
     */
    private int          backOff;
    
    /** 
     * Used to move forward until we find the first word matching the hit,
     * then moves backward to add context before the hit. 
     */
    private WordIter     iter0;
    
    /**
     * Used to find the end of a hit, then keeps moving forward to add
     * context after the hit.
     */
    private WordIter     iter1;
    
    /** true if there are more words in iter0 */
    private boolean      more0;
    
    /** true if there are more words in iter1 */
    private boolean      more1;
    
    /** true if we are currently processing within a hit */
    private boolean      inHit;
    
    /**
     * Constructs a SnippetMaker, ready to make snippets using the given
     * index reader to load text data.
     * 
     * @param reader        Index reader to fetch text data from
     * @param docNumMap     Maps chunk numbers to document numbers
     * @param chunkSize     Max # words per chunk
     * @param chunkOverlap  Overlap between adjacent chunks
     * @param stopSet       Stop words removed (e.g. "the", "a", "and", etc.)
     * @param maxContext    Target # chars for hit + context
     * @param terms         Terms to mark with &lt;term&gt; tags.
     */
    public SnippetMaker( IndexReader reader,
                         DocNumMap   docNumMap,
                         int         chunkSize,
                         int         chunkOverlap,
                         Set         stopSet,
                         int         maxContext,
                         TermMap     terms )
    {
        this.reader         = reader;
        this.docNumMap      = docNumMap;
        this.chunkSize      = chunkSize;
        this.chunkOverlap   = chunkOverlap;
        this.stopSet        = stopSet;
        this.maxContext   = maxContext;
        this.terms          = terms;
        
        // Use the indexer's actual analyzer, so that our results always
        // agree (especially the positions which are critical.)
        //
        analyzer = new XTFTextAnalyzer( null, -1, blurbedText );

        buf = new StringBuffer( maxContext );
    } // constructor
    
    /** Obtain a list of the query terms */
    public TermMap terms() {
        return terms;
    }
    
    /** 
     * Obtain a list of stop-words in the index (e.g. "the", "a", "and", 
     * etc.) 
     */
    public Set stopSet() {
        return stopSet;
    }
    
    /**
     * Worker function that determines the start and end of a hit. When no
     * context is required, then this is all the snippet-making process
     * consists of. 
     * 
     * @param mainDoc   Lucene index of the document info chunk
     * @param sp        Span hit to work on
     * @return          A snippet, fully formed except for its 'text'.
     * 
     * @throws IOException  If something goes wrong reading the index.
     */
    private Snippet narrowHit( int mainDoc, SpanHit sp )
        throws IOException
    {
        curSnippet = new Snippet();
        curSnippet.score = sp.score;
        
        // Record the first and last docs for the main document.
        minDoc = docNumMap.getFirstChunk( mainDoc );
        maxDoc = docNumMap.getLastChunk( mainDoc );
        
        // Clear the buffer.
        buf.setLength( 0 );
        length = 0;
        
        // Find the start word.
        iter0 = new WordIter( sp.chunk );
        while( iter0.next() && iter0.wordPos() < sp.start )
            ;
        
        // Record the section type here.
        curSnippet.sectionType = iter0.sectionType;
        
        // Mark the start of the hit in the output.
        startHit();

        // Then add the first hit word.
        assert iter0.wordPos() == sp.start;
        String lastTerm = iter0.term();
        assert terms.contains( lastTerm );
        addWord( iter0, true );
        
        // Now record words up to the end word.
        //
        // We don't check 'more1' in the loop control, because we need to be
        // sure to scan to the last word even if we can't fit it into the
        // snippet.
        //
        iter1 = (WordIter) iter0.clone();
        more1 = true;
        int lastNode = iter1.nodeNum;
        int lastOff  = iter1.nodeWordOffset;
        while( true ) {
            if( !iter1.next() ) {
                more1 = false;
                break;
            }
            if( iter1.wordPos() >= sp.end )
                break;

            lastNode = iter1.nodeNum;
            lastOff  = iter1.nodeWordOffset;
            lastTerm = iter1.term();
            if( more1 ) {
                if( !addWord(iter1, true) )
                    more1 = false;
            }
        }
        assert terms.contains(lastTerm) : "position mismatch";
        
        // Mark this as the end of the hit.
        endHit( lastNode, lastOff+1 );        
        
        // All done.
        return curSnippet;
    } // narrowHit()

    /**
     * Full-blown snippet formation process, which narrows the hit using
     * {@link #narrowHit(int, SpanHit)}, then adds context before and after
     * the hit up to {@link #maxContext} characters.
     * 
     * @param mainDoc   Lucene index of the main document chunk
     * @param sp        Span representing the hit within the document's text
     * @param getText   true to do the full process, fetching the text;
     *                  false to only find the starting and ending
     *                  node and word offsets.
     * 
     * @return          A resolved Snippet, with text if 'getText' was true
     * 
     * @throws IOException  If something went wrong reading from the index
     */
    public Snippet make( int mainDoc, SpanHit sp, boolean getText )
        throws IOException
    {
        // First do the general work of finding the hit itself. If no text
        // required, then we're done.
        //
        Snippet snippet = narrowHit( mainDoc, sp );
        if( !getText )
            return snippet;
        
        // Now alternate adding more words until we fill the requisite
        // number of chars.
        //
        addedBefore = 0;
        addedAfter  = 0;
        more0 = true;
        while( more0 || more1 ) 
        {
            int oldLength = length;
            
            // Half the time, add a word at the start.
            while( more0 && (!more1 || addedBefore <= addedAfter) ) {
                if( !iter0.prev() )
                    more0 = false;
                else if( !addWord(iter0, false) )
                    more0 = false;
            }
                
            // The other half, add a word at the end.
            while( more1 && (!more0 || addedAfter <= addedBefore) ) {
                if( !addWord(iter1, true) )
                    more1 = false;
                else if( !iter1.next() )
                    more1 = false;
            }
            
            // Certain situations can lead to infinite loop... break out.
            if( length == oldLength )
                break;
        } // while( more0 || more1 )
        
        // Got the snippet now.
        snippet.text = buf.toString().trim();
        return snippet;
        
    } // make()
    
    /**
     * Marks all the terms within the given text. Typically used to mark
     * terms within a meta-data query.
     * 
     * @param text      The text to mark up
     * @param field     Meta-data field to add to the &lt;term&gt; tags.
     * 
     * @return          Marked up text
     */
    public String makeFull( String text, String field )
    {
        int prevEnd = 0;
        try {
            buf.setLength( 0 );
            blurbedText.replace( 0, 100000000, text );
            TokenStream stream = analyzer.tokenStream( "text",
                                                       new StringReader(text) );
            while( true ) {
                Token t = null;
                t = stream.next();
                if( t == null )
                    break;
                
                // Handle intervening punctuation and spaces.
                if( t.startOffset() > prevEnd ) {
                    buf.append( text.substring(prevEnd, t.startOffset())
                                    .replaceAll("\\<", "&lt;")
                                    .replaceAll("\\>", "&gt;") );
                }
                prevEnd = t.endOffset();
                
                String term = text.substring( t.startOffset(), t.endOffset() );
                String termField = terms.getField( term, field );
                if( termField == null || !termField.equals(field) )
                    buf.append( term );
                else if( stopSet != null && stopSet.contains(t.termText()) )
                    buf.append( term );
                else {
                    buf.append( 
                       "<term>" + term + "</term>" );
                }
            }
            stream.close();
        }
        catch( IOException e ) {
            assert false : "How could StringReader throw an exception?";
        }
        
        if( text.length() > prevEnd )
            buf.append( text.substring(prevEnd) );
        return buf.toString();
    } // makeFull()
    
    /** Adds the initial &lt;hit&gt; marker, at the current snippet end */
    private void startHit()
    {
        buf.append( "<hit>" );
        
        // Record starting node info
        curSnippet.startNode   = iter0.nodeNum;
        curSnippet.startOffset = iter0.nodeWordOffset;
        
        // Remember that we're in a hit, so that stop words will be marked
        // in here.
        //
        inHit = true;
    } // startHit()
    
    /** Adds the ending &lt;/hit&gt; marker, at the current snippet end */
    private void endHit( int nodeNum, int wordOffset )
    {
        // Back off the last non-word chars, then insert the tag end.
        int pos = buf.length() - backOff;
        buf.insert( pos, "</hit>" );
        
        // Record ending node info
        curSnippet.endNode   = nodeNum;
        curSnippet.endOffset = wordOffset;
        
        // We're done with the hit... turn off stop word marking.
        inHit = false;
    } // endHit()
    
    /**
     * Adds a word from the given iterator to the current snippet, either at
     * the start or the end.
     * 
     * @param iter      Iterator to get the word from
     * @param addAtEnd  true to add at the end of the snippet, false to add
     *                  at the start
     * 
     * @return          true if there was room to add the word.
     */
    private boolean addWord( WordIter iter, boolean addAtEnd )
    {
        final int wordLenPlus = iter.lengthPlus();
        if( (length + wordLenPlus) <= maxContext ) {
            simpleAddWord( iter, true, addAtEnd );
            length += wordLenPlus;
            return true;
        }
        
        // If adding at start, we always need the extra space to separate the
        // word from those after it.
        //
        if( !addAtEnd )
            return false;
                
        final int wordLen = iter.length();
        if( (length + wordLen) <= maxContext ) {
            simpleAddWord( iter, false, addAtEnd );
            length += wordLen;
            return true;
        }
        
        return false;
    }
    
    /**
     * Helper function that adds a word, with or without its trailing non-
     * word chars, to the current snippet.
     * 
     * @param iter      Iterator to get the word from
     * @param plus      true to add non-word chars after the word
     * @param addAtEnd  true to add at the end of the snippet, false to add
     *                  at the start
     */
    private void simpleAddWord( WordIter iter, boolean plus, boolean addAtEnd )
    {
        String text = iter.word();
        String textField = terms.getField( iter.term(), "text" );
        if( textField != null && textField.equals("text") ) {
            if( inHit || stopSet == null || !stopSet.contains(iter.term()) ) {
                text = "<term>" + text + "</term>";
            }
        }
        if( plus ) {
            String plusStr = iter.plus();
            if( plusStr.indexOf("<") >= 0 )
                plusStr = plusStr.replaceAll( "\\<", "" );
            if( plusStr.indexOf(">") >= 0 )
                plusStr = plusStr.replaceAll( "\\>", "" );
            if( plusStr.indexOf("&") >= 0 )
                plusStr = plusStr.replaceAll( "\\&", "" );
            text += plusStr;
            if( addAtEnd )
                backOff = plusStr.length();
        }
        else if( addAtEnd )
            backOff = 0;
        
        if( addAtEnd ) {
            addedAfter += iter.lengthPlus();
            buf.append( text );
        }
        else {
            addedBefore += iter.lengthPlus();
            buf.insert( 0, text );
        }
    } // addWord()
        
    /**
     * Utility class used to iterate either forward or backward through the
     * tokens in one or more text chunks read from the Lucene index
     */
    private class WordIter implements Cloneable
    {
        /** Lucene index of the current chunk */
        private int         doc;
        
        /** Un-tokenized text of the current chunk */
        private String      text;
        
        /** Array of tokens, holding words from the current chunk */
        private Token[]     tokens = new Token[chunkSize+1]; // sentinel room
        
        /** Number of tokens in the current chunk */
        private int         nTokens;
        
        /** Current token this iterator is pointed at */
        private int         tokNum;
        
        /** Maximum word position (i.e. position of last token+1) */
        private int         maxWordPos;
        
        /** Word position of the curren token */
        private int         wordPos = -1;
        
        /** Offset of the first word in the chunk relative to the last node */
        private int         wordOffset;
        
        /** 
         * Number (a'la LazyDocument) of the node closest before the current
         * word.
         */
        private int         nodeNum;
        
        /** Offset from the most recent node of the current word */
        private int         nodeWordOffset = -1;
        
        /** Extra (non-word) chars after the current token */
        private String      plus;
        
        /** true if there are more tokens awaiting, false if end of document */
        private boolean     more;
        
        /** 'sectionType' of the current section in the source document */
        private String      sectionType;
        
        /** 
         * Special sentinel token at the end of the token list. Tried to
         * pick a word that would never appear in real life, though it won't
         * really cause any harm if it does.
         */
        private static final String SENTINEL = "kqprzzw";
        
        /**
         * Construct the iterator and read in starting text from the given
         * chunk.
         * 
         * @param doc               Chunk number to read
         * @throws IOException      If something goes wrong reading the data
         */
        public WordIter( int doc )
            throws IOException
        {
            this.doc      = doc;
            more          = true;
            readDoc();
        } // constructor
        
        /** Makes an independent copy of this iterator */
        public Object clone() {
            try {
                Token[] copy = new Token[tokens.length];
                System.arraycopy( tokens, 0, copy, 0, chunkSize );
                Object o = super.clone();
                tokens = copy;
                return o;
            }
            catch( CloneNotSupportedException e ) {
                assert false;
                return null;
            }
        }
        
        /** Reads the text of the current chunk and tokenizes it */
        private boolean readDoc()
            throws IOException
        {
            // Check the document number for validity.
            if( doc < minDoc || doc > maxDoc ) {
                more = false;
                return false;
            }
            
            // First, fetch the text. It's possible that whole range has been 
            // deleted, and we wouldn't necessarily know. Hence the catch
            // below.
            //
            Document document = null;
            try {
                document = reader.document( doc );
            }
            catch( IllegalArgumentException e ) {
                  if( reader.isDeleted(doc) ) {
                        more = false;
                        return false;
                  }
                  throw e;
            }
            text = document.get("text");
            
            // We need to pick up the final word bump if there is one, so put
            // a sentinel at the end of the text.
            //
            text += " " + SENTINEL;
            
            // Tokenize the whole thing.
            blurbedText.replace( 0, 100000000, text );
            TokenStream stream = analyzer.tokenStream( "text",
                                                       new StringReader(text) );
            nTokens = 0;
            maxWordPos = -1;
            while( true ) {
                Token t = stream.next();
                if( t == null )
                    break;
                tokens[nTokens++] = t;
                maxWordPos += t.getPositionIncrement();
            }
            stream.close();
            assert maxWordPos <= chunkSize;
            
            // Also record the section type if present.
            sectionType = document.get( "sectionType" );
            
            // Record the node number and node word offset.
            nodeNum = Integer.parseInt( document.get("node") );
            nodeWordOffset = Integer.parseInt(document.get("wordOffset")) - 1;
            assert nodeNum        >= 0  : "node number missing from index";
            assert nodeWordOffset >= -1 : "node word offset missing from index";
            
            // Reset variables
            tokNum = wordPos = -1;
            
            // And we're done.
            return true;
        } // readDoc()
        
        /**
         * <p><b>DEBUGGING ONLY:</b></p>
         * 
         * <p>Verify that the word psoitions in the n-gram token stream used by 
         * the indexer are identical to those given by our non-ngramming 
         * stream.</p>
         * 
         * <p><i>For efficiency's sake, this should not be called in 
         * normal use.</i></p>
         */
        private void testNgrams()
            throws IOException
        {
            Trace.debug( "Testing ngrams" );
            
            blurbedText.replace( 0, 100000000, text );
            Analyzer analyzer = new XTFTextAnalyzer( stopSet, -1, blurbedText );
            TokenStream stream = analyzer.tokenStream( "text",
                                                       new StringReader(text) );
            int pos1 = -1;
            int pos2 = -1;
            Token[] streamTokens = new Token[nTokens*3];
            int streamTokNum = 0;
            int origTokNum = 0;
            Token origTok = null;
            while( true ) {
                Token t = stream.next();
                if( t == null )
                    break;
                streamTokens[streamTokNum++] = t;
                pos1 += t.getPositionIncrement();
                while( origTokNum < nTokens && pos2 < pos1 ) {
                    origTok = tokens[origTokNum];
                    pos2 += tokens[origTokNum].getPositionIncrement();
                    origTokNum++;
                }
                assert pos1 == pos2;
                assert t.termText().startsWith(origTok.termText());
            }
            stream.close();
            assert origTokNum == nTokens;
        }
        
        /**
         * <p><b>DEBUGGING ONLY:</b></p>
         * 
         * Print out debugging info for the current chunk, including all of
         * its tokens.
         */
        private void debugDoc()
        {
            StringBuffer buf1 = new StringBuffer();
            StringBuffer buf2 = new StringBuffer();
            String spaces = 
                "                                                            ";
            
            Trace.debug( "*** DOC " + doc + " ***" );
            int pos = -1;
            for( int i = 0; i < nTokens; i++ ) {
                int tokLen = (i < nTokens-1) ? 
                        (tokens[i+1].startOffset() - tokens[i].startOffset()) :
                        (text.length() - tokens[i].startOffset());
                        
                if( buf1.length() + tokLen > 80 ) {
                    Trace.debug( buf1.toString() );
                    Trace.debug( buf2.toString() + "\n" );
                    buf1.setLength( 0 );
                    buf2.setLength( 0 );
                }

                pos += tokens[i].getPositionIncrement();
                String num = Integer.toString( pos );
                if( num.length() < tokLen )
                    num += spaces.substring( 0, tokLen - num.length() );
                
                buf1.append( num );
                buf2.append( text.substring(tokens[i].startOffset(), 
                                            tokens[i].startOffset() + tokLen) );    
            }
            
            Trace.debug( buf1.toString() );
            Trace.debug( buf2.toString() + "\n" );
        } // debugDoc()
        
        /** Advance to the next token, reading another chunk if necessary */
        public final boolean next()
            throws IOException
        {
            if( !more )
                return false;
            
            // Update our state.
            tokNum++;
            wordPos += tokens[tokNum].getPositionIncrement();

            // If we've hit the sentinel, read the next chunk.
            if( tokNum == nTokens-1 ) {
                assert tokens[tokNum].termText().equals(SENTINEL);
                
                boolean doSkip       = true;
                int    targetWordPos = wordPos - (chunkSize - chunkOverlap);
                String targetTerm    = 
                    (targetWordPos >= chunkSize-chunkOverlap) ?
                    tokens[tokNum-1].termText() :
                    null;
                
                while( true ) {
                    doc++;
                    if( !readDoc() ) {
                        more = false;
                        return false;
                    }
                    wordOffset += (chunkSize - chunkOverlap);
                    if( nTokens == 1 )
                        doSkip = false;
                    else
                        break;
                }
                    
                // Skip the overlapping words.
                if( doSkip ) {
                    while( doSkip && wordPos < targetWordPos ) {
                        if( !next() ) {
                            more = false;
                            return false;
                        }
                    } // while
                    assert targetTerm == null ||
                           tokens[tokNum-1].termText().equals(targetTerm);
                }
                else
                    next();
                
            } // if
            
            // Node that we've eaten one word.
            nodeWordOffset++;
            
            // See if we passed any node boundaries.
            final int startChar = (tokNum > 0) ? tokens[tokNum-1].endOffset()
                                               : 0;
            final int endChar   = tokens[tokNum].startOffset();
            for( int i = startChar; i < endChar; i++ ) {
                if( text.charAt(i) == XtfSpecialTokensFilter.nodeMarker ) {
                    nodeNum++;
                    nodeWordOffset = 0;
                }
            } // for i
            
            // And we're done.
            plus = null;
            return more;
        } // next()
        
        /** Go to the previous token, reading another chunk if necessary */
        public final boolean prev()
            throws IOException
        {
            if( !more )
                return false;
            
            // If we've already passed the first word, go to the previous
            // chunk for more.
            //
            if( tokNum == 0 ) {
                
                String targetTerm = tokens[0].termText();
                int    targetWordPos  = -1 + 
                                        tokens[0].getPositionIncrement() + 
                                        (chunkSize - chunkOverlap); 

                boolean doSkip = true;
                while( true ) {
                    doc--;
                    if( !readDoc() ) {
                        more = false;
                        return false;
                    }
                    wordOffset -= chunkOverlap;
                    if( nTokens == 0 )
                        doSkip = false;
                    else
                        break;
                }
                    
                tokNum  = nTokens-1;
                wordPos = maxWordPos;
                
                // Skip the overlapping words.
                if( doSkip ) {
                    while( doSkip && wordPos > targetWordPos ) {
                        if( !prev() ) {
                            more = false;
                            return false;
                        }
                    } // while
                    assert targetWordPos == chunkSize ||
                           tokens[tokNum].termText().equals( targetTerm );
                    prev();
                }
                else
                    prev();
            } // if
            else {
                // Okay, back up one spot.
                wordPos -= tokens[tokNum].getPositionIncrement();
                --tokNum;
            }

            // And we're done.
            plus = null;
            return more;
        } // prev()
        
        /** Get the word position, within the chunk, of the current word */
        public final int wordPos() {
            return wordPos + wordOffset;
        }
        
        /** Get the length of the current token's word characters */
        public final int length() {
            return tokens[tokNum].endOffset() - 
                   tokens[tokNum].startOffset();
        }
        
        /** 
         * Get the length of the current token plus any non-word chars
         * following it.
         */
        public final int lengthPlus() {
            if( plus == null )
                plus();
            return length() + plus.length();
        }
        
        /** 
         * Get the term text (a massaged version of the source text) for the
         * current token.
         */
        public final String term() {
            assert tokNum != nTokens-1 : "ran beyond end";
            return tokens[tokNum].termText();
        }

        /** Get the actual source word for the current token. */
        public final String word() {
            assert tokNum < nTokens-1 : "ran beyond end";
            return text.substring( tokens[tokNum].startOffset(),
                                   tokens[tokNum].endOffset() );
        }
        
        /** Get the non-word chars (if any) following the current token */
        public final String plus() {
            assert tokNum < nTokens-1 : "ran beyond end";
            plus = text.substring( tokens[tokNum].endOffset(),
                                   tokens[tokNum+1].startOffset() );
            
            // Strip out special tokens (might be one, might be more)
            while( true ) {
                int start = plus.indexOf( XtfSpecialTokensFilter.bumpMarker );
                if( start < 0 )
                    break;
                int end = plus.indexOf( XtfSpecialTokensFilter.bumpMarker, start+1 );
                assert end >= 0 : "Special token missing end mark";

                if( start == 0 )
                    plus = plus.substring(end+1);
                else if( end == plus.length()-1 )
                    plus = plus.substring(0, start);
                else
                    plus = plus.substring(0, start) + plus.substring(end+1);
            }
            
            // Also strip out node markers.
            while( true ) {
                int pos = plus.indexOf( XtfSpecialTokensFilter.nodeMarker );
                if( pos < 0 )
                    break;
                
                if( pos == 0 )
                    plus = plus.substring( pos+1 );
                else if( pos == plus.length()-1 )
                    plus = plus.substring( 0, pos );
                else
                    plus = plus.substring(0, pos) + plus.substring(pos+1);
            }
            
            return plus;
        } // plus()
    } // class WordIter
    
} // class SnippetMaker
