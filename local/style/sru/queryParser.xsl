<!--Local customization file for "queryParser.xsl"-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:dc="http://purl.org/dc/elements/1.1/"
                xmlns:mets="http://www.loc.gov/METS/"
                xmlns:xlink="http://www.w3.org/TR/xlink"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:parse="http://cdlib.org/parse"
                xmlns:srw="http://www.loc.gov/zing/srw/"
                xmlns:diag="http://www.loc.gov/zing/srw/diagnostic/"
                xmlns:xcql="http://www.loc.gov/zing/srw/xcql/"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema"
                xmlns:xsd="http://www.w3.org/2001/XMLSchema-instance"
                version="2.0"
                exclude-result-prefixes="xsl dc mets xlink xs parse">
   <xsl:import href="../../../style/sru/queryParser.xsl"/>

   <!--Any declarations in this file take precedence over those in the stylesheet imported above.-->
   <xsl:output method="xml" indent="yes" encoding="utf-8"/>
   <xsl:strip-space elements="*"/>
</xsl:stylesheet>

