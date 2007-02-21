package org.cdlib.xtf.dynaXML.test;


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
import java.io.IOException;
import javax.servlet.ServletException;
import org.cdlib.xtf.test.FakeOutputStream;
import org.cdlib.xtf.test.FakeServletConfig;
import org.cdlib.xtf.test.FakeServletContext;
import org.cdlib.xtf.test.FakeServletRequest;
import org.cdlib.xtf.test.FakeServletResponse;

/**
 * This class is used to perform an end-to-end test of the LazyTree/SearchTree
 * system.
 *
 * @author Martin Haye
 */
public class SearchTest 
{
  /** The servlet used in the test */
  protected TestableDynaXML dynaXML;

  /** Base directory for relative file paths */
  protected String baseDir;

  /**
   * Default constructor
   *
   * @param baseDir   The directory that the servlet normally calls home.
   */
  public SearchTest(String baseDir)
    throws ServletException 
  {
    this.baseDir = baseDir;

    // Make sure assertions are enabled
    boolean ok = false;
    assert (ok = true) == true;
    if (!ok)
      throw new RuntimeException("Assertions must be enabled.");

    // Create an instance of the servlet
    dynaXML = new TestableDynaXML();
    FakeServletContext context = new FakeServletContext();
    dynaXML.init(new FakeServletConfig(context, baseDir, "dynaXML"));
  } // constructor

  /**
   * Randomly re-orders the elements of an array.
   */
  public static void randomizeArray(Object[] array) 
  {
    for (int i = array.length - 1; i > 0; i--) {
      int pos = (int)(Math.random() * (i + 1));
      Object tmp = array[pos];
      array[pos] = array[i];
      array[i] = tmp;
    }
  } // randomizeArray()

  /**
   * Run a request through DynaXML
   */
  public String runDynaXML(String url)
    throws IOException, ServletException 
  {
    // Make up a request.
    FakeServletRequest req = new FakeServletRequest(url);

    // And make a place for the response.
    FakeOutputStream out = new FakeOutputStream();
    FakeServletResponse res = new FakeServletResponse(out);

    // Go for it!
    dynaXML.doGet(req, res);

    // And we're done.
    return out.toString();
  } // runDynaXML()
} // class SearchTest
