<!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
<!-- Index document selection stylesheet                                    -->
<!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
        xmlns:saxon="http://saxon.sf.net/"
        xmlns:xtf="http://cdlib.org/xtf"
        xmlns:date="http://exslt.org/dates-and-times"
        extension-element-prefixes="date"
        exclude-result-prefixes="#all">

<!--
   Copyright (c) 2004, Regents of the University of California
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
    When the textIndexer tool encounters a directory filled with possible 
    files to index, it passes them to this stylesheet for evaluation. Our job
    here is to decide which of the files to index, and specify various
    parameters for those we do decide to index.

    Specifically, the stylesheet receives a document that looks like this:
      
    <directory dirPath="{path of the directory}">
        <file fileName="{file name #1}"/>
        <file fileName="{file name #2}"/>
        ...etc...
    </directory>
    
    The output of this stylesheet should be an XML document like this:
      
    <indexFiles>
        <indexFile dirPath="{path of the directory}"
                   fileName="{file name #1}"
                   format="{XML|PDF|HTML|...}"
                   inputFilter="{path to input filter stylesheet}"
                   displayStyle="{path to display stylesheet}"/>
        <indexFile .../>
        ...etc...
    </indexFiles>

    Notes on the output tags:
      
      - If no files should be indexed, the output document should be empty.
      
      - All filesystem paths are relative to the XTF home directory.
      
      - The 'dirPath' and 'fileName' attributes are required
      
      - The other attributes ('format', 'inputFilter' and 'displayStyle') are
        all optional.
        
      - If the 'format' attribute is not specified, the textIndexer will 
        attempt to deduce the file's format based on its file name extension.
        
      - If 'inputFilter' isn't specified, no pre-filtering will be performed
        on the source document.
        
      - If 'displayStyle' isn't specified, no XSL keys will be pre-computed
        (see below for more info on displayStyle.)

    What is 'displayStyle' all about? Well, stylesheet processing can be 
    optimized by using XSLT 'keys', which are declared with an <xsl:key> tag.
    The first time a key is used in a given source document, it must be 
    calculated and its values stored on disk. The text indexer can optionally 
    pre-compute the keys so they need not be calculated later during the 
    display process. 
    
    The 'displayStyle' attribute simply specifies a stylesheet that the
    text indexer will look in to gather XSLT key definitions. Then it will
    pre-compute all of these keys for the document and store them on disk.
    
-->

<!-- ====================================================================== -->
<!-- Templates                                                              -->
<!-- ====================================================================== -->
  
  <xsl:template match="directory">
    <indexFiles>
      <xsl:apply-templates/>
    </indexFiles>
  </xsl:template>

  <xsl:template match="file">
    <xsl:if test="ends-with(@fileName, '.xml')">
      <xsl:if test="not(ends-with(@fileName, '.mets.xml')) and
                    not(ends-with(@fileName, '.dc.xml'))">
        <indexFile fileName="{@fileName}"
                   inputFilter="style/textIndexer/default/prefilter.xsl"
                   displayStyle="style/dynaXML/docFormatter/default/docFormatter.xsl"/>
      </xsl:if>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
