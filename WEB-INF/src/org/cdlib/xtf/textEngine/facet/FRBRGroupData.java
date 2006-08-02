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
import java.util.StringTokenizer;

import org.apache.lucene.index.IndexReader;
import org.cdlib.xtf.util.FloatList;
import org.cdlib.xtf.util.IntList;
import org.cdlib.xtf.util.StringList;
import org.cdlib.xtf.util.Trace;

/**
 * Implements a dynamic mapping from document to a FRBR-style title/author key.
 * 
 * @author Martin Haye
 */
public class FRBRGroupData extends DynamicGroupData
{
  /** Original parameter string */
  private String params;

  /** Tag/doc data for the specified fields */
  private FRBRData data;

  /** IDs of matching documents */
  private IntList docs = new IntList();
  
  /** Highest doc ID encountered */
  private int maxDoc = 0;

  /** Score of each matching document */
  private FloatList docScores = new FloatList();

  /** Mapping of documents to groups */
  private IntList docGroups;
  
  /** First document in each group (for sorting purposes) */
  private IntList groupDocs;
  
  /** Number of documents in each group */
  private IntList groupDocCounts;
  
  /** Score of each group */
  private FloatList groupScores;

  /** Number of groups created so far */
  private int nGroups = 1; // group 0 is always the root
  
  /** Primary field to sort by */
  private int primarySort = FRBRData.TYPE_TITLE;

  /**
   * Read in the FRBR data for the a delimited list of fields.
   */
  public void init(IndexReader indexReader, String params) throws IOException
  {
    // Record the input
    this.params = params;
    
    // Break the string of parameters into a list of fields.
    StringTokenizer t = new StringTokenizer(params, " \t,;|");
    StringList fields = new StringList(t.countTokens());
    while (t.hasMoreTokens()) {
      String tok = t.nextToken();
      if (tok.startsWith("[")) {
        if (tok.equals("[sort=title]"))
          primarySort = FRBRData.TYPE_TITLE;
        else if (tok.equals("[sort=author]"))
          primarySort = FRBRData.TYPE_AUTHOR;
        else if (tok.equals("[sort=date]"))
          primarySort = FRBRData.TYPE_DATE;
        else if (tok.equals("[sort=id]"))
          primarySort = FRBRData.TYPE_ID;
        else
          throw new RuntimeException( "Unknown control marker: " + tok );
      }
      else
        fields.add(tok);
    }

    // And fetch the doc/tag data for those fields.
    data = FRBRData.getCachedTags(indexReader, fields.toArray());
  }

  /**
   * Add a document (that matched the query) to our data.
   */
  public void collect(int doc, float score)
  {
    assert docs.isEmpty() || docs.getLast() < doc : "docs out of order";
    docs.add(doc);
    docScores.add(score);
    maxDoc = Math.max(maxDoc, doc);
  } // collect()

  /**
   * Form the final FRBR groups for the document set.
   */
  public void finish()
  {
    Trace.debug("Building FRBR groups for " + docs.size() + " docs...");
    Trace.tab();
    
    // Save space in the document and score lists.
    docs.compact();
    docScores.compact();

    // Figure out a group for each document.
    docGroups = new IntList(maxDoc+1);
    docGroups.fill(-1);
    for (int i = 0; i < docs.size(); i++) 
    {
      int doc = docs.get(i);
      
      // Skip docs that already have a group assigned.
      if (docGroups.get(doc) >= 0)
        continue;
      
      // Go looking...
      findGroup(doc);
    }
    
    Trace.debug(nGroups + " groups. Inverting map...");
    
    // Form the count and score lists.
    groupDocs = new IntList(nGroups);
    groupDocCounts = new IntList(nGroups);
    groupScores = new FloatList(nGroups);
    for (int i = 0; i < docs.size(); i++ ) {
      int doc = docs.get(i);
      float score = docScores.get(i);
      int group = docGroups.get(doc);
      assert group >= 0 : "group should have been assigned";

      if (groupDocs.get(group) == 0)
        groupDocs.set(group, doc);
      
      groupDocCounts.set(group, groupDocCounts.get(group) + 1);
      
      groupScores.set(group, Math.max(groupScores.get(group), score));
      groupScores.set(0, Math.max(groupScores.get(0), score));
    }
    groupDocCounts.set(0, docs.size());
    
    Trace.debug("Done.");
    Trace.untab();
    
  } // finish()

