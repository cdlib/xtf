package org.cdlib.xtf.textEngine;

/*
 * Copyright (c) 2004, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, 
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, 
 *   this list of conditions and the following disclaimer in the documentation 
 *   and/or other materials provided with the distribution.
 * - Neither the name of the University of California nor the names of its
 *   contributors may be used to endorse or promote products derived from this 
 *   software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.xml.transform.Source;

import net.sf.saxon.Configuration;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.TreeBuilder;

import org.apache.lucene.chunk.SpanChunkedNotQuery;
import org.apache.lucene.chunk.SpanDechunkingQuery;
import org.apache.lucene.index.Term;
import org.apache.lucene.mark.ContextMarker;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanNotNearQuery;
import org.apache.lucene.search.spans.SpanOrNearQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.cdlib.xtf.textEngine.facet.FacetSpec;
import org.cdlib.xtf.textEngine.facet.GroupSelector;
import org.cdlib.xtf.textEngine.facet.MarkSelector;
import org.cdlib.xtf.textEngine.facet.RootSelector;
import org.cdlib.xtf.textEngine.facet.SelectorParser;
import org.cdlib.xtf.util.EasyNode;
import org.cdlib.xtf.util.FloatList;
import org.cdlib.xtf.util.GeneralException;
import org.cdlib.xtf.util.Path;
import org.cdlib.xtf.util.StringList;
import org.cdlib.xtf.util.Trace;

/**
 * Processes URL parameters into a Lucene query, using a stylesheet to perform
 * the heavy lifting.
 * 
 * @author Martin Haye
 */
public class QueryRequestParser 
{
    /** Partially parsed request in progress */
    private QueryRequest req;
    
    /** 
     * Keeps track of the servlet base directory, used to map relative
     * file paths.
     */
    private File        baseDir;
    
    /** 
     * Configuration object used when building trees (only created if
     * necessary.)
     */
    private Configuration config;
    
    /** The top-level source node */
    private NodeInfo topNode;
    
    /** Global attributes that were actually specified in the query */
    private HashSet specifiedGlobalAttrs = new HashSet();
    
    /** Accumulated list of grouping specifications */
    private Vector groupSpecs = new Vector(); 
    
    /** Default value for maxSnippets, so we can recognize difference between
     *  the default and a user-specified value.
     */
    private static final int DEFAULT_MAX_SNIPPETS = 888888888; 
    
    /**
     * Produce a Lucene query from the intermediate format that is normally
     * produced by the formatting stylesheet. Includes setting a default
     * indexPath, so the query doesn't have to contain one internally.
     * 
     * @param queryDoc A document containing the query.
     */
    public QueryRequest parseRequest( Source queryDoc,
                                      File   baseDir,
                                      String defaultIndexPath )
        throws QueryGenException, QueryFormatError
    {
        // Make a new request to start filling in.
        req = new QueryRequest();
        
        // Record the parameters
        this.baseDir = baseDir;
        req.indexPath = defaultIndexPath;
        
        // Now parse it, according to the kind of Source.
        if( queryDoc instanceof NodeInfo )
            parseOutputTop( new EasyNode((NodeInfo)queryDoc) );
        else {
            if( config == null )
                config = new Configuration();
            try {
                NodeInfo top = TreeBuilder.build( queryDoc, null, config );
                parseOutputTop( new EasyNode(top) );
            }
            catch( XPathException e ) {
                throw new RuntimeException( e );
            }
        }
        
        // Convert the grouping specifications to an easy-to-use array.
        if( groupSpecs.size() > 0 ) {
            req.facetSpecs = (FacetSpec[]) 
                groupSpecs.toArray( new FacetSpec[groupSpecs.size()] );
        }
        
        // And we're done.
        return req;
    } // parseRequest
    
    
    /**
     * Produce a Lucene query from the intermediate format that is normally
     * produced by the formatting stylesheet.
     * 
     * @param queryDoc A document containing the query.
     */
    public QueryRequest parseRequest( Source queryDoc,
                                      File   baseDir )
        throws QueryGenException, QueryFormatError
    {
        return parseRequest( queryDoc, baseDir, null );
    } // constructor
    
    
    /** Get an XML source suitable for re-creating this query */
    public Source getSource()
    {
        return topNode;
    } // getSource()
    
    
    /** Get the base directory from which relative paths are resolved */
    public File getBaseDir()
    {
        return baseDir;
    } // getBaseDir()
    
    
    /**
     * Convenience function to throw a {@link QueryGenException} with the 
     * given message.
     */
    private void error( String message )
        throws QueryGenException
    {
        throw new QueryGenException( message );
    } // error()
    
    
    /**
     * Processes the output of the generator stylesheet, turning it into a
     * Lucene query.
     * 
     * @param output The stylesheet output, whose first (and only) child
     *               should be a 'query' element.
     */
    private void parseOutputTop( EasyNode output )
        throws QueryGenException, QueryFormatError
    {
        if( "query".equals(output.name()) || "error".equals(output.name()) ) {
            parseOutput( output );
            return;
        }
        
        topNode = output.getWrappedNode();
        
        for( int i = 0; i < output.nChildren(); i++ ) {
            EasyNode main = output.child( i );
            String   name = main.name();

            if( !main.isElement() )
                continue;
            
            if( !name.equals("query") && !name.equals("error") )
                error( "Expected 'query' or 'error' element at " +
                       "top level; found '" + name + "'" );
            
            parseOutput( main );
        }
    } // parseOutput()
    
