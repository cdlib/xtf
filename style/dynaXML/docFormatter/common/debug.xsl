<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
                xmlns:xtf="http://cdlib.org/xtf">
  
  <xsl:output method="xml" encoding="UTF-8" indent="yes" media-type="text/xml"/>
  
  <!-- ====================================================================== -->
  <!-- Debug Template                                                         -->
  <!-- ====================================================================== -->
  
  <!-- Produces raw XML -->
  
  <xsl:template match="*" mode="debug">
    <xsl:element name="{name(.)}">
      <xsl:copy-of select="@*"/>
      <xsl:apply-templates mode="debug"/>
    </xsl:element>
  </xsl:template>
  
</xsl:stylesheet>
