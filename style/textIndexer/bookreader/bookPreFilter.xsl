<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
   xmlns:FileUtils="java:org.cdlib.xtf.xslt.FileUtils"
   xmlns:local="http://cdlib.org/local"
   xmlns:METS="http://www.loc.gov/METS/"
   xmlns:parse="http://cdlib.org/xtf/parse"
   xmlns:saxon="http://saxon.sf.net/"
   xmlns:scribe="http://archive.org/scribe/xml"
   xmlns:xlink="http://www.w3.org/1999/xlink"
   xmlns:xs="http://www.w3.org/2001/XMLSchema"
   xmlns:xtf="http://cdlib.org/xtf"
   exclude-result-prefixes="#all">
   
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
   
   <!-- ====================================================================== -->
   <!-- Import Common Templates and Functions                                  -->
   <!-- ====================================================================== -->
   
   <xsl:import href="../common/preFilterCommon.xsl"/>
   <!-- ====================================================================== -->
   <!-- Output parameters                                                      -->
   <!-- ====================================================================== -->
   
   <xsl:output method="xml" 
      indent="yes" 
      encoding="UTF-8"/>
   
   <!-- ====================================================================== -->
   <!-- Global variables                                                       -->
   <!-- ====================================================================== -->
   
   <xsl:variable name="metsMeta" select="//METS:xmlData/metadata"/>
   
   <xsl:variable name="pageAssertions">
      <xsl:for-each select="//*:pageNumData/*:assertion[matches(*:pageNum/string(), '^[0-9]+$')]">
         <xsl:sort select="number(*:pageNum/string())"/>
         <assertion pageNum="{*:pageNum/string()}" leafNum="{*:leafNum/string()}"/>
      </xsl:for-each>
   </xsl:variable>
   
   <xsl:variable name="numLeaves" select="count(//*:pageData/*:page)"/>
      
   <xsl:variable name="leafToPage">
      <xsl:call-template name="makeLeafToPage">
         <xsl:with-param name="prevLeafNum" select="1"/>
         <xsl:with-param name="isAsserted" select="false()"/>
         <xsl:with-param name="prevPageNum" select="1"/>
         <xsl:with-param name="assertions" select="$pageAssertions/*"/>
      </xsl:call-template>
   </xsl:variable>
   
   <!-- ====================================================================== -->
   <!-- Root Template                                                          -->
   <!-- ====================================================================== -->
   
   <xsl:template match="/*">
      <xtf-converted-book>
         <xsl:namespace name="xtf" select="'http://cdlib.org/xtf'"/>
         <xsl:call-template name="get-meta"/>
         <xsl:call-template name="convertPages"/>
      </xtf-converted-book>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Metadata Indexing                                                      -->
   <!-- ====================================================================== -->
   
   <xsl:template name="get-meta">
      <!-- Access Dublin Core Record (if present) -->
      <xsl:variable name="dcMeta">
         <xsl:call-template name="get-dc-meta"/>
      </xsl:variable>
      
      <!-- If no Dublin Core present, then extract meta-data from the METS -->
      <xsl:variable name="meta">
         <xsl:choose>
            <xsl:when test="$dcMeta/*">
               <xsl:copy-of select="$dcMeta"/>
            </xsl:when>
            <xsl:otherwise>
               <xsl:call-template name="get-book-title"/>
               <xsl:call-template name="get-book-creator"/>
               <xsl:call-template name="get-book-subject"/>
               <xsl:call-template name="get-book-publisher"/>
               <xsl:call-template name="get-book-contributor"/>
               <xsl:call-template name="get-book-date"/>
               <xsl:call-template name="get-book-type"/>
               <xsl:call-template name="get-book-format"/>
               <xsl:call-template name="get-book-identifier"/>
               <xsl:call-template name="get-book-language"/>
               <xsl:call-template name="get-book-relation"/>
               <xsl:call-template name="get-book-coverage"/>
               <xsl:call-template name="get-book-rights"/>
               <!-- special values for OAI -->
               <xsl:call-template name="oai-datestamp"/>
               <xsl:call-template name="oai-set"/>
            </xsl:otherwise>
         </xsl:choose>
      </xsl:variable>
      
      <!-- Add doc kind and sort fields to the data, and output the result. -->
      <xsl:call-template name="add-fields">
         <xsl:with-param name="display" select="'dynaxml'"/>
         <xsl:with-param name="meta" select="$meta"/>
      </xsl:call-template>    
   </xsl:template>
   
   <!-- title -->
   <xsl:template name="get-book-title">
      <xsl:choose>
         <xsl:when test="$metsMeta/title">
            <title xtf:meta="true">
               <xsl:value-of select="string($metsMeta/title[1])"/>
            </title>
         </xsl:when>
         <xsl:otherwise>
            <title xtf:meta="true">
               <xsl:value-of select="'unknown'"/>
            </title>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- creator -->
   <xsl:template name="get-book-creator">
      <xsl:choose>
         <xsl:when test="$metsMeta/creator">
            <xsl:for-each select="$metsMeta/creator">
               <creator xtf:meta="true">
                  <xsl:value-of select="."/>
               </creator>
            </xsl:for-each>
         </xsl:when>
         <xsl:otherwise>
            <creator xtf:meta="true">
               <xsl:value-of select="'unknown'"/>
            </creator>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- subject -->
   <xsl:template name="get-book-subject">
      <xsl:choose>
         <xsl:when test="$metsMeta/subject">
            <xsl:for-each select="$metsMeta/subject">
               <subject xtf:meta="true">
                  <xsl:value-of select="."/>
               </subject>
            </xsl:for-each>
         </xsl:when>
      </xsl:choose>
   </xsl:template>
   
   <!-- publisher -->
   <xsl:template name="get-book-publisher">
      <xsl:choose>
         <xsl:when test="$metsMeta/publisher">
            <publisher xtf:meta="true">
               <xsl:value-of select="string($metsMeta/publisher[1])"/>
            </publisher>
         </xsl:when>
         <xsl:otherwise>
            <publisher xtf:meta="true">
               <xsl:value-of select="'unknown'"/>
            </publisher>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- contributor -->
   <xsl:template name="get-book-contributor">
      <xsl:choose>
         <xsl:when test="$metsMeta/contributor">
            <xsl:for-each select="$metsMeta/contributor">
               <contributor xtf:meta="true">
                  <xsl:value-of select="."/>
               </contributor>
            </xsl:for-each>
         </xsl:when>
         <xsl:otherwise>
            <contributor xtf:meta="true">
               <xsl:value-of select="'unknown'"/>
            </contributor>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- date -->
   <xsl:template name="get-book-date">
      <xsl:choose>
         <xsl:when test="$metsMeta/date">
            <date xtf:meta="true">
               <xsl:value-of select="string($metsMeta/date[1])"/>
            </date>
         </xsl:when>
         <xsl:otherwise>
            <date xtf:meta="true">
               <xsl:value-of select="'unknown'"/>
            </date>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- type -->
   <xsl:template name="get-book-type">
      <type xtf:meta="true">ebook</type>
   </xsl:template>
   
   <!-- format -->
   <xsl:template name="get-book-format">
      <format xtf:meta="true">xml</format>
   </xsl:template>
   
   <!-- identifier -->
   <xsl:template name="get-book-identifier">
      <xsl:choose>
         <xsl:when test="$metsMeta/identifier">
            <identifier xtf:meta="true" xtf:tokenize="no">
               <xsl:value-of select="string($metsMeta/identifier[1])"/>
            </identifier>
         </xsl:when>
         <xsl:otherwise>
            <identifier xtf:meta="true" xtf:tokenize="no">
               <xsl:value-of select="'unknown'"/>
            </identifier>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- language -->
   <xsl:template name="get-book-language">
      <xsl:choose>
         <xsl:when test="$metsMeta/language">
            <language xtf:meta="true">
               <xsl:value-of select="string($metsMeta/language[1])"/>
            </language>
         </xsl:when>
         <xsl:otherwise>
            <language xtf:meta="true">
               <xsl:value-of select="'unknown'"/>
            </language>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <!-- relation -->
   <xsl:template name="get-book-relation">
      <xsl:choose>
         <xsl:when test="$metsMeta/identifier-access">
            <relation xtf:meta="true">
               <xsl:value-of select="string($metsMeta/identifier-access)"/>
            </relation>
         </xsl:when>
         <xsl:otherwise>
            <relation xtf:meta="true">
               <xsl:value-of select="'unknown'"/>
            </relation>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- coverage -->
   <xsl:template name="get-book-coverage">
      <coverage xtf:meta="true">unknown</coverage>
   </xsl:template>
   
   <!-- rights -->
   <xsl:template name="get-book-rights">
      <xsl:choose>
         <xsl:when test="$metsMeta/copyright-status[string() = 'NOT_IN_COPYRIGHT'] or
                         $metsMeta/possible-copyright-status[string() = 'NOT_IN_COPYRIGHT']">
            <rights xtf:meta="true">
               <xsl:value-of select="'public'"/>
            </rights>
         </xsl:when>
         <xsl:otherwise>
            <rights xtf:meta="true">
               <xsl:value-of select="'unknown'"/>
            </rights>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- OAI dateStamp -->
   <xsl:template name="oai-datestamp">
      <dateStamp xtf:meta="true" xtf:tokenize="no">
         <xsl:choose>
            <xsl:when test="$metsMeta/date">
               <xsl:value-of select="concat(parse:year(string($metsMeta/date[1]))[1],'-01-01')"/>
            </xsl:when>
            <xsl:otherwise>
               <!-- I don't know, what would you put? -->
               <xsl:value-of select="'1950-01-01'"/>
            </xsl:otherwise>
         </xsl:choose>
      </dateStamp>
   </xsl:template>
   
   <!-- OAI sets -->
   <xsl:template name="oai-set">
      <xsl:for-each select="$metsMeta/subject">
         <set xtf:meta="true">
            <xsl:value-of select="."/>
         </set>
      </xsl:for-each>
      <xsl:if test="$metsMeta/copyright-status[string() = 'NOT_IN_COPYRIGHT'] or
                    $metsMeta/possible-copyright-status[string() = 'NOT_IN_COPYRIGHT']">
         <set xtf:meta="true">
            <xsl:value-of select="'public'"/>
         </set>
      </xsl:if>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Page processing                                                        -->
   <!-- ====================================================================== -->
   
   <xsl:template name="makeLeafToPage">
      <xsl:param name="prevLeafNum"/>
      <xsl:param name="isAsserted"/>
      <xsl:param name="prevPageNum"/>
      <xsl:param name="assertions"/>
      
      <!-- Standard recursive gyrations for XSLT -->
      <xsl:variable name="firstAssertion" select="$assertions[1]"/>
      <xsl:variable name="otherAssertions" select="$assertions[position() &gt; 1]"/>
      
      <!-- Grab the asserted page to leaf correspondence -->
      <xsl:variable name="assertedPageNum" select="$firstAssertion/@pageNum cast as xs:integer"/>
      <xsl:variable name="assertedLeafNum" select="$firstAssertion/@leafNum cast as xs:integer"/>
      
      <!-- Handle the part of the sequence before the first correspondence, if any. -->
      <xsl:variable name="pageNum" select="if ($isAsserted) then $assertedPageNum else 1"/>
      <xsl:variable name="leafNum" select="if ($isAsserted) then $assertedLeafNum else $assertedLeafNum - $assertedPageNum + 1"/>
      
      <xsl:for-each select="$prevLeafNum to $leafNum - 1">
         <xsl:variable name="outPage" select=". - $prevLeafNum + $prevPageNum"/>
         <xsl:if test="$outPage &lt; $pageNum">
            <mapping leafNum="{.}" pageNum="{$outPage}"/>
         </xsl:if>
      </xsl:for-each>

      <xsl:choose>
         <xsl:when test="count($otherAssertions)">
            <!-- Now recursively process the remaining assertions -->
            <xsl:call-template name="makeLeafToPage">
               <xsl:with-param name="prevLeafNum" select="$leafNum"/>
               <xsl:with-param name="prevPageNum" select="$pageNum"/>
               <xsl:with-param name="isAsserted" select="true()"/>
               <xsl:with-param name="assertions" select="$otherAssertions"/>
            </xsl:call-template>
         </xsl:when>
         <xsl:otherwise>
            <!-- Handle last asserted leaf. -->
            <mapping leafNum="{$leafNum}" pageNum="{$pageNum}"/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- Convert all the pages in the book to our compact, single-file format. -->
   <xsl:template name="convertPages">
      
      <!--<xsl:message>
         leafToPage: 
      </xsl:message>
      <xsl:for-each select="$leafToPage/*">
         <xsl:message><xsl:copy-of select="."/></xsl:message>
         </xsl:for-each>-->
      
      <xsl:for-each select="//*:pageData/*:page">
         <xsl:call-template name="processPage"/>
      </xsl:for-each>
   </xsl:template>
   
   <!-- Convert a single leaf -->
   <xsl:template name="processPage">
      <xsl:variable name="leafNum" select="number(@leafNum)"/>
      <leaf leafNum="{$leafNum}" 
            type="{*:pageType}"
            access="{*:addToAccessFormats}">
         
         <!-- Associate a logical page number, if any. -->
         <xsl:if test="$leafToPage/mapping[@leafNum = $leafNum]/@pageNum">
            <xsl:attribute name="pageNum" select="$leafToPage/mapping[@leafNum = $leafNum]/@pageNum"/>
         </xsl:if>
         
         <!-- Jump through hoops to find the image file -->
         <xsl:variable name="imageFileLoc" select="concat(format-number(@leafNum, '00000000'), '.jpg')"/>
         <xsl:variable name="docpath" select="saxon:system-id()"/>
         <xsl:variable name="base" select="replace($docpath, '(.*)/[^/]+$', '$1')"/>
         <xsl:variable name="imagePath" select="concat($base, '/', $imageFileLoc)"/>
         <xsl:if test="FileUtils:exists($imagePath)">
            <xsl:attribute name="imgFile" select="$imageFileLoc"/>
         </xsl:if>
         
         <!-- Copy the crop box dimensions -->
         <xsl:if test="*:cropBox">
            <cropBox 
               x="{replace(*:cropBox/*:x, '\.0$', '')}"
               y="{replace(*:cropBox/*:y, '\.0$', '')}"
               w="{replace(*:cropBox/*:w, '\.0$', '')}"
               h="{replace(*:cropBox/*:h, '\.0$', '')}"/>
         </xsl:if>
         
         <!-- Now process the DJVU XML -->
         <xsl:variable name="xmlFileLoc" select="concat(format-number(@leafNum, '00000000'), '.xml')"/>
         <xsl:variable name="xmlPath" select="concat($base, '/', $xmlFileLoc)"/>
         <xsl:if test="FileUtils:exists($xmlPath)">
            <xsl:variable name="xmlDoc" select="document($xmlPath)"/>
            <xsl:choose>
               <xsl:when test="$xmlDoc/DjVuXML">
                  <xsl:for-each select="$xmlDoc//LINE">
                     <xsl:call-template name="processLine"/>
                  </xsl:for-each>
               </xsl:when>
               <xsl:otherwise>
                  <noDjVuFound/>
               </xsl:otherwise>
            </xsl:choose>            
         </xsl:if>
      </leaf>
   </xsl:template>
   
   <!-- Convert one line on the leaf. -->
   <xsl:template name="processLine">
      <xsl:variable name="words">
         <xsl:for-each select="WORD">
            <xsl:variable name="coords" select="tokenize(@coords, ',')"/>
            <xsl:variable name="nums" select="$coords[1] cast as xs:integer,
                                              $coords[2] cast as xs:integer,
                                              $coords[3] cast as xs:integer,
                                              $coords[4] cast as xs:integer"/>
            <xsl:choose>
               <!-- Not sure why the top and bottom are in a weird order, but we adjust -->
               <xsl:when test="$nums[2] &lt; $nums[4]">
                  <word l="{$nums[1]}" t="{$nums[2]}" r="{$nums[3]}" b="{$nums[4]}"/>
               </xsl:when>
               <xsl:otherwise>
                  <word l="{$nums[1]}" t="{$nums[4]}" r="{$nums[3]}" b="{$nums[2]}"/>
               </xsl:otherwise>
            </xsl:choose>
         </xsl:for-each>
      </xsl:variable>
      <line l="{min($words/word/@l)}"
            t="{min($words/word/@t)}"
            r="{max($words/word/@r)}"
            b="{max($words/word/@b)}">
         <xsl:attribute name="spacing">
            <xsl:for-each select="$words/word">
               <xsl:variable name="pos" select="position()"/>
               <xsl:variable name="prev" select="preceding-sibling::*[1]"/>
               <xsl:if test="$prev">
                  <xsl:value-of select="concat(' ', @l - $prev/@r, ' ')"/>
               </xsl:if>
               <xsl:value-of select="@r - @l"/>
            </xsl:for-each>
         </xsl:attribute>
         <xsl:for-each select="WORD">
            <xsl:if test="position() &gt; 1">
               <xsl:text> </xsl:text>
            </xsl:if>
            <xsl:value-of select="replace(string(.), ' ', '_')"/>
         </xsl:for-each>
      </line>
   </xsl:template>
   
</xsl:stylesheet>
