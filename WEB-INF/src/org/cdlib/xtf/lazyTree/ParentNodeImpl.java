package org.cdlib.xtf.lazyTree;

/**
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

import org.w3c.dom.Node;

import net.sf.saxon.om.Axis;
import net.sf.saxon.om.AxisIterator;
import net.sf.saxon.pattern.NodeTest;

/**
 * Represents any node that can have children.
 * 
 * @author Martin Haye
 */
abstract class ParentNodeImpl extends NodeImpl
{
    int          childNum;
    
    public ParentNodeImpl( LazyDocument document ) {
        super( document );
    }

    /**
     * Get an enumeration of the children of this node
     * 
     * @param test A NodeTest to be satisfied by the child nodes, or null
     * if all child node are to be returned
     */
    public final AxisIterator enumerateChildren( NodeTest test ) 
    {
        return new ChildEnumeration( this, test );
    }
    
    /**
     * Determine if the node has children.
     */
    public boolean hasChildNodes() {
        return childNum >= 0;
    }

    /**
     * Get first child (DOM method)
     * 
     * @return the first child node of this node, or null if it has no children
     */
    public Node getFirstChild()  {
        return document.getNode( childNum );
    }

    /**
     * Return the string-value of the node, that is, the concatenation
     * of the character content of all descendent elements and text nodes.
     * 
     * @return the accumulated character content of the element, including 
     * descendant elements.
     */
    public String getStringValue() 
    {
        StringBuffer sb = null;

        AxisIterator iter = iterateAxis( Axis.DESCENDANT );
        while( true ) {
            NodeImpl node = (NodeImpl) iter.next();
            if( node == null )
                break;
            if( !(node instanceof TextImpl) )
                continue;
            if( sb == null )
                sb = new StringBuffer();
            sb.append( node.getStringValue() );
        }

        if( sb == null ) 
            return "";
        return sb.toString();
    } // getStringValue()
    
} // class ParentNodeImpl
