package org.cdlib.xtf.crossQuery.test;

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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cdlib.xtf.crossQuery.CrossQuery;
import org.cdlib.xtf.util.Trace;

/**
 * Derived version of the crossQuery servlet, used to abuse crossQuery during
 * load tests. The only difference is that it throws exceptions upward instead
 * of formatting an error page. This ensures that exceptions don't get hidden
 * in the noise. 
 * 
 * @author Martin Haye
 */
public class TestableCrossQuery extends CrossQuery
{
  /**
  * Generate an error page based on the given exception. Utilizes the system
  * error stylesheet to produce a nicely formatted HTML page.
  *
  * @param req  The HTTP request we're responding to
  * @param res  The HTTP result to write to
  * @param exc  The exception producing the error. If it's a
  *             DynaXMLException, the attributes will be passed to
  *             the error stylesheet.
  */
  protected void genErrorPage( HttpServletRequest  req, 
                               HttpServletResponse res, 
                               Exception           exc )
  {
    Trace.error( "Exception occurred in crossQuery: " + exc );
    throw new RuntimeException( exc );
  } // genErrorPage()

} // class TestableCrossQuery
