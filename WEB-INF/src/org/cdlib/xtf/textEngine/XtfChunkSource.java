package org.cdlib.xtf.textEngine;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.chunk.Chunk;
import org.apache.lucene.chunk.ChunkSource;
import org.apache.lucene.chunk.DocNumMap;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.cdlib.xtf.textIndexer.XtfSpecialTokensFilter;
import org.cdlib.xtf.util.Trace;

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
 
/*
 * This file created on Jan 15, 2005 by Martin Haye
 */


/** Performs special loading duties for our XTF chunks */
class XtfChunkSource extends ChunkSource
{
  private static final char bumpMarker = XtfSpecialTokensFilter.bumpMarker;
  private static final char nodeMarker = XtfSpecialTokensFilter.nodeMarker;

  /** Construct a chunk source */
  XtfChunkSource( IndexReader reader,
                  DocNumMap docNumMap,
                  int mainDocNum,
                  String field,
                  Analyzer analyzer )
  {
    super( reader, docNumMap, mainDocNum, field, analyzer );
  } // constructor

  /** 
   * Create a new storage place for chunk tokens (derived classes may 
   * wish to override) 
   */
  protected Chunk createChunkTokens( int chunkNum ) 
  {
    return new XtfChunk(this, chunkNum);
  } // createChunkTokens()
  
  /** 
   * Read the text for the given chunk (derived classes may 
   * wish to override) 
   */
  protected void loadText(int chunkNum, Chunk ct) 
      throws IOException
  {
    XtfChunk chunk = (XtfChunk)ct;
    
    Document doc = reader.document( chunkNum );
    chunk.text = doc.get(field);
    
    // Record the starting node number and word offset.
    try {
        chunk.startNodeNumber = Integer.parseInt( doc.get("node") );
        chunk.startWordOffset = Integer.parseInt( doc.get("wordOffset") );
        chunk.sectionType = doc.get( "sectionType" );
    } catch( NumberFormatException e ) { 
        throw new RuntimeException( e ); 
    }
  } // loadText()
  
  /** 
   * Read in and tokenize a chunk. Maintains a cache of recently loaded
   * chunks for speed.
   */
  public Chunk loadChunk( int chunkNum )
  {
    assert chunkNum >= firstChunk && chunkNum <= lastChunk;
    
    // First, do the normal loading/tokenizing work.
    XtfChunk chunk = (XtfChunk) super.loadChunk(chunkNum);
    
    // If we already post-processed this chunk, don't do it again.
    if( chunk.nodeNumbers != null )
        return chunk;
    
    // Now figure out the word offset and node number for each token. Along
    // the way, we also want to delete all the markers and create new tokens
    // that reference the modified text.
    //
    chunk.nodeNumbers = new int[chunk.tokens.length];
    chunk.wordOffsets = new int[chunk.tokens.length];
    
    int nodeNumber = chunk.startNodeNumber;
    int wordOffset = chunk.startWordOffset;
    
    int totalWordOffset = chunk.minWordPos - 1;
    int tokenWordOffset = chunk.minWordPos - 1;
    int prevCharPos = 0;
    
    StringBuffer buf = new StringBuffer( chunk.text.length() );
    
    for( int i = 0; i <= chunk.tokens.length; i++ ) 
    {
        int charPos = (i<chunk.tokens.length) ? 
            chunk.tokens[i].startOffset() : chunk.text.length();
        String textBetween = chunk.text.substring( prevCharPos, charPos );
        
        if( i < chunk.tokens.length ) {
            totalWordOffset++;
            tokenWordOffset += chunk.tokens[i].getPositionIncrement();
        }
        
        // Process any node or bump markers between the previous token and
        // this one.
        //
        int pos = 0;
        while( true ) {
            int nodeMarkerPos = textBetween.indexOf( nodeMarker, pos );
            int bumpMarkerPos = textBetween.indexOf( bumpMarker, pos );
            if( nodeMarkerPos >= 0 && 
                (bumpMarkerPos < 0 || nodeMarkerPos < bumpMarkerPos) ) 
            {
                buf.append( textBetween.substring(pos, nodeMarkerPos) );
                
                pos++;
                nodeNumber++;
                wordOffset = 0;
                
                pos = nodeMarkerPos + 1;
            }
            else if( bumpMarkerPos >= 0 ) 
            {
                buf.append( textBetween.substring(pos, bumpMarkerPos) );
                
                int bumpEnd = textBetween.indexOf( bumpMarker, bumpMarkerPos+1 );
                assert bumpEnd >= 0;
                String bumpText = textBetween.substring( bumpMarkerPos+1, bumpEnd );
                try {
                    int bump = Integer.parseInt( bumpText );
                    //WRONG: wordOffset += bump; 
                    totalWordOffset += bump;
                } catch( NumberFormatException e ) { 
                    throw new RuntimeException(e); 
                }
                
                pos = bumpEnd + 1;
            }
            else
                break;
        }
        
        // Trim whitespace at start of the line.
        if( i > 0 )
            buf.append( textBetween.substring(pos) );
        
        if( i == chunk.tokens.length )
            break;
        
        assert totalWordOffset == tokenWordOffset;
        
        int startPos = buf.length();
        buf.append( chunk.text.substring(chunk.tokens[i].startOffset(),
                                         chunk.tokens[i].endOffset()) );
        int endPos = buf.length();
        

        Token oldToken = chunk.tokens[i];
        chunk.tokens[i] = new Token( oldToken.termText(),
                                     startPos, endPos );
        chunk.tokens[i].setPositionIncrement( oldToken.getPositionIncrement() );
        chunk.nodeNumbers[i] = nodeNumber;
        chunk.wordOffsets[i] = wordOffset;
        wordOffset++;
        
        chunk.maxWordPos = totalWordOffset;
        
        prevCharPos = oldToken.endOffset();
    } // for i
    
    // Replace the old text with the new (which has the markers removed).
    chunk.text = buf.toString();
    
    // All done!
    return chunk;
  } // loadChunk()
  
