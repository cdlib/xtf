package org.cdlib.xtf.lazyTree.hackedSaxon;

import net.sf.saxon.Configuration;
import net.sf.saxon.event.ReceiverOptions;
import net.sf.saxon.om.*;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.tree.LineNumberMap;
import net.sf.saxon.tree.SystemIdMap;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.UntypedAtomicValue;
import java.util.ArrayList;

/**
 * A data structure to hold the contents of a tree. As the name implies, this implementation
 * of the data model is optimized for size, and for speed of creation: it minimizes the number
 * of Java objects used.
 *
 * <p>It can be used to represent a tree that is rooted at a document node, or one that is rooted
 * at an element node.</p>
 */
public final class TinyTree 
{
  // CDL-HACK: Changed some members to 'public' to provide easy access for
  // the XTF lazy tree builder.
  //
  private Configuration config;

  // List of top-level document nodes.
  private ArrayList documentList = new ArrayList(5);

  // The document number (really a tree number: it can identify a non-document root node
  protected int documentNumber;

  // the contents of the document
  protected char[] charBuffer;
  protected int charBufferLength = 0;
  protected FastStringBuffer commentBuffer = null; // created when needed
  public int numberOfNodes = 0; // excluding attributes and namespaces

  // The following arrays contain one entry for each node other than attribute
  // and namespace nodes, arranged in document order.

  // nodeKind indicates the kind of node, e.g. element, text, or comment
  public byte[] nodeKind;

  // depth is the depth of the node in the hierarchy, i.e. the number of ancestors
  protected short[] depth;

  // next is the node number of the next sibling
  // - unless it points backwards, in which case it is the node number of the parent
  public int[] next;

  // alpha holds a value that depends on the node kind. For text nodes, it is the offset
  // into the text buffer. For comments and processing instructions, it is the offset into
  // the comment buffer. For elements, it is the index of the first attribute node, or -1
  // if this element has no attributes.
  public int[] alpha;

  // beta holds a value that depends on the node kind. For text nodes, it is the length
  // of the text. For comments and processing instructions, it is the length of the text.
  // For elements, it is the index of the first namespace node, or -1
  // if this element has no namespaces.
  public int[] beta;

  // nameCode holds the name of the node, as an identifier resolved using the name pool
  public int[] nameCode;

  // the prior array indexes preceding-siblings; it is constructed only when required
  public int[] prior = null;

  // the typeCode array holds type codes for element nodes; it is constructed only
  // if at least one element has a type other than untyped (-1)
  int[] typeCodeArray = null;

  // the owner array gives fast access from a node to its parent; it is constructed
  // only when required
  // protected int[] parentIndex = null;

  // the following arrays have one entry for each attribute.
  public int numberOfAttributes = 0;

  // attParent is the index of the parent element node
  public int[] attParent;

  // attCode is the nameCode representing the attribute name
  public int[] attCode;

  // attValue is the string value of the attribute
  public CharSequence[] attValue;

  // attTypeCode holds type annotations. The array is created only if any nodes have a type annotation
  public int[] attTypeCode;

  // The following arrays have one entry for each namespace declaration
  public int numberOfNamespaces = 0;

  // namespaceParent is the index of the element node owning the namespace declaration
  public int[] namespaceParent;

  // namespaceCode is the namespace code used by the name pool: the top half is the prefix
  // code, the bottom half the URI code
  public int[] namespaceCode;
  private LineNumberMap lineNumberMap;
  private SystemIdMap systemIdMap = null;

  //private int IDtype;
  public TinyTree() {
    this(4000, 100, 20, 4000);
  }

  public TinyTree(int nodes, int attributes, int namespaces, int characters) 
  {
    nodeKind = new byte[nodes];
    depth = new short[nodes];
    next = new int[nodes];
    alpha = new int[nodes];
    beta = new int[nodes];
    nameCode = new int[nodes];

    numberOfAttributes = 0;
    attParent = new int[attributes];
    attCode = new int[attributes];
    attValue = new String[attributes];

    numberOfNamespaces = 0;
    namespaceParent = new int[namespaces];
    namespaceCode = new int[namespaces];

    charBuffer = new char[characters];
    charBufferLength = 0;
  }

