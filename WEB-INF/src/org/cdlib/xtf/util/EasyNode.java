package org.cdlib.xtf.util;

import java.util.Vector;

import net.sf.saxon.om.Axis;
import net.sf.saxon.om.AxisIterator;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.type.Type;

/*
 * Copyright (c) 2005, Regents of the University of California
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
 * Provides an easy-to-use wrapper around a Saxon NodeInfo.
 * 
 * @author Martin Haye
 */
public class EasyNode 
{
  /** The node we are wrapping */
  private NodeInfo wrapped;
  
  /** Attribute names for this node */
  private Vector attrNames;
  
  /** Attribute values for this node */
  private Vector attrValues;
  
  /** Child elements for this node */
  private Vector children;
  
  /** Wrap a NodeInfo */
  public EasyNode( NodeInfo toWrap ) {
    wrapped = toWrap;
  }
  
  /**
   * Iterate the attributes and fill the 'attrNames' and 'attrValues' vectors.
   * This is a memo function and only has to do actual work once.
   */
  private void getAttrs() 
  {
    if( attrNames != null )
        return;
    
    attrNames = new Vector();
    attrValues = new Vector();
    
    NodeInfo attr;
    AxisIterator iter = wrapped.iterateAxis( Axis.ATTRIBUTE );
    while( (attr = (NodeInfo)iter.next()) != null ) {
        String name = attr.getLocalPart();
        String val  = attr.getStringValue();
        attrNames.add( name );
        attrValues.add( val );
    }
  } // getAttribs()
  
  /**
   * Iterate the children and fill the 'children' vector. This is a memo
   * function and only has to do actual work once.
   */
  private void getChildren()
  {
    if( children != null )
        return;
    
    children = new Vector();
    
    NodeInfo child;
    AxisIterator iter = wrapped.iterateAxis( Axis.CHILD );
    while( (child = (NodeInfo)iter.next()) != null ) {
        if( child.getNodeKind() == Type.ELEMENT ||
            child.getNodeKind() == Type.TEXT )
        {
            children.add( new EasyNode(child) );
        }
    }
  } // getChildren()
  
  /** Get the number of attributes this node has */
  public int nAttrs()
  {
    getAttrs();
    return attrNames.size();
  } // nAttrs()
  
  /** Get a specific numbered attribute's name */
  public String attrName( int index )
  {
    getAttrs();
    return (String) attrNames.get( index );
  } // attrName()
  
  /** Get a specific numbered attribute's value */
  public String attrValue( int index )
  {
    getAttrs();
    return (String) attrValues.get( index );
  } // attrName()
  
  /** Check if this node has the given attribute */
  public boolean hasAttr( String name )
  {
    return attrValue(name) != null;
  } // hasAttr()
  
  /** Get a named attribute's value, or null if no such name found. The
   *  name matching is performed case-insensitive. */
  public String attrValue( String name )
  {
    getAttrs();
    for( int i = 0; i < attrNames.size(); i++ ) {
      if( ((String)attrNames.get(i)).equalsIgnoreCase(name) )
          return (String) attrValues.get( i );
    }
    return null;
  } // attrValue()
  
  /** Get a count of the number of children this node has */
  public int nChildren()
  {
    getChildren();
    return children.size();
  } // nChildren()
  
  /** Get a specific numbered child of this node */
  public EasyNode child( int index )
  {
    getChildren();
    return (EasyNode) children.get(index);
  }
  
  /** Get the first child node with the specified name, case insensitive.
   *  If no such child is found, returns null. */
  public EasyNode child( String name )
  {
    getChildren();
    for( int i = 0; i < children.size(); i++ ) {
        if( ((EasyNode)children.get(i)).name().equalsIgnoreCase(name) )
            return (EasyNode) children.get(i);
    }
    return null;
  } // child()
  
  /** Get the parent of this node (if any) */
  public EasyNode parent()
  {
    NodeInfo parent = wrapped.getParent();
    if( parent == null )
        return null;
    return new EasyNode( parent );
  } // parent()
  
  /** Get the name of this node */
  public String name()
  {
    return wrapped.getLocalPart();
  } // getName()
  
  /** Get the actual node we're wrapping */
  public NodeInfo getWrappedNode()
  {
    return wrapped;
  } // getWrappedNode()
  
} // class EasyNode
