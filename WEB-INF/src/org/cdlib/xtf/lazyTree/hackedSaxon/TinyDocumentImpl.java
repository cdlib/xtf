package org.cdlib.xtf.lazyTree.hackedSaxon;
import net.sf.saxon.Configuration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
  * A node in the XML parse tree representing the Document itself (or equivalently, the root
  * node of the Document).<P>
  */

public final class TinyDocumentImpl extends TinyParentNodeImpl
    implements DocumentInfo {

    private HashMap idTable = null;
    private HashMap elementList = null;
    private HashMap entityTable = null;


    public TinyDocumentImpl(TinyTree tree) {
        this.tree = tree;
    }

    /**
     * Get the tree containing this node
     */

    public TinyTree getTree() {
        return tree;
    }

    /**
    * Set the Configuration that contains this document
    */

    public void setConfiguration(Configuration config) {
        if (config != tree.getConfiguration()) {
            throw new IllegalArgumentException(
                    "Configuration of document differs from that of the supporting TinyTree");
        }
    }

    /**
     * Get the configuration previously set using setConfiguration
     */

    public Configuration getConfiguration() {
        return tree.getConfiguration();
    }

 	/**
	* Get the unique document number
	*/

	public int getDocumentNumber() {
	    return tree.getDocumentNumber();
	}

    /**
    * Set the system id of this node
    */

    public void setSystemId(String uri) {
        tree.setSystemId(nodeNr, uri);
    }

    /**
    * Get the system id of this root node
    */

    public String getSystemId() {
        return tree.getSystemId(nodeNr);
    }

    /**
    * Get the base URI of this root node. For a root node the base URI is the same as the
    * System ID.
    */

    public String getBaseURI() {
        return getSystemId();
    }

    /**
    * Get the line number of this root node.
    * @return 0 always
    */

    public int getLineNumber() {
        return 0;
    }

    /**
    * Return the type of node.
    * @return Type.DOCUMENT (always)
    */

    public final int getNodeKind() {
        return Type.DOCUMENT;
    }

    /**
     * Find the parent node of this node.
     * @return The Node object describing the containing element or root node.
     */

    public NodeInfo getParent()  {
        return null;
    }

    /**
    * Get the root node
    * @return the NodeInfo that is the root of the tree - not necessarily a document node
    */

    public NodeInfo getRoot() {
        return this;
    }

    /**
    * Get the root (document) node
    * @return the DocumentInfo representing the document node, or null if the
    * root of the tree is not a document node
    */

    public DocumentInfo getDocumentRoot() {
        return this;
    }

    /**
    * Get a character string that uniquely identifies this node
    * @return an identifier based on the document number
    */

    public String generateId() {
        return "d"+getDocumentNumber();
    }

    /**
    * Get a list of all elements with a given name. This is implemented
    * as a memo function: the first time it is called for a particular
    * element type, it remembers the result for next time.
    */

    AxisIterator getAllElements(int fingerprint) {
    	Integer key = new Integer(fingerprint);
    	if (elementList==null) {
    	    elementList = new HashMap(20);
    	}
        List list = (List)elementList.get(key);
        if (list==null) {
            list = getElementList(fingerprint);
            elementList.put(key, list);
        }
        return new ListIterator(list);
    }

    /**
     * Get a list containing all the elements with a given element name
     * @param fingerprint the fingerprint of the element name
     * @return list a List containing the TinyElementImpl objects
     */

    List getElementList(int fingerprint) {
        List list = new ArrayList(tree.getNumberOfNodes()/20);
        int i = nodeNr+1;
        while (tree.depth[i] != 0) {
            if (tree.nodeKind[i]==Type.ELEMENT &&
                    (tree.nameCode[i] & 0xfffff) == fingerprint) {
                list.add(tree.getNode(i));
            }
            i++;
        }
        return list;
    }

    /**
    * Register a unique element ID. Fails if there is already an element with that ID.
    * @param e The NodeInfo (always an element) having a particular unique ID value
    * @param id The unique ID value. The caller is responsible for checking that this
     * is a valid NCName.
    */

    void registerID(NodeInfo e, String id) {
        if (idTable==null) {
            idTable = new HashMap(256);
        }

        // the XPath spec (5.2.1) says ignore the second ID if it's not unique
        NodeInfo old = (NodeInfo)idTable.get(id);
        if (old==null) {
            idTable.put(id, e);
        }

    }

    /**
    * Get the element with a given ID.
    * @param id The unique ID of the required element, previously registered using registerID()
    * @return The NodeInfo (always an Element) for the given ID if one has been registered,
    * otherwise null.
    */

    public NodeInfo selectID(String id) {
        if (idTable==null) return null;			// no ID values found
        return (NodeInfo)idTable.get(id);
    }

    /**
    * Set an unparsed entity URI associated with this document. For system use only, while
    * building the document.
    */

    void setUnparsedEntity(String name, String uri, String publicId) {
        if (entityTable==null) {
            entityTable = new HashMap(20);
        }
        String[] ids = new String[2];
        ids[0] = uri;
        ids[1] = publicId;
        entityTable.put(name, ids);
    }

    /**
    * Get the unparsed entity with a given nameID if there is one, or null if not. If the entity
    * does not exist, return null.
    * @param name the name of the entity
    * @return if the entity exists, return an array of two Strings, the first holding the system ID
    * of the entity, the second holding the public
    */

    public String[] getUnparsedEntity(String name) {
        if (entityTable==null) {
            return null;
        }
        return (String[])entityTable.get(name);
    }

    /**
    * Copy this node to a given outputter
    */

    public void copy(Receiver out, int whichNamespaces, boolean copyAnnotations, int locationId) throws XPathException {

        out.startDocument(0);

        // output the children

        AxisIterator children = iterateAxis(Axis.CHILD);
        while (true) {
            NodeInfo n = (NodeInfo)children.next();
            if (n == null) {
                break;
            }
            n.copy(out, whichNamespaces, copyAnnotations, locationId);
        }

        out.endDocument();
    }

    public void showSize() {
        tree.showSize();
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
// The Original Code is: all this file 
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//
