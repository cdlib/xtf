<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
   
   
   
   <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
   <!-- Search forms stylesheet                                                -->
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
   
   <!-- ====================================================================== -->
   <!-- Form Templates                                                         -->
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
   
</xsl:stylesheet>
