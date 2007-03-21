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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

import net.sf.saxon.Configuration;
import net.sf.saxon.om.AxisIterator;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NodeListIterator;
import net.sf.saxon.om.StrippedNode;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.index.Term;
import org.apache.lucene.mark.ContextMarker;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.cdlib.xtf.textEngine.DocHit;
import org.cdlib.xtf.textEngine.QueryProcessor;
import org.cdlib.xtf.textEngine.QueryRequest;
import org.cdlib.xtf.textEngine.QueryResult;
import org.cdlib.xtf.textEngine.Snippet;
import org.cdlib.xtf.util.CharMap;
import org.cdlib.xtf.util.CheckingTokenStream;
import org.cdlib.xtf.util.FastStringReader;
import org.cdlib.xtf.util.FastTokenizer;
import org.cdlib.xtf.util.StructuredStore;
import org.cdlib.xtf.util.Trace;
import org.cdlib.xtf.util.WordMap;

/**
 * <p>SearchTree annotates a lazy-loading tree with TextEngine search results.
 * Many careful gyrations are required to load as little as possible of the
 * lazy tree from disk.</p>
 *
 * <p>This class maintains the illusion that the entire tree has been loaded
 * from disk, carefully searched, each hit annotated, and a list of all the
 * snippets inserted at the top. In reality, this is done on-the-fly as
 * needed, leaving as much as possible on disk.</p>
 *
 * <p>To use SearchTree, simply call the constructor:
 * {@link #SearchTree(Configuration, String, StructuredStore)}, 
 * passing it the key to use for index
 * lookups and the persistent file to load from. Then call the
 * {@link #search(QueryProcessor, QueryRequest)} method to perform the
 * actual search, and use the tree normally. As you access various parts of
 * the tree, they'll be annotated on the fly.
 *
 * @author Martin Haye
 */
public class SearchTree extends LazyDocument 
{
  /** Prefix for this document in the Lucene index */
  String sourceKey;

  /** Map containing all terms used in the query */
  Set termMap;

  /** Set of "stop-words" (i.e. short words like "the", "and", "a", etc.) */
  Set stopSet;

  /** Set of plural words to change from plural to singular */
  WordMap pluralMap;

  /** Set of accented chars to remove diacritics from */
  CharMap accentMap;

  /** Document hit from the text engine, containing snippets for this doc */
  DocHit docHit;

  /** Total number of text hits within this document */
  int nHits;

  /** Array of snippets sorted by descending score */
  Snippet[] hitsByScore;

  /** Array of snippets sorted in document order */
  Snippet[] hitsByLocation;

  /** Mapping from hitsByScore -> hitsByLocation */
  int[] hitRankToNum;

  /** Where to mark terms (all, context only, etc.) */
  int termMode;

  /**
   * True to suppress marking the hits with scores (useful for automated
   * testing where the exact score isn't being tested.
   */
  boolean suppressScores;

  /**
   * All the synthetic nodes added in the tree are assigned a node number
   * &gt;= MARKER_BASE
   */
  static final int MARKER_BASE = 1000000000;

  /**
   * There are several kinds of synthetic nodes; each one takes up a range
   * of node numbers of size MARKER_RANGE.
   */
  static final int MARKER_RANGE = 100000000;

  /**
   * Special node numbers are used to mark an un-loaded sibling so that
   * getNode() can catch them and secretly load the node before anybody
   * notices. These special elements all have node numbers x such that:
   * PREV_SIB_MARKER &lt;= x &lt; PREV_SIB_MARKER+MARKER_RANGE
   */
  static final int PREV_SIB_MARKER = MARKER_BASE + MARKER_RANGE * 1;

  /**
   * Each hit in the document is marked by a &lt;hit&gt; element. These
   * elements all have node numbers x such that:
   * HIT_ELMT_MARKER &lt;= x &lt; HIT_ELMT_MARKER+MARKER_RANGE
   */
  static final int HIT_ELMT_MARKER = MARKER_BASE + MARKER_RANGE * 2;

  /**
   * At the start of the document, the SearchTree adds a synthetic
   * &lt;xtf:snippets&gt; element, and under that creates on demand a
   * &lt;xtf:snippet&gt; element for each snippet. These elements all
   * have node numbers x such that:
   * SNIPPET_MARKER &lt;= x &lt; SNIPPET_MARKER+MARKER_RANGE
   */
  static final int SNIPPET_MARKER = MARKER_BASE + MARKER_RANGE * 3;

  /**
   * Marking a hit in the middle of a string of text requires splitting
   * up real nodes and inserting virtual ones. These virtual nodes all have
   * node numbers x such that:
   * VIRTUAL_MARKER &lt;= x &lt; VIRTUAL_MARKER+MARKER_RANGE
   */
  static final int VIRTUAL_MARKER = MARKER_BASE + MARKER_RANGE * 4;

  /**
   * Keeps track of the node number to assign the next virtual node (see
   * {@link #VIRTUAL_MARKER} for more info.)
   */
  int nextVirtualNum = VIRTUAL_MARKER + 1;

  /**
   * The top-level &lt;xtf:snippet&gt; element.
   */
  SearchElementImpl topSnippetNode;

  /**
   * Snippet, hit, and term elements will all be marked with the XTF
   * namespace, given by this URI: "http://cdlib.org/xtf"
   */
  static final String xtfURI = "http://cdlib.org/xtf";

  /** Namespace code for the XTF namespace */
  int xtfNamespaceCode;

  /** Name fingerprint for &lt;xtf:hit&gt; elements (includes namespace) */
  int hitElementFingerprint;

