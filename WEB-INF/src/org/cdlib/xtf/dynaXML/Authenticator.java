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

import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Vector;

import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.cdlib.xtf.cache.*;
import org.cdlib.xtf.util.*;

/**
 * Performs all authentication tasks for the servlet, including IP-based,
 * LDAP, and external authentication.
 */
class Authenticator
{
    /** Used for generating random nonce values */
    private SecureRandom secureRandom = new SecureRandom();

    /** Caches IP maps */
    private IpListCache ipListCache;

    /** Caches authorized session IDs */
    private StringCache authCache;

    /** Caches nonce values for external log-ins */
    private StringCache loginCache;
    
    /** Servlet to get dependencies from */
    private DynaXML servlet;
    
    /** Configuration info */
    private DynaXMLConfig config;
    
    /** 
     * Construct an authorizer, initializing all the caches.
     *
     * @param servlet   Servlet whose cache we will access
     */
    public Authenticator( DynaXML servlet )
    {
        this.servlet = servlet;
        this.config  = (DynaXMLConfig) servlet.getConfig();
        
        authCache = new StringCache( "AuthCache",
            config.authCacheSize, config.authCacheExpire );

        loginCache = new StringCache( "LoginCache",
            config.loginCacheSize, config.loginCacheExpire );

        ipListCache = new IpListCache( 
            config.ipListCacheSize, config.ipListCacheExpire,
            config.dependencyCheckingEnabled );

    } // constructor


    /** Utility method to check if a string is null or "" */
    private boolean isEmpty( String s)
    {
        return ( s == null || s.equals("") );
    } // isEmpty()


    /** 
     * Creates an AuthSpec from an 'auth' element produced by the docReqParser
     * stylesheet. Parses the various parameters depending on the type.
     */
    public AuthSpec processAuthTag( Element el )
        throws DynaXMLException
    {
        AuthSpec spec = null;

        // Output extended debugging info if requested.
        if( Trace.getOutputLevel() >= Trace.debug )
        {
            StringBuffer buf = new StringBuffer();

            NamedNodeMap map = el.getAttributes();
            for( int i = 0; i < map.getLength(); i++ ) {
                Node item = map.item( i );
                if( item.getNodeType() != Node.ATTRIBUTE_NODE )
                    continue;
                String name = item.getNodeName();

                buf.append( name + "=" + el.getAttribute(name) + " " );
            }

            Trace.debug( "Processing auth spec: " + buf.toString() );
        }

        // Make sure a type is specified
        if( !el.hasAttribute("type") )
            throw new DynaXMLException( 
                "Auth type not specified by docReqParserSheet" );

        // Look for different parameters, depending on the type.
        String type = el.getAttribute( "type" );
        if( type.equals("all") ) {
            spec = new AllAuthSpec();
            spec.type = AuthSpec.TYPE_ALL;
        }
        else if( type.equals("IP") ) {
            spec = new IPAuthSpec();
            spec.type = AuthSpec.TYPE_IP;

            if( !el.hasAttribute("list") )
                throw new DynaXMLException( 
                    "Auth IP 'list' not specified by docReqParserSheet" );
            ((IPAuthSpec)spec).ipList = 
                DynaXML.getRealPath( el.getAttribute("list") );
        }
        else if( type.equals("LDAP") ) {
            LdapAuthSpec lspec = new LdapAuthSpec();
            spec = lspec;
            spec.type = AuthSpec.TYPE_LDAP;

            lspec.realm        = el.getAttribute( "realm" );
            lspec.server       = el.getAttribute( "server" );
            lspec.bindName     = el.getAttribute( "bindName" );
            lspec.bindPassword = el.getAttribute( "bindPassword" );
            lspec.queryName    = el.getAttribute( "queryName" );
            lspec.matchField   = el.getAttribute( "matchField" );
            lspec.matchValue   = el.getAttribute( "matchValue" );

            if( isEmpty(lspec.server) )
                throw new DynaXMLException(
                    "LDAP server not specified by docReqParserSheet" );

            if( isEmpty(lspec.queryName) )
                throw new DynaXMLException(
                    "LDAP queryName not specified by docReqParserSheet" );

            if( ( isEmpty(lspec.matchField) &&
                 !isEmpty(lspec.matchValue))
             || (!isEmpty(lspec.matchField) &&
                  isEmpty(lspec.matchValue)) )
            {
                throw new DynaXMLException(
                    "LDAP matchField and matchValue must be either " +
                    "both present or both absent in docReqParserSheet." );
            }

            if( isEmpty(lspec.bindName) &&
                isEmpty(lspec.matchField) )
            {
                throw new DynaXMLException(
                    "Either LDAP bindName or matchField must be " +
                    "specified by docReqParserSheet." );
            }
        }
        else if( type.equals("external") ) {
            ExternalAuthSpec espec = new ExternalAuthSpec();
            spec = espec;
            spec.type = AuthSpec.TYPE_EXTERNAL;

            espec.url       = el.getAttribute( "url" );
            espec.secretKey = el.getAttribute( "key" );

            if( isEmpty(espec.url) )
                throw new DynaXMLException(
                    "External authorization page url not specified " +
                    "by docReqParserSheet" );
            if( isEmpty(espec.secretKey) )
                throw new DynaXMLException(
                    "External authorization key (secret) not specified " +
                    "by docReqParserSheet" );
        }
        else
            throw new DynaXMLException( "Invalid auth type '" + type +
                "' specified by docReqParserSheet" );

        // Make sure an access mode (allow or deny) has been specified.
        if( !el.hasAttribute("access") )
            throw new DynaXMLException( "Auth access (allow or deny) " +
                "must be specified by docReqParserSheet" );

        // Record it.
        String access = el.getAttribute( "access" );
        if( access.equals("allow") )
            spec.access = AuthSpec.ACCESS_ALLOW;
        else if( access.equals("deny") )
            spec.access = AuthSpec.ACCESS_DENY;
        else
            throw new DynaXMLException( "Invalid access '" + access +
                "' specified by docReqParser" );

        // And we're done.
        return spec;
    } // processAuthTag()


