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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;

import org.xml.sax.InputSource;

import net.sf.saxon.Configuration;
import net.sf.saxon.event.ProxyReceiver;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.Sender;
import net.sf.saxon.om.AllElementStripper;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.type.Type;

import org.cdlib.xtf.lazyTree.hackedSaxon.TinyBuilder;
import org.cdlib.xtf.lazyTree.hackedSaxon.TinyDocumentImpl;
import org.cdlib.xtf.lazyTree.hackedSaxon.TinyNodeImpl;
import org.cdlib.xtf.util.*;
import org.cdlib.xtf.util.ConsecutiveMap;
import org.cdlib.xtf.util.PackedByteBuf;

/**
 * <p>Creates and/or loads a disk-based representation of an XML tree. Once 
 * created, the persistent version can be quickly and incrementally loaded
 * into memory.</p>
 * 
 * <p>To build a tree, there are two methods:</p>
 * 
 * <p>Method 1: Simply instatiate an instance, then call
 * {@link #build(File, File)}, which does all the work of loading a file
 * and saving it to a persistent file.</p>
 * 
 * <p>Method 2: Call the {@link #begin(File)} method to start the process.
 * Using the Receiver it returns, pass all the SAX events gathered from
 * parsing the document. Finally, {@link #finish(Receiver)} will complete
 * the process.</p>
 * 
 * <p>To load a tree that was built previously, use either load method:
 * {@link #load(File)} or {@link #load(File, LazyDocument)}.
 * 
 * @author Martin Haye
 */
public class LazyTreeBuilder
{
    /** The Saxon 'tiny' document, used to load the input tree */
    private TinyDocumentImpl doc;
    
    /** Name pool used to map namecodes */
    private NamePool         namePool;
    
    /** Mapping of names found to our internal name numbers */
    private ConsecutiveMap   names = new ConsecutiveMap();
    
    /** Saxon configuration used for tree loading */
    private Configuration    config;

    
    /** File version stored in the persistent file. */
    public static final String curVersion = "1.01";
    
    /** Default constructor -- sets up the configuration */
    public LazyTreeBuilder()
    {
        config = new Configuration();
        config.setErrorListener( new XTFSaxonErrorListener() );
    }


    /** Establishes the name pool used to resolve namecodes */
    public void setNamePool( NamePool pool ) {
        namePool = pool;
    }
    
    /**
     * Builds a tree that's loaded on-demand from a persistent disk file. If
     * the persistent file doesn't exist or is out-of-date, it is rebuilt
     * from scratch (which takes some time.)
     * 
     * @param sourceFile    The original XML document
     * @param persistFile   Location for the persistent version of that doc
     * 
     * @return              The root node of the document
     */
    public NodeInfo build( File sourceFile, File persistFile )
        throws TransformerException, IOException
    {
        assert namePool != null : "Must setNamePool() before building LazyTree";
        
        // Check for existence and up-to-dateness of the persistent file.
        if( !persistFile.exists() ||
            persistFile.lastModified() < sourceFile.lastModified() )
        {
            persistFile.delete();
            generate( sourceFile, persistFile );
        }
        
        // Now load it.
        try {
            return load( persistFile );
        }
        catch( Throwable t ) {
            // Maybe the file was corrupt. Rebuild and give it one more try.
            persistFile.delete();
            generate( sourceFile, persistFile );
            return load( persistFile );
        }
    } // build()
    
    
    /**
     * Load a persistent document using the default loader.
     * 
     * @param persistFile   The file to load from
     * 
     * @return The root node of the document (which implements DocumentInfo)
     */
    public NodeInfo load( File persistFile )
        throws FileNotFoundException, IOException
    {
        LazyDocument targetDoc = new LazyDocument();
        load( persistFile, targetDoc );
        return targetDoc;
    } // load()

    
    /**
     * Load a pre-existing persistent tree and load it into an empty in-memory
     * document.
     * 
     * @param persistFile   The file to load from
     * @param emptyDoc      An empty document object to initialize
     */
    public void load( File persistFile, LazyDocument emptyDoc )
        throws FileNotFoundException, IOException
    {
        // Open the structured file.
        StructuredFile file = StructuredFile.open( persistFile );
        
        // Don't use it if the version number is old.
        String fileVer = file.getUserVersion();
        if( fileVer.compareTo(curVersion) < 0 )
            throw new IOException( "Cannot use old version of LazyTree file" ); 
        
        // Now init the document (which loads the root node.)
        emptyDoc.init( namePool, file );
        emptyDoc.setSystemId( persistFile.getCanonicalPath() );
    }

