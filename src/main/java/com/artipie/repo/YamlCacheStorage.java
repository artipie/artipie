/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.repo;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.YamlStorage;
import com.artipie.asto.Storage;
import java.time.Duration;

/**
 * Proxy cache storage config from YAML.
 * @since 0.23
 */
final class YamlCacheStorage implements CacheStorage {
    /**
     * Cache storage.
     */
    private final Storage cstorage;

    /**
     * Max available size.
     */
    private final Long cmaxsize;

    /**
     * Time to live.
     */
    private final Duration ttl;

    /**
     * Ctor.
     * @param source Source YAML with only cache section
     */
    YamlCacheStorage(final YamlMapping source) {
        this(new YamlStorage(source).storage());
    }

    /**
     * Ctor with default max size and time to live.
     * @param storage Cache storage
     */
    YamlCacheStorage(final Storage storage) {
        this(storage, Long.MAX_VALUE, Duration.ofMillis(Long.MAX_VALUE));
    }

    /**
     * Ctor.
     * @param cstorage Cache storage
     * @param maxsize Max available size
     * @param ttl Time to live
     */
    YamlCacheStorage(final Storage cstorage, final Long maxsize, final Duration ttl) {
        this.cstorage = cstorage;
        this.cmaxsize = maxsize;
        this.ttl = ttl;
    }

    @Override
    public Storage storage() {
        return this.cstorage;
    }

    @Override
    public Long maxSize() {
        return this.cmaxsize;
    }

    @Override
    public Duration timeToLive() {
        return this.ttl;
    }
}
