package org.cdlib.xtf.textEngine;


/**
 * Copyright 2004 The Apache Software Foundation
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
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.Weight;
import java.io.IOException;
import java.util.Set;

/**
 * A query that implements efficient range searching on numeric data. Handles
 * positive numbers up to 63 bits.
 */
public class NumericRangeQuery extends Query 
{
  private final String fieldName;
  private final boolean includeLower;
  private final String lowerVal;
  private final boolean includeUpper;
  private final String upperVal;

  public NumericRangeQuery(String fieldName, String lowerVal, String upperVal,
                           boolean includeLower, boolean includeUpper) 
  {
    // do a little bit of normalization...
    if ("".equals(lowerVal)) {
      lowerVal = null;
    }

    if ("".equals(upperVal)) {
      upperVal = null;
    }

    this.fieldName = fieldName.intern(); // intern it, just like terms...
    this.lowerVal = lowerVal;
    this.upperVal = upperVal;
    this.includeLower = includeLower;
    this.includeUpper = includeUpper;
  }

  /** Returns the field name for this query */
  public String getField() {
    return fieldName;
  }

  /** Returns the value of the lower endpoint of this range query, null if open ended */
  public String getLowerVal() {
    return lowerVal;
  }

  /** Returns the value of the upper endpoint of this range query, null if open ended */
  public String getUpperVal() {
    return upperVal;
  }

  /** Returns <code>true</code> if the lower endpoint is inclusive */
  public boolean includesLower() {
    return includeLower;
  }

  /** Returns <code>true</code> if the upper endpoint is inclusive */
  public boolean includesUpper() {
    return includeUpper;
  }

  public Query rewrite(IndexReader reader)
    throws IOException 
  {
    return this;
  }

  public void extractTerms(Set terms) 
  {
    // OK to not add any terms when used for MultiSearcher,
    // but may not be OK for highlighting
  }

  private class NumericRangeWeight implements Weight 
  {
    private NumericRangeQuery query;
    private Similarity similarity;
    private float queryNorm;
    private float queryWeight;

    public NumericRangeWeight(NumericRangeQuery query, Searcher searcher) {
      this.similarity = getSimilarity(searcher);
    }

    public Query getQuery() {
      return NumericRangeQuery.this;
    }

    public float getValue() {
      return queryWeight;
    }

    public float sumOfSquaredWeights()
      throws IOException 
    {
      queryWeight = getBoost();
      return queryWeight * queryWeight;
    }

    public void normalize(float norm) {
      this.queryNorm = norm;
      queryWeight *= this.queryNorm;
    }

    public Scorer scorer(IndexReader reader)
      throws IOException 
    {
      return new NumericRangeScorer(similarity, reader, this);
    }

    public Explanation explain(IndexReader reader, int doc)
      throws IOException 
    {
      NumericRangeScorer cs = (NumericRangeScorer)scorer(reader);
      int docPos = cs.docPos(doc);
      boolean inRange = docPos >= 0 ? cs.inRange(docPos) : false;

      Explanation result = new Explanation();

      if (inRange) {
        result.setDescription(
          "NumericRangeQuery(" + query.toString() + "), product of:");
        result.setValue(queryWeight);
        result.addDetail(new Explanation(getBoost(), "boost"));
        result.addDetail(new Explanation(queryNorm, "queryNorm"));
      }
      else {
        result.setDescription(
          "NumericRangeQuery(" + query.toString() + ") doesn't match id " +
          doc);
        result.setValue(0);
      }
      return result;
    }
  }

  private class NumericRangeScorer extends Scorer 
  {
    final NumericFieldData data;
    final float theScore;
    final int dataSize;
    final boolean checkLower;
    final long lowerNum;
    final boolean checkUpper;
    final long upperNum;
    int dataPos = -1;

