package org.cdlib.xtf.lazyTree;

import net.sf.saxon.om.*;
import net.sf.saxon.pattern.NameTest;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.type.Type;

/**
* AttributeEnumeration is an enumeration of all the attribute nodes of an Element.
*/
final class AttributeEnumeration extends AxisIteratorImpl
  implements LookaheadIterator 
{
  private ElementImpl element;
  private NodeTest nodeTest;
  private NodeInfo next;
  private int index;
  private int length;

  /**
  * Constructor
  * @param node the element whose attributes are required. This may be any type of node,
  * but if it is not an element the enumeration will be empty
  * @param nodeTest condition to be applied to the names of the attributes selected
  */
  public AttributeEnumeration(NodeImpl node, NodeTest nodeTest) 
  {
    this.nodeTest = nodeTest;

    if (node.getNodeKind() == Type.ELEMENT) 
    {
      element = (ElementImpl)node;

      if (element.attrNames == null)
        return;

      if (nodeTest instanceof NameTest) 
      {
        NameTest test = (NameTest)nodeTest;
        int fingerprint = test.getFingerprint();
        for (int i = 0; i < element.attrNames.length; i++) 
        {
          if ((element.attrNames[i] & 0xfffff) == fingerprint) {
            next = new AttributeImpl(element, i);
            return;
          }
        }

        next = null;
      }
      else {
        length = element.attrNames.length;
        advance();
      }
    }
    else { // if it's not an element, or if we're not looking for attributes,

             // then there's nothing to find
      next = null;
      index = 0;
      length = 0;
    }
  }

  /**
  * Test if there are mode nodes still to come.
  * ("elements" is used here in the sense of the Java enumeration class, not in the XML sense)
  */
  public boolean hasNext() {
    return next != null;
  }

  /**
  * Get the next node in the iteration, or null if there are no more.
  */
  public Item next() 
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
  * Move to the next node in the enumeration.
  */
  private void advance() 
  {
    do 
    {
      if (index < length) {
        next = new AttributeImpl(element, index);
        index++;
      }
      else {
        next = null;
        return;
      }
    } while (!nodeTest.matches(next.getNodeKind(),
                               next.getFingerprint(),
                               next.getTypeAnnotation()));
  }

  /**
  * Get another enumeration of the same nodes
  */
  public SequenceIterator getAnother() {
    return new AttributeEnumeration(element, nodeTest);
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
