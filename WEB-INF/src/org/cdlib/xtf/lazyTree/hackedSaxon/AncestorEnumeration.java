package org.cdlib.xtf.lazyTree.hackedSaxon;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.om.AxisIteratorImpl;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;

/**
* This class enumerates the ancestor:: or ancestor-or-self:: axes,
* starting at a given node. The start node will never be the root.
*/

final class AncestorEnumeration extends AxisIteratorImpl {

    private TinyNodeImpl startNode;
    private NodeTest test;
    private boolean includeSelf;

    public AncestorEnumeration(TinyNodeImpl node, NodeTest nodeTest, boolean includeSelf) {
        test = nodeTest;
        this.startNode = node;
        this.includeSelf = includeSelf;
        current = startNode;
    }

    public Item next() {
        if (position==0 && includeSelf &&
                test.matches(startNode.getNodeKind(), startNode.getFingerprint(), startNode.getTypeAnnotation())) {
            current = startNode;
            position = 1;
            return current;
        } else {
            NodeInfo node = ((NodeInfo)current).getParent();
            while (node != null && !test.matches(node.getNodeKind(), node.getFingerprint(), node.getTypeAnnotation())) {
                node = node.getParent();
            }
            current = node;
            position++;
            return current;
        }
    }

    /**
    * Get another enumeration of the same nodes
    */

    public SequenceIterator getAnother() {
        return new AncestorEnumeration(startNode, test, includeSelf);
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