    /** Clears all the caches used by the authenticator. */
    public void clearCaches()
    {
        ipListCache.clear();
        authCache.clear();
        loginCache.clear();
    } // clearCaches()


    /**
     * Uses an LDAP server to authorize user access with a username and
     * password. Name and password are gathered using the HTTP 'basic'
     * authentication mechanism.
     *
     * @param docKey  Cache key for the document being processed
     * @param spec  The authorization spec containing details (server to
     *              connect to, what to look up, etc.)
     * @param req   The HTTP request (contains username and password)
     * @param res   The HTTP response (only used to re-request user auth)
     *
     * @throws NoPermissionException
     *         If permission isn't granted, or the browser must re-validate
     *         the password.
     * @throws Exception
     *         Communication or other miscellaneous problems.
     */
    private void authLdap( LinkedList          docKey,
                           LdapAuthSpec        spec, 
                           HttpServletRequest  req,
                           HttpServletResponse res )
        throws Exception
    {
        // If we've already authorized this session, no need to do more.
        HttpSession session = req.getSession();
        String sessionID = req.getSession().getId();
        String authCacheKey = sessionID + 
                              ":LDAP" +
                              ":" + spec.server +
                              ":" + spec.bindName +
                              ":" + spec.queryName +
                              ":" + spec.matchField +
                              ":" + spec.matchValue;
        if( authCache.has(authCacheKey) )
            return;

        // Figure out the "realm" string. This displays in the dialog box
        // the user's browser presents when asking for username/password.
        //
        String realm = isEmpty(spec.realm) ? "dynaXML" : spec.realm;

        // The first time we see a new session, force the browser to re-request
        // the password from the user.
        //
        if( session.getAttribute("LDAP_attempted") == null )
        {
            session.setAttribute( "LDAP_attempted", new Boolean(true) );
            Trace.debug( "New session (" + session.getId() + ")... " + 
                "forcing re-authentication" );
            res.addHeader( "WWW-Authenticate",
                "Basic realm=\"" + realm + "\"" );
            res.setStatus( HttpServletResponse.SC_UNAUTHORIZED );
            throw new NoPermissionException();
        }
        else
            session.removeAttribute( "LDAP_attempted" );

        // If the HTTP header has a user name and password in it (via the
        // "Authorization" header), then pick them out.
        //
        String userName = "";
        String password = "";
        String auth = req.getHeader( "Authorization" );
        if( auth != null ) {
            int spacePos = auth.indexOf( ' ' );
            if( spacePos >= 0 ) {
                String method = auth.substring( 0, spacePos );
                String stuff  = auth.substring( spacePos+1 );
                if( method.equals("Basic") ) {
                    String decoded = Base64.decodeString( stuff );
                    int colonPos = decoded.indexOf( ':' );
                    if( colonPos >= 0 ) {
                        userName = decoded.substring( 0, colonPos );
                        password = decoded.substring( colonPos + 1 );
                    }
                }
            }
        }

        // Make a temporary spec that's a copy with the substituted values.
        LdapAuthSpec oldSpec = spec;
        spec = new LdapAuthSpec();
        spec.realm        = oldSpec.realm;
        spec.server       = oldSpec.server;
        spec.bindName     = oldSpec.bindName.replaceAll( "\\%", userName );
        spec.bindPassword = oldSpec.bindPassword.replaceAll( "\\%", password );
        spec.queryName    = oldSpec.queryName.replaceAll( "\\%", userName );
        spec.matchField   = oldSpec.matchField;
        spec.matchValue   = oldSpec.matchValue.replaceAll( "\\%", password );

        // Set up the environment variables for connecting to the LDAP server.
        Hashtable env = new Hashtable();
        env.put( javax.naming.Context.INITIAL_CONTEXT_FACTORY,
                 "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( javax.naming.Context.PROVIDER_URL,
                 spec.server );

        // Fill in the security parameters for the LDAP bind.
        env.put( javax.naming.Context.SECURITY_AUTHENTICATION, "simple" );
        if( !isEmpty(spec.bindName) )
            env.put( javax.naming.Context.SECURITY_PRINCIPAL, spec.bindName );
        if( !isEmpty(spec.bindPassword) )
            env.put( javax.naming.Context.SECURITY_CREDENTIALS, 
                spec.bindPassword );

        try
        {
            // Now try to connect to the LDAP server and look up the entry.
            // If these fail an exception will be thrown (caught below).
            //
            DirContext ctx = new InitialDirContext( env );
            javax.naming.directory.Attributes attribs;
            attribs = ctx.getAttributes( spec.queryName );

            // If we got no attributes, access denied.
            if( attribs.size() == 0 )
                throw new NoPermissionException();

            // If a 'matchField' was specified, look for it.
            if( !isEmpty(spec.matchField) ) {

                Attribute attrib = attribs.get( spec.matchField );
                if( attrib == null ) {
                    Trace.warning( "[sensitive] LDAP: Cannot find field '" + 
                        spec.matchField + "'" );
                    throw new NoPermissionException();
                }

                // Check that the value matches.
                if( !attrib.contains(spec.matchValue) ) {
                    Trace.warning( "[sensitive] " +
                        "LDAP: Cannot match value '" + 
                        spec.matchValue +
                        "' for field '" + 
                        spec.matchField + "'" );
                    throw new NoPermissionException();
                } // if
            } // if

        } // try
        catch( Exception e )
        {
            // Output lots of log info to help find deployment problems.
            Trace.warning( "[sensitive] LDAP authentication failure: " + 
                e.getClass().getName() + " - " + e.getMessage() + ". " +
                "server='"       + spec.server + "', " +
                "bindName='"     + spec.bindName + "', " +
                "bindPassword='" + spec.bindPassword + "', " +
                "queryName='"    + spec.queryName + "', " +
                "matchField='"   + spec.matchField + "', " +
                "matchValue='"   + spec.matchValue + "'" );

            // Notify the user's browser to ask for a username and password.
            res.addHeader( "WWW-Authenticate",
                "Basic realm=\"" + realm + "\"" );
            res.setStatus( 
                HttpServletResponse.SC_UNAUTHORIZED );
            
            // Don't force re-re-authentication.
            session.setAttribute( "LDAP_attempted", new Boolean(true) );

            throw new NoPermissionException( e );
        }

        // Record that this session has been authorized. Make it dependent
        // on the docInfo entry, so it will be invalidated if the auth info
        // for this document changes.
        //
        authCache.set( authCacheKey, "LDAP", servlet.getDocDependency(docKey) );
        
    } // authLdap()


