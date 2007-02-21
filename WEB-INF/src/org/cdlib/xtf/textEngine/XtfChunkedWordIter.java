package org.cdlib.xtf.textEngine;


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
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.chunk.ChunkedWordIter;
import org.apache.lucene.chunk.DocNumMap;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.mark.MarkPos;
import org.apache.lucene.mark.WordIter;

/**
 * Handles iterating over XTF's tokenized documents, including special
 * tracking of node numbers and word offsets.
 *
 * @author Martin Haye
 */
public class XtfChunkedWordIter extends ChunkedWordIter 
{
  /**
   * Construct the iterator and read in starting text from the given
   * chunk.
   *
   * @param reader      where to read chunks from
   * @param docNumMap   maps main doc num to chunk numbers
   * @param mainDocNum  doc ID of the main document
   * @param field       field tokenize and iterate
   * @param analyzer    used to tokenize the field
   */
  public XtfChunkedWordIter(IndexReader reader, DocNumMap docNumMap,
                            int mainDocNum, String field, Analyzer analyzer) 
  {
    super(new XtfChunkSource(reader, docNumMap, mainDocNum, field, analyzer));
  } // constructor

  /** Create an uninitialized MarkPos structure */
  public MarkPos getPos(int startOrEnd) {
    MarkPos pos = new XtfChunkMarkPos();
    getPos(pos, startOrEnd);
    return pos;
  }

  /** Get the position of the start of the current word */
  public void getPos(MarkPos pos, int startOrEnd) 
  {
    super.getPos(pos, startOrEnd);

    XtfChunkMarkPos xPos = (XtfChunkMarkPos)pos;

    if (startOrEnd == WordIter.TERM_END_PLUS)
      xPos.trim();

    if (chunk != null) {
      XtfChunk xc = (XtfChunk)chunk;
      xPos.nodeNumber = xc.nodeNumbers[tokNum];
      xPos.wordOffset = xc.wordOffsets[tokNum];
      xPos.sectionType = xc.sectionType;
    }
  }
} // class XtfChunkedWordIter
