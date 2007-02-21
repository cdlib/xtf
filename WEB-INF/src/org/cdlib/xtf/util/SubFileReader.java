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
import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Reads a single sub-file within a {@link StructuredFile}. A sub-file
 * provides standard DataInput/DataOutput facilities, and takes care of
 * reading from the correct subset of the main StructuredFile.
 *
 * @author Martin Haye
 */
class SubFileReader extends SubStoreReader 
{
  /** Actual disk file to write to */
  private RandomAccessFile file;

  /** The structured file that owns this Subfile */
  private StructuredFile parent;

  /** Absolute file position for the subfile's start */
  private long segOffset;

  /** Length of this subfile */
  private long segLength;

  /** Current read position within the subfile */
  private long curPos;

  /**
   * Construct a subfile reader. Reads will be constrained to the
   * specified limit.
   *
   * @param file      Disk file to attach to
   * @param parent    Structured file to attach to
   * @param segOffset Beginning offset of the segment
   * @param segLength Length of the segment
   */
  SubFileReader(RandomAccessFile file, StructuredFile parent, long segOffset,
                long segLength)
    throws IOException 
  {
    this.file = file;
    this.parent = parent;
    this.segOffset = segOffset;
    this.segLength = segLength;
    curPos = 0;
  }

  public void close()
    throws IOException 
  {
    synchronized (parent) {
      parent.closeReader(this);
      file = null;
    }
  }

  public long getFilePointer()
    throws IOException 
  {
    return curPos;
  }

  public long length()
    throws IOException 
  {
    return segLength;
  }

  /**
   * Ensure that the sub-file has room to read the specified number of
   * bytes. As a side-effect, we also check that the main file position
   * is current for this sub-file, and if not, we save the position for
   * the other sub-file and restore ours.
   *
   * @param nBytes    Amount of space desired
   */
  private void checkLength(int nBytes)
    throws IOException 
  {
    synchronized (parent) 
    {
      if (parent.curSubFile != this) {
        file.seek(segOffset + curPos);
        parent.curSubFile = this;
      }

      if (curPos + nBytes > segLength)
        throw new EOFException("End of sub-file reached");
    }
  }

  public void read(byte[] b, int off, int len)
    throws IOException 
  {
    synchronized (parent) {
      checkLength(len);
      file.readFully(b, off, len);
      curPos += len;
    }
  }

  public void seek(long pos)
    throws IOException 
  {
    synchronized (parent) {
      if (segLength >= 0 && pos > segLength)
        throw new EOFException("Cannot seek past end of subfile");
      parent.curSubFile = this;
      file.seek(pos + segOffset);
      curPos = pos;
    }
  }

  public byte readByte()
    throws IOException 
  {
    synchronized (parent) {
      checkLength(1);
      byte ret = file.readByte();
      curPos++;
      return ret;
    }
  }

  public int readInt()
    throws IOException 
  {
    synchronized (parent) {
      checkLength(4);
      int ret = file.readInt();
      curPos += 4;
      return ret;
    }
  }
} // class Subfile