  /**
   * Name fingerprint for the &lt;xtf:snippet&gt; element
   * (includes namespace)
   */
  int snippetElementFingerprint;

  /** Name-code for all &lt;hit&gt; elements */
  int hitElementCode;

  /** Name-code for all &lt;more&gt; elements */
  int moreElementCode;

  /** Name-code for all &lt;term&gt; elements */
  int termElementCode;

  /** Name-code for all &lt;snippet&gt; elements */
  int snippetElementCode;

  /** Name-code for the &lt;snippets&gt; element */
  int snippetsElementCode;

  /**
   * Name-code for all &lt;xtf:hitCount&gt; attributes
   * (includes namespace)
   */
  int xtfHitCountAttrCode;

  /**
   * Name-code for all &lt;xtf:firstHit&gt; attributes
   * (includes namespace)
   */
  int xtfFirstHitAttrCode;

  /** Name-code for all &lt;hitCount&gt; attributes */
  int hitCountAttrCode;

  /** Name-code for all &lt;totalHitCount&gt; attributes */
  int totalHitCountAttrCode;

  /** Name-code for all &lt;score&gt; attributes */
  int scoreAttrCode;

  /** Name-code for all &lt;rank&gt; attributes */
  int rankAttrCode;

  /** Name-code for all &lt;hitNum&gt; attributes */
  int hitNumAttrCode;

  /** Name-code for all &lt;continues&gt; attributes */
  int continuesAttrCode;

  /** Name-code for all &lt;sectionType&gt; attributes */
  int sectionTypeAttrCode;

  /**
   * Load the tree from a disk file, and get ready to search it. To start
   * the actual search, use the {@link #search(QueryProcessor, QueryRequest)}
   * method.
   */
  public SearchTree(Configuration config, String sourceKey, StructuredStore treeStore)
    throws FileNotFoundException, IOException 
  {
    super(config);
    
    this.sourceKey = sourceKey;

    LazyTreeBuilder builder = new LazyTreeBuilder(config);
    builder.setNamePool(NamePool.getDefaultNamePool());
    builder.load(treeStore, this);

    // We'll be using a special namespace.
    addXTFNamespace();

    // Get all the namecodes we'll be using, so we only have to do it once.
    hitElementCode = getNameCode("hit", true);
    moreElementCode = getNameCode("more", true);
    termElementCode = getNameCode("term", true);
    snippetElementCode = getNameCode("snippet", true);
    snippetsElementCode = getNameCode("snippets", true);

    xtfHitCountAttrCode = getNameCode("hitCount", true); // special
    xtfFirstHitAttrCode = getNameCode("firstHit", true); // special
    hitCountAttrCode = getNameCode("hitCount", false);
    totalHitCountAttrCode = getNameCode("totalHitCount", false);
    scoreAttrCode = getNameCode("score", false);
    rankAttrCode = getNameCode("rank", false);
    hitNumAttrCode = getNameCode("hitNum", false);
    continuesAttrCode = getNameCode("more", false);
    sectionTypeAttrCode = getNameCode("sectionType", false);

    hitElementFingerprint = namePool.getFingerprint(xtfURI, "hit");
    snippetElementFingerprint = namePool.getFingerprint(xtfURI, "snippet");
  } // constructor

  /**
   * Retrieve the proper name code from the name pool.
   */
  private int getNameCode(String name, boolean withNamespace) 
  {
    if (!withNamespace)
      return namePool.allocate("", "", name);

    String prefix = namePool.suggestPrefixForURI(xtfURI);
    if (prefix == null)
      prefix = "xtf";
    return namePool.allocate(prefix, xtfURI, name);
  } // getNameCode

  /**
   * Suppresses score attributes on the snippets. Generally this is useful
   * when running regressions, since the scoring algorithm changes frequently.
   */
  public void suppressScores(boolean flag) {
    suppressScores = flag;
  }

