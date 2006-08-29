<!-- EAD Cookbook Style 7      Version 0.9   19 January 2004 -->

<!-- This is just one variation. For many others, check out the EAD Cookbook:
	http://www.archivists.org/saagroups/ead/ead2002cookbookhelp.html -->

<!--  This stylesheet generates a Table of Contents in an HTML frame along
the left side of the screen. It is an update to eadcbs3.xsl designed
to work with EAD 2002.-->
<xsl:stylesheet 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  xmlns:saxon="http://icl.com/saxon" 
  xmlns:xtf="http://cdlib.org/xtf"
  extension-element-prefixes="saxon" 
  version="2.0">
  
  <xsl:strip-space elements="*"/>
  
  <xsl:output method="html"
    indent="yes"
    encoding="utf-8"
    media-type="text/html"
    doctype-public="-//W3C//DTD HTML 4.0//EN"/>
  
  <xsl:include href="../common/docFormatterCommon.xsl"/>
  <xsl:include href="parameter.xsl"/>
  <xsl:include href="search.xsl"/>
  
  <xsl:key name="hit-num-dynamic" match="xtf:hit" use="@hitNum"/>
  <xsl:key name="hit-rank-dynamic" match="xtf:hit" use="@rank"/>
  
  <!-- Creates a variable equal to the value of the number in eadid which serves as the base
    for file names for the various components of the frameset.-->
  <xsl:variable name="file">
    <xsl:value-of select="ead/eadheader/eadid"/>
  </xsl:variable>
  
  <!-- The following statements create the four output files that comprise the
    frameset.-->
  <xsl:template match="/ead">
    <xsl:choose>
      
      <!-- Creates the button bar.-->
      <xsl:when test="$doc.view = 'bbar'">
        <xsl:call-template name="bbar"/>
      </xsl:when>
      
      <!-- Creates the basic table of contents.-->
      <xsl:when test="$doc.view = 'toc'">
        <xsl:call-template name="toc"/>
      </xsl:when>

      <!-- Creates the body of the finding aid.-->
      <xsl:when test="$doc.view = 'content'">
        <xsl:call-template name="body"/>
      </xsl:when>

      <!-- Creates the basic frameset.-->
      <xsl:otherwise>
        <xsl:call-template name="frameset"/>
      </xsl:otherwise>
      
    </xsl:choose>
  </xsl:template>
  
  <!-- Defines the frameset.-->
  <xsl:template name="frameset">
    <xsl:variable name="bbar.href"><xsl:value-of select="$query.string"/>&#038;doc.view=bbar&#038;brand=<xsl:value-of select="$brand"/><xsl:value-of select="$search"/></xsl:variable> 
    <xsl:variable name="toc.href"><xsl:value-of select="$query.string"/>&#038;doc.view=toc&#038;brand=<xsl:value-of select="$brand"/>&#038;toc.id=<xsl:value-of select="$toc.id"/><xsl:value-of select="$search"/>#X</xsl:variable>
    <xsl:variable name="content.href"><xsl:value-of select="$query.string"/>&#038;doc.view=content&#038;brand=<xsl:value-of select="$brand"/>&#038;anchor.id=<xsl:value-of select="$anchor.id"/><xsl:value-of select="$search"/><xsl:call-template name="create.anchor"/></xsl:variable>
    
    <html>
      <head>
        <link rel="stylesheet" type="text/css" href="{$css.path}content.css"/>
        
        <title>
          <xsl:value-of select="eadheader/filedesc/titlestmt/titleproper"/>
          <xsl:text>  </xsl:text>
          <xsl:value-of select="eadheader/filedesc/titlestmt/subtitle"/>
        </title>
        <xsl:call-template name="metadata"/>
      </head>
      
      <frameset rows="80,*" border="2" framespacing="2" frameborder="1">
        <frame scrolling="no" title="Navigation Bar">
          <xsl:attribute name="name">bbar</xsl:attribute>
          <xsl:attribute name="src"><xsl:value-of select="$xtfURL"/><xsl:value-of select="$dynaxmlPath"/>?<xsl:value-of select="$bbar.href"/></xsl:attribute>
        </frame>
        <frameset cols="35%,65%" border="2" framespacing="2" frameborder="1">
          <frame title="Table of Contents">
            <xsl:attribute name="name">toc</xsl:attribute>
            <xsl:attribute name="src"><xsl:value-of select="$xtfURL"/><xsl:value-of select="$dynaxmlPath"/>?<xsl:value-of select="$toc.href"/></xsl:attribute>
          </frame>
          <frame title="Content">
            <xsl:attribute name="name">content</xsl:attribute>
            <xsl:attribute name="src"><xsl:value-of select="$xtfURL"/><xsl:value-of select="$dynaxmlPath"/>?<xsl:value-of select="$content.href"/></xsl:attribute>
          </frame>
        </frameset>
      </frameset>
      <noframes>
        <h1>Sorry, your browser doesn't support frames...</h1>
      </noframes>
    </html>
  </xsl:template>
  
  <!-- ====================================================================== -->
  <!-- Anchor Template                                                        -->
  <!-- ====================================================================== -->
  
  <xsl:template name="create.anchor">
    <xsl:choose>
      <xsl:when test="($query != '0' and $query != '') and $hit.rank != '0'">
        <xsl:text>#</xsl:text><xsl:value-of select="key('hit-rank-dynamic', $hit.rank)/@hitNum"/>
      </xsl:when>
      <xsl:when test="($query != '0' and $query != '') and $set.anchor != '0'">
        <xsl:text>#</xsl:text><xsl:value-of select="$set.anchor"/>
      </xsl:when>
      <xsl:when test="$query != '0' and $query != ''">
        <xsl:text>#</xsl:text><xsl:value-of select="/*/@xtf:firstHit"/>
      </xsl:when>
      <xsl:when test="$anchor.id != '0'">
        <xsl:text>#X</xsl:text>
      </xsl:when>
    </xsl:choose>
  </xsl:template>
  
  <!-- ====================================================================== -->
  <!-- Button Bar Templates                                                   -->
  <!-- ====================================================================== -->
  
  <xsl:template name="bbar">
    <html>
      <head>
        <title>
          <xsl:value-of select="$doc.title"/>
        </title>
        <xsl:copy-of select="$brand.links"/>
        <link rel="stylesheet" type="text/css" href="{$css.path}bbar.css"/>
      </head>
      <body>
        <xsl:call-template name="bbar.table"/>
      </body>
    </html>
  </xsl:template>
  
  <xsl:template name="bbar.table">
    
    <xsl:variable name="target">
      <xsl:text>_top</xsl:text>
    </xsl:variable>
    
    <table width="100%" border="0" cellpadding="0" cellspacing="0">
      
      <xsl:copy-of select="$brand.header.dynaxml.header"/>
      
      <!-- BEGIN TOPNAV ROW -->
      <tr  width="100%">
        <td class="topnav-outer" colspan="3" width="100%" height="31" align="center" valign="middle">
          
          <!-- BEGIN TOPNAV LEFT -->
          <table width="100%" height="27" border="0" cellpadding="0" cellspacing="0">
            <tr>
              <td class="topnav-inner" width="25%"  align="center" valign="middle">
                
                <!-- BEGIN TOPNAV LEFT INNER TABLE -->
                <table border="0" cellpadding="0" cellspacing="0">
                  <tr align="left" valign="middle">
                    <td width="8" nowrap="nowrap"><img src="{$icon.path}spacer.gif" width="8"/></td>
                    <td width="15" nowrap="nowrap">
                      <a href="search" target="{$target}">
                        <img src="{$icon.path}arrow.gif" width="15" height="15" border="0"/>
                      </a>
                    </td>
                    <td nowrap="nowrap">&#160;<a class="topnav" href="search" target="{$target}">Home</a></td>
                    <td width="10" nowrap="nowrap"><img src="{$icon.path}spacer.gif" width="10"/></td>
                    
                  </tr>
                </table>
                <!-- END TOPNAV LEFT INNER TABLE -->
                
              </td>
              <!-- END TOPNAV LEFT -->
              
              <td width="2"><img src="{$icon.path}spacer.gif" width="2"/></td>
              
              <!-- BEGIN TOPNAV CENTER -->
              <form action="{$xtfURL}{$dynaxmlPath}" target="{$target}" method="GET">
                <input type="hidden" name="docId">
                  <xsl:attribute name="value">
                    <xsl:value-of select="$docId"/>
                  </xsl:attribute>
                </input>
                <input type="hidden" name="chunk.id">
                  <xsl:attribute name="value">
                    <xsl:value-of select="$chunk.id"/>
                  </xsl:attribute>
                </input>              
                
                <td class="topnav-inner" width="50%" align="center" nowrap="nowrap">
                  
                  <!-- BEGIN TOPNAV LEFT INNER TABLE -->
                  <table border="0" cellpadding="0" cellspacing="0">
                    <tr align="left" valign="middle">
                      <td nowrap="nowrap"><span class="search-text">Search</span>&#160;</td>                      
                      <td nowrap="nowrap"><input name="query" type="text" size="15"/>&#160;<input type="submit" value="Go"/></td>
                    </tr>
                  </table>
                  <!-- END TOPNAV LEFT INNER TABLE -->
                  
                </td>
              </form>
              <!-- END TOPNAV CENTER -->
              
              <td width="2"><img src="{$icon.path}spacer.gif" width="2"/></td>
              
              <!-- BEGIN TOPNAV RIGHT -->
              <td class="topnav-inner" width="25%" align="center" valign="middle">
                
                <!-- BEGIN TOPNAV RIGHT INNER TABLE -->
                <table border="0" cellpadding="0" cellspacing="0">
                  <tr align="right" valign="middle">
                    <td width="15" nowrap="nowrap"><img src="{$icon.path}spacer.gif" width="10"/></td> 
                    <td width="15" nowrap="nowrap"><img src="{$icon.path}spacer.gif" width="10"/></td> 
                    <td width="10" nowrap="nowrap"><img src="{$icon.path}spacer.gif" width="10"/></td>
                    <td width="15" nowrap="nowrap">
                      <a class="topnav" href="http://xtf.sourceforge.net" target="{$target}">
                        <img src="{$icon.path}arrow.gif" width="15" height="15" border="0"/>
                      </a>
                    </td>
                    <td nowrap="nowrap">&#160;<a class="topnav" href="http://xtf.sourceforge.net" target="{$target}">Help</a></td>
                    <td width="8" nowrap="nowrap"><img src="{$icon.path}spacer.gif" width="8"/></td>
                  </tr>
                </table>
                <!-- END TOPNAV RIGHT INNER TABLE -->
                
              </td>
              <!-- END TOPNAV RIGHT -->
              
            </tr>
          </table>
          
        </td>
      </tr>
    </table>
  </xsl:template>
  
  <!--This template creates HTML meta tags that are inserted into the HTML ouput
    for use by web search engines indexing this file.   The content of each
    resulting META tag uses Dublin Core semantics and is drawn from the text of
    the finding aid.-->
  <xsl:template name="metadata">
    <meta http-equiv="Content-Type" name="dc.title" content="{eadheader/filedesc/titlestmt/titleproper  }{eadheader/filedesc/titlestmt/subtitle}"/>
    <meta http-equiv="Content-Type" name="dc.author" content="{archdesc/did/origination}"/>
    
    <xsl:for-each select="//controlaccess/persname | //controlaccess/corpname">
      <xsl:choose>
        <xsl:when test="@encodinganalog='600'">
          <meta http-equiv="Content-Type" name="dc.subject" content="{.}"/>
        </xsl:when>
        
        <xsl:when test="//@encodinganalog='610'">
          <meta http-equiv="Content-Type" name="dc.subject" content="{.}"/>
        </xsl:when>
        
        <xsl:when test="//@encodinganalog='611'">
          <meta http-equiv="Content-Type" name="dc.subject" content="{.}"/>
        </xsl:when>
        
        <xsl:when test="//@encodinganalog='700'">
          <meta http-equiv="Content-Type" name="dc.contributor" content="{.}"/>
        </xsl:when>
        
        <xsl:when test="//@encodinganalog='710'">
          <meta http-equiv="Content-Type" name="dc.contributor" content="{.}"/>
        </xsl:when>
        
        <xsl:otherwise>
          <meta http-equiv="Content-Type" name="dc.contributor" content="{.}"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:for-each>
    <xsl:for-each select="//controlaccess/subject">
      <meta http-equiv="Content-Type" name="dc.subject" content="{.}"/>
    </xsl:for-each>
    <xsl:for-each select="//controlaccess/geogname">
      <meta http-equiv="Content-Type" name="dc.subject" content="{.}"/>
    </xsl:for-each>
    
    <meta http-equiv="Content-Type" name="dc.title" content="{archdesc/did/unittitle}"/>
    <meta http-equiv="Content-Type" name="dc.type" content="text"/>
    <meta http-equiv="Content-Type" name="dc.format" content="manuscripts"/>
    <meta http-equiv="Content-Type" name="dc.format" content="finding aids"/>
    
  </xsl:template>
  
  <!--This part of the template creates the base table for the finding aid with
    two columns. -->
  
  <xsl:template name="toc">
    <xsl:variable name="sum">
      <xsl:choose>
        <xsl:when test="($query != '0') and ($query != '')">
          <xsl:value-of select="number(/*[1]/@xtf:hitCount)"/>
        </xsl:when>
        <xsl:otherwise>0</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="occur">
      <xsl:choose>
        <xsl:when test="$sum != 1">occurrences</xsl:when>
        <xsl:otherwise>occurrence</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    
    <html>
      <head>
        <base target="body"/>
        <link rel="stylesheet" type="text/css" href="{$css.path}toc.css"/>
      </head>
      <body>
        
        <div class="toc">
          <table>
            <tr>
              <td height="25">
                <b>
                  <xsl:attribute name="target">_top</xsl:attribute>
                  <xsl:value-of select="$doc.title"/>
                </b>
              </td>
            </tr>
          </table>
          
          <xsl:if test="($query != '0') and ($query != '')">
            <hr/>
            <div align="center">
              <b>
                <span class="hit-count">
                  <xsl:value-of select="$sum"/>
                </span>
                <xsl:text> </xsl:text>
                <xsl:value-of select="$occur"/>
                <xsl:text> of </xsl:text>
                <span class="hit-count">
                  <xsl:value-of select="$query"/>
                </span>
              </b>
              <br/>
              <xsl:text> [</xsl:text>
              <a>
                <xsl:attribute name="href">
                  <xsl:value-of select="$doc.path"/>&#038;chunk.id=<xsl:value-of select="$chunk.id"/>&#038;toc.depth=<xsl:value-of select="$toc.depth"/>&#038;toc.id=<xsl:value-of select="$toc.id"/>&#038;brand=<xsl:value-of select="$brand"/>
                </xsl:attribute>
                <xsl:attribute name="target">_top</xsl:attribute>
                <xsl:text>Clear Hits</xsl:text>
              </a>
              <xsl:text>]</xsl:text>
            </div>
          </xsl:if>
          <hr/>
          
          <br/>
          <!-- The Table of Contents template performs a series of tests to
            determine which elements will be included in the table
            of contents.  Each if statement tests to see if there is
            a matching element with content in the finding aid.-->
          <xsl:if test="archdesc/did/head">
            <xsl:apply-templates select="archdesc/did/head" mode="tocLink"/>
          </xsl:if>
          <xsl:if test="archdesc/bioghist/head">
            <xsl:apply-templates select="archdesc/bioghist/head" mode="tocLink"/>
          </xsl:if>
          <xsl:if test="archdesc/scopecontent/head">
            <xsl:apply-templates select="archdesc/scopecontent/head" mode="tocLink"/>
          </xsl:if>
          <xsl:if test="archdesc/arrangement/head">
            <xsl:apply-templates select="archdesc/arrangement/head" mode="tocLink"/>
          </xsl:if>
          
          <xsl:if test="archdesc/userestrict/head   or archdesc/accessrestrict/head   or archdesc/*/userestrict/head   or archdesc/*/accessrestrict/head">
            <xsl:call-template name="make-toc-link">
              <xsl:with-param name="name" select="'Restrictions'"/>
              <xsl:with-param name="id" select="'restrictlink'"/>
              <xsl:with-param name="nodes" select="archdesc/userestrict|archdesc/accessrestrict|archdesc/*/userestrict|archdesc/*/accessrestrict"/>
            </xsl:call-template>
          </xsl:if>
          <xsl:if test="archdesc/controlaccess/head">
            <xsl:apply-templates select="archdesc/controlaccess/head" mode="tocLink"/>
          </xsl:if>
          <xsl:if test="archdesc/relatedmaterial   or archdesc/separatedmaterial   or archdesc/*/relatedmaterial   or archdesc/*/separatedmaterial">
            <xsl:call-template name="make-toc-link">
              <xsl:with-param name="name" select="'Related Material'"/>
              <xsl:with-param name="id" select="'relatedmatlink'"/>
              <xsl:with-param name="nodes" select="archdesc/relatedmaterial|archdesc/separatedmaterial|archdesc/*/relatedmaterial|archdesc/*/separatedmaterial"/>
            </xsl:call-template>
          </xsl:if>
          <xsl:if test="archdesc/acqinfo/*   or archdesc/processinfo/*   or archdesc/prefercite/*   or archdesc/custodialhist/*   or archdesc/processinfo/*   or archdesc/appraisal/*   or archdesc/accruals/*   or archdesc/*/acqinfo/*   or archdesc/*/processinfo/*   or archdesc/*/prefercite/*   or archdesc/*/custodialhist/*   or archdesc/*/procinfo/*   or archdesc/*/appraisal/*   or archdesc/*/accruals/*">
            <xsl:call-template name="make-toc-link">
              <xsl:with-param name="name" select="'Administrative Information'"/>
              <xsl:with-param name="id" select="'adminlink'"/>
              <xsl:with-param name="nodes" select="archdesc/acqinfo|archdesc/prefercite|archdesc/custodialhist|archdesc/custodialhist|archdesc/processinfo|archdesc/appraisal|archdesc/accruals|archdesc/*/acqinfo|archdesc/*/processinfo|archdesc/*/prefercite|archdesc/*/custodialhist|archdesc/*/procinfo|archdesc/*/appraisal|archdesc/*/accruals/*"/>
            </xsl:call-template>
          </xsl:if>
          
          <xsl:if test="archdesc/otherfindaid/head    or archdesc/*/otherfindaid/head">
            <xsl:choose>
              <xsl:when test="archdesc/otherfindaid/head">
                <xsl:apply-templates select="archdesc/otherfindaid/head" mode="tocLink"/>
              </xsl:when>
              <xsl:when test="archdesc/*/otherfindaid/head">
                <xsl:apply-templates select="archdesc/*/otherfindaid/head" mode="tocLink"/>
              </xsl:when>
            </xsl:choose>
          </xsl:if>
          
          <!--The next test covers the situation where there is more than one odd element
            in the document.-->
          <xsl:if test="archdesc/odd/head">
            <xsl:for-each select="archdesc/odd">
              <xsl:call-template name="make-toc-link">
                <xsl:with-param name="name" select="head"/>
                <xsl:with-param name="id" select="xtf:make-id(head)"/>
                <xsl:with-param name="nodes" select="."/>
              </xsl:call-template>
            </xsl:for-each>
          </xsl:if>
          
          <xsl:if test="archdesc/bibliography/head    or archdesc/*/bibliography/head">
            <xsl:choose>
              <xsl:when test="archdesc/bibliography/head">
                <xsl:apply-templates select="archdesc/bibliography/head" mode="tocLink"/>
              </xsl:when>
              <xsl:when test="archdesc/*/bibliography/head">
                <xsl:apply-templates select="archdesc/*/bibliography/head" mode="tocLink"/>
              </xsl:when>
            </xsl:choose>
          </xsl:if>
          
          <xsl:if test="archdesc/index/head    or archdesc/*/index/head">
            <xsl:choose>
              <xsl:when test="archdesc/index/head">
                <xsl:apply-templates select="archdesc/index/head" mode="tocLink"/>
              </xsl:when>
              <xsl:when test="archdesc/*/index/head">
                <xsl:apply-templates select="archdesc/*/index/head" mode="tocLink"/>
              </xsl:when>
            </xsl:choose>
          </xsl:if>
          
          <xsl:if test="archdesc/dsc/head">
            <xsl:apply-templates select="archdesc/dsc/head" mode="tocLink"/>
            <!-- Displays the unittitle and unitdates for a c01 if it is a series (as
              evidenced by the level attribute series)and numbers them
              to form a hyperlink to each.   Delete this section if you do not
              wish the c01 titles to appear in the table of contents.-->
            <xsl:for-each select="archdesc/dsc/c01[@level='series' or @level='subseries'    or @level='subgrp' or @level='subcollection']">
              <xsl:call-template name="make-toc-link">
                <xsl:with-param name="name">
                  <xsl:choose>
                    <xsl:when test="did/unittitle/unitdate">
                      <xsl:for-each select="did/unittitle">
                        <xsl:value-of select="text()"/>
                        <xsl:text> </xsl:text>
                        <xsl:apply-templates select="./unitdate"/>
                      </xsl:for-each>
                    </xsl:when>
                    
                    <xsl:otherwise>
                      <xsl:apply-templates select="did/unittitle"/>
                      <xsl:text> </xsl:text>
                      <xsl:apply-templates select="did/unitdate"/>
                    </xsl:otherwise>
                  </xsl:choose>
                </xsl:with-param>
                <xsl:with-param name="id">
                  <xsl:value-of select="concat('series', position())"/>
                </xsl:with-param>
                <xsl:with-param name="nodes" select="."/>
                <xsl:with-param name="indent" select="2"/>
              </xsl:call-template>
            </xsl:for-each>
            <!--This ends the section that causes the c01 titles to appear in the table of contents.-->
          </xsl:if>
          <!--End of the table of contents. -->
        </div>
      </body>
    </html>
  </xsl:template>
  
  <xsl:template match="node()" mode="tocLink">
    <xsl:call-template name="make-toc-link">
      <xsl:with-param name="name" select="string(.)"/>
      <xsl:with-param name="id" select="xtf:make-id(.)"/>
      <xsl:with-param name="nodes" select="parent::*"/>
    </xsl:call-template>
  </xsl:template>
  
  <xsl:template name="make-toc-link">
    <xsl:param name="name"/>
    <xsl:param name="id"/>
    <xsl:param name="nodes"/>
    <xsl:param name="indent" select="1"/>
    
    <xsl:variable name="hit.count" select="sum($nodes/*/@xtf:hitCount)"/>
    <xsl:variable name="content.href"><xsl:value-of select="$query.string"/>&#038;doc.view=content&#038;brand=<xsl:value-of select="$brand"/>&#038;anchor.id=<xsl:value-of select="$anchor.id"/><xsl:value-of select="$search"/></xsl:variable>
    
    <table border="0" cellpadding="0" cellspacing="0" width="820">
      <tr>
        <td align="right">
          <xsl:attribute name="width" select="20 * $indent"/>
          <span class="hit-count">
            <xsl:if test="$hit.count &gt; 0">
              <xsl:value-of select="$hit.count"/>
            </xsl:if>  
          </span>
        </td>
        <td width="15" align="center">
          <img src="http://localhost:8080/xtf-1.8/icons/default/i_colpse.gif" border="0" alt="collapse section"/>
        </td>
        <td align="left" valign="top" width="700">
          <nobr>
            <a>
              <xsl:attribute name="href">
                <xsl:value-of select="$xtfURL"/><xsl:value-of select="$dynaxmlPath"/>?<xsl:value-of select="$content.href"/>#<xsl:value-of select="$id"/>
              </xsl:attribute>
              <xsl:attribute name="target">content</xsl:attribute>
              <xsl:value-of select="$name"/>
            </a>
          </nobr>
        </td>
      </tr>
    </table>
  </xsl:template>
  
  <!-- Creates the body of the finding aid.-->
  <xsl:template name="body">
    <xsl:variable name="file">
      <xsl:value-of select="ead/eadheader/eadid"/>
    </xsl:variable>
    <html>
      <head>
        <link rel="stylesheet" type="text/css" href="{$css.path}content.css"/>
        
        <title>
          <xsl:value-of select="eadheader/filedesc/titlestmt/titleproper"/>
          <xsl:text>  </xsl:text>
          <xsl:value-of select="eadheader/filedesc/titlestmt/subtitle"/>
        </title>
        
        <xsl:call-template name="metadata"/>
      </head>
      <body>
        <div class="content">
        <!--This part of template inserts a logo and title
          at the top of the display.  Insert the proper path ro
          your image in place of yourlogo.gif.-->
        
        <!--If you do not want to include an image, delete the center
          element and its contents.-->
        <center>
          <img src="yourlogo.gif"/>
        </center>
        
        <xsl:apply-templates select="eadheader"/>
        
        <!--To change the order of display, adjust the sequence of
          the following apply-template statements which invoke the various
          templates that populate the finding aid.  Multiple statements
          are included to handle the possibility that descgrp has been used
          as a wrapper to replace add and admininfo.  In several cases where
          multiple elemnents are displayed together in the output, a call-template
          statement is used-->
        
        <xsl:apply-templates select="archdesc/did"/>
        <xsl:apply-templates select="archdesc/bioghist"/>
        <xsl:apply-templates select="archdesc/scopecontent"/>
        <xsl:apply-templates select="archdesc/arrangement"/>
        <xsl:call-template name="archdesc-restrict"/>
        <xsl:call-template name="archdesc-relatedmaterial"/>
        <xsl:apply-templates select="archdesc/controlaccess"/>
        <xsl:apply-templates select="archdesc/odd"/>
        <xsl:apply-templates select="archdesc/originalsloc"/>
        <xsl:apply-templates select="archdesc/phystech"/>
        <xsl:call-template name="archdesc-admininfo"/>
        <xsl:apply-templates select="archdesc/descgrp"/>
        <xsl:apply-templates select="archdesc/otherfindaid | archdesc/*/otherfindaid"/>
        <xsl:apply-templates select="archdesc/fileplan | archdesc/*/fileplan"/>
        <xsl:apply-templates select="archdesc/bibliography | archdesc/*/bibliography"/>
        <xsl:apply-templates select="archdesc/index | archdesc/*/index"/>
        <xsl:apply-templates select="archdesc/dsc"/></div>
      </body>
    </html>
  </xsl:template>		
  
  
  <!-- The following general templates format the display of various RENDER
    attributes.-->
  <xsl:template match="emph[@render='bold']">
    <b>
      <xsl:apply-templates/>
    </b>
  </xsl:template>
  <xsl:template match="emph[@render='italic']">
    <i>
      <xsl:apply-templates/>
    </i>
  </xsl:template>
  <xsl:template match="emph[@render='underline']">
    <u>
      <xsl:apply-templates/>
    </u>
  </xsl:template>
  <xsl:template match="emph[@render='sub']">
    <sub>
      <xsl:apply-templates/>
    </sub>
  </xsl:template>
  <xsl:template match="emph[@render='super']">
    <super>
      <xsl:apply-templates/>
    </super>
  </xsl:template>
  
  <xsl:template match="emph[@render='quoted']">
    <xsl:text>"</xsl:text>
    <xsl:apply-templates/>
    <xsl:text>"</xsl:text>
  </xsl:template>
  
  <xsl:template match="emph[@render='doublequote']">
    <xsl:text>"</xsl:text>
    <xsl:apply-templates/>
    <xsl:text>"</xsl:text>
  </xsl:template>
  <xsl:template match="emph[@render='singlequote']">
    <xsl:text>'</xsl:text>
    <xsl:apply-templates/>
    <xsl:text>'</xsl:text>
  </xsl:template>
  <xsl:template match="emph[@render='bolddoublequote']">
    <b>
      <xsl:text>"</xsl:text>
      <xsl:apply-templates/>
      <xsl:text>"</xsl:text>
    </b>
  </xsl:template>
  <xsl:template match="emph[@render='boldsinglequote']">
    <b>
      <xsl:text>'</xsl:text>
      <xsl:apply-templates/>
      <xsl:text>'</xsl:text>
    </b>
  </xsl:template>
  <xsl:template match="emph[@render='boldunderline']">
    <b>
      <u>
        <xsl:apply-templates/>
      </u>
    </b>
  </xsl:template>
  <xsl:template match="emph[@render='bolditalic']">
    <b>
      <i>
        <xsl:apply-templates/>
      </i>
    </b>
  </xsl:template>
  <xsl:template match="emph[@render='boldsmcaps']">
    <font style="font-variant: small-caps">
      <b>
        <xsl:apply-templates/>
      </b>
    </font>
  </xsl:template>
  <xsl:template match="emph[@render='smcaps']">
    <font style="font-variant: small-caps">
      <xsl:apply-templates/>
    </font>
  </xsl:template>
  <xsl:template match="title[@render='bold']">
    <b>
      <xsl:apply-templates/>
    </b>
  </xsl:template>
  <xsl:template match="title[@render='italic']">
    <i>
      <xsl:apply-templates/>
    </i>
  </xsl:template>
  <xsl:template match="title[@render='underline']">
    <u>
      <xsl:apply-templates/>
    </u>
  </xsl:template>
  <xsl:template match="title[@render='sub']">
    <sub>
      <xsl:apply-templates/>
    </sub>
  </xsl:template>
  <xsl:template match="title[@render='super']">
    <super>
      <xsl:apply-templates/>
    </super>
  </xsl:template>
  
  <xsl:template match="title[@render='quoted']">
    <xsl:text>"</xsl:text>
    <xsl:apply-templates/>
    <xsl:text>"</xsl:text>
  </xsl:template>
  
  <xsl:template match="title[@render='doublequote']">
    <xsl:text>"</xsl:text>
    <xsl:apply-templates/>
    <xsl:text>"</xsl:text>
  </xsl:template>
  
  <xsl:template match="title[@render='singlequote']">
    <xsl:text>'</xsl:text>
    <xsl:apply-templates/>
    <xsl:text>'</xsl:text>
  </xsl:template>
  <xsl:template match="title[@render='bolddoublequote']">
    <b>
      <xsl:text>"</xsl:text>
      <xsl:apply-templates/>
      <xsl:text>"</xsl:text>
    </b>
  </xsl:template>
  <xsl:template match="title[@render='boldsinglequote']">
    <b>
      <xsl:text>'</xsl:text>
      <xsl:apply-templates/>
      <xsl:text>'</xsl:text>
    </b>
  </xsl:template>
  
  <xsl:template match="title[@render='boldunderline']">
    <b>
      <u>
        <xsl:apply-templates/>
      </u>
    </b>
  </xsl:template>
  <xsl:template match="title[@render='bolditalic']">
    <b>
      <i>
        <xsl:apply-templates/>
      </i>
    </b>
  </xsl:template>
  <xsl:template match="title[@render='boldsmcaps']">
    <font style="font-variant: small-caps">
      <b>
        <xsl:apply-templates/>
      </b>
    </font>
  </xsl:template>
  <xsl:template match="title[@render='smcaps']">
    <font style="font-variant: small-caps">
      <xsl:apply-templates/>
    </font>
  </xsl:template>
  <!-- This template converts a Ref element into an HTML anchor.-->
  <xsl:template match="ref">
    <a href="#{@target}">
      <xsl:apply-templates/>
    </a>
  </xsl:template>
  
  <!--This template rule formats a list element anywhere
    except in arrangement.-->
  <xsl:template match="list[parent::*[not(self::arrangement)]]/head">
    <div style="margin-left: 25pt">
      <b>
        <xsl:apply-templates/>
      </b>
    </div>
  </xsl:template>
  
  <xsl:template match="list[parent::*[not(self::arrangement)]]/item">
    <div style="margin-left: 40pt">
      <xsl:apply-templates/>
    </div>
  </xsl:template>
  
  <!--Formats a simple table. The width of each column is defined by the colwidth attribute in a colspec element.-->
  <xsl:template match="table">
    <table width="75%" style="margin-left: 25pt">
      <tr>
        <td colspan="3">
          <h4>
            <xsl:apply-templates select="head"/>
          </h4>
        </td>
      </tr>
      <xsl:for-each select="tgroup">
        <tr>
          <xsl:for-each select="colspec">
            <td width="{@colwidth}"/>
          </xsl:for-each>
        </tr>
        <xsl:for-each select="thead">
          <xsl:for-each select="row">
            <tr>
              <xsl:for-each select="entry">
                <td valign="top">
                  <b>
                    <xsl:apply-templates/>
                  </b>
                </td>
              </xsl:for-each>
            </tr>
          </xsl:for-each>
        </xsl:for-each>
        
        <xsl:for-each select="tbody">
          <xsl:for-each select="row">
            <tr>
              <xsl:for-each select="entry">
                <td valign="top">
                  <xsl:apply-templates/>
                </td>
              </xsl:for-each>
            </tr>
          </xsl:for-each>
        </xsl:for-each>
      </xsl:for-each>
    </table>
  </xsl:template>
  <!--This template rule formats a chronlist element.-->
  <xsl:template match="chronlist">
    <table width="100%" style="margin-left:25pt">
      <tr>
        <td width="5%"> </td>
        <td width="15%"> </td>
        <td width="80%"> </td>
      </tr>
      <xsl:apply-templates/>
    </table>
  </xsl:template>
  
  <xsl:template match="chronlist/head">
    <tr>
      <td colspan="3">
        <h4>
          <xsl:apply-templates/>
        </h4>
      </td>
    </tr>
  </xsl:template>
  
  <xsl:template match="chronlist/listhead">
    <tr>
      <td> </td>
      <td>
        <b>
          <xsl:apply-templates select="head01"/>
        </b>
      </td>
      <td>
        <b>
          <xsl:apply-templates select="head02"/>
        </b>
      </td>
    </tr>
  </xsl:template>
  
  <xsl:template match="chronitem">
    <!--Determine if there are event groups.-->
    <xsl:choose>
      <xsl:when test="eventgrp">
        <!--Put the date and first event on the first line.-->
        <tr>
          <td> </td>
          <td valign="top">
            <xsl:apply-templates select="date"/>
          </td>
          <td valign="top">
            <xsl:apply-templates select="eventgrp/event[position()=1]"/>
          </td>
        </tr>
        <!--Put each successive event on another line.-->
        <xsl:for-each select="eventgrp/event[not(position()=1)]">
          <tr>
            <td> </td>
            <td> </td>
            <td valign="top">
              <xsl:apply-templates select="."/>
            </td>
          </tr>
        </xsl:for-each>
      </xsl:when>
      <!--Put the date and event on a single line.-->
      <xsl:otherwise>
        <tr>
          <td> </td>
          <td valign="top">
            <xsl:apply-templates select="date"/>
          </td>
          <td valign="top">
            <xsl:apply-templates select="event"/>
          </td>
        </tr>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
  <!--Suppreses all other elements of eadheader.-->
  <xsl:template match="eadheader">
    <h2 style="text-align:center">
      <a name="{xtf:make-id(titlestmt/titleproper)}">
        <xsl:value-of select="filedesc/titlestmt/titleproper"/>
      </a>
    </h2>
    <h3 style="text-align:center">
      <xsl:value-of select="filedesc/titlestmt/subtitle"/>
    </h3>
    <br/>
  </xsl:template>
  
  <!--This template creates a table for the did, inserts the head and then
    each of the other did elements.  To change the order of appearance of these
    elements, change the sequence of the apply-templates statements.-->
  <xsl:template match="archdesc/did">
    <table width="100%">
      <tr>
        <td width="25%"> </td>
        <td width="75%"> </td>
      </tr>
      <tr>
        <td colspan="2">
          <h3>
            <a name="{xtf:make-id(head)}">
              <xsl:apply-templates select="head"/>
            </a>
          </h3>
        </td>
      </tr>
      
      <!--One can change the order of appearance for the children of did
        by changing the order of the following statements.-->
      <xsl:apply-templates select="repository"/>
      <xsl:apply-templates select="origination"/>
      <xsl:apply-templates select="unittitle"/>
      <xsl:apply-templates select="unitdate"/>
      <xsl:apply-templates select="physdesc"/>
      <xsl:apply-templates select="abstract"/>
      <xsl:apply-templates select="unitid"/>
      <xsl:apply-templates select="physloc"/>
      <xsl:apply-templates select="langmaterial"/>
      <xsl:apply-templates select="materialspec"/>
      <xsl:apply-templates select="note"/>
    </table>
    <hr/>
  </xsl:template>
  
  
  
  <!--This template formats the repostory, origination, physdesc, abstract,
    unitid, physloc and materialspec elements of archdesc/did which share a common presentaiton.
    The sequence of their appearance is governed by the previous template.-->
  <xsl:template match="archdesc/did/repository  | archdesc/did/origination  | archdesc/did/physdesc  | archdesc/did/unitid  | archdesc/did/physloc  | archdesc/did/abstract  | archdesc/did/langmaterial  | archdesc/did/materialspec">
    <!--The template tests to see if there is a label attribute,
      inserting the contents if there is or adding display textif there isn't.
      The content of the supplied label depends on the element.  To change the
      supplied label, simply alter the template below.-->
    <xsl:choose>
      <xsl:when test="@label">
        <tr>
          
          <td valign="top">
            <b>
              <xsl:value-of select="@label"/>
            </b>
          </td>
          <td>
            <xsl:apply-templates/>
          </td>
        </tr>
      </xsl:when>
      <xsl:otherwise>
        <tr>
          <td valign="top">
            <b>
              <xsl:choose>
                <xsl:when test="self::repository">
                  <xsl:text>Repository: </xsl:text>
                </xsl:when>
                <xsl:when test="self::origination">
                  <xsl:text>Creator: </xsl:text>
                </xsl:when>
                <xsl:when test="self::physdesc">
                  <xsl:text>Quantity: </xsl:text>
                </xsl:when>
                <xsl:when test="self::physloc">
                  <xsl:text>Location: </xsl:text>
                </xsl:when>
                <xsl:when test="self::unitid">
                  <xsl:text>Identification: </xsl:text>
                </xsl:when>
                <xsl:when test="self::abstract">
                  <xsl:text>Abstract:</xsl:text>
                </xsl:when>
                <xsl:when test="self::langmaterial">
                  <xsl:text>Language: </xsl:text>
                </xsl:when>
                <xsl:when test="self::materialspec">
                  <xsl:text>Technical: </xsl:text>
                </xsl:when>
              </xsl:choose>
            </b>
          </td>
          <td>
            <xsl:apply-templates/>
          </td>
        </tr>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
  
  <!-- The following two templates test for and processes various permutations
    of unittitle and unitdate.-->
  <xsl:template match="archdesc/did/unittitle">
    <!--The template tests to see if there is a label attribute for unittitle,
      inserting the contents if there is or adding one if there isn't. -->
    <xsl:choose>
      <xsl:when test="@label">
        <tr>
          
          <td valign="top">
            <b>
              <xsl:value-of select="@label"/>
            </b>
          </td>
          <td>
            <!--Inserts the text of unittitle and any children other that unitdate.-->
            <xsl:apply-templates select="text() |* [not(self::unitdate)]"/>
          </td>
        </tr>
      </xsl:when>
      
      <xsl:otherwise>
        <tr>
          
          <td valign="top">
            <b>
              <xsl:text>Title: </xsl:text>
            </b>
          </td>
          <td>
            <xsl:apply-templates select="text() |* [not(self::unitdate)]"/>
          </td>
        </tr>
      </xsl:otherwise>
    </xsl:choose>
    <!--If unitdate is a child of unittitle, it inserts unitdate on a new line.  -->
    <xsl:if test="child::unitdate">
      <!--The template tests to see if there is a label attribute for unittitle,
        inserting the contents if there is or adding one if there isn't. -->
      <xsl:choose>
        <xsl:when test="unitdate/@label">
          <tr>
            
            <td valign="top">
              <b>
                <xsl:value-of select="unitdate/@label"/>
              </b>
            </td>
            <td>
              <xsl:apply-templates select="unitdate"/>
            </td>
          </tr>
        </xsl:when>
        
        <xsl:otherwise>
          <tr>
            
            <td valign="top">
              <b>
                <xsl:text>Dates: </xsl:text>
              </b>
            </td>
            <td>
              <xsl:apply-templates select="unitdate"/>
            </td>
          </tr>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:if>
  </xsl:template>
  <!-- Processes the unit date if it is not a child of unit title but a child of did, the current context.-->
  <xsl:template match="archdesc/did/unitdate">
    
    <!--The template tests to see if there is a label attribute for a unittitle that is the
      child of did and not unittitle, inserting the contents if there is or adding one if there isn't.-->
    <xsl:choose>
      <xsl:when test="@label">
        <tr>
          
          <td valign="top">
            <b>
              <xsl:value-of select="@label"/>
            </b>
          </td>
          <td>
            <xsl:apply-templates/>
          </td>
        </tr>
      </xsl:when>
      
      <xsl:otherwise>
        <tr>
          
          <td valign="top">
            <b>
              <xsl:text>Dates: </xsl:text>
            </b>
          </td>
          <td>
            <xsl:apply-templates/>
          </td>
        </tr>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
  
  
  <!--This template processes the note element.-->
  <xsl:template match="archdesc/did/note">
    <xsl:for-each select="p">
      <!--The template tests to see if there is a label attribute,
        inserting the contents if there is or adding one if there isn't. -->
      <xsl:choose>
        <xsl:when test="parent::note[@label]">
          <!--This nested choose tests for and processes the first paragraph. Additional paragraphs do not get a label.-->
          <xsl:choose>
            <xsl:when test="position()=1">
              <tr>
                
                <td valign="top">
                  <b>
                    <xsl:value-of select="@label"/>
                  </b>
                </td>
                <td valign="top">
                  <xsl:apply-templates/>
                </td>
              </tr>
            </xsl:when>
            
            <xsl:otherwise>
              <tr>
                
                <td valign="top"/>
                <td valign="top">
                  <xsl:apply-templates/>
                </td>
              </tr>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <!--Processes situations where there is no
          label attribute by supplying default text.-->
        <xsl:otherwise>
          <!--This nested choose tests for and processes the first paragraph. Additional paragraphs do not get a label.-->
          <xsl:choose>
            <xsl:when test="position()=1">
              <tr>
                
                <td valign="top">
                  <b>
                    <xsl:text>Note: </xsl:text>
                  </b>
                </td>
                <td>
                  <xsl:apply-templates/>
                </td>
              </tr>
            </xsl:when>
            
            <xsl:otherwise>
              <tr>
                <td valign="top"/>
                <td>
                  <xsl:apply-templates/>
                </td>
              </tr>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:otherwise>
      </xsl:choose>
      <!--Closes each paragraph-->
    </xsl:for-each>
  </xsl:template>
  
  <!--This template rule formats the top-level bioghist element and
    creates a link back to the top of the page after the display of the element.-->
  <xsl:template match="archdesc/bioghist |    archdesc/scopecontent |    archdesc/phystech |    archdesc/odd   |    archdesc/arrangement">
    <xsl:if test="child::*">
      <xsl:apply-templates/>