  /**
  * Set the Configuration that contains this document
  */
  public void setConfiguration(Configuration config) 
  {
    this.config = config;
    NamePool pool = config.getNamePool();
    addNamespace(0, pool.getNamespaceCode("xml", NamespaceConstant.XML));

    //IDtype = pool.allocate("xs", NamespaceConstant.SCHEMA, "ID") & 0xfffff;
  }

  /**
   * Get the configuration previously set using setConfiguration
   */
  public Configuration getConfiguration() {
    return config;
  }

  /**
      * Get the name pool used for the names in this document
      */
  public NamePool getNamePool() {
    return config.getNamePool();
  }

  void ensureNodeCapacity() 
  {
    if (nodeKind.length < numberOfNodes + 1) 
    {
      //System.err.println("Number of nodes = " + numberOfNodes);
      int k = numberOfNodes * 2;

      byte[] nodeKind2 = new byte[k];
      int[] next2 = new int[k];
      short[] depth2 = new short[k];
      int[] alpha2 = new int[k];
      int[] beta2 = new int[k];
      int[] nameCode2 = new int[k];

      System.arraycopy(nodeKind, 0, nodeKind2, 0, numberOfNodes);
      System.arraycopy(next, 0, next2, 0, numberOfNodes);
      System.arraycopy(depth, 0, depth2, 0, numberOfNodes);
      System.arraycopy(alpha, 0, alpha2, 0, numberOfNodes);
      System.arraycopy(beta, 0, beta2, 0, numberOfNodes);
      System.arraycopy(nameCode, 0, nameCode2, 0, numberOfNodes);

      nodeKind = nodeKind2;
      next = next2;
      depth = depth2;
      alpha = alpha2;
      beta = beta2;
      nameCode = nameCode2;

      if (typeCodeArray != null) {
        int[] typeCodeArray2 = new int[k];
        System.arraycopy(typeCodeArray, 0, typeCodeArray2, 0, numberOfNodes);
        typeCodeArray = typeCodeArray2;
      }
    }
  }

  private void ensureAttributeCapacity() 
  {
    if (attParent.length < numberOfAttributes + 1) 
    {
      int k = numberOfAttributes * 2;
      if (k == 0) {
        k = 10;
      }

      int[] attParent2 = new int[k];
      int[] attCode2 = new int[k];
      String[] attValue2 = new String[k];

      System.arraycopy(attParent, 0, attParent2, 0, numberOfAttributes);
      System.arraycopy(attCode, 0, attCode2, 0, numberOfAttributes);
      System.arraycopy(attValue, 0, attValue2, 0, numberOfAttributes);

      attParent = attParent2;
      attCode = attCode2;
      attValue = attValue2;

      if (attTypeCode != null) {
        int[] attTypeCode2 = new int[k];
        System.arraycopy(attTypeCode, 0, attTypeCode2, 0, numberOfAttributes);
        attTypeCode = attTypeCode2;
      }
    }
  }

  private void ensureNamespaceCapacity() 
  {
    if (namespaceParent.length < numberOfNamespaces + 1) 
    {
      int k = numberOfNamespaces * 2;

      int[] namespaceParent2 = new int[k];
      int[] namespaceCode2 = new int[k];

      System.arraycopy(namespaceParent, 0, namespaceParent2, 0,
                       numberOfNamespaces);
      System.arraycopy(namespaceCode, 0, namespaceCode2, 0, numberOfNamespaces);

      namespaceParent = namespaceParent2;
      namespaceCode = namespaceCode2;
    }
  }

