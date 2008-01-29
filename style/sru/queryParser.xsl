<!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
<!-- SRU query parser stylesheet                                            -->
<!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->

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
  This stylesheet implements a query parser that translates an XCQL query
  to XTF's internal query format.
  
  The input and output of this stylesheet are identical to crossQuery, except
  that instead of tokenizing the 'query' parameter, the sru servlet parses
  it as CQL and transforms it to XCQL.
-->

<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                              xmlns:dc="http://purl.org/dc/elements/1.1/" 
                              xmlns:mets="http://www.loc.gov/METS/"
                              xmlns:xlink="http://www.w3.org/TR/xlink" 
                              xmlns:xs="http://www.w3.org/2001/XMLSchema"
                              xmlns:parse="http://cdlib.org/parse"
                              xmlns:srw="http://www.loc.gov/zing/srw/" 
                              xmlns:diag="http://www.loc.gov/zing/srw/diagnostic/" 
                              xmlns:xcql="http://www.loc.gov/zing/srw/xcql/" 
                              xmlns:xsi="http://www.w3.org/2001/XMLSchema" 
                              xmlns:xsd="http://www.w3.org/2001/XMLSchema-instance"
                              exclude-result-prefixes="xsl dc mets xlink xs parse">
  
  <xsl:output method="xml" indent="yes" encoding="utf-8"/>
  
  <xsl:strip-space elements="*"/>
  
<!-- ====================================================================== -->
<!-- Global parameters (specified in the URL)                               -->
<!-- ====================================================================== -->

  <!-- servlet path from the URL -->
  <xsl:param name="servlet.path"/>
  <xsl:param name="root.path"/>
  
  <!-- operation (explain, searchRetrieve, etc.) -->
  <xsl:param name="operation" select="'searchRetrieve'"/>

  <!-- SRU version -->
  <xsl:param name="version" select="'1.1'"/>

  <!-- XCQL query -->
  <xsl:param name="query"/>
  
  <!-- first hit on page -->
  <xsl:param name="startRecord" select='1'/>
  
  <!-- documents per page -->
  <xsl:param name="maximumRecords" select="20"/>
  
  <!-- how to pack records -->
  <xsl:param name="recordPacking" select="'xml'"/>
   
  <!-- schema for records -->
  <xsl:param name="recordSchema" select="'dc'"/>
   
<!-- ====================================================================== -->
<!-- Root Template                                                          -->
<!-- ====================================================================== -->
  
  <xsl:template match="/">

    <!-- Check that we support the requested version -->
    <xsl:if test="not($version = '1.1')">
      <xsl:call-template name="error">
        <xsl:with-param name="uri" select="'info:srw/diagnostic/1/5'"/>
        <xsl:with-param name="message" select="concat('Unsupported version: ', $version)"/>
      </xsl:call-template>
    </xsl:if>
    
    <!-- Check that we support the requested record packing -->
    <xsl:if test="not($recordPacking = 'xml')">
      <xsl:call-template name="error">
        <xsl:with-param name="uri" select="'info:srw/diagnostic/1/71'"/>
        <xsl:with-param name="message" select="concat('Unsupported recordPacking: ', $recordPacking)"/>
      </xsl:call-template>
    </xsl:if>
    
    <!-- Perform the requested operation -->
    <xsl:choose>
      <xsl:when test="$operation = 'explain'">
        <xsl:call-template name="explain"/>
      </xsl:when>
      <xsl:when test="$operation = 'searchRetrieve'">
        <xsl:call-template name="searchRetrieve"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="error">
          <xsl:with-param name="uri" select="'info:srw/diagnostic/1/4'"/>
          <xsl:with-param name="message" select="concat('Unsupported operation: ', $operation)"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>

  </xsl:template>

