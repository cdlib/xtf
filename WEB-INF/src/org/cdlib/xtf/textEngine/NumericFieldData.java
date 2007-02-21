package org.cdlib.xtf.textEngine;


/**
 * Copyright (c) 2006, Regents of the University of California
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
 *
 * Acknowledgements:
 *
 * A significant amount of new and/or modified code in this module
 * was made possible by a grant from the Andrew W. Mellon Foundation,
 * as part of the Melvyl Recommender Project.
 */
import java.io.IOException;
import java.util.HashMap;
import java.util.WeakHashMap;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.cdlib.xtf.util.IntList;
import org.cdlib.xtf.util.LongList;

/**
 * Holds numeric data for a field from a Lucene index. Data is cached for a
 * given index reader, to speed access after the initial load.
 *
 * @author Martin Haye
 */
public class NumericFieldData 
{
  /** Cached data. If the reader goes away, our cache will too. */
  private static WeakHashMap cache = new WeakHashMap();

  /** Document IDs containing values for the field */
  private IntList docs = new IntList();

  /** Associated numeric value for each document */
  private LongList values = new LongList();

  /**
   * Retrieves tags for a given field from a given reader. Maintains a cache
   * so that if the same fields are requested again for this reader, we don't have
   * to re-read the tags.
   *
   * @param reader  Where to read the tags from
   * @param field   Which field to read
   * @return        FRBR tags for the specified field
   */
  public static NumericFieldData getCachedData(IndexReader reader, String field)
    throws IOException 
  {
    // See if we have a cache for this reader.
    HashMap readerCache = (HashMap)cache.get(reader);
    if (readerCache == null) {
      readerCache = new HashMap();
      cache.put(reader, readerCache);
    }

    // Now see if we've already read data for this field.
    NumericFieldData data = (NumericFieldData)readerCache.get(field);
    if (data == null) 
    {
      // Don't have cached data, so read and remember it.
      data = new NumericFieldData(reader, field);
      readerCache.put(field, data);
    }

    return data;
  } // getCachedTags()

  /** Parse the numeric characters of a string, ignoring all non-digits */
  public static long parseVal(String str) 
  {
    long ret = 0;
    for (int i = 0; i < str.length(); i++) 
    {
      int digit = Character.digit(str.charAt(i), 10);
      if (digit >= 0) {
        ret = (ret * 10) + digit;
      }
    }
    return ret;
  }

  /**
   * Load data from the given field of the reader, and parse the values as
   * numbers.
   */
  private NumericFieldData(IndexReader reader, String field)
    throws IOException 
  {
    TermDocs termDocs = reader.termDocs();
    TermEnum termEnum = reader.terms(new Term(field, ""));

    try 
    {
      // First, collect all the doc/value pairs.
      if (termEnum.term() == null)
        throw new IOException("no terms in field " + field);

      do 
      {
        Term term = termEnum.term();
        if (term.field() != field)
          break;

        String termText = term.text();

        // Skip terms with the special XTF field markers.
        if (termText.length() > 1) {
          if (termText.charAt(0) == Constants.FIELD_START_MARKER)
            continue;
          if (termText.charAt(termText.length() - 1) == Constants.FIELD_END_MARKER)
            continue;
        }

        long value = parseVal(termText);

        termDocs.seek(termEnum);
        while (termDocs.next()) {
          int doc = termDocs.doc();
          docs.add(doc);
          values.add(value);
        }
      } while (termEnum.next());

      // Save space.
      docs.compact();
      values.compact();

      // Now sort by document ID, and apply the same ordering to the values,
      // to keep them in sync.
      //
      int[] map = docs.calcSortMap();
      docs.remap(map);
      values.remap(map);

      // Check to be sure no documents have multiple values.
      for (int i = 1; i < docs.size(); i++) 
      {
        if (docs.get(i - 1) == docs.get(i)) {
          throw new IOException(
            "A document contains more than one value in numeric field '" +
            field + "': values " + values.get(i - 1) + " and " + values.get(i));
        }
      } // for
    } // try
    finally {
      termEnum.close();
      termDocs.close();
    }
  } // constructor

  public final int size() {
    return docs.size();
  }

  public final int doc(int index) {
    return docs.get(index);
  }

  public final long value(int index) {
    return values.get(index);
  }

  public final int findDocIndex(int docId) {
    int idx = docs.binarySearch(docId);
    if (idx >= 0)
      return idx;
    else
      return -idx - 1; // from -ins - 1
  }

  public final int docPos(int docId) {
    return docs.binarySearch(docId);
  }
} // class NumericFieldData
