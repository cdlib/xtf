package org.cdlib.xtf.textIndexer;

import java.util.LinkedList;

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

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

/**
 * This class maintains information about the current section in a text
 * document that the TextIndexer program is processing. <br><br>
 *
 * On-line documents are stored as "nodes" in XML files that contain information
 * about the document, and the document text itself. The nodes usually form
 * a heirarchical tree structure, with the outer-most nodes recording
 * various bits of information about the text within. Inside the outer nodes
 * are additional nodes that record the organization of the text itself,
 * including things like section, chapter, and paragraph information. To the
 * text indexer program and search engine, sections have special significance.
 * Text in two adjacent sections that have different names, are considered
 * to not be "near" one another, so that proximity searches will not produce
 * results that span across two or more sections. <br><br>
 *
 * Since sections can be nested inside one-another, a stack of the current
 * nesting level needs to be maintained by the text indexer when a document
 * is being processed. Doing so does two things: <br><br>
 *
 * - It allows unnamed inner sections to inherit properties from the parent
 *   sections that contain them. <br>
 * - When the end of an named section has been reached, the text indexer can
 *   return to using the parent section's properties and continue processing.
 *   <br><br>
 *
 * Information recorded for each section consists of the following: <br><br>
 *
 * - The type name of the current section. <br>
 * - The repeat depth, if the section name is the same as the parent's. <br>
 * - The number of words that this section should offset from the previous one.
 *   <br>
 * - The previous word bump for this section, if any. <br>
 * - The word bump to apply at the end of each sentence. <br>
 * - The relevance boost to apply to words in this section. <br><br>
 *
 * This class is then used as the entry for a
 * {@link org.cdlib.xtf.textIndexer.SectionInfoStack }
 * that maintains the current stacking order within the source text being
 * processed. <br><br>
 *
 */
public class SectionInfo 
{
  /** Index/No-Index Flag Value: Use parent section index/no-index state.
   *  <br><br>
   *
   *  @.notes
   *  This index flag value is never actually stored in the index flag attribute
   *  for a <code>SectionInfo</code> instance. It is only passed as an argument
   *  to the
   *  explicit section push
   *  method defined by the {@link org.cdlib.xtf.textIndexer.SectionInfoStack}
   *  class. That method in turn uses the parent section's index flag value,
   *  which will be either
   *  {@link org.cdlib.xtf.textIndexer.SectionInfo#index index} or
   *  {@link org.cdlib.xtf.textIndexer.SectionInfo#noIndex noIndex}.
   *  <br><br>
   */
  public final static int parentIndex = -1;

  /** Index/No-Index Flag Value: Index the current section.
   *  <br><br>
   *
   *  This value is used for the
   *  {@link org.cdlib.xtf.textIndexer.SectionInfo#indexFlag indexFlag} field
   *  to indicate that the current section should not be indexed.
   *  <br><br>
   */
  public final static int noIndex = 0;

  /** Index/No-Index Flag Value: Index the current section.
   *  <br><br>
   *
   *  This value is used for the
   *  {@link org.cdlib.xtf.textIndexer.SectionInfo#indexFlag indexFlag} field
   *  to indicate that the current section should be indexed.
   *  <br><br>
   */
  public final static int index = 1;

  /** Special Section Bump: Value = Use parent's section bump.
   *  <br><br>
   *
   *  This special value when used for the
   *  {@link org.cdlib.xtf.textIndexer.SectionInfo#sectionBump sectionBump}
   *  field indicates that the parent's section bump value should be used.
   *  <br><br>
   *
   *  @.notes
   *  This section bump value is never actually stored in the section bump
   *  attribute for a <code>SectionInfo</code> instance. It is only passed as
   *  an argument to the
   *  explicit section push
   *  method defined by the {@link org.cdlib.xtf.textIndexer.SectionInfoStack}
   *  class. That method in turn uses the parent section's bump value for the
   *  new entry on the stack.<br><br>
   */
  public final static int parentSectionBump = -1;

  /** Default state for Index/No-Index Flag. Value = index.
   *  <br><br>
   *
   *  This is the default value applied to the
   *  {@link org.cdlib.xtf.textIndexer.SectionInfo#indexFlag indexFlag}
   *  field whenever a <code>SectionInfo</code> class is constructed.
   *  <br><br>
   */
  public final static int defaultIndexFlag = index;

