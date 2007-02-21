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
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.*;
import org.apache.lucene.bigram.BigramStopFilter;
import org.apache.lucene.search.spell.SpellWriter;
import org.cdlib.xtf.textEngine.Constants;
import org.cdlib.xtf.util.CharMap;
import org.cdlib.xtf.util.FastStringReader;
import org.cdlib.xtf.util.FastTokenizer;
import org.cdlib.xtf.util.WordMap;

////////////////////////////////////////////////////////////////////////////////

/** The <code>XTFTextAnalyzer</code> class performs the task of breaking up a
 *  contiguous chunk of text into a list of separate words (<b><i>tokens</i></b>
 *  in Lucene parlance.) The resulting list of words is what Lucene iterates
 *  through and adds to its database for an index. <br><br>
 *
 *  Within this analyzer, there are four main phases:
 *
 *  <blockquote dir=ltr style="MARGIN-RIGHT: 0px">
 *    <u>Tokenizing</u><br>
 *    The first phase is the conversion of the contiguous text into a list of
 *    separate tokens. This step is performed by the {@link FastTokenizer}
 *    class. This class uses a set of rules to separate words in western text
 *    from the  spacing and puntuation that normally accompanies it. The
 *    <code>FastTokenizer</code> class uses the same basic tokenizing rules as
 *    the Lucene <code>StandardAnalyzer</code> class, but has been optimized
 *    for speed. <br><br>
 *
 *    <u>Special Token Filtering</u><br>
 *    In the process of creating chunks of text for indexing, the Text Indexer
 *    program inserts
 *    {@linkplain XMLTextProcessor#insertVirtualWords(StringBuffer) virtual words}
 *    and other special tokens that help it to relate the chunks of text stored
 *    in the Lucene index back to its original XML source text. The
 *    <code>XTFTextAnalyzer</code> looks for those special tokens, removes them
 *    from the token list, and translates them into position increments for the
 *    first non-special tokens they preceed. For more about special token
 *    filtering, see the {@link XtfSpecialTokensFilter} class. <br><br>
 *
 *    <u>Lowercase Conversion</u><br>
 *    The next step performed by the <code>XTFTextAnalyzer</code>
 *    is to convert all the remaining tokens in the token list to lowercase.
 *    Converting indexed words search phrases to lowercase has the effect of
 *    making searches case insensitive. <br><br>
 *
 *    <u>Plural and Accent Folding</u><br>
 *    Next the <code>XTFTextAnalyzer</code> converts plural words to singular
 *    form using a {@link WordMap}, and strips diacritics from the word using
 *    an {@link CharMap}. These conversions can yield more complete search
 *    results. <br><br>
 *
 *    <u>Stop-Word Filtering</u><br>
 *    The next step performed by the <code>XTFTextAnalyzer</code> is to remove
 *    certain words called <i>stop-words</i>. Stop-words are words that by
 *    themselves are not worth indexing, such as <i>a</i>, <i>the</i>,
 *    <i>and</i>, <i>of</i>, etc. These words appear so many times in English
 *    text, that indexing all their occurences just slows down searching for
 *    other words without providing any real value. Consequently, they are
 *    filtered out of the token list. <br><br>
 *
 *    It should be noted, however, that while stop-words are filtered, they are
 *    not simply omitted from the database. This is because stop-words do
 *    impart special meaning when they appear in certain phrases or titles. For
 *    example, in <i>Man <b>of</b> War</i> the word <i>of</i> doesn't simply
 *    act as a conjunction, but rather helps form the common name for a type of
 *    jellyfish. Similarly, the word <i>and</i> in the phrase <i>black
 *    <b>and</b> white</i> doesn't simply join <i>black</i> and <i>white</i>,
 *    but forms a phrase meaning a condition where no ambiguity exists. In
 *    these cases it is important to preserve the stop-words, because ignoring
 *    them would produce undesired matches. For example, in a search for the
 *    words "man of war" (meaning the jellyfish), ignoring stop-words would
 *    produce "man and war", "man in war", and "man against war" as undesired
 *    matches. <br><br>
 *
 *    To record stop-words in special phases without slowing searching, the
 *    <code>XTFTextAnalyzer</code> performs an operation called
 *    <b><i>bi-gramming</i></b> for its third phase of filtering. For more
 *    details about how bi-grams actually work, see the {@link BigramStopFilter}
 *    class. <br><br>
 *
 *    <u>Adding End Tokens</u><br>
 *    As a final step, the analyzer double-indexes the first and last tokens of
 *    fields that contain the special start-of-field and end-of-field characters.
 *    Essentially, those tokens are indexed with and without the markers. This
 *    enables exact matching at query time, since Lucene offers no other way to
 *    determine the end of a field. Note that this processing is only performed
 *    on non-text fields (i.e. meta-data fields.)<br><br>
 *
 *  </blockquote>
 *
 *  Once the <code>XTFTextAnalyzer</code> has completed its work, it returns
 *  the final list of tokens back to Lucene to be added to the index database.
 *  <br><br>
 */
