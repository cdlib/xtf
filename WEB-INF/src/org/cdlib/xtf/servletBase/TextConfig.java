package org.cdlib.xtf.servletBase;

/**
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
 */

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.cdlib.xtf.util.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Attr;


/** Common members and methods for servlet configuration classes */
public abstract class TextConfig
{
    /** Logging level: "silent", "errors", "warnings", "info", or "debug" */
    public String  logLevel = "info";

    /** Max # of stylesheets to cache */
    public int     stylesheetCacheSize       = 10;

    /** Max length of time (in seconds) to cache a stylesheet. */
    public int     stylesheetCacheExpire     = 0;

    /**
     * Filesystem path to a stylesheet used to generate error pages
     * (no permission, invalid document, general exceptions, etc.)
     */
    public String  errorGenSheet;

    /** 
     * Turns on dependency checking for the caches, so that (for instance) 
     * changing a stylesheet forces it to be reloaded.
     */
    public boolean dependencyCheckingEnabled = true;

    /** All the configuration attributes in the form of name/value pairs */
    public AttribList attribs = new AttribList();

    /**
     * Constructor - Reads and parses the global configuration file (XML) for 
     * the servlet.
     *
     * @param  path                Filesystem path to the config file.
     * @throws DynaXMLException    If a read or parse error occurs.
     */
    public void read( String expectedRootTag, String path )
        throws GeneralException
    {
        try {
            // Read in the document (it's in XML format)
            DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = fac.newDocumentBuilder();
            Document doc = builder.parse( new File(path) );
            
            // Make sure the root tag is correct.
            Element rootEl = (Element) doc.getDocumentElement();
            String rootTag = doc.getDocumentElement().getNodeName();
            if( !rootTag.equals(expectedRootTag) )
                throw new GeneralException( "Config file \"" + path + "\" " +
                        "should contain <" + expectedRootTag + ">" );
            
            // Pick out the elements
            NodeList elements = doc.getDocumentElement().getChildNodes();
            for( int i = 0; i < elements.getLength(); i++ ) {
                Node node = elements.item( i );
                if( node.getNodeType() != Node.ELEMENT_NODE )
                    continue;

                Element el      = (Element) node;
                String  tagName = el.getTagName();
                
                // Scan each attribute of each element.
                NamedNodeMap attrs = el.getAttributes();
                for( int j = 0; j < attrs.getLength(); j++ ) {
                    Attr attrNode = (Attr) attrs.item(j);
                    String attrName = attrNode.getNodeName();
                    
                    String  strVal  = attrNode.getNodeValue();
                    int     intVal  = -1;
                    try { 
                        intVal = Integer.parseInt( strVal );
                    }
                    catch( Exception e ) { }
                    
                    // See if we recognize handle it
                    handleProperty(tagName, attrName, strVal, intVal);
                    
                    // Also put this tag in the attribute list so it can be
                    // passed to the stylesheets.
                    attribs.put( tagName + "." + attrName, strVal );
                }
                

            } // for i
        }
        catch( Exception e ) {
            throw new GeneralException( "Error reading config file " + path +
                ": " + e );
        }

        // Make sure that required parameters were specified.
        requireOrElse( errorGenSheet,
                       "Config file error: errorGen stylesheet not specified" );
    } // constructor

    /**
     * Called when a property is encountered. Derived classes, if they don't
     * recognize the property, should call the base class version of
     * handleProperty().
     * 
     * @param tagName   Name of the element being considered
     * @param attrName  Name of the attribute being considered
     * @param strVal    It's string value
     * @param intVal    Integer version of the string value, or -1.
     */
    protected void handleProperty( String tagName,
                                   String attrName,
                                   String strVal,
                                   int    intVal )
        throws GeneralException
    {
        boolean bad = false;
        
        if( tagName.equals("logging") ) {
            if( strVal.equals("0") )
                strVal = "silent";
            else if( strVal.equals("1") )
                strVal = "info";
            else if( strVal.equals("2") )
                strVal = "debug";
            logLevel = strVal;
        }

        else if( tagName.equals("stylesheetCache") ) {
            if( attrName.equals("size") )
                stylesheetCacheSize = intVal;
            else if( attrName.equals("expire") )
                stylesheetCacheExpire = intVal;
            else
                bad = true;
        }
        
        else if( tagName.equals("errorGen") ) {
            if( attrName.equals("path") )
                errorGenSheet = TextServlet.getRealPath( strVal );
            else
                bad = true;
        }

        else if( tagName.equals("dependencyChecking") ) {
            if( attrName.equals("check") )
                dependencyCheckingEnabled = !(strVal.equals("no")) &&
                                            !(strVal.equals("false")) &&
                                            !(strVal.equals("0"));
            else
                bad = true;
        }
            
        // If we found an element we recognize with an attribute we don't,
        // then barf out.
        //
        if( bad )
            throw new GeneralException( "Config file property " +
                                        tagName + "." + attrName +
                                        " not recognized" );
        
    } // handleProperty()
    
    
    /** 
     * Utility function - if the value is empty, throws an exception.
     *
     * @param value     Value to check for null or ""
     * @param descrip   If thrown, the exception uses this as the message.
     * @throws DynaXMLException    If the value is null.
     */
    public static void requireOrElse( String value, String descrip )
        throws GeneralException
    {
        if( value == null || value.equals("") )
            throw new GeneralException( descrip );
    } // requireOrElse()

} // class TextConfig


