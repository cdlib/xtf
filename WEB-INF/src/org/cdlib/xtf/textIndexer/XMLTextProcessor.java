package org.cdlib.xtf.textIndexer;


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
 *
 * Acknowledgements:
 *
 * A significant amount of new and/or modified code in this module
 * was made possible by a grant from the Andrew W. Mellon Foundation,
 * as part of the Melvyl Recommender Project.
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXResult;

import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.PreparedStylesheet;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.ReceivingContentHandler;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.om.NamePool;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.spelt.SpellWriter;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.analysis.*;
import org.apache.lucene.bigram.BigramStopFilter;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.cdlib.xtf.lazyTree.LazyDocument;
import org.cdlib.xtf.lazyTree.LazyKeyManager;
import org.cdlib.xtf.lazyTree.LazyTreeBuilder;
import org.cdlib.xtf.textEngine.IndexUtil;
import org.cdlib.xtf.textEngine.Constants;
import org.cdlib.xtf.textEngine.XtfSearcher;
import org.cdlib.xtf.util.CharMap;
import org.cdlib.xtf.util.FastStringReader;
import org.cdlib.xtf.util.FastTokenizer;
import org.cdlib.xtf.util.Path;
import org.cdlib.xtf.util.StructuredStore;
import org.cdlib.xtf.util.Trace;
import org.cdlib.xtf.util.WordMap;

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

/**
 * This class performs the actual parsing of the XML source text files and
 * generates index information for it. <br><br>
 *
 * The <code>XMLTextProcessor</code> class uses the configuration information
 * recorded in the {@link IndexerConfig} instance passed to add one or more
 * source text documents to the associated Lucene index. The process of indexing
 * an XML source document consists of breaking the document up into small
 * overlapping "chunks" of text, and indexing the individual words encountered
 * in each chunk. <br><br>
 *
 * The reason source documents are split into chunks during indexing is to
 * allow the search engine to load only small pieces of a document when
 * displaying summary "blurbs" for matched text. This significantly lowers the
 * memory requirements to display search results for multiple documents. The
 * reason chunks are overlapped is to allow proximity matches to be found
 * that span adjacent chunks. At this time, the maximum distance that a
 * proximity can be found using this approach is equal to or less than the
 * chunk size used when the text document was indexed. This is because
 * proximity search checks are currently only performed on two adjacent
 * chunks. <br><br>
 *
 * Within a chunk, adjacent words are considered to be one word apart. In
 * Lucene parlance, the <i><b>word bump</b></i> for adjacent words is one.
 * Larger word bump values can be set for sub-sections of a document. Doing
 * so makes proximity matches within a sub-section more relevant than ones that
 * span sections. <br><br>
 *
 * Word bump adjustments are made through the use of attributes added to nodes
 * in the XML source text file. The available word bump attributes are:
 *
 * <blockquote dir=ltr style="MARGIN-RIGHT: 0px">
 *   <b><code>xtf:sentencebump="</code><font color=#0000ff><i>xxx</i></font><code>"</code></b><br>
 *   Set the additional word distance implied by sentence breaks in the
 *   associated node. If not set explicitly, the default sentence bump value
 *   is 5. <br><br>
 *
 *   <b><code>xtf:sectiontype="</code><font color=#0000ff><i>xxx</i></font><code>"</code></b><br>
 *   While this attribute's primary purpose is to assign names to a section of
 *   text, it also forces sections with a different names to start in new, non-
 *   overlapping chunks. The net result is equivalent to placing an "infinite
 *   word bump" between differently named sections, causing proximity searches
 *   to never find a match that spans multiple sections. <br><br>
 *
 *   <b><code>xtf:proximitybreak</code></b><br>
 *   Forces its associated node in the source text to start in a new, non-overlapping
 *   chunk. As with new sections described above, the net result is equivalent to
 *   placing an "infinite word bump" between adjacent sections, causing proximity
 *   searches to never find a match that spans the proximity break. <br><br>
 * </blockquote>
 *
 * In addition to the the word bump modifiers described above, there are two
 * additional non-bump attributes that can be applied to nodes in a source text
 * file:
 *
 * <blockquote dir=ltr style="MARGIN-RIGHT: 0px">
 *   <b><code>xtf:boost="</code><font color=#0000ff><i>xxx</i></font><code>"</code></b><br>
 *   Boosts the ranking of words found in the associated node by multiplying
 *   their base relevance by the number <font color=#0000ff><i>xxx</i></font>.
 *   Normally, a boost value greater than <code>1.0</code> is used to emphasize
 *   the associated text, but values less than <code>1.0</code> can be used as
 *   an "inverse" boost to de-emphasize the relevance of text.  Also, since
 *   Lucene only applies boost values to entire chunks, changing the boost
 *   value for a node causes the text to start in a new, non-overlapping chunk.
 *   <br><br>
 *
 *   <b><code>xtf:noindex</code></b><br>
 *   This attribute when added to a source text node causes the contained text
 *   to not be indexed.
 * </blockquote>
 *
 * Normally, the above mentioned node attributes aren't actually in the source
 * text nodes, but are embedded via the use of an XSL pre-filter before the
 * node is indexed. The XSL pre-filter used is the one defined for the current
 * index in the XML configuration file passed to the <code>TextIndexer</code>.
 * <br><br>
 *
 * For both bump and non-bump attributes, the namespace <code>uri</code> defined
 * by the {@link XMLTextProcessor#xtfUri xtfUri} member must be specified for
 * the <code>XMLTextProcessor</code> to recognize and process them. <br><br>
 *
 */
public class XMLTextProcessor extends DefaultHandler 
{
  /** Initial size for various text accumulation buffers used by this class. */
  private final static int bufStartSize = 16 * 1024;

  /** The number of chunks of XML source text that have been processed. Used
   *  to assign a unique chunk number to each chunk for a document.
   */
  private int chunkCount = 0;

  /** The current XML node we are currently reading source text from. */
  private int curNode = -1;

  /** Number of words encountered so far for the current XML node. Used to
   *  to track the current working position in a node. This value is recorded
   *  in {@link XMLTextProcessor#chunkWordOffset chunkWordOffset} whenever a
   *  new text chunk is started.
   */
  private int nodeWordCount = 0;

  /** The XML node in which the current chunk starts (may be different from the
   *  current node being processed, since chunks may span nodes.)
  */
  private int chunkStartNode = -1;

  /** Number of words accumulated so far for the current chunk. Used to track
   *  when a chunk is "full", and a new chunk needs to be started.
   */
  private int chunkWordCount = 0;

  /** The {@link XMLTextProcessor#nodeWordCount nodeWordCount}
   *  at which the current chunk begins. This value is stored with the chunk
   *  in the index so that the search engine knows where a chunk appears in
   *  the original source text.
   */
  private int chunkWordOffset = 0;

  /** Start node of next chunk. Used to hold the start node for the next
   *  overlapping chunk while the current chunk is being processed. Copied
   *  to {@link XMLTextProcessor#chunkStartNode chunkStartNode} when
   *  processing for the current node is complete.
   *
   */
  private int nextChunkStartNode = -1;

  /** Number of words encountered so far in the next XML node. Used to track
   *  how "full" the next overlapping chunk is while the current chunk is
   *  bing processed. Copied to {@link XMLTextProcessor#chunkWordCount chunkWordCount}
   *  when processing for the current node is complete.
   */
  private int nextChunkWordCount = 0;

  /** The {@link XMLTextProcessor#nodeWordCount nodeWordCount}
   *  at which the next chunk begins. Used to track the offset of the next
   *  overlapping chunk while the current chunk is being processed. Copied
   *  to {@link XMLTextProcessor#chunkWordOffset chunkWordOffset}
   *  when processing for the current node is complete.
   */
  private int nextChunkWordOffset = 0;

  /** The character index in the chunk text accumulation buffer where the next
   *  overlapping chunk beings. Used to track how many characters in the
   *  accumulation buffer need to be saved for the next chunk because of
   *  its overlap with the current chunk.
   */
  private int nextChunkStartIdx = 0;

  /** The size in words of a chunk. */
  private int chunkWordSize = 0;

  /** The number of words of overlap between two adjacent chunks. */
  private int chunkWordOvlp = 0;

  /** The start word offset at which the next overlapping chunk begins. Simply
   *  a precalculated convenience variable based on
   *  {@link XMLTextProcessor#chunkWordSize chunkWordSize} and
   *  {@link XMLTextProcessor#chunkWordOvlp chunkWordOvlp}.
   */
  private int chunkWordOvlpStart = 0;

  /** The set of stop words to remove while indexing. See
   *  {@link IndexInfo#stopWords} for details.
   */
  private Set stopSet = null;

  /** The set of plural words to de-pluralize while indexing. See
   *  {@link IndexInfo#pluralMapPath} for details.
   */
  private WordMap pluralMap = null;

  /** The set of accented chars to remove diacritics from. See
   *  {@link IndexInfo#accentMapPath} for details.
   */
  private CharMap accentMap = null;

  /** Flag indicating that a new chunk needs to be created. Set to <code>true</code>
   *  when a node's section name changes or a <code>proximitybreak</code> attribute
   *  is encountered.
   */
  private boolean forcedChunk = false;

  /** A reference to the configuration information for the current index being
   *  updated. See the {@link IndexInfo} class description for more details.
   */
  private IndexInfo indexInfo;

  /** The base directory from which to resolve relative paths (if any) */
  private String xtfHomePath;

  /** List of files to process. For an explanation of file queuing, see the
   *  {@link XMLTextProcessor#processQueuedTexts() processQueuedTexts()} method.
   */
  private LinkedList fileQueue = new LinkedList();

  /** The location of the XML source text file currently being indexed. For
   *  more information about this structure, see the
   * {@link IndexSource} class.
   */
  private IndexSource curIdxSrc;

  /** The current record being indexed within {@link #curIdxSrc} */
  private IndexRecord curIdxRecord;

  /** Display name of the current file */
  private String curPrettyKey;

  /** The base directory for the current Lucene database. */
  private String indexPath;
  
  /** Object used to construct a "lazy tree" representation of the XML source
   *  file being processed. <br><br>
   *
   *  A "lazy tree" representation of an XML document is a copy of the source
   *  document optimized for quick access and low memory loading. This is
   *  accomplished by breaking up the original XML document into separately
   *  addressable subsections that can be read in individually as needed. For
   *  more detailed information about "lazy tree" organization, see the
   * {@link org.cdlib.xtf.lazyTree.LazyTreeBuilder LazyTreeBuilder} class. <br><br>
   */
  private LazyTreeBuilder lazyBuilder;

  /** Storage for the "lazy tree" */
  private StructuredStore lazyStore;

  /** SAX Handler object for processing XML nodes into a "lazy tree"
   *  representation of the source docuement. For more details, see the
   *  {@link XMLTextProcessor#lazyBuilder lazyBuilder} member. <br><br>
   */
  private Receiver lazyReceiver;

  /** Wrapper for the {@link XMLTextProcessor#lazyReceiver lazyReceiver} object
   *  that translates SAX events to Saxon's internal Receiver API.
   *  {@link XMLTextProcessor#lazyReceiver lazyReceiver} and
   *  {@link XMLTextProcessor#lazyBuilder lazyBuilder} members for more details.
   */
  private ReceivingContentHandler lazyHandler;

  /** Character buffer for accumulating partial text blocks (possibly) passed
   *  in to the {@link XMLTextProcessor#characters(char[],int,int) characters()}
   *  method from the SAX parser.
   */
  private char[] charBuf = new char[1024];

  /** Current end of the {@link XMLTextProcessor#charBuf charBuf} buffer. */
  private int charBufPos = 0;

  /** Flag indicating how deeply nested in a meta-data section the current
   *  text/tag being processed is.
   */
  private int inMeta = 0;

  /** The current meta-field data being processed. */
  private MetaField metaField;

  /** A buffer for accumulating meta-text from the source XML file. */
  private StringBuffer metaBuf = new StringBuffer();

  /** An Lucene index reader object, used in conjunction with the
   * {@link XMLTextProcessor#indexSearcher indexSearcher} to check if
   * the current document needs to be added to, updated in, or
   * removed from the index.
   */
  private IndexReader indexReader;

  /** An Lucene index searcher object, used in conjunction with the
   * {@link XMLTextProcessor#indexReader indexReader} to check if the
   * current document needs to be added to, updated in, or removed
   * from the index.
   */
  private IndexSearcher indexSearcher;

  /** An Lucene index writer object, used to add or update documents
   *  to the index currently opened for writing.
   */
  private IndexWriter indexWriter;

  /** Queues words for spelling dictionary creator */
  private SpellWriter spellWriter;

  /** Keeps track of fields we already know are tokenized */
  private HashSet tokenizedFields;

  /** Maximum number of document deletions to do in a single batch */
  private static final int MAX_DELETION_BATCH = 50;

  /** A buffer containing the "blurbified" text to be stored in the index. For
   *  more about how text is "blurbified", see the
   * {@link XMLTextProcessor#blurbify(StringBuffer,boolean) blurbify()}
   *  method.
   */
  private StringBuffer blurbedText;

  /** A buffer used to accumulate actual words from the source text, along
   *  with "virtual words" implied by any <code>sectiontype</code> and
   *  <code>proximitybreak</code> attributes encountered, as well as various
   *  special markers used to locate where in the XML source text the indexed
   *  text is stored.
   */
  private StringBuffer accumText;

  /** A version of the {@link XMLTextProcessor#accumText accumText} member
   *  where individual "virtual words" have been compacted down into special
   *  offset markers. To learn more about "virtual words", see the
   *  {@link XMLTextProcessor#insertVirtualWords(StringBuffer) insertVirtualWords()}
   *  and
   *  {@link XMLTextProcessor#compactVirtualWords() compactVirtualWords()}
   *  methods.
   */
  private StringBuffer compactedAccumText;

  /** Stack containing the nesting level of the current text being processed.
   *  <br><br>
   *
   *  Since various section types can be nested in an XML source document, a
   *  stack needs to be maintained of the current nesting depth and order, so
   *  that previous section types can be restored when the end of the active
   *  section type is encountered. See the {@link SectionInfoStack} class for
   *  more about section nesting.
   */
  private SectionInfoStack section;

  /** The namespace string used to identify attributes that must be processed
   *  by the <code>XMLTextProcessor</code> class. <br><br>
   *
   *  Indexer specific attributes are usuall inserted into XML source text
   *  elements by way of an XSL pre-filter. For these pre-filter attributes
   *  to be recognized by the <code>XMLTextProcessor</code>, this string
   *  ({@value}) must be set as the attributes <code>uri</code>. To learn more
   *  about pre-filter attributes, see the {@link XMLTextProcessor} class
   *  description.
   */
  private static final String xtfUri = "http://cdlib.org/xtf";

  ////////////////////////////////////////////////////////////////////////////

