package org.cdlib.xtf.util;

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

import java.util.LinkedList;

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

/** This class provides a simple way to produce indented XML with matched
 *   begin and end tags. <br><br>
 *
 *    To use this class, you basically do the following: <br><br>
 *
 *    <code>
 *    Writer XMLDoc = new OutputStreamWriter(
 *                          new FileOutputStream( XMLFilePath )
 *                        );
 *
 *    FormatXML.tabSize(4); // (Defaults to 2 spaces if not specified)
 *
 *    XMLDoc.write(
 *      FormatXML.procInstr( "xml version=\"1.0\" encoding=\"utf-8\"" )
 *    );
 *
 *    XMLDoc.write(
 *     FormatXML.beginTag( "tagName", "tagAttr=\"value\"" )
 *    );
 *
 *    XMLDoc.write(
 *      FormatXML.text( "A bunch of text within a tag." ) + newLine(2)
 *    );
 *
 *    XMLDoc.write(
 *      FormatXML.endTag()
 *    );
 *    </code> <br><br>
 *
 *    This will produce an XML file with the following contents: <br><br>
 *
 *    &lt;?xml version="1.0" encoding="utf-8"?&gt;
 *
 *    &lt;tagName tagAttr="value"&gt;
 *
 *        A bunch of text within a tag.
 *
 *    &lt;/tagName&gt;
 */
public class FormatXML {

  /** Default amount to indent when {@link #tab()} is called. */
  public static int defaultTabSize = 2;

  //////////////////////////////////////////////////////////////////////////////

  /** Return whether or not a blank line will automatically be inserted after
   *  each new tag. <br><br>
   *
   *  The default behavior is to insert a blank line after each tag. To change
   *  this behavior, call the
   *  {@link FormatXML#blankLineAfterTag(boolean) blankLineAfterTag(boolean enable) }
   *  function. <br><br>
   *
   *  @return
   *     <code>true</code> - Blank lines will be inserted after each new tag.
   *     <code>false</code> - No blank lines will be inserted after each new tag.
   */
  public static final boolean blankLineAfterTag()

  {

    return mBlankLineAfterTag;

  } // public blankLineAfterTag()


  //////////////////////////////////////////////////////////////////////////////

  /** Set whether or not a blank line will automatically be inserted after
   *  each new tag. <br><br>
   *
   *  @param enable   Enable (<code>true</code>) or disable (<code>false</code>)
   *                  automatic blank line insertion after each tag.
   *
   *
   *  @return
   *      The value of the blank line flag just prior to this call.
   */
  public static final boolean blankLineAfterTag( boolean enable )

  {

    // Get the previous blank line flag.
    boolean oldBlankLineAfterTag = mBlankLineAfterTag;

    // Set the new blank line flag value.
    mBlankLineAfterTag = enable;

    // Return the previous value to the caller.
    return oldBlankLineAfterTag;

  } // public blankLineAfterTag( boolean enable )


  //////////////////////////////////////////////////////////////////////////////

  /** Return the current tab size used for indenting nested tags. <br><br>
   *
   *  By default, the tab size is set to 2 spaces. To change the indent size,
   *  call the {@link FormatXML#tabSize(int) tabSize(int newTabSize) }
   *  method.
   *
   *  @return
   *      The number of spaces each a nested line will be indented from its
   *      container.
   */
  public static final int tabSize()

  {

    return tabSize;

  } // public tabSize()


  //////////////////////////////////////////////////////////////////////////////

  /** Set the tab size used for indenting nested tags. <br><br>
   *
   *  @param newTabSize  The new tab size (in spaces) to indent a nested tag
   *                     from its containing tag.
   *
   *  @return
   *      The number of spaces each a nested line will be indented from its
   *      container.
   */
  public static final int tabSize( int newTabSize )

  {

    // Get hold of the previous tab size.
    int oldTabSize = tabSize;

    // Set the new tab size, and limit it to between 0 and 8 spaces per
    // nesting.
    //
    tabSize = newTabSize;
    if( tabSize < 0 ) tabSize = 0;
    if( tabSize > 8 ) tabSize = 8;

    // Return the previous indent size to the caller.
    return oldTabSize;

  } // public tabSize( int newTabSize )


