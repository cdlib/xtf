package org.cdlib.xtf.textIndexer;


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
 */
import java.io.File;
import java.io.IOException;
import org.cdlib.xtf.util.Path;
import org.cdlib.xtf.util.StructuredFile;
import org.cdlib.xtf.util.StructuredStore;
import org.cdlib.xtf.util.SubStoreReader;
import org.cdlib.xtf.util.SubStoreWriter;

/*
 * This file created on Mar 22, 2005 by Martin Haye
 */

/**
 * Used to put off actually creating a structured store until it is needed.
 * Essentially, all methods are delegated to a StructuredFile that is created
 * when the first time a method is called. The file is released after a
 * close() or delete() operation.
 *
 * @author Martin Haye
 */
public class StructuredFileProxy implements StructuredStore 
{
  private File path;
  private StructuredFile realStore = null;

  public StructuredFileProxy(File path) {
    this.path = path;
  }

  public SubStoreWriter createSubStore(String name)
    throws IOException 
  {
    return realStore().createSubStore(name);
  }

  public SubStoreReader openSubStore(String name)
    throws IOException 
  {
    return realStore().openSubStore(name);
  }

  public void close()
    throws IOException 
  {
    if (realStore != null)
      realStore.close();
    realStore = null;
    path = null;
  }

  public void delete()
    throws IOException 
  {
    if (realStore != null)
      realStore.delete();
    realStore = null;
    path = null;
  }

  public String getSystemId() {
    return realStore().getSystemId();
  }

  public void setUserVersion(String ver)
    throws IOException 
  {
    realStore().setUserVersion(ver);
  }

  public String getUserVersion() {
    return realStore().getUserVersion();
  }

  private StructuredFile realStore() 
  {
    try 
    {
      if (realStore == null) {
        if (path.canRead())
          path.delete();
        Path.createPath(path.getParent());
        realStore = StructuredFile.create(path);
      }
      return realStore;
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
} // class StructuredFileProxy
