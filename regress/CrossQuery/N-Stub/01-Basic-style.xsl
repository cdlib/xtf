<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet version="2.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  xmlns:FileUtils="java:org.cdlib.xtf.xslt.FileUtils">
                              
<!-- ====================================================================== -->
<!-- Output Parameters                                                      -->
<!-- ====================================================================== -->

  <xsl:output method="xml" indent="yes" encoding="UTF-8" media-type="text/xml"/>

<!-- ====================================================================== -->
<!-- Root Template                                                          -->
<!-- ====================================================================== -->
  
  <xsl:template match="/crossQueryResult">
    <documents>
      <xsl:call-template name="identify">
        <xsl:with-param name="file" select="'filesToID/bell.xml'"/>        
      </xsl:call-template>
      <xsl:call-template name="identify">
        <xsl:with-param name="file" select="'filesToID/bmc.xml'"/>        
      </xsl:call-template>
      <xsl:call-template name="identify">
        <xsl:with-param name="file" select="'filesToID/ft1n39n7wg.xml'"/>        
      </xsl:call-template>
      <xsl:call-template name="identify">
        <xsl:with-param name="file" select="'filesToID/ft2g5004sk.xml'"/>        
      </xsl:call-template>
      <xsl:call-template name="identify">
        <xsl:with-param name="file" select="'filesToID/ft6p3007r2.dc.xml'"/>        
      </xsl:call-template>
      <xsl:call-template name="identify">
        <xsl:with-param name="file" select="'filesToID/AbeGruber.xml'"/>        
      </xsl:call-template>
    </documents>
  </xsl:template>

  <xsl:template name="identify">
    <xsl:param name="file"/>
    <xsl:for-each select="FileUtils:readXMLStub($file)">
      <xsl:variable name="root-element-name" select="name(*[1])"/>
      <doc name="{$file}">
        <xsl:if test="unparsed-entity-public-id($root-element-name)">
          <xsl:attribute name="DOCTYPE.publicID" select="unparsed-entity-public-id($root-element-name)"/>
        </xsl:if>
        <xsl:if test="unparsed-entity-uri($root-element-name)">
          <xsl:attribute name="DOCTYPE.uri" select="unparsed-entity-uri($root-element-name)"/>
        </xsl:if>
        <xsl:copy-of select="*"/>
      </doc>
    </xsl:for-each>
  </xsl:template>
</xsl:stylesheet>
