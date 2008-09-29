<!--Revision date 21 July 2004-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<!-- This stylesheet formats the dsc portion of a finding aid.-->
	<!--It formats components that have 2 container elements of any type.-->
	<!--It assumes that c01 and optionally <c02> is a high-level description
	such as a series, subseries, subgroup or subcollection and does not have container
	elements associated with it. However, it does accommodate situations
	where there a <c01> that is a file is occasionally interspersed. However,
	if <c01> is always a file, use dsc10.xsl instead. -->
	<!--Column headings for containers are displayed when either the value or
	the type of a component's first container differs from that of
	the comparable container in the preceding component. -->
	<!-- The content of column headings are taken from the type
    attribute of the container elements.-->
	<!--The content of any and all container elements is always displayed.-->

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
<!--This section of the stylesheet contains a named-template
that is used generically throughout the stylesheet.-->

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
			<!--If there should be miscellaneous c01 components that are
			actually file descriptions with associated container data,
			process them in the same way as a c02 is done.   This assumes
			that in these situations there is no c03.-->
			<xsl:choose>
				<xsl:when test="@level='file'">
					<xsl:call-template name="c02-level-container"/>	
				</xsl:when>
				<xsl:otherwise>
					<xsl:call-template name="c01-level"/>	
				</xsl:otherwise>
			</xsl:choose>
			

			<!--<xsl:apply-templates select="thead"/>-->
			<xsl:for-each select="c02">
				
				<xsl:choose>
					<xsl:when test="@level='subseries' or @level='series'">
						<xsl:call-template name="c02-level-subseries"/>	
					</xsl:when>
					<xsl:otherwise>
						<xsl:call-template name="c02-level-container"/>	
					</xsl:otherwise>	
				</xsl:choose>
				<!--<xsl:apply-templates select="thead"/>-->
				<xsl:for-each select="c03">
					<xsl:call-template name="c03-level"/>	

					<!--<xsl:apply-templates select="thead"/>-->
					<xsl:for-each select="c04">
						<xsl:call-template name="c04-level"/>	

						<!--<xsl:apply-templates select="thead"/>-->
						<xsl:for-each select="c05">
							<xsl:call-template name="c05-level"/>	

							<!--<xsl:apply-templates select="thead"/>-->
							<xsl:for-each select="c06">
								<xsl:call-template name="c06-level"/>	

								<!--<xsl:apply-templates select="thead"/>-->
								<xsl:for-each select="c07">
									<xsl:call-template name="c07-level"/>	

									<!--<xsl:apply-templates select="thead"/>-->
									<xsl:for-each select="c08">
										<xsl:call-template name="c08-level"/>	

										<!--<xsl:apply-templates select="thead"/>-->
										<xsl:for-each select="c09">
											<xsl:call-template name="c09-level"/>	

											<!--<xsl:apply-templates select="thead"/>-->
											<xsl:for-each select="c10">
												<xsl:call-template name="c10-level"/>	

												<!--<xsl:apply-templates select="thead"/>-->
											</xsl:for-each><!--Closes c10-->
										</xsl:for-each><!--Closes c09-->
									</xsl:for-each><!--Closes c08-->
								</xsl:for-each><!--Closes c07-->
							</xsl:for-each><!--Closes c06-->
						</xsl:for-each><!--Closes c05-->
					</xsl:for-each><!--Closes c04-->
				</xsl:for-each><!--Closes c03-->
			</xsl:for-each><!--Closes c02-->
		</table>
	</xsl:template>
	
