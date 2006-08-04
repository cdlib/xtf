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
          <xsl:call-template name="get-ead-author"/>
          <xsl:call-template name="get-ead-date"/>
          <xsl:call-template name="get-ead-description"/>
          <xsl:call-template name="get-ead-identifier"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <!-- Add doc kind and sort fields to the data, and output the result. -->
    <xsl:call-template name="add-fields">
      <xsl:with-param name="display-kind" select="'dynaXML/EAD'"/>
      <xsl:with-param name="meta" select="$meta"/>
    </xsl:call-template>    
  </xsl:template>
  
  <!-- Fetch title info from the eadHeader or the first archdesc/did. --> 
  <xsl:template name="get-ead-title">
    <xsl:choose>
      <xsl:when test="/ead/eadheader/filedesc/titlestmt/titleproper">
        <xsl:variable name="titleproper" select="string(/ead/eadheader/filedesc/titlestmt/titleproper)"/>
        <xsl:variable name="subtitle" select="string(//filedesc/titlestmt/subtitle)"/>
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
      <xsl:when test="/ead/archdesc[1]/did[1]/unittitle">
        <title xtf:meta="true">
          <xsl:value-of select="/ead/archdesc[1]/did[1]/unittitle[1]"/>
        </title>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <!-- Fetch creator (author) info from the eadHeader or the first archdesc/did. --> 
  <xsl:template name="get-ead-author">
    <xsl:choose>
      <xsl:when test="/ead/eadheader/filedesc/titlestmt/author">
        <creator xtf:meta="true">
          <xsl:value-of select="/ead/eadheader/filedesc/titlestmt/author"/>
        </creator>
      </xsl:when>
      <xsl:when test="/ead/archdesc[1]/did[1]/origination[starts-with(@label, 'Creator')]">
        <creator xtf:meta="true">
          <xsl:value-of select="/ead/archdesc[1]/did[1]/origination[@label, 'Creator']"/>
        </creator>
      </xsl:when>
    </xsl:choose>
  </xsl:template>
  
  <!-- Fetch date info from the publicationStmt --> 
  <xsl:template name="get-ead-date">
    <xsl:choose>
      <xsl:when test="/ead/eadheader/filedesc/publicationstmt/date">
        <date xtf:meta="true">
          <xsl:value-of select="/ead/eadheader/filedesc/publicationstmt/date"/>
        </date>
      </xsl:when>
      <xsl:when test="/ead/eadheader/profiledesc/creation/date">
        <date xtf:meta="true">
          <xsl:value-of select="/ead/eadheader/profiledesc/creation/date"/>
        </date>
      </xsl:when>
    </xsl:choose>
  </xsl:template>
  
  <!-- Fetch description info from the EAD, if possible --> 
  <xsl:template name="get-ead-description">
    <xsl:choose>
      <xsl:when test="/ead/archdesc[1]/did[1]/abstract">
        <description xtf:meta="true">
          <xsl:value-of select="/ead/archdesc[1]/did[1]/abstract[1]"/>
        </description>
      </xsl:when>
    </xsl:choose>
  </xsl:template>
  
  <!-- Fetch identifier info from the EAD, if possible --> 
  <xsl:template name="get-ead-identifier">
    <xsl:choose>
      <xsl:when test="/ead/archdesc[1]/did[1]/unitid[1]">
        <identifier xtf:meta="true">
          <xsl:value-of select="/ead/archdesc[1]/did[1]/unitid[1]"/>
        </identifier>
      </xsl:when>
    </xsl:choose>
  </xsl:template>
  
</xsl:stylesheet>
