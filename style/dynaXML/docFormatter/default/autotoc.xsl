<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                              xmlns:xtf="http://cdlib.org/xtf">

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

<xsl:template name="book.autotoc">

  <xsl:variable name="view">
    <xsl:choose>
      <xsl:when test="$doc.view='frames' or $doc.view='bbar' or $doc.view='toc' or $doc.view='content'">frames</xsl:when>
      <xsl:otherwise>0</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="sum">
    <xsl:choose>
      <xsl:when test="($query != '0') and ($query != '')">
        <xsl:value-of select="/TEI.2/@xtf:hitCount"/>
      </xsl:when>
      <xsl:otherwise>0</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="occur">
    <xsl:choose>
      <xsl:when test="$sum != 1">occurences</xsl:when>
      <xsl:otherwise>occurence</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

    <xsl:choose>
      <xsl:when test="$doc.view='0'">
        <table width="100%" border="0" cellpadding="0" cellspacing="0">
          <!-- BEGIN TITLE ROW -->
          <tr width="100%">
            <td width="100%" valign="top">
              <div class="title">
                <xsl:value-of select="$doc.author"/><br/>
                <a href="http://ark.cdlib.org/{/TEI.2/teiHeader/fileDesc/publicationStmt/idno[@type='ARK']}">
                  <strong><xsl:value-of select="$doc.title"/></strong><br/>
                </a>
                <em><xsl:value-of select="$doc.subtitle"/></em><br/>
                <xsl:if test="contains($brand, 'ucpress')">
                  <a href="{$serverURL}cgi-bin/nph-ucpgo.pl?http://www.ucpress.edu/books/pages/{$bnum}.html">
                    <img class="buy" src="{$icon.path}buy_this_book.gif" width="92" height="15" border="0" alt="Buy This Book"/>
                  </a>
                </xsl:if>
              </div>
            </td>
          </tr>
        </table>
      </xsl:when>
      <xsl:otherwise>
        <table>
          <tr>
            <td height="25">
              <b>
                <a class="booktitle" href="http://ark.cdlib.org/{/TEI.2/teiHeader/fileDesc/publicationStmt/idno[@type='ARK']}">
                  <xsl:attribute name="target">_top</xsl:attribute>
                  <xsl:value-of select="/TEI.2/text/front/titlePage/titlePart[@type='main']"/>
                </a>
              </b>
            </td>
          </tr>
        </table>
      </xsl:otherwise>
    </xsl:choose>

    <xsl:if test="($query != '0') and ($query != '')">
      <hr/>
      <div align="center">
        <b><span class="hit-count"><xsl:value-of select="$sum"/></span><xsl:text> </xsl:text><xsl:value-of select="$occur"/><xsl:text> of </xsl:text><span class="hit-count"><xsl:value-of select="$query"/></span></b><br/>
        <xsl:text> [</xsl:text>
        <a>
          <xsl:attribute name="href"><xsl:value-of select="$doc.path"/>&#038;doc.view=<xsl:value-of select="$view"/>&#038;chunk.id=<xsl:value-of select="$chunk.id"/>&#038;toc.depth=<xsl:value-of select="$toc.depth"/>&#038;toc.id=<xsl:value-of select="$toc.id"/></xsl:attribute>
          <xsl:attribute name="target">_top</xsl:attribute>
          <xsl:text>Clear Hits</xsl:text>
        </a>
        <xsl:text>]</xsl:text>
      </div>
    </xsl:if>
    <hr/>

    <xsl:apply-templates select="/TEI.2/text/front/div1" mode="toc"/>
    <br/>
    <xsl:apply-templates select="/TEI.2/text/body/div1" mode="toc"/>
    <br/>
    <xsl:apply-templates select="/TEI.2/text/back/div1" mode="toc"/>

    <xsl:if test="($query != '0') and ($query != '')">
      <hr/>
      <div align="center">
        <b><span class="hit-count"><xsl:value-of select="$sum"/></span><xsl:text> </xsl:text><xsl:value-of select="$occur"/><xsl:text> of </xsl:text><span class="hit-count"><xsl:value-of select="$query"/></span></b><br/>
        <xsl:text> [</xsl:text>
        <a>
          <xsl:attribute name="href"><xsl:value-of select="$doc.path"/>&#038;doc.view=<xsl:value-of select="$view"/>&#038;chunk.id=<xsl:value-of select="$chunk.id"/>&#038;toc.depth=<xsl:value-of select="$toc.depth"/>&#038;toc.id=<xsl:value-of select="$toc.id"/></xsl:attribute>
          <xsl:attribute name="target">_top</xsl:attribute>
          <xsl:text>Clear Hits</xsl:text>
        </a>
        <xsl:text>]</xsl:text>
      </div>
    </xsl:if>
    <hr/>
    <div align="center">
      <span class="expand-collapse">
        <a class="expand-collapse">
          <xsl:attribute name="href">
            <xsl:value-of select="$doc.path"/>&#038;doc.view=<xsl:value-of select="$view"/>&#038;chunk.id=<xsl:value-of select="$chunk.id"/>&#038;toc.depth=<xsl:value-of select="1"/><xsl:value-of select="$search"/>
          </xsl:attribute>
          <xsl:attribute name="target">_top</xsl:attribute>
          <img src="{$icon.path}i_colpse.gif" border="0" alt="collapse section"/>
          <xsl:text> Collapse All</xsl:text>
        </a>
        <xsl:text> | </xsl:text>
        <a class="expand-collapse">
          <xsl:attribute name="href">
            <xsl:value-of select="$doc.path"/>&#038;doc.view=<xsl:value-of select="$view"/>&#038;chunk.id=<xsl:value-of select="$chunk.id"/>&#038;toc.depth=<xsl:value-of select="100"/><xsl:value-of select="$search"/>
          </xsl:attribute>
          <xsl:attribute name="target">_top</xsl:attribute>
          <xsl:text>Expand All </xsl:text>
          <img src="{$icon.path}i_expand.gif" border="0" alt="expand section"/>
        </a>
      </span>
    </div>

