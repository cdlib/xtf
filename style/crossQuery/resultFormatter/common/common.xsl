<!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
<!-- Query result formatter stylesheet                                      -->
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

<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
                              xmlns:dc="http://purl.org/dc/elements/1.1/" 
                              xmlns:mets="http://www.loc.gov/METS/" 
                              xmlns:xlink="http://www.w3.org/TR/xlink">

  <xsl:output method="html" indent="yes" encoding="UTF-8" media-type="text/html" doctype-public="-//W3C//DTD HTML 4.0//EN"/>
  
  <!-- ====================================================================== -->
  <!-- Parameters                                                             -->
  <!-- ====================================================================== -->

  <!-- Full Text -->
  <xsl:param name="text"/>
  <xsl:param name="text-join"/>
  <xsl:param name="text-prox"/>
  <xsl:param name="text-exclude"/>
  
  <!-- Dublin Core Metadata Elements -->
  <xsl:param name="title"/>
  <xsl:param name="title-join"/>
  <xsl:param name="title-prox"/>
  <xsl:param name="title-exclude"/>
  
  <xsl:param name="creator"/>
  <xsl:param name="creator-join"/>
  <xsl:param name="creator-prox"/>
  <xsl:param name="creator-exclude"/>
  
  <xsl:param name="subject"/>
  <xsl:param name="subject-join"/>
  <xsl:param name="subject-prox"/>
  <xsl:param name="subject-exclude"/>
  
  <xsl:param name="description"/>
  <xsl:param name="description-join"/>
  <xsl:param name="description-prox"/>
  <xsl:param name="description-exclude"/>

  <xsl:param name="publisher"/>
  <xsl:param name="publisher-join"/>
  <xsl:param name="publisher-prox"/>
  <xsl:param name="publisher-exclude"/>

  <xsl:param name="contributor"/>
  <xsl:param name="contributor-join"/>
  <xsl:param name="contributor-prox"/>
  <xsl:param name="contributor-exclude"/>

  <xsl:param name="date"/>
  <xsl:param name="date-join"/>
  <xsl:param name="date-prox"/>
  <xsl:param name="date-exclude"/>

  <xsl:param name="type"/>
  <xsl:param name="type-join"/>
  <xsl:param name="type-prox"/>
  <xsl:param name="type-exclude"/>

  <xsl:param name="format"/>
  <xsl:param name="format-join"/>
  <xsl:param name="format-prox"/>
  <xsl:param name="format-exclude"/>

  <xsl:param name="identifier"/>
  <xsl:param name="identifier-join"/>
  <xsl:param name="identifier-prox"/>
  <xsl:param name="identifier-exclude"/>

  <xsl:param name="source"/>
  <xsl:param name="source-join"/>
  <xsl:param name="source-prox"/>
  <xsl:param name="source-exclude"/>

  <xsl:param name="language"/>
  <xsl:param name="language-join"/>
  <xsl:param name="language-prox"/>
  <xsl:param name="language-exclude"/>
 
  <xsl:param name="relation"/>
  <xsl:param name="relation-join"/>
  <xsl:param name="relation-prox"/>
  <xsl:param name="relation-exclude"/>
 
  <xsl:param name="coverage"/>
  <xsl:param name="coverage-join"/>
  <xsl:param name="coverage-prox"/>
  <xsl:param name="coverage-exclude"/>
  
  <xsl:param name="rights"/>
  <xsl:param name="rights-join"/>
  <xsl:param name="rights-prox"/>
  <xsl:param name="rights-exclude"/>

  <!-- Special XTF Metadata Field based on Date -->
  <xsl:param name="year"/>
  <xsl:param name="year-join"/>
  <xsl:param name="year-prox"/>
  <xsl:param name="year-exclude"/>

  <!-- Structural Search -->
  <xsl:param name="sectionType"/>

  <!-- Search and Result Behavior URL Parameters -->
  <xsl:param name="smode"/>
  <xsl:param name="rmode"/>
  <xsl:param name="sort"/>

  <xsl:param name="startDoc" select="1"/>

  <!-- Documents per Page -->
  <xsl:param name="docsPerPage">
    <xsl:choose>
      <xsl:when test="$smode = 'test' or $smode = 'debug'">
        <xsl:value-of select="10000"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="20"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:param>

  <!-- Path Parameters -->
  <xsl:param name="servlet.path"/>
  <xsl:param name="dynaxml.path" select="if (matches($servlet.path, 'org.cdlib.xtf.crossQuery.CrossQuery')) then 'org.cdlib.xtf.dynaXML.DynaXML' else 'view'"/>

  <!-- Query String -->
  
  <xsl:param name="queryString">
    <xsl:call-template name="queryString">
      <xsl:with-param name="textParams" select="'text text-join text-exclude text-prox title title-join title-prox title-exclude creator creator-join creator-prox creator-exclude subject subject-join subject-prox subject-exclude description description-join description-prox description-exclude publisher publisher-join publisher-prox publisher-exclude contributor contributor-join contributor-prox contributor-exclude date date-join date-prox date-exclude type type-join type-prox type-exclude format format-join format-prox format-exclude identifier identifier-join identifier-prox identifier-exclude source source-join source-prox source-exclude language language-join language-prox language-exclude relation relation-join relation-prox relation-exclude coverage coverage-join coverage-prox coverage-exclude rights rights-join rights-prox rights-exclude year year-join year-prox year-exclude sectionType rmode '"/>
      <xsl:with-param name="count" select="1"/>
    </xsl:call-template>
  </xsl:param>
  
  <xsl:template name="queryString">
    <xsl:param name="textParams"/>
    <xsl:param name="count"/>
        
    <xsl:variable name="param" select="substring-before($textParams, ' ')"/>

    <xsl:if test="$param != ''">
      <xsl:choose>
        <xsl:when test="$param = 'text' and $text">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>text=</xsl:text>
          <xsl:value-of select="$text"/>
        </xsl:when>
        <xsl:when test="$param = 'text-join' and $text-join">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>text-join=</xsl:text>
          <xsl:value-of select="$text-join"/>
        </xsl:when>
        <xsl:when test="$param = 'text-exclude' and $text-exclude">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>text-exclude=</xsl:text>
          <xsl:value-of select="$text-exclude"/>
        </xsl:when>
        <xsl:when test="$param = 'text-prox' and $text-prox">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>text-prox=</xsl:text>
          <xsl:value-of select="$text-prox"/>
        </xsl:when>
        <xsl:when test="$param = 'title' and $title">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>title=</xsl:text>
          <xsl:value-of select="$title"/>
        </xsl:when>
        <xsl:when test="$param = 'title-join' and $title-join">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>title-join=</xsl:text>
          <xsl:value-of select="$title-join"/>
        </xsl:when>
        <xsl:when test="$param = 'title-prox' and $title-prox">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>title-prox=</xsl:text>
          <xsl:value-of select="$title-prox"/>
        </xsl:when>
        <xsl:when test="$param = 'title-exclude' and $title-exclude">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>title-exclude=</xsl:text>
          <xsl:value-of select="$title-exclude"/>
        </xsl:when>
        <xsl:when test="$param = 'creator' and $creator">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>creator=</xsl:text>
          <xsl:value-of select="$creator"/>
        </xsl:when>
        <xsl:when test="$param = 'creator-join' and $creator-join">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>creator-join=</xsl:text>
          <xsl:value-of select="$creator-join"/>
        </xsl:when>
        <xsl:when test="$param = 'creator-prox' and $creator-prox">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>creator-prox=</xsl:text>
          <xsl:value-of select="$creator-prox"/>
        </xsl:when>
        <xsl:when test="$param = 'creator-exclude' and $creator-exclude">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>creator-exclude=</xsl:text>
          <xsl:value-of select="$creator-exclude"/>
        </xsl:when>
        <xsl:when test="$param = 'subject' and $subject">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>subject=</xsl:text>
          <xsl:value-of select="$subject"/>
        </xsl:when>
        <xsl:when test="$param = 'subject-join' and $subject-join">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>subject-join=</xsl:text>
          <xsl:value-of select="$subject-join"/>
        </xsl:when>
        <xsl:when test="$param = 'subject-prox' and $subject-prox">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>subject-prox=</xsl:text>
          <xsl:value-of select="$subject-prox"/>
        </xsl:when>
        <xsl:when test="$param = 'subject-exclude' and $subject-exclude">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>subject-exclude=</xsl:text>
          <xsl:value-of select="$subject-exclude"/>
        </xsl:when>
        <xsl:when test="$param = 'description' and $description">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>description=</xsl:text>
          <xsl:value-of select="$description"/>
        </xsl:when>
        <xsl:when test="$param = 'description-join' and $description-join">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>description-join=</xsl:text>
          <xsl:value-of select="$description-join"/>
        </xsl:when>
        <xsl:when test="$param = 'description-prox' and $description-prox">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>description-prox=</xsl:text>
          <xsl:value-of select="$description-prox"/>
        </xsl:when>
        <xsl:when test="$param = 'description-exclude' and $description-exclude">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>description-exclude=</xsl:text>
          <xsl:value-of select="$description-exclude"/>
        </xsl:when>
        <xsl:when test="$param = 'publisher' and $publisher">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>publisher=</xsl:text>
          <xsl:value-of select="$publisher"/>
        </xsl:when>
        <xsl:when test="$param = 'publisher-join' and $publisher-join">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>publisher-join=</xsl:text>
          <xsl:value-of select="$publisher-join"/>
        </xsl:when>
        <xsl:when test="$param = 'publisher-prox' and $publisher-prox">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>publisher-prox=</xsl:text>
          <xsl:value-of select="$publisher-prox"/>
        </xsl:when>
        <xsl:when test="$param = 'publisher-exclude' and $publisher-exclude">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>publisher-exclude=</xsl:text>
          <xsl:value-of select="$publisher-exclude"/>
        </xsl:when>
        <xsl:when test="$param = 'contributor' and $contributor">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>contributor=</xsl:text>
          <xsl:value-of select="$contributor"/>
        </xsl:when>
        <xsl:when test="$param = 'contributor-join' and $contributor-join">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>contributor-join=</xsl:text>
          <xsl:value-of select="$contributor-join"/>
        </xsl:when>
        <xsl:when test="$param = 'contributor-prox' and $contributor-prox">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>contributor-prox=</xsl:text>
          <xsl:value-of select="$contributor-prox"/>
        </xsl:when>
        <xsl:when test="$param = 'contributor-exclude' and $contributor-exclude">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>contributor-exclude=</xsl:text>
          <xsl:value-of select="$contributor-exclude"/>
        </xsl:when>
        <xsl:when test="$param = 'date' and $date">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>date=</xsl:text>
          <xsl:value-of select="$date"/>
        </xsl:when>
        <xsl:when test="$param = 'date-join' and $date-join">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>date-join=</xsl:text>
          <xsl:value-of select="$date-join"/>
        </xsl:when>
        <xsl:when test="$param = 'date-prox' and $date-prox">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>date-prox=</xsl:text>
          <xsl:value-of select="$date-prox"/>
        </xsl:when>
        <xsl:when test="$param = 'date-exclude' and $date-exclude">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>date-exclude=</xsl:text>
          <xsl:value-of select="$date-exclude"/>
        </xsl:when>
        <xsl:when test="$param = 'type' and $type">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>type=</xsl:text>
          <xsl:value-of select="$type"/>
        </xsl:when>
        <xsl:when test="$param = 'type-join' and $type-join">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>type-join=</xsl:text>
          <xsl:value-of select="$type-join"/>
        </xsl:when>
        <xsl:when test="$param = 'type-prox' and $type-prox">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>type-prox=</xsl:text>
          <xsl:value-of select="$type-prox"/>
        </xsl:when>
        <xsl:when test="$param = 'type-exclude' and $type-exclude">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>type-exclude=</xsl:text>
          <xsl:value-of select="$type-exclude"/>
        </xsl:when>
        <xsl:when test="$param = 'format' and $format">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>format=</xsl:text>
          <xsl:value-of select="$format"/>
        </xsl:when>
        <xsl:when test="$param = 'format-join' and $format-join">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>format-join=</xsl:text>
          <xsl:value-of select="$format-join"/>
        </xsl:when>
        <xsl:when test="$param = 'format-prox' and $format-prox">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>format-prox=</xsl:text>
          <xsl:value-of select="$format-prox"/>
        </xsl:when>
        <xsl:when test="$param = 'format-exclude' and $format-exclude">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>format-exclude=</xsl:text>
          <xsl:value-of select="$format-exclude"/>
        </xsl:when>
        <xsl:when test="$param = 'identifier' and $identifier">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>identifier=</xsl:text>
          <xsl:value-of select="$identifier"/>
        </xsl:when>
        <xsl:when test="$param = 'identifier-join' and $identifier-join">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>identifier-join=</xsl:text>
          <xsl:value-of select="$identifier-join"/>
        </xsl:when>
        <xsl:when test="$param = 'identifier-prox' and $identifier-prox">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>identifier-prox=</xsl:text>
          <xsl:value-of select="$identifier-prox"/>
        </xsl:when>
        <xsl:when test="$param = 'identifier-exclude' and $identifier-exclude">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>identifier-exclude=</xsl:text>
          <xsl:value-of select="$identifier-exclude"/>
        </xsl:when>
        <xsl:when test="$param = 'source' and $source">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>source=</xsl:text>
          <xsl:value-of select="$source"/>
        </xsl:when>
        <xsl:when test="$param = 'source-join' and $source-join">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>source-join=</xsl:text>
          <xsl:value-of select="$source-join"/>
        </xsl:when>
        <xsl:when test="$param = 'source-prox' and $source-prox">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>source-prox=</xsl:text>
          <xsl:value-of select="$source-prox"/>
        </xsl:when>
        <xsl:when test="$param = 'source-exclude' and $source-exclude">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>source-exclude=</xsl:text>
          <xsl:value-of select="$source-exclude"/>
        </xsl:when>
        <xsl:when test="$param = 'language' and $language">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>language=</xsl:text>
          <xsl:value-of select="$language"/>
        </xsl:when>
        <xsl:when test="$param = 'language-join' and $language-join">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>language-join=</xsl:text>
          <xsl:value-of select="$language-join"/>
        </xsl:when>
        <xsl:when test="$param = 'language-prox' and $language-prox">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>language-prox=</xsl:text>
          <xsl:value-of select="$language-prox"/>
        </xsl:when>
        <xsl:when test="$param = 'language-exclude' and $language-exclude">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>language-exclude=</xsl:text>
          <xsl:value-of select="$language-exclude"/>
        </xsl:when>
        <xsl:when test="$param = 'relation' and $relation">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>relation=</xsl:text>
          <xsl:value-of select="$relation"/>
        </xsl:when>
        <xsl:when test="$param = 'relation-join' and $relation-join">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>relation-join=</xsl:text>
          <xsl:value-of select="$relation-join"/>
        </xsl:when>
        <xsl:when test="$param = 'relation-prox' and $relation-prox">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>relation-prox=</xsl:text>
          <xsl:value-of select="$relation-prox"/>
        </xsl:when>
        <xsl:when test="$param = 'relation-exclude' and $relation-exclude">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>relation-exclude=</xsl:text>
          <xsl:value-of select="$relation-exclude"/>
        </xsl:when>
        <xsl:when test="$param = 'coverage' and $coverage">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>coverage=</xsl:text>
          <xsl:value-of select="$coverage"/>
        </xsl:when>
        <xsl:when test="$param = 'coverage-join' and $coverage-join">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>coverage-join=</xsl:text>
          <xsl:value-of select="$coverage-join"/>
        </xsl:when>
        <xsl:when test="$param = 'coverage-prox' and $coverage-prox">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>coverage-prox=</xsl:text>
          <xsl:value-of select="$coverage-prox"/>
        </xsl:when>
        <xsl:when test="$param = 'coverage-exclude' and $coverage-exclude">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>coverage-exclude=</xsl:text>
          <xsl:value-of select="$coverage-exclude"/>
        </xsl:when>
        <xsl:when test="$param = 'rights' and $rights">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>rights=</xsl:text>
          <xsl:value-of select="$rights"/>
        </xsl:when>
        <xsl:when test="$param = 'rights-join' and $rights-join">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>rights-join=</xsl:text>
          <xsl:value-of select="$rights-join"/>
        </xsl:when>
        <xsl:when test="$param = 'rights-prox' and $rights-prox">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>rights-prox=</xsl:text>
          <xsl:value-of select="$rights-prox"/>
        </xsl:when>
        <xsl:when test="$param = 'rights-exclude' and $rights-exclude">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>rights-exclude=</xsl:text>
          <xsl:value-of select="$rights-exclude"/>
        </xsl:when>
        <xsl:when test="$param = 'year' and $year">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>year=</xsl:text>
          <xsl:value-of select="$year"/>
        </xsl:when>
        <xsl:when test="$param = 'year-join' and $year-join">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>year-join=</xsl:text>
          <xsl:value-of select="$year-join"/>
        </xsl:when>
        <xsl:when test="$param = 'year-prox' and $year-prox">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>year-prox=</xsl:text>
          <xsl:value-of select="$year-prox"/>
        </xsl:when>
        <xsl:when test="$param = 'year-exclude' and $year-exclude">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>year-exclude=</xsl:text>
          <xsl:value-of select="$year-exclude"/>
        </xsl:when>
        <xsl:when test="$param = 'sectionType' and $sectionType">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>sectionType=</xsl:text>
          <xsl:value-of select="$sectionType"/>
        </xsl:when>
        <xsl:when test="$param = 'rmode' and $rmode">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>rmode=</xsl:text>
          <xsl:value-of select="$rmode"/>
        </xsl:when>
      </xsl:choose>
      <xsl:call-template name="queryString">
        <xsl:with-param name="textParams" select="replace(substring-after($textParams, $param), '^ ', '')"/>
        <xsl:with-param name="count" select="$count + 1"/>
      </xsl:call-template>
    </xsl:if>
    
  </xsl:template>
  
  <!-- Human Readable Form of Query -->
  
  <xsl:param name="query">
    <!-- Will have to do some regex cleanup of string here. -->
    <xsl:value-of select="$queryString"/>
  </xsl:param>
    
  <!-- ====================================================================== -->
  <!-- Result Paging                                                          -->
  <!-- ====================================================================== -->

  <xsl:template name="pages">

    <xsl:variable name="nPages" select="floor(((@totalDocs+number($docsPerPage))-1) div number($docsPerPage))+1"/>

    <xsl:variable name="showPages">
      <xsl:choose>
        <xsl:when test="$nPages >= 11">
          <xsl:value-of select="number(10)"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$nPages - 1"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:for-each select="(1 to 10)">
      <!-- Figure out what page we're on -->
      <xsl:variable name="pageNum" select="position()"/>
      <xsl:variable name="pageStart" select="(($pageNum - 1) * number($docsPerPage)) + 1"/>
  
      <xsl:if test="($pageNum = 1) and ($pageStart != number($startDoc))">
         <xsl:variable name="prevPage" select="number($startDoc) - number($docsPerPage)"/>
        <a href="{$servlet.path}?{$queryString}&amp;startDoc={$prevPage}">prev</a>
        <xsl:text>&#160;&#160;</xsl:text>
      </xsl:if>
            
      <!-- If there are hits on the page, show it -->
      <xsl:if test="$nPages &gt; number(.)">
        <xsl:choose>
          <!-- Make a hyperlink if it's not the page we're currently on. -->
          <xsl:when test="($pageStart != number($startDoc))">
            <a href="{$servlet.path}?{$queryString}&amp;startDoc={$pageStart}">
              <xsl:value-of select="$pageNum"/>
            </a>
            <xsl:if test="$pageNum &lt; $showPages">
              <xsl:text>, </xsl:text>
            </xsl:if>
          </xsl:when>
          <xsl:when test="($pageStart = number($startDoc))">
            <xsl:value-of select="$pageNum"/>
            <xsl:if test="$pageNum &lt; $showPages">
              <xsl:text>, </xsl:text>
            </xsl:if>
          </xsl:when>
        </xsl:choose>
      </xsl:if>
    
    <xsl:if test="($pageNum = $showPages) and ($pageStart != number($startDoc))">
      <xsl:variable name="nextPage" select="number($startDoc) + number($docsPerPage)"/>
      <xsl:text>&#160;&#160;</xsl:text>
      <a href="{$servlet.path}?{$queryString}&amp;startDoc={$nextPage}">next</a>
    </xsl:if>

    </xsl:for-each>

  </xsl:template>
    
  <!-- ====================================================================== -->
  <!-- Help Page                                                              -->
  <!-- ====================================================================== -->

  <xsl:template name="help">
    <br/>
    <hr/>
    <br/>
    <h3>Sample Queries</h3>
    <blockquote>
      <h4>Keyword</h4>
      <ul>
        <li>
          <font color="brown">south africa</font>&#160;<i>All keywords and'd together.</i>
        </li>
      </ul>
      <h4>Phrase</h4>
      <ul>
        <li>
          <font color="brown">"south africa"</font>&#160;<i>Full phrase only.</i>
        </li>
      </ul>
      <h4>Wildcards</h4>
      <ul>
        <li>
          <font color="brown">apar*</font>&#160;<i>0 or more characters following the string</i>
        </li>
        <li>
          <font color="brown">apar?</font>&#160;<i>Each '?' stands for 1 character</i>
        </li>
      </ul>
      <h4>Diacritics</h4>
      <ul>
        <li>
          <font color="brown">S&#x00E9;gr&#x00E9;gation</font>&#160;<i>Note: Some
            interface problems here.</i>
        </li>
      </ul>
      <h4>Boolean Searches</h4>
      <p>"and, "or", and "not" are supported.</p>
      <ul>
        <li>
          <font color="brown">apartheid and race not africa</font>
        </li>
        <li>
          <font color="brown">apartheid and race or south and africa</font>
          <ul>
            <li>Equivalent to: <font color="brown">(apartheid and race) or (south and africa)</font>
            </li>
            <li>Equivalent to: <font color="brown">(apartheid race) or (south africa)</font>
            </li>
          </ul>
        </li>
      </ul>
      <h4>Proximity Searches</h4>
      <ul>
        <li>
          <font color="brown">"famine food"~5</font>&#160;<i>Find 'famine' within 5 words of 'food'.</i>
        </li>
      </ul>
      <h4>Metadata Searching</h4>
      <ul>
        <li>Search any of the dublin core fields indexed for an object (title, creator, subject,
          description, date, and relation) using any of the search features listed above.</li>
      </ul>
      <h4>Date Range Searches</h4>
      <ul>
        <li>
          <font color="brown">&gt;2004, &gt;=1999, &lt;1980, &lt;=2000, or
            1999..2004</font>&#160;<i>Note: Some date normalization work remains to be done</i>
        </li>
      </ul>
    </blockquote>
  </xsl:template>

  <!-- ====================================================================== -->
  <!-- Subject Links                                                          -->
  <!-- ====================================================================== -->
      
  <xsl:template match="subject">
    <a href="{$servlet.path}?subject=%22{.}%22&amp;relation={$relation}&amp;rmode={$rmode}">
      <xsl:apply-templates/>
    </a>
    <xsl:if test="not(position() = last())">
      <xsl:text> | </xsl:text>
    </xsl:if>
  </xsl:template>
    
  <!-- ====================================================================== -->
  <!-- Sort Options                                                           -->
  <!-- ====================================================================== -->
   
  <xsl:template name="sort.options">
    <xsl:choose>
      <xsl:when test="$sort = ''">
        <span class="select">Relevance</span>
        <xsl:text> | </xsl:text>
        <a href="{$servlet.path}?{$queryString}&amp;sort=title">Title</a> 
        <xsl:text> | </xsl:text>
        <a href="{$servlet.path}?{$queryString}&amp;sort=creator">Author</a>
        <xsl:text> | </xsl:text>
        <a href="{$servlet.path}?{$queryString}&amp;sort=year">Year</a>
      </xsl:when>
      <xsl:when test="$sort = 'title'">
        <a href="{$servlet.path}?{$queryString}">Relevance</a> 
        <xsl:text> | </xsl:text>
        <span class="select">Title</span>
        <xsl:text> | </xsl:text>
        <a href="{$servlet.path}?{$queryString}&amp;sort=creator">Author</a>
        <xsl:text> | </xsl:text>
        <a href="{$servlet.path}?{$queryString}&amp;sort=year">Year</a>
      </xsl:when>
      <xsl:when test="$sort = 'creator'">
        <a href="{$servlet.path}?{$queryString}">Relevance</a> 
        <xsl:text> | </xsl:text>
        <a href="{$servlet.path}?{$queryString}&amp;sort=title">Title</a>
        <xsl:text> | </xsl:text>
        <span class="select">Author</span>
        <xsl:text> | </xsl:text>
        <a href="{$servlet.path}?{$queryString}&amp;sort=year">Year</a>
      </xsl:when>
      <xsl:when test="$sort = 'year'">
        <a href="{$servlet.path}?{$queryString}">Relevance</a> 
        <xsl:text> | </xsl:text>
        <a href="{$servlet.path}?{$queryString}&amp;sort=title">Title</a>
        <xsl:text> | </xsl:text>
        <a href="{$servlet.path}?{$queryString}&amp;sort=creator">Author</a>
        <xsl:text> | </xsl:text>
        <span class="select">Year</span>
      </xsl:when>
    </xsl:choose>
  </xsl:template>
    
  <!-- ====================================================================== -->
  <!-- Access Options                                                         -->
  <!-- ====================================================================== -->
   
  <xsl:template name="access.options">
    <xsl:variable name="queryString" select="replace($queryString, 'rights=[a-z]+&amp;', '')"/>
    <xsl:choose>
      <xsl:when test="$rights = ''">
        <span class="select">All</span>
        <xsl:text> | </xsl:text>
        <a href="{$servlet.path}?{$queryString}&amp;rights=public">Public</a> 
        <xsl:text> | </xsl:text>
        <a href="{$servlet.path}?{$queryString}&amp;rights=uconly">UC Only</a>
      </xsl:when>
      <xsl:when test="$rights = 'public'">
        <a href="{$servlet.path}?{$queryString}">All</a> 
        <xsl:text> | </xsl:text>
        <span class="select">Public</span>
        <xsl:text> | </xsl:text>
        <a href="{$servlet.path}?{$queryString}&amp;rights=uconly">UC Only</a>    
      </xsl:when>
      <xsl:when test="$rights = 'uconly'">
        <a href="{$servlet.path}?{$queryString}">All</a> 
        <xsl:text> | </xsl:text>
        <a href="{$servlet.path}?{$queryString}&amp;rights=public">Public</a>
        <xsl:text> | </xsl:text>
        <span class="select">UC Only</span>
      </xsl:when>
    </xsl:choose>
  </xsl:template>
    
  <!-- ====================================================================== -->
  <!-- Alpha Anchors                                                          -->
  <!-- ====================================================================== -->

  <xsl:template name="alpha.anchors">
    
    <xsl:param name="alpha-list"/>
    <xsl:param name="anchor-list"/>
    
    <xsl:variable name="char" select="substring($alpha-list, 1, 1)"/>
    
    <xsl:if test="$alpha-list != ''">
      <xsl:if test="contains($anchor-list, $char)">
        <a href="#{$char}"><xsl:value-of select="$char"/></a>
        <xsl:if test="replace($anchor-list, $char, '') != ''">
          <xsl:text> | </xsl:text>
        </xsl:if>
      </xsl:if>
      <xsl:call-template name="alpha.anchors">
        <xsl:with-param name="alpha-list" select="substring-after($alpha-list, $char)"/>
        <xsl:with-param name="anchor-list" select="replace($anchor-list, $char, '')"/>
      </xsl:call-template>
    </xsl:if>
    
  </xsl:template>
  
  <!-- ====================================================================== -->
  <!-- Generate ARK List for Testing                                          -->
  <!-- ====================================================================== -->

  <!-- Leave indenting as is in the following template! -->

  <xsl:template match="crossQueryResult" mode="test">
    <html>
      <head>
        <title>XTF: Test Results</title>
      </head>
      <body>
        <h1>XTF: Test Results</h1>
        <pre>
          <xsl:for-each select="docHit">
            <xsl:sort select="substring(meta/identifier, string-length(meta/identifier)-9)"/>
