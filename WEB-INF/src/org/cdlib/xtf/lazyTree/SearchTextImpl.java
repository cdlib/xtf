package org.cdlib.xtf.lazyTree;

/**
 * Copyright (c) 2004, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, 
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, 
 *   this list of conditions and the following disclaimer in the documentation 
 *   and/or other materials provided with the distribution.
 * - Neither the name of the University of California nor the names of its
 *   contributors may be used to endorse or promote products derived from this 
 *   software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * Represents a text node that has been (possibly) modified to reflect query
 * results.
 * 
 * @author Martin Haye
 */
public class SearchTextImpl extends TextImpl implements SearchNode
{
    /** Default constructor; attaches to the given document */
    public SearchTextImpl( SearchTree document ) {
        super( document );
    }
    
    /** Establish the node number */
    public void setNodeNum( int num ) { nodeNum = num; }
    
    /** Establish the parent node number */
    public void setParentNum( int num )  { parentNum  = num; }
    
    /** Establish the next sibling node number */
    public void setNextSibNum( int num ) { nextSibNum = num; }
    
    /** Establish the previous sibling node number */
    public void setPrevSibNum( int num ) { prevSibNum = num; }

    /** Establish the text value for this node */
    public void setStringValue( String newText ) {
        text = newText;
    }
    
    /**
     * Get a unique sequence number for this node. These are used for
     * sorting nodes in document order.
     */
    protected long getSequenceNumber()
    {
        // If this node isn't virtual, do the normal thing.
        if( nodeNum <= SearchTree.VIRTUAL_MARKER )
            return super.getSequenceNumber();
        
        // Okay, find the next previous non-virtual node, and use its sequence
        // number as a base, to which we add the count of intervening virtual
        // nodes.
        //
        NodeImpl node = this;
        int count = 0;
        while( (node = node.getPreviousInDocument()) != null ) {
            ++count;
            if( node.nodeNum <= SearchTree.VIRTUAL_MARKER )
                return node.getSequenceNumber() + (count << 16);
        }
        
        assert false : "Virtual node must be preceeded by some real node";
        return 0;
    } // getSequenceNumber()
    
} // class SearchTextImpl
