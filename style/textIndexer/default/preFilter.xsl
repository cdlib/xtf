<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
        xmlns:saxon="http://saxon.sf.net/"
        xmlns:xtf="http://cdlib.org/xtf"
        xmlns:date="http://exslt.org/dates-and-times"
        xmlns:parse="http://cdlib.org/xtf/parse"
        xmlns:mets="http://www.loc.gov/METS/"
        extension-element-prefixes="date"
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
      <xsl:copy-of select="@*"/>
      <xsl:call-template name="get-meta"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>

<!-- ====================================================================== -->
<!-- TEI Indexing                                                           -->
<!-- ====================================================================== -->

  <!-- Ignored Elements -->
  
  <xsl:template match="teiHeader">
    <xsl:copy>
      <xsl:copy-of select="@*"/>
      <xsl:attribute name="xtf:index" select="'no'"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>

  <!-- sectionType Indexing and Element Boosting -->
  
  <xsl:template match="head[parent::*[self::div1 or self::div2 or self::div3 or self::div4 or self::div5 or self::div6 or self::div7]]">
    <xsl:copy>
      <xsl:copy-of select="@*"/>
      <xsl:attribute name="xtf:sectionType" select="concat('head ', @type)"/>
      <xsl:attribute name="xtf:wordBoost" select="2.0"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>
  
  <xsl:template match="titlePart[ancestor::titlePage]">
    <xsl:copy>
      <xsl:copy-of select="@*"/>
      <xsl:attribute name="xtf:wordBoost" select="100.0"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>
  
<!-- ====================================================================== -->
<!-- Metadata Indexing                                                      -->
<!-- ====================================================================== -->

  <!-- Access Dublin Core Record -->
  <xsl:template name="get-meta">
    <xsl:variable name="docpath" select="saxon:system-id()"/>
    <xsl:variable name="base" select="substring-before($docpath, '.xml')"/>
    <xsl:variable name="dcpath" select="concat($base, '.dc.xml')"/>
    <xsl:variable name="metspath" select="concat($base, '.mets.xml')"/>
    <xtf:meta>
      <xsl:apply-templates select="document($dcpath)" mode="inmeta"/>
    </xtf:meta>
  </xsl:template>
  
  <!-- Process DC -->
  <xsl:template match="dc" mode="inmeta">
    
    <!-- metadata fields -->
    <xsl:for-each select="*">
      <xsl:element name="{name()}">
        <xsl:attribute name="xtf:meta" select="'true'"/>
        <xsl:copy-of select="@*"/>
        <xsl:value-of select="string()"/>
      </xsl:element>
    </xsl:for-each>
    
    <!-- create sort fields -->
    <xsl:apply-templates select="title" mode="sort"/>    
    <xsl:apply-templates select="creator" mode="sort"/>
    <xsl:apply-templates select="date" mode="sort"/>
    
    <!-- create facet fields -->
    <xsl:apply-templates select="title" mode="facet"/>
    <xsl:apply-templates select="creator" mode="facet"/>
    <xsl:apply-templates select="subject" mode="facet"/>
    <xsl:apply-templates select="description" mode="facet"/>
    <xsl:apply-templates select="publisher" mode="facet"/>
    <xsl:apply-templates select="contributor" mode="facet"/>
    <xsl:apply-templates select="date" mode="facet"/>
    <xsl:apply-templates select="type" mode="facet"/>
    <xsl:apply-templates select="format" mode="facet"/>
    <xsl:apply-templates select="identifier" mode="facet"/>
    <xsl:apply-templates select="source" mode="facet"/>
    <xsl:apply-templates select="language" mode="facet"/>
    <xsl:apply-templates select="relation" mode="facet"/>
    <xsl:apply-templates select="coverage" mode="facet"/>
    <xsl:apply-templates select="rights" mode="facet"/>
    
  </xsl:template>

  <!-- generate sort-title -->
  <xsl:template match="title" mode="sort">
    
    <xsl:variable name="title" select="string(.)"/>
 
    <sort-title>
      <xsl:attribute name="xtf:meta" select="'true'"/>
      <xsl:attribute name="xtf:tokenize" select="'no'"/>
      <xsl:value-of select="parse:title($title)"/>
    </sort-title>
  </xsl:template>

  <!-- generate sort-creator -->
  <xsl:template match="creator" mode="sort">
    
    <xsl:variable name="creator" select="string(.)"/>
    
    <xsl:if test="number(position()) = 1">
      <sort-creator>
        <xsl:attribute name="xtf:meta" select="'true'"/>
        <xsl:attribute name="xtf:tokenize" select="'no'"/>
        <xsl:copy-of select="parse:name($creator)"/>
      </sort-creator>
    </xsl:if>
    
  </xsl:template>
  
  <!-- generate year and sort-year -->
  <xsl:template match="date" mode="sort">

    <xsl:variable name="date" select="string(.)"/>
    <xsl:variable name="pos" select="number(position())"/>
    
    <xsl:copy-of select="parse:year($date, $pos)"/>
  </xsl:template>

</xsl:stylesheet>