  /**
   * Run the search and save the results for annotating the tree.
   *
   * @param processor     Processor used to run the query
   * @param origReq       Query to run
   *
   * @throws IOException  If anything goes wrong reading from the Lucene
   *                      index or the lazy tree file.
   */
  public void search(QueryProcessor processor, QueryRequest origReq)
    throws IOException 
  {
    // Don't modify the original query request, since it might be in use
    // by another thread at the same time. Rather, make a clone and then
    // modify that with our restricted query.
    //
    QueryRequest req = (QueryRequest)origReq.clone();

    // Make sure the input request is reasonable
    if (req.query instanceof SpanQuery) 
    {
      // -1, or some pos int OK.
      assert ((SpanQuery)req.query).getSpanRecording() != 0;
    }
    assert req.maxDocs != 0; // -1, or som pos int OK.
    assert req.startDoc == 0;

    // Record the real term mode (which we'll respond to directly). Then
    // limit the one in the query to only terms within the context, since
    // marking all terms would be really slow.
    //
    termMode = req.termMode;
    req.termMode = Math.min(req.termMode, ContextMarker.MARK_CONTEXT_TERMS);

    // Add a meta-query that restricts to this document alone. Besides
    // giving us only the hits we want, this also makes the query faster.
    //
    BooleanQuery bq = new BooleanQuery();
    bq.add(new TermQuery(new Term("docInfo", "1")), BooleanClause.Occur.MUST);
    Term t = new Term("key", sourceKey);
    bq.add(new TermQuery(t), BooleanClause.Occur.MUST);
    bq.add(req.query, BooleanClause.Occur.MUST);
    req.query = bq;

    // Run the query and get the results.
    QueryResult result = processor.processRequest(req);
    assert result.docHits.length <= 1;
    docHit = (result.docHits.length > 0) ? result.docHits[0] : null;
    nHits = (docHit != null) ? docHit.nSnippets() : 0;
    hitsByScore = new Snippet[nHits];
    for (int i = 0; i < nHits; i++)
      hitsByScore[i] = docHit.snippet(i, false);

    // We'll need the term map later when we're marking hits.
    if (nHits > 0) 
    {
      termMap = result.textTerms;

      // We also need the stopword set, and the plural map.
      stopSet = result.context.stopSet;
      pluralMap = result.context.pluralMap;
      accentMap = result.context.accentMap;
    }

    // Make a second array of the hits, this time sorted by location.
    hitsByLocation = new Snippet[hitsByScore.length];
    System.arraycopy(hitsByScore, 0, hitsByLocation, 0, hitsByScore.length);
    Arrays.sort(hitsByLocation,
      new Comparator() 
      {
        public int compare(Object o1, Object o2) 
        {
          final Snippet s1 = (Snippet)o1;
          final Snippet s2 = (Snippet)o2;
          int n;
          if ((n = s1.startNode - s2.startNode) != 0)
            return n;
          if ((n = s1.startOffset - s2.startOffset) != 0)
            return n;

          // Debugging help
          @SuppressWarnings("unused")
          String str1 = docHit.snippet(s1.rank, true).text;
          @SuppressWarnings("unused")
          String str2 = docHit.snippet(s2.rank, true).text;
          assert false : "Chunk hits should never overlap!";
          return 0;
        }
      });

    // Extra check to be absolutely sure the hits don't overlap.
    for (int i = 0; i < nHits - 1; i++) 
    {
      Snippet s1 = hitsByLocation[i];
      Snippet s2 = hitsByLocation[i + 1];
      assert s1.endNode >= s1.startNode;
      assert s2.endNode >= s2.startNode;
      assert s2.startNode >= s1.endNode;

      if (s2.startNode == s1.endNode) 
      {
        if (s2.startOffset < s1.endOffset) 
        {
          s1 = docHit.snippet(s1.rank, true);
          s2 = docHit.snippet(s2.rank, true);

          // Debugging help
          @SuppressWarnings("unused")
          String t1 = s1.text;
          @SuppressWarnings("unused")
          String t2 = s2.text;
          assert false;
        }
      }
    }

    // Make a mapping between the two arrays.
    hitRankToNum = new int[nHits];
    for (int i = 0; i < nHits; i++)
      hitRankToNum[hitsByLocation[i].rank] = i;

    // Add special nodes for snippets
    addSnippets();
  } // search()

  /**
   * Get a node by its node number. Handles generating synthetic nodes if
   * necessary.
   *
   * @param num The number of the node to get
   * @return A node, or null if the number is invalid.
   */
  public NodeImpl getNode(int num) 
  {
    // Early out for not-a-node.
    if (num == -1)
      return null;

    // First, check the cache. Note that virtual nodes, once created, 
    // will *always* be in the cache.
    //
    NodeImpl node = checkCache(num);
    if (node != null)
      return node;

    // Catch requests for snippet nodes. They are only created when needed,
    // on-the-fly.
    //
    if (num >= SNIPPET_MARKER && num < SNIPPET_MARKER + MARKER_RANGE)
      return (SearchElementImpl)createSnippetNode(num, true);

    // Catch requests for out-of-context hit nodes.
    if (num >= HIT_ELMT_MARKER && num < HIT_ELMT_MARKER + MARKER_RANGE)
      return getHitElement(num - HIT_ELMT_MARKER);

    // We have to treat requests for the previous sibling as special. This
    // is because the previous node might be a text node which needs to be
    // expanded. We use this detection logic so we can secretly expand it
    // and get the last node from the expansion as the previous sibling of
    // this node.
    //
    int normNum = num;
    if (num >= PREV_SIB_MARKER && num < PREV_SIB_MARKER + MARKER_RANGE) 
    {
      normNum = num - PREV_SIB_MARKER;
      node = checkCache(normNum);
      if (node != null) {
        if (allPermanent)
          nodeCache.put(Integer.valueOf(num), node);
        else
          nodeCache.put(Integer.valueOf(num), new SoftReference(node));
        return node;
      }
    }

    // Okay, load the node from disk. This also puts it into the cache.
    node = super.getNode(normNum);
    if (node == null)
      return null;
    
    if (allPermanent)
      nodeCache.put(Integer.valueOf(normNum), node);
    
    assert node.parentNum >= 0 || node == this;
    assert node.nextSibNum >= -1;
    assert node.prevSibNum >= -1;
    assert node.parentNum < 0 || node.parentNum < MARKER_BASE;
    assert node.nextSibNum < MARKER_BASE;
    assert node.prevSibNum < MARKER_BASE;
    assert node.prevSibNum != node.nextSibNum || node.prevSibNum < 0;

    // We need to differentiate backward references to other nodes.
    if (node.prevSibNum >= 0) {
      node.prevSibNum += PREV_SIB_MARKER;
      assert node.prevSibNum >= 0;
    }

    // Gotta do special stuff to text nodes. And if we're getting the 
    // previous sibling, return the *last* node of the expansion rather 
    // than the first.
    //
    if (node instanceof SearchTextImpl)
      node = expandText((SearchTextImpl)node, normNum != num);

    // All done.
    if (num >= MARKER_BASE)
      nodeCache.put(Integer.valueOf(num), node);
    return node;
  } // getNode()

