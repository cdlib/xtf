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
    <xsl:variable name="data">
      <exec:run command="sed" xsl:extension-element-prefixes="exec">
        <exec:arg>s/tag2/tag3/g</exec:arg>
        <exec:input>
          <tag1>
            <tag2>Wow foo</tag2>
            <tag3>Happy bar</tag3>
            <tag4>Zippy wazoo</tag4>
          </tag1>
        </exec:input>
      </exec:run>
    </xsl:variable>
    
    Output from external command:
    <xsl:copy-of select="$data"/>
    
    Applying templates:
    <xsl:apply-templates select="$data//tag3"/>
  </xsl:template>
  
  <xsl:template match="tag3">
    Tag 3: <xsl:value-of select="."/>
  </xsl:template>
   
</xsl:stylesheet>
