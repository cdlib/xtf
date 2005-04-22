package org.cdlib.xtf.xslt;

import java.io.File;

import org.cdlib.xtf.util.Path;

import net.sf.saxon.expr.XPathContext;

/*
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
    String stylesheetPath = 
        context.getOrigin().getInstructionInfo().getSystemId();
    stylesheetPath = stylesheetPath.replaceFirst( "^file:", "" );
    File stylesheetDir  = new File(stylesheetPath).getParentFile();
    String resolved = Path.resolveRelOrAbs( stylesheetDir, filePath );
    boolean result = new File(resolved).canRead();
    return result;
  } // exists()

} // class FileUtils
