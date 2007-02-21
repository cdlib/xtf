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
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.WeakHashMap;

////////////////////////////////////////////////////////////////////////////////

/** The <code>Trace</code> class provides a mechanism for logging output
 *  messages to the console or any PrintStream or Writer.
 *
 *  A number of output message levels are supported by this class, including
 *  errors, warnings, info messages, and debug messages. Outputting messages
 *  for any one of these levels is accomplished by passing a message string
 *  to the {@link Trace#error(String) error()}, {@link Trace#warning(String) warning()},
 *  {@link Trace#info(String) info()}, or {@link Trace#debug(String) debug()}
 *  functions. <br><br>
 *
 *  Messages may be indented to reflect the nesting of function calls by using
 *  the {@link Trace#tab() tab()} and {@link Trace#untab() untab()} functions.
 *  Single-line multi-part messages (where the first part is tabbed from the
 *  left, and additional parts are tacked on to the end of the line after
 *  additional processing) may be created using the {@link Trace#more(String) more() }
 *  functions. Indentation is normally two spaces per tab level, but this
 *  may be changed by setting the {@link #defaultTabSize} variable.<br><br>
 *
 *  The message level actually output by the trace system may be
 *  adjusted to allow all or some message types to be displayed. The output
 *  level for the trace system is set using the function
 *  {@link Trace#setOutputLevel(int) setOutputLevel()} function.<br><br>
 *
 *  Each output line can be automatically prefixed with a timestamp by
 *  calling the {@link Trace#printTimestamps(boolean)} method. The default
 *  is to print <i>without</i> timestamps. When enabled, timestamps will be
 *  printed with a simple date format: YYYY-MM-DD:HH:MM:SS, but this can be
 *  changed if necessary by changing the value of the {@link Trace#dateFormat}
 *  member variable.<br><br>
 *
 *  All output goes to the console (System.out) by default, but it can
 *  optionally be redirected to any PrintStream or Writer by calling
 *  {@link #setPrintStream(PrintStream)} or {@link #setWriter(Writer)}
 *  respectively.<br><br>
 *
 *  If multiple threads use the Trace facility, the output of each will
 *  be marked with "[1]" for the first thread, "[2]" for the second, etc.
 *  (but if only one thread uses Trace, no such markers will be printed.)
 *  Trace automatically maintains a separate tab level for each thread, but
 *  the other variables, such as PrintStream/Writer, output level, etc.,
 *  are static and apply to all threads.
 */
public class Trace 
{
  //////////////////////////////////////////////////////////////////////////////

  // Error reporting level constants. Note that these currently accumulate
  // lower level output. That is, selecting 'info' will also output 'warnings'
  // and 'errors'. However, since the error levels are separate bits, this 
  // could be changed to show any combination of output levels.
  //

  /** Print no messages */
  public static final int silent = 0;

  /** Print errors only */
  public static final int errors = 1;

  /** Print errors and warnings */
  public static final int warnings = 2;

  /** Print info messages, errors, and warnings */
  public static final int info = 4;

  /** Print all messages (debug, info, errors, and warnings.) */
  public static final int debug = 8;

  /** Amount to indent when {@link #tab()} is called. Default value: 2 */
  public static int defaultTabSize = 2;

  /** Format to output dates in. Default: yyyy-MM-dd:HH:mm:ss */
  public static DateFormat dateFormat = new SimpleDateFormat(
    "yyyy-MM-dd:HH:mm:ss");

  //////////////////////////////////////////////////////////////////////////////

  /** Set the level of message output. Messages below this level will not be
   *  printed.
   *
   *  @param level  One of {@link #silent}, {@link #errors},
   *                {@link #warnings}, {@link #info}, or {@link #debug}.
   */
  public static void setOutputLevel(int level) 
  {
    // Right now, treat the passed level as cumulative (i.e., "warnings" is 
    // actually treated as "errors" + "warnings".) We could however change 
    // the passed value to be a mask if that was more useful.
    //
    outputMask = silent;
    if (level >= errors)
      outputMask |= errors;
    if (level >= warnings)
      outputMask |= warnings;
    if (level >= info)
      outputMask |= info;
    if (level >= debug)
      outputMask |= debug;
  } // setOutputLevel()