  /**
   * Open a TextIndexer (Lucene) index for reading or writing. <br><br>
   *
   * The primary purpose of this method is to open the index identified by the
   * <code>cfgInfo</code> for reading and searching. Index reading and searching
   * operations are used to clean, cull, or optimize an index. Opening an index
   * for writing is performed by the method
   * {@link XMLTextProcessor#openIdxForWriting() openIdxForWriting()}
   * only when the index is being updated with new document information. <br><br>
   *
   * @param  homePath Path from which to resolve relative path names.<br>
   *
   * @param  idxInfo  A config structure containing information about the index
   *                  to open. <br>
   *
   * @param  clean    true to truncate any existing index; false to add to it.
   *                  <br><br>
   *
   * @.notes
   *   This method will create an index if it doesn't exist, or truncate an index
   *   that does exist if the <code>clean</code> flag is set in the
   *   <code>cfgInfo</code> structure. <br><br>
   *
   *   This method makes a private internal reference
   *   {@link XMLTextProcessor#indexInfo indexInfo}
   *   to the passed configuration structure for use by other methods in this
   *   class. <br><br>
   *
   * @throws
   *   IOException  Any I/O exceptions that occurred during the opening,
   *                creation, or truncation of the Lucene index. <br><br>
   *
   */
  public void open(String homePath, IndexInfo idxInfo, boolean clean)
    throws IOException 
  {
    fileQueue = new LinkedList();
    
    try 
    {
      // Get a reference to the passed in configuration that all the
      // methods can access.
      //
      this.indexInfo = idxInfo;
      this.xtfHomePath = homePath;

      // Determine where the index database is located.
      indexPath = getIndexPath();

      // Determine the set of stop words to remove (if any)
      if (indexInfo.stopWords != null)
        stopSet = BigramStopFilter.makeStopSet(indexInfo.stopWords);

      // If we were told to create a clean index...
      if (clean) 
      {
        // Create the index db Path in case it doesn't exist yet.
        Path.createPath(indexPath);

        // And then create the index.
        createIndex(indexInfo);
      }

      // Otherwise...
      else 
      {
        // If it doesn't exist yet, create the index db Path. If it 
        // does exist, this call will do nothing.
        //
        Path.createPath(indexPath);

        // Get a Lucene style directory.
        FSDirectory idxDir = FSDirectory.getDirectory(indexPath);

        // If an index doesn't exist there, create it.
        if (!IndexReader.indexExists(idxDir))
          createIndex(indexInfo);
      } // else( !clean )

      // Try to open the index for reading and searching.
      openIdxForReading();

      // Locate the index information document
      Hits match = indexSearcher.search(new TermQuery(new Term("indexInfo", "1")));

      // If we can't find it, then this index is either corrupt or
      // very old. Fail in either case.
      //
      if (match.length() == 0)
        throw new RuntimeException("Index missing indexInfo");

      Document doc = match.doc(0);
      
      // Ensure that the version is compatible.
      String indexVersion = doc.get("xtfIndexVersion");
      if (indexVersion == null)
        indexVersion = "1.0";
      if (indexVersion.compareTo(TextIndexer.REQUIRED_VERSION) < 0) {
        throw new RuntimeException(
          "Incompatible index version " + indexVersion + "; require at least " + 
          TextIndexer.REQUIRED_VERSION + "... consider re-indexing with '-clean'.");
      }

      // Ensure that the chunk size and overlap are the same.
      if (Integer.parseInt(doc.get("chunkSize")) != indexInfo.getChunkSize()) {
        throw new RuntimeException(
          "Index chunk size (" + doc.get("chunkSize") +
          ") doesn't match config (" + indexInfo.getChunkSize() + ")");
      }

      if (Integer.parseInt(doc.get("chunkOvlp")) != indexInfo.getChunkOvlp()) {
        throw new RuntimeException(
          "Index chunk overlap (" + doc.get("chunkOvlp") +
          ") doesn't match config (" + indexInfo.getChunkOvlp() + ")");
      }

      // Ensure that the stop-word settings are the same.
      String stopWords = indexInfo.stopWords;
      if (stopWords == null)
        stopWords = "";
      if (!doc.get("stopWords").equals(stopWords)) {
        throw new RuntimeException(
          "Index stop words (" + doc.get("stopWords") +
          ") doesn't match config (" + indexInfo.stopWords + ")");
      }

      // Read in the plural map, if there is one.
      String pluralMapName = doc.get("pluralMap");
      if (pluralMapName != null && pluralMapName.length() > 0) 
      {
        File pluralMapFile = new File(
          Path.normalizePath(indexPath + pluralMapName));
        InputStream stream = new FileInputStream(pluralMapFile);

        if (pluralMapName.endsWith(".gz"))
          stream = new GZIPInputStream(stream);

        pluralMap = new WordMap(stream);
      }

      // Read in the accent map, if there is one.
      String accentMapName = doc.get("accentMap");
      if (accentMapName != null && accentMapName.length() > 0) 
      {
        File accentMapFile = new File(
          Path.normalizePath(indexPath + accentMapName));
        InputStream stream = new FileInputStream(accentMapFile);

        if (accentMapName.endsWith(".gz"))
          stream = new GZIPInputStream(stream);

        accentMap = new CharMap(stream);
      }

      // Read in the the list of all the tokenized fields (if any).
      tokenizedFields = XtfSearcher.readTokenizedFields(indexPath, indexReader);
    } // try

    catch (IOException e) 
    {
      // Log the error caught.
      Trace.tab();
      Trace.error("*** IOException Opening or Creating Index: " + e);
      Trace.untab();

      // Shut down any open index files.
      close();

      // And pass the exception on.
      throw e;
    } // catch( IOException e )
  } // open()

  ////////////////////////////////////////////////////////////////////////////

  /**
   * Close the Lucene index. <br><br>
   *
   * This method closes the current open Lucene index (if any.) <br><br>
   *
   * @.notes
   *   This method closes any <code>indexReader</code>, <code>indexWriter</code>
   *   or <code>indexSearcher</code> objects open for the current Lucene index.
   *   <br><br>
   *
   * @throws
   *   IOException  Any I/O exceptions that occurred during the closing,
   *                of the Lucene index. <br><br>
   *
   */
  public void close()
    throws IOException 
  {
    if (spellWriter != null)
      spellWriter.close();
    if (indexWriter != null)
      indexWriter.close();
    if (indexSearcher != null)
      indexSearcher.close();
    if (indexReader != null)
      indexReader.close();

    spellWriter = null;
    indexWriter = null;
    indexSearcher = null;
    indexReader = null;
  } // close() 

  ////////////////////////////////////////////////////////////////////////////

  /**
   * Utility function to create a new Lucene index database for reading or
   * searching. <br><br>
   *
   * This method is used by the {@link XMLTextProcessor#createIndex(IndexInfo) createIndex() }
   * method to create a new or clean index for reading and searching. <br><br>
   *
   * @.notes
   *   This method creates the Lucene database for the index, and then adds
   *   an "index info chunk" that identifies the chunk size and overlap used.
   *   This information is required by the search engine to correctly detect
   *   and hilight proximity search results. <br><br>
   *
   * @throws
   *   IOException  Any I/O exceptions that occurred during the deletion of
   *                a previous Lucene database or during the creation of the
   *                new index currently specified by the internal
   *                {@link XMLTextProcessor#indexInfo indexInfo}
   *                structure. <br><br>
   *
   */
  private void createIndex(IndexInfo indexInfo)
    throws IOException 
  {
    try 
    {
      // Delete the old directory.
      Path.deleteDir(new File(indexPath));

      // First, make the index.
      indexWriter = new IndexWriter(indexPath,
                                    new XTFTextAnalyzer(
                                                        stopSet,
                                                        pluralMap,
                                                        accentMap),
                                    true);

      // Then add the index info chunk to it.
      Document doc = new Document();
      doc.add(new Field("indexInfo", "1", Field.Store.YES, Field.Index.UN_TOKENIZED));
      doc.add(new Field("xtfIndexVersion", TextIndexer.CURRENT_VERSION, Field.Store.YES, Field.Index.NO));
      doc.add(new Field("chunkSize", indexInfo.getChunkSizeStr(), Field.Store.YES, Field.Index.NO));
      doc.add(new Field("chunkOvlp", indexInfo.getChunkOvlpStr(), Field.Store.YES, Field.Index.NO));

      // If a plural map was specified, copy it to the index directory.
      if (indexInfo.pluralMapPath != null &&
          indexInfo.pluralMapPath.length() > 0) 
      {
        String fileName = new File(indexInfo.pluralMapPath).getName();
        String sourcePath = Path.normalizeFileName(
          xtfHomePath + indexInfo.pluralMapPath);
        String targetPath = Path.normalizeFileName(indexPath + fileName);
        Path.copyFile(new File(sourcePath), new File(targetPath));
        doc.add(new Field("pluralMap", fileName, Field.Store.YES, Field.Index.NO));
      }

      // If an accent map was specified, copy it to the index directory.
      if (indexInfo.accentMapPath != null &&
          indexInfo.accentMapPath.length() > 0) 
      {
        String fileName = new File(indexInfo.accentMapPath).getName();
        String sourcePath = Path.normalizeFileName(
          xtfHomePath + indexInfo.accentMapPath);
        String targetPath = Path.normalizeFileName(indexPath + fileName);
        Path.copyFile(new File(sourcePath), new File(targetPath));
        doc.add(new Field("accentMap", fileName, Field.Store.YES, Field.Index.NO));
      }

      // Copy the stopwords to the index
      String stopWords = indexInfo.stopWords;
      if (stopWords == null)
        stopWords = "";
      doc.add(new Field("stopWords", stopWords, Field.Store.YES, Field.Index.NO));
      indexWriter.addDocument(doc);
    } // try

    finally 
    {
      // Finish up.
      if (indexWriter != null) {
        indexWriter.close();
        indexWriter = null;
      }
    } // finally
  } // createIndex()

  ////////////////////////////////////////////////////////////////////////////

  /** Check and conditionally queue a source text file for
   *  (re)indexing. <br><br>
   *
   *  This method first checks if the given source file is already in the
   *  index. If not, it adds it to a queue of files to be (re)indexed.
   *
   *  @param idxSrc  The source to add to the queue of sources to be
   *                 indexed/reindexed. <br><br>
   *
   *  @.notes
   *    For more about why source text files are queued, see the
   *    {@link XMLTextProcessor#processQueuedTexts() processQueuedTexts()}
   *    method. <br><br>
   *
   */
  public void checkAndQueueText(IndexSource idxSrc)
    throws ParserConfigurationException, SAXException, IOException 
  {
    // Check the status of this file. If the index is already up to date, 
    // we're done.
    //
    int ret = checkFile(idxSrc);
    if (ret == 1)
      return;

    // Otherwise, queue it to be indexed. If an older version is already in
    // the index, make sure it will get deleted before re-indexing it.
    //
    boolean deleteFirst = (ret == 2);
    queueText(idxSrc, deleteFirst);
  } // checkAndQueueText()

  ////////////////////////////////////////////////////////////////////////////

  /** Queue a source text file for indexing. Old chunks with the same
   *  key will not be deleted first, so this method should only be used
   *  for new texts, or to append chunks for an existing text. <br><br>
   *
   *  @param idxSrc  The data source to add to the queue of sources to be
   *                 indexed/reindexed. <br><br>
   *
   *  @.notes
   *    For more about why source text files are queued, see the
   *    {@link XMLTextProcessor#processQueuedTexts() processQueuedTexts()}
   *    method. <br><br>
   *
   */
  public void queueText(IndexSource idxSrc) {
    queueText(idxSrc, false);
  } // queueText( IndexSource )

  ////////////////////////////////////////////////////////////////////////////

  /** Queue a source text file for (re)indexing. <br><br>
   *
   *  @param srcInfo  The source XML text file to add to the queue of
   *                  files to be indexed/reindexed. <br><br>
   *
   *  @.notes
   *    For more about why source text files are queued, see the
   *    {@link XMLTextProcessor#processQueuedTexts() processQueuedTexts()}
   *    method. <br><br>
   *
   */
  public void queueText(IndexSource srcInfo, boolean deleteFirst) {
    fileQueue.add(new FileQueueEntry(srcInfo, deleteFirst));
  } // queueText( IndexSource, boolean )

  ////////////////////////////////////////////////////////////////////////////

  /** Find out how many texts have been queued up using
   *  {@link #queueText(IndexSource, boolean)} but not yet processed by
   *  {@link #processQueuedTexts()}.
   */
  public int getQueueSize() {
    return fileQueue.size();
  } // getQueueSize()

  ////////////////////////////////////////////////////////////////////////////

  /** Remove a single document from the index.<br><br>
   *
   *  @param srcFile    The original XML source file, used to calculate the
   *                    location of the corresponding *.lazy file to delete.
   *                    If null, this step is skipped.
   *
   *  @param key        The key associated with the document in the index.
   *
   *  @return           true if a document was found and removed, false if
   *                    no match was found.
   *
   */
  public boolean removeSingleDoc(File srcFile, String key)
    throws ParserConfigurationException, SAXException, IOException 
  {
    // Oddly, we need an IndexReader to delete things from a Lucene index.
    // Make sure we've got one.
    //
    openIdxForReading();

    // If any old version of this document exists, delete it.
    int nDeleted = indexReader.deleteDocuments(new Term("key", key));

    // If there might be a lazy file...
    if (srcFile != null) 
    {
      // Delete the old lazy file, if any. Might as well delete any
      // empty parent directories as well.
      //
      File lazyFile = IndexUtil.calcLazyPath(new File(xtfHomePath),
                                             indexInfo,
                                             srcFile,
                                             false);
      Path.deletePath(lazyFile.toString());
    }

    // Let the caller know if we succeeded in deleting the document's chunks.
    return nDeleted > 0;
  } // removeSingleDoc()

  ////////////////////////////////////////////////////////////////////////////

  /** Checks if a given document exists in the index.<br><br>
   *
   *  @param key        The key associated with the document in the index.
   *
   *  @return           true if a document was found, false if not.
   *
   */
  public boolean docExists(String key)
    throws ParserConfigurationException, SAXException, IOException 
  {
    // We need to find the docInfo chunk that contains the specified
    // file. So construct a boolean query looking for a chunk with 
    // a "docInfo" field AND a "key" field containing the specified
    // source file key.
    //
    BooleanQuery query = new BooleanQuery();
    Term docInfo = new Term("docInfo", "1");
    Term srcPath = new Term("key", key);
    query.add(new TermQuery(docInfo), BooleanClause.Occur.MUST);
    query.add(new TermQuery(srcPath), BooleanClause.Occur.MUST);

    // Use the query to see if the document is in the index..
    Hits match = indexSearcher.search(query);

    // Let the caller know if we found a match.
    return match.length() > 0;
  } // docExists()

  ////////////////////////////////////////////////////////////////////////////

  /** If the first entry in the file queue requires deletion, we start up
   *  a batch delete up to {@link #MAX_DELETION_BATCH} deletions. We batch
   *  these up because in Lucene, you can only delete with an IndexReader.
   *  It costs time to close our IndexWriter, open an IndexReader for
   *  the deletions, and then reopen the IndexWriter.
   *
   *  @throws
   *    IOException   Any I/O exceptions encountered when reading the source
   *                  text file or writing to the Lucene index. <br><br>
   */
  public void batchDelete()
    throws IOException 
  {
    // If the first queue entry doesn't need deletion, don't do anything.
    if (fileQueue.isEmpty() ||
        !((FileQueueEntry)fileQueue.getFirst()).deleteFirst)
      return;

    // Okay, we need an index reader for the deletions. This has the effect
    // of closing the index writer, but it will be reopened after the 
    // deletions.
    //
    openIdxForReading();

    // Let's do it.
    int batchSize = 0;
    Iterator iter = fileQueue.iterator();
    while (iter.hasNext() && batchSize < MAX_DELETION_BATCH) 
    {
      FileQueueEntry ent = (FileQueueEntry)iter.next();
      batchSize++;

      // Skip entries that don't need deleting.
      if (!ent.deleteFirst)
        continue;

      // Okay, delete chunks from the old document, and clear the flag.
      indexReader.deleteDocuments(new Term("key", ent.idxSrc.key()));
      ent.deleteFirst = false;
    }
  } // public batchDelete()

  ////////////////////////////////////////////////////////////////////////////