    /**
     * Alternate way of constructing a lazy tree. First, begin() is called,
     * returning a Receiver that should receive all the SAX events from the
     * input. When all events have been sent, then call 
     * {@link #finish(Receiver)}.
     */
    public Receiver begin( File persistFile )
        throws IOException
    {
        // A great way to read the tree in just the form we need it is to
        // use Saxon's "TinyTree" implementation. Unfortunately, all of its
        // members are private, so we actually use a hacked version whose only
        // difference is that they're made public, and that text is accumulated
        // straight to the disk file, rather than to a memory buffer.
        //
        TinyBuilder builder = new TinyBuilder();
        if( namePool == null ) 
            namePool = NamePool.getDefaultNamePool();
        builder.setConfiguration( config );

        // We're going to make a structured file to contain the entire tree.
        // To save memory, we'll write the character data directly to it
        // rather than buffer it up.
        //
        StructuredFile treeFile = StructuredFile.create( persistFile );
        builder.setTreeFile( treeFile );
        
        treeFile.setUserVersion( curVersion );
        
        Subfile textFile = treeFile.createSubfile( "text" );
        builder.setTextFile( textFile );
        
        // Done for now.
        return builder;
        
    } // begin()
    
    /**
     * Retrieves the current node number in the build. Indexer uses this to
     * record node numbers in text chunks.
     * 
     * @param inBuilder     The builder gotten from begin()
     * @return              The current node number.
     */
    public int getNodeNum( Receiver inBuilder ) {
        TinyBuilder builder = (TinyBuilder)inBuilder;
        doc = (TinyDocumentImpl) builder.getCurrentDocument();
        return doc.numberOfNodes;
    } // getNodeNum()

    /**
     * Completes writing out a disk-based file. Assumes that the receiver
     * (which must come from begin()) has been sent all the SAX events for 
     * the input document.
     */
    public void finish( Receiver inBuilder )
        throws IOException
    {
        TinyBuilder builder = (TinyBuilder)inBuilder;
        StructuredFile treeFile = builder.getTreeFile();
        File persistFile = treeFile.getFile();
        
        doc = (TinyDocumentImpl) builder.getCurrentDocument();
         
        // Done with the text file now.
        builder.getTextFile().close();
        
        // If the build failed, delete the file.
        if( doc == null ) {
            treeFile.close();
            persistFile.delete();
            return;
        }
         
        // Make sure we support all the features used in the document.
        checkSupport();
         
        // Now make a structured file containing the entire tree's contents.
        writeNames   ( treeFile.createSubfile("names") );
        writeAttrs   ( treeFile.createSubfile("attributes") ); // must be before nodes
        writeNodes   ( treeFile.createSubfile("nodes") );
         
        // All done!
        treeFile.close();
        doc      = null;
        names    = null;
    }
    
    /** 
     * Build a new persistent disk file from scratch, using the sourceFile
     * as input.
     */
    private void generate( File sourceFile, File persistFile ) 
        throws TransformerException, IOException
    {
        Receiver builder = begin( persistFile );

        SAXSource source = new SAXSource(
                new InputSource(sourceFile.toURL().toString()));

        ProxyReceiver stripper = AllElementStripper.getInstance();
        stripper.setUnderlyingReceiver( builder );
        
        new Sender(config).send( source, stripper );
        
        finish( builder );
    } // generate()

