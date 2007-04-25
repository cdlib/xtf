package org.cdlib.xtf.lazyTree;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import net.sf.saxon.Configuration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.AxisIterator;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.FastStringBuffer;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.NodeListIterator;
import net.sf.saxon.om.StrippedNode;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.SystemIdMap;
import net.sf.saxon.type.Type;

import org.cdlib.xtf.util.DiskHashReader;
import org.cdlib.xtf.util.DiskHashWriter;
import org.cdlib.xtf.util.PackedByteBuf;
import org.cdlib.xtf.util.StructuredStore;
import org.cdlib.xtf.util.SubStoreReader;
import org.cdlib.xtf.util.SubStoreWriter;
import org.cdlib.xtf.util.ThreadWatcher;
import org.cdlib.xtf.util.Trace;

/**
 * <p>LazyDocument accesses the binary persistent disk file created by
 * {@link LazyTreeBuilder}, loading nodes on demand rather than holding all
 * of them in RAM.</p>
 *
 * <p>This class should never be instatiated directly, but rather loaded by
 * LazyTreeBuilder.</p>
 *
 * <p>Once loaded, a soft reference to the node is kept in RAM; if memory runs
 * low, these soft references will be thrown away. This behavior can be
 * defeated by calling {@link #setAllPermanent(boolean)}.</p>
 *
 * @author Martin Haye
 */
