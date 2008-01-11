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
   
   <!-- list of fields to search in 'keyword' search -->
   <xsl:param name="fieldList" select="'text title creator subject description publisher contributor '"/>
   
   <!-- special hierarchical facet -->
   <xsl:param name="f1-date"/>
   
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
         
         <!-- flat subject facet, normally shows first 10 but user can 'expand' -->
         <facet field="facet-subject"
            select="{if ($expand='subject') then '*' else '*[1-10]'}"
            sortGroupsBy="{if ($expand='subject') then 'value' else 'totalDocs'}"/>
         
         <!-- hierarchical date facet, allows user to drill down -->
         <facet field="facet-date" sortGroupsBy="value">
            <xsl:attribute name="select">
               <xsl:text>*</xsl:text> <!-- always select top level -->
               <xsl:if test="//param[matches(@name, 'f[0-9]+-date')]">
                  <xsl:for-each select="//param[matches(@name, 'f[0-9]+-date')]">
                     <xsl:value-of select="concat('|', @value, '|', @value, '::*')"/>
                  </xsl:for-each>
               </xsl:if>
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
      
      <!-- Find the meta-data and full-text queries, if any -->
      <xsl:variable name="queryParams"
         select="param[not(matches(@name,'style|smode|rmode|expand|brand|sort|startDoc|docsPerPage|sectionType|fieldList|normalizeScores|explainScores|f[0-9]+-.+|facet-.+|browse-*|.*-exclude|.*-join|.*-prox|.*-ignore'))]"/>
      
      <and>
         <!-- Process the meta-data and text queries, if any -->
         <xsl:apply-templates select="$queryParams"/>

         <!-- Process special facet query params -->
         <xsl:if test="//param[matches(@name,'f[0-9]+-.+')]">
            <and maxSnippets="0">
               <xsl:for-each select="//param[matches(@name,'f[0-9]+-.+')]">
                  <term field="{replace(@name,'f[0-9]+-','facet-')}">
                     <xsl:value-of select="@value"/>
                  </term>
               </xsl:for-each>
            </and>
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
   
</xsl:stylesheet>
