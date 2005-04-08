<!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
<!-- Document lookup stylesheet                                             -->
<!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->

<!--
Copyright (c) 2005, Regents of the University of California
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
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:dc="http://purl.org/dc/elements/1.1/"
  xmlns:mets="http://www.loc.gov/METS/"
  xmlns:mods="http://www.loc.gov/mods/"
  xmlns:xlink="http://www.w3.org/TR/xlink"
  xmlns:parse="http://cdlib.org/parse"
  exclude-result-prefixes="xsl dc mets mods xlink parse">
  
  <!-- Templates used for parsing text queries -->               
  <xsl:import href="../crossQuery/queryParser.xsl"/>
  
  <xsl:output method="xml"
    indent="yes"
    encoding="utf-8"/>
  
  <!--
  When a request is made for a particular document (by specifying docId),
  dynaXML will run the document lookup stylesheet to obtain the source path, 
  stylesheet, branding profile, and authorization info for that document.
  
  Specifically, the stylesheet receives a XSLT parameter $docId 
  containing the document ID as specified in the request URL.
  
  It should output an XML document, which must always contain "style",
  "source", and one or more "auth" tags.
  
  Note that all filesystem paths are relative to the servlet base directory.
  -->
  
  <!-- ====================================================================== -->
  <!-- Parameters                                                             -->
  <!-- ====================================================================== -->
  
  <xsl:param name="docId"/>
  <xsl:param name="query" select="'0'"/>
  <xsl:param name="query-join" select="'0'"/>
  <xsl:param name="query-exclude" select="'0'"/>
  <xsl:param name="sectionType" select="'0'"/>
  
  <!-- ====================================================================== -->
  <!-- Variables                                                              -->
  <!-- ====================================================================== -->
  
  <xsl:variable name="subDir" select="substring($docId, 9, 2)"/>
  <xsl:variable name="sourceDir" select="concat('data/', $subDir, '/', $docId, '/')"/>
  
  <!-- ====================================================================== -->
  <!-- Root Template                                                          -->
  <!-- ====================================================================== -->
  
  <xsl:template match="/">
    
    <!-- ==================================================================
    The "style" tag specifies a filesystem path, relative to the servlet
    base directory, to a stylesheet that translates an XML source document
    into an HTML page
    -->
    
    <style path="style/dynaXML/docFormatter/default/docFormatter.xsl"/>
    
    <!-- ==================================================================
    The "source" tag specifies a filesystem path (relative to the servlet
    base directory), or an HTTP URL. The referenced XML document is
    parsed and fed into the display stylesheet.
    -->
    
    <source path="{concat($sourceDir, $docId, '.xml')}"/>
    
    <!-- ==================================================================
    The optional "brand" tag specifies a filesystem path (relative to the
    servlet base directory) that is a simple stylesheet. It should produce
    any custom brand-related parameters that need be passed to the display
    stylesheet. The output should be of the form:
    
    <name>value</name>
    <name>value</name>
    ...
    
    This can be quite useful for instance if you want to have two or more
    color schemes for different sets of documents.
    -->

    <!-- ==================================================================
    For speed, a persistent or "lazy" version of each document is
    stored in the index. The servlet needs to be able to get back to
    that place to fetch the persistent version.
    -->
    
    <index configPath="conf/textIndexer.conf" name="default"/>
    
    <!-- ==================================================================
    If the user specifies a text query, it needs to be parsed into the
    same format as required by CrossQuery. Uses templates imported 
    from queryParser.xsl to do most of the work... see detailed 
    comments there for more information.
    -->
    <xsl:if test="$query != '0' and $query != ''">
      
      <xsl:variable name="query" select="/parameters/param[@name='query']"/>
      <xsl:variable name="sectionType" select="/parameters/param[@name='sectionType']"/>
      
      <query indexPath="index" termLimit="1000" workLimit="500000">
        <xsl:apply-templates select="$query"/>
      </query>
    </xsl:if>
    
    
    <!-- ==================================================================
    Finally, one or more "auth" sections must be included. These sections 
    will be processed in the order produced until one of them matches.  If 
    none match, access will be denied.
    
    To grant or deny access to everyone, output an auth tag with type 
    "all", like this:
    
    <auth access = ["allow" | "deny"]
    type   = "all"/>
    
    To allow or deny access based on the IP address of the requestor, use 
    the "IP" type like this:
    
    <auth access = ["allow" | "deny"]
    type   = "IP" 
    list   = [URL or file path to a list of IP addresses] />
    
    To allow access to be controlled by an LDAP database, output an auth 
    tag with type "LDAP". There are typically three ways an LDAP database 
    can be used for authentication:
    
    (1) Anonymous bind to the LDAP server, then look up user's record 
    in the database and verify the password. Supply these attributes:
    - server
    - realm
    - queryName  (containing "%" where user name should be placed)
    - matchField (name of field containing password)
    - matchValue (containing "%" where password should be placed)
    
    (2) Bind as administrator to LDAP server using admin password, then
    look up user's record in database and verify their password.
    Supply these attributes:
    - server
    - realm
    - bindName     (the DN of the administrator)
    - bindPassword (the password of the administrator)
    - queryName    (containing "%" where user name should be placed)
    - matchField   (name of field containing password)
    - matchValue   (containing "%" where password should be placed)
    
    (3) Bind to LDAP server using the user's name and password.
    - server
    - realm
    - bindName     (the user's DN, put "%" where user name should go)
    - bindPassword (the string "%" to supply the user's password)
    - queryName    (record to query, often the same as bindName)
    
    <auth access       = "allow"
    type         = "LDAP"
    server       = [URL of LDAP server; begins with "ldap://"]
    realm        = [Description of document collection, shown to
    user when requesting name and password]
    bindName     = [Optional: DN for LDAP bind; string may contain
    "%" which will be replaced by user name]
    bindPassword = [Optional: Password for bind, or use "%" for
    the password entered by the requestor]
    queryName    = [DN to query; string may contain "%" which will
    be replaced by user name]
    matchField   = [Optional: field name to look for]
    matchValue   = [Optional: value to match, "%" to put in the
    user's password]/>
    
    To allow access to be controlled by an external web page (e.g. 
    form-based login, .htpasswd, Shibboleth, etc) output an "external" 
    type auth tag. The external page will receive the following URL 
    parameters:
    
    - returnto    (the URL to return to once authentication is accepted)
    - nonce       (a nonsense string specific to this attempt)
    
    and it must return the following parameters to the "returnto" URL:
    
    - nonce       (the same nonsense string)
    - hash        (hex string MD5 hash of the concatenation of 
    "nonce:key" where key is the shared secret key string)
    
    <auth access = "allow"
    type   = "external" 
    key    = [shared secret key string]
    url    = [URL of the external authentication web page]/>
    -->
    
    <!-- CDL-specific: Since we don't read METS yet, each directory has an 
    authInfo.xml file which has authentication directives.
    
    Note that the path here, since we're reading the file directly, must
    be relative to this stylesheet, *not* to the servlet base directory.
    Hence the "../" below.
    
    If not found, allow access. This is for testing only!
    -->
    
    <auth access="allow" type="all"/>
    
  </xsl:template>
  
</xsl:stylesheet>

