package org.cdlib.xtf.textIndexer;

/**
 * Copyright (c) 2009, Regents of the University of California
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import org.cdlib.xtf.util.Trace;

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

/**
 * This class represents the contents of the Document Selector Cache maintained
 * by the indexer. It provides for loading, saving, and searching the cache.
 * The cache is underlain by a HashMap mapping String keys to Entry values.
 */
public class DocSelCache extends HashMap
{
  public String dependencies;
  public HashMap<String, Entry> map = new HashMap<String, Entry>();
  public boolean modified = true;
  
  /** Load a previously saved docSelector cache.
   *
   * @param  file        The file to load from.
   * @throws IOException If something goes wrong reading the file.
   */
  public void load(File file) throws IOException
  {
    clear();
    
    // Open the file and read it.
    FileInputStream fis = null;
    InflaterInputStream iis = null;
    ObjectInputStream ois = null;
    
    try 
    {
      fis = new FileInputStream(file);
      iis = new InflaterInputStream(fis);
      ois = new ObjectInputStream(iis);

      String fileVersion = ois.readUTF();
      if (!fileVersion.equals("docSelectorCache v2.2")) {
        Trace.warning("Unrecognized docSelector cache \"" + file + "\"");
        return;
      }

      // Read the dependencies.
      dependencies = ois.readUTF();
      
      // And load the map.
      map = (HashMap)ois.readObject();
      
      // Remember that it's not modified yet.
      modified = false;
    } catch (ClassNotFoundException e) {
      throw new IOException(e.getMessage());
    }
    finally {
      if (ois != null)
        try { ois.close(); } catch(Exception e) { /*ignore*/ }
      if (iis != null)
        try { iis.close(); } catch(Exception e) { /*ignore*/ }
      if (fis != null)
        try { ois.close(); } catch(Exception e) { /*ignore*/ }
    }
  } // load()

  ////////////////////////////////////////////////////////////////////////////

  /** Save the docSelector cache.
   * @throws IOException 
   */
  public void save(File file) throws IOException 
  {
    // Skip if not modified.
    if (!modified)
      return;
    
    // Let's keep the old file intact until the new one is ready.
    File newFile = new File(file.toString() + ".new");
    FileOutputStream fos = null;
    DeflaterOutputStream dos = null;
    ObjectOutputStream oos = null;

    try 
    {
      // First, open the new file.
      fos = new FileOutputStream(newFile);
      dos = new DeflaterOutputStream(fos);
      oos = new ObjectOutputStream(dos);

      // Write the version info first.
      oos.writeUTF("docSelectorCache v2.2");

      // Next, write the current stylesheet dependency info.
      oos.writeUTF(dependencies);

      // Now write the mapping.
      oos.writeObject(map);

      // All done. Close the new file.
      oos.close();
      dos.close();
      fos.close();

      // Get rid of the old file, and rename the new one.
      file.delete();
      newFile.renameTo(file);
    }
    catch (IOException e) {
      newFile.delete();
      throw e;
    }
    finally {
      if (oos != null)
        try { oos.close(); } catch (Exception e2) { /*ignore*/ }
      if (fos != null)
        try { fos.close(); } catch (Exception e2) { /*ignore*/ }
    }
  } // save()
  
  ////////////////////////////////////////////////////////////////////////////
  // Delegated methods 
  ////////////////////////////////////////////////////////////////////////////

  /** Delegated to underlying map. */
  public void clear() {
    modified = true;
    map.clear();
  }

  /** Delegated to underlying map. */
  public boolean containsKey(String key) {
    return map.containsKey(key);
  }

  /** Delegated to underlying map. */
  public Entry get(String key) {
    return map.get(key);
  }

  /** Delegated to underlying map. */
  public Set<String> keySet() {
    return map.keySet();
  }

  /** Delegated to underlying map. */
  public Entry put(String key, Entry value) {
    modified = true;
    return map.put(key, value);
  }

  /** Delegated to underlying map. */
  public Entry remove(Object key) {
    modified = true;
    return map.remove(key);
  }

  /** Delegated to underlying map. */
  public int size() {
    return map.size();
  }

  /** Delegated to underlying map. */
  public Set<Map.Entry<String, DocSelCache.Entry>> entrySet() {
    return map.entrySet();
  }
  
  ////////////////////////////////////////////////////////////////////////////

  /** One entry in the docSelector cache */
  static class Entry implements Serializable 
  {
    String  filesAndTimes;
    long    scanTime;
    boolean anyProcessed;

    Entry() { // only used by serialization
    }

    Entry(String filesAndTimes, boolean anyProcessed) {
      this.filesAndTimes = filesAndTimes;
      this.scanTime = System.currentTimeMillis();
      this.anyProcessed = anyProcessed;
    }
  } // class CacheEntry

} // class DocSelCache