<!--      <p>
        <a href="#">Return to the Table of Contents</a>
      </p>
-->      <hr/>
    </xsl:if>
  </xsl:template>
  
  <!--This template formats various head elements and makes them targets for
    links from the Table of Contents.-->
  <xsl:template match="archdesc/bioghist/head  |    archdesc/scopecontent/head |    archdesc/phystech/head |    archdesc/controlaccess/head |    archdesc/odd/head |    archdesc/arrangement/head">
    <h3>
      <a name="{xtf:make-id(.)}">
        <xsl:apply-templates/>
      </a>
    </h3>
  </xsl:template>
  
  <xsl:template match="archdesc/bioghist/p |    archdesc/scopecontent/p |    archdesc/phystech/p |    archdesc/controlaccess/p |    archdesc/odd/p |    archdesc/bioghist/note/p |    archdesc/scopecontent/note/p |    archdesc/phystech/note/p |    archdesc/controlaccess/note/p |    archdesc/odd/note/p">
    <p style="margin-left:25pt">
      <xsl:apply-templates/>
    </p>
  </xsl:template>
  
  <xsl:template match="archdesc/bioghist/bioghist/head |   archdesc/scopecontent/scopecontent/head">
    <h3 style="margin-left:25pt">
      <xsl:apply-templates/>
    </h3>
  </xsl:template>
  
  <xsl:template match="archdesc/bioghist/bioghist/p |   archdesc/scopecontent/scopecontent/p |   archdesc/bioghist/bioghist/note/p |   archdesc/scopecontent/scopecontent/note/p">
    <p style="margin-left: 50pt">
      <xsl:apply-templates/>
    </p>
  </xsl:template>
  
  <!-- The next two templates format an arrangement
    statement embedded in <scopecontent>.-->
  
  <xsl:template match="archdesc/scopecontent/arrangement/head">
    <h4 style="margin-left:25pt">
      <a name="{xtf:make-id(.)}">
        <xsl:apply-templates/>
      </a>
    </h4>
  </xsl:template>
  
  
  <xsl:template match="archdesc/scopecontent/arrangement/p  | archdesc/scopecontent/arrangement/note/p">
    <p style="margin-left:50pt">
      <xsl:apply-templates/>
    </p>
  </xsl:template>
  
  <!-- The next three templates format a list within an arrangement
    statement whether it is directly within <archdesc> or embedded in
    <scopecontent>.-->
  
  <xsl:template match="archdesc/scopecontent/arrangement/list/head">
    <div style="margin-left:25pt">
      <a name="{xtf:make-id(.)}">
        <xsl:apply-templates/>
      </a>
    </div>
  </xsl:template>
  
  <xsl:template match="archdesc/arrangement/list/head">
    <div style="margin-left:25pt">
      <a name="{xtf:make-id(.)}">
        <xsl:apply-templates/>
      </a>
    </div>
  </xsl:template>
  
  <xsl:template match="archdesc/scopecontent/arrangement/list/item  | archdesc/arrangement/list/item">
    <div style="margin-left:50pt">
      <a>
        <xsl:attribute name="href">#series<xsl:number/>
        </xsl:attribute>
        <xsl:apply-templates/>
      </a>
    </div>
  </xsl:template>
  
  <!--This template rule formats the top-level related material
    elements by combining any related or separated materials
    elements. It begins by testing to see if there related or separated
    materials elements with content.-->
  <xsl:template name="archdesc-relatedmaterial">
    <xsl:if test="archdesc/relatedmaterial or   archdesc/*/relatedmaterial or   archdesc/separatedmaterial or   archdesc/*/separatedmaterial">
      <h3>
        <a name="relatedmatlink">
          <b>
            <xsl:text>Related Material</xsl:text>
          </b>
        </a>
      </h3>
      <xsl:apply-templates select="archdesc/relatedmaterial/p     | archdesc/*/relatedmaterial/p     | archdesc/relatedmaterial/note/p     | archdesc/*/relatedmaterial/note/p"/>
      <xsl:apply-templates select="archdesc/separatedmaterial/p     | archdesc/*/separatedmaterial/p     | archdesc/separatedmaterial/note/p     | archdesc/*/separatedmaterial/note/p"/>
