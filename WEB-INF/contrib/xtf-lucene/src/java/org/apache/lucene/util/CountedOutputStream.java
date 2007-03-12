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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Wraps an OutputStream, and counts how many bytes have been written to it.
 *
 * @author Martin Haye
 */
public class CountedOutputStream extends FilterOutputStream 
{
  /** Count of the number of bytes written to the stream so far */
  private long nWritten = 0;

  /** Wrap an output stream */
  public CountedOutputStream(OutputStream out) {
    super(out);
  }

  /** Find out how many bytes have been written so far */
  public long nWritten() {
    return nWritten;
  }

  // inherit JavaDoc
  public void write(byte[] b, int off, int len)
    throws IOException 
  {
    out.write(b, off, len);
    nWritten += len;
  }

  // inherit JavaDoc
  public void write(int b)
    throws IOException 
  {
    out.write(b);
    nWritten++;
  }
} // class CountedOutputStream
