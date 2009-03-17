package org.cdlib.xtf.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Copyright (c) 2009, Regents of the University of California
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

public class ProcessRunner
{
  /**
   * Run the external process, applying a timeout if specified, feeding it
   * input on stdin and gathering the results from stdout. If a non-zero
   * exit status is returned, we throw an exception containing the output
   * string from stderr. The input and output are encoded using the
   * system-default charset.
   * 
   * @throws IOException            If something goes wrong starting the process.
   * @throws InterruptedException   If process exceeds the given timeout.
   * @throws CommandFailedException If the process exits with non-zero status.
   */
  public static String runAndGrab(String[] argArray, String input, int timeout) 
    throws InterruptedException, CommandFailedException, IOException
  {
    Charset charset = Charset.defaultCharset();
    ByteBuffer inBuf = charset.encode(input);
    inBuf.compact();
    byte[] inBytes = inBuf.array();
    
    byte[] outBytes = runAndGrab(argArray, inBytes, timeout);
    
    CharBuffer outBuf = charset.decode(ByteBuffer.wrap(outBytes));
    String output = outBuf.toString();
    return output;
  }
  
  /**
   * Run the external process, applying a timeout if specified, feeding it
   * input on stdin and gathering the results from stdout. If a non-zero
   * exit status is returned, we throw an exception containing the output
   * string from stderr.
   * 
   * @throws IOException            If something goes wrong starting the process.
   * @throws InterruptedException   If process exceeds the given timeout.
   * @throws CommandFailedException If the process exits with non-zero status.
   */
  public static byte[] runAndGrab(String[] argArray, byte[] inputBytes, int timeout)
      throws InterruptedException, CommandFailedException, IOException 
  {
    // Get ready to go
    Process process = null;
  
    InputStream stdout = null;
    OutputGrabber stdoutGrabber = null;
    InputStream stderr = null;
    OutputGrabber stderrGrabber = null;
  
    OutputStream stdin = null;
    InputStuffer stdinStuffer = null;
  
    Timer timer = null;
    Interrupter interrupter = null;
    
    boolean exception = false;
  
    try 
    {
      // Fire up the process.
      process = Runtime.getRuntime().exec(argArray);
  
      // Stuff input into it (even if that input is nothing).
      stdin = process.getOutputStream();
      stdinStuffer = new InputStuffer(stdin, inputBytes);
      stdinStuffer.start();
  
      // Grab all the output
      stdout = process.getInputStream();
      stdoutGrabber = new OutputGrabber(stdout);
      stderr = process.getErrorStream();
      stderrGrabber = new OutputGrabber(stderr);
  
      stdoutGrabber.start();
      stderrGrabber.start();
  
      // Set a timer so we can stop the process if it exceeds the timeout.
      if (timeout > 0) {
        interrupter = new Interrupter(Thread.currentThread());
        timer = new Timer();
        timer.schedule(interrupter, timeout);
      }
  
      // Wait for the process to finish.
      process.waitFor();
    } // try
    catch (IOException e) {
      exception = true;
      throw e;
    }
    catch (InterruptedException e) {
      exception = true;
      throw e;
    }
    finally 
    {
      if (interrupter != null) { 
        synchronized (interrupter) {
          timer.cancel(); // avoid further interruptions
          interrupter.mainThread = null; // make sure it can't get to us
          Thread.interrupted(); // clear out the interrupted flag.
        }
      }
      if (exception)
      {
        if (stdinStuffer != null)
          stdinStuffer.interrupt();
        if (stdoutGrabber != null)
          stdoutGrabber.interrupt();
        if (stderrGrabber != null)
          stderrGrabber.interrupt();
        if (process != null)
          process.destroy();
        
        if (stdin != null)
          try { stdin.close();  } catch (IOException e2) { /*ignore*/ }
        if (stdout != null)
          try { stdout.close(); } catch (IOException e2) { /*ignore*/ }
        if (stderr != null)
          try { stderr.close(); } catch (IOException e2) { /*ignore*/ }
      }
    } // finally
  
    // Wait for the stuffer and grabbers to finish their work.
    try 
    {
      if (stdinStuffer != null) { 
        while (true) 
        {
          synchronized (stdinStuffer) {
            if (stdinStuffer.done)
              break;
            stdinStuffer.wait();
          }
        }
      }
      while (true) { 
        synchronized (stdoutGrabber) {
          if (stdoutGrabber.done)
            break;
          stdoutGrabber.wait();
        }
      }
      while (true) { 
        synchronized (stderrGrabber) {
          if (stderrGrabber.done)
            break;
          stderrGrabber.wait();
        }
      }
    }
    catch (InterruptedException e) {
      assert false : "should not be interrupted at this stage";
    }
  
    // Make sure all the streams are closed (to avoid leaking.)
    if (stdin != null)
      try { stdin.close();  } catch (IOException e) { /*ignore*/ }
    if (stdout != null)
      try { stdout.close(); } catch (IOException e) { /*ignore*/ }
    if (stderr != null)
      try { stderr.close(); } catch (IOException e) { /*ignore*/ }
  
    // If we got a non-zero exit status, and something came out on stderr,
    // then throw an exception.
    //
    if (process.exitValue() != 0) {
      String errStr = new String(stderrGrabber.outBytes);
      throw new CommandFailedException(
        "External command '" + argArray[0] + "' exited with status " +
        process.exitValue() + ". Output from stderr:\n" + errStr);
    }
  
    // Return the results from stdout
    return stdoutGrabber.outBytes;
  }

