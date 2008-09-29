<!-- EAD Cookbook Style 9     Version 0.9   30 July 2003 -->

<!--This stylesheet is provisional as of 30 July 2003 and may
be rolled back into style 6 as the only difference is in the
c02 links in the table of contents.
needs serious consideration for dropping as it no onger matches 6-->

<!--This stylesheet supports EAD Version 2002.-->

<!--This stylesheet generates a Table of Contents in an HTML table cell
along the left side of the screen. -->

<!--The table of contents includes
links to c02 elements that are identified as subseries.-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:strip-space elements="*"/>
	<xsl:output method="html" encoding="utf-8" doctype-public="-//W3C//DTD HTML 4.0 Transitional//EN"/>
	<!-- Creates the body of the finding aid.-->
	<xsl:template match="/ead">
		<html>
			<head>
				<style type="text/css">
				h1, h2, h3, h4 {font-family: arial}
				td {vertical-align: top}
				</style>
		
				<title>
					<xsl:value-of select="eadheader/filedesc/titlestmt/titleproper"/>
					<xsl:text>  </xsl:text>
					<xsl:value-of select="eadheader/filedesc/titlestmt/subtitle"/>
				</title>
		
				<xsl:call-template name="metadata"/>
			</head>
	
	<!--This part of the template creates a table for the finding aid with
	two columns. -->
			<body>
				<table width="100%" align="center">
					<tr>
						<!--In the left column is the table of contents.  -->
			
						<td valign="top" bgcolor="#E0E0E0" width="20%">
							<xsl:call-template name="toc"/>
						</td>
				
						<!--The body of the finding aid is in the right column.  -->	
						<td valign="top" bgcolor="#FAFDD5" width="80%">
	
				<!--This part of template inserts a logo and title
				at the top of the display.  Insert the proper path ro
				your image in place of yourlogo.gif.
				
				If you do not want to include an image, delete the center
				 element and its contents.-->
				<center>
					<img src="yourlogo.gif"></img>
				</center>
						
				<xsl:apply-templates select="eadheader"/>
													
				<hr></hr>
				
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
							<xsl:call-template name="archdesc-arrangement"/>
							<xsl:call-template name="archdesc-restrict"/>
							<xsl:call-template name="archdesc-relatedmaterial"/>
							<xsl:apply-templates select="archdesc/controlaccess"/>
							<xsl:apply-templates select="archdesc/odd"/>
							<xsl:call-template name="archdesc-admininfo"/>
							<xsl:apply-templates select="archdesc/descgrp"/>
							<xsl:apply-templates select="archdesc/otherfindaid | archdesc/*/otherfindiad"/>
							<xsl:apply-templates select="archdesc/bibliography | archdesc/*/bibliogrpahy"/>
							<xsl:apply-templates select="archdesc/*/index | archdesc/index"/>
							<xsl:apply-templates select="archdesc/dsc"/>				
						</td>
					</tr>
				</table>
			</body>
		</html>
	</xsl:template>
	
<!--This template creates HTML meta tags that are inserted into the HTML ouput
for use by web search engines indexing this file.   The content of each
resulting META tag uses Dublin Core semantics and is drawn from the text of
the finding aid.-->
	<xsl:template name="metadata">
		<meta http-equiv="Content-Type" name="dc.title"
		content="{eadheader/filedesc/titlestmt/titleproper&#x20; }{eadheader/filedesc/titlestmt/subtitle}"/>
		<meta http-equiv="Content-Type" name="dc.author" content="{archdesc/did/origination}"/>
		
		<xsl:for-each select="//controlaccess/persname | //controlaccess/corpname">
			<xsl:choose>
				<xsl:when test="@encodinganalog='600'">
				<meta http-equiv="Content-Type" name="dc.author" content="{.}"/>
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

	<!--This template creates the Table of Contents column for the finding aid.-->
	<xsl:template name="toc">

		<h3>TABLE OF CONTENTS</h3>
		<br></br>
		<!-- The Table of Contents template performs a series of tests to
