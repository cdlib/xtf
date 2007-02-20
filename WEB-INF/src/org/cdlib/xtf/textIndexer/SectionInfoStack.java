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
import java.util.Stack;

////////////////////////////////////////////////////////////////////////////////

/**
 * This class maintains information about the current nesting of sections
 * in a text document that the TextIndexer program is processing. <br><br>
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
 * The <code>SectionInfoStack</code> class is used to maintain the current
 * state of nested sections encountered in a document by the text indexer,
 * while the {@link org.cdlib.xtf.textIndexer.SectionInfo} class holds
 * the section attributes for each entry in the current stack. <br><br>
 *
 */
public class SectionInfoStack 
{
  /** Actual generic stack that holds the
   *  {@link org.cdlib.xtf.textIndexer.SectionInfo} objects.
   */
  private Stack infoStack = new Stack();

  //////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////

  /** Explicit section push operator. <br><br>
   *
   *  Call this method to push a new section onto the stack with explicitly
   *  specified values for the section's attributes. <br><br>
   *
   *  @param indexFlag     A flag indicating whether or not the current section
   *                       should be indexed. Valid values are
   *                       {@link org.cdlib.xtf.textIndexer#parentIndex parentIndex},
   *                       {@link org.cdlib.xtf.textIndexer#index index},
   *                       {@link org.cdlib.xtf.textIndexer#noIndex noIndex}.
   *                       <br><br>
   *
   *  @param sectionType   The type name for the section being pushed. This may
   *                       either a caller defined string or an empty string
   *                       (""). Note that if an empty string is passed, the
   *                       section name is inherited from the parent section
   *                       (if defined.) <br><br>
   *
   *  @param sectionBump   The offset (in words) of the current section from
   *                       the previous section. Used to lower the relevance of
   *                       (or completely avoid) proximity matches that
   *                       span two sections. This value is typically set to
   *                       zero (for no de-emphasis of proximity matches
   *                       across adjacent sections), or a value greater than
   *                       or equal to the chunk overlap used by the index (to
   *                       completely avoid proximity matches across adjacent
   *                       sections.) <br><br>
   *
   *  @param wordBoost     Boost factor to apply to words in this section.
   *                       values greater than 1.0 make the words found in this
   *                       section more relevant in a search, while values less
   *                       than 1.0 make words in the section less relevant.
   *                       <br><br>
   *
   *  @param sentenceBump  The offset (in words) for this section between the
   *                       start of a new sentence and the end of the previous
   *                       one. Like the section bump, this value is used to
   *                       adjust the relevance of proximity matches made
   *                       across sentence boundaries. Typical values are
   *                       one (for no de-emphasis of proximity matches across
   *                       sentence boundaries), a value between one and the
   *                       chunk overlap for the index (for partial de-emphasis
   *                       of proximity matches across sentence boundaries), or
   *                       a value greater than or equal to the chunk size to
   *                       completely avoid proximity matches across sentence
   *                       boundaries.) <br><br>
   *
   *  @param spellFlag     A flag indicating whether or not words in the current
   *                       section should be added to the spelling correction
   *                       dictionary. Valid values are
   *                       {@link org.cdlib.xtf.textIndexer#parentSpell parentSpell},
   *                       {@link org.cdlib.xtf.textIndexer#spell spell},
   *                       {@link org.cdlib.xtf.textIndexer#noSpell noSpell}.
   *                       <br><br>
   *
   *  @.notes
   *       This method compares the passed attributes to the section currently
   *       at the top of the stack (if any.) If the attributes are identical,
   *       the {@link org.cdlib.xtf.textIndexer.SectionInfoStack#push() depth-push}
   *       method is called to save space. Otherwise, the new section entry
   *       with the passed attributes is created and placed on the stack.
   *       <br><br>
   *
   *       For a more complete description of the above listed attributes,
   *       see the {@link org.cdlib.xtf.textIndexer.SectionInfo SectionInfo}
   *       class. <br><br>
   */
  public void push(int indexFlag, String sectionType, int sectionBump,
                   float wordBoost, int sentenceBump, int spellFlag) 
  {
    int prevSectionBump = 0;

    // See what's on the top of the stack.
    SectionInfo info = top();

    // If there's something on the stack...
    if (info != null) 
    {
      // And we were asked to inherit the parent's index flag, do so.
      if (indexFlag == SectionInfo.parentIndex)
        indexFlag = info.indexFlag;

      // If we were asked to inherit the parent's spell flag, do so.
      if (spellFlag == SectionInfo.parentSpell)
        spellFlag = info.spellFlag;

      // If no section name was specified, inherit the parent's section name.
      if (sectionType == "")
        sectionType = info.sectionType;

      // If the information being pushed is the same as what's already
      // on the top of the stack, simply increase the depth of the current
      // info at the top of the stack. 
      //
      if (!valuesChanged(indexFlag,
                         sectionType,
                         sectionBump,
                         wordBoost,
                         sentenceBump,
                         spellFlag)) 
      {
        push();
        return;
      }

      /////////////////////////////////////////////////////////////////
      // If we got here, then the section information passed doesn't //
      // match what's at the top of the stack, so fall through and   //
      // add a new entry to the stack.                               //
      /////////////////////////////////////////////////////////////////

      // If there was still a section bump pending from the previous
      // section, forward it onto the current section. By doing this,
      // we will correctly accumulate bump values if a new section
      // occurs immediately after a containing section (i.e., no text
      // appears between the start of a parent and child node.) 
      //
      prevSectionBump = info.saveSectionBump();
    } // if( info != null )

    // If there was no previous section info on the stack, and we were asked
    // to inherit the parent's index flag, turn on indexing by default.
    //
    if (indexFlag == SectionInfo.parentIndex)
      indexFlag = SectionInfo.defaultIndexFlag;

    // Likewise with the spell flag.
    if (spellFlag == SectionInfo.parentSpell)
      spellFlag = SectionInfo.defaultSpellFlag;

    // At this point, we need to push new section info on the stack, either
    // because there's nothing on the stack, or because the specified section
    // info doesn't match the previous section info.
    //
    infoStack.push(new SectionInfo(0,
                                   indexFlag,
                                   sectionType,
                                   prevSectionBump + sectionBump,
                                   wordBoost,
                                   sentenceBump,
                                   spellFlag));
  } // public push( indexFlag, ... )