    /** 
     * Build and write out the table of names referenced by the tree. Also
     * includes the namespaces.
     * 
     * @param out   Subfile to write to.
     */
    private void writeNames( Subfile out )
        throws IOException
    {
        PackedByteBuf buf = new PackedByteBuf( 1000 );
        
        // Write out all the namespaces.
        buf.writeInt( doc.numberOfNamespaces );
        for( int i = 0; i < doc.numberOfNamespaces; i++ ) {
            int code = doc.namespaceCode[i];
            buf.writeString( namePool.getPrefixFromNamespaceCode(code) );
            buf.writeString( namePool.getURIFromNamespaceCode(code) );
            buf.writeInt( doc.namespaceParent[i] );
        }
        
        // Add all the namecodes from elements and attributes.
        for( int i = 0; i < doc.numberOfNodes; i++ ) {
            if( doc.nameCode[i] >= 0 )
                names.put( new Integer(doc.nameCode[i]) );
        }
        
        for( int i = 0; i < doc.numberOfAttributes; i++ ) {  
            if( doc.attCode[i] >= 0 )
                names.put( new Integer(doc.attCode[i]) );
        }
        
        // Write out all the namecodes.
        Object[] nameArray = names.getArray();
        buf.writeInt( nameArray.length );
        for( int i = 0; i < nameArray.length; i++ ) {
            int code = ((Integer)nameArray[i]).intValue();
            buf.writeString( namePool.getPrefix(code) );
            buf.writeString( namePool.getURI(code) );
            buf.writeString( namePool.getLocalName(code) );
        }
        
        // All done.
        buf.output( out );
        out.close();
    } // writeNames()
    
    /** 
     * Build and write out all the nodes in the tree. The resulting table has
     * fixed-sized entries, sized to fit the largest node.
     * 
     * @param out   Subfile to write to.
     */
    private void writeNodes( Subfile out )
    throws IOException
    {
        // Write the root node's number
        out.writeInt( doc.rootNode ); 

        // Pack up each node. That way we can calculate the maximum size of
        // any particular one, and they can be randomly accessed by multiplying
        // the node number by that size.
        //
        PackedByteBuf[] nodeBufs = new PackedByteBuf[doc.numberOfNodes];
        int maxSize = 0;
        doc.ensurePriorIndex();
        for( int i = 0; i < doc.numberOfNodes; i++ ) {
            PackedByteBuf buf = nodeBufs[i] = new PackedByteBuf( 20 );
            
            // Kind
            buf.writeByte( doc.nodeKind[i] );
            
            // Flag
            NodeInfo node     = doc.getNode( i );
            int      nameCode = doc.nameCode[i];
            int      parent   = (node.getParent() != null) ?
                                (((TinyNodeImpl)node.getParent()).nodeNr) :
                                 -1;
            int      prevSib  = doc.prior[i];
            int      nextSib  = (doc.next[i] > i) ? doc.next[i] : -1;
            int      child    = node.hasChildNodes() ? (i+1) : -1;
            int      alpha    = doc.alpha[i];
            int      beta     = doc.beta[i];

            int flags = ((nameCode != -1) ? Flag.HAS_NAMECODE     : 0) |
                        ((parent   != -1) ? Flag.HAS_PARENT       : 0) |
                        ((prevSib  != -1) ? Flag.HAS_PREV_SIBLING : 0) |
                        ((nextSib  != -1) ? Flag.HAS_NEXT_SIBLING : 0) |
                        ((child    != -1) ? Flag.HAS_CHILD        : 0) |
                        ((alpha    != -1) ? Flag.HAS_ALPHA        : 0) |
                        ((beta     != -1) ? Flag.HAS_BETA         : 0);
            buf.writeInt( flags );
            
            // Name code
            if( nameCode >= 0 ) {
                int nameIdx = names.get( new Integer(nameCode) );
                assert nameIdx >= 0 : "A name was missed when writing name codes";
                buf.writeInt( nameIdx );
            }
            
            // Parent
            if( parent >= 0 )
                buf.writeInt( parent );
            
            // Prev sibling
            if( prevSib >= 0 )
                buf.writeInt( prevSib  );
            
            // Next sibling.
            if( nextSib >= 0 )
                buf.writeInt( nextSib );
            
            // First child (if any).
            if( child >= 0 ) {
                assert child != 0;
                buf.writeInt( child );
            }
            
            // Alpha and beta
            if( alpha != -1 )
                buf.writeInt( doc.alpha[i] );
            if( beta != -1 )
                buf.writeInt( doc.beta[i] );
            
            // Now calculate the size of the buffer, and bump the max if needed
            buf.compact();
            maxSize = Math.max( maxSize, buf.length() );
        } // for i
        
        // Okay, we're ready to write out the node table now. First comes the
        // number of nodes, followed by the size in bytes of each one.
        //
        out.writeInt( doc.numberOfNodes );
        out.writeInt( maxSize );
        
        for( int i = 0; i < doc.numberOfNodes; i++ )
            nodeBufs[i].output( out, maxSize );
        
        // All done.
        out.close();
    } // writeNodes()   
    
