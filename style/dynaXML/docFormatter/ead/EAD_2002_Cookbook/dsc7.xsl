<!--Revision date 4 January 2004-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<!-- This stylesheet formats the dsc portion of a finding aid.-->
	<!--It formats components that have 2 container elements of any type.-->
	<!--It assumes that c01 and optionally <c02> is a high-level description
	such as a series, subgroup or subcollection and does not have container
	elements associated with it. However, it does accommodate situations
	where there a <c01> that is a file is occasionally interspersed. However,
	if <c01> is always a file, use dsc13.xsl instead. -->
	<!--The position and content of column headings are determined
	by the presence of <thead> elements encoded in the finding aid.-->
	<!--The content of initial container elements is displayed when
	either the content or the @type of a component's first container
	differs from that of the comparable container in the preceding component. -->

<!-- .................Section 1.................. -->

<!--This section of the stylesheet formats dsc, its head, and
any introductory paragraphs.-->

	<xsl:template match="archdesc/dsc">
			<xsl:apply-templates/>
	</xsl:template>
	
	<!--Formats dsc/head and makes it a link target.-->
	<xsl:template match="dsc/head">
		<h3>
			<a name="{generate-id()}">
				<xsl:apply-templates/>
			</a>
		</h3>
	</xsl:template>
	
	<xsl:template match="dsc/p | dsc/note/p">
		<p style="margin-left:25pt">
			<xsl:apply-templates/>
		</p>
	</xsl:template>
	
		
<!-- ................Section 2 ...........................-->
<!--This section of the stylesheet contains two named-templates
that are used generically throughout the stylesheet.-->

	<!--This template formats the unitid, origination, unittitle,
	unitdate, and physdesc elements of components at all levels.  They appear on
	a separate line from other did elements. It is generic to all
	component levels.-->
	
	<xsl:template name="component-did">
		<!--Inserts unitid and a space if it exists in the markup.-->
		<xsl:if test="unitid">
			<xsl:apply-templates select="unitid"/>
			<xsl:text>&#x20;</xsl:text>
		</xsl:if>

		<!--Inserts origination and a space if it exists in the markup.-->
		<xsl:if test="origination">
			<xsl:apply-templates select="origination"/>
			<xsl:text>&#x20;</xsl:text>
		</xsl:if>

		<!--This choose statement selects between cases where unitdate is a child of
		unittitle and where it is a separate child of did.-->
		<xsl:choose>
			<!--This code processes the elements when unitdate is a child
			of unittitle.-->
			<xsl:when test="unittitle/unitdate">
				<xsl:apply-templates select="unittitle/text()| unittitle/*[not(self::unitdate)]"/>
				<xsl:text>&#x20;</xsl:text>
				<xsl:for-each select="unittitle/unitdate">
					<xsl:apply-templates/>
					<xsl:text>&#x20;</xsl:text>
				</xsl:for-each>
			</xsl:when>

			<!--This code process the elements when unitdate is not a
					child of untititle-->
			<xsl:otherwise>
				<xsl:apply-templates select="unittitle"/>
				<xsl:text>&#x20;</xsl:text>
				<xsl:for-each select="unitdate">
					<xsl:apply-templates/>
					<xsl:text>&#x20;</xsl:text>
				</xsl:for-each>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:apply-templates select="physdesc"/>
	</xsl:template>
	
	<!--This template formats the appearance of <thead> elements
	where ever they occur in <dsc>.-->

	<xsl:template match="thead">
		<xsl:for-each select="row">
			<tr>
				<xsl:for-each select="entry">
					<td>
						<b>
							<xsl:apply-templates/>
						</b>
					</td>
				</xsl:for-each>
			</tr>
		</xsl:for-each>
	</xsl:template>
	
<!-- ...............Section 3.............................. -->
<!--This section of the stylesheet creates an HTML table for each c01.
It then recursively processes each child component of the
c01 by calling a named template specific to that component level.
The named templates are in section 4.-->

	<xsl:template match="c01">
		<table width="100%">
			<tr>
				<td width="8%"> </td>
				<td width="8%"> </td>
				<td width="8%"> </td>
				<td width="8%"> </td>
				<td width="8%"> </td>
				<td width="8%"> </td>
				<td width="8%"> </td>
				<td width="8%"> </td>
				<td width="8%"> </td>
				<td width="8%"> </td>
				<td width="8%"> </td>
				<td width="12%"> </td>
			</tr>
			<xsl:choose>
				<xsl:when test="did/container">
					<xsl:call-template name="c01-container"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:apply-templates/>
				</xsl:otherwise>
			</xsl:choose>
		</table>
	</xsl:template>
	
