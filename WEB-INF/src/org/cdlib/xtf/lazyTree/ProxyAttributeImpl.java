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

import javax.xml.transform.TransformerException;

import net.sf.saxon.event.Receiver;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.type.Type;

import org.w3c.dom.Attr;
import org.w3c.dom.Node;

/**
 * Represents an attribute node from a persistent XML document. Used by
 * {@link ProxyElement} to implement lazy attribute loading.
 * 
 * @author Martin Haye
 */
class ProxyAttributeImpl extends NodeImpl implements Attr
{
    ProxyElement element;
    int          index;
    
    /**
     * Construct an Attribute node for the n'th attribute of a given element
     * @param element The element containing the relevant attribute
     * @param index The index position of the attribute starting at zero
     */
    public ProxyAttributeImpl( ProxyElement element, int index ) {
        super( element.document );
        parentNum = element.nodeNum;
        this.element = element;
        this.index   = index;
    }

    /**
     * Get the name code, which enables the name to be located in the name pool
     */
    public int getNameCode() {
        return element.attrNames[index];
    }

    /**
     * Get the root node of the tree (not necessarily a document node)
     * @return the NodeInfo representing the root of this tree
     */
    public NodeInfo getRoot() {
        return element.document.getRoot();
    }

    /**
     * Get the root (document) node
     * @return the DocumentInfo representing the containing document
     */

    public DocumentInfo getDocumentRoot() {
        return element.document.getDocumentRoot();
    }

    /**
     * Get the NamePool for the tree containing this node
     * @return the NamePool
     */

    public NamePool getNamePool() {
        return element.document.getNamePool();
    }

    /**
     * Determine whether this is the same node as another node
     * @return true if this Node object and the supplied Node object represent the
     * same node in the tree.
     */
    public boolean isSameNode( NodeInfo other ) {
        if( this == other ) 
            return true;
        if( !(other instanceof ProxyAttributeImpl) ) 
            return false;
        ProxyAttributeImpl otherAtt = (ProxyAttributeImpl)other;
        return (element.isSameNode(otherAtt.element) &&
                index == otherAtt.index);
    }

    /**
     * Get the node sequence number (in document order). Sequence numbers are monotonic but not
     * consecutive. In the current implementation, parent nodes (elements and roots) have a zero
     * least-significant word, while namespaces, attributes, text nodes, comments, and PIs have
     * the top word the same as their owner and the bottom half reflecting their relative position.
     */

    protected long getSequenceNumber() {
        return element.getSequenceNumber() + 0x8000 + index;
        // note the 0x8000 is to leave room for namespace nodes
    }

    /**
     * Return the type of node.
     * @return Node.ATTRIBUTE
     */

     public final int getNodeKind() {
         return Type.ATTRIBUTE;
     }

    /**
     * Return the character value of the node.
     * @return the attribute value
     */

    public String getStringValue() {
        return element.attrValues[index];
    }

    /**
     * Get next sibling - not defined for attributes
     */

    public Node getNextSibling() {
        return null;
    }

    /**
     * Get previous sibling - not defined for attributes
     */

    public Node getPreviousSibling() {
        return null;
    }

    /**
     * Get the previous node in document order (skipping attributes)
     */

    public NodeImpl getPreviousInDocument() {
        return (NodeImpl)getParent();
    }

    /**
     * Get the next node in document order (skipping attributes)
     */

    public NodeImpl getNextInDocument(NodeImpl anchor) {
        if (anchor==this) return null;
        return ((NodeImpl)getParent()).getNextInDocument(anchor);
    }

    /**
     * Get sequential key. Returns key of owning element with the attribute name as a suffix
     */

    public String generateId() {
        return element.generateId() + "_" + getDisplayName();
    }
    
    /**
     * Obtain the displayable name of this attribute.
     */
    public String getDisplayName()
    {
        if( getNameCode() < 0 ) 
            return "";
        return getNamePool().getDisplayName(getNameCode());
    }

    /**
     * Copy this node to a given outputter
     */

    public void copy( Receiver out, int whichNamespaces,
                      boolean copyAnnotations ) 
        throws TransformerException 
    {
        int nameCode = getNameCode();
        //if ((nameCode>>20 & 0xff) != 0) { // non-null prefix
        // check there is no conflict of namespaces
        //  nameCode = out.checkAttributePrefix(nameCode);
        //}
        out.attribute(nameCode, 0, getStringValue(), 0);
    }
    
} // class ProxyAttributeImpl
