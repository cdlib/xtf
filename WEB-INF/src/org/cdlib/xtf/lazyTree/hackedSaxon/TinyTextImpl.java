package org.cdlib.xtf.lazyTree.hackedSaxon;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.type.Type;
import net.sf.saxon.xpath.XPathException;

import org.w3c.dom.*;

/**
  * A node in the XML parse tree representing character content<P>
  * @author Michael H. Kay
  */

final class TinyTextImpl extends TinyNodeImpl implements Text {

    public TinyTextImpl(TinyDocumentImpl doc, int nodeNr) {
        this.document = doc;
        this.nodeNr = nodeNr;
    }

    /**
    * Return the character value of the node.
    * @return the string value of the node
    */

    public String getStringValue() {
        int start = document.alpha[nodeNr];
        int len = document.beta[nodeNr];
        return new String(document.charBuffer, start, len);
    }

    /**
    * Return the type of node.
    * @return Type.TEXT
    */

    public final int getNodeKind() {
        return Type.TEXT;
    }

    /**
    * Copy this node to a given outputter
    */

    public void copy(Receiver out, int whichNamespaces, boolean copyAnnotations, int locationId) throws XPathException {
        int start = document.alpha[nodeNr];
        int len = document.beta[nodeNr];
        out.characters(new CharSlice(document.charBuffer, start, len), 0, 0);
    }

    /**
    * Copy the string-value of this node to a given outputter
    */
/*
    public void copyStringValue(Receiver out) throws XPathException {
        int start = document.offset[nodeNr];
        int len = document.length[nodeNr];
        out.characters(CharBuffer.wrap(document.charBuffer, start, len), 0);
    }
*/
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
