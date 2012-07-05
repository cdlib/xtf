<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:FileUtils="java:org.cdlib.xtf.xslt.FileUtils"
   extension-element-prefixes="FileUtils"
   version="2.0">
   
   <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
   <!-- OAI error page generation                                              -->
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
      OAI error generation depends on parsing a string of the following format:
      
      OAI::verb::code::messageText 
      
      verb = OAI verb employed in the faulty request
      code = error code
      messageText = dull text of error message
      
      This string is passed from the OAI query parser
   -->
   
   <xsl:output method="xml" media-type="text/xml" encoding="UTF-8" indent="yes"/>
   
   <xsl:param name="exception"/>
   <xsl:param name="message"/>
   <xsl:param name="stackTrace" select="''"/>
   <xsl:param name="http.URL"/>
   <xsl:param name="http.rawURL"/>
   
   <xsl:template match="/">
      <xsl:choose>
         <!-- when compliant message is passed -->
         <xsl:when test="QueryFormat/message">
            <xsl:call-template name="oaiError">
               <xsl:with-param name="message" select="replace(string(QueryFormat/message),'^OAI::','')"/>
            </xsl:call-template>
         </xsl:when>
         <!-- fallback if OAI error page generation breaks down -->
         <xsl:otherwise>
            <error>
               <exception><xsl:value-of select="$exception"/></exception>
               <message><xsl:value-of select="$message"/></message>
               <stackTrace><xsl:value-of select="$stackTrace"/></stackTrace>
            </error>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- OAI Error Template -->
   <xsl:template name="oaiError">
      
      <xsl:param name="message"/>
      <xsl:variable name="responseDate" select="replace(replace(FileUtils:curDateTime('yyyy-MM-dd::HH:mm:ss'),'::','T'),'([0-9])$','$1Z')"/>
      <xsl:variable name="request" select="$http.rawURL"/>
      <xsl:variable name="verb" select="replace($message,'(.+)::.+::.+','$1')"/>
      <xsl:variable name="code" select="replace($message,'.+::(.+)::.+','$1')"/>
      <xsl:variable name="messageText" select="replace($message,'.+::.+::(.+)','$1')"/>
      
      <OAI-PMH xmlns="http://www.openarchives.org/OAI/2.0/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd">
         <responseDate>
            <xsl:value-of select="replace(string($responseDate),'\n','')"/>
         </responseDate>
         <xsl:choose>
             <xsl:when test="$verb='badVerb'">
                 <request>
                     <xsl:value-of select="$request"/>
                 </request>
             </xsl:when>
             <xsl:otherwise>
                 <request verb="{$verb}">
                     <xsl:value-of select="$request"/>
                 </request>
                </xsl:otherwise>
            </xsl:choose>
         <error code="{$code}">
            <xsl:value-of select="$messageText"/>
         </error>
      </OAI-PMH>
      
   </xsl:template>
   
</xsl:stylesheet>
