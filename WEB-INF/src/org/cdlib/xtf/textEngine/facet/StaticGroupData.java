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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;
import java.util.WeakHashMap;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermPositions;
import org.apache.lucene.index.TermEnum;
import org.cdlib.xtf.util.IntegerValues;

/**
 * This class contains the mapping, for a given field, from documents to
 * one or more term values in that document.
 * 
 * @author Martin Haye
 */
public class StaticGroupData extends GroupData 
{
  /** The particular field we have data from */
  private String   field;
  
  /** Array of document IDs */
  private int[]    docs;
  
  /** 
   * Array of links: 0..docs.length is either positive to indicate a single group
   * for this doc, or negative to indicate a link later in the array to a list
   * of groups. docs.length..links.length holds the extra groups; each entry is
   * a group number, negative to mean end of the groups for a single doc.
   */ 
  private int[]    links;
  
  /** Array of group names */
  private String[] groups;
  
  /** The parent of each group, or -1 for none */
  private int[]    groupParents;
  
  /** The first child of each group, or -1 for none. */
  private int[]    groupChildren;
  
  /** The next sibling of each group, or -1 for none. */
  private int[]    groupSiblings;
  
  /** Cached data. If the reader goes away, our cache will too. */
  private static WeakHashMap cache = new WeakHashMap();
  
  /**
   * Retrieves GroupData for a given field from a given reader. Maintains a cache
   * so that if the same field is requested again for this reader, we don't have
   * to re-read the group data.
   * 
   * @param reader  Where to read the data from
   * @param field   Which field to read
   * @return        Group data for the specified field
   */
  public static StaticGroupData getCachedData( IndexReader reader, String field )
    throws IOException
  {
    // See if we have a cache for this reader.
    HashMap readerCache = (HashMap) cache.get( reader );
    if( readerCache == null ) {
        readerCache = new HashMap();
        cache.put( reader, readerCache );
    }
    
    // Now see if we've already read data for this field.
    StaticGroupData data = (StaticGroupData) readerCache.get( field );
    if( data == null )
    {
        // Don't have cached data, so read and remember it.
        data = new StaticGroupData( reader, field );
        readerCache.put( field, data );
    }
    
    return data;
    
  } // getCachedData()
  
  /**
   * Read in the term data for a given field, and build up the various arrays
   * of document to group info, and hierarchical relationships between the
   * groups.
   * 
   * @param reader    Where to read the term data from
   * @param field     Which field to read
   */
  public StaticGroupData( IndexReader reader, String field ) throws IOException
  { 
    this.field = field;
   
    TermPositions termPositions = reader.termPositions();
    TermEnum      termEnum      = reader.terms(new Term(field, ""));
    
    HashMap groupMap = new HashMap();
    Vector  groupVec = new Vector();
    HashMap childMap = new HashMap();
    HashMap docMap   = new HashMap();
    HashSet lcTerms  = new HashSet();       
    int     nLinks   = 0;
    
    // Add a default root group.
    groupVec.add( "".intern() );
    groupMap.put( "".intern(), IntegerValues.valueOf(0) );

    // Make an entry for each document and each term. Ensure that
    // there is only one term in this field per document.
    //
    try {
        if( termEnum.term() == null )
            throw new RuntimeException( "no terms in field " + field );
  
        do {
            Term term = termEnum.term();
            if( !term.field().equals(field) )
                break;
            
            // If we've seen this term before, skip it. This can happen if
            // the real term was mixed case, and we encounter the lower-case
            // version later.
            //
            String lcTerm = term.text().toLowerCase();
            if( lcTerms.contains(lcTerm) )
                continue;
            lcTerms.add( lcTerm );
    
            // Add a group key for this term. Also, if it's hierarchical,
            // find the ancestor groups and add them to the child map.
            //
            Integer termKey = addTermKey( term.text(), groupVec, groupMap, 
                                          childMap );
            
            // Now process each document which contains this term.
            termPositions.seek( termEnum );
            while( termPositions.next() ) 
            {
                // Get or create a vector for this document.
                int docId = termPositions.doc();
                Integer docKey = IntegerValues.valueOf(docId);
                
                Vector docGroups = (Vector) docMap.get( docKey );
                if( docGroups == null ) {
                    docGroups = new Vector( 1 );
                    docMap.put( docKey, docGroups );
                }
                
                // If we're going from one group to two, add extra link space
                // for the initial link.
                //
                if( docGroups.size() == 1 )
                    nLinks++;
                
                docGroups.add( termKey );
                nLinks++;
            } // while( termPositions.next() )
        } while (termEnum.next());
    } finally {
        termPositions.close();
        termEnum.close();
    }

    // Build the final array of groups. Basically we just take the last
    // component of each path.
    //
    groups = (String[]) groupVec.toArray( new String[groupVec.size()] );
    for( int i = 0; i < groups.length; i++ ) {
        int lastSep = groups[i].lastIndexOf( "::" );
        if( lastSep >= 0 )
            groups[i] = groups[i].substring( lastSep+2 );
    }
    
    // Build the group parent/child/sibling tables.
    buildHierarchy( childMap );
    
    // Now we're ready to build our final arrays that condense all the
    // document -> group information.
    //
    docs   = new int[docMap.size()];
    links  = new int[nLinks];
    buildLinks( docMap );
    
  } // constructor

