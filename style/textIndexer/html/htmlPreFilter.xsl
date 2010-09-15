<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
   xmlns:parse="http://cdlib.org/xtf/parse"
   xmlns:xtf="http://cdlib.org/xtf"
   xmlns:saxon="http://saxon.sf.net/"
   extension-element-prefixes="saxon"
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
   <!-- Metadata Indexing                                                      -->
   <!-- ====================================================================== -->
   <xsl:template name="get-meta">
      <!-- Access Dublin Core Record (if present) -->
      <xsl:variable name="dcMeta">
         <xsl:call-template name="get-dc-meta"/>
      </xsl:variable>
      
      <!-- If no Dublin Core present, then extract meta-data from meta tags -->
      <xsl:variable name="meta">
         <xsl:choose>
            <xsl:when test="$dcMeta/*">
               <xsl:copy-of select="$dcMeta"/>
            </xsl:when>
            <xsl:otherwise>
               <xsl:call-template name="get-htm-title"/>
               <xsl:call-template name="get-htm-creator"/>
               <xsl:call-template name="get-htm-subject"/>
               <xsl:call-template name="get-htm-description"/>
               <xsl:call-template name="get-htm-publisher"/>
               <xsl:call-template name="get-htm-contributor"/>
               <xsl:call-template name="get-htm-date"/>
               <xsl:call-template name="get-htm-type"/>
               <xsl:call-template name="get-htm-format"/>
               <xsl:call-template name="get-htm-identifier"/>
               <xsl:call-template name="get-htm-source"/>
               <xsl:call-template name="get-htm-language"/>
               <xsl:call-template name="get-htm-relation"/>
               <xsl:call-template name="get-htm-coverage"/>
               <xsl:call-template name="get-htm-rights"/>
               <!-- special values for OAI -->
               <xsl:call-template name="oai-datestamp"/>
               <xsl:call-template name="oai-set"/>
            </xsl:otherwise>
         </xsl:choose>
      </xsl:variable>
      
      <!-- Add display and sort fields to the data, and output the result. -->
      <xsl:call-template name="add-fields">
         <xsl:with-param name="display" select="'raw'"/>
         <xsl:with-param name="meta" select="$meta"/>
      </xsl:call-template>    
   </xsl:template>
   
   <!-- title -->
   <xsl:template name="get-htm-title">
      <xsl:choose>
         <xsl:when test="//*:meta[@name='dc.title']">
            <title xtf:meta="true">
               <xsl:value-of select="//*:meta[@name='dc.title'][1]/@content"/>
            </title>
         </xsl:when>
      </xsl:choose>
   </xsl:template>
   
   <!-- creator -->
   <xsl:template name="get-htm-creator">
      <xsl:choose>
         <xsl:when test="//*:meta[@name='dc.creator']">
            <creator xtf:meta="true">
               <xsl:value-of select="//*:meta[@name='dc.creator'][1]/@content"/>
            </creator>
         </xsl:when>
      </xsl:choose>
   </xsl:template>
   
   <!-- subject -->
   <xsl:template name="get-htm-subject">
      <xsl:choose>
         <xsl:when test="//*:meta[@name='dc.subject']">
            <xsl:for-each select="//*:meta[@name='dc.subject']">
               <subject xtf:meta="true">
                  <xsl:value-of select="@content"/>
               </subject>
            </xsl:for-each>
         </xsl:when>
      </xsl:choose>
   </xsl:template>
   
   <!-- description -->
   <xsl:template name="get-htm-description">
      <xsl:choose>
         <xsl:when test="//*:meta[@name='dc.description']">
            <description xtf:meta="true">
               <xsl:value-of select="//*:meta[@name='dc.description'][1]/@content"/>
            </description>
         </xsl:when>
      </xsl:choose>
   </xsl:template>
   
   <!-- publisher -->
   <xsl:template name="get-htm-publisher">
      <xsl:choose>
         <xsl:when test="//*:meta[@name='dc.publisher']">
            <publisher xtf:meta="true">
               <xsl:value-of select="//*:meta[@name='dc.publisher'][1]/@content"/>
            </publisher>
         </xsl:when>
      </xsl:choose>
   </xsl:template>
   
   <!-- contributor -->
   <xsl:template name="get-htm-contributor">
      <xsl:choose>
         <xsl:when test="//*:meta[@name='dc.contributor']">
            <contributor xtf:meta="true">
               <xsl:value-of select="//*:meta[@name='dc.contributor'][1]/@content"/>
            </contributor>
         </xsl:when>
      </xsl:choose>
   </xsl:template>
   
   <!-- date -->
   <xsl:template name="get-htm-date">
      <xsl:choose>
         <xsl:when test="//*:meta[@name='dc.date']">
            <date xtf:meta="true">
               <xsl:value-of select="//*:meta[@name='dc.date'][1]/@content"/>
            </date>
         </xsl:when>
      </xsl:choose>
   </xsl:template>
   
   <!-- type -->
   <xsl:template name="get-htm-type">
      <type xtf:meta="true">html</type>
   </xsl:template>
   
   <!-- format -->
   <xsl:template name="get-htm-format">
      <xsl:choose>
         <xsl:when test="//*:meta[@name='dc.format']">
            <format xtf:meta="true">
               <xsl:value-of select="//*:meta[@name='dc.format'][1]/@content"/>
            </format>
         </xsl:when>
      </xsl:choose>
   </xsl:template>
   
   <!-- identifier -->
   <xsl:template name="get-htm-identifier">
      <xsl:choose>
         <xsl:when test="//*:meta[@name='dc.identifier']">
            <identifier xtf:meta="true" xtf:tokenize="no">
               <xsl:value-of select="replace(replace(//*:meta[@name='dc.identifier'][1]/@content,'^.+/',''),'\.[A-Za-z]+$','')"/>
            </identifier>
         </xsl:when>
         <xsl:otherwise>
            <identifier xtf:meta="true" xtf:tokenize="no">
               <xsl:value-of select="replace(replace(saxon:system-id(),'^.+/',''),'\.[A-Za-z]+$','')"/>
            </identifier>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- source -->
   <xsl:template name="get-htm-source">
      <xsl:choose>
         <xsl:when test="//*:meta[@name='dc.source']">
            <source xtf:meta="true">
               <xsl:value-of select="//*:meta[@name='dc.date'][1]/@content"/>
            </source>
         </xsl:when>
      </xsl:choose>
   </xsl:template>
   
   <!-- language -->
   <xsl:template name="get-htm-language">
      <xsl:choose>
         <xsl:when test="//*:meta[@name='dc.date']">
            <language xtf:meta="true">
               <xsl:value-of select="//*:meta[@name='dc.source'][1]/@content"/>
            </language>
         </xsl:when>
      </xsl:choose>
   </xsl:template>
   
   <!-- relation -->
   <xsl:template name="get-htm-relation">
      <xsl:choose>
         <xsl:when test="//*:meta[@name='dc.relation']">
            <relation xtf:meta="true">
               <xsl:value-of select="//*:meta[@name='dc.relation'][1]/@content"/>
            </relation>
         </xsl:when>
      </xsl:choose>
   </xsl:template>
   
   <!-- coverage -->
   <xsl:template name="get-htm-coverage">
      <xsl:choose>
         <xsl:when test="//*:meta[@name='dc.coverage']">
            <coverage xtf:meta="true">
               <xsl:value-of select="//*:meta[@name='dc.coverage'][1]/@content"/>
            </coverage>
         </xsl:when>
      </xsl:choose>
   </xsl:template>
   
   <!-- rights -->
   <xsl:template name="get-htm-rights">
      <xsl:choose>
         <xsl:when test="//*:meta[@name='dc.rights']">
            <rights xtf:meta="true">
               <xsl:value-of select="//*:meta[@name='dc.rights'][1]/@content"/>
            </rights>
         </xsl:when>
      </xsl:choose>
   </xsl:template>
   
   <!-- OAI dateStamp -->
   <xsl:template name="oai-datestamp">
      <dateStamp xtf:meta="true" xtf:tokenize="no">
         <xsl:choose>
            <xsl:when test="//*:meta[@name='dc.date']">
               <xsl:value-of select="concat(parse:year(string(//*:meta[@name='dc.date'][1]/@content)),'-01-01')"/>
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
      <xsl:for-each select="//*:meta[@name='dc.subject']">
         <set xtf:meta="true">
            <xsl:value-of select="@content"/>
         </set>
      </xsl:for-each>
      <set xtf:meta="true">
         <xsl:value-of select="'public'"/>
      </set>
   </xsl:template>
   
</xsl:stylesheet>
