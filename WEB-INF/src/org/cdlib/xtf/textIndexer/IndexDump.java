package org.cdlib.xtf.textIndexer;


/**
 * Copyright (c) 2005, Regents of the University of California
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
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import org.apache.lucene.chunk.DocNumMap;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.cdlib.xtf.textEngine.Constants;
import org.cdlib.xtf.textEngine.XtfSearcher;
import org.cdlib.xtf.util.Path;
import org.cdlib.xtf.util.Trace;

/**
 * This class dumps the contents of user-selected fields from an XTF text
 * index.
 *
 * @author Martin Haye
 */
public class IndexDump 
{
  //////////////////////////////////////////////////////////////////////////////

  /** Main entry-point for the index dumper. <br><br>
   *
   *  This function takes the command line arguments passed and uses them to
   *  find an index and print out fields in it.
   */
  public static void main(String[] args) 
  {
    try 
    {
      IndexerConfig cfgInfo = new IndexerConfig();
      XMLConfigParser cfgParser = new XMLConfigParser();

      int startArg = 0;
      boolean showUsage = false;
      boolean termFreqMode = false;
      boolean allFieldsMode = false;
      boolean xmlMode = false;

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

      // Write output in UTF-8 format.
      Writer out = new OutputStreamWriter(System.out, "UTF-8");

      // Process each index
      for (;;) 
      {
        // The minimum set of arguments consists of the name of an index
        // to scan and a field to dump. That requires three args; if we don't 
        // get that many, we will show the usage text and bail.
        //
        if (args.length < 3)
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
              return;
            }
          } // else( ret != -1 )

          // Save the new start argument for the next time.
          startArg = ret;
        } // else( args.length >= 4 )

        // Parse additional mode parameters
        Vector fieldNames = new Vector();
        while (startArg < args.length)
        {
          // Is term frequency mode enabled?
          if (args[startArg].equalsIgnoreCase("-termFreq")) 
          {
            startArg++;
            termFreqMode = true;
          }
  
          // Is all fields mode enabled?
          else if (args[startArg].equalsIgnoreCase("-allFields")) 
          {
            startArg++;
            allFieldsMode = true;
          }
  
          // Is XML mode enabled?
          else if (args[startArg].equalsIgnoreCase("-xml")) 
          {
            startArg++;
            xmlMode = true;
          }
          
          // Is a field name specified?
          else if (args[startArg].equals("-field"))
          {
            startArg++;
            if (startArg == args.length || args[startArg].startsWith("-"))
              showUsage = true;
            else 
            {
              if (args[startArg].equals("text") && !termFreqMode) {
                Trace.error("Error: contents of the 'text' field cannot be dumped");
                System.exit(1);
              }
              fieldNames.add(args[startArg]);
              startArg++;
            }
          }
          
          // Barf on other parameters
          else {
            showUsage = true;
            break;
          }
        }

        // Do a little checking for sanity
        if ((allFieldsMode && !fieldNames.isEmpty()) ||
            (!allFieldsMode && fieldNames.isEmpty()))
          showUsage = true;

        String[] fieldNameArray = (String[])fieldNames.toArray(
          new String[fieldNames.size()]);

        // If the config file was read successfully, we can begin processing.
        if (showUsage) 
        {
          // Do so...
          Trace.error("  usage: ");
          Trace.tab();
          Trace.error(
            "indexDump {-config <configfile>} -index <indexname> " +
            "{-xml} {-termFreq} {-allFields|-field fieldName1 {-field fieldName2}*}... \n\n");
          Trace.untab();

          // And then bail.
          System.exit(1);
        } // if( showUsage )    

        // Try to open the index for reading. If we fail and throw, skip the 
        // index.
        //
        IndexInfo idxInfo = cfgInfo.indexInfo;
        String idxPath = Path.resolveRelOrAbs(cfgInfo.xtfHomePath,
                                              idxInfo.indexPath);
        XtfSearcher searcher = new XtfSearcher(idxPath, 30);
        IndexReader indexReader = searcher.indexReader();
        DocNumMap docNumMap = searcher.docNumMap();

        // Go for it.
        if (termFreqMode)
          dumpTermFreqs(indexReader, docNumMap, fieldNameArray, out);
        else
          dumpFields(indexReader, fieldNameArray, xmlMode, allFieldsMode, out);

