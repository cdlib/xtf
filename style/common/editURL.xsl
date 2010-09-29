<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
     xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
     xmlns:editURL="http://cdlib.org/xtf/editURL">
  
   <!-- ====================================================================== -->
   <!-- Utility functions for handy editing of URLs                           -->
   <!-- ====================================================================== -->
   
   <!-- Function to set the value of a URL parameter, replacing the old value 
        of that parameter if any.
   -->
   <xsl:function name="editURL:set">
      <xsl:param name="url"/>
      <xsl:param name="param"/>
      <xsl:param name="value"/>
      
      <!-- Protect ampersands, semicolons, etc. in the value so they 
           don't get interpreted as URL parameter separators. -->
      <xsl:variable name="protectedValue" select="editURL:protectValue($value)"/>
      
      <xsl:variable name="regex" select="concat('(^|;)', $param, '[^;]*(;|$)')"/>
      <xsl:choose>
         <xsl:when test="matches($url, $regex)">
            <xsl:value-of select="editURL:clean(replace($url, $regex, concat(';', $param, '=', $protectedValue, ';')))"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:value-of select="editURL:clean(concat($url, ';', $param, '=', $protectedValue))"/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:function>
   
   <!-- Function to remove a URL parameter, if it exists in the URL -->
   <xsl:function name="editURL:remove">
      <xsl:param name="url"/>
      <xsl:param name="param"/>
      
      <xsl:variable name="regex" select="concat('(^|;)', $param, '[^;]*(;|$)')"/>
      <xsl:value-of select="editURL:clean(replace($url, $regex, ';'))"/>
   </xsl:function>
   
   <!-- Utility function to escape characters that have special meaning in a regular
        expression. Call this when passing raw values in 'param' to editURL:remove.
        
        From the XPath specification:
        [10] Char ::= [^.\?*+{}()|^$#x5B#x5D]
        The characters #x5B and #x5D correspond to "[" and "]" respectively.  
   -->
   <xsl:function name="editURL:escapeRegex">
      <xsl:param name="val"/>
      <xsl:value-of select="replace($val, '([.\\?*+{}()|^\[\]$])', '\\$1')"/>
   </xsl:function>
   
   <!-- Utility function to escape ampersands, equal signs, and other characters
   that have special meaning in a URL. -->
   <xsl:function name="editURL:protectValue">
      <xsl:param name="value"/>
      <xsl:value-of select="replace(replace(replace(replace(replace(replace($value,
                              '%',     '%25'),
                              '[+]',   '%2B'),
                              '&amp;', '%26'), 
                              ';',     '%3B'), 
                              '=',     '%3D'),
                              '#',     '%23')"/>
   </xsl:function>
      
   <!-- Function to replace an empty URL with a value. If the URL isn't empty
        it is returned unchanged. By the way, certain parameters such as
        "expand" are still counted as empty.
   -->
   <xsl:function name="editURL:replaceEmpty">
      <xsl:param name="url"/>
      <xsl:param name="replacement"/>
      <xsl:choose>
         <xsl:when test="matches(editURL:clean($url), '^(expand=[^;]*)*$')">
            <xsl:value-of select="$replacement"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:value-of select="$url"/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:function>
   
   <!-- Function to clean up a URL, removing leading and trailing ';' chars, etc. -->
   <xsl:function name="editURL:clean">
      <xsl:param name="v0"/>
      <!-- Change old ampersands to new easy-to-read semicolons -->
      <xsl:variable name="v1" select="replace($v0, '&amp;([^&amp;=;]+=)', ';$1')"/>
      <!-- Get rid of empty parameters -->
      <xsl:variable name="v2" select="replace($v1, '[^;=]+=(;|$)', '')"/>
      <!-- Replace ";;" with ";" -->
      <xsl:variable name="v3" select="replace($v2, ';;+', ';')"/>
      <!-- Get rid of leading and trailing ';' -->
      <xsl:variable name="v4" select="replace($v3, '^;|;$', '')"/>
      <!-- All done. -->
      <xsl:value-of select="$v4"/>
   </xsl:function>
   
   <!-- Function to calculate an unused name for the next facet parameter -->
   <xsl:function name="editURL:nextFacetParam">
      <xsl:param name="queryString"/>
      <xsl:param name="field"/>
      <xsl:variable name="nums">
         <num n="0"/>
         <xsl:analyze-string select="$queryString" regex="(^|;)f([0-9]+)-">
            <xsl:matching-substring><num n="{number(regex-group(2))}"/></xsl:matching-substring>
         </xsl:analyze-string>
      </xsl:variable>
      <xsl:value-of select="concat('f', 1+max(($nums/*/@n)), '-', $field)"/>
   </xsl:function>

</xsl:stylesheet>