public class XTFTextAnalyzer extends Analyzer 
{
  /** The list of stop-words currently set for this filter. */
  private Set stopSet;

  /** The set of words to change from plural to singular */
  private WordMap pluralMap;

  /** The set of accented chars to remove diacritics from */
  private CharMap accentMap;

  /** A reference to the contiguous source text block to be tokenized and
   *  filtered. (Used by the {@link XTFTextAnalyzer#tokenStream(String,Reader) tokenStream()}
   *  method to read the source text for filter operations in random access
   *  fashion.)
   */
  private String srcText;

  /**
   * List of fields marked as "facets" and thus get special tokenization
   */
  private HashSet facetFields = new HashSet();

  /**
   * List of fields that marked as possibly misspelled, and thus don't get
   * added to the spelling correction dictionary.
   */
  private HashSet misspelledFields = new HashSet();

  /** If building a spelling correction dictionary, this is the writer */
  private SpellWriter spellWriter = null;

  //////////////////////////////////////////////////////////////////////////////

  /** Constructor. <br><br>
   *
   *  This method creates a <code>XTFTextAnalyzer</code> and initializes its
   *  member variables.
   *
   *  @param  stopSet   The set of stop-words to be used when filtering text.
   *                    For more information about stop-words, see the
   *                    {@link XTFTextAnalyzer} class description.
   *
   *  @param  pluralMap The set of plural words to de-pluralize when
   *                    filtering text. See {@link IndexInfo#pluralMapPath}
   *                    for more information.
   *
   *  @param  accentMap The set of accented chars to remove diacritics from
   *                    when filtering text. See
   *                    {@link IndexInfo#accentMapPath} for more information.
   *
   *  @.notes
   *    Use this method to initialize an instance of an
   *    <code>XTFTextAnalyzer</code> and pass it to a Lucene
   *    <code>IndexWriter</code> instance. Lucene will then call the
   *    {@link XTFTextAnalyzer#tokenStream(String,Reader) tokenStream()}
   *    method each time a chunk of text is added to the index. <br><br>
   */
  public XTFTextAnalyzer(Set stopSet, WordMap pluralMap, CharMap accentMap) 
  {
    // Record the set of stop words to use and the combine distance
    this.stopSet = stopSet;

    // Record the plural words to change from plural to singular
    this.pluralMap = pluralMap;

    // Record the chars to remove diacritics from
    this.accentMap = accentMap;
  } // public XTFTextAnalyzer( stopWords, blurbedText )

  /**
   * Clears the list of fields marked as facets. Facet fields receive special
   * tokenization.
   */
  public void clearFacetFields() {
    facetFields.clear();
  }

  /**
   * Mark a field as a "facet field", that will receive special tokenization
   * to deal with hierarchy.
   *
   * @param fieldName   Name of the field to consider a facet field.
   */
  public void addFacetField(String fieldName) {
    facetFields.add(fieldName);
  }

  /**
   * Clears the list of fields marked as misspelled. Misspelled fields are not
   * added to the spelling correction dictionary.
   */
  public void clearMisspelledFields() {
    misspelledFields.clear();
  }

