package org.cdlib.xtf.textEngine;

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

/**
 * Holds global constants for the XTF text system.
 * 
 * @author Martin Haye
 */
public class Constants 
{

  /** The character used to mark the start/end of a special bump token. */ 
  public static final char BUMP_MARKER = '\uEBBB';
  
  /** The special marker used to track the location of nodes within 
   *  a chunk of text to be indexed.
   */
  public static final char NODE_MARKER = '\uE90D';
  
  /** The string used to represent a virtual word in a chunk of text. This
   *  string is chosen in such a way to be an unlikely combination of 
   *  characters in typical western texts. Initially, the characters <b>qw</b>
   *  were selected as a mnemonic for a "quiet word".
   */ 
  public  static final String VIRTUAL_WORD = "qw";
  
  // Special token that marks the start of a field
  public static final char FIELD_START_MARKER = '\uEBEB';
  
  // Special token that marks the end of a field
  public static final char FIELD_END_MARKER   = '\uEE1D';

} // class XtfConstants