  /** Process the list of files queued for indexing or reindexing. <br><br>
   *
   *  This method iterates through the list of queued source text files,
   *  (re)indexing the files as needed. <br><br>
   *
   *  @throws
   *    IOException   Any I/O exceptions encountered when reading the source
   *                  text file or writing to the Lucene index. <br><br>
   *
   *  @.notes
   *    Originally, the <code>XMLTextProcessor</code> opened the Lucene
   *    database, (re)indexed the source file, and then closed the database
   *    for each XML file encountered in the source tree. Unfortunately,
   *    opening and closing the Lucene database is a fairly time consuming
   *    operation, and doing so for each file made the time to index an entire
   *    source tree much higher than it had to be. So to minimize the open/close
   *    overhead, the <code>XMLTextProcessor</code> was changed to traverse
   *    the source tree first and collect all the XML filenames it found into a
   *    processing queue. Once the files were queued, the Lucene database could
   *    be opened, all the files in the queue could be (re)indexed, and the
   *    database could be closed. Doing so significantly reduced the time to
   *    index the entire source tree. <br><br>
   *
   *    It should be noted that each file in the queue is identified by a
   *    "relocatable" path to the source tree directory where it was found,
   *    and that this relocatable path is stored in the Lucene database when
   *    the file is indexed. This relocatable path consists of the index name
   *    followed by the source tree sub-path at which the file is located.
   *    Storing this relocatable file path in the index allows the indexer
   *    and the search engine to correctly locate the source text, even if
   *    the source tree base directory has been renamed or moved. Correctly
   *    locating the original source text for chunks in an index is necessary
   *    when displaying search results, or to determine if source text needs
   *    to be reindexed due to changes, or removed from an index because
   *    it no longer exists. Ultimately, both the indexer and the query
   *    engine use the {@linkplain XMLConfigParser index configuration file} to
   *    map the index name back into an absolute path when a source text needs
   *    to be accessed. <br><br>
   *
   */
  public void processQueuedTexts()
    throws IOException 
  {
    // Initialize the string buffers for accumulating and compacting the 
    // text to index.
    //
    blurbedText = new StringBuffer(bufStartSize);
    accumText = new StringBuffer(bufStartSize);
    compactedAccumText = new StringBuffer(bufStartSize);

    // Calculate the total size of files in the queue
    long totalSize = 0;
    for (Iterator iter = fileQueue.iterator(); iter.hasNext();) {
      FileQueueEntry ent = (FileQueueEntry)iter.next();
      totalSize += ent.idxSrc.totalSize();
    }
    if (totalSize < 1)
      totalSize = 1; // avoid divide-by-zero problems
    long processedSize = 0;

    final int recordBatchSize = 100;

    // Process each queued file.
    while (!fileQueue.isEmpty()) 
    {
      boolean printDone = false;

      // Process deletions in batches.
      batchDelete();

      // Open the index writer (which might have been closed by a batch
      // deletion.)
      //
      openIdxForWriting();

      // Get the next file.
      FileQueueEntry ent = (FileQueueEntry)fileQueue.removeFirst();
      IndexSource idxFile = ent.idxSrc;
      assert !ent.deleteFirst; // Should have been processed by batchDelete()

      // Index each record within the file (often there's only one, but
      // there may be millions.)
      //
      IndexRecord idxRec;
      try 
      {
        while ((idxRec = idxFile.nextRecord()) != null) 
        {
          long fileBytesDone = idxRec.percentDone() * idxFile.totalSize() / 100;
          int percentDone = (int)((processedSize + fileBytesDone) * 100 / totalSize);
          int recordNum = idxRec.recordNum();

          // Print out a nice message to keep the user informed of
          // our progress.
          //
          String key = idxFile.key();
          curPrettyKey = (key.indexOf(':') >= 0)
                         ? key.substring(key.indexOf(':') + 1) : key;
          if (recordNum > 0)
            curPrettyKey += "/" + recordNum;

          if (recordNum == 0 || ((recordNum % recordBatchSize) == 1)) 
          {
            if (printDone)
              Trace.more(Trace.info, "Done.");

            // Print a message for the next record.
            String msg = "";
            msg = ("(" + percentDone + "%) ");
            while (msg.length() < 7)
              msg += " ";
            Trace.info(msg + "Indexing [" + curPrettyKey + "] ... ");
            printDone = true;
          }

          // Now index this record.
          processText(idxFile, idxRec, recordNum);
        } // while
      }
      catch (SAXException e) {
        throw new RuntimeException(e);
      }

      if (printDone)
        Trace.more(Trace.info, "Done.");

      processedSize += idxFile.totalSize();
    }
  } // processQueuedTexts()

  ////////////////////////////////////////////////////////////////////////////

  /** Add the specified XML source record to the active Lucene index.
   *
   *  This method indexes the specified XML source text file, adding it to the
   *  Lucene database currently specified by the {@link #indexPath} member.
   *
   * @param file      The XML source text file to process.
   * @param record    Record within the XML file to process.
   * @param recordNum Zero-based index of this record in the XML file.
   *
   * @throws
   *   IOException  Any I/O errors encountered opening or reading the XML source
   *                text file or the Lucene database. <br><br>
   *
   * @.notes
   *   To learn more about the actual mechanincs of how XML source files are
   *   indexed, see the {@link XMLTextProcessor} class description.
   *
   */
  private int processText(IndexSource file, IndexRecord record, int recordNum)
    throws IOException 
  {
    // Clear the text buffers.
    accumText.setLength(0);
    compactedAccumText.setLength(0);

    // Record the file's parameters so other methods can get to them easily. Then
    // tell the user what we're doing.
    //
    curIdxSrc = file;
    curIdxRecord = record;

    // Build a lazy tree if requested.
    lazyStore = record.lazyStore();
    if (lazyStore != null) 
    {
      // While we parse the source document, we're going to also build up 
      // a tree that will be written to the lazy file.
      //
      Configuration config = new Configuration();
      config.setNamePool(NamePool.getDefaultNamePool());
      lazyBuilder = new LazyTreeBuilder(config);
      lazyReceiver = lazyBuilder.begin(lazyStore);

      lazyBuilder.setNamePool(config.getNamePool());

      lazyHandler = new ReceivingContentHandler();
      lazyHandler.setReceiver(lazyReceiver);
      lazyHandler.setPipelineConfiguration(lazyReceiver.getPipelineConfiguration());
    }
    else {
      lazyBuilder = null;
      lazyReceiver = null;
      lazyHandler = null;
    }

    // Determine how many words should be in each chunk.
    chunkWordSize = indexInfo.getChunkSize();

    // Determine the first word in each chunk that the next overlapping
    // chunk should start at.
    //
    chunkWordOvlp = indexInfo.getChunkOvlp();
    chunkWordOvlpStart = chunkWordSize - chunkWordOvlp;

    // Reset the node tracking info to the first node in the document.
    // Note that we start the current node number at 0, because the
    // first call to startElement() will pre-increment it to one (which is
    // correct since the document is node zero).
    //
    curNode = 0;
    chunkStartNode = -1;
    nextChunkStartNode = -1;

    nodeWordCount = 0;

    chunkWordCount = 0;
    chunkWordOffset = 0;

    nextChunkStartIdx = 0;
    nextChunkWordCount = 0;
    nextChunkWordOffset = 0;

    forcedChunk = false;
    inMeta = 0;

    // Reset the meta info for the new document.
    metaBuf.setLength(0);

    // Reset count of how many chunks we've written.
    chunkCount = 0;

    // Create an initial unnamed section with depth zero, indexing turned on, 
    // a blank section name, no section bump, a word bump of 1, no word boost,
    // default sentence bump, blank subdocument name, and empty meta-data.
    //
    section = new SectionInfoStack();
    section.push();
    
    // Now parse it.
    int result = parseText();

    // Regardless of result, finish the lazy tree so we don't leave everything
    // hanging open.
    //
    if (lazyBuilder != null) 
    {
      lazyBuilder.finish(lazyReceiver, false); // don't close Store yet

      // If a stylesheet has been specified that contains xsl:key defs
      // to apply to the lazy tree, do so now.
      //
      if (result == 0) 
      {
        Templates displayStyle = file.displayStyle();
        if (displayStyle != null) 
        {
          try 
          {
            precacheXSLKeys();
          }
          catch (IOException e) {
            Trace.tab();
            Trace.error(
              "Error pre-caching XSL keys from " + "display stylesheet \"" +
              displayStyle + "\": " + e);
            Trace.untab();

            throw e;
          }
          catch (Throwable t) {
            Trace.tab();
            Trace.error(
              "Error pre-caching XSL keys from " + "display stylesheet \"" +
              displayStyle + "\": " + t);
            Trace.untab();

            if (t instanceof RuntimeException)
              throw (RuntimeException)t;
            else
              throw new IOException(
                "Error pre-caching XSL keys from " + "display stylesheet \"" +
                displayStyle + "\": " + t);
          }
        }
      }

      // Now that the keys are built, it's safe to close the lazy store.
      lazyStore.close();
    } // if

    // And we're done.
    return result;
  } // processText()

  ////////////////////////////////////////////////////////////////////////////

  /** Parse the XML source text file specified. <br><br>
   *
   *  This method instantiates a SAX XML file parser and passes this class
   *  as the token handler. Doing so causes the
   *  {@link XMLTextProcessor#startDocument() startDocument()},
   *  {@link XMLTextProcessor#startElement(String,String,String,Attributes) startElement()},
   *  {@link XMLTextProcessor#endElement(String,String,String) endElement()},
   *  {@link XMLTextProcessor#endDocument() endDocument()}, and
   *  {@link XMLTextProcessor#characters(char[],int,int) characters()}
   *  methods in this class to be called. These methods in turn process the
   *  actual text in the XML source document, "blurbifying" the text, breaking
   *  it up into overlapping chunks, and adding it to the Lucene index. <br><br>
   *
   *  @return  <code>0</code> - XML source file successfully parsed and indexed.
   *                            <br>
   *           <code>-1</code> - One or more errors encountered processing
   *                             XML source file.
   *
   *  @.notes
   *    For more about "blurbifying" text, see the {@link XMLTextProcessor#blurbify(StringBuffer,boolean) blurbify()}
   *    method. <br><br>
   *
   *    This function enables namespaces for XML tag attributes. Consquently,
   *    attributes such as <code>sectiontype</code> and <code>proximitybreak</code>
   *    are assumed to be prefixed by the namespace <code>xtf</code>. <br><br>
   *
   *    If present in the {@link XMLTextProcessor#indexInfo indexInfo} member,
   *    the XML file will be prefiltered with the specified XSL filter before
   *    XML parsing begins. This allows node attributes to be inserted that
   *    modify the proximity of various text sections as well as boost or
   *    deemphasize the relevance sections of text. For a description of
   *    attributes handled by this XML parser, see the {@link XMLTextProcessor}
   *    class description. <br><br>
   */
  private int parseText() 
  {
    try 
    {
      // Instantiate a new XML parser, being sure to get the right one.
      SAXParser xmlParser = IndexUtil.createSAXParser();

      // Get the input source from the record.
      InputSource xmlSource = curIdxRecord.xmlSource();

      // If there are no XSLT input filters defined for this index, just 
      // parse the source XML file directly, and return early.
      //
      Templates[] prefilters = curIdxSrc.preFilters();
      if (prefilters == null || prefilters.length == 0) {
        xmlParser.parse(xmlSource, this);
        return 0;
      }

      // Apply the prefilters.
      IndexUtil.applyPreFilters(prefilters,
                                xmlParser.getXMLReader(),
                                xmlSource,
                                new SAXResult(this));
    } // try

    catch (Throwable t) 
    {
      // Abort the lazy tree building so we don't leave a half-cooked lazy
      // tree file laying around.
      //
      if (lazyReceiver != null) 
      {
        lazyBuilder.abort(lazyReceiver);
        lazyBuilder = null;
        lazyReceiver = null;
        lazyHandler = null;
      }
      
      // Tell the caller (and the user) that ther was an error..      
      Trace.more(Trace.info, "Skipping Due to Errors");

      String message = "*** XML Parser Exception: " + t.getClass() + "\n" +
                       "    With message: " + t.getMessage() + "\n" +
                       "    File: " + curPrettyKey;
      if (curIdxRecord.recordNum() > 0)
        message += ("\n    Record number: " + curIdxRecord.recordNum());

      Trace.info(message);

      return -1;
    } // try( to filter the input file )

    // If we got to this point, tell the caller that all went well.
    return 0;
  } // public parseText() 

  ////////////////////////////////////////////////////////////////////////////

  /** To speed accesses in dynaXML, the lazy tree is capable of storing
   *  pre-cached indexes to support each xsl:key declaration. These take a
   *  while to build, however, so it's desirable to do this at index time
   *  rather than on-demand.
   *
   *  This method reads a stylesheet that should contain the xsl:key
   *  declarations that will be used. It then generates each key and stores
   *  it in the lazy file.
   *
   * @throws Exception      If anything goes awry.
   */
  private void precacheXSLKeys()
    throws Exception 
  {
    // Get the stylesheet.
    Templates stylesheet = curIdxSrc.displayStyle();

    // Register a lazy key manager
    PreparedStylesheet pss = (PreparedStylesheet)stylesheet;
    Executable exec = pss.getExecutable();
    if (!(exec.getKeyManager() instanceof LazyKeyManager))
      exec.setKeyManager(new LazyKeyManager(pss.getConfiguration(), exec.getKeyManager()));

    Transformer trans = pss.newTransformer();
    LazyKeyManager keyMgr = (LazyKeyManager)exec.getKeyManager();
    LazyDocument doc = (LazyDocument)lazyBuilder.load(lazyStore);

    // For every xsl:key registered in the stylesheet, build the lazy key
    // hash.
    //
    int nKeysCreated = keyMgr.createAllKeys(doc,
                                            ((Controller)trans).newXPathContext());
    Trace.more(Trace.info, "(" + nKeysCreated + " stored " +
               ((nKeysCreated == 1) ? "key" : "keys") + ") ... ");

    // Make sure to close it when we're done.
    doc.close();
  } // precacheXSLKeys()

  ////////////////////////////////////////////////////////////////////////////

  /** Process the start of a new XML document.
   *
   *  Called by the XML file parser at the beginning of a new document. <br><br>
   *
   *  @throws
   *    SAXException  Any exceptions encountered by the
   *                  {@link XMLTextProcessor#lazyBuilder lazyBuilder}
   *                  during start of document processing. <br><br>
   *
   *  @.notes
   *    This method simply calls the start of document handler for the "lazy
   *    tree" builder object {@link XMLTextProcessor#lazyBuilder lazyBuilder}.
   */
  public void startDocument()
    throws SAXException 
  {
    if (lazyHandler != null)
      lazyHandler.startDocument();
  } // startDocument()

  ////////////////////////////////////////////////////////////////////////////

  /** Process the start of a new XML source text element/node/tag. <br><br>
   *
   *  Called by the XML file parser each time a new start tag is encountered.
   *  <br><br>
   *
   *  @param  uri        Any namespace qualifier that applies to the current
   *                     XML tag.
   *
   *  @param  localName  The non-qualified name of the current XML tag.
   *
   *  @param  qName      The qualified name of the current XML tag.
   *
   *  @param  atts       Any attributes for the current tag. Note that only
   *                     attributes that are in the namespace specified by the
   *                     {@link XMLTextProcessor#xtfUri xtfUri}
   *                     member of this class are actually processed by this
   *                     method. <br><br>
   *
   *  @throws
   *    SAXException  Any exceptions generated by calls to "lazy tree" or
   *                  Lucene database access methods. <br><br>
   *  @.notes
   *    This method processes any text accumulated before the current start tag
   *    was encountered by calling the {@link XMLTextProcessor#flushCharacters() flushCharacters()}
   *    method. It also calls the {@link XMLTextProcessor#lazyHandler lazyHandler}
   *    object to write the accumulated text to the "lazy tree" representation
   *    of the XML source file. Finally, it then resets the node tracking
   *    information to match the new node, including any boost or bump
   *    attributes set for the new node. <br><br>
   */
  public void startElement(String uri, String localName, String qName,
                           Attributes atts)
    throws SAXException// called at element start
   
