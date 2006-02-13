<!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
<!-- Simple query parser stylesheet                                         -->
<!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->

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

<!--
  This stylesheet implements a simple query parser which does not handle any
  complex queries (boolean and/or/not, ranges, nested queries, etc.)
-->

<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                              xmlns:session="java:org.cdlib.xtf.xslt.Session"
                              exclude-result-prefixes="xsl">
  
<!-- ====================================================================== -->
<!-- Import Common Templates                                                -->
<!-- ====================================================================== -->

  <xsl:import href="../common/queryParserCommon.xsl"/>
  
<!-- ====================================================================== -->
<!-- Output Parameters                                                      -->
<!-- ====================================================================== -->

  <xsl:output method="xml" indent="yes" encoding="utf-8"/>
  <xsl:strip-space elements="*"/>
  
<!-- ====================================================================== -->
<!-- Local parameters                                                       -->
<!-- ====================================================================== -->

  <!-- list of keyword search fields -->
  <xsl:param name="fieldList"/>
   
<!-- ====================================================================== -->
<!-- Root Template                                                          -->
<!-- ====================================================================== -->
  
  <xsl:template match="/">
    
    <xsl:variable name="stylesheet" select="'style/crossQuery/resultFormatter/default/resultFormatter.xsl'"/>

    <!-- The top-level query element tells what stylesheet will be used to
       format the results, which document to start on, and how many documents
       to display on this page. -->
    <query indexPath="index" termLimit="1000" workLimit="1000000" style="{$stylesheet}" startDoc="{$startDoc}" maxDocs="{$docsPerPage}">

      <!-- sort attribute -->
      <xsl:if test="$sort">
        <xsl:attribute name="sortMetaFields">
          <xsl:choose>
            <xsl:when test="$sort='title'">
              <xsl:value-of select="'sort-title,sort-creator,sort-publisher,sort-year'"/>
            </xsl:when>
            <xsl:when test="$sort='year'">
              <xsl:value-of select="'sort-year,sort-title,sort-creator,sort-publisher'"/>
            </xsl:when>              
            <xsl:when test="$sort='creator'">
              <xsl:value-of select="'sort-creator,sort-year,sort-title'"/>
            </xsl:when>
            <xsl:when test="$sort='publisher'">
              <xsl:value-of select="'sort-publisher,sort-title,sort-year'"/>
            </xsl:when>              
          </xsl:choose>
        </xsl:attribute>
      </xsl:if>
      
      <!-- score normalization and explanation -->
      <xsl:if test="$normalizeScores">
        <xsl:attribute name="normalizeScores" select="$normalizeScores"/>
      </xsl:if>
      <xsl:if test="$explainScores">
        <xsl:attribute name="explainScores" select="$explainScores"/>
      </xsl:if>
      
      <!-- process query -->
      <xsl:choose>
        <xsl:when test="$smode = 'addToBag'">
          <xsl:call-template name="addToBag"/>
        </xsl:when>
        <xsl:when test="$smode = 'removeFromBag'">
          <xsl:call-template name="removeFromBag"/>
        </xsl:when>
        <xsl:when test="$smode = 'showBag'">
          <xsl:call-template name="showBag"/>
        </xsl:when>
        <xsl:when test="$smode = 'moreLike'">
          <xsl:call-template name="moreLike"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates/>
        </xsl:otherwise>
      </xsl:choose>
  
    </query>
  </xsl:template>

<!-- ====================================================================== -->
<!-- Parameters Template                                                    -->
<!-- ====================================================================== -->
  
  <xsl:template match="parameters">

    <!-- Scan for non-empty parameters (but skip "-exclude", "-join", "-prox", "-max", and "-ignore") -->
    <xsl:variable name="queryParams" select="param[count(*) &gt; 0 and not(matches(@name, '.*-exclude')) 
                                                                   and not(matches(@name, '.*-join')) 
                                                                   and not(matches(@name, '.*-prox')) 
                                                                   and not(matches(@name, '.*-max')) 
                                                                   and not(matches(@name, '.*-ignore'))]"/>

    <!-- Find the full-text query, if any -->
    <xsl:variable name="textParam" select="$queryParams[matches(@name, 'text|query')]"/>
    
    <!-- Find the meta-data queries, if any -->
    <xsl:variable name="metaParams" select="$queryParams[not(matches(@name, 'text*|query*|style|smode|rmode|brand|sort|startDoc|docsPerPage|sectionType|fieldList|normalizeScores|explainScores|.*-ignore'))]"/>
 
    <and>
      <!-- Process the meta-data queries, if any -->
      <xsl:if test="count($metaParams) &gt; 0">
        <xsl:apply-templates select="$metaParams"/>
      </xsl:if>       
      <!-- Process the text query, if any -->
      <xsl:if test="count($textParam) &gt; 0">
        <xsl:apply-templates select="$textParam"/>
      </xsl:if>     
    </and>
    
  </xsl:template>
  
</xsl:stylesheet>
