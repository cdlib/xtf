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

import java.util.HashMap;
import java.util.Map;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.om.AttributeCollection;
import net.sf.saxon.om.Axis;
import net.sf.saxon.style.ExtensionInstruction;
import net.sf.saxon.trans.XPathException;

/**
 * Base class that automates much of the tedious Saxon housekeeping for an
 * extension element that supports arbitrary content.
 * 
 * @author Martin Haye
 */
public abstract class ElementWithContent extends ExtensionInstruction 
{
  protected Map<String, Expression> attribs = new HashMap();

  /**
   * Parse mandatory and optional attributes during prepareAttributes() call.
   */
  protected void parseAttributes(String[] mandatoryAtts, String[] optionalAtts) 
    throws XPathException
  {
    AttributeCollection inAtts = getAttributeList();
    for (int i = 0; i < inAtts.getLength(); i++) 
    {
      String attName = inAtts.getLocalName(i);
      for (String m : mandatoryAtts) {
        if (m.equalsIgnoreCase(attName))
          attribs.put(m, makeAttributeValueTemplate(inAtts.getValue(i)));
      }
      for (String m : optionalAtts) {
        if (m.equalsIgnoreCase(attName))
          attribs.put(m, makeAttributeValueTemplate(inAtts.getValue(i)));
      }
      
      // There may be other attributes present (e.g. xmlns:xxx, etc.) so don't complain
      // if there's no match.
    }
    
    // Make sure all manditory attributes were specified
    for (String m : mandatoryAtts) {
      if (!attribs.containsKey(m)) {
        reportAbsence(m);
        return;
      }
    }    
  }
  
  /**
   * Derived classes need to come up with their mandatory and optional
   * attributes and call parseAttributes() above.
   */
  public abstract void prepareAttributes() throws XPathException; 
  
  /**
   * Call during compile() to get the content expression.
   */
  protected Expression compileContent(Executable exec) 
    throws XPathException 
  {
    return compileSequenceConstructor(exec, iterateAxis(Axis.CHILD), true); 
  }
  
  /**
   * Determine whether this type of element is allowed to contain a template-body
   */
  public boolean mayContainSequenceConstructor() {
    return true;
  }
}