package org.apache.lucene.util;

/*
 * Copyright 2007 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Random;

import junit.framework.TestCase;

/**
 * Test the {@link IntList} class.
 * 
 * @author Martin Haye
 */
public class IntListTest extends TestCase
{
  // Test of basic methods like add, set, clear
  public void testAdd()
  {
    IntList list = new IntList(2);
    assertTrue(list.isEmpty());

    // Test adding
    list.add(1);
    assertFalse(list.isEmpty());

    list.add(2);
    assertEquals(2, list.size());
    assertEquals(1, list.get(0));
    assertEquals(2, list.get(1));
    assertEquals(2, list.getLast());
    
    list.add(3);
    assertEquals(3, list.size());
    assertEquals(3, list.get(2));
    assertEquals(3, list.getLast());
    
    // Make sure compact() doesn't affect anything
    list.compact();
    assertEquals(3, list.size());
    assertEquals(1, list.get(0));
    assertEquals(2, list.get(1));
    assertEquals(3, list.get(2));
    
    // Change one element
    list.set(1, 89);
    assertEquals(89, list.get(1));

    // Make an array
    int[] array = list.toArray();
    assertEquals(3, array.length);
    assertEquals(1, array[0]);
    assertEquals(89, array[1]);
    assertEquals(3, array[2]);
    
    // Fill every slot
    list.fill(9);
    assertEquals(9, list.get(0));
    assertEquals(9, list.get(1));
    assertEquals(9, list.get(2));
    
    // Clear
    list.clear();
    assertEquals(0, list.size());
    assertTrue(list.isEmpty());
  }
  
  // Test a small list.
  public void testSmall()
  {
    IntList list = new IntList(0);
    list.add(1);
    assertFalse(list.isEmpty());

    list.add(2);
    assertEquals(2, list.size());
    assertEquals(1, list.get(0));
    assertEquals(2, list.get(1));
    assertEquals(2, list.getLast());
    
    list = new IntList(1);
    list.add(1);
    assertFalse(list.isEmpty());

    list.add(2);
    assertEquals(2, list.size());
    assertEquals(1, list.get(0));
    assertEquals(2, list.get(1));
    assertEquals(2, list.getLast());
  }
  
  // Test of sort and search methods
  public void testSort() 
  {
    IntList list = new IntList();
    final int SIZE = 10000;
    Random rand = new Random(1);
    
    // First, try a straight sort
    for (int i = 0; i < SIZE; i++)
      list.add(Math.abs(rand.nextInt()));
    
    list.sort();
    for (int i = 1; i < SIZE; i++)
      assertTrue(list.get(i - 1) <= list.get(i));
    
    // Clear out, and try a remapping sort.
    list.clear();
    for (int i = 0; i < SIZE; i++)
      list.add(Math.abs(rand.nextInt()));

    int[] map = list.calcSortMap();
    for (int i = 1; i < SIZE; i++)
      assertTrue(list.get(map[i - 1]) <= list.get(map[i]));

    list.remap(map);
    for (int i = 1; i < SIZE; i++)
      assertTrue(list.get(i - 1) <= list.get(i));
    
    // Test binary searching
    assertEquals(list.get(0), list.get(list.binarySearch(list.get(0))));
    assertEquals(list.get(SIZE*1/3), list.get(list.binarySearch(list.get(SIZE*1/3))));
    assertEquals(list.get(SIZE*2/3), list.get(list.binarySearch(list.get(SIZE*2/3))));
    assertEquals(list.get(SIZE-1), list.get(list.binarySearch(list.get(SIZE-1))));
  }
}
