package org.cdlib.xtf.lazyTree.hackedSaxon;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.AxisIteratorImpl;
import net.sf.saxon.om.SequenceIterator;

/**
* Enumerate all the nodes on the preceding axis from a given start node.
* The calling code ensures that the start node is not a root, attribute,
* or namespace node. As well as the standard XPath preceding axis, this
* class also implements a Saxon-specific "preceding-or-ancestor" axis
* which returns ancestor nodes as well as preceding nodes. This is used
* when performing xsl:number level="any".
*/

final class PrecedingEnumeration extends AxisIteratorImpl {

    private TinyDocumentImpl document;
    private TinyNodeImpl startNode;
    private NodeTest test;
    private int nextNodeNr;
    private int nextAncestorDepth;
    private boolean includeAncestors;

    public PrecedingEnumeration(TinyDocumentImpl doc, TinyNodeImpl node,
                                NodeTest nodeTest, boolean includeAncestors) {

        this.includeAncestors = includeAncestors;
        test = nodeTest;
        document = doc;
        startNode = node;
        nextNodeNr = node.nodeNr;
        nextAncestorDepth = doc.depth[nextNodeNr] - 1;
        advance();
        // SaxonTODO: no longer need to look ahead
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

    private void advance() {
        do {
            nextNodeNr--;
            if (!includeAncestors) {
                // skip over ancestor elements
                while (nextNodeNr >= 0 && document.depth[nextNodeNr] == nextAncestorDepth) {
                    nextAncestorDepth--;
                    nextNodeNr--;
                }
            }
        } while ( nextNodeNr >= 0 &&
                !test.matches(document.nodeKind[nextNodeNr],
                              document.nameCode[nextNodeNr],
                              document.getElementAnnotation(nextNodeNr)));
    }

    /**
    * Get another enumeration of the same nodes
    */

    public SequenceIterator getAnother() {
        return new PrecedingEnumeration(document, startNode, test, includeAncestors);
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
