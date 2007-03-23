package net.sf.saxon.trans;

import net.sf.saxon.Configuration;
import net.sf.saxon.Platform;
import net.sf.saxon.expr.*;
import net.sf.saxon.functions.SystemFunction;
import net.sf.saxon.functions.Tokenize;
import net.sf.saxon.instruct.SlotManager;
import net.sf.saxon.om.*;
import net.sf.saxon.om.ListIterator;
import net.sf.saxon.pattern.IdrefTest;
import net.sf.saxon.pattern.PatternFinder;
import net.sf.saxon.sort.*;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.BuiltInType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.*;
import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.util.*;

/**
  * MCH: Hacked to change some methods from "private" to "protected", to allow LazyKeyManager
  *      to use inheritance effectively. Also, HashMap has been changed to the more general
  *      Map.
  *      
  * HackedKeyManager manages the set of key definitions in a stylesheet, and the indexes
  * associated with these key definitions. It handles xsl:sort-key as well as xsl:key
  * definitions.
  *
  * <p>The memory management in this class is subtle, with extensive use of weak references.
  * The idea is that an index should continue to exist in memory so long as both the compiled
  * stylesheet and the source document exist in memory: if either is removed, the index should
  * go too. The document itself holds no reference to the index. The compiled stylesheet (which
  * owns the HackedKeyManager) holds a weak reference to the index. The index, of course, holds strong
  * references to the nodes in the document. The Controller holds a strong reference to the
  * list of indexes used for each document, so that indexes remain in memory for the duration
  * of a transformation even if the documents themselves are garbage collected.</p>
  *
  * <p>Potentially there is a need for more than one index for a given key name, depending
  * on the primitive type of the value provided to the key() function. An index is built
  * corresponding to the type of the requested value; if subsequently the key() function is
  * called with the same name and a different type of value, then a new index is built.</p>
  *
  * <p>For XSLT-defined keys, equality matching follows the rules of the eq operator, which means
  * that untypedAtomic values are treated as strings. In backwards compatibility mode, <i>all</i>
  * values are converted to strings.</p>
 *
 * <p>This class is also used for internal indexes constructed (a) to support the idref() function,
 * and (b) (in Saxon-SA only) to support filter expressions of the form /a/b/c[d=e], where the
 * path expression being filtered must be a single-document context-free path rooted at a document node,
 * where exactly one of d and e must be dependent on the focus, and where certain other conditions apply
 * such as the filter predicate not being positional. The operator in this case may be either "=" or "eq".
 * If it is "eq", then the semantics are very similar to xsl:key indexes, except that use of non-comparable
 * types gives an error rather than a non-match. If the operator is "=", however, then the rules for
 * handling untypedAtomic values are different: these must be converted to the type of the other operand.
 * In this situation the following rules apply. Assume that the predicate is [use=value], where use is
 * dependent on the focus (the indexed value), and value is the sought value.</p>
 *
 * <ul>
 * <li>If value is a type other than untypedAtomic, say T, then we build an index for type T, in which any
 * untypedAtomic values that arise in evaluating "use" are converted to type T. A conversion failure results
 * in an error. A value of a type that is not comparable to T also results in an error.</li>
 * <li>If value is untypedAtomic, then we build an index for every type actually encountered in evaluating
 * the use expression (treating untypedAtomic as string), and then search each of these indexes. (Note that
 * it is not an error if the use expression returns a mixture of say numbers and dates, provided that the
 * sought value is untypedAtomic).</li>
 * </ul>
  *
  * @author Michael H. Kay
  */
public class HackedKeyManager extends KeyManager
{
  protected IntHashMap keyList; // one entry for each named key; the entry contains

                              // a list of key definitions with that name
  private transient WeakHashMap docIndexes;

  // one entry for each document that is in memory;
  // the entry contains a Map mapping the fingerprint of
  // the key name plus the primitive item type
  // to the Map that is the actual index
  // of key/value pairs.

  /**
  * create a HackedKeyManager and initialise variables
  */
  public HackedKeyManager(Configuration config) 
  {
    super(config);
    
    keyList = new IntHashMap(10);
    docIndexes = new WeakHashMap(10);

    // Create a key definition for the idref() function
    registerIdrefKey(config);
  }

