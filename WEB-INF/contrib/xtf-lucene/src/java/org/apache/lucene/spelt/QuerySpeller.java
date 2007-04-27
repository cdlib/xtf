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
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;

/**
 * Handles spelling correction for simple queries produced by the Lucene
 * {@link QueryParser}. Allows a custom {@link QueryParser} to be supplied,
 * though it must retain the case of the input tokens, so that we can supply
 * spelling corrections using the same case the user did.
 *  
 * @author Martin Haye
 */
class QuerySpeller extends SimpleQueryRewriter
{
  /** Used to get spelling suggestions */
  private SpellReader spellReader;

  /** Used to tokenize queries */
  private Analyzer analyzer;
  
  /** Set of fields we're allowed to collect terms for */
  private HashSet<String> fieldSet;
  
  /** List of terms collected */
  private LinkedHashSet<String> terms;
  
  /** Mapping of terms to replace */
  private HashMap<String, String> suggestMap;

  /** Used to parse queries */
  private QueryParser queryParser;
      
  /** 
   * Construct a new speller using a given dictionary reader. The queries
   * will be parsed with a {@link MinimalAnalyzer}, and the default field
   * name will be "text".
   *
   * @param spellReader source for spelling suggestions -- see
   *                    {@link SpellReader#open(File)}.
   */
  public QuerySpeller(SpellReader spellReader)
  {
    this(spellReader, new QueryParser("text", new MinimalAnalyzer()));
  }
  
  /**
   * Construct a new speller using a given dictionary reader and analyzer (note
   * that the analyzer should do MINIMAL token filtering, without any case
   * conversion).
   * 
   * @param spellReader
   *          source for spelling suggestions -- see {@link SpellReader#open(File)}.
   * @param minimalQueryParser
   *          used to parse queries; note that the analyzer it uses should do
   *          only MINIMAL token filtering, not even conversion to lower case,
   *          so that suggestions can be made in the same case the user typed
   *          them. In particular, StandardAnalyzer should not be used.
   */
  public QuerySpeller(SpellReader spellReader, 
                      QueryParser minimalQueryParser)
  {
    this.spellReader = spellReader;
    this.queryParser = minimalQueryParser;
    
    // Test out the query parser's analyzer to make sure it preserves
    // the case of input tokens.
    //
    validateAnalyzer();
  }
  
  /** 
   * Make sure the analyzer preserves the case of input tokens. If it didn't,
   * we would be unable to make spelling suggestions that match the case of
   * user queries.
   */
  private void validateAnalyzer()
  {
    TokenStream toks = queryParser.getAnalyzer().tokenStream(
        queryParser.getField(), new StringReader("MixedCaseToken"));
    
    try {
      Token t;
      while ((t = toks.next()) != null) {
        if (t.termText().equals("MixedCaseToken"))
          return;
      }
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
    throw new IllegalArgumentException(
        "Unacceptable analyzer passed to QuerySpeller - must not convert to lower case");
  }
  
  /**
   * Suggest alternate spellings for terms in a Lucene query. By default,
   * we consider terms in any field. If you need to specify a subset of fields 
   * to consider, use the 
   * {@linkplain #suggest(String, String[]) alternate method} below.
   * 
   * @param     inQuery the original query to scan
   * @return    an query with some suggested spelling corrections, or
   *            null if no suggestions could be found. 
   */
  public synchronized String suggest(String inQuery) 
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
  public synchronized String suggest(String inQuery, String[] fields) 
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
      
    // Okay, traverse the query once, but don't make any changes. Just collect
    // the terms.
    //
    Query inQueryParsed = queryParser.parse(inQuery); 
    suggestMap = new HashMap<String, String>();
    terms = new LinkedHashSet<String>();
    rewriteQuery(inQueryParsed);
    
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
    Query rewritten = rewriteQuery(inQueryParsed);
    
    // Finally, convert the query back to a string, and we're done.
    return rewritten.toString(queryParser.getField());
  }
  
  /** This is the way we slip in to grab or rewrite terms */
  protected @Override Term rewrite(Term t) 
  {
    // Skip fields we're not supposed to look at.
    if (fieldSet != null && !fieldSet.contains(t.field()))
      return t;
    
    // Add this term to our accumulating list (if it's not already there)
    String text = t.text();
    terms.add(text);
    
    // If there's a suggestion, implement it.
    if (suggestMap.containsKey(text)) 
    {
      String suggText = suggestMap.get(text);
      if (suggText != null)
        return new Term(t.field(), suggText);
      else
        return null;
    }
    else
      return t;
  }

  /** 
   * Performs minimal token processing, without case conversion. 
   */
  public static class MinimalAnalyzer extends Analyzer
  {
    public TokenStream tokenStream(String fieldName, Reader reader) {
      TokenStream result = new StandardTokenizer(reader);
      result = new StandardFilter(result);
      return result;
    }
  }
}