        // Close the index reader, and make sure all output is displayed.
        indexReader.close();
        out.flush();
      } // for(;;)
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
  private static void dumpDelimitedRecord(ArrayList<Field> fieldData, Writer out)
    throws IOException 
  {
    String prevName = null;
    for (Field f : fieldData)
    {
      if (prevName != null) {
        if (f.name().equals(prevName))
          out.write(";");
        else
          out.write("|");
      }
      prevName = f.name();

      out.write(stripValue(f.stringValue(), true));
    }
    out.write("|\n");
  }
  

  //////////////////////////////////////////////////////////////////////////////
  private static void dumpXmlRecord(ArrayList<Field> fieldData, Writer out)
    throws IOException 
  {
    out.write("  <document>\n");
    for (Field f : fieldData)
    {
      out.write("    <" + f.name() + ">");
      out.write(stripValue(f.stringValue(), false));
      out.write("</" + f.name() + ">\n");
    }
    out.write("  </document>\n");
  }
  
  //////////////////////////////////////////////////////////////////////////////
  private static void dumpFields(IndexReader indexReader, String[] fieldNames,
                                 boolean xmlMode, boolean allFieldsMode, Writer out)
    throws IOException 
  {
    if (xmlMode) {
      out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
      out.write("<xtfIndexDocuments>\n");
    }
      
    // Iterate every document.
    int maxDoc = indexReader.maxDoc();
    for (int i = 0; i < maxDoc; i++) 
    {
      // Skip deleted docs
      if (indexReader.isDeleted(i))
        continue;
      
      // Skip non-metadata docs (e.g. indexInfo, text blocks)
      Document doc = indexReader.document(i);
      if (doc.getField("docInfo") == null)
        continue;

      // See if any of the desired fields are present, and if so, record
      // their values.
      //
      ArrayList<Field> toPrint = new ArrayList();
      if (allFieldsMode) {
        for (Field f : (List<Field>)doc.getFields())
        {
          // Only output user fields (i.e. skip XTF-internal fields)
          if (!f.name().matches("^(docInfo|chunkCount|key|fileDate)$"))
            toPrint.add(f);
        }
      }
      else
      {
        for (int j = 0; j < fieldNames.length; j++) 
        {
          Field[] got = doc.getFields(fieldNames[j]);
          if (got != null)
            toPrint.addAll(Arrays.asList(got));
        }
      }
      
      if (!toPrint.isEmpty())
      {
        if (xmlMode)
          dumpXmlRecord(toPrint, out);
        else
          dumpDelimitedRecord(toPrint, out);
      }
    }
    
    if (xmlMode)
      out.write("</xtfIndexDocuments>\n");
  } // dumpFields()

  //////////////////////////////////////////////////////////////////////////////
  private static void dumpTermFreqs(IndexReader indexReader,
                                    DocNumMap docNumMap, String[] fields,
                                    Writer out)
    throws IOException 
  {
    TermDocs docs = indexReader.termDocs();

    // Iterate every field.
    for (int i = 0; i < fields.length; i++) 
    {
      // Iterate all the terms for this field.
      TermEnum terms = indexReader.terms(new Term(fields[i], ""));
      while (terms.next()) 
      {
        Term t = terms.term();
        if (!t.field().equals(fields[i]))
          break;

        // Skip bi-grams
        String text = t.text();
        if (text.indexOf("~") >= 0)
          continue;

        // Skip empty terms (there shouldn't be any though) 
        if (text.length() == 0)
          continue;

        // Skip special start/end of field marks (normal terms will also
        // be present, without the marks.) Also skip element and attribute
        // markers.
        //
        char c = text.charAt(0);
        if (c == Constants.FIELD_START_MARKER ||
            c == Constants.ELEMENT_MARKER ||
            c == Constants.ATTRIBUTE_MARKER) 
        {
          continue;
        }

        c = text.charAt(text.length() - 1);
        if (c == Constants.FIELD_END_MARKER ||
            c == Constants.ELEMENT_MARKER ||
            c == Constants.ATTRIBUTE_MARKER) 
        {
          continue;
        }

        // Okay, we have a live one. Accumulate the total occurrences of 
        // the term in all documents. For the benefit of the 'text' field,
        // accumulate chunk counts into the main document.
        //
        int prevMainDoc = -1;
        int docFreq = 0;
        docs.seek(terms);
        int termFreq = 0;
        while (docs.next()) 
        {
          int mainDoc = docs.doc();
          if (t.field().equals("text"))
            mainDoc = docNumMap.getDocNum(docs.doc());
          if (mainDoc != prevMainDoc) {
            ++docFreq;
            prevMainDoc = mainDoc;
          }
          termFreq += docs.freq();
        }

        // Output the results.
        out.write(
          fields[i] + "|" + docFreq + "|" + termFreq + "|" + t.text() + "\n");
      } // while
    } // for i
  } // dumpTermFreqs()

  //////////////////////////////////////////////////////////////////////////////

  /**
   * Removes XTF's special characters (such as bump markers and field start/end
   * markers) from the input string. Also changes characters we use for
   * field and value markers ('|' and ';') to something else so they won't
   * be taken for markers.
   */
  private static String stripValue(String str, boolean changeDelimiters) 
  {
    char[] in = str.toCharArray();
    char[] out = new char[in.length * 2];
    int outLen = 0;

    for (int i = 0; i < in.length; i++) 
    {
      switch (in[i]) {
        case Constants.FIELD_START_MARKER:
        case Constants.FIELD_END_MARKER:
        case Constants.NODE_MARKER:
          break;
        case Constants.BUMP_MARKER:
          i++;
          while (i < in.length && in[i] != Constants.BUMP_MARKER)
            i++;
          if (i < in.length)
            i++;
          out[outLen++] = ';';
          break;
        case ';':
          out[outLen++] = changeDelimiters ? ',' : in[i];
          break;
        case '|':
          out[outLen++] = changeDelimiters ? '.' : in[i];
          break;
        case '\n':
          if (changeDelimiters) {
            out[outLen++] = '\\';
            out[outLen++] = 'n';
          }
          else
            out[outLen++] = in[i];
          break;
        default:
          out[outLen++] = in[i];
          break;
      }
    } // for i

    return new String(out, 0, outLen);
  } // stripValue()
} // class IndexDump
