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
                              xmlns:xs="http://www.w3.org/2001/XMLSchema">
  
  <!-- ====================================================================== -->
  <!-- Output Parameters                                                      -->
  <!-- ====================================================================== -->

  <xsl:output method="xhtml" indent="yes" encoding="UTF-8" media-type="text/html" 
              doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN" 
              doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd" 
              omit-xml-declaration="yes"/>

  <!-- ====================================================================== -->
  <!-- Parameters                                                             -->
  <!-- ====================================================================== -->

  <!-- Full Text -->
  <xsl:param name="text"/>
  <xsl:param name="text-join"/>
  <xsl:param name="text-prox"/>
  <xsl:param name="text-exclude"/>
  <xsl:param name="text-max"/>
  
  <!-- Dublin Core Metadata Elements -->
  <xsl:param name="title"/>
  <xsl:param name="title-join"/>
  <xsl:param name="title-prox"/>
  <xsl:param name="title-exclude"/>
  <xsl:param name="title-max"/>
  
  <xsl:param name="creator"/>
  <xsl:param name="creator-join"/>
  <xsl:param name="creator-prox"/>
  <xsl:param name="creator-exclude"/>
  <xsl:param name="creator-max"/>
  
  <xsl:param name="subject"/>
  <xsl:param name="subject-join"/>
  <xsl:param name="subject-prox"/>
  <xsl:param name="subject-exclude"/>
  <xsl:param name="subject-max"/>
  
  <xsl:param name="description"/>
  <xsl:param name="description-join"/>
  <xsl:param name="description-prox"/>
  <xsl:param name="description-exclude"/>
  <xsl:param name="description-max"/>

  <xsl:param name="publisher"/>
  <xsl:param name="publisher-join"/>
  <xsl:param name="publisher-prox"/>
  <xsl:param name="publisher-exclude"/>
  <xsl:param name="publisher-max"/>

  <xsl:param name="contributor"/>
  <xsl:param name="contributor-join"/>
  <xsl:param name="contributor-prox"/>
  <xsl:param name="contributor-exclude"/>
  <xsl:param name="contributor-max"/>

  <xsl:param name="date"/>
  <xsl:param name="date-join"/>
  <xsl:param name="date-prox"/>
  <xsl:param name="date-exclude"/>
  <xsl:param name="date-max"/>

  <xsl:param name="type"/>
  <xsl:param name="type-join"/>
  <xsl:param name="type-prox"/>
  <xsl:param name="type-exclude"/>
  <xsl:param name="type-max"/>

  <xsl:param name="format"/>
  <xsl:param name="format-join"/>
  <xsl:param name="format-prox"/>
  <xsl:param name="format-exclude"/>
  <xsl:param name="format-max"/>

  <xsl:param name="identifier"/>
  <xsl:param name="identifier-join"/>
  <xsl:param name="identifier-prox"/>
  <xsl:param name="identifier-exclude"/>
  <xsl:param name="identifier-max"/>

  <xsl:param name="source"/>
  <xsl:param name="source-join"/>
  <xsl:param name="source-prox"/>
  <xsl:param name="source-exclude"/>
  <xsl:param name="source-max"/>

  <xsl:param name="language"/>
  <xsl:param name="language-join"/>
  <xsl:param name="language-prox"/>
  <xsl:param name="language-exclude"/>
  <xsl:param name="language-max"/>
 
  <xsl:param name="relation"/>
  <xsl:param name="relation-join"/>
  <xsl:param name="relation-prox"/>
  <xsl:param name="relation-exclude"/>
  <xsl:param name="relation-max"/>
 
  <xsl:param name="coverage"/>
  <xsl:param name="coverage-join"/>
  <xsl:param name="coverage-prox"/>
  <xsl:param name="coverage-exclude"/>
  <xsl:param name="coverage-max"/>
  
  <xsl:param name="rights"/>
  <xsl:param name="rights-join"/>
  <xsl:param name="rights-prox"/>
  <xsl:param name="rights-exclude"/>
  <xsl:param name="rights-max"/>

  <!-- Special XTF Metadata Field based on Date -->
  <xsl:param name="year"/>
  <xsl:param name="year-join"/>
  <xsl:param name="year-prox"/>
  <xsl:param name="year-exclude"/>
  <xsl:param name="year-max"/>

  <!-- Special XTF Metadata Field containing CDL Profile ARK -->
  <xsl:param name="profile"/>
  <xsl:param name="profile-join"/>
  <xsl:param name="profile-prox"/>
  <xsl:param name="profile-exclude"/>
  <xsl:param name="profile-max"/>

  <!-- Structural Search -->
  <xsl:param name="sectionType"/>

  <!-- Search and Result Behavior URL Parameters -->
  <xsl:param name="smode"/>
  <xsl:param name="rmode"/>
  <xsl:param name="sort"/>

  <!-- Paging Parameters-->  
  <xsl:param name="startDoc" as="xs:integer" select="1"/>
  <!-- Documents per Page -->
  <xsl:param name="docsPerPage" as="xs:integer">
    <xsl:choose>
      <xsl:when test="$smode = 'test' or $smode = 'debug'">
        <xsl:value-of select="10000"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="20"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:param>
  <!-- Page Block Size -->
  <xsl:param name="blockSize" as="xs:integer" select="10"/>
  <!-- Maximum number of hits allowed -->
  <xsl:param name="maxHits" as="xs:integer" select="1000"/>  
  <!-- Maximum Pages -->
  <xsl:param name="maxPages" as="xs:integer" select="100"/>

  <!-- Path Parameters -->
  <xsl:param name="servlet.path"/>
  <xsl:param name="dynaxml.path" select="if (matches($servlet.path, 'org.cdlib.xtf.crossQuery.CrossQuery')) then 'org.cdlib.xtf.dynaXML.DynaXML' else 'view'"/>

  <!-- Query String -->
  
  <xsl:param name="queryString">
    <xsl:call-template name="queryString">
      <xsl:with-param name="textParams" select="'text text-join text-prox text-exclude text-max title title-join title-prox title-exclude title-max creator creator-join creator-prox creator-exclude creator-max subject subject-join subject-prox subject-exclude subject-max description description-join description-prox description-exclude description-max publisher publisher-join publisher-prox publisher-exclude publisher-max contributor contributor-join contributor-prox contributor-exclude contributor-max date date-join date-prox date-exclude date-max type type-join type-prox type-exclude type-max format format-join format-prox format-exclude format-max identifier identifier-join identifier-prox identifier-exclude identifier-max source source-join source-prox source-exclude source-max language language-join language-prox language-exclude language-max relation relation-join relation-prox relation-exclude relation-max coverage coverage-join coverage-prox coverage-exclude coverage-max rights rights-join rights-prox rights-exclude rights-max year year-join year-prox year-exclude year-max profile profile-join profile-prox profile-exclude profile-max sectionType rmode '"/>
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
        <xsl:when test="$param = 'text-prox' and $text-prox">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>text-prox=</xsl:text>
          <xsl:value-of select="$text-prox"/>
        </xsl:when>
        <xsl:when test="$param = 'text-exclude' and $text-exclude">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>text-exclude=</xsl:text>
          <xsl:value-of select="$text-exclude"/>
        </xsl:when>
        <xsl:when test="$param = 'text-max' and $text-max">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>text-max=</xsl:text>
          <xsl:value-of select="$text-max"/>
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
        <xsl:when test="$param = 'title-max' and $title-max">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>title-max=</xsl:text>
          <xsl:value-of select="$title-max"/>
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
        <xsl:when test="$param = 'creator-max' and $creator-max">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>creator-max=</xsl:text>
          <xsl:value-of select="$creator-max"/>
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
        <xsl:when test="$param = 'subject-max' and $subject-max">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>subject-max=</xsl:text>
          <xsl:value-of select="$subject-max"/>
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
        <xsl:when test="$param = 'description-max' and $description-max">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>description-max=</xsl:text>
          <xsl:value-of select="$description-max"/>
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
        <xsl:when test="$param = 'publisher-max' and $publisher-max">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>publisher-max=</xsl:text>
          <xsl:value-of select="$publisher-max"/>
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
        <xsl:when test="$param = 'contributor-max' and $contributor-max">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>contributor-max=</xsl:text>
          <xsl:value-of select="$contributor-max"/>
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
        <xsl:when test="$param = 'date-max' and $date-max">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>date-max=</xsl:text>
          <xsl:value-of select="$date-max"/>
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
        <xsl:when test="$param = 'type-max' and $type-max">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>type-max=</xsl:text>
          <xsl:value-of select="$type-max"/>
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
        <xsl:when test="$param = 'format-max' and $format-max">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>format-max=</xsl:text>
          <xsl:value-of select="$format-max"/>
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
        <xsl:when test="$param = 'identifier-max' and $identifier-max">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>identifier-max=</xsl:text>
          <xsl:value-of select="$identifier-max"/>
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
        <xsl:when test="$param = 'source-max' and $source-max">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>source-max=</xsl:text>
          <xsl:value-of select="$source-max"/>
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
        <xsl:when test="$param = 'language-max' and $language-max">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>language-max=</xsl:text>
          <xsl:value-of select="$language-max"/>
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
        <xsl:when test="$param = 'relation-max' and $relation-max">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>relation-max=</xsl:text>
          <xsl:value-of select="$relation-max"/>
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
        <xsl:when test="$param = 'coverage-max' and $coverage-max">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>coverage-max=</xsl:text>
          <xsl:value-of select="$coverage-max"/>
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
        <xsl:when test="$param = 'rights-max' and $rights-max">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>rights-max=</xsl:text>
          <xsl:value-of select="$rights-max"/>
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
        <xsl:when test="$param = 'year-max' and $year-max">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>year-max=</xsl:text>
          <xsl:value-of select="$year-max"/>
        </xsl:when>
        <xsl:when test="$param = 'profile' and $profile">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>profile=</xsl:text>
          <xsl:value-of select="$profile"/>
        </xsl:when>
        <xsl:when test="$param = 'profile-join' and $profile-join">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>profile-join=</xsl:text>
          <xsl:value-of select="$profile-join"/>
        </xsl:when>
        <xsl:when test="$param = 'profile-prox' and $profile-prox">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>profile-prox=</xsl:text>
          <xsl:value-of select="$profile-prox"/>
        </xsl:when>
        <xsl:when test="$param = 'profile-exclude' and $profile-exclude">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>profile-exclude=</xsl:text>
          <xsl:value-of select="$profile-exclude"/>
        </xsl:when>
        <xsl:when test="$param = 'profile-max' and $profile-max">
          <xsl:if test="$count > 1">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
          <xsl:text>profile-max=</xsl:text>
          <xsl:value-of select="$profile-max"/>
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
  
  <!-- Hidden Query String -->

  <xsl:template name="hidden.query">    
    <xsl:if test="$text">
      <input type="hidden" name="text" value="{$text}"/>
    </xsl:if>
    <xsl:if test="$text-join">
      <input type="hidden" name="text-join" value="{$text-join}"/>
    </xsl:if>
    <xsl:if test="$text-prox">
      <input type="hidden" name="text-prox" value="{$text-prox}"/>
    </xsl:if>
    <xsl:if test="$text-exclude">
      <input type="hidden" name="text-exclude" value="{$text-exclude}"/>
    </xsl:if>
    <xsl:if test="$text-max">
      <input type="hidden" name="text-max" value="{$text-max}"/>
    </xsl:if>
    <xsl:if test="$title">
      <input type="hidden" name="title" value="{$title}"/>
    </xsl:if>
    <xsl:if test="$title-join">
      <input type="hidden" name="title-join" value="{$title-join}"/>
    </xsl:if>
    <xsl:if test="$title-prox">
      <input type="hidden" name="title-prox" value="{$title-prox}"/>
    </xsl:if>
    <xsl:if test="$title-exclude">
      <input type="hidden" name="title-exclude" value="{$title-exclude}"/>
    </xsl:if>
    <xsl:if test="$title-max">
      <input type="hidden" name="title-max" value="{$title-max}"/>
    </xsl:if>
    <xsl:if test="$creator">
      <input type="hidden" name="creator" value="{$creator}"/>
    </xsl:if>
    <xsl:if test="$creator-join">
      <input type="hidden" name="creator-join" value="{$creator-join}"/>
    </xsl:if>
    <xsl:if test="$creator-prox">
      <input type="hidden" name="creator-prox" value="{$creator-prox}"/>
    </xsl:if>
    <xsl:if test="$creator-exclude">
      <input type="hidden" name="creator-exclude" value="{$creator-exclude}"/>
    </xsl:if>
    <xsl:if test="$creator-max">
      <input type="hidden" name="creator-max" value="{$creator-max}"/>
    </xsl:if>
    <xsl:if test="$subject">
      <input type="hidden" name="subject" value="{$subject}"/>
    </xsl:if>
    <xsl:if test="$subject-join">
      <input type="hidden" name="subject-join" value="{$subject-join}"/>
    </xsl:if>
    <xsl:if test="$subject-prox">
      <input type="hidden" name="subject-prox" value="{$subject-prox}"/>
    </xsl:if>
    <xsl:if test="$subject-exclude">
      <input type="hidden" name="subject-exclude" value="{$subject-exclude}"/>
    </xsl:if>
    <xsl:if test="$subject-max">
      <input type="hidden" name="subject-max" value="{$subject-max}"/>
    </xsl:if>
    <xsl:if test="$description">
      <input type="hidden" name="description" value="{$description}"/>
    </xsl:if>
    <xsl:if test="$description-join">
      <input type="hidden" name="description-join" value="{$description-join}"/>
    </xsl:if>
    <xsl:if test="$description-prox">
      <input type="hidden" name="description-prox" value="{$description-prox}"/>
    </xsl:if>
    <xsl:if test="$description-exclude">
      <input type="hidden" name="description-exclude" value="{$description-exclude}"/>
    </xsl:if>
    <xsl:if test="$description-max">
      <input type="hidden" name="description-max" value="{$description-max}"/>
    </xsl:if>
    <xsl:if test="$publisher">
      <input type="hidden" name="publisher" value="{$publisher}"/>
    </xsl:if>
    <xsl:if test="$publisher-join">
      <input type="hidden" name="publisher-join" value="{$publisher-join}"/>
    </xsl:if>
    <xsl:if test="$publisher-prox">
      <input type="hidden" name="publisher-prox" value="{$publisher-prox}"/>
    </xsl:if>
    <xsl:if test="$publisher-exclude">
      <input type="hidden" name="publisher-exclude" value="{$publisher-exclude}"/>
    </xsl:if>
    <xsl:if test="$publisher-max">
      <input type="hidden" name="publisher-max" value="{$publisher-max}"/>
    </xsl:if>
    <xsl:if test="$contributor">
      <input type="hidden" name="contributor" value="{$contributor}"/>
    </xsl:if>
    <xsl:if test="$contributor-join">
      <input type="hidden" name="contributor-join" value="{$contributor-join}"/>
    </xsl:if>
    <xsl:if test="$contributor-prox">
      <input type="hidden" name="contributor-prox" value="{$contributor-prox}"/>
    </xsl:if>
    <xsl:if test="$contributor-exclude">
      <input type="hidden" name="contributor-exclude" value="{$contributor-exclude}"/>
    </xsl:if>
    <xsl:if test="$contributor-max">
      <input type="hidden" name="contributor-max" value="{$contributor-max}"/>
    </xsl:if>
    <xsl:if test="$date">
      <input type="hidden" name="date" value="{$date}"/>
    </xsl:if>
    <xsl:if test="$date-join">
      <input type="hidden" name="date-join" value="{$date-join}"/>
    </xsl:if>
    <xsl:if test="$date-prox">
      <input type="hidden" name="date-prox" value="{$date-prox}"/>
    </xsl:if>
    <xsl:if test="$date-exclude">
      <input type="hidden" name="date-exclude" value="{$date-exclude}"/>
    </xsl:if>
    <xsl:if test="$date-max">
      <input type="hidden" name="date-max" value="{$date-max}"/>
    </xsl:if>
    <xsl:if test="$type">
      <input type="hidden" name="type" value="{$type}"/>
    </xsl:if>
    <xsl:if test="$type-join">
      <input type="hidden" name="type-join" value="{$type-join}"/>
    </xsl:if>
    <xsl:if test="$type-prox">
      <input type="hidden" name="type-prox" value="{$type-prox}"/>
    </xsl:if>
    <xsl:if test="$type-exclude">
      <input type="hidden" name="type-exclude" value="{$type-exclude}"/>
    </xsl:if>
    <xsl:if test="$type-max">
      <input type="hidden" name="type-max" value="{$type-max}"/>
    </xsl:if>
    <xsl:if test="$format">
      <input type="hidden" name="format" value="{$format}"/>
    </xsl:if>
    <xsl:if test="$format-join">
      <input type="hidden" name="format-join" value="{$format-join}"/>
    </xsl:if>
    <xsl:if test="$format-prox">
      <input type="hidden" name="format-prox" value="{$format-prox}"/>
    </xsl:if>
    <xsl:if test="$format-exclude">
      <input type="hidden" name="format-exclude" value="{$format-exclude}"/>
    </xsl:if>
    <xsl:if test="$format-max">
      <input type="hidden" name="format-max" value="{$format-max}"/>
    </xsl:if>
    <xsl:if test="$identifier">
      <input type="hidden" name="identifier" value="{$identifier}"/>
    </xsl:if>
    <xsl:if test="$identifier-join">
      <input type="hidden" name="identifier-join" value="{$identifier-join}"/>
    </xsl:if>
    <xsl:if test="$identifier-prox">
      <input type="hidden" name="identifier-prox" value="{$identifier-prox}"/>
    </xsl:if>
    <xsl:if test="$identifier-exclude">
      <input type="hidden" name="identifier-exclude" value="{$identifier-exclude}"/>
    </xsl:if>
    <xsl:if test="$identifier-max">
      <input type="hidden" name="identifier-max" value="{$identifier-max}"/>
    </xsl:if>
    <xsl:if test="$source">
      <input type="hidden" name="source" value="{$source}"/>
    </xsl:if>
    <xsl:if test="$source-join">
      <input type="hidden" name="source-join" value="{$source-join}"/>
    </xsl:if>
    <xsl:if test="$source-prox">
      <input type="hidden" name="source-prox" value="{$source-prox}"/>
    </xsl:if>
    <xsl:if test="$source-exclude">
      <input type="hidden" name="source-exclude" value="{$source-exclude}"/>
    </xsl:if>
    <xsl:if test="$source-max">
      <input type="hidden" name="source-max" value="{$source-max}"/>
    </xsl:if>
    <xsl:if test="$language">
      <input type="hidden" name="language" value="{$language}"/>
    </xsl:if>
    <xsl:if test="$language-join">
      <input type="hidden" name="language-join" value="{$language-join}"/>
    </xsl:if>
    <xsl:if test="$language-prox">
      <input type="hidden" name="language-prox" value="{$language-prox}"/>
    </xsl:if>
    <xsl:if test="$language-exclude">
      <input type="hidden" name="language-exclude" value="{$language-exclude}"/>
    </xsl:if>
    <xsl:if test="$language-max">
      <input type="hidden" name="language-max" value="{$language-max}"/>
    </xsl:if>
    <xsl:if test="$relation">
      <input type="hidden" name="relation" value="{$relation}"/>
    </xsl:if>
    <xsl:if test="$relation-join">
      <input type="hidden" name="relation-join" value="{$relation-join}"/>
    </xsl:if>
    <xsl:if test="$relation-prox">
      <input type="hidden" name="relation-prox" value="{$relation-prox}"/>
    </xsl:if>
    <xsl:if test="$relation-exclude">
      <input type="hidden" name="relation-exclude" value="{$relation-exclude}"/>
    </xsl:if>
    <xsl:if test="$relation-max">
      <input type="hidden" name="relation-max" value="{$relation-max}"/>
    </xsl:if>
    <xsl:if test="$coverage">
      <input type="hidden" name="coverage" value="{$coverage}"/>
    </xsl:if>
    <xsl:if test="$coverage-join">
      <input type="hidden" name="coverage-join" value="{$coverage-join}"/>
    </xsl:if>
    <xsl:if test="$coverage-prox">
      <input type="hidden" name="coverage-prox" value="{$coverage-prox}"/>
    </xsl:if>
    <xsl:if test="$coverage-exclude">
      <input type="hidden" name="coverage-exclude" value="{$coverage-exclude}"/>
    </xsl:if>
    <xsl:if test="$coverage-max">
      <input type="hidden" name="coverage-max" value="{$coverage-max}"/>
    </xsl:if>
    <!-- Do we need this? -->
    <!-- xsl:if test="$rights">
    <input type="hidden" name="rights" value="{$rights}"/>
    </xsl:if>
    <xsl:if test="$rights-join">
    <input type="hidden" name="rights-join" value="{$rights-join}"/>
    </xsl:if>
    <xsl:if test="$rights-prox">
    <input type="hidden" name="rights-prox" value="{$rights-prox}"/>
    </xsl:if>
    <xsl:if test="$rights-exclude">
    <input type="hidden" name="rights-exclude" value="{$rights-exclude}"/>
    </xsl:if>
    <xsl:if test="$rights-max">
    <input type="hidden" name="rights-max" value="{$rights-max}"/>
    </xsl:if -->
    <xsl:if test="$year">
      <input type="hidden" name="year" value="{$year}"/>
    </xsl:if>
    <xsl:if test="$year-join">
      <input type="hidden" name="year-join" value="{$year-join}"/>
    </xsl:if>
    <xsl:if test="$year-prox">
      <input type="hidden" name="year-prox" value="{$year-prox}"/>
    </xsl:if>
    <xsl:if test="$year-exclude">
      <input type="hidden" name="year-exclude" value="{$year-exclude}"/>
    </xsl:if>
    <xsl:if test="$year-max">
      <input type="hidden" name="year-max" value="{$year-max}"/>
    </xsl:if>
    <xsl:if test="$profile">
      <input type="hidden" name="profile" value="{$profile}"/>
    </xsl:if>
    <xsl:if test="$profile-join">
      <input type="hidden" name="profile-join" value="{$profile-join}"/>
    </xsl:if>
    <xsl:if test="$profile-prox">
      <input type="hidden" name="profile-prox" value="{$profile-prox}"/>
    </xsl:if>
    <xsl:if test="$profile-exclude">
      <input type="hidden" name="profile-exclude" value="{$profile-exclude}"/>
    </xsl:if>
    <xsl:if test="$profile-max">
      <input type="hidden" name="profile-max" value="{$profile-max}"/>
    </xsl:if>
    <xsl:if test="$sectionType">
      <input type="hidden" name="sectionType" value="{$sectionType}"/>
    </xsl:if>
  </xsl:template>
  
  <!-- Human Readable Form of Query -->
  
  <xsl:param name="query">
    <xsl:copy-of select="replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace($queryString, 
                          '&amp;rmode=([A-Za-z0-9&quot;\-\.\*\+ ]+)', ''), 
                          '&amp;smode=([A-Za-z0-9&quot;\-\.\*\+ ]+)', ''), 
                          '&amp;relation=([A-Za-z0-9&quot;\-\.\*\+ ]+)', ''), 
                          '&amp;profile=([A-Za-z0-9&quot;\-\.\*\+ ]+)', ''), 
                          '&amp;profile-join=([A-Za-z0-9&quot;\-\.\*\+ ]+)', ''),     
                          'year=([0-9]+)&amp;year-max=([0-9]+)', 'year=$1-$2'),    
                          'text=([A-Za-z0-9&quot;\-\.\*\+ ]+)&amp;text-prox=([0-9]+)', '$1 within $2 words'), 
                          'text=([A-Za-z0-9&quot;\-\.\*\+ ]+)', 'keywords=$1'), 
                          '([A-Za-z0-9&quot;\- ]+)=([A-Za-z0-9&quot;\-\.\*\+ ]+)', 'XX $2 in $1 XX'),
                          '&amp;', ' and '),
                          '^ and ', '')"/>
  </xsl:param>
    
  <!-- ====================================================================== -->
  <!-- Format Query for Display                                               -->
  <!-- ====================================================================== -->
  
  <xsl:template name="format-query">
    
    <xsl:param name="query"/>
    
    <xsl:analyze-string select="$query" regex="XX ([A-Za-z0-9&quot;\-\.\*\+ ]+?) in ([A-Za-z0-9\-]+) XX">
      <xsl:matching-substring>
        <span class="search-term"><xsl:value-of select="regex-group(1)"/></span>
        <xsl:text> in </xsl:text>
        <strong><xsl:value-of select="regex-group(2)"/></strong>
      </xsl:matching-substring>
      <xsl:non-matching-substring>
        <xsl:value-of select="."/>
      </xsl:non-matching-substring>
    </xsl:analyze-string>
    
  </xsl:template>

  <!-- How do I chain the one above and this one? -->
  <xsl:template name="format-proximity">
    
    <xsl:param name="query"/>

    <xsl:analyze-string select="$query" regex="([A-Za-z0-9&quot;\-\.\*\+ ]+?) within ([0-9]+) words">
      <xsl:matching-substring>
        <span class="search-term"><xsl:value-of select="regex-group(1)"/></span>
        <xsl:text> within </xsl:text>
        <strong><xsl:value-of select="regex-group(2)"/></strong>
        <xsl:text> words</xsl:text>
      </xsl:matching-substring>
      <xsl:non-matching-substring>
        <xsl:value-of select="."/>
      </xsl:non-matching-substring>
    </xsl:analyze-string>
    
  </xsl:template>
    
  <!-- ====================================================================== -->
  <!-- Result Paging                                                          -->
  <!-- ====================================================================== -->

  <!-- Summarize Results -->
  <xsl:template name="page-summary">
    
    <xsl:variable name="totalDocs" as="xs:integer" select="@totalDocs"/>
    
    <xsl:variable name="lastOnPage" as="xs:integer">
      <xsl:choose>
        <xsl:when test="(($startDoc + $docsPerPage)-1) > $totalDocs">
          <xsl:value-of select="$totalDocs"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="($startDoc + $docsPerPage)-1"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>    
    
    <xsl:text> Displaying </xsl:text>
    <xsl:value-of select="$startDoc"/>
    <xsl:text> - </xsl:text>
    <xsl:value-of select="$lastOnPage"/>
    <xsl:text> of </xsl:text>
    <strong>
      <xsl:value-of select="@totalDocs"/>
    </strong>
    <xsl:text> books</xsl:text>
    <xsl:if test="$totalDocs > $docsPerPage">
      <xsl:text>: </xsl:text>
    </xsl:if>
    
  </xsl:template>

  <!-- Page Linking -->  
  <xsl:template name="pages">
    
    <xsl:variable name="totalDocs" as="xs:integer" select="@totalDocs"/>
    <xsl:variable name="nPages" as="xs:double" select="floor((($totalDocs+$docsPerPage)-1) div $docsPerPage)+1"/>

    <xsl:variable name="showPages" as="xs:integer">
      <xsl:choose>
        <xsl:when test="$nPages >= ($maxPages + 1)">
          <xsl:value-of select="$maxPages"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$nPages - 1"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    
    <xsl:variable name="pageQueryString">
      <xsl:choose>
        <xsl:when test="$sort != ''">
          <xsl:value-of select="concat($queryString, '&amp;sort=', $sort)"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$queryString"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    
    <xsl:if test="$nPages &gt; 2">
      <xsl:text>Page: </xsl:text>
    </xsl:if>

    <xsl:for-each select="(1 to $maxPages)">
      <!-- Figure out which block you need to be in -->
      <xsl:variable name="blockStart" as="xs:integer">
        <xsl:choose>
          <xsl:when test="$startDoc &lt;= ($docsPerPage * $blockSize)">
            <xsl:value-of select="1"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="((floor($startDoc div ($docsPerPage * $blockSize))) * $blockSize) + 1"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <!-- Figure out what page we're on -->
      <xsl:variable name="pageNum" as="xs:integer" select="position()"/>
      <xsl:variable name="pageStart" as="xs:integer" select="(($pageNum - 1) * $docsPerPage) + 1"/>

      <!-- Paging by Blocks -->
      <xsl:variable name="prevBlock" as="xs:integer" select="(($blockStart - $blockSize) * $docsPerPage) - ($docsPerPage - 1)"/>
      <xsl:if test="($pageNum = 1) and ($prevBlock &gt;= 1)">
        <a href="{$servlet.path}?{$pageQueryString}&amp;startDoc={$prevBlock}">&lt;&lt;</a>
        <xsl:text>&#160;&#160;</xsl:text>
      </xsl:if>

      <!-- Individual Paging -->
      <!-- xsl:if test="($pageNum = 1) and ($pageStart != $startDoc)">
        <xsl:variable name="prevPage" as="xs:integer" select="$startDoc - $docsPerPage"/>
        <a href="{$servlet.path}?{$pageQueryString}&amp;startDoc={$prevPage}">&lt;&lt;</a>
        <xsl:text>&#160;&#160;</xsl:text>
      </xsl:if -->
                
      <!-- If there are hits on the page, show it -->
      <xsl:if test="(($pageNum &gt;= $blockStart) and ($pageNum &lt;= ($blockStart + ($blockSize - 1)))) and
                    (($nPages &gt; $pageNum) and ($nPages &gt; 2))">
        <xsl:choose>
          <!-- Make a hyperlink if it's not the page we're currently on. -->
          <xsl:when test="($pageStart != $startDoc)">
            <a href="{$servlet.path}?{$pageQueryString}&amp;startDoc={$pageStart}">
              <xsl:value-of select="$pageNum"/>
            </a>
            <xsl:if test="$pageNum &lt; $showPages">
              <xsl:text>&#160;</xsl:text>
            </xsl:if>
          </xsl:when>
          <xsl:when test="($pageStart = $startDoc)">
            <xsl:value-of select="$pageNum"/>
            <xsl:if test="$pageNum &lt; $showPages">
              <xsl:text>&#160;</xsl:text>
            </xsl:if>
          </xsl:when>
        </xsl:choose>
      </xsl:if>

      <!-- Individual Paging -->      
      <!-- xsl:if test="($pageNum = $showPages) and ($pageStart != $startDoc)">
        <xsl:variable name="nextPage" as="xs:integer" select="$startDoc + $docsPerPage"/>
        <xsl:text>&#160;&#160;</xsl:text>
        <a href="{$servlet.path}?{$pageQueryString}&amp;startDoc={$nextPage}">&gt;&gt;</a>
      </xsl:if -->

      <!-- Paging by Blocks -->   
      <xsl:variable name="nextBlock" as="xs:integer" select="(($blockStart + $blockSize) * $docsPerPage) - ($docsPerPage - 1)"/>
      <xsl:if test="($pageNum = $showPages) and (($showPages * $docsPerPage) &gt; $nextBlock)">
        <xsl:text>&#160;&#160;</xsl:text>
        <a href="{$servlet.path}?{$pageQueryString}&amp;startDoc={$nextBlock}">&gt;&gt;</a>
      </xsl:if>

    </xsl:for-each>

  </xsl:template>

  <!-- ====================================================================== -->
  <!-- Subject Links                                                          -->
  <!-- ====================================================================== -->
      
  <xsl:template match="subject">
    <a href="{$servlet.path}?subject=%22{.}%22&amp;profile={$profile}&amp;profile-join={$profile-join}&amp;rmode={$rmode}">
      <xsl:apply-templates/>
    </a>
    <xsl:if test="not(position() = last())">
      <xsl:text> | </xsl:text>
    </xsl:if>
  </xsl:template>
   
  <!-- ====================================================================== -->
  <!-- "More" Blocks                                                            -->
  <!-- ====================================================================== -->
      
  <xsl:template name="moreBlock">
  
    <xsl:param name="block"/>    
    <xsl:variable name="hideString" select="replace($queryString, '(rmode=[A-Za-z0-9]+)-show', '$1')"/>

    <xsl:choose>
      <xsl:when test="(contains($rmode, 'show')) and (matches(string() , '.{500}'))">
        <xsl:apply-templates select="$block"/>
        <xsl:text>&#160;&#160;&#160;</xsl:text>
        <a href="{$servlet.path}?{$hideString}">[brief]</a>         
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates select="$block" mode="crop"/>        
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
  <!-- Cropped blocks are not going to show search results. Not sure there is a way around this... -->
  <xsl:template match="node()" mode="crop">
    
    <xsl:variable name="moreString" select="replace($queryString, '(rmode=[A-Za-z0-9]+)', '$1-show')"/>
    
    <xsl:choose>
      <xsl:when test="matches(string(.) , '.{300}')">
        <xsl:value-of select="replace(., '(.{300}).+', '$1')"/>
        <xsl:text> . . . </xsl:text>
        <a href="{$servlet.path}?{$moreString}">[more]</a>                
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
    
  <!-- ====================================================================== -->
  <!-- Sort Options                                                           -->
  <!-- ====================================================================== -->
   
  <xsl:template name="sort.options">
    <select size="1" name="sort">
      <xsl:choose>
        <xsl:when test="$sort = ''">
          <option value="" selected="selected">relevance</option>
          <option value="title">title</option>
          <option value="creator">author</option>
          <option value="year">publication date</option>
        </xsl:when>
        <xsl:when test="$sort = 'title'">
          <option value="">relevance</option>
          <option value="title" selected="selected">title</option>
          <option value="creator">author</option>
          <option value="year">publication date</option>
        </xsl:when>
        <xsl:when test="$sort = 'creator'">
          <option value="">relevance</option>
          <option value="title">title</option>
          <option value="creator" selected="selected">author</option>
          <option value="year">publication date</option>
        </xsl:when>
        <xsl:when test="$sort = 'year'">
          <option value="">relevance</option>
          <option value="title">title</option>
          <option value="creator">author</option>
          <option value="year" selected="selected">publication date</option>
        </xsl:when>
      </xsl:choose>
    </select>
  </xsl:template>
    
  <!-- ====================================================================== -->
  <!-- Access Options                                                         -->
  <!-- ====================================================================== -->
   
  <xsl:template name="access.options">
 
    <select size="1" name="rights">
      <xsl:choose>
        <xsl:when test="$rights = ''">
          <option value="" selected="selected">all books</option>
          <option value="public">public access books</option> 
        </xsl:when>
        <xsl:when test="$rights = 'public'">
          <option value="">all books</option>
          <option value="public" selected="selected">public access books</option> 
        </xsl:when>
      </xsl:choose>
    </select>
    
  </xsl:template>
    
  <!-- ====================================================================== -->
  <!-- dynaXML URL Template                                                   -->
  <!-- ====================================================================== -->
  
  <xsl:template name="dynaxml.url">
    
    <xsl:param name="fullark"/>
    <xsl:variable name="ark" select="substring($fullark, string-length($fullark)-9)"/>
    <xsl:variable name="subDir" select="substring($ark, 9, 2)"/>
    
    <xsl:value-of select="concat($dynaxml.path, '?docId=', $ark, '&amp;query=', replace($text, '&amp;', '%26'))"/>
    <!-- -join & -prox are mutually exclusive -->
    <xsl:choose>
      <xsl:when test="$text-prox">
        <xsl:value-of select="concat('&amp;query-prox=', $text-prox)"/>
      </xsl:when>
      <xsl:when test="$text-join">
        <xsl:value-of select="concat('&amp;query-join=', $text-join)"/>
      </xsl:when>            
    </xsl:choose>
    <xsl:if test="$text-exclude">
      <xsl:value-of select="concat('&amp;query-exclude=', $text-exclude)"/>
    </xsl:if>
    <xsl:if test="$sectionType">
      <xsl:value-of select="concat('&amp;sectionType=', $sectionType)"/>
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