  //////////////////////////////////////////////////////////////////////////////

  /** Return a string containing a properly indented begin tag consisting
   *  only of the tag name. <br><br>
   *
   *  @param tagName  The name of the tag to create.
   */
  public static final String beginTag( String tagName )

  {

    // If no tag name was specified, simply return an empty string.
    if( tagName == null || tagName.length() == 0 ) return "";

    // Indent, and write the begin tag name between angle brackets.
    String outStr = spaces.substring( 0, tabCount ) + "<" + tagName + ">\n";

    // Add the tag name to the current nesting stack so that the
    // endTag() method can properly close the tag when called.
    //
    tagStack.addLast( tagName );

    // If the user wants a blank line after each tag, put one in.
    if( mBlankLineAfterTag ) outStr += "\n";

    // Indent subsequent output to lie within this tag.
    tab();

    // And finally return the tag string to the caller.
    return outStr;

  } // public beginTag( String tagName )


  //////////////////////////////////////////////////////////////////////////////

  /** Format a tag attribute from an attribute name and an associated string
   *  value.
   */
   public static final String attr( String attName, String attValue )

  {

    return " " + attName + "=\"" + attValue + "\"";

  } // public attr()


  //////////////////////////////////////////////////////////////////////////////

  /** Format a tag attribute from an attribute name and an associated integer
   *  value.
   */
  public static final String attr( String attName, int attValue )

  {

    return " " + attName + "=\"" + Integer.toString(attValue) + "\"";

  } // public attr()


  //////////////////////////////////////////////////////////////////////////////

  /** Format a tag attribute from an attribute name and an associated 
   *  floating-point value.
   */
  public static final String attr( String attName, float attValue )

  {

    return " " + attName + "=\"" + Float.toString(attValue) + "\"";

  } // public attr()


  //////////////////////////////////////////////////////////////////////////////

  /** Return a string containing a properly indented begin tag consisting
   *  of a tag name and a list of attributes. <br><br>
   *
   *  @param tagName        The name of the tag to create.
   *      
   *  @param tagAtts        A string of attributes to tadd to the tag.
   *      
   *  @.notes
   *       Use the {@link FormatXML#attr() attr() } simplify constructing
   *       attribute name/value pairs.      
   */
  public static final String beginTag( 
  
      String  tagName, 
      String  tagAtts
  )

  {

    // If no tag name was specified, simply return an empty string.
    if( tagName == null || tagName.length() == 0 ) return "";

    // If the tag attributes string is empty, have the simple beginTag()
    // function do the work.
    //
    if( tagAtts == null || tagAtts.length() == 0 ) return beginTag( tagName );

    // Format up the tag name and attributes between angle brackets. Start
    // with the angle bracket and tag name.
    //
    String outStr = spaces.substring( 0, tabCount ) + "<" + tagName;
    
    // If the tag string doesn't start with a space, add one to separate
    // the first attribute from the tag name.
    //
    if( tagAtts.charAt(0) != ' ' ) outStr += " ";
    
    // Then add the closing angle bracket and we're done.
    outStr += tagAtts +">\n";
 
    // Add the tag name to the current nesting stack so that the
    // endTag() method can properly close the tag when called.
    //
    tagStack.addLast( tagName );

    // If the user wants a blank line after each tag, put one in.
    if( mBlankLineAfterTag ) outStr += "\n";

    // Indent subsequent output to lie within this tag.
    tab();

    // And finally return the tag string to the caller.
    return outStr;

  } // public beginTag( String tagName, String tagAtts )


  //////////////////////////////////////////////////////////////////////////////

  /** Return a string containing a properly indented end tag for the
   *  closest open tag (if any.)
   */
  public static final String endTag()