    /**
     * Processes the main query node, turning it into a Lucene query.
     * 
     * @param main The 'query' element
     */
    private void parseOutput( EasyNode main )
    {
        if( main.name().equals("error") )
            throw new QueryFormatError( main.attrValue("message") );
        
        // Process all the top-level attributes.
        for( int i = 0; i < main.nAttrs(); i++ ) {
            String name = main.attrName( i );
            String val  = main.attrValue( i );
            parseMainAttrib( main, name, val );
        }

        // Process the children. If we find an old <combine> element,
        // traverse it just like a top-level query.
        //
        int nChildQueries = 0;
        for( int i = 0; i < main.nChildren(); i++ ) {
            EasyNode el = main.child( i );
            if( !el.isElement() )
                continue;
            else if( "facet".equalsIgnoreCase(el.name()) )
                parseFacetSpec( el );
            else if( "spellcheck".equalsIgnoreCase(el.name()) )
                parseSpellcheck( el );
            else if( "resultData".equalsIgnoreCase(el.name()) )
                continue;
            else {
                req.query = 
                    deChunk( parseQuery(el, null, DEFAULT_MAX_SNIPPETS) );
                nChildQueries++;
            }
        }

        if( nChildQueries > 1 ) {
            error( "<" + main.name() + "> element must have " +
                   " at most one child query" );
        }
        
        if( main.name().equals("query") &&
            Trace.getOutputLevel() >= Trace.debug )
        {
            Trace.debug( "Lucene query as parsed: " + req.query );
        }
        
        // Check that we got the required parameters.
        if( main.name().equals("query") ) {
            if( req.indexPath == null )
                error( "'indexPath' attribute missing from <query> element" );
        }
        
    } // parseOutput()
    
    
    /**
     * Parses a 'facet' element and adds a FacetSpec to the query.
     * 
     * @param el  The 'facet' element to parse
     */
    void parseFacetSpec( EasyNode el ) 
    {
        // Process all the attributes.
        FacetSpec fs = new FacetSpec();
        for( int i = 0; i < el.nAttrs(); i++ ) 
        {
            if( el.attrName(i).equalsIgnoreCase("field") )
                fs.field = el.attrValue( i );
            else if( el.attrName(i).equalsIgnoreCase("sortGroupsBy") ) {
                if( el.attrValue(i).matches("^(totalDocs|value|maxDocScore)$") )
                    fs.sortGroupsBy = el.attrValue( i );
                else {
                    error( "Expected 'totalDocs', 'maxDocScore', or 'value' for '" +
                           el.attrName(i) + "' attribute, but found '" +
                           el.attrValue(i) + "' (on '" + el.name() + 
                           " element)" );
                }
            }
            else if( el.attrName(i).equalsIgnoreCase("sortDocsBy") )
                fs.sortDocsBy = el.attrValue( i );
            else if( el.attrName(i).equalsIgnoreCase("includeEmptyGroups") ) {
                if( el.attrValue(i).matches("^(true|yes)$") )
                    fs.includeEmptyGroups = true;
                else if( el.attrValue(i).matches("^(false|no)$") )
                    fs.includeEmptyGroups = false;
                else {
                    error( "Expected 'yes', 'no', 'true', or 'false' for '" +
                           el.attrName(i) + "' attribute, but found '" +
                           el.attrValue(i) + "' (on '" + el.name() +
                           " element)" );
                }
            }
            else if( el.attrName(i).equalsIgnoreCase("select") ) {
                try {
                    SelectorParser parser = new SelectorParser(
                        new StringReader(el.attrValue(i)) );
                    fs.groupSelector = parser.parse();
                } 
                catch (org.cdlib.xtf.textEngine.facet.ParseException e) {
                    error( "Error parsing '" + el.attrName(i) +
                           "' expression: " + e.getMessage() );
                } // catch
            } // else
        } // for i
        
        // Make sure a field name was specified.
        if( fs.field == null || fs.field.length() == 0 )
            error( "'" + el.name() + "' element requires 'field' attribute" );
        
        // If no group selection, put in the default.
        if( fs.groupSelector == null ) {
            GroupSelector root = new RootSelector();
            GroupSelector mark = new MarkSelector();
            root.setNext( mark );
            fs.groupSelector = root;
        }
        
        // Make sure there is only one groupField element per field.
        for( int i = 0; i < groupSpecs.size(); i++ ) {
            FacetSpec other = ((FacetSpec)groupSpecs.elementAt(i));
            if( other.field.equalsIgnoreCase(fs.field) )
                error( "Specifying two '" + el.name() + "' elements for the " +
                       "same field is illegal" );
        }
        
        // Finally, add the new group spec to the query.
        groupSpecs.add( fs );
        
    } // parseFacetSpec
    
    
    /**
     * Parses a 'spellcheck' element and adds a SpellcheckParams to the query.
     * 
     * @param el  The 'spellcheck' element to parse
     */
    void parseSpellcheck( EasyNode el ) 
    {
        SpellcheckParams params = new SpellcheckParams();
        
        // Process all the attributes.
        FacetSpec fs = new FacetSpec();
        for( int i = 0; i < el.nAttrs(); i++ ) 
        {
            String attr = el.attrName(i);
            
            if( attr.equalsIgnoreCase("suggestionsPerTerm") )
                params.suggestionsPerTerm = parseIntAttrib( el, attr );
            else if( attr.equalsIgnoreCase("fields") ||
                     attr.equalsIgnoreCase("field") )
            {
            }
            else if( attr.equalsIgnoreCase("docScoreCutoff") )
                params.docScoreCutoff = parseFloatAttrib( el, attr );
            else if( attr.equalsIgnoreCase("totalDocsCutoff") )
                params.totalDocsCutoff = parseIntAttrib( el, attr );
            else if( attr.equalsIgnoreCase("termOccurrenceFactor") ||
                     attr.equalsIgnoreCase("termOcurrenceFactor") ||
                     attr.equalsIgnoreCase("termOccurenceFactor") )
            {
                params.termOccurrenceFactor = parseFloatAttrib( el, attr );
            }
            else if( attr.equalsIgnoreCase("accuracy") )
                params.accuracy = parseFloatAttrib( el, attr );
            else
                error( "Unknown attribute '" + attr + "' on '" + el.name() + "' element" );
        } // for i
        
        // Make sure the number of suggestions was specified.
        if( params.suggestionsPerTerm <= 0 )
            error( "'" + el.name() + "' element requires 'suggestionsPerTerm' attribute" );
        
        // Finally, add the new params to the query.
        req.spellcheckParams = params;
        
    } // parseSpellcheck
    
    
    /**
     * Recursively parse a query.
     */
    private Query parseQuery( EasyNode parent, String field, int maxSnippets )
       throws QueryGenException
    {
        String name = parent.name();
        if( !name.matches("^(" +
                "query|term|all|range|phrase|exact|near" +
                "|and|or|not|orNear" +
                "|moreLike" +             // experimental
                "|combine|meta|text)$") ) // old stuff, for compatability
        {
            error( "Expected: 'query', 'term', 'all', 'range', 'phrase', " +
                   "'exact', 'near', 'orNear', 'and', 'or', 'not', " +
                   "or 'moreLike'; " +
                   "found '" + name + "'" );
        }
        
        // Old stuff, for compatability.
        if( name.equals("text") )
            field = "text";
        else
            field = parseField( parent, field );

        // 'not' queries are handled at the level above.
        assert( !name.equals("not") );
        
        // Default to no boost.
        float boost = 1.0f;
        
        // Validate all attributes.
        for( int i = 0; i < parent.nAttrs(); i++ ) {
            String attrName = parent.attrName( i );
            String attrVal  = parent.attrValue( i );
            
            if( attrName.equals("boost") )
                boost = parseFloatAttrib( parent, attrName );
            else if( attrName.equals("maxSnippets") ) {
                int oldVal = maxSnippets;
                maxSnippets = parseIntAttrib( parent, attrName );
                if( maxSnippets < 0 )
                    maxSnippets = 999999999;
                if( oldVal != DEFAULT_MAX_SNIPPETS &&
                    maxSnippets != oldVal )
                {
                    error( "Value specified for 'maxSnippets' attribute " +
                           "differs from that of an ancestor element." );
                }
            }
            else
                parseMainAttrib( parent, attrName, attrVal );
        }
        
        // Make sure boostSet and boostSetField are specified together
        if( req.boostSetParams != null ) {
            if( req.boostSetParams.field != null && req.boostSetParams.path == null )
                error( "'boostSetField' specified without 'boostSet'" );
            if( req.boostSetParams.field == null && req.boostSetParams.path != null ) 
                error( "'boostSet' specified without 'boostSetField'" );
            if( req.boostSetParams.exponent != 1.0f && req.boostSetParams.path == null ) 
                error( "'boostSetExponent' specified without 'boostSet'" );
            if( req.boostSetParams.defaultBoost != 1.0f && req.boostSetParams.path == null ) 
                error( "'boostSetDefault' specified without 'boostSet'" );
        }
        
        // Do the bulk of the parsing below...
        Query result = parseQuery2( parent, name, field, maxSnippets );
        if( result == null )
            return null;
        
        // And set any boost that was specified.
        if( boost != 1.0f )
            result.setBoost( boost );
        
        // If a sectionType query was specified, add that to the mix.
        SpanQuery secType = parseSectionType( parent, field, maxSnippets );
        if( secType != null ) {
            SpanQuery combo = 
                 new SpanSectionTypeQuery( (SpanQuery)result, secType );
            combo.setSpanRecording( ((SpanQuery)result).getSpanRecording() );
            result = combo;
        }
        
        // All done!
        return result;
        
    } // parseQuery()
   
    
    /** 
     * Main work of recursively parsing a query. 
     */
    private Query parseQuery2( EasyNode parent, String name, String field,
                               int maxSnippets )
        throws QueryGenException
    {
        // Term query is the simplest kind.
        if( name.equals("term") ) {
            Term term = parseTerm( parent, field, "term" );
            SpanQuery q = isWildcardTerm(term) ? 
                new XtfSpanWildcardQuery( term, req.termLimit ) :
                new SpanTermQuery( term );
            q.setSpanRecording( maxSnippets );
            return q;
        }
        
        // Get field name if specified.
        field = parseField( parent, field );
        
        // Range queries are also pretty simple.
        if( name.equals("range") )
            return parseRange( parent, field, maxSnippets );
        
        // Handle 'moreLike' queries separately.
        if( name.equals("moreLike") )
            return parseMoreLike( parent, field, maxSnippets );
        
        // Multi-field queries are a bit tricky, and therefore handled separately.
        if( parent.hasAttr("fields") )
            return parseMultiFieldQuery( parent, field, maxSnippets );

        // For text queries, 'all', 'phrase', 'exact', and 'near' can be viewed
        // as phrase queries with different slop values.
        //
        // 'all'    means essentially infinite slop (limited to the actual
        //          chunk overlap at runtime.)
        // 'phrase' means zero slop
        // 'exact'  means -1 slop (meaning use a SpanExactQuery)
        // 'near'   allows specifying the slop (again limited to the actual
        //          chunk overlap at runtime.)
        // 'orNear' is a special case which also allows specifying slop, but
        //          activates a different query.
        //
        if( name.matches("^(all|phrase|exact|near|orNear)$") )
        {   
            int slop = name.equals("all") ? 999999999 :
                       name.equals("phrase") ? 0 :
                       name.equals("exact") ? -1 :
                       parseIntAttrib( parent, "slop" );
            return makeProxQuery( parent, slop, field, maxSnippets );
        }
        
        // All other cases fall through to here: and, or. Generally we try
        // to convert these to span queries when possible. However, this
        // behavior can be turned off by setting the "useProximity" attribute
        // to false.
        //
        boolean useProximity = 
            parseBooleanAttrib( parent, "useProximity", true );
        if( !useProximity && !name.equals("and") )
            error( "The 'useProximity' attribute is only applicable to 'and' queries" );
        
        // Use our special de-duplicating span logic. Get all the sub-queries 
        // (including nots). As we go along, group them by field, and maintain 
        // a list of the unique field names in the order the fields 
        // were encountered.
        //
        HashMap      subMap  = new HashMap();
        Vector       fields  = new Vector();
        BooleanQuery bq      = new BooleanQuery();
        boolean      require = !name.equals("or");
        
        for( int i = 0; i < parent.nChildren(); i++ ) {
            EasyNode el = parent.child( i );
            if( !el.isElement() )
                continue;
            if( el.name().equals("sectionType") )
                continue; // handled elsewhere
            else if( el.name().equalsIgnoreCase("resultData") )
                continue; // ignore, handled by client's resultFormatter.xsl

            Query q;
            boolean isNot = false;
            if( el.name().equals("not") ) {
                q = parseQuery2(el, name, field, 0);
                isNot = true;
            }
            else
                q = parseQuery(el, field, maxSnippets);
            
            if( q == null )
                continue;
            
            if( useProximity && q instanceof SpanQuery ) {
                String queryField = ((SpanQuery)q).getField();
                QueryEntry ent = (QueryEntry) subMap.get( queryField );
                if( ent == null ) {
                    fields.add( queryField );
                    ent = new QueryEntry( queryField );
                    subMap.put( queryField, ent );
                }
                
                if( isNot )
                    ent.nots.add( q );
                else
                    ent.queries.add( q );
            }
            else {
                bq.add( deChunk(q), isNot ? false : require, isNot );
            }
        }

        // If there are no generic clauses (that is, clauses that span fields),
        // we can optimize.
        //
        BooleanClause[] genericClauses = bq.getClauses();
        if( genericClauses.length == 0 ) 
        {
            // If no sub-queries or not queries, return an empty query.
            if( subMap.isEmpty() )
                return null;
            
            // If there's only one field, we don't need (or want) to do dechunking
            // at this level. Simply make a span query for this field.
            //
            if( fields.size() == 1 ) {
                QueryEntry ent = (QueryEntry) subMap.get( fields.get(0) );
                if( ent.nots.isEmpty() ) {
                    return processSpanJoin( name, ent.queries, ent.nots, 
                                            maxSnippets );
                }
            }
        }
        
        // Process each field in turn, grouping queries into SpanQueries if
        // possible.
        //
        for( int i = 0; i < fields.size(); i++ ) {
            QueryEntry ent = (QueryEntry) subMap.get( fields.get(i) );
            int nQueries = ent.queries.size();
            int nNots    = ent.nots.size();
            
            // If there's more than one query for this field, or if there is one
            // query and one or more nots, group them together with a span query.
            //
            if( nQueries > 1 || (nQueries == 1 && nNots > 0) ) 
            {
                SpanQuery sq = processSpanJoin( name, ent.queries, ent.nots, 
                                                maxSnippets );
                bq.add( deChunk(sq), require, false );
                continue;
            }
            
            // Otherwise, simply add these as normal boolean clauses (of course
            // applying dechunking if necessary.)
            //
            for( int j = 0; j < ent.queries.size(); j++ )
                bq.add( deChunk((Query)ent.queries.get(j)), require, false );
            for( int j = 0; j < ent.nots.size(); j++ )
                bq.add( deChunk((Query)ent.nots.get(j)), false, true );
        } // for i
        
        // If we ended up with only one clause, we may have more to do...
        BooleanClause[] clauses = bq.getClauses();
        if( clauses.length == 1 ) 
        {
            // If the clause is required, just return it.
            if( clauses[0].required )
                return clauses[0].query;
            
            // If the clause is a 'not', it needs something to 'not' against.
            // Add another clause that just returns all valid documents.
            //
            else if( clauses[0].prohibited ) {
                Query allDocsQuery = new TermQuery( new Term("docInfo", "1") ); 
                bq.add( allDocsQuery, true, false );
            }
        }
        
        // Simplify the BooleanQuery (if possible), for instance collapsing
        // an AND query inside another AND query.
        //
        return simplifyBooleanQuery( bq );

    } // parseQuery2() 
        
        
    /**
     * Parse a 'keyword' query, known internally as a multi-field AND.
     */
    private Query parseMultiFieldQuery( EasyNode parent, String field, int maxSnippets )
    {
        // At the moment, only <and> and <or> are allowed to have multiple fields.
        String name = parent.name();
        if( !name.matches("^(and|or)$") )
            error( "multiple fields only supported for 'and' or 'or' queries" );
        
        // First, check that no regular 'field' has been specified.
        if( field != null )
            error( "multi-field query requires 'fields' attribute, not 'field'" );
        
        // Make sure 'fields' is present.
        String fieldsStr = parseStringAttrib( parent, "fields" );
        
        // Parse that into an array of fields.
        StringList fields = new StringList();
        StringTokenizer st = new StringTokenizer( fieldsStr, ";,| \t" );
        while( st.hasMoreTokens() )
            fields.add( st.nextToken() );
        
        // Make sure slop has been specified
        int slop = parseIntAttrib( parent, "slop" );
        
        // Optionally, the user can specify separate maxSnippets for text vs.
        // meta-data.
        //
        int maxMetaSnippets = parseIntAttrib( parent, "maxMetaSnippets", maxSnippets );
        int maxTextSnippets = parseIntAttrib( parent, "maxTextSnippets", maxSnippets );
        
        // Also, the user can specify a boost factor per field
        float[] boosts = null;
        if( parent.hasAttr("boosts") ) {
            boosts = parseFieldBoosts( parent, "boosts" );
            if( boosts != null && boosts.length > fields.size() )
                error( "'boosts' attribute may not contain more values than 'fields'" );
        }
        
        // Now parse all the sub-queries.
        ArrayList queryList = new ArrayList();
        for( int i = 0; i < parent.nChildren(); i++ ) {
            EasyNode el = parent.child( i );
            if( !el.isElement() )
                continue;
            else if( el.name().equalsIgnoreCase("resultData") )
                continue; // ignore, handled by client's resultFormatter.xsl
  
            Query q = parseQuery( el, fieldsStr, maxSnippets );
            if( q == null )
                continue;
            
            if( !(q instanceof SpanQuery) )
                error( "Internal error: sub-queries of 'keyword' must be span queries" );
            
            queryList.add( q );
        }
        
        // Form the final query.
        SpanQuery[] subQueries = (SpanQuery[])
            queryList.toArray( new SpanQuery[queryList.size()] );
        return createMultiFieldQuery( parent, fields.toArray(), boosts, 
                                      subQueries, slop, 
                                      maxMetaSnippets, maxTextSnippets );
        
    } // parseMultiFieldQuery()

