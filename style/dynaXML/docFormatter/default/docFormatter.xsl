<!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
<!-- dynaXML Stylesheet                                                     -->
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

<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xtf="http://cdlib.org/xtf">
  
  <xsl:output method="html"
              indent="yes"
              encoding="utf-8"
              media-type="text/html"
              doctype-public="-//W3C//DTD HTML 4.0//EN"/>
  
<!-- ====================================================================== -->
<!-- Strip Space                                                            -->
<!-- ====================================================================== -->

<xsl:strip-space elements="*"/>

<!-- ====================================================================== -->
<!-- Included Stylesheets                                                   -->
<!-- ====================================================================== -->

<xsl:include href="../common/docFormatterCommon.xsl"/>

<xsl:include href="autotoc.xsl"/>
<xsl:include href="component.xsl"/>
<xsl:include href="search.xsl"/>
<xsl:include href="parameter.xsl"/>
<xsl:include href="structure.xsl"/>
<xsl:include href="table.xsl"/>
<xsl:include href="titlepage.xsl"/>

<!-- ====================================================================== -->
<!-- Define Keys                                                            -->
<!-- ====================================================================== -->

<xsl:key name="pb-id" match="pb|milestone" use="@id"/>
<xsl:key name="ref-id" match="ref" use="@id"/>
<xsl:key name="formula-id" match="formula" use="@id"/>
<xsl:key name="fnote-id" match="note[@type='footnote' or @place='foot']" use="@id"/>
<xsl:key name="endnote-id" match="note[@type='endnote' or @place='end']" use="@id"/>
<xsl:key name="div-id" match="div1|div2|div3|div4|div5|div6" use="@id"/>
<xsl:key name="hit-num-dynamic" match="xtf:hit" use="@hitNum"/>
<xsl:key name="hit-rank-dynamic" match="xtf:hit" use="@rank"/>
<xsl:key name="generic-id" match="note[not(@type='footnote' or @place='foot' or @type='endnote' or @place='end')]|figure|bibl|table" use="@id"/>

<!-- ====================================================================== -->
<!-- Root Template                                                          -->
<!-- ====================================================================== -->

<xsl:template match="/">

  <xsl:choose>
    <xsl:when test="$doc.view='bbar'">
      <xsl:call-template name="bbar"/>
    </xsl:when>
    <xsl:when test="$doc.view='toc'">
      <xsl:call-template name="toc"/>
    </xsl:when>
    <xsl:when test="$doc.view='content'">
      <xsl:call-template name="content"/>
    </xsl:when>
    <xsl:when test="$doc.view='popup'">
      <xsl:call-template name="popup"/>
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

  <xsl:variable name="bbar.href"><xsl:value-of select="$query.string"/>&#038;doc.view=bbar&#038;chunk.id=<xsl:value-of select="$chunk.id"/>&#038;toc.depth=<xsl:value-of select="$toc.depth"/>&#038;brand=<xsl:value-of select="$brand"/><xsl:value-of select="$search"/></xsl:variable>

  <xsl:variable name="toc.href"><xsl:value-of select="$query.string"/>&#038;doc.view=toc&#038;chunk.id=<xsl:value-of select="$chunk.id"/>&#038;toc.depth=<xsl:value-of select="$toc.depth"/>&#038;brand=<xsl:value-of select="$brand"/>&#038;toc.id=<xsl:value-of select="$toc.id"/><xsl:value-of select="$search"/>#X</xsl:variable>

  <xsl:variable name="content.href"><xsl:value-of select="$query.string"/>&#038;doc.view=content&#038;chunk.id=<xsl:value-of select="$chunk.id"/>&#038;toc.depth=<xsl:value-of select="$toc.depth"/>&#038;brand=<xsl:value-of select="$brand"/>&#038;anchor.id=<xsl:value-of select="$anchor.id"/><xsl:value-of select="$search"/><xsl:call-template name="create.anchor"/></xsl:variable>

  <html>
    <head>
      <title>
	<xsl:value-of select="$doc.title"/>
      </title>
    </head>
    <frameset rows="80,*" border="2" framespacing="2" frameborder="1">
      <frame scrolling="no" title="Navigation Bar">
	<xsl:attribute name="name">bbar</xsl:attribute>
	<xsl:attribute name="src"><xsl:value-of select="$servlet.path"/>?<xsl:value-of select="$bbar.href"/></xsl:attribute>
      </frame>
      <frameset cols="35%,65%" border="2" framespacing="2" frameborder="1">
	<frame title="Table of Contents">
	  <xsl:attribute name="name">toc</xsl:attribute>
	  <xsl:attribute name="src"><xsl:value-of select="$servlet.path"/>?<xsl:value-of select="$toc.href"/></xsl:attribute>
	</frame>
	<frame title="Content">
	  <xsl:attribute name="name">content</xsl:attribute>
          <xsl:attribute name="src"><xsl:value-of select="$servlet.path"/>?<xsl:value-of select="$content.href"/></xsl:attribute>
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
    <xsl:when test="($query != '0' and $query != '') and $set.anchor != '0'">
      <xsl:text>#</xsl:text><xsl:value-of select="$set.anchor"/>
    </xsl:when>
    <xsl:when test="$query != '0' and $query != ''">
      <xsl:text>#</xsl:text><xsl:value-of select="key('div-id', $chunk.id)/@xtf:firstHit"/>
    </xsl:when>
    <xsl:when test="$anchor.id != '0'">
      <xsl:text>#X</xsl:text>
    </xsl:when>
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
        <xsl:call-template name="book.autotoc"/>
      </div>
    </body>
  </html>
