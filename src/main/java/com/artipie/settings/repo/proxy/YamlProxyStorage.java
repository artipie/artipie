/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.repo.proxy;

import com.artipie.asto.Storage;
import java.time.Duration;

/**
 * Proxy cache storage config from YAML.
 * @since 0.23
 */
final class YamlProxyStorage implements ProxyConfig.CacheStorage {

    /**
     * Cache storage.
     */
    private final Storage asto;

    /**
     * Max available size.
     */
    private final Long size;

    /**
     * Time to live.
     */
    private final Duration ttl;

    /**
     * Ctor with default max size and time to live.
     * @param storage Cache storage
     */
    YamlProxyStorage(final Storage storage) {
        this(storage, Long.MAX_VALUE, Duration.ofMillis(Long.MAX_VALUE));
    }

    /**
     * Ctor.
     * @param storage Cache storage
     * @param maxsize Max available size
     * @param ttl Time to live
     */
    YamlProxyStorage(final Storage storage, final Long maxsize, final Duration ttl) {
        this.asto = storage;
        this.size = maxsize;
        this.ttl = ttl;
    }

    @Override
    public Storage storage() {
        return this.asto;
    }

    @Override
    public Long maxSize() {
        return this.size;
    }

    @Override
    public Duration timeToLive() {
        return this.ttl;
    }

}
