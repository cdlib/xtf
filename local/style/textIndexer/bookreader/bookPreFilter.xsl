<!--Local customization file for "bookPreFilter.xsl"-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:FileUtils="java:org.cdlib.xtf.xslt.FileUtils"
                xmlns:local="http://cdlib.org/local"
                xmlns:METS="http://www.loc.gov/METS/"
                xmlns:parse="http://cdlib.org/xtf/parse"
                xmlns:saxon="http://saxon.sf.net/"
                xmlns:scribe="http://archive.org/scribe/xml"
                xmlns:xlink="http://www.w3.org/1999/xlink"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:xtf="http://cdlib.org/xtf"
                version="2.0"
                exclude-result-prefixes="#all">
   <xsl:import href="../../../../style/textIndexer/bookreader/bookPreFilter.xsl"/>

   <!--Any declarations in this file take precedence over those in the stylesheet imported above.-->
   <xsl:import href="../common/preFilterCommon.xsl"/>
   <xsl:output method="xml" indent="yes" encoding="UTF-8"/>
</xsl:stylesheet>

