package org.cdlib.xtf.lazyTree.hackedSaxon;
import net.sf.saxon.om.*;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.type.Type;

import org.w3c.dom.Attr;

import net.sf.saxon.xpath.XPathException;


/**
  * A node in the XML parse tree representing an attribute. Note that this is
  * generated only "on demand", when the attribute is selected by a select pattern.<P>
  * @author Michael H. Kay
  */

final class TinyAttributeImpl extends TinyNodeImpl implements Attr {

    public TinyAttributeImpl(TinyDocumentImpl doc, int nodeNr) {
        this.document = doc;
        this.nodeNr = nodeNr;
    }

    /**
    * Get the parent node
    */

    public NodeInfo getParent() {
        return document.getNode(document.attParent[nodeNr]);
    }

    /**
    * Get the node sequence number (in document order). Sequence numbers are monotonic but not
    * consecutive. In the current implementation, elements have a zero
    * least-significant word, and attributes and namespaces use the same value in the top word as
    * the containing element, and use the bottom word to hold
    * a sequence number, which numbers namespaces first and then attributes.
    */

    protected long getSequenceNumber() {
        // need the variable as workaround for a Java HotSpot problem, reported 11 Oct 2000
        long z =
            ((TinyNodeImpl)getParent()).getSequenceNumber()
            + 0x8000 +
            (nodeNr - document.alpha[document.attParent[nodeNr]]);
        return z;
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
        return document.attValue[nodeNr].toString();
    }

	/**
	* Get the fingerprint of the node, used for matching names
	*/

	public int getFingerprint() {
		return document.attCode[nodeNr] & 0xfffff;
	}

	/**
	* Get the name code of the node, used for finding names in the name pool
	*/

	public int getNameCode() {
		return document.attCode[nodeNr];
	}

    /**
    * Get the prefix part of the name of this node. This is the name before the ":" if any.
    * @return the prefix part of the name. For an unnamed node, return null.
    */

    public String getPrefix() {
    	int code = document.attCode[nodeNr];
    	if ((code>>20 & 0xff) == 0) return "";
    	return document.getNamePool().getPrefix(code);
    }

    /**
    * Get the display name of this node. For elements and attributes this is [prefix:]localname.
    * For unnamed nodes, it is an empty string.
    * @return The display name of this node.
    * For a node with no name, return an empty string.
    */

    public String getDisplayName() {
        return document.getNamePool().getDisplayName(document.attCode[nodeNr]);
    }


    /**
    * Get the local name of this node.
    * @return The local name of this node.
    * For a node with no name, return an empty string.
    */

    public String getLocalPart() {
        return document.getNamePool().getLocalName(document.attCode[nodeNr]);
    }

    /**
    * Get the URI part of the name of this node.
    * @return The URI of the namespace of this node. For the default namespace, return an
    * empty string
    */

    public final String getURI() {
        return document.getNamePool().getURI(document.attCode[nodeNr]);
    }

    /**
    * Get the type annotation of this node, if any
    * Returns 0 if there is no type annotation
    */

    public int getTypeAnnotation() {
        return document.getAttributeAnnotation(nodeNr);
    }

    /**
    * Generate id. Returns key of owning element with the attribute namecode as a suffix
    */

    public String generateId() {
        return (getParent()).generateId() + "a" + document.attCode[nodeNr];
        // we previously used the attribute name. But this breaks the requirement
        // that the result of generate-id consists entirely of alphanumeric ASCII
        // characters
    }

    /**
    * Copy this node to a given outputter
    */

    public void copy(Receiver out, int whichNamespaces, boolean copyAnnotations, int locationId) throws XPathException {
		int nameCode = document.attCode[nodeNr];
		int typeCode = (copyAnnotations ? getTypeAnnotation() : -1);
        out.attribute(nameCode, typeCode, getStringValue(), 0, 0);
    }

    /**
    * Get the line number of the node within its source document entity
    */

    public int getLineNumber() {
        return getParent().getLineNumber();
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
