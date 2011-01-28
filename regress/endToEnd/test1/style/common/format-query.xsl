<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:xtf="http://cdlib.org/xtf" 
   xmlns:editURL="http://cdlib.org/xtf/editURL" 
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
      Stylesheet used for reformatting the query structure into a user-friendly 
      string for display.  
   -->
   
   <!-- ====================================================================== -->
   <!-- Local Parameters                                                       -->
   <!-- ====================================================================== -->
   
   <!-- hidden queries -->
   <xsl:param name="noShow" select="'all|display|browse-[a-z]+'"/>
   
   <!-- ====================================================================== -->
   <!-- Format Query for Display                                               -->
   <!-- ====================================================================== -->
   
   <!-- main template -->
   <xsl:template name="format-query">
      
      <xsl:choose>
         <xsl:when test="$browse-all">
            <div class="subQuery">&#160;All</div>
         </xsl:when>
         <xsl:otherwise>
            <div class="subQuery">
               <xsl:apply-templates select="query" mode="query"/>
            </div>
         </xsl:otherwise>
      </xsl:choose>
      
   </xsl:template>
   
   <!-- and|or|exact|near|range operators -->
   <xsl:template match="and|or|exact|near|range" mode="query">
      <!-- field -->
      <xsl:variable name="field" select="if (@field) then @field else 'keyword'"/> 
      <!-- terms -->
      <xsl:variable name="terms">
         <xsl:for-each select=".//term/text()">
            <xsl:value-of select="editURL:escapeRegex(editURL:protectValue(.))"/>
            <text>[+ ]?</text>
         </xsl:for-each>
      </xsl:variable>
      <!-- query removal url -->
      <xsl:variable name="v1" select="editURL:remove($queryString, concat('(smode|expand|', $field, ')'))"/>
      <xsl:variable name="v2" select="if (matches($field,'text|query')) then editURL:remove($v1, 'sectionType') else $v1"/>
      <xsl:variable name="v3" select="editURL:remove($v2, concat(replace($field, 'facet-', 'f[0-9]+-'),'=',$terms))"/>
      <xsl:variable name="removeString" select="editURL:replaceEmpty($v3, 'browse-all=yes')"/>
      
      <!-- If unchanged, it probably means there's a 'freeform' query that we don't understand,
           so remove that.
      -->
      <xsl:variable name="finalString" select="if ($removeString = $queryString)
         then replace($removeString, 'freeformQuery=[^=;]+', '')
         else $removeString"/>
      
      <xsl:choose>
         <!-- hidden queries -->
         <xsl:when test="matches(@field,$noShow)"/>
         
         <!-- 'text' supplement to keyword queries -->
         <xsl:when test="@field='text' and parent::*[1]/and/@fields"/>
         
         <!-- a query at this level -->
         <xsl:when test="@field or @fields or not/@field">
            <!-- query -->
            <xsl:apply-templates mode="query"/>
            <xsl:text> in </xsl:text>
            <!-- field -->
            <b>
               <xsl:choose>
                  <xsl:when test="@fields">
                     <xsl:text> keywords</xsl:text>
                  </xsl:when>
                  <xsl:when test="child::sectionType">
                     <xsl:value-of select="sectionType/term"/>
                     <xsl:text> sections</xsl:text>
                  </xsl:when>
                  <xsl:when test="@field = 'text'">
                     <xsl:text> the full text </xsl:text>
                  </xsl:when>
                  <xsl:when test="not(@field) and not[@field]">
                     <xsl:value-of select="not/@field"/>
                  </xsl:when>
                  <xsl:otherwise>
                     <!-- mask facets -->
                     <xsl:value-of select="replace(replace(replace(@field,'facet-',''),'subject','subject'),'date','date')"/>
                  </xsl:otherwise>
               </xsl:choose>
            </b>
            <xsl:text>&#160;</xsl:text>
            <!-- query removal widget -->
            <a href="{$xtfURL}{$crossqueryPath}?{editURL:clean($finalString)}">[X]</a>
            <br/>
         </xsl:when>
         
         <!-- keep searching down until we find the query -->
         <xsl:otherwise>
            <xsl:apply-templates mode="query"/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- term -->
   <xsl:template match="term" mode="query" priority="-1">
      <xsl:if test="preceding-sibling::term and (. != $keyword)">
         <xsl:value-of select="name(..)"/>
         <!--<xsl:text>&#160;</xsl:text>-->
      </xsl:if>
      <span class="subhit">
         <xsl:value-of select="."/>
      </span>
      <!--<xsl:text>&#160;</xsl:text>-->
   </xsl:template>
   
   <!-- phrase -->
   <xsl:template match="phrase" mode="query">
      <span class="subhit">
         <xsl:text>&quot;</xsl:text>
         <xsl:value-of select="term"/>
         <xsl:text>&quot;</xsl:text>
      </span>
   </xsl:template>
   
   <!-- exact -->
   <xsl:template match="exact" mode="query">
      <span class="subhit">
         <xsl:text>'</xsl:text>
         <xsl:value-of select="term"/>
         <xsl:text>'</xsl:text>
      </span>
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