    /**
     * Does the work of creating the guts of a keyword query.
     */
    private Query createMultiFieldQuery( EasyNode    parent, 
                                         String[]    fields, 
                                         float[]     boosts, 
                                         SpanQuery[] spanQueries, 
                                         int         slop,
                                         int         maxMetaSnippets,
                                         int         maxTextSnippets )
    {
        BooleanQuery mainQuery = new BooleanQuery(true /* disable coord */);
      
        // We'll be changing the field names a lot.
        RefieldingQueryRewriter refielder = new RefieldingQueryRewriter();
        
        // If it's an AND (as opposed to OR)...
        if( parent.name().equals("and") )
        {
            // Form a clause for each term, across all fields. This implements:
            //
            // And(
            //   term1 in field1 or field2 or field3...
            //   term2 in field1 or field2 or field3...
            //   ..
            // )
            //
            for( int i = 0; i < spanQueries.length; i++ ) {
                BooleanQuery termOrQuery = new BooleanQuery();
                for( int j = 0; j < fields.length; j++ ) {
                    Query tq = refielder.refield( spanQueries[i], fields[j] );
                    tq = deChunk( tq );
                    if( tq instanceof SpanQuery )
                        ((SpanQuery)tq).setSpanRecording( 0 );
                    termOrQuery.add( tq, false, false );
                }
                
                // Make sure these don't contribute to the overall score, but each
                // term must match in at least one field.
                //
                termOrQuery.setBoost( 0.0f );
                mainQuery.add( termOrQuery, true, false );
            }
        }
        
        // For highlighting and scoring computations, make a clause for
        // each field, searching for all terms if present. This implements:
        //
        // Or(
        //   OrNear(field1: term1,term2,...)
        //   OrNear(field2: term1,term2,...)
        //   ..
        // )
        //
        for( int i = 0; i < fields.length; i++ ) {
            SpanQuery[] termQueries = new SpanQuery[spanQueries.length];
            for( int j = 0; j < spanQueries.length; j++ )
                termQueries[j] = (SpanQuery) refielder.refield( spanQueries[j], fields[i] );
            SpanQuery fieldOrQuery = (SpanQuery) deChunk(
                new SpanOrNearQuery(termQueries, slop, true) );
            int maxSnippets = (fields[i].equals("text")) ? 
                              maxTextSnippets : maxMetaSnippets;
            fieldOrQuery.setSpanRecording( maxSnippets );
            if( boosts != null && i < boosts.length )
                fieldOrQuery.setBoost( boosts[i] );
            mainQuery.add( fieldOrQuery, false, false );
        }
        
        // All done.
        return simplifyBooleanQuery( mainQuery );
        
    } // createMultiFieldQuery()