<!--      <p>
        <a href="#">Return to the Table of Contents</a>
      </p>
-->      <hr/>
    </xsl:if>
  </xsl:template>
  
  <xsl:template match="archdesc/relatedmaterial/p   | archdesc/*/relatedmaterial/p   | archdesc/separatedmaterial/p   | archdesc/*/separatedmaterial/p   | archdesc/relatedmaterial/note/p   | archdesc/*/relatedmaterial/note/p   | archdesc/separatedmaterial/note/p   | archdesc/*/separatedmaterial/note/p">
    <p style="margin-left: 25pt">
      <xsl:apply-templates/>
    </p>
  </xsl:template>
  
  <!--This template formats the top-level controlaccess element.
    It begins by testing to see if there is any controlled
    access element with content. It then invokes one of two templates
    for the children of controlaccess.  -->
  <xsl:template match="archdesc/controlaccess">
    <xsl:if test="child::*">
      <a name="{xtf:make-id(head)}">
        <xsl:apply-templates select="head"/>
      </a>
      <p style="text-indent:25pt">
        <xsl:apply-templates select="p | note/p"/>
      </p>
      <xsl:choose>
        <!--Apply this template when there are recursive controlaccess
          elements.-->
        <xsl:when test="controlaccess">
          <xsl:apply-templates mode="recursive" select="."/>
        </xsl:when>
        <!--Apply this template when the controlled terms are entered
          directly under the controlaccess element.-->
        <xsl:otherwise>
          <xsl:apply-templates mode="direct" select="."/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:if>
  </xsl:template>
  
  <!--This template formats controlled terms that are entered
    directly under the controlaccess element.  Elements are alphabetized.-->
  <xsl:template mode="direct" match="archdesc/controlaccess">
    <xsl:for-each select="subject |corpname | famname | persname | genreform | title | geogname | occupation">
      <xsl:sort select="." data-type="text" order="ascending"/>
      <div style="margin-left:50pt">
        <xsl:apply-templates/>
      </div>
    </xsl:for-each>
