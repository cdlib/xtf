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

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.index.TermPositions;
import org.apache.lucene.store.Directory;

/**
 * Wrapper around Lucene's IndexReader which provides the ability to limit the
 * amount of work (i.e. number of docs and positions read) to protect against
 * malicious queries.
 * 
 * @author Martin Haye
 */
public class LimIndexReader extends IndexReader
{
    private IndexReader wrapped;
    
    int workLimit;
    int workCount = 0;
  
    /**
     * Wrap an index reader and enforce the specified limit.
     * 
     * @param toWrap        The reader to wrap
     * @param workLimit     Limit on the amount of wokr
     */
    public LimIndexReader( IndexReader toWrap, int workLimit )
    {
        super( null );
        this.wrapped   = toWrap;
        this.workLimit = workLimit;
    }
    
    /**
     * Called by LimTermDocs and LimTermPositions to notify us that a certain
     * amount of work has been done. We check the limit, and if exceeded, throw
     * an exception.
     * 
     * @param amount    How much work has been done. The unit is typically one
     *                  term or term-position.
     */
    final void work( int amount )
        throws IOException
    {
        workCount += amount;
        if( workCount > workLimit )
            throw new ExcessiveWorkException();
    } // work()
    
    /************************************************************************* 
     * DELEGATED METHODS
     *************************************************************************/
    
    public static long getCurrentVersion( File directory )
        throws IOException
    {
        return IndexReader.getCurrentVersion( directory );
    }
    public static long getCurrentVersion( String directory )
        throws IOException
    {
        return IndexReader.getCurrentVersion( directory );
    }
    public static long getCurrentVersion( Directory directory )
        throws IOException
    {
        return IndexReader.getCurrentVersion( directory );
    }
    public static boolean indexExists( File directory )
    {
        return IndexReader.indexExists( directory );
    }
    public static boolean indexExists( String directory )
    {
        return IndexReader.indexExists( directory );
    }
    public static boolean indexExists( Directory directory )
        throws IOException
    {
        return IndexReader.indexExists( directory );
    }
    public static boolean isLocked( String directory )
        throws IOException
    {
        return IndexReader.isLocked( directory );
    }
    public static boolean isLocked( Directory directory )
        throws IOException
    {
        return IndexReader.isLocked( directory );
    }
    public static IndexReader open( File path )
        throws IOException
    {
        assert false : "Method should never be called";
        throw new RuntimeException();
    }
    public static IndexReader open( String path )
        throws IOException
    {
        assert false : "Method should never be called";
        throw new RuntimeException();
    }
    public static IndexReader open( Directory directory )
        throws IOException
    {
        assert false : "Method should never be called";
        throw new RuntimeException();
    }
    public static void unlock( Directory directory )
        throws IOException
    {
        IndexReader.unlock( directory );
    }
    public Directory directory()
    {
        return wrapped.directory();
    }
    public int docFreq( Term t )
        throws IOException
    {
        return wrapped.docFreq( t );
    }
    public Document document( int n )
        throws IOException
    {
        return wrapped.document( n );
    }
    public boolean equals( Object obj )
    {
        return wrapped.equals( obj );
    }
    public Collection getFieldNames()
        throws IOException
    {
        return wrapped.getFieldNames();
    }
    public Collection getFieldNames( boolean indexed )
        throws IOException
    {
        return wrapped.getFieldNames( indexed );
    }
    public Collection getIndexedFieldNames( boolean storedTermVector )
    {
        return wrapped.getIndexedFieldNames( storedTermVector );
    }
    public TermFreqVector getTermFreqVector( int docNumber, String field )
        throws IOException
    {
        return wrapped.getTermFreqVector( docNumber, field );
    }
    public TermFreqVector[] getTermFreqVectors( int docNumber )
        throws IOException
    {
        return wrapped.getTermFreqVectors( docNumber );
    }
    public boolean hasDeletions()
    {
        return wrapped.hasDeletions();
    }
    public int hashCode()
    {
        return wrapped.hashCode();
    }
    public boolean isDeleted( int n )
    {
        return wrapped.isDeleted( n );
    }
    public int maxDoc()
    {
        return wrapped.maxDoc();
    }
    public byte[] norms( String field )
        throws IOException
    {
        return wrapped.norms( field );
    }
    public void norms( String field, byte[] bytes, int offset )
        throws IOException
    {
        wrapped.norms( field, bytes, offset );
    }
    public int numDocs()
    {
        return wrapped.numDocs();
    }
    public void setNorm( int doc, String field, float value )
        throws IOException
    {
        wrapped.setNorm( doc, field, value );
    }
    public TermDocs termDocs()
        throws IOException
    {
        return new LimTermDocs( this, wrapped.termDocs() );
    }
    public TermDocs termDocs( Term term )
        throws IOException
    {
        return new LimTermDocs( this, wrapped.termDocs(term) );
    }
    public TermPositions termPositions()
        throws IOException
    {
        return new LimTermPositions( this, wrapped.termPositions() );
    }
    public TermPositions termPositions( Term term )
        throws IOException
    {
        return new LimTermPositions( this, wrapped.termPositions(term) );
    }
    public TermEnum terms()
        throws IOException
    {
        return wrapped.terms();
    }
    public TermEnum terms( Term t )
        throws IOException
    {
        return wrapped.terms( t );
    }
    public String toString()
    {
        return wrapped.toString();
    }
    
    protected void doClose() throws IOException
    {
        assert false : "method should never be called";
    }
    protected void doCommit() throws IOException
    {
        assert false : "method should never be called";
    }
    protected void doDelete(int docNum) throws IOException
    {
        assert false : "method should never be called";
    }
    protected void doSetNorm(int doc, String field, byte value) 
          throws IOException
    {
        assert false : "method should never be called";
    }
    protected void doUndeleteAll() throws IOException
    {
        assert false : "method should never be called";
    }

} // class LimIndexReader