    /**
     * Simplify a BooleanQuery that contains other BooleanQuery/ies with the
     * same type of clauses. If there's any boosting involved, don't do
     * the optimization.
     */
    private Query simplifyBooleanQuery( BooleanQuery bq )
    {
        boolean anyBoosting = false;
        boolean anyBoolSubs = false;
        boolean allSame = true;
        boolean first = true;
        boolean prevRequired = true;
        boolean prevProhibited = true;
        
        // Scan each clause.
        BooleanClause[] clauses = bq.getClauses();
        for( int i = 0; i < clauses.length; i++ ) 
        {
            // See if this clause is the same as the previous one.
            if( !first && 
                (prevRequired   != clauses[i].required ||
                 prevProhibited != clauses[i].prohibited ) )
                allSame = false;
            
            prevRequired   = clauses[i].required;
            prevProhibited = clauses[i].prohibited;
            first = false;
          
            // Detect any boosting
            if( clauses[i].query.getBoost() != 1.0f )
                anyBoosting = true;
            
            // If the clause is a BooleanQuery, check the sub-clauses...
            if( clauses[i].query instanceof BooleanQuery ) 
            {
                BooleanQuery    subQuery = (BooleanQuery) clauses[i].query;
                BooleanClause[] subClauses = subQuery.getClauses();
                
                // Scan each sub-clause
                for( int j = 0; j < subClauses.length; j++ ) 
                {
                    // Make sure it's the same as the previous clause.
                    if( prevRequired   != subClauses[j].required ||
                        prevProhibited != subClauses[j].prohibited )
                        allSame = false;
                    
                    prevRequired = subClauses[j].required;
                    prevProhibited = subClauses[j].prohibited;
                    
                    // Detect any boosting.
                    if( subClauses[j].query.getBoost() != 1.0f )
                        anyBoosting = true;
                } // for j
                
                // Note that we found at least one BooleanQuery clause.
                anyBoolSubs = true;
            }
        } // for i
        
        // If the main BooleanQuery doesn't meet all of our criteria for
        // simplification, simply return it unmodified.
        //
        if( !anyBoolSubs || !allSame || anyBoosting )
            return bq;
        
        // Create a new, simplified, query.
        bq = new BooleanQuery();
        for( int i = 0; i < clauses.length; i++ ) {
            if( clauses[i].query instanceof BooleanQuery ) {
                BooleanQuery    subQuery = (BooleanQuery) clauses[i].query;
                BooleanClause[] subClauses = subQuery.getClauses();
                for( int j = 0; j < subClauses.length; j++ )
                    bq.add( subClauses[j] );
            }
            else
                bq.add( clauses[i] );
        }

        // And we're done.
        return bq;
        
    } // simplifyBooleanQuery()
        
    
    /**
     * Parse an attribute on the main query element (or, for backward
     * compatability, on its immediate children.)
     * 
     * If the attribute isn't recognized, an error exception is thrown.
     */
    void parseMainAttrib( EasyNode el, String attrName, String val )
    {
        if( attrName.equals("style") )
            req.displayStyle = onceOnlyPath( req.displayStyle, el, attrName );

        else if( attrName.equals("startDoc") ) {
            req.startDoc = onceOnlyAttrib( req.startDoc+1, el, attrName );
            
            // Adjust for 1-based start doc.
            req.startDoc = Math.max( 0, req.startDoc-1 );
        }
        
        else if( attrName.equals("maxDocs") )
            req.maxDocs = onceOnlyAttrib( req.maxDocs, el, attrName );
        
        else if( attrName.equals("indexPath") )
            req.indexPath = onceOnlyPath( req.indexPath, el, attrName );
        
        else if( attrName.equals("termLimit") )
            req.termLimit = onceOnlyAttrib( req.termLimit, el, attrName );
        
        else if( attrName.equals("workLimit") )
            req.workLimit = onceOnlyAttrib( req.workLimit, el, attrName );
        
        else if( attrName.equals("sortDocsBy") ||
                 attrName.equals("sortMetaFields") ) // old, for compatibility
            req.sortMetaFields = onceOnlyAttrib( req.sortMetaFields, el, attrName );
        
        else if( attrName.equals("maxContext") || attrName.equals("contextChars") )
            req.maxContext = onceOnlyAttrib( req.maxContext, el, attrName );
        
        else if( attrName.equals("termMode") ) {
            int oldTermMode = req.termMode;
            if( val.equalsIgnoreCase("none") )
                req.termMode = ContextMarker.MARK_NO_TERMS;
            else if( val.equalsIgnoreCase("hits") )
                req.termMode = ContextMarker.MARK_SPAN_TERMS;
            else if( val.equalsIgnoreCase("context") )
                req.termMode = ContextMarker.MARK_CONTEXT_TERMS;
            else if( val.equalsIgnoreCase("all") )
                req.termMode = ContextMarker.MARK_ALL_TERMS;
            else
                error( "Unknown value for 'termMode'; expecting " +
                       "'none', 'hits', 'context', or 'all'" );
            
            if( specifiedGlobalAttrs.contains(attrName) && 
                req.termMode != oldTermMode )
            {
                error( "'termMode' attribute should only be specified once." );
            }
            specifiedGlobalAttrs.add( attrName );
        }
        
        else if( attrName.equalsIgnoreCase("boostSet") ) {
            if( req.boostSetParams == null )
                req.boostSetParams = new BoostSetParams();
            req.boostSetParams.path = onceOnlyPath( req.boostSetParams.path, el, attrName );
        }
        
        else if( attrName.equalsIgnoreCase("boostSetField") ){
            if( req.boostSetParams == null )
                req.boostSetParams = new BoostSetParams();
            req.boostSetParams.field = parseStringAttrib( el, attrName );
        }
        
        else if( attrName.equalsIgnoreCase("boostSetExponent") ){
            if( req.boostSetParams == null )
                req.boostSetParams = new BoostSetParams();
            req.boostSetParams.exponent = parseFloatAttrib( el, attrName );
        }
        
        else if( attrName.equalsIgnoreCase("boostSetDefault") ){
            if( req.boostSetParams == null )
                req.boostSetParams = new BoostSetParams();
            req.boostSetParams.defaultBoost = parseFloatAttrib( el, attrName );
        }
        
        else if( attrName.equalsIgnoreCase("normalizeScores") )
            req.normalizeScores = parseBooleanAttrib( el, "normalizeScores" );

        else if( attrName.equalsIgnoreCase("explainScores") )
            req.explainScores = parseBooleanAttrib( el, "explainScores" );

        else if( attrName.equals("field") || attrName.equals("metaField") )
            ; // handled elsewhere
        
        else if( attrName.equals("fields") && el.name().matches("^(and|or)$") )
          ; // handled elsewhere
      
        else if( (attrName.equals("inclusive") || attrName.equals("numeric")) &&
                 el.name().equals("range") )
            ; // handled elsewhere
        
        else if( attrName.equals("slop") &&
                 el.name().matches("^(near|orNear)$") )
            ; // handled elsewhere
        
        else if( attrName.matches("^(slop|boosts)$") &&
                 el.name().matches("^(and|or)$") &&
                 el.hasAttr("fields"))
           ; // handled elsewhere
        
        else if( attrName.matches("^(maxTextSnippets|maxMetaSnippets)$") &&
                 el.name().matches("^(and|or)$") &&
                 el.hasAttr("fields"))
           ; // handled elsewhere
   
        else if( attrName.equalsIgnoreCase("useProximity") &&
                 el.name().matches("^(and|or)$") )
            ; // handled elsewhere
        
        else if( attrName.matches("^(fields|boosts|minWordLen|maxWordLen|minDocFreq|maxDocFreq|minTermFreq|termBoost|maxQueryTerms)$") &&
                 el.name().equals("moreLike") )
            ; // handled elsewhere
        
        else {
            error( "Unrecognized attribute \"" + attrName + "\" " +
                   "on <" + el.name() + "> element" );
        }
    } // parseMainAttrib()
    
    
    /**
     * Parse a 'sectionType' query element, if one is present. If not, 
     * simply returns null.
     */
    private SpanQuery parseSectionType( EasyNode parent, 
                                        String field,
                                        int maxSnippets )
        throws QueryGenException
    {
        // Find the sectionType element (if any)
        EasyNode sectionType = parent.child( "sectionType" );
        if( sectionType == null )
            return null;
        
        // These sectionType queries only belong in the "text" field.
        if( !(field.equals("text")) )
            error( "'sectionType' element is only appropriate in queries on the 'text' field" );
        
        // Make sure it only has one child.
        if( sectionType.nChildren() != 1 )
            error( "'sectionType' element requires exactly " +
                   "one child element" );
        
        Query ret = parseQuery( sectionType.child(0), 
                                "sectionType", maxSnippets );
        if( !(ret instanceof SpanQuery) )
            error( "'sectionType' sub-query must use proximity" );
        
        return (SpanQuery) ret;
    } // parseSectionType()

    
    /**
     * If the given element has a 'field' attribute, return its value;
     * otherwise return 'parentField'. Also checks that field cannot be
     * specified if parentField has already been.
     */
    private String parseField( EasyNode el, String parentField )
        throws QueryGenException
    {
        if( !el.hasAttr("metaField") && !el.hasAttr("field") )
            return parentField;
        String attVal = el.attrValue("field");
        if( attVal == null || attVal.length() == 0 )
            attVal = el.attrValue( "metaField" );
        
        if( attVal.length() == 0 )
            error( "'field' attribute cannot be empty" );
        if( attVal.equals("sectionType") &&
            (parentField == null || !parentField.equals("sectionType")) )
            error( "'sectionType' is not valid for the 'field' attribute" );
        if( parentField != null && !parentField.equals(attVal) )
            error( "Cannot override ancestor 'field' attribute" );
        
        return attVal;
    }

    
    /**
     * Joins a number of span queries together using a span query.
     * 
     * @param name    'and', 'or', 'near', etc.
     * @param subVec  Vector of sub-clauses
     * @param notVec  Vector of not clauses (may be empty)
     * 
     * @return        A new Span query joining the sub-clauses.
     */
    private SpanQuery processSpanJoin( String name, Vector subVec, 
                                       Vector notVec, int maxSnippets )
    {
        // Get a handy array of the queries.
        SpanQuery[] subQueries = 
            (SpanQuery[]) subVec.toArray( new SpanQuery[0] ); 
        
        // If there's only one query (with no nots) then just return it.
        if( subQueries.length == 1 && notVec.isEmpty() )
            return subQueries[0];
        
        // Now make the top-level query.
        SpanQuery q;
        if( subQueries.length == 1 )
            q = subQueries[0];
        else if( name.equals("orNear") ) {
            // We can't know the actual slop until the query is run against
            // an index (the slop will be equal to max proximity). So set
            // it to a big value for now, and it will be clamped later
            // when the query is run.
            //
            q = new SpanOrNearQuery( subQueries, 999999999, true );
        }
        else if( !name.equals("or") ) {
            // We can't know the actual slop until the query is run against
            // an index (the slop will be equal to max proximity). So set
            // it to a big value for now, and it will be clamped later
            // when the query is run.
            //
            q = new SpanNearQuery( subQueries, 999999999, false );
        }
        else
            q = new SpanOrQuery( subQueries );

        q.setSpanRecording( maxSnippets );
        
        // Finish up by handling any not clauses found.
        return processSpanNots( q, notVec, maxSnippets );
        
    } // processSpanJoin()

