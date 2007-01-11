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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.PriorityQueue;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/** 
 * Performs a disk-based sort of the lines of a text file, similar to the
 * UNIX sort command. However, it is Unicode-aware.
 * 
 * @author Martin Haye
 */
public class FileSorter
{
  private static final int DEFAULT_MEM_LIMIT = 10*1024*1024; // 10 megs
  private static String SENTINEL = "\ueeee\ueede\ueee1";
  
  /** Generalized interface for feeding lines to the sorter */
  public interface Input {
    String readLine() throws IOException;
    void close() throws IOException;
  }
  
  /** Generalized interface for writing lines from the sorter */
  public interface Output {
    void writeLine(String line) throws IOException;
    void close() throws IOException;
  }
  
  /** Get sort input from a file */
  public static class FileInput implements Input {
    private BufferedReader in;
    public FileInput(File f) throws FileNotFoundException {
      in = new BufferedReader(new FileReader(f));
    }
    public String readLine() throws IOException {
      return in.readLine();
    }
    public void close() throws IOException {
      in.close();
    }
  }
  
  /** Write sort input to a file */
  public static class FileOutput implements Output {
    private BufferedWriter out;
    public FileOutput(File f, int bufSize) throws IOException {
      out = new BufferedWriter(new FileWriter(f), bufSize);
    }
    public void writeLine(String s) throws IOException {
      out.write(s);
      out.write('\n');
    }
    public void close() throws IOException {
      out.close();
    }
  }
  
  /** Source of input lines */
  private Input in;
  
  /** Sink for output lines */
  private Output out;
  
  /** File to use for temporary disk storage (automatically deleted) */
  private File tmpFile;
  
  /** Approximate limit on the amount of memory to consume during sort */
  private int memLimit;
  
  /** Count of how many lines were read in */
  private int nLines;
  
  /** Default constructor with default memory limit */
  public FileSorter() {
    this(DEFAULT_MEM_LIMIT);
  }
  
  /** Constructor to specify a memory limit */
  public FileSorter(int memLimit) {
    this.memLimit = memLimit;
  }
  
  /** General sort method, independent of input and output specifics. */
  public void sort(Input in, Output out, File tmpFile) throws IOException
  {
    this.in = in;
    this.out = out;
    this.tmpFile = tmpFile;
    
    try 
    {
      // Clear the temp file.
      clearFile(tmpFile);
      
      // First, divide the input file into sorted blocks
      nLines = 0;
      ArrayList blocks = partition();
      
      // Then merge the blocks to form the final output
      int nWritten = merge(blocks);
      assert nWritten == nLines : "merge missed some lines";
    }
    finally {
      in.close();
      out.close();
      tmpFile.delete();
    }
  }
  
  /** Sort from an input file to an output file */
  public static void sort(File inFile, File outFile, File tmpFile, int memLimit) 
    throws IOException
  {
    try 
    {
      // Clear the output file first.
      clearFile(outFile);
      
      // Do the main work of sorting.
      FileSorter sorter = new FileSorter(memLimit);
      sorter.sort(new FileInput(inFile), new FileOutput(outFile, memLimit/10), tmpFile);
    }
    catch (IOException e) {
      outFile.delete();
      throw e;
    }
  }
  
  /** Simple API: Sort from an input file to an output file */
  public static void sort(File inFile, File outFile) throws IOException {
    File tmpFile = new File(inFile.toString() + ".sort_tmp");
    sort(inFile, outFile, tmpFile, DEFAULT_MEM_LIMIT);
  }
  
  /** Command-line interface */
  public static void main(String[] args)
  {
    if (args.length != 2) {
      System.err.println("Usage: sort <inFile> <outFile>");
    }
    File inFile  = new File(args[0]);
    File outFile = new File(args[1]);
    try {
      long startTime = System.currentTimeMillis();
      sort(inFile, outFile);
      System.out.println("Elapsed: " + (System.currentTimeMillis() - startTime));
    }
    catch (Throwable t) {
      System.err.println(t);
      System.exit(1);
    }
  }

  /** Delete, or at least truncate, the given file */
  private static void clearFile(File f) throws IOException
  {
    if (f.delete())
      return;
    FileOutputStream truncator = new FileOutputStream(f);
    truncator.close();
    f.delete();
  }
  
