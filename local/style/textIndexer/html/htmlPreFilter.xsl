<!--Local customization file for "htmlPreFilter.xsl"-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:parse="http://cdlib.org/xtf/parse"
                xmlns:xtf="http://cdlib.org/xtf"
                xmlns:saxon="http://saxon.sf.net/"
                version="2.0"
                extension-element-prefixes="saxon"
                exclude-result-prefixes="#all">
   <xsl:import href="../../../../style/textIndexer/html/htmlPreFilter.xsl"/>

   <!--Any declarations in this file take precedence over those in the stylesheet imported above.-->
   <xsl:import href="../common/preFilterCommon.xsl"/>
   <xsl:output method="xml" indent="yes" encoding="UTF-8"/>
</xsl:stylesheet>

