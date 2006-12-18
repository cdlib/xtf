<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
                xmlns:cdl="http://cdlib.org">
  
  <xsl:param name="keyword"/>

  <!-- ====================================================================== -->
  <!-- Generate Spelling Suggestion                                           -->
  <!-- ====================================================================== -->

  <xsl:template name="did-you-mean">
    <xsl:param name="baseURL"/>
    <xsl:param name="spelling"/>
    
    <xsl:variable name="newURL" select="cdl:replace-misspellings($baseURL, $spelling/term)"/>
    <br/>
    <b>Did you mean to search for
    <a href="{$newURL}">
      <xsl:call-template name="format-spelling"/>
    </a>
    <xsl:text>?</xsl:text> </b>
  </xsl:template>
  
  <!-- 
    Scan the URL and replace possibly misspelled words with suggestions
    from the spelling correction engine.
  -->
  <xsl:function name="cdl:replace-misspellings">
    <xsl:param name="baseURL"/>
    <xsl:param name="terms"/>
    
    <!-- Figure out what field to apply to... handle the 'keywords' pseudo-field specially -->
    <xsl:variable name="term" select="$terms[1]"/>
    <xsl:variable name="field">
      <xsl:choose>
        <xsl:when test="$keyword and ($term/@field = 'text')">
          <xsl:text>keyword</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$term/@field"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    
    <xsl:choose>
      <xsl:when test="$term">
        <xsl:variable name="remainder" select="cdl:replace-misspellings($baseURL, $terms[position() > 1])"/>
        <xsl:variable name="match" select="concat('(', $field, '=[^;]*)', $term/@term)"/>
        <xsl:value-of select="replace($remainder, $match, concat('$1', $term/suggestion[1]/@term), 'i')"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$baseURL"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:function>

    <!-- 
    Scan the URL and replace possibly misspelled words with suggestions
    from the spelling correction engine.
  -->
  <xsl:function name="cdl:fix-terms">
    <xsl:param name="terms"/>
    <xsl:param name="spelling"/>
    
    <!-- Figure out what field to apply to... handle the 'keywords' pseudo-field specially -->
    <xsl:variable name="term" select="$terms[1]"/>
    <xsl:variable name="field">
      <xsl:choose>
        <xsl:when test="$term/ancestor-or-self::*[@field]">
          <xsl:value-of select="$term/ancestor-or-self::*[@field][1]/@field"/>
        </xsl:when>
        <xsl:otherwise> <!-- if no field, assume it's 'fields', i.e. the keyword field -->
          <xsl:value-of select="'text'"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    
    <xsl:if test="$term">
      <xsl:variable name="replacement" select="$spelling/term[(@field = $field) and (@term = string($term))]"/>
      <xsl:choose>
        <xsl:when test="$replacement">
          <xsl:value-of select="$replacement/suggestion[1]/@term"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="string($term)"/>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:if test="count($terms) > 1">
        <xsl:text> </xsl:text>
        <xsl:value-of select="cdl:fix-terms($terms[position() > 1], $spelling)"/>
      </xsl:if>
    </xsl:if>
  </xsl:function>

<!-- ====================================================================== -->
<!-- Format Terms with Spelling Corrections                                 -->
<!-- ====================================================================== -->
    
    <xsl:template name="format-spelling">
        
        <xsl:choose>
            <xsl:when test="$keyword">
                <!-- probably very fragile -->
                <xsl:apply-templates select="(query//*[@fields])[1]/*" mode="spelling"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates select="query" mode="spelling"/>
            </xsl:otherwise>
        </xsl:choose>
        
    </xsl:template>
    
    <xsl:template match="term" mode="spelling">
        <span class="search-term">
            <xsl:value-of select="cdl:fix-terms(., //spelling)"/>
        </span>
        <xsl:text> </xsl:text>
    </xsl:template>
  
    <xsl:template match="phrase" mode="spelling">
        <xsl:text>&quot;</xsl:text>
        <span class="search-term">
          <xsl:value-of select="cdl:fix-terms(term, //spelling)"/>
        </span>
        <xsl:text>&quot;</xsl:text>
    </xsl:template>
    
    <xsl:template match="exact" mode="spelling">
        <xsl:text>'</xsl:text>
        <span class="search-term"><xsl:value-of select="cdl:fix-terms(term, //spelling)"/></span>
        <xsl:text>'</xsl:text>
    </xsl:template>
    
    <xsl:template match="upper" mode="spelling">
        <xsl:if test="../lower != .">
            <xsl:text> - </xsl:text>
            <xsl:apply-templates mode="spelling"/>
        </xsl:if>
    </xsl:template>
</xsl:stylesheet>
