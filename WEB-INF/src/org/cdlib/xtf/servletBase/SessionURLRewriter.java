package org.cdlib.xtf.servletBase;

/*
 * Copyright (c) 2005, Regents of the University of California
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

import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sf.saxon.event.ProxyReceiver;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.trans.XPathException;

/**
 * If session data has been established, this class takes care of adding the
 * session ID to URLs output by a Saxon transformation.
 */
public class SessionURLRewriter extends ProxyReceiver 
{
  private Pattern             encodeURLPattern;
  private HttpServletRequest  httpRequest;
  private HttpServletResponse httpResponse;
  private NamePool            namePool;
  private String              elementName;
  
  /** 
   * Construct the URL rewriter, recording a reference to the receiver
   * that will get the data, and the HTTP servlet response we'll use to
   * get session info and to rewrite URLs.
   */
  SessionURLRewriter( Receiver            underlyingReceiver,
                      Pattern             encodeURLPattern,
                      HttpServletRequest  httpRequest,
                      HttpServletResponse httpResponse )
  {
      setUnderlyingReceiver( underlyingReceiver );
      this.encodeURLPattern = encodeURLPattern;
      this.httpRequest      = httpRequest;
      this.httpResponse     = httpResponse;
  }
  
  /** 
   * Called when an element begins. We simply record the name for later
   * reference.
   */
  public void startElement( int nameCode, 
                            int typeCode, 
                            int locationId, 
                            int properties ) 
    throws XPathException 
  {
    elementName = getLocalName( nameCode );
    super.startElement( nameCode, typeCode, locationId, properties );
  }
    
  /** 
   * Called when an attribute is output. If it needs a session ID added,
   * we do so here.
   */
  public void attribute( int nameCode, 
                         int typeCode, 
                         CharSequence value, 
                         int locationId, 
                         int properties)
    throws XPathException 
  {
    // Is this an attribute we want to edit? Right now, we try to be 
    // conservative:
    //
    // (1) "href" attribute of a <a> element;
    // (2) "action" attribute of a <form> element;
    // (3) "src" attribute of a <frame> element.
    //
    String attrName = getLocalName( nameCode );
    if( !(elementName.equals("a") && attrName.equals("href")) && 
        !(elementName.equals("form") && attrName.equals("action")) &&
        !(elementName.equals("frame") && attrName.equals("src")) )
    {
        super.attribute( nameCode, typeCode, value, locationId, properties );
        return;
    }
    
    // If there's no session data, then rewriting is never necesssary.
    HttpSession session = httpRequest.getSession( false ); // don't create
    if( session == null ) {
        super.attribute( nameCode, typeCode, value, locationId, properties );
        return;
    }
    
    // If the URL is absolute and a pattern has been specified, only encode it
    // if a pattern was specified and the URL matches the pattern.
    //
    String strVal = value.toString();
    if( strVal.startsWith("http") &&
        (encodeURLPattern == null || 
        !encodeURLPattern.matcher(strVal).matches()) )
    {
        super.attribute( nameCode, typeCode, value, locationId, properties );
        return;
    }

    // See if the servlet container wants to map this URL to add a session ID.
    String mapped = httpResponse.encodeURL( strVal );
    if( !mapped.equals(value) ) {
        super.attribute( nameCode, typeCode, mapped, locationId, properties );
        return;
    }
    
    // No match. Leave the URL unchanged.
    super.attribute( nameCode, typeCode, value, locationId, properties );
    
  } // attribute()

  /** Get the local name corresponding to the given namecode */
  private String getLocalName( int nameCode )
  {
    if( namePool == null )
        namePool = getNamePool();
    return namePool.getLocalName(nameCode);
  }
  
} // class SessionURLRewriter
