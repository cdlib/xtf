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
                    <h2>Set Language |<xsl:value-of select="$lang"/>|</h2>
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