<xsl:stylesheet version="2.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"   
  xmlns:oac="http://oac.cdlib.org">
  <!--
      Copyright (c) 2011, Regents of the University of California
      All rights reserved.
      BSD License, see bottom of file
   -->

<xsl:function name="oac:supply-heading">
	<xsl:param name="element"/>
	<xsl:variable name="element-name" select="name($element)"/>
	<xsl:choose>
	<!-- supply headings if not present -->
		<xsl:when test="$element-name = 'dsc'">
			<xsl:text>Container List</xsl:text>
		</xsl:when>
		<xsl:when test="$element-name = 'accessrestrict'">
			<xsl:text>Access Information</xsl:text>
		</xsl:when>
		<xsl:when test="$element-name = 'accurals'">
			<xsl:text>Future Additions</xsl:text>
		</xsl:when>
		<xsl:when test="$element-name = 'acquinfo'">
			<xsl:text>Acquisition Information</xsl:text>
		</xsl:when>
		<xsl:when test="$element-name = 'altformavail'">
			<xsl:text>Alternative For of Materials Available</xsl:text>
		</xsl:when>
		<xsl:when test="$element-name = 'appraisal'">
			<xsl:text>Appraisal Information</xsl:text>
		</xsl:when>
		<xsl:when test="$element-name = 'arrangement'">
			<xsl:text>Arrangement</xsl:text>
		</xsl:when>
		<xsl:when test="$element-name = 'bibliography'">
			<xsl:text>Bibliography</xsl:text>
		</xsl:when>
		<xsl:when test="$element-name = 'bioghist'">
			<xsl:text>Biography/Organization History</xsl:text>
		</xsl:when>
		<xsl:when test="$element-name = 'controlaccess'">
			<xsl:text>Subjects and Indexing Terms</xsl:text>
		</xsl:when>
		<xsl:when test="$element-name = 'custodhist'">
			<xsl:text>Custodial History</xsl:text>
		</xsl:when>
		<xsl:when test="$element-name = 'descgrp'">
			<xsl:text>Administrative Information</xsl:text>
		</xsl:when>
		<xsl:when test="$element-name = 'fileplan'">
			<xsl:text>Original Filing System</xsl:text>
		</xsl:when>
		<xsl:when test="$element-name = 'index'">
			<xsl:text>Index</xsl:text>
		</xsl:when>
		<xsl:when test="$element-name = 'note'">
			<xsl:text>Note</xsl:text>
		</xsl:when>
		<xsl:when test="$element-name = 'odd'">
			<xsl:text>Additional Note</xsl:text>
		</xsl:when>
		<xsl:when test="$element-name = 'originalsloc'">
			<xsl:text>Location of Original Material</xsl:text>
		</xsl:when>
		<xsl:when test="$element-name = 'phystech'">
			<xsl:text>Technical Details</xsl:text>
		</xsl:when>
		<xsl:when test="$element-name = 'perfercite'">
			<xsl:text>How to Cite</xsl:text>
		</xsl:when>
		<xsl:when test="$element-name = 'processinfo'">
			<xsl:text>Processing/Project Information</xsl:text>
		</xsl:when>
		<xsl:when test="$element-name = 'relatedmaterial'">
			<xsl:text>Related Material</xsl:text>
		</xsl:when>
		<xsl:when test="$element-name = 'scopecontent'">
			<xsl:text>Scope and Content Note</xsl:text>
		</xsl:when>
		<xsl:when test="$element-name = 'seperatedmaterial'">
			<xsl:text>Removed or Separated Material</xsl:text>
		</xsl:when>
		<xsl:when test="$element-name = 'userestrict'">
			<xsl:text>Conditions of Use</xsl:text>
		</xsl:when>
	</xsl:choose>
</xsl:function>
<!--       Copyright (c) 2011, Regents of the University of California
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
