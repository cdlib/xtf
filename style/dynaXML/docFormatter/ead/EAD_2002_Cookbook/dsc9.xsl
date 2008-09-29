<!--Revision date 25 December 2003-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<!-- This stylesheet formats the dsc portion of a finding aid.-->
	<!--It formats components that have a single container element
	of any type.-->
	<!--It assumes that c01 describes a file.-->
	<!--Column headings are inserted for the first component, for every
	seventh one, and whenever a container's type is not the same as that
	container of the preceding component.
	 The text of the column head is taken from the container's @type-->
	<!--The content of container elements is always displayed.-->

<!-- .................Section 1.................. -->

<!--This section of the stylesheet formats dsc, its head, and
any introductory paragraphs.  It creates a single HTML that contains
all of the dsc portion of the finding aid.-->

	<xsl:template match="archdesc/dsc">
			<xsl:apply-templates select="head"/>
			<xsl:apply-templates select="p | note/p"/>
			<table width="100%">
			<tr>
				<td width="16%"> </td>
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
				<td width="4%"> </td>
			</tr>
			<xsl:apply-templates select="*[not(self::head or self::p or self::note)]"/>
		</table>
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
<!--This section of the stylesheet contains a template
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
<!--This section of the stylesheet processes each c01 and then
recursively processes each child component of the
c01 by calling a named template specific to that component level.
The named templates are in section 4.-->

	<xsl:template match="c01">
		<xsl:call-template name="c01-level"/>	
	
			<xsl:for-each select="c02">
				<xsl:call-template name="c02-level"/>	
			
				<xsl:for-each select="c03">
					<xsl:call-template name="c03-level"/>	

					<xsl:for-each select="c04">
						<xsl:call-template name="c04-level"/>	

						<xsl:for-each select="c05">
							<xsl:call-template name="c05-level"/>	
							
							<xsl:for-each select="c06">
								<xsl:call-template name="c06-level"/>	
								
								<xsl:for-each select="c07">
									<xsl:call-template name="c07-level"/>	

									<xsl:for-each select="c08">
										<xsl:call-template name="c08-level"/>	
															
									</xsl:for-each><!--Closes c08-->
								</xsl:for-each><!--Closes c07-->
							</xsl:for-each><!--Closes c06-->
						</xsl:for-each><!--Closes c05-->
					</xsl:for-each><!--Closes c04-->
				</xsl:for-each><!--Closes c03-->
			</xsl:for-each><!--Closes c02-->
	</xsl:template>
	
	
