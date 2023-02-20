/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.cache;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.ArtipieException;
import com.artipie.asto.Storage;
import com.artipie.asto.factory.Storages;
import com.artipie.jfr.JfrStorage;
import com.artipie.jfr.StorageCreateEvent;
import com.artipie.misc.ArtipieProperties;
import com.artipie.misc.Property;
import com.artipie.settings.Settings;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of cache for storages with similar configurations
 * in Artipie settings using {@link LoadingCache}.
 *
 * @since 0.23
 * @checkstyle DesignForExtensionCheck (500 lines)
 */
public class CachedStorages implements StoragesCache {

    /**
     * Storages factory.
     */
    private static final Storages STORAGES = new Storages();

    /**
     * Cache for storages.
     */
    private final Cache<YamlMapping, Storage> cache;

    /**
     * Ctor.
     */
    public CachedStorages() {
        this.cache = CacheBuilder.newBuilder()
            .expireAfterWrite(
                //@checkstyle MagicNumberCheck (1 line)
                new Property(ArtipieProperties.STORAGE_TIMEOUT).asLongOrDefault(180_000L),
                TimeUnit.MILLISECONDS
            ).softValues()
            .build();
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
            return this.cache.get(
                yaml,
                () -> {
                    final String type = yaml.string("type");
                    if (Strings.isNullOrEmpty(type)) {
                        throw new IllegalArgumentException("Storage type cannot be null or empty.");
                    }
                    final Storage res;
                    final StorageCreateEvent event = new StorageCreateEvent();
                    if (event.isEnabled()) {
                        event.begin();
                        res = new JfrStorage(CachedStorages.STORAGES.newStorage(type, yaml));
                        event.storage = res.identifier();
                        event.commit();
                    } else {
                        res = new JfrStorage(CachedStorages.STORAGES.newStorage(type, yaml));
                    }
                    return res;
                }
            );
        } catch (final ExecutionException err) {
            throw new ArtipieException(err);
        }
    }

    @Override
    public long size() {
        return this.cache.size();
    }

    @Override
    public void invalidate() {
        this.cache.invalidateAll();
    }

    @Override
    public String toString() {
        return String.format(
            "%s(size=%d)",
            this.getClass().getSimpleName(), this.cache.size()
        );
    }
}
