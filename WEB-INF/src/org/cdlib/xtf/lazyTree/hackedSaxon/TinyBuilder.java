package org.cdlib.xtf.lazyTree.hackedSaxon;

import java.io.IOException;

import net.sf.saxon.event.Builder;
import net.sf.saxon.event.LocationProvider;
import net.sf.saxon.event.ReceiverOptions;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.FastStringBuffer;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;

import org.cdlib.xtf.util.PackedByteBuf;
import org.cdlib.xtf.util.StructuredStore;
import org.cdlib.xtf.util.SubStoreWriter;


/**
  * The TinyBuilder class is responsible for taking a stream of SAX events and constructing
  * a Document tree, using the "TinyTree" implementation.
  *
  * @author Michael H. Kay
  */

public class TinyBuilder extends Builder  {

    private TinyTree tree;

    private int currentDepth = 0;
    private int nodeNr = 0;             // this is the local sequence within this document
    private boolean ended = false;
    private int[] sizeParameters;       // estimate of number of nodes, attributes, namespaces, characters
    private PackedByteBuf textBuf = new PackedByteBuf( 1000 );
    private StructuredStore treeStore;
    private SubStoreWriter textStore;

    public void setSizeParameters(int[] params) {
        sizeParameters = params;
    }
    
    public void setTreeStore( StructuredStore treeStore ) {
        this.treeStore = treeStore;
    }
    
    public StructuredStore getTreeStore() {
        return treeStore;
    }

    public void setTextStore( SubStoreWriter textStore ) {
        this.textStore = textStore;
    }
    
    public SubStoreWriter getTextStore() {
        return textStore;
    }
    
    private int[] prevAtDepth = new int[100];
            // this array is scaffolding used while constructing the tree, it is
            // not present in the final tree.

    public TinyTree getTree() {
        return tree;
    }

    /**
     * Set the root (document) node to use. This method is used to support
     * the JAXP facility to attach transformation output to a supplied Document
     * node. It must be called before startDocument(), and the type of document
     * node must be compatible with the type of Builder used.
     */

    public void setRootNode(DocumentInfo doc) {
        currentRoot = doc;
        if (doc instanceof TinyDocumentImpl) {
            tree = ((TinyDocumentImpl)doc).getTree();
            currentDepth = 1;
            prevAtDepth[0] = 0;
            prevAtDepth[1] = -1;
            tree.next[0] = -1;
        }
    }


    /**
     * Open the event stream
     */

