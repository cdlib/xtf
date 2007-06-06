package net.sf.saxon.tinytree;

/** 
 * Hacked version of the TinyBuilder class. Changes include:
 * (1) Made some members public so they can be read to write a lazy tree file.
 * (2) Added straight-to-disk text storage.
 * (3) Changed to filter out processing instructions and comments.
 */

import java.io.IOException;
import org.cdlib.xtf.util.PackedByteBuf;
import org.cdlib.xtf.util.StructuredStore;
import org.cdlib.xtf.util.SubStoreWriter;
import net.sf.saxon.event.Builder;
import net.sf.saxon.event.LocationProvider;
import net.sf.saxon.event.ReceiverOptions;
import net.sf.saxon.event.SourceLocationProvider;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.tinytree.TinyDocumentImpl;
import net.sf.saxon.tinytree.TinyTree;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;

public class HackedTinyBuilder extends Builder 
{
  // MCH: Added stuff for straight-to-disk text storage.
  private PackedByteBuf textBuf = new PackedByteBuf(1000);
  private StructuredStore treeStore;
  private SubStoreWriter textStore;

  public void setTreeStore(StructuredStore treeStore) {
    this.treeStore = treeStore;
  }

  public StructuredStore getTreeStore() {
    return treeStore;
  }

  public void setTextStore(SubStoreWriter textStore) {
    this.textStore = textStore;
  }

  public SubStoreWriter getTextStore() {
    return textStore;
  }

  // MCH: Get rid of parent pointers, which mess up our node counts.
  public static final int PARENT_POINTER_INTERVAL = Integer.MAX_VALUE;

  // a lower value allocates more parent pointers which takes more space but reduces
  // the length of parent searches
  private TinyTree tree;
  private int currentDepth = 0;
  private int nodeNr = 0; // this is the local sequence within this document
  private boolean ended = false;
  private int[] sizeParameters; // estimate of number of nodes, attributes, namespaces, characters

  public HackedTinyBuilder() {
  }

  public void setSizeParameters(int[] params) {
    sizeParameters = params;
  }

  public int[] getSizeParameters() 
  {
    int[] params = {
                     tree.getNumberOfNodes(), tree.getNumberOfAttributes(),
                     tree.getNumberOfNamespaces(),
                     tree.getCharacterBuffer().length()
                   };
    return params;
  }

  private int[] prevAtDepth = new int[100];

  // this array is scaffolding used while constructing the tree, it is
  // not present in the final tree. For each level of the tree, it records the
  // node number of the most recent node at that level.
  private int[] siblingsAtDepth = new int[100];

  // more scaffolding. For each level of the tree, this array records the
  // number of siblings processed at that level. When this exceeds a threshold value,
  // a dummy node is inserted into the arrays to contain a parent pointer: this it to
  // prevent excessively long searches for a parent node, which is normally found by
  // scanning the siblings.
  private boolean isIDElement = false;

  public TinyTree getTree() {
    return tree;
  }

  /**
   * Open the event stream
   */
  public void open()
    throws XPathException 
  {
    if (started) 
    {
      // this happens when using an IdentityTransformer
      return;
    }
    if (tree == null) 
    {
      if (sizeParameters == null) {
        tree = new TinyTree();
      }
      else {
        tree = new TinyTree(sizeParameters[0],
                            sizeParameters[1],
                            sizeParameters[2],
                            sizeParameters[3]);
      }
      tree.setConfiguration(config);
      currentDepth = 0;
      if (lineNumbering) {
        tree.setLineNumbering();
      }
    }
    super.open();
  }

  /**
  * Write a document node to the tree
  */
  public void startDocument(int properties)
    throws XPathException 
  {
    //        if (currentDepth == 0 && tree.numberOfNodes != 0) {
    //            System.err.println("**** FOREST DOCUMENT ****");
    //        }
    if ((started && !ended) || currentDepth > 0) 
    {
      // this happens when using an IdentityTransformer, or when copying a document node to form
      // the content of an element
      return;
    }
    started = true;
    ended = false;

    currentRoot = new TinyDocumentImpl(tree);
    TinyDocumentImpl doc = (TinyDocumentImpl)currentRoot;
    doc.setSystemId(getSystemId());
    doc.setBaseURI(getBaseURI());
    doc.setConfiguration(config);

    currentDepth = 0;
    tree.addDocumentNode((TinyDocumentImpl)currentRoot);
    prevAtDepth[0] = 0;
    prevAtDepth[1] = -1;
    siblingsAtDepth[0] = 0;
    siblingsAtDepth[1] = 0;
    tree.next[0] = -1;

    currentDepth++;

    super.startDocument(0);
  }

  /**
  * Callback interface for SAX: not for application use
  */
  public void endDocument()
    throws XPathException 
  {
    // System.err.println("TinyBuilder: " + this + " End document");
    if (currentDepth > 1)
      return;

    // happens when copying a document node as the child of an element
    if (ended)
      return; // happens when using an IdentityTransformer
    ended = true;

    prevAtDepth[currentDepth] = -1;
    currentDepth--;
  }

  public void close()
    throws XPathException 
  {
    //System.err.println("Tree.close " + tree + " size=" + tree.numberOfNodes);
    tree.addNode(Type.STOPPER, 0, 0, 0, -1);
    tree.condense();
    super.close();
  }