determine which elements will be included in the table
of contents.  Each if statement tests to see if there is
a matching element with content in the finding aid.-->
		<xsl:if test="string(archdesc/did/head)">
			<p>
				<b>
					<a href="#{generate-id(archdesc/did/head)}">
						<xsl:value-of select="archdesc/did/head"/>
					</a>
				</b>
			</p>
		</xsl:if>
		<xsl:if test="string(archdesc/bioghist/head)">
			<p style="margin-top:-5pt">
				<b>
					<a href="#{generate-id(archdesc/bioghist/head)}">
						<xsl:value-of select="archdesc/bioghist/head"/>
					</a>
				</b>
			</p>
		</xsl:if>
		<xsl:if test="string(archdesc/scopecontent/head)">
			<p style="margin-top:-5pt">
				<b>
					<a href="#{generate-id(archdesc/scopecontent/head)}">
						<xsl:value-of select="archdesc/scopecontent/head"/>
					</a>
				</b>
			</p>
		</xsl:if>
		<xsl:if test="string(archdesc/organization/head) or string(archdesc/arrangement/head)">
			<p style="margin-top:-5pt">
				<b>
					<a href="#arrangementlink">
						<xsl:text>Arrangement</xsl:text>
					</a>
				</b>
			</p>
		</xsl:if>
		
		<xsl:if test="string(archdesc/userestrict/head)
		or string(archdesc/accessrestrict/head)
		or string(archdesc/*/userestrict/head)
		or string(archdesc/*/accessrestrict/head)">
			<p style="margin-top:-5pt">
				<b>
					<a href="#restrictlink">
						<xsl:text>Restrictions</xsl:text>
					</a>
				</b>
			</p>
		</xsl:if>
		<xsl:if test="string(archdesc/controlaccess/head)">
			<p style="margin-top:-5pt">
				<b>
					<a href="#{generate-id(archdesc/controlaccess/head)}">
						<xsl:value-of select="archdesc/controlaccess/head"/>
					</a>
				</b>
			</p>
		</xsl:if>
		<xsl:if test="string(archdesc/relatedmaterial/head)
		or string(archdesc/separatedmaterial/head)
		or string(archdesc/*/relatedmaterial/head)
		or string(archdesc/*/separatedmaterial/head)">
			<p style="margin-top:-5pt">
				<b>
					<a href="#relatedmatlink">
						<xsl:text>Related Material</xsl:text>
					</a>
				</b>
			</p>
		</xsl:if>
		<xsl:if test="string(archdesc/acqinfo/*)
		or string(archdesc/processinfo/*)
		or string(archdesc/prefercite/*)
		or string(archdesc/custodialhist/*)
		or string(archdesc/processinfo/*)
		or string(archdesc/appraisal/*)
		or string(archdesc/accruals/*)
		or string(archdesc/*/acqinfo/*)
		or string(archdesc/*/processinfo/*)
		or string(archdesc/*/prefercite/*)
		or string(archdesc/*/custodialhist/*)
		or string(archdesc/*/procinfo/*)
		or string(archdesc/*/appraisal/*)
		or string(archdesc/*/accruals/*)">
			<p style="margin-top:-5pt">
				<b>
					<a href="#adminlink">
					<xsl:text>Administrative Information</xsl:text>
					</a>
				</b>
			</p>
		</xsl:if>
		<xsl:if test="string(archdesc/otherfindaid/head)
			or string(archdesc/*/otherfindaid/head)">
			<p style="margin-top:-5pt">
				<b>
					<xsl:choose>
						<xsl:when test="archdesc/otherfindaid/head">
							<a href="#{generate-id(archdesc/otherfindaid/head)}">
								<xsl:value-of select="archdesc/otherfindaid/head"/>
							</a>
						</xsl:when>
						<xsl:when test="archdesc/*/otherfindaid/head">
							<a href="#{generate-id(archdesc/*/otherfindaid/head)}">
								<xsl:value-of select="archdesc/*/otherfindaid/head"/>
							</a>
						</xsl:when>
					</xsl:choose>
				</b>
			</p>
		</xsl:if>
		
		<!--The next test covers the situation where there is more than one odd element
			in the document.-->
		<xsl:if test="string(archdesc/odd/head)">
			<xsl:for-each select="archdesc/odd">
				<p style="margin-top:-5pt">
					<b>
						<a href="#{generate-id(head)}">
							<xsl:value-of select="head"/>
						</a>
					</b>
				</p>
			</xsl:for-each>
		</xsl:if>
		
		<xsl:if test="string(archdesc/bibliography/head)
			or string(archdesc/*/bibliography/head)">
			<p style="margin-top:-5pt">
				<b>
					<xsl:choose>
						<xsl:when test="archdesc/bibliography/head">
							<a href="#{generate-id(archdesc/bibliography/head)}">
								<xsl:value-of select="archdesc/bibliography/head"/>
							</a>
						</xsl:when>
						<xsl:when test="archdesc/*/bibliography/head">
							<a href="#{generate-id(archdesc/*/bibliography/head)}">
								<xsl:value-of select="archdesc/*/bibliography/head"/>
							</a>
						</xsl:when>
					</xsl:choose>
				</b>
			</p>
		</xsl:if>
		
		<xsl:if test="string(archdesc/index/head)
			or string(archdesc/*/index/head)">
			<p style="margin-top:-5pt">
				<b>
					<xsl:choose>
						<xsl:when test="archdesc/index/head">
							<a href="#{generate-id(archdesc/index/head)}">
								<xsl:value-of select="archdesc/index/head"/>
							</a>
						</xsl:when>
						<xsl:when test="archdesc/*/index/head">
							<a href="#{generate-id(archdesc/*/index/head)}">
								<xsl:value-of select="archdesc/*/index/head"/>
							</a>
						</xsl:when>
					</xsl:choose>
				</b>
			</p>
		</xsl:if>
		
		<xsl:if test="string(archdesc/dsc/head)">
			<p style="margin-top:-5pt">
				<b>
					<a href="#{generate-id(archdesc/dsc/head)}">
						<xsl:value-of select="archdesc/dsc/head"/>
					</a>
				</b>
			</p>
			<!-- Displays the unittitle and unitdates for a c01 if it is a series (as
evidenced by the level attribute series)and numbers them
to form a hyperlink to each.   Delete this section if you do not
wish the c01 titles to appear in the table of contents.-->
			<xsl:for-each select="archdesc/dsc/c01[@level='series']">
				<p style="margin-left:10pt; margin-top:-5pt; font-size:10pt">
					<b>
						<a href="#{generate-id(.)}">
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
						</a>
					</b>
				</p>
				
				<xsl:for-each select="c02[@level='subseries']">
				<p style="margin-left:15pt; margin-top:-5pt; font-size:10pt">
					<b>
						<a href="#{generate-id(.)}">
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
						</a>
					</b>
				</p>
			</xsl:for-each>
			</xsl:for-each>
			
			<!--This ends the section that causes the c01 titles to appear in the table of contents.-->
		</xsl:if>
		<!--End of the table of contents. -->
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
	
	<!--This template rule formats a list element.-->
	<xsl:template match="list/head">
		<div style="margin-left: 30pt">
			<b>
				<xsl:apply-templates/>
			</b>
		</div>
	</xsl:template>
		
	<xsl:template match="list/item">
			<div style="margin-left: 40pt">
				<xsl:apply-templates/>
			</div>
	</xsl:template>
	
	<!--Formats a simple table. The width of each column is defined by the colwidth attribute in a colspec element.-->
	<xsl:template match="table">
		<h3>
			<xsl:apply-templates select="head"/>
		</h3>
		<table width="100%">
			<xsl:for-each select="tgroup">
				<tr>
					<xsl:for-each select="colspec">
						<td width="{@colwidth}"></td>
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
		<table width="100%">
			<tr>
				<td width="5%"> </td>
				<td width="30%"> </td>
				<td width="65%"> </td>
			</tr>
			<xsl:apply-templates/>
		</table>
	</xsl:template>
	
	<xsl:template match="chronlist/head">
	<tr>
			<td> </td>
			<td>
				<b>
				<xsl:apply-templates/>
				</b>
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

	<xsl:template match="chronlist/chronitem">
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
		<a name="{generate-id(titlestmt/titleproper)}">
			<xsl:value-of select="filedesc/titlestmt/titleproper"/>
		</a>
	</h2>
	<h3 style="text-align:center">
			<xsl:value-of select="filedesc/titlestmt/subtitle"/>
	</h3>
	<br></br>
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
						<a name="{generate-id(head)}">
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
		<hr></hr>
	</xsl:template>
	


	<!--This template formats the repostory, origination, physdesc, abstract,
	unitid, physloc and materialspec elements of archdesc/did which share a common presentaiton.
	The sequence of their appearance is governed by the previous template.-->
	<xsl:template match="archdesc/did/repository
	| archdesc/did/origination
	| archdesc/did/physdesc
	| archdesc/did/unitid
	| archdesc/did/physloc
	| archdesc/did/abstract
	| archdesc/did/langmaterial
	| archdesc/did/materialspec">
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
								
								<td valign="top"></td>
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
								<td valign="top"></td>
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
	<xsl:template match="archdesc/bioghist |
			archdesc/scopecontent |
			archdesc/phystech |
			archdesc/odd">
		<xsl:if test="string(child::*)">	
			<xsl:apply-templates/>
			<p>
				<a href="#">Return to the Table of Contents</a>
			</p>
			<hr></hr>
		</xsl:if>
	</xsl:template>
	
	<!--This template formats various head elements and makes them targets for
		links from the Table of Contents.-->
	<xsl:template match="archdesc/bioghist/head  |
			archdesc/scopecontent/head |
			archdesc/phystech/head |
			archdesc/controlaccess/head |
			archdesc/odd/head">
		<h3>
			<a name="{generate-id()}">
				<xsl:apply-templates/>
			</a>
		</h3>
	</xsl:template>

	<xsl:template match="archdesc/bioghist/p |
			archdesc/scopecontent/p |
			archdesc/phystech/p |
			archdesc/controlaccess/p |
			archdesc/odd/p">
		<p style="margin-left:25pt">
			<xsl:apply-templates/>
		</p>
	</xsl:template>
	
	<xsl:template match="archdesc/bioghist/bioghist/head |
		archdesc/scopecontent/scopecontent/head">
		<h3 style="margin-left:25pt">
			<xsl:apply-templates/>
		</h3>
	</xsl:template>
	
	<xsl:template match="archdesc/bioghist/bioghist/p |
		archdesc/scopecontent/scopecontent/p">
		<p style="margin-left: 50pt">
			<xsl:apply-templates/>
		</p>
	</xsl:template>
	
	<!-- This formats an organization list embedded in a scope content statement.-->
	<xsl:template match="archdesc/scopecontent/organization/head |
		archdesc/scopecontent/arrangement/head">
		<h4 style="margin-left:25pt">
			<b>
				<xsl:apply-templates/>
			</b>
		</h4>
	</xsl:template>
	
	<xsl:template match="archdesc/scopecontent/organization/p |
		archdesc/scopecontent/arrangement/p">
		<p style="margin-left:25pt">
			<xsl:apply-templates/>
		</p>
	</xsl:template>
	
	<xsl:template match="archdesc/scopecontent/organization/list/head |
		archdesc/scopecontent/arrangement/list/head">
		<div style="margin-left:25pt">
			<xsl:apply-templates/>
		</div>
	</xsl:template>
	
	<xsl:template match="archdesc/scopecontent/organization/list/item |
		archdesc/scopecontent/arrangement/list/item">
		<div style="margin-left:50pt">
			<a>
				<xsl:attribute name="href">#series
					<xsl:number/>
				</xsl:attribute>
				<xsl:apply-templates/>
			</a>
		</div>
	</xsl:template>
	
	<!--This template rule formats the organization and/or arrangement
	elements.  It supplies the heading Arrangement and makes it a target
	for a link from the Table of Contents.-->
	<xsl:template name="archdesc-arrangement">
		<xsl:if test="string(archdesc/arrangement/child::*) or string(archdesc/organization/child::*)">
			<h3>
			<a name="arrangementlink">
				<xsl:text>Arrangement</xsl:text>
			</a>
			</h3>
			<xsl:for-each select="archdesc/organization | archdesc/arrangement">
				<xsl:apply-templates select="*[not(self::head)]"/>
			</xsl:for-each>
			<p>
				<a href="#">Return to the Table of Contents</a>
			</p>
			<hr></hr>
		</xsl:if>
	</xsl:template>

	<!--This template formats the paragraph
	 in an organization or arrangment element.-->
	<xsl:template match="archdesc/organization/p |
			archdesc/arrangement/p">
		<p style="margin-left:25pt">
			<xsl:apply-templates/>
		</p>
	</xsl:template>
	
	<!--This template formats the head element
	 in an organization or arrangment element list.-->
	<xsl:template match="archdesc/organization/list/head |
			archdesc/arrangement/list/head">
		<div style="margin-left:25pt">
			<xsl:apply-templates/>
		</div>
	</xsl:template>
	
	<!--This template formats an item
	 in an organization or arrangment element list
	 and makes the source for a link to the series
	 descriptions themselves.-->
	<xsl:template match="archdesc/organization/list/item |
			archdesc/arrangement/list/item">
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
		<xsl:if test="string(archdesc/relatedmaterial) or
		string(archdesc/*/relatedmaterial) or
		string(archdesc/separatedmaterial) or
		string(archdesc/*/separatedmaterial)">
			<h3>
				<a name="relatedmatlink">
					<b>
						<xsl:text>Related Material</xsl:text>
					</b>
				</a>
			</h3>
			<xsl:apply-templates select="archdesc/relatedmaterial/p
				| archdesc/*/relatedmaterial/p"/>
			<xsl:apply-templates select="archdesc/separatedmaterial/p
				| archdesc/*/separatedmaterial/p"/>	
			<p>
				<a href="#">Return to the Table of Contents</a>
			</p>
			<hr></hr>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="archdesc/relatedmaterial/p
		| archdesc/*/relatedmaterial/p
		| archdesc/separatedmaterial/p
		| archdesc/*/separatedmaterial/p">
		<p style="margin-left: 25pt">
			<xsl:apply-templates/>
		</p>
	</xsl:template>
	
	<!--This template formats the top-level controlaccess element.
	It begins by testing to see if there is any controlled
	access element with content. It then invokes one of two templates
	for the children of controlaccess.  -->
	<xsl:template match="archdesc/controlaccess">
		<xsl:if test="string(child::*)">
			<a name="{generate-id(head)}">
				<xsl:apply-templates select="head"/>
			</a>
			<p style="text-indent:25pt">
				<xsl:apply-templates select="p"/>
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
		<p>	
			<a href="#">
				Return to the Table of Contents
			</a>
		</p>
		<hr> </hr>
	</xsl:template>
	
	<!--When controlled terms are nested within recursive
	controlaccess elements, the template for controlaccess/controlaccess
	is applied.-->
	<xsl:template mode="recursive" match="archdesc/controlaccess">
			<xsl:apply-templates select="controlaccess"/>
		<p>
		<a href="#">
			Return to the Table of Contents
		</a>
		</p>
		<hr> </hr>
	</xsl:template>

	<!--This template formats controlled terms that are nested within recursive
	controlaccess elements.   Terms are alphabetized within each grouping.-->
	<xsl:template match="archdesc/controlaccess/controlaccess">
		<h4 style="margin-left:25pt">
			<xsl:apply-templates select="head"/>
		</h4>
		<xsl:for-each select="subject |corpname |famname | persname | genreform | title | geogname | occupation">
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
		<xsl:if test="string(archdesc/userestrict/*)
		or string(archdesc/accessrestrict/*)
		or string(archdesc/*/userestrict/*)
		or string(archdesc/*/accessrestrict/*)">
			<h3>
				<a name="restrictlink">
					<b>
						<xsl:text>Restrictions</xsl:text>
					</b>
				</a>
			</h3>
			<xsl:apply-templates select="archdesc/accessrestrict
				| archdesc/*/accessrestrict"/>
			<xsl:apply-templates select="archdesc/userestrict
				| archdesc/*/userestrict"/>
			<p>
				<a href="#">Return to the Table of Contents</a>
			</p>
			<hr></hr>
		</xsl:if>
	</xsl:template>

	<xsl:template match="archdesc/accessrestrict/head
	| archdesc/userestrict/head
	| archdesc/*/accessrestrict/head
	| archdesc/*/userestrict/head">
		<h4 style="margin-left: 25pt">
			<xsl:apply-templates/>
		</h4>
	</xsl:template>

	<xsl:template match="archdesc/accessrestrict/p
	| archdesc/userestrict/p
	| archdesc/*/accessrestrict/p
	| archdesc/*/userestrict/p">
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
		<xsl:if test="string(archdesc/admininfo/custodhist/*)
		or string(archdesc/altformavailable/*)
		or string(archdesc/prefercite/*)
		or string(archdesc/acqinfo/*)
		or string(archdesc/processinfo/*)
		or string(archdesc/appraisal/*)
		or string(archdesc/accruals/*)
		or string(archdesc/*/custodhist/*)
		or string(archdesc/*/altformavailable/*)
		or string(archdesc/*/prefercite/*)
		or string(archdesc/*/acqinfo/*)
		or string(archdesc/*/processinfo/*)
		or string(archdesc/*/appraisal/*)
		or string(archdesc/*/accruals/*)">
			<h3>
				<a name="adminlink">
					<xsl:text>Administrative Information</xsl:text>
				</a>
			</h3>
			<xsl:apply-templates select="archdesc/custodhist
				| archdesc/*/custodhist"/>
			<xsl:apply-templates select="archdesc/altformavailable
				| archdesc/*/altformavailable"/>
			<xsl:apply-templates select="archdesc/prefercite
				| archdesc/*/prefercite"/>
			<xsl:apply-templates select="archdesc/acqinfo
				| archdesc/*/acqinfo"/>
			<xsl:apply-templates select="archdesc/processinfo
				| archdesc/*/processinfo"/>
			<xsl:apply-templates select="archdesc/admininfo/appraisal
				| archdesc/*/appraisal"/>
			<xsl:apply-templates select="archdesc/admininfo/accruals
				| archdesc/*/accruals"/>
			<p>
				<a href="#">Return to the Table of Contents</a>
			</p>
			<hr></hr>
		</xsl:if>
	</xsl:template>
	

	<!--This template rule formats the head element of top-level elements of
	administrative information.-->
		<xsl:template match="custodhist/head
		| archdesc/altformavailable/head
		| archdesc/prefercite/head
		| archdesc/acqinfo/head
		| archdesc/processinfo/head
		| archdesc/appraisal/head
		| archdesc/accruals/head
		| archdesc/*/custodhist/head
		| archdesc/*/altformavailable/head
		| archdesc/*/prefercite/head
		| archdesc/*/acqinfo/head
		| archdesc/*/processinfo/head
		| archdesc/*/appraisal/head
		| archdesc/*/accruals/head">
		<h4 style="margin-left:25pt">
			<a name="{generate-id()}">
				<b>
					<xsl:apply-templates/>
				</b>
			</a>
		</h4>
	</xsl:template>	
		
	<xsl:template match="custodhist/p
		| archdesc/altformavailable/p
		| archdesc/prefercite/p
		| archdesc/acqinfo/p
		| archdesc/processinfo/p
		| archdesc/appraisal/p
		| archdesc/accruals/p
		| archdesc/*/custodhist/p
		| archdesc/*/altformavailable/p
		| archdesc/*/prefercite/p
		| archdesc/*/acqinfo/p
		| archdesc/*/processinfo/p
		| archdesc/*/appraisal/p
		| archdesc/*/accruals/p">
		
		<p style="margin-left:25pt">
			<xsl:apply-templates/>
		</p>
	</xsl:template>
	
	<xsl:template match="archdesc/otherfindaid
		| archdesc/*/otherfindaid
		| archdesc/bibliography
		| archdesc/*/bibliography
		| archdesc/descgrp">
			<xsl:apply-templates/>

			<hr></hr>
	</xsl:template>
		
	<xsl:template match="archdesc/otherfindaid/head
		| archdesc/*/otherfindaid/head
		| archdesc/bibliography/head
		| archdesc/*/bibliography/head
		| archdesc/fileplan/head
		| archdesc/*/fileplan/head
		| archdesc/descgrp/head">
		<h3>
			<b>
				<xsl:apply-templates/>
			</b>
		</h3>
	</xsl:template>

	<xsl:template match="archdesc/otherfindaid/p
		| archdesc/*/otherfindaid/p
		| archdesc/bibliography/p
		| archdesc/*/bibliography/p
		| archdesc/otherfindaid/note/p
		| archdesc/*/otherfindaid/note/p
		| archdesc/bibliography/note/p
		| archdesc/*/bibliography/note/p
		| archdesc/fileplan/p
		| archdesc/*/fileplan/p
		| archdesc/fileplan/note/p
		| archdesc/*/fileplan/note/p
		| archdesc/descgrp/p
		| archdesc/descgrp/note/p">
		<p style="margin-left:25pt">
			<xsl:apply-templates/>
		</p>
	</xsl:template>


	<!--This template rule tests for and formats the top-level index element. It begins
	by testing to see if there is an index element with content.-->
	<xsl:template match="archdesc/index
		| archdesc/*/index">
			<table width="100%">
				<tr>
					<td width="5%"> </td>
					<td width="45%"> </td>
					<td width="50%"> </td>
				</tr>
				<tr>
					<td colspan="3">
						<h3>
							<a name="{generate-id(head)}">
								<b>
									<xsl:apply-templates select="head"/>
								</b>
							</a>
						</h3>
					</td>
				</tr>
				<xsl:for-each select="p">
					<tr>
						<td></td>
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
						<td></td>
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
			<p>
				<a href="#">Return to the Table of Contents</a>
			</p>
			<hr></hr>
	</xsl:template>

<!--Insert the address for the dsc stylesheet of your choice here.-->
	<xsl:include href="dsc14a.xsl"/>
</xsl:stylesheet>