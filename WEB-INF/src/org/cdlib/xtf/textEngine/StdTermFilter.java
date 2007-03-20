package org.cdlib.xtf.textEngine;


/*
 * Copyright (c) 2007, Regents of the University of California
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
import java.io.IOException;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.cdlib.xtf.util.FastStringReader;
import org.cdlib.xtf.util.FastTokenizer;

/*
 * This file created on Jan 17, 2007 by Martin Haye
 */

/**
 * Performs standard tokenization activities for terms, such as
 * mapping to lowercase, removing apostrophes, etc.
 *
 * @author Martin Haye
 */
public class StdTermFilter 
{
  private DribbleStream dribble;
  private TokenStream filter;

  /**
   * During tokenization, the '*' wildcard has to be changed to a word
   * to keep it from being removed.
   */
  private static final String SAVE_WILD_STAR = "jwxbkn";

  /**
   * During tokenization, the '?' wildcard has to be changed to a word
   * to keep it from being removed.
   */
  private static final String SAVE_WILD_QMARK = "vkyqxw";

  /** Construct the rewriter */
  public StdTermFilter() {
    dribble = new DribbleStream();
    filter = new StandardFilter(new LowerCaseFilter(dribble));
  }

  /**
   * Apply the standard mapping to the given term.
   *
   * @return changed version, or original term if no change required.
   */
  public String filter(String term) 
  {
    dribble.nextToken = saveWildcards(term);
    try {
      Token mapped = filter.next();
      String restored = restoreWildcards(mapped.termText());
      if (restored.equals(term))
        return term;
      return restored;
    }
    catch (IOException e) {
      throw new RuntimeException("Very unexpected IO exception: " + e);
    }
  }

  /**
   * Converts wildcard characters into word-looking bits that would never
   * occur in real text, so the standard tokenizer will keep them part of
   * words. Resurrect using {@link #restoreWildcards(String)}.
   */
  protected static String saveWildcards(String s) 
  {
    // Early out if no wildcards found.
    if (s.indexOf('*') < 0 && s.indexOf('?') < 0)
      return s;

    // Convert to wordish stuff.
    s = s.replaceAll("\\*", SAVE_WILD_STAR);
    s = s.replaceAll("\\?", SAVE_WILD_QMARK);
    return s;
  } // saveWildcards()

  /**
   * Restores wildcards saved by {@link #saveWildcards(String)}.
   */
  protected static String restoreWildcards(String s) 
  {
    // Early out if no wildcards found.
    if (s.indexOf(SAVE_WILD_STAR) < 0 && s.indexOf(SAVE_WILD_QMARK) < 0)
      return s;

    // Convert back from wordish stuff to real wildcards.
    s = s.replaceAll(SAVE_WILD_STAR, "*");
    s = s.replaceAll(SAVE_WILD_QMARK, "?");
    return s;
  } // restoreWildcards()

  private class DribbleStream extends TokenStream 
  {
    public String nextToken;

    /** Return a token equal to the last one we were sent. */
    @Override
    public Token next()
      throws IOException 
    {
      FastTokenizer toks = new FastTokenizer(new FastStringReader(nextToken));
      Token t = toks.next();
      
      // If it doesn't see it as a token, make our own.
      if (t == null)
        return new Token(nextToken, 0, nextToken.length());
      
      // If the entire text wasn't consumed, ignore the result and make our
      // own token.
      //
      else if (t.startOffset() != 0 || t.endOffset() != nextToken.length()) 
        return new Token(nextToken, 0, nextToken.length());
      
      // Good, it consumed the whole thing. Return the parsed token.
      else
        return t;
    }
  }
} // class