<!-- ====================================================================== -->
<!-- Explain Operation Template                                             -->
<!-- ====================================================================== -->

  <xsl:template name="explain">
    <srw:explainResponse>
      <srw:version>1.1</srw:version> 
      <srw:record> 
        <srw:recordSchema>http://explain.z3950.org/dtd/2.0/</srw:recordSchema> 
        <srw:recordPacking>xml</srw:recordPacking> 
        <srw:recordData>
        
          <explain id="XTF SRU Test System" 
                   authoritative="true"  
                   xmlns="http://explain.z3950.org/dtd/2.0/">
                   
            <serverInfo protocol="sru" version="1.0">
              <xsl:analyze-string select="$servlet.path"
                                  regex="^http://([^:/]+)(:(\d+))?(.+)">
                <xsl:matching-substring>
                  <host><xsl:value-of select="regex-group(1)"/></host> 
                  <port><xsl:value-of select="regex-group(3)"/></port>
                  <database><xsl:value-of select="regex-group(4)"/></database>
                </xsl:matching-substring>
                <xsl:non-matching-substring>
                  <xsl:call-template name="error">
                    <xsl:with-param name="uri" select="'info:srw/diagnostic/1/1'"/>
                    <xsl:with-param name="message" select="'Unable to parse $servlet.path'"/>
                  </xsl:call-template>
                </xsl:non-matching-substring>
              </xsl:analyze-string>
            </serverInfo>
        
            <databaseInfo> 
              <title lang="en" primary="true">Test XTF Database</title> 
              <description lang="en" primary="true">Test database for SRU interface to an XTF index</description> 
            </databaseInfo>
        
            <indexInfo>
              <indexSet identifier="http://www.loc.gov/zing/cql/dc-indexes/v1.0/" name="dc" />  
              <index> 
                <title>Title</title> 
                <map><name indexSet="dc">title</name></map>
              </index> 
              <index> 
                <title>Creator</title>  
                <map><name indexSet="dc">creator</name></map>            
              </index> 
              <index> 
                <title>Subject</title> 
                <map><name indexSet="dc">subject</name></map> 
              </index> 
              <index> 
                <title>Description</title> 
                <map><name indexSet="dc">description</name></map> 
              </index> 
              <index> 
                <title>Publisher</title> 
                <map><name indexSet="dc">publisher</name></map> 
              </index> 
              <index> 
                <title>Contributor</title> 
                <map><name indexSet="dc">publisher</name></map> 
              </index> 
              <index> 
                <title>Year of publication</title> 
                <map><name indexSet="dc">date</name></map> 
              </index> 
              <index> 
                <title>Type</title> 
                <map><name indexSet="dc">type</name></map> 
              </index> 
              <index> 
                <title>Format</title> 
                <map><name indexSet="dc">format</name></map> 
              </index> 
              <index> 
                <title>Identifier</title> 
                <map><name indexSet="dc">identifier</name></map> 
              </index> 
              <index> 
                <title>Source</title> 
                <map><name indexSet="dc">source</name></map> 
              </index> 
              <index> 
                <title>Language</title> 
                <map><name indexSet="dc">language</name></map> 
              </index> 
              <index> 
                <title>Relation</title> 
                <map><name indexSet="dc">relation</name></map> 
              </index> 
              <index> 
                <title>Coverage</title> 
                <map><name indexSet="dc">coverage</name></map> 
              </index> 
              <index> 
                <title>Rights</title> 
                <map><name indexSet="dc">rights</name></map> 
              </index> 
            </indexInfo> 
        
            <schemaInfo> 
              <schema identifier="info:srw/schema/1/dc-v1.1"
                   sort="true" retrieve="true" name="dc">
              <title>Dublin Core</title>
              </schema>
            </schemaInfo> 
        
            <configInfo> 
              <default type="indexSet">dc</default> 
              <default type="index">any</default> 
              <default type="recordPacking">xml</default> 
              <default type="relation">=</default> 
              <default type="retrieveSchema">dc</default> 
              <supports type="relation">all</supports>
              <supports type="proximity"></supports>
           </configInfo>
         </explain>
       </srw:recordData>
     </srw:record>
     
     <srw:echoedExplainRequest>
       <srw:version><xsl:value-of select="$version"/></srw:version>
       <srw:recordPacking><xsl:value-of select="$recordPacking"/></srw:recordPacking>
     </srw:echoedExplainRequest>
   </srw:explainResponse>

  </xsl:template>

<!-- ====================================================================== -->
<!-- Search/Retrieve Operation Template                                     -->
<!-- ====================================================================== -->
  
  <xsl:template name="searchRetrieve">
  
    <xsl:variable name="stylesheet" select="'style/sru/resultFormatter/resultFormatter.xsl'"/>
    
    <!-- The top-level query element tells what stylesheet will be used to
       format the results, which document to start on, and how many documents
       to display on this page. -->
    <query indexPath="index" termLimit="1000" workLimit="1000000" style="{$stylesheet}" startDoc="{$startRecord}" maxDocs="{$maximumRecords}">

      <!-- process query -->
      <and>
        <xsl:apply-templates select="parameters/param[@name='query']"/>
      </and>
      
    </query>
    
  </xsl:template>
  
<!-- ====================================================================== -->
<!-- "triple" Template (specifies boolean operators)                        -->
<!-- ====================================================================== -->
  
  <xsl:template match="triple">
    <xsl:variable name="operator" select="boolean"/>
    <xsl:choose>
      <xsl:when test="$operator = 'and'">
        <and>
          <xsl:apply-templates select="leftOperand"/>
          <xsl:apply-templates select="rightOperand"/>
        </and>
      </xsl:when>
      <xsl:when test="$operator = 'prox'">
        <near>
          <xsl:apply-templates select="leftOperand"/>
          <xsl:apply-templates select="rightOperand"/>
        </near>
      </xsl:when>
      <xsl:when test="$operator = 'or'">
        <or>
          <xsl:apply-templates select="leftOperand"/>
          <xsl:apply-templates select="rightOperand"/>
        </or>
      </xsl:when>
      <xsl:when test="$operator = 'not'">
        <and>
          <xsl:apply-templates select="leftOperand"/>
          <not>
            <xsl:apply-templates select="rightOperand"/>
          </not>
        </and>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="error">
          <xsl:with-param name="uri" select="'info:srw/diagnostic/1/37'"/>
          <xsl:with-param name="message" select="concat('Unsupported boolean operator: ', $operator)"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