</xsl:template>

<xsl:template match="div1" mode="toc">

  <xsl:variable name="head" select="head"/>

  <xsl:variable name="hit.count">
    <xsl:choose>
      <xsl:when test="($query != '0') and ($query != '') and (@xtf:hitCount)">
        <xsl:value-of select="@xtf:hitCount"/>
      </xsl:when>
      <xsl:otherwise>0</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:if test="$head">
    <xsl:if test="(@type='volume' or @type='part') and (preceding-sibling::*)">
      <br/>
    </xsl:if>
    <table border="0" cellpadding="0" cellspacing="0" width="820">
      <tr>
        <xsl:choose>
          <xsl:when test="($query != '0') and ($query != '')">
            <xsl:choose>
              <xsl:when test="$hit.count != '0'">
                <td width="20" align="right">
                  <span class="hit-count">
                    <xsl:value-of select="$hit.count"/>
                  </span>
                </td>
              </xsl:when>
              <xsl:otherwise>
                <td width="20">&#160;</td>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:when>
          <xsl:otherwise>
	    <td width="1">&#160;</td>
	  </xsl:otherwise>
        </xsl:choose>
	<xsl:choose>
          <xsl:when test="($toc.depth &lt; 2 and div2/head) and (not(@id = key('div-id', $toc.id)/ancestor-or-self::*/@id)) and (not(@type='volume' or @type='part'))">
	    <td width="15" align="center">
              <xsl:call-template name="expand"/>
	    </td>
	  </xsl:when>
	  <xsl:when test="($toc.depth > 1 and div2/head) or (@id = key('div-id', $toc.id)/ancestor-or-self::*/@id) or (@type='volume' or @type='part')">
	    <td width="15" align="center">
	      <xsl:call-template name="collapse"/>
	    </td>
	  </xsl:when>
	  <xsl:otherwise>
	    <td width="15">&#160;</td>
	  </xsl:otherwise>
	</xsl:choose>
        <td align="left" valign="top" width="700">
          <nobr><xsl:apply-templates select="head[1]" mode="toc"/></nobr>
	</td>
      </tr>
    </table>
    <xsl:if test="($toc.depth > 1 and div2/head) or (@id = key('div-id', $toc.id)/ancestor-or-self::*/@id) or (@type='volume' or @type='part')">
      <xsl:apply-templates select="div2" mode="toc"/>
    </xsl:if>
  </xsl:if>
</xsl:template>

