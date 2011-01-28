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
      
   <xsl:param name="icon.path" select="concat($xtfURL, 'icons/default/')"/>
   
   <xsl:param name="css.path" select="concat($xtfURL, 'css/default/')"/>
   
   <xsl:param name="content.css" select="'tei.css'"/>
   
   <xsl:param name="fig.ent" select="'0'"/>
   
   <xsl:param name="doc.title" select="replace(/*/*:text/*:front/*:titlePage//*:titlePart[@type='main'], ',$', '')"/>
   
   <xsl:param name="doc.subtitle" select="/*/*:text/*:front/*:titlePage//*:titlePart[@type='subtitle']"/>
   
   <xsl:param name="doc.author">
      <xsl:choose>
         <xsl:when test="/*/*:text/*:front/*:titlePage/*:docAuthor[1]/name">
            <xsl:value-of select="/*/*:text/*:front/*:titlePage/*:docAuthor[1]/*:name"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:value-of select="/*/*:text/*:front/*:titlePage/*:docAuthor[1]"/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:param>
   
</xsl:stylesheet>