  /**
   * Add our namespace to the list of namespaces.
   */
  private void addXTFNamespace() 
  {
    assert numberOfNamespaces >= 1 : "must start with root namespace";
    numberOfNamespaces++;

    int[] codes2 = new int[numberOfNamespaces];
    System.arraycopy(namespaceCode, 0, codes2, 0, numberOfNamespaces - 1);
    namespaceCode = codes2;

    int[] parents2 = new int[numberOfNamespaces];
    System.arraycopy(namespaceParent, 0, parents2, 0, numberOfNamespaces - 1);
    namespaceParent = parents2;

    namespaceCode[numberOfNamespaces - 1] = xtfNamespaceCode = namePool.allocateNamespaceCode(
      "xtf",
      xtfURI);
    namespaceParent[numberOfNamespaces - 1] = 1;

    ElementImpl rootKid = (ElementImpl)getNode(1);
    modifyNode(rootKid);
    rootKid.nameSpace = numberOfNamespaces - 1;
  } // addXTFNamespace()

  /**
   * Given a hit number, this method retrieves the synthetic hit node for it.
   */
  private SearchElementImpl getHitElement(int hitNum) 
  {
    // Get the associated text node. This will have the effect of generating
    // the element we need.
    //
    getNode(hitsByLocation[hitNum].startNode);

    // The element we want should now be in the cache.
    SearchElementImpl el = (SearchElementImpl)nodeCache.get(
      Integer.valueOf(HIT_ELMT_MARKER + hitNum));
    assert el != null : "Search element must be created with its text";
    return el;
  } // getHitElement

  /**
   * Create an element node. Derived classes can override this to provide
   * their own element implementation.
   */
  protected @Override NodeImpl createElementNode() {
    return new SearchElementImpl(this);
  }

  /**
   * Create a text node. Derived classes can override this to provide their
   * own text implementation.
   */
  protected @Override NodeImpl createTextNode() {
    return new SearchTextImpl(this);
  }

  /**
   * Checks to see if we've already loaded the node corresponding with the
   * given number. If so, return it, else null.
   */
  protected NodeImpl checkCache(int num) {
    NodeImpl node = super.checkCache(num);
    assert !(node == null && num >= VIRTUAL_MARKER &&
    num < VIRTUAL_MARKER + MARKER_RANGE) : "Missing virtual node";
    return node;
  } // checkCache()

  /**
   * Annotate a text node with search results.
   *
   * @param origNode
   *            The text node as loaded from disk.
   * @param returnLastNode
   *            true to return the last added node, else first.
   * @return The adjusted node.
   */
  private NodeImpl expandText(SearchTextImpl origNode, boolean returnLastNode) 
  {
    // Figure out the first hit that involves this node.
    final int num = origNode.nodeNum;
    int hitNum = findFirstHit(num);

    SearchTextImpl curNode = origNode;
    final String text = curNode.getStringValue();
    final int textLen = text.length();

    Snippet snippet = null;
    int hitStart = -1;
    int hitEnd = -1;
    if (hitNum < nHits) 
    {
      snippet = hitsByLocation[hitNum];
      if (num < snippet.startNode)
        snippet = null;
      else {
        hitStart = (num == snippet.startNode) ? snippet.startOffset : 0;
        hitEnd = (num == snippet.endNode) ? snippet.endOffset : Integer.MAX_VALUE;
      }
    }

    // If we're only marking terms within hits and there are no hits in this
    // node, then we need do nothing more.
    //
    if (termMode < ContextMarker.MARK_ALL_TERMS && hitStart < 0)
      return origNode;

    // Okay, now scan every word. Use a fast tokenizer, since the Standard
    // one is dog-slow. Special case: if the 'check' flag is turned on, we
    // run both tokenizers in parallel and check that they give the exact
    // same tokens.
    //
    boolean check = false;
    TokenStream tokenizer = new FastTokenizer(new FastStringReader(text));
    if (check) {
      TokenStream stdTok = new StandardTokenizer(new StringReader(text));
      tokenizer = new CheckingTokenStream(tokenizer, stdTok);
    }
    tokenizer = new StandardFilter(tokenizer);

    int wordOffset = 0;
    int startChar = 0;
    int endChar = 0;
    boolean inHit = false;

    while (true) 
    {
      // Get the next word.
      Token token;
      try {
        token = tokenizer.next();
      }
      catch (Exception e) {
        assert false : "How can string tokenization fail?!";
        throw new RuntimeException(e);
      }

      // At the start of a hit, skip any leading non-token chars. Don't
      // do that in mid-hit, since it could introduce non-marked gaps
      // at tag boundaries.
      //
      if (token != null && snippet != null && snippet.startNode == num)
        endChar = token.startOffset();

      // Convert the term to lower-case, and depluralize if necessary.
      String mappedTerm = null;
      if (token != null) 
      {
        mappedTerm = token.termText().toLowerCase();
        if (pluralMap != null) {
          String singular = pluralMap.lookup(mappedTerm);
          if (singular != null)
            mappedTerm = singular;
        }
        if (accentMap != null) {
          String unaccented = accentMap.mapWord(mappedTerm);
          if (unaccented != null)
            mappedTerm = unaccented;
        }
      }

      // Are we at the start of a hit?
      if (wordOffset == hitStart) 
      {
        assert snippet.startNode != num || termMap.contains(mappedTerm) : "first hit token must be in search terms";
        assert !inHit;
        inHit = true;

        // Truncate the string in the current text node.
        curNode.setStringValue(text.substring(startChar, endChar));

        // Add the "hit" element
        boolean firstForHit = (snippet.startNode == num);
        boolean lastForHit = (snippet.endNode == num);

        SearchElementImpl el = (SearchElementImpl)createHitElement(firstForHit,
                                                                   lastForHit,
                                                                   hitNum,
                                                                   true); // real not proxy
        linkSibling(curNode, el);

        // Resume inside the new element.
        curNode = addText(el, text.substring(endChar, textLen), true);

        startChar = endChar;
        inHit = true;
      } // if

      // Are we out of words?
      if (token == null)
        break;

      wordOffset++;

      // If the token matches a query term, mark it.
      if (termMap != null &&
          termMap.contains(mappedTerm) &&
          (termMode == ContextMarker.MARK_ALL_TERMS ||
           (termMode >= ContextMarker.MARK_SPAN_TERMS && inHit)) &&
          (inHit || stopSet == null || !stopSet.contains(mappedTerm))) 
      {
        final int soff = token.startOffset();
        final int eoff = token.endOffset();

        // Truncate the string in the current text node.
        curNode.setStringValue(text.substring(startChar, soff));

        // Add the "xtfTerm" element
        SearchElementImpl el = addElement(curNode, termElementCode, 0, false);

        // Put the term text inside it
        addText(el, text.substring(soff, eoff), true);

        // Resume with the text after the term.
        curNode = addText(el, text.substring(eoff, textLen), false);
        startChar = token.endOffset();
      } // if

      endChar = token.endOffset();

      // Are we at the end of a hit? If not, go again.
      if (hitEnd < 0 || wordOffset <= hitEnd)
        continue;
      assert snippet.endNode != num || termMap.contains(mappedTerm) : "last hit token must be a search term";
      assert inHit;
      inHit = false;

      // Truncate the string in the current text node.
      curNode.setStringValue(text.substring(startChar, endChar));

      // Resume outside the "hit" element.
      SearchElementImpl el = (SearchElementImpl)curNode.getParent();
      curNode = addText(el, text.substring(endChar, textLen), false);
      startChar = endChar;
      inHit = false;

      // Try the next hit.
      hitNum++;
      snippet = null;
      hitStart = hitEnd = -1;
      if (hitNum < nHits) 
      {
        snippet = hitsByLocation[hitNum];
        if (num < snippet.startNode)
          snippet = null;
        else {
          hitStart = (num == snippet.startNode) ? snippet.startOffset : 0;
          hitEnd = (num == snippet.endNode) ? snippet.endOffset
                   : Integer.MAX_VALUE;
        } // else
      } // if
    } // while

    // All done!
    if (returnLastNode) {
      if (inHit)
        return (NodeImpl)curNode.getParent();
      return curNode;
    }
    return origNode;
  } // expandText()

