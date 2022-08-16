/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.repo.proxy;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Storage;
import com.artipie.settings.YamlStorage;
import java.time.Duration;
import java.util.Optional;

/**
 * Proxy cache storage config from YAML.
 * @since 0.23
 */
final class YamlProxyStorage implements ProxyConfig.CacheStorage {
    /**
     * Max size key in proxy config.
     */
    static final String MAX_SIZE = "max-size";

    /**
     * Time to live key in proxy config.
     */
    static final String TTL = "time-to-live";

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
     * Ctor.
     * @param source Source YAML with only cache section
     */
    YamlProxyStorage(final YamlMapping source) {
        this(
            new YamlStorage(
                Optional.ofNullable(source.value("storage"))
                    .orElseThrow(
                        () -> new IllegalStateException("'storage' key is absent in proxy config")
                    ).asMapping()
            ).storage(),
            YamlProxyStorage.valueOf(YamlProxyStorage.MAX_SIZE, source),
            Duration.ofMillis(YamlProxyStorage.valueOf(YamlProxyStorage.TTL, source))
        );
    }

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

    /**
     * Obtains value of node from yaml config.
     * @param key Node key
     * @param yaml Yaml config
     * @return Value of node from yaml config or default value.
     */
    private static long valueOf(final String key, final YamlMapping yaml) {
        return Optional.ofNullable(yaml.value(key)).map(
            node -> Long.valueOf(node.asScalar().value())
        ).orElse(Long.MAX_VALUE);
    }
}
