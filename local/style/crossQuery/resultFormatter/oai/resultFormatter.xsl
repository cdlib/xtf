<!--Local customization file for "resultFormatter.xsl"-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:FileUtils="java:org.cdlib.xtf.xslt.FileUtils"
                extension-element-prefixes="FileUtils"
                exclude-result-prefixes="#all"
                version="2.0">
   <xsl:import href="../../../../../style/crossQuery/resultFormatter/oai/resultFormatter.xsl"/>

   <!--Any declarations in this file take precedence over those in the stylesheet imported above.-->
   <xsl:output method="xml"
               encoding="UTF-8"
               media-type="text/xml"
               indent="yes"
               exclude-result-prefixes="#all"/>
</xsl:stylesheet>