<xsl:template match="div2" mode="toc">

  <xsl:variable name="head" select="head"/>

  <xsl:variable name="hit.count">
    <xsl:choose>
      <xsl:when test="($query != '0') and ($query != '') and (@xtf:hitCount)">
        <xsl:value-of select="@xtf:hitCount"/>
      </xsl:when>
      <xsl:otherwise>0</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:if test="$head">
    <xsl:if test="@type='volume' or @type='part'">
      <br/>
    </xsl:if>
    <table border="0" cellpadding="0" cellspacing="0" width="820">
      <tr>
        <xsl:choose>
          <xsl:when test="($query != '0') and ($query != '')">
            <xsl:choose>
              <xsl:when test="$hit.count != '0'">
                <td width="40" align="right">
                  <span class="hit-count">
                    <xsl:value-of select="$hit.count"/>
                  </span>
                </td>
              </xsl:when>
              <xsl:otherwise>
                <td width="40">&#160;</td>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:when>
          <xsl:otherwise>
	    <td width="20">&#160;</td>
	  </xsl:otherwise>
        </xsl:choose>
	<xsl:choose>
	  <xsl:when test="($toc.depth &lt; 3 and div3/head) and (not(@id = key('div-id', $toc.id)/ancestor-or-self::*/@id)) and (not(@type='volume' or @type='part'))">
	    <td width="15" align="center">
	      <xsl:call-template name="expand"/>
	    </td>
	  </xsl:when>
	  <xsl:when test="($toc.depth > 2 and div3/head) or (@id = key('div-id', $toc.id)/ancestor-or-self::*/@id) or (@type='volume' or @type='part')">
	    <td width="15" align="center">
	      <xsl:call-template name="collapse"/>
	    </td>
	  </xsl:when>
	  <xsl:otherwise>
	    <td width="15" align="center">&#x2022;</td>
	  </xsl:otherwise>
	</xsl:choose>
	<td align="left" valign="top" width="700">
	  <nobr><xsl:apply-templates select="head[1]" mode="toc"/></nobr>
	</td>
      </tr>
    </table>
    <xsl:if test="($toc.depth > 2 and div3/head) or (@id = key('div-id', $toc.id)/ancestor-or-self::*/@id) or (@type='volume' or @type='part')">
      <xsl:apply-templates select="div3" mode="toc"/>
    </xsl:if>
  </xsl:if>
</xsl:template>

<xsl:template match="div3" mode="toc">

  <xsl:variable name="head" select="head"/>

  <xsl:variable name="hit.count">
    <xsl:choose>
      <xsl:when test="($query != '0') and ($query != '') and (@xtf:hitCount)">
        <xsl:value-of select="@xtf:hitCount"/>
      </xsl:when>
      <xsl:otherwise>0</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:if test="$head">
    <table border="0" cellpadding="0" cellspacing="0" width="820">
      <tr>
        <xsl:choose>
          <xsl:when test="($query != '0') and ($query != '')">
            <xsl:choose>
              <xsl:when test="$hit.count != '0'">
                <td width="60" align="right">
                  <span class="hit-count">
                    <xsl:value-of select="$hit.count"/>
                  </span>
                </td>
              </xsl:when>
              <xsl:otherwise>
                <td width="60">&#160;</td>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:when>
          <xsl:otherwise>
	    <td width="40">&#160;</td>
	  </xsl:otherwise>
        </xsl:choose>
	<xsl:choose>
	  <xsl:when test="($toc.depth &lt; 4 and div4/head) and (not(@id = key('div-id', $toc.id)/ancestor-or-self::*/@id))">
	    <td width="15" align="center">
	      <xsl:call-template name="expand"/>
	    </td>
	  </xsl:when>
	  <xsl:when test="($toc.depth > 3 and div4/head) or (@id = key('div-id', $toc.id)/ancestor-or-self::*/@id)">
	    <td width="15" align="center">
	      <xsl:call-template name="collapse"/>
	    </td>
	  </xsl:when>
	  <xsl:otherwise>
	    <td width="15" align="center">&#x2022;</td>
	  </xsl:otherwise>
	</xsl:choose>
	<td align="left" valign="top" width="700">
	  <nobr><xsl:apply-templates select="head[1]" mode="toc"/></nobr>
	</td>
      </tr>
    </table>
    <xsl:if test="($toc.depth > 3 and div4/head) or (@id = key('div-id', $toc.id)/ancestor-or-self::*/@id)">
      <xsl:apply-templates select="div4" mode="toc"/>
    </xsl:if>
  </xsl:if>