  //////////////////////////////////////////////////////////////////////////////

  /** Implicit depth-push operator. <br><br>
   *
   *  Call this method to push a section onto the stack with all the same
   *  attributes as the previous section. This method uses the
   *  {@link org.cdlib.xtf.textIndexer.SectionInfo#depth depth} field of the
   *  {@link org.cdlib.xtf.textIndexer.SectionInfo} class to maintain the
   *  correct depth for nested sections with identical attributes while avoiding
   *  pushing entire duplicate entries. <br><br>
   *
   *  @.notes Use the
   *  {@link org.cdlib.xtf.textIndexer.SectionInfoStack#valuesChanged(int,String,int,float,int,int) valuesChanged()}
   *  method to determine if your attributes for a new section are identical to
   *  the section currently at the top of the stack before calling this method.
   *  Alternately, you can simply pass your new attributes to the
   *  {@linkplain org.cdlib.xtf.textIndexer.SectionInfoStack#push(int,String,int,float,int,int) explicit section-push operator},
   *  which performs the same check internally and calls this method as needed.
   *  <br><br>
   *
   */
  public void push() 
  {
    // If there's nothing on the stack, push a default section info structure.
    if (isEmpty()) 
    {
      // As a default, push a new section with initial depth of zero, the
      // index flag set to "true", no section bump, a word bump of one, no
      // no word boost, and a sentence bump of 5.
      //
      push(new SectionInfo());
      return;
    }

    // Something's on the stack, so increment it's depth.
    top().depth++;
  } // public push()

  //////////////////////////////////////////////////////////////////////////////

  /** Section de-stacking operator. <br><br>
   *
   *  Call this method to pop a section off the nesting stack. <br><br>
   *
   *  @.notes Internally, this method decrements the depth of the topmost entry
   *          in the stack, and if the depth goes to zero, it removes the
   *          topmost entry from the stack. <br><br>
   *
   *          This method does nothing if there nesting stack is empty.
   */
  public void pop() 
  {
    // If there's nothing on the stack to pop, we're done.
    if (isEmpty())
      return;

    // Assume we don't need  to restore the previous section bump.
    boolean restorePrevSectionBump = false;

    // If the accumulated section bump didn't get used in the current
    // section, we must restore the section bump for the previous section.
    //
    if (top().sectionBump != 0)
      restorePrevSectionBump = true;

    // Actually pop the top item off the section info stack.
    if (--(top().depth) == -1)
      infoStack.pop();

    // If there's nothing left on the stack, we're done.
    if (isEmpty())
      return;

    // If we need to restore the previous section's bump value, do so.
    if (restorePrevSectionBump)
      top().restoreSectionBump();
  } // pop()