  /**
   * Add a document node to the tree. The data structure can contain any number of document (or element) nodes
   * as top-level nodes. The document node is retained in the documentList list, and its offset in that list
   * is held in the alpha array for the relevant node number.
   */
  void addDocumentNode(TinyDocumentImpl doc) {
    documentList.add(doc);
    addNode(Type.DOCUMENT, 0, documentList.size() - 1, 0, -1);
  }

  /**
   * Add a node to the tree
   * @param kind          The kind of the node
   * @param depth0        The depth in the tree
   * @param alpha0        Pointer to attributes or text
   * @param beta0         Pointer to namespaces or text
   * @param nameCode0     The name of the node
   * @return the node number of the node that was added
   */
  int addNode(short kind, int depth0, int alpha0, int beta0, int nameCode0) 
  {
    ensureNodeCapacity();
    nodeKind[numberOfNodes] = (byte)kind;
    depth[numberOfNodes] = (short)depth0;
    alpha[numberOfNodes] = alpha0;
    beta[numberOfNodes] = beta0;
    nameCode[numberOfNodes] = nameCode0;
    next[numberOfNodes] = -1; // safety precaution

    if (typeCodeArray != null) {
      typeCodeArray[numberOfNodes] = -1;
    }

    if (numberOfNodes == 0) {
      NodeInfo node = getNode(0);
      documentNumber = getNamePool().allocateDocumentNumber(node);
    }
    return numberOfNodes++;
  }

  void appendChars(CharSequence chars) 
  {
    if (charBuffer.length < charBufferLength + chars.length()) {
      char[] ch2 = new char[Math.max(charBuffer.length * 2,
                                     charBufferLength + chars.length() + 100)];
      System.arraycopy(charBuffer, 0, ch2, 0, charBufferLength);
      charBuffer = ch2;
    }
    if (chars instanceof CharSlice) 
    {
      ((CharSlice)chars).copyTo(charBuffer, charBufferLength);
    }
    else {
      char[] newchars = chars.toString().toCharArray();
      System.arraycopy(newchars, 0, charBuffer, charBufferLength, chars.length());

      // TODO: this is inefficient. There is no absolute reason that the
      // character data for the whole document needs to be in one array.
    }
    charBufferLength += chars.length();
  }

  /**
  * Condense the tree: release unused memory. This is done after the full tree has been built.
  * The method makes a pragmatic judgement as to whether it is worth reclaiming space; this is
  * only done when the constructed tree is very small compared with the space allocated.
  */
  public void condense() 
  {
    if (numberOfNodes * 3 < nodeKind.length ||
        (nodeKind.length - numberOfNodes > 20000) ||
        numberOfNodes == nodeKind.length) 
    {
      // the last condition actually expands the arrays to make room for the stopper
      int k = numberOfNodes + 1;

      byte[] nodeKind2 = new byte[k];
      int[] next2 = new int[k];
      short[] depth2 = new short[k];
      int[] alpha2 = new int[k];
      int[] beta2 = new int[k];
      int[] nameCode2 = new int[k];

      System.arraycopy(nodeKind, 0, nodeKind2, 0, numberOfNodes);
      System.arraycopy(next, 0, next2, 0, numberOfNodes);
      System.arraycopy(depth, 0, depth2, 0, numberOfNodes);
      System.arraycopy(alpha, 0, alpha2, 0, numberOfNodes);
      System.arraycopy(beta, 0, beta2, 0, numberOfNodes);
      System.arraycopy(nameCode, 0, nameCode2, 0, numberOfNodes);
      if (typeCodeArray != null) {
        int[] type2 = new int[k];
        System.arraycopy(typeCodeArray, 0, type2, 0, numberOfNodes);
        typeCodeArray = type2;
      }

      nodeKind = nodeKind2;
      next = next2;
      depth = depth2;
      alpha = alpha2;
      beta = beta2;
      nameCode = nameCode2;
    }
    nodeKind[numberOfNodes] = Type.STOPPER;
    depth[numberOfNodes] = 0;

    if ((numberOfAttributes * 3 < attParent.length) ||
        (attParent.length - numberOfAttributes > 1000)) 
    {
      int k = numberOfAttributes;

      int[] attParent2 = new int[k];
      int[] attCode2 = new int[k];
      String[] attValue2 = new String[k];

      System.arraycopy(attParent, 0, attParent2, 0, numberOfAttributes);
      System.arraycopy(attCode, 0, attCode2, 0, numberOfAttributes);
      System.arraycopy(attValue, 0, attValue2, 0, numberOfAttributes);

      attParent = attParent2;
      attCode = attCode2;
      attValue = attValue2;

      if (attTypeCode != null) {
        int[] attTypeCode2 = new int[k];
        System.arraycopy(attTypeCode, 0, attTypeCode2, 0, numberOfAttributes);
        attTypeCode = attTypeCode2;
      }
    }

    if (numberOfNamespaces * 3 < namespaceParent.length) 
    {
      int k = numberOfNamespaces;
      int[] namespaceParent2 = new int[k];
      int[] namespaceCode2 = new int[k];

      System.arraycopy(namespaceParent, 0, namespaceParent2, 0,
                       numberOfNamespaces);
      System.arraycopy(namespaceCode, 0, namespaceCode2, 0, numberOfNamespaces);

      namespaceParent = namespaceParent2;
      namespaceCode = namespaceCode2;
    }

    if (charBufferLength * 3 < charBuffer.length ||
        charBuffer.length - charBufferLength > 10000) 
    {
      char[] c2 = new char[charBufferLength];
      System.arraycopy(charBuffer, 0, c2, 0, charBufferLength);
      charBuffer = c2;
    }
  }