<!--    <p>
      <a href="#">
        Return to the Table of Contents
      </a>
    </p>
-->    <hr> </hr>
  </xsl:template>
  
  <!--When controlled terms are nested within recursive
    controlaccess elements, the template for controlaccess/controlaccess
    is applied.-->
  <xsl:template mode="recursive" match="archdesc/controlaccess">
    <xsl:apply-templates select="controlaccess"/>
<!--    <p>
      <a href="#">
        Return to the Table of Contents
      </a>
    </p>
-->    <hr> </hr>
  </xsl:template>
  
  <!--This template formats controlled terms that are nested within recursive
    controlaccess elements.   Terms are alphabetized within each grouping.-->
  <xsl:template match="archdesc/controlaccess/controlaccess">
    <h4 style="margin-left:25pt">
      <xsl:apply-templates select="head"/>
    </h4>
    <xsl:for-each select="subject |corpname | famname | persname | genreform | title | geogname | occupation">
      <xsl:sort select="." data-type="text" order="ascending"/>
      <div style="margin-left:50pt">
        <xsl:apply-templates/>
      </div>
    </xsl:for-each>
  </xsl:template>
  
  <!--This template rule formats a top-level access and use retriction elements.
    They are displayed under a common heading.
    It begins by testing to see if there are any restriction elements with content.-->
  <xsl:template name="archdesc-restrict">
    <xsl:if test="archdesc/userestrict/*   or archdesc/accessrestrict/*   or archdesc/*/userestrict/*   or archdesc/*/accessrestrict/*">
      <h3>
        <a name="restrictlink">
          <b>
            <xsl:text>Restrictions</xsl:text>
          </b>
        </a>
      </h3>
      <xsl:apply-templates select="archdesc/accessrestrict     | archdesc/*/accessrestrict"/>
      <xsl:apply-templates select="archdesc/userestrict     | archdesc/*/userestrict"/>
