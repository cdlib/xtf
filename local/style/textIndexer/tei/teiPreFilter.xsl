<!--Local customization file for "teiPreFilter.xsl"-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:date="http://exslt.org/dates-and-times"
                xmlns:parse="http://cdlib.org/xtf/parse"
                xmlns:xtf="http://cdlib.org/xtf"
                xmlns:FileUtils="java:org.cdlib.xtf.xslt.FileUtils"
                version="2.0"
                extension-element-prefixes="date FileUtils"
                exclude-result-prefixes="#all">
   <xsl:import href="../../../../style/textIndexer/tei/teiPreFilter.xsl"/>

   <!--Any declarations in this file take precedence over those in the stylesheet imported above.-->
   <xsl:import href="../common/preFilterCommon.xsl"/>
   <xsl:output method="xml" indent="yes" encoding="UTF-8"/>
</xsl:stylesheet>

