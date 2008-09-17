package org.cdlib.xtf.lazyTree;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.StrippedNode;
import net.sf.saxon.pattern.NodeTestPattern;
import net.sf.saxon.pattern.PatternFinder;
import net.sf.saxon.sort.IntIterator;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.value.StringValue;
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

import org.apache.lucene.util.Hash64;
import org.cdlib.xtf.util.DiskHashReader;
import org.cdlib.xtf.util.Trace;

/**
  * LazyKeyManager wraps a Saxon KeyManager, but stores keys on disk instead
  * of keeping them in RAM. If the same index is accessed later, it need not
  * be recomputed.
  *
  * @author Martin Haye
  */
public class LazyKeyManager extends KeyManager 
{
  /** Count of keys actually stored on disk */
  private int nKeysStored;
  
  /**
   * Construct and initialize the manager, grabbing existing key definitions
   * from the previous key manager.
   */
  public LazyKeyManager(Configuration config, KeyManager prevMgr) {
    super(config);
    keyList = prevMgr.keyList;
  }

  // inherit JavaDoc
  public synchronized Map buildIndex(int keyNameFingerprint,
                                     BuiltInAtomicType itemType,
                                     Set foundItemTypes, DocumentInfo doc,
                                     XPathContext context)
    throws XPathException 
  {
    // If the document isn't a lazy tree, just do normal index building
    // (we can't store keys for a non-lazy tree).
    //
    LazyDocument document = getDocumentImpl(doc);
    if (document == null)
      return super.buildIndex(keyNameFingerprint, itemType, foundItemTypes, doc, context);
    
    // If the key name has 'dynamic' in it, this is a signal that we
    // shouldn't store the index...
    //
    NamePool pool = context.getController().getNamePool();
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
    String indexName = calcIndexName(pool, fingerName, definitions, document.config);

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
      nKeysStored++;
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
   * Optimized to use node test directly when possible, for speed.
   */
  protected void constructIndex(DocumentInfo doc, Map index,
                              KeyDefinition keydef,
                              BuiltInAtomicType soughtItemType,
                              Set foundItemTypes, XPathContext context,
                              boolean isFirst)
    throws XPathException 
  {
    PatternFinder match = keydef.getMatch();
    if (match instanceof NodeTestPattern) {
      match = new FastNodeTestPattern(((NodeTestPattern)match).getNodeTest());
      KeyDefinition oldDef = keydef;
      keydef = new KeyDefinition(match, 
                                 oldDef.getUse(),
                                 oldDef.getCollationName(), 
                                 oldDef.getCollation());
    }
    super.constructIndex(doc, index, keydef, soughtItemType,
                         foundItemTypes, context, isFirst);
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
  public synchronized int createAllKeys(LazyDocument doc, XPathContext context)
    throws XPathException 
  {
    StringValue val = new StringValue("1");
    NamePool pool = context.getController().getNamePool();
    
    // In debug mode, output keys being created.
    if (Trace.getOutputLevel() == Trace.debug)
      doc.setDebug(true);

    // Create a key for every definition we have, and count how many actually
    // get stored on disk.
    //
    nKeysStored = 0;
    IntIterator iter = keyList.keyIterator();
    while (iter.hasNext()) {
      int fingerprint = iter.next();
      String fingerName = pool.getDisplayName(fingerprint);
      if (fingerName.indexOf("dynamic") >= 0)
        continue;

      // Do a fake lookup on this fingerprint, and ignore the results.
      // This will have the effect of building the on-disk hash.
      //
      selectByKey(fingerprint, doc, val, context);
    }

    return nKeysStored;
  } // createAllKeys()

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
   * @param config        Associated Saxon configuration
   *
   * @return              A unique string for this xsl:key
   */
  private String calcIndexName(NamePool pool, 
                               String fingerName,
                               List definitions,
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
    } // for k

    return sbuf.toString();
  } // calcIndexName()

}
