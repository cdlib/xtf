package org.apache.lucene.search.spans;

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

/** 
 * Data holder to keep track of a single matching span.
 * 
 * <p>Created: Dec 8, 2004</p>
 *
 * @author  Martin Haye
 * @version $Id: Span.java,v 1.1 2005-02-08 23:20:40 mhaye Exp $
 */
public class Span implements Cloneable {
  
  /** Score of the span */
  public float score;
  
  /** Rank - zero for top hit, 1 for next, etc. */
  public int   rank;
  
  /** Word position of the span start */
  public int   start;
  
  /** Word position of the span end */
  public int   end;

  /** Make an exact copy of this Span */
  public Object clone() {
    try { 
      return super.clone(); 
    } catch (CloneNotSupportedException e) { 
      return new RuntimeException(e); 
    }
  }
}