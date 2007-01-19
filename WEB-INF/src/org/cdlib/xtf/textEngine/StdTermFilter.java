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

/*
 * This file created on Jan 17, 2007 by Martin Haye
 */

/**
 * Performs standard tokenization activities for terms, such as
 * mapping to lowercase, removing apostrophes, etc.
 * 
 * @author Martin Haye
 */
public class StdTermFilter extends XtfQueryRewriter 
{
  private DribbleStream dribble;
  private TokenStream filter;
  
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
  public String filter(String term) {
    dribble.nextToken = term;
    try {
      Token mapped = filter.next();
      if (mapped.termText().equals(term))
        return term;
      return mapped.termText();
    }
    catch (IOException e) {
      throw new RuntimeException("Very unexpected IO exception: " + e);
    }
  }
  
  private class DribbleStream extends TokenStream
  {
    public String nextToken;
    
    /** Return a token equal to the last one we were sent. */
    @Override
    public Token next() throws IOException
    {
      return new Token(nextToken, 0, 0);
    }
  }
  
} // class
