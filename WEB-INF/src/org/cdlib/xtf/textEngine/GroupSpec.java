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
 * Stores a grouping specification, as part of a {@link QueryRequest}.
 * 
 * @author Martin Haye
 */
public class GroupSpec 
{
  /** Name of the meta-data field to group by */
  public String field;
  
  /** How to sort the groups. Currently "value" and "count" are the only
   *  permissible values.
   */
  public String sortGroupsBy = "count";
  
  /** Set this non-null to expand the named group. If null, none of the 
   *  groups will be expanded. A special value, {@link #EXPAND_FIRST}, is 
   *  recognized to mean that the first group in sort order should be 
   *  expanded. 
   */ 
  public String expandValue;
  
  /** Special string allowed in {@link #expandValue} field to denote that
   *  the first group, in sort order, should be expanded.
   */
  public static final String EXPAND_FIRST = "#first";
  
  /** If {@link #expandValue} is non-null, this field specifies which 
   *  meta-data field(s) to sort the documents by. If null, they are sorted
   *  in descending order by score.
   */
  public String sortDocsBy;
  
  /** If {@link #expandValue} is non-null, this field specifies the
   *  first document hit to return (zero-based) 
   */
  public int startDoc = 0;
  
  /** If {@link #expandValue} is non-null, this field specifies the 
   *  max # of documents to return
   */
  public int maxDocs = 10;
  
} // class GroupSpec
