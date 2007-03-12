package org.cdlib.xtf.lazyTree;

import net.sf.saxon.om.Axis;
import net.sf.saxon.om.AxisIterator;
import net.sf.saxon.om.EmptyIterator;
import net.sf.saxon.om.FastStringBuffer;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.pattern.AnyNodeTest;
import net.sf.saxon.pattern.NodeTest;

/**
 * Represents any node that can have children.
 * 
 * Important note: when comparing for a Saxon upgrade, this code is kind of 
 * a mix between net.sf.saxon.tinytree.TinyParentNodeImpl and
 * net.sf.saxon.tree.ParentNodeImpl.
 */
public abstract class ParentNodeImpl extends NodeImpl 
{
  int childNum;

  public ParentNodeImpl(LazyDocument document, NodeInfo parent) {
    super(document, parent);
  }

  // inherit JavaDoc
  public final boolean hasChildNodes() {
    return (childNum >= 0);
  }

  // inherit JavaDoc
  public final AxisIterator enumerateChildren(NodeTest test) 
  {
    if (childNum < 0)
      return EmptyIterator.getInstance();
    else
      return new ChildEnumeration(this, test);
  }

  // Normally provided by NodeInfo, but it does an explicit instanceof check
  // for ParentNodeImpl. Since we're a LazyParentNodeImpl, we don't match
  // that check. So we do it ourselves here.
  //
  public AxisIterator iterateAxis(byte axisNumber) 
  {
    // Fast path for child axis
    if (axisNumber == Axis.CHILD) 
      return enumerateChildren(null);
    else
      return super.iterateAxis(axisNumber, AnyNodeTest.getInstance());
  }

  // Normally provided by NodeInfo, but it does an explicit instanceof check
  // for ParentNodeImpl. Since we're a LazyParentNodeImpl, we don't match
  // that check. So we do it ourselves here.
  //
  public AxisIterator iterateAxis(byte axisNumber, NodeTest nodeTest) 
  {
    // Fast path for child axis
    if (axisNumber == Axis.CHILD) 
      return enumerateChildren(nodeTest);
    else
      return super.iterateAxis(axisNumber, nodeTest);
  }

  /**
   * Get first child (DOM method)
   *
   * @return the first child node of this node, or null if it has no children
   */
  public NodeInfo getFirstChild() {
    return document.getNode(childNum);
  }

  /** The last child of this Node, or null if none. */
  public NodeInfo getLastChild() 
  {
    NodeInfo last = getFirstChild();
    if (last != null) 
    {
      while (true) {
        NodeInfo next = ((NodeImpl)last).getNextSibling();
        if (next == null)
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
  public String getStringValue() {
    return getStringValueCS().toString();
  }

  public CharSequence getStringValueCS() 
  {
    FastStringBuffer sb = null;

    NodeImpl next = (NodeImpl)getFirstChild();
    while (next != null) 
    {
      if (next instanceof TextImpl) 
      {
        if (sb == null) {
          sb = new FastStringBuffer(1024);
        }
        sb.append(next.getStringValueCS());
      }
      next = next.getNextInDocument(this);
    }
    if (sb == null)
      return "";
    return sb.condense();
  }

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
