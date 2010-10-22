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
               br.bookUrl  = '<xsl:value-of select="concat($xtfURL, $dynaxmlPath, ';docId=', $docId)"/>';
               
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
   
   <!-- Workhorse to generate the box around each hit and the term(s) inside it -->
   <xsl:template match="xtf:hit | xtf:more" mode="search-results">
      
      <!-- Locate the hit, line and leaf this hit is part of -->
      <xsl:variable name="hitNum" select="@hitNum"/>
      <xsl:variable name="line" select="ancestor::line"/>
      <xsl:variable name="leaf" select="$line/ancestor::leaf"/>
      
      <!-- The top and bottom of the line are easy to find, as are the leaf dimensions. -->
      <xsl:variable name="top" select="$line/@t"/>
      <xsl:variable name="bottom" select="$line/@b"/>
      <xsl:variable name="leafWidth" select="$leaf/cropBox/@w"/>
      <xsl:variable name="leafHeight" select="$leaf/cropBox/@h"/>
      
      
      <!-- The line data contains spacing that looks like this: "23 3 14 5 16...". Here's how
         to interpret the data in this sample:
         
         23 = width of 1st word
         3 = space between 1st and 2nd words
         14 = width of 2nd word
         5 = space between 2nd and 3rd words
         16 = width of 3rd word
         ...etc...
         
         To be useful in our XSLT code, we need to split out the values into a sequence
         that we can index by position. That's what the xsl:analyze-string does here.
      -->
      <xsl:variable name="lineSpacing">
         <xsl:analyze-string select="$line/@spacing" regex="\s+">
            <xsl:non-matching-substring>
               <spacing xmlns="" width="{.}"/>
            </xsl:non-matching-substring>
         </xsl:analyze-string>
      </xsl:variable>
      
      <!-- To determine the left-hand boundary of the box for the hit or term, we
         need to figure out how many words are before it. Then we can use the
         spacing data to determine the exact coordinate. -->
      <xsl:variable name="textBeforeStart">
         <xsl:if test="local-name() = 'term'">
            <xsl:for-each select="parent::*/preceding-sibling::node()">
               <xsl:value-of select="string(.)"/>
            </xsl:for-each>
         </xsl:if>
         <xsl:for-each select="preceding-sibling::node()">
            <xsl:value-of select="string(.)"/>
         </xsl:for-each>
      </xsl:variable>
      
      <xsl:variable name="left" select="$line/@l + xtf:sumSpacing($lineSpacing, $textBeforeStart, 0)"/>
      
      <!-- Similarly, we compute the right-hand box boundary by adding the spacing for
         the words inside the hit. -->
      <xsl:variable name="textBeforeEnd" select="concat($textBeforeStart, string(.))"/>
      <xsl:variable name="right" select="$line/@l + xtf:sumSpacing($lineSpacing, $textBeforeEnd, -1)"/>
      
      <!-- Whew, that was a lot of computation. We're finally ready to generate the box. 
           We want a little padding so the yellow box is bigger than the word. 
           For some reason, the top needs a bit more than the others, and the
           bottom doesn't need any. 
      -->
      <!-- Spit out the leaf number and context. The context comes from the snippet element at the doc top -->
      { 'leaf':<xsl:value-of select="$leaf/@leafNum"/>,
        'context':"<xsl:apply-templates select="/*/xtf:snippets/xtf:snippet[@hitNum=$hitNum]" mode="hit-context"/>",
        'clientKey':<xsl:value-of select="if ($hit.rank) then @rank else @hitNum"/>,
        'l': <xsl:value-of select="max((0,            $left   - 10))"/>,
        't': <xsl:value-of select="max((0,            $top    - 10))"/>,
        'r': <xsl:value-of select="min(($leafWidth,   $right  + 10))"/>,
        'b': <xsl:value-of select="min(($leafHeight,  $bottom + 10))"/>,
      },
   </xsl:template>
   
   <!-- This function sums up the spacing for a given number of words. Used to determine
      the left and right coordinates of a hit. -->
   <xsl:function name="xtf:sumSpacing">
      <xsl:param name="lineSpacing"/>
      <xsl:param name="textBefore"/>
      <xsl:param name="tail"/>
      
      <!-- For each word (that is, a series of non-space characters) make a mark. -->
      <xsl:variable name="trimmedTextBefore" select="if ($tail) then $textBefore else replace($textBefore, '\S+$', '')"/>
      <xsl:variable name="wordsBefore">
         <xsl:analyze-string select="$trimmedTextBefore" regex="\s+">
            <xsl:non-matching-substring>1</xsl:non-matching-substring>
         </xsl:analyze-string>
      </xsl:variable>
      
      <!-- Count the marks we just made -->
      <xsl:variable name="nWordsBefore" select="string-length($wordsBefore)"/>
      
      <!-- Grab the spacing for each of those words, plus the space between them -->
      <xsl:variable name="spacingBefore" select="$lineSpacing/*[position() &lt;= (($nWordsBefore * 2) + $tail)]"/>
      
      <!-- And return the sum -->
      <xsl:value-of select="sum($spacingBefore/@width)"/>
   </xsl:function>
   
   <xsl:template match="xtf:snippet" mode="hit-context">
      <xsl:apply-templates mode="hit-context"/>
   </xsl:template>
   
   <xsl:template match="xtf:term" mode="hit-context">
      <b>
         <xsl:apply-templates mode="hit-context"/>
      </b>
   </xsl:template>
   
   <xsl:template match="text()" mode="hit-context">
      <xsl:value-of select="local:unquote(.)"/>
   </xsl:template>
   
   <xsl:function name="local:unquote">
      <xsl:param name="str"/>
      <xsl:variable name="quote" select="'&quot;'"/>
      <xsl:value-of select="replace(replace($str, $quote, ''), '\\', '')"/>
   </xsl:function>
   
   <!-- ====================================================================== -->
   <!-- Print Template                                                         -->
   <!-- ====================================================================== -->
   
   <xsl:template name="print">
      <html xml:lang="en" lang="en">
         <head>
            <title>
               <xsl:value-of select="$doc.title"/>
            </title>
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
