<xsl:stylesheet version="2.0" 
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:xtf="http://cdlib.org/xtf"
   xmlns:session="java:org.cdlib.xtf.xslt.Session"
   xmlns:editURL="http://cdlib.org/xtf/editURL"
   xmlns:local="http://local"
   xmlns="http://www.w3.org/1999/xhtml"
   extension-element-prefixes="session"
   exclude-result-prefixes="#all">
   
   <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
   <!-- BookReader dynaXML Stylesheet                                          -->
   <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
   
   <!--
      Copyright (c) 2010, Regents of the University of California
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
      NOTE: This is a stab at providing XTF access to scanned books, using the
      Open Library BookReader.
   -->
   
   <!-- ====================================================================== -->
   <!-- Import Common Templates                                                -->
   <!-- ====================================================================== -->
   
   <xsl:import href="../common/docFormatterCommon.xsl"/>
   <xsl:import href="../../../xtfCommon/xtfCommon.xsl"/>
   
   <!-- ====================================================================== -->
   <!-- Output Format                                                          -->
   <!-- ====================================================================== -->
   
   <xsl:output method="xhtml" indent="yes" 
      encoding="UTF-8" media-type="text/html; charset=UTF-8" 
      doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN" 
      doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd" 
      exclude-result-prefixes="#all"
      omit-xml-declaration="yes"/>
   
   <!-- ====================================================================== -->
   <!-- Strip Space                                                            -->
   <!-- ====================================================================== -->
   
   <xsl:strip-space elements="*"/>
   
   <!-- ====================================================================== -->
   <!-- Included Stylesheets                                                   -->
   <!-- ====================================================================== -->
   
   <xsl:include href="search.xsl"/>
   
   <!-- ====================================================================== -->
   <!-- Define Keys                                                            -->
   <!-- ====================================================================== -->
   
   <xsl:key name="div-id" match="sec" use="@id"/>
   
   <!-- ====================================================================== -->
   <!-- Define Parameters                                                      -->
   <!-- ====================================================================== -->
   
   <xsl:param name="root.URL"/>
   <xsl:param name="doc.title" select="/xtf-converted-book/xtf:meta/title"/>
   <xsl:param name="servlet.dir"/>
   <!-- for docFormatterCommon.xsl -->
   <xsl:param name="css.path" select="'css/default/'"/>
   <xsl:param name="icon.path" select="'css/default/'"/>
   
   <!-- ====================================================================== -->
   <!-- Root Template                                                          -->
   <!-- ====================================================================== -->
   
   <xsl:template match="/">
      <xsl:choose>
         <!-- robot solution -->
         <xsl:when test="matches($http.user-agent,$robots)">
            <xsl:call-template name="robot"/>
         </xsl:when>
         <xsl:when test="$doc.view='citation'">
            <xsl:call-template name="citation"/>
         </xsl:when>
         <xsl:when test="$doc.view='print'">
            <xsl:call-template name="print"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:call-template name="content"/>
         </xsl:otherwise>
      </xsl:choose>
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
            <link rel="shortcut icon" href="icons/default/favicon.ico" />
            <link rel="stylesheet" type="text/css" href="{$root.URL}css/bookreader/BookReader.css"/>
            <link rel="stylesheet" type="text/css" href="{$root.URL}css/bookreader/BookReaderXTF.css"/>
            <script type="text/javascript" src="{$root.URL}script/bookreader/jquery-1.2.6.min.js"></script>
            <script type="text/javascript" src="{$root.URL}script/bookreader/jquery.easing.1.3.js"></script>
            <script type="text/javascript" src="{$root.URL}script/bookreader/BookReader.js"></script>    
            <script type="text/javascript" src="{$root.URL}script/bookreader/BookReader-xtf.js"></script>    
            <script type="text/javascript" src="{$root.URL}script/bookreader/dragscrollable.js"></script>
            <link rel="stylesheet" type="text/css" href="{$root.URL}css/default/bbar.css" /> 
         </head>
         <body>
            <!-- Standard button bar, but not using frames -->
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
                           <input type="hidden" name="hit.rank" value="1"/>
                           <input type="submit" value="Search this Item"/>
                        </form>
                     </td>
                     <td class="right">
                        <a>
                           <xsl:attribute name="href">javascript://</xsl:attribute>
                           <xsl:attribute name="onclick">
                              <xsl:text>javascript:window.open('</xsl:text><xsl:value-of select="$xtfURL"/><xsl:value-of select="$dynaxmlPath"/><xsl:text>?docId=</xsl:text><xsl:value-of
                                 select="editURL:protectValue($docId)"/><xsl:text>;doc.view=citation</xsl:text><xsl:text>','popup','width=800,height=400,resizable=yes,scrollbars=no')</xsl:text>
                           </xsl:attribute>
                           <xsl:text>Citation</xsl:text>
                        </a>
                     </td>
                  </tr>
               </table>
            </div>
            
            <xsl:choose>
               <xsl:when test="$query">
                  <div id="BookReader" style="left:6px; right:196px; top:110px; bottom:6px;">x</div> 
                  <div id="BookReaderSearch" style="width:190px; right:6px; top:110px; bottom:6px;"> 
                     <div id="BookReaderSearchResults"> 
                        Search results
                     </div> 
                  </div> 
               </xsl:when>
               <xsl:otherwise>
                  <div id="BookReader" style="left:6px; right:6px; top:110px; bottom:6px;">x</div> 
               </xsl:otherwise>
            </xsl:choose>
            
            <!-- Dynamic javascript for this book -->
            <script type="text/javascript">
               
               <xsl:comment> Dynamic javascript with specific page parameters for this book. </xsl:comment>
               
               <!-- If no query, start on the title page if there is one -->
               <xsl:for-each select="/*/leaf[@access='true']">
                  <xsl:if test="@type = 'Title Page'">
                     br.titleLeaf = <xsl:value-of select="@leafNum"/>;
                  </xsl:if>
               </xsl:for-each>
               
               br.xtfDocDir = "<xsl:value-of select="$doc.dir"/>";
               
               br.pages = [
                  <xsl:variable name="quote" select="'&quot;'"/>
                  <xsl:for-each select="/*/leaf[@access='true']">new br.Page(<xsl:value-of select="
                     concat(
                        cropBox/@w, ',', 
                        cropBox/@h, ',',
                        $quote, @imgFile, $quote, ',',
                        @leafNum,   ',',
                        position() - 1, ',',
                        if (@pageNum &gt; 0) then @pageNum else 'null')"/>),
                  </xsl:for-each>
               ];
               
               // Override the default doSearch
               br.doSearch = function() {
                  this.updateSearchResults([
                     <xsl:apply-templates select="/*/leaf[@xtf:hitCount]//(xtf:hit|xtf:more)" mode="search-results"/>
                  ]);
               }
               
               br.numLeafs /*sic*/ = br.pages.length;
               
               br.bookTitle= "<xsl:value-of select="local:unquote($doc.title)"/>";
               br.bookUrl  = '<xsl:value-of select="concat($xtfURL, $dynaxmlPath, '?docId=', editURL:protectValue($docId))"/>';
               
               br.init();
               
               // $$$ hack to workaround sizing bug when starting in two-up mode
               $(document).ready(function() {
                  $(window).trigger('resize');
                  <xsl:if test="$query">
                     br.doSearch();
                     <xsl:choose>
                        <xsl:when test="$hit.rank">
                           br.jumpToSearchResult(<xsl:value-of select="$hit.rank"/>);
                        </xsl:when>
                        <xsl:otherwise>
                           br.jumpToSearchResult(<xsl:value-of select="1"/>);
                        </xsl:otherwise>
                     </xsl:choose>
                  </xsl:if>
               });
               
            </script>
            
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
            <link rel="shortcut icon" href="icons/default/favicon.ico" />
            <link rel="stylesheet" type="text/css" href="{$root.URL}/css/bookreader/BookReader.css"/>
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
