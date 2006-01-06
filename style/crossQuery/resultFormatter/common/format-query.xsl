<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xtf="http://cdlib.org/xtf"
                version="2.0">
    
    <xsl:param name="keyword"/>
    <xsl:param name="fieldList"/>    
    <xsl:param name="newKeyword"/>
    
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
            <xsl:when test="$newKeyword">
                <!-- probably very fragile -->
                <xsl:apply-templates select="(query//*[@field])[1]/*" mode="query"/>
                <xsl:text> in </xsl:text>
                <span class="search-type"> keywords </span>
                <xsl:apply-templates select="query//*[@field != 'newKeyword']" mode="query"/>
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
                            <xsl:choose>
                                <xsl:when test="$style='melrec'">
                                    <xsl:call-template name="map-index">
                                        <xsl:with-param name="string" select="@field"/>
                                    </xsl:call-template>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:value-of select="@field"/>
                                </xsl:otherwise>
                            </xsl:choose>
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
    
    <!-- ====================================================================== -->
    <!-- Mapping Templates                                                      -->
    <!-- ====================================================================== -->
    
    <xsl:template name="map-index">
        <xsl:param name="string"/>
        <xsl:variable name="map" select="document('')//xtf:label-map"/>
        <xsl:value-of select="$map/xtf:regex/xtf:replace[preceding-sibling::xtf:find[1]/text()=$string]"/>
    </xsl:template>
    
    <xtf:label-map>
        <xtf:regex>
            <xtf:find>title-journal</xtf:find>
            <xtf:replace>journal title</xtf:replace>
        </xtf:regex>
        <xtf:regex>
            <xtf:find>title-main</xtf:find>
            <xtf:replace>title</xtf:replace>
        </xtf:regex>
        <xtf:regex>
            <xtf:find>title-series</xtf:find>
            <xtf:replace>series title</xtf:replace>
        </xtf:regex>
        <xtf:regex>
            <xtf:find>author</xtf:find>
            <xtf:replace>author</xtf:replace>
        </xtf:regex>
        <xtf:regex>
            <xtf:find>author-corporate</xtf:find>
            <xtf:replace>organization author</xtf:replace>
        </xtf:regex>
        <xtf:regex>
            <xtf:find>pub-place</xtf:find>
            <xtf:replace>place of publication</xtf:replace>
        </xtf:regex>
        <xtf:regex>
            <xtf:find>publisher</xtf:find>
            <xtf:replace>publisher</xtf:replace>
        </xtf:regex>
        <xtf:regex>
            <xtf:find>year</xtf:find>
            <xtf:replace>year</xtf:replace>
        </xtf:regex>
        <xtf:regex>
            <xtf:find>language</xtf:find>
            <xtf:replace>language</xtf:replace>
        </xtf:regex>
        <xtf:regex>
            <xtf:find>note</xtf:find>
            <xtf:replace>note</xtf:replace>
        </xtf:regex>
        <xtf:regex>
            <xtf:find>subject</xtf:find>
            <xtf:replace>subject</xtf:replace>
        </xtf:regex>
        <xtf:regex>
            <xtf:find>subject-geographic</xtf:find>
            <xtf:replace>georgraphic subject</xtf:replace>
        </xtf:regex>
        <xtf:regex>
            <xtf:find>subject-topic</xtf:find>
            <xtf:replace>topical subject</xtf:replace>
        </xtf:regex>
        <xtf:regex>
            <xtf:find>subject-temporal</xtf:find>
            <xtf:replace>temporal subject</xtf:replace>
        </xtf:regex>
        <xtf:regex>
            <xtf:find>callnum</xtf:find>
            <xtf:replace>call number</xtf:replace>
        </xtf:regex>
        <xtf:regex>
            <xtf:find>callnum-class</xtf:find>
            <xtf:replace>call number class</xtf:replace>
        </xtf:regex>
        <xtf:regex>
            <xtf:find>format</xtf:find>
            <xtf:replace>format</xtf:replace>
        </xtf:regex>
        <xtf:regex>
            <xtf:find>location</xtf:find>
            <xtf:replace>location</xtf:replace>
        </xtf:regex>
        <xtf:regex>
            <xtf:find>identifier-isbn</xtf:find>
            <xtf:replace>ISBN</xtf:replace>
        </xtf:regex>
        <xtf:regex>
            <xtf:find>identifier-issn</xtf:find>
            <xtf:replace>ISSN</xtf:replace>
        </xtf:regex>
    </xtf:label-map>    
</xsl:stylesheet>