  /** Default section type name: Value = {@value}.
  *  <br><br>
  *
  *  This is the default value applied to the
  *  {@link org.cdlib.xtf.textIndexer.SectionInfo#sectionType sectionType}
  *  field whenever a <code>SectionInfo</code> class is constructed.
  *  <br><br>
  */
  public final static String defaultSectionType = "";
  
  /** Default subdocument: Value = null.
  *  <br><br>
  *
  *  This is the default value applied to the
  *  {@link org.cdlib.xtf.textIndexer.SectionInfo#subDocument subDocument}
  *  field whenever a <code>SectionInfo</code> class is constructed.
  *  <br><br>
  */
  public final static String defaultSubDocument = null;

  /** Default word bump for a section: Value = {@value}.
   *  <br><br>
   *
   *  This is the default value applied to the
   *  {@link org.cdlib.xtf.textIndexer.SectionInfo#sectionBump sectionBump}
   *  field whenever a <code>SectionInfo</code> class is constructed. <br><br>
   */
  public final static int defaultSectionBump = 0;

  /** Default word boost for a section: Value = {@value}.
   *  <br><br>
   *
   *  This is the default value applied to the
   *  {@link org.cdlib.xtf.textIndexer.SectionInfo#wordBoost wordBoost}
   *  field whenever a <code>SectionInfo</code> class is constructed.
   *  <br><br>
   */
  public final static float defaultWordBoost = 1;

  /** Default sentence bump for a section: Value = {@value}.
   *  <br><br>
   *
   *  This is the default value applied to the
   *  {@link org.cdlib.xtf.textIndexer.SectionInfo#sentenceBump sentenceBump}
   *  field whenever a <code>SectionInfo</code> class is constructed.
   *  <br><br>
   */
  public final static int defaultSentenceBump = 5;

  /** Default depth for a section: Value = {@value}.
   *  <br><br>
   *
   *  This is the default value applied to the
   *  {@link org.cdlib.xtf.textIndexer.SectionInfo#depth depth}
   *  field whenever a <code>SectionInfo</code> class is constructed.
   *  <br><br>
   */
  public final static int defaultDepth = 0;

  /** Spell/No-Spell Flag Value: Use parent section spell/no-spell state.
   *  <br><br>
   *
   *  @.notes
   *  This spell flag value is never actually stored in the spell flag attribute
   *  for a <code>SectionInfo</code> instance. It is only passed as an argument
   *  to the
   *  explicit section push
   *  method defined by the {@link org.cdlib.xtf.textIndexer.SectionInfoStack}
   *  class. That method in turn uses the parent section's spell flag value,
   *  which will be either
   *  {@link org.cdlib.xtf.textIndexer.SectionInfo#spell spell} or
   *  {@link org.cdlib.xtf.textIndexer.SectionInfo#noSpell noSpell}.
   *  <br><br>
   */
  public final static int parentSpell = -1;

  /** No-Spell Flag Value: Do not add words from the current section to the
   *  spelling correction dictionary.
   *  <br><br>
   *
   *  This value is used for the
   *  {@link org.cdlib.xtf.textIndexer.SectionInfo#spellFlag spellFlag} field
   *  to indicate that words from the current section should not be added to
   *  the spelling correction dictionary.
   *  <br><br>
   */
  public final static int noSpell = 0;

  /** Spell Flag Value: Add words from the current section to the
   *  spelling correction dictionary.
   *  <br><br>
   *
   *  This value is used for the
   *  {@link org.cdlib.xtf.textIndexer.SectionInfo#spellFlag spellFlag} field
   *  to indicate that words from the current section should be added to the
   *  spelling correction dictionary.
   *  <br><br>
   */
  public final static int spell = 1;

  /** Default state for Spell/No-Spell Flag. Value = spell.
   *  <br><br>
   *
   *  This is the default value applied to the
   *  {@link org.cdlib.xtf.textIndexer.SectionInfo#spellFlag spellFlag}
   *  field whenever a <code>SectionInfo</code> class is constructed.
   *  <br><br>
   */
  public final static int defaultSpellFlag = spell;