  /** Partition the input file into sorted blocks */
  private ArrayList partition() 
    throws IOException
  {
    BufferedReader in = null;
    CountedOutputStream countedTmp = new CountedOutputStream(
        new FileOutputStream(tmpFile));
    ArrayList blocks = new ArrayList();
    
    try 
    {
      while (true)
      {
        // Since this is disk-based, we achieve a significant performance gain
        // by simply compressing the data going out, and coming back in.
        //
        DeflaterOutputStream deflater = new DeflaterOutputStream(countedTmp);
        DataOutputStream out = new DataOutputStream(deflater);
        
        // Read a block of lines, up to the memory limit
        ArrayList block = readBlock();
        if (block.isEmpty())
          break;
        
        // Sort it in memory
        Collections.sort(block);
        
        // Record the block's position
        long blockPos = countedTmp.nWritten();
        blocks.add(new Long(blockPos));
        
        // Write out the block.
        for (int i=0; i<block.size(); i++)
          out.writeUTF((String) block.get(i));
        out.writeUTF(SENTINEL);

        // Finish off the block.
        out.flush();
        deflater.finish();
      }
    }
    finally {
      if (in != null)
        in.close();
      if (countedTmp != null)
        countedTmp.close();
    }
    return blocks;
  }

  /** Merge sorted blocks from the temp file into the final output file. */
  private int merge(ArrayList blocks) 
    throws IOException
  {
    // Calculate the memory limit for each block. Make them at least 16k.
    int blockMemLimit = Math.max(16384, memLimit / blocks.size());
    
    // Open the temporary file which contains the sorted blocks.
    RandomAccessFile tmp = new RandomAccessFile(tmpFile, "r");

    try
    {
      // Make a priority queue of each of the input blocks.
      PriorityQueue queue = new PriorityQueue(blocks.size());
      for (int i=0; i<blocks.size(); i++) {
        long blockPos = ((Long)blocks.get(i)).longValue();
        BlockReader block = new BlockReader(tmp, blockPos, blockMemLimit);
        if (block.next())
          queue.add(block);
      }
      
      // Now write all the lines in order.
      String prev = "";
      int nLinesWritten = 0;
      while (!queue.isEmpty()) 
      {
        BlockReader block = (BlockReader) queue.remove();
        
        String line = block.cur();
        assert line.compareTo(prev) >= 0 : "merge or sort algorithm failed";
        prev = line;
        
        out.writeLine(line);
        nLinesWritten++;
        
        if (block.next())
          queue.add(block);
      }
      return nLinesWritten;
    }
    finally {
      tmp.close();
    }
  }

  /**
   * Read in a block of lines from a file, trying to not exceed the specified
   * memory limit.
   */
  private ArrayList readBlock()
    throws IOException
  {
    long memUsed = 0;
    ArrayList block = new ArrayList(1000);
    while (memUsed < memLimit) {
      String line = in.readLine();
      if (line == null)
        break;
      block.add(line);
      nLines++;
      memUsed += memSize(line);
    }
    return block;
  }
  
  /** Give a rough estimate of how much memory a given string takes */
  private static int memSize(String s) {
    return (s.length() * 2) + 32;
  }
  
  /** 
   * Reads a block of compressed lines from the temporary disk file, and
   * feeds them out one at a time. Is Comparable (which compares the
   * current line) so it can be used in a PriorityQueue.
   */
  private static class BlockReader implements Comparable
  {
    /** The temporary file being read */
    RandomAccessFile base;
    
    /** Input source that decompresses and reads UTF strings */
    DataInput in;
    
    /** Current position within the random access file */
    long pos;
    
    /** Memory limit for this particular reader */
    long memLimit;
    
    /** Buffer of lines */
    ArrayList buffer = new ArrayList();
    
    /** Position within buffer */
    int cur = -1;
    
    /** Set to true when last line has been read */
    boolean eof = false;

    /** Construct the reader */
    public BlockReader(RandomAccessFile base, long pos, int memLimit) 
      throws IOException 
    {
      this.base = base;
      this.pos = pos;
      this.memLimit = memLimit;
      in = new DataInputStream(
             new InflaterInputStream(new RandomAccessInputStream(base)));
    }
   
    /** 
     * Advance to the next line. Must be called at least once before
     * calling {@link #cur()}.
     * 
     * @return true if there is another line, false if EOF
     */
    public boolean next() throws IOException 
    {
      cur++;
      if (cur == buffer.size()) {
        if (!fill())
          return false;
      }
      return true;
    }

    /** 
     * Obtain the current line of the file. Only valid if the last call
     * to {@link #next()} returned true.
     */
    public String cur() {
      return (String) buffer.get(cur);
    }

    /** Compare the current line of this reader with that of another. */
    public int compareTo(Object other) {
      return cur().compareTo(((BlockReader)other).cur());
    }
    
    /** 
     * Fill the buffer with more lines.
     * 
     * @return true if at least one line was read
     */
    private boolean fill() throws IOException 
    {
      buffer.clear();
      if (eof)
        return false;
      
      base.seek(pos);
      long memUsed = 0;
      while (memUsed < memLimit) {
        String line = in.readUTF();
        if (line.equals(SENTINEL)) {
          eof = true;
          break;
        }
        buffer.add(line);
        memUsed += memSize(line);
      }
      pos = base.getFilePointer();
      cur = 0;
      return memUsed > 0;
    }
  } // class
    
} // class