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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 * Used to gather the response from crossQuery or dynaXML during a test.
 * 
 * @author Martin Haye
 */
public class FakeServletResponse implements HttpServletResponse
{
  ServletOutputStream out;
  
  public FakeServletResponse( ServletOutputStream out ) {
    this.out = out;
  }
  
  public void addCookie( Cookie cookie )
  {
    assert false;

  }
  public void addDateHeader( String name, long date )
  {
    assert false;

  }
  public void addHeader( String name, String value )
  {
    assert false;

  }
  public void addIntHeader( String name, int value )
  {
    assert false;

  }
  public boolean containsHeader( String name )
  {
    assert false;
    return false;
  }
  public String encodeRedirectUrl( String url )
  {
    assert false;
    return null;
  }
  public String encodeRedirectURL( String url )
  {
    assert false;
    return null;
  }
  public String encodeUrl( String url )
  {
    assert false;
    return null;
  }
  public String encodeURL( String url )
  {
    assert false;
    return null;
  }
  public void sendError( int sc, String msg )
    throws IOException
  {
    assert false;

  }
  public void sendError( int sc )
    throws IOException
  {
    assert false;

  }
  public void sendRedirect( String location )
    throws IOException
  {
    assert false;

  }
  public void setDateHeader( String name, long date )
  {
    assert false;

  }
  public void setHeader( String name, String value )
  {
    assert false;

  }
  public void setIntHeader( String name, int value )
  {
    assert false;

  }
  public void setStatus( int sc, String sm )
  {
    assert false;

  }
  public void setStatus( int sc )
  {
    assert false;

  }
  public void flushBuffer()
    throws IOException
  {
    assert false;

  }
  public int getBufferSize()
  {
    assert false;
    return 0;
  }
  public String getCharacterEncoding()
  {
    assert false;
    return null;
  }
  public Locale getLocale()
  {
    assert false;
    return null;
  }
  public ServletOutputStream getOutputStream()
    throws IOException
  {
    return out;
  }
  public PrintWriter getWriter()
    throws IOException
  {
    assert false;
    return null;
  }
  public boolean isCommitted()
  {
    assert false;
    return false;
  }
  public void reset()
    throws IllegalStateException
  {
    assert false;

  }
  public void resetBuffer()
    throws IllegalStateException
  {
    assert false;

  }
  public void setBufferSize( int size )
  {
    assert false;

  }
  public void setContentLength( int len )
  {
    assert false;

  }
  public void setContentType( String type )
  {
    // Do nothing.
  }
  public void setLocale( Locale loc )
  {
    assert false;

  }
}
