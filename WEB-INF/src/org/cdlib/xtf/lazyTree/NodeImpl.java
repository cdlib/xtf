package org.cdlib.xtf.lazyTree;

// IMPORTANT NOTE: When comparing, this file is most similar to 
//                 Saxon's net.sf.tree.NodeImpl

import java.io.IOException;

import javax.xml.transform.SourceLocator;

import net.sf.saxon.Configuration;
import net.sf.saxon.Err;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.pattern.NameTest;
import net.sf.saxon.pattern.AnyNodeTest;
import net.sf.saxon.om.FingerprintedNode;
import net.sf.saxon.om.NamespaceIterator;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.AxisIterator;
import net.sf.saxon.om.EmptyIterator;
import net.sf.saxon.om.PrependIterator;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.SingletonIterator;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.UntypedAtomicValue;
import net.sf.saxon.event.Receiver;


/**
  * A node in the XML parse tree representing an XML element, character content, or attribute.<P>
  * This is the top-level class in the implementation class hierarchy; it essentially contains
  * all those methods that can be defined using other primitive methods, without direct access
  * to data.
  * 
  * @author Michael Kay, Martin Haye
  */
public abstract class NodeImpl 
    implements NodeInfo, FingerprintedNode, SourceLocator 
{
    LazyDocument document;
    int          nodeNum;
    int          nameCode;
    int          parentNum;
    int          prevSibNum;
    int          nextSibNum;
    
    /**
    * Chararacteristic letters to identify each type of node, indexed using the node type
    * values. These are used as the initial letter of the result of generate-id()
    */
    public static final char[] NODE_LETTER =
        {'x', 'e', 'a', 't', 'x', 'x', 'x', 'p', 'c', 'r', 'x', 'x', 'x', 'n'};

    /** Create a new node and attach it to a document */
    public NodeImpl( LazyDocument document ) {
        this.document = document;
    }
    
    /** Optional initialization function, depends on derived class */
    public void init( int alpha, int beta ) throws IOException { }
    
    /**
     * Get the value of the item as a CharSequence. This is in some cases more efficient than
     * the version of the method that returns a String.
     */
    public CharSequence getStringValueCS() {
        return getStringValue();
    }

    /**
    * Get the type annotation of this node, if any
    */
    public int getTypeAnnotation() {
        return -1;
    }

    /**
    * Get the column number of the node.
    * The default implementation returns -1, meaning unknown
    */
    public int getColumnNumber() {
        return -1;
    }

    /**
    * Get the public identifier of the document entity containing this node.
    * The default implementation returns null, meaning unknown
    */
    public String getPublicId() {
        return null;
    }

    /**
     * Get the typed value of this node.
     * If there is no type annotation, we return the string value, as an instance
     * of xdt:untypedAtomic
    */

    public SequenceIterator getTypedValue() throws XPathException {
        int annotation = getTypeAnnotation();
        if (annotation==-1) {
            return SingletonIterator.makeIterator(new UntypedAtomicValue(getStringValue()));
        } else {
            SchemaType stype = getConfiguration().getSchemaType(annotation);
            if (stype == null) {
                String typeName = getNamePool().getDisplayName(annotation);
                throw new DynamicError("Unknown type annotation " +
                        Err.wrap(typeName) + " in document instance");
            } else {
                return stype.getTypedValue(this);
            }
        }
    }

    /**
    * Set the system ID of this node. This method is provided so that a NodeInfo
    * implements the javax.xml.transform.Source interface, allowing a node to be
    * used directly as the Source of a transformation
    */
    public void setSystemId( String uri ) {
        // overridden in DocumentImpl and ElementImpl
        if( this instanceof AttributeImpl )
            getParent().setSystemId( uri );
        else
            document.setSystemId( nodeNum, uri );
    }

    /**
    * Determine whether this is the same node as another node
    * @return true if this Node object and the supplied Node object represent the
    * same node in the tree.
     */
    public boolean isSameNodeInfo( NodeInfo other ) {
        // default implementation: differs for attribute and namespace nodes   
        if( this==other ) return true;
        if( !(other instanceof NodeImpl) ) return false;
        if( this.document != ((NodeImpl)other).document ) return false;
        if( this.nodeNum != ((NodeImpl)other).nodeNum ) return false;
        if( this.getNodeKind() != other.getNodeKind() ) return false;
        return true;
    }

    /**
     * Get the nameCode of the node. This is used to locate the name in the NamePool
     */
    public int getNameCode() {
        return nameCode;
    }

    /**
    * Get the fingerprint of the node. This is used to compare whether two nodes
    * have equivalent names. Return -1 for a node with no name.
    */

    public int getFingerprint() {
        int nameCode = getNameCode();
        if (nameCode==-1) return -1;
             return nameCode & 0xfffff;
    }

    /**
    * Get a character string that uniquely identifies this node within this document
    * (The calling code will prepend a document identifier)
    * @return a string.
     */
    public String generateId() {
        return "d" + document.generateId() +
                NODE_LETTER[getNodeKind()] +
                nodeNum;
    }
   
    /**
     * Get the document number of the document containing this node
     * (Needed when the document isn't a real node, for sorting free-standing elements)
     */

    public int getDocumentNumber() {
        return document.getDocumentNumber();
    }

    /**
    * Get the system ID for the node. Default implementation for child nodes.
     */
    public String getSystemId() {
        return document.getSystemId( nodeNum );
    }

    /**
    * Get the base URI for the node. Default implementation for child nodes.
     */

    public String getBaseURI() {
        return (getParent()).getBaseURI();
    }

    /**
    * Get the node sequence number (in document order). Sequence numbers are monotonic but not
    * consecutive. In the current implementation, parent nodes (elements and roots) have a zero
    * least-significant word, while namespaces, attributes, text nodes, comments, and PIs have
    * the top word the same as their owner and the bottom half reflecting their relative position.
    * This is the default implementation for child nodes.
     */
    protected long getSequenceNumber() {
        return (long)nodeNum << 32;
    }

    /**
    * Determine the relative position of this node and another node, in document order.
    * The other node will always be in the same document.
    * @param other The other node, whose position is to be compared with this node
    * @return -1 if this node precedes the other node, +1 if it follows the other
    * node, or 0 if they are the same node. (In this case, isSameNode() will always
    * return true, and the two nodes will produce the same result for generateId())
     */

    public final int compareOrder( NodeInfo other ) {
        long a = getSequenceNumber();
        if( other instanceof NodeImpl ) {
            long b = ((NodeImpl)other).getSequenceNumber();
            if (a<b) return -1;
            if (a>b) return +1;
            return 0;
        } else {
            // it must be a namespace node
            return 0 - other.compareOrder(this);
        }
    }

    /**
     * Get the configuration
     */

    public Configuration getConfiguration() {
        return getDocumentRoot().getConfiguration();
    }

    /**
     * Get the NamePool
     */

    public NamePool getNamePool() {
        return document.getNamePool();
    }

    /**
     * Get the prefix part of the name of this node. This is the name before the ":" if any.
    * @return the prefix part of the name. For an unnamed node, return an empty string.
     */

    public String getPrefix() {
        if( nameCode<0 ) 
            return "";
        if( (nameCode>>20 & 0xff) == 0 ) 
            return "";
        return document.getNamePool().getPrefix(nameCode);
    }

    /**
    * Get the URI part of the name of this node. This is the URI corresponding to the
    * prefix, or the URI of the default namespace if appropriate.
    * @return The URI of the namespace of this node. For the default namespace, return an
    * empty string. For an unnamed node, return the empty string.
     */

    public String getURI() {
        if( nameCode < 0 ) 
            return "";
        return document.getNamePool().getURI(nameCode);
    }

    /**
    * Get the display name of this node. For elements and attributes this is [prefix:]localname.
    * For unnamed nodes, it is an empty string.
    * @return The display name of this node.
    * For a node with no name, return an empty string.
    */

    public String getDisplayName() {
        if( nameCode < 0 ) 
            return "";
        return document.getNamePool().getDisplayName(nameCode);
    }

    /**
     * Get the local name of this node.
     *
     * @return The local name of this node. For a node with no name, return "".
     */

    public String getLocalPart() {
        if( nameCode < 0 ) 
            return "";
        String localPart = document.getNamePool().getLocalName(nameCode);
        return localPart;
    }

    /**
    * Get the line number of the node within its source document entity
    */

    public int getLineNumber() {
        return -1;
    }

    /**
     * Find the parent node of this node.
     * @return The Node object describing the containing element or root node.
     */

    public NodeInfo getParent() {
        return document.getNode( parentNum );
    }

    /**
    * Get the previous sibling of the node
     * @return The previous sibling node. Returns null if the current node is the first
     * child of its parent.
     */

    public NodeInfo getPreviousSibling()  {
        return document.getNode( prevSibNum );
    }


    /**
     * Get next sibling node (DOM method)
     * 
     * @return The next sibling node. Returns null if the current node is the last
     * child of its parent.
     */
    public NodeInfo getNextSibling()  {
        return document.getNode( nextSibNum );
    }

    /**
    * Get first child - default implementation used for leaf nodes
    * @return null
     */

    public NodeInfo getFirstChild()  {
        return null; // overridden in ParentNodeImpl
    }

    /**
    * Get last child - default implementation used for leaf nodes
    * @return null
    */

    public NodeInfo getLastChild()  {
        return null;
    }

    /**
    * Return an enumeration over the nodes reached by the given axis from this node
    * @param axisNumber The axis to be iterated over
    * @return an AxisIterator that scans the nodes reached by the axis in turn.
    */

    public AxisIterator iterateAxis( byte axisNumber ) {
        // Fast path for child axis
        if (axisNumber==Axis.CHILD) {
             if (this instanceof ParentNodeImpl) {
                return ((ParentNodeImpl)this).enumerateChildren(null);
             } else {
                return EmptyIterator.getInstance();
             }
        } else {
            return iterateAxis(axisNumber, AnyNodeTest.getInstance());
        }
    }

    /**
    * Return an enumeration over the nodes reached by the given axis from this node
    * @param axisNumber The axis to be iterated over
    * @param nodeTest A pattern to be matched by the returned nodes
    * @return an AxisIterator that scans the nodes reached by the axis in turn.
    */

    @SuppressWarnings("cast")
    public AxisIterator iterateAxis( byte axisNumber, NodeTest nodeTest ) { 

        int type = getNodeKind();
        switch (axisNumber) {
            case Axis.ANCESTOR:
                 return new AncestorEnumeration( this, nodeTest, false );

            case Axis.ANCESTOR_OR_SELF:
                 return new AncestorEnumeration( this, nodeTest, true );

            case Axis.ATTRIBUTE:
                 if( this.getNodeKind() != Type.ELEMENT) return EmptyIterator.getInstance();
                 return new AttributeEnumeration( this, nodeTest );

            case Axis.CHILD:
                 if( this instanceof ParentNodeImpl ) {
                    return ((ParentNodeImpl)this).enumerateChildren(nodeTest);
                 } else {
                    return EmptyIterator.getInstance();
                 }

            case Axis.DESCENDANT:
                if( type == Type.DOCUMENT &&
                    nodeTest instanceof NameTest &&
                    nodeTest.getPrimitiveType() == Type.ELEMENT ) { 
                    return ((LazyDocument)this).getAllElements(
                                nodeTest.getFingerprint());
                } else if (hasChildNodes()) {
                    return new DescendantEnumeration( this, nodeTest, false );
                } else {
                    return EmptyIterator.getInstance();
                }

            case Axis.DESCENDANT_OR_SELF:
                return new DescendantEnumeration( this, nodeTest, true );

            case Axis.FOLLOWING:
                return new FollowingEnumeration( this, nodeTest );

            case Axis.FOLLOWING_SIBLING:
                 return new FollowingSiblingEnumeration( this, nodeTest );

            case Axis.NAMESPACE:
                 if( this.getNodeKind() != Type.ELEMENT ) return EmptyIterator.getInstance();
                 return new NamespaceIterator( (ElementImpl)this, nodeTest );

            case Axis.PARENT:
                 NodeInfo parent = getParent();
                 if( parent == null ) return EmptyIterator.getInstance();
                 if( nodeTest.matches(parent.getNodeKind(), 
                                      parent.getFingerprint(),
                                      parent.getTypeAnnotation()) ) { 
                    return SingletonIterator.makeIterator( parent );
                 }
                 return EmptyIterator.getInstance();

            case Axis.PRECEDING:
                return new PrecedingEnumeration( this, nodeTest );

            case Axis.PRECEDING_SIBLING:
                 return new PrecedingSiblingEnumeration( this, nodeTest );

            case Axis.SELF:
                 if( nodeTest.matches(getNodeKind(), 
                                      getFingerprint(), 
                                      getTypeAnnotation()) ) {
                    return SingletonIterator.makeIterator( this );
                 }
                 return EmptyIterator.getInstance();

            case Axis.PRECEDING_OR_ANCESTOR:
                 if (type==Type.DOCUMENT) {
                     return EmptyIterator.getInstance();
                 } else if (type==Type.ATTRIBUTE || type==Type.NAMESPACE) {
                     // See test numb32.
                     NodeImpl el = (NodeImpl)getParent();
                     return new PrependIterator(el, new PrecedingEnumeration(el, nodeTest));
                 } else {
                     return new PrecedingEnumeration(this, nodeTest);
                 }

            default:
                 throw new IllegalArgumentException("Unknown axis number " + axisNumber);
        }
    }

    /**
    * Determine whether the node has any children.
    * @return <code>true</code> if this node has any attributes,
    *   <code>false</code> otherwise.
    */
    public boolean hasChildNodes() {
        // overridden in ParentNodeImpl
        return false;
    }

    /**
    * Get the value of a given attribute of this node
    * @param fingerprint The fingerprint of the attribute name
    * @return the attribute value if it exists or null if not
    */

    public String getAttributeValue(int fingerprint) {
    	  return null;
    }

    /**
    * Get the root node
    * @return the NodeInfo representing the containing document
     */

    public NodeInfo getRoot() {
        return getDocumentRoot();
    }

    /**
     * Get the root (document) node
     * @return the DocumentInfo representing the containing document
     */

    public DocumentInfo getDocumentRoot() {
        return document.getDocumentRoot();
    }

    /**
     * Get the next node in document order
    * @param anchor the scan stops when it reaches a node that is not a descendant of the specified
    * anchor node
     * @return the next node in the document, or null if there is no such node
     */

    public NodeImpl getNextInDocument( NodeImpl anchor ) {
        // find the first child node if there is one; otherwise the next sibling node
        // if there is one; otherwise the next sibling of the parent, grandparent, etc, up to the anchor element.
        // If this yields no result, return null.

        NodeImpl next = (NodeImpl)getFirstChild();
        if( next != null ) return next;
        if( this == anchor ) return null;
        next = (NodeImpl)getNextSibling();
        if( next != null ) return next;
        NodeImpl parent = this;
        while( true ) {
            parent = (NodeImpl)parent.getParent();
            if( parent == null ) return null;
            if( parent == anchor ) return null;
            next = (NodeImpl)parent.getNextSibling();
            if( next != null ) return next;
        }
    }
    

    /**
     * Get the previous node in document order
     * @return the previous node in the document, or null if there is no such node
     */
    public NodeImpl getPreviousInDocument() {

        // finds the last child of the previous sibling if there is one;
        // otherwise the previous sibling element if there is one;
        // otherwise the parent, up to the anchor element.
        // If this reaches the document root, return null.

        NodeImpl prev = (NodeImpl)getPreviousSibling();
        if( prev != null ) return prev.getLastDescendantOrSelf();
        return (NodeImpl)getParent();
    }
    
    /**
     * Get the last descendant of this node, or if it has no descendants,
     * return the node itself.
     */
    private NodeImpl getLastDescendantOrSelf() {
        NodeImpl last = (NodeImpl)getLastChild();
        if( last == null ) return this;
        return last.getLastDescendantOrSelf();
    }

    /**
    * Output all namespace nodes associated with this element. Does nothing if
    * the node is not an element.
    * @param out The relevant outputter
    * @param includeAncestors True if namespaces declared on ancestor elements must
    */
    public void sendNamespaceDeclarations(Receiver out, boolean includeAncestors)
        throws XPathException
    {}

    /**
     * Get all namespace undeclarations and undeclarations defined on this element.
     *
     * @param buffer If this is non-null, and the result array fits in this buffer, then the result
     *               may overwrite the contents of this array, to avoid the cost of allocating a new array on the heap.
     * @return An array of integers representing the namespace declarations and undeclarations present on
     *         this element. For a node other than an element, return null. Otherwise, the returned array is a
     *         sequence of namespace codes, whose meaning may be interpreted by reference to the name pool. The
     *         top half word of each namespace code represents the prefix, the bottom half represents the URI.
     *         If the bottom half is zero, then this is a namespace undeclaration rather than a declaration.
     *         The XML namespace is never included in the list. If the supplied array is larger than required,
     *         then the first unused entry will be set to -1.
     *         <p/>
     *         <p>For a node other than an element, the method returns null.</p>
     */
    public int[] getDeclaredNamespaces(int[] buffer) {
        return null;
    }

    /**
    * Copy nodes. Copying type annotations is not yet supported for this tree
    * structure, so we simply map the new interface onto the old
    */

    public abstract void copy(Receiver out, int whichNamespaces, boolean copyAnnotations, int locationId) throws XPathException;
    
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
