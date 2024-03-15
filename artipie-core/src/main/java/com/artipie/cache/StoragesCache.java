/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.cache;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.ArtipieException;
import com.artipie.asto.Storage;
import com.artipie.asto.factory.Config;
import com.artipie.asto.factory.StoragesLoader;
import com.artipie.asto.misc.Cleanable;
import com.artipie.jfr.JfrStorage;
import com.artipie.jfr.StorageCreateEvent;
import com.artipie.misc.ArtipieProperties;
import com.artipie.misc.Property;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import org.apache.commons.lang3.NotImplementedException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of cache for storages with similar configurations
 * in Artipie settings using {@link LoadingCache}.
 */
public class StoragesCache implements Cleanable<YamlMapping> {

    /**
     * Cache for storages.
     */
    private final Cache<YamlMapping, Storage> cache;

    /**
     * Ctor.
     */
    public StoragesCache() {
        this.cache = CacheBuilder.newBuilder()
            .expireAfterWrite(
                new Property(ArtipieProperties.STORAGE_TIMEOUT).asLongOrDefault(180_000L),
                TimeUnit.MILLISECONDS
            ).softValues()
            .build();
    }

    /**
     * Finds storage by specified in settings configuration cache or creates
     * a new item and caches it.
     *
     * @param yaml Storage settings
     * @return Storage
     */
    public Storage storage(final YamlMapping yaml) {
        final String type = yaml.string("type");
        if (Strings.isNullOrEmpty(type)) {
            throw new ArtipieException("Storage type cannot be null or empty.");
        }
        try {
            return this.cache.get(
                yaml,
                () -> {
                    final Storage res;
                    final StorageCreateEvent event = new StorageCreateEvent();
                    if (event.isEnabled()) {
                        event.begin();
                        res = new JfrStorage(
                            StoragesLoader.STORAGES
                                .newObject(type, new Config.YamlStorageConfig(yaml))
                        );
                        event.storage = res.identifier();
                        event.commit();
                    } else {
                        res = new JfrStorage(
                            StoragesLoader.STORAGES
                                .newObject(type, new Config.YamlStorageConfig(yaml))
                        );
                    }
                    return res;
                }
            );
        } catch (final ExecutionException err) {
            throw new ArtipieException(err);
        }
    }

    /**
     * Returns the approximate number of entries in this cache.
     *
     * @return Number of entries
     */
    public long size() {
        return this.cache.size();
    }

    @Override
    public String toString() {
        return String.format(
            "%s(size=%d)",
            this.getClass().getSimpleName(), this.cache.size()
        );
    }

    @Override
    public void invalidate(final YamlMapping mapping) {
        throw new NotImplementedException("This method is not supported in cached storages!");
    }

    @Override
    public void invalidateAll() {
        this.cache.invalidateAll();
    }
}
