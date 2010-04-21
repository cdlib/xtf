package org.cdlib.xtf.xslt;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.cdlib.xtf.textIndexer.HTMLToString;
import org.cdlib.xtf.util.Path;

import org.xml.sax.InputSource;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.trace.InstructionInfo;
import net.sf.saxon.trans.XPathException;

/*
 * Copyright (c) 2006, Regents of the University of California
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

/*
 * This file created on Apr 21, 2005 by Martin Haye
 */

/**
 * Provides file-related utilities to be called by XSLT stylesheets through
 * Saxon's extension function mechanism.
 *
 * @author Martin Haye
 */
public class FileUtils 
{
  /** Used to avoid recreating SimpleDateFormat objects all the time */
  private static HashMap dateFormatCache = new HashMap();
  
  /** Used to track temp files, per thread */
  private static ThreadLocal<ArrayList<File>> tempFiles =
    new ThreadLocal<ArrayList<File>>();

  /**
   * Checks whether a file with the given path exists (that is, if it can
   * be read.) If the path is relative, it is resolved relative to the
   * stylesheet calling this function.
   *
   * @param context   Context used to figure out which stylesheet is calling
   *                  the function.
   * @param filePath  Path to the file in question
   * @return          true if the file exists and can be read, else false
   */
  public static boolean exists(XPathContext context, String filePath) {
    File file = resolveFile(context, filePath);
    return file.canRead();
  } // exists()

  /**
   * Gets the last-modified time of the file with the given path exists (that
   * is, if it can be read.) If the path is relative, it is resolved relative
   * to the stylesheet calling this function.
   *
   * @param context   Context used to figure out which stylesheet is calling
   *                  the function.
   * @param filePath  Path to the file in question
   * @param formatStr A simple format string; see {@link SimpleDateFormat}.
   * @return          The formatted date/time if the file exists; null if
   *                  the file doesn't exist.
   */
  public static String lastModified(XPathContext context, String filePath,
                                    String formatStr) 
  {
    File file = resolveFile(context, filePath);
    if (!file.canRead())
      return null;

    SimpleDateFormat fmt = getDateFormat(formatStr);
    String result = fmt.format(new Date(file.lastModified()));
    return result;
  } // lastModified()

  /**
   * Gets the size in bytes of the file with the given path (that
   * is, if it can be read.) If the path is relative, it is resolved relative
   * to the stylesheet calling this function.
   *
   * @param context   Context used to figure out which stylesheet is calling
   *                  the function.
   * @param filePath  Path to the file in question
   * @return          The file size, or -1 if it doesn't exist.
   */
  public static long length(XPathContext context, String filePath) {
    File file = resolveFile(context, filePath);
    if (!file.canRead())
      return -1;
    return file.length();
  } // length()