<!-- ====================================================================== -->
<!-- searchClause template                                                  -->
<!-- ====================================================================== -->
  
  <xsl:template match="searchClause">
  
    <xsl:variable name="index" select="string(index)"/>
    <xsl:variable name="relation" select="string(relation/value)"/>
    
    <xsl:variable name="field">
      <xsl:choose>
        <xsl:when test="$index = 'dc.title'   or $index = 'title'">title</xsl:when>
        <xsl:when test="$index = 'dc.creator' or $index = 'creator'">creator</xsl:when>
        <xsl:when test="$index = 'dc.subject' or $index = 'subject'">subject</xsl:when>
        <xsl:when test="$index = 'dc.description' or $index = 'description'">description</xsl:when>
        <xsl:when test="$index = 'dc.publisher' or $index = 'publisher'">publisher</xsl:when>
        <xsl:when test="$index = 'dc.contributor' or $index = 'contributor'">contributor</xsl:when>
        <xsl:when test="$index = 'dc.date' or $index = 'date'">year</xsl:when>
        <xsl:when test="$index = 'dc.type' or $index = 'type'">type</xsl:when>
        <xsl:when test="$index = 'dc.format' or $index = 'format'">format</xsl:when>
        <xsl:when test="$index = 'dc.identifier' or $index = 'identifier'">identifier</xsl:when>
        <xsl:when test="$index = 'dc.source' or $index = 'source'">source</xsl:when>
        <xsl:when test="$index = 'dc.language' or $index = 'language'">language</xsl:when>
        <xsl:when test="$index = 'dc.relation' or $index = 'relation'">relation</xsl:when>
        <xsl:when test="$index = 'dc.coverage' or $index = 'coverage'">subject</xsl:when>
        <xsl:when test="$index = 'dc.rights' or $index = 'rights'">rights</xsl:when>
        <xsl:when test="$index = 'text'">text</xsl:when>
        <xsl:when test="$index = 'srw.serverChoice'">title</xsl:when>
        <xsl:otherwise>unsupported</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
        
    <xsl:if test="$field = 'unsupported'">          
      <xsl:call-template name="error">
        <xsl:with-param name="uri" select="'info:srw/diagnostic/1/16'"/>
        <xsl:with-param name="message" select="concat('Unsupported index: ', $index)"/>
      </xsl:call-template>
    </xsl:if>
    
    <xsl:choose>
      <xsl:when test="$relation = '=' or $relation = 'all' or $relation = 'scr'">
        <and field="{$field}">
          <xsl:apply-templates select="term"/>
        </and>
      </xsl:when>
      <xsl:when test="$relation = 'exact'">
        <phrase field="{$field}">
          <xsl:apply-templates select="term"/>
        </phrase>
      </xsl:when>
      <xsl:when test="$relation = 'any'">
        <or field="{$field}">
          <xsl:apply-templates select="term"/>
        </or>
      </xsl:when>
      <xsl:when test="$relation = '>'">
        <range inclusive="no" field="{$field}">
          <lower>
            <xsl:apply-templates select="term"/>
          </lower>
        </range>
      </xsl:when>
      <xsl:when test="$relation = '>='">
        <range inclusive="yes" field="{$field}">
          <lower>
            <xsl:apply-templates select="term"/>
          </lower>
        </range>
      </xsl:when>
      <xsl:when test="$relation = '&lt;'">
        <range inclusive="no" field="{$field}">
          <upper>
            <xsl:apply-templates select="term"/>
          </upper>
        </range>
      </xsl:when>
      <xsl:when test="$relation = '&lt;='">
        <range inclusive="yes" field="{$field}">
          <upper>
            <xsl:apply-templates select="term"/>
          </upper>
        </range>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="error">
          <xsl:with-param name="uri" select="'info:srw/diagnostic/1/19'"/>
          <xsl:with-param name="message" select="concat('Unsupported relation: ', $relation)"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
<!-- ====================================================================== -->
<!-- Term Template                                                          -->
<!-- ====================================================================== -->

  <xsl:template match="term">
  
    <!-- The "term" element in XCQL may actually contain multiple terms.
         We have to break them up into individual XTF <term> elements.
    -->
    <xsl:analyze-string select="string(.)" regex="\s+">
      <xsl:non-matching-substring>
        <term>
          <xsl:value-of select="."/>
        </term>
      </xsl:non-matching-substring>
    </xsl:analyze-string>
  </xsl:template>
  
  
<!-- ====================================================================== -->
<!-- Error Template                                                         -->
<!-- ====================================================================== -->

  <xsl:template name="error">
    <xsl:param name="uri"/>
    <xsl:param name="message"/>
    <xsl:param name="details" select="''"/>
    
    <srw:diagnostics>
      <diag:diagnostic>
        <diag:uri><xsl:value-of select="$uri"/></diag:uri>
        <xsl:if test="not($details = '')">
          <diag:details><xsl:value-of select="$details"/></diag:details>
        </xsl:if>
        <diag:message><xsl:value-of select="$message"/></diag:message>
      </diag:diagnostic>
    </srw:diagnostics>

  </xsl:template>
  
</xsl:stylesheet>
