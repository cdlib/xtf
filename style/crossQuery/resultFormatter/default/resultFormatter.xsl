<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
   xmlns:session="java:org.cdlib.xtf.xslt.Session"
   xmlns:editURL="http://cdlib.org/xtf/editURL"
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
   
   <xsl:output method="xhtml" indent="no" encoding="UTF-8" media-type="text/html" 
      doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN" 
      doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd" 
      exclude-result-prefixes="#all"/>
   
   <!-- ====================================================================== -->
   <!-- Local Parameters                                                       -->
   <!-- ====================================================================== -->
   
   <xsl:param name="css.path" select="concat($xtfURL, 'css/default/')"/>
   <xsl:param name="icon.path" select="concat($xtfURL, 'icons/default/')"/>
   
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
            <span>Added</span>
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
         <xsl:when test="crossQueryResult/query/*/*">
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
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
            <xsl:copy-of select="$brand.links"/>
         </head>
         <body>
            <xsl:copy-of select="$brand.header"/>
            <div class="searchPage">
               <div class="forms">
                  <table>
                     <tr>
                        <td class="{if(matches($smode,'simple')) then 'tab-select' else 'tab'}"><a href="search?smode=simple">Keyword</a></td>
                        <td class="{if(matches($smode,'advanced')) then 'tab-select' else 'tab'}"><a href="search?smode=advanced">Advanced</a></td>
                        <td class="{if(matches($smode,'browse')) then 'tab-select' else 'tab'}"><a href="search?smode=browse">Browse</a></td>
                     </tr>
                     <tr>
                        <td colspan="3">
                           <div class="form">
                              <xsl:choose>
                                 <xsl:when test="matches($smode,'simple')">
                                    <xsl:call-template name="simpleForm"/>
                                 </xsl:when>
                                 <xsl:when test="matches($smode,'advanced')">
                                    <xsl:call-template name="advancedForm"/>
                                 </xsl:when>
                                 <xsl:when test="matches($smode,'browse')">
                                    <table>
                                       <tr>
                                          <td>
                                             <xsl:text>Browse by </xsl:text>
                                             <xsl:call-template name="browseLinks"/>
                                          </td>
                                       </tr>
                                    </table>
                                 </xsl:when>
                              </xsl:choose>
                           </div>
                        </td>
                     </tr>
                  </table>
               </div>
            </div>
            <xsl:copy-of select="$brand.footer"/>
         </body>
      </html>
   </xsl:template>
   
   <!-- simple form -->
   <xsl:template name="simpleForm" exclude-result-prefixes="#all">
      <form method="get" action="{$xtfURL}{$crossqueryPath}">
         <table>
            <tr>
               <td>
                  <input type="text" name="keyword" size="40" value="{$keyword}"/>
                  <xsl:text>&#160;</xsl:text>
                  <input type="submit" value="Search"/>
                  <input type="reset" onclick="location.href='{$xtfURL}{$crossqueryPath}'" value="Clear"/>
               </td>
            </tr>
            <tr>
               <td>Examples go here.</td>
            </tr>
         </table>
      </form>
   </xsl:template>
   
   <!-- advanced form -->
   <xsl:template name="advancedForm" exclude-result-prefixes="#all">
      <form method="get" action="{$xtfURL}{$crossqueryPath}">
         <table>
            <tr>
               <td colspan="3">
                  <h4>Entire Text</h4>
               </td>
            </tr>
            <tr>
               <td class="indent">&#160;</td>
               <td colspan="2">
                  <input type="text" name="text" size="45" value="{$text}"/>
               </td>
            </tr>
            <tr>
               <td class="indent">&#160;</td>
               <td colspan="2">
                  <xsl:choose>
                     <xsl:when test="$text-join = 'or'">
                        <input type="radio" name="text-join" value=""/>
                        <xsl:text> all of </xsl:text>
                        <input type="radio" name="text-join" value="or" checked="checked"/>
                        <xsl:text> any of </xsl:text>
                     </xsl:when>
                     <xsl:otherwise>
                        <input type="radio" name="text-join" value="" checked="checked"/>
                        <xsl:text> all of </xsl:text>
                        <input type="radio" name="text-join" value="or"/>
                        <xsl:text> any of </xsl:text>
                     </xsl:otherwise>
                  </xsl:choose>
                  <xsl:text>these words</xsl:text>
               </td>
            </tr>
            <tr>
               <td class="indent">&#160;</td>
               <td><b>Exclude</b></td>
               <td>
                  <input type="text" name="text-exclude" size="30" value="{$text-exclude}"/>
               </td>
            </tr>
            <tr>
               <td class="indent">&#160;</td>
               <td><b>Proximity</b></td>
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
               <td class="indent">&#160;</td>
               <td><b>Section</b></td>
               <td>
                  <xsl:choose>
                     <xsl:when test="$sectionType = 'head'">
                        <input type="radio" name="sectionType" value=""/><xsl:text> any </xsl:text>
                        <input type="radio" name="sectionType" value="head" checked="checked"/><xsl:text> headings </xsl:text>
                        <input type="radio" name="sectionType" value="citation"/><xsl:text> citations </xsl:text>
                     </xsl:when>
                     <xsl:when test="$sectionType = 'note'"> 
                        <input type="radio" name="sectionType" value=""/><xsl:text> any </xsl:text>
                        <input type="radio" name="sectionType" value="head"/><xsl:text> headings </xsl:text>
                        <input type="radio" name="sectionType" value="citation" checked="checked"/><xsl:text> citations </xsl:text>
                     </xsl:when>
                     <xsl:otherwise>
                        <input type="radio" name="sectionType" value="" checked="checked"/><xsl:text> any </xsl:text>
                        <input type="radio" name="sectionType" value="head"/><xsl:text> headings </xsl:text>
                        <input type="radio" name="sectionType" value="citation"/><xsl:text> citations </xsl:text>
                     </xsl:otherwise>
                  </xsl:choose>
               </td>
            </tr>
            <tr>
               <td colspan="3"><h4>Metadata</h4></td>
            </tr>
            <tr>
               <td class="indent">&#160;</td>
               <td><b>Title</b></td>
               <td>
                  <input type="text" name="title" size="30" value="{$title}"/>
               </td>
            </tr>
            <tr>
               <td class="indent">&#160;</td>
               <td><b>Author</b></td>
               <td>
                  <input type="text" name="creator" size="30" value="{$creator}"/>
               </td>
            </tr>
            <tr>
               <td class="indent">&#160;</td>
               <td><b>Subject</b></td>
               <td>
                  <input type="text" name="subject" size="30" value="{$subject}"/>
               </td>
            </tr>
            <tr>
               <td class="indent">&#160;</td>
               <td><b>Year(s)</b></td>
               <td>
                  <xsl:text>From </xsl:text>
                  <input type="text" name="year" size="5" value="{$year}"/>
                  <xsl:text> to </xsl:text>
                  <input type="text" name="year-max" size="5" value="{$year-max}"/>
               </td>
            </tr>
            <tr>
               <td class="indent">&#160;</td>
               <td><b>Type</b></td>
               <td>
                  <select size="1" name="type">
                     <option value="">All</option>
                     <option value="ead">EAD</option>
                     <option value="html">HTML</option>
                     <option value="msword">MSWord</option>
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
               <td class="indent">&#160;</td>
               <td>&#160;</td>
               <td>
                  <input type="hidden" name="smode" value="advanced"/>
                  <input type="submit" value="Search"/>
                  <input type="reset" onclick="location.href='{$xtfURL}{$crossqueryPath}?smode=advanced'" value="Clear"/>
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
      <xsl:variable name="modify" select="if(matches($smode,'simple')) then 'simple-modify' else 'advanced-modify'"/>
      <xsl:variable name="modifyString" select="editURL:set($queryString, 'smode', $modify)"/>
      
      <html>
         <head>
            <title>XTF: Search Results</title>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
            <xsl:copy-of select="$brand.links"/>
            <!-- AJAX support -->
            <script src="script/yui/yahoo-dom-event.js" type="application/javascript"/> 
            <script src="script/yui/connection-min.js" type="application/javascript"/> 
         </head>
         <body>
            
            <!-- header -->
            <xsl:copy-of select="$brand.header"/>
            
            <!-- result header -->
            <div class="resultsHeader">
               <table>
                  <tr>
                     <td colspan="2" class="right">
                        <xsl:if test="$smode != 'showBag'">
                           <xsl:variable name="bag" select="session:getData('bag')"/>
                           <a href="{$xtfURL}{$crossqueryPath}?smode=showBag">Bookbag</a>
                           (<span id="itemCount"><xsl:value-of select="count($bag/bag/savedDoc)"/></span>)
                        </xsl:if>
                     </td>
                  </tr>
                  <tr>
                     <td>
                        <xsl:if test="not($smode='showBag')">
                           <div class="query">
                              <div class="label">
                                 <b><xsl:value-of select="if($browse-all) then 'Browse by' else 'Search'"/>:</b>
                              </div>
                              <xsl:call-template name="format-query"/>
                           </div>
                        </xsl:if>
                     </td>
                     <td class="right">
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
                        <td>
                           <xsl:call-template name="did-you-mean">
                              <xsl:with-param name="baseURL" select="concat($xtfURL, $crossqueryPath, '?', $queryString)"/>
                              <xsl:with-param name="spelling" select="//spelling"/>
                           </xsl:call-template>
                        </td>
                        <td class="right">&#160;</td>
                     </tr>
                  </xsl:if>
                  <tr>
                     <td>
                        <b><xsl:value-of select="if($smode='showBag') then 'Bookbag' else 'Results'"/>:</b>&#160;
                        <span id="itemCount"><xsl:value-of select="@totalDocs"/></span>
                        <xsl:text> Item(s)</xsl:text>
                     </td>
                     <td class="right">
                        <xsl:text>Browse by </xsl:text>
                        <xsl:call-template name="browseLinks"/>
                     </td>
                  </tr>
                  <xsl:if test="docHit">
                     <tr>
                        <td>
                           <form method="get" action="{$xtfURL}{$crossqueryPath}">
                              <b>Sorted by:&#160;</b>
                              <xsl:call-template name="sort.options"/>
                              <xsl:call-template name="hidden.query">
                                 <xsl:with-param name="queryString" select="editURL:remove($queryString, 'sort')"/>
                              </xsl:call-template>
                              <xsl:text>&#160;</xsl:text>
                              <input type="submit" value="Go!"/>
                           </form>
                        </td>
                        <td class="right">
                           <xsl:call-template name="pages"/>
                        </td>
                     </tr>
                  </xsl:if>
               </table>
            </div>
            
            <!-- results -->
            <xsl:choose>
               <xsl:when test="docHit">
                  <div class="results">
                     <table>
                        <tr>
                           <xsl:if test="not($smode='showBag')">
                              <td class="facet">
                                 <xsl:apply-templates select="facet[@field='facet-subject']"/>
                                 <xsl:apply-templates select="facet[@field='facet-date']"/>
                              </td>
                           </xsl:if>
                           <td class="docHit">
                              <xsl:apply-templates select="docHit"/>
                           </td>
                        </tr>
                        <xsl:if test="@totalDocs > $docsPerPage">
                           <tr>
                              <td colspan="2" class="center">
                                 <xsl:call-template name="pages"/>
                              </td>
                           </tr>
                        </xsl:if>
                     </table>
                  </div>
               </xsl:when>
               <xsl:otherwise>
                  <div class="results">
                     <table>
                        <tr>
                           <td>
                              <p>Sorry, no results...</p>
                              <p>Some suggestions should go here...</p>
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
   
   <xsl:template match="crossQueryResult" mode="browse" exclude-result-prefixes="#all">
      
      <xsl:variable name="alphaList" select="'A B C D E F G H I J K L M N O P Q R S T U V W Y Z OTHER'"/>
      
      <html>
         <head>
            <title>XTF: Search Results</title>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
            <xsl:copy-of select="$brand.links"/>
            <!-- AJAX support -->
            <script src="script/yui/yahoo-dom-event.js" type="application/javascript"/> 
            <script src="script/yui/connection-min.js" type="application/javascript"/> 
         </head>
         <body>
            
            <!-- header -->
            <xsl:copy-of select="$brand.header"/>
            
            <!-- result header -->
            <div class="resultsHeader">
               <table>
                  <tr>
                     <tr>
                        <td colspan="2" class="right">
                           <xsl:variable name="bag" select="session:getData('bag')"/>
                           <a href="{$xtfURL}{$crossqueryPath}?smode=showBag">Bookbag</a>
                           (<span id="itemCount"><xsl:value-of select="count($bag/bag/savedDoc)"/></span>)
                        </td>
                     </tr>
                     <td>
                        <b>Browse by:&#160;</b>
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
                     <td class="right">
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
                     <td>
                        <b>Results:&#160;</b>
                        <xsl:value-of select="facet/group[docHit]/@totalDocs"/>
                        <xsl:text> Item(s)</xsl:text>
                     </td>
                     <td class="right">
                        <xsl:text>Browse by </xsl:text>
                        <xsl:call-template name="browseLinks"/>
                     </td>
                  </tr>
                  <tr>
                     <td colspan="2" class="center">
                        <xsl:call-template name="alphaList">
                           <xsl:with-param name="alphaList" select="$alphaList"/>
                        </xsl:call-template>
                     </td>
                  </tr>
                  
               </table>
            </div>
            
            <!-- results -->
            <div class="results">
               <table>
                  <tr>
                     <td>
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
   
   <xsl:template name="browseLinks">
      <xsl:choose>
         <xsl:when test="$browse-all">
            <xsl:text>Facet | </xsl:text>
            <a href="{$xtfURL}{$crossqueryPath}?browse-title=first;sort=title">Title</a>
            <xsl:text> | </xsl:text>
            <a href="{$xtfURL}{$crossqueryPath}?browse-creator=first;sort=creator">Author</a>
         </xsl:when>
         <xsl:when test="$browse-title">
            <a href="{$xtfURL}{$crossqueryPath}?browse-all=yes">Facet</a>
            <xsl:text> | Title | </xsl:text>
            <a href="{$xtfURL}{$crossqueryPath}?browse-creator=first;sort=creator">Author</a>
         </xsl:when>
         <xsl:when test="$browse-creator">
            <a href="{$xtfURL}{$crossqueryPath}?browse-all=yes">Facet</a>
            <xsl:text> | </xsl:text>
            <a href="{$xtfURL}{$crossqueryPath}?browse-title=first;sort=title">Title</a>
            <xsl:text>  | Author</xsl:text>
         </xsl:when>
         <xsl:otherwise>
            <a href="{$xtfURL}{$crossqueryPath}?browse-all=yes">Facet</a>
            <xsl:text> | </xsl:text>
            <a href="{$xtfURL}{$crossqueryPath}?browse-title=first;sort=title">Title</a>
            <xsl:text> | </xsl:text>
            <a href="{$xtfURL}{$crossqueryPath}?browse-creator=first;sort=creator">Author</a>
         </xsl:otherwise>
      </xsl:choose>
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
         <table>          
            <tr>
               <td class="col1">
                  <xsl:choose>
                     <xsl:when test="$sort = ''">
                        <b><xsl:value-of select="@rank"/></b>
                     </xsl:when>
                     <xsl:otherwise>
                        <xsl:text>&#160;</xsl:text>
                     </xsl:otherwise>
                  </xsl:choose>
               </td>
               <td class="col2">
                  <xsl:if test="$sort = 'creator'">
                     <a name="{$anchor}"/>
                  </xsl:if>
                  <b>Author:&#160;&#160;</b>
               </td>
               <td class="col3">
                  <xsl:choose>
                     <xsl:when test="meta/creator">
                        <xsl:apply-templates select="meta/creator[1]"/>
                     </xsl:when>
                     <xsl:otherwise>none</xsl:otherwise>
                  </xsl:choose>
               </td>
               <td class="col4">
                  <!-- Add/remove logic for the session bag (only if session tracking enabled) -->
                  <xsl:if test="session:isEnabled()">
                     <xsl:choose>
                        <xsl:when test="$smode = 'showBag'">
                           <script type="text/javascript">
                              remove_<xsl:value-of select="@rank"/> = function() {
                                 var span = YAHOO.util.Dom.get('remove_<xsl:value-of select="@rank"/>');
                                 span.innerHTML = "Deleting...";
                                 YAHOO.util.Connect.asyncRequest('GET', 
                                    '<xsl:value-of select="concat($xtfURL, $crossqueryPath, '?smode=removeFromBag;identifier=', $identifier)"/>',
                                    {  success: function(o) { 
                                          var main = YAHOO.util.Dom.get('main_<xsl:value-of select="@rank"/>');
                                          main.parentNode.removeChild(main);
                                          --(YAHOO.util.Dom.get('itemCount').innerHTML);
                                       },
                                       failure: function(o) { span.innerHTML = 'Failed to delete!'; }
                                    }, null);
                              };
                           </script>
                           <span id="remove_{@rank}">
                              <a href="javascript:remove_{@rank}()">Delete</a>
                           </span>
                        </xsl:when>
                        <xsl:when test="session:noCookie()">
                           <span><a href="javascript:alert('To use the bag, you must enable cookies in your web browser.')">Requires cookie*</a></span>                                 
                        </xsl:when>
                        <xsl:otherwise>
                           <xsl:choose>
                              <xsl:when test="session:getData('bag')/bag/savedDoc[@id=$indexId]">
                                 <span>Added</span>
                              </xsl:when>
                              <xsl:otherwise>
                                 <script type="text/javascript">
                                    add_<xsl:value-of select="@rank"/> = function() {
                                       var span = YAHOO.util.Dom.get('add_<xsl:value-of select="@rank"/>');
                                       span.innerHTML = "Adding...";
                                       YAHOO.util.Connect.asyncRequest('GET', 
                                          '<xsl:value-of select="concat($xtfURL, $crossqueryPath, '?smode=addToBag;identifier=', $identifier)"/>',
                                          {  success: function(o) { 
                                                span.innerHTML = o.responseText;
                                                ++(YAHOO.util.Dom.get('itemCount').innerHTML);
                                             },
                                             failure: function(o) { span.innerHTML = 'Failed to add!'; }
                                          }, null);
                                    };
                                 </script>
                                 <span id="add_{@rank}">
                                    <a href="javascript:add_{@rank}()">Add</a>
                                 </span>
                              </xsl:otherwise>
                           </xsl:choose>
                           <xsl:value-of select="session:setData('queryURL', concat($xtfURL, $crossqueryPath, '?', $queryString))"/>
                        </xsl:otherwise>
                     </xsl:choose>
                  </xsl:if>
               </td>
            </tr>
            <tr>
               <td class="col1">
                  <xsl:text>&#160;</xsl:text>
               </td>
               <td class="col2">
                  <xsl:if test="$sort = 'title'">
                     <a name="{$anchor}"/>
                  </xsl:if>
                  <b>Title:&#160;&#160;</b>
               </td>
               <td class="col3">
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
                  <xsl:text>&#160;</xsl:text>
                  <xsl:variable name="type" select="meta/type"/>
                  <i><b><xsl:value-of select="$type"/></b></i>
               </td>
               <td class="col4">
                  <xsl:text>&#160;</xsl:text>
               </td>
            </tr>
            <tr>
               <td class="col1">
                  <xsl:text>&#160;</xsl:text>
               </td>
               <td class="col2">
                  <b>Published:&#160;&#160;</b>
               </td>
               <td class="col3">
                  <xsl:choose>
                     <xsl:when test="meta/year">
                        <xsl:value-of select="replace(meta/year,'^.+ ','')"/>
                     </xsl:when>
                     <xsl:otherwise>
                        <xsl:apply-templates select="meta/date"/>
                     </xsl:otherwise>
                  </xsl:choose>
               </td>
               <td class="col4">
                  <xsl:text>&#160;</xsl:text>
               </td>
            </tr>
            <xsl:if test="meta/subject">
               <tr>
                  <td class="col1">
                     <xsl:text>&#160;</xsl:text>
                  </td>
                  <td class="col2">
                     <b>Subjects:&#160;&#160;</b>
                  </td>
                  <td class="col3">
                     <xsl:apply-templates select="meta/subject"/>
                  </td>
                  <td class="col4">
                     <xsl:text>&#160;</xsl:text>
                  </td>
               </tr>
            </xsl:if>
            <xsl:if test="snippet">
               <tr>
                  <td class="col1">
                     <xsl:text>&#160;</xsl:text>
                  </td>
                  <td class="col2">
                     <b>Matches:&#160;&#160;</b>
                     <br/>
                     <xsl:value-of select="@totalHits"/> hit(s)&#160;&#160;&#160;&#160;
                  </td>
                  <td class="col3" colspan="2">
                     <xsl:apply-templates select="snippet" mode="text"/>
                  </td>
               </tr>
            </xsl:if>
            
            <!-- "more like this" -->
            <tr>
               <td class="col1">
                  <xsl:text>&#160;</xsl:text>
               </td>
               <td class="col2">
                  <b>Similar&#160;Items:&#160;&#160;</b>
               </td>
               <td class="col3" colspan="2">
                  <script type="text/javascript">
                     getMoreLike_<xsl:value-of select="@rank"/> = function() {
                        var span = YAHOO.util.Dom.get('moreLike_<xsl:value-of select="@rank"/>');
                        span.innerHTML = "Fetching...";
                        YAHOO.util.Connect.asyncRequest('GET', 
                           '<xsl:value-of select="concat('search?smode=moreLike;docsPerPage=5;identifier=', $identifier)"/>',
                           { success: function(o) { span.innerHTML = o.responseText; },
                             failure: function(o) { span.innerHTML = "Failed!" } 
                           }, null);
                     };
                  </script>
                  <span id="moreLike_{@rank}">
                     <a href="javascript:getMoreLike_{@rank}()">Find</a>
                  </span>
               </td>
            </tr>
            
         </table>
      </div>
      
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
               <ol>
                  <xsl:apply-templates select="docHit" mode="moreLike"/>
               </ol>
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
      
      <li>
         <xsl:apply-templates select="meta/creator[1]"/>
         <xsl:text>. </xsl:text>
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
         <xsl:text>. </xsl:text>
         <xsl:apply-templates select="meta/year[1]"/>
         <xsl:text>. </xsl:text>
      </li>
      
   </xsl:template>
   
</xsl:stylesheet>
