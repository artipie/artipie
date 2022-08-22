/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.cache;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlInput;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.misc.UncheckedIOFunc;
import com.artipie.asto.misc.UncheckedScalar;
import com.artipie.misc.ArtipieProperties;
import com.artipie.misc.Property;
import com.artipie.settings.ConfigFile;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of cache for credentials with similar configurations
 * in Artipie settings using {@link LoadingCache}.
 * @since 0.23
 */
public final class CachedCreds implements CredsConfigCache {
    /**
     * Cache for credentials settings.
     */
    private final Cache<Metadata, CompletionStage<YamlMapping>> creds;

    /**
     * Ctor.
     * Here an instance of cache is created. It is important that cache
     * is a local variable.
     */
    public CachedCreds() {
        this(
            CacheBuilder.newBuilder()
                .expireAfterWrite(
                    //@checkstyle MagicNumberCheck (1 line)
                    new Property(ArtipieProperties.CREDS_TIMEOUT).asLongOrDefault(180_000L),
                    TimeUnit.MILLISECONDS
                ).softValues()
                .build()
        );
    }

    /**
     * Ctor.
     * @param cache Credentials configuration cache
     */
    public CachedCreds(final Cache<Metadata, CompletionStage<YamlMapping>> cache) {
        this.creds = cache;
    }

    @Override
    public CompletionStage<YamlMapping> credentials(final Storage storage, final Key path) {
        return new UncheckedScalar<>(
            () -> this.creds.get(
                new Metadata(storage, path),
                () -> new ConfigFile(path).valueFrom(storage)
                    .thenApply(PublisherAs::new)
                    .thenCompose(PublisherAs::asciiString)
                    .thenApply(Yaml::createYamlInput)
                    .thenApply(new UncheckedIOFunc<>(YamlInput::readYamlMapping))
            )
        ).value();
    }

    @Override
    public void invalidateAll() {
        this.creds.invalidateAll();
    }

    @Override
    public String toString() {
        return String.format(
            "%s(size=%d)",
            this.getClass().getSimpleName(), this.creds.size()
        );
    }

    /**
     * Extra class for using metadata information in static section.
     * It is important that storages here are obtained from cache because
     * it allows comparing by keys (e.g. path to file with credentials) and
     * storages. It compares by storages because otherwise it would not be
     * possible to cache different values for the same paths but different storages.
     * @since 0.23
     */
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    static final class Metadata {
        /**
         * Storage.
         */
        private final Storage cstorage;

        /**
         * Path to the credentials file.
         */
        private final Key cpath;

        /**
         * Ctor.
         * @param storage Storage
         * @param path Path to the credentials file
         */
        Metadata(final Storage storage, final Key path) {
            this.cstorage = storage;
            this.cpath = path;
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
                final boolean keys = Key.CMP_STRING.compare(
                    this.cpath, meta.cpath
                ) == 0;
                res = keys && this.cstorage.equals(meta.cstorage);
            }
            return res;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.cstorage, this.cpath.string());
        }

        /**
         * Obtains storage.
         * @return Storage.
         */
        public Storage storage() {
            return this.cstorage;
        }

        /**
         * Obtains path to credentials file.
         * @return Path to credentials file.
         */
        public Key path() {
            return this.cpath;
        }
    }
}
