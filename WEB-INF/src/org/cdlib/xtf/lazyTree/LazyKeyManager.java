package org.cdlib.xtf.lazyTree;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.StrippedNode;
import net.sf.saxon.sort.IntIterator;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.trans.HackedKeyManager;
import net.sf.saxon.trans.KeyDefinition;
import net.sf.saxon.trans.KeyManager;
import net.sf.saxon.trans.XPathException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.zip.CRC32;

import org.apache.lucene.util.Hash64;
import org.cdlib.xtf.util.DiskHashReader;
import org.cdlib.xtf.util.Trace;

/**
  * LazyKeyManager wraps a Saxon KeyManager, but stores keys on disk instead
  * of keeping them in RAM. If the same index is accessed later, it need not
  * be recomputed.
  *
  * KeyManager manages the set of key definitions in a stylesheet, and the indexes
  * associated with these key definitions. It handles xsl:sort-key as well as xsl:key
  * definitions.
  *
  * <p>The memory management in this class is subtle, with extensive use of weak references.
  * The idea is that an index should continue to exist in memory so long as both the compiled
  * stylesheet and the source document exist in memory: if either is removed, the index should
  * go too. The document itself holds no reference to the index. The compiled stylesheet (which
  * owns the KeyManager) holds a weak reference to the index. The index, of course, holds strong
  * references to the nodes in the document. The Controller holds a strong reference to the
  * list of indexes used for each document, so that indexes remain in memory for the duration
  * of a transformation even if the documents themselves are garbage collected.</p>
  *
  * <p>Potentially there is a need for more than one index for a given key name, depending
  * on the primitive type of the value provided to the key() function. An index is built
  * corresponding to the type of the requested value; if subsequently the key() function is
  * called with the same name and a different type of value, then a new index is built.</p>
  *
  * @author Michael H. Kay, Martin Haye
  */
public class LazyKeyManager extends HackedKeyManager 
{
  KeyManager wrapped;
  
  /**
   * create a LazyKeyManager and initialise variables
   */
  public LazyKeyManager(Configuration config, KeyManager toWrap) {
    super(config);
    this.wrapped = toWrap;
  }

  // inherit JavaDoc
  public List getKeyDefinitions(int fingerprint) 
  {
    List ret = (List) keyList.get(fingerprint);
    if (ret != null)
      return ret;
    return wrapped.getKeyDefinitions(fingerprint);
  }
  
  // inherit JavaDoc
  protected synchronized Map buildIndex(int keyNameFingerprint,
                                            BuiltInAtomicType itemType,
                                            Set foundItemTypes, DocumentInfo doc,
                                            XPathContext context)
    throws XPathException 
  {
    // If the key name has 'dynamic' in it, this is a signal that we
    // shouldn't store the index...
    //
    NamePool pool = context.getController().getNamePool();
    LazyDocument document = getDocumentImpl(doc);
    String fingerName = pool.getDisplayName(keyNameFingerprint);
    if (fingerName.indexOf("dynamic") >= 0)
    {
      if (document.getDebug()) {
        Trace.debug(
          "Building dynamic (non-stored) index " +
          new File(doc.getSystemId()).getName() + ": '" + fingerName + ":" +
          itemType + "'");
      }
      Map index = super.buildIndex(keyNameFingerprint, itemType, foundItemTypes, doc, context);
      if (document.getDebug())
        Trace.debug("...done");
      return index;
    }

      // Calculate a string to uniquely describe this index.
    List definitions = getKeyDefinitions(keyNameFingerprint);
    String indexName = calcIndexName(pool, fingerName, definitions, itemType, document.config);

    // Do we already have a stored version of this index?
    DiskHashReader reader = document.getIndex(indexName);
    if (reader != null)
      return new LazyHashMap(document, reader);

    if (document.getDebug()) {
      Trace.info(
        "Building key index " + new File(doc.getSystemId()).getName() +
        ": '" + fingerName + "' {" + indexName + "}...");
    }

    // Alert! since our indexes on disk are always string, convert to
    // string.
    //
    itemType = BuiltInAtomicType.STRING;

    // Use Saxon's method to do the work of computing the nodes
    Map index = super.buildIndex(keyNameFingerprint, 
                                 itemType, 
                                 foundItemTypes, 
                                 doc, 
                                 context);

    // Store it, then return.
    try {
      document.putIndex(indexName, index);
    }
    catch (IOException e) {
      Trace.error("Error storing persistent index! " + e);
      return index;
    }

    if (document.getDebug()) {
      Trace.info("...done");
    }

    return new LazyHashMap(document, document.getIndex(indexName));
  }