  /** Depth count for a section. <br><br>
   *
   *  This field is used to count the depth of a section when more than one
   *  section with the same attributes nests inside another. Using a depth
   *  count saves having to add an entire duplicate entry to the stack. <br><br>
   */
  public int depth;

  /** Index flag for a section. <br><br>
   *
   *  This field indicates whether the associated section should be indexed
   *  or not. There are three valid values for this flag:
   *  {@link org.cdlib.xtf.textIndexer.SectionInfo#parentIndex parentIndex},
   *  {@link org.cdlib.xtf.textIndexer.SectionInfo#noIndex noIndex},
   *  and {@link org.cdlib.xtf.textIndexer.SectionInfo#index index}.
   *
   *  @.notes
   *  The value {@link org.cdlib.xtf.textIndexer.SectionInfo#parentIndex parentIndex}
   *  is never actually stored in the index flag attribute for a
   *  <code>SectionInfo</code> instance. It is only passed as an argument to the
   *  explicit section push
   *  method defined by the {@link org.cdlib.xtf.textIndexer.SectionInfoStack}
   *  class. That method in turn uses the parent section's index flag value,
   *  which will be either
   *  {@link org.cdlib.xtf.textIndexer.SectionInfo#index index} or
   *  {@link org.cdlib.xtf.textIndexer.SectionInfo#index noIndex}.
   *  <br><br>
   */
  public int indexFlag;

  /** Type name for a section. <br><br>
   *
   *  This field indentifies the name of the associated section. This field
   *  can be an empty string (""), in which case the parent section name (if
   *  any) is inherited, or an arbitrary string. <br><br>
   */
  public String sectionType;

  /** Word bump to add for a section. <br><br>
   *
   *  This field specifies how far in words a section is from the previous or
   *  containing section, and is used to adjust the likelyhood of a proximity
   *  match being found across section boundaries as compared to within a
   *  single section. <br><br>
   */
  public int sectionBump;

  /** Previous section bump for this section. <br><br>
   *
   *  This field is used correctly accumulate section bump values when multiple
   *  nested sections starts are encountered with no intervening text.
   *
   *  @.notes
   *  The value {@link org.cdlib.xtf.textIndexer.SectionInfo#parentSectionBump parentSectionBump}
   *  is never actually stored in the sectionBump attribute for a
   *  <code>SectionInfo</code> instance. It is only passed as an argument to the
   *  explicit section push
   *  method defined by the {@link org.cdlib.xtf.textIndexer.SectionInfoStack}
   *  class. That method in turn uses the parent section's bump value. <br><br>
   */
  public int prevSectionBump;

  /** Word boost value for this section. <br><br>
   *
   *  This field is identifies a relevance multiplier for words found in this
   *  section. If greater than 1.0, words in this section are considered better
   *  matches for searches when added to the index. If less than 1.0, words in
   *  this section are considered poorer matches.
   */
  public float wordBoost;

  /** Sentence bump value for this section. <br><br>
   *
   *  This field is identifies the distance (in number of words) that occurs
   *  between the end of one sentence and the beginning of the next. This value
   *  is used to adjust the likelyhood that a proximity match is found across
   *  multiple sentences as compared to within a single sentence. <br><br>
   *.
   */
  public int sentenceBump;

  /** Spell flag for a section. <br><br>
   *
   *  This field indicates whether words from the associated section should be
   *  added to the spelling correction dictionary or not.
   *  There are three valid values for this flag:
   *  {@link org.cdlib.xtf.textIndexer.SectionInfo#parentSpell parentSpell},
   *  {@link org.cdlib.xtf.textIndexer.SectionInfo#noSpell noSpell},
   *  and {@link org.cdlib.xtf.textIndexer.SectionInfo#spell spell}.
   *
   *  @.notes
   *  The value {@link org.cdlib.xtf.textIndexer.SectionInfo#parentSpell parentSpell}
   *  is never actually stored in the spell flag attribute for a
   *  <code>SectionInfo</code> instance. It is only passed as an argument to the
   *  explicit section push
   *  method defined by the {@link org.cdlib.xtf.textIndexer.SectionInfoStack}
   *  class. That method in turn uses the parent section's spell flag value,
   *  which will be either
   *  {@link org.cdlib.xtf.textIndexer.SectionInfo#spell spell} or
   *  {@link org.cdlib.xtf.textIndexer.SectionInfo#spell noSpell}.
   *  <br><br>
   */
  public int spellFlag;
  
