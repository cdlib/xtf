package org.cdlib.xtf.lazyTree.hackedSaxon;
import net.sf.saxon.om.AxisIteratorImpl;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.pattern.NodeTest;

/**
* Iterate over the following axis starting at a given node.
* The start node must not be a namespace or attribute node.
*/

final class FollowingEnumeration extends AxisIteratorImpl {

    private TinyTree tree;
    private TinyNodeImpl startNode;
    private NodeTest test;
    private boolean includeDescendants;

    /**
     * Create an iterator over the following axis
     * @param doc the containing TinyTree
     * @param node the start node. If the actual start was an attribute or namespace node, this will
     * be the parent element of that attribute or namespace
     * @param nodeTest condition that all the returned nodes must satisfy
     * @param includeDescendants true if descendants of the start node are to be included. This will
     * be false if the actual start was an element node, true if it was an attribute or namespace node
     * (since the children of their parent follow the attribute or namespace in document order).
     */

    public FollowingEnumeration(TinyTree doc, TinyNodeImpl node,
                                 NodeTest nodeTest, boolean includeDescendants) {
        tree = doc;
        test = nodeTest;
        startNode = node;
        this.includeDescendants = includeDescendants;
    }

    public Item next() {
        int nodeNr;
        if (position == 0) {
            // first time call
            nodeNr = startNode.nodeNr;
            int depth = tree.depth[nodeNr];

            // skip the descendant nodes if any
            if (includeDescendants) {
                nodeNr++;
            } else {
                do {
                    nodeNr++;
                    if (tree.depth[nodeNr] == 0) {
                        current = null;
                        return null;
                    }
                } while (tree.depth[nodeNr] > depth);
            }
        } else {
            nodeNr = ((TinyNodeImpl)current).nodeNr + 1;
        }

        while (true) {
            if (tree.depth[nodeNr] == 0) {
                current = null;
                return null;
            }
            if (test.matches(tree.nodeKind[nodeNr], tree.nameCode[nodeNr],
                    tree.getElementAnnotation(nodeNr))) {
                position++;
                current = tree.getNode(nodeNr);
                return current;
            }
            nodeNr++;
        }
    }

    /**
    * Get another enumeration of the same nodes
    */

    public SequenceIterator getAnother() {
        return new FollowingEnumeration(tree, startNode, test, includeDescendants);
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
