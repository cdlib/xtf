package org.cdlib.xtf.textEngine.facet;


/*
 * Copyright (c) 2004, Regents of the University of California
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
import java.util.Arrays;
import org.apache.lucene.util.PriorityQueue;
import org.cdlib.xtf.textEngine.DocHit;
import org.cdlib.xtf.textEngine.DocHitImpl;

/**
 * Maintains an ongoing count of groups and how many document hits were
 * found in each group.
 *
 * @author Martin Haye
 */
public class GroupCounts 
{
  private GroupData data;
  private FacetSpec spec;
  private HitQueueMaker hitQueueMaker;
  private boolean prepMode = false;
  private int[] count;
  private float[] score;
  private int[] mark;
  private int[] selection;
  private int[] startDoc;
  private int[] maxDocs;
  private PriorityQueue[] hitQueue;
  private int[] sortedChild;
  private int[] sortedSibling;
  private int curMark = 1000;
  private static final int SORT_BY_VALUE = 0;
  private static final int SORT_BY_REVERSE_VALUE = 1;
  private static final int SORT_BY_TOTAL_DOCS = 2;
  private static final int SORT_BY_MAX_DOC_SCORE = 3;

  /** Construct an object with all counts at zero */
  public GroupCounts(GroupData groupData, FacetSpec spec,
                     HitQueueMaker hitQueueMaker) 
  {
    // Record the input parameters for later use
    this.data = groupData;
    this.spec = spec;
    this.hitQueueMaker = hitQueueMaker;

    // Allocate our arrays of counts and such
    if (!data.isDynamic()) {
      count = new int[data.nGroups()];
      score = new float[data.nGroups()];
    }
    mark = new int[data.nGroups()];
    selection = new int[data.nGroups()];
    startDoc = new int[data.nGroups()];
    maxDocs = new int[data.nGroups()];
    hitQueue = new PriorityQueue[data.nGroups()];

    // For dynamic data, we can perform the final sort and selection
    // right now, since the group counts and scores are known.
    //
    if (data.isDynamic())
      sortAndSelect();

    // For static data, make a conservative selection.
    else
      conservativePrep();
  } // constructor

  /** Gather data about which groups to gather DocHits for. */
  private void conservativePrep() 
  {
    // Enter prep mode so that when selectGroup() and collectHits() are called,
    // they'll know what to do.
    //
    try 
    {
      prepMode = true;

      // Tell the selector to talk to us.
      GroupSelector sel = spec.groupSelector;
      synchronized (sel) 
      {
        // Tell the selector to talk to us.
        sel.setCounts(this);

        //
        // Tell the selector to be conservative in choosing which groups
        // to select.
        //
        sel.reset(true);

        // Now ask it to select everything (start it out with the root)
        sel.process(0);
        sel.flush();
      }
    }
    finally {
      // Exit prep mode, no matter what.
      prepMode = false;
    }
  } // prep()

  /** Called by GroupSelector to select a given group */
  public final void selectGroup(int group) 
  {
    if (prepMode)
      return;

    // Select the group, and put a secondary selection on each ancestor (up to 
    // the root)
    //
    boolean first = true;
    for (; group >= 0; group = data.parent(group)) {
      if (first)
        selection[group] = 1;
      else if (selection[group] == 0)
        selection[group] = 2;
      else
        break;
    } // for
  } // selectGroup()

  /** Called by GroupSelector to mark groups to receive documents */
  public final void gatherDocs(int group, int startDoc, int maxDocs) {
    this.startDoc[group] = startDoc;
    this.maxDocs[group] = maxDocs;
  }

  /** Called by GroupSelector to find out if the ordering is non-default */
  public final boolean nondefaultSort() {
    return !spec.sortGroupsBy.equals("value");
  }

  /** Called by GroupSelector to find out if it should include a given group */
  public final boolean shouldInclude(int group) 
  {
    if (!spec.includeEmptyGroups) 
    {
      if (data.isDynamic()) {
        if (data.nDocHits(group) == 0)
          return false;
      }
      else if (!prepMode && count[group] == 0)
        return false;
    }
    return true;
  }

  /** Get the total number of groups */
  public final int nGroups() {
    return data.nGroups();
  }

  /** Get the first child of the given group, in properly sorted order */
  public final int child(int group) {
    if (sortedChild != null)
      return sortedChild[group];
    return data.child(group);
  }

  /** Get the next sibling of the given group, in properly sorted order */
  public final int sibling(int group) {
    if (sortedSibling != null)
      return sortedSibling[group];
    return data.sibling(group);
  }

