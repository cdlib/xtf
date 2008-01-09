<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:session="java:org.cdlib.xtf.xslt.Session"
   extension-element-prefixes="session"
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
   
   <xsl:param name="fieldList" select="'text title creator subject description publisher contributor date type format identifier source language relation coverage rights year '"/>
   <xsl:param name="facet-date"/>
   
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
         
         <!-- flat subject facet -->
         <facet field="facet-subject">
            <xsl:attribute name="sortGroupsBy">
               <xsl:choose>
                  <xsl:when test="$expand='subject'">
                     <xsl:value-of select="'value'"/>
                  </xsl:when>
                  <xsl:otherwise>
                     <xsl:value-of select="'totalDocs'"/>
                  </xsl:otherwise>
               </xsl:choose>
            </xsl:attribute>
            <xsl:attribute name="select">
               <xsl:choose>
                  <xsl:when test="$expand='subject'">
                     <xsl:value-of select="'*'"/>
                  </xsl:when>
                  <xsl:otherwise>
                     <xsl:value-of select="'*[1-10]'"/>
                  </xsl:otherwise>
               </xsl:choose>
            </xsl:attribute>
         </facet>
         
         <!-- hierarchical date facet -->
         <facet field="facet-date" sortGroupsBy="value">
            <xsl:attribute name="select">
               <xsl:choose>
                  <xsl:when test="matches($facet-date,'[0-9]+::[0-9]+::[0-9]+')">
                     <xsl:attribute name="select" select="$facet-date"/>
                  </xsl:when>
                  <xsl:when test="$facet-date">
                     <xsl:attribute name="select" select="concat($facet-date,'::*')"/>
                  </xsl:when>
                  <xsl:otherwise>
                     <xsl:attribute name="select" select="'*'"/>       
                  </xsl:otherwise>
               </xsl:choose>
            </xsl:attribute>
         </facet>
         
         <!-- to support title browse pages -->
         <xsl:if test="//param[matches(@name,'browse-title')]">
            <xsl:variable name="browse-title" select="//param[matches(@name,'browse-title')]/@value"/> 
            <facet field="browse-title" sortGroupsBy="value">
               <xsl:attribute name="select">
                  <xsl:value-of select="concat('*|',$browse-title,'#all')"/>
               </xsl:attribute>
            </facet>
         </xsl:if>
         
         <!-- to support author browse pages -->
         <xsl:if test="//param[matches(@name,'browse-creator')]">
            <xsl:variable name="browse-creator" select="//param[matches(@name,'browse-creator')]/@value"/> 
            <facet field="browse-creator" sortGroupsBy="value">
               <xsl:attribute name="select">
                  <xsl:value-of select="concat('*|',$browse-creator,'#all')"/>
               </xsl:attribute>
            </facet>
         </xsl:if>
         
         <!-- process query -->
         <xsl:choose>
            <xsl:when test="matches($http.User-Agent,$robots)">
               <xsl:call-template name="robot"/>
            </xsl:when>
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
      
      <!-- Scan for non-empty parameters (but skip "-exclude", "-join", "-prox", "-max", and "-ignore") -->
      <xsl:variable name="queryParams" select="param[count(*) &gt; 0 and not(matches(@name, '.*-exclude')) 
         and not(matches(@name, '.*-join')) 
         and not(matches(@name, '.*-prox')) 
         and not(matches(@name, '.*-max')) 
         and not(matches(@name, '.*-ignore'))]"/>
      
      <!-- Find the full-text query, if any -->
      <xsl:variable name="textParam" select="$queryParams[matches(@name, 'text|query')]"/>
      
      <!-- Find the meta-data queries, if any -->
      <xsl:variable name="metaParams"
         select="$queryParams[not(matches(@name,'text*|query*|style|smode|rmode|expand|brand|sort|startDoc|docsPerPage|sectionType|fieldList|normalizeScores|explainScores|f[0-9]+-.+|facet-.+|browse-*|.*-ignore'))]"/>
      
      <and>
         <!-- Process the meta-data queries, if any -->
         <xsl:if test="count($metaParams) &gt; 0">
            <xsl:apply-templates select="$metaParams"/>
         </xsl:if>       
         <!-- Process the text query, if any -->
         <xsl:if test="count($textParam) &gt; 0">
            <xsl:apply-templates select="$textParam"/>
         </xsl:if>
         <!-- Process special facet query params -->
         <!-- flat facets -->
         <xsl:for-each select="//param[matches(@name,'f[0-9]+-.+')]">
            <xsl:variable name="field" select="replace(@name,'f[0-9]+-','facet-')"/>
            <and field="{$field}">
               <term maxSnippets="0">
                  <xsl:value-of select="@value"/>
               </term>
            </and>
         </xsl:for-each>
         <!-- hierarchical facets -->
         <xsl:for-each select="//param[matches(@name,'facet-.+')]">
            <xsl:variable name="field" select="@name"/>
            <and field="{$field}">
               <term maxSnippets="0">
                  <xsl:value-of select="concat(@value,'*')"/>
               </term>
            </and>
         </xsl:for-each>
         <!-- Unary Not -->
         <xsl:for-each select="param[contains(@name, '-exclude')]">
            <xsl:variable name="field" select="replace(@name, '-exclude', '')"/>
            <xsl:if test="not(//param[@name=$field])">
               <not field="{$field}">
                  <xsl:apply-templates/>
               </not>
            </xsl:if>
         </xsl:for-each>  
         <!-- If there are no meta, text queries, or unary nots, output a dummy -->
         <xsl:if test="count($metaParams) = 0 and count($textParam) = 0 and not(param[matches(@name, '.*-exclude')])">
            <and field="all"><term>NADA</term></and>
         </xsl:if>
      </and>
      
   </xsl:template>
   
</xsl:stylesheet>
