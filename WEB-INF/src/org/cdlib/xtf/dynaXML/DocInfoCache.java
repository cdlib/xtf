package org.cdlib.xtf.dynaXML;

/**
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
import java.util.Iterator;
import java.util.LinkedList;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;

import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.tree.TreeBuilder;
import net.sf.saxon.value.StringValue;

import org.cdlib.xtf.servletBase.TextServlet;
import org.cdlib.xtf.textEngine.QueryRequest;
import org.cdlib.xtf.util.AttribList;
import org.cdlib.xtf.util.EasyNode;
import org.cdlib.xtf.util.GeneralException;
import org.cdlib.xtf.util.Trace;
import org.cdlib.xtf.util.XMLWriter;
import org.cdlib.xtf.util.XTFSaxonErrorListener;

import org.cdlib.xtf.cache.CacheDependency;
import org.cdlib.xtf.cache.GeneratingCache;

/**
 * This class is used to cache parsed document requests (which map URL 
 * parameters to source, style, authType, etc.)
 */
class DocInfoCache extends GeneratingCache
{
    private DynaXMLConfig  config;
    private DynaXML        servlet;

    private ThreadLocal prevStylesheet  = new ThreadLocal();
    private ThreadLocal prevTransformer = new ThreadLocal();
    
    
    /**
     * Default constructor.
     *
     * @param servlet   Servlet whose caches we are to use
     */
    public DocInfoCache( DynaXML servlet )
    {
        super( ((DynaXMLConfig)servlet.getConfig()).docLookupCacheSize,
               ((DynaXMLConfig)servlet.getConfig()).docLookupCacheExpire );
        this.config  = (DynaXMLConfig) servlet.getConfig();
        this.servlet = servlet;
    }

    /** 
     * Given a document's params, locate the document info. If not currently
     * cached, the docLookup stylesheet is called to do the work.
     *
     * @param  docParams    Parameters specifying the document
     * @throws Exception    If anything goes wrong
     */
    public DocInfo find( LinkedList docParams )
        throws Exception
    {
        return (DocInfo) super.find( docParams );
    }