    /**
     * Uses an external login web page to authorize user access. Redirects
     * to an external login page and sends a 'nonce' value along with the
     * redirect. Eventually the login gets back to our page with an encrypted
     * version of the nonce so we can prevent spurious returns.
     *
     * @param docKey Cache key for the document being processed
     * @param spec  The authorization spec containing URL to contact.
     * @param req   The HTTP request (contains nonce when we get the return
     *              from the authorization page).
     * @param res   The HTTP response
     *
     * @return      true if granted, false if redirected
     *
     * @throws NoPermissionException     
     *         If permission isn't granted.
     * @throws Exception                
     *         For miscellaneous problems.
     */
    private boolean authExternal( LinkedList          docKey,
                                  ExternalAuthSpec    spec, 
                                  HttpServletRequest  req,
                                  HttpServletResponse res )
        throws Exception
    {
        // If we've already authorized this session, no need to do more.
        HttpSession session = req.getSession();
        String sessionID = req.getSession().getId();
        String authCacheKey = sessionID + ":ext:" + spec.url;
        if( authCache.has(authCacheKey) )
            return true;

        // If this is the return response from the authorization page,
        // validate it.
        //
        if( req.getParameter("hash" ) != null && 
            req.getParameter("nonce") != null )
        {
            String nonce = req.getParameter("nonce");
            String hash  = req.getParameter("hash");

            // Ensure that the nonce is still valid.
            if( sessionID.equals(loginCache.get(nonce)) ) {

                // Invalidate this nonce so it cannot be used in a replay 
                // attack.
                //
                loginCache.remove( nonce );

                // Compute our own hash of the nonce and the key.
                String strToHash = nonce + ":" + spec.secretKey;
                MessageDigest digest = MessageDigest.getInstance( "MD5" );
                for( int i = 0; i < strToHash.length(); i++ )
                    digest.update( (byte)strToHash.charAt(i) );
                String compHash = bytesToHex( digest.digest() );

                // If it's the same, the authentication is successful.
                if( compHash.equals(hash) ) {

                    // Record that this session has been authorized. Make it 
                    // dependent on the docInfo entry, so it will be 
                    // invalidated if the auth info for this document changes.
                    //
                    authCache.set( authCacheKey, "ext", 
                        servlet.getDocDependency(docKey) );

                    // Now redirect to the original URL, minus all the
                    // authorization stuff.
                    //
                    res.addHeader( "Cache-Control", "no-cache" );
                    res.sendRedirect( 
                        (String)session.getAttribute("Ext_orig_url") );
                    res.addHeader( "Cache-Control", "no-cache" );
                    return false;
                }

                // Otherwise, we better try again.
                Trace.warning( 
                    "Hash " + hash + " doesn't match calc'd " + compHash );
            } // if
            else if( loginCache.has(nonce) ) {
                Trace.warning( 
                    "Bad external session: " + loginCache.get(nonce) +
                    " turned into " + sessionID );
                loginCache.remove( nonce );
            }
        } // if

        // Okay, the user needs to log in using the external authentication
        // web page. First, construct the URL that will return to this page.
        // It should be the root URL, plus all parameters except a leftover
        // 'nonce' or 'hash' from a failed attempt.
        //
        StringBuffer returnUrl = req.getRequestURL();
        Enumeration e = req.getParameterNames();
        boolean first = true;
        while( e.hasMoreElements() ) {
            String name = (String)e.nextElement();
            if( name.equals("nonce") || name.equals("hash") )
                continue;
            returnUrl.append( first ? "?" : "&" );
            returnUrl.append( name + "=" + req.getParameter(name) );
            first = false;
        }

        // Store this (without all the session, nonce, etc. stuff) as the
        // URL to go to when authorization is all finished.
        //
        session.setAttribute( "Ext_orig_url", returnUrl.toString() );

        // Create a random nonce that the validator will need to encode.
        byte[] bytes = new byte[16];
        secureRandom.nextBytes( bytes );
        String nonce = bytesToHex( bytes );

        // Put the nonce in a cache that will time it out eventually.
        loginCache.set( nonce, sessionID );

        // Now construct the final redirect URL.
        StringBuffer finalUrl = new StringBuffer( spec.url );
        finalUrl.append( "?returnto=" + 
                         URLEncoder.encode(returnUrl.toString(), "UTF-8") );
        finalUrl.append( "&nonce=" + nonce );

        // Redirect the client to that location.
        res.addHeader( "Cache-Control", "no-cache" );
        res.sendRedirect( finalUrl.toString() );
        res.addHeader( "Cache-Control", "no-cache" );
        return false;

    } // authExternal()

    
    /**
     * Based on a list of authentication specifications, checks if the
     * current session is allowed to access this document. Handles IP-based,
     * LDAP, and external authentication methods.
     *
     * @param docKey    Cache key for the document being accessed
     * @param ipAddr    Real IP address of the requestor
     * @param authSpecs List of authentication specifications (allow/deny),
     *                  processed in order.
     * @param req       The HTTP request that was made
     * @param res       The HTTP response being generated
     *
     * @return          true if ok, false to redirect.
     *
     * @throws NoPermissionException     
     *         Authentication failed
     * @throws Exception                 
     *         Miscellaneous problems
     */
    public boolean checkAuth( LinkedList          docKey,
                              String              ipAddr, 
                              Vector              authSpecs,
                              HttpServletRequest  req,
                              HttpServletResponse res )
        throws Exception
    {
        // See if the requestor has permission to access this document.
        boolean allow = false;
        for( int i = 0; !allow && i < authSpecs.size(); i++ ) {
            AuthSpec spec = (AuthSpec) authSpecs.get( i );
            switch( spec.type ) {
                case AuthSpec.TYPE_ALL:
                    if( spec.access == AuthSpec.ACCESS_ALLOW ) {
                        Trace.debug( "Auth allow all" );
                        allow = true;
                    }
                    else {
                        Trace.debug( "Auth deny all" );
                        throw new NoPermissionException( ipAddr );
                    }
                    break;

                case AuthSpec.TYPE_IP:
                    IpList ipList = ipListCache.find( 
                        ((IPAuthSpec)spec).ipList );
                    boolean onList = ipList.isApproved( ipAddr );
                    if( spec.access == AuthSpec.ACCESS_ALLOW ) 
                    {
                        Trace.debug( "Auth allow IP " + ipAddr +
                            ": on-list=" + (onList ? "yes" : "no") );
                        if( onList )
                            allow = true;
                    }
                    else {
                        Trace.debug( "Auth deny IP " + ipAddr +
                            ": on-list=" + (onList ? "yes" : "no") );
                        if( onList )
                            throw new NoPermissionException( ipAddr );
                    }
                    break;

                case AuthSpec.TYPE_LDAP:
                    if( spec.access == AuthSpec.ACCESS_DENY )
                        throw new NoPermissionException( ipAddr );

                    authLdap( docKey, (LdapAuthSpec)spec, req, res );
                    Trace.debug( "Auth LDAP: ok" );
                    allow = true;
                    break;

                case AuthSpec.TYPE_EXTERNAL:
                    if( spec.access == AuthSpec.ACCESS_DENY )
                        throw new NoPermissionException( ipAddr );

                    if( authExternal( docKey, (ExternalAuthSpec)spec, 
                                      req, res ) )
                    {
                        Trace.debug( "Auth external: yes" );
                        allow = true;
                    }
                    else {
                        Trace.debug( "Auth external: no" );
                        return false;
                    }
                    break;

                default:
                    throw new DynaXMLException( "Internal error" );
            }
        }

        if( !allow )
            throw new NoPermissionException( ipAddr );

        return true;

    } // checkAuth()