  {
    // Process any characters accumulated for the previous node, writing them
    // out as chunks to the index if needed.
    // 
    flushCharacters();

    // And add the accumulated text to the "lazy tree" representation as well.
    if (lazyHandler != null)
      lazyHandler.startElement(uri, localName, qName, atts);

    // If this is the start of a meta data node (marked with an xtf:meta
    // attribute), read in the meta data. Note that these meta-data nodes are 
    // not indexed as part of the general text.
    //
    int metaIndex = atts.getIndex(xtfUri, "meta");
    String metaVal = (metaIndex >= 0) ? atts.getValue(metaIndex) : "";
    if (metaIndex >= 0 && ("yes".equals(metaVal) || "true".equals(metaVal))) 
    {
      if (inMeta > 0)
        throw new RuntimeException("Meta-data fields may not nest");

      inMeta = 1;

      // See if there is a "store" attribute set for this node. If not,
      // default to true.
      //
      boolean store = true;
      int tokIdx = atts.getIndex(xtfUri, "store");
      if (tokIdx >= 0) {
        String tokStr = atts.getValue(tokIdx);
        if (tokStr != null && (tokStr.equals("no") || tokStr.equals("false")))
          store = false;
      }

      // See if there is an "index" attribute set for this node. If not,
      // default to true.
      //
      boolean index = true;
      tokIdx = atts.getIndex(xtfUri, "index");
      if (tokIdx >= 0) {
        String tokStr = atts.getValue(tokIdx);
        if (tokStr != null && (tokStr.equals("no") || tokStr.equals("false")))
          index = false;
      }

      // See if there is a "noIndex" attribute set for this node.
      tokIdx = atts.getIndex(xtfUri, "noIndex");
      if (tokIdx >= 0) {
        String tokStr = atts.getValue(tokIdx);
        if (tokStr != null && (tokStr.equals("yes") || tokStr.equals("true")))
          index = false;
      }

      // See if there is a "tokenize" attribute set for this node. If not,
      // default to true.
      //
      boolean tokenize = true;
      tokIdx = atts.getIndex(xtfUri, "tokenize");
      if (tokIdx >= 0) {
        String tokStr = atts.getValue(tokIdx);
        if (tokStr != null && (tokStr.equals("no") || tokStr.equals("false")))
          tokenize = false;
      }

      // See if there is a "facet" attribute set for this node. If not,
      // default to false.
      //
      boolean isFacet = false;
      tokIdx = atts.getIndex(xtfUri, "facet");
      if (tokIdx >= 0) {
        String tokStr = atts.getValue(tokIdx);
        if (tokStr != null && (tokStr.equals("yes") || tokStr.equals("true")))
          isFacet = true;
      }

      // See if there is a "spell" attribute set for this node. If not,
      // default to true.
      //
      boolean spell = true;
      tokIdx = atts.getIndex(xtfUri, "spell");
      if (tokIdx >= 0) {
        String tokStr = atts.getValue(tokIdx);
        if (tokStr != null && (tokStr.equals("no") || tokStr.equals("false")))
          spell = false;
      }

      // See if there is a "wordBoost" attribute for this node. If not, 
      // default to 1.0f.
      //
      float boost = 1.0f;
      int boostIdx = atts.getIndex(xtfUri, "wordBoost");
      if (boostIdx < 0)
        boostIdx = atts.getIndex(xtfUri, "wordboost");
      if (boostIdx >= 0) {
        String boostStr = atts.getValue(boostIdx);
        boost = Float.parseFloat(boostStr);
      }
      
      // Certain field names are reserved for internal use.
      if (localName.matches("^(text|key|docInfo|chunkCount|chunkOvlp|chunkSize|fileDate|indexInfo|stopWords|tokenizedFields|xtfIndexVersion)$"))
        throw new RuntimeException("Reserved name '" + localName + "' not allowed as meta-data field");

      // Allocate a place to store the contents of the meta-data field.
      metaField = new MetaField(localName,
                                store,
                                index,
                                tokenize,
                                isFacet,
                                spell,
                                boost);
      assert metaBuf.length() == 0 : "Should have cleared meta-buf";

      // If there are non-XTF attributes on the node, record them.
      String attrString = processMetaAttribs(atts);
      if (attrString.length() > 0)
        metaBuf.append("<$ " + attrString + ">");
    }

    // If there are nested tags below a meta-field (and if they're not
    // meta-fields themselves), keep track of how far down they go, so we can
    // know when we hit the end of the top-level tag.
    //
    else if (inMeta > 0) {
      inMeta++;
      metaBuf.append("<" + localName);
      String attrString = processMetaAttribs(atts);
      if (attrString.length() > 0)
        metaBuf.append(" " + attrString);
      metaBuf.append(">");
    }

    // All other nodes need to be tracked, so increment the node number and 
    // reset the word count for the node.
    //
    else 
    {
      // Get the current section type and word boost.
      SectionInfo prev = section.prev();

      // Process the node specific attributes such as section type,
      // section bump, word bump, word boost, and so on.
      //
      processNodeAttributes(atts);

      // If the section type changed, we need to start a new chunk.
      if (section.sectionType() != prev.sectionType ||
          section.wordBoost()   != prev.wordBoost   ||
          section.spellFlag()   != prev.spellFlag   ||
          section.subDocument() != prev.subDocument) 
      {
        // Clear out any remaining accumulated text.
        forceNewChunk(prev);

        // Diagnostic info.
        //Trace.tab();
        //Trace.debug("Begin Section [" + section.sectionType() + "]");
        //Trace.untab();
      }
    }

    // Increment the tag ID (count) for the new node we encountered, and
    // reset the accumulated word count for this node.
    //
    incrementNode();
  } // public startElement()

  ////////////////////////////////////////////////////////////////////////////

  /** Build a string representing any non-XTF attributes in the given
   *  attribute list. This will be a series of name="value" pairs, separated
   *  by spaces. If there are no non-XTF attributes, empty string is returned.
   *  <br><br>
   */
  private String processMetaAttribs(Attributes atts) 
  {
    StringBuffer buf = new StringBuffer();

    // Scan the list
    for (int i = 0; i < atts.getLength(); i++) 
    {
      // Skip XTF-specific attributes
      String attrUri = atts.getURI(i);
      if (attrUri.equals(xtfUri))
        continue;

      // Found one. Add it to the buffer.
      if (buf.length() > 0)
        buf.append(' ');
      String name = atts.getLocalName(i);
      String value = atts.getValue(i);
      buf.append(name + "=\"" + value + "\"");
    } // for i

    // All done.
    return buf.toString();
  } // processMetaAttribs( Attributes atts )

  ////////////////////////////////////////////////////////////////////////////

  /** Increment the node tracking information. <br><br>
   *
   *  @.notes
   *    This method is called when a new node in a source XML document
   *    has been encountered. It increments the current node count, resets the
   *    number of words accumulated for the new node to zero, and if a partial
   *    chunk has been accumulated, inserts a node marker into the accumulated
   *    text buffer.
   */
  private void incrementNode() 
  {
    // First, bump the node counter and reset the word count.
    curNode++;
    nodeWordCount = 0;

    // If a chunk is in progress, add a node marker to it, so that later, the
    // snippet maker can detect the change of node.
    //
    if (accumText.length() > 0)
      accumText.append(Constants.NODE_MARKER);
  } // private incrementNode()

  ////////////////////////////////////////////////////////////////////////////

  /** Process the end of a new XML source text element/node/tag. <br><br>
   *
   *  Called by the XML file parser each time an end-tag is encountered.
   *  <br><br>
   *
   *  @param  uri        Any namespace qualifier that applies to the current
   *                     XML tag.
   *
   *  @param  localName  The non-qualified name of the current XML tag.
   *
   *  @param  qName      The qualified name of the current XML tag. <br><br>
   *
   *  @throws
   *    SAXException  Any exceptions generated by calls to "lazy tree" or
   *                  Lucene database access methods. <br><br>
   *  @.notes
   *    This method processes any text accumulated before the current end tag
   *    was encountered by calling the
   *    {@link XMLTextProcessor#flushCharacters() flushCharacters()} method.
   *    It also calls the {@link XMLTextProcessor#lazyHandler lazyHandler}
   *    object to write the accumulated text to the "lazy tree" representation
   *    of the XML source file. Finally, it returns the node tracking
   *    information back to a state that match the parent node, including
   *    any boost or bump attributes previously set for that node. <br><br>
   */
  public void endElement(String uri, String localName, String qName)
    throws SAXException// called at element end
   
  {
    // Process any characters accumulated for the previous node, writing them
    // out as chunks to the index if needed.
    // 
    flushCharacters();
    
    // And add the accumulated text to the "lazy tree" representation as well.
    if (lazyHandler != null)
      lazyHandler.endElement(uri, localName, qName);

    // If we're in a meta-data field, record the end tag (except if it's the
    // top-level tag, which we leave out to save space on non-structured 
    // meta-data.)
    //
    if (inMeta > 1)
      metaBuf.append("</" + localName + ">");
    
    // If this is the end of a meta-data field, record it.
    if (inMeta == 1) 
    {
      metaField.value = metaBuf.toString().trim();

      // If tokenized, add the special start-of-field and end-of-field tokens
      // to the meta-data value.
      //
      if (metaField.tokenize && !metaField.isFacet) 
      {
        // If attributes were recorded for the top-level node, be sure to
        // put the start marker after them.
        //
        if (metaField.value.length() > 0 && metaField.value.charAt(0) == '<') {
          int insertPoint = metaField.value.indexOf('>') + 1;
          metaField.value = metaField.value.substring(0, insertPoint) +
                            Constants.FIELD_START_MARKER +
                            metaField.value.substring(insertPoint) +
                            Constants.FIELD_END_MARKER;
        }
        else {
          metaField.value = Constants.FIELD_START_MARKER + metaField.value +
                            Constants.FIELD_END_MARKER;
        }
      }

      // Lucene will fail subtly if we add two fields with the same name.
      // Basically, the terms for each field are added at overlapping
      // positions, causing a phrase search to easily span them. To counter
      // this, we stick them all together in one field, but add word bump
      // separators to keep hits from occurring across one and the next.
      // We use the special word bump 'x' to mean a million, which should be
      // quite sufficient to keep matches from spanning these boundaries.
      //
      // Of course, we only need to do this work for tokenized fields (as
      // we must assume that untokenized fields will be used for sorting and
      // grouping only, and glomming things together would mess that up.)
      //
      boolean add = true;
      int nFound = 0;
      for (Iterator i = section.metaInfo().iterator(); i.hasNext();) 
      {
        MetaField mf = (MetaField)i.next();
        boolean found = mf.name.equals(metaField.name);
        if (found)
          ++nFound;
        if (found && mf.tokenize && !mf.isFacet) {
          StringBuffer buf = new StringBuffer();
          buf.append(mf.value);
          buf.append(Constants.BUMP_MARKER);
          buf.append('x');
          buf.append(Constants.BUMP_MARKER);
          buf.append(' ');
          buf.append(metaField.value);
          mf.value = buf.toString();
          add = false;
        }
      }

      if (add) 
      {
        // Only pay attention to the boost of the first value for a given
        // field, since Lucene multiplies them all together. If we didn't 
        // do this, four values with a boost of 2 would result in a total 
        // boost of 16, which isn't what we want.
        //
        if (nFound > 0)
          metaField.wordBoost = 1.0f;

        // Record the new field.
        section.metaInfo().add(metaField);
      }

      metaField = null;
      metaBuf.setLength(0);
      inMeta = 0;
    }
    else if (inMeta > 1)
      inMeta--;
    else
    {
      // For non-meta nodes, we need to pop the section stack. First, 
      // save the section type and word boost value so we can use them
      // if changes occur.
      //
      SectionInfo prev = section.prev();
  
      // Decrease the section stack depth as needed, possibly pulling
      // the entire section entry off the stack.
      //
      section.pop();
  
      // If the section type changed, force new text to start in a new chunk. 
      if (section.sectionType() != prev.sectionType ||
          section.wordBoost() != prev.wordBoost ||
          section.spellFlag() != prev.spellFlag) 
      {
        // Output any remaining accumulated text.
        forceNewChunk(prev);
  
        // Diagnostic info.
        //Trace.tab();
        //Trace.debug("End Section [" + prevSectionType + "]");
        //Trace.untab();
      }
      
      // If the subdocument has changed...
      if (section.subDocument() != prev.subDocument) 
      {
        // Clear out any partially accumulated chunks.         
        forceNewChunk(prev);
  
        // Insert a docInfo chunk right here, if there are chunks to include.
        if (chunkCount > 0)
          saveDocInfo(prev);
  
        // We're now in a new section, and a new subdocument.
        chunkStartNode = -1;
        chunkWordOffset = -1;
      }
    }

    // Cross-check to make sure our node counting matches the lazy tree.
    if (lazyBuilder != null)
      assert lazyBuilder.getNodeNum(lazyReceiver) == curNode + 1;
  } // public endElement()

  ////////////////////////////////////////////////////////////////////////////
  public void processingInstruction(String target, String data)
    throws SAXException 
  {
    // Filter out processing instructions.
  } // processingInstruction()

  ////////////////////////////////////////////////////////////////////////////
  public void startPrefixMapping(String prefix, String uri)
    throws SAXException 
  {
    if (lazyHandler != null)
      lazyHandler.startPrefixMapping(prefix, uri);
  } // startPrefixMapping()

  ////////////////////////////////////////////////////////////////////////////
  public void endPrefixMapping(String prefix)
    throws SAXException 
  {
    if (lazyHandler != null)
      lazyHandler.endPrefixMapping(prefix);
  } // endPrefixMapping()

  ////////////////////////////////////////////////////////////////////////////

  /** Perform any final document processing when the end of the XML source
   *  text has been reached. <br><br>
   *
   *  Called by the XML file parser when the end of the source document is
   *  encountered. <br><br>
   *
   *  @throws  SAXException  Any exceptions generated during the final writing
   *                         of the Lucene database or the "lazy tree"
   *                         representation of the XML file. <br><br>
   *
   *  @.notes
   *    This method indexes any remaining accumulated text, adds any remaining
   *    text to the "lazy tree" representation of the the XML document, and
   *    writes out the document summary record (chunk) to the Lucene database.
   *    <br><br>
   *
   */
  public void endDocument()
    throws SAXException 
  {
    // Index the remaining accumulated chunk (if any).
    indexText(section.peek());

    // Save the document "header" info.
    saveDocInfo(section.peek());

    // Finish building the lazy tree
    if (lazyHandler != null)
      lazyHandler.endDocument();
  } // public endDocument()

  ////////////////////////////////////////////////////////////////////////////

