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
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;

import net.sf.saxon.Controller;
import net.sf.saxon.PreparedStylesheet;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.ReceivingContentHandler;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.om.NamePool;

import org.apache.lucene.document.DateField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.FSDirectory;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.Token;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import org.cdlib.xtf.lazyTree.LazyDocument;
import org.cdlib.xtf.lazyTree.LazyKeyManager;
import org.cdlib.xtf.lazyTree.LazyTreeBuilder;
import org.cdlib.xtf.lazyTree.RecordingNamePool;
import org.cdlib.xtf.servletBase.StylesheetCache;
import org.cdlib.xtf.textEngine.IdxConfigUtil;
import org.cdlib.xtf.util.DocTypeDeclRemover;
import org.cdlib.xtf.util.FastStringReader;
import org.cdlib.xtf.util.FastTokenizer;
import org.cdlib.xtf.util.Path;
import org.cdlib.xtf.util.Trace;
import org.cdlib.xtf.util.XTFSaxonErrorListener;


////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

/**
 * This class performs the actual parsing of the XML source text files and 
 * generates index information for it. <br><br>
 * 
 * The <code>XMLTextParser</code> class uses the configuration information 
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
  private final static int bufStartSize = 16*1024;
  
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
  
  /** Maximum number of adjacent stop words to combine. See
   *  {@link IndexInfo#stopWords} for details.
   */
  private int stopCombine = 0;
  
  /** Flag indicating that a new chunk needs to be created. Set to <code>true</code>
   *  when a node's section name changes or a <code>proximitybreak</code> attribute
   *  is encountered.
   */
  private boolean forcedChunk = false;
  
  /** A reference to the configuration information for the current index being
   *  updated. See the {@link IndexerConfig} and {@link XMLConfigParser} class 
   *  descriptions for more details.
   */
  private IndexerConfig configInfo;
  
  /** List of files to process. For an explanation of file queuing, see the
   *  {@link XMLTextProcessor#processQueuedTexts() processQueuedTexts()} method.
   */
  private LinkedList fileQueue = new LinkedList();
  
  /** The location of the XML source text file currently being indexed. For
   *  more information about this structure, see the 
   * {@link SrcTextInfo} class.
   */
  private SrcTextInfo srcText;
  
  /** The base directory for the current Lucene database specified by the
   *  {@link XMLTextProcessor#configInfo configInfo} member.
   */ 
  private String indexPath;
 
  /** Used to cache the input filter stylesheet, and any xslKeysFrom
   *  stylesheets, so we don't have to parse them over and over.
   */
  private static StylesheetCache stylesheetCache = 
                                          new StylesheetCache( 50, 0, true );
  
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

  /** Actual file path of the "lazy tree" being built.
   *  See {@link XMLTextProcessor#lazyBuilder lazyBuilder} for more details.
   */
  private File lazyFile;
  
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
  private int inMeta  = 0;
  
  /** List of meta-info currently accumulated for the current meta-tag. */
  private LinkedList metaInfo;  
  
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
   *  to the index currently defined by the 
   *  {@link XMLTextProcessor#configInfo configInfo}
   *  member.
   */
  private IndexWriter indexWriter;
  
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
  private SectionInfoStack section = new SectionInfoStack();
  
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
   * @param  cfgInfo  A config structure containing information about the index
   *                  to open. <br><br>
   *  
   * @.notes
   *   This method will create an index if it doesn't exist, or truncate an index
   *   that does exist if the <code>clean</code> flag is set in the 
   *   <code>cfgInfo</code> structure. <br><br>
   * 
   *   This method makes a private internal reference 
   *   {@link XMLTextProcessor#configInfo configInfo}
   *   to the passed configuration structure for use by other methods in this 
   *   class. <br><br>
   * 
   * @throws
   *   IOException  Any I/O exceptions that occurred during the opening,
   *                creation, or truncation of the Lucene index. <br><br>
   * 
   */

  public void open( IndexerConfig cfgInfo ) throws IOException
  {
  
      fileQueue = new LinkedList();
      
      try {
          
          // Get a reference to the passed in configuration that all the
          // methods can access.
          //
          configInfo = cfgInfo;
          
          // If no XTF home directory specified, assume it is the same
          // directory as the config file.
          //
          if( configInfo.xtfHomePath == null ) {
              configInfo.xtfHomePath = 
                  new File(configInfo.cfgFilePath).getParentFile()
                                                  .getCanonicalPath();
          }
    
          // Determine where the index database is located.
          indexPath = getIndexPath();
            
          // Determine the set of stop words to remove (if any)
          if( configInfo.indexInfo.stopWords != null )
              stopSet = NgramStopFilter.makeStopSet( configInfo.indexInfo.stopWords );
          
          // If we were told to create a clean index...
          if( configInfo.clean ) {
              
              // Create the index db Path in case it doesn't exist yet.
              Path.createPath( indexPath );
          
              // And then create the index.
              createIndex( indexPath );
              configInfo.clean = false;
          }
          
          // Otherwise...
          else {
            
              // If it doesn't exist yet, create the index db Path. If it 
              // does exist, this call will do nothing.
              //
              Path.createPath( indexPath );
          
              // Get a Lucene style directory.
              FSDirectory idxDir = FSDirectory.getDirectory( indexPath, false );
            
              // If an index doesn't exist there, create it.
              if( !IndexReader.indexExists( idxDir ) ) createIndex( indexPath );              
            
          } // else( !configInfo.clean )

          // Try to open the index for reading and searching.
          indexReader   = IndexReader.open( indexPath );
          indexSearcher = new IndexSearcher( indexReader );
          
          // Locate the index information document
          Hits match = indexSearcher.search( 
                              new TermQuery( new Term("indexInfo", "1")) ); 
          
          // If we can't find it, then this index is either corrupt or
          // very old. Fail in either case.
          //
          if( match.length() == 0 )
              throw new RuntimeException( "Index missing indexInfo" );
          
          Document doc = match.doc( 0 );
          
          // Ensure that the chunk size and overlap are the same.
          if( Integer.parseInt( doc.get("chunkSize") ) !=
              configInfo.indexInfo.getChunkSize() ) 
          {
              throw new RuntimeException( 
                         "Index chunk size (" + doc.get("chunkSize") +
                         ") doesn't match config (" + 
                         configInfo.indexInfo.getChunkSize() + ")" );
          }
          
          if( Integer.parseInt(doc.get("chunkOvlp")) !=
              configInfo.indexInfo.getChunkOvlp() ) {
                
              throw new RuntimeException( 
                         "Index chunk overlap (" + doc.get("chunkOvlp") +
                         ") doesn't match config (" + 
                         configInfo.indexInfo.getChunkOvlp() + ")" );
          }
          
          // Ensure that the stop-word settings are the same.
          String stopWords = configInfo.indexInfo.stopWords;
          if( stopWords == null )
              stopWords = "";
          if( !doc.get("stopWords").equals(stopWords) ) {

              throw new RuntimeException( 
                         "Index stop words (" + doc.get("stopWords") +
                         ") doesn't match config (" + 
                         configInfo.indexInfo.stopWords + ")" );
          }
          
      } // try
      
      catch( IOException e ) {
          
          // Log the error caught.
          Trace.tab();
          Trace.error( "*** IOException Opening Index: " + e );
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
  public void close() throws IOException

  {
      
      if( indexWriter   != null ) indexWriter.close();
      if( indexSearcher != null ) indexSearcher.close();
      if( indexReader   != null ) indexReader.close();
      
      indexWriter   = null;
      indexSearcher = null;
      indexReader   = null;

  } // close() 

  
  ////////////////////////////////////////////////////////////////////////////

  /**
   * Utility function to create a new Lucene index database for reading or 
   * searching. <br><br>
   * 
   * This method is used by the {@link XMLTextProcessor#createIndex(String) createIndex() }
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
   *                {@link XMLTextProcessor#configInfo configInfo}
   *                structure. <br><br>
   * 
   */
  
  private void createIndex( String indexPath ) throws IOException

  {

    try {
        
        // Delete the old directory.
        Path.deleteDir( new File(indexPath) );
        
        // First, make the index.
        indexWriter = new IndexWriter( 
                     indexPath, 
                     new XTFTextAnalyzer( stopSet, 
                                          configInfo.indexInfo.getChunkSize(),
                                          compactedAccumText ), 
                     true );
        
        // Then add the index info chunk to it.
        Document doc = new Document();
        doc.add( Field.Keyword( "indexInfo", "1" ) );
        doc.add( Field.UnIndexed( "chunkSize", 
                                  configInfo.indexInfo.getChunkSizeStr()) );
        doc.add( Field.UnIndexed( "chunkOvlp", 
                                  configInfo.indexInfo.getChunkOvlpStr()) );
        
        String stopWords = configInfo.indexInfo.stopWords;
        if( stopWords == null )
            stopWords = "";
        doc.add( Field.UnIndexed( "stopWords", stopWords ) );
        indexWriter.addDocument( doc );
    
    } // try
    
    finally {
        
        // Finish up.
        if( indexWriter != null ) {
            indexWriter.close();
            indexWriter = null;
        }
        
    } // finally
      
  } // createIndex()
  
  
  ////////////////////////////////////////////////////////////////////////////

  /** Queue a source text file for (re)indexing. <br><br>
   * 
   *  This method generates a relocatable path identifier for the specified XML 
   *  source file and then adds it to a queue of files to be (re)indexed. 
   * 
   *  @param xmlTextFile  The source XML text file to add to the queue of 
   *                      files to be indexed/reindexed. <br><br>
   *  
   *  @.notes
   *    For more about why source text files are queued, see the 
   *    {@link XMLTextProcessor#processQueuedTexts() processQueuedTexts()} 
   *    method. <br><br>
   *     
   */
  public void queueText(
      
      SrcTextInfo srcInfo
      
  ) throws ParserConfigurationException, 
           SAXException, 
           IOException

  {
    
    // We need to refer to the file in a way that isn't dependent on the
    // particular location the index is at right now. So calculate a key
    // that just contains the index name and the part of the path after that
    // index's data directory.
    //
    File srcFile = new File( srcInfo.source.getSystemId() );
    String key = IdxConfigUtil.calcDocKey( new File(configInfo.xtfHomePath),
                                           configInfo.indexInfo, srcFile );
    
    // Make a pretty version of the path for display purposes. It's basically
    // just the key, without the index name on it.
    //
    srcInfo.prettyPath = key.substring( key.indexOf(':') + 1 );

    // Pack all the information regarding this file into a handy unit that we
    // can check and queue.
    //
    srcInfo.key = key;
    this.srcText = srcInfo;
    
    // Check the status of this file.
    int ret = checkFile();
    
    // If the index is already up to date, we're done.
    if( ret == 1 ) return;
    
    // Otherwise, queue it to be indexed.
    fileQueue.add( srcText );

  } // queueText()
  
  
  ////////////////////////////////////////////////////////////////////////////

  /** Index a single source file in one step, no queueing necessary.
   *  If a document with the same key already exists, it is first
   *  deleted.<br><br>
   * 
   *  Note that this is inefficient if adding many documents to the
   *  index, but serves well for a single document.<br><br>
   * 
   *  @param inStream   An XML byte stream representing the document
   *                    to index.<br><br>
   *  @param key        The key to associate the document with in the
   *                    index.<br><br>
   *  
   */
  public void indexSingleStream(
      
      InputStream   inStream,
      String        key
      
  ) throws ParserConfigurationException, 
           SAXException, 
           IOException

  {
    // If any old version of this document exists, delete it.
    indexReader.delete( new Term( "key", key ) );
      
    // Pack all the information regarding this doc into a handy unit 
    // that we queue, and queue it.
    //
    srcText = new SrcTextInfo();
    srcText.source = new InputSource( inStream );
    srcText.key = key;
    srcText.format = "XML";
    fileQueue.add( srcText );

    // And finally, process our queue of length one.
    processQueuedTexts();
    
  } // indexSingleStream()
  
  
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
  public boolean removeSingleDoc(
      
      File          srcFile,
      String        key
      
  ) throws ParserConfigurationException, 
           SAXException, 
           IOException

  {
    // If any old version of this document exists, delete it.
    int nDeleted = indexReader.delete( new Term( "key", key ) );

    // If there might be a lazy file...
    if( srcFile != null )
    {
        // Delete the old lazy file, if any. Might as well delete any
        // empty parent directories as well.
        //
        lazyFile = IdxConfigUtil.calcLazyPath(
                                         new File(configInfo.xtfHomePath),
                                         configInfo.indexInfo, 
                                         srcFile,
                                         false );
        Path.deletePath( lazyFile.toString() );
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
  public boolean docExists(
      
      String        key
      
  ) throws ParserConfigurationException, 
           SAXException, 
           IOException

  {
      // We need to find the docInfo chunk that contains the specified
      // file. So construct a boolean query looking for a chunk with 
      // a "docInfo" field AND a "key" field containing the specified
      // source file key.
      //
      BooleanQuery query = new BooleanQuery();
      Term docInfo = new Term( "docInfo", "1" );
      Term srcPath = new Term( "key", key );
      query.add( new TermQuery( docInfo ), true, false );
      query.add( new TermQuery( srcPath ), true, false );
      
      // Use the query to see if the document is in the index..
      boolean docInIndex = false;
      boolean docChanged = false;

      Hits match = indexSearcher.search( query );
      
      // Let the caller know if we found a match.
      return match.length() > 0;
      
  } // docExists()
  
  
  ////////////////////////////////////////////////////////////////////////////

  /** Process the list of files queued for indexing or reindexing. <br><br>
   * 
   *  This method iterates through the list of queued source text files,
   *  calling the {@link XMLTextProcessor#processText(XMLTextProcessor.SrcTextInfo,int) processText()}
   *  method to (re)index the file as needed. <br><br>
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

  public void processQueuedTexts() throws IOException
  {
      // Close the reader and searcher, since doing so will make indexing 
      // go much more quickly.
      //
      if( indexSearcher != null ) indexSearcher.close();
      if( indexReader   != null ) indexReader.close();
      indexSearcher = null;
      indexReader   = null;

      // Initialize the string buffers for accumulating and compacting the 
      // text to index.
      //
      blurbedText        = new StringBuffer( bufStartSize );
      accumText          = new StringBuffer( bufStartSize );
      compactedAccumText = new StringBuffer( bufStartSize );
      
      try {
          // Open the index writer.
          openIdxForWriting();
          
          int nFiles  = fileQueue.size();
          int fileNum = 0;
          
          // Process each queued file.
          while( !fileQueue.isEmpty() ) 
          {
              SrcTextInfo idxFile = (SrcTextInfo) fileQueue.removeFirst();
              int percent = (nFiles <= 1) ? -1 :
                            ((fileNum+1) * 100 / nFiles);
              processText( idxFile, percent );
              fileNum++;
          }
      }
      catch( IOException e ) {
          Trace.tab();
          Trace.error( "*** Exception Processing Queued Texts: " + e );
          Trace.untab();
      }
      
      finally {
          if( indexWriter != null ) indexWriter.close();
          indexWriter = null;
      }
          
      assert indexWriter == null : "indexWriter not closed in finally block";
      
  } // processQueuedTexts()
  

  ////////////////////////////////////////////////////////////////////////////
  
  /** Add the specified XML source text to the active Lucene index.
   * 
   *  This method indexes the specified XML source text file, adding it to the
   *  Lucene database currently specified by the 
   * {@link XMLTextProcessor#configInfo configInfo} member.
   * 
   * @param file    The XML source text file to process.
   * 
   * @param percent The percentage of source texts that will have been
   *                processed when indexing for this file has completed.
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
  private int processText( 
  
      SrcTextInfo file, 
      int         percent 
  
  ) throws IOException
  
  {
    
    // Clear the text buffers.
    accumText.setLength( 0 );
    compactedAccumText.setLength( 0 );
    
    // Record the file's parameters so other methods can get to them easily. Then
    // tell the user what we're doing.
    //
    srcText = file;
    String msg = "";
    if( percent >= 0 )
        msg = "(" + percent + "%) ";
    Trace.info( msg + "Indexing [" + file.prettyPath + "] ... " );
    
    // Figure out where to put the lazy tree file. If an old one already exists,
    // toss it. Also make sure the directory exists.
    //
    if( srcText.source.getSystemId() != null ) {
        File srcFile = new File( srcText.source.getSystemId() );
        lazyFile = IdxConfigUtil.calcLazyPath( 
                new File(configInfo.xtfHomePath),
                configInfo.indexInfo,
                srcFile,
                true );
        if( lazyFile.canRead() )
            lazyFile.delete();
        
        // We need to make sure to use a special name pool that records all
        // possible name codes. We can use it later to iterate through and
        // find all the registered keys (it's the only way.)
        //
        NamePool namePool = NamePool.getDefaultNamePool();
        if( !(namePool instanceof RecordingNamePool) ) {
            namePool = new RecordingNamePool();
            NamePool.setDefaultNamePool( namePool );
        }
        
        // While we parse the source document, we're going to also build up a tree
        // that will be written to the lazy file.
        //
        lazyBuilder = new LazyTreeBuilder();
        lazyReceiver = lazyBuilder.begin( lazyFile );
        
        lazyBuilder.setNamePool( namePool );
        
        lazyHandler = new ReceivingContentHandler();
        lazyHandler.setReceiver( lazyReceiver );
        lazyHandler.setConfiguration( lazyReceiver.getConfiguration() );
    }
    else {
        lazyBuilder  = null;
        lazyReceiver = null;
        lazyHandler  = null;
    }
          
    // Determine how many words should be in each chunk.
    chunkWordSize = configInfo.indexInfo.getChunkSize();
    
    // Determine the first word in each chunk that the next overlapping
    // chunk should start at.
    //
    chunkWordOvlp      = configInfo.indexInfo.getChunkOvlp();
    chunkWordOvlpStart = chunkWordSize - chunkWordOvlp;
    
    // Reset the node tracking info to the first node in the document.
    // Note that we start the current node number at 0, because the
    // first call to startElement() will pre-increment it to one (which is
    // correct since the document is node zero).
    //
    curNode             =  0;
    chunkStartNode      =  -1;
    nextChunkStartNode  =  -1;

    nodeWordCount       =  0;
   
    chunkWordCount      =  0;
    chunkWordOffset     =  0;

    nextChunkStartIdx   =  0;
    nextChunkWordCount  =  0;
    nextChunkWordOffset =  0;
  
    forcedChunk         =  false;
    inMeta              =  0;
      
    // Reset the meta info for the new document.
    metaBuf.setLength( 0 );
    metaInfo = null;
    
    // Reset count of how many chunks we've written.
    chunkCount = 0;
    
    // Create an initial unnamed section with depth zero, indexing turned on, 
    // a blank section name, no section bump, a word bump of 1, no word boost,
    // and a default sentence bump.
    //
    section.push();
    
    // Now parse it.
    int result = parseText( file );
    
    // Regardless of result, finish the lazy tree so we don't leave everything
    // hanging open.
    //
    if( lazyBuilder != null ) {
        lazyBuilder.finish( lazyReceiver );
    
        // If a stylesheet has been specified that contains xsl:key defs
        // to apply to the lazy tree, do so now.
        //
        if( result == 0 ) {
            Templates displayStyle = srcText.displayStyle;
            if( displayStyle != null ) {
                try {
                    precacheXSLKeys( displayStyle );
                }
                catch( IOException e ) {
                    throw e;
                }
                catch( Exception e ) {
                    throw new IOException( 
                        "Error pre-caching XSL keys from " +
                        "display stylesheet \"" + displayStyle + "\": " + e );
                }
            }
        }
    }

    // If it went well, let the user know.
    if( result == 0 )
          Trace.more( Trace.info, "Done." );
    
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
   *  @param file The XML source document to parse. <br><br>
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
   *    If present in the {@link XMLTextProcessor#configInfo configInfo} member,
   *    the XML file will be prefiltered with the specified XSL filter before 
   *    XML parsing begins. This allows node attributes to be inserted that 
   *    modify the proximity of various text sections as well as boost or 
   *    deemphasize the relevance sections of text. For a description of 
   *    attributes handled by this XML parser, see the {@link XMLTextProcessor} 
   *    class description. <br><br> 
   */
  private int parseText( SrcTextInfo file )
  
  {
    
    // For some evil reason, Resin overrides the default transformer
    // and parser implementations with its own, deeply inferior,
    // versions. Screw that.
    //
    System.setProperty( "javax.xml.parsers.TransformerFactory",
                        "net.sf.saxon.TransformerFactoryImpl" );
    
    //System.setProperty( "javax.xml.parsers.SAXParserFactory",
    //                    "org.apache.crimson.jaxp.SAXParserFactoryImpl" );
    
    // Create a SAX parser factory.
    SAXParserFactory spf = SAXParserFactory.newInstance();
    
    try 
    {
        // Instantiate a new XML parser instance.
        SAXParser xmlParser = spf.newSAXParser();
        XMLReader xmlReader = xmlParser.getXMLReader();
        xmlReader.setFeature( 
                  "http://xml.org/sax/features/namespaces", true );
        xmlReader.setFeature( 
                  "http://xml.org/sax/features/namespace-prefixes", true );
        
        // Convert our XML text file into a SAXON input source.
        InputStream inStream;
        if( file.source.getByteStream() == null )
            inStream = new FileInputStream( file.source.getSystemId() );
        else
            inStream = file.source.getByteStream();
        
        // Remove DOCTYPE declarations, since the XML reader will barf if it
        // can't resolve the entity reference, and we really don't care.
        //
        inStream = new DocTypeDeclRemover( inStream );
    
        // If there no XSLT input filter defined for this index, just 
        // parse the source XML file directly, and return early.
        //
        if( srcText.inputFilter == null ) {
            xmlParser.parse( inStream, this );
            return 0;
        }
        
        // If we got to this point, there is an XSLT filter specified. So 
        // set up to use it, and process the resulting filtered XML text.
        //
        Templates stylesheet = srcText.inputFilter;
        
        // Create an actual transform filter from the stylesheet for this
        // particular document we're indexing.
        //
        Transformer filter = stylesheet.newTransformer();
      
        // Now make an input source.
        InputSource inSrc = new InputSource( inStream );
        
        // Put a proper system ID on it.
        if( file.source.getSystemId() != null )
            inSrc.setSystemId( file.source.getSystemId() );
        
        // And finally, make a SAX source that combines the XML reader with
        // the filtered input source.
        //
        SAXSource srcText = new SAXSource( xmlReader, inSrc );
      
        // Identify this class as the parser for the XML events that 
        // the XSLT translation will produce.
        //
        SAXResult filteredText = new SAXResult( this );
      
        // Make sure errors get directed to the right place.
        if( !(filter.getErrorListener() instanceof XTFSaxonErrorListener) )
            filter.setErrorListener( new XTFSaxonErrorListener() );
    
        // Perform the translation.
        filter.transform( srcText, filteredText );

    } // try
    
    catch( Exception e ) {
      
        // Tell the caller (and the user) that ther was an error..      
        Trace.more( Trace.info, "Skipping Due to Errors." );

        Trace.debug( "*** XML Parser Exception: " + 
            e.getClass() + "\n"  +
            "    With message: " + 
            e.getMessage() );
        
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
   * @param stylesheet      The stylesheet containing xsl:key declarations.
   * 
   * @throws Exception      If anything goes awry.
   */
  private void precacheXSLKeys( Templates stylesheet )
      throws Exception
  
  {
      
    // Register a lazy key manager
    PreparedStylesheet pss = (PreparedStylesheet) stylesheet;
    Executable exec = pss.getExecutable();
    if( !(exec.getKeyManager() instanceof LazyKeyManager) ) {
        exec.setKeyManager( new LazyKeyManager(exec.getKeyManager(),
                                               pss.getConfiguration()) );
    }
    
    Transformer trans = pss.newTransformer();
    LazyKeyManager keyMgr = (LazyKeyManager) exec.getKeyManager();
    LazyDocument doc = (LazyDocument) lazyBuilder.load( lazyFile );
    
    // For every xsl:key registered in the stylesheet, build the lazy key
    // hash.
    //
    int nKeysCreated = keyMgr.createAllKeys( doc, 
                               ((Controller)trans).newXPathContext() );
    Trace.more( Trace.info, "(" + nKeysCreated + " keys) " );
    
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
  public void startDocument() throws SAXException
  
  {
    
    if( lazyHandler != null )
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
  public void startElement( 

      String     uri,
      String     localName,
      String     qName,
      Attributes atts

  ) throws SAXException

  // called at element start

  {
    
    // Process any characters accumulated for the previous node, writing them
    // out as chunks to the index if needed.
    // 
    flushCharacters();
    
    // And add the accumulated text to the "lazy tree" representation as well.
    if( lazyHandler != null )
        lazyHandler.startElement( uri, localName, qName, atts );
      
    // If this is the start of a meta data node (marked with an xtf:meta
    // attribute), read in the meta data. Note that these meta-data nodes are 
    // not indexed as part of the general text.
    //
    int metaIndex = atts.getIndex( xtfUri, "meta" );
    String metaVal = (metaIndex >= 0) ? atts.getValue(metaIndex) : "";
    if( metaIndex >= 0 && ("yes".equals(metaVal) || "true".equals(metaVal)) )
    {
        if( inMeta > 0 )
            throw new RuntimeException( "Meta-data fields may not nest" );

        inMeta = 1;
        if( metaInfo == null )
            metaInfo = new LinkedList();

        // See if there is a "tokenize" attribute set for this node. If not,
        // default to true.
        //
        boolean tokenize = true;
        int tokIdx = atts.getIndex( xtfUri, "tokenize" );
        if( tokIdx >= 0 ) {
            String tokStr = atts.getValue( tokIdx );
            if( tokStr != null && (tokStr.equals("no") || tokStr.equals("false")) )
                tokenize = false;
        }
        
        // Process the current meta field, tokenizing it as needed.
        metaField = new MetaField( localName, tokenize );

        assert metaBuf.length() == 0 : "Should have cleared meta-buf";
    }
    
    // If there are nested tags below a meta-field (and if they're not
    // meta-fields themselves), keep track of how far down they go, so we can
    // know when we hit the end of the top-level tag.
    //
    else if( inMeta > 0 )
        inMeta++;
    
    // All other nodes need to be tracked, so increment the node number and 
    // reset the word count for the node.
    //
    else {

        // Get the current section type and word boost.
        String prevSectionType = section.sectionType();
        float  prevWordBoost   = section.wordBoost();
        
        // Process the node specific attributes such as section type,
        // section bump, word bump, word boost, and so on.
        //
        ProcessNodeAttributes( atts );
        
        // If the section type changed, we need to start a new chunk.
        if( section.sectionType() != prevSectionType ||
            section.wordBoost()   != prevWordBoost ) {          
            
            // Clear out any remaining accumulated text.
            forceNewChunk( prevSectionType, prevWordBoost );
            
            // Diagnostic info.
            Trace.tab();
            Trace.debug( "Begin Section [" + section.sectionType() + "]" );
            Trace.untab();
        }
    }
    
    // Increment the tag ID (count) for the new node we encountered, and
    // reset the accumulated word count for this node.
    //
    incrementNode();
    
  } // public startElement()

  
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
      if( accumText.length() > 0 )
          accumText.append( XtfSpecialTokensFilter.nodeMarker );
      
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
  public void endElement( 
      
      String uri,
      String localName,
      String qName
      
  ) throws SAXException

  // called at element end

  {

    // Process any characters accumulated for the previous node, writing them
    // out as chunks to the index if needed.
    // 
    flushCharacters();
    
    // And add the accumulated text to the "lazy tree" representation as well.
    if( lazyHandler != null ) 
        lazyHandler.endElement( uri, localName, qName );

    // If this is the end of a meta-data field, record it.
    if( inMeta == 1 ) {
        metaField.value = metaBuf.toString().trim();
        metaInfo.add( metaField );
        metaField = null;
        metaBuf.setLength( 0 );
        inMeta = 0;
    }
    else if( inMeta > 1 )
        inMeta--;
    
    // Save the section type and word boost value before popping the current 
    // section info off the stack.
    //
    String prevSectionType = section.sectionType();
    float  prevWordBoost   = section.wordBoost();
      
    // Decrease the section stack depth as needed, possibly pulling
    // the entire section entry off the stack.
    //
    section.pop();
    
    // If the section type changed, force new text to start in a new chunk. 
    if( section.sectionType() != prevSectionType || 
        section.wordBoost()   != prevWordBoost ) {
      
        // Output any remaining accumulated text.
        forceNewChunk( prevSectionType, prevWordBoost );

        // Diagnostic info.
        Trace.tab();
        Trace.debug( "End Section [" + prevSectionType + "]" );
        Trace.untab();
    }       

    // Cross-check to make sure our node counting matches the lazy tree.
    if( lazyBuilder != null )
        assert lazyBuilder.getNodeNum(lazyReceiver) == curNode + 1;
    
  } // public endElement()

  
  ////////////////////////////////////////////////////////////////////////////

  public void processingInstruction( 
  
      String target, 
      String data 
      
  ) throws SAXException
  
  {

      // Increment the tag ID (count) for the new node we encountered, and
      // reset the accumulated word count for this node.
      //
      incrementNode();
      
      // Build the lazy tree along the way.
      if( lazyHandler != null )
          lazyHandler.processingInstruction( target, data );

  } // processingInstruction()
  
  
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
  public void endDocument() throws SAXException

  {
    
    // Index the remaining accumulated chunk (if any).
    indexText( section.sectionType(), section.wordBoost() );
    
    // Save the document "header" info.
    saveDocInfo();
  
    // Finish building the lazy tree
    if( lazyHandler != null )
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
  public void characters(

      char[] ch,
      int    start,
      int    length
      
  ) throws SAXException    
  
  {

    // If the accumulation buffer is not big enough to receive the current 
    // block of new characters...
    //
    if( charBufPos + length > charBuf.length ) {
        
        // Hang on to the old buffer.
        char[] old = charBuf;
        
        // Create a new buffer that does have space plus a bit (to try to 
        // avoid unnecessary reallocations for any small additional chunks
        // that might follow.)
        //
        charBuf = new char[ charBufPos + length + 1024 ];
        
        // And copy the previously accumulated text into the new buffer.
        System.arraycopy( old, 0, charBuf, 0, charBufPos );
    }
    
    // Add the new block of text to the accumulation buffer, and update the
    // count of accumulated characters.
    //
    System.arraycopy( ch, start, charBuf, charBufPos, length );
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
  public void flushCharacters() throws SAXException

  {
    
    // Get local references to the accumulated text buffer that we can 
    // adjust as we go.
    //
    char[] ch  = charBuf;
    int length = charBufPos;
    int start  = 0;

    // Reset the accumulated character count to zero in anticipation of 
    // accumulating characters for a new node later on. (Do this now because
    // of multiple exit points in this function.)
    //
    charBufPos = 0;

    // If the entire buffer is whitespace (or empty), we can safely strip it.
    int i;
    for( i = 0; i < length; i++ )
        if( !Character.isWhitespace(charBuf[i]) ) break;
    if( i == length ) return;

    // Build this part of the lazy tree, and increment the node number in
    // concert with it.
    //
    if( lazyHandler != null )
        lazyHandler.characters( ch, start, length );
    incrementNode();

    // If we're processing a meta-info section, simply add the characters to
    // the meta-info buffer.
    //
    if( inMeta > 0 ) {
        metaBuf.append( ch, start, length );
        return;
    }
      
    // If we aren't supposed to index this section, return immediately.
    if( section.indexFlag() == SectionInfo.noIndex ) return;
        
    // Create a blurbed text buffer.
    blurbedText.setLength( 0 );
   
    // Place the text section passed to this function in the buffer.
    blurbedText.append( ch, start, length );    
        
    // Blurbify the text (i.e., convert line feeds, tabs, multiple spaces, 
    // and other weird white-space into something that's nicer to read in
    // a blurb.
    //
    blurbify( blurbedText, true );
    
    // Insert any virutal words implied by section bumps, ends of sentences,
    // global word bump changes and so on.
    //
    insertVirtualWords( blurbedText );    
    
    // If after blurbification, there's no text remaining, we're done.
    if( blurbedText.length() <= 0 ) return;    

    // Create a string reader for use by the tokenizer.
    String       blurbedTextStr = blurbedText.toString();    
    //StringReader reader         = new StringReader( blurbedTextStr );
    FastStringReader reader     = new FastStringReader( blurbedTextStr );
    
    // Create a tokenizer to locate the start and end of words in the
    // blurbified text.
    //
    //TokenStream result = new StandardTokenizer( reader );
    TokenStream result = new FastTokenizer( reader );
    
    // Set the start of punctuation index to the beginning of the blurbified 
    // text buffer.
    //
    int punctStart = 0;
    
    // Trim all the trailing spaces off the accumulated text buffer
    // except for one, and get back the resulting length of the 
    // trimmed, accumulated text.
    //
    int accumTextLen = trimAccumText( true );
          
    // Start out having to fetch the first word in the token list.
    boolean mustGetNextWord = true;
    Token word = null;
    
    // Process the entire token list, accumulating and indexing chunks of
    // text as we go.
    //
    for(;;) {
        
        try {
          
            // If we haven't already fetched the next word, do so now.
            if( mustGetNextWord ) word = result.next();
            
            // If there is no next word, we're done.
            if( word == null ) break;
            
            // Flag that the next word hasn't been fetched.
            mustGetNextWord = true;
            
            // If this is the first word in the chunk, record its node and
            // offset.
            //
            if( chunkStartNode < 0 ) {
                chunkStartNode = curNode;
                chunkWordOffset = nodeWordCount;
            }
            
            // Determine where the current word starts and ends.
            int wordStart = word.startOffset();
            int wordEnd   = word.endOffset();
            
            // Determine how much punctuation there is before the current
            // current word we're processing.
            //
            int punctLen = wordStart - punctStart;
            
            // If the word we just processed is the first word of the next
            // overlapping chunk...
            //
            if( chunkWordCount == chunkWordOvlpStart ) {
              
                // Record how much of the current chunk's text we need to
                // keep in the buffer for the next node.
                //
                nextChunkStartIdx = accumTextLen + punctLen;
                
                // Record what node the next chunk starts in, and what offset
                // the chunk has relative to the start of the node.
                //
                nextChunkStartNode  = curNode;
                nextChunkWordOffset = nodeWordCount;
  
                // Finally, start with no words in the next node. 
                nextChunkWordCount = 0;
                
            } // if( chunkWordCount == chunkWordOvlpStart )
            
            // Append the new word and its preceeding punctuation/spacing to 
            // the text to index/store.
            //
            accumText.append( 
                blurbedTextStr.substring( punctStart, wordEnd ) );
      
            // Track where the punctuation starts for the next word.  
            punctStart = wordEnd;
            
            // Account for this new word in the node, the current chunk, and 
            // (possibly) the next chunk.
            //
            chunkWordCount++;
            nextChunkWordCount++;
            if( !word.termText().equals(XtfSpecialTokensFilter.virtualWord) )
                nodeWordCount++;

            // If we've got all the words required for the current chunk...
            if( chunkWordCount == chunkWordSize ) {

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
                if( word == null ) punctEnd = blurbedText.length();
                else               punctEnd = word.startOffset();
                
                // Tack the punctuation onto the end of the chunk and clean
                // it up so as to make it look all purdy.
                //
                accumText.append( 
                    blurbedTextStr.substring( punctStart, punctEnd ) );

                // Trim all the trailing spaces off the accumulated text.
                trimAccumText( false );
                
                // Index the accumulated text.
                indexText( section.sectionType(), section.wordBoost() );
                                
                // Advance the punctuation start point for the next word to
                // the beginning of the word itself, since we already 
                // accumulated the punctuation after the current word.
                //
                punctStart = punctEnd;               
                
                // Make the next chunk current.
                chunkStartNode  = nextChunkStartNode;
                chunkWordOffset = nextChunkWordOffset;
                chunkWordCount  = nextChunkWordCount;
  
                // Remove the text from the buffer that was in the previous
                // chunk but not in the next one.
                //
                accumText.delete( 0, nextChunkStartIdx );
                                
                // Make sure that the next word added doesn't bump up against
                // the last one accumulated.
                //
                accumTextLen = trimAccumText( true );

                // Reset the start index for the next chunk.
                nextChunkStartIdx = 0;
                nextChunkWordCount = 0;
                                               
            } // if( chunkWordCount == chunkWordSize )
            
            // Trim all the trailing spaces off the accumulated text 
            // buffer and get back the resulting accumulated text
            // length.
            //
            else
                accumTextLen = trimAccumText( false );           
            
        } // try( to process next word in token list )
        
        catch( Exception e ) {}
      
    } // for(;;)
    
    // Accumulate and closing text/punctuation in this text block.
    accumText.append( 
      blurbedTextStr.substring( punctStart, blurbedTextStr.length() ) );
    
    // Trim all the trailing spaces off the accumulated text buffer.
    trimAccumText( false );
    
  } // public characters()


  //////////////////////////////////////////////////////////////////////////////
  
  /** Forces subsequent text to start at the beginning of a new chunk. <br><br>
   * 
   *  This method is used to ensure that source text marked with proximity 
   *  breaks or new section types does not overlap with any previously 
   *  accumulated source text.
   * 
   *  @param  sectionType  The section type to apply to the previously
   *                       accumulated chunk.
   * 
   *  @param  wordBoost    The word boost to apply to the previously 
   *                       accumulated chunk. <br><br>
   * 
   *  @.notes
   *    This method writes out any accumulated text and resets the chunk
   *    tracking information to the start of a new chunk.
   */
  private void forceNewChunk( String sectionType, float wordBoost )
  
  {
    
    // Index whatever is left in the accumulated text buffer.
    indexText( sectionType, wordBoost );
  
    // Since we're forcing a new chunk, advance the next chunk past
    // the one we just wrote.
    //
    chunkWordOffset += chunkWordCount;
      
    // Zero the accumulated word count for the chunk.
    chunkWordCount      =  0;
    nextChunkWordCount  =  0;
    nextChunkWordOffset =  0;
    accumText.setLength( 0 );

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
  private int trimAccumText( boolean oneEndSpace )
  
  {

    // Figure out how long the current accumulated text is.
    int length = accumText.length();
    
    // Trim all the trailing spaces off the accumulated text buffer.
    while( length > 0 && accumText.charAt(length-1) == ' ' )
        accumText.deleteCharAt(--length);
    
    // If there's any accumulated text left, and the caller wants the 
    // accumulated text to end with a space (to guarantee that the next
    // word added will not run into the previously accumulated one), 
    // add back one space.
    //    
    if( length > 0 && oneEndSpace ) {
        accumText.append( ' ' ); 
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
  private void blurbify( 
  
        StringBuffer text, 
        boolean      trim
  )
  
  {
    int i;
    
    // Determine the length of the passed text.
    int length = text.length();

    // Run through and replace any line-feeds, tabs or other funny
    // white-space characters in the text with a space character, as 
    // it will be more readable in the blurb.
    //
    for( i = 0; i < length; i++ ) {
        
        // Get the current character.
        char theChar = text.charAt(i);
        
        // If it's the special token marker character, a tab, linefeed or 
        // some other spacing (but not actually a space character), replace
        // it so as to not cause problems in the blurb.
        //
        if( theChar == XtfSpecialTokensFilter.bumpMarker ||
            theChar == XtfSpecialTokensFilter.nodeMarker ||
            ( theChar != ' ' && Character.isWhitespace(theChar) ) ) 
            text.setCharAt( i, ' ' );

    } // for( i = 0; i < length; i++ )
  
    // Compact multiple spaces down into a single space.
    for( i = 0; i < length-1; ) {
        
        // If there are two spaces in a row at the current position...
        if( text.charAt(i) == ' ' && text.charAt(i+1) == ' ' ) {
            
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
    if( trim ) {
        
        // Trim the leading spaces (if any), and adjust the next chunk start
        // index to match.
        //
        while( length > 0 && text.charAt(0) == ' ' ) { 
            text.deleteCharAt(0); 
            length--; 
        } 
        
        // Trim the trailing spaces as well.
        while( length > 0 && text.charAt(length-1) == ' ' ) 
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
   *    {@link XtfSpecialTokensFilter#virtualWord virtualWord}
   *    member of the {@link XtfSpecialTokensFilter} class, and has been chosen
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
  private void insertVirtualWords( StringBuffer text )
    
  {
    int i; 
    
    // Get a spaced representation of the virtual word that we can use 
    // over and over again.
    //
    String vWord = XtfSpecialTokensFilter.virtualWord + " ";
              
    // Determine the length of the text to adjust.
    int len = text.length();
  
    // Move through all the text, looking for the end of sentences.
    for( i = 0; i < len; i++ ) {
      
        // If we find the end of a sentence, insert the number of virtual
        // words to match the sentence bump value.
        //
        if( isEndOfSentence( i, len, text ) ) {
          
            // If this is at the end of a quote, move beyond the closing
            // quote before inserting the virtual words.
            //          
            if( i < len-1 && text.charAt(i+1) == '"' ) i++;
            
            // Put in the virtual words.
            insertVirtualWords( vWord, section.sentenceBump(), text, i+1 );
            
            // Now that we've inserted virtual words, the length is no longer
            // valid. So update it before we continue.
            //
            len = text.length();
        }
    }

    // If there is currently no section bump pending...
    if( section.sectionBump() == 0 ) {
      
        // And a new chunk has been forced, insert a proximity bump at the
        // beginning of the text.
        //
        if( forcedChunk ) {
         
            // Must insert chunkWordSize virtual words (not chunkWordOvlp
            // which could make it look like the next chunk overlaps).
            //
            insertVirtualWords( vWord, chunkWordSize, text, 0 );
          
            // Cancel the forced chunk flag, now that we've handled it.
            forcedChunk = false;
      
        } // if( forcedChunk )      
  
    } // if( section.sectionBump() == 0 )
  
    // Otherwise, there is a section bump pending, so add it to the beginning
    // of the accumulated text.
    //
    else
        insertVirtualWords( vWord, section.useSectionBump(), text, 0 );            
       
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
  private boolean isEndOfSentence( 
      int          idx, 
      int          len, 
      StringBuffer text 
  )
  
  {
    
    // Get the current character from the text. If it is not even a sentence
    // punctuation mark, return early.
    //
    char currChar = text.charAt(idx);
    if( !isSentencePunctuationChar(currChar) ) return false;
    
    // The current character is sentence punctuation, so lets get what's before
    // and after it.
    //
    char prevChar = ' ';
    char nextChar = ' ';
    if( idx > 0     ) prevChar = text.charAt(idx-1);
    if( idx < len-1 ) nextChar = text.charAt(idx+1);
    
    // If the current character is a period...
    //
    if( currChar == '.' ) {
        
        // It might be part of a number like 61.7, or part of an acronym like
        // I.B.M. So if the next char is alphanumeric, assume this period
        // doesn't end the sentence.
        //
        if( Character.isLetterOrDigit(nextChar) )
            return false;
        
        // If it's not part of '...', we found the end of a sentence.
        if( prevChar != '.' && nextChar != '.' ) return true;
        
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
    if( currChar == '!' ) {
        if( !isSentencePunctuationChar(nextChar) ) return true;
        return false;
    }
    
    // If we found a question mark, and it's the only one, or the last
    // one in a chain of '???' or '!?!?' we found an end of sentence.
    //
    if( currChar == '?' ) {
        if( !isSentencePunctuationChar(nextChar) ) return true;
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
  private boolean isSentencePunctuationChar( char theChar )
  
  {
    
    // Currently, only '.', '?', and '!' are considered end of sentence
    // punctuation characters.
    //
    if( theChar == '.' || theChar == '?' || theChar == '!' ) return true;
    
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
  private void insertVirtualWords( 
      
      String       vWord, 
      int          count,
      StringBuffer text, 
      int          pos 
  )
  
  { 

    // If the caller asked for no virtual words to be inserted, return early.
    if( count == 0 ) return;

    // Always start a block of virtual words with a space. Why? Because 
    // Lucene's standard tokenizer seems to treat a sequence like "it.qw"
    // as a single token. In fact, we want it to be treated like the word
    // 'it' followed by the virtual word token 'qw'. Adding the space 
    // assures that this is the case. (Don't worry, we compact the extra
    // space out later anyway when we convert to bump count notation.)
    //
    text.insert( pos++, ' ' );       
    
    // Insert the required number of virtual words.
    for( int j = 0; j < count; j++ ) text.insert( pos, vWord );
    
  } // insertVirtualWords()
  
      
  ////////////////////////////////////////////////////////////////////////////

  /** Add the current accumulated chunk of text to the Lucene database for 
   *  the active index. <br><br>
   * 
   *  @param  sectionType  The section type name for this chunk of text.
   *  @param  wordBoost    The word boost value for this chunk of text. <br><br>
   * 
   *  @.notes
   *    This method peforms the final step of adding a chunk of assembled text
   *    to the Lucene database specified by the 
   *    {@link XMLTextProcessor#configInfo configInfo} configuration member. 
   *    This includes compacting virtual words via the 
   *    {@link XMLTextProcessor#compactVirtualWords() compactVirtualWords()}
   *    method, and recording the unique document identifier (key) for the 
   *    chunk, the section type for the chunk, the word boost for the chunk,
   *    the XML node in which the chunk begins, the word offset of the chunk, 
   *    and the "blurbified" text for the chunk. <br><br>   
   */
  private void indexText( String sectionType, float wordBoost )
  
  {
   
    try {

        // Convert virtual words into special bump tokens and adjusted chunk 
        // offsets to save space in the blurb text.
        //
        compactVirtualWords();    
    }
    
    // If anything went wrong...
    catch( Exception e ) {
        
        // Log the error.
        Trace.tab();
        Trace.error( "*** Exception Compacting Virtual Words: " + e );
        Trace.untab();
        
        // And exit early.
        return;
    }
    
    // If after compaction there's nothing to index, we're done.
    if( compactedAccumText.length() == 0 ) return;
    
    // Make a new document, to which we can add our fields.     
    Document doc = new Document();
    
    // Add the key value so we can find this index entry again. Since we'll 
    // use this only as a finding aid, store the path as a non-stored, indexed,
    // non-tokenized field.
    //
    doc.add( new Field( "key", srcText.key, false, true, false ) );

    // Write the current section type as a stored, indexed, tokenized field.
    if( sectionType != null && sectionType.length() > 0 )
        doc.add( new Field( "sectionType", 
                             sectionType, 
                             true, true, true ) );

    // Convert the various integer field values to strings for writing.
    String nodeStr       = Integer.toString( chunkStartNode );          
    String wordOffsetStr = Integer.toString( chunkWordOffset );
    String textStr       = compactedAccumText.toString();
    
    // Diagnostic output.
    Trace.tab();
    Trace.debug( "node " + nodeStr + ", offset = " + wordOffsetStr );
    Trace.more( " text = [" + textStr + "]" );
    Trace.untab(); 

    // Add the node number for this chunk. Store, but don't index or tokenize.
    doc.add( new Field( "node", nodeStr, true, false, false ) );
    
    // Add the word offset for this chunk. Store, but don't index or tokenize.
    doc.add( new Field( "wordOffset", wordOffsetStr, true, false, false ) );
    
    // Create the text field for this document.                        
    Field textField = new Field( "text", textStr, true, true, true );
    
    // Set the boost value for the text.
    textField.setBoost( wordBoost );
    
    // Finally, add the text in the chunk to the index as a stored, indexed,
    // tokenized field.
    //
    doc.add( textField );
        
    try {

        // Add the resulting list of fields (document) to the index.
        indexWriter.addDocument( doc );
      
        // Account for the new chunk added.
        chunkCount++;    
    }
    
    // If anything went wrong adding the document to the index...
    catch( Exception e ) {
        
        // Log the error.
        Trace.tab();
        Trace.error( "*** Exception Adding Text to Index: " + e );
        Trace.untab();
          
        // And bail.
        return;
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
  private static boolean isAllWhitespace( String str, int start, int end ) 
  
  {
      
      for( int i = start; i < end; i++ )
          if( !Character.isWhitespace(str.charAt(i)) )
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
   *    {@link XtfSpecialTokensFilter#bumpMarker bumpMarker} member of the
   *    {@link XtfSpecialTokensFilter} class. <br><br>  
   * 
   */
  private void compactVirtualWords() throws IOException
    
  { 
    int i;
    
    // Get convienient versions of the special bump token marker and the 
    // virtual word string.
    // 
    char   marker = XtfSpecialTokensFilter.bumpMarker;
    String vWord  = XtfSpecialTokensFilter.virtualWord;

    // Start by setting the compacted text buffer to the contents of the
    // accumulated text buffer.
    //
    compactedAccumText.setLength(0);
    compactedAccumText.append( accumText );

    // Convert the compacted text into a list of tokens we can use.
    String           textStr   = compactedAccumText.toString();    
    FastStringReader reader    = new FastStringReader( textStr );
    TokenStream      tokenList = new FastTokenizer( reader );
    
    int posAdj = 0;
    Token theToken = null;
    
    // Look for blocks of virtual words, and turn them into special bump
    // tag notation for compactness.
    //
    boolean mustGetNextToken = true;
    for(;;) {
        
        // Get the next token from the list (unless we already got it.)
        if( mustGetNextToken )
            theToken = tokenList.next();
        else
            mustGetNextToken = true;
        if( theToken == null ) break;
        
        // Start with no virtual words encountered.
        int vWordCount = 0;
        
        // Mark the start and end of the current block of virtual words.
        int vRunStart = posAdj + theToken.startOffset();
        int vRunEnd   = vRunStart;       
        
        // For each virtual word we encounter in a row (possibly none)...
        while( vWord.equalsIgnoreCase(theToken.termText()) ) {
             
             // If the previous token was also a virtual word...
             if( vWordCount > 0 ) {
                 
                 // Make sure that only spaces separate them. If there's
                 // punctuation in there, it's not safe to compact this one
                 // with the previous.
                 //
                 if( !isAllWhitespace(textStr, 
                                      vRunEnd - posAdj, 
                                      theToken.startOffset()) ) 
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
             if( theToken == null ) break;
             
        } // while( vWord.equalsIgnoreCase(theToken.termText()) )
        
        // If we found any virtual words...
        if( vWordCount > 0 ) {
            
            // Determine how long the run of virtual words was and remove them.
            int vRunLen = vRunEnd - vRunStart;
            compactedAccumText.delete( vRunStart, vRunEnd );
            
            // Update the position adjustment to account for the run 
            // of virtual words we removed.
            //
            posAdj -= vRunLen;
            
            // Insert a special bump token equivalent to the number of 
            // virtual words removed by this run.
            //
            String bumpStr = marker + Integer.toString( vWordCount ) + marker;
            compactedAccumText.insert( vRunStart, bumpStr );

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
    boolean inSpecialToken    = false;
    int     startSpecialToken = 0; 
    for( i = 0; i < compactedAccumText.length()-1; ) {
        
        // Get the current and next characters.
        char currChar = compactedAccumText.charAt( i );
        char nextChar = compactedAccumText.charAt(i+1);
        
        // If the current character is a space...
        if( currChar == ' ' ) {
          
            // If the next character is also a space or a special bump tag
            // marker, we can remove the space. Note that we don't advance
            // since the deletion of the space effectively slid the remaining
            // characters one slot to the left.
            //
            if( nextChar == ' ' || nextChar == marker ) {
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
  private boolean trueOrFalse( String value, boolean defaultResult )
  
  {

    // If the string was 'true' or 'yes' return true.
    if( value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") )
        return true;

    // If it was 'false' or 'no' return false.
    if( value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no") )
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
  private void ProcessNodeAttributes( Attributes atts )
  
  { 
    String valueStr;
    
    // Default all the section-based attributes to the current values
    // belonging to the parent section at the top of the stack.
    // 
    String sectionTypeStr = section.sectionType();
    float  wordBoost      = section.wordBoost();
    int    sentenceBump   = section.sentenceBump();
    int    indexFlag      = SectionInfo.parentIndex;
    int    sectionBump    = 0;    
    
    // Process each of the specified node attributes, outputting warnings
    // for the ones we don't recognize.
    //
    for( int i = 0; i < atts.getLength(); i++ ) {
        
        // Skip non-XTF related attributes.
        if( !xtfUri.equals(atts.getURI(i)) )
            continue;
        
        String attName = atts.getLocalName(i);
        
        // If the current attribute is the section type, get it.      
        if( attName.equalsIgnoreCase("sectionType") )
            sectionTypeStr = atts.getValue(i);
        
        // If the current attribute is the section bump, get it.    
        else if( attName.equalsIgnoreCase("sectionBump") ) {
            valueStr    = atts.getValue(i);
            sectionBump = Integer.valueOf(valueStr).intValue();
        }
        
        // If the current attribute is the word boost, get it. 
        else if( attName.equalsIgnoreCase("wordBoost") ) {
            valueStr  = atts.getValue(i);
            wordBoost = Float.valueOf(valueStr).intValue();
        }
        
        // If the current attribute is the sentence bump, get it.
        else if( attName.equalsIgnoreCase("sentenceBump") ) {
            valueStr = atts.getValue(i);
            sentenceBump = Integer.valueOf(valueStr).intValue();
        }
        
        // If the current attribute is the no-index flag...
        else if( attName.equalsIgnoreCase("noIndex") ) {
  
            // Determine the default "no index" flag value from the parent
            // section. Note that the default value is the inverse of the 
            // parent section value, as the final flag is an "index" flag
            // rather than a "no-index" flag.
            //   
            boolean defaultNoIndexFlag = (indexFlag == SectionInfo.index) ?
                                          false : true;
            
            // Get the value of the "no index" attribute.
            valueStr = atts.getValue(i);
            
            // Build the final index flag based on the attribute and default
            // values passed.
            //
            indexFlag = trueOrFalse( valueStr, defaultNoIndexFlag )
                        ? SectionInfo.noIndex : SectionInfo.index;
                              
        } // else if( atts.getQName(i).equalsIgnoreCase("xtfNoIndex") )
        
        // If the current attribute is the index flag...
        else if( attName.equalsIgnoreCase("index") ) {
      
            // Determine the default "index" flag value from the parent
            // section.
            //
            boolean defaultIndexFlag = (indexFlag == SectionInfo.index ) ?
                                        true : false;
              
            // Get the value of the "no index" attribute.
            valueStr = atts.getValue(i);
              
            // Build the final index flag based on the attribute and default
            // values passed.
            //
            indexFlag = trueOrFalse( valueStr, defaultIndexFlag )
                        ? SectionInfo.index : SectionInfo.noIndex;
                              
        } // else if( atts.getQName(i).equalsIgnoreCase("xtfIndex") )
        
        // If the current attribute is the proximity break attribute...
        else if( attName.equalsIgnoreCase("proximityBreak") ) {
      
            // Get the proximity break value.
            valueStr = atts.getValue(i);
  
            // If a break is specifically requested, force a new chunk to
            // be started. This ensures that proximity matches wont occur
            // accross the break.
            //
            if( trueOrFalse( valueStr, false ) ) {
       
                // Clear out any partially accumulated chunks.         
                forceNewChunk( section.sectionType(), section.wordBoost() );
        
                // Reset the chunk position to the node we're in now.
                chunkStartNode  = -1;
                chunkWordOffset = -1;
      
                Trace.tab();
                Trace.debug( "Proximity Break" );
                Trace.untab();
            }
            
        } // else if( atts.getQName(i).equalsIgnoreCase("xtfProximityBreak") )
        
        // If the current attribute is the word boost attribute...
        else if( attName.equalsIgnoreCase("wordBoost") ) {
        
            // Get the word boost value.
            valueStr = atts.getValue(i);
            float newBoost = Float.valueOf(valueStr).intValue();
            
            // If the word boost changed...
            if( wordBoost != newBoost ) {
         
                // Clear out any partially accumulated chunks.         
                forceNewChunk( section.sectionType(), section.wordBoost() );
          
                // Reset the chunk position to the node we're in now.
                chunkStartNode  = -1;
                chunkWordOffset = -1;
        
                // Hang on to the new boost value.
                wordBoost = newBoost;
                
                Trace.tab();
                Trace.debug( "Word Boost: " + newBoost );
                Trace.untab();
            }
              
        } // else if( atts.getQName(i).equalsIgnoreCase("xtfProximityBreak") )
          
        // If we got to this point, and we've encountered an unrecognized 
        // xtf:xxxx attribute, display a warning message (if enabled for 
        // output.)
        //
        else {
            Trace.tab();
            Trace.warning( "*** Warning: Unrecognized XTF Attribute [ " +
                           atts.getQName(i) + "=" + atts.getValue(i)     + 
                           " ]." );
            Trace.untab();
        }
      
    } // for( int i = 0; i < atts.getLength(); i++ )
    
    // Finally, push the new section info. Note that if all the values passed
    // match the parent values exactly, this call will simply increase the 
    // depth of the parent to save space.
    //
    section.push( indexFlag, 
                  sectionTypeStr, 
                  sectionBump, 
                  wordBoost, 
                  sentenceBump );

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
  private void saveDocInfo()
  
  {
    
    // Make a new document, to which we can add our fields.     
    Document doc = new Document();
    
    // Add a header flag as a stored, indexed, non-tokenized field 
    // to the document database.
    //
    doc.add( new Field( "docInfo", "1", true, true, false ) );

    // Add the number of chunks in the index for this document.
    doc.add( new Field( "chunkCount", Integer.toString(chunkCount), 
                        true, false, false ) );
    
    // Add the key to the document header as a stored, indexed, 
    // non-tokenized field.
    //
    doc.add( new Field( "key", srcText.key, true, true, false ) );
    
    // Determine when the file was last modified.
    if( srcText.source.getSystemId() != null ) 
    {
        File srcFile = new File( srcText.source.getSystemId() );
        String fileDateStr = DateField.timeToString( srcFile.lastModified() );
        
        // Add the XML file modification date as a stored, non-indexed, 
        // non-tokenized field.
        //
        doc.add( new Field( "fileDate", fileDateStr, true, false, false ) );
    }
    
    // Make sure we got meta-info for this document.
    if( metaInfo == null || metaInfo.isEmpty() ) {
        Trace.tab();
        Trace.warning( "*** Warning: No meta data found for document." );
        Trace.untab();
    }
    
    else {
    
        // Get an iterator so we can add the various meta fields we found for
        // the document (most usually, things like author, title, etc.)
        //
        Iterator metaIter = metaInfo.iterator();
        
        // Add all the meta fields to the docInfo chunk.
        while(  metaIter.hasNext() ) {
            
            // Get the next meta field.
            MetaField field = (MetaField) metaIter.next();
            
            // Add it to the document as stored, indexed, and tokenized.
            doc.add( new Field( field.name,
                                field.value,
                                true, true, field.tokenize ) );
        
        } // while(  metaIter.hasNext() )
   
    } // else( metaInfo != null && !metaInfo.isEmpty() )
    
    try {
      
        // Add the document info block to the index.
        indexWriter.addDocument( doc );
    }

    // If something went wrong...
    catch( Exception e ) {
      
        // Log the problem.
        Trace.tab();
        Trace.error( "*** Exception Adding docInfo to Index: " + e );
        Trace.untab();
    }
    
  } // saveDocInfo()
    
  
  ////////////////////////////////////////////////////////////////////////////
  
  /** Returns a normalized version of the base path of the Lucene database
   *  for an index. <br><br>
   * 
   *  @throws IOException  Any exceptions generated retrieving the path for
   *                       a Lucene database. <br><br> 
   */
  private String getIndexPath() throws IOException
  
  {

    File xtfHomeDir = new File(configInfo.xtfHomePath);
    File idxFile = Path.resolveRelOrAbs(xtfHomeDir, 
                                        configInfo.indexInfo.indexPath);
    String path = Path.normalizePath( idxFile.getCanonicalPath() );
    if( !path.endsWith("/") )
        path += "/";
    return path;

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
   *     {@link XMLTextProcessor#srcText srcText} member. <br><br>
   * 
   *     An XML source document needs reindexing if its modification date 
   *     differs from the modification date stored in the summary info chunk
   *     the last time it was indexed. <br><br>
   */
  private int checkFile() throws IOException
  
  {
      // We need to find the docInfo chunk that contains the specified
      // file. So construct a boolean query looking for a chunk with 
      // a "docInfo" field AND a "key" field containing the specified
      // source file key.
      //
      BooleanQuery query = new BooleanQuery();
      Term docInfo = new Term( "docInfo", "1" );
      Term srcPath = new Term( "key", srcText.key );
      query.add( new TermQuery( docInfo ), true, false );
      query.add( new TermQuery( srcPath ), true, false );
      
      // Use the query to see if the document is in the index..
      boolean docInIndex = false;
      boolean docChanged = false;

      Hits match = indexSearcher.search( query );
      if( match.length() > 0 ) {
        
        // Flag that the document is in the index.
        docInIndex = true;
        
        // Get the file modification date from the "docInfo" chunk.
        Document doc = match.doc( 0 );           
        String indexDateStr = doc.get( "fileDate" );
        
        // See what the date is on the actual source file right now.
        File srcFile = new File( srcText.source.getSystemId() );
        String fileDateStr = DateField.timeToString( srcFile.lastModified() );
        
        // If the dates are different...
        if( fileDateStr.compareTo(indexDateStr) != 0 ) {                    
            
            // Delete the old chunks.
            indexReader.delete( new Term( "key", srcText.key ) );
            
            // Delete the old lazy file, if any. Might as well delete any
            // empty parent directories as well.
            //
            File lazyFile = IdxConfigUtil.calcLazyPath(
                                             new File(configInfo.xtfHomePath),
                                             configInfo.indexInfo, 
                                             srcFile,
                                             false );
            Path.deletePath( lazyFile.toString() );
            
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
        
      } // else( match.next() )
    
    // Now let the caller know the status.
    if( !docInIndex ) return 0;
    if( !docChanged ) return 1;
    return 2;
    
  } // checkFile()
  
  
  ////////////////////////////////////////////////////////////////////////////

  /**
   * Runs an optimization pass (which can be quite time-consuming) on the
   * currently open index. Optimization speeds future query access to the
   * index.
   */
  public void optimizeIndex() throws IOException
  {
      // Close the reader and searcher, since doing so will make optimization 
      // go much more quickly.
      //
      if( indexSearcher != null ) indexSearcher.close();
      if( indexReader   != null ) indexReader.close();
      indexSearcher = null;
      indexReader   = null;

      try {
          // Open the index writer.
          openIdxForWriting();

          // Run the optimizer.
          indexWriter.optimize();
      }
      catch( IOException e ) {
          Trace.tab();
          Trace.error( "*** Exception Optimizing Index: " + e );
          Trace.untab();
      }
      
      finally {
          if( indexWriter != null ) indexWriter.close();
          indexWriter = null;
      }
          
      assert indexWriter == null : "indexWriter not closed in finally block";
      
  } // optimizeIndex()


  ////////////////////////////////////////////////////////////////////////////

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
  private void openIdxForWriting() throws IOException
  
  {
    // Create an index writer, using the selected index db Path
    // and create mode. Pass it our own text analyzer, currently 
    // with an empty stop-word list (not null, as we interpret
    // null to mean the standard stopword list), and the original
    // blurbed text for XTF special token processing. 
    //
    indexWriter = new IndexWriter( 
                     indexPath,
                     new XTFTextAnalyzer( stopSet, 
                                          configInfo.indexInfo.getChunkSize(),
                                          compactedAccumText ), 
                                          false );
      
    // Since we end up adding tons of little 'documents' to Lucene, it's much 
    // faster to queue up a bunch in RAM before sorting and writing them out. 
    // This limit gives good speed, but requires quite a bit of RAM (probably
    // 100 megs is about right.)
    //
    indexWriter.minMergeDocs = 100;
    
    // Don't use compound files, since they can't be added to later.
    indexWriter.setUseCompoundFile( false );
    
  } // private openIdxForWriting()  


  ////////////////////////////////////////////////////////////////////////////

  private class MetaField {
    
    public String  name;
    public String  value;
    public boolean tokenize;
    
    public MetaField( String name, boolean tokenize ) {
      this.name     = name;
      this.tokenize = tokenize;
    }

  } // private class MetaField

} // class XMLTextProcessor
