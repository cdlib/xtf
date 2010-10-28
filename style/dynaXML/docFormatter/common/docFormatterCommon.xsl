<xsl:stylesheet version="2.0" 
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:xtf="http://cdlib.org/xtf"
   xmlns="http://www.w3.org/1999/xhtml"
   xmlns:session="java:org.cdlib.xtf.xslt.Session"
   extension-element-prefixes="session"
   exclude-result-prefixes="#all">
   
   <!-- ====================================================================== -->
   <!-- Common DynaXML Stylesheet                                              -->
   <!-- ====================================================================== -->
   
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
   <!-- Import Stylesheets                                                     -->
   <!-- ====================================================================== -->
   
   <xsl:import href="../../../xtfCommon/xtfCommon.xsl"/>
   
   <!-- ====================================================================== -->
   <!-- Global Keys                                                            -->
   <!-- ====================================================================== -->
   
   <xsl:key name="hit-num-dynamic" match="xtf:hit" use="@hitNum"/>
   <xsl:key name="hit-rank-dynamic" match="xtf:hit" use="@rank"/>
   
   <!-- ====================================================================== -->
   <!-- Global Parameters                                                      -->
   <!-- ====================================================================== -->
   
   <!-- Path Parameters -->
   
   <xsl:param name="servlet.path"/>
   <xsl:param name="root.path"/>
   <xsl:param name="xtfURL" select="$root.path"/>
   <xsl:param name="dynaxmlPath" select="if (matches($servlet.path, 'org.cdlib.xtf.crossQuery.CrossQuery')) then 'org.cdlib.xtf.dynaXML.DynaXML' else 'view'"/>
   
   <xsl:param name="docId"/>
   <xsl:param name="docPath" select="replace($docId, '[^/]+\.xml$', '')"/>
   
   <!-- If an external 'source' document was specified, include it in the
      query string of links we generate. -->
   <xsl:param name="source" select="''"/>
   
   <xsl:variable name="sourceStr">
      <xsl:if test="$source">;source=<xsl:value-of select="$source"/></xsl:if>
   </xsl:variable>
   
   <xsl:param name="query.string" select="concat('docId=', $docId, $sourceStr)"/>
   
   <xsl:param name="doc.path"><xsl:value-of select="$xtfURL"/><xsl:value-of select="$dynaxmlPath"/>?<xsl:value-of select="$query.string"/></xsl:param>
   
   <xsl:variable name="systemId" select="saxon:systemId()" xmlns:saxon="http://saxon.sf.net/"/>
   
   <xsl:param name="doc.dir">
      <xsl:choose>
         <xsl:when test="starts-with($systemId, 'http://')">
            <xsl:value-of select="replace($systemId, '/[^/]*$', '')"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:value-of select="concat($xtfURL, 'data/', $docPath)"/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:param>
   
   <xsl:param name="figure.path" select="concat($doc.dir, '/figures/')"/>
   
   <xsl:param name="pdf.path" select="concat($doc.dir, '/pdfs/')"/>
   
   <!-- navigation parameters -->
   
   <xsl:param name="doc.view" select="'0'"/>
   
   <xsl:param name="toc.depth" select="1"/>
   
   <xsl:param name="anchor.id" select="'0'"/>
   
   <xsl:param name="set.anchor" select="'0'"/>
   
   <xsl:param name="chunk.id"/>
      
   <xsl:param name="toc.id"/>
   
   <!-- search parameters -->
   
   <xsl:param name="query"/>
   <xsl:param name="query-join"/>
   <xsl:param name="query-exclude"/>
   <xsl:param name="sectionType"/>
   
   <xsl:param name="search">
      <xsl:if test="$query">
         <xsl:value-of select="concat(';query=', replace($query, ';', '%26'))"/>
      </xsl:if>
      <xsl:if test="$query-join">
         <xsl:value-of select="concat(';query-join=', $query-join)"/>
      </xsl:if>
      <xsl:if test="$query-exclude">
         <xsl:value-of select="concat(';query-exclude=', $query-exclude)"/>
      </xsl:if>
      <xsl:if test="$sectionType">
         <xsl:value-of select="concat(';sectionType=', $sectionType)"/>
      </xsl:if>
   </xsl:param>  
   
   <xsl:param name="hit.rank" select="'0'"/>
   
   <!-- Branding Parameters -->
   
   <xsl:param name="brand" select="'default'"/>
   
   <xsl:variable name="brand.file">
      <xsl:choose>
         <xsl:when test="$brand != ''">
            <xsl:copy-of select="document(concat('../../../../brand/',$brand,'.xml'))"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:copy-of select="document('../../../../brand/default.xml')"/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:variable>
   
   <xsl:param name="brand.links" select="$brand.file/brand/dynaxml.links/*" xpath-default-namespace="http://www.w3.org/1999/xhtml"/>
   <xsl:param name="brand.header" select="$brand.file/brand/dynaxml.header/*" xpath-default-namespace="http://www.w3.org/1999/xhtml"/>
   <xsl:param name="brand.footer" select="$brand.file/brand/footer/*" xpath-default-namespace="http://www.w3.org/1999/xhtml"/>
   
   <!-- Special Robot Parameters -->
   
   <xsl:param name="http.user-agent"/>
   <!-- WARNING: Inclusion of 'Wget' is for testing only, please remove before going into production -->
   <xsl:param name="robots" select="'Googlebot|Slurp|msnbot|Teoma|Wget'"/>
   
   <!-- ====================================================================== -->
   <!-- Button Bar Templates                                                   -->
   <!-- ====================================================================== -->
   
   <xsl:template name="bbar">
      <xsl:call-template name="translate">
         <xsl:with-param name="resultTree">
            <html xml:lang="en" lang="en">
               <head>
                  <title>
                     <xsl:value-of select="$doc.title"/>
                  </title>
                  <link rel="stylesheet" type="text/css" href="{$css.path}bbar.css"/>
                  <link rel="shortcut icon" href="icons/default/favicon.ico" />


               </head>
               <body>
                  <div class="bbar">
                     <table border="0" cellpadding="0" cellspacing="0">
                        <tr>
                           <td colspan="3" align="center">
                              <xsl:copy-of select="$brand.header"/>
                           </td>
                        </tr>
                        <tr>
                           <td class="left">
                              <a href="{$xtfURL}search" target="_top">Home</a><xsl:text> | </xsl:text>
                              <xsl:choose>
                                 <xsl:when test="session:getData('queryURL')">
                                    <a href="{session:getData('queryURL')}" target="_top">Return to Search Results</a>
                                 </xsl:when>
                                 <xsl:otherwise>
                                    <span class="notActive">Return to Search Results</span>
                                 </xsl:otherwise>
                              </xsl:choose>
                           </td>
                           <td width="34%" class="center">
                              <form action="{$xtfURL}{$dynaxmlPath}" target="_top" method="get">
                                 <input name="query" type="text" size="15"/>
                                 <input type="hidden" name="docId" value="{$docId}"/>
                                 <input type="hidden" name="chunk.id" value="{$chunk.id}"/>
                                 <input type="submit" value="Search this Item"/>
                              </form>
                           </td>
                           <td class="right">
                              <a>
                                 <xsl:attribute name="href">javascript://</xsl:attribute>
                                 <xsl:attribute name="onclick">
                                    <xsl:text>javascript:window.open('</xsl:text><xsl:value-of select="$xtfURL"/><xsl:value-of select="$dynaxmlPath"/><xsl:text>?docId=</xsl:text><xsl:value-of
                                       select="$docId"/><xsl:text>;doc.view=citation</xsl:text><xsl:text>','popup','width=800,height=400,resizable=yes,scrollbars=no')</xsl:text>
                                 </xsl:attribute>
                                 <xsl:text>Citation</xsl:text>
                              </a>
                              <xsl:text> | </xsl:text>
                              <a href="{$doc.path}&#038;doc.view=print;chunk.id={$chunk.id}" target="_top">Print View</a>
                              <xsl:text> | </xsl:text>
                              <a href="javascript://" onclick="javascript:window.open('/xtf/search?smode=getLang','popup','width=500,height=200,resizable=no,scrollbars=no')">Choose Language</a>
                           </td>
                        </tr>
                     </table>
                  </div>
               </body>
            </html>
         </xsl:with-param>
      </xsl:call-template>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Citation Template                                                      -->
   <!-- ====================================================================== -->
   
   <xsl:template name="citation">
      
      <html xml:lang="en" lang="en">
         <head>
            <title>
               <xsl:value-of select="$doc.title"/>
            </title>
            <link rel="stylesheet" type="text/css" href="{$css.path}bbar.css"/>
            <link rel="shortcut icon" href="icons/default/favicon.ico" />

         </head>
         <body>
            <xsl:copy-of select="$brand.header"/>
            <div class="container">
               <h2>Citation</h2>
               <div class="citation">
                  <p><xsl:value-of select="/*/*:meta/*:creator[1]"/>. 
                     <xsl:value-of select="/*/*:meta/*:title[1]"/>. 
                     <xsl:value-of select="/*/*:meta/*:year[1]"/>.<br/>
                     [<xsl:value-of select="concat($xtfURL,$dynaxmlPath,'?docId=',$docId)"/>]</p>
                  <a>
                     <xsl:attribute name="href">javascript://</xsl:attribute>
                     <xsl:attribute name="onClick">
                        <xsl:text>javascript:window.close('popup')</xsl:text>
                     </xsl:attribute>
                     <span class="down1">Close this Window</span>
                  </a>
               </div>
            </div>
         </body>
      </html>
     
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Robot Template                                                         -->
   <!-- ====================================================================== -->
   
   <xsl:template name="robot">
      <html>
         <head>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
            <title><xsl:value-of select="//xtf:meta/title[1]"/></title>
            <link rel="shortcut icon" href="icons/default/favicon.ico" />

         </head>
         <body>
            <div>
               <xsl:apply-templates select="//text()" mode="robot"/>
            </div>
         </body>
      </html>
   </xsl:template>
   
   <xsl:template match="text()" mode="robot">
      <xsl:value-of select="."/><xsl:text> </xsl:text>
   </xsl:template>
   
</xsl:stylesheet>
