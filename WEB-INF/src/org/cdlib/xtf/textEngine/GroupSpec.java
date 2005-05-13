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
  
  /** How to sort the groups. Currently "value" and "totalDocs" are the only
   *  permissible values.
   */
  public String sortGroupsBy = "totalDocs";
  
  /** Subset specifications */
  public Subset[] subsets;
  
  /** A target subset of the groups for a single field */
  public static class Subset
  {
      /** Target a specific, named group for counting or reporting. If non-
       *  null, then {@link #maxGroups} should be set to zero.
       */
      public String value;
      
      /** First group to count or report (zero-based.) */
      public int startGroup;
      
      /** Max # of groups to count or report. If non-zero, {@link #value}
       *  should be null.
       */
      public int maxGroups;
      
      /** Rank of first document to report in the selected groups 
       * (zero-based) 
       */
      public int startDoc;
      
      /** Max # of documents to report in the selected groups. Zero means
       *  to only count the documents but not report them.
       */
      public int maxDocs;
      
      /** If {@link #maxDocs} is >= 0, this field specifies which meta-data 
       *  field(s) to sort the documents by. If null, they are sorted in 
       *  descending order by score.
       */
      public String sortDocsBy;

  } // class Subset
  
} // class GroupSpec
