<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
                              xmlns:exec="java:/org.cdlib.xtf.saxonExt.Exec"
                              xmlns:FileUtils="java:org.cdlib.xtf.xslt.FileUtils">
                              
<!-- ====================================================================== -->
<!-- Output Parameters                                                      -->
<!-- ====================================================================== -->

  <xsl:output method="xml" indent="yes" encoding="UTF-8" media-type="text/xml"/>

<!-- ====================================================================== -->
<!-- Root Template                                                          -->
<!-- ====================================================================== -->
  
  <xsl:template match="/crossQueryResult">
    FileUtils test:
    
    Existence: <xsl:value-of select="FileUtils:exists('33-FileUtils-in.xml')"/>
    Non-existence: <xsl:value-of select="FileUtils:exists('foobar.xml')"/>
    
    Length: <xsl:value-of select="FileUtils:length('33-FileUtils-in.xml')"/>
    
    Human bytes: <xsl:value-of select="FileUtils:humanFileSize(123)"/>
    Human K: <xsl:value-of select="FileUtils:humanFileSize(12345)"/>
    Human M: <xsl:value-of select="FileUtils:humanFileSize(12345678)"/>
    Human G: <xsl:value-of select="FileUtils:humanFileSize(12345678901)"/>
    
    MD5: <xsl:value-of select="FileUtils:md5Hash('abcdef')"/>
  </xsl:template>
   
</xsl:stylesheet>