  /**
   * An internal key definition is used to support the idref() function. The key definition
   * is equivalent to xsl:key match="element(*, xs:IDREF) | element(*, IDREFS) |
   * attribute(*, xs:IDREF) | attribute(*, IDREFS)" use=".". This method creates this
   * key definition.
   * @param config The configuration. This is needed because the patterns that are
   * generated need access to schema information.
   */
  private void registerIdrefKey(Configuration config) 
  {
    PatternFinder idref = IdrefTest.getInstance();
    Expression eval = new Atomizer(new ContextItemExpression(), config);
    Tokenize use = (Tokenize)SystemFunction.makeSystemFunction("tokenize",
                                                               2,
                                                               config.getNamePool());
    StringLiteral regex = new StringLiteral("\\s");
    Expression[] params = { eval, regex };
    use.setArguments(params);
    KeyDefinition key = new KeyDefinition(idref, use, null, null);
    key.setIndexedItemType(BuiltInAtomicType.STRING);
    try {
      addKeyDefinition(StandardNames.XS_IDREFS, key, config);
    }
    catch (StaticError err) {
      throw new AssertionError(err); // shouldn't happen
    }
  }

  /**
  * Register a key definition. Note that multiple key definitions with the same name are
  * allowed
  * @param fingerprint Integer representing the name of the key
   * @param keydef The details of the key's definition
   * @param config The configuration
   * @throws StaticError if this key definition is inconsistent with existing key definitions having the same name
   */
  public void addKeyDefinition(int fingerprint, KeyDefinition keydef,
                               Configuration config)
    throws StaticError 
  {
    // Happens because of weird order-of-initialization; we cannot create our
    // list before the super constructor goes and adds the idRef key.
    //
    if (keyList == null)
      keyList = new IntHashMap(10);
    
    ArrayList v = (ArrayList)keyList.get(fingerprint);
    if (v == null) 
    {
      v = new ArrayList(3);
      keyList.put(fingerprint, v);
    }
    else {
      // check the consistency of the key definitions
      String collation = keydef.getCollationName();
      if (collation == null) 
      {
        for (int i = 0; i < v.size(); i++) 
        {
          if (((KeyDefinition)v.get(i)).getCollationName() != null) {
            StaticError err = new StaticError(
              "All keys with the same name must use the same collation");
            err.setErrorCode("XTSE1220");
            throw err;
          }
        }
      }
      else {
        for (int i = 0; i < v.size(); i++) 
        {
          if (!collation.equals(((KeyDefinition)v.get(i)).getCollationName())) {
            StaticError err = new StaticError(
              "All keys with the same name must use the same collation");
            err.setErrorCode("XTSE1220");
            throw err;
          }
        }
      }
    }
    v.add(keydef);
    boolean backwardsCompatible = false;
    for (int i = 0; i < v.size(); i++) 
    {
      if (((KeyDefinition)v.get(i)).isBackwardsCompatible()) {
        backwardsCompatible = true;
        break;
      }
    }
    if (backwardsCompatible) 
    {
      // In backwards compatibility mode, convert all the use-expression results to sequences of strings
      for (int i = 0; i < v.size(); i++) 
      {
        KeyDefinition kd = (KeyDefinition)v.get(i);
        kd.setBackwardsCompatible(true);
        if (!kd.getBody().getItemType(config.getTypeHierarchy()).equals(
          BuiltInAtomicType.STRING)) 
        {
          Expression exp = new AtomicSequenceConverter(kd.getBody(),
                                                       BuiltInAtomicType.STRING);
          kd.setBody(exp);
        }
      }
    }
  }

  /**
  * Get all the key definitions that match a particular fingerprint
  * @param fingerprint The fingerprint of the name of the required key
  * @return The list of key definitions of the named key if there are any, or null otherwise.
  * The members of the list will be instances of {@link KeyDefinition}
  */
  public List getKeyDefinitions(int fingerprint) {
    return (List)keyList.get(fingerprint);
  }

