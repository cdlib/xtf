package org.cdlib.xtf.test;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.cdlib.xtf.util.Attrib;
import org.cdlib.xtf.util.AttribList;

/**
 * A synthetic servlet request, useful when calling dynaXML or crossQuery
 * programatically.
 * 
 * @author Martin Haye
 */
public class FakeServletRequest implements HttpServletRequest
{
  private String     url;
  private AttribList params = new AttribList();
  
  public FakeServletRequest( String url ) {
    url = url.replaceAll( "&amp;", "&" );
    this.url = url;
    StringTokenizer tok1 = new StringTokenizer( url, "?&" );
    tok1.nextToken();
    while( tok1.hasMoreTokens() ) {
        StringTokenizer tok2 = new StringTokenizer( tok1.nextToken(), "=" );
        String name  = tok2.nextToken();
        String value = (tok2.hasMoreTokens()) ? tok2.nextToken() : "";
        params.put( name, decodeHtml(value) );
    }
  }
  
  private String decodeHtml( String in )
  {
      StringBuffer buf = new StringBuffer( in.length() * 2 );
      for( int i = 0; i < in.length(); i++ ) {
          char c = in.charAt( i );
          if( c == '+' )
              buf.append( ' ' );
          else if( c == '%' ) {
              char[] both = new char[2];
              both[0] = in.charAt( ++i );
              both[1] = in.charAt( ++i );
              c = (char) Integer.parseInt( new String(both), 16 );
              buf.append( c );
          }
          else
              buf.append( c );
      }
      
      return buf.toString();
  } // decodeHtml

  public String getAuthType()
  {
    assert false;
    return null;
  }
  public String getContextPath()
  {
    assert false;
    return null;
  }
  public Cookie[] getCookies()
  {
    assert false;
    return null;
  }
  public long getDateHeader( String name )
  {
    assert false;
    return 0;
  }
  public String getHeader( String name )
  {
    assert false;
    return null;
  }
  public Enumeration getHeaderNames()
  {
    return new Enumeration() { 
      public boolean hasMoreElements() { return false; }
      public Object nextElement() { assert false; return null; }
    };
  }
  public Enumeration getHeaders( String name )
  {
    assert false;
    return null;
  }
  public int getIntHeader( String name )
  {
    assert false;
    return 0;
  }
  public String getMethod()
  {
    assert false;
    return null;
  }
  public String getPathInfo()
  {
    assert false;
    return null;
  }
  public String getPathTranslated()
  {
    assert false;
    return null;
  }
  public String getQueryString()
  {
    StringBuffer buf = new StringBuffer();
    for( Iterator iter = params.iterator(); iter.hasNext(); ) {
        Attrib att = (Attrib) iter.next();
        if( buf.length() > 0 )
            buf.append( '&' );
        buf.append( att.key + "=" + att.value );
    }
    return buf.toString();
  }
  public String getRemoteUser()
  {
    assert false;
    return null;
  }
  public String getRequestedSessionId()
  {
    assert false;
    return null;
  }
  public String getRequestURI()
  {
    return url;
  }
  public StringBuffer getRequestURL()
  {
    return new StringBuffer( url );
  }
  public String getServletPath()
  {
    assert false;
    return null;
  }
  public HttpSession getSession()
  {
    assert false;
    return null;
  }
  public HttpSession getSession( boolean create )
  {
    assert false;
    return null;
  }
  public Principal getUserPrincipal()
  {
    assert false;
    return null;
  }
  public boolean isRequestedSessionIdFromCookie()
  {
    assert false;
    return false;
  }
  public boolean isRequestedSessionIdFromUrl()
  {
    assert false;
    return false;
  }
  public boolean isRequestedSessionIdFromURL()
  {
    assert false;
    return false;
  }
  public boolean isRequestedSessionIdValid()
  {
    assert false;
    return false;
  }
  public boolean isUserInRole( String role )
  {
    assert false;
    return false;
  }
  public Object getAttribute( String name )
  {
    assert false;
    return null;
  }
  public Enumeration getAttributeNames()
  {
    assert false;
    return null;
  }
  public String getCharacterEncoding()
  {
    assert false;
    return null;
  }
  public int getContentLength()
  {
    assert false;
    return 0;
  }
  public String getContentType()
  {
    assert false;
    return null;
  }
  public ServletInputStream getInputStream()
    throws IOException
  {
    assert false;
    return null;
  }
  public Locale getLocale()
  {
    assert false;
    return null;
  }
  public Enumeration getLocales()
  {
    assert false;
    return null;
  }
  public String getParameter( String name )
  {
    return params.get( name );
  }
  public Map getParameterMap()
  {
    assert false;
    return null;
  }
  public Enumeration getParameterNames()
  {
    final Iterator iter = params.iterator();
    return new Enumeration() {
        public boolean hasMoreElements() {
          return iter.hasNext();
        }
        public Object nextElement() { 
          return ((Attrib)iter.next()).key;
        }
    };
  }
  public String[] getParameterValues( String name )
  {
    assert false;
    return null;
  }
  public String getProtocol()
  {
    assert false;
    return null;
  }
  public BufferedReader getReader()
    throws IOException
  {
    assert false;
    return null;
  }
  public String getRealPath( String path )
  {
    assert false;
    return null;
  }
  public String getRemoteAddr()
  {
    return "192.168.1.1";
  }
  public String getRemoteHost()
  {
    assert false;
    return null;
  }
  public RequestDispatcher getRequestDispatcher( String path )
  {
    assert false;
    return null;
  }
  public String getScheme()
  {
    assert false;
    return null;
  }
  public String getServerName()
  {
    assert false;
    return null;
  }
  public int getServerPort()
  {
    assert false;
    return 0;
  }
  public boolean isSecure()
  {
    assert false;
    return false;
  }
  public void removeAttribute( String name )
  {
    assert false;

  }
  public void setAttribute( String name, Object o )
  {
    assert false;

  }
  public void setCharacterEncoding( String env )
  {
    assert false;

  }
} // class FakeServletRequest
