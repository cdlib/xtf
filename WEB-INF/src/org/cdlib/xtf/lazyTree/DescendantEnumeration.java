package org.cdlib.xtf.lazyTree;

import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.pattern.NodeTest;

/** Saxon: DescendantEnumeration provides an enumeration over all the descendants
 *  of a node.
 */
final class DescendantEnumeration extends TreeEnumeration 
{
  private NodeImpl root;
  private int endNodeNum;
  private boolean includeSelf;

  public DescendantEnumeration(NodeImpl node, NodeTest nodeTest,
                               boolean includeSelf) 
  {
    super(node, nodeTest);
    root = node;
    this.includeSelf = includeSelf;

    NodeImpl p = root;
    while (p != null) 
    {
      NodeImpl sib = (NodeImpl)p.getNextSibling();
      if (sib != null) {
        p = sib;
        break;
      }
      p = (NodeImpl)p.getParent();
    }
    endNodeNum = (p != null) ? p.nodeNum : node.document.numberOfNodes;

    if (!includeSelf || !conforms(node)) {
      advance();
    }
  }

  protected void step() {
    int nextNum = next.nodeNum + 1;
    next = (nextNum < endNodeNum) ? next.document.getNode(nextNum) : null;
  }

  /**
  * Get another enumeration of the same nodes
  */
  public SequenceIterator getAnother() {
    return new DescendantEnumeration(start, nodeTest, includeSelf);
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
