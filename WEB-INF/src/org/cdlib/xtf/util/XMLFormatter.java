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

import java.io.StringReader;
import java.util.LinkedList;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.Configuration;
import net.sf.saxon.om.AllElementStripper;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.tinytree.TinyBuilder;
import net.sf.saxon.trans.XPathException;

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

/** This class provides a simple way to produce indented XML with matched
 *   begin and end tags. <br><br>
 *
 *    To use this class, you basically do the following: <br><br>
 *
 *    <code>
 *
 *    XMLFormatter formatter = new XMLFormatter();
 *    
 *    formatter.tabSize(4); // (Defaults to 2 spaces if not specified)
 *    formatter.procInstr( "xml version=\"1.0\" encoding=\"utf-8\"" )
 *    formatter.beginTag( "tagName", "tagAttr=\"value\"" )
 *    formatter.text( "A bunch of text within a tag." ) + newLine(2)
 *    formatter.endTag();
 *    String result = formatter.toString();
 *    </code> <br><br>
 *
 *    This will produce an XML string with the following contents: <br><br>
 *
 *    &lt;?xml version="1.0" encoding="utf-8"?&gt;
 *
 *    &lt;tagName tagAttr="value"&gt;
 *
 *        A bunch of text within a tag.
 *
 *    &lt;/tagName&gt;
 */
public class XMLFormatter {
  
  /** Buffer to accumulate the results in */
  private StringBuffer buf = new StringBuffer();

  /** Default amount to indent when {@link #tab()} is called. */
  public int defaultTabSize = 2;

  //////////////////////////////////////////////////////////////////////////////