    /**
     * Ensures that the given query, if it is a span query on the "text"
     * field, is wrapped by a de-chunking query.
     */
    public static Query deChunk( Query q )
    {
        // We only need to de-chunk span queries, not other queries.
        if( !(q instanceof SpanQuery) )
            return q;
        
        // Furthermore, we only need to de-chunk queries on the "text"
        // field.
        //
        SpanQuery sq = (SpanQuery) q;
        if( !sq.getField().equals("text") )
            return q;
        
        // If it's already de-chunked, no need to do it again.
        if( sq instanceof SpanDechunkingQuery )
            return q;
        
        // Okay, wrap it.
        SpanDechunkingQuery dq = new SpanDechunkingQuery( sq );
        dq.setSpanRecording( sq.getSpanRecording() );
        return dq;
        
    } // deChunk()  
      
    /** Determines if the term contains a wildcard character ('*' or '?') */
    private boolean isWildcardTerm( Term term )
    {
        if( term.text().indexOf('*') >= 0 )
            return true;
        if( term.text().indexOf('?') >= 0 )
            return true;
        return false;
    } // isWildcardTerm()

    /**
     * Parse a range query.
     */
    private Query parseRange( EasyNode parent, String field, int maxSnippets )
        throws QueryGenException
    {
        // Inclusive or exclusive?
        boolean inclusive = parseBooleanAttrib(parent, "inclusive", true );
        boolean numeric   = parseBooleanAttrib(parent, "numeric",   false );
        
        // Check the children for the lower and upper bounds.
        Term lower = null;
        Term upper = null;
        for( int i = 0; i < parent.nChildren(); i++ ) {
            EasyNode child = parent.child( i );
            if( !child.isElement() )
                continue;
            String name = child.name();
            if( name.equals("lower") ) {
                if( lower != null )
                    error( "'lower' only allowed once as child of 'range' element" );
                if( child.child("term") != null )
                    lower = parseTerm( child.child("term"), field, "term" );
                else
                    lower = parseTerm( child, field, "lower" );
            }
            else if( name.equals("upper") ) {
                if( upper != null )
                    error( "'upper' only allowed once as child of 'range' element" );
                if( child.child("term") != null )
                    upper = parseTerm( child.child("term"), field, "term" );
                else
                    upper = parseTerm( child, field, "upper" );
            }
            else
                error( "'range' element may only have 'lower' and/or 'upper' " +
                       "as child elements" );
        } // for iter
        
        // Upper, lower, or both must be specified.
        if( lower == null && upper == null )
            error( "'range' element must have 'lower' and/or 'upper' child element(s)" );
        
        // And we're done.
        if( numeric ) {
          return new NumericRangeQuery( field, 
              (lower == null) ? null : lower.text(), 
              (upper == null) ? null : upper.text(), 
              inclusive, inclusive );
        }
        else 
        {
            // If no upper specified, we're in danger of accidentally matching the
            // XTF special tokens. So be sure to exclude the whole area that marker
            // characters are in.
            //
            if( upper == null ) {
                char[] tmp = new char[1];
                tmp[0] = Constants.MARKER_BASE;
                upper = new Term( lower.field(), new String(tmp) );
            }
        
            // Now make the query.
            SpanQuery q = new XtfSpanRangeQuery( lower, upper, inclusive, req.termLimit );
            q.setSpanRecording( maxSnippets );
            return q;
        }
    } // parseRange()

