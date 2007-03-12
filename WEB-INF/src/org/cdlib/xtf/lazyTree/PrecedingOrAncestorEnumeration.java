package org.cdlib.xtf.lazyTree;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.pattern.NodeTest;

final class PrecedingOrAncestorEnumeration extends TreeEnumeration 
{

    /**
    * This axis cannot be requested directly in an XPath expression
    * but is used when evaluating xsl:number. It is provided because
    * taking the union of the two axes would be very inefficient
    */

   
    public PrecedingOrAncestorEnumeration(NodeImpl node, NodeTest nodeTest) {
        super(node, nodeTest);
        advance();   
    }

    protected void step() {
        next = next.getPreviousInDocument();
    }

    /**
    * Get another enumeration of the same nodes
    */
    
    public SequenceIterator getAnother() {
        return new PrecedingOrAncestorEnumeration(start, nodeTest);
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
