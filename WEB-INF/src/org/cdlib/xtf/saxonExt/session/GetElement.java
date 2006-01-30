package org.cdlib.xtf.saxonExt.session;

/*
 * Copyright (c) 2006, Regents of the University of California
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
import javax.servlet.http.HttpSession;
import javax.xml.transform.TransformerConfigurationException;

import org.cdlib.xtf.servletBase.TextServlet;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.SimpleExpression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.om.AttributeCollection;
import net.sf.saxon.om.Item;
import net.sf.saxon.style.ExtensionInstruction;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.StringValue;

/**
 * Implements a Saxon instruction that gets the value of a session state 
 * variable.
 * 
 * @author Martin Haye
 */
public class GetElement extends ExtensionInstruction 
{
  private String     name   = null;
  
  public void prepareAttributes() throws TransformerConfigurationException 
  {
    AttributeCollection atts = getAttributeList();

    for( int a=0; a<atts.getLength(); a++ ) 
    {
        String localName = atts.getLocalName( a );
        if( localName.equals("name") )
            name = atts.getValue(a);
        else
            checkUnknownAttribute( atts.getNameCode(a) );
    }

    if( name == null )
        reportAbsence( "name" );
  } // prepareAttributes()
    
  public Expression compile(Executable exec) 
    throws TransformerConfigurationException 
  {
    return new GetInstruction( name );
  }

  private static class GetInstruction extends SimpleExpression 
  {
    String     name;

    public GetInstruction( String name )
    {
      this.name = name;
    }

    /**
     * A subclass must provide one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of the three is provided.
     */
    public int getImplementationMethod() {
      return Expression.EVALUATE_METHOD;
    }

    public String getExpressionType() {
      return "session:getData";
    }

    public Item evaluateItem( XPathContext context ) throws XPathException 
    {
      HttpServletRequest req = TextServlet.getCurRequest();
      
      HttpSession session = req.getSession( false );
      if( session == null )
          return null;
      
      String val = (String) session.getAttribute( name );
      if( val == null )
          return null;
      
      return new StringValue(val);
    } // evaluateItem()

  } // class GetInstruction
  
} // class GetElement
