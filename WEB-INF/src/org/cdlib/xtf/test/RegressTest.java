package org.cdlib.xtf.test;


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
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Vector;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.PreparedStylesheet;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.om.AllElementStripper;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.KeyManager;
import net.sf.saxon.tree.TreeBuilder;

import org.cdlib.xtf.lazyTree.LazyKeyManager;
import org.cdlib.xtf.lazyTree.SearchTree;
import org.cdlib.xtf.servletBase.StylesheetCache;
import org.cdlib.xtf.textEngine.IndexUtil;
import org.cdlib.xtf.textEngine.DefaultQueryProcessor;
import org.cdlib.xtf.textEngine.IndexWarmer;
import org.cdlib.xtf.textEngine.QueryProcessor;
import org.cdlib.xtf.textEngine.QueryRequest;
import org.cdlib.xtf.textEngine.QueryRequestParser;
import org.cdlib.xtf.textEngine.QueryResult;
import org.cdlib.xtf.textEngine.XtfBigramQueryRewriter;
import org.cdlib.xtf.textIndexer.TagFilter;
import org.cdlib.xtf.textIndexer.TextIndexer;
import org.cdlib.xtf.util.CircularQueue;
import org.cdlib.xtf.util.DiskHashWriter;
import org.cdlib.xtf.util.FastIntCache;
import org.cdlib.xtf.util.FastStringCache;
import org.cdlib.xtf.util.IntHash;
import org.cdlib.xtf.util.IntMultiMap;
import org.cdlib.xtf.util.Path;
import org.cdlib.xtf.util.StructuredFile;
import org.cdlib.xtf.util.StructuredStore;
import org.cdlib.xtf.util.TagArray;
import org.cdlib.xtf.util.Trace;
import org.cdlib.xtf.util.XMLWriter;
import org.cdlib.xtf.util.XTFSaxonErrorListener;

/**
 * Runs a series of regression tests. These take a set of test files and index
 * them, then run queries and check the results.<br><br>
 *
 * The test scans subdirectories of the directory it's started in, looking
 * for:
 *
 * <ul>
 *      <li>IndexConfig.xml - tells what to index and how</li>
 *      <li>xxx-in.xml - An input query (same format as output by queryGen.xsl)</li>
 *      <li>gold/xxx.xml - The expected output hits from the query</li>
 *
 * Note that the index for a given directory will be built before all query
 * tests in that directory are run.
 *
 * @author Martin Haye
 */
public class RegressTest 
{
  int nRun = 0;
  int nSucceeded = 0;
  String baseDir;
  File filterDir;
  File filterFile;
  LinkedList failedTests = new LinkedList();
  static Configuration config = new Configuration();
  static { config.setNamePool(NamePool.getDefaultNamePool()); }
  StylesheetCache stylesheetCache = new StylesheetCache(10, 0, false);

  public static void main(String[] args) 
  {
    // Make sure assertions are enabled.
    boolean ok = false;
    assert (ok = true) == true; // Intentional side-effect
    if (!ok) {
      Trace.error(
        "Error: Regression requires assertions to be enabled.\n" +
        "       Pass -enableassertions command-line switch to JVM.");
      System.exit(1);
    }

    // Test the libraries we depend on.
    Path.tester.test();
    StructuredFile.tester.test();
    IntHash.tester.test();
    DiskHashWriter.tester.test();
    CircularQueue.tester.test();
    XtfBigramQueryRewriter.tester.test();
    TagFilter.tester.test();
    FastIntCache.tester.test();
    FastStringCache.tester.test();
    TagArray.tester.test();
    IntMultiMap.tester.test();

    // Go for it.
    RegressTest test = new RegressTest();
    test.run(args);

    // All done.
    System.exit(0);
  } // main()

  public void run(String[] args) 
  {
    try 
    {
      // If a specific test was specified, set that as a filter.
      if (args.length > 0) 
      {
        filterFile = new File(args[0]).getAbsoluteFile();
        if (!filterFile.exists())
          filterFile = new File(args[0] + "-in.xml").getAbsoluteFile();
        if (!filterFile.exists()) {
          Trace.error("Unable to locate " + args[0]);
          System.exit(1);
        }

        if (filterFile.isDirectory()) {
          filterDir = filterFile;
          filterFile = null;
        }
        else
          filterDir = filterFile.getParentFile();
      }

      // Start from the working directory, and scan for regress 
      // sub-directories.
      //
      processDir(new File(System.getProperty("user.dir")), new LinkedList());
    }
    catch (Exception e) {
      Trace.error("Unexpected regress error: " + e);
    }

    if (nRun == 0)
      Trace.info("\nNo tests found to run.");
    else if (nRun == nSucceeded)
      Trace.info("\nAll " + nRun + " tests passed.");
    else
      Trace.info(
        "\n" + nRun + " tests ran, but " + (nRun - nSucceeded) + " failed.");

    if (!failedTests.isEmpty()) {
      Trace.warning("Failed tests:");
      while (!failedTests.isEmpty())
        Trace.warning(failedTests.removeFirst().toString());
    }
  } // run()

