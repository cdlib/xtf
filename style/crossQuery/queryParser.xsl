<!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
<!-- Simple query parser stylesheet                                         -->
<!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->

<!--
   Copyright (c) 2004, Regents of the University of California
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
  complex queries (boolean and/or/not, ranges, nested queries, etc.) An
  experimental parser is available that does parse these constructs; see
  complexQueryParser.xsl.
  
  For details on the input and output expected of this stylesheet, see the
  comment section at the bottom of this file.
-->

<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                              xmlns:dc="http://purl.org/dc/elements/1.1/" 
                              xmlns:mets="http://www.loc.gov/METS/"
                              xmlns:xlink="http://www.w3.org/TR/xlink" 
                              xmlns:xs="http://www.w3.org/2001/XMLSchema"
                              xmlns:parse="http://cdlib.org/parse"
                              exclude-result-prefixes="xsl dc mets xlink xs parse">
  
  <xsl:output method="xml" indent="yes" encoding="utf-8"/>
  <xsl:strip-space elements="*"/>
  
<!-- ====================================================================== -->
<!-- Global parameters (specified in the URL)                               -->
<!-- ====================================================================== -->
  
  <!-- search mode -->
  <xsl:param name="smode"/>

  <!-- result mode -->
  <xsl:param name="rmode"/>

  <!-- sort mode -->
  <xsl:param name="sort"/>

  <xsl:param name="startDoc" select="1"/>
  
  <xsl:param name="docsPerPage">
    <xsl:choose>
      <xsl:when test="($smode = 'test') or ($smode='debug')">
        <xsl:value-of select="10000"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="20"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:param>
  
<!-- ====================================================================== -->
<!-- Root Template                                                          -->
<!-- ====================================================================== -->
  
  <xsl:template match="/">
    
    <xsl:variable name="stylesheet" select="'style/crossQuery/resultFormatter/default/resultFormatter.xsl'"/>

    <!-- The top-level output element tells what stylesheet will be used to
       format the results, which document to start on, and how many documents
       to display on this page. -->
    <query style="{$stylesheet}" startDoc="{$startDoc}" maxDocs="{$docsPerPage}">
      <!-- Assume we'll need to combine a full-text query with a meta-data
      query. If it turns out there's only one or the other, the query
      processor optimizes this out, so it's harmless -->
      <combine indexPath="index" termLimit="1000" workLimit="500000">
        <!-- KVH: Added to test Lucene sorting -->
        <xsl:if test="$sort != ''">
          <xsl:attribute name="sortMetaFields">
            <xsl:choose>
              <xsl:when test="$sort='title'">
                <xsl:value-of select="'sort-title,sort-creator,sort-year'"/>
              </xsl:when>
              <xsl:when test="$sort='creator'">
                <xsl:value-of select="'sort-creator,sort-year,sort-title'"/>
              </xsl:when>
              <xsl:when test="$sort='year'">
                <xsl:value-of select="'sort-year,sort-title,sort-creator'"/>
              </xsl:when>
            </xsl:choose>
          </xsl:attribute>
        </xsl:if>
        <!-- Process query -->
        <xsl:apply-templates/>
      </combine>
    </query>
  </xsl:template>

  <xsl:template match="parameters">
    
    <!-- Scan for non-empty parameters (but skip "-exclude", "-join", "-prox", "-max", and "-ignore") -->
    <xsl:variable name="queryParams" select="param[count(*) &gt; 0 and not(matches(@name, '.*-exclude')) and not(matches(@name, '.*-join')) and not(matches(@name, '.*-prox')) and not(matches(@name, '.*-max')) and not(matches(@name, '.*-ignore'))]"/>
    
    <!-- Find the full-text query, if any -->
    <xsl:variable name="textParam" select="$queryParams[matches(@name, 'text|query')]"/>
    
    <!-- Find the meta-data queries, if any -->
    <xsl:variable name="metaParams" select="$queryParams[not(matches(@name, 'text*|rmode|smode|sort|startDoc|docsPerPages|sectionType|.*-ignore'))]"/>
    
    <!-- Process the meta-data queries, if any -->
    <xsl:if test="count($metaParams) &gt; 0">
      <meta>
        <!-- KVH: Added to test Lucene sorting -->
        <xsl:if test="$sort != ''">
          <xsl:attribute name="sortMetaFields">
            <xsl:choose>
              <xsl:when test="$sort='title'">
                <xsl:value-of select="'sort-title,sort-creator,sort-year'"/>
              </xsl:when>
              <xsl:when test="$sort='creator'">
                <xsl:value-of select="'sort-creator,sort-year,sort-title'"/>
              </xsl:when>
              <xsl:when test="$sort='year'">
                <xsl:value-of select="'sort-year,sort-title,sort-creator'"/>
              </xsl:when>
            </xsl:choose>
          </xsl:attribute>
        </xsl:if>        
        <!-- 'AND' all the meta-data queries together. If there's only one,
        the query processor optimizes this out, so it's harmless. -->
        <and>
          <xsl:apply-templates select="$metaParams"/>
        </and>
      </meta>
    </xsl:if>
    
    <!-- Process the text query, if any -->
    <xsl:if test="count($textParam) &gt; 0">
      <text maxSnippets="3" maxContext="80">
        <!-- KVH: Added to test Lucene sorting -->
        <xsl:if test="$sort != ''">
          <xsl:attribute name="sortMetaFields">
            <xsl:choose>
              <xsl:when test="$sort='title'">
                <xsl:value-of select="'sort-title,sort-creator,sort-year'"/>
              </xsl:when>
              <xsl:when test="$sort='creator'">
                <xsl:value-of select="'sort-creator,sort-year,sort-title'"/>
              </xsl:when>
              <xsl:when test="$sort='year'">
                <xsl:value-of select="'sort-year,sort-title,sort-creator'"/>
              </xsl:when>
            </xsl:choose>
          </xsl:attribute>
        </xsl:if>
        <xsl:apply-templates select="$textParam"/>
      </text>
    </xsl:if>
    
    <!-- If there are no meta and no text queries, output a dummy -->
    <xsl:if test="count($metaParams) = 0 and count($textParam) = 0">
      <meta>
        <term metaField="title">!@$$!@$</term>
      </meta>
    </xsl:if>
    
  </xsl:template>
  
