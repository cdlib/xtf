<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
   xmlns:xs="http://www.w3.org/2001/XMLSchema"
   xmlns:xtf="http://cdlib.org/xtf" 
   xmlns:editURL="http://cdlib.org/xtf/editURL"
   xmlns="http://www.w3.org/1999/xhtml"
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
   
   <xsl:import href="../../../xtfCommon/xtfCommon.xsl"/>
   <xsl:import href="format-query.xsl"/>
   <xsl:import href="spelling.xsl"/>
   
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
   <xsl:param name="browse-all"/>
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
   
   <xsl:param name="brand.links" select="$brand.file//links/*" xpath-default-namespace="http://www.w3.org/1999/xhtml"/>
   <xsl:param name="brand.header" select="$brand.file//header/*" xpath-default-namespace="http://www.w3.org/1999/xhtml"/>
   <xsl:param name="brand.footer" select="$brand.file//footer/*" xpath-default-namespace="http://www.w3.org/1999/xhtml"/>
   
   <!-- Paging Parameters-->  
   <xsl:param name="startDoc" as="xs:integer" select="1"/>
   <!-- Documents per Page -->
   <xsl:param name="docsPerPage" as="xs:integer">
      <xsl:choose>
         <xsl:when test="$raw">
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
            <xsl:value-of select="ceiling($maxHits div $docsPerPage)"/>
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
   <xsl:param name="servlet.URL"/>
   
   <!-- extract query string and clean it up -->
   <xsl:param name="queryString">
      <xsl:variable name="pieces">
         <xsl:value-of select="replace($servlet.URL, '.+search(\?|$)|.+oai(\?|$)', '')"/>
         <xsl:for-each select="//param">
            <xsl:if test="string-length(@value) &gt; 0 and @name != 'startDoc'">
               <xsl:value-of select="concat(@name, '=', editURL:protectValue(@value), ';')"/>
            </xsl:if>
         </xsl:for-each>
      </xsl:variable>
      <xsl:value-of select="editURL:clean(string-join($pieces, ''))"/>
   </xsl:param>
   
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
   <xsl:param name="http.user-agent"/>
   <!-- WARNING: Inclusion of 'Wget' is for testing only, please remove before going into production -->
   <xsl:param name="robots" select="'Googlebot|Slurp|msnbot|Teoma|Wget'"></xsl:param>
   
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
               <xsl:value-of select="/crossQueryResult/@totalDocs"/>
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
          <xsl:value-of select="/crossQueryResult/@totalDocs"/>
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
      
      <xsl:choose>
         <xsl:when test="$nPages &gt; 2">
            <xsl:text>Page: </xsl:text>
         </xsl:when>
         <xsl:otherwise>
            <xsl:text>Page: 1</xsl:text>
         </xsl:otherwise>
      </xsl:choose>
      
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
      <a href="{$xtfURL}{$crossqueryPath}?subject={editURL:protectValue(.)};subject-join=exact;smode={$smode};rmode={$rmode};style={$style};brand={$brand}">
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

      <!-- Must protect value of $docId in the URL, in case it contains special chars. -->
      <xsl:value-of select="concat($dynaxmlPath, '?docId=', editURL:protectValue($docId), ';query=', replace($query, ';', '%26'))"/>
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
          <xsl:when test="/crossQueryResult/facet[@field=concat('browse-',$browse-name)]/group[@value=$browse-link and count(descendant::group[docHit]) > 1]">
            <span style="color: red"><xsl:value-of select="upper-case($alpha)"/></span>
         </xsl:when>
          <xsl:when test="/crossQueryResult/facet[@field=concat('browse-',$browse-name)]/group[@value=$browse-link and count(docHit) > 0]">
            <span style="color: red"><xsl:value-of select="upper-case($alpha)"/></span>
         </xsl:when>
          <xsl:when test="/crossQueryResult/facet[@field=concat('browse-',$browse-name)]/group[@value=$browse-link]">
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
   <!-- Robot Browse Template                                                  -->
   <!-- ====================================================================== -->
   
   <xsl:template match="crossQueryResult" mode="robot">
      
      <!-- Page Maximum: this will handle up to 1 million objects -->
      <xsl:param name="PM" select="11112"/>
      <!-- Total Documents -->
       <xsl:param name="TD" select="/crossQueryResult/@totalDocs"/>
      <!-- Page Number -->
      <xsl:param name="PN" select="($startDoc - 1) div 90"/>
      <!-- Start Document -->
      <xsl:param name="SD">
         <xsl:choose>
            <xsl:when test="$startDoc">
               <xsl:value-of select="$startDoc + 90"/>
            </xsl:when>
            <xsl:otherwise>
               <xsl:value-of select="1" />
            </xsl:otherwise>
         </xsl:choose>
      </xsl:param>
      
      <html>
         <head>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
            <title>XTF: Results</title>
         </head>
         <body>
            
            <ol>
               <xsl:apply-templates select="docHit" mode="robot"/>
            </ol>
            
            <xsl:if test="($TD - $SD) > 0">
               <xsl:for-each select="(1 to 10)">
                  
                  <!-- New Page -->
                  <xsl:variable name="NP" select="($PN * 10) + position()"/>
                  <!-- New Start Document -->
                  <xsl:variable name="NS" select="($NP * 90) + 1"/>
                  
                  <xsl:if test="($NS &lt; $TD) and ($NP &lt; $PM)">
                     <a href="{$xtfURL}search?startDoc={$NS}"><xsl:value-of select="$NP"/></a>
                     <br/>
                  </xsl:if>
                  
               </xsl:for-each>
            </xsl:if>
            
         </body>
      </html>
   </xsl:template>
   
   <xsl:template match="docHit" mode="robot">
      
       <xsl:variable name="path" select="/crossQueryResult/@path"/>
      
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
   
   <!-- ====================================================================== -->
   <!-- Facets and their Groups                                                -->
   <!-- ====================================================================== -->
   
   <!-- Facet -->
    <xsl:template match="/crossQueryResult/facet[matches(@field,'^facet-')]" exclude-result-prefixes="#all">
      <xsl:variable name="field" select="replace(@field, 'facet-(.*)', '$1')"/>
      <xsl:variable name="needExpand" select="@totalGroups > count(group)"/>
      <div class="facet">
         <div class="facetName">
            <xsl:apply-templates select="." mode="facetName"/>
         </div>
         <xsl:if test="$expand=$field">
            <div class="facetLess">
               <i><a href="{$xtfURL}{$crossqueryPath}?{editURL:remove($queryString,'expand')}">less</a></i>
            </div>
         </xsl:if>
         <div class="facetGroup">
            <table>
               <xsl:apply-templates/>
            </table>
         </div>
         <xsl:if test="$needExpand and not($expand=$field)">
            <div class="facetMore">
               <i><a href="{$xtfURL}{$crossqueryPath}?{editURL:set($queryString,'expand',$field)}">more</a></i>
            </div>
         </xsl:if>
      </div>
   </xsl:template>
   
   <!-- Plain (non-hierarchical) group of a facet -->
   <xsl:template match="group[not(parent::group) and @totalSubGroups = 0]" exclude-result-prefixes="#all">
      <xsl:variable name="field" select="replace(ancestor::facet/@field, 'facet-(.*)', '$1')"/>
      <xsl:variable name="value" select="@value"/>
      <xsl:variable name="nextName" select="editURL:nextFacetParam($queryString, $field)"/>
      
      <xsl:variable name="selectLink" select="
         concat(xtfURL, $crossqueryPath, '?',
                editURL:set(editURL:remove($queryString,'browse-all=yes'), 
                            $nextName, $value))">
      </xsl:variable>
      
      <xsl:variable name="clearLink" select="
         concat(xtfURL, $crossqueryPath, '?',
                editURL:replaceEmpty(
                   editURL:remove($queryString, 
                                  concat('f[0-9]+-',$field,'=',
                                         editURL:escapeRegex(editURL:protectValue($value)))),
                   'browse-all=yes'))">
      </xsl:variable>
      
      <tr>
         <td class="col1">&#8226;</td> <!-- bullet char -->
         <!-- Display the group name, with '[X]' box if it is selected. -->
         <xsl:choose>
            <xsl:when test="//param[matches(@name,concat('f[0-9]+-',$field))]/@value=$value">
               <td class="col2">
                  <xsl:apply-templates select="." mode="beforeGroupValue"/>
                  <i>
                     <xsl:value-of select="$value"/>
                  </i>
                  <xsl:apply-templates select="." mode="afterGroupValue"/>
               </td>
               <td class="col3">
                  <a href="{$clearLink}">[X]</a>
               </td>
            </xsl:when>
            <xsl:otherwise>
               <td class="col2">
                  <xsl:apply-templates select="." mode="beforeGroupValue"/>
                  <a href="{$selectLink}">
                     <xsl:value-of select="$value"/>
                  </a>
                  <xsl:apply-templates select="." mode="afterGroupValue"/>
               </td>
               <td class="col3">
                  (<xsl:value-of select="@totalDocs"/>)
               </td>
            </xsl:otherwise>
         </xsl:choose>
      </tr>
   </xsl:template>
   
   <!-- Hierarchical group or sub-group of facet -->
   <xsl:template match="group[parent::group or @totalSubGroups > 0]" exclude-result-prefixes="#all">
      <xsl:variable name="field" select="replace(ancestor::facet/@field, 'facet-(.*)', '$1')"/>
      
      <!-- Value of the parent of this group (if any) -->
      <xsl:variable name="parentValue">
         <xsl:for-each select="parent::*/ancestor::group/@value">
            <xsl:value-of select="concat(.,'::')"/>
         </xsl:for-each>
         <xsl:value-of select="parent::group/@value"/>
      </xsl:variable>
      
      <!-- The full hierarchical value of this group, including its ancestor groups if any -->
      <xsl:variable name="fullValue" select="if ($parentValue != '') then concat($parentValue,'::',@value) else @value"/>
      
      <!-- Make a query string with this value's parameter (and ancestors and children) cleared -->
      <xsl:variable name="clearedString">
         <xsl:analyze-string select="$queryString" regex="{concat('f[0-9]+-',$field,'=([^;]+)')}">
            <xsl:matching-substring>
               <xsl:variable name="paramValue" select="regex-group(1)"/>
               <xsl:choose>
                  <!-- Clear this group, its ancestors, and it descendants -->
                  <xsl:when test="$paramValue = $fullValue"/>
                  <xsl:when test="matches($paramValue, concat('^', $fullValue, '::'))"/>
                  <xsl:when test="matches($fullValue, concat('^', $paramValue, '::'))"/>
                  <!-- Keep everything else -->
                  <xsl:otherwise><xsl:value-of select="."/></xsl:otherwise>
               </xsl:choose>
            </xsl:matching-substring>
            <xsl:non-matching-substring><xsl:value-of select="."/></xsl:non-matching-substring>
         </xsl:analyze-string>
      </xsl:variable>
      
      <!-- Pick an unused number for the next parameter -->
      <xsl:variable name="nextName" select="editURL:nextFacetParam($clearedString, $field)"/>

      <!-- Clicking on this node will do one of three things:
           (1) Expand the node;
           (2) Collapse the node (returning to the parent level); or
           (3) Clear this facet
      -->

      <!-- Link to expand or select a facet group -->
      <xsl:variable name="selectLink" select="
         concat(xtfURL, $crossqueryPath, '?',
                editURL:remove(editURL:set($clearedString, $nextName, $fullValue), 'browse-all'))">
      </xsl:variable>
      
      <!-- Link to collapse a facet group -->
      <xsl:variable name="collapseLink" select="
         concat(xtfURL, $crossqueryPath, '?',
                editURL:set($clearedString, $nextName, $parentValue))">
      </xsl:variable>

      <!-- Link to clear the facet -->
      <xsl:variable name="clearLink" select="
         concat(xtfURL, $crossqueryPath, '?',
                editURL:clean(editURL:replaceEmpty($clearedString, 'browse-all=yes')))">
      </xsl:variable>
      
      <!-- Figure out which choice to give the user for this group -->
      <tr>
         <xsl:choose>
            <!-- selected terminal node: click [X] to collapse -->
            <xsl:when test="count(group) = 0 and //param[matches(@name,concat('f[0-9]+-',$field))]/@value=$fullValue">
               <td class="col1">&#8226;</td> <!-- bullet char -->
               <td class="col2">
                  <xsl:apply-templates select="." mode="beforeGroupValue"/>
                  <i>
                     <xsl:value-of select="@value"/>
                  </i>
                  <xsl:apply-templates select="." mode="afterGroupValue"/>
               </td>
               <td class="col3"><a href="{$collapseLink}">[X]</a></td>
            </xsl:when>
            
            <!-- non-selected terminal node: click to select -->
            <xsl:when test="count(group) = 0 and @totalSubGroups = 0">
               <td class="col1">&#8226;</td> <!-- bullet char -->
               <td class="col2">
                  <xsl:apply-templates select="." mode="beforeGroupValue"/>
                  <a href="{$selectLink}">
                     <xsl:value-of select="@value"/>
                  </a>
                  <xsl:apply-templates select="." mode="afterGroupValue"/>
               </td>
               <td class="col3">
                  (<xsl:value-of select="@totalDocs"/>)
               </td>
            </xsl:when>
            
            <!-- closed node: click to expand -->
            <xsl:when test="count(group) = 0">
               <td class="col1">
                  <a href="{$selectLink}">
                     <img src="{$icon.path}/i_expand.gif" border="0" alt="expand"/>
                  </a>
               </td>
               <td class="col2">
                  <xsl:apply-templates select="." mode="beforeGroupValue"/>
                  <a href="{$selectLink}">
                     <xsl:value-of select="@value"/>
                  </a>
                  <xsl:apply-templates select="." mode="afterGroupValue"/>
               </td>
               <td class="col3">
                  (<xsl:value-of select="@totalDocs"/>)
               </td>
            </xsl:when>
               
            <!-- top-level open node: click to clear the facet -->
            <xsl:when test="not(parent::group)">
               <td class="col1">
                  <a href="{$clearLink}">
                     <img src="{$icon.path}/i_colpse.gif" border="0" alt="collapse"/>
                  </a>
               </td>
               <td class="col2">
                  <xsl:apply-templates select="." mode="beforeGroupValue"/>
                  <a href="{$clearLink}">
                     <xsl:value-of select="@value"/>
                  </a>
                  <xsl:apply-templates select="." mode="afterGroupValue"/>
               </td>
               <td class="col3"/>
            </xsl:when>
               
            <!-- mid-level open node: click to collapse -->
            <xsl:otherwise>
               <td class="col1">
                  <a href="{$collapseLink}">
                     <img src="{$icon.path}/i_colpse.gif" border="0" alt="collapse"/>
                  </a>
               </td>
               <td colspan="col2">
                  <xsl:apply-templates select="." mode="beforeGroupValue"/>
                  <a href="{$collapseLink}">
                     <xsl:value-of select="@value"/>
                  </a>
                  <xsl:apply-templates select="." mode="afterGroupValue"/>
               </td>
               <td colspan="col3"/>
            </xsl:otherwise>
         </xsl:choose>
      </tr>
      
      <!-- Handle sub-groups if any -->
      <xsl:if test="group">
         <tr>
            <td class="col1"/>
            <td class="col2" colspan="2">
               <div class="facetSubGroup">
                  <table border="0" cellspacing="0" cellpadding="0">
                     <xsl:apply-templates/>                  
                  </table>
               </div>
            </td>
         </tr>
      </xsl:if>
   </xsl:template>

   <!-- Default template to display the name of a facet. Override to specialize. -->
   <xsl:template match="facet" mode="facetName" priority="-1">
      <xsl:variable name="rawName" select="replace(@field, '^facet-', '')"/>
      <xsl:value-of select="concat(upper-case(substring($rawName, 1, 1)), substring($rawName, 2))"/>
   </xsl:template>

   <!-- Default template to add data before the name of a facet group. Override to specialize. -->
   <xsl:template match="group" mode="beforeGroupValue" priority="-1">
   </xsl:template>

   <!-- Default template to add data after the name of a facet group. Override to specialize. -->
   <xsl:template match="group" mode="afterGroupValue" priority="-1">
   </xsl:template>
   
</xsl:stylesheet>
