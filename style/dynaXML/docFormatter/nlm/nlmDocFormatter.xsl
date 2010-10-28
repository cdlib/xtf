<xsl:stylesheet version="2.0" 
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:xtf="http://cdlib.org/xtf"
   xmlns:session="java:org.cdlib.xtf.xslt.Session"
   xmlns="http://www.w3.org/1999/xhtml"
   extension-element-prefixes="session"
   exclude-result-prefixes="#all">
   
   <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
   <!-- NLM dynaXML Stylesheet                                                 -->
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
      NOTE: This is rough adaptation of the NLM stylesheets to get them to 
      work with XTF. It should in no way be considered a production interface 
   -->
   
   <!-- ====================================================================== -->
   <!-- Import Common Templates                                                -->
   <!-- ====================================================================== -->
   
   <xsl:import href="../common/docFormatterCommon.xsl"/>
   
   <!-- ====================================================================== -->
   <!-- Output Format                                                          -->
   <!-- ====================================================================== -->
   
   <xsl:output method="xhtml" indent="no" 
      encoding="UTF-8" media-type="text/html; charset=UTF-8" 
      doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN" 
      doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd" 
      exclude-result-prefixes="#all"
      omit-xml-declaration="yes"/>
   
   <xsl:output name="frameset" method="xhtml" indent="no" 
      encoding="UTF-8" media-type="text/html; charset=UTF-8" 
      doctype-public="-//W3C//DTD XHTML 1.0 Frameset//EN" 
      doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-frameset.dtd" 
      omit-xml-declaration="yes"
      exclude-result-prefixes="#all"/>
   
   <!-- ====================================================================== -->
   <!-- Strip Space                                                            -->
   <!-- ====================================================================== -->
   
   <xsl:strip-space elements="*"/>
   
   <!-- ====================================================================== -->
   <!-- Included Stylesheets                                                   -->
   <!-- ====================================================================== -->
   
   <xsl:include href="ViewNLM-v2.3.xsl"/>
   <xsl:include href="search.xsl"/>
   
   <!-- ====================================================================== -->
   <!-- Define Keys                                                            -->
   <!-- ====================================================================== -->
   
   <xsl:key name="div-id" match="sec" use="@id"/>
   
   <!-- ====================================================================== -->
   <!-- Define Parameters                                                      -->
   <!-- ====================================================================== -->
   
   <xsl:param name="ID" select="replace($docId,'[A-Za-z0-9]+\.xml$','')"/>
   <xsl:param name="icon.path" select="concat($xtfURL, 'icons/default/')"/>
   <xsl:param name="doc.title" select="/article/front/article-meta/title-group/article-title[1]"/>
   <xsl:param name="css.path" select="'css/default/'"/>
   <xsl:param name="content.css" select="'nlm.css'"/>
   
   <!-- ====================================================================== -->
   <!-- Root Template                                                          -->
   <!-- ====================================================================== -->
   
   <xsl:template match="/">
      <xsl:choose>
         <!-- robot solution -->
         <xsl:when test="matches($http.user-agent,$robots)">
            <xsl:call-template name="robot"/>
         </xsl:when>
         <xsl:when test="$doc.view='bbar'">
            <xsl:call-template name="bbar"/>
         </xsl:when>
         <xsl:when test="$doc.view='toc'">
            <xsl:call-template name="toc"/>
         </xsl:when>
         <xsl:when test="$doc.view='content'">
            <xsl:call-template name="content"/>
         </xsl:when>
         <xsl:when test="$doc.view='citation'">
            <xsl:call-template name="citation"/>
         </xsl:when>
         <xsl:when test="$doc.view='print'">
            <xsl:call-template name="print"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:call-template name="frames"/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Frames Template                                                        -->
   <!-- ====================================================================== -->
   
   <xsl:template name="frames">
      
      <xsl:variable name="bbar.href"><xsl:value-of select="$query.string"/>&#038;doc.view=bbar&#038;toc.depth=<xsl:value-of select="$toc.depth"/>&#038;brand=<xsl:value-of select="$brand"/><xsl:value-of select="$search"/></xsl:variable> 
      <xsl:variable name="toc.href"><xsl:value-of select="$query.string"/>&#038;doc.view=toc&#038;toc.depth=<xsl:value-of select="$toc.depth"/>&#038;brand=<xsl:value-of select="$brand"/>&#038;toc.id=<xsl:value-of select="$toc.id"/><xsl:value-of select="$search"/>#X</xsl:variable>
      <xsl:variable name="content.href"><xsl:value-of select="$query.string"/>&#038;doc.view=content&#038;toc.depth=<xsl:value-of select="$toc.depth"/>&#038;brand=<xsl:value-of select="$brand"/><xsl:value-of select="$search"/><xsl:call-template name="create.anchor"/></xsl:variable>
      
      <xsl:result-document format="frameset" exclude-result-prefixes="#all">
         <html xml:lang="en" lang="en">
            <head>
               <title>
                  <xsl:value-of select="$doc.title"/>
               </title>
               <link rel="shortcut icon" href="icons/default/favicon.ico" />
            </head>
            <frameset rows="120,*">
               <frame frameborder="1" scrolling="no" title="Navigation Bar">
                  <xsl:attribute name="name">bbar</xsl:attribute>
                  <xsl:attribute name="src"><xsl:value-of select="$xtfURL"/>view?<xsl:value-of select="$bbar.href"/></xsl:attribute>
               </frame>
               <frameset cols="25%,75%">
                  <frame frameborder="1" title="Table of Contents">
                     <xsl:attribute name="name">toc</xsl:attribute>
                     <xsl:attribute name="src"><xsl:value-of select="$xtfURL"/>view?<xsl:value-of select="$toc.href"/></xsl:attribute>
                  </frame>
                  <frame frameborder="1" title="Content">
                     <xsl:attribute name="name">content</xsl:attribute>
                     <xsl:attribute name="src"><xsl:value-of select="$xtfURL"/>view?<xsl:value-of select="$content.href"/></xsl:attribute>
                  </frame>
               </frameset>
               <noframes>
                  <body>
                     <h1>Sorry, your browser doesn't support frames...</h1>
                  </body>
               </noframes>
            </frameset>
         </html>
      </xsl:result-document>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Anchor Template                                                        -->
   <!-- ====================================================================== -->
   
   <xsl:template name="create.anchor">
      <xsl:choose>
         <xsl:when test="($query != '0' and $query != '') and $hit.rank != '0'">
            <xsl:text>#</xsl:text><xsl:value-of select="key('hit-rank-dynamic', $hit.rank)/@hitNum"/>
         </xsl:when>
         <xsl:when test="$anchor.id != '0' and $anchor.id != ''">
            <xsl:choose>
               <xsl:when test="key('div-id',$anchor.id)/@xtf:firstHit">
                  <xsl:text>#</xsl:text><xsl:value-of select="key('div-id',$anchor.id)/@xtf:firstHit"/>
               </xsl:when>
               <xsl:otherwise>
                  <xsl:text>#</xsl:text><xsl:value-of select="$anchor.id"/>
               </xsl:otherwise>
            </xsl:choose>
         </xsl:when>
         <xsl:when test="$query != '0' and $query != ''">
            <xsl:text>#</xsl:text><xsl:value-of select="/*/@xtf:firstHit"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:text>#X</xsl:text>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- TOC Template                                                           -->
   <!-- ====================================================================== -->
   
   <xsl:template name="toc">
      <xsl:call-template name="translate">
         <xsl:with-param name="resultTree">
            <html xml:lang="en" lang="en">
               <head>
                  <title>
                     <xsl:value-of select="$doc.title"/>
                  </title>
                  <link rel="stylesheet" type="text/css" href="{$css.path}toc.css"/>
                  <link rel="shortcut icon" href="icons/default/favicon.ico" />

               </head>
               <body>
                  <div class="toc">
                     <h4><xsl:value-of select="$doc.title"/></h4>
                     <hr/>
                     <xsl:apply-templates select="/article/body/sec" mode="toc"/>
                     <hr/>
                  </div>
               </body>
            </html>
         </xsl:with-param>
      </xsl:call-template>
   </xsl:template>
   
   <xsl:template match="sec" mode="toc">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
         <tr>
            <td>
               <xsl:attribute name="width">
                  <xsl:choose>
                     <xsl:when test="parent::sec/parent::sec/parent::sec">100</xsl:when>
                     <xsl:when test="parent::sec/parent::sec">75</xsl:when>
                     <xsl:when test="parent::sec">50</xsl:when>
                     <xsl:otherwise>25</xsl:otherwise>
                  </xsl:choose>
               </xsl:attribute>
               <xsl:text>&#160;</xsl:text>
            </td>
            <td width="25" valign="top">
               <xsl:choose>
                  <xsl:when test="@xtf:hitCount">
                     <span class="hit-count">
                        <xsl:value-of select="@xtf:hitCount"/>
                     </span>
                  </xsl:when>
                  <xsl:otherwise>
                     <xsl:text>&#160;</xsl:text>
                  </xsl:otherwise>
               </xsl:choose>
            </td>
            <td>
               <a href="{$xtfURL}{$dynaxmlPath}?docId={$docId};query={$query};brand={$brand};anchor.id={@id}" target="_top">
                  <xsl:value-of select="title"/>
               </a>
            </td>
         </tr>
      </table>
      <xsl:if test="sec">
         <xsl:apply-templates select="sec" mode="toc"/>
      </xsl:if>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Content Template                                                       -->
   <!-- ====================================================================== -->
   
   <xsl:template name="content">
      
      <html xml:lang="en" lang="en">
         <head>
            <title>
               <xsl:value-of select="$doc.title"/>
            </title>
            <link rel="stylesheet" type="text/css" href="{$css.path}{$content.css}"/>
            <link rel="shortcut icon" href="icons/default/favicon.ico" />

         </head>
         <body>
            
            <table width="100%" border="0" cellpadding="0" cellspacing="0">
               <!-- BEGIN MIDNAV ROW -->
               <tr width="100%">
                  <td colspan="2" width="100%" align="center" valign="top">
                     <!-- BEGIN MIDNAV INNER TABLE -->
                     <table width="94%" border="0" cellpadding="0" cellspacing="0">
                        <tr>
                           <td colspan="3">
                              <xsl:text>&#160;</xsl:text>
                           </td>
                        </tr>
                        <tr>
                           <td colspan="3" >
                              <hr class="hr-title"/>
                           </td>
                        </tr>
                     </table>
                     <!-- END MIDNAV INNER TABLE -->
                  </td>
               </tr>
               <!-- END MIDNAV ROW -->
            </table>
            
            <!-- BEGIN CONTENT ROW -->
            <table width="100%" border="0" cellpadding="0" cellspacing="0">
               <tr>
                  <td align="left" valign="top">
                     <div class="content">
                        <!-- BEGIN CONTENT -->
                        <xsl:apply-templates select="article"/>
                     </div>
                  </td>
               </tr>
            </table>
            
            <table width="100%" border="0" cellpadding="0" cellspacing="0">
               <!-- BEGIN MIDNAV ROW -->
               <tr width="100%">
                  <td colspan="2" width="100%" align="center" valign="top">
                     <!-- BEGIN MIDNAV INNER TABLE -->
                     <table width="94%" border="0" cellpadding="0" cellspacing="0">
                        <tr>
                           <td colspan="3">
                              <hr class="hr-title"/>
                           </td>
                        </tr>
                        <tr>
                           <td colspan="3" >
                              <xsl:text>&#160;</xsl:text>
                           </td>
                        </tr>
                     </table>
                     <!-- END MIDNAV INNER TABLE -->
                  </td>
               </tr>
               <!-- END MIDNAV ROW -->
            </table>
            
         </body>
      </html>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Print Template                                                         -->
   <!-- ====================================================================== -->
   
   <xsl:template name="print">
      <html xml:lang="en" lang="en">
         <head>
            <title>
               <xsl:value-of select="$doc.title"/>
            </title>
            <link rel="stylesheet" type="text/css" href="{$css.path}{$content.css}"/>
            <link rel="shortcut icon" href="icons/default/favicon.ico" />

         </head>
         <body bgcolor="white">
            <hr class="hr-title"/>
            <div align="center">
               <table width="95%">
                  <tr>
                     <td>
                        <xsl:apply-templates select="/article"/>
                     </td>
                  </tr>
               </table>
            </div>
            <hr class="hr-title"/>
         </body>
      </html>
   </xsl:template>
   
</xsl:stylesheet>
