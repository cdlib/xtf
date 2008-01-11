<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
   xmlns:xs="http://www.w3.org/2001/XMLSchema"
   xmlns:xtf="http://cdlib.org/xtf" 
   xmlns:editURL="http://cdlib.org/xtf/editURL"
   exclude-result-prefixes="#all"
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
   
   <!-- ====================================================================== -->
   <!-- Import Stylesheets                                                     -->
   <!-- ====================================================================== -->
   
   <xsl:import href="format-query.xsl"/>
   <xsl:import href="spelling.xsl"/>
   
   <!-- ====================================================================== -->
   <!-- Output Parameters                                                      -->
   <!-- ====================================================================== -->
   
   <xsl:output method="xhtml" indent="yes" encoding="UTF-8" media-type="text/html" 
      doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN" 
      doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd" 
      exclude-result-prefixes="#all"/>
   
   <!-- ====================================================================== -->
   <!-- Parameters                                                             -->
   <!-- ====================================================================== -->
   
   <!-- Keyword Search (text and metadata) -->
   <xsl:param name="keyword"/>
   <xsl:param name="keyword-join"/>
   <xsl:param name="keyword-prox"/>
   <xsl:param name="keyword-exclude"/>
   <xsl:param name="fieldList"/>
   
   <!-- Full Text -->
   <xsl:param name="text"/>
   <xsl:param name="text-join"/>
   <xsl:param name="text-prox"/>
   <xsl:param name="text-exclude"/>
   <xsl:param name="text-max"/>
   
   <!-- Dublin Core Metadata Elements -->
   <xsl:param name="title"/>
   <xsl:param name="title-join"/>
   <xsl:param name="title-prox"/>
   <xsl:param name="title-exclude"/>
   <xsl:param name="title-max"/>
   
   <xsl:param name="creator"/>
   <xsl:param name="creator-join"/>
   <xsl:param name="creator-prox"/>
   <xsl:param name="creator-exclude"/>
   <xsl:param name="creator-max"/>
   
   <xsl:param name="subject"/>
   <xsl:param name="subject-join"/>
   <xsl:param name="subject-prox"/>
   <xsl:param name="subject-exclude"/>
   <xsl:param name="subject-max"/>
   
   <xsl:param name="description"/>
   <xsl:param name="description-join"/>
   <xsl:param name="description-prox"/>
   <xsl:param name="description-exclude"/>
   <xsl:param name="description-max"/>
   
   <xsl:param name="publisher"/>
   <xsl:param name="publisher-join"/>
   <xsl:param name="publisher-prox"/>
   <xsl:param name="publisher-exclude"/>
   <xsl:param name="publisher-max"/>
   
   <xsl:param name="contributor"/>
   <xsl:param name="contributor-join"/>
   <xsl:param name="contributor-prox"/>
   <xsl:param name="contributor-exclude"/>
   <xsl:param name="contributor-max"/>
   
   <xsl:param name="date"/>
   <xsl:param name="date-join"/>
   <xsl:param name="date-prox"/>
   <xsl:param name="date-exclude"/>
   <xsl:param name="date-max"/>
   
   <xsl:param name="type"/>
   <xsl:param name="type-join"/>
   <xsl:param name="type-prox"/>
   <xsl:param name="type-exclude"/>
   <xsl:param name="type-max"/>
   
   <xsl:param name="format"/>
   <xsl:param name="format-join"/>
   <xsl:param name="format-prox"/>
   <xsl:param name="format-exclude"/>
   <xsl:param name="format-max"/>
   
   <xsl:param name="identifier"/>
   <xsl:param name="identifier-join"/>
   <xsl:param name="identifier-prox"/>
   <xsl:param name="identifier-exclude"/>
   <xsl:param name="identifier-max"/>
   
   <xsl:param name="source"/>
   <xsl:param name="source-join"/>
   <xsl:param name="source-prox"/>
   <xsl:param name="source-exclude"/>
   <xsl:param name="source-max"/>
   
   <xsl:param name="language"/>
   <xsl:param name="language-join"/>
   <xsl:param name="language-prox"/>
   <xsl:param name="language-exclude"/>
   <xsl:param name="language-max"/>
   
   <xsl:param name="relation"/>
   <xsl:param name="relation-join"/>
   <xsl:param name="relation-prox"/>
   <xsl:param name="relation-exclude"/>
   <xsl:param name="relation-max"/>
   
   <xsl:param name="coverage"/>
   <xsl:param name="coverage-join"/>
   <xsl:param name="coverage-prox"/>
   <xsl:param name="coverage-exclude"/>
   <xsl:param name="coverage-max"/>
   
   <xsl:param name="rights"/>
   <xsl:param name="rights-join"/>
   <xsl:param name="rights-prox"/>
   <xsl:param name="rights-exclude"/>
   <xsl:param name="rights-max"/>
   
   <!-- Special XTF Metadata Field based on Date -->
   <xsl:param name="year"/>
   <xsl:param name="year-join"/>
   <xsl:param name="year-prox"/>
   <xsl:param name="year-exclude"/>
   <xsl:param name="year-max"/>
   
   <!-- Special XTF all field -->
   <xsl:param name="all"/>
   
   <!-- Structural Search -->
   <xsl:param name="sectionType"/>
   
   <!-- facet expand field -->
   <xsl:param name="expand"/>
   
   <!-- alpha browse parameters -->
   <xsl:param name="browse-title"/>
   <xsl:param name="browse-creator"/>
   
   <!-- Search and Result Behavior URL Parameters -->
   <xsl:param name="style"/>
   <xsl:param name="smode" select="'simple'"/>
   <xsl:param name="rmode" select="'none'"/>
   <xsl:param name="brand" select="'default'"/>
   <xsl:param name="sort"/>
   
   <!-- XML Output Parameter -->
   <xsl:param name="raw"/>
   
   <!-- Retrieve Branding Nodes -->
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
   
   <xsl:param name="brand.links" select="$brand.file//links/*"/>
   <xsl:param name="brand.header" select="$brand.file//header/*"/>
   <xsl:param name="brand.footer" select="$brand.file//footer/*"/>
   
   <!-- Paging Parameters-->  
   <xsl:param name="startDoc" as="xs:integer" select="1"/>
   <!-- Documents per Page -->
   <xsl:param name="docsPerPage" as="xs:integer">
      <xsl:choose>
         <xsl:when test="$smode = 'test' or $raw">
            <xsl:value-of select="10000"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:value-of select="20"/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:param>
   <!-- Page Block Size -->
   <xsl:param name="blockSize" as="xs:integer" select="5"/>
   <!-- Maximum number of hits allowed -->
   <xsl:param name="maxHits" as="xs:integer" select="100000"/>  
   <!-- Maximum Pages -->
   <!--<xsl:param name="maxPages" as="xs:integer" select="$maxHits div $docsPerPage"/>-->  
   <xsl:param name="maxPages" as="xs:integer">
      <xsl:choose>
         <xsl:when test="$docsPerPage > 0">
            <xsl:value-of select="$maxHits div $docsPerPage"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:value-of select="0"/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:param>
   
   <!-- Path Parameters -->
   <xsl:param name="servlet.path"/>
   <xsl:param name="root.path"/>
   <xsl:param name="xtfURL" select="$root.path"/>
   <xsl:param name="serverURL" select="replace($xtfURL, '(http://.+)[:/].+', '$1/')"/>
   <xsl:param name="crossqueryPath" select="if (matches($servlet.path, 'org.cdlib.xtf.dynaXML.DynaXML')) then 'org.cdlib.xtf.crossQuery.CrossQuery' else 'search'"/>
   <xsl:param name="dynaxmlPath" select="if (matches($servlet.path, 'org.cdlib.xtf.crossQuery.CrossQuery')) then 'org.cdlib.xtf.dynaXML.DynaXML' else 'view'"/>
   
   <!-- Grouping Parameters -->
   <xsl:param name="facet"/>
   <xsl:param name="group"/>
   <xsl:param name="startGroup" as="xs:integer" select="1"/>
   <xsl:param name="groupsPerPage" as="xs:integer" select="20"/>
   <xsl:param name="sortGroupsBy"/>
   <xsl:param name="sortDocsBy"/>
   
   <!-- Query String -->
   <!-- grab url -->
   <xsl:param name="http.URL"/>
   <!-- extract query string and clean it up -->
   <xsl:param name="queryString" select="editURL:remove(replace($http.URL, '.+search\?|.+oai\?', ''),'startDoc')"/> 
   
   <!-- Hidden Query String -->
   <xsl:template name="hidden.query">
      <xsl:param name="queryString"/>
      <xsl:variable name ="before" select="if(contains($queryString, ';')) then substring-before($queryString, ';') else $queryString"/>
      <xsl:variable name ="after" select="substring-after($queryString, ';')"/>
      <xsl:variable name="name" select="substring-before($before, '=')"/>
      <xsl:variable name="value" select="replace(substring-after($before, '='), '\+', ' ')"/>
      <input type="hidden" name="{$name}" value="{$value}"/>
      <xsl:if test="$after != ''">
         <xsl:call-template name="hidden.query">
            <xsl:with-param name="queryString" select="$after"/>
         </xsl:call-template>
      </xsl:if>
   </xsl:template>
   
   <!-- Special Robot Parameters -->
   <xsl:param name="http.User-Agent"/>
   <!-- WARNING: Inclusion of 'Wget' is for testing only, please remove before going into production -->
   <xsl:param name="robots" select="'Googlebot|Slurp|msnbot|Teoma|Wget'"></xsl:param>
   
   <!-- ====================================================================== -->
   <!-- Utility functions for handy editing of URLs                           -->
   <!-- ====================================================================== -->
   
   <xsl:function name="editURL:set">
      <xsl:param name="url"/>
      <xsl:param name="param"/>
      <xsl:param name="value"/>
      
      <xsl:variable name="regex" select="concat('(^|;)', $param, '[^;]*(;|$)')"/>
      <xsl:choose>
         <xsl:when test="matches($url, $regex)">
            <xsl:value-of select="editURL:clean(replace($url, $regex, concat(';', $param, '=', $value, ';')))"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:value-of select="editURL:clean(concat($url, ';', $param, '=', $value))"/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:function>
   
   <xsl:function name="editURL:remove">
      <xsl:param name="url"/>
      <xsl:param name="param"/>
      
      <xsl:variable name="regex" select="concat('(^|;)', $param, '[^;]*(;|$)')"/>
      <xsl:value-of select="editURL:clean(replace($url, $regex, ';'))"/>
   </xsl:function>
   
   <xsl:function name="editURL:clean">
      <xsl:param name="v0"/>
      <!-- Change old ampersands to new easy-to-read semicolons -->
      <xsl:variable name="v1" select="replace($v0, '&amp;', ';')"/>
      <!-- Get rid of empty parameters -->
      <xsl:variable name="v2" select="replace($v1, '[^;=]+=(;|$)', '')"/>
      <!-- Replace ";;" with ";" -->
      <xsl:variable name="v3" select="replace($v2, ';;+', ';')"/>
      <!-- Get rid of leading and trailing ';' -->
      <xsl:variable name="v4" select="replace($v3, '^;|;$', '')"/>
      <!-- All done. -->
      <xsl:value-of select="$v4"/>
   </xsl:function>
   
   <!-- ====================================================================== -->
   <!-- Result Paging                                                          -->
   <!-- ====================================================================== -->
   
   <!-- Summarize Results -->
   <xsl:template name="page-summary">
      
      <xsl:param name="object-type"/>
      
      <xsl:variable name="total" as="xs:integer">
         <xsl:choose>
            <xsl:when test="matches($smode,'moreLike')">
               <xsl:value-of select="'20'"/>
            </xsl:when>
            <xsl:otherwise>
               <xsl:value-of select="@totalDocs"/>
            </xsl:otherwise>
         </xsl:choose>
      </xsl:variable>
      
      <xsl:variable name="lastOnPage" as="xs:integer">
         <xsl:choose>
            <xsl:when test="(($startDoc + $docsPerPage)-1) > $total">
               <xsl:value-of select="$total"/>
            </xsl:when>
            <xsl:otherwise>
               <xsl:value-of select="($startDoc + $docsPerPage)-1"/>
            </xsl:otherwise>
         </xsl:choose>
      </xsl:variable>    
      
      <xsl:text> Displaying </xsl:text>
      <xsl:value-of select="$startDoc"/>
      <xsl:text> - </xsl:text>
      <xsl:value-of select="$lastOnPage"/>
      <xsl:text> of </xsl:text>
      <strong>
         <xsl:value-of select="$total"/>
      </strong>
      <xsl:text> </xsl:text>
      <xsl:value-of select="$object-type"/>
      
   </xsl:template>
   
   <!-- Page Linking -->  
   <xsl:template name="pages">
      
      <xsl:variable name="total" as="xs:integer">
         <xsl:value-of select="@totalDocs"/>
      </xsl:variable>
      
      <xsl:variable name="start" as="xs:integer">
         <xsl:value-of select="$startDoc"/>
      </xsl:variable>
      
      <xsl:variable name="startName">
         <xsl:value-of select="'startDoc'"/>
      </xsl:variable>
      
      <xsl:variable name="perPage" as="xs:integer">
         <xsl:value-of select="$docsPerPage"/>
      </xsl:variable>
      
      <xsl:variable name="nPages" as="xs:double">
         <xsl:value-of select="floor((($total+$perPage)-1) div $perPage)+1"/>
      </xsl:variable>
      
      <xsl:variable name="showPages" as="xs:integer">
         <xsl:choose>
            <xsl:when test="$nPages >= ($maxPages + 1)">
               <xsl:value-of select="$maxPages"/>
            </xsl:when>
            <xsl:otherwise>
               <xsl:value-of select="$nPages - 1"/>
            </xsl:otherwise>
         </xsl:choose>
      </xsl:variable>
      
      <xsl:variable name="pageQueryString">
         <xsl:value-of select="editURL:remove($queryString, 'startDoc')"/>
      </xsl:variable>   
      
      <xsl:if test="$nPages &gt; 2">
         <xsl:text>Page: </xsl:text>
      </xsl:if>
      
      <xsl:for-each select="(1 to $maxPages)">
         <!-- Figure out which block you need to be in -->
         <xsl:variable name="blockStart" as="xs:integer">
            <xsl:choose>
               <xsl:when test="$start &lt;= ($perPage * $blockSize)">
                  <xsl:value-of select="1"/>
               </xsl:when>
               <xsl:otherwise>
                  <xsl:value-of select="((floor($start div ($perPage * $blockSize))) * $blockSize) + 1"/>
               </xsl:otherwise>
            </xsl:choose>
         </xsl:variable>
         <!-- Figure out what page we're on -->
         <xsl:variable name="pageNum" as="xs:integer" select="position()"/>
         <xsl:variable name="pageStart" as="xs:integer" select="(($pageNum - 1) * $perPage) + 1"/>
         
         <!-- Individual Paging -->
         <xsl:if test="($pageNum = 1) and ($pageStart != $start)">
            <xsl:variable name="prevPage" as="xs:integer" select="$start - $perPage"/>
            <a href="{$xtfURL}{$crossqueryPath}?{$pageQueryString};{$startName}={$prevPage}">Prev</a>
            <xsl:text>&#160;&#160;</xsl:text>
         </xsl:if>
         
         <!-- Paging by Blocks -->
         <xsl:variable name="prevBlock" as="xs:integer" select="(($blockStart - $blockSize) * $perPage) - ($perPage - 1)"/>
         <xsl:if test="($pageNum = 1) and ($prevBlock &gt;= 1)">
            <a href="{$xtfURL}{$crossqueryPath}?{$pageQueryString};{$startName}={$prevBlock}">...</a>
            <xsl:text>&#160;&#160;</xsl:text>
         </xsl:if>
         
         <!-- If there are hits on the page, show it -->
         <xsl:if test="(($pageNum &gt;= $blockStart) and ($pageNum &lt;= ($blockStart + ($blockSize - 1)))) and
            (($nPages &gt; $pageNum) and ($nPages &gt; 2))">
            <xsl:choose>
               <!-- Make a hyperlink if it's not the page we're currently on. -->
               <xsl:when test="($pageStart != $start)">
                  <a href="{$xtfURL}{$crossqueryPath}?{$pageQueryString};{$startName}={$pageStart}">
                     <xsl:value-of select="$pageNum"/>
                  </a>
                  <xsl:if test="$pageNum &lt; $showPages">
                     <xsl:text>&#160;</xsl:text>
                  </xsl:if>
               </xsl:when>
               <xsl:when test="($pageStart = $start)">
                  <xsl:value-of select="$pageNum"/>
                  <xsl:if test="$pageNum &lt; $showPages">
                     <xsl:text>&#160;</xsl:text>
                  </xsl:if>
               </xsl:when>
            </xsl:choose>
         </xsl:if>
         
         <!-- Paging by Blocks -->   
         <xsl:variable name="nextBlock" as="xs:integer" select="(($blockStart + $blockSize) * $perPage) - ($perPage - 1)"/>
         <xsl:if test="($pageNum = $showPages) and (($showPages * $perPage) &gt; $nextBlock)">
            <xsl:text>&#160;&#160;</xsl:text>
            <a href="{$xtfURL}{$crossqueryPath}?{$pageQueryString};{$startName}={$nextBlock}">...</a>
         </xsl:if>
         
         <!-- Individual Paging -->      
         <xsl:if test="($pageNum = $showPages) and ($pageStart != $start)">
            <xsl:variable name="nextPage" as="xs:integer" select="$start + $perPage"/>
            <xsl:text>&#160;&#160;</xsl:text>
            <a href="{$xtfURL}{$crossqueryPath}?{$pageQueryString};{$startName}={$nextPage}">Next</a>
         </xsl:if>
         
      </xsl:for-each>
      
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Subject Links                                                          -->
   <!-- ====================================================================== -->
   
   <xsl:template match="subject">
      <a href="{$xtfURL}{$crossqueryPath}?subject={.};subject-join=exact;smode={$smode};rmode={$rmode};style={$style};brand={$brand}">
         <xsl:apply-templates/>
      </a>
      <xsl:if test="not(position() = last())">
         <xsl:text> | </xsl:text>
      </xsl:if>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- "More" Blocks                                                            -->
   <!-- ====================================================================== -->
   
   <xsl:template name="moreBlock">
      
      <xsl:param name="block"/>    
      <xsl:param name="identifier"/>
      <xsl:variable name="string" select="normalize-space(string($block))"/>
      <xsl:variable name="hideString" select="editURL:remove($queryString, 'rmode')"/>
      
      <xsl:choose>
         <xsl:when test="(contains($rmode, 'showDescrip')) and (matches($string , '.{500}'))">
            <xsl:apply-templates select="$block"/>
            <xsl:text>&#160;&#160;&#160;</xsl:text>
            <a href="{$xtfURL}{$crossqueryPath}?{$hideString};startDoc={$startDoc};rmode=hideDescrip#{$identifier}">[brief]</a>         
         </xsl:when>
         <xsl:otherwise>
            <xsl:apply-templates select="$block" mode="crop">
               <xsl:with-param name="identifier" select="$identifier"/>
            </xsl:apply-templates>        
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- Cropped blocks are not going to show search results. Not sure there is a way around this... -->
   <xsl:template match="node()" mode="crop">
      
      <xsl:param name="identifier"/>
      <xsl:variable name="string" select="normalize-space(string(.))"/>   
      <xsl:variable name="moreString" select="editURL:remove($queryString, 'rmode')"/>
      
      <xsl:choose>
         <xsl:when test="matches($string , '.{300}')">
            <xsl:value-of select="replace($string, '(.{300}).+', '$1')"/>
            <xsl:text> . . . </xsl:text>
            <a href="{$xtfURL}{$crossqueryPath}?{$moreString};startDoc={$startDoc};rmode=showDescrip#{$identifier}">[more]</a>  
         </xsl:when>
         <xsl:otherwise>
            <xsl:apply-templates/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Sort Options                                                           -->
   <!-- ====================================================================== -->
   
   <xsl:template name="sort.options">
      <select size="1" name="sort">
         <xsl:choose>
            <xsl:when test="$smode='showBag'">
               <xsl:choose>
                  <xsl:when test="$sort = ''">
                     <option value="title" selected="selected">title</option>
                     <option value="creator">author</option>
                     <option value="year">publication date</option>
                     <option value="reverse-year">reverse date</option>
                  </xsl:when>
                  <xsl:when test="$sort = 'title'">
                     <option value="title" selected="selected">title</option>
                     <option value="creator">author</option>
                     <option value="year">publication date</option>
                     <option value="reverse-year">reverse date</option>
                  </xsl:when>
                  <xsl:when test="$sort = 'creator'">
                     <option value="title">title</option>
                     <option value="creator" selected="selected">author</option>
                     <option value="year">publication date</option>
                     <option value="reverse-year">reverse date</option>
                  </xsl:when>
                  <xsl:when test="$sort = 'year'">
                     <option value="title">title</option>
                     <option value="creator">author</option>
                     <option value="year" selected="selected">publication date</option>
                     <option value="reverse-year">reverse date</option>
                  </xsl:when>
                  <xsl:when test="$sort = 'reverse-year'">
                     <option value="title">title</option>
                     <option value="creator">author</option>
                     <option value="year">publication date</option>
                     <option value="reverse-year" selected="selected">reverse date</option>
                  </xsl:when>
               </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
               <xsl:choose>
                  <xsl:when test="$sort = ''">
                     <option value="" selected="selected">relevance</option>
                     <option value="title">title</option>
                     <option value="creator">author</option>
                     <option value="year">publication date</option>
                     <option value="reverse-year">reverse date</option>
                  </xsl:when>
                  <xsl:when test="$sort = 'title'">
                     <option value="">relevance</option>
                     <option value="title" selected="selected">title</option>
                     <option value="creator">author</option>
                     <option value="year">publication date</option>
                     <option value="reverse-year">reverse date</option>
                  </xsl:when>
                  <xsl:when test="$sort = 'creator'">
                     <option value="">relevance</option>
                     <option value="title">title</option>
                     <option value="creator" selected="selected">author</option>
                     <option value="year">publication date</option>
                     <option value="reverse-year">reverse date</option>
                  </xsl:when>
                  <xsl:when test="$sort = 'year'">
                     <option value="">relevance</option>
                     <option value="title">title</option>
                     <option value="creator">author</option>
                     <option value="year" selected="selected">publication date</option>
                     <option value="reverse-year">reverse date</option>
                  </xsl:when>
                  <xsl:when test="$sort = 'reverse-year'">
                     <option value="">relevance</option>
                     <option value="title">title</option>
                     <option value="creator">author</option>
                     <option value="year">publication date</option>
                     <option value="reverse-year" selected="selected">reverse date</option>
                  </xsl:when>
               </xsl:choose>
            </xsl:otherwise>
         </xsl:choose>
      </select>
   </xsl:template>  
   
   <!-- ====================================================================== -->
   <!-- dynaXML URL Template                                                   -->
   <!-- ====================================================================== -->
   
   <xsl:template name="dynaxml.url">
      
      <xsl:param name="path"/>
      
      <xsl:variable name="docId">
         <xsl:choose>
            <xsl:when test="matches($path,'^[A-Za-z0-9]+:')">
               <xsl:value-of select="replace($path, '^[A-Za-z0-9]+:', '')"/>
            </xsl:when>
            <xsl:otherwise>
               <xsl:value-of select="$path"/>
            </xsl:otherwise>
         </xsl:choose>
      </xsl:variable>
      
      <xsl:variable name="query">
         <xsl:choose>
            <xsl:when test="$keyword != ''">
               <xsl:value-of select="$keyword"/>
            </xsl:when>
            <xsl:otherwise>
               <xsl:value-of select="$text"/>
            </xsl:otherwise>
         </xsl:choose>
      </xsl:variable>
      
      <xsl:value-of select="concat($dynaxmlPath, '?docId=', $docId, ';query=', replace($query, ';', '%26'))"/>
      <!-- -join & -prox are mutually exclusive -->
      <xsl:choose>
         <xsl:when test="$text-prox">
            <xsl:value-of select="concat(';query-prox=', $text-prox)"/>
         </xsl:when>
         <xsl:when test="$text-join">
            <xsl:value-of select="concat(';query-join=', $text-join)"/>
         </xsl:when>            
      </xsl:choose>
      <xsl:if test="$text-exclude">
         <xsl:value-of select="concat(';query-exclude=', $text-exclude)"/>
      </xsl:if>
      <!-- -join & -prox are mutually exclusive -->
      <xsl:choose>
         <xsl:when test="$keyword-prox">
            <xsl:value-of select="concat(';query-prox=', $keyword-prox)"/>
         </xsl:when>
         <xsl:when test="$keyword-join">
            <xsl:value-of select="concat(';query-join=', $keyword-join)"/>
         </xsl:when>            
      </xsl:choose>
      <xsl:if test="$keyword-exclude">
         <xsl:value-of select="concat(';query-exclude=', $keyword-exclude)"/>
      </xsl:if>
      <xsl:if test="$sectionType">
         <xsl:value-of select="concat(';sectionType=', $sectionType)"/>
      </xsl:if>
      <xsl:if test="$brand">
         <xsl:value-of select="concat(';brand=',$brand)"/>
      </xsl:if>
      
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Raw document display URL Template                                      -->
   <!-- ====================================================================== -->
   
   <xsl:template name="rawDisplay.url">
      <xsl:param name="path"/>
      <xsl:variable name="file" select="replace($path, '[^:]+:(.*)', '$1')"/>
      <xsl:value-of select="concat($xtfURL, 'data/', $file)"/>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Select Builder                                                         -->
   <!-- ====================================================================== -->
   
   <xsl:template name="selectBuilder">
      <xsl:param name="selectType"/>
      <xsl:param name="optionList"/>
      <xsl:param name="count"/>
      
      <xsl:variable name="option" select="substring-before($optionList, '::')"/>
      
      <xsl:choose>
         <xsl:when test="$selectType='subject'">    
            <xsl:if test="$option != ''"> 
               <option>
                  <xsl:attribute name="value">"<xsl:value-of select="$option"/>"</xsl:attribute>
                  <xsl:if test="contains($subject,$option)">
                     <xsl:attribute name="selected" select="'yes'"/>
                  </xsl:if>
                  <xsl:value-of select="$option"/>
               </option>    
               <xsl:call-template name="selectBuilder">
                  <xsl:with-param name="selectType" select="$selectType"/>
                  <xsl:with-param name="optionList" select="replace(substring-after($optionList, $option), '^::', '')"/>
                  <xsl:with-param name="count" select="$count + 1"/>
               </xsl:call-template>
            </xsl:if>
         </xsl:when>
         <xsl:when test="$selectType='keyword-prox'">    
            <xsl:if test="$option != ''"> 
               <option>
                  <xsl:attribute name="value"><xsl:value-of select="$option"/></xsl:attribute>
                  <xsl:if test="$keyword-prox = $option">
                     <xsl:attribute name="selected" select="'yes'"/>
                  </xsl:if>
                  <xsl:value-of select="$option"/>
               </option>    
               <xsl:call-template name="selectBuilder">
                  <xsl:with-param name="selectType" select="$selectType"/>
                  <xsl:with-param name="optionList" select="replace(substring-after($optionList, $option), '^::', '')"/>
                  <xsl:with-param name="count" select="$count + 1"/>
               </xsl:call-template>
            </xsl:if>
         </xsl:when>     
         <xsl:when test="$selectType='text-prox'">    
            <xsl:if test="$option != ''"> 
               <option>
                  <xsl:attribute name="value"><xsl:value-of select="$option"/></xsl:attribute>
                  <xsl:if test="$text-prox = $option">
                     <xsl:attribute name="selected" select="'yes'"/>
                  </xsl:if>
                  <xsl:value-of select="$option"/>
               </option>    
               <xsl:call-template name="selectBuilder">
                  <xsl:with-param name="selectType" select="$selectType"/>
                  <xsl:with-param name="optionList" select="replace(substring-after($optionList, $option), '^::', '')"/>
                  <xsl:with-param name="count" select="$count + 1"/>
               </xsl:call-template>
            </xsl:if>
         </xsl:when>      
         <xsl:when test="$selectType='year'">    
            <xsl:if test="$option != ''"> 
               <option>
                  <xsl:attribute name="value"><xsl:value-of select="$option"/></xsl:attribute>
                  <xsl:if test="$year = $option">
                     <xsl:attribute name="selected" select="'yes'"/>
                  </xsl:if>
                  <xsl:value-of select="$option"/>
               </option>    
               <xsl:call-template name="selectBuilder">
                  <xsl:with-param name="selectType" select="$selectType"/>
                  <xsl:with-param name="optionList" select="replace(substring-after($optionList, $option), '^::', '')"/>
                  <xsl:with-param name="count" select="$count + 1"/>
               </xsl:call-template>
            </xsl:if>
         </xsl:when> 
         <xsl:when test="$selectType='year-max'">    
            <xsl:if test="$option != ''"> 
               <option>
                  <xsl:attribute name="value"><xsl:value-of select="$option"/></xsl:attribute>
                  <xsl:if test="$year-max = $option">
                     <xsl:attribute name="selected" select="'yes'"/>
                  </xsl:if>
                  <xsl:value-of select="$option"/>
               </option>    
               <xsl:call-template name="selectBuilder">
                  <xsl:with-param name="selectType" select="$selectType"/>
                  <xsl:with-param name="optionList" select="replace(substring-after($optionList, $option), '^::', '')"/>
                  <xsl:with-param name="count" select="$count + 1"/>
               </xsl:call-template>
            </xsl:if>
         </xsl:when>
      </xsl:choose>    
      
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Alpha List Builder                                                     -->
   <!-- ====================================================================== -->
   
   <xsl:template name="alphaList">
      
      <xsl:param name="alphaList"/>
      
      <xsl:variable name="browse-name">
         <xsl:choose>
            <xsl:when test="$browse-creator">
               <xsl:value-of select="'creator'"/>
            </xsl:when>
            <xsl:when test="$browse-title">
               <xsl:value-of select="'title'"/>
            </xsl:when>
         </xsl:choose>
      </xsl:variable>
      
      <xsl:variable name="browse-value">
         <xsl:choose>
            <xsl:when test="$browse-creator">
               <xsl:value-of select="$browse-creator"/>
            </xsl:when>
            <xsl:when test="$browse-title">
               <xsl:value-of select="$browse-title"/>
            </xsl:when>
         </xsl:choose>
      </xsl:variable>
      
      <xsl:variable name="alpha" select="replace($alphaList,' .+$','')"/>
      
      <xsl:variable name="browse-link">
         <xsl:choose>
            <xsl:when test="matches($alpha,'^.$')">
               <xsl:value-of select="lower-case(concat($alpha,$alpha))"/>
            </xsl:when>
            <xsl:otherwise>
               <xsl:value-of select="lower-case($alpha)"/>
            </xsl:otherwise>
         </xsl:choose>
      </xsl:variable>
      
      <xsl:choose>
         <xsl:when test="lower-case($browse-link) = $browse-value">
            <span style="color: red"><xsl:value-of select="upper-case($alpha)"/></span>
         </xsl:when>
         <xsl:when test="facet[@field=concat('browse-',$browse-name)]/group[@value=$browse-link]">
            <a href="{$xtfURL}{$crossqueryPath}?browse-{$browse-name}={$browse-link};sort={$browse-name}"><xsl:value-of select="$alpha"/></a>
         </xsl:when>
         <xsl:otherwise>
            <xsl:value-of select="upper-case($alpha)"/>
         </xsl:otherwise>
      </xsl:choose>
      
      <xsl:if test="contains($alphaList,' ')">
         <xsl:text> | </xsl:text>
         <xsl:call-template name="alphaList">
            <xsl:with-param name="alphaList" select="replace($alphaList,'^[A-Z]+ ','')"/>
         </xsl:call-template>
      </xsl:if>
      
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- URL Encoding                                                           -->
   <!-- ====================================================================== -->
   
   <xtf:encoding-map>
      <xtf:regex>
         <xtf:find>%</xtf:find>    <!-- % must be done first! -->
         <xtf:replace>%25</xtf:replace>
      </xtf:regex>
      <xtf:regex>
         <xtf:find>SPACE</xtf:find>
         <xtf:replace>%20</xtf:replace>
      </xtf:regex>
      <xtf:regex>
         <xtf:find>"</xtf:find>
         <xtf:replace>%22</xtf:replace>
      </xtf:regex>
      <xtf:regex>
         <xtf:find>'</xtf:find>
         <xtf:replace>%27</xtf:replace>
      </xtf:regex>
      <xtf:regex>
         <xtf:find>&lt;</xtf:find>
         <xtf:replace>%3C</xtf:replace>
      </xtf:regex>
      <xtf:regex>
         <xtf:find>&gt;</xtf:find>
         <xtf:replace>%3E</xtf:replace>
      </xtf:regex>
      <xtf:regex>
         <xtf:find>#</xtf:find>
         <xtf:replace>%23</xtf:replace>
      </xtf:regex>
   </xtf:encoding-map>
   
   <xsl:template name="url-encode">
      <xsl:param name="url-string"/>
      <xsl:param name="regex" select="document('')/*/xtf:encoding-map/xtf:regex"/>
      <xsl:variable name="encoded-string">
         <xsl:call-template name="regex-replace">
            <xsl:with-param name="string" select="$url-string"/>
            <xsl:with-param name="find" select="$regex[1]/xtf:find"/>
            <xsl:with-param name="replace" select="$regex[1]/xtf:replace"/>
         </xsl:call-template>
      </xsl:variable>
      <xsl:choose>
         <xsl:when test="$regex[2]">
            <xsl:call-template name="url-encode">
               <xsl:with-param name="url-string" select="$encoded-string"/>
               <xsl:with-param name="regex" select="$regex[position() > 1]"/>
            </xsl:call-template>
         </xsl:when>
         <xsl:otherwise>
            <xsl:value-of select="$encoded-string"/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <xsl:template name="regex-replace">
      <xsl:param name="string"/>
      <xsl:param name="find"/>
      <xsl:param name="replace"/>
      <xsl:choose>
         <xsl:when test="contains($string, ' ') and $find='SPACE'">
            <xsl:value-of select="substring-before($string, ' ')"/>
            <xsl:value-of select="$replace"/>
            <xsl:call-template name="regex-replace">
               <xsl:with-param name="string" select="substring-after($string, ' ')"/>
               <xsl:with-param name="find" select="$find"/>
               <xsl:with-param name="replace" select="$replace"/>
            </xsl:call-template>
         </xsl:when>
         <xsl:when test="contains($string, $find)">
            <xsl:value-of select="substring-before($string, $find)"/>
            <xsl:value-of select="$replace"/>
            <xsl:call-template name="regex-replace">
               <xsl:with-param name="string" select="substring-after($string, $find)"/>
               <xsl:with-param name="find" select="$find"/>
               <xsl:with-param name="replace" select="$replace"/>
            </xsl:call-template>
         </xsl:when>
         <xsl:otherwise>
            <xsl:value-of select="$string"/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>  
   
   <!-- ====================================================================== -->
   <!-- Robot Browse Template                                                  -->
   <!-- ====================================================================== -->
   
   <xsl:template match="crossQueryResult" mode="robot">
      <html>
         <head>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
            <title>XTF: Results</title>
         </head>
         <body>
            <xsl:variable name="TD" select="@totalDocs"/>
            <xsl:variable name="SD">
               <xsl:choose>
                  <xsl:when test="$startDoc">
                     <xsl:value-of select="$startDoc + 90"/>
                  </xsl:when>
                  <xsl:otherwise>
                     <xsl:value-of select="1" />
                  </xsl:otherwise>
               </xsl:choose>
            </xsl:variable>
            <xsl:if test="($TD - $SD) > 0">
               <a href="{$xtfURL}search?startDoc={$SD}">NEXT</a>
            </xsl:if>
            <ol>
               <xsl:apply-templates select="docHit" mode="robot"/>
            </ol>
         </body>
      </html>
   </xsl:template>
   
   <xsl:template match="docHit" mode="robot">
      
      <xsl:variable name="path" select="@path"/>
      
      <li>
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
            <xsl:value-of select="meta/title[1]"/>
         </a>
      </li>
   </xsl:template>
   
</xsl:stylesheet>