</xsl:template>

<!-- ====================================================================== -->
<!-- Content Template                                                       -->
<!-- ====================================================================== -->

<xsl:template name="content">

  <xsl:variable name="navbar">
    <xsl:call-template name="navbar"/>
  </xsl:variable>

  <html>
    <head>
      <title>
        <xsl:value-of select="$doc.title"/> "<xsl:value-of select="$chunk.id"/>"
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
              <xsl:copy-of select="$navbar"/>
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
              <xsl:choose>
                <xsl:when test="$chunk.id = '0'">
                  <xsl:apply-templates select="/TEI.2/text/front/titlePage"/>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:apply-templates select="key('div-id', $chunk.id)"/>          
                </xsl:otherwise>
              </xsl:choose>
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
              <xsl:copy-of select="$navbar"/>
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
              <xsl:apply-templates select="/TEI.2/text"/>
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

  <table width="100%" border="0" cellpadding="0" cellspacing="0">
    
    <xsl:copy-of select="$brand.header.dynaxml.header"/>

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
                    <a href="http://xtf.sourceforge.net" target="{$target}">
                      <img src="{$icon.path}arrow.gif" width="15" height="15" border="0"/>
                    </a>
                  </td>
                  <td nowrap="nowrap">&#160;<a class="topnav" href="http://xtf.sourceforge.net" target="{$target}">Home</a></td>
                  <td width="10" nowrap="nowrap"><img src="{$icon.path}spacer.gif" width="10"/></td>

                  <td width="15" nowrap="nowrap">
                    <a class="topnav" target="{$target}">
                      <xsl:attribute name="href"><xsl:value-of select="$doc.path"/>&#038;doc.view=print&#038;chunk.id=<xsl:value-of select="$chunk.id"/>&#038;toc.depth=<xsl:value-of select="$toc.depth"/>&#038;toc.id=<xsl:value-of select="$toc.id"/><xsl:value-of select="$search"/></xsl:attribute>
                      <img src="{$icon.path}arrow.gif" width="15" height="15" border="0"/>
                    </a>
                  </td>

                  <td nowrap="nowrap">
                    <xsl:text>&#160;</xsl:text>
                    <a class="topnav" target="{$target}">
                      <xsl:attribute name="href"><xsl:value-of select="$doc.path"/>&#038;doc.view=print&#038;chunk.id=<xsl:value-of select="$chunk.id"/>&#038;toc.depth=<xsl:value-of select="$toc.depth"/>&#038;toc.id=<xsl:value-of select="$toc.id"/><xsl:value-of select="$search"/></xsl:attribute>
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
            <form action="{$servlet.path}" target="{$target}" method="GET">
              <input type="hidden" name="docId">
                <xsl:attribute name="value">
                  <xsl:value-of select="$docId"/>
                </xsl:attribute>
              </input>
              <input type="hidden" name="chunk.id">
                <xsl:attribute name="value">
                  <xsl:value-of select="$chunk.id"/>
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
<!-- Popup Window Template                                                  -->
<!-- ====================================================================== -->

