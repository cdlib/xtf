<!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
<!-- Complex query parser stylesheet                                        -->
<!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->

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

<xsl:stylesheet version="2.0"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:dc="http://purl.org/dc/elements/1.1/"
        xmlns:mets="http://www.loc.gov/METS/"
        xmlns:xlink="http://www.w3.org/TR/xlink"
        xmlns:xs="http://www.w3.org/2001/XMLSchema"
        xmlns:parse="http://cdlib.org/parse"
        exclude-result-prefixes="xsl dc mets xlink xs parse">

<xsl:output method="xml"
      indent="yes"
      encoding="utf-8"/>

<!-- ====================================================================== -->
<!-- DESCRIPTION AND WARNING                                                -->
<!-- ====================================================================== -->

<!--

    This stylesheet parses complex boolean queries. Here are some examples
    of the sort of query it handles:

    	man war not "man of war"
		(hat or cat) and (bat or rat)
		"cat hat"~5
		1872..1885

	The code here should be considered experimental, as there are likely
    some odd cases where it does not function correctly.

    Consider using the simple query parser instead, 'queryParser.xsl', with
    a series of form values to give the user a better experience.

-->
      
<!-- ====================================================================== -->
<!-- Global parameters (specified in the URL)                               -->
<!-- ====================================================================== -->

  <!-- search mode -->
  <xsl:param name="smode"/>

  <!-- result mode -->
  <xsl:param name="rmode"/>
  
  <xsl:param name="startDoc" select="1"/>
  
  <xsl:param name="docsPerPage">
    <xsl:choose>
      <xsl:when test="($rmode != '') or ($smode = 'test') or ($smode='debug')">
        <xsl:value-of select="10000"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="10"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:param>

<!-- ====================================================================== -->
<!-- Root Template                                                          -->
<!-- ====================================================================== -->

<xsl:template match="/">
  
  <!-- It's useful when testing this stylesheet to have a test input document
     instead of having to run the servlet every time. -->
     
  <!--
    <xsl:variable name="testDoc">
      <param name="text">
        <phrase>
          <token value="woozy"/>
          <token value="man"/>
        </phrase>
        <token value="~"/>
        <token value="5"/>
      </param>
    </xsl:variable>

    <xsl:variable name="result" 
                  select="parse:processParams($testDoc, 'crossQuery')"/>
  -->

  <!-- processParams() does most of the work. -->
  <xsl:variable name="result" 
                select="parse:processParams(parameters, 'crossQuery')"/>

  <!-- If an error was found at any stage, return only that. Otherwise, make
     a 'query' element out of the resulting compound query. -->
  <xsl:choose>
    <xsl:when test="$result//error">
      <xsl:copy-of select="$result//error"/>
    </xsl:when>
    <xsl:otherwise>
      <query style="style/crossQuery/resultFormatter.xsl" 
           startDoc="{$startDoc}" 
           maxDocs="{$docsPerPage}">
        <xsl:copy-of select="$result"/>
      </query>
    </xsl:otherwise>
  </xsl:choose>

</xsl:template>

<!-- ======================================================================
   Function:    parse:processParams(params)
   Description: Forms a query combining a text query and one or more 
                meta-data queries.
   ====================================================================== -->

<xsl:function name="parse:processParams">
  
  <xsl:param name="params"/>
  <xsl:param name="mode"/>

  <combine indexPath="index" termLimit="1000" workLimit="500000" >
    
    <!-- First, if there is a text query, process it. -->
    <xsl:variable name="textParam" select="$params/param[@name='text' and count(*) > 0]"/>
    <xsl:if test="$textParam">
      <xsl:variable name="query" select="parse:query('text', $textParam/*)"/>
      
      <!-- Enforce the rule that range queries aren't allowed on full text -->
      <xsl:choose>
        <xsl:when test="$query//range or $query[name() = 'range']">
          <error message="Sorry, range queries (using '&lt;', '&lt;=', '>', '>=', or '..') 
                          are only allowed on meta-fields, not full text."/>
        </xsl:when>
        <xsl:when test="$mode = 'dynaXML'">
          <text maxSnippets="1000" contextChars="80">
            <xsl:copy-of select="$query"/>
          </text>
        </xsl:when>
        <xsl:otherwise>
          <text maxSnippets="3" contextChars="80">
            <xsl:copy-of select="$query"/>
          </text>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:if>
    
    <!-- Now do the meta-queries if any -->
    <xsl:variable name="metaParams" select="$params/param[parse:isMeta(@name) and count(*) > 0]"/>
    <xsl:if test="$metaParams">
      <meta>
        <and>
          <xsl:for-each select="$metaParams">
            <xsl:copy-of select="parse:query(@name, *)"/> 
          </xsl:for-each>
        </and>
      </meta>
    </xsl:if>
    
    <!-- But what if nothing is specified? -->
    <xsl:if test="count($textParam)+count($metaParams) = 0">
      <meta>
        <term field="title">!@#!$!#@$</term>
      </meta>
    </xsl:if>
  
  </combine>
  
