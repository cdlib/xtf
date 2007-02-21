package org.cdlib.xtf.util;


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

/*
 * This file created on Mar 11, 2005 by Martin Haye
 */
import java.io.IOException;

/**
 * A simple structured storage with a flat top-level directory.
 * Substores can be added to an existing storage using {@link #createSubStore(String)},
 * and accessed later using {@link #openSubStore(String)}.
 *
 * @author Martin Haye
 */
public interface StructuredStore 
{
  /**
   * Create a new sub-store with the specified name. Returns a SubStore that
   * has most of the interface of a RandomAccessFile, except that seeks
   * will be relative to the sub-store start.
   *
   * Only one substore may be created at a time (though many others may be
   * opened, provided they were created before.)
   *
   * The caller must call SubStore.close() when the file is complete, to
   * ensure that the directory gets written.
   *
   * @param name  Name of the sub-file to create. Must not exist.
   * @return      A sub-store to write to.
   */
  SubStoreWriter createSubStore(String name)
    throws IOException;

  /**
   * Opens a pre-existing sub-store for read (or write). Returns a sub-store that
   * has most of the interface of a RandomAccessFile, except that seeks will
   * be relative to the sub-file start, and IO operations cannot exceed the
   * boundaries of the sub-file.
   *
   * Many sub-stores may be open simultaneously; each one has an independent
   * file pointer. Each one is light weight, so it's okay to have many open
   * at a time.
   *
   * @param name  Name of pre-existing sub-store to open.
   */
  SubStoreReader openSubStore(String name)
    throws IOException;

  /** Gets the path, URI, or other unique identifier for this store */
  String getSystemId();

  /**
   * Sets a user-defined version number for the file. It can be retrieved
   * later with {@link #getUserVersion()}.
   *
   * @param ver   The version number to set.
   */
  void setUserVersion(String ver)
    throws IOException;

  /**
   * Gets the user version (if any) set by {@link #setUserVersion(String)}.
   */
  String getUserVersion();

  /**
   * Closes the store. This should always be called, to ensure that all
   * sub-stores have been closed and that the directory has been written.
   */
  void close()
    throws IOException;

  /** Deletes the storage completely (implies close() first) */
  void delete()
    throws IOException;
}
