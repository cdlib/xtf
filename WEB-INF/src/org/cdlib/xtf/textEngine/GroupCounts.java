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
  private int           hitStartDoc;
  private int           hitMaxDocs;
  private int           hitTotalDocs;
  
  private int[]         counts;
  private int[]         marks;
  
  private int           curMark = 1000;
  
  /** Construct an object with all counts at zero */
  public GroupCounts( GroupData groupData ) {
    this.data = groupData;
    counts = new int[data.nGroups()];
    marks  = new int[data.nGroups()];
  }
  
  /** Record hits for a single group only */
  public void recordHits( String value, 
                          PriorityQueue hitQueue,
                          int startDoc,
                          int maxDocs ) 
  {
    hitGroupName     = value.intern();
    this.hitQueue    = hitQueue;
    this.hitStartDoc = startDoc;
    this.hitMaxDocs  = maxDocs;
    hitTotalDocs     = 0;
  }
  
  /** Add a document hit to the counts */
  public void addDoc( int doc, float score, FieldSpans spans )
  {
    int link, group;
    
    // Use a unique mark for each doc.
    curMark++;

    // Process each group this document is in.
    for( link = data.firstLink(doc); link >= 0; link = data.nextLink(link) )
    {
        // Bump the count for the group and each ancestor (up to the root)
        for( group = data.linkGroup(link);
             group >= 0; 
             group = data.parent(group) )
        {
            // Don't count the same doc twice for one group.
            if( marks[group] == curMark )
                break;
            
            // Bump the count, and mark this group so we don't do it for this
            // doc again.
            //
            counts[group]++;
            marks[group] = curMark;
            
            // If this is the group we're recording hits for, do it.
            if( data.name(group) == hitGroupName ) {
                hitQueue.insert( new DocHitImpl(doc, score, spans) );
                hitTotalDocs++;
            }
        } // for group
    } // for link
    
  } // addDoc()
  
  /**
   * Retrieve all or a subset of the groups and their associated counts.
   * 
   * @param sortBy      How to sort the groups: "count" or "value" are the
   *                    only permissible options.
   * @param startGroup  Ordinal rank of first group to return (zero-based)
   * @param maxGroups   Max # of groups to return
   * @return            Array of the groups
   */
  public ResultField getGroups( String sortBy, int startGroup, int maxGroups )
  {
    // Create an empty result to start with
    ResultField resultField = new ResultField();
    resultField.field = data.field();
    
    // Locate the highest level in the hierarchy with differences. If the root
    // is the only node with a count, return an empty result set.
    //
    int parent = findBestParent( 0 );
    if( parent < 0 ) {
        resultField.groups = new ResultGroup[0];
        return resultField;
    }
    
    // Build an array of the groups at that level.
    ArrayList groups = new ArrayList();
    for( int kid = data.child(parent); kid >= 0; kid = data.sibling(kid) ) 
    {
        if( counts[kid] == 0 )
            continue;
        
        ResultGroup g = new ResultGroup();
        g.value = data.name( kid );
        g.totalDocs = counts[kid];
        
        // Expand one group specified by name.
        if( g.value == hitGroupName )
            makeDocHits( g );
        
        groups.add( g );
    }
    
    // If sorting by count, we have to reorder.
    if( sortBy.equals("totalDocs") ) {
        Collections.sort( groups, new Comparator() { 
            public int compare( Object o1, Object o2 ) {
                ResultGroup g1 = (ResultGroup) o1;
                ResultGroup g2 = (ResultGroup) o2;
                if( g1.totalDocs != g2.totalDocs )
                    return g2.totalDocs - g1.totalDocs;
                return g1.value.compareTo( g2.value );
            }
        } );
    }
    else
        assert sortBy.equals("value") : "Unsupported sortGroupsBy option";
    
    // Now extract just the requested part.
    int from = Math.min( groups.size(), startGroup );
    int to   = Math.min( groups.size(), startGroup + maxGroups );
    int n    = to - from;
    
    resultField.totalGroups = groups.size();
    resultField.startGroup  = from;
    resultField.endGroup    = to;
    resultField.totalDocs   = counts[parent]; 
    resultField.groups      = (ResultGroup[]) groups.subList(from, to)
                                                  .toArray(new ResultGroup[n]);
    
    return resultField;
    
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
  private void makeDocHits( ResultGroup group )
  {
    int nFound = hitQueue.size();
    DocHitImpl[] hitArray = new DocHitImpl[nFound];
    float maxDocScore = 0.0f;
    for( int i = 0; i < nFound; i++ ) {
        int index = nFound - i - 1;
        hitArray[index] = (DocHitImpl) hitQueue.pop();
    }
    
    int nHits = Math.min( nFound, hitMaxDocs );
    group.docHits = new DocHit[nHits];
    
    group.totalDocs = hitTotalDocs;
    group.startDoc  = hitStartDoc;
    group.endDoc    = hitStartDoc + nHits;

    for( int i = hitStartDoc; i < nFound; i++ )
        group.docHits[i-hitStartDoc] = hitArray[i];
    
  } // makeDocHits()
  
} // class GroupCounts
