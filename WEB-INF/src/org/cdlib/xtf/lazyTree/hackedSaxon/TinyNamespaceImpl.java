package org.cdlib.xtf.lazyTree.hackedSaxon;
import net.sf.saxon.om.*;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.ReceiverOptions;
import net.sf.saxon.type.Type;

import net.sf.saxon.xpath.XPathException;
/**
  * A node in the XML parse tree representing a Namespace. Note that this is
  * generated only "on demand", when the namespace axis is expanded.<P>
  * @author Michael H. Kay
  * @version 28 September 2000
  */

final class TinyNamespaceImpl extends TinyNodeImpl {

    private int parentNode;     // an entry in the namespace array corresponds
                                // to a namespace declaration. This can result in one
                                // namespace node for each ancestor element. Therefore
                                // the namespace node needs to contain a reference to the
                                // actual parent element.
	private int nameCode;		// the name code of the name of the namespace node.
								// The name of the namespace node is the prefix: the
								// namecode is NOT the same as the namespace code, which
								// identifies the prefix/uri pair

    public TinyNamespaceImpl(TinyDocumentImpl doc, int nodeNr) {
        document = doc;
        this.nodeNr = nodeNr;
        nameCode = document.getNamePool().allocate("", "", getLocalPart());
    }

    /**
    * Get the namespace code (a numeric representation of the prefix and URI)
    */

    public int getNamespaceCode() {
        return document.namespaceCode[nodeNr];
    }

    /**
    * Get the fingerprint
    */

    public int getFingerprint() {
        return nameCode & 0xfffff;
    }

    /**
    * Set the parent element for this namespace node
    */

    protected void setParentNode(int nodeNr) {
        parentNode = nodeNr;
    }

	/**
	* Get the nameCode, for name matching
	*/

	public int getNameCode() {
		return nameCode;
	}

    /**
    * Get the prefix part of the name of this node. This is the name before the ":" if any.
    * @return the prefix part of the name. Always null.
    */

    public String getPrefix() {
        return null;
    }

    /**
    * Get the display name of this node. For namespaces this is the namespace prefix.
    * @return The display name of this node.
    * For a node with no name, return an empty string.
    */

    public String getDisplayName() {
        return getLocalPart();
    }

    /**
    * Get the local name of this node. For namespaces this is the namespace prefix.
    * @return The local name of this node. Returns "" for the default namespace.
    */

    public String getLocalPart() {
        return document.getNamePool().getPrefixFromNamespaceCode(
        				document.namespaceCode[nodeNr]);
    }

    /**
    * Get the URI part of the name of this node.
    * @return The URI of the namespace of this node. Always the empty string.
    */

    public String getURI() {
        return "";
    }

    /**
    * Get the parent element of this namespace node
    */

    public NodeInfo getParent() {
        return document.getNode(parentNode);
    }

    /**
    * Determine whether this is the same node as another node
    * @return true if this Node object and the supplied Node object represent the
    * same node in the tree.
    */

    public final boolean isSameNodeInfo(NodeInfo other) {
        if (!(other instanceof TinyNamespaceImpl)) return false;
        if (this==other) return true;
        TinyNamespaceImpl otherN = (TinyNamespaceImpl)other;
        return (this.parentNode==((TinyNamespaceImpl)other).parentNode &&
             this.document==otherN.document &&
             this.nodeNr==((TinyNamespaceImpl)other).nodeNr);
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

    public final String getStringValue() {
        return document.getNamePool().getURIFromNamespaceCode(
        				document.namespaceCode[nodeNr]);
    }

    /**
     * Get the base URI for the node. In XPath 2.0, the base URI of a namespace node
     * is (), which we represent as null.
    */

    public String getBaseURI() {
        return null;
    }

    /**
    * Get unique identifier. Returns key of owning element with the namespace prefix as a suffix
    */

    public String generateId() {
        return (getParent()).generateId() + "n" + getNameCode();
        // we previously used the namespace prefix as part of the id, but this breaks
        // the rule that the result of generate-id() must consist entirely of alphanumeric
        // ASCII characters.
    }

    /**
    * Copy this node to a given outputter
    */

    public void copy(Receiver out, int whichNamespaces, boolean copyAnnotations, int locationId) throws XPathException {
		out.namespace(getNamespaceCode(), ReceiverOptions.REJECT_DUPLICATES);
    }

    /**
    * Get the node sequence number (in document order). Sequence numbers are monotonic but not
    * consecutive. In the current implementation, parent nodes (elements and roots) have a zero
    * least-significant word, while namespaces, attributes, text nodes, comments, and PIs have
    * the top word the same as their owner and the bottom half reflecting their relative position.
    */

    protected long getSequenceNumber() {
        return ((TinyNodeImpl)getParent()).getSequenceNumber() + nodeNr + 1;
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
// The Original Code is: most of this file. 
//
// The Initial Developer of the Original Code is
// Michael Kay of International Computers Limited (michael.h.kay@ntlworld.com).
//
// Portions created by Martin Haye are Copyright (C) Regents of the University 
// of California. All Rights Reserved. 
//
// Contributor(s): Martin Haye. 
//