<xsl:text>
</xsl:text>
<xsl:value-of select="substring(meta/identifier, string-length(meta/identifier)-9)"/>
          </xsl:for-each>
<xsl:text>
</xsl:text>
        </pre>
      </body>
    </html>
  </xsl:template>
    
  <!-- ====================================================================== -->
  <!-- Debugging Templates                                                    -->
  <!-- ====================================================================== -->
  
  <xsl:template match="*" mode="debug">
    <xsl:call-template name="write-starttag"/>
    <xsl:apply-templates mode="debug"/>   
    <xsl:if test="*|text()|comment()|processing-instruction()">
      <xsl:call-template name="write-endtag"/>
    </xsl:if>
  </xsl:template>
  
  <xsl:template name="write-starttag">
    <span style="color: green">
      <xsl:text>&lt;</xsl:text>
      <xsl:value-of select="name()"/>
      <xsl:for-each select="@*">
        <xsl:call-template name="write-attribute"/>
      </xsl:for-each>
      <xsl:if test="not(*|text()|comment()|processing-instruction())"> /</xsl:if>
      <xsl:text>&gt;</xsl:text>
    </span>
  </xsl:template>
  
  <xsl:template name="write-endtag">
    <span style="color: green">
      <xsl:text>&lt;/</xsl:text>
      <xsl:value-of select="name()"/>
      <xsl:text>&gt;</xsl:text>
    </span>
  </xsl:template>
  
  <xsl:template name="write-attribute">
    <span style="color: orange">
      <xsl:text> </xsl:text>
      <xsl:value-of select="name()"/>
      <xsl:text>="</xsl:text>
      <xsl:value-of select="."/>
      <xsl:text>"</xsl:text>
    </span>
  </xsl:template>
  

</xsl:stylesheet>
