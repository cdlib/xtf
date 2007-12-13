<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
        xmlns:saxon="http://saxon.sf.net/"
        xmlns:xtf="http://cdlib.org/xtf"
        xmlns:parse="http://cdlib.org/xtf/parse"
        exclude-result-prefixes="#all">

<!--
   Copyright (c) 2005, Regents of the University of California
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
   <!-- EAD Indexing                                                           -->
   <!-- ====================================================================== -->
   
   <!-- Ignored Elements -->
   
   <xsl:template match="eadheader">
      <xsl:copy>
         <xsl:copy-of select="@*"/>
         <xsl:attribute name="xtf:index" select="'no'"/>
         <xsl:apply-templates/>
      </xsl:copy>
   </xsl:template>
   
   
   <!-- sectionType Indexing and Element Boosting -->
   
   <xsl:template match="unittitle[parent::did]">
      <xsl:copy>
         <xsl:copy-of select="@*"/>
         <xsl:attribute name="xtf:sectionType" select="concat('head ', @type)"/>
         <xsl:attribute name="xtf:wordBoost" select="2.0"/>
         <xsl:apply-templates/>
      </xsl:copy>
   </xsl:template>
   
   <xsl:template match="titleproper[parent::titlestmt]">
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
      
      <!-- If no Dublin Core present, then extract meta-data from the EAD -->
      <xsl:variable name="meta">
         <xsl:choose>
            <xsl:when test="$dcMeta/*">
               <xsl:copy-of select="$dcMeta"/>
            </xsl:when>
            <xsl:otherwise>
               <xsl:call-template name="get-ead-title"/>
               <xsl:call-template name="get-ead-creator"/>
               <xsl:call-template name="get-ead-subject"/>
               <xsl:call-template name="get-ead-description"/>
               <xsl:call-template name="get-ead-publisher"/>
               <xsl:call-template name="get-ead-contributor"/>
               <xsl:call-template name="get-ead-date"/>
               <xsl:call-template name="get-ead-type"/>
               <xsl:call-template name="get-ead-format"/>
               <xsl:call-template name="get-ead-identifier"/>
               <xsl:call-template name="get-ead-source"/>
               <xsl:call-template name="get-ead-language"/>
               <xsl:call-template name="get-ead-relation"/>
               <xsl:call-template name="get-ead-coverage"/>
               <xsl:call-template name="get-ead-rights"/>
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
   <xsl:template name="get-ead-title">
      <xsl:choose>
         <xsl:when test="/ead/archdesc/did/unittitle">
            <title xtf:meta="true">
               <xsl:value-of select="/ead/archdesc/did/unittitle"/>
            </title>
         </xsl:when>
         <xsl:when test="/ead/eadheader/filedesc/titlestmt/titleproper">
            <xsl:variable name="titleproper" select="string(/ead/eadheader/filedesc/titlestmt/titleproper)"/>
            <xsl:variable name="subtitle" select="string(/ead/eadheader/filedesc/titlestmt/subtitle)"/>
            <title xtf:meta="true">
               <xsl:value-of select="$titleproper"/>
               <xsl:if test="$subtitle">
                  <!-- Put a colon between main and subtitle, if none present already -->
                  <xsl:if test="not(matches($titleproper, ':\s*$') or matches($subtitle, '^\s*:'))">
                     <xsl:text>: </xsl:text>
                  </xsl:if>  
                  <xsl:value-of select="$subtitle"/>
               </xsl:if>
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
   <xsl:template name="get-ead-creator">
      <xsl:choose>
         <xsl:when test="/ead/archdesc/did/origination[starts-with(@label, 'Creator')]">
            <creator xtf:meta="true">
               <xsl:value-of select="string(/ead/archdesc/did/origination[@label, 'Creator'][1])"/>
            </creator>
         </xsl:when>
         <xsl:when test="/ead/eadheader/filedesc/titlestmt/author">
            <creator xtf:meta="true">
               <xsl:value-of select="string(/ead/eadheader/filedesc/titlestmt/author[1])"/>
            </creator>
         </xsl:when>
         <xsl:otherwise>
            <creator xtf:meta="true">
               <xsl:value-of select="'unknown'"/>
            </creator>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- subject --> 
   <xsl:template name="get-ead-subject">
      <xsl:choose>
         <xsl:when test="/ead/archdesc//controlaccess/subject">
            <xsl:for-each select="/ead/archdesc//controlaccess/subject">
               <subject xtf:meta="true">
                  <xsl:value-of select="."/>
               </subject>
            </xsl:for-each>
         </xsl:when>
         <xsl:when test="/ead/eadheader/filedesc/notestmt/subject">
            <xsl:for-each select="/ead/eadheader/filedesc/notestmt/subject">
               <subject xtf:meta="true">
                  <xsl:value-of select="."/>
               </subject>
            </xsl:for-each>
         </xsl:when>
      </xsl:choose>
   </xsl:template>
   
   <!-- description --> 
   <xsl:template name="get-ead-description">
      <xsl:choose>
         <xsl:when test="/ead/archdesc/did/abstract">
            <description xtf:meta="true">
               <xsl:value-of select="string(/ead/archdesc/did/abstract[1])"/>
            </description>
         </xsl:when>
         <xsl:when test="/ead/eadheader/filedesc/notestmt/note">
            <description xtf:meta="true">
               <xsl:value-of select="string(/ead/eadheader/filedesc/notestmt/note[1])"/>
            </description>
         </xsl:when>
      </xsl:choose>
   </xsl:template>
   
   <!-- publisher -->
   <xsl:template name="get-ead-publisher">
      <xsl:choose>
         <xsl:when test="/ead/archdesc/did/repository">
            <publisher xtf:meta="true">
               <xsl:value-of select="string(/ead/archdesc/did/repository[1])"/>
            </publisher>
         </xsl:when>
         <xsl:when test="/ead/eadheader/filedesc/publicationstmt/publisher">
            <publisher xtf:meta="true">
               <xsl:value-of select="string(/ead/eadheader/filedesc/publicationstmt/publisher[1])"/>
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
   <xsl:template name="get-ead-contributor">
      <xsl:choose>
         <xsl:when test="/ead/eadheader/filedesc/titlestmt/author">
            <contributor xtf:meta="true">
               <xsl:value-of select="string(/ead/eadheader/filedesc/titlestmt/author[1])"/>
            </contributor>
         </xsl:when>
         <xsl:otherwise>
            <contributor xtf:meta="true">
               <xsl:value-of select="'unknown'"/>
            </contributor>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- date --> 
   <xsl:template name="get-ead-date">
      <xsl:choose>
         <xsl:when test="/ead/eadheader/filedesc/publicationstmt/date">
            <date xtf:meta="true">
               <xsl:value-of select="string(/ead/eadheader/filedesc/publicationstmt/date[1])"/>
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
   <xsl:template name="get-ead-type">
      <type xtf:meta="true">ead</type>
   </xsl:template>
   
   <!-- format -->
   <xsl:template name="get-ead-format">
      <format xtf:meta="true">xml</format>
   </xsl:template>
   
   <!-- identifier --> 
   <xsl:template name="get-ead-identifier">
      <xsl:choose>
         <xsl:when test="/ead/archdesc/did/unitid">
            <identifier xtf:meta="true" xtf:tokenize="no">
               <xsl:value-of select="string(/ead/archdesc/did/unitid[1])"/>
            </identifier>
         </xsl:when>
         <xsl:when test="/ead/eadheader/eadid" xtf:tokenize="no">
            <identifier xtf:meta="true" xtf:tokenize="no">
               <xsl:value-of select="string(/ead/eadheader/eadid[1])"/>
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
   <xsl:template name="get-ead-source">
      <source xtf:meta="true">unknown</source>
   </xsl:template>
   
   <!-- language -->
   <xsl:template name="get-ead-language">
      <xsl:choose>
         <xsl:when test="/ead/eadheader/profiledesc/langusage/language">
            <language xtf:meta="true">
               <xsl:value-of select="string(/ead/eadheader/profiledesc/langusage/language[1])"/>
            </language>
         </xsl:when>
         <xsl:otherwise>
            <language xtf:meta="true">
               <xsl:value-of select="'english'"/>
            </language>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- relation -->
   <xsl:template name="get-ead-relation">
      <relation xtf:meta="true">unknown</relation>
   </xsl:template>
   
   <!-- coverage -->
   <xsl:template name="get-ead-coverage">
      <xsl:choose>
         <xsl:when test="/ead/archdesc/did/unittitle/unitdate">
            <coverage xtf:meta="true">
               <xsl:value-of select="string(/ead/archdesc/did/unittitle/unitdate[1])"/>
            </coverage>
         </xsl:when>
         <xsl:otherwise>
            <coverage xtf:meta="true">
               <xsl:value-of select="'unknown'"/>
            </coverage>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- rights -->
   <xsl:template name="get-ead-rights">
      <rights xtf:meta="true">public</rights>
   </xsl:template>
   
   <xsl:template name="oai-datestamp">
      <dateStamp xtf:meta="true" xtf:tokenize="no">
         <xsl:choose>
            <xsl:when test="/ead/eadheader/filedesc/publicationstmt/date">
               <xsl:value-of select="concat(parse:year(string(/ead/eadheader/filedesc/publicationstmt/date[1])),'-01-01')"/>
            </xsl:when>
            <xsl:otherwise>
               <!-- I don't know, what would you put? -->
               <xsl:value-of select="'1950-01-01'"/>
            </xsl:otherwise>
         </xsl:choose>
      </dateStamp>
   </xsl:template>
   
   <xsl:template name="oai-set">
      <xsl:for-each select="/ead/archdesc//controlaccess/subject">
         <set xtf:meta="true">
            <xsl:value-of select="."/>
         </set>
      </xsl:for-each>
      <xsl:for-each select="/ead/eadheader/filedesc/notestmt/subject">
         <set xtf:meta="true">
            <xsl:value-of select="."/>
         </set>
      </xsl:for-each>
      <set xtf:meta="true">
         <xsl:value-of select="'public'"/>
      </set>
   </xsl:template>
   
</xsl:stylesheet>
