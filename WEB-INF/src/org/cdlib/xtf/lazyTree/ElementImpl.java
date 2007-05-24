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
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;
import org.cdlib.xtf.util.PackedByteBuf;
import java.io.IOException;

/**
 * A node in the XML parse tree representing an XML element.<P>
 * This class is an implementation of NodeInfo. The object is a wrapper around
 * one entry in the arrays maintained by the LazyTree. Note that the same node
 * might be represented by different LazyElementImpl objects at different times.
 * @author Michael H. Kay
 */
public class ElementImpl extends ParentNodeImpl 
{
  int nameSpace;
  int[] attrNames;
  String[] attrValues;

  public void init(int attrOffset, int nameSpace)
    throws IOException 
  {
    this.nameSpace = nameSpace;

    if (attrOffset >= 0) 
    {
      // Read in the attributes.
      document.attrFile.seek(attrOffset);
      document.attrFile.read(document.attrBytes);

      PackedByteBuf buf = document.attrBuf;
      buf.setBytes(document.attrBytes);

      int nAttrs = buf.readInt();
      attrNames = new int[nAttrs];
      attrValues = new String[nAttrs];

      for (int i = 0; i < nAttrs; i++) {
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
    return document.getTypeAnnotation(nodeNum);
  }

  /**
  * Output all namespace nodes associated with this element.
  * @param out The relevant outputter
   * @param includeAncestors True if namespaces associated with ancestor
   */
  public void sendNamespaceDeclarations(Receiver out, boolean includeAncestors)
    throws XPathException 
  {
    if (!document.usesNamespaces) {
      return;
    }

    int ns = nameSpace;
    if (ns > 0) 
    {
      while (ns < document.numberOfNamespaces &&
             document.namespaceParent[ns] == nodeNum) 
      {
        int nscode = document.namespaceCode[ns];
        out.namespace(nscode, 0);
        ns++;
      }
    }

    // now add the namespaces defined on the ancestor nodes. We rely on the receiver
    // to eliminate multiple declarations of the same prefix
    if (includeAncestors && document.isUsingNamespaces()) 
    {
      NodeInfo parent = getParent();
      if (parent != null) {
        parent.sendNamespaceDeclarations(out, true);
      }

      // terminates when the parent is a root node
    }
  }

  /**
   * Get all namespace undeclarations and undeclarations defined on this element.
   *
   * @param buffer If this is non-null, and the result array fits in this buffer, then the result
   *               may overwrite the contents of this array, to avoid the cost of allocating a new array on the heap.
   * @return An array of integers representing the namespace declarations and undeclarations present on
   *         this element. For a node other than an element, return null. Otherwise, the returned array is a
   *         sequence of namespace codes, whose meaning may be interpreted by reference to the name pool. The
   *         top half word of each namespace code represents the prefix, the bottom half represents the URI.
   *         If the bottom half is zero, then this is a namespace undeclaration rather than a declaration.
   *         The XML namespace is never included in the list. If the supplied array is larger than required,
   *         then the first unused entry will be set to -1.
   *         <p/>
   *         <p>For a node other than an element, the method returns null.</p>
   */
  public int[] getDeclaredNamespaces(int[] buffer) {
    return getDeclaredNamespaces(document, nodeNum, nameSpace, buffer);
  }

  /**
   * Static method to get all namespace undeclarations and undeclarations defined on a given element,
   * without instantiating the node object.
   * @param doc The lazy document containing the given element node
   * @param nodeNr The node number of the given element node within the tinyTree
   * @param buffer If this is non-null, and the result array fits in this buffer, then the result
   *               may overwrite the contents of this array, to avoid the cost of allocating a new array on the heap.
   * @return An array of integers representing the namespace declarations and undeclarations present on
   *         this element. For a node other than an element, return null. Otherwise, the returned array is a
   *         sequence of namespace codes, whose meaning may be interpreted by reference to the name pool. The
   *         top half word of each namespace code represents the prefix, the bottom half represents the URI.
   *         If the bottom half is zero, then this is a namespace undeclaration rather than a declaration.
   *         The XML namespace is never included in the list. If the supplied array is larger than required,
   *         then the first unused entry will be set to -1.
   *         <p/>
   *         <p>For a node other than an element, the method returns null.</p>
   */
  static int[] getDeclaredNamespaces(LazyDocument doc, int nodeNr,
                                     int nameSpace, int[] buffer) 
  {
    int ns = nameSpace;
    if (ns > 0) 
    {
      int count = 0;
      while (ns < doc.numberOfNamespaces && doc.namespaceParent[ns] == nodeNr) {
        count++;
        ns++;
      }
      if (count == 0) 
      {
        return NodeInfo.EMPTY_NAMESPACE_LIST;
      }
      else if (count <= buffer.length) {
        System.arraycopy(doc.namespaceCode, nameSpace, buffer, 0, count);
        if (count < buffer.length) {
          buffer[count] = -1;
        }
        return buffer;
      }
      else {
        int[] array = new int[count];
        System.arraycopy(doc.namespaceCode, nameSpace, array, 0, count);
        return array;
      }
    }
    else {
      return NodeInfo.EMPTY_NAMESPACE_LIST;
    }
  }

  /**
   * Get the value of a given attribute of this node
   * @param fingerprint The fingerprint of the attribute name
   * @return the attribute value if it exists or null if not
   */
  public String getAttributeValue(int fingerprint) 
  {
    if (attrNames == null)
      return null;

    for (int i = 0; i < attrNames.length; i++) {
      if ((attrNames[i] & 0xfffff) == fingerprint)
        return attrValues[i];
    } // for i
    return null;
  }

  /**
  * Copy this node to a given receiver
  * @param whichNamespaces indicates which namespaces should be copied: all, none,
  * or local (i.e., those not declared on a parent element)
  */
  public void copy(Receiver out, int whichNamespaces, boolean copyAnnotations,
                   int locationId)
    throws XPathException 
  {
    int typeCode = (copyAnnotations ? getTypeAnnotation() : StandardNames.XS_UNTYPED);
    if (locationId == 0 && out instanceof LocationCopier) {
      out.setSystemId(getSystemId());
      ((LocationCopier)out).setLineNumber(getLineNumber());
    }
    out.startElement(nameCode, typeCode, locationId, 0);

    // output the namespaces
    if (whichNamespaces != NO_NAMESPACES)
      sendNamespaceDeclarations(out, whichNamespaces == ALL_NAMESPACES);

    // output the attributes
    if (attrNames != null) 
    {
      for (int i = 0; i < attrNames.length; i++) {
        new AttributeImpl(this, i).copy(out,
                                        NO_NAMESPACES,
                                        copyAnnotations,
                                        locationId);
      } // for i
    } // if

    // indicate start of content
    out.startContent();

    // output the children
    AxisIterator children = iterateAxis(Axis.CHILD);

    int childNamespaces = (whichNamespaces == NO_NAMESPACES ? NO_NAMESPACES
                           : LOCAL_NAMESPACES);
    while (true) {
      NodeInfo next = (NodeInfo)children.next();
      if (next == null)
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