    /**
     * Called when a lookup isn't (yet) in the cache. Passes the URL parameters
     * to the docReqParser stylesheet, and gathers the result into a
     * {@link DocInfo} structure.
     *
     * @param key           (LinkedList) URL parameters specifying the request
     * @return              (DocInfo) info about that document
     * @throws Exception    If anything goes wrong loading or running
     *                      the docLookup stylesheet.
     */
    protected Object generate( Object key )
        throws Exception
    {
        LinkedList params = (LinkedList) key;
        DocInfo info = new DocInfo();

        // First, load the lookup stylesheet.
        Templates pss = TextServlet.stylesheetCache.find( 
            config.docLookupSheet );
        
        // Running the stylesheet may produce additional dependencies if the
        // sheet reads in extra files dependent on the document ID. To avoid
        // having to throw away the stylesheet entry, we record the current
        // dependencies so we can restore them later.
        // 
        Iterator di = TextServlet.stylesheetCache.getDependencies(
            config.docLookupSheet );
        LinkedList oldStylesheetDeps = new LinkedList();
        while( di.hasNext() )
            oldStylesheetDeps.add( di.next() );

        // Get a transformer to handle this stylesheet. To avoid making the
        // same one over and over, we maintain a thread-local copy of the
        // last one, which will usually be perfectly sufficient.
        //
        Transformer trans;
        if( prevStylesheet.get() == pss ) {
            trans = (Transformer) prevTransformer.get();
            trans.clearParameters();
        }
        else {
            trans = pss.newTransformer();
            //prevStylesheet.set( pss );
            //prevTransformer.set( trans );
        }
        
        // Stuff the transformer full of parameters. Note that the parameters 
        // come only from the config file, not from the specific HTTP request 
        // being processed. 
        //
        AttribList attrList = new AttribList();
        for( Iterator iter = params.iterator(); iter.hasNext(); ) {
            String paramName = (String) iter.next();
            String paramVal  = (String) iter.next();
            trans.setParameter( paramName, new StringValue(paramVal) );
            attrList.put( paramName, paramVal );
        }
        
        // Make sure errors get directed to the right place.
        if( !(trans.getErrorListener() instanceof XTFSaxonErrorListener) )
            trans.setErrorListener( new XTFSaxonErrorListener() );
        
        // Make a document containing the tokenized and untokenized versions
        // of the parameters (typically useful for queries.)
        //
        NodeInfo paramDoc = servlet.tokenizeParams( attrList );

        if( Trace.getOutputLevel() >= Trace.debug ) {
            Trace.debug( "*** docReqParser input ***" );
            Trace.tab();
            Trace.debug( XMLWriter.toString(paramDoc) );
            Trace.untab();
        }
        
        // Now request it to give us the info for this document. The stylesheet
        // might load additional files based on the document ID. Add those
        // dependencies directly to our cache entry, not the stylesheet's.
        //
        TreeBuilder result;
        synchronized( TextServlet.stylesheetCache ) {
            TextServlet.stylesheetCache.setDependencyReceiver( this );
            try {
                result = new TreeBuilder();
                trans.transform( paramDoc, result );
            }
            finally {
                TextServlet.stylesheetCache.setDependencyReceiver( null );
            }
        }

        if( Trace.getOutputLevel() >= Trace.debug ) {
            Trace.debug( "*** docReqParser output ***" );
            Trace.tab();
            Trace.debug( XMLWriter.toString(result.getCurrentRoot()) );
            Trace.untab();
        }
        
        // Also, add a dependency on the stylesheet cache entry.
        addDependency( new CacheDependency(TextServlet.stylesheetCache,
                                           config.docLookupSheet) );

        // Extract the data we need.
        EasyNode root = new EasyNode( result.getCurrentRoot() );
        for( int i = 0; i < root.nChildren(); i++ ) {
            EasyNode el      = root.child( i );
            String   tagName = el.name();

            if( tagName.equals("style") )
                info.style = DynaXML.getRealPath( el.attrValue("path") );
            else if( tagName.equals("source") )
                info.source = DynaXML.getRealPath( el.attrValue("path") );
            else if( tagName.equals("index") ) {
                info.indexConfig = DynaXML.getRealPath( el.attrValue("configPath") );
                info.indexName   = el.attrValue( "name" );
            }
            else if( tagName.equals("brand") )
                info.brand = DynaXML.getRealPath( el.attrValue("path") );
            else if( tagName.equals("auth") )
                info.authSpecs.add( 
                        DynaXML.authenticator.processAuthTag(el) );
            else if( tagName.equals("query") )
                info.query = new QueryRequest( el.getWrappedNode(), 
                                               new File(TextServlet.getRealPath("")) );
            else
                throw new DynaXMLException( "Unknown tag '" + tagName +
                    "' specified by docReqParser" );

        } // for node

        // If no source, assume that means an invalid document ID.
        if( DynaXML.isEmpty(info.source) )
            throw new InvalidDocumentException();

        // Make sure a stylesheet was specified.
        DynaXML.requireOrElse( info.style, 
                               "docReqParser didn't specify 'style'" );
        
        // Index config and index name must be either both specified or both
        // absent.
        //
        if( DynaXML.isEmpty(info.indexConfig) && !DynaXML.isEmpty(info.indexName) )
            throw new GeneralException( "docReqParser specified 'indexName' without 'indexConfig'" );
        if( !DynaXML.isEmpty(info.indexConfig) && DynaXML.isEmpty(info.indexName) )
            throw new GeneralException( "docReqParser specified 'indexConfig' without 'indexName'" );

        // And we're done.
        return info;
    } // generate()


    /** Prints out useful debugging info */
    protected void logAction( String action, Object key, Object value )
    {
        if( Trace.getOutputLevel() < Trace.debug ) 
            return;
        
        StringBuffer buf = new StringBuffer( 100 );
        buf.append( "DocInfoCache: " + action + "." );
        for( Iterator iter = ((LinkedList)key).iterator(); iter.hasNext(); ) {
            String paramName = (String) iter.next();
            String paramVal  = (String) iter.next();
            buf.append( " " + paramName + "=\"" + paramVal + "\"" );
        }
        Trace.debug( buf.toString() );
    } // logAction()

} // class DocInfoCache
