<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
   xmlns:dc="http://purl.org/dc/elements/1.1/"
   xmlns:expand="http://cdlib.org/xtf/expand"
   xmlns:parse="http://cdlib.org/xtf/parse"
   xmlns:xtf="http://cdlib.org/xtf"
   xmlns:saxon="http://saxon.sf.net/"
   xmlns:FileUtils="java:org.cdlib.xtf.xslt.FileUtils"
   extension-element-prefixes="saxon FileUtils"
   exclude-result-prefixes="#all">
   
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
   <!-- Templates                                                              -->
   <!-- ====================================================================== -->
   
   <!-- Fetch meta-data from a Dublin Core record, if present -->
   <xsl:template name="get-dc-meta">
      <xsl:variable name="docpath" select="saxon:system-id()"/>
      <xsl:variable name="base" select="replace($docpath, '(.*)\.[^\.]+$', '$1')"/>
      <xsl:variable name="dcpath" select="concat($base, '.dc.xml')"/>
      <xsl:if test="FileUtils:exists($dcpath)">
         <xsl:apply-templates select="document($dcpath)" mode="inmeta"/>
         <xsl:if test="not(document($dcpath)//*[matches(local-name(),'identifier')])">
            <identifier xtf:meta="true" xtf:tokenize="no">
               <xsl:value-of select="replace(replace($docpath,'^.+/',''),'\.[A-Za-z]+$','')"/>
            </identifier>
         </xsl:if>
         <!-- special field for OAI -->
         <set xtf:meta="true">
            <xsl:value-of select="'public'"/>
         </set>
      </xsl:if>
   </xsl:template>
   
   <!-- Process DC -->
   <xsl:template match="*" mode="inmeta">
      
      <!-- Copy all metadata fields -->
      <xsl:for-each select="*">
         <xsl:choose>
            <xsl:when test="matches(name(),'identifier')">
               <identifier xtf:meta="true" xtf:tokenize="no">
                  <xsl:copy-of select="@*"/>
                  <xsl:value-of select="replace(replace(string(),'^.+/',''),'\.[A-Za-z]+$','')"/>
               </identifier>
            </xsl:when>
            <xsl:otherwise>
               <xsl:element name="{name()}">
                  <xsl:attribute name="xtf:meta" select="'true'"/>
                  <xsl:copy-of select="@*"/>
                  <xsl:value-of select="string()"/>
               </xsl:element>
            </xsl:otherwise>
         </xsl:choose>
         <!-- special fields for OAI -->
         <xsl:choose>
            <xsl:when test="matches(name(),'date')">
               <dateStamp xtf:meta="true" xtf:tokenize="no">
                  <xsl:value-of select="concat(parse:year(string(.)),'-01-01')"/>
               </dateStamp>
            </xsl:when>
            <xsl:when test="matches(name(),'subject')">
               <set xtf:meta="true">
                  <xsl:value-of select="string()"/>
               </set>
            </xsl:when>
         </xsl:choose>
      </xsl:for-each>
      
   </xsl:template>
   
   <!-- Add sort fields to DC meta-data -->
   <xsl:template name="add-fields">
      <xsl:param name="meta"/>
      <xsl:param name="display"/>
      
      <xtf:meta>
         <!-- Copy all the original fields -->
         <xsl:copy-of select="$meta/*"/>
         
         <!-- Add a field to record the document kind -->
         <display xtf:meta="true" xtf:tokenize="no">
            <xsl:value-of select="$display"/>
         </display>
         
         <all xtf:meta="true">all</all>
         
         <!-- Parse the date field to create a year (or range of years) -->
         <xsl:apply-templates select="$meta/*[matches(local-name(),'^date$')]" mode="year"/>
         
         <!-- Create sort fields -->
         <xsl:apply-templates select="$meta/*[matches(local-name(),'^title$')][1]" mode="sort"/>    
         <xsl:apply-templates select="$meta/*[matches(local-name(),'^creator$')][1]" mode="sort"/>
         <xsl:apply-templates select="$meta/*[matches(local-name(),'^date$')][1]" mode="sort"/>
         
         <!-- Create facets -->
         <xsl:apply-templates select="$meta/*[matches(local-name(),'^date$')]" mode="facet"/>
         <xsl:apply-templates select="$meta/*[matches(local-name(),'^subject$')]" mode="facet"/>
         
         <xsl:apply-templates select="$meta/*[matches(local-name(),'^title$')][1]" mode="browse"/>    
         <xsl:apply-templates select="$meta/*[matches(local-name(),'^creator$')][1]" mode="browse"/>
         
      </xtf:meta>
   </xsl:template>
   
   <!-- Parse the date to determine the year (or range of years) -->
   <xsl:template match="*[matches(local-name(),'^date$')]" mode="year">
      <year xtf:meta="yes">
         <xsl:copy-of select="parse:year(string(.))"/>
      </year>
   </xsl:template>
   
   <!-- Generate sort-title -->
   <xsl:template match="*[matches(local-name(),'^title$')]" mode="sort">
      <sort-title xtf:meta="yes" xtf:tokenize="no">
         <xsl:value-of select="parse:title(string(.))"/>
      </sort-title>
   </xsl:template>
   
   <!-- Generate sort-creator -->
   <xsl:template match="*[matches(local-name(),'^creator$')]" mode="sort">
      <sort-creator xtf:meta="yes" xtf:tokenize="no">
         <xsl:copy-of select="parse:name(string(.))"/>
      </sort-creator>
   </xsl:template>
   
   <!-- Generate sort-year (if range, only use first year) -->
   <xsl:template match="*[matches(local-name(),'^date$')]" mode="sort">
      <sort-year xtf:meta="true" xtf:tokenize="no">
         <xsl:value-of select="parse:year(string(.))[1]"/>
      </sort-year>
   </xsl:template>
   
   <!-- Generate facet-date -->
   <xsl:template match="*[matches(local-name(),'^date$')]" mode="facet">
      <facet-date>
         <xsl:attribute name="xtf:meta" select="'true'"/>
         <xsl:attribute name="xtf:tokenize" select="'no'"/>
         <xsl:choose>
            <xsl:when test="matches(.,'[0-9]{2}-[0-9]{2}-[0-9]{4}')">
               <xsl:value-of select="replace(.,'([0-9]{2})-([0-9]{2})-([0-9]{4})','$3::$1::$2')"/>
            </xsl:when>
            <xsl:when test="matches(.,'[0-9]{4}-[0-9]{2}-[0-9]{2}')">
               <xsl:value-of select="replace(.,'-','::')"/>
            </xsl:when>
            <xsl:otherwise>
               <xsl:value-of select="concat(parse:year(string(.)),'::01::01')"/>
            </xsl:otherwise>
         </xsl:choose>
      </facet-date>
   </xsl:template>
   
   <!-- Generate facet-subject -->
   <xsl:template match="*[matches(local-name(),'^subject$')]" mode="facet">
      <facet-subject>
         <xsl:attribute name="xtf:meta" select="'true'"/>
         <xsl:attribute name="xtf:tokenize" select="'no'"/>
         <xsl:value-of select="string(.)"/>
      </facet-subject>
   </xsl:template>
   
   <!-- Generate browse-title -->
   <xsl:template match="*[matches(local-name(),'^title$')]" mode="browse">
      <browse-title>
         <xsl:attribute name="xtf:meta" select="'true'"/>
         <xsl:attribute name="xtf:tokenize" select="'no'"/>
         <xsl:value-of select="parse:alpha(parse:title(.))"/>
      </browse-title>
   </xsl:template>
   
   <!-- Generate browse-creator -->
   <xsl:template match="*[matches(local-name(),'^creator$')]" mode="browse">
      <browse-creator>
         <xsl:attribute name="xtf:meta" select="'true'"/>
         <xsl:attribute name="xtf:tokenize" select="'no'"/>
         <xsl:value-of select="parse:alpha(parse:name(.))"/>
      </browse-creator>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Functions                                                              -->
   <!-- ====================================================================== -->  
   
   <!-- Function to parse normalized titles out of titles -->  
   <xsl:function name="parse:title">
      
      <xsl:param name="title"/>
      
      <!-- Normalize Spaces & Case-->
      <xsl:variable name="lower-title">
         <xsl:value-of select="translate(normalize-space($title), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')"/>
      </xsl:variable>
      
      <!-- Remove Punctuation -->
      <xsl:variable name="parse-title">
         <xsl:value-of select="replace($lower-title, '[^a-z0-9 ]', '')"/>
      </xsl:variable>
      
      <!-- Remove Leading Articles -->
      <xsl:choose>
         <xsl:when test="matches($parse-title, '^a ')">
            <xsl:value-of select="replace($parse-title, '^a (.+)', '$1')"/>
         </xsl:when>
         <xsl:when test="matches($parse-title, '^an ')">
            <xsl:value-of select="replace($parse-title, '^an (.+)', '$1')"/>
         </xsl:when>
         <xsl:when test="matches($parse-title, '^the ')">
            <xsl:value-of select="replace($parse-title, '^the (.+)', '$1')"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:value-of select="$parse-title"/>
         </xsl:otherwise>
      </xsl:choose>
      
   </xsl:function>  
   
   <!-- Function to parse last names out of various name formats -->  
   <xsl:function name="parse:name">
      
      <xsl:param name="creator"/>
      
      <!-- Normalize Spaces & Case-->
      <xsl:variable name="lower-name">
         <xsl:value-of select="translate(normalize-space($creator), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')"/>
      </xsl:variable>
      
      <!-- Remove additional authors and information -->
      <xsl:variable name="first-creator">
         <xsl:choose>
            <!-- Pattern:  NAME and NAME -->
            <xsl:when test="matches($lower-name, '[^,]+ and.+')">
               <xsl:value-of select="replace($lower-name, '(.+?) and.+', '$1')"/>
            </xsl:when>
            <!-- Pattern:  NAME, NAME and NAME -->
            <xsl:when test="matches($lower-name, ', .+ and')">
               <xsl:value-of select="replace($lower-name, '(.+?), .+', '$1')"/>
            </xsl:when>
            <!-- Pattern:  NAME -->
            <xsl:otherwise>
               <xsl:value-of select="$lower-name"/>
            </xsl:otherwise>
         </xsl:choose>
      </xsl:variable>
      
      <xsl:choose>
         <!-- Pattern:  NAME, NAME -->
         <xsl:when test="matches($first-creator, ', ')">
            <xsl:value-of select="replace($first-creator, '(.+?), .+', '$1')"/>
         </xsl:when>
         <!-- Pattern:  'X. NAME' or ' NAME' -->
         <xsl:when test="matches($first-creator, '^.+\.? (\w{2,100})')">
            <xsl:value-of select="replace($first-creator, '^.+\.? (\w{2,100})', '$1')"/>
         </xsl:when>
         <!-- Pattern:  Everything else -->
         <xsl:otherwise>
            <xsl:value-of select="$first-creator"/>
         </xsl:otherwise>
      </xsl:choose>
      
   </xsl:function>
   
   <!-- Function to parse years out of various date formats -->  
   <xsl:function name="parse:year">
      <xsl:param name="date"/>
      
      <xsl:choose>
         
         <!-- Pattern: 1989-12-1 -->
         <xsl:when test="matches($date, '([^0-9]|^)(\d\d\d\d)[^0-9]*[\-/][^0-9]*\d\d?[^0-9]*[\-/][^0-9]*\d\d?([^0-9]|$)')">
            <xsl:analyze-string select="$date" regex="([^0-9]|^)(\d\d\d\d)[^0-9]*[\-/][^0-9]*\d\d?[^0-9]*[\-/][^0-9]*\d\d?([^0-9]|$)">
               <xsl:matching-substring>
                  <xsl:copy-of select="number(regex-group(2))"/>
               </xsl:matching-substring>
            </xsl:analyze-string>
         </xsl:when>
         
         <!-- Pattern: 1980 - 1984 -->
         <xsl:when test="matches($date, '([^0-9]|^)([12]\d\d\d)[^0-9]*-[^0-9]*([12]\d\d\d)([^0-9]|$)')">
            <xsl:analyze-string select="$date" regex="([^0-9]|^)([12]\d\d\d)[^0-9]*-[^0-9]*([12]\d\d\d)([^0-9]|$)">
               <xsl:matching-substring>
                  <xsl:copy-of select="parse:output-range(regex-group(2), regex-group(3))"/>
               </xsl:matching-substring>
            </xsl:analyze-string>
         </xsl:when>
         
         <!-- Pattern: 1980 - 84 -->
         <xsl:when test="matches($date, '([^0-9]|^)([12]\d\d\d)[^0-9]*-[^0-9]*(\d\d)([^0-9]|$)')">
            <xsl:analyze-string select="$date" regex="([^0-9]|^)([12]\d\d\d)[^0-9]*-[^0-9]*(\d\d)([^0-9]|$)">
               <xsl:matching-substring>
                  <xsl:variable name="year1" select="number(regex-group(2))"/>
                  <xsl:variable name="century" select="floor($year1 div 100) * 100"/>
                  <xsl:variable name="pyear2" select="number(regex-group(3))"/>
                  <xsl:variable name="year2" select="$pyear2 + $century"/>
                  <xsl:copy-of select="parse:output-range($year1, $year2)"/>
               </xsl:matching-substring>
            </xsl:analyze-string>
         </xsl:when>
         
         <!-- Pattern: 1-12-89 -->
         <xsl:when test="matches($date, '([^0-9]|^)\d\d?[^0-9]*[\-/][^0-9]*\d\d?[^0-9]*[\-/][^0-9]*(\d\d)([^0-9]|$)')">
            <xsl:analyze-string select="$date" regex="([^0-9]|^)\d\d?[^0-9]*[\-/][^0-9]*\d\d?[^0-9]*[\-/][^0-9]*(\d\d)([^0-9]|$)">
               <xsl:matching-substring>
                  <xsl:copy-of select="number(regex-group(2)) + 1900"/>
               </xsl:matching-substring>
            </xsl:analyze-string>
         </xsl:when>
         
         <!-- Pattern: 19890112 -->
         <xsl:when test="matches($date, '([^0-9]|^)([12]\d\d\d)[01]\d[0123]\d')">
            <xsl:analyze-string select="$date" regex="([^0-9]|^)([12]\d\d\d)[01]\d[0123]\d">
               <xsl:matching-substring>
                  <xsl:copy-of select="number(regex-group(2))"/>
               </xsl:matching-substring>
            </xsl:analyze-string>
         </xsl:when>
         
         <!-- Pattern: 890112 -->
         <xsl:when test="matches($date, '([^0-9]|^)([4-9]\d)[01]\d[0123]\d')">
            <xsl:analyze-string select="$date" regex="([^0-9]|^)(\d\d)[01]\d[0123]\d">
               <xsl:matching-substring>
                  <xsl:copy-of select="number(regex-group(2)) + 1900"/>
               </xsl:matching-substring>
            </xsl:analyze-string>
         </xsl:when>
         
         <!-- Pattern: 011291 -->
         <xsl:when test="matches($date, '([^0-9]|^)[01]\d[0123]\d(\d\d)')">
            <xsl:analyze-string select="$date" regex="([^0-9]|^)[01]\d[0123]\d(\d\d)">
               <xsl:matching-substring>
                  <xsl:copy-of select="number(regex-group(2)) + 1900"/>
               </xsl:matching-substring>
            </xsl:analyze-string>
         </xsl:when>
         
         <!-- Pattern: 1980 -->
         <xsl:when test="matches($date, '([^0-9]|^)([12]\d\d\d)([^0-9]|$)')">
            <xsl:analyze-string select="$date" regex="([^0-9]|^)([12]\d\d\d)([^0-9]|$)">
               <xsl:matching-substring>
                  <xsl:copy-of select="regex-group(2)"/>            
               </xsl:matching-substring>
            </xsl:analyze-string>
         </xsl:when>
         
         <!-- Pattern: any 4 digits starting with 1 or 2 -->
         <xsl:when test="matches($date, '([12]\d\d\d)')">
            <xsl:analyze-string select="$date" regex="([12]\d\d\d)">
               <xsl:matching-substring>
                  <xsl:copy-of select="regex-group(1)"/>            
               </xsl:matching-substring>
            </xsl:analyze-string>
         </xsl:when>
         
      </xsl:choose>
      
   </xsl:function>
   
   <!-- Function to parse year ranges -->
   <xsl:function name="parse:output-range">
      <xsl:param name="year1-in"/>
      <xsl:param name="year2-in"/>
      
      <xsl:variable name="year1" select="number($year1-in)"/>
      <xsl:variable name="year2" select="number($year2-in)"/>
      
      <xsl:choose>
         
         <xsl:when test="$year2 > $year1 and ($year2 - $year1) &lt; 500">
            <xsl:for-each select="(1 to 500)">
               <xsl:if test="$year1 + position() - 1 &lt;= $year2">
                  <xsl:value-of select="$year1 + position() - 1"/>
                  <xsl:value-of select="' '"/>
               </xsl:if>
            </xsl:for-each>
         </xsl:when>
         
         <xsl:otherwise>
            <xsl:value-of select="$year1"/>
            <xsl:value-of select="' '"/>
            <xsl:value-of select="$year2"/>
         </xsl:otherwise>
         
      </xsl:choose>
      
   </xsl:function>
   
   <!-- function to expand date strings -->
   <xsl:function name="expand:date">
      <xsl:param name="date"/>
      
      <xsl:variable name="year" select="replace($date, '[0-9]+/[0-9]+/([0-9]+)', '$1')"/>
      
      <xsl:variable name="month">
         <xsl:choose>
            <xsl:when test="matches($date,'^[0-9]/[0-9]+/[0-9]+')">
               <xsl:value-of select="0"/>
               <xsl:value-of select="replace($date, '^([0-9])/[0-9]+/[0-9]+', '$1')"/>
            </xsl:when>
            <xsl:otherwise>
               <xsl:value-of select="replace($date, '([0-9]+)/[0-9]+/[0-9]+', '$1')"/>
            </xsl:otherwise>
         </xsl:choose>
      </xsl:variable>  
      
      <xsl:variable name="day">
         <xsl:choose>
            <xsl:when test="matches($date,'[0-9]+/[0-9]/[0-9]+')">
               <xsl:value-of select="0"/>
               <xsl:value-of select="replace($date, '[0-9]+/([0-9])/[0-9]+', '$1')"/>
            </xsl:when>
            <xsl:otherwise>
               <xsl:value-of select="replace($date, '[0-9]+/([0-9]+)/[0-9]+', '$1')"/>
            </xsl:otherwise>
         </xsl:choose>
      </xsl:variable>
      
      <xsl:value-of select="concat($year, '::', $month, '::', $day)"/>
      
   </xsl:function>
   
   <!-- function to create alpha browse facets -->
   <xsl:function name="parse:alpha">
      
      <xsl:param name="string"/>
      
      <xsl:variable name="cleanString" select="lower-case(string($string))"/>
      
      <xsl:choose>
         <xsl:when test="matches($cleanString,'^\W*[a-z]')">
            <xsl:value-of select="replace($cleanString,'^\W*([a-z]).*','$1$1')"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:value-of select="'other'"/>
         </xsl:otherwise>
      </xsl:choose>
      
   </xsl:function>
</xsl:stylesheet>
