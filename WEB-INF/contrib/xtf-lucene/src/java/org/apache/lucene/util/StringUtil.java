package org.apache.lucene.util;

/*
 * Copyright 2006-2007 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.regex.Pattern;

/** 
 * Provides some handy utilities missing from the Java String class, such as
 * splitting on spaces, and joining with spaces, as well as case mapping. 
 * 
 * @author Martin Haye
 */
public class StringUtil 
{
  /** Used for splitting strings on spaces */
  private static final Pattern spacePat = Pattern.compile("\\s+");

  /**
   * Join a number of strings (or other objects) into a single
   * string, separated by spaces. Each object's toString() method will be used.
   *
   * @param in array of Strings or objects to join
   * @return the joined string
   */
  public static String join(Object[] in) {
    return join(in, " ");
  }

  /**
   * Join a number of strings (or other objects) into a single
   * string. Each object's toString() method will be used.
   *
   * @param in array of Strings or objects to join
   * @param separator a string to put between them
   * @return the joined string
   */
  public static String join(Object[] in, String separator) 
  {
    if (in == null)
      return null;
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < in.length; i++) {
      if (in[i] == null)
        continue;
      if (i > 0)
        buf.append(separator);
      buf.append(in[i].toString());
    }
    return buf.toString();
  }

  /** Pad the end of a string with spaces to make its final length >= len */
  public static String padEnd(String in, int len) {
    return padEnd(in, len, ' ');
  }

  /** Same as {@link #padEnd(String, int)} */
  public static String justifyLeft(String in, int len) {
    return padEnd(in, len, ' ');
  }

  /**
   * Pad the end of a string with the given character to make its final
   * length >= len
   */
  public static String padEnd(String in, int len, char padChar) {
    if (in.length() >= len)
      return in;
    StringBuffer buf = new StringBuffer(len);
    buf.append(in);
    for (int i = 0; i < len - in.length(); i++)
      buf.append(padChar);
    return buf.toString();
  }

  /** Pad the start of a string with spaces to make its final length >= len */
  public static String padStart(String in, int len) {
    return padStart(in, len, ' ');
  }

  /** Same as {@link #padStart(String, int)} */
  public static String justifyRight(String in, int len) {
    return padStart(in, len, ' ');
  }

  /**
   * Pad the start of a string with the given character to make its final
   * length >= len
   */
  public static String padStart(String in, int len, char padChar) {
    if (in.length() >= len)
      return in;
    StringBuffer buf = new StringBuffer(len);
    for (int i = 0; i < len - in.length(); i++)
      buf.append(padChar);
    buf.append(in);
    return buf.toString();
  }

  /**
   * Examines the pattern string to see whether it's lowercase, uppercase,
   * or title case, and then applies that case to the given input string.
   * If the pattern doesn't match any of the categories, we return the
   * input string unchanged.
   *
   * @param pattern string to examine for case
   * @param in string to convert to the same case as 'pattern'
   * @return resulting converted form of 'in'
   */
  public static String copyCase(String pattern, String in) {
    if (isLowerCase(pattern))
      return in.toLowerCase();
    if (isUpperCase(pattern))
      return in.toUpperCase();
    if (isTitleCase(pattern))
      return toTitleCase(in);
    return in;
  }

  /** Check if the given string is all upper-case */
  public static boolean isUpperCase(String in) {
    if (in == null || in.length() == 0)
      return false;
    for (int i=0; i<in.length(); i++) {
      if (!Character.isUpperCase(in.charAt(i)))
        return false;
    }
    return true;
  }

  /** Check if the given string is all lower-case */
  public static boolean isLowerCase(String in) {
    if (in == null || in.length() == 0)
      return false;
    for (int i=0; i<in.length(); i++) {
      if (!Character.isLowerCase(in.charAt(i)))
        return false;
    }
    return true;
  }

  /**
   * Checks if the given string is "title case", i.e. the first letter is
   * uppercase and the rest are lower case. If the string has multiple words,
   * checks if *each* word is title case.
   */
  public static boolean isTitleCase(String in) 
  {
    // Basic punts.
    if (in == null || in.length() == 0)
      return false;
    
    // Break the string up into words
    String[] words = splitWords(in);

    // If only one word, check its title-caseness
    if (words.length == 1) {
      String word = words[0];
      if (word.length() <= 1)
        return false;
      return Character.isUpperCase(word.charAt(0)) &&
             isLowerCase(word.substring(1));
    }

    // Consider each word in turn. They must all be title-case.
    for (int i = 0; i < words.length; i++) {
      if (!isTitleCase(words[i]))
        return false;
    }
    return true;
  }

  /**
   * Convert a string to "title case", i.e. making the first letter of each
   * word uppercase, and the rest of the letters lowercase.
   */
  public static String toTitleCase(String in) 
  {
    // See if there's more than one word.
    String[] words = splitWords(in);

    // If no words, there's nothing to do.
    if (words.length == 0)
      return in;

    // If only one word, convert it.
    if (words.length == 1) {
      String word = words[0];
      if (word.length() < 1)
        return word;
      return Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
    }

    // If a series of words, convert them all, then join.
    String[] converted = new String[words.length];
    for (int i = 0; i < words.length; i++)
      converted[i] = toTitleCase(words[i]);
    return join(converted);
  }

  /**
   * Break a string up into words, defined by whitespace boundaries.
   *
   * @param in a string to break up
   * @return an array of the words in the string
   */
  public static String[] splitWords(String in) {
    if (in == null)
      return new String[0];
    String trimmed = in.trim();
    if (trimmed.length() == 0)
      return new String[0];
    return spacePat.split(trimmed);
  }
  
  /**
   * Replaces 'special' HTML characters with their corresponding character
   * entity references. For instance, '&lt;' is replaced by '&amp;lt;'.
   * 
   * @param in  The string to work on
   * @return    A modified version with special characters replaced
   */
  public static String escapeHTMLChars(String in) {
    StringBuffer buf = new StringBuffer(in.length());
    for (int i=0; i<in.length(); i++) {
      char c = in.charAt(i);

      // Characters 0-31 (except 9, 11, and 13) are not legal in HTML
      // or XLM, so filter them out.
      if (c >= 0 && c <= 31 && c != 9 && c != 11 && c != 13)
        continue;
      
      switch (c) 
      {
        // Translate special characters to known HTML entities     
        case '<':  buf.append("&lt;");    break;
        case '>':  buf.append("&gt;");    break;
        case '&':  buf.append("&amp;");   break;
        case '\'': buf.append("&apos;");  break;
        case '\"': buf.append("&quot;");  break;
        case '\n': buf.append("<br/>\n"); break;
        
        // Characters 128-159 are not legal in HTML (nor generally in Unicode).
        // We have no way of knowing where they came from, so we make a 
        // reasonable guess and treat them as ANSI. The equivalency below comes
        // from http://www.alanwood.net/demos/ansi.html, accessed on 
        // 30-Oct-2007.
        //
        case 128:  buf.append("\u20ac");  break;
        case 129:  buf.append("\u0081");  break;
        case 130:  buf.append("\u201A");  break;
        case 131:  buf.append("\u0192");  break;
        case 132:  buf.append("\u201E");  break;
        case 133:  buf.append("\u2026");  break;
        case 134:  buf.append("\u2020");  break;
        case 135:  buf.append("\u2021");  break;
        case 136:  buf.append("\u02C6");  break;
        case 137:  buf.append("\u2030");  break;
        case 138:  buf.append("\u0160");  break;
        case 139:  buf.append("\u2039");  break;
        case 140:  buf.append("\u0152");  break;
        case 141:  buf.append("\u008D");  break;
        case 142:  buf.append("\u017D");  break;
        case 143:  buf.append("\u008F");  break;
        case 144:  buf.append("\u0090");  break;
        case 145:  buf.append("\u2018");  break;
        case 146:  buf.append("\u2019");  break;
        case 147:  buf.append("\u201C");  break;
        case 148:  buf.append("\u201D");  break;
        case 149:  buf.append("\u2022");  break;
        case 150:  buf.append("\u2013");  break;
        case 151:  buf.append("\u2014");  break;
        case 152:  buf.append("\u02DC");  break;
        case 153:  buf.append("\u2122");  break;
        case 154:  buf.append("\u0161");  break;
        case 155:  buf.append("\u203A");  break;
        case 156:  buf.append("\u0153");  break;
        case 157:  buf.append("\u009D");  break;
        case 158:  buf.append("\u017E");  break;
        case 159:  buf.append("\u0178");  break;
        
        // All other characters need no translation.
        default:
          buf.append(c);
          break;
      }
    }
    return buf.toString();
  }
}
