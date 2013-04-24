package org.cdlib.xtf.saxonExt.pipe;

/*
 * Copyright (c) 2012, Regents of the University of California
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
import java.io.RandomAccessFile;

import com.lowagie.text.pdf.RandomAccessFileOrArray;

/**
 * Class to provide buffered, random access to a PDF file. Useful for when we
 * can't realistically fit a PDF file into memory.
 * 
 * @author Martin Haye
 */
class BufferedRandomAccessFile extends RandomAccessFileOrArray
{
  // Unbuffered base file
  RandomAccessFile baseFile;
  
  // Support for pushing back a single byte
  byte prevByte;
  boolean havePrevByte = false;
  
  // Buffering
  final int BUFFER_SIZE = 4096;
  byte[] buffer = new byte[BUFFER_SIZE];
  int startOffset = 0;
  int bufferLength = 0;
  int bufferPos = 0;
  int bufferFilePointer = 0;
  String filename;

  /* Constructor - open up the file and initialize the file pointer to zero */
  public BufferedRandomAccessFile(String filename) throws IOException {
    super(filename, false, true);
    this.filename = filename;
    baseFile = new RandomAccessFile(filename, "r");
  }

  @Override
  public void pushBack(byte b) {
    prevByte = b;
    havePrevByte = true;
  }
  
  /**
   * Fill our buffer with data at the current file pointer.
   */
  private void fillBuffer() throws IOException 
  {
    bufferFilePointer = (int) baseFile.getFilePointer();
    bufferLength = baseFile.read(buffer);
    bufferPos = 0;
  }
  
  @Override
  public int read() throws IOException 
  {
    if (havePrevByte) {
      havePrevByte = false;
      return prevByte & 0xff;
    }
    if (bufferPos >= bufferLength) {
      fillBuffer();
      if (bufferPos >= bufferLength)
        return -1;
    }
    return buffer[bufferPos++] & 0xff;
  }
  
  @Override
  public int read(byte[] b, int off, int len) throws IOException 
  {
    int origLen = len;
    
    // If there was a pushback, use it.
    if (havePrevByte && len > 0) {
      havePrevByte = false;
      b[off] = prevByte;
      ++off;
      --len;
    }
    
    // Copy as much as we can from the buffer.
    int toCopy = Math.min(bufferLength - bufferPos, len);
    if (toCopy > 0) {
      System.arraycopy(buffer, bufferPos, b, off, toCopy);
      bufferPos += toCopy;
      off += toCopy;
      len -= toCopy;
    }
    
    // For anything remaining, get it straight from the file.
    if (len > 0) {
      int nRead = baseFile.read(b, off, len);
      off += nRead;
      len -= nRead;
    }
    
    // And let the caller know how much we were able to read.
    return origLen - len;
  }
  
  @Override
  public int skipBytes(int n) throws IOException 
  {
    int origN = n;

    // Eat the 'back' byte
    if (havePrevByte && n > 0) {
      havePrevByte = false;
      --n;
    }
    
    // Skip in the buffer if we can
    int nBufSkip = Math.min(bufferLength - bufferPos, n);
    if (nBufSkip > 0) {
      bufferPos += nBufSkip;
      n -= nBufSkip;
    }
    
    // Skip by seeking if we must
    if (n > 0) {
      baseFile.seek(baseFile.getFilePointer() + n);
      n = 0;
    }
    
    return Math.max(0, origN - n);
  }
  
  @Override
  public void reOpen() throws IOException {
    if (filename != null && baseFile == null)
      baseFile = new RandomAccessFile(filename, "r");
    seek(0);
  }

  @Override
  protected void insureOpen() throws IOException {
    if (filename != null && baseFile == null) {
        reOpen();
    }
  }
  
  @Override
  public boolean isOpen() {
    return (baseFile != null);
  }
  
  @Override
  public void close() throws IOException {
    havePrevByte = false;
    if (baseFile != null) {
      baseFile.close();
      baseFile = null;
    }
    super.close();
  }
  
  @Override
  public void setStartOffset(int off) {
    startOffset = off;
  }
  
  @Override
  public int getStartOffset() {
    return startOffset;
  }
  
  @Override
  public int length() throws IOException {
    insureOpen();
    return (int) baseFile.length() - startOffset;
  }
  
  @Override
  public void seek(int pos) throws IOException 
  {
    insureOpen();
    havePrevByte = false;
    if (pos >= bufferFilePointer && (pos - bufferFilePointer) < bufferLength) {
      bufferPos = pos - bufferFilePointer;
      return;
    }
    baseFile.seek(pos + startOffset);
    bufferLength = bufferPos = 0;
    bufferFilePointer = pos;
  }
  
  @Override
  public void seek(long pos) throws IOException {
    seek((int)pos);
  }
  
  @Override
  public int getFilePointer() throws IOException {
    insureOpen();
    return bufferFilePointer + bufferPos - startOffset - (havePrevByte ? 1 : 0);
  }
  
  @Override
  public java.nio.ByteBuffer getNioByteBuffer() throws IOException { 
    throw new RuntimeException("Not supported");
  }
}