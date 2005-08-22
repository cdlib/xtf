package org.cdlib.xtf.lazyTree;

import net.sf.saxon.om.Axis;
import net.sf.saxon.om.AxisIterator;
import net.sf.saxon.om.NodeInfo;
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
    public NodeInfo getFirstChild()  {
        return document.getNode( childNum );
    }

    /** The last child of this Node, or null if none. */
    public NodeInfo getLastChild() {

        NodeInfo last = getFirstChild();
        if( last != null ) {
            while( true ) {
                NodeInfo next = ((NodeImpl)last).getNextSibling();
                if( next == null )
                    break;
                last = next;
            }
        }
        return last;

    } // getLastChild()

    /**
    * Return the string-value of the node, that is, the concatenation
    * of the character content of all descendent elements and text nodes.
    * @return the accumulated character content of the element, including descendant elements.
    */
    public final String getStringValue() {
        return getStringValueCS().toString();
    }

    /**
     * Get the value of the item as a CharSequence. This is in some cases more efficient than
     * the version of the method that returns a String.
     */

    public CharSequence getStringValueCS() {
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


//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
