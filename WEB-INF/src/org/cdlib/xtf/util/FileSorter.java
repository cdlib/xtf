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
  /** Default memory limit if none specified */
  public static final int DEFAULT_MEM_LIMIT = 10 * 1024 * 1024; // 10 megs

  /** File to use for temporary disk storage (automatically deleted) */
  private File tmpFile;

  /** Approximate limit on the amount of memory to consume during sort */
  private int memLimit;

  /** Count of how many lines were read in */
  private int nLinesAdded;

  /** Approximate amount of memory consumed by the current block of lines */
  private int curBlockMem = 0;

  /** Buffer of lines in the current block */
  private ArrayList curBlockLines = new ArrayList();

  /** Offsets of blocks already written to the temp file */
  private ArrayList blockOffsets = new ArrayList();

  /** Sentinel string used to mark end of blocks */
  private static String SENTINEL = "\ueeee\ueede\ueee1";

  /**
   * Protected constructor -- do not construct directly; rather, use one
   * of the simple, intermediate, or advanced API methods below.
   */
  protected FileSorter() {
  }

  /** Simple command-line interface */
  public static void main(String[] args) 
  {
    if (args.length != 2) {
      System.err.println("Usage: sort <inFile> <outFile>");
      System.exit(1);
    }
    try {
      long startTime = System.currentTimeMillis();
      sort(new File(args[0]), new File(args[1]));
      System.out.println(
        "Sort time: " + ((System.currentTimeMillis() - startTime) / 1000.0f) +
        " sec");
    }
    catch (IOException e) {
      System.err.println(e);
    }
  }

  /** Simple API: Sort from an input file to an output file */
  public static void sort(File inFile, File outFile)
    throws IOException 
  {
    sort(inFile, outFile, null, DEFAULT_MEM_LIMIT);
  }

  /**
   * Intermediate API: sort from a file, to a file, using a specified temporary
   * directory and memory limit.
   *
   * @param inFile source of input lines, in UTF-8 encoding
   * @param outFile destination of output lines
   * @param tmpDir filesystem directory for temporary storage during sort. If
   *               null, then the system default temp directory will be used.
   * @param memLimit approximate max amount of RAM to use during sort
   */
  public static void sort(File inFile, File outFile, File tmpDir, int memLimit)
    throws IOException 
  {
    // Clear the output file
    clearFile(outFile);

    // Open the input file.
    BufferedReader in = new BufferedReader(new FileReader(inFile));
    try 
    {
      // Do the main work of sorting.
      FileSorter sorter = FileSorter.start(tmpDir, memLimit);
      while (true) {
        String line = in.readLine();
        if (line == null)
          break;
        sorter.addLine(line);
      }
      sorter.finish(new FileOutput(outFile, memLimit / 10));
    }
    catch (IOException e) {
      outFile.delete();
      throw e;
    }
    finally {
      in.close();
    }
  }

  /** Advanced API interface for writing lines from the sorter */
  public interface Output 
  {
    void writeLine(String line)
      throws IOException;

    void close()
      throws IOException;
  }

  /** Advanced API class: write output to a file */
  public static class FileOutput implements Output 
  {
    private BufferedWriter out;

    public FileOutput(File f, int bufSize)
      throws IOException 
    {
      out = new BufferedWriter(new FileWriter(f), bufSize);
    }

    public void writeLine(String s)
      throws IOException 
    {
      out.write(s);
      out.write('\n');
    }

    public void close()
      throws IOException 
    {
      out.close();
    }
  }

  /**
   * Advanced API, independent of input and output format. Uses "push"
   * method, where first you call start() to obtain a FileSorter object.
   * Then you repeatedly call putLine() to specify each line to be
   * sorted. Then finally you call finish() to complete the sorting.
   *
   * @param tmpDir a filesystem directory to store temporary data during sort.
   * @param memLimit approximate limit on the amount of RAM to use during sort.
   */
  public static FileSorter start(File tmpDir, int memLimit)
    throws IOException 
  {
    if (tmpDir != null && !tmpDir.isDirectory())
      throw new IOException("Invalid temp directory specified");

    FileSorter sorter = new FileSorter();
    sorter.memLimit = memLimit;
    sorter.tmpFile = File.createTempFile("sort", ".tmp", tmpDir);
    return sorter;
  }

  /**
   * Add a line to be sorted.
   *
   * @param line one line of data to be sorted
   */
  public void addLine(String line)
    throws IOException 
  {
    // Add this line to our buffer for the current block. If it's full, flush
    // it to the temp file.
    //
    curBlockLines.add(line);
    ++nLinesAdded;
    curBlockMem += memSize(line);
    if (curBlockMem >= memLimit)
      flushBlock();
  }

  /** Find out how many lines were added */
  public int nLinesAdded() {
    return nLinesAdded;
  }

  /**
   * Perform the main work of sorting, sending the results to the specified
   * output.
   */
  public void finish(Output out)
    throws IOException 
  {
    // Special case: if all the lines are in memory, avoid the temp file
    // completely.
    //
    if (blockOffsets.isEmpty()) {
      Collections.sort(curBlockLines);
      for (int i = 0; i < curBlockLines.size(); i++)
        out.writeLine((String)curBlockLines.get(i));
      out.close();
      clearFile(tmpFile);
      return;
    }

    // Okay, we have to use disk-based sorting. First, flush any lines in the
    // last block.
    //
    flushBlock();

    // We will be keeping part of every block in memory while merging. 
    // Calculate the memory limit for each block so we maximize the buffers
    // (which minimizes disk seek time).
    //
    int blockMemLimit = Math.max(16384, memLimit / blockOffsets.size());

    // Open the temporary file which contains the sorted blocks.
    RandomAccessFile tmpIn = new RandomAccessFile(tmpFile, "r");
    try 
    {
      // Make a priority queue of each of the input blocks.
      PriorityQueue queue = new PriorityQueue(blockOffsets.size());
      for (int i = 0; i < blockOffsets.size(); i++) {
        long blockPos = ((Long)blockOffsets.get(i)).longValue();
        BlockReader block = new BlockReader(tmpIn, blockPos, blockMemLimit);
        if (block.next())
          queue.add(block);
      }

      // Now write all the lines in order.
      String prev = "";
      int nLinesWritten = 0;
      while (!queue.isEmpty()) 
      {
        BlockReader block = (BlockReader)queue.remove();

        String line = block.cur();
        assert line.compareTo(prev) >= 0 : "merge or sort algorithm failed";
        prev = line;

        out.writeLine(line);
        nLinesWritten++;

        if (block.next())
          queue.add(block);
      }
      assert nLinesWritten == nLinesAdded : "wrong number of lines written";
    }
    finally {
      out.close();
      tmpIn.close();
      clearFile(tmpFile);
    }
  }

  /**
   * Flush currently buffered lines to the temporary file. This involves
   * sorting them, and writing them out as a compressed block.
   */
  private void flushBlock()
    throws IOException 
  {
    // Sort the lines we have buffered
    Collections.sort(curBlockLines);

    // Record the block's starting offset in the temp file.
    blockOffsets.add(new Long(tmpFile.length()));

    // Open the temp file and record the offset of the new block.
    FileOutputStream tmpOut = new FileOutputStream(tmpFile, true);
    try 
    {
      // Testing has shown a significant performance gain (around 40%) from
      // compressing the data going to and from disk.
      //
      DeflaterOutputStream deflater = new DeflaterOutputStream(tmpOut);
      DataOutputStream blockOut = new DataOutputStream(deflater);

      // Write out each line from the block, followed by a sentinel to mark
      // the end.
      //
      for (int i = 0; i < curBlockLines.size(); i++)
        blockOut.writeUTF((String)curBlockLines.get(i));
      blockOut.writeUTF(SENTINEL);

      // Finish off the compression.
      blockOut.flush();
      deflater.finish();

      // Clear the buffer in preparation for the next block.
      curBlockLines.clear();
      curBlockMem = 0;
    }
    finally {
      tmpOut.close();
    }
  }

  /** Delete, or at least truncate, the given file (if it exists) */
  private static void clearFile(File f)
    throws IOException 
  {
    if (!f.canRead())
      return;
    if (f.delete())
      return;
    FileOutputStream truncator = new FileOutputStream(f);
    truncator.close();
    f.delete();
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
    public boolean next()
      throws IOException 
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
      return (String)buffer.get(cur);
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
    private boolean fill()
      throws IOException 
    {
      buffer.clear();
      if (eof)
        return false;

      base.seek(pos);
      long memUsed = 0;
      while (memUsed < memLimit) 
      {
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