</xsl:template>

<xsl:template match="div4" mode="toc">

  <xsl:variable name="head" select="head"/>

  <xsl:variable name="hit.count">
    <xsl:choose>
      <xsl:when test="($query != '0') and ($query != '') and (@xtf:hitCount)">
        <xsl:value-of select="@xtf:hitCount"/>
      </xsl:when>
      <xsl:otherwise>0</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:if test="$head">
    <table border="0" cellpadding="0" cellspacing="0" width="820">
      <tr>
        <xsl:choose>
          <xsl:when test="($query != '0') and ($query != '')">
            <xsl:choose>
              <xsl:when test="$hit.count != '0'">
                <td width="80" align="right">
                  <span class="hit-count">
                    <xsl:value-of select="$hit.count"/>
                  </span>
                </td>
              </xsl:when>
              <xsl:otherwise>
                <td width="80">&#160;</td>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:when>
          <xsl:otherwise>
	    <td width="60">&#160;</td>
	  </xsl:otherwise>
        </xsl:choose>
	<xsl:choose>
	  <xsl:when test="($toc.depth &lt; 5 and div5/head) and (not(@id = key('div-id', $toc.id)/ancestor-or-self::*/@id))">
	    <td width="15" align="center">
	      <xsl:call-template name="expand"/>
	    </td>
	  </xsl:when>
	  <xsl:when test="($toc.depth > 4 and div5/head) or (@id = key('div-id', $toc.id)/ancestor-or-self::*/@id)">
	    <td width="15" align="center">
	      <xsl:call-template name="collapse"/>
	    </td>
	  </xsl:when>
	  <xsl:otherwise>
	    <td width="15" align="center">&#x2022;</td>
	  </xsl:otherwise>
	</xsl:choose>
	<td align="left" valign="top" width="700">
	  <nobr><xsl:apply-templates select="head[1]" mode="toc"/></nobr>
	</td>
      </tr>
    </table>
    <xsl:if test="($toc.depth > 4 and div5/head) or (@id = key('div-id', $toc.id)/ancestor-or-self::*/@id)">
      <xsl:apply-templates select="div5" mode="toc"/>
    </xsl:if>
  </xsl:if>
</xsl:template>

<xsl:template match="div5" mode="toc">

  <xsl:variable name="head" select="head"/>

  <xsl:variable name="hit.count">
    <xsl:choose>
      <xsl:when test="($query != '0') and ($query != '') and (@xtf:hitCount)">
        <xsl:value-of select="@xtf:hitCount"/>
      </xsl:when>
      <xsl:otherwise>0</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:if test="$head">
    <table border="0" cellpadding="0" cellspacing="0" width="820">
      <tr>
        <xsl:choose>
          <xsl:when test="($query != '0') and ($query != '')">
            <xsl:choose>
              <xsl:when test="$hit.count != '0'">
                <td width="100" align="right">
                  <span class="hit-count">
                    <xsl:value-of select="$hit.count"/>
                  </span>
                </td>
              </xsl:when>
              <xsl:otherwise>
                <td width="100">&#160;</td>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:when>
          <xsl:otherwise>
	    <td width="80">&#160;</td>
	  </xsl:otherwise>
        </xsl:choose>
	<xsl:choose>
	  <xsl:when test="($toc.depth &lt; 6 and div6/head) and (not(@id = key('div-id', $toc.id)/ancestor-or-self::*/@id))">
	    <td width="15" align="center">
	      <xsl:call-template name="expand"/>
	    </td>
	  </xsl:when>
	  <xsl:when test="($toc.depth > 5 and div6/head) or (@id = key('div-id', $toc.id)/ancestor-or-self::*/@id)">
	    <td width="15" align="center">
	      <xsl:call-template name="collapse"/>
	    </td>
	  </xsl:when>
	  <xsl:otherwise>
	    <td width="15" align="center">&#x2022;</td>
	  </xsl:otherwise>
	</xsl:choose>
	<td align="left" valign="top" width="700">
	  <nobr><xsl:apply-templates select="head[1]" mode="toc"/></nobr>
	</td>
      </tr>
    </table>
    <xsl:if test="($toc.depth > 5 and div6/head) or (@id = key('div-id', $toc.id)/ancestor-or-self::*/@id)">
      <xsl:apply-templates select="div6" mode="toc"/>
    </xsl:if>
  </xsl:if>
