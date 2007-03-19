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
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.lucene.util.Prime;

/**
 * Creates a persistent string to byte buffer hash table on disk, optimized
 * for quick access. It can be read back later using a
 * {@link DiskHashReader}.
 *
 * @author Martin Haye
 */
public class DiskHashWriter 
{
  /** Keeps track of entries in memory until we're ready to write to disk */
  private HashMap memMap = new HashMap(100);

  /**
   * Add a new key/value pair to the hash.
   */
  public void put(String key, PackedByteBuf val) 
  {
    // We don't allow zero-length strings, because that's how an empty
    // hash slot is denoted.
    //
    if (key.length() == 0)
      key = " ";

    memMap.put(key, val.clone());
  } // put()

  /** Writes out the entire hash */
  public void outputTo(SubStoreWriter out)
    throws IOException 
  {
    // Calculate a good size for the hash. We want to have plenty of open
    // spaces to avoid excessive collisions.
    //
    int nItems = memMap.size();
    int hashSize = Prime.findAfter(nItems * 2);

    // Throw all the added entries into the hash.
    PackedByteBuf[] slots = new PackedByteBuf[hashSize];
    int maxSlotSize = 0;

    for (Iterator iter = memMap.keySet().iterator(); iter.hasNext();) 
    {
      String key = (String)iter.next();
      PackedByteBuf val = (PackedByteBuf)memMap.get(key);

      // Add it to the correct slot.
      int slotNum = (key.hashCode() & 0xffffff) % hashSize;
      if (slots[slotNum] == null)
        slots[slotNum] = new PackedByteBuf(val.length() + key.length() + 5);
      slots[slotNum].writeString(key);
      slots[slotNum].writeBuffer(val);
    }

    // Finish all the slots.
    for (int i = 0; i < hashSize; i++) 
    {
      if (slots[i] == null)
        continue;

      slots[i].writeString(""); // Marks end of slot
      maxSlotSize = Math.max(maxSlotSize, slots[i].length());
    }

    // Now write the header and the slot offsets.
    out.write("hash".getBytes());
    out.writeInt(hashSize);
    out.writeInt(maxSlotSize);
    assert DiskHashReader.headerSize == (int)out.length();
    int startOffset = (int)out.length() + (hashSize * 4);
    int curOffset = startOffset;

    for (int i = 0; i < hashSize; i++) 
    {
      if (slots[i] == null) {
        out.writeInt(0);
        continue;
      }
      out.writeInt(curOffset);
      curOffset += slots[i].length();
      assert slots[i].length() <= maxSlotSize;
    } // for i
    assert out.length() == startOffset;

    // Finally, write all the data.
    for (int i = 0; i < hashSize; i++) {
      if (slots[i] == null)
        continue;
      slots[i].output(out);
    }
    assert out.length() == curOffset;

    // To make sure that the hash reader doesn't have to worry about
    // accidentally reading past the end of the sub-file, write an extra
    // block of bytes.
    //
    out.write(new byte[maxSlotSize]);

    // All done!
    out.close();
  } // outputTo()

  // Perform a basic regression test on the DiskHash system. Writes a file
  // in the current directory during the test, but erases it on completion.
  //
  public static final Tester tester = new Tester("DiskHash") 
  {
    protected void testImpl()
      throws Exception 
    {
      // Since we depend on StructuredFile, make sure it passes.
      StructuredFile.tester.test();

      File testFile = new File("DiskHashTest.sf");
      StructuredFile f = null;

      try 
      {
        // Create a structured file to hold the hash.
        f = StructuredFile.create(testFile);

        // Make a test hash.
        DiskHashWriter w = new DiskHashWriter();

        PackedByteBuf buf = new PackedByteBuf(20);
        buf.writeInt(11);
        buf.writeString("hello");

        w.put("foo", buf);

        buf.reset();
        buf.writeInt(22);
        buf.writeString("kangaroo");

        w.put("bar", buf);

        w.outputTo(f.createSubStore("testhash"));

        DiskHashReader r = new DiskHashReader(f.openSubStore("testhash"));

        buf = r.find("bar");
        assert buf != null;
        assert buf.readInt() == 22;
        assert buf.readString().equals("kangaroo");

        buf = r.find("foo");
        assert buf != null;
        assert buf.readInt() == 11;
        assert buf.readString().equals("hello");
        assert r.find("xyz") == null;
      }
      finally {
        // All done. Close and clean up our file.
        if (f != null)
          f.close();
        testFile.delete();
      }
    } // testImpl()
  };
} // class DiskHashWriter
