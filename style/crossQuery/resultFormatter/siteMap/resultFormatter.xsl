<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   exclude-result-prefixes="#all" version="2.0">

   <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
   <!-- Site map result formatter stylesheet                                   -->
   <!-- (see copyright notice at end of file)                                  -->
   <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->

   <!-- 
      This stylesheet implements a simple result formatter for generating
      dynamic site maps.
   -->

   <!-- ====================================================================== -->
   <!-- Import Common Templates                                                -->
   <!-- ====================================================================== -->
   
   <xsl:import href="../common/resultFormatterCommon.xsl"/>
   <xsl:param name="icon.path" select="concat($xtfURL, 'icons/default/')"/>
   
   <!-- ====================================================================== -->
   <!-- Output                                                                 -->
   <!-- ====================================================================== -->

   <xsl:output method="xml" encoding="UTF-8" media-type="text/xml" indent="yes"
      exclude-result-prefixes="#all"/>

   <!-- ====================================================================== -->
   <!-- Root Template                                                          -->
   <!-- ====================================================================== -->
   <xsl:template match="/">
      <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://www.sitemaps.org/schemas/sitemap/0.9 http://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd">
         <xsl:apply-templates select="crossQueryResult/docHit" mode="siteMap" exclude-result-prefixes="#all"/>
      </urlset>
   </xsl:template>

   <xsl:template match="docHit" mode="siteMap" exclude-result-prefixes="#all">

      <xsl:variable name="identifier" select="meta/identifier"/>
      <xsl:variable name="id" select="replace(@path,'^default:','')"/>
      <xsl:variable name="loc" select="concat($xtfURL,'view?docId=',$id)"/>

      <url xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
         <loc xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
            <xsl:value-of select="$loc"/>
         </loc>
         <changefreq xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">monthly</changefreq>
         <lastmod xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
            <xsl:value-of select="meta/dateStamp"/>
         </lastmod>
      </url>

   </xsl:template>


   <!--
      Copyright (c) 2012, Regents of the University of California
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

</xsl:stylesheet>
