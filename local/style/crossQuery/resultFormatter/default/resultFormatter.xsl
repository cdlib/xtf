<!--Local customization file for "resultFormatter.xsl"-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:session="java:org.cdlib.xtf.xslt.Session"
                xmlns:editURL="http://cdlib.org/xtf/editURL"
                xmlns="http://www.w3.org/1999/xhtml"
                extension-element-prefixes="session"
                exclude-result-prefixes="#all"
                version="2.0">
   <xsl:import href="../../../../../style/crossQuery/resultFormatter/default/resultFormatter.xsl"/>

   <!--Any declarations in this file take precedence over those in the stylesheet imported above.-->
   <xsl:import href="../common/resultFormatterCommon.xsl"/>
   <xsl:import href="rss.xsl"/>
   <xsl:include href="searchForms.xsl"/>
   <xsl:output method="xhtml"
               indent="no"
               encoding="UTF-8"
               media-type="text/html; charset=UTF-8"
               doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
               doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"
               omit-xml-declaration="yes"
               exclude-result-prefixes="#all"/>
</xsl:stylesheet>

