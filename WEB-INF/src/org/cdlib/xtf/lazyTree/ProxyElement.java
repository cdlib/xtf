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

import javax.xml.transform.SourceLocator;

import net.sf.saxon.Configuration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.AxisIterator;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.FingerprintedNode;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.trans.XPathException;

/**
 * A very lazy element. It assumes the attributes of the element can be known
 * in advance without actually loading that element. If that assumption
 * proves incorrect (i.e. by accessing anything else), the real element is
 * loaded and its actual attributes are used. This class is used by SearchTree
 * for many of its synthetic nodes.
 * 
 * @author Martin Haye
 */
public final class ProxyElement 
    implements NodeInfo, FingerprintedNode, SourceLocator, SearchElement
{
    /** The actual element (null until loaded */
    ElementImpl   element = null;
    
    /** Document to use for loading */
    LazyDocument  document;
    
    /** Node number represented by this element */
    int           nodeNum;
    
    /** Pre-computed attribute names */
    int[]         attrNames;

    /** Pre-computed attribute values */
    String[]      attrValues;
    
    /** General constructor, attaches to a given lazy-loading document */
    public ProxyElement( LazyDocument realDocument ) {
        this.document = realDocument;
    }

    /** Set the node number for this node. */
    public void setNodeNum( int nodeNum ) {
        this.nodeNum = nodeNum;
    }
    
    /** Allocate the attribute array. */
    public void allocateAttributes( int nAttrs ) {
        attrNames  = new int[nAttrs];
        attrValues = new String[nAttrs];
    }
    
    /** Set an attribute */
    public void setAttribute( int attrNum, int nameCode, String value ) {
        attrNames [attrNum] = nameCode;
        attrValues[attrNum] = value;
    }

    /** Establish the parent node number */
    public void setParentNum( int num ) { }

    /** Establish the child node number */
    public void setChildNum( int num ) { }

    /** Establish the next sibling node number */
    public void setNextSibNum( int num ) { }

    /** Establish the previous sibling node number */
    public void setPrevSibNum( int num ) { }
    
    /** Establish a name for this node */
    public void setNameCode( int code ) { }
    
    /** Obtain the real underlying ElementImpl. If not loaded, load it now. */
    private final ElementImpl real()
    {
        if( element == null )
            element = (SearchElementImpl) 
                document.getNode( nodeNum );
        return element;
    } // real()
    
    /** Loads the real node and defers to it */
    public long getSequenceNumber() {
        return real().getSequenceNumber();
    }
    
    /** Loads the real node and defers to it */
    public int compareOrder( NodeInfo other ) {
        return real().compareOrder( other );
    }
    public Configuration getConfiguration() {
        return real().getConfiguration();
    }
    /** Loads the real node and defers to it */
    public String generateId() {
        return real().generateId();
    }
    /** Loads the real node and defers to it */
    public String getAttributeValue( int fingerprint ) {
        return real().getAttributeValue( fingerprint );
    }
    /** Loads the real node and defers to it */
    public String getBaseURI() {
        return real().getBaseURI();
    }
    /** Loads the real node and defers to it */
    public int getFingerprint() {
        return real().getFingerprint();
    }
    /** Loads the real node and defers to it */
    public String getLocalPart() {
        return real().getLocalPart();
    }
    /** Loads the real node and defers to it */
    public int getNameCode() {
        return real().getNameCode();
    }
    /** Loads the real node and defers to it */
    public NodeInfo getParent() {
        return real().getParent();
    }
    /** Loads the real node and defers to it */
    public String getStringValue() {
        return real().getStringValue();
    }
    /** Loads the real node and defers to it */
    public String getSystemId() {
        return real().getSystemId();
    }
    /** Loads the real node and defers to it */
    public String getURI() {
        return real().getURI();
    }
    /** Loads the real node and defers to it */
    public boolean hasChildNodes() {
        return real().hasChildNodes();
    }
    /** Loads the real node and defers to it */
    public boolean isSameNodeInfo( NodeInfo other ) {
        return real().isSameNodeInfo( other );
    }
    /** 
     * If only the attributes are accessed, uses ProxyAttributeEnumeration
     * to serve up the pre-computed attributes. Otherwise, we load the real
     * element and defer to it. 
     */
    public AxisIterator iterateAxis( byte axisNumber, NodeTest nodeTest ) {
        if( axisNumber == Axis.ATTRIBUTE ) {
             return new ProxyAttributeEnumeration( this, nodeTest );
        }
        return real().iterateAxis( axisNumber, nodeTest );
    }
    /** Loads the real node and defers to it */
    public String getPrefix() {
        return real().getPrefix();
    }
    /** Loads the real node and defers to it */
    public void copy( Receiver out, int whichNamespaces, boolean copyAnnotations, int locationId )
        throws XPathException
    {
        real().copy( out, whichNamespaces, copyAnnotations, locationId );
    }
    /** Loads the real node and defers to it */
    public NamePool getNamePool() {
        return real().getNamePool();
    }
    /** Loads the real node and defers to it */
    public int getNodeKind() {
        return real().getNodeKind();
    }
    /** Loads the real node and defers to it */
    public AxisIterator iterateAxis( byte axisNumber ) {
        return real().iterateAxis( axisNumber );
    }
    /** Loads the real node and defers to it */
    public void setSystemId( String systemId ) {
        real().setSystemId( systemId );
    }
    /** Loads the real node and defers to it */
    public String getDisplayName() {
        return real().getDisplayName();
    }
    /** Loads the real node and defers to it */
    public int getDocumentNumber() {
        return real().getDocumentNumber();
    }
    /** Loads the real node and defers to it */
    public DocumentInfo getDocumentRoot() {
        return real().getDocumentRoot();
    }
    /** Loads the real node and defers to it */
    public NodeInfo getRoot() {
        return real().getRoot();
    }
    /** Loads the real node and defers to it */
    public int getTypeAnnotation() {
        return real().getTypeAnnotation();
    }
    /** Loads the real node and defers to it */
    public void sendNamespaceDeclarations(Receiver out, boolean includeAncestors)
        throws XPathException 
    {
        real().sendNamespaceDeclarations(out, includeAncestors);
    }
    /** Loads the real node and defers to it */
    public int getColumnNumber() {
        return real().getColumnNumber();
    }
    /** Loads the real node and defers to it */
    public int[] getDeclaredNamespaces(int[] buffer) {
        return real().getDeclaredNamespaces(buffer);
    }
    /** Loads the real node and defers to it */
    public int getLineNumber() {
        return real().getLineNumber();
    }
    /** Loads the real node and defers to it */
    public String getPublicId() {
        return real().getPublicId();
    }
    /** Loads the real node and defers to it */
    public CharSequence getStringValueCS() {
        return real().getStringValueCS();
    }
    /** Loads the real node and defers to it */
    public SequenceIterator getTypedValue() throws XPathException {
        return real().getTypedValue();
    }
} // ProxyElement
