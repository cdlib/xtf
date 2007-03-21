package org.cdlib.xtf.lazyTree;


// IMPORTANT NOTE: When comparing, this file is most similar to 
//                 Saxon's net.sf.tree.AttributeImpl
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.FastStringBuffer;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;
import net.sf.saxon.event.Receiver;

/**
 * Represents an attribute node from a persistent XML document.
 *
 * @author Martin Haye
 */
final class AttributeImpl extends NodeImpl
{
  ElementImpl element;
  int index;

  /**
  * Construct an Attribute node for the n'th attribute of a given element
  * @param element The element containing the relevant attribute
  * @param index The index position of the attribute starting at zero
  */
  public AttributeImpl(ElementImpl element, int index) {
    this.index = index;
    this.element = element;
    nameCode = element.attrNames[index];
  }

  /**
   * Get the root node of the tree (not necessarily a document node)
   * @return the NodeInfo representing the root of this tree
   */
  public @Override NodeInfo getRoot() {
    return element.document.getRoot();
  }

  /**
   * Get the root (document) node
   * @return the DocumentInfo representing the containing document
   */
  public @Override DocumentInfo getDocumentRoot() {
    return element.document.getDocumentRoot();
  }

  /**
   * Get the NamePool for the tree containing this node
   * @return the NamePool
   */
  public @Override NamePool getNamePool() {
    return element.document.getNamePool();
  }

  /**
   * Determine whether this is the same node as another node
   * @return true if this Node object and the supplied Node object represent the
   * same node in the tree.
   */
  public @Override boolean isSameNodeInfo(NodeInfo other) {
    if (!(other instanceof AttributeImpl))
      return false;
    if (this == other)
      return true;
    AttributeImpl otherAtt = (AttributeImpl)other;
    return (element.isSameNodeInfo(otherAtt.element) &&
           index == otherAtt.index);
  }

  /**
   * The hashCode() method obeys the contract for hashCode(): that is, if two objects are equal
   * (represent the same node) then they must have the same hashCode()
   * @since 8.7 Previously, the effect of the equals() and hashCode() methods was not defined. Callers
   * should therefore be aware that third party implementations of the NodeInfo interface may
   * not implement the correct semantics.
   */
  public int hashCode() {
    return element.hashCode() ^ getFingerprint();
  }

  /**
   * Get the node sequence number (in document order). Sequence numbers are monotonic but not
   * consecutive. In the current implementation, parent nodes (elements and roots) have a zero
   * least-significant word, while namespaces, attributes, text nodes, comments, and PIs have
   * the top word the same as their owner and the bottom half reflecting their relative position.
   */
  protected @Override long getSequenceNumber() 
  {
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
  public @Override NodeInfo getNextSibling() {
    return null;
  }

  /**
   * Get previous sibling - not defined for attributes
   */
  public @Override NodeInfo getPreviousSibling() {
    return null;
  }

  /**
   * Get the previous node in document order (skipping attributes)
   */
  public @Override NodeImpl getPreviousInDocument() {
    return (NodeImpl)getParent();
  }

  /**
   * Get sequential key. Returns key of owning element with the attribute name as a suffix
   */
  public void generateId(FastStringBuffer buffer) {
    getParent().generateId(buffer);
    buffer.append('a');
    buffer.append(Integer.toString(index));
  }

  /**
   * Obtain the displayable name of this attribute.
   */
  public @Override String getDisplayName() {
    if (getNameCode() < 0)
      return "";
    return getNamePool().getDisplayName(getNameCode());
  }

  /**
   * Copy this node to a given outputter
   */
  public void copy(Receiver out, int whichNamespaces, boolean copyAnnotations,
                   int locationId)
    throws XPathException 
  {
    int nameCode = getNameCode();

    //if ((nameCode>>20 & 0xff) != 0) {	// non-null prefix
    // check there is no conflict of namespaces
    //	nameCode = out.checkAttributePrefix(nameCode);
    //}
    out.attribute(nameCode, -1, getStringValue(), 0, 0);
  }
} // class AttributeImpl

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
