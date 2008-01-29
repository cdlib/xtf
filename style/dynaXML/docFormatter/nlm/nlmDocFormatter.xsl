<xsl:stylesheet version="2.0"
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:xtf="http://cdlib.org/xtf">
   
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
   
   <!-- ====================================================================== -->
   <!-- Import Common Templates                                                -->
   <!-- ====================================================================== -->
   
   <xsl:import href="../common/docFormatterCommon.xsl"/>
   
   <xsl:output method="html" 
      indent="yes" 
      encoding="UTF-8" 
      doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
      doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"/>
   
   
   <!-- ====================================================================== -->
   <!-- Strip Space                                                            -->
   <!-- ====================================================================== -->
   
   <xsl:strip-space elements="*"/>
   
   <!-- ====================================================================== -->
   <!-- Included Stylesheets                                                   -->
   <!-- ====================================================================== -->
   
   <xsl:include href="ViewNLM-v2.3.xsl"/>
   
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
         <xsl:when test="matches($http.User-Agent,$robots)">
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
      
      <html>
         <head>
            <title>
               <xsl:value-of select="$doc.title"/>
            </title>
         </head>
         <frameset rows="80,*" border="2" framespacing="2" frameborder="1">
            <frame scrolling="no" title="Navigation Bar">
               <xsl:attribute name="name">bbar</xsl:attribute>
               <xsl:attribute name="src"><xsl:value-of select="$xtfURL"/>view?<xsl:value-of select="$bbar.href"/></xsl:attribute>
            </frame>
            <frameset cols="25%,75%" border="2" framespacing="2" frameborder="1">
               <frame title="Table of Contents">
                  <xsl:attribute name="name">toc</xsl:attribute>
                  <xsl:attribute name="src"><xsl:value-of select="$xtfURL"/>view?<xsl:value-of select="$toc.href"/></xsl:attribute>
               </frame>
               <frame title="Content">
                  <xsl:attribute name="name">content</xsl:attribute>
                  <xsl:attribute name="src"><xsl:value-of select="$xtfURL"/>view?<xsl:value-of select="$content.href"/></xsl:attribute>
               </frame>
            </frameset>
         </frameset>
         <noframes>
            <h1>Sorry, your browser doesn't support frames...</h1>
         </noframes>
      </html>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Anchor Template                                                        -->
   <!-- ====================================================================== -->
   
   <xsl:template name="create.anchor">
      <xsl:choose>
         <xsl:when test="($query != '0' and $query != '') and $hit.rank != '0'">
            <xsl:text>#</xsl:text><xsl:value-of select="key('hit-rank-dynamic', $hit.rank)/@hitNum"/>
         </xsl:when>
         <xsl:when test="$anchor.id != '0'">
            <xsl:text>#</xsl:text><xsl:value-of select="$anchor.id"/>
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
      <html>
         <head>
            <title>
               <xsl:value-of select="$doc.title"/>
            </title>
            <xsl:copy-of select="$brand.links"/>
            <link rel="stylesheet" type="text/css" href="{$css.path}toc.css"/>
         </head>
         <body>
            <div class="toc">
               <h4><xsl:value-of select="$doc.title"/></h4>
               <hr/>
               <xsl:apply-templates select="//body/sec" mode="toc"/>
               <hr/>
            </div>
         </body>
      </html>
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
      
      <html>
         <head>
            <title>
               <xsl:value-of select="$doc.title"/>
            </title>
            <xsl:copy-of select="$brand.links"/>
            <link rel="stylesheet" type="text/css" href="{$css.path}{$content.css}"/>
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
   <!-- Print Template                                                  -->
   <!-- ====================================================================== -->
   
   <xsl:template name="print">
      <html>
         <head>
            <title>
               <xsl:value-of select="$doc.title"/>
            </title>
            <xsl:copy-of select="$brand.links"/>
            <link rel="stylesheet" type="text/css" href="{$css.path}{$content.css}"/>
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
   
   <!-- ====================================================================== -->
   <!-- Button Bar Templates                                                   -->
   <!-- ====================================================================== -->
   
   <xsl:template name="bbar">
      <html>
         <head>
            <title>
               <xsl:value-of select="$doc.title"/>
            </title>
            <xsl:copy-of select="$brand.links"/>
            <link rel="stylesheet" type="text/css" href="{$css.path}bbar.css"/>
         </head>
         <body>
            <xsl:call-template name="bbar.table"/>
         </body>
      </html>
   </xsl:template>
   
   <xsl:template name="bbar.table">
      
      <xsl:variable name="target">
         <xsl:text>_top</xsl:text>
      </xsl:variable>
      
      <xsl:copy-of select="$brand.header"/>
      
      <table width="100%" border="0" cellpadding="0" cellspacing="0">
         
         <!-- BEGIN TOPNAV ROW -->
         <tr  width="100%">
            <td class="topnav-outer" colspan="3" width="100%" height="31" align="center" valign="middle">
               
               <!-- BEGIN TOPNAV LEFT -->
               <table width="100%" height="27" border="0" cellpadding="0" cellspacing="0">
                  <tr>
                     <td class="topnav-inner" width="25%"  align="center" valign="middle">
                        
                        <!-- BEGIN TOPNAV LEFT INNER TABLE -->
                        <table border="0" cellpadding="0" cellspacing="0">
                           <tr align="left" valign="middle">
                              <td width="8" nowrap="nowrap"><img src="{$icon.path}spacer.gif" width="8"/></td>
                              <td width="15" nowrap="nowrap">
                                 <a href="search" target="{$target}">
                                    <img src="{$icon.path}arrow.gif" width="15" height="15" border="0"/>
                                 </a>
                              </td>
                              <td nowrap="nowrap">&#160;<a class="topnav" href="search" target="{$target}">Home</a></td>
                              <td width="10" nowrap="nowrap"><img src="{$icon.path}spacer.gif" width="10"/></td>
                              
                              <td width="15" nowrap="nowrap">
                                 <a class="topnav" target="{$target}">
                                    <xsl:attribute name="href"><xsl:value-of select="$doc.path"/>&#038;doc.view=print&#038;toc.depth=<xsl:value-of select="$toc.depth"/>&#038;toc.id=<xsl:value-of select="$toc.id"/><xsl:value-of select="$search"/></xsl:attribute>
                                    <img src="{$icon.path}arrow.gif" width="15" height="15" border="0"/>
                                 </a>
                              </td>
                              
                              <td nowrap="nowrap">
                                 <xsl:text>&#160;</xsl:text>
                                 <a class="topnav" target="{$target}">
                                    <xsl:attribute name="href"><xsl:value-of select="$doc.path"/>&#038;doc.view=print&#038;toc.depth=<xsl:value-of select="$toc.depth"/>&#038;toc.id=<xsl:value-of select="$toc.id"/><xsl:value-of select="$search"/></xsl:attribute>
                                    <xsl:text>Print View</xsl:text>
                                 </a>
                                 <xsl:text>&#160;</xsl:text>
                              </td>
                           </tr>
                        </table>
                        <!-- END TOPNAV LEFT INNER TABLE -->
                        
                     </td>
                     <!-- END TOPNAV LEFT -->
                     
                     <td width="2"><img src="{$icon.path}spacer.gif" width="2"/></td>
                     
                     <!-- BEGIN TOPNAV CENTER -->
                     <form action="{$xtfURL}{$dynaxmlPath}" target="{$target}" method="GET">
                        <input type="hidden" name="docId">
                           <xsl:attribute name="value">
                              <xsl:value-of select="$docId"/>
                           </xsl:attribute>
                        </input>     
                        
                        <td class="topnav-inner" width="50%" align="center" nowrap="nowrap">
                           
                           <!-- BEGIN TOPNAV LEFT INNER TABLE -->
                           <table border="0" cellpadding="0" cellspacing="0">
                              <tr align="left" valign="middle">
                                 <td nowrap="nowrap"><span class="search-text">Search</span>&#160;</td>                      
                                 <td nowrap="nowrap"><input name="query" type="text" size="15"/>&#160;<input type="submit" value="Go"/></td>
                              </tr>
                           </table>
                           <!-- END TOPNAV LEFT INNER TABLE -->
                           
                        </td>
                     </form>
                     <!-- END TOPNAV CENTER -->
                     
                     <td width="2"><img src="{$icon.path}spacer.gif" width="2"/></td>
                     
                     <!-- BEGIN TOPNAV RIGHT -->
                     <td class="topnav-inner" width="25%" align="center" valign="middle">
                        
                        <!-- BEGIN TOPNAV RIGHT INNER TABLE -->
                        <table border="0" cellpadding="0" cellspacing="0">
                           <tr align="right" valign="middle">
                              <td width="15" nowrap="nowrap"><img src="{$icon.path}spacer.gif" width="10"/></td> 
                              <td width="15" nowrap="nowrap"><img src="{$icon.path}spacer.gif" width="10"/></td> 
                              <td width="10" nowrap="nowrap"><img src="{$icon.path}spacer.gif" width="10"/></td>
                              <td width="15" nowrap="nowrap">
                                 <a class="topnav" href="http://xtf.sourceforge.net" target="{$target}">
                                    <img src="{$icon.path}arrow.gif" width="15" height="15" border="0"/>
                                 </a>
                              </td>
                              <td nowrap="nowrap">&#160;<a class="topnav" href="http://xtf.sourceforge.net" target="{$target}">Help</a></td>
                              <td width="8" nowrap="nowrap"><img src="{$icon.path}spacer.gif" width="8"/></td>
                           </tr>
                        </table>
                        <!-- END TOPNAV RIGHT INNER TABLE -->
                        
                     </td>
                     <!-- END TOPNAV RIGHT -->
                     
                  </tr>
               </table>
               
            </td>
         </tr>
      </table>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Search Hits                                                            -->
   <!-- ====================================================================== -->
   
   <xsl:template match="xtf:hit">
      
      <a name="{@hitNum}"/>
      
      <xsl:call-template name="prev.hit"/>
      
      <xsl:choose>
         <xsl:when test="xtf:term">
            <span style="background-color: #D6DCE5;">
               <xsl:apply-templates/>
            </span>
         </xsl:when>
         <xsl:otherwise>
            <span style="background-color: #D6DCE5; color: red; font-weight: bold;">
               <xsl:apply-templates/>
            </span>
         </xsl:otherwise>
      </xsl:choose>
      
      <xsl:if test="not(@more='yes')">
         <xsl:call-template name="next.hit"/>
      </xsl:if>
      
   </xsl:template>
   
   <xsl:template match="xtf:more">
      
      <span style="background-color: #D6DCE5;">
         <xsl:apply-templates/>
      </span>
      
      <xsl:if test="not(@more='yes')">
         <xsl:call-template name="next.hit"/>
      </xsl:if>
      
   </xsl:template>
   
   <xsl:template match="xtf:term">
      <span style="color: red; font-weight: bold;">
         <xsl:apply-templates/>
      </span>
   </xsl:template>
   
   <xsl:template name="prev.hit">
      
      <xsl:variable name="num" select="@hitNum"/>
      <xsl:variable name="prev" select="$num - 1"/>
      
      <a>
         <xsl:attribute name="href">
            <xsl:text>#</xsl:text><xsl:value-of select="$prev"/>
         </xsl:attribute>
         <img src="{$icon.path}b_inprev.gif" border="0" alt="previous hit"/>
      </a>
      <xsl:text>&#160;</xsl:text>
      
   </xsl:template>
   
   <xsl:template name="next.hit">
      
      <xsl:variable name="num" select="@hitNum"/>
      <xsl:variable name="next" select="$num + 1"/>
      
      <xsl:text>&#160;</xsl:text>
      <a>
         <xsl:attribute name="href">
            <xsl:text>#</xsl:text><xsl:value-of select="$next"/>
         </xsl:attribute>
         <img src="{$icon.path}b_innext.gif" border="0" alt="next hit"/>
      </a>
      
   </xsl:template>
   
</xsl:stylesheet>