</xsl:function>

<!-- ======================================================================
  Function:    parse:isMeta(field)
  Description: Tells whether the given field name is a valid meta-data
               field.
  ====================================================================== -->

<xsl:function name="parse:isMeta">
  <xsl:param name="field"/>
  <xsl:choose>
    <xsl:when test="$field = 'text'"></xsl:when>
    <xsl:when test="$field = 'startDoc'"></xsl:when>
    <xsl:when test="$field = 'docsPerPage'"></xsl:when>
    <!-- Ignore result mode flag -->
    <xsl:when test="$field = 'rmode'"></xsl:when>
    <xsl:otherwise>yes</xsl:otherwise>
  </xsl:choose>
</xsl:function>

<!-- ======================================================================
   Function:    parse:query(field, in)
   Description: Parses a basic query for the given field.
   Parameters:  field - either 'text' or the name of a meta-data field
                in - one or more phrase and/or token elements
   ====================================================================== -->

<xsl:function name="parse:query">
  <xsl:param name="field"/>
  <xsl:param name="in"/>
  
  <!-- Parse it as an expression -->
  <xsl:variable name="result" select="parse:expr($in)"/>
  
  <xsl:choose>
    
    <!-- If there was an error, propagate it up -->
    <xsl:when test="$result//error">
      <xsl:copy-of select="$result//error"/>
    </xsl:when>
    
    <!-- If one or more tokens weren't recognized, flag that as an error -->
    <xsl:when test="$result/rest/*[1]">
      <error>
        <xsl:attribute name="message">
          <xsl:value-of select="concat('Syntax error near &quot;', $result/rest/*[1]/@value, '&quot;.')"/>
        </xsl:attribute>
      </error>
    </xsl:when>
    
    <!-- There should only be one value back... a single query -->
    <xsl:when test="count($result/value/*) != 1">
      <error message="Internal queryGen error: got multiple results"/>
    </xsl:when>
    
    <!-- Add on the field name -->
    <xsl:otherwise>
      <xsl:element name="{name($result/value/*[1])}">
        <xsl:if test="$field != 'text'">
          <xsl:attribute name="field">
            <xsl:value-of select="$field"/>
          </xsl:attribute>
        </xsl:if>
        <xsl:copy-of select="$result/value/*[1]/*|@*|child::text()"/>
      </xsl:element>
    </xsl:otherwise>
  </xsl:choose>
</xsl:function>

<!-- ======================================================================
   Function:    parse:expr(in)
   Description: Parses an expression. Simply starts at the top to see if
                it is an 'or' query.
   Parameters:  in - one or more phrase and/or token elements
   ====================================================================== -->

<xsl:function name="parse:expr">
  <xsl:param name="in"/>
  <xsl:copy-of select="parse:orExpr($in)"/>
</xsl:function>

<!-- ======================================================================
   Function:    parse:orExpr(in)
   Description: Parses either a single 'and' expression, or multiple 'and'
                expressions joined by 'or'.
   Parameters:  in - one or more phrase and/or token elements
   ====================================================================== -->