  //////////////////////////////////////////////////////////////////////////////

  /** Retrieves the current output level established by
   *  {@link #setOutputLevel(int)}.
   */
  public static int getOutputLevel() 
  {
    // Right now, return the highest output level only. We could however change 
    // the returned value to be the mask if that was more useful.
    //
    if ((outputMask & debug) != 0)
      return debug;
    if ((outputMask & info) != 0)
      return info;
    if ((outputMask & warnings) != 0)
      return warnings;
    if ((outputMask & errors) != 0)
      return errors;

    return silent;
  } // getOutputLevel()

  //////////////////////////////////////////////////////////////////////////////

  /** Enables or disables prefixing each output line with a timestamp.
   *  Default is disabled.
   */
  public static void printTimestamps(boolean flag) {
    printTimestamps = flag;
  }

  //////////////////////////////////////////////////////////////////////////////

  /** Enables or disables immediate newline and flushing of each output line
   *  (note that this somewhat defeats the more() feature.) Normally,
   *  a newline isn't printed until Trace can be sure that more() won't be
   *  used to add to the existing line.
   */
  public static void setAutoFlush(boolean flag) {
    autoFlush = flag;
  }

  //////////////////////////////////////////////////////////////////////////////

  /** Indent all subsequent output lines by {@link #defaultTabSize}
   *  (default 2) spaces. Call {@link #untab()} or {@link #clearTabs()} to
   *  undo this effect.
   */
  public static void tab() {
    getThreadTrace().t_tab();
  }

  /** Internal threaded helper for {@link #tab()} */
  private void t_tab() 
  {
    // Tab in by the specified amount.
    tabCount += tabSize;
  } // tab()

  //////////////////////////////////////////////////////////////////////////////

  /** Undoes effect of {@link #tab()}, un-indenting subsequent output lines. */
  public static void untab() {
    getThreadTrace().t_untab();
  }

  /** Internal threaded helper for {@link #untab()} */
  private void t_untab() 
  {
    // Tab out by the specified amount, but not less than zero.
    tabCount -= tabSize;
    if (tabCount < 0)
      tabCount = 0;
  } // untab()

  //////////////////////////////////////////////////////////////////////////////

  /** Resets the tab level to zero, undoing the effects of any calls to
   *  {@link #tab()}.
   */
  public static void clearTabs() {
    getThreadTrace().t_clearTabs();
  }

  /** Internal threaded helper for {@link #clearTabs()}. */
  private void t_clearTabs() 
  {
    // Reset the tabs to zero indent.
    tabCount = 0;
  } // clearTabs()

  //////////////////////////////////////////////////////////////////////////////

  /** Overrides the default output destination, System.out, with the given
   *  alternate print stream.
   *
   *  @param pstream    Where to direct subsequent output to
   */
  public static void setPrintStream(PrintStream pstream) {
    printStream = pstream;
    writer = null;
  } // setPrintStream()

  //////////////////////////////////////////////////////////////////////////////

  /** Overrides the default output destination, System.out, with the given
   *  alternate Writer.
   *
   *  @param w    Where to direct subsequent output to
   */
  public static void setWriter(Writer w) {
    writer = w;
    printStream = null;
  } // setPrintStream()

  //////////////////////////////////////////////////////////////////////////////

  /** Retrieve the thread identifier that is printed for the current thread.
   */
  public static String getCurrentThreadId() {
    return getThreadTrace().threadId;
  }

  //////////////////////////////////////////////////////////////////////////////

  /** Retrieve the thread identifier that is printed for messages from the
   *  specified thread.
   */
  public static String getThreadId(Thread thread) {
    Trace trace = (Trace)threadTraces.get(thread);
    if (trace == null)
      return null;
    return trace.threadId;
  }

  //////////////////////////////////////////////////////////////////////////////

  /** Output a message at the 'error' level. If the output level established
   *  by {@link #setOutputLevel(int)} is {@link #errors}, {@link #warnings},
   *  {@link #info}, or {@link #debug}, the message will be printed. If
   *  the output level is {@link #silent}, it will be suppressed.
   */
  public static void error(String msg) {
    getThreadTrace().t_error(msg);
  }