  /**
  * Set the type annotation of an element node
  */
  void setElementAnnotation(int nodeNr, int typeCode) 
  {
    if (typeCodeArray == null) 
    {
      typeCodeArray = new int[nodeKind.length];
      for (int i = 0; i < nodeKind.length; i++) {
        typeCodeArray[i] = -1;
      }
    }
    typeCodeArray[nodeNr] = typeCode;
  }

  /**
  * Get the type annotation of an element node.
   * Type.UNTYPED if there is no type annotation
  */
  int getElementAnnotation(int nodeNr) 
  {
    if (typeCodeArray == null) {
      return -1;
    }
    return typeCodeArray[nodeNr];
  }

  /**
  * On demand, make an index for quick access to preceding-sibling nodes
  */
  public void ensurePriorIndex() 
  {
    if (prior == null) {
      makePriorIndex();
    }
  }

  private synchronized void makePriorIndex() 
  {
    prior = new int[numberOfNodes];
    for (int i = 0; i < numberOfNodes; i++) {
      prior[i] = -1;
    }
    for (int i = 0; i < numberOfNodes; i++) 
    {
      int nextNode = next[i];
      if (nextNode > i) {
        prior[nextNode] = i;
      }
    }
  }

  void addAttribute(NodeInfo root, int parent, int nameCode, int typeCode,
                    CharSequence attValue, int properties) 
  {
    ensureAttributeCapacity();
    attParent[numberOfAttributes] = parent;
    attCode[numberOfAttributes] = nameCode;
    this.attValue[numberOfAttributes] = attValue;

    if (typeCode != -1) 
    {
      if (attTypeCode == null) 
      {
        attTypeCode = new int[attParent.length];
        for (int i = 0; i < numberOfAttributes; i++) {
          attTypeCode[i] = -1;
        }
      }
    }
    if (attTypeCode != null) {
      attTypeCode[numberOfAttributes] = typeCode;
    }

    if (alpha[parent] == -1) {
      alpha[parent] = numberOfAttributes;
    }

    if (root instanceof TinyDocumentImpl &&
        (typeCode == StandardNames.XS_ID ||
         ((properties & ReceiverOptions.DTD_ID_ATTRIBUTE) != 0) ||
         ((nameCode & NamePool.FP_MASK) == StandardNames.XML_ID))) 
    {
      // TODO: what about subtypes of ID?

      // The attribute is marked as being an ID. But we don't trust it - it
      // might come from a non-validating parser. Before adding it to the index, we
      // check that it really is an ID.
      String id = attValue.toString().trim();
      if (XMLChar.isValidNCName(id)) {
        NodeInfo e = getNode(parent);
        ((TinyDocumentImpl)root).registerID(e, id);
      }
      else if (attTypeCode != null) {
        attTypeCode[numberOfAttributes] = Type.UNTYPED_ATOMIC;
      }
    }

    numberOfAttributes++;
  }

