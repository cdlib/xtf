<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
        xmlns:saxon="http://saxon.sf.net/"
        xmlns:xtf="http://cdlib.org/xtf"
        xmlns:date="http://exslt.org/dates-and-times"
        xmlns:parse="http://cdlib.org/xtf/parse"
        extension-element-prefixes="date"
        exclude-result-prefixes="#all">

<!--
   Copyright (c) 2004, Regents of the University of California
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
