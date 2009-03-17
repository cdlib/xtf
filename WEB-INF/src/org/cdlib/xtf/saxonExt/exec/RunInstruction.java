package org.cdlib.xtf.saxonExt.exec;

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
 *
 * Acknowledgements:
 *
 * A significant amount of new and/or modified code in this module
 * was made possible by a grant from the Andrew W. Mellon Foundation,
 * as part of the Melvyl Recommender Project.
 */

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.stream.StreamSource;

import org.cdlib.xtf.util.ProcessRunner;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.SimpleExpression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.AllElementStripper;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.tinytree.TinyBuilder;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.StringValue;

/**
 * Utility class that does most of the work for RunElement.
 */
class RunInstruction extends SimpleExpression 
{
  protected Expression command;
  protected int timeout;
  protected int nArgs;
  protected InputElement.InputInstruction inputExpr;

  public RunInstruction(Expression command, int timeout, List args) 
  {
    this.command = command;
    this.timeout = timeout;

    nArgs = args.size();

    if (args.size() > 0 &&
        args.get(args.size() - 1) instanceof InputElement.InputInstruction) 
    {
      inputExpr = (InputElement.InputInstruction)args.get(args.size() - 1);
      --nArgs;
    }

    Expression[] sub = new Expression[args.size()];
    for (int i = 0; i < args.size(); i++)
      sub[i] = (Expression)args.get(i);
    setArguments(sub);
  }

  /**
   * A subclass must provide one of the methods evaluateItem(), iterate(), or process().
   * This method indicates which of the three is provided.
   */
  public int getImplementationMethod() {
    return Expression.EVALUATE_METHOD;
  }

  public String getExpressionType() {
    return "exec:run";
  }

  public Item evaluateItem(XPathContext context)
    throws XPathException 
  {
    String[] argArray = gatherArgs(context);
    byte[] outBytes = runAndGrab(context, argArray);
    
    byte[] lookFor = "<?xml".getBytes();
    int i;
    for (i = 0; i < lookFor.length; i++) {
      if (i >= outBytes.length || outBytes[i] != lookFor[i])
        break;
    }

    if (i < lookFor.length) 
    {
      // Doesn't look like XML. Just parse it as a string.
      return new StringValue(new String(outBytes));
    }

    // Ooh, we got some XML. Let's make a real tree out of it.
    StreamSource src = new StreamSource(new ByteArrayInputStream(outBytes));
    NodeInfo doc = null;
    try {
      doc = TinyBuilder.build(src,
                              new AllElementStripper(),
                              context.getController().getConfiguration());
    }
    catch (XPathException e) {
      dynamicError(
        "Error parsing XML output from external command '" + command + "': " +
        e,
        "EXEC004", context);
    }

    // All done.
    return doc;
  } // evaluateItem()

  /**
   * Run the external process, applying a timeout if specified, feeding it
   * input on stdin and gathering the results from stdout. If a non-zero
   * exit status is returned, we throw an exception containing the output
   * string from stderr.
   */
  protected byte[] runAndGrab(XPathContext context, String[] argArray)
      throws XPathException, DynamicError 
  {
    // Is there input to send to the process?
    byte[] inputBytes = new byte[0];
    if (inputExpr != null)
      inputBytes = inputExpr.getStream(context);

    byte[] outputBytes = null;
    try {
      outputBytes = ProcessRunner.runAndGrab(argArray, inputBytes, timeout);
    }
    catch (IOException e) 
    {
      dynamicError(
        "IO exception occurred processing external command '" + command +
        "': " + e,
        "EXEC005", context);
    }
    catch (InterruptedException e) {
      dynamicError(
        "External command '" + command + "' exceeded timeout of " +
        (new DecimalFormat().format(timeout / 1000.0)) + " sec",
        "EXEC002", context);
    }
    catch (ProcessRunner.CommandFailedException e) {
      dynamicError(e.getMessage(), "EXEC003", context);
    }
    
    // Return the results from stdout
    return outputBytes;
  }

  /**
   * Gather all the arguments for this instruction and make them into a
   * convenient array.
   */
  protected String[] gatherArgs(XPathContext context) throws XPathException 
  {
    ArrayList args = new ArrayList(10);

    // Put the command first in our list of arguments.
    args.add(command.evaluateAsString(context));

    // Gather all the arguments
    for (int c = 0; c < nArgs; c++) {
      String strVal = ((ArgElement.ArgInstruction)arguments[c]).getSelectValue(
        context).getStringValue();
      args.add(strVal);
    } // for c
    
    String[] argArray = (String[])args.toArray(new String[args.size()]);
    return argArray;
  }
  
} // class RunInstruction