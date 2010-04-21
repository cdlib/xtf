package org.cdlib.xtf.textEngine;


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
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.ScoreDocComparator;
import org.apache.lucene.search.SortComparatorSource;
import org.apache.lucene.search.SortField;

/*
 * Used to sort DocHits in order of number of totalHits.
 */
public class TotalHitsComparator implements SortComparatorSource 
{
  /** Make a comparator for the given field using the given reader */
  public ScoreDocComparator newComparator(IndexReader reader, String fieldName)
    throws IOException 
  {
    return new HitsComp();
  } // newComparator()

  private class HitsComp implements ScoreDocComparator 
  {
    /**
     * Compares two ScoreDoc objects and returns a result indicating their
     * sort order.
     * @param d1 First ScoreDoc
     * @param d2 Second ScoreDoc
     * @return <code>-1</code> if <code>i</code> should come before <code>j</code><br><code>1</code> if <code>i</code> should come after <code>j</code><br><code>0</code> if they are equal
     * @see java.util.Comparator
     */
    public int compare(ScoreDoc d1, ScoreDoc d2) 
    {
      assert d1 instanceof DocHit;
      assert d2 instanceof DocHit;
      
      int o1 = ((DocHit)d1).totalSnippets();
      int o2 = ((DocHit)d2).totalSnippets();
      
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
     * @param doc The document
     */
    public Comparable sortValue(ScoreDoc doc) {
      assert doc instanceof DocHit;
      return new Integer(((DocHit)doc).totalSnippets());
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
  }
}