    /**
     * If any 'not' clauses are present, this builds a query that filters them
     * out of the main query.
     */
    SpanQuery processSpanNots( SpanQuery query, Vector notClauses,
                               int maxSnippets ) 
    {
        // If there aren't any 'not' clauses, we're done.
        if( notClauses.isEmpty() )
            return query;
        
        // If there's only one, the sub-query is simple.
        SpanQuery subQuery;
        if( notClauses.size() == 1 )
            subQuery = (SpanQuery) notClauses.get( 0 );
        else 
        {
            // Otherwise, 'or' all the nots together.
            SpanQuery[] subs = (SpanQuery[]) 
                notClauses.toArray( new SpanQuery[0] );
            subQuery = new SpanOrQuery( subs );
            subQuery.setSpanRecording( maxSnippets );
        }
        
        // Now make the final 'not' query. If on the text field,
        // use the special chunk-aware version.
        //
        SpanQuery nq;
        if( query.getField().equals("text") ) {
        
            // Note that the actual slop will have to be fixed when the 
            // query is run.
            //
            nq = new SpanChunkedNotQuery( query, subQuery, 999999999 );
        }
        else
            nq = new SpanNotNearQuery( query, subQuery, 999999999 );
        
        // Establish the span recording, and we're done.
        nq.setSpanRecording( maxSnippets );
        return nq;
    } // processTextNots();
    
    
    /**
     * Generate a proximity query on a field. This uses the de-duplicating span
     * system.
     * 
     * @param parent The element containing the field name and terms.
     */
    Query makeProxQuery( EasyNode parent, int slop, String field,
                         int maxSnippets )
        throws QueryGenException
    {
        Vector terms  = new Vector();
        Vector notVec = new Vector();
        for( int i = 0; i < parent.nChildren(); i++ ) {
            EasyNode el = parent.child( i );
            if( !el.isElement() )
                continue;
            if( el.name().equals("not") ) {
                if( parent.name().matches("^(phrase|exact)$") )
                    error( "'not' clauses aren't supported in phrase/exact queries" );
                
                // Make sure to avoid adding the 'not' terms to the term map,
                // since it would be silly to hilight them.
                //
                notVec.add( parseQuery2(el, "not", field, maxSnippets) );
            }
            else {
                SpanQuery q;
                if( slop == 0 ) {
                    Term t = parseTerm( el, field, "term" );
                    if( isWildcardTerm(t) )
                        q = new XtfSpanWildcardQuery(t, req.termLimit);
                    else
                        q = new SpanTermQuery(t);
                    q.setSpanRecording( maxSnippets );
                    terms.add( q );
                }
                else
                    terms.add( parseQuery(el, field, maxSnippets) );
            }
        }
        
        if( terms.size() == 0 )
            error( "'" + parent.name() + "' element requires at " +
                   "least one term" );
        
        // Handle 'exact' queries specially.
        SpanQuery q;
        SpanQuery[] termQueries = (SpanQuery[]) terms.toArray(
                                                   new SpanQuery[terms.size()] ); 
        if( slop < 0 )
            q = new SpanExactQuery( termQueries );

        // Optimization: treat a single-term 'all' query as just a simple
        // term query.
        //
        else if( terms.size() == 1 )
            q = (SpanQuery) terms.elementAt( 0 );
        
        // Handle orNear queries specially.
        else if( parent.name().equals("orNear") )
            q = new SpanOrNearQuery( termQueries, slop, true );
        
        // Make a 'near' query out of it. Zero slop implies in-order.
        else
            q = new SpanNearQuery( termQueries, slop, slop == 0 );

        // Set up the span recording, and add in any nots present.
        q.setSpanRecording( maxSnippets );
        return processSpanNots( q, notVec, maxSnippets );
        
    } // makeProxQuery()
    
