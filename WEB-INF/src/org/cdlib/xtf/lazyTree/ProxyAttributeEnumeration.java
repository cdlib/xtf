package org.cdlib.xtf.lazyTree;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.AxisIteratorImpl;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.LookaheadIterator;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.pattern.NameTest;

/**
* Saxon: ProxyAttributeEnumeration is an enumeration of all the attribute nodes 
* of an Element. It is used by {@link ProxyElement} to implement lazy attribute
* loading.
*/

final class ProxyAttributeEnumeration extends AxisIteratorImpl implements LookaheadIterator {

    private ProxyElement element;
    private NodeTest nodeTest;
    private NodeInfo next;
    private int index;
    private int length;

    /**
    * Constructor
    * @param element the element whose attributes are required. This may be any type of node,
    * but if it is not an element the enumeration will be empty
    * @param nodeTest condition to be applied to the names of the attributes selected
    */

    public ProxyAttributeEnumeration(ProxyElement element, NodeTest nodeTest) {

        this.nodeTest = nodeTest;

        if( element.attrNames == null )
            return;

        if (nodeTest instanceof NameTest) {
            NameTest test = (NameTest)nodeTest;
            int fingerprint = test.getFingerprint();
            for( int i = 0; i < element.attrNames.length; i++ ) {
                if( (element.attrNames[i] & 0xfffff) == fingerprint ) {
                    next = new ProxyAttributeImpl( element, i );
                    return;
                }
            }
            
            next = null;
        } else  {
            length = element.attrNames.length;
            advance();
        }
    } // constructor

    /**
    * Test if there are mode nodes still to come.
    * ("elements" is used here in the sense of the Java enumeration class, not in the XML sense)
    */

    public boolean hasNext() {
        return next != null;
    }

    /**
    * Get the next node in the iteration, or null if there are no more.
    */

    public Item next() {
        if (next == null) {
            return null;
        } else {
            current = next;
            position++;
            advance();
            return current;
        }
    }

    /**
    * Move to the next node in the enumeration.
    */

    private void advance() {
        do {
            if (index<length) {
                next = new ProxyAttributeImpl(element, index);
                index++;
            } else {
                next = null;
                return;
            }
        } while (!nodeTest.matches(next.getNodeKind(),
                                   next.getFingerprint(),
                                   next.getTypeAnnotation()));
    }

    /**
    * Get another enumeration of the same nodes
    */

    public SequenceIterator getAnother() {
        return new ProxyAttributeEnumeration(element, nodeTest);
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