    /**
     * Converts an array of bytes to the hex representation of them, two
     * digits per byte and no spaces.
     *
     * @param bytes     An array of bytes to convert
     * @return          A long string representing those bytes in hex form
     */
    private static String bytesToHex( byte[] bytes )
    {
        StringBuffer buf = new StringBuffer();
        for( int i = 0; i < bytes.length; i++ ) {
            int n = (int)(bytes[i]) & 0xff;
            if( n < 16 )
                buf.append( "0" );
            buf.append( Integer.toHexString(n) );
        }
        return buf.toString();
    } // bytesToHex()


    /** Holds information on a particular authorization specification */
    private class AuthSpec
    {
        public static final int ACCESS_DENY  = 0;
        public static final int ACCESS_ALLOW = 1;

        public static final int TYPE_ALL      = 0;
        public static final int TYPE_IP       = 1;
        public static final int TYPE_LDAP     = 2;
        public static final int TYPE_EXTERNAL = 3;
    
        int    access;
        int    type;
    }

    /** Allow or deny all access */
    private class AllAuthSpec extends AuthSpec
    {
    }

    /** 
     * Allow or deny based on whether requestor's IP address is in the
     * specified list.
     */
    private class IPAuthSpec extends AuthSpec
    {
        String ipList;
    }

    /** Allow or deny based on looking up an entry in an LDAP database. */
    private class LdapAuthSpec extends AuthSpec
    {
        String realm;
        String server;
        String bindName;
        String bindPassword;
        String queryName;
        String matchField;
        String matchValue;
    }

    /** Allow or deny based on an external login page */
    private class ExternalAuthSpec extends AuthSpec
    {
        String url;
        String secretKey;
    }

    /**
     * This class is used to cache IP maps so we don't have to load the
     * same ones over and over.
     */
    private class IpListCache extends GeneratingCache
    {
        private boolean dependencyChecking;
        
        /** Constructor - initializes the cache */
        public IpListCache( int maxEntries, int maxTime,
                            boolean dependencyChecking )
        {
            super( maxEntries, maxTime );
            this.dependencyChecking = dependencyChecking;
        }

        /** 
         * Locate the IP list for the given path.
         *
         * @param path          The full filesystem path of the IP list to 
         *                      load.
         * @throws Exception     If not found or invalid format
         */
        public IpList find( String path )
            throws Exception
        {
            return (IpList) super.find( path );
        }


        /**
         * Load an IP list from the filesystem.
         * @param key       Full path of the file to load
         * @throws Exception If not found or bad format.
         */
        protected Object generate( Object key )
            throws Exception
        {
            String path = (String)key;
            if( dependencyChecking )
                addDependency( new FileDependency(path) );

            // Try to load it. On failure, it will throw an exception (which
            // will be handled by doGet().
            //
            return new IpList( path );
        } // generate()

        /** Prints out useful debugging info */
        protected void logAction( String action, Object key, Object value )
        {
            Trace.warning( "IpListCache: " + action + 
                           ". Path=" + (String)key );
        } // logAction()

    } // class IpListCache

} // class Authenticator

