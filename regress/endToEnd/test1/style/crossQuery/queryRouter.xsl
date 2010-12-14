<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   exclude-result-prefixes="#all"
   version="2.0">
   
   <xsl:template match="/">
      <route>
         <queryParser path="style/crossQuery/queryParser.xsl"/>
         <errorGen path="style/crossQuery/errorGen.xsl"/>
      </route>
   </xsl:template>
   
</xsl:stylesheet>
