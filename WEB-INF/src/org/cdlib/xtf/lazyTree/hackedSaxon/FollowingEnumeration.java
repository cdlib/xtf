package org.cdlib.xtf.lazyTree.hackedSaxon;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.AxisIteratorImpl;
import net.sf.saxon.om.SequenceIterator;

/**
* Enumerate the following axis starting at a given node.
* The start node must not be a namespace or attribute node.
*/

final class FollowingEnumeration extends AxisIteratorImpl {

    private TinyDocumentImpl document;
    private TinyNodeImpl startNode;
    private int nextNodeNr;
    private NodeTest test;
    private boolean includeDescendants;

    public FollowingEnumeration(TinyDocumentImpl doc, TinyNodeImpl node,
                                 NodeTest nodeTest, boolean includeDescendants) {
        document = doc;
        test = nodeTest;
        startNode = node;
        nextNodeNr = node.nodeNr;
        this.includeDescendants = includeDescendants;
        int depth = doc.depth[nextNodeNr];

        // skip the descendant nodes if any
        if (includeDescendants) {
            nextNodeNr++;
        } else {
            do {
                nextNodeNr++;
                if (nextNodeNr >= doc.numberOfNodes) {
                    nextNodeNr = -1;
                    return;
                }
            } while (doc.depth[nextNodeNr] > depth);
        }

        if (!test.matches(doc.nodeKind[nextNodeNr], doc.nameCode[nextNodeNr],
                            doc.getElementAnnotation(nextNodeNr))) {
            advance();
            // SaxonTODO: no longer need to look ahead
        }
    }

    private void advance() {
        do {
            nextNodeNr++;
            if (nextNodeNr >= document.numberOfNodes) {
                nextNodeNr = -1;
                return;
            }
        } while (!test.matches(document.nodeKind[nextNodeNr], document.nameCode[nextNodeNr],
                    document.getElementAnnotation(nextNodeNr)));
    }

    public Item next() {
        if (nextNodeNr >= 0) {
            position++;
            current = document.getNode(nextNodeNr);
            advance();
            return current;
        } else {
            return null;
        }
    }

    /**
    * Get another enumeration of the same nodes
    */

    public SequenceIterator getAnother() {
        return new FollowingEnumeration(document, startNode, test, includeDescendants);
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
