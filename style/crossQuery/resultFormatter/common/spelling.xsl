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
    
    <xsl:variable name="newURL" select="cdl:replace-misspellings($baseURL, $spelling/*)"/>
    <br/>
    <b>Did you mean to search for
    <a href="{$newURL}">
      <xsl:apply-templates select="query" mode="spelling"/>
    </a>
    <xsl:text>?</xsl:text> </b>
  </xsl:template>
  
  <!-- 
    Scan the URL and replace possibly misspelled words with suggestions
    from the spelling correction engine.
  -->
  <xsl:function name="cdl:replace-misspellings">
    <xsl:param name="baseURL"/>
    <xsl:param name="suggestions"/>
    
    <xsl:choose>
      <xsl:when test="$suggestions">
        <xsl:variable name="sugg" select="$suggestions[1]"/>
        <xsl:variable name="remainder" select="cdl:replace-misspellings($baseURL, $suggestions[position() > 1])"/>
        <xsl:variable name="fields" select="concat($sugg/@fields, ',keyword')"/>

        <!-- Replace the term in the proper field(s) from the URL. Make sure it has word
             boundaries on either side of it. -->
        <xsl:variable name="matchPattern" 
                      select="concat('(\W(', replace($fields, ',', '|'), ')=([^=;&amp;]+\W)?)', 
                                     $sugg/@originalTerm,
                                     '(\W|$)')"/>
        <xsl:variable name="changed" 
                      select="replace($remainder, $matchPattern, 
                                      concat('$1', $sugg/@suggestedTerm, '$4'), 'i')"/>
        <xsl:value-of select="$changed"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$baseURL"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:function>

  <!-- 
    Scan a list of terms and replace possibly misspelled words with suggestions
    from the spelling correction engine.
  -->
  <xsl:function name="cdl:fix-terms">
    <xsl:param name="terms"/>
    <xsl:param name="spelling"/>
  
    <!-- Get the first term -->
    <xsl:variable name="term" select="$terms[1]"/>
    <xsl:if test="$term">

      <!-- Figure out what field(s) to apply to. -->
      <xsl:variable name="rawFields">
        <xsl:choose>
          <xsl:when test="$term/ancestor-or-self::*[@fields]">
            <xsl:value-of select="$term/ancestor-or-self::*[@fields][1]/@fields"/>
          </xsl:when>
          <xsl:when test="$term/ancestor-or-self::*[@field]">
            <xsl:value-of select="$term/ancestor-or-self::*[@field][1]/@field"/>
          </xsl:when>
        </xsl:choose>
      </xsl:variable>
      
      <!-- Make the field list into a handy regular expression that matches any of the fields -->
      <xsl:variable name="fieldsRegex" select="replace($rawFields, '[\s,;]+', '|')"/>

      <!-- See if there's a replacement for this term -->
      <xsl:variable name="replacement" select="$spelling/suggestion[matches(@fields, $fieldsRegex) 
                                           and (@originalTerm = string($term))]"/>
      <xsl:choose>
        <xsl:when test="$replacement">
          <xsl:value-of select="$replacement/@suggestedTerm"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="string($term)"/>
        </xsl:otherwise>
      </xsl:choose>
      
      <!-- Process the remaining terms in the list -->
      <xsl:if test="count($terms) > 1">
        <xsl:text> </xsl:text>
        <xsl:value-of select="cdl:fix-terms($terms[position() > 1], $spelling)"/>
      </xsl:if>
    </xsl:if>
  </xsl:function>

<!-- ====================================================================== -->
<!-- Format Terms with Spelling Corrections                                 -->
<!-- ====================================================================== -->
    
    <xsl:template match="term" mode="spelling">
        <span class="search-term">
            <xsl:value-of select="cdl:fix-terms(., //spelling)"/>
        </span>
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