  /**
   * Converts the size of a file to a human-readable string, e.g.
   * "36 Kb", "1.2 Mb", etc. Contributor: Michael A. Russell
   * 
   * @param  longFileSize        The size to convert
   * @return                     Human-readable string approximating that size.
   */
  public static String humanFileSize(Long longFileSize) 
  {
    /* If the input is negative, return a zero-length string.  */
    if (longFileSize < 0) return("");

    /* If it's up to 512, use the number itself.  */
    if (longFileSize < 512) return(longFileSize.toString( ));

    /* Provide a place to put the result of the division.  */
    double doubleBytes;

    /* We want at most two digits to the right of the decimal point.  */
    DecimalFormat outputFormat = new DecimalFormat("0.00");

    /* Provide a place to put the converted value.  */
    StringBuffer outputStrBuf = new StringBuffer( );

    /* Provide a "FieldPosition" object.  It looks like it's returned
     * by the "format( )" method, but I don't really care about it.
     * I haven't been able to find an example of how to use it, so
     * I'll just try using zero, and see what that does.
     */
    FieldPosition fieldPos = new FieldPosition(0);

    /* If it's up to 1024 * 512, express in terms of kilobytes.  */
    if (longFileSize < (1024L * 512L)) {
      doubleBytes = longFileSize.doubleValue( ) / 1024.0;
      outputFormat.format(doubleBytes, outputStrBuf, fieldPos);
      return(outputStrBuf.toString( ) + " Kb");
    }

    /* If it's up to 1024 * 1024 * 512, express in terms of megabytes.  */
    if (longFileSize < (1024L * 1024L * 512L)) {
      doubleBytes = longFileSize.doubleValue( ) / (1024.0 * 1024.0);
      outputFormat.format(doubleBytes, outputStrBuf, fieldPos);
      return(outputStrBuf.toString( ) + " Mb");
    }

    /* If it's up to 1024 * 1024 * 1024 * 512, express in terms of
     * gigabytes.
     */
    if (longFileSize < (1024L * 1024L * 1024L * 512L)) {
      doubleBytes = longFileSize.doubleValue( ) / (1024.0 * 1024.0 *
          1024.0);
      outputFormat.format(doubleBytes, outputStrBuf, fieldPos);
      return(outputStrBuf.toString( ) + " Gb");
    }

    /* If it's up to 1024 * 1024 * 1024 * 1024 * 512, express in terms of
     * terabytes.
     */
    if (longFileSize < (1024L * 1024L * 1024L * 1024L * 512L)) {
      doubleBytes = longFileSize.doubleValue( ) / (1024.0 * 1024.0 *
          1024.0 * 1024.0);
      outputFormat.format(doubleBytes, outputStrBuf, fieldPos);
      return(outputStrBuf.toString( ) + " Tb");
    }

    /* If it's up to 1024 * 1024 * 1024 * 1024 * 1024 * 512, express in
     * terms of petabytes.
     */
    if (longFileSize < (1024L * 1024L * 1024L * 1024L * 1024L * 512L)) {
      doubleBytes = longFileSize.doubleValue( ) / (1024.0 * 1024.0 *
          1024.0 * 1024.0 * 1024.0);
      outputFormat.format(doubleBytes, outputStrBuf, fieldPos);
      return(outputStrBuf.toString( ) + " Pb");
    }

    /* Express in exabytes.  A long integer can be at most 2**63 - 1,
     * and that's about 9 exabytes, so we don't need to go higher.
     */
    doubleBytes = longFileSize.doubleValue( ) / (1024.0 * 1024.0 *
            1024.0 * 1024.0 * 1024.0 * 1024.0);
    outputFormat.format(doubleBytes, outputStrBuf, fieldPos);
    return(outputStrBuf.toString( ) + " Eb");
  }
  
  /**
   * Calculate the MD5 digest of a string. Contributor: Michael A. Russell
   * 
   * @param inputString         String to digest
   * @return                    The string's MD5 hash
   */
  public static String md5Hash(String inputString) 
  {
    /* Get an md5 message digest object.  */
    MessageDigest msgDigest;
    try {
      msgDigest = MessageDigest.getInstance("MD5");
    }
    catch (Exception e) {
      /* NoSuchAlgorithmException probably.  Return a zero-length
       * string.
       */
      return("");
    }

    /* Calculate the md5 digest for the input string.  */
    msgDigest.update(inputString.getBytes( ), 0, inputString.length( ));

    /* Get a BigInteger version of the md5 digest.  */
    BigInteger bigInt;
    try {
      bigInt = new BigInteger(1, msgDigest.digest( ));
    }
    catch (Exception e) {
      /* NumberFormatException probably.  Return a zero-length string.  */
      return("");
    }

    /* Convert the BigInteger to a hex string.  */
    String outputString = bigInt.toString(16);

    /* If the number of characters is odd, then prefix a zero.  */
    if (outputString.length( ) % 2 == 1)
      outputString = "0" + outputString;

    /* Return the result.  */
    return(outputString);
  }