  /**
   * Figure out a group to put the document in. If it matches other documents,
   * the group will contain all of them; otherwise, it'll be a singleton.
   * 
   * @param mainDoc     Document to put into a group
   */
  private void findGroup(int mainDoc)
  {
    // This document will be its own group, but hopefully we can add more
    // documents to that group.
    //
    docGroups.set(mainDoc, nGroups++);
    
    // Our starting point is the title(s) of the current document.
    for (int pos = data.docTags.firstPos(mainDoc); pos >= 0; pos = data.docTags.nextPos(pos))
    {
      int mainTitle = data.docTags.getValue(pos);
      if (data.tags.getType(mainTitle) != FRBRData.TYPE_TITLE)
        continue;
      
      // Scan forward looking for matching titles. Do compare the main title,
      // since other documents may match that title exactly.
      //
      int compTitle = mainTitle;
      while (compTitle >= 0) {
        if (!matchOnTitle(mainDoc, mainTitle, compTitle))
          break;
        compTitle = data.tags.next(compTitle);
      }

      // Scan backward through the titles in like manner.
      compTitle = data.tags.prev(mainTitle);
      while (compTitle >= 0) {
        if (!matchOnTitle(mainDoc, mainTitle, compTitle))
          break;
        compTitle = data.tags.prev(compTitle);
      }
    } // for title
  } // findGroup()
  
  /**
   * Determines if the two titles match enough to warrant further examination,
   * and if so, continues the matching process on documents from the 
   * comparable title.
   * 
   * @param mainDoc           main document being matched
   * @param mainTitle     main doc's title tag
   * @param compTitle     title tag to compare
   * @return              true if title iteration should continue.
   */
  private boolean matchOnTitle( int mainDoc, 
                                int mainTitle, 
                                int compTitle )
  {
    // Compare the titles for a match.
    int titleScore = scoreTitleMatch(mainTitle, compTitle);
    
    // If no match, stop the title iteration.
    if (titleScore < 50)
      return false;
    
    // Okay, iterate all the documents that match on title (except the main
    // doc which of course matches itself.)
    //
    for (int pos = data.tagDocs.firstPos(compTitle); pos >= 0; pos = data.tagDocs.nextPos(pos))
    {
      int compDoc = data.tagDocs.getValue(pos);
      if (compDoc == mainDoc)
        continue;
      
      // If the document isn't in our query set, skip it.
      if (docs.binarySearch(compDoc) < 0)
        continue;
      
      // If it's already in a group, skip it (hopefully this is rare)
      if (docGroups.get(compDoc) >= 0) {
        if (docGroups.get(compDoc) != docGroups.get(mainDoc)) {
          // hopefully rare
        }
        continue;
      }
      
      // See if it's close enough to call it a match.
      if( !multiFieldMatch(mainDoc, compDoc) )
        continue;
      
      // Okay, we got a live one. Put it in the same group as the main doc.
      int group = docGroups.get(mainDoc);
      docGroups.set(compDoc, group);
    }
    
    // Continue title iteration, since the title matched (even if no docs 
    // matched).
    //
    return true;

  } // matchOnTitle()
  
