package org.cdlib.xtf.textEngine.workLimiter;

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

import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;

/**
 * Used by LimIndexReader to help enforce the work limit while processing a
 * query.
 * 
 * @author Martin Haye
 */
class LimTermDocs implements TermDocs
{
    private LimIndexReader reader;
    private TermDocs       wrapped;
  
    public LimTermDocs( LimIndexReader reader, TermDocs toWrap )
    {
        this.wrapped = toWrap;
        this.reader  = reader;
    }
  
    /************************************************************************* 
     * DELEGATED METHODS
     *************************************************************************/

    public void close()
        throws IOException
    {
        wrapped.close();
    }
    public int doc()
    {
        return wrapped.doc();
    }
    public boolean equals( Object obj )
    {
        return wrapped.equals( obj );
    }
    public int freq()
    {
        return wrapped.freq();
    }
    public int hashCode()
    {
        return wrapped.hashCode();
    }
    public boolean next()
        throws IOException
    {
        reader.work( 1 );
        return wrapped.next();
    }
    public int read( int[] docs, int[] freqs )
        throws IOException
    {
        int nRead = wrapped.read( docs, freqs );
        reader.work( nRead );
        return nRead;
    }
    public void seek( Term term )
        throws IOException
    {
        reader.work( 1 );
        wrapped.seek( term );
    }
    public void seek( TermEnum termEnum )
        throws IOException
    {
        reader.work( 1 );
        wrapped.seek( termEnum );
    }
    public boolean skipTo( int target )
        throws IOException
    {
        reader.work( 1 );
        return wrapped.skipTo( target );
    }
    public String toString()
    {
        return wrapped.toString();
    }
} // class LimTermDocs