</xsl:template>

<xsl:template match="div6" mode="toc">

  <xsl:variable name="head" select="head"/>

  <xsl:variable name="hit.count">
    <xsl:choose>
      <xsl:when test="($query != '0') and ($query != '') and (@xtf:hitCount)">
        <xsl:value-of select="@xtf:hitCount"/>
      </xsl:when>
      <xsl:otherwise>0</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:if test="$head">
    <table border="0" cellpadding="0" cellspacing="0" width="820">
      <tr>
        <xsl:choose>
          <xsl:when test="($query != '0') and ($query != '')">
            <xsl:choose>
              <xsl:when test="$hit.count != '0'">
                <td width="100" align="right">
                  <span class="hit-count">
                    <xsl:value-of select="$hit.count"/>
                  </span>
                </td>
              </xsl:when>
              <xsl:otherwise>
                <td width="100">&#160;</td>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:when>
          <xsl:otherwise>
	    <td width="80">&#160;</td>
	  </xsl:otherwise>
        </xsl:choose>
	<xsl:choose>
	  <xsl:when test="($toc.depth &lt; 7 and div7/head) and (not(@id = key('div-id', $toc.id)/ancestor-or-self::*/@id))">
	    <td width="15" align="center">
	      <xsl:call-template name="expand"/>
	    </td>
	  </xsl:when>
	  <xsl:when test="($toc.depth > 6 and div7/head) or (@id = key('div-id', $toc.id)/ancestor-or-self::*/@id)">
	    <td width="15" align="center">
	      <xsl:call-template name="collapse"/>
	    </td>
	  </xsl:when>
	  <xsl:otherwise>
	    <td width="15" align="center">&#x2022;</td>
	  </xsl:otherwise>
	</xsl:choose>
	<td align="left" valign="top" width="700">
	  <nobr><xsl:apply-templates select="head[1]" mode="toc"/></nobr>
	</td>
      </tr>
    </table>
    <xsl:if test="($toc.depth > 6 and div7/head) or (@id = key('div-id', $toc.id)/ancestor-or-self::*/@id)">
      <xsl:apply-templates select="div7" mode="toc"/>
    </xsl:if>
  </xsl:if>
</xsl:template>

<xsl:template match="div7" mode="toc">

  <xsl:variable name="head" select="head"/>

  <xsl:variable name="hit.count">
    <xsl:choose>
      <xsl:when test="($query != '0') and ($query != '') and (@xtf:hitCount)">
        <xsl:value-of select="@xtf:hitCount"/>
      </xsl:when>
      <xsl:otherwise>0</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:if test="$head">
    <table border="0" cellpadding="0" cellspacing="0" width="820">
      <tr>
        <xsl:choose>
          <xsl:when test="($query != '0') and ($query != '')">
            <xsl:choose>
              <xsl:when test="$hit.count != '0'">
                <td width="120" align="right">
                  <span class="hit-count">
                    <xsl:value-of select="$hit.count"/>
                  </span>
                </td>
              </xsl:when>
              <xsl:otherwise>
                <td width="120">&#160;</td>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:when>
          <xsl:otherwise>
	    <td width="100">&#160;</td>
	  </xsl:otherwise>
        </xsl:choose>
	<td width="15" align="center">&#x2022;</td>
	<td align="left" valign="top" width="700">
	  <nobr><xsl:apply-templates select="head[1]" mode="toc"/></nobr>
	</td>
      </tr>
    </table>
  </xsl:if>
</xsl:template>

