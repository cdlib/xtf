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
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Maintains a list of key/value pairs. Can be easily iterated over or
 * searched by key.
 */
public class AttribList 
{
  /**
   * The list is stored as a linked list. Not so fast to iterate, but fast
   * to add/remove.
   */
  private LinkedList list = new LinkedList();

  /**
   * Add a key/value pair to the list. Note: does not check for duplicates!
   *
   * @param key       Key identifier
   * @param value     Value to associate with that key
   */
  public void put(String key, String value) {
    list.add(new Attrib(key, value));
  }

  /**
   * Retrieves the value associated with the given key, or null if not
   * present.
   */
  public String get(String key) 
  {
    for (Iterator iter = iterator(); iter.hasNext();) {
      Attrib att = (Attrib)iter.next();
      if (att.key.equals(key))
        return att.value;
    }
    return null;
  }

  /** Get an iterator on the list */
  public Iterator iterator() {
    return list.iterator();
  }

  /** Remove all key/value pairs from the list */
  public void clear() {
    list.clear();
  }

  /** Check if the list is empty */
  public boolean isEmpty() {
    return list.isEmpty();
  }

  /** Return the number of key/value pairs in the list */
  public int size() {
    return list.size();
  }
} // class AttribList