<!--      <p>
        <a href="#">Return to the Table of Contents</a>
      </p>
-->      <hr/>
    </xsl:if>
  </xsl:template>
  
  <xsl:template match="archdesc/accessrestrict/head  | archdesc/userestrict/head  | archdesc/*/accessrestrict/head  | archdesc/*/userestrict/head">
    <h4 style="margin-left: 25pt">
      <xsl:apply-templates/>
    </h4>
  </xsl:template>
  
  <xsl:template match="archdesc/accessrestrict/p  | archdesc/userestrict/p  | archdesc/*/accessrestrict/p  | archdesc/*/userestrict/p  | archdesc/accessrestrict/note/p  | archdesc/userestrict/note/p  | archdesc/*/accessrestrict/note/p  | archdesc/*/userestrict/note/p">
    <p style="margin-left:50pt">
      <xsl:apply-templates/>
    </p>
  </xsl:template>
  
  <!--This templates consolidates all the other administrative information
    elements into one block under a common heading.  It formats these elements
    regardless of which of three encodings has been utilized.  They may be
    children of archdesc, admininfo, or descgrp.
    It begins by testing to see if there are any elements of this type
    with content.-->
  
  <xsl:template name="archdesc-admininfo">
    <xsl:if test="archdesc/admininfo/custodhist/*   or archdesc/altformavailable/*   or archdesc/prefercite/*   or archdesc/acqinfo/*   or archdesc/processinfo/*   or archdesc/appraisal/*   or archdesc/accruals/*   or archdesc/*/custodhist/*   or archdesc/*/altformavailable/*   or archdesc/*/prefercite/*   or archdesc/*/acqinfo/*   or archdesc/*/processinfo/*   or archdesc/*/appraisal/*   or archdesc/*/accruals/*">
      <h3>
        <a name="adminlink">
          <xsl:text>Administrative Information</xsl:text>
        </a>
      </h3>
      <xsl:apply-templates select="archdesc/custodhist     | archdesc/*/custodhist"/>
      <xsl:apply-templates select="archdesc/altformavailable     | archdesc/*/altformavailable"/>
      <xsl:apply-templates select="archdesc/prefercite     | archdesc/*/prefercite"/>
      <xsl:apply-templates select="archdesc/acqinfo     | archdesc/*/acqinfo"/>
      <xsl:apply-templates select="archdesc/processinfo     | archdesc/*/processinfo"/>
      <xsl:apply-templates select="archdesc/appraisal     | archdesc/*/appraisal"/>
      <xsl:apply-templates select="archdesc/accruals     | archdesc/*/accruals"/>
