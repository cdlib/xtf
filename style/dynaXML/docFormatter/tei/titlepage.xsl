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
   
   
   <xsl:variable name="uniqueKey" select="replace(replace($docId,'^.+/',''), '\.xml$', '')"/>
   
   <xsl:template match="*:titlePage">
      <table width="100%" cellpadding="5" cellspacing="5">
         <tr>
            <td width="200" align="center">
               <img src="{$figure.path}{$uniqueKey}_cover.jpg" alt="cover"/>
            </td>
            <td>
               <xsl:apply-templates select="/*/*:text/*:front/*:titlePage/*" mode="titlepage"/>
            </td>
         </tr>
      </table>
      
      <hr/>
      
      <div align="center">
         <span class="down1">
            <xsl:if test="/*/*:text/*:front/*:div1[@type='dedication']">
               <xsl:text> [</xsl:text>
               <a>
                  <xsl:attribute name="href">javascript://</xsl:attribute>
                  <xsl:attribute name="onclick">
                     <xsl:text>javascript:window.open('</xsl:text><xsl:value-of select="$doc.path"/>&#038;doc.view=popup&#038;chunk.id=<xsl:value-of select="/*/*:text/*:front/*:div1[@type='dedication']/@*:id"/><xsl:text>','popup','width=300,height=300,resizable=yes,scrollbars=yes')</xsl:text>
                  </xsl:attribute>
                  <xsl:text>Dedication</xsl:text>
               </a>
               <xsl:text>] </xsl:text>
            </xsl:if>
            <xsl:if test="/*/*:text/*:front/*:div1[@type='copyright']">
               <xsl:text> [</xsl:text>
               <a>
                  <xsl:attribute name="href">javascript://</xsl:attribute>
                  <xsl:attribute name="onclick">
                     <xsl:text>javascript:window.open('</xsl:text><xsl:value-of select="$doc.path"/>&#038;doc.view=popup&#038;chunk.id=<xsl:value-of select="/*/*:text/*:front/*:div1[@type='copyright']/@*:id"/><xsl:text>','popup','width=300,height=300,resizable=yes,scrollbars=yes')</xsl:text>
                  </xsl:attribute>
                  <xsl:text>Copyright</xsl:text>
               </a>
               <xsl:text>] </xsl:text>
            </xsl:if>
            <xsl:if test="/*/*:text/*:front/*:div1[@type='epigraph']">
               <xsl:text> [</xsl:text>
               <a>
                  <xsl:attribute name="href">javascript://</xsl:attribute>
                  <xsl:attribute name="onclick">
                     <xsl:text>javascript:window.open('</xsl:text><xsl:value-of select="$doc.path"/>&#038;doc.view=popup&#038;chunk.id=<xsl:value-of select="/*/*:text/*:front/*:div1[@type='epigraph']/@*:id"/><xsl:text>','popup','width=300,height=300,resizable=yes,scrollbars=yes')</xsl:text>
                  </xsl:attribute>
                  <xsl:text>Epigraph</xsl:text>
               </a>
               <xsl:text>] </xsl:text>
            </xsl:if>
         </span>
      </div>
      
   </xsl:template>
   
   <xsl:template match="*:titlePart" mode="titlepage">
      <xsl:choose>
         <xsl:when test="@type='subtitle'">
            <h4><i><xsl:apply-templates/></i></h4>
         </xsl:when>
         <xsl:otherwise>
            <h2><xsl:apply-templates/></h2>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <xsl:template match="*:docAuthor" mode="titlepage">
      <xsl:choose>
         <xsl:when test="name">
            <xsl:apply-templates mode="titlepage"/>
         </xsl:when>
         <xsl:otherwise>
            <h4><xsl:apply-templates/></h4>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <xsl:template match="*:docAuthor/*:name" mode="titlepage">
      <h4><xsl:apply-templates/></h4>
   </xsl:template>
   
   <xsl:template match="*:docAuthor/*:address" mode="titlepage">
      <h5><xsl:apply-templates/></h5>
   </xsl:template>
   
   <xsl:template match="*:docImprint/*:publisher" mode="titlepage">
      <h6><xsl:apply-templates/></h6>
   </xsl:template>
   
   <xsl:template match="*:docImprint/*:pubPlace" mode="titlepage">
      <h6><i><xsl:apply-templates/></i></h6>
   </xsl:template>
   
   <xsl:template match="*:docImprint/*:docDate" mode="titlepage">
      <h6>
         <xsl:text>&#169; </xsl:text><xsl:apply-templates/>
         <xsl:text> The Regents of the University of California</xsl:text>
      </h6>
   </xsl:template>
   
   <xsl:template match="*:div1[@type='dedication']" mode="titlepage">
      <xsl:apply-templates/>
   </xsl:template>
   
   <xsl:template match="*:div1[@type='copyright']" mode="titlepage">
      <xsl:apply-templates/>
   </xsl:template>
   
   <xsl:template match="*:div1[@type='epigraph']" mode="titlepage">
      <xsl:apply-templates/>
   </xsl:template>
   
</xsl:stylesheet>
