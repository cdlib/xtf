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
import java.io.IOException;

import org.cdlib.xtf.util.DirSync;
import org.cdlib.xtf.util.Path;
import org.cdlib.xtf.util.Trace;

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

/**
 * This class is the main class for the TextIndexer program. <br><br>
 *
 * Internally, this class retrieves command line arguments, and processes them
 * in order to index source XML files into one or more Lucene databases. The
 * command line arguments required by the TextIndexer program are as follows:
 *
 * <blockquote dir=ltr style="MARGIN-RIGHT: 0px"><code>
 * <b>TextIndexer -config</b> <font color=#0000ff><i>CfgFilePath</i></font>
 * { {<b>-clean</b>|<b>-incremental</b>}?
 * {<b>-trace errors</b>|<b>warnings</b>|<b>info</b>|<b>debug</b>}?
 * <b>-index</b> <font color=#0000ff><i>IndexName</i></font> }+
 * </b></code></blockquote>
 *
 * The <code>-config</code> argument identifies an XML configuration file that
 * defines one or more indices to be created, updated, or deleted. This argument
 * must be the first argument passed, and it must be passed only once. For a
 * complete description of the contents of the configuration file, see the
 * {@link XMLConfigParser} class.<br><br>
 *
 * The <code>-clean</code> / <code>-incremental</code> argument is an optional
 * argument that specifies whether Lucene indices should be rebuilt from scratch
 * (<code>-clean</code>) or should be updated (<code>-incremental</code>). If
 * this argument is not specified, the default behavior is incremental. <br><br>
 *
 * The <code>-buildlazy</code> / <code>-nobuildlazy</code> argument is an
 * optional argument that specifies whether the indexer should build a
 * persistent ("lazy") version of each document during the indexing process.
 * The lazy files are stored in the index directory, and they speed dynaXML
 * access later. If this argument is not specified, the default behavior is
 * to build lazy versions of the documents. <br><br>
 *
 * The <code>-optimize</code> / <code>-nooptimize</code> argument is an optional
 * argument that specifies whether the indexer should optimize the indexes after
 * they are built. Optimization improves query speed, but can take a very long
 * time to complete depending on the index size. If this argument is not
 * specified, the default behavior is to optimize. <br><br>
 *
 * The <code>-trace</code> argument is an optional argument that sets the level
 * of output displayed by the text indexer. The output levels are defined as
 * follows:
 *
 * <blockquote dir=ltr style="MARGIN-RIGHT: 0px">
 *   <code>errors</code>  - Only error messages are displayed. <br>
 *   <code>warnings</code> -  Both error and warning messages are displayed. <br>
 *   <code>info</code> - Error, warning, and informational messages are displayed. <br>
 *   <code>debug</code> - Low level debug output is displayed in addition to
 *   error, warning and informational messages.<br><br>
 * </blockquote>
 *
 * If this argument is not specified, the TextIndexer defaults to displaying
 * informational (<code>info</code>) level messages.<br><br>
 *
 * The <code>-index</code> argument identifies the name of the index to be
 * created/updated. The name must be one of the index names contained in the
 * configuration file specified as the first parameter. As is mentioned above,
 * the <code>-config</code> parameter must be specified first. After that,
 * the remaining arguments may be used one or more times to update a single
 * index or multiple indices. <br><br><br>
 *
 * A simple example of a command line parameters for the TextIndexer might
 * look like this:
 * <br><br>
 *
 *   <code><blockquote dir=ltr style="MARGIN-RIGHT: 0px"><b>
 *   TextIndexer -config IdxConfig.xml -clean -index AllText
 *   </b></blockquote></code>
 *
 * This example assumes that the config file is called <code>IdxConfig.xml</code>,
 * that the config file contains an entry for an index called <b>AllText</b>, and
 * that the user wants the index to be rebuilt from scratch (because of the
 * <code>-clean</code> argument. <br><br>
 *
 */
public class TextIndexer 
{
  /** The version of the text indexer (placed into any indexes created) */
  public static final String CURRENT_VERSION = "2.2";
  
  /** The minimum index version that we can read */
  public static final String REQUIRED_VERSION = "2.0";
  
  //////////////////////////////////////////////////////////////////////////////

