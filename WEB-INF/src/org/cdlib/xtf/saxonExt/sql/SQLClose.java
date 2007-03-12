package org.cdlib.xtf.saxonExt.sql;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.SimpleExpression;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.style.ExtensionInstruction;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.SaxonErrorCode;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.ObjectValue;
import java.sql.Connection;
import java.sql.SQLException;

/**
* An sql:close element in the stylesheet.
*/
public class SQLClose extends ExtensionInstruction 
{
  Expression connection = null;

  public void prepareAttributes()
    throws XPathException 
  {
    String connectAtt = getAttributeList().getValue("", "connection");
    if (connectAtt == null) {
      reportAbsence("connection");
    }
    else {
      connection = makeExpression(connectAtt);
    }
  }

  public void validate()
    throws XPathException 
  {
    super.validate();
    connection = typeCheck("connection", connection);
  }

  public Expression compile(Executable exec)
    throws XPathException 
  {
    return new CloseInstruction(connection);
  }

  private static class CloseInstruction extends SimpleExpression 
  {
    public static final int CONNECTION = 0;

    public CloseInstruction(Expression connect) {
      Expression[] sub = { connect };
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
      return "sql:close";
    }

    public int computeCardinality() {
      return StaticProperty.ALLOWS_ZERO_OR_ONE;
    }

    public Item evaluateItem(XPathContext context)
      throws XPathException 
    {
      Item conn = arguments[CONNECTION].evaluateItem(context);
      if (!(conn instanceof ObjectValue &&
          ((ObjectValue)conn).getObject() instanceof Connection)) 
      {
        dynamicError("Value of connection expression is not a JDBC Connection",
                     SaxonErrorCode.SXSQ0001, context);
      }
      Connection connection = (Connection)((ObjectValue)conn).getObject();
      try {
        connection.close();
      }
      catch (SQLException ex) {
        dynamicError("(SQL) Failed to close connection: " + ex.getMessage(),
                     SaxonErrorCode.SXSQ0002, context);
      }
      return null;
    }
  }
}

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Additional Contributor(s): Rick Bonnett [rbonnett@acadia.net]
//
