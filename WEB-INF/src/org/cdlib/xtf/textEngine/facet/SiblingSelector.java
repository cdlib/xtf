package org.cdlib.xtf.textEngine.facet;

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
 */

import java.util.HashSet;

/** Select the top level of the hierarchy that has a choice. */
public class SiblingSelector extends GroupSelector
{
  private HashSet parents = new HashSet();
  
  public void reset( boolean conservative ) {
    super.reset( conservative );
    parents.clear();
  }
  
  public void process( int group ) 
  {
    // In conservative mode, we have to select the entire tree
    if( conservative ) {
        next.process( group );
        return;
    }
    
    // Normal (non-conservative mode)... Have we seen this parent before?
    // If so, ignore it.
    //
    int parent = counts.parent( group );
    Integer parentKey = Integer.valueOf( parent );
    if( parents.contains(parentKey) )
        return;
    
    // Okay, process all the children under this parent.
    for( int kid = counts.child(parent); kid >= 0; kid = counts.sibling(kid) ) {
        if( !counts.shouldInclude(kid) )
            continue;
        next.process( kid );
    }
    
    // And record that we've finished this parent now.
    parents.add( parentKey );
  } // process()
  
  public String toString() {
    return "siblings -> " + next.toString();
  }
}