  {

    // If there are no open tags left, simply return an empty string.
    if( tagStack.isEmpty() ) return "";

    // Undo the current indent level.
    untab();

    // Format up the end-tag.
    String outStr = spaces.substring( 0, tabCount ) + "</" +
                    (String)(tagStack.removeLast()) + ">\n";

    // If the user wants a blank line after each tag, add one.
    if( mBlankLineAfterTag ) outStr += "\n";

    // Finally, return the end tag to the caller.
    return outStr;

  } // public endTag()


  //////////////////////////////////////////////////////////////////////////////

  /** Return a string containing properly indented end tags for any
   *  remaining open tags.
   */
  public static final String endAllTags()

  {

    // Start with no accumulated end tags.
    String outStr = "";

    // While there are open tags left, end each one.
    while( !tagStack.isEmpty() ) outStr += endTag();

    // Return the accumulated end tags.
    return outStr;

  } // public endAllTags()


  //////////////////////////////////////////////////////////////////////////////

  /** Return a processing instruction tag.
   *
   *  @param  tagStr  The processing instruction string to place in the tag.
   *
   *  @return
   *      A processing instruction tag indented to the current indentation
   *      level.
   */
  public static final String tag( String tagStr )

  {

    // Indent and assemble the specified tag.
    String outStr = spaces.substring( 0, tabCount ) + "<" + tagStr + "/>\n";

    // If the user wants a blank line after the tag, add one.
    if( mBlankLineAfterTag ) outStr += "\n";

    // Return the tag to the caller.
    return outStr;

  } // public tag()


  //////////////////////////////////////////////////////////////////////////////

  /** Return a processing instruction tag at the current level of indentation.
   *
   *  @param  procStr   The processing instruction string to place in the tag.
   */
  public static final String procInstr( String procStr )

  {

     // Format up the processing instruction tag.
     String outStr = spaces.substring( 0, tabCount ) + "<?" + procStr + "?>\n";

     // If the user wants a blank line after each tag, put one in.
     if( mBlankLineAfterTag ) outStr += "\n";

     // Return the tag string to the caller.
     return outStr;

  } // procInstr( String procStr )


  //////////////////////////////////////////////////////////////////////////////

  /** Return a string of text at the current level of indentation.
   *
   *  @param  str  The text to indent.
   */
  public static final String text( String str )

  {

    // If no text was passed by the caller, simply return an empty string.
    if( str == null || str.length() == 0 ) return "";
            
    // Otherwise, indent the text, and return it to the caller.
    return spaces.substring( 0, tabCount ) + str;

  } // public text( String str )


  //////////////////////////////////////////////////////////////////////////////

  /** Return a string containing text broken across multiple indented
   *  lines less than or equal to a maximum line length. <br><br>
   *
   *   @param  str        The string to break across multiple lines.
   *
   *   @param  maxWidth   The maximum width for each line.
   *
   *   @return
   *       The original input text broken across multiple lines.
   */
  public static final String text( String str, int maxWidth )

  {

    // If the caller didn't pass any text in, simply return an empty string.
    if( str == null ) return "";

    // If no maximum width was specified by the caller, call the simple
    // text function to do the work.
    //
    if( maxWidth <= 0 ) return text( str );

    // Start with no accumulated text.
    String outStr = "";

    // While the remaining text is longer than the maximum width...
    while( str != null ) {

        // Trim leading and trailing spaces from the the string.
        str = str.trim();

        // Start with the necessary indentation.
        outStr += spaces.substring( 0, tabCount );

        // Look for the first space/carriage-return/line-feed at or 
        // before the maximum width.
        //
        int spaceIdx = str.lastIndexOf( ' ',  maxWidth );
        int crIdx    = str.indexOf( '\r' );
        int lfIdx    = str.indexOf( '\n' );
        
        // Assume we'll break the string at the location of the space.
        int breakIdx = spaceIdx;
        
        // If we found a carriage-return, and it's within the max line
        // width, have it override the space as the break point.
        // 
        if( crIdx > -1 && crIdx < maxWidth ) {
            breakIdx = crIdx;
          
            // If we also found a line-feed, and it's before the carriage
            // return, have it override the carriage-return as the break
            // point.
            //
            if( lfIdx > -1 && lfIdx < crIdx ) breakIdx = lfIdx;
        }
        
        // We didn't find a carriage return. But if found a line-feed
        // and it's within the maximum line width, have it override the
        // space as the break point.
        // 
        else if( lfIdx > -1 && lfIdx < maxWidth ) 
            breakIdx = lfIdx;
        
        // If no break was found, simply use the string as is, and
        // consider the job done.
        //
        if( breakIdx == -1 ) {
            outStr += str;
            str = null;
        }

        // If the break was the first character in the
        // remaining text, skip it and go 'round again.
        //
        else if( breakIdx == 0 )
            str = str.substring( 1 );

        // For the break at any other location....
        else {

            // Tack on the text up to break character.
            outStr += str.substring( 0, breakIdx ) + " \n";

            // If the break was not the last character in the
            // remaining string, chop off the part we just
            // added to the output string and go 'round again.
            //
            if( breakIdx < str.length()-1)
                str = str.substring( breakIdx + 1 );

            // Otherwise, there's nothing left in the original
            // source string, so flag that we're finished.
            //
            else
                str = null;

        } // else( space at any other location )

    } // while( str != null && str.length() > maxWidth )

    // Finally, return the resulting output string.
    return outStr;

  } // public text( String str, int maxWidth )


