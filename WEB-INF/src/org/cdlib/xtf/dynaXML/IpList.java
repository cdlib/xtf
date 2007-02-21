package org.cdlib.xtf.dynaXML;


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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeMap;
import javax.servlet.ServletOutputStream;

/**
 * Loads and provides quick access to a map of IP addresses. Reads a file
 * containing IP addresses and ranges, as well as excluded addresses, and
 * provides a way to check if a given IP address matches.
 */
class IpList 
{
  /**
   * Data class to keep track of the four numeric components of an IP
   * address.
   */
  private class IpAddr 
  {
    public int[] components = new int[4];

    /** Constructs a blank IP address structure */
    IpAddr() {
    }

    /** Constructs an IP address from the four numeric components */
    IpAddr(int c0, int c1, int c2, int c3) {
      components[0] = c0;
      components[1] = c1;
      components[2] = c2;
      components[3] = c3;
    } // IpAddr()

    /**
     * Checks if the first 'nComps' components of this IP address are
     * equal to the specified one.
     *
     * @param other     The IP address to compare to
     * @param nComps    How many components to compare (1-4)
     */
    boolean componentsEqual(IpAddr other, int nComps) 
    {
      for (int i = 0; i < nComps; i++) {
        if (components[i] != other.components[i])
          return false;
      }
      return true;
    } // componentsEqual()

    /**
     * Parses a string of the form "aaa.bbb.ccc.ddd", replacing the
     * contents of this IP address structure with the results. The
     * components don't need all three digits to be valid.
     *
     * @param str   String to parse
     *
     * @return      The remainder that wasn't part of the IP address,
     *              or null if no IP address could be parsed.
     */
    String parse(String str) 
    {
      // Parse each component.
      int pos = 0;
      for (int compNum = 0; compNum < 4; compNum++) 
      {
        // Get the next character. If none, that's an error.
        if (pos >= str.length())
          return null;
        char c = str.charAt(pos++);

        // If it's a '*', that means 'any' to us.
        if (c == '*')
          components[compNum] = 999;

        // If it's a digit, process the number.
        else if (Character.isDigit(c)) 
        {
          // Form the whole number.
          StringBuffer numStr = new StringBuffer();
          numStr.append(c);
          while (pos < str.length()) {
            c = str.charAt(pos);
            if (!Character.isDigit(c))
              break;
            numStr.append(c);
            pos++;
          }

          // Make sure it's 0-255.
          int num = Integer.parseInt(numStr.toString());
          if (num > 255)
            return null;

          // Record it.
          components[compNum] = num;
        } // else if

        // Any other character is an error.
        else
          return null;

        // If this isn't the last component, we expect a '.'
        // separator next.
        //
        if (compNum < 3) {
          if (pos >= str.length())
            return null;
          c = str.charAt(pos++);
          if (c != '.')
            return null;
        }
      } // for compNum

      // Return the unparsed remainder of the string.
      return str.substring(pos);
    } // parse()
  } // class IpAddr

  /**
   * Data structure to keep track of a range of IP addresses and whether
   * they are "positive" or "negative". Provides a way to check if an IP
   * address falls within the range.
   */
  private class IpRange 
  {
    /** Start of the range */
    public IpAddr startAddr;

    /** End of the range (can be equal to startAddr) */
    public IpAddr endAddr;

    /**
     * true if the range is specified IP address to include, false if
     * it specifies addresses to exclude.
     */
    public boolean isPositive;

    /**
     * Construct an IP range.
     *
     * @param _start     Starting address
     * @param _end       Ending address (can equal _start)
     * @param _isPos     true if range specified IPs to include, false to
     *                   specify IPs to exclude.
     */
    IpRange(IpAddr _start, IpAddr _end, boolean _isPos) {
      startAddr = _start;
      endAddr = _end;
      isPositive = _isPos;
    }

    /** Checks if a specified IP address falls within the range. */
    boolean matches(IpAddr addr) 
    {
      // Try to match each component in turn.
      for (int i = 0; i < 4; i++) 
      {
        int min = startAddr.components[i];
        int max = endAddr.components[i];
        if (min > max) {
          int tmp = min;
          min = max;
          max = tmp;
        }

        // Wildcards always match.
        if (min == 999)
          continue;

        // Check the range.
        if (addr.components[i] < min)
          return false;
        if (addr.components[i] > max)
          return false;
      } // for i

      // All components matched!
      return true;
    } // matches()
  } // class IpRange

  /**
   * Compares IP ranges to other ranges, or to IP addresses. Used to maintain
   * a sorted map of ranges for quick searching.
   */
  private class IpRangeComparator implements Comparator 
  {
    /**
     * Compare two ranges, or a range and an address, or two addresses.
     *
     * If comparing two ranges, the start addresses are compared first; if
     * equal, then the end addresses are compared.
     *
     * If comparing a range to an address, returns -1 if the address falls
     * within the range; otherwise, compares the addr to the range's
     * start address.
     *
     * If comparing an address to an address, simple component-by-component
     * comparison is performed.
     *
     * @param o1    First IpRange or IpAddr
     * @param o2    Second IpRange or IpAddr
     *
     * @return      -1 if o1 < o2, zero if o1 == o2, 1 if o1 > o2.
     */
    public int compare(Object o1, Object o2) 
    {
      IpRange ip1;
      IpRange ip2;

      try 
      {
        ip1 = (IpRange)o1;
      }
      catch (ClassCastException e) {
        IpAddr ad = (IpAddr)o1;
        try {
          ip2 = (IpRange)o2;
        }
        catch (ClassCastException e2) {
          return compareAddr(ad, (IpAddr)o2);
        }

        if (ip2.matches(ad))
          return -1;
        return compareAddr(ad, ip2.startAddr);
      }

      try {
        ip2 = (IpRange)o2;
      }
      catch (ClassCastException e) {
        IpAddr ad = (IpAddr)o2;
        if (ip1.matches(ad))
          return 1;
        return compareAddr(ad, ip1.startAddr);
      }

      int ret = compareAddr(ip1.startAddr, ip2.startAddr);
      if (ret == 0)
        ret = compareAddr(ip1.endAddr, ip2.endAddr);
      return ret;
    } // compare()

    /** Compare two IP addresses component by component. */
    private int compareAddr(IpAddr a1, IpAddr a2) 
    {
      for (int i = 0; i < 4; i++) {
        int diff = a1.components[i] - a2.components[i];
        if (diff != 0)
          return diff;
      }
      return 0;
    } // compareAddr()
  } // class IpRangeComparator

  /**
   * Constructs and loads an IP map from the specified file.
   *
   * @param     path          Path to the file to load the IP map from.
   *
   * @exception IOException   If the IP map file couldn't be opened.
   */
  public IpList(String path)
    throws IOException 
  {
    map = new TreeMap(new IpRangeComparator());
    readMap(path);
  } // IpList()

  /**
   * Parses the given IP address and checks whether it falls within one
   * of the positive ranges of the map, and doesn't fall in one of the
   * excluded ranges.
   *
   * @param ipAddrStr     A string of the form "a.b.c.d" where each component
   *                      is a decimal number from 0-255.
   *
   * @return              true if and only if the address matches.
   */
  public boolean isApproved(String ipAddrStr) 
  {
    // First, parse the address we got. If we can't, it's definitely not
    // approved.
    //
    IpAddr parsedAddr = new IpAddr();
    if (parsedAddr.parse(ipAddrStr) == null)
      return false;

    // We want to scan entries that will match this. We can do that
    // pretty easily by scanning from the first entry greater than or
    // equal to this one.
    //
    Iterator i = map.tailMap(parsedAddr).keySet().iterator();
    boolean positive = false;
    boolean negative = false;
    while (i.hasNext()) 
    {
      IpRange mapRange = (IpRange)i.next();

      // Stop when we reach an address that doesn't match, and the 
      // the first two components don't match.
      //
      if (!mapRange.matches(parsedAddr) &&
          !mapRange.startAddr.componentsEqual(parsedAddr, 2)) 
      {
        break;
      }

      // See if the parsed address matches the one in the map. If not,
      // skip it.
      //
      if (!mapRange.matches(parsedAddr))
        continue;

      // Found a matching entry. Set the appropriate flag.
      if (mapRange.isPositive)
        positive = true;
      else
        negative = true;
    } // while

    // If any matching negative entry was found, that takes precedence.
    if (negative)
      return false;

    // If a positive entry was found, the address is approved.
    if (positive)
      return true;

    // If there was no match, the address is not approved.
    return false;
  } // isApproved()

  /**
   * Reads the contents the given file into the IP map.
   *
   * @param       path            Path to the file to load
   *
   * @exception   IOException     If the file couldn't be read from.
   */
  private void readMap(String path)
    throws IOException 
  {
    Reader rawReader;
    if (path.startsWith("http://")) {
      URLConnection conn = new URL(path).openConnection();
      conn.setAllowUserInteraction(false);
      conn.setDoInput(true);
      conn.setDoOutput(false);
      conn.setUseCaches(false);
      conn.connect();
      rawReader = new InputStreamReader(conn.getInputStream());
    }
    else
      rawReader = new FileReader(path);

    // Open the file for reading line-by-line.
    LineNumberReader reader = new LineNumberReader(rawReader);

    // Process each line in turn.
    while (true) 
    {
      String line = reader.readLine();
      if (line == null)
        break;

      // Strip leading and trailing whitespace.
      line = line.trim();

      // Skip blank lines.
      if (line.equals(""))
        continue;

      // Lines beginning with numbers specify positive IP addresses
      if (Character.isDigit(line.charAt(0)) || (line.charAt(0) == '*')) {
        processEntry(line, true);
      }

      // Lines beginning with "exclude" specify negative IP addresses
      else if (line.startsWith("exclude")) {
        line = line.substring(7).trim();
        processEntry(line, false);
      }

      // Ignore other kinds of lines.
      else
        continue;
    }
  } // readMap()

  /**
   * Used by readMap to parse a single entry in the IP map file.
   *
   * @param line          The line of text to parse
   * @param isPositive    true if this is an "exclude" line
   */
  private void processEntry(String line, boolean isPositive) 
  {
    IpAddr startIp = new IpAddr();
    IpAddr endIp = new IpAddr();

    // Parse the first IP address. If invalid, skip this line.
    String str = line;
    str = startIp.parse(str);
    if (str == null)
      return;

    // If the next char is a "-", then it's a range.
    str = str.trim();
    if (str.startsWith("-")) 
    {
      // Strip the "-" and any spaces after it.
      str = str.substring(1).trim();

      // Now try to parse the end IP addr. If invalid, skip the line.
      str = endIp.parse(str);
      if (str == null)
        return;
    }

    // Otherwise, it's a single address.
    else
      endIp = startIp;

    // Make a new entry in the map.
    IpRange range = new IpRange(startIp, endIp, isPositive);
    map.put(range, null);
  }

  /**
   * Prints the contents of the IP map to a ServletOutputStream. For
   * debugging purposes only.
   *
   * @param out   The stream to print to
   */
  public void print(ServletOutputStream out)
    throws IOException 
  {
    out.println("IpList contents:");
    Iterator i = map.keySet().iterator();
    while (i.hasNext()) {
      IpRange range = (IpRange)i.next();
      out.println(
        "Range: " + range.startAddr.components[0] + "." +
        range.startAddr.components[1] + "." + range.startAddr.components[2] +
        "." + range.startAddr.components[3] + " - " +
        range.endAddr.components[0] + "." + range.endAddr.components[1] + "." +
        range.endAddr.components[2] + "." + range.endAddr.components[3] +
        " ==> " + range.isPositive);
    } // while
  } // print()

  /** Sorted map of IpRanges. The values are irrelevant (and set to null). */
  private TreeMap map;
} // class IpList
