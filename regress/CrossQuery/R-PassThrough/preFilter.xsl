<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
        xmlns:saxon="http://saxon.sf.net/"
        xmlns:xtf="http://cdlib.org/xtf"
        exclude-result-prefixes="#all">

<xsl:param name="pass1"/>
<xsl:param name="pass2"/>
  
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
    <xsl:choose>
      <xsl:when test="$pass1 = 'myPass1' and $pass2 = 'myPass2'">
        <xsl:copy>
          <xsl:copy-of select="@*"/>
          <xsl:apply-templates/>
        </xsl:copy>
      </xsl:when>
      <xsl:otherwise>
        <xsl:message terminate="yes">
          ERROR: pass1/pass2 should be 'myPass1'/'myPass2' but are '<xsl:value-of select="$pass1"/>/<xsl:value-of select="$pass2"/>'
        </xsl:message>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
<!-- ====================================================================== -->
<!-- Metadata Marking                                                       -->
<!-- ====================================================================== -->

  <xsl:template match="title|creator|subject|description|date|type|identifier|relation|rights|foo">
    <xsl:copy>
      <xsl:copy-of select="@*"/>
      <xsl:attribute name="xtf:meta" select="'true'"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
