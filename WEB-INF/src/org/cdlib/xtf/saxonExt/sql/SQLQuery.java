package org.cdlib.xtf.saxonExt.sql;

import net.sf.saxon.Controller;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.ReceiverOptions;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.SimpleExpression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.style.ExtensionInstruction;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.ObjectValue;
import net.sf.saxon.value.StringValue;
import javax.xml.transform.TransformerConfigurationException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * An sql:query element in the stylesheet.
 *
 * For example:
 * <pre>
 *   &lt;sql:query column="{$column}" table="{$table}" where="{$where}"
 *                 xsl:extension-element-prefixes="sql"/ &gt;
 *
 * </pre>
 * (result with HTML-table-output) <BR>
 * <pre>
 *   &lt;sql:query column="{$column}" table="{$table}" where="{$where}"
 *                 row-tag="TR" column-tag="TD"
 *                 separatorType="tag"
 *                 xsl:extension-element-prefixes="sql"/ &gt;
 * </pre>
 * @author claudio.thomas@unix-ag.org (based on Michael Kay's SQLInsert.java)
*/
public class SQLQuery extends ExtensionInstruction 
{
  Expression connection;
  /** selected column(s) to query */
  Expression column;
  /** the table(s) to query in */
  Expression table;
  /** conditions of query (can be omitted)*/
  Expression where;
  String rowTag;
  /** name of element to hold the rows */
  String colTag;
  /** name of element to hold the columns */
  boolean disable = false; // true means disable-output-escaping="yes"

  public void prepareAttributes()
    throws TransformerConfigurationException 
  {
    // Attributes for SQL-statement
    String dbCol = attributeList.getValue("", "column");
    if (dbCol == null)
      reportAbsence("column");
    column = makeAttributeValueTemplate(dbCol);

    String dbTab = attributeList.getValue("", "table");
    if (dbTab == null)
      reportAbsence("table");
    table = makeAttributeValueTemplate(dbTab);

    String dbWhere = attributeList.getValue("", "where");
    if (dbWhere == null) {
      where = StringValue.EMPTY_STRING;
    }
    else {
      where = makeAttributeValueTemplate(dbWhere);
    }

    String connectAtt = attributeList.getValue("", "connection");
    if (connectAtt == null) {
      reportAbsence("connection");
    }
    else {
      connection = makeExpression(connectAtt);
    }

    // Atributes for row & column element names
    rowTag = attributeList.getValue("", "row-tag");
    if (rowTag == null) {
      rowTag = "row";
    }
    if (rowTag.indexOf(':') >= 0) {
      compileError("rowTag must not contain a colon");
    }

    colTag = attributeList.getValue("", "column-tag");
    if (colTag == null) {
      colTag = "col";
    }
    if (colTag.indexOf(':') >= 0) {
      compileError("colTag must not contain a colon");
    }

    // Attribute output-escaping
    String disableAtt = attributeList.getValue("", "disable-output-escaping");
    if (disableAtt != null) 
    {
      if (disableAtt.equals("yes")) {
        disable = true;
      }
      else if (disableAtt.equals("no")) {
        disable = false;
      }
      else {
        compileError("disable-output-escaping attribute must be either yes or no");
      }
    }
  }

  public void validate()
    throws TransformerConfigurationException 
  {
    super.validate();
    column = typeCheck("column", column);
    table = typeCheck("table", table);
    where = typeCheck("where", where);
    connection = typeCheck("connection", connection);
  }

  public Expression compile(Executable exec)
    throws TransformerConfigurationException 
  {
    QueryInstruction inst = new QueryInstruction(connection,
                                                 column,
                                                 table,
                                                 where,
                                                 rowTag,
                                                 colTag,
                                                 disable);
    return inst;
  }

  private static class QueryInstruction extends SimpleExpression 
  {
    public static final int CONNECTION = 0;
    public static final int COLUMN = 1;
    public static final int TABLE = 2;
    public static final int WHERE = 3;
    String rowTag;
    String colTag;
    int options;

    public QueryInstruction(Expression connection, Expression column,
                            Expression table, Expression where, String rowTag,
                            String colTag, boolean disable) 
    {
      Expression[] sub = { connection, column, table, where };
      setArguments(sub);
      this.rowTag = rowTag;
      this.colTag = colTag;
      this.options = (disable ? ReceiverOptions.DISABLE_ESCAPING : 0);
    }

    /**
     * A subclass must provide one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of the three is provided.
     */
    public int getImplementationMethod() {
      return Expression.PROCESS_METHOD;
    }

    public String getExpressionType() {
      return "sql:query";
    }

    public void process(XPathContext context)
      throws XPathException 
    {
      // Prepare the SQL statement (only do this once)
      Controller controller = context.getController();
      Item conn = arguments[CONNECTION].evaluateItem(context);
      if (!(conn instanceof ObjectValue &&
          ((ObjectValue)conn).getObject() instanceof Connection)) 
      {
        DynamicError de = new DynamicError(
          "Value of connection expression is not a JDBC Connection");
        de.setXPathContext(context);
        throw de;
      }
      Connection connection = (Connection)((ObjectValue)conn).getObject();

      String dbCol = arguments[COLUMN].evaluateAsString(context);
      String dbTab = arguments[TABLE].evaluateAsString(context);
      String dbWhere = arguments[WHERE].evaluateAsString(context);

      NamePool pool = controller.getNamePool();
      int rowCode = pool.allocate("", "", rowTag);
      int colCode = pool.allocate("", "", colTag);

      try 
      {
        StringBuffer statement = new StringBuffer();
        statement.append("SELECT " + dbCol + " FROM " + dbTab);
        if (dbWhere != "") {
          statement.append(" WHERE " + dbWhere);
        }

        //System.err.println("-> SQL: " + statement.toString());

        // -- Prepare the SQL statement
        PreparedStatement ps = connection.prepareStatement(statement.toString());
        controller.setUserData(this, "sql:statement", ps);

        // -- Execute Statement
        ResultSet rs = ps.executeQuery();

        // -- Print out Result
        Receiver out = context.getReceiver();
        String result = "";
        int icol = rs.getMetaData().getColumnCount();
        while (rs.next()) { // next row

                            //System.out.print("<- SQL : "+ rowStart);
          out.startElement(rowCode, -1, locationId, 0);
          for (int col = 1; col <= icol; col++) { // next column

                                                  // Read result from RS only once, because
                                                  // of JDBC-Specifications
            result = rs.getString(col);
            out.startElement(colCode, -1, locationId, 0);
            if (result != null) {
              out.characters(result, locationId, options);
            }
            out.endElement();
          }

          //System.out.println(rowEnd);
          out.endElement();
        }
        rs.close();

        if (!connection.getAutoCommit()) {
          connection.commit();
        }
      }
      catch (SQLException ex) {
        DynamicError de = new DynamicError("(SQL) " + ex.getMessage());
        de.setXPathContext(context);
        throw de;
      }
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
//
// Contributor(s): claudio.thomas@unix-ag.org (based on SQLInsert.java)
//