  /**
   * Add the given term to the group vector and map. If it's hierarchical,
   * add relationships for the parent and all ancestors as well.
   * 
   * @param termText    Term to add
   * @param groupVec    Vector of groups in sort order
   * @param groupMap    Mapping of terms to group numbers
   * @param childMap    Mapping of parent key to child vector
   * @return            New key for the term
   */
  private Integer addTermKey( String  termText, 
                              Vector  groupVec, 
                              HashMap groupMap,
                              HashMap childMap )
  {
    String  curName   = termText;
    Integer childKey  = null;
    Integer termKey   = null;
    while( true ) 
    {
        // Find or make a key for the current name.
        String  parentName = curName.intern();
        Integer parentKey  = (Integer) groupMap.get( parentName );
        if( parentKey == null ) {
            parentKey = IntegerValues.valueOf( groupVec.size() ); 
            groupVec.add( parentName );
            groupMap.put( parentName, parentKey );
        }
        
        // If this is the first go-round, record the new key.
        if( termKey == null )
            termKey = parentKey;
        
        // On the second and subsequent go-rounds, record the relationship 
        // between the parent and its child
        //
        else {
            HashSet parentChildSet = (HashSet) childMap.get( parentKey );
            if( parentChildSet == null ) {
                parentChildSet = new HashSet();
                childMap.put( parentKey, parentChildSet );
            }
            parentChildSet.add( childKey );
        }
        
        // Stop when we reach the root.
        if( curName.length() == 0 )
            break;

        // Go up one level in the hierarchy.
        childKey = parentKey;
        int lastColon = curName.lastIndexOf( "::" );
        if( lastColon >= 0 )
            curName  = curName.substring( 0, lastColon );
        else
            curName = "";
    }
    
    // Return the first key we made (for the term itself, not its ancestors.)
    return termKey;
    
  } // addTermKey()

  /**
   * Based on a hierarchy data map, build the parent, child, and sibling 
   * relationship arrays that make all this info easy to find and fast to 
   * traverse.
   * 
   * @param childMap    Map of parent key to vector of child keys
   */
  private void buildHierarchy( HashMap childMap )
  {
    groupParents = new int[groups.length];
    Arrays.fill( groupParents, -1 );
    
    groupChildren = new int[groups.length];
    Arrays.fill( groupChildren, -1 );

    groupSiblings = new int[groups.length];
    Arrays.fill( groupSiblings, -1 );

    for( Iterator iter = childMap.keySet().iterator(); iter.hasNext(); ) {
        Integer parentKey = (Integer) iter.next();
        int     parent    = parentKey.intValue();
        HashSet childSet  = (HashSet) childMap.get( parentKey );
        
        assert groupChildren[parent] < 0 : "multiple child lists for parent"; 

        int prev = -1;
        ArrayList children = new ArrayList( childSet );
        Collections.sort( children );
        for( int i = 0; i < children.size(); i++ ) {
            int child = ((Integer) children.get(i)).intValue();
            groupParents[child] = parent;
            assert child != prev;
            if( prev < 0 )
                groupChildren[parent] = child;
            else
                groupSiblings[prev] = child;
            prev = child;
        }
    }
  } // buildHierarchy()
  
