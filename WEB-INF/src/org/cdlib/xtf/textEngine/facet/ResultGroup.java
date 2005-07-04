package org.cdlib.xtf.textEngine.facet;

import org.cdlib.xtf.textEngine.DocHit;

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
 * Records the results of a single group in field-grouped query.
 * 
 * @author Martin Haye
 */
public class ResultGroup 
{
  /** Facet value for this group */
  public String value;
  
  /** Ordinal rank of this group */
  public int rank;
  
  /** 
   * Total number of sub-groups (possibly more than are selected by this
   * particular request.)
   */
  public int totalSubGroups;

  /** The selected sub-groups (if any) */
  public ResultGroup[] subGroups;
  
  /** 
   * Total number of documents in this group (possibly many more than are 
   * returned in this particular request.)
   */
  public int totalDocs;

  /** Ordinal rank of the first document hit returned (0-based) */
  public int startDoc;
  
  /** Oridinal rank of the last document hit returned, plus 1 */
  public int endDoc;
  
  /** One hit per document */
  public DocHit[] docHits;
  
} // class ResultGroup
