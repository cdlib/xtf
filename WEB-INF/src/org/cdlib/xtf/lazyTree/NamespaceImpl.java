package org.cdlib.xtf.lazyTree;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.ReceiverOptions;
import net.sf.saxon.type.Type;
import net.sf.saxon.xpath.XPathException;

import org.w3c.dom.Node;

/**
  * A node in the XML parse tree representing a Namespace. Note that this is
  * generated only "on demand", when the namespace axis is expanded.<P>
  * @author Michael H. Kay
  * @version 3 November 1999
  */

final class NamespaceImpl extends NodeImpl {

    private NodeImpl parent;
    private int nsCode;     // indexes the prefix and uri in the name pool
    private int nameCode;   // identifies the name of this node
    private int index;

    /**
    * Construct a Namespace node
    * @param element The element owning the namespace node
    * @param nsCode The namespace code
    * @param index Integer identifying this namespace node among the nodes for its parent
    */

    public NamespaceImpl(NodeImpl element, int nsCode, int index) {
        super( element.document );
        this.parent = element;
        this.nsCode = nsCode;
        NamePool pool = getNamePool();
        this.nameCode = pool.allocate("", "", pool.getPrefixFromNamespaceCode(nsCode));
        this.index = index;
    }
    
    public void setParentNode( NodeImpl parent ) {
        this.parent = parent;
    }

    /**
    * Get the namecode for this name. Not the same as the namespace code!
    */

    public int getNameCode() {
        return nameCode;
    }

    /**
    * Get the namespace code for this prefix/uri pair. Not the same as the name code!
    */

    public int getNamespaceCode() {
        return nsCode;
    }

    /**
    * Determine whether this is the same node as another node
    * @return true if this Node object and the supplied Node object represent the
    * same node in the tree.
    */

    public boolean isSameNode(NodeInfo other) {
        if (!(other instanceof NamespaceImpl)) return false;
        if (this==other) return true;
        NamespaceImpl otherN = (NamespaceImpl)other;
        return (parent.isSameNode(otherN.parent) &&
                this.nsCode==otherN.nsCode);
    }

    /**
    * Get the prefix of the namespace that this node relates to.
     * @return the namespace prefix, or "" for the default namespace
    */

    public String getLocalPart() {
        return getNamePool().getPrefixFromNamespaceCode(nsCode);
    }

    /**
    * Return the type of node.
    * @return Type.NAMESPACE
    */

    public final int getNodeKind() {
        return Type.NAMESPACE;
    }

    /**
    * Return the string value of the node.
    * @return the namespace uri
    */

    public String getStringValue() {
        return getNamePool().getURIFromNamespaceCode(nsCode);
    }

    /**
    * Get the name of this node, following the DOM rules (which aren't actually defined
    * for Namespace nodes...)
    * @return the namespace prefix
    */

    public String getNodeName() {
        return getLocalPart();
    }

    /**
    * Get next sibling - not defined for namespace nodes
    */

    public Node getNextSibling() {
        return null;
    }

    /**
    * Get previous sibling - not defined for namespace nodes
    */

    public Node getPreviousSibling() {
        return null;
    }

    /**
    * Get the previous node in document order (skipping namespace nodes)
    */

    public NodeImpl getPreviousInDocument() {
        return (NodeImpl)getParent();
    }

    /**
    * Get the next node in document order (skipping namespace nodes)
    */

    public NodeImpl getNextInDocument(NodeImpl anchor) {
        if (this==anchor) return null;
        return ((NodeImpl)getParent()).getNextInDocument(anchor);
    }

    /**
     * Get the base URI for the node. In XPath 2.0, the base URI of a namespace node
     * is (), which we represent as null.
    */

    public String getBaseURI() {
        return null;
    }

    /**
    * Get sequential key. Returns key of owning element with the namespace prefix as a suffix
    */

    public String generateId() {
        // generate-id() is required to return ASCII strings
        return parent.generateId() + "ns" + getLocalPart().hashCode();
    }

    /**
    * Copy this node to a given outputter
    */

    public void copy(Receiver out, int whichNamespaces, boolean copyAnnotations, int locationId) throws XPathException {
        out.namespace(nsCode, ReceiverOptions.REJECT_DUPLICATES);
    }

    /**
    * Get the node sequence number (in document order). Sequence numbers are monotonic but not
    * consecutive. In the current implementation, parent nodes (elements and roots) have a zero
    * least-significant word, while namespaces, attributes, text nodes, comments, and PIs have
    * the top word the same as their owner and the bottom half reflecting their relative position.
    */

    protected long getSequenceNumber() {
        return parent.getSequenceNumber() + index;
    }

}

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
