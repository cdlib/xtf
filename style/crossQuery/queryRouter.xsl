<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   exclude-result-prefixes="#all"
   version="2.0">
   
   <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
   <!-- Simple query router stylesheet                                         -->
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
      This stylesheet implements a simple switching mechanism, allowing one to
      set up multiple distinct crossQuery domains, each with its own query parser
      and associated stylesheets.
      
      As shipped, there are two domains, "default" and "oai", which supports OAI 
      harvesting of your collections
   -->
   
   <xsl:output method="xml" indent="yes" encoding="utf-8"/>
   <xsl:strip-space elements="*"/>
   
   <!-- ====================================================================== -->
   <!-- Root Template                                                          -->
   <!-- ====================================================================== -->
   
   <xsl:param name="http.URL"/>
   <xsl:param name="smode"/>
   
   <xsl:template match="/">
      
      <route>
         <xsl:choose>
            <!-- oai -->
            <xsl:when test="matches($http.URL,'oai\?')">
               <queryParser path="style/crossQuery/queryParser/oai/queryParser.xsl"/>
               <errorGen path="style/crossQuery/oaiErrorGen.xsl"/>
            </xsl:when>
            <!-- sitemap -->
            <xsl:when test="matches($smode,'siteMap')">
               <queryParser path="style/crossQuery/queryParser/siteMap/queryParser.xsl"/>
            </xsl:when>
            <!-- default -->
            <xsl:otherwise>
               <queryParser path="style/crossQuery/queryParser/default/queryParser.xsl"/>
            </xsl:otherwise>
         </xsl:choose>
      </route>
   </xsl:template>
   
</xsl:stylesheet>
