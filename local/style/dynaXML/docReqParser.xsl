<!--Local customization file for "docReqParser.xsl"-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:dc="http://purl.org/dc/elements/1.1/"
                xmlns:mets="http://www.loc.gov/METS/"
                xmlns:mods="http://www.loc.gov/mods/"
                xmlns:xlink="http://www.w3.org/TR/xlink"
                xmlns:parse="http://cdlib.org/parse"
                xmlns:FileUtils="java:org.cdlib.xtf.xslt.FileUtils"
                version="2.0"
                extension-element-prefixes="FileUtils"
                exclude-result-prefixes="#all">
   <xsl:import href="../../../style/dynaXML/docReqParser.xsl"/>

   <!--Any declarations in this file take precedence over those in the stylesheet imported above.-->
   <xsl:output method="xml" indent="yes" encoding="utf-8"/>
</xsl:stylesheet>

