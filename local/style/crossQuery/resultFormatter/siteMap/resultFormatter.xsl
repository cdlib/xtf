<!--Local customization file for "resultFormatter.xsl"-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                exclude-result-prefixes="#all"
                version="2.0">
   <xsl:import href="../../../../../style/crossQuery/resultFormatter/siteMap/resultFormatter.xsl"/>

   <!--Any declarations in this file take precedence over those in the stylesheet imported above.-->
   <xsl:import href="../common/resultFormatterCommon.xsl"/>
   <xsl:output method="xml"
               encoding="UTF-8"
               media-type="text/xml"
               indent="yes"
               exclude-result-prefixes="#all"/>
</xsl:stylesheet>

