<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:session="java:org.cdlib.xtf.xslt.Session"
   extension-element-prefixes="session"
   exclude-result-prefixes="#all" 
   version="2.0">
   
   <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
   <!-- Common templates for query parser stylesheets                          -->
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
      Parameters and templates common to all query parsers
   -->
   
   <!-- ====================================================================== -->
   <!-- Global parameters (specified in the URL)                               -->
   <!-- ====================================================================== -->
   
   <!-- style param -->
   <xsl:param name="style"/>
   
   <!-- search mode -->
   <xsl:param name="smode"/>
   
   <!-- facet expand mode -->
   <xsl:param name="expand"/>  
   
   <!-- result mode -->
   <xsl:param name="rmode"/>  
   
   <!-- brand mode -->
   <xsl:param name="brand"/>
   
   <!-- sort mode -->
   <xsl:param name="sort"/>
   
   <!-- raw XML dump flag -->
   <xsl:param name="raw"/>
   
   <!-- score normalization and explanation (for debugging) -->
   <xsl:param name="normalizeScores"/>
   <xsl:param name="explainScores"/>
   
   <!-- first hit on page -->
   <xsl:param name="startDoc" select="1"/>
   
   <!-- documents per page -->
   <xsl:param name="docsPerPage">
      <xsl:choose>
         <xsl:when test="$raw">
            <xsl:value-of select="10000"/>
         </xsl:when>
         <xsl:when test="matches($http.user-agent,$robots)">
            <xsl:value-of select="90"/><!-- maximum amount allowed by google is 100 -->
         </xsl:when>
         <xsl:otherwise>
            <xsl:value-of select="20"/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:param>
   
   <!-- Special Robot Parameters -->
   <xsl:param name="http.user-agent"/>
   <!-- WARNING: Inclusion of 'Wget' is for testing only, please remove before going into production -->
   <xsl:param name="robots" select="'Googlebot|Slurp|msnbot|Teoma|Wget'"/>
   
   <!-- list of keyword search fields -->
   <xsl:param name="fieldList"/>
   
   <!-- ====================================================================== -->
   <!-- Multi-field keyword query template                                     -->
   <!--                                                                        -->
   <!-- Join all the terms of a multi-field query together.                    -->
   <!-- ====================================================================== -->
   
   <xsl:template match="param[@name = 'keyword']">
      <or>
         <and fields="{replace($fieldList, 'text ?', '')}"
              slop="10"
              maxMetaSnippets="all"
              maxContext="60">
            <xsl:apply-templates/>
         </and>
         <and field="text" maxSnippets="3" maxContext="60">
            <xsl:apply-templates/>
         </and>
      </or>
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
         
         <!-- Specify the field name the query -->
         <xsl:choose>
            <xsl:when test="not(matches(@name, 'text|query'))">
               <xsl:attribute name="field" select="$metaField"/>
            </xsl:when>
            <xsl:when test="@name = 'text'">
               <xsl:attribute name="field" select="'text'"/>
               <xsl:attribute name="maxSnippets" select="'3'"/>
               <xsl:attribute name="maxContext" select="'60'"/>
            </xsl:when>
            <xsl:when test="@name = 'query'">
               <xsl:attribute name="field" select="'text'"/>
               <xsl:attribute name="maxSnippets" select="'all'"/>
               <xsl:attribute name="maxContext" select="'80'"/>
            </xsl:when>
         </xsl:choose>
         
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
         
         <!-- If there is a sectionType parameter, process it -->
         <xsl:if test="matches($metaField, 'text|query') and (//param[@name='sectionType']/@value != '')">
            <sectionType>
               <xsl:apply-templates select="//param[@name='sectionType']/*"/>
            </sectionType>
         </xsl:if>
         
      </xsl:element>
      
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
   
   <!-- ====================================================================== -->
   <!-- Freeform queries                                                       -->
   <!--                                                                        -->
   <!-- These come pretty much in XTF format from the Java parser... we just   -->
   <!-- need to do some decoration.                                            -->
   <!-- ====================================================================== -->
   
   <!-- Handle multi-field keyword queries -->
   <xsl:template match="and[matches(@field, '^(serverChoice|keywords)$')] |
                        or[matches(@field, '^(serverChoice|keywords)$')]"
                 mode="freeform">
      <xsl:copy>
         <xsl:attribute name="fields" select="$fieldList"/>
         <xsl:attribute name="slop" select="10"/>
         <xsl:attribute name="maxTextSnippets" select="'3'"/>
         <xsl:attribute name="maxMetaSnippets" select="'all'"/>
         <xsl:attribute name="maxContext" select="'60'"/>
         <xsl:apply-templates mode="freeform"/>
      </xsl:copy>
   </xsl:template>
   
   <!-- Wrap terms or phrases with a field spec into an <and>, to make the query
        formatting logic simpler.
   -->
   <xsl:template match="term[@field] | phrase[@field]" mode="freeform">
      <xsl:variable name="wrapped">
         <and field="{@field}">
            <xsl:copy>
               <xsl:copy-of select="*|text()"/>
            </xsl:copy>
         </and>
      </xsl:variable>
      <xsl:apply-templates select="$wrapped" mode="freeform"/>
   </xsl:template>
   
   <!-- Arbitrarily change multi-field unary not to 'text' (have to pick something) -->
   <xsl:template match="not[matches(@field, '^(serverChoice|keywords)$')]" mode="freeform">
      <and field="text">
         <not>
            <xsl:apply-templates mode="freeform"/>
         </not>
      </and>
   </xsl:template>
   
   <!-- Add maxContext to all fielded queries, plus maxSnippets for text queries -->
   <xsl:template match="*[@field]" priority="-1" mode="freeform">
      <xsl:copy>
         <xsl:copy-of select="@*"/>
         <xsl:attribute name="maxContext" select="'60'"/>
         <xsl:if test="@field = 'text'">
            <xsl:attribute name="maxSnippets" select="'3'"/>
         </xsl:if>
         <xsl:apply-templates mode="freeform"/>
      </xsl:copy>
   </xsl:template>
   
   <!-- All other stuff can be copied unchanged -->
   <xsl:template match="*" mode="freeform" priority="-2">
      <xsl:copy>
         <xsl:copy-of select="@*"/>
         <xsl:apply-templates mode="freeform"/>
      </xsl:copy>
   </xsl:template>
      
   <!-- ====================================================================== -->
   <!-- "Add To Bag" template                                                  -->
   <!--                                                                        -->
   <!-- Adds the document identifier specified in the URL to the bag in the    -->
   <!-- current session.                                                       -->
   <!-- ====================================================================== -->
   
   <xsl:template name="addToBag">
      <xsl:variable name="identifier" select="string(//param[@name='identifier']/@value)"/>
      <xsl:variable name="newBag">
         <bag>
            <xsl:copy-of select="session:getData('bag')/bag/savedDoc"/>
            <savedDoc id="{$identifier}"/>
         </bag>
      </xsl:variable>
      <xsl:value-of select="session:setData('bag', $newBag)"/>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- "Remove From Bag" template                                             -->
   <!--                                                                        -->
   <!-- Removes the document identifier specified in the URL from the bag in   -->
   <!-- the current session.                                                   -->
   <!-- ====================================================================== -->
   
   <xsl:template name="removeFromBag">
      <xsl:variable name="identifier" select="string(//param[@name='identifier']/@value)"/>
      <xsl:variable name="newBag">
         <bag>
            <xsl:copy-of select="session:getData('bag')/bag/savedDoc[not(@id=$identifier)]"/>
         </bag>
      </xsl:variable>
      <xsl:value-of select="session:setData('bag', $newBag)"/>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- "Show Bag" template                                                    -->
   <!--                                                                        -->
   <!-- Forms a query of all the documents currently in the bag.               -->
   <!-- ====================================================================== -->
   
   <xsl:template name="showBag">
      <xsl:variable name="bag" select="session:getData('bag')"/>
      <xsl:if test="$bag/bag/savedDoc">
         <or>
            <xsl:for-each select="$bag/bag/savedDoc">
               <term field="identifier"><xsl:value-of select="@id"/></term>
            </xsl:for-each>
         </or>
      </xsl:if>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- "More Like This" template                                              -->
   <!--                                                                        -->
   <!-- Forms a query of a single document ID, and fetches documents like it.  -->
   <!-- ====================================================================== -->
   
   <xsl:template name="moreLike">
      <xsl:variable name="identifier" select="string(//param[@name='identifier']/@value)"/>
      <moreLike fields="title,subject">
         <term field="identifier"><xsl:value-of select="$identifier"/></term>
      </moreLike>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Robot Template                                                         -->
   <!--                                                                        -->
   <!-- Ensure that crawlers get the entire collection                         -->
   <!-- ====================================================================== -->
   
   <xsl:template name="robot">
      <and>
         <allDocs/>
      </and>
   </xsl:template>
   
</xsl:stylesheet>
