package org.apache.lucene.search;

/**
 * Copyright 2004 The Apache Software Foundation
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;

/*
 * Similar to a normal string comparator, except that it is optimized for
 * the case where most documents (or chunks, in our case) will be null.
 * Thus, instead of a huge array indexed by document ID, we keep an array
 * list in ID order and binary search it.
 *
 * @author Martin Haye
 */
public class SparseStringComparator implements SortComparatorSource {
  private static final WeakHashMap cache = new WeakHashMap();

  private static final EntryComparator entryComparator = new EntryComparator();

  /** Make a comparator for the given field using the given reader */
  public ScoreDocComparator newComparator(IndexReader reader, String fieldName)
      throws IOException {
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
    SparseComp comp = (SparseComp) readerCache.get(fieldName);
    if (comp == null) {
      comp = new SparseComp(reader, fieldName);
      readerCache.put(fieldName, comp);
    }

    // Return the resulting comparator.
    return comp;

  } // newComparator()

  private class SparseComp implements ScoreDocComparator {
    ArrayList entries = new ArrayList(500);

    SparseComp(IndexReader reader, String field) throws IOException {
      TermDocs termDocs = reader.termDocs();
      TermEnum termEnum = reader.terms(new Term(field, ""));
      int t = 0; // current term number

      // Make an entry for each document and each term. Ensure that
      // there is only one term in this field per document.
      //
      HashMap docs = new HashMap();
      try {
        if (termEnum.term() == null)
          throw new RuntimeException("no terms in field " + field);

        do {
          Term term = termEnum.term();
          if (term.field() != field)
            break;

          String termText = term.text();

          termDocs.seek(termEnum);
          while (termDocs.next()) {
            int docId = termDocs.doc();
            Integer key = new Integer(docId);
            if (docs.get(key) != null) {
              throw new RuntimeException("A document has more than one term ('"
                  + termText + "', '" + (String) docs.get(key) + "') in field "
                  + field);
            }
            docs.put(key, termText);

            Entry ent = new Entry();
            ent.docId = termDocs.doc();
            ent.termText = termText;
            ent.order = t;

            entries.add(ent);
          }

          t++;
        } while (termEnum.next());
      } finally {
        termDocs.close();
        termEnum.close();
      }

      // Now sort the array by document ID.
      Collections.sort(entries, entryComparator);

    } // constructor

    /** Retrieve the entry for a given document, or null if not found. Uses
     *  an efficient binary search over the array. */
    private Entry findEntry(int docId) {
      Entry toFind = new Entry();
      toFind.docId = docId;

      int index = Collections.binarySearch(entries, toFind, entryComparator);
      if (index < 0 || index >= entries.size())
        return null;

      Entry got = (Entry) entries.get(index);
      if (got.docId != docId)
        return null;

      return got;
    } // findEntry()

    /**
     * Compares two ScoreDoc objects and returns a result indicating their
     * sort order.
     * @param d1 First ScoreDoc
     * @param d2 Second ScoreDoc
     * @return <code>-1</code> if <code>i</code> should come before <code>j</code><br><code>1</code> if <code>i</code> should come after <code>j</code><br><code>0</code> if they are equal
     * @see java.util.Comparator
     */
    public int compare(ScoreDoc d1, ScoreDoc d2) {
      Entry e1 = findEntry(d1.doc);
      Entry e2 = findEntry(d2.doc);

      int o1 = (e1 != null) ? e1.order : Integer.MAX_VALUE;
      int o2 = (e2 != null) ? e2.order : Integer.MAX_VALUE;

      if (o1 < o2)
        return -1;
      else if (o1 > o2)
        return 1;
      else
        return 0;
    }

    /**
     * Returns the value used to sort the given document.  The
     * object returned must implement the java.io.Serializable
     * interface.  This is used by multisearchers to determine how to collate results from their searchers.
     * @see FieldDoc
     * @param i Document
     * @return Serializable object
     */
    public Comparable sortValue(ScoreDoc i) {
      Entry ent = findEntry(i.doc);
      if (ent != null)
        return ent.termText;
      return "";
    }

    /**
     * Returns the type of sort.  Should return <code>SortField.SCORE</code>, <code>SortField.DOC</code>, <code>SortField.STRING</code>, <code>SortField.INTEGER</code>, 
     * <code>SortField.FLOAT</code> or <code>SortField.CUSTOM</code>.  It is not valid to return <code>SortField.AUTO</code>.
     * This is used by multisearchers to determine how to collate results from their searchers.
     * @return One of the constants in SortField.
     * @see SortField
     */
    public int sortType() {
      return SortField.CUSTOM;
    }

  } // class SparseComp

  /** A single entry in the sorting table */
  class Entry {
    int docId;

    String termText;

    int order;
  }

  /** Compare two entries for sorting purposes */
  static class EntryComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      int d1 = ((Entry) o1).docId;
      int d2 = ((Entry) o2).docId;
      if (d1 < d2)
        return -1;
      else if (d1 > d2)
        return 1;
      else
        return 0;
    }
  } // class EntryComparator

} // class SparseStringComparator
