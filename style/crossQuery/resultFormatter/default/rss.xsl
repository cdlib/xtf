<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xtf="http://cdlib.org/xtf" exclude-result-prefixes="#all" version="2.0">

    <xsl:output method="xml" name="rss-xml" encoding="UTF-8" media-type="text/xml" indent="yes" exclude-result-prefixes="#all"/>

    <!-- The "entity-ignore" param is used to construct the landing page URL for ORUs and journals. -->
    <xsl:param name="entity-ignore"/>

    <xsl:template match="crossQueryResult" mode="rss" exclude-result-prefixes="#all">
        <xsl:result-document format="rss-xml">
            <rss version="2.0" xmlns:atom="http://www.w3.org/2005/Atom">
                <channel>

                    <!-- RSS 2.0: A URL that points to the documentation for the format used in the RSS file. -->
                    <docs>http://www.rssboard.org/rss-specification</docs>

                    <!-- RSS 2.0: An RSS feed can identify its own URL using the atom:link element within a channel. -->
                    <!-- There's no means to do this with RSS elements defined in the specification. Identifying -->
                    <!-- a feed's URL within the feed makes it more portable, self-contained, and easier to cache. -->
                    <!-- For these reasons, a feed should contain an atom:link used for this purpose. -->
                    <atom:link rel="self" type="application/rss+xml">
                        <xsl:attribute name="href">
                            <xsl:value-of select="concat($xtfURL,'search?')"/>
                            <xsl:value-of select="replace(replace($queryString,' ','%20'),'&quot;','%22')"/>
                        </xsl:attribute>
                    </atom:link>

                    <!-- RSS 2.0: How often RSS readers should check for new content (specified in minutes). -->
                    <!-- The ttl element does not prevent an RSS reader from hitting your feed more frequently, -->
                    <!-- but a "polite" reader should use a cached version of your feed for the specified -->
                    <!-- length of time before it refreshes again. -->
                    <ttl>720</ttl>

                    <!-- RSS 2.0: The name of the channel. It's how people refer to your service. -->
                    <!-- If you have an HTML website that contains the same information as your RSS file, -->
                    <!-- the title of your channel should be the same as the title of your website. -->
                    <title>
                        <xsl:text>XTF Search Results (</xsl:text>
                        <xsl:value-of select="replace(replace($queryString,';facet-',';'),';entity-ignore=[A-Za-z0-9_\-]*|;docsPerPage=\d*|;rmode=[A-Za-z0-9_\-]*|;sort=[A-Za-z0-9_\-]*|;discipline=\d*','')"/>
                        <xsl:text>)</xsl:text>
                    </title>

                    <!-- RSS 2.0: The URL to the HTML website corresponding to the channel. -->
                    <link>
                        <xsl:value-of select="concat($xtfURL,'search?')"/>
                        <xsl:value-of select="replace(xtf:urlEncode(replace($queryString,';entity-ignore=[A-Za-z0-9_\-]*|;docsPerPage=\d*|;rmode=[A-Za-z0-9_\-]*|;sort=[A-Za-z0-9_\-]*','')),'&quot;','%22')"/>
                    </link>

                    <!-- RSS 2.0: Phrase or sentence describing the channel. -->
                    <description>
                        <xsl:text>Results for your query: </xsl:text>
                        <xsl:value-of select="replace($queryString,';entity-ignore=[A-Za-z0-9_\-]*|;docsPerPage=\d*|;rmode=[A-Za-z0-9_\-]*|;sort=[A-Za-z0-9_\-]*','')"/>
                    </description>

                    <!-- RSS 2.0: The publication date for the content in the channel. -->
                    <!-- Although the specification for RSS 2.0 says this element in channel is optional, -->
                    <!-- you really should provide a pubDate whenever possible. This element tells -->
                    <!-- RSS readers the date and time when the last item was published in your feed. -->
                    <!-- In practice, this date should correspond to the date of the newest item in your feed. -->
                    <pubDate>
                        <xsl:value-of select="xtf:dateTimeRFC822(docHit[1]/meta/dateStamp)"/>
                    </pubDate>

                    <!-- process individual results -->
                    <xsl:apply-templates select="docHit" mode="rss"/>

                </channel>
            </rss>
        </xsl:result-document>
    </xsl:template>

    <xsl:template match="docHit" mode="rss" exclude-result-prefixes="#all">

        <!-- path inside data directory to .meta.xml file (e.g. erep:05/g6/92/8h/qt05g6928h/meta/qt05g6928h.meta.xml)-->
        <xsl:variable name="path" select="@path"/>

        <!-- Creating URL for dynaxml or pdf object view -->
        <xsl:variable name="href">
            <xsl:choose>
                <xsl:when test="matches(meta/display, 'dynaxml')">
                    <xsl:value-of select="$xtfURL"/>
                    <xsl:call-template name="dynaxml.url">
                        <xsl:with-param name="path" select="$path"/>
                    </xsl:call-template>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:call-template name="rawDisplay.url">
                        <xsl:with-param name="path" select="$path"/>
                    </xsl:call-template>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <item>

            <!-- RSS 2.0: The title of the item. -->
            <title>
                <xsl:apply-templates select="meta/title[1]" mode="rmHTML"/>
                <xsl:text>. </xsl:text>
                <!-- adding authors to title, because rss author element apparently is for author email only -->
                <xsl:for-each select="meta/creator">
                    <xsl:value-of select="."/>
                    <xsl:if test="not(position() = last())">
                        <xsl:text>, </xsl:text>
                    </xsl:if>
                </xsl:for-each>
            </title>

            <!-- RSS 2.0: The URL of the item. -->
            <link>
                <xsl:value-of select="replace($href,';.*','')"/>
            </link>

            <!-- RSS 2.0: The item synopsis. -->
            <description>
                <xsl:variable name="desc">
                    <xsl:apply-templates select="meta/description[1]" mode="rmHTML"/>
                </xsl:variable>
                <xsl:choose>
                    <xsl:when test="string-length($desc) gt 1024">
                        <xsl:value-of select="substring($desc,1,1024)"/>
                        <xsl:text>...</xsl:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="$desc"/>
                    </xsl:otherwise>
                </xsl:choose>
            </description>

            <!-- RSS 2.0: A string that uniquely identifies the item. -->
            <!-- Because we are claiming that it is a permaLink, we need to change this to the ARK format. -->
            <!-- *** Waiting for ARK-format URLs to be supported... *** -->
            <guid isPermaLink="true">
                <xsl:value-of select="replace($href,';.*','')"/>
            </guid>

            <!-- RSS 2.0: Indicates when the item was published. -->
            <!-- publication date, should be in RFC-822 (e.g. Sun, 19 May 2002 15:21:36 GMT) -->
            <!-- meta/dateStamp is in YEAR-MONTH-DAY -->
            <pubDate>
                <xsl:value-of select="xtf:dateTimeRFC822(meta/date)"/>
            </pubDate>

        </item>
    </xsl:template>
    
    <!-- =================================== -->
    <!-- Remove all HTML                     -->
    <!-- =================================== -->
    
    <!-- remove html tags -->
    <xsl:template match="text()" mode="rmHTML">
        <xsl:value-of select="replace(string(.), '&lt;[^>]+>', '')"/>
    </xsl:template>

    <!-- ========= -->
    <!-- Functions -->
    <!-- ========= -->

    <xsl:function name="xtf:dateTimeRFC822">
    <!-- Generates RFC822-compliant date from YYYY-MM-DD date -->
        <xsl:param name="docDate"/>
        <xsl:variable name="docDateYear"  select="replace(tokenize($docDate, '-')[1], '\s+', '')"/>
        <xsl:variable name="docDateMonth" select="replace(tokenize($docDate, '-')[2], '\s+', '')"/>
        <xsl:variable name="docDateDay"   select="replace(tokenize($docDate, '-')[3], '\s+', '')"/>
        <xsl:variable name="docDateYear">
            <xsl:choose>
                <xsl:when test="(string(number($docDateYear))='NaN') or
                                (number($docDateYear) lt 1800) or
                                (number($docDateYear) gt 3000)">
                    <xsl:value-of select="1970"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="xs:integer($docDateYear)"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="docDateMonth">
            <xsl:choose>
                <xsl:when test="(string(number($docDateMonth))='NaN') or
                                (number($docDateMonth) lt 1) or
                                (number($docDateMonth) gt 12)">
                    <xsl:value-of select="format-number(xs:integer(1),'00')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="format-number(xs:integer($docDateMonth),'00')"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="docDateDay">
            <xsl:choose>
                <xsl:when test="(string(number($docDateDay))='NaN') or
                                (number($docDateDay) lt 1) or
                                (number($docDateDay) gt 31)">
                    <xsl:value-of select="format-number(xs:integer(1),'00')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="format-number(xs:integer($docDateDay),'00')"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="docDateTime" select="dateTime(xs:date(concat($docDateYear, '-', $docDateMonth, '-', $docDateDay)), xs:time('12:00:00-00:00'))"/>
        <xsl:value-of select="format-dateTime($docDateTime, '[FNn,3-3], [D01] [MNn,3-3] [Y] [H01]:[m01]:[s01] [ZN]')"/>
    </xsl:function>
             
</xsl:stylesheet>

