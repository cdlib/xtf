<!-- Copyright 2012 UC Regents all Rights Reserved -->
<!-- BSD License at botton of file -->
<xsl:stylesheet 
  version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:ead="urn:isbn:1-931666-22-9"
  xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns="urn:isbn:1-931666-22-9">

<!-- set strip-namespace to anything other than yes to
     disable default stripping
-->
<xsl:param name="strip-namespace" select="'yes'"/>

<!-- set label-to-physdesc to anything other than '' to 
     disable default removal of mis-labeled container/@label's
     and they will be converted to sibling <physdesc>'s instead
-->
<xsl:param name="label-to-physdesc"/> 

<!-- supply alternate dsc type or use default -->
<xsl:param name="dsc-type" select="'combined'"/>

<!-- supply repositorycode or use default -->
<xsl:param 
	name="repositorycode" 
	select="/ead:ead/ead:eadheader/ead:eadid/@mainagencycode"/>

<!-- supply countrycode or use default -->
<xsl:param 
	name="countrycode" 
	select="translate(
		/ead:ead/ead:eadheader/ead:eadid/@countrycode,
		'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ' )" />

<xsl:variable name="namespace">
  <xsl:choose>
    <xsl:when test="$strip-namespace = 'yes'"/>
    <xsl:otherwise>
      <xsl:text>urn:isbn:1-931666-22-9</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
</xsl:variable>

<xsl:variable name="xlink-namespace">
  <xsl:choose>
    <xsl:when test="$strip-namespace = 'yes'"/>
    <xsl:otherwise>
      <xsl:text>http://www.w3.org/1999/xlink</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
</xsl:variable>

<!-- root template -->
<xsl:template match="/|comment()|processing-instruction()" mode="at2oac">
    <xsl:copy>
      <xsl:apply-templates mode="at2oac"/>
    </xsl:copy>
</xsl:template>

<!-- add dsc/@type -->
<xsl:template match="ead:dsc[parent::ead:archdesc and position()=1]" mode="at2oac">
  <xsl:element name="{name()}" namespace="{$namespace}">
    <xsl:apply-templates select="@*" mode="at2oac"/>
    <xsl:if test="not(@type) and $dsc-type!=''">
      <xsl:attribute name="type">
        <xsl:value-of select="$dsc-type"/>
      </xsl:attribute>
    </xsl:if>
    <xsl:apply-templates mode="at2oac"/>
  </xsl:element>
</xsl:template>

<!-- copy repositorycode and countrycode from eadid to unitid in
     archdesc/did/unittitle
-->
<xsl:template match="ead:unitid[parent::ead:did and not(ancestor::ead:dsc)]" mode="at2oac">
  <xsl:element name="{name()}" namespace="{$namespace}">
    <xsl:apply-templates select="@*" mode="at2oac"/>
    <xsl:if test="not(@repositorycode) and $repositorycode!=''">
      <xsl:attribute name="repositorycode">
        <xsl:value-of select="$repositorycode"/>
      </xsl:attribute>
    </xsl:if>
    <xsl:if test="not(@countrycode) and $countrycode!=''">
      <xsl:attribute name="countrycode">
        <xsl:value-of select="$countrycode"/>
      </xsl:attribute>
    </xsl:if>
    <xsl:apply-templates mode="at2oac"/>
  </xsl:element>
</xsl:template>

<!-- upper-case the country code -->
<xsl:template match="@countrycode[parent::ead:eadid]" mode="at2oac">
  <xsl:attribute name="countrycode">
    <xsl:value-of select="$countrycode"/>
  </xsl:attribute>
</xsl:template>

<!-- copy overloaded container labels to sibling physdesc -->
<!-- TODO: specifically target AT @label values? -->
<xsl:template match="ead:container[@label]" mode="at2oac">
  <xsl:element name="{name()}" namespace="{$namespace}">
    <xsl:apply-templates select="@*[name()!='label'] | node() " mode="at2oac"/>
  </xsl:element>
  <xsl:if test="$label-to-physdesc">
    <xsl:element name="physdesc" namespace="{$namespace}">
      <xsl:value-of select="@label"/>
    </xsl:element>
  </xsl:if>
</xsl:template>

<!-- dao from AT style to MOAC style -->