<!--      <p>
        <a href="#">Return to the Table of Contents</a>
      </p>
-->      <hr/>
    </xsl:if>
  </xsl:template>
  
  
  <!--This template rule formats the head element of top-level elements of
    administrative information.-->
  <xsl:template match="custodhist/head   | archdesc/altformavailable/head   | archdesc/prefercite/head   | archdesc/acqinfo/head   | archdesc/processinfo/head   | archdesc/appraisal/head   | archdesc/accruals/head   | archdesc/*/custodhist/head   | archdesc/*/altformavailable/head   | archdesc/*/prefercite/head   | archdesc/*/acqinfo/head   | archdesc/*/processinfo/head   | archdesc/*/appraisal/head   | archdesc/*/accruals/head">
    <h4 style="margin-left:50pt">
      <a name="{xtf:make-id(.)}">
        <b>
          <xsl:apply-templates/>
        </b>
      </a>
    </h4>
  </xsl:template>
  
  <xsl:template match="custodhist/p   | archdesc/altformavailable/p   | archdesc/prefercite/p   | archdesc/acqinfo/p   | archdesc/processinfo/p   | archdesc/appraisal/p   | archdesc/accruals/p   | archdesc/*/custodhist/p   | archdesc/*/altformavailable/p   | archdesc/*/prefercite/p   | archdesc/*/acqinfo/p   | archdesc/*/processinfo/p   | archdesc/*/appraisal/p   | archdesc/*/accruals/p   | archdesc/custodhist/note/p   | archdesc/altformavailable/note/p   | archdesc/prefercite/note/p   | archdesc/acqinfo/note/p   | archdesc/processinfo/note/p   | archdesc/appraisal/note/p   | archdesc/accruals/note/p   | archdesc/*/custodhist/note/p   | archdesc/*/altformavailable/note/p   | archdesc/*/prefercite/note/p   | archdesc/*/acqinfo/note/p   | archdesc/*/processinfo/note/p   | archdesc/*/appraisal/note/p   | archdesc/*/accruals/note/p">
    
    <p style="margin-left:25pt">
      <xsl:apply-templates/>
    </p>
  </xsl:template>
  
  <xsl:template match="archdesc/otherfindaid   | archdesc/*/otherfindaid   | archdesc/bibliography   | archdesc/*/bibliography   | archdesc/phystech   | archdesc/originalsloc">
    <xsl:apply-templates/>
