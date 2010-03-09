package org.cdlib.xtf.textEngine;


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
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.FieldSpanSource;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.spans.FieldSpans;
import org.cdlib.xtf.util.AttribList;

/**
 * Represents a query hit at the document level. May contain {@link Snippet}s
 * if those were requested.
 *
 * @author Martin Haye
 */
public class DocHitImpl extends DocHit 
{
  /** Used to load and format snippets */
  private SnippetMaker snippetMaker;

  /** Source of spans. Only valid during collection. */
  private FieldSpanSource fieldSpanSource;
  
  /** Spans per field */
  private FieldSpans fieldSpans;

  /** Array of pre-built snippets */
  private Snippet[] snippets;

  /** Index key for this document */
  private String docKey;

  /** Date the original source XML document was last modified */
  @SuppressWarnings("unused")
  private long fileDate = -1;

  /** Record number of this document within the main file */
  private int recordNum = 0;
  
  /** Record the subdocument within the main file, if any */
  private String subDocument = null;

  /** Total number of chunks for this document */
  private int chunkCount = -1;

  /** Document's meta-data fields (copied from the docInfo chunk) */
  private AttribList metaData;

  /** Explanation of this document's score */
  private Explanation explanation;

  /**
   * Construct a document hit. Package-private because these should only
   * be constructed inside the text engine.
   *
   * @param docNum    Lucene ID for the document info chunk
   * @param score     Score for this hit
   */
  DocHitImpl(int docNum, float score) {
    super(docNum, score);
  }

  /**
   * Sets the source for spans (to perform deduplication)
   */
  void setSpanSource(FieldSpanSource src) {
    this.fieldSpanSource = src;
  }

  /**
   * Called after all hits have been gathered to normalize the scores and
   * associate a snippetMaker for later use.
   *
   * @param snippetMaker    Will be used later by snippet() to actually
   *                        create the snippets.
   * @param docScoreNorm    Multiplied into the document's score
   */
  void finish(SnippetMaker snippetMaker, float docScoreNorm) 
  {
    // Don't do this twice.
    if (this.snippetMaker != null)
      return;

    // Record the snippet maker... we'll use it later if loading is
    // necessary.
    //
    this.snippetMaker = snippetMaker;

    // Adjust our score.
    score *= docScoreNorm;
  } // finish()

  /**
   * Called after all hits have been gathered to normalize the scores and
   * associate a snippetMaker for later use. Also calculates an explanation
   * of the score.
   *
   * @param snippetMaker    Will be used later by snippet() to actually
   *                        create the snippets.
   * @param docScoreNorm    Multiplied into the document's score
   * @param weight          The query weight that will be used to calculate
   *                        an explanation.
   * @param boostSet        The boost set used, or null if none
   * @param boostParams     Other boost set parameters (e.g. exponent)
   */
  void finishWithExplain(SnippetMaker snippetMaker, float docScoreNorm,
                         Weight weight, BoostSet boostSet,
                         BoostSetParams boostParams)
    throws IOException 
  {
    // Don't do this twice.
    if (this.snippetMaker != null)
      return;

    // Do the normal work first.
    finish(snippetMaker, docScoreNorm);

    // And figure out an explanation.
    explanation = weight.explain(snippetMaker.reader, doc);

    // Add any boost set stuff if necessary.
    if (boostSet != null) 
    {
      Explanation result = new Explanation(0, "boosted, product of:");

      Explanation boostExpl = new Explanation(boostSet.getBoost(
                                                                doc,
                                                                boostParams.defaultBoost),
                                              "boostSetFactor");
      if (boostParams.exponent != 1.0f) {
        Explanation exponentExpl = new Explanation(
          (float)Math.pow(boostExpl.getValue(), boostParams.exponent),
          "exponentBoosted");
        exponentExpl.addDetail(boostExpl);
        exponentExpl.addDetail(new Explanation(boostParams.exponent,
                                               "boostSetExponent"));
        boostExpl = exponentExpl;
      }

      result.addDetail(boostExpl);
      result.addDetail(explanation);
      result.setValue(boostExpl.getValue() * explanation.getValue());

      explanation = result;
    }
  } // finishWithExplain()

  /**
   * Read in the document info chunk and record the path, date, etc. that
   * we find there.
   */
  private void load() 
  {
    // Read in our fields
    Document docContents;
    try {
      assert !snippetMaker.reader.isDeleted(doc);
      docContents = snippetMaker.reader.document(doc);
    }
    catch (IOException e) {
      throw new HitLoadException(e);
    }

    // Record the ones of interest.
    metaData = new AttribList();
    for (Field f : (List<Field>)docContents.getFields()) 
    {
      String name = f.name();
      String value = f.stringValue();

      if (name.equals("key"))
        docKey = value;
      else if (name.equals("fileDate")) {
        try {
          fileDate = DateTools.stringToTime(value);
        }
        catch (java.text.ParseException e1) {
        }
      }
      else if (name.equals("chunkCount"))
        chunkCount = Integer.parseInt(value);
      else if (name.equals("recordNum"))
        recordNum = Integer.parseInt(value);
      else if (name.equals("subDocument"))
        subDocument = value;
      else if (!name.equals("docInfo")) 
      {
        // Note: We cannot use f.isTokenized() below, because in the case of
        //       facet values we tokenize in Lucene-land but in XTF land
        //       consider them to be un-tokenized. Hence the use of
        //       snippetMaker.tokFields() instead.
        //
        loadMetaField(name, value, docContents, metaData, 
                      snippetMaker.tokFields().contains(f.name()));
      }
    }

    // We should have gotten at least the special fields.
    assert docKey != null : "Incomplete data in index - missing 'key'";
    assert chunkCount != -1 : "Incomplete data in index - missing 'chunkCount'";
  } // load()

