<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
   xmlns:parse="http://cdlib.org/xtf/parse"
   xmlns:xlink="http://www.w3.org/TR/xlink/"
   xmlns:xtf="http://cdlib.org/xtf"
   exclude-result-prefixes="#all">
   
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
   
   <!-- ====================================================================== -->
   <!-- Import Common Templates and Functions                                  -->
   <!-- ====================================================================== -->
   
   <xsl:import href="../common/preFilterCommon.xsl"/>
   
   <!-- ====================================================================== -->
   <!-- Output parameters                                                      -->
   <!-- ====================================================================== -->
   
   <xsl:output method="xml" 
      indent="yes" 
      encoding="UTF-8"/>
   
   <!-- ====================================================================== -->
   <!-- Default: identity transformation                                       -->
   <!-- ====================================================================== -->
   
   <xsl:template match="@*|node()">
      <xsl:copy>
         <xsl:apply-templates select="@*|node()"/>
      </xsl:copy>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Root Template                                                          -->
   <!-- ====================================================================== -->
   
   <xsl:template match="/*">
      <xsl:copy>
         <xsl:namespace name="xtf" select="'http://cdlib.org/xtf'"/>
         <xsl:copy-of select="@*"/>
         <xsl:call-template name="get-meta"/>
         <xsl:apply-templates/>
      </xsl:copy>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- NLM Indexing                                                           -->
   <!-- ====================================================================== -->
   
   <!-- Ignored Elements -->
   <xsl:template match="journal-meta">
      <xsl:copy>
         <xsl:copy-of select="@*"/>
         <xsl:attribute name="xtf:index" select="'no'"/>
         <xsl:apply-templates/>
      </xsl:copy>
   </xsl:template>
   
   <xsl:template match="article-meta">
      <xsl:copy>
         <xsl:copy-of select="@*"/>
         <xsl:attribute name="xtf:index" select="'no'"/>
         <xsl:apply-templates/>
      </xsl:copy>
   </xsl:template>
   
   
   <!-- sectionType Indexing and Element Boosting -->
   <xsl:template match="title[parent::sec]">
      <xsl:copy>
         <xsl:copy-of select="@*"/>
         <xsl:attribute name="xtf:sectionType" select="concat('head ', @type)"/>
         <xsl:attribute name="xtf:wordBoost" select="2.0"/>
         <xsl:apply-templates/>
      </xsl:copy>
   </xsl:template>
   
   <xsl:template match="citation">
      <xsl:copy>
         <xsl:copy-of select="@*"/>
         <xsl:attribute name="xtf:sectionType" select="'citation'"/>
         <xsl:apply-templates/>
      </xsl:copy>
   </xsl:template>
   
   <xsl:template match="article-title[parent::title-group]">
      <xsl:copy>
         <xsl:copy-of select="@*"/>
         <xsl:attribute name="xtf:wordBoost" select="100.0"/>
         <xsl:apply-templates/>
      </xsl:copy>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Metadata Indexing                                                      -->
   <!-- ====================================================================== -->
   
   <xsl:template name="get-meta">
      <!-- Access Dublin Core Record (if present) -->
      <xsl:variable name="dcMeta">
         <xsl:call-template name="get-dc-meta"/>
      </xsl:variable>
      
      <!-- If no Dublin Core present, then extract meta-data from the NLM -->
      <xsl:variable name="meta">
         <xsl:choose>
            <xsl:when test="$dcMeta/*">
               <xsl:copy-of select="$dcMeta"/>
            </xsl:when>
            <xsl:otherwise>
               <xsl:call-template name="get-nlm-title"/>
               <xsl:call-template name="get-nlm-creator"/>
               <xsl:call-template name="get-nlm-subject"/>
               <xsl:call-template name="get-nlm-description"/>
               <xsl:call-template name="get-nlm-publisher"/>
               <xsl:call-template name="get-nlm-contributor"/>
               <xsl:call-template name="get-nlm-date"/>
               <xsl:call-template name="get-nlm-type"/>
               <xsl:call-template name="get-nlm-format"/>
               <xsl:call-template name="get-nlm-identifier"/>
               <xsl:call-template name="get-nlm-source"/>
               <xsl:call-template name="get-nlm-language"/>
               <xsl:call-template name="get-nlm-relation"/>
               <xsl:call-template name="get-nlm-coverage"/>
               <xsl:call-template name="get-nlm-rights"/>
               <!-- special values for OAI -->
               <xsl:call-template name="oai-datestamp"/>
               <xsl:call-template name="oai-set"/>
            </xsl:otherwise>
         </xsl:choose>
      </xsl:variable>
      
      <!-- Add doc kind and sort fields to the data, and output the result. -->
      <xsl:call-template name="add-fields">
         <xsl:with-param name="display" select="'dynaxml'"/>
         <xsl:with-param name="meta" select="$meta"/>
      </xsl:call-template>    
   </xsl:template>
   
   <!-- title -->
   <xsl:template name="get-nlm-title">
      <xsl:choose>
         <xsl:when test="/article/front/article-meta/title-group/article-title">
            <title xtf:meta="true">
               <xsl:value-of select="string(/article/front/article-meta/title-group/article-title[1])"/>
            </title>
         </xsl:when>
         <xsl:otherwise>
            <title xtf:meta="true">
               <xsl:value-of select="'unknown'"/>
            </title>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- creator -->
   <xsl:template name="get-nlm-creator">
      <xsl:choose>
         <xsl:when test="/article/front/article-meta/contrib-group/contrib/name">
            <xsl:for-each select="/article/front/article-meta/contrib-group/contrib/name">
               <creator xtf:meta="true">
                  <xsl:value-of select="surname"/>
                  <xsl:text>, </xsl:text>
                  <xsl:value-of select="given-names"/>
               </creator>
            </xsl:for-each>
         </xsl:when>
         <xsl:otherwise>
            <creator xtf:meta="true">
               <xsl:value-of select="'unknown'"/>
            </creator>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- subject -->
   <xsl:template name="get-nlm-subject">
      <xsl:choose>
         <xsl:when test="/article/front/article-meta/article-categories/subj-group">
            <xsl:for-each select="/article/front/article-meta/article-categories/subj-group">
               <subject xtf:meta="true">
                  <xsl:value-of select="subject"/>
                  <xsl:if test="subj-group">
                     <xsl:text>: </xsl:text>
                     <xsl:value-of select="subj-group/subject"/>
                  </xsl:if>
               </subject>
            </xsl:for-each>
         </xsl:when>
      </xsl:choose>
   </xsl:template>
   
   <!-- description -->
   <xsl:template name="get-nlm-description">
      <xsl:choose>
         <xsl:when test="/article/front/article-meta/abstract">
            <description xtf:meta="true">
               <xsl:value-of select="string(/article/front/article-meta/abstract[1])"/>
            </description>
         </xsl:when>
         <xsl:otherwise>
            <description xtf:meta="true">
               <xsl:value-of select="/article/body/p[1]"/>
            </description>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- publisher -->
   <xsl:template name="get-nlm-publisher">
      <xsl:choose>
         <xsl:when test="/article/front/journal-meta/publisher">
            <publisher xtf:meta="true">
               <xsl:value-of select="string(/article/front/journal-meta/publisher[1])"/>
            </publisher>
         </xsl:when>
         <xsl:otherwise>
            <publisher xtf:meta="true">
               <xsl:value-of select="'unknown'"/>
            </publisher>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- contributor -->
   <xsl:template name="get-nlm-contributor">
      <xsl:choose>
         <xsl:when test="/article/front/article-meta/contrib-group/contrib/name">
            <xsl:for-each select="/article/front/article-meta/contrib-group/contrib/name">
               <contributor xtf:meta="true">
                  <xsl:value-of select="surname"/>
                  <xsl:text>, </xsl:text>
                  <xsl:value-of select="given-names"/>
               </contributor>
            </xsl:for-each>
         </xsl:when>
         <xsl:otherwise>
            <contributor xtf:meta="true">
               <xsl:value-of select="'unknown'"/>
            </contributor>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- date -->
   <xsl:template name="get-nlm-date">
      <xsl:choose>
         <xsl:when test="/article/front/article-meta/pub-date[@pub-type='pub']">
            <date xtf:meta="true">
               <xsl:value-of select="/article/front/article-meta/pub-date[@pub-type='pub'][1]/day"/>
               <xsl:text>-</xsl:text>
               <xsl:value-of select="/article/front/article-meta/pub-date[@pub-type='pub'][1]/month"/>
               <xsl:text>-</xsl:text>
               <xsl:value-of select="/article/front/article-meta/pub-date[@pub-type='pub'][1]/year"/>
            </date>
         </xsl:when>
         <xsl:when test="/article/front/article-meta/pub-date">
            <date xtf:meta="true">
               <xsl:value-of select="/article/front/article-meta/pub-date[1]/day"/>
               <xsl:text>-</xsl:text>
               <xsl:value-of select="/article/front/article-meta/pub-date[1]/month"/>
               <xsl:text>-</xsl:text>
               <xsl:value-of select="/article/front/article-meta/pub-date[1]/year"/>
            </date>
         </xsl:when>
         <xsl:otherwise>
            <date xtf:meta="true">
               <xsl:value-of select="'unknown'"/>
            </date>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- type -->
   <xsl:template name="get-nlm-type">
      <type xtf:meta="true">nlm</type>
   </xsl:template>
   
   <!-- format -->
   <xsl:template name="get-nlm-format">
      <format xtf:meta="true">xml</format>
   </xsl:template>
   
   <!-- identifier -->
   <xsl:template name="get-nlm-identifier">
      <xsl:choose>
         <xsl:when test="/article/front/article-meta/article-id[@pub-id-type='doi']">
            <identifier xtf:meta="true" xtf:tokenize="no">
               <xsl:value-of select="replace(string(/article/front/article-meta/article-id[@pub-id-type='doi'][1]),'^.+/','')"/>
            </identifier>
         </xsl:when>
         <xsl:when test="/article/front/article-meta/article-id">
            <identifier xtf:meta="true" xtf:tokenize="no">
               <xsl:value-of select="replace(string(/article/front/article-meta/article-id[1]),'^.+/','')"/>
            </identifier>
         </xsl:when>
         <xsl:otherwise>
            <identifier xtf:meta="true" xtf:tokenize="no">
               <xsl:value-of select="'unknown'"/>
            </identifier>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- source -->
   <xsl:template name="get-nlm-source">
      <xsl:choose>
         <xsl:when test="/article/front/journal-meta/journal-title">
            <source xtf:meta="true">
               <xsl:value-of select="string(/article/front/journal-meta/journal-title[1])"/>
            </source>
         </xsl:when>
         <xsl:otherwise>
            <source xtf:meta="true">
               <xsl:value-of select="'unknown'"/>
            </source>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- language -->
   <xsl:template name="get-nlm-language">
      <language xtf:meta="true">english</language>
   </xsl:template>
   
   <xsl:template name="get-nlm-relation">
      <xsl:choose>
         <xsl:when test="/article/front/article-meta/ext-link">
            <relation xtf:meta="true">
               <xsl:value-of select="string(/article/front/article-meta/ext-link[1]/@xlink:href)"/>
            </relation>
         </xsl:when>
         <xsl:otherwise>
            <relation xtf:meta="true">
               <xsl:value-of select="'unknown'"/>
            </relation>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- coverage -->
   <xsl:template name="get-nlm-coverage">
      <coverage xtf:meta="true">unknown</coverage>
   </xsl:template>
   
   <!-- rights -->
   <xsl:template name="get-nlm-rights">
      <xsl:choose>
         <xsl:when test="/article/front/article-meta/permissions">
            <rights xtf:meta="true">
               <xsl:value-of select="string(/article/front/article-meta/permissions[1])"/>
            </rights>
         </xsl:when>
         <xsl:otherwise>
            <rights xtf:meta="true">
               <xsl:value-of select="'unknown'"/>
            </rights>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- OAI dateStamp -->
   <xsl:template name="oai-datestamp">
      <dateStamp xtf:meta="true" xtf:tokenize="no">
         <xsl:choose>
            <xsl:when test="/article/front/article-meta/pub-date[@pub-type='epub']">
               <xsl:value-of select="/article/front/article-meta/pub-date[@pub-type='epub'][1]/year"/>
               <xsl:text>-</xsl:text>
               <xsl:value-of select="/article/front/article-meta/pub-date[@pub-type='epub'][1]/month"/>
               <xsl:text>-</xsl:text>
               <xsl:value-of select="/article/front/article-meta/pub-date[@pub-type='epub'][1]/day"/>
            </xsl:when>
            <xsl:when test="/article/front/article-meta/pub-date[@pub-type='pub']">
               <xsl:value-of select="/article/front/article-meta/pub-date[@pub-type='pub'][1]/year"/>
               <xsl:text>-</xsl:text>
               <xsl:value-of select="/article/front/article-meta/pub-date[@pub-type='pub'][1]/month"/>
               <xsl:text>-</xsl:text>
               <xsl:value-of select="/article/front/article-meta/pub-date[@pub-type='pub'][1]/day"/>
            </xsl:when>
            <xsl:when test="/article/front/article-meta/pub-date">
               <xsl:value-of select="/article/front/article-meta/pub-date[1]/year"/>
               <xsl:text>-</xsl:text>
               <xsl:value-of select="/article/front/article-meta/pub-date[1]/month"/>
               <xsl:text>-</xsl:text>
               <xsl:value-of select="/article/front/article-meta/pub-date[1]/day"/>
            </xsl:when>
            <xsl:otherwise>
               <!-- I don't know, what would you put? -->
               <xsl:value-of select="'1950-01-01'"/>
            </xsl:otherwise>
         </xsl:choose>
      </dateStamp>
   </xsl:template>
   
   <!-- OAI sets -->
   <xsl:template name="oai-set">
      <xsl:for-each select="/article/front/article-meta/article-categories/subj-group">
         <set xtf:meta="true">
            <xsl:value-of select="subject"/>
            <xsl:if test="subj-group">
               <xsl:text>: </xsl:text>
               <xsl:value-of select="subj-group/subject"/>
            </xsl:if>
         </set>
      </xsl:for-each>
      <set xtf:meta="true">
         <xsl:value-of select="'public'"/>
      </set>
   </xsl:template>
   
</xsl:stylesheet>
