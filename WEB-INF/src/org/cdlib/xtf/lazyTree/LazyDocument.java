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

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.om.ArrayIterator;
import net.sf.saxon.om.ListIterator;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.AxisIterator;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.Navigator;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.StrippedNode;
import net.sf.saxon.tree.SystemIdMap;
import net.sf.saxon.type.Type;
import net.sf.saxon.xpath.XPathException;

import org.cdlib.xtf.util.DiskHashReader;
import org.cdlib.xtf.util.DiskHashWriter;
import org.cdlib.xtf.util.PackedByteBuf;
import org.cdlib.xtf.util.StructuredFile;
import org.cdlib.xtf.util.Subfile;
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
public class LazyDocument extends ParentNodeImpl
    implements DocumentInfo, Document, PersistentTree
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
    protected StructuredFile mainFile;

    /** Contains all the text, processing instructions, and comments */
    protected Subfile textFile;
    
    /** Contains all the nodes */
    protected Subfile nodeFile;
    
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
    protected Subfile attrFile;
    
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
    
    public LazyDocument() {
        super( null );
    }
    
    /**
     * Open a lazy tree and read in the root node.
     * 
     * @param pool  The name pool to map namecodes with
     * @param file  The file to open
     */
    public void init( NamePool pool, StructuredFile file )
        throws IOException
    {
        this.mainFile = file;
        
        nodeNum = 0;
        document = this;

        // Record the name pool, and allocate our document number.
        namePool = pool;
        documentNumber = pool.allocateDocumentNumber( this );

        // First, read in the names.
        synchronized( mainFile ) {
            readNames( file.openSubfile("names") );
    
            // Now open the other files and read their headers.
            nodeFile = file.openSubfile( "nodes" );
            rootNodeNum   = nodeFile.readInt();
            numberOfNodes = nodeFile.readInt();
            maxNodeSize   = nodeFile.readInt();
            
            attrFile = file.openSubfile( "attributes" );
            maxAttrSize = attrFile.readInt();
            
            textFile = file.openSubfile( "text" );
            
            // Allocate the buffer for reading nodes.
            nodeBytes = new byte[maxNodeSize];
            nodeBuf = new PackedByteBuf( 0 );
            
            // Likewise for reading attributes.
            attrBytes = new byte[maxAttrSize];
            attrBuf = new PackedByteBuf( 0 );
            
            // Read in the root node (shenanigans to force loading)
            nodeNum = rootNodeNum;
            rootNodeNum = -1;
            getNode( nodeNum );
            rootNodeNum = nodeNum;
        }
    } // constructor
    
    /**
     * If 'flag' is true, all loaded nodes will be cached until the tree goes
     * away, instead of being held by weak references.
     */
    public void setAllPermanent( boolean flag ) {
        allPermanent = flag;
        if( allPermanent )
            nodeCache.put( new Integer(0), this );
    }

    /**
     * Establish whether to print out debugging statements when key indexes
     * are created.
     */
    public void setDebug( boolean flag ) {
        this.debug = flag;
    }
    
    /** Find out whether debug lines are printed during key index creation */
    public boolean getDebug( ) {
        return debug;
    }
    
    /**
     * Closes all disk files opened by the document. While this will
     * theoretically be done when the LazyDocument is garbage collected, it's
     * a good idea to conserve file handles by closing them promptly as soon
     * as the tree's usefulness is done.
     */
    public void close()
    {
        try {
            textFile.close();
            nodeFile.close();
            attrFile.close();
            mainFile.close();
        }
        catch( IOException e ) {
            // Not a big deal if we can't close... ignore the error.
        }
    } // close()

    /**
     * Fetches the name list from a sub-file in the persistent disk file.
     * 
     * @param in The subfile to load from
     */
    private void readNames( Subfile in )
        throws IOException
    {
        // Read in the packed data.
        byte[] data = new byte[ (int)in.length() ];
        in.read( data );
        PackedByteBuf buf = new PackedByteBuf( data );
        
        // Read in the namespaces and calculate their codes.
        numberOfNamespaces = buf.readInt();
        namespaceParent = new int[numberOfNamespaces];
        namespaceCode   = new int[numberOfNamespaces];
        for( int i = 0; i < numberOfNamespaces; i++ ) {
            String prefix = buf.readString();
            String uri = buf.readString();
            namespaceCode[i] = namePool.allocateNamespaceCode( prefix, uri );
            namespaceParent[i] = buf.readInt();
        }
        
        // Now process all the namecodes.
        int nNamecodes = buf.readInt();
        nameNumToCode = new int[nNamecodes];
        for( int i = 0; i < nNamecodes; i++ ) {
            String prefix    = buf.readString();
            String uri       = buf.readString();
            String localName = buf.readString();
            nameNumToCode[i] = namePool.allocate( prefix, uri, localName );
        }
    } // readNames()
    
    /**
     * Writes a disk-based version of an xsl:key index. Use getIndex() later 
     * to read it back.
     * 
     * @param indexName Uniquely computed name
     * @param index     HashMap mapping String -> ArrayList[NodeImpl]
     */
    public void putIndex( String indexName, HashMap index )
        throws IOException
    {
        DiskHashWriter writer = new DiskHashWriter();
        PackedByteBuf buf = new PackedByteBuf( 100 );
        
        // Pack up each key and put into the DiskHashWriter
        for( Iterator iter = index.keySet().iterator(); iter.hasNext(); ) {
            String key = (String) iter.next();
            ArrayList list = (ArrayList) index.get( key );
            
            buf.reset();
            buf.writeInt( list.size() );
            
            int currentNum = 0;
            for( int i = 0; i < list.size(); i++ ) {
                Item node = (Item) list.get(i);
                int nodeNum;
                if( node instanceof NodeImpl )
                    nodeNum = ((NodeImpl)node).nodeNum;
                else if( node instanceof StrippedNode )
                    nodeNum = ((NodeImpl)((StrippedNode)node).getUnderlyingNode()).nodeNum;
                else {
                    assert false : "Cannot get node number";
                    nodeNum = 1;
                }
                buf.writeInt( nodeNum - currentNum );
                currentNum = nodeNum;
            } // for i
            
            writer.put( key, buf ); 
        } // for iter
            
        // Now write out the full hash.
        writer.outputTo( mainFile.createSubfile(indexName) );
    } // putIndex()
    
    /**
     * Access a disk-based xsl:key index stored by putIndex(). Note that the
     * entire index isn't loaded, just the header. Individual entries will
     * be loaded as needed by the DiskHashReader.
     * 
     * @param indexName     Name of the index to load
     * @return              Reader to access the index with.
     */
    public DiskHashReader getIndex( String indexName )
    {
        try {
            Subfile indexFile = mainFile.openSubfile( indexName );
            return new DiskHashReader( indexFile );
        }
        catch( Exception e ) {
            return null;
        }
    } // getIndex()

    /**
    * Set the Configuration that contains this document
    */

    public void setConfiguration(Configuration config) {
        this.config = config;
        NamePool pool = config.getNamePool();
        documentNumber = pool.allocateDocumentNumber(this);
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
    public void setRootNode( NodeInfo root ) {
        rootNodeNum = ((NodeImpl)root).nodeNum;
    }

    /**
     * Set the type annotation of an element node
     */
    protected void setElementAnnotation(int nodeNum, int typeCode) {
        assert false : "LazyTree doesn't support element annotations yet";
    }

    /**
     * Get the type annotation of an element node.
     * -1 if there is no type annotation
     */
    protected int getElementAnnotation(int nodeNum) {
        return -1;
    }
    
    /**
     * Get the type of node this document is -- ie it's a document node.
     */
    public int getNodeKind() { return Type.DOCUMENT; }


    /**
     * Get a node by its node number, loading it from disk if necessary.
     * 
     * @param num   The number to get
     * @return      A node, or null if the number is invalid.
     */
    public NodeImpl getNode( int num ) 
    {
        // If it's in the cache, we need do no more.
        NodeImpl node = checkCache( num );
        if( node != null )
            return node;
        
        try {
            // Validate the number
            if( num >= numberOfNodes || num < 0 ) {
                assert num == -1;
                return null;
            }

            // Easy out for root node
            if( num == rootNodeNum )
                return this;
            
            // Bump the count if we're profiling
            if( profileListener != null )
                profileListener.bumpCount( num );
            
            // Read the most data it could be.
            synchronized( mainFile ) {
                nodeFile.seek( NODE_FILE_HEADER_SIZE + (num * maxNodeSize) );
                nodeFile.readFully( nodeBytes );
            }
            
            // Get the type and the flags.
            nodeBuf.setBytes( nodeBytes );
            short kind  = nodeBuf.readByte();
            int   flags = nodeBuf.readInt();
            
            // Construct the node based on the kind.
            switch( kind ) {
            case Type.DOCUMENT:
                node = this; break;
            case Type.ELEMENT:
                node = createElementNode(); break;
            case Type.TEXT:
                node = createTextNode(); break;
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
            node.nodeNum  = num;
            
            // Read other stuff according to the flags.
            if( (flags & Flag.HAS_NAMECODE) != 0 ) {
                int nameIdx = nodeBuf.readInt();
                node.nameCode = nameNumToCode[nameIdx];
            }
            else 
                node.nameCode = -1;
            
            if( (flags & Flag.HAS_PARENT) != 0 )
                node.parentNum = nodeBuf.readInt();
            else
                node.parentNum = -1;
                
            if( (flags & Flag.HAS_PREV_SIBLING) != 0 )
                node.prevSibNum = nodeBuf.readInt();
            else
                node.prevSibNum = -1;
            
            if( (flags & Flag.HAS_NEXT_SIBLING) != 0 )
                node.nextSibNum = nodeBuf.readInt();
            else
                node.nextSibNum = -1;
            
            if( (flags & Flag.HAS_CHILD) != 0 )
            {
                assert node instanceof ParentNodeImpl;
                ((ParentNodeImpl)node).childNum = nodeBuf.readInt();
                assert ((ParentNodeImpl)node).childNum > 0;
            }
            else if( node instanceof ParentNodeImpl )
                ((ParentNodeImpl)node).childNum = -1;
            
            int alpha = -1;
            if( (flags & Flag.HAS_ALPHA) != 0 )
                alpha = nodeBuf.readInt();
            
            int beta = -1;
            if( (flags & Flag.HAS_BETA) != 0 )
                beta = nodeBuf.readInt();
            
            node.init( alpha, beta );
    
            // All done!
            nodeCache.put( new Integer(num), new SoftReference(node) );
            return node;
        } // try
        catch( IOException e ) {
            return null;
        }
    } // getNode()

    /**
     * Checks to see if we've already loaded the node corresponding with the
     * given number. If so, return it, else null.
     */
    protected NodeImpl checkCache( int num ) 
    {
      // Do we have a reference in the cache? If not, return. And if it's a
      // strong reference to a node, return it.
      //
      Object ref = nodeCache.get( new Integer( num ) );
      NodeImpl node = null;
      if( ref instanceof NodeImpl )
          node = (NodeImpl)ref;
      else if( ref instanceof SoftReference ) {

          // Is the reference still valid? If not, remove it.
          SoftReference weak = (SoftReference)ref;
          node = (NodeImpl)weak.get();
          if( node == null )
              nodeCache.remove( new Integer( num ) );
      }

      // All done.
      return node;
    } // checkCache()
    
    /**
     * Create an element node. Derived classes can override this to provide their
     * own element implementation.
     */
    protected NodeImpl createElementNode() {
        return new ElementImpl( this );
    }
    
    /**
     * Create a text node. Derived classes can override this to provide their
     * own text implementation.
     */
    protected NodeImpl createTextNode() {
        return new TextImpl( this );
    }

    /**
     * Get the node sequence number (in document order). Sequence numbers are 
     * monotonic but not consecutive.
     */
    public long getSequenceNumber() {
        return 0;
    }

    /**
     * determine whether this document uses namespaces
     */

    protected boolean isUsingNamespaces() {
        return usesNamespaces;
    }

    /**
    * Make a (transient) namespace node from the array of namespace declarations
    */

    protected NamespaceImpl getNamespaceNode(int nr) {
        return new NamespaceImpl(this, namespaceCode[nr], nr);
    }

    /**
     * Set the system id of this node
     */

    public void setSystemId(String uri) {
        if( uri==null )
            uri = "";
        if( systemIdMap == null )
            systemIdMap = new SystemIdMap();
        systemIdMap.setSystemId( nodeNum, uri);
    }

    /**
     * Get the system id of this root node
     */

    public String getSystemId() {
        if( systemIdMap == null )
            return null;
        return systemIdMap.getSystemId( nodeNum );
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

    protected void setSystemId(int seq, String uri) {
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

    protected String getSystemId(int seq) {
        if (systemIdMap==null) {
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

    protected void setLineNumber( int sequence, int line ) {
        assert false : "LazyTree does not support line numbering yet";
    }

    /**
     * Get the line number for an element. Return -1 if line numbering is off.
     */
    protected int getLineNumber( int sequence ) {
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
        return( (rootNodeNum == nodeNum) ? this : getNode(rootNodeNum) );
    }

    /**
     * Get the root (document) node
     * @return the DocumentInfo representing the document node, or null if the
     * root of the tree is not a document node
     */

    public DocumentInfo getDocumentRoot() {
        return( (rootNodeNum == nodeNum) ? this : null );
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
    protected AxisIterator getAllElements( int fingerprint )
    {
        // See if there's already a subfile for this name.
        String subName = "all-" + namePool.getDisplayName(fingerprint);
        try {
            Subfile indexFile = mainFile.openSubfile( subName );
            PackedByteBuf buf = new PackedByteBuf( indexFile, (int)indexFile.length() );
            int nNodes = buf.readInt();
            Item[] nodes = new Item[nNodes];
            int curNodeNum = 0;
            for( int i = 0; i < nNodes; i++ ) {
                curNodeNum += buf.readInt();
                nodes[i] = getNode( curNodeNum );
            }
            indexFile.close();
            return new ArrayIterator( nodes );
        }
        catch( IOException e ) {
        }
        
        if( debug ) {
            Trace.debug( "    Building list of elements named '" +
                         namePool.getDisplayName(fingerprint) + "'..." );
        }
        
        // Okay, we need to build a list.
        ArrayList nodes = new ArrayList( numberOfNodes / 8 );
        Vector nodeNums = new Vector( numberOfNodes / 8 );
        
        for( int i = 0; i < numberOfNodes; i++ ) {
            NodeImpl node = getNode( i );
            if( (node.getNameCode() & 0xfffff) != fingerprint )
                continue;
            nodes.add( node );
            nodeNums.add( new Integer(node.nodeNum) );
        }
        
        // Pack up the results.
        PackedByteBuf buf = new PackedByteBuf( nodeNums.size() * 3 );
        buf.writeInt( nodeNums.size() );
        int curNum = 0;
        for( int i = 0; i < nodeNums.size(); i++ ) {
            int num = ((Integer)nodeNums.get(i)).intValue();
            buf.writeInt( num - curNum );
            curNum = num;
        }
        
        try {
            // Now write a new sub-file.
            Subfile indexFile = mainFile.createSubfile( subName );
            buf.output( indexFile );
            indexFile.close();
        }
        catch( IOException e ) {
        }
        
        if( debug )
            Trace.debug( "done" );
        
        // Return the list we made (no need to re-read it).
        return new ListIterator(nodes);
    } // getAllElements()

    /**
     * Get the element with a given ID.
     * @param id The unique ID of the required element, previously registered using registerID()
     * @return The NodeInfo (always an Element) for the given ID if one has been registered,
     * otherwise null.
     */
    public NodeInfo selectID( String id ) {
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
    public String[] getUnparsedEntity( String name ) {
        assert false : "LazyTree does not support unparsed entities yet";
        return null;
    }

    /**
     * Copy this node to a given outputter
     */
    public void copy(Receiver out, int whichNamespaces, boolean copyAnnotations, int locationId)
        throws XPathException 
    {

        // output the children

        AxisIterator children = iterateAxis(Axis.CHILD);
        while( true ) {
            NodeInfo child = (NodeInfo) children.next();
            if( child == null )
                break;
            child.copy(out, whichNamespaces, copyAnnotations, locationId);
        }
    }
    
    /**
     * Turns on tracking for profiling purposes.
     */
    public void enableProfiling( Controller controller )
    {
        profileListener = new ProfilingListener( controller );
        controller.addTraceListener( profileListener );
    } // enableProfiling()
    
    /**
     * Prints the results of a trace run, to Trace.info().
     */
    public void printProfile()
        throws IOException
    {
        // Get a sorted array of the counts.
        ProfilingListener.ProfileCount[] counts = profileListener.getCounts();
        
        // Print it out.
        for( int i = counts.length-1; i >= 0; i-- ) {
            Trace.info( counts[i].count    + " " +
                        counts[i].systemId + ":" + 
                        counts[i].lineNum  );
            if( false ) {
                // For fun, print out all the nodes too.
                Set keys = counts[i].nodes.keySet();
                List list = new ArrayList(keys);
                Collections.sort( list );
                for( Iterator iter = list.iterator(); iter.hasNext(); ) {
                    int nodeNum = ((Integer)iter.next()).intValue();
                    NodeImpl node = getNode( nodeNum );
                    String path = Navigator.getPath( node );
                    Trace.info( "    " + path );
                }
            }
        }
    } // printProfile()
    
    /**
     * Shut off profiling... this is required if the same controller will
     * be used for another profiling session later.
     */
    public void disableProfiling()
    {
        assert profileListener != null : "Cannot disable profiling before enabling it";
        profileListener.getController().removeTraceListener( profileListener );
        profileListener = null;
    }    
    
    /**
     * The following methods are defined in DOM Level 3, and we include 
     * nominal implementations of these methods so that the code will 
     * compile when DOM Level 3 interfaces are installed.
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
    public DOMConfiguration getDomConfig() {
        return null;
    }

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

} // class DocumentImpl
