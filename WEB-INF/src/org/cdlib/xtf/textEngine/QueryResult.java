package org.cdlib.xtf.textEngine;

import java.io.StringReader;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.Set;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import org.apache.lucene.search.Explanation;
import org.cdlib.xtf.servletBase.TextServlet;
import org.cdlib.xtf.textEngine.facet.ResultFacet;
import org.cdlib.xtf.textEngine.facet.ResultGroup;
import org.cdlib.xtf.util.Attrib;

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

/**
 * Represents the results of a query. This consists of a few statistics,
 * followed by an array of document hit(s).
 *
 * @author Martin Haye
 */
public class QueryResult 
{
  /**
   * Context of the query (including stop word list, and maps for
   * plurals and accents). CrossQuery doesn't use the context, but dynaXML
   * does.
   */
  public QueryContext context;

  /**
   * A set that can be used to check whether a given term is present
   * in the original query that produced this hit. Only applies to the "text"
   * field (i.e. the full text of the document.) CrossQuery doesn't use
   * the text term set, but dynaXML does.
   */
  public Set textTerms;

  /**
   * Total number of documents matched by the query (possibly many more
   * than are returned in this particular request.)
   */
  public int totalDocs;

  /** Ordinal rank of the first document hit returned (0-based) */
  public int startDoc;

  /** Oridinal rank of the last document hit returned, plus 1 */
  public int endDoc;

  /**
   * Whether document scores were normalized so that highest ranking doc
   * has score 100.
   */
  public boolean scoresNormalized;

  /** One hit per document */
  public DocHit[] docHits;

  /** Faceted results grouped by field value (if specified in query) */
  public ResultFacet[] facets;

  /** Spelling suggestions for query terms (if spellcheck specified) */
  public SpellingSuggestion[] suggestions;

  /** Formatter for non-normalized scores */
  private DecimalFormat decFormat;

  /**
   * Makes an XML document out of the list of document hits, and returns a
   * Source object that represents it.
   *
   * @param mainTagName Name of the top-level tag to generate (e.g.
   *                    "crossQueryResult", etc.)
   * @param extraStuff  Additional XML to insert into the query
   *                    result document. Typically includes &lt;parameters>
   *                    block and &lt;query> block.
   * @return            XML Source containing all the hits and snippets.
   */
  public Source hitsToSource(String mainTagName, String extraStuff) {
    String str = hitsToString(mainTagName, extraStuff);
    return new StreamSource(new StringReader(str));
  } // hitsToSource()

  /**
   * Makes an XML document out of the list of document hits, and returns a
   * String object that represents it.
   *
   * @param mainTagName Name of the top-level tag to generate (e.g.
   *                    "crossQueryResult", etc.)
   * @param extraStuff  Additional XML to insert into the query
   *                    result document. Typically includes &lt;parameters>
   *                    block and &lt;query> block.
   * @return            XML string containing all the hits and snippets.
   */
  public String hitsToString(String mainTagName, String extraStuff) 
  {
    StringBuffer buf = new StringBuffer(1000);

    buf.append(
      "<" + mainTagName + " totalDocs=\"" + totalDocs + "\" " + " startDoc=\"" +
      Math.min(startDoc + 1, endDoc) + "\" " + // Note above: 1-based start
      " endDoc=\"" + endDoc + "\">");

    // If extra XML was specified, dump it in here.
    if (extraStuff != null)
      buf.append(extraStuff);

    // If spelling suggestions were made, put them in.
    if (suggestions != null)
      structureSuggestions(buf);

    // Add the top-level doc hits.
    structureDocHits(docHits, startDoc, buf);

    // If faceting was specified, add that info too.
    if (facets != null) 
    {
      // Process each facet in turn
      for (int i = 0; i < facets.length; i++) 
      {
        ResultFacet facet = facets[i];
        buf.append(
          "<facet field=\"" + facet.field + "\" " + "totalGroups=\"" +
          facet.rootGroup.totalSubGroups + "\" " + "totalDocs=\"" +
          facet.rootGroup.totalDocs + "\">");

        // Recursively process all the groups.
        if (facet.rootGroup.subGroups != null) {
          for (int j = 0; j < facet.rootGroup.subGroups.length; j++)
            structureGroup(facet.rootGroup.subGroups[j], buf);
        }
        buf.append("</facet>");
      } // for i
    } // if

    // Add the final tag.
    buf.append("</" + mainTagName + ">\n");

    // Now make the final string.
    return buf.toString();
  } // hitsToString()