  /**
   * Perform the final build step, forming the 'docs' and 'links' arrays.
   * 
   * @param docMap  Map of document ID to vector of group IDs
   */
  private void buildLinks( HashMap docMap )
  {
    // Get an array of all the documents, sorted by document ID.
    ArrayList keyList = new ArrayList( docMap.keySet() );
    assert keyList.size() == docs.length : "incorrect calculation";
    Collections.sort( keyList );

    int topLink = docs.length;

    for( int i = 0; i < docs.length; i++ ) {
        Integer docKey    = (Integer)keyList.get( i );
        Vector  docGroups = (Vector) docMap.get( docKey );
        int docNum = docKey.intValue();
        docs[i] = docNum;
        
        // Two cases. If there's only one group, record it directly. Otherwise,
        // record a link to a list of groups.
        //
        if( docGroups.size() == 1 )
            links[i] = ((Integer)docGroups.get(0)).intValue();
        else {
            links[i] = -topLink;
            for( Iterator iter = docGroups.iterator(); iter.hasNext(); ) {
                int groupNum = ((Integer)iter.next()).intValue();
                if( !iter.hasNext() )
                    groupNum = -groupNum;
                links[topLink++] = groupNum;
            }
        }
    }
    
    // We should have just the right number of links; no more, no less.
    assert topLink == links.length : "incorrect calculation";
    
  } // buildLinks()
    
  /**
   * Return the ID of the first link for the given document, or -1 if there
   * are no links for that document.
   * 
   * @param docId document to look for 
   * @return      the first link ID, or -1 if none
   */
  public final int firstLink( int docId )
  {
    int found = Arrays.binarySearch( docs, docId );
    if( found < 0 || found >= docs.length )
        return -1;
    
    if( links[found] >= 0 )
        return found;
    else
        return -links[found];
  } // getDocLink()
  
  /** Return the ID of the link after the specified one, or -1 if no more */
  public final int nextLink( int linkId )
  {
    if( linkId < docs.length )
        return -1;
    else if( links[linkId] < 0 )
        return -1;
    else
        return linkId + 1;
  } // getNextLink()
  
  /** Returns the group number of the specified link */
  public final int linkGroup( int linkId )
  {
    int n = links[linkId];
    return (n < 0) ? -n : n;
  } // getLinkGroup()

  /** Get the name of the grouping field */
  public final String field() {
    return field;
  }
  
  /** Get the total number of groups */
  public final int nGroups() {
    return groups.length;
  }
  
  /** Get the name of a group given its number */
  public final String name( int groupId ) { 
    return groups[groupId]; 
  }
  
  /** Get the parent of the given group, or -1 if group is the root */
  public final int parent( int groupId ) {
    return groupParents[groupId]; 
  }

  /** Get the number of children a group has */
  public final int nChildren( int groupId ) {
    int nChildren = 0;
    for( int kid = groupChildren[groupId]; kid >= 0; kid = groupSiblings[kid] )
        nChildren++;
    return nChildren;
  }
  
  /** Get the first child of the given group, or -1 if it has no children */
  public final int child( int groupId ) {
    return groupChildren[groupId]; 
  }

  /** Get the sibling of the given group, or -1 if no more */
  public final int sibling( int groupId ) {
    return groupSiblings[groupId]; 
  }
  
  /** Locate a group by name and return its index, or -1 if not found */
  public final int findGroup( String name ) {
    name = name.intern();
    for( int i = 0; i < groups.length; i++ ) {
        if( name == groups[i] )
            return i;
    }
    return -1;
  }
  
} // GroupData
