package org.cdlib.xtf.textIndexer;


/**
 * Copyright (c) 2006, Regents of the University of California
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
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Vector;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.cdlib.xtf.textEngine.NativeFSDirectory;
import org.cdlib.xtf.util.Path;
import org.cdlib.xtf.util.Trace;

/**
 * This class merges the contents of two or more XTF indexes, with certain
 * caveats.
 *
 * @author Martin Haye
 */
public class IndexMerge 
{
  //////////////////////////////////////////////////////////////////////////////

  /** Main entry-point for the index merger. <br><br>
   *
   *  This function takes the command line arguments passed and uses them to
   *  find the indexes and merge them.
   */
  public static void main(String[] args) 
  {
    Trace.info("IndexMerge v. 3.1");

    try 
    {
      IndexerConfig cfgInfo = new IndexerConfig();
      XMLConfigParser cfgParser = new XMLConfigParser();

      int startArg = 0;
      boolean showUsage = false;

      // Make sure the XTF_HOME environment variable is specified.
      cfgInfo.xtfHomePath = System.getProperty("xtf.home");
      if (cfgInfo.xtfHomePath == null || cfgInfo.xtfHomePath.length() == 0) {
        Trace.error("Error: xtf.home property not found");
        return;
      }

      cfgInfo.xtfHomePath = Path.normalizePath(cfgInfo.xtfHomePath);
      if (!new File(cfgInfo.xtfHomePath).isDirectory()) {
        Trace.error(
          "Error: xtf.home directory \"" + cfgInfo.xtfHomePath +
          "\" does not exist or cannot be read.");
        return;
      }

      // Parse the command-line arguments.
      Vector mergePaths = new Vector();
      HashSet pathSet = new HashSet();
      while (!showUsage && startArg < args.length) 
      {
        // The minimum set of arguments consists of the name of an index
        // to read and an output index. That requires four arguments; if
        // we don't get that many, we will show the usage text and bail.
        //
        if (args.length < 4)
          showUsage = true;

        // We have enough arguments, so...
        else 
        {
          // Read the command line arguments until we find what we
          // need to do some work, or until we run out.
          //
          int ret = cfgInfo.readCmdLine(args, startArg);

          // If we didn't find enough command line arguments... 
          if (ret == -1) 
          {
            // And this is the first time we've visited the command 
            // line arguments, avoid trying to doing work and just 
            // display the usage text. Otherwise, we're done.
            //
            if (startArg == 0)
              showUsage = true;
            else
              break;
          } // if( ret == -1 )

          // We did find enough command line arguments, so...
          //
          else 
          {
            // Make sure the configuration path is absolute
            if (!(new File(cfgInfo.cfgFilePath).isAbsolute())) {
              cfgInfo.cfgFilePath = Path.resolveRelOrAbs(cfgInfo.xtfHomePath,
                                                         cfgInfo.cfgFilePath);
            }

            // Get the configuration for the index specified by the 
            // current command line arguments.
            //
            if (cfgParser.configure(cfgInfo) < 0) {
              Trace.error(
                "Error: index '" + cfgInfo.indexInfo.indexName +
                "' not found\n");
              System.exit(1);
            }
          } // else( ret != -1 )

          // Save the new start argument for the next time.
          startArg = ret;

          // The indexes should all be in different directories.
          IndexInfo idxInfo = cfgInfo.indexInfo;
          String idxPath = Path.resolveRelOrAbs(cfgInfo.xtfHomePath,
                                                idxInfo.indexPath);
          if (pathSet.contains(idxPath)) {
            Trace.error(
              "Error: indexes to be merged must be in separate directories.");
            System.exit(1);
          }
          pathSet.add(idxPath);

          // Save the index info for the merge
          mergePaths.add(idxPath);
        } // else
      } // for

      // At least two indexes should be specified.
      if (mergePaths.size() < 2)
        showUsage = true;

      // Show usage message if any problems found.
      if (showUsage) 
      {
        // Do so...
        Trace.error("  usage: ");
        Trace.tab();
        Trace.error(
          "indexMerge -config <config1> -index <index1> " +
          "-config <config2> -index <inputIndex2> ...\n\n" +
          "Sample use:\n" +
          "1. Make two config files, each with different input and output directories.\n" +
          "2. textIndexer -config conf/textIndexer.conf1 -clean -noupdatespell -index default\n" +
          "3. textIndexer -config conf/textIndexer.conf2 -clean -noupdatespell -index default\n" +
          "4. indexMerge -config conf/textIndexer.conf1 -index default -config conf/textIndexer.conf2 -index default\n\n" +
          "This *experimental* command merges data from all the specified indexes into the \n" +
          "first index.\n\n");
        Trace.untab();

        // And then bail.
        System.exit(1);
      } // if( showUsage )    

      // Make sure all the indexes exist, and that their parameters are at
      // least minimally compatible.
      //
      DirInfo[] dirInfos = new DirInfo[mergePaths.size()];
      boolean createTarget = false;
      for (int i = 0; i < mergePaths.size(); i++) 
      {
        String idxPath = (String)mergePaths.get(i);
        if (!IndexReader.indexExists(idxPath)) 
        {
          // It's okay if the target index doesn't exist.
          if (i == 0) {
            createTarget = true;
            dirInfos[0] = new DirInfo(idxPath, null);
            continue;
          }
          throw new RuntimeException(
            "Error: Cannot locate index in directory '" + idxPath + "'");
        }

        Directory srcDir = NativeFSDirectory.getDirectory(idxPath);
        dirInfos[i] = readInfo(idxPath, srcDir);

        // Check for parameter compatibility
        if ((i == 1 && !createTarget) || (i > 1)) {
          if (dirInfos[i].chunkOverlap != dirInfos[i - 1].chunkOverlap)
            throw new RuntimeException(
              "Error: index parameters must match exactly (chunkOverlap mismatch detected)");
          if (dirInfos[i].chunkSize != dirInfos[i - 1].chunkSize)
            throw new RuntimeException(
              "Error: index parameters must match exactly (chunkSize mismatch detected)");
          if (!dirInfos[i].stopWords.equals(dirInfos[i - 1].stopWords))
            throw new RuntimeException(
              "Error: index parameters must match exactly (stopWords mismatch detected)");
          if (!dirInfos[i].accentMapName.equals(dirInfos[i - 1].accentMapName))
            throw new RuntimeException(
              "Error: index parameters must match exactly (accentMapName mismatch detected)");
          if (!dirInfos[i].pluralMapName.equals(dirInfos[i - 1].pluralMapName))
            throw new RuntimeException(
              "Error: index parameters must match exactly (pluralMapName mismatch detected)");
        }
      } // for

      // Well, enough preparing. Let's do the job we were sent to do.
      doMerge(dirInfos, createTarget);
    } // try

    // Log any unhandled exceptions.    
    catch (Exception e) {
      Trace.error("*** Last Chance Exception: " + e.getClass());
      Trace.error("             With message: " + e.getMessage());
      Trace.error("");
      e.printStackTrace(System.out);
      System.exit(1);
    }
    catch (Throwable t) {
      Trace.error("*** Last Chance Exception: " + t.getClass());
      Trace.error("             With message: " + t);
      Trace.error("");
      t.printStackTrace(System.out);
      System.exit(1);
    }

    // Exit successfully.
    System.exit(0);
  } // main()