  /**
   * Does the work of creating a "hit" element.
   *
   * @param firstForHit   true if this is the first element for the hit
   * @param lastForHit    true if this is the last element for the hit
   * @param hitNum        The hit being referenced
   * @param realNotProxy  true to create a real node, else make a proxy.
   */
  SearchElement createHitElement(boolean firstForHit, boolean lastForHit,
                                 int hitNum, boolean realNotProxy) 
  {
    Snippet snippet = hitsByLocation[hitNum];

    int nameCode = firstForHit ? hitElementCode : moreElementCode;
    int nAttrs = suppressScores ? 3 : 4;
    SearchElement el = realNotProxy
                       ? (SearchElement)new SearchElementImpl(this)
                       : (SearchElement)new ProxyElement(this);
    initElement(el, nameCode, nAttrs);

    // If this is the first element for the hit, give it a special number.
    if (firstForHit)
      el.setNodeNum(HIT_ELMT_MARKER + hitNum);

    // Add the identifying attributes. We add one to the hit number
    // because XSLT generally expects 1-based counting.
    //
    int attrNum = 0;
    if (!suppressScores)
      el.setAttribute(attrNum++,
                      scoreAttrCode,
                      Integer.toString(Math.round(snippet.score * 100)));
    el.setAttribute(attrNum++, rankAttrCode, Integer.toString(snippet.rank + 1));
    el.setAttribute(attrNum++, hitNumAttrCode, Integer.toString(hitNum + 1));
    el.setAttribute(attrNum++, continuesAttrCode, lastForHit ? "no" : "yes");
    assert attrNum == nAttrs;

    // All done!
    return el;
  } // createHitElement()

  /**
   * Create an element as the sibling of another node.
   *
   * @param prev
   *            Node to add sibling to
   * @param elNameCode
   *            Name of the new element
   * @param nAttribs
   *            How many attributes it will have
   * @param addAsChild
   *            true to add as a child of 'prev', false to add as a sibling.
   */
  private SearchElementImpl addElement(NodeImpl prev, int elNameCode,
                                       int nAttribs, boolean addAsChild) 
  {
    // Create the new node and link it in.
    SearchElementImpl el = createElement(elNameCode, nAttribs);
    if (addAsChild)
      linkChild((ParentNodeImpl)prev, el);
    else
      linkSibling(prev, el);
    return el;
  }

  /**
   * Create a text node.
   *
   * @param prev
   *            Node to add sibling to
   * @param text
   *            Initial text string for the new node
   * @param addAsChild
   *            true to add as a child of 'prev', false to add as a sibling.
   */
  private SearchTextImpl addText(NodeImpl prev, String text, boolean addAsChild) 
  {
    // Create the new node and link it in.
    SearchTextImpl textNode = createText(text);
    if (addAsChild)
      linkChild((ParentNodeImpl)prev, textNode);
    else
      linkSibling(prev, textNode);
    return textNode;
  } // addSiblingElement()

