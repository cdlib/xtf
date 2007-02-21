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

/**
 * This class implements a linked list, where the links are embedded within
 * the objects added to the list.<br><br>
 *
 * Why another linked list you ask? Why not use the java.util.LinkedList?
 * It depends on what you're doing with the list. Since the built-in LinkedList
 * doesn't keep links in the object, any operation that takes an object
 * as a parameter has to do a linear scan of the list to find it. So the
 * remove(object) operation is slow, and therefore moveToHead() and
 * moveToTail() are also slow (well, LinkedList doesn't have them directly.)
 * <br><br>
 *
 * In contrast, embedding the links in the object allows {@link #remove(Linkable)}
 * to run in constant rather than linear time.<br><br>
 *
 * The downside to embedding the links is that an object can only be in a
 * single EmbeddedList at one time.<br><br>
 *
 * Any object placed into an EmbeddedList must support the {@link Linkable}
 * interface. The easiest way to do this is to simply extend the
 * {@link LinkableImpl} class, and then no additional work is needed.
 */
public class EmbeddedList 
{
  /**
   * Add an object to the head of the list.
   *
   * @param l     The object to add. Note that it must not be in any other
   *              EmbeddedList.
   */
  public void addHead(Linkable l) 
  {
    if (l.getOwner() != null)
      throw new IllegalArgumentException();
    l.setOwner(this);
    assert l.getPrev() == null;
    assert l.getNext() == null;

    l.setPrev(null);
    l.setNext(head);

    if (head != null)
      head.setPrev(l);
    head = l;

    if (tail == null)
      tail = l;

    ++count;
  }

  /**
   * Add an object to the tail of the list.
   *
   * @param l     The object to add. Note that it must not be in any other
   *              EmbeddedList.
   */
  public void addTail(Linkable l) 
  {
    if (l.getOwner() != null)
      throw new IllegalArgumentException();
    l.setOwner(this);
    assert l.getPrev() == null;
    assert l.getNext() == null;

    l.setPrev(tail);
    l.setNext(null);

    if (tail != null)
      tail.setNext(l);
    tail = l;

    if (head == null)
      head = l;

    ++count;
  }

  /**
   * Get the first object in the list.
   *
   * @return  The object, or null if there are none in the list.
   */
  public Linkable getHead() {
    return head;
  }

  /**
   * Get the last object in the list.
   *
   * @return  The object, or null if there are none in the list.
   */
  public Linkable getTail() {
    return tail;
  }

  /**
   * Get a count of the number of objects in the list.
   *
   * @return  Number of objects.
   */
  public int getCount() {
    return count;
  }

  /**
   * Remove (and return) the first object in the list.
   *
   * @return  The first object in the list, or null if the list is empty.
   */
  public Linkable removeHead() {
    if (head == null)
      return null;
    else
      return remove(head);
  }

  /**
   * Remove (and return) the last object in the list.
   *
   * @return  The last object in the list, or null if the list is empty.
   */
  public Linkable removeTail() {
    if (tail == null)
      return null;
    else
      return remove(tail);
  }

  /**
   * Move the specified object to the head of the list (if it isn't
   * already there).
   */
  public void moveToHead(Linkable l) 
  {
    if (l.getOwner() != this)
      throw new IllegalArgumentException();

    if (l == head)
      return;

    addHead(remove(l));
  }

  /**
   * Move the specified object to the tail of the list (if it isn't
   * already there).
   */
  public void moveToTail(Linkable l) 
  {
    if (l.getOwner() != this)
      throw new IllegalArgumentException();

    if (l == tail)
      return;

    addTail(remove(l));
  }

  /**
   * Remove (and return) the specified object from the list. Happily,
   * unlike the standard LinkedList, this runs in constant time
   * (not linear time.)
   *
   * @return  The same object, useful for operator chaining.
   */
  public Linkable remove(Linkable l) 
  {
    Linkable prev = l.getPrev();
    Linkable next = l.getNext();

    if (l.getOwner() != this)
      throw new IllegalArgumentException();
    l.setOwner(null);

    if (prev != null) {
      assert head != l;
      prev.setNext(next);
    }
    else {
      assert head == l;
      head = next;
    }

    if (next != null) {
      assert tail != l;
      next.setPrev(prev);
    }
    else {
      assert tail == l;
      tail = prev;
    }

    l.setPrev(null);
    l.setNext(null);

    --count;

    return l;
  }

  /** Reference to the first object in the list, or null if empty */
  private Linkable head;

  /** Reference to the last object in the list, or null if empty */
  private Linkable tail;

  /** How many objects are currently in the list */
  private int count = 0;
} // class EmbeddedList
