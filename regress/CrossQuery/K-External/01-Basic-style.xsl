<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
                              xmlns:exec="java:/org.cdlib.xtf.saxonExt.Exec">
                              
<!-- ====================================================================== -->
<!-- Output Parameters                                                      -->
<!-- ====================================================================== -->

  <xsl:output method="xml" indent="yes" encoding="UTF-8" media-type="text/xml"/>

<!-- ====================================================================== -->
<!-- Root Template                                                          -->
<!-- ====================================================================== -->
  
  <xsl:template match="/crossQueryResult">
    Result from external command:
    <exec:run command="echo" xsl:extension-element-prefixes="exec">
      <exec:arg>foo</exec:arg>
      <exec:arg select="'bar'"/>
    </exec:run>
  </xsl:template>
   
</xsl:stylesheet>
