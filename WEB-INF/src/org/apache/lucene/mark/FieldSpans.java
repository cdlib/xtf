package org.apache.lucene.mark;

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

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Set;

/**
 * Keeps a record of the matching spans and search terms for each field.
 *
 * <p>Created: Dec 6, 2004</p>
 *
 * @author  Martin Haye
 * @version $Id: FieldSpans.java,v 1.4 2005-07-04 18:49:43 mhaye Exp $
 */
public class FieldSpans
{
  /** One {@link Entry} per field */
  private HashMap entries = new HashMap();

  /**
   * Record matching spans for a given field.
   * 
   * @param field     field that was matched
   * @param spanTotal total number of matching spans (which might be more
   *                  than we're recording if some low-scoring ones dropped
   *                  off the bottom.)
   * @param spans     matching spans
   * @param terms     set of all search terms on this field
   */
  public void recordSpans(String field, int spanTotal, 
                          ArrayList spans, Set terms) 
  {
    entries.put(field, new Entry(spanTotal, spans, terms)); 
  }
  
  /**
   * Add matching spans for one or more fields.
   *
   * @param other     set of matching spans
   */
  public void addSpans(FieldSpans other)
  {
    entries.putAll(other.entries);
  }
  
  /** Get a set of all the field names */
  public Set getFields() {
    return entries.keySet();
  }
  
  /** Retrieve the total number of spans which matched the field */
  public int getSpanTotal(String field) {
    Entry ent = (Entry)entries.get(field);
    return (ent == null) ? 0 : ent.total;
  }
  
  /** Retrieve the number of spans stored for a given field */
  public int getSpanCount(String field) {
    Entry ent = (Entry)entries.get(field);
    return (ent == null) ? 0 : ent.spans.size();
  }
  
  /** Retrieve the matching spans for a given field */
  public ArrayList getSpans(String field) { 
    Entry ent = (Entry)entries.get(field);
    return (ent == null) ? null : ent.spans;
  }
  
  /** Retrieve the set of search terms for a given field */
  public Set getTerms(String field) {
    Entry ent = (Entry)entries.get(field);
    return (ent == null) ? null : ent.terms;
  }    
  
  /** true if no spans have yet been stored */
  public boolean isEmpty() {
    return entries.isEmpty();
  }
  
  /** Stores all the information for a field */
  private class Entry {
    int total;
    ArrayList spans;
    Set terms;
    
    Entry(int total, ArrayList spans, Set terms) {
      this.total = total;
      this.spans = spans;
      this.terms = terms;
    }
  }
}
