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
import java.util.ArrayList;
import java.util.List;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.AxisIterator;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.style.ExtensionInstruction;
import net.sf.saxon.trans.XPathException;

/**
 * Implements a Saxon instruction that executes an external process and
 * properly formats the result. Provides timeout and error checking.
 *
 * @author Martin Haye
 */
public class RunElement extends ExtensionInstruction 
{
  protected Expression command;
  protected int timeoutMsec;

  public void prepareAttributes()
    throws XPathException 
  {
    // Get mandatory 'command' attribute
    command = makeAttributeValueTemplate(getAttributeList().getValue("", "command"));
    if (command == null) {
      reportAbsence("command");
      return;
    }

    // Get optional 'timeout' attribute
    String timeoutStr = getAttributeList().getValue("", "timeout");
    if (timeoutStr == null)
      timeoutMsec = 0;
    else 
    {
      try {
        timeoutMsec = (int)(Float.parseFloat(timeoutStr) * 1000);
        timeoutMsec = Math.max(0, timeoutMsec);
      }
      catch (NumberFormatException e) {
        compileError("'timeout' must be a number");
      }
    }
  } // prepareAttributes()

  public Expression compile(Executable exec)
    throws XPathException 
  {
    return new RunInstruction(command, timeoutMsec, getArgInstructions(exec));
  }

  public List getArgInstructions(Executable exec)
    throws XPathException 
  {
    List list = new ArrayList(10);

    AxisIterator kids = iterateAxis(Axis.CHILD);
    NodeInfo child;
    while (true) 
    {
      child = (NodeInfo)kids.next();
      if (child == null)
        break;
      if (child instanceof ArgElement)
        list.add(((ArgElement)child).compile(exec));
      if (child instanceof InputElement) {
        list.add(((InputElement)child).compile(exec));
      }
    }

    return list;
  } // getArgInstructions()

} // class ExecInstruction