  /** Main entry-point for the Text Indexer. <br><br>
   *
   *  This function takes the command line arguments passed and uses them to
   *  create or update the specified indices with the specified source text.
   *  <br><br>
   *
   *  @param  args    Command line arguments to process. The command line
   *  arguments required by the TextIndexer program are as follows:
   *
   * <blockquote dir=ltr style="MARGIN-RIGHT: 0px"><code>
   * <b>TextIndexer -config</b> <font color=#0000ff><i>CfgFilePath</i></font>
   * { {<b>-clean</b>|<b>-incremental</b>}?
   * {<b>-trace errors</b>|<b>warnings</b>|<b>info</b>|<b>debug</b>}?
   * <b>-index</b> <font color=#0000ff><i>IndexName</i></font> }+
   * </b></code></blockquote>
   *
   * For a complete description of each command line argument, see the
   * {@link TextIndexer} class description.
   * <br><br>
   *
   */
  public static void main(String[] args) 
  {
    try 
    {
      IndexerConfig cfgInfo = new IndexerConfig();
      XMLConfigParser cfgParser = new XMLConfigParser();
      IdxTreeCleaner indexCleaner = new IdxTreeCleaner();

      int startArg = 0;
      boolean showUsage = false;

      boolean firstIndex = true;

      long startTime = System.currentTimeMillis();

      // Regardless of whether we succeed or fail, say our name.
      Trace.info("TextIndexer v" + CURRENT_VERSION);
      Trace.info("");
      Trace.tab();

      // Make sure the XTF_HOME environment variable is specified.
      cfgInfo.xtfHomePath = System.getProperty("xtf.home");
      if (cfgInfo.xtfHomePath == null || cfgInfo.xtfHomePath.length() == 0) {
        Trace.error("Error: xtf.home property not found");
        System.exit(1);
      }

      cfgInfo.xtfHomePath = Path.normalizePath(cfgInfo.xtfHomePath);
      if (!new File(cfgInfo.xtfHomePath).isDirectory()) {
        Trace.error(
          "Error: xtf.home directory \"" + cfgInfo.xtfHomePath +
          "\" does not exist or cannot be read.");
        System.exit(1);
      }
      File xtfHomeFile = new File(cfgInfo.xtfHomePath);

      // Perform indexing for each index specified.
      for (;;) 
      {
        // The minimum set of arguments consists of the name of an index
        // to update. If we don't get at least that two, we will show the 
        // usage text and bail.
        //
        if (args.length < 2)
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

            // Since we're starting a new index, reset the 'must
            // clean' flag so that the index will get cleaned if
            // requested.
            //
            cfgInfo.mustClean = cfgInfo.clean;

            // Set the tracing level specified by the user.
            Trace.setOutputLevel(cfgInfo.traceLevel);
          } // else( ret != -1 )

          // Save the new start argument for the next time.
          startArg = ret;
        } // else( args.length >= 4 )

        // If the config file was not okay, print a message and bail out.
        if (showUsage) 
        {
          // Do so...
          Trace.error("Usage: textIndexer {options} -index indexname");
          Trace.error("Basic options:");
          Trace.tab();
          Trace.error("-config <configfile>                  Default: -config textIndexer.conf");
          Trace.error("-incremental|-clean|-force            Default: -incremental");
          Trace.error("-dir <subdir1> -dir <subdir2>...      Default: (all directories)");
          Trace.error("-dirlist <fileOfSubdirs>              Default: (all directories)");
          Trace.error("\n");
          Trace.untab();
          Trace.error("Advanced options:");
          Trace.error("-nosync|-syncfrom <otherIndexDir>     Default: -nosync");
          Trace.error("-optimize|-nooptimize                 Default: -optimize");
          Trace.error("-trace errors|warnings|info|debug     Default: -trace info");
          Trace.error("-buildlazy|-nobuildlazy               Default: -buildlazy");
          Trace.error("-updatespell|-noupdatespell           Default: -updatespell");
          Trace.tab();
          Trace.error("\n");
          Trace.untab();

          // And then bail.
          System.exit(1);
        } // if( showUsage )    

        // If rotation is enabled for this index, target the "-new" directory.
        if (cfgInfo.indexInfo.rotate) 
        {
          cfgInfo.indexInfo.indexPath = 
            cfgInfo.indexInfo.indexPath.replaceFirst("/$", "-new/");
        }

        // Lock each index directory as we encounter it.
        // If this is our first time through, purge any incomplete
        // documents from the indices, and tell the user what 
        // we're doing.
        //
        // Clean all indices below the root index directory. 
        File idxRoot = new File(
            Path.resolveRelOrAbs(xtfHomeFile, cfgInfo.indexInfo.indexPath));
        if (firstIndex) 
        {
          if (!cfgInfo.mustClean) 
          {
            Trace.info("");
            Trace.info("Purging Incomplete Documents From Indexes:");
            Trace.tab();

            indexCleaner.processDir(idxRoot);

            Trace.untab();
            Trace.info("Done.");
          }

          Trace.info("");
          Trace.info("Indexing New/Updated Documents:");
          Trace.tab();

          // Indicate that the next pass through this loop is not 
          // the first one.
          //
          firstIndex = false;
        }

        // Say what index we're working on.
        Trace.info("Index: \"" + cfgInfo.indexInfo.indexName + "\"");

        // And if we're debugging, say some more about the index.
        if (Trace.getOutputLevel() == Trace.debug)
          Trace.more(
            " [ Chunk Size = " + cfgInfo.indexInfo.getChunkSize() +
            ", Overlap = " + cfgInfo.indexInfo.getChunkOvlp() + " ]");
        Trace.tab();

        // Process the index directories.
        if (!cfgInfo.skipIndexing) 
          doIndexing(cfgInfo, xtfHomeFile);

        Trace.untab();
        Trace.info("Done.");
      } // for(;;)

