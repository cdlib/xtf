package org.cdlib.xtf.crossQuery;

/*
 * Copyright (c) 2006, Regents of the University of California
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

import net.sf.saxon.om.NodeInfo;

import org.cdlib.xtf.util.EasyNode;
import org.cdlib.xtf.util.GeneralException;

/**
 * Routes a request to a particular query parser. Optionally contains 
 * special tokenizing instructions for one or more URL parameters.
 */
public class QueryRoute 
{
  /** Path to the query parser stylesheet */
  public String queryParserSheet;
  
  /** Special parsing requests for particular URL parameters */
  public HashMap tokenizerMap = new HashMap();
  
  /** Optional: input to query router stylesheet */
  public String routerInput = null;
  
  /** Optional: output from query router stylesheet */
  public String routerOutput = null;
  
  /** Do not construct directly -- use {@link #parse(NodeInfo)} */
  protected QueryRoute() { }
  
  /**
   * Create a default route to the given query parser
   */
  public static QueryRoute createDefault( String queryParserSheet )
  {
    QueryRoute ret = new QueryRoute();
    ret.queryParserSheet = queryParserSheet;
    return ret;
  } // createDefault()
  
  /**
   * Reads and parses the route output from a queryRouter stylesheet.
   *
   * @param  path                Filesystem path to the config file.
   * @throws DynaXMLException    If a read or parse error occurs.
   */
  public static QueryRoute parse( NodeInfo input )
      throws GeneralException
  {
      // Create the (empty) result
      QueryRoute ret = new QueryRoute();
      
      // Make sure the root tag is correct.
      EasyNode root = new EasyNode( input );
      String rootTag = root.name();
      if( rootTag.equals("") && root.nChildren() == 1 ) {
          root = root.child( 0 );
          rootTag = root.name();
      }
      
      if( !rootTag.equalsIgnoreCase("route") )
          throw new QueryRouteException( "Query router stylesheet must output a 'route' element" );
      
      // Pick out the elements
      for( int i = 0; i < root.nChildren(); i++ ) {
          EasyNode el = root.child( i );
          if( !el.isElement() )
              continue;

          // Was a query parser specified?
          String tagName = el.name();
          if( tagName.equalsIgnoreCase("queryParser") )
              ret.parseQueryParser( el );
          else if( tagName.equalsIgnoreCase("tokenize") )
              ret.parseTokenizer( el );
          
      } // for i
      
      // Make sure that required parameters were specified.
      if( ret.queryParserSheet == null || ret.queryParserSheet.length() == 0 )
          throw new QueryRouteException( "Query router stylesheet must output a 'queryParser' element" );
      
      return ret;
      
  } // parse()
  
  
  /**
   * Parse a 'queryParser' element
   */
  private void parseQueryParser( EasyNode el )
  {
    // Scan each attribute of each element.
    for( int j = 0; j < el.nAttrs(); j++ ) {
        if( el.attrName(j).equalsIgnoreCase("path") )
            queryParserSheet = el.attrValue( j );
        else {
            throw new GeneralException( "Query router attribute " +
                el.name() + "." + el.attrName(j) + " not recognized" );
        }
    }
  } // parseQueryParser()


  /**
   * Parse a 'tokenize' element
   */
  private void parseTokenizer( EasyNode el )
  {
    String paramName = null;
    String tokenizer = null;
    
    // Scan each attribute of each element.
    for( int j = 0; j < el.nAttrs(); j++ ) {
        if( el.attrName(j).equalsIgnoreCase("param") )
            paramName = el.attrValue( j );
        else if( el.attrName(j).equalsIgnoreCase("tokenizer") )
            tokenizer = el.attrValue( j );
        else {
            throw new GeneralException( "Query router attribute " +
                el.name() + "." + el.attrName(j) + " not recognized" );
        }
    }
    
    // Make sure both specified.
    if( paramName == null || tokenizer == null )
        throw new GeneralException( el.name() + " element requires 'param' and 'tokenizer' attributes to be specified" );
    
    // Add it.
    tokenizerMap.put( paramName, tokenizer );
    
  } // parseTokenizer()
  
} // class QueryRoute
