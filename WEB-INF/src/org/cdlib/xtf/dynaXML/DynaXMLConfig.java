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

import org.cdlib.xtf.servletBase.TextConfig;
import org.cdlib.xtf.util.GeneralException;


/** Holds global configuration information for the dynaXML servlet.  */
class DynaXMLConfig extends TextConfig
{
    /** IP address of the reverse proxy, if any */
    public String  reverseProxyIP;

    /**
     * Name of the special HTTP header used to record the original IP
     * address by the reverse proxy.
     */
    public String  reverseProxyMarker;

    /** The default header to recording the original IP address. */
    public String  reverseProxyDefaultMarker = "X-Forwarded-For";

    /** 
     * Filesystem path to the 'doclookup' stylesheet, used to get info
     * about documents given their docId's.
     */
    public String  docLookupSheet;
    
    /**
     * List of the URL parameters that should be passed through to the
     * docLookup stylesheet.
     */
    public String  docLookupParams;

    /** Max # of doc lookups to cache */
    public int     docLookupCacheSize        = 100;

    /** Max amount of time (seconds) to cache doc lookups */
    public int     docLookupCacheExpire      = 0;
    
    /** Max # of authentication lookups to cache */
    public int     authCacheSize             = 1000;
    
    /** Max amount of time (seconds) to cache authentication lookups */
    public int     authCacheExpire           = 30*60; // 30 minutes
    
    /** Max # of simultaneous external logins */
    public int     loginCacheSize            = 100;
    
    /** Max amount of time (seconds) before login attempt fails */
    public int     loginCacheExpire          = 5*60; // 5 minutes
    
    /** Max # of IP lists to cache */
    public int     ipListCacheSize           = 10;
    
    /** Max amount of time (seconds) before IP list is automatically reloaded */
    public int     ipListCacheExpire         = 15*60; // 15 minutes
    
    /** Whether to print out a stylesheet profile after each request */
    public boolean stylesheetProfiling       = false;

    /**
     * Constructor - Reads and parses the global configuration file (XML) for 
     * the servlet.
     *
     * @param  path                Filesystem path to the config file.
     * @throws DynaXMLException    If a read or parse error occurs.
     */
    public DynaXMLConfig( String path )
        throws GeneralException
    {
        super.read( "dynaXML-config", path );
        
        // Make sure required things were specified.
        requireOrElse( docLookupSheet, 
            "Config file error: docReqParser path not specified" );
        requireOrElse( docLookupParams,
            "Config file error: docReqParser params not specified" );
    }

    /**
     * Called by when a property is encountered in the configuration file. 
     * If we recognize the property we process it here; otherwise, we pass
     * it on to the base class for recognition there.
     */
    public void handleProperty( String tagName, String attrName,
                                String strVal, int intVal )
        throws GeneralException
    {
        boolean bad = false;
        
        if( tagName.equals("reverseProxy") ) {
            if( attrName.equalsIgnoreCase("IP") )
                reverseProxyIP = strVal;
            else if( attrName.equals("marker") )
                reverseProxyMarker = strVal;
            else
                bad = true;
        }

        else if( tagName.equals("reqParserCache") ) {
            if( attrName.equals("size") )
                docLookupCacheSize = intVal;
            else if( attrName.equals("expire") )
                docLookupCacheExpire = intVal;
            else
                bad = true;
        }

        else if( tagName.equals("docReqParser") ) {
            if( attrName.equals("path") )
                docLookupSheet = DynaXML.getRealPath( strVal );
            else if( attrName.equals("params") )
                docLookupParams = strVal;
            else
                bad = true;
        }
        
        else if( tagName.equals("ipListCache") ) {
            if( attrName.equals("size") )
                ipListCacheSize = intVal;
            else if( attrName.equals("expire") )
                ipListCacheExpire = intVal;
            else
                bad = true;
        }

        else if( tagName.equals("authCache") ) {
            if( attrName.equals("size") )
                authCacheSize = intVal;
            else if( attrName.equals("expire") )
                authCacheExpire = intVal;
            else
                bad = true;
        }
        
        else if( tagName.equals("loginCache") ) {
            if( attrName.equals("size") )
                loginCacheSize = intVal;
            else if( attrName.equals("expire") )
                loginCacheExpire = intVal;
            else
                bad = true;
        }
        
        else if( tagName.equals("stylesheetProfiling") ) {
            if( attrName.equals("profile") )
                stylesheetProfiling = strVal.equals("yes") ||
                                      strVal.equals("true") ||
                                      strVal.equals("1");
            else
                bad = true;
        }
            
        else
            super.handleProperty( tagName, attrName, strVal, intVal );
        
        // If we found an element we recognize with an attribute we don't,
        // then barf out.
        //
        if( bad )
            throw new GeneralException( "Config file property " +
                                        tagName + "." + attrName +
                                        " not recognized" );
    } // handleProperty()

} // class DynaXMLConfig


