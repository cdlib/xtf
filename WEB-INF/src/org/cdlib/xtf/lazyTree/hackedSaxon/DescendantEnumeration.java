package org.cdlib.xtf.lazyTree.hackedSaxon;
import net.sf.saxon.om.AxisIteratorImpl;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.type.Type;

/**
* This class supports both the descendant:: and descendant-or-self:: axes, which are
* identical except for the route to the first candidate node.
* It enumerates descendants of the specified node.
* The calling code must ensure that the start node is not an attribute or namespace node.
*/

final class DescendantEnumeration extends AxisIteratorImpl {

    private TinyDocumentImpl document;
    private TinyNodeImpl startNode;
    private boolean includeSelf;
    private int nextNodeNr;
    private int startDepth;
    private NodeTest test;

    protected DescendantEnumeration(TinyDocumentImpl doc, TinyNodeImpl node,
                                    NodeTest nodeTest, boolean includeSelf) {
        document = doc;
        startNode = node;
        this.includeSelf = includeSelf;
        test = nodeTest;
        nextNodeNr = node.nodeNr;
        startDepth = doc.depth[nextNodeNr];
        if (includeSelf) {          // descendant-or-self:: axis
            // no action
        } else {                    // descendant:: axis
            nextNodeNr++;
            if (doc.depth[nextNodeNr] <= startDepth) {
                nextNodeNr = -1;
            }
        }

        // check if this matches the conditions
        if (nextNodeNr >= 0 &&
                nextNodeNr < doc.numberOfNodes &&
                !nodeTest.matches(document.nodeKind[nextNodeNr],
                              document.nameCode[nextNodeNr],
                              (document.nodeKind[nextNodeNr] == Type.ELEMENT ?
                                    document.getElementAnnotation(nextNodeNr) :
                                    -1))) {
            advance();
            // SaxonTODO: no longer need to look ahead.
        }
    }

    public Item next() {
        if (nextNodeNr >= 0) {
            position++;
            if (isAtomizing() && document.getTypeAnnotation()==-1) {
                current = document.getUntypedAtomicValue(nextNodeNr);
            } else {
                current = document.getNode(nextNodeNr);
            }
            advance();
            return current;
        } else {
            return null;
        }
    }

    private void advance() {
        do {
            nextNodeNr++;
            if (nextNodeNr >= document.numberOfNodes ||
                document.depth[nextNodeNr] <= startDepth) {
                nextNodeNr = -1;
                return;
            }
        } while (!test.matches(document.nodeKind[nextNodeNr],
                                document.nameCode[nextNodeNr],
                               (document.nodeKind[nextNodeNr] == Type.ELEMENT ?
                                    document.getElementAnnotation(nextNodeNr) :
                                    -1)));
    }

    /**
    * Get another enumeration of the same nodes
    */

    public SequenceIterator getAnother() {
        return new DescendantEnumeration(document, startNode, test, includeSelf);
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