<!-- ...............Section 4.............................. -->
<!--This section of the stylesheet contains separate templates for
each component level.  The contents of each is identical except for the
spacing that is inserted to create the proper column display in HTML
for each level.-->

	<!--Processes c01 which is assumed to be a series
	description without associated components.-->
	<xsl:template match="c01/did">
		<tr>
				<td colspan="12">
					<b>
						<a>
							<xsl:attribute name="name">
								<xsl:text>series</xsl:text><xsl:number from="dsc" count="c01 "/>
							</xsl:attribute>
							<xsl:call-template name="component-did"/>
						</a>
					</b>
				</td>
			</tr>
			<xsl:for-each select="abstract | note/p | langmaterial | materialspec">
				<tr>
					<td> </td>
					<td> </td>
					<td colspan="10" valign="top">
						<xsl:apply-templates/>
					</td>
				</tr>
			</xsl:for-each>
	</xsl:template>

		<!--This template creates a separate row for each child of
		the listed elements.-->
	<xsl:template match="c01/scopecontent | c01/bioghist | c01/arrangement
			| c01/userestrict | c01/accessrestrict | c01/processinfo |
			c01/acqinfo | c01/custodhist | c01/controlaccess/controlaccess |
			c01/odd | c01/note | c01/descgrp/*">
			<xsl:for-each select="head">
				<tr>
					<td> </td>
					<td> </td>
					<td colspan="10">
						<b>
							<xsl:apply-templates/>
						</b>
					</td>
				</tr>
			</xsl:for-each>
			<xsl:for-each select="*[not(self::head)]">
				<tr>
					<td> </td>
					<td> </td>
					<td colspan="10">
						<xsl:apply-templates/>
					</td>
				</tr>
			</xsl:for-each>
	</xsl:template>
	
	<!--This template processes c01 elements that have associated containers, for
	example when c01 is a file. -->
	<xsl:template name="c01-container">
		<xsl:for-each select="did">

		<!--The next two variables define the set of container types that
		may appear in the first column of a two column container list.
		Add or subtract container types to fix institutional practice.-->
		<xsl:variable name="first" select="container[@type='Box' or @type='Oversize' or @type='Volume' or @type='Carton']"/>

		<!--This variable defines the set of container types that
		may appear in the second column of a two column container list.
		Add or subtract container types to fix institutional practice.-->
		<xsl:variable name="second" select="container[@type='Folder' or @type='Frame' or @type='Page'  or @type='Reel']"/>

		<xsl:variable name="preceding" select="preceding::did[1]/container[@type='Box' or @type='Oversize' or @type='Volume' or @type='Carton' or @type='Reel']"/>

		<xsl:choose>
			<!--When the container value or the container type of the first
			 container is not are the same as that of the comparable container
			in the previous component, insert column heads and the contents of
			the container elements.-->
			<xsl:when test="not($preceding=$first) or
			not($preceding/@type=$first/@type)">
				 <tr>
					<td valign="top">
						<xsl:apply-templates select="$first"/>
					</td>
					<td valign="top">
						<xsl:apply-templates select="$second"/>
					</td>
					<td valign="top" colspan="10">
						<xsl:call-template name="component-did"/>
					</td>
				</tr>
			</xsl:when>
			<xsl:otherwise>
				<tr>
					<td valign="top"> </td>
					<td valign="top">
						<xsl:value-of select="$second"/>
					</td>
					<td valign="top" colspan="10">
						<xsl:call-template name="component-did"/>
					</td>
				</tr>
			</xsl:otherwise>
		</xsl:choose>
			<xsl:for-each select="abstract | note/p | langmaterial | materialspec">
				<tr>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td colspan="8" valign="top">
						<xsl:apply-templates/>
					</td>
				</tr>
			</xsl:for-each>
		</xsl:for-each><!--Closes the did.-->

		<xsl:for-each select="scopecontent | bioghist | arrangement |
			userestrict | accessrestrict | processinfo |
			acqinfo | custodhist | controlaccess/controlaccess | odd | note | descgrp/*">
			<xsl:for-each select="head">
				<tr>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td colspan="8">
						<b>
							<xsl:apply-templates/>
						</b>
					</td>
				</tr>
			</xsl:for-each>
			<xsl:for-each select="*[not(self::head)]">
				<tr>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td colspan="8">
						<xsl:apply-templates/>
					</td>
				</tr>
			</xsl:for-each>
		</xsl:for-each>
		<xsl:apply-templates select="c02 | thead"/>
	</xsl:template>

	<!--This template processes c02 elements.-->
	<xsl:template match="c02">
		<xsl:choose>
			<xsl:when test="not(did[container])">
				<xsl:call-template name="c02-level-subseries"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:apply-templates/>
			</xsl:otherwise>
		</xsl:choose>
		
	</xsl:template>

	<xsl:template match="c02/did">

		<!--The next two variables define the set of container types that
		may appear in the first column of a two column container list.
		Add or subtract container types to fix institutional practice.-->
		<xsl:variable name="first" select="container[@type='Box' or @type='Oversize' or @type='Volume' or @type='Carton']"/>

		<!--This variable defines the set of container types that
		may appear in the second column of a two column container list.
		Add or subtract container types to fix institutional practice.-->
		<xsl:variable name="second" select="container[@type='Folder' or @type='Frame' or @type='Page'  or @type='Reel']"/>

		<xsl:variable name="preceding" select="preceding::did[1]/container[@type='Box' or @type='Oversize' or @type='Volume' or @type='Carton' or @type='Reel']"/>

			<xsl:choose>
			<!--When the container value or the container type of the first
			 container is not are the same as that of the comparable container
			in the previous component, insert column heads and the contents of
			the container elements.-->
			<xsl:when test="not($preceding=$first) or
			not($preceding/@type=$first/@type)">
				 <tr>
					<td valign="top">
						<xsl:apply-templates select="$first"/>
					</td>
					<td valign="top">
						<xsl:apply-templates select="$second"/>
					</td>
					<td valign="top" colspan="10">
						<xsl:call-template name="component-did"/>
					</td>
				</tr>
			</xsl:when>
			<xsl:otherwise>
				<tr>
					<td valign="top"> </td>
					<td valign="top">
						<xsl:value-of select="$second"/>
					</td>
					<td valign="top" colspan="10">
						<xsl:call-template name="component-did"/>
					</td>
				</tr>
			</xsl:otherwise>
		</xsl:choose>
			<xsl:for-each select="abstract | note/p | langmaterial | materialspec">
				<tr>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td colspan="8" valign="top">
						<xsl:apply-templates/>
					</td>
				</tr>
			</xsl:for-each>
	</xsl:template>

	<xsl:template match="c02/scopecontent | c02/bioghist | c02/arrangement |
			c02/userestrict | c02/accessrestrict | c02/processinfo |
			c02/acqinfo | c02/custodhist | c02/controlaccess/controlaccess |
			c02/odd | c02/note | c02/descgrp/*">
			<xsl:for-each select="head">
				<tr>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td colspan="8">
						<b>
							<xsl:apply-templates/>
						</b>
					</td>
				</tr>
			</xsl:for-each>
			<xsl:for-each select="*[not(self::head)]">
				<tr>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td colspan="8">
						<xsl:apply-templates/>
					</td>
				</tr>
			</xsl:for-each>
	</xsl:template>

	<!--This template processes c02 level components that do not have
	associated containers, for example if the c02 is a subseries.  The
	various subelements are all indented one column to the right of c01.-->
	<xsl:template name="c02-level-subseries">
		<xsl:for-each select="did">
			<tr>
				<td valign="top"></td>
				<td valign="top" colspan="11">
					<b>
						<xsl:call-template name="component-did"/>
					</b>

				</td>
			</tr>
			<xsl:for-each select="abstract | note/p | langmaterial | materialspec">
				<tr>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td colspan="8" valign="top">
						<xsl:apply-templates/>
					</td>
				</tr>
			</xsl:for-each>
		</xsl:for-each>
		<xsl:for-each select="scopecontent | bioghist | arrangement |
			descgrp/* | userestrict | accessrestrict | processinfo |
			acqinfo | custodhist | controlaccess/controlaccess | odd | note">
			<xsl:for-each select="*">
				<tr>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td colspan="8">
						<xsl:apply-templates/>
					</td>
				</tr>
			</xsl:for-each>
		</xsl:for-each>
	</xsl:template>

	<xsl:template match="c03">
		<xsl:apply-templates/>
	</xsl:template>
		
	<xsl:template match="c03/did">

		<!--The next two variables define the set of container types that
		may appear in the first column of a two column container list.
		Add or subtract container types to fix institutional practice.-->
		<xsl:variable name="first" select="container[@type='Box' or @type='Oversize' or @type='Volume' or @type='Carton']"/>

		<!--This variable defines the set of container types that
		may appear in the second column of a two column container list.
		Add or subtract container types to fix institutional practice.-->
		<xsl:variable name="second" select="container[@type='Folder' or @type='Frame' or @type='Page'  or @type='Reel']"/>

		<xsl:variable name="preceding" select="preceding::did[1]/container[@type='Box' or @type='Oversize' or @type='Volume' or @type='Carton' or @type='Reel']"/>

		<xsl:choose>
			<!--When the container value or the container type of the first
			 container is not are the same as that of the comparable container
			in the previous component, insert column heads and the contents of
			the container elements.-->
			<xsl:when test="not($preceding=$first) or
			not($preceding/@type=$first/@type)">
				 <tr>
					<td valign="top">
						<xsl:apply-templates select="$first"/>
					</td>
					<td valign="top">
						<xsl:apply-templates select="$second"/>
					</td>
					<td> </td>
					<td valign="top" colspan="9">
						<xsl:call-template name="component-did"/>
					</td>
				</tr>
			</xsl:when>
			<xsl:otherwise>
			<tr>
				<td valign="top"> </td>
				<td valign="top">
					<xsl:value-of select="$second"/>
				</td>
				<td> </td>
				<td valign="top" colspan="9">
					<xsl:call-template name="component-did"/>
				</td>
			</tr>
			</xsl:otherwise>
		</xsl:choose>
			<xsl:for-each select="abstract | note/p | langmaterial | materialspec">
				<tr>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td colspan="7" valign="top">
						<xsl:apply-templates/>
					</td>
				</tr>
			</xsl:for-each>
		</xsl:template>

		<xsl:template match="c03/scopecontent | c03/bioghist | c03/arrangement |
			c03/userestrict | c03/accessrestrict | c03/processinfo |
			c03/acqinfo | c03/custodhist | c03/controlaccess/controlaccess |
			c03/odd | c03/note | c03/descgrp/*">
			<xsl:for-each select="head">
				<tr>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td colspan="7">
						<b>
							<xsl:apply-templates/>
						</b>
					</td>
				</tr>
			</xsl:for-each>
			<xsl:for-each select="*[not(self::head)]">
				<tr>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td colspan="7">
						<xsl:apply-templates/>
					</td>
				</tr>
			</xsl:for-each>
	</xsl:template>

	<!--This template processes c04 level components.-->
	<xsl:template match="c04">
		<xsl:apply-templates/>
	</xsl:template>
		
	<xsl:template match="c04/did">
	
		<!--The next two variables define the set of container types that
		may appear in the first column of a two column container list.
		Add or subtract container types to fix institutional practice.-->
		<xsl:variable name="first" select="container[@type='Box' or @type='Oversize' or @type='Volume' or @type='Carton']"/>

		<!--This variable defines the set of container types that
		may appear in the second column of a two column container list.
		Add or subtract container types to fix institutional practice.-->
		<xsl:variable name="second" select="container[@type='Folder' or @type='Frame' or @type='Page'  or @type='Reel']"/>

		<xsl:variable name="preceding" select="preceding::did[1]/container[@type='Box' or @type='Oversize' or @type='Volume' or @type='Carton' or @type='Reel']"/>

		<xsl:choose>
			<!--When the container value or the container type of the first
			 container is not are the same as that of the comparable container
			in the previous component, insert column heads and the contents of
			the container elements.-->
			<xsl:when test="not($preceding=$first) or
			not($preceding/@type=$first/@type)">
				<tr>
					<td valign="top">
						<xsl:apply-templates select="$first"/>
					</td>
					<td valign="top">
						<xsl:apply-templates select="$second"/>
					</td>
					<td> </td>
					<td> </td>
					<td valign="top" colspan="8">
						<xsl:call-template name="component-did"/>
					</td>
				</tr>
			</xsl:when>
			<xsl:otherwise>
			<tr>
				<td valign="top"> </td>
				<td valign="top">
					<xsl:value-of select="$second"/>
				</td>
				<td> </td>
				<td> </td>
				<td valign="top" colspan="8">
					<xsl:call-template name="component-did"/>
				</td>
			</tr>
			</xsl:otherwise>
		</xsl:choose>
			<xsl:for-each select="abstract | note/p | langmaterial | materialspec">
				<tr>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td colspan="6" valign="top">
						<xsl:apply-templates/>
					</td>
				</tr>
			</xsl:for-each>
	</xsl:template>

	<xsl:template match="c04/scopecontent | c04/bioghist | c04/arrangement |
			c04/descgrp/* | c04/userestrict | c04/accessrestrict | c04/processinfo |
			c04/acqinfo | c04/custodhist | c04/controlaccess/controlaccess |
			c04/odd | c04/note">
			<xsl:for-each select="head">
				<tr>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td colspan="6">
						<b>
							<xsl:apply-templates/>
						</b>
					</td>
				</tr>
			</xsl:for-each>
			<xsl:for-each select="*[not(self::head)]">
				<tr>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td colspan="6">
						<xsl:apply-templates/>
					</td>
				</tr>
			</xsl:for-each>
	</xsl:template>

	<xsl:template match="c05">
		<xsl:apply-templates/>
	</xsl:template>
	
	<xsl:template match="c05/did">
		
		<!--The next two variables define the set of container types that
		may appear in the first column of a two column container list.
		Add or subtract container types to fix institutional practice.-->
		<xsl:variable name="first" select="container[@type='Box' or @type='Oversize' or @type='Volume' or @type='Carton']"/>

		<!--This variable defines the set of container types that
		may appear in the second column of a two column container list.
		Add or subtract container types to fix institutional practice.-->
		<xsl:variable name="second" select="container[@type='Folder' or @type='Frame' or @type='Page'  or @type='Reel']"/>

		<xsl:variable name="preceding" select="preceding::did[1]/container[@type='Box' or @type='Oversize' or @type='Volume' or @type='Carton' or @type='Reel']"/>

		<xsl:choose>
			<!--When the container value or the container type of the first
			 container is not are the same as that of the comparable container
			in the previous component, insert column heads and the contents of
			the container elements.-->
			<xsl:when test="not($preceding=$first) or
			not($preceding/@type=$first/@type)">
				 <tr>
					<td valign="top">
						<xsl:apply-templates select="$first"/>
					</td>
					<td valign="top">
						<xsl:apply-templates select="$second"/>
					</td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td valign="top" colspan="7">
						<xsl:call-template name="component-did"/>
					</td>
				</tr>
			</xsl:when>
			<xsl:otherwise>
				<tr>
					<td valign="top"> </td>
					<td valign="top">
						<xsl:value-of select="$second"/>
					</td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td valign="top" colspan="7">
						<xsl:call-template name="component-did"/>
					</td>
				</tr>
			</xsl:otherwise>
		</xsl:choose>
			<xsl:for-each select="abstract | note/p | langmaterial | materialspec">
				<tr>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td colspan="5" valign="top">
						<xsl:apply-templates/>
					</td>
				</tr>
			</xsl:for-each>
	</xsl:template>

	<xsl:template match="c05/scopecontent | c05/bioghist | c05/arrangement |
			c05/descgrp/* | c05/userestrict | c05/accessrestrict | c05/processinfo |
			c05/acqinfo | c05/custodhist | c05/controlaccess/controlaccess |
			c05/odd | c05/note">
			<xsl:for-each select="head">
				<tr>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td colspan="5">
						<b>
							<xsl:apply-templates/>
						</b>
					</td>
				</tr>
			</xsl:for-each>
			<xsl:for-each select="*[not(self::head)]">
				<tr>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td colspan="5">
						<xsl:apply-templates/>
					</td>
				</tr>
			</xsl:for-each>
	</xsl:template>

	<!--This template processes c06 components.-->
	<xsl:template match="c06">
		<xsl:apply-templates/>
	</xsl:template>
	
	<xsl:template match="c06/did">
		
		<!--The next two variables define the set of container types that
		may appear in the first column of a two column container list.
		Add or subtract container types to fix institutional practice.-->
		<xsl:variable name="first" select="container[@type='Box' or @type='Oversize' or @type='Volume' or @type='Carton']"/>

		<!--This variable defines the set of container types that
		may appear in the second column of a two column container list.
		Add or subtract container types to fix institutional practice.-->
		<xsl:variable name="second" select="container[@type='Folder' or @type='Frame' or @type='Page'  or @type='Reel']"/>

		<xsl:variable name="preceding" select="preceding::did[1]/container[@type='Box' or @type='Oversize' or @type='Volume' or @type='Carton' or @type='Reel']"/>

		<xsl:choose>
			<!--When the container value or the container type of the first
			 container is not are the same as that of the comparable container
			in the previous component, insert column heads and the contents of
			the container elements.-->
			<xsl:when test="not($preceding=$first) or
			not($preceding/@type=$first/@type)">
				<tr>
					<td valign="top">
						<xsl:apply-templates select="$first"/>
					</td>
					<td valign="top">
						<xsl:apply-templates select="$second"/>
					</td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td valign="top" colspan="6">
						<xsl:call-template name="component-did"/>
					</td>
				</tr>
			</xsl:when>
			<xsl:otherwise>
				<tr>
					<td valign="top"> </td>
					<td valign="top">
						<xsl:value-of select="$second"/>
					</td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td valign="top" colspan="6">
						<xsl:call-template name="component-did"/>
					</td>
				</tr>
			</xsl:otherwise>
		</xsl:choose>
			<xsl:for-each select="abstract | note/p | langmaterial | materialspec">
				<tr>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td colspan="4" valign="top">
						<xsl:apply-templates/>
					</td>
				</tr>
			</xsl:for-each>
		</xsl:template>

		<xsl:template match="c06/scopecontent | c06/bioghist | c06/arrangement |
			c06/userestrict | c06/accessrestrict | c06/processinfo |
			c06/acqinfo | c06/custodhist | c06/controlaccess/controlaccess |
			c06/odd | c06/note | c06/descgrp/*">
			<xsl:for-each select="head">
				<tr>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td colspan="4">
						<b>
							<xsl:apply-templates/>
						</b>
					</td>
				</tr>
			</xsl:for-each>
			<xsl:for-each select="*[not(self::head)]">
				<tr>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td colspan="4">
						<xsl:apply-templates/>
					</td>
				</tr>
			</xsl:for-each>
	</xsl:template>

	<xsl:template match="c07">
		<xsl:apply-templates/>
	</xsl:template>
	
	<xsl:template match="c07/did">
		
		<!--The next two variables define the set of container types that
		may appear in the first column of a two column container list.
		Add or subtract container types to fix institutional practice.-->
		<xsl:variable name="first" select="container[@type='Box' or @type='Oversize' or @type='Volume' or @type='Carton']"/>

		<!--This variable defines the set of container types that
		may appear in the second column of a two column container list.
		Add or subtract container types to fix institutional practice.-->
		<xsl:variable name="second" select="container[@type='Folder' or @type='Frame' or @type='Page'  or @type='Reel']"/>

		<xsl:variable name="preceding" select="preceding::did[1]/container[@type='Box' or @type='Oversize' or @type='Volume' or @type='Carton' or @type='Reel']"/>

		<xsl:choose>
			<!--When the container value or the container type of the first
			 container is not are the same as that of the comparable container
			in the previous component, insert column heads and the contents of
			the container elements.-->
			<xsl:when test="not($preceding=$first) or
			not($preceding/@type=$first/@type)">
				 <tr>
					<td valign="top">
						<xsl:apply-templates select="$first"/>
					</td>
					<td valign="top">
						<xsl:apply-templates select="$second"/>
					</td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td valign="top" colspan="5">
						<xsl:call-template name="component-did"/>
					</td>
				</tr>
			</xsl:when>
			<xsl:otherwise>
				<tr>
					<td valign="top"> </td>
					<td valign="top">
						<xsl:value-of select="$second"/>
					</td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td valign="top" colspan="5">
						<xsl:call-template name="component-did"/>
					</td>
				</tr>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:for-each select="abstract | note/p | langmaterial | materialspec">
				<tr>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td colspan="3" valign="top">
						<xsl:apply-templates/>
					</td>
				</tr>
			</xsl:for-each>
	</xsl:template>

	<xsl:template match="c07/scopecontent | c07/bioghist | c07/arrangement |
			c07/descgrp/* | c07/userestrict | c07/accessrestrict | c07/processinfo |
			c07/acqinfo | c07/custodhist | c07/controlaccess/controlaccess |
			c07/odd | c07/note">
			<xsl:for-each select="head">
				<tr>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td colspan="3">
						<b>
							<xsl:apply-templates/>
						</b>
					</td>
				</tr>
			</xsl:for-each>
			<xsl:for-each select="*[not(self::head)]">
				<tr>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td colspan="3">
						<xsl:apply-templates/>
					</td>
				</tr>
			</xsl:for-each>
	</xsl:template>

	<xsl:template match="c08">
		<xsl:apply-templates/>
	</xsl:template>
	
	<xsl:template match="c08/did">
		
		<!--The next two variables define the set of container types that
		may appear in the first column of a two column container list.
		Add or subtract container types to fix institutional practice.-->
		<xsl:variable name="first" select="container[@type='Box' or @type='Oversize' or @type='Volume' or @type='Carton']"/>

		<!--This variable defines the set of container types that
		may appear in the second column of a two column container list.
		Add or subtract container types to fix institutional practice.-->
		<xsl:variable name="second" select="container[@type='Folder' or @type='Frame' or @type='Page'  or @type='Reel']"/>

		<xsl:variable name="preceding" select="preceding::did[1]/container[@type='Box' or @type='Oversize' or @type='Volume' or @type='Carton' or @type='Reel']"/>

		<xsl:choose>
			<!--When the container value or the container type of the first
			 container is not are the same as that of the comparable container
			in the previous component, insert column heads and the contents of
			the container elements.-->
			<xsl:when test="not($preceding=$first) or
			not($preceding/@type=$first/@type)">
				<tr>
					<td valign="top">
						<xsl:value-of select="$first"/>
					</td>
					<td valign="top">
						<xsl:apply-templates select="$second"/>
					</td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td valign="top" colspan="4">
						<xsl:call-template name="component-did"/>
					</td>
				</tr>
			</xsl:when>
			<xsl:otherwise>
				<tr>
					<td valign="top"> </td>
					<td valign="top">
						<xsl:value-of select="$second"/>
					</td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td valign="top" colspan="4">
						<xsl:call-template name="component-did"/>
					</td>
				</tr>
			</xsl:otherwise>
		</xsl:choose>
			<xsl:for-each select="abstract | note/p | langmaterial | materialspec">
				<tr>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td colspan="2" valign="top">
						<xsl:apply-templates/>
					</td>
				</tr>
			</xsl:for-each>
		</xsl:template>

		<xsl:template match="c08/scopecontent | c08/bioghist | c08/arrangement |
			c08/descgrp/* | c08/userestrict | c08/accessrestrict | c08/processinfo |
			c08/acqinfo | c08/custodhist | c08/controlaccess/controlaccess |
			c08/odd | c08/note">
			<xsl:for-each select="head">
				<tr>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td colspan="2">
						<b>
							<xsl:apply-templates/>
						</b>
					</td>
				</tr>
			</xsl:for-each>
			<xsl:for-each select="*[not(self::head)]">
				<tr>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td> </td>
					<td colspan="2">
						<xsl:apply-templates/>
					</td>
				</tr>
			</xsl:for-each>
		</xsl:template>
</xsl:stylesheet>