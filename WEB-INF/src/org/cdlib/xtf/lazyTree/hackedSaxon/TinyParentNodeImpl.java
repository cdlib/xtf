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

    public boolean hasChildNodes() {
        return (nodeNr+1 < document.numberOfNodes &&
                document.depth[nodeNr+1] > document.depth[nodeNr]);
    }

    /**
    * Return the string-value of the node, that is, the concatenation
    * of the character content of all descendent elements and text nodes.
    * @return the accumulated character content of the element, including descendant elements.
    */

    public String getStringValue() {
        int level = document.depth[nodeNr];
        StringBuffer sb = null;

        // note, we can't rely on the value being contiguously stored because of whitespace
        // nodes: the data for these may still be present.

        int next = nodeNr+1;
        while (next < document.numberOfNodes && document.depth[next] > level) {
            if (document.nodeKind[next]==Type.TEXT) {
                if (sb==null) {
                    sb = new StringBuffer();
                }
                int length = document.beta[next];
                int start = document.alpha[next];
                sb.append(document.charBuffer, start, length);
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
