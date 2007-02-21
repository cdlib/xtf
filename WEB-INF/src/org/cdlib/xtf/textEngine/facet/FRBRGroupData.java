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
 *
 * Acknowledgements:
 *
 * A significant amount of new and/or modified code in this module
 * was made possible by a grant from the Andrew W. Mellon Foundation,
 * as part of the Melvyl Recommender Project.
 */
import java.io.IOException;
import java.util.StringTokenizer;
import org.apache.lucene.index.IndexReader;
import org.cdlib.xtf.util.FloatList;
import org.cdlib.xtf.util.IntList;
import org.cdlib.xtf.util.Prime;
import org.cdlib.xtf.util.StringList;
import org.cdlib.xtf.util.TagChars;
import org.cdlib.xtf.util.Trace;

/**
 * Implements a dynamic mapping from document to a FRBR-style title/author key.
 *
 * @author Martin Haye
 */
public class FRBRGroupData extends DynamicGroupData 
{
  /** Original parameter string */
  @SuppressWarnings("unused")
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

  /** Whether primary sort is in reverse order */
  private boolean reversePrimarySort = false;

  /**
   * Read in the FRBR data for the a delimited list of fields.
   */
  public void init(IndexReader indexReader, String params)
    throws IOException 
  {
    // Record the input
    this.params = params;

    // Break the string of parameters into a list of fields.
    StringTokenizer t = new StringTokenizer(params, " \t,;|");
    StringList fields = new StringList(t.countTokens());
    while (t.hasMoreTokens()) 
    {
      String tok = t.nextToken();
      if (tok.startsWith("[")) 
      {
        if (tok.equals("[sort=title]"))
          primarySort = FRBRData.TYPE_TITLE;
        else if (tok.equals("[sort=author]"))
          primarySort = FRBRData.TYPE_AUTHOR;
        else if (tok.equals("[sort=date]"))
          primarySort = FRBRData.TYPE_DATE;
        else if (tok.equals("[sort=-date]")) {
          primarySort = FRBRData.TYPE_DATE;
          reversePrimarySort = true;
        }
        else if (tok.equals("[sort=id]"))
          primarySort = FRBRData.TYPE_ID;
        else
          throw new RuntimeException("Unknown control marker: " + tok);
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
  public void collect(int doc, float score) {
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
    docGroups = new IntList(maxDoc + 1);
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
    for (int i = 0; i < docs.size(); i++) 
    {
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
    for (int pos = data.docTags.firstPos(mainDoc); pos >= 0;
         pos = data.docTags.nextPos(pos)) 
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
   * @param mainDoc       main document being matched
   * @param mainTitle     main doc's title tag
   * @param compTitle     title tag to compare
   * @return              true if title iteration should continue.
   */
  private boolean matchOnTitle(int mainDoc, int mainTitle, int compTitle) 
  {
    // If they don't match exactly, check for match before colon. If that
    // doesn't match either, stop the iteration.
    ///
    if (mainTitle != compTitle && !matchPartialTitle(mainTitle, compTitle))
      return false;

    // Okay, iterate all the documents that match on title (except the main
    // doc which of course matches itself.)
    //
    for (int pos = data.tagDocs.firstPos(compTitle); pos >= 0;
         pos = data.tagDocs.nextPos(pos)) 
    {
      int compDoc = data.tagDocs.getValue(pos);
      if (compDoc == mainDoc)
        continue;

      // If the document isn't in our query set, skip it.
      if (docs.binarySearch(compDoc) < 0)
        continue;

      // If it's already in a group, skip it (hopefully this is rare)
      if (docGroups.get(compDoc) >= 0) 
      {
        if (docGroups.get(compDoc) != docGroups.get(mainDoc)) 
        {
          // hopefully rare
        }
        continue;
      }

      // See if it's close enough to call it a match.
      if (!multiFieldMatch(mainDoc, compDoc))
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

  // Instance variables to avoid re-allocation for each iteration.
  private IntList matchTags1 = new IntList();
  private IntList matchTags2 = new IntList();

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

    int p1 = data.docTags.firstPos(doc1);
    int tag1 = (p1 >= 0) ? data.docTags.getValue(p1) : -1;
    int type1 = (p1 >= 0) ? data.tags.getType(tag1) : 99;

    int p2 = data.docTags.firstPos(doc2);
    int tag2 = (p2 >= 0) ? data.docTags.getValue(p2) : -1;
    int type2 = (p2 >= 0) ? data.tags.getType(tag2) : 99;

    // Iterate through each type in turn
    while (p1 >= 0 || p2 >= 0) 
    {
      // Pick the next available type to work on.
      int curType = Math.min(type1, type2);
      assert curType != 99;

      // Collect tags from the first doc for the current type.
      matchTags1.clear();
      while (type1 == curType) {
        matchTags1.add(tag1);
        p1 = data.docTags.nextPos(p1);
        tag1 = (p1 >= 0) ? data.docTags.getValue(p1) : -1;
        type1 = (p1 >= 0) ? data.tags.getType(tag1) : 99;
      }

      // Collect tags from the second doc for the same type.
      matchTags2.clear();
      while (type2 == curType) {
        matchTags2.add(tag2);
        p2 = data.docTags.nextPos(p2);
        tag2 = (p2 >= 0) ? data.docTags.getValue(p2) : -1;
        type2 = (p2 >= 0) ? data.tags.getType(tag2) : 99;
      }

      // And calculate an appropriate score.
      switch (curType) 
      {
        case FRBRData.TYPE_TITLE:
          debugFieldMatch("title", doc1, doc2);
          titleScore = scoreTitleMatch(matchTags1, matchTags2);
          break;
        case FRBRData.TYPE_AUTHOR:
          debugFieldMatch("author", doc1, doc2);
          authorScore = scoreAuthorMatch(matchTags1, matchTags2);
          break;
        case FRBRData.TYPE_DATE:

          //debugFieldMatch("date", doc1, doc2);
          //dateScore = scoreDateMatch(matchTags1, matchTags2);
          break;
        case FRBRData.TYPE_ID:
          debugFieldMatch("id", doc1, doc2);
          idScore = scoreIdMatch(matchTags1, matchTags2);
          break;
      }
    } // while
    assert p1 < 0 && p2 < 0;

    // Is the total score high enough?
    int totalScore = titleScore + authorScore + dateScore + idScore;

    //if (compareField(FRBRData.TYPE_TITLE, doc1, doc2) != 0 && totalScore >= 150) {
    if (false) {
      outputDisplayKey("Match: ", doc1);
      outputDisplayKey("   vs: ", doc2);
      Trace.debug(
        "     = " + titleScore + "t + " + authorScore + "a + " + dateScore +
        "d + " + idScore + "i = " + totalScore);
    }

    if (totalScore < 150)
      return false;

    return true;
  }

  private void debugFieldMatch(String field, int doc1, int doc2) 
  {
    if (true || Trace.getOutputLevel() != Trace.debug)
      return;
    Trace.debug("Match " + field + ":");
    Trace.tab();

    Trace.debug("Doc " + doc1);
    Trace.tab();
    for (int i = 0; i < matchTags1.size(); i++)
      Trace.debug(
        data.tags.getString(matchTags1.get(i)) + " {tag=" + matchTags1.get(i) +
        "}");

    Trace.untab();
    Trace.debug("Doc " + doc2);
    Trace.tab();
    for (int i = 0; i < matchTags2.size(); i++)
      Trace.debug(
        data.tags.getString(matchTags2.get(i)) + " {tag=" + matchTags2.get(i) +
        "}");

    Trace.untab();
    Trace.untab();
  }

  private void outputDisplayKey(String title, int doc) 
  {
    int nToSkip = 0;
    int[] fieldMax = { 0, 50, 40, 4, 30 };
    final String spaces = "                                                             ";

    int found = 0;
    do 
    {
      StringBuffer buf = new StringBuffer();
      found = 0;
      for (int t = FRBRData.FIRST_TYPE; t <= FRBRData.LAST_TYPE; t++) 
      {
        int skipped = 0;
        String value = "";
        for (int pos = data.docTags.firstPos(doc); pos >= 0;
             pos = data.docTags.nextPos(pos)) 
        {
          int tag = data.docTags.getValue(pos);
          int type = data.tags.getType(tag);
          int subType = data.tags.getSubType(tag);

          if (type != t)
            continue;
          if (skipped++ == nToSkip) {
            value = data.tags.getString(tag) + " [" + subType + "]";
            found++;
          }
        }

        int lenToKeep = Math.min(value.length(), fieldMax[t]);
        if (buf.length() > 0)
          buf.append(" | ");
        buf.append(value.substring(0, lenToKeep) +
                   spaces.substring(0, fieldMax[t] - lenToKeep));
      } // for

      if (found > 0 || nToSkip == 0) {
        Trace.debug(title + buf);
        title = spaces.substring(0, title.length());
        ++nToSkip;
      }
    } while (found > 0);
  } // outputDisplayKey()

  private TagChars chars1 = new TagChars();
  private TagChars chars2 = new TagChars();

  /**
   * Score the potential match of two lists of titles.
   */
  private int scoreTitleMatch(IntList list1, IntList list2) 
  {
    // If both lists are empty, it's no foul, no score.
    if (list1.isEmpty() && list2.isEmpty())
      return 0;

    // See how many match exactly, and how many we need to skip.
    int p1 = 0;

    // See how many match exactly, and how many we need to skip.
    int p2 = 0;
    final int size1 = list1.size();
    final int size2 = list2.size();
    int nMatches = 0;
    int skipped1 = 0;
    int skipped2 = 0;
    int maxScore = 100;
    while (p1 < size1 && p2 < size2) 
    {
      int tag1 = list1.get(p1);
      int tag2 = list2.get(p2);
      int subType1 = data.tags.getSubType(tag1);
      int subType2 = data.tags.getSubType(tag2);

      // If they match exactly, advance.
      if (subType1 == subType2) 
      {
        if (tag1 == tag2) {
          ++nMatches;
          ++p1;
          ++p2;
          continue;
        }

        // If they match before a colon, advance.
        if (matchPartialTitle(tag1, tag2)) {
          ++nMatches;
          ++p1;
          ++p2;
          maxScore = 80;
          continue;
        }
      }

      // Okay, figure out which one to skip.
      if (tag1 < tag2) {
        ++skipped1;
        ++p1;
      }
      else {
        ++skipped2;
        ++p2;
      }
    }
    skipped1 += (size1 - p1);
    skipped2 += (size2 - p2);

    // Are the lists identical?
    if (skipped1 == 0 && skipped2 == 0) {
      assert nMatches > 0;
      return maxScore;
    }

    // Is one a subset of the other?
    if (nMatches > 0 && (skipped1 == 0 || skipped2 == 0))
      return 80;

    // Okay, even if there were some matches, there was at least one mismatch.
    return -100;
  } // scoreTitleMatch()

  /**
   * Check if one title matches the other without a colon.
   */
  private boolean matchPartialTitle(int tag1, int tag2) 
  {
    data.tags.getChars(tag1, chars1);
    data.tags.getChars(tag2, chars2);

    // If at least 10 chars don't match, don't even try.
    int prefixMatch = chars1.prefixMatch(chars2);
    if (prefixMatch < 10)
      return false;

    // Which one has the colon?
    int colonPos = chars1.indexOf(':');
    if (colonPos >= 10)
      return prefixMatch == chars2.length() && prefixMatch >= colonPos;

    colonPos = chars2.indexOf(':');
    if (colonPos >= 10)
      return prefixMatch == chars1.length() && prefixMatch >= colonPos;

    return false;
  }

  /**
   * Score the potential match of two lists of authors.
   */
  private int scoreAuthorMatch(IntList list1, IntList list2) 
  {
    // If both lists are empty, consider that a bit of good.
    if (list1.isEmpty() && list2.isEmpty())
      return 75;

    // See how many match exactly, and how many we have to skip.
    int p1 = 0;

    // See how many match exactly, and how many we have to skip.
    int p2 = 0;
    final int size1 = list1.size();
    final int size2 = list2.size();
    int nMatches = 0;
    int skipped1 = 0;
    int skipped2 = 0;
    int maxScore = 100;
    while (p1 < size1 && p2 < size2) 
    {
      int tag1 = list1.get(p1);
      int tag2 = list2.get(p2);
      int subType1 = data.tags.getSubType(tag1);
      int subType2 = data.tags.getSubType(tag2);

      // If they match exactly, advance.
      if (subType1 == subType2) 
      {
        if (tag1 == tag2) {
          ++nMatches;
          ++p1;
          ++p2;
          continue;
        }

        // If they match out-of-order, advance.
        if (matchPartialAuthor(tag1, tag2)) {
          ++nMatches;
          ++p1;
          ++p2;
          maxScore = 80;
          continue;
        }
      }

      // Okay, figure out which one to skip.
      if (tag1 < tag2) {
        ++skipped1;
        ++p1;
      }
      else {
        ++skipped2;
        ++p2;
      }
    }
    skipped1 += (size1 - p1);
    skipped2 += (size2 - p2);

    // Are the lists identical?
    if (skipped1 == 0 && skipped2 == 0) {
      assert nMatches > 0;
      return maxScore;
    }

    // Is one a subset of the other?
    if (nMatches > 0 && (skipped1 == 0 || skipped2 == 0))
      return 80;

    // Okay, even if there were some matches, there was at least one mismatch.
    return -100;
  } // scoreAuthorMatch()

  private int wordHashKey = 0;
  private static final int WORD_HASH_SIZE = Prime.findAfter(1000000);
  private int[] wordHash = new int[WORD_HASH_SIZE];
  private static final char[] charType = new char[0x10000];

  static 
  {
    // Whitespace
    charType[' '] = 'p';
    charType['\t'] = 'p';
    charType['\n'] = 'p';
    charType['\r'] = 'p';
    charType['\f'] = 'p';

    // Punctuation
    charType['\''] = 'p';
    charType['"'] = 'p';
    charType['.'] = 'p';
    charType['&'] = 'p';
    charType['@'] = 'p';
    charType['-'] = 'p';
    charType['/'] = 'p';
    charType[','] = 'p';
    charType[':'] = 'p';
    charType[';'] = 'p';
    charType['('] = 'p';
    charType[')'] = 'p';
    charType['['] = 'p';
    charType[']'] = 'p';
  };

  /**
   * Compare two author names to see if the keywords from one are completely
   * contained within the other.
   */
  private boolean matchPartialAuthor(int tag1, int tag2) 
  {
    // Pick the longer one to start with
    data.tags.getChars(tag1, chars1);
    data.tags.getChars(tag2, chars2);

    if (chars2.length() > chars1.length()) 
    {
      int tmp = tag1;
      tag1 = tag2;
      tag2 = tmp;

      TagChars cTmp = chars1;
      chars1 = chars2;
      chars2 = cTmp;
    }

    // Advance to the next key value, so we can distinguish old hash values
    // from new ones.
    //
    ++wordHashKey;

    // Add all the words from the first author to the hash
    int i = 0;
    while (i < chars1.length()) 
    {
      int hashCode = 0;
      int nChars = 0;
      for (; i < chars1.length(); i++) 
      {
        char c = chars1.charAt(i);
        if (charType[c] == 'p') {
          i++;
          break;
        }
        hashCode = (hashCode * 31) + c;
        ++nChars;
      }

      if (hashCode != 0 && nChars > 3)
        wordHash[(hashCode & 0x7FFFFFFF) % WORD_HASH_SIZE] = wordHashKey;
    }

    // Now check all the words from the second (shorter) author to see if 
    // they're present
    //
    i = 0;
    int nWords2 = 0;
    int nMatch2 = 0;
    while (i < chars2.length()) 
    {
      int hashCode = 0;
      int nChars = 0;
      for (; i < chars2.length(); i++) 
      {
        char c = chars2.charAt(i);
        if (charType[c] == 'p') {
          i++;
          break;
        }
        hashCode = (hashCode * 31) + c;
        ++nChars;
      }

      if (hashCode != 0 && nChars > 3) {
        ++nWords2;
        if (wordHash[(hashCode & 0x7FFFFFFF) % WORD_HASH_SIZE] == wordHashKey)
          ++nMatch2;
      }
    } // while

    // If all the words from the shorter author matched (and there were at least
    // two words found), call it good.
    return (nWords2 == nMatch2 && nWords2 >= 2);
  } // matchPartialAuthor()

  /**
   * Compare two dates for a match.
   */
  @SuppressWarnings("unused")
  private int scoreDateMatch(IntList list1, IntList list2) 
  {
    // If no date, don't consider it a problem.
    if (list1.isEmpty() || list2.isEmpty())
      return 0;

    // Since at the moment we're using sort-year, there should be only one.
    assert list1.size() == 1;
    assert list2.size() == 1;

    int tag1 = list1.get(0);
    int tag2 = list2.get(0);

    // If they're exactly equal, great.
    if (tag1 == tag2)
      return 50;

    // Parse the years
    String str1 = data.tags.getString(tag1);
    String str2 = data.tags.getString(tag2);
    int year1 = -99;
    int year2 = -99;
    try {
      year1 = Integer.parseInt(str1);
    }
    catch (NumberFormatException e) {
    }
    try {
      year2 = Integer.parseInt(str2);
    }
    catch (NumberFormatException e) {
    }

    // If either is missing, no match.
    if (year1 < 0 || year2 < 0)
      return 0;

    // If within 2 years, consider that only slightly bad.
    if (Math.abs(year1 - year2) <= 2)
      return -25;

    // All other cases: no match.
    return -50;
  } // scoreDateMatch

  /**
   * Score the potential match of two lists of identifiers.
   */
  private int scoreIdMatch(IntList list1, IntList list2) 
  {
    // If both lists are empty, it's no foul, no score.
    if (list1.isEmpty() && list2.isEmpty())
      return 0;

    // See how many match exactly, and how many we need to skip.
    int p1 = 0;

    // See how many match exactly, and how many we need to skip.
    int p2 = 0;
    final int size1 = list1.size();
    final int size2 = list2.size();
    int nMatches = 0;
    int skipped1 = 0;
    int skipped2 = 0;
    int maxScore = 100;
    while (p1 < size1 && p2 < size2) 
    {
      int tag1 = list1.get(p1);
      int tag2 = list2.get(p2);
      int subType1 = data.tags.getSubType(tag1);
      int subType2 = data.tags.getSubType(tag2);

      // If they match exactly, advance.
      if (subType1 == subType2) 
      {
        if (tag1 == tag2) {
          ++nMatches;
          ++p1;
          ++p2;
          continue;
        }

        // If they match before a paren, advance.
        if (matchPartialId(tag1, tag2)) {
          ++nMatches;
          ++p1;
          ++p2;
          maxScore = 80;
          continue;
        }
      }

      // Okay, figure out which one to skip.
      if (tag1 < tag2) {
        ++skipped1;
        ++p1;
      }
      else {
        ++skipped2;
        ++p2;
      }
    }
    skipped1 += (size1 - p1);
    skipped2 += (size2 - p2);

    // Are the lists identical?
    if (skipped1 == 0 && skipped2 == 0) {
      assert nMatches > 0;
      return maxScore;
    }

    // Is one a subset of the other?
    if (nMatches > 0 && (skipped1 == 0 || skipped2 == 0))
      return 80;

    // Okay, even if there were some matches, there was at least one mismatch.
    // This is pretty common with identifiers, so don't count this as a 
    // negative.
    //
    return 0;
  } // scoreIdMatch()

  /**
   * Check if two identifiers match before parentheses
   */
  private boolean matchPartialId(int tag1, int tag2) 
  {
    data.tags.getChars(tag1, chars1);
    data.tags.getChars(tag2, chars2);

    // If at least 6 chars don't match, don't even try.
    int prefixMatch = chars1.prefixMatch(chars2);
    if (prefixMatch < 6)
      return false;

    // Which one has the parenthesis?
    int parenPos = chars1.indexOf('(');
    if (parenPos >= 6)
      return prefixMatch == chars2.length() && prefixMatch >= parenPos;

    parenPos = chars2.indexOf('(');
    if (parenPos >= 6)
      return prefixMatch == chars1.length() && prefixMatch >= parenPos;

    return false;
  }

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
  public int findGroup(String name) {
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
    return (groupId == 0 || groupId == nGroups - 1) ? -1 : (groupId + 1);
  }

  // inherit JavaDoc
  public int parent(int groupId) {
    return (groupId == 0) ? -1 : 0;
  }

  // inherit JavaDoc
  public int nChildren(int groupId) {
    return (groupId == 0) ? (nGroups - 1) : 0;
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
    if ((x = compareField(primarySort, doc1, doc2, reversePrimarySort)) != 0)
      return x;

    // Now compare the secondary fields, in order.
    for (int t = FRBRData.FIRST_TYPE; t <= FRBRData.LAST_TYPE; ++t) {
      if (t != primarySort && (x = compareField(t, doc1, doc2, false)) != 0)
        return x;
    }

    // No differences found.
    return 0;
  }

  /** Find the title of a document */
  @SuppressWarnings("unused")
  private String docTitle(int doc) {
    for (int pos = data.docTags.firstPos(doc); pos >= 0;
         pos = data.docTags.nextPos(pos)) {
      int tag = data.docTags.getValue(pos);
      int type = data.tags.getType(tag);
      if (type != FRBRData.TYPE_TITLE)
        continue;
      return data.tags.getString(tag);
    }

    return "";
  }

  /** Compare a particular field of two groups */
  private int compareField(int type, int doc1, int doc2, boolean reverse) 
  {
    // Locate this field in the first doc.
    int tag1 = 0;
    for (int pos = data.docTags.firstPos(doc1); pos >= 0 && tag1 == 0;
         pos = data.docTags.nextPos(pos)) {
      int tag = data.docTags.getValue(pos);
      if (data.tags.getType(tag) == type)
        tag1 = tag;
    }

    // ... and locate it in the second doc.
    int tag2 = 0;
    for (int pos = data.docTags.firstPos(doc2); pos >= 0 && tag2 == 0;
         pos = data.docTags.nextPos(pos)) {
      int tag = data.docTags.getValue(pos);
      if (data.tags.getType(tag) == type)
        tag2 = tag;
    }

    // Make sure docs that don't have an entry sort at the end, not the beginning.
    if (tag1 == 0)
      tag1 = reverse ? Integer.MIN_VALUE : Integer.MAX_VALUE;
    if (tag2 == 0)
      tag2 = reverse ? Integer.MIN_VALUE : Integer.MAX_VALUE;

    // Now a simple numerical comparison on the tags will do.
    if (reverse)
      return (tag1 < tag2) ? +1 : ((tag1 > tag2) ? -1 : 0);
    else
      return (tag1 < tag2) ? -1 : ((tag1 > tag2) ? +1 : 0);
  } // compareField
} // class FRBRGroupData