    public void open() throws XPathException {
        if (started) {
            // this happens when using an IdentityTransformer
            return;
        }
        if (tree == null) {
            if (sizeParameters==null) {
                tree = new TinyTree();
            } else {
                tree = new TinyTree(sizeParameters[0],
                                        sizeParameters[1], sizeParameters[2], sizeParameters[3]);
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

    public void startDocument (int properties) throws XPathException {
//        if (currentDepth == 0 && tree.numberOfNodes != 0) {
//            System.err.println("**** FOREST DOCUMENT ****");
//        }
        if (started || currentDepth > 0) {
            // this happens when using an IdentityTransformer, or when copying a document node to form
            // the content of an element
            return;
        }
        started = true;

        if (currentRoot==null) {
            // normal case
            currentRoot = new TinyDocumentImpl(tree);
            TinyDocumentImpl doc = (TinyDocumentImpl)currentRoot;
            doc.setSystemId(getSystemId());
            doc.setConfiguration(config);
            //tree.document = doc;
        } else {
            // document node supplied by user
            if (!(currentRoot instanceof TinyDocumentImpl)) {
                throw new DynamicError("Document node supplied is of wrong kind");
            }
            if (currentRoot.hasChildNodes()) {
                throw new DynamicError("Supplied document is not empty");
            }
            //currentRoot.setConfiguration(config);
        }

        currentDepth = 0;
        tree.addDocumentNode((TinyDocumentImpl)currentRoot);
        prevAtDepth[0] = 0;
        prevAtDepth[1] = -1;
        tree.next[0] = -1;

        currentDepth++;

        super.startDocument(0);

    }

    /**
    * Callback interface for SAX: not for application use
    */

    public void endDocument () throws XPathException {
             // System.err.println("TinyBuilder: " + this + " End document");

        if (currentDepth > 1) return;
            // happens when copying a document node as the child of an element

        if (ended) return;  // happens when using an IdentityTransformer
        ended = true;

        prevAtDepth[currentDepth] = -1;



        //super.close();
    }

    public void close() throws XPathException {
        tree.condense();
        super.close();
    }

    /**
    * Notify the start tag of an element
    */

    public void startElement (int nameCode, int typeCode, int locationId, int properties) throws XPathException
    {
//        if (currentDepth == 0 && tree.numberOfNodes != 0) {
//            System.err.println("**** FOREST ELEMENT ****");
//        }
		nodeNr = tree.addNode(Type.ELEMENT, currentDepth, -1, -1, nameCode);

		if (typeCode != -1) {
		    tree.setElementAnnotation(nodeNr, typeCode);
		}

        if (currentDepth == 0) {
            prevAtDepth[0] = 0;
            prevAtDepth[1] = -1;
            tree.next[0] = -1;
            currentRoot = tree.getNode(nodeNr);
        } else {
            int prev = prevAtDepth[currentDepth];
            if (prev > 0) {
                tree.next[prev] = nodeNr;
            }
            tree.next[nodeNr] = prevAtDepth[currentDepth - 1];   // *O* owner pointer in last sibling
            prevAtDepth[currentDepth] = nodeNr;
        }
        currentDepth++;

        if (currentDepth == prevAtDepth.length) {
            int[] p2 = new int[currentDepth*2];
            System.arraycopy(prevAtDepth, 0, p2, 0, currentDepth);
            prevAtDepth = p2;
        }
        prevAtDepth[currentDepth] = -1;

        LocationProvider locator = pipe.getLocationProvider();
        if (locator != null) {
            tree.setSystemId(nodeNr, locator.getSystemId(locationId));
            if (lineNumbering) {
                tree.setLineNumber(nodeNr, locator.getLineNumber(locationId));
            }
        }
    }

    public void namespace(int namespaceCode, int properties) throws XPathException {
        tree.addNamespace( nodeNr, namespaceCode );
    }

    public void attribute(int nameCode, int typeCode, CharSequence value, int locationId, int properties)
    throws XPathException {
        // System.err.println("attribute " + nameCode + "=" + value);

        if ((properties & ReceiverOptions.DISABLE_ESCAPING) != 0) {
            DynamicError err = new DynamicError("Cannot disable output escaping when writing a tree");
            err.setErrorCode("XT1610");
            throw err;
        }

        tree.addAttribute(currentRoot, nodeNr, nameCode, typeCode, value, properties);
    }

    public void startContent() {
        nodeNr++;
    }

    /**
    * Callback interface for SAX: not for application use
    */

    public void endElement () throws XPathException
    {
        //System.err.println("End element ()");

        prevAtDepth[currentDepth] = -1;
        currentDepth--;
    }

    /**
    * Callback interface for SAX: not for application use
    */

    public void characters (CharSequence chars, int locationId, int properties) throws XPathException
    {
        if (chars.length()>0) {
            if ((properties & ReceiverOptions.DISABLE_ESCAPING) != 0) {
                DynamicError err = new DynamicError("Cannot disable output escaping when writing a tree");
                err.setErrorCode("XT1610");
                throw err;
            }
            
            long startPos;
            textBuf.reset();
            textBuf.writeCharSequence( chars );
            try {
                startPos = textStore.length();
                textBuf.output( textStore );
            }
            catch( IOException e ) {
                throw new DynamicError( e );
            }
            
            nodeNr = tree.addNode(Type.TEXT, currentDepth, 
                                  (int)startPos, textBuf.length(),
                                  -1);

            int prev = prevAtDepth[currentDepth];
            if (prev > 0) {
                tree.next[prev] = nodeNr;
            }
            tree.next[nodeNr] = prevAtDepth[currentDepth - 1];   // *O* owner pointer in last sibling
            prevAtDepth[currentDepth] = nodeNr;
        }
    }

    /**
    * Callback interface for SAX: not for application use<BR>
    */

    public void processingInstruction (String piname, CharSequence remainder, int locationId, int properties) throws XPathException
    {
        if (tree.commentBuffer==null) {
            tree.commentBuffer = new FastStringBuffer(200);
        }
        int s = tree.commentBuffer.length();
        tree.commentBuffer.append(remainder.toString());
        int nameCode = namePool.allocate("", "", piname);

        nodeNr = tree.addNode(Type.PROCESSING_INSTRUCTION, currentDepth, s, remainder.length(),
        			 nameCode);

        int prev = prevAtDepth[currentDepth];
        if (prev > 0) {
            tree.next[prev] = nodeNr;
        }
        tree.next[nodeNr] = prevAtDepth[currentDepth - 1];   // *O* owner pointer in last sibling
        prevAtDepth[currentDepth] = nodeNr;

        LocationProvider locator = pipe.getLocationProvider();
        if (locator != null) {
            tree.setSystemId(nodeNr, locator.getSystemId(locationId));
            if (lineNumbering) {
                tree.setLineNumber(nodeNr, locator.getLineNumber(locationId));
            }
        }
    }

    /**
    * Callback interface for SAX: not for application use
    */

    public void comment (CharSequence chars, int locationId, int properties) throws XPathException {
        if (tree.commentBuffer==null) {
            tree.commentBuffer = new FastStringBuffer(200);
        }
        int s = tree.commentBuffer.length();
        tree.commentBuffer.append(chars.toString());
        nodeNr = tree.addNode(Type.COMMENT, currentDepth, s, chars.length(), -1);

        int prev = prevAtDepth[currentDepth];
        if (prev > 0) {
            tree.next[prev] = nodeNr;
        }
        tree.next[nodeNr] = prevAtDepth[currentDepth - 1];   // *O* owner pointer in last sibling
        prevAtDepth[currentDepth] = nodeNr;

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
// The Original Code is: most of this file. 
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by Martin Haye are Copyright (C) Regents of the University 
// of California. All Rights Reserved. 
//
// Contributor(s): Martin Haye. 
//
