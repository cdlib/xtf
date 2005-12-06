<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
        xmlns:saxon="http://saxon.sf.net/"
        xmlns:xtf="http://cdlib.org/xtf"
        xmlns:date="http://exslt.org/dates-and-times"
        xmlns:parse="http://cdlib.org/xtf/parse"
        xmlns:expand="http://cdlib.org/xtf/expand"
        extension-element-prefixes="date"
        exclude-result-prefixes="#all">

<!--
   Copyright (c) 2005, Regents of the University of California
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
  
  <!-- Year and sort-year templates used by date functions -->
  
  <xsl:template name="year">
    
    <xsl:param name="year"/>
    <xsl:variable name="string-year" select="string($year)"/>
    
    <year xtf:meta="true">
      <xsl:value-of select="$year"/>
    </year>
    
  </xsl:template>
  
  <xsl:template name="sort-year">
    
    <xsl:param name="year"/>
    <xsl:variable name="string-year" select="string($year)"/>
    
    <sort-year xtf:meta="true" xtf:tokenize="no">
      <xsl:value-of select="replace($string-year, '.*([0-9]{4}).*', '$1')"/>
    </sort-year>
    
  </xsl:template>

  <!-- generate facet fields -->
  <xsl:template match="*" mode="facet"> 
    <xsl:variable name="name">
      <xsl:choose>
        <!-- To disambiguate types -->
        <xsl:when test="name() = 'type'">
          <xsl:value-of select="concat('facet-',name(),'-',position())"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="concat('facet-',name())"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <!-- Get rid of quotes -->
    <xsl:variable name="value" select="replace(string(.), '&quot;', '')"/>
    <xsl:element name="{$name}">
      <xsl:attribute name="xtf:meta" select="'true'"/>
      <xsl:attribute name="xtf:tokenize" select="'no'"/>
      <xsl:choose>
        <xsl:when test="normalize-space() = ''">
          <xsl:value-of select="'1 EMPTY'"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$value"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:element>
  </xsl:template>

  <!-- generate facet-title -->
  <xsl:template match="title" mode="facet"> 
    <xsl:variable name="title" select="string(.)"/>
    <facet-title>
      <xsl:attribute name="xtf:meta" select="'true'"/>
      <xsl:attribute name="xtf:tokenize" select="'no'"/>
      <xsl:choose>
        <!-- for numeric titles -->
        <xsl:when test="matches(parse:title($title), '^[0-9]')">
          <xsl:value-of select="'0-9'"/>
        </xsl:when>
        <xsl:when test="matches(parse:title($title), '^[A-Ca-c]')">
          <xsl:value-of select="'A-C'"/>
        </xsl:when>
        <xsl:when test="matches(parse:title($title), '^[D-Fd-f]')">
          <xsl:value-of select="'D-F'"/>
        </xsl:when>
        <xsl:when test="matches(parse:title($title), '^[G-Ig-i]')">
          <xsl:value-of select="'G-I'"/>
        </xsl:when>
        <xsl:when test="matches(parse:title($title), '^[J-Lj-l]')">
          <xsl:value-of select="'J-L'"/>
        </xsl:when>
        <xsl:when test="matches(parse:title($title), '^[M-Om-o]')">
          <xsl:value-of select="'M-O'"/>
        </xsl:when>
        <xsl:when test="matches(parse:title($title), '^[P-Rp-r]')">
          <xsl:value-of select="'P-R'"/>
        </xsl:when>
        <xsl:when test="matches(parse:title($title), '^[S-Vs-v]')">
          <xsl:value-of select="'S-V'"/>
        </xsl:when>
        <xsl:when test="matches(parse:title($title), '^[W-Zw-z\w]')">
          <xsl:value-of select="'W-Z'"/>
        </xsl:when>
        <xsl:otherwise>
          <!-- to catch unusual titles -->
          <xsl:value-of select="'OTHER'"/>
        </xsl:otherwise>
      </xsl:choose>
    </facet-title>
  </xsl:template>
  
  <!-- generate facet-creator -->
  <xsl:template match="creator" mode="facet"> 
    <xsl:variable name="creator" select="string(.)"/>
    <facet-creator>
      <xsl:attribute name="xtf:meta" select="'true'"/>
      <xsl:attribute name="xtf:tokenize" select="'no'"/>
      <xsl:choose>
        <xsl:when test="matches(parse:name($creator), '^[A-Ca-c]')">
          <xsl:value-of select="'A-C'"/>
        </xsl:when>
        <xsl:when test="matches(parse:name($creator), '^[D-Fd-f]')">
          <xsl:value-of select="'D-F'"/>
        </xsl:when>
        <xsl:when test="matches(parse:name($creator), '^[G-Ig-i]')">
          <xsl:value-of select="'G-I'"/>
        </xsl:when>
        <xsl:when test="matches(parse:name($creator), '^[J-Lj-l]')">
          <xsl:value-of select="'J-L'"/>
        </xsl:when>
        <xsl:when test="matches(parse:name($creator), '^[M-Om-o]')">
          <xsl:value-of select="'M-O'"/>
        </xsl:when>
        <xsl:when test="matches(parse:name($creator), '^[P-Rp-r]')">
          <xsl:value-of select="'P-R'"/>
        </xsl:when>
        <xsl:when test="matches(parse:name($creator), '^[S-Vs-v]')">
          <xsl:value-of select="'S-V'"/>
        </xsl:when>
        <!-- also includes all diacritics, which seem to be sorted to the end by XTF -->
        <xsl:when test="matches(parse:name($creator), '^[W-Zw-z\w]')">
          <xsl:value-of select="'W-Z'"/>
        </xsl:when>
        <xsl:otherwise>
          <!-- to catch unusal creators -->
          <xsl:value-of select="'OTHER'"/>
        </xsl:otherwise>
      </xsl:choose>
    </facet-creator>
  </xsl:template>
  
  <!-- generate facet-date -->
  <xsl:template match="date" mode="facet">   
    <xsl:variable name="date" select="string(.)"/>
    <facet-date>
      <xsl:attribute name="xtf:meta" select="'true'"/>
      <xsl:attribute name="xtf:tokenize" select="'no'"/>
      <xsl:value-of select="expand:date($date)"/>
    </facet-date>
  </xsl:template>
  
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
  
  <!-- missing elements? -->
  <xsl:template name="metaMissing">
    <xsl:if test="not(title)">
      <facet-title xtf:meta="true" xtf:tokenize="no">1 MISSING</facet-title>
      <title xtf:meta="true">1 MISSING TITLE</title>
    </xsl:if>
    <xsl:if test="not(creator)">
      <facet-creator xtf:meta="true" xtf:tokenize="no">1 MISSING</facet-creator>
    </xsl:if>
    <xsl:if test="not(subject)">
      <facet-subject xtf:meta="true" xtf:tokenize="no">1 MISSING</facet-subject>
    </xsl:if>
    <xsl:if test="not(description)">
      <facet-description xtf:meta="true" xtf:tokenize="no">1 MISSING</facet-description>
    </xsl:if>
    <xsl:if test="not(publisher)">
      <facet-publisher xtf:meta="true" xtf:tokenize="no">1 MISSING</facet-publisher>
    </xsl:if>
    <xsl:if test="not(contributor)">
      <facet-contributor xtf:meta="true" xtf:tokenize="no">1 MISSING</facet-contributor>
    </xsl:if>
    <xsl:if test="not(date)">
      <facet-date xtf:meta="true" xtf:tokenize="no">1 MISSING</facet-date>
    </xsl:if>
    <xsl:if test="not(type)">
      <facet-type xtf:meta="true" xtf:tokenize="no">1 MISSING</facet-type>
    </xsl:if>
    <xsl:if test="not(format)">
      <facet-format xtf:meta="true" xtf:tokenize="no">1 MISSING</facet-format>
    </xsl:if>
    <xsl:if test="not(identifier)">
      <facet-identifier xtf:meta="true" xtf:tokenize="no">1 MISSING</facet-identifier>
    </xsl:if>
    <xsl:if test="not(source)">
      <facet-source xtf:meta="true" xtf:tokenize="no">1 MISSING</facet-source>
    </xsl:if>
    <xsl:if test="not(language)">
      <facet-language xtf:meta="true" xtf:tokenize="no">1 MISSING</facet-language>
    </xsl:if>
    <xsl:if test="not(relation)">
      <facet-relation xtf:meta="true" xtf:tokenize="no">1 MISSING</facet-relation>
    </xsl:if>
    <xsl:if test="not(coverage)">
      <facet-coverage xtf:meta="true" xtf:tokenize="no">1 MISSING</facet-coverage>
    </xsl:if>
    <xsl:if test="not(rights)">
      <facet-rights xtf:meta="true" xtf:tokenize="no">1 MISSING</facet-rights>
    </xsl:if>
  </xsl:template>    

