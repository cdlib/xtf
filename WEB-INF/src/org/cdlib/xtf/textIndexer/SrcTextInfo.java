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

import javax.xml.transform.Templates;

import org.xml.sax.InputSource;

/**
 * Simple data holder representing a single file to index, including where to
 * find it, its format, the input filter to use, and the index key.
 */
public class SrcTextInfo 
{  
  /** Source to read XML/PDF/HTML/etc. data from */
  public InputSource source;
  
  /** Format of the input data: must be "XML", "PDF", "HTML", or "Text" */
  public String format;
  
  /** Key used to identify this file in the index */
  public String key;
  
  /** XSLT pre-filter used to massage the XML document (null for none) */
  public Templates inputFilter;

  /** Stylesheet from which to gather XSLT key definitions to be computed
   *  and cached on disk. Typically, one would use the actual display 
   *  stylesheet for this purpose, guaranteeing that all of its keys will be 
   *  pre-cached.<br><br>
   * 
   *  Background: stylesheet processing can be optimized by using XSLT 'keys', 
   *  which are declared with an &lt;xsl:key&gt; tag. The first time a key 
   *  is used in a given source document, it must be calculated and its values 
   *  stored on disk. The text indexer can optionally pre-compute the keys so 
   *  they need not be calculated later during the display process.
   */
  public Templates displayStyle;
  
} // class SrcTextInfo