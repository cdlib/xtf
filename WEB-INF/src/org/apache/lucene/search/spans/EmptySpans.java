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

import java.io.IOException;
import java.util.Set;

/** 
 * Expert: An empty list of spans, suitable for ORing with other lists.
 * 
 * @author  Martin Haye
 * @version $Id: EmptySpans.java,v 1.1 2005-02-08 23:20:39 mhaye Exp $
 */
public class EmptySpans implements Spans {
  /** Static instance; there's no need to ever create a new EmptySpans() */
  public static EmptySpans theInstance = new EmptySpans();
  
  /** Don't create a new instance... use {@link #theInstance}. */
  private EmptySpans() { }
  
  public boolean next() throws IOException { return false; }

  public boolean skipTo(int target) throws IOException { return false; }

  public int doc() { throw new UnsupportedOperationException(); }

  public int start() { throw new UnsupportedOperationException(); }

  public int end() { throw new UnsupportedOperationException(); }
  
  public float score() { throw new UnsupportedOperationException(); }
  
  public void collectTerms(Set terms) { }
}
