<!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
<!-- Index document selection stylesheet                                    -->
<!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
        xmlns:saxon="http://saxon.sf.net/"
        xmlns:xtf="http://cdlib.org/xtf"
        xmlns:date="http://exslt.org/dates-and-times"
        extension-element-prefixes="date"
        exclude-result-prefixes="#all">
  
  <xsl:template match="directory">
    <indexFiles>
      <xsl:apply-templates/>
    </indexFiles>
  </xsl:template>

  <xsl:template match="file">
    <xsl:if test="ends-with(@fileName, '.xml')">
      <indexFile fileName="{@fileName}" type="XML" prefilter="preFilter.xsl"/>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
