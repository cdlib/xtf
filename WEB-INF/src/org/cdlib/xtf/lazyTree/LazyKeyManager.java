package org.cdlib.xtf.lazyTree;

import net.sf.saxon.Configuration;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.ArrayIterator;
import net.sf.saxon.om.ListIterator;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.AxisIterator;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.EmptyIterator;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.SingletonIterator;
import net.sf.saxon.om.StrippedNode;
import net.sf.saxon.pattern.Pattern;
import net.sf.saxon.sort.LocalOrderComparer;
import net.sf.saxon.expr.*;
import net.sf.saxon.instruct.SlotManager;
import net.sf.saxon.pattern.*;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.NumericValue;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.value.Value;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.KeyDefinition;
import net.sf.saxon.trans.KeyManager;
import net.sf.saxon.trans.XPathException;
import javax.xml.transform.TransformerConfigurationException;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.Collator;
import java.text.RuleBasedCollator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;
import java.util.zip.CRC32;
import org.cdlib.xtf.util.DiskHashReader;
import org.cdlib.xtf.util.IntegerValues;
import org.cdlib.xtf.util.PackedByteBuf;
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
public class LazyKeyManager extends KeyManager 
{
  private HashMap keyList; // one entry for each named key; the entry contains

                           // a list of key definitions with that name
  private transient WeakHashMap docIndexes;

  // one entry for each document that is in memory;
  // the entry contains a HashMap mapping the fingerprint of
  // the key name plus the primitive item type
  // to the HashMap that is the actual index
  // of key/value pairs.
  private KeyManager wrapped;

  /**
   * create a LazyKeyManager and initialise variables
   */
  public LazyKeyManager(KeyManager toWrap, Configuration config) {
    super(config);
    this.wrapped = toWrap;
    docIndexes = new WeakHashMap();
  }

  /**
   * Retrieve the Saxon KeyManager being wrapped by this LazyKeyManager
   */
  public KeyManager getUnderlyingManager() {
    return wrapped;
  }

  /**
   * Register a key definition. Note that multiple key definitions with the same name are
   * allowed
   * @param fingerprint Integer representing the name of the key
   * @param keydef The details of the key's definition
   */
  public void setKeyDefinition(int fingerprint, KeyDefinition keydef)
    throws TransformerConfigurationException 
  {
    Integer keykey = IntegerValues.valueOf(fingerprint);
    if (keyList == null)
      keyList = new HashMap();
    ArrayList v = (ArrayList)keyList.get(keykey);
    if (v == null) 
    {
      v = new ArrayList();
      keyList.put(keykey, v);
    }
    else {
      // check the consistency of the key definitions
      String collation = keydef.getCollationName();
      if (collation == null) 
      {
        for (int i = 0; i < v.size(); i++) 
        {
          if (((KeyDefinition)v.get(i)).getCollationName() != null) {
            throw new TransformerConfigurationException(
              "All keys with the same name must use the same collation");
          }
        }
      }
      else {
        for (int i = 0; i < v.size(); i++) 
        {
          if (!collation.equals(((KeyDefinition)v.get(i)).getCollationName())) {
            throw new TransformerConfigurationException(
              "All keys with the same name must use the same collation");
          }
        }
      }
    }
    v.add(keydef);
  }

  /**
   * Get all the key definitions that match a particular fingerprint
   * @param fingerprint The fingerprint of the name of the required key
   * @return The key definition of the named key if there is one, or null otherwise.
   */
  public List getKeyDefinitions(int fingerprint) 
  {
    // Have we seen this one before?
    List list = (ArrayList)keyList.get(IntegerValues.valueOf(fingerprint));
    if (list != null)
      return list;

    // If not, see if the wrapped one has seen it. If not, give up.
    return wrapped.getKeyDefinitions(fingerprint);
  }