  /**
   * Does the work of creating an element, but doesn't link it into the tree.
   *
   * @param elNameCode  The name for the new element
   * @param nAttribs    How many attributes it will have.
   * @return            The new element.
   */
  private SearchElementImpl createElement(int elNameCode, int nAttribs) {
    SearchElementImpl el = new SearchElementImpl(this);
    initElement(el, elNameCode, nAttribs);
    return el;
  } // createElement()

  /**
   * Initialize all the fields of a new element node.
   */
  private void initElement(SearchElement el, int elNameCode, int nAttrs) {
    initNode(el);
    el.allocateAttributes(nAttrs);
    el.setNameCode(elNameCode);
  } // initElement()

  /**
   * Does the work of creating a text node, but doesn't link it into the tree.
   *
   * @param text
   *            The initial text for the node
   * @return The newly created node.
   */
  private SearchTextImpl createText(String text) {
    SearchTextImpl node = new SearchTextImpl(this);
    initNode(node);
    node.setStringValue(text);
    return node;
  } // createText()

  /**
   * Does the work of linking in a new sibling element or text node.
   */
  private void linkSibling(NodeImpl prev, NodeImpl node) 
  {
    // Mark the nodes to modify.
    NodeImpl next = (NodeImpl)prev.getNextSibling();
    modifyNode(prev);
    modifyNode(next);

    // Link it in
    node.parentNum = prev.parentNum;
    node.prevSibNum = prev.nodeNum;
    node.nextSibNum = prev.nextSibNum;
    prev.nextSibNum = node.nodeNum;
    if (next != null)
      next.prevSibNum = node.nodeNum;
  } // linkSibling()

  /**
   * Does the work of linking in a new child element or text node. It will be
   * added as the first child.
   */
  private void linkChild(ParentNodeImpl parent, NodeImpl node) 
  {
    // Mark the node to modify.
    modifyNode(parent);

    // Link it in
    node.parentNum = parent.nodeNum;
    node.prevSibNum = -1;
    node.nextSibNum = parent.childNum;
    parent.childNum = node.nodeNum;
  } // linkChild()

  /**
   * Performs initialization tasks common to text and element nodes.
   */
  private void initNode(SearchNode node) 
  {
    node.setNodeNum(nextVirtualNum);

    if (!(node instanceof ProxyElement))
      nodeCache.put(Integer.valueOf(nextVirtualNum), node);

    nextVirtualNum++;
  } // initNode

  /**
   * Prepares a node for modification. Essentially, makes sure that it will be
   * cached and never reloaded from disk.
   */
  private void modifyNode(NodeImpl node) 
  {
    // Before modifying the node, make sure we hold onto a hard reference
    // (normally the node cache only contains weak references.)
    //
    if (node != null)
      nodeCache.put(Integer.valueOf(node.nodeNum), node);
  } // modifyNode()

  /**
   * Adds the top-level &lt;xtf:snippets&gt; element. If its children are
   * fetched later, they'll be created on the fly.
   */
  private void addSnippets() 
  {
    ElementImpl rootKid = (ElementImpl)getNode(1);
    topSnippetNode = addElement(rootKid, snippetsElementCode, 2, true);
    topSnippetNode.setAttribute(0,
                                totalHitCountAttrCode,
                                (docHit != null)
                                  ? Integer.toString(docHit.totalSnippets()) : "0");
    topSnippetNode.setAttribute(1, hitCountAttrCode, Integer.toString(nHits));

    if (nHits > 0)
      topSnippetNode.childNum = SNIPPET_MARKER + 0;
  } // addSnippets()

  /**
   * Creates an on-the-fly snippet node.
   *
   * @param num  The node number (SNIPPET_MARKER + hit #)
   */
  private SearchElement createSnippetNode(int num, boolean realNotProxy) 
  {
    // Figure out which hit is being referenced
    int hitNum = num - SNIPPET_MARKER;
    Snippet snippet = realNotProxy ? docHit.snippet(hitNum, true)
                      : // we need the text.
                        hitsByScore[hitNum];

    // Make the element, and create its links to other elements.
    int nAttribs = 2 + (snippet.sectionType != null ? 1 : 0) +
                   (suppressScores ? 0 : 1);
    SearchElement snippetElement = realNotProxy
                                   ? (SearchElement)new SearchElementImpl(this)
                                   : (SearchElement)new ProxyElement(this);
    initElement(snippetElement, snippetElementCode, nAttribs);
    snippetElement.setPrevSibNum((hitNum == 0) ? -1 : SNIPPET_MARKER + hitNum -
                                 1);
    snippetElement.setNextSibNum(
      (hitNum == nHits - 1) ? -1 : SNIPPET_MARKER + hitNum + 1);
    snippetElement.setParentNum(topSnippetNode.nodeNum);

    // Give it a special place in the node cache so we can find it again.
    snippetElement.setNodeNum(num);
    if (realNotProxy)
      nodeCache.put(Integer.valueOf(num), snippetElement);

    // Add the score (if not suppressed), hit number, and (if present) 
    // the section type.
    //
    int attrNum = 0;
    if (!suppressScores)
      snippetElement.setAttribute(attrNum++,
                                  scoreAttrCode,
                                  Integer.toString(Math.round(snippet.score * 100)));
    snippetElement.setAttribute(attrNum++,
                                rankAttrCode,
                                Integer.toString(hitNum + 1)); // XSLT expects 1-based
    snippetElement.setAttribute(attrNum++,
                                hitNumAttrCode,
                                Integer.toString(hitRankToNum[hitNum] + 1));
    if (snippet.sectionType != null)
      snippetElement.setAttribute(attrNum++,
                                  sectionTypeAttrCode,
                                  snippet.sectionType);
    assert attrNum == nAttribs;

    // If we're only making a proxy node, don't do the text stuff.
    if (!realNotProxy)
      return snippetElement;

    // Add text before the <hit> marker.
    String text = snippet.text;
    int hitStart = text.indexOf("<hit");
    assert hitStart >= 0 : "missing <hit> in snippet";

    String beforeText = text.substring(0, hitStart);
    NodeImpl prev = breakupText(beforeText,
                                (SearchElementImpl)snippetElement,
                                true);

    // Add the stuff inside the <hit>
    int hitTextStart = text.indexOf('>', hitStart) + 1;
    int hitEnd = text.indexOf("</hit>");
    assert hitEnd >= 0 : "missing </hit> in snippet";

    prev = addElement(prev, hitElementCode, 0, false);
    String hitText = text.substring(hitTextStart, hitEnd);
    breakupText(hitText, prev, true); // as child, don't update prev.

    // And add the text after the <hit>
    int textResume = text.indexOf('>', hitEnd) + 1;
    String afterText = text.substring(textResume);
    prev = breakupText(afterText, prev, false);

    // All done!
    return snippetElement;
  } // createSnippetNode()

  // Precompiled patterns for undoing entity expansion in snippets
  private static final Pattern ampPattern = Pattern.compile("&amp;");
  private static final Pattern ltPattern = Pattern.compile("&lt;");
  private static final Pattern gtPattern = Pattern.compile("&gt;");

  /**
   * Change entities back into normal text (entities are created inside
   * snippets to differentiate them from normal tags.)
   *
   * @param str   String to replace entities within
   * @return      Modified string (or same string if no entities found).
   */
  private String undoEntities(String str) {
    if (str.indexOf("&amp;") >= 0)
      str = ampPattern.matcher(str).replaceAll("&");
    if (str.indexOf("&lt;") >= 0)
      str = ltPattern.matcher(str).replaceAll("<");
    if (str.indexOf("&gt;") >= 0)
      str = gtPattern.matcher(str).replaceAll(">");
    return str;
  } // undoEntities()

  /**
   * Create the appropriate node(s) for text within a snippet, including
   * elements for any marked &lt;term&gt;s.
   *
   * @param text
   *            Text to process, with " &lt;term&gt;" stuff inside it.
   * @param prev
   *            Node to add to
   * @param addAsChild
   *            true to add to prev as a child, else as sibling.
   * @return Last node added.
   */
  private NodeImpl breakupText(String text, NodeImpl prev, boolean addAsChild) 
  {
    int startPos = 0;

    while (true) 
    {
      // Is there markup we need to worry about?
      int markerPos = text.indexOf("<term", startPos);
      if (markerPos < 0)
        markerPos = text.length();

      // Add a text node for everything up to the marker (or the end if
      // there isn't a marker).
      //
      String beforeText = text.substring(startPos, markerPos);
      beforeText = undoEntities(beforeText);
      prev = addText(prev, beforeText, addAsChild);
      addAsChild = false;

      // If no marker, we're done.
      if (markerPos == text.length())
        break;

      // Now insert the term element and its text.
      int termStart = text.indexOf('>', markerPos) + 1;
      int markEnd = text.indexOf("</term>", markerPos);
      String termText = text.substring(termStart, markEnd);
      termText = undoEntities(termText);
      prev = addElement(prev, termElementCode, 0, false);
      addText(prev, termText, true);

      // Go again...
      startPos = text.indexOf('>', markEnd) + 1;
    } // while

    return prev;
  } // breakupText()

  /**
   * Locates the first hit that could conceivably involve this node, that is,
   * the first hit with node number &gt;= 'nodeNum'.
   *
   * @param nodeNum   The node of interest.
   * @return          Index of the hit (might be == nHits, meaning no hit
   *                  could apply.)
   */
  int findFirstHit(final int nodeNum) 
  {
    // Figure out the first hit that involves this node.
    int hitNum = Arrays.binarySearch(hitsByLocation, null,
      new Comparator() 
      {
        public int compare(Object o1, Object o2) {
          return ((Snippet)o1).endNode < nodeNum ? -1 : 1;
        }
      });

    // None? Get out.
    assert hitNum < 0 : "Comparator should never return an exact match";
    hitNum = -hitNum - 1;
    assert hitNum >= 0 && hitNum <= nHits;

    return hitNum;
  } // findFirstHit

  /**
   * Locates the last hit that could conceivably involve this node, that is,
   * the last hit with node number &gt;= 'nodeNum'.
   *
   * @param nodeNum    Node number of the element in question.
   * @return           Index of the hit (might be == nHits, meaning no hit
   *                   could apply.)
   */
  int findLastHit(final int nodeNum) 
  {
    // To figure out the last hit, we need the number of the next sibling, 
    // or sibling of a parent.
    //
    NodeImpl node = getNode(nodeNum);
    while (node != null) 
    {
      NodeImpl sib = (NodeImpl)node.getNextSibling();
      if (sib != null) {
        node = sib;
        break;
      }
      node = (ParentNodeImpl)node.getParent();
    }

    int lastNodeNum = (node == null) ? numberOfNodes : node.nodeNum;
    return findFirstHit(lastNodeNum);
  } // findLastHit()

