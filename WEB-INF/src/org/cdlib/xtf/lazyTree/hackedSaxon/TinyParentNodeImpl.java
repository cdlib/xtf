package org.cdlib.xtf.lazyTree.hackedSaxon;
import net.sf.saxon.type.Type;

/**
  * TinyParentNodeImpl is an implementation of a non-leaf node (specifically, an Element node
  * or a Document node)
  * @author Michael H. Kay
  */


abstract class TinyParentNodeImpl extends TinyNodeImpl {

    /**
    * Determine if the node has children.
    */

    public final boolean hasChildNodes() {
        return (nodeNr+1 < tree.numberOfNodes &&
                tree.depth[nodeNr+1] > tree.depth[nodeNr]);
    }

    /**
    * Return the string-value of the node, that is, the concatenation
    * of the character content of all descendent elements and text nodes.
    * @return the accumulated character content of the element, including descendant elements.
    */

    public final String getStringValue() {
        int level = tree.depth[nodeNr];
        StringBuffer sb = null;

        // note, we can't rely on the value being contiguously stored because of whitespace
        // nodes: the data for these may still be present.

        int next = nodeNr+1;
        while (next < tree.numberOfNodes && tree.depth[next] > level) {
            if (tree.nodeKind[next]==Type.TEXT) {
                if (sb==null) {
                    sb = new StringBuffer(200);
                }
                int length = tree.beta[next];
                int start = tree.alpha[next];
                sb.append(tree.charBuffer, start, length);
            }
            next++;
        }
        if (sb==null) return "";
        return sb.toString();
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