      Trace.untab();
      Trace.info("Done.");

      // Optimize the indices, now that we're all done processing them.
      if (cfgInfo.optimize) 
      {
        // Create a tree culler.
        IdxTreeOptimizer optimizer = new IdxTreeOptimizer();

        Trace.info("");
        Trace.info("Optimizing Index:");
        Trace.tab();

        File idxRootDir = new File(Path.resolveRelOrAbs(
                                                        cfgInfo.xtfHomePath,
                                                        cfgInfo.indexInfo.indexPath));
        optimizer.processDir(idxRootDir);

        Trace.untab();
        Trace.info("Done.");
      }
      else {
        Trace.info("");
        Trace.info("Skipping Optimization Pass.");
      }

      // Create spelling dictionaries, now that we're done indexing.
      if (cfgInfo.updateSpellDict && cfgInfo.indexInfo.createSpellcheckDict) 
      {
        IdxTreeDictMaker dictMaker = new IdxTreeDictMaker();

        Trace.info("");
        Trace.info("Updating Spellcheck Dictionary:");
        Trace.tab();

        File idxRootDir = new File(Path.resolveRelOrAbs(
                                                        cfgInfo.xtfHomePath,
                                                        cfgInfo.indexInfo.indexPath));
        dictMaker.processDir(idxRootDir);

        Trace.untab();
        Trace.info("Done.");
      }
      else {
        Trace.info("");
        Trace.info("Skipping Spellcheck Dictionary Pass.");
      }
      
      // Finally, perform index rotation if specified.
      if (cfgInfo.indexInfo.rotate)
      {
        if (cfgInfo.rotate)
        {
          Trace.info("");
          Trace.info("Performing Index Rotation:");
          Trace.tab();

          doRotation(cfgInfo, xtfHomeFile);
        }
        else {
          Trace.info("");
          Trace.info("Skipping Index Rotation Pass.");
        }
      }

      Trace.untab();
      Trace.info("");

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

