<!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
<!-- Index document selection stylesheet                                    -->
<!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
   exclude-result-prefixes="#all">

   <xsl:template match="directory">
      <indexFiles>
         <xsl:apply-templates/>
      </indexFiles>
   </xsl:template>
   
   <xsl:template match="file">
      <xsl:if test="ends-with(@fileName, '.xml')">
         <indexFile fileName="{@fileName}"
                    preFilter="style/textIndexer/preFilter.xsl"
                    displayStyle="style/dynaXML/docFormatter.xsl"/>
       </xsl:if>
   </xsl:template>

</xsl:stylesheet>