  /**
   * Unfortunately the interface for getting systemId from an XPath context changed
   * between Saxon 9.0 and Saxon 9.1, so we jump through hoops to be compatible
   * with both.
   */
  private static String getSystemId(XPathContext context)
  {
    try {
      // Saxon 9.0 and below
      return context.getOrigin().getInstructionInfo().getSystemId();
    }
    catch (NoSuchMethodError e)
    {
      // Saxon 9.1 and above
      for (Method method : context.getClass().getMethods()) {
        if (method.getName().equals("getOrigin")) 
        {
          try {
            return ((InstructionInfo)method.invoke(context)).getSystemId();
          } catch (Exception e2) {
            throw new RuntimeException(e2);
          }
        }
      }
    }
    return null;
  }

  
  /**
   * Resolve the location of a file given the stylesheet context.
   */
  public static File resolveFile(XPathContext context, String filePath) 
  {
    String stylesheetPath = getSystemId(context);
    stylesheetPath = stylesheetPath.replaceFirst("^file:", "");
    stylesheetPath = stylesheetPath.replaceAll("%20", " "); // fix spaces from Saxon on Windows
    File stylesheetDir = new File(stylesheetPath).getParentFile();

    filePath = filePath.replaceFirst("^file:", "");
    filePath = filePath.replaceAll("%20", " "); // fix spaces from Saxon on Windows

    String resolved = Path.resolveRelOrAbs(stylesheetDir, filePath);
    return new File(resolved);
  } // resolveFile
  
  /**
   * Resolve the location of a file given the stylesheet context. If the
   * path is absolute, nothing is done. If it is relative, it is converted
   * to absolute by resolving it relative to the stylesheet path.
   */
  public static String resolvePath(XPathContext context, String filePath)
  {
    String stylesheetPath = getSystemId(context);
    stylesheetPath = stylesheetPath.replaceFirst("^file:", "");
    stylesheetPath = stylesheetPath.replaceAll("%20", " "); // fix spaces from Saxon on Windows
    File stylesheetDir = new File(stylesheetPath).getParentFile();

    filePath = filePath.replaceFirst("^file:", "");
    filePath = filePath.replaceAll("%20", " "); // fix spaces from Saxon on Windows

    String resolved = Path.resolveRelOrAbs(stylesheetDir, filePath);
    return Path.normalize(resolved);
  }

  /**
   * Gets the current date and time.
   *
   * @param context   Context used to figure out which stylesheet is calling
   *                  the function.
   * @param formatStr A simple format string; see {@link SimpleDateFormat}.
   * @return          The formatted date/time.
   */
  public static String curDateTime(XPathContext context, String formatStr) {
    SimpleDateFormat fmt = getDateFormat(formatStr);
    String result = fmt.format(new Date());
    return result;
  } // curDateTime()
  
  /**
   * All minutes have this many milliseconds except the last minute of the day on a day defined with
   * a leap second.
   */
  private static final long MILLISECS_PER_MINUTE = 60*1000;
  
  /**
   * Number of milliseconds per hour, except when a leap second is inserted.
   */
  private static final long MILLISECS_PER_HOUR   = 60*MILLISECS_PER_MINUTE;
  
  /**
   * Number of leap seconds per day except on 
   * <BR/>1. days when a leap second has been inserted, e.g. 1999 JAN  1.
   * <BR/>2. Daylight-savings "spring forward" or "fall back" days.
   */
  private static final long MILLISECS_PER_DAY = 24*MILLISECS_PER_HOUR;

  /**
   * Compute the number of days, hours, or minutes that have elapsed between
   * the given time and now.
   * 
   * @param context     Context used to figure out which stylesheet is calling
   *                    the function.
   * @param targetDateStr The target date
   * @param units       Units to return: 'days', 'hours', or 'minutes'. Plural
   *                    is optional, and single-letter abbreviations are accepted.
   * @param formatStr   The format of the target date; see {@link SimpleDateFormat}.
   * @return number of days, hours or minutes elapsed
   */
  public static long timeSince(XPathContext context, 
                               String targetDateStr, String units, String formatStr) 
  {
    try {
      // First, parse the target time.
      SimpleDateFormat fmt = getDateFormat(formatStr);
      Date targetDate = fmt.parse(targetDateStr);
      Calendar tmpCal = Calendar.getInstance();
      tmpCal.setTime(targetDate);
      long targetMillis = adjustedMillis(tmpCal);
      
      // Now get the current time for comparison
      tmpCal.setTime(new Date());
      long currentMillis = adjustedMillis(tmpCal);
      long diff = currentMillis - targetMillis;
      
      // Compute the answer in the desired units.
      if (units.matches("d|D|day|Day|days|Days"))
        return diff / MILLISECS_PER_DAY;
      else if (units.matches("h|H|hour|Hour|hours|Hours"))
        return diff / MILLISECS_PER_HOUR;
      else if (units.matches("m|M|min|Min|minute|Minute|minutes|Minutes"))
        return diff / MILLISECS_PER_MINUTE;
      else
        throw new RuntimeException("timeSince units must be days, hours, or minutes (or d/h/m)");
    }
    catch (ParseException e) {
      throw new RuntimeException("error parsing date '" + targetDateStr + "'");
    }
  }
  
