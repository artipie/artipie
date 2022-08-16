/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.cache;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.ArtipieException;
import com.artipie.MeasuredStorage;
import com.artipie.asto.Storage;
import com.artipie.asto.misc.UncheckedScalar;
import com.artipie.misc.ArtipieProperties;
import com.artipie.misc.Property;
import com.artipie.settings.Settings;
import com.artipie.settings.YamlStorage;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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
    private final Cache<Metadata, Storage> storages;

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
    CachedStorages(final Cache<Metadata, Storage> cache) {
        this.storages = cache;
    }

    @Override
    public Storage storage(final Settings settings) {
        final Metadata meta = new Metadata(settings);
        return new UncheckedScalar<>(
            () -> this.storages.get(
                meta,
                () -> new MeasuredStorage(
                    new YamlStorage(meta.storageMeta()).storage()
                )
            )
        ).value();
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

    /**
     * Extra class for using metadata information in static section.
     * @since 0.22
     */
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    static final class Metadata {
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
