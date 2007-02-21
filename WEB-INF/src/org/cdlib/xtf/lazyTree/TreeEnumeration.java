package org.cdlib.xtf.lazyTree;

import net.sf.saxon.om.AxisIterator;
import net.sf.saxon.om.Item;
import net.sf.saxon.pattern.NodeTest;

/** Saxon: Base class for Node enumerators */
abstract class TreeEnumeration implements AxisIterator 
{
  protected NodeImpl start;
  protected NodeImpl next;
  protected NodeTest nodeTest;
  protected NodeImpl current = null;
  protected int position = 0;

  //protected int last = -1;

  /**
  * Create an axis enumeration for a given type and name of node, from a given
  * origin node
   * @param origin the node from which the axis originates
   * @param nodeTest test to be satisfied by the returned nodes, or null if all nodes
   * are to be returned.
  */
  public TreeEnumeration(NodeImpl origin, NodeTest nodeTest) {
    next = origin;
    start = origin;
    this.nodeTest = nodeTest;
  }

  /**
  * Test whether a node conforms to the node type and name constraints.
  * Note that this returns true if the supplied node is null, this is a way of
  * terminating a loop.
  */
  protected boolean conforms(NodeImpl node) 
  {
    if (node == null || nodeTest == null) {
      return true;
    }
    return nodeTest.matches(node.getNodeKind(),
                            node.getFingerprint(),
                            node.getTypeAnnotation());
  }

  /**
  * Advance along the axis until a node is found that matches the required criteria
  */
  protected final void advance() 
  {
    do {
      step();
    } while (!conforms(next));
  }

  /**
  * Advance one step along the axis: the resulting node might not meet the required
  * criteria for inclusion
  */
  protected abstract void step();

  /**
  * Return the next node in the enumeration
  */
  public final Item next() 
  {
    if (next == null) {
      return null;
    }
    else {
      current = next;
      position++;
      advance();
      return current;
    }
  }

  /**
  * Return the current Item
  */
  public final Item current() {
    return current;
  }

  /**
  * Return the current position
  */
  public final int position() {
    return position;
  }

  /**
   * Indicate that any nodes returned in the sequence will be atomized. This
   * means that if it wishes to do so, the implementation can return the typed
   * values of the nodes rather than the nodes themselves. The implementation
   * is free to ignore this hint.
   * @param atomizing true if the caller of this iterator will atomize any
   * nodes that are returned, and is therefore willing to accept the typed
   * value of the nodes instead of the nodes themselves.
   */

  //public void setIsAtomizing(boolean atomizing) {}
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
