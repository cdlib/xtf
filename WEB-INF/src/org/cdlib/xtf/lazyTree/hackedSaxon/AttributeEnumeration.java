package org.cdlib.xtf.lazyTree.hackedSaxon;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.AxisIteratorImpl;
import net.sf.saxon.type.Type;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.pattern.NameTest;
import net.sf.saxon.value.UntypedAtomicValue;

/**
* AttributeEnumeration is an iterator over all the attribute nodes of an Element.
*/

final class AttributeEnumeration extends AxisIteratorImpl {

    private TinyDocumentImpl doc;
    private int element;
    private NodeTest nodeTest;
    private int index;

    /**
    * Constructor. Note: this constructor will only be called if the relevant node
    * is an element and if it has one or more attributes. Otherwise an EmptyEnumeration
    * will be constructed instead.
    * @param doc the containing document
    * @param element the element whose attributes are required
    * @param nodeTest condition to be applied to the names of the attributes selected
    */

    protected AttributeEnumeration(TinyDocumentImpl doc, int element, NodeTest nodeTest) {

        this.nodeTest = nodeTest;
        this.doc = doc;
        this.element = element;
        index = doc.alpha[element];
    }

    public final void setIsAtomizing(boolean atomizing) {
        super.setIsAtomizing(atomizing);
    }

    /**
    * Get the next node in the iteration.
    */

    public Item next() {
        while (true) {
            if (index >= doc.numberOfAttributes || doc.attParent[index] != element) {
                index = Integer.MAX_VALUE;
                return null;
            }
            int typeCode = doc.getAttributeAnnotation(index);
            if (nodeTest.matches(Type.ATTRIBUTE, doc.attCode[index], typeCode)) {
                position++;
                int node = index++;
                if (nodeTest instanceof NameTest) {
                    // there can only be one match, so abandon the search after this node
                    index = Integer.MAX_VALUE;
                }
                if (isAtomizing() && typeCode==-1) {
                    // optimization: avoid creating the Node object if not needed
                    current = new UntypedAtomicValue(doc.attValue[node]);
                    return current;
                } else {
                    current = doc.getAttributeNode(node);
                    return current;
                }
            }
            index++;
        }
    }

    /**
    * Get another iteration over the same nodes
    */

    public SequenceIterator getAnother() {
        return new AttributeEnumeration(doc, element, nodeTest);
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