    /** 
     * Build and write out all the attributes in the tree. The resulting table
     * has variable-sized entries. 
     * 
     * @param out   Subfile to write to.
     */
    private void writeAttrs( Subfile out )
        throws IOException
    {
        // Write placeholder for the maximum size of any block.
        out.writeInt( 0 );
        
        // Write out each attrib, and record their offsets and lengths.
        int maxSize = 0;
        PackedByteBuf buf = new PackedByteBuf( 100 );
        for( int i = 0; i < doc.numberOfAttributes; ) 
        {
            // Figure out how many attributes for this parent.
            int j;
            for( j = i+1; j < doc.numberOfAttributes; j++ ) {
                if( doc.attParent[j] != doc.attParent[i] )
                    break;
            }
            
            int nAttrs = j - i;
            
            // Pack them all up.
            buf.reset();
            buf.writeInt( nAttrs );
            for( j = i; j < i+nAttrs; j++ ) {
            
                // Name code
                int nameIdx = names.get( new Integer(doc.attCode[j]) );
                assert nameIdx >= 0 : "A name was missed when writing name codes";
                buf.writeInt( nameIdx );
                
                // Value
                buf.writeString( doc.attValue[j].toString() );
            }
            
            // Record the offset in the attribute file.
            int parent = doc.attParent[i];
            assert doc.nodeKind[parent] == Type.ELEMENT;
            doc.alpha[parent] = (int) out.getFilePointer();
            
            // Write out the data, and bump the max if necessary.
            buf.output( out );
            maxSize = Math.max( maxSize, buf.length() );
            
            // Next!
            i += nAttrs;
        } // for i
        
        // To avoid the reader having to worry about overrunning the subfile,
        // write an extra maxSize bytes.
        //
        byte[] tmp = new byte[maxSize];
        out.write( tmp );
        
        // Go back and write the max size.
        out.seek( 0 );
        out.writeInt( maxSize );
        
        // All done.
        out.close();
    } // writeAttrs()   
    
    /**
     * Checks that the tree doesn't use features we don't support. If it does,
     * we throw an exception.
     */
    private void checkSupport()
        throws IOException
    {
        if( doc.idTable != null )
            throw new IOException( "LazyTree does not support idTable yet" );
        if( doc.elementList != null )
            throw new IOException( "LazyTree does not support elementList yet" );
        if( doc.entityTable != null )
            throw new IOException( "LazyTree does not support entityTable yet" );
        if( doc.attTypeCode != null )
            throw new IOException( "LazyTree does not support attribute type annotations yet" );
    } // checkSupport()
    
    
} // class LazyTreeBuilder
