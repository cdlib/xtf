<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
   
   <xsl:output method="xml" media-type="text/xml" encoding="UTF-8" indent="yes"/>
   
   <xsl:param name="exception"/>
   <xsl:param name="message"/>
   <xsl:param name="stackTrace" select="''"/>
   <xsl:param name="http.URL"/>
   
   <xsl:template match="/">
      <xsl:choose>
         <xsl:when test="QueryFormat/message">
            <xsl:call-template name="oaiError">
               <xsl:with-param name="message" select="replace(string(QueryFormat/message),'^OAI::','')"/>
            </xsl:call-template>
         </xsl:when>
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
      <xsl:variable name="responseDate" select="replace(replace(FileUtils:curDateTime('yyyy-MM-dd::HH:mm:ss'),'::','T'),'([0-9])$','$1Z')" xmlns:FileUtils="java:org.cdlib.xtf.xslt.FileUtils"/>
      <xsl:variable name="request" select="$http.URL"/>
      <xsl:variable name="verb" select="replace($message,'(.+)::.+::.+','$1')"/>
      <xsl:variable name="code" select="replace($message,'.+::(.+)::.+','$1')"/>
      <xsl:variable name="messageText" select="replace($message,'.+::.+::(.+)','$1')"/>
      
      <OAI-PMH xmlns="http://www.openarchives.org/OAI/2.0/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd">
         <responseDate>
            <xsl:value-of select="replace(string($responseDate),'\n','')"/>
         </responseDate>
         <request verb="{$verb}">
            <xsl:value-of select="$request"/>
         </request>
         <error code="{$code}">
            <xsl:value-of select="$messageText"/>
         </error>
      </OAI-PMH>
      
   </xsl:template>
   
</xsl:stylesheet>