  /** Accumulate chunks of text encountered between element/node/tags.
   *
   *  @param  ch      A block of characters from which to accumulate text.
   *                  <br><br>
   *
   *  @param  start   The starting offset in <code>ch</code> of the characters
   *                  to accumulate. <br><br>
   *
   *  @param  length  The number of characters to accumulate from <code>ch</code>.
   *                  <br><br>
   *
   *  @.notes
   *    Depending on how the XML parser is implemented, a call to this function
   *    may or may not receive all the characters encountered between two tags
   *    in an XML file. However, for the <code>XMLTextProcessor</code> to
   *    correctly assemble overlapping chunks for the Lucene database, it needs
   *    to have all the characters between two tags available as a single chunk
   *    of text. Consequently this method simply accumulates text, and calls
   *    calls from the XML parser to
   *    {@link XMLTextProcessor#startElement(String,String,String,Attributes) startElement()}
   *    and
   *    {@link XMLTextProcessor#endElement(String,String,String) endElement()}
   *    trigger the actual processing of accumulated text.
   */
  public void characters(char[] ch, int start, int length)
    throws SAXException 
  {
    // If the accumulation buffer is not big enough to receive the current 
    // block of new characters...
    //
    if (charBufPos + length > charBuf.length) 
    {
      // Hang on to the old buffer.
      char[] old = charBuf;

      // Create a new buffer that does have space plus a bit (to try to 
      // avoid unnecessary reallocations for any small additional chunks
      // that might follow.)
      //
      charBuf = new char[charBufPos + length + 1024];

      // And copy the previously accumulated text into the new buffer.
      System.arraycopy(old, 0, charBuf, 0, charBufPos);
    }

    // Add the new block of text to the accumulation buffer, and update the
    // count of accumulated characters.
    //
    System.arraycopy(ch, start, charBuf, charBufPos, length);
    charBufPos += length;
  } // public characters()

  ////////////////////////////////////////////////////////////////////////////

  /** Process any accumulated source text, writing indexing completed chunks
   *  to the Lucene database as necessary. <br><br>
   *
   *   @throws
   *       SAXException  Any exceptions encountered during the processing of
   *                     the accumulated chunks, or writing them to the Lucene
   *                     index. <br><br>
   *  @.notes
   *    This method processes any accumulated text as follows:
   *
   *    <blockquote dir=ltr style="MARGIN-RIGHT: 0px">
   *     1. First the accumulated text is "blurbified." See the
   *        {@link XMLTextProcessor#blurbify(StringBuffer,boolean) blurbify()}
   *        method for more information about what this entails. <br><br>
   *
   *     2. Next, a chunk is assembled a word at a time from the accumulated
   *        text until the required chunk size (in words) is reached. The
   *        completed chunk is then added to the Lucene database.
   *        <br><br>
   *
   *     3. Step two is repeated until no more complete chunks can be assembled
   *        from the accumulated text. (Any partial chunk text is saved until
   *        the next call to this method.)
   *     </blockquote>
   */
  public void flushCharacters()
    throws SAXException 
  {
    // Get local references to the accumulated text buffer that we can 
    // adjust as we go.
    //
    char[] ch = charBuf;
    int length = charBufPos;
    int start = 0;

    // Reset the accumulated character count to zero in anticipation of 
    // accumulating characters for a new node later on. (Do this now because
    // of multiple exit points in this function.)
    //
    charBufPos = 0;

    // If the entire buffer is whitespace (or empty), we can safely strip it.
    int i = 0;
    if (indexInfo.stripWhitespace) {
      for (i = 0; i < length; i++)
        if (!Character.isWhitespace(charBuf[i]))
          break;
    }
    if (i == length)
      return;

    // Build this part of the lazy tree, and increment the node number in
    // concert with it.
    //
    if (lazyHandler != null)
      lazyHandler.characters(ch, start, length);
    incrementNode();

    // If we're processing a meta-info section, simply add the characters to
    // the meta-info buffer.
    //
    if (inMeta > 0) 
    {
      String tmp = new String(ch, start, length);

      // Map special XML characters to entities, so we can tell the difference
      // between these and embedded XML in the meta-data.
      //
      if (tmp.indexOf('&') >= 0)
        tmp = tmp.replaceAll("&", "&amp;");
      if (tmp.indexOf('<') >= 0)
        tmp = tmp.replaceAll("<", "&lt;");
      if (tmp.indexOf('>') >= 0)
        tmp = tmp.replaceAll(">", "&gt;");
      metaBuf.append(tmp);
      return;
    }

    // If we aren't supposed to index this section, return immediately.
    if (section.indexFlag() == SectionInfo.noIndex)
      return;

    // Create a blurbed text buffer.
    blurbedText.setLength(0);

    // Place the text section passed to this function in the buffer.
    blurbedText.append(ch, start, length);

    // Blurbify the text (i.e., convert line feeds, tabs, multiple spaces, 
    // and other weird white-space into something that's nicer to read in
    // a blurb.
    //
    blurbify(blurbedText, true);

    // Insert any virutal words implied by section bumps, ends of sentences,
    // global word bump changes and so on.
    //
    insertVirtualWords(blurbedText);

    // If after blurbification, there's no text remaining, we're done.
    if (blurbedText.length() <= 0)
      return;

    // Create a string reader for use by the tokenizer.
    String blurbedTextStr = blurbedText.toString();

    //StringReader reader         = new StringReader( blurbedTextStr );
    FastStringReader reader = new FastStringReader(blurbedTextStr);

    // Create a tokenizer to locate the start and end of words in the
    // blurbified text.
    //
    //TokenStream result = new StandardTokenizer( reader );
    TokenStream result = new FastTokenizer(reader);

    // Set the start of punctuation index to the beginning of the blurbified 
    // text buffer.
    //
    int punctStart = 0;

    // Trim all the trailing spaces off the accumulated text buffer
    // except for one, and get back the resulting length of the 
    // trimmed, accumulated text.
    //
    int accumTextLen = trimAccumText(true);

    // Start out having to fetch the first word in the token list.
    boolean mustGetNextWord = true;
    Token word = null;

    // Process the entire token list, accumulating and indexing chunks of
    // text as we go.
    //
    for (;;) 
    {
      try 
      {
        // If we haven't already fetched the next word, do so now.
        if (mustGetNextWord)
          word = result.next();

        // If there is no next word, we're done.
        if (word == null)
          break;

        // Flag that the next word hasn't been fetched.
        mustGetNextWord = true;

        // If this is the first word in the chunk, record its node and
        // offset.
        //
        if (chunkStartNode < 0) {
          chunkStartNode = curNode;
          chunkWordOffset = nodeWordCount;
        }

        // Determine where the current word starts and ends.
        int wordStart = word.startOffset();
        int wordEnd = word.endOffset();

        // Determine how much punctuation there is before the current
        // current word we're processing.
        //
        int punctLen = wordStart - punctStart;

        // If the word we just processed is the first word of the next
        // overlapping chunk...
        //
        if (chunkWordCount == chunkWordOvlpStart) 
        {
          // Record how much of the current chunk's text we need to
          // keep in the buffer for the next node.
          //
          nextChunkStartIdx = accumTextLen + punctLen;

          // Record what node the next chunk starts in, and what offset
          // the chunk has relative to the start of the node.
          //
          nextChunkStartNode = curNode;
          nextChunkWordOffset = nodeWordCount;

          // Finally, start with no words in the next node. 
          nextChunkWordCount = 0;
        } // if( chunkWordCount == chunkWordOvlpStart )

        // Append the new word and its preceeding punctuation/spacing to 
        // the text to index/store.
        //
        accumText.append(blurbedTextStr.substring(punctStart, wordEnd));

        // Track where the punctuation starts for the next word.  
        punctStart = wordEnd;

        // Account for this new word in the node, the current chunk, and 
        // (possibly) the next chunk.
        //
        chunkWordCount++;
        nextChunkWordCount++;
        if (!word.termText().equals(Constants.VIRTUAL_WORD))
          nodeWordCount++;

        // If we've got all the words required for the current chunk...
        if (chunkWordCount == chunkWordSize) 
        {
          // Get the start of the next word (if any), and flag that
          // we don't need to get it at the top of the loop.
          //
          word = result.next();
          mustGetNextWord = false;

          // If there is no next word, the trailing punctuation will
          // end at the end of the current buffer. Otherwise, the
          // trailing punctuation will end at the start of the next
          // word.
          //
          int punctEnd;
          if (word == null)
            punctEnd = blurbedText.length();
          else
            punctEnd = word.startOffset();

          // Tack the punctuation onto the end of the chunk and clean
          // it up so as to make it look all purdy.
          //
          accumText.append(blurbedTextStr.substring(punctStart, punctEnd));

          // Trim all the trailing spaces off the accumulated text.
          trimAccumText(false);

          // Index the accumulated text.
          indexText(section.peek());

          // Advance the punctuation start point for the next word to
          // the beginning of the word itself, since we already 
          // accumulated the punctuation after the current word.
          //
          punctStart = punctEnd;

          // Make the next chunk current.
          chunkStartNode = nextChunkStartNode;
          chunkWordOffset = nextChunkWordOffset;
          chunkWordCount = nextChunkWordCount;

          // Remove the text from the buffer that was in the previous
          // chunk but not in the next one.
          //
          accumText.delete(0, nextChunkStartIdx);

          // Make sure that the next word added doesn't bump up against
          // the last one accumulated.
          //
          accumTextLen = trimAccumText(true);

          // Reset the start index for the next chunk.
          nextChunkStartIdx = 0;
          nextChunkWordCount = 0;
        } // if( chunkWordCount == chunkWordSize )

        // Trim all the trailing spaces off the accumulated text 
        // buffer and get back the resulting accumulated text
        // length.
        //
        else
          accumTextLen = trimAccumText(false);
      } // try( to process next word in token list )

      catch (Exception e) 
      {
        // Log the error caught. There's no good reason to fail here, so
        // throw the exception on up.
        //
        Trace.tab();
        Trace.error("*** Exception Processing text: " + e);
        Trace.untab();

        throw new SAXException(e);
      }
      catch (Throwable t) 
      {
        // Log the error caught. There's no good reason to fail here, so
        // throw the exception on up.
        //
        Trace.tab();
        Trace.error("*** Exception Processing text: " + t);
        Trace.untab();

        throw new RuntimeException(t);
      }
    } // for(;;)

    // Accumulate and closing text/punctuation in this text block.
    accumText.append(blurbedTextStr.substring(punctStart,
                                              blurbedTextStr.length()));

    // Trim all the trailing spaces off the accumulated text buffer.
    trimAccumText(false);
  } // public characters()

  //////////////////////////////////////////////////////////////////////////////

  /** Forces subsequent text to start at the beginning of a new chunk. <br><br>
   *
   *  This method is used to ensure that source text marked with proximity
   *  breaks or new section types does not overlap with any previously
   *  accumulated source text.
   *
   *  @.notes
   *    This method writes out any accumulated text and resets the chunk
   *    tracking information to the start of a new chunk.
   */
  private void forceNewChunk(SectionInfo secInfo) 
  {
    // If there is a full chunk that hasn't been written yet, do it
    // now.
    if (nextChunkStartIdx > 0) 
    {
      indexText(secInfo);

      // Make the next chunk current.
      chunkStartNode = nextChunkStartNode;
      chunkWordOffset = nextChunkWordOffset;
      chunkWordCount = nextChunkWordCount;

      // Remove the text from the buffer that was in the previous
      // chunk but not in the next one.
      //
      accumText.delete(0, nextChunkStartIdx);

      // Make sure that the next word added doesn't bump up against
      // the last one accumulated.
      //
      trimAccumText(true);

      // Reset the start index for the next chunk.
      nextChunkStartIdx = 0;
      nextChunkWordCount = 0;
    }

    // Index whatever is left in the accumulated text buffer.
    indexText(secInfo);

    // Since we're forcing a new chunk, advance the next chunk past
    // the one we just wrote.
    //
    chunkWordOffset += chunkWordCount;

    // Zero the accumulated word count for the chunk.
    chunkWordCount = 0;
    nextChunkWordCount = 0;
    nextChunkWordOffset = 0;
    accumText.setLength(0);

    // Subsequent data might start in a new node.
    chunkStartNode = -1;

    // Indicate that the next chunk is being forced.
    forcedChunk = true;
  } // forceNewChunk()

  ////////////////////////////////////////////////////////////////////////////

  /** Utility method to trim trailing space characters from the end of the
   *  accumulated chunk text buffer. <br><br>
   *
   *  @param  oneEndSpace  Flag indicating whether the accumulated chunk text
   *                       buffer should be completely stripped of trailing
   *                       whitespace, or if one ending space should be left.
   *                       <br><br>
   *
   *  @return
   *    The final length of the accumulated chunk text buffer after the
   *    trailing spaces have been stripped. <br><br>
   *
   */
  private int trimAccumText(boolean oneEndSpace) 
  {
    // Figure out how long the current accumulated text is.
    int length = accumText.length();

    // Trim all the trailing spaces off the accumulated text buffer.
    while (length > 0 && accumText.charAt(length - 1) == ' ')
      accumText.deleteCharAt(--length);

    // If there's any accumulated text left, and the caller wants the 
    // accumulated text to end with a space (to guarantee that the next
    // word added will not run into the previously accumulated one), 
    // add back one space.
    //    
    if (length > 0 && oneEndSpace) {
      accumText.append(' ');
      length++;
    }

    // Tell the caller what the final length of the accumulated text
    // buffer was.
    //
    return length;
  } // trimAccumText()

  ////////////////////////////////////////////////////////////////////////////

  /** Convert the given source text into a "blurb." <br><br>
   *
   *  This method replaces line-feeds, tabs, and other whitespace characters
   *  with simple space characters to make the text more readable when
   *  presented to the user as the summary of a search query. <br><br>
   *
   *  @param  text   Upon entry, the text to be converted into a "blurb."
   *                 Upon return, the resulting "blurbed" text.
   *
   *  @param  trim   A flag indicating whether or not leading and trailing
   *                 whitespace should be trimmed from the resulting "blurb"
   *                 text. <br><br>
   *
   *  @.notes
   *    This function also compresses multiple space characters into a single
   *    space character, and removes any internal processing markers (i.e.,
   *    node tracking or bump tracking markers.)
   */
  private void blurbify(StringBuffer text, boolean trim) 
  {
    int i;

    // Determine the length of the passed text.
    int length = text.length();

    // Run through and replace any line-feeds, tabs or other funny
    // white-space characters in the text with a space character, as 
    // it will be more readable in the blurb.
    //
    for (i = 0; i < length; i++) 
    {
      // Get the current character.
      char theChar = text.charAt(i);

      // If it's the special token marker character, a tab, linefeed or 
      // some other spacing (but not actually a space character), replace
      // it so as to not cause problems in the blurb.
      //
      if (theChar == Constants.BUMP_MARKER ||
          theChar == Constants.NODE_MARKER ||
          (theChar != ' ' && Character.isWhitespace(theChar)))
        text.setCharAt(i, ' ');
    } // for( i = 0; i < length; i++ )

    // Compact multiple spaces down into a single space.
    for (i = 0; i < length - 1;) 
    {
      // If there are two spaces in a row at the current position...
      if (text.charAt(i) == ' ' && text.charAt(i + 1) == ' ') 
      {
        // Remove one of the spaces.
        text.deleteCharAt(i);

        // Account for the character we removed.
        length--;

        // And resume checking at the current position.
        continue;
      }

      // We didn't encounter two spaces in a row at the current character
      // position, so move on to the next one.
      //
      i++;
    } // for( i = 0; i < length-1; )

    // If the caller wants us to trim the leading and trailing spaces, do so.
    if (trim) 
    {
      // Trim the leading spaces (if any), and adjust the next chunk start
      // index to match.
      //
      while (length > 0 && text.charAt(0) == ' ') {
        text.deleteCharAt(0);
        length--;
      }

      // Trim the trailing spaces as well.
      while (length > 0 && text.charAt(length - 1) == ' ')
        text.deleteCharAt(--length);
    }
  } // blurbify()

  ////////////////////////////////////////////////////////////////////////////

  /** Inserts "virtual words" into the specified text as needed. <br><br>
   *
   *  @param  text  The text into which virtual words should be inserted.
   *                <br><br>
   *
   *  @.notes
   *    Virtual words? What's that all about? Well... <br><br>
   *
   *    The search engine is capable of performing proximity searches (up to
   *    the size of one text chunk.) Normally, the proximity of two words in
   *    a chunk is simply determined by the number of words between them.
   *    However, it might be nice to consider two words within a single
   *    sentence closer together than the same two words appearing in two
   *    adjacent sentences. Similarly, it might be nice if two words in a
   *    single section are considered closer together than the same two words
   *    in adjacent sections. <br><br>
   *
   *    To accomodate this, the text indexer supports the concept of increasing
   *    the distance between words at the end of a section or sentence and the
   *    beginning of the next (refererred to as "section bump" and "sentence
   *    bump" respectively.) The question is, how can we make it seem like the
   *    distance between a word at the end of one sentence/section and the first
   *    word in the next sentence/section is larger than it really is? The
   *    answer is to insert "virtual words." <br><br>
   *
   *    Virtual words are simply word-like markers inserted in the text that
   *    obey the same counting rules as real words, but do not actually appear
   *    in the final "blurb" seen by the user. For example, assume we have a
   *    sentence bump set to five, and the following text:
   *
   *    <blockquote dir=ltr style="MARGIN-RIGHT: 0px">
   *    Luke Luck likes lakes. <br>
   *    Luke's duck likes lakes. <br>
   *    Luke Luck licks lakes. <br>
   *    Luke's duck licks lakes. <br>
   *    Duck takes licks in lakes Luck Luck likes. <br>
   *    And Luke Luck takes licks in lakes duck likes.
   *    </blockquote>
   *
   *    When virtual words are inserted into this text from Dr. Seuss'
   *    <b>Fox in Sox</b>, the resulting blurb text that is added to
   *    the index looks as follows:
   *
   *    <blockquote dir=ltr style="MARGIN-RIGHT: 0px">
   *    Luke Luck likes lakes. <i>vw vw vw vw vw</i><br>
   *    Luke's duck likes lakes. <i>vw vw vw vw vw</i><br>
   *    Luke Luck licks lakes. <i>vw vw vw vw vw</i><br>
   *    Luke's duck licks lakes. <i>vw vw vw vw vw</i><br>
   *    Duck takes licks in lakes Luck Luck likes. <i>vw vw vw vw vw</i><br>
   *    And Luke Luck takes licks in lakes duck likes.
   *    </blockquote>
   *
   *    Because of the virtual word insertion, the <b>Luke</b> at the beginning
   *    of the first sentence is considered to be two words away from the
   *    <b>lakes</b> at end, and the <b>Luke</b> at the beginning of the second
   *    sentence is considered to be five words away from the <b>lakes</b> at
   *    the end of the first sentence. The result is that in these sentences,
   *    the <b>Luke</b>s are considered closer to the <b>lake</b>s in their
   *    respective sentences than the ones in the adjacent sentences. <br><br>
   *
   *    The actual marker used for virtual words is not really <b><i>vw</i></b>,
   *    since that combination of letters is likely to occur in regular text
   *    that discusses Volkswagens. The marker used is defined by the
   *    {@link Constants#VIRTUAL_WORD VIRTUAL_WORD}
   *    member of the {@link Constants} class, and has been chosen
   *    to be unlikely to appear in any actual western text. <br><br>
   *
   *    The added feature of using virtual words is that counting them like real
   *    words will result in text chunks with correct word counts, regardless of
   *    any bumps introduced. This is important if the code that displays search
   *    results is to correctly hilight the matched text. <br><br>
   *
   *    While the use of virtual words as shown will yield the correct spacing
   *    of bumped text, it is not very space efficient, and would yield larger
   *    than necessary Lucene databases. To eliminate the unwanted overhead,
   *    virtual words are compacted just before the chunk is written to the
   *    Lucene database with the method
   *    {@link XMLTextProcessor#compactVirtualWords() compactVirtualWords()}.
   *    <br><br>
   *
   *
   */
  private void insertVirtualWords(StringBuffer text) 
  {
    int i;

    // Get a spaced representation of the virtual word that we can use 
    // over and over again.
    //
    String vWord = Constants.VIRTUAL_WORD + " ";

    // Determine the length of the text to adjust.
    int len = text.length();

    // Move through all the text, looking for the end of sentences.
    for (i = 0; i < len; i++) 
    {
      // If we find the end of a sentence, insert the number of virtual
      // words to match the sentence bump value.
      //
      if (isEndOfSentence(i, len, text)) 
      {
        // If this is at the end of a quote, move beyond the closing
        // quote before inserting the virtual words.
        //          
        if (i < len - 1 && text.charAt(i + 1) == '"')
          i++;

        // Put in the virtual words.
        insertVirtualWords(vWord, section.sentenceBump(), text, i + 1);

        // Now that we've inserted virtual words, the length is no longer
        // valid. So update it before we continue.
        //
        len = text.length();
      }
    }

    // If there is currently no section bump pending...
    if (section.sectionBump() == 0) 
    {
      // And a new chunk has been forced, insert a proximity bump at the
      // beginning of the text.
      //
      if (forcedChunk) 
      {
        // Must insert chunkWordSize virtual words (not chunkWordOvlp
        // which could make it look like the next chunk overlaps).
        //
        // Update: I can't figure out why the above is true. It seems
        //         quite wasteful in fact.
        //
        insertVirtualWords(vWord, chunkWordOvlp, text, 0);

        // Cancel the forced chunk flag, now that we've handled it.
        forcedChunk = false;
      } // if( forcedChunk )      
    } // if( section.sectionBump() == 0 )

    // Otherwise, there is a section bump pending, so add it to the beginning
    // of the accumulated text.
    //
    else
      insertVirtualWords(vWord, section.useSectionBump(), text, 0);
  } // insertVirtualWords()

  ////////////////////////////////////////////////////////////////////////////

  /** Utility function to determine if the current character marks the end
   *  of a sentence.
   *
   *  @param  idx  The character offset in the accumulated chunk text buffer
   *               to check.
   *
   *  @param  len  The total length of the text in the text buffer passed.
   *
   *  @param  text The text buffer to check.
   *
   *  @return
   *    <code>true</code> - The current character marks the end of a sentence.<br>
   *    <code>false</code> - The current character does <b>not</b> mark
   *                         the end of a sentence. <br><br>
   *
   *  @.notes
   *     This method handles obvious end of sentence markers like <b>.</b>,
   *     <b>?</b> and <b>!</b>, but also "artistic" punctuation like <b>???</b>,
   *     <b>!!!</b>, and <b>?!?!</b>. Currently, it considers <b>...</b> to
   *     represent a long pause (extended comma), and does not treat it as the
   *     end of a sentence. It also tries to avoid mistaking periods as decimals
   *     and in acronyms (i.e., 61.7 and I.B.M.) as end of sentence markers.
   *     <br><br>
   */
  private boolean isEndOfSentence(int idx, int len, StringBuffer text) 
  {
    // Get the current character from the text. If it is not even a sentence
    // punctuation mark, return early.
    //
    char currChar = text.charAt(idx);
    if (!isSentencePunctuationChar(currChar))
      return false;

    // The current character is sentence punctuation, so lets get what's before
    // and after it.
    //
    char prevChar = ' ';
    char nextChar = ' ';
    if (idx > 0)
      prevChar = text.charAt(idx - 1);
    if (idx < len - 1)
      nextChar = text.charAt(idx + 1);

    // If the current character is a period...
    //
    if (currChar == '.') 
    {
      // It might be part of a number like 61.7, or part of an acronym like
      // I.B.M. So if the next char is alphanumeric, assume this period
      // doesn't end the sentence.
      //
      if (Character.isLetterOrDigit(nextChar))
        return false;

      // If it's not part of '...', we found the end of a sentence.
      if (prevChar != '.' && nextChar != '.')
        return true;

      // Otherwise, it's not the end of a sentence.
      return false;
    }

    //
    // Note: '...' is considered a comma (long pause) by the previous
    //       piece of code. if '...' should be treated as a period (end 
    //       of sentence), use this code should be used instead:
    //
    //  if( currChar == '.' ) {
    //      if( nextChar != '.' ) return true;
    //      return false;
    //  }

    // If we found an exclamation point, and it's the only one, or the last
    // one in a chain of '!!!' or '?!?!' we found an end of sentence.
    //
    if (currChar == '!') {
      if (!isSentencePunctuationChar(nextChar))
        return true;
      return false;
    }

    // If we found a question mark, and it's the only one, or the last
    // one in a chain of '???' or '!?!?' we found an end of sentence.
    //
    if (currChar == '?') {
      if (!isSentencePunctuationChar(nextChar))
        return true;
    }

    // If we got to this point, the character isn't even sentence punctuation,
    // so we definitely don't have the end of a sentence.
    //
    return false;
  } // isEndOfSentence()

  ////////////////////////////////////////////////////////////////////////////

  /** Utility function to detect sentence punctuation characters.
   *  <br><br>
   *
   *  @param  theChar  The character to check.
   *
   *  @return
   *    <code>true</code> - The specified character is a sentence punctuation
   *                        character. <br>
   *
   *    <code>false</code> - The specified character is <b>not</b> a
   *                         sentence punctuation character. <br><br>
   *
   *  @.notes
   *    This function looks for punctuation that marks the end of a sentence
   *    (not clause markers, like <b>;</b>, <b>:</b>, etc.) At this time, only
   *    <b>.</b>,, <b>?</b>, and <b>!</b> are considered end of sentence
   *    punctuation characters.
   */
  private boolean isSentencePunctuationChar(char theChar) 
  {
    // Currently, only '.', '?', and '!' are considered end of sentence
    // punctuation characters.
    //
    if (theChar == '.' || theChar == '?' || theChar == '!')
      return true;

    // Everything else is not.
    return false;
  } // isSentenctPunctuationChar()

  ////////////////////////////////////////////////////////////////////////////

  /** Utility function used by the main
   *  {@link XMLTextProcessor#insertVirtualWords(StringBuffer) insertVirtualWords()}
   *  method to insert a specified number of virtual word symbols. <br><br>
   *
   *  @param  vWord  The virtual word symbol to insert.
   *  @param  count  The number of virtual words to insert.
   *  @param  text   The text to insert the virtual words into.
   *  @param  pos    The character index in the text at which to insert the
   *                 virtual words. <br><br>
   *
   *  @.notes
   *    For an in-depth explanation of virtual words, see the main
   *    {@link XMLTextProcessor#insertVirtualWords(StringBuffer) insertVirtualWords()}
   *    method.
   */
  private void insertVirtualWords(String vWord, int count, StringBuffer text,
                                  int pos) 
  {
    // If the caller asked for no virtual words to be inserted, return early.
    if (count == 0)
      return;

    // Always start a block of virtual words with a space. Why? Because 
    // Lucene's standard tokenizer seems to treat a sequence like "it.qw"
    // as a single token. In fact, we want it to be treated like the word
    // 'it' followed by the virtual word token 'qw'. Adding the space 
    // assures that this is the case. (Don't worry, we compact the extra
    // space out later anyway when we convert to bump count notation.)
    //
    text.insert(pos++, ' ');

    // Insert the required number of virtual words.
    for (int j = 0; j < count; j++)
      text.insert(pos, vWord);
  } // insertVirtualWords()

  ////////////////////////////////////////////////////////////////////////////

  /** Add the current accumulated chunk of text to the Lucene database for
   *  the active index. <br><br>
   *
   *  @param  section      Info such as sectionType, wordBoost, etc.
   *
   *  @.notes
   *    This method peforms the final step of adding a chunk of assembled text
   *    to the Lucene database specified by the
   *    {@link XMLTextProcessor#indexInfo indexInfo} configuration member.
   *    This includes compacting virtual words via the
   *    {@link XMLTextProcessor#compactVirtualWords() compactVirtualWords()}
   *    method, and recording the unique document identifier (key) for the
   *    chunk, the section type for the chunk, the word boost for the chunk,
   *    the XML node in which the chunk begins, the word offset of the chunk,
   *    and the "blurbified" text for the chunk. <br><br>
   */
  private void indexText(SectionInfo secInfo) 
  {
    try 
    {
      // Convert virtual words into special bump tokens and adjusted chunk 
      // offsets to save space in the blurb text.
      //
      compactVirtualWords();
    }

    // There's no good reason for something to go wrong here, and no good
    // way to recover.
    //
    catch (Throwable t) 
    {
      // Log the error.
      Trace.tab();
      Trace.error("*** Exception Compacting Virtual Words: " + t);
      Trace.untab();

      // Throw the exception on up.
      if (t instanceof RuntimeException)
        throw (RuntimeException)t;
      else
        throw new RuntimeException(t);
    }

    // If after compaction there's nothing to index, we're done.
    if (compactedAccumText.length() == 0)
      return;

    // Make a new document, to which we can add our fields.     
    Document doc = new Document();

    // Add the key value so we can find this index entry again. Since we'll 
    // use this only as a finding aid, store the path as a non-stored, indexed,
    // non-tokenized field.
    //
    doc.add(new Field("key", curIdxSrc.key(), Field.Store.NO, Field.Index.UN_TOKENIZED));

    // Write the current section type as a stored, indexed, tokenized field.
    if (secInfo.sectionType != null && secInfo.sectionType.length() > 0)
      doc.add(new Field("sectionType", secInfo.sectionType, Field.Store.YES, Field.Index.TOKENIZED));
    
    // Write the subdocument id as a non-stored, indexed, non-tokenized field. It's
    // non-tokenized because the intent is that it will contain some sort of
    // subdocument identifier. And it's non-stored because the returning the subdoc
    // will only be done at the docHit level, not the chunk level.
    //
    if (secInfo.subDocument != null && secInfo.subDocument.length() > 0)
      doc.add(new Field("subDocument", secInfo.subDocument, Field.Store.NO, Field.Index.UN_TOKENIZED));

    // Convert the various integer field values to strings for writing.
    String nodeStr = Integer.toString(chunkStartNode);
    String wordOffsetStr = Integer.toString(chunkWordOffset);
    String textStr = compactedAccumText.toString();

    // Diagnostic output.
    //Trace.tab();
    //Trace.info("Chunk: text = [" + textStr + "], subDoc = " + secInfo.subDocument);
    //Trace.untab();

    // Add the node number for this chunk. Store, but don't index or tokenize.
    doc.add(new Field("node", nodeStr, Field.Store.YES, Field.Index.NO));

    // Add the word offset for this chunk. Store, but don't index or tokenize.
    doc.add(new Field("wordOffset", wordOffsetStr, Field.Store.YES, Field.Index.NO));

    // Create the text field for this document.                        
    Field textField = new Field("text", textStr, Field.Store.YES, Field.Index.TOKENIZED);

    // Set the boost value for the text.
    textField.setBoost(secInfo.wordBoost);

    // Establish whether to add words to the spellcheck dictionary.
    XTFTextAnalyzer analyzer = (XTFTextAnalyzer)indexWriter.getAnalyzer();
    analyzer.clearMisspelledFields();
    if (secInfo.spellFlag == SectionInfo.noSpell)
      analyzer.addMisspelledField("text");

    // Finally, add the text in the chunk to the index as a stored, indexed,
    // tokenized field.
    //
    doc.add(textField);

    try 
    {
      // Add the resulting list of fields (document) to the index.
      indexWriter.addDocument(doc);

      // Account for the new chunk added.
      chunkCount++;
    }

    // If anything went wrong adding the document to the index...
    catch (Throwable t) 
    {
      // Log the error.
      Trace.tab();
      Trace.error("*** Exception Adding Text to Index: " + t);
      Trace.untab();

      // And bail.
      if (t instanceof RuntimeException)
        throw (RuntimeException)t;
      else
        throw new RuntimeException(t);
    }
  } // indexText()

