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
 *
 * Acknowledgements:
 *
 * A significant amount of new and/or modified code in this module
 * was made possible by a grant from the Andrew W. Mellon Foundation,
 * as part of the Melvyl Recommender Project.
 */

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WordlistLoader;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

/**
 * Drop-in replacement for the Lucene {@link StandardAnalyzer}, which performs
 * all the same functions plus queues words to a spelling dictionary.
 * 
 * @author Martin Haye
 */
public class SpellWritingAnalyzer extends Analyzer
{
  /** Set of stop words to remove during analysis */
  private Set stopSet;

  /** Destination for words to be added to dictionary */
  private SpellWriter writer;

  /**
   * An array containing some common English words that are usually not useful
   * for searching.
   */
  public static final String[] STOP_WORDS = StopAnalyzer.ENGLISH_STOP_WORDS;

  /**
   * Builds an analyzer which writes to the given spelling dictionary, with the
   * default stop words ({@link #STOP_WORDS}).
   */
  public SpellWritingAnalyzer(SpellWriter spellWriter)
  {
    this(StandardAnalyzer.STOP_WORDS, spellWriter);
  }

  /**
   * Builds an analyzer which writes to the given spelling dictionary, using the
   * given stop words.
   */
  public SpellWritingAnalyzer(Set stopWords, SpellWriter spellWriter)
  {
    this.stopSet = stopWords;
    this.writer = spellWriter;
    writer.setStopwords(stopWords);
  }

  /**
   * Builds an analyzer which writes to the given spelling dictionary, using the
   * given stop words.
   */
  public SpellWritingAnalyzer(String[] stopWords, SpellWriter spellWriter)
  {
    this(StopFilter.makeStopSet(stopWords), spellWriter);
  }

  /**
   * Builds an analyzer which writes to the given spelling dictionary, using the
   * stop words from the given file.
   * 
   * @see WordlistLoader#getWordSet(File)
   */
  public SpellWritingAnalyzer(File stopwords, SpellWriter spellWriter)
      throws IOException
  {
    this(WordlistLoader.getWordSet(stopwords), spellWriter);
  }

  /**
   * Builds an analyzer which writes to the given spelling dictionary, using the
   * stop words from the given reader.
   * 
   * @see WordlistLoader#getWordSet(Reader)
   */
  public SpellWritingAnalyzer(Reader stopwords, SpellWriter spellWriter)
      throws IOException
  {
    this(WordlistLoader.getWordSet(stopwords), spellWriter);
  }

  /**
   * Constructs a {@link StandardTokenizer} filtered by a {@link
   * StandardFilter}, a {@link SpellWritingFilter}, a {@link LowerCaseFilter}
   * and a {@link StopFilter}.
   */
  public TokenStream tokenStream(String fieldName, Reader reader)
  {
    TokenStream result = new StandardTokenizer(reader);
    result = new StandardFilter(result);
    result = new SpellWritingFilter(result, writer);
    result = new LowerCaseFilter(result);
    result = new StopFilter(result, stopSet);
    return result;
  }
}
