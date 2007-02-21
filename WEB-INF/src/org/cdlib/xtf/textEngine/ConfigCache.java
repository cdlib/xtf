package org.cdlib.xtf.textEngine;


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
import org.cdlib.xtf.cache.GeneratingCache;
import org.cdlib.xtf.textIndexer.IndexerConfig;
import org.cdlib.xtf.textIndexer.XMLConfigParser;
import org.cdlib.xtf.util.Path;

/**
 * Used to maintain a simple cache of config files, so we don't have to keep
 * loading the same one over and over.
 */
public class ConfigCache extends GeneratingCache 
{
  /**
   * Default constructor - defines the default cache size and expiration time.
   * These should probably be configurable in some file somewhere, but does
   * anyone really care?
   */
  public ConfigCache() {
    super(50, 300); // 50 entries, 300 seconds
  }

  /** Find or load the configuration given its File */
  public IndexerConfig find(File configFile, String indexName)
    throws Exception 
  {
    ConfigCacheKey key = new ConfigCacheKey();
    key.configPath = Path.normalizeFileName(configFile.toString());
    key.indexName = indexName;
    return (IndexerConfig)super.find(key);
  } // find()

  /** Load a configuration given its path */
  protected Object generate(Object key)
    throws Exception 
  {
    ConfigCacheKey realKey = (ConfigCacheKey)key;

    IndexerConfig config = new IndexerConfig();
    config.xtfHomePath = System.getProperty("xtf.home");
    config.cfgFilePath = realKey.configPath;
    config.indexInfo.indexName = realKey.indexName;

    XMLConfigParser parser = new XMLConfigParser();
    parser.configure(config);

    return config;
  } // generate()

  /**
   * A key in the ConfigCache.
   */
  private static class ConfigCacheKey 
  {
    public String configPath;
    public String indexName;

    public int hashCode() {
      return configPath.hashCode() ^ indexName.hashCode();
    }

    public boolean equals(ConfigCacheKey other) {
      return configPath.equals(other.configPath) &&
             indexName.equals(other.indexName);
    }
  } // class ConfigCacheKey
}
