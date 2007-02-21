package org.apache.lucene.limit;


/**
 * Copyright 2005 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.io.IOException;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermPositions;

/**
 * Used by LimIndexReader to help enforce the work limit while processing a
 * query.
 *
 * @author Martin Haye
 */
public class LimTermPositions implements TermPositions 
{
  private LimIndexReader reader;
  private TermPositions wrapped;

  /** Create a new wrapper around a TermPositions */
  public LimTermPositions(LimIndexReader reader, TermPositions toWrap) {
    this.wrapped = toWrap;
    this.reader = reader;
  }

  /*************************************************************************
   * DELEGATED METHODS THAT PERFORM "WORK"
   *************************************************************************/
  public int nextPosition()
    throws IOException 
  {
    reader.work(1);
    return wrapped.nextPosition();
  }

  public int read(int[] docs, int[] freqs)
    throws IOException 
  {
    int nRead = wrapped.read(docs, freqs);
    reader.work(nRead);
    return nRead;
  }

  public void seek(Term term)
    throws IOException 
  {
    reader.work(1);
    wrapped.seek(term);
  }

  public void seek(TermEnum termEnum)
    throws IOException 
  {
    reader.work(1);
    wrapped.seek(termEnum);
  }

  public boolean skipTo(int target)
    throws IOException 
  {
    reader.work(1);
    return wrapped.skipTo(target);
  }

  /*************************************************************************
   * OTHER DELEGATED METHODS
   *************************************************************************/
  public void close()
    throws IOException 
  {
    wrapped.close();
  }

  public int doc() {
    return wrapped.doc();
  }

  public boolean equals(Object obj) {
    return wrapped.equals(obj);
  }

  public int freq() {
    return wrapped.freq();
  }

  public int hashCode() {
    return wrapped.hashCode();
  }

  public boolean next()
    throws IOException 
  {
    return wrapped.next();
  }

  public String toString() {
    return wrapped.toString();
  }
} // class LimTermPositions
