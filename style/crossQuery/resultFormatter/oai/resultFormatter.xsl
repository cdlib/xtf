<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
   xmlns:FileUtils="java:org.cdlib.xtf.xslt.FileUtils"
   extension-element-prefixes="FileUtils"
   exclude-result-prefixes="#all" 
   version="2.0">
   
   <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
   <!-- Query result formatter stylesheet                                      -->
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
   
   <!-- 
      This stylesheet implements a simple OAI response mechanism. 
      Be sure to change the values marked "CHANGE" below or you will produce 
      an invalid response.
   -->
   
   <!-- ====================================================================== -->
   <!-- Output                                                                 -->
   <!-- ====================================================================== -->
   
   <xsl:output method="xml" encoding="UTF-8" media-type="text/xml" indent="yes" exclude-result-prefixes="#all"/>
   
   <!-- ====================================================================== -->
   <!-- Global parameters (specified in the URL)                               -->
   <!-- ====================================================================== -->
   
   <!-- entire url -->
   <xsl:param name="http.URL"/>
   <!-- entire url, escaped as it came in -->
   <xsl:param name="http.rawURL"/>
   <!-- verb param -->
   <xsl:param name="verb"/>
   <!-- identifier param -->
   <xsl:param name="identifier"/>
   <!-- metadataPrefix param -->
   <xsl:param name="metadataPrefix"/>
   <!-- from param -->
   <xsl:param name="from"/>
   <!-- until param -->
   <xsl:param name="until"/>
   <!-- resumptionToken param -->
   <xsl:param name="resumptionToken"/>
   <!-- startDoc param -->
   <xsl:param name="startDoc" select="1"/>
   <!-- totalDocs param -->
   <xsl:param name="totalDocs" select="/crossQueryResult/@totalDocs"/>
   <!-- nextPage param -->
   <xsl:param name="nextPage" select="number($startDoc) + 20"/>
   <!-- cursor param -->
   <xsl:param name="cursor" select="$nextPage - 21"/>
   
   <!-- ====================================================================== -->
   <!-- Local parameters                                                       -->
   <!-- ====================================================================== -->
   
   <!-- Timestamp -->
   <xsl:variable name="responseDate">
      <xsl:value-of select="replace(replace(FileUtils:curDateTime('yyyy-MM-dd::HH:mm:ss'),'::','T'),'([0-9])$','$1Z')" xmlns:FileUtils="java:org.cdlib.xtf.xslt.FileUtils"/>
   </xsl:variable>
   
   <!-- error messages -->
   <xsl:variable name="idDoesNotExistMessage">
      <xsl:text>The value of the identifier argument is unknown or illegal in this repository.</xsl:text>
   </xsl:variable>
   <xsl:variable name="noRecordsMatchMessage">
      <xsl:text>The combination of the values of the from, until, set and metadataPrefix arguments results in an empty list.</xsl:text>
   </xsl:variable>
   
   <!-- resumption token for next page of ListIdentifiers or ListRecords -->
   <xsl:variable name="nextResumptionToken">
      <xsl:variable name="params" select="replace(replace(replace(replace(replace(replace($http.URL,
         'http(s?)://[^\?]+\?', ''),
         'verb=[^;&amp;]+', ''),
         'resumptionToken=[^;&amp;]+', ''),
         'startDoc=[^;&amp;]+', ''),
         '[;&amp;]+', '&amp;'),
         '^[;&amp;]+|[;&amp;]$', '')"/>
      <xsl:value-of select="encode-for-uri(concat($params,'&amp;startDoc=',$nextPage))"/>
   </xsl:variable>
      
   <!-- Some OAI harvesters double-escape our percent encoding, some need it -->
   <xsl:variable name="decodedResumpToken" xmlns:decoder="java:java.net.URLDecoder"
      select="if ($resumptionToken)
      then decoder:decode(decoder:decode($resumptionToken,'UTF-8'),'UTF-8') 
      else $resumptionToken" />
   
   
   <!-- ====================================================================== -->
   <!-- Root Template                                                          -->
   <!-- ====================================================================== -->
   
   <xsl:template match="/">
      <!-- select verb -->
      <xsl:choose>
         <xsl:when test="$verb='GetRecord'">
            <xsl:call-template name="GetRecord"/>
         </xsl:when>
         <xsl:when test="$verb='Identify'">
            <xsl:call-template name="Identify"/>
         </xsl:when>
         <xsl:when test="$verb='ListIdentifiers'">
            <xsl:call-template name="ListIdentifiers"/>
         </xsl:when>
         <xsl:when test="$verb='ListMetadataFormats'">
            <xsl:call-template name="ListMetadataFormats"/>
         </xsl:when>
         <xsl:when test="$verb='ListRecords'">
            <xsl:call-template name="ListRecords"/>
         </xsl:when>
         <xsl:when test="$verb='ListSets'">
            <xsl:call-template name="ListSets"/>
         </xsl:when>
      </xsl:choose>
   </xsl:template>
   
   <!-- verb: getRecord -->
   <xsl:template name="GetRecord">
      
      <xsl:choose>
         <xsl:when test="crossQueryResult/docHit">
            <OAI-PMH xmlns="http://www.openarchives.org/OAI/2.0/" 
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd">
               <responseDate><xsl:value-of select="$responseDate"/></responseDate>
               <request identifier="{$identifier}" metadataPrefix="{$metadataPrefix}" verb="GetRecord">
                  <xsl:value-of select="$http.rawURL"/>
               </request>
               <GetRecord>
                  <xsl:apply-templates select="crossQueryResult/docHit"/>
               </GetRecord>
            </OAI-PMH>
         </xsl:when>
         <xsl:otherwise>
            <xsl:call-template name="oaiError">
               <xsl:with-param name="message">
                  <xsl:value-of select="concat('GetRecord::idDoesNotExist::',$idDoesNotExistMessage)"/>
               </xsl:with-param>
            </xsl:call-template>
         </xsl:otherwise>
      </xsl:choose>
      
   </xsl:template>
   
   <!-- verb: Identify -->
   <xsl:template name="Identify">
      
      <xsl:variable name="earliestDateStamp" select="//docHit[matches(meta/dateStamp,'^[0-9]{4}-[0-9]{2}-[0-9]{2}$')][1]/meta/dateStamp[1]"/>
      
      <OAI-PMH xmlns="http://www.openarchives.org/OAI/2.0/" 
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd">
         <responseDate><xsl:value-of select="$responseDate"/></responseDate>
         <request verb="Identify">
            <xsl:value-of select="$http.rawURL"/>
         </request>
         <Identify>
            <repositoryName>XTF Sample Repository</repositoryName>
            <baseURL><xsl:value-of select="replace($http.rawURL, '/oai.*', '/oai')"/></baseURL>
            <protocolVersion>2.0</protocolVersion>
            <!-- CHANGE -->
            <adminEmail>admin@server.org</adminEmail>
            <earliestDatestamp>
               <xsl:value-of select="$earliestDateStamp"/>
            </earliestDatestamp>
            <deletedRecord>transient</deletedRecord>
            <granularity>YYYY-MM-DD</granularity>
         </Identify>
      </OAI-PMH>
      
   </xsl:template>
   
   <!-- verb: ListIdentifiers -->
   <xsl:template name="ListIdentifiers">
      
      <xsl:choose>
         <xsl:when test="crossQueryResult/docHit">
            <OAI-PMH xmlns="http://www.openarchives.org/OAI/2.0/" 
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd">
               <responseDate><xsl:value-of select="$responseDate"/></responseDate>
               <xsl:choose>
                  <xsl:when test="string-length($resumptionToken) &gt; 0">
                     <!-- Form a new request using the contents of the resumption token, and fetch it -->
                     <xsl:variable name="url" select="concat(replace($http.rawURL, '[;&amp;]resumptionToken=[^;&amp;]+', ''), '&amp;', $decodedResumpToken)"/>
                     <xsl:variable name="result" select="document($url)/*:OAI-PMH"/>
                     <!-- Pick out the pieces for our final result -->
                     <request metadataPrefix="{$result/*:request/@metadataPrefix}" verb="ListIdentifiers">
                        <xsl:value-of select="$http.rawURL"/>
                     </request>
                     <ListIdentifiers>
                        <xsl:copy-of select="$result/*:ListIdentifiers/*"/>
                     </ListIdentifiers>
                  </xsl:when>
                  <xsl:otherwise>
                     <request metadataPrefix="{$metadataPrefix}" verb="ListIdentifiers">
                        <xsl:value-of select="$http.rawURL"/>
               </request>
               <ListIdentifiers>
                  <xsl:apply-templates select="crossQueryResult/docHit" mode="idOnly"/>
               <xsl:if test="$totalDocs > $nextPage">
                  <resumptionToken completeListSize="{$totalDocs}" cursor="{$cursor}">
                              <xsl:value-of select="$nextResumptionToken"/>
                  </resumptionToken>
               </xsl:if>
               </ListIdentifiers>
                  </xsl:otherwise>
               </xsl:choose>
            </OAI-PMH>
         </xsl:when>
         <xsl:otherwise>
            <xsl:call-template name="oaiError">
               <xsl:with-param name="message">
                  <xsl:value-of select="concat('ListIdentifiers::noRecordsMatch::',$noRecordsMatchMessage)"/>
               </xsl:with-param>
            </xsl:call-template>
         </xsl:otherwise>
      </xsl:choose>
      
   </xsl:template>
   
   <!-- verb: ListMetadataFormats -->
   <xsl:template name="ListMetadataFormats">
      
      <OAI-PMH xmlns="http://www.openarchives.org/OAI/2.0/" 
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd">
         <responseDate><xsl:value-of select="$responseDate"/></responseDate>
         <request verb="ListMetadataFormats" identifier="{$identifier}">
            <xsl:value-of select="$http.rawURL"/>
         </request>
         <ListMetadataFormats>
            <metadataFormat>
               <metadataPrefix>oai_dc</metadataPrefix>
               <schema>http://www.openarchives.org/OAI/2.0/oai_dc.xsd</schema>
               <metadataNamespace>http://www.openarchives.org/OAI/2.0/oai_dc/</metadataNamespace>
            </metadataFormat>
         </ListMetadataFormats>
      </OAI-PMH>
      
   </xsl:template>
   
   <!-- verb: ListRecords -->
   <xsl:template name="ListRecords">
      
      <xsl:choose>
         <xsl:when test="crossQueryResult/docHit">
            <OAI-PMH xmlns="http://www.openarchives.org/OAI/2.0/" 
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd">
               <responseDate><xsl:value-of select="$responseDate"/></responseDate>
               <xsl:choose>
                  <xsl:when test="string-length($resumptionToken) &gt; 0">
                     <!-- Form a new request using the contents of the resumption token, and fetch it -->
                     <xsl:variable name="url" select="concat(replace($http.rawURL, '[;&amp;]resumptionToken=[^;&amp;]+', ''), '&amp;', $resumptionToken)"/>
                     <xsl:variable name="result" select="document($url)/*:OAI-PMH"/>
                     <!-- Pick out the pieces for our final result -->
                     <request metadataPrefix="{$result/*:request/@metadataPrefix}" verb="ListRecords">
                        <xsl:value-of select="$http.rawURL"/>
                     </request>
                     <ListRecords>
                        <xsl:copy-of select="$result/*:ListRecords/*"/>
                     </ListRecords>
                  </xsl:when>
                  <xsl:otherwise>
                     <request metadataPrefix="{$metadataPrefix}" verb="ListRecords">
                        <xsl:value-of select="$http.rawURL"/>
                     </request>
                     <ListRecords>
                        <xsl:apply-templates select="crossQueryResult/docHit"/>
                        <xsl:if test="$totalDocs > $nextPage">
                           <resumptionToken completeListSize="{$totalDocs}" cursor="{$cursor}">
                              <xsl:value-of select="$nextResumptionToken"/>
                           </resumptionToken>
                        </xsl:if>
                     </ListRecords>
                  </xsl:otherwise>
               </xsl:choose>
            </OAI-PMH>
         </xsl:when>
         <xsl:otherwise>
            <xsl:call-template name="oaiError">
               <xsl:with-param name="message">
                  <xsl:value-of select="concat('ListRecords::noRecordsMatch::',$noRecordsMatchMessage)"/>
               </xsl:with-param>
            </xsl:call-template>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- verb: ListSets -->
   <xsl:template name="ListSets">
      
      <OAI-PMH xmlns="http://www.openarchives.org/OAI/2.0/" 
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd">
         <responseDate><xsl:value-of select="$responseDate"/></responseDate>
         <request verb="ListSets">
            <xsl:value-of select="$http.rawURL"/>
         </request>
         <ListSets>
            <xsl:apply-templates select="crossQueryResult/facet/group"/>
         </ListSets>
      </OAI-PMH>
      
   </xsl:template>
   
   <!-- Formatting of individual reocrds -->
   <xsl:template match="docHit">
      
      <xsl:variable name="identifier" select="meta/identifier[1]"/>
      <xsl:variable name="dateStamp" select="meta/dateStamp[1]"/>
      
      <record xmlns="http://www.openarchives.org/OAI/2.0/">
         <header>
            <identifier>
               <xsl:value-of select="replace($identifier,'.+/','')"/>
            </identifier>
            <datestamp>
               <xsl:value-of select="$dateStamp"/>
            </datestamp>
         </header>
         <metadata>
            <oai_dc:dc xmlns:dc="http://purl.org/dc/elements/1.1/" 
               xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" 
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">
               <xsl:apply-templates mode="DC" select="meta/*"/>
            </oai_dc:dc>
         </metadata>
      </record>
      
   </xsl:template>
   
   <!-- records as displayed for ListIdentifiers -->
   <xsl:template match="docHit" mode="idOnly">
      
      <xsl:variable name="identifier" select="meta/identifier[1]"/>
      <xsl:variable name="dateStamp" select="meta/dateStamp[1]"/>
      
      <header xmlns="http://www.openarchives.org/OAI/2.0/">
         <identifier>
            <xsl:value-of select="replace($identifier,'.+/','')"/>
         </identifier>
         <datestamp>
            <xsl:value-of select="$dateStamp"/>
         </datestamp>
      </header>
      
   </xsl:template>
   
   <!-- Add namespace to metadata elements -->
   <xsl:template match="title|creator|subject|description|publisher|contributor|date|type|format|identifier|source|language|relation|coverage|rights" mode="DC">
      <xsl:element name="{concat('dc:',local-name())}" namespace="http://purl.org/dc/elements/1.1/">
         <xsl:apply-templates/>
      </xsl:element>
   </xsl:template>
   
   <!-- skip non-standard dublin core elements -->
   <xsl:template match="*" mode="DC"/>
   
   <!-- set formatting -->
   <xsl:template match="group">
      <set xmlns="http://www.openarchives.org/OAI/2.0/">
         <setSpec>
            <xsl:value-of select="replace(replace(replace(@value,' &amp; ',' and '),', ',''),' ','_')"/>
         </setSpec>
         <setName>
            <xsl:value-of select="@value"/>
         </setName>
      </set>
   </xsl:template>   
   
   <!-- error template -->
   <xsl:template name="oaiError">
      
      <xsl:param name="message"/>
      
      <xsl:variable name="request" select="$http.URL"/>
      <xsl:variable name="verb" select="replace($message,'(.+)::.+::.+','$1')"/>
      <xsl:variable name="code" select="replace($message,'.+::(.+)::.+','$1')"/>
      <xsl:variable name="messageText" select="replace($message,'.+::.+::(.+)','$1')"/>
      
      <xsl:result-document method="xml" encoding="UTF-8" media-type="text/xml" indent="yes" exclude-result-prefixes="#all">
         <OAI-PMH xmlns="http://www.openarchives.org/OAI/2.0/" 
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd">
            <responseDate><xsl:value-of select="replace(string($responseDate),'\n','')"/></responseDate>
            <request verb="{$verb}"><xsl:value-of select="$request"/></request>
            <error code="{$code}"><xsl:value-of select="$messageText"/></error>
         </OAI-PMH>
      </xsl:result-document>
      
   </xsl:template>
   
</xsl:stylesheet>