  private void processDir(File curFile, LinkedList inFiles)
    throws IOException 
  {
    if (baseDir == null)
      baseDir = curFile.toString();

    // If the file we were passed was in fact a directory...
    if (curFile.isDirectory()) 
    {
      // Get the list of files it contains.
      String[] files = curFile.list();

      // Sort the list.
      ArrayList list = new ArrayList();
      for (int i = 0; i < files.length; i++)
        list.add(files[i]);
      Collections.sort(list);

      // And process each of them.
      LinkedList newInFiles = new LinkedList();
      for (int i = 0; i < list.size(); i++)
        processDir(new File(curFile, (String)list.get(i)), newInFiles);

      // If we have some input files for tests to run, run them now.
      if (newInFiles.isEmpty())
        return;

      runTests(newInFiles);
      return;
    }

    // If it's an index config file, index it.
    String path = curFile.toString();
    if (path.endsWith("IndexConfig.xml")) {
      if (filterDir == null || curFile.getParentFile().equals(filterDir))
        index(curFile);
      return;
    }

    // If it's an input file, queue it for processing after the index is
    // built.
    //
    if (path.endsWith("-in.xml")) {
      if ((filterDir == null || curFile.getParentFile().equals(filterDir)) &&
          (filterFile == null || curFile.equals(filterFile)))
        inFiles.add(curFile);
    }
  } // processDir()

  /**
   * Runs a configuration file to produce an index. Note that we always do
   * a "clean" index.
   *
   * @param configFile    Path to the config file.
   */
  private void index(File configFile)
    throws IOException 
  {
    String dir = configFile.getParentFile().toString();
    System.setProperty("user.dir", new File(dir).getAbsolutePath());

    // Blow away the old index directory.
    File indexDir = new File(dir, "IndexDB");
    Path.deleteDir(indexDir);

    // Set the xtf.home property to the regression directory.
    System.setProperty("xtf.home", dir);

    // Make a command-line to run the indexer.
    String[] args = new String[] {
                      "-trace", "info", "-config", "", "-clean", "-index", "all"
                    };
    args[3] = configFile.toString();
    TextIndexer.main(args);
    Trace.info("");
    Trace.info("");
  } // index()

  private void runTests(LinkedList inFiles)
    throws IOException 
  {
    // Process each test in turn.
    while (!inFiles.isEmpty()) {
      File inFile = (File)inFiles.removeFirst();
      runTest(inFile);
    }
  }

  private String chopPath(String in) {
    if (in.startsWith(baseDir))
      in = in.substring(baseDir.length());
    if (in.endsWith("-in.xml"))
      in = in.substring(0, in.length() - 7);
    if (in.startsWith("\\") || in.startsWith("/"))
      in = in.substring(1);
    return "\"" + in + "\"";
  }

