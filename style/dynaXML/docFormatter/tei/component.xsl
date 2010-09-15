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
   
   <!-- ====================================================================== -->
   <!-- Heads                                                                  -->
   <!-- ====================================================================== -->
   
   <xsl:template match="*:head">
      
      <xsl:variable name="type" select="parent::*/@type"/>
      
      <xsl:variable name="class">
         <xsl:choose>
            <xsl:when test="@rend">
               <xsl:value-of select="@rend"/>
            </xsl:when>
            <xsl:otherwise>normal</xsl:otherwise>
         </xsl:choose>
      </xsl:variable>
      
      <xsl:choose>
         <xsl:when test="@type='sub' or @type='subtitle'">
            <!-- Needs more choices here -->
            <h3 class="{$class}"><xsl:apply-templates/></h3>
         </xsl:when>
         <xsl:when test="$type='fmsec'">
            <h2 class="{$class}"><xsl:apply-templates/></h2>
         </xsl:when>
         <xsl:when test="$type='volume'">
            <h1 class="{$class}">
               <xsl:if test="parent::*/@n">
                  <xsl:value-of select="parent::*/@n"/><xsl:text>. </xsl:text>
               </xsl:if>
               <xsl:apply-templates/>
            </h1>
         </xsl:when>
         <xsl:when test="$type='part'">
            <h1 class="{$class}">
               <xsl:if test="parent::*/@n">
                  <xsl:value-of select="parent::*/@n"/><xsl:text>. </xsl:text>
               </xsl:if>
               <xsl:apply-templates/>
            </h1>
         </xsl:when>
         <xsl:when test="$type='chapter'">
            <h2 class="{$class}">
               <xsl:if test="parent::*/@n">
                  <xsl:value-of select="parent::*/@n"/><xsl:text>. </xsl:text>
               </xsl:if>
               <xsl:apply-templates/>
            </h2>
         </xsl:when>
         <xsl:when test="$type='ss1'">
            <h3 class="{$class}">
               <xsl:if test="parent::*/@n">
                  <xsl:value-of select="parent::*/@n"/><xsl:text>. </xsl:text>
               </xsl:if>
               <xsl:apply-templates/>
            </h3>
         </xsl:when>
         <xsl:when test="$type='ss2'">
            <h3 class="{$class}"><xsl:apply-templates/></h3>
         </xsl:when>
         <xsl:when test="$type='ss3'">
            <h3 class="{$class}"><xsl:apply-templates/></h3>
         </xsl:when>
         <xsl:when test="$type='ss4'">
            <h4 class="{$class}"><xsl:apply-templates/></h4>
         </xsl:when>
         <xsl:when test="$type='ss5'">
            <h4 class="{$class}"><xsl:apply-templates/></h4>
         </xsl:when>
         <xsl:when test="$type='bmsec'">
            <h2 class="{$class}"><xsl:apply-templates/></h2>
         </xsl:when>
         <xsl:when test="$type='appendix'">
            <h2 class="{$class}">
               <xsl:if test="parent::*/@n">
                  <xsl:value-of select="parent::*/@n"/><xsl:text>. </xsl:text>
               </xsl:if>
               <xsl:apply-templates/>
            </h2>
         </xsl:when>
         <xsl:when test="$type='endnotes'">
            <h3 class="{$class}"><xsl:apply-templates/></h3>
         </xsl:when>
         <xsl:when test="$type='bibliography'">
            <h2 class="{$class}"><xsl:apply-templates/></h2>
         </xsl:when>
         <xsl:when test="$type='glossary'">
            <h2 class="{$class}"><xsl:apply-templates/></h2>
         </xsl:when>
         <xsl:when test="$type='index'">
            <h2 class="{$class}"><xsl:apply-templates/></h2>
         </xsl:when>
         <xsl:otherwise>
            <h4 class="{$class}"><xsl:apply-templates/></h4>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <xsl:template match="*:docAuthor">
      <h4><xsl:apply-templates/></h4>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Verse                                                                  -->
   <!-- ====================================================================== -->
   
   <xsl:template match="*:lg">
      <xsl:choose>
         <xsl:when test="@type='stanza' and parent::lg">
            <tr>
               <td colspan="2">
                  <br/><table border="0" cellspacing="0" cellpadding="0"><xsl:apply-templates/></table>
               </td>
            </tr>
         </xsl:when>
         <xsl:otherwise>
            <table border="0" cellspacing="0" cellpadding="0"><xsl:apply-templates/></table>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <xsl:template match="*:l">
      <xsl:choose>
         <xsl:when test="parent::lg">
            <tr>
               <td width="30">
                  <xsl:choose>
                     <xsl:when test="@n">
                        <span class="run-head"><xsl:value-of select="@n"/></span>
                     </xsl:when>
                     <xsl:otherwise>
                        <xsl:text> </xsl:text>
                     </xsl:otherwise>
                  </xsl:choose>
               </td>
               <td>
                  <xsl:apply-templates/>
               </td>
            </tr>
         </xsl:when>
         <xsl:otherwise>
            <xsl:if test="@n">
               <span class="run-head"><xsl:value-of select="@n"/></span>
            </xsl:if>
            <xsl:apply-templates/><br/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <xsl:template match="*:seg">
      <xsl:if test="position() > 1">
         <xsl:text>&#160;&#160;&#160;&#160;</xsl:text>
      </xsl:if>
      <xsl:apply-templates/><br/>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Speech                                                                 -->
   <!-- ====================================================================== -->
   
   <xsl:template match="*:sp">
      <xsl:apply-templates/><br/>
   </xsl:template>
   
   <xsl:template match="*:speaker">
      <b><xsl:apply-templates/></b>
   </xsl:template>
   
   <xsl:template match="*:sp/*:p">
      <p class="noindent"><xsl:apply-templates/></p>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Lists                                                                  -->
   <!-- ====================================================================== -->
   
   <xsl:template match="*:list">
      <xsl:choose>
         <xsl:when test="@type='gloss'">
            <dl><xsl:apply-templates/></dl>
         </xsl:when>
         <xsl:when test="@type='simple'">
            <ul class="nobull"><xsl:apply-templates/></ul>
         </xsl:when>
         <xsl:when test="@type='ordered'">
            <xsl:choose>
               <xsl:when test="@rend='alpha'">
                  <ol class="alpha"><xsl:apply-templates/></ol>
               </xsl:when>
               <xsl:otherwise>
                  <ol><xsl:apply-templates/></ol>
               </xsl:otherwise>
            </xsl:choose>
         </xsl:when>
         <xsl:when test="@type='unordered'">
            <ul><xsl:apply-templates/></ul>
         </xsl:when>
         <xsl:when test="@type='bulleted'">
            <xsl:choose>
               <xsl:when test="@rend='dash'">
                  <ul class="nobull"><xsl:text>- </xsl:text><xsl:apply-templates/></ul>
               </xsl:when>
               <xsl:otherwise>
                  <ul><xsl:apply-templates/></ul>
               </xsl:otherwise>
            </xsl:choose>
         </xsl:when>
         <xsl:when test="@type='bibliographic'">
            <ol><xsl:apply-templates/></ol>
         </xsl:when>
         <xsl:when test="@type='special'">
            <ul><xsl:apply-templates/></ul>
         </xsl:when>
      </xsl:choose>
   </xsl:template>
   
   <xsl:template match="*:item">
      <xsl:choose>
         <xsl:when test="parent::list[@type='gloss']">
            <dd><xsl:apply-templates/></dd>
         </xsl:when>
         <xsl:otherwise>
            <li><xsl:apply-templates/></li>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <xsl:template  match="*:label">
      <dt><xsl:apply-templates/></dt>
   </xsl:template>
   
   <xsl:template match="*:name">
      <xsl:apply-templates/>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Notes                                                                  -->
   <!-- ====================================================================== -->
   
   <xsl:template match="*:note">
      <xsl:choose>
         <xsl:when test="@type='footnote' or @place='foot'">
            <xsl:if test="$doc.view='popup' or $doc.view='print'">
               <xsl:apply-templates/>
            </xsl:if>
         </xsl:when>
         <xsl:when test="@type='endnote' or @place='end'">
            <xsl:choose>
               <xsl:when test="$anchor.id=@*:id">
                  <a name="X"></a>
                  <div class="note-hi">
                     <xsl:apply-templates/>
                  </div>
               </xsl:when>
               <xsl:otherwise>
                  <div class="note">
                     <xsl:apply-templates/>
                  </div>
               </xsl:otherwise>
            </xsl:choose>
         </xsl:when>
         <xsl:when test="@type='note' or @place='inline'">
            <div class="inline-note">
               <xsl:apply-templates/>
            </div>
         </xsl:when>
         <xsl:otherwise>
            <div class="note">
               <xsl:apply-templates/>
            </div>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <xsl:template match="*:p[ancestor::note[@type='footnote' or @place='foot']]">
      
      <xsl:variable name="n" select="parent::note/@n"/>
      
      <p>
         <xsl:if test="position()=1">
            <xsl:if test="$n != ''">
               <xsl:text>[</xsl:text><xsl:value-of select="$n"/><xsl:text>] </xsl:text>
            </xsl:if>
         </xsl:if>
         <xsl:apply-templates/>
      </p>
      
   </xsl:template>
   
   <xsl:template match="*:p[ancestor::note[@type='endnote' or @place='end']]">
      
      <xsl:variable name="n" select="parent::note/@n"/>
      
      <xsl:variable name="class">
         <xsl:choose>
            <xsl:when test="position()=1">noindent</xsl:when>
            <xsl:otherwise>indent</xsl:otherwise>
         </xsl:choose>
      </xsl:variable>
      
      <p class="{$class}">
         <xsl:if test="position()=1">
            <xsl:if test="$n != ''">
               <xsl:value-of select="$n"/><xsl:text>. </xsl:text>
            </xsl:if>
         </xsl:if>
         <xsl:apply-templates/>
         <xsl:if test="position()=last()">
            <xsl:if test="parent::note/@corresp">
               
               <xsl:variable name="corresp" select="parent::note/@corresp"/>
               
               <xsl:variable name="chunk" select="key('ref-id', $corresp)/ancestor::*[matches(local-name(), '^div[1-6]$')][1]/@*:id"/>
               
               <xsl:variable name="toc" select="key('div-id', $chunk)/parent::*/@*:id"/>
               
               <span class="down1">
                  <xsl:text> [</xsl:text>
                  <a>
                     <xsl:attribute name="href"><xsl:value-of select="$doc.path"/>&#038;chunk.id=<xsl:value-of select="$chunk"/>&#038;toc.id=<xsl:value-of select="$toc"/>&#038;toc.depth=<xsl:value-of select="$toc.depth"/>&#038;brand=<xsl:value-of select="$brand"/><xsl:value-of select="$search"/>&#038;anchor.id=<xsl:value-of select="$corresp"/>#X</xsl:attribute>
                     <xsl:attribute name="target">_top</xsl:attribute>
                     <xsl:text>BACK</xsl:text>
                  </a>
                  <xsl:text>]</xsl:text>
               </span>
            </xsl:if>
         </xsl:if>
      </p>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Paragraphs                                                             -->
   <!-- ====================================================================== -->
   
   <xsl:template match="*:p[not(ancestor::note[@type='endnote' or @place='end'])]">
      
      <xsl:choose>
         <xsl:when test="@rend='center'">
            <p class="center"><xsl:apply-templates/></p>
         </xsl:when>
         <xsl:when test="name(preceding-sibling::node()[1])='pb'">
            <p class="noindent"><xsl:apply-templates/></p>
         </xsl:when>
         <xsl:when test="parent::td">
            <p><xsl:apply-templates/></p>
         </xsl:when>
         <xsl:when test="contains(@rend, 'IndentHanging')">
            <p class="{@rend}"><xsl:apply-templates/></p>
         </xsl:when>
         <xsl:when test="not(preceding-sibling::p)">
            <xsl:choose>
               <xsl:when test="@rend='hang'">
                  <p class="hang"><xsl:apply-templates/></p>
               </xsl:when>
               <xsl:when test="@rend='indent'">
                  <p class="indent"><xsl:apply-templates/></p>
               </xsl:when>
               <xsl:when test="@rend='noindent'">
                  <p class="noindent"><xsl:apply-templates/></p>
               </xsl:when>
               <xsl:otherwise>
                  <p class="noindent"><xsl:apply-templates/></p>
               </xsl:otherwise>
            </xsl:choose>
         </xsl:when>
         <xsl:when test="not(following-sibling::p)">
            <xsl:choose>
               <xsl:when test="@rend='hang'">
                  <p class="hang"><xsl:apply-templates/></p>
               </xsl:when>
               <xsl:when test="@rend='indent'">
                  <p class="indent"><xsl:apply-templates/></p>
               </xsl:when>
               <xsl:when test="@rend='noindent'">
                  <p class="noindent"><xsl:apply-templates/></p>
               </xsl:when>
               <xsl:otherwise>
                  <p class="padded"><xsl:apply-templates/></p>
               </xsl:otherwise>
            </xsl:choose>
         </xsl:when>
         <xsl:otherwise>
            <xsl:choose>
               <xsl:when test="@rend">
                  <p class="{@rend}"><xsl:apply-templates/></p>
               </xsl:when>
               <xsl:otherwise>
                  <p class="normal"><xsl:apply-templates/></p>
               </xsl:otherwise>
            </xsl:choose>
         </xsl:otherwise>
      </xsl:choose>
      
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Other Text Blocks                                                      -->
   <!-- ====================================================================== -->
   
   <xsl:template match="*:epigraph">
      <blockquote><xsl:apply-templates/></blockquote><br/>
   </xsl:template>
   
   <xsl:template match="*:epigraph/*:bibl">
      <p class="right"><xsl:apply-templates/></p>
   </xsl:template>
   
   <xsl:template match="*:byline">
      <p class="right"><xsl:apply-templates/></p>
   </xsl:template>
   
   <xsl:template match="*:cit">
      <xsl:apply-templates/>
   </xsl:template>
   
   <xsl:template match="*:cit/*:bibl">
      <p class="right"><xsl:apply-templates/></p>
   </xsl:template>
   
   <xsl:template match="*:quote">
      <xsl:apply-templates/>
   </xsl:template>
   
   <xsl:template match="*:q">
      <blockquote><xsl:apply-templates/></blockquote>
   </xsl:template>
   
   <xsl:template match="*:date">
      <xsl:apply-templates/>
   </xsl:template>
   
   <xsl:template match="*:foreign">
      <i><xsl:apply-templates/></i>
   </xsl:template>
   
   <xsl:template match="*:address">
      <xsl:apply-templates/>
   </xsl:template>
   
   <xsl:template match="*:addrLine">
      <xsl:apply-templates/><br/>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Bibliographies                                                         -->
   <!-- ====================================================================== -->
   
   <xsl:template match="*:listBibl">
      <xsl:if test="$anchor.id=@*:id">
         <a name="X"></a>
      </xsl:if>
      <xsl:apply-templates/>
   </xsl:template>
   
   <xsl:template match="*:bibl">
      <xsl:choose>
         <xsl:when test="parent::listBibl">
            <xsl:choose>
               <xsl:when test="$anchor.id=@*:id">
                  <a name="X"></a>
                  <div class="bibl-hi">
                     <p class="hang">
                        <xsl:apply-templates/>
                     </p>
                  </div>
               </xsl:when>
               <xsl:otherwise>
                  <p class="hang">
                     <xsl:apply-templates/>
                  </p>
               </xsl:otherwise>
            </xsl:choose>
         </xsl:when>
         <xsl:otherwise>
            <xsl:apply-templates/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- Because of order in the following, "rend" takes precedence over "level" -->
   
   <xsl:template match="*:title">
      <xsl:choose>
         <xsl:when test="@rend='italic'">
            <i><xsl:apply-templates/></i>
         </xsl:when>
         <xsl:when test="@level='m'">
            <i><xsl:apply-templates/></i>
         </xsl:when>
         <xsl:when test="@level='a'">
            &#x201C;<xsl:apply-templates/>&#x201D;
         </xsl:when>
         <xsl:when test="@level='j'">
            <i><xsl:apply-templates/></i>
         </xsl:when>
         <xsl:when test="@level='s'">
            <i><xsl:apply-templates/></i>
         </xsl:when>
         <xsl:when test="@level='u'">
            <i><xsl:apply-templates/></i>
         </xsl:when>
         <xsl:otherwise>
            <xsl:apply-templates/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <xsl:template match="*:author">
      <xsl:choose>
         <xsl:when test="@rend='hide'">
            <xsl:text>&#x2014;&#x2014;&#x2014;</xsl:text>
         </xsl:when>
         <xsl:otherwise>
            <xsl:apply-templates/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Formatting                                                             -->
   <!-- ====================================================================== -->
   
   <xsl:template match="*:hi">
      <xsl:choose>
         <xsl:when test="@rend='bold'">
            <b><xsl:apply-templates/></b>
         </xsl:when>
         <xsl:when test="@rend='italic'">
            <i><xsl:apply-templates/></i>
         </xsl:when>
         <xsl:when test="@rend='mono'">
            <code><xsl:apply-templates/></code>
         </xsl:when>
         <xsl:when test="@rend='roman'">
            <span class="normal"><xsl:apply-templates/></span>
         </xsl:when>
         <xsl:when test="@rend='smallcaps'">
            <span class="sc"><xsl:apply-templates/></span>
         </xsl:when>
         <xsl:when test="@rend='sub' or @rend='subscript'">
            <sub><xsl:apply-templates/></sub>
         </xsl:when>
         <xsl:when test="@rend='sup' or @rend='superscript'">
            <sup><xsl:apply-templates/></sup>
         </xsl:when>
         <xsl:when test="@rend='underline'">
            <u><xsl:apply-templates/></u>
         </xsl:when>
         <xsl:otherwise>
            <i><xsl:apply-templates/></i>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <xsl:template match="*:lb">
      <br/>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- References                                                             -->
   <!-- ====================================================================== -->
   
   <xsl:template match="*:ref">
      
      <!-- variables -->
      <xsl:variable name="target" select="@target"/>
      <xsl:variable name="chunk">
         <xsl:choose>
            <xsl:when test="@type='secref'">
               <xsl:value-of select="$target"/>
            </xsl:when>
            <xsl:when test="@type='noteref' or @type='endnote'">
               <xsl:value-of select="key('endnote-id', $target)/ancestor::*[matches(local-name(), '^div[1-6]$')][1]/@*:id"/>
            </xsl:when>
            <xsl:when test="@type='fnoteref'">
               <xsl:value-of select="key('fnote-id', $target)/ancestor::*[matches(local-name(), '^div[1-6]$')][1]/@*:id"/>
            </xsl:when>
            <xsl:when test="@type='pageref'">
               <xsl:choose>
                  <xsl:when test="$target='endnotes'">
                     <xsl:value-of select="'endnotes'"/>
                  </xsl:when>
                  <xsl:otherwise>
                     <xsl:value-of select="key('pb-id', $target)/ancestor::*[matches(local-name(), '^div[1-6]$')][1]/@*:id"/>
                  </xsl:otherwise>
               </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
               <xsl:value-of select="key('generic-id', $target)/ancestor::*[matches(local-name(), '^div[1-6]$')][1]/@*:id"/>
            </xsl:otherwise>
         </xsl:choose>
      </xsl:variable>
      <xsl:variable name="toc" select="key('div-id', $chunk)/parent::*/@*:id"/>
      <xsl:variable name="class">
         <xsl:choose>
            <xsl:when test="$anchor.id=@*:id">ref-hi</xsl:when>
            <xsl:otherwise>ref</xsl:otherwise>
         </xsl:choose>
      </xsl:variable>
      
      <!-- back link scrolling -->
      <xsl:if test="$anchor.id=@*:id">
         <a name="X"></a>
      </xsl:if>
      
      <!-- process refs -->
      <xsl:choose>
         <!-- end note refs -->
         <xsl:when test="@type='noteref' or @type='endnote'">
            <sup>
               <xsl:attribute name="class">
                  <xsl:value-of select="$class"/>
               </xsl:attribute>
               <xsl:text>[</xsl:text>
               <a>
                  <xsl:attribute name="href"><xsl:value-of select="$doc.path"/>&#038;chunk.id=<xsl:value-of select="$chunk"/>&#038;toc.id=<xsl:value-of select="$toc"/>&#038;toc.depth=<xsl:value-of select="$toc.depth"/>&#038;brand=<xsl:value-of select="$brand"/><xsl:value-of select="$search"/>&#038;anchor.id=<xsl:value-of select="$target"/>#X</xsl:attribute>
                  <xsl:attribute name="target">_top</xsl:attribute>
                  <xsl:apply-templates/>
               </a>
               <xsl:text>]</xsl:text>
            </sup>
         </xsl:when>
         <!-- footnote refs -->
         <xsl:when test="@type='fnoteref'">
            <sup>
               <xsl:attribute name="class">
                  <xsl:value-of select="$class"/>
               </xsl:attribute>
               <xsl:text>[</xsl:text>
               <a>
                  <xsl:attribute name="href">javascript://</xsl:attribute>
                  <xsl:attribute name="onclick">
                     <xsl:text>javascript:window.open('</xsl:text><xsl:value-of select="$doc.path"/>&#038;doc.view=popup&#038;chunk.id=<xsl:value-of select="$target"/><xsl:text>','popup','width=300,height=300,resizable=yes,scrollbars=yes')</xsl:text>
                  </xsl:attribute>
                  <xsl:apply-templates/>
               </a>
               <xsl:text>]</xsl:text>
            </sup>
         </xsl:when>
         <!-- page refs -->
         <xsl:when test="@type='pageref'">
            <a>
               <xsl:attribute name="href"><xsl:value-of select="$doc.path"/>&#038;chunk.id=<xsl:value-of select="$chunk"/>&#038;toc.id=<xsl:value-of select="$toc"/>&#038;toc.depth=<xsl:value-of select="$toc.depth"/>&#038;brand=<xsl:value-of select="$brand"/>&#038;anchor.id=<xsl:value-of select="$target"/>#X</xsl:attribute>
               <xsl:attribute name="target">_top</xsl:attribute>
               <xsl:apply-templates/>
            </a>
         </xsl:when>
         <!-- all others -->
         <xsl:otherwise>
            <a>
               <xsl:attribute name="href"><xsl:value-of select="$doc.path"/>&#038;chunk.id=<xsl:value-of select="$chunk"/>&#038;toc.id=<xsl:value-of select="$toc"/>&#038;toc.depth=<xsl:value-of select="$toc.depth"/>&#038;brand=<xsl:value-of select="$brand"/>&#038;anchor.id=<xsl:value-of select="$target"/>#X</xsl:attribute>
               <xsl:attribute name="target">_top</xsl:attribute>
               <xsl:apply-templates/>
            </a>
         </xsl:otherwise>
      </xsl:choose>
      
   </xsl:template>
   
   <xsl:template match="*:xref">
      <xsl:choose>
         <xsl:when test="@type='pdf'">
            <a class="ref">
               <xsl:attribute name="href">
                  <xsl:value-of select="$pdf.path"/>
                  <xsl:value-of select="@doc"/>
               </xsl:attribute>
               <sup class="down1">[PDF]</sup>
            </a>
         </xsl:when>
         <xsl:otherwise>
            <a>
               <xsl:attribute name="href">
                  <xsl:value-of select="@to"/>
               </xsl:attribute>
               <xsl:attribute name="target">_top</xsl:attribute>
               <xsl:apply-templates/>
            </a>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Figures                                                                -->
   <!-- ====================================================================== -->
   
   <xsl:template match="*:figure">
      
      <xsl:variable name="img_src">
         <xsl:choose>
            <xsl:when test="contains($docId, 'preview')">
               <xsl:value-of select="unparsed-entity-uri(@entity)"/>
            </xsl:when>
            <xsl:otherwise>
               <xsl:value-of select="concat($figure.path, @entity)"/>
            </xsl:otherwise>
         </xsl:choose>
      </xsl:variable>
      
      <xsl:variable name="fullsize">
         <xsl:choose>
            <xsl:when test="contains($docId, 'preview')">
               <xsl:value-of select="unparsed-entity-uri(substring-before(substring-after(@rend, 'popup('), ')'))"/>
            </xsl:when>
            <xsl:otherwise>
               <xsl:value-of select="concat($figure.path, substring-before(substring-after(@rend,'('),')'))"/>
            </xsl:otherwise>
         </xsl:choose>
      </xsl:variable>
      
      <xsl:variable name="height">
         <xsl:choose>
            <xsl:when test="contains(@rend,'X')">
               <xsl:value-of select="number(substring-before(@rend,'X'))"/>
            </xsl:when>
            <xsl:otherwise>0</xsl:otherwise>
         </xsl:choose>
      </xsl:variable>
      
      <xsl:variable name="width">
         <xsl:choose>
            <xsl:when test="contains(@rend,'X')">
               <xsl:value-of select="number(substring-after(@rend,'X'))"/>
            </xsl:when>
            <xsl:otherwise>0</xsl:otherwise>
         </xsl:choose>
      </xsl:variable>
      
      <xsl:if test="$anchor.id=@*:id">
         <a name="X"></a>
      </xsl:if>
      
      <xsl:choose>
         <xsl:when test="@rend='hide'">
            <div class="illgrp">
               <p>Image Withheld</p>
               <!-- for figDesc -->
               <xsl:apply-templates/>
            </div>
         </xsl:when>
         <xsl:when test="@rend='inline'">
            <img src="{$img_src}" alt="inline image"/>
         </xsl:when>
         <xsl:when test="@rend='block'">
            <div class="illgrp">
               <img src="{$img_src}" width="400" alt="block image"/>
               <!-- for figDesc -->
               <xsl:apply-templates/>
               <br/>
               <xsl:text>[</xsl:text>
               <a>
                  <xsl:attribute name="href">javascript://</xsl:attribute>
                  <xsl:attribute name="onclick">
                     <xsl:text>javascript:window.open('</xsl:text><xsl:value-of select="$doc.path"/><xsl:text>&#038;doc.view=popup&#038;fig.ent=</xsl:text><xsl:value-of select="$img_src"/><xsl:text>','popup','width=600,height=600,resizable=yes,scrollbars=yes')</xsl:text>
                  </xsl:attribute>
                  <xsl:text>Full Size</xsl:text>
               </a>
               <xsl:text>]</xsl:text>
            </div>
         </xsl:when>
         <xsl:when test="contains(@rend, 'popup(')">
            <div class="illgrp">
               <img src="{$img_src}" alt="figure"/>
               <!-- for figDesc -->
               <xsl:apply-templates/>
               <br/>
               <xsl:text>[</xsl:text>
               <a>
                  <xsl:attribute name="href">javascript://</xsl:attribute>
                  <xsl:attribute name="onclick">
                     <xsl:text>javascript:window.open('</xsl:text><xsl:value-of select="$doc.path"/><xsl:text>&#038;doc.view=popup&#038;fig.ent=</xsl:text><xsl:value-of select="$fullsize"/><xsl:text>','popup','width=400,height=400,resizable=yes,scrollbars=yes')</xsl:text>
                  </xsl:attribute>
                  <xsl:text>Full Size</xsl:text>
               </a>
               <xsl:text>]</xsl:text>
            </div>
         </xsl:when>
         <xsl:when test="($height != '0') and ($height &lt; 50)">
            <xsl:text>&#160;</xsl:text><img src="{$img_src}" width="{$width}" height="{$height}" alt="image"/>
         </xsl:when>
         <xsl:when test="($height != '0') and ($height &gt; 50) and ($width &lt; 400)">
            <div class="illgrp">
               <img src="{$img_src}" width="{$width}" height="{$height}" alt="image"/>
               <!-- for figDesc -->
               <xsl:apply-templates/>
               <br/>
               <xsl:text>[</xsl:text>
               <a>
                  <xsl:attribute name="href">javascript://</xsl:attribute>
                  <xsl:attribute name="onclick">
                     <xsl:text>javascript:window.open('</xsl:text><xsl:value-of select="$doc.path"/><xsl:text>&#038;doc.view=popup&#038;fig.ent=</xsl:text><xsl:value-of select="$img_src"/><xsl:text>','popup','width=</xsl:text><xsl:value-of select="$width + 50"/><xsl:text>,height=</xsl:text><xsl:value-of select="$height + 50"/><xsl:text>,resizable=yes,scrollbars=yes')</xsl:text>
                  </xsl:attribute>
                  <xsl:text>Full Size</xsl:text>
               </a>
               <xsl:text>]</xsl:text>
            </div> 
         </xsl:when>
         <xsl:when test="($height != '0') and ($height &gt; 50) and ($width &gt; 400)">
            <div class="illgrp">
               <img src="{$img_src}" width="400" alt="image"/>
               <!-- for figDesc -->
               <xsl:apply-templates/>
               <br/>
               <xsl:text>[</xsl:text>
               <a>
                  <xsl:attribute name="href">javascript://</xsl:attribute>
                  <xsl:attribute name="onclick">
                     <xsl:text>javascript:window.open('</xsl:text><xsl:value-of select="$doc.path"/><xsl:text>&#038;doc.view=popup&#038;fig.ent=</xsl:text><xsl:value-of select="$img_src"/><xsl:text>','popup','width=</xsl:text><xsl:value-of select="$width + 50"/><xsl:text>,height=</xsl:text><xsl:value-of select="$height + 50"/><xsl:text>,resizable=yes,scrollbars=yes')</xsl:text>
                  </xsl:attribute>
                  <xsl:text>Full Size</xsl:text>
               </a>
               <xsl:text>]</xsl:text>
            </div>
         </xsl:when>
         <xsl:otherwise>
            <div class="illgrp">
               <img src="{$img_src}" width="400" alt="image"/>
               <!-- for figDesc -->
               <xsl:apply-templates/>
               <br/>
               <xsl:text>[</xsl:text>
               <a>
                  <xsl:attribute name="href">javascript://</xsl:attribute>
                  <xsl:attribute name="onclick">
                     <xsl:text>javascript:window.open('</xsl:text><xsl:value-of select="$doc.path"/><xsl:text>&#038;doc.view=popup&#038;fig.ent=</xsl:text><xsl:value-of select="$img_src"/><xsl:text>','popup','width=400,height=400,resizable=yes,scrollbars=yes')</xsl:text>
                  </xsl:attribute>
                  <xsl:text>Full Size</xsl:text>
               </a>
               <xsl:text>]</xsl:text>
            </div>
         </xsl:otherwise>
      </xsl:choose>
      
   </xsl:template>
   
   <xsl:template match="*:figDesc">
      <br/><span class="down1"><xsl:if test="@n"><xsl:value-of select="@n"/>. </xsl:if><xsl:apply-templates/></span>
   </xsl:template>
   
   <!-- ====================================================================== -->
   <!-- Milestones                                                             -->
   <!-- ====================================================================== -->
   
   <xsl:template match="*:pb">
      <xsl:choose>
         <!-- xsl:when test="not(following-sibling::*)"/ -->
         <xsl:when test="$anchor.id=@*:id">
            <a name="X"></a>
            <hr class="pb"/>
            <div align="center">&#x2015; <span class="run-head"><xsl:value-of select="@n"/></span> &#x2015;</div>
         </xsl:when>
         <xsl:otherwise>
            <hr class="pb"/>
            <div align="center">&#x2015; <span class="run-head"><xsl:value-of select="@n"/></span> &#x2015;</div>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <xsl:template match="*:milestone">
      
      <xsl:if test="$anchor.id=@*:id">
         <a name="X"></a>
      </xsl:if>
      
      <xsl:if test="@rend='ornament' or @rend='ornamental_break'">
         <div align="center">
            <table border="0" width="40%"><tr align="center"><td>&#x2022;</td><td>&#x2022;</td><td>&#x2022;</td></tr></table>
         </div>
      </xsl:if>    
      
   </xsl:template>
   
</xsl:stylesheet>
