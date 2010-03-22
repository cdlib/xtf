/**
 * 
 */
package org.cdlib.xtf.saxonExt;

/**
 * Copyright (c) 2007, Regents of the University of California
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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.ExpressionTool;
import net.sf.saxon.expr.Optimizer;
import net.sf.saxon.expr.PromotionOffer;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.instruct.Instruction;
import net.sf.saxon.instruct.TailCall;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.pattern.EmptySequenceTest;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;

/**
 * Base class that automates much of the tedious Saxon housekeeping for an
 * extension instruction that supports arbitrary content.
 * 
 * @author Martin Haye
 */
public abstract class InstructionWithContent extends Instruction 
{
  protected final String            name;
  private int                       nameCode;
  protected Map<String, Expression> attribs;
  protected Expression              content;

  /**
   * Construct the content instruction.
   */
  public InstructionWithContent(String name, 
                                Map<String, Expression> attribs, 
                                Expression content) 
  {
    // Record input parameters
    this.name    = name;
    this.attribs = attribs;
    this.content = content;

    // Own all the attribute exprssions and the content
    for (Expression exp : attribs.values())
      adoptChildExpression(exp);
    adoptChildExpression(content);
  }
  
  // XTF convenience functions
  public String getAttribStr(String attrName, XPathContext context) 
    throws XPathException 
  {
    return getAttribStr(attrName, context, null);
  }
  
  public String getAttribStr(String attrName, XPathContext context, String defaultVal) 
    throws XPathException
  {
    if (!attribs.containsKey(attrName))
      return defaultVal;
    return attribs.get(attrName).evaluateAsString(context);
  }
  
  public boolean getAttribBool(String attrName, XPathContext context, boolean defaultVal) 
    throws XPathException
  {
    String str = getAttribStr(attrName, context);
    if (str == null)
      return defaultVal;
    if (str.toLowerCase().matches("1|yes|true"))
      return true;
    if (str.toLowerCase().matches("0|no|false"))
      return false;
    return defaultVal;
  }

  /**
   * Simplify an expression. This performs any static optimization (by rewriting the expression
   * as a different expression). The default implementation does nothing.
   * @return the simplified expression
   * @throws net.sf.saxon.trans.XPathException
   *          if an error is discovered during expression rewriting
   */
  public Expression simplify(StaticContext env) throws XPathException 
  {
      for (Entry<String, Expression> attrib : attribs.entrySet())
        attrib.setValue(attrib.getValue().simplify(env));
      
      if (content != null) {
          content = content.simplify(env);
      }
      return this;
  }

  public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException 
  {
      for (Entry<String, Expression> attrib : attribs.entrySet()) {
        attrib.setValue(attrib.getValue().typeCheck(env, contextItemType));
        adoptChildExpression(attrib.getValue());
      }
      if (content != null) {
          content = content.typeCheck(env, contextItemType);
          adoptChildExpression(content);
      }
      return this;
  }

 public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException 
 {
      for (Entry<String, Expression> attrib : attribs.entrySet()) {
        Expression exp = attrib.getValue();
        exp = exp.optimize(opt, env, contextItemType);
        attrib.setValue(exp);
        adoptChildExpression(exp);
      }
      
      if (content != null) {
          content = content.optimize(opt, env, contextItemType);
          adoptChildExpression(content);
      }
      return this;
  }

  /**
  * Get the name of this instruction for diagnostic and tracing purposes
  */

  public int getInstructionNameCode() {
      return nameCode;
  }

  public ItemType getItemType(TypeHierarchy th) {
      return EmptySequenceTest.getInstance();
  }

  public int getCardinality() {
      return StaticProperty.EMPTY;
  }

  /**
   * Determine whether this instruction creates new nodes.
   * This implementation returns true.
   */
  public final boolean createsNewNodes() {
      return true;
  }
  
  /**
   * Handle promotion offers, that is, non-local tree rewrites.
   * @param offer The type of rewrite being offered
   * @throws XPathException
   */
  protected void promoteInst(PromotionOffer offer) throws XPathException 
  {
      for (Entry<String, Expression> attrib : attribs.entrySet()) {
        Expression exp = attrib.getValue();
        if (exp != null) {
          exp = doPromotion(exp, offer);
          attrib.setValue(exp);
        }
      }
      if (content != null) {
          content = doPromotion(content, offer);
      }
  }

  /**
   * Get all the XPath expressions associated with this instruction
   * (in XSLT terms, the expression present on attributes of the instruction,
   * as distinct from the child instructions in a sequence construction)
   */

  public Iterator iterateSubExpressions() 
  {
      ArrayList<Expression> list = new ArrayList<Expression>(attribs.size() + 1);
      for (Expression exp : attribs.values())
        list.add(exp);
      if (content != null) {
          list.add(content);
      }
      return list.iterator();
  }

  /**
   * Replace one subexpression by a replacement subexpression
   * @param original the original subexpression
   * @param replacement the replacement subexpression
   * @return true if the original subexpression is found
   */

  public boolean replaceSubExpression(Expression original, Expression replacement) 
  {
      boolean found = false;
      for (Entry<String, Expression> attrib : attribs.entrySet()) {
        Expression exp = attrib.getValue();
        if (exp == original) {
          attrib.setValue(replacement);
          found = true;
        }
      }
      if (content == original) {
          content = replacement;
          found = true;
      }
      return found;
  }

  /**
   * This is where the main work should be performed. Subclasses must implement
   * this method.
   */
  public abstract TailCall processLeavingTail(XPathContext context) throws XPathException; 
  
  /**
   * Utility function to convert an expression (which might be a sequence) to a string
   * value.
   */
  protected static String sequenceToString(Expression exp, XPathContext context) 
    throws XPathException
  {
    StringBuffer buf = new StringBuffer();
    SequenceIterator iter = exp.iterate(context);
    Item item;
    while ((item = iter.next()) != null)
      buf.append(item.getStringValue());
    return buf.toString();
  }
  
  /**
   * Diagnostic print of expression structure. The expression is written to the System.err
   * output stream
   *
   * @param level indentation level for this expression
   * @param out
   * @param config
   */

  public void display(int level, PrintStream out, Configuration config) 
  {
      out.println(ExpressionTool.indent(level) + name);
  }
  
  /** Special version of dynamicError that includes a cause with the exception. **/
  protected void dynamicError(Throwable cause, String message, String code, XPathContext context) 
    throws DynamicError 
  {
    DynamicError err = new DynamicError(message, getSourceLocator(), cause);
    err.setXPathContext(context);
    err.setErrorCode(code);
    throw err;
  }

}