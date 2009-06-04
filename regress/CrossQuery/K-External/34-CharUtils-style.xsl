<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
                              xmlns:exec="java:/org.cdlib.xtf.saxonExt.Exec"
                              xmlns:CharUtils="java:org.cdlib.xtf.xslt.CharUtils">
                              
<!-- ====================================================================== -->
<!-- Output Parameters                                                      -->
<!-- ====================================================================== -->

  <xsl:output method="xml" indent="yes" encoding="UTF-8" media-type="text/xml"/>

<!-- ====================================================================== -->
<!-- Root Template                                                          -->
<!-- ====================================================================== -->
  
  <xsl:template match="/crossQueryResult">
    CharUtils test:
    
    No-op: <xsl:value-of select="CharUtils:applyAccentMap('accentMap.txt.gz', 'This is a test.')"/>
    Single word: <xsl:value-of select="CharUtils:applyAccentMap('accentMap.txt.gz', 'buñuel')"/>
    Multiple words: <xsl:value-of select="CharUtils:applyAccentMap('accentMap.txt.gz', '  buñuel  mah&#x323;mu&#x304;d arch&#xe6;ology  ')"/>
  </xsl:template>
   
</xsl:stylesheet>
