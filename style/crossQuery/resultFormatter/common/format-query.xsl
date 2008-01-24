<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:xtf="http://cdlib.org/xtf" 
   xmlns:editURL="http://cdlib.org/xtf/editURL" 
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
      Stylesheet used for reformatting the query structure into a user-friendly 
      string for display.  
   -->
   
   <!-- ====================================================================== -->
   <!-- Local Parameters                                                       -->
   <!-- ====================================================================== -->
   
   <xsl:param name="noShow" select="'all|display|browse-[a-z]+'"/>
   
   <!-- ====================================================================== -->
   <!-- Format Query for Display                                               -->
   <!-- ====================================================================== -->
   
   <!-- main template -->
   <xsl:template name="format-query">
      
      <xsl:choose>
         <!-- keyword queries get special handling -->
         <xsl:when test="$browse-all">All</xsl:when>
         <xsl:when test="$keyword">
            <xsl:apply-templates select="(query//*[@fields])[1]/*" mode="query"/>
            <xsl:choose>
               <!-- if they did not search all fields, tell them which are in the fieldList -->
               <xsl:when test="$fieldList">
                  <xsl:text> in </xsl:text>
                  <b>
                     <xsl:value-of select="replace($fieldList,'\s',', ')"/>
                  </b>
               </xsl:when>
               <xsl:otherwise>
                  <xsl:text> in </xsl:text>
                  <b> keywords</b>
               </xsl:otherwise>
            </xsl:choose>
            <!-- query removal checkbox -->
            <xsl:text>&#160;</xsl:text>
            <a href="{$xtfURL}{$crossqueryPath}?{editURL:set(editURL:remove($queryString,'keyword'),'browse-all','yes')}">[X]</a>
            <br/>
            <xsl:apply-templates select="query" mode="query"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:apply-templates select="query" mode="query"/>
         </xsl:otherwise>
      </xsl:choose>
      
   </xsl:template>
   
   <!-- and|or|exact|near|range operators -->
   <xsl:template match="and|or|exact|near|range" mode="query">
      <xsl:variable name="pos" select="count(preceding-sibling::*[not(matches(@field,$noShow))]) + 1"/>
      <xsl:choose>
         <xsl:when test="@fields"/><!-- keyword -->
         <xsl:when test="matches(@field,$noShow)"/>
         <xsl:when test="@field"> 
            <xsl:apply-templates mode="query"/>
            <xsl:text> in </xsl:text>
            <b>
               <xsl:choose>
                  <xsl:when test="child::sectionType">
                     <xsl:value-of select="sectionType/term"/>
                     <xsl:text> sections</xsl:text>
                  </xsl:when>
                  <xsl:when test="@field = 'text'">
                     <xsl:text> the full text </xsl:text>
                  </xsl:when>
                  <xsl:otherwise>
                     <xsl:value-of select="replace(@field,'facet-','')"/>
                  </xsl:otherwise>
               </xsl:choose>
            </b>
            <xsl:text> </xsl:text>
            <xsl:variable name="field" select="@field"/> 
            <xsl:variable name="terms">
               <xsl:for-each select=".//term/text()">
                  <xsl:value-of select="."/>
                  <text>%20</text>
               </xsl:for-each>
            </xsl:variable>
            <xsl:variable name="removeString">
               <xsl:analyze-string select="$queryString" regex="([^=;]+)=([^;]+)">
                  <xsl:matching-substring>
                     <xsl:variable name="param" select="regex-group(1)"/>
                     <xsl:variable name="value" select="regex-group(2)"/>
                     <xsl:choose>
                        <!-- things to remove -->
                        <xsl:when test="matches($param,$field)"/>
                        <xsl:when test="matches($field,replace($param,'f[0-9]','facet')) and matches($terms,$value)"/>
                        <xsl:when test="matches($param,'sectionType') and matches($field,'text|query')"/>
                        <xsl:when test="matches($param,'expand')"/>
                        <!-- keep everything else -->
                        <xsl:otherwise>
                           <xsl:value-of select="."/>
                        </xsl:otherwise>
                     </xsl:choose>
                  </xsl:matching-substring>
                  <xsl:non-matching-substring>
                     <xsl:value-of select="."/>
                  </xsl:non-matching-substring>
               </xsl:analyze-string>
            </xsl:variable>
            <!-- if only facets remain (or nothing remains), add "browse-all=yes" to the query -->
            <xsl:variable name="finalString" select="if (matches(editURL:clean($removeString), '^(f[0-9]+-[^;]+;?)*$')) then concat('browse-all=yes;', $removeString) else $removeString"/>
            <a href="{$xtfURL}{$crossqueryPath}?{editURL:clean($finalString)}">[X]</a>
            <br/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:apply-templates mode="query"/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- term -->
   <xsl:template match="term" mode="query">
      <xsl:if test="preceding-sibling::term and (. != $keyword)">
         <xsl:value-of select="name(..)"/>
         <xsl:text> </xsl:text>
      </xsl:if>
      <span class="subhit">
         <xsl:value-of select="."/>
      </span>
      <xsl:text> </xsl:text>
   </xsl:template>
   
   <!-- phrase -->
   <xsl:template match="phrase" mode="query">
      <xsl:text>&quot;</xsl:text>
      <span class="subhit">
         <xsl:value-of select="term"/>
      </span>
      <xsl:text>&quot;</xsl:text>
   </xsl:template>
   
   <!-- exact -->
   <xsl:template match="exact" mode="query">
      <xsl:text>'</xsl:text>
      <span class="subhit">
         <xsl:value-of select="term"/>
      </span>
      <xsl:text>'</xsl:text>
      <xsl:if test="@field">
         <xsl:text> in </xsl:text>
         <b><xsl:value-of select="@field"/></b>
      </xsl:if>
   </xsl:template>
   
   <!-- range queries -->
   <xsl:template match="lower" mode="query">
      <span class="subhit">
         <xsl:value-of select="."/>
      </span>
   </xsl:template>
   
   <xsl:template match="upper" mode="query">
      <xsl:if test="../lower != .">
         <xsl:text> - </xsl:text>
         <span class="subhit">
            <xsl:value-of select="."/>
         </span>
      </xsl:if>
   </xsl:template>
   
   <!-- not -->
   <xsl:template match="not" mode="query">
      <xsl:text> not </xsl:text>
      <xsl:apply-templates mode="query"/>
   </xsl:template>
   
   <!-- hide sectionType -->
   <xsl:template match="sectionType" mode="query"/>
   
</xsl:stylesheet>
