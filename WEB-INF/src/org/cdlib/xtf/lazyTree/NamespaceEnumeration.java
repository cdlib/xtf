package org.cdlib.xtf.lazyTree;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.om.NamespaceConstant;
import net.sf.saxon.om.AxisIteratorImpl;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.type.Type;
import java.util.ArrayList;

/**
* Enumeration of the namespace nodes of an element
*/

final class NamespaceEnumeration extends AxisIteratorImpl {

    private LazyDocument document;
    private ElementImpl element;
    private NamePool pool;
    private ElementImpl currentElement;
    private int index;
    private ArrayList list = new ArrayList();
    private NodeTest nodeTest;
    private int xmlNamespace;

    /**
    * Constructor. Note: this constructor will only be called if the owning
    * node is an element. Otherwise, an EmptyEnumeration will be returned
    */

    protected NamespaceEnumeration(ElementImpl node, NodeTest nodeTest) {
        // System.err.println("new NS enum, nodetest = " + nodeTest.getClass());
        element = node;
        document = node.document;
        // document.diagnosticDump();
        pool = document.getNamePool();
        currentElement = node;
        index = node.nameSpace;
        this.nodeTest = nodeTest;
        xmlNamespace = pool.allocate("", "", "xml");
    }

    private void advance() {
        if (index == 0) {
            index = -1;
            return;
        } else if (index > 0) {
            while (index < document.numberOfNamespaces &&
                            document.namespaceParent[index] == currentElement.nodeNum) {

                int nsCode = document.namespaceCode[index];

                // don't return a namespace undeclaration (xmlns=""), but add it to the list
                // of prefixes encountered, to suppress outer xmlns="xyz" declarations

                if (nsCode == NamespaceConstant.NULL_CODE) {
                    list.add(new Short((short)0));
                } else {
                    if (matches(nsCode)) {
                        short prefixCode = (short)(nsCode>>16);

                        int max = list.size();
                        boolean duplicate = false;

                        // Don't add a node if the prefix has been previously encountered
                        for (int j=0; j<max; ) {
                            short nsj = ((Short)(list.get(j++))).shortValue();
                            if (nsj==prefixCode) {
                                duplicate = true;
                                break;
                            }
                        }
                        if (!duplicate) {
                            list.add(new Short(prefixCode));
                            return;
                        }
                    }
                }

                index++;
            }
        }

        NodeInfo parent = currentElement.getParent();
        if (parent==null || parent.getNodeKind()==Type.DOCUMENT) {
            if (nodeTest.matches(Type.NAMESPACE, xmlNamespace, -1)) {
                index = 0;
            } else {
                index = -1;
            }
        } else {
            currentElement = (ElementImpl) parent;
            index = currentElement.nameSpace;
            advance();
        }

    }

    private boolean matches(int nsCode) {
        if (nodeTest instanceof NodeKindTest && nodeTest.getPrimitiveType()==Type.NAMESPACE) {
            // fast path when selecting namespace::*
            return true;
        } else {
            int nameCode = pool.allocate("", "", pool.getPrefixFromNamespaceCode(nsCode));
            return nodeTest.matches(Type.NAMESPACE, nameCode, -1);
        }
    }

    public Item next() {
        advance();
        if (index >= 0) {
            position++;
            current = document.getNamespaceNode(index);
            ((NamespaceImpl)current).setParentNode(element);
            return current;
        } else {
            return null;
        }
    }

    /**
    * Get another enumeration of the same nodes
    */

    public SequenceIterator getAnother() {
        return new NamespaceEnumeration(element, nodeTest);
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