  /**
   * Like tracePatOrExp, but makes a small string out of the essence. Just
   * calls tracePatOrExp(), then calculates the CRC value of the result.
   */
  private String getPatOrExpCRC(Object obj, NamePool pool) 
  {
    StringBuffer buf = new StringBuffer(4096);
    HashSet seen = new HashSet(16);
    try {
      tracePatOrExp(obj, seen, pool, buf);
    }
    catch (IllegalAccessException e) {
      assert false : "Should have access to fields";
    }

    CRC32 crc = new CRC32();
    crc.reset();
    String s = buf.toString();
    crc.update(s.getBytes());
    return Long.toHexString(crc.getValue());
  } // getPatternCRC

  /**
   * Forms a string representing the essence of a pattern or expression,
   * or as close as we can get to the essence. Uses Java's reflection
   * mechanisms to trace the object and its contents, and adds them to the
   * buffer.
   */
  private void tracePatOrExp(Object obj, HashSet seen, NamePool pool,
                             StringBuffer buf)
    throws IllegalAccessException 
  {
    // Record the class name.
    Class c = obj.getClass();
    String name = c.getName();
    if (name.startsWith(Pattern.class.getPackage().getName()))
      name = name.substring(Pattern.class.getPackage().getName().length() + 1);
    if (name.startsWith(Expression.class.getPackage().getName()))
      name = name.substring(Expression.class.getPackage().getName().length() +
                            1);
    if (name.startsWith(Value.class.getPackage().getName()))
      name = name.substring(Value.class.getPackage().getName().length() + 1);
    buf.append(name);
    buf.append('(');

    // Trace all the fields of this class and its superclasses.
    while (c != Object.class) 
    {
      Field[] fields = c.getDeclaredFields();
      for (int i = 0; i < fields.length; i++) 
      {
        // Get the field's name and value.
        Field f = fields[i];
        String fieldName = f.getName();
        f.setAccessible(true);
        Object val = f.get(obj);
        int modifiers = f.getModifiers();

        // Skip null values, static fields, and final fields.
        if (val == null)
          continue;
        if ((modifiers & Modifier.STATIC) != 0)
          continue;
        if ((modifiers & Modifier.FINAL) != 0)
          continue;

        // First, handle atomic fields.
        if (fieldName.equalsIgnoreCase("fingerprint") &&
            val instanceof Integer) 
        {
          buf.append(
            fieldName + '=' + pool.getDisplayName(((Integer)val).intValue()) +
            ' ');
          continue;
        }

        if (fieldName.equalsIgnoreCase("nodeType") && val instanceof Integer) {
          buf.append(fieldName + '=' + ((Integer)val).intValue() + ' ');
          continue;
        }

        if (fieldName.equalsIgnoreCase("value") && val instanceof String) {
          buf.append(fieldName + '=' + (String)val + ' ');
          continue;
        }

        // Handle single objects as an array of one.
        Object[] array;
        if (val instanceof Object[])
          array = (Object[])val;
        else {
          array = new Object[1];
          array[0] = val;
        }

        // Now we can ignore the difference between objects and arrays
        for (int j = 0; j < array.length; j++) 
        {
          // Skip classes we're not interested in.
          Object o = array[j];
          if (!(o instanceof Pattern || o instanceof Expression))
            continue;

          // Avoid recursive loops
          if (seen.contains(o))
            continue;
          seen.add(o);

          // Recursively process
          buf.append(fieldName +
                     ((array.length == 1) ? "=" : ("[" + j + "]=")));
          tracePatOrExp(o, seen, pool, buf);
          buf.append(' ');
        } // for j
      } // for i

      // Now try the superclass.
      c = c.getSuperclass();
    } // while

    // All done.
    if (buf.charAt(buf.length() - 1) == ' ')
      buf.setCharAt(buf.length() - 1, ')');
    else
      buf.append(')');
  } // tracePattern

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
                               List definitions, int itemType) 
  {
    StringBuffer sbuf = new StringBuffer();
    sbuf.append("key:" + fingerName);
    for (int k = 0; k < definitions.size(); k++) 
    {
      KeyDefinition def = (KeyDefinition)definitions.get(k);

      // The match pattern
      sbuf.append(":" + getPatOrExpCRC(def.getMatch(), pool));

      // The use expression
      sbuf.append(":" + getPatOrExpCRC(def.getUse(), pool));

      // The item type
      sbuf.append(":" + itemType);

      // Collation
      Collator coll = def.getCollation();
      if (coll != null) {
        int decomp = coll.getDecomposition();
        sbuf.append(
           ":" +
            ((decomp == Collator.NO_DECOMPOSITION) ? "NoDecomp"
           : (decomp == Collator.FULL_DECOMPOSITION) ? "FullDecomp"
           : (decomp == Collator.CANONICAL_DECOMPOSITION) ? "CanonicalDecomp"
           : "UnkDecomp"));
        if (coll instanceof RuleBasedCollator)
          sbuf.append(":" + ((RuleBasedCollator)coll).getRules());
      }
    } // for k

    return sbuf.toString();
  } // calcIndexName()

  /**
   * Build the index for a particular document for a named key.
   *
   * This method is *NOT* synchronized, because synchronization is handled
   * at the level of the caller.
   *
   * @param fingerprint The fingerprint of the name of the required key
   * @param doc The source document in question
   * @param context The dynamic context
   * @return the index in question, either as a DiskHashMap (for persistent
   *         indexes) or as a HashMap (for dynamic indexes). Each maps a key
   *         value onto an ArrayList of nodes.
   */
  private Object buildIndex(int fingerprint, int itemType, DocumentInfo doc,
                            XPathContext context)
    throws XPathException 
  {
    List definitions = getKeyDefinitions(fingerprint);
    if (definitions == null) {
      DynamicError de = new DynamicError(
        "Key " +
        context.getController().getNamePool().getDisplayName(fingerprint) +
        " has not been defined");
      de.setXPathContext(context);
      de.setErrorCode("XT1260");
      throw de;
    }

    // If the key name has 'dynamic' in it, this is a signal that we
    // shouldn't store the index...
    //
    String indexName = null;
    LazyDocument document = getDocumentImpl(doc);
    NamePool pool = context.getController().getNamePool();
    String fingerName = pool.getDisplayName(fingerprint);
    if (fingerName.indexOf("dynamic") < 0) 
    {
      // Calculate a string to uniquely describe this index.
      indexName = calcIndexName(pool, fingerName, definitions, itemType);

      // Do we already have a stored version of this index?
      DiskHashReader reader = document.getIndex(indexName);
      if (reader != null)
        return reader;

      if (document.getDebug()) {
        Trace.info(
          "Building key index " + new File(doc.getSystemId()).getName() +
          ": '" + fingerName + "' {" + indexName + "}...");
      }
    } // if
    else 
    {
      if (document.getDebug()) {
        Trace.debug(
          "Building dynamic (non-stored) index " +
          new File(doc.getSystemId()).getName() + ": '" + fingerName + ":" +
          itemType + "'");
      }
    } // else

    // Okay, time to build a new index.
    HashMap hashMap = new HashMap();
    for (int k = 0; k < definitions.size(); k++) {
      constructIndex(doc,
                     hashMap,
                     (KeyDefinition)definitions.get(k),
                     itemType,
                     context,
                     k == 0);
    } // for k

    // Store it, then return.
    if (fingerName.indexOf("dynamic") < 0) 
    {
      try {
        document.putIndex(indexName, hashMap);
      }
      catch (IOException e) {
        Trace.error("Error storing persistent index! " + e);
        return hashMap;
      }

      if (document.getDebug()) {
        if (fingerName.indexOf("dynamic") < 0)
          Trace.info("...done");
        else
          Trace.debug("...done");
      }

      return document.getIndex(indexName);
    }
    else
      return hashMap;
  }

  /**
  * Process one key definition to add entries to an index
  */
  private void constructIndex(DocumentInfo doc, HashMap index,
                              KeyDefinition keydef, int soughtItemType,
                              XPathContext context, boolean isFirst)
    throws XPathException 
  {
    Pattern match = keydef.getMatch();
    Expression use = keydef.getUse();
    Collator collator = keydef.getCollation();

    NodeInfo curr;
    XPathContextMajor xc = context.newContext();
    xc.setOrigin(keydef);

    // The use expression (or sequence constructor) may contain local variables.
    SlotManager map = keydef.getStackFrameMap();
    if (map != null) {
      xc.openStackFrame(map);
    }

    int nodeType = match.getNodeKind();

    if (nodeType == Type.ATTRIBUTE ||
        nodeType == Type.NODE ||
        nodeType == Type.DOCUMENT) 
    {
      // If the match pattern allows attributes to appear, we must visit them.
      // We also take this path in the pathological case where the pattern can match
      // document nodes.
      SequenceIterator all = doc.iterateAxis(Axis.DESCENDANT_OR_SELF);
      while (true) 
      {
        curr = (NodeInfo)all.next();
        if (curr == null) {
          break;
        }
        if (curr.getNodeKind() == Type.ELEMENT) 
        {
          SequenceIterator atts = curr.iterateAxis(Axis.ATTRIBUTE);
          while (true) 
          {
            NodeInfo att = (NodeInfo)atts.next();
            if (att == null) {
              break;
            }
            if (match.matches(att, xc)) {
              processKeyNode(att,
                             use,
                             soughtItemType,
                             collator,
                             index,
                             xc,
                             isFirst);
            }
          }
          if (nodeType == Type.NODE) 
          {
            // index the element as well as its attributes
            if (match.matches(curr, xc)) {
              processKeyNode(curr,
                             use,
                             soughtItemType,
                             collator,
                             index,
                             xc,
                             isFirst);
            }
          }
        }
        else {
          if (match.matches(curr, xc)) {
            processKeyNode(curr, use, soughtItemType, collator, index, xc,
                           isFirst);
          }
        }
      }
    }
    else {
      SequenceIterator all = doc.iterateAxis(Axis.DESCENDANT,
                                             match.getNodeTest());

      // If the match is a nodetest, we avoid testing it again
      while (true) 
      {
        curr = (NodeInfo)all.next();
        if (curr == null) {
          break;
        }
        if (match instanceof NodeTestPattern || match.matches(curr, xc)) {
          processKeyNode(curr, use, soughtItemType, collator, index, xc, isFirst);
        }
      }
    }

    //if (map != null) {
    //  b.closeStackFrame();
    //}
  }

  /**
  * Process one matching node, adding entries to the index if appropriate
   * @param curr the node being processed
   * @param use the expression used to compute the key values for this node
   * @param soughtItemType the primitive item type of the argument to the key() function that triggered
   * this index to be built
   * @param collation the collation defined in the key definition
   * @param index the index being constructed
   * @param xc the context for evaluating expressions
   * @param isFirst indicates whether this is the first key definition with a given key name (which means
   * no sort of the resulting key entries is required)
  */
  private void processKeyNode(NodeInfo curr, Expression use,
                              int soughtItemType, Collator collation,
                              HashMap index, XPathContext xc, boolean isFirst)
    throws XPathException 
  {
    // Make the node we are testing the context node and the current node,
    // with context position and context size set to 1
    AxisIterator si = SingletonIterator.makeIterator(curr);
    si.next(); // need to position iterator at first node

    xc.setCurrentIterator(si);

    //xc.getController().setCurrentIterator(si);                                        X

    // Evaluate the "use" expression against this context node
    SequenceIterator useval = use.iterate(xc);
    while (true) 
    {
      AtomicValue item = (AtomicValue)useval.next();
      if (item == null) {
        break;
      }
      int actualItemType = item.getItemType().getPrimitiveType();
      if (!Type.isComparable(actualItemType, soughtItemType)) 
      {
        // if the types aren't comparable, simply ignore this key value
        break;
      }
      Object val;

      if (soughtItemType == Type.UNTYPED_ATOMIC) 
      {
        // if the supplied key value is untyped atomic, we build an index using the
        // actual type returned by the use expression
        if (collation == null) {
          val = item.getStringValue();
        }
        else {
          val = collation.getCollationKey(item.getStringValue());
        }
      }
      else if (soughtItemType == Type.STRING) {
        // if the supplied key value is a string, there is no match unless the use expression
        // returns a string or an untyped atomic value
        if (collation == null) {
          val = item.getStringValue();
        }
        else {
          val = collation.getCollationKey(item.getStringValue());
        }
      }
      else {
        // Ignore NaN values
        if (item instanceof NumericValue && ((NumericValue)item).isNaN()) {
          break;
        }
        try 
        {
          val = item.convert(soughtItemType);
        }
        catch (XPathException err) {
          // ignore values that can't be converted to the required type
          break;
        }
      }

      ArrayList nodes = (ArrayList)index.get(val);
      if (nodes == null) 
      {
        // this is the first node with this key value
        nodes = new ArrayList();
        index.put(val, nodes);
        nodes.add(curr);
      }
      else {
        // this is not the first node with this key value.
        // add the node to the list of nodes for this key,
        // unless it's already there
        if (isFirst) 
        {
          // if this is the first index definition that we're processing,
          // then this node must be after all existing nodes in document
          // order, or the same node as the last existing node
          if (nodes.get(nodes.size() - 1) != curr) {
            nodes.add(curr);
          }
        }
        else {
          // otherwise, we need to insert the node at the correct
          // position in document order.
          LocalOrderComparer comparer = LocalOrderComparer.getInstance();
          for (int i = 0; i < nodes.size(); i++) 
          {
            int d = comparer.compare(curr, (NodeInfo)nodes.get(i));
            if (d <= 0) 
            {
              if (d == 0) 
              {
                // node already in list; do nothing
              }
              else {
                // add the node at this position
                nodes.add(i, curr);
              }
              return;
            }

            // else continue round the loop
          }

          // if we're still here, add the new node at the end
          nodes.add(curr);
        }
      }
    }
  }

  /**
  * Get the nodes with a given key value
  * @param fingerprint The fingerprint of the name of the required key
  * @param doc The source document in question
  * @param value The required key value
  * @param context The dynamic context, needed only the first time when the key is being built
  * @return an enumeration of nodes, always in document order
  */
  public SequenceIterator selectByKey(int fingerprint, DocumentInfo doc,
                                      AtomicValue value, XPathContext context)
    throws XPathException 
  {
    if (getDocumentImpl(doc) == null)
      return wrapped.selectByKey(fingerprint, doc, value, context);

    // If the key value is numeric, promote it to a double
    int itemType = value.getItemType().getPrimitiveType();
    if (itemType == StandardNames.XS_INTEGER ||
        itemType == StandardNames.XS_DECIMAL ||
        itemType == StandardNames.XS_FLOAT) 
    {
      itemType = StandardNames.XS_DOUBLE;
      value = value.convert(itemType);
    }

    // Alert! since our indexes on disk are always string, convert to
    // string.
    //
    itemType = Type.STRING;

    Object indexObject = getIndex(doc, fingerprint, itemType);
    if (indexObject instanceof String) 
    {
      // index is under construction
      DynamicError de = new DynamicError("Key definition is circular");
      de.setXPathContext(context);
      de.setErrorCode("XT0640");
      throw de;
    }
    if (indexObject == null) 
    {
      // It's often the case that the same lazy file is being accessed
      // by more than one thread. This is usually not a problem, except
      // when key indexes are being built. We serialize to guarantee
      // that the same key doesn't get built twice.
      //
      String docName = doc.getSystemId();
      synchronized (docName.intern()) 
      {
        indexObject = getIndex(doc, fingerprint, itemType);
        if (indexObject == null) {
          putIndex(doc, fingerprint, itemType, "Under Construction", context);
          indexObject = buildIndex(fingerprint, itemType, doc, context);
          putIndex(doc, fingerprint, itemType, indexObject, context);
        }
      }
    }

    KeyDefinition definition = (KeyDefinition)getKeyDefinitions(fingerprint)
                               .get(0);

    // the itemType and collation will be the same for all keys with the same name
    Collator collation = definition.getCollation();

    Object val;
    if (itemType == Type.STRING || itemType == Type.UNTYPED_ATOMIC) 
    {
      if (collation == null) {
        val = value.getStringValue();
      }
      else {
        val = collation.getCollationKey(value.getStringValue());
      }
    }
    else {
      val = value;
    }

    // Handle non-stored indexes.
    if (indexObject instanceof HashMap) 
    {
      HashMap index = (HashMap)indexObject;
      ArrayList nodes = (ArrayList)index.get(val);
      if (nodes == null) {
        return EmptyIterator.getInstance();
      }
      else {
        return new ListIterator(nodes);
      }
    }

    // Do we have an entry in the index for this key?
    LazyDocument document = getDocumentImpl(doc);
    try 
    {
      DiskHashReader index = (DiskHashReader)indexObject;
      PackedByteBuf buf = index.find(val.toString());
      if (buf == null)
        return EmptyIterator.getInstance();

      int nNodes = buf.readInt();
      Item[] nodes = new Item[nNodes];

      int curNum = 0;
      for (int i = 0; i < nNodes; i++) {
        curNum += buf.readInt();
        nodes[i] = document.getNode(curNum);
      }

      return new ArrayIterator(nodes);
    } // try
    catch (IOException e) {
      assert false : "Error reading from persistent hash";
      DynamicError de = new DynamicError("Error reading from persistent hash");
      de.setXPathContext(context);
      de.setErrorCode("XTF0001");
      throw de;
    }
  }

  /**
   * Tells whether any keys have been registered.
   */
  public boolean isEmpty() {
    return keyList.isEmpty();
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

    // Try every namecode, creating every key we find.
    RecordingNamePool pool = (RecordingNamePool)context.getController()
                             .getNamePool();
    int nKeysCreated = 0;
    for (Iterator iter = pool.fingerprints.iterator(); iter.hasNext();) 
    {
      int fingerprint = ((Integer)iter.next()).intValue();
      if (getKeyDefinitions(fingerprint) == null)
        continue;

      // Do a fake lookup on this fingerprint, and ignore the results.
      // This will have the effect of building the on-disk hash.
      //
      selectByKey(fingerprint, doc, val, context);
      nKeysCreated++;
    }

    return nKeysCreated;
  } // createAllKeys()

  /**
  * Save the index associated with a particular key, a particular item type,
  * and a particular document. This
  * needs to be done in such a way that the index is discarded by the garbage collector
  * if the document is discarded. We therefore use a WeakHashMap indexed on the DocumentInfo,
  * which returns HashMap giving the index for each key fingerprint. This index is itself another
  * HashMap.
  * The methods need to be synchronized because several concurrent transformations (which share
  * the same KeyManager) may be creating indexes for the same or different documents at the same
  * time.
  */
  private synchronized void putIndex(DocumentInfo doc, int keyFingerprint,
                                     int itemType, Object index,
                                     XPathContext context) 
  {
    if (docIndexes == null) 
    {
      // it's transient, so it will be null when reloading a compiled stylesheet
      docIndexes = new WeakHashMap();
    }
    WeakReference indexRef = (WeakReference)docIndexes.get(doc);
    HashMap indexList;
    if (indexRef == null || indexRef.get() == null) 
    {
      indexList = new HashMap();

      // ensure there is a firm reference to the indexList for the duration of a transformation
      context.getController().setUserData(doc, "key-index-list", indexList);
      docIndexes.put(doc, new WeakReference(indexList));
    }
    else {
      indexList = (HashMap)indexRef.get();
    }
    indexList.put(new Long(((long)keyFingerprint) << 32 | itemType), index);
  }

  /**
  * Get the index associated with a particular key, a particular source document,
   * and a particular primitive item type
  */
  private synchronized Object getIndex(DocumentInfo doc, int keyFingerprint,
                                       int itemType) 
  {
    if (docIndexes == null) 
    {
      // it's transient, so it will be null when reloading a compiled stylesheet
      docIndexes = new WeakHashMap();
    }
    WeakReference ref = (WeakReference)docIndexes.get(doc);
    if (ref == null)
      return null;
    HashMap indexList = (HashMap)ref.get();
    if (indexList == null)
      return null;
    return indexList.get(new Long(((long)keyFingerprint) << 32 | itemType));
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
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
