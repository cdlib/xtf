<xsl:stylesheet version="2.0" 
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns="http://www.w3.org/1999/xhtml"
   exclude-result-prefixes="#all">
   
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
   
   
   <xsl:template match="*:table">
      
      <xsl:if test="$anchor.id=@*:id">
         <a name="X"></a>
      </xsl:if>
      
      <xsl:element name="table">
         <xsl:copy-of select="@*"/>
         <xsl:choose>
            <xsl:when test="@border"/>
            <xsl:otherwise>
               <xsl:attribute name="border">1</xsl:attribute>
            </xsl:otherwise>
         </xsl:choose>
         <xsl:apply-templates/>
         <xsl:if test="normalize-space(.)"><xsl:text> </xsl:text></xsl:if>
      </xsl:element>
   </xsl:template>
   
   <!-- TEI table model -->
   
   <xsl:template match="*:row">
      <xsl:element name="tr">
         <xsl:copy-of select="@*"/>
         <xsl:apply-templates/>
         <xsl:if test="normalize-space(.)"><xsl:text>&#160;</xsl:text></xsl:if>
      </xsl:element>
   </xsl:template>
   
   <xsl:template match="*:cell">
      <xsl:element name="td">
         <xsl:copy-of select="@*[not(name()='cols' and name()='rows')]"/>
         <xsl:if test="@cols">
            <xsl:attribute name="colspan" select="@cols"/>
         </xsl:if>
         <xsl:if test="@rows">
            <xsl:attribute name="rowspan" select="@rows"/>
         </xsl:if>
         <xsl:apply-templates/>
         <xsl:if test="normalize-space(.)"><xsl:text>&#160;</xsl:text></xsl:if>
      </xsl:element>
   </xsl:template>
   
   <!-- HTML Table Model -->
   
   <xsl:template match="*:caption[ancestor::*:table]">
      <xsl:element name="caption">
         <xsl:copy-of select="@*"/>
         <xsl:apply-templates/>
         <xsl:if test="normalize-space(.)"><xsl:text> </xsl:text></xsl:if>
      </xsl:element>
   </xsl:template>
   
   <xsl:template match="*:thead">
      <xsl:element name="thead">
         <xsl:copy-of select="@*"/>
         <xsl:apply-templates/>
         <xsl:if test="normalize-space(.)"><xsl:text> </xsl:text></xsl:if>
      </xsl:element>
   </xsl:template>
   
   <xsl:template match="*:tfoot">
      <xsl:element name="tfoot">
         <xsl:copy-of select="@*"/>
         <xsl:apply-templates/>
         <xsl:if test="normalize-space(.)"><xsl:text> </xsl:text></xsl:if>
      </xsl:element>
   </xsl:template>
   
   <xsl:template match="*:tbody">
      <xsl:element name="tbody">
         <xsl:copy-of select="@*"/>
         <xsl:apply-templates/>
         <xsl:if test="normalize-space(.)"><xsl:text> </xsl:text></xsl:if>
      </xsl:element>
   </xsl:template>
   
   <xsl:template match="*:colgroup">
      <xsl:element name="colgroup">
         <xsl:copy-of select="@*"/>
         <xsl:apply-templates/>
         <xsl:if test="normalize-space(.)"><xsl:text> </xsl:text></xsl:if>
      </xsl:element>
   </xsl:template>
   
   <xsl:template match="*:col">
      <xsl:element name="col">
         <xsl:copy-of select="@*"/>
         <xsl:apply-templates/>
         <xsl:if test="normalize-space(.)"><xsl:text> </xsl:text></xsl:if>
      </xsl:element>
   </xsl:template>
   
   <xsl:template match="*:tr">
      <xsl:element name="tr">
         <xsl:copy-of select="@*"/>
         <xsl:apply-templates/>
         <xsl:if test="normalize-space(.)"><xsl:text> </xsl:text></xsl:if>
      </xsl:element>
   </xsl:template>
   
   <xsl:template match="*:th">
      <xsl:element name="th">
         <xsl:copy-of select="@*"/>
         <xsl:apply-templates/>
         <xsl:if test="normalize-space(.)"><xsl:text> </xsl:text></xsl:if>
      </xsl:element>
   </xsl:template>
   
   <xsl:template match="*:td">
      <xsl:element name="td">
         <xsl:copy-of select="@*"/>
         <xsl:apply-templates/>
         <xsl:if test="normalize-space(.)"><xsl:text> </xsl:text></xsl:if>
      </xsl:element>
   </xsl:template>
   
</xsl:stylesheet>