  //////////////////////////////////////////////////////////////////////////////

  /** Return a copy of the section currently at the top of the nesting
   *  without popping the stack.
   *  <br><br>
   *
   *  @return  A copy of the top entry in the nesting stack.
   *
   */
  public SectionInfo peek() {
    return top();
  }

  //////////////////////////////////////////////////////////////////////////////

  /** Query method to determine if there are any nested sections currently on
   *  the nesting stack. <br><br>
   *
   *  @return <code>true</code> - No nested sections currently on the stack.
   *                              <br>
   *          <code>false</code> - One or more nested sections are currently
   *                               on the stack. <br><br>
   */
  public boolean isEmpty() {
    return infoStack.isEmpty();
  }

  //////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////

  /** Query method to determine if the passed set of section attributes differs
   *  from the section at the top of the nesting stack. <br><br>
   *
   *  @return <code>true</code> - One or more of the passed attributes do not
   *                              match the attributes for the section
   *                              currently at the top of the stack.
   *                              <br>
   *          <code>false</code> - The passed attributes are identical to those
   *                               for the section currently at the top of the
   *                               stack. <br><br>
   *
   *  @.notes  If the stack is empty when this method is called, the value
   *           <code>true</code>. <br><br>
   */
  public boolean valuesChanged(int indexFlag, String sectionType,
                               int sectionBump, float wordBoost,
                               int sentenceBump, int spellFlag) 
  {
    // If the stack is empty, tell the caller that the values have changed.
    if (isEmpty())
      return true;

    // Otherwise, take a peek at the top entry on the stack.
    SectionInfo info = top();

    // If the caller wants to use the parent's index flag, get it.
    if (indexFlag == SectionInfo.parentIndex)
      indexFlag = info.indexFlag;

    // If the caller wants to use the parent's spell flag, get it.
    if (spellFlag == SectionInfo.parentSpell)
      spellFlag = info.spellFlag;

    // If no explicit section bump was specified, inherit the parent's bump.
    if (sectionBump == 0)
      sectionBump = info.sectionBump;

    // Now this part looks a bit weird... If the specified bump was an explicit
    // value, set it to some crazy value for the following comparison.  Why? 
    // To ensure that section bump values for parent and child nodes with 
    // no intervening text are considered different (because nested explicit 
    // section bumps should be additive.)
    //  
    else
      sectionBump = -1;

    // If any of the values passed differs from the ones on the stack,
    // tell the caller.
    //
    if (indexFlag != info.indexFlag ||
        sectionType != info.sectionType ||
        sectionBump != info.sectionBump ||
        wordBoost != info.wordBoost ||
        sentenceBump != info.sentenceBump ||
        spellFlag != info.spellFlag)
      return true;

    // Otherwise, indicate that the values are the same.    
    return false;
  } // public valuesChanged()

  //////////////////////////////////////////////////////////////////////////////

  /** Return the current depth of the top section on the nesting stack. <br><br>
   *
   *  @return  The current depth of the entry at the top of the section stack,
   *           or <code>-1</code> if the stack is empty.
   */
  public int depth() 
  {
    // If the stack is empty, indicate with a -1 that there is no current depth.
    if (isEmpty())
      return -1;

    // Otherwise return the actual depth for the top entry.
    return top().depth;
  } // depth()

  //////////////////////////////////////////////////////////////////////////////

  /** Return the index flag for the top section on the nesting stack. <br><br>
   *
   *  @return  Returns {@link org.cdlib.xtf.textIndexer.SectionInfo#index index}
   *           or
   *           {@link org.cdlib.xtf.textIndexer.SectionInfo#noIndex noIndex}.
   *           <br><br>
   *
   *  @.notes
   *
   *  This function will never return
   *  {@link org.cdlib.xtf.textIndexer.SectionInfo#parentIndex parentIndex}.
   *  That value is only used as an argument when calling the
   *  {@linkplain org.cdlib.xtf.textIndexer.SectionInfoStack#push(int,String,int,float,int,int) explicit section-push}
   *  operator to force the new section to adopt it's parents index
   *  flag.<br><br>
   *
   *  For a complete explanation of the <code>indexFlag</code> attribute, see
   *  the {@link org.cdlib.xtf.textIndexer.SectionInfo#indexFlag indexFlag}
   *  field in the {@link org.cdlib.xtf.textIndexer.SectionInfo} class. <br><br>
   */
  public int indexFlag() 
  {
    // If the stack is empty, return a no indexing flag, as the outermost node
    // is seldomly ever indexed.
    //
    if (isEmpty())
      return SectionInfo.noIndex;

    // Otherwise return the actual index flag for the top entry.
    return top().indexFlag;
  } // indexFlag()

