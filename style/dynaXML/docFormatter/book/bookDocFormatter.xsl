<xsl:stylesheet version="2.0" 
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:xtf="http://cdlib.org/xtf"
   xmlns:session="java:org.cdlib.xtf.xslt.Session"
   xmlns:editURL="http://cdlib.org/xtf/editURL"
   xmlns="http://www.w3.org/1999/xhtml"
   extension-element-prefixes="session"
   exclude-result-prefixes="#all">
   
   <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
   <!-- BookReader dynaXML Stylesheet                                          -->
   <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
   
   <!--
      Copyright (c) 2010, Regents of the University of California
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
      NOTE: This is a stab at providing XTF access to scanned books, using the
      Open Library BookReader.
   -->
   
   <!-- ====================================================================== -->
   <!-- Import Common Templates                                                -->
   <!-- ====================================================================== -->
   
   <xsl:import href="../common/docFormatterCommon.xsl"/>
   <xsl:import href="../../../common/editURL.xsl"/>
   
   <!-- ====================================================================== -->
   <!-- Output Format                                                          -->
   <!-- ====================================================================== -->
   
   <xsl:output method="xhtml" indent="yes" 
      encoding="UTF-8" media-type="text/html; charset=UTF-8" 
      doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN" 
      doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd" 
      exclude-result-prefixes="#all"
      omit-xml-declaration="yes"/>
   
   <!-- ====================================================================== -->
   <!-- Strip Space                                                            -->
   <!-- ====================================================================== -->
   
   <xsl:strip-space elements="*"/>
   
   <!-- ====================================================================== -->
   <!-- Included Stylesheets                                                   -->
   <!-- ====================================================================== -->
   
   <xsl:include href="search.xsl"/>
   
   <!-- ====================================================================== -->
   <!-- Define Keys                                                            -->
   <!-- ====================================================================== -->
   
   <xsl:key name="div-id" match="sec" use="@id"/>
   
   <!-- ====================================================================== -->
   <!-- Define Parameters                                                      -->
   <!-- ====================================================================== -->
   
   <xsl:param name="root.URL"/>
   <xsl:param name="doc.title" select="/xtf-converted-book/xtf:meta/title"/>
   <!-- for docFormatterCommon.xsl -->
   <xsl:param name="css.path" select="'css/default/'"/>
   <xsl:param name="icon.path" select="'css/default/'"/>
   
   <!-- ====================================================================== -->
   <!-- Root Template                                                          -->
   <!-- ====================================================================== -->
   
   <xsl:template match="/">
      <xsl:choose>
         <!-- robot solution -->
         <xsl:when test="matches($http.user-agent,$robots)">
            <xsl:call-template name="robot"/>
         </xsl:when>
         <xsl:when test="$doc.view='citation'">
            <xsl:call-template name="citation"/>
         </xsl:when>
         <xsl:when test="$doc.view='print'">
            <xsl:call-template name="print"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:call-template name="content"/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Anchor Template                                                        -->
   <!-- ====================================================================== -->
   
   <xsl:template name="create.anchor">
      <xsl:choose>
         <xsl:when test="($query != '0' and $query != '') and $hit.rank != '0'">
            <xsl:text>#</xsl:text><xsl:value-of select="key('hit-rank-dynamic', $hit.rank)/@hitNum"/>
         </xsl:when>
         <xsl:when test="$anchor.id != '0' and $anchor.id != ''">
            <xsl:choose>
               <xsl:when test="key('div-id',$anchor.id)/@xtf:firstHit">
                  <xsl:text>#</xsl:text><xsl:value-of select="key('div-id',$anchor.id)/@xtf:firstHit"/>
               </xsl:when>
               <xsl:otherwise>
                  <xsl:text>#</xsl:text><xsl:value-of select="$anchor.id"/>
               </xsl:otherwise>
            </xsl:choose>
         </xsl:when>
         <xsl:when test="$query != '0' and $query != ''">
            <xsl:text>#</xsl:text><xsl:value-of select="/*/@xtf:firstHit"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:text>#X</xsl:text>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <!-- ====================================================================== -->
   <!-- Content Template                                                       -->
   <!-- ====================================================================== -->
   
   <xsl:template name="content">
      
      <html xml:lang="en" lang="en">
         <head>
            <title>
               <xsl:value-of select="$doc.title"/>
            </title>
            <link rel="stylesheet" type="text/css" href="{$root.URL}css/bookreader/BookReader.css"/>
            <script type="text/javascript" src="{$root.URL}script/bookreader/jquery-1.2.6.min.js"></script>
            <script type="text/javascript" src="{$root.URL}script/bookreader/jquery.easing.1.3.js"></script>
            <script type="text/javascript" src="{$root.URL}script/bookreader/BookReader.js"></script>    
            <script type="text/javascript" src="{$root.URL}script/bookreader/dragscrollable.js"></script>
            <link rel="stylesheet" type="text/css" href="{$root.URL}css/default/bbar.css" /> 
         </head>
         <body>
            <!-- Standard button bar, but not using frames -->
            <xsl:variable name="bbarPage">
               <xsl:call-template name="bbar"/>
            </xsl:variable>
            <xsl:copy-of select="$bbarPage//*:body/*"/>
            
            <!-- Dynamic javascript for this book -->
            <script type="text/javascript">
               <xsl:comment> Dynamic javascript with specific page parameters for this book. </xsl:comment>
               
               br = new BookReader();
               
               br.titleLeaf = <xsl:value-of select="(/*/leaf[@type='Title Page']/@leafNum, 1)[1]"/>;
               
               //   We have three numbering systems to deal with here:
               //   1. "Leaf": Numbered sequentially starting at 1. This is a page from the original scan. 
               //              Most of these are to be shown but a few (e.g. color cards) should be suppressed
               //              for pleasant viewing. Hence...
               //              
               //   2. "Index": Also sequential starting at 1, but representing only the viewable pages. So the
               //               number of index values is less than or equal to the number of leaves.
               //               Example: if pages 3-9 are not viewable, the mapping from Index to Leaf would be:
               //               
               //                  index 1 : leaf 1
               //                  index 2 : leaf 2
               //                  index 3 : leaf 10
               //                  index 4 : leaf 11
               //                  etc.
               //               
               //   3. "Page": This is the human-readable, logical page number. So for instance, the cover page
               //              doesn't count, and embedded color plates are often not numbered. The number of
               //              page numbers is less than or equal to the number of indexes. Continuing our
               //              example from above, let's say that "Page 1" of the book is leaf 10. The
               //              mapping from page to index to leaf is:
               //              
               //                 page 1 : index 3 : leaf 10
               //                 page 2 : index 4 : leaf 11
               //                 etc.
               
               br.numLeafs <!--sic--> = <xsl:value-of select="max(/*/leaf/number(@leafNum))"/>;
               
               br.pageNumToPage = {};
               br.indexToPage = {};
               br.leafToPage = {};
               
               br.Page = function(w, h, imgFile, leafNum, indexNum, pageNum) {
                  this.w = w;
                  this.h = h;
                  this.imgFile = imgFile;
                  this.leafNum = leafNum;
                  this.indexNum = indexNum;
                  this.pageNum = pageNum;
                  
                  br.leafToPage[indexNum] = this;
                  br.indexToPage[indexNum] = this;
                  br.pageNumToPage[pageNum] = this;
               };
               
               br.getPageWidth = function(index) {
                  return br.indexToPage[index].w;
               }
               
               br.getPageHeight = function(index) {
                  return br.indexToPage[index].h;
               }
               
               br.canRotatePage = function(index) { return false; } // We don't support rotation (yet anyway)
               
               br.pages = [
                  <xsl:variable name="quote" select="'&quot;'"/>
                  <xsl:for-each select="/*/leaf[@access='true']">new br.Page(<xsl:value-of select="
                     concat(
                        cropBox/@w, ',', 
                        cropBox/@h, ',',
                        $quote, @imgFile, $quote, ',',
                        @leafNum,   ',',
                        position(), ',',
                        if (@pageNum &gt; 0) then @pageNum else 'null')"/>),
                  </xsl:for-each>
               ];
            </script>
         </body>
      </html>
   </xsl:template>
   
   
   <!-- ====================================================================== -->
   <!-- Print Template                                                         -->
   <!-- ====================================================================== -->
   
   <xsl:template name="print">
      <html xml:lang="en" lang="en">
         <head>
            <title>
               <xsl:value-of select="$doc.title"/>
            </title>
            <link rel="stylesheet" type="text/css" href="{$root.URL}/css/bookreader/BookReader.css"/>
         </head>
         <body bgcolor="white">
            <hr class="hr-title"/>
            <div align="center">
               <table width="95%">
                  <tr>
                     <td>
                        <xsl:apply-templates select="/article"/>
                     </td>
                  </tr>
               </table>
            </div>
            <hr class="hr-title"/>
         </body>
      </html>
   </xsl:template>
   
</xsl:stylesheet>