  /**
  * Notify the start tag of an element
  */
  public void startElement(int nameCode, int typeCode, int locationId,
                           int properties)
    throws XPathException 
  {
    //        if (currentDepth == 0 && tree.numberOfNodes != 0) {
    //            System.err.println("**** FOREST ELEMENT **** trees=" + tree.rootIndexUsed );
    //        }

    // if the number of siblings exceeds a certain threshold, add a parent pointer, in the form
    // of a pseudo-node
    if (siblingsAtDepth[currentDepth] > PARENT_POINTER_INTERVAL) 
    {
      nodeNr = tree.addNode(Type.PARENT_POINTER,
                            currentDepth,
                            prevAtDepth[currentDepth - 1],
                            0,
                            0);
      int prev = prevAtDepth[currentDepth];
      if (prev > 0) {
        tree.next[prev] = nodeNr;
      }
      tree.next[nodeNr] = prevAtDepth[currentDepth - 1];
      prevAtDepth[currentDepth] = nodeNr;
      siblingsAtDepth[currentDepth] = 0;
    }

    // now add the element node itself
    nodeNr = tree.addNode(Type.ELEMENT, currentDepth, -1, -1, nameCode);

    isIDElement = ((properties & ReceiverOptions.IS_ID) != 0);
    if (typeCode != StandardNames.XS_UNTYPED && typeCode != -1) 
    {
      tree.setElementAnnotation(nodeNr, typeCode);
      if (!isIDElement && config.getTypeHierarchy().isIdCode(typeCode)) {
        isIDElement = true;
      }
    }

    if (currentDepth == 0) 
    {
      prevAtDepth[0] = nodeNr;
      prevAtDepth[1] = -1;

      //tree.next[0] = -1;
      currentRoot = tree.getNode(nodeNr);
    }
    else {
      int prev = prevAtDepth[currentDepth];
      if (prev > 0) {
        tree.next[prev] = nodeNr;
      }
      tree.next[nodeNr] = prevAtDepth[currentDepth - 1]; // *O* owner pointer in last sibling
      prevAtDepth[currentDepth] = nodeNr;
      siblingsAtDepth[currentDepth]++;
    }
    currentDepth++;

    if (currentDepth == prevAtDepth.length) {
      int[] p2 = new int[currentDepth * 2];
      System.arraycopy(prevAtDepth, 0, p2, 0, currentDepth);
      prevAtDepth = p2;
      p2 = new int[currentDepth * 2];
      System.arraycopy(siblingsAtDepth, 0, p2, 0, currentDepth);
      siblingsAtDepth = p2;
    }
    prevAtDepth[currentDepth] = -1;
    siblingsAtDepth[currentDepth] = 0;

    LocationProvider locator = pipe.getLocationProvider();
    if (locator instanceof SourceLocationProvider) 
    {
      tree.setSystemId(nodeNr, locator.getSystemId(locationId));
      if (lineNumbering) {
        tree.setLineNumber(nodeNr, locator.getLineNumber(locationId));
      }
    }
    else if (currentDepth == 1) {
      tree.setSystemId(nodeNr, systemId);
    }
  }

  public void namespace(int namespaceCode, int properties)
    throws XPathException 
  {
    tree.addNamespace(nodeNr, namespaceCode);
  }

  public void attribute(int nameCode, int typeCode, CharSequence value,
                        int locationId, int properties)
    throws XPathException 
  {
    // System.err.println("attribute " + nameCode + "=" + value);
    tree.addAttribute(currentRoot, nodeNr, nameCode, typeCode, value, properties);
  }

  public void startContent() {
    nodeNr++;
  }

  /**
  * Callback interface for SAX: not for application use
  */
  public void endElement()
    throws XPathException 
  {
    prevAtDepth[currentDepth] = -1;
    siblingsAtDepth[currentDepth] = 0;
    currentDepth--;
    if (isIDElement) 
    {
      // we're relying on the fact that an ID element has no element children!
      tree.indexIDElement(currentRoot,
                          prevAtDepth[currentDepth],
                          config.getNameChecker());
      isIDElement = false;
    }
  }

  /**
  * Callback interface for SAX: not for application use
  */
  public void characters(CharSequence chars, int locationId, int properties)
    throws XPathException 
  {
    // MCH: Added straight-to-disk storage of text
    final int len = chars.length();
    if (len > 0) 
    {
      long startPos;
      textBuf.reset();
      textBuf.writeCharSequence(chars);
      try {
        startPos = textStore.length();
        textBuf.output(textStore);
      }
      catch (IOException e) {
        throw new DynamicError(e);
      }

      nodeNr = tree.addNode(Type.TEXT,
                            currentDepth,
                            (int)startPos,
                            textBuf.length(),
                            -1);

      int prev = prevAtDepth[currentDepth];
      if (prev > 0) {
        tree.next[prev] = nodeNr;
      }
      tree.next[nodeNr] = prevAtDepth[currentDepth - 1]; // *O* owner pointer in last sibling
      prevAtDepth[currentDepth] = nodeNr;
      siblingsAtDepth[currentDepth]++;
    }
  }

  /**
  * Callback interface for SAX: not for application use<BR>
  */
  public void processingInstruction(String piname, CharSequence remainder,
                                    int locationId, int properties)
    throws XPathException 
  {
    // MCH: Filter out processing instructions
  }

  /**
  * Callback interface for SAX: not for application use
  */
  public void comment(CharSequence chars, int locationId, int properties)
    throws XPathException 
  {
    // MCH: Filter out comments
  }

  /**
  * Set an unparsed entity in the document
  */
  public void setUnparsedEntity(String name, String uri, String publicId) {
    ((TinyDocumentImpl)currentRoot).setUnparsedEntity(name, uri, publicId);
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
// Portions created by Martin Haye, marked by "MCH:", are Copyright (C) 2005, Regents of the University of California. All Rights Reserved.
//
// Contributor(s): Martin Haye.
//
