package org.cdlib.xtf.lazyTree.hackedSaxon;

import net.sf.saxon.om.AxisIteratorImpl;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.pattern.NodeTest;

/**
* This class supports the preceding-sibling axis.
* The starting node must be an element, text node, comment, or processing instruction:
* to ensure this, construct the enumeration using NodeInfo#getEnumeration()
*/
final class PrecedingSiblingEnumeration extends AxisIteratorImpl 
{
  private TinyTree document;
  private TinyNodeImpl startNode;
  private int nextNodeNr;
  private NodeTest test;
  private TinyNodeImpl parentNode;

  PrecedingSiblingEnumeration(TinyTree doc, TinyNodeImpl node, NodeTest nodeTest) {
    document = doc;
    document.ensurePriorIndex();
    test = nodeTest;
    startNode = node;
    nextNodeNr = node.nodeNr;
    parentNode = node.parent; // doesn't matter if this is null (unknown)
  }

  public Item next() 
  {
    //        if (nextNodeNr < 0) {
    //            return null;
    //        }
    while (true) 
    {
      nextNodeNr = document.prior[nextNodeNr];
      if (nextNodeNr < 0) {
        return null;
      }
      if (test.matches(document.nodeKind[nextNodeNr],
                       document.nameCode[nextNodeNr],
                       document.getElementAnnotation(nextNodeNr))) 
      {
        position++;
        current = document.getNode(nextNodeNr);
        ((TinyNodeImpl)current).setParentNode(parentNode);
        return current;
      }
      ;
    }
  }

  /**
  * Get another enumeration of the same nodes
  */
  public SequenceIterator getAnother() {
    return new PrecedingSiblingEnumeration(document, startNode, test);
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
