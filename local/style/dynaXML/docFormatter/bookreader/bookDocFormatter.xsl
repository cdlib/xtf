<!--Local customization file for "bookDocFormatter.xsl"-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xtf="http://cdlib.org/xtf"
                xmlns:session="java:org.cdlib.xtf.xslt.Session"
                xmlns:editURL="http://cdlib.org/xtf/editURL"
                xmlns:local="http://local"
                xmlns="http://www.w3.org/1999/xhtml"
                version="2.0"
                extension-element-prefixes="session"
                exclude-result-prefixes="#all">
   <xsl:import href="../../../../../style/dynaXML/docFormatter/bookreader/bookDocFormatter.xsl"/>

   <!--Any declarations in this file take precedence over those in the stylesheet imported above.-->
   <xsl:import href="../common/docFormatterCommon.xsl"/>
   <xsl:import href="../../../xtfCommon/xtfCommon.xsl"/>
   <xsl:output method="xhtml"
               indent="yes"
               encoding="UTF-8"
               media-type="text/html; charset=UTF-8"
               doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
               doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"
               exclude-result-prefixes="#all"
               omit-xml-declaration="yes"/>
   <xsl:strip-space elements="*"/>
   <xsl:include href="search.xsl"/>
</xsl:stylesheet>

