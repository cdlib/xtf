<!--Local customization file for "eadPreFilter.xsl"-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:parse="http://cdlib.org/xtf/parse"
                xmlns:xtf="http://cdlib.org/xtf"
                version="2.0"
                exclude-result-prefixes="#all">
   <xsl:import href="../../../../style/textIndexer/ead/eadPreFilter.xsl"/>

   <!--Any declarations in this file take precedence over those in the stylesheet imported above.-->
   <xsl:import href="../common/preFilterCommon.xsl"/>
   <xsl:import href="./supplied-headings.xsl"/>
   <xsl:import href="./at2oac.xsl"/>
   <xsl:output method="xml" indent="yes" encoding="UTF-8"/>
</xsl:stylesheet>

