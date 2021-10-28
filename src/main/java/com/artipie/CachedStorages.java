/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Storage;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of cache for storages with similar configurations
 * in Artipie settings using {@link LoadingCache}.
 * @since 0.23
 */
final class CachedStorages implements StorageConfigCache {
    /**
     * Cache for storages settings.
     */
    private static LoadingCache<Metadata, Storage> storages;

    static {
        final int timeout;
        try {
            timeout = Integer.parseInt(
                Optional.ofNullable(
                    System.getProperty(ArtipieProperties.STORAGE_TIMEOUT)
                ).flatMap(ignored -> new ArtipieProperties().storageCacheTimeout())
                .orElse("180000")
            );
        } catch (final NumberFormatException exc) {
            throw new ArtipieException(
                String.format("Failed to read property '%s'", ArtipieProperties.STORAGE_TIMEOUT),
                exc
            );
        }
        CachedStorages.storages = CacheBuilder.newBuilder()
            .expireAfterAccess(timeout, TimeUnit.MILLISECONDS)
            .softValues()
            .build(
                new CacheLoader<>() {
                    @Override
                    public Storage load(final Metadata meta) {
                        return new MeasuredStorage(
                            new YamlStorage(meta.storageMeta()).storage()
                        );
                    }
                }
            );
    }

    @Override
    public Storage storage(final Settings settings) {
        return CachedStorages.storages.getUnchecked(new Metadata(settings));
    }

    @Override
    public void invalidateAll() {
        CachedStorages.storages.invalidateAll();
    }

    @Override
    public String toString() {
        return String.format(
            "%s(size=%d)",
            this.getClass().getSimpleName(), CachedStorages.storages.size()
        );
    }

    /**
     * Extra class for using metadata information in static section.
     * @since 0.22
     */
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    private static final class Metadata {
        /**
         * Settings of Artipie server.
         */
        private final Settings csettings;

        /**
         * Ctor.
         * @param settings Settings
         */
        Metadata(final Settings settings) {
            this.csettings = settings;
        }

        @Override
        public boolean equals(final Object obj) {
            final boolean res;
            if (this == obj) {
                res = true;
            } else if (obj == null || this.getClass() != obj.getClass()) {
                res = false;
            } else {
                final Metadata meta = (Metadata) obj;
                res = Objects.equals(this.storageMeta(), meta.storageMeta());
            }
            return res;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.storageMeta());
        }

        /**
         * Obtains settings.
         * @return Settings.
         */
        Settings settings() {
            return this.csettings;
        }

        /**
         * Obtains meta information about storage configuration.
         * @return Information about storage configuration.
         */
        YamlMapping storageMeta() {
            return Optional.ofNullable(
                this.csettings.meta().yamlMapping("storage")
            ).orElseThrow(
                () -> new ArtipieException(
                    String.format("Failed to find storage configuration in \n%s", this.csettings)
                )
            );
        }
    }
}