<!-- ====================================================================== -->
<!-- Functions                                                              -->
<!-- ====================================================================== -->
  
  <!-- Function to parse normalized titles out of dc:title -->  
  <xsl:function name="parse:title">
    
    <xsl:param name="title"/>
    
    <!-- Normalize Case -->
    <xsl:variable name="lower-title">
      <xsl:value-of select="translate($title, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')"/>
    </xsl:variable>
    
    <!-- Remove Punctuation -->
    <xsl:variable name="parse-title">
      <xsl:value-of select="replace($lower-title, '[^a-z0-9 ]', '')"/>
    </xsl:variable>
    
    <!-- Remove Leading Articles -->
    <!-- KVH: Eventually this should handle French, German, and Spanish articles as well -->
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
  
  <!-- Function to parse last names out of various dc:creator formats -->  
  
  <xsl:function name="parse:name">
    
    <xsl:param name="creator"/>
    
    <!-- Remove additional authors and information -->
    <xsl:variable name="parse-name">
      <xsl:choose>
        <!-- Pattern:  NAME and NAME -->
        <xsl:when test="matches($creator, '[^,]+ and.+,')">
          <xsl:value-of select="replace($creator, '(.+?) and.+', '$1')"/>
        </xsl:when>
        <!-- Pattern:  NAME, NAME and NAME -->
        <xsl:when test="matches($creator, ', .+ and')">
          <xsl:value-of select="replace($creator, '(.+?), .+', '$1')"/>
        </xsl:when>
        <!-- Pattern:  NAME, NAME -->
        <xsl:when test="matches($creator, ', ')">
          <xsl:value-of select="replace($creator, '(.+?), .+', '$1')"/>
        </xsl:when>
        <!-- Pattern:  NAME -->
        <xsl:otherwise>
          <xsl:value-of select="$creator"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:choose>
      <!-- Pattern:  'X. NAME' or ' NAME' -->
      <xsl:when test="matches($parse-name, '^.+\.? (\w{2,100})')">
        <xsl:value-of select="replace($parse-name, '^.+\.? (\w{2,100})', '$1')"/>
      </xsl:when>
      <!-- Pattern:  Everything else -->
      <xsl:otherwise>
        <xsl:value-of select="$parse-name"/>
      </xsl:otherwise>
    </xsl:choose>
    
  </xsl:function>

  <!-- Function to parse years out of various date formats -->  

  <xsl:function name="parse:year">
    <xsl:param name="date"/>
    <xsl:param name="pos"/>
    
    <xsl:choose>
      
      <!-- Pattern: 1980 - 1984 -->
      <xsl:when test="matches($date, '([^0-9]|^)([12]\d\d\d)[^0-9]*-[^0-9]*([12]\d\d\d)([^0-9]|$)')">
        <xsl:analyze-string select="$date" regex="([^0-9]|^)([12]\d\d\d)[^0-9]*-[^0-9]*([12]\d\d\d)([^0-9]|$)">
          <xsl:matching-substring>
            <xsl:call-template name="year">
              <xsl:with-param name="year">
                <xsl:copy-of select="parse:output-range(regex-group(2), regex-group(3))"/>
              </xsl:with-param>
            </xsl:call-template>           
          </xsl:matching-substring>
        </xsl:analyze-string>
        <xsl:if test="$pos = 1">
          <xsl:call-template name="sort-year"><xsl:with-param name="year" select="number(replace($date, '.*([12]\d\d\d)[^0-9]*-[^0-9]*([12]\d\d\d).*', '$1'))"/></xsl:call-template>
        </xsl:if>
      </xsl:when>
      
      <!-- Pattern: 1980 - 84 -->
      <xsl:when test="matches($date, '([^0-9]|^)([12]\d\d\d)[^0-9]*-[^0-9]*(\d\d)([^0-9]|$)')">
        <xsl:analyze-string select="$date" regex="([^0-9]|^)([12]\d\d\d)[^0-9]*-[^0-9]*(\d\d)([^0-9]|$)">
          <xsl:matching-substring>
            <xsl:variable name="year1" select="number(regex-group(2))"/>
            <xsl:variable name="century" select="floor($year1 div 100) * 100"/>
            <xsl:variable name="pyear2" select="number(regex-group(3))"/>
            <xsl:variable name="year2" select="$pyear2 + $century"/>
            <xsl:call-template name="year">
              <xsl:with-param name="year">            
                <xsl:copy-of select="parse:output-range($year1, $year2)"/>
              </xsl:with-param>
            </xsl:call-template>
          </xsl:matching-substring>
        </xsl:analyze-string>
        <xsl:if test="$pos = 1">
          <xsl:call-template name="sort-year"><xsl:with-param name="year" select="number(replace($date, '.*([12]\d\d\d)[^0-9]*-[^0-9]*(\d\d).*', '$1'))"/></xsl:call-template>
        </xsl:if>
      </xsl:when>
      
      <!-- Pattern: 1-12-89 -->
      <xsl:when test="matches($date, '([^0-9]|^)\d\d?[^0-9]*[\-/][^0-9]*\d\d?[^0-9]*[\-/][^0-9]*(\d\d)([^0-9]|$)')">
        <xsl:analyze-string select="$date" regex="([^0-9]|^)\d\d?[^0-9]*[\-/][^0-9]*\d\d?[^0-9]*[\-/][^0-9]*(\d\d)([^0-9]|$)">
          <xsl:matching-substring>
            <xsl:call-template name="year"><xsl:with-param name="year" select="number(regex-group(2)) + 1900"/></xsl:call-template>
          </xsl:matching-substring>
        </xsl:analyze-string>
        <xsl:if test="$pos = 1">
          <xsl:call-template name="sort-year"><xsl:with-param name="year" select="number(replace($date, '.*\d\d?[^0-9]*[\-/][^0-9]*\d\d?[^0-9]*[\-/][^0-9]*(\d\d).*', '$1')) + 1900"/></xsl:call-template>
        </xsl:if>
      </xsl:when>
      
      <!-- Pattern: 19890112 -->
      <xsl:when test="matches($date, '([^0-9]|^)([12]\d\d\d)[01]\d[0123]\d')">
        <xsl:analyze-string select="$date" regex="([^0-9]|^)([12]\d\d\d)[01]\d[0123]\d">
          <xsl:matching-substring>
            <xsl:call-template name="year"><xsl:with-param name="year" select="number(regex-group(2))"/></xsl:call-template>            
          </xsl:matching-substring>
        </xsl:analyze-string>
        <xsl:if test="$pos = 1">
          <xsl:call-template name="sort-year"><xsl:with-param name="year" select="number(replace($date, '.*([12]\d\d\d)[01]\d[0123]\d', '$1')) + 1900"/></xsl:call-template>
        </xsl:if>
      </xsl:when>
      
      <!-- Pattern: 890112 -->
      <xsl:when test="matches($date, '([^0-9]|^)([4-9]\d)[01]\d[0123]\d')">
        <xsl:analyze-string select="$date" regex="([^0-9]|^)(\d\d)[01]\d[0123]\d">
          <xsl:matching-substring>
            <xsl:call-template name="year"><xsl:with-param name="year" select="number(regex-group(2)) + 1900"/></xsl:call-template>
          </xsl:matching-substring>
        </xsl:analyze-string>
        <xsl:if test="$pos = 1">
          <xsl:call-template name="sort-year"><xsl:with-param name="year" select="number(replace($date, '.*(\d\d)[01]\d[0123]\d', '$1')) + 1900"/></xsl:call-template>
        </xsl:if>
      </xsl:when>
      
      <!-- Pattern: 011291 -->
      <xsl:when test="matches($date, '([^0-9]|^)[01]\d[0123]\d(\d\d)')">
        <xsl:analyze-string select="$date" regex="([^0-9]|^)[01]\d[0123]\d(\d\d)">
          <xsl:matching-substring>
            <xsl:call-template name="year"><xsl:with-param name="year" select="number(regex-group(2)) + 1900"/></xsl:call-template>
          </xsl:matching-substring>
        </xsl:analyze-string>
        <xsl:if test="$pos = 1">
          <xsl:call-template name="sort-year"><xsl:with-param name="year" select="number(replace($date, '.*[01]\d[0123]\d(\d\d)', '$1')) + 1900"/></xsl:call-template>
        </xsl:if>
      </xsl:when>
      
      <!-- Pattern: 1980 -->
      <xsl:when test="matches($date, '([^0-9]|^)([12]\d\d\d)([^0-9]|$)')">
        <xsl:analyze-string select="$date" regex="([^0-9]|^)([12]\d\d\d)([^0-9]|$)">
          <xsl:matching-substring>
            <xsl:call-template name="year"><xsl:with-param name="year" select="regex-group(2)"/></xsl:call-template>            
          </xsl:matching-substring>
        </xsl:analyze-string>
        <xsl:if test="$pos = 1">
          <!-- NOT WORKING -->
          <xsl:call-template name="sort-year"><xsl:with-param name="year" select="number(replace($date, '.*([12]\d\d\d).*', '$1'))"/></xsl:call-template>
        </xsl:if>
      </xsl:when>
      
      <!-- Pattern: any 4 digits starting with 1 or 2 -->
      <xsl:when test="matches($date, '([12]\d\d\d)')">
        <xsl:analyze-string select="$date" regex="([12]\d\d\d)">
          <xsl:matching-substring>
            <xsl:call-template name="year"><xsl:with-param name="year" select="regex-group(1)"/></xsl:call-template>            
          </xsl:matching-substring>
        </xsl:analyze-string>
        <xsl:if test="$pos = 1">
          <xsl:call-template name="sort-year"><xsl:with-param name="year" select="number($date)"/></xsl:call-template>
        </xsl:if>
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

</xsl:stylesheet>