  //////////////////////////////////////////////////////////////////////////////

  /** Return the spell flag for the top section on the nesting stack. <br><br>
   *
   *  @return  Returns {@link org.cdlib.xtf.textIndexer.SectionInfo#spell spell}
   *           or
   *           {@link org.cdlib.xtf.textIndexer.SectionInfo#noSpell noSpell}.
   *           <br><br>
   *
   *  @.notes
   *
   *  This function will never return
   *  {@link org.cdlib.xtf.textIndexer.SectionInfo#parentSpell parentSpell}.
   *  That value is only used as an argument when calling the
   *  {@linkplain org.cdlib.xtf.textIndexer.SectionInfoStack#push(int,String,int,float,int,int) explicit section-push}
   *  operator to force the new section to adopt it's parents spell
   *  flag.<br><br>
   *
   *  For a complete explanation of the <code>spellFlag</code> attribute, see
   *  the {@link org.cdlib.xtf.textIndexer.SectionInfo#spellFlag spellFlag}
   *  field in the {@link org.cdlib.xtf.textIndexer.SectionInfo} class. <br><br>
   */
  public int spellFlag() 
  {
    // If the stack is empty, return the default spelling flag.
    //
    if (isEmpty())
      return SectionInfo.defaultSpellFlag;

    // Otherwise return the actual spell flag for the top entry.
    return top().spellFlag;
  } // spellFlag()

  //////////////////////////////////////////////////////////////////////////////

  /** Return the section type name for the top section on the nesting stack.
   *  <br><br>
   *
   *  @return  Returns the name of the top section entry on the stack (if any)
   *           or an empty string if no type name is assigned or the stack is
   *           empty. <br><br>
   *
   *  @.notes
   *  For a complete explanation of the <code>sectionType</code> attribute, see
   *  the {@link org.cdlib.xtf.textIndexer.SectionInfo#sectionType sectionType}
   *  field in the {@link org.cdlib.xtf.textIndexer.SectionInfo} class. <br><br>
   */
  public String sectionType() 
  {
    // If the stack is empty, return the default (empty) type name.
    if (isEmpty())
      return SectionInfo.defaultSectionType;

    // Otherwise, return the actual type name from the top entry.
    return top().sectionType;
  } // sectionType()

  //////////////////////////////////////////////////////////////////////////////

  /** Return the section bump value for the top section on the nesting stack.
   *  <br><br>
   *
   *  @return  Returns the bump value for the top section entry on the stack
   *           (if any), or the {@link org.cdlib.xtf.textIndexer.SectionInfo#defaultSectionBump defaultSectionBump}
   *           value if the stack is empty. <br><br>
   *
   *  @.notes
   *  For a complete explanation of the <code>sectionBump</code> attribute, see
   *  the {@link org.cdlib.xtf.textIndexer.SectionInfo#sectionBump sectionBump}
   *  field in the {@link org.cdlib.xtf.textIndexer.SectionInfo} class. <br><br>
   */
  public int sectionBump() 
  {
    // If the stack is empty, return the default section bump value.
    if (isEmpty())
      return SectionInfo.defaultSectionBump;

    // Otherwise, return the actual section bump for the top entry.
    return top().sectionBump;
  } // sectionBump()

  //////////////////////////////////////////////////////////////////////////////

  /** Use and clear the section bump value for the top section on the nesting
   *  stack. <br><br>
   *
   *  @return  Returns the bump value for the top section entry on the stack
   *           (if any), or the {@link org.cdlib.xtf.textIndexer.SectionInfo#defaultSectionBump defaultSectionBump}
   *           value if the stack is empty. <br><br>
   *
   *  @.notes
   *  "Using" the section bump at the top of the stack consists of retrieving
   *  its value and resetting its field to zero. This is done so that any
   *  accumulated bump from nested sections is used only once. After the reset,
   *  subsequent calls to this function will return zero, thus preventing any
   *  unwanted repeat bumping. <br><br>
   *
   *  For a complete explanation of the <code>sectionBump</code> attribute, see
   *  the {@link org.cdlib.xtf.textIndexer.SectionInfo#sectionBump sectionBump}
   *  field in the {@link org.cdlib.xtf.textIndexer.SectionInfo} class. <br><br>
   *
   */
  public int useSectionBump() 
  {
    // If the stack is empty, return the default section bump value.
    if (isEmpty())
      return SectionInfo.defaultSectionBump;

    // Otherwise, get a reference to the top entry.
    SectionInfo info = top();

    // Then fetch the bump value to return.
    int sectionBump = info.sectionBump;

    // And zero the bump out so that it only gets used once. 
    info.sectionBump = 0;

    // Then return the original bump to the caller.
    return sectionBump;
  } // useSectionBump()

