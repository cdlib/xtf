package org.cdlib.xtf.textEngine.sort;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.ScoreDocComparator;

import java.io.IOException;
import java.io.Serializable;

/**
 * Expert: returns a comparator for sorting ScoreDocs.
 *
 * <p>Created: Apr 21, 2004 3:49:28 PM
 * 
 * @author  Tim Jones
 * @version $Id: SortComparatorSource.java,v 1.1.1.1 2004-10-08 19:53:11 mhaye Exp $
 * @since   1.4
 */
public interface SortComparatorSource
extends Serializable {

  /**
   * Creates a comparator for the field in the given index.
   * @param reader Index to create comparator for.
   * @param fieldname  Field to create comparator for.
   * @return Comparator of ScoreDoc objects.
   * @throws IOException If an error occurs reading the index.
   */
  ScoreDocComparator newComparator (IndexReader reader, String fieldname)
  throws IOException;
}