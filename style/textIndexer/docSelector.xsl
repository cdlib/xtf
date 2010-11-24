<!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
<!-- Index document selection stylesheet                                    -->
<!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
   xmlns:FileUtils="java:org.cdlib.xtf.xslt.FileUtils"
   extension-element-prefixes="FileUtils"
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
        <indexFile fileName="{file name #1}"
                   type="{XML|PDF|HTML|MSWord...}"
                   preFilter="{path to input filter stylesheet}"
                   displayStyle="{path to display stylesheet}"/>
        <indexFile .../>
        ...etc...
    </indexFiles>

    Notes on the output tags:
      
      - If no files should be indexed, the output document should be empty.
      
      - If you use relative paths for the input filter or display style 
        attributes, they will be interpreted as being relative
        to XTF_HOME.
      
      - The 'fileName' attribute is required, and SHOULD NOT contain path
        information. Essentially, this should be one of the file names 
        from an input <file... /> tag.
      
      - The other attributes ('type', 'preFilter' and 'displayStyle') are
        all optional.
        
      - If the 'type' attribute is not specified, the textIndexer will 
        attempt to deduce the file's format based on its file name extension.
        
      - If 'preFilter' isn't specified, no pre-filtering will be performed
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
      <xsl:variable name="dirPath" select="parent::*/@dirPath"/>
      <xsl:choose>
         <!-- XML files -->
         <xsl:when test="ends-with(@fileName, '.xml')">
            
            <xsl:choose>
               <!-- Skip document-less METS and DC files -->
               <xsl:when test="ends-with(@fileName, '.mets.xml') or ends-with(@fileName, '.dc.xml')"/>

               <!-- Skip bookreader page files -->
               <xsl:when test="matches($dirPath, '^.*bookreader') and matches(@fileName, '\d{8}\.xml')"/>
               
               <!-- All other XML files -->
               <xsl:otherwise>
                  
                  <xsl:variable name="fileName" select="@fileName"/>
                  <xsl:variable name="file" select="concat($dirPath,$fileName)"/>
                  
                  <!-- We need to determine what kind of XML file we're looking at. XTF provides a
                       handy function that quickly reads in only the first part of an XML file
                       (up to the first close element tag, e.g. </element>). We make our decision
                       based on the name of the root element, the entity information, and namespace.
                       
                       Note that the "unparsed-entity-public-id" and "unparsed-entity-uri" XPath
                       functions operate on whatever document is the current context. We use
                       <xsl:for-each> to switch to the target document's context, rather than the
                       context of the input we received from the textIndexer. In this case,
                       "for-each" is a bit of a misnomer, since the stub is a single document
                       so the code below runs only once.
                  -->
                  <xsl:for-each select="FileUtils:readXMLStub($file)">
                     
                     <xsl:variable name="root-element-name" select="name(*[1])"/>
                     <xsl:variable name="pid" select="unparsed-entity-public-id($root-element-name)"/>
                     <xsl:variable name="uri" select="unparsed-entity-uri($root-element-name)"/>
                     <xsl:variable name="ns" select="namespace-uri(*[1])"/>
                     
                     <xsl:choose>
                        <!-- Look for EAD XML files -->
                        <xsl:when test="matches($root-element-name,'^ead$') or
                                        matches($pid,'EAD') or 
                                        matches($uri,'ead\.dtd') or 
                                        matches($ns,'ead')">
                           <indexFile fileName="{$fileName}"
                              preFilter="style/textIndexer/ead/eadPreFilter.xsl"
                              displayStyle="style/dynaXML/docFormatter/ead/eadDocFormatter.xsl"/>
                        </xsl:when>
                        <!-- Look for NLM XML files -->
                        <xsl:when test="matches($root-element-name,'^article$') or
                                        matches($pid,'NLM') or 
                                        matches($uri,'journalpublishing\.dtd') or 
                                        matches($ns,'nlm')">
                           <indexFile fileName="{$fileName}"
                              preFilter="style/textIndexer/nlm/nlmPreFilter.xsl"
                              displayStyle="style/dynaXML/docFormatter/nlm/nlmDocFormatter.xsl"/>
                        </xsl:when>
                        <!-- Look for TEI XML file -->
                        <xsl:when test="matches($root-element-name,'^TEI') or 
                           matches($pid,'TEI') or 
                           matches($uri,'tei2\.dtd') or 
                           matches($ns,'tei')">
                           <indexFile fileName="{$fileName}"
                              preFilter="style/textIndexer/tei/teiPreFilter.xsl"
                              displayStyle="style/dynaXML/docFormatter/tei/teiDocFormatter.xsl"/>
                        </xsl:when>
                        <!-- DjVu files are typically subordinate to a main doc -->
                        <xsl:when test="matches($root-element-name, 'DjVuXML')">
                           <!-- skip -->
                        </xsl:when>
                        <!-- Look for METS-encoded scanned books, skip other METS files as likely subordinate -->
                        <xsl:when test="matches($root-element-name,'^METS')">
                           <xsl:variable name="metsData" select="document($file)"/>
                           <xsl:if test="$metsData//*:book">
                              <indexFile fileName="{$fileName}"
                                 preFilter="style/textIndexer/bookreader/bookPreFilter.xsl"
                                 displayStyle="style/dynaXML/docFormatter/bookreader/bookDocFormatter.xsl"/>
                           </xsl:if>
                        </xsl:when>
                        <!-- Default processing for XML files -->
                        <xsl:otherwise>
                           <indexFile fileName="{$fileName}" 
                              type="XML"
                              preFilter="style/textIndexer/default/defaultPreFilter.xsl"/>
                           <xsl:message select="'Unrecognized XML structure. Indexing using the default preFilter.'"/>
                        </xsl:otherwise>
                     </xsl:choose>
                  </xsl:for-each>
               </xsl:otherwise>
            </xsl:choose>
         </xsl:when>
         
         <!-- HTML files -->
         <xsl:when test="ends-with(@fileName, 'html') or ends-with(@fileName, '.xhtml')">
            <indexFile fileName="{@fileName}" 
               type="HTML"
               preFilter="style/textIndexer/html/htmlPreFilter.xsl"/>
         </xsl:when>
         
         <!-- PDF files -->
         <xsl:when test="ends-with(@fileName, '.pdf')">
            <indexFile fileName="{@fileName}" 
               type="PDF"
               preFilter="style/textIndexer/default/defaultPreFilter.xsl"/>
         </xsl:when>
         
         <!-- Microsoft Word documents -->
         <xsl:when test="ends-with(@fileName, '.doc')">
            <indexFile fileName="{@fileName}" 
               type="MSWord"
               preFilter="style/textIndexer/default/defaultPreFilter.xsl"/>
         </xsl:when>
         
         <!-- Plain text files. Exception: skip book/*.txt as they're typically subordinate. -->
         <xsl:when test="ends-with(@fileName, '.txt') and not(matches($dirPath, '/bookreader/'))">
            <indexFile fileName="{@fileName}" 
               type="text"
               preFilter="style/textIndexer/default/defaultPreFilter.xsl"/>
         </xsl:when>
      </xsl:choose>
      
   </xsl:template>

</xsl:stylesheet>
