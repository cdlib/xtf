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
    <exec:run command="sed" xsl:extension-element-prefixes="exec">
      <exec:arg>s/\/tag1/tag1/</exec:arg>
      <exec:input>
        <tag1>
          This is some <b>structured</b> input.
        </tag1>
      </exec:input>
    </exec:run>
  </xsl:template>
   
</xsl:stylesheet>