  /**
  * Build the index for a particular document for a named key
  * @param keyNameFingerprint The fingerprint of the name of the required key
  * @param itemType the type of the values to be indexed.
  * @param doc The source document in question
  * @param context The dynamic context
  * @return the index in question, as a Map mapping a key value onto a ArrayList of nodes
  */
  protected synchronized Map buildIndex(int keyNameFingerprint,
                                            BuiltInAtomicType itemType,
                                            Set foundItemTypes, DocumentInfo doc,
                                            XPathContext context)
    throws XPathException 
  {
    //explainKeys(context.getConfiguration(), System.out);
    List definitions = getKeyDefinitions(keyNameFingerprint);
    if (definitions == null) {
      DynamicError de = new DynamicError(
        "Key " + context.getNamePool().getDisplayName(keyNameFingerprint) +
        " has not been defined");
      de.setXPathContext(context);
      de.setErrorCode("XTDE1260");
      throw de;
    }

    Map index = new HashMap(100);

    // There may be multiple xsl:key definitions with the same name. Index them all.
    for (int k = 0; k < definitions.size(); k++) {
      constructIndex(doc,
                     index,
                     (KeyDefinition)definitions.get(k),
                     itemType,
                     foundItemTypes,
                     context,
                     k == 0);
    }

    return index;
  }

  /**
  * Process one key definition to add entries to an index
  */
  protected void constructIndex(DocumentInfo doc, Map index,
                              KeyDefinition keydef,
                              BuiltInAtomicType soughtItemType,
                              Set foundItemTypes, XPathContext context,
                              boolean isFirst)
    throws XPathException 
  {
    PatternFinder match = keydef.getMatch();

    //NodeInfo curr;
    XPathContextMajor xc = context.newContext();
    xc.setOrigin(keydef);

    // The use expression (or sequence constructor) may contain local variables.
    SlotManager map = keydef.getStackFrameMap();
    if (map != null) {
      xc.openStackFrame(map);
    }

    SequenceIterator iter = match.selectNodes(doc, xc);
    while (true) 
    {
      Item item = iter.next();
      if (item == null) {
        break;
      }
      processKeyNode((NodeInfo)item,
                     soughtItemType,
                     foundItemTypes,
                     keydef,
                     index,
                     xc,
                     isFirst);
    }
  }

