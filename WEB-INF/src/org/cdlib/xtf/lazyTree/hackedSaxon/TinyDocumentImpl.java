package org.cdlib.xtf.lazyTree.hackedSaxon;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.ReceiverOptions;
import net.sf.saxon.om.*;
import net.sf.saxon.tree.LineNumberMap;
import net.sf.saxon.tree.SystemIdMap;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.UntypedAtomicValue;
import org.w3c.dom.Document;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

import net.sf.saxon.xpath.XPathException;
import net.sf.saxon.Configuration;

import java.util.ArrayList;
import java.util.HashMap;


/**
  * A node in the XML parse tree representing the Document itself (or equivalently, the root
  * node of the Document).<P>
  * @author Michael H. Kay
  * @version 26 April 1999
  */

public final class TinyDocumentImpl extends TinyParentNodeImpl
    implements DocumentInfo, Document {

    // CDL-HACK: All private and protected members changed to public, so that
    //           the contents of this structure can be re-used.
    //
    public HashMap idTable = null;
    public Configuration config;
    public NamePool namePool;
    public int documentNumber;
    public HashMap elementList = null;
    public boolean usesNamespaces = false;
    public HashMap entityTable = null;
    //private HashMap elementTypeMap = null;

    // This structure supports trees whose root is an element node rather than a document node.
    // The document node still exists, for implementation reasons, but it is not regarded as
    // part of the tree. The variable rootNode identifies the actual root of the tree, which
    // is the document node by default.

    public int rootNode = 0;

    // the contents of the document

    public char[] charBuffer;
    public int charBufferLength = 0;
    public StringBuffer commentBuffer = null; // created when needed

    public int numberOfNodes = 0;    // excluding attributes and namespaces

    // The following arrays contain one entry for each node other than attribute
    // and namespace nodes, arranged in document order.

    // nodeKind indicates the kind of node, e.g. element, text, or comment
    public byte[] nodeKind;

    // depth is the depth of the node in the hierarchy, i.e. the number of ancestors
    public short[] depth;

    // next is the node number of the next sibling
    // *O* unless it points backwards, in which case it is the node number of the parent
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

    // nameCode holds the name of the node, as an index into the name pool
    public int[] nameCode;

    // the prior array indexes preceding-siblings; it is constructed only when required
    public int[] prior = null;

    // the typeCode array holds type codes for element nodes; it is constructed only
    // if at least one element has a type other than untyped (-1)
    private int[] typeCodeArray = null;

    // the owner array gives fast access from a node to its parent; it is constructed
    // only when required
    // public int[] parentIndex = null;

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

    public LineNumberMap lineNumberMap;
    public SystemIdMap systemIdMap = null;

    private int IDtype;

    public TinyDocumentImpl() {
        this(4000, 100, 20, 4000);
    }

    public TinyDocumentImpl(int nodes, int attributes, int namespaces, int characters) {
        nodeNr = 0;
        document = this;
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

    public void setConfiguration(Configuration config) {
        this.config = config;
        NamePool pool = config.getNamePool();
		addNamespace(0, pool.getNamespaceCode("xml", NamespaceConstant.XML));
		documentNumber = pool.allocateDocumentNumber(this);
        IDtype = pool.allocate("xs", NamespaceConstant.SCHEMA, "ID") & 0xfffff;
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

	/**
	* Get the unique document number
	*/

	public int getDocumentNumber() {
	    return documentNumber;
	}


    void ensureNodeCapacity() {
        if (nodeKind.length < numberOfNodes+1) {
            //System.err.println("Number of nodes = " + numberOfNodes);
            int k = numberOfNodes*2;

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

    private void ensureAttributeCapacity() {
        if (attParent.length < numberOfAttributes+1) {
            int k = numberOfAttributes*2;
            if (k==0) {
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

    private void ensureNamespaceCapacity() {
        if (namespaceParent.length < numberOfNamespaces+1) {
            int k = numberOfNamespaces*2;

            int[] namespaceParent2 = new int[k];
            int[] namespaceCode2 = new int[k];

            System.arraycopy(namespaceParent, 0, namespaceParent2, 0, numberOfNamespaces);
            System.arraycopy(namespaceCode, 0, namespaceCode2, 0, numberOfNamespaces);

            namespaceParent = namespaceParent2;
            namespaceCode = namespaceCode2;
        }
    }

    /**
    * Set the root node. Parentless elements are implemented using a full tree structure
    * containing a document node, but the document node is not regarded as part of the tree
    */

    public void setRootNode(NodeInfo root) {
        rootNode = ((TinyNodeImpl)root).nodeNr;
    }

    /**
     * Add a node to the document
     * @param kind         The kind of the node
     * @param depth0        The depth in the tree
     * @param alpha0        Pointer to attributes or text
     * @param beta0         Pointer to namespaces or text
     * @param nameCode0     The name of the node
     * @return the node number of the node that was added
     */
    int addNode(short kind, int depth0, int alpha0, int beta0, int nameCode0) {
        ensureNodeCapacity();
        nodeKind[numberOfNodes] = (byte)kind;
        depth[numberOfNodes] = (short)depth0;
        alpha[numberOfNodes] = alpha0;
        beta[numberOfNodes] = beta0;
        nameCode[numberOfNodes] = nameCode0;
        next[numberOfNodes] = -1;      // safety precaution, esp for preview mode

        if (typeCodeArray != null) {
            typeCodeArray[numberOfNodes] = -1;
        }
//        if (depth0 == 1) lastLevelOneNode = numberOfNodes;

        return numberOfNodes++;
    }

    void appendChars(CharSequence chars) {
        while (charBuffer.length < charBufferLength + chars.length()) {
            char[] ch2 = new char[charBuffer.length * 2];
            System.arraycopy(charBuffer, 0, ch2, 0, charBufferLength);
            charBuffer = ch2;
        }
        if (chars instanceof CharSlice) {
            ((CharSlice)chars).copyTo(charBuffer, charBufferLength);
        } else {
            char[] newchars = chars.toString().toCharArray();
            System.arraycopy(newchars, 0, charBuffer, charBufferLength, chars.length());
            // SaxonTODO: this is inefficient. There is no absolute reason that the
            // character data for the whole document needs to be in one array.
        }
        charBufferLength += chars.length();
    }

    /**
    * Condense the tree: release unused memory. This is done after the full tree has been built.
    * The method makes a pragmatic judgement as to whether it is worth reclaiming space; this is
    * only done when the constructed tree is very small compared with the space allocated.
    */

    public void condense() {
        if (numberOfNodes * 3 < nodeKind.length ||
                (nodeKind.length - numberOfNodes > 20000)) {
            int k = numberOfNodes;

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
        }

        if ((numberOfAttributes * 3 < attParent.length) ||
                (attParent.length - numberOfAttributes > 1000)) {
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

        if (numberOfNamespaces * 3 < namespaceParent.length) {
            int k = numberOfNamespaces;
            int[] namespaceParent2 = new int[k];
            int[] namespaceCode2 = new int[k];

            System.arraycopy(namespaceParent, 0, namespaceParent2, 0, numberOfNamespaces);
            System.arraycopy(namespaceCode, 0, namespaceCode2, 0, numberOfNamespaces);

            namespaceParent = namespaceParent2;
            namespaceCode = namespaceCode2;
        }

        if (charBufferLength * 3 < charBuffer.length ||
                charBuffer.length - charBufferLength > 10000) {
            char[] c2 = new char[charBufferLength];
            System.arraycopy(charBuffer,  0, c2, 0, charBufferLength);
            charBuffer = c2;
        }
    }

    /**
    * Set the type annotation of an element node
    */

    void setElementAnnotation(int nodeNr, int typeCode) {
        // The implementation here is not very economical. It is optimized for the case
        // where annotating an element with a type is unusual.
        if (typeCodeArray == null) {
            typeCodeArray = new int[nodeKind.length];
            for (int i=0; i<nodeKind.length; i++) {
                typeCodeArray[i] = -1;
            }
        }
        typeCodeArray[nodeNr] = typeCode;
    }

    /**
    * Get the type annotation of an element node.
     * Type.UNTYPED if there is no type annotation
    */

    int getElementAnnotation(int nodeNr) {
        if (typeCodeArray == null) {
            return -1;
        }
        return typeCodeArray[nodeNr];
    }

    /**
    * On demand, make an index for quick access to preceding-sibling nodes
    */

    public void ensurePriorIndex() {
        if (prior==null) {
            makePriorIndex();
        }
    }

    private synchronized void makePriorIndex() {
        prior = new int[numberOfNodes];
        for (int i=0; i<numberOfNodes; i++) {
            prior[i] = -1;
        }
        for (int i=0; i<numberOfNodes; i++) {
            int nextNode = next[i];
            //if (nextNode!=-1) {
            if (nextNode > i) {       // *O*
                prior[nextNode] = i;
            }
        }
    }


    void addAttribute(int parent0, int code0, int type0, CharSequence value0, int properties) {
        // System.err.println("addAttribute(" + parent0 + "," + code0 + "," + type0 + "," + value0 + ")");
        ensureAttributeCapacity();
        attParent[numberOfAttributes] = parent0;
        attCode[numberOfAttributes] = code0;
        attValue[numberOfAttributes] = value0;

        if (type0 != -1) {
            if (attTypeCode==null) {
                attTypeCode = new int[attParent.length];
                for (int i=0; i<numberOfAttributes; i++) {
                    attTypeCode[i] = -1;
                }
            }
        }
        if (attTypeCode != null) {
            attTypeCode[numberOfAttributes] = type0;
        }

        if (alpha[parent0] == -1) {
            alpha[parent0] = numberOfAttributes;
        }

        if (type0 == IDtype || (properties & ReceiverOptions.DTD_ID_ATTRIBUTE) != 0) {
            // TODO: what about subtypes of ID?

            // The attribute is marked as being an ID. But we don't trust it - it
            // might come from a non-validating parser. Before adding it to the index, we
            // check that it really is an ID.

            if (XMLChar.isValidNCName(value0.toString())) {
            	if (idTable==null) {
            		idTable = new HashMap(256);
            	}
    			NodeInfo e = getNode(parent0);
                registerID(e, value0.toString());
            } else if (attTypeCode != null) {
                attTypeCode[numberOfAttributes] = Type.UNTYPED_ATOMIC;
            }
        }

        numberOfAttributes++;
    }

    void addNamespace(int parent, int nscode ) {
        usesNamespaces = true;
        ensureNamespaceCapacity();
        namespaceParent[numberOfNamespaces] = parent;
        namespaceCode[numberOfNamespaces] = nscode;

        if (beta[parent] == -1) {
            beta[parent] = numberOfNamespaces;
        }
        numberOfNamespaces++;
    }

    public TinyNodeImpl getNode(int nr) {

        switch ((short)nodeKind[nr]) {
            case Type.DOCUMENT:
                return this;
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

    UntypedAtomicValue getUntypedAtomicValue(int nodeNr) {
        switch (nodeKind[nodeNr]) {
            case Type.ELEMENT:
            case Type.DOCUMENT:
                int level = depth[nodeNr];
                StringBuffer sb = null;
                int next = nodeNr+1;
                while (next < numberOfNodes && depth[next] > level) {
                    if (nodeKind[next]==Type.TEXT) {
                        if (sb==null) {
                            sb = new StringBuffer(1024);
                        }
                        int length = beta[next];
                        int start = alpha[next];
                        sb.append(charBuffer, start, length);
                    }
                    next++;
                }
                if (sb==null) return UntypedAtomicValue.ZERO_LENGTH_UNTYPED;
                return new UntypedAtomicValue(sb);
            case Type.TEXT:
                int start = alpha[nodeNr];
                int len = beta[nodeNr];
                return new UntypedAtomicValue(
                        new String(charBuffer, start, len));
            case Type.COMMENT:
            case Type.PROCESSING_INSTRUCTION:
                int start2 = alpha[nodeNr];
                int len2 = beta[nodeNr];
                if (len2==0) return UntypedAtomicValue.ZERO_LENGTH_UNTYPED;
                char[] dest = new char[len2];
                commentBuffer.getChars(start2, start2+len2, dest, 0);
                return new UntypedAtomicValue(new String(dest, 0, len2));
            default:
                throw new IllegalStateException("Unknown node kind");
        }
    }

    /**
    * Get the node sequence number (in document order). Sequence numbers are monotonic but not
    * consecutive.
    */

    public long getSequenceNumber() {
        return 0;
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

    int getAttributeAnnotation(int nr) {
        if (attTypeCode == null) {
            return -1;
        } else {
            return attTypeCode[nr];
        }
    }

    /**
    * determine whether this document uses namespaces
    */

    boolean isUsingNamespaces() {
        return usesNamespaces;
    }

    /**
    * Make a (transient) namespace node from the array of namespace declarations
    */


    TinyNamespaceImpl getNamespaceNode(int nr) {
        return new TinyNamespaceImpl(this, nr);
    }

    /**
    * Set the system id of this node
    */

    public void setSystemId(String uri) {
        if (uri==null) {
            uri = "";
        }
        if (systemIdMap==null) {
            systemIdMap = new SystemIdMap();
        }
        systemIdMap.setSystemId(nodeNr, uri);
    }

    /**
    * Get the system id of this root node
    */

    public String getSystemId() {
        if (systemIdMap==null) {
            return null;
        }
        return systemIdMap.getSystemId(nodeNr);
    }

    /**
    * Get the base URI of this root node. For a root node the base URI is the same as the
    * System ID.
    */

    public String getBaseURI() {
        return getSystemId();
    }

    /**
    * Set the system id of an element in the document
    */

    void setSystemId(int seq, String uri) {
        if (uri==null) {
            uri = "";
        }
        if (systemIdMap==null) {
            systemIdMap = new SystemIdMap();
        }
        systemIdMap.setSystemId(seq, uri);
    }


    /**
    * Get the system id of an element in the document
    */

    String getSystemId(int seq) {
        if (systemIdMap==null) {
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

    void setLineNumber(int sequence, int line) {
        if (lineNumberMap != null) {
            lineNumberMap.setLineNumber(sequence, line);
        }
    }

    /**
    * Get the line number for an element. Return -1 if line numbering is off.
    */

    int getLineNumber(int sequence) {
        if (lineNumberMap != null) {
            return lineNumberMap.getLineNumber(sequence);
        }
        return -1;
    }

    /**
    * Get the line number of this root node.
    * @return 0 always
    */

    public int getLineNumber() {
        return 0;
    }

    /**
    * Return the type of node.
    * @return Type.DOCUMENT (always)
    */

    public final int getNodeKind() {
        return Type.DOCUMENT;
    }

    /**
     * Find the parent node of this node.
     * @return The Node object describing the containing element or root node.
     */

    public NodeInfo getParent()  {
        return null;
    }

    /**
    * Get the root node
    * @return the NodeInfo that is the root of the tree - not necessarily a document node
    */

    public NodeInfo getRoot() {
        return (rootNode==nodeNr ? this : getNode(rootNode));
    }

    /**
    * Get the root (document) node
    * @return the DocumentInfo representing the document node, or null if the
    * root of the tree is not a document node
    */

    public DocumentInfo getDocumentRoot() {
        return (rootNode==nodeNr ? this : null);
    }

    /**
    * Get a character string that uniquely identifies this node
    * @return an identifier based on the document number
    */

    public String generateId() {
        return "d"+documentNumber;
    }

    /**
    * Get a list of all elements with a given name. This is implemented
    * as a memo function: the first time it is called for a particular
    * element type, it remembers the result for next time.
    */

    AxisIterator getAllElements(int fingerprint) {
    	Integer key = new Integer(fingerprint);
    	if (elementList==null) {
    	    elementList = new HashMap(20);
    	}
        ArrayList list = (ArrayList)elementList.get(key);
        if (list==null) {
            list = new ArrayList(numberOfNodes/20);
            for (int i=1; i<numberOfNodes; i++) {
                if (nodeKind[i]==Type.ELEMENT &&
                        (nameCode[i] & 0xfffff ) == fingerprint) {
                    list.add(getNode(i));
                }
            }
            elementList.put(key, list);
        }
        return new ListIterator(list);
    }

    /**
    * Register a unique element ID. Fails if there is already an element with that ID.
    * @param e The NodeInfo (always an element) having a particular unique ID value
    * @param id The unique ID value
    */

    private void registerID(NodeInfo e, String id) {
        // the XPath spec (5.2.1) says ignore the second ID if it's not unique
        NodeInfo old = (NodeInfo)idTable.get(id);
        if (old==null) {
            idTable.put(id, e);
        }

    }

    /**
    * Get the element with a given ID.
    * @param id The unique ID of the required element, previously registered using registerID()
    * @return The NodeInfo (always an Element) for the given ID if one has been registered,
    * otherwise null.
    */

    public NodeInfo selectID(String id) {
        if (idTable==null) return null;			// no ID values found
        return (NodeInfo)idTable.get(id);
    }

    /**
    * Set an unparsed entity URI associated with this document. For system use only, while
    * building the document.
    */

    void setUnparsedEntity(String name, String uri, String publicId) {
        // System.err.println("setUnparsedEntity( " + name + "," + uri + ")");
        if (entityTable==null) {
            entityTable = new HashMap(20);
        }
        String[] ids = new String[2];
        ids[0] = uri;
        ids[1] = publicId;
        entityTable.put(name, ids);
    }

    /**
    * Get the unparsed entity with a given nameID if there is one, or null if not. If the entity
    * does not exist, return null.
    * @param name the name of the entity
    * @return if the entity exists, return an array of two Strings, the first holding the system ID
    * of the entity, the second holding the public
    */

    public String[] getUnparsedEntity(String name) {
        if (entityTable==null) {
            return null;
        }
        return (String[])entityTable.get(name);
    }

    /**
     * The following methods are defined in DOM Level 3, and Saxon includes nominal implementations of these
     * methods so that the code will compile when DOM Level 3 interfaces are installed.
     */

    public String getInputEncoding() {
        return null;
    }

    public String getXmlEncoding() {
        return null;
    }

    public boolean getXmlStandalone() {
        return false;
    }

    public void setXmlStandalone(boolean xmlStandalone)
                                  throws DOMException {
        disallowUpdate();
    }

    public String getXmlVersion() {
        return "1.0";
    }
    public void setXmlVersion(String xmlVersion)
                                  throws DOMException {
        disallowUpdate();
    }

    public boolean getStrictErrorChecking() {
        return false;
    }

    public void setStrictErrorChecking(boolean strictErrorChecking) {
        disallowUpdate();
    }

    public String getDocumentURI() {
        return getSystemId();
    }

    public void setDocumentURI(String documentURI) {
        disallowUpdate();
    }

    public Node adoptNode(Node source)
                          throws DOMException {
        disallowUpdate();
        return null;
    }

//    DOM LEVEL 3 METHOD
//    public DOMConfiguration getDomConfig() {
//        return null;
//    }

    public void normalizeDocument() {
        disallowUpdate();
    }

    public Node renameNode(Node n,
                           String namespaceURI,
                           String qualifiedName)
                           throws DOMException {
        disallowUpdate();
        return null;
    }

    /**
    * Copy this node to a given outputter
    */

    public void copy(Receiver out, int whichNamespaces, boolean copyAnnotations, int locationId) throws XPathException {

        out.startDocument(0);

        // output the children

        AxisIterator children = iterateAxis(Axis.CHILD);
        while (true) {
            NodeInfo n = (NodeInfo)children.next();
            if (n == null) {
                break;
            }
            n.copy(out, whichNamespaces, copyAnnotations, locationId);
        }

        out.endDocument();
    }

	/**
	* Produce diagnostic print of main tree arrays
	*/

	public void diagnosticDump() {
		System.err.println("    node    type   depth    next   alpha    beta    name");
		for (int i=0; i<numberOfNodes; i++) {
			System.err.println(n8(i) + n8(nodeKind[i]) + n8(depth[i]) + n8(next[i]) +
									 n8(alpha[i]) + n8(beta[i]) + n8(nameCode[i]));
		}
		System.err.println("    attr  parent    name    value");
		for (int i=0; i<numberOfAttributes; i++) {
		    System.err.println(n8(i) + n8(attParent[i]) + n8(attCode[i]) + "    " + attValue[i]);
		}
		System.err.println("      ns  parent  prefix     uri");
		for (int i=0; i<numberOfNamespaces; i++) {
		    System.err.println(n8(i) + n8(namespaceParent[i]) + n8(namespaceCode[i]>>16) + n8(namespaceCode[i]&0xffff));
		}
	}

    /**
    * Output a number as a string of 8 characters
    */

    private String n8(int val) {
        String s = "        " + val;
        return s.substring(s.length()-8);
    }

    public void showSize() {
        System.err.println("Tree size: " + numberOfNodes + " nodes, " + charBufferLength + " characters, " +
                                numberOfAttributes + " attributes");
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
// Portions marked PB-SYNC are Copyright (C) Peter Bryant (pbryant@bigfoot.com). All Rights Reserved.
//
// Portions created by Martin Haye are Copyright (C) Regents of the University 
// of California. All Rights Reserved. 
//
// Contributor(s): Peter Bryant, Martin Haye. 
//
