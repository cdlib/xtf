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
<!-- Default: identity transformation                                       -->
<!-- ====================================================================== -->

<xsl:template match="@*|node()">
  <xsl:copy>
    <xsl:apply-templates select="@*|node()"/>
  </xsl:copy>
</xsl:template>

<!-- ====================================================================== -->
<!-- Root Template                                                          -->
<!-- ====================================================================== -->

<xsl:template match="/*">
  <xsl:copy>
    <xsl:copy-of select="@*"/>
    <xsl:call-template name="get-mets"/>
    <xsl:apply-templates/>
  </xsl:copy>
</xsl:template>

<xsl:template match="teiHeader">
  <xsl:copy>
    <xsl:copy-of select="@*"/>
    <xsl:attribute name="xtf:index" select="'no'"/>
    <xsl:apply-templates/>
  </xsl:copy>
</xsl:template>

<!-- Test implmentation of structural searching. KVH 8-30-04 -->
<xsl:template match="head[parent::*[self::div1 or self::div2 or self::div3 or self::div4 or self::div5 or self::div6 or self::div7]]">
  <xsl:copy>
    <xsl:copy-of select="@*"/>
    <xsl:attribute name="xtf:sectionType" select="concat('head ', @type)"/>
    <xsl:attribute name="xtf:wordBoost" select="2.0"/>
    <xsl:apply-templates/>
  </xsl:copy>
</xsl:template>

<xsl:template name="get-mets">
  <xsl:variable name="docpath" select="saxon:system-id()"/>
  <xsl:variable name="base" select="substring-before($docpath, '.xml')"/>
  <xsl:variable name="dcpath" select="concat($base, '.dc.xml')"/>
  <xsl:apply-templates select="document($dcpath)" mode="inmets"/>
</xsl:template>

<xsl:template match="dc" mode="inmets">
  <xsl:for-each select="*">
    <xsl:element name="{name()}">
      <xsl:attribute name="xtf:meta" select="'true'"/>
      <xsl:value-of select="string()"/>
    </xsl:element>
  </xsl:for-each>
    
  <!-- If a date field is present, try to extract a year or year range from 
       it, and add that to the meta-data. -->
  <xsl:if test="date">
    <xsl:variable name="date" select="string(date)"/>
    <xsl:copy-of select="parse:year($date)"/>
  </xsl:if>
</xsl:template>

<xsl:function name="parse:year">
  <xsl:param name="date"/>

  <xsl:choose>

    <!-- Pattern: 1980 - 1984 -->
    <xsl:when test="matches($date, '([^0-9]|^)([12]\d\d\d)[^0-9]*-[^0-9]*([12]\d\d\d)([^0-9]|$)')">
      <xsl:analyze-string select="$date" regex="([^0-9]|^)([12]\d\d\d)[^0-9]*-[^0-9]*([12]\d\d\d)([^0-9]|$)">
        <xsl:matching-substring>
          <xsl:copy-of select="parse:output-range(regex-group(2), 
                                                  regex-group(3))"/>
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
          <year xtf:meta="true"><xsl:value-of select="number(regex-group(2)) + 1900"/></year>
        </xsl:matching-substring>
      </xsl:analyze-string>
    </xsl:when>

    <!-- Pattern: 19890112 -->
    <xsl:when test="matches($date, '([^0-9]|^)([12]\d\d\d)[01]\d[0123]\d')">
      <xsl:analyze-string select="$date" regex="([^0-9]|^)([12]\d\d\d)[01]\d[0123]\d">
        <xsl:matching-substring>
          <year xtf:meta="true"><xsl:value-of select="number(regex-group(2))"/></year>
        </xsl:matching-substring>
      </xsl:analyze-string>
    </xsl:when>

    <!-- Pattern: 890112 -->
    <xsl:when test="matches($date, '([^0-9]|^)([4-9]\d)[01]\d[0123]\d')">
      <xsl:analyze-string select="$date" regex="([^0-9]|^)(\d\d)[01]\d[0123]\d">
        <xsl:matching-substring>
          <year xtf:meta="true"><xsl:value-of select="number(regex-group(2)) + 1900"/></year>
        </xsl:matching-substring>
      </xsl:analyze-string>
    </xsl:when>

    <!-- Pattern: 011291 -->
    <xsl:when test="matches($date, '([^0-9]|^)[01]\d[0123]\d(\d\d)')">
      <xsl:analyze-string select="$date" regex="([^0-9]|^)[01]\d[0123]\d(\d\d)">
        <xsl:matching-substring>
          <year xtf:meta="true"><xsl:value-of select="number(regex-group(2)) + 1900"/></year>
        </xsl:matching-substring>
      </xsl:analyze-string>
    </xsl:when>

    <!-- Pattern: 1980 -->
    <xsl:when test="matches($date, '([^0-9]|^)([12]\d\d\d)([^0-9]|$)')">
      <xsl:analyze-string select="$date" regex="([^0-9]|^)([12]\d\d\d)([^0-9]|$)">
        <xsl:matching-substring>
          <year xtf:meta="true"><xsl:value-of select="regex-group(2)"/></year>
        </xsl:matching-substring>
      </xsl:analyze-string>
    </xsl:when>

    <!-- Pattern: any 4 digits starting with 1 or 2 -->
    <xsl:when test="matches($date, '([12]\d\d\d)')">
      <xsl:analyze-string select="$date" regex="([12]\d\d\d)">
        <xsl:matching-substring>
          <year xtf:meta="true"><xsl:value-of select="regex-group(1)"/></year>
        </xsl:matching-substring>
      </xsl:analyze-string>
    </xsl:when>

  </xsl:choose>

</xsl:function>

<xsl:function name="parse:output-range">
  <xsl:param name="year1-in"/>
  <xsl:param name="year2-in"/>

  <xsl:variable name="year1" select="number($year1-in)"/>
  <xsl:variable name="year2" select="number($year2-in)"/>

  <year xtf:meta="true">

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

  </year>

</xsl:function>


</xsl:stylesheet>
