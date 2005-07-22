<!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
<!-- Query result formatter stylesheet                                      -->
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

<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
                              xmlns:xs="http://www.w3.org/2001/XMLSchema">
  
  <!-- ====================================================================== -->
  <!-- Output Parameters                                                      -->
  <!-- ====================================================================== -->

  <xsl:output method="xhtml" indent="yes" encoding="UTF-8" media-type="text/html" 
              doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN" 
              doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd" 
              omit-xml-declaration="yes"/>
  
  <xsl:output name="xml" method="xml" indent="yes" media-type="text/xml" encoding="UTF-8"/>

  <!-- ====================================================================== -->
  <!-- Parameters                                                             -->
  <!-- ====================================================================== -->

  <!-- Keyword Search (text and metadata) -->
  <xsl:param name="keyword"/>
  <xsl:param name="keyword-join"/>
  <xsl:param name="keyword-prox"/>
  <xsl:param name="keyword-exclude"/>
  <xsl:param name="fieldList"/>
  
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
  <xsl:param name="style"/>
  <xsl:param name="smode" select="'simple'"/>
  <xsl:param name="rmode" select="'none'"/>
  <xsl:param name="brand" select="'default'"/>
  <xsl:param name="sort"/>
  
  <!-- XML Output Parameter -->
  <xsl:param name="raw"/>
  
  <!-- Retrieve Branding Nodes -->
  <xsl:variable name="brand.file">
    <xsl:choose>
      <xsl:when test="$brand != ''">
        <xsl:copy-of select="document(concat('../../../../brand/',$brand,'.xml'))"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy-of select="document('../../../../brand/default.xml')"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  
  <xsl:param name="brand.links" select="$brand.file//links/*"/>
  <xsl:param name="brand.header" select="$brand.file//header/*"/>
  <xsl:param name="brand.footer" select="$brand.file//footer/*"/>

  <!-- Paging Parameters-->  
  <xsl:param name="startDoc" as="xs:integer" select="1"/>
  <!-- Documents per Page -->
  <xsl:param name="docsPerPage" as="xs:integer">
    <xsl:choose>
      <xsl:when test="$smode = 'test' or $raw">
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
  <xsl:param name="root.path"/>
  <xsl:param name="xtfURL" select="$root.path"/>
  <xsl:param name="serverURL" select="replace($xtfURL, '(http://.+)[:/].+', '$1/')"/>
  <xsl:param name="crossqueryPath" select="if (matches($servlet.path, 'org.cdlib.xtf.dynaXML.DynaXML')) then 'org.cdlib.xtf.crossQuery.CrossQuery' else 'search'"/>
  <xsl:param name="dynaxmlPath" select="if (matches($servlet.path, 'org.cdlib.xtf.crossQuery.CrossQuery')) then 'org.cdlib.xtf.dynaXML.DynaXML' else 'view'"/>

  <!-- Query String -->
  <!-- grab url -->
  <xsl:param name="http.URL"/>
  <!-- extract query string and clean it up -->
  <xsl:param name="queryString">
    <xsl:value-of select="replace(replace(replace(replace($http.URL, '.+\?', ''), 
      '[0-9A-Za-z\-]+=&amp;', '&amp;'), 
      '&amp;[0-9A-Za-z\-]+=$', '&amp;'), 
      '&amp;+', '&amp;')"/>
  </xsl:param>
  
  <!-- Hidden Query String -->

  <xsl:template name="hidden.query">   
    <xsl:if test="$keyword">
      <input type="hidden" name="keyword" value="{$keyword}"/>
    </xsl:if>
    <xsl:if test="$keyword-join">
      <input type="hidden" name="keyword-join" value="{$keyword-join}"/>
    </xsl:if>
    <xsl:if test="$keyword-prox">
      <input type="hidden" name="keyword-prox" value="{$keyword-prox}"/>
    </xsl:if>
    <xsl:if test="$keyword-exclude">
      <input type="hidden" name="keyword-exclude" value="{$keyword-exclude}"/>
    </xsl:if>
    <xsl:if test="$fieldList">
      <input type="hidden" name="fieldList" value="{$fieldList}"/>
    </xsl:if>    
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
    <xsl:copy-of select="replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace($queryString,                           
                          '&amp;style=([A-Za-z0-9&quot;\-\.\*\+ ]+)', ''),
                          '&amp;smode=([A-Za-z0-9&quot;\-\.\*\+ ]+)', ''),
                          '&amp;rmode=([A-Za-z0-9&quot;\-\.\*\+ ]+)', ''),
                          '&amp;brand=([A-Za-z0-9&quot;\-\.\*\+ ]+)', ''),
                          '&amp;relation=([A-Za-z0-9&quot;\-\.\*\+ ]+)', ''), 
                          '&amp;profile=([A-Za-z0-9&quot;\-\.\*\+ ]+)', ''), 
                          '&amp;profile-join=([A-Za-z0-9&quot;\-\.\*\+ ]+)', ''), 
                          '&amp;fieldList=([A-Za-z0-9&quot;\-\.\*\+ ]+)', ''),      
                          'year=([0-9]+)&amp;year-max=([0-9]+)', 'year=$1-$2'),    
                          'text=([A-Za-z0-9&quot;\-\.\*\+ ]+)&amp;text-prox=([0-9]+)', '$1 within $2 words'), 
                          'keyword=([A-Za-z0-9&quot;\-\.\*\+ ]+)', 'keywords=$1'), 
                          'text=([A-Za-z0-9&quot;\-\.\*\+ ]+)', 'keywords=$1'), 
                          'creator=([A-Za-z0-9&quot;\-\.\*\+ ]+)', 'author=$1'), 
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
        <span class="search-type"><xsl:value-of select="regex-group(2)"/></span>
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
        <span class="search-type"><xsl:value-of select="regex-group(2)"/></span>
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
    
    <xsl:param name="object-type"/>
    
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
    <xsl:text> </xsl:text>
    <xsl:value-of select="$object-type"/>
    
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
        <a href="{$xtfURL}{$crossqueryPath}?{$pageQueryString}&amp;startDoc={$prevBlock}">&lt;&lt;</a>
        <xsl:text>&#160;&#160;</xsl:text>
      </xsl:if>

      <!-- Individual Paging -->
      <!-- xsl:if test="($pageNum = 1) and ($pageStart != $startDoc)">
        <xsl:variable name="prevPage" as="xs:integer" select="$startDoc - $docsPerPage"/>
        <a href="{$xtfURL}{$crossqueryPath}?{$pageQueryString}&amp;startDoc={$prevPage}">&lt;&lt;</a>
        <xsl:text>&#160;&#160;</xsl:text>
      </xsl:if -->
                
      <!-- If there are hits on the page, show it -->
      <xsl:if test="(($pageNum &gt;= $blockStart) and ($pageNum &lt;= ($blockStart + ($blockSize - 1)))) and
                    (($nPages &gt; $pageNum) and ($nPages &gt; 2))">
        <xsl:choose>
          <!-- Make a hyperlink if it's not the page we're currently on. -->
          <xsl:when test="($pageStart != $startDoc)">
            <a href="{$xtfURL}{$crossqueryPath}?{$pageQueryString}&amp;startDoc={$pageStart}">
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
        <a href="{$xtfURL}{$crossqueryPath}?{$pageQueryString}&amp;startDoc={$nextPage}">&gt;&gt;</a>
      </xsl:if -->

      <!-- Paging by Blocks -->   
      <xsl:variable name="nextBlock" as="xs:integer" select="(($blockStart + $blockSize) * $docsPerPage) - ($docsPerPage - 1)"/>
      <xsl:if test="($pageNum = $showPages) and (($showPages * $docsPerPage) &gt; $nextBlock)">
        <xsl:text>&#160;&#160;</xsl:text>
        <a href="{$xtfURL}{$crossqueryPath}?{$pageQueryString}&amp;startDoc={$nextBlock}">&gt;&gt;</a>
      </xsl:if>

    </xsl:for-each>

  </xsl:template>

  <!-- ====================================================================== -->
  <!-- Subject Links                                                          -->
  <!-- ====================================================================== -->
      
  <xsl:template match="subject">
    <a href="{$xtfURL}{$crossqueryPath}?subject=%22{.}%22&amp;profile={$profile}&amp;profile-join={$profile-join}&amp;style={$style}&amp;smode={$smode}&amp;rmode={$rmode}&amp;brand={$brand}">
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
    <xsl:param name="identifier"/>
    <xsl:variable name="hideString" select="replace($queryString, 'rmode=[A-Za-z0-9]*', 'rmode=hideDescrip')"/>

    <xsl:choose>
      <xsl:when test="(contains($rmode, 'showDescrip')) and (matches(string() , '.{500}'))">
        <xsl:apply-templates select="$block"/>
        <xsl:text>&#160;&#160;&#160;</xsl:text>
        <a href="{$xtfURL}{$crossqueryPath}?{$hideString}&amp;startDoc={$startDoc}#{$identifier}">[brief]</a>         
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates select="$block" mode="crop">
          <xsl:with-param name="identifier" select="$identifier"/>
        </xsl:apply-templates>        
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
  <!-- Cropped blocks are not going to show search results. Not sure there is a way around this... -->
  <xsl:template match="node()" mode="crop">
    
    <xsl:param name="identifier"/>   
    <xsl:variable name="moreString" select="replace($queryString, 'rmode=[A-Za-z0-9]*', 'rmode=showDescrip')"/>
    
    <xsl:choose>
      <xsl:when test="matches(string(.) , '.{300}')">
        <xsl:value-of select="replace(., '(.{300}).+', '$1')"/>
        <xsl:text> . . . </xsl:text>
        <a href="{$xtfURL}{$crossqueryPath}?{$moreString}&amp;startDoc={$startDoc}#{$identifier}">[more]</a>  
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
          <option value="Public">public access books</option> 
        </xsl:when>
        <xsl:when test="$rights = 'Public'">
          <option value="">all books</option>
          <option value="Public" selected="selected">public access books</option> 
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
    <xsl:variable name="query">
      <xsl:choose>
        <xsl:when test="$keyword != ''">
          <xsl:value-of select="$keyword"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$text"/>
        </xsl:otherwise>
      </xsl:choose>     
    </xsl:variable>
    
    <xsl:value-of select="concat($dynaxmlPath, '?docId=', $ark, '&amp;query=', replace($query, '&amp;', '%26'))"/>
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
    <!-- -join & -prox are mutually exclusive -->
    <xsl:choose>
      <xsl:when test="$keyword-prox">
        <xsl:value-of select="concat('&amp;query-prox=', $keyword-prox)"/>
      </xsl:when>
      <xsl:when test="$keyword-join">
        <xsl:value-of select="concat('&amp;query-join=', $keyword-join)"/>
      </xsl:when>            
    </xsl:choose>
    <xsl:if test="$keyword-exclude">
      <xsl:value-of select="concat('&amp;query-exclude=', $keyword-exclude)"/>
    </xsl:if>
    <xsl:if test="$sectionType">
      <xsl:value-of select="concat('&amp;sectionType=', $sectionType)"/>
    </xsl:if>
    <xsl:if test="$brand">
      <xsl:value-of select="concat('&amp;brand=',$brand)"/>
    </xsl:if>
    
    <!-- Do I still need this? -->
    <!--<xsl:choose>
      <xsl:when test="ancestor::docHit/meta/relation[contains(.,'ucpress')]">
        <xsl:value-of select="'&amp;brand=ucpress'"/>
      </xsl:when>
      <xsl:when test="ancestor::docHit/meta/relation[contains(.,'escholarship')]">
        <xsl:value-of select="'&amp;brand=eschol'"/>
      </xsl:when>
    </xsl:choose>-->
    
  </xsl:template>
    
  <!-- ====================================================================== -->
  <!-- Select Builder                                                         -->
  <!-- ====================================================================== -->
  
  <xsl:template name="selectBuilder">
    <xsl:param name="selectType"/>
    <xsl:param name="optionList"/>
    <xsl:param name="count"/>
    
    <xsl:variable name="option" select="substring-before($optionList, '::')"/>
    
    <xsl:choose>
      <xsl:when test="$selectType='subject'">    
        <xsl:if test="$option != ''"> 
          <option>
            <xsl:attribute name="value">"<xsl:value-of select="$option"/>"</xsl:attribute>
            <xsl:if test="contains($subject,$option)">
              <xsl:attribute name="selected" select="'yes'"/>
            </xsl:if>
            <xsl:value-of select="$option"/>
          </option>    
          <xsl:call-template name="selectBuilder">
            <xsl:with-param name="selectType" select="$selectType"/>
            <xsl:with-param name="optionList" select="replace(substring-after($optionList, $option), '^::', '')"/>
            <xsl:with-param name="count" select="$count + 1"/>
          </xsl:call-template>
        </xsl:if>
      </xsl:when>
      <xsl:when test="$selectType='keyword-prox'">    
        <xsl:if test="$option != ''"> 
          <option>
            <xsl:attribute name="value"><xsl:value-of select="$option"/></xsl:attribute>
            <xsl:if test="$keyword-prox = $option">
              <xsl:attribute name="selected" select="'yes'"/>
            </xsl:if>
            <xsl:value-of select="$option"/>
          </option>    
          <xsl:call-template name="selectBuilder">
            <xsl:with-param name="selectType" select="$selectType"/>
            <xsl:with-param name="optionList" select="replace(substring-after($optionList, $option), '^::', '')"/>
            <xsl:with-param name="count" select="$count + 1"/>
          </xsl:call-template>
        </xsl:if>
      </xsl:when>     
      <xsl:when test="$selectType='text-prox'">    
        <xsl:if test="$option != ''"> 
          <option>
            <xsl:attribute name="value"><xsl:value-of select="$option"/></xsl:attribute>
            <xsl:if test="$text-prox = $option">
              <xsl:attribute name="selected" select="'yes'"/>
            </xsl:if>
            <xsl:value-of select="$option"/>
          </option>    
          <xsl:call-template name="selectBuilder">
            <xsl:with-param name="selectType" select="$selectType"/>
            <xsl:with-param name="optionList" select="replace(substring-after($optionList, $option), '^::', '')"/>
            <xsl:with-param name="count" select="$count + 1"/>
          </xsl:call-template>
        </xsl:if>
      </xsl:when>      
      <xsl:when test="$selectType='year'">    
        <xsl:if test="$option != ''"> 
          <option>
            <xsl:attribute name="value"><xsl:value-of select="$option"/></xsl:attribute>
            <xsl:if test="$year = $option">
              <xsl:attribute name="selected" select="'yes'"/>
            </xsl:if>
            <xsl:value-of select="$option"/>
          </option>    
          <xsl:call-template name="selectBuilder">
            <xsl:with-param name="selectType" select="$selectType"/>
            <xsl:with-param name="optionList" select="replace(substring-after($optionList, $option), '^::', '')"/>
            <xsl:with-param name="count" select="$count + 1"/>
          </xsl:call-template>
        </xsl:if>
      </xsl:when> 
      <xsl:when test="$selectType='year-max'">    
        <xsl:if test="$option != ''"> 
          <option>
            <xsl:attribute name="value"><xsl:value-of select="$option"/></xsl:attribute>
            <xsl:if test="$year-max = $option">
              <xsl:attribute name="selected" select="'yes'"/>
            </xsl:if>
            <xsl:value-of select="$option"/>
          </option>    
          <xsl:call-template name="selectBuilder">
            <xsl:with-param name="selectType" select="$selectType"/>
            <xsl:with-param name="optionList" select="replace(substring-after($optionList, $option), '^::', '')"/>
            <xsl:with-param name="count" select="$count + 1"/>
          </xsl:call-template>
        </xsl:if>
      </xsl:when>
    </xsl:choose>    
    
  </xsl:template>
  
  <!-- ====================================================================== -->
  <!-- Generate ARK List for Testing                                          -->
  <!-- ====================================================================== -->

  <!-- Leave indenting as is in the following template! -->

  <xsl:template match="crossQueryResult" mode="test">
    <xsl:result-document format="xml" exclude-result-prefixes="#all">
      <search count="{@totalDocs}" queryString="{$queryString}">
        <xsl:for-each select="docHit">
          <xsl:sort select="number(@rank)" />
          <hit ark="{replace(@path, '.+/([A-Za-z0-9]+)/.+', '$1')}"
            rank="{@rank}"
            score="{@score}"
            totalHits="{@totalHits}"/>
        </xsl:for-each>
      </search>
    </xsl:result-document>
  </xsl:template>

</xsl:stylesheet>