  /**
   * Tells whether any keys have been registered.
   */
  public boolean isEmpty() {
    return keyList.size() == 0;
  }

  /**
   * Called after creation of a lazy tree during the index process.
   * Iterates through all registered keys, and builds the associated
   * disk-based key indexes on the given tree.
   *
   * @param doc        The LazyTree to work on.
   * @param context    Context used for name pool, etc.
   *
   * @return int       The number of keys created
   */
  public int createAllKeys(LazyDocument doc, XPathContext context)
    throws XPathException 
  {
    StringValue val = new StringValue("1");

    // Create a key for every definition we have
    int nKeysCreated = 0;
    IntIterator iter = keyList.keyIterator();
    while (iter.hasNext()) {
      int fingerprint = iter.next();

      // Do a fake lookup on this fingerprint, and ignore the results.
      // This will have the effect of building the on-disk hash.
      //
      selectByKey(fingerprint, doc, val, context);
      nKeysCreated++;
    }

    return nKeysCreated;
  } // createAllKeys()

  /**
   * Calculate a CRC code for the given string.
   */
  private String calcCRCString(String s) 
  {
    CRC32 crc = new CRC32();
    crc.reset();
    crc.update(s.getBytes());
    return Long.toHexString(crc.getValue());
  } // calcCRCString

  /**
   * Retrieve the lazy document for the given doc, if possible.
   */
  public static LazyDocument getDocumentImpl(DocumentInfo doc) 
  {
    while (doc instanceof StrippedNode)
      doc = (DocumentInfo)((StrippedNode)doc).getUnderlyingNode();

    if (doc instanceof LazyDocument)
      return (LazyDocument)doc;
    else
      return null;
  } // getDocumentImpl()

  /**
   * Calculates a string name for a given set of xsl:key definitions. This
   * is done very carefully to ensure that the same key will generate the
   * same name, regardless of ephemeral things like particular name codes
   * or other variables that might be different on a different run.
   *
   * @param pool          Name pool used to look up names
   * @param fingerName    Fingerprint of the key
   * @param definitions   List of key definitions
   * @param itemType      The type of value to be stored.
   *
   * @return              A unique string for this xsl:key
   */
  private String calcIndexName(NamePool pool, String fingerName,
                               List definitions, BuiltInAtomicType itemType,
                               Configuration config) 
  {
    StringBuffer sbuf = new StringBuffer();
    sbuf.append("key|" + fingerName);
    for (int k = 0; k < definitions.size(); k++) 
    {
      KeyDefinition def = (KeyDefinition)definitions.get(k);

      // Capture the match pattern.
      String matchStr = def.getMatch().toString();
      sbuf.append("|" + Long.toString(Hash64.hash(matchStr), 16));
      
      // Capture the 'use' expression
      if (def.getUse() instanceof Expression) 
      {
        // Saxon likes to dump debug stuff to a PrintStream, and we need to
        // capture to a buffer.
        //
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bytes);
        
        ((Expression)def.getUse()).display(10, ps, config);
        ps.flush();
        String useStr = bytes.toString();
        sbuf.append("|" + Long.toString(Hash64.hash(useStr), 16));
      }
      else
        sbuf.append("|non-exp");

      // The item type
      sbuf.append("|" + itemType);
    } // for k

    return sbuf.toString();
  } // calcIndexName()

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
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
