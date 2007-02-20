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
 *
 * Acknowledgements:
 *
 * A significant amount of new and/or modified code in this module
 * was made possible by a grant from the Andrew W. Mellon Foundation,
 * as part of the Melvyl Recommender Project.
 */
import java.io.File;
import java.io.IOException;
import javax.xml.transform.Templates;
import org.xml.sax.SAXException;

/**
 * Represents a single source of data for an XTF index. It may contain one or
 * more {@link IndexRecord}s, including its key and prefilters to apply to the
 * records.
 */
public abstract class IndexSource 
{
  /** Obtain the path to the file (or null if it's not a local file) */
  public abstract File path();

  /** Obtain a unique key for this input file */
  public abstract String key();

  /**
   * Obtain set of prefilters to be run, serially in order, on each input
   * record.
   *
   * @return Prefilter stylesheet(s) to run, or null to for none.
   */
  public abstract Templates[] preFilters();

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
  public abstract Templates displayStyle();

  /**
   * Obtain the total size of the source file (used to calculate
   * overall % done). If you don't know, return 1.
   */
  public abstract long totalSize();

  /** Obtain the next record from the file, or null if no more. */
  public abstract IndexRecord nextRecord()
    throws SAXException, IOException;
} // class IndexSource
