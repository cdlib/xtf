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

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

/**
 * Test the {@link SpellWritingAnalyzer} class (thus also testing
 * {@link SpellWritingFilter}). Performs the same tests as
 * {@link SpellReadWriteTest} but makes builds the dictionary while making a
 * Lucene index.
 * 
 * @author Martin Haye
 */
public class SpellWritingAnalyzerTest extends LuceneIndexToDictTest
{
  /** Create the temporary spelling dictionary */
  protected @Override void setUp() throws Exception
  {
    // Set up to create the spelling dictionary
    createDictDir("LuceneIndexToDictTest");
    SpellWriter spellWriter = SpellWriter.open(dictDir);
    spellWriter.setStopwords(STOP_SET);
    spellWriter.setMinWordFreq(1);

    try 
    {
      // Make the Lucene index using paragraphs from Call of the Wild.
      Directory luceneDir = new RAMDirectory();
      IndexWriter luceneWriter = new IndexWriter(luceneDir,
          new SpellWritingAnalyzer(spellWriter));
      addParagraphDocs(luceneWriter);
      luceneWriter.close();
      
      // Finish the dictionary.
      spellWriter.flushQueuedWords();
      spellWriter.close();

      // Ready to test.
      reader = SpellReader.open(dictDir);
      reader.setStopwords(STOP_SET);
    }
    finally {
      spellWriter.close();
    }
  }
}
