<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:xtf="http://cdlib.org/xtf" 
    xmlns:session="java:org.cdlib.xtf.xslt.Session"
    xmlns:editURL="http://cdlib.org/xtf/editURL"
    xmlns="http://www.w3.org/1999/xhtml"
    extension-element-prefixes="session"
    exclude-result-prefixes="#all"
    version="2.0">
    
    <xsl:param name="lang" select="if (normalize-space(session:getData('lang')) != '') then session:getData('lang') else 'en'"/>
    <xsl:param name="transTable" select="document(concat('g10n/translation_', $lang, '.xml'))"/>
    
    <!-- ====================================================================== -->
    <!-- URL Encoding                                                           -->
    <!-- ====================================================================== -->
    
    <xsl:function name="xtf:urlEncode">
        <xsl:param name="string"/>
        <xsl:variable name="string1" select="replace(replace($string,'^ +',''),' +$','')"/>
        <xsl:variable name="string2" select="replace($string1,'%','%25')"/>
        <xsl:variable name="string3" select="replace($string2,' +','%20')"/>
        <xsl:variable name="string4" select="replace($string3,'#','%23')"/>
        <xsl:variable name="string5" select="replace($string4,'&amp;','%26')"/>
        <xsl:variable name="string6" select="replace($string5,'\+','%2B')"/>
        <xsl:variable name="string7" select="replace($string6,'/','%2F')"/>
        <xsl:variable name="string8" select="replace($string7,'&lt;','%3C')"/>
        <xsl:variable name="string9" select="replace($string8,'=','%3D')"/>
        <xsl:variable name="string10" select="replace($string9,'>','%3E')"/>
        <xsl:variable name="string11" select="replace($string10,'\?','%3F')"/>
        <xsl:variable name="string12" select="replace($string11,':','%3A')"/>
        <xsl:value-of select="$string12"/>
    </xsl:function>
    
    
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
    
    
    <!-- ====================================================================== -->
    <!-- Localization Templates                                                 -->
    <!-- ====================================================================== -->
    
    <xsl:template name="translate">
        <xsl:param name="resultTree"/>
        <xsl:choose>
            <xsl:when test="$lang='en'">
                <xsl:copy-of select="$resultTree"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates select="$resultTree" mode="translate"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template match="*" mode="translate">
        <xsl:element name="{name(.)}">
            <xsl:copy-of select="@*"/>
            <xsl:apply-templates mode="translate"/>
        </xsl:element>
    </xsl:template>
    
    <xsl:template match="*:input[matches(@*:type,'submit|reset')]" mode="translate">
        <xsl:variable name="string" select="@value"/>
        <xsl:element name="{name(.)}">
            <xsl:copy-of select="@*[not(name()='value')]"/>
            <xsl:if test="$string">
                <xsl:attribute name="value">
                    <xsl:call-template name="tString">
                        <xsl:with-param name="string" select="$string"/>
                    </xsl:call-template>
                </xsl:attribute>
            </xsl:if>
            <xsl:apply-templates mode="translate"/>
        </xsl:element>
    </xsl:template>
    
    <xsl:template match="text()" mode="translate">
        <xsl:variable name="string" select="."/>
        <!-- might need a switch here that checks if parent is a protected div/span element -->
        <xsl:call-template name="tString">
            <xsl:with-param name="string" select="$string"/>
        </xsl:call-template>
    </xsl:template>
    
    <xsl:template name="tString">
        <xsl:param name="string"/>
        <xsl:choose>
            <!-- full match -->
            <xsl:when test="$transTable/transTable/trans[from/text()=$string]">
                <xsl:value-of select="$transTable/transTable/trans[from/text()=$string]/to"/>
            </xsl:when>
            <!-- partial match -->
            <xsl:when test="$transTable/transTable/trans/from[contains($string,.)]">
                <xsl:value-of select="replace($string,
                    $transTable/transTable/trans[contains($string,from)][1]/from,
                    $transTable/transTable/trans[contains($string,from)][1]/to)"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$string"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template name="getLang">
        <!--<xsl:variable name="lang" select="session:getData('lang')"/>-->
        <html xml:lang="en" lang="en">
            <head>
                <title>XTF: Set Language</title>
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
                <xsl:copy-of select="$brand.links"/>
            </head>
            <body>
                <xsl:copy-of select="$brand.header"/>
                <div>
                    <h2>Set Language</h2>
                    <form action="/xtf/search" method="get">
                        <input type="radio" name="lang" value="en">
                            <xsl:if test="$lang='en'"><xsl:attribute name="checked" select="'checked'"/></xsl:if>
                            <xsl:text>Engish</xsl:text>
                        </input>&#160;&#160;
                        <input type="radio" name="lang" value="sp">
                            <xsl:if test="$lang='sp'"><xsl:attribute name="checked" select="'checked'"/></xsl:if>
                            <xsl:text>Spanish</xsl:text>
                        </input>&#160;
                        <input type="radio" name="lang" value="fr">
                            <xsl:if test="$lang='fr'"><xsl:attribute name="checked" select="'checked'"/></xsl:if>
                            <xsl:text>French</xsl:text>
                        </input>&#160;
                        <input type="radio" name="lang" value="it">
                            <xsl:if test="$lang='it'"><xsl:attribute name="checked" select="'checked'"/></xsl:if>
                            <xsl:text>Italian</xsl:text>
                        </input>&#160;
                        <input type="radio" name="lang" value="de">
                            <xsl:if test="$lang='de'"><xsl:attribute name="checked" select="'checked'"/></xsl:if>
                            <xsl:text>German</xsl:text>
                        </input>&#160;
                        <input type="submit" value="submit"/>
                        <input type="hidden" name="smode" value="setLang"/>
                    </form>
                </div>
            </body>
        </html>
    </xsl:template>
    
    <xsl:template name="setLang">
        <xsl:value-of select="session:setData('lang', $lang)"/>
        <html xml:lang="en" lang="en">
            <head>
                <title>XTF: Success</title>
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
                <xsl:copy-of select="$brand.links"/>
            </head>
            <body onload="autoCloseTimer = setTimeout('window.close()', 1000); opener.top.location.reload();">
                <!-- How are we going to reload previous window? -->
                <xsl:copy-of select="$brand.header"/>
                <h1>Language Set</h1>
                <b>
                    <xsl:text>Your language has been set to </xsl:text>
                    <xsl:value-of select="if ($lang='sp') then 'Spanish' 
                        else if ($lang='fr') then 'French' 
                        else if ($lang='it') then 'Italian' 
                        else if ($lang='de') then 'German' 
                        else 'English'"/>
                </b>
            </body>
        </html>
    </xsl:template>
    
</xsl:stylesheet>