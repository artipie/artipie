/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.cache;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.ArtipieException;
import com.artipie.asto.Storage;
import com.artipie.misc.ArtipieProperties;
import com.artipie.misc.Property;
import com.artipie.settings.Settings;
import com.artipie.settings.YamlStorage;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of cache for storages with similar configurations
 * in Artipie settings using {@link LoadingCache}.
 * @since 0.23
 */
final class CachedStorages implements StoragesCache {
    /**
     * Cache for storages settings.
     */
    private final Cache<YamlMapping, Storage> storages;

    /**
     * Ctor.
     * Here an instance of cache is created. It is important that cache
     * is a local variable.
     */
    CachedStorages() {
        this(
            CacheBuilder.newBuilder()
                .expireAfterWrite(
                    //@checkstyle MagicNumberCheck (1 line)
                    new Property(ArtipieProperties.STORAGE_TIMEOUT).asLongOrDefault(180_000L),
                    TimeUnit.MILLISECONDS
                ).softValues()
                .build()
        );
    }

    /**
     * Ctor.
     * @param cache Cache for storages settings
     */
    CachedStorages(final Cache<YamlMapping, Storage> cache) {
        this.storages = cache;
    }

    @Override
    public Storage storage(final Settings settings) {
        final YamlMapping yaml = settings.meta().yamlMapping("storage");
        if (yaml == null) {
            throw new ArtipieException(
                String.format("Failed to find storage configuration in \n%s", settings)
            );
        }
        return this.storage(yaml);
    }

    @Override
    public Storage storage(final YamlMapping yaml) {
        try {
            return this.storages.get(
                yaml,
                () -> new YamlStorage(yaml).storage()
            );
        } catch (final ExecutionException err) {
            throw new ArtipieException(err);
        }
    }

    @Override
    public void invalidateAll() {
        this.storages.invalidateAll();
    }

    @Override
    public String toString() {
        return String.format(
            "%s(size=%d)",
            this.getClass().getSimpleName(), this.storages.size()
        );
    }
}