<xsl:function name="parse:orExpr">
  <xsl:param name="in"/>
  
  <!-- Parse the first part as an 'and' expression. The effect is that 'or'
     has precedence over 'and'. -->
  <xsl:variable name="op1" select="parse:andExpr($in)"/>
  
  <!-- If the next word is 'or', then we have two and expressions to join
     together. Otherwise, it's just a single 'and' expression. -->
  <xsl:choose>
    <xsl:when test="$op1/rest/token[1]/@value = 'or'">
      <xsl:variable name="op2" select="parse:orExpr($op1/rest/*[position() &gt; 1])"/>
      <xsl:copy-of select="parse:checkOp('or', $op2, parse:join('or', $op1, $op2))"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:copy-of select="$op1"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:function>

<!-- ======================================================================
   Function:    parse:checkOp(name, op, result)
   Description: Used by parse:orExpr and parse:andExpr to make sure that
                something follows the 'or' or 'and' token.
   Parameters:  name - the operator being parsed ('or' or 'and')
                op - the remaining tokens to parse
                result - what to return if okay
   ====================================================================== -->

<xsl:function name="parse:checkOp">
  <xsl:param name="name"/>
  <xsl:param name="op"/>
  <xsl:param name="result"/>
  
  <xsl:choose>
    <xsl:when test="count($op//term) = 0">
      <xsl:copy-of select="parse:makeError(
        concat('Missing word after &quot;', $name, '&quot;.'), '')"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:copy-of select="$result"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:function>

<!-- ======================================================================
   Function:    parse:andExpr(in)
   Description: Parses either a single term expression, or multiple term
                expressions joined by 'and'.
   Parameters:  in - one or more phrase and/or token elements
   ====================================================================== -->

<xsl:function name="parse:andExpr">
  <xsl:param name="in"/>
  
  <!-- Parse the first part as a term expression -->
  <xsl:variable name="op1" select="parse:terms($in)"/>
  
  <!-- If the next token is 'and', then we have multiple terms to join 
     together; otherwise, just return the one we got. -->
  <xsl:choose>
    <xsl:when test="$op1/rest/token[1]/@value = 'and'">
      <xsl:variable name="op2" select="parse:andExpr($op1/rest/*[position() &gt; 1])"/>
      <xsl:copy-of select="parse:checkOp('and', $op2, parse:join('and', $op1, $op2))"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:copy-of select="$op1"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:function>

<!-- ======================================================================
   Function:    parse:join(opName, op1, op2)
   Description: Used by parse:orExpr and parse:andExpr to join two expressions
                together with an operator (either 'or' or 'and'). If the
                output would look like this:
          
          <or>
             term1
             <or>
              term2
              term3
             <or>
          </or>
          
          we produce this instead:
          
          <or>
            term1
            term2
            term3
          </or>
          
   Parameters:  opName - the operator to join the two expressions
                op1 - the first expression
                op2 - ... and the second.
   ====================================================================== -->