<!-- ====================================================================== -->
<!-- Single-field parameter template                                        -->
<!--                                                                        -->
<!-- Join all the terms of a single text or meta-data query together. For   -->
<!-- meta-data queries, we must also specify the field to search in.        -->
<!-- ====================================================================== -->
  
  <xsl:template match="param">
    
    <xsl:variable name="metaField" select="@name"/>
    
    <xsl:variable name="exclude" select="//param[@name=concat($metaField, '-exclude')]"/>
    <xsl:variable name="join" select="//param[@name=concat($metaField, '-join')]"/>
    <xsl:variable name="prox" select="//param[@name=concat($metaField, '-prox')]"/>
    <xsl:variable name="max" select="//param[@name=concat($metaField, '-max')]"/>
   
    <xsl:variable name="op">
      <xsl:choose>
         <xsl:when test="$max/@value != ''">
          <xsl:value-of select="'range'"/>
        </xsl:when>       
        <xsl:when test="$prox/@value != ''">
          <xsl:value-of select="'near'"/>
        </xsl:when>
        <xsl:when test="$join/@value != ''">
          <xsl:value-of select="$join/@value"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="'and'"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    
    <!-- 'and' all the terms together, unless "<field>-join" specifies a
       different operator (like 'or'). In the simple case when there's 
       only one term, the query processor optimizes this out, so it's 
       harmless. -->
    <xsl:element name="{$op}">
      
      <!-- Specify the field name for meta-data queries -->
      <xsl:if test="not(matches(@name, 'text|query'))">
        <xsl:attribute name="metaField" select="$metaField"/>
      </xsl:if>
      
      <!-- Specify the maximum term separation for a proximity query -->
      <xsl:if test="$prox/@value != ''">
        <xsl:attribute name="slop" select="$prox/@value"/>
      </xsl:if>
      
      <!-- Process all the phrases and/or terms -->
      <xsl:apply-templates/>
      
      <!-- If there is an 'exclude' parameter for this field, process it -->
      <xsl:if test="$exclude/@value != ''">
        <not>
          <xsl:apply-templates select="$exclude/*"/>
        </not>
      </xsl:if>
      
    </xsl:element>
    
    <!-- If there is a sectionType parameter, process it -->
    <xsl:if test="matches($metaField, 'text|query') and (//param[@name='sectionType']/@value != '')">
      <sectionType>
        <xsl:apply-templates select="//param[@name='sectionType']"/>
      </sectionType>
    </xsl:if>
    
  </xsl:template>

<!-- ====================================================================== -->
<!-- Phrase template                                                        -->
<!--                                                                        -->
<!-- A phrase consists simply of several tokens.                            -->
<!-- ====================================================================== -->
  
  <xsl:template match="phrase">
    <phrase>
      <xsl:apply-templates/>
    </phrase>
  </xsl:template>
  
<!-- ====================================================================== -->
<!-- Token template                                                         -->
<!--                                                                        -->
<!-- Tokens form the basic search terms which make up all other types of    -->
<!-- queries. For simplicity, we disregard any "non-word" tokens (i.e.      -->
<!-- symbols such as "+", "&", etc.)                                        -->
<!-- ====================================================================== -->
  
  <xsl:template match="token[@isWord='yes']">
    
    <xsl:variable name="metaField" select="parent::*/@name"/>
    <xsl:variable name="max" select="//param[@name=concat($metaField, '-max')]"/>
   
    <xsl:choose>
      <xsl:when test="$max/@value != ''">
        <lower>
          <xsl:value-of select="@value"/>
        </lower>
        <upper>
          <xsl:value-of select="$max/@value"/>
        </upper>
      </xsl:when>
      <xsl:otherwise>       
        <term>
          <xsl:value-of select="@value"/>
        </term>
      </xsl:otherwise>
    </xsl:choose>
    
  </xsl:template>
  
</xsl:stylesheet>