  /**
   * Does the work of turning faceted groups into XML.
   *
   * @param group   The group to work on
   * @param buf     Buffer to add XML to
   */
  private void structureGroup(ResultGroup group, StringBuffer buf) 
  {
    // Do the info for the group itself.
    buf.append(
      "<group value=\"" + group.value.replaceAll("\"", "&quot;") + "\" " +
      "rank=\"" + (group.rank + 1) + "\" " + "totalSubGroups=\"" +
      group.totalSubGroups + "\" " + "totalDocs=\"" + group.totalDocs + "\" " +
      "startDoc=\"" + (group.endDoc > 0 ? group.startDoc + 1 : 0) + "\" " +
      "endDoc=\"" + (group.endDoc) + "\">");

    // If the group has any dochits, do them now.
    if (group.docHits != null)
      structureDocHits(group.docHits, group.startDoc, buf);

    // Do all the sub-groups.
    if (group.subGroups != null) {
      for (int i = 0; i < group.subGroups.length; i++)
        structureGroup(group.subGroups[i], buf);
    }

    // All done.
    buf.append("</group>");
  } // structureGroup

  /**
   * Does the work of turning DocHits into XML.
   *
   * @param docHits Array of DocHits to structure
   * @param buf     Buffer to add the XML to
   */
  private void structureDocHits(DocHit[] docHits, int startDoc, StringBuffer buf) 
  {
    if (docHits == null)
      return;

    for (int i = 0; i < docHits.length; i++) 
    {
      DocHit docHit = docHits[i];

      String scoreStr;
      if (scoresNormalized)
        scoreStr = Integer.toString(Math.round(docHit.score * 100));
      else {
        if (decFormat == null)
          decFormat = (DecimalFormat)DecimalFormat.getInstance();
        scoreStr = decFormat.format(docHit.score);
      }

      buf.append(
        "<docHit" + " rank=\"" + (i + startDoc + 1) + "\"" + " path=\"" +
        docHit.filePath() + "\"" + " score=\"" + scoreStr + "\"" +
        " totalHits=\"" + docHit.totalSnippets() + "\"");
      if (docHit.recordNum() > 0)
        buf.append(" recordNum=\"" + docHit.recordNum() + "\"");
      buf.append(">\n");

      Explanation explanation = docHit.explanation();
      if (explanation != null)
        structureExplanation(explanation, buf);

      if (!docHit.metaData().isEmpty()) 
      {
        buf.append("<meta>\n");
        for (Iterator atts = docHit.metaData().iterator(); atts.hasNext();) {
          Attrib attrib = (Attrib)atts.next();
          buf.append(attrib.value);
        } // for atts
        buf.append("</meta>\n");
      }

      for (int j = 0; j < docHit.nSnippets(); j++) 
      {
        Snippet snippet = docHit.snippet(j, true);
        buf.append(
          "<snippet rank=\"" + (j + 1) + "\" score=\"" +
          Math.round(snippet.score * 100) + "\"");

        if (snippet.sectionType != null)
          buf.append(" sectionType=\"" + snippet.sectionType + "\"");

        buf.append(
          ">" + TextServlet.makeHtmlString(snippet.text, true) +
          "</snippet>\n");
      } // for j

      buf.append("</docHit>\n");
    } // for i
  } // structureDocHits()

  /**
   * Does the work of turning a score explanation into XML.
   */
  private void structureExplanation(Explanation exp, StringBuffer buf) 
  {
    buf.append("<explanation value=\"");
    buf.append(exp.getValue());
    buf.append("\" description=\"");
    buf.append(exp.getDescription());
    buf.append("\">\n");

    Explanation[] subs = exp.getDetails();
    if (subs != null) {
      for (int i = 0; i < subs.length; i++)
        structureExplanation(subs[i], buf);
    }

    buf.append("</explanation>\n");
  } // structureExplanation

  /**
   * Does the work of translating spelling suggestions into XML.
   */
  private void structureSuggestions(StringBuffer buf) 
  {
    buf.append("<spelling>\n");

    for (int i = 0; i < suggestions.length; i++) 
    {
      SpellingSuggestion sugg = suggestions[i];
      StringBuffer fieldsBuf = new StringBuffer();
      for (int j = 0; j < sugg.fields.length; j++) {
        if (fieldsBuf.length() > 0)
          fieldsBuf.append(",");
        fieldsBuf.append(sugg.fields[j]);
      }
      buf.append(
        "  <suggestion" + " originalTerm=\"" + sugg.origTerm + "\"" +
        " fields=\"" + fieldsBuf + "\"" + " suggestedTerm=\"" +
        (sugg.suggestedTerm != null ? sugg.suggestedTerm : "") + "\"" + "/>\n");
    }

    buf.append("</spelling>\n");
  } // structureSuggestions()
} // class QueryResult
