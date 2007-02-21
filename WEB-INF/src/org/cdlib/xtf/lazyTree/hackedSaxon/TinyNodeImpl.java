package org.cdlib.xtf.lazyTree.hackedSaxon;

import net.sf.saxon.Configuration;
import net.sf.saxon.Err;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.AnyNodeTest;
import net.sf.saxon.pattern.NameTest;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.UntypedAtomicValue;
import javax.xml.transform.SourceLocator;

/**
  * A node in a TinyTree representing an XML element, character content, or attribute.<P>
  * This is the top-level class in the implementation class hierarchy; it essentially contains
  * all those methods that can be defined using other primitive methods, without direct access
  * to data.
  * @author Michael H. Kay
  */
public abstract class TinyNodeImpl implements NodeInfo, FingerprintedNode,
                                              SourceLocator 
{
  // CDL-HACK: All private and protected members changed to public, so that
  //           the contents of this structure can be re-used.
  //
  public TinyTree tree;
  public int nodeNr;
  public TinyNodeImpl parent = null;

  /**
  * Chararacteristic letters to identify each type of node, indexed using the node type
  * values. These are used as the initial letter of the result of generate-id()
  */
  public static final char[] NODE_LETTER = {
                                             'x', 'e', 'a', 't', 'x', 'x', 'x',
                                             'p', 'c', 'r', 'x', 'x', 'x', 'n'
                                           };

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
  public SequenceIterator getTypedValue()
    throws XPathException 
  {
    int annotation = getTypeAnnotation();
    if (annotation == -1) 
    {
      return SingletonIterator.makeIterator(
        new UntypedAtomicValue(getStringValue()));
    }
    else {
      SchemaType stype = getConfiguration().getSchemaType(annotation);
      if (stype == null) {
        String typeName = getNamePool().getDisplayName(annotation);
        throw new DynamicError(
          "Unknown type annotation " + Err.wrap(typeName) +
          " in document instance");
      }
      else {
        return stype.getTypedValue(this);
      }
    }
  }

  /**
  * Set the system id of this node. <br />
  * This method is present to ensure that
  * the class implements the javax.xml.transform.Source interface, so a node can
  * be used as the source of a transformation.
  */
  public void setSystemId(String uri) 
  {
    short type = tree.nodeKind[nodeNr];
    if (type == Type.ATTRIBUTE || type == Type.NAMESPACE) {
      getParent().setSystemId(uri);
    }
    else {
      tree.setSystemId(nodeNr, uri);
    }
  }

  /**
  * Set the parent of this node. Providing this information is useful,
  * if it is known, because otherwise getParent() has to search backwards
  * through the document.
  */
  protected void setParentNode(TinyNodeImpl parent) {
    this.parent = parent;
  }

  /**
  * Determine whether this is the same node as another node
  * @return true if this Node object and the supplied Node object represent the
  * same node in the tree.
  */
  public boolean isSameNodeInfo(NodeInfo other) {
    if (this == other)
      return true;
    if (!(other instanceof TinyNodeImpl))
      return false;
    if (this.tree != ((TinyNodeImpl)other).tree)
      return false;
    if (this.nodeNr != ((TinyNodeImpl)other).nodeNr)
      return false;
    if (this.getNodeKind() != other.getNodeKind())
      return false;
    return true;
  }

  /**
  * Get the system ID for the entity containing the node.
  */
  public String getSystemId() {
    return tree.getSystemId(nodeNr);
  }

  /**
  * Get the base URI for the node. Default implementation for child nodes gets
  * the base URI of the parent node.
  */
  public String getBaseURI() {
    return (getParent()).getBaseURI();
  }

  /**
  * Get the line number of the node within its source document entity
  */
  public int getLineNumber() {
    return tree.getLineNumber(nodeNr);
  }

  /**
  * Get the node sequence number (in document order). Sequence numbers are monotonic but not
  * consecutive. The sequence number must be unique within the document (not, as in
  * previous releases, within the whole document collection).
  * For document nodes, elements, text nodes, comment nodes, and PIs, the sequence number
   * is a long with the sequential node number in the top half and zero in the bottom half.
   * The bottom half is used only for attributes and namespace.
  */
  protected long getSequenceNumber() {
    return (long)nodeNr << 32;
  }

  /**
  * Determine the relative position of this node and another node, in document order.
  * The other node will always be in the same document.
  * @param other The other node, whose position is to be compared with this node
  * @return -1 if this node precedes the other node, +1 if it follows the other
  * node, or 0 if they are the same node. (In this case, isSameNode() will always
  * return true, and the two nodes will produce the same result for generateId())
  */
  public final int compareOrder(NodeInfo other) 
  {
    long a = getSequenceNumber();
    if (other instanceof TinyNodeImpl) 
    {
      long b = ((TinyNodeImpl)other).getSequenceNumber();
      if (a < b)
        return -1;
      if (a > b)
        return +1;
      return 0;
    }
    else {
      // it must be a namespace node
      return 0 - other.compareOrder(this);
    }
  }

  /**
  * Get the fingerprint of the node, used for matching names
  */
  public int getFingerprint() {
    int nc = getNameCode();
    if (nc == -1)
      return -1;
    return nc & 0xfffff;
  }

  /**
  * Get the name code of the node, used for matching names
  */
  public int getNameCode() 
  {
    // overridden for attributes and namespace nodes.
    return tree.nameCode[nodeNr];
  }

  /**
  * Get the prefix part of the name of this node. This is the name before the ":" if any.
  * @return the prefix part of the name. For an unnamed node, return "".
  */
  public String getPrefix() {
    int code = tree.nameCode[nodeNr];
    if (code < 0)
      return "";
    if ((code >> 20 & 0xff) == 0)
      return "";
    return tree.getNamePool().getPrefix(code);
  }

  /**
  * Get the URI part of the name of this node. This is the URI corresponding to the
  * prefix, or the URI of the default namespace if appropriate.
  * @return The URI of the namespace of this node. For an unnamed node, or for
  * an element or attribute in the default namespace, return an empty string.
  */
  public String getURI() {
    int code = tree.nameCode[nodeNr];
    if (code < 0)
      return "";
    return tree.getNamePool().getURI(code);
  }

  /**
  * Get the display name of this node (a lexical QName). For elements and attributes this is [prefix:]localname.
  * The original prefix is retained. For unnamed nodes, the result is an empty string.
  * @return The display name of this node.
  * For a node with no name, return an empty string.
  */
  public String getDisplayName() {
    int code = tree.nameCode[nodeNr];
    if (code < 0)
      return "";
    return tree.getNamePool().getDisplayName(code);
  }

  /**
  * Get the local part of the name of this node.
  * @return The local name of this node.
  * For a node with no name, return "".
  */
  public String getLocalPart() {
    int code = tree.nameCode[nodeNr];
    if (code < 0)
      return "";
    return tree.getNamePool().getLocalName(code);
  }

  /**
  * Return an iterator over all the nodes reached by the given axis from this node
  * @param axisNumber Identifies the required axis, eg. Axis.CHILD or Axis.PARENT
  * @return a AxisIteratorImpl that scans the nodes reached by the axis in turn.
  */
  public AxisIterator iterateAxis(byte axisNumber) 
  {
    // fast path for child axis
    if (axisNumber == Axis.CHILD) 
    {
      if (hasChildNodes()) {
        return new SiblingEnumeration(tree, this, null, true);
      }
      else {
        return EmptyIterator.getInstance();
      }
    }
    else {
      return iterateAxis(axisNumber, AnyNodeTest.getInstance());
    }
  }

  /**
  * Return an iterator over the nodes reached by the given axis from this node
  * @param axisNumber Identifies the required axis, eg. Axis.CHILD or Axis.PARENT
  * @param nodeTest A pattern to be matched by the returned nodes.
  * @return a AxisIteratorImpl that scans the nodes reached by the axis in turn.
  */
  public AxisIterator iterateAxis(byte axisNumber, NodeTest nodeTest) 
  {
    int type = getNodeKind();
    switch (axisNumber) 
    {
      case Axis.ANCESTOR:
        return new AncestorEnumeration(this, nodeTest, false);
      case Axis.ANCESTOR_OR_SELF:
        return new AncestorEnumeration(this, nodeTest, true);
      case Axis.ATTRIBUTE:
        if (type != Type.ELEMENT) 
      {
          return EmptyIterator.getInstance();
        }
        if (tree.alpha[nodeNr] < 0) {
          return EmptyIterator.getInstance();
        }
        return new AttributeEnumeration(tree, nodeNr, nodeTest);
      case Axis.CHILD:
        if (hasChildNodes()) 
      {
          return new SiblingEnumeration(tree, this, nodeTest, true);
        }
        else {
          return EmptyIterator.getInstance();
        }
      case Axis.DESCENDANT:
        if (type == Type.DOCUMENT &&
            nodeTest instanceof NameTest &&
            nodeTest.getPrimitiveType() == Type.ELEMENT) 
        {
          return ((TinyDocumentImpl)this).getAllElements(nodeTest.getFingerprint());
        }
        else if (hasChildNodes()) {
          return new DescendantEnumeration(tree, this, nodeTest, false);
        }
        else {
          return EmptyIterator.getInstance();
        }
      case Axis.DESCENDANT_OR_SELF:
        if (hasChildNodes()) 
        {
          return new DescendantEnumeration(tree, this, nodeTest, true);
        }
        else {
          if (nodeTest.matches(getNodeKind(),
                               getFingerprint(),
                               getTypeAnnotation())) 
          {
            return SingletonIterator.makeIterator(this);
          }
          else {
            return EmptyIterator.getInstance();
          }
        }
      case Axis.FOLLOWING:
        if (type == Type.ATTRIBUTE || type == Type.NAMESPACE) 
        {
          return new FollowingEnumeration(tree,
                                          (TinyNodeImpl)getParent(),
                                          nodeTest,
                                          true);
        }
        else if (tree.depth[nodeNr] == 0) {
          return EmptyIterator.getInstance();
        }
        else {
          return new FollowingEnumeration(tree, this, nodeTest, false);
        }
      case Axis.FOLLOWING_SIBLING:
        if (type == Type.ATTRIBUTE ||
            type == Type.NAMESPACE ||
            tree.depth[nodeNr] == 0) 
        {
          return EmptyIterator.getInstance();
        }
        else {
          return new SiblingEnumeration(tree, this, nodeTest, false);
        }
      case Axis.NAMESPACE:
        if (type != Type.ELEMENT) 
        {
          return EmptyIterator.getInstance();
        }
        return new NamespaceIterator(this, nodeTest);
      case Axis.PARENT:
        NodeInfo parent = getParent();
        if (parent == null)
          return EmptyIterator.getInstance();
        if (nodeTest.matches(parent.getNodeKind(),
                             parent.getFingerprint(),
                             getTypeAnnotation())) 
        {
          return SingletonIterator.makeIterator(parent);
        }
        return EmptyIterator.getInstance();
      case Axis.PRECEDING:
        if (type == Type.ATTRIBUTE || type == Type.NAMESPACE) 
        {
          return new PrecedingEnumeration(tree,
                                          (TinyNodeImpl)getParent(),
                                          nodeTest,
                                          false);
        }
        else if (tree.depth[nodeNr] == 0) {
          return EmptyIterator.getInstance();
        }
        else {
          return new PrecedingEnumeration(tree, this, nodeTest, false);
        }
      case Axis.PRECEDING_SIBLING:
        if (type == Type.ATTRIBUTE ||
            type == Type.NAMESPACE ||
            tree.depth[nodeNr] == 0) 
        {
          return EmptyIterator.getInstance();
        }
        else {
          return new PrecedingSiblingEnumeration(tree, this, nodeTest);
        }
      case Axis.SELF:
        if (nodeTest.matches(getNodeKind(), getFingerprint(),
                             getTypeAnnotation())) 
        {
          return SingletonIterator.makeIterator(this);
        }
        return EmptyIterator.getInstance();
      case Axis.PRECEDING_OR_ANCESTOR:
        if (type == Type.DOCUMENT) 
        {
          return EmptyIterator.getInstance();
        }
        else if (type == Type.ATTRIBUTE || type == Type.NAMESPACE) {
          // See test numb32.
          TinyNodeImpl el = (TinyNodeImpl)getParent();
          return new PrependIterator(el,
                                     new PrecedingEnumeration(
                                                              tree,
                                                              el,
                                                              nodeTest,
                                                              true));
        }
        else {
          return new PrecedingEnumeration(tree, this, nodeTest, true);
        }
      default:
        throw new IllegalArgumentException("Unknown axis number " + axisNumber);
    }
  }

  /**
   * Find the parent node of this node.
   * @return The Node object describing the containing element or root node.
   */
  public NodeInfo getParent() 
  {
    if (tree.depth[nodeNr] == 0) {
      parent = null;
      return null;
    }

    if (parent != null) {
      return parent;
    }

    /*
    * Following code to create/use owner pointers works, but doesn't
    * appear to give a performance improvement

        // if parent is unknown, use the parent index
        if (document.parentIndex == null) {
            document.makeParentIndex();
        }
        parent = document.getNode(document.parentIndex[nodeNr]);
        return parent;
    */

    // if parent is unknown, follow the next-sibling pointers until we reach a backwards pointer
    int p = tree.next[nodeNr];
    while (p > nodeNr) {
      p = tree.next[p];
    }
    parent = tree.getNode(p);
    return parent;
  }

  /**
  * Determine whether the node has any children.
  * @return <code>true</code> if this node has any attributes,
  *   <code>false</code> otherwise.
  */
  public boolean hasChildNodes() 
  {
    // overridden in TinyParentNodeImpl
    return false;
  }

  /**
  * Get the value of a given attribute of this node
  * @param fingerprint The fingerprint of the attribute name
  * @return the attribute value if it exists or null if not
  */
  public String getAttributeValue(int fingerprint) 
  {
    // overridden in TinyElementImpl
    return null;
  }

  /**
  * Get the root node of the tree (not necessarily a document node)
  * @return the NodeInfo representing the root of this tree
  */
  public NodeInfo getRoot() {
    return tree.getNode(0);
  }

  /**
  * Get the root (document) node
  * @return the DocumentInfo representing the containing document
  */
  public DocumentInfo getDocumentRoot() 
  {
    NodeInfo root = getRoot();
    if (root instanceof DocumentInfo) {
      return (DocumentInfo)root;
    }
    else {
      return null;
    }
  }

  /**
   * Get the configuration
   */
  public Configuration getConfiguration() {
    return tree.getConfiguration();
  }

  /**
   * Get the NamePool for the tree containing this node
   * @return the NamePool
   */
  public NamePool getNamePool() {
    return tree.getNamePool();
  }

  /**
  * Output all namespace nodes associated with this element. Does nothing if
  * the node is not an element.
  * @param out The relevant outputter
   * @param includeAncestors True if namespaces declared on ancestor elements must
   */
  public void sendNamespaceDeclarations(Receiver out, boolean includeAncestors)
    throws XPathException 
  {
  }

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
  * Get a character string that uniquely identifies this node
  * @return a string.
  */
  public String generateId() {
    return "d" + tree.getDocumentNumber() + NODE_LETTER[getNodeKind()] +
           nodeNr;
  }

  /**
   * Get the document number of the document containing this node
   * (Needed when the document isn't a real node, for sorting free-standing elements)
   */
  public int getDocumentNumber() {
    return tree.getDocumentNumber();
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by Martin Haye are Copyright (C) Regents of the University 
// of California. All Rights Reserved. 
//
// Contributor(s): Martin Haye. 
//
