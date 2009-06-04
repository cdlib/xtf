package org.cdlib.xtf.xslt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

import org.cdlib.xtf.util.CharMap;

import net.sf.saxon.expr.XPathContext;

/*
 * Copyright (c) 2009, Regents of the University of California
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

/*
 * This file created on Jun 4, 2009 by Martin Haye
 */

/**
 * Provides Unicode character-related utilities to be called by XSLT 
 * stylesheets through Saxon's extension function mechanism.
 *
 * @author Martin Haye
 */
public class CharUtils 
{
  /** Used to avoid recreating AccentMap objects all the time */
  private static HashMap<String,CharMap> accentMapCache = new HashMap();

  /** 
   * Get the accent map corresponding to a file. 
   * 
   * @throws IOException if we can't read the file.
   */
  private static CharMap getAccentMap(XPathContext context, String filePath) 
    throws IOException
  {
    synchronized (accentMapCache) 
    {
      // First resolve relative path name.
      String fullPath = FileUtils.resolvePath(context, filePath);
      File file = new File(fullPath);
      
      // Make sure we can read the file.
      if (!file.canRead())
        throw new IOException("Error reading accent map file '" + fullPath + "'");
      
      // Do we already have this version of this file loaded?
      String key = fullPath + "|" + file.lastModified();
      if (accentMapCache.containsKey(key))
        return accentMapCache.get(key);
      
      // Okay, we need to read and cache it.
      InputStream stream = new FileInputStream(file);
      if (fullPath.endsWith(".gz"))
        stream = new GZIPInputStream(stream);
      CharMap map = new CharMap(stream);
      accentMapCache.put(key, map);
      return map;
    }
  }
  
  /**
   * Applies an accent map to a string, normalizing spaces in the process.
   * This function is typically used to remove diacritic marks from 
   * alphabetic characters. The accent map is read
   * from the file with the given path. If the path is relative, it is 
   * resolved relative to the stylesheet calling this function. Note that
   * the accent map is cached in memory so it doesn't need to be
   * repeatedly read.
   *
   * @param context   Context used to figure out which stylesheet is calling
   *                  the function.
   * @param filePath  Path to the accent map file in question (typically
   *                  leading to conf/accentFolding/accentMap.txt)
   * @param str       The string whose characters should be mapped.
   * @return          A new string with its characters mapped.
   */
  public static String applyAccentMap(XPathContext context, 
                                      String filePath,
                                      String str) 
    throws IOException
  {
    // First read in (or get cached) accent map file.
    CharMap accentMap;
    accentMap = getAccentMap(context, filePath);
    
    // Then apply it to each word.
    StringBuilder buf = new StringBuilder();
    for (String word : str.split("\\s")) {
      if (word.length() == 0)
        continue;
      String mappedWord = accentMap.mapWord(word);
      if (mappedWord != null)
        word = mappedWord;
      if (buf.length() > 0)
        buf.append(' ');
      buf.append(word);
    }
    
    // Return the result joined by spaces.
    return buf.toString();
  }
  
} // class CharUtils
