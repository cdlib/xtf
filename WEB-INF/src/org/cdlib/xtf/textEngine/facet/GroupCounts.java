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
  private GroupData       data;
  private FacetSpec       spec;
  private HitQueueMaker   hitQueueMaker;
  
  private boolean         prepMode = false;
  
  private int[]           count;
  private int[]           mark;
  private int[]           selection;
  private int[]           startDoc;
  private int[]           maxDocs;
  private PriorityQueue[] hitQueue;
  
  private int[]           sortedChild;
  private int[]           sortedSibling;
  
  private int             curMark = 1000;
  
  /** Construct an object with all counts at zero */
  public GroupCounts( GroupData     groupData, 
                      FacetSpec     spec,
                      HitQueueMaker hitQueueMaker ) 
  {
    // Record the input parameters for later use
    this.data          = groupData;
    this.spec          = spec;
    this.hitQueueMaker = hitQueueMaker;
    
    // Allocate our arrays of counts and such
    count     = new int[data.nGroups()];
    mark      = new int[data.nGroups()];
    selection = new int[data.nGroups()];
    startDoc  = new int[data.nGroups()];
    maxDocs   = new int[data.nGroups()];
    hitQueue  = new PriorityQueue[data.nGroups()];
    
    // Gather conservative data about which groups to gather DocHits for.
    prep();
  } // constructor
  
  /** Gather data about which groups to gather DocHits for. */
  private void prep()
  {
    // Enter prep mode so that when selectGroup() and collectHits() are called,
    // they'll know what to do.
    //
    try {
        prepMode = true;

        // Now, do a conservative selection of the groups, and record which
        // ones might need DocHits recorded.
        //
        GroupSelector sel = spec.groupSelector;
        sel.setCounts( this );
        
        // Tell the selector to be conservative in choosing which groups
        // to selet.
        //
        sel.reset( true );
        
        // Now ask it to select everyting (start it out with the root)
        sel.process( 0 );
        sel.flush();
    }
    finally 
    {
        // Exit prep mode, no matter what.
        prepMode = false;
    }
  } // prep()
  
  /** Called by GroupSelector to select a given group */
  public final void selectGroup( int group ) {
    if( prepMode )
        return;
    
    // Select the group, and put a secondary selection on each ancestor (up to 
    // the root)
    //
    boolean first = true;
    for( ; group >= 0; group = data.parent(group) ) 
    {
        if( first )
            selection[group] = 1;
        else if( selection[group] == 0 )
            selection[group] = 2;
        else
            break;
    } // for
    
  } // selectGroup()
  
  /** Called by GroupSelector to mark groups to receive documents */
  public final void gatherDocs( int group, int startDoc, int maxDocs ) {
    this.startDoc[group] = startDoc;
    this.maxDocs[group]  = maxDocs;
  }

  /** Called by GroupSelector to find out if the ordering is non-default */
  public final boolean nondefaultSort() {
    return !spec.sortGroupsBy.equals("value");
  }
  
  /** Called by GroupSelector to find out if it should include a given group */
  public final boolean shouldInclude( int group ) {
    if( !prepMode && !spec.includeEmptyGroups && count[group] == 0 )
        return false;
    return true;
  }
  
  /** Get the total number of groups */
  public final int nGroups() {
    return data.nGroups();
  }
  
  /** Get the first child of the given group, in properly sorted order */
  public final int child( int group ) {
    if( sortedChild != null )
        return sortedChild[group];
    return data.child( group );
  }
  
  /** Get the next sibling of the given group, in properly sorted order */
  public final int sibling( int group ) {
    if( sortedSibling != null )
        return sortedSibling[group];
    return data.sibling( group );
  }
  
  /** Get the parent of the given group */
  public final int parent( int group ) {
    return data.parent( group );
  }
  
  /** Get the name of a specific group */
  public final String name( int group ) {
    return data.name( group );
  }
  
  /** Find out whether the given group is selected */
  public final boolean isSelected( int group ) {
    return selection[group] == 1;
  }
  
  /** Find out the number of doc hits for the given group */
  public final int nDocHits( int group ) {
    return count[group];
  }
  
  /** Add a document hit to the counts */
  public void addDoc( DocHitMaker docHitMaker )
  {
    int link, group;
    
    // Use a unique mark for each doc.
    curMark++;

    // Process each group this document is in.
    int doc = docHitMaker.getDocNum();
    for( link = data.firstLink(doc); link >= 0; link = data.nextLink(link) )
    {
        // Bump the count for the group and each ancestor (up to the root)
        for( group = data.linkGroup(link);
             group >= 0; 
             group = data.parent(group) )
        {
            // Don't count the same doc twice for one group.
            if( mark[group] == curMark )
                break;
            
            // Bump the count, and mark this group so we don't do it for this
            // doc again.
            //
            count[group]++;
            mark[group] = curMark;
            
            // If we're not recording hits for this group, we're done.
            if( maxDocs[group] == 0 )
                continue;
            
            // Create a DocHitQueue if not done yet.
            if( hitQueue[group] == null ) {
                hitQueue[group] = hitQueueMaker.makeQueue( startDoc[group] + 
                                                           maxDocs[group] );
            }
            
            // And add this document to the hit queue.
            if( maxDocs[group] >= 999999999 )
                hitQueue[group].ensureCapacity( 1 );
            docHitMaker.insertInto( hitQueue[group] );
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
    
    // Clear the startDoc/maxDocs arrays so we can rebuild them knowing now
    // exactly which groups need docs (we had to be conservative up front.)
    //
    Arrays.fill( startDoc, 0 );
    Arrays.fill( maxDocs,  0 );
    
    // The groups are by default sorted by name. Sort by count if necessary.
    if( spec.sortGroupsBy.equals("value") )
        ;
    else if( spec.sortGroupsBy.equals("totalDocs") )
        sortByCount();
    else {
        throw new RuntimeException( "Unknown option for sortGroupsBy: " +
            spec.sortGroupsBy );
    }
    
    // Now select the proper groups.
    GroupSelector sel = spec.groupSelector;
    sel.reset( false );
    sel.process( 0 );
    sel.flush();
    
    // Recursively build the result set.
    resultFacet.rootGroup = buildResultGroup( 0 );
    
    // All done.
    return resultFacet;
  }
  
  public ResultGroup buildResultGroup( int parent )
  {
    // Make a place for the result
    ResultGroup result = new ResultGroup();
    
    // Record the value of the parent group.
    if( parent != 0 )
        result.value = data.name( parent );
    
    // Record the total number of doc hits for the parent group
    result.totalDocs = count[parent];
    
    // Count the child groups
    int nSelected = 0;
    for( int kid = child(parent); kid >= 0; kid = sibling(kid) ) {
        if( !shouldInclude(kid) )
            continue;
        ++result.totalSubGroups;
        if( selection[kid] != 0 )
            ++nSelected;
    }
    
    // Build an array of the child groups.
    if( nSelected > 0 )
        result.subGroups = new ResultGroup[nSelected];
    int rank = 0;
    int n = 0;
    for( int kid = child(parent); kid >= 0; kid = sibling(kid) ) {
        if( !shouldInclude(kid) )
            continue;
        if( selection[kid] != 0 ) {
            result.subGroups[n] = buildResultGroup( kid );
            result.subGroups[n].rank = rank;
            n++;
        }
        ++rank;
    }
    assert n == nSelected : "miscount";
    
    // If dochits were requested for this group, grab them.
    if( maxDocs[parent] != 0 && hitQueue[parent] != null )
        buildDocHits( parent, result );

    // All done!
    return result;
    
  } // getGroups()
  
  /** Re-sort the hierarchy by descending count, and store the new 
   *  child/sibling relationships.
   */
  private void sortByCount()
  {
    final long mask = (1L << 20) - 1;
    
    // The code below depends on bit shifting, and we allocate 20 bits at most.
    if( (data.nGroups() >> 20) != 0 )
        throw new RuntimeException( "Too many groups (more than 20 bits req)" );
    
    // Build an easy-to-sort array of the child groups and their counts.
    long[] array = new long[data.nGroups()];
    for( int i = 0; i < data.nGroups(); i++ ) {
        long x = 0;
        x |= ((long)data.parent(i))    << 40;
        x |= ((long)(count[i] ^ mask)) << 20;
        x |= i;
        array[i] = x;
    }
    
    // Sort by parent, then by descending count, then by ascending name.
    Arrays.sort( array );
    
    // Allocate and clear arrays to hold the sorted child and sibling numbers.
    sortedChild   = new int[data.nGroups()];
    sortedSibling = new int[data.nGroups()];
    Arrays.fill( sortedChild,   -1 );
    Arrays.fill( sortedSibling, -1 );
    
    // Process each sibling group.
    int prevParent = -1;
    int prevGroup  = -1;
    for( int i = 1; i < data.nGroups(); i++ ) { // skip root
        long val = array[i];
        int parent = (int) (val >> 40L);
        int group  = (int) (val & mask);
        
        if( parent != prevParent )
            sortedChild[parent] = group;
        else
            sortedSibling[prevGroup] = group;
        
        prevGroup = group;
        prevParent = parent;
    } // for i
  } // sortByCount()
  
  /**
   * Locates the highest level in the hierarchy at which there are differences.
   * 
   * @return  ID of the best group (might be zero, meaning the root.)
   */
  private int findBestParent( int parent )
  {
    // See if there are differences at this level.
    int nonzeroKid = -1;
    for( int kid = data.child(parent); kid >= 0; kid = data.sibling(kid) )
    {
        if( count[kid] > 0 ) {
            if( nonzeroKid >= 0 )
                return parent;
            nonzeroKid = kid;
        }
    } // for kid
    
    // Nope. If one kid had counts, drill down into that kid.
    if( nonzeroKid >= 0 )
        return findBestParent( nonzeroKid );
    else
        return -1;
    
  } // findBestParent()
  
  /** Construct the array of doc hits for the hit group. */
  private void buildDocHits( int group, ResultGroup resultGroup )
  {
    PriorityQueue queue = hitQueue[group];
    int nFound = queue.size();
    DocHitImpl[] hitArray = new DocHitImpl[nFound];
    float maxDocScore = 0.0f;
    for( int i = 0; i < nFound; i++ ) {
        int index = nFound - i - 1;
        hitArray[index] = (DocHitImpl) queue.pop();
    }
    
    int start = startDoc[group];
    int max   = maxDocs[group];
    
    int nHits = Math.max(0, Math.min(nFound - start, max) );
    resultGroup.docHits = new DocHit[nHits];
    
    resultGroup.totalDocs = count[group];
    resultGroup.startDoc  = start;
    resultGroup.endDoc    = start + nHits;

    for( int i = startDoc[group]; i < nFound; i++ )
        resultGroup.docHits[i-start] = hitArray[i];
    
  } // buildDocHits()
  
  public static interface HitQueueMaker {
    PriorityQueue makeQueue( int size );
  }
  
  public static interface DocHitMaker {
    int     getDocNum();
    boolean insertInto( PriorityQueue queue );
  }
  
} // class GroupCounts