      Trace.info("Indexing complete.");
      Trace.info("");
    } // try

    // Log any unhandled exceptions.    
    catch (Throwable t) 
    {
      Trace.clearTabs();
      Trace.error("*** Error: " + t.getClass());
      Trace.error("");
      t.printStackTrace(System.out);
      Trace.error("Indexing Process Aborted.");

      System.exit(1);
    }

    // Exit successfully.
    return;
  } // main()

  
  //////////////////////////////////////////////////////////////////////////////

  /**
   * Handles the main work of adding and removing documents to/from the index.
   */
  private static void doIndexing(IndexerConfig cfgInfo, File xtfHomeFile)
    throws Exception 
  {
    SrcTreeProcessor srcTreeProcessor = new SrcTreeProcessor();
    srcTreeProcessor.open(cfgInfo);
    
    // Start at the root directory specified by the config file. 
    String srcRootDir = Path.resolveRelOrAbs(xtfHomeFile,
                                             cfgInfo.indexInfo.sourcePath);

    if (cfgInfo.indexInfo.cloneData) 
    {
      Trace.info("Cloning Data Directories...");
      if (cfgInfo.indexInfo.subDirs == null)
        srcTreeProcessor.cloneData(new File(srcRootDir), true);
      else {
        for (String subDir : cfgInfo.indexInfo.subDirs) {
          String subDirPath = Path.resolveRelOrAbs(srcRootDir, Path.normalizePath(subDir));
          srcTreeProcessor.cloneData(new File(subDirPath), false);
        }
      }
      Trace.more(Trace.info, " Done.");
    }
      
    Trace.info("Scanning Data Directories...");
    if (cfgInfo.indexInfo.subDirs == null)
      srcTreeProcessor.processDir(new File(srcRootDir), true);
    else {
      for (String subDir : cfgInfo.indexInfo.subDirs) {
        String subDirPath = Path.resolveRelOrAbs(srcRootDir, Path.normalizePath(subDir));
        srcTreeProcessor.processDir(new File(subDirPath), false);
      }
    }
    Trace.more(Trace.info, " Done.");

    srcTreeProcessor.close();
    
    // Cull files which are present in the index but missing
    // from the filesystem.
    //
    IdxTreeCuller culler = new IdxTreeCuller();

    Trace.info("Removing Missing Documents From Index:");
    Trace.tab();

    culler.cullIndex(new File(cfgInfo.xtfHomePath), cfgInfo.indexInfo);

    Trace.untab();
    Trace.info("Done.");
  }
  
  
  //////////////////////////////////////////////////////////////////////////////

  /**
   * Rotates a rotation-enabled index.
   * 
   * @throws IOException if anything goes wrong 
   */
  private static void doRotation(IndexerConfig cfgInfo, File xtfHomeFile) 
    throws IOException 
  {
    // Let's figure out the paths to the various versions of the index
    String indexPath = cfgInfo.indexInfo.indexPath;
    assert indexPath.endsWith("-new/"); // Should have been modified above
    
    File newIndex = new File(
        Path.resolveRelOrAbs(xtfHomeFile, indexPath));
    File currentIndex = new File(Path.resolveRelOrAbs(
        xtfHomeFile, indexPath.replaceFirst("-new/$", "/")));
    File pendingIndex = new File(Path.resolveRelOrAbs(
        xtfHomeFile, indexPath.replaceFirst("-new/$", "-pending/")));
    File spareIndex = new File(Path.resolveRelOrAbs(
        xtfHomeFile, indexPath.replaceFirst("-new/$", "-spare/")));
    
    // If there's a pending index, it means the servlets haven't gotten around
    // to grabbing it yet, so we can't rotate yet.
    //
    if (pendingIndex.exists()) {
      Trace.info("Rotation skipped: a previous index is still pending.");
      return;
    }
    
    // If nothing has happened to the new index since the current one was
    // made, then it would be silly to rotate.
    //
    if (currentIndex.exists() &&
        IndexSync.newestTime(currentIndex).equals(IndexSync.newestTime(newIndex)))
    {
      Trace.info("Rotation skipped: no changes found.");
      return;
    }
    
    // To rotate, we'll need a spare. If there isn't one yet, create it from 
    // scratch by cloning the entire new index.
    //
    if (!spareIndex.exists()) {
      Trace.info("Creating spare index clone.");
      if (!spareIndex.mkdir())
        throw new IOException("Error creating spare index '" + spareIndex + "'");
      new DirSync().syncDirs(newIndex, spareIndex);
      Trace.more(Trace.info, " Done.");
    }
    
    // If there is a spare already, update it from the new index. We use
    // the fancy IndexSync class that knows how to do this faster than a
    // brute-force scan.
    //
    else {
      Trace.info("Bringing spare index clone up to date:");
      Trace.tab();
      new IndexSync().syncDirs(cfgInfo.indexInfo.indexName, newIndex, spareIndex);
      Trace.untab();
      Trace.info("Done.");
    }
    
    // Ready to rotate. Let's do it!
    Trace.info("Rotating Indexes:  [pending]  <-  [new]  <-  [spare]");
    renameOrElse(newIndex, pendingIndex);
    renameOrElse(spareIndex, newIndex);
    Trace.untab();
    Trace.info("Done.");
  }

  /**
   * Utility function to perform a rename, and throw an exception if the it
   * fails.
   * @throws IOException 
   */
  private static void renameOrElse(File from, File to) throws IOException
  {
    if (!from.renameTo(to))
      throw new IOException("Error renaming '" + from + "' to '" + to + "'");
  }
  
} // class textIndexer
