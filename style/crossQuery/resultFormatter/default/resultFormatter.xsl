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
                              xmlns:dc="http://purl.org/dc/elements/1.1/" 
                              xmlns:mets="http://www.loc.gov/METS/" 
                              xmlns:xlink="http://www.w3.org/TR/xlink"
                              xmlns:session="java:org.cdlib.xtf.xslt.Session">
  
  <!-- ====================================================================== -->
  <!-- Import Common Templates                                                -->
  <!-- ====================================================================== -->

  <xsl:import href="../common/resultFormatterCommon.xsl"/>
  
  <!-- ====================================================================== -->
  <!-- Output Parameters                                                      -->
  <!-- ====================================================================== -->

  <xsl:output method="html" indent="yes" encoding="UTF-8" media-type="text/html" doctype-public="-//W3C//DTD HTML 4.0//EN"/>

  <!-- ====================================================================== -->
  <!-- Local Parameters                                                       -->
  <!-- ====================================================================== -->

  <xsl:param name="css.path" select="concat($xtfURL, 'css/default/')"/>
    
  <!-- ====================================================================== -->
  <!-- Root Template                                                          -->
  <!-- ====================================================================== -->
  
  <xsl:template match="/">
    <xsl:choose>
      <xsl:when test="$smode = 'addToBag'">
        <span class="highlight">Added to bag.</span>
      </xsl:when>
      <xsl:when test="$smode = 'removeFromBag'">
        <!-- No output needed -->
      </xsl:when>
      <xsl:when test="$smode = 'moreLike'">
        <xsl:apply-templates select="crossQueryResult" mode="moreLike"/>
      </xsl:when>
      <xsl:when test="$smode = 'test'">
        <xsl:apply-templates select="crossQueryResult" mode="test"/>
      </xsl:when>
      <xsl:when test="contains($smode, '-modify')">
        <xsl:apply-templates select="crossQueryResult" mode="form"/>
      </xsl:when>
      <xsl:when test="
        $text or 
        $title or 
        $creator or 
        $subject or 
        $description or 
        $publisher or 
        $contributor or 
        $date or 
        $type or 
        $format or 
        $identifier or 
        $source or 
        $language or 
        $relation or 
        $coverage or 
        $rights or 
        $year or
        $smode ='showBag'">
        <xsl:apply-templates select="crossQueryResult" mode="results"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates select="crossQueryResult" mode="form"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
  <!-- ====================================================================== -->
  <!-- Results Template                                                       -->
  <!-- ====================================================================== -->

  <xsl:template match="crossQueryResult" mode="results">
    
    <xsl:variable name="alpha-list" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ'"/>

    <xsl:variable name="anchor-list">
      <xsl:choose>
        <xsl:when test="$sort = 'creator'">
          <xsl:for-each select="//meta">
            <xsl:value-of select="substring(string(creator), 1, 1)"/>
          </xsl:for-each>
        </xsl:when>
        <xsl:when test="$sort = 'title'">
          <xsl:for-each select="//meta">
            <xsl:value-of select="substring(string(title), 1, 1)"/>
          </xsl:for-each>
        </xsl:when>
      </xsl:choose>
    </xsl:variable>

    <xsl:variable name="modifyString">
      <xsl:choose>
        <xsl:when test="contains($queryString, 'smode')">
          <xsl:analyze-string select="$queryString" regex=".*(smode=[A-Za-z0-9]+).*">
            <xsl:matching-substring>
              <xsl:value-of select="regex-group(1)"/>
              <xsl:value-of select="'-modify'"/>
            </xsl:matching-substring>
            <xsl:non-matching-substring>
              <xsl:value-of select="."/>
            </xsl:non-matching-substring>
          </xsl:analyze-string>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="concat($queryString, '&amp;smode=simple-modify')"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <html>
      <head>
        <title>XTF: Search Results</title>
        <xsl:copy-of select="$brand.links"/>
        
        <!-- If session tracking enabled, load bag tracking scripts -->
        <script src="script/AsyncLoader.js"/>
        <script src="script/MoreLike.js"/>
        <xsl:if test="session:isEnabled()">
          <script src="script/BookBag.js"/>
        </xsl:if>
        
      </head>
      <body bgcolor="ivory">
        
        <table width="100%" border="0" cellpadding="0" cellspacing="0" bgcolor="ivory">
          <xsl:copy-of select="$brand.header"/>
        </table>
        
        <table style="margin-top: 1%; margin-bottom: 1%" width="100%" cellpadding="0" cellspacing="2" border="0" bgcolor="ivory">
          <tr>
            <td align="right" width="10%">
              <span class="heading">Search:</span>
            </td>
            <td width="1%"/>
            <td align="left">
              <xsl:choose>
                <xsl:when test="$smode = 'showBag'">
                  Bag contents
                </xsl:when>
                <xsl:otherwise>
                  <xsl:call-template name="format-query"/>
                </xsl:otherwise>
              </xsl:choose>
            </td>
          </tr>
          <xsl:if test="//spelling/term/suggestion">
            <tr>
              <td align="right" width="10%"></td>
              <td width="1%"/>
              <td align="left">
                <xsl:call-template name="did-you-mean">
                  <xsl:with-param name="baseURL" select="concat($xtfURL, $crossqueryPath, '?', $queryString)"/>
                  <xsl:with-param name="spelling" select="//spelling"/>
                </xsl:call-template>
              </td>
            </tr>
          </xsl:if>
          <tr>
            <td align="right" width="10%">
              <span class="heading">Results:</span>
            </td>
            <td width="1%"/>
            <td align="left">
              <span id="itemCount">
                <xsl:value-of select="@totalDocs"/>
              </span>
              <xsl:text> Item(s)</xsl:text>
            </td>
          </tr>
          <tr>
            <td align="right" width="10%">
              <span class="heading">Sorted by:</span>
            </td>
            <td width="1%"/>
            <form class="search-form" method="get" action="{$xtfURL}{$crossqueryPath}">
              <td align="left" valign="bottom">
                <xsl:call-template name="sort.options"/>
                <xsl:call-template name="hidden.query">
                  <xsl:with-param name="queryString" select="replace($queryString, 'sort=[^&amp;]+&amp;', '')"/>
                </xsl:call-template>
                <input type="submit" value="Go!"/>
              </td>
            </form>
          </tr>
          <tr>
            <td colspan="3">
              <xsl:call-template name="pages"/>
            </td>
          </tr> 
          <tr>
            <td colspan="3" align="center">
              <xsl:if test="$smode != 'showBag'">
                <a class="highlight" href="{$xtfURL}{$crossqueryPath}?{$modifyString}">
                  <xsl:text>Modify Search</xsl:text>
                </a>
                <xsl:text>&#160;&#160;</xsl:text>
              </xsl:if>
              <a class="highlight" href="{$xtfURL}{$crossqueryPath}">
                <xsl:text>Begin New Search</xsl:text>
              </a>
              <xsl:if test="$smode = 'showBag'">
                <xsl:text>&#160;&#160;</xsl:text>
                <a class="highlight" href="{session:getData('queryURL')}">
                  <xsl:text>Return to Search Results</xsl:text>
                </a>
              </xsl:if>
            </td>
          </tr>
        </table>
        
        <xsl:if test="docHit">
          <table width="100%" cellpading="0" cellspacing="2" bgcolor="ivory">          
            <tr>
              <td colspan="4">
                <hr size="1" width="100%"/>
              </td>
            </tr>    
          </table>
          <xsl:apply-templates select="*"/>
        </xsl:if>
        
        <xsl:copy-of select="$brand.footer"/>    
        
      </body>
    </html>
  </xsl:template>
  
  <!-- ====================================================================== -->
  <!-- Form Template                                                          -->
  <!-- ====================================================================== -->

  <xsl:template match="crossQueryResult" mode="form">
    <html>
      <head>
        <title>XTF: Search Form</title>
        <xsl:copy-of select="$brand.links"/>
      </head>
      <body bgcolor="ivory">
        <table width="100%" border="0" cellpadding="0" cellspacing="0" bgcolor="ivory">
          <xsl:copy-of select="$brand.header"/>
        </table>  
        <form method="get" action="{$xtfURL}{$crossqueryPath}">
          <table width="90%" cellpading="0" cellspacing="2" bgcolor="ivory" border="0">
            <tr>
              <td colspan="3"><h4>Full Text</h4></td>
            </tr>
            <tr>
              <td width="200">
                <xsl:choose>
                  <xsl:when test="$text-join = 'or'">
                    <b>all</b>
                    <xsl:text> or </xsl:text>
                    <b>any</b><input type="checkbox" name="text-join" value="or" checked="yes"/>
                  </xsl:when>
                  <xsl:otherwise>
                    <b>all</b>
                    <xsl:text> or </xsl:text>
                    <b>any</b><input type="checkbox" name="text-join" value="or"/>
                  </xsl:otherwise>
                </xsl:choose>
                <xsl:text>of the words</xsl:text>
              </td>
              <td width="40"/>
              <td>
                <input type="text" name="text" size="40" value="{$text}"/>
              </td>
            </tr>
            <tr>
              <td width="200"><b>without</b> the words</td>
              <td width="40"/>
              <td>
                <input type="text" name="text-exclude" size="40" value="{$text-exclude}"/>
              </td>
            </tr>
            <tr>
              <td width="200"><b>Proximity</b></td>
              <td width="40"/>
              <td>
                <select size="1" name="text-prox">
                  <xsl:choose>
                    <xsl:when test="$text-prox = '1'">
                      <option value=""></option>
                      <option value="1" selected="yes">1</option>
                      <option value="2">2</option>
                      <option value="3">3</option>
                      <option value="4">4</option>
                      <option value="5">5</option>
                      <option value="10">10</option>
                      <option value="20">20</option>
                    </xsl:when>
                    <xsl:when test="$text-prox = '2'">
                      <option value=""></option>
                      <option value="1">1</option>
                      <option value="2" selected="yes">2</option>
                      <option value="3">3</option>
                      <option value="4">4</option>
                      <option value="5">5</option>
                      <option value="10">10</option>
                      <option value="20">20</option>
                    </xsl:when>
                    <xsl:when test="$text-prox = '3'">
                      <option value=""></option>
                      <option value="1">1</option>
                      <option value="2">2</option>
                      <option value="3" selected="yes">3</option>
                      <option value="4">4</option>
                      <option value="5">5</option>
                      <option value="10">10</option>
                      <option value="20">20</option>
                    </xsl:when>
                    <xsl:when test="$text-prox = '4'">
                      <option value=""></option>
                      <option value="1">1</option>
                      <option value="2">2</option>
                      <option value="3">3</option>
                      <option value="4" selected="yes">4</option>
                      <option value="5">5</option>
                      <option value="10">10</option>
                      <option value="20">20</option>
                    </xsl:when>
                    <xsl:when test="$text-prox = '5'">
                      <option value=""></option>
                      <option value="1">1</option>
                      <option value="2">2</option>
                      <option value="3">3</option>
                      <option value="4">4</option>
                      <option value="5" selected="yes">5</option>
                      <option value="10">10</option>
                      <option value="20">20</option>
                    </xsl:when>
                    <xsl:when test="$text-prox = '10'">
                      <option value=""></option>
                      <option value="1">1</option>
                      <option value="2">2</option>
                      <option value="3">3</option>
                      <option value="4">4</option>
                      <option value="5">5</option>
                      <option value="10" selected="yes">10</option>
                      <option value="20">20</option>
                    </xsl:when>
                    <xsl:when test="$text-prox = '20'">
                      <option value=""></option>
                      <option value="1">1</option>
                      <option value="2">2</option>
                      <option value="3">3</option>
                      <option value="4">4</option>
                      <option value="5">5</option>
                      <option value="10">10</option>
                      <option value="20" selected="yes">20</option>
                    </xsl:when>
                    <xsl:otherwise>
                      <option value="" selected="yes"></option>
                      <option value="1">1</option>
                      <option value="2">2</option>
                      <option value="3">3</option>
                      <option value="4">4</option>
                      <option value="5">5</option>
                      <option value="10">10</option>
                      <option value="20">20</option>
                    </xsl:otherwise>
                  </xsl:choose>
                </select>
                <xsl:text> word(s)</xsl:text>
              </td>
            </tr>           
            <tr>
              <td width="200"><b>Search Area(s)</b></td>
              <td width="40"/>
              <td>
                <xsl:choose>
                  <xsl:when test="$sectionType = 'head'">
                    <xsl:text>All</xsl:text><input type="radio" name="sectionType" value=""/> 
                    <xsl:text>Titles</xsl:text><input type="radio" name="sectionType" value="head" checked="yes"/> 
                  </xsl:when>
                  <xsl:otherwise>
                    <xsl:text>All</xsl:text><input type="radio" name="sectionType" value="" checked="yes"/> 
                    <xsl:text>Titles</xsl:text><input type="radio" name="sectionType" value="head"/> 
                  </xsl:otherwise>
                </xsl:choose>
              </td>
            </tr>
            <tr>
              <td colspan="3"><h4>Metadata</h4></td>
            </tr>
            <tr>
              <td width="200"><b>Title</b></td>
              <td width="40"/>
              <td>
                <input type="text" name="title" size="40" value="{$title}"/>
              </td>
            </tr>
            <tr>
              <td width="200"><b>Author</b></td>
              <td width="40"/>
              <td>
                <input type="text" name="creator" size="40" value="{$creator}"/>
              </td>
            </tr>
            <tr>
              <td width="200"><b>Subject</b></td>
              <td width="40"/>
              <td>
                <!-- need to make sure that the chosen subject is retained in modify -->
                <select size="1" name="subject">
                  <option value="">Select</option>
                  <option value="African History">African History</option>
                  <option value="African Studies">African Studies</option>
                  <option value="American Literature">American Literature</option>
                  <option value="American Studies">American Studies</option>
                  <option value="Anthropology">Anthropology</option>
                  <option value="Asian Studies">Asian Studies</option>
                  <option value="Cinema and Performance Arts">Cinema and Performance Arts</option>
                  <option value="Cultural Anthropology">Cultural Anthropology</option>
                  <option value="English Literature">English Literature</option>
                  <option value="Ethnic Studies">Ethnic Studies</option>
                  <option value="Gender Studies">Gender Studies</option>
                  <option value="Intellectual History">Intellectual History</option>
                  <option value="Law">Law</option>
                  <option value="Literary Theory and Criticism">Literary Theory and Criticism</option>
                  <option value="Literature">Literature</option>
                  <option value="Media Studies">Media Studies</option>
                  <option value="Middle Eastern History">Middle Eastern History</option>
                  <option value="Middle Eastern Studies">Middle Eastern Studies</option>
                  <option value="Music">Music</option>
                  <option value="Native American Studies">Native American Studies</option>
                  <option value="Politics">Politics</option>
                  <option value="Postcolonial Studies">Postcolonial Studies</option>
                  <option value="Public Policy">Public Policy</option>
                  <option value="Renaissance Literature">Renaissance Literature</option>
                  <option value="Science">Science</option>
                  <option value="Social Problems">Social Problems</option>
                  <option value="Sociology">Sociology</option>
                  <option value="Urban Studies">Urban Studies</option>
                </select>
              </td>
            </tr>
            <tr>
              <td width="200"><b>Year</b></td>
              <td width="40"/>
              <td>
                <input type="text" name="year" size="40" value="{$year}"/>
              </td>
            </tr>
            <tr>
              <td width="120" colspan="2"/>
              <td height="40">
                <input type="submit" value="Search"/>
                <input type="reset" OnClick="location.href='{$xtfURL}{$crossqueryPath}'" value="Clear"/>
              </td>
            </tr>
          </table>
        </form>
        <xsl:copy-of select="$brand.footer"/>
      </body>
    </html>
  </xsl:template>
  
  <!-- ====================================================================== -->
  <!-- Document Hit Template                                                       -->
  <!-- ====================================================================== -->
  
  <xsl:template match="docHit">

    <xsl:variable name="path" select="@path"/>
    
    <xsl:variable name="fullark" select="meta/identifier[1]"/>
    <xsl:variable name="ark" select="substring($fullark, string-length($fullark)-9)"/>
    <xsl:variable name="quotedArk" select="concat('&quot;', $ark, '&quot;')"/>
    
    <xsl:variable name="collection" select="string(meta/collection)"/>

    <!-- The identifier stored in the index is the full ark minus "http:/cdlib/ark:/" -->    
    <xsl:variable name="indexId" select="replace($fullark, '.*:/', '')"/>

    <xsl:variable name="anchor">
      <xsl:choose>
        <xsl:when test="$sort = 'creator'">
          <xsl:value-of select="substring(string(meta/creator), 1, 1)"/>
        </xsl:when>
        <xsl:when test="$sort = 'title'">
          <xsl:value-of select="substring(string(meta/title), 1, 1)"/>
        </xsl:when>
      </xsl:choose>
    </xsl:variable>

    <div id="{$ark}-main">    
    <table width="100%" cellpading="0" cellspacing="2" bgcolor="ivory">          
    
    <tr>
      <td align="right" width="4%">
        <xsl:choose>
          <xsl:when test="$sort = ''">
            <span class="heading"><xsl:value-of select="@rank"/></span>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>&#160;</xsl:text>
          </xsl:otherwise>
        </xsl:choose>
      </td>
      <td align="right" width="10%">
        <xsl:if test="$sort = 'creator'">
          <a name="{$anchor}"/>
        </xsl:if>
        <span class="heading">Author:&#160;&#160;</span>
      </td>
      <td align="left">
        <xsl:apply-templates select="meta/creator[1]"/>
      </td>
      <td align="right" width="4%">
        <xsl:text>&#160;</xsl:text>
      </td>
    </tr>
    <tr>
      <td align="right">
        <xsl:text>&#160;</xsl:text>
      </td>
      <td align="right">
        <xsl:if test="$sort = 'title'">
          <a name="{$anchor}"/>
        </xsl:if>
        <span class="heading">Title:&#160;&#160;</span>
      </td>
      <td align="left">
        <a>
          <xsl:attribute name="href">
            <xsl:call-template name="dynaxml.url">
              <xsl:with-param name="path" select="$path"/>
            </xsl:call-template>
          </xsl:attribute>
          <xsl:apply-templates select="meta/title[1]"/>
        </a>
      </td>
      <td align="center">
        <span class="heading">
          <xsl:value-of select="@score"/>
          <xsl:if test="not(matches($normalizeScores, 'no|false'))">
            <xsl:text>%</xsl:text>
          </xsl:if>
        </span>
      </td>
    </tr>
    <tr>
      <td align="right">
        <xsl:text>&#160;</xsl:text>
      </td>
      <td align="right">
        <span class="heading">Collection:&#160;&#160;</span>
      </td>
      <td align="left">
        <xsl:apply-templates select="meta/relation[1]"/>
      </td>
      <td align="right">
        <xsl:text>&#160;</xsl:text>
      </td>
    </tr>
    <tr>
      <td align="right">
        <xsl:text>&#160;</xsl:text>
      </td>
      <td align="right">
        <span class="heading">Published:&#160;&#160;</span>
      </td>
      <td align="left">
        <xsl:apply-templates select="meta/year"/>
      </td>
      <td align="right">
        <xsl:text>&#160;</xsl:text>
      </td>
    </tr>
    <tr>
      <td align="right">
        <xsl:text>&#160;</xsl:text>
      </td>
      <td align="right">
        <span class="heading">Subjects:&#160;&#160;</span>
      </td>
      <td align="left">
        <xsl:apply-templates select="meta/subject"/>
      </td>
      <td align="right">
        <xsl:text>&#160;</xsl:text>
      </td>
    </tr>
    <xsl:if test="(snippet) and ($sort = '')">
      <tr>
        <td align="right">
          <xsl:text>&#160;</xsl:text>
        </td>
        <td align="right" valign="top">
          <span class="heading">Matches:&#160;&#160;</span>
        </td>
        <td align="left">
          <xsl:apply-templates select="snippet" mode="text"/>
        </td>
        <td align="right">
          <xsl:text>&#160;</xsl:text>
        </td>
      </tr>
    </xsl:if>
    <xsl:if test="explanation">
      <tr>
        <td align="right">
          <xsl:text>&#160;</xsl:text>
        </td>
        <td align="right" valign="top">
          <span class="heading">Explanation:&#160;&#160;</span>
        </td>
        <td align="left">
          <xsl:apply-templates select="explanation"/>
        </td>
        <td align="right">
          <xsl:text>&#160;</xsl:text>
        </td>
      </tr>
    </xsl:if>

    <!-- Add/remove logic for the session bag (only if session tracking enabled) -->
    <xsl:if test="session:isEnabled()">
      <tr>
        <td align="right">
          <xsl:text>&#160;</xsl:text>
        </td>
        <td align="right" valign="top">
          <span class="heading">Bag:&#160;&#160;</span>
        </td>
        <td align="left">
        
          <xsl:choose>
            <xsl:when test="$smode = 'showBag'">
              <xsl:variable name="removeURL" select="session:encodeURL(concat($xtfURL, $crossqueryPath, '?smode=removeFromBag&amp;identifier=', $indexId))"/>
              <span id="{$ark}-remove">
                <a class="highlight" href="{concat('javascript:removeFromBag(', $quotedArk, ', &quot;', $removeURL, '&quot;)')}">
                  Remove from bag
                </a>
              </span>
            </xsl:when>
            <xsl:otherwise>
              <a href="{concat($xtfURL, $crossqueryPath, '?smode=showBag&amp;', $queryString)}">Show bag</a> |
              <xsl:choose>
                <xsl:when test="session:getData('bag')/bag/savedDoc[@id=$indexId]">
                  <span class="highlight">In bag.</span>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:variable name="addURL" select="session:encodeURL(concat($xtfURL, $crossqueryPath, '?smode=addToBag&amp;identifier=', $indexId))"/>
                  <span id="{$ark}-add">
                    <a href="{concat('javascript:addToBag(', $quotedArk, ', &quot;', $addURL, '&quot;)')}">
                      Add to bag
                    </a>
                  </span>
                </xsl:otherwise>
              </xsl:choose>
              <xsl:value-of select="session:setData('queryURL', concat($xtfURL, $crossqueryPath, '?', $queryString))"/>
            </xsl:otherwise>
          </xsl:choose>
        </td>
        <td align="right">
          <xsl:text>&#160;</xsl:text>
        </td>
      </tr>
    </xsl:if>

    <!-- Test "more like this" -->
    <tr>
      <td align="right">
        <xsl:text>&#160;</xsl:text>
      </td>
      <td align="right" valign="top">
        <span class="heading">Similar:&#160;&#160;</span>
      </td>
      <td align="left">
        <xsl:variable name="url" select="session:encodeURL(concat($xtfURL, $crossqueryPath, '?smode=moreLike&amp;docsPerPage=5&amp;identifier=', $indexId))"/>
        <span id="{$ark}-moreLike">
          <a class="highlight" href="{concat('javascript:moreLike(', $quotedArk, ', &quot;', $url, '&quot;)')}">
            Fetch
          </a>
        </span>
      </td>
      <td align="right">
        <xsl:text>&#160;</xsl:text>
      </td>
    </tr>

    <tr>
      <td colspan="4">
        <hr size="1" width="100%"/>
      </td>
    </tr>
    
    </table>
    </div>
    
  </xsl:template>
    
  <!-- ====================================================================== -->
  <!-- Snippet Template (for snippets in the full text)                       -->
  <!-- ====================================================================== -->

  <xsl:template match="snippet">
    <xsl:choose>
      <xsl:when test="ancestor::*">
        <xsl:apply-templates/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>...xxx</xsl:text>
        <xsl:apply-templates/>
        <xsl:text>...</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
    <br/>
  </xsl:template>
    
  <!-- ====================================================================== -->
  <!-- Term Template (for snippets in the full text)                          -->
  <!-- ====================================================================== -->
 
  <xsl:template match="term" mode="text">
    <xsl:variable name="path" select="ancestor::docHit/@path"/>
    <xsl:variable name="collection" select="string(meta/collection)"/>
    <xsl:variable name="hit.rank"><xsl:value-of select="ancestor::snippet/@rank"/></xsl:variable>
    <xsl:variable name="snippet.link">    
      <xsl:call-template name="dynaxml.url">
        <xsl:with-param name="path" select="$path"/>
      </xsl:call-template>
      <xsl:value-of select="concat('&amp;hit.rank=', $hit.rank)"/>
    </xsl:variable>
    
    <xsl:choose>
      <xsl:when test="ancestor::query"/>
      <xsl:when test="not(ancestor::snippet)">
        <span class="term">
          <xsl:apply-templates/>
        </span>
      </xsl:when>
      <xsl:otherwise>
        <a class="term" href="{$snippet.link}">
          <xsl:apply-templates/>
        </a>
      </xsl:otherwise>
    </xsl:choose> 
   
  </xsl:template>

  <!-- ====================================================================== -->
  <!-- Term Template (for snippets in meta-data fields)                       -->
  <!-- ====================================================================== -->
  
  <xsl:template match="term">
    <xsl:choose>
      <xsl:when test="ancestor::query"/>
      <xsl:otherwise>
        <span class="term">
          <xsl:apply-templates/>
        </span>
      </xsl:otherwise>
    </xsl:choose> 
    
  </xsl:template>
  
  <!-- ====================================================================== -->
  <!-- Explanation Template                                                   -->
  <!-- ====================================================================== -->
  
  <xsl:template match="explanation">
    <ul>
      <li>
        <xsl:value-of select="@value"/>
        <xsl:text> = </xsl:text>
        <xsl:value-of select="@description"/>
      </li>
      <xsl:apply-templates/>
    </ul>
  </xsl:template>
    
  <!-- ====================================================================== -->
  <!-- More Like This Template                                                -->
  <!-- ====================================================================== -->
  <xsl:template match="crossQueryResult" mode="moreLike">
    <xsl:choose>
      <xsl:when test="docHit">
        <xsl:apply-templates select="docHit" mode="moreLike"/>
      </xsl:when>
      <xsl:otherwise>
        No similar documents found.
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
  <xsl:template match="docHit" mode="moreLike">

    <xsl:variable name="path" select="@path"/>
    
    <a>
      <xsl:attribute name="href">
        <xsl:call-template name="dynaxml.url">
          <xsl:with-param name="path" select="$path"/>
        </xsl:call-template>
      </xsl:attribute>
      <xsl:apply-templates select="meta/title[1]"/>
    </a>
    (<xsl:apply-templates select="meta/creator[1]"/>)<br/>
  </xsl:template>
    
</xsl:stylesheet>
