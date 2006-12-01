package org.cdlib.xtf.saxonExt;

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
 * 
 * Acknowledgements:
 * 
 * A significant amount of new and/or modified code in this module
 * was made possible by a grant from the Andrew W. Mellon Foundation,
 * as part of the Melvyl Recommender Project.
 */

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerConfigurationException;

import org.cdlib.xtf.servletBase.TextServlet;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.SimpleExpression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.om.Item;
import net.sf.saxon.style.ExtensionElementFactory;
import net.sf.saxon.style.ExtensionInstruction;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;

/**
 * Implements the "Redirect" Saxon extension, which allows stylesheets to 
 * force an immediate HTTP redirect to a different URL.
 * 
 * This extension should be used prior to generating any output; otherwise
 * an exception will be thrown.
 * 
 * @author Martin Haye
 */
public class Redirect implements ExtensionElementFactory 
{
  /**
  * Identify the class to be used for stylesheet elements with a given local name.
  * The returned class must extend net.sf.saxon.style.StyleElement
  * @return null if the local name is not a recognised element type in this
  * namespace.
  */

  public Class getExtensionClass(String localname)  
  {
      if (localname.equals("send")) return SendElement.class;

      return null;
  }

  /**
   * Implements a Saxon instruction that executes an external process and
   * properly formats the result. Provides timeout and error checking.
   * 
   * @author Martin Haye
   */
  public static class SendElement extends ExtensionInstruction 
  {
    String url;
    
    public void prepareAttributes() throws TransformerConfigurationException 
    {
      // Get mandatory 'url' attribute
      url = getAttributeList().getValue("", "url");
      if( url == null )
          reportAbsence( "url" );       
    } // prepareAttributes()
      
    public Expression compile(Executable exec) 
      throws TransformerConfigurationException 
    {
      return new SendInstruction(url);
    }

    private class SendInstruction extends SimpleExpression 
    {
      String  url;

      public SendInstruction( String url )
      {
          this.url = url;
      }

      /**
       * A subclass must provide one of the methods evaluateItem(), iterate(), or process().
       * This method indicates which of the three is provided.
       */

      public int getImplementationMethod() {
        return Expression.EVALUATE_METHOD;
      }

      public String getExpressionType() {
        return "redirect:send";
      }

      public Item evaluateItem(XPathContext context) throws XPathException 
      {
        HttpServletResponse res = TextServlet.getCurResponse();
        String encodedUrl = res.encodeRedirectURL( url );
        try {
          res.sendRedirect( encodedUrl );
        }
        catch (IOException e) {
          throw new DynamicError( e );
        }
        return null;
      } // evaluateItem()

    } // class SendInstruction
  } // class SendElement
} // class Redirect
