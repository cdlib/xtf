package org.cdlib.xtf.util;

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

import java.io.StringWriter;
import java.util.Properties;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

/**
 * Simple utility class that takes a Node or Source (representing an XML
 * document) and produces an indented string representation of it. This is
 * very useful for debugging.
 * 
 * @author Martin Haye
 */
public class XMLWriter
{

    /**
     * Prints the node, in XML format, to Trace.debug()
     */
    public static void debug( EasyNode node )
    {
        Trace.debug( toString(node) );
    }

    
    /**
     * Prints the node, in XML format, to Trace.debug()
     */
    public static void debug( Source node )
    {
        Trace.debug( toString(node) );
    } // debugNode()
    
    
    /** 
     * Format a nice, multi-line, indented, representation of the given
     * XML fragment.
     * 
     * @param node  Base node to format.
     */
    public static String toString( EasyNode node )
    {
        return toString( node.getWrappedNode() );
    }
    
    /** 
     * Format a nice, multi-line, indented, representation of the given
     * XML fragment.
     * 
     * @param node  Base node to format.
     */
    public static String toString( Source node )
    {
        try {
            StringWriter writer = new StringWriter();
            StreamResult tmp = new StreamResult( writer );
            TransformerFactory factory = new net.sf.saxon.TransformerFactoryImpl();
            Transformer trans = factory.newTransformer();
            Properties props = trans.getOutputProperties();
            props.put( "indent", "yes" );
            props.put( "method", "xml" );
            trans.setOutputProperties( props );
            
            // Make sure errors get directed to the right place.
            if( !(trans.getErrorListener() instanceof XTFSaxonErrorListener) )
                trans.setErrorListener( new XTFSaxonErrorListener() );
    
            trans.transform( node, tmp );
            return writer.toString();
        }
        catch( Exception e ) {
            return "Error writing XML: " + e;
        } 
    }
    
} // class XMLWriter
