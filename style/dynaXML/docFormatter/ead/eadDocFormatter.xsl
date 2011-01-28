<xsl:stylesheet version="2.0" 
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:xtf="http://cdlib.org/xtf"
   xmlns="http://www.w3.org/1999/xhtml"
   xmlns:session="java:org.cdlib.xtf.xslt.Session"
   extension-element-prefixes="session"
   exclude-result-prefixes="#all">
   
   <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
   <!-- EAD dynaXML Stylesheet                                                 -->
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
      NOTE: This is rough adaptation of the EAD Cookbook stylesheets to get them 
      to work with XTF. It should in no way be considered a production interface 
   -->
   
   <!-- ====================================================================== -->
   <!-- Import Common Templates                                                -->
   <!-- ====================================================================== -->
   
   <xsl:import href="../common/docFormatterCommon.xsl"/>
   
   <!-- ====================================================================== -->
   <!-- Output Format                                                          -->
   <!-- ====================================================================== -->
   
   <xsl:output method="xhtml" indent="yes" 
      encoding="UTF-8" media-type="text/html; charset=UTF-8" 
      doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN" 
      doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"
      exclude-result-prefixes="#all"
      omit-xml-declaration="yes"/>
   
   <xsl:output name="frameset" method="xhtml" indent="yes" 
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
   
   <xsl:include href="eadcbs7.xsl"/>
   <xsl:include href="parameter.xsl"/>
   <xsl:include href="search.xsl"/>
   
   <!-- ====================================================================== -->
   <!-- Define Keys                                                            -->
   <!-- ====================================================================== -->
   
   <xsl:key name="chunk-id" match="*[parent::archdesc or matches(local-name(), '^(c|c[0-9][0-9])$')][@id]" use="@id"/>
   
   <!-- ====================================================================== -->
   <!-- EAD-specific parameters                                                -->
   <!-- ====================================================================== -->

   <!-- If a query was specified but no particular hit rank, jump to the first hit 
        (in document order) 
   -->
   <xsl:param name="hit.num" select="'0'"/>
   
   <xsl:param name="hit.rank">
      <xsl:choose>
         <xsl:when test="$hit.num != '0'">
            <xsl:value-of select="key('hit-num-dynamic', string($hit.num))/@rank"/>
         </xsl:when>
         <xsl:when test="$query and not($query = '0')">
            <xsl:value-of select="key('hit-num-dynamic', '1')/@rank"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:value-of select="'0'"/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:param>
   
   <!-- To support direct links from snippets, the following two parameters must check value of $hit.rank -->
   <xsl:param name="chunk.id">
      <xsl:choose>
         <xsl:when test="$hit.rank != '0'">
            <xsl:call-template name="findHitChunk">
               <xsl:with-param name="hitNode" select="key('hit-rank-dynamic', string($hit.rank))"/>
            </xsl:call-template>
         </xsl:when>
         <xsl:otherwise>
            <xsl:value-of select="'0'"/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:param>
   
   <!-- ====================================================================== -->
   <!-- Root Template                                                          -->
   <!-- ====================================================================== -->
   
   <xsl:template match="/ead">
      <xsl:choose>
         <!-- robot solution -->
         <xsl:when test="matches($http.user-agent,$robots)">
            <xsl:call-template name="robot"/>
         </xsl:when>
         <!-- Creates the button bar.-->
         <xsl:when test="$doc.view = 'bbar'">
            <xsl:call-template name="bbar"/>
         </xsl:when>
         <!-- Creates the basic table of contents.-->
         <xsl:when test="$doc.view = 'toc'">
            <xsl:call-template name="toc"/>
         </xsl:when>
         <!-- Creates the body of the finding aid.-->
         <xsl:when test="$doc.view = 'content'">
            <xsl:call-template name="body"/>
         </xsl:when>
         <!-- print view -->
         <xsl:when test="$doc.view='print'">
            <xsl:call-template name="print"/>
         </xsl:when>
         <!-- citation -->
         <xsl:when test="$doc.view='citation'">
            <xsl:call-template name="citation"/>
         </xsl:when>
         <!-- Creates the basic frameset.-->
         <xsl:otherwise>
            <xsl:call-template name="frameset"/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Frameset Template                                                      -->
   <!-- ====================================================================== -->
   
   <xsl:template name="frameset">
      <xsl:variable name="bbar.href"><xsl:value-of select="$query.string"/>;doc.view=bbar;brand=<xsl:value-of select="$brand"/><xsl:value-of select="$search"/></xsl:variable> 
      <xsl:variable name="toc.href"><xsl:value-of select="$query.string"/>;doc.view=toc;brand=<xsl:value-of select="$brand"/>;chunk.id=<xsl:value-of select="$chunk.id"/>;<xsl:value-of select="$search"/>#X</xsl:variable>
      <xsl:variable name="content.href"><xsl:value-of select="$query.string"/>;doc.view=content;brand=<xsl:value-of select="$brand"/>;chunk.id=<xsl:value-of select="$chunk.id"/><xsl:value-of select="$search"/></xsl:variable>
      
      <xsl:result-document format="frameset" exclude-result-prefixes="#all">
         <html xml:lang="en" lang="en">
            <head>
               <link rel="stylesheet" type="text/css" href="{$css.path}ead.css"/>
               <link rel="shortcut icon" href="icons/default/favicon.ico" />

               
               <title>
                  <xsl:value-of select="eadheader/filedesc/titlestmt/titleproper"/>
                  <xsl:text>  </xsl:text>
                  <xsl:value-of select="eadheader/filedesc/titlestmt/subtitle"/>
               </title>
            </head>
            
            <frameset rows="120,*">
               <frame frameborder="1" scrolling="no" title="Navigation Bar">
                  <xsl:attribute name="name">bbar</xsl:attribute>
                  <xsl:attribute name="src"><xsl:value-of select="$xtfURL"/><xsl:value-of select="$dynaxmlPath"/>?<xsl:value-of select="$bbar.href"/></xsl:attribute>
               </frame>
               <frameset cols="35%,65%">
                  <frame frameborder="1" title="Table of Contents">
                     <xsl:attribute name="name">toc</xsl:attribute>
                     <xsl:attribute name="src"><xsl:value-of select="$xtfURL"/><xsl:value-of select="$dynaxmlPath"/>?<xsl:value-of select="$toc.href"/></xsl:attribute>
                  </frame>
                  <frame frameborder="1" title="Content">
                     <xsl:attribute name="name">content</xsl:attribute>
                     <xsl:attribute name="src"><xsl:value-of select="$xtfURL"/><xsl:value-of select="$dynaxmlPath"/>?<xsl:value-of select="$content.href"/>#X</xsl:attribute>
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
   <!-- TOC Templates                                                          -->
   <!-- ====================================================================== -->
   
   <xsl:template name="toc">
      <xsl:variable name="sum">
         <xsl:choose>
            <xsl:when test="($query != '0') and ($query != '')">
               <xsl:value-of select="number(/*[1]/@xtf:hitCount)"/>
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
      
      <xsl:call-template name="translate">
         <xsl:with-param name="resultTree">
            <html xml:lang="en" lang="en">
               <head>
                  <base target="body"/>
                  <link rel="stylesheet" type="text/css" href="{$css.path}toc.css"/>
               </head>
               <body>
                  
                  <div class="toc">
                     <table>
                        <tr>
                           <td height="25">
                              <b>
                                 <xsl:attribute name="target">_top</xsl:attribute>
                                 <xsl:value-of select="$doc.title"/>
                              </b>
                           </td>
                        </tr>
                     </table>
                     
                     <xsl:if test="($query != '0') and ($query != '')">
                        <hr/>
                        <div align="center">
                           <b>
                              <span class="hit-count">
                                 <xsl:value-of select="$sum"/>
                              </span>
                              <xsl:text> </xsl:text>
                              <xsl:value-of select="$occur"/>
                              <xsl:text> of </xsl:text>
                              <span class="hit-count">
                                 <xsl:value-of select="$query"/>
                              </span>
                           </b>
                           <br/>
                           <xsl:text> [</xsl:text>
                           <a>
                              <xsl:attribute name="href">
                                 <xsl:value-of select="$doc.path"/>;chunk.id=<xsl:value-of select="$chunk.id"/>;toc.depth=<xsl:value-of select="$toc.depth"/>;brand=<xsl:value-of select="$brand"/>
                              </xsl:attribute>
                              <xsl:attribute name="target">_top</xsl:attribute>
                              <xsl:text>Clear Hits</xsl:text>
                           </a>
                           <xsl:text>]</xsl:text>
                        </div>
                     </xsl:if>
                     <hr/>
                     
                     <br/>
                     <!-- The Table of Contents template performs a series of tests to
                        determine which elements will be included in the table
                        of contents.  Each if statement tests to see if there is
                        a matching element with content in the finding aid.-->
                     <xsl:if test="archdesc/did">
                        <xsl:call-template name="make-toc-link">
                           <xsl:with-param name="name" select="'Descriptive Summary'"/>
                           <xsl:with-param name="id" select="'headerlink'"/>
                           <xsl:with-param name="nodes" select="archdesc/did"/>
                        </xsl:call-template>
                     </xsl:if>
                     <xsl:if test="archdesc/did/head">
                        <xsl:apply-templates select="archdesc/did/head" mode="tocLink"/>
                     </xsl:if>
                     <xsl:if test="archdesc/bioghist/head">
                        <xsl:apply-templates select="archdesc/bioghist/head" mode="tocLink"/>
                     </xsl:if>
                     <xsl:if test="archdesc/scopecontent/head">
                        <xsl:apply-templates select="archdesc/scopecontent/head" mode="tocLink"/>
                     </xsl:if>
                     <xsl:if test="archdesc/arrangement/head">
                        <xsl:apply-templates select="archdesc/arrangement/head" mode="tocLink"/>
                     </xsl:if>
                     
                     <xsl:if test="archdesc/userestrict/head   or archdesc/accessrestrict/head   or archdesc/*/userestrict/head   or archdesc/*/accessrestrict/head">
                        <xsl:call-template name="make-toc-link">
                           <xsl:with-param name="name" select="'Restrictions'"/>
                           <xsl:with-param name="id" select="'restrictlink'"/>
                           <xsl:with-param name="nodes" select="archdesc/userestrict|archdesc/accessrestrict|archdesc/*/userestrict|archdesc/*/accessrestrict"/>
                        </xsl:call-template>
                     </xsl:if>
                     <xsl:if test="archdesc/controlaccess/head">
                        <xsl:apply-templates select="archdesc/controlaccess/head" mode="tocLink"/>
                     </xsl:if>
                     <xsl:if test="archdesc/relatedmaterial   or archdesc/separatedmaterial   or archdesc/*/relatedmaterial   or archdesc/*/separatedmaterial">
                        <xsl:call-template name="make-toc-link">
                           <xsl:with-param name="name" select="'Related Material'"/>
                           <xsl:with-param name="id" select="'relatedmatlink'"/>
                           <xsl:with-param name="nodes" select="archdesc/relatedmaterial|archdesc/separatedmaterial|archdesc/*/relatedmaterial|archdesc/*/separatedmaterial"/>
                        </xsl:call-template>
                     </xsl:if>
                     <xsl:if test="archdesc/acqinfo/*   or archdesc/processinfo/*   or archdesc/prefercite/*   or archdesc/custodialhist/*   or archdesc/processinfo/*   or archdesc/appraisal/*   or archdesc/accruals/*   or archdesc/*/acqinfo/*   or archdesc/*/processinfo/*   or archdesc/*/prefercite/*   or archdesc/*/custodialhist/*   or archdesc/*/procinfo/*   or archdesc/*/appraisal/*   or archdesc/*/accruals/*">
                        <xsl:call-template name="make-toc-link">
                           <xsl:with-param name="name" select="'Administrative Information'"/>
                           <xsl:with-param name="id" select="'adminlink'"/>
                           <xsl:with-param name="nodes" select="archdesc/acqinfo|archdesc/prefercite|archdesc/custodialhist|archdesc/custodialhist|archdesc/processinfo|archdesc/appraisal|archdesc/accruals|archdesc/*/acqinfo|archdesc/*/processinfo|archdesc/*/prefercite|archdesc/*/custodialhist|archdesc/*/procinfo|archdesc/*/appraisal|archdesc/*/accruals/*"/>
                        </xsl:call-template>
                     </xsl:if>
                     
                     <xsl:if test="archdesc/otherfindaid/head    or archdesc/*/otherfindaid/head">
                        <xsl:choose>
                           <xsl:when test="archdesc/otherfindaid/head">
                              <xsl:apply-templates select="archdesc/otherfindaid/head" mode="tocLink"/>
                           </xsl:when>
                           <xsl:when test="archdesc/*/otherfindaid/head">
                              <xsl:apply-templates select="archdesc/*/otherfindaid/head" mode="tocLink"/>
                           </xsl:when>
                        </xsl:choose>
                     </xsl:if>
                     
                     <!--The next test covers the situation where there is more than one odd element
                        in the document.-->
                     <xsl:for-each select="archdesc/odd">
                        <xsl:call-template name="make-toc-link">
                           <xsl:with-param name="name" select="head"/>
                           <xsl:with-param name="id" select="@id"/>
                           <xsl:with-param name="nodes" select="."/>
                        </xsl:call-template>
                     </xsl:for-each>
                     
                     <xsl:if test="archdesc/bibliography/head    or archdesc/*/bibliography/head">
                        <xsl:choose>
                           <xsl:when test="archdesc/bibliography/head">
                              <xsl:apply-templates select="archdesc/bibliography/head" mode="tocLink"/>
                           </xsl:when>
                           <xsl:when test="archdesc/*/bibliography/head">
                              <xsl:apply-templates select="archdesc/*/bibliography/head" mode="tocLink"/>
                           </xsl:when>
                        </xsl:choose>
                     </xsl:if>
                     
                     <xsl:if test="archdesc/index/head    or archdesc/*/index/head">
                        <xsl:choose>
                           <xsl:when test="archdesc/index/head">
                              <xsl:apply-templates select="archdesc/index/head" mode="tocLink"/>
                           </xsl:when>
                           <xsl:when test="archdesc/*/index/head">
                              <xsl:apply-templates select="archdesc/*/index/head" mode="tocLink"/>
                           </xsl:when>
                        </xsl:choose>
                     </xsl:if>
                     
                     <xsl:if test="archdesc/dsc/head">
                        <xsl:apply-templates select="archdesc/dsc/head" mode="tocLink"/>
                        <!-- Displays the unittitle and unitdates for a c01 if it is a series (as
                           evidenced by the level attribute series)and numbers them
                           to form a hyperlink to each.   Delete this section if you do not
                           wish the c01 titles to appear in the table of contents.-->
                        <xsl:for-each select="archdesc/dsc/c01[@level='series' or @level='subseries' or @level='subgrp' or @level='subcollection']">
                           <xsl:call-template name="make-toc-link">
                              <xsl:with-param name="name">
                                 <xsl:choose>
                                    <xsl:when test="did/unittitle/unitdate">
                                       <xsl:for-each select="did/unittitle">
                                          <xsl:value-of select="text()"/>
                                          <xsl:text> </xsl:text>
                                          <xsl:apply-templates select="./unitdate"/>
                                       </xsl:for-each>
                                    </xsl:when>
                                    <xsl:otherwise>
                                       <xsl:apply-templates select="did/unittitle"/>
                                       <xsl:text> </xsl:text>
                                       <xsl:apply-templates select="did/unitdate"/>
                                    </xsl:otherwise>
                                 </xsl:choose>
                              </xsl:with-param>
                              <xsl:with-param name="id" select="@id"/>
                              <xsl:with-param name="nodes" select="."/>
                              <xsl:with-param name="indent" select="2"/>
                           </xsl:call-template>
                           
                           <!-- Displays the unittitle and unitdates for each c02 if it is a subseries 
                              (as evidenced by the level attribute series) and forms a hyperlink to each.   
                              Delete this section if you do not wish the c02 titles to appear in the 
                              table of contents. -->
                           <xsl:for-each select="c02[@level='subseries']">
                              <xsl:call-template name="make-toc-link">
                                 <xsl:with-param name="name">
                                    <xsl:choose>
                                       <xsl:when test="did/unittitle/unitdate">
                                          <xsl:for-each select="did/unittitle">
                                             <xsl:value-of select="text()"/>
                                             <xsl:text> </xsl:text>
                                             <xsl:apply-templates select="./unitdate"/>
                                          </xsl:for-each>
                                       </xsl:when>
                                       <xsl:otherwise>
                                          <xsl:apply-templates select="did/unittitle"/>
                                          <xsl:text> </xsl:text>
                                          <xsl:apply-templates select="did/unitdate"/>
                                       </xsl:otherwise>
                                    </xsl:choose>
                                 </xsl:with-param>
                                 <xsl:with-param name="id" select="@id"/>
                                 <xsl:with-param name="nodes" select="."/>
                                 <xsl:with-param name="indent" select="3"/>
                              </xsl:call-template>
                           </xsl:for-each>
                           <!--This ends the section that causes the c02 titles to appear in the table of contents.-->
                        </xsl:for-each>
                        <!--This ends the section that causes the c01 titles to appear in the table of contents.-->
                     </xsl:if>
                     <!--End of the table of contents. -->
                  </div>
               </body>
            </html>
         </xsl:with-param>
      </xsl:call-template>
   </xsl:template>
   
   <xsl:template match="node()" mode="tocLink">
      <xsl:call-template name="make-toc-link">
         <xsl:with-param name="name" select="string(.)"/>
         <xsl:with-param name="id" select="ancestor-or-self::*[@id][1]/@id"/>
         <xsl:with-param name="nodes" select="parent::*"/>
      </xsl:call-template>
   </xsl:template>
   
   <xsl:template name="make-toc-link">
      <xsl:param name="name"/>
      <xsl:param name="id"/>
      <xsl:param name="nodes"/>
      <xsl:param name="indent" select="1"/>
      
      <xsl:variable name="hit.count" select="sum($nodes/@xtf:hitCount)"/>
      <xsl:variable name="content.href"><xsl:value-of select="$query.string"/>;chunk.id=<xsl:value-of select="$id"/>;brand=<xsl:value-of select="$brand"/><xsl:value-of select="$search"/></xsl:variable>

      <xsl:if test="@id = $chunk.id">
         <a name="X"/>
      </xsl:if>

      <table border="0" cellpadding="1" cellspacing="0" width="820">
         <tr>
            <td align="right">
               <xsl:attribute name="width" select="20 * $indent"/>
               <span class="hit-count">
                  <xsl:if test="$hit.count &gt; 0">
                     <xsl:value-of select="$hit.count"/>
                  </xsl:if>  
               </span>
            </td>
            <td align="left" valign="top" width="700">
               <nobr>
                  <xsl:choose>
                     <xsl:when test="$chunk.id = @id">
                        <a name="X"/>
                        <span class="toc-hi">
                           <xsl:value-of select="$name"/>
                        </span>
                     </xsl:when>
                     <xsl:otherwise>
                        <a>
                           <xsl:attribute name="href">
                              <xsl:value-of select="$xtfURL"/><xsl:value-of select="$dynaxmlPath"/>?<xsl:value-of select="$content.href"/>
                           </xsl:attribute>
                           <xsl:attribute name="target">_top</xsl:attribute>
                           <xsl:value-of select="$name"/>
                        </a>
                     </xsl:otherwise>
                  </xsl:choose>
               </nobr>
            </td>
         </tr>
      </table>
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
         </head>
         <body>
            <hr/>
            <div align="center">
               <table width="95%">
                  <tr>
                     <td>
                        <xsl:call-template name="body"/>
                     </td>
                  </tr>
               </table>
            </div>
            <hr/>
         </body>
      </html>
   </xsl:template>
   
</xsl:stylesheet>