  /**
   * Compare the fields of two documents to determine if they should be in
   * the same FRBR group.
   * 
   * @param doc1     First document
   * @param doc2     Second document
   * @return            true if they're equivalent
   */
  private boolean multiFieldMatch(int doc1, int doc2)
  {
    int titleScore = 0;
    int authorScore = 0;
    int dateScore = 0;
    int idScore = 0;
    
    // Compare each field to each other field. We assume that there are not
    // very many, so that this isn't terribly inefficient.
    //
    for (int p1 = data.docTags.firstPos(doc1); p1 >= 0; p1 = data.docTags.nextPos(p1)) {
      int tag1  = data.docTags.getValue(p1);
      int type1 = data.tags.getType(tag1);
      
      for (int p2 = data.docTags.firstPos(doc2); p2 >= 0; p2 = data.docTags.nextPos(p2)) {
        int tag2  = data.docTags.getValue(p2);
        int type2 = data.tags.getType(tag2);
        
        if (type1 != type2)
          continue;
        
        switch (type1) {
          case FRBRData.TYPE_TITLE:
            titleScore = Math.max(titleScore, scoreTitleMatch(tag1, tag2));
            break;
          case FRBRData.TYPE_AUTHOR:
            authorScore = Math.max(authorScore, scoreAuthorMatch(tag1, tag2));
            break;
          case FRBRData.TYPE_DATE:
            dateScore = Math.max(dateScore, scoreDateMatch(tag1, tag2));
            break;
          case FRBRData.TYPE_ID:
            idScore = Math.max(idScore, scoreIdMatch(tag1, tag2));
            break;
        } // switch
      } // for p2
    } // for p1
    
    // Is the total score high enough?
    int totalScore = titleScore + authorScore + dateScore + idScore;
    return totalScore >= 200;
  } // multiFieldMatch()

  /**
   * Compare two titles, and return a match score 0..100
   */
  private int scoreTitleMatch(int tag1, int tag2)
  {
    // If they're exactly equal, great.
    if (tag1 == tag2)
      return 100;
    
    // For now, fail on not equal.
    return 0;
    
    /*
    // Don't do prefix scanning on short titles.
    String str1 = data.tags.getString(tag1);
    String str2 = data.tags.getString(tag2);
    if (str1.length() < 5 || str2.length() < 5)
      return 0;
    
    // If either is a prefix of the other, we have a match.
    if (str1.startsWith(str2) || str2.startsWith(str1))
      return 100;
    
    // All other cases: fail for now at least.
    return 0;
    */
  } // scoreTitleMatch

  /**
   * Compare two authors, and return a match score 0..100
   */
  private int scoreAuthorMatch(int tag1, int tag2)
  {
    // If they're exactly equal, great.
    if (tag1 == tag2)
      return 100;
    
    // For now, fail on not equal.
    return 0;
    
    /*
    // Don't do prefix scanning on short titles.
    String str1 = data.tags.getString(tag1);
    String str2 = data.tags.getString(tag2);
    if (str1.length() < 5 || str2.length() < 5)
      return 0;
    
    // If either is a prefix of the other, we have a match.
    if (str1.startsWith(str2) || str2.startsWith(str1))
      return 100;
    
    // All other cases: fail for now at least.
    return 0;
    */
  } // scoreAuthorMatch

  /**
   * Compare two dates, and return a match score 0..100
   */
  private int scoreDateMatch(int tag1, int tag2)
  {
    // If they're exactly equal, great.
    if (tag1 == tag2)
      return 50;
    
    // For now, fail on not equal.
    return 0;
    
    /*
    // Parse the years
    String str1 = data.tags.getString(tag1);
    String str2 = data.tags.getString(tag2);
    int year1 = -99;
    int year2 = -99;
    try { year1 = Integer.parseInt(str1); }
    catch (NumberFormatException e) { }
    try { year2 = Integer.parseInt(str2); }
    catch (NumberFormatException e) { }

    // If either is missing, no match.
    if (year1 < 0 || year2 < 0)
      return 0;
    
    // If within 2 years, consider that a partial match.
    if (Math.abs(year1 - year2) <= 2)
      return 25;
    
    // All other cases: fail for now at least.
    return 0;
    */
  } // scoreDateMatch