<xsl:template name="popup">
  <html>
    <head>
      <title>
        <xsl:choose>
          <xsl:when test="(key('fnote-id', $chunk.id)/@type = 'footnote') or (key('fnote-id', $chunk.id)/@place = 'foot')">
            <xsl:text>Footnote</xsl:text>
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/@type = 'dedication'">
            <xsl:text>Dedication</xsl:text>
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/@type = 'copyright'">
            <xsl:text>Copyright</xsl:text>
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/@type = 'epigraph'">
            <xsl:text>Epigraph</xsl:text>
          </xsl:when>
          <xsl:when test="$fig.ent != '0'">
            <xsl:text>Illustration</xsl:text>
          </xsl:when>
          <xsl:when test="$formula.id != '0'">
            <xsl:text>Formula</xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>popup</xsl:text>
          </xsl:otherwise>
        </xsl:choose>
      </title>
      <xsl:copy-of select="$brand.links"/>
      <link rel="stylesheet" type="text/css" href="{$css.path}{$content.css}"/>
    </head>
    <body>
      <div class="content">
        <xsl:choose>
          <xsl:when test="(key('fnote-id', $chunk.id)/@type = 'footnote') or (key('fnote-id', $chunk.id)/@place = 'foot')">
            <xsl:apply-templates select="key('fnote-id', $chunk.id)"/>  
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/@type = 'dedication'">
            <xsl:apply-templates select="key('div-id', $chunk.id)" mode="titlepage"/>  
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/@type = 'copyright'">
            <xsl:apply-templates select="key('div-id', $chunk.id)" mode="titlepage"/>  
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/@type = 'epigraph'">
            <xsl:apply-templates select="key('div-id', $chunk.id)" mode="titlepage"/>  
          </xsl:when>
          <xsl:when test="$fig.ent != '0'">
            <img src="{$fig.ent}" alt="full-size image"/>        
          </xsl:when>
          <xsl:when test="$formula.id != '0'">
            <div align="center">
              <applet code="HotEqn.class" archive="{$serverURL}applets/HotEqn.jar" height="550" width="550" name="{$formula.id}" align="middle">
                <param name="equation">
                  <xsl:attribute name="value">
                    <xsl:value-of select="key('formula-id', $formula.id)"/>
                  </xsl:attribute>
                </param>
                <param name="fontname" value="TimesRoman"/>
                <param name="bgcolor" value="CCCCCC"/>
                <param name="fgcolor" value="0000ff"/>
                <param name="halign" value="center"/>
                <param name="valign" value="middle"/> 
                <param name="debug" value="true"/>
              </applet>
            </div>
          </xsl:when>
          <xsl:otherwise>
            <xsl:call-template name="custom"/>
          </xsl:otherwise>
        </xsl:choose>
        <p>
          <a>
            <xsl:attribute name="href">javascript://</xsl:attribute>
            <xsl:attribute name="onClick">
              <xsl:text>javascript:window.close('popup')</xsl:text>
            </xsl:attribute>
            <span class="down1">Close this Window</span>
          </a>
        </p>
      </div>
    </body>
  </html>
</xsl:template>

<!-- ====================================================================== -->
<!-- Customization Template                                                 -->
<!-- ====================================================================== -->

<xsl:template name="custom">
  <!-- Dead Template -->
</xsl:template>

<!-- ====================================================================== -->
<!-- Navigation Bar Template                                                -->
<!-- ====================================================================== -->

