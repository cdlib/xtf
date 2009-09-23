package org.cdlib.xtf.saxonExt.redirect;

/*
 * Copyright (c) 2009, Regents of the University of California
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

import javax.servlet.http.HttpServletResponse;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.SimpleExpression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.om.Item;
import net.sf.saxon.style.ExtensionInstruction;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;

import org.cdlib.xtf.servletBase.TextServlet;

/**
 * Implements a Saxon instruction that generates an HTTP error
 * with a code and optional message.
 *
 * @author Martin Haye
 */
public class HttpErrorElement extends ExtensionInstruction 
{
  Expression codeExp;
  Expression messageExp;

  public void prepareAttributes()
    throws XPathException 
  {
    // Get mandatory 'code' attribute
    String codeAtt = getAttributeList().getValue("", "code");
    if (codeAtt == null) {
      reportAbsence("code");
      return;
    }
    codeExp = makeAttributeValueTemplate(codeAtt);
    
    // Get optional 'message' attribute
    String messageAtt = getAttributeList().getValue("", "message");
    if (messageAtt != null)
      messageExp = makeAttributeValueTemplate(messageAtt);
    
  } // prepareAttributes()

  public Expression compile(Executable exec)
    throws XPathException 
  {
    return new HttpErrorInstruction(codeExp, messageExp);
  }

  private class HttpErrorInstruction extends SimpleExpression 
  {
    Expression codeExp;
    Expression messageExp;

    public HttpErrorInstruction(Expression codeExp, Expression messageExp) {
      this.codeExp = codeExp;
      this.messageExp = messageExp;
    }

    /**
     * A subclass must provide one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of the three is provided.
     */
    public int getImplementationMethod() {
      return Expression.EVALUATE_METHOD;
    }

    public String getExpressionType() {
      return "redirect:sendHttpError";
    }

    public Item evaluateItem(XPathContext context)
      throws XPathException 
    {
      HttpServletResponse res = TextServlet.getCurResponse();
      
      try 
      {
        // Determine the code to send
        String codeStr = codeExp.evaluateAsString(context);
        int code = Integer.parseInt(codeStr);
        
        // Send it, with message if specified.
        if (messageExp == null)
          res.sendError(code);
        else
          res.sendError(code, messageExp.evaluateAsString(context));
      }
      catch (IOException e) {
        throw new DynamicError(e);
      }
      catch (NumberFormatException e) {
        throw new DynamicError(e);
      }
      return null;
    }
  }
}