<!-- ...............Section 4.............................. -->
<!--This section of the stylesheet contains a separate named template for
each component level.  The contents of each is identical except for the
spacing that is inserted to create the proper column display in HTML
for each level.-->
	
	<!--Processes c01 which is assumed to be a series
	description without associated components.-->
	<xsl:template name="c01-level">
		

		<xsl:for-each select="did">
		<xsl:variable name="sequence">
			<xsl:number  level="any" count="did" from="dsc"/>
		</xsl:variable>
			<!--The value of @type is inserted as a column heading
			for the first component, for every seventh one, and whenever
			a container's type is not the same as that of the container
			in the preceding component.-->
			<xsl:if test="$sequence=1 or $sequence mod 7 =0 or
			not(container/@type=preceding::did[1]/container/@type)">
				<tr>
					<td>
						<b>
							<xsl:value-of select="container/@type"/>
						</b>
					</td>
				</tr>
			</xsl:if>
			<tr>
				<td valign="top">
					<xsl:apply-templates select="container"/>
				</td>
				<td valign="top" colspan="11">
					<xsl:call-template name="component-did"/>
				</td>
			</tr>
			<xsl:for-each select="abstract | note/p | langmaterial | materialspec">
				<tr>
					<td> </td>
					<td> </td>
					<td> </td>
					<td colspan="9" valign="top">
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
			<!--The head element is displayed in bold.-->
			<xsl:for-each select="head">
				<tr>
					<td> </td>
					<td> </td>
					<td> </td>
					<td colspan="9">
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
					<td colspan="9">
						<xsl:apply-templates/>
					</td>
				</tr>
			</xsl:for-each>
		</xsl:for-each>
	</xsl:template>

	<!--This template processes c02 elements that have associated containers, for
	example when c02 is a file.-->
	<xsl:template name="c02-level">
		<xsl:for-each select="did">
		
		<xsl:variable name="sequence">
			<xsl:number  level="any" count="did" from="dsc"/>
		</xsl:variable>
		<!--The value of @type is inserted as a column heading
			for the first component, for every seventh one, and whenever
			a container's type is not the same as that of the container
			in the preceding component.-->
			<xsl:if test="$sequence=1 or $sequence mod 7 =0 or
			not(container/@type=preceding::did[1]/container/@type)">
			<tr>
				<td>
					<b>
						<xsl:value-of select="container/@type"/>
					</b>
				</td>
			</tr>
		</xsl:if>
			<tr>
				<td valign="top">
					<xsl:value-of select="container"/>
				</td>
				<td> </td>
				<td valign="top" colspan="10">
					<xsl:call-template name="component-did"/>
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
		</xsl:for-each><!--Closes the did.-->
		
		<xsl:for-each select="scopecontent | bioghist | arrangement |
			userestrict | accessrestrict | processinfo |
			acqinfo | custodhist | controlaccess/controlaccess | odd | note | descgrp/*">
			<!--The head element is displayed in bold.-->
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

	
	<xsl:template name="c03-level">
		<xsl:for-each select="did">
		<xsl:variable name="sequence">
			<xsl:number  level="any" count="did" from="dsc"/>
		</xsl:variable>
		
		<!--The value of @type is inserted as a column heading
			for the first component, for every seventh one, and whenever
			a container's type is not the same as that of the container
			in the preceding component.-->
			
			<xsl:if test="$sequence=1 or $sequence mod 7 =0 or
			not(container/@type=preceding::did[1]/container/@type)">
			<tr>
				<td>
					<b>
						<xsl:value-of select="container/@type"/>
					</b>
				</td>
			</tr>
		</xsl:if>
			<tr>
				<td valign="top">
					<xsl:value-of select="container"/>
				 </td>
				<td valign="top"> </td>
				<td> </td>
				<td valign="top" colspan="9">
					<xsl:call-template name="component-did"/>
				</td>
			</tr>
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
			<!--The head element is displayed in bold.-->
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
		<xsl:variable name="sequence">
			<xsl:number  level="any" count="did" from="dsc"/>
		</xsl:variable>
		
		<!--The value of @type is inserted as a column heading
			for the first component, for every seventh one, and whenever
			a container's type is not the same as that of the container
			in the preceding component.-->

			<xsl:if test="$sequence=1 or $sequence mod 7 =0 or
			not(container/@type=preceding::did[1]/container/@type)">
			<tr>
				<td>
					<b>
						<xsl:value-of select="container/@type"/>
					</b>
				</td>
			</tr>
		</xsl:if>
			<tr>
				<td valign="top">
					<xsl:apply-templates select="container"/>
				</td>
				<td> </td>
				<td> </td>
				<td> </td>
				<td valign="top" colspan="8">
					<xsl:call-template name="component-did"/>
				</td>
			</tr>
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
			<!--The head element is displayed in bold.-->
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
		<xsl:variable name="sequence">
			<xsl:number  level="any" count="did" from="dsc"/>
		</xsl:variable>
			
		<!--The value of @type is inserted as a column heading
			for the first component, for every seventh one, and whenever
			a container's type is not the same as that of the container
			in the preceding component.-->

			<xsl:if test="$sequence=1 or $sequence mod 7 =0 or
			not(container/@type=preceding::did[1]/container/@type)">
				<tr>
					<td>
						<b>
							<xsl:value-of select="container/@type"/>
						</b>
					</td>
				</tr>
			</xsl:if>
			<tr>
				<td valign="top">
					<xsl:value-of select="container"/>
				</td>
				<td> </td>
				<td> </td>
				<td> </td>
				<td> </td>
				<td valign="top" colspan="7">
					<xsl:call-template name="component-did"/>
				</td>
			</tr>
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
			<!--The head element is displayed in bold.-->
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
			<xsl:variable name="sequence">
				<xsl:number  level="any" count="did" from="dsc"/>
			</xsl:variable>
			
		<!--The value of @type is inserted as a column heading
			for the first component, for every seventh one, and whenever
			a container's type is not the same as that of the container
			in the preceding component.-->

			<xsl:if test="$sequence=1 or $sequence mod 7 =0 or
			not(container/@type=preceding::did[1]/container/@type)">
				<tr>
					<td>
						<b>
							<xsl:value-of select="container/@type"/>
						</b>
					</td>
				</tr>
			</xsl:if>
			<tr>
				<td valign="top">
					<xsl:value-of select="container"/>
				</td>
				<td> </td>
				<td> </td>
				<td> </td>
				<td> </td>
				<td> </td>
				<td valign="top" colspan="6">
					<xsl:call-template name="component-did"/>
				</td>
			</tr>
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
			<!--The head element is displayed in bold.-->
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
			<xsl:variable name="sequence">
				<xsl:number  level="any" count="did" from="dsc"/>
			</xsl:variable>
			
		<!--The value of @type is inserted as a column heading
			for the first component, for every seventh one, and whenever
			a container's type is not the same as that of the container
			in the preceding component.-->

			<xsl:if test="$sequence=1 or $sequence mod 7 =0 or
			not(container/@type=preceding::did[1]/container/@type)">
				<tr>
					<td>
						<b>
							<xsl:value-of select="container/@type"/>
						</b>
					</td>
				</tr>
			</xsl:if>
			<tr>
				<td valign="top">
					<xsl:value-of select="container"/>
				</td>
				<td> </td>
				<td> </td>
				<td> </td>
				<td> </td>
				<td> </td>
				<td> </td>
				<td valign="top" colspan="5">
					<xsl:call-template name="component-did"/>
				</td>
			</tr>
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
		</xsl:for-each> <!--Closes the did.-->

		<xsl:for-each select="scopecontent | bioghist | arrangement |
			descgrp/* | userestrict | accessrestrict | processinfo |
			acqinfo | custodhist | controlaccess/controlaccess | odd | note">
			<!--The head element is displayed in bold.-->
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
			<xsl:variable name="sequence">
				<xsl:number  level="any" count="did" from="dsc"/>
			</xsl:variable>
			
		<!--The value of @type is inserted as a column heading
			for the first component, for every seventh one, and whenever
			a container's type is not the same as that of the container
			in the preceding component.-->

			<xsl:if test="$sequence=1 or $sequence mod 7 =0 or
			not(container/@type=preceding::did[1]/container/@type)">
				<tr>
					<td>
						<b>
							<xsl:value-of select="container/@type"/>
						</b>
					</td>
				</tr>
			</xsl:if>
			<tr>
				<td valign="top">
					<xsl:value-of select="container"/>
				</td>
				<td> </td>
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
			<!--The head element is displayed in bold.-->
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
</xsl:stylesheet>