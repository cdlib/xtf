package org.cdlib.xtf.saxonExt.exec;

/*
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
 * 
 * Acknowledgements:
 * 
 * A significant amount of new and/or modified code in this module
 * was made possible by a grant from the Andrew W. Mellon Foundation,
 * as part of the Melvyl Recommender Project.
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.SimpleExpression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.om.AllElementStripper;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.AxisIterator;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.style.ExtensionInstruction;
import net.sf.saxon.tinytree.TinyBuilder;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.StringValue;

/**
 * Implements a Saxon instruction that executes an external process and
 * properly formats the result. Provides timeout and error checking.
 * 
 * @author Martin Haye
 */
public class RunElement extends ExtensionInstruction 
{
  String command;
  int    timeoutMsec;
  
  public void prepareAttributes() throws TransformerConfigurationException 
  {
    // Get mandatory 'command' attribute
    command = getAttributeList().getValue("", "command");
    if( command == null )
        reportAbsence( "command" );
    
    // Get optional 'timeout' attribute
    String timeoutStr = getAttributeList().getValue( "", "timeout" );
    if( timeoutStr == null )
        timeoutMsec = 0;
    else {
        try {
            timeoutMsec = (int) (Float.parseFloat(timeoutStr) * 1000);
            timeoutMsec = Math.max( 0, timeoutMsec );
        } catch( NumberFormatException e ) {
            compileError( "'timeout' must be a number" ); 
        }
    }
  } // prepareAttributes()
    
  public Expression compile(Executable exec) 
    throws TransformerConfigurationException 
  {
    return new RunInstruction(command, timeoutMsec, getArgInstructions(exec));
  }

  public List getArgInstructions(Executable exec) 
    throws TransformerConfigurationException 
  {
    List list = new ArrayList(10);
    
    AxisIterator kids = iterateAxis(Axis.CHILD);
    NodeInfo child;
    while (true) {
        child = (NodeInfo)kids.next();
        if( child == null )
            break;
        if( child instanceof ArgElement )
            list.add( ((ArgElement)child).compile(exec) );
        if( child instanceof InputElement ) {
            list.add( ((InputElement)child).compile(exec) );
        }
    }

    return list;
  } // getArgInstructions()

  private static class RunInstruction extends SimpleExpression 
  {
    String     command;
    int        timeout;
    int        nArgs;
    InputElement.InputInstruction inputExpr;

    public RunInstruction( String     command, 
                           int        timeout, 
                           List       args )
    {
        this.command = command;
        this.timeout = timeout;
        
        nArgs = args.size();
        
        if( args.size() > 0 &&
            args.get(args.size()-1) instanceof InputElement.InputInstruction )
        {
            inputExpr = (InputElement.InputInstruction) args.get(args.size()-1); 
            --nArgs;
        }

        Expression[] sub = new Expression[args.size()];
        for (int i=0; i<args.size(); i++)
            sub[i] = (Expression)args.get(i);
        setArguments(sub);
    }

    /**
     * A subclass must provide one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of the three is provided.
     */

    public int getImplementationMethod() {
      return Expression.EVALUATE_METHOD;
    }

    public String getExpressionType() {
      return "exec:run";
    }

    public Item evaluateItem(XPathContext context) throws XPathException 
    {
      ArrayList args = new ArrayList( 10 );
      
      // Put the command first in our list of arguments.
      args.add( command );
      
      // Gather all the arguments
      for( int c = 0; c < nArgs; c++ ) 
      {
          String strVal = ((ArgElement.ArgInstruction)arguments[c]).
                                  getSelectValue(context).toString();
          args.add( strVal );
      } // for c
      
      // Is there input to send to the process?
      byte[] inputBytes = null;
      if( inputExpr != null )
          inputBytes = inputExpr.getStream( context );
      
      // Get ready to go
      Process       process       = null;
      
      InputStream   stdout        = null;
      OutputGrabber stdoutGrabber = null;
      InputStream   stderr        = null;
      OutputGrabber stderrGrabber = null;
      
      OutputStream  stdin         = null;
      InputStuffer  stdinStuffer  = null;
      
      Timer         timer         = new Timer();
      Interrupter   interrupter   = null;
      
      try {
          // Fire up the process.
          String[] argArray = (String[]) args.toArray(new String[args.size()]);
          process = Runtime.getRuntime().exec( argArray );
          
          // Stuff input into it, if necessary.
          if( inputBytes != null ) {
              stdin        = process.getOutputStream();
              stdinStuffer = new InputStuffer( stdin, inputBytes );
              stdinStuffer.start();
          }
          
          // Grab all the output
          stdout        = process.getInputStream();
          stdoutGrabber = new OutputGrabber( stdout );
          stderr        = process.getErrorStream();
          stderrGrabber = new OutputGrabber( stderr );
          
          stdoutGrabber.start();
          stderrGrabber.start();
          
          // Set a timer so we can stop the process if it exceeds the timeout.
          if( timeout > 0 ) {
              interrupter = new Interrupter( Thread.currentThread() );
              timer.schedule( interrupter, timeout );
          }
          
          // Wait for the process to finish.
          process.waitFor();
      } // try
      catch( IOException e ) {
	      if( stdin != null )
	          try { stdin.close();  } catch( IOException e2 ) { /*ignore*/ }
	      if( stdout != null )
	          try { stdout.close(); } catch( IOException e2 ) { /*ignore*/ }
	      if( stderr != null )
	          try { stderr.close(); } catch( IOException e2 ) { /*ignore*/ }
          dynamicError( 
              "IO exception occurred processing external command '" + command + 
              "': " + e, context );
      }
      catch( InterruptedException e ) {
          if( stdinStuffer != null )
              stdinStuffer.interrupt();
          if( stdoutGrabber != null )
              stdoutGrabber.interrupt();
          if( stderrGrabber != null )
              stderrGrabber.interrupt();
          if( process != null )
              process.destroy();
          dynamicError( 
              "External command '" + command + "' exceeded timeout of " +
                  (new DecimalFormat().format(timeout/1000.0)) + " sec",
              context );
      }
      finally {
          if( interrupter != null ) {
              synchronized( interrupter ) {
                  timer.cancel(); // avoid further interruptions
                  interrupter.mainThread = null; // make sure it can't get to us
                  Thread.interrupted(); // clear out the interrupted flag.
              }
          }
      }
      
      // Wait for the stuffer and grabbers to finish their work.
      try {
          if( stdinStuffer != null ) {
              while( true ) {
                  synchronized( stdinStuffer ) {
                      if( stdinStuffer.done )
                          break;
                      stdinStuffer.wait();
                  }
              }
          }
          while( true ) {
              synchronized( stdoutGrabber ) {
                  if( stdoutGrabber.done )
                      break;
                  stdoutGrabber.wait();
              }
          }
          while( true ) {
              synchronized( stderrGrabber ) {
                  if( stderrGrabber.done )
                      break;
                  stderrGrabber.wait();
              }
          }
      }
      catch( InterruptedException e ) {
          assert false : "should not be interrupted at this stage";
      }
      
      // Make sure all the streams are closed (to avoid leaking.)
      if( stdin != null )
          try { stdin.close();  } catch( IOException e ) { /*ignore*/ }
      if( stdout != null )
          try { stdout.close(); } catch( IOException e ) { /*ignore*/ }
      if( stderr != null )
          try { stderr.close(); } catch( IOException e ) { /*ignore*/ }

      // If we got a non-zero exit status, and something came out on stderr,
      // then throw an exception.
      //
      if( process.exitValue() != 0 && stderrGrabber.outBytes.length > 0 )
      {
          String errStr = new String( stderrGrabber.outBytes );
          dynamicError( 
              "External command '" + command + "' exited with status " +
                  process.exitValue() + ". Output from stderr:\n" +
                  errStr,
              context );
      }
      
      // See if we got XML.
      byte[] outBytes = stdoutGrabber.outBytes;
      byte[] lookFor  = "<?xml".getBytes();
      int i;
      for( i = 0; i < lookFor.length; i++ ) {
          if( i >= outBytes.length || outBytes[i] != lookFor[i] )
              break;
      }
      
      if( i < lookFor.length ) 
      {
          // Doesn't look like XML. Just parse it as a string.
          return new StringValue( new String(outBytes) );
      }
      
      // Ooh, we got some XML. Let's make a real tree out of it.
      StreamSource src = new StreamSource( new ByteArrayInputStream(outBytes) );
      NodeInfo doc = null;
      try {
          doc = TinyBuilder.build( src, new AllElementStripper(), 
                     context.getController().getConfiguration() );
      }
      catch( XPathException e ) {
          dynamicError( 
              "Error parsing XML output from external command '" + command + 
              "': " + e, context );
      }
      
      // All done.
      return doc;

    } // evaluateItem()

  } // class RunInstruction

  /** Used to interrupt the main thread if a timeout occurs */
  private static class Interrupter extends TimerTask
  {
    public Thread mainThread;
    
    public Interrupter( Thread mainThread ) {
        this.mainThread = mainThread;
    }
    
    public synchronized void run()
    {
        if( mainThread != null )
            mainThread.interrupt();
    }
  } // class Interrupter
  
  /**
   * Class to stuff input into the process's input stream (an OutputStream to
   * us).
   */
  private static class InputStuffer extends Thread
  {
    private OutputStream          outStream;
    private byte[]                bytes;
    
    public Throwable error;
    
    public boolean   done = false;
    
    public InputStuffer( OutputStream stream, byte[] bytes )
      throws UnsupportedEncodingException
    {
      this.outStream = stream;
      this.bytes     = bytes;
    }
    
    public void run()
    {
      try {
          // Write all the data.
          outStream.write( bytes );
          
          // Inform the process that this is the end now.
          outStream.close();
      } // try
      catch( IOException e ) {
          error = e;
      }
      finally {
          synchronized( this ) {
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
    private InputStream           inStream;
    private ByteArrayOutputStream buffer = new ByteArrayOutputStream( 100 );
    
    public byte[]    outBytes = new byte[0];
    public Throwable error;
    
    public boolean   done = false;
    
    public OutputGrabber( InputStream stream  ) {
      this.inStream = stream;
    }
    
    public void run()
    {
      // Read data until it's exhausted (we will be interrupted when it's time
      // to stop.)
      //
      try {
          byte[] tmp = new byte[4096];
          while( true )
          {
              // Read some stuff.
              int got = inStream.read( tmp );
              if( got < 0 )
                  break;
              
              // Save it up.
              buffer.write( tmp, 0, got );
          }
          
          // Get a byte array that the caller can process.
          outBytes = buffer.toByteArray();
      } // try
      catch( IOException e ) {
          error = e;
      }
      finally {
          synchronized( this ) {
              done = true;
              notifyAll();
          }
      }
    } // run()
  } // class OutputGrabber
  
} // class ExecInstruction
