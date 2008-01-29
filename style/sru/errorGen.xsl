<!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
<!-- Error page generation stylesheet                                       -->
<!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->

<!--
   Copyright (c) 2008, Regents of the University of California
   All rights reserved.
 
   Redistribution and use in source and binary forms, with or without 
   modification, are permitted provided that the following conditions are 
   met:

   - Redistributions of source code must retain the above copyright notice, 
     this list of conditions and the following disclaimer.
   - Redistributions in binary form must reproduce the above copyright 
     notice, this list of conditions and the following disclaimer in the 
     documentation and/or other materials provided with the distribution.
   - Neither the name of the University of California nor the names of its
     contributors may be used to endorse or promote products derived from 
     this software without specific prior written permission.

   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
   AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
   IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
   ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
   LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
   CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
   SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
   INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
   CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
   ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
   POSSIBILITY OF SUCH DAMAGE.
-->

<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:srw="http://www.loc.gov/zing/srw/" 
                xmlns:diag="http://www.loc.gov/zing/srw/diagnostic/">

<xsl:output method="xml"
            indent="yes"
            encoding="utf-8"
            media-type="text/xml"/>

<!-- =====================================================================
    
    When an error occurs (either authorization or an internal error),
    a stylesheet is used to produce a nicely formatted page for the
    requester. This tag specifies the path to the stylesheet, relative
    to the servlet base directory.

    The stylesheet receives one of the following mini-documents as input:

        <CQLParse>
          <message>error message</message>
        </CQLParse>

    or

        <TermLimit>
            <message>error message</message>
        </TermLimit>

    or

        <ExcessiveWork>
            <message>error message</message>
        </ExcessiveWork>

    or

        <some general exception>
            <message>a descriptive error message (if any)</message>
            <stackTrace>HTML-formatted Java stack trace</stackTrace>
        </some general exception>

    For convenience, all of this information is also made available in XSLT
    parameters:

        $exception    Name of the exception that occurred - "TermLimit",
                      "QueryFormat", or other names.

        $message      More descriptive details about what occurred

        $stackTrace   JAVA stack trace for non-standard exceptions.

======================================================================= -->


<!-- ====================================================================== -->
<!-- Parameters                                                             -->
<!-- ====================================================================== -->

<xsl:param name="exception"/>
<xsl:param name="message"/>
<xsl:param name="stackTrace" select="''"/>

<!-- ====================================================================== -->
<!-- Root Template                                                          -->
<!-- ====================================================================== -->

<!-- ======================================================================
    For this sample error generation stylesheet, we use the input mini-
    document instead of the parameters.
-->

<xsl:template match="/">

  <xsl:choose>
    <xsl:when test="CQLParse
                    or TermLimit
                    or ExcessiveWork">
      <xsl:apply-templates/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:for-each select="*">
        <xsl:call-template name="GeneralError"/>
      </xsl:for-each>
    </xsl:otherwise>
  </xsl:choose>

</xsl:template>


<!-- For a CQLParse exception, the message contains a further description. -->
<xsl:template match="CQLParse">
  <xsl:call-template name="error">
    <xsl:with-param name="uri" select="'info:srw/diagnostic/1/10'"/>
    <xsl:with-param name="message" select="'Query syntax error'"/>
    <xsl:with-param name="details" select="message"/>
  </xsl:call-template>
</xsl:template>


<!-- For a TermLimit exception, the message contains the first 50 matches. -->
<xsl:template match="TermLimit">
  <xsl:call-template name="error">
    <xsl:with-param name="uri" select="'info:srw/diagnostic/1/1'"/>
    <xsl:with-param name="message" select="'The query matched too many terms. Try using a smaller range, 
       eliminating wildcards, or making them more specific.'"/>
    <xsl:with-param name="details" select="message"/>
  </xsl:call-template>
</xsl:template>


<!-- For a ExcessiveWork exception, the message is not relevant. -->
<xsl:template match="ExcessiveWork">
  <xsl:call-template name="error">
    <xsl:with-param name="uri" select="'info:srw/diagnostic/1/1'"/>
    <xsl:with-param name="message" select="'The query took too much work to process. Try being more specific.'"/>
  </xsl:call-template>
</xsl:template>


<!-- For all other exceptions, output all the information we can. -->
<xsl:template name="GeneralError">
  <xsl:call-template name="error">
    <xsl:with-param name="uri" select="'info:srw/diagnostic/1/1'"/>
    <xsl:with-param name="message" select="concat('Unexpected servlet error: ', message)"/>
    <xsl:with-param name="details" select="stackTrace"/>
  </xsl:call-template>
</xsl:template>


<!-- ====================================================================== -->
<!-- Error Template                                                         -->
<!-- ====================================================================== -->

  <xsl:template name="error">
    <xsl:param name="uri"/>
    <xsl:param name="message"/>
    <xsl:param name="details" select="''"/>
    
    <srw:diagnostics>
      <diag:diagnostic>
        <diag:uri><xsl:value-of select="$uri"/></diag:uri>
        <diag:message><xsl:value-of select="$message"/></diag:message>
        <xsl:if test="not($details = '')">
          <diag:details><xsl:value-of select="$details"/></diag:details>
        </xsl:if>
      </diag:diagnostic>
    </srw:diagnostics>

  </xsl:template>
  
</xsl:stylesheet>

