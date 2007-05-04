package org.cdlib.xtf.util;

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

import java.lang.reflect.Method;

/**
 * Handles Unicode normalization, dynamically choosing whichever of the built-in
 * Java classes is available to do the work (these changed between Java 1.5 and
 * Java 1.6).
 * 
 * @author Martin Haye
 */
public class Normalizer
{
//  /** This will do the actual work, depending on the platform * 
  private static PlatformNormalizer platformNormalizer = null;

  /**
   * Perform normalization on a string, meaning canonical decomposition
   * followed by canonical composition.
   */
  public static String normalize(String in)
  {
    if (platformNormalizer == null) {
      try {
        platformNormalizer = new Jdk16Normalizer();
      }
      catch (Exception e) {
        try {
          platformNormalizer = new Jdk15Normalizer();
        }
        catch (Exception e2) {
          throw new RuntimeException(e2);
        }
      }
    }

    return platformNormalizer.normalize(in);
  }

  /** Generic interface for normalizers */
  private interface PlatformNormalizer
  {
    String normalize(String in);
  }

  /** Normalizer that runs on JDK 1.6 / 6.0 and higher */
  private static class Jdk16Normalizer implements PlatformNormalizer
  {
    private Method method;
    private Object form;

    /** Constructor - use Java Reflection to locate the class and method */
    public Jdk16Normalizer() throws Exception
    {
      Class normalizerClass = Class.forName("java.text.Normalizer");
      Class formClass = Class.forName("java.text.Normalizer$Form");

      method = normalizerClass.getMethod("normalize", new Class[] {
          CharSequence.class, formClass });

      form = formClass.getField("NFC").get(null);
    }

    /** Normalize a string using the method we found */
    public String normalize(String in)
    {
      try {
        return (String) method.invoke(null, new Object[] { (CharSequence)in, form });
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  /** Normalizer that runs on JDK 1.5 / 5.0 */
  private static class Jdk15Normalizer implements PlatformNormalizer
  {
    private Method method;
    private Object mode;

    /** Constructor - use Java Reflection to locate the class and method */
    public Jdk15Normalizer() throws Exception
    {
      Class normalizerClass = Class.forName("sun.text.Normalizer");
      Class modeClass = Class.forName("sun.text.Normalizer$Mode");

      method = normalizerClass.getMethod("normalize", new Class[] {
          String.class, modeClass, int.class });

      mode = normalizerClass.getField("COMPOSE").get(null);
    }

    /** Normalize a string using the method we found */
    public String normalize(String in)
    {
      try {
        return (String) method.invoke(null, new Object[] { in, mode, 0 });
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
