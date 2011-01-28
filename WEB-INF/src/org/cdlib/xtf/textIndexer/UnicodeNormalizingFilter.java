
package org.cdlib.xtf.textIndexer;

import java.io.IOException;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.cdlib.xtf.util.FastCache;
import org.cdlib.xtf.util.Normalizer;

/**
 * Apply Unicode Normalization to the tokens.
 * 
 * @see java.text.Normalizer
 * 
 * @author Marcos Fragomeni
 */
public class UnicodeNormalizingFilter extends TokenFilter 
{
    /** How many recent mappings to maintain */
    private static final int CACHE_SIZE = 5000;
  
    /** Keep a cache of lookups performed to-date */
    private FastCache<String, String> cache = new FastCache(CACHE_SIZE);
  
    public UnicodeNormalizingFilter(TokenStream input) {
        super(input);
    }

    @Override
    public Token next() throws IOException 
    {
        Token t = input.next();

        if (t == null) {
            return null;
        }

        // Only do the (sometimes lengthy) normalization step if we haven't already 
        // looked up this token.
        //
        String text = t.termText();
        if (!cache.contains(text)) {
          String normalizedText = Normalizer.normalize(text);
          cache.put(text, normalizedText);
        }
        String newText = cache.get(text);
        if (!newText.equals(text))
          t.setTermText(newText);

        return t;
    }
}
