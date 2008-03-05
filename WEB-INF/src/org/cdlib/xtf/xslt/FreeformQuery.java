package org.cdlib.xtf.xslt;

/*
 * Copyright (c) 2008, Regents of the University of California
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

import java.io.StringReader;
import javax.xml.transform.stream.StreamSource;
import org.cdlib.xtf.textEngine.freeform.FreeformQueryParser;
import org.cdlib.xtf.textEngine.freeform.ParseException;
import org.cdlib.xtf.textEngine.freeform.FreeformQueryParser.FNode;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.AllElementStripper;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.tinytree.TinyBuilder;
import net.sf.saxon.trans.XPathException;

/**
 * Utility function to parse a "freeform" google-style query into XTF compatible
 * format.
 *
 * @author Martin Haye
 */
public class FreeformQuery 
{
  /**
   * Driver for calling from Saxon. Returns result as a traversible XML tree.
   */
  public static NodeInfo parse(XPathContext context, String queryStr) 
    throws ParseException
  {
    FreeformQueryParser parser = new FreeformQueryParser(new StringReader(queryStr));
    FNode parsed = parser.Query();
    String strVersion = parsed.toXML();
    StreamSource src = new StreamSource(new StringReader(strVersion));
    try {
      return TinyBuilder.build(src, new AllElementStripper(), context.getConfiguration());
    }
    catch (XPathException e) {
      throw new RuntimeException(e);
    }
  }
}
