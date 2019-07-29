<!--Local customization file for "resultFormatter.xsl"-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema"
                xmlns:dc="http://purl.org/dc/elements/1.1/"
                xmlns:srw="http://www.loc.gov/zing/srw/"
                xmlns:srw_dc="info:srw/schema/1/dc-schema"
                version="2.0">
   <xsl:import href="../../../../style/sru/resultFormatter/resultFormatter.xsl"/>

   <!--Any declarations in this file take precedence over those in the stylesheet imported above.-->
   <xsl:output method="xml"
               indent="yes"
               encoding="UTF-8"
               media-type="text/xml"/>
</xsl:stylesheet>

