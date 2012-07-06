<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0" >
   
   <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
   <!-- Simple query parser stylesheet                                         -->
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
      This stylesheet implements a simple query parser for handling OAI 
      harvesting requests. It is far from perfect, but does pass all of 
      the tests at http://www.openarchives.org/Register/ValidateSite
      Unfortunately I had to hard code some dates to make it all work. You'll 
      have to change these or think of a better solution.
   -->
   
   <!-- ====================================================================== -->
   <!-- Output                                                                 -->
   <!-- ====================================================================== -->
   
   <xsl:output method="xml" encoding="UTF-8" indent="yes"/>
   <xsl:strip-space elements="*"/>
   
   <!-- ====================================================================== -->
   <!-- Global parameters (specified in the URL)                               -->
   <!-- ====================================================================== -->
   
   <!-- whole URL -->
   <xsl:param name="http.URL"/>
   
   <!-- verb param -->
   <xsl:param name="verb"/>
   <!-- identifier param -->
   <xsl:param name="identifier"/>
   <!-- metadataPrefix param -->
   <xsl:param name="metadataPrefix"/>
   <!-- resumptionToken param -->
   <xsl:param name="resumptionToken"/>
   <!-- set param -->
   <xsl:param name="set"/>
   <!-- from param -->
   <xsl:param name="from"/>
   <!-- until param -->
   <xsl:param name="until"/>
   
   <!-- startDoc param -->
   <xsl:param name="startDoc" select="1"/>
   
   <!-- ====================================================================== -->
   <!-- Local parameters                                                       -->
   <!-- ====================================================================== -->
   
   <!-- earliestDatestamp param -->
   <!-- this is a real kludge, please reset to the earliest date in your collection -->
   <xsl:param name="earliestDatestamp" select="'1800-01-01'"/>
   
   <!-- maxDocs param -->
   <xsl:param name="maxDocs">
      <xsl:choose>
         <xsl:when test="$verb='Identify' or $verb='ListIdentifiers' or $verb='ListRecords'">20</xsl:when>
         <xsl:otherwise>1</xsl:otherwise>
      </xsl:choose>
   </xsl:param>
   
   <!-- error messages -->
   <xsl:variable name="badArgumentMessage">
      <xsl:text>The request includes illegal arguments, is missing required arguments, includes a repeated argument, or values for arguments have an illegal syntax.</xsl:text>
   </xsl:variable>
   <xsl:variable name="badResumptionTokenMessage">
      <xsl:text>The value of the resumptionToken argument is invalid or expired.</xsl:text>
   </xsl:variable>
   <xsl:variable name="badVerbMessage">
      <xsl:text>Value of the verb argument is not a legal OAI-PMH verb, the verb argument is missing, or the verb argument is repeated.</xsl:text>
   </xsl:variable>
   <xsl:variable name="cannotDisseminateFormatMessage">
      <xsl:text>The metadata format identified by the value given for the metadataPrefix argument is not supported by the item or by the repository.</xsl:text>
   </xsl:variable>
   <xsl:variable name="idDoesNotExistMessage">
      <xsl:text>The value of the identifier argument is unknown or illegal in this repository.</xsl:text>
   </xsl:variable>
   <xsl:variable name="noRecordsMatchMessage">
      <xsl:text>The combination of the values of the from, until, set and metadataPrefix arguments results in an empty list.</xsl:text>
   </xsl:variable>
   <xsl:variable name="noMetadataFormatsMessage">
      <xsl:text>There are no metadata formats available for the specified item.</xsl:text>
   </xsl:variable>
   
   <!-- this regex allows only identifiers compliant with the oai-identifer schema (well almost) -->
   <xsl:variable name="idPattern" select="'^[A-Za-z0-9\.\?\*\+\(\)\-\$,;/:@&amp;=_!~'']+$'"/>
   
   <!-- Some OAI harvesters double-escape our percent encoding, some need it -->
   <xsl:variable name="decodedResumpToken" xmlns:decoder="java:java.net.URLDecoder"
      select="if ($resumptionToken)
              then decoder:decode(decoder:decode($resumptionToken,'UTF-8'),'UTF-8') 
              else $resumptionToken" />
   
   <!-- ====================================================================== -->
   <!-- Root Template                                                          -->
   <!-- ====================================================================== -->
   
   <xsl:template match="/">
      
      <!-- illegal params. 
           Note startDoc is internal only (used by recursive resumption token processing)
      -->
      <xsl:variable name="queryParams" select="//param[count(*) &gt; 0 
         and not(@name='verb') 
         and not(@name='identifier') 
         and not(@name='metadataPrefix') 
         and not(@name='resumptionToken') 
         and not(@name='startDoc')
         and not(@name='set') 
         and not(@name='from') 
         and not(@name='until')]"/>
      
      <!-- Error Handling -->
      
      <xsl:choose>
         
         <!-- illegal argumentss -->
         <xsl:when test="count($queryParams) &gt; 0">
            <error message="OAI::{$verb}::badArgument::{$badArgumentMessage}"/>
         </xsl:when>
         
         <!-- double params -->
         <xsl:when test="contains(substring-after($http.URL,'verb'),'verb') or 
            contains(substring-after($http.URL,'identifier'),'identifier') or 
            contains(substring-after($http.URL,'metadataPrefix'),'metadataPrefix') or 
            contains(substring-after($http.URL,'resumptionToken'),'resumptionToken') or 
            contains(substring-after($http.URL,'set'),'set') or 
            contains(substring-after($http.URL,'from'),'from') or 
            contains(substring-after($http.URL,'until'),'until')">
            <error message="OAI::{$verb}::badArgument::{$badArgumentMessage}"/>
         </xsl:when>
         
         <!-- 'from' and 'until' -->
         <xsl:when test="$from and not(matches($from,'^[0-9]{4}-[0-9]{2}-[0-9]{2}$'))">
            <error message="OAI::{$verb}::badArgument::{$badArgumentMessage}"/>
         </xsl:when>
         <xsl:when test="$until and not(matches($until,'^[0-9]{4}-[0-9]{2}-[0-9]{2}$'))">
            <error message="OAI::{$verb}::badArgument::{$badArgumentMessage}"/>
         </xsl:when>
         <xsl:when test="number(replace($until,'-.+','')) &lt; number(replace($earliestDatestamp,'-.+',''))">
            <error message="OAI::{$verb}::noRecordsMatch::{$noRecordsMatchMessage}"/>
         </xsl:when>
         <!-- this should work for months as well -->
         <xsl:when test="($from and $until) and (number(replace($from,'-.+','')) &gt; number(replace($until,'-.+','')))">
            <error message="OAI::{$verb}::badArgument::{$badArgumentMessage}"/>
         </xsl:when>
         
         <!-- If resumption token specified, no error checking until the recursive re-query -->
         <xsl:when test="$resumptionToken and not(matches($decodedResumpToken,'[\w%.&amp;]*startDoc=\d+'))">
            <error message="OAI::{$verb}::badResumptionToken::{$badResumptionTokenMessage}"/>
         </xsl:when>
         <xsl:when test="string-length($resumptionToken) &gt; 0">
            <query indexPath="index" maxDocs="1" startDoc="1"  style="style/crossQuery/resultFormatter/oai/resultFormatter.xsl">
               <allDocs/>
            </query>
         </xsl:when>
         
         <!-- verb: GetRecord -->
         <xsl:when test="$verb='GetRecord'">
            <xsl:choose>
               <xsl:when test="not($identifier) or not($metadataPrefix)">
                  <error message="OAI::{$verb}::badArgument::{$badArgumentMessage}"/>
               </xsl:when>
               <xsl:when test="$metadataPrefix != 'oai_dc'">
                  <error message="OAI::{$verb}::cannotDisseminateFormat::{$cannotDisseminateFormatMessage}"/>
               </xsl:when>
               <xsl:when test="not(matches($identifier,$idPattern))">
                  <error message="OAI::{$verb}::idDoesNotExist::{$idDoesNotExistMessage}"/>
               </xsl:when>
               <xsl:otherwise>
                  <xsl:call-template name="query"/>
               </xsl:otherwise>
            </xsl:choose>
         </xsl:when>
         
         <!-- verb: Identify -->
         <xsl:when test="$verb='Identify'">
            <xsl:choose>
               <xsl:when test="$identifier or $metadataPrefix or $resumptionToken or $set or $from or $until">
                  <error message="OAI::{$verb}::badArgument::{$badArgumentMessage}"/>
               </xsl:when>
               <xsl:otherwise>
                  <xsl:call-template name="query"/>
               </xsl:otherwise>
            </xsl:choose>
         </xsl:when>
         
         <!-- verb: ListIdentifiers -->
         <xsl:when test="$verb='ListIdentifiers'">
            <xsl:choose>
               <xsl:when test="not($metadataPrefix)">
                  <error message="OAI::{$verb}::badArgument::{$badArgumentMessage}"/>
               </xsl:when>
               <xsl:when test="$metadataPrefix != 'oai_dc'">
                  <error message="OAI::{$verb}::cannotDisseminateFormat::{$cannotDisseminateFormatMessage}"/>
               </xsl:when>
               <xsl:otherwise>
                  <xsl:call-template name="query"/>
               </xsl:otherwise>
            </xsl:choose>
         </xsl:when>
         
         <!-- verb: ListMetadataFormats -->
         <xsl:when test="$verb='ListMetadataFormats'">
            <xsl:choose>
               <xsl:when test="$metadataPrefix or $resumptionToken or $set or $from or $until">
                  <error message="OAI::{$verb}::badArgument::{$badArgumentMessage}"/>
               </xsl:when>
               <xsl:when test="$identifier and not(matches($identifier,$idPattern))">
                  <error message="OAI::{$verb}::idDoesNotExist::{$idDoesNotExistMessage}"/>
               </xsl:when>
               <xsl:otherwise>
                  <xsl:call-template name="query"/>
               </xsl:otherwise>
            </xsl:choose>
         </xsl:when>
         
         <!-- verb: ListRecords -->
         <xsl:when test="$verb='ListRecords'">
            <xsl:choose>
               <xsl:when test="not($metadataPrefix)">
                  <error message="OAI::{$verb}::badArgument::{$badArgumentMessage}"/>
               </xsl:when>
               <xsl:when test="$metadataPrefix != 'oai_dc'">
                  <error message="OAI::{$verb}::cannotDisseminateFormat::{$cannotDisseminateFormatMessage}"/>
               </xsl:when>
               <xsl:otherwise>
                  <xsl:call-template name="query"/>
               </xsl:otherwise>
            </xsl:choose>
         </xsl:when>
         
         <!-- verb: ListSets -->
         <xsl:when test="$verb='ListSets'">
            <xsl:choose>
               <xsl:when test="$identifier or $metadataPrefix or $from or $until">
                  <error message="OAI::{$verb}::badArgument::{$badArgumentMessage}"/>
               </xsl:when>
               <xsl:otherwise>
                  <xsl:call-template name="query"/>
               </xsl:otherwise>
            </xsl:choose>
         </xsl:when>
         
         <!-- error: illegal or missing verb -->
         <xsl:when test="$verb">
            <error message="OAI::badVerb::badVerb::{$badVerbMessage}"/>
         </xsl:when>
         <xsl:otherwise>
            <error message="OAI::noVerb::badVerb::{$badVerbMessage}"/>
         </xsl:otherwise>
      </xsl:choose>
      
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Query Template                                                         -->
   <!-- ====================================================================== -->
   
   <!-- construct query -->
   <xsl:template name="query">
      <query indexPath="index" maxDocs="{$maxDocs}" startDoc="{$startDoc}" sortMetaFields="dateStamp" style="style/crossQuery/resultFormatter/oai/resultFormatter.xsl" termLimit="1000" workLimit="1000000">
         <xsl:choose>
            <xsl:when test="$verb='GetRecord'">
               <and field="identifier">
                  <term><xsl:value-of select="$identifier"/></term>
               </and>
            </xsl:when>
            <xsl:when test="$verb='ListIdentifiers' or $verb='ListRecords'">
               <and>
                  <xsl:choose>
                     <xsl:when test="$from or $until">
                        <range field="dateStamp" numeric="yes">
                           <xsl:if test="$until">
                              <upper><xsl:value-of select="$until"/></upper>
                           </xsl:if>
                           <xsl:if test="$from">
                              <lower><xsl:value-of select="$from"/></lower>
                           </xsl:if>
                        </range>
                     </xsl:when>
                      <xsl:otherwise>
                        <and>
                          <allDocs/>
                        </and>
                     </xsl:otherwise>
                  </xsl:choose>
                  <xsl:if test="$set">
                     <exact field="set">
                        <xsl:call-template name="tokenize">
                           <xsl:with-param name="input" select="$set"/>
                        </xsl:call-template>
                     </exact>
                  </xsl:if>
               </and>
            </xsl:when>
            <xsl:when test="$verb='ListMetadataFormats'">
               <xsl:choose>
                  <xsl:when test="$identifier">
                     <and field="identifier">
                        <term><xsl:value-of select="$identifier"/></term>
                     </and>
                  </xsl:when>
                  <xsl:otherwise>
                     <and field="display">
                        <term>all</term>
                     </and>
                  </xsl:otherwise>
               </xsl:choose>
            </xsl:when>
            <xsl:when test="$verb='ListSets'">
               <facet field="facet-subject" sortGroupsBy="value" sortDocsBy="dateStamp" select="*"/>
               <and>
                  <allDocs/>
               </and>
            </xsl:when>
            <!-- I really hate using these hard-coded dates -->
            <xsl:when test="$verb='Identify'">
               <range field="dateStamp" numeric="yes">
                  <upper>1970-01-01</upper>
               </range>
            </xsl:when>
         </xsl:choose>
      </query>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Tokenizing Template                                                    -->
   <!--                                                                        -->
   <!-- For tokenizing space delimited strings                                 -->
   <!-- ====================================================================== -->
   
   <xsl:template name="tokenize">
      <xsl:param name="input"/>
      <xsl:if test="normalize-space($input)!=''">
         <xsl:variable name="cleanString" select="replace(replace(replace($input,' +',' '),'^ ',''),' $','')"/>
         <xsl:variable name="term" select="substring-before(concat($cleanString,' '),' ')"/>
         <xsl:variable name="afterTerm" select="substring-after($cleanString,' ')"/>
         <term><xsl:value-of select="$term"/></term>
         <xsl:call-template name="tokenize">
            <xsl:with-param name="input" select="$afterTerm"/>
         </xsl:call-template>
      </xsl:if>
   </xsl:template>
   
</xsl:stylesheet>
