<xsl:stylesheet version="2.0" 
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:xtf="http://cdlib.org/xtf"
   exclude-result-prefixes="#all">
   
   <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
   <!-- ERC (Electronic Resource Citations) Stylesheet                         -->
   <!-- See the ERC spec: http://www.cdlib.org/inside/diglib/ark/ercspec.html  -->
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
   
   <!-- ====================================================================== -->
   <!-- Output Format                                                          -->
   <!-- ====================================================================== -->
   
   <xsl:output method="text" encoding="UTF-8" media-type="text/plain; charset=UTF-8"/>
   
   <!-- ====================================================================== -->
   <!-- Global parameters and variables                                        -->
   <!-- ====================================================================== -->
   
   <xsl:param name="http.URL"/>
   <xsl:variable name="ercPat" select="'^(http://[^?]+)/erc/([^?]+)\?q$'"/>
   <xsl:param name="docId">
      <!-- Normally this is a URL parameter, but in ERC mode it's part of the main URL. -->
      <xsl:value-of select="replace($http.URL, $ercPat, '$2')"/>
   </xsl:param>
   
   <!-- ====================================================================== -->
   <!-- Root Template                                                          -->
   <!-- ====================================================================== -->
   
   <!-- Do not indent these lines. -->
<xsl:template match="/">erc:
who:   <xsl:value-of select="//creator[@xtf:meta][1]"/>
what:  <xsl:value-of select="//title[@xtf:meta][1]"/>
when:  <xsl:value-of select="//date[@xtf:meta][1]"/>
where: <xsl:value-of select="replace($http.URL, $ercPat, '$1/view?docId=$2')"/>
</xsl:template>

</xsl:stylesheet>
