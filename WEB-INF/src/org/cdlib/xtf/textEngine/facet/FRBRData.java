package org.cdlib.xtf.textEngine.facet;

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
 */

import java.io.IOException;
import java.util.HashMap;
import java.util.WeakHashMap;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermPositions;
import org.cdlib.xtf.util.IntMultiMap;
import org.cdlib.xtf.util.TagArray;

/**
 * Keeps a large in-memory table of the title, author, and other info for each
 * document.
 * 
 * @author Martin Haye
 */
public class FRBRData
{
  /** Cached data. If the reader goes away, our cache will too. */
  private static WeakHashMap cache = new WeakHashMap();

  public static final int TYPE_TITLE  = 1;
  public static final int TYPE_AUTHOR = 2;
  public static final int TYPE_DATE   = 3;
  public static final int TYPE_ID     = 4;

  public final TagArray    tags;
  public final IntMultiMap docTags;
  public final IntMultiMap tagDocs;

  /**
   * Retrieves tags for a given set of fields from a given reader. Maintains a cache
   * so that if the same fields are requested again for this reader, we don't have
   * to re-read the tags.
   * 
   * @param reader  Where to read the tags from
   * @param fields  Which fields to read
   * @return        FRBR tags for the specified fields
   */
  public static FRBRData getCachedTags(IndexReader reader, String[] fields)
      throws IOException
  {
    // See if we have a cache for this reader.
    HashMap readerCache = (HashMap) cache.get(reader);
    if (readerCache == null) {
      readerCache = new HashMap();
      cache.put(reader, readerCache);
    }

    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < fields.length; i++)
      buf.append(fields[i] + "|");
    String allFields = buf.toString();

    // Now see if we've already read data for this set of fields.
    FRBRData tags = (FRBRData) readerCache.get(allFields);
    if (tags == null) {
      // Don't have cached data, so read and remember it.
      tags = new FRBRData(reader, fields);
      readerCache.put(allFields, tags);
    }

    return tags;

  } // getCachedTags()

  /**
   * Read tags for a given set of fields from the given reader. Do not construct
   * directly, but rather use {@link #getCachedTags(IndexReader, String[])}.
   */
  private FRBRData(IndexReader reader, String[] fields) throws IOException
  {
    // First, allocate the tag array and all our types
    tags = new TagArray();

    // Add all our types
    int tt;
    tt = tags.findType("title");  assert tt == TYPE_TITLE;
    tt = tags.findType("author"); assert tt == TYPE_AUTHOR;
    tt = tags.findType("date");   assert tt == TYPE_DATE;
    tt = tags.findType("id");     assert tt == TYPE_ID;   
    
    // Next, allocate the mapping from document to tag.
    int maxDoc = reader.maxDoc();
    docTags = new IntMultiMap(maxDoc);

    // Read in each field.
    for (int i = 0; i < fields.length; i++) {
      String field = fields[i];

      // Identify the type
      int type = calcType(field);

      // Read all the data, and add it to the map.
      readField(reader, field, type);
    } // for each field

    // Now construct the inverse mapping, from tag to document.
    tagDocs = new IntMultiMap(tags.size());
    for (int doc = 0; doc < maxDoc; doc++) {
      for (int link = docTags.firstPos(doc); link >= 0; link = docTags.nextPos(link))
        tagDocs.add(docTags.getValue(link), doc);
    }
  } // constructor

  /**
   * Read all the term->document mappings from a given field, and add them to
   * the tag array, and docTags mapping.
   */
  private void readField(IndexReader reader, String field, int type)
      throws IOException
  {
    TermPositions termPositions = reader.termPositions();
    TermEnum termEnum = reader.terms(new Term(field, ""));

    try {
      if (termEnum.term() == null)
        throw new IOException("no terms in field " + field);

      do {
        Term term = termEnum.term();
        if (!term.field().equals(field))
          break;

        // Add a tag for this term.
        int tag = tags.add(term.text(), type);

        // Now process each document which contains this term.
        termPositions.seek(termEnum);
        while (termPositions.next())
          docTags.add(termPositions.doc(), tag);
      } while (termEnum.next());
    }
    finally {
      termPositions.close();
      termEnum.close();
    }
  } // readField()

  /**
   * Calculate the type of a given field, based on the field name.
   */
  private int calcType(String field) throws IOException
  {
    if (field.contains("title"))
      return TYPE_TITLE;
    else if (field.contains("author") || field.contains("creator"))
      return TYPE_AUTHOR;
    else if (field.contains("date") || field.contains("year"))
      return TYPE_DATE;
    else if (field.contains("id") || field.contains("ID"))
      return TYPE_ID;
    else
      throw new IOException("Unable to identify field type: '" + field + "'");
  }

} // class FRBRTags
