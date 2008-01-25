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

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;

import junit.framework.TestCase;

public class SimpleQueryRewriterTest extends TestCase
{
  private QueryParser parser = new QueryParser("text", new StandardAnalyzer());
  private SimpleQueryRewriter rewriter;
  
  public void testSimple() throws ParseException
  {
    rewriter = new SimpleRewriter();
    testQuery("happy", "foohappy");
    testQuery("happy bear", "foohappy foobear");
    testQuery("a:happy b:bear", "a:foohappy b:foobear");
    testQuery("+happy +bear", "+foohappy +foobear");
    testQuery("happy^5", "foohappy^5.0");
    testQuery("\"happy bear\"", "\"foohappy foobear\"");
  }

  public void testSplitting() throws ParseException
  {
    rewriter = new SplittingRewriter();
    testQuery("happy", "happy");
    testQuery("abxyz", "\"ab yz\"");
    testQuery("abxyz^5", "\"ab yz\"^5.0");
    testQuery("a:mxn b:pxq", "a:\"m n\" b:\"p q\"");
    testQuery("\"axb pxq\"", "\"a b p q\"");
  }
  
  public void testDropping() throws ParseException
  {
    rewriter = new DroppingRewriter();
    testQuery("happy bear times three", "happy times");
    testQuery("\"happy bear times three\"", "\"happy times\"");
  }
  
  private void testQuery(String inQuery, String outQuery) 
    throws ParseException
  {
    Query inParsed = parser.parse(inQuery);
    Query rewritten = rewriter.rewriteQuery(inParsed);
    String rewrittenStr = rewritten.toString("text");
    assertEquals(outQuery, rewrittenStr);
  }
  
  private class SimpleRewriter extends SimpleQueryRewriter
  {
    public Term rewrite(Term t) {
      return new Term(t.field(), "foo" + t.text());
    }
  }
  
  private class SplittingRewriter extends SimpleQueryRewriter
  {
    public Term rewrite(Term t)
    {
      String text = t.text();
      int pos = text.indexOf('x');
      if (pos < 0)
        return t;
      
      return new Term(t.field(), text.replace('x', ' '));
    }
  }
  
  private class DroppingRewriter extends SimpleQueryRewriter
  {
    private int num = 0;
    
    public Term rewrite(Term t)
    {
      num++;
      if ((num % 2) == 1) // drop every other term
        return t;
      else
        return null;
    }
  }
}
