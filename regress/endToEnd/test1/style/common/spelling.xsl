<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
   xmlns:cdl="http://cdlib.org" 
   xmlns="http://www.w3.org/1999/xhtml"
   exclude-result-prefixes="#all"
   version="2.0">
   
   <!--
      Copyright (c) 2008, Regents of the University of California
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
   
   <!-- 
      Stylesheet for formatting spelling suggestions.
   -->
   
   <!-- ====================================================================== -->
   <!-- Local Parameters                                                       -->
   <!-- ====================================================================== -->
   
   <xsl:param name="keyword"/>
   
   <!-- ====================================================================== -->
   <!-- Generate Spelling Suggestion                                           -->
   <!-- ====================================================================== -->
   
   <xsl:template name="did-you-mean">
      <xsl:param name="baseURL"/>
      <xsl:param name="spelling"/>
      
      <xsl:variable name="newURL" select="cdl:replace-misspellings($baseURL, $spelling/*)"/>
      <b>Did you mean to search for
         <a href="{$newURL}">
            <xsl:apply-templates select="query" mode="spelling"/>
         </a>
         <xsl:text>?</xsl:text> </b>
   </xsl:template>
   
   <!-- 
      Scan the URL and replace possibly misspelled words with suggestions
      from the spelling correction engine.
   -->
   <xsl:function name="cdl:replace-misspellings">
      <xsl:param name="baseURL"/>
      <xsl:param name="suggestions"/>
      
      <xsl:choose>
         <xsl:when test="$suggestions">
            <xsl:variable name="sugg" select="$suggestions[1]"/>
            <xsl:variable name="remainder" select="cdl:replace-misspellings($baseURL, $suggestions[position() > 1])"/>
            <xsl:variable name="fields" select="concat($sugg/@fields, ',keyword,freeformQuery')"/>
            
            <!-- 
               Replace the term in the proper field(s) from the URL. Make sure it has word
               boundaries on either side of it. 
            -->
            <xsl:variable name="matchPattern" 
               select="concat('(\W(', replace($fields, ',', '|'), ')=([^=;&amp;]+\W)?)', 
               $sugg/@originalTerm,
               '(\W|$)')"/>
            <xsl:variable name="changed" 
               select="replace($remainder, $matchPattern, 
               concat('$1', $sugg/@suggestedTerm, '$4'), 'i')"/>
            <xsl:value-of select="$changed"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:value-of select="$baseURL"/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:function>
   
   <!-- 
      Scan a list of terms and replace possibly misspelled words with suggestions
      from the spelling correction engine.
   -->
   <xsl:function name="cdl:fix-terms">
      <xsl:param name="terms"/>
      <xsl:param name="spelling"/>
      
      <!-- Get the first term -->
      <xsl:variable name="term" select="$terms[1]"/>
      <xsl:if test="$term">
         
         <!-- Figure out what field(s) to apply to. -->
         <xsl:variable name="rawFields">
            <xsl:choose>
               <xsl:when test="$term/ancestor-or-self::*[@fields]">
                  <xsl:value-of select="$term/ancestor-or-self::*[@fields][1]/@fields"/>
               </xsl:when>
               <xsl:when test="$term/ancestor-or-self::*[@field]">
                  <xsl:value-of select="$term/ancestor-or-self::*[@field][1]/@field"/>
               </xsl:when>
            </xsl:choose>
         </xsl:variable>
         
         <!-- Make the field list into a handy regular expression that matches any of the fields -->
         <xsl:variable name="fieldsRegex" select="replace($rawFields, '[\s,;]+', '|')"/>
         
         <!-- See if there's a replacement for this term -->
         <xsl:variable name="replacement" select="$spelling/suggestion[matches(@fields, $fieldsRegex) and (@originalTerm = string($term))]"/>
         <xsl:choose>
            <xsl:when test="$replacement">
               <xsl:value-of select="$replacement/@suggestedTerm"/>
            </xsl:when>
            <xsl:otherwise>
               <xsl:value-of select="string($term)"/>
            </xsl:otherwise>
         </xsl:choose>
         
         <!-- Process the remaining terms in the list -->
         <xsl:if test="count($terms) > 1">
            <xsl:text> </xsl:text>
            <xsl:value-of select="cdl:fix-terms($terms[position() > 1], $spelling)"/>
         </xsl:if>
      </xsl:if>
   </xsl:function>
   
   <!-- ====================================================================== -->
   <!-- Format Terms with Spelling Corrections                                 -->
   <!-- ====================================================================== -->
   
   <!-- term -->
   <xsl:template match="term" mode="spelling">
      <font color="red">
         <xsl:value-of select="cdl:fix-terms(., //spelling)"/>
      </font>
   </xsl:template>
   
   <!-- phrase -->
   <xsl:template match="phrase" mode="spelling">
      <xsl:text>&quot;</xsl:text>
      <font color="red">
         <xsl:value-of select="cdl:fix-terms(term, //spelling)"/>
      </font>
      <xsl:text>&quot;</xsl:text>
   </xsl:template>
   
   <!-- exact -->
   <xsl:template match="exact" mode="spelling">
      <xsl:text>'</xsl:text>
      <font color="red"><xsl:value-of select="cdl:fix-terms(term, //spelling)"/></font>
      <xsl:text>'</xsl:text>
   </xsl:template>
   
   <!-- range -->
   <xsl:template match="upper" mode="spelling">
      <xsl:if test="../lower != .">
         <xsl:text> - </xsl:text>
         <xsl:apply-templates mode="spelling"/>
      </xsl:if>
   </xsl:template>
   
   <!-- ignore 'all' portion of query -->
   <xsl:template match="*[@field='all']" mode="spelling"/>
   
   <!-- ignore text supplement to 'keyword' query -->
   <xsl:template match="and[@field='text' and parent::*/and/@fields]" mode="spelling"/>
   
</xsl:stylesheet>
