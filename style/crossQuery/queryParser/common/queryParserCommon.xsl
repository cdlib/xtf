<!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
<!-- Common templates for query parser stylesheets                          -->
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
  complex queries (boolean and/or/not, ranges, nested queries, etc.) An
  experimental parser is available that does parse these constructs; see
  complexQueryParser.xsl.
  
  For details on the input and output expected of this stylesheet, see the
  comment section at the bottom of this file.
-->

<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:session="java:org.cdlib.xtf.xslt.Session"
   exclude-result-prefixes="xsl session">
   
   <!-- ====================================================================== -->
   <!-- Global parameters (specified in the URL)                               -->
   <!-- ====================================================================== -->
   
   <!-- style param -->
   <xsl:param name="style"/>
   
   <!-- search mode -->
   <xsl:param name="smode"/>
   
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
         <xsl:when test="($smode = 'test') or $raw">
            <xsl:value-of select="10000"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:value-of select="20"/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:param>
   
   <!-- list of keyword search fields -->
   <xsl:param name="fieldList"/>
   
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
            <xsl:when test="$metaField = 'keyword'">
               <xsl:value-of select="'or'"/>
            </xsl:when>
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
         <xsl:choose>
            <xsl:when test="matches(@name, 'keyword')"/>
            <xsl:when test="not(matches(@name, 'text|query'))">
               <xsl:attribute name="field" select="$metaField"/>
            </xsl:when>
            <xsl:when test="matches(@name, 'text')">
               <xsl:attribute name="field" select="'text'"/>
               <xsl:attribute name="maxSnippets" select="'3'"/>
               <xsl:attribute name="maxContext" select="'100'"/>
            </xsl:when>
            <xsl:when test="matches(@name, 'query')">
               <xsl:attribute name="field" select="'text'"/>
               <xsl:attribute name="maxSnippets" select="'-1'"/>
               <xsl:attribute name="maxContext" select="'80'"/>
            </xsl:when>
         </xsl:choose>
         
         <xsl:choose>
            <xsl:when test="matches(@name, 'keyword')">
               <xsl:call-template name="keyQuery">
                  <xsl:with-param name="fieldList">
                     <xsl:choose>
                        <xsl:when test="$fieldList != ''">
                           <xsl:value-of select="concat($fieldList, ' ')"/>
                        </xsl:when>
                        <xsl:otherwise>
                           <xsl:value-of select="'text title creator subject description publisher or contributor date type format identifier source language relation coverage rights year '"/>
                        </xsl:otherwise>
                     </xsl:choose>
                  </xsl:with-param>
                  <xsl:with-param name="prox" select="$prox"/>
                  <xsl:with-param name="join" select="$join"/>
                  <xsl:with-param name="exclude" select="$exclude"/>
               </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
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
            </xsl:otherwise>
         </xsl:choose>
         
         <!-- If there is a sectionType parameter, process it -->
         <xsl:if test="matches($metaField, 'text|query') and (//param[@name='sectionType']/@value != '')">
            <sectionType>
               <xsl:apply-templates select="//param[@name='sectionType']/*"/>
            </sectionType>
         </xsl:if>
         
      </xsl:element>
      
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Keyword query template                                                 -->
   <!--                                                                        -->
   <!-- Constructs a full combined text and metadata query from the following  -->
   <!-- URL constructs: keyword=foo, keyword-prox=20, keyword-exclude=bar.     -->
   <!-- ====================================================================== -->
   
   <xsl:template name="keyQuery">
      
      <xsl:param name="fieldList"/>
      <xsl:param name="prox"/>
      <xsl:param name="join"/>
      <xsl:param name="exclude"/>
      
      <xsl:variable name="op">
         <xsl:choose>   
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
      
      <xsl:variable name="field" select="substring-before($fieldList, ' ')"/>
      
      <xsl:if test="$field != ''">
         <xsl:element name="{$op}">
            
            <xsl:attribute name="field" select="$field"/>
            
            <xsl:if test="$field = 'text'">
               <xsl:attribute name="maxSnippets" select="'3'"/>
               <xsl:attribute name="maxContext" select="'100'"/>
            </xsl:if>
            
            <xsl:if test="$prox/@value != ''">
               <xsl:attribute name="slop" select="$prox/@value"/>
            </xsl:if>
            
            <xsl:apply-templates/>
            
            <xsl:if test="$exclude/@value != ''">
               <not>
                  <xsl:apply-templates select="$exclude/*"/>
               </not>
            </xsl:if>
            
         </xsl:element>
         
         <xsl:call-template name="keyQuery">
            <xsl:with-param name="fieldList" select="replace(substring-after($fieldList, $field), '^ ', '')"/>
            <xsl:with-param name="prox" select="$prox"/>
            <xsl:with-param name="join" select="$join"/>
            <xsl:with-param name="exclude" select="$exclude"/>
         </xsl:call-template>
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
   
</xsl:stylesheet>
