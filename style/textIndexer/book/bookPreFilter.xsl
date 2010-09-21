<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
   xmlns:parse="http://cdlib.org/xtf/parse"
   xmlns:xtf="http://cdlib.org/xtf"
   xmlns:METS="http://www.loc.gov/METS/"
   xmlns:scribe="http://archive.org/scribe/xml"
   xmlns:local="http://cdlib.org/local"
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
   <!-- Global variables                                                       -->
   <!-- ====================================================================== -->
   
   <xsl:variable name="metsMeta" select="//METS:xmlData/metadata"/>
   
   <xsl:variable name="pageAssertions">
      <xsl:for-each select="//scribe:pageNumData/scribe:assertion">
         <assertion pageNum="scribe:pageNum/string()" leafNum="scribe:leafNum/string()"/>
      </xsl:for-each>
   </xsl:variable>
   
   <xsl:variable name="numPages" select="count(//scribe:pageData/scribe:page)"/>
      
   <xsl:variable name="leafToPage" select="local:makeLeafToPage(1, 0, 1, $pageAssertions)"/>
   
   <!-- ====================================================================== -->
   <!-- Keys for speedy processing                                             -->
   <!-- ====================================================================== -->
   
   <xsl:key name="leafToPage" 
            match="//scribe:pageNumData/scribe:assertion" 
            use="scribe:leafNum/string()"/>
   
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
      <xtf-converted-book>
         <xsl:namespace name="xtf" select="'http://cdlib.org/xtf'"/>
         <xsl:call-template name="get-meta"/>
         <xsl:message>
            Pages:
            <xsl:call-template name="convertPages"/>
         </xsl:message>
         <xsl:call-template name="convertPages"/>
      </xtf-converted-book>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Book METS Indexing                                                     -->
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
      
      <!-- If no Dublin Core present, then extract meta-data from the METS -->
      <xsl:variable name="meta">
         <xsl:choose>
            <xsl:when test="$dcMeta/*">
               <xsl:copy-of select="$dcMeta"/>
            </xsl:when>
            <xsl:otherwise>
               <xsl:call-template name="get-book-title"/>
               <xsl:call-template name="get-book-creator"/>
               <xsl:call-template name="get-book-subject"/>
               <xsl:call-template name="get-book-publisher"/>
               <xsl:call-template name="get-book-contributor"/>
               <xsl:call-template name="get-book-date"/>
               <xsl:call-template name="get-book-type"/>
               <xsl:call-template name="get-book-format"/>
               <xsl:call-template name="get-book-identifier"/>
               <xsl:call-template name="get-book-language"/>
               <xsl:call-template name="get-book-relation"/>
               <xsl:call-template name="get-book-coverage"/>
               <xsl:call-template name="get-book-rights"/>
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
   <xsl:template name="get-book-title">
      <xsl:choose>
         <xsl:when test="$metsMeta/title">
            <title xtf:meta="true">
               <xsl:value-of select="string($metsMeta/title[1])"/>
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
   <xsl:template name="get-book-creator">
      <xsl:choose>
         <xsl:when test="$metsMeta/creator">
            <xsl:for-each select="$metsMeta/creator">
               <creator xtf:meta="true">
                  <xsl:value-of select="."/>
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
   <xsl:template name="get-book-subject">
      <xsl:choose>
         <xsl:when test="$metsMeta/subject">
            <xsl:for-each select="$metsMeta/subject">
               <subject xtf:meta="true">
                  <xsl:value-of select="."/>
               </subject>
            </xsl:for-each>
         </xsl:when>
      </xsl:choose>
   </xsl:template>
   
   <!-- publisher -->
   <xsl:template name="get-book-publisher">
      <xsl:choose>
         <xsl:when test="$metsMeta/publisher">
            <publisher xtf:meta="true">
               <xsl:value-of select="string($metsMeta/publisher[1])"/>
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
   <xsl:template name="get-book-contributor">
      <xsl:choose>
         <xsl:when test="$metsMeta/contributor">
            <xsl:for-each select="$metsMeta/contributor">
               <contributor xtf:meta="true">
                  <xsl:value-of select="."/>
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
   <xsl:template name="get-book-date">
      <xsl:choose>
         <xsl:when test="$metsMeta/date">
            <date xtf:meta="true">
               <xsl:value-of select="string($metsMeta/date[1])"/>
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
   <xsl:template name="get-book-type">
      <type xtf:meta="true">book</type>
   </xsl:template>
   
   <!-- format -->
   <xsl:template name="get-book-format">
      <format xtf:meta="true">xml</format>
   </xsl:template>
   
   <!-- identifier -->
   <xsl:template name="get-book-identifier">
      <xsl:choose>
         <xsl:when test="$metsMeta/identifier">
            <identifier xtf:meta="true" xtf:tokenize="no">
               <xsl:value-of select="string($metsMeta/identifier[1])"/>
            </identifier>
         </xsl:when>
         <xsl:otherwise>
            <identifier xtf:meta="true" xtf:tokenize="no">
               <xsl:value-of select="'unknown'"/>
            </identifier>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- language -->
   <xsl:template name="get-book-language">
      <xsl:choose>
         <xsl:when test="$metsMeta/language">
            <language xtf:meta="true">
               <xsl:value-of select="string($metsMeta/language[1])"/>
            </language>
         </xsl:when>
         <xsl:otherwise>
            <language xtf:meta="true">
               <xsl:value-of select="'unknown'"/>
            </language>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <!-- relation -->
   <xsl:template name="get-book-relation">
      <xsl:choose>
         <xsl:when test="$metsMeta/identifier-access">
            <relation xtf:meta="true">
               <xsl:value-of select="string($metsMeta/identifier-access)"/>
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
   <xsl:template name="get-book-coverage">
      <coverage xtf:meta="true">unknown</coverage>
   </xsl:template>
   
   <!-- rights -->
   <xsl:template name="get-book-rights">
      <xsl:choose>
         <xsl:when test="$metsMeta/copyright-status[string() = 'NOT_IN_COPYRIGHT'] or
                         $metsMeta/possible-copyright-status[string() = 'NOT_IN_COPYRIGHT']">
            <rights xtf:meta="true">
               <xsl:value-of select="'public'"/>
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
            <xsl:when test="$metsMeta/date">
               <xsl:value-of select="concat(parse:year(string($metsMeta/date[1]))[1],'-01-01')"/>
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
      <xsl:for-each select="$metsMeta/subject">
         <set xtf:meta="true">
            <xsl:value-of select="."/>
         </set>
      </xsl:for-each>
      <xsl:if test="$metsMeta/copyright-status[string() = 'NOT_IN_COPYRIGHT'] or
                    $metsMeta/possible-copyright-status[string() = 'NOT_IN_COPYRIGHT']">
         <set xtf:meta="true">
            <xsl:value-of select="'public'"/>
         </set>
      </xsl:if>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Page processing                                                        -->
   <!-- ====================================================================== -->
   
   <xsl:template name="convertPages">
      <xsl:apply-templates select="//scribe:pageData/scribe:page"/>
   </xsl:template>
   
   <xsl:function name="local:makeLeafToPage">
      <xsl:param name="prevLeafNum"/>
      <xsl:param name="isAsserted"/>
      <xsl:param name="prevPageNum"/>
      <xsl:param name="assertions"/>
      
      <xsl:variable name="firstAssertion" select="$assertions/assertion[1]"/>
      <xsl:variable name="otherAssertions" select="$assertions/assertion[position() &gt; 1]"/>
      
      <xsl:message>Assertion: <xsl:copy-of select="$firstAssertion"/></xsl:message>
      
      <xsl:for-each select="$prevLeafNum to $numPages">
         <!-- TODO -->
      </xsl:for-each>
   </xsl:function>
   
   <xsl:template match="scribe:page">
      <xsl:variable name="leafNum" select="number(@leafNum)"/>
      <page leafNum="{@leafNum}">
         <xsl:variable name="pageNum" select="key('leafToPage', @leafNum)//*:pageNum"/>
         <xsl:if test="$pageNum">
            <xsl:attribute name="pageNum" select="$pageNum"/>
         </xsl:if>
      </page>
   </xsl:template>
   
</xsl:stylesheet>
