package org.cdlib.xtf.lazyTree.hackedSaxon;
//import net.sf.saxon.om.*;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.type.Type;

import javax.xml.transform.TransformerException;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.DOMException;

/**
  * TProcInstImpl is an implementation of ProcInstInfo
  * @author Michael H. Kay (michael.h.kay@ntlworld.com)
  * @version 16 July 1999
  */


final class TinyProcInstImpl extends TinyNodeImpl implements ProcessingInstruction {

    public TinyProcInstImpl(TinyDocumentImpl doc, int nodeNr) {
        this.document = doc;
        this.nodeNr = nodeNr;
    }

    public String getStringValue() {
        int start = document.alpha[nodeNr];
        int len = document.beta[nodeNr];
        if (len==0) {
            return "";    // need to special-case this for the Microsoft JVM
        }
        char[] dest = new char[len];
        document.commentBuffer.getChars(start, start+len, dest, 0);
        return new String(dest, 0, len);
    }

    public final int getNodeKind() {
        return Type.PROCESSING_INSTRUCTION;
    }

    /**
    * Copy this node to a given outputter
    */

    public void copy(Receiver out, int whichNamespaces, boolean copyAnnotations) throws TransformerException {
        out.processingInstruction(getDisplayName(), getStringValue(), 0);
    }

    // DOM methods

    /**
     * The target of this processing instruction. XML defines this as being
     * the first token following the markup that begins the processing
     * instruction.
     */

    public String getTarget() {
        return getDisplayName();
    }

    /**
     *  The content of this processing instruction. This is from the first non
     * white space character after the target to the character immediately
     * preceding the <code>?&gt;</code> .
     */

    public String getData() {
        return getStringValue();
    }

    /**
     * Set the content of this PI. Always fails.
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised when the node is readonly.
     */

    public void setData(String data) throws DOMException {
        disallowUpdate();
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
