<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
   xmlns:date="http://exslt.org/dates-and-times"
   xmlns:parse="http://cdlib.org/xtf/parse"
   xmlns:xtf="http://cdlib.org/xtf"
   xmlns:FileUtils="java:org.cdlib.xtf.xslt.FileUtils"
   extension-element-prefixes="date FileUtils"
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
   <!-- TEI Indexing                                                           -->
   <!-- ====================================================================== -->
   
   <!-- Ignored Elements. -->
   <xsl:template match="*:teiHeader">
      <xsl:copy>
         <xsl:copy-of select="@*"/>
         <xsl:attribute name="xtf:index" select="'no'"/>
         <xsl:apply-templates/>
      </xsl:copy>
   </xsl:template>
   
   <!-- sectionType Indexing and Element Boosting -->
   <xsl:template match="*:head[parent::*[matches(local-name(),'^div')]]">
      <xsl:copy>
         <xsl:copy-of select="@*"/>
         <xsl:attribute name="xtf:sectionType" select="concat('head ', @type)"/>
         <xsl:attribute name="xtf:wordBoost" select="2.0"/>
         <xsl:apply-templates/>
      </xsl:copy>
   </xsl:template>
   
   <xsl:template match="*:bibl">
      <xsl:copy>
         <xsl:copy-of select="@*"/>
         <xsl:attribute name="xtf:sectionType" select="'citation'"/>
         <xsl:attribute name="xtf:wordBoost" select="2.0"/>
         <xsl:apply-templates/>
      </xsl:copy>
   </xsl:template>
   
   <xsl:template match="*:titlePart[ancestor::*:titlePage]">
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
      
      <!-- If no Dublin Core present, then extract meta-data from the TEI -->
      <xsl:variable name="meta">
         <xsl:choose>
            <xsl:when test="$dcMeta/*">
               <xsl:copy-of select="$dcMeta"/>
            </xsl:when>
            <xsl:otherwise>
               <xsl:call-template name="get-tei-title"/>
               <xsl:call-template name="get-tei-creator"/>
               <xsl:call-template name="get-tei-subject"/>
               <xsl:call-template name="get-tei-description"/>
               <xsl:call-template name="get-tei-publisher"/>
               <xsl:call-template name="get-tei-contributor"/>
               <xsl:call-template name="get-tei-date"/>
               <xsl:call-template name="get-tei-type"/>
               <xsl:call-template name="get-tei-format"/>
               <xsl:call-template name="get-tei-identifier"/>
               <xsl:call-template name="get-tei-source"/>
               <xsl:call-template name="get-tei-language"/>
               <xsl:call-template name="get-tei-relation"/>
               <xsl:call-template name="get-tei-coverage"/>
               <xsl:call-template name="get-tei-rights"/>
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
   <xsl:template name="get-tei-title">
      <xsl:choose>
         <xsl:when test="//*:fileDesc/*:titleStmt/*:title">
            <title xtf:meta="true">
               <xsl:value-of select="string(//*:fileDesc/*:titleStmt/*:title[1])"/>
            </title>
         </xsl:when>
         <xsl:when test="//*:titlePage/*:titlePart[@type='main']">
            <title xtf:meta="true">
               <xsl:value-of select="string(//*:titlePage/*:titlePart[@type='main'])"/>
               <xsl:if test="//*:titlePage/*:titlePart[@type='subtitle']">
                  <xsl:text>: </xsl:text>
                  <xsl:value-of select="string(//*:titlePage/*:titlePart[@type='subtitle'][1])"/>
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
   <xsl:template name="get-tei-creator">
      <xsl:choose>
         <xsl:when test="//*:fileDesc/*:titleStmt/*:author">
            <creator xtf:meta="true">
               <xsl:value-of select="string(//*:fileDesc/*:titleStmt/*:author[1])"/>
            </creator>
         </xsl:when>
         <xsl:when test="//*:titlePage/*:docAuthor">
            <creator xtf:meta="true">
               <xsl:value-of select="string(//*:titlePage/*:docAuthor[1])"/>
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
   <xsl:template name="get-tei-subject">
      <xsl:choose>
         <xsl:when test="//*:keywords/*:list/*:item">
            <xsl:for-each select="//*:keywords/*:list/*:item">
               <subject xtf:meta="true">
                  <xsl:value-of select="."/>
               </subject>
            </xsl:for-each>
         </xsl:when>
      </xsl:choose>
   </xsl:template>
   
   <!-- description --> 
   <xsl:template name="get-tei-description">
      <xsl:choose>
         <xsl:when test="//*:text/*:body/*:div1[1]/*:p">
            <description xtf:meta="true">
               <xsl:value-of select="//*:text/*:body/*:div1[1]/*:p[1]"/>
            </description>
         </xsl:when>
      </xsl:choose>
   </xsl:template>
   
   <!-- publisher -->
   <xsl:template name="get-tei-publisher">
      <xsl:choose>
         <xsl:when test="//*:fileDesc/*:publicationStmt/*:publisher">
            <publisher xtf:meta="true">
               <xsl:value-of select="string(//*:fileDesc/*:publicationStmt/*:publisher[1])"/>
            </publisher>
         </xsl:when>
         <xsl:when test="//*:text/*:front/*:titlePage//*:publisher">
            <publisher xtf-meta="true">
               <xsl:value-of select="string(//*:text/*:front/*:titlePage//*:publisher[1])"/>
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
   <xsl:template name="get-tei-contributor">
      <xsl:choose>
         <xsl:when test="//*:fileDesc/*:respStmt/*:name">
            <contributor xtf-meta="true">
               <xsl:value-of select="string(//*:fileDesc/*:respStmt/*:name[1])"/>
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
   <xsl:template name="get-tei-date">
      <xsl:choose>
         <xsl:when test="//*:fileDesc/*:publicationStmt/*:date">
            <date xtf:meta="true">
               <xsl:value-of select="string(//*:fileDesc/*:publicationStmt/*:date)"/>
            </date>
         </xsl:when>
         <xsl:when test="//*:titlePage/*:docImprint/*:docDate">
            <date xtf:meta="true">
               <xsl:value-of select="string(//*:titlePage/*:docImprint/*:docDate)"/>
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
   <xsl:template name="get-tei-type">
      <type xtf:meta="true">tei</type>
   </xsl:template>
   
   <!-- format -->
   <xsl:template name="get-tei-format">
      <format xtf:meta="true">xml</format>
   </xsl:template>
   
   <!-- identifier --> 
   <xsl:template name="get-tei-identifier">
      <xsl:choose>
         <xsl:when test="//*:fileDesc/*:publicationStmt/*:idno">
            <identifier xtf:meta="true" xtf:tokenize="no">
               <xsl:value-of select="replace(string(//*:fileDesc/*:publicationStmt/*:idno[1]),'^.+/','')"/>
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
   <xsl:template name="get-tei-source">
      <xsl:choose>
         <xsl:when test="//*:sourceDesc/*:bibl">
            <source xtf-meta="true">
               <xsl:value-of select="string(//*:sourceDesc/*:bibl[1])"/>
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
   <xsl:template name="get-tei-language">
      <xsl:choose>
         <xsl:when test="//*:profileDesc/*:langUsage/*:language">
            <language xtf-meta="true">
               <xsl:value-of select="string((//*:profileDesc/*:langUsage/*:language)[1])"/>
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
   <xsl:template name="get-tei-relation">
      <xsl:choose>
         <xsl:when test="//*:fileDesc/*:seriesStmt/*:title">
            <relation xtf-meta="true">
               <xsl:value-of select="string(//*:fileDesc/*:seriesStmt/*:title)"/>
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
   <xsl:template name="get-tei-coverage">
      <coverage xtf:meta="true">
         <xsl:value-of select="'unknown'"/>
      </coverage>
   </xsl:template>
   
   <!-- rights -->
   <xsl:template name="get-tei-rights">
      <rights xtf-meta="true">
         <xsl:value-of select="'public'"/>
      </rights>
   </xsl:template>
   
   <!-- OAI dateStamp -->
   <xsl:template name="oai-datestamp" xmlns:FileUtils="java:org.cdlib.xtf.xslt.FileUtils">
      <xsl:variable name="filePath" select="saxon:system-id()" xmlns:saxon="http://saxon.sf.net/"/>
      <dateStamp xtf:meta="true" xtf:tokenize="no">
         <xsl:value-of select="FileUtils:lastModified($filePath, 'yyyy-MM-dd')"/>
      </dateStamp>
   </xsl:template>
   
   <!-- OAI sets -->
   <xsl:template name="oai-set">
      <xsl:for-each select="//*:keywords/*:list/*:item">
         <set xtf:meta="true">
            <xsl:value-of select="."/>
         </set>
      </xsl:for-each>
      <set xtf:meta="true">
         <xsl:value-of select="'public'"/>
      </set>
   </xsl:template>
   
</xsl:stylesheet>
