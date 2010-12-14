<xsl:stylesheet version="2.0"
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:dc="http://purl.org/dc/elements/1.1/"
   xmlns:mets="http://www.loc.gov/METS/"
   xmlns:mods="http://www.loc.gov/mods/"
   xmlns:xlink="http://www.w3.org/TR/xlink"
   xmlns:parse="http://cdlib.org/parse"
   xmlns:FileUtils="java:org.cdlib.xtf.xslt.FileUtils"
   extension-element-prefixes="FileUtils"
   exclude-result-prefixes="#all">
   
   <!-- Templates used for parsing text queries -->               
   <xsl:import href="../crossQuery/queryParser.xsl"/>
   
   <xsl:output method="xml"
      indent="yes"
      encoding="utf-8"/>
   
   <xsl:param name="http.URL"/>
   <xsl:variable name="ercPat" select="'^(http://[^?]+)/erc/([^?]+)\?q$'"/>
   <!-- Normally this is a URL parameter, but for ERC we also support an 
        abbreviated form where it's part of the main URL instead. -->
   <xsl:param name="docId" select="replace($http.URL, $ercPat, '$2')"/>
   <xsl:param name="query" select="'0'"/>
   <xsl:param name="query-join" select="'0'"/>
   <xsl:param name="query-exclude" select="'0'"/>
   <xsl:param name="sectionType" select="'0'"/>
   
   <!-- ====================================================================== -->
   <!-- Root Template                                                          -->
   <!-- ====================================================================== -->
   
   <xsl:template match="/">
      <style path="style/dynaXML/docFormatter.xsl"/>
      <source path="{concat('data/',$docId)}"/>
      <index configPath="conf/textIndexer.conf" name="default"/>
      <preFilter path="style/textIndexer/preFilter.xsl"/>

      <xsl:if test="$query != '0' and $query != ''">
         
         <xsl:variable name="query" select="/parameters/param[@name='query']"/>
         <xsl:variable name="sectionType" select="/parameters/param[@name='sectionType']"/>
         
         <query indexPath="index" termLimit="1000" workLimit="500000">
            <xsl:apply-templates select="$query"/>
         </query>
      </xsl:if>

      <auth access="allow" type="all"/>
   </xsl:template>
   
</xsl:stylesheet>