  /** Get the parent of the given group */
  public final int parent(int group) {
    return data.parent(group);
  }

  /** Get the name of a specific group */
  public final String name(int group) {
    return data.name(group);
  }

  /** Find out whether the given group is selected */
  public final boolean isSelected(int group) {
    return selection[group] == 1;
  }

  /** Find out the number of doc hits for the given group */
  public final int nDocHits(int group) {
    if (data.isDynamic())
      return data.nDocHits(group);
    return count[group];
  }

  /** Find out the score of the given group */
  public final float score(int group) {
    if (data.isDynamic())
      return data.score(group);
    return score[group];
  }

  /** Add a document hit to the counts */
  public void addDoc(DocHitMaker docHitMaker) 
  {
    int link;
    int group;

    // Use a unique mark for each doc.
    curMark++;

    // Process each group this document is in.
    int doc = docHitMaker.getDocNum();
    float docScore = docHitMaker.getScore();
    for (link = data.firstLink(doc); link >= 0; link = data.nextLink(link)) 
    {
      // Bump the count for the group and each ancestor (up to the root)
      for (group = data.linkGroup(link); group >= 0;
           group = data.parent(group)) 
      {
        // Don't count the same doc twice for one group.
        if (mark[group] == curMark)
          break;

        // Bump the count, and mark this group so we don't do it for this
        // doc again.
        //
        if (!data.isDynamic()) {
          count[group]++;
          score[group] = Math.max(score[group], docScore);
        }
        mark[group] = curMark;

        // If we're not recording hits for this group, we're done.
        if (maxDocs[group] == 0)
          continue;

        // Create a DocHitQueue if not done yet.
        if (hitQueue[group] == null) {
          hitQueue[group] = hitQueueMaker.makeQueue(
            startDoc[group] + maxDocs[group]);
        }

        // And add this document to the hit queue.
        docHitMaker.insertInto(hitQueue[group]);
      } // for group
    } // for link
  } // addDoc()

  /**
   * Retrieve the result facet with its groupings.
   */
  public ResultFacet getResult() 
  {
    // Create an empty result to start with
    ResultFacet resultFacet = new ResultFacet();
    resultFacet.field = data.field();

    // For dynamic facets, the groups have already been sorted and selected.
    // For static facets, we don't know until this point what the counts and
    // such are, so we couldn't make the final selection.
    //
    if (!data.isDynamic())
      sortAndSelect();

    // Recursively build the result set.
    resultFacet.rootGroup = buildResultGroup(0);

    // All done.
    return resultFacet;
  }

  /**
   * Called during the prep phase for dynamic groups, and in the result
   * building phase for static groups. Sorts the groups based on the
   * facet spec, and performs the final (non-conservative) selection.
   */
  private void sortAndSelect() 
  {
    // Clear the startDoc/maxDocs arrays so we can rebuild them knowing now
    // exactly which groups need docs (we had to be conservative up front.)
    //
    Arrays.fill(startDoc, 0);
    Arrays.fill(maxDocs, 0);

    // Sort the groups (if necessary)
    sortGroups();

    // Now select the proper groups.
    GroupSelector sel = spec.groupSelector;
    synchronized (sel) {
      sel.setCounts(this);
      sel.reset(false); // not conservative, since all is sorted now.
      sel.process(0);
      sel.flush();
    }
  }

  public ResultGroup buildResultGroup(int parent) 
  {
    // Make a place for the result
    ResultGroup result = new ResultGroup();

    // Record the value of the parent group.
    if (parent != 0)
      result.value = data.name(parent);

    // Record the total number of doc hits for the parent group
    result.totalDocs = nDocHits(parent);

    // Count the child groups
    int nSelected = 0;
    for (int kid = child(parent); kid >= 0; kid = sibling(kid)) {
      if (!shouldInclude(kid))
        continue;
      ++result.totalSubGroups;
      if (selection[kid] != 0)
        ++nSelected;
    }

    // Build an array of the child groups.
    if (nSelected > 0)
      result.subGroups = new ResultGroup[nSelected];
    int rank = 0;
    int n = 0;
    for (int kid = child(parent); kid >= 0; kid = sibling(kid)) 
    {
      if (!shouldInclude(kid))
        continue;
      if (selection[kid] != 0) {
        result.subGroups[n] = buildResultGroup(kid);
        result.subGroups[n].rank = rank;
        n++;
      }
      ++rank;
    }
    assert n == nSelected : "miscount";

    // If DocHits were requested for this group, grab them.
    if (maxDocs[parent] != 0 && hitQueue[parent] != null)
      buildDocHits(parent, result);

    // All done!
    return result;
  } // getGroups()

