package org.cdlib.xtf.xslt;


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
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.cdlib.xtf.servletBase.TextServlet;
import org.cdlib.xtf.util.XTFSaxonErrorListener;
import net.sf.saxon.FeatureKeys;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.AllElementStripper;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.AxisIterator;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.tinytree.TinyBuilder;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.SingletonNode;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.value.Value;

/**
 * Utility functions to store and access variables in the user's session.
 * Also provides functions that can be called to check whether session
 * tracking is enabled, and map URLs.
 *
 * @author Martin Haye
 */
public class Session 
{
  /** Checks whether session tracking was enabled in the servlet config */
  public static boolean isEnabled(XPathContext context) {
    return TextServlet.getCurServlet().isSessionTrackingEnabled();
  } // isEnabled()

  /** Function to get a data from a session variable. */
  public static Value getData(XPathContext context, String name)
    throws XPathException 
  {
    HttpServletRequest req = TextServlet.getCurRequest();

    HttpSession session = req.getSession(false);
    if (session == null)
      return null;

    String val = (String)session.getAttribute(name);
    if (val == null)
      return null;

    return getValue(context, val);
  } // getData()

  /** Function to put structured data into a session variable. */
  public static void setData(XPathContext context, String name, Value value)
    throws XPathException 
  {
    setData(context, name, getString(context, value));
  } // setData()

  /** Function to put data into a session variable. */
  public static void setData(XPathContext context, String name, String value) 
  {
    // Make sure session tracking is enabled in the servlet.
    if (!TextServlet.getCurServlet().isSessionTrackingEnabled()) {
      throw new RuntimeException(
        "Error: session tracking must be enabled in servlet config file " +
        "before storing session data");
    }

    // Now store the value.
    HttpServletRequest req = TextServlet.getCurRequest();
    HttpSession session = req.getSession(true);
    String oldVal = (String)session.getAttribute(name);
    if (!value.equals(oldVal))
      session.setAttribute(name, value);
  } // setData()

  /** Function to encode a URL, adding session ID if necessary. */
  public static String encodeURL(XPathContext context, String origURL) {
    HttpServletResponse res = TextServlet.getCurResponse();
    String mappedURL = res.encodeURL(origURL);
    return mappedURL;
  } // encodeURL()

  /** Function to get the current session's identifier */
  public static String getID() 
  {
    // Make sure session tracking is enabled in the servlet.
    if (!TextServlet.getCurServlet().isSessionTrackingEnabled()) {
      throw new RuntimeException(
        "Error: session tracking must be enabled in servlet config file " +
        "before getting session ID");
    }

    // Now get the session ID.
    HttpServletRequest req = TextServlet.getCurRequest();
    HttpSession session = req.getSession(true);
    return session.getId();
  } // getSessionID()
  
  /** Function to detect if cookies are turned off */
  public static boolean noCookie() 
  {
    // Make sure session tracking is enabled in the servlet.
    if (!TextServlet.getCurServlet().isSessionTrackingEnabled())
      return false;
    
    // Now check whether the ID is from a URL instead of a cookie.
    HttpServletRequest req = TextServlet.getCurRequest();
    return !req.isRequestedSessionIdFromCookie();
  }

  /**
   * Gets a proper string for the value. If the value is simply a string, we
   * return just that. If the value is some structured XML, we return
   * XML with a header.
   *
   * @param context Context for the evaluation
   * @return        A byte stream, properly formatted
   */
  public static String getString(XPathContext context, Value value)
    throws XPathException 
  {
    if (value instanceof StringValue)
      return value.getStringValue();

    try 
    {
      // Convert the value to a proper NodeInfo we can examine
      NodeInfo node = (NodeInfo)value.convertToJava(NodeInfo.class, context);

      // Detect whether there are any elements in the document.
      int nElements = 0;
      AxisIterator iter = node.iterateAxis(Axis.CHILD);
      while (true) 
      {
        Item kid = iter.next();
        if (kid == null)
          break;
        if (kid instanceof NodeInfo &&
            ((NodeInfo)kid).getNodeKind() == Type.ELEMENT) 
        {
          ++nElements;
        }
      }

      // If no elements, get the simple string value.
      if (nElements == 0)
        return value.toString();
      else if (nElements > 1) {
        DynamicError err = new DynamicError(
          "Error converting XML to string: there must be exactly one root element");
        err.setXPathContext(context);
        throw err;
      }

      // Convert to XML.
      StringWriter writer = new StringWriter();
      StreamResult streamResult = new StreamResult(writer);
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
      String ret = writer.getBuffer().toString();
      return ret;
    }
    catch (Exception e) {
      DynamicError err = new DynamicError(
        "Exception occurred converting XML to string: " + e);
      err.setXPathContext(context);
      throw err;
    }
  } // getString()

  /**
   * Checks if the input string is actually an XML document. If so, returns
   * a value containing the parsed XML as a node. Otherwise, returns a
   * simple string value.
   *
   * @param str   The string to check.
   * @return      Either a SingletonNode or a StringValue.
   */
  private static Value getValue(XPathContext context, String str)
    throws XPathException 
  {
    // See if we got XML.
    int idx = str.indexOf("<?xml");
    if (idx < 0 || idx > 10) 
    {
      // Doesn't look like XML. Just treat it as a string.
      return new StringValue(str);
    }

    // Ooh, we got some XML. Let's make a real Node out of it.
    StreamSource src = new StreamSource(new StringReader(str));
    NodeInfo doc = TinyBuilder.build(src,
                                     new AllElementStripper(),
                                     context.getController().getConfiguration());
    return new SingletonNode(doc);
  } // getValue()
} // class Session
