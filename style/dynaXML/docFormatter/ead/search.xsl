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
      
      <xsl:if test="@hitNum = key('chunk-id', $chunk.id)/@xtf:firstHit">
         <a name="X"/>
      </xsl:if>
      
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
   
   <!-- This template locates the TOC chunk that a particular hit resides
        in. It's a bit complicated by the way we group sets of archdesc
        elements (like for "Restrictions", "Administrative Information",
        etc.) -->
   <xsl:template name="findHitChunk">
      <xsl:param name="hitNode"/>
      <xsl:choose>
         <xsl:when test="$hitNode/ancestor::c02[@level='subseries']">
            <xsl:value-of select="$hitNode/ancestor::c02[1]/@id"/>
         </xsl:when>
         <xsl:when test="$hitNode/ancestor::c01">
            <xsl:value-of select="$hitNode/ancestor::c01[1]/@id"/>
         </xsl:when>
         <xsl:when test="$hitNode/ancestor::accessrestrict/ancestor::archdesc or
                         $hitNode/ancestor::userestrict/ancestor::archdesc">
            <xsl:value-of select="'restrictlink'"/>
         </xsl:when>
         <xsl:when test="$hitNode/ancestor::relatedmaterial/ancestor::archdesc or
                         $hitNode/ancestor::separatedmaterial/ancestor::archdesc">
            <xsl:value-of select="'relatedmatlink'"/>
         </xsl:when>
         <xsl:when test="$hitNode/ancestor::custodhist/ancestor::archdesc or
                         $hitNode/ancestor::altformavailable/ancestor::archdesc or
                         $hitNode/ancestor::prefercite/ancestor::archdesc or
                         $hitNode/ancestor::acqinfo/ancestor::archdesc or
                         $hitNode/ancestor::processinfo/ancestor::archdesc or
                         $hitNode/ancestor::appraisal/ancestor::archdesc or
                         $hitNode/ancestor::accruals/ancestor::archdesc">
            <xsl:value-of select="'adminlink'"/>
         </xsl:when>
         <xsl:when test="$hitNode/ancestor::*[@id]/ancestor::archdesc">
            <xsl:value-of select="$hitNode/ancestor::*[@id][1]/@id"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:value-of select="'0'"/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <!-- Output the linking arrow to move to the previous hit. If the hit is in
        the same chunk as the current hit, we jump to its anchor; otherwise
        we jump to the external chunk. -->
   <xsl:template name="prev.hit">
      
      <xsl:variable name="num" select="@hitNum"/>
      <xsl:variable name="prev" select="$num - 1"/>
      
      <xsl:if test="$prev &gt; 0">
         <xsl:variable name="target.chunk">
            <xsl:call-template name="findHitChunk">
               <xsl:with-param name="hitNode" select="key('hit-num-dynamic', string($prev))"/>
            </xsl:call-template>
         </xsl:variable>
         <a>
            <xsl:choose>
               <xsl:when test="$target.chunk = $chunk.id">
                  <xsl:attribute name="href" select="concat('#', $prev)"/>
               </xsl:when>
               <xsl:otherwise>
                  <xsl:attribute name="target" select="'_top'"/>
                  <xsl:attribute name="href" select="
                     concat($xtfURL, $dynaxmlPath, '?', $query.string, 
                            ';hit.num=', $prev, ';brand=', $brand, $search)"/>
               </xsl:otherwise>
            </xsl:choose>
            <img src="{$icon.path}b_inprev.gif" border="0" alt="previous hit"/>
         </a>         
         <xsl:text>&#160;</xsl:text>
      </xsl:if>
   </xsl:template>
   
   <!-- Output the linking arrow to move to the next hit. If the hit is in
        the same chunk as the current hit, we jump to its anchor; otherwise
        we jump to the external chunk. -->
   <xsl:template name="next.hit">
      
      <xsl:variable name="num" select="@hitNum"/>
      <xsl:variable name="next" select="$num + 1"/>
      <xsl:variable name="totalHits" select="/*[1]/@xtf:hitCount"/>
      
      <xsl:if test="$next &lt;= $totalHits">
         <xsl:variable name="target.chunk">
            <xsl:call-template name="findHitChunk">
               <xsl:with-param name="hitNode" select="key('hit-num-dynamic', string($next))"/>
            </xsl:call-template>
         </xsl:variable>       
         <xsl:text>&#160;</xsl:text>
         <a>
            <xsl:choose>
               <xsl:when test="$target.chunk = $chunk.id">
                  <xsl:attribute name="href" select="concat('#', $next)"/>
               </xsl:when>
               <xsl:otherwise>
                  <xsl:attribute name="target" select="'_top'"/>
                  <xsl:attribute name="href" select="
                     concat($xtfURL, $dynaxmlPath, '?', $query.string, 
                            ';hit.num=', $next, ';brand=', $brand, $search)"/>
               </xsl:otherwise>
            </xsl:choose>
            <img src="{$icon.path}b_innext.gif" border="0" alt="next hit"/>
         </a>
      </xsl:if>
   </xsl:template>
   
</xsl:stylesheet>
