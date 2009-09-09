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
import java.io.ByteArrayOutputStream;
import java.util.Properties;
import net.sf.saxon.FeatureKeys;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.RoleLocator;
import net.sf.saxon.expr.TypeChecker;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.instruct.GeneralVariable;
import net.sf.saxon.instruct.InstructionDetails;
import net.sf.saxon.instruct.TailCall;
import net.sf.saxon.om.*;
import net.sf.saxon.style.XSLGeneralVariable;
import net.sf.saxon.trace.InstructionInfo;
import net.sf.saxon.trace.Location;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.Value;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.cdlib.xtf.util.XTFSaxonErrorListener;

/**
 * Represents an &lt;input> element below a &lt;run> instruction.
 *
 * @author Martin Haye
 */
public class InputElement extends XSLGeneralVariable 
{
  /**
  * Determine whether this node is an instruction.
  * @return false - it is not an instruction
  */
  public boolean isInstruction() {
    return false;
  }

  /**
  * Determine whether this type of element is allowed to contain a template-body
  * @return false: no, it may not contain a template-body
  */
  public boolean mayContainSequenceConstructor() {
    return false;
  }

  public void prepareAttributes()
    throws XPathException 
  {
    getVariableFingerprint();

    AttributeCollection atts = getAttributeList();

    String selectAtt = null;

    for (int a = 0; a < atts.getLength(); a++) 
    {
      String localName = atts.getLocalName(a);
      if (localName.equals("select")) {
        selectAtt = atts.getValue(a);
      }
      else {
        checkUnknownAttribute(atts.getNameCode(a));
      }
    }

    if (selectAtt != null) {
      select = makeExpression(selectAtt);
    }
  } // prepareAttributes()

  public void validate()
    throws XPathException 
  {
    if (!(getParent() instanceof RunElement)) {
      compileError("parent node must be exec:run");
    }
    if (select != null) 
    {
      select = typeCheck("select", select);
      try {
        RoleLocator role = new RoleLocator(RoleLocator.INSTRUCTION,
                                           "exec:run/input",
                                           0,
                                           null);
        select = TypeChecker.staticTypeCheck(select,
                                             SequenceType.SINGLE_ATOMIC,
                                             false,
                                             role,
                                             getStaticContext());
      }
      catch (XPathException err) {
        compileError(err);
      }
    }
  } // validate()

  public Expression compile(Executable exec)
    throws XPathException 
  {
    InputInstruction inst = new InputInstruction();
    initializeInstruction(exec, inst);
    return inst;
  }

  protected static class InputInstruction extends GeneralVariable 
  {
    public InputInstruction() {
    }

    public InstructionInfo getInstructionInfo() {
      InstructionDetails details = (InstructionDetails)super.getInstructionInfo();
      details.setConstructType(Location.EXTENSION_INSTRUCTION);
      return details;
    }

    public TailCall processLeavingTail(XPathContext context) {
      return null;
    }

    /**
     * Evaluate the variable (method exists only to satisfy the interface)
     */
    public ValueRepresentation evaluateVariable(XPathContext context)
      throws XPathException 
    {
      throw new UnsupportedOperationException();
    }

    /**
     * Gets a proper byte stream for the value. If the value is simply a
     * string, it will be a UTF-8 encoding of that string. If the value is
     * some structured XML, it will be XML with a header.
     *
     * @param context Context for the evaluation
     * @return        A byte stream, properly formatted
     */
    public byte[] getStream(XPathContext context)
      throws XPathException 
    {
      try 
      {
        // Convert the value to a proper NodeInfo we can examine
        Value value = Value.asValue(getSelectValue(context));
        NodeInfo node = (NodeInfo)value.convertToJava(NodeInfo.class, context);

        // Detect whether there are any elements in the document.
        boolean anyElements = false;
        AxisIterator iter = node.iterateAxis(Axis.CHILD);
        while (true) {
          Item kid = iter.next();
          if (kid == null)
            break;
          if (kid instanceof NodeInfo && ((NodeInfo)kid).hasChildNodes())
            anyElements = true;
        }

        // If no elements, get the simple string value.
        if (!anyElements) {
          String str = value.toString() + "\n";
          return str.getBytes("UTF-8");
        }

        // Convert to XML.
        ByteArrayOutputStream outStream = new ByteArrayOutputStream(100);
        StreamResult streamResult = new StreamResult(outStream);
        TransformerFactory factory = new net.sf.saxon.TransformerFactoryImpl();

        // Avoid NamePool translation, as it triggers a Saxon bug. 
        factory.setAttribute(FeatureKeys.NAME_POOL, node.getNamePool());

        Transformer trans = factory.newTransformer();
        Properties props = trans.getOutputProperties();
        props.put("indent", "yes");
        props.put("method", "xml");
        trans.setOutputProperties(props);

        // Make sure errors get directed to the right place.
        if (!(trans.getErrorListener() instanceof XTFSaxonErrorListener))
          trans.setErrorListener(new XTFSaxonErrorListener());

        trans.transform(node, streamResult);

        // All done.
        outStream.write("\n".getBytes());
        outStream.close();
        return outStream.toByteArray();
      }
      catch (Exception e) {
        dynamicError(
          "Exception occurred converting input for external command: " + e,
          "EXEC001", context);
        return null;
      }
    } // getStream()
  } // class InputElement
} // class InputElement