  ////////////////////////////////////////////////////////////////////////////

  /** Utility function to check if a string or a portion of a string is
   *  entirely whitespace. <br><br>
   *
   *  @param  str    String to check for all whitespace.
   *  @param  start  First character in string to check.
   *  @param  end    One index past the last character to check. <br><br>
   *
   *  @return
   *    <code>true</code> - The specified range of the string is all whitespace.
   *                        <br>
   *   <code>false</code> - The specified range of the string is <b>not</b> all
   *                        whitespace. <br><br>
   */
  private static boolean isAllWhitespace(String str, int start, int end) 
  {
    for (int i = start; i < end; i++)
      if (!Character.isWhitespace(str.charAt(i)))
        return false;

    return true;
  } // isAllWhitespace()

  ////////////////////////////////////////////////////////////////////////////

  /** Compacts multiple adjacent virtual words into a special "virtual word
   *  count" token.
   *
   *  @throws  IOException  Any exceptions generated by low level string
   *                        operations. <br><br>
   *
   *  @.notes
   *    For an explanation of "virtual words", see the main
   *    {@link XMLTextProcessor#insertVirtualWords(StringBuffer) insertVirtualWords()}
   *    method. <br><br>
   *
   *    A virtual word count consists of a special start marker, followed by
   *    the virtual word count, and an ending marker. Currently, the start and
   *    end markers are the same, allowing virtual word markers to be detected
   *    in the same way regardless of which direction a string is processed.
   *    <br><br>
   *
   *    The actual virtual word count marker character is defined by the
   *    {@link Constants#BUMP_MARKER BUMP_MARKER} member of the
   *    {@link Constants} class. <br><br>
   *
   */
  private void compactVirtualWords()
    throws IOException 
  {
    int i;

    // Get convienient versions of the special bump token marker and the 
    // virtual word string.
    // 
    char marker = Constants.BUMP_MARKER;
    String vWord = Constants.VIRTUAL_WORD;

    // Start by setting the compacted text buffer to the contents of the
    // accumulated text buffer.
    //
    compactedAccumText.setLength(0);
    compactedAccumText.append(accumText);

    // Convert the compacted text into a list of tokens we can use.
    String textStr = compactedAccumText.toString();
    FastStringReader reader = new FastStringReader(textStr);
    TokenStream tokenList = new FastTokenizer(reader);

    int posAdj = 0;
    Token theToken = null;

    // Look for blocks of virtual words, and turn them into special bump
    // tag notation for compactness.
    //
    boolean mustGetNextToken = true;
    for (;;) 
    {
      // Get the next token from the list (unless we already got it.)
      if (mustGetNextToken)
        theToken = tokenList.next();
      else
        mustGetNextToken = true;
      if (theToken == null)
        break;

      // Start with no virtual words encountered.
      int vWordCount = 0;

      // Mark the start and end of the current block of virtual words.
      int vRunStart = posAdj + theToken.startOffset();
      int vRunEnd = vRunStart;

      // For each virtual word we encounter in a row (possibly none)...
      while (vWord.equalsIgnoreCase(theToken.termText())) 
      {
        // If the previous token was also a virtual word...
        if (vWordCount > 0) 
        {
          // Make sure that only spaces separate them. If there's
          // punctuation in there, it's not safe to compact this one
          // with the previous.
          //
          if (!isAllWhitespace(textStr, vRunEnd - posAdj, theToken.startOffset())) 
          {
            // Okay, we must break out of this sequence. But
            // before we do, make sure the next go-round will start
            // with this virtual word, instead of the next token.
            //
            mustGetNextToken = false;

            // Stop the sequence.
            break;
          }
        }

        // Advance the end of run position to the end of the next 
        // virtual word. Note that this offset must be adjusted 
        // based on the number of virtual words we've already
        // removed, and the number of bump tokens we've inserted.
        // 
        vRunEnd = posAdj + theToken.endOffset();

        // Keep track of how many virtual words were in the run.
        vWordCount++;

        // Go and find the next virtual word. If there isn't one,
        // we're done finding the run.
        //
        theToken = tokenList.next();
        if (theToken == null)
          break;
      } // while( vWord.equalsIgnoreCase(theToken.termText()) )

      // If we found any virtual words...
      if (vWordCount > 0) 
      {
        // Determine how long the run of virtual words was and remove them.
        int vRunLen = vRunEnd - vRunStart;
        compactedAccumText.delete(vRunStart, vRunEnd);

        // Update the position adjustment to account for the run 
        // of virtual words we removed.
        //
        posAdj -= vRunLen;

        // Insert a special bump token equivalent to the number of 
        // virtual words removed by this run.
        //
        String bumpStr = marker + Integer.toString(vWordCount) + marker;
        compactedAccumText.insert(vRunStart, bumpStr);

        // Update the position adjustment to account for the special
        // bump token we just added to the text.
        //
        posAdj += bumpStr.length();
      } // if( vWordCount > 0 )
    } // for(;;)

    // Once all the bump tokens are in, we can remove any accumulated
    // unwanted spaces left over from the original insertion of the 
    // virtual words.
    //
    for (i = 0; i < compactedAccumText.length() - 1;) 
    {
      // Get the current and next characters.
      char currChar = compactedAccumText.charAt(i);
      char nextChar = compactedAccumText.charAt(i + 1);

      // If the current character is a space...
      if (currChar == ' ') 
      {
        // If the next character is also a space or a special bump tag
        // marker, we can remove the space. Note that we don't advance
        // since the deletion of the space effectively slid the remaining
        // characters one slot to the left.
        //
        if (nextChar == ' ' || nextChar == marker) {
          compactedAccumText.deleteCharAt(i);
          continue;
        }
      } // if (currChar == ' ' ) 

      // If we got to this point, we didn't find multiple spaces. So just
      // advance to the next character and try again.
      //
      i++;
    } // for( i = 0; i < compactedAccumText.length()-1; )

    /*
    The following text optimization is incomplete. It attempts to null out
    chunks that are all virtual words to save space in the index. However,
    it does not currently offset the next chunk's text if there are one or
    more intervening section bumps or proximity breaks.

    //////////////////////////////////////////////////////////////////////////
    // At this point, the current text may have compacted down to a single  //
    // special bump token, or some real text that ends in a special bump    //
    // token. Both waste space in the index, so lets delete them.           //
    //////////////////////////////////////////////////////////////////////////

    // Begin by determining the length of the current compacted text.
    int textLen = compactedAccumText.length();

    // If the compacted text doesn't end in a special bump marker, we're done.
    if( compactedAccumText.charAt(textLen-1) != marker ) return;

    // Back up to find the beginning special bump marker.
    for( i = textLen-2; compactedAccumText.charAt(i) != marker; i-- );

    // Once we've found the start marker, we can remove the special token.
    // Note that this will remove both ending bump tokens and zero out
    // chunks that only contain a special bump token.
    //
    compactedAccumText.delete( i, textLen );
    */
  } // compactVirtualWords()

  ////////////////////////////////////////////////////////////////////////////

  /** Utility function to check if a string contains the word <b>true</b> or
   *  <b>false</b> or the equivalent values <b>yes</b> or <b>no</b>.
   *
   *  @param  value          The string to check the value of.
   *
   *  @param  defaultResult  The boolean value to default to if the string
   *                         doesn't contain <b>true</b>, <b>false</b>,
   *                         <b>yes</b> or <b>no</b>. <br><br>
   *
   *  @return
   *    The equivalent boolean value specified by the string, or the default
   *    boolean value if the string doesn't contain <b>true</b>, <b>false</b>,
   *    <b>yes</b> or <b>no</b>. <br><br>
   *
   *  @.notes
   *    This function is primarily used to interpret values of on/off style
   *    attributes associated with prefiltered nodes in the XML source text.
   *    <br><br>
   */
  private boolean trueOrFalse(String value, boolean defaultResult) 
  {
    // If the string was 'true' or 'yes' return true.
    if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes"))
      return true;

    // If it was 'false' or 'no' return false.
    if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no"))
      return false;

    // Otherwise, return the specified default boolean value.
    return defaultResult;
  } // trueOrFalse()

  ////////////////////////////////////////////////////////////////////////////

  /** Process the attributes associated with an XML source text node. <br><br>
   *
   *  Sets internal flags and variables used during text processing based on
   *  any special attributes encountered in the given attribute list. <br><br>
   *
   *  @param  atts  The attribute list to process. <br><br>
   *
   *  @.notes
   *    This method is called to process a list of attributes associated with
   *    a node. These attributes are typically inserted into the XML source
   *    text by an XSL prefilter. <br><br>
   *
   *    Since many of these attributes nest (i.e., values for child nodes
   *    temporarily override parent attributes), the current state of the
   *    attributes is maintained on a "section info" stack. See the
   *    {@link XMLTextProcessor#section section} member for more
   *    details. <br><br>
   *
   *    For a description of node attributes handled by this method, see the
   *    {@link XMLTextProcessor} class description. <br><br>
   *
   */
  private void processNodeAttributes(Attributes atts) 
  {
    String valueStr;

    // Default all the section-based attributes to the current values
    // belonging to the parent section at the top of the stack.
    // 
    String sectionTypeStr = section.sectionType();
    float wordBoost = section.wordBoost();
    int sentenceBump = section.sentenceBump();
    int indexFlag = SectionInfo.parentIndex;
    int spellFlag = SectionInfo.parentSpell;
    int sectionBump = 0;
    String subDocumentStr = "";
    LinkedList metaInfo = null;

    // Process each of the specified node attributes, outputting warnings
    // for the ones we don't recognize.
    //
    for (int i = 0; i < atts.getLength(); i++) 
    {
      // Skip non-XTF related attributes.
      if (!xtfUri.equals(atts.getURI(i)))
        continue;

      String attName = atts.getLocalName(i);

      // If the current attribute is the section type, get it.      
      if (attName.equalsIgnoreCase("sectionType"))
        sectionTypeStr = atts.getValue(i);

      else if (attName.equalsIgnoreCase("sectionTypeAdd"))
        sectionTypeStr += " " + atts.getValue(i);

      // If the current attribute is the section bump, get it.    
      else if (attName.equalsIgnoreCase("sectionBump")) {
        valueStr = atts.getValue(i);
        sectionBump = Integer.parseInt(valueStr);
      }

      // If the current attribute is the word boost, get it. 
      else if (attName.equalsIgnoreCase("wordBoost")) {
        valueStr = atts.getValue(i);
        wordBoost = Float.parseFloat(valueStr);
      }

      // If the current attribute is the sentence bump, get it.
      else if (attName.equalsIgnoreCase("sentenceBump")) {
        valueStr = atts.getValue(i);
        sentenceBump = Integer.parseInt(valueStr);
      }

      // If the current attribute is the no-index flag...
      else if (attName.equalsIgnoreCase("noIndex")) 
      {
        // Determine the default "no index" flag value from the parent
        // section. Note that the default value is the inverse of the 
        // parent section value, as the final flag is an "index" flag
        // rather than a "no-index" flag.
        //   
        boolean defaultNoIndexFlag = (indexFlag == SectionInfo.index) ? false
                                     : true;

        // Get the value of the "no index" attribute.
        valueStr = atts.getValue(i);

        // Build the final index flag based on the attribute and default
        // values passed.
        //
        indexFlag = trueOrFalse(valueStr, defaultNoIndexFlag)
                    ? SectionInfo.noIndex : SectionInfo.index;
      } // else if( atts.getQName(i).equalsIgnoreCase("xtfNoIndex") )

      // If the current attribute is the index flag...
      else if (attName.equalsIgnoreCase("index")) 
      {
        // Determine the default "index" flag value from the parent
        // section.
        //
        boolean defaultIndexFlag = (section.indexFlag() == SectionInfo.index)
                                   ? true : false;

        // Get the value of the "index" attribute.
        valueStr = atts.getValue(i);

        // Build the final index flag based on the attribute and default
        // values passed.
        //
        indexFlag = trueOrFalse(valueStr, defaultIndexFlag) ? SectionInfo.index
                    : SectionInfo.noIndex;
      } // else if( atts.getQName(i).equalsIgnoreCase("xtfIndex") )

      // If the current attribute is the proximity break attribute...
      else if (attName.equalsIgnoreCase("proximityBreak")) 
      {
        // Get the proximity break value.
        valueStr = atts.getValue(i);

        // If a break is specifically requested, force a new chunk to
        // be started. This ensures that proximity matches wont occur
        // accross the break.
        //
        if (trueOrFalse(valueStr, false)) 
        {
          // Clear out any partially accumulated chunks.         
          forceNewChunk(section.peek());

          // Reset the chunk position to the node we're in now.
          chunkStartNode = -1;
          chunkWordOffset = -1;

          //Trace.tab();
          //Trace.debug("Proximity Break");
          //Trace.untab();
        }
      } // else if( atts.getQName(i).equalsIgnoreCase("xtfProximityBreak") )

      // If the current attribute is the word boost attribute...
      else if (attName.equalsIgnoreCase("wordBoost")) 
      {
        // Get the word boost value.
        valueStr = atts.getValue(i);
        float newBoost = Float.parseFloat(valueStr);

        // If the word boost changed...
        if (wordBoost != newBoost) 
        {
          // Clear out any partially accumulated chunks.         
          forceNewChunk(section.peek());

          // Reset the chunk position to the node we're in now.
          chunkStartNode = -1;
          chunkWordOffset = -1;

          // Hang on to the new boost value.
          wordBoost = newBoost;

          //Trace.tab();
          //Trace.debug("Word Boost: " + newBoost);
          //Trace.untab();
        }
      } // else if( atts.getQName(i).equalsIgnoreCase("xtfProximityBreak") )

      // If the current attribute is the spell flag...
      else if (attName.equalsIgnoreCase("spell")) 
      {
        // Determine the default spell flag value from the parent
        // section.
        //
        boolean defaultSpellFlag = (section.spellFlag() == SectionInfo.spell)
                                   ? true : false;

        // Get the value of the "misspelled" attribute.
        valueStr = atts.getValue(i);

        // Build the final spell flag based on the attribute and default
        // values passed.
        //
        spellFlag = trueOrFalse(valueStr, defaultSpellFlag) ? SectionInfo.spell
                    : SectionInfo.noSpell;

        // If the spell flag changed...
        if (spellFlag != section.spellFlag()) 
        {
          // Clear out any partially accumulated chunks.         
          forceNewChunk(section.peek());

          // Reset the chunk position to the node we're in now.
          chunkStartNode = -1;
          chunkWordOffset = -1;

          //Trace.tab();
          //Trace.debug("Spell: " + spellFlag);
          //Trace.untab();
        }
      } // else if( atts.getQName(i).equalsIgnoreCase("xtfIndex") )
      
      // If the current attribute indicates a new subdocument, get it.  
      else if (attName.equalsIgnoreCase("subDocument")) {
        subDocumentStr = atts.getValue(i);
        
        // If the subdocument has changed...
        if (subDocumentStr != section.subDocument()) 
        {
          // Clear out any partially accumulated chunks.         
          forceNewChunk(section.peek());

          // Insert a docInfo chunk right here, if there are chunks to include.
          if (chunkCount > 0)
            saveDocInfo(section.peek());

          // We're now in a new section, and a new subdocument.
          chunkStartNode = -1;
          chunkWordOffset = -1;
          
          // A new subdocument means forking the metadata as well.
          metaInfo = (LinkedList) section.metaInfo().clone();
        }
      }

      // If we got to this point, and we've encountered an unrecognized 
      // xtf:xxxx attribute, display a warning message (if enabled for 
      // output.)
      //
      else {
        Trace.tab();
        Trace.warning(
          "*** Warning: Unrecognized XTF Attribute [ " + atts.getQName(i) +
          "=" + atts.getValue(i) + " ].");
        Trace.untab();
      }
    } // for( int i = 0; i < atts.getLength(); i++ )

    // Finally, push the new section info. Note that if all the values passed
    // match the parent values exactly, this call will simply increase the 
    // depth of the parent to save space.
    //
    section.push(indexFlag,
                 sectionTypeStr,
                 sectionBump,
                 wordBoost,
                 sentenceBump,
                 spellFlag,
                 subDocumentStr,
                 metaInfo);
  } // private ProcessNodeAttributes()