  //////////////////////////////////////////////////////////////////////////////

  /** This function sets the section bump value for the top entry in the stack.
   *  <br><br>
   *
   *  @param newBump   New bump value to set for top entry. <br><br>
   *
   *  @return          The bump value set for the top entry in the stack just
   *                   before this call was made. <br><br>
   *
   *  @.notes
   *  For a complete explanation of the <code>sectionBump</code> attribute, see
   *  the {@link org.cdlib.xtf.textIndexer.SectionInfo#sectionBump sectionBump}
   *  field in the {@link org.cdlib.xtf.textIndexer.SectionInfo} class. <br><br>
   */
  public int setSectionBump(int newBump) 
  {
    // If the stack is empty, return the default section bump.
    if (isEmpty())
      return SectionInfo.defaultSectionBump;

    // Get a reference to the top entry in the stack.
    SectionInfo info = top();

    // Hang on to the section bump value already recorded.
    int retBump = info.sectionBump;

    // Set the new bump value passed in.
    info.sectionBump = newBump;

    // And finally return the original bump value to the caller.
    return retBump;
  } // setSectionBump()

  //////////////////////////////////////////////////////////////////////////////

  /** Return the word boost value for the top entry in the stack.
   *
   *  @return   If the stack is empty, this function returns
   *            {@link org.cdlib.xtf.textIndexer.SectionInfo#defaultWordBoost}.
   *            Otherwise, it returns the word boost for the section currently
   *            at the top of the stack. <br><br>
   *
   *  @.notes
   *  For a complete explanation of the <code>wordBoost</code> attribute, see
   *  the {@link org.cdlib.xtf.textIndexer.SectionInfo#wordBoost wordBoost}
   *  field in the {@link org.cdlib.xtf.textIndexer.SectionInfo} class. <br><br>
   */
  public float wordBoost() 
  {
    // If the stack is empty, simply return the default word boost.
    if (isEmpty())
      return SectionInfo.defaultWordBoost;

    // Otherwise return the actual word boost for the top entry.
    return top().wordBoost;
  } // wordBoost()

  //////////////////////////////////////////////////////////////////////////////

  /** Return the sentence bump value for the top entry in the stack.
    *
    *  @return   If the stack is empty, this function returns
    *            {@link org.cdlib.xtf.textIndexer.SectionInfo#defaultSentenceBump}.
    *            Otherwise, it returns the sentence bump for the section
    *            currently at the top of the stack. <br><br>
    *
   *  @.notes
   *  For a complete explanation of the <code>sentenceBump</code> attribute, see
   *  the {@link org.cdlib.xtf.textIndexer.SectionInfo#sentenceBump sentenceBump}
   *  field in the {@link org.cdlib.xtf.textIndexer.SectionInfo} class. <br><br>
    */
  public int sentenceBump() 
  {
    // If the stack is empty, simply return the default word boost.
    if (isEmpty())
      return SectionInfo.defaultSentenceBump;

    // Otherwise, return the sentence bump for the top entry.
    return top().sentenceBump;
  } // sentenceBump()

  //////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////

  /** Push a {@link org.cdlib.xtf.textIndexer.SectionInfo} instance onto the
   *  top of the section stack. <br><br>
   *
   *  @.notes
   *  This method is a convenience function that does the necessary down-
   *  casting to have the generic stack object take a <code>SectionInfo</code>
   *  instance.
   *
   */
  private void push(SectionInfo info) {
    infoStack.push(info);
  }

  //////////////////////////////////////////////////////////////////////////////

  /** Return a reference to the top entry in the section stack, if any.
   *  <br><br>
   *
   *  @return  A reference to the top item in the section info stack, or
   *           <code>null</code> if the stack is empty.
   *
   *  @.notes
   *  This method is a convenience function that does the necessary up-
   *  casting to have the generic stack object return a <code>SectionInfo</code>
   *  instance.
   *
   */
  private SectionInfo top() 
  {
    // If the stack is empty, say so.
    if (isEmpty())
      return null;

    // Otherwise up-cast a reference to whatever is at the top of the stack.
    return (SectionInfo)(infoStack.peek());
  } // top()
} // class SectionInfoStack
