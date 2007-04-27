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

import java.io.IOException;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;

/**
 * A simple token filter for Lucene, that adds words to a spelling correction
 * dictionary as they're being indexed by Lucene. Generally this should be
 * placed early in the chain of token filters, generally before any non-case
 * modifications (such as thesaurus expansion, stemming, plural/singular
 * conversion, etc.). This way, the correction engine will be able to suggest
 * words that resemble what the user typed in.
 * 
 * @author Martin Haye
 */
public class SpellWritingFilter extends TokenFilter
{
  /** Queues words for addition to a spelling correction dictionary */
  private SpellWriter spellWriter;

  /**
   * Construct a new filter which writes words to a spelling dictionary, but
   * doesn't change any tokens.
   * 
   * @param input
   *          stream to read tokens from
   * @param spellWriter
   *          destination for spelling words
   */
  public SpellWritingFilter(TokenStream input, SpellWriter spellWriter)
  {
    super(input);
    this.spellWriter = spellWriter;
  }

  /**
   * Get the next token in the stream. We simply call the input filter to get
   * the token, queue the word for the spelling dictionary, and return it for
   * further processing by Lucene.
   */
  @Override
  public Token next() throws IOException
  {
    // Get the next token from the input stream.
    Token t = input.next();

    // If it has a non-standard position gap, disable pairing (i.e. don't
    // consider this word as adjacent to the previous one). Also do this
    // at the end of a field (i.e. the last token in the stream), so that
    // the first word of the next field doesn't get paired.
    //
    if (t == null || t.getPositionIncrement() != 1)
      spellWriter.queueBreak();

    // Okay, queue the word to be added to the spelling dictionary.
    if (t != null)
      spellWriter.queueWord(t.termText());

    // And pass on the token for further Lucene processing.
    return t;
  }

}
