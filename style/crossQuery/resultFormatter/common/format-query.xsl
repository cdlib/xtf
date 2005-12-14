<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
    
    <xsl:param name="keyword"/>
    <xsl:param name="fieldList"/>
    
<!-- ====================================================================== -->
<!-- Format Query for Display                                               -->
<!-- ====================================================================== -->
    
    <xsl:template name="format-query">
        
        <xsl:param name="query"/>
        
        <xsl:choose>
            <xsl:when test="$keyword">
                <!-- probably very fragile -->
                <xsl:apply-templates select="(query//*[@field])[1]/*" mode="query"/>
                <xsl:choose>
                    <!-- if they did not search all fields, tell them which are in the fieldList -->
                    <xsl:when test="$fieldList">
                        <xsl:text> in </xsl:text>
                        <span class="search-type">
                            <xsl:value-of select="replace($fieldList,'\s',', ')"/>
                        </span>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:text> in </xsl:text>
                        <span class="search-type"> keywords</span>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates select="query" mode="query"/>
            </xsl:otherwise>
        </xsl:choose>
        
    </xsl:template>
    
    <xsl:template match="and | or | exact | near | range | not" mode="query">
        <xsl:choose>
            <xsl:when test="@field">    
                <xsl:if test="not(position() = 2)">
                    <xsl:value-of select="name(..)"/><xsl:text> </xsl:text>
                </xsl:if>
                <xsl:apply-templates mode="query"/>
                <xsl:text> in </xsl:text>
                <span class="search-type">
                    <xsl:choose>
                        <xsl:when test="@field = 'text'">
                            <xsl:text> the full text </xsl:text>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="@field"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </span>
            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates mode="query"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template match="term" mode="query">
        <xsl:if test="preceding-sibling::term and (. != $keyword)">
            <xsl:value-of select="name(..)"/>
            <xsl:text> </xsl:text>
        </xsl:if>
        <span class="search-term">
            <xsl:value-of select="."/>
        </span>
        <xsl:text> </xsl:text>
    </xsl:template>
    
    <xsl:template match="phrase" mode="query">
        <xsl:text>&quot;</xsl:text>
        <span class="search-term">
            <xsl:value-of select="term"/>
        </span>
        <xsl:text>&quot;</xsl:text>
    </xsl:template>
    
    <xsl:template match="exact" mode="query">
        <xsl:text>'</xsl:text>
        <span class="search-term"><xsl:value-of select="term"/></span>
        <xsl:text>'</xsl:text>
        <xsl:if test="@field">
            <xsl:text> in </xsl:text>
            <span class="search-type"><xsl:value-of select="@field"/></span>
        </xsl:if>
    </xsl:template>
    
    <xsl:template match="upper" mode="query">
        <xsl:if test="../lower != .">
            <xsl:text> - </xsl:text>
            <xsl:apply-templates mode="query"/>
        </xsl:if>
    </xsl:template>
    
</xsl:stylesheet>
