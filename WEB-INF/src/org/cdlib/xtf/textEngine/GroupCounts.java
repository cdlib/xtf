package org.cdlib.xtf.textEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.apache.lucene.mark.FieldSpans;
import org.apache.lucene.util.PriorityQueue;

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

/**
 * Maintains an ongoing count of groups and how many document hits were
 * found in each group.
 * 
 * @author Martin Haye
 */
public class GroupCounts 
{
  private GroupData     data;
  private String        hitGroupName;
  private PriorityQueue hitQueue;
  private int[]         counts;
  
  /** Construct an object with all counts at zero */
  public GroupCounts( GroupData groupData ) {
    this.data = groupData;
    counts = new int[data.nGroups()];
  }
  
  /** Record hits for a single group only */
  public void recordHits( String value, PriorityQueue hitQueue ) {
    hitGroupName = value.intern();
    this.hitQueue = hitQueue;
  }
  
  /** Add a document hit to the counts */
  public void addDoc( int doc, float score, FieldSpans spans )
  {
    int link, group;

    // Process each group this document is in.
    for( link = data.firstLink(doc); link >= 0; link = data.nextLink(link) )
    {
        // Bump the count for the group and each ancestor (up to the root)
        for( group = data.linkGroup(link);
             group >= 0; 
             group = data.parent(group) )
        {
            counts[group]++;
            
            // If this is the group we're recording hits for, do it.
            if( data.name(group) == hitGroupName )
                hitQueue.insert( new DocHit(doc, score, spans) );
        } // for group
    } // for link
    
  } // addDoc()
  
  /**
   * Retrieve all or a subset of the groups and their associated counts.
   * 
   * @param sortBy  How to sort the groups: "count" or "value" are the
   *                only permissible options.
   * @param start   First group to return (zero-based)
   * @param max     Max # of groups to return
   * @return        Array of the groups
   */
  public Group[] getGroups( String sortBy, int start, int max )
  {
    // Locate the highest level in the hierarchy with differences.
    int parent = findBestParent( 0 );
    if( parent < 0 )
        return new Group[0];
    
    // Build an array of the groups at that level.
    ArrayList groups = new ArrayList();
    int rank = 0;
    for( int kid = data.child(parent); kid >= 0; kid = data.sibling(kid) ) 
    {
        if( counts[kid] == 0 )
            continue;
        
        Group g = new Group();
        g.rank = rank++;
        g.value = data.name( kid );
        g.count = counts[kid];
        if( g.value == hitGroupName )
            g.docHits = makeDocHits();
        
        groups.add( g );
    }
    
    // If sorting by count, we have to reorder.
    if( sortBy.equals("count") ) {
        Collections.sort( groups, new Comparator() { 
            public int compare( Object o1, Object o2 ) {
                Group g1 = (Group) o1;
                Group g2 = (Group) o2;
                if( g1.count != g2.count )
                    return g1.count - g2.count;
                return g1.value.compareTo( g2.value );
            }
        } );
    }
    
    // Now extract just the requested part.
    int from = Math.min( groups.size(), start );
    int to   = Math.min( groups.size(), start + max );
    int n    = to - from;
    Group[] array = (Group[]) groups.subList(from, to).toArray(new Group[n]);
    return array;
    
  } // getGroups()
  
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
        if( counts[kid] > 0 ) {
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
  private DocHit[] makeDocHits()
  {
    int nHits = hitQueue.size();
    DocHit[] hits = new DocHit[nHits];
    for( int i = 0; i < nHits; i++ )
        hits[i] = (DocHit) hitQueue.pop();
    return hits;
  } // makeDocHits()
  
  /** Contains all the information for a single group */
  public static class Group
  {
    int      rank;
    String   value;
    int      count;
    DocHit[] docHits;
  } // class Group

} // class GroupCounts