  private void runTest(File inFile)
    throws IOException 
  {
    ++nRun;

    String dir = inFile.getParentFile().toString();
    System.setProperty("user.dir", dir);

    String inFilePath = inFile.toString();
    Trace.info("Running test " + chopPath(inFilePath) + "...");

    String testFilePath = formPath(inFilePath, "actual");
    File testFile = new File(testFilePath);

    // Make sure the test directory exists. If not, create it.
    Path.createPath(testFile.getParent());

    // Okay, read the input file. It contains a query.
    NodeInfo queryDoc = null;
    try {
      Source src = new StreamSource(inFile.toString());
      queryDoc = TreeBuilder.build(src, new AllElementStripper(), config);
    }
    catch (Exception e) {
      Trace.error(
        "Unexpected exception while reading " + chopPath(inFilePath) + ":\n" +
        e);
      return;
    }

    // It may also contain a marker telling us to do a search-annotated
    // tree rather than a CrossQuery-style output.
    //
    try 
    {
      String inSpec = readFile(inFile);
      QueryProcessor processor = new DefaultQueryProcessor();
      processor.setXtfHome(dir);
      IndexWarmer indexWarmer = new IndexWarmer(Path.normalizePath(dir), 5);
      processor.setIndexWarmer(indexWarmer);
      QueryRequest request = new QueryRequestParser().parseRequest(queryDoc,
                                                                   new File(dir));

      if (inSpec.indexOf("regress-search-tree") >= 0) 
      {
        // Get the document path
        int n;

        // Get the document path
        int n2;
        n = inSpec.indexOf("regress-search-tree");
        n = inSpec.indexOf("=\"", n) + 2;
        n2 = inSpec.indexOf("\"", n);
        String docPath = Path.normalizeFileName(
          dir + "/" + inSpec.substring(n, n2));

        // Calculate the persistent path.
        String configPath = Path.normalizeFileName(dir + "/" +
                                                   "IndexConfig.xml");
        File lazyFile = IndexUtil.calcLazyPath(inFile.getParentFile(),
                                               new File(configPath),
                                               "all",
                                               new File(docPath),
                                               false);

        // Locate the display stylesheet, and get its configuration
        PreparedStylesheet displaySheet = null;
        Configuration config;
        if (request.displayStyle == null ||
            request.displayStyle.indexOf("NullStyle") >= 0)
        {
          config = new Configuration();
        }
        else {
          displaySheet = (PreparedStylesheet)
            stylesheetCache.find(request.displayStyle);
          config = displaySheet.getConfiguration();
        }
    
        // Load and search the document. We suppress scores in the
        // snippets because the scoring algorithm changes often and
        // we don't really care exactly what the scores are.
        //
        String docKey = IndexUtil.calcDocKey(inFile.getParentFile(),
                                             new File(configPath),
                                             "all",
                                             new File(docPath));
        StructuredStore treeStore = StructuredFile.open(lazyFile);
        SearchTree tree = new SearchTree(config, docKey, treeStore);
        tree.suppressScores(true);
        tree.search(processor, request);

        // Output the resultant tree.
        if (displaySheet == null)
          writeTree(testFile, tree);
        else
          formatTree(testFile, tree, displaySheet);
      }
      else {
        // Now run the query to obtain hits.
        QueryResult result = processor.processRequest(request);

        // Write the hits to a file.
        if (request.displayStyle == null ||
            request.displayStyle.indexOf("NullStyle") >= 0) 
        {
          writeHits(testFile, result);
        }
        else
          formatHits(testFile, result, request.displayStyle);
      }
    }
    catch (Exception e) {
      PrintWriter out = new PrintWriter(
        new OutputStreamWriter(new FileOutputStream(testFile), "UTF-8"));
      out.println("Exception encountered:\n" + e);
      out.close();
    }

    // See if there's a gold file, and if so, compare it.
    String goldFilePath = formPath(inFilePath, "gold");
    File goldFile = new File(goldFilePath);
    if (!goldFile.exists()) {
      Trace.info("Missing gold file. Test result was:\n" + readFile(testFile));
      Trace.error(
        "   ...test " + chopPath(inFilePath) +
        " failed (missing gold file).\n");
      failedTests.add(chopPath(inFilePath));
    }
    else if (!filesEqual(goldFile, testFile)) {
      Trace.error(
        "   ...Incorrect result. Test " + chopPath(inFilePath) + " failed!\n");
      failedTests.add(chopPath(inFilePath));
    }
    else {
      Trace.info("   ...ok");
      ++nSucceeded;
    }
  } // runTests()

  private String formPath(String path, String sub) {
    String tmp = path.replaceAll("-in", "");
    int slashPos = tmp.lastIndexOf('/');
    if (slashPos < 0)
      slashPos = tmp.lastIndexOf('\\');
    assert slashPos >= 0 : "invalid path";
    tmp = tmp.substring(0, slashPos + 1) + sub + tmp.substring(slashPos);
    return tmp;
  }