  ////////////////////////////////////////////////////////////////////////////

  /** Save document information associated with a collection of chunks. <br><br>
   *
   *  This method saves a special document summary information chunk to the
   *  Lucene database that binds all the indexed text chunks for a document
   *  back to the original XML source text.
   *
   *  @.notes
   *    The document summary chunk is the last chunk written to a Lucene
   *    database for a given XML source document. Its presence or absence
   *    then can be used to identify whether or a document was completely
   *    indexed or not. The absence of a document summary for any given
   *    text chunk implies that indexing was aborted before the document
   *    was completely indexed. This property of document summary chunks
   *    is used by the  {@link IdxTreeCleaner} class to stript out any
   *    partially indexed documents.<br><br>
   *
   *    The document summary includes the relative path to the original
   *    XML source text, the number of chunks indexed for the document,
   *    a unique key that associates this summary with all the indexed
   *    text chunks, the date the document was added to the index, and any
   *    meta-data associated with the document. <br><br>
   */
  private void saveDocInfo(SectionInfo secInfo) 
  {
    // Make a new document, to which we can add our fields.     
    Document doc = new Document();

    // Add a header flag as a stored, indexed, non-tokenized field 
    // to the document database.
    //
    doc.add(new Field("docInfo", "1", 
                      Field.Store.YES, Field.Index.UN_TOKENIZED));

    // Add the number of chunks in the index for this document.
    doc.add(new Field("chunkCount",
                      Integer.toString(chunkCount),
                      Field.Store.YES, Field.Index.NO));
    
    // Reset the chunk count, in case this is just a subdocument within
    // the larger text.
    //
    chunkCount = 0;

    // Add the key to the document header as a stored, indexed, 
    // non-tokenized field.
    //
    doc.add(new Field("key", curIdxSrc.key(), 
                      Field.Store.YES, Field.Index.UN_TOKENIZED));

    // If record number is non-zero, write it out as a stored, non-indexed field.
    int recordNum = curIdxRecord.recordNum();
    if (recordNum > 0) {
      doc.add(new Field("recordNum",
                        Integer.toString(recordNum),
                        Field.Store.YES, Field.Index.NO));
    }

    // If subdocument name is non-empty, write it out as a stored, non-indexed field.
    // Why not indexed? Because it's also put on the individual text chunks, and
    // on those it is indexed. Search is across text chunks, not docInfo chunks.
    //
    if (secInfo.subDocument != null) {
      doc.add(new Field("subDocument",
                        secInfo.subDocument,
                        Field.Store.YES, Field.Index.NO));
    }

    // Determine when the file was last modified.
    File srcPath = curIdxSrc.path();
    if (srcPath != null) 
    {
      String fileDateStr = DateTools.timeToString(
        srcPath.lastModified(), DateTools.Resolution.MILLISECOND);

      // Add the XML file modification date as a stored, non-indexed, 
      // non-tokenized field.
      //
      doc.add(new Field("fileDate", fileDateStr, Field.Store.YES, Field.Index.NO));
    }

    // Get the analyzer that will be used to tokenize fields. Tell it to
    // forget what it knows about facet fields (we'll re-mark them below.)
    //
    XTFTextAnalyzer analyzer = (XTFTextAnalyzer)indexWriter.getAnalyzer();
    analyzer.clearFacetFields();

    // Make sure we got meta-info for this document.
    if (secInfo.metaInfo.isEmpty()) {
      Trace.tab();
      Trace.warning("*** Warning: No meta data found for document.");
      Trace.untab();
    }
    else 
    {
      // Get an iterator so we can add the various meta fields we found for
      // the document (most usually, things like author, title, etc.)
      //
      Iterator metaIter = secInfo.metaInfo.iterator();

      // Add all the meta fields to the docInfo chunk.
      while (metaIter.hasNext()) 
      {
        // Get the next meta field.
        MetaField metaField = (MetaField)metaIter.next();

        // If it's a facet field, tell the analyzer so it knows to apply
        // special tokenization.
        //
        if (metaField.isFacet && metaField.index) {
          metaField.tokenize = true;
          analyzer.addFacetField(metaField.name);
        }

        // If it's marked as misspelled, inform the analyzer so it doesn't
        // add the field data to the spelling correction dictionary.
        //
        if (!metaField.spell && metaField.index)
          analyzer.addMisspelledField(metaField.name);

        // Add it to the document. Store, index, and/or tokenize as
        // specified by the field.
        //
        Field docField = new Field(metaField.name,
                                   metaField.value,
                                   metaField.store ? Field.Store.YES : Field.Store.NO,
                                   metaField.index ?
                                     (metaField.tokenize ? 
                                          Field.Index.TOKENIZED 
                                        : Field.Index.UN_TOKENIZED)
                                     : Field.Index.NO);
        docField.setBoost(metaField.wordBoost);
        doc.add(docField);

        // Record which fields are tokenized, the first time we notice
        // the fact. It doesn't matter which document we do this on, as
        // the reader code simply iterates the terms.
        //
        if (metaField.tokenize && !metaField.isFacet) 
        {
          if (!tokenizedFields.contains(metaField.name)) {
            addToTokenizedFieldsFile(metaField.name);
            tokenizedFields.add(metaField.name);
          }
        }
      } // while(  metaIter.hasNext() )
    } // else( metaInfo != null && !metaInfo.isEmpty() )

    try 
    {
      // Add the document info block to the index.
      indexWriter.addDocument(doc);
    }

    // If something went wrong...
    catch (Throwable t) 
    {
      // Log the problem.
      Trace.tab();
      Trace.error("*** Exception Adding docInfo to Index: " + t);
      Trace.untab();

      if (t instanceof RuntimeException)
        throw (RuntimeException)t;
      else
        throw new RuntimeException(t);
    }
  } // saveDocInfo()

  ////////////////////////////////////////////////////////////////////////////

  /**
   * Adds a field to the on-disk list of tokenized fields for an index.
   * Exceptions are handled internally and thrown as RuntimeException.
   */
  private void addToTokenizedFieldsFile(String field)
  {
    try 
    {
      File tokFieldsFile = new File(
          Path.normalizePath(indexPath + "tokenizedFields.txt"));
      FileWriter writer = new FileWriter(tokFieldsFile, true /*append*/);
      writer.append(field + "\n");
      writer.close();
    }

    // If something went wrong...
    catch (Throwable t) 
    {
      // Log the problem.
      Trace.tab();
      Trace.error("*** Exception Adding to tokenizedFields.txt: " + t);
      Trace.untab();

      if (t instanceof RuntimeException)
        throw (RuntimeException)t;
      else
        throw new RuntimeException(t);
    }
  }

  ////////////////////////////////////////////////////////////////////////////

  /** Returns a normalized version of the base path of the Lucene database
   *  for an index. <br><br>
   *
   *  @throws IOException  Any exceptions generated retrieving the path for
   *                       a Lucene database. <br><br>
   */
  private String getIndexPath()
    throws IOException 
  {
    String idxPath = Path.resolveRelOrAbs(xtfHomePath, indexInfo.indexPath);
    return Path.normalizePath(idxPath);
  } // private getIndexPath()

  ////////////////////////////////////////////////////////////////////////////

  /** Check to see if the current XML source text file exists in the Lucene
   *  database, and if so, whether or not it needs to be updated. <br><br>
   *
   *  @return
   *    <code>0</code> - Specified XML source document not found in the Lucene
   *                     database. <br>
   *
   *    <code>1</code> - Specified XML source document found in the index, and
   *                     the index is up-to-date. <br>
   *
   *    <code>2</code> - Specified XML source document is in the index, but
   *                     the source text has changed since it was last indexed.
   *                     <br><br>
   *
   *  @.notes
   *     The XML source document checked by this function is specified by the
   *     {@link XMLTextProcessor#curIdxSrc curIdxSrc} member. <br><br>
   *
   *     An XML source document needs reindexing if its modification date
   *     differs from the modification date stored in the summary info chunk
   *     the last time it was indexed. <br><br>
   */
  private int checkFile(IndexSource srcInfo)
    throws IOException 
  {
    // We need to find the docInfo chunk that contains the specified
    // file. So construct a boolean query looking for a chunk with 
    // a "docInfo" field AND a "key" field containing the specified
    // source file key.
    //
    BooleanQuery query = new BooleanQuery();
    Term docInfo = new Term("docInfo", "1");
    Term keyTerm = new Term("key", srcInfo.key());
    query.add(new TermQuery(docInfo), BooleanClause.Occur.MUST);
    query.add(new TermQuery(keyTerm), BooleanClause.Occur.MUST);

    // Use the query to see if the document is in the index..
    boolean docInIndex = false;
    boolean docChanged = false;

    Hits match = indexSearcher.search(query);
    if (match.length() > 0) 
    {
      // Flag that the document is in the index.
      docInIndex = true;

      // Get the file modification date from the "docInfo" chunk.
      Document doc = match.doc(0);
      String indexDateStr = doc.get("fileDate");

      // See what the date is on the actual source file right now.
      File srcPath = srcInfo.path();
      String fileDateStr = DateTools.timeToString(
        srcPath.lastModified(), 
        DateTools.Resolution.MILLISECOND);

      // If the dates are different...
      if (fileDateStr.compareTo(indexDateStr) != 0) 
      {
        // Delete the old lazy file, if any. Might as well delete any
        // empty parent directories as well.
        //
        File lazyFile = IndexUtil.calcLazyPath(new File(xtfHomePath),
                                               indexInfo,
                                               srcPath,
                                               false);
        Path.deletePath(lazyFile.toString());

        // And flag that we need to re-add them.
        docInIndex = true;
        docChanged = true;

        ////////////////////////////////////////////////////////
        //                                                    //                  
        // (We reindex like this with a 'remove and add'      //
        //  because Lucene doesn't have a 'reindex document' //
        //  operation.)                                       //
        //                                                    //
        ////////////////////////////////////////////////////////
      } // if( fileDateStr.compareTo(indexDateStr) != 0 ) 
    } // if( match.length() > 0 )

    // Now let the caller know the status.
    if (!docInIndex)
      return 0;
    if (!docChanged)
      return 1;
    return 2;
  } // checkFile()

  ////////////////////////////////////////////////////////////////////////////

  /**
   * Runs an optimization pass (which can be quite time-consuming) on the
   * currently open index. Optimization speeds future query access to the
   * index.
   */
  public void optimizeIndex()
    throws IOException 
  {
    try 
    {
      // Open the index writer (which closes the reader and searcher).
      openIdxForWriting();

      // Run the optimizer.
      indexWriter.optimize();
    }
    catch (IOException e) {
      Trace.tab();
      Trace.error("*** Exception Optimizing Index: " + e);
      Trace.untab();

      throw e;
    }
    finally {
      if (indexWriter != null)
        indexWriter.close();
      indexWriter = null;
    }
    assert indexWriter == null : "indexWriter not closed in finally block";
  } // optimizeIndex()

  ////////////////////////////////////////////////////////////////////////////

  /** Open the active Lucene index database for reading (and deleting, an
   *  oddity in Lucene). <br><br>
   *
   *  @throws
   *    IOException  Any exceptions generated during the creation of the
   *                 Lucene database writer object.
   *
   *  @.notes
   *    This method attempts to open the Lucene database specified by the
   *    {@link XMLTextProcessor#indexPath indexPath} member for reading
   *    and/or deleting. It is strange that you delete things from a
   *    Lucene index by using an IndexReader, but hey, whatever floats your
   *    boat man.
   *    <br><br>
   */
  private void openIdxForReading()
    throws IOException 
  {
    if (indexWriter != null)
      indexWriter.close();
    indexWriter = null;

    if (spellWriter != null)
      spellWriter.close();
    spellWriter = null;

    if (indexReader == null)
      indexReader = IndexReader.open(indexPath);

    if (indexSearcher == null)
      indexSearcher = new IndexSearcher(indexReader);
  } // openIdxForReading()

  /** Open the active Lucene index database for writing. <br><br>
   *
   *  @throws
   *    IOException  Any exceptions generated during the creation of the
   *                 Lucene database writer object.
   *
   *  @.notes
   *    This method attempts to open the Lucene database specified by the
   *    {@link XMLTextProcessor#indexPath indexPath} member for writing.
   *    <br><br>
   */
  private void openIdxForWriting()
    throws IOException 
  {
    // Close the reader and searcher, since doing so will make indexing 
    // go much more quickly.
    //
    if (indexSearcher != null)
      indexSearcher.close();
    if (indexReader != null)
      indexReader.close();
    indexSearcher = null;
    indexReader = null;

    // If already open for writing, it would be bad to do it over again.
    if (indexWriter != null)
      return;

    // Make an analyzer that does all kinds of special stuff for us.
    XTFTextAnalyzer analyzer = new XTFTextAnalyzer(stopSet, pluralMap, accentMap);

    // Create an index writer, using the selected index db Path
    // and create mode. Pass it our own text analyzer. 
    //
    indexWriter = new IndexWriter(indexPath, analyzer, false);

    // Since we end up adding tons of little 'documents' to Lucene, it's much 
    // faster to queue up a bunch in RAM before sorting and writing them out. 
    // This limit gives good speed, but requires quite a bit of RAM (probably
    // 100 megs is about right.)
    //
    indexWriter.setMaxBufferedDocs(100);

    // If requested to make a spellcheck dictionary for this index, attach 
    // a spelling writer to the text analyzer, so that tokenized words get 
    // passed to it and queued.
    //
    if (indexInfo.createSpellcheckDict) {
      if (spellWriter == null) {
        spellWriter = SpellWriter.open(new File(indexPath + "spellDict/"));
        spellWriter.setStopwords(stopSet);
        spellWriter.setMinWordFreq(3);
      }
      analyzer.setSpellWriter(spellWriter);
    }
  } // private openIdxForWriting()  

  ////////////////////////////////////////////////////////////////////////////
  private class MetaField 
  {
    public String name;
    public String value;
    public boolean store;
    public boolean index;
    public boolean tokenize;
    public boolean isFacet;
    public boolean spell;
    public float wordBoost;

    public MetaField(String name, boolean store, boolean index,
                     boolean tokenize, boolean isFacet, boolean spell,
                     float wordBoost) 
    {
      this.name = name;
      this.store = store;
      this.index = index;
      this.tokenize = tokenize;
      this.isFacet = isFacet;
      this.spell = spell;
      this.wordBoost = wordBoost;
    }
  } // private class MetaField

  ////////////////////////////////////////////////////////////////////////////
  private class FileQueueEntry 
  {
    public IndexSource idxSrc;
    public boolean deleteFirst;

    public FileQueueEntry(IndexSource idxSrc, boolean deleteFirst) {
      this.idxSrc = idxSrc;
      this.deleteFirst = deleteFirst;
    }
  } // private class FileQueueEntry
} // class XMLTextProcessor
