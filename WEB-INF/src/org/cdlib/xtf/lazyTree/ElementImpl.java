package org.cdlib.xtf.lazyTree;

// IMPORTANT NOTE: When comparing, this file is most similar to Saxon's
//                 net.sf.saxon.tinytree.TinyElementImpl.java
//

import net.sf.saxon.event.LocationCopier;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.AxisIterator;
import net.sf.saxon.om.Navigator;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.tree.DOMExceptionImpl;
import net.sf.saxon.type.Type;
import net.sf.saxon.xpath.XPathException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

import org.cdlib.xtf.util.PackedByteBuf;

import java.io.IOException;

/**
 * A node in the XML parse tree representing an XML element.<P>
 * This class is an implementation of NodeInfo and also implements the
 * DOM Element interface
  * @author Michael H. Kay
 */

class ElementImpl extends ParentNodeImpl
    implements Element {

    int nameSpace;
    
    int[]    attrNames;
    String[] attrValues;
    
   /**
    * Constructor
    */

    public ElementImpl( LazyDocument document ) {
        super( document );
    }
    
    public void init( int attrOffset, int nameSpace )
        throws IOException
    {
        this.nameSpace  = nameSpace;
        
        if( attrOffset >= 0 ) 
        {
            // Read in the attributes.
            document.attrFile.seek( attrOffset );
            document.attrFile.readFully( document.attrBytes );
            
            PackedByteBuf buf = document.attrBuf;
            buf.setBytes( document.attrBytes );
            
            int nAttrs = buf.readInt();
            attrNames  = new int[nAttrs];
            attrValues = new String[nAttrs];
            
            for( int i = 0; i < nAttrs; i++ ) {
                int nameNum = buf.readInt();
                attrNames[i] = document.nameNumToCode[nameNum];
                attrValues[i] = buf.readString();
            } // for i
        } // if
    } // init()
    
    /**
     * Return the type of node.
     * @return Type.ELEMENT
     */

    public final int getNodeKind() {
        return Type.ELEMENT;
    }

    /**
    * Get the base URI of this element node. This will be the same as the System ID unless
    * xml:base has been used.
     */

    public String getBaseURI() {
        return Navigator.getBaseURI(this);
    }

    /**
     * Get the type annotation of this node, if any
     * Returns Type.UNTYPED_ANY if there is no type annotation
     */
    public int getTypeAnnotation() {
        return document.getElementAnnotation( nodeNum );
    }

    /**
     * Output all namespace nodes associated with this element.
     * @param out The relevant outputter
     * @param includeAncestors True if namespaces associated with ancestor
     * elements must also be output; false if these are already known to be
     * on the result tree.
     */
    public void outputNamespaceNodes( Receiver out, boolean includeAncestors )
         throws XPathException {

        int ns = nameSpace;
        if( ns > 0 ) {
            while (ns < document.numberOfNamespaces &&
                    document.namespaceParent[ns] == nodeNum ) {
                int nscode = document.namespaceCode[ns];
                out.namespace(nscode, 0);
                ns++;
            }
        }

        // now add the namespaces defined on the ancestor nodes. We rely on the receiver
        // to eliminate multiple declarations of the same prefix

        if (includeAncestors && document.isUsingNamespaces()) {
            NodeInfo parent = getParent();
            if (parent != null) {
                parent.outputNamespaceNodes(out, true);
            }
            // terminates when the parent is a root node
        }
    }

    /**
     * Returns whether this node (if it is an element) has any attributes.
     * @return <code>true</code> if this node has any attributes,
     *   <code>false</code> otherwise.
     * @since DOM Level 2
     */

    public boolean hasAttributes() {
        return attrNames != null;
    }

    /**
     * Get the value of a given attribute of this node
     * @param fingerprint The fingerprint of the attribute name
     * @return the attribute value if it exists or null if not
     */

    public String getAttributeValue(int fingerprint) {
        if( attrNames == null )
            return null;
        
        for( int i = 0; i < attrNames.length; i++ ) {
            if( (attrNames[i] & 0xfffff) == fingerprint )
                return attrValues[i];
        } // for i
        return null;
    }

    /**
    * Set the value of an attribute on the current element. This affects subsequent calls
    * of getAttribute() for that element.
    * @param name The name of the attribute to be set. Any prefix is interpreted relative
    * to the namespaces defined for this element.
    * @param value The new value of the attribute. Set this to null to remove the attribute.
    */

    public void setAttribute(String name, String value ) throws DOMException {
        throw new DOMExceptionImpl((short)9999, "LazyTree DOM is not updateable");
    }

    /**
     * Find the value of a given attribute of this node. <BR>
     * This method is defined on all nodes to meet XSL requirements, but for 
     * nodes other than elements it will always return null.
     * 
     * @param uri the namespace uri of an attribute
     * @param localName the local name of an attribute
     * @return the value of the attribute, if it exists, otherwise null
     */
    public String getAttributeValue( String uri, String localName ) {
        int f = document.getNamePool().getFingerprint( uri, localName );
        return getAttributeValue( f );
    }

    public void copy(Receiver out, int whichNamespaces, boolean copyAnnotations, int locationId) throws XPathException {

        int typeCode = (copyAnnotations ? getTypeAnnotation() : 0);
        if (locationId == 0 && out instanceof LocationCopier) {
            out.setSystemId(getSystemId());
            ((LocationCopier)out).setLineNumber(getLineNumber());
        }
        out.startElement( nameCode, typeCode, locationId, 0 );

        // output the namespaces
        if (whichNamespaces != NO_NAMESPACES)
            outputNamespaceNodes(out, whichNamespaces == ALL_NAMESPACES);

        // output the attributes
        if( attrNames != null )
        {
            for( int i = 0; i < attrNames.length; i++ ) {
                new AttributeImpl(this, i).copy( 
                        out, NO_NAMESPACES, copyAnnotations, locationId );
            } // for i
        } // if
        
        // output the children
        AxisIterator children = iterateAxis( Axis.CHILD );

        int childNamespaces =
            (whichNamespaces == NO_NAMESPACES
                ? NO_NAMESPACES
                : LOCAL_NAMESPACES);
        while( true ) {
            NodeInfo next = (NodeInfo) children.next();
            if( next == null )
                break;
            next.copy(out, childNamespaces, copyAnnotations, locationId);
        }
        out.endElement();
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
