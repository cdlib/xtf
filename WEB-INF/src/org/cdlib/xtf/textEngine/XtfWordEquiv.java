package org.cdlib.xtf.textEngine;


/**
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
import org.apache.lucene.search.spell.WordEquiv;
import org.cdlib.xtf.util.CharMap;
import org.cdlib.xtf.util.FastStringCache;
import org.cdlib.xtf.util.WordMap;

/** Used for eliminating redundant spelling suggestions */
public class XtfWordEquiv implements WordEquiv 
{
  private CharMap accentMap;
  private WordMap pluralMap;
  private StdTermFilter stdTermFilter = new StdTermFilter();
  private FastStringCache recent = new FastStringCache(1000);

  public XtfWordEquiv(CharMap accentMap, WordMap pluralMap) {
    this.accentMap = accentMap;
    this.pluralMap = pluralMap;
  }

  /**
   * Checks if two words can be considered equivalent, and thus not form a
   * real spelling suggestion.
   */
  public boolean isEquivalent(String word1, String word2) 
  {
    // Filter both words (convert to lower case, remove plurals, etc.)
    word1 = filter(word1);
    word2 = filter(word2);

    // And compare the filtered versions.
    return word1.equals(word2);
  }

  private String filter(String in) 
  {
    String out = (String)recent.get(in);
    if (out == null) 
    {
      out = stdTermFilter.filter(in);

      // Next, ignore accents.
      String tmp;
      if (accentMap != null) {
        tmp = accentMap.mapWord(out);
        if (tmp != null)
          out = tmp;
      }

      // Then ignore plurals.
      tmp = pluralMap.lookup(out);
      if (pluralMap != null) {
        if (tmp != null)
          out = tmp;
        recent.put(in, out);
      }
    }
    return out;
  }
}