  //////////////////////////////////////////////////////////////////////////////
  private static DirInfo readInfo(String path, Directory dir)
    throws IOException 
  {
    IndexReader indexReader = IndexReader.open(dir);

    try 
    {
      // Fetch the chunk size and overlap from the index.
      Hits match = new IndexSearcher(indexReader).search(
        new TermQuery(new Term("indexInfo", "1")));
      if (match.length() == 0)
        throw new IOException("Index missing indexInfo doc");
      assert match.id(0) == 0 : "indexInfo chunk must be first in index";
      Document doc = match.doc(0);

      // Pick out all the info we need.
      DirInfo ret = new DirInfo(path, dir);
      ret.chunkSize = Integer.parseInt(doc.get("chunkSize"));
      ret.chunkOverlap = Integer.parseInt(doc.get("chunkOvlp"));
      ret.stopWords = doc.get("stopWords");
      ret.pluralMapName = doc.get("pluralMap");
      ret.accentMapName = doc.get("accentMap");

      return ret;
    }
    finally {
      indexReader.close();
    }
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
   * Merge a bunch of indexes together.
   */
  private static void doMerge(DirInfo[] dirInfos, boolean createTarget)
    throws InterruptedException, IOException 
  {
    long startTime = System.currentTimeMillis();

    // Let the user know what's about to occur.
    Trace.info("Ready to merge data from the following index directories:");
    Trace.tab();
    for (int i = 1; i < dirInfos.length; i++)
      Trace.info(dirInfos[i].path);
    Trace.untab();
    Trace.info("into (and including data from) index directory:");
    Trace.tab();
    Trace.info(dirInfos[0].path);
    Trace.untab();

    // Give them time to abort without consequences.
    Trace.info("");
    Trace.info("Merge will begin in 5 seconds ... ");
    Thread.sleep(5000);

    Trace.info("");
    Trace.info("Merging indexes ... ");
    Trace.tab();

    // Open the writer for the target Lucene index
    IndexWriter writer = new IndexWriter(NativeFSDirectory.getDirectory(dirInfos[0].path),
                                         new StandardAnalyzer(),
                                         createTarget);

    // Merge each piece (spelling, lazy files, main indexes)
    mergeSpelling(dirInfos);
    mergeLazy(dirInfos);
    mergeAux(dirInfos);
    mergeLucene(writer, dirInfos);

    // All done. Report how long we spent.
    Trace.untab();
    long timeMsec = System.currentTimeMillis() - startTime;
    long timeSec = timeMsec / 1000;
    long timeMin = timeSec / 60;
    long timeHour = timeMin / 60;

    Trace.info("Total time: ");
    if (timeHour > 0) {
      String ending = (timeHour == 1) ? "" : "s";
      Trace.more(Trace.info, timeHour + " hour" + ending + ", ");
    }
    if (timeMin > 0) {
      String ending = ((timeMin % 60) == 1) ? "" : "s";
      Trace.more(Trace.info, (timeMin % 60) + " minute" + ending + ", ");
    }
    String ending = ((timeSec % 60) == 1) ? "" : "s";
    Trace.more(Trace.info, (timeSec % 60) + " second" + ending + ".");
    Trace.info("Merge completed successfully.");
    Trace.info("");
  } // doMerge()

  //////////////////////////////////////////////////////////////////////////////
  private static void mergeSpelling(DirInfo[] dirInfos)
    throws IOException 
  {
    // If there are none to do, skip this step.
    boolean anyToDo = false;
    for (int i = 1; i < dirInfos.length; i++) {
      String sourceDir = dirInfos[i].path;
      File sourceFile = new File(sourceDir + "spellDict/newWords.txt");
      if (!sourceFile.isFile() && sourceFile.canRead())
        continue;
      anyToDo = true;
    }

    if (!anyToDo)
      return;

    Trace.info("Processing spellcheck word lists ... ");

    // Append each input file.
    for (int i = 1; i < dirInfos.length; i++) 
    {
      String sourceDir = dirInfos[i].path;
      File sourceFile = new File(sourceDir + "spellDict/newWords.txt");
      if (!sourceFile.isFile() && sourceFile.canRead())
        continue;

      // Open the target file.
      String targetDir = dirInfos[0].path;
      Path.createPath(targetDir + "spellDict");
      File targetFile = new File(targetDir + "spellDict/newWords.txt");
      PrintWriter targetWriter = new PrintWriter(
        new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
                                                                       targetFile,
                                                                       targetFile.isFile()),
                                                  "UTF-8")));

