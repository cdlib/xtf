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

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

/**
 * Test the {@link LuceneIndexToDict} class. Performs the same tests as
 * {@link SpellReadWriteTest} but makes and converts a Lucene index instead of
 * making the dictionary directly.
 * 
 * @author Martin Haye
 */
public class LuceneIndexToDictTest extends SpellReadWriteTest
{
  /** Create the temporary spelling dictionary */
  protected @Override void setUp() throws Exception
  {
    // Make the Lucene index using paragraphs from Call of the Wild.
    Directory luceneDir = new RAMDirectory();
    IndexWriter luceneWriter = new IndexWriter(luceneDir,
        new StandardAnalyzer());
    addParagraphDocs(luceneWriter);
    luceneWriter.close();

    // Set up to create the spelling dictionary
    createDictDir("LuceneIndexToDictTest");
    SpellWriter spellWriter = SpellWriter.open(dictDir);
    spellWriter.setStopwords(STOP_SET);
    spellWriter.setMinWordFreq(1);

    try {
      // Convert the Lucene index to a spelling dictionary.
      IndexReader luceneReader = IndexReader.open(luceneDir);
      LuceneIndexToDict.createDict(luceneReader, new MinimalAnalyzer(), spellWriter, null);

      // Ready to test.
      reader = SpellReader.open(dictDir);
      reader.setStopwords(STOP_SET);
    }
    finally {
      spellWriter.close();
    }
  }

  /**
   * Divide the Call of the Wild text into paragraphs. Add them as various
   * fields in several documents to a Lucene index writer.
   * 
   * @param luceneWriter    destination for the new docs
   */
  protected void addParagraphDocs(IndexWriter luceneWriter) throws IOException
  {
    // Divide the text into paragraphs.
    String[] paras = CALL_OF_THE_WILD.split("\n\n");
    
    // Stick in some accented chars to test end-to-end accent preservation
    paras[0] = paras[0] + " europ\u00e4ische europ\u00e4ische europ\u00e4ische";

    // Put the paragraphs into the Lucene index, splitting them up into a few
    // fields and documents.
    //
    Document doc = new Document();
    for (int i = 0; i < paras.length; i++) {
      int nFields = doc.getFields().size();
      doc.add(new Field("field" + nFields, paras[i], Field.Store.YES,
          Field.Index.TOKENIZED));
      if (nFields + 1 == 3) {
        luceneWriter.addDocument(doc);
        doc = new Document();
      }
    }

    if (doc.getFields().size() > 0)
      luceneWriter.addDocument(doc);
  }
}
