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

<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

   <xsl:output method="xhtml" indent="no" 
      encoding="UTF-8" media-type="text/html; charset=UTF-8" 
      doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN" 
      doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd" 
      exclude-result-prefixes="#all"
      omit-xml-declaration="yes"/>

<!-- =====================================================================
    
    When an error occurs (either authorization or an internal error),
    a stylesheet is used to produce a nicely formatted page for the
    requester. This tag specifies the path to the stylesheet, relative
    to the servlet base directory.

    The stylesheet receives one of the following mini-documents as input:

        <InvalidDocument>
            <docId>document identifier requested</docId>
        </InvalidDocument>

    or

        <NoPermission>
            <docId>document identifier requested</docId>
            <ipAddr>requester's IP address</ipAddr>
        </NoIPPermission>
        
    or

        <some general exception>
            <docId>document identifier requested</docId>
            <message>a descriptive error message (if any)</message>
            <stackTrace>HTML-formatted Java stack trace</stackTrace>
        </some general exception>

    For convenience, all of this information is also made available in XSLT
    parameters:

        $docId        Identifier of requested document

        $exception    Name of the exception that occurred - "InvalidDocument",
                      "NoPermission", or other names.

        $message      More descriptive details about what occurred

        $ipAddr       Requestor's IP address (if applicable)

        $stackTrace   JAVA stack trace for non-standard exceptions.

======================================================================= -->


<!-- ====================================================================== -->
<!-- Parameters                                                             -->
<!-- ====================================================================== -->

<xsl:param name="docId"/>
<xsl:param name="exception"/>
<xsl:param name="message"/>
<xsl:param name="ipAddr" select="''"/>
<xsl:param name="stackTrace" select="''"/>

<!-- ====================================================================== -->
<!-- Root Template                                                          -->
<!-- ====================================================================== -->

<!-- ======================================================================
    For this sample error generation stylesheet, we use the input mini-
    document instead of the parameters.
-->

<xsl:variable name="subDir" select="substring($docId, 9, 2)"/>

<xsl:variable name="sourceDir" select="concat('data/', $subDir, '/', $docId, '/')"/>

<xsl:variable name="METS" select="document(concat('../../', $sourceDir, $docId, '.mets.xml'))"/>


<xsl:variable name="project-name">
   <xsl:text>XTF</xsl:text>
</xsl:variable>

<xsl:variable name="reason">
  <xsl:choose>
    <xsl:when test="//InvalidDocument">
      <xsl:text>dynaXML Error: Invalid Document</xsl:text>
    </xsl:when>
    <xsl:when test="//NoPermission">
      <xsl:text>dynaXML Error: Permission Denied</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>dynaXML Error: Servlet Error</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
</xsl:variable>

<xsl:template match="/">

  <html>
    <head>
      <title><xsl:value-of select="$reason"/></title>
      <link type="text/css" rel="stylesheet" href="css/default/content.css"/>
      <link rel="shortcut icon" href="icons/default/favicon.ico" />

    </head>

    <body>

      <div class="content">
        <xsl:choose>
          <xsl:when test="InvalidDocument 
                          or NoPermission">
            <xsl:apply-templates/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:for-each select="*">
              <xsl:call-template name="GeneralError"/>
            </xsl:for-each>
          </xsl:otherwise>
        </xsl:choose>

      </div>

     </body>
  </html>

</xsl:template>


<!-- In the case of an InvalidDocument exception, the only relevant info is the document identifier. -->
<xsl:template match="InvalidDocument">
  <h1>Document Not Found</h1>
  <p>Document <b><xsl:value-of select="$docId"/></b> is not available. Please check that you have typed the address correctly or that the referring page does not have an error in its link.</p>
  <p>Document ID: <b><xsl:value-of select="$docId"/></b></p>
</xsl:template>


<!-- For a NoPermission exception, the document identifier and IP address are relevant. -->
<xsl:template match="NoPermission">
  <h1>Access Restricted</h1>
  <p>Document ID: <b><xsl:value-of select="$docId"/></b> and  IP Address:<b> <xsl:apply-templates select="ipAddr"/></b>.</p>
</xsl:template>


<!-- For all other exceptions, output all the information we can. -->
<xsl:template name="GeneralError">
  <h1>Servlet Error: <xsl:value-of select="name()"/></h1>
  <h3>An unexpected servlet error has occurred.</h3>
  <xsl:apply-templates/>
</xsl:template>


<!-- If a message was passed in, format it. -->
<xsl:template match="message">
  <p><b>Message:</b><br/><br/><span style="font-family: Courier"><xsl:apply-templates/></span></p>
</xsl:template>


<!-- If stack trace was specified, format it. Note that it already contains internal <br/> tags to separate lines. -->
<xsl:template match="stackTrace">
  <p><b>Stack Trace:</b><br/><br/><xsl:apply-templates/></p>
</xsl:template>


<!-- Pass <br> tags through untouched. -->
<xsl:template match="br">
    <br/>
</xsl:template>


</xsl:stylesheet>

