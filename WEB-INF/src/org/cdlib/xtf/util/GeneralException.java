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

/**
 * A general-purpose exception used for problems that may occasionally happen
 * and are expected to. When one of these is thrown, the errorGen stylesheet
 * only receives a callstack if isSevere() returns true.
 */
public class GeneralException extends RuntimeException
{
    /** 
     * Default constructor.
     *
     * @param message   Description of what happened
     */
    public GeneralException( String message )
    {
        super( message );
    }

    /**
     * Constructor that includes a reference to the exception that caused
     * this one.
     *
     * @param message   Description of what happened
     * @param cause     The exception that caused this one.
     */
    public GeneralException( String message, Throwable cause )
    {
        super( message, cause );
    }

    /** Sets an attribute for further information on the exception.  */
    public void set( String attribName, String attribValue )
    {
        attribs.put( attribName, attribValue );
    }
    
    /** 
     * Tells whether this is a really bad problem. Derived classes should
     * override if it's not. If this method returns true, a call stack is
     * passed to the errorGen stylesheet.
     */
    public boolean isSevere() { return true; }

    /** Attributes that give more info on the exception */
    public AttribList attribs = new AttribList();

} // class GeneralException
