package org.cdlib.xtf.xslt;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.cdlib.xtf.util.Path;

import net.sf.saxon.expr.XPathContext;

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
public class FileUtils {
  
  /** Used to avoid recreating SimpleDateFormat objects all the time */
  private static HashMap dateFormatCache = new HashMap();
  
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
  public static boolean exists( XPathContext context, String filePath )
  {
    File file = resolveFile( context, filePath );
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
  public static String lastModified( XPathContext context, 
                                     String filePath,
                                     String formatStr )
  {
    File file = resolveFile( context, filePath );
    if( !file.canRead() )
        return null;
    
    SimpleDateFormat fmt = getDateFormat( formatStr );
    String result = fmt.format( new Date(file.lastModified()) );
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
  public static long length( XPathContext context, 
                             String filePath )
  {
    File file = resolveFile( context, filePath );
    if( !file.canRead() )
        return -1;
    return file.length();
  } // length()
  
  /**
   * Resolve the location of a file given the stylesheet context.
   */
  private static File resolveFile( XPathContext context, String filePath )
  {
    String stylesheetPath = 
      context.getOrigin().getInstructionInfo().getSystemId();
    stylesheetPath = stylesheetPath.replaceFirst( "^file:", "" );
    File stylesheetDir  = new File(stylesheetPath).getParentFile();

    filePath = filePath.replaceFirst( "^file:", "" );

    String resolved = Path.resolveRelOrAbs( stylesheetDir, filePath );
    return new File(resolved);
  } // resolveFile

  /** 
   * Get a SimpleDateFormatter for the given format string. If one has
   * already been created, use that; otherwise, make a new one.
   * 
   * @param formatStr is the format string to use
   * @return          a SimpleDateFormatter for that format string.
   */
  private static SimpleDateFormat getDateFormat( String formatStr )
  {
    if( !dateFormatCache.containsKey(formatStr) )
        dateFormatCache.put( formatStr, new SimpleDateFormat(formatStr) );
    return (SimpleDateFormat) dateFormatCache.get( formatStr );
  }
  
} // class FileUtils
