<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
                              
<!-- ====================================================================== -->
<!-- Output Parameters                                                      -->
<!-- ====================================================================== -->

  <xsl:output method="xml" indent="yes" encoding="UTF-8" media-type="text/xml"/>

<!-- ====================================================================== -->
<!-- Root Template                                                          -->
<!-- ====================================================================== -->
  
  <xsl:template match="/">
    <xsl:apply-templates select="//div2"/>
  </xsl:template>
  
  <xsl:template match="div2">
      Div2 <xsl:value-of select="@id"/>: count(previous)= <xsl:value-of select="count(preceding::div2)"/>
  </xsl:template>
   
</xsl:stylesheet>
