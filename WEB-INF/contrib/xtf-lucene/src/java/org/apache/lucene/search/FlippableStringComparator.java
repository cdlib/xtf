package org.apache.lucene.search;

/**
 * Copyright (c) 2007, Regents of the University of California
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
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.lucene.index.IndexReader;

/*
 * Similar to a normal string comparator, except that it puts empty docs (i.e.
 * docs that have no entry for the field) at the end of the sort order, unless
 * ":flipEmpty" is appended to the field name.
 */
public class FlippableStringComparator implements SortComparatorSource
{
  private static final WeakHashMap cache = new WeakHashMap();

  /** Make a comparator for the given field using the given reader */
  public ScoreDocComparator newComparator(IndexReader reader, String fieldName)
      throws IOException
  {
    // Check if we have a cache for this reader yet. If not, make one.
    Map readerCache = (Map) cache.get(reader);
    if (readerCache == null) {
      readerCache = new HashMap();
      cache.put(reader, readerCache);
    }

    // Now check if we have a comparator already for this field. If not,
    // make one.
    //
    fieldName = fieldName.intern();
    FlippableComp comp = (FlippableComp) readerCache.get(fieldName);
    if (comp == null) {
      comp = new FlippableComp(reader, fieldName);
      readerCache.put(fieldName, comp);
    }

    // Return the resulting comparator.
    return comp;
  } // newComparator()

  private class FlippableComp implements ScoreDocComparator
  {
    boolean flipEmpty = false;
    FieldCache.StringIndex index;

    FlippableComp(IndexReader reader, String field) throws IOException
    {
      // Grab the flipEmpty modifier if present
      if (field.endsWith(":flipEmpty")) {
        flipEmpty = true;
        field = field.replace(":flipEmpty", "");
      }
      field = field.intern();

      // Grab the string index for this field.
      index = FieldCache.DEFAULT.getStringIndex (reader, field);
    } // constructor

    // inherit JavaDoc
    public final int compare (final ScoreDoc i, final ScoreDoc j) {
      int fi = index.order[i.doc];
      int fj = index.order[j.doc];
      
      if (!flipEmpty) {
        if (fi == 0) fi = Integer.MAX_VALUE;
        if (fj == 0) fj = Integer.MAX_VALUE;
      }
      
      if (fi < fj) return -1;
      if (fi > fj) return 1;
      return 0;
    }

    // inherit JavaDoc
    public Comparable sortValue (final ScoreDoc i) {
      return index.lookup[index.order[i.doc]];
    }

    // inherit JavaDoc
    public int sortType()
    {
      return SortField.CUSTOM;
    }
  } // class FlippableComp
} // class FlippableStringComparator