<!--    <p>
      <a href="#">Return to the Table of Contents</a>
    </p>
-->    <hr/>
  </xsl:template>
  
  <xsl:template match="archdesc/otherfindaid/head   | archdesc/*/otherfindaid/head   | archdesc/bibliography/head   | archdesc/*/bibliography/head   | archdesc/fileplan/head   | archdesc/*/fileplan/head   | archdesc/phystech/head   | archdesc/originalsloc/head">
    <h3>
      <a name="{xtf:make-id(.)}">
        <b>
          <xsl:apply-templates/>
        </b>
      </a>
    </h3>
  </xsl:template>
  
  <xsl:template match="archdesc/otherfindaid/p   | archdesc/*/otherfindaid/p   | archdesc/bibliography/p   | archdesc/*/bibliography/p   | archdesc/otherfindaid/note/p   | archdesc/*/otherfindaid/note/p   | archdesc/bibliography/note/p   | archdesc/*/bibliography/note/p   | archdesc/fileplan/p   | archdesc/*/fileplan/p   | archdesc/fileplan/note/p   | archdesc/*/fileplan/note/p   | archdesc/phystech/p   | archdesc/phystechc/note/p   | archdesc/originalsloc/p   | archdesc/originalsloc/note/p">
    <p style="margin-left:25pt">
      <xsl:apply-templates/>
    </p>
  </xsl:template>
  
  <!--This template rule tests for and formats the top-level index element. It begins
    by testing to see if there is an index element with content.-->
  <xsl:template match="archdesc/index   | archdesc/*/index">
    <table width="100%">
      <tr>
        <td width="5%"> </td>
        <td width="45%"> </td>
        <td width="50%"> </td>
      </tr>
      <tr>
        <td colspan="3">
          <h3>
            <a name="{xtf:make-id(head)}">
              <b>
                <xsl:apply-templates select="head"/>
              </b>
            </a>
          </h3>
        </td>
      </tr>
      <xsl:for-each select="p | note/p">
        <tr>
          <td/>
          <td colspan="2">
            <xsl:apply-templates/>
          </td>
        </tr>
      </xsl:for-each>
      
      <!--Processes each index entry.-->
      <xsl:for-each select="indexentry">
        
        <!--Sorts each entry term.-->
        <xsl:sort select="corpname | famname | function | genreform | geogname | name | occupation | persname | subject"/>
        <tr>
          <td/>
          <td>
            <xsl:apply-templates select="corpname | famname | function | genreform | geogname | name | occupation | persname | subject"/>
          </td>
          <!--Supplies whitespace and punctuation if there is a pointer
            group with multiple entries.-->
          
          <xsl:choose>
            <xsl:when test="ptrgrp">
              <td>
                <xsl:for-each select="ptrgrp">
                  <xsl:for-each select="ref | ptr">
                    <xsl:apply-templates/>
                    <xsl:if test="preceding-sibling::ref or preceding-sibling::ptr">
                      <xsl:text>, </xsl:text>
                    </xsl:if>
                  </xsl:for-each>
                </xsl:for-each>
              </td>
            </xsl:when>
            <!--If there is no pointer group, process each reference or pointer.-->
            <xsl:otherwise>
              <td>
                <xsl:for-each select="ref | ptr">
                  <xsl:apply-templates/>
                </xsl:for-each>
              </td>
            </xsl:otherwise>
          </xsl:choose>
        </tr>
        <!--Closes the indexentry.-->
      </xsl:for-each>
    </table>
<!--    <p>
      <a href="#">Return to the Table of Contents</a>
    </p>
-->    <hr/>
  </xsl:template>
  
  <xsl:function name="xtf:make-id">
    <xsl:param name="node"/>
    <xsl:choose>
      <xsl:when test="$node">
        <xsl:value-of select="concat(xtf:make-id($node/parent::*), '.', count($node/preceding-sibling::*))"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>node</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:function>
  
  <!--Insert the address for the dsc stylesheet of your choice here.-->
  <xsl:include href="dsc4.xsl"/>
</xsl:stylesheet>