  /** Internal threaded helper for {@link #error(String)}. */
  private void t_error(String msg) 
  {
    // Set the previous message level to "error", so that more() can decide
    // whether to filter it's message or not.
    //
    prevMsgLevel = errors;

    // If we need to suppress error messages, return early.
    if ((outputMask & errors) == 0)
      return;

    // Issue a linefeed and output the tabbed message.
    output(msg, true, true);
  } // error()

  //////////////////////////////////////////////////////////////////////////////

  /** Output a message at the 'warning' level. If the output level
   *  established by {@link #setOutputLevel(int)} is {@link #warnings},
   *  {@link #info}, or {@link #debug}, the message will be printed. If
   *  the output level is {@link #errors} or {@link #silent}, it will be
   *  suppressed.
   */
  public static void warning(String msg) {
    getThreadTrace().t_warning(msg);
  }

  /** Internal threaded helper for {@link #warning(String)}. */
  private void t_warning(String msg) 
  {
    // Set the previous message level to "warning", so that more() can decide
    // whether to filter it's message or not.
    //
    prevMsgLevel = warnings;

    // If we need to suppress warning messages, return early.
    if ((outputMask & warnings) == 0)
      return;

    // Issue a linefeed and output the tabbed message.
    output(msg, true, true);
  } // warning()

  //////////////////////////////////////////////////////////////////////////////

  /** Output a message at the 'info' level. If the output level
   *  established by {@link #setOutputLevel(int)} is
   *  {@link #info}, or {@link #debug}, the message will be printed. If
   *  the output level is {@link #warnings}, {@link #errors}, or
   *  {@link #silent}, it will be suppressed.
   */
  public static void info(String msg) {
    getThreadTrace().t_info(msg);
  }

  /** Internal threaded helper for {@link #info(String)}. */
  private void t_info(String msg) 
  {
    // Set the previous message level to "info", so that more() can decide
    // whether to filter it's message or not.
    //
    prevMsgLevel = info;

    // If we need to suppress info messages, return early.
    if ((outputMask & info) == 0)
      return;

    // Issue a linefeed and output the tabbed message.
    output(msg, true, true);
  } // info()

  //////////////////////////////////////////////////////////////////////////////

  /** Output a message at the 'debug' level. If the output level
   *  established by {@link #setOutputLevel(int)} is {@link #debug}, the
   *  message will be printed. If the output level is {@link #info},
   *  {@link #warnings}, {@link #errors}, or {@link #silent}, it will be
   *  suppressed.
   */
  public static void debug(String msg) {
    getThreadTrace().t_debug(msg);
  }

  /** Internal threaded helper for {@link #debug(String)}. */
  private void t_debug(String msg) 
  {
    // Set the previous message level to "debug", so that more() can decide
    // whether to filter it's message or not.
    //
    prevMsgLevel = debug;

    // If we need to suppress debug messages, return early.
    if ((outputMask & debug) == 0)
      return;

    // Issue a linefeed and output the tabbed message.
    output(msg, true, true);
  } // debug()

  //////////////////////////////////////////////////////////////////////////////

  /** Append more text to the previous output line (unless of course it was
   *  suppressed).
   */
  public static void more(String msg) {
    getThreadTrace().t_more(msg);
  }

  /** Internal threaded helper for {@link #more(String)}. */
  private void t_more(String msg) 
  {
    // If we need to suppress the current message level, return early.
    if ((outputMask & prevMsgLevel) == 0)
      return;

    // Output the tabbed message without a linefeed.
    output(msg, false, false);
  } // more()

  //////////////////////////////////////////////////////////////////////////////

  /** Append more text to the previous output line if it was the same
   *  output level; if not, write the text to a new output line.
   *
   *  @param level  Output level to write the message at (one of
   *                {@link #errors}, {@link #warnings}, {@link #info}, or
   *                {@link #debug}.
   *  @param msg    Message to write
   */
  public static void more(int level, String msg) {
    getThreadTrace().t_more(level, msg);
  }

  /** Internal threaded helper for {@link #more(int, String)}. */
  private void t_more(int level, String msg) 
  {
    // If the output level changed and the previous output level was enabled,
    // force change in level to start on next line.
    //
    if (prevMsgLevel != level && ((prevMsgLevel & outputMask) != 0))
      output("", true, true);

    // Set the previous message level to the new value set.
    prevMsgLevel = level;

    // And call the simple more() function to output the message.
    more(msg);
  } // more()

  //////////////////////////////////////////////////////////////////////////////

  /** Gets a thread-specific instance of Trace. If there wasn't one already,
   *  a new one is created.
   */
  private static Trace getThreadTrace() 
  {
    Trace trace = (Trace)threadTraces.get(Thread.currentThread());
    if (trace == null) {
      trace = new Trace();
      threadTraces.put(Thread.currentThread(), trace);
    }
    return trace;
  } // getThreadTrace()

  //////////////////////////////////////////////////////////////////////////////

  /** Private constructor -- used by {@link #getThreadTrace()}. Calculates
   *  a thread ID for this thread, used to prefix output messages.
   */
  private Trace() 
  {
    // Assign a unique thread ID to this instance of the trace. To keep
    // output looking nice in the case of a single thread only, the first
    // thread has no visible ID until a second thread is created.
    //
    synchronized (getClass()) 
    {
      nThreads++;
      if (firstTrace == null) {
        threadId = "";
        firstTrace = this;
      }
      else {
        assert nThreads >= 2;
        if (firstTrace.threadId.length() == 0)
          firstTrace.threadId = "[1] ";
        threadId = "[" + nThreads + "] ";
      }
    }
  } // constructor

  //////////////////////////////////////////////////////////////////////////////

  /** Workhorse output function -- handles tabbing, prefixing the output with
   *  a thread ID, timestamping, adding newlines, and directing to the proper
   *  output PrintStream or Writer.
   */
  private void output(String msg, boolean tabbed, boolean linefeed) 
  {
    String outMsg;

    synchronized (getClass()) 
    {
      // If the previous output was from a different thread, force a newline
      // and tabbing.
      //
      if (prevTrace != this) {
        tabbed = true;
        linefeed = true;
        prevTrace = this;
      }

      // If the caller wants tabbed output, build a final tabbed message 
      // string.
      //
      if (tabbed)
        outMsg = spaces.substring(0, tabCount) + msg;
      else
        outMsg = msg;

      try 
      {
        // Force output to start on the next line.
        if (linefeed) 
        {
          String newLine = autoFlush ? "" : "\n";

          String dateStr = "";
          if (printTimestamps) {
            dateStr = dateFormat.format(new Date(System.currentTimeMillis())) +
                      " ";
          }

          if (writer != null)
            writer.write(newLine + dateStr + threadId);
          else
            printStream.print(newLine + dateStr + threadId);
        }

        if (writer != null)
          writer.write(autoFlush ? (outMsg + "\n") : outMsg);
        else
          printStream.print(autoFlush ? (outMsg + "\n") : outMsg);
      }
      catch (IOException e) {
      }
    } // synchronized
  } // output()

  /** Trace instance for the current thread */
  private static final WeakHashMap threadTraces = new WeakHashMap();

  /** Trace instance that last wrote to the output stream */
  private static Trace prevTrace = null;

  /** First trace instance ever created */
  private static Trace firstTrace = null;

  /** Number of threads that have accessed Trace */
  private static int nThreads = 0;

  /** PrintStream to write to, or null to write to {@link #writer} */
  private static PrintStream printStream = System.out;

  /** Writer to write to, or null to write to {@link #printStream} */
  private static Writer writer = null;

  /** Previous message level */
  private static int prevMsgLevel = silent;

  /** Current mask of which messages to output */
  private static int outputMask = errors | warnings | info;

  /** True to print a timestamp on each line */
  private static boolean printTimestamps = false;

  /** True to force an immediate newline after every output */
  private static boolean autoFlush = false;

  /** Used for tabbing */
  private static final String spaces = "                                                                      " +
                                       "                                                                      " +
                                       "                                                                      ";

  // Following are the only thread-specific variables.

  /** String to prefix messages from this thread with */
  private String threadId = "";

  /** Current tab level for this thread */
  private int tabCount = 0;

  /** Amount to indent when {@link #tab()} is called. */
  private int tabSize = defaultTabSize;
} // class Trace