<xsl:template match="ead:c01|ead:c02|ead:c03|ead:c04|ead:c05|ead:c06|ead:c07|ead:c08|ead:c09|ead:c10|ead:c11|ead:c12" mode="at2oac">
<!-- head? , did , (accessrestrict | accruals | acqinfo | altformavail | appraisal | arrangement | bibliography | bioghist | controlaccess | custodhist | descgrp | fileplan | index | odd | originalsloc | otherfindaid | phystech | prefercite | processinfo | relatedmaterial | scopecontent | separatedmaterial | userestrict | dsc | dao | daogrp | note)* , (thead? , c04+) 
-->
  <xsl:element name="{name()}" namespace="{$namespace}">
    <xsl:apply-templates select="@*" mode="at2oac"/>
    <xsl:apply-templates select="ead:head | ead:did | ead:accessrestrict | ead:accruals
      | ead:acqinfo | ead:altformavail | ead:appraisal | ead:arrangement | ead:bibliography 
      | ead:bioghist | ead:controlaccess | ead:custodhist | ead:descgrp | ead:fileplan | ead:index
      | ead:odd | ead:originalsloc | ead:otherfindaid | ead:phystech | ead:prefercite 
      | ead:processinfo | ead:relatedmaterial | ead:scopecontent | ead:separatedmaterial 
      | ead:userestrict | ead:dsc | ead:daogrp | ead:note | ead:thead" mode="at2oac"/>
    <xsl:apply-templates select="ead:dao" mode="convert" />
    <xsl:apply-templates select="ead:c01|ead:c02|ead:c03|ead:c04|ead:c05|ead:c06|ead:c07|ead:c08|ead:c09|ead:c10|ead:c11|ead:c12" mode="at2oac"/>

  </xsl:element>
</xsl:template>

<xsl:template match="ead:dao" mode="convert"><xsl:text>
</xsl:text>
  <xsl:variable name="plusone">
    <xsl:value-of select="number(substring(name(..),2,2))+1"/>
  </xsl:variable>
  <xsl:variable name="cD">
    <xsl:choose>
      <xsl:when test="$plusone &lt; 10">
        <xsl:text>c0</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>c1</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <!-- c0x -->
  <xsl:element name="{$cD}{number($plusone) mod 10}" namespace="{$namespace}">
    <!-- did -->
    <xsl:element name="did" namespace="{$namespace}">
      <!-- unititle -->
      <xsl:element namespace="{$namespace}" name="unittitle">
        <xsl:value-of select="ead:daodesc/ead:p"/>
      </xsl:element>
      <!-- dao -->
      <xsl:element namespace="{$namespace}" name="dao">
        <xsl:attribute name="href" namespace="{$xlink-namespace}">
          <xsl:value-of select="@xlink:href"/>
        </xsl:attribute>
      </xsl:element>
    </xsl:element>
  </xsl:element>
</xsl:template>

<!-- modified identity templates -->

<xsl:template match="*" mode="at2oac">
  <xsl:choose>
    <xsl:when test="$namespace!=''">
      <xsl:copy>
        <xsl:apply-templates select="@*|node()" mode="at2oac"/>
      </xsl:copy>
    </xsl:when>
    <xsl:otherwise>
      <xsl:element name="{local-name()}" namespace="{$namespace}">
        <xsl:apply-templates select="@*|node()" mode="at2oac"/>
      </xsl:element>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="@*" mode="at2oac">
  <xsl:choose>
    <xsl:when test="$namespace!=''">
      <xsl:copy/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:attribute name="{local-name()}">
        <xsl:value-of select="."/>
      </xsl:attribute>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>


<!-- convert @xlink to EAD linking attributes if $namespace='' -->
<!-- @xlink:
      http://www.w3.org/TR/xlink/#link-behaviors -->
<!-- EAD linking attributes: 
      http://www.loc.gov/ead/tglib/att_link.html -->

<!-- rename xlink:type to linktype (or copy as is) -->
<xsl:template match="@xlink:type" mode="at2oac">
  <xsl:choose>
    <xsl:when test="$namespace!=''">
      <!-- keep the @xlink: attributes if we are not nuking namespaces -->
      <xsl:copy/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:attribute name='linktype'>
        <xsl:value-of select="."/>
      </xsl:attribute>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="@xlink:actuate | @xlink:show" mode="at2oac">
  <xsl:choose>
    <xsl:when test="$namespace!=''">
      <!-- keep the @xlink: attributes if we are not nuking namespaces -->
      <xsl:copy/>
    </xsl:when>
    <xsl:otherwise>
      <!-- hack @xlink: attributes to be EAD 2002 DTD link attributes -->
      <xsl:attribute name="{local-name()}">
        <xsl:choose>
          <!-- @xlink:acuate and @xlink:show 
               whith the value "other" and "none"
               convert to 
               acutateother, showother, acutatenone, or shownone
          -->
          <xsl:when test=".='other' or .='none'">
            <xsl:value-of select="local-name()"/>
            <xsl:value-of select="."/>
          </xsl:when>
          <xsl:otherwise>
              <!-- @xlink camel case values get foled to EAD DTD all lower case values -->
              <xsl:value-of select="translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="@xsi:*" mode="at2oac">
  <xsl:choose>
    <xsl:when test="$namespace!=''">
      <xsl:copy/>
    </xsl:when>
    <xsl:otherwise/>
  </xsl:choose>
</xsl:template>

</xsl:stylesheet>
<!--

Copyright (c) 2012, Regents of the University of California
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

- Redistributions of source code must retain the above copyright notice, 
  this list of conditions and the following disclaimer.
- Redistributions in binary form must reproduce the above copyright notice, 
  this list of conditions and the following disclaimer in the documentation 
  and/or other materials provided with the distribution.
- Neither the name of the University of California nor the names of its
  contributors may be used to endorse or promote products derived from this 
  software without specific prior written permission.

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
