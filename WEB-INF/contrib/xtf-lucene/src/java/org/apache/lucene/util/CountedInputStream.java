package org.apache.lucene.util;

/*
 * Copyright 2006-2007 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Wraps an InputStream, and counts how many bytes have been read from it.
 *
 * @author Martin Haye
 */
public class CountedInputStream extends FilterInputStream 
{
  /** Count of the number of bytes read from the stream so far */
  private long nRead = 0;

  /** Wrap an input stream */
  public CountedInputStream(InputStream in) {
    super(in);
  }

  /** Find out how many bytes have been read so far */
  public long nRead() {
    return nRead;
  }

  // inherit JavaDoc
  public int read()
    throws IOException 
  {
    int retVal = in.read();
    if (retVal >= 0)
      ++nRead;
    return retVal;
  }

  // inherit JavaDoc
  public int read(byte[] b, int off, int len)
    throws IOException 
  {
    int retVal = in.read(b, off, len);
    if (retVal >= 0)
      nRead += retVal;
    return retVal;
  }

  // inherit JavaDoc
  public long skip(long n)
    throws IOException 
  {
    long retVal = in.skip(n);
    if (retVal >= 0)
      nRead += retVal;
    return retVal;
  }

  // inherit JavaDoc
  public boolean markSupported() {
    return false; // we don't want to worry about marking the number of bytes
  }
} // class CountedInputStream
