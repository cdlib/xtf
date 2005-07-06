<!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
<!-- Query result formatter stylesheet                                      -->
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

<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
                              xmlns:xsi="http://www.w3.org/2001/XMLSchema" 
                              xmlns:dc="http://purl.org/dc/elements/1.1/" 
                              xmlns:srw="http://www.loc.gov/zing/srw/" 
                              xmlns:srw_dc="info:srw/schema/1/dc-schema">
                              
<!-- ====================================================================== -->
<!-- Global parameters (specified in the URL)                               -->
<!-- ====================================================================== -->

  <!-- servlet path from the URL -->
  <xsl:param name="servlet.path"/>
  <xsl:param name="root.path"/>
  
  <!-- operation (explain, searchRetrieve, etc.) -->
  <xsl:param name="operation" select="'searchRetrieve'"/>

  <!-- SRU version -->
  <xsl:param name="version" select="'1.1'"/>

  <!-- XCQL query -->
  <xsl:param name="query"/>
  
  <!-- first hit on page -->
  <xsl:param name="startRecord" select="1"/>
  
  <!-- documents per page -->
  <xsl:param name="maximumRecords" select="20"/>
  
  <!-- how to pack records -->
  <xsl:param name="recordPacking" select="'xml'"/>
   
  <!-- schema for records -->
  <xsl:param name="recordSchema" select="'dc'"/>
   
<!-- ====================================================================== -->
<!-- Output Parameters                                                      -->
<!-- ====================================================================== -->

  <xsl:output method="xml" indent="yes" encoding="UTF-8" media-type="text/xml"/>

<!-- ====================================================================== -->
<!-- Root Template                                                          -->
<!-- ====================================================================== -->
  
  <xsl:template match="/SRUResult">
    <srw:searchRetrieveResponse>
      <srw:version>1.1</srw:version>
      <srw:numberOfRecords><xsl:value-of select="@totalDocs"/></srw:numberOfRecords>
      <srw:records>
        <xsl:apply-templates select="docHit"/>
      </srw:records>
      <srw:echoedSearchRetrieveRequest>
        <srw:version><xsl:value-of select="$version"/></srw:version>
        <srw:query><xsl:value-of select="$query"/></srw:query>
        <srw:startRecord><xsl:value-of select="$startRecord"/></srw:startRecord>
        <srw:maximumRecords><xsl:value-of select="$maximumRecords"/></srw:maximumRecords>
        <srw:recordPacking><xsl:value-of select="$recordPacking"/></srw:recordPacking>
        <srw:recordSchema><xsl:value-of select="$recordSchema"/></srw:recordSchema>
      </srw:echoedSearchRetrieveRequest>
    </srw:searchRetrieveResponse>
  </xsl:template>
  
  <xsl:template match="docHit">
    <srw:record>
      <srw:recordPacking>XML</srw:recordPacking>
      <srw:recordSchema>info:srw/schema/1/dc-v1.1</srw:recordSchema>
      <srw:recordData>
        <srw_dc:dc xsi:schemaLocation="info:srw/schema/1/dc-schema http://www.loc.gov/z3950/agency/zing/srw/dc-schema.xsd">
          <xsl:apply-templates select="meta"/>
        </srw_dc:dc>
      </srw:recordData>
    </srw:record>
  </xsl:template>
  
  <xsl:template match="meta">
    <xsl:apply-templates select="title|creator|subject|description|publisher|contributor|date|type|format|identifier|source|language|relation|coverage|rights"/>
  </xsl:template>
  
  <xsl:template match="title|creator|subject|description|publisher|contributor|date|type|format|identifier|source|language|relation|coverage|rights">
    <xsl:element name="{concat('dc:', name())}">
      <xsl:value-of select="."/>
    </xsl:element>
  </xsl:template>
  
</xsl:stylesheet>
