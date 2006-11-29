package org.cdlib.xtf.textEngine;

import java.util.Set;

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

/**
 * Various parameters that affect spell-checking of query terms.
 */
public class SpellcheckParams 
{
  /** Maximum number of suggestions to provide per term. Required. */
  public int suggestionsPerTerm;
  
  /** 
   * Fields to scan in the query for possibly misspelled terms. If null,
   * all tokenized fields are considered.
   */
  public Set fields = null;
  
  /** 
   * Document score cutoff. If any document's non-normalized score is higher
   * than this, no suggestions will be made.
   */
  public float docScoreCutoff = 0;
  
  /** 
   * Total documents cutoff. If the query results in more document hits than
   * this, no suggestions will be made.
   */
  public int totalDocsCutoff = 10;
  
  /**
   * Term occurrence factor. If this is greater than zero, suggested terms
   * must occur more frequently than the original term. For instance, if the
   * factor is set to 2.0, suggested terms must occur at least twice as
   * frequently as the original term.
   */
  public float termOccurrenceFactor = 2.0f;
  
  /**
   * Edit-distance accuracy. A score is calculated based on the "edit distance"
   * between the original term and each suggested term. If that score falls
   * below this accuracy threshhold, the suggested term is removed from
   * consideration. A value of 0.5 is generally pretty good. Lower values
   * will allow looser matches; higher values restrict to closer matches.
   */
  public float accuracy = 0.5f;
  
} // class SpellcheckParams
