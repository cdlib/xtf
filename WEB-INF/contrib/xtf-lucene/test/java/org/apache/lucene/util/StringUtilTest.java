package org.apache.lucene.util;

/**
 * Copyright (c) 2007, Regents of the University of California
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

import java.util.Arrays;

import junit.framework.TestCase;

/** Test the {@link StringUtil} class */
public class StringUtilTest extends TestCase
{
  public void testJoin()
  {
    String[] array = new String[] { "abc", "def", "ghi" };
    assertEquals(StringUtil.join(array), "abc def ghi");
    assertEquals(StringUtil.join(array, " -> "), "abc -> def -> ghi");
    assertEquals(StringUtil.join(new String[0]), "");
    assertEquals(StringUtil.join(null), null);
  }

  public void testPad()
  {
    assertEquals(StringUtil.padEnd("foo", 3), "foo");
    assertEquals(StringUtil.padEnd("foo", 5), "foo  ");
    assertEquals(StringUtil.padEnd("foo", 5, 'x'), "fooxx");
    assertEquals(StringUtil.justifyLeft("foo", 3), "foo");
    assertEquals(StringUtil.justifyLeft("foo", 5), "foo  ");

    assertEquals(StringUtil.padStart("foo", 3), "foo");
    assertEquals(StringUtil.padStart("foo", 5), "  foo");
    assertEquals(StringUtil.padStart("foo", 5, 'x'), "xxfoo");
    assertEquals(StringUtil.justifyRight("foo", 3), "foo");
    assertEquals(StringUtil.justifyRight("foo", 5), "  foo");
  }

  public void testCase()
  {
    assertTrue(StringUtil.isUpperCase("ABC"));
    assertTrue(StringUtil.isUpperCase("X"));
    assertFalse(StringUtil.isUpperCase("abc"));
    assertFalse(StringUtil.isUpperCase("Abc"));
    assertFalse(StringUtil.isUpperCase("1"));
    assertFalse(StringUtil.isUpperCase(""));
   
    assertTrue(StringUtil.isLowerCase("abc"));
    assertTrue(StringUtil.isLowerCase("x"));
    assertFalse(StringUtil.isLowerCase("ABC"));
    assertFalse(StringUtil.isLowerCase("aBC"));
    assertFalse(StringUtil.isLowerCase("1"));
    assertFalse(StringUtil.isLowerCase(""));
   
    assertTrue(StringUtil.isTitleCase("Abc"));
    assertFalse(StringUtil.isTitleCase("abc"));
    assertFalse(StringUtil.isTitleCase("ABC"));
    assertFalse(StringUtil.isTitleCase("X"));
    assertFalse(StringUtil.isTitleCase("x"));
    assertFalse(StringUtil.isTitleCase("1"));
    assertFalse(StringUtil.isLowerCase(""));

    assertTrue(StringUtil.isTitleCase("Abc Def"));
    assertFalse(StringUtil.isTitleCase("Abc def"));
    assertFalse(StringUtil.isTitleCase("abc Def"));

    assertEquals(StringUtil.toTitleCase("Abc"), "Abc");
    assertEquals(StringUtil.toTitleCase("ABC"), "Abc");
    assertEquals(StringUtil.toTitleCase("abc"), "Abc");
    assertEquals(StringUtil.toTitleCase("x"), "X");
    assertEquals(StringUtil.toTitleCase("X"), "X");
    assertEquals(StringUtil.toTitleCase("1"), "1");
    
    assertEquals(StringUtil.toTitleCase("Abc Def"), "Abc Def");
    assertEquals(StringUtil.toTitleCase("ABC def"), "Abc Def");
    assertEquals(StringUtil.toTitleCase("abc DEF"), "Abc Def");

    assertEquals(StringUtil.copyCase("Abc", "Xyz"), "Xyz");
    assertEquals(StringUtil.copyCase("Abc", "xyz"), "Xyz");
    assertEquals(StringUtil.copyCase("Abc", "XYZ"), "Xyz");
    
    assertEquals(StringUtil.copyCase("ABC", "Xyz"), "XYZ");
    assertEquals(StringUtil.copyCase("ABC", "xyz"), "XYZ");
    assertEquals(StringUtil.copyCase("ABC", "XYZ"), "XYZ");

    assertEquals(StringUtil.copyCase("abc", "Xyz"), "xyz");
    assertEquals(StringUtil.copyCase("abc", "xyz"), "xyz");
    assertEquals(StringUtil.copyCase("abc", "XYZ"), "xyz");

    assertEquals(StringUtil.copyCase("1", "Xyz"), "Xyz");
    assertEquals(StringUtil.copyCase("1", "xyz"), "xyz");
    assertEquals(StringUtil.copyCase("1", "XYZ"), "XYZ");

    assertEquals(StringUtil.copyCase("abc Def", "Xyz"), "Xyz");
    assertEquals(StringUtil.copyCase("abc Def", "xyz"), "xyz");
    assertEquals(StringUtil.copyCase("abc Def", "XYZ"), "XYZ");
}
  
  public void testSplitWords()
  {
    assertTrue(Arrays.deepEquals(StringUtil.splitWords("abc def ghi"),
                                 new String[] {"abc", "def", "ghi"}));
    assertTrue(Arrays.deepEquals(StringUtil.splitWords("a b c"),
                                 new String[] {"a", "b", "c"}));
    assertTrue(Arrays.deepEquals(StringUtil.splitWords("abc"),
                                 new String[] {"abc"}));
    assertTrue(Arrays.deepEquals(StringUtil.splitWords(""),
                                 new String[0]));
    assertTrue(Arrays.deepEquals(StringUtil.splitWords(null),
                                 new String[0]));
  }

}