<xsl:function name="parse:join">
  <xsl:param name="opName"/>
  <xsl:param name="op1"/>
  <xsl:param name="op2"/>
  
  <!-- The result consists of the value we've built up so far, then the
     remaining unparsed tokens -->
  <result>
  
    <!-- Value so far -->
    <value>
      <xsl:element name="{$opName}">
        <xsl:copy-of select="$op1/value/*"/>

        <!-- Handle the special case outlined above -->
        <xsl:choose>
          <xsl:when test="count($op2/value/*) = 1 and
                  name($op2/value/*[1]) = $opName">
            <xsl:copy-of select="$op2/value/*[1]/*"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:copy-of select="$op2/value/*"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:element>
    </value>
    
    <!-- Remaining unparsed tokens -->
    <rest>
      <xsl:copy-of select="$op2/rest/*"/>
    </rest>
  </result>
</xsl:function>

<!-- ======================================================================
   Function:    parse:terms(in)
   Description: Parses one or more term expressions. If more than one, they
                are joined with the default operator.
   Parameters:  in - one or more phrase and/or token elements
   ====================================================================== -->

<xsl:function name="parse:terms">
  <xsl:param name="in"/>

  <!-- Parse the first term, then the remaining ones recursively -->
  <xsl:variable name="term" select="parse:term($in)"/>
  <xsl:variable name="restTerms" select="parse:terms($term/rest/*)"/>

  <xsl:choose>

    <!-- If there was nothing to parse, punt -->    
    <xsl:when test="count($in) = 0">
      <xsl:copy-of select="$term"/>
    </xsl:when>
    
    <!-- If no value was produced, punt -->      
    <xsl:when test="count($term/value/*) = 0">
      <xsl:copy-of select="$term"/>
    </xsl:when>

    <!-- If we only found one, then return just that one -->
    <xsl:when test="count($restTerms/value/*) = 0">
      <xsl:copy-of select="$term"/>
    </xsl:when>

    <!-- Okay, we have multiple terms. Join them together -->
    <xsl:otherwise>
      <result>
        <value>
          <xsl:element name="{$defaultOp}">
            <xsl:copy-of select="$term/value/*"/>
            <xsl:copy-of select="$restTerms/value/*[name()!=$defaultOp]"/>
            <xsl:copy-of select="$restTerms/value/*[name()=$defaultOp]/*"/>
          </xsl:element>
        </value>
        <rest>
          <xsl:copy-of select="$restTerms/rest/*"/>
        </rest>
      </result>
    </xsl:otherwise>
    
  </xsl:choose>
  
</xsl:function>

<!-- ======================================================================
   Function:    parse:term(in)
   Description: Parses a single term or phrase. Yes, the name of the function
                could be parse:termOrPhrase, but it would just be too wordy.
   Parameters:  in - one or more phrase and/or token elements
   ====================================================================== -->

<xsl:function name="parse:term">
  <xsl:param name="in"/>
  
  <!-- Divide the work up: the first, and all the rest -->
  <xsl:variable name="word" select="$in[1]/@value"/>
  <xsl:variable name="rest" select="$in[position() &gt; 1]"/>
  
  <xsl:choose>
  
    <!-- If there's nothing left to parse, return a null value -->
    <xsl:when test="count($in) = 0">
      <result>
        <value/>
        <rest/>
      </result>
    </xsl:when>
    
    <!-- If we're looking at a phrase, parse it separately -->
    <xsl:when test="name($in[1]) = 'phrase'">
      <xsl:copy-of select="parse:phrase($in)"/>
    </xsl:when>
    
    <!-- If the input contains the special marker 'error', propagate the
       error upwards. -->
    <xsl:when test="name($in[1]) = 'error'">
      <result>
        <value/>
        <rest>
          <xsl:copy-of select="$in"/>
        </rest>
      </result>
    </xsl:when>
    
    <!-- Handle parenthesized sub-expressions -->
    <xsl:when test="$word = '('">
      <xsl:copy-of select="parse:subExpr($rest)"/>
    </xsl:when>
    
    <!-- Handle 'not' clauses -->
    <xsl:when test="$word = 'not'">
      <xsl:copy-of select="parse:not($rest)"/>
    </xsl:when>
    
    <!-- Handle range queries -->
    <xsl:when test="$word = '>'">
      <xsl:copy-of select="parse:range($word, $rest[1], $in/null, $rest[position() > 1], 'no')"/>
    </xsl:when>
    <xsl:when test="$word = '>='">
      <xsl:copy-of select="parse:range($word, $rest[1], $in/null, $rest[position() > 1], 'yes')"/>
    </xsl:when>
    <xsl:when test="$word = '&lt;'">
      <xsl:copy-of select="parse:range($word, $in/null, $rest[1], $rest[position() > 1], 'no')"/>
    </xsl:when>
    <xsl:when test="$word = '&lt;='">
      <xsl:copy-of select="parse:range($word, $in/null, $rest[1], $rest[position() > 1], 'yes')"/>
    </xsl:when>
    <xsl:when test="$rest[1][name()='token']/@value = '..'">
      <xsl:copy-of select="parse:range('..', $in[1], $rest[2], $rest[position() > 2], 'yes')"/>
    </xsl:when>
    
    <!-- If the word is reserved, it must be part of a higher-level
       expression. Stop processing here and allow the higher level to
       resume. -->
    <xsl:when test="parse:isReserved($word)">
      <result>
        <value/>
        <rest>
          <xsl:copy-of select="$in"/>
        </rest>
      </result>
    </xsl:when>

    <!-- If it's not a real word (i.e. it's punctuation), skip it and
       parse the next token as a term.-->
    <xsl:when test="$in[1]/@isWord = 'no'">
      <xsl:copy-of select="parse:term($rest)"/>
    </xsl:when>

    <!-- Okay, treat the word as a single term. -->
    <xsl:otherwise>
      <result>
        <value>
          <term>
            <xsl:value-of select="$word"/>
          </term>
        </value>
        <rest>
          <xsl:copy-of select="$rest"/>
        </rest>
      </result>
    </xsl:otherwise>
    
  </xsl:choose>
  
</xsl:function>

<!-- ======================================================================
   Function:    parse:phrase(in)
   Description: Parses a single phrase.
   Parameters:  in - one or more phrase and/or token elements
   ====================================================================== -->

<xsl:function name="parse:phrase">
  <xsl:param name="in"/>
  
  <!-- Divide the work up: the first thing, then all the rest -->
  <xsl:variable name="phrase" select="$in[1]"/>
  <xsl:variable name="rest" select="$in[position() &gt; 1]"/>
  
  <!-- If the phrase is followed by ~, it's a 'near' query. -->
  <xsl:choose>
    <xsl:when test="$rest[1]/@value = '~'">
      <xsl:copy-of select="parse:near($in)"/>
    </xsl:when>

    <xsl:otherwise>

      <!-- Otherwise, it's an exact phrase query. As usual, the result
         consists of the thing we parsed, then the rest of the
         unparsed tokens. -->
      <result>
        <value>
          <phrase>
            <xsl:for-each select="$phrase/token[@isWord = 'yes']">
              <term>
                <xsl:value-of select="@value"/>
              </term>
            </xsl:for-each>
          </phrase>
        </value>
        <rest>
          <xsl:copy-of select="$rest"/>
        </rest>
      </result>
    </xsl:otherwise>
  </xsl:choose>
</xsl:function>

<!-- ======================================================================
   Function:    parse:near(in)
   Description: Parses a 'near' query, where a slop value is specified.
   Parameters:  in - one or more phrase and/or token elements
   ====================================================================== -->

<xsl:function name="parse:near">
  <xsl:param name="in"/>
  
  <!-- Divide the work up: the first thing is a phrase; the second is a tilde;
     the third is the slop number, and then all the rest. -->
  <xsl:variable name="phrase" select="$in[1]"/>
  <xsl:variable name="tilde" select="$in[2]"/>
  <xsl:variable name="slop" select="string(number($in[3]/@value))"/>
  <xsl:variable name="rest" select="$in[position() &gt; 3]"/>
  
  <!-- Make sure slop was specified -->
  <xsl:choose>
  
    <!-- If we find a tilde character, make sure a slop value was specified -->
    <xsl:when test="$slop = 'NaN'">
      <xsl:copy-of select="parse:makeError('Number expected after &quot;~&quot;.', $rest)"/>
    </xsl:when>
    
    <xsl:otherwise>
      <result>
        <value>
          <near>
            <xsl:attribute name="slop">
              <xsl:value-of select="$slop"/>
            </xsl:attribute>
            <xsl:for-each select="$phrase/token">
              <term>
                <xsl:value-of select="@value"/>
              </term>
            </xsl:for-each>
          </near>
        </value>
        <rest>
          <xsl:copy-of select="$rest"/>
        </rest>
      </result>
    </xsl:otherwise>
  </xsl:choose>
</xsl:function>

<!-- ======================================================================
   Function:    parse:not(in)
   Description: Parses a 'not' clause.
   Parameters:  in - one or more phrase and/or token elements
   ====================================================================== -->

<xsl:function name="parse:not">
  <xsl:param name="in"/>
  
  <!-- What follows should be a term to exclude. -->
  <xsl:variable name="result" select="parse:term($in)"/>
  
  <!-- Our result is simply that term, followed by the remaining unparsed
     tokens. -->
  <result>
    <value>
      <not>
        <xsl:copy-of select="$result/value/*"/>
      </not>
    </value>
    <xsl:copy-of select="$result/rest"/>
  </result>
</xsl:function>

<!-- ======================================================================
   Function:    parse:range(operator, lower, upper, rest, inclusive)
   Description: Parses a range with a lower bound, upper bound, or both.
   Parameters:  opertor - '<', '<=', '>', '>=', or '..'
                lower - the lower bound, or () for none
                upper - the upper bound, or () for none
                rest - the remaining input
                inclusive - 'yes' or 'no'
   ====================================================================== -->

<xsl:function name="parse:range">
  <xsl:param name="operator"/>
  <xsl:param name="lower"/>
  <xsl:param name="upper"/>
  <xsl:param name="rest"/>
  <xsl:param name="inclusive"/>
  
  <xsl:choose>
  
    <!-- First, some error checking -->
    <xsl:when test="count($lower) > 0 and name($lower) != 'token'">
      <xsl:copy-of select="parse:makeError(
        concat('Invalid lower bound for range operator ', $operator), $rest)"/> 
    </xsl:when>
    <xsl:when test="count($upper) > 0 and name($upper) != 'token'">
      <xsl:copy-of select="parse:makeError(
        concat('Invalid upper bound for range operator ', $operator), $rest)"/>
    </xsl:when>
    <xsl:when test="count($lower) = 0 and count($upper) = 0">
      <xsl:copy-of select="parse:makeError(
        concat('Missing bound for range operator ', $operator), $rest)"/>
    </xsl:when>
    
    <!-- One of three cases is possible: lower, upper, or lower+upper. -->
    <xsl:otherwise>
      <result>
        <value>
          <range inclusive="{$inclusive}">
            <xsl:if test="count($lower) > 0">
              <lower> <xsl:value-of select="$lower/@value"/> </lower>
            </xsl:if>
            <xsl:if test="count($upper) > 0">
              <upper> <xsl:value-of select="$upper/@value"/> </upper>
            </xsl:if>
          </range>
        </value>
        <rest>
          <xsl:copy-of select="$rest"/>
        </rest>
      </result>
    </xsl:otherwise>
    
  </xsl:choose>

</xsl:function>

<!-- ======================================================================
   Function:    parse:subExpr(in)
   Description: Parses a parenthesized sub-expression.
   Parameters:  in - one or more phrase and/or token elements
   ====================================================================== -->

<xsl:function name="parse:subExpr">
  <xsl:param name="in"/>
  
  <!-- Parse the whole sub-expression. Note that the opening paren has
     already been eaten, so we don't have to worry about it. -->
  <xsl:variable name="result" select="parse:expr($in)"/>
  
  <!-- Make sure it's followed by a closing paren -->
  <xsl:choose>
    <xsl:when test="$result/rest/*[1]/@value = ')'">
      <result>
        <xsl:copy-of select="$result/value"/>
        <rest>
          <xsl:copy-of select="$result/rest/*[position() &gt; 1]"/>
        </rest>
      </result>
    </xsl:when>
    <xsl:otherwise>
      <xsl:copy-of select="parse:makeError('Missing &quot; ) &quot;', $in)"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:function>

<!-- ======================================================================
   Function:    parse:makeError(message, in)
   Description: Wraps an error message in a return value suitable for
                returning.
   Parameters:  message - the error message to wrap
                in - the remaining (unparsed) tokens
   ====================================================================== -->

<xsl:function name="parse:makeError">
  <xsl:param name="message"/>
  <xsl:param name="in"/>
  
  <result>
    <value>
      <error>
        <xsl:attribute name="message">
          <xsl:value-of select="$message"/>
        </xsl:attribute>
      </error>
    </value>
    <rest>
      <error>
        <xsl:attribute name="message">
          <xsl:value-of select="$message"/>
        </xsl:attribute>
      </error>
      <xsl:copy-of select="$in"/>
    </rest>
  </result>
</xsl:function>

<!-- ======================================================================
   Function:    parse:isReserved(word)
   Description: Tells whether a word is "reserved", i.e. that it has special
                meaning to the parser. For example, 'and' and 'or' are
                special. But parentheses are also special.
   Parameters:  word - the word to check
   Return val:  'yes' if it's reserved, else nothing.
   ====================================================================== -->

<xsl:function name="parse:isReserved">
  <xsl:param name="word"/>
  <xsl:choose>
    <xsl:when test="$word='and'">yes</xsl:when>
    <xsl:when test="$word='or'">yes</xsl:when>
    <xsl:when test="$word='not'">yes</xsl:when>
    <xsl:when test="$word='('">yes</xsl:when>
    <xsl:when test="$word=')'">yes</xsl:when>
  </xsl:choose>
</xsl:function>

</xsl:stylesheet>