    /**
     * Parses a "more like this" query.
     */
    private Query parseMoreLike( EasyNode parent, 
                                 String   field, 
                                 int      maxSnippets )
    {
        // First, parse the sub-query.
        Query subQuery = null;
        for( int i = 0; i < parent.nChildren(); i++ ) {
            EasyNode el = parent.child( i );
            if( !el.isElement() )
                continue;
            else if( el.name().equalsIgnoreCase("resultData") )
                continue; // ignore, handled by client's resultFormatter.xsl

            if( subQuery != null )
                error( "'moreLike' element may not have more than one sub-query" );
            
            subQuery = parseQuery( el, field, 0 ); // no snippets
        }

        if( subQuery == null )
            error( "'moreLike' element requires a sub-query" );
        
        // Form up the result.
        MoreLikeThisQuery ret = new MoreLikeThisQuery( subQuery );
        
        // Process any optional attributes.
        for( int i = 0; i < parent.nAttrs(); i++ ) {
            String attrName = parent.attrName( i );
            if( attrName.equalsIgnoreCase("minWordLen") )
                ret.setMinWordLen( parseIntAttrib(parent, attrName) );
            else if( attrName.equalsIgnoreCase("maxWordLen") )
                ret.setMaxWordLen( parseIntAttrib(parent, attrName) );
            else if( attrName.equalsIgnoreCase("minDocFreq") )
                ret.setMinDocFreq( parseIntAttrib(parent, attrName) );
            else if( attrName.equalsIgnoreCase("maxDocFreq") )
                ret.setMaxDocFreq( parseIntAttrib(parent, attrName) );
            else if( attrName.equalsIgnoreCase("minTermFreq") )
                ret.setMinTermFreq( parseIntAttrib(parent, attrName) );
            else if( attrName.equalsIgnoreCase("termBoost") )
                ret.setBoost( parseBooleanAttrib(parent, attrName) );
            else if( attrName.equalsIgnoreCase("maxQueryTerms") )
                ret.setMaxQueryTerms( parseIntAttrib(parent, attrName) );
            else if( attrName.equalsIgnoreCase("fields") )
                ret.setFieldNames( parseFieldNames(parent, attrName) );
            else if( attrName.equalsIgnoreCase("boosts") )
                ret.setFieldBoosts( parseFieldBoosts(parent, attrName) );
            else
                error( "Unrecognized attribute '" + attrName + "' on 'moreLike' element" );
        }
        
        // Make sure at least one field was specified.
        String[] fields = ret.getFieldNames();
        if( fields == null || fields.length == 0 )
            error( "At least one field name must be specified in 'fields' attribute on 'moreLike' query" );
        
        // Make sure that, if boosts were specified, there are the same number.
        float[] boosts = ret.getFieldBoosts();
        if( boosts != null && boosts.length != fields.length )
            error( "Must specify same number of boosts as fields in 'boosts' attribute on 'moreLike' query" );
        
        // All done.
        return ret;
      
    } // parseMoreLike()
    

    /**
     * Parse a list of field names. They can be separated by spaces, tabs,
     * commas, semicolons, or pipe symbols.
     * 
     * @param parent      Node to look at
     * @param attrName    Attribute to get the list from
     * @return            Array of field names, or null if none.
     */
    private String[] parseFieldNames( EasyNode parent, String attrName )
    {
        String val = parseStringAttrib( parent, attrName );
        StringTokenizer tok = new StringTokenizer( val, " \t\r\n,;|" );
        ArrayList list = new ArrayList();
        while( tok.hasMoreTokens() )
            list.add( tok.nextToken() );
        if( list.size() > 0 )
            return (String[]) list.toArray(new String[list.size()]);
        else
            return null;
    } // parseFieldNames()
    
    
    /**
     * Parse a list of field boosts. They can be separated by spaces, tabs,
     * commas, semicolons, or pipe symbols.
     * 
     * @param parent      Node to look at
     * @param attrName    Attribute to get the list from
     * @return            Array of field boosts, or null if none.
     */
    private float[] parseFieldBoosts( EasyNode parent, String attrName )
    {
        String val = parseStringAttrib( parent, attrName );
        StringTokenizer tok = new StringTokenizer( val, " \t\r\n,;|" );
        FloatList list = new FloatList();
        while( tok.hasMoreTokens() ) {
            String strVal = tok.nextToken();
            try {
                list.add( Float.parseFloat(strVal) );
            }
            catch( NumberFormatException e ) {
                error( "Each value for 'boosts' must be a valid floating-point number" );
            }
        }
        if( list.size() > 0 )
            return list.toArray();
        else
            return null;
    } // parseFieldBoosts()
    

    /**
     * Parses a 'term' element. If not so marked, an exception is thrown.
     * 
     * @param parent The element to parse
     */
    private Term parseTerm( EasyNode parent, String field, String expectedName )
        throws QueryGenException
    {
        // Get field name if specified.
        field = parseField( parent, field );
        if( field == null )
            error( "'term' element requires 'field' attribute on " +
                   "itself or an ancestor" );
        
        if( !parent.name().equals(expectedName) )
            error( "Expected '" + expectedName + "' as child of '" + 
                   parent.parent().name() +
                   "' element, but found '" + parent.name() + "'" );
        
        String termText  = getText( parent );
        if( termText == null || termText.length() == 0 )
            error( "Missing term text in element '" + parent.name() + "'" );
        
        // For now, convert text to lowercase. In the future, we might allow
        // case-sensitive searching.
        //
        termText = termText.toLowerCase();
        
        // Make a term out of the field and the text.
        Term term = new Term( field, termText );
        
        return term;
        
    } // parseTerm()
    
    /**
     * Ensures that the element has only a single child node (ignoring
     * attributes), and that it's a text node.
     * 
     * @param el The element to get the text of
     * @return The string value of the text
     */
    private String getText( EasyNode el )
        throws QueryGenException
    {
        // There should be no element children, only text.
        int count = 0;
        String text = null;
        for( int i = 0; i < el.nChildren(); i++ ) {
            EasyNode n = el.child(i);
            if( !n.isElement() && !n.isText() )
            {
                count = -1;
                break;
            }
            if( n.isText() )
                text = n.toString();
            count++;
        }
        
        if( count != 1 )
            error( "A single text node is required for the '" +
                   el.name() + "' element" );
        
        return text;
    } // getText()

    
    /**
     * Like parseIntAttrib(), but adds additional processing to ensure that
     * global parameters are only specified once (or if multiple times, that
     * the same value is used each time.)
     * 
     * @param oldVal      Current value of the global parameter
     * @param el          Element to get the attribute from
     * @param attribName  Name of the attribute
     * @return            New value for the parameter
     */
    private int onceOnlyAttrib( int oldVal, EasyNode el, String attribName )
    {
        int newVal = parseIntAttrib( el, attribName );
        if( specifiedGlobalAttrs.contains(attribName) && newVal != oldVal ) {
            error( "'" + attribName + 
                   "' attribute should only be specified once." );
        }
        specifiedGlobalAttrs.add( attribName );
        return newVal;
    } // onceOnlyAttrib()

    /**
     * Like parseStringAttrib(), but adds additional processing to ensure that
     * global parameters are only specified once (or if multiple times, that
     * the same value is used each time.)
     * 
     * @param oldVal      Current value of the global parameter
     * @param el          Element to get the attribute from
     * @param attribName  Name of the attribute
     * @return            New value for the parameter
     */
    private String onceOnlyAttrib( String oldVal, 
                                   EasyNode el, 
                                   String attribName )
    {
        String newVal = parseStringAttrib( el, attribName );
        if( specifiedGlobalAttrs.contains(attribName) && 
            !oldVal.equals(newVal) )
        {
            error( "'" + attribName + 
                   "' attribute should only be specified once." );
        }
        specifiedGlobalAttrs.add( attribName );
        return newVal;
    } // onceOnlyAttrib()

    /**
     * Like onceOnlyAttrib(), but also ensures that the given file can
     * actually be resolved as a path that can be read.
     * 
     * @param oldVal      Current value of the global parameter
     * @param el          Element to get the attribute from
     * @param attribName  Name of the attribute
     * @return            New value for the parameter
     */
    private String onceOnlyPath( String oldVal, 
                                 EasyNode el, 
                                 String attribName )
    {
        String newVal = parseStringAttrib( el, attribName );
        String path;
        if( newVal.startsWith("http:") )
            path = newVal;
        else
            path = Path.resolveRelOrAbs( baseDir, newVal );
        
        if( specifiedGlobalAttrs.contains(attribName) && 
            !oldVal.equals(path) )
        {
            error( "'" + attribName + 
                   "' attribute should only be specified once." );
        }
        specifiedGlobalAttrs.add( attribName );

        if( !path.startsWith("http:") &&
            !newVal.equals("NullStyle.xsl") && 
            !(new File(path).canRead()) )
        {
            error( "File \"" + newVal + "\" specified in '" + 
                   el.name() + "' element " + "does not exist" );
        }
        
        return path;
    } // onceOnlyPath()

    /**
     * Locate the named attribute and retrieve its value as an integer.
     * If not found, an error exception is thrown.
     * 
     * @param el Element to search
     * @param attribName Attribute to find
     */
    private int parseIntAttrib( EasyNode el, String attribName )
        throws QueryGenException
    {
        return parseIntAttrib( el, attribName, false, 0 );
    }
    
