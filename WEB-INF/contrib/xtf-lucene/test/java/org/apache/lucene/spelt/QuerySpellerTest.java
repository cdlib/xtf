package org.apache.lucene.spelt;

/*
 * Copyright 2007 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;

/** 
 * Test the {@link SpellReader} and {@link SpellWriter} classes 
 *
 * @author Martin Haye
 */
public class QuerySpellerTest extends SpellReadWriteTest
{
  private QueryParser parser = new QueryParser("text", new MinimalAnalyzer());
  private QuerySpeller speller;
  
  protected @Override void setUp() throws Exception
  {
    super.setUp();
    speller = new QuerySpeller(reader);
  }
  
  /** Test out single-word replacements */
  public @Override void testSingleWords() throws IOException
  {
    // First, test some words that shouldn't get corrected
    checkSuggestion("London", null);
    checkSuggestion("newspapers", null);
    checkSuggestion("asdfkjlh", null);
    
    // Also make sure stop words don't result in suggestions
    checkSuggestion("the", null);
    checkSuggestion("and", null);
    
    // Okay, let's try some things that should get a suggestion.
    checkSuggestion("newpapers", "newspapers");
    checkSuggestion("newspaper", "newspapers");
    checkSuggestion("bck", "buck");
    checkSuggestion("bcuk", "buck");
    
    // Check the case copying facility
    checkSuggestion("Newpapers", "Newspapers");
    checkSuggestion("NEWPAPERS", "NEWSPAPERS");
    checkSuggestion("Bck", "Buck");
  }
  
  /** Test out multi-word replacements */
  public @Override void testMultiWords() throws IOException
  {
    checkSuggestion("news papers", "newspapers");
    checkSuggestion("\"news papers\"", "newspapers");
    checkSuggestion("readnewspapers", "\"read newspapers\"");
    checkSuggestion("readn ewspapers", "read newspapers");
    checkSuggestion("\"readn ewspapers\"", "\"read newspapers\"");
    
    checkSuggestion("orchards and bery patches", "orchards and berry patches");
  }
  
  /** Check that the given input query results in the right suggestion */
  private void checkSuggestion(String inQuery, String outQuery) 
    throws IOException
  {
    try {
      String suggestion = speller.suggest(inQuery);
      assertEquals(outQuery, suggestion);
    }
    catch (ParseException e) {
      throw new IOException(e.toString());
    }
  }
}