  /**
  * Process one matching node, adding entries to the index if appropriate
   * @param curr the node being processed
   * @param soughtItemType the primitive item type of the argument to the key() function that triggered
   * this index to be built
   * @param keydef the key definition
   * @param index the index being constructed
   * @param xc the context for evaluating expressions
   * @param isFirst indicates whether this is the first key definition with a given key name (which means
   * no sort of the resulting key entries is required)
  */
  private void processKeyNode(NodeInfo curr, BuiltInAtomicType soughtItemType,
                              Set foundItemTypes, KeyDefinition keydef,
                              Map index, XPathContext xc, boolean isFirst)
    throws XPathException 
  {
    // Make the node we are testing the context node,
    // with context position and context size set to 1
    AxisIterator si = SingleNodeIterator.makeIterator(curr);
    si.next(); // need to position iterator at first node

    xc.setCurrentIterator(si);

    Platform platform = null;
    StringCollator collation = keydef.getCollation();
    if (collation != null) {
      platform = Configuration.getPlatform();
    }

    // Evaluate the "use" expression against this context node
    SequenceIterable use = keydef.getUse();
    SequenceIterator useval = use.iterate(xc);
    while (true) 
    {
      AtomicValue item = (AtomicValue)useval.next();
      if (item == null) {
        break;
      }
      BuiltInAtomicType actualItemType = item.getPrimitiveType();
      if (foundItemTypes != null) {
        foundItemTypes.add(actualItemType);
      }
      if (!Type.isComparable(actualItemType, soughtItemType, false)) 
      {
        // the types aren't comparable
        if (keydef.isStrictComparison()) {
          DynamicError de = new DynamicError(
            "Cannot compare " + soughtItemType + " to " + actualItemType +
            " using 'eq'");
          de.setErrorCode("XPTY0004");
          throw de;
        }
        else if (keydef.isConvertUntypedToOther() &&
                   actualItemType.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
          item = item.convert(soughtItemType, xc);
        }
        else if (keydef.isConvertUntypedToOther() &&
                   soughtItemType.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) 
                   {
          // index the item as is
        }
        else {
          // simply ignore this key value
          continue;
        }
      }
      Object val;

      if (soughtItemType.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) 
      {
        // if the supplied key value is untyped atomic, we build an index using the
        // actual type returned by the use expression
        if (collation == null) {
          val = item.getStringValue();
        }
        else {
          val = collation.getCollationKey(item.getStringValue(), platform);
        }
      }
      else if (soughtItemType.equals(BuiltInAtomicType.STRING)) {
        // if the supplied key value is a string, there is no match unless the use expression
        // returns a string or an untyped atomic value
        if (collation == null) {
          val = item.getStringValue();
        }
        else {
          val = collation.getCollationKey(item.getStringValue(), platform);
        }
      }
      else {
        // Ignore NaN values
        if (item instanceof NumericValue && ((NumericValue)item).isNaN()) {
          break;
        }
        try 
        {
          val = item.convert(soughtItemType, xc);
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
        nodes = new ArrayList(4);
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
          // position in document order. This code does an insertion sort:
          // not ideal for performance, but it's very unusual to have more than
          // one key definition for a key.
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
  * @param keyNameFingerprint The fingerprint of the name of the required key
  * @param doc The source document in question
  * @param soughtValue The required key value
  * @param context The dynamic context, needed only the first time when the key is being built
  * @return an iteration of the selected nodes, always in document order with no duplicates
  */
  public SequenceIterator selectByKey(int keyNameFingerprint, DocumentInfo doc,
                                      AtomicValue soughtValue,
                                      XPathContext context)
    throws XPathException 
  {
    //System.err.println("*********** USING KEY ************");
    if (soughtValue == null) {
      return EmptyIterator.getInstance();
    }
    List definitions = getKeyDefinitions(keyNameFingerprint);
    if (definitions == null) {
      throw new DynamicError(
        "Key " + context.getNamePool().getDisplayName(keyNameFingerprint) +
        " has not been defined",
        "XTDE1260",
        context);
    }
    KeyDefinition definition = (KeyDefinition)definitions.get(0);

    // the itemType and collation and BC mode will be the same for all keys with the same name
    StringCollator collation = definition.getCollation();
    Platform platform = null;
    if (collation != null) {
      platform = Configuration.getPlatform();
    }
    boolean backwardsCompatible = definition.isBackwardsCompatible();

    if (backwardsCompatible) 
    {
      // if backwards compatibility is in force, treat all values as strings
      soughtValue = soughtValue.convert(BuiltInAtomicType.STRING, context);
    }
    else {
      // If the key value is numeric, promote it to a double
      // TODO: this could result in two decimals comparing equal because they convert to the same double
      BuiltInAtomicType itemType = soughtValue.getPrimitiveType();
      if (itemType.equals(BuiltInAtomicType.INTEGER) ||
          itemType.equals(BuiltInAtomicType.DECIMAL) ||
          itemType.equals(BuiltInAtomicType.FLOAT)) 
      {
        soughtValue = new DoubleValue(((NumericValue)soughtValue).getDoubleValue());
      }
    }

    // If the sought value is untypedAtomic and the equality matching mode is
    // "convertUntypedToOther", then we construct and search one index for each
    // primitive atomic type that could occur in the result of the "use" expression,
    // and merge the results. We rely on the fact that in this case, there will only
    // be one key definition.
    HashSet foundItemTypes = null;
    AtomicValue value = soughtValue;
    if (soughtValue instanceof UntypedAtomicValue &&
        definition.isConvertUntypedToOther()) 
    {
      // We try string first, but at the same time as building an index for strings,
      // we collect details of the other types actually encountered for the use expression
      BuiltInAtomicType useType = definition.getIndexedItemType();
      if (useType.equals(BuiltInAtomicType.ANY_ATOMIC)) {
        foundItemTypes = new HashSet(10);
        useType = BuiltInAtomicType.STRING;
      }
      value = soughtValue.convert(useType, context);
    }

    // No special action needed for anyURI to string promotion (it just seems to work: tests idky44, 45)
    BuiltInAtomicType itemType = value.getPrimitiveType();
    Object indexObject = getIndex(doc, keyNameFingerprint, itemType);
    if (indexObject instanceof String) 
    {
      // index is under construction
      DynamicError de = new DynamicError("Key definition is circular");
      de.setXPathContext(context);
      de.setErrorCode("XTDE0640");
      throw de;
    }
    Map index = (Map)indexObject;

    // If the index does not yet exist, then create it.
    if (index == null) 
    {
      // Mark the index as being under construction, in case the definition is circular
      putIndex(doc, keyNameFingerprint, itemType, "Under Construction", context);
      index = buildIndex(keyNameFingerprint,
                         itemType,
                         foundItemTypes,
                         doc,
                         context);
      putIndex(doc, keyNameFingerprint, itemType, index, context);
      if (foundItemTypes != null) 
      {
        // build indexes for each item type actually found
        for (Iterator f = foundItemTypes.iterator(); f.hasNext();) 
        {
          BuiltInAtomicType t = (BuiltInAtomicType)f.next();
          if (!t.equals(BuiltInAtomicType.STRING)) {
            putIndex(doc, keyNameFingerprint, t, "Under Construction", context);
            index = buildIndex(keyNameFingerprint, t, null, doc, context);
            putIndex(doc, keyNameFingerprint, t, index, context);
          }
        }
      }
    }

    if (foundItemTypes == null) 
    {
      ArrayList nodes = (ArrayList)index.get(getCollationKey(
                                                             value,
                                                             itemType,
                                                             collation,
                                                             platform));
      if (nodes == null) {
        return EmptyIterator.getInstance();
      }
      else {
        return new ListIterator(nodes);
      }
    }
    else {
      // we need to search the indexes for all possible types, and combine the results.
      SequenceIterator result = null;
      WeakReference ref = (WeakReference)docIndexes.get(doc);
      if (ref != null) 
      {
        Map indexList = (Map)ref.get();
        if (indexList != null) 
        {
          for (Iterator i = indexList.keySet().iterator(); i.hasNext();) 
          {
            long key = ((Long)i.next()).longValue();
            if (((key >> 32) & 0xffffffff) == keyNameFingerprint) 
            {
              int typefp = (int)(key & 0xffffffff);

              BuiltInAtomicType type = (BuiltInAtomicType)BuiltInType.getSchemaType(
                typefp);

              Object indexObject2 = getIndex(doc, keyNameFingerprint, type);
              if (indexObject2 instanceof String) 
              {
                // index is under construction
                DynamicError de = new DynamicError("Key definition is circular");
                de.setXPathContext(context);
                de.setErrorCode("XTDE0640");
                throw de;
              }
              Map index2 = (Map)indexObject2;

              // NOTE: we've been known to encounter a null index2 here, but it doesn't seem possible
              if (index2.size() > 0) 
              {
                value = soughtValue.convert(type, context);
                ArrayList nodes = (ArrayList)index2.get(getCollationKey(
                                                                        value,
                                                                        type,
                                                                        collation,
                                                                        platform));
                if (nodes != null) 
                {
                  if (result == null) {
                    result = new ListIterator(nodes);
                  }
                  else {
                    result = new UnionEnumeration(result,
                                                  new ListIterator(nodes),
                                                  LocalOrderComparer.getInstance());
                  }
                }
              }
            }
          }
        }
      }
      if (result == null) {
        return EmptyIterator.getInstance();
      }
      else {
        return result;
      }
    }
  }

  private static Object getCollationKey(AtomicValue value,
                                        BuiltInAtomicType itemType,
                                        StringCollator collation,
                                        Platform platform) 
  {
    Object val;
    if (itemType.equals(BuiltInAtomicType.STRING) ||
        itemType.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) 
    {
      if (collation == null) {
        val = value.getStringValue();
      }
      else {
        val = collation.getCollationKey(value.getStringValue(), platform);
      }
    }
    else {
      val = value;
    }
    return val;
  }

  /**
  * Save the index associated with a particular key, a particular item type,
  * and a particular document. This
  * needs to be done in such a way that the index is discarded by the garbage collector
  * if the document is discarded. We therefore use a WeakHashMap indexed on the DocumentInfo,
  * which returns Map giving the index for each key fingerprint. This index is itself another
  * Map.
  * The methods need to be synchronized because several concurrent transformations (which share
  * the same HackedKeyManager) may be creating indexes for the same or different documents at the same
  * time.
  */
  private synchronized void putIndex(DocumentInfo doc, int keyFingerprint,
                                     AtomicType itemType, Object index,
                                     XPathContext context) 
  {
    if (docIndexes == null) 
    {
      // it's transient, so it will be null when reloading a compiled stylesheet
      docIndexes = new WeakHashMap(10);
    }
    WeakReference indexRef = (WeakReference)docIndexes.get(doc);
    Map indexList;
    if (indexRef == null || indexRef.get() == null) 
    {
      indexList = new HashMap(10);

      // ensure there is a firm reference to the indexList for the duration of a transformation
      context.getController().setUserData(doc, "key-index-list", indexList);
      docIndexes.put(doc, new WeakReference(indexList));
    }
    else {
      indexList = (Map)indexRef.get();
    }
    indexList.put(
      new Long(((long)keyFingerprint) << 32 | itemType.getFingerprint()),
      index);
  }

  /**
   * Get the index associated with a particular key, a particular source document,
   * and a particular primitive item type
   * @return either an index (as a Map), or the String "under construction", or null
  */
  private synchronized Object getIndex(DocumentInfo doc, int keyFingerprint,
                                       AtomicType itemType) 
  {
    if (docIndexes == null) 
    {
      // it's transient, so it will be null when reloading a compiled stylesheet
      docIndexes = new WeakHashMap(10);
    }
    WeakReference ref = (WeakReference)docIndexes.get(doc);
    if (ref == null)
      return null;
    Map indexList = (Map)ref.get();
    if (indexList == null)
      return null;
    return indexList.get(
      new Long(((long)keyFingerprint) << 32 | itemType.getFingerprint()));
  }

  /**
  * Get the index associated with a particular key, a particular source document,
   * and a particular primitive item type
   * @return either an index (as a Map), or the String "under construction", or null
  */

  //    private synchronized IntSet getIndexedTypes(DocumentInfo doc, int keyFingerprint) {
  //        IntSet set = new IntHashSet(4);  // TODO: since the set of indexed types is very small, and we iterate over it, an array would be better
  //        if (docIndexes==null) {
  //            return set;
  //        }
  //        WeakReference ref = (WeakReference)docIndexes.get(doc);
  //        if (ref==null) return set;
  //        Map indexList = (Map)ref.get();
  //        if (indexList==null) return set;
  //        for (Iterator i=indexList.keySet().iterator(); i.hasNext();) {
  //            long key = ((Long)i.next()).longValue();
  //            if (((key>>32) & 0xffffffff) == keyFingerprint) {
  //                set.add((int)(key & 0xffffffff));
  //            }
  //        }
  //        return set;
  //    }

  /**
   * Diagnostic output explaining the keys
   */
  public void explainKeys(Configuration config, PrintStream out) 
  {
    if (keyList.size() < 2) 
    {
      // don't bother with IDREFS if it's the only index
      return;
    }
    out.println("============ Indexes ======================");
    IntIterator keyIter = keyList.keyIterator();
    while (keyIter.hasNext()) 
    {
      int fp = keyIter.next();
      List list = (List)keyList.get(fp);
      for (int i = 0; i < list.size(); i++) 
      {
        KeyDefinition kd = (KeyDefinition)list.get(i);
        out.println(" Index " + config.getNamePool().getDisplayName(fp));
        out.println("   match = ");
        out.println(kd.getMatch().toString());
        if (kd.getUse() instanceof Expression) {
          out.println("   use = ");
          ((Expression)kd.getUse()).display(10, out, config);
        }
      }
    }
    out.println("===========================================");
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