<xsl:template name="navbar">

  <xsl:variable name="target">
    <xsl:text>_top</xsl:text>
  </xsl:variable>

  <xsl:variable name="div" select="name(key('div-id', $chunk.id))"/>

  <xsl:variable name="prev">
    <xsl:choose>
      <xsl:when test="$div = 'div1'">
        <xsl:choose>
          <xsl:when test="key('div-id', $chunk.id)/preceding-sibling::div1[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/preceding-sibling::div1[head][@id][1]/@id"/>
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/parent::front">
            <xsl:value-of select="0"/>
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/parent::body/preceding-sibling::front/div1[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/parent::body/preceding-sibling::front/div1[head][@id][position()=last()]/@id"/>
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/parent::back/preceding-sibling::body/div1[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/parent::back/preceding-sibling::body/div1[head][@id][position()=last()]/@id"/>
          </xsl:when>
          <xsl:otherwise>0</xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:when test="$div = 'div2'">
        <xsl:choose>
          <xsl:when test="key('div-id', $chunk.id)/preceding-sibling::div2[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/preceding-sibling::div2[head][@id][1]/@id"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="key('div-id', $chunk.id)/parent::div1[head][@id][1]/@id"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:when test="$div = 'div3'">
        <xsl:choose>
          <xsl:when test="key('div-id', $chunk.id)/preceding-sibling::div3[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/preceding-sibling::div3[head][@id][1]/@id"/>            
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="key('div-id', $chunk.id)/parent::div2[head][@id][1]/@id"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:when test="$div = 'div4'">
        <xsl:choose>
          <xsl:when test="key('div-id', $chunk.id)/preceding-sibling::div4[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/preceding-sibling::div4[head][@id][1]/@id"/>            
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="key('div-id', $chunk.id)/parent::div3[head][@id][1]/@id"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:when test="$div = 'div5'">
        <xsl:choose>
          <xsl:when test="key('div-id', $chunk.id)/preceding-sibling::div5[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/preceding-sibling::div5[head][@id][1]/@id"/>            
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="key('div-id', $chunk.id)/parent::div4[head][@id][1]/@id"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:when test="$div = 'div6'">
        <xsl:choose>
          <xsl:when test="key('div-id', $chunk.id)/preceding-sibling::div6[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/preceding-sibling::div6[head][@id][1]/@id"/>            
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="key('div-id', $chunk.id)/parent::div5[head][@id][1]/@id"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:when test="$div = 'div7'">
        <xsl:choose>
          <xsl:when test="key('div-id', $chunk.id)/preceding-sibling::div7[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/preceding-sibling::div7[head][@id][1]/@id"/>            
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="key('div-id', $chunk.id)/parent::div6[head][@id][1]/@id"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>0</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="prev_toc">
    <xsl:choose>
      <xsl:when test="key('div-id', $prev)/*[head][@id]">
        <xsl:value-of select="key('div-id', $prev)/@id"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="key('div-id', $prev)/parent::*[head][@id]/@id"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="prev_type">
    <xsl:choose>
      <xsl:when test="contains(key('div-id', $prev)/@type, 'volume')">
        <xsl:text>volume</xsl:text>
      </xsl:when>
      <xsl:when test="contains(key('div-id', $prev)/@type, 'part')">
        <xsl:text>part</xsl:text>
      </xsl:when>
      <xsl:when test="contains(key('div-id', $prev)/@type, 'chapter')">
        <xsl:text>chapter</xsl:text>
      </xsl:when>
      <xsl:when test="contains(key('div-id', $prev)/@type, 'ss')">
        <xsl:text>sub-section</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>section</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="prev_text">
    <xsl:choose>
      <xsl:when test="$prev != '0'">
        <xsl:text>previous </xsl:text><xsl:value-of select="$prev_type"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>no previous</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="next">
    <xsl:choose>
      <xsl:when test="$div = 'div1'">
        <xsl:choose>
          <xsl:when test="key('div-id', $chunk.id)/following-sibling::div1[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/following-sibling::div1[head][@id][1]/@id"/>
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/parent::front/following-sibling::body/div1[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/parent::front/following-sibling::body/div1[head][@id]/@id"/>
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/parent::body/following-sibling::back/div1[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/parent::body/following-sibling::back/div1[head][@id]/@id"/>
          </xsl:when>
          <xsl:otherwise>0</xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:when test="$div = 'div2'">
        <xsl:choose>
          <xsl:when test="key('div-id', $chunk.id)/following-sibling::div2[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/following-sibling::div2[head][@id][1]/@id"/>
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/parent::div1/following-sibling::div1[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/parent::div1/following-sibling::div1[head][@id][1]/@id"/>
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/ancestor::front/following-sibling::body/div1[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/ancestor::front/following-sibling::body/div1[head][@id]/@id"/>
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/ancestor::body/following-sibling::back/div1[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/ancestor::body/following-sibling::back/div1[head][@id]/@id"/>
          </xsl:when>
          <xsl:otherwise>0</xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:when test="$div = 'div3'">
        <xsl:choose>
          <xsl:when test="key('div-id', $chunk.id)/following-sibling::div3[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/following-sibling::div3[head][@id][1]/@id"/>            
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/parent::div2/following-sibling::div2[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/parent::div2/following-sibling::div2[head][@id][1]/@id"/>
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/ancestor::div1/following-sibling::div1[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/ancestor::div1/following-sibling::div1[head][@id][1]/@id"/>
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/ancestor::front/following-sibling::body/div1[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/ancestor::front/following-sibling::body/div1[head][@id]/@id"/>
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/ancestor::body/following-sibling::back/div1[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/ancestor::body/following-sibling::back/div1[head][@id]/@id"/>
          </xsl:when>
          <xsl:otherwise>0</xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:when test="$div = 'div4'">
        <xsl:choose>
          <xsl:when test="key('div-id', $chunk.id)/following-sibling::div4[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/following-sibling::div4[head][@id][1]/@id"/>            
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/parent::div3/following-sibling::div3[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/parent::div3/following-sibling::div3[head][@id][1]/@id"/>
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/ancestor::div2/following-sibling::div2[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/ancestor::div2/following-sibling::div2[head][@id][1]/@id"/>
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/ancestor::div1/following-sibling::div1[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/ancestor::div1/following-sibling::div1[head][@id][1]/@id"/>            
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/ancestor::front/following-sibling::body/div1[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/ancestor::front/following-sibling::body/div1[head][@id]/@id"/>
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/ancestor::body/following-sibling::back/div1[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/ancestor::body/following-sibling::back/div1[head][@id]/@id"/>
          </xsl:when>
          <xsl:otherwise>0</xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:when test="$div = 'div5'">
        <xsl:choose>
          <xsl:when test="key('div-id', $chunk.id)/following-sibling::div5[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/following-sibling::div5[head][@id][1]/@id"/>            
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/parent::div4/following-sibling::div4[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/parent::div4/following-sibling::div4[head][@id][1]/@id"/>
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/ancestor::div3/following-sibling::div3[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/ancestor::div3/following-sibling::div3[head][@id][1]/@id"/>
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/ancestor::div2/following-sibling::div2[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/ancestor::div2/following-sibling::div2[head][@id][1]/@id"/>
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/ancestor::div1/following-sibling::div1[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/ancestor::div1/following-sibling::div1[head][@id][1]/@id"/>            
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/ancestor::front/following-sibling::body/div1[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/ancestor::front/following-sibling::body/div1[head][@id]/@id"/>
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/ancestor::body/following-sibling::back/div1[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/ancestor::body/following-sibling::back/div1[head][@id]/@id"/>
          </xsl:when>
          <xsl:otherwise>0</xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:when test="$div = 'div6'">
        <xsl:choose>
          <xsl:when test="key('div-id', $chunk.id)/following-sibling::div6[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/following-sibling::div6[head][@id][1]/@id"/>            
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/parent::div5/following-sibling::div5[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/parent::div5/following-sibling::div5[head][@id][1]/@id"/>
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/ancestor::div4/following-sibling::div4[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/ancestor::div4/following-sibling::div4[head][@id][1]/@id"/>
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/ancestor::div3/following-sibling::div3[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/ancestor::div3/following-sibling::div3[head][@id][1]/@id"/>
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/ancestor::div2/following-sibling::div2[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/ancestor::div2/following-sibling::div2[head][@id][1]/@id"/>
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/ancestor::div1/following-sibling::div1[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/ancestor::div1/following-sibling::div1[head][@id][1]/@id"/>            
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/ancestor::front/following-sibling::body/div1[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/ancestor::front/following-sibling::body/div1[head][@id]/@id"/>
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/ancestor::body/following-sibling::back/div1[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/ancestor::body/following-sibling::back/div1[head][@id]/@id"/>
          </xsl:when>
          <xsl:otherwise>0</xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:when test="$div = 'div7'">
        <xsl:choose>
          <xsl:when test="key('div-id', $chunk.id)/following-sibling::div7[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/following-sibling::div7[head][@id][1]/@id"/>            
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/parent::div6/following-sibling::div6[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/parent::div6/following-sibling::div6[head][@id][1]/@id"/>
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/ancestor::div5/following-sibling::div5[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/ancestor::div5/following-sibling::div5[head][@id][1]/@id"/>
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/ancestor::div4/following-sibling::div4[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/ancestor::div4/following-sibling::div4[head][@id][1]/@id"/>
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/ancestor::div3/following-sibling::div3[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/ancestor::div3/following-sibling::div3[head][@id][1]/@id"/>
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/ancestor::div2/following-sibling::div2[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/ancestor::div2/following-sibling::div2[head][@id][1]/@id"/>
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/ancestor::div1/following-sibling::div1[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/ancestor::div1/following-sibling::div1[head][@id][1]/@id"/>            
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/ancestor::front/following-sibling::body/div1[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/ancestor::front/following-sibling::body/div1[head][@id]/@id"/>
          </xsl:when>
          <xsl:when test="key('div-id', $chunk.id)/ancestor::body/following-sibling::back/div1[head][@id]">
            <xsl:value-of select="key('div-id', $chunk.id)/ancestor::body/following-sibling::back/div1[head][@id]/@id"/>
          </xsl:when>
          <xsl:otherwise>0</xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <!-- Gives you a value for 'next' when you are not in a <divn>, which as far as I can tell only occurs when you are displaying the titlepage -->
      <xsl:otherwise>
        <xsl:choose>
          <xsl:when test="/TEI.2/text/front/div1[head][@id]">
            <xsl:value-of select="/TEI.2/text/front/div1[head][@id][1]/@id"/>            
          </xsl:when>
          <xsl:when test="/TEI.2/text/body/div1[head][@id]">
            <xsl:value-of select="/TEI.2/text/body/div1[head][@id][1]/@id"/>            
          </xsl:when>
          <xsl:otherwise>0</xsl:otherwise>
        </xsl:choose>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="next_toc">
    <xsl:choose>
      <xsl:when test="key('div-id', $next)/*[head][@id]">
        <xsl:value-of select="key('div-id', $next)/@id"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="key('div-id', $next)/parent::*[head][@id]/@id"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="next_type">
    <xsl:choose>
      <xsl:when test="contains(key('div-id', $next)/@type, 'volume')">
        <xsl:text>volume</xsl:text>
      </xsl:when>
      <xsl:when test="contains(key('div-id', $next)/@type, 'part')">
        <xsl:text>part</xsl:text>
      </xsl:when>
      <xsl:when test="contains(key('div-id', $next)/@type, 'chapter')">
        <xsl:text>chapter</xsl:text>
      </xsl:when>
      <xsl:when test="contains(key('div-id', $next)/@type, 'ss')">
        <xsl:text>sub-section</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>section</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="next_text">
    <xsl:choose>
      <xsl:when test="$next != '0'">
        <xsl:text>next </xsl:text><xsl:value-of select="$next_type"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>no next</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

          <tr>
            <td width="25%" align="left">
              <!-- BEGIN PREVIOUS SELECTION TABLE -->
              <table border="0" cellpadding="0" cellspacing="0">
                <tr>
                  <td width="15" align="left" valign="middle">
                    <a>
                      <xsl:choose>
                        <xsl:when test="$prev != '0'">
                          <xsl:attribute name="href">
                            <xsl:value-of select="$doc.path"/>
                            <xsl:text>&#038;chunk.id=</xsl:text>
                            <xsl:value-of select="$prev"/>
                            <xsl:text>&#038;toc.id=</xsl:text>
                            <xsl:value-of select="$prev_toc"/>
                            <xsl:text>&#038;brand=</xsl:text>
                            <xsl:value-of select="$brand"/>
                            <xsl:value-of select="$search"/>
                          </xsl:attribute>
                          <xsl:attribute name="target"><xsl:value-of select="$target"/></xsl:attribute>
                          <img src="{$icon.path}b_prev.gif" width="15" height="15" border="0" alt="{$prev_text}"/>
                        </xsl:when>
                        <xsl:otherwise>
                          <img src="{$icon.path}d_prev.gif" width="15" height="15" border="0" alt="{$prev_text}"/>
                        </xsl:otherwise>
                      </xsl:choose>
                    </a>
                  </td>
                  <td width="8" nowrap="nowrap">
                    <img src="{$icon.path}spacer.gif" width="8"/>
                  </td>
                  <td align="left" valign="middle">
                    <xsl:if test="$prev != '0'">
                      <a class="midnav">
                        <xsl:attribute name="href">
                          <xsl:value-of select="$doc.path"/>
                          <xsl:text>&#038;chunk.id=</xsl:text>
                          <xsl:value-of select="$prev"/>
                          <xsl:text>&#038;toc.id=</xsl:text>
                          <xsl:value-of select="$prev_toc"/>
                          <xsl:text>&#038;brand=</xsl:text>
                          <xsl:value-of select="$brand"/>
                          <xsl:value-of select="$search"/>
                        </xsl:attribute>
                        <xsl:attribute name="target"><xsl:value-of select="$target"/></xsl:attribute>
                        <xsl:if test="$doc.view = '0'">
                          <xsl:value-of select="$prev_text"/>
                        </xsl:if>
                      </a>
                    </xsl:if>
                  </td>
                </tr>
              </table>
              <!-- END PREVIOUS SELECTION TABLE -->
            </td>
            <td width="50%" align="center">
              <span class="chapter-text">
                <xsl:value-of select="key('div-id', $chunk.id)/ancestor-or-self::*[@type='fmsec' or @type='volume' or @type='part' or @type='chapter' or @type='bmsec'][1]/head[1]"/>
              </span>
            </td>
            <td width="25%" align="right">
              <!-- BEGIN NEXT SELECTION TABLE -->
              <table border="0" cellpadding="0" cellspacing="0">
                <tr>
                  <td align="right" valign="middle">
                    <xsl:if test="$next != '0'">
                      <a class="midnav">
                        <xsl:attribute name="href">
                          <xsl:value-of select="$doc.path"/>
                          <xsl:text>&#038;chunk.id=</xsl:text>
                          <xsl:value-of select="$next"/>
                          <xsl:text>&#038;toc.id=</xsl:text>
                          <xsl:value-of select="$next_toc"/>
                          <xsl:text>&#038;brand=</xsl:text>
                          <xsl:value-of select="$brand"/>
                          <xsl:value-of select="$search"/>
                        </xsl:attribute>
                        <xsl:attribute name="target"><xsl:value-of select="$target"/></xsl:attribute>
                        <xsl:if test="$doc.view = '0'">
                          <xsl:value-of select="$next_text"/>
                        </xsl:if>
                      </a>
                    </xsl:if>
                    </td>
                  <td width="8" nowrap="nowrap">
                    <img src="{$icon.path}spacer.gif" width="8"/>
                  </td>
                  <td width="15" align="right" valign="middle">
                    <a>
                      <xsl:choose>
                        <xsl:when test="$next != '0'">
                          <xsl:attribute name="href">
                            <xsl:value-of select="$doc.path"/>
                            <xsl:text>&#038;chunk.id=</xsl:text>
                            <xsl:value-of select="$next"/>
                            <xsl:text>&#038;toc.id=</xsl:text>
                            <xsl:value-of select="$next_toc"/>
                            <xsl:text>&#038;brand=</xsl:text>
                            <xsl:value-of select="$brand"/>
                            <xsl:value-of select="$search"/>
                          </xsl:attribute>
                          <xsl:attribute name="target"><xsl:value-of select="$target"/></xsl:attribute>
                          <img src="{$icon.path}b_next.gif" width="15" height="15" border="0" alt="{$next_text}"/>
                        </xsl:when>
                        <xsl:otherwise>
                          <img src="{$icon.path}d_next.gif" width="15" height="15" border="0" alt="{$next_text}"/>
                        </xsl:otherwise>
                      </xsl:choose>
                    </a>
                  </td>
                </tr>
              </table>
              <!-- END NEXT SELECTION TABLE -->
            </td>
          </tr>

</xsl:template>

</xsl:stylesheet>
