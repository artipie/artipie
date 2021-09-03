/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.repo;

import com.artipie.asto.Storage;
import java.time.Duration;

/**
 * Proxy cache storage config.
 * @since 0.23
 */
public interface CacheStorage {
    /**
     * Storage settings of cache.
     * @return Storage.
     */
    Storage storage();

    /**
     * Max available size of cache storage in bytes.
     * @return Max available size.
     */
    Long maxSize();

    /**
     * Obtains time to live for cache storage.
     * @return Time to live for cache storage.
     */
    Duration timeToLive();
}
