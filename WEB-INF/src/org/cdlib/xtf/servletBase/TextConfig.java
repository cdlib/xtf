package org.cdlib.xtf.servletBase;

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
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.Configuration;
import net.sf.saxon.om.AllElementStripper;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.tinytree.TinyBuilder;
import net.sf.saxon.trans.XPathException;

import org.cdlib.xtf.util.*;


/** Common members and methods for servlet configuration classes */
public abstract class TextConfig
{
    /** Servlet we are part of */
    public TextServlet servlet;
    
    /** Logging level: "silent", "errors", "warnings", "info", or "debug" */
    public String  logLevel = "info";

    /** Max # of stylesheets to cache */
    public int     stylesheetCacheSize       = 10;

    /** Max length of time (in seconds) to cache a stylesheet. */
    public int     stylesheetCacheExpire     = 0;

    /**
     * Filesystem path to a stylesheet used to generate error pages
     * (no permission, invalid document, general exceptions, etc.)
     */
    public String  errorGenSheet;

    /** 
     * Turns on dependency checking for the caches, so that (for instance) 
     * changing a stylesheet forces it to be reloaded.
     */
    public boolean dependencyCheckingEnabled = true;

    /**
     * Turns on latency reporting for the servlet.
     */
    public boolean reportLatency = false;
    
    /**
     * Enables a cutoff size for latency reporting (if {@link #reportLatency} 
     * is true.) Specifies the number of bytes after which a latency message 
     * will be printed, even if the output is not complete.
     */
    public int latencyCutoffSize = 0;

    /** 
     * Amount of time (in seconds) that a request is allowed to run
     * before we consider it a possible "runaway" and start logging warning
     * messages. Default: 10 seconds
     */
    public long runawayNormalTime = 0;
    
    /** 
     * Amount of time (in seconds) after which a request should 
     * voluntarily kill itself. Default: 5 minutes (300 seconds)
     */
    public long runawayKillTime = 0;
    
    /** Whether session tracking is enabled. Default: false */
    public boolean trackSessions = false;
    
    /** 
     * Which URLs to apply encoding to, if session tracking enabled and
     * user doesn't allow cookies.
     */
    public Pattern sessionEncodeURLPattern = null;
    
    /** All the configuration attributes in the form of name/value pairs */
    public AttribList attribs = new AttribList();
    
    /** Configuration used for parsing XML files */
    private Configuration config = new Configuration();
    
    /** Create a configuration and attach it to a servlet */
    public TextConfig( TextServlet servlet ) {
        this.servlet = servlet;
    }

    /**
     * Constructor - Reads and parses the global configuration file (XML) for 
     * the servlet.
     *
     * @param  path                Filesystem path to the config file.
     */
    public void read( String expectedRootTag, String path )
        throws GeneralException
    {
        try {
            // Read in the document (it's in XML format)
            StreamSource src = new StreamSource( new File(path) );
            NodeInfo doc = null;
            try {
                doc = TinyBuilder.build( src, new AllElementStripper(), config );
            }
            catch( XPathException e ) {
                throw new RuntimeException( e );
            }
            
            // Make sure the root tag is correct.
            EasyNode root = new EasyNode( doc );
            String rootTag = root.name();
            if( rootTag.equals("") && root.nChildren() == 1 ) {
                root = root.child( 0 );
                rootTag = root.name();
            }
            
            if( !rootTag.equals(expectedRootTag) )
                throw new GeneralException( "Config file \"" + path + "\" " +
                        "should contain <" + expectedRootTag + ">" );
            
            // Pick out the elements
            for( int i = 0; i < root.nChildren(); i++ ) {
                EasyNode el = root.child( i );
                if( !el.isElement() )
                    continue;

                String tagName = el.name();
                
                // Scan each attribute of each element.
                for( int j = 0; j < el.nAttrs(); j++ ) {
                    String attrName = el.attrName( j );
                    String strVal  = el.attrValue( j );

                    // See if we recognize the property (if so, handle it).
                    if( !handleProperty(tagName + "." + attrName, strVal) ) 
                    {
                        // It is perfectly acceptable (and often necessary) for
                        // the user to define pass-through tags. Somehow this got
                        // forgotten in the onslaught of new features. So, if we
                        // don't recognize the tag, just pass it by.
                        //
                      
                        ; // do nothing
                    }

                    // Also put this tag in the attribute list so it can be
                    // passed to the stylesheets.
                    attribs.put( tagName + "." + attrName, strVal );
                }
                

            } // for i
        }
        catch( Exception e ) {
            throw new GeneralException( "Error reading config file " + path +
                ": " + e );
        }

        // Make sure that required parameters were specified.
        requireOrElse( errorGenSheet,
                       "Config file error: errorGen stylesheet not specified" );
    } // constructor