  /** Used to interrupt the main thread if a timeout occurs */
  private static class Interrupter extends TimerTask 
  {
    public Thread mainThread;

    public Interrupter(Thread mainThread) {
      this.mainThread = mainThread;
    }

    public synchronized void run() {
      if (mainThread != null)
        mainThread.interrupt();
    }
  } // class Interrupter

  /**
   * Class to stuff input into the process's input stream (an OutputStream to
   * us).
   */
  private static class InputStuffer extends Thread 
  {
    private OutputStream outStream;
    private byte[] bytes;
    public Throwable error;
    public boolean done = false;

    public InputStuffer(OutputStream stream, byte[] bytes)
      throws UnsupportedEncodingException 
    {
      this.outStream = stream;
      this.bytes = bytes;
    }

    public void run() 
    {
      try 
      {
        // Write all the data.
        outStream.write(bytes);

        // Inform the process that this is the end now.
        outStream.close();
      } // try
      catch (IOException e) 
      {
        error = e;
      }
      finally {
        synchronized (this) {
          done = true;
          notifyAll();
        }
      }
    } // run()
  } // class InputStuffer

  /**
   * Class to accumulate the output from a process's output stream (which is
   * an InputStream to us), and turn it into a string.
   */
  private static class OutputGrabber extends Thread 
  {
    private InputStream inStream;
    private ByteArrayOutputStream buffer = new ByteArrayOutputStream(100);
    public byte[] outBytes = new byte[0];
    public Throwable error;
    public boolean done = false;

    public OutputGrabber(InputStream stream) {
      this.inStream = stream;
    }

    public void run() 
    {
      // Read data until it's exhausted (we will be interrupted when it's time
      // to stop.)
      //
      try 
      {
        byte[] tmp = new byte[4096];
        while (true) 
        {
          // Read some stuff.
          int got = inStream.read(tmp);
          if (got < 0)
            break;

          // Save it up.
          buffer.write(tmp, 0, got);
        }

        // Get a byte array that the caller can process.
        outBytes = buffer.toByteArray();
      } // try
      catch (IOException e) 
      {
        error = e;
      }
      finally {
        synchronized (this) {
          done = true;
          notifyAll();
        }
      }
    } // run()
  } // class OutputGrabber
  
  /**
   * Exception thrown if an external command ends with a non-zero exit
   * status.
   */
  public static class CommandFailedException extends Exception {
    public CommandFailedException() {
      super();
    }
    public CommandFailedException(String s) {
      super(s);
    }
  }
  
}
