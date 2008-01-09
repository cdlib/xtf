<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
   xmlns:session="java:org.cdlib.xtf.xslt.Session" 
   extension-element-prefixes="session"
   version="2.0">
   
   <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
   <!-- Query result formatter stylesheet                                      -->
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
   
   <!-- this stylesheet implements very simple search forms and query results. 
      Alpha and facet browsing are also included. Formatting has been kept to a 
      minimum to make the stylesheets easily adaptable. -->
   
   <!-- ====================================================================== -->
   <!-- Import Common Templates                                                -->
   <!-- ====================================================================== -->
   
   <xsl:import href="../common/resultFormatterCommon.xsl"/>
   
   <!-- ====================================================================== -->
   <!-- Output                                                                 -->
   <!-- ====================================================================== -->
   
   <xsl:output method="xhtml" indent="yes" encoding="UTF-8" media-type="text/html" 
      doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN" 
      doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd" 
      exclude-result-prefixes="#all"/>
   
   <!-- ====================================================================== -->
   <!-- Local Parameters                                                       -->
   <!-- ====================================================================== -->
   
   <xsl:param name="css.path" select="concat($xtfURL, 'css/default/')"/>
   <xsl:param name="icon.path" select="concat($xtfURL, 'icons/default/')"/>
   <!-- to support multiple group values per facet -->
   <xsl:param name="f1-subject"/>
   
   <!-- ====================================================================== -->
   <!-- Root Template                                                          -->
   <!-- ====================================================================== -->
   
   <xsl:template match="/" exclude-result-prefixes="#all">
      <xsl:choose>
         <!-- robot response -->
         <xsl:when test="matches($http.User-Agent,$robots)">
            <xsl:apply-templates select="crossQueryResult" mode="robot"/>
         </xsl:when>
         <!-- book bag -->
         <xsl:when test="$smode = 'addToBag'">
            <span>Added to bag.</span>
         </xsl:when>
         <xsl:when test="$smode = 'removeFromBag'">
            <!-- no output needed -->
         </xsl:when>
         <!-- similar item -->
         <xsl:when test="$smode = 'moreLike'">
            <xsl:apply-templates select="crossQueryResult" mode="moreLike"/>
         </xsl:when>
         <!-- modify search -->
         <xsl:when test="contains($smode, '-modify')">
            <xsl:apply-templates select="crossQueryResult" mode="form"/>
         </xsl:when>
         <!-- browse pages -->
         <xsl:when test="$browse-title or $browse-creator">
            <xsl:apply-templates select="crossQueryResult" mode="browse"/>
         </xsl:when>
         <!-- show results -->
         <xsl:when test="
            $text or 
            $keyword or 
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
            $all or
            $smode ='showBag'">
            <xsl:apply-templates select="crossQueryResult" mode="results"/>
         </xsl:when>
         <!-- show form -->
         <xsl:otherwise>
            <xsl:apply-templates select="crossQueryResult" mode="form"/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
  
   <!-- ====================================================================== -->
   <!-- Form Template                                                          -->
   <!-- ====================================================================== -->
   
   <!-- main form page -->
   <xsl:template match="crossQueryResult" mode="form" exclude-result-prefixes="#all">
      <html>
         <head>
            <title>XTF: Search Form</title>
            <xsl:copy-of select="$brand.links"/>
         </head>
         <body>
            <xsl:copy-of select="$brand.header"/>
            <div class="form">
               <h4>Keyword Search</h4>
               <xsl:call-template name="simpleForm"/>
            </div>
            <div class="form">
               <h4>Advanced Search</h4>
               <xsl:call-template name="advancedForm"/>
            </div>
            <div class="browse">
               <h4>Browse</h4>
               <table width="50%" cellpadding="0" cellspacing="5" border="0" style="border-style: solid; border-color: blue; border-width: 1px 1px 1px 1px;">
                  <tr>
                     <td align="center">
                        <xsl:text>Browse by </xsl:text>
                        <a href="{$xtfURL}{$crossqueryPath}?all=all">Facet</a>
                        <xsl:text> | </xsl:text>
                        <a href="{$xtfURL}{$crossqueryPath}?browse-title=aa;sort=title;all=all">Title</a>
                        <xsl:text> | </xsl:text>
                        <a href="{$xtfURL}{$crossqueryPath}?browse-creator=aa;sort=creator;all=all">Author</a>
                     </td>
                  </tr>
               </table>
            </div>
            <xsl:copy-of select="$brand.footer"/>
         </body>
      </html>
   </xsl:template>
   
   <!-- simple form -->
   <xsl:template name="simpleForm">
      <form method="get" action="{$xtfURL}{$crossqueryPath}">
         <table width="50%" cellpadding="0" cellspacing="5" border="0" style="border-style: solid; border-color: blue; border-width: 1px 1px 1px 1px;">
            <tr>
               <td width="5%"/>
               <td width="30%"><b>Terms</b></td>
               <td>
                  <input type="text" name="keyword" size="30" value="{$keyword}"/>
               </td>
            </tr>
            <tr>
               <td/>
               <td/>
               <td>
                  <input type="hidden" name="all" value="all"/>
                  <input type="submit" value="Search"/>
                  <input type="reset" onclick="location.href='{$xtfURL}{$crossqueryPath}'" value="Clear"/>
               </td>
            </tr>
         </table>
      </form>
   </xsl:template>
   
   <!-- advanced form -->
   <xsl:template name="advancedForm">
      <form method="get" action="{$xtfURL}{$crossqueryPath}">
         <table width="50%" cellpadding="0" cellspacing="5" border="0" style="border-style: solid; border-color: blue; border-width: 1px 1px 1px 1px;">
            <tr>
               <td width="5%"/>
               <td width="30%"><b>Full Text</b></td>
               <td>
                  <input type="text" name="text" size="30" value="{$text}"/>
               </td>
            </tr>
            <tr>
               <td/>
               <td/>
               <td>
                  <xsl:choose>
                     <xsl:when test="$text-join = 'or'">
                        <b>all</b><input type="radio" name="text-join" value=""/>
                        <xsl:text> or </xsl:text>
                        <b>any</b><input type="radio" name="text-join" value="or" checked="checked"/>
                        <xsl:text>words</xsl:text>
                     </xsl:when>
                     <xsl:otherwise>
                        <b>all</b><input type="radio" name="text-join" value="" checked="checked"/>
                        <xsl:text> or </xsl:text>
                        <b>any</b><input type="radio" name="text-join" value="or"/>
                        <xsl:text>words</xsl:text>
                     </xsl:otherwise>
                  </xsl:choose>
               </td>
            </tr>
            <tr>
               <td/>
               <td>&#160;&#160;&#160;&#160;<i><b>Exclude</b></i></td>
               <td>
                  <input type="text" name="text-exclude" size="30" value="{$text-exclude}"/>
               </td>
            </tr>
            <tr>
               <td/>
               <td>&#160;&#160;&#160;&#160;<i><b>Proximity</b></i></td>
               <td>
                  <select size="1" name="text-prox">
                     <xsl:choose>
                        <xsl:when test="$text-prox = '1'">
                           <option value=""></option>
                           <option value="1" selected="selected">1</option>
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
                           <option value="2" selected="selected">2</option>
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
                           <option value="3" selected="selected">3</option>
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
                           <option value="4" selected="selected">4</option>
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
                           <option value="5" selected="selected">5</option>
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
                           <option value="10" selected="selected">10</option>
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
                           <option value="20" selected="selected">20</option>
                        </xsl:when>
                        <xsl:otherwise>
                           <option value="" selected="selected"></option>
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
               <td/>
               <td>&#160;&#160;&#160;&#160;<i><b>Search Area(s)</b></i></td>
               <td>
                  <xsl:choose>
                     <xsl:when test="$sectionType = 'head'">
                        <xsl:text>All</xsl:text><input type="radio" name="sectionType" value=""/> 
                        <xsl:text>Titles</xsl:text><input type="radio" name="sectionType" value="head" checked="checked"/> 
                     </xsl:when>
                     <xsl:otherwise>
                        <xsl:text>All</xsl:text><input type="radio" name="sectionType" value="" checked="checked"/> 
                        <xsl:text>Titles</xsl:text><input type="radio" name="sectionType" value="head"/> 
                     </xsl:otherwise>
                  </xsl:choose>
               </td>
            </tr>
            <tr>
               <td colspan="3">&#160;</td>
            </tr>
            <tr>
               <td/>
               <td ><b>Title</b></td>
               <td>
                  <input type="text" name="title" size="30" value="{$title}"/>
               </td>
            </tr>
            <tr>
               <td/>
               <td ><b>Author</b></td>
               <td>
                  <input type="text" name="creator" size="30" value="{$creator}"/>
               </td>
            </tr>
            <tr>
               <td/>
               <td ><b>Subject</b></td>
               <td>
                  <input type="text" name="subject" size="30" value="{$subject}"/>
               </td>
            </tr>
            <tr>
               <td/>
               <td ><b>Year(s)</b></td>
               <td>
                  <xsl:text>From </xsl:text>
                  <input type="text" name="year" size="5" value="{$year}"/>
                  <xsl:text> to </xsl:text>
                  <input type="text" name="year-max" size="5" value="{$year-max}"/>
               </td>
            </tr>
            <tr>
               <td/>
               <td ><b>Type</b></td>
               <td>
                  <select size="1" name="type">
                     <option value="">All</option>
                     <option value="ead">EAD</option>
                     <option value="html">HTML</option>
                     <option value="nlm">NLM</option>
                     <option value="pdf">PDF</option>
                     <option value="tei">TEI</option>
                     <option value="text">Text</option>
                  </select>
               </td>
            </tr>
            <tr>
               <td colspan="3">&#160;</td>
            </tr>
            <tr>
               <td/>
               <td/>
               <td>
                  <input type="hidden" name="all" value="all"/>
                  <input type="submit" value="Search"/>
                  <input type="reset" onclick="location.href='{$xtfURL}{$crossqueryPath}'" value="Clear"/>
               </td>
            </tr>
         </table>
      </form>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Results Template                                                       -->
   <!-- ====================================================================== -->
   
   <xsl:template match="crossQueryResult" mode="results" exclude-result-prefixes="#all">
      
      <!-- modify query URL -->
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
               <xsl:value-of select="concat($queryString, ';smode=simple-modify')"/>
            </xsl:otherwise>
         </xsl:choose>
      </xsl:variable>
      
      <html>
         <head>
            <title>XTF: Search Results</title>
            <xsl:copy-of select="$brand.links"/>
            <script src="script/prototype.js" type="application/javascript"/> <!-- AJAX support -->
         </head>
         <body>
            
            <!-- header -->
            <xsl:copy-of select="$brand.header"/>
            
            <!-- result header -->
            <div class="resultsHeader">
               <table width="100%" cellpadding="0" cellspacing="0">
                  <tr>
                     <td align="right" width="10%" valign="top">
                        <b>Search:&#160;</b>
                     </td>
                     <td align="left">
                        <xsl:choose>
                           <xsl:when test="$smode = 'showBag'">
                              Bag contents
                           </xsl:when>
                           <xsl:when test="$keyword or $text or $title or $creator or $subject or $year or $type">
                              <xsl:call-template name="format-query"/>
                           </xsl:when>
                           <xsl:otherwise>
                              All Items
                           </xsl:otherwise>
                        </xsl:choose>
                     </td>
                     <td align="right" valign="top">
                        <xsl:if test="$smode != 'showBag'">
                           <a href="{$xtfURL}{$crossqueryPath}?{$modifyString}">
                              <xsl:text>Modify Search</xsl:text>
                           </a>
                           <xsl:text>&#160;|&#160;</xsl:text>
                        </xsl:if>
                        <a href="{$xtfURL}{$crossqueryPath}">
                           <xsl:text>New Search</xsl:text>
                        </a>
                        <xsl:if test="$smode = 'showBag'">
                           <xsl:text>&#160;|&#160;</xsl:text>
                           <a href="{session:getData('queryURL')}">
                              <xsl:text>Return to Search Results</xsl:text>
                           </a>
                        </xsl:if>
                     </td>
                  </tr>
                  <xsl:if test="//spelling">
                     <tr>
                        <td align="right" width="10%"></td>
                        <td align="left">
                           <xsl:call-template name="did-you-mean">
                              <xsl:with-param name="baseURL" select="concat($xtfURL, $crossqueryPath, '?', $queryString)"/>
                              <xsl:with-param name="spelling" select="//spelling"/>
                           </xsl:call-template>
                        </td>
                        <td/>
                     </tr>
                  </xsl:if>
                  <tr>
                     <td align="right" width="10%">
                        <b>Results:&#160;</b>
                     </td>
                     <td align="left">
                        <span id="itemCount"><xsl:value-of select="@totalDocs"/></span>
                        <xsl:text> Item(s)</xsl:text>
                     </td>
                     <td align="right">
                        <xsl:text>Browse by </xsl:text>
                        <a href="{$xtfURL}{$crossqueryPath}?browse-title=aa;sort=title;all=all">Title</a>
                        <xsl:text> | </xsl:text>
                        <a href="{$xtfURL}{$crossqueryPath}?browse-creator=aa;sort=creator;all=all">Author</a>
                     </td>
                  </tr>
                  <tr>
                     <td align="right" width="10%">
                        <b>Sorted by:&#160;</b>
                     </td>
                     <td align="left" valign="bottom">
                        <form method="get" action="{$xtfURL}{$crossqueryPath}">
                           <xsl:call-template name="sort.options"/>
                           <xsl:call-template name="hidden.query">
                              <xsl:with-param name="queryString" select="replace($queryString, 'sort=[^;]+;', '')"/>
                           </xsl:call-template>
                           <input type="submit" value="Go!"/>
                        </form>
                     </td>
                     <td align="right">
                        <xsl:call-template name="pages"/>
                     </td>
                  </tr>
                  
               </table>
            </div>
            
            <!-- results -->
            <xsl:choose>
               <xsl:when test="docHit">
                  <div class="results">
                     <table width="100%" cellpadding="5" cellspacing="0" border="1">
                        <tr>
                           <xsl:if test="not($smode='showBag')">
                              <td width="25%" valign="top">
                                 <xsl:apply-templates select="facet[@field='facet-subject']"/>
                                 <xsl:apply-templates select="facet[@field='facet-date']"/>
                              </td>
                           </xsl:if>
                           <td valign="top">
                              <xsl:apply-templates select="docHit"/>
                           </td>
                        </tr>
                        <xsl:if test="@totalDocs > $docsPerPage">
                           <tr>
                              <td align="center" colspan="2">
                                 <xsl:call-template name="pages"/>
                              </td>
                           </tr>
                        </xsl:if>
                     </table>
                  </div>
               </xsl:when>
               <xsl:otherwise>
                  <div class="results">
                     <table width="100%" cellpadding="5" cellspacing="0" border="1">
                        <tr>
                           <td align="center">
                              <p>Sorry, no results...</p>
                              <xsl:choose>
                                 <xsl:when test="$keyword">
                                    <xsl:call-template name="simpleForm"/>
                                 </xsl:when>
                                 <xsl:otherwise>
                                    <xsl:call-template name="advancedForm"/>
                                 </xsl:otherwise>
                              </xsl:choose>
                           </td>
                        </tr>
                     </table>
                  </div>
               </xsl:otherwise>
            </xsl:choose>
            
            <!-- footer -->
            <xsl:copy-of select="$brand.footer"/>
            
         </body>
      </html>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Browse Template                                                        -->
   <!-- ====================================================================== -->
   
   <xsl:template match="crossQueryResult" mode="browse">
      
      <html>
         <head>
            <title>XTF: Search Results</title>
            <xsl:copy-of select="$brand.links"/>
            <script src="script/prototype.js" type="application/javascript"/> <!-- AJAX support -->
         </head>
         <body>
            
            <!-- header -->
            <xsl:copy-of select="$brand.header"/>
            
            <!-- result header -->
            <div class="resultsHeader">
               <table width="100%" cellpadding="0" cellspacing="0">
                  <tr>
                     <td align="right" width="10%" valign="top">
                        <b>Browse by:&#160;</b>
                     </td>
                     <td align="left">
                        <xsl:choose>
                           <xsl:when test="$browse-title">
                              Title
                           </xsl:when>
                           <xsl:when test="$browse-creator">
                              Author
                           </xsl:when>
                           <xsl:otherwise>
                              All Items
                           </xsl:otherwise>
                        </xsl:choose>
                     </td>
                     <td align="right" valign="top">
                        <a href="{$xtfURL}{$crossqueryPath}">
                           <xsl:text>New Search</xsl:text>
                        </a>
                        <xsl:if test="$smode = 'showBag'">
                           <xsl:text>&#160;|&#160;</xsl:text>
                           <a href="{session:getData('queryURL')}">
                              <xsl:text>Return to Search Results</xsl:text>
                           </a>
                        </xsl:if>
                     </td>
                  </tr>
                  <tr>
                     <td align="right" width="10%">
                        <b>Results:&#160;</b>
                     </td>
                     <td align="left">
                        <xsl:value-of select="facet/group[docHit]/@totalDocs"/>
                        <xsl:text> Item(s)</xsl:text>
                     </td>
                     <td align="right">
                        <xsl:text>Browse by </xsl:text>
                        <xsl:choose>
                           <xsl:when test="$browse-title">
                              <xsl:text>Title | </xsl:text>
                              <a href="{$xtfURL}{$crossqueryPath}?browse-creator=aa;sort=creator;all=all">Author</a>
                           </xsl:when>
                           <xsl:otherwise>
                              <a href="{$xtfURL}{$crossqueryPath}?browse-title=aa;sort=title;all=all">Title</a>
                              <xsl:text>  | Author</xsl:text>
                           </xsl:otherwise>
                        </xsl:choose>
                     </td>
                  </tr>
                  
                  <xsl:variable name="alphaList" select="'A B C D E F G H I J K L M N O P Q R S T U V W Y Z OTHER'"/>
                  <tr>
                     <td colspan="3" align="center">
                        <xsl:call-template name="alphaList">
                           <xsl:with-param name="alphaList" select="$alphaList"/>
                        </xsl:call-template>
                     </td>
                  </tr>
                  
               </table>
            </div>
            
            <!-- results -->
            <div class="results">
               <table width="100%" cellpadding="5" cellspacing="0" border="1">
                  <tr>
                     <td valign="top">
                        <xsl:choose>
                           <xsl:when test="$browse-title">
                              <xsl:apply-templates select="facet[@field='browse-title']/group/docHit"/>
                           </xsl:when>
                           <xsl:otherwise>
                              <xsl:apply-templates select="facet[@field='browse-creator']/group/docHit"/>
                           </xsl:otherwise>
                        </xsl:choose>
                     </td>
                  </tr>
               </table>
            </div>
            
            <!-- footer -->
            <xsl:copy-of select="$brand.footer"/>
            
         </body>
      </html>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Document Hit Template                                                       -->
   <!-- ====================================================================== -->
   
   <xsl:template match="docHit" exclude-result-prefixes="#all">
      
      <xsl:variable name="path" select="@path"/>
      
      <xsl:variable name="identifier" select="meta/identifier[1]"/>
      <xsl:variable name="quotedID" select="concat('&quot;', $identifier, '&quot;')"/>
      <xsl:variable name="indexId" select="replace($identifier, '.*/', '')"/>
      
      <!-- scrolling anchor -->
      <xsl:variable name="anchor">
         <xsl:choose>
            <xsl:when test="$sort = 'creator'">
               <xsl:value-of select="substring(string(meta/creator[1]), 1, 1)"/>
            </xsl:when>
            <xsl:when test="$sort = 'title'">
               <xsl:value-of select="substring(string(meta/title[1]), 1, 1)"/>
            </xsl:when>
         </xsl:choose>
      </xsl:variable>
      
      <div id="main_{@rank}" class="docHit">    
         <table width="100%" cellpadding="0" cellspacing="0">          
            <tr>
               <td align="right" width="4%">
                  <xsl:choose>
                     <xsl:when test="$sort = ''">
                        <b><xsl:value-of select="@rank"/></b>
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
                  <b>Author:&#160;&#160;</b>
               </td>
               <td align="left">
                  <xsl:choose>
                     <xsl:when test="meta/creator">
                        <xsl:apply-templates select="meta/creator[1]"/>
                     </xsl:when>
                     <xsl:otherwise>none</xsl:otherwise>
                  </xsl:choose>
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
                  <b>Title:&#160;&#160;</b>
               </td>
               <td align="left">
                  <a>
                     <xsl:attribute name="href">
                        <xsl:choose>
                           <xsl:when test="matches(meta/display, 'dynaxml')">
                              <xsl:call-template name="dynaxml.url">
                                 <xsl:with-param name="path" select="$path"/>
                              </xsl:call-template>
                           </xsl:when>
                           <xsl:otherwise>
                              <xsl:call-template name="rawDisplay.url">
                                 <xsl:with-param name="path" select="$path"/>
                              </xsl:call-template>
                           </xsl:otherwise>
                        </xsl:choose>
                     </xsl:attribute>
                     <xsl:choose>
                        <xsl:when test="meta/title">
                           <xsl:apply-templates select="meta/title[1]"/>
                        </xsl:when>
                        <xsl:otherwise>none</xsl:otherwise>
                     </xsl:choose>
                  </a>
               </td>
               <td>
                  <xsl:variable name="type" select="meta/type"/>
                  <i><b><xsl:value-of select="$type"/></b></i>
               </td>
            </tr>
            <tr>
               <td align="right">
                  <xsl:text>&#160;</xsl:text>
               </td>
               <td align="right">
                  <b>Published:&#160;&#160;</b>
               </td>
               <td align="left">
                  <xsl:choose>
                     <xsl:when test="meta/year">
                        <xsl:value-of select="replace(meta/year,'^.+ ','')"/>
                     </xsl:when>
                     <xsl:otherwise>
                        <xsl:apply-templates select="meta/date"/>
                     </xsl:otherwise>
                  </xsl:choose>
               </td>
               <td align="right">
                  <xsl:text>&#160;</xsl:text>
               </td>
            </tr>
            <xsl:if test="meta/subject">
               <tr>
                  <td align="right">
                     <xsl:text>&#160;</xsl:text>
                  </td>
                  <td align="right" valign="top">
                     <b>Subjects:&#160;&#160;</b>
                  </td>
                  <td align="left">
                     <xsl:apply-templates select="meta/subject"/>
                  </td>
                  <td align="right">
                     <xsl:text>&#160;</xsl:text>
                  </td>
               </tr>
            </xsl:if>
            <xsl:if test="snippet">
               <tr>
                  <td align="right">
                     <xsl:text>&#160;</xsl:text>
                  </td>
                  <td align="right" valign="top">
                     <b>Matches:&#160;&#160;</b>
                     <br/>
                     <i>(<xsl:value-of select="@totalHits"/> hits)&#160;&#160;&#160;&#160;</i>
                  </td>
                  <td align="left">
                     <xsl:apply-templates select="snippet" mode="text"/>
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
                     <b>Bag:&#160;&#160;</b>
                  </td>
                  <td align="left">
                     
                     <xsl:choose>
                        <xsl:when test="$smode = 'showBag'">
                           <script type="text/javascript">
                              remove_<xsl:value-of select="@rank"/> = function() {
                                 var span = $('remove_<xsl:value-of select="@rank"/>');
                                 span.innerHTML = "Removing...";
                                 new Ajax.Request('<xsl:value-of select="concat($xtfURL, $crossqueryPath, '?smode=removeFromBag;identifier=', $identifier)"/>', {
                                    onSuccess: function() { 
                                       var main = $('main_<xsl:value-of select="@rank"/>');
                                       main.parentNode.removeChild(main);
                                       --($('itemCount').innerHTML);
                                    },
                                    onFailure: function() { span.innerHTML = 'Failed to remove!'; }
                                 });
                              };
                           </script>
                           <span id="remove_{@rank}">
                              <a href="javascript:remove_{@rank}()">Remove from bag</a>
                           </span>
                        </xsl:when>
                        <xsl:when test="session:noCookie()">
                           <span><a href="javascript:alert('To use the bag, you must enable cookies in your web browser.')">Requires cookie*</a></span>                                 
                        </xsl:when>
                        <xsl:otherwise>
                           <a href="{concat('search?smode=showBag;', $queryString)}">Show bag</a> |
                           <xsl:choose>
                              <xsl:when test="session:getData('bag')/bag/savedDoc[@id=$indexId]">
                                 <span>In bag.</span>
                              </xsl:when>
                              <xsl:otherwise>
                                 <script type="text/javascript">
                                    add_<xsl:value-of select="@rank"/> = function() {
                                       var span = $('add_<xsl:value-of select="@rank"/>');
                                       span.innerHTML = "Adding...";
                                       new Ajax.Request('<xsl:value-of select="concat($xtfURL, $crossqueryPath, '?smode=addToBag;identifier=', $identifier)"/>', {
                                          onSuccess: function(transport) { span.innerHTML = transport.responseText; },
                                          onFailure: function(transport) { span.innerHTML = "Failed to add!"; }
                                       });
                                    };
                                 </script>
                                 <span id="add_{@rank}">
                                    <a href="javascript:add_{@rank}()">Add to bag</a>
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
            
            <!-- "more like this" -->
            <tr>
               <td align="right">
                  <xsl:text>&#160;</xsl:text>
               </td>
               <td align="right" valign="top">
                  <b>Similar:&#160;&#160;</b>
               </td>
               <td align="left">
                  <script type="text/javascript">
                     getMoreLike_<xsl:value-of select="@rank"/> = function() {
                        var span = $('moreLike_<xsl:value-of select="@rank"/>');
                        span.innerHTML = "Fetching...";
                        new Ajax.Request('<xsl:value-of select="concat('search?smode=moreLike;docsPerPage=5;identifier=', $identifier)"/>',
                           { onSuccess: function(transport) { span.innerHTML = transport.responseText; },
                             onFailure: function(transport) { span.innerHTML = "Failed!" } });
                     };
                  </script>
                  <span id="moreLike_{@rank}">
                     <a href="javascript:getMoreLike_{@rank}()">Fetch</a>
                  </span>
               </td>
               <td align="right">
                  <xsl:text>&#160;</xsl:text>
               </td>
            </tr>
            
            <xsl:if test="not(position()=last())">
               <tr>
                  <td colspan="4">
                     <hr size="1" width="80%"/>
                  </td>
               </tr>
            </xsl:if>
            
         </table>
      </div>
      
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Facet and Groups                                                       -->
   <!-- ====================================================================== -->
   
   <!-- hierarchical date facet -->
   <xsl:template match="facet[@field='facet-date']" exclude-result-prefixes="#all">
      <div class="facet">
         <h4>Date</h4>
         <ul>
            <xsl:apply-templates/>
         </ul>
      </div>
   </xsl:template>
   
   <!-- subject facet -->
   <xsl:template match="facet[@field='facet-subject']" exclude-result-prefixes="#all">
      <xsl:variable name="expandString" select="replace($queryString,'[;&amp;]expand=[^;&amp;]+','')"/>
      <div class="facet">
         <h4>
            <xsl:choose>
               <xsl:when test="not($expand='subject')">
                  <a href="{$xtfURL}{$crossqueryPath}?{$expandString};expand=subject">
                     <img src="{$icon.path}/i_expand.gif" border="0" alt="expand"/>
                  </a>
               </xsl:when>
               <xsl:otherwise>
                  <a href="{$xtfURL}{$crossqueryPath}?{$expandString}">
                     <img src="{$icon.path}/i_colpse.gif" border="0" alt="collapse"/>
                  </a>
               </xsl:otherwise>
            </xsl:choose>
            <xsl:text> Subject</xsl:text>
         </h4>
         <ul>
            <xsl:apply-templates/>
         </ul>
         <xsl:if test="not($expand='subject') and (@totalDocs > 10)">
            <a href="{$xtfURL}{$crossqueryPath}?{$expandString};expand=subject">more</a>
         </xsl:if>
      </div>
   </xsl:template>
   
   <!-- hierarchical date group -->
   <xsl:template match="group[ancestor::facet[@field='facet-date']]" exclude-result-prefixes="#all">
      <xsl:variable name="value" select="@value"/>
      <xsl:variable name="facetString" select="replace($queryString,'[;&amp;]f1-date=[^;&amp;]+','')"/>
      <xsl:variable name="hierarchy">
         <xsl:for-each select="ancestor::group/@value">
            <xsl:value-of select="concat(.,'::')"/>
         </xsl:for-each>
      </xsl:variable>
      <xsl:variable name="groupURL">
         <xsl:value-of select="concat($xtfURL,$crossqueryPath,'?',$facetString,';f1-date=',$hierarchy,$value)"/>
      </xsl:variable> 
      <xsl:variable name="upURL">
         <xsl:value-of select="concat($xtfURL,$crossqueryPath,'?',$facetString,';f1-date=',replace($hierarchy,'::$',''))"/>
      </xsl:variable> 
      
      <li>
         <xsl:choose>
            <!-- terminal node with siblings -->
            <xsl:when test="@totalSubGroups = 0 and (preceding-sibling::group or following-sibling::group)">
               <a href="{$groupURL}">
                  <xsl:value-of select="@value"/>
               </a>
               &#160;(<xsl:value-of select="@totalDocs"/>)
            </xsl:when>
            <!-- single terminal node -->
            <xsl:when test="(@totalSubGroups = 0)">
               <a href="{replace($upURL,'::[^:]+$','')}">
                  <img src="{$icon.path}/i_colpse.gif" border="0" alt="collapse"/>
               </a>
               <i><xsl:value-of select="@value"/></i>
            </xsl:when>
            <!-- open node -->
            <xsl:when test="group">
               <a href="{$upURL}">
                  <img src="{$icon.path}/i_colpse.gif" border="0" alt="collapse"/>
               </a>
               <xsl:value-of select="@value"/>
               &#160;(<xsl:value-of select="@totalDocs"/>)
               <xsl:if test="group">
                  <ul>
                     <xsl:apply-templates/>
                  </ul>
               </xsl:if>
            </xsl:when>
            <!-- closed node -->
            <xsl:otherwise>
               <a href="{$groupURL}">
                  <img src="{$icon.path}/i_expand.gif" border="0" alt="expand"/>
               </a>
               <a href="{$groupURL}">
                  <xsl:value-of select="@value"/>
               </a>
               &#160;(<xsl:value-of select="@totalDocs"/>)
            </xsl:otherwise>
         </xsl:choose>
      </li>
      
   </xsl:template>
   
   <!-- subject group -->
   <xsl:template match="group[parent::facet[@field='facet-subject']]" exclude-result-prefixes="#all">
      
      <xsl:variable name="nextNum">
         <xsl:choose>
            <xsl:when test="//param[matches(@name,'f[0-9]+-subject')]">
               <xsl:for-each select="//param[matches(@name,'f[0-9]+-subject')]">
                  <xsl:variable name="num" select="number(replace(@name,'f([0-9]+)-subject','$1')) + 1"/>
                  <xsl:if test="not(//param[@name=concat('f',$num,'-subject')])">
                     <xsl:value-of select="$num"/>
                  </xsl:if>
               </xsl:for-each>
            </xsl:when>
            <xsl:otherwise>
               <xsl:value-of select="'1'"/>
            </xsl:otherwise>
         </xsl:choose>
      </xsl:variable>
      <xsl:variable name="name" select="concat('f',$nextNum,'-subject')"/>
      <xsl:variable name="value" select="@value"/>
      
      <li>
         <xsl:choose>
            <xsl:when test="//param[matches(@name,'f[0-9]+-subject')]/@value=$value">
               <xsl:variable name="removeString">
                  <xsl:analyze-string select="$queryString" regex="f[0-9]+-subject=([^;&amp;]+)">
                     <xsl:matching-substring>
                        <xsl:variable name="check" select="regex-group(1)"/>
                        <xsl:choose>
                           <xsl:when test="$value=$check"/>
                           <xsl:otherwise>
                              <xsl:value-of select="."/>
                           </xsl:otherwise>
                        </xsl:choose>  
                     </xsl:matching-substring>
                     <xsl:non-matching-substring>
                        <xsl:value-of select="."/>
                     </xsl:non-matching-substring>
                  </xsl:analyze-string>
               </xsl:variable>
               <i><xsl:value-of select="$value"/></i> <a href="{$xtfURL}{$crossqueryPath}?{replace($removeString,'^[;&amp;]+','')}">[X]</a>
            </xsl:when>
            <xsl:otherwise>
               <a href="{$xtfURL}{$crossqueryPath}?{$queryString};{$name}={$value}">
                  <xsl:value-of select="$value"/>
               </a>
               &#160;(<xsl:value-of select="@totalDocs"/>)
            </xsl:otherwise>
         </xsl:choose>
      </li>
      
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Snippet Template (for snippets in the full text)                       -->
   <!-- ====================================================================== -->
   
   <xsl:template match="snippet" mode="text" exclude-result-prefixes="#all">
      <xsl:text>...</xsl:text>
      <xsl:apply-templates mode="text"/>
      <xsl:text>...</xsl:text>
      <br/>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Term Template (for snippets in the full text)                          -->
   <!-- ====================================================================== -->
   
   <xsl:template match="term" mode="text" exclude-result-prefixes="#all">
      <xsl:variable name="path" select="ancestor::docHit/@path"/>
      <xsl:variable name="display" select="ancestor::docHit/meta/display"/>
      <xsl:variable name="hit.rank"><xsl:value-of select="ancestor::snippet/@rank"/></xsl:variable>
      <xsl:variable name="snippet.link">    
         <xsl:call-template name="dynaxml.url">
            <xsl:with-param name="path" select="$path"/>
         </xsl:call-template>
         <xsl:value-of select="concat(';hit.rank=', $hit.rank)"/>
      </xsl:variable>
      
      <xsl:choose>
         <xsl:when test="ancestor::query"/>
         <xsl:when test="not(ancestor::snippet) or not(matches($display, 'dynaxml'))">
            <span class="hit"><xsl:apply-templates/></span>
         </xsl:when>
         <xsl:otherwise>
            <a href="{$snippet.link}" class="hit"><xsl:apply-templates/></a>
         </xsl:otherwise>
      </xsl:choose> 
      
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Term Template (for snippets in meta-data fields)                       -->
   <!-- ====================================================================== -->
   
   <xsl:template match="term" exclude-result-prefixes="#all">
      <xsl:choose>
         <xsl:when test="ancestor::query"/>
         <xsl:otherwise>
            <span class="hit"><xsl:apply-templates/></span>
         </xsl:otherwise>
      </xsl:choose> 
      
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- More Like This Template                                                -->
   <!-- ====================================================================== -->
   
   <!-- results -->
   <xsl:template match="crossQueryResult" mode="moreLike" exclude-result-prefixes="#all">
      <xsl:choose>
         <xsl:when test="docHit">
            <div class="moreLike">
               <xsl:apply-templates select="docHit" mode="moreLike"/>
            </div>
         </xsl:when>
         <xsl:otherwise>
            <div class="moreLike">
               <b>No similar documents found.</b>
            </div>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- docHit -->
   <xsl:template match="docHit" mode="moreLike" exclude-result-prefixes="#all">
      
      <xsl:variable name="path" select="@path"/>
      
      <a>
         <xsl:attribute name="href">
            <xsl:choose>
               <xsl:when test="matches(meta/display, 'dynaxml')">
                  <xsl:call-template name="dynaxml.url">
                     <xsl:with-param name="path" select="$path"/>
                  </xsl:call-template>
               </xsl:when>
               <xsl:otherwise>
                  <xsl:call-template name="rawDisplay.url">
                     <xsl:with-param name="path" select="$path"/>
                  </xsl:call-template>
               </xsl:otherwise>
            </xsl:choose>
         </xsl:attribute>
         <xsl:apply-templates select="meta/title[1]"/>
      </a>
      (<xsl:apply-templates select="meta/creator[1]"/>)<br/>
   </xsl:template>
   
</xsl:stylesheet>