  /** Return whether or not a blank line will automatically be inserted after
   *  each new tag. <br><br>
   *
   *  The default behavior is to insert a blank line after each tag. To change
   *  this behavior, call the
   *  {@link XMLFormatter#blankLineAfterTag(boolean) blankLineAfterTag(boolean enable) }
   *  function. <br><br>
   *
   *  @return
   *     <code>true</code> - Blank lines will be inserted after each new tag.
   *     <code>false</code> - No blank lines will be inserted after each new tag.
   */
  public boolean blankLineAfterTag()

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
  public boolean blankLineAfterTag( boolean enable )

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
   *  call the {@link XMLFormatter#tabSize(int) tabSize(int newTabSize) }
   *  method.
   *
   *  @return
   *      The number of spaces each a nested line will be indented from its
   *      container.
   */
  public int tabSize()

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
  public int tabSize( int newTabSize )

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

  /** Add a string containing a properly indented begin tag consisting
   *  only of the tag name. <br><br>
   *
   *  @param tagName  The name of the tag to create.
   */
  public void beginTag( String tagName )

  {

    // Close any previous tag that's hanging open.
    closeTagStart();
    
    // If no tag name was specified, simply return.
    if( tagName == null || tagName.length() == 0 ) return;

    // Indent, and write the begin tag name between angle brackets.
    buf.append( spaces.substring(0, tabCount) );
    buf.append( "<" );
    buf.append( tagName );
    
    // Remember that we have a tag currently open. It will be closed by
    // any operation except adding an attribute.
    //
    tagStartOpen = true;

    // Add the tag name to the current nesting stack so that the
    // endTag() method can properly close the tag when called.
    //
    tagStack.addLast( tagName );

    // Indent subsequent output to lie within this tag.
    tab();

  } // public beginTag( String tagName )
  
  
  //////////////////////////////////////////////////////////////////////////////

  /**
   * If there has been a beginTag(), we need to be sure and add the closing
   * ">" before doing anything else.
   */
  private void closeTagStart()
  
  {
    
    // If there's no tag open, simply return.
    if( !tagStartOpen ) return;
    
    // Emit the closing bracket and a newline.
    buf.append( ">\n" );
    
    // All done.
    tagStartOpen = false;
    
  } // private closeTag()


  //////////////////////////////////////////////////////////////////////////////

  /** Format a tag attribute from an attribute name and an associated string
   *  value.
   */
  public void attr( String attName, String attValue )

  {

    buf.append( " " );
    buf.append( attName );
    buf.append( "=\"" );
    buf.append( escapeText(attValue) );
    buf.append( "\"" );

  } // public attr()


  //////////////////////////////////////////////////////////////////////////////

  /** Format a tag attribute from an attribute name and an associated integer
   *  value.
   */
  public void attr( String attName, int attValue )

  {
    attr( attName, Integer.toString(attValue) );

  } // public attr()


  //////////////////////////////////////////////////////////////////////////////

  /** Format a tag attribute from an attribute name and an associated 
   *  floating-point value.
   */
  public void attr( String attName, float attValue )

  {
    attr( attName, Float.toString(attValue) );

  } // public attr()


  //////////////////////////////////////////////////////////////////////////////

  /** Format a string containing a properly indented begin tag consisting
   *  of a tag name and a list of attributes. <br><br>
   *
   *  @param tagName        The name of the tag to create.
   *      
   *  @param tagAtts        A string of attributes to tadd to the tag.
   *      
   *  @.notes
   *       Use the {@link XMLFormatter#attr(String, String) attr() } method
   *       and its cousins to simplify constructing attribute name/value 
   *       pairs.      
   */
  public void beginTag( 
  
      String  tagName, 
      String  tagAtts
  )

  {

    // Close any previous tag that's hanging open.
    closeTagStart();
    
    // If no tag name was specified, simply return.
    if( tagName == null || tagName.length() == 0 ) return;

    // If the tag attributes string is empty, have the simple beginTag()
    // function do the work.
    //
    if( tagAtts == null || tagAtts.length() == 0 ) {
        beginTag( tagName );
        return;
    }

    // Format up the tag name and attributes between angle brackets. Start
    // with the angle bracket and tag name.
    //
    buf.append( spaces.substring(0, tabCount) );
    buf.append( "<" );
    buf.append( tagName );
    
    // If the tag string doesn't start with a space, add one to separate
    // the first attribute from the tag name.
    //
    if( tagAtts.charAt(0) != ' ' ) buf.append( " " );
    
    // Then add the closing angle bracket and we're done.
    buf.append( tagAtts );
    buf.append( ">\n" );
 
    // Add the tag name to the current nesting stack so that the
    // endTag() method can properly close the tag when called.
    //
    tagStack.addLast( tagName );

    // If the user wants a blank line after each tag, put one in.
    if( mBlankLineAfterTag ) buf.append( "\n" );

    // Indent subsequent output to lie within this tag.
    tab();

  } // public beginTag( String tagName, String tagAtts )


  //////////////////////////////////////////////////////////////////////////////

  /** Add a string containing a properly indented end tag for the
   *  closest open tag (if any.)
   */
  public void endTag()

  {

    // Close any previous tag that's hanging open.
    closeTagStart();
    
    // If there are no open tags left, simply return;
    if( tagStack.isEmpty() ) return;

    // Undo the current indent level.
    untab();

    // Format up the end-tag.
    buf.append( spaces.substring( 0, tabCount ) );
    buf.append( "</" );
    buf.append( (String)(tagStack.removeLast()) );
    buf.append( ">\n" );

    // If the user wants a blank line after each tag, add one.
    if( mBlankLineAfterTag ) buf.append( "\n" );

  } // public endTag()


  //////////////////////////////////////////////////////////////////////////////

  /** Add a string containing properly indented end tags for any
   *  remaining open tags.
   */
  public void endAllTags()

  {

    // Close any previous tag that's hanging open.
    closeTagStart();
    
    // Start with no accumulated end tags.
    String outStr = "";

    // While there are open tags left, end each one.
    while( !tagStack.isEmpty() ) endTag();

  } // public endAllTags()


  //////////////////////////////////////////////////////////////////////////////

  /** Format an element tag.
   *
   *  @param  tagStr  The string to place in the tag.
   */
  public void tag( String tagStr )

  {

    // Close any previous tag that's hanging open.
    closeTagStart();
    
    // Indent and assemble the specified tag.
    buf.append( spaces.substring( 0, tabCount ) );
    buf.append( "<" );
    buf.append( tagStr );
    buf.append( "/>\n" );

    // If the user wants a blank line after the tag, add one.
    if( mBlankLineAfterTag ) buf.append( "\n" );

  } // public tag()


  //////////////////////////////////////////////////////////////////////////////

  /** Format a processing instruction tag at the current level of indentation.
   *
   *  @param  procStr   The processing instruction string to place in the tag.
   */
  public void procInstr( String procStr )

  {

    // Close any previous tag that's hanging open.
    closeTagStart();
    
     // Format up the processing instruction tag.
     buf.append( spaces.substring(0, tabCount) );
     buf.append( "<?" );
     buf.append( procStr );
     buf.append( "?>\n" );

     // If the user wants a blank line after each tag, put one in.
     if( mBlankLineAfterTag ) buf.append( "\n" );

  } // procInstr( String procStr )


  //////////////////////////////////////////////////////////////////////////////

  /** Format a string of text at the current level of indentation.
   *
   *  @param  str  The text to indent.
   */
  public void text( String str )

  {

    // Close any previous tag that's hanging open.
    closeTagStart();
    
    // If no text was passed by the caller, simply return.
    if( str == null || str.length() == 0 ) return;
            
    // Escape any special characters.
    str = escapeText( str );
    
    // Otherwise, indent the text, and add it to the buffer.
    buf.append( spaces.substring(0, tabCount) );
    buf.append( str );

  } // public text( String str )


  //////////////////////////////////////////////////////////////////////////////

  /** Format a string containing text broken across multiple indented
   *  lines less than or equal to a maximum line length. <br><br>
   *
   *   @param  str        The string to break across multiple lines.
   *
   *   @param  maxWidth   The maximum width for each line.
   */
  public void text( String str, int maxWidth )

  {

    // Close any previous tag that's hanging open.
    closeTagStart();
    
    // Escape any special characters.
    str = escapeText( str );
    
    // If the caller didn't pass any text in, simply return.
    if( str == null ) return;

    // If no maximum width was specified by the caller, call the simple
    // text function to do the work.
    //
    if( maxWidth <= 0 ) {
        text( str );
        return;
    }

    // While the remaining text is longer than the maximum width...
    while( str != null ) {

        // Trim leading and trailing spaces from the the string.
        str = str.trim();

        // Start with the necessary indentation.
        buf.append( spaces.substring(0, tabCount) );

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
            buf.append( str );
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
            buf.append( str.substring(0, breakIdx) );
            buf.append( " \n" );

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

  } // public text( String str, int maxWidth )


  //////////////////////////////////////////////////////////////////////////////

  /** Adds a text string, unformatted and unescaped, directly to the buffer.
   *  <br><br>
   *
   *   @param  str        The string to add to the buffer.
   */
  public void rawText( String str )

  {
    
    // Close any previous tag that's hanging open.
    closeTagStart();

    // And add the string, with no escaping, formatting, etc.
    buf.append( str );
    
  } // public rawText( String str )
  
  
  //////////////////////////////////////////////////////////////////////////////

  /** Add a single new-line.
   * 
   */
  public void newLine() { buf.append( "\n" ); }

  //////////////////////////////////////////////////////////////////////////////

  /** Add a specified number of new-lines.
   *
   *  @param lineCount  The number of new-lines to add.
   * 
   */
  public void newLine( int lineCount )

  {

    // If the caller wants more new lines than we can provide, default
    // to the maximum we can provide.
    //
    if( lineCount > newLines.length() ) lineCount = newLines.length();

    // Add a string containing the specified number of new lines.
    buf.append( newLines.substring(0, lineCount) );

  } // public newLine( int lineCount )


  //////////////////////////////////////////////////////////////////////////////

  /** Adds a string containing one or two new-lines depending on whether 
   *  the user wants blank lines after tags or not.
   * 
   */
  public void newLineAfterText()

  {
    newLine( mBlankLineAfterTag ? 2 : 1 );
    
  } // public newLineAfterText()
  
  
  //////////////////////////////////////////////////////////////////////////////

  /** Get the current tab indent level (in spaces). */
  public int tabCount()

  {
    return tabCount;
  }  


  //////////////////////////////////////////////////////////////////////////////

  /** Indent by the current tab size for all subsequent calls to FormatXML
   *  output functions.
   */
  private void tab()

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
  private void untab()

  {

    // If the current indentation is greater than the current tab size,
    // actually unindent. Otherwise, unindent as far as possible, but not
    // back past zero.
    //
    if( tabCount > tabSize ) tabCount -= tabSize;
    else                     tabCount =  0;

  } // public untab()
  
  
  //////////////////////////////////////////////////////////////////////////////

  /**
   * Change any XML-special characters to their ampersand equivalents.
   *  
   * @param text  Text to scan
   * @return      Modified version with escaped characters.
   */
  public static String escapeText( String text )
  
  {
    
      if( text.indexOf('&') >= 0 )
          text = text.replaceAll( "&", "&amp;" );
      
      if( text.indexOf('<') >= 0 )
          text = text.replaceAll( "<", "&lt;"  );
      
      if( text.indexOf('>') >= 0 )
          text = text.replaceAll( ">", "&gt;"  );
      
      if( text.indexOf('\"') >= 0 )
          text = text.replaceAll( "\"", "&quot;"  );
      
      return text;
    
  } // public escapeText()


  //////////////////////////////////////////////////////////////////////////////

  /** Get the formatted results as a string.
   */
  public String toString()

  {
    return buf.toString();

  } // public toString()


  //////////////////////////////////////////////////////////////////////////////

  /** Get the results as a Saxon-compatible Source.
   */
  public Source toSource()

  {
    return new StreamSource( new StringReader(buf.toString()) );

  } // public toSource()


  //////////////////////////////////////////////////////////////////////////////

  /** Get the results as a Saxon NodeInfo.
   */
  public NodeInfo toNode()

  {
    String strVersion = buf.toString();
    StreamSource src = new StreamSource( new StringReader(strVersion) );
    try {
        return TinyBuilder.build( 
            src, new AllElementStripper(), new Configuration() );
    }
    catch( XPathException e ) {
        throw new RuntimeException( e );
    }
    
  } // public toNode()
  

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
  private LinkedList tagStack = new LinkedList();

  /** Current tab level for this thread */
  private int tabCount = 0;
  
  /** Is there currently a begin tag open? */
  private boolean tagStartOpen = false;

  /** Automatically insert blank lines after tags */
  private boolean mBlankLineAfterTag = true;

  /** Amount to indent when {@link #tab()} is called. */
  private int tabSize = defaultTabSize;
}