  /** Writes the hits in a very simple format to the output file */
  private void writeHits(File outFile, QueryResult result)
    throws IOException 
  {
    // Use the normal CrossQuery method to structure the hits
    Source hitDoc = result.hitsToSource("crossQueryResult", null);

    // Get rid of scores, since they change a lot and we don't really
    // care (unless the order changes.) There are a couple other things
    // we don't care about, so get rid of them too.
    //
    String str = XMLWriter.toString(hitDoc);
    str = str.replaceAll(" score=\"\\d+\"", "");
    str = str.replaceAll(" path=\"all:", " file=\"");
    str = str.replaceAll("(<suggestion.*) freq=\"[0-9]+\"", "$1");
    str = str.replaceAll("(<suggestion.*) score=\"[0-9.]+\"", "$1");

    // Now we're ready to write the file.
    PrintWriter out = new PrintWriter(
      new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8"));
    out.println(str);
    out.close();
  } // writeHits()

  /**
   * Formats a list of hits using the resultFormatter stylesheet.
   *
   * @param outFile       Where to write the results.
   * @param result        Hits resulting from the query request
   * @param displayStyle  Path of the resultFormatter stylesheet
   */
  protected void formatHits(File outFile, QueryResult result,
                            String displayStyle)
    throws Exception 
  {
    // Locate the display stylesheet.
    Templates displaySheet = stylesheetCache.find(displayStyle);

    // Make a transformer for this specific query.
    Transformer trans = displaySheet.newTransformer();

    // Make an input document for it based on the document hits. Insert
    // an attribute documenting how long the query took, including
    // formatting the hits.
    //
    String hitsString = result.hitsToString("crossQueryResult", null);
    Source sourceDoc = new StreamSource(new StringReader(hitsString));

    // Make sure errors get directed to the right place.
    if (!(trans.getErrorListener() instanceof XTFSaxonErrorListener))
      trans.setErrorListener(new XTFSaxonErrorListener());

    // Do it!
    FileOutputStream out = new FileOutputStream(outFile);
    trans.transform(sourceDoc, new StreamResult(out));
    out.close();
  } // formatHits()

  /** Writes the search tree to a file */
  private void writeTree(File outFile, SearchTree tree)
    throws IOException 
  {
    OutputStreamWriter ow = new OutputStreamWriter(new FileOutputStream(outFile),
                                                   "UTF-8");
    PrintWriter out = new PrintWriter(ow);
    String strVersion = XMLWriter.toString(tree);
    out.println(strVersion);
    out.close();
  } // writeHits()

  /**
   * Formats a search tree with a stylesheet, and writes it to the output
   * file.
   *
   * @param outFile       Where to write the results.
   * @param tree          Tree to output from
   * @param displaySheet  The resultFormatter stylesheet
   */
  protected void formatTree(File outFile, SearchTree tree, PreparedStylesheet displaySheet)
    throws Exception 
  {
    // Make a transformer for this specific query.
    Transformer trans = displaySheet.newTransformer();
    
    // We need a special key manager on the lazy tree, so that we can
    // use lazily stored keys on disk.
    //
    Controller controller = (Controller) trans;
    Executable e = controller.getExecutable();
    KeyManager k = e.getKeyManager();
    if (!(k instanceof LazyKeyManager))
      e.setKeyManager(new LazyKeyManager(controller.getConfiguration(), k));

    // Make sure errors get directed to the right place.
    if (!(trans.getErrorListener() instanceof XTFSaxonErrorListener))
      trans.setErrorListener(new XTFSaxonErrorListener());

    // Do it!
    FileOutputStream out = new FileOutputStream(outFile);
    trans.transform(tree, new StreamResult(out));
    out.close();
  } // formatHits()

  /**
   * Breaks up a string by newlines into an array of strings, one per line.
   *
   * @param str   String to break up
   * @return      Array of the lines
   */
  String[] slurp(String str) 
  {
    BufferedReader br = new BufferedReader(new StringReader(str));
    Vector lines = new Vector(100);
    while (true) 
    {
      try {
        String line = br.readLine();
        if (line == null)
          break;
        lines.add(line);
      }
      catch (IOException e) {
        assert false : "String reader should never have IO exception";
        throw new RuntimeException(e);
      }
    } // while

    return (String[])lines.toArray(new String[lines.size()]);
  } // slurp()

  /**
   * Compares two strings for equality. If not the same, a message
   * is printed.
   *
   * @param result1   First string
   * @param result2   Second string
   * @return          true if equal, false if not.
   */
  private boolean sameResults(String result1, String result2) 
  {
    if (result1.equals(result2))
      return true;

    if (result1.startsWith("Exception encountered") &&
        result2.startsWith("Exception encountered")) 
    {
      Trace.warning("\nExceptions differ: " + result1 + "\nvs\n" + result2);
      return true;
    }

    Trace.info("\n*** Mismatch! ***");
    return false;
  } // sameResults()

  private boolean filesEqual(File file1, File file2)
    throws IOException 
  {
    String s1 = readFile(file1);
    String s2 = readFile(file2);
    return sameResults(s1, s2);
  } // filesEqual()

  private String readFile(File file)
    throws IOException 
  {
    InputStreamReader reader = new InputStreamReader(new FileInputStream(file),
                                                     "UTF-8");
    int size = (int)file.length();
    char[] buf = new char[size + 1];
    int got = reader.read(buf);
    reader.close();
    if (got == size + 1 || got <= 0)
      throw new IOException("Error reading file " + file.toString());
    return new String(buf, 0, got).replaceAll("\r\n", "\n");
  } // readFile()
} // class RegressTest