  /**
   * Compare two identifiers, and return a match score 0..100
   */
  private int scoreIdMatch(int tag1, int tag2)
  {
    // If they're exactly equal, great.
    if (tag1 == tag2)
      return 100;

    // Fail for now.
    return 0;
  } // scoreIdMatch

  /**
   * Get the field name (synthetic in our case)
   */
  public String field() {
    return "dynamicFRBR";
  }

  // inherit JavaDoc
  public String name(int groupId) {
    return "group-" + groupId;
  }

  // inherit JavaDoc
  public int findGroup(String name)
  {
    if (!name.startsWith("group-"))
      return -1;
    return Integer.parseInt(name.substring("group-".length()));
  }

  // inherit JavaDoc
  public int child(int groupId) {
    return (groupId == 0 && nGroups > 1) ? 1 : -1;
  }

  // inherit JavaDoc
  public int sibling(int groupId) {
    return (groupId == 0 || groupId == nGroups-1) ? -1 : (groupId+1);
  }

  // inherit JavaDoc
  public int parent(int groupId) {
    return (groupId == 0) ? -1 : 0;
  }

  // inherit JavaDoc
  public int nChildren(int groupId) {
    return (groupId == 0) ? (nGroups-1) : 0;
  }

  // inherit JavaDoc
  public int firstLink(int docId) {
    return docGroups.get(docId);
  }

  // inherit JavaDoc
  public int nextLink(int linkId) {
    return -1;
  }
  
  // inherit JavaDoc
  public int linkGroup(int linkId) {
    return linkId;
  }

  // inherit JavaDoc
  public int nGroups() {
    return nGroups;
  }

  // inherit JavaDoc
  public boolean isDynamic() {
    return true;
  }

  // inherit JavaDoc
  public int nDocHits(int groupId) {
    return groupDocCounts.get(groupId);
  }

  // inherit JavaDoc
  public float score(int groupId) {
    return groupScores.get(groupId);
  }
  
  // inherit JavaDoc
  public final int compare(int group1, int group2) 
  {
    // Are they exactly equal?
    if (group1 == group2)
      return 0;
    
    // Get the first document in each group.
    int doc1 = groupDocs.get(group1);
    int doc2 = groupDocs.get(group2);
    
    // First, compare the primary field.
    int x;
    if ((x=compareField(primarySort, doc1, doc2)) != 0) 
      return x;
    
    // Now compare the secondary fields, in order.
    for (int t = FRBRData.FIRST_TYPE; t <= FRBRData.LAST_TYPE; ++t) {
      if (t != primarySort && (x=compareField(t, doc1, doc2)) != 0) 
        return x;
    }
    
    // No differences found.
    return 0;
  }
  
  /** Compare a particular field of two groups */
  private int compareField(int type, int doc1, int doc2) 
  {
    // Locate this field in the first doc.
    int tag1 = 0;
    for (int pos = data.docTags.firstPos(doc1); pos >= 0 && tag1 == 0; pos = data.docTags.nextPos(pos)) {
      int tag = data.docTags.getValue(pos);
      if (data.tags.getType(tag) == type)
        tag1 = tag;
    }

    // ... and locate it in the second doc.
    int tag2 = 0;
    for (int pos = data.docTags.firstPos(doc2); pos >= 0 && tag2 == 0; pos = data.docTags.nextPos(pos)) {
      int tag = data.docTags.getValue(pos);
      if (data.tags.getType(tag) == type)
        tag2 = tag;
    }
    
    // Make sure docs that don't have an entry sort at the end, not the beginning.
    if (tag1 == 0)
      tag1 = Integer.MAX_VALUE;
    if (tag2 == 0)
      tag2 = Integer.MAX_VALUE;
    
    // Now a simple numerical comparison on the tags will do.
    return (tag1 < tag2) ? -1 : ((tag1 > tag2) ? 1 : 0);
  } // compareField
  
} // class FRBRGroupData
