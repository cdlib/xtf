<!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
<!-- Query result formatter stylesheet                                      -->
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

<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
                              xmlns:dc="http://purl.org/dc/elements/1.1/" 
                              xmlns:mets="http://www.loc.gov/METS/" 
                              xmlns:xlink="http://www.w3.org/TR/xlink">
  
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

  <xsl:param name="css.path" select="'/xtf/css/default/'"/>
    
  <!-- ====================================================================== -->
  <!-- Root Template                                                          -->
  <!-- ====================================================================== -->
  
  <xsl:template match="/">
    <xsl:choose>
      <xsl:when test="$smode = 'debug'">
        <pre>
          <xsl:apply-templates select="*" mode="debug"/>
        </pre>
      </xsl:when>
      <xsl:when test="$smode = 'test'">
        <xsl:apply-templates select="crossQueryResult" mode="test"/>
      </xsl:when>
      <xsl:when test="$smode = 'modify'">
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
        $year">
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

    <html>
      <head>
        <title>XTF: Search Results</title>
        <link rel="stylesheet" href="{$css.path}results.css" type="text/css"/>
      </head>
      <body bgcolor="ivory">
        <xsl:call-template name="header"/>
        <table style="margin-top: 1%; margin-bottom: 1%" width="100%" cellpadding="0" cellspacing="2" border="0" bgcolor="ivory">
          <tr>
            <td align="right" width="10%">
              <span class="heading">Search:</span>
            </td>
            <td width="1%"/>
            <td align="left">
              <xsl:call-template name="format-query">
                <xsl:with-param name="query" select="$query"/>
              </xsl:call-template>
            </td>
          </tr>
          <tr>
            <td align="right" width="10%">
              <span class="heading">Results:</span>
            </td>
            <td width="1%"/>
            <td align="left">
              <xsl:value-of select="@totalDocs"/>
              <xsl:text> Item(s)</xsl:text>
            </td>
          </tr>
          <tr>
            <td align="right" width="10%">
              <span class="heading">Sorted by:</span>
            </td>
            <td width="1%"/>
            <form class="search-form" method="get" action="{$servlet.path}">
              <td align="left" valign="bottom">
                <xsl:call-template name="sort.options"/>
                <xsl:call-template name="hidden.query"/>
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
              <a class="highlight" href="{$servlet.path}?{$queryString}&amp;smode=modify">
                <xsl:text>Modify Search</xsl:text>
              </a>
              <xsl:text>&#160;&#160;</xsl:text>
              <a class="highlight" href="{$servlet.path}">
                <xsl:text>Begin New Search</xsl:text>
              </a>
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
            <xsl:apply-templates select="*"/>
          </table>
        </xsl:if>
        
        <xsl:call-template name="footer"/>      
        
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
        <link rel="stylesheet" href="{$css.path}results.css" type="text/css"/>
      </head>
      <body bgcolor="ivory">
        <xsl:call-template name="header"/>
        <form method="get" action="{$servlet.path}">
          <div align="center">
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
                  <input type="reset" OnClick="location.href='{$servlet.path}'" value="Clear"/>
                </td>
              </tr>
            </table>
          </div>
        </form>
        <xsl:call-template name="footer"/>
      </body>
    </html>
  </xsl:template>
  
  <!-- ====================================================================== -->
  <!-- Document Hit Template                                                       -->
  <!-- ====================================================================== -->
  
  <xsl:template match="docHit">

    <xsl:variable name="fullark" select="meta/identifier"/>
    <xsl:variable name="ark" select="substring($fullark, string-length($fullark)-9)"/>
    <xsl:variable name="collection" select="string(meta/collection)"/>

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
    
    <tr>
      <td align="right" width="4%">
        <xsl:choose>
          <xsl:when test="$sort != 'title' and $sort != 'creator' and $sort != 'year'">
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
            <xsl:value-of select="concat($dynaxml.path, '?docId=', $ark, '&amp;query=', replace($text, '&amp;', '%26'))"/>
            <!-- -join & -prox are mutually exclusive -->
            <xsl:choose>
              <xsl:when test="$text-prox">
                <xsl:value-of select="concat('&amp;query-prox=', $text-prox)"/>
              </xsl:when>
              <xsl:when test="$text-join">
                <xsl:value-of select="concat('&amp;query-join=', $text-join)"/>
              </xsl:when>            
            </xsl:choose>
            <xsl:if test="$text-exclude">
              <xsl:value-of select="concat('&amp;query-exclude=', $text-exclude)"/>
            </xsl:if>
            <xsl:if test="$sectionType">
              <xsl:value-of select="concat('&amp;sectionType=', $sectionType)"/>
            </xsl:if>
            <xsl:value-of select="'&amp;doc.view=frames'"/>
          </xsl:attribute>
          <xsl:apply-templates select="meta/title[1]"/>
        </a>
      </td>
      <td align="center">
        <span class="heading"><xsl:value-of select="@score"/>%</span>
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
        <!-- THIS NEEDS WORK -->
        <xsl:choose>
          <xsl:when test="contains(meta/relation[2], 'escholarship')">
            <xsl:text>eScholarship Editions&#160;&#160;</xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <xsl:apply-templates select="meta/relation[1]"/>
            <xsl:text>&#160;&#160;</xsl:text>
          </xsl:otherwise>
        </xsl:choose>
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
        <!-- THIS NEEDS WORK -->
        <xsl:choose>
          <xsl:when test="contains(meta/relation[1], 'ucpress')">
            <xsl:text>University of California Press.&#160;&#160;</xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <xsl:apply-templates select="meta/relation[1]"/>
            <xsl:text>&#160;&#160;</xsl:text>
          </xsl:otherwise>
        </xsl:choose>
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
    <xsl:if test="(snippet) and ($sort != 'title' and $sort != 'creator' and $sort != 'year')">
      <tr>
        <td align="right">
          <xsl:text>&#160;</xsl:text>
        </td>
        <td align="right" valign="top">
          <span class="heading">Matches:&#160;&#160;</span>
        </td>
        <td align="left">
          <xsl:apply-templates select="snippet"/>
        </td>
        <td align="right">
          <xsl:text>&#160;</xsl:text>
        </td>
      </tr>
    </xsl:if>
    <tr>
      <td colspan="4">
        <hr size="1" width="100%"/>
      </td>
    </tr>
    
  </xsl:template>
    
  <!-- ====================================================================== -->
  <!-- Snippet Template                                                       -->
  <!-- ====================================================================== -->

  <xsl:template match="snippet">
    <xsl:text>...</xsl:text>
    <xsl:apply-templates/>
    <xsl:text>...</xsl:text>
    <br/>
  </xsl:template>
    
  <!-- ====================================================================== -->
  <!-- Hit Template                                                           -->
  <!-- ====================================================================== -->
  
  <xsl:template match="hit">
    <xsl:apply-templates/>
  </xsl:template>
    
  <!-- ====================================================================== -->
  <!-- Term Template                                                          -->
  <!-- ====================================================================== -->
 
  <xsl:template match="term">
    <xsl:variable name="fullark" select="ancestor::docHit/meta/identifier"/>
    <xsl:variable name="ark" select="substring($fullark, string-length($fullark)-9)"/>
    <xsl:variable name="collection" select="string(meta/collection)"/>
    <xsl:variable name="hit.rank"><xsl:value-of select="ancestor::snippet/@rank"/></xsl:variable>
    <xsl:variable name="snippet.link">
      <xsl:value-of select="concat($dynaxml.path, '?docId=', $ark, '&amp;query=', replace($text, '&amp;', '%26'))"/>
      <!-- -join & -prox are mutually exclusive -->
      <xsl:choose>
        <xsl:when test="$text-prox">
          <xsl:value-of select="concat('&amp;query-prox=', $text-prox)"/>
        </xsl:when>
        <xsl:when test="$text-join">
          <xsl:value-of select="concat('&amp;query-join=', $text-join)"/>
        </xsl:when>            
      </xsl:choose>
      <xsl:if test="$text-exclude">
        <xsl:value-of select="concat('&amp;query-exclude=', $text-exclude)"/>
      </xsl:if>
      <xsl:if test="$sectionType">
        <xsl:value-of select="concat('&amp;sectionType=', $sectionType)"/>
      </xsl:if>
      <xsl:value-of select="concat('&amp;hit.rank=', $hit.rank)"/>
      <xsl:value-of select="'&amp;doc.view=frames'"/>
    </xsl:variable>
    
    <xsl:choose>
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
  <!-- Display Header & Footer                                                -->
  <!-- ====================================================================== -->

  <xsl:template name="header">
    <table width="100%" border="0" cellpadding="0" cellspacing="0" bgcolor="ivory">     
      <tr bgcolor="#62708D">
        <td width="33%" height="44" align="left">
          <a href="http://xtf.sourceforge.net" target="_top">
            <img src="/xtf/icons/default/spacer.gif" border="0" width="15"/>
            <img src="/xtf/icons/default/xtfMan.gif" border="0" height="44" alt="XTF"/>
          </a>
        </td>
        <td width="34%" height="44" align="center">
          <h1 style="color: white">XTF Demonstration</h1>
        </td>
        <td width="33%" height="44" align="right">
          <a href="http://www.sourceforge.net" target="_top">
            <img src="/xtf/icons/default/sflogo.php.png" border="0" alt="SourceForge"/>
            <img src="/xtf/icons/default/spacer.gif" border="0" width="15"/>
          </a>
        </td>
      </tr>
      <tr bgcolor="silver">
        <td align="left">
          <a href="{$servlet.path}">Search</a>
          <xsl:text> | </xsl:text>
          <a href="{$servlet.path}?relation=www.ucpress.edu&amp;sort=title">Browse</a>
        </td>
        <td align="right" colspan="2">
          <a href="http://xtf.sourceforge.net/index.html#docs">Help</a>
        </td>
      </tr>
    </table>
  </xsl:template>
 
  <xsl:template name="footer">
    <table width="100%" cellpading="0" cellspacing="2" bgcolor="ivory">        
      <tr>
        <td>
          <a href="https://sourceforge.net/tracker/?func=add&amp;group_id=119724&amp;atid=684779">Comments? Questions?</a>
        </td>
      </tr>
    </table>
  </xsl:template>

</xsl:stylesheet>
