<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:xtf="http://cdlib.org/xtf" 
   xmlns="http://www.w3.org/1999/xhtml"
   version="2.0">
   
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
   
   <!-- Search Hits -->
   
   <xsl:template match="xtf:hit">
      
      <a name="{@hitNum}"/>
      
      <xsl:call-template name="prev.hit"/>
      
      <xsl:choose>
         <xsl:when test="xtf:term">
            <span class="hitsection">
               <xsl:apply-templates/>
            </span>
         </xsl:when>
         <xsl:otherwise>
            <span class="hit">
               <xsl:apply-templates/>
            </span>
         </xsl:otherwise>
      </xsl:choose>
      
      <xsl:if test="not(@more='yes')">
         <xsl:call-template name="next.hit"/>
      </xsl:if>
      
   </xsl:template>
   
   <xsl:template match="xtf:more">
      
      <span class="hitsection">
         <xsl:apply-templates/>
      </span>
      
      <xsl:if test="not(@more='yes')">
         <xsl:call-template name="next.hit"/>
      </xsl:if>
      
   </xsl:template>
   
   <xsl:template match="xtf:term">
      <span class="subhit">
         <xsl:apply-templates/>
      </span>
   </xsl:template>
   
   <xsl:template name="prev.hit">
      
      <xsl:variable name="num" select="@hitNum"/>
      <xsl:variable name="prev" select="$num - 1"/>
      
      <xsl:if test="$prev &gt; 0">
         <a>
            <xsl:attribute name="href">
               <xsl:text>#</xsl:text><xsl:value-of select="$prev"/>
            </xsl:attribute>
            <img src="{$icon.path}b_inprev.gif" border="0" alt="previous hit"/>
         </a>
      </xsl:if>
   </xsl:template>
   
   <xsl:template name="next.hit">
      
      <xsl:variable name="num" select="@hitNum"/>
      <xsl:variable name="next" select="$num + 1"/>
      <xsl:variable name="totalHits" select="/*[1]/@xtf:hitCount"/>
      
      <xsl:if test="$next &lt;= $totalHits">
         <xsl:text>&#160;</xsl:text>
         <a>
            <xsl:attribute name="href">
               <xsl:text>#</xsl:text><xsl:value-of select="$next"/>
            </xsl:attribute>
            <img src="{$icon.path}b_innext.gif" border="0" alt="next hit"/>
         </a>
      </xsl:if>
   </xsl:template>
   
</xsl:stylesheet>
