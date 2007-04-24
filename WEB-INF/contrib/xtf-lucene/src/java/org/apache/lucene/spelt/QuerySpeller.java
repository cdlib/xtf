package org.apache.lucene.spelt;

/**
 * Copyright 2007 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;

import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Query;

class QuerySpeller extends SimpleQueryRewriter
{
  /** Used to get spelling suggestions */
  private SpellReader spellReader;
  
  /** Set of fields we're allowed to collect terms for */
  private HashSet<String> fieldSet;
  
  /** List of terms collected */
  private LinkedHashSet<String> terms;
  
  /** Mapping of terms to replace */
  private HashMap<String, String> suggestMap;
      
  /** 
   * Construct a new speller using a given dictionary reader.
   *
   * @param spellReader source for spelling suggestions -- see
   *                    {@link SpellReader#open(File)}.
   */
  public QuerySpeller(SpellReader spellReader)
  {
    this.spellReader = spellReader;
  }
  
  /**
   * Suggest alternate spellings for terms in a Lucene query. By default,
   * we consider terms in any field. If you need to specify a subset of fields 
   * to consider, use the 
   * {@linkplain #suggest(Query, String[]) alternate method} below.
   * 
   * @param     inQuery the original query to scan
   * @return    an query with some suggested spelling corrections, or
   *            null if no suggestions could be found. 
   */
  public synchronized Query suggest(Query inQuery) 
    throws ParseException, IOException
  {
    return suggest(inQuery, null);
  }
  
  /**
   * Suggest alternate spellings for terms in a Lucene query.
   * 
   * @param     inQuery the original query to scan
   * @param     fields to consider for correction, or null for all
   * @return    a query with some suggested spelling corrections, or
   *            null if no suggestions could be found. 
   */
  public synchronized Query suggest(Query inQuery, String[] fields) 
    throws ParseException, IOException
  {
    // Record the set of fields to consider.
    if (fields == null)
      fieldSet = null;
    else {
      fieldSet = new HashSet(fields.length);
      for (String f : fields)
        fieldSet.add(f);
    }
      
    // Okay, traverse the query once, but don't make any changes.
    suggestMap = new HashMap<String, String>();
    rewriteQuery(inQuery);
    
    // No terms found? Then we can't make a suggestion.
    if (terms.isEmpty())
      return null;
    
    // Get some suggestions for these terms. If none found, we're outta here.
    String[] oldTerms = terms.toArray(new String[0]);
    String[] suggTerms = spellReader.suggestKeywords(oldTerms);
    if (suggTerms == null)
      return null;
    
    // Make a mapping of the suggestions.
    for (int i=0; i<oldTerms.length; i++)
      suggestMap.put(oldTerms[i], suggTerms[i]);
    
    // Rewrite the query, replacing the suggested words.
    return rewriteQuery(inQuery);
  }
  
  /** This is the way we slip in to grab or rewrite terms */
  @Override
  protected Term rewrite(Term t) 
  {
    // Skip fields we're not supposed to look at.
    if (fieldSet == null || !fieldSet.contains(t.field()))
      return t;
    
    // Add this term to our accumulating list (if it's not already there)
    String text = t.text();
    terms.add(text);
    
    // If there's a suggestion, implement it.
    String suggText = suggestMap.get(text);
    if (suggText != null)
      return new Term(suggText, t.field());
    else
      return t;
  }
}