  /**
   * Gets the time in milliseconds from a Calendar, adjusting for timezone
   * so that day subtraction works properly.
   */
  private static long adjustedMillis(Calendar cal) {
    return cal.getTimeInMillis() +  cal.getTimeZone().getOffset(cal.getTimeInMillis() );
  }

  /**
   * Get a SimpleDateFormatter for the given format string. If one has
   * already been created, use that; otherwise, make a new one.
   *
   * @param formatStr is the format string to use
   * @return          a SimpleDateFormatter for that format string.
   */
  private static SimpleDateFormat getDateFormat(String formatStr) {
    if (!dateFormatCache.containsKey(formatStr))
      dateFormatCache.put(formatStr, new SimpleDateFormat(formatStr));
    return (SimpleDateFormat)dateFormatCache.get(formatStr);
  }
  
  /**
   * Generates a temporary file in the default temporary-file directory,
   * using the given prefix and suffix to generate the name. Also registers
   * the file for deletion at the end of the current request.
   *
   * @param context   Context used to figure out which stylesheet is calling
   *                  the function.
   * @param prefix    Prefix for the resulting file name.
   * @param suffix    Suffix for the resulting file name.
   * @return          The new temporary file name.
   */
  public static String createTempFile(XPathContext context, 
                                      String prefix, String suffix)
    throws IOException
  {
    File out = File.createTempFile(prefix, suffix);
    out.delete();
    ArrayList<File> files = tempFiles.get();
    if (files == null) {
      files = new ArrayList<File>();
      tempFiles.set(files);
    }
    tempFiles.get().add(out);
    return out.getAbsolutePath();
  }

  /**
   * Deletes all temporary files created by the current thread using
   * {@link #createTempFile}.
   */
  public static void deleteTempFiles()
  {
    ArrayList<File> files = tempFiles.get();
    if (files != null) {
      for (File f : files) {
        if (f.delete())
          files.remove(f);
      }
    }
  }
  
  /**
   * Reads in the first part of an XML file, stopping at the first
   * close-element marker. Generally this captures enough information to
   * identify which kind of XML data is inside the file.
   * @throws IOException if the file can't be read
   * @throws XPathException if the document cannot be parsed
   */
  public static DocumentInfo readXMLStub(XPathContext context, String filePath)
    throws IOException, XPathException
  {
    // First, locate the file
    File file = resolveFile(context, filePath);
    if (!file.canRead())
      throw new IOException("Cannot read file '" + file.toString() + "'");
    
    // Now read it in, up to the first close-element marker.
    XMLStubReader xmlReader = new XMLStubReader();
    BufferedInputStream bufStream = new BufferedInputStream(new FileInputStream(file));
    InputSource inputSrc = new InputSource(bufStream);
    inputSrc.setSystemId(file.toURI().toString());
    Source saxSrc = new SAXSource(xmlReader, inputSrc);
    DocumentInfo doc = context.getConfiguration().buildDocument(saxSrc);
    return doc;
  }
  
  /**
   * Reads in an HTML page (specified by URL), and uses JTidy to make it into
   * XML that can be subsequently processed by a stylesheet.
   * 
   * @throws IOException if the file can't be read
   * @throws XPathException if the document cannot be parsed
   */
  public static DocumentInfo readHTMLPage(XPathContext context, String urlStr)
    throws IOException, XPathException
  {
    // Read the HTML page, and convert it to an XML string
    URL url;
    URLConnection connection;
    InputStream inStream = null;
    String pageStr;
    try {
      url = new URL(urlStr);
      connection = url.openConnection();
      inStream = connection.getInputStream();
      pageStr = HTMLToString.convert(inStream);
    }
    finally {
      if (inStream != null)
        inStream.close();
    }
    
    // And convert that string to an in-memory XML document.
    DocumentInfo doc = context.getConfiguration().buildDocument(
        new StreamSource(new StringReader(pageStr), urlStr));
    return doc;
  }
  
} // class FileUtils