  /**
   * <p><b>DEBUGGING ONLY:</b></p>
   * 
   * Print out debugging info for the current chunk, including all of
   * its tokens.
   */
  private void debugChunk( XtfChunk chunk )
  {
      StringBuffer buf1 = new StringBuffer();
      StringBuffer buf2 = new StringBuffer();
      String spaces = 
          "                                                            ";
      
      Trace.debug( "*** CHUNK " + chunk.chunkNum + " ***" );
      
      if( chunk.tokens.length == 0 ) {
          Trace.debug( "   [[chunk has no tokens]]" );
          return;
      }
      
      buf1.append( spaces.substring(0, chunk.tokens[0].startOffset()) );
      buf2.append( chunk.text.substring(0, chunk.tokens[0].startOffset()) );
      
      int pos = chunk.minWordPos - 1;
      for( int i = 0; i < chunk.tokens.length; i++ ) {
          int tokLen = (i < chunk.tokens.length-1) ? 
                  (chunk.tokens[i+1].startOffset() - chunk.tokens[i].startOffset()) :
                  (chunk.text.length() - chunk.tokens[i].startOffset());
                  
          if( buf1.length() + tokLen > 80 ) {
              Trace.debug( " " + buf1.toString() );
              Trace.debug( "\"" + buf2.toString() + "\"\n" );
              buf1.setLength( 0 );
              buf2.setLength( 0 );
          }

          pos += chunk.tokens[i].getPositionIncrement();
          String num = Integer.toString(pos) + " ";
          if( num.length() < tokLen )
              num += spaces.substring( 0, tokLen - num.length() );
          
          String tokText = chunk.text.substring(
                                   chunk.tokens[i].startOffset(), 
                                   chunk.tokens[i].startOffset() + tokLen );
          if( tokText.length() < num.length() )
              tokText += spaces.substring( 0, num.length() - tokText.length() );
         
          buf1.append( num );
          buf2.append( tokText );    
      }
      
      Trace.debug( " " + buf1.toString() );
      Trace.debug( "\"" + buf2.toString() + "\"\n" );
  } // debugChunk()
  
}