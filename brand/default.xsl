<!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
<!-- Branding stylesheet                                                    -->
<!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->

<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml"
            indent="no"
            encoding="utf-8"/>

<!-- ====================================================================== -->
<!-- Root Template                                                          -->
<!-- ====================================================================== -->

<xsl:template match="/">

    <!-- 
        Each element placed below will be passed as a parameter to the main
        display stylesheet. These can be for anything you want to control
        to make a consistent "brand" or look and feel.
    -->

    <testvar>This is a test</testvar>

</xsl:template>

</xsl:stylesheet>