<!-- ...............Section 4.............................. -->
<!--This section of the stylesheet contains a separte named template for
each component level.  The contents of each is identical except for the
spacing that is inserted to create the proper column display in HTML
for each level.-->

	<!--Processes c01 which is assumed to be a series
	description without associated components.-->
	<xsl:template name="c01-level">
		<xsl:for-each select="did">
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
		</xsl:for-each><!--Closes the did.-->

		<!--This template creates a separate row for each child of
		the listed elements.-->
		<xsl:for-each select="scopecontent | bioghist | arrangement
			| userestrict | accessrestrict | processinfo |
			acqinfo | custodhist | controlaccess/controlaccess | odd | note
			| descgrp/*">
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
		</xsl:for-each>
	</xsl:template>

	<!--This template processes c02 elements that have associated containers, for
	example when c02 is a file.-->
	<xsl:template name="c02-level-container">
		<xsl:for-each select="did">

		<!--The next two variables define the set of container types that
		may appear in the first column of a two column container list.
		Add or subtract container types to fix institutional practice.-->
		<xsl:variable name="first" select="container[@type='Box' or @type='Oversize' or @type='Volume' or @type='Carton']"/>
		<xsl:variable name="preceding" select="preceding::did[1]/container[@type='Box' or @type='Oversize' or @type='Volume' or @type='Carton' or @type='Reel']"/>

		<!--This variable defines the set of container types that
		may appear in the second column of a two column container list.
		Add or subtract container types to fix institutional practice.-->
		<xsl:variable name="second" select="container[@type='Folder' or @type='Frame' or @type='Page'  or @type='Reel']"/>

		<xsl:choose>
			<!--When the container value or the container type of the first
			 container is not are the same as that of the comparable container
			in the previous component, insert column heads and the contents of
			the container elements.-->
			<xsl:when test="not($preceding=$first) or
			not($preceding/@type=$first/@type)">
				<tr>
				<b>
					<td valign="top">
						<xsl:value-of select="$first/@type"/>
					</td>
					<td valign="top">
						<xsl:value-of select="$second/@type"/>
					</td>
				</b>	
				</tr>
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

	<xsl:template name="c03-level">
		<xsl:for-each select="did">

		<!--The next two variables define the set of container types that
		may appear in the first column of a two column container list.
		Add or subtract container types to fix institutional practice.-->
		<xsl:variable name="first" select="container[@type='Box' or @type='Volume' or @type='Carton' or @type='Reel']"/>
		<xsl:variable name="preceding" select="preceding::did[1]/container[@type='Box' or @type='Volume' or @type='Carton' or @type='Reel']"/>

		<!--This variable defines the set of container types that
		may appear in the second column of a two column container list.
		Add or subtract container types to fix institutional practice.-->
		<xsl:variable name="second" select="container[@type='Folder' or @type='Frame' or @type='Page']"/>

		<xsl:choose>
			<!--When the container value or the container type of the first
			 container is not are the same as that of the comparable container
			in the previous component, insert column heads and the contents of
			the container elements.-->
			<xsl:when test="not($preceding=$first) or
			not($preceding/@type=$first/@type)">
				<tr><b>
					<td valign="top">
						<xsl:value-of select="$first/@type"/>
					</td>
					<td valign="top">
						<xsl:value-of select="$second/@type"/>
					</td>
					</b>
				</tr>
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
		</xsl:for-each><!--Closes the did.-->

		<xsl:for-each select="scopecontent | bioghist | arrangement |
			userestrict | accessrestrict | processinfo |
			acqinfo | custodhist | controlaccess/controlaccess | odd | note |
			descgrp/*">
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
		</xsl:for-each>
	</xsl:template>

	<!--This template processes c04 level components.-->
	<xsl:template name="c04-level">
		<xsl:for-each select="did">

		<!--The next two variables define the set of container types that
		may appear in the first column of a two column container list.
		Add or subtract container types to fix institutional practice.-->
		<xsl:variable name="first" select="container[@type='Box' or @type='Volume' or @type='Carton' or @type='Reel']"/>
		<xsl:variable name="preceding" select="preceding::did[1]/container[@type='Box' or @type='Volume' or @type='Carton' or @type='Reel']"/>

		<!--This variable defines the set of container types that
		may appear in the second column of a two column container list.
		Add or subtract container types to fix institutional practice.-->
		<xsl:variable name="second" select="container[@type='Folder' or @type='Frame' or @type='Page']"/>

		<xsl:choose>
			<!--When the container value or the container type of the first
			 container is not are the same as that of the comparable container
			in the previous component, insert column heads and the contents of
			the container elements.-->
			<xsl:when test="not($preceding=$first) or
			not($preceding/@type=$first/@type)">
				<tr>
					<td valign="top">
						<xsl:value-of select="$first/@type"/>
					</td>
					<td valign="top">
						<xsl:value-of select="$second/@type"/>
					</td>
				</tr>
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
		</xsl:for-each><!--Closes the did-->

		<xsl:for-each select="scopecontent | bioghist | arrangement |
			descgrp/* | userestrict | accessrestrict | processinfo |
			acqinfo | custodhist | controlaccess/controlaccess | odd | note">
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
		</xsl:for-each>
	</xsl:template>

	<xsl:template name="c05-level">
		<xsl:for-each select="did">

		<!--The next two variables define the set of container types that
		may appear in the first column of a two column container list.
		Add or subtract container types to fix institutional practice.-->
		<xsl:variable name="first" select="container[@type='Box' or @type='Volume' or @type='Carton' or @type='Reel']"/>
		<xsl:variable name="preceding" select="preceding::did[1]/container[@type='Box' or @type='Volume' or @type='Carton' or @type='Reel']"/>

		<!--This variables defines the set of container types that
		may appear in the second column of a two column container list.
		Add or subtract container types to fix institutional practice.-->
		<xsl:variable name="second" select="container[@type='Folder' or @type='Frame' or @type='Page']"/>

		<xsl:choose>
			<!--When the container value or the container type of the first
			 container is not are the same as that of the comparable container
			in the previous component, insert column heads and the contents of
			the container elements.-->
			<xsl:when test="not($preceding=$first) or
			not($preceding/@type=$first/@type)">
				<tr>
					<td valign="top">
						<xsl:value-of select="$first/@type"/>
					</td>
					<td valign="top">
						<xsl:value-of select="$second/@type"/>
					</td>
				</tr>
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
		</xsl:for-each><!--Closes the did.-->

		<xsl:for-each select="scopecontent | bioghist | arrangement |
			descgrp/* | userestrict | accessrestrict | processinfo |
			acqinfo | custodhist | controlaccess/controlaccess | odd | note">
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
		</xsl:for-each>
	</xsl:template>

	<!--This template processes c06 components.-->
	<xsl:template name="c06-level">
		<xsl:for-each select="did">

		<!--The next two variables define the set of container types that
		may appear in the first column of a two column container list.
		Add or subtract container types to fix institutional practice.-->
		<xsl:variable name="first" select="container[@type='Box' or @type='Volume' or @type='Carton' or @type='Reel']"/>
		<xsl:variable name="preceding" select="preceding::did[1]/container[@type='Box' or @type='Volume' or @type='Carton' or @type='Reel']"/>

		<!--This variable defines the set of container types that
		may appear in the second column of a two column container list.
		Add or subtract container types to fix institutional practice.-->
		<xsl:variable name="second" select="container[@type='Folder' or @type='Frame' or @type='Page']"/>

		<xsl:choose>
			<!--When the container value or the container type of the first
			 container is not are the same as that of the comparable container
			in the previous component, insert column heads and the contents of
			the container elements.-->
			<xsl:when test="not($preceding=$first) or
			not($preceding/@type=$first/@type)">
				<tr>
					<td valign="top">
						<xsl:value-of select="$first/@type"/>
					</td>
					<td valign="top">
						<xsl:value-of select="$second/@type"/>
					</td>
				</tr>
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
		</xsl:for-each><!--Closes the did.-->

		<xsl:for-each select="scopecontent | bioghist | arrangement |
			userestrict | accessrestrict | processinfo |
			acqinfo | custodhist | controlaccess/controlaccess | odd | note |
			descgrp/*">
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
		</xsl:for-each>
	</xsl:template>

	<xsl:template name="c07-level">
		<xsl:for-each select="did">

		<!--The next two variables define the set of container types that
		may appear in the first column of a two column container list.
		Add or subtract container types to fix institutional practice.-->
		<xsl:variable name="first" select="container[@type='Box' or @type='Volume' or @type='Carton' or @type='Reel']"/>
		<xsl:variable name="preceding" select="preceding::did[1]/container[@type='Box' or @type='Volume' or @type='Carton' or @type='Reel']"/>

		<!--This variable defines the set of container types that
		may appear in the second column of a two column container list.
		Add or subtract container types to fix institutional practice.-->
		<xsl:variable name="second" select="container[@type='Folder' or @type='Frame' or @type='Page']"/>

		<xsl:choose>
			<!--When the container value or the container type of the first
			 container is not are the same as that of the comparable container
			in the previous component, insert column heads and the contents of
			the container elements.-->
			<xsl:when test="not($preceding=$first) or
			not($preceding/@type=$first/@type)">
				<tr>
					<td valign="top">
						<xsl:value-of select="$first/@type"/>
					</td>
					<td valign="top">
						<xsl:value-of select="$second/@type"/>
					</td>
				</tr>
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
			</xsl:otherwise>
		</xsl:choose>
		</xsl:for-each> <!--Closes the did.-->
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

		<xsl:for-each select="scopecontent | bioghist | arrangement |
			descgrp/* | userestrict | accessrestrict | processinfo |
			acqinfo | custodhist | controlaccess/controlaccess | odd | note">
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
		</xsl:for-each>
	</xsl:template>

	<xsl:template name="c08-level">
		<xsl:for-each select="did">

		<!--The next two variables define the set of container types that
		may appear in the first column of a two column container list.
		Add or subtract container types to fix institutional practice.-->
		<xsl:variable name="first" select="container[@type='Box' or @type='Volume' or @type='Carton' or @type='Reel']"/>
		<xsl:variable name="preceding" select="preceding::did[1]/container[@type='Box' or @type='Volume' or @type='Carton' or @type='Reel']"/>

		<!--This variable defines the set of container types that
		may appear in the second column of a two column container list.
		Add or subtract container types to fix institutional practice.-->
		<xsl:variable name="second" select="container[@type='Folder' or @type='Frame' or @type='Page']"/>

		<xsl:choose>
			<!--When the container value or the container type of the first
			 container is not are the same as that of the comparable container
			in the previous component, insert column heads and the contents of
			the container elements.-->
			<xsl:when test="not($preceding=$first) or
			not($preceding/@type=$first/@type)">
				<tr>
					<td valign="top">
						<xsl:value-of select="$first/@type"/>
					</td>
					<td valign="top">
						<xsl:value-of select="$second/@type"/>
					</td>
				</tr>
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
					<td> </td>
					<td valign="top" colspan="4">
						<xsl:call-template name="component-did"/>
					</td>
				</tr>
			</xsl:when>
			<xsl:otherwise>
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
		</xsl:for-each><!--Closes the did.-->

		<xsl:for-each select="scopecontent | bioghist | arrangement |
			descgrp/* | userestrict | accessrestrict | processinfo |
			acqinfo | custodhist | controlaccess/controlaccess | odd | note">
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
		</xsl:for-each>
	</xsl:template>

	<xsl:template name="c09-level">
		<xsl:for-each select="did">

		<!--The next two variables define the set of container types that
		may appear in the first column of a two column container list.
		Add or subtract container types to fix institutional practice.-->
		<xsl:variable name="first" select="container[@type='Box' or @type='Volume' or @type='Carton' or @type='Reel']"/>
		<xsl:variable name="preceding" select="preceding::did[1]/container[@type='Box' or @type='Volume' or @type='Carton' or @type='Reel']"/>

		<!--This variable defines the set of container types that
		may appear in the second column of a two column container list.
		Add or subtract container types to fix institutional practice.-->
		<xsl:variable name="second" select="container[@type='Folder' or @type='Frame' or @type='Page']"/>

		<xsl:choose>
			<!--When the container value or the container type of the first
			 container is not are the same as that of the comparable container
			in the previous component, insert column heads and the contents of
			the container elements.-->
			<xsl:when test="not($preceding=$first) or
			not($preceding/@type=$first/@type)">
				<tr>
					<td valign="top">
						<xsl:value-of select="$first/@type"/>
					</td>
					<td valign="top">
						<xsl:value-of select="$second/@type"/>
					</td>
				</tr>
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
					<td> </td>
					<td> </td>
					<td valign="top" colspan="3">
						<xsl:call-template name="component-did"/>
					</td>
				</tr>
			</xsl:when>
			<xsl:otherwise>
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
					<td> </td>
					<td> </td>
					<td valign="top" colspan="3">
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
					<td> </td>
					<td colspan="1" valign="top">
						<xsl:apply-templates/>
					</td>
				</tr>
			</xsl:for-each>
		</xsl:for-each><!--Closes the did.-->

		<xsl:for-each select="scopecontent | bioghist | arrangement |
			descgrp/* | userestrict | accessrestrict | processinfo |
			acqinfo | custodhist | controlaccess/controlaccess | odd | note">
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
					<td> </td>
					<td colspan="1" valign="top">
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
					<td> </td>
					<td colspan="1" valign="top">
						<xsl:apply-templates/>
					</td>
				</tr>
			</xsl:for-each>
		</xsl:for-each>
	</xsl:template>

	<xsl:template name="c10-level">
		<xsl:for-each select="did">

		<!--The next two variables define the set of container types that
		may appear in the first column of a two column container list.
		Add or subtract container types to fix institutional practice.-->
		<xsl:variable name="first" select="container[@type='Box' or @type='Volume' or @type='Carton' or @type='Reel']"/>
		<xsl:variable name="preceding" select="preceding::did[1]/container[@type='Box' or @type='Volume' or @type='Carton' or @type='Reel']"/>

		<!--This variable defines the set of container types that
		may appear in the second column of a two column container list.
		Add or subtract container types to fix institutional practice.-->
		<xsl:variable name="second" select="container[@type='Folder' or @type='Frame' or @type='Page']"/>

		<xsl:choose>
			<!--When the container value or the container type of the first
			 container is not are the same as that of the comparable container
			in the previous component, insert column heads and the contents of
			the container elements.-->
			<xsl:when test="not($preceding=$first) or
			not($preceding/@type=$first/@type)">
				<tr>
					<td valign="top">
						<xsl:value-of select="$first/@type"/>
					</td>
					<td valign="top">
						<xsl:value-of select="$second/@type"/>
					</td>
				</tr>
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
					<td> </td>
					<td> </td>
					<td> </td>
					<td valign="top" colspan="2">
						<xsl:call-template name="component-did"/>
					</td>
				</tr>
			</xsl:when>
			<xsl:otherwise>
				<tr>
					<td valign="top">
					<xsl:apply-templates select="$second"/>
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
					<td> </td>
					<td> </td>
					<td valign="top" colspan="2">
						<xsl:call-template name="component-did"/>
					</td>
				</tr>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:for-each>	<!--Closes the did.-->
	</xsl:template>

</xsl:stylesheet>