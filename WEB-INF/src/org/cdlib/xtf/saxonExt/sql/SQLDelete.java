package org.cdlib.xtf.saxonExt.sql;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.SimpleExpression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.om.Item;
import net.sf.saxon.style.ExtensionInstruction;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.IntegerValue;
import net.sf.saxon.value.ObjectValue;

import javax.xml.transform.TransformerConfigurationException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
* An sql:delete element in the stylesheet.
*/

public class SQLDelete extends ExtensionInstruction {

    Expression connection;
    Expression table;
    Expression where;

    public void prepareAttributes() throws TransformerConfigurationException {

        String connectAtt = getAttributeList().getValue("", "connection");
        if (connectAtt==null) reportAbsence("connection");
        connection = makeExpression(connectAtt);
        
        String tableAtt = attributeList.getValue("", "table");
        if (tableAtt==null) reportAbsence("table");
        table = makeAttributeValueTemplate(tableAtt);

        String whereAtt = attributeList.getValue("", "where");
        if (whereAtt==null) reportAbsence("where");
        where = makeAttributeValueTemplate(whereAtt);
    }

    public void validate() throws TransformerConfigurationException {
        super.validate();
        connection = typeCheck("connection", connection);
        table = typeCheck("table", table);
        where = typeCheck("where", where);
    }

    public Expression compile(Executable exec) throws TransformerConfigurationException {
        return new DeleteInstruction(connection, table, where);
    }

    private static class DeleteInstruction extends SimpleExpression {

        public static final int CONNECTION = 0;
        public static final int TABLE = 1;
        public static final int WHERE = 2;

        public DeleteInstruction(Expression connection, Expression table, Expression where) 
        {
            Expression[] sub = {connection, table, where};
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
            return "sql:delete";
        }

        public Item evaluateItem(XPathContext context) throws XPathException {

            // Construct the SQL statement.
            String dbTab = arguments[TABLE].evaluateAsString(context);
            String dbWhere = arguments[WHERE].evaluateAsString(context);

            StringBuffer statement = new StringBuffer(120);
            statement.append("DELETE FROM " + dbTab + " WHERE " + dbWhere);
            
            // Prepare the SQL statement (only do this once)
            Item conn = arguments[CONNECTION].evaluateItem(context);
            if (!(conn instanceof ObjectValue && ((ObjectValue)conn).getObject() instanceof Connection) ) {
                dynamicError("Value of connection expression is not a JDBC Connection", context);
            }
            Connection connection = (Connection)((ObjectValue)conn).getObject();
            PreparedStatement ps = null;
            int nDeleted = -1;

            try {
                ps = connection.prepareStatement(statement.toString());
                nDeleted = ps.executeUpdate();
                if (!connection.getAutoCommit()) {
                    connection.commit();
                }

            } catch (SQLException ex) {
                dynamicError("(SQL DELETE) " + ex.getMessage(), context);
            } finally {
               if (ps != null) {
                   try {
                       ps.close();
                   } catch (SQLException ignore) {}
               }
            }

            // Return the number of rows that were deleted.
            return new IntegerValue(nDeleted);
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
// Additional Contributor(s): Martin Haye
//
