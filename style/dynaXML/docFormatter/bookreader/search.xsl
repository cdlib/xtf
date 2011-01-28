<xsl:stylesheet version="2.0"
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:xtf="http://cdlib.org/xtf"
   xmlns="http://www.w3.org/1999/xhtml"
   xmlns:local="http://local"
   exclude-result-prefixes="#all">
   
   <!--
      Copyright (c) 2010, Regents of the University of California
      All rights reserved.
      
      Redistribution and use in source and binary forms, with or without 
      modification, are permitted provided that the following conditions are 
      met:
      
      - Redistributions of source code must retain the above copyright notice, 
      this list of conditions and the following disclaimer.
      - Redistributions in binary form must reproduce the above copyright 
      notice, this list of conditions and the following disclaimer in the 
      documentation and/or other materials provided with the distribution.
      - Neither the name of the University of California nor the names of its
      contributors may be used to endorse or promote products derived from 
      this software without specific prior written permission.
      
      THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
      AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
      IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
      ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
      LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
      CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
      SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
      INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
      CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
      ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
      POSSIBILITY OF SUCH DAMAGE.
   -->
   
   <!-- Search Hits -->
   
   <!-- Workhorse to generate the box around each hit and the term(s) inside it -->
   <xsl:template match="xtf:hit | xtf:more" mode="search-results">
      
      <!-- Locate the hit, line and leaf this hit is part of -->
      <xsl:variable name="hitNum" select="@hitNum"/>
      <xsl:variable name="line" select="ancestor::line"/>
      <xsl:variable name="leaf" select="$line/ancestor::leaf"/>
      
      <!-- The top and bottom of the line are easy to find, as are the leaf dimensions. -->
      <xsl:variable name="top" select="$line/@t"/>
      <xsl:variable name="bottom" select="$line/@b"/>
      <xsl:variable name="leafWidth" select="$leaf/cropBox/@w"/>
      <xsl:variable name="leafHeight" select="$leaf/cropBox/@h"/>
      
      
      <!-- The line data contains spacing that looks like this: "23 3 14 5 16...". Here's how
         to interpret the data in this sample:
         
         23 = width of 1st word
         3 = space between 1st and 2nd words
         14 = width of 2nd word
         5 = space between 2nd and 3rd words
         16 = width of 3rd word
         ...etc...
         
         To be useful in our XSLT code, we need to split out the values into a sequence
         that we can index by position. That's what the xsl:analyze-string does here.
      -->
      <xsl:variable name="lineSpacing">
         <xsl:analyze-string select="$line/@spacing" regex="\s+">
            <xsl:non-matching-substring>
               <spacing xmlns="" width="{.}"/>
            </xsl:non-matching-substring>
         </xsl:analyze-string>
      </xsl:variable>
      
      <!-- To determine the left-hand boundary of the box for the hit or term, we
         need to figure out how many words are before it. Then we can use the
         spacing data to determine the exact coordinate. -->
      <xsl:variable name="textBeforeStart">
         <xsl:if test="local-name() = 'term'">
            <xsl:for-each select="parent::*/preceding-sibling::node()">
               <xsl:value-of select="string(.)"/>
            </xsl:for-each>
         </xsl:if>
         <xsl:for-each select="preceding-sibling::node()">
            <xsl:value-of select="string(.)"/>
         </xsl:for-each>
      </xsl:variable>
      
      <xsl:variable name="left" select="$line/@l + xtf:sumSpacing($lineSpacing, $textBeforeStart, 0)"/>
      
      <!-- Similarly, we compute the right-hand box boundary by adding the spacing for
         the words inside the hit. -->
      <xsl:variable name="textBeforeEnd" select="concat($textBeforeStart, string(.))"/>
      <xsl:variable name="right" select="$line/@l + xtf:sumSpacing($lineSpacing, $textBeforeEnd, -1)"/>
      
      <!-- Whew, that was a lot of computation. We're finally ready to generate the box. 
           We want a little padding so the yellow box is bigger than the word. 
           For some reason, the top needs a bit more than the others, and the
           bottom doesn't need any. 
      -->
      <!-- Spit out the leaf number and context. The context comes from the snippet element at the doc top -->
      { 'leaf':<xsl:value-of select="$leaf/@leafNum"/>,
        'context':"<xsl:apply-templates select="/*/xtf:snippets/xtf:snippet[@hitNum=$hitNum]" mode="hit-context"/>",
        'clientKey':<xsl:value-of select="if ($hit.rank) then @rank else @hitNum"/>,
        'l': <xsl:value-of select="max((0,            $left   - 10))"/>,
        't': <xsl:value-of select="max((0,            $top    - 10))"/>,
        'r': <xsl:value-of select="min(($leafWidth,   $right  + 10))"/>,
        'b': <xsl:value-of select="min(($leafHeight,  $bottom + 10))"/>,
      },
   </xsl:template>
   
   <!-- This function sums up the spacing for a given number of words. Used to determine
      the left and right coordinates of a hit. -->
   <xsl:function name="xtf:sumSpacing">
      <xsl:param name="lineSpacing"/>
      <xsl:param name="textBefore"/>
      <xsl:param name="tail"/>
      
      <!-- For each word (that is, a series of non-space characters) make a mark. -->
      <xsl:variable name="trimmedTextBefore" select="if ($tail) then $textBefore else replace($textBefore, '\S+$', '')"/>
      <xsl:variable name="wordsBefore">
         <xsl:analyze-string select="$trimmedTextBefore" regex="\s+">
            <xsl:non-matching-substring>1</xsl:non-matching-substring>
         </xsl:analyze-string>
      </xsl:variable>
      
      <!-- Count the marks we just made -->
      <xsl:variable name="nWordsBefore" select="string-length($wordsBefore)"/>
      
      <!-- Grab the spacing for each of those words, plus the space between them -->
      <xsl:variable name="spacingBefore" select="$lineSpacing/*[position() &lt;= (($nWordsBefore * 2) + $tail)]"/>
      
      <!-- And return the sum -->
      <xsl:value-of select="sum($spacingBefore/@width)"/>
   </xsl:function>
   
   <xsl:template match="xtf:snippet" mode="hit-context">
      <xsl:apply-templates mode="hit-context"/>
   </xsl:template>
   
   <xsl:template match="xtf:term" mode="hit-context">
      <b>
         <xsl:apply-templates mode="hit-context"/>
      </b>
   </xsl:template>
   
   <xsl:template match="text()" mode="hit-context">
      <xsl:value-of select="local:unquote(.)"/>
   </xsl:template>

   <xsl:function name="local:unquote">
      <xsl:param name="str"/>
      <xsl:variable name="quote" select="'&quot;'"/>
      <xsl:value-of select="replace(replace($str, $quote, ''), '\\', '')"/>
   </xsl:function>

</xsl:stylesheet>