public class LazyDocument extends ParentNodeImpl implements DocumentInfo,
                                                            PersistentTree 
{
  /** Saxon configuration info */
  protected Configuration config;

  /** Name pool used to look up namecodes */
  protected NamePool namePool;

  /** Unique number assigned to each document */
  protected int documentNumber;

  /** Determines whether this document is using namespaces. Not sure why
   *  this works when false, but it does.
   */
  protected boolean usesNamespaces = false;

  /**
   * This structure supports trees whose root is an element node rather than
   * a document node. The document node still exists, for implementation
   * reasons, but it is not regarded as part of the tree. The variable
   * rootNode identifies the actual root of the tree, which is the document
   * node by default.
   */
  protected int rootNodeNum = 0;

  /** Flag denoting whether to print out when key indexes are created */
  protected boolean debug = false;

  /** The structured file that contains all our subfiles */
  protected StructuredStore mainStore;

  /** Contains all the text, processing instructions, and comments */
  protected SubStoreReader textFile;

  /** Contains all the nodes */
  protected SubStoreReader nodeFile;

  /** How many nodes, excluding attributes and namespaces. */
  protected int numberOfNodes;

  /** Size of the header on the node file */
  protected static final int NODE_FILE_HEADER_SIZE = 12;

  /** The size of the largest node entry on disk */
  protected int maxNodeSize;

  /** Byte buffer for reading nodes */
  protected byte[] nodeBytes;

  /** Buffer for unpacking nodes */
  protected PackedByteBuf nodeBuf;

  /** Contains all the attributes */
  protected SubStoreReader attrFile;

  /** The max size of any attribute block */
  protected int maxAttrSize;

  /** Byte buffer for reading nodes */
  protected byte[] attrBytes;

  /** Buffer for unpacking nodes */
  protected PackedByteBuf attrBuf;

  /** Number of namespaces currently declared */
  public int numberOfNamespaces = 0;

  /** namespaceParent is the index of the element node owning the namespace
   *  declaration
   */
  public int[] namespaceParent;

  /** namespaceCode is the namespace code used by the name pool: the top
   *  half is the prefix code, the bottom half the URI code
   */
  public int[] namespaceCode;

  /** Maps system IDs to nodes in the tree */
  public SystemIdMap systemIdMap = null;

  /** Maps name numbers in the file to namecodes in the current NamePool */
  int[] nameNumToCode;

  /** Caches nodes in memory so they only have to be loaded once. */
  HashMap nodeCache = new HashMap();

  /** True if nodes in the cache should be permanent, false for weak refs */
  boolean allPermanent = false;

  /** Notified of profile-related events */
  private ProfilingListener profileListener;

  /** Counter to govern periodic checking for thread time limit */
  private int killCheckCounter = 0;

  /**
   * Construct a new (empty) document. Should call
   * {@link #init(NamePool, StructuredStore)} afterward.
   */
  public LazyDocument(Configuration config) {
    this.config = config;
    documentNumber = config.getDocumentNumberAllocator()
                       .allocateDocumentNumber();

    // Check if we're being profiled.
    if (config.getTraceListener() instanceof ProfilingListener)
      profileListener = (ProfilingListener)config.getTraceListener();
  }

  /**
   * Open a lazy tree and read in the root node.
   *
   * @param pool  The name pool to map namecodes with
   * @param store  The file to open
   */
  public void init(NamePool pool, StructuredStore store)
    throws IOException 
  {
    this.mainStore = store;

    nodeNum = 0;
    parentNum = -1;
    document = this;

    // Record the name pool.
    namePool = pool;

    // First, read in the names.
    synchronized (mainStore) 
    {
      readNames(store.openSubStore("names"));

      // Now open the other files and read their headers.
      nodeFile = store.openSubStore("nodes");
      rootNodeNum = nodeFile.readInt();
      numberOfNodes = nodeFile.readInt();
      maxNodeSize = nodeFile.readInt();

      attrFile = store.openSubStore("attributes");
      maxAttrSize = attrFile.readInt();

      textFile = store.openSubStore("text");

      // Allocate the buffer for reading nodes.
      nodeBytes = new byte[maxNodeSize];
      nodeBuf = new PackedByteBuf(0);

      // Likewise for reading attributes.
      attrBytes = new byte[maxAttrSize];
      attrBuf = new PackedByteBuf(0);

      // Read in the root node (shenanigans to force loading)
      nodeNum = rootNodeNum;
      rootNodeNum = -1;
      getNode(nodeNum);
      rootNodeNum = nodeNum;
    }
  } // constructor

  /**
   * If 'flag' is true, all loaded nodes will be cached until the tree goes
   * away, instead of being held by weak references.
   */
  public void setAllPermanent(boolean flag) {
    allPermanent = flag;
    if (allPermanent)
      nodeCache.put(Integer.valueOf(0), this);
  }

  /**
   * Establish whether to print out debugging statements when key indexes
   * are created.
   */
  public void setDebug(boolean flag) {
    this.debug = flag;
  }

  /** Find out whether debug lines are printed during key index creation */
  public boolean getDebug() {
    return debug;
  }

  /** Print out the profile (if one was collected) */
  public void printProfile()
    throws IOException 
  {
    if (profileListener != null)
      profileListener.printProfile();
  }

  /**
   * Closes all disk files opened by the document. While this will
   * theoretically be done when the LazyDocument is garbage collected, it's
   * a good idea to conserve file handles by closing them promptly as soon
   * as the tree's usefulness is done.
   */
  public void close() 
  {
    try 
    {
      textFile.close();
      nodeFile.close();
      attrFile.close();
      mainStore.close();
    }
    catch (IOException e) {
      // Not a big deal if we can't close... ignore the error.
    }
  } // close()

  /**
   * Fetches the name list from a sub-file in the persistent disk file.
   *
   * @param in The subfile to load from
   */
  private void readNames(SubStoreReader in)
    throws IOException 
  {
    // Read in the packed data.
    byte[] data = new byte[(int)in.length()];
    in.read(data);
    PackedByteBuf buf = new PackedByteBuf(data);

    // Read in the namespaces and calculate their codes.
    numberOfNamespaces = buf.readInt();
    namespaceParent = new int[numberOfNamespaces];
    namespaceCode = new int[numberOfNamespaces];
    for (int i = 0; i < numberOfNamespaces; i++) {
      String prefix = buf.readString();
      String uri = buf.readString();
      namespaceCode[i] = namePool.allocateNamespaceCode(prefix, uri);
      namespaceParent[i] = buf.readInt();
    }

    // Now process all the namecodes.
    int nNamecodes = buf.readInt();
    nameNumToCode = new int[nNamecodes];
    for (int i = 0; i < nNamecodes; i++) {
      String prefix = buf.readString();
      String uri = buf.readString();
      String localName = buf.readString();
      nameNumToCode[i] = namePool.allocate(prefix, uri, localName);
    }
  } // readNames()

  /**
   * Writes a disk-based version of an xsl:key index. Use getIndex() later
   * to read it back.
   *
   * @param indexName Uniquely computed name
   * @param index     HashMap mapping String -> ArrayList[NodeImpl]
   */
  public void putIndex(String indexName, Map index)
    throws IOException 
  {
    DiskHashWriter writer = new DiskHashWriter();
    PackedByteBuf buf = new PackedByteBuf(100);

    // Pack up each key and put into the DiskHashWriter
    for (Iterator iter = index.keySet().iterator(); iter.hasNext();) 
    {
      String key = (String)iter.next();
      ArrayList list = (ArrayList)index.get(key);

      buf.reset();
      buf.writeInt(list.size());

      int currentNum = 0;
      for (int i = 0; i < list.size(); i++) 
      {
        Item node = (Item)list.get(i);
        int nodeNum;
        if (node instanceof NodeImpl)
          nodeNum = ((NodeImpl)node).nodeNum;
        else if (node instanceof StrippedNode)
          nodeNum = ((NodeImpl)((StrippedNode)node).getUnderlyingNode()).nodeNum;
        else {
          assert false : "Cannot get node number";
          nodeNum = 1;
        }
        buf.writeInt(nodeNum - currentNum);
        currentNum = nodeNum;
      } // for i

      writer.put(key, buf);
    } // for iter

    // Now write out the full hash. Be careful to avoid writing two files
    // at the same time.
    //
    synchronized (mainStore) {
      writer.outputTo(mainStore.createSubStore(indexName));
    }
  } // putIndex()

  /**
   * Access a disk-based xsl:key index stored by putIndex(). Note that the
   * entire index isn't loaded, just the header. Individual entries will
   * be loaded as needed by the DiskHashReader.
   *
   * @param indexName     Name of the index to load
   * @return              Reader to access the index with.
   */
  public DiskHashReader getIndex(String indexName) 
  {
    try 
    {
      synchronized (mainStore) {
        SubStoreReader indexFile = mainStore.openSubStore(indexName);
        return new DiskHashReader(indexFile);
      }
    }
    catch (Exception e) {
      return null;
    }
  } // getIndex()

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
    return namePool;
  }

  /**
   * Get the unique document number
   */
  public int getDocumentNumber() {
    return documentNumber;
  }

  /**
   * Set the root node. Parentless elements are implemented using a full tree structure
   * containing a document node, but the document node is not regarded as part of the tree
   */
  public void setRootNode(NodeInfo root) {
    rootNodeNum = ((NodeImpl)root).nodeNum;
  }

  /**
   * Set the type annotation of an element node
   */
  protected void setElementAnnotation(int nodeNum, int typeCode) {
    assert false : "LazyTree doesn't support element annotations yet";
  }

  /**
   * Get the type annotation of a node.
   * -1 if there is no type annotation
   */
  protected int getTypeAnnotation(int nodeNum) {
    return -1;
  }

  /**
   * Get the type of node this document is -- ie it's a document node.
   */
  public int getNodeKind() {
    return Type.DOCUMENT;
  }

  /**
   * Get a node by its node number, loading it from disk if necessary.
   *
   * @param num   The number to get
   * @return      A node, or null if the number is invalid.
   */
  public NodeImpl getNode(int num) 
  {
    // If it's in the cache, we need do no more.
    NodeImpl node = checkCache(num);
    if (node != null)
      return node;

    try 
    {
      // Validate the number
      if (num >= numberOfNodes || num < 0) {
        assert num == -1;
        return null;
      }

      // Easy out for root node
      if (num == rootNodeNum)
        return this;

      // Bump the count if we're profiling
      if (profileListener != null)
        profileListener.bumpCount(num);

      // Read the most data it could be.
      synchronized (mainStore) {
        nodeFile.seek(NODE_FILE_HEADER_SIZE + (num * maxNodeSize));
        nodeFile.read(nodeBytes);
      }

      // Get the type and the flags.
      nodeBuf.setBytes(nodeBytes);
      short kind = nodeBuf.readByte();
      int flags = nodeBuf.readInt();

      // Construct the node based on the kind.
      switch (kind) {
        case Type.DOCUMENT:
          node = this;
          break;
        case Type.ELEMENT:
          node = createElementNode();
          break;
        case Type.TEXT:
          node = createTextNode();
          break;
        case Type.COMMENT:
          assert false : "comments not yet supported";
          break;
        case Type.PROCESSING_INSTRUCTION:
          assert false : "processing instructions not yet supported";
          break;
        default:
          assert false : "Invalid node kind";
          return null;
      }

      // Make sure the node knows how to get back to the document.
      node.nodeNum = num;
      node.document = this;

      // Read other stuff according to the flags.
      if ((flags & Flag.HAS_NAMECODE) != 0) {
        int nameIdx = nodeBuf.readInt();
        node.nameCode = nameNumToCode[nameIdx];
      }
      else
        node.nameCode = -1;

      if ((flags & Flag.HAS_PARENT) != 0)
        node.parentNum = nodeBuf.readInt();
      else
        node.parentNum = -1;

      if ((flags & Flag.HAS_PREV_SIBLING) != 0)
        node.prevSibNum = nodeBuf.readInt();
      else
        node.prevSibNum = -1;

      if ((flags & Flag.HAS_NEXT_SIBLING) != 0)
        node.nextSibNum = nodeBuf.readInt();
      else
        node.nextSibNum = -1;
      
      assert node.prevSibNum != node.nextSibNum || node.prevSibNum < 0;
      assert node.prevSibNum < node.nodeNum;
      assert node.nextSibNum > node.nodeNum || node.nextSibNum < 0;

      if ((flags & Flag.HAS_CHILD) != 0) {
        assert node instanceof ParentNodeImpl;
        ((ParentNodeImpl)node).childNum = nodeBuf.readInt();
        assert ((ParentNodeImpl)node).childNum > 0;
      }
      else if (node instanceof ParentNodeImpl)
        ((ParentNodeImpl)node).childNum = -1;

      int alpha = -1;
      if ((flags & Flag.HAS_ALPHA) != 0)
        alpha = nodeBuf.readInt();

      int beta = -1;
      if ((flags & Flag.HAS_BETA) != 0)
        beta = nodeBuf.readInt();

      node.init(alpha, beta);

      // All done!
      nodeCache.put(Integer.valueOf(num), new SoftReference(node));
      return node;
    } // try
    catch (IOException e) {
      return null;
    }
  } // getNode()

  /**
   * Checks to see if we've already loaded the node corresponding with the
   * given number. If so, return it, else null.
   */
  protected NodeImpl checkCache(int num) 
  {
    // Every once in a while, check if our thread has exceeded its time
    // limit and should kill itself.
    //
    if (killCheckCounter++ > 1000) {
      killCheckCounter = 0;
      if (ThreadWatcher.shouldDie(Thread.currentThread()))
        throw new RuntimeException("Runaway request - time limit exceeded");
    }

    // Do we have a reference in the cache? If not, return. And if it's a
    // strong reference to a node, return it.
    //
    Object ref = nodeCache.get(Integer.valueOf(num));
    NodeImpl node = null;
    if (ref instanceof NodeImpl)
      node = (NodeImpl)ref;
    else if (ref instanceof SoftReference) 
    {
      // Is the reference still valid? If not, remove it.
      SoftReference weak = (SoftReference)ref;
      node = (NodeImpl)weak.get();
      if (node == null)
        nodeCache.remove(Integer.valueOf(num));
    }

    // All done.
    return node;
  } // checkCache()

  /**
   * Create an element node. Derived classes can override this to provide their
   * own element implementation.
   */
  protected NodeImpl createElementNode() {
    return new ElementImpl();
  }

  /**
   * Create a text node. Derived classes can override this to provide their
   * own text implementation.
   */
  protected NodeImpl createTextNode() {
    return new TextImpl();
  }

  /**
   * Get the node sequence number (in document order). Sequence numbers are
   * monotonic but not consecutive.
   */
  public long getSequenceNumber() {
    return 0;
  }

  /**
  * Get next sibling - always null
  * @return null
  */
  public final NodeInfo getNextSibling() {
    return null;
  }

  /**
  * Get previous sibling - always null
  * @return null
  */
  public final NodeInfo getPreviousSibling() {
    return null;
  }

  /**
  * Get a character string that uniquely identifies this node
  *  @param buffer a buffer into which will be placed a string based on the document number
   *
   */
  public void generateId(FastStringBuffer buffer) {
    buffer.append('d');
    buffer.append(Integer.toString(documentNumber));
  }

  /**
   * determine whether this document uses namespaces
   */
  protected boolean isUsingNamespaces() {
    return usesNamespaces;
  }

  /**
   * Set the system id of this node
   */
  public void setSystemId(String uri) {
    if (uri == null)
      uri = "";
    if (systemIdMap == null)
      systemIdMap = new SystemIdMap();
    systemIdMap.setSystemId(nodeNum, uri);
  }

  /**
   * Get the system id of this root node
   */
  public String getSystemId() {
    if (systemIdMap == null)
      return null;
    return systemIdMap.getSystemId(nodeNum);
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
  protected void setSystemId(int seq, String uri) 
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
  protected String getSystemId(int seq) 
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
    assert false : "LazyTree does not support line numbering yet";
  }

  /**
   * Set the line number for an element. Ignored if line numbering is off.
   */
  protected void setLineNumber(int sequence, int line) {
    assert false : "LazyTree does not support line numbering yet";
  }

  /**
   * Get the line number for an element. Return -1 if line numbering is off.
   */
  protected int getLineNumber(int sequence) {
    assert false : "LazyTree does not support line numbering yet";
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
  public final int getItemType() {
    return Type.DOCUMENT;
  }

  /**
   * Get the root node
   * @return the NodeInfo that is the root of the tree - not necessarily a document node
   */
  public NodeInfo getRoot() {
    return ((rootNodeNum == nodeNum) ? this : getNode(rootNodeNum));
  }

  /**
   * Get the root (document) node
   * @return the DocumentInfo representing the document node, or null if the
   * root of the tree is not a document node
   */
  public DocumentInfo getDocumentRoot() {
    return ((rootNodeNum == nodeNum) ? this : null);
  }

  /**
   * Get a character string that uniquely identifies this node
   * @return an identifier based on the document number
   */
  public String generateId() {
    return "d" + documentNumber;
  }

  /**
   * Get a list of all elements with a given name. This is implemented
   * as a memo function: the first time it is called for a particular
   * element type, it remembers the result for next time.
   */
  protected AxisIterator getAllElements(int fingerprint) 
  {
    synchronized(mainStore)
    {
      // See if there's already a subfile for this name.
      String subName = "all-" + namePool.getDisplayName(fingerprint);
      try 
      {
        SubStoreReader indexFile = mainStore.openSubStore(subName);
        PackedByteBuf buf = new PackedByteBuf(indexFile, (int)indexFile.length());
        int nNodes = buf.readInt();
        ArrayList nodes = new ArrayList(nNodes);
        int curNodeNum = 0;
        for (int i = 0; i < nNodes; i++) {
          curNodeNum += buf.readInt();
          nodes.add(getNode(curNodeNum));
        }
        indexFile.close();
        return new NodeListIterator(nodes);
      }
      catch (IOException e) {
      }
  
      if (debug) {
        Trace.debug(
          "Building list of elements named '" +
          namePool.getDisplayName(fingerprint) + "'.");
      }
  
      // Okay, we need to build a list.
      ArrayList nodes = new ArrayList(numberOfNodes / 8);
      Vector nodeNums = new Vector(numberOfNodes / 8);
  
      for (int i = 0; i < numberOfNodes; i++) {
        NodeImpl node = getNode(i);
        if (node == null || (node.getNameCode() & 0xfffff) != fingerprint)
          continue;
        nodes.add(node);
        nodeNums.add(Integer.valueOf(node.nodeNum));
      }
  
      // Pack up the results.
      PackedByteBuf buf = new PackedByteBuf(nodeNums.size() * 3);
      buf.writeInt(nodeNums.size());
      int curNum = 0;
      for (int i = 0; i < nodeNums.size(); i++) {
        int num = ((Integer)nodeNums.get(i)).intValue();
        buf.writeInt(num - curNum);
        curNum = num;
      }
  
      try 
      {
        // Now write a new sub-file.
        SubStoreWriter indexFile = mainStore.createSubStore(subName);
        buf.output(indexFile);
        indexFile.close();
      }
      catch (IOException e) {
      }
  
      // Return the list we made (no need to re-read it).
      return new NodeListIterator(nodes);
    }
  } // getAllElements()

  /**
   * Get the element with a given ID.
   * @param id The unique ID of the required element, previously registered using registerID()
   * @return The NodeInfo (always an Element) for the given ID if one has been registered,
   * otherwise null.
   */
  public NodeInfo selectID(String id) {
    assert false : "LazyTree does not support selectId() yet";
    return null;
  }

  /**
   * Get the unparsed entity with a given nameID if there is one, or null if not. If the entity
   * does not exist, return null.
   * @param name the name of the entity
   * @return if the entity exists, return an array of two Strings, the first holding the system ID
   * of the entity, the second holding the public
   */
  public String[] getUnparsedEntity(String name) {
    assert false : "LazyTree does not support unparsed entities yet";
    return null;
  }

  /**
   * Copy this node to a given outputter
   */
  public void copy(Receiver out, int whichNamespaces, boolean copyAnnotations,
                   int locationId)
    throws XPathException 
  {
    // output the children
    AxisIterator children = iterateAxis(Axis.CHILD);
    while (true) {
      NodeInfo child = (NodeInfo)children.next();
      if (child == null)
        break;
      child.copy(out, whichNamespaces, copyAnnotations, locationId);
    }
  }
} // class DocumentImpl

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