<xsl:template match="head" mode="toc">

  <xsl:variable name="view">
    <xsl:choose>
      <xsl:when test="$doc.view='frames' or $doc.view='bbar' or $doc.view='toc' or $doc.view='content'">frames</xsl:when>
      <xsl:otherwise>0</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <!-- Mechanism by which the proper TOC branch is expanded -->
  <!-- Remember we are processing the head element, so we have to check the parent's status as a node. -->
  <xsl:variable name="local.toc.id">
    <xsl:choose>
      <!-- If this node is not terminal, expand this node -->
      <xsl:when test="parent::*[self::div1 or self::div2 or self::div3 or self::div4 or self::div5 or self::div6]/*[self::div1 or self::div2 or self::div3 or self::div4 or self::div5 or self::div6 or self::div7]">
        <xsl:value-of select="parent::*[self::div1 or self::div2 or self::div3 or self::div4 or self::div5 or self::div6 or self::div7]/@id"/>
      </xsl:when>
      <!-- If this node is terminal, expand the parent node -->
      <xsl:otherwise>
         <xsl:value-of select="parent::*[self::div1 or self::div2 or self::div3 or self::div4 or self::div5 or self::div6 or self::div7]/parent::*[self::div1 or self::div2 or self::div3 or self::div4 or self::div5 or self::div6]/@id"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:choose>
    <xsl:when test="$chunk.id=ancestor::*[1]/@id">
      <a name="X"></a>
      <span class="toc-hi">
        <xsl:call-template name="divnum"/>
        <b><xsl:apply-templates select="." mode="text-only"/></b>
      </span>
    </xsl:when>
    <xsl:otherwise>
      <xsl:call-template name="divnum"/>
      <a>
	<xsl:attribute name="href"><xsl:value-of select="$doc.path"/>&#038;doc.view=<xsl:value-of select="$view"/>&#038;chunk.id=<xsl:value-of select="ancestor::*[1]/@id"/>&#038;toc.depth=<xsl:value-of select="$toc.depth"/>&#038;toc.id=<xsl:value-of select="$local.toc.id"/><xsl:value-of select="$search"/><xsl:call-template name="create.anchor"/></xsl:attribute>
	<xsl:attribute name="target">_top</xsl:attribute>
        <xsl:apply-templates select="." mode="text-only"/>
      </a>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="divnum">
  <xsl:if test="parent::*/@n">
    <b><xsl:value-of select="parent::*/@n"/><xsl:text>. </xsl:text></b>
  </xsl:if>
</xsl:template>

<!-- Used to extract the text of titles without <lb/>'s and other formatting -->

<xsl:template match="text()" mode="text-only">
  <xsl:value-of select="." />
</xsl:template>

<xsl:template match="lb" mode="text-only">
  <xsl:text>&#160;</xsl:text>
</xsl:template>

<!-- Expand and Collapse Templates -->

<xsl:template name="expand">

  <xsl:variable name="view">
    <xsl:choose>
      <xsl:when test="$doc.view='frames' or $doc.view='bbar' or $doc.view='toc' or $doc.view='content'">frames</xsl:when>
      <xsl:otherwise>0</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="local.toc.id" select="@id"/>

  <a>
    <xsl:attribute name="href"><xsl:value-of select="$doc.path"/>&#038;doc.view=<xsl:value-of select="$view"/>&#038;chunk.id=<xsl:value-of select="$chunk.id"/>&#038;toc.id=<xsl:value-of select="$local.toc.id"/><xsl:value-of select="$search"/></xsl:attribute>
    <xsl:attribute name="target">_top</xsl:attribute>
    <img src="{$icon.path}i_expand.gif" border="0" alt="expand section"/>
  </a>
</xsl:template>

<xsl:template name="collapse">

  <xsl:variable name="view">
    <xsl:choose>
      <xsl:when test="$doc.view='frames' or $doc.view='bbar' or $doc.view='toc' or $doc.view='content'">frames</xsl:when>
      <xsl:otherwise>0</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <!-- This is probably another point of inefficiency, but how else do you check if there are child nodes if the name is unknown? -->
  <xsl:variable name="local.toc.id">
    <xsl:choose>
      <xsl:when test="*[head and @id]">
        <xsl:value-of select="parent::*[head and @id]/@id"/>
      </xsl:when>
      <xsl:otherwise>
         <xsl:value-of select="0"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <a>
    <xsl:attribute name="href"><xsl:value-of select="$doc.path"/>&#038;doc.view=<xsl:value-of select="$view"/>&#038;chunk.id=<xsl:value-of select="$chunk.id"/>&#038;toc.id=<xsl:value-of select="$local.toc.id"/><xsl:value-of select="$search"/></xsl:attribute>
    <xsl:attribute name="target">_top</xsl:attribute>
    <img src="{$icon.path}i_colpse.gif" border="0" alt="collapse section"/>
  </a>
</xsl:template>

</xsl:stylesheet>
