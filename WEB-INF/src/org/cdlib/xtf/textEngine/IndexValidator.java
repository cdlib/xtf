package org.cdlib.xtf.textEngine;

/**
 * Copyright (c) 2009, Regents of the University of California
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

import javax.servlet.ServletException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.cdlib.xtf.crossQuery.test.TestableCrossQuery;
import org.cdlib.xtf.dynaXML.test.TestableDynaXML;
import org.cdlib.xtf.util.EasyNode;
import org.cdlib.xtf.util.Trace;

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

/**
 * This class performs the validation steps for a specified index, checking that
 * the results are acceptable. This is used at index time to decide whether
 * to rotate in a new index, and also by the servlets to "warm up" a new index
 * before presenting it to the user.
 * 
 * @author Martin Haye
 */
public class IndexValidator
{
  private TestableCrossQuery crossQuery;
  private TestableDynaXML dynaXML;
  private int nErrs;
  
  /**
   * Run validations for the given index.
   * 
   * @param baseDir      XTF home directory
   * @param indexPath    path to the index data
   * @param indexReader  Lucene reader for the index
   * @return             true iff all validations passed
   * @throws IOException if the index can't be read
   */
  public boolean validate(String baseDir, String indexPath, IndexReader indexReader) 
    throws IOException
  {
    // Create the servlets we'll use for testing.
    try {
      crossQuery = new TestableCrossQuery(baseDir);
      crossQuery.overrideIndexDir(indexPath);
      
      dynaXML = new TestableDynaXML(baseDir);
      dynaXML.overrideIndexDir(indexPath);
    } catch (ServletException e) {
      throw new IOException(e.getMessage());
    }
    
    // Fetch the index information chunk.
    Hits match = new IndexSearcher(indexReader).search(new TermQuery(new Term("indexInfo", "1")));
    if (match.length() == 0)
      throw new IOException("Index missing indexInfo doc");
    assert match.id(0) == 0 : "indexInfo chunk must be first in index";
    Document doc = match.doc(0);

    // If no validation is specified, we're done.
    String validationName = doc.get("validation");
    if (validationName == null || validationName.length() == 0)
      return true;

    // Let the user know we're validating now.
    Trace.info(String.format("Validating Index:"));
    Trace.tab();
    
    try 
    {
      // Read the validation file.
      File validationFile = new File(indexPath, validationName);
      EasyNode root = EasyNode.readXMLFile(validationFile);
      
      // Run the validation.
      nErrs = 0;
      traverse(root, 0);
      
      // If any errors, abort.
      if (nErrs > 0) {
        Trace.untab();
        Trace.error(String.format("Validation failed: %d error(s)", nErrs));
        return false;
      }
        
      // All checks passed... the index is golden.
      Trace.untab();
      Trace.info("Done.");
      return true;
    }
    catch (NumberFormatException err) {
      Trace.untab();
      Trace.error("Validation failed: non-numeric attribute found in validation specification");
      return false;
    } 
    catch (Exception err) {
      Trace.untab();
      Trace.error("Validation failed: " + err.getMessage());
      return false;
    }
  }

  /**
   * Traverse the validation specification document, visiting each node.
   */
  private void traverse(EasyNode node, int level) 
    throws ValidationError, ServletException, IOException 
  {
    visit(node, level);
    for (EasyNode child : node.children())
      traverse(child, level+1);
  }

  /**
   * Process one node of the validation specification document.
   */
  private void visit(EasyNode node, int level) 
    throws ValidationError, ServletException, IOException
  {
    // Forget the outer doc wrapper
    if (level == 0)
      return;
    
    // Check the root level
    if (level == 1) {
      String lookFor = "index-validation";
      if (!node.name().equals(lookFor))
        throw new ValidationError("Root element of validation file must be '<index-validation>'");
      return;
    }
    
    // The meat is at the second level, so there shouldn't be things at any other level.
    else if (level > 2) {
      if (!node.isText())
        throw new ValidationError("Element '%s' not recognized at level %s", node.name(), level);
    }
    
    // Validate the attributes.
    int minHits = 0;
    for (String attrName : node.attrNames())
    {
      if (attrName.equals("minHits"))
        minHits = Integer.parseInt(node.attrValue(attrName));
      else
        throw new ValidationError("Attribute '%s' not recognized on '%s' element", attrName, node.name());
    }
    
    // And perform the validation (use the appropriate servlet).
    String url = node.toString();
    int nHits = 0;
    int prevTraceLevel = Trace.getOutputLevel();
    if (node.name().equals("crossQuery")) { 
      Trace.info("crossQuery: [%s] ...", url);
      Trace.setOutputLevel(Trace.warnings);
      crossQuery.service(url);
      nHits = crossQuery.nHits();
    }
    else if (node.name().equals("dynaXML")) {
      Trace.info("dynaXML:    [%s] ...", url);
      Trace.setOutputLevel(Trace.warnings);
      dynaXML.service(url);
      nHits = dynaXML.nHits();
    }
    else if (node.isText())
      return;
    Trace.setOutputLevel(prevTraceLevel);
    
    if (minHits != 0 && nHits < minHits) {
      Trace.more(Trace.info, " Failed:");
      Trace.error("            Validation required at least %d hits, but query returned %d", minHits, nHits);
      ++nErrs;
    }
    else
      Trace.more(Trace.info, " Done.");
  }

  /** Internal exception for quickly passing errors up the call chain. */
  public static class ValidationError extends Exception {
    ValidationError(String msg, Object ... args) {
      super(String.format(msg, args));
    }
  }
}