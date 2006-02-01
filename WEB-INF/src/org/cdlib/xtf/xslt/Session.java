package org.cdlib.xtf.xslt;

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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.cdlib.xtf.servletBase.TextServlet;

import net.sf.saxon.expr.XPathContext;

/**
 * Utility functions to store and access variables in the user's session. 
 * Also provides functions that can be called to check whether session 
 * tracking is enabled, and map URLs.
 * 
 * @author Martin Haye
 */
public class Session
{
  /** Checks whether session tracking was enabled in the servlet config */
  public static boolean isEnabled( XPathContext context )
  {
    return TextServlet.getCurServlet().isSessionTrackingEnabled();
  } // isEnabled()
  
  /** Function to get a data from a session variable. */
  public static String getData( XPathContext context, String name )
  {
    HttpServletRequest req = TextServlet.getCurRequest();
    
    HttpSession session = req.getSession( false );
    if( session == null )
        return null;
    
    String val = (String) session.getAttribute( name );
    if( val == null )
        return null;
    
    return val;
  } // getData()
  
  /** Function to put data into a session variable. */
  public static void setData( XPathContext context, String name, String value )
  {
    // Make sure session tracking is enabled in the servlet.
    if( !TextServlet.getCurServlet().isSessionTrackingEnabled() ) {
        throw new RuntimeException( 
            "Error: session tracking must be enabled in servlet config file " +
            "before storing session data" );
    }
    
    // Now store the value.
    HttpServletRequest req = TextServlet.getCurRequest();
    HttpSession session = req.getSession( true );
    String oldVal = (String) session.getAttribute( name );
    session.setAttribute( name, value );
  } // setData()
  
  /** Function to encode a URL, adding session ID if necessary. */
  public static String encodeURL( XPathContext context, String origURL )
  {
    HttpServletResponse res = TextServlet.getCurResponse();
    String mappedURL = res.encodeURL( origURL );
    return mappedURL;
  } // encodeURL()

} // class Session
