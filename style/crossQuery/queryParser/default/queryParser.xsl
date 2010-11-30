<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:session="java:org.cdlib.xtf.xslt.Session"
   xmlns:freeformQuery="java:org.cdlib.xtf.xslt.FreeformQuery"
   extension-element-prefixes="session freeformQuery"
   exclude-result-prefixes="#all" 
   version="2.0">
   
   <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
   <!-- Simple query parser stylesheet                                         -->
   <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
   
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
   
   <!--
      This stylesheet implements a simple query parser which does not handle any
      complex queries (boolean and/or/not, ranges, nested queries, etc.)
   -->
   
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
   <!-- Local Parameters                                                       -->
   <!-- ====================================================================== -->
   
   <!-- list of fields to search in 'keyword' search; generally these should
        be the same fields shown in the search result listing, so the user
        can see all the matching words. -->
   <xsl:param name="fieldList" select="'text title creator subject '"/>
   
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
                  <xsl:when test="$sort='reverse-year'">
                     <xsl:value-of select="'-sort-year,sort-title,sort-creator,sort-publisher'"/>
                  </xsl:when>              
                  <xsl:when test="$sort='creator'">
                     <xsl:value-of select="'sort-creator,sort-year,sort-title'"/>
                  </xsl:when>
                  <xsl:when test="$sort='publisher'">
                     <xsl:value-of select="'sort-publisher,sort-title,sort-year'"/>
                  </xsl:when>     
                  <xsl:when test="$sort='rss'">
                     <xsl:value-of select="'-sort-date,sort-title'"/>
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
         
         <!-- subject facet, normally shows top 10 sorted by count, but user can select 'more' 
              to see all sorted by subject. 
         -->
         <xsl:call-template name="facet">
            <xsl:with-param name="field" select="'facet-subject'"/>
            <xsl:with-param name="topGroups" select="'*[1-5]'"/>
            <xsl:with-param name="sort" select="'totalDocs'"/>
         </xsl:call-template>
         
         <!-- hierarchical date facet, shows most recent years first -->
         <xsl:call-template name="facet">
            <xsl:with-param name="field" select="'facet-date'"/>
            <xsl:with-param name="topGroups" select="'*'"/>
            <xsl:with-param name="sort" select="'reverseValue'"/>
         </xsl:call-template>
         
         <!-- to support title browse pages -->
         <xsl:if test="//param[@name='browse-title']">
            <xsl:variable name="page" select="//param[@name='browse-title']/@value"/>
            <xsl:variable name="pageSel" select="if ($page = 'first') then '*[1]' else $page"/>
            <facet field="browse-title" sortGroupsBy="value" sortDocsBy="sort-title,sort-creator,sort-publisher,sort-year" select="{concat('*|',$pageSel,'#all')}"/>
         </xsl:if>
         
         <!-- to support author browse pages -->
         <xsl:if test="//param[matches(@name,'browse-creator')]">
            <xsl:variable name="page" select="//param[matches(@name,'browse-creator')]/@value"/> 
            <xsl:variable name="pageSel" select="if ($page = 'first') then '*[1]' else $page"/>
            <facet field="browse-creator" sortGroupsBy="value" sortDocsBy="sort-creator,sort-title,sort-publisher,sort-year" select="{concat('*|',$pageSel,'#all')}"/>
         </xsl:if>
         
         <!-- process query -->
         <xsl:choose>
            <xsl:when test="matches($http.user-agent,$robots)">
               <xsl:call-template name="robot"/>
            </xsl:when>
            <xsl:when test="$smode = 'addToBag'">
               <xsl:call-template name="addToBag"/>
            </xsl:when>
            <xsl:when test="$smode = 'removeFromBag'">
               <xsl:call-template name="removeFromBag"/>
            </xsl:when>
            <xsl:when test="matches($smode,'showBag|emailFolder')">
               <xsl:call-template name="showBag"/>
            </xsl:when>
            <xsl:when test="$smode = 'moreLike'">
               <xsl:call-template name="moreLike"/>
            </xsl:when>
            <xsl:otherwise>
               <spellcheck/>
               <xsl:apply-templates/>
            </xsl:otherwise>
         </xsl:choose>
         
      </query>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Parameters Template                                                    -->
   <!-- ====================================================================== -->
   
   <xsl:template match="parameters">
      
      <!-- Find the meta-data and full-text queries, if any -->
      <xsl:variable name="queryParams"
         select="param[not(matches(@name,'style|smode|rmode|expand|brand|sort|startDoc|docsPerPage|sectionType|fieldList|normalizeScores|explainScores|f[0-9]+-.+|facet-.+|browse-*|email|.*-exclude|.*-join|.*-prox|.*-max|.*-ignore|freeformQuery'))]"/>
      
      <and>
         <!-- Process the meta-data and text queries, if any -->
         <xsl:apply-templates select="$queryParams"/>

         <!-- Process special facet query params -->
         <xsl:if test="//param[matches(@name,'f[0-9]+-.+')]">
            <and maxSnippets="0">
               <xsl:for-each select="//param[matches(@name,'f[0-9]+-.+')]">
                  <and field="{replace(@name,'f[0-9]+-','facet-')}">
                     <term><xsl:value-of select="@value"/></term>
                  </and>
               </xsl:for-each>
            </and>
         </xsl:if>
         
         <!-- Freeform query language -->
         <xsl:if test="//param[matches(@name, '^freeformQuery$')]">
            <xsl:variable name="strQuery" select="//param[matches(@name, '^freeformQuery$')]/@value"/>
            <xsl:variable name="parsed" select="freeformQuery:parse($strQuery)"/>
            <xsl:apply-templates select="$parsed/query/*" mode="freeform"/>
         </xsl:if>
        
         <!-- Unary Not -->
         <xsl:for-each select="param[contains(@name, '-exclude')]">
            <xsl:variable name="field" select="replace(@name, '-exclude', '')"/>
            <xsl:if test="not(//param[@name=$field])">
               <not field="{$field}">
                  <xsl:apply-templates/>
               </not>
            </xsl:if>
         </xsl:for-each>
      
         <!-- to enable you to see browse results -->
         <xsl:if test="param[matches(@name, 'browse-')]">
            <allDocs/>
         </xsl:if>

      </and>
      
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Facet Query Template                                                   -->
   <!-- ====================================================================== -->
   
   <xsl:template name="facet">
      <xsl:param name="field"/>
      <xsl:param name="topGroups"/>
      <xsl:param name="sort"/>
      
      <xsl:variable name="plainName" select="replace($field,'^facet-','')"/>
      
      <!-- Select facet values based on previously clicked ones. Include the
           ancestors and direct children of these (handles hierarchical facets).
      --> 
      <xsl:variable name="selection">
         <!-- First, select the top groups, or all at the top in expand mode -->
         <xsl:value-of select="if ($expand = $plainName) then '*' else $topGroups"/>
         <!-- For each chosen facet value -->
         <xsl:for-each select="//param[matches(@name, concat('f[0-9]+-',$plainName))]">
            <!-- Quote parts of the value that have special meaning in facet language -->
            <xsl:variable name="escapedValue">
               <xsl:variable name="pieces">
                  <xsl:for-each select="tokenize(@value, '::')">
                     <piece str="{if (matches(., '[#:|*()\\=\[\]&quot;&lt;&gt;&amp;]')) 
                                  then concat(
                                         '&quot;', 
                                         replace(string(.), '&quot;', '\\&quot;'), 
                                        '&quot;')
                                  else string(.)}"/>
                  </xsl:for-each>
               </xsl:variable>
               <xsl:value-of select="string-join($pieces/piece/@str, '::')"/>
            </xsl:variable>
            <!-- Select the value itself -->
            <xsl:value-of select="concat('|', $escapedValue)"/>
            <!-- And select its immediate children -->
            <xsl:value-of select="concat('|', $escapedValue, '::*')"/>
            <!-- And select its siblings, if any -->
            <xsl:value-of select="concat('|', $escapedValue, '[siblings]')"/>
            <!-- If only one child, expand it (and its single child, etc.) -->
            <xsl:value-of select="concat('|', $escapedValue, '::**[singleton]::*')"/>
         </xsl:for-each>
      </xsl:variable>
      
      <!-- generate the facet query -->
      <!-- in expand mode, don't sort by totalDocs -->
      <facet field="{$field}" 
             select="{$selection}"
             sortGroupsBy="{ if ($expand = $plainName) 
                             then replace($sort, 'totalDocs', 'value') 
                             else $sort }">
      </facet>
   </xsl:template>
   
</xsl:stylesheet>
