package org.cdlib.xtf.lazyTree.hackedSaxon;

import net.sf.saxon.event.Receiver;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;

/**
  * TinyCommentImpl is an implementation of CommentInfo
  * @author Michael H. Kay
  */
final class TinyCommentImpl extends TinyNodeImpl 
{
  public TinyCommentImpl(TinyTree tree, int nodeNr) {
    this.tree = tree;
    this.nodeNr = nodeNr;
  }

  /**
  * Get the XPath string value of the comment
  */
  public final String getStringValue() {
    int start = tree.alpha[nodeNr];
    int len = tree.beta[nodeNr];
    if (len == 0)
      return "";
    char[] dest = new char[len];
    tree.commentBuffer.getChars(start, start + len, dest, 0);
    return new String(dest, 0, len);
  }

  /**
  * Get the node type
  * @return Type.COMMENT
  */
  public final int getNodeKind() {
    return Type.COMMENT;
  }

  /**
  * Copy this node to a given outputter
  */
  public void copy(Receiver out, int whichNamespaces, boolean copyAnnotations,
                   int locationId)
    throws XPathException 
  {
    out.comment(getStringValue(), 0, 0);
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
