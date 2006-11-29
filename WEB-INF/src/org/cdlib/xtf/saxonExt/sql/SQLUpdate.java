package org.cdlib.xtf.saxonExt.sql;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.SimpleExpression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.AxisIterator;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.style.ExtensionInstruction;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.ObjectValue;

import javax.xml.transform.TransformerConfigurationException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
* An sql:update element in the stylesheet.
*/

public class SQLUpdate extends ExtensionInstruction {

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
        return new UpdateInstruction(connection, table, where, getColumnInstructions(exec));
    }
    
    public List getColumnInstructions(Executable exec) throws TransformerConfigurationException {
        List list = new ArrayList(10);

        AxisIterator kids = iterateAxis(Axis.CHILD);
        NodeInfo child;
        while (true) {
            child = (NodeInfo)kids.next();
            if (child == null) {
                break;
            }
            if (child instanceof SQLColumn) {
                list.add(((SQLColumn)child).compile(exec));
            }
        }

        return list;
    }

    private static class UpdateInstruction extends SimpleExpression {

        public static final int CONNECTION = 0;
        public static final int TABLE = 1;
        public static final int WHERE = 2;
        public static final int FIRST_COLUMN = 3;

        public UpdateInstruction(Expression connection, Expression table, 
            Expression where, List columnInstructions) 
        {
            Expression[] sub = new Expression[columnInstructions.size() + 3];
            sub[CONNECTION] = connection;
            sub[TABLE] = table;
            sub[WHERE] = where;
            for (int i=0; i<columnInstructions.size(); i++) {
                sub[i+FIRST_COLUMN] = (Expression)columnInstructions.get(i);
            }
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
            return "sql:update";
        }

        public Item evaluateItem(XPathContext context) throws XPathException {

            // Construct the SQL statement, with question marks for the data.
            String dbTab = arguments[TABLE].evaluateAsString(context);
            String dbWhere = arguments[WHERE].evaluateAsString(context);

            StringBuffer statement = new StringBuffer(120);
            statement.append("UPDATE " + dbTab + " SET ");
            
            for (int c=FIRST_COLUMN; c<arguments.length; c++) {
                if (c > FIRST_COLUMN) statement.append(',');
                SQLColumn.ColumnInstruction colInst = 
                    (SQLColumn.ColumnInstruction)arguments[c]; 
                String colname = colInst.getColumnName();
                statement.append(colname);
                
                if (colInst.evalSql()) {
                    String val = colInst.getSelectValue(context).toString();
                    
                    // Strip leading/trailing quotes from the expression.
                    if ((val.startsWith("\"") && val.endsWith("\"")) || 
                        (val.startsWith("'")  && val.endsWith("'")))
                    {
                        val = val.substring(1, val.length()-1);
                    }
                    statement.append("=" + val);
                }
                else
                    statement.append("=?");
            }
            
            statement.append(" WHERE " + dbWhere);
            
            // Prepare the SQL statement (only do this once)
            Item conn = arguments[CONNECTION].evaluateItem(context);
            if (!(conn instanceof ObjectValue && ((ObjectValue)conn).getObject() instanceof Connection) ) {
                dynamicError("Value of connection expression is not a JDBC Connection", context);
            }
            Connection connection = (Connection)((ObjectValue)conn).getObject();
            PreparedStatement ps = null;
            int nUpdated = -1;

            try {
                ps=connection.prepareStatement(statement.toString());

                // Add the actual column values to be inserted
                int i = 1;
                for (int c=FIRST_COLUMN; c<arguments.length; c++) {
                    SQLColumn.ColumnInstruction colInst = (SQLColumn.ColumnInstruction)arguments[c];
                    if (colInst.evalSql())
                        continue;

                    // TODO: the values are all strings. There is no way of adding to a numeric column
                    String val = ((AtomicValue)colInst.getSelectValue(context)).
                                  getStringValue();

                    // another hack: setString() doesn't seem to like single-character string values
                    if (val.length()==1) val += " ";
                    ps.setObject(i++, val);
                }

                nUpdated = ps.executeUpdate();
                if (!connection.getAutoCommit()) {
                    connection.commit();
                }

            } catch (SQLException ex) {
                dynamicError("(SQL UPDATE) " + ex.getMessage(), context);
            } finally {
               if (ps != null) {
                   try {
                       ps.close();
                   } catch (SQLException ignore) {}
               }
            }

            // Return nothing, so that it's unnecessary to re-route the result.
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
// Additional Contributor(s): Martin Haye
//
// Acknowledgements:
//
// A significant amount of new and/or modified code in this module
// was made possible by a grant from the Andrew W. Mellon Foundation,
// as part of the Melvyl Recommender Project.
//