  /**
   * Add a namespace node to the current element
   * @param parent the node number of the element
   * @param nscode namespace code identifying the prefix and uri
   */
  void addNamespace(int parent, int nscode) 
  {
    ensureNamespaceCapacity();
    namespaceParent[numberOfNamespaces] = parent;
    namespaceCode[numberOfNamespaces] = nscode;

    if (beta[parent] == -1) {
      beta[parent] = numberOfNamespaces;
    }
    numberOfNamespaces++;
  }

  @SuppressWarnings("cast")
  public TinyNodeImpl getNode(int nr) {
    switch ((short)nodeKind[nr]) {
      case Type.DOCUMENT:
        return (TinyDocumentImpl)documentList.get(alpha[nr]);
      case Type.ELEMENT:
        return new TinyElementImpl(this, nr);
      case Type.TEXT:
        return new TinyTextImpl(this, nr);
      case Type.COMMENT:
        return new TinyCommentImpl(this, nr);
      case Type.PROCESSING_INSTRUCTION:
        return new TinyProcInstImpl(this, nr);
    }

    return null;
  }

  /**
   * Get the typed value of a node whose type is known to be untypedAtomic.
   * The node must be a document, element, text,
   * comment, or processing-instruction node, and it must have no type annotation.
   * This method gets the typed value
   * of a numbered node without actually instantiating the NodeInfo object, as
   * a performance optimization.
   */
  UntypedAtomicValue getUntypedAtomicValue(int nodeNr) 
  {
    switch (nodeKind[nodeNr]) 
    {
      case Type.ELEMENT:
      case Type.DOCUMENT:
        int level = depth[nodeNr];
        int next = nodeNr + 1;

        // we optimize two special cases: firstly, where the node has no children, and secondly,
        // where it has a single text node as a child.
        if (depth[next] <= level) {
          return UntypedAtomicValue.ZERO_LENGTH_UNTYPED;
        }
        else if (nodeKind[next] == Type.TEXT && depth[next + 1] <= level) {
          int length = beta[next];
          int start = alpha[next];
          return new UntypedAtomicValue(new CharSlice(charBuffer, start, length));
        }

        // Now handle the general case
        StringBuffer sb = null;
        while (next < numberOfNodes && depth[next] > level) 
        {
          if (nodeKind[next] == Type.TEXT) 
          {
            if (sb == null) {
              sb = new StringBuffer(1024);
            }
            int length = beta[next];
            int start = alpha[next];
            sb.append(charBuffer, start, length);
          }
          next++;
        }
        if (sb == null) 
        {
          return UntypedAtomicValue.ZERO_LENGTH_UNTYPED;
        }
        else {
          //sb.trimToSize();  // TODO: reinstate this in JDK 1.5
          char[] buff = new char[sb.length()];
          sb.getChars(0, sb.length(), buff, 0);
          return new UntypedAtomicValue(new CharSlice(buff));
        }
      case Type.TEXT:
        int start = alpha[nodeNr];
        int len = beta[nodeNr];
        return new UntypedAtomicValue(new CharSlice(charBuffer, start, len));
      case Type.COMMENT:
      case Type.PROCESSING_INSTRUCTION:
        int start2 = alpha[nodeNr];
        int len2 = beta[nodeNr];
        if (len2 == 0)
          return UntypedAtomicValue.ZERO_LENGTH_UNTYPED;
        char[] dest = new char[len2];
        commentBuffer.getChars(start2, start2 + len2, dest, 0);
        return new UntypedAtomicValue(new CharSlice(dest, 0, len2));
      default:
        throw new IllegalStateException("Unknown node kind");
    }
  }

  /**
  * Make a (transient) attribute node from the array of attributes
  */
  TinyAttributeImpl getAttributeNode(int nr) {
    return new TinyAttributeImpl(this, nr);
  }

  /**
  * Get the type annotation of an attribute node.
   * @return Type.UNTYPED_ATOMIC if there is no annotation
  */
  int getAttributeAnnotation(int nr) 
  {
    if (attTypeCode == null) {
      return -1;
    }
    else {
      return attTypeCode[nr];
    }
  }

  /**
  * Set the system id of an element in the document. This identifies the external entity containing
   * the node - this is not necessarily the same as the base URI.
   * @param seq the node number
   * @param uri the system ID
  */
  void setSystemId(int seq, String uri) 
  {
    if (uri == null) {
      uri = "";
    }
    if (systemIdMap == null) {
      systemIdMap = new SystemIdMap();
    }
    systemIdMap.setSystemId(seq, uri);
  }

  /**
  * Get the system id of an element in the document
  */
  String getSystemId(int seq) 
  {
    if (systemIdMap == null) {
      return null;
    }
    return systemIdMap.getSystemId(seq);
  }

  /**
  * Set line numbering on
  */
  public void setLineNumbering() {
    lineNumberMap = new LineNumberMap();
    lineNumberMap.setLineNumber(0, 0);
  }

  /**
  * Set the line number for an element. Ignored if line numbering is off.
  */
  void setLineNumber(int sequence, int line) 
  {
    if (lineNumberMap != null) {
      lineNumberMap.setLineNumber(sequence, line);
    }
  }

  /**
  * Get the line number for an element. Return -1 if line numbering is off.
  */
  int getLineNumber(int sequence) 
  {
    if (lineNumberMap != null) {
      return lineNumberMap.getLineNumber(sequence);
    }
    return -1;
  }

  /**
   * Get the document number (actually, the tree number)
   */
  public int getDocumentNumber() {
    return documentNumber;
  }

  /**
  * Produce diagnostic print of main tree arrays
  */
  public void diagnosticDump() 
  {
    System.err.println("    node    type   depth    next   alpha    beta    name");
    for (int i = 0; i < numberOfNodes; i++) {
      System.err.println(
        n8(i) + n8(nodeKind[i]) + n8(depth[i]) + n8(next[i]) + n8(alpha[i]) +
        n8(beta[i]) + n8(nameCode[i]));
    }
    System.err.println("    attr  parent    name    value");
    for (int i = 0; i < numberOfAttributes; i++) {
      System.err.println(
        n8(i) + n8(attParent[i]) + n8(attCode[i]) + "    " + attValue[i]);
    }
    System.err.println("      ns  parent  prefix     uri");
    for (int i = 0; i < numberOfNamespaces; i++) {
      System.err.println(
        n8(i) + n8(namespaceParent[i]) + n8(namespaceCode[i] >> 16) +
        n8(namespaceCode[i] & 0xffff));
    }
  }

  /**
  * Output a number as a string of 8 characters
  */
  private String n8(int val) {
    String s = "        " + val;
    return s.substring(s.length() - 8);
  }

  public void showSize() {
    System.err.println(
      "Tree size: " + numberOfNodes + " nodes, " + charBufferLength +
      " characters, " + numberOfAttributes + " attributes");
  }

  /**
   * Get the number of nodes in the tree, excluding attributes and namespace nodes
   * @return the number of nodes.
   */
  public int getNumberOfNodes() {
    return numberOfNodes;
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
// The Original Code is: all this file 
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//
