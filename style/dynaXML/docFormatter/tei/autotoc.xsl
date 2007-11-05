<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xtf="http://cdlib.org/xtf">
  
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
  
   <xsl:template name="book.autotoc">
      <!-- book title -->
      <table cellpadding="0" cellspacing="0" width="100%">
         <tr>
            <td>&#160;</td>
         </tr>
         <tr>
            <td>
               <a style="text-decoration: none; font-weight: bold">
                  <xsl:attribute name="href">
                     <xsl:value-of select="$doc.path"/>;brand=<xsl:value-of select="$brand"/>;<xsl:value-of select="$search"/>
                  </xsl:attribute>
                  <xsl:attribute name="target">_top</xsl:attribute>
                  <xsl:value-of select="/TEI.2/text/front/titlePage/titlePart[@type='main']"/>
               </a>
            </td>
         </tr>
      </table>
      <!-- hit summary -->
      <xsl:if test="($query != '0') and ($query != '')">
         <hr/>
         <xsl:call-template name="hitSummary"/>
      </xsl:if>
      <hr/>
      <!-- front -->
      <xsl:apply-templates select="/TEI.2/text/front/*[matches(name(),'^div')]" mode="toc"/>
      <br/>
      <!-- body -->
      <xsl:apply-templates select="/TEI.2/text/body/*[matches(name(),'^div')]" mode="toc"/>
      <br/>
      <!-- back -->
      <xsl:apply-templates select="/TEI.2/text/back/*[matches(name(),'^div')]" mode="toc"/>
      <!-- hit summary -->
      <xsl:if test="($query != '0') and ($query != '')">
         <hr/>
         <xsl:call-template name="hitSummary"/>
      </xsl:if>
      <!-- expand/collapse all -->
      <xsl:call-template name="expandAll"/>
   </xsl:template>
   
   <!-- div processing template -->
   <xsl:template match="*[matches(name(),'^div')]" mode="toc">
      
      <!-- head element -->
      <xsl:variable name="head" select="head"/>
      <!-- hit count for this node -->
      <xsl:variable name="hit.count">
         <xsl:choose>
            <xsl:when test="($query != '0') and ($query != '') and (@xtf:hitCount)">
               <xsl:value-of select="@xtf:hitCount"/>
            </xsl:when>
            <xsl:otherwise>0</xsl:otherwise>
         </xsl:choose>
      </xsl:variable>
      <!-- hierarchical level -->
      <xsl:variable name="level" select="count(ancestor::*[matches(name(),'^div')]) + 1"/>
      <!-- number of pixels per hierarchical level -->
      <xsl:variable name="factor" select="20"/>
      
      <xsl:if test="$head">
         <table border="0" cellpadding="0" cellspacing="0" width="820">
            <tr>
               <!-- show node hits -->
               <xsl:choose>
                  <xsl:when test="($query != '0') and ($query != '')">
                     <xsl:choose>
                        <xsl:when test="$hit.count != '0'">
                           <td width="{($level * $factor) + 10}" align="right">
                              <b style="color: red">
                                 <xsl:value-of select="$hit.count"/>
                              </b>
                           </td>
                        </xsl:when>
                        <xsl:otherwise>
                           <td width="{($level * $factor) + 10}">&#160;</td>
                        </xsl:otherwise>
                     </xsl:choose>
                  </xsl:when>
                  <xsl:otherwise>
                     <td>
                        <xsl:attribute name="width">
                           <xsl:choose>
                              <xsl:when test="$level=1">
                                 <xsl:value-of select="1"/>
                              </xsl:when>
                              <xsl:otherwise>
                                 <xsl:value-of select="($level * $factor) - $factor"/>
                              </xsl:otherwise>
                           </xsl:choose>
                        </xsl:attribute>
                        &#160;
                     </td>
                  </xsl:otherwise>
               </xsl:choose>
               <!-- create expand/collapse buttons -->
               <xsl:choose>
                  <xsl:when test="(number($toc.depth) &lt; ($level + 1) and *[matches(name(),'^div')]/head) and (not(@id = key('div-id', $toc.id)/ancestor-or-self::*/@id))">
                     <td width="15" align="center">
                        <xsl:call-template name="expand"/>
                     </td>
                  </xsl:when>
                  <xsl:when test="(number($toc.depth) > $level and *[matches(name(),'^div')]/head) or (@id = key('div-id', $toc.id)/ancestor-or-self::*/@id)">
                     <td width="15" align="center">
                        <xsl:call-template name="collapse"/>
                     </td>
                  </xsl:when>
                  <xsl:otherwise>
                     <td width="15">&#160;</td>
                  </xsl:otherwise>
               </xsl:choose>
               <!-- div number, if present -->
               <xsl:if test="//*[matches(name(),'^div')]/@n">
                  <td width="30" align="right">
                     <xsl:choose>
                        <xsl:when test="@n">
                           <b>
                              <xsl:value-of select="@n"/>
                              <xsl:text>.&#160;</xsl:text>
                           </b>
                        </xsl:when>
                        <xsl:otherwise>&#160;</xsl:otherwise>
                     </xsl:choose>
                  </td>
               </xsl:if>
               <!-- actual title -->
               <td align="left" valign="top" width="700">
                  <nobr>
                     <xsl:apply-templates select="head[1]" mode="toc"/>
                  </nobr>
               </td>
            </tr>
         </table>
         <!-- process node children if required -->
         <xsl:if test="(number($toc.depth) > $level and *[matches(name(),'^div')]/head) or (@id = key('div-id', $toc.id)/ancestor-or-self::*/@id)">
            <xsl:apply-templates select="*[matches(name(),'^div')]" mode="toc"/>
         </xsl:if>
      </xsl:if>
   </xsl:template>
   
   <!-- processs head element for toc -->
   <xsl:template match="head" mode="toc">
      
      <!-- mechanism by which the proper toc branch is expanded -->
      <xsl:variable name="local.toc.id">
         <xsl:choose>
            <!-- if this node is not terminal, expand this node -->
            <xsl:when test="parent::*[matches(name(),'^div')]/*[matches(name(),'^div')]">
               <xsl:value-of select="parent::*[matches(name(),'^div')]/@id"/>
            </xsl:when>
            <!-- if this node is terminal, expand the parent node -->
            <xsl:otherwise>
               <xsl:value-of select="parent::*[matches(name(),'^div')]/parent::*[matches(name(),'^div')]/@id"/>
            </xsl:otherwise>
         </xsl:choose>
      </xsl:variable>
      
      <xsl:choose>
         <xsl:when test="$chunk.id=ancestor::*[1]/@id">
            <a name="X"/>
            <b style="background-color: #FFCC66">
               <xsl:apply-templates select="." mode="text-only"/>
            </b>
         </xsl:when>
         <xsl:otherwise>
            <a>
               <xsl:attribute name="href">
                  <xsl:value-of select="$doc.path"/>;chunk.id=<xsl:value-of select="ancestor::*[1]/@id"/>;toc.depth=<xsl:value-of select="$toc.depth"/>;toc.id=<xsl:value-of select="$local.toc.id"/>;brand=<xsl:value-of select="$brand"/><xsl:value-of select="$search"/><xsl:call-template name="create.anchor"/>
               </xsl:attribute>
               <xsl:attribute name="target">_top</xsl:attribute>
               <xsl:apply-templates select="." mode="text-only"/>
            </a>
         </xsl:otherwise>
      </xsl:choose>
      
   </xsl:template>
   
   <xsl:template name="hitSummary">
      
      <xsl:variable name="sum">
         <xsl:choose>
            <xsl:when test="($query != '0') and ($query != '')">
               <xsl:value-of select="number(/TEI.2/@xtf:hitCount)"/>
            </xsl:when>
            <xsl:otherwise>0</xsl:otherwise>
         </xsl:choose>
      </xsl:variable>
      <xsl:variable name="occur">
         <xsl:choose>
            <xsl:when test="$sum != 1">occurrences</xsl:when>
            <xsl:otherwise>occurrence</xsl:otherwise>
         </xsl:choose>
      </xsl:variable>
      
      <div align="center">
         <b>
            <b style="color: red">
               <xsl:value-of select="$sum"/>
            </b>
            <xsl:text>&#160;</xsl:text>
            <xsl:value-of select="$occur"/>
            <xsl:text> of </xsl:text>
            <b style="color: red">
               <xsl:value-of select="$query"/>
            </b>
         </b>
         <br/>
         <xsl:text> [</xsl:text>
         <a>
            <xsl:attribute name="href">
               <xsl:value-of select="$doc.path"/>;chunk.id=<xsl:value-of select="$chunk.id"/>;toc.depth=<xsl:value-of select="$toc.depth"/>;toc.id=<xsl:value-of select="$toc.id"/>;brand=<xsl:value-of select="$brand"/>
            </xsl:attribute>
            <xsl:attribute name="target">_top</xsl:attribute>
            <xsl:text>Clear Hits</xsl:text>
         </a>
         <xsl:text>]</xsl:text>
      </div>
      
   </xsl:template>
   
   <!-- templates for expanding and collapsing single nodes -->
   <xsl:template name="expand">
      <xsl:variable name="local.toc.id" select="@id"/>
      <a>
         <xsl:attribute name="href">
            <xsl:value-of select="$doc.path"/>;chunk.id=<xsl:value-of select="$chunk.id"/>;toc.id=<xsl:value-of select="$local.toc.id"/>;brand=<xsl:value-of select="$brand"/><xsl:value-of select="$search"/>
         </xsl:attribute>
         <xsl:attribute name="target">_top</xsl:attribute>
         <img src="{$icon.path}i_expand.gif" border="0" alt="expand section"/>
      </a>
   </xsl:template>
   
   <xsl:template name="collapse">
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
         <xsl:attribute name="href">
            <xsl:value-of select="$doc.path"/>;chunk.id=<xsl:value-of select="$chunk.id"/>;toc.id=<xsl:value-of select="$local.toc.id"/>;brand=<xsl:value-of select="$brand"/><xsl:value-of select="$search"/>
         </xsl:attribute>
         <xsl:attribute name="target">_top</xsl:attribute>
         <img src="{$icon.path}i_colpse.gif" border="0" alt="collapse section"/>
      </a>
   </xsl:template>
   
   <!-- expand or collapse entire hierarchy -->
   <xsl:template name="expandAll">
      <hr/>
      <div align="center">
         <span style="color: #6699CC">
            <a style="text-decoration: none">
               <xsl:attribute name="href">
                  <xsl:value-of select="$doc.path"/>;chunk.id=<xsl:value-of select="$chunk.id"/>;toc.depth=<xsl:value-of select="1"/>;brand=<xsl:value-of select="$brand"/><xsl:value-of select="$search"/>
               </xsl:attribute>
               <xsl:attribute name="target">_top</xsl:attribute>
               <img src="{$icon.path}i_colpse.gif" border="0" alt="collapse section"/>
               <xsl:text> Collapse All</xsl:text>
            </a>
            <xsl:text> | </xsl:text>
            <a style="text-decoration: none">
               <xsl:attribute name="href">
                  <xsl:value-of select="$doc.path"/>;chunk.id=<xsl:value-of select="$chunk.id"/>;toc.depth=<xsl:value-of select="100"/>;brand=<xsl:value-of select="$brand"/><xsl:value-of select="$search"/>
               </xsl:attribute>
               <xsl:attribute name="target">_top</xsl:attribute>
               <xsl:text>Expand All </xsl:text>
               <img src="{$icon.path}i_expand.gif" border="0" alt="expand section"/>
            </a>
         </span>
      </div>
   </xsl:template>
   
   <!-- used to extract the text of titles without <lb/>'s and other formatting -->
   <xsl:template match="text()" mode="text-only">
      <xsl:value-of select="."/>
   </xsl:template>
   
   <xsl:template match="lb" mode="text-only">
      <xsl:text>&#160;</xsl:text>
   </xsl:template>
   
</xsl:stylesheet>