  /**
   * Mark a field as a "misspelled field", that won't be added to the spelling
   * correction dictionary.
   *
   * @param fieldName   Name of the field to consider a misspelled field.
   */
  public void addMisspelledField(String fieldName) {
    misspelledFields.add(fieldName);
  }

  /**
   * Sets a writer to receive tokenized words just before they are indexed.
   * Use this to build a spelling correction dictionary at index time.
   *
   * @param writer    The writer to add words to
   */
  public void setSpellWriter(SpellWriter writer) {
    this.spellWriter = writer;
  }

  //////////////////////////////////////////////////////////////////////////////

  /** Convert a chunk of contiguous text to a list of tokens, ready for
   *  indexing.
   *
   *  @param fieldName  The name of the Lucene database field that the
   *                    resulting tokens will be place in. Used to decide
   *                    which filters need to be applied to the text.
   *
   *  @param reader     A <code>Reader</code> object from which the source
   *                    text
   *
   *  @return
   *    A filtered <code>TokenStream</code> containing the tokens that should
   *    be indexed by the Lucene database. <br><br>
   */
  public TokenStream tokenStream(String fieldName, Reader reader) 
  {
    // Create a fast string reader if we weren't passed one already.
    FastStringReader fastReader;
    if (reader instanceof FastStringReader)
      fastReader = (FastStringReader)reader;
    else
      fastReader = new FastStringReader(reader);

    // Record the text string for later use.
    srcText = fastReader.getString();

    // If this is a facet field, tokenize it specially.
    if (facetFields.contains(fieldName))
      return new FacetTokenizer(srcText);

    // Convert the text into tokens.
    TokenStream result = new FastTokenizer(fastReader);

    // Perform the standard filtering (remove apostrophes, dots from 
    // acronyms, etc.)
    //
    result = new StandardFilter(result);

    // For meta-data fields, mark XML tokens specially.
    if (!fieldName.equals("text"))
      result = new TagFilter(result, srcText);

    // Filter special tokens (word bump, node markers, etc.)
    result = new XtfSpecialTokensFilter(result, srcText);

    // Normalize everything to be lowercase.
    result = new LowerCaseFilter(result);

    // If adding to a spelling dictionary, put an adder in the chain.
    if (spellWriter != null && !misspelledFields.contains(fieldName))
      result = new SpellWritingFilter(result, spellWriter);

    // If a plural map was specified, fold plural and singular words together.
    if (pluralMap != null)
      result = new PluralFoldingFilter(result, pluralMap);

    // If an accent map was specified, fold accented and unaccented chars
    // together.
    //
    if (accentMap != null)
      result = new AccentFoldingFilter(result, accentMap);

    // Convert stop-words to bi-grams (if any stop words were specified). We must
    // do this after XtfSpecialTokensFilter to ensure that special tokens don't
    // become part of any bi-grams. Also, we must do it after the lower-case
    // filter so we can properly recognize the stop words.  
    //
    if (stopSet != null) 
    {
      if (fieldName.equals("text"))
        result = new BigramStopFilter(result, stopSet);
      else 
      {
        // For meta-data fields, include special code to ignore the 
        // field start/end markers during the bi-gramming process.
        //
        result = new BigramStopFilter(result, stopSet) 
        {
          protected boolean isStopWord(String word) {
            if (word.length() > 0 &&
                word.charAt(0) == Constants.FIELD_START_MARKER)
              word = word.substring(1);
            if (word.length() > 0 &&
                word.charAt(word.length() - 1) == Constants.FIELD_END_MARKER)
              word = word.substring(0, word.length() - 1);
            return super.isStopWord(word);
          }
        };
      }
    }

    // Index with and without the special start-of-field/end-of-field markers.
    // If there aren't any, the filter doesn't do any harm. Also, there'll never
    // be any on the 'text' field, so skip that.
    //
    if (!fieldName.equals("text"))
      result = new StartEndFilter(result);

    // Return the final list of tokens to the caller.
    return result;
  } // public tokenStream()
} // class XTFTextAnalyzer  
