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
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

class QuerySpeller extends QueryParser
{
  /** Used to get spelling suggestions */
  private SpellReader spellReader;
  
  /** List of terms collected during parse */
  private ArrayList<String> terms;
  
  /** Parsing can be in one of three different modes */
  private enum Mode { NORMAL, COLLECT_TERMS, REPLACE_TERMS };
  
  /** The parsing mode we're currently in */
  private Mode mode;
  
  /** Replacements to be made */
  private HashMap<String, String> suggestMap;
  
    
  /** 
   * Construct a new speller using a given dictionary and Lucene query
   * parser.
   *
   * @param field       the default field for query terms.
   * @param analyzer    used to find tokenize in the query text.
   * @param spellReader source for spelling suggestions -- see
   *                    {@link SpellReader#open(File)}.
   */
  public QuerySpeller(String field, Analyzer analyzer, SpellReader spellReader)
  {
    super(field, analyzer);
    this.spellReader = spellReader;
  }
  
  /** Parse a query as normal, with no spelling correction */
  public synchronized Query parse(String query) 
    throws ParseException
  {
    mode = Mode.NORMAL;
    return super.parse(query);
  }
  
  /**
   * Suggest alternate spellings for terms in a Lucene query.
   * 
   * @param     inQuery the original query to scan
   * @return    an query with some suggested spelling corrections, or
   *            null if no suggestions could be found. 
   */
  public synchronized String suggest(String inQuery) 
    throws ParseException, IOException
  {
    // Okay, parse the query once to get a list of the terms in it.
    terms = new ArrayList<String>();
    mode = Mode.COLLECT_TERMS;
    super.parse(inQuery);
    
    // No terms found? Then we can't make a suggestion.
    if (terms.isEmpty())
      return null;
    
    // Get some suggestions for these terms. If none found, we're outta here.
    String[] suggTerms = spellReader.suggestKeywords(terms.toArray(new String[0]));
    if (suggTerms == null)
      return null;
    
    // Make a mapping of the suggestions.
    suggestMap = new HashMap<String, String>();
    for (int i=0; i<terms.size(); i++)
      suggestMap.put(terms.get(i), suggTerms[i]);
    
    // Replace the terms and we're done.
    mode = Mode.REPLACE_TERMS;
    Query outQuery = super.parse(inQuery);
    return outQuery.toString();
  }
  
  /** This is the way we slip in and grab term queries */
  protected Query getFieldQuery(String field, String queryText) 
    throws ParseException
  {
    Query ret = super.getFieldQuery(field, queryText);
    
    // In normal mode, just return whatever we got.
    if (mode == Mode.NORMAL)
      return ret;
    
    // In collection mode, accumulate a list of terms found.
    if (mode == Mode.COLLECT_TERMS) {
      collectTerms(ret);
      return ret;
    }
    
    // In replacement mode, replace terms if possible.
    return replaceTerms(ret);
  }
  
  /** Collect terms if possible from a Lucene query */
  protected void collectTerms(Query q)
  {
    if (q instanceof TermQuery)
      collectTerms((TermQuery)q);
    else if (q instanceof PhraseQuery)
      collectTerms((PhraseQuery)q);
  }
  
  /** Collect terms from a normal term query */
  protected void collectTerms(TermQuery tq) {
    terms.add(tq.getTerm().text());
  }
  
  /** Collect terms from a phrase query */
  protected void collectTerms(PhraseQuery pq) {
    for (Term t : pq.getTerms())
      terms.add(t.text());
  }
  
  /** Replace terms if possible within a Lucene query */
  protected Query replaceTerms(Query q)
  {
    if (q instanceof TermQuery)
      return replaceTerms((TermQuery)q);
    else if (q instanceof PhraseQuery)
      return replaceTerms((PhraseQuery)q);
    else
      return q;
  }
  
  /** Replace terms within a normal term query */
  protected Query replaceTerms(TermQuery tq) 
  {
    // See if there's a suggestion.
    String sugg = suggestMap.get(tq.getTerm().text());
    if (sugg == null)
      return tq;
    
    // 
    Term newTerm = new Term(tq.getTerm().field(),
                            suggestTerm(tq.getTerm().text()));
    TermQuery newQ = new TermQuery(newTerm);
    newQ.setBoost(tq.getBoost());
    return newQ;
  }
  
  /** Replace terms within a phrase query */
  protected Query replaceTerms(PhraseQuery pq) 
  {
    //String[] oldTerms = pq.getTerms();
    //ArrayList<String> newTerms
    return null;
  }
  
  /** Look up a suggested term, or return original if none */
  private String suggestTerm(String term)
  {
    String found = suggestMap.get(term);
    if (found != null)
      return found;
    return term;
  }
}
