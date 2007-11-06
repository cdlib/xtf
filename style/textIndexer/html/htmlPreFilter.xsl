<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
        xmlns:saxon="http://saxon.sf.net/"
        xmlns:xtf="http://cdlib.org/xtf"
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
            </xsl:otherwise>
         </xsl:choose>
      </xsl:variable>
      
      <!-- Add doc kind and sort fields to the data, and output the result. -->
      <xsl:call-template name="add-fields">
         <xsl:with-param name="display-kind" select="'html'"/>
         <xsl:with-param name="meta" select="$meta"/>
      </xsl:call-template>    
   </xsl:template>
   
   <xsl:template name="get-htm-title">
      <xsl:choose>
         <xsl:when test="//*[local-name()='meta'][@name='dc.title']">
            <title xtf:meta="true">
               <xsl:value-of select="//*[local-name()='meta'][@name='dc.title'][1]/@content"/>
            </title>
         </xsl:when>
      </xsl:choose>
   </xsl:template>
   
   <xsl:template name="get-htm-creator">
      <xsl:choose>
         <xsl:when test="//*[local-name()='meta'][@name='dc.creator']">
            <creator xtf:meta="true">
               <xsl:value-of select="//*[local-name()='meta'][@name='dc.creator'][1]/@content"/>
            </creator>
         </xsl:when>
      </xsl:choose>
   </xsl:template>
   
   <xsl:template name="get-htm-subject">
      <xsl:choose>
         <xsl:when test="//*[local-name()='meta'][@name='dc.subject']">
            <xsl:for-each select="//*[local-name()='meta'][@name='dc.subject']">
               <subject xtf:meta="true">
                  <xsl:value-of select="@content"/>
               </subject>
            </xsl:for-each>
         </xsl:when>
      </xsl:choose>
   </xsl:template>
   
   <xsl:template name="get-htm-description">
      <xsl:choose>
         <xsl:when test="//*[local-name()='meta'][@name='dc.description']">
            <description xtf:meta="true">
               <xsl:value-of select="//*[local-name()='meta'][@name='dc.description'][1]/@content"/>
            </description>
         </xsl:when>
      </xsl:choose>
   </xsl:template>
   
   <xsl:template name="get-htm-publisher">
      <xsl:choose>
         <xsl:when test="//*[local-name()='meta'][@name='dc.publisher']">
            <publisher xtf:meta="true">
               <xsl:value-of select="//*[local-name()='meta'][@name='dc.publisher'][1]/@content"/>
            </publisher>
         </xsl:when>
      </xsl:choose>
   </xsl:template>
   
   <xsl:template name="get-htm-contributor">
      <xsl:choose>
         <xsl:when test="//*[local-name()='meta'][@name='dc.contributor']">
            <contributor xtf:meta="true">
               <xsl:value-of select="//*[local-name()='meta'][@name='dc.contributor'][1]/@content"/>
            </contributor>
         </xsl:when>
      </xsl:choose>
   </xsl:template>
   
   <xsl:template name="get-htm-date">
      <xsl:choose>
         <xsl:when test="//*[local-name()='meta'][@name='dc.date']">
            <date xtf:meta="true">
               <xsl:value-of select="//*[local-name()='meta'][@name='dc.date'][1]/@content"/>
            </date>
         </xsl:when>
      </xsl:choose>
   </xsl:template>
   
   <xsl:template name="get-htm-type">
      <type xtf:meta="true">html</type>
   </xsl:template>
   
   <xsl:template name="get-htm-format">
      <xsl:choose>
         <xsl:when test="//*[local-name()='meta'][@name='dc.format']">
            <format xtf:meta="true">
               <xsl:value-of select="//*[local-name()='meta'][@name='dc.format'][1]/@content"/>
            </format>
         </xsl:when>
      </xsl:choose>
   </xsl:template>
   
   <xsl:template name="get-htm-identifier">
      <xsl:choose>
         <xsl:when test="//*[local-name()='meta'][@name='dc.identifier']">
            <identifier xtf:meta="true">
               <xsl:value-of select="//*[local-name()='meta'][@name='dc.identifier'][1]/@content"/>
            </identifier>
         </xsl:when>
      </xsl:choose>
   </xsl:template>
   
   <xsl:template name="get-htm-source">
      <xsl:choose>
         <xsl:when test="//*[local-name()='meta'][@name='dc.source']">
            <source xtf:meta="true">
               <xsl:value-of select="//*[local-name()='meta'][@name='dc.date'][1]/@content"/>
            </source>
         </xsl:when>
      </xsl:choose>
   </xsl:template>
   
   <xsl:template name="get-htm-language">
      <xsl:choose>
         <xsl:when test="//*[local-name()='meta'][@name='dc.date']">
            <language xtf:meta="true">
               <xsl:value-of select="//*[local-name()='meta'][@name='dc.source'][1]/@content"/>
            </language>
         </xsl:when>
      </xsl:choose>
   </xsl:template>
   
   <xsl:template name="get-htm-relation">
      <xsl:choose>
         <xsl:when test="//*[local-name()='meta'][@name='dc.relation']">
            <relation xtf:meta="true">
               <xsl:value-of select="//*[local-name()='meta'][@name='dc.relation'][1]/@content"/>
            </relation>
         </xsl:when>
      </xsl:choose>
   </xsl:template>
   
   <xsl:template name="get-htm-coverage">
      <xsl:choose>
         <xsl:when test="//*[local-name()='meta'][@name='dc.coverage']">
            <coverage xtf:meta="true">
               <xsl:value-of select="//*[local-name()='meta'][@name='dc.coverage'][1]/@content"/>
            </coverage>
         </xsl:when>
      </xsl:choose>
   </xsl:template>
   
   <xsl:template name="get-htm-rights">
      <xsl:choose>
         <xsl:when test="//*[local-name()='meta'][@name='dc.rights']">
            <rights xtf:meta="true">
               <xsl:value-of select="//*[local-name()='meta'][@name='dc.rights'][1]/@content"/>
            </rights>
         </xsl:when>
      </xsl:choose>
   </xsl:template>

</xsl:stylesheet>