  /**
   * Writes a disk-based version of an index. Use getIndex() later to read
   * it. This method is overriden to ensure that no virtual nodes ever get
   * written to a disk index.
   *
   * @param indexName Uniquely computed name
   * @param index     HashMap mapping String -> ArrayList[NodeImpl]
   */
  public void putIndex(String indexName, HashMap index)
    throws IOException 
  {
    // Check each node in the index to make sure it's not virtual or
    // otherwise synthetic.
    //
    for (Iterator iter = index.values().iterator(); iter.hasNext();) 
    {
      ArrayList list = (ArrayList)iter.next();

      for (int i = 0; i < list.size(); i++) 
      {
        Item item = (Item)list.get(i);
        NodeImpl node = (item instanceof ProxyElement) ? null
                        : (item instanceof NodeImpl) ? ((NodeImpl)item)
                        : (item instanceof StrippedNode)
                        ? ((NodeImpl)((StrippedNode)item).getUnderlyingNode())
                        : null;
        if (node == null ||
            node.nodeNum >= MARKER_BASE ||
            ((node instanceof ParentNodeImpl) &&
             ((ParentNodeImpl)node).childNum >= MARKER_BASE)) 
        {
          throw new RuntimeException(
            "Error: Key index '" + indexName +
            "' references virtual search-related nodes.\n" +
            "Change the key so it doesn't reference dynamic " +
            "nodes, or else change the key's name to contain " +
            "'dynamic' so it won't be stored.");
        }
      } // for i
    } // for iter

    // All nodes are real... write the index to disk.
    super.putIndex(indexName, index);
  } // putIndex()

  /**
   * Get a list of all elements with a given name. This is implemented
   * as a memo function: the first time it is called for a particular
   * element type, it remembers the result for next time.
   *
   * It's overriden here to take the special case where "xtf:hit" or
   * "xtf:snippet" is specified.
   */
  protected AxisIterator getAllElements(int fingerprint) 
  {
    // Is it one of our special names? If not, do the normal thing.
    if (fingerprint != hitElementFingerprint &&
        fingerprint != snippetElementFingerprint)
      return super.getAllElements(fingerprint);

    Trace.debug(
      "    Building DYNAMIC list of elements named '" +
      namePool.getClarkName(fingerprint) + "'...");

    // Both cases will result in a list of 'nHits' hits.
    ArrayList items = new ArrayList(nHits);

    // Now handle the cases.
    if (fingerprint == snippetElementFingerprint) {
      for (int i = 0; i < nHits; i++)
        items.add(createSnippetNode(i, false));
    } // if
    else 
    {
      assert fingerprint == hitElementFingerprint : "incorrect switching";
      for (int i = 0; i < nHits; i++) {
        Snippet snippet = hitsByLocation[i];
        boolean lastForHit = (snippet.startNode == snippet.endNode);
        items.add(createHitElement(true, lastForHit, i, false));
      }
    } // else

    Trace.debug("done");

    return new NodeListIterator(items);
  } // getAllElements()

  /**
   * DEBUGGING ONLY: Removes parts of the tree that haven't been loaded yet.
   * This can be useful to view the subset of the tree that have actually
   * been accessed.
   *
   * Note that to be useful, {@link #setAllPermanent(boolean)} should be
   * called before accessing the tree to ensure that all nodes referenced
   * are kept in RAM.
   */
  public void pruneUnused() 
  {
    assert allPermanent : "allPermanent should be true for pruneUnused()";

    // To be able to reach every node in memory, we have to make sure all
    // of their parents and previous siblings are also in memory. Use a
    // stack to make sure we get them all (and any others we have to load).
    //
    NodeImpl[] stack = new NodeImpl[(numberOfNodes + nHits) * 3];
    int top = 0;

    for (Iterator iter = nodeCache.values().iterator(); iter.hasNext();) {
      Object ref = iter.next();
      if (ref instanceof NodeImpl)
        stack[top++] = (NodeImpl)ref;
      else
        assert false : "allPermanent should be true for pruneUnused()";
    } // for iter

    // Keep processing until we've finished everything.
    while (top > 0) 
    {
      NodeImpl node = stack[--top];

      // Have we loaded the previous sibling? If not, load it and add it
      // to the stack for processing.
      //
      if (node.prevSibNum >= 0) {
        if (!nodeCache.containsKey(Integer.valueOf(node.prevSibNum)))
          stack[top++] = getNode(node.prevSibNum);
        assert nodeCache.containsKey(Integer.valueOf(node.prevSibNum));
      }

      // Ditto the parent.
      if (node.parentNum != 0)
        stack[top++] = getNode(node.parentNum);
    } // while

    // Cool. We've loaded everything necessary to get to the nodes that
    // were loaded before. Kill off all other links.
    //
    for (Iterator iter = nodeCache.values().iterator(); iter.hasNext();) 
    {
      NodeImpl node = (NodeImpl)iter.next();
      if (node.prevSibNum >= 0 &&
          !nodeCache.containsKey(Integer.valueOf(node.prevSibNum)))
        assert false : "Should have loaded prev sib";
      if (node.nextSibNum >= 0 &&
          !nodeCache.containsKey(Integer.valueOf(node.nextSibNum)))
        node.nextSibNum = -1;
      if (node instanceof ParentNodeImpl) {
        ParentNodeImpl pnode = (ParentNodeImpl)node;
        if (pnode.childNum >= 0 &&
            !nodeCache.containsKey(Integer.valueOf(pnode.childNum)))
          pnode.childNum = -1;
      }
    } // for iter
  } // pruneUnused()
} // class SearchTree
