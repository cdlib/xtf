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

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Handles tedious details of making a little regression test for a given 
 * class.
 * 
 * @author Martin Haye
 */
public abstract class Tester
{
    /** 
     * List of all tests (or at least, tests for all classes that have been
     * loaded.
     */
    private static LinkedList allTests = new LinkedList();
    
    /** Name of this test */
    private String  name;
    
    /** True after test has been tried */
    private boolean testedAlready;
    
    /** Add this test to the global list of tests */
    public Tester( String name ) 
    { 
        this.name = name;
        allTests.add( this );
    }
    
    /**
     * Run all registered tests. It doesn't matter which runs first, since if
     * a test has a dependency it should directly call those tests it depends 
     * on.
     */
    public static final void testAll()
    {
        for( Iterator iter = allTests.iterator(); iter.hasNext(); )
            ((Tester)iter.next()).test();
    } // testAll()

    
    /**
     * Run this particular test. If it has already been run, the test is 
     * skipped.
     */
    public final void test()
    {
        // Don't run this test again if it already ran.
        if( testedAlready )
            return;
        testedAlready = true;
        
        // Make sure assertions are turned on.
        boolean ok = false;
        assert ok = true;
        if( !ok )
            throw new AssertionError( "Must turn on assertions for test()" );
            
        // Run the test
        Trace.info( "Running test '" + name + "'..." );
        try {
            testImpl();
        }
        catch( Exception e ) {
            Trace.error( "... Test '" + name + "' failed: " + e );
            if( e instanceof RuntimeException )
                throw (RuntimeException) e;
            else
                throw new RuntimeException( e );
        }
        Trace.info( "... Test '" + name + "' passed." );
    } // test()
    
    /** 
     * Derived classes should override this method to perform the actual
     * work of the test.
     */
    protected abstract void testImpl() throws Exception;
    
} // class Tester
