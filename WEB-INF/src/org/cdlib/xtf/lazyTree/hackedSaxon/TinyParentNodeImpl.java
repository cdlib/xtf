package org.cdlib.xtf.lazyTree.hackedSaxon;
import net.sf.saxon.type.Type;
import net.sf.saxon.om.FastStringBuffer;

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
        return getStringValue(tree, nodeNr).toString();
    }

    /**
     * Get the value of the item as a CharSequence. This is in some cases more efficient than
     * the version of the method that returns a String.
     */

    public CharSequence getStringValueCS() {
        return getStringValue(tree, nodeNr);
    }

    /**
     * Get the string value of a node. This static method allows the string value of a node
     * to be obtained without instantiating the node as a Java object. The method also returns
     * a CharSequence rather than a string, which means it can sometimes avoid copying the
     * data.
     * @param tree The containing document
     * @param nodeNr identifies the node whose string value is required. This must be a
     * document or element node. The caller is trusted to ensure this.
     * @return the string value of the node, as a CharSequence
     */

    public static final CharSequence getStringValue(TinyTree tree, int nodeNr) {
        int level = tree.depth[nodeNr];

        // note, we can't rely on the value being contiguously stored because of whitespace
        // nodes: the data for these may still be present.

        int next = nodeNr+1;

        // we optimize two special cases: firstly, where the node has no children, and secondly,
        // where it has a single text node as a child.

        if (tree.depth[next] <= level) {
            return "";
        } else if (tree.nodeKind[next] == Type.TEXT && tree.depth[next+1] <= level) {
            int length = tree.beta[next];
            int start = tree.alpha[next];
            return new CharSlice(tree.charBuffer, start, length);
        }

        // now handle the general case

        FastStringBuffer sb = null;
        while (next < tree.numberOfNodes && tree.depth[next] > level) {
            if (tree.nodeKind[next]==Type.TEXT) {
                int length = tree.beta[next];
                int start = tree.alpha[next];
                if (sb==null) {
                    sb = new FastStringBuffer(1024);
                }
                sb.append(tree.charBuffer, start, length);
            }
            next++;
        }
        if (sb==null) return "";
        return sb.condense();
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