  /** Re-sort the hierarchy according to the facet spec, and store the new
   *  child/sibling relationships.
   */
  private void sortGroups() 
  {
    // Figure out what kind of sort was requested.
    int sortKind;
    if (spec.sortGroupsBy.equals("value"))
      sortKind = SORT_BY_VALUE;
    else if (spec.sortGroupsBy.equals("reverseValue"))
      sortKind = SORT_BY_REVERSE_VALUE;
    else if (spec.sortGroupsBy.equals("totalDocs"))
      sortKind = SORT_BY_TOTAL_DOCS;
    else if (spec.sortGroupsBy.equals("maxDocScore"))
      sortKind = SORT_BY_MAX_DOC_SCORE;
    else
      throw new RuntimeException(
        "Unknown option for sortGroupsBy: " + spec.sortGroupsBy);

    // For static data, the groups are already sorted by name.
    if (!data.isDynamic() && sortKind == SORT_BY_VALUE)
      return;

    // Allocate storage for sorted child/sibling links
    int nBefore = countDescendants(0);
    sortedChild = new int[data.nGroups()];
    sortedSibling = new int[data.nGroups()];
    Arrays.fill(sortedChild, -1);
    Arrays.fill(sortedSibling, -1);

    // Okay, do a recursive merge sort, starting at the root.
    sortChildren(0, sortKind);

    // Verify that we didn't lose anybody in the sort.
    int nAfter = countDescendants(0);
    assert nAfter == nBefore : "mis-count on sort";
  } // sortGroups()
  
  /** Utility function to count the group and all of its descendants */
  private int countDescendants(int group)
  {
    int count = 1; // for this group itself
    for (int kid = child(group); kid >= 0; kid = sibling(kid))
      count += countDescendants(kid);
    return count;
  }

  /** Construct the array of doc hits for the hit group. */
  private void buildDocHits(int group, ResultGroup resultGroup) 
  {
    PriorityQueue queue = hitQueue[group];
    int nFound = queue.size();
    DocHitImpl[] hitArray = new DocHitImpl[nFound];
    for (int i = 0; i < nFound; i++) {
      int index = nFound - i - 1;
      hitArray[index] = (DocHitImpl)queue.pop();
    }

    int start = startDoc[group];
    int max = maxDocs[group];

    int nHits = Math.max(0, Math.min(nFound - start, max));
    resultGroup.docHits = new DocHit[nHits];

    resultGroup.totalDocs = nDocHits(group);
    resultGroup.startDoc = start;
    resultGroup.endDoc = start + nHits;

    for (int i = startDoc[group]; i < nFound; i++)
      resultGroup.docHits[i - start] = hitArray[i];
  } // buildDocHits()

  public static interface HitQueueMaker {
    PriorityQueue makeQueue(int size);
  }

  public static interface DocHitMaker 
  {
    int getDocNum();

    float getScore();

    boolean insertInto(PriorityQueue queue);
  }

