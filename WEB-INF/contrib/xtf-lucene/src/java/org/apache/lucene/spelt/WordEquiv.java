package org.apache.lucene.spelt;

/*
 * Copyright 2006-2007 The Apache Software Foundation.
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

/**
 * Used to establish whether a potential spelling suggestion is simply
 * "equivalent" to the original word, and should thus be skipped.
 * 
 * @author Martin Haye
 */
public interface WordEquiv 
{
  boolean isEquivalent(String word1, String word2);

  static WordEquiv DEFAULT = new WordEquiv() 
  {
    public boolean isEquivalent(String w1, String w2) {
      return w1.equalsIgnoreCase(w2);
    }
  };
}