    /**
     * Called when a property is encountered. Derived classes, if they don't
     * recognize the property, should call the base class version of
     * handleProperty().
     * 
     * @param tagAttr   Combined element/attribute name being considered
     * @param strVal    It's string value
     * @return          true if handled, false if unrecognized
     */
    protected boolean handleProperty( String tagAttr, String strVal )
    {
        if( tagAttr.equalsIgnoreCase("logging.level") ) {
            if( strVal.equals("0") )
                strVal = "silent";
            else if( strVal.equals("1") )
                strVal = "info";
            else if( strVal.equals("2") )
                strVal = "debug";
            logLevel = strVal;
            return true;
        }

        else if( tagAttr.equalsIgnoreCase("stylesheetCache.size") ) {
            stylesheetCacheSize = parseInt( tagAttr, strVal );
            return true;
        }
        else if( tagAttr.equalsIgnoreCase("stylesheetCache.expire") ) {
            stylesheetCacheExpire = parseInt( tagAttr, strVal );
            return true;
        }
        
        else if( tagAttr.equalsIgnoreCase("errorGen.path") ) {
            errorGenSheet = servlet.getRealPath( strVal );
            return true;
        }

        else if( tagAttr.equalsIgnoreCase("dependencyChecking.check") ) {
            dependencyCheckingEnabled = parseBoolean( tagAttr, strVal );
            return true;
        }
            
        else if( tagAttr.equalsIgnoreCase("reportLatency.report") ||
                tagAttr.equalsIgnoreCase("reportLatency.enable") /* old, for backward compat */ )
        {
            reportLatency = parseBoolean( tagAttr, strVal );
            return true;
        }
        else if( tagAttr.equalsIgnoreCase("reportLatency.cutoffSize") ) {
            latencyCutoffSize = parseInt( tagAttr, strVal );
            return true;
        }
            
        else if( tagAttr.equalsIgnoreCase("runawayTimer.normalTime") ) {
            runawayNormalTime = parseInt( tagAttr, strVal );
            return true;
        }
        else if( tagAttr.equalsIgnoreCase("runawayTimer.killTime") ) {
            runawayKillTime = parseInt( tagAttr, strVal );
            return true;
        }
            
        else if( tagAttr.equalsIgnoreCase("trackSessions.track") ) {
            trackSessions = parseBoolean( tagAttr, strVal );
            return true;
        }
        
        else if( tagAttr.equalsIgnoreCase("trackSessions.encodeURLPattern") ) {
            try {
                sessionEncodeURLPattern = Pattern.compile( strVal );
            }
            catch( PatternSyntaxException e ) {
                throw new GeneralException( "Config file property " +
                    tagAttr + " must be a valid regular expression" );
                
            }
            return true;
        }

        // Not recognized.
        return false;
    } // handleProperty()
    
    
    /** 
     * Utility function - parse an integer value. If a valid integer isn't
     * specified, throws an exception.
     *
     * @param tagAttr   Name of the element/attribute being considered
     * @param strVal    It's string value
     */
    public static int parseInt( String tagAttr, String strVal )
        throws GeneralException
    {
        try {
            return Integer.parseInt( strVal );
        }
        catch( NumberFormatException e ) {
            throw new GeneralException( "Integer value expected for " + tagAttr );
        }
    } // parseBoolean()

    /** 
     * Utility function - parse a boolean value. Allows "true", "false",
     * "yes", "no", "1", or "0". If not one of these, throws an exception.
     *
     * @param tagAttr   Name of the element/attribute being considered
     * @param strVal    It's string value
     */
    public static boolean parseBoolean( String tagAttr, 
                                        String strVal )
        throws GeneralException
    {
        if( strVal.equalsIgnoreCase("true") ||
            strVal.equalsIgnoreCase("yes") ||
            strVal.equals("1") )
        {
            return true;
        }
        
        if( strVal.equalsIgnoreCase("false") ||
            strVal.equalsIgnoreCase("no") ||
            strVal.equals("0") )
        {
            return false;
        }
            
        throw new GeneralException( "Boolean value expected for " +
                      tagAttr + " (value must be one of: " +
                      "'true', 'false', 'yes', 'no', '1', or '0')" );
        
    } // parseBoolean()

    
    /** 
     * Utility function - if the value is empty, throws an exception.
     *
     * @param value     Value to check for null or ""
     * @param descrip   If thrown, the exception uses this as the message.
     */
    public static void requireOrElse( String value, String descrip )
        throws GeneralException
    {
        if( value == null || value.equals("") )
            throw new GeneralException( descrip );
    } // requireOrElse()

} // class TextConfig


