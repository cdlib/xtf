<!--Local customization file for "rss.xsl"-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:xtf="http://cdlib.org/xtf"
                exclude-result-prefixes="#all"
                version="2.0">
   <xsl:import href="../../../../../style/crossQuery/resultFormatter/default/rss.xsl"/>

   <!--Any declarations in this file take precedence over those in the stylesheet imported above.-->
   <xsl:output method="xml"
               name="rss-xml"
               encoding="UTF-8"
               media-type="text/xml"
               indent="yes"
               exclude-result-prefixes="#all"/>
</xsl:stylesheet>

