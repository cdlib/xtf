package org.apache.lucene.search.spell;


/**
 * Copyright 2002-2004 The Apache Software Foundation
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

/**
 * Edit distance  class
 */
final class TRStringDistance {

    final char[] sa;
    final int n;
    final int[][][] cache=new int[30][][];


    /**
     * Optimized to run a bit faster than the static getDistance().
     * In one benchmark times were 5.3sec using ctr vs 8.5sec w/ static method, thus 37% faster.
     */
    public TRStringDistance (String target) {
        sa=target.toCharArray();
        n=sa.length;
    }


      /**
       * Compute Damerau-Levenstein distance between the target string and
       * another string. Damerau-Levenstein is similar to Levenstein except
       * that it also accounts for transposition in the set of edit operations.
       * This more fully reflects a common source of misspellings. 
       */
      public final int getDistance (String other) {
          int d[][]; // matrix
          int baseCost, replaceCost, insertCost, deleteCost, transposeCost;

          // First, initialize the matrix.
          final char[] ta=other.toCharArray();
          final int m=ta.length;
          if (n==0)
              return m;
          if (m==0)
              return n;

          if (m>=cache.length)
              d=form(n, m);
          else if (cache[m]!=null)
              d=cache[m];
          else
              d=cache[m]=form(n, m);
          
          // Process each source character
          char s_i2 = 0;
          for (int i=1; i<=n; i++) 
          {
              final char s_i=sa[i-1];

              // Process each target character
              char t_j2 = 0;
              for (int j=1; j<=m; j++) 
              {
                  final char t_j=ta[j-1];

                  baseCost      = s_i == t_j ? 0 : 2;
                  replaceCost   = d[i-1][j-1] + baseCost;
                  insertCost    = d[i-1][j]   + (s_i == s_i2 ? 1 : 2);
                  deleteCost    = d[i][j-1]   + (t_j == t_j2 ? 1 : 2);
                  
                  d[i][j]=min3(replaceCost, insertCost, deleteCost);
                  
                  // Check for transposition
                  if (s_i != t_j && s_i == t_j2 && t_j == s_i2)
                     d[i][j] = Math.min(d[i][j], d[i-2][j-2] + 1);
                  
                  t_j2 = t_j;
              }
              
              s_i2 = s_i;
          }

          // Step 7
          return d[n][m];

      }
      
    /**
     *
     */
    private static int[][] form (int n, int m) {
        int[][] d=new int[n+1][m+1];
        // Step 2

        for (int i=0; i<=n; i++) {
            d[i][0]=i;

        }
        for (int j=0; j<=m; j++) {
            d[0][j]=j;
        }
        return d;
    }


    //****************************
     // Get minimum of three values
     //****************************
      private static int min3 (int a, int b, int c) {
          int mi=a;
          if (b<mi) {
              mi=b;
          }
          if (c<mi) {
              mi=c;
          }
          return mi;

      }
}
