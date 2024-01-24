/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.settings.cache;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.ArtipieException;
import com.artipie.asto.Storage;
import com.artipie.asto.factory.Config;
import com.artipie.asto.factory.StoragesLoader;
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
import org.apache.commons.lang3.NotImplementedException;

/**
 * Implementation of cache for storages with similar configurations
 * in Artipie settings using {@link LoadingCache}.
 *
 * @since 0.23
 */
public class CachedStorages implements StoragesCache {

    /**
     * Storages factory.
     */
    public static final StoragesLoader STORAGES = new StoragesLoader();

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
                        res = new JfrStorage(
                            CachedStorages.STORAGES
                                .newObject(type, new Config.YamlStorageConfig(yaml))
                        );
                        event.storage = res.identifier();
                        event.commit();
                    } else {
                        res = new JfrStorage(
                            CachedStorages.STORAGES
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

    @Override
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
