package org.cdlib.xtf.lazyTree;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;

import org.cdlib.xtf.util.Trace;

import net.sf.saxon.Controller;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.trace.InstructionInfo;
import net.sf.saxon.trace.TraceListener;

/**
 * Used to keep track of the current instruction being executed, and to
 * keep track of counts for each one.
 * 
 * @author Martin Haye
 */
class ProfilingListener implements TraceListener
{
    /** 
     * Stack of instructions, used to keep track of what XSLT instruction is
     * being processed.
     */
    private LinkedList   instructionStack = new LinkedList();
    
    /** Dummy used for counting nodes when no instruction is specified */
    private ProfileCount emptyInstr = new ProfileCount();

    /** Keeps a count of how many nodes are accessed by each instruction. */
    private HashMap      countMap = new HashMap();
    
    /** Saxon Controller used to control the transformation */
    private Controller   controller;
    
    /** Default constructor; attaches to given controller and document */
    public ProfilingListener( Controller controller ) {
        this.controller = controller;
    }

    /** Unused */
    public void open() { }

    /** Unused */
    public void close() { }

    /**
     * Record the instruction being entered, so that subsequent counts can
     * be attributed to it.
     */
    public void enter(InstructionInfo instruction, XPathContext context)
    {
        if( false ) {
            for( int i = 0; i < instructionStack.size(); i++ )
                Trace.debug( "  " );
            String s = instruction.getSystemId();
            s = s.substring( s.lastIndexOf('/') + 1 );
            Trace.debug( s + ":" + instruction.getLineNumber() );
        }
        
        instructionStack.addLast( new ProfileCount(instruction.getSystemId(),
                                                   instruction.getLineNumber()) );
    }

    /**
     * Called when an instruction is exited. Subsequent counts get applied to
     * the instruction that was previously active.
     */
    public void leave(InstructionInfo instruction)
    {
    }

    /** Unused */
    public void startCurrentItem(Item currentItem) { }

    /** Unused */
    public void endCurrentItem(Item currentItem) { }

    /**
     * Increases the count by one for the current instruction.
     */
    public void bumpCount( int nodeNum )
    {
        ProfileCount instr;
        if( instructionStack.isEmpty() )
            instr = emptyInstr;
        else
            instr = (ProfileCount) instructionStack.getLast();
        
        ProfileCount pc = (ProfileCount) countMap.get(instr);
        if( pc == null ) {
            pc = new ProfileCount(instr.systemId, instr.lineNum);
            countMap.put( instr, pc );
        }
        
        if( !pc.nodes.containsKey(new Integer(nodeNum)) ) {
            pc.count++;
            pc.nodes.put( new Integer(nodeNum), new Boolean(true) );
        }
    } // bumpCount()

    /**
     * Gets a list of all the counts, sorted by ascending count.
     */
    public ProfileCount[] getCounts()
    {
        ArrayList list = new ArrayList( countMap.values() );
        Collections.sort( list, new Comparator() {
            public int compare( Object o1, Object o2 ) {
                ProfileCount p1 = (ProfileCount) o1;
                ProfileCount p2 = (ProfileCount) o2;
                if( p1.count != p2.count )
                    return p1.count - p2.count;
                if( !p1.systemId.equals(p2.systemId) )
                    return p1.systemId.compareTo(p2.systemId);
                return p1.lineNum - p2.lineNum;
            }
        } );
        
        ProfileCount[] array = new ProfileCount[ list.size() ];
        list.toArray( array );
        return array;
    } // getCounts()

    /**
     * Simple data structure to keep track of counts.
     */
    public class ProfileCount
    {
        /** ID representing the XSLT file of the instruction */
        public String  systemId;
        
        /** Line number of the instruction within the XSLT file */
        public int     lineNum;
        
        /** Count of how many nodes were referenced to serve this instr */
        public int     count;
        
        /** Map of each node hit by this instruction */
        public HashMap nodes = new HashMap();
        
        /** Construct an empty ProfileCount */
        public ProfileCount() { }
        
        /** Construct a ProfileCount referencing a specific instruction */
        public ProfileCount( String systemId, int lineNum ) {
            this.systemId = systemId;
            this.lineNum  = lineNum;
        }
        
        /** Obtain a hash code so that ProfileCounts can be stored in a map */
        public int hashCode() {
            if( systemId != null )
                return lineNum ^ systemId.hashCode();
            return lineNum;
        }
        
        /** Determine if this ProfileCount is the same as another */
        public boolean equals( Object other ) {
            if( !(other instanceof ProfileCount) )
                return false;
            ProfileCount p = (ProfileCount) other;
            return p.systemId == systemId && p.lineNum == lineNum;
        }
        
    } // class ProfileCount
        
    /**
     * Returns the controller used for the transformation.
     */
    public Controller getController()
    {
        return controller;
    }

} // class ProfilingListener