  /** Name for a subdocument. <br><br>
   * 
   * This field indicates a section of the document that should be treated
   * as an individual searchable unit, but should be viewed in the context
   * of its containing document. If null, the section is simply considered
   * part of the document with no subdocument distinction.
   */
  public String subDocument;
  
  /** Meta-data collection list for a subdocument. <br><br>
   * 
   * This field contains a list of meta-data that will be added to when
   * xtf:meta attributes are encountered while indexing the current 
   * subdocument. Since only a subdocument can have unique meta-data, this 
   * attribute should only be pushed when a new subdocument is begun.
   */
  public LinkedList metaInfo;

  //////////////////////////////////////////////////////////////////////////////

  /** Default Constructor. <br><br>
   *
   *  Initializes all the fields in a <code>SectionInfo</code> instance to
   *  reasonable default values. <br><br>
   *
   *  @.notes
   *  See the {@link org.cdlib.xtf.textIndexer.SectionInfo#defaultDepth},
   *  {@link org.cdlib.xtf.textIndexer.SectionInfo#defaultIndexFlag},
   *  {@link org.cdlib.xtf.textIndexer.SectionInfo#defaultSectionType},
   *  {@link org.cdlib.xtf.textIndexer.SectionInfo#defaultSectionBump},
   *  {@link org.cdlib.xtf.textIndexer.SectionInfo#defaultWordBoost},
   *  and
   *  {@link org.cdlib.xtf.textIndexer.SectionInfo#defaultSentenceBump}
   *  constants for more on the actual values set. <br><br>
   */
  public SectionInfo() 
  {
    // Set up some initial defaults.
    this.depth = defaultDepth;
    this.indexFlag = defaultIndexFlag;
    this.sectionType = defaultSectionType;
    this.sectionBump = defaultSectionBump;
    this.prevSectionBump = 0;
    this.wordBoost = defaultWordBoost;
    this.sentenceBump = defaultSentenceBump;
    this.spellFlag = defaultSpellFlag;
    this.subDocument = null;
    this.metaInfo = null;
  } // SectionInfo()

  //////////////////////////////////////////////////////////////////////////////

  /** Explicit Constructor. <br><br>
   *
   *  Initializes all the fields in a <code>SectionInfo</code> instance to
   *  values passed by the caller. <br><br>
   */
  public SectionInfo(int depth, int indexFlag, String sectionType,
                     int sectionBump, float wordBoost, int sentenceBump,
                     int spellFlag, String subDocument, LinkedList metaInfo) 
  {
    this.depth = depth;
    this.indexFlag = indexFlag;
    this.sectionType = sectionType;
    this.prevSectionBump = 0;
    this.sectionBump = sectionBump;
    this.wordBoost = wordBoost;
    this.sentenceBump = sentenceBump;
    this.spellFlag = spellFlag;
    this.subDocument = subDocument;
    assert subDocument == null || subDocument.length() > 0;
    this.metaInfo = metaInfo;
  } // sectionBump()

  //////////////////////////////////////////////////////////////////////////////

  /** Saves the section bump value for later restore.<br><br>
   *
   *  This method is used to save the specific bump value assigned to a section
   *  when accumulating nested section bumps with no intervening text.<br><br>
   *
   *  @return    The previous section bump value saved.<br><br>
   *
   *  @.notes
   *     Once saved, the
   *     {@link org.cdlib.xtf.textIndexer.SectionInfo#sectionBump sectionBump}
   *     field is reset to zero in anticipation of accumulating bump values
   *     from previous sections. <br><br>
   */
  public int saveSectionBump() {
    prevSectionBump = sectionBump;
    sectionBump = 0;
    return prevSectionBump;
  }

  //////////////////////////////////////////////////////////////////////////////

  /** Restore a previously saved section bump value.<br><br>
   *
   *  This method is a convenience method for restoring the section bump value
   *  previously saved via
   *  {@link org.cdlib.xtf.textIndexer.SectionInfo#saveSectionBump() saveSectionBump()}.
   *  <br><br>
   */
  public void restoreSectionBump() {
    sectionBump = prevSectionBump;
  }
} // class SectionInfo
