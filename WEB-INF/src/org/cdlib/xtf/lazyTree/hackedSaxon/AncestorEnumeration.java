package org.cdlib.xtf.lazyTree.hackedSaxon;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.om.AxisIteratorImpl;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;

/**
* This class enumerates the ancestor:: or ancestor-or-self:: axes,
* starting at a given node. The start node will never be the root.
*/

final class AncestorEnumeration extends AxisIteratorImpl {

    //private int nextNodeNr;
    private TinyNodeImpl next;
    private TinyDocumentImpl document;
    private TinyNodeImpl startNode;
    private NodeTest test;
    private TinyNodeImpl first = null;
    private boolean includeSelf;

    public AncestorEnumeration(TinyDocumentImpl doc, TinyNodeImpl node,
                                NodeTest nodeTest, boolean includeSelf) {
        document = doc;
        test = nodeTest;
        this.startNode = node;
        this.includeSelf = includeSelf;
        if (includeSelf && nodeTest.matches(node.getNodeKind(), node.getFingerprint(), node.getTypeAnnotation())) {
            first = node;
        }

        // this code is designed to catch the case where the first node
        // is an attribute or namespace node

        next = (TinyNodeImpl)node.getParent();
        if (next != null &&
                !nodeTest.matches(next.getNodeKind(), next.getFingerprint(), next.getTypeAnnotation())) {
            advance();
            // SaxonTODO: lookahead no longer needed
        }
    }

    public Item next() {
        if (first==null && next==null) {
            return null;
        }
        position++;
        if (first!=null) {
            current = first;
            first = null;
            return current;
        } else {
            current = next;
            advance();
            return current;
        }
    }

    private void advance() {
        do {
            next = (TinyNodeImpl)next.getParent();
        } while (next != null && !test.matches(next.getNodeKind(), next.getFingerprint(), next.getTypeAnnotation()));
    }

    /**
    * Get another enumeration of the same nodes
    */

    public SequenceIterator getAnother() {
        return new AncestorEnumeration(document, startNode, test, includeSelf);
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