  /*
   * The following code is adapted from a super-cool linked list mergesort
   * algorithm by Simon Tatham. The code appears to be unrestricted, and
   * was obtained from this URL on 7/25/2006:
   *
   * http://www.chiark.greenend.org.uk/~sgtatham/algorithms/listsort.html
   *
   * Following is the original rights statement.
   *
   * This file is copyright 2001 Simon Tatham.
   *
   * Permission is hereby granted, free of charge, to any person
   * obtaining a copy of this software and associated documentation
   * files (the "Software"), to deal in the Software without
   * restriction, including without limitation the rights to use,
   * copy, modify, merge, publish, distribute, sublicense, and/or
   * sell copies of the Software, and to permit persons to whom the
   * Software is furnished to do so, subject to the following
   * conditions:
   *
   * The above copyright notice and this permission notice shall be
   * included in all copies or substantial portions of the Software.
   *
   * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
   * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
   * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
   * NONINFRINGEMENT.  IN NO EVENT SHALL SIMON TATHAM BE LIABLE FOR
   * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
   * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
   * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
   * SOFTWARE.
   */
  private void sortChildren(int parent, int sortKind) 
  {
    int first = data.child(parent);
    int p;
    int q;

    // If no children, we have nothing to do.
    if (first < 0)
      return;

    // Initialize the links.
    int nChildrenBefore = 0;
    for (p = first; p >= 0; p = q) {
      q = data.sibling(p);
      sortedSibling[p] = q;
      ++nChildrenBefore;
    }

    // Merge lists of size 1, 2, 4, 8, 16, ...
    int insize = 1;
    while (true) 
    {
      p = first;
      first = -1;
      int tail = -1;

      int nmerges = 0; /* count number of merges we do in this pass */

      while (p >= 0) 
      {
        nmerges++; /* there exists a merge to be done */
        /* step `insize' places along from p */
        q = p;
        int psize = 0;
        for (int i = 0; i < insize; i++) {
          psize++;
          q = sortedSibling[q];
          if (q < 0)
            break;
        }

        /* if q hasn't fallen off end, we have two lists to merge */
        int qsize = insize;

        /* now we have two lists; merge them */
        while (psize > 0 || (qsize > 0 && q >= 0)) 
        {
          /* decide whether next element of merge comes from p or q */
          int e;
          if (psize == 0) 
          {
            /* p is empty; e must come from q. */
            e = q;
            q = sortedSibling[q];
            qsize--;
          }
          else if (qsize == 0 || q < 0) 
          {
            /* q is empty; e must come from p. */
            e = p;
            p = sortedSibling[p];
            psize--;
          }
          else if (compare(p, q, sortKind) <= 0) 
          {
            /* First element of p is lower (or same);
             * e must come from p. */
            e = p;
            p = sortedSibling[p];
            psize--;
          }
          else 
          {
            /* First element of q is lower; e must come from q. */
            e = q;
            q = sortedSibling[q];
            qsize--;
          }

          /* add the next element to the merged list */
          if (tail >= 0)
            sortedSibling[tail] = e;
          else
            first = e;
          tail = e;
        }

        /* now p has stepped `insize' places along, and q has too */
        p = q;
      }
      sortedSibling[tail] = -1;

      /* If we have done only one merge, we're finished. */
      if (nmerges <= 1) /* allow for nmerges==0, the empty list case */
        break;

      /* Otherwise repeat, merging lists twice the size */
      insize *= 2;
    }

    // Record the first child.
    sortedChild[parent] = first;

    // Sort all the descendants of the children, and verify the sort.
    for (p = first; p >= 0; p = sortedSibling[p])
      sortChildren(p, sortKind);

    int nChildrenAfter = 0;
    for (p = sortedChild[parent]; p >= 0; p = sortedSibling[p]) {
      if (sortedSibling[p] >= 0)
        assert compare(p, sortedSibling[p], sortKind) <= 0 : "error in merge sort";
      ++nChildrenAfter;
    } // for
    assert nChildrenAfter == nChildrenBefore;
  } // sortChildren()

  /*
   * End of adapted code.
   */

  /**
   * Compare two groups for sorting purposes.
   */
  private int compare(int g1, int g2, int sortKind) 
  {
    int x;
    switch (sortKind) {
      case SORT_BY_VALUE:
        if ((x = data.compare(g1, g2)) != 0)
          return x;
        if ((x = -compare(score(g1), score(g2))) != 0)
          return x;
        if ((x = -compare(nDocHits(g1), nDocHits(g2))) != 0)
          return x;
        return 0;
      case SORT_BY_REVERSE_VALUE:
        if ((x = -data.compare(g1, g2)) != 0)
          return x;
        if ((x = -compare(score(g1), score(g2))) != 0)
          return x;
        if ((x = -compare(nDocHits(g1), nDocHits(g2))) != 0)
          return x;
        return 0;
      case SORT_BY_TOTAL_DOCS:
        if ((x = -compare(nDocHits(g1), nDocHits(g2))) != 0)
          return x;
        if ((x = data.compare(g1, g2)) != 0)
          return x;
        if ((x = -compare(score(g1), score(g2))) != 0)
          return x;
        return 0;
      case SORT_BY_MAX_DOC_SCORE:
        if ((x = -compare(score(g1), score(g2))) != 0)
          return x;
        if ((x = data.compare(g1, g2)) != 0)
          return x;
        if ((x = -compare(nDocHits(g1), nDocHits(g2))) != 0)
          return x;
        return 0;
      default:
        return 0;
    }
  } // compare()

  /** Compare two ints for sorting purposes */
  private static int compare(int x, int y) {
    return (x < y) ? -1 : ((x > y) ? 1 : 0);
  }

  /** Compare two floats for sorting purposes */
  private static int compare(float x, float y) {
    return (x < y) ? -1 : ((x > y) ? 1 : 0);
  }
} // class GroupCounts
