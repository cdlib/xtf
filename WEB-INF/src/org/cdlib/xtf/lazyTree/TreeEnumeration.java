package org.cdlib.xtf.lazyTree;

import net.sf.saxon.om.AxisIterator;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.LookaheadIterator;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.value.Value;
import net.sf.saxon.trans.XPathException;

abstract class TreeEnumeration implements AxisIterator, LookaheadIterator 
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
    return nodeTest.matches(node);
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
   * Determine whether there are more items to come. Note that this operation
   * is stateless and it is not necessary (or usual) to call it before calling
   * next(). It is used only when there is an explicit need to tell if we
   * are at the last element.
   *
   * @return true if there are more items in the sequence
   */
  public boolean hasNext() {
    return next != null;
  }

  /**
   * Move to the next node, without returning it. Returns true if there is
   * a next node, false if the end of the sequence has been reached. After
   * calling this method, the current node may be retrieved using the
   * current() function.
   */
  public boolean moveNext() {
    return (next() != null);
  }

  /**
  * Return the next node in the enumeration
  */
  public final Item next() 
  {
    if (next == null) {
      current = null;
      position = -1;
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
   * Return an iterator over an axis, starting at the current node.
   *
   * @param axis the axis to iterate over, using a constant such as
   *             {@link net.sf.saxon.om.Axis#CHILD}
   * @param test a predicate to apply to the nodes before returning them.
   * @throws NullPointerException if there is no current node
   */
  public AxisIterator iterateAxis(byte axis, NodeTest test) {
    return current.iterateAxis(axis, test);
  }

  /**
   * Return the atomized value of the current node.
   *
   * @return the atomized value.
   * @throws NullPointerException if there is no current node
   */
  public Value atomize()
    throws XPathException 
  {
    return current.atomize();
  }

  /**
   * Return the string value of the current node.
   *
   * @return the string value, as an instance of CharSequence.
   * @throws NullPointerException if there is no current node
   */
  public CharSequence getStringValue() {
    return current.getStringValueCS();
  }

  /**
   * Get properties of this iterator, as a bit-significant integer.
   *
   * @return the properties of this iterator. This will be some combination of
   *         properties such as GROUNDED, LAST_POSITION_FINDER,
   *         and LOOKAHEAD. It is always
   *         acceptable to return the value zero, indicating that there are no known special properties.
   *         It is acceptable for the properties of the iterator to change depending on its state.
   */
  public int getProperties() {
    return LOOKAHEAD;
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