  /**
   * Performs all the manipulations and marking for a meta-data field.
   *
   * @param name      Name of the field
   * @param value     Raw string value of the field
   * @param docContents   Where to get spans from
   * @param metaData  Where to put the resulting data
   * @param isTokenized true if the field was tokenized and should be
   *                    marked.
   */
  private void loadMetaField(String name, String value, Document docContents,
                             AttribList metaData, boolean isTokenized) 
  {
    // First, mark up the value.
    String markedValue;
    if (isTokenized)
      markedValue = snippetMaker.markField(docContents, fieldSpans, name, value);
    else
      markedValue = value;

    // Now fix up the result. This involves three transformations:
    // (1) Strip the special start-of-field and end-of-field tokens; and
    // (2) Insert proper <element>...</element> tags if they were left out
    //     to save index space.
    // (3) Lucene will fail subtly if we add two fields with the same 
    //     name. Basically, the terms for each field are added at 
    //     overlapping positions, causing a phrase search to easily 
    //     span them. To counter this, the text indexer artificially 
    //     introduces bump markers between them. And now, we reverse 
    //     the process so it's invisible to the end-user.
    //
    // Note: the previous version of this code did not handle XML elements
    //       as the only content of a metadata field, and did not handle
    //       XML in multiple valued fields.
    //
    StringBuilder buf = new StringBuilder(markedValue.length() * 2);
    char[] chars = markedValue.toCharArray();
    for (int i = 0; i < chars.length; i++) 
    {
      char c = chars[i];
      
      // Insert element start tag. There will be a placeholder if there were
      // attributes, otherwise we have to fill in the whole thing.
      //
      if (c == '<' && i < markedValue.length()-2 && markedValue.charAt(i+1) == '$') {
        buf.append('<');
        buf.append(name);
        i++; // skip $
        continue;
      }
      else if (buf.length() == 0) {
        buf.append('<');
        buf.append(name);
        buf.append(">");
      }
      
      // Copy normal characters.
      if (c != Constants.FIELD_START_MARKER &&
          c != Constants.FIELD_END_MARKER &&
          c != Constants.BUMP_MARKER)
      {
        buf.append(c);
      }
      
      // At end of field (or subfield), insert element end tag and write the metadata.
      if (i == chars.length-1 || c == Constants.BUMP_MARKER) {
        buf.append("</");
        buf.append(name);
        buf.append('>');
        String cleanedVal = buf.toString();
        metaData.put(name, cleanedVal);
        buf.delete(0, buf.length());
        
        // Bump markers come in pairs; skip past the second one.
        if (c == Constants.BUMP_MARKER) {
          i++;
          while (i < chars.length && chars[i] != Constants.BUMP_MARKER)
            i++;
          i++;
          // Eat extra space after the bump marker
          if (i < chars.length && Character.isWhitespace(chars[i]))
            i++;
          i--; // because loop above will increment it
        }
      }
    } // for i
  } // loadMetaField()

  /**
   * Fetch a map that can be used to check whether a given term is present
   * in the original query that produced this hit.
   */
  public Set textTerms() {
    if (fieldSpans == null) {
      if (fieldSpanSource != null)
        fieldSpans = fieldSpanSource.getSpans(doc);
      else
        return null;
    }
    return fieldSpans.getTerms("text");
  }

  /**
   * Retrieve the original file path as recorded in the index (if any.)
   */
  public final String filePath() {
    if (docKey == null)
      load();
    return docKey;
  } // filePath()

  /**
   * Retrieve the record number of this document within the main file, or
   * zero if this is the only record.
   */
  public final int recordNum() {
    if (docKey == null)
      load();
    return recordNum;
  } // filePath()
  
  /**
   * Retrieve the subdocument name of this section within the main
   * file, if any.
   */
  public final String subDocument() {
    if (docKey == null)
      load();
    return subDocument;
  }

  /**
   * Retrieve a list of all meta-data name/value pairs associated with this
   * document.
   */
  public final AttribList metaData() {
    if (docKey == null)
      load();
    return metaData;
  }

  /** Return the total number of snippets found for this document (not the
   *  number actually returned, which is limited by the max # of snippets
   *  specified in the query.)
   */
  public final int totalSnippets() {
    if (fieldSpans == null) {
      if (fieldSpanSource != null)
        fieldSpans = fieldSpanSource.getSpans(doc);
      else
        return 0;
    }
    return fieldSpans.getSpanTotal("text");
  }

  /**
   * Return the number of snippets available (limited by the max # specified
   * in the original query.)
   */
  public final int nSnippets() {
    if (fieldSpans == null) {
      if (fieldSpanSource != null)
        fieldSpans = fieldSpanSource.getSpans(doc);
      else
        return 0;
    }
    return fieldSpans.getSpanCount("text");
  }

  /**
   * Retrieve the specified snippet.
   *
   * @param hitNum    0..nSnippets()
   * @param getText   true to fetch the snippet text in context, false to
   *                  only fetch the rank, score, etc.
   */
  public final Snippet snippet(int hitNum, boolean getText) 
  {
    // If we haven't built the snippets yet (or if we didn't get the
    // text for them), do so now.
    //
    if (snippets == null || (getText && snippets[hitNum].text == null))
      snippets = snippetMaker.makeSnippets(fieldSpans, doc, "text", getText);

    // Return the pre-built snippet.
    return snippets[hitNum];
  } // snippet()

  /** Retrieve an explanation of this document's score */
  public Explanation explanation() {
    return explanation;
  }
} // class DocHit
