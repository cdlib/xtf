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

import java.io.*;

import org.cdlib.xtf.util.Path;
import org.cdlib.xtf.util.Trace;

////////////////////////////////////////////////////////////////////////////////

/** The <code>FileWalker</code> class is a utility class that simplifies 
 *  traversing all the files in a file-system directory, and optionally, in
 *  any sub-directories. <br><br>
 * 
 *  To use this class, create a derived class that implements the abstract
 *  method {@link FileWalker#processFile(String,String,String,String) processFile() }.
 *  Then, create an instance of the derived class and call the 
 *  {@link FileWalker#processFile(String,boolean) processFiles() } method. <br><br> 
 */

public abstract class FileWalker 

{
  
  //////////////////////////////////////////////////////////////////////////////

  /** Process all the files in the specified directory, and optionally in 
   *  all its sub-directories. <br><br>
   * 
   *  This method calls the derived {@link FileWalker#processFile(String,String,String,String) processFile() }
   *  to process any files found. <br><br>
   * 
   *  @param  baseDir  The base directory containing files to process. <br><br>
   * 
   *  @param  subDirs  A flag that indicates whether files in sub-directories 
   *                   should also be processed (<code>true</code>) or not 
   *                   (<code>false</code>). <br><br>
   */
  public void processFiles( String baseDir, boolean subDirs ) 
              throws IOException
  {
    
    try {
        // Convert the specified path to a file equivalent.
        mBasePath = new File( Path.normalizePath( baseDir ) );
    }
    
    // Catch any problems that occurred during conversion.
    catch( Exception e ) {
         Trace.error( "*** FileWalker Exception: " + e.getClass() );
         Trace.error( "            With message: " + e.getMessage() );
         Trace.error( "" );
         return;
    }
    
    // Call the recursive file processing function to do the work.
    mContinueProcessing = true;  
    processFiles( mBasePath, subDirs );
    
  } // public processFiles( String baseDir, boolean subDirs )
  
  //////////////////////////////////////////////////////////////////////////////

  /** File processing function. <br><br>
   * 
   *  This function is called once for every file encountered in the specified
   *  base directory. This function is abstract, and requires a derived class 
   *  to actually implement it. <br><br>
   * 
   *  @param basePath  The base path under which the current file was found.
   *                   This path will end in a forward slash (/) character to
   *                   simplify the construction of a full path/file 
   *                   specification for the current file.<br><br>
   * 
   *  @param subPath   The sub-path (if any) under which the current file was
   *                   found. As with the base path, this sub-path will end in
   *                   a forward slash (/) character to simplify the 
   *                   construction of a full path/file specification for 
   *                   the current file. <br><br>
   * 
   *  @param fileName  The name of the current file (without the extension).
   *                   <br><br>
   * 
   *  @param fileExt   The extension of the current file (if any). If the 
   *                   current file has an extension, then this string will
   *                   begin with a period (.), to simplify the construction 
   *                   of a full path/file specifiecation for the current file.
   *                   <br><br>
   * 
   *  @return          The derived function should return <code> true </code> 
   *                   if file processing should continue, or <code> false </code>
   *                   if file processing shouild stop. 
   */
  protected abstract boolean processFile( String basePath,
                                          String subPath, 
                                          String fileName,
                                          String fileExt );

  //////////////////////////////////////////////////////////////////////////////

  /** Internal recursive directory/file iterating function. <br><br>
   * 
   *  This function is called recursively when a file encountered is a 
   *  directory and sub-directory processing is enabled. <br><br>
   * 
   *  @param theFile  The name of the current file/directory to process.
   *                  <br><br>
   * 
   *  @param subDirs  A flag indicating whether or not sub-directories 
   *                  should be processed. <br><br>
   * 
   *  @.notes         
   *      This method calls itself recursively if sub-directory processing
   *      is enabled with the <code> subDirs </code> parameter. Once an 
   *      actual file is encountered, this method calls the derived 
   *      {@link FileWalker#processFile(String,String,String,String) processFile() }
   *      method to actually perform some work for the file found.
   */
  private void processFiles( File theFile, boolean subDirs ) 
               throws IOException
  {
    
    // If the derived class wants us to stop processing files, return now.
    if( !mContinueProcessing ) return;
    
    // If the file we were passed was in fact a directory...
    if( theFile.isDirectory() ) {
      
        // Process each file in the current directory.
        String[] subFiles = theFile.list();
        for( int i = 0; i < subFiles.length; i++ ) {
            
            // Create a file object for the current file in the list.
            File currFile = new File( theFile, subFiles[i] );
            
            // If the current file is a sub-directory and we're not supposed 
            // to process sub-directories, skip it.
            //
            if( currFile.isDirectory() && !subDirs ) continue;
            
            // Process the current file, which may or may not be a directory.
            processFiles( currFile, subDirs );
        }
        
        // At this point we've processed the entire current directory. So 
        // return now to unwind the current level of recursion.
        //
        return;
    }
      
    // The current file is not a directory, so start by getting the absolute
    // representation of the base path for the current file.
    //
    String basePath = Path.normalizePath( mBasePath.getAbsolutePath() );
    
    // Next, determine the name of the current file.
    String fileName = theFile.getName();

    // Now get the entire path to the file.
    String subPath = Path.normalizeFileName( theFile.getAbsolutePath() );

    // And isolate the sub-path by subtracting out the base path and the name
    // of the file.
    //
    subPath = subPath.substring( basePath.length(), 
                                 subPath.length()-fileName.length() );
                                 
    // Lastly, see if the file name has an extension.
    String fileExt = "";
    int    fileExtIdx = fileName.lastIndexOf( '.' );

    // If it does, separate the file name from the extension.
    if( fileExtIdx > -1 ) {
        fileExt  = fileName.substring( fileExtIdx );
        fileName = fileName.substring( 0, fileExtIdx );   
    }
    
    // One final check: If the file name is empty, but there's an extension, 
    // treat this as a file name with an initial '.' and no extension (for 
    // files like .login)
    //
    if( fileName.length() == 0 && fileExt.length() != 0 ) {
        fileName = fileExt;
        fileExt  = "";
    }
                               
    // Now call the derived file processing method for this file. Also, based
    // on the return value, flag whether we should keep processing files or 
    // not.
    //
    mContinueProcessing = processFile( basePath, subPath, fileName, fileExt );
            
  } // ProcessFiles( File theFile, boolean subDirs )
  
    
  //////////////////////////////////////////////////////////////////////////////

  /** Local copy of the path to the base directory to process (as passed into
   *  {@link FileWalker#processFiles(String,boolean) processFiles() }.
   */
  private File mBasePath = null;

  /** Flag indicating whether file processing should continue or stop (set by
   *  the value returned from the derived 
   *  {@link FileWalker#processFile(String,String,StringString) processFile() }
   *  method.)
   */
  private boolean mContinueProcessing = true;

} // public class FileWalker
