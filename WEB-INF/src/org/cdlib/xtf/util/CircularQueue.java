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
 * A simple queue of fixed size, that provides very fast insertion, deletion,
 * and scanning.
 * 
 * @author Martin Haye
 */
public class CircularQueue
{
  /** Max # of entries in the queue */
  private int      maxSize;
  
  /** The current queue entries */
  private Object[] entries;
  
  /** Points to the current bottom entry */
  private int      bottom;
  
  /** Number of entries currently in the queue */
  private int      count;

  /**
   * Construct a queue that can hold, at most, 'maxSize' entries.
   * 
   * @param maxSize   Maximum number of entries in the queue
   */
  public CircularQueue( int maxSize )
  {
    this.maxSize = maxSize;
    entries = new Object[maxSize];
  } // constructor
  
  /**
   * Add an object to the end of the queue. If the queue is full, the first
   * object is removed to make room.
   * 
   * @param obj   Object to add.
   */
  public final void addTail( Object obj )
  {
    if( count == maxSize )
        removeHead();
    
    int next = (bottom + count >= maxSize) ? 
                    (bottom + count - maxSize) : (bottom + count);
    entries[next] = obj;
    count++;
  } // addTail()
  
  /**
   * Add an object to the start of the queue. If the queue is full, the last
   * object is removed to make room.
   * 
   * @param obj   Object to add.
   */
  public final void addHead( Object obj )
  {
    if( count == maxSize )
        removeTail();
    
    bottom = (bottom == 0) ? (maxSize - 1) : (bottom - 1);
    entries[bottom] = obj;
    count++;
  } // addTail()
  
  /**
   * Removes and returns the first object in the queue.
   * 
   * @return  The object that was at the head, or null if the queue is empty.
   */
  public final Object removeHead()
  {
    if( count == 0 )
        return null;

    int first = bottom;
    if( bottom == maxSize - 1 )
        bottom = 0;
    else
        bottom++;

    count--;
    
    return entries[first];
  } // removeHead()
  
  /**
   * Removes and returns the last object in the queue.
   * 
   * @return  The object that was at the tail, or null if the queue is empty.
   */
  public final Object removeTail()
  {
    if( count == 0 )
        return null;

    int last = bottom + count - 1;
    if( last == maxSize )
        last = 0;
    
    count--;
    
    return entries[last];
  } // removeHead()
  
  /**
   * Peek into the queue but do not remove the object.
   * 
   * @param distance  How far to peek into the queue (zero means peek at the
   *                  head, one to peek at head+1, etc.)
   * 
   * @return  The object at the specified position, or null if queue is empty
   */
  public final Object peek( int distance )
  {
    if( count == 0 )
        return null;
    
    if( distance >= count )
        return null;
    
    int n = bottom + distance;
    if( n >= maxSize )
        n -= maxSize;
    
    return entries[n];
  } // peek()
  
  /**
   * Removes all entries from the queue.
   */
  public final void clear()
  {
    bottom = count = 0;
  } // clear()
  
  /**
   * Counts how many items are currently in the queue
   * 
   * @return  Number of items in the queue
   */
  public final int count()
  {
    return count;
  } // count()
  
  /**
   * Checks whether the queue is currently empty
   * 
   * @return    true if empty, false if anything is queued
   */
  public final boolean isEmpty()
  {
    return (count == 0);
  } // isEmpty()
  
  /**
   * Checks whether the queue is currently full
   * 
   * @return  true if full, false if there is room for more entries
   */
  public final boolean isFull()
  {
    return count == maxSize;
  } // isFull()
  
  /**
   * Basic regression test
   */
  public static final Tester tester = new Tester("CircularQueue") {
    protected void testImpl() {
        CircularQueue queue = new CircularQueue( 3 );
        
        Object one   = IntegerValues.valueOf( 1 );
        Object two   = IntegerValues.valueOf( 2 );
        Object three = IntegerValues.valueOf( 3 );
        Object four  = IntegerValues.valueOf( 4 );

        assert queue.count() == 0;
        assert queue.isEmpty();
        assert queue.removeHead() == null;
        assert queue.removeTail() == null;
        assert queue.peek(0) == null;
        assert queue.peek(1) == null;

        queue.addTail( one );
        queue.addTail( two );
        
        assert !queue.isEmpty();
        assert queue.count() == 2;
        assert queue.peek( 0 ) == one;
        assert queue.peek( 1 ) == two;
        
        queue.addTail( three );
        queue.addTail( four );
        
        assert queue.count() == 3;
        assert queue.peek(0) == two;
        assert queue.peek(1) == three;
        assert queue.peek(2) == four;
        assert queue.peek(3) == null;

        queue.addHead( one );
        assert queue.count() == 3;
        assert queue.peek(0) == one;
        assert queue.peek(1) == two;
        assert queue.peek(2) == three;
        assert queue.peek(3) == null;

        assert queue.removeHead() == one;
        assert queue.removeTail() == three;
        assert queue.count() == 1;
        assert queue.peek(0) == two;
        assert queue.peek(1) == null;
        
        queue.addHead( one );
        queue.addTail( three );
        assert queue.count() == 3;
        assert queue.peek(0) == one;
        assert queue.peek(1) == two;
        assert queue.peek(2) == three;
        assert queue.peek(3) == null;
        
    } // testImpl()
  };

} // class CircularQueue