  //////////////////////////////////////////////////////////////////////////////

  /** Return a string with a single new-line.
   * 
   */
  public static final String newLine() { return "\n"; }

  //////////////////////////////////////////////////////////////////////////////

  /** Return a string containing a specified number of new-lines.
   *
   *  @param lineCount  The number of new-lines to return.
   * 
   */
  public static final String newLine( int lineCount )

  {

    // If the caller wants more new lines than we can provide, default
    // to the maximum we can provide.
    //
    if( lineCount > newLines.length() ) lineCount = newLines.length();

    // Return a string containing the specified number of new lines.
    return newLines.substring( 0, lineCount );

  } // public newLine( int lineCount )


  //////////////////////////////////////////////////////////////////////////////

  /** Return a string containing one or two new-lines depending on whether 
   *  the user wants blank lines after tags or not.
   * 
   */
  public static final String newLineAfterText()

  {

    // If the caller wants a blank line after the text, return two new-lines.
    if( mBlankLineAfterTag ) return "\n\n";
    
    // Otherwise, just return one.
    return "\n";
    
  } // public newLineAfterText()


  //////////////////////////////////////////////////////////////////////////////

  /** Indent by the current tab size for all subsequent calls to FormatXML
   *  output functions.
   */
  private static final void tab()

  {

    // Increment the indent amount based on the current tab size.
    tabCount += tabSize;

    // And ensure that we don't indent more than the the number of
    // spaces available in the indent source string.
    //
    if( tabCount > spaces.length() ) tabCount = spaces.length();

  } // public tab()


  //////////////////////////////////////////////////////////////////////////////

  /** Un-indent by the current tab size for all subsequent calls to FormatXML
   *  output functions.
   */
  private static final void untab()

  {

    // If the current indentation is greater than the current tab size,
    // actually unindent. Otherwise, unindent as far as possible, but not
    // back past zero.
    //
    if( tabCount > tabSize ) tabCount -= tabSize;
    else                     tabCount =  0;

  } // public untab()


  //////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////

  /** Used for tabbing */
  private static final String spaces =
  "                                                                      " +
  "                                                                      " +
  "                                                                      " +
  "                                                                      " +
  "                                                                      " +
  "                                                                      " +
  "                                                                      " +
  "                                                                      ";

 /** Used for multiple blank line output */
  private static final String newLines =
  "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" +
  "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" +
  "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" +
  "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" +
  "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" +
  "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" +
  "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" +
  "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n";

  /** Stack of current tag nestings */
  private static LinkedList tagStack = new LinkedList();

  /** Current tab level for this thread */
  private static int tabCount = 0;

  /** Automatically insert blank lines after tags */
  private static boolean mBlankLineAfterTag = true;

  /** Amount to indent when {@link #tab()} is called. */
  private static int tabSize = defaultTabSize;
}
