package org.cdlib.xtf.lazyTree.hackedSaxon;
import net.sf.saxon.om.NamePool;

import java.util.Iterator;

/**
* An iterator supplying the prefixes of the declared namespaces for an element node in a TinyTree
*/

final class DeclaredPrefixIterator implements Iterator {

    private TinyTree tree;
    private NamePool pool;
    private int owner;
    private int index;


    public DeclaredPrefixIterator(TinyElementImpl node) {
        owner = node.nodeNr;
        tree = node.tree;
        pool = tree.getNamePool();
        index = tree.beta[owner]; // by convention
    }

    /**
     * Returns <tt>true</tt> if the iteration has more elements. (In other
     * words, returns <tt>true</tt> if <tt>next</tt> would return an element
     * rather than throwing an exception.)
     *
     * @return <tt>true</tt> if the iterator has more elements.
     */

    public boolean hasNext() {
        return index >= 0 && tree.namespaceParent[index] == owner;
    }

    /**
     * Returns the next element in the iteration.  Calling this method
     * repeatedly until the {@link #hasNext()} method returns false will
     * return each element in the underlying collection exactly once.
     *
     * @return the next element in the iteration.
     * @throws java.util.NoSuchElementException
     *          iteration has no more elements.
     */

    public Object next() {
        int nscode = tree.namespaceCode[index--];
        return pool.getPrefix(nscode>>16);
    }


    /**
     * Removes from the underlying collection the last element returned by the
     * iterator (optional operation).  This method can be called only once per
     * call to <tt>next</tt>.  The behavior of an iterator is unspecified if
     * the underlying collection is modified while the iteration is in
     * progress in any way other than by calling this method.
     *
     * @throws UnsupportedOperationException if the <tt>remove</tt>
     *                                       operation is not supported by this Iterator.
     * @throws IllegalStateException         if the <tt>next</tt> method has not
     *                                       yet been called, or the <tt>remove</tt> method has already
     *                                       been called after the last call to the <tt>next</tt>
     *                                       method.
     */
    public void remove() {
        throw new UnsupportedOperationException();
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