    /**
     * Locate the named attribute and retrieve its value as an integer.
     * If not found, return a default value.
     * 
     * @param el EasyNode to search
     * @param attribName Attribute to find
     * @param defaultVal If not found and useDefault is true, return this 
     *                   value.
     */
    private int parseIntAttrib( EasyNode el, 
                                String attribName, 
                                int defaultVal  )
        throws QueryGenException
    {
        return parseIntAttrib( el, attribName, true, defaultVal );
    }
    
    /**
     * Locate the named attribute and retrieve its value as an integer.
     * Handles default processing if requested.
     * 
     * @param el EasyNode to search
     * @param attribName Attribute to find
     * @param useDefault true to supply a default value if none found,
     *                   false to throw an exception if not found.
     * @param defaultVal If not found and useDefault is true, return this 
     *                   value.
     */
    private int parseIntAttrib( EasyNode el, String attribName, 
                                boolean useDefault, int defaultVal )
        throws QueryGenException
    {
        String elName = el.name();
        String str = parseStringAttrib( el, 
                                        attribName,
                                        useDefault,
                                        null );
        if( str == null && useDefault )
            return defaultVal;
        
        if( str.equals("all") )
            return 999999999;
        
        try {
            return Integer.parseInt( str );
        } catch( Exception e ) {
            error( "'" + attribName + "' attribute of '" + elName + 
                   "' element is not a valid integer" );
            return 0;
        }
    } // parseIntAttrib()
    
    
    /**
     * Locate the named attribute and retrieve its value as a float.
     * If not found, an error exception is thrown.
     * 
     * @param el Element to search
     * @param attribName Attribute to find
     */
    private float parseFloatAttrib( EasyNode el, String attribName )
        throws QueryGenException
    {
        return parseFloatAttrib( el, attribName, false, 0 );
    }
    
    /**
     * Locate the named attribute and retrieve its value as a float.
     * If not found, return a default value.
     * 
     * @param el EasyNode to search
     * @param attribName Attribute to find
     * @param defaultVal If not found and useDefault is true, return this 
     *                   value.
     */
    private float parseFloatAttrib( EasyNode el, 
                                    String attribName, 
                                    float defaultVal  )
        throws QueryGenException
    {
        return parseFloatAttrib( el, attribName, true, defaultVal );
    }
    
    /**
     * Locate the named attribute and retrieve its value as a float. Negative
     * values are not allowed. Handles default processing if requested.
     * 
     * @param el EasyNode to search
     * @param attribName Attribute to find
     * @param useDefault true to supply a default value if none found,
     *                   false to throw an exception if not found.
     * @param defaultVal If not found and useDefault is true, return this 
     *                   value.
     */
    private float parseFloatAttrib( EasyNode el, String attribName, 
                                    boolean useDefault, float defaultVal )
        throws QueryGenException
    {
        String elName = el.name();
        String str = parseStringAttrib( el, 
                                        attribName,
                                        useDefault,
                                        null );
        if( str == null && useDefault )
            return defaultVal;
        
        try {
            float ret = Float.parseFloat( str );
            if( ret < 0 ) {
                error( "'" + attribName + "' attribute of '" + elName + 
                   "' element is not allowed to be negative" );
            }
            return ret;
        } catch( NumberFormatException e ) {
            error( "'" + attribName + "' attribute of '" + elName + 
                   "' element is not a valid floating-point number" );
            return 0;
        }
    } // parseFloatAttrib()
    
    
    /**
     * Locate the named attribute and retrieve its value as an boolean.
     * If not found, an error exception is thrown.
     * 
     * @param el Element to search
     * @param attribName Attribute to find
     */
    private boolean parseBooleanAttrib( EasyNode el, String attribName )
        throws QueryGenException
    {
        return parseBooleanAttrib( el, attribName, false, false );
    }
    
    /**
     * Locate the named attribute and retrieve its value as an boolean.
     * If not found, return a default value.
     * 
     * @param el EasyNode to search
     * @param attribName Attribute to find
     * @param defaultVal If not found and useDefault is true, return this 
     *                   value.
     */
    private boolean parseBooleanAttrib( EasyNode el, 
                                        String   attribName, 
                                        boolean  defaultVal  )
        throws QueryGenException
    {
        return parseBooleanAttrib( el, attribName, true, defaultVal );
    }
    
    /**
     * Locate the named attribute and retrieve its value as an boolean.
     * Handles default processing if requested.
     * 
     * @param el EasyNode to search
     * @param attribName Attribute to find
     * @param useDefault true to supply a default value if none found,
     *                   false to throw an exception if not found.
     * @param defaultVal If not found and useDefault is true, return this 
     *                   value.
     */
    private boolean parseBooleanAttrib( EasyNode el, String attribName, 
                                        boolean useDefault, 
                                        boolean defaultVal )
        throws QueryGenException
    {
        String elName = el.name();
        String str = parseStringAttrib( el, 
                                        attribName,
                                        useDefault,
                                        null );
        if( str == null && useDefault )
            return defaultVal;
        
        if( str.matches("^(yes|true|1)$") )
            return true;
        else if( str.matches("^(no|false|0)$") )
            return false;
        
        error( "'" + attribName + "' attribute of '" + elName + 
               "' element is not a valid boolean (yes/no/true/false/1/0)" );
        return false;
    } // parseBooleanAttrib()
    
    
    /**
     * Locate the named attribute and retrieve its value as a string. If
     * not found, an error exception is thrown.
     * 
     * @param el EasyNode to search
     * @param attribName Attribute to find
     */
    private String parseStringAttrib( EasyNode el, 
                                      String  attribName ) 
        throws QueryGenException
    {
        return parseStringAttrib( el, attribName, false, null );
    }
    
    /**
     * Locate the named attribute and retrieve its value as a string. If
     * not found, return a default value.
     * 
     * @param el EasyNode to search
     * @param attribName Attribute to find
     * @param defaultVal If not found, return this value.
     */
    private String parseStringAttrib( EasyNode el, 
                                      String  attribName,
                                      String  defaultVal ) 
        throws QueryGenException
    {
        return parseStringAttrib( el, attribName, true, defaultVal );
    }
    
    /**
     * Locate the named attribute and retrieve its value as a string.
     * Handles default processing if requested.
     * 
     * @param el EasyNode to search
     * @param attribName Attribute to find
     * @param useDefault true to supply a default value if none found,
     *                   false to throw an exception if not found.
     * @param defaultVal If not found and useDefault is true, return this 
     *                   value.
     */
    private String parseStringAttrib( EasyNode el, 
                                      String  attribName, 
                                      boolean useDefault,
                                      String  defaultVal )
        throws QueryGenException
    {
        String elName = el.name();
        String str = el.attrValue( attribName );

        if( str == null ) {
            if( !useDefault )
                error( "'" + elName + "' element must specify '" + 
                       attribName + "' attribute" );
            return defaultVal;
        }
        else if( str.length() == 0 ) {
            if( !useDefault )
                error( "'" + elName + "' element specified empty '" + 
                       attribName + "' attribute" );
            return defaultVal;
        }
        
        return str;
        
    } // parseStringAttrib()
    
    
    /**
     * Exception class used to report errors from the query generator.
     */
    public class QueryFormatError extends GeneralException
    {
        public QueryFormatError( String message ) {
            super( message );
        }
        
        public boolean isSevere() { return false; }
    } // class QueryFormatError

    /** Keeps track of all the queries for a given field */
    private static class QueryEntry
    {
      public Vector  queries = new Vector();
      public Vector  nots    = new Vector();
      public String  field;
      
      public QueryEntry( String field ) {
        this.field = field;
      }
    } // class QueryEntry
    
}