      BufferedReader sourceReader = new BufferedReader(
        new InputStreamReader(new FileInputStream(sourceFile), "UTF-8"));

      boolean eof = false;
      while (!eof) 
      {
        try {
          String word = sourceReader.readLine();
          if (word == null)
            eof = true;
          else
            targetWriter.println(word);
        }
        catch (EOFException e) {
          eof = true;
        }
        catch (IOException e) {
          Trace.warning(
            "Warning: Exception encountered (may be due to unfinished index): " +
            e);
          eof = true;
        }
      }

      sourceReader.close();
      targetWriter.close();
    } // for

    Trace.more("Done.");
  } // mergeSpelling()

  //////////////////////////////////////////////////////////////////////////////
  private static void mergeAux(DirInfo[] dirInfos)
    throws IOException 
  {
    // If there are none to do, skip this step.
    boolean anyToDo = false;
    for (int i = 1; i < dirInfos.length; i++) 
    {
      String sourceDir = dirInfos[i].path;

      File accentFile = new File(sourceDir + dirInfos[i].accentMapName);
      File pluralFile = new File(sourceDir + dirInfos[i].pluralMapName);
      File tokFldFile = new File(sourceDir + "tokenizedFields.txt");

      if (accentFile.canRead() || pluralFile.canRead() || tokFldFile.canRead())
        anyToDo = true;
    }

    if (!anyToDo)
      return;

    Trace.info("Processing auxiliary files ... ");

    // Copy files from each directory...
    for (int i = 1; i < dirInfos.length; i++) 
    {
      File accentSrc = new File(dirInfos[i].path, dirInfos[i].accentMapName);
      File accentDst = new File(dirInfos[0].path, dirInfos[i].accentMapName);
      if (accentSrc.canRead() && !accentDst.canRead())
        Path.copyFile(accentSrc, accentDst);

      File pluralSrc = new File(dirInfos[i].path, dirInfos[i].pluralMapName);
      File pluralDst = new File(dirInfos[0].path, dirInfos[i].pluralMapName);
      if (pluralSrc.canRead() && !pluralDst.canRead())
        Path.copyFile(pluralSrc, pluralDst);
      
      File tokFldSrc = new File(dirInfos[i].path, "tokenizedFields.txt");
      File tokFldDst = new File(dirInfos[0].path, "tokenizedFields.txt");
      if (tokFldSrc.canRead() && !tokFldDst.canRead())
        mergeTokFldFiles(tokFldSrc, tokFldDst);
      else if (tokFldSrc.canRead() && tokFldDst.canRead())
        mergeTokFldFiles(tokFldSrc, tokFldDst);
    } // for

    Trace.more("Done.");
  } // mergeAux()
  
  //////////////////////////////////////////////////////////////////////////////
  private static void mergeTokFldFiles(File file1, File file2) throws IOException
  {
    LinkedHashSet set = new LinkedHashSet();
    
    // Read in the first file
    BufferedReader reader = new BufferedReader(new FileReader(file1));
    String line;
    while ((line = reader.readLine()) != null)
      set.add(line);
    reader.close();
    
    // Add entries from the second file
    FileWriter writer = new FileWriter(file1, true /*append*/);
    reader = new BufferedReader(new FileReader(file2));
    while ((line = reader.readLine()) != null) {
      if (!set.contains(line))
        writer.append(line + "\n");
    }
    writer.close();
  }

  //////////////////////////////////////////////////////////////////////////////
  private static void mergeLazy(DirInfo[] dirInfos)
    throws IOException 
  {
    // Get the target lazy directory.
    String targetDir = dirInfos[0].path;

    // See if there are any source directories to merge.
    boolean anyToDo = false;
    for (int i = 1; i < dirInfos.length; i++) {
      String sourceDir = dirInfos[i].path;
      File lazyDir = new File(sourceDir, "lazy");
      if (lazyDir.isDirectory())
        anyToDo = true;
    }

    if (!anyToDo)
      return;

    Trace.info("Processing lazy tree files ... ");

    // Process each source directory.
    for (int i = 1; i < dirInfos.length; i++) {
      String sourceDir = dirInfos[i].path;
      mergeLazy(new File(sourceDir, "lazy"), new File(targetDir, "lazy"));
    } // for

    Trace.more("Done.");
  } // mergeLazy()

  //////////////////////////////////////////////////////////////////////////////
  private static void mergeLazy(File src, File dst)
    throws IOException 
  {
    // If the source is a file, copy it.
    if (src.isFile()) 
    {
      // If the target file already exists, don't overwrite.
      if (dst.isFile())
        return;

      // Copy away.
      Path.copyFile(src, dst);
      return;
    }

    // If the source is a directory, create the corresponding target 
    // directory, and copy the files and sub-directories.
    //
    if (src.isDirectory()) 
    {
      if (!dst.isDirectory()) {
        if (!Path.createPath(dst.toString()))
          throw new IOException("Error creating lazy file directory '" + dst +
                                "'");
      }

      // Process each sub-file
      String[] subFiles = src.list();
      for (int i = 0; i < subFiles.length; i++) {
        mergeLazy(new File(src, subFiles[i]), new File(dst, subFiles[i]));
      } // for
    } // if
  } // mergeLazy()

  //////////////////////////////////////////////////////////////////////////////
  private static void mergeLucene(IndexWriter writer, DirInfo[] dirInfos)
    throws IOException 
  {
    Trace.info("Processing Lucene indexes (can be very time-consuming) ... ");
    Directory[] dirs = new Directory[dirInfos.length - 1];
    for (int i = 1; i < dirInfos.length; i++)
      dirs[i - 1] = dirInfos[i].dir;
    writer.addIndexes(dirs);
    writer.optimize();
    writer.close();
    Trace.more("Done.");
  } // mergeLucene()

  //////////////////////////////////////////////////////////////////////////////
  private static class DirInfo 
  {
    public DirInfo(String idxPath, Directory srcDir) {
      this.path = idxPath;
      this.dir = srcDir;
    }

    String path;
    Directory dir;
    int chunkSize;
    int chunkOverlap;
    String stopWords;
    String pluralMapName;
    String accentMapName;
  } // class DirInfo
} // class IndexMerge