    public NumericRangeScorer(Similarity similarity, IndexReader reader,
                              Weight w)
      throws IOException 
    {
      super(similarity);
      theScore = w.getValue();
      data = NumericFieldData.getCachedData(reader, fieldName);
      dataSize = data.size();
      checkLower = (lowerVal != null);
      this.lowerNum = checkLower ? NumericFieldData.parseVal(lowerVal) : -1;
      checkUpper = (upperVal != null);
      this.upperNum = checkUpper ? NumericFieldData.parseVal(upperVal) : -1;
    }

    public boolean next()
      throws IOException 
    {
      while (true) {
        ++dataPos;
        if (dataPos >= dataSize)
          break;
        if (inRange(dataPos))
          break;
      }
      return dataPos < dataSize;
    }

    public final boolean inRange(int dataPos) 
    {
      long value = data.value(dataPos);
      if (checkLower) {
        if (value < lowerNum)
          return false;
        if (value == lowerNum && !includeLower)
          return false;
      }
      if (checkUpper) {
        if (value > upperNum)
          return false;
        if (value == upperNum && !includeUpper)
          return false;
      }
      return true;
    }

    public int docPos(int doc) {
      return data.docPos(doc);
    }

    public int doc() {
      return data.doc(dataPos);
    }

    public float score()
      throws IOException 
    {
      return theScore;
    }

    public boolean skipTo(int target)
      throws IOException 
    {
      dataPos = Math.max(dataPos, data.findDocIndex(target) - 1);
      return next();
    }

    public Explanation explain(int doc)
      throws IOException 
    {
      throw new UnsupportedOperationException();
    }
  }

  protected Weight createWeight(Searcher searcher) {
    return new NumericRangeQuery.NumericRangeWeight(this, searcher);
  }

  /** Prints a user-readable version of this query. */
  public String toString(String field) 
  {
    StringBuffer buffer = new StringBuffer();
    if (!getField().equals(field)) {
      buffer.append(getField());
      buffer.append(":");
    }
    buffer.append(includeLower ? '[' : '{');
    buffer.append(lowerVal != null ? lowerVal : "*");
    buffer.append(" TO ");
    buffer.append(upperVal != null ? upperVal : "*");
    buffer.append(includeUpper ? ']' : '}');
    if (getBoost() != 1.0f) {
      buffer.append("^");
      buffer.append(Float.toString(getBoost()));
    }
    return buffer.toString();
  }

  /** Returns true if <code>o</code> is equal to this. */
  public boolean equals(Object o) 
  {
    if (this == o)
      return true;
    if (!(o instanceof NumericRangeQuery))
      return false;
    NumericRangeQuery other = (NumericRangeQuery)o;

    if (this.fieldName != other.fieldName // interned comparison
         ||
        this.includeLower != other.includeLower ||
        this.includeUpper != other.includeUpper) 
    {
      return false;
    }
    if (this.lowerVal != null ? !this.lowerVal.equals(other.lowerVal)
        : other.lowerVal != null)
      return false;
    if (this.upperVal != null ? !this.upperVal.equals(other.upperVal)
        : other.upperVal != null)
      return false;
    return this.getBoost() == other.getBoost();
  }

  /** Returns a hash code value for this object.*/
  public int hashCode() 
  {
    int h = Float.floatToIntBits(getBoost()) ^ fieldName.hashCode();

    // hashCode of "" is 0, so don't use that for null...
    h ^= lowerVal != null ? lowerVal.hashCode() : 0x965a965a;

    // don't just XOR upperVal with out mixing either it or h, as it will cancel
    // out lowerVal if they are equal.
    h ^= (h << 17) | (h >>> 16); // a reversible (one to one) 32 bit mapping mix
    h ^= (upperVal != null ? (upperVal.hashCode()) : 0x5a695a69);
    h ^= (includeLower ? 0x665599aa : 0) ^ (includeUpper ? 0x99aa5566 : 0);
    return h;
  }
} // class NumericRangeQuery
