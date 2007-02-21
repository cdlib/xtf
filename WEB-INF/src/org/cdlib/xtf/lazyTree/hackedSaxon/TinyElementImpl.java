package org.cdlib.xtf.lazyTree.hackedSaxon;

import net.sf.saxon.event.Receiver;
import net.sf.saxon.om.NamespaceResolver;
import net.sf.saxon.om.Navigator;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;

/**
  * A node in the XML parse tree representing an XML element.<P>
  * This class is an implementation of NodeInfo and also implements the
  * DOM Element interface
  * @author Michael H. Kay
  */
final class TinyElementImpl extends TinyParentNodeImpl 
{
  /**
  * Constructor
  */
  public TinyElementImpl(TinyTree tree, int nodeNr) {
    this.tree = tree;
    this.nodeNr = nodeNr;
  }

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
    return tree.getElementAnnotation(nodeNr);
  }

  /**
  * Output all namespace nodes associated with this element.
  * @param out The relevant outputter
   * @param includeAncestors True if namespaces associated with ancestor
   */
  public void sendNamespaceDeclarations(Receiver out, boolean includeAncestors)
    throws XPathException 
  {
    int ns = tree.beta[nodeNr]; // by convention
    if (ns > 0) {
      while (ns < tree.numberOfNamespaces &&
             tree.namespaceParent[ns] == nodeNr) 
      {
        int nscode = tree.namespaceCode[ns];
        out.namespace(nscode, 0);
        ns++;
      }
    }

    // now add the namespaces defined on the ancestor nodes. We rely on the receiver
    // to eliminate multiple declarations of the same prefix
    if (includeAncestors) 
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
    return getDeclaredNamespaces(tree, nodeNr, buffer);
  }

  static int[] getDeclaredNamespaces(TinyTree tree, int nodeNr, int[] buffer) 
  {
    int ns = tree.beta[nodeNr]; // by convention
    if (ns > 0) {
      int count = 0;
      while (ns < tree.numberOfNamespaces &&
             tree.namespaceParent[ns] == nodeNr) 
      {
        count++;
        ns++;
      }
      if (count == 0) 
      {
        return NodeInfo.EMPTY_NAMESPACE_LIST;
      }
      else if (count <= buffer.length) {
        System.arraycopy(tree.namespaceCode, tree.beta[nodeNr], buffer, 0, count);
        if (count < buffer.length) {
          buffer[count] = -1;
        }
        return buffer;
      }
      else {
        int[] array = new int[count];
        System.arraycopy(tree.namespaceCode, tree.beta[nodeNr], array, 0, count);
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
    int a = tree.alpha[nodeNr];
    if (a < 0)
      return null;
    while (a < tree.numberOfAttributes && tree.attParent[a] == nodeNr) 
    {
      if ((tree.attCode[a] & 0xfffff) == fingerprint) {
        return tree.attValue[a].toString();
      }
      a++;
    }
    return null;
  }

  /**
  * Copy this node to a given receiver
  * @param whichNamespaces indicates which namespaces should be copied: all, none,
  * or local (i.e., those not declared on a parent element)
  */
  public void copy(Receiver receiver, int whichNamespaces,
                   boolean copyAnnotations, int locationId)
    throws XPathException 
  {
    // Based on an algorithm supplied by Ruud Diterwich

    // Performance measurements show that this achieves no speed-up over the OLD version
    // (in 7.4). So might as well switch back.

    // control vars
    short level = -1;
    boolean closePending = false;
    short startLevel = tree.depth[nodeNr];
    boolean first = true;
    int next = nodeNr;

    // document.diagnosticDump();
    do 
    {
      // determine node depth
      short nodeLevel = tree.depth[next];

      // extra close required?
      if (closePending) {
        level++;
      }

      // close former elements
      for (; level > nodeLevel; level--) {
        receiver.endElement();
      }

      // new node level
      level = nodeLevel;

      // output depends on node type
      switch (tree.nodeKind[next]) 
      {
        case Type.ELEMENT: 
        {
          // start element
          receiver.startElement(tree.nameCode[next],
                                (copyAnnotations
                                 ? tree.getElementAnnotation(next) : -1),
                                locationId,
                                0);

          //(first ? ReceiverOptions.DISINHERIT_NAMESPACES : 0));

          // there is an element to close
          closePending = true;

          // output namespaces
          if (whichNamespaces != NO_NAMESPACES) 
          {
            if (first) 
            {
              sendNamespaceDeclarations(receiver,
                                        whichNamespaces == ALL_NAMESPACES);
            }
            else {
              int ns = tree.beta[next]; // by convention
              if (ns > 0) {
                while (ns < tree.numberOfNamespaces &&
                       tree.namespaceParent[ns] == next) 
                {
                  int nscode = tree.namespaceCode[ns];
                  receiver.namespace(nscode, 0);
                  ns++;
                }
              }
            }
          }
          first = false;

          // output attributes
          int att = tree.alpha[next];
          if (att >= 0) 
          {
            while (att < tree.numberOfAttributes &&
                   tree.attParent[att] == next) 
            {
              int attCode = tree.attCode[att];
              int attType = (copyAnnotations ? tree.getAttributeAnnotation(att)
                             : -1);
              receiver.attribute(attCode,
                                 attType,
                                 tree.attValue[att],
                                 locationId,
                                 0);
              att++;
            }
          }

          // start content
          receiver.startContent();
          break;
        }
        case Type.TEXT: 
        {
          // don't close text nodes
          closePending = false;

          // output characters
          int start = tree.alpha[next];
          int len = tree.beta[next];
          receiver.characters(new CharSlice(tree.charBuffer, start, len),
                              locationId,
                              0);
          break;
        }
        case Type.COMMENT: 
        {
          // don't close text nodes
          closePending = false;

          // output copy of comment
          int start = tree.alpha[next];
          int len = tree.beta[next];
          if (len > 0) {
            receiver.comment(tree.commentBuffer.subSequence(start, start + len),
                             locationId,
                             0);
          }
          else {
            receiver.comment("", 0, 0);
          }
          break;
        }
        case Type.PROCESSING_INSTRUCTION: 
        {
          // don't close text nodes
          closePending = false;

          // output copy of PI
          NodeInfo pi = tree.getNode(next);
          receiver.processingInstruction(pi.getLocalPart(),
                                         pi.getStringValue(),
                                         locationId,
                                         0);
          break;
        }
      }

      next++;
    } while (next < tree.numberOfNodes && tree.depth[next] > startLevel);

    // close all remaining elements
    if (closePending) {
      level++;
    }
    for (; level > startLevel; level--) {
      receiver.endElement();
    }
  }

  //    public void copyOLD(Receiver out, int whichNamespaces, boolean copyAnnotations) throws XPathException {
  //
  //        int nc = getNameCode();
  //        int typeCode = (copyAnnotations ? getTypeAnnotation() : 0);
  //        out.startElement(nc, typeCode, 0, 0);
  //
  //        // output the namespaces
  //
  //        if (whichNamespaces != NO_NAMESPACES) {
  //            outputNamespaceNodes(out, whichNamespaces==ALL_NAMESPACES);
  //        }
  //
  //        // output the attributes
  //
  //        int a = document.alpha[nodeNr];
  //        if (a >= 0) {
  //            while (a < document.numberOfAttributes && document.attParent[a] == nodeNr) {
  //            	document.getAttributeNode(a).copy(out, NO_NAMESPACES, copyAnnotations, locationId);
  //                a++;
  //            }
  //        }
  //
  //        // output the children
  //
  //        AxisIterator children =
  //            iterateAxis(Axis.CHILD, AnyNodeTest.getInstance());
  //
  //        int childNamespaces = (whichNamespaces==NO_NAMESPACES ? NO_NAMESPACES : LOCAL_NAMESPACES);
  //        while (true) {
  //            NodeInfo next = (NodeInfo)children.next();
  //            if (next==null) break;
  //            next.copy(out, childNamespaces, copyAnnotations, locationId);
  //        }
  //        out.endElement();
  //    }

  /**
   * Get the namespace URI corresponding to a given prefix. Return null
   * if the prefix is not in scope.
   *
   * @param prefix     the namespace prefix. May be the zero-length string, indicating
   *                   that there is no prefix. This indicates either the default namespace or the
   *                   null namespace, depending on the value of useDefault.
   * @param useDefault true if the default namespace is to be used when the
   *                   prefix is "". If false, the method returns "" when the prefix is "".
   * @return the uri for the namespace, or null if the prefix is not in scope.
   *         The "null namespace" is represented by the pseudo-URI "".
   */
  public String getURIForPrefix(String prefix, boolean useDefault) 
  {
    if (!useDefault && "".equals(prefix)) {
      return "";
    }
    int prefixCode = getNamePool().getCodeForPrefix(prefix);
    if (prefixCode == -1) {
      return null;
    }
    int ns = tree.beta[nodeNr]; // by convention
    if (ns > 0) {
      while (ns < tree.numberOfNamespaces &&
             tree.namespaceParent[ns] == nodeNr) 
      {
        int nscode = tree.namespaceCode[ns];
        if ((nscode >> 16) == prefixCode) 
        {
          int uriCode = nscode & 0xffff;
          if (uriCode == 0) 
          {
            // this is a namespace undeclaration, so the prefix is not in scope
            if (prefixCode == 0) 
            {
              // the namespace xmlns="" is always in scope
              return "";
            }
            else {
              return null;
            }
          }
          else {
            return getNamePool().getURIFromURICode((short)uriCode);
          }
        }
        ns++;
      }
    }

    // now search the namespaces defined on the ancestor nodes.
    NodeInfo parent = getParent();
    if (parent instanceof NamespaceResolver) {
      return ((NamespaceResolver)parent).getURIForPrefix(prefix, useDefault);
    }
    return null;
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
// The new copy() routine (in version 7.4.1) is contributed by Ruud Diterwich